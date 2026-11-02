#!/usr/bin/env python3
# =============================================================================
#  ChipLab - binary AndroidManifest.xml (AXML) writer.
#
#  The sandbox cannot download the Android SDK (aapt), so we emit the binary
#  XML ourselves: UTF-8 string pool, resource-id map (ids extracted from the
#  real android.R$attr inside android.jar), namespace and element chunks.
#  The manifest is tiny (manifest > uses-sdk, application > activity >
#  intent-filter) and references no bundled resources, so no aapt is needed.
#
#  Usage: python3 tools/make_axml.py <android.jar> <out AndroidManifest.xml>
# =============================================================================
import struct
import sys
import zipfile

ANDROID_NS = "http://schemas.android.com/apk/res/android"

RES_STRING_POOL_TYPE = 0x0001
RES_XML_TYPE = 0x0003
RES_XML_RESOURCE_MAP_TYPE = 0x0180
RES_XML_START_NAMESPACE_TYPE = 0x0100
RES_XML_END_NAMESPACE_TYPE = 0x0101
RES_XML_START_ELEMENT_TYPE = 0x0102
RES_XML_END_ELEMENT_TYPE = 0x0103

TYPE_STRING = 0x03
TYPE_INT_DEC = 0x10
TYPE_INT_BOOLEAN = 0x12

WANTED_ATTRS = ["versionCode", "versionName", "minSdkVersion", "targetSdkVersion",
                "label", "allowBackup", "name", "exported", "screenOrientation",
                "configChanges"]


def parse_r_attr(jar_path):
    """Read android/R$attr.class from android.jar -> {name: resource id}."""
    with zipfile.ZipFile(jar_path) as zf:
        data = zf.read("android/R$attr.class")
    pos = 0

    def u2():
        nonlocal pos
        v = struct.unpack_from(">H", data, pos)[0]
        pos += 2
        return v

    def u4():
        nonlocal pos
        v = struct.unpack_from(">I", data, pos)[0]
        pos += 4
        return v

    magic = u4()
    assert magic == 0xCAFEBABE, "bad class file"
    u2(); u2()
    cp_count = u2()
    cp = [None] * cp_count
    i = 1
    while i < cp_count:
        tag = data[pos]; pos += 1
        if tag == 1:  # Utf8
            ln = u2()
            cp[i] = ("utf8", data[pos:pos + ln].decode("utf-8", "replace"))
            pos += ln
        elif tag == 3:  # Integer
            cp[i] = ("int", struct.unpack_from(">i", data, pos)[0] & 0xFFFFFFFF)
            pos += 4
        elif tag in (4,):
            cp[i] = ("float", 0); pos += 4
        elif tag in (5, 6):
            cp[i] = ("wide", 0); pos += 8; i += 1
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = ("ref", u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = ("ref2", (u2(), u2()))
        elif tag == 15:
            pos += 1; cp[i] = ("mh", u2())
        else:
            raise ValueError("unknown cp tag %d" % tag)
        i += 1
    u2(); u2(); u2()  # access, this, super
    for _ in range(u2()):
        u2()
    result = {}
    fields = u2()
    for _ in range(fields):
        u2()  # access
        name = cp[u2()][1]
        u2()  # descriptor
        value = None
        for _ in range(u2()):
            attr_name = cp[u2()][1]
            ln = u4()
            if attr_name == "ConstantValue":
                value = cp[u2()][1]
                pos += max(0, ln - 2)
            else:
                pos += ln
        if name in WANTED_ATTRS and value is not None:
            result[name] = value
    missing = [a for a in WANTED_ATTRS if a not in result]
    if missing:
        raise ValueError("R.attr missing: %s" % missing)
    return result


class StringPool:
    def __init__(self):
        self.strings = []
        self.index = {}

    def add(self, s):
        if s not in self.index:
            self.index[s] = len(self.strings)
            self.strings.append(s)
        return self.index[s]

    @staticmethod
    def _u8len(n):
        if n < 0x80:
            return bytes([n])
        return bytes([0x80 | (n >> 8), n & 0xFF])

    def chunk(self):
        encoded = []
        blob = bytearray()
        offsets = []
        for s in self.strings:
            raw = s.encode("utf-8")
            entry = self._u8len(len(s)) + self._u8len(len(raw)) + raw + b"\x00"
            offsets.append(len(blob))
            blob += entry
            encoded.append(entry)
        header_size = 28
        strings_start = header_size + 4 * len(self.strings)
        size = strings_start + len(blob)
        pad = (4 - size % 4) % 4
        size += pad
        blob += b"\x00" * pad
        out = struct.pack("<HHIIIIII", RES_STRING_POOL_TYPE, header_size, size,
                          len(self.strings), 0, 0x100, strings_start, 0)
        out += b"".join(struct.pack("<I", o) for o in offsets)
        out += bytes(blob)
        assert len(out) == size
        return out


def node_header(type_, header_size, size):
    # ResChunk_header + lineNumber + comment(-1)
    return struct.pack("<HHIII", type_, header_size, size, 0, 0xFFFFFFFF)


def make_manifest(attr_ids):
    pool = StringPool()
    nodes = []  # built after all strings are known? strings must be added first

    # We register every string while building a tree, then serialize.
    NS = pool.add(ANDROID_NS)
    ANDROID = pool.add("android")

    def attr(name, value, type_):
        return (NS, pool.add(name), value, type_)

    def sattr(name, s):  # string attribute in android ns
        si = pool.add(s)
        return (NS, pool.add(name), (si, si), TYPE_STRING)

    def iattr(name, v):
        return attr(name, (0xFFFFFFFF, v & 0xFFFFFFFF), TYPE_INT_DEC)

    def battr(name, v):
        return attr(name, (0xFFFFFFFF, 0xFFFFFFFF if v else 0), TYPE_INT_BOOLEAN)

    events = []  # ("start_ns", prefix, uri) ("elem", ns, name, [attrs]) ("end", ns, name)

    def elem(name, attrs=(), children=()):
        n = pool.add(name)
        events.append(("elem", 0xFFFFFFFF, n, list(attrs)))
        for ch in children:
            ch()
        events.append(("end", 0xFFFFFFFF, n))

    manifest_attrs = [
        (0xFFFFFFFF, pool.add("package"), None, None),  # placeholder fixed below
        iattr("versionCode", 3),
        sattr("versionName", "1.2"),
    ]
    pkg_si = pool.add("com.chiplab")
    manifest_attrs[0] = (0xFFFFFFFF, pool.add("package"), (pkg_si, pkg_si), TYPE_STRING)

    events.append(("start_ns", ANDROID, NS))

    def activity():
        def intent_filter():
            elem("action", [sattr("name", "android.intent.action.MAIN")])
            elem("category", [sattr("name", "android.intent.category.LAUNCHER")])
        elem("activity", [
            sattr("name", "com.chiplab.MainActivity"),
            battr("exported", True),
            iattr("screenOrientation", 1),          # portrait
            iattr("configChanges", 0x0080 | 0x0200 | 0x0400),  # orientation|keyboardHidden|screenSize
        ], [lambda: elem("intent-filter", [], [intent_filter])])

    elem("manifest", manifest_attrs, [
        lambda: elem("uses-sdk", [iattr("minSdkVersion", 21), iattr("targetSdkVersion", 28)]),
        lambda: elem("application", [
            sattr("label", "ChipLab — Симулятор чипов"),
            battr("allowBackup", False),
        ], [activity]),
    ])
    events.append(("end_ns", ANDROID, NS))

    # resource map: one u32 per string (0 unless android attr)
    res_ids = []
    for s in pool.strings:
        res_ids.append(attr_ids.get(s, 0))
    resmap = struct.pack("<HHI", RES_XML_RESOURCE_MAP_TYPE, 8, 8 + 4 * len(res_ids))
    resmap += b"".join(struct.pack("<I", v) for v in res_ids)

    body = bytearray()
    for ev in events:
        if ev[0] in ("start_ns", "end_ns"):
            t = RES_XML_START_NAMESPACE_TYPE if ev[0] == "start_ns" else RES_XML_END_NAMESPACE_TYPE
            body += node_header(t, 16, 24)
            body += struct.pack("<II", ev[1], ev[2])
        elif ev[0] == "elem":
            _, ns, name, attrs = ev
            size = 16 + 20 + 20 * len(attrs)
            body += node_header(RES_XML_START_ELEMENT_TYPE, 16, size)
            body += struct.pack("<IIHHHHHH", ns, name, 20, 20, len(attrs), 0, 0, 0)
            for (ans, aname, val, vtype) in attrs:
                raw, data = val
                body += struct.pack("<IIIHBBI", ans, aname, raw, 8, 0, vtype, data)
        elif ev[0] == "end":
            _, ns, name = ev
            body += node_header(RES_XML_END_ELEMENT_TYPE, 16, 24)
            body += struct.pack("<II", ns, name)

    pool_chunk = pool.chunk()
    total = 8 + len(pool_chunk) + len(resmap) + len(body)
    out = struct.pack("<HHI", RES_XML_TYPE, 8, total) + pool_chunk + resmap + bytes(body)
    assert len(out) == total
    return out


def main():
    jar, out_path = sys.argv[1], sys.argv[2]
    attr_ids = parse_r_attr(jar)
    print("[axml] R.attr ids:", {k: hex(v) for k, v in sorted(attr_ids.items())})
    data = make_manifest(attr_ids)
    open(out_path, "wb").write(data)
    print("[axml] wrote %s (%d bytes)" % (out_path, len(data)))


if __name__ == "__main__":
    main()
