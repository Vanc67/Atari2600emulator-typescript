#!/bin/bash
# Proves that the DEX in the APK behaves like the Java it was generated from.
#
# There is no device and no Android emulator on the build host, so correctness
# is established three ways:
#
#   1. dexcheck.py   - structural pass over build/classes.dex: every
#                      instruction decodes and every branch, switch target and
#                      exception handler lands on an instruction boundary.
#   2. selftest/Probe.java - language features the translator has to support
#                      (tableswitch/lookupswitch, arrays, exceptions, long
#                      arithmetic, interfaces, string building).  Run on the
#                      JVM and on the generated DEX; the output must match.
#   3. selftest/CoreRun.java - the real emulator core: boot a demo disc and
#                      single step the MIPS CPU, printing a fingerprint of the
#                      machine state.  Again JVM vs DEX, byte for byte.
#
# Everything it needs is in the repository plus the toolchain cache
# (tools/setup_toolchain.sh).
set -eu
cd "$(dirname "$0")/.."

JAVA_HOME=${JAVA_HOME:-$HOME/.cache/jdk/jdk11}
ECJ_JAR=${ECJ_JAR:-$HOME/.cache/ecj/ecj.jar}
CACHE=${PSX_APK_CACHE:-$HOME/.cache/psx-apk}
JAVA="$JAVA_HOME/bin/java"
DEX=tools/dex
OUT=build/verify

[ -f "$CACHE/apktool.jar" ] || tools/setup_toolchain.sh >/dev/null

run_case() {
    name=$1; shift
    echo "[verify] $name"
}

# ---------------------------------------------------------------- structure --
if [ -f build/classes.dex ]; then
    python3 $DEX/dexcheck.py build/classes.dex
    run_case "dex structure"
else
    echo "[verify] build/classes.dex missing - run ./build.sh first" >&2
    exit 1
fi

# ------------------------------------------------------------- probe (lang) --
mkdir -p $OUT/probe
"$JAVA" -jar "$ECJ_JAR" -source 1.6 -target 1.6 -nowarn -proc:none -g:none \
    -d $OUT/probe/classes $DEX/selftest/Probe.java
"$JAVA" -cp $OUT/probe/classes Probe > $OUT/probe/jvm.txt
python3 $DEX/jvm2smali.py $OUT/probe/classes $OUT/probe/smali
"$JAVA" -cp "$CACHE:$CACHE/apktool.jar" Smali2Dex \
    $OUT/probe/smali $OUT/probe/classes.dex 10
python3 $DEX/dvmlite.py $OUT/probe/classes.dex 'LProbe;' main > $OUT/probe/dvm.txt
if diff -q $OUT/probe/jvm.txt $OUT/probe/dvm.txt >/dev/null; then
    echo "[verify] probe: identical ($(wc -l < $OUT/probe/jvm.txt) lines)"
else
    echo "[verify] probe: MISMATCH" >&2
    diff $OUT/probe/jvm.txt $OUT/probe/dvm.txt | head -20 >&2
    exit 1
fi

# ------------------------------------------------------- core (real machine) --
# The Dalvik interpreter is a few hundred times slower than the JVM, so the
# core runs a bounded number of MIPS instructions instead of whole frames.
STEPS=${PSX_VERIFY_STEPS:-40000}
IMAGES=${PSX_VERIFY_IMAGES:-}      # empty = all seven demo discs
mkdir -p $OUT/core
find src/psx -maxdepth 1 -name '*.java' > $OUT/core/sources.txt
echo $DEX/selftest/CoreRun.java >> $OUT/core/sources.txt
"$JAVA" -jar "$ECJ_JAR" -source 1.6 -target 1.6 -nowarn -proc:none -g:none \
    -d $OUT/core/classes @$OUT/core/sources.txt
"$JAVA" -cp $OUT/core/classes CoreRun $STEPS $STEPS $IMAGES > $OUT/core/jvm.txt
python3 $DEX/jvm2smali.py $OUT/core/classes $OUT/core/smali
"$JAVA" -cp "$CACHE:$CACHE/apktool.jar" Smali2Dex \
    $OUT/core/smali $OUT/core/classes.dex 10
python3 tools/dex/run_core.py $OUT/core/classes.dex $OUT/core/dvm.txt \
    $STEPS $STEPS $IMAGES >/dev/null
if diff -q $OUT/core/jvm.txt $OUT/core/dvm.txt >/dev/null; then
    echo "[verify] core: identical ($(wc -l < $OUT/core/jvm.txt) lines)"
else
    echo "[verify] core: MISMATCH" >&2
    diff $OUT/core/jvm.txt $OUT/core/dvm.txt | head -20 >&2
    exit 1
fi

echo "[verify] ok"
