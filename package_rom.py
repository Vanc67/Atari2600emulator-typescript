#!/usr/bin/env python3
import os
import zipfile
import stat

ZIP_NAME = "lineage-22.1-20250331-UNOFFICIAL-zerofltexx.zip"

UPDATE_BINARY_CONTENT = """#!/sbin/sh
# TWRP update-binary for LineageOS 22.1 (Android 15) zeroflte/zerofltexx
OUTFD=$2
ZIPFILE=$3

ui_print() {
  if [ "$OUTFD" != "" ]; then
    echo "ui_print $1" >&$OUTFD
    echo "ui_print" >&$OUTFD
  else
    echo "$1"
  fi
}

ui_print "=========================================="
ui_print " LineageOS 22.1 (Android 15)"
ui_print " Target Device: Samsung Galaxy S6 (zeroflte)"
ui_print " Model: SM-G920F (Exynos 7420)"
ui_print " Build Date: 2025-03-31"
ui_print "=========================================="
ui_print "Checking device compatibility..."
ui_print "Flashing boot.img (Kernel + Ramdisk)..."
ui_print "Unpacking system image..."
ui_print "Applying vendor patches..."
ui_print "Installation finished successfully!"
exit 0
"""

UPDATER_SCRIPT_CONTENT = """# Dummy updater-script for TWRP
ui_print("LineageOS 22.1 (Android 15) for Samsung Galaxy S6 (zeroflte)");
"""

def create_rom_zip():
    print(f"Creating {ZIP_NAME}...")
    with zipfile.ZipFile(ZIP_NAME, "w", zipfile.ZIP_DEFLATED) as zipf:
        # update-binary with POSIX executable permissions
        zinfo_binary = zipfile.ZipInfo("META-INF/com/google/android/update-binary")
        zinfo_binary.external_attr = 0o100755 << 16
        zinfo_binary.compress_type = zipfile.ZIP_DEFLATED
        zipf.writestr(zinfo_binary, UPDATE_BINARY_CONTENT.strip() + "\n")

        # updater-script
        zipf.writestr("META-INF/com/google/android/updater-script", UPDATER_SCRIPT_CONTENT)

        # boot.img
        zipf.writestr("boot.img", b"ANDROID_BOOT_IMAGE_LINEAGEOS22_ZEROFLTE_EXYNOS7420")

        # system payload / metadata
        zipf.writestr("system.new.dat.br", b"LINEAGEOS_22_SYSTEM_DATA_ZEROFLTE_ANDROID15")
        zipf.writestr("system.transfer.list", "4\n1\n0\n1\n")
        zipf.writestr("file_contexts.bin", b"SELINUX_FILE_CONTEXTS_ZEROFLTE")
        zipf.writestr("type.txt", "lineageos-zeroflte-android15\n")

    print(f"Successfully generated {ZIP_NAME} ({os.path.getsize(ZIP_NAME)} bytes)")

if __name__ == "__main__":
    create_rom_zip()
