#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
LIB="$ROOT/lib"
mkdir -p "$LIB"

fetch() {
    path=$1
    output=$2
    url="https://raw.githubusercontent.com/Enough-Software/j2mepolish/master/$path"
    echo "Fetching $output"
    if ! curl -fL --retry 3 "$url" -o "$LIB/$output"; then
        if command -v gh >/dev/null 2>&1; then
            echo "Raw download failed; trying the GitHub API through gh."
            gh api "repos/Enough-Software/j2mepolish/contents/$path" --jq '.content' \
                | base64 -d > "$LIB/$output"
        else
            return 1
        fi
    fi
}

fetch "enough-polish-build/import/cldc-1.1.jar" "cldc-1.1.jar"
fetch "enough-polish-build/import/midp-2.0.jar" "midp-2.0.jar"
fetch "enough-polish-build/lib/proguard.jar" "proguard.jar"

echo "Compile-only dependencies are in $LIB"
