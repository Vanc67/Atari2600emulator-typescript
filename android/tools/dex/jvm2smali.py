"""JVM class files -> smali source.

This is the droid toolchain's own dexer.  There is no Android SDK on the build
host, so ECJ output (class files) is translated to smali text and handed to the
smali assembler (dexlib2, shipped inside apktool.jar).

Stack slots map straight onto registers:

    JVM local slot i          -> register i
    JVM operand-stack slot d  -> register max_locals + d
    scratch registers         -> max_locals + max_stack + n

so values that are adjacent on the JVM stack stay adjacent in the Dalvik
register file, which is what invoke-* wants.  A long occupies two slots, i.e.
two adjacent registers, matching Dalvik's pair convention.
"""

import os
import struct
import sys

import classfile


class Unsupported(Exception):
    pass


# ---------------------------------------------------------------- descriptors
def ret_desc(desc):
    return desc[desc.index(")") + 1:]


def arg_desc(desc):
    return desc[1:desc.index(")")]


def parse_types(s):
    out = []
    i = 0
    while i < len(s):
        c = s[i]
        if c == "[":
            j = i
            while s[j] == "[":
                j += 1
            if s[j] == "L":
                j = s.index(";", j)
            out.append(s[i:j + 1])
            i = j + 1
        elif c == "L":
            j = s.index(";", i)
            out.append(s[i:j + 1])
            i = j + 1
        else:
            out.append(c)
            i += 1
    return out


def slot_size(t):
    return 2 if t in ("J", "D") else 1


def arg_slots(desc):
    return sum(slot_size(t) for t in parse_types(arg_desc(desc)))


def ret_slots(desc):
    r = ret_desc(desc)
    return 0 if r == "V" else slot_size(r)


def smali_type(name):
    """JVM internal class name -> smali type descriptor."""
    return name if name.startswith("[") else "L%s;" % name


def is_ref(desc):
    return desc.startswith("L") or desc.startswith("[")


def wide_suffix(desc):
    """smali opcode suffix for a field/array element of type desc."""
    if desc in ("J", "D"):
        return "-wide"
    if is_ref(desc):
        return "-object"
    return ""


# ---------------------------------------------------------------- opcode table
# operand format for the decoder
FMT = {}
# stack effect (pops, pushes) in slots; 0/0 means "resolved by the translator"
EFFECT = {}


def _op(code, fmt, pops, pushes):
    FMT[code] = fmt
    EFFECT[code] = (pops, pushes)


_op(0x00, "none", 0, 0)                     # nop
_op(0x01, "none", 0, 1)                     # aconst_null
for _k in range(8):                         # iconst_m1 .. iconst_5
    _op(0x02 + _k, "none", 0, 1)
for _k in range(2):                         # lconst_0 .. lconst_1
    _op(0x09 + _k, "none", 0, 2)
for _k in range(3):                         # fconst_*
    _op(0x0B + _k, "none", 0, 1)
for _k in range(2):                         # dconst_*
    _op(0x0E + _k, "none", 0, 2)
_op(0x10, "s1", 0, 1)                       # bipush
_op(0x11, "s2", 0, 1)                       # sipush
_op(0x12, "u1", 0, 0)                       # ldc
_op(0x13, "u2", 0, 0)                       # ldc_w
_op(0x14, "u2", 0, 0)                       # ldc2_w

LOAD_SIZE = {0x15: 1, 0x16: 2, 0x17: 1, 0x18: 2, 0x19: 1}
for _code, _size in LOAD_SIZE.items():
    _op(_code, "u1", 0, _size)
STORE_SIZE = {0x36: 1, 0x37: 2, 0x38: 1, 0x39: 2, 0x3A: 1}
for _code, _size in STORE_SIZE.items():
    _op(_code, "u1", _size, 0)
for _k in range(4):                         # load_n / store_n shorthands
    _op(0x1A + _k, "none", 0, 1)            # iload_0..3
    _op(0x1E + _k, "none", 0, 2)            # lload_0..3
    _op(0x22 + _k, "none", 0, 1)            # fload_0..3
    _op(0x26 + _k, "none", 0, 2)            # dload_0..3
    _op(0x2A + _k, "none", 0, 1)            # aload_0..3
    _op(0x3B + _k, "none", 1, 0)            # istore_0..3
    _op(0x3F + _k, "none", 2, 0)            # lstore_0..3
    _op(0x43 + _k, "none", 1, 0)            # fstore_0..3
    _op(0x47 + _k, "none", 2, 0)            # dstore_0..3
    _op(0x4B + _k, "none", 1, 0)            # astore_0..3

ARRAY_LOAD = {0x2E: ("I", "aget"), 0x2F: ("J", "aget-wide"),
              0x30: ("F", "aget"), 0x31: ("D", "aget-wide"),
              0x32: ("L", "aget-object"), 0x33: ("I", "aget-byte"),
              0x34: ("I", "aget-char"), 0x35: ("I", "aget-short")}
DASTORE = {0x4F: "aput", 0x50: "aput-wide", 0x51: "aput", 0x52: "aput-wide",
           0x53: "aput-object", 0x54: "aput-byte", 0x55: "aput-char",
           0x56: "aput-short"}
for _code, (_t, _n) in ARRAY_LOAD.items():
    _op(_code, "none", 2, slot_size(_t))
for _code in DASTORE:
    _tcode = {0x4F: "I", 0x50: "J", 0x51: "F", 0x52: "D", 0x53: "L",
              0x54: "I", 0x55: "I", 0x56: "I"}[_code]
    _op(_code, "none", 2 + slot_size(_tcode), 0)

for _code in range(0x57, 0x60):             # stack juggling
    _op(_code, "none", 0, 0)

BINOP = {}
for _base, _t, _size in ((0x60, "I", 1), (0x61, "J", 2),
                         (0x62, "F", 1), (0x63, "D", 2)):
    for _k, _name in enumerate(("add", "sub", "mul", "div", "rem")):
        BINOP[_base + 4 * _k] = (_name, _t, _size)
for _code, (_n, _t, _size) in BINOP.items():
    _op(_code, "none", 2 * _size, _size)
for _base, _size in ((0x74, 1), (0x75, 2)):  # ineg/lneg, fneg/dneg
    _op(_base, "none", _size, _size)
    _op(_base + 2, "none", _size, _size)
for _base, _size in ((0x78, 1), (0x79, 2)):  # shifts
    for _k in range(3):
        _op(_base + 2 * _k, "none", _size + 1, _size)
for _base, _size in ((0x7E, 1), (0x7F, 2)):  # and/or/xor
    for _k in range(3):
        _op(_base + 2 * _k, "none", 2 * _size, _size)
_op(0x84, "u1s1", 0, 0)                     # iinc

CONV = {0x85: "int-to-long", 0x86: "int-to-float", 0x87: "int-to-double",
        0x88: "long-to-int", 0x89: "long-to-float", 0x8A: "long-to-double",
        0x8B: "float-to-int", 0x8C: "float-to-long", 0x8D: "float-to-double",
        0x8E: "double-to-int", 0x8F: "double-to-long", 0x90: "double-to-float",
        0x91: "int-to-byte", 0x92: "int-to-char", 0x93: "int-to-short"}
CONV_SIZE = {0x85: (1, 2), 0x86: (1, 1), 0x87: (1, 2), 0x88: (2, 1),
             0x89: (2, 1), 0x8A: (2, 2), 0x8B: (1, 1), 0x8C: (1, 2),
             0x8D: (1, 2), 0x8E: (2, 1), 0x8F: (2, 2), 0x90: (2, 1),
             0x91: (1, 1), 0x92: (1, 1), 0x93: (1, 1)}
for _code, (_p, _u) in CONV_SIZE.items():
    _op(_code, "none", _p, _u)

for _code in (0x94, 0x95, 0x96, 0x97, 0x98):      # cmp ops
    _p = {"0x94": 4, "0x95": 2, "0x96": 2, "0x97": 4, "0x98": 4}["0x%02x" % _code]
    _op(_code, "none", _p, 1)

for _code in range(0x99, 0x9F):             # if<cond>
    _op(_code, "s2", 1, 0)
for _code in range(0x9F, 0xA5):             # if_icmp<cond>
    _op(_code, "s2", 2, 0)
for _code in range(0xA5, 0xA7):             # if_acmp<cond>
    _op(_code, "s2", 2, 0)
_op(0xA7, "s2", 0, 0)                       # goto
_op(0xA8, "s2", 0, 0)                       # jsr
_op(0xA9, "u1", 0, 0)                       # ret
_op(0xAA, "table", 0, 0)
_op(0xAB, "lookup", 0, 0)
_op(0xAC, "none", 1, 0)                     # ireturn
_op(0xAD, "none", 2, 0)                     # lreturn
_op(0xAE, "none", 1, 0)                     # freturn
_op(0xAF, "none", 2, 0)                     # dreturn
_op(0xB0, "none", 1, 0)                     # areturn
_op(0xB1, "none", 0, 0)                     # return

for _code in (0xB2, 0xB3, 0xB4, 0xB5):      # fields
    _op(_code, "u2", 0, 0)
for _code in (0xB6, 0xB7, 0xB8, 0xB9, 0xBA):    # invokes
    _op(_code, "u2", 0, 0)

_op(0xBB, "u2", 0, 1)                       # new
_op(0xBC, "u1", 1, 1)                       # newarray
_op(0xBD, "u2", 1, 1)                       # anewarray
_op(0xBE, "none", 1, 1)                     # arraylength
_op(0xBF, "none", 1, 0)                     # athrow
_op(0xC0, "u2", 1, 1)                       # checkcast
_op(0xC1, "u2", 1, 1)                       # instanceof
_op(0xC2, "none", 1, 0)                     # monitorenter
_op(0xC3, "none", 1, 0)                     # monitorexit
_op(0xC4, "wide", 0, 0)                     # wide
_op(0xC5, "u2u1", 0, 1)                     # multianewarray
_op(0xC6, "s2", 1, 0)                       # ifnull
_op(0xC7, "s2", 1, 0)                       # ifnonnull
_op(0xC8, "s4", 0, 0)                       # goto_w
_op(0xC9, "s4", 0, 0)                       # jsr_w
FMT[0xB9] = "u2u1u1"                        # invokeinterface has 5 bytes
FMT[0xBA] = "u2u1u1"                        # invokedynamic has 5 bytes


def decode(code, pc):
    """-> (opcode, operands, size_in_bytes, is_wide)."""
    op = code[pc]
    if op not in FMT:
        raise Unsupported("opcode 0x%02x at %d" % (op, pc))
    f = FMT[op]
    if f == "none":
        return op, (), 1, False
    if f == "u1":
        return op, (code[pc + 1],), 2, False
    if f == "s1":
        v = code[pc + 1]
        return op, (v - 256 if v > 127 else v,), 2, False
    if f == "u2":
        return op, ((code[pc + 1] << 8) | code[pc + 2],), 3, False
    if f == "s2":
        v = (code[pc + 1] << 8) | code[pc + 2]
        return op, (v - 65536 if v > 32767 else v,), 3, False
    if f == "u1s1":
        v = code[pc + 2]
        return op, (code[pc + 1], v - 256 if v > 127 else v), 3, False
    if f == "u2u1":
        return op, ((code[pc + 1] << 8) | code[pc + 2], code[pc + 3]), 4, False
    if f == "u2u1u1":
        return op, ((code[pc + 1] << 8) | code[pc + 2], code[pc + 3],
                    code[pc + 4]), 5, False
    if f == "s4":
        return op, (struct.unpack_from(">i", code, pc + 1)[0],), 5, False
    if f == "wide":
        sub = code[pc + 1]
        idx = (code[pc + 2] << 8) | code[pc + 3]
        if sub == 0x84:
            c = (code[pc + 4] << 8) | code[pc + 5]
            return sub, (idx, c - 65536 if c > 32767 else c), 6, True
        if sub not in FMT:
            raise Unsupported("bad wide sub-opcode 0x%02x" % sub)
        return sub, (idx,), 4, True
    if f == "table":
        p = pc + 1
        while p % 4:
            p += 1
        default = struct.unpack_from(">i", code, p)[0]
        low = struct.unpack_from(">i", code, p + 4)[0]
        high = struct.unpack_from(">i", code, p + 8)[0]
        n = high - low + 1
        offs = list(struct.unpack_from(">%di" % n, code, p + 12))
        return op, (default, low, offs), p + 12 + 4 * n - pc, False
    if f == "lookup":
        p = pc + 1
        while p % 4:
            p += 1
        default = struct.unpack_from(">i", code, p)[0]
        n = struct.unpack_from(">i", code, p + 4)[0]
        pairs = []
        q = p + 8
        for _ in range(n):
            k = struct.unpack_from(">i", code, q)[0]
            t = struct.unpack_from(">i", code, q + 4)[0]
            pairs.append((k, t))
            q += 8
        return op, (default, pairs), q - pc, False
    raise Unsupported("format %s" % f)


# ---------------------------------------------------------------- helpers
def lit32(v):
    v &= 0xFFFFFFFF
    return v - 0x100000000 if v >= 0x80000000 else v


def const_insn(reg, value):
    """Smallest int constant instruction whose register form fits reg."""
    if reg >= 256:
        raise Unsupported("register %d too high for a constant (8-bit field)"
                          % reg)
    if -8 <= value <= 7 and reg < 16:
        return "const/4 v%d, %d" % (reg, value)
    if -32768 <= value <= 32767 and reg < 256:
        return "const/16 v%d, %d" % (reg, value)
    return "const v%d, 0x%08x" % (reg, value & 0xFFFFFFFF)


def const_wide_insn(reg, value):
    if reg >= 256:
        raise Unsupported("register %d too high for a constant (8-bit field)"
                          % reg)
    if -32768 <= value <= 32767 and reg < 256:
        return "const-wide/16 v%d, %dL" % (reg, value)
    return "const-wide v%d, 0x%xL" % (reg, value & 0xFFFFFFFFFFFFFFFF)


def escape_string(s):
    out = []
    for ch in s:
        o = ord(ch)
        if ch == '"':
            out.append('\\"')
        elif ch == "\\":
            out.append("\\\\")
        elif ch == "\n":
            out.append("\\n")
        elif ch == "\r":
            out.append("\\r")
        elif ch == "\t":
            out.append("\\t")
        elif 32 <= o < 127:
            out.append(ch)
        else:
            out.append("\\u%04x" % o)
    return "".join(out)


# stack juggling: output slots (top first) expressed as input slot indices
JUGGLE_SLOTS = {0x57: 1, 0x58: 2, 0x59: 1, 0x5A: 2, 0x5B: 3, 0x5C: 2,
                0x5D: 3, 0x5E: 4, 0x5F: 2}

# output slot lists, bottom -> top, as indices into the popped slots
JUGGLE_OUT = {
    0x57: [],                       # pop
    0x58: [],                       # pop2
    0x59: [0, 0],                   # dup
    0x5A: [1, 0, 1],                # dup_x1
    0x5B: [2, 0, 1, 2],             # dup_x2
    0x5C: [0, 1, 0, 1],             # dup2
    0x5D: [1, 2, 0, 1, 2],          # dup2_x1
    0x5E: [2, 3, 0, 1, 2, 3],       # dup2_x2
    0x5F: [1, 0],                   # swap
}


def t_v(n):
    return "v%d" % n


FIELD_FLAGS = ((0x0001, "public"), (0x0002, "private"), (0x0004, "protected"),
               (0x0008, "static"), (0x0010, "final"), (0x0040, "volatile"),
               (0x0080, "transient"), (0x1000, "synthetic"), (0x4000, "enum"))

METHOD_FLAGS = ((0x0001, "public"), (0x0002, "private"), (0x0004, "protected"),
                (0x0008, "static"), (0x0010, "final"), (0x0020, "synchronized"),
                (0x0040, "bridge"), (0x0080, "varargs"), (0x0100, "native"),
                (0x0400, "abstract"), (0x1000, "synthetic"), (0x0800, "strictfp"))


def flags_of(access, table):
    return " ".join(n for bit, n in table if access & bit)


def const_value_text(cf, c):
    tag, val = c[0], c[1]
    if tag == classfile.TAG_INT:
        return "0x%x" % (val & 0xFFFFFFFF)
    if tag == classfile.TAG_LONG:
        return "0x%xL" % (val & 0xFFFFFFFFFFFFFFFF)
    if tag == classfile.TAG_STRING:
        return '"%s"' % escape_string(cf.utf8(val))
    if tag == classfile.TAG_FLOAT:
        raise Unsupported("float constant value")
    if tag == classfile.TAG_DOUBLE:
        raise Unsupported("double constant value")
    raise Unsupported("constant value tag %d" % tag)




SCRATCH = 4          # v0..v3 are scratch registers, never JVM state


def slot_off(pattern, index):
    """slot offset of value #index (values counted from the bottom)."""
    return sum(pattern[:index])


def slot_widths(pattern):
    out = []
    for w in pattern:
        if w == 2:
            out.append(2)
            out.append(0)
        else:
            out.append(1)
    return out


class MethodTranslator(object):
    """Translates one JVM method.

    Register layout:

        v0 .. v3                        scratch (stack juggling, wide temps)
        v4 .. v4+max_locals-1           JVM local variables
        next max_stack registers        JVM operand-stack slots
        trailing registers              incoming parameters (p0 ..)

    Parameters are copied down into the local slots in the prologue.  Because a
    parameter register always has a higher number than the local slot it is
    copied to, copying in ascending slot order never clobbers an unread
    parameter.
    """

    def __init__(self, cf, m, verbose=False):
        self.cf = cf
        self.m = m
        self.verbose = verbose
        self.max_locals = m.max_locals
        self.max_stack = m.max_stack
        self.ins = arg_slots(m.desc) + (0 if (m.access & 0x0008) else 1)
        # parameter registers sit on top of the stack area, and double as the
        # JVM local slots 0..ins-1, so nothing has to be copied in the prologue
        self.total = SCRATCH + self.max_locals + self.max_stack + self.ins
        self.insns = []
        self.by_pc = {}
        self.pattern_at = {}
        self.handlers = []
        self.code_len = 0
        self._selfinit_pcs = set()

    # -- registers -----------------------------------------------------
    def loc(self, idx):
        if idx < 0 or idx >= self.max_locals:
            raise Unsupported("local %d out of range in %s (max %d)"
                              % (idx, self.m.name, self.max_locals))
        if idx < self.ins:
            return self.total - self.ins + idx
        return SCRATCH + idx

    def st(self, slot):
        if slot < 0 or slot >= self.max_stack + 4:
            raise Unsupported("stack slot %d out of range in %s (max %d)"
                              % (slot, self.m.name, self.max_stack))
        return SCRATCH + self.max_locals + slot

    @staticmethod
    def v(n):
        return "v%d" % n

    def mv(self, dest, src, wide=False):
        kind = "move-wide" if wide else "move"
        if dest < 16 and src < 16:
            return "%s %s, %s" % (kind, self.v(dest), self.v(src))
        if dest < 256 and src < 65536:
            return "%s/from16 %s, %s" % (kind, self.v(dest), self.v(src))
        return "%s/16 %s, %s" % (kind, self.v(dest), self.v(src))

    def mvobj(self, dest, src):
        if dest < 16 and src < 16:
            return "move-object %s, %s" % (self.v(dest), self.v(src))
        if dest < 256 and src < 65536:
            return "move-object/from16 %s, %s" % (self.v(dest), self.v(src))
        return "move-object/16 %s, %s" % (self.v(dest), self.v(src))

    # -- decoding ------------------------------------------------------
    def decode_all(self):
        code = self.m.code
        raw = []
        pc = 0
        while pc < len(code):
            op, ops, size, wide = decode(code, pc)
            raw.append([pc, op, list(ops), size, wide])
            pc += size
        total_bytes = len(code)
        # branch targets become absolute addresses
        for ins in raw:
            pc, op, ops = ins[0], ins[1], ins[2]
            if 0x99 <= op <= 0xA6 or op in (0xC6, 0xC7, 0xA7, 0xC8):
                ops[0] = pc + ops[0]
            elif op == 0xAA:
                default, low, offs = ops
                ins[2] = [pc + default, low, [pc + o for o in offs]]
            elif op == 0xAB:
                default, pairs = ops
                ins[2] = [pc + default, [(k, pc + t) for (k, t) in pairs]]
        # `new X; dup; ...; invokespecial <init>` - the dup copy would be an
        # uninitialised reference for the Dalvik verifier, so drop it.  The
        # object then simply stays in the register that <init> initialises.
        targets = set()
        for ins in raw:
            op, ops = ins[1], ins[2]
            if 0x99 <= op <= 0xA7 or op in (0xC6, 0xC7, 0xC8):
                targets.add(ops[0])
            elif op == 0xAA:
                targets.add(ops[0])
                targets.update(ops[2])
            elif op == 0xAB:
                targets.add(ops[0])
                targets.update(t for (_k, t) in ops[1])
        boundary = set(targets)
        for (st, en, h, _t) in self.m.exceptions:
            boundary.add(st)
            boundary.add(en)
            boundary.add(h)
        drop = set()
        for i in range(len(raw) - 1):
            if raw[i][1] == 0xBB and raw[i + 1][1] == 0x59:
                if raw[i + 1][0] not in boundary:
                    drop.add(i + 1)
        # An allocation whose dup we dropped is initialised in place, so the
        # matching invoke-direct <init> must NOT consume its receiver: the
        # object stays in the register.  Inner allocations are always
        # initialised before the outer one, hence a simple LIFO matching.
        selfinit_pc = set()
        dropped_pcs = set(raw[i][0] for i in drop)
        pending = []
        for ins in raw:
            pc, op, ops = ins[0], ins[1], ins[2]
            if op == 0xBB:
                pending.append((pc, (pc + 3) in dropped_pcs))
                continue
            if 0xB6 <= op <= 0xB9 and pending:
                _o, name, _d, _k = self.cf.ref(ops[0])
                if name == "<init>":
                    _alloc_pc, dropped = pending.pop()
                    if dropped:
                        selfinit_pc.add(pc)
        remap = {}
        newpc = 0
        for i, ins in enumerate(raw):
            if i in drop:
                continue
            remap[ins[0]] = newpc
            newpc += ins[3]
        remap[total_bytes] = newpc
        self.next_pc = {}
        self.insns = []
        for i, ins in enumerate(raw):
            if i in drop:
                continue
            pc, op, ops, size, wide = ins
            nops = list(ops)
            if 0x99 <= op <= 0xA6 or op in (0xC6, 0xC7, 0xA7, 0xC8):
                nops[0] = remap[ops[0]]
            elif op == 0xAA:
                nops = [remap[ops[0]], ops[1], [remap[t] for t in ops[2]]]
            elif op == 0xAB:
                nops = [remap[ops[0]], [(k, remap[t]) for (k, t) in ops[1]]]
            entry = (remap[pc], op, nops, size, wide)
            self.insns.append(entry)
            self.by_pc[entry[0]] = entry
        for i, entry in enumerate(self.insns[:-1]):
            self.next_pc[entry[0]] = self.insns[i + 1][0]
        starts = sorted(remap)

        def addr(old):
            """map an arbitrary old address to the new instruction stream"""
            if old in remap:
                return remap[old]
            for pc in starts:
                if pc >= old:
                    return remap[pc]
            return newpc

        self.handlers = [(addr(st), addr(en), addr(h), t)
                         for (st, en, h, t) in self.m.exceptions]
        self.selfinit = set(remap[pc] for pc in selfinit_pc if pc in remap)
        self._selfinit_pcs = self.selfinit
        self.code_len = newpc

    def successors(self, pc, op, ops):
        nxt = self.next_pc.get(pc, self.code_len)
        if op in (0xA7, 0xC8):
            return [ops[0]]
        if 0x99 <= op <= 0xA6 or op in (0xC6, 0xC7):
            return [nxt, ops[0]]
        if op == 0xAA:
            return [ops[0]] + list(ops[2])
        if op == 0xAB:
            return [ops[0]] + [t for (_k, t) in ops[1]]
        if op in (0xAC, 0xAD, 0xAE, 0xAF, 0xB0, 0xB1, 0xBF, 0xA9):
            return []
        return [nxt]

    def widths(self, pc, op, ops, pattern):
        """(popped value widths bottom->top, pushed value widths bottom->top)."""
        def one(width, count=1):
            return tuple([width] * count)

        if op in (0x00,):
            return (), ()
        if op in (0x01,) or 0x02 <= op <= 0x08:
            return (), one(1)
        if 0x09 <= op <= 0x0A:
            return (), one(2)
        if 0x0B <= op <= 0x0D:
            return (), one(1)
        if 0x0E <= op <= 0x0F:
            return (), one(2)
        if op in (0x10, 0x11, 0x12, 0x13):
            return (), one(1)
        if op == 0x14:
            return (), one(2)
        if op in LOAD_SIZE:
            return (), one(LOAD_SIZE[op])
        if op in STORE_SIZE:
            return one(STORE_SIZE[op]), ()
        if 0x1A <= op <= 0x2D:
            w = 2 if (0x1E <= op <= 0x21 or 0x26 <= op <= 0x29) else 1
            return (), one(w)
        if 0x3B <= op <= 0x4E:
            w = 2 if (0x3F <= op <= 0x42 or 0x47 <= op <= 0x4A) else 1
            return one(w), ()
        if op in ARRAY_LOAD:
            ty = ARRAY_LOAD[op][0]
            return (1, 1), one(2 if ty in ("J", "D") else 1)
        if op in DASTORE:
            w = {0x4F: 1, 0x50: 2, 0x51: 1, 0x52: 2, 0x53: 1, 0x54: 1,
                 0x55: 1, 0x56: 1}[op]
            return (1, 1, w), ()
        if 0x57 <= op <= 0x5F:
            return self.juggle_widths(op, pattern)
        if op in BINOP:
            size = BINOP[op][2]
            return one(size, 2), one(size)
        if op in (0x74, 0x75):
            size = 1 if op == 0x74 else 2
            return one(size), one(size)
        if op == 0x76:
            return one(1), one(1)
        if op == 0x77:
            return one(2), one(2)
        if op in (0x78, 0x7A, 0x7C):
            return (1, 1), one(1)
        if op in (0x79, 0x7B, 0x7D):
            return (2, 1), one(2)
        if op in (0x7E, 0x80, 0x82):
            return one(1, 2), one(1)
        if op in (0x7F, 0x81, 0x83):
            return one(2, 2), one(2)
        if op == 0x84:
            return (), ()
        if op in CONV:
            return one(CONV_SIZE[op][0]), one(CONV_SIZE[op][1])
        if op == 0x94:
            return one(2, 2), one(1)
        if op in (0x95, 0x96):
            return one(1, 2), one(1)
        if op in (0x97, 0x98):
            return one(2, 2), one(1)
        if 0x99 <= op <= 0x9E or op in (0xC6, 0xC7):
            return one(1), ()
        if 0x9F <= op <= 0xA6:
            return one(1, 2), ()
        if op in (0xA7, 0xC8):
            return (), ()
        if op in (0xA8, 0xA9, 0xC9):
            raise Unsupported("jsr/ret at %d" % pc)
        if op in (0xAA, 0xAB):
            return one(1), ()
        if op == 0xAC:
            return one(1), ()
        if op == 0xAD:
            return one(2), ()
        if op == 0xB0:
            return one(1), ()
        if op == 0xB1:
            return (), ()
        if op == 0xAE:
            return one(1), ()
        if op == 0xAF:
            return one(2), ()
        if op in (0xB2, 0xB3, 0xB4, 0xB5):
            _o, _n, desc, _k = self.cf.ref(ops[0])
            w = 2 if desc in ("J", "D") else 1
            if op == 0xB2:
                return (), one(w)
            if op == 0xB3:
                return one(w), ()
            if op == 0xB4:
                return one(1), one(w)
            return (1, w), ()
        if 0xB6 <= op <= 0xB9:
            _o, _n, desc, _k = self.cf.ref(ops[0])
            args = tuple(slot_size(x) for x in parse_types(arg_desc(desc)))
            pops = args if op == 0xB8 else (1,) + args
            if pc in self._selfinit_pcs:
                pops = args              # receiver stays on the stack
            r = ret_desc(desc)
            return pops, () if r == "V" else one(slot_size(r))
        if op == 0xBA:
            raise Unsupported("invokedynamic at %d" % pc)
        if op == 0xBB:
            return (), one(1)
        if op in (0xBC, 0xBD, 0xBE):
            return one(1), one(1)
        if op == 0xBF:
            return one(1), ()
        if op in (0xC0, 0xC1):
            return one(1), one(1)
        if op in (0xC2, 0xC3):
            return one(1), ()
        if op == 0xC5:
            raise Unsupported("multianewarray at %d" % pc)
        raise Unsupported("opcode 0x%02x at %d in %s" % (op, pc, self.m.name))

    def juggle_widths(self, op, pattern):
        """Slot-level juggling expressed as value widths."""
        k = JUGGLE_SLOTS[op]           # slots popped
        if k > len(slot_widths(pattern)):
            raise Unsupported("stack underflow in juggle")
        sw = slot_widths(pattern)[-k:] if k else []
        # rebuild values from the popped slots
        pops = []
        i = 0
        while i < len(sw):
            if sw[i] == 2:
                pops.append(2)
                i += 2
            else:
                pops.append(1)
                i += 1
        out_slots = JUGGLE_OUT[op]
        # rebuild pushed values from the output slot pattern
        pushes = []
        i = 0
        while i < len(out_slots):
            w = sw[out_slots[i]]
            if w == 2:
                if i + 1 >= len(out_slots) or out_slots[i + 1] != out_slots[i] + 1:
                    raise Unsupported("broken wide juggle")
                pushes.append(2)
                i += 2
            else:
                pushes.append(1)
                i += 1
        return tuple(pops), tuple(pushes)

    def compute_depths(self):
        self.decode_all()
        work = [(0, ())] + [(h, (1,)) for (_s, _e, h, _t) in self.handlers]
        steps = 0
        limit = len(self.insns) * 64 + 4096
        while work:
            pc, pat = work.pop()
            if pc == self.code_len:
                continue
            if pc not in self.by_pc:
                raise Unsupported("bad branch target %d in %s" % (pc, self.m.name))
            old = self.pattern_at.get(pc)
            if old is not None:
                if old != pat:
                    raise Unsupported("stack pattern %r vs %r at pc %d in %s"
                                      % (old, pat, pc, self.m.name))
                continue
            steps += 1
            if steps > limit:
                raise Unsupported("dataflow does not converge in %s" % self.m.name)
            self.pattern_at[pc] = pat
            op, ops = self.by_pc[pc][1], self.by_pc[pc][2]
            pops, pushes = self.widths(pc, op, ops, pat)
            if len(pops) > len(pat):
                raise Unsupported("stack underflow at pc %d in %s" % (pc, self.m.name))
            if pat[len(pat) - len(pops):] != pops:
                raise Unsupported("stack type mismatch at pc %d in %s (%r vs %r)"
                                  % (pc, self.m.name, pat[len(pat) - len(pops):],
                                     pops))
            out = pat[:len(pat) - len(pops)] + pushes
            slots = sum(out)
            if slots > self.max_stack + 4:
                raise Unsupported("stack overrun at pc %d in %s (%d slots)"
                                  % (pc, self.m.name, slots))
            for s in self.successors(pc, op, ops):
                work.append((s, out))


class Converter(object):
    def __init__(self, verbose=False):
        self.verbose = verbose
        self.stats = {"classes": 0, "methods": 0}

    # -- class ---------------------------------------------------------
    def convert(self, cf):
        out = []
        a = cf.access
        flags = []
        for bit, name in ((0x0001, "public"), (0x0010, "final"),
                          (0x0200, "interface"), (0x0400, "abstract"),
                          (0x1000, "synthetic"), (0x2000, "annotation"),
                          (0x4000, "enum")):
            if a & bit:
                flags.append(name)
        out.append(".class %s L%s;" % (" ".join(flags), cf.name))
        if cf.super_name:
            out.append(".super L%s;" % cf.super_name)
        src = cf.attrs.get("SourceFile")
        if src is not None:
            out.append('.source "%s"'
                       % cf.utf8(struct.unpack_from(">H", src.data, 0)[0]))
        for i in cf.iface_names:
            out.append(".implements L%s;" % i)
        out.append("")
        for f in cf.fields:
            fl = flags_of(f.access, FIELD_FLAGS)
            line = ".field"
            if fl:
                line += " " + fl
            line += " %s:%s" % (f.name, f.desc)
            cv = f.attrs.get("value")
            if cv is not None:
                line += " = " + const_value_text(cf, cv)
            out.append(line)
        if cf.fields:
            out.append("")
        for m in cf.methods:
            self.stats["methods"] += 1
            out.extend(self.convert_method(cf, m))
            out.append("")
        self.stats["classes"] += 1
        return "\n".join(out) + "\n"

    # -- method --------------------------------------------------------
    def convert_method(self, cf, m):
        name = m.name
        if name == "<init>":
            name = "constructor <init>"
        elif name == "<clinit>":
            name = "constructor <clinit>"
        acc = flags_of(m.access, METHOD_FLAGS)
        if m.name in ("<init>", "<clinit>"):
            acc = (acc + " constructor").strip()
        head = ".method %s %s(%s)%s" % (acc, name, arg_desc(m.desc),
                                        ret_desc(m.desc))
        if m.attrs.get("Code") is None:
            return [head, ".end method"]
        t = MethodTranslator(cf, m, self.verbose)
        t.compute_depths()
        lines = [head, "    .registers %d" % t.total]
        body, payloads = self.translate(t)
        lines.extend(body)
        for (name, low, entries) in payloads:
            lines.append(":%s" % name)
            if low is None:
                lines.append(".sparse-switch")
                for (key, label) in entries:
                    lines.append("    %s -> %s" % (key, label))
                lines.append(".end sparse-switch")
            else:
                lines.append(".packed-switch 0x%x" % low)
                for label in entries:
                    lines.append("    %s" % label)
                lines.append(".end packed-switch")
        lines.append(".end method")
        return lines

    def translate(self, t):
        cf, m = t.cf, t.m
        out = []
        for (start, end, hp, ctype) in t.handlers:
            if ctype:
                out.append("    .catch L%s; {:L%d .. :L%d} :L%d"
                           % (m.exc_types[ctype], start, end, hp))
            else:
                out.append("    .catchall {:L%d .. :L%d} :L%d" % (start, end, hp))
        payloads = []
        for (pc, op, ops, _size, wide) in t.insns:
            pat = t.pattern_at.get(pc)
            if pat is None:
                raise Unsupported("unreachable bytecode at %d in %s"
                                  % (pc, m.name))
            out.append(":L%d" % pc)
            text = self.one(t, pc, op, ops, pat, payloads)
            if text:
                for line in text.split("\n"):
                    out.append("    " + line)
        out.append(":L%d" % t.code_len)
        return [x for x in out if x], payloads

    # -- helpers -------------------------------------------------------
    def wide_field(self, desc):
        return desc in ("J", "D")

    def field_suffix(self, desc):
        if desc == "J" or desc == "D":
            return "-wide"
        if desc.startswith("L") or desc.startswith("["):
            return "-object"
        if desc == "Z":
            return "-boolean"
        if desc == "B":
            return "-byte"
        if desc == "C":
            return "-char"
        if desc == "S":
            return "-short"
        return ""

    def r12x(self, op, dst, src):
        """Emit a format-12x instruction (4-bit registers) with spilling."""
        if dst < 16 and src < 16:
            return "%s %s, %s" % (op, t_v(dst), t_v(src))
        lines = []
        lines.append("move/from16 v0, %s" % t_v(src))
        lines.append("%s v1, v0" % op)
        lines.append("move/16 %s, v1" % t_v(dst))
        return "\n".join(lines)

    def r22c(self, op, dst, obj, ref):
        """Format 22c: both register fields are four bits wide.

        `dst` is the destination for the loads (iget, sget, instance-of,
        new-array) but a *source* for the stores (iput), so it has to be
        copied into the scratch register before the instruction is issued.
        Copying it unconditionally keeps one code path for both cases.
        """
        if dst < 16 and obj < 16:
            return "%s %s, %s, %s" % (op, t_v(dst), t_v(obj), ref)
        lines = []
        if dst >= 16:
            lines.append("move/from16 v0, %s" % t_v(dst))
        lines.append("move/from16 v1, %s" % t_v(obj))
        if dst < 16:
            lines.append("%s %s, v1, %s" % (op, t_v(dst), ref))
        else:
            lines.append("%s v0, v1, %s" % (op, ref))
            lines.append("move/16 %s, v0" % t_v(dst))
        return "\n".join(lines)

    def r22b(self, op, dst, src, lit):
        if dst < 16 and src < 16:
            return "%s %s, %s, %d" % (op, t_v(dst), t_v(src), lit)
        lines = ["move/from16 v0, %s" % t_v(src)]
        lines.append("%s v1, v0, %d" % (op, lit))
        lines.append("move/16 %s, v1" % t_v(dst))
        return "\n".join(lines)

    def r22t(self, op, a, b, label):
        if a < 16 and b < 16:
            return "%s %s, %s, %s" % (op, t_v(a), t_v(b), label)
        lines = ["move/from16 v0, %s" % t_v(a),
                 "move/from16 v1, %s" % t_v(b),
                 "%s v0, v1, %s" % (op, label)]
        return "\n".join(lines)

    # -- one instruction ----------------------------------------------
    def one(self, t, pc, op, ops, pat, payloads, wide=False):
        cf = t.cf
        d = len(pat)

        def off(i):
            return slot_off(pat, i)

        def rs(i):
            """register of value i (counted from the bottom)"""
            return t.st(off(i))

        def top(k=1):
            """register of the k-th value from the top (1 = top)"""
            return rs(d - k)

        def is_wide(i):
            return pat[i] == 2

        # ---- constants
        if op == 0x00:
            return None
        if op == 0x01:
            return const_insn(rs(d), 0)
        if 0x02 <= op <= 0x08:
            return const_insn(rs(d), op - 0x03)
        if 0x09 <= op <= 0x0A:
            return const_wide_insn(rs(d), op - 0x09)
        if op in (0x10, 0x11):
            return const_insn(rs(d), ops[0])
        if op in (0x12, 0x13):
            return self.ldc(cf, cf.cp[ops[0]], rs(d))
        if op == 0x14:
            c = cf.cp[ops[0]]
            if c[0] == classfile.TAG_LONG:
                return const_wide_insn(rs(d), c[1])
            if c[0] == classfile.TAG_DOUBLE:
                bits = struct.unpack("<Q", struct.pack("<d", c[1]))[0]
                return "const-wide %s, 0x%xL" % (t_v(rs(d)), bits)
            raise Unsupported("ldc2_w on constant tag %d" % c[0])
        if 0x0B <= op <= 0x0D:
            bits = {0x0B: 0x00000000, 0x0C: 0x3F800000, 0x0D: 0x40000000}[op]
            return "const %s, 0x%08x" % (t_v(rs(d)), bits)
        if 0x0E <= op <= 0x0F:
            bits = {0x0E: 0, 0x0F: 0x3FF0000000000000}[op]
            return "const-wide %s, 0x%xL" % (t_v(rs(d)), bits)

        # ---- loads / stores
        if op in LOAD_SIZE:
            return t.mv(rs(d), t.loc(ops[0]), wide=(LOAD_SIZE[op] == 2))
        if op in STORE_SIZE:
            size = STORE_SIZE[op]
            return t.mv(t.loc(ops[0]), rs(d - 1), wide=(size == 2))
        if 0x1A <= op <= 0x2D:
            for start, real in ((0x1A, 0x15), (0x1E, 0x16), (0x22, 0x17),
                                (0x26, 0x18), (0x2A, 0x19)):
                if start <= op < start + 4:
                    return t.mv(rs(d), t.loc(op - start),
                                wide=(LOAD_SIZE[real] == 2))
        if 0x3B <= op <= 0x4E:
            for start, real in ((0x3B, 0x36), (0x3F, 0x37), (0x43, 0x38),
                                (0x47, 0x39), (0x4B, 0x3A)):
                if start <= op < start + 4:
                    return t.mv(t.loc(op - start), rs(d - 1),
                                wide=(STORE_SIZE[real] == 2))
        if op in (0x15, 0x16, 0x17, 0x18, 0x19, 0x36, 0x37, 0x38, 0x39, 0x3A):
            pass

        # ---- arrays
        if op in ARRAY_LOAD:
            ty, name = ARRAY_LOAD[op]
            return "%s %s, %s, %s" % (name, t_v(rs(d - 2)), t_v(rs(d - 2)),
                                      t_v(rs(d - 1)))
        if op in DASTORE:
            name = DASTORE[op]
            return "%s %s, %s, %s" % (name, t_v(rs(d - 1)), t_v(rs(d - 3)),
                                      t_v(rs(d - 2)))
        if op == 0xBE:
            return self.r12x("array-length", rs(d - 1), rs(d - 1))

        # ---- stack juggling
        if 0x57 <= op <= 0x5F:
            return self.juggle(t, op, pat)

        # ---- arithmetic
        if op in BINOP:
            name, ty, size = BINOP[op]
            return "%s-%s %s, %s, %s" % (name, {"I": "int", "J": "long",
                                                "F": "float", "D": "double"}[ty],
                                         t_v(rs(d - 2)), t_v(rs(d - 2)),
                                         t_v(rs(d - 1)))
        if op in (0x74, 0x75, 0x76, 0x77):
            kind = {0x74: "int", 0x75: "long", 0x76: "float", 0x77: "double"}[op]
            return self.r12x("neg-" + kind, rs(d - 1), rs(d - 1))
        if op in (0x78, 0x7A, 0x7C, 0x79, 0x7B, 0x7D):
            return "%s-%s %s, %s, %s" % (
                {0x78: "shl", 0x7A: "shr", 0x7C: "ushr",
                 0x79: "shl", 0x7B: "shr", 0x7D: "ushr"}[op],
                "int" if op in (0x78, 0x7A, 0x7C) else "long",
                t_v(rs(d - 2)), t_v(rs(d - 2)), t_v(rs(d - 1)))
        if op in (0x7E, 0x80, 0x82, 0x7F, 0x81, 0x83):
            return "%s-%s %s, %s, %s" % (
                {0x7E: "and", 0x80: "or", 0x82: "xor",
                 0x7F: "and", 0x81: "or", 0x83: "xor"}[op],
                "int" if op in (0x7E, 0x80, 0x82) else "long",
                t_v(rs(d - 2)), t_v(rs(d - 2)), t_v(rs(d - 1)))
        if op == 0x84:
            idx, k = ops
            name = "add-int/lit8" if -128 <= k <= 127 else "add-int/lit16"
            return self.r22b(name, t.loc(idx), t.loc(idx), k)
        if op in CONV:
            pops = CONV_SIZE[op][0]
            return self.r12x(CONV[op], rs(d - pops), rs(d - pops))
        if op == 0x94:
            return "cmp-long %s, %s, %s" % (t_v(rs(d - 2)), t_v(rs(d - 2)),
                                           t_v(rs(d - 1)))
        if op in (0x95, 0x96, 0x97, 0x98):
            name = {0x95: "cmpl-float", 0x96: "cmpg-float",
                    0x97: "cmpl-double", 0x98: "cmpg-double"}[op]
            return "%s %s, %s, %s" % (name, t_v(rs(d - 2)), t_v(rs(d - 2)),
                                      t_v(rs(d - 1)))

        # ---- branches
        if 0x99 <= op <= 0x9E:
            return "%s %s, :L%d" % (
                {0x99: "if-eqz", 0x9A: "if-nez", 0x9B: "if-ltz", 0x9C: "if-gez",
                 0x9D: "if-gtz", 0x9E: "if-lez"}[op],
                t_v(rs(d - 1)), ops[0])
        if 0x9F <= op <= 0xA4:
            return self.r22t({0x9F: "if-eq", 0xA0: "if-ne", 0xA1: "if-lt",
                              0xA2: "if-ge", 0xA3: "if-gt", 0xA4: "if-le"}[op],
                             rs(d - 2), rs(d - 1), ":L%d" % ops[0])
        if 0xA5 <= op <= 0xA6:
            return self.r22t("if-eq" if op == 0xA5 else "if-ne", rs(d - 2),
                             rs(d - 1), ":L%d" % ops[0])
        if op in (0xC6, 0xC7):
            return "%s %s, :L%d" % ("if-eqz" if op == 0xC6 else "if-nez",
                                    t_v(rs(d - 1)), ops[0])
        if op in (0xA7, 0xC8):
            return "goto :L%d" % ops[0]
        if op in (0xA8, 0xA9, 0xC9):
            raise Unsupported("jsr/ret at %d" % pc)
        if op == 0xAA:
            default, low, offs = ops
            name = "p%d" % pc
            payloads.append((name, low, [(":L%d" % o) for o in offs]))
            return "packed-switch %s, :%s\n%s" % (t_v(rs(d - 1)), name,
                                                  "goto :L%d" % default)
        if op == 0xAB:
            default, pairs = ops
            name = "s%d" % pc
            payloads.append((name, None, [("%d" % k, ":L%d" % o)
                                          for (k, o) in pairs]))
            return "sparse-switch %s, :%s\n%s" % (t_v(rs(d - 1)), name,
                                                  "goto :L%d" % default)

        # ---- returns
        if op == 0xAC:
            return "return %s" % t_v(rs(d - 1))
        if op == 0xAD:
            return "return-wide %s" % t_v(rs(d - 1))
        if op == 0xB0:
            return "return-object %s" % t_v(rs(d - 1))
        if op == 0xB1:
            return "return-void"
        if op == 0xAE:
            return "return %s" % t_v(rs(d - 1))
        if op == 0xAF:
            return "return-wide %s" % t_v(rs(d - 1))

        # ---- fields
        if op in (0xB2, 0xB3, 0xB4, 0xB5):
            owner, name, desc, _k = cf.ref(ops[0])
            sfx = self.field_suffix(desc)
            ref = "L%s;->%s:%s" % (owner, name, desc)
            if op == 0xB2:
                return "sget%s %s, %s" % (sfx, t_v(rs(d)), ref)
            if op == 0xB3:
                return "sput%s %s, %s" % (sfx, t_v(rs(d - 1)), ref)
            if op == 0xB4:
                return self.r22c("iget" + sfx, rs(d - 1), rs(d - 1), ref)
            return self.r22c("iput" + sfx, rs(d - 1), rs(d - 2), ref)

        # ---- invokes
        if 0xB6 <= op <= 0xB9:
            return self.invoke(t, pc, op, ops, pat)
        if op == 0xBA:
            raise Unsupported("invokedynamic at %d" % pc)

        # ---- objects and arrays
        if op == 0xBB:
            return "new-instance %s, %s" % (t_v(rs(d)),
                                            smali_type(cf.class_name(ops[0])))
        if op == 0xBC:
            atypes = {4: "Z", 5: "C", 6: "F", 7: "D", 8: "B", 9: "S", 10: "I",
                      11: "J"}
            ty = atypes[ops[0]]
            return self.r22c("new-array", rs(d - 1), rs(d - 1), "[%s" % ty)
        if op == 0xBD:
            return self.r22c("new-array", rs(d - 1), rs(d - 1),
                             "[%s" % smali_type(cf.class_name(ops[0])))
        if op == 0xBF:
            return "throw %s" % t_v(rs(d - 1))
        if op == 0xC0:
            return "check-cast %s, %s" % (t_v(rs(d - 1)),
                                          smali_type(cf.class_name(ops[0])))
        if op == 0xC1:
            return self.r22c("instance-of", rs(d - 1), rs(d - 1),
                             smali_type(cf.class_name(ops[0])))
        if op == 0xC2:
            return "monitor-enter %s" % t_v(rs(d - 1))
        if op == 0xC3:
            return "monitor-exit %s" % t_v(rs(d - 1))
        if op == 0xC5:
            raise Unsupported("multianewarray at %d" % pc)
        raise Unsupported("opcode 0x%02x at %d in %s" % (op, pc, t.m.name))

    # -- pieces --------------------------------------------------------
    def juggle(self, t, op, pat):
        sw = slot_widths(pat)
        k = JUGGLE_SLOTS[op]
        if k > len(sw):
            raise Unsupported("stack underflow in juggle in %s" % t.m.name)
        base = sum(sw[:-k]) if k else sum(sw)
        out = JUGGLE_OUT[op]
        if not out:
            return None
        lines = []
        # stash the popped slots into the scratch registers
        i = 0
        while i < k:
            widep = (sw[base + i] == 2)
            if widep:
                lines.append("move-wide v%d, %s" % (i, t_v(t.st(base + i))))
                i += 2
            else:
                lines.append("move/from16 v%d, %s" % (i, t_v(t.st(base + i))))
                i += 1
        # write them back according to the permutation
        i = 0
        while i < len(out):
            kind_wide = (sw[base + out[i]] == 2)
            if kind_wide:
                lines.append(t.mv(t.st(base + i), i if False else out[i],
                                  wide=True))
                i += 2
            else:
                lines.append(t.mv(t.st(base + i), out[i]))
                i += 1
        return "\n".join(lines)

    def ldc(self, cf, c, reg):
        tag = c[0]
        if tag == classfile.TAG_INT:
            return const_insn(reg, c[1])
        if tag == classfile.TAG_LONG:
            return const_wide_insn(reg, c[1])
        if tag == classfile.TAG_FLOAT:
            bits = struct.unpack("<I", struct.pack("<f", c[1]))[0]
            return "const %s, 0x%08x" % (t_v(reg), bits)
        if tag == classfile.TAG_STRING:
            return 'const-string %s, "%s"' % (t_v(reg),
                                              escape_string(cf.utf8(c[1])))
        if tag == classfile.TAG_CLASS:
            return "const-class %s, %s" % (t_v(reg),
                                           smali_type(cf.class_name(c[1])))
        raise Unsupported("ldc constant tag %d" % tag)

    def invoke(self, t, pc, op, ops, pat):
        cf = t.cf
        owner, name, desc, _kind = cf.ref(ops[0])
        widths = [slot_size(x) for x in parse_types(arg_desc(desc))]
        nargs = sum(widths)
        recv = 0 if op == 0xB8 else 1
        selfinit = pc in t._selfinit_pcs
        nvalues = len(widths) + recv
        d = len(pat)
        first = d - nvalues
        if selfinit:
            # the receiver is the object itself; it is not consumed
            nvalues = len(widths)
            first = d - nvalues - 1
        if first < 0:
            raise Unsupported("invoke argument underflow in %s" % t.m.name)
        start = t.st(slot_off(pat, first))
        total = nargs + recv
        regs = [start + i for i in range(total)]
        use_range = total > 5 or (regs and max(regs) >= 16)
        if use_range:
            argtxt = "{v%d .. v%d}" % (regs[0], regs[-1])
        else:
            argtxt = "{%s}" % ", ".join(t_v(r) for r in regs)
        if op == 0xB6:
            kind = "invoke-virtual"
        elif op == 0xB9:
            kind = "invoke-interface"
        elif op == 0xB8:
            kind = "invoke-static"
        elif name in ("<init>", "<clinit>") or owner == cf.name:
            kind = "invoke-direct"
        else:
            kind = "invoke-super"
        if use_range:
            kind += "/range"
        ret = ret_desc(desc)
        text = "%s %s, L%s;->%s(%s)%s" % (kind, argtxt, owner, name,
                                          arg_desc(desc), ret)
        if ret != "V":
            if ret in ("J", "D"):
                suffix = "-wide"
            elif ret.startswith("L") or ret.startswith("["):
                suffix = "-object"
            else:
                suffix = ""
            text += "\n" + "move-result%s %s" % (suffix, t_v(start))
        return text


def convert_tree(src_dir, out_dir, verbose=False):
    conv = Converter(verbose)
    n = 0
    for root, _dirs, files in os.walk(src_dir):
        for fn in sorted(files):
            if not fn.endswith(".class"):
                continue
            path = os.path.join(root, fn)
            cf = classfile.read_file(path)
            text = conv.convert(cf)
            rel = os.path.relpath(path, src_dir)[:-len(".class")] + ".smali"
            target = os.path.join(out_dir, rel)
            d = os.path.dirname(target)
            if d and not os.path.isdir(d):
                os.makedirs(d)
            with open(target, "w") as fh:
                fh.write(text)
            n += 1
            if verbose:
                print("  %s" % rel)
    return n, conv.stats


def main(argv):
    if len(argv) < 3:
        sys.stderr.write("usage: jvm2smali.py <classes-dir> <smali-dir> [-v]\n")
        return 2
    n, stats = convert_tree(argv[1], argv[2], "-v" in argv)
    print("converted %d classes, %d methods" % (n, stats["methods"]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
