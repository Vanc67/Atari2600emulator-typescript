#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
PREVERIFIED="$BUILD/preverified"
RELEASE="$ROOT/release"
LIB="$ROOT/lib"
JAVAC=${JAVAC:-javac}
JAVA=${JAVA:-java}
JAR=${JAR:-jar}
CLDC="$LIB/cldc-1.1.jar"
MIDP="$LIB/midp-2.0.jar"
PROGUARD="$LIB/proguard.jar"
for required in "$CLDC" "$MIDP" "$PROGUARD"; do
    [ -f "$required" ] || { echo "Missing dependency: $required" >&2; exit 1; }
done
rm -rf "$BUILD"
mkdir -p "$CLASSES" "$PREVERIFIED" "$RELEASE"
find "$ROOT/src/main/java" -name '*.java' -print | sort > "$BUILD/sources.list"
"$JAVAC" -encoding UTF-8 -source 1.2 -target 1.2 -Xlint:-options -g:none \
    -bootclasspath "$CLDC:$MIDP" -classpath "$CLDC:$MIDP" -d "$CLASSES" @"$BUILD/sources.list"
"$JAR" cf "$BUILD/raw.jar" -C "$CLASSES" .
"$JAVA" -jar "$PROGUARD" -injars "$BUILD/raw.jar" -outjars "$BUILD/preverified.jar" \
    -libraryjars "$CLDC:$MIDP" -dontshrink -dontoptimize -dontobfuscate -microedition
(cd "$PREVERIFIED" && "$JAR" xf "$BUILD/preverified.jar")
"$JAR" cfm "$RELEASE/Wreckbox-K510i.jar" "$ROOT/manifest.MF" \
    -C "$PREVERIFIED" . -C "$ROOT/src/main/resources" .
SIZE=$(wc -c < "$RELEASE/Wreckbox-K510i.jar" | tr -d ' ')
{
    echo 'MIDlet-1: Wreckbox,,wreckbox.platform.WreckboxMidlet'
    echo 'MIDlet-Name: Wreckbox'
    echo 'MIDlet-Vendor: Wreckbox J2ME port'
    echo 'MIDlet-Version: 1.8.0'
    echo 'MIDlet-Jar-URL: Wreckbox-K510i.jar'
    echo "MIDlet-Jar-Size: $SIZE"
    echo 'MicroEdition-Profile: MIDP-2.0'
    echo 'MicroEdition-Configuration: CLDC-1.1'
} | awk '{printf "%s\r\n", $0}' > "$RELEASE/Wreckbox-K510i.jad"
printf 'Built %s (%s bytes)\n' "$RELEASE/Wreckbox-K510i.jar" "$SIZE"
