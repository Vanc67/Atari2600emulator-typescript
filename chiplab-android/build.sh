#!/bin/sh
# =============================================================================
#  ChipLab - build the signed APK without the Android SDK.
#
#    1. tools/make_bins.py    example chips (assets/*.bin) + AI prompt
#    2. javac 1.7             game sources against the real android.jar
#    3. harness/Smoke         run all example CPUs on the desktop JVM
#    4. dx                    .class -> classes.dex
#    5. tools/make_axml.py    binary AndroidManifest.xml (verified by Google's
#                             own AndroidBinXmlParser inside apksig)
#    6. tools/pack_apk.py     zip: manifest + dex + assets
#    7. apksig                sign v1+v2+v3 with a local debug key, then verify
#
#  Usage: ./tools/setup_toolchain.sh   (once)
#         ./build.sh                   -> ChipLab.apk
# =============================================================================
set -e
cd "$(dirname "$0")"
TC=".toolchain"
BUILD="build"

if [ ! -x "$TC/jre/bin/java" ] || [ ! -d "$TC/dx-classes" ]; then
    echo "[build] no toolchain - run tools/setup_toolchain.sh first" >&2
    exit 1
fi
JAVA="$TC/jre/bin/java"
javac8() { "$JAVA" -Xmx1g -cp "$TC/tools.jar" com.sun.tools.javac.Main "$@"; }

mkdir -p "$BUILD/classes" "$BUILD/test-classes" "$BUILD/tool-classes" "$BUILD/dex"

echo "[build] 1/7 example chips + AI prompt"
python3 tools/make_bins.py assets

echo "[build] 2/7 compile game (javac -bootclasspath android.jar, Java 7 bytecode)"
rm -rf "$BUILD/classes"; mkdir -p "$BUILD/classes"
javac8 -source 1.7 -target 1.7 -bootclasspath "$TC/android.jar" -encoding UTF-8 -nowarn \
    -d "$BUILD/classes" \
    src/com/chiplab/ChipBin.java src/com/chiplab/Cpu.java src/com/chiplab/MainActivity.java

echo "[build] 3/7 Smoke test: run every example CPU on the desktop JVM"
rm -rf "$BUILD/test-classes"; mkdir -p "$BUILD/test-classes"
javac8 -source 1.7 -target 1.7 -bootclasspath "$TC/android.jar" -encoding UTF-8 -nowarn \
    -d "$BUILD/test-classes" \
    src/com/chiplab/ChipBin.java src/com/chiplab/Cpu.java \
    harness/com/chiplab/harness/Smoke.java
"$JAVA" -cp "$BUILD/test-classes" com.chiplab.harness.Smoke assets

echo "[build] 4/7 dex (AOSP dx)"
rm -rf "$BUILD/dex"; mkdir -p "$BUILD/dex"
"$JAVA" -cp "$TC/dx-classes" com.android.dx.command.Main \
    --dex --min-sdk-version=21 --output="$BUILD/dex" "$BUILD/classes"

echo "[build] 5/7 binary AndroidManifest.xml"
python3 tools/make_axml.py "$TC/android.jar" "$BUILD/AndroidManifest.xml"

echo "[build] 6/7 pack + verify manifest with Google's parser"
rm -rf "$BUILD/tool-classes"; mkdir -p "$BUILD/tool-classes"
javac8 -source 1.8 -target 1.8 -bootclasspath "$TC/stub-classes:$TC/android.jar" \
    -cp "$TC/apksig-classes" -nowarn -d "$BUILD/tool-classes" tools/SignApk.java
"$JAVA" -cp "$BUILD/tool-classes:$TC/apksig-classes" SignApk manifest "$BUILD/AndroidManifest.xml"
python3 tools/pack_apk.py "$BUILD/AndroidManifest.xml" "$BUILD/dex/classes.dex" assets \
    "$BUILD/chiplab-unsigned.apk"

echo "[build] 7/7 sign (v1+v2+v3) + verify"
if [ ! -f "$TC/debug.keystore" ]; then
    "$TC/jre/bin/keytool" -genkeypair -keystore "$TC/debug.keystore" -storetype PKCS12 \
        -storepass android -keypass android -alias chiplab -keyalg RSA -keysize 2048 \
        -validity 10000 -dname "CN=ChipLab,O=ChipLab,C=RU"
fi
"$JAVA" -cp "$BUILD/tool-classes:$TC/apksig-classes" SignApk sign \
    "$TC/debug.keystore" android chiplab android \
    "$BUILD/chiplab-unsigned.apk" ChipLab.apk
"$JAVA" -cp "$BUILD/tool-classes:$TC/apksig-classes" SignApk verify ChipLab.apk

echo "[build] DONE: ChipLab.apk"
