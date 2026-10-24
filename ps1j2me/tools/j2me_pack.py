#!/usr/bin/env python3
"""J2ME jar packer / verifier / class downgrader.

Used by build.sh as a portable fallback when ProGuard or a Java 1.3 capable
javac is not available, and as the final verification step of every build.

    python3 tools/j2me_pack.py pack --classes build/classes --res res --out PS1J2ME.jar
    python3 tools/j2me_pack.py downgrade --classes build/classes
    python3 tools/j2me_pack.py verify PS1J2ME.jar

Only the Python standard library is used.
"""

import argparse
import os
import struct
import sys
import zipfile

CLDC_MAJOR = 47  # Java 1.3 / CLDC 1.1 bytecode

MANIFEST = """Manifest-Version: 1.0
MIDlet-Name: PS1J2ME
MIDlet-Version: 1.4.0
MIDlet-Vendor: PS1J2ME Project
MIDlet-1: PS1J2ME, , psx.PS1Midlet
MicroEdition-Configuration: CLDC-1.1
MicroEdition-Profile: MIDP-2.0
"""


# --------------------------------------------------------------------------
#  minimal class file reader
# --------------------------------------------------------------------------

def u1(data, off):
    return data[off], off + 1


def u2(data, off):
    return struct.unpack_from(">H", data, off)[0], off + 2


def u4(data, off):
    return struct.unpack_from(">I", data, off)[0], off + 4


class ClassFile(object):
    def __init__(self, data):
        self.data = data
        magic, off = u4(data, 0)
        if magic != 0xCAFEBABE:
            raise ValueError("not a class file")
        self.minor, off = u2(data, off)
        self.major, off = u2(data, off)
        self.cp_count, off = u2(data, off)
        self.cp_start = off
        cp = {}
        i = 1
        while i < self.cp_count:
            tag, off = u1(data, off)
            if tag == 1:
                ln, off = u2(data, off)
                cp[i] = data[off:off + ln].decode("utf-8", "replace")
                off += ln
            elif tag in (7, 8, 16, 19, 20):
                off += 2
            elif tag in (15,):
                off += 3
            elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
                off += 4
            elif tag in (5, 6):
                off += 8
                i += 1
            else:
                raise ValueError("bad constant pool tag %d" % tag)
            i += 1
        self.cp = cp
        self.cp_end = off
        self.access, off = u2(data, off)
        self.this_class, off = u2(data, off)
        self.super_class, off = u2(data, off)
        ic, off = u2(data, off)
        off += 2 * ic
        off = self._skip_members(off, cp)
        off = self._skip_members(off, cp)
        self.attrs_off = off
        self.attr_count, off = u2(data, off)
        self.attrs = []
        for _ in range(self.attr_count):
            name_idx, off = u2(data, off)
            ln, off = u4(data, off)
            self.attrs.append((cp.get(name_idx, ""), off, ln))
            off += ln
        self.end = off

    def _skip_members(self, off, cp):
        count, off = u2(self.data, off)
        for _ in range(count):
            off += 6
            nattr, off = u2(self.data, off)
            for _ in range(nattr):
                name_idx, off = u2(self.data, off)
                ln, off = u4(self.data, off)
                off += ln
        return off

    def name(self):
        return self.cp.get(self.cp.get(self.this_class), "?")


def strip_stackmaps(data):
    """Returns a class file rewritten for CLDC: major 47, no StackMapTable."""
    cf = ClassFile(data)
    if cf.major == CLDC_MAJOR and not _has_stackmap(cf):
        return data

    out = bytearray()
    out += struct.pack(">IHH", 0xCAFEBABE, 3, CLDC_MAJOR)
    out += data[cf.cp_start - 4:cf.cp_end]          # constant pool (verbatim)
    body = bytearray()
    body += struct.pack(">HHH", cf.access, cf.this_class, cf.super_class)
    # interfaces + members + attributes: copy with attribute filtering
    off = cf.cp_end
    ic, _ = u2(data, off)
    body += struct.pack(">H", ic)
    off += 2
    body += data[off:off + 2 * ic]
    off += 2 * ic
    body, off = _copy_members(data, off, cf.cp, body)
    body, off = _copy_members(data, off, cf.cp, body)
    body += struct.pack(">H", cf.attr_count)
    for name, aoff, ln in cf.attrs:
        if name in ("StackMapTable",):
            continue
        body += struct.pack(">HI", _cp_index(cf.cp, name), ln)
        body += data[aoff:aoff + ln]
    out += body
    return bytes(out)


def _has_stackmap(cf):
    for name, _, _ in cf.attrs:
        if name == "StackMapTable":
            return True
    return False


def _cp_index(cp, name):
    for idx, val in cp.items():
        if val == name:
            return idx
    raise KeyError(name)


def _copy_members(data, off, cp, out):
    count, off = u2(data, off)
    out += struct.pack(">H", count)
    for _ in range(count):
        out += data[off:off + 6]
        off += 6
        nattr, off = u2(data, off)
        kept = []
        for _ in range(nattr):
            name_idx, off = u2(data, off)
            ln, off = u4(data, off)
            name = cp.get(name_idx, "")
            if name == "StackMapTable":
                off += ln
                continue
            kept.append((name_idx, data[off:off + ln]))
            off += ln
        out += struct.pack(">H", len(kept))
        for name_idx, payload in kept:
            out += struct.pack(">HI", name_idx, len(payload))
            out += payload
    return out, off


# --------------------------------------------------------------------------
#  commands
# --------------------------------------------------------------------------

def iter_classes(root):
    for dirpath, _, files in os.walk(root):
        for f in files:
            if f.endswith(".class"):
                yield os.path.join(dirpath, f)


def cmd_downgrade(args):
    changed = 0
    for path in iter_classes(args.classes):
        with open(path, "rb") as fh:
            data = fh.read()
        new = strip_stackmaps(data)
        if new != data:
            with open(path, "wb") as fh:
                fh.write(new)
            changed += 1
    print("[pack] downgraded %d class files to major %d" % (changed, CLDC_MAJOR))


def cmd_pack(args):
    if not hasattr(args, "store_res"):
        args.store_res = False
    entries = []
    for path in iter_classes(args.classes):
        arc = os.path.relpath(path, args.classes).replace(os.sep, "/")
        entries.append((arc, path))
    if os.path.isdir(args.res):
        for dirpath, _, files in os.walk(args.res):
            for f in files:
                full = os.path.join(dirpath, f)
                arc = "res/" + os.path.relpath(full, args.res).replace(os.sep, "/")
                entries.append((arc, full))
    entries.sort()
    manifest = MANIFEST
    if args.manifest and os.path.exists(args.manifest):
        with open(args.manifest, "r") as fh:
            manifest = fh.read()
    if not args.out.endswith(".jar"):
        args.out += ".jar"
    outdir = os.path.dirname(os.path.abspath(args.out))
    if outdir and not os.path.isdir(outdir):
        os.makedirs(outdir)
    with zipfile.ZipFile(args.out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("META-INF/MANIFEST.MF", manifest)
        for arc, path in entries:
            # Resources stay uncompressed: the BIOS and the demo discs are
            # random looking data that barely compresses, and inflating 512 KiB
            # of BIOS on a KVM costs seconds of start-up time. Storing them
            # also keeps the archive at a predictable ~1 MiB.
            # Everything is deflated by default: a ~95 KiB JAR installs on every
            # handset, while 1 MiB of stored resources hit the device size limit.
            method = zipfile.ZIP_STORED if arc.startswith("res/") and args.store_res \
                else zipfile.ZIP_DEFLATED
            z.write(path, arc, compress_type=method)
    size = os.path.getsize(args.out)
    print("[pack] wrote %s (%d entries, %d KiB)" % (args.out, len(entries) + 1, size // 1024))


def cmd_jad(args):
    jar = args.jar
    size = os.path.getsize(jar)
    name = os.path.basename(jar)
    lines = [
        "MIDlet-Name: PS1J2ME",
        "MIDlet-Version: 1.4.0",
        "MIDlet-Vendor: PS1J2ME Project",
        "MIDlet-1: PS1J2ME, , psx.PS1Midlet",
        "MicroEdition-Configuration: CLDC-1.1",
        "MicroEdition-Profile: MIDP-2.0",
        "MIDlet-Jar-URL: " + name,
        "MIDlet-Jar-Size: " + str(size),
    ]
    out = args.out or (os.path.splitext(jar)[0] + ".jad")
    with open(out, "w") as fh:
        fh.write("\r\n".join(lines) + "\r\n")
    print("[pack] wrote %s (jar %d bytes)" % (out, size))


def cmd_verify(args):
    ok = True
    with zipfile.ZipFile(args.jar) as z:
        names = z.namelist()
        if "META-INF/MANIFEST.MF" not in names:
            print("FAIL: missing manifest")
            ok = False
        bios = [n for n in names if n.lower() == "res/roms/scph1001.bin"]
        if not bios:
            print("WARN: /res/roms/scph1001.bin not embedded")
        else:
            info = z.getinfo(bios[0])
            if info.file_size != 512 * 1024:
                print("WARN: BIOS size is %d (expected 524288)" % info.file_size)
        classes = 0
        for n in names:
            if not n.endswith(".class"):
                continue
            classes += 1
            data = z.read(n)
            try:
                cf = ClassFile(data)
            except Exception as exc:  # noqa: BLE001
                print("FAIL: %s: %s" % (n, exc))
                ok = False
                continue
            if cf.major != CLDC_MAJOR:
                print("FAIL: %s major=%d (expected 47)" % (n, cf.major))
                ok = False
            for attr, _, _ in cf.attrs:
                if attr == "StackMapTable":
                    print("FAIL: %s still has StackMapTable" % n)
                    ok = False
            if b"java/nio" in data:
                print("FAIL: %s references java.nio" % n)
                ok = False
        print("[verify] %d classes checked" % classes)
    print("[verify] %s" % ("OK - CLDC 1.1 / MIDP 2.0 compatible" if ok else "FAILED"))
    return 0 if ok else 1


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    sub = ap.add_subparsers(dest="cmd")

    p = sub.add_parser("pack")
    p.add_argument("--classes", required=True)
    p.add_argument("--res", default="res")
    p.add_argument("--store-res", dest="store_res", action="store_true")
    p.add_argument("--out", required=True)
    p.add_argument("--manifest")
    p.set_defaults(func=cmd_pack)

    d = sub.add_parser("downgrade")
    d.add_argument("--classes", required=True)
    d.set_defaults(func=cmd_downgrade)

    j = sub.add_parser("jad")
    j.add_argument("jar")
    j.add_argument("--out")
    j.set_defaults(func=cmd_jad)

    v = sub.add_parser("verify")
    v.add_argument("jar")
    v.set_defaults(func=cmd_verify)

    args = ap.parse_args()
    if not getattr(args, "func", None):
        ap.print_help()
        return 2
    return args.func(args) or 0


if __name__ == "__main__":
    sys.exit(main())
