#!/bin/sh
# =============================================================================
#  Retro PC Builder - one-shot toolchain setup
#
#  The game is compiled for CLDC 1.1 / MIDP 2.0 with Java 1.3 bytecode. Any
#  javac 8 can do that, but this sandbox has no JDK at all, so we fetch two
#  portable pieces instead of installing a distribution:
#
#    * a JRE  ... python wheel "jdk4py" (Temurin runtime, no compiler)
#    * javac ... npm package "dataslope-tools-jar" (JDK 8 tools.jar, the
#                compiler itself; it runs fine on a modern JVM)
#
#  Both end up in .toolchain/ which is git-ignored.
#
#  If you have a real JDK:  JAVA_HOME=/path/to/jdk8 TOOLS_JAR=/path/to/tools.jar
#  If you have ecj.jar:     it can be used with
#                           java -cp ecj.jar org.eclipse.jdt.internal.compiler.batch.Main
# =============================================================================
set -e

cd "$(dirname "$0")/.."

TC=".toolchain"
mkdir -p "$TC"

if [ -x "$TC/jre/bin/java" ] && [ -f "$TC/tools.jar" ]; then
    echo "[toolchain] already present in $TC/"
    exit 0
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

PY=python3
command -v python3 >/dev/null 2>&1 || PY=python

echo "[toolchain] fetching JRE (jdk4py)"
$PY -m pip download --no-deps --no-cache-dir jdk4py -d "$TMP/jre" >/dev/null
WHEEL=$(ls "$TMP"/jre/*.whl 2>/dev/null | head -1)
if [ -z "$WHEEL" ]; then
    echo "[toolchain] could not download jdk4py" >&2
    exit 1
fi
( cd "$TMP" && unzip -q "$WHEEL" -d jre-x )
JRE_BIN=$(dirname "$(find "$TMP/jre-x" -type f -name java -path '*/bin/*' | head -1)")
if [ -z "$JRE_BIN" ]; then
    echo "[toolchain] java executable not found in the wheel" >&2
    exit 1
fi
rm -rf "$TC/jre"
cp -r "$(dirname "$JRE_BIN")" "$TC/jre"

echo "[toolchain] fetching javac (JDK 8 tools.jar from npm)"
( cd "$TMP" && npm pack dataslope-tools-jar >/dev/null 2>&1 )
TGZ=$(ls "$TMP"/*.tgz 2>/dev/null | head -1)
if [ -z "$TGZ" ]; then
    echo "[toolchain] could not download dataslope-tools-jar" >&2
    exit 1
fi
( cd "$TMP" && tar xzf "$TGZ" )
cp "$TMP/package/tools.jar" "$TC/tools.jar"

echo "[toolchain] done:"
"$TC/jre/bin/java" -version 2>&1 | head -1
"$TC/jre/bin/java" -cp "$TC/tools.jar" com.sun.tools.javac.Main -version
echo "[toolchain] now run ./build.sh"
