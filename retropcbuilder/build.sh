#!/bin/sh
# =============================================================================
#  Retro PC Builder - J2ME build script
#
#  Compiles the game for CLDC 1.1 / MIDP 2.0 (class file 47.0, the format the
#  old handsets actually accept) without requiring a J2ME SDK:
#
#     1. tools/gen_stub_classes.py turns the CLDC/MIDP API stubs under stubs/
#        into class files - they become javac's boot class path
#     2. src/rpc/**/*.java is compiled with -source 1.3 -target 1.3. That
#        matters: it makes javac emit StringBuffer (not StringBuilder, which
#        CLDC 1.1 does not have) and keeps the bytecode at major 47
#     3. tools/pack_jar.py packs classes + the data files (parts.pak,
#        gallery.pak, soundbank.pak, assets.bin, levels.txt, dict.txt) into
#        RetroPcBuilder-5.0.jar, then verifies the result
#
#  Usage:
#     ./build.sh                 build data + jar
#     ./build.sh --clean         remove build/ first
#     ./build.sh --harness       also compile the desktop test harness
#     ./build.sh --shots         render the UI screenshots (needs --harness)
#
#  Environment:
#     JAVA_HOME          JDK/JRE to use (bin/java); default: .toolchain/jre
#     TOOLS_JAR          javac jar (JDK 8 tools.jar); default: .toolchain/tools.jar
#     SKIP_DATA=1        do not regenerate the data packs
# =============================================================================
set -e

cd "$(dirname "$0")"

OUT_JAR="RetroPcBuilder-5.0.jar"
BUILD="build"
CLASSES="$BUILD/classes"
STUBS="$BUILD/stub-classes"
HARNESS_CLASSES="$BUILD/harness-classes"
WANT_HARNESS=0
WANT_SHOTS=0

while [ $# -gt 0 ]; do
    case "$1" in
        --clean) rm -rf "$BUILD"; shift ;;
        --harness) WANT_HARNESS=1; shift ;;
        --shots) WANT_HARNESS=1; WANT_SHOTS=1; shift ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "unknown option: $1"; exit 2 ;;
    esac
done

PY=python3
command -v python3 >/dev/null 2>&1 || PY=python

JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/java}"
[ -x "$JAVA_BIN" ] || JAVA_BIN="$PWD/.toolchain/jre/bin/java"
[ -x "$JAVA_BIN" ] || JAVA_BIN=$(command -v java || true)
TOOLS_JAR="${TOOLS_JAR:-$PWD/.toolchain/tools.jar}"

if [ -z "$JAVA_BIN" ] || [ ! -x "$JAVA_BIN" ] || [ ! -f "$TOOLS_JAR" ]; then
    echo "[build] no Java toolchain found."
    echo "[build] run tools/setup_toolchain.sh once (creates .toolchain/), or"
    echo "[build] set JAVA_HOME and TOOLS_JAR."
    exit 1
fi
echo "[build] java:  $JAVA_BIN"
echo "[build] javac: $TOOLS_JAR"

mkdir -p "$BUILD" "$CLASSES" "$STUBS" "$HARNESS_CLASSES"

# ---------------------------------------------------------------------------
#  1. API stubs -> boot class path
# ---------------------------------------------------------------------------
$PY tools/gen_stub_classes.py --src stubs --out "$STUBS" --quiet

# ---------------------------------------------------------------------------
#  2. compile the game
# ---------------------------------------------------------------------------
SRC=$(find src -name '*.java' | sort)
echo "[build] compiling $(echo "$SRC" | wc -l | tr -d ' ') sources"
# shellcheck disable=SC2086
"$JAVA_BIN" -Xmx512m -cp "$TOOLS_JAR" com.sun.tools.javac.Main \
    -source 1.3 -target 1.3 -bootclasspath "$STUBS" -nowarn \
    -d "$CLASSES" $SRC

$PY tools/pack_jar.py downgrade --classes "$CLASSES"

# ---------------------------------------------------------------------------
#  3. jar + verification
# ---------------------------------------------------------------------------
$PY tools/pack_jar.py pack --classes "$CLASSES" --res data --out "$OUT_JAR"
$PY tools/pack_jar.py jad "$OUT_JAR"
$PY tools/pack_jar.py verify "$OUT_JAR"

# ---------------------------------------------------------------------------
#  4. optional: desktop harness (renders the real game to BMP frames)
# ---------------------------------------------------------------------------
if [ "$WANT_HARNESS" = "1" ]; then
    HSRC=$(find harness -name '*.java' | sort)
    # shellcheck disable=SC2086
    "$JAVA_BIN" -Xmx512m -cp "$TOOLS_JAR" com.sun.tools.javac.Main \
        -source 1.3 -target 1.3 -bootclasspath "$STUBS" -nowarn \
        -classpath "$CLASSES" -d "$HARNESS_CLASSES" $HSRC
    echo "[build] harness compiled -> $HARNESS_CLASSES"
    "$JAVA_BIN" -cp "$CLASSES:$HARNESS_CLASSES" rpc.harness.Smoke data
    if [ "$WANT_SHOTS" = "1" ]; then
        mkdir -p docs/screens
        "$JAVA_BIN" -cp "$CLASSES:$HARNESS_CLASSES" rpc.harness.Shots \
            docs/screens
        "$JAVA_BIN" -cp "$CLASSES:$HARNESS_CLASSES" rpc.harness.FontTest \
            docs/screens/font.bmp
    fi
fi

echo "[build] done: $OUT_JAR"
