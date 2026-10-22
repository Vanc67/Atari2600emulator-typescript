#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA=${JAVA:-java}
LOW_HEAP=${LOW_HEAP:-2560k}
export JAVA
"$ROOT/test.sh"
LIB="$ROOT/lib"
CP="$ROOT/release/Wreckbox-K510i.jar"
BOOT="$LIB/cldc-1.1.jar:$LIB/midp-2.0.jar"
"$JAVA" "-Xmx$LOW_HEAP" -cp "$ROOT/build/test-classes:$CP:$BOOT" wreckbox.test.LowHeapTest
