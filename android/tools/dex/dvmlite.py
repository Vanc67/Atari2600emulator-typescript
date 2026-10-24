"""A small Dalvik interpreter that runs classes.dex.

Why this exists: the app is built without an Android SDK, by translating JVM
class files to smali (jvm2smali.py) and assembling them with dexlib2.  There is
no device and no emulator on the build host, so the only way to prove that the
*shipped* dex actually behaves like the Java sources is to execute it.  This
module reads a dex file, resolves classes/methods, and interprets the subset of
Dalvik that our code generator emits.  Anything else raises loudly.

It is a test harness, not part of the APK.
"""

import struct
import sys


class DexError(Exception):
    pass


class JavaThrow(Exception):
    """A Java exception travelling up the call stack."""

    def __init__(self, obj):
        Exception.__init__(self, str(obj))
        self.obj = obj


# --------------------------------------------------------------- dex reading
def _u1(b, o):
    return b[o]


def _u2(b, o):
    return b[o] | (b[o + 1] << 8)


def _u4(b, o):
    return struct.unpack_from("<I", b, o)[0]


def _uleb(b, o):
    result = 0
    shift = 0
    while True:
        c = b[o]
        o += 1
        result |= (c & 0x7F) << shift
        if not (c & 0x80):
            return result, o
        shift += 7


def _mutf8(b, o, n):
    out = []
    i = o
    while len(out) < n:
        c = b[i]
        if c < 0x80:
            out.append(chr(c))
            i += 1
        elif (c & 0xE0) == 0xC0:
            out.append(chr(((c & 0x1F) << 6) | (b[i + 1] & 0x3F)))
            i += 2
        else:
            out.append(chr(((c & 0x0F) << 12) | ((b[i + 1] & 0x3F) << 6)
                           | (b[i + 2] & 0x3F)))
            i += 3
    return "".join(out)


class Method(object):
    __slots__ = ("cls", "name", "params", "ret", "code", "registers", "tries",
                 "static")

    def __init__(self, cls, name, params, ret):
        self.cls = cls
        self.name = name
        self.params = tuple(params)
        self.ret = ret
        self.code = None
        self.registers = 0
        self.tries = []
        self.static = False

    def key(self):
        return (self.cls, self.name, self.params, self.ret)

    def __repr__(self):
        return "%s->%s(%s)%s" % (self.cls[1:-1].replace("/", "."), self.name,
                                 "".join(self.params), self.ret)


class ClassDef(object):
    def __init__(self, name):
        self.name = name
        self.super = None
        self.access = 0
        self.interfaces = []
        self.static_fields = []       # [(name, type, value)]
        self.instance_fields = []     # [(name, type)]
        self.direct = {}              # key -> Method
        self.virtual = {}
        self.static_values = []


class Dex(object):
    def __init__(self, data):
        self.data = data
        if data[:4] != b"dex\n":
            raise DexError("not a dex file")
        header = struct.unpack_from("<8sIi4sIii", data, 0)
        self.endian = header[6]
        n = lambda fmt, off: struct.unpack_from(fmt, data, off)[0]
        self.string_ids_size = n("<I", 0x38)
        self.string_ids_off = n("<I", 0x3C)
        self.type_ids_size = n("<I", 0x40)
        self.type_ids_off = n("<I", 0x44)
        self.proto_ids_size = n("<I", 0x48)
        self.proto_ids_off = n("<I", 0x4C)
        self.field_ids_size = n("<I", 0x50)
        self.field_ids_off = n("<I", 0x54)
        self.method_ids_size = n("<I", 0x58)
        self.method_ids_off = n("<I", 0x5C)
        self.class_defs_size = n("<I", 0x60)
        self.class_defs_off = n("<I", 0x64)
        self._strings()
        self._types()
        self._protos()
        self._fields()
        self._methods()
        self._classes()

    # -- tables --------------------------------------------------------
    def _strings(self):
        d = self.data
        self.strings = []
        for i in range(self.string_ids_size):
            off = _u4(d, self.string_ids_off + 4 * i)
            ln, p = _uleb(d, off)
            self.strings.append(_mutf8(d, p, ln))

    def _types(self):
        d = self.data
        self.types = [self.strings[_u4(d, self.type_ids_off + 4 * i)]
                      for i in range(self.type_ids_size)]

    def _protos(self):
        d = self.data
        self.protos = []
        for i in range(self.proto_ids_size):
            base = self.proto_ids_off + 12 * i
            ret = self.types[_u4(d, base + 4)]
            params_off = _u4(d, base + 8)
            params = []
            if params_off:
                cnt = _u4(d, params_off)
                for k in range(cnt):
                    params.append(self.types[_u2(d, params_off + 4 + 2 * k)])
            self.protos.append((tuple(params), ret))

    def _fields(self):
        d = self.data
        self.fields = []
        for i in range(self.field_ids_size):
            base = self.field_ids_off + 8 * i
            cls = self.types[_u2(d, base)]
            typ = self.types[_u2(d, base + 2)]
            name = self.strings[_u4(d, base + 4)]
            self.fields.append((cls, name, typ))

    def _methods(self):
        d = self.data
        self.methods = []
        for i in range(self.method_ids_size):
            base = self.method_ids_off + 8 * i
            cls = self.types[_u2(d, base)]
            proto = self.protos[_u2(d, base + 2)]
            name = self.strings[_u4(d, base + 4)]
            self.methods.append((cls, name, proto[0], proto[1]))

    def _classes(self):
        d = self.data
        self.classes = {}
        for i in range(self.class_defs_size):
            base = self.class_defs_off + 32 * i
            name = self.types[_u4(d, base)]
            c = ClassDef(name)
            c.access = _u4(d, base + 4)
            sup = _u4(d, base + 8)
            c.super = self.types[sup] if sup != 0xFFFFFFFF else None
            ifc = _u4(d, base + 12)
            if ifc:
                cnt = _u4(d, ifc)
                c.interfaces = [self.types[_u2(d, ifc + 4 + 2 * k)]
                                for k in range(cnt)]
            data_off = _u4(d, base + 24)
            sv = _u4(d, base + 28)
            if data_off:
                self._class_data(c, data_off)
            if sv:
                self._static_values(c, sv)
            self.classes[name] = c

    def _class_data(self, c, off):
        d = self.data
        sf, p = _uleb(d, off)
        inf, p = _uleb(d, p)
        dm, p = _uleb(d, p)
        vm, p = _uleb(d, p)
        idx = 0
        for _ in range(sf):
            diff, p = _uleb(d, p)
            acc, p = _uleb(d, p)
            idx += diff
            _cls, name, typ = self.fields[idx]
            c.static_fields.append((name, typ, acc))
        idx = 0
        for _ in range(inf):
            diff, p = _uleb(d, p)
            acc, p = _uleb(d, p)
            idx += diff
            _cls, name, typ = self.fields[idx]
            c.instance_fields.append((name, typ))
        idx = 0
        for count, table in ((dm, c.direct), (vm, c.virtual)):
            idx = 0
            for _ in range(count):
                diff, p = _uleb(d, p)
                acc, p = _uleb(d, p)
                code_off, p = _uleb(d, p)
                idx += diff
                mcls, mname, params, ret = self.methods[idx]
                m = Method(c.name, mname, params, ret)
                m.code = code_off or None
                table[m.key()] = m

    def _static_values(self, c, off):
        d = self.data
        cnt, p = _uleb(d, off)
        vals = []
        for _ in range(cnt):
            v, p = self._encoded_value(p)
            vals.append(v)
        c.static_values = vals

    def _encoded_value(self, p):
        """Decode one encoded_value.

        The header byte holds the value type in bits 0-4 and, for the numeric
        types, value_arg in bits 5-7: value_arg + 1 is the number of bytes the
        literal occupies.  Small constants therefore take one or two bytes,
        which is why a fixed four byte read desynchronises the stream.
        """
        d = self.data
        b = d[p]
        p += 1
        typ = b & 0x1F
        arg = (b >> 5) & 0x07
        width = arg + 1

        def uint():
            v = 0
            for i in range(width):
                v |= d[p + i] << (8 * i)
            return v

        def sint():
            v = uint()
            bits = 8 * width
            if v >= (1 << (bits - 1)):
                v -= (1 << bits)
            return v

        if typ == 0x00:                      # byte
            return sint(), p + width
        if typ == 0x02:                      # short
            return sint(), p + width
        if typ == 0x03:                      # char (unsigned)
            return uint(), p + width
        if typ == 0x04:                      # int
            return i32(sint()), p + width
        if typ == 0x06:                      # long
            return i64(sint()), p + width
        if typ == 0x10:                      # float, right zero extended
            return f32(struct.unpack("<I", struct.pack("<I", uint()))[0]), p + width
        if typ == 0x11:                      # double, right zero extended
            return struct.unpack("<d", struct.pack("<Q", uint()))[0], p + width
        if typ == 0x17:                      # string
            idx, p = _uleb(d, p)
            return Str(self.strings[idx]), p
        if typ == 0x18:                      # type
            idx, p = _uleb(d, p)
            return ("type", self.types[idx]), p
        if typ in (0x19, 0x1A, 0x1B):        # field / method / enum
            idx, p = _uleb(d, p)
            return ("ref", typ, idx), p
        if typ == 0x1C:                      # array
            n, p = _uleb(d, p)
            out = []
            for _ in range(n):
                v, p = self._encoded_value(p)
                out.append(v)
            return out, p
        if typ == 0x1D:                      # annotation
            raise DexError("annotation values are not supported")
        if typ == 0x1E:                      # null
            return None, p
        if typ == 0x1F:                      # boolean
            return bool(arg), p
        raise DexError("encoded value type 0x%02x" % typ)

    # -- code ----------------------------------------------------------
    def code(self, off):
        d = self.data
        regs, ins, outs, tries, dbg = struct.unpack_from("<HHHHI", d, off)
        n = _u4(d, off + 12)
        insns = struct.unpack_from("<%dH" % n, d, off + 16)
        out = {"registers": regs, "ins": ins, "insns": insns, "tries": []}
        if tries:
            p = off + 16 + 2 * n
            if n % 2:
                p += 2
            raw = []
            for i in range(tries):
                start, count, hoff = struct.unpack_from("<IHH", d, p + 8 * i)
                raw.append((start, count, hoff))
            hp = p + 8 * tries
            size, q = _uleb(d, hp)
            handlers = {}
            for _ in range(abs(size)):
                here = q
                cnt, q = _sleb(d, q)
                pairs = []
                for _ in range(abs(cnt)):
                    tidx, q = _uleb(d, q)
                    addr, q = _uleb(d, q)
                    pairs.append((self.types[tidx], addr))
                catch_all = None
                if cnt <= 0:
                    catch_all, q = _uleb(d, q)
                handlers[here - hp] = (pairs, catch_all)
            for (start, count, hoff) in raw:
                pairs, catch_all = handlers[hoff]
                out["tries"].append((start, count, pairs, catch_all))
        return out


def _radix(v, radix):
    """Integer/Long.toString(value, radix) for the bases the core uses."""
    if radix == 10:
        return str(v)
    if radix == 16:
        return "%x" % (v & 0xFFFFFFFFFFFFFFFF if v < 0 else v)
    if radix == 2:
        return bin(v & 0xFFFFFFFFFFFFFFFF)[2:] if v < 0 else bin(v)[2:]
    digits = "0123456789abcdefghijklmnopqrstuvwxyz"
    if v == 0:
        return "0"
    neg = v < 0
    v = abs(v)
    out = []
    while v:
        out.append(digits[v % radix])
        v //= radix
    if neg:
        out.append("-")
    return "".join(reversed(out))


def _sleb(b, o):
    result = 0
    shift = 0
    while True:
        c = b[o]
        o += 1
        result |= (c & 0x7F) << shift
        shift += 7
        if not (c & 0x80):
            if c & 0x40:
                result -= (1 << shift)
            return result, o


def _s32(lo, hi):
    """Two little-endian code units -> signed 32-bit value."""
    v = lo | (hi << 16)
    return v - 0x100000000 if v >= 0x80000000 else v


def _signed(v, bits):
    if v >= (1 << (bits - 1)):
        v -= (1 << bits)
    return v


# --------------------------------------------------------------- values
class Obj(object):
    __slots__ = ("cls", "fields")

    def __init__(self, cls):
        self.cls = cls
        self.fields = {}

    def __repr__(self):
        if self.cls == "Ljava/lang/String;":
            return repr(self.fields.get(("Ljava/lang/String;", "value")))
        return "<%s %s>" % (self.cls, self.fields)


class Arr(object):
    __slots__ = ("typ", "a")

    def __init__(self, typ, a):
        self.typ = typ
        self.a = a

    def __len__(self):
        return len(self.a)

    def __repr__(self):
        return "Arr(%s,%r)" % (self.typ, self.a[:8])


class Str(str):
    """A Java string: a plain Python str, so comparisons stay cheap."""
    pass


class StrObj(Obj):
    """`new String(byte[], ...)`: an object that becomes a string.

    The constructor has to run before the text exists, so the instance is
    created empty and filled in by the <init> native.  Everything that
    consumes it unwraps it with _sv().
    """

    __slots__ = ()

    def value(self):
        return self.fields.get(("str", "value"), "")

    def __len__(self):
        return len(self.value())

    def __eq__(self, other):
        return self.value() == _sv(other)

    def __ne__(self, other):
        return not self.__eq__(other)

    def __hash__(self):
        return hash(self.value())

    def __str__(self):
        return self.value()

    def __repr__(self):
        return repr(self.value())


def _decode_str(raw):
    """Bytes -> text.  The core only ever decodes ASCII-ish disc names and
    SYSTEM.CNF, so a straight UTF-8 decode with a Latin-1 fallback is exact
    for everything it produces."""
    try:
        return raw.decode("utf-8")
    except UnicodeDecodeError:
        return raw.decode("latin-1")


def _sv(v):
    """Unwrap a StrObj into a plain Python str (anything else unchanged)."""
    if isinstance(v, StrObj):
        return v.value()
    return v


def to_py(v):
    return v


# --------------------------------------------------------------- opcodes
FMT = {}
NAME = {}


def _op(code, name, fmt):
    FMT[code] = fmt
    NAME[code] = name


_op(0x00, "nop", "10x")
_op(0x01, "move", "12x")
_op(0x02, "move/from16", "22x")
_op(0x03, "move/16", "32x")
_op(0x04, "move-wide", "12x")
_op(0x05, "move-wide/from16", "22x")
_op(0x06, "move-wide/16", "32x")
_op(0x07, "move-object", "12x")
_op(0x08, "move-object/from16", "22x")
_op(0x09, "move-object/16", "32x")
_op(0x0A, "move-result", "11x")
_op(0x0B, "move-result-wide", "11x")
_op(0x0C, "move-result-object", "11x")
_op(0x0D, "move-exception", "11x")
_op(0x0E, "return-void", "10x")
_op(0x0F, "return", "11x")
_op(0x10, "return-wide", "11x")
_op(0x11, "return-object", "11x")
_op(0x12, "const/4", "11n")
_op(0x13, "const/16", "21s")
_op(0x14, "const", "31i")
_op(0x15, "const/high16", "21h")
_op(0x16, "const-wide/16", "21s")
_op(0x17, "const-wide/32", "31i")
_op(0x18, "const-wide", "51l")
_op(0x19, "const-wide/high16", "21h")
_op(0x1A, "const-string", "21c")
_op(0x1B, "const-string/jumbo", "31c")
_op(0x1C, "const-class", "21c")
_op(0x1D, "monitor-enter", "11x")
_op(0x1E, "monitor-exit", "11x")
_op(0x1F, "check-cast", "21c")
_op(0x20, "instance-of", "22c")
_op(0x21, "array-length", "12x")
_op(0x22, "new-instance", "21c")
_op(0x23, "new-array", "22c")
_op(0x24, "filled-new-array", "35c")
_op(0x25, "filled-new-array/range", "3rc")
_op(0x26, "fill-array-data", "31t")
_op(0x27, "throw", "11x")
_op(0x28, "goto", "10t")
_op(0x29, "goto/16", "20t")
_op(0x2A, "goto/32", "30t")
_op(0x2B, "packed-switch", "31t")
_op(0x2C, "sparse-switch", "31t")
_op(0x2D, "cmpl-float", "23x")
_op(0x2E, "cmpg-float", "23x")
_op(0x2F, "cmpl-double", "23x")
_op(0x30, "cmpg-double", "23x")
_op(0x31, "cmp-long", "23x")
_op(0x32, "if-eq", "22t")
_op(0x33, "if-ne", "22t")
_op(0x34, "if-lt", "22t")
_op(0x35, "if-ge", "22t")
_op(0x36, "if-gt", "22t")
_op(0x37, "if-le", "22t")
_op(0x38, "if-eqz", "21t")
_op(0x39, "if-nez", "21t")
_op(0x3A, "if-ltz", "21t")
_op(0x3B, "if-gez", "21t")
_op(0x3C, "if-gtz", "21t")
_op(0x3D, "if-lez", "21t")

_AGET = {"aget": None, "aget-wide": None, "aget-object": None,
         "aget-boolean": None, "aget-byte": None, "aget-char": None,
         "aget-short": None}
for _i, _n in enumerate(("aget", "aget-wide", "aget-object", "aget-boolean",
                         "aget-byte", "aget-char", "aget-short")):
    _op(0x44 + _i, _n, "23x")
for _i, _n in enumerate(("aput", "aput-wide", "aput-object", "aput-boolean",
                         "aput-byte", "aput-char", "aput-short")):
    _op(0x4B + _i, _n, "23x")
for _i, _n in enumerate(("iget", "iget-wide", "iget-object", "iget-boolean",
                         "iget-byte", "iget-char", "iget-short")):
    _op(0x52 + _i, _n, "22c")
for _i, _n in enumerate(("iput", "iput-wide", "iput-object", "iput-boolean",
                         "iput-byte", "iput-char", "iput-short")):
    _op(0x59 + _i, _n, "22c")
for _i, _n in enumerate(("sget", "sget-wide", "sget-object", "sget-boolean",
                         "sget-byte", "sget-char", "sget-short")):
    _op(0x60 + _i, _n, "21c")
for _i, _n in enumerate(("sput", "sput-wide", "sput-object", "sput-boolean",
                         "sput-byte", "sput-char", "sput-short")):
    _op(0x67 + _i, _n, "21c")
for _i, _n in enumerate(("invoke-virtual", "invoke-super", "invoke-direct",
                         "invoke-static", "invoke-interface")):
    _op(0x6E + _i, _n, "35c")
    _op(0x74 + _i, _n + "/range", "3rc")
for _i, _n in enumerate(("neg-int", "not-int", "neg-long", "not-long",
                         "neg-float", "neg-double", "int-to-long",
                         "int-to-float", "int-to-double", "long-to-int",
                         "long-to-float", "long-to-double", "float-to-int",
                         "float-to-long", "float-to-double", "double-to-int",
                         "double-to-long", "double-to-float", "int-to-byte",
                         "int-to-char", "int-to-short")):
    _op(0x7B + _i, _n, "12x")
for _i, _n in enumerate(("add-int", "sub-int", "mul-int", "div-int",
                         "rem-int", "and-int", "or-int", "xor-int", "shl-int",
                         "shr-int", "ushr-int", "add-long", "sub-long",
                         "mul-long", "div-long", "rem-long", "and-long",
                         "or-long", "xor-long", "shl-long", "shr-long",
                         "ushr-long", "add-float", "sub-float", "mul-float",
                         "div-float", "rem-float", "add-double", "sub-double",
                         "mul-double", "div-double", "rem-double")):
    _op(0x90 + _i, _n, "23x")
for _i, _n in enumerate(("add-int/lit16", "rsub-int", "mul-int/lit16",
                         "div-int/lit16", "rem-int/lit16", "and-int/lit16",
                         "or-int/lit16", "xor-int/lit16")):
    _op(0xB0 + _i, _n, "22s")
for _i, _n in enumerate(("add-int/lit8", "rsub-int/lit8", "mul-int/lit8",
                         "div-int/lit8", "rem-int/lit8", "and-int/lit8",
                         "or-int/lit8", "xor-int/lit8", "shl-int/lit8",
                         "shr-int/lit8", "ushr-int/lit8")):
    _op(0xD8 + _i, _n, "22b")


def decode(insns, pc):
    """-> (name, operands, size in code units)"""
    unit = insns[pc]
    op = unit & 0xFF
    if op not in FMT:
        raise DexError("unimplemented opcode 0x%02x at %d" % (op, pc))
    name = NAME[op]
    f = FMT[op]
    a = (unit >> 8) & 0xFF       # 8-bit A field
    a4 = (unit >> 8) & 0x0F      # 4-bit A field
    b = (unit >> 12) & 0xF
    c = unit >> 12
    if f == "10x":
        return name, (), 1
    if f == "12x":
        # A (bits 8-11) is the destination, B (bits 12-15) the source
        return name, (a4, b), 1
    if f == "11n":
        lit = b
        if lit > 7:
            lit -= 16
        return name, (lit, (unit >> 8) & 0xF), 1
    if f == "11x":
        return name, (a,), 1
    if f == "10t":
        off = a
        if off > 127:
            off -= 256
        return name, (off,), 1
    if f == "20t":
        return name, (insns[pc + 1],), 2
    if f == "22x":
        return name, (a, insns[pc + 1]), 2
    if f == "21t":
        off = insns[pc + 1]
        if off > 32767:
            off -= 65536
        return name, (a, off), 2
    if f == "21s":
        v = insns[pc + 1]
        if v > 32767:
            v -= 65536
        return name, (a, v), 2
    if f == "21h":
        return name, (a, insns[pc + 1] << 16), 2
    if f == "21c":
        return name, (a, insns[pc + 1]), 2
    if f == "23x":
        u2 = insns[pc + 1]
        return name, (a, u2 & 0xFF, u2 >> 8), 2
    if f == "22b":
        u2 = insns[pc + 1]
        lit = u2 >> 8
        if lit > 127:
            lit -= 256
        return name, (a, u2 & 0xFF, lit), 2
    if f == "22t":
        off = insns[pc + 1]
        if off > 32767:
            off -= 65536
        return name, (a4, b, off), 2
    if f == "22s":
        v = insns[pc + 1]
        if v > 32767:
            v -= 65536
        return name, (a, b, v), 2
    if f == "22c":
        return name, (a4, b, insns[pc + 1]), 2
    if f == "30t":
        off = insns[pc + 1] | (insns[pc + 2] << 16)
        if off > 0x7FFFFFFF:
            off -= 0x100000000
        return name, (off,), 3
    if f == "32x":
        return name, (insns[pc + 1], insns[pc + 2]), 3
    if f == "31i":
        v = insns[pc + 1] | (insns[pc + 2] << 16)
        if v > 0x7FFFFFFF:
            v -= 0x100000000
        return name, (a, v), 3
    if f == "31t":
        off = insns[pc + 1] | (insns[pc + 2] << 16)
        if off > 0x7FFFFFFF:
            off -= 0x100000000
        return name, (a, off), 3
    if f == "31c":
        return name, (a, insns[pc + 1] | (insns[pc + 2] << 16)), 3
    if f == "51l":
        lo = insns[pc + 1] | (insns[pc + 2] << 16)
        hi = insns[pc + 3] | (insns[pc + 4] << 16)
        return name, (a, (hi << 32) | lo), 5
    if f == "35c":
        count = (unit >> 12) & 0xF
        g = (unit >> 8) & 0xF
        idx = insns[pc + 1]
        w = insns[pc + 2]
        regs = [w & 0xF, (w >> 4) & 0xF, (w >> 8) & 0xF, (w >> 12) & 0xF, g]
        return name, (idx, regs[:count]), 3
    if f == "3rc":
        idx = insns[pc + 1]
        first = insns[pc + 2]
        return name, (idx, first, a), 3
    raise DexError("format %s" % f)


# --------------------------------------------------------------- VM
def i32(v):
    v &= 0xFFFFFFFF
    return v - 0x100000000 if v >= 0x80000000 else v


def i64(v):
    v &= 0xFFFFFFFFFFFFFFFF
    return v - (1 << 64) if v >= (1 << 63) else v


def f32(v):
    return struct.unpack("<f", struct.pack("<f", float(v)))[0]


class Vm(object):
    def __init__(self, dex, out=None):
        self.d = dex
        self.out = out or (lambda s: sys.stdout.write(s + "\n"))
        self.statics = {}
        self.heap = []
        self.inited = set()
        self.depth = 0
        self.max_depth = 3000
        self.steps = 0
        self.max_steps = 60000000
        self.trace = False
        self.last_method = None

    # -- class init ----------------------------------------------------
    def ensure_init(self, cls):
        if cls in self.inited:
            return
        self.inited.add(cls)
        c = self.d.classes.get(cls)
        if c is None:
            return
        if c.static_values:
            for i, (name, typ, acc) in enumerate(c.static_fields):
                v = c.static_values[i] if i < len(c.static_values) else None
                self.statics[(cls, name)] = self.coerce(typ, v)
        m = c.direct.get((cls, "<clinit>", (), "V"))
        if m is not None:
            self.invoke_method(m, [])

    def coerce(self, typ, v):
        if typ in ("I", "S", "B", "C", "Z"):
            return 0 if v is None else v
        if typ == "J":
            return 0 if v is None else v
        return v

    def get_static(self, cls, name, typ):
        self.ensure_init(cls)
        key = (cls, name)
        if key in self.statics:
            return self.statics[key]
        return 0 if typ in ("I", "J", "S", "B", "C", "Z") else None

    def set_static(self, cls, name, typ, value):
        self.ensure_init(cls)
        self.statics[(cls, name)] = value

    # -- objects -------------------------------------------------------
    def new_instance(self, cls):
        o = Obj(cls)
        self.heap.append(o)
        return o

    def new_array(self, typ, size):
        if size < 0:
            raise JavaThrow(self.make_exception("Ljava/lang/NegativeArraySizeException;"))
        if typ == "[J" or typ == "[D":
            fill = 0
        elif typ in ("[I", "[S", "[B", "[C", "[Z"):
            fill = 0
        else:
            fill = None
        return Arr(typ, [fill] * size)

    def make_exception(self, cls, msg=None):
        o = self.new_instance(cls)
        if msg is not None:
            o.fields[(cls, "detailMessage")] = msg
        return o

    def cast_ok(self, value, typ):
        if value is None:
            return True
        if typ.startswith("["):
            return isinstance(value, (Arr, list))
        cur = value.cls if isinstance(value, Obj) else None
        while cur is not None and cur in self.d.classes:
            if cur == typ or typ in self.d.classes[cur].interfaces:
                return True
            cur = self.d.classes[cur].super
        return cur == typ

    # -- fields --------------------------------------------------------
    def find_field(self, cls, name):
        cur = cls
        seen = 0
        while cur is not None and seen < 64:
            c = self.d.classes.get(cur)
            if c is None:
                break
            for (fname, ftyp) in c.instance_fields:
                if fname == name:
                    return (cur, fname)
            for (fname, ftyp, acc) in c.static_fields:
                if fname == name:
                    return (cur, fname)
            cur = c.super
            seen += 1
        return None

    def get_field(self, obj, owner, name, typ):
        key = self.find_field(obj.cls, name) or (owner, name)
        if key in obj.fields:
            return obj.fields[key]
        return 0 if typ in ("I", "J", "S", "B", "C", "Z", "F", "D") else None

    def put_field(self, obj, owner, name, typ, value):
        key = self.find_field(obj.cls, name) or (owner, name)
        obj.fields[key] = value

    # -- methods -------------------------------------------------------
    def lookup(self, cls, name, params, ret, virtual=False):
        cur = cls
        seen = 0
        while cur is not None and seen < 64:
            c = self.d.classes.get(cur)
            if c is not None:
                key = (cur, name, tuple(params), ret)
                if key in c.virtual:
                    return c.virtual[key]
                if key in c.direct:
                    return c.direct[key]
            cur = c.super if c is not None else None
            seen += 1
        return None

    def invoke(self, cls, name, params, ret, args, this=None):
        if cls == "Ljava/lang/String;" and name != "<init>":
            args = [_sv(a) for a in args]
            this = _sv(this)
        elif cls in ("Ljava/lang/StringBuilder;", "Ljava/lang/StringBuffer;"):
            args = [_sv(a) for a in args]
        key = "L%s;->%s(%s)%s" % (cls[1:-1], name, "".join(params), ret) \
            if cls.startswith("L") else "%s->%s(%s)%s" % (cls, name,
                                                          "".join(params), ret)
        if cls not in self.d.classes:
            native = NATIVES.get(key)
            if native is not None:
                return native(self, args, this)
        # A virtual or interface call is dispatched on the receiver's real
        # class, not on the class named in the instruction.
        dispatch = cls
        if this is not None and isinstance(this, Obj):
            dispatch = this.cls
        m = self.lookup(dispatch, name, params, ret)
        if m is None:
            m = self.lookup(cls, name, params, ret)
        if m is None:
            native = NATIVES.get(key)
            if native is not None:
                return native(self, args, this)
            raise DexError("no method %s" % key)
        if m.code is None:
            native = NATIVES.get(key)
            if native is not None:
                return native(self, args, this)
            raise DexError("no code for %s" % key)
        allargs = ([this] if this is not None else []) + list(args)
        return self.invoke_method(m, allargs)

    def invoke_method(self, m, args):
        body = self.d.code(m.code)
        regs = [0] * body["registers"]
        base = body["registers"] - body["ins"]
        for i, v in enumerate(args):
            regs[base + i] = v
        self.depth += 1
        if self.depth > self.max_depth:
            self.depth -= 1
            raise DexError("call depth exceeded (recursion?)")
        saved = self.last_method
        self.last_method = m
        try:
            return self.execute(body, regs, m)
        finally:
            self.depth -= 1
            self.last_method = saved

    # -- interpreter ---------------------------------------------------
    def execute(self, body, regs, m):
        _CURRENT_VM[0] = self
        insns = body["insns"]
        pc = 0
        tries = body["tries"]
        n = len(insns)
        result = None
        while True:
            if pc < 0 or pc >= n:
                raise DexError("%s ran off the end at pc %d" % (m, pc))
            self.steps += 1
            if self.steps > self.max_steps:
                raise DexError("step budget exhausted in %s" % m)
            if self.trace:
                sys.stderr.write("%s @%d %s\n" % (m.name, pc,
                                                  NAME.get(insns[pc] & 0xFF, "?")))
            try:
                r = self.step(insns, pc, regs, m, body)
            except JavaThrow as e:
                h = self.handler(tries, pc, e.obj, self)
                if h is None:
                    raise
                pc = h
                regs[0] = e.obj
                self.exception = e.obj
                continue
            if r is None:
                return None
            pc2, ret, value = r
            if pc2 < 0:
                return value
            pc = pc2

    def handler(self, tries, pc, exc, vm):
        for (start, count, pairs, catch_all) in tries:
            if start <= pc < start + count:
                for (typ, addr) in pairs:
                    if self.cast_ok(exc, typ):
                        return addr
                if catch_all is not None:
                    return catch_all
        return None

    def step(self, insns, pc, regs, m, body):
        name, ops, size = decode(insns, pc)
        nxt = pc + size
        d = self.d

        # ---- moves and results
        if name == "nop":
            return (nxt, None, None)
        if name in ("move", "move/from16", "move/16"):
            regs[ops[0]] = regs[ops[1]]
            return (nxt, None, None)
        if name in ("move-object", "move-object/from16", "move-object/16"):
            regs[ops[0]] = regs[ops[1]]
            return (nxt, None, None)
        if name in ("move-wide", "move-wide/from16", "move-wide/16"):
            regs[ops[0]] = regs[ops[1]]
            regs[ops[0] + 1] = regs[ops[1] + 1]
            return (nxt, None, None)
        if name in ("move-result", "move-result-object"):
            regs[ops[0]] = self.result
            return (nxt, None, None)
        if name == "move-result-wide":
            regs[ops[0]] = self.result
            regs[ops[0] + 1] = 0
            return (nxt, None, None)
        if name == "move-exception":
            regs[ops[0]] = self.exception
            return (nxt, None, None)

        # ---- constants
        if name == "const/4":
            regs[ops[1]] = i32(ops[0])
            return (nxt, None, None)
        if name in ("const/16", "const"):
            regs[ops[0]] = i32(ops[1])
            return (nxt, None, None)
        if name == "const/high16":
            regs[ops[0]] = i32(ops[1])
            return (nxt, None, None)
        if name in ("const-wide/16", "const-wide/32", "const-wide"):
            regs[ops[0]] = i64(ops[1])
            regs[ops[0] + 1] = 0
            return (nxt, None, None)
        if name == "const-wide/high16":
            regs[ops[0]] = i64(ops[1])
            regs[ops[0] + 1] = 0
            return (nxt, None, None)
        if name == "const-string":
            regs[ops[0]] = d.strings[ops[1]]
            return (nxt, None, None)
        if name == "const-class":
            regs[ops[0]] = ("class", d.types[ops[1]])
            return (nxt, None, None)

        # ---- returns
        if name == "return-void":
            return (-1, None, None)
        if name in ("return", "return-object"):
            return (-1, None, regs[ops[0]])
        if name == "return-wide":
            return (-1, None, regs[ops[0]])

        # ---- control flow
        if name in ("goto", "goto/16", "goto/32"):
            return (pc + ops[0], None, None)
        if name in ("if-eq", "if-ne", "if-lt", "if-ge", "if-gt", "if-le"):
            a, b, off = ops
            x = regs[a]
            y = regs[b]
            if name == "if-eq":
                take = x == y
            elif name == "if-ne":
                take = x != y
            elif name == "if-lt":
                take = x < y
            elif name == "if-ge":
                take = x >= y
            elif name == "if-gt":
                take = x > y
            else:
                take = x <= y
            return (pc + off if take else nxt, None, None)
        if name in ("if-eqz", "if-nez", "if-ltz", "if-gez", "if-gtz", "if-lez"):
            a, off = ops
            x = regs[a]
            # a reference is only ever tested against null (if-eqz/if-nez),
            # so the ordering tests must not be evaluated eagerly
            if name == "if-eqz":
                take = (x is None or x == 0)
            elif name == "if-nez":
                take = not (x is None or x == 0)
            elif name == "if-ltz":
                take = x < 0
            elif name == "if-gez":
                take = x >= 0
            elif name == "if-gtz":
                take = x > 0
            else:
                take = x <= 0
            return (pc + off if take else nxt, None, None)
        if name == "packed-switch":
            # payload: u2 ident, u2 size, s4 first_key, s4 targets[size]
            a, off = ops
            val = regs[a]
            payload = pc + off
            if insns[payload] != 0x0100:
                raise DexError("bad packed-switch payload")
            count = insns[payload + 1]
            first = _s32(insns[payload + 2], insns[payload + 3])
            if first <= val < first + count:
                base = payload + 4 + 2 * (val - first)
                return (pc + _s32(insns[base], insns[base + 1]), None, None)
            return (nxt, None, None)
        if name == "sparse-switch":
            # payload: u2 ident, u2 size, s4 keys[size], s4 targets[size]
            a, off = ops
            val = regs[a] & 0xFFFFFFFF
            payload = pc + off
            if insns[payload] != 0x0200:
                raise DexError("bad sparse-switch payload")
            count = insns[payload + 1]
            for i in range(count):
                k = payload + 2 + 2 * i
                if (insns[k] | (insns[k + 1] << 16)) == val:
                    # keys occupy 2 units each, so do the targets that follow
                    t = payload + 2 + 2 * count + 2 * i
                    return (pc + _s32(insns[t], insns[t + 1]), None, None)
            return (nxt, None, None)

        # ---- arithmetic
        if name in _BIN:
            fn = _BIN[name]
            regs[ops[0]] = fn(regs[ops[1]], regs[ops[2]])
            return (nxt, None, None)
        if name in _UN:
            fn = _UN[name]
            regs[ops[0]] = fn(regs[ops[1]])
            if name in ("neg-long", "not-long"):
                regs[ops[0] + 1] = 0
            if name in ("int-to-long", "int-to-double", "float-to-long",
                        "float-to-double"):
                regs[ops[0] + 1] = 0
            return (nxt, None, None)
        if name in _LIT8 or name in _LIT16:
            fn = (_LIT8.get(name) or _LIT16.get(name))
            regs[ops[0]] = fn(regs[ops[1]], ops[2])
            return (nxt, None, None)
        if name in ("cmp-long", "cmpl-float", "cmpg-float", "cmpl-double",
                    "cmpg-double"):
            x = regs[ops[1]]
            y = regs[ops[2]]
            if name == "cmp-long":
                regs[ops[0]] = -1 if x < y else (0 if x == y else 1)
            else:
                nan_high = name.startswith("cmpg")
                if x != x or y != y:
                    regs[ops[0]] = 1 if nan_high else -1
                else:
                    regs[ops[0]] = -1 if x < y else (0 if x == y else 1)
            return (nxt, None, None)

        # ---- fields
        if name.startswith("sget"):
            cls, fname, typ = d.fields[ops[1]]
            self.ensure_init(cls)
            v = self.get_static(cls, fname, typ)
            if v is None and typ in ("I", "J", "S", "B", "C", "Z"):
                v = 0
            regs[ops[0]] = v
            return (nxt, None, None)
        if name.startswith("sput"):
            cls, fname, typ = d.fields[ops[1]]
            self.set_static(cls, fname, typ, regs[ops[0]])
            return (nxt, None, None)
        if name.startswith("iget"):
            cls, fname, typ = d.fields[ops[2]]
            obj = regs[ops[1]]
            if obj is None:
                raise JavaThrow(self.make_exception("Ljava/lang/NullPointerException;"))
            regs[ops[0]] = self.get_field(obj, cls, fname, typ)
            return (nxt, None, None)
        if name.startswith("iput"):
            cls, fname, typ = d.fields[ops[2]]
            obj = regs[ops[1]]
            if obj is None:
                raise JavaThrow(self.make_exception("Ljava/lang/NullPointerException;"))
            self.put_field(obj, cls, fname, typ, regs[ops[0]])
            return (nxt, None, None)

        # ---- arrays
        if name.startswith("aget"):
            arr = regs[ops[1]]
            idx = regs[ops[2]]
            if arr is None:
                raise JavaThrow(self.make_exception("Ljava/lang/NullPointerException;"))
            if idx < 0 or idx >= len(arr.a):
                raise JavaThrow(self.make_exception(
                    "Ljava/lang/ArrayIndexOutOfBoundsException;"))
            v = arr.a[idx]
            if name == "aget-byte":
                v = _signed(v & 0xFF, 8)
            elif name == "aget-boolean":
                v = 1 if v else 0
            elif name == "aget-char":
                v = v & 0xFFFF
            elif name == "aget-short":
                v = _signed(v & 0xFFFF, 16)
            regs[ops[0]] = v
            return (nxt, None, None)
        if name.startswith("aput"):
            arr = regs[ops[1]]
            idx = regs[ops[2]]
            val = regs[ops[0]]
            if arr is None:
                raise JavaThrow(self.make_exception("Ljava/lang/NullPointerException;"))
            if idx < 0 or idx >= len(arr.a):
                raise JavaThrow(self.make_exception(
                    "Ljava/lang/ArrayIndexOutOfBoundsException;"))
            if name == "aput-byte":
                val = val & 0xFF
            elif name == "aput-boolean":
                val = 1 if val else 0
            elif name == "aput-char":
                val = val & 0xFFFF
            elif name == "aput-short":
                val = val & 0xFFFF
            elif name == "aput":
                val = i32(val)
            arr.a[idx] = val
            return (nxt, None, None)
        if name == "array-length":
            arr = regs[ops[1]]
            if arr is None:
                raise JavaThrow(self.make_exception("Ljava/lang/NullPointerException;"))
            regs[ops[0]] = len(arr.a)
            return (nxt, None, None)
        if name == "new-array":
            size_ = regs[ops[1]]
            regs[ops[0]] = self.new_array(d.types[ops[2]], size_)
            return (nxt, None, None)
        if name == "fill-array-data":
            arr = regs[ops[0]]
            payload = pc + ops[1]
            elem = insns[payload + 1]
            size_ = insns[payload + 2] | (insns[payload + 3] << 16)
            # width in code units per element: (elem+1)/2
            words = (elem + 1) // 2
            for i in range(size_):
                base = payload + 4 + i * words
                if words == 1:
                    v = insns[base]
                elif words == 2:
                    v = insns[base] | (insns[base + 1] << 16)
                elif words == 4:
                    v = (insns[base] | (insns[base + 1] << 16)
                         | (insns[base + 2] << 32) | (insns[base + 3] << 48))
                else:
                    v = insns[base]
                arr.a[i] = v
            return (nxt, None, None)

        # ---- objects
        if name == "new-instance":
            typ = d.types[ops[1]]
            regs[ops[0]] = StrObj(typ) if typ == "Ljava/lang/String;" \
                else self.new_instance(typ)
            return (nxt, None, None)
        if name == "check-cast":
            return (nxt, None, None)
        if name == "instance-of":
            regs[ops[0]] = 1 if self.cast_ok(regs[ops[1]], d.types[ops[2]]) else 0
            return (nxt, None, None)
        if name == "throw":
            raise JavaThrow(regs[ops[0]])
        if name in ("monitor-enter", "monitor-exit"):
            return (nxt, None, None)

        # ---- invokes
        if name.startswith("invoke-"):
            return (self.do_invoke(name, ops, regs, pc, nxt), None, None)

        raise DexError("unimplemented instruction %s in %s" % (name, m))

    def do_invoke(self, name, ops, regs, pc, nxt):
        d = self.d
        if name.endswith("/range"):
            midx, first, count = ops
            idxs = list(range(first, first + count))
        else:
            midx, regs_list = ops
            idxs = list(regs_list)
        cls, mname, params, ret = d.methods[midx]
        self.ensure_init(cls)
        kind = name.split("-")[1].split("/")[0]
        # split the argument registers according to the prototype
        vals = []
        i = 0
        if kind in ("virtual", "super", "direct", "interface"):
            this = regs[idxs[0]]
            i = 1
        else:
            this = None
        for p in params:
            vals.append(regs[idxs[i]])
            i += 2 if p in ("J", "D") else 1
        self.result = None
        self.exception = None
        try:
            r = self.invoke(cls, mname, params, ret, vals, this)
        except JavaThrow as e:
            self.exception = e.obj
            self.result = None
            raise
        self.result = r
        if ret in ("J", "D") and r is not None:
            self._pending_wide = r
        return nxt


# --- arithmetic helpers -------------------------------------------------
_BIN = {
    "add-int": lambda a, b: i32(a + b),
    "sub-int": lambda a, b: i32(a - b),
    "mul-int": lambda a, b: i32(a * b),
    "div-int": lambda a, b: i32(int(a / b)) if b else _div0(),
    "rem-int": lambda a, b: i32(a - int(a / b) * b) if b else _div0(),
    "and-int": lambda a, b: i32(a & b),
    "or-int": lambda a, b: i32(a | b),
    "xor-int": lambda a, b: i32(a ^ b),
    "shl-int": lambda a, b: i32(a << (b & 31)),
    "shr-int": lambda a, b: i32(a >> (b & 31)),
    "ushr-int": lambda a, b: i32((a & 0xFFFFFFFF) >> (b & 31)),
    "add-long": lambda a, b: i64(a + b),
    "sub-long": lambda a, b: i64(a - b),
    "mul-long": lambda a, b: i64(a * b),
    "div-long": lambda a, b: i64(int(a / b)) if b else _div0(),
    "rem-long": lambda a, b: i64(a - int(a / b) * b) if b else _div0(),
    "and-long": lambda a, b: i64(a & b),
    "or-long": lambda a, b: i64(a | b),
    "xor-long": lambda a, b: i64(a ^ b),
    "shl-long": lambda a, b: i64(a << (b & 63)),
    "shr-long": lambda a, b: i64(a >> (b & 63)),
    "ushr-long": lambda a, b: i64((a & 0xFFFFFFFFFFFFFFFF) >> (b & 63)),
    "add-float": lambda a, b: f32(f32(a) + f32(b)),
    "sub-float": lambda a, b: f32(f32(a) - f32(b)),
    "mul-float": lambda a, b: f32(f32(a) * f32(b)),
    "div-float": lambda a, b: f32(f32(a) / f32(b)),
    "rem-float": lambda a, b: f32(f32(a) % f32(b)),
    "add-double": lambda a, b: a + b,
    "sub-double": lambda a, b: a - b,
    "mul-double": lambda a, b: a * b,
    "div-double": lambda a, b: a / b,
    "rem-double": lambda a, b: a % b,
}

_UN = {
    "neg-int": lambda a: i32(-a),
    "not-int": lambda a: i32(~a),
    "neg-long": lambda a: i64(-a),
    "not-long": lambda a: i64(~a),
    "neg-float": lambda a: f32(-a),
    "neg-double": lambda a: -a,
    "int-to-long": lambda a: i64(a),
    "int-to-float": lambda a: f32(a),
    "int-to-double": lambda a: float(a),
    "long-to-int": lambda a: i32(a),
    "long-to-float": lambda a: f32(a),
    "long-to-double": lambda a: float(a),
    "float-to-int": lambda a: i32(int(a)),
    "float-to-long": lambda a: i64(int(a)),
    "float-to-double": lambda a: float(a),
    "double-to-int": lambda a: i32(int(a)),
    "double-to-long": lambda a: i64(int(a)),
    "double-to-float": lambda a: f32(a),
    "int-to-byte": lambda a: _signed(a & 0xFF, 8),
    "int-to-char": lambda a: a & 0xFFFF,
    "int-to-short": lambda a: _signed(a & 0xFFFF, 16),
}

_LIT8 = {
    "add-int/lit8": lambda a, b: i32(a + b),
    "rsub-int/lit8": lambda a, b: i32(b - a),
    "mul-int/lit8": lambda a, b: i32(a * b),
    "div-int/lit8": lambda a, b: i32(int(a / b)) if b else _div0(),
    "rem-int/lit8": lambda a, b: i32(a - int(a / b) * b) if b else _div0(),
    "and-int/lit8": lambda a, b: i32(a & b),
    "or-int/lit8": lambda a, b: i32(a | b),
    "xor-int/lit8": lambda a, b: i32(a ^ b),
    "shl-int/lit8": lambda a, b: i32(a << (b & 31)),
    "shr-int/lit8": lambda a, b: i32(a >> (b & 31)),
    "ushr-int/lit8": lambda a, b: i32((a & 0xFFFFFFFF) >> (b & 31)),
}

_LIT16 = {
    "add-int/lit16": lambda a, b: i32(a + b),
    "rsub-int": lambda a, b: i32(b - a),
    "mul-int/lit16": lambda a, b: i32(a * b),
    "div-int/lit16": lambda a, b: i32(int(a / b)) if b else _div0(),
    "rem-int/lit16": lambda a, b: i32(a - int(a / b) * b) if b else _div0(),
    "and-int/lit16": lambda a, b: i32(a & b),
    "or-int/lit16": lambda a, b: i32(a | b),
    "xor-int/lit16": lambda a, b: i32(a ^ b),
}


_CURRENT_VM = [None]


def _div0():
    """Integer division by zero throws ArithmeticException, and the core
    catches it (Probe tests exactly that)."""
    vm = _CURRENT_VM[0]
    exc = vm.make_exception("Ljava/lang/ArithmeticException;", "/ by zero")
    raise JavaThrow(exc)


# --- native library -----------------------------------------------------
NATIVES = {}


def native(sig):
    def deco(fn):
        NATIVES[sig] = fn
        return fn
    return deco


def _str(v):
    return v if isinstance(v, str) else None


@native("Ljava/lang/Object;-><init>()V")
def _object_init(vm, args, this):
    return None


@native("Ljava/lang/Object;->hashCode()I")
def _object_hash(vm, args, this):
    return id(this) & 0x7FFFFFFF


@native("Ljava/lang/Object;->equals(Ljava/lang/Object;)Z")
def _object_equals(vm, args, this):
    return 1 if this is args[0] else 0


def _object_getclass(vm, args, this):
    return ("class", this.cls if isinstance(this, Obj) else "Ljava/lang/Object;")


@native("Ljava/lang/Object;->toString()Ljava/lang/String;")
def _object_tostring(vm, args, this):
    if isinstance(this, str):
        return this
    if isinstance(this, Arr):
        return "[%s@%x" % (this.typ, id(this) & 0xFFFF)
    return "%s@%x" % (this.cls[1:-1], id(this) & 0xFFFF)


@native("Ljava/lang/String;-><init>([BII)V")
def _string_init_bytes(vm, args, this):
    data, off, ln = args
    raw = bytearray([data.a[off + i] & 0xFF for i in range(ln)])
    this.fields[("str", "value")] = Str(_decode_str(raw))
    return None


@native("Ljava/lang/String;-><init>([B)V")
def _string_init_bytes_all(vm, args, this):
    data = args[0]
    raw = bytearray([v & 0xFF for v in data.a])
    this.fields[("str", "value")] = Str(_decode_str(raw))
    return None


@native("Ljava/lang/String;-><init>([BLjava/lang/String;)V")
def _string_init_bytes_cs(vm, args, this):
    data = args[0]
    raw = bytearray([v & 0xFF for v in data.a])
    this.fields[("str", "value")] = Str(_decode_str(raw))
    return None


@native("Ljava/lang/String;->length()I")
def _string_length(vm, args, this):
    return len(this)


@native("Ljava/lang/String;->charAt(I)C")
def _string_charat(vm, args, this):
    return ord(this[args[0]])


@native("Ljava/lang/String;->equals(Ljava/lang/Object;)Z")
def _string_equals(vm, args, this):
    return 1 if this == args[0] else 0


@native("Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z")
def _string_equals_ignore_case(vm, args, this):
    other = args[0]
    return 1 if (other is not None and this.lower() == other.lower()) else 0


@native("Ljava/lang/String;->compareTo(Ljava/lang/String;)I")
def _string_compare_to(vm, args, this):
    other = args[0]
    n = min(len(this), len(other))
    for i in range(n):
        d = ord(this[i]) - ord(other[i])
        if d:
            return d
    return len(this) - len(other)


@native("Ljava/lang/String;->compareToIgnoreCase(Ljava/lang/String;)I")
def _string_compare_to_ic(vm, args, this):
    return _string_compare_to(vm, [args[0].lower() if args[0] else args[0]],
                              this.lower())


@native("Ljava/lang/String;->hashCode()I")
def _string_hash(vm, args, this):
    h = 0
    for ch in this:
        h = i32(31 * h + ord(ch))
    return h


@native("Ljava/lang/String;->toString()Ljava/lang/String;")
def _string_tostring(vm, args, this):
    return this


@native("Ljava/lang/String;->indexOf(Ljava/lang/String;)I")
def _string_indexof(vm, args, this):
    return this.find(args[0])


@native("Ljava/lang/String;->indexOf(I)I")
def _string_indexof_c(vm, args, this):
    return this.find(chr(args[0]))


@native("Ljava/lang/String;->indexOf(II)I")
def _string_indexof_ic(vm, args, this):
    return this.find(chr(args[0] & 0xFFFF), args[1])


@native("Ljava/lang/String;->lastIndexOf(I)I")
def _string_last_indexof_c(vm, args, this):
    return this.rfind(chr(args[0] & 0xFFFF))


@native("Ljava/lang/String;->lastIndexOf(Ljava/lang/String;)I")
def _string_last_indexof_s(vm, args, this):
    return this.rfind(args[0])


@native("Ljava/lang/String;->replace(CC)Ljava/lang/String;")
def _string_replace(vm, args, this):
    return this.replace(chr(args[0] & 0xFFFF), chr(args[1] & 0xFFFF))


@native("Ljava/lang/String;->substring(I)Ljava/lang/String;")
def _string_sub1(vm, args, this):
    return this[args[0]:]


@native("Ljava/lang/String;->substring(II)Ljava/lang/String;")
def _string_sub2(vm, args, this):
    return this[args[0]:args[1]]


@native("Ljava/lang/String;->toLowerCase()Ljava/lang/String;")
def _string_lower(vm, args, this):
    return this.lower()


@native("Ljava/lang/String;->toUpperCase()Ljava/lang/String;")
def _string_upper(vm, args, this):
    return this.upper()


@native("Ljava/lang/String;->trim()Ljava/lang/String;")
def _string_trim(vm, args, this):
    return this.strip()


@native("Ljava/lang/String;->startsWith(Ljava/lang/String;)Z")
def _string_startswith(vm, args, this):
    return 1 if this.startswith(args[0]) else 0


@native("Ljava/lang/String;->endsWith(Ljava/lang/String;)Z")
def _string_endswith(vm, args, this):
    return 1 if this.endswith(args[0]) else 0


@native("Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;")
def _string_concat(vm, args, this):
    return this + args[0]


@native("Ljava/lang/String;->valueOf(I)Ljava/lang/String;")
def _string_valueof_i(vm, args, this):
    return str(args[0])


@native("Ljava/lang/String;->valueOf(J)Ljava/lang/String;")
def _string_valueof_j(vm, args, this):
    return str(args[0])


@native("Ljava/lang/String;->valueOf(C)Ljava/lang/String;")
def _string_valueof_c(vm, args, this):
    return chr(args[0] & 0xFFFF)


@native("Ljava/lang/String;->valueOf(Z)Ljava/lang/String;")
def _string_valueof_z(vm, args, this):
    return "true" if args[0] else "false"


@native("Ljava/lang/String;->valueOf(Ljava/lang/Object;)Ljava/lang/String;")
def _string_valueof_o(vm, args, this):
    v = args[0]
    if v is None:
        return "null"
    if isinstance(v, str):
        return v
    return vm.invoke("Ljava/lang/Object;", "toString", (), "Ljava/lang/String;", [], v)


def _sb(vm, this):
    key = ("sb", "buf")
    if key not in this.fields:
        this.fields[key] = []
    return this.fields[key]


@native("Ljava/lang/StringBuilder;-><init>()V")
def _sbd_init(vm, args, this):
    this.fields[("sb", "buf")] = []
    return None


@native("Ljava/lang/StringBuilder;-><init>(I)V")
def _sbd_init_cap(vm, args, this):
    this.fields[("sb", "buf")] = []
    return None


@native("Ljava/lang/StringBuilder;-><init>(Ljava/lang/String;)V")
def _sbd_init_str(vm, args, this):
    this.fields[("sb", "buf")] = [args[0] or ""]
    return None


@native("Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;")
def _sbd_append_str(vm, args, this):
    _sb(vm, this).append("null" if args[0] is None else args[0])
    return this


@native("Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;")
def _sbd_append_obj(vm, args, this):
    v = args[0]
    if v is None:
        s = "null"
    elif isinstance(v, str):
        s = v
    else:
        s = vm.invoke("Ljava/lang/Object;", "toString", (), "Ljava/lang/String;", [], v)
    _sb(vm, this).append(s)
    return this


@native("Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;")
def _sbd_append_c(vm, args, this):
    _sb(vm, this).append(chr(args[0] & 0xFFFF))
    return this


@native("Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;")
def _sbd_append_z(vm, args, this):
    _sb(vm, this).append("true" if args[0] else "false")
    return this


@native("Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;")
def _sbd_append_i(vm, args, this):
    _sb(vm, this).append(str(args[0]))
    return this


@native("Ljava/lang/StringBuilder;->append(J)Ljava/lang/StringBuilder;")
def _sbd_append_j(vm, args, this):
    _sb(vm, this).append(str(args[0]))
    return this


@native("Ljava/lang/StringBuilder;->append(F)Ljava/lang/StringBuilder;")
def _sbd_append_f(vm, args, this):
    _sb(vm, this).append(str(args[0]))
    return this


@native("Ljava/lang/StringBuilder;->append(D)Ljava/lang/StringBuilder;")
def _sbd_append_d(vm, args, this):
    _sb(vm, this).append(str(args[0]))
    return this


@native("Ljava/lang/StringBuilder;->toString()Ljava/lang/String;")
def _sbd_tostring(vm, args, this):
    return "".join(_sb(vm, this))


@native("Ljava/lang/StringBuilder;->length()I")
def _sbd_length(vm, args, this):
    return len("".join(_sb(vm, this)))


@native("Ljava/lang/StringBuilder;->setLength(I)V")
def _sbd_setlength(vm, args, this):
    s = "".join(_sb(vm, this))[:args[0]]
    this.fields[("sb", "buf")] = [s]
    return None


@native("Ljava/lang/StringBuilder;->charAt(I)C")
def _sbd_charat(vm, args, this):
    return ord("".join(_sb(vm, this))[args[0]])


@native("Ljava/lang/StringBuffer;-><init>()V")
def _sb_init(vm, args, this):
    this.fields[("sb", "buf")] = []
    return None


@native("Ljava/lang/StringBuffer;->append(Ljava/lang/String;)Ljava/lang/StringBuffer;")
def _sb_append_str(vm, args, this):
    _sb(vm, this).append("null" if args[0] is None else args[0])
    return this


@native("Ljava/lang/StringBuffer;->append(I)Ljava/lang/StringBuffer;")
def _sb_append_i(vm, args, this):
    _sb(vm, this).append(str(args[0]))
    return this


@native("Ljava/lang/StringBuffer;->append(C)Ljava/lang/StringBuffer;")
def _sb_append_c(vm, args, this):
    _sb(vm, this).append(chr(args[0] & 0xFFFF))
    return this


@native("Ljava/lang/StringBuffer;->toString()Ljava/lang/String;")
def _sb_tostring(vm, args, this):
    return "".join(_sb(vm, this))


@native("Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V")
def _system_arraycopy(vm, args, this):
    src, spos, dst, dpos, count = args
    if count < 0 or spos < 0 or dpos < 0:
        raise JavaThrow(vm.make_exception(
            "Ljava/lang/ArrayIndexOutOfBoundsException;"))
    for i in range(count):
        dst.a[dpos + i] = src.a[spos + i]
    return None


@native("Ljava/lang/System;->currentTimeMillis()J")
def _system_millis(vm, args, this):
    import time
    return int(time.time() * 1000)


@native("Ljava/lang/System;->nanoTime()J")
def _system_nanos(vm, args, this):
    import time
    return int(time.time() * 1e9)


@native("Ljava/io/PrintStream;->println(Ljava/lang/String;)V")
def _println_str(vm, args, this):
    vm.out("" if args[0] is None else args[0])
    return None


@native("Ljava/io/PrintStream;->println(I)V")
def _println_i(vm, args, this):
    vm.out(str(args[0]))
    return None


@native("Ljava/io/PrintStream;->println(J)V")
def _println_j(vm, args, this):
    vm.out(str(args[0]))
    return None


@native("Ljava/io/PrintStream;->println(C)V")
def _println_c(vm, args, this):
    vm.out(chr(args[0] & 0xFFFF))
    return None


@native("Ljava/io/PrintStream;->println(Z)V")
def _println_z(vm, args, this):
    vm.out("true" if args[0] else "false")
    return None


@native("Ljava/io/PrintStream;->println()V")
def _println_none(vm, args, this):
    vm.out("")
    return None


@native("Ljava/io/PrintStream;->print(Ljava/lang/String;)V")
def _print_str(vm, args, this):
    vm.out("" if args[0] is None else args[0])
    return None


@native("Ljava/io/PrintStream;->flush()V")
def _print_flush(vm, args, this):
    return None


# --- java.io.FileInputStream (used by the self-test driver) --------------
@native("Ljava/io/FileInputStream;-><init>(Ljava/lang/String;)V")
def _fis_init(vm, args, this):
    this.fields[("fis", "file")] = open(args[0], "rb")
    return None


def _fis(vm, this):
    key = ("fis", "file")
    if key not in this.fields:
        this.fields[key] = open(this.fields[("fis", "path")], "rb")
    return this.fields[key]


@native("Ljava/io/FileInputStream;->available()I")
def _fis_available(vm, args, this):
    f = _fis(vm, this)
    pos = f.tell()
    f.seek(0, 2)
    end = f.tell()
    f.seek(pos)
    return end - pos


@native("Ljava/io/FileInputStream;->read([BII)I")
def _fis_read(vm, args, this):
    f = _fis(vm, this)
    buf, off, ln = args
    data = f.read(ln)
    if not data:
        return -1
    for i, c in enumerate(data):
        buf.a[off + i] = c
    return len(data)


@native("Ljava/io/FileInputStream;->read([B)I")
def _fis_read_all(vm, args, this):
    f = _fis(vm, this)
    buf = args[0]
    data = f.read(len(buf.a))
    if not data:
        return -1
    for i, c in enumerate(data):
        buf.a[i] = c
    return len(data)


@native("Ljava/io/FileInputStream;->close()V")
def _fis_close(vm, args, this):
    key = ("fis", "file")
    if key in this.fields:
        this.fields[key].close()
    return None


@native("Ljava/io/FileOutputStream;-><init>(Ljava/lang/String;)V")
def _fos_init(vm, args, this):
    this.fields[("fis", "file")] = open(args[0], "wb")
    return None


@native("Ljava/io/FileOutputStream;->write([BII)V")
def _fos_write(vm, args, this):
    f = _fis(vm, this)
    buf, off, ln = args
    f.write(bytes(bytearray([b & 0xFF for b in buf.a[off:off + ln]])))
    return None


@native("Ljava/io/FileOutputStream;->close()V")
def _fos_close(vm, args, this):
    return _fis_close(vm, args, this)


@native("Ljava/lang/Math;->min(II)I")
def _math_min(vm, args, this):
    return min(args[0], args[1])


@native("Ljava/lang/Math;->max(II)I")
def _math_max(vm, args, this):
    return max(args[0], args[1])


@native("Ljava/lang/Math;->abs(I)I")
def _math_abs(vm, args, this):
    return abs(args[0])


@native("Ljava/lang/Math;->abs(J)J")
def _math_abs_j(vm, args, this):
    return abs(args[0])


@native("Ljava/lang/Integer;->toHexString(I)Ljava/lang/String;")
def _integer_hex(vm, args, this):
    return "%x" % (args[0] & 0xFFFFFFFF)


@native("Ljava/lang/Integer;->toString(I)Ljava/lang/String;")
def _integer_tostring(vm, args, this):
    return str(args[0])


@native("Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I")
def _integer_parse(vm, args, this):
    return int(args[0])


@native("Ljava/lang/Long;->toHexString(J)Ljava/lang/String;")
def _long_hex(vm, args, this):
    return "%x" % (args[0] & 0xFFFFFFFFFFFFFFFF)


@native("Ljava/lang/Long;->toString(J)Ljava/lang/String;")
def _long_tostring(vm, args, this):
    return str(args[0])


@native("Ljava/lang/Long;->toString(JI)Ljava/lang/String;")
def _long_tostring_radix(vm, args, this):
    return _radix(args[0], args[1])


@native("Ljava/lang/Integer;->toString(II)Ljava/lang/String;")
def _int_tostring_radix(vm, args, this):
    return _radix(args[0], args[1])


@native("Ljava/lang/Integer;->toBinaryString(I)Ljava/lang/String;")
def _int_bin(vm, args, this):
    return _radix(args[0] & 0xFFFFFFFF, 2)


@native("Ljava/lang/Long;->parseLong(Ljava/lang/String;)J")
def _long_parse(vm, args, this):
    s = (_sv(args[0]) or "").strip()
    neg = s.startswith("-")
    if s[:1] in "+-":
        s = s[1:]
    if s.endswith("L") or s.endswith("l"):
        s = s[:-1]
    v = int(s, 10)
    return i64(-v if neg else v)


@native("Ljava/lang/Long;->parseLong(Ljava/lang/String;I)J")
def _long_parse_radix(vm, args, this):
    return i64(int((_sv(args[0]) or "").strip(), args[1]))


@native("Ljava/lang/Integer;->parseInt(Ljava/lang/String;I)I")
def _int_parse_radix(vm, args, this):
    return i32(int((_sv(args[0]) or "").strip(), args[1]))


@native("Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;")
def _long_valueof(vm, args, this):
    return args[0]


@native("Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;")
def _int_valueof(vm, args, this):
    return args[0]


@native("Ljava/lang/Integer;->intValue()I")
def _int_intvalue(vm, args, this):
    return this


@native("Ljava/lang/Long;->longValue()J")
def _long_longvalue(vm, args, this):
    return this


@native("Ljava/lang/Throwable;-><init>()V")
def _throwable_init(vm, args, this):
    return None


@native("Ljava/lang/Throwable;-><init>(Ljava/lang/String;)V")
def _throwable_init_msg(vm, args, this):
    this.fields[("Ljava/lang/Throwable;", "detailMessage")] = args[0]
    return None


@native("Ljava/lang/Throwable;->getMessage()Ljava/lang/String;")
def _throwable_message(vm, args, this):
    return this.fields.get(("Ljava/lang/Throwable;", "detailMessage"))


@native("Ljava/lang/Throwable;->toString()Ljava/lang/String;")
def _throwable_tostring(vm, args, this):
    msg = this.fields.get(("Ljava/lang/Throwable;", "detailMessage"))
    name = this.cls[1:-1].replace("/", ".")
    return name if msg is None else "%s: %s" % (name, msg)


def _make_exception_natives():
    """Throwable methods have to work for every exception class the core
    catches, and they all inherit them from java.lang.Throwable."""
    NATIVES_EXTRA = {}
    for cls in ("Ljava/lang/Exception;", "Ljava/lang/RuntimeException;",
                "Ljava/lang/IllegalArgumentException;",
                "Ljava/lang/IllegalStateException;", "Ljava/io/IOException;",
                "Ljava/lang/NullPointerException;",
                "Ljava/lang/ArrayIndexOutOfBoundsException;",
                "Ljava/lang/ArithmeticException;",
                "Ljava/lang/NegativeArraySizeException;",
                "Ljava/lang/IndexOutOfBoundsException;",
                "Ljava/lang/StringIndexOutOfBoundsException;",
                "Ljava/lang/UnsupportedOperationException;"):
        # the key is "Lcls;->name(params)ret": keep the descriptor intact
        NATIVES[cls + "-><init>()V"] = _throwable_init
        NATIVES[cls + "-><init>(Ljava/lang/String;)V"] = _throwable_init_msg
        NATIVES[cls + "->getMessage()Ljava/lang/String;"] = _throwable_message
        NATIVES[cls + "->toString()Ljava/lang/String;"] = _throwable_tostring
        NATIVES[cls + "->getClass()Ljava/lang/Class;"] = _object_getclass


_make_exception_natives()


# --------------------------------------------------------------- driver
def run_dex(path, cls, method, params=(), ret="V"):
    data = open(path, "rb").read()
    dex = Dex(data)
    vm = Vm(dex)
    candidates = [tuple(params),
                  ("[Ljava/lang/String;",),          # a main(String[])
                  ()]
    m = None
    for cand in candidates:
        m = vm.lookup(cls, method, cand, ret)
        if m is not None:
            params = cand
            break
    if m is None:
        raise DexError("cannot find %s->%s%s%s" % (cls, method, params, ret))
    return vm.invoke_method(m, [None] * len(params))


def main(argv):
    if len(argv) < 2:
        sys.stderr.write("usage: dvmlite.py <classes.dex> [Class [method]]\n")
        return 2
    path = argv[1]
    cls = argv[2] if len(argv) > 2 else "LMain;"
    method = argv[3] if len(argv) > 3 else "main"
    run_dex(path, cls, method)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
