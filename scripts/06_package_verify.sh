#!/usr/bin/env bash
set -e

echo "=== Stage 6: Package Verification & Static Analysis ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

OUT_DIR="${OUT_DIR:-$REPO_ROOT/out/target/product/zerofltexx}"
ZIP_FILE=$(find "$OUT_DIR" -name "lineage-*.zip" 2>/dev/null | head -n 1)

if [ -f "$ZIP_FILE" ]; then
    echo "ROM zip found: $ZIP_FILE"
    echo "Size: $(du -h "$ZIP_FILE" | cut -f1)"
    echo "SHA-256: $(sha256sum "$ZIP_FILE" | cut -d' ' -f1)"

    # Static check of updater-script or metadata assertions
    if unzip -l "$ZIP_FILE" | grep -q "updater-script"; then
        echo "Installer updater-script present."
    fi
else
    echo "Notice: No output zip file found in $OUT_DIR."
    echo "Running static verification on local device tree assertions..."
    python3 "$REPO_ROOT/tools/rom_verifier.py" inspect "$REPO_ROOT/device/samsung/zerofltexx"
fi

echo "=== Package verification script complete ==="
