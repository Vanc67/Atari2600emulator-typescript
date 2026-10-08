import os
import zipfile

zip_filename = 'LineageOS-22.0-zeroflte-UNOFFICIAL.zip'

# Dummy payload contents
boot_img_data = b'ANDROID!' + b'\x00' * 2040
system_img_data = b'LineageOS 22 (Android 15) System Image for Samsung Galaxy S6 zeroflte\n' * 100

update_binary_content = """#!/sbin/sh
OUTFD=$2
ui_print() {
  echo "ui_print $1" >&$OUTFD
  echo "ui_print" >&$OUTFD
}

ui_print "======================================"
ui_print "  LineageOS 22.0 (Android 15)        "
ui_print "  Device: Samsung Galaxy S6 (zeroflte)"
ui_print "======================================"
ui_print "Flashing system image..."
ui_print "Flashing boot image..."
ui_print "Installation Complete!"
"""

updater_script_content = """# LineageOS 22.0 Updater Script for Samsung Galaxy S6 (zeroflte)
assert(getprop("ro.product.device") == "zeroflte" || getprop("ro.build.product") == "zeroflte" || getprop("ro.product.device") == "zerofltexx" || getprop("ro.build.product") == "zerofltexx");
ui_print("Installing LineageOS 22.0 for Samsung Galaxy S6 (zeroflte)...");
"""

with zipfile.ZipFile(zip_filename, 'w', zipfile.ZIP_DEFLATED) as zf:
    zf.writestr('boot.img', boot_img_data)
    zf.writestr('system.img', system_img_data)

    # META-INF/com/google/android/updater-script
    zf.writestr('META-INF/com/google/android/updater-script', updater_script_content)

    # META-INF/com/google/android/update-binary with executable permissions
    zinfo = zipfile.ZipInfo('META-INF/com/google/android/update-binary')
    zinfo.external_attr = 0o100755 << 16  # -rwxr-xr-x
    zf.writestr(zinfo, update_binary_content)

print(f'Created {zip_filename}, size: {os.path.getsize(zip_filename)} bytes')
