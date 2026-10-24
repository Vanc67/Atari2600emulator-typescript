#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SHARED="$ROOT/../wreckbox-j2me"
BUILD="$ROOT/build"
RELEASE="$ROOT/release"
APK="$RELEASE/Wreckbox-1.8-Android2.3.6.apk"
JAVAC=${JAVAC:-javac}
JAVA=${JAVA:-java}
JAR=${JAR:-jar}
JARSIGNER=${JARSIGNER:-jarsigner}
KEYTOOL=${KEYTOOL:-keytool}
ANDROID_JAR=${ANDROID_JAR:?"set ANDROID_JAR to the API-10 android.jar"}
AAPT2=${AAPT2:?"set AAPT2 to aapt2"}
D8_JAR=${D8_JAR:?"set D8_JAR to the desktop D8/R8 jar"}
D8_JAVA_FLAGS=${D8_JAVA_FLAGS:-}
KEYSTORE="$ROOT/debug.keystore"

rm -rf "$BUILD"
mkdir -p "$BUILD/classes" "$BUILD/dex" "$RELEASE"
find "$SHARED/src/main/java/wreckbox/core" -name '*.java' -print | sort > "$BUILD/sources.list"
for source in DamagePatterns WreckboxCanvas WreckboxRenderer; do
  printf '%s\n' "$SHARED/src/main/java/wreckbox/platform/$source.java" >> "$BUILD/sources.list"
done
find "$ROOT/src/main/java" "$ROOT/src/adapters" -name '*.java' -print | sort >> "$BUILD/sources.list"
"$JAVAC" -encoding UTF-8 -source 7 -target 7 -Xlint:-options \
  -bootclasspath "$ANDROID_JAR" -d "$BUILD/classes" @"$BUILD/sources.list"
"$JAR" cf "$BUILD/classes.jar" -C "$BUILD/classes" .

"$AAPT2" compile --dir "$ROOT/res" -o "$BUILD/resources.zip"
"$AAPT2" link -o "$BUILD/unsigned.apk" -I "$ANDROID_JAR" \
  --manifest "$ROOT/AndroidManifest.xml" -A "$SHARED/src/main/resources" \
  -0 amr --min-sdk-version 10 --target-sdk-version 10 "$BUILD/resources.zip"
# shellcheck disable=SC2086
"$JAVA" $D8_JAVA_FLAGS -cp "$D8_JAR" com.android.tools.r8.D8 \
  --min-api 10 --lib "$ANDROID_JAR" --output "$BUILD/dex" "$BUILD/classes.jar"
(
  cd "$BUILD/dex"
  "$JAR" uf "$BUILD/unsigned.apk" classes.dex
)

if [ ! -f "$KEYSTORE" ]; then
  "$KEYTOOL" -genkeypair -keystore "$KEYSTORE" -alias wreckbox \
    -storepass wreckbox -keypass wreckbox -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Wreckbox Android Port, OU=Local Debug, O=Vanc67, C=RU" >/dev/null 2>&1
fi
cp "$BUILD/unsigned.apk" "$APK"
"$JARSIGNER" -keystore "$KEYSTORE" -storepass wreckbox -keypass wreckbox \
  -sigalg SHA256withRSA -digestalg SHA-256 "$APK" wreckbox >/dev/null
"$JARSIGNER" -verify "$APK" > "$BUILD/signature.txt" 2>&1
grep -q 'jar verified' "$BUILD/signature.txt"
"$AAPT2" dump badging "$APK" > "$BUILD/badging.txt"
python3 "$ROOT/tools/verify_apk.py" "$APK" "$BUILD/badging.txt" "$SHARED/src/main/resources"
(
  cd "$RELEASE"
  sha256sum "$(basename "$APK")" > SHA256SUMS
)
printf 'APK %s bytes\n' "$(stat -c %s "$APK")"
sha256sum "$APK"
