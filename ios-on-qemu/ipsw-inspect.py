#!/usr/bin/env python3
"""
ipsw-inspect.py - tell the truth about an IPSW before you spend an evening on it.

Standard library only (zipfile + plistlib), so it runs anywhere Python 3 does,
including inside a sandbox with no network. It never uploads or transmits
anything: the file is read locally.

What it does
------------
  * reads BuildManifest.plist  -> ProductVersion / ProductBuildVersion /
    SupportedDeviceModels / BuildTrain
  * classifies the archive members into the buckets every QEMU-iOS path needs
    (kernelcache, DeviceTree, restore ramdisk, rootfs dmg, sep-firmware,
    trustcache, boot chain)
  * given --need PATH, reports exactly which files that path wants, whether
    they are present, and their sizes

What it deliberately does not do
--------------------------------
  * no decryption, no key database. Keys are per-device secrets, redistributing
    them is not this project's business; use `ipsw` (blacktop) or the upstream
    bootstrap scripts for that step.
  * no firmware is bundled, referenced by URL, or downloaded.

Exit status: 0 = the archive is a readable IPSW. 2 = the requested path's files
are incomplete. 3 = the file is not an IPSW at all.
"""

import argparse
import fnmatch
import json
import os
import plistlib
import re
import sys
import zipfile

# --------------------------------------------------------------------------
# SoC / device model table. This is the part that answers "can I run iOS N?"
# --------------------------------------------------------------------------

# device model identifier -> (marketing name, soc, has_qemu_machine_model)
DEVICES = {
    "iPhone1,1": ("iPhone 2G", "S5L8900", True),
    "iPhone1,2": ("iPhone 3G", "S5L8900", True),
    "iPhone2,1": ("iPhone 3GS", "S5L8920", True),
    "iPhone3,1": ("iPhone 4 GSM", "A4 / S5L8930", True),
    "iPhone3,2": ("iPhone 4 GSM (rev)", "A4 / S5L8930", True),
    "iPhone3,3": ("iPhone 4 CDMA", "A4 / S5L8930", True),
    "iPhone4,1": ("iPhone 4s", "A5 / S5L8940x", False),
    "iPhone5,1": ("iPhone 5 GSM", "A6 / S5l8950x", False),
    "iPhone5,2": ("iPhone 5 GSM+CDMA", "A6 / s5l8950x", False),
    "iPhone5,3": ("iPhone 5c GSM", "A6 / s5l8950x", False),
    "iPhone5,4": ("iPhone 5c GSM+CDMA", "A6 / s5l8950x", False),
    "iPhone6,1": ("iPhone 5s", "A7 / s5l8960x", False),
    "iPhone6,2": ("iPhone 5s", "A7 / s5l8960x", False),
    "iPhone7,2": ("iPhone 6", "A8 / t7000", False),
    "iPhone7,1": ("iPhone 6 Plus", "A8 / t7000", False),
    "iPhone8,1": ("iPhone 6s", "A9 / s8000", True),   # console only
    "iPhone8,2": ("iPhone 6s Plus", "A9 / s8000", True),  # console only (xnu-qemu-arm64)
    "iPhone8,4": ("iPhone SE (1st gen)", "A9 / s8003", False),
    "iPhone10,3": ("iPhone X", "A11 / t8015", False),   # ios-qemu uses a rebuilt XNU, not this SoC
    "iPhone12,1": ("iPhone 11", "A13 / t8030", True),   # Inferno, full GUI
    "iPod1,1": ("iPod touch 1G", "S5L8900", True),
    "iPod2,1": ("iPod touch 2G", "S5L8720", True),
    "iPod3,1": ("iPod touch 3G", "S5L8922", True),
    "iPod4,1": ("iPod touch 4G", "A4 / S5L8930", True),
    "iPod5,1": ("iPod touch 5G", "A5 / s5l8942x", False),
    "iPad1,1": ("iPad 1", "A4 / S5L8930", True),
    "iPad2,1": ("iPad 2 Wi-Fi", "A5 / s5l8940x", False),
    "iPad2,2": ("iPad 2 GSM", "A5 / s5l8940x", False),
    "iPad2,3": ("iPad 2 CDMA", "A5 / s5l8940x", False),
}

# Which QEMU path (if any) is documented to work for a device model.
# value: list of path keys from PATHS below
DEVICE_PATHS = {
    "iPod1,1": ["ipod-touch-1g"],
    "iPod2,1": ["ipod-touch-2g"],
    "iPhone2,1": ["iphone-3gs"],
    "iPod3,1": ["ipod-touch-3g"],
    "iPhone3,1": ["iphone-4"],
    "iPhone3,3": ["iphone-4"],
    "iPad1,1": ["ipad-1"],
    "iPod4,1": ["ipod-touch-4g"],
    "iPhone8,2": ["ios12-shell"],
    "iPhone12,1": ["iphone11-ios14"],
}

# --------------------------------------------------------------------------
# Member classification
# --------------------------------------------------------------------------

CLASSIFIERS = [
    ("kernelcache", lambda p: re.search(r"(?i)(^|/)kernelcache\.", p) is not None),
    ("devicetree", lambda p: re.search(r"(?i)DeviceTree\..*\.im4p$", p) is not None),
    ("trustcache", lambda p: p.lower().endswith(".trustcache")),
    ("sep_firmware", lambda p: re.search(r"(?i)(^|/)sep-firmware\.", p) is not None),
    ("bootrom", lambda p: re.search(r"(?i)(^|/)(bootrom|SecureROM)[\._]", p) is not None),
    ("boot_chain", lambda p: re.search(r"(?i)(^|/)(iBoot|iBSS|iBEC|LLB)\.", p) is not None),
    ("rootfs_dmg", lambda p: p.lower().endswith(".dmg") and re.search(r"(?i)(OS|root|main)", p) is not None),
    ("ramdisk_dmg", lambda p: p.lower().endswith(".dmg")),
    ("manifest", lambda p: p.endswith("BuildManifest.plist")),
    ("restore_plist", lambda p: p.endswith("Restore.plist")),
]

# --------------------------------------------------------------------------
# What each QEMU path actually consumes, from the upstream docs
# --------------------------------------------------------------------------

PATHS = {
    "ipod-touch-1g": {
        "title": "iPod touch 1G / iPhoneOS 1.0 - devos50/samhenrigold qemu-ios",
        "gui": "yes (home screen, touch, apps)",
        "host": "Linux (Wayland) or macOS",
        "note": (
            "This path does NOT boot from a raw IPSW. It needs four prepared "
            "artifacts published by the project itself (release n45ap_v1): a "
            "S5L8900 bootrom, a NOR image and a NAND directory holding the root "
            "filesystem. devos50/qemu-ios-generate-nor and -generate-nand "
            "describe how to build them from the stock IPSW."
        ),
        "wants": [],  # nothing is taken straight out of the IPSW
    },
    "ipod-touch-2g": {"title": "iPod touch 2G (n72ap) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
                      "note": "Release n72ap_v1: bootrom + NOR + NAND.", "wants": []},
    "ipod-touch-3g": {"title": "iPod touch 3G (n18ap, S5L8922) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
                      "note": "Board docs: docs/n18/README.md", "wants": []},
    "iphone-3gs": {"title": "iPhone 3GS (n88ap, S5L8920) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
                   "note": "Board docs: docs/n88/README.md", "wants": []},
    "iphone-4": {"title": "iPhone 4 GSM (n90ap, A4/S5L8930) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
                 "note": ("A4 is the ceiling of the legacy boards. Verified firmware for these boards is "
                          "iPhoneOS 1.x-4.x (the fork's day-to-day image is iOS 3.1.3; iPad 1 has an "
                          "ios4.md). iOS 7.1.2 on n90ap is not documented as working anywhere."),
                 "wants": []},
    "ipad-1": {"title": "iPad 1 (k48ap, A4/S5L8930) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
               "note": "Board docs: docs/ipad1/README.md (includes ios4.md).", "wants": []},
    "ipod-touch-4g": {"title": "iPod touch 4G (n81ap, A4) - qemu-ios", "gui": "yes", "host": "Linux/macOS",
                      "note": "Board docs: docs/n81/README.md", "wants": []},
    "ios12-shell": {
        "title": "iOS 12.1 / iPhone 6s Plus (n66, A9) - alephsecurity xnu-qemu-arm64",
        "gui": "NO - text console / root bash only. No home screen is possible with this fork.",
        "host": "Linux (with the ios-emu-linux socket patches) or macOS",
        "note": "Needs -m 6G, one emulated CPU, and two HFS+ disk images you build yourself.",
        "wants": [
            ("kernelcache.release.n66", "kernelcache"),
            ("Firmware/all_flash/DeviceTree.n66ap.im4p", "devicetree"),
            ("*.dmg", "ramdisk_dmg"),
            ("*.dmg", "rootfs_dmg"),
        ],
    },
    "iphone11-ios14": {
        "title": "iOS 14.0 beta 5 (18A5351d) / iPhone 11 (iPhone12,1, t8030) - ChefKiss Inferno",
        "gui": "yes (full modern home screen, Settings, Safari, Photos)",
        "host": "Linux x86_64 (8+ threads, 16-32 GB RAM, ~25 GB disk)",
        "note": ("Only iOS 14.0 beta 5 (18A5351d) for iPhone12,1 is supported. Setup automation "
                 "scripts are explicitly not published upstream, so this tool only checks that the "
                 "IPSW you have is the right one - follow chefkiss.dev/applehax/inferno/ by hand."),
        "wants": [
            ("kernelcache*", "kernelcache"),
            ("Firmware/all_flash/DeviceTree.n104ap.im4p", "devicetree"),
            ("*.dmg", "ramdisk_dmg"),
            ("*.dmg.trustcache", "trustcache"),
            ("sep-firmware*", "sep_firmware"),
        ],
    },
    "iphone-x-ios16": {
        "title": "iOS 16.7.16 (20H392) / iPhone X (iPhone10,3) - lgrachov/ios-qemu",
        "gui": "yes (lock screen + home screen, stock apps, Safari browses the web)",
        "host": "Apple Silicon Mac with hvf, ~40 GB disk",
        "note": ("The kernel is NOT taken from the IPSW - it is rebuilt from Apple's open-source "
                 "xnu-8796 on GitHub Actions. From the IPSW you need the three disk images plus "
                 "the device tree that MobileGestalt reads."),
        "wants": [
            ("*OS.dmg", "rootfs_dmg"),
            ("Firmware/all_flash/DeviceTree.d22ap.im4p", "devicetree"),
        ],
    },
}


def human(n):
    for unit in ("B", "KB", "MB", "GB", "TB"):
        if n < 1024 or unit == "TB":
            return "%.1f %s" % (n, unit) if unit != "B" else "%d B" % n
        n /= 1024.0


def classify(path):
    for name, test in CLASSIFIERS:
        if test(path):
            return name
    return None


def inspect(ipsw_path):
    """Return a dict describing the archive. Raises if it is not a zip."""
    result = {
        "path": os.path.abspath(ipsw_path),
        "size": os.path.getsize(ipsw_path),
        "members": 0,
        "buckets": {},
        "interesting": [],
        "manifest": {},
    }
    with zipfile.ZipFile(ipsw_path) as zf:
        infos = zf.infolist()
        result["members"] = len(infos)
        for info in infos:
            if info.is_dir():
                continue
            bucket = classify(info.filename)
            if bucket:
                result["buckets"].setdefault(bucket, []).append(
                    {"path": info.filename, "size": info.file_size}
                )
                result["interesting"].append(
                    {"path": info.filename, "bucket": bucket, "size": info.file_size}
                )
            if info.filename.endswith("BuildManifest.plist"):
                try:
                    with zf.open(info) as fh:
                        result["manifest"] = parse_manifest(fh.read())
                except Exception as exc:  # noqa: BLE001 - report, don't crash
                    result["manifest"] = {"error": str(exc)}
    result["interesting"].sort(key=lambda e: e["path"])
    return result


def parse_manifest(raw):
    """BuildManifest.plist -> the few fields that decide whether a path applies."""
    try:
        pl = plistlib.loads(raw)
    except Exception as exc:  # noqa: BLE001
        return {"error": "unparseable plist: %s" % exc}
    out = {
        "ProductVersion": pl.get("ProductVersion"),
        "ProductBuildVersion": pl.get("ProductBuildVersion"),
        "BuildTrain": pl.get("BuildTrain"),
        "SupportedDeviceModels": pl.get("SupportedDeviceModels") or [],
        "SupportedDevices": pl.get("SupportedDevices") or [],
    }
    return out


def verdict(info):
    """Human-readable judgement: which paths this IPSW can feed."""
    models = list(info["manifest"].get("SupportedDeviceModels") or [])
    lines = []
    usable = []
    blocked = []
    for m in models:
        dev = DEVICES.get(m)
        if dev is None:
            lines.append("  ?  %-12s unknown model identifier" % m)
            continue
        name, soc, has_model = dev
        paths = DEVICE_PATHS.get(m, [])
        mark = "OK" if paths else ("--" if not has_model else "??")
        lines.append("  %s %-12s %-22s %-16s %s" % (mark, m, name, soc, ",".join(paths) or "no documented path"))
        if paths:
            usable.extend(paths)
        elif not has_model:
            blocked.append((m, name, soc))
    return lines, sorted(set(usable)), blocked


def need(path_key, info):
    """Check the concrete files a path wants against the archive."""
    spec = PATHS[path_key]
    names = set()
    for pattern, bucket in spec["wants"]:
        for entry in info["buckets"].get(bucket, []):
            if fnmatch.fnmatch(entry["path"], pattern) or fnmatch.fnmatch(
                os.path.basename(entry["path"]), pattern
            ):
                names.add((entry["path"], entry["size"]))
    missing = []
    for pattern, bucket in spec["wants"]:
        if not any(fnmatch.fnmatch(p, pattern) or fnmatch.fnmatch(os.path.basename(p), pattern)
                   for p, _ in names):
            missing.append("%s (%s)" % (pattern, bucket))
    return spec, sorted(names), missing


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("ipsw", help="path to an .ipsw (or any zip) you already downloaded from Apple")
    ap.add_argument("--need", metavar="PATH", help="check one path: " + ", ".join(sorted(PATHS)))
    ap.add_argument("--json", action="store_true", help="machine-readable output")
    ap.add_argument("--paths", action="store_true", help="list the known paths and exit")
    args = ap.parse_args(argv)

    if args.paths:
        for key in sorted(PATHS):
            spec = PATHS[key]
            print("%-18s %s" % (key, spec["title"]))
            print("%-18s   GUI: %s" % ("", spec["gui"]))
            print("%-18s   host: %s" % ("", spec["host"]))
        return 0

    if not os.path.exists(args.ipsw):
        print("no such file: %s" % args.ipsw, file=sys.stderr)
        return 3
    try:
        info = inspect(args.ipsw)
    except zipfile.BadZipFile:
        print("not a zip/IPSW: %s" % args.ipsw, file=sys.stderr)
        return 3

    if args.json:
        print(json.dumps(info, indent=2, sort_keys=True))
        return 0

    m = info["manifest"]
    print("file            : %s (%s)" % (info["path"], human(info["size"])))
    print("members         : %d" % info["members"])
    if m.get("error"):
        print("BuildManifest   : %s" % m["error"])
    else:
        print("ProductVersion  : %s" % m.get("ProductVersion"))
        print("BuildVersion    : %s" % m.get("ProductBuildVersion"))
        print("BuildTrain      : %s" % m.get("BuildTrain"))
        print("device models   : %s" % (", ".join(m.get("SupportedDeviceModels") or []) or "(none listed)"))

    print("\nclassified members:")
    for bucket in sorted(info["buckets"]):
        entries = info["buckets"][bucket]
        total = sum(e["size"] for e in entries)
        print("  %-14s %4d file(s)  %10s" % (bucket, len(entries), human(total)))
        for e in entries[:4]:
            print("      %-62s %9s" % (e["path"], human(e["size"])))
        if len(entries) > 4:
            print("      ... and %d more" % (len(entries) - 4))

    lines, usable, blocked = verdict(info)
    print("\nverdict:")
    for line in lines:
        print(line)
    if usable:
        print("\n  usable paths: %s" % ", ".join(usable))
    if blocked:
        print("\n  NO QEMU MACHINE MODEL EXISTS for:")
        for model, name, soc in blocked:
            print("      %s (%s, %s)" % (model, name, soc))
        print("  An IPSW for one of these cannot be booted by anyone today -")
        print("  somebody would first have to reverse-engineer that SoC into QEMU.")
    if not usable and not blocked:
        print("\n  no documented QEMU path for this device model.")

    if args.need:
        if args.need not in PATHS:
            print("\nunknown path %r; try --paths" % args.need, file=sys.stderr)
            return 3
        spec, found, missing = need(args.need, info)
        print("\n=== %s ===" % args.need)
        print("%s" % spec["title"])
        print("GUI : %s" % spec["gui"])
        print("host: %s" % spec["host"])
        print("note: %s" % spec["note"])
        if found:
            print("found:")
            for path, size in found:
                print("   %-62s %9s" % (path, human(size)))
        if missing:
            print("MISSING: %s" % ", ".join(missing))
            return 2
        if not spec["wants"]:
            print("(this path takes nothing directly from the IPSW - see note)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
