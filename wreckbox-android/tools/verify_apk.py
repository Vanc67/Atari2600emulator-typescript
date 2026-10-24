#!/usr/bin/env python3
"""Static Gingerbread/package checks; this is not a device/emulator test."""
import hashlib
import pathlib
import struct
import sys
import zipfile

apk_path, badging_path, resource_root = map(pathlib.Path, sys.argv[1:])
badging = badging_path.read_text(encoding="utf-8", errors="replace")
required_badging = [
    "package: name='ru.vanc67.wreckbox' versionCode='18' versionName='1.8'",
    "minSdkVersion:'10'", "targetSdkVersion:'10'",
    "launchable-activity: name='ru.vanc67.wreckbox.WreckboxActivity'",
]
for text in required_badging:
    if text not in badging:
        raise SystemExit(f"missing badging: {text}")
if "android.permission.INTERNET" in badging:
    raise SystemExit("unexpected INTERNET permission")

with zipfile.ZipFile(apk_path) as apk:
    corrupt = apk.testzip()
    if corrupt:
        raise SystemExit(f"CRC failure: {corrupt}")
    names = set(apk.namelist())
    for name in ["AndroidManifest.xml", "classes.dex", "resources.arsc", "META-INF/MANIFEST.MF"]:
        if name not in names:
            raise SystemExit(f"missing APK entry: {name}")
    if "classes2.dex" in names or any(name.startswith("lib/") for name in names):
        raise SystemExit("unexpected multidex/native dependency")
    dex = apk.read("classes.dex")
    if not dex.startswith(b"dex\n035\0"):
        raise SystemExit(f"DEX is not Gingerbread 035: {dex[:8]!r}")
    file_size, header_size, endian = struct.unpack_from("<III", dex, 32)
    if file_size != len(dex) or header_size != 0x70 or endian != 0x12345678:
        raise SystemExit("invalid DEX header")
    method_count = struct.unpack_from("<I", dex, 88)[0]
    class_count = struct.unpack_from("<I", dex, 96)[0]
    if method_count >= 65536:
        raise SystemExit(f"DEX method reference overflow: {method_count}")
    if b"android/webkit/WebView" in dex:
        raise SystemExit("WebView dependency found")
    if b"ru/vanc67/wreckbox/WreckboxActivity" not in dex:
        raise SystemExit("activity missing from DEX")

    expected = {"maps": 9, "textures": 12, "audio": 54}
    for group, count in expected.items():
        source_files = sorted((resource_root / group).iterdir())
        if group == "maps" and len([f for f in source_files if f.suffix == ".wbmp"]) != 8:
            raise SystemExit("source maps: expected 8 playable WBMP files")
        if len(source_files) != count:
            raise SystemExit(f"source {group}: expected {count}, got {len(source_files)}")
        apk_files = [n for n in names if n.startswith(f"assets/{group}/") and not n.endswith("/")]
        if len(apk_files) != count:
            raise SystemExit(f"APK {group}: expected {count}, got {len(apk_files)}")
        for source in source_files:
            name = f"assets/{group}/{source.name}"
            info = apk.getinfo(name)
            if group == "audio" and info.compress_type != zipfile.ZIP_STORED:
                raise SystemExit(f"AMR must stay uncompressed for AssetManager.openFd: {name}")
            if apk.read(name) != source.read_bytes():
                raise SystemExit(f"asset changed during packaging: {name}")
    signature = [n for n in names if n.startswith("META-INF/") and n.endswith((".SF", ".RSA", ".DSA"))]
    if len(signature) < 2:
        raise SystemExit("v1 JAR signature missing")

sha256 = hashlib.sha256(apk_path.read_bytes()).hexdigest()
print(f"APK STATIC PASS: API 10, DEX 035, {class_count} classes, {method_count} method refs")
print("ASSETS PASS: 8 maps, 12 textures, 54 uncompressed AMR; bytes match source")
print(f"ZIP/SIGNATURE PASS: {apk_path.stat().st_size} bytes, sha256={sha256}")
