#!/usr/bin/env bash
#
# build-apk.sh - build and sign a real Android APK with no Gradle, no Android
# Studio and no network access to dl.google.com.
#
# Pipeline:
#   tools/mkactivity.py   emits MainActivity.class      (hand-written class-file writer)
#   d8                    class -> classes.dex
#   tools/make_icons.py   renders the launcher PNGs     (stdlib zlib, no Pillow)
#   aapt2 compile/link    resources + manifest + assets -> base APK
#   (zip)                 classes.dex is added to the APK
#   zipalign              4-byte alignment
#   keytool + apksigner   generate a key, sign v1+v2+v3
#
#   ./build-apk.sh                          # payload = ./www
#   ./build-apk.sh --payload /path/to/dist  # any static web build
#   ./build-apk.sh --stop-after dex         # stop early (debugging)
#   ./build-apk.sh --out build/app.apk --version-name 1.2 --version-code 3
#
# Requirements: a Java *runtime* (a JRE is enough - nothing is compiled with
# javac), and an Android SDK build-tools directory plus one android.jar. Set
# ANDROID_SDK / JAVA_HOME or let the script find them.
#
set -euo pipefail

# The script lives in tools/, but every path below is relative to the project
# root (android/, www/, build/), so resolve one level up.
HERE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$HERE"

PAYLOAD="$HERE/www"
OUT="$HERE/build/app.apk"
STOP=""
VERSION_NAME="1.0"
VERSION_CODE="1"
API="33"
MIN_API="21"
VERBOSE=0

usage() { sed -n '2,25p' "$0"; exit 0; }
while [ $# -gt 0 ]; do
  case "$1" in
    --payload)      PAYLOAD="$2"; shift 2 ;;
    --out)          OUT="$2"; shift 2 ;;
    --stop-after)   STOP="$2"; shift 2 ;;
    --version-name) VERSION_NAME="$2"; shift 2 ;;
    --version-code) VERSION_CODE="$2"; shift 2 ;;
    --api)          API="$2"; shift 2 ;;
    --min-api)      MIN_API="$2"; shift 2 ;;
    -v|--verbose)   VERBOSE=1; shift ;;
    -h|--help)      usage ;;
    *) echo "unknown option: $1" >&2; exit 64 ;;
  esac
done

log() { printf '\033[36m[apk]\033[0m %s\n' "$*"; }
die() { printf '\033[31m[apk] %s\033[0m\n' "$*" >&2; exit 1; }
step() { [ "$VERBOSE" = 1 ] && echo "+ $*" >&2; "$@"; }

# ---------------------------------------------------------------- locate JDK
if [ -z "${JAVA_HOME:-}" ]; then
  for cand in \
      "$HOME"/jenv/lib/python*/site-packages/jdk4py/java-runtime \
      "$HOME"/.venv/lib/python*/site-packages/jdk4py/java-runtime \
      /usr/lib/jvm/* /opt/java/* ; do
    if [ -x "$cand/bin/java" ]; then JAVA_HOME="$cand"; break; fi
  done
fi
[ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ] || die "no Java runtime found (JAVA_HOME, or pip install jdk4py)"
export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

# ---------------------------------------------------------------- locate SDK
SDK="${ANDROID_SDK:-${ANDROID_HOME:-$HOME/android-sdk}}"
BT=""
if [ -d "$SDK/build-tools" ]; then
  BT="$(ls -1d "$SDK"/build-tools/*/ 2>/dev/null | sort -V | tail -1)"
  BT="${BT%/}"
fi
[ -n "$BT" ] && [ -x "$BT/aapt2" ] || die "no Android build-tools under $SDK/build-tools (set ANDROID_SDK)"
JAR="$(ls -1d "$SDK"/platforms/android-"$API"/android.jar 2>/dev/null | head -1 || true)"
if [ -z "$JAR" ]; then
  JAR="$(ls -1d "$SDK"/platforms/*/android.jar 2>/dev/null | sort -V | tail -1 || true)"
  [ -n "$JAR" ] || die "no android.jar under $SDK/platforms"
  API="$(basename "$(dirname "$JAR")" | sed 's/android-//')"
fi

log "java       : $JAVA_HOME"
log "build-tools: $BT"
log "android.jar: $JAR (API $API)"
log "payload    : $PAYLOAD"
[ -d "$PAYLOAD" ] && [ -f "$PAYLOAD/index.html" ] || die "payload $PAYLOAD has no index.html"

rm -rf "$HERE/build/classes" "$HERE/build/dex" "$HERE/build/res"
mkdir -p "$HERE/build/classes" "$HERE/build/dex" "$HERE/build/apk" "$(dirname "$OUT")"
stop() { [ -n "$STOP" ] && [ "$STOP" = "$1" ] && { log "stopped after $1"; exit 0; }; return 0; }

# ---------------------------------------------------------------- 1. class
log "1/7 emitting MainActivity.class (no javac in this toolchain)"
step python3 tools/mkactivity.py "$HERE/build/classes"
CLASSFILE="$(find "$HERE/build/classes" -name 'MainActivity.class' | head -1)"
[ -n "$CLASSFILE" ] || die "mkactivity.py produced no class file"

# ---------------------------------------------------------------- 2. dex
log "2/7 d8 -> classes.dex"
step "$BT/d8" --min-api "$MIN_API" --lib "$JAR" --output "$HERE/build/dex" "$CLASSFILE"
[ -f "$HERE/build/dex/classes.dex" ] || die "d8 produced no classes.dex"
stop dex

# ---------------------------------------------------------------- 3. icons
log "3/7 rendering launcher icons"
step python3 tools/make_icons.py "$HERE/build/res"

# ---------------------------------------------------------------- 4. aapt2
log "4/7 aapt2 compile + link"
# aapt2's -A makes the *given directory* the assets root, so pointing it at the
# payload would put index.html at assets/index.html. The Activity loads
# file:///android_asset/www/index.html, so stage the payload under www/ first.
STAGE="$HERE/build/assets"
rm -rf "$STAGE"; mkdir -p "$STAGE/www"
cp -R "$PAYLOAD"/. "$STAGE/www/"
[ -f "$STAGE/www/index.html" ] || die "staged payload has no www/index.html"
log "   staged $(find "$STAGE/www" -type f | wc -l) file(s) under assets/www/"

"$BT/aapt2" compile --dir "$HERE/build/res" -o "$HERE/build/res.zip"
"$BT/aapt2" compile --dir "$HERE/android/res" -o "$HERE/build/res-app.zip"
step "$BT/aapt2" link \
  -o "$HERE/build/apk/base.apk" \
  -I "$JAR" \
  --manifest "$HERE/android/AndroidManifest.xml" \
  -A "$STAGE" \
  -R "$HERE/build/res.zip" \
  -R "$HERE/build/res-app.zip" \
  --auto-add-overlay \
  --min-sdk-version "$MIN_API" \
  --target-sdk-version "$API" \
  --version-code "$VERSION_CODE" \
  --version-name "$VERSION_NAME"
stop link

# ---------------------------------------------------------------- 5. add dex
log "5/7 adding classes.dex to the package"
BASE="$HERE/build/apk/base.apk"
DEXED="$HERE/build/apk/with-dex.apk"
python3 - "$BASE" "$HERE/build/dex/classes.dex" "$DEXED" <<'PY'
import shutil, sys, zipfile
base, dex, out = sys.argv[1], sys.argv[2], sys.argv[3]
shutil.copyfile(base, out)
with open(dex, "rb") as fh:
    payload = fh.read()
with zipfile.ZipFile(out, "a") as zf:
    # STORED, so that zipalign can 4-byte align it (what ART wants)
    zf.writestr(zipfile.ZipInfo("classes.dex"), payload, compress_type=zipfile.ZIP_STORED)
print("   classes.dex: %d bytes" % len(payload))
PY
stop apk

# ---------------------------------------------------------------- 6. align
log "6/7 zipalign"
ALIGNED="$HERE/build/apk/aligned.apk"
"$BT/zipalign" -f 4 "$DEXED" "$ALIGNED"

# ---------------------------------------------------------------- 7. sign
log "7/7 signing (v1 + v2 + v3)"
KS="${APK_KEYSTORE:-$HERE/build/debug.keystore}"
KSPASS="${APK_KEYSTORE_PASS:-android}"
if [ ! -f "$KS" ]; then
  log "   generating a debug key at $KS"
  "$JAVA_HOME/bin/keytool" -genkeypair -v \
    -keystore "$KS" -storepass "$KSPASS" -keypass "$KSPASS" \
    -alias apkshell -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=apk-toolchain, OU=Arena, O=Arena, L=Unknown, ST=Unknown, C=US" >/dev/null 2>&1
fi
"$BT/apksigner" sign \
  --ks "$KS" --ks-key-alias apkshell \
  --ks-pass "pass:$KSPASS" --key-pass "pass:$KSPASS" \
  --v1-signing-enabled true --v2-signing-enabled true --v3-signing-enabled true \
  --out "$OUT" "$ALIGNED"

echo
"$BT/apksigner" verify --print-certs --verbose "$OUT" | sed 's/^/   /'
echo
log "badging:"
"$BT/aapt2" dump badging "$OUT" 2>/dev/null | grep -E "^(package|application-label|launchable-activity|sdkVersion|targetSdkVersion|uses-permission)" | sed 's/^/   /'
echo
log "OK -> $OUT ($(du -h "$OUT" | cut -f1))"
log "install with:  adb install -r $OUT"
