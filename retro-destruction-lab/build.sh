#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BUILD="$ROOT/build"
CLASSES="$BUILD/classes"
PREVERIFIED="$BUILD/preverified"
RELEASE="$ROOT/release"
LIB_DIR=${J2ME_LIB_DIR:-"$ROOT/lib"}
PROGUARD_JAR=${PROGUARD_JAR:-"$LIB_DIR/proguard.jar"}
JAVAC=${JAVAC:-javac}
JAVA=${JAVA:-java}
JAR=${JAR:-jar}
CLDC_JAR="$LIB_DIR/cldc-1.1.jar"
MIDP_JAR="$LIB_DIR/midp-2.0.jar"

for required in "$CLDC_JAR" "$MIDP_JAR" "$PROGUARD_JAR"; do
    if [ ! -f "$required" ]; then
        echo "Missing build dependency: $required" >&2
        echo "Run $ROOT/tools/fetch-deps.sh first." >&2
        exit 1
    fi
done

rm -rf "$BUILD"
mkdir -p "$CLASSES" "$PREVERIFIED" "$RELEASE"
find "$ROOT/src" -name '*.java' -print > "$BUILD/sources.list"

# JDK 8 is intentional: -target 1.2 emits the old class format expected by CLDC.
"$JAVAC" -source 1.2 -target 1.2 -Xlint:-options -g:none -encoding UTF-8 \
    -bootclasspath "$CLDC_JAR:$MIDP_JAR" \
    -classpath "$CLDC_JAR:$MIDP_JAR" \
    -d "$CLASSES" @"$BUILD/sources.list"

"$JAR" cf "$BUILD/raw.jar" -C "$CLASSES" .
"$JAVA" -jar "$PROGUARD_JAR" \
    -injars "$BUILD/raw.jar" \
    -outjars "$BUILD/preverified.jar" \
    -libraryjars "$CLDC_JAR:$MIDP_JAR" \
    -dontshrink -dontoptimize -dontobfuscate -microedition

(
    cd "$PREVERIFIED"
    "$JAR" xf "$BUILD/preverified.jar"
)
"$JAR" cfm "$RELEASE/RetroDestructionLab.jar" "$ROOT/manifest.MF" \
    -C "$PREVERIFIED" . -C "$ROOT/res" .

cp "$ROOT/deploy.jad" "$BUILD/RetroDestructionLab.jad"
JAR_SIZE=$(wc -c < "$RELEASE/RetroDestructionLab.jar" | tr -d ' ')
echo "MIDlet-Jar-Size: $JAR_SIZE" >> "$BUILD/RetroDestructionLab.jad"
# CRLF is accepted by picky older JAD parsers, including legacy phone installers.
awk '{printf "%s\r\n", $0}' "$BUILD/RetroDestructionLab.jad" > "$RELEASE/RetroDestructionLab.jad"

printf '\nBuilt Retro Destruction Lab for K510i:\n'
printf '  %s (%s bytes)\n' "$RELEASE/RetroDestructionLab.jar" "$JAR_SIZE"
printf '  %s\n' "$RELEASE/RetroDestructionLab.jad"
