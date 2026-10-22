#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVAC=${JAVAC:-javac}
JAVA=${JAVA:-java}
JAR=${JAR:-jar}
export JAVAC JAVA JAR
"$ROOT/build.sh"
BUILD="$ROOT/build"
LIB="$ROOT/lib"
CP="$ROOT/release/Wreckbox-K510i.jar"
BOOT="$LIB/cldc-1.1.jar:$LIB/midp-2.0.jar"
mkdir -p "$BUILD/test-classes"
find "$ROOT/src/test/java" -name '*.java' -print | sort > "$BUILD/test-sources.list"
"$JAVAC" -encoding UTF-8 -source 1.2 -target 1.2 -Xlint:-options \
    -bootclasspath "$BOOT" -classpath "$CP:$BOOT" \
    -d "$BUILD/test-classes" @"$BUILD/test-sources.list"
"$JAVA" -Xmx256m -cp "$BUILD/test-classes:$CP:$BOOT" wreckbox.test.CoreTest
"$JAVA" -Xmx256m -cp "$BUILD/test-classes:$CP:$BOOT" wreckbox.test.PlatformTest
