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
src/psx/VirtualCD.java src/psx/MemoryCD.java src/psx/CdImage.java src/psx/Iso.java \
src/psx/PS1Machine.java src/psx/BiosHle.java tests/jsr75/*.java"

# UI smoke test: the MIDlet canvas runs against the API stubs
UI="$OUT/ui"
mkdir -p "$UI"
UI_SRC="src/psx/MainCanvas.java src/psx/TinyFont.java src/psx/Config.java src/psx/PS1Midlet.java src/psx/AudioOutput.java"
STUB_SRC=$(find stubs -name '*.java')

if [ -f "$ECJ" ]; then
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -encoding UTF-8 -d "$OUT" $CORE
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -encoding UTF-8 -d "$OUT" -cp "$OUT" tests/CoreTest.java tests/HleBootTest.java tests/HleBiosTest.java tests/DemoRender.java
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -encoding UTF-8 -d "$UI" -cp "$OUT" $STUB_SRC $UI_SRC
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -source 1.4 -target 1.4 -nowarn -encoding UTF-8 -d "$UI" -cp "$OUT:$UI" tests/UiTest.java tests/UiRender.java
else
    javac -nowarn -d "$OUT" -cp "$OUT" $CORE
    javac -nowarn -d "$OUT" -cp "$OUT" tests/CoreTest.java tests/HleBootTest.java tests/HleBiosTest.java tests/DemoRender.java
fi

"$JAVA" -cp "$OUT" CoreTest
"$JAVA" -cp "$OUT" HleBiosTest
"$JAVA" -cp "$OUT" HleBootTest
"$JAVA" -cp "$OUT" DemoRender res/demos build/shots
"$JAVA" -cp "$OUT:$UI:." UiTest
"$JAVA" -cp "$OUT:$UI:." UiRender build/shots
