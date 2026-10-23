#!/bin/sh
# Desktop test-suite: compiles the platform independent emulator core with a
# java.io backed JSR-75 shim and runs the CPU/GPU/GTE/SPU and disc boot tests.
set -e
cd "$(dirname "$0")/.."

ECJ="${ECJ_JAR:-lib/ecj.jar}"
JAVA="${JAVA_HOME:+$JAVA_HOME/bin/}java"
OUT=build/tests

rm -rf "$OUT"; mkdir -p "$OUT"

CORE="src/psx/Bus.java src/psx/CodeBlock.java src/psx/CodeCache.java src/psx/Cpu.java \
src/psx/Dma.java src/psx/Gpu.java src/psx/Gte.java src/psx/Irq.java src/psx/Mdec.java \
src/psx/Pad.java src/psx/Timers.java src/psx/Spu.java src/psx/Cdrom.java \
src/psx/VirtualCD.java src/psx/Iso.java src/psx/PS1Machine.java tests/jsr75/*.java"

if [ -f "$ECJ" ]; then
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -d "$OUT" $CORE
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -d "$OUT" -cp "$OUT" tests/CoreTest.java tests/HleBootTest.java
else
    javac -nowarn -d "$OUT" -cp "$OUT" $CORE
    javac -nowarn -d "$OUT" -cp "$OUT" tests/CoreTest.java tests/HleBootTest.java
fi

"$JAVA" -cp "$OUT" CoreTest
"$JAVA" -cp "$OUT" HleBootTest
