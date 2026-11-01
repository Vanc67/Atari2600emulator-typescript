#!/bin/sh
# =============================================================================
#  ChipLab - one-shot Android toolchain setup WITHOUT the Android SDK.
#
#  The sandbox has no JDK and cannot reach dl.google.com / Maven Central /
#  GitHub release assets. Everything below comes from PyPI, npm or plain
#  `git clone` from github.com (repository contents only):
#
#    JRE            PyPI wheel "jdk4py" (Temurin runtime + keytool)
#    javac          npm package "dataslope-tools-jar" (JDK 8 tools.jar)
#    android.jar    Sable/android-platforms repo (API 28 stubs from the SDK)
#    dx (class->dex) compiled from AOSP mirror aosp-mirror/platform_dalvik
#    apksig         compiled from LineageOS/android_tools_apksig (AOSP mirror)
#
#  javac 8 has no rt.jar on a modular JRE, so everything is compiled with
#  -bootclasspath android.jar (Android libcore signatures for java.*).
#  apksig additionally needs java.lang.invoke.LambdaMetafactory, which
#  libcore API 28 does not have; a signature-only stub is compiled for it
#  and the real class is used at runtime on the desktop JRE.
#
#  Result lives in .toolchain/ (git-ignored): jre/, tools.jar, android.jar,
#  dx-classes/, apksig-classes/, stub-classes/, debug.keystore (made later).
# =============================================================================
set -e
cd "$(dirname "$0")/.."
TC=".toolchain"
mkdir -p "$TC/dl"

if [ -x "$TC/jre/bin/java" ] && [ -f "$TC/tools.jar" ] && [ -f "$TC/android.jar" ] \
   && [ -d "$TC/dx-classes" ] && [ -d "$TC/apksig-classes" ]; then
    echo "[toolchain] already present in $TC/"
    exit 0
fi

JAVA="$TC/jre/bin/java"
javac8() { "$JAVA" -Xmx1g -cp "$TC/tools.jar" com.sun.tools.javac.Main "$@"; }

# --- JRE from PyPI -----------------------------------------------------------
if [ ! -x "$JAVA" ]; then
    echo "[toolchain] fetching JRE (jdk4py from PyPI)"
    python3 -m pip download --no-deps --no-cache-dir jdk4py -d "$TC/dl" >/dev/null
    python3 - <<'PYEOF'
import glob, os, zipfile
w = glob.glob(".toolchain/dl/*.whl")[0]
z = zipfile.ZipFile(w)
prefix = "jdk4py/java-runtime/"
for n in z.namelist():
    if n.startswith(prefix):
        out = os.path.join(".toolchain/jre", n[len(prefix):])
        if n.endswith("/"):
            os.makedirs(out, exist_ok=True)
        else:
            os.makedirs(os.path.dirname(out), exist_ok=True)
            open(out, "wb").write(z.read(n))
            if z.getinfo(n).external_attr >> 16 & 0o111:
                os.chmod(out, 0o755)
print("[toolchain] JRE extracted")
PYEOF
    chmod +x "$TC/jre/bin/"* 2>/dev/null || true
fi
"$JAVA" -version 2>&1 | head -1

# --- javac (JDK 8 tools.jar) from npm ----------------------------------------
if [ ! -f "$TC/tools.jar" ]; then
    echo "[toolchain] fetching javac (dataslope-tools-jar from npm)"
    (cd "$TC/dl" && npm pack dataslope-tools-jar >/dev/null 2>&1 && tar xzf *.tgz)
    cp "$TC/dl/package/tools.jar" "$TC/tools.jar"
fi
javac8 -version

# --- android.jar (API 28) via sparse git clone --------------------------------
if [ ! -f "$TC/android.jar" ]; then
    echo "[toolchain] fetching android.jar (Sable/android-platforms, sparse)"
    rm -rf "$TC/ap"
    git clone --filter=blob:none --sparse --depth 1 \
        https://github.com/Sable/android-platforms.git "$TC/ap"
    (cd "$TC/ap" && git sparse-checkout set android-28)
    cp "$TC/ap/android-28/android.jar" "$TC/android.jar"
    rm -rf "$TC/ap"
fi

# --- dx from AOSP sources ------------------------------------------------------
if [ ! -d "$TC/dx-classes" ]; then
    echo "[toolchain] building dx (aosp-mirror/platform_dalvik)"
    rm -rf "$TC/dalvik"
    git clone --filter=blob:none --sparse --depth 1 \
        https://github.com/aosp-mirror/platform_dalvik.git "$TC/dalvik"
    (cd "$TC/dalvik" && git sparse-checkout set dx)
    mkdir -p "$TC/dx-classes"
    find "$TC/dalvik/dx/src" -name '*.java' > "$TC/dl/dx-sources.txt"
    javac8 -source 1.7 -target 1.7 -bootclasspath "$TC/android.jar" -nowarn \
        -d "$TC/dx-classes" @"$TC/dl/dx-sources.txt"
    rm -rf "$TC/dalvik"
fi

# --- LambdaMetafactory stub (compile-time only) --------------------------------
mkdir -p "$TC/stubs/java/lang/invoke" "$TC/stub-classes"
cat > "$TC/stubs/java/lang/invoke/LambdaMetafactory.java" <<'EOF'
package java.lang.invoke;
public final class LambdaMetafactory {
    public static final int FLAG_SERIALIZABLE = 1;
    public static final int FLAG_MARKERS = 2;
    public static final int FLAG_BRIDGES = 4;
    private LambdaMetafactory() {}
    public static CallSite metafactory(MethodHandles.Lookup caller, String invokedName,
            MethodType invokedType, MethodType samMethodType, MethodHandle implMethod,
            MethodType instantiatedMethodType) throws LambdaConversionException {
        throw new RuntimeException("Stub!");
    }
    public static CallSite altMetafactory(MethodHandles.Lookup caller, String invokedName,
            MethodType invokedType, Object... args) throws LambdaConversionException {
        throw new RuntimeException("Stub!");
    }
}
EOF
javac8 -source 1.8 -target 1.8 -bootclasspath "$TC/android.jar" -nowarn \
    -d "$TC/stub-classes" "$TC/stubs/java/lang/invoke/LambdaMetafactory.java"

# --- apksig from AOSP sources (APK Signature Scheme v1/v2/v3) -------------------
if [ ! -d "$TC/apksig-classes" ]; then
    echo "[toolchain] building apksig (LineageOS/android_tools_apksig)"
    rm -rf "$TC/apksig-src"
    git clone --depth 1 -b lineage-20.0 \
        https://github.com/LineageOS/android_tools_apksig.git "$TC/apksig-src"
    mkdir -p "$TC/apksig-classes"
    find "$TC/apksig-src/src/main/java" -name '*.java' > "$TC/dl/apksig-sources.txt"
    javac8 -source 1.8 -target 1.8 -bootclasspath "$TC/stub-classes:$TC/android.jar" \
        -nowarn -d "$TC/apksig-classes" @"$TC/dl/apksig-sources.txt"
    # keep apksig-src: tools/SignApk.java is compiled against its public API
fi

echo "[toolchain] done. Now run ./build.sh"
