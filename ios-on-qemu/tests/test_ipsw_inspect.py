#!/usr/bin/env python3
"""
Tests for ipsw-inspect.py. No Apple files, no network: every fixture is a
synthetic zip built here with plistlib + zipfile. Run directly:

    python3 tests/test_ipsw_inspect.py
"""

import io
import os
import plistlib
import sys
import tempfile
import unittest
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# the script name has a dash, so it cannot be imported normally - load it by path
import importlib.util  # noqa: E402

_spec = importlib.util.spec_from_file_location(
    "ipsw_inspect", os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "ipsw-inspect.py")
)
ipsw_inspect = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ipsw_inspect)


def make_ipsw(path, version, build, models, members):
    """members: dict of archive path -> bytes"""
    manifest = {
        "ProductVersion": version,
        "ProductBuildVersion": build,
        "BuildTrain": "Azul",
        "SupportedDeviceModels": models,
        "SupportedDevices": models,
    }
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr("BuildManifest.plist", plistlib.dumps(manifest))
        for name, data in members.items():
            zf.writestr(name, data)


BLOB = b"\x00" * 4096


class ClassifyTest(unittest.TestCase):
    def test_buckets(self):
        cases = {
            "kernelcache.release.n66": "kernelcache",
            "Firmware/all_flash/DeviceTree.n66ap.im4p": "devicetree",
            "Firmware/all_flash/DeviceTree.d22ap.im4p": "devicetree",
            "sep-firmware.n104.RELEASE.img4": "sep_firmware",
            "038-44135-124.dmg.trustcache": "trustcache",
            "Firmware/all_flash/iBoot.n66ap.RELEASE.im4p": "boot_chain",
            "048-32651-104.dmg": "ramdisk_dmg",
            "048-31952-103OS.dmg": "rootfs_dmg",
            "BuildManifest.plist": "manifest",
            "Restore.plist": "restore_plist",
            "Firmware/dfu/iBSS.n66ap.RELEASE.im4p": "boot_chain",
            "Payload/random.bin": None,
        }
        for name, expected in cases.items():
            self.assertEqual(ipsw_inspect.classify(name), expected, name)

    def test_classifier_order_trustcache_before_dmg(self):
        # ".dmg.trustcache" must land in trustcache, not ramdisk_dmg
        self.assertEqual(ipsw_inspect.classify("x/038-44135-124.dmg.trustcache"), "trustcache")


class DeviceTableTest(unittest.TestCase):
    def test_a5_devices_have_no_machine_model(self):
        """The point of the whole exercise: iPhone 4s / iPad 2 / iPod touch 5G are A5."""
        for model in ("iPhone4,1", "iPad2,1", "iPad2,2", "iPad2,3", "iPod5,1"):
            self.assertIn(model, ipsw_inspect.DEVICES)
            name, soc, has_model = ipsw_inspect.DEVICES[model]
            self.assertFalse(has_model, "%s (%s) should have no QEMU machine model" % (model, soc))
            self.assertIn("A5", soc)
            self.assertNotIn(model, ipsw_inspect.DEVICE_PATHS)

    def test_a6_devices_have_no_machine_model(self):
        for model in ("iPhone5,1", "iPhone5,2", "iPhone5,3", "iPhone5,4"):
            _, soc, has_model = ipsw_inspect.DEVICES[model]
            self.assertFalse(has_model)
            self.assertIn("A6", soc)

    def test_a4_is_the_legacy_ceiling(self):
        for model in ("iPhone3,1", "iPad1,1", "iPod4,1"):
            _, soc, has_model = ipsw_inspect.DEVICES[model]
            self.assertTrue(has_model)
            self.assertIn("A4", soc)
            self.assertIn(model, ipsw_inspect.DEVICE_PATHS)

    def test_modern_paths_exist(self):
        self.assertEqual(ipsw_inspect.DEVICE_PATHS["iPhone12,1"], ["iphone11-ios14"])
        self.assertEqual(ipsw_inspect.DEVICE_PATHS["iPhone8,2"], ["ios12-shell"])
        self.assertEqual(ipsw_inspect.DEVICE_PATHS["iPod1,1"], ["ipod-touch-1g"])


class InspectTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()

    def tearDown(self):
        for name in os.listdir(self.tmp):
            os.unlink(os.path.join(self.tmp, name))
        os.rmdir(self.tmp)

    def test_ios12_iphone6splus_is_usable(self):
        p = os.path.join(self.tmp, "iPhone_5.5_12.1_16B92_Restore.ipsw")
        make_ipsw(
            p,
            "12.1",
            "16B92",
            ["iPhone8,2"],
            {
                "kernelcache.release.n66": BLOB,
                "Firmware/all_flash/DeviceTree.n66ap.im4p": BLOB,
                "048-32651-104.dmg": BLOB,
                "048-31952-103OS.dmg": BLOB,
                "sep-firmware.n66.RELEASE.img4": BLOB,
            },
        )
        info = ipsw_inspect.inspect(p)
        self.assertEqual(info["manifest"]["ProductVersion"], "12.1")
        self.assertEqual(info["manifest"]["ProductBuildVersion"], "16B92")
        self.assertEqual(info["manifest"]["SupportedDeviceModels"], ["iPhone8,2"])
        self.assertIn("kernelcache", info["buckets"])
        self.assertIn("devicetree", info["buckets"])

        lines, usable, blocked = ipsw_inspect.verdict(info)
        self.assertEqual(usable, ["ios12-shell"])
        self.assertEqual(blocked, [])

        spec, found, missing = ipsw_inspect.need("ios12-shell", info)
        self.assertEqual(missing, [])
        self.assertGreaterEqual(len(found), 4)

    def test_ios9_iphone4s_is_a_dead_end(self):
        """An iOS 9.3.6 iPhone 4s IPSW parses fine but has no path anywhere."""
        p = os.path.join(self.tmp, "iPhone4,1_9.3.6_13G37_Restore.ipsw")
        make_ipsw(
            p,
            "9.3.6",
            "13G37",
            ["iPhone4,1"],
            {
                "kernelcache.release.s5l8942x": BLOB,
                "Firmware/all_flash/DeviceTree.s5l8942xap.im4p": BLOB,
                "048-05151-134.dmg": BLOB,
            },
        )
        info = ipsw_inspect.inspect(p)
        self.assertEqual(info["manifest"]["ProductVersion"], "9.3.6")
        lines, usable, blocked = ipsw_inspect.verdict(info)
        self.assertEqual(usable, [])
        self.assertEqual(len(blocked), 1)
        model, name, soc = blocked[0]
        self.assertEqual(model, "iPhone4,1")
        self.assertEqual(name, "iPhone 4s")
        self.assertIn("A5", soc)

    def test_ios14_iphone11_needs_sep_and_trustcache(self):
        p = os.path.join(self.tmp, "iPhone12,1_14.0_18A5351d_Restore.ipsw")
        make_ipsw(
            p,
            "14.0",
            "18A5351d",
            ["iPhone12,1"],
            {
                "kernelcache.research.iphone12b": BLOB,
                "Firmware/all_flash/DeviceTree.n104ap.im4p": BLOB,
                "038-44135-124.dmg": BLOB,
                "038-44135-124.dmg.trustcache": BLOB,
                "sep-firmware.n104.RELEASE.img4": BLOB,
            },
        )
        info = ipsw_inspect.inspect(p)
        _, usable, _ = ipsw_inspect.verdict(info)
        self.assertEqual(usable, ["iphone11-ios14"])
        spec, found, missing = ipsw_inspect.need("iphone11-ios14", info)
        self.assertEqual(missing, [])

    def test_missing_members_are_reported(self):
        p = os.path.join(self.tmp, "partial.ipsw")
        make_ipsw(p, "12.1", "16B92", ["iPhone8,2"], {"kernelcache.release.n66": BLOB})
        info = ipsw_inspect.inspect(p)
        spec, found, missing = ipsw_inspect.need("ios12-shell", info)
        self.assertTrue(missing)
        self.assertTrue(any("devicetree" in m for m in missing))

    def test_legacy_path_takes_nothing_from_the_ipsw(self):
        p = os.path.join(self.tmp, "iPod1,1.ipsw")
        make_ipsw(p, "1.0", "3A100a", ["iPod1,1"], {"kernelcache.s5l8900xrb": BLOB})
        info = ipsw_inspect.inspect(p)
        _, usable, _ = ipsw_inspect.verdict(info)
        self.assertEqual(usable, ["ipod-touch-1g"])
        spec, found, missing = ipsw_inspect.need("ipod-touch-1g", info)
        self.assertEqual(spec["wants"], [])
        self.assertEqual(missing, [])

    def test_not_a_zip(self):
        p = os.path.join(self.tmp, "garbage.ipsw")
        with open(p, "wb") as fh:
            fh.write(b"this is not a zip at all")
        with self.assertRaises(zipfile.BadZipFile):
            ipsw_inspect.inspect(p)
        self.assertEqual(ipsw_inspect.main([p]), 3)

    def test_cli_json(self):
        p = os.path.join(self.tmp, "cli.ipsw")
        make_ipsw(p, "9.3.6", "13G37", ["iPhone4,1"], {"kernelcache.release.s5l8942x": BLOB})
        buf = io.StringIO()
        old = sys.stdout
        sys.stdout = buf
        try:
            rc = ipsw_inspect.main([p, "--json"])
        finally:
            sys.stdout = old
        self.assertEqual(rc, 0)
        import json

        data = json.loads(buf.getvalue())
        self.assertEqual(data["manifest"]["ProductVersion"], "9.3.6")

    def test_cli_need_returns_2_when_incomplete(self):
        p = os.path.join(self.tmp, "cli2.ipsw")
        make_ipsw(p, "12.1", "16B92", ["iPhone8,2"], {"kernelcache.release.n66": BLOB})
        buf = io.StringIO()
        old = sys.stdout
        sys.stdout = buf
        try:
            rc = ipsw_inspect.main([p, "--need", "ios12-shell"])
        finally:
            sys.stdout = old
        self.assertEqual(rc, 2)


class HumanTest(unittest.TestCase):
    def test_units(self):
        self.assertEqual(ipsw_inspect.human(512), "512 B")
        self.assertIn("KB", ipsw_inspect.human(2048))
        self.assertIn("MB", ipsw_inspect.human(5 * 1024 * 1024))
        self.assertIn("GB", ipsw_inspect.human(6 * 1024 ** 3))


if __name__ == "__main__":
    unittest.main(verbosity=2)
