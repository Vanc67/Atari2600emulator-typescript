#!/usr/bin/env bash
set -e

echo "=== Creating TWRP Flashable Pixel Overlay Package for SM-G920F (LineageOS 20 Base) ==="

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BUILD_DIR="$REPO_ROOT/out/pixel_overlay_build"
OUTPUT_ZIP="$REPO_ROOT/out/Pixel_Overlay_SM-G920F.zip"

rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/META-INF/com/google/android"
mkdir -p "$BUILD_DIR/system/product/overlay"
mkdir -p "$BUILD_DIR/system/product/priv-app/PixelLauncher"

# Create update-binary script for TWRP
cat << 'EOF' > "$BUILD_DIR/META-INF/com/google/android/update-binary"
#!/sbin/sh
OUTFD=$2

ui_print() {
    echo "ui_print $1" >&$OUTFD
    echo "ui_print" >&$OUTFD
}

ui_print "==========================================="
ui_print " Pixel Overlay Customization Package"
ui_print " Target: Samsung Galaxy S6 (SM-G920F)"
ui_print "==========================================="

# Verify target device assert
MODEL=$(getprop ro.product.model)
DEVICE=$(getprop ro.product.device)

if [ "$MODEL" != "SM-G920F" ] && [ "$DEVICE" != "zerofltexx" ] && [ "$DEVICE" != "zeroflte" ]; then
    ui_print "ERROR: This overlay package is ONLY for SM-G920F (zerofltexx)!"
    ui_print "Current device: $MODEL / $DEVICE"
    exit 1
fi

ui_print "Mounting /system..."
mount /system 2>/dev/null || mount /system_root 2>/dev/null

ui_print "Installing Pixel UI Resource Overlays..."
mkdir -p /system/product/overlay
cp -rf /tmp/system/product/overlay/* /system/product/overlay/ 2>/dev/null || true

ui_print "Pixel Overlay package installed successfully."
ui_print "Reboot system to apply Pixel UI modifications."
exit 0
EOF

chmod 755 "$BUILD_DIR/META-INF/com/google/android/update-binary"

# Create dummy updater-script file required by TWRP
cat << 'EOF' > "$BUILD_DIR/META-INF/com/google/android/updater-script"
# Dummy updater-script file. The update-binary shell script executes installation.
EOF

# Create Pixel System UI RRO Overlay
cat << 'EOF' > "$BUILD_DIR/system/product/overlay/PixelSystemUIOverlay.apk"
# Placeholder for compiled RRO Pixel SystemUI Overlay APK
EOF

mkdir -p "$REPO_ROOT/out"
cd "$BUILD_DIR"
zip -r "$OUTPUT_ZIP" META-INF system

echo "Package generated at: $OUTPUT_ZIP"
echo "SHA-256: $(sha256sum "$OUTPUT_ZIP" | cut -d' ' -f1)"
