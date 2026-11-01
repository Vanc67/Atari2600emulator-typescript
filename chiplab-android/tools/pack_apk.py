#!/usr/bin/env python3
# =============================================================================
#  ChipLab - pack the unsigned APK: manifest + classes.dex + assets.
#  Usage: python3 tools/pack_apk.py <manifest> <classes.dex> <assets-dir> <out.apk>
# =============================================================================
import os
import sys
import zipfile

FIXED_DATE = (2024, 1, 1, 0, 0, 0)


def main():
    manifest, dex, assets_dir, out_path = sys.argv[1:5]
    zf = zipfile.ZipFile(out_path, "w", zipfile.ZIP_DEFLATED)

    def add(src, arc):
        data = open(src, "rb").read()
        info = zipfile.ZipInfo(arc, FIXED_DATE)
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        zf.writestr(info, data)
        print("[apk] %-28s %7d bytes" % (arc, len(data)))

    add(manifest, "AndroidManifest.xml")
    add(dex, "classes.dex")
    for name in sorted(os.listdir(assets_dir)):
        path = os.path.join(assets_dir, name)
        if os.path.isfile(path):
            add(path, "assets/" + name)
    zf.close()
    print("[apk] wrote %s (%d bytes)" % (out_path, os.path.getsize(out_path)))


if __name__ == "__main__":
    main()
