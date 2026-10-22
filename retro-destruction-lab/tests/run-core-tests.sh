#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT="$ROOT/build/test-classes"
JAVAC=${JAVAC_TEST:-javac}
JAVA=${JAVA_TEST:-java}
rm -rf "$OUT"
mkdir -p "$OUT"
"$JAVAC" -d "$OUT" "$ROOT/src/rdl/core/World.java" "$ROOT/tests/rdl/core/WorldTest.java"
"$JAVA" -cp "$OUT" rdl.core.WorldTest
