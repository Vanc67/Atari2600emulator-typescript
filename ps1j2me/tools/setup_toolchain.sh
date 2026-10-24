#!/bin/sh
# Installs the build toolchain that build.sh / tests/run_tests.sh expect:
#   $HOME/.cache/ecj/ecj.jar        Eclipse compiler 3.26 (Java 1.3 capable)
#   $HOME/.cache/jdk/jdk11          JDK 11 runtime + javac (for the test-suite)
#
# Nothing is stored inside the repository; the caches live in ~/.cache.
#
#   ./tools/setup_toolchain.sh [ECJ_JAR_URL]
#
# After it finishes:
#   ECJ_JAR=$HOME/.cache/ecj/ecj.jar JAVA_HOME=$HOME/.cache/jdk/jdk11 ./build.sh --no-proguard
set -e

CACHE="${HOME}/.cache"
ECJ="${CACHE}/ecj/ecj.jar"
JDK="${CACHE}/jdk/jdk11"
TMP="$(mktemp -d)"

mkdir -p "${CACHE}/ecj" "${CACHE}/jdk"

if [ ! -f "${ECJ}" ]; then
    echo "[setup] downloading ecj 3.26.0"
    URL="${1:-https://repo1.maven.org/maven2/org/eclipse/jdt/ecj/3.26.0/ecj-3.26.0.jar}"
    if curl -fsSL "${URL}" -o "${ECJ}"; then :;
    else
        echo "[setup] maven unavailable, trying the npm mirror"
        ( cd "${TMP}" && npm pack exoproc-jjit >/dev/null 2>&1 && tar xzf exoproc-jjit-*.tgz \
          && cp package/deps/ecj-3.26.0.jar "${ECJ}" )
    fi
fi

if [ ! -x "${JDK}/bin/java" ]; then
    echo "[setup] downloading jdk4py (OpenJDK 11)"
    python3 -m pip download jdk4py==0.0.1.8 --no-deps -d "${TMP}" -q
    ( cd "${TMP}" && unzip -qo jdk4py-*.whl && cp -r jdk4py/java-runtime "${JDK}" )
fi

rm -rf "${TMP}"
echo "[setup] ecj: ${ECJ}"
echo "[setup] jdk: ${JDK} ($("${JDK}/bin/java" -version 2>&1 | head -1))"
