#!/usr/bin/env bash
# Fetch the minimum Android SDK needed by build-apk.sh.
#
# dl.google.com and objects.githubusercontent.com are unreachable from this
# environment, so the usual `sdkmanager` route does not work and GitHub release
# assets fail too. What does work is the contents API against a repository that
# carries the build-tools, requested with the raw media type. That is what this
# script does.
#
# The nine files below are everything the pipeline touches: aapt2 and zipalign
# are native and need libc++.so.1, d8 and apksigner are shell wrappers around
# the two jars, and android.jar is the compile-time stub the class is built
# against. Nothing else in the SDK is used.
set -euo pipefail

REPO="${REPO:-ItzVladik/android-sdk}"
DEST="${1:-$HOME/android-sdk}"

FILES=(
  "build-tools/36.1.0/aapt2"
  "build-tools/36.1.0/apksigner"
  "build-tools/36.1.0/d8"
  "build-tools/36.1.0/zipalign"
  "build-tools/36.1.0/lib/apksigner.jar"
  "build-tools/36.1.0/lib/d8.jar"
  "build-tools/36.1.0/lib64/libc++.so"
  "build-tools/36.1.0/lib64/libc++.so.1"
  "platforms/android-33/android.jar"
)

ok=0; fail=0
for rel in "${FILES[@]}"; do
  out="$DEST/$rel"
  if [[ -s "$out" ]]; then echo "  = $rel (already present)"; ok=$((ok+1)); continue; fi
  mkdir -p "$(dirname "$out")"
  if gh api -H "Accept: application/vnd.github.raw" \
        "repos/$REPO/contents/$rel" > "$out" 2>/dev/null && [[ -s "$out" ]]; then
    echo "  + $rel ($(stat -c%s "$out") bytes)"
    ok=$((ok+1))
  else
    echo "  ! $rel FAILED"; rm -f "$out"; fail=$((fail+1))
  fi
done

chmod +x "$DEST"/build-tools/*/aapt2 "$DEST"/build-tools/*/zipalign \
         "$DEST"/build-tools/*/d8 "$DEST"/build-tools/*/apksigner 2>/dev/null || true

echo "fetch-sdk: ok=$ok fail=$fail -> $DEST"
[[ $fail -eq 0 ]]
