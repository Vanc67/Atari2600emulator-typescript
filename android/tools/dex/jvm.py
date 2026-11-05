"""Java class file reader.

Only what the smali back end needs: the constant pool, fields, methods and
the ``Code`` attribute of every method.  Stack map and annotation attributes
are deliberately ignored -- they carry no executable meaning and Dalvik does
not need them.

The parser is strict: anything it does not understand raises, because a
silently mis-parsed class file would turn into a silently broken emulator.
"""


class ClassFormatError(Exception):
    pass


# JVM internal names we convert to smali descriptors.
def class_desc(name):
    """``java/lang/String`` -> ``Ljava/lang/String;``."""
    return "L" + name + ";"


def split_desc(desc):
    """Splits a method descriptor into a list of argument descriptors."""
    if not desc.startswith("("):
        raise ClassFormatError("bad method descriptor: " + desc)
    args = []
    i = 1
    while desc[i] != ")":
        j = i
        while desc[j] == "[":
            j += 1
        if desc[j] == "L":
            j = desc.index(";", j) + 1
        else:
            j += 1
        args.append(desc[i:j])
        i = j
    return args, desc[i + 1:]


def param_slots(desc, is_static):
    """Number of JVM / DEX registers the parameters of a method occupy."""
    args, _ = split_desc(desc)
    slots = 0 if is_static else 1
    for a in args:
        slots += 2 if a in ("J", "D") else 1
    return slots


class Attr(object):
    def __init__(self, name, data):
        self.name = name
        self.data = data


class Field(object):
    def __init__(self, access, name, desc, attrs):
        self.access = access
        self.name = name
        self.desc = desc
        self.attrs = attrs
        self.constant = None


class Method(object):
    def __init__(self, access, name, desc, attrs):
        self.access = access
        self.name = name
        self.desc = desc
        self.attrs = attrs
        self.code = None          # bytes of the Code attribute (raw)
        self.max_stack = 0
        self.max_locals = 0
        self.insns = []           # decoded instructions
        self.exception_table = []  # (start_pc, end_pc, handler_pc, catch_type)
        self.is_abstract = bool(access & 0x0400)
        self.is_native = bool(access & 0x0100)
        self.is_static = bool(access & 0x0008)
        self.is_private = bool(access & 0x0002)

    def has_code(self):
        return self.code is not None and not self.is_abstract and not self.is_native


class ClassFile(object):
    def __init__(self, data, source="_"):
        self.source = source
        self.data = data
        b = data
        if b[0:4] != b"\xca\xfe\xba\xbe":
            raise ClassFormatError("not a class file: " + source)
        self.magic = b[0:4]
        self.minor, self.major = _u2(b, 4), _u2(b, 6)
        pos = 8
        # ---- constant pool ------------------------------------------------
        count = _u2(b, pos)
        pos += 2
        cp = [None] * count
        i = 1
        while i < count:
            tag = b[pos]
            pos += 1
            if tag == 1:                                  # Utf8
                n = _u2(b, pos)
                pos += 2
                cp[i] = (1, _decode_mutf8(b[pos:pos + n]))
                pos += n
            elif tag in (7, 8, 16, 19, 20):               # refs to utf8
                cp[i] = (tag, _u2(b, pos))
                pos += 2
            elif tag in (3, 4):                           # Int, Float
                cp[i] = (tag, _u4(b, pos))
                pos += 4
            elif tag in (5, 6):                           # Long, Double
                cp[i] = (tag, (_u4(b, pos) << 32) | _u4(b, pos + 4))
                pos += 8
                cp[i + 1] = None
                i += 1
            elif tag in (9, 10, 11, 12, 17, 18):          # refs, NameAndType
                cp[i] = (tag, _u2(b, pos), _u2(b, pos + 2))
                pos += 4
            elif tag == 15:                               # MethodHandle
                cp[i] = (tag, b[pos], _u2(b, pos + 1))
                pos += 3
            else:
                raise ClassFormatError("constant pool tag %d at %d in %s"
                                       % (tag, pos - 1, source))
            i += 1
        self.cp = cp
        # ---- header -------------------------------------------------------
        self.access = _u2(b, pos); pos += 2
        self.this_idx = _u2(b, pos); pos += 2
        self.super_idx = _u2(b, pos); pos += 2
        n_ifaces = _u2(b, pos); pos += 2
        self.interfaces = []
        for _ in range(n_ifaces):
            self.interfaces.append(self.class_name(_u2(b, pos)))
            pos += 2
        # ---- fields -------------------------------------------------------
        self.fields = []
        n = _u2(b, pos); pos += 2
        for _ in range(n):
            access = _u2(b, pos); pos += 2
            name = self.utf8(_u2(b, pos)); pos += 2
            desc = self.utf8(_u2(b, pos)); pos += 2
            attrs, pos = self._attrs(b, pos)
            f = Field(access, name, desc, attrs)
            for a in attrs:
                if a.name == "ConstantValue":
                    f.constant = (self.cp[_u2(a.data, 0)][0],
                                  self.cp[_u2(a.data, 0)][1])
            self.fields.append(f)
        # ---- methods ------------------------------------------------------
        self.methods = []
        n = _u2(b, pos); pos += 2
        for _ in range(n):
            access = _u2(b, pos); pos += 2
            name = self.utf8(_u2(b, pos)); pos += 2
            desc = self.utf8(_u2(b, pos)); pos += 2
            attrs, pos = self._attrs(b, pos)
            m = Method(access, name, desc, attrs)
            for a in attrs:
                if a.name == "Code":
                    m.code = a.data
                    self._parse_code(m, a.data)
            self.methods.append(m)
        self.attrs, pos = self._attrs(b, pos)
        self.name = self.class_name(self.this_idx)
        self.super_name = (self.class_name(self.super_idx)
                           if self.super_idx else None)

    # -- helpers -----------------------------------------------------------
    def _attrs(self, b, pos):
        n = _u2(b, pos); pos += 2
        out = []
        for _ in range(n):
            name = self.utf8(_u2(b, pos)); pos += 2
            length = _u4(b, pos); pos += 4
            out.append(Attr(name, b[pos:pos + length]))
            pos += length
        return out, pos

    def _parse_code(self, m, data):
        m.max_stack = _u2(data, 0)
        m.max_locals = _u2(data, 2)
        n = _u4(data, 4)
        code = data[8:8 + n]
        pos = 8 + n
        m.insns = decode(code)
        nex = _u2(data, pos); pos += 2
        for _ in range(nex):
            start = _u2(data, pos)
            end = _u2(data, pos + 2)
            handler = _u2(data, pos + 4)
            ctype = _u2(data, pos + 6)
            pos += 8
            m.exception_table.append((start, end, handler, ctype))

    def utf8(self, i):
        t = self.cp[i]
        if t is None or t[0] != 1:
            raise ClassFormatError("constant %d is not Utf8" % i)
        return t[1]

    def class_name(self, i):
        """Index of a CONSTANT_Class -> internal name."""
        if i == 0:
            return None
        t = self.cp[i]
        if t[0] != 7:
            raise ClassFormatError("constant %d is not a Class" % i)
        return self.utf8(t[1])

    def name_and_type(self, i):
        t = self.cp[i]
        return self.utf8(t[1]), self.utf8(t[2])

    def member(self, i):
        """Index of a Fieldref/Methodref -> (owner, name, desc, kind)."""
        t = self.cp[i]
        if t[0] not in (9, 10, 11):
            raise ClassFormatError("constant %d is not a member ref" % i)
        owner = self.class_name(t[1])
        name, desc = self.name_and_type(t[2])
        return owner, name, desc, t[0]

    def is_interface_ref(self, i):
        return self.cp[i][0] == 11


def _u2(b, o):
    return (b[o] << 8) | b[o + 1]


def _u4(b, o):
    return (b[o] << 24) | (b[o + 1] << 16) | (b[o + 2] << 8) | b[o + 3]


def _s1(v):
    return v - 256 if v > 127 else v


def _s2(v):
    return v - 65536 if v > 32767 else v


def _s4(v):
    return v - 0x100000000 if v > 0x7FFFFFFF else v


def _decode_mutf8(raw):
    """Modified UTF-8 as used by the JVM constant pool.

    ``\\xC0\\x80`` is NUL and supplementary characters are surrogate pairs,
    exactly what smali's ``\\uXXXX`` escapes reproduce.
    """
    out = []
    i = 0
    n = len(raw)
    while i < n:
        c = raw[i]
        if c < 0x80:
            out.append(chr(c))
            i += 1
        elif (c & 0xE0) == 0xC0:
            out.append(chr(((c & 0x1F) << 6) | (raw[i + 1] & 0x3F)))
            i += 2
        elif (c & 0xF0) == 0xE0:
            out.append(chr(((c & 0x0F) << 12) |
                           ((raw[i + 1] & 0x3F) << 6) |
                           (raw[i + 2] & 0x3F)))
            i += 3
        else:
            raise ClassFormatError("bad modified UTF-8 byte")
    return "".join(out)


# ---------------------------------------------------------------------------
# JVM instruction decoding
# ---------------------------------------------------------------------------

# name, length, operand spec.  Spec characters:
#   b  unsigned byte, B signed byte, h unsigned short, H signed short,
#   i  unsigned int, s  signed int, c  constant pool index (u2),
#   t  branch target (s2, relative to the opcode), T branch target (s4)
_TABLE = {}


def _op(code, name, length, spec=""):
    _TABLE[code] = (name, length, spec)


_op(0x00, "nop", 1)
_op(0x01, "aconst_null", 1)
for _i in range(8):
    _op(0x02 + _i, "iconst_%d" % (_i - 1) if _i else "iconst_m1", 1)
_op(0x09, "lconst_0", 1)
_op(0x0a, "lconst_1", 1)
_op(0x0b, "fconst_0", 1)
_op(0x0c, "fconst_1", 1)
_op(0x0d, "fconst_2", 1)
_op(0x0e, "dconst_0", 1)
_op(0x0f, "dconst_1", 1)
_op(0x10, "bipush", 2, "B")
_op(0x11, "sipush", 3, "H")
_op(0x12, "ldc", 2, "b")
_op(0x13, "ldc_w", 3, "h")
_op(0x14, "ldc2_w", 3, "h")
for _c, _n in ((0x15, "iload"), (0x16, "lload"), (0x17, "fload"),
               (0x18, "dload"), (0x19, "aload")):
    _op(_c, _n, 2, "b")
for _base, _n in ((0x1a, "iload"), (0x1e, "lload"), (0x22, "fload"),
                  (0x26, "dload"), (0x2a, "aload")):
    for _k in range(4):
        _op(_base + _k, "%s_%d" % (_n, _k), 1)
for _c, _n in ((0x2e, "iaload"), (0x2f, "laload"), (0x30, "faload"),
               (0x31, "daload"), (0x32, "aaload"), (0x33, "baload"),
               (0x34, "caload"), (0x35, "saload")):
    _op(_c, _n, 1)
for _c, _n in ((0x36, "istore"), (0x37, "lstore"), (0x38, "fstore"),
               (0x39, "dstore"), (0x3a, "astore")):
    _op(_c, _n, 2, "b")
for _base, _n in ((0x3b, "istore"), (0x3f, "lstore"), (0x43, "fstore"),
                  (0x47, "dstore"), (0x4b, "astore")):
    for _k in range(4):
        _op(_base + _k, "%s_%d" % (_n, _k), 1)
for _c, _n in ((0x4f, "iastore"), (0x50, "lastore"), (0x51, "fastore"),
               (0x52, "dastore"), (0x53, "aastore"), (0x54, "bastore"),
               (0x55, "castore"), (0x56, "sastore"),
               (0x57, "pop"), (0x58, "pop2"), (0x59, "dup"),
               (0x5a, "dup_x1"), (0x5b, "dup_x2"), (0x5c, "dup2"),
               (0x5d, "dup2_x1"), (0x5e, "dup2_x2"), (0x5f, "swap")):
    _op(_c, _n, 1)
for _c, _n in ((0x60, "iadd"), (0x61, "ladd"), (0x62, "fadd"),
               (0x63, "dadd"), (0x64, "isub"), (0x65, "lsub"),
               (0x66, "fsub"), (0x67, "dsub"), (0x68, "imul"),
               (0x69, "lmul"), (0x6a, "fmul"), (0x6b, "dmul"),
               (0x6c, "idiv"), (0x6d, "ldiv"), (0x6e, "fdiv"),
               (0x6f, "ddiv"), (0x70, "irem"), (0x71, "lrem"),
               (0x72, "frem"), (0x73, "drem"), (0x74, "ineg"),
               (0x75, "lneg"), (0x76, "fneg"), (0x77, "dneg"),
               (0x78, "ishl"), (0x79, "lshl"), (0x7a, "ishr"),
               (0x7b, "lshr"), (0x7c, "iushr"), (0x7d, "lushr"),
               (0x7e, "iand"), (0x7f, "land"), (0x80, "ior"),
               (0x81, "lor"), (0x82, "ixor"), (0x83, "lxor")):
    _op(_c, _n, 1)
_op(0x84, "iinc", 3, "bB")
for _c, _n in ((0x85, "i2l"), (0x86, "i2f"), (0x87, "i2d"), (0x88, "l2i"),
               (0x89, "l2f"), (0x8a, "l2d"), (0x8b, "f2i"), (0x8c, "f2l"),
               (0x8d, "f2d"), (0x8e, "d2i"), (0x8f, "d2l"), (0x90, "d2f"),
               (0x91, "i2b"), (0x92, "i2c"), (0x93, "i2s"),
               (0x94, "lcmp"), (0x95, "fcmpl"), (0x96, "fcmpg"),
               (0x97, "dcmpl"), (0x98, "dcmpg")):
    _op(_c, _n, 1)
for _c, _n in ((0x99, "ifeq"), (0x9a, "ifne"), (0x9b, "iflt"),
               (0x9c, "ifge"), (0x9d, "ifgt"), (0x9e, "ifle"),
               (0x9f, "if_icmpeq"), (0xa0, "if_icmpne"),
               (0xa1, "if_icmplt"), (0xa2, "if_icmpge"),
               (0xa3, "if_icmpgt"), (0xa4, "if_icmple"),
               (0xa5, "if_acmpeq"), (0xa6, "if_acmpne")):
    _op(_c, _n, 3, "t")
_op(0xa7, "goto", 3, "t")
_op(0xa8, "jsr", 3, "t")
_op(0xa9, "ret", 2, "b")
_op(0xaa, "tableswitch", -1)
_op(0xab, "lookupswitch", -1)
for _c, _n in ((0xac, "ireturn"), (0xad, "lreturn"), (0xae, "freturn"),
               (0xaf, "dreturn"), (0xb0, "areturn"), (0xb1, "return")):
    _op(_c, _n, 1)
for _c, _n in ((0xb2, "getstatic"), (0xb3, "putstatic"), (0xb4, "getfield"),
               (0xb5, "putfield"), (0xb6, "invokevirtual"),
               (0xb7, "invokespecial"), (0xb8, "invokestatic"),
               (0xbb, "new"), (0xbd, "anewarray"), (0xc0, "checkcast"),
               (0xc1, "instanceof")):
    _op(_c, _n, 3, "c")
_op(0xb9, "invokeinterface", 5, "cb")
_op(0xba, "invokedynamic", 5, "ch")
_op(0xbc, "newarray", 2, "b")
_op(0xbe, "arraylength", 1)
_op(0xbf, "athrow", 1)
_op(0xc2, "monitorenter", 1)
_op(0xc3, "monitorexit", 1)
_op(0xc4, "wide", -1)
_op(0xc5, "multianewarray", 4, "cb")
_op(0xc6, "ifnull", 3, "t")
_op(0xc7, "ifnonnull", 3, "t")
_op(0xc8, "goto_w", 5, "T")
_op(0xc9, "jsr_w", 5, "T")


class Insn(object):
    """One decoded JVM instruction."""

    __slots__ = ("pc", "op", "name", "size", "args")

    def __init__(self, pc, op, name, size, args):
        self.pc = pc
        self.op = op
        self.name = name
        self.size = size
        self.args = args

    def __repr__(self):
        return "%04x %s %r" % (self.pc, self.name, self.args)


def decode(code):
    """Decodes a Code attribute into a list of Insn."""
    out = []
    pc = 0
    n = len(code)
    while pc < n:
        op = code[pc]
        ent = _TABLE.get(op)
        if ent is None:
            raise ClassFormatError("unknown opcode 0x%02x at %d" % (op, pc))
        name, size, spec = ent
        if op in (0xaa, 0xab):                     # switches: 4 byte aligned
            base = pc + 1
            pad = (4 - (base % 4)) % 4
            p = base + pad
            default = _s4(_u4(code, p))
            if op == 0xaa:
                low = _s4(_u4(code, p + 4))
                high = _s4(_u4(code, p + 8))
                cnt = high - low + 1
                if cnt < 0:
                    raise ClassFormatError("bad tableswitch at %d" % pc)
                p += 12
                offs = []
                for _ in range(cnt):
                    offs.append(_s4(_u4(code, p)))
                    p += 4
                args = (default, low, high, offs)
            else:
                npairs = _s4(_u4(code, p + 4))
                p += 8
                pairs = []
                for _ in range(npairs):
                    pairs.append((_s4(_u4(code, p)), _s4(_u4(code, p + 4))))
                    p += 8
                args = (default, pairs)
            size = p - pc
            out.append(Insn(pc, op, name, size, args))
            pc = p
            continue
        if op == 0xc4:                             # wide
            nxt = code[pc + 1]
            if nxt == 0x84:
                size = 6
                args = ((_u2(code, pc + 2), _s2(_u2(code, pc + 4))),)
            else:
                size = 4
                args = (_u2(code, pc + 2),)
            out.append(Insn(pc, op, "wide_" + _TABLE[nxt][0], size, args))
            pc += size
            continue
        args = []
        for k, ch in enumerate(spec):
            o = pc + 1 + k
            if ch in "bB":
                args.append(_s1(code[o]) if ch == "B" else code[o])
            elif ch in "hH":
                args.append(_s2(_u2(code, o)) if ch == "H" else _u2(code, o))
            elif ch == "c":
                args.append(_u2(code, o))
            elif ch == "t":
                args.append(pc + _s2(_u2(code, o)))
            elif ch == "T":
                args.append(pc + _s4(_u4(code, o)))
        out.append(Insn(pc, op, name, size, tuple(args)))
        pc += size
    return out


def read_class(path):
    with open(path, "rb") as f:
        return ClassFile(f.read(), path)
