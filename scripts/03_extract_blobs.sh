#!/usr/bin/env bash
set -e

echo "=== Stage 3: Extracting Samsung Exynos 7420 Vendor Blobs ==="

DEVICE_DIR="device/samsung/zerofltexx"
VENDOR_DIR="vendor/samsung/zerofltexx"

if [ ! -d "$DEVICE_DIR" ]; then
    echo "Error: Device directory $DEVICE_DIR not found!"
    exit 1
fi

echo "Extracting proprietary binaries for Samsung Galaxy S6 (zerofltexx)..."
echo "Note: Blobs must be extracted legally from an official stock G920F Nougat dump or connected device."

if [ -f "$DEVICE_DIR/extract-files.sh" ]; then
    bash "$DEVICE_DIR/extract-files.sh" "$@"
else
    echo "Notice: extract-files.sh not present in device tree. Using pre-populated vendor repository at $VENDOR_DIR if available."
    if [ -d "vendor/samsung" ]; then
        echo "Vendor binaries located in vendor/samsung."
    else
        echo "Warning: Vendor repository missing. Pull proprietary vendor tree from source manifest."
    fi
fi

echo "=== Vendor blob check complete ==="
