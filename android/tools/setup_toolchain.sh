#!/bin/bash
# Fetches everything the APK build needs into ~/.cache/psx-apk.
#
# There is no Android SDK on this machine, so the pieces are picked up from
# places that are reachable:
#   * apktool 2.4.1 (npm, "apktool-jar") -- supplies dexlib2's smali assembler
#     and the linux aapt/aapt2 binaries
#   * android.jar for API 10 (GitHub, Sable/android-platforms) -- the real
#     Gingerbread API surface, compiled against and used for resource linking
#
# Everything is checked by size and by actually running it, so a truncated
# download fails here instead of producing a broken APK later.
set -eu

CACHE=${PSX_APK_CACHE:-$HOME/.cache/psx-apk}
JAVA_HOME=${JAVA_HOME:-$HOME/.cache/jdk/jdk11}
ECJ_JAR=${ECJ_JAR:-$HOME/.cache/ecj/ecj.jar}
APKTOOL_VERSION=2.4.1

mkdir -p "$CACHE"

die() { echo "error: $*" >&2; exit 1; }

need_java() {
    [ -x "$JAVA_HOME/bin/java" ] || die "no JDK at $JAVA_HOME (run ps1j2me/tools/setup_toolchain.sh)"
    [ -f "$ECJ_JAR" ] || die "no ecj.jar at $ECJ_JAR"
    "$JAVA_HOME/bin/java" -version 2>&1 | head -1
}

# ---------------------------------------------------------------- apktool ---
if [ ! -f "$CACHE/apktool.jar" ] || [ "$(stat -c%s "$CACHE/apktool.jar")" -lt 10000000 ]; then
    echo "[toolchain] fetching apktool $APKTOOL_VERSION"
    tmp=$(mktemp -d)
    curl -sSfL -o "$tmp/apktool.tgz" \
        "https://registry.npmjs.org/apktool-jar/-/apktool-jar-$APKTOOL_VERSION.tgz" \
        || die "cannot download apktool"
    tar xzf "$tmp/apktool.tgz" -C "$tmp"
    cp "$tmp/package/bin/apktool_$APKTOOL_VERSION.jar" "$CACHE/apktool.jar"
    rm -rf "$tmp"
fi

# ------------------------------------------------------------------- aapt ---
if [ ! -x "$CACHE/aapt" ]; then
    echo "[toolchain] extracting aapt"
    unzip -o -j -q "$CACHE/apktool.jar" prebuilt/linux/aapt prebuilt/linux/aapt2 -d "$CACHE"
    chmod +x "$CACHE/aapt" "$CACHE/aapt2"
fi
"$CACHE/aapt" version >/dev/null || die "aapt does not run"

# -------------------------------------------------------------- android.jar -
if [ ! -f "$CACHE/android-10.jar" ]; then
    echo "[toolchain] fetching android.jar (API 10)"
    tmp=$(mktemp -d)
    git clone -q --depth 1 --filter=blob:none --sparse \
        https://github.com/Sable/android-platforms.git "$tmp/src" \
        || die "cannot clone android-platforms"
    ( cd "$tmp/src" && git sparse-checkout set android-10 >/dev/null )
    cp "$tmp/src/android-10/android.jar" "$CACHE/android-10.jar"
    rm -rf "$tmp"
fi
unzip -l "$CACHE/android-10.jar" | grep -q "resources.arsc" \
    || die "android-10.jar has no resources.arsc"

# --------------------------------------------------- smali -> dex driver ----
SRC=$(cd "$(dirname "$0")" && pwd)
if [ ! -f "$CACHE/Smali2Dex.class" ]; then
    echo "[toolchain] building the smali->dex driver"
    "$JAVA_HOME/bin/java" -jar "$ECJ_JAR" -source 1.8 -target 1.8 -nowarn \
        -cp "$CACHE/apktool.jar" -d "$CACHE" "$SRC/Smali2Dex.java" \
        || die "cannot compile Smali2Dex"
fi

# ------------------------------------------------------ APK v1 verifier ----
if [ ! -f "$CACHE/ApkVerify.class" ]; then
    echo "[toolchain] building the APK verifier"
    "$JAVA_HOME/bin/java" -jar "$ECJ_JAR" -source 1.8 -target 1.8 -nowarn \
        -d "$CACHE" "$SRC/ApkVerify.java" \
        || die "cannot compile ApkVerify"
fi

echo "[toolchain] ok"
echo "  java       $JAVA_HOME"
echo "  apktool    $CACHE/apktool.jar"
echo "  aapt       $CACHE/aapt"
echo "  android    $CACHE/android-10.jar"
echo "  smali2dex  $CACHE/Smali2Dex.class"
