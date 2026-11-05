#!/usr/bin/env python3
"""Mini MIPS R3000 assembler + ISO9660 writer used to build the demo discs.

Produces bootable PlayStation images (SYSTEM.CNF + PSX.EXE) that exercise the
emulator: GPU rasterizer, block font, GTE 3D pipeline and the SPU.

    python3 tools/mkdemo.py --out res/demos          # build every demo
    python3 tools/mkdemo.py --out res/demos --only shapes

Only the Python standard library is used, everything is little endian.
"""

import argparse
import os
import struct
import sys

SECTOR = 2048

# --------------------------------------------------------------------------
#  assembler
# --------------------------------------------------------------------------

REGS = {
    "zero": 0, "at": 1, "v0": 2, "v1": 3, "a0": 4, "a1": 5, "a2": 6, "a3": 7,
    "t0": 8, "t1": 9, "t2": 10, "t3": 11, "t4": 12, "t5": 13, "t6": 14, "t7": 15,
    "s0": 16, "s1": 17, "s2": 18, "s3": 19, "s4": 20, "s5": 21, "s6": 22, "s7": 23,
    "t8": 24, "t9": 25, "k0": 26, "k1": 27, "gp": 28, "sp": 29, "fp": 30, "ra": 31,
}


def reg(name):
    name = name.strip()
    if name.startswith("$"):
        name = name[1:]
    if name in REGS:
        return REGS[name]
    return int(name, 0)


class Asm(object):
    """Two pass assembler: labels, data words, pseudo instructions."""

    def __init__(self, org=0x80010000):
        self.org = org
        self.items = []          # (kind, payload) kind: 'ins' | 'label' | 'word'
        self.labels = {}

    # ---- emission --------------------------------------------------------
    def ins(self, fn):
        self.items.append(("ins", fn))

    def label(self, name):
        self.items.append(("label", name))

    def words(self, *values):
        for v in values:
            self.items.append(("word", v & 0xFFFFFFFF))

    def data(self, values):
        for v in values:
            self.items.append(("word", v & 0xFFFFFFFF))

    # ---- pseudo ops ------------------------------------------------------
    def li(self, rt, value):
        value &= 0xFFFFFFFF
        lo = value & 0xFFFF
        hi = (value >> 16) & 0xFFFF
        if hi != 0:
            self.ins(lambda pc, t=rt, h=hi: (0x0F << 26) | (t << 16) | h)
        if lo != 0 or hi == 0:
            if hi != 0:
                self.ins(lambda pc, t=rt, l=lo: (0x0D << 26) | (t << 21) | (t << 16) | l)
            else:
                self.ins(lambda pc, t=rt, l=lo: (0x0D << 26) | (t << 16) | l)

    def la(self, rt, label):
        """Load the absolute address of a label (org + offset)."""
        def emit(pc, t=rt, lb=label):
            addr = self.labels[lb]
            hi = (addr >> 16) & 0xFFFF
            lo = addr & 0xFFFF
            return ("pair", (0x0F << 26) | (t << 16) | hi,
                    (0x0D << 26) | (t << 21) | (t << 16) | lo)
        self.items.append(("la", emit))

    def nop(self):
        self.ins(lambda pc: 0)

    # ---- real instructions ------------------------------------------------
    def _r(self, rs, rt, rd, sa, fn):
        self.ins(lambda pc: (rs << 21) | (rt << 16) | (rd << 11) | (sa << 6) | fn)

    def _i(self, op, rs, rt, imm):
        self.ins(lambda pc: (op << 26) | (rs << 21) | (rt << 16) | (imm & 0xFFFF))

    def _branch(self, op, rs, rt, label):
        def emit(pc):
            off = (self.labels[label] - (pc + 4)) >> 2
            return (op << 26) | (rs << 21) | (rt << 16) | (off & 0xFFFF)
        self.ins(emit)

    def addiu(self, rt, rs, imm): self._i(0x09, rs, rt, imm)
    def addi(self, rt, rs, imm): self._i(0x08, rs, rt, imm)
    def ori(self, rt, rs, imm): self._i(0x0D, rs, rt, imm)
    def andi(self, rt, rs, imm): self._i(0x0C, rs, rt, imm)
    def xori(self, rt, rs, imm): self._i(0x0E, rs, rt, imm)
    def lui(self, rt, imm): self._i(0x0F, 0, rt, imm)
    def slti(self, rt, rs, imm): self._i(0x0A, rs, rt, imm)
    def sltiu(self, rt, rs, imm): self._i(0x0B, rs, rt, imm)
    def lw(self, rt, off, base): self._i(0x23, base, rt, off)

    def lw_safe(self, rt, off, base):
        """Load + nop: MIPS I exposes the old value in the delay slot."""
        self.lw(rt, off, base)
        self.nop()
    def lh(self, rt, off, base): self._i(0x21, base, rt, off)
    def lb(self, rt, off, base): self._i(0x20, base, rt, off)
    def lbu(self, rt, off, base): self._i(0x24, base, rt, off)
    def sw(self, rt, off, base): self._i(0x2B, base, rt, off)
    def sh(self, rt, off, base): self._i(0x29, base, rt, off)
    def sb(self, rt, off, base): self._i(0x28, base, rt, off)

    def addu(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x21)
    def subu(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x23)
    def and_(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x24)
    def or_(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x25)
    def xor(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x26)
    def nor(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x27)
    def slt(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x2A)
    def sltu(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x2B)
    def movz(self, rd, rs, rt): self._r(rs, rt, rd, 0, 0x0A)
    def sll(self, rd, rt, sa): self._r(0, rt, rd, sa, 0x00)
    def srl(self, rd, rt, sa): self._r(0, rt, rd, sa, 0x02)
    def sra(self, rd, rt, sa): self._r(0, rt, rd, sa, 0x03)
    def mult(self, rs, rt): self._r(rs, rt, 0, 0, 0x18)
    def div(self, rs, rt): self._r(rs, rt, 0, 0, 0x1A)
    def divu(self, rs, rt): self._r(rs, rt, 0, 0, 0x1B)
    def mfhi(self, rd): self._r(0, 0, rd, 0, 0x10)
    def mflo(self, rd): self._r(0, 0, rd, 0, 0x12)
    def jr(self, rs): self._r(rs, 0, 0, 0, 0x08)
    def jalr(self, rs, rd=31): self._r(rs, 0, rd, 0, 0x09)
    def syscall(self): self._r(0, 0, 0, 0, 0x0C)

    def beq(self, rs, rt, label): self._branch(0x04, rs, rt, label)
    def bne(self, rs, rt, label): self._branch(0x05, rs, rt, label)
    def bltz(self, rs, label): self._branch(0x01, rs, 0x00, label)
    def bgez(self, rs, label): self._branch(0x01, rs, 0x01, label)
    def blez(self, rs, label): self._branch(0x06, rs, 0, label)
    def bgtz(self, rs, label): self._branch(0x07, rs, 0, label)

    def j(self, label):
        def emit(pc):
            target = self.labels[label]
            return (0x02 << 26) | ((target >> 2) & 0x03FFFFFF)
        self.ins(emit)

    def jal(self, label):
        def emit(pc):
            target = self.labels[label]
            return (0x03 << 26) | ((target >> 2) & 0x03FFFFFF)
        self.ins(emit)

    # ---- COP0 / COP2 -----------------------------------------------------
    # COP2 instructions: opcode 0x12 in bits 26-31, sub-op in the rs field
    def _cop2(self, sub, rt, rd):
        self.ins(lambda pc: (0x12 << 26) | (sub << 21) | (rt << 16) | (rd << 11))
    def mtc2(self, rt, rd): self._cop2(0x04, rt, rd)
    def mfc2(self, rt, rd): self._cop2(0x00, rt, rd)
    def cfc2(self, rt, rd): self._cop2(0x02, rt, rd)
    def ctc2(self, rt, rd): self._cop2(0x06, rt, rd)

    def gte(self, value):
        """COP2 command word (bit 25 set => command)."""
        self.ins(lambda pc: value & 0xFFFFFFFF)

    # ---- assembling ------------------------------------------------------
    def _pass1(self):
        addr = self.org
        for kind, payload in self.items:
            if kind == "label":
                self.labels[payload] = addr
            elif kind in ("word", "ins"):
                addr += 4
            elif kind == "la":
                addr += 8

    def assemble(self):
        # pass 1: addresses
        addr = self.org
        for kind, payload in self.items:
            if kind == "label":
                self.labels[payload] = addr
            elif kind == "word":
                addr += 4
            elif kind == "ins":
                addr += 4
            elif kind == "la":
                addr += 8
        # pass 2: encode
        out = bytearray()
        addr = self.org
        for kind, payload in self.items:
            if kind == "label":
                continue
            if kind == "word":
                out += struct.pack("<I", payload)
                addr += 4
            elif kind == "ins":
                out += struct.pack("<I", payload(addr) & 0xFFFFFFFF)
                addr += 4
            elif kind == "la":
                kind2, w1, w2 = payload(addr)
                out += struct.pack("<II", w1, w2)
                addr += 8
        return bytes(out)

    def here(self):
        """Current address (side effect free)."""
        addr = self.org
        for kind, payload in self.items:
            if kind in ("word", "ins"):
                addr += 4
            elif kind == "la":
                addr += 8
        return addr

    def uniq(self, prefix="L"):
        """Returns a unique label name (labels must be resolvable up front)."""
        self._counter = getattr(self, "_counter", 0) + 1
        return "%s_%d" % (prefix, self._counter)

    def align16(self):
        while ((self.here() - self.org) & 0xF) != 0:
            self.nop()


# --------------------------------------------------------------------------
#  ISO9660 image
# --------------------------------------------------------------------------

def _le32(v):
    return struct.pack("<I", v & 0xFFFFFFFF)


def _be32(v):
    return struct.pack(">I", v & 0xFFFFFFFF)


def _dir_record(lba, size, is_dir, name):
    name_bytes = name.encode("ascii")
    ln = 33 + len(name_bytes)
    if ln & 1:
        ln += 1
    rec = bytearray(ln)
    rec[0] = ln
    rec[1] = 0
    rec[2:6] = _le32(lba)
    rec[6:10] = _be32(lba)
    rec[10:14] = _le32(size)
    rec[14:18] = _be32(size)
    rec[25] = 2 if is_dir else 0
    rec[28] = 1
    rec[31] = 1
    rec[32] = len(name_bytes)
    rec[33:33 + len(name_bytes)] = name_bytes
    return bytes(rec)


def build_iso(files, volume_id="PS1J2ME_DEMO"):
    """files: ordered list of (name, bytes). Returns the ISO image."""
    root_lba = 20
    data_lba = 21
    layout = []
    lba = data_lba
    for name, data in files:
        sectors = max(1, (len(data) + SECTOR - 1) // SECTOR)
        layout.append((name, data, lba, sectors))
        lba += sectors
    total = lba + 1

    img = bytearray(total * SECTOR)

    # PVD
    pvd = bytearray(SECTOR)
    pvd[0] = 1
    pvd[1:6] = b"CD001"
    pvd[6] = 1
    pvd[8:40] = b"PLAYSTATION".ljust(32, b" ")
    pvd[40:72] = volume_id.encode("ascii")[:32].ljust(32, b" ")
    pvd[80:88] = _le32(total)
    pvd[88:96] = _be32(total)
    pvd[120:122] = struct.pack("<H", 1)
    pvd[122:124] = struct.pack(">H", 1)
    pvd[124:126] = struct.pack("<H", SECTOR)
    pvd[126:128] = struct.pack(">H", SECTOR)
    root = _dir_record(root_lba, SECTOR, True, "\x00")
    pvd[156:156 + len(root)] = root
    img[16 * SECTOR:17 * SECTOR] = pvd
    # terminator
    term = bytearray(SECTOR)
    term[0] = 255
    term[1:6] = b"CD001"
    term[6] = 1
    img[17 * SECTOR:18 * SECTOR] = term

    # root directory
    dirsec = bytearray(SECTOR)
    off = 0
    rec = _dir_record(root_lba, SECTOR, True, "\x00")
    dirsec[off:off + len(rec)] = rec
    off += len(rec)
    rec = _dir_record(root_lba, SECTOR, True, "\x01")
    dirsec[off:off + len(rec)] = rec
    off += len(rec)
    for name, data, flba, _ in layout:
        rec = _dir_record(flba, len(data), False, name)
        dirsec[off:off + len(rec)] = rec
        off += len(rec)
    img[root_lba * SECTOR:(root_lba + 1) * SECTOR] = dirsec

    for name, data, flba, sectors in layout:
        start = flba * SECTOR
        img[start:start + len(data)] = data
    return bytes(img)


def build_exe(code, org=0x80010000, stack=0x801FFFF0, name="PS1J2ME DEMO"):
    exe = bytearray(0x800 + len(code))
    exe[0:8] = b"PS-X EXE"
    exe[0x0C:0x0C + len(name)] = name.encode("ascii")[:60]
    exe[0x10:0x14] = _le32(org)          # pc
    exe[0x14:0x18] = _le32(0)            # gp
    exe[0x18:0x1C] = _le32(org)          # load address
    exe[0x1C:0x20] = _le32(len(code))    # size
    exe[0x30:0x34] = _le32(stack)        # stack pointer
    exe[0x34:0x38] = _le32(len(code))
    exe[0x800:0x800 + len(code)] = code
    return bytes(exe)


def build_disc(code, volume_id, org=0x80010000):
    exe = build_exe(code, org)
    cnf = ("BOOT = cdrom:\\PSX.EXE;1\r\n"
           "TCB = 4\r\n"
           "EVENT = 10\r\n"
           "STACK = 801FFFF0\r\n").encode("ascii")
    return build_iso([("SYSTEM.CNF;1", cnf), ("PSX.EXE;1", exe)], volume_id)


# --------------------------------------------------------------------------
#  demo building blocks
# --------------------------------------------------------------------------

GP0 = 0x1F801810
GP1 = 0x1F801814
SCRATCH = 0x1F800000


def init_gpu(a, fill=0x02000810):
    """Reset, display on, 320x240, full draw area, dark background."""
    a.li(9, GP0)
    a.li(8, GP1)
    a.li(10, 0x00000000)
    a.sw(10, 0, 9)                      # GP0 reset
    a.li(10, 0x03000000)
    a.sw(10, 0, 8)                      # display on
    a.li(10, 0x05000000)
    a.sw(10, 0, 8)                      # display start 0,0
    a.li(10, 0x08000001)
    a.sw(10, 0, 8)                      # 320x240
    a.li(10, 0xE3000000)
    a.sw(10, 0, 9)
    a.li(10, 0xE4000000 | (239 << 10) | 319)
    a.sw(10, 0, 9)
    a.li(10, 0xE5000000)
    a.sw(10, 0, 9)
    a.li(10, 0xE1010000)                # texpage, 15 bit
    a.sw(10, 0, 9)
    fill_screen(a, fill)


def gp0(a, word):
    """Emits the store of one GP0 word.

    The GP0 base is reloaded every time on purpose. It used to live in t1 and
    helpers such as draw_number() borrowed that register for their own loops,
    which sent the following stores to address 1..5 and raised address-error
    exceptions in the middle of a frame.
    """
    a.li(9, GP0)
    a.li(10, word & 0xFFFFFFFF)
    a.sw(10, 0, 9)


def gp1(a, word):
    a.li(11, GP1)
    a.li(10, word & 0xFFFFFFFF)
    a.sw(10, 0, 11)


def fill_screen(a, color):
    gp0(a, 0x02000000 | (color & 0xFFFFFF))
    gp0(a, 0x00000000)
    gp0(a, 0x00F00140)                  # 320x240 at 0,0


def delay(a, outer=8, inner=400):
    """Nested busy loop (~outer*inner*4 instructions)."""
    lab_o = a.uniq("dly_o")
    lab_i = a.uniq("dly_i")
    a.li(20, outer)
    a.label(lab_o)
    a.li(21, inner)
    a.label(lab_i)
    a.addiu(21, 21, -1)
    a.bne(21, 0, lab_i)
    a.nop()
    a.addiu(20, 20, -1)
    a.bne(20, 0, lab_o)
    a.nop()


def wait_vblank(a):
    """Waits for one toggle of the GPU field bit (GPUSTAT bit 31).

    Bounded on purpose: if a device (or emulator build) never toggles the bit,
    the demo continues instead of spinning forever, which used to look like a
    frozen emulator.
    """
    l_poll = a.uniq("vb")
    l_done = a.uniq("vbx")
    a.li(11, GP1)
    a.lw_safe(12, 0, 11)
    a.srl(12, 12, 31)           # field bit of the current field
    a.li(6, 0)                  # poll counter
    a.label(l_poll)
    a.lw_safe(13, 0, 11)
    a.srl(13, 13, 31)
    # The flip is the *good* case, so branch out on difference. The old code
    # branched back on difference while the counter was still small, which made
    # every demo wait for the poll bound and run at half speed.
    a.bne(13, 12, l_done)
    a.addiu(6, 6, 1)            # delay slot: count this poll
    a.sltiu(7, 6, 40000)        # give up after 40k polls
    a.bne(7, 0, l_poll)
    a.nop()
    a.label(l_done)


def triangle(a, x0, y0, x1, y1, x2, y2, color, semi=False):
    cmd = 0x20000000 | (color & 0xFFFFFF)
    if semi:
        cmd |= 0x02 << 24
    gp0(a, cmd)
    gp0(a, (x0 & 0xFFFF) | ((y0 & 0xFFFF) << 16))
    gp0(a, (x1 & 0xFFFF) | ((y1 & 0xFFFF) << 16))
    gp0(a, (x2 & 0xFFFF) | ((y2 & 0xFFFF) << 16))


def gouraud_tri(a, p0, c0, p1, c1, p2, c2):
    """Gouraud triangle: colour 0 rides in the command word, then
    xy1, colour1, xy2, colour2 (psx-spx polygon layout)."""
    gp0(a, 0x30000000 | (c0 & 0xFFFFFF))
    gp0(a, (p0[0] & 0xFFFF) | ((p0[1] & 0xFFFF) << 16))
    gp0(a, c1)
    gp0(a, (p1[0] & 0xFFFF) | ((p1[1] & 0xFFFF) << 16))
    gp0(a, c2)
    gp0(a, (p2[0] & 0xFFFF) | ((p2[1] & 0xFFFF) << 16))


def quad(a, p0, p1, p2, p3, color, semi=False):
    cmd = 0x28000000 | (color & 0xFFFFFF)
    if semi:
        cmd |= 0x02 << 24
    gp0(a, cmd)
    for (x, y) in (p0, p1, p2, p3):
        gp0(a, (x & 0xFFFF) | ((y & 0xFFFF) << 16))


def sprite(a, x, y, w, h, color):
    gp0(a, 0x60000000 | (color & 0xFFFFFF))
    gp0(a, (x & 0xFFFF) | ((y & 0xFFFF) << 16))
    gp0(a, ((w - 1) & 0xFFFF) | (((h - 1) & 0xFFFF) << 16))


def line(a, x0, y0, x1, y1, color):
    gp0(a, 0x40000000 | (color & 0xFFFFFF))
    gp0(a, (x0 & 0xFFFF) | ((y0 & 0xFFFF) << 16))
    gp0(a, (x1 & 0xFFFF) | ((y1 & 0xFFFF) << 16))


# --------------------------------------------------------------------------
#  demo 1: GPU shapes
# --------------------------------------------------------------------------

def demo_shapes():
    a = Asm()
    init_gpu(a, 0x00402010)
    a.li(16, 20)                    # animated sprite x

    a.label("main")
    wait_vblank(a)
    fill_screen(a, 0x00402010)

    # flat triangles
    triangle(a, 10, 20, 110, 20, 60, 110, 0x000000FF)
    triangle(a, 10, 130, 110, 130, 60, 220, 0x0000FF00)
    # gouraud triangle
    gouraud_tri(a, (130, 20), 0x0000FF00, (300, 40), 0x00FF0000, (210, 150), 0x00FFFFFF)
    # semi transparent quad over the shapes
    quad(a, (40, 160), (160, 170), (200, 235), (20, 235), 0x0080FFFF, semi=True)
    # colour bars (sprites)
    for i in range(8):
        sprite(a, 20 + i * 34, 205, 24, 16, (0xFF0000 >> ((i % 3) * 8)) | (0x0000FF if i % 2 else 0x008000))
    # moving 16x16 sprite driven by a counter (position in s0)
    sprite(a, 20, 128, 16, 16, 0x00FF8000)
    a.addiu(16, 16, 4)
    a.slti(17, 16, 280)
    a.bne(17, 0, "noreset")
    a.nop()
    a.li(16, 20)
    a.label("noreset")
    a.j("main")
    a.nop()
    return a


# --------------------------------------------------------------------------
#  demo 2: block font logo
# --------------------------------------------------------------------------

FONT = {
    "A": [0x0E, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11],
    "B": [0x1E, 0x11, 0x11, 0x1E, 0x11, 0x11, 0x1E],
    "C": [0x0E, 0x11, 0x10, 0x10, 0x10, 0x11, 0x0E],
    "D": [0x1E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x1E],
    "E": [0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x1F],
    "F": [0x1F, 0x10, 0x10, 0x1E, 0x10, 0x10, 0x10],
    "G": [0x0E, 0x11, 0x10, 0x17, 0x11, 0x11, 0x0F],
    "H": [0x11, 0x11, 0x11, 0x1F, 0x11, 0x11, 0x11],
    "I": [0x0E, 0x04, 0x04, 0x04, 0x04, 0x04, 0x0E],
    "J": [0x07, 0x02, 0x02, 0x02, 0x02, 0x12, 0x0E],
    "K": [0x11, 0x12, 0x14, 0x18, 0x14, 0x12, 0x11],
    "L": [0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x1F],
    "M": [0x11, 0x1B, 0x15, 0x15, 0x11, 0x11, 0x11],
    "N": [0x11, 0x19, 0x19, 0x15, 0x13, 0x13, 0x11],
    "O": [0x0E, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E],
    "P": [0x1E, 0x11, 0x11, 0x1E, 0x10, 0x10, 0x10],
    "Q": [0x0E, 0x11, 0x11, 0x11, 0x15, 0x12, 0x0D],
    "R": [0x1E, 0x11, 0x11, 0x1E, 0x14, 0x12, 0x11],
    "S": [0x0F, 0x10, 0x10, 0x0E, 0x01, 0x01, 0x1E],
    "T": [0x1F, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04],
    "U": [0x11, 0x11, 0x11, 0x11, 0x11, 0x11, 0x0E],
    "V": [0x11, 0x11, 0x11, 0x11, 0x11, 0x0A, 0x04],
    "W": [0x11, 0x11, 0x11, 0x15, 0x15, 0x1B, 0x11],
    "X": [0x11, 0x11, 0x0A, 0x04, 0x0A, 0x11, 0x11],
    "Y": [0x11, 0x11, 0x0A, 0x04, 0x04, 0x04, 0x04],
    "Z": [0x1F, 0x01, 0x02, 0x04, 0x08, 0x10, 0x1F],
    "0": [0x0E, 0x11, 0x13, 0x15, 0x19, 0x11, 0x0E],
    "1": [0x04, 0x0C, 0x04, 0x04, 0x04, 0x04, 0x0E],
    "2": [0x0E, 0x11, 0x01, 0x06, 0x08, 0x10, 0x1F],
    "3": [0x1F, 0x02, 0x04, 0x02, 0x01, 0x11, 0x0E],
    "4": [0x02, 0x06, 0x0A, 0x12, 0x1F, 0x02, 0x02],
    "5": [0x1F, 0x10, 0x1E, 0x01, 0x01, 0x11, 0x0E],
    "6": [0x06, 0x08, 0x10, 0x1E, 0x11, 0x11, 0x0E],
    "7": [0x1F, 0x01, 0x02, 0x04, 0x08, 0x08, 0x08],
    "8": [0x0E, 0x11, 0x11, 0x0E, 0x11, 0x11, 0x0E],
    "9": [0x0E, 0x11, 0x11, 0x0F, 0x01, 0x02, 0x0C],
    ".": [0x00, 0x00, 0x00, 0x00, 0x00, 0x0C, 0x0C],
    ",": [0x00, 0x00, 0x00, 0x00, 0x0C, 0x04, 0x08],
    "-": [0x00, 0x00, 0x00, 0x1F, 0x00, 0x00, 0x00],
    ":": [0x00, 0x0C, 0x0C, 0x00, 0x0C, 0x0C, 0x00],
    "/": [0x01, 0x02, 0x02, 0x04, 0x08, 0x08, 0x10],
    "!": [0x04, 0x04, 0x04, 0x04, 0x04, 0x00, 0x04],
    " ": [0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00],
}


def draw_text(a, text, x, y, scale, color):
    for ci, ch in enumerate(text):
        rows = FONT.get(ch, FONT[" "])
        base_x = x + ci * 6 * scale
        for ry, bits in enumerate(rows):
            for rx in range(5):
                if (bits >> (4 - rx)) & 1:
                    sprite(a, base_x + rx * scale, y + ry * scale, scale, scale, color)


def demo_logo():
    a = Asm()
    init_gpu(a, 0x00001828)

    a.label("main")
    wait_vblank(a)
    # gradient bands
    for i in range(12):
        gp0(a, 0x02000000 | ((i * 8) << 8) | ((0x20 - i) << 16))
        gp0(a, i * 20 << 16)
        gp0(a, 0x00140140)
    # shadow + logo
    draw_text(a, "PS1J2ME", 40, 62, 6, 0x00000000)
    draw_text(a, "PS1J2ME", 36, 58, 6, 0x00FFFFFF)
    draw_text(a, "PLAYSTATION", 70, 130, 3, 0x0080C0FF)
    draw_text(a, "DEMO  1.0", 100, 165, 3, 0x00FFD000)
    # blinking cursor
    a.li(17, 0)
    a.label("blink")
    sprite(a, 36 + 8 * 36, 58, 6, 6, 0x00FF3030)
    delay(a, 6, 400)
    wait_vblank(a)
    sprite(a, 36 + 8 * 36, 58, 6, 6, 0x00001828)
    delay(a, 6, 400)
    a.j("blink")
    a.nop()
    return a


# --------------------------------------------------------------------------
#  demo 3: GTE rotating cube
# --------------------------------------------------------------------------

def rot_matrix(angle, scale=0x1000, tilt_factor=0.6):
    """Orthonormal R = Ry(angle) * Rx(angle*0.6) packed as 5 COP2 words.

    Coefficients are 4.12 fixed point (0x1000 == 1.0); the packing matches
    the GTE control registers 0..4 (two 16 bit values per word).
    """
    import math
    a = angle
    b = angle * tilt_factor
    ca, sa = math.cos(a), math.sin(a)
    cb, sb = math.cos(b), math.sin(b)
    m = [
        ca, sa * sb, sa * cb,
        0.0, cb, -sb,
        -sa, ca * sb, ca * cb,
    ]
    m = [int(round(v * scale)) & 0xFFFF for v in m]
    return [m[0] | (m[1] << 16),
            m[2] | (m[3] << 16),
            m[4] | (m[5] << 16),
            m[6] | (m[7] << 16),
            m[8]]


def project_screen(angle, verts, trz=0x600, h=0x140, ofx=160, ofy=120):
    """Projected screen coordinates, mirroring the RTPT pipeline."""
    out = []
    for (X, Y, Z) in project(angle, verts, trz):
        if Z <= 0:
            out.append((ofx, ofy))
            continue
        sx = ofx + int((h * 0x10000 // Z) * X / 65536.0)
        sy = ofy + int((h * 0x10000 // Z) * Y / 65536.0)
        out.append((sx, sy))
    return out


def project(angle, verts, trz=0x600, scale=0x1000):
    """CPU-side copy of the RTPT math, used to order the cube faces."""
    import math
    m = rot_matrix(angle)
    coeff = []
    for word in m[:4]:
        coeff.append(word & 0xFFFF)
        coeff.append((word >> 16) & 0xFFFF)
    coeff.append(m[4] & 0xFFFF)
    coeff = [(c ^ 0x8000) - 0x8000 for c in coeff]      # sign extend
    out = []
    for (x, y, z) in verts:
        X = (coeff[0] * x + coeff[1] * y + coeff[2] * z) >> 12
        Y = (coeff[3] * x + coeff[4] * y + coeff[5] * z) >> 12
        Z = (coeff[6] * x + coeff[7] * y + coeff[8] * z) >> 12
        out.append((X, Y, Z + trz))
    return out


CUBE_VERTS = [(-300, -300, -300), (300, -300, -300), (300, 300, -300), (-300, 300, -300),
              (-300, -300, 300), (300, -300, 300), (300, 300, 300), (-300, 300, 300)]
CUBE_FACES = [(0, 1, 2, 3), (4, 5, 6, 7), (0, 1, 5, 4),
              (2, 3, 7, 6), (0, 3, 7, 4), (1, 2, 6, 5)]
FACE_COLORS = [0x003060C0, 0x0030B060, 0x00C05040, 0x00A060C0, 0x005080D0, 0x00D0A040]


def demo_3d():
    """Rotating textured-face cube: GTE transforms, GPU polygons."""
    import math
    a = Asm()
    init_gpu(a, 0x00000810)

    # ---- GTE setup -------------------------------------------------------
    a.li(11, 0x140)          # H  (field of view)
    a.ctc2(11, 26)
    a.li(11, 160)            # OFX
    a.ctc2(11, 24)
    a.li(11, 120)            # OFY
    a.ctc2(11, 25)
    a.li(11, 0)
    a.ctc2(11, 5)            # TRX
    a.ctc2(11, 6)            # TRY
    a.li(11, 0x600)          # TRZ (camera distance)
    a.ctc2(11, 7)

    n_angles = 16
    tables = [rot_matrix(i * 2 * math.pi / n_angles, tilt_factor=0.5) for i in range(n_angles)]
    row_stride = 4 * n_angles

    a.li(16, 0)              # s0: angle index
    a.label("frame")
    wait_vblank(a)
    fill_screen(a, 0x00000810)

    # ---- load the rotation matrix for this angle ------------------------
    a.la(19, "mtx_table")
    a.sll(18, 16, 2)
    a.addu(19, 19, 18)
    for reg in range(5):
        a.lw_safe(11, reg * row_stride, 19)
        a.ctc2(11, reg)

    # ---- transform the 8 vertices (RTPT x3), stash SXY/SZ --------------
    for base in range(0, 8, 3):
        for k in range(3):
            idx = base + k
            if idx > 7:
                idx = 7
            x, y, z = CUBE_VERTS[idx]
            a.li(11, ((y & 0xFFFF) << 16) | (x & 0xFFFF))
            a.mtc2(11, k * 2)
            a.li(11, z & 0xFFFF)
            a.mtc2(11, k * 2 + 1)
        a.gte(0x4A080030)        # RTPT, sf = 1
        for k in range(3):
            idx = base + k
            if idx > 7:
                idx = 7
            a.mfc2(12, 12 + k)                  # SXY
            a.li(14, SCRATCH + idx * 4)
            a.sw(12, 0, 14)
            a.mfc2(12, 16 + k)                  # SZ
            a.li(14, SCRATCH + 64 + idx * 4)
            a.sw(12, 0, 14)
    a.mfc2(12, 19)                              # SZ3 (8th vertex)
    a.li(14, SCRATCH + 64 + 7 * 4)
    a.sw(12, 0, 14)

    # ---- jump to the precomputed back-to-front face order ---------------
    a.la(19, "face_ptrs")
    a.sll(18, 16, 2)
    a.addu(19, 19, 18)
    a.lw_safe(11, 0, 19)
    a.jr(11)
    a.nop()

    # one block per angle, faces sorted by depth
    for ai in range(n_angles):
        proj = project(ai * 2 * math.pi / n_angles, CUBE_VERTS)
        order = []
        for fi, face in enumerate(CUBE_FACES):
            depth = sum(proj[v][2] for v in face) / 4.0
            order.append((depth, fi))
        order.sort(reverse=True)                 # farthest first
        a.label("faces_%d" % ai)
        screen = project_screen(ai * 2 * math.pi / n_angles, CUBE_VERTS)
        for _, fi in order:
            face = CUBE_FACES[fi]
            p0, p1, p2, p3 = [screen[v] for v in face]
            d02 = (p0[0] - p2[0]) ** 2 + (p0[1] - p2[1]) ** 2
            d13 = (p1[0] - p3[0]) ** 2 + (p1[1] - p3[1]) ** 2
            if d02 <= d13:
                tris = ((face[0], face[1], face[2]), (face[0], face[2], face[3]))
            else:
                tris = ((face[0], face[1], face[3]), (face[1], face[2], face[3]))
            for tri in tris:
                a.li(10, 0x20000000 | FACE_COLORS[fi])
                a.sw(10, 0, 9)
                for vi in tri:
                    a.li(15, SCRATCH + vi * 4)
                    a.lw_safe(16, 0, 15)
                    a.sw(16, 0, 9)
        a.j("wire")

    a.label("wire")
    edges = [(0, 1), (1, 2), (2, 3), (3, 0), (4, 5), (5, 6), (6, 7), (7, 4),
             (0, 4), (1, 5), (2, 6), (3, 7)]
    for (i, j) in edges:
        a.li(14, SCRATCH + i * 4)
        a.lw_safe(15, 0, 14)
        a.li(14, SCRATCH + j * 4)
        a.lw_safe(16, 0, 14)
        a.li(10, 0x40FFFF00)                # flat line, cyan (R=00 G=FF B=FF)
        a.sw(10, 0, 9)
        a.sw(15, 0, 9)
        a.sw(16, 0, 9)

    a.addiu(16, 16, 1)
    a.andi(16, 16, n_angles - 1)
    a.j("frame")
    a.nop()

    # ---- data tables -----------------------------------------------------
    a.label("face_ptrs")
    a.data([1 for _ in range(n_angles)])        # patched below with real addresses
    a.label("mtx_table")
    for row in range(5):
        a.data([t[row] for t in tables])
    return a


# --------------------------------------------------------------------------
#  demo 4: SPU tone
# --------------------------------------------------------------------------

def adpcm_tone(blocks=32, period=8, amp=7, loop=True):
    out = bytearray()
    for b in range(blocks):
        header = 0x00
        if b == blocks - 1 and loop:
            header = 0x03        # loop end + repeat
        samples = []
        for i in range(28):
            n = (b * 28 + i) // period
            samples.append(amp if n % 2 == 0 else -amp)
        packed = []
        for i in range(0, 28, 2):
            packed.append(((samples[i + 1] & 0xF) << 4) | (samples[i] & 0xF))
        out += bytes([header] + packed)
    return bytes(out)


def demo_sound():
    """Uploads a looping ADPCM square wave into SPU RAM and plays it while
    drawing a bouncing bar (a quick visual reference for the audio demo)."""
    a = Asm()
    init_gpu(a, 0x00001020)

    # ---- SPU setup -------------------------------------------------------
    a.li(9, 0x1F801C00)
    a.li(10, 0x3FFF)
    a.sh(10, 0x000, 9)          # voice 0 volume L
    a.sh(10, 0x002, 9)          # voice 0 volume R
    a.li(10, 0x1000)
    a.sh(10, 0x004, 9)          # pitch 1.0
    a.li(10, 0x00FF)
    a.sh(10, 0x008, 9)          # ADSR lo
    a.li(10, 0x7F00)
    a.sh(10, 0x00A, 9)          # ADSR hi
    a.li(10, 0x3FFF)
    a.sh(10, 0x180, 9)          # main volume L
    a.sh(10, 0x182, 9)          # main volume R
    a.li(10, 0x0000)
    a.sh(10, 0x1A6, 9)          # transfer address 0
    a.li(10, 0x8000)
    a.sh(10, 0x1AA, 9)          # SPU enable (unmute)

    payload = adpcm_tone()
    for i in range(0, len(payload), 4):
        word = int.from_bytes(payload[i:i + 4].ljust(4, b"\x00"), "little")
        a.li(10, word)
        a.sw(10, 0x1A8, 9)      # SPU transfer FIFO

    a.li(10, 0x0000)
    a.sh(10, 0x006, 9)          # start address
    a.li(10, 0x0001)
    a.sh(10, 0x188, 9)          # key on voice 0

    # ---- visual: 10 bars, height follows a phase counter -----------------
    a.li(9, GP0)                # restore the GP0 pointer (t1 held SPU base)
    a.li(16, 0)                 # phase counter
    a.label("anim")
    wait_vblank(a)
    fill_screen(a, 0x00001020)

    a.li(19, 0)                 # column index
    a.li(20, 30)                # x position
    a.label("bars")
    # height = 30 + ((column*17 + frame) & 63)
    a.li(21, 17)
    a.mult(19, 21)
    a.mflo(21)
    a.addu(21, 21, 16)
    a.andi(21, 21, 63)
    a.addiu(21, 21, 30)         # bar height
    # y = 200 - height
    a.li(22, 200)
    a.subu(22, 22, 21)
    a.sll(22, 22, 16)
    a.or_(22, 22, 20)
    a.li(10, 0x6000C0FF)                # green bar (0xRRGGBB)
    a.sw(10, 0, 9)
    a.sw(22, 0, 9)
    # width 16, height = bar height
    a.addiu(23, 21, -1)
    a.sll(23, 23, 16)
    a.li(24, 0x0000000F)                # width-1 = 15
    a.or_(23, 23, 24)
    a.sw(23, 0, 9)

    a.addiu(19, 19, 1)
    a.addiu(20, 20, 30)
    a.slti(25, 19, 10)
    a.bne(25, 0, "bars")
    a.nop()

    a.addiu(16, 16, 3)
    a.j("anim")
    a.nop()
    return a



# --------------------------------------------------------------------------
#  texture helpers
# --------------------------------------------------------------------------

TEX_X, TEX_Y = 0, 256          # 4 bit texture page (texpage X=0, Y=256)
CLUT_X, CLUT_Y = 0, 480
SCRATCH2 = 0x1F800200


def palette16():
    """16 bright BGR555 colours (index 0 stays transparent)."""
    cols = []
    for i in range(16):
        r = (i * 2) & 31
        g = (i * 7) & 31
        b = ((15 - i) * 2) & 31
        if i >= 8:
            r = 31
        cols.append(r | (g << 5) | (b << 10))
    cols[0] = 0x0000
    cols[1] = 0x7FFF
    return cols


def texture_words(size=64, checker=8):
    """Packs a 64x64 4 bit texture the way GP0 sees it.

    One GP0 word carries eight 4 bit texels (two 16 bit VRAM halfwords), so a
    64 texel row needs eight words and the transfer header says 16 halfwords
    wide by 64 lines.
    """
    texels = []
    for y in range(size):
        for x in range(size):
            if ((x // checker) + (y // checker)) & 1:
                idx = 2 + ((x >> 4) + (y >> 4)) % 13
            else:
                idx = 1 + ((x + y) >> 5)
            texels.append(idx & 0xF)
    words = []
    for i in range(0, len(texels), 8):
        w = 0
        for k in range(8):
            w |= (texels[i + k] & 0xF) << (k * 4)
        words.append(w & 0xFFFFFFFF)
    return words


def upload_texture(a, words, x=TEX_X, y=TEX_Y, halfwords=16, lines=64):
    """GP0 0xA0: CPU -> VRAM transfer of a 4 bit texture.

    The header counts 16 bit cells: a 64 texel row is 16 of them, so the
    payload is one GP0 word per two cells.
    """
    gp0(a, 0xA0000000)
    gp0(a, (x & 0xFFFF) | ((y & 0xFFFF) << 16))
    gp0(a, ((halfwords - 1) & 0xFFFF) | (((lines - 1) & 0xFFFF) << 16))
    for w in words:
        gp0(a, w & 0xFFFFFFFF)


def upload_clut(a, cols, x=CLUT_X, y=CLUT_Y):
    gp0(a, 0xA0000000)
    gp0(a, (x & 0xFFFF) | ((y & 0xFFFF) << 16))
    gp0(a, 15)                       # 16 x 1 pixel: (w-1) | ((h-1) << 16) = 15
    for i in range(0, 16, 2):
        gp0(a, (cols[i] & 0xFFFF) | ((cols[i + 1] & 0xFFFF) << 16))


def clut_word():
    return ((CLUT_X // 16) & 0x3F) << 16 | ((CLUT_Y & 0x1FF) << 22)


def textured_quad(a, p0, p1, p2, p3, u0, v0, u1, v1, u2, v2, u3, v3, color=0xFFFFFF):
    gp0(a, 0x2C000000 | (color & 0xFFFFFF))
    gp0(a, (p0[0] & 0xFFFF) | ((p0[1] & 0xFFFF) << 16))
    gp0(a, (u0 & 0xFF) | ((v0 & 0xFF) << 8) | clut_word())
    gp0(a, (p1[0] & 0xFFFF) | ((p1[1] & 0xFFFF) << 16))
    gp0(a, (u1 & 0xFF) | ((v1 & 0xFF) << 8) | (((TEX_Y // 256) << 4) << 16))
    gp0(a, (p2[0] & 0xFFFF) | ((p2[1] & 0xFFFF) << 16))
    gp0(a, (u2 & 0xFF) | ((v2 & 0xFF) << 8))
    gp0(a, (p3[0] & 0xFFFF) | ((p3[1] & 0xFFFF) << 16))
    gp0(a, (u3 & 0xFF) | ((v3 & 0xFF) << 8))


def read_pad(a, out_lo=6, out_hi=7):
    """Reads a digital pad through SIO0 into two registers."""
    a.li(11, 0x1F801040)
    a.li(10, 0x0001)
    a.sw(10, 0, 11)
    a.li(10, 0x0042)
    a.sw(10, 0, 11)
    a.lw_safe(12, 0, 11)            # 0x41  (id)
    a.li(10, 0)
    a.sw(10, 0, 11)
    a.lw_safe(12, 0, 11)            # 0x5A  (ready)
    a.li(10, 0)
    a.sw(10, 0, 11)
    a.lw_safe(out_lo, 0, 11)        # buttons low  (active low)
    a.li(10, 0)
    a.sw(10, 0, 11)
    a.lw_safe(out_hi, 0, 11)        # buttons high
    a.li(10, 4)
    a.sh(10, 0x0A, 11)              # JOY_CTRL: deselect (16 bit register)


# --------------------------------------------------------------------------
#  demo 5: textured shapes
# --------------------------------------------------------------------------

def demo_tex():
    """4 bit textured quad + a skewed, tinted copy, uploaded by the CPU."""
    a = Asm()
    init_gpu(a, 0x00100818)
    gp0(a, 0xE1000000 | ((TEX_Y // 256) << 4) | 0)
    upload_clut(a, palette16())
    upload_texture(a, texture_words())

    a.label("main")
    wait_vblank(a)
    fill_screen(a, 0x00100818)
    draw_text(a, "4 BIT TEX", 40, 8, 3, 0x00FFFFFF)
    textured_quad(a, (40, 34), (280, 34), (280, 150), (40, 150),
                  0, 0, 64, 0, 64, 64, 0, 64)
    textured_quad(a, (60, 165), (260, 185), (250, 235), (70, 225),
                  0, 0, 32, 0, 32, 32, 0, 32, 0xC0C0FF)
    a.j("main")
    a.nop()
    return a


# --------------------------------------------------------------------------
#  generic primitives (register safe: they only clobber t0/t1/t2)
# --------------------------------------------------------------------------

def rect(a, x, y, w, h, color):
    sprite(a, x, y, w, h, color)


def rect_var(a, x, yreg, w, h, color):
    """Rectangle at a run time Y (in yreg); X may be a constant or a register."""
    a.li(9, GP0)
    a.li(10, 0x60000000 | (color & 0xFFFFFF))
    a.sw(10, 0, 9)
    a.sll(12, yreg, 16)
    if isinstance(x, int):
        a.li(13, x & 0xFFFF)
        a.or_(12, 12, 13)
    else:
        a.or_(12, 12, x[0])
    a.sw(12, 0, 9)
    a.li(12, ((w - 1) & 0xFFFF) | (((h - 1) & 0xFFFF) << 16))
    a.sw(12, 0, 9)


# --------------------------------------------------------------------------
#  demo 6: pad tester
# --------------------------------------------------------------------------

PAD_BUTTONS = [
    ("U",   4,  76,  34), ("D",   6,  76,  76), ("L",   7,  44,  55), ("R",   5, 108,  55),
    ("TRI", 12, 206,  34), ("CIR", 13, 246,  62), ("X",  14, 206,  92), ("SQR", 15, 166,  62),
    ("SEL", 0,  72, 124), ("STA", 3, 140, 124),
]


def demo_pad():
    """Interactive controller test: every pressed button lights up."""
    a = Asm()
    init_gpu(a, 0x00081424)

    a.label("main")
    wait_vblank(a)
    fill_screen(a, 0x00081424)
    read_pad(a, 6, 7)                    # a2 = buttons low, a3 = buttons high

    draw_text(a, "PAD TEST", 44, 12, 4, 0x00FFFFFF)

    for (label, bit, x, y) in PAD_BUTTONS:
        on = "on_" + label
        off = "off_" + label
        done = "done_" + label
        if bit < 8:
            a.li(8, 1 << bit)
            a.and_(8, 6, 8)
        else:
            a.li(8, 1 << (bit - 8))
            a.and_(8, 7, 8)
        a.beq(8, 0, on)               # active low: bit clear == pressed
        a.nop()
        rect(a, x, y, 28, 16, 0x00406060)     # released: grey
        a.j(done)
        a.nop()
        a.label(on)
        rect(a, x, y, 28, 16, 0x0000FF40)     # pressed: green
        a.label(done)
        draw_text(a, label, x + (14 - len(label) * 3), y + 20, 2, 0x00B0C0D0)

    draw_text(a, "PRESS ANY", 60, 170, 3, 0x00FFD000)
    draw_text(a, "BUTTON", 78, 194, 3, 0x00FFD000)
    a.j("main")
    a.nop()
    return a


# --------------------------------------------------------------------------
#  demo 7: pong
# --------------------------------------------------------------------------

def demo_pong():
    """Playable pong: 2/8 move the paddle, first to five points."""
    a = Asm()
    init_gpu(a, 0x00101C2C)       # visible dark blue play field, never black
    a.li(16, 100)                 # s0: player paddle y
    a.li(17, 100)                 # s1: cpu paddle y
    a.li(18, 160)                 # s2: ball x
    a.li(19, 120)                 # s3: ball y
    a.li(20, 3)                   # s4: ball dx
    a.li(21, 2)                   # s5: ball dy
    a.li(22, 0)                   # s6: player score
    a.li(23, 0)                   # s7: cpu score

    a.label("frame")
    wait_vblank(a)
    fill_screen(a, 0x00101C2C)
    read_pad(a, 6, 7)

    # ---- player paddle --------------------------------------------------
    # The pad reports active low: a cleared bit means "pressed", so a set bit
    # jumps over the movement. (This used to be inverted, which made the
    # paddle move while nothing was pressed.)
    a.andi(8, 6, 0x10)            # UP
    a.bne(8, 0, "no_up")
    a.nop()
    a.addiu(16, 16, -10)
    a.label("no_up")
    a.andi(8, 6, 0x40)            # DOWN
    a.bne(8, 0, "no_down")
    a.nop()
    a.addiu(16, 16, 10)
    a.label("no_down")
    a.li(8, 8)
    a.slt(8, 16, 8)
    a.beq(8, 0, "clamp_t")
    a.nop()
    a.li(16, 8)
    a.label("clamp_t")
    a.li(8, 184)
    a.slt(8, 8, 16)
    a.beq(8, 0, "clamp_b")
    a.nop()
    a.li(16, 184)
    a.label("clamp_b")

    # ---- ball motion + walls -------------------------------------------
    a.addu(18, 18, 20)
    a.addu(19, 19, 21)
    a.slti(8, 19, 8)
    a.beq(8, 0, "wall_b")
    a.nop()
    a.subu(21, 0, 21)
    a.li(19, 8)
    a.label("wall_b")
    a.li(8, 230)
    a.slt(8, 8, 19)
    a.beq(8, 0, "wall_t")
    a.nop()
    a.subu(21, 0, 21)
    a.li(19, 230)
    a.label("wall_t")

    # ---- player paddle hit (ball left, paddle at x=24..32) -------------
    a.li(8, 34)
    a.slt(8, 18, 8)
    a.beq(8, 0, "no_hit_p")
    a.nop()
    a.li(8, 4)
    a.slt(8, 20, 8)
    a.beq(8, 0, "no_hit_p")
    a.nop()
    # paddle range [py, py+40]
    a.addiu(8, 16, 40)
    a.slt(8, 19, 8)
    a.beq(8, 0, "no_hit_p")
    a.nop()
    a.slt(8, 19, 16)
    a.bne(8, 0, "no_hit_p")
    a.nop()
    a.li(20, 4)
    a.li(18, 36)
    a.label("no_hit_p")

    # ---- cpu paddle follows the ball -----------------------------------
    a.addiu(8, 17, 20)
    a.slt(8, 8, 19)
    a.beq(8, 0, "cpu_up")
    a.nop()
    a.addiu(17, 17, 3)
    a.j("cpu_done")
    a.nop()
    a.label("cpu_up")
    a.addiu(17, 17, -3)
    a.label("cpu_done")
    a.li(8, 8)
    a.slt(8, 17, 8)
    a.beq(8, 0, "cpu_ct")
    a.nop()
    a.li(17, 8)
    a.label("cpu_ct")
    a.li(8, 184)
    a.slt(8, 8, 17)
    a.beq(8, 0, "cpu_cb")
    a.nop()
    a.li(17, 184)
    a.label("cpu_cb")

    # ---- cpu paddle hit ------------------------------------------------
    a.li(8, 290)
    a.slt(8, 8, 18)
    a.beq(8, 0, "no_hit_c")
    a.nop()
    a.slti(8, 20, 0)
    a.beq(8, 0, "no_hit_c")
    a.nop()
    a.addiu(8, 17, 40)
    a.slt(8, 19, 8)
    a.beq(8, 0, "no_hit_c")
    a.nop()
    a.slt(8, 19, 17)
    a.bne(8, 0, "no_hit_c")
    a.nop()
    a.li(20, -4)
    a.li(18, 286)
    a.label("no_hit_c")

    # ---- goals ---------------------------------------------------------
    a.slti(8, 18, 0)
    a.beq(8, 0, "no_goal_c")
    a.nop()
    a.addiu(23, 23, 1)
    a.li(18, 160)
    a.li(19, 120)
    a.li(20, 3)
    a.label("no_goal_c")
    a.li(8, 312)
    a.slt(8, 8, 18)
    a.beq(8, 0, "no_goal_p")
    a.nop()
    a.addiu(22, 22, 1)
    a.li(18, 160)
    a.li(19, 120)
    a.li(20, -3)
    a.label("no_goal_p")

    # ---- draw ----------------------------------------------------------
    draw_text(a, "PONG", 128, 6, 2, 0x00FFD040)
    rect_var(a, 24, 16, 8, 40, 0x00FFFFFF)
    rect_var(a, 296, 17, 8, 40, 0x00FF8020)
    rect_var(a, (18,), 19, 8, 8, 0x00A0E0FF)
    # dashed centre line (counter in t8: s7 holds the CPU score)
    a.li(24, 0)
    a.label("net")
    a.li(9, GP0)
    a.li(10, 0x60505050)
    a.sw(10, 0, 9)
    a.li(13, 12)                # dash height step
    a.mult(24, 13)
    a.mflo(13)                  # y = index * 12
    a.sll(13, 13, 16)           # move y into the high half of the xy word
    a.li(12, 158)
    a.or_(12, 12, 13)
    a.sw(12, 0, 9)
    a.li(12, 0x0003000A)
    a.sw(12, 0, 9)
    a.addiu(24, 24, 1)
    a.slti(8, 24, 10)
    a.bne(8, 0, "net")
    a.nop()

    # ---- score display -------------------------------------------------
    draw_number(a, 22, 120, 20, 0x00FFFFFF)
    draw_number(a, 23, 180, 20, 0x00FFFFFF)
    a.j("frame")
    a.nop()
    return a


NUM_SEGMENTS = [
    (0, 0, 1, 1), (1, 0, 1, 1), (3, 0, 1, 1), (4, 0, 1, 1),
]


def draw_number(a, reg, x, y, color):
    """Draws the small score digits (0..5) using a 3x5 segment font."""
    for digit in range(6):
        done = "dig_%d_%d" % (x, digit)
        a.li(9, digit)
        a.bne(reg, 9, done)
        a.nop()
        for (cx, cy, w, h) in digit_cells(digit):
            rect(a, x + cx * 4, y + cy * 4, w, h, color)
        a.label(done)


DIGITS = {
    0: ["111", "101", "101", "101", "111"],
    1: ["010", "110", "010", "010", "111"],
    2: ["111", "001", "111", "100", "111"],
    3: ["111", "001", "111", "001", "111"],
    4: ["101", "101", "111", "001", "001"],
    5: ["111", "100", "111", "001", "111"],
}


def digit_cells(d):
    rows = DIGITS[d]
    cells = []
    for ry, row in enumerate(rows):
        for rx, ch in enumerate(row):
            if ch == "1":
                cells.append((rx, ry, 1, 1))
    return cells



DEMOS = [
    ("shapes", "demo_shapes", "GPU: треугольники, спрайты, полупрозрачность", demo_shapes),
    ("logo", "demo_logo", "Текст блочным шрифтом, фон, курсор", demo_logo),
    ("3d", "demo_3d", "GTE: вращающийся каркасный куб", demo_3d),
    ("sound", "demo_sound", "SPU: загрузка ADPCM и воспроизведение", demo_sound),
    ("tex", "demo_tex", "Текстуры: 4-битная загрузка и кварды", demo_tex),
    ("pad", "demo_pad", "Геймпад: интерактивный тест кнопок", demo_pad),
    ("pong", "demo_pong", "Игра: Pong (2/8 - ракетка)", demo_pong),
]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="res/demos")
    ap.add_argument("--only", default=None)
    args = ap.parse_args()
    if not os.path.isdir(args.out):
        os.makedirs(args.out)
    built = 0
    for key, fname, desc, fn in DEMOS:
        if args.only and args.only != key:
            continue
        asm = fn()
        if key == "3d":
            # resolve label addresses, then point face_ptrs[i] at faces_<i>
            asm._pass1()
            idx = asm.items.index(("label", "face_ptrs"))
            for i in range(16):
                asm.items[idx + 1 + i] = ("word", asm.labels["faces_%d" % i])
        code = asm.assemble()
        img = build_disc(code, "PS1J2ME_" + key.upper())
        path = os.path.join(args.out, fname + ".iso")
        with open(path, "wb") as fh:
            fh.write(img)
        print("[demo] %-6s %6d bytes code, %7d bytes iso  (%s)" % (key, len(code), len(img), desc))
        built += 1
    print("[demo] %d image(s) written to %s" % (built, args.out))


if __name__ == "__main__":
    sys.exit(main())
