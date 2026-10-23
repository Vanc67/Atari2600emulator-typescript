#!/bin/sh
# Root convenience wrapper: builds the Java ME PlayStation 1 emulator.
# The real build script (compilation, ProGuard, jar packing, verification)
# lives in ps1j2me/build.sh - see ps1j2me/README.md.
exec "$(dirname "$0")/ps1j2me/build.sh" "$@"
