#!/bin/sh
# =============================================================================
#  PS1J2ME build script
#
#  Compiles the emulator for CLDC 1.1 / MIDP 2.0 / JSR-75, embeds the BIOS
#  image from res/roms/scph1001.bin and produces PS1J2ME.jar with Java 1.3
#  (major version 47) bytecode.
#
#  Pipeline:
#     1. compile the J2ME API stubs (bootclasspath, never packaged)
#     2. compile psx/*.java with -source 1.3 -target 1.3
#     3. optionally run ProGuard (-microedition -target 1.3) to shrink and
#        obfuscate; ProGuard also rewrites the stack maps for KVM
#     4. fall back to tools/j2me_pack.py (strips StackMapTable, forces 47.0)
#     5. pack classes + /res/roms/scph1001.bin + /res/demos/*.iso into
#        PS1J2ME.jar and verify it
#
#  Usage:
#     ./build.sh                     build PS1J2ME.jar
#     ./build.sh --bios my/scph1001.bin
#     ./build.sh --no-proguard       skip ProGuard
#     ./build.sh --clean             remove build/ first
#
#  Environment:
#     JAVA_HOME     JDK to use (javac/java/jar are looked up there first)
#     ECJ_JAR       path to ecj.jar (Java 1.3 capable compiler)
#     PROGUARD_JAR  path to proguard.jar
# =============================================================================
set -e

cd "$(dirname "$0")"

APP_NAME="PS1J2ME"
OUT_JAR="PS1J2ME.jar"
BUILD="build"
CLASSES="$BUILD/classes"
STUBS="$BUILD/stubs"
BIOS_SRC="res/roms/scph1001.bin"
USE_PROGUARD=1
CLEAN=0

while [ $# -gt 0 ]; do
    case "$1" in
        --bios) BIOS_SRC="$2"; shift 2 ;;
        --out) OUT_JAR="$2"; shift 2 ;;
        --no-proguard) USE_PROGUARD=0; shift ;;
        --clean) CLEAN=1; shift ;;
        -h|--help) sed -n '2,30p' "$0"; exit 0 ;;
        *) echo "unknown option: $1"; exit 2 ;;
    esac
done

if [ "$CLEAN" = "1" ]; then rm -rf "$BUILD"; fi
mkdir -p "$BUILD" "$CLASSES" "$STUBS" lib

PY=python3
command -v python3 >/dev/null 2>&1 || PY=python

# ---------------------------------------------------------------------------
#  tool discovery
# ---------------------------------------------------------------------------
find_tool() {
    name="$1"
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/$name" ]; then
        echo "$JAVA_HOME/bin/$name"; return 0
    fi
    command -v "$name" 2>/dev/null && return 0
    return 1
}

JAVA_BIN=$(find_tool java || true)
JAVAC_BIN=$(find_tool javac || true)

download() {
    url="$1"; dest="$2"
    echo "[build] downloading $url"
    if command -v curl >/dev/null 2>&1; then curl -fsSL "$url" -o "$dest"
    elif command -v wget >/dev/null 2>&1; then wget -q "$url" -O "$dest"
    else return 1; fi
}

ECJ=""
if [ -n "$ECJ_JAR" ] && [ -f "$ECJ_JAR" ]; then
    ECJ="$ECJ_JAR"
elif [ -f lib/ecj.jar ]; then
    ECJ="lib/ecj.jar"
else
    download "https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.26.0/ecj-3.26.0.jar" lib/ecj.jar 2>/dev/null && ECJ="lib/ecj.jar" || true
fi

compile_java() {
    # $1 = output dir, $2 = bootclasspath (may be empty), $3.. = sources
    out="$1"; bcp="$2"; shift 2
    if [ -n "$ECJ" ] && [ -n "$JAVA_BIN" ]; then
        if [ -n "$bcp" ]; then
            "$JAVA_BIN" -Xmx256m -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
                -source 1.3 -target 1.3 -nowarn -g -encoding UTF-8 \
                -bootclasspath "$bcp" -d "$out" "$@"
        else
            "$JAVA_BIN" -Xmx256m -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
                -source 1.3 -target 1.3 -nowarn -g -encoding UTF-8 -d "$out" "$@"
        fi
    elif [ -n "$JAVAC_BIN" ]; then
        if [ -n "$bcp" ]; then
            "$JAVAC_BIN" -encoding UTF-8 -source 1.3 -target 1.3 -nowarn -g -bootclasspath "$bcp" -d "$out" "$@" 2>/dev/null || \
            "$JAVAC_BIN" --release 7 -nowarn -g -d "$out" "$@"
        else
            "$JAVAC_BIN" -encoding UTF-8 -source 1.3 -target 1.3 -nowarn -g -d "$out" "$@" 2>/dev/null || \
            "$JAVAC_BIN" --release 7 -nowarn -g -d "$out" "$@"
        fi
    else
        echo "[build] ERROR: no Java compiler found." >&2
        echo "        Install a JDK, or put ecj.jar in lib/ or set ECJ_JAR." >&2
        exit 1
    fi
}

# ---------------------------------------------------------------------------
#  1. stubs (J2ME API, compile time only)
# ---------------------------------------------------------------------------
echo "[build] compiling CLDC/MIDP/JSR-75 stubs"
compile_java "$STUBS" "" $(find stubs -name '*.java')

# ---------------------------------------------------------------------------
#  2. emulator
# ---------------------------------------------------------------------------
echo "[build] compiling emulator (source/target 1.3)"
rm -rf "$CLASSES"; mkdir -p "$CLASSES"
compile_java "$CLASSES" "$STUBS" $(find src -name '*.java')

# ---------------------------------------------------------------------------
#  3. BIOS
# ---------------------------------------------------------------------------
if [ ! -f "$BIOS_SRC" ] || [ "$(wc -c < "$BIOS_SRC")" -ne 524288 ]; then
    if [ -f "$BIOS_SRC" ]; then
        echo "[build] WARNING: $BIOS_SRC is not 524288 bytes, using placeholder"
    fi
    echo "[build] generating placeholder BIOS (put your own SCPH1001.BIN in res/roms/)"
    "$PY" tools/mkbios_stub.py "$BIOS_SRC"
else
    echo "[build] BIOS: $BIOS_SRC ($(wc -c < "$BIOS_SRC") bytes)"
fi
mkdir -p res/roms

# ---------------------------------------------------------------------------
#  3b. demo discs (embedded in the JAR, built from tools/mkdemo.py)
# ---------------------------------------------------------------------------
echo "[build] generating demo discs"
"$PY" tools/mkdemo.py --out res/demos | tail -1

# ---------------------------------------------------------------------------
#  4. ProGuard
# ---------------------------------------------------------------------------
PACK_DIR="$CLASSES"
if [ "$USE_PROGUARD" = "1" ]; then
    PG=""
    if [ -n "$PROGUARD_JAR" ] && [ -f "$PROGUARD_JAR" ]; then
        PG="$PROGUARD_JAR"
    elif [ -f lib/proguard.jar ]; then
        PG="lib/proguard.jar"
    else
        download "https://repo1.maven.org/maven2/net/sf/proguard/proguard-base/6.2.2/proguard-base-6.2.2.jar" lib/proguard.jar 2>/dev/null && PG="lib/proguard.jar" || true
    fi
    if [ -n "$PG" ] && [ -n "$JAVA_BIN" ]; then
        echo "[build] running ProGuard (-microedition -target 1.3)"
        rm -rf "$BUILD/obf"; mkdir -p "$BUILD/obf"
        # ProGuard may need its dependencies on the classpath
        "$JAVA_BIN" -Xmx512m -cp "$PG:$(dirname "$PG")/*" proguard.ProGuard \
            @proguard.pro \
            -injars "$CLASSES" \
            -outjars "$BUILD/obf/" \
            -libraryjars "$STUBS" \
            -dump "$BUILD/proguard-dump.txt" 2>&1 | tail -20
        PACK_DIR="$BUILD/obf"
    else
        echo "[build] ProGuard not available, using built-in CLDC downgrader"
        USE_PROGUARD=0
    fi
fi

if [ "$USE_PROGUARD" = "0" ] || [ ! -d "$PACK_DIR" ]; then
    "$PY" tools/j2me_pack.py downgrade --classes "$CLASSES"
    PACK_DIR="$CLASSES"
fi

# ---------------------------------------------------------------------------
#  5. package + verify
# ---------------------------------------------------------------------------
echo "[build] packaging $OUT_JAR"
"$PY" tools/j2me_pack.py pack --classes "$PACK_DIR" --res res --out "$OUT_JAR"
"$PY" tools/j2me_pack.py jad "$OUT_JAR"
"$PY" tools/j2me_pack.py verify "$OUT_JAR"

echo "[build] done: $(pwd)/$OUT_JAR"
