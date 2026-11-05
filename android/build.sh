#!/bin/bash
# Builds PSX.apk from source with no Android SDK:
#   ECJ      java sources -> class files (against the real API 10 android.jar)
#   jvm2smali tools/dex/jvm2smali.py -> smali text
#   smali    dexlib2 assembler (inside the apktool jar) -> classes.dex
#   aapt     manifest + assets -> APK, then a hand rolled v1 signature
set -eu
cd "$(dirname "$0")"

JAVA_HOME=${JAVA_HOME:-$HOME/.cache/jdk/jdk11}
ECJ_JAR=${ECJ_JAR:-$HOME/.cache/ecj/ecj.jar}
CACHE=${PSX_APK_CACHE:-$HOME/.cache/psx-apk}
API=10

need() { [ -e "$1" ] || { "$(dirname "$0")/tools/setup_toolchain.sh" >/dev/null; }; }
need "$CACHE/apktool.jar"

echo "[build] cleaning"
rm -rf build/classes build/smali build/classes.dex build/PSX.apk
mkdir -p build

echo "[build] javac (ECJ, source/target 1.6, API $API)"
find src -name '*.java' > build/sources.txt
"$JAVA_HOME/bin/java" -jar "$ECJ_JAR" \
    -source 1.6 -target 1.6 -nowarn -proc:none -g:none \
    -cp "$CACHE/android-10.jar" -d build/classes @build/sources.txt

echo "[build] jvm -> smali"
python3 tools/dex/jvm2smali.py build/classes build/smali

echo "[build] smali -> dex"
"$JAVA_HOME/bin/java" -cp "$CACHE:$CACHE/apktool.jar" Smali2Dex \
    build/smali build/classes.dex $API

echo "[build] packaging + signing"
python3 tools/apkpack.py --manifest AndroidManifest.xml \
    --dex build/classes.dex --out build/PSX.apk \
    --aapt "$CACHE/aapt" --android "$CACHE/android-10.jar" \
    --assets assets --sign build

echo "[build] verifying"
"$JAVA_HOME/bin/java" -cp "$CACHE" ApkVerify build/PSX.apk
"$CACHE/aapt" dump badging build/PSX.apk | head -6
echo "[build] done: build/PSX.apk"
