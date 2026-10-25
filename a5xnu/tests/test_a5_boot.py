"""
Verification of the A5 machine model against the published kernel entry
protocol - without any Apple firmware.

The test vector is the documented behaviour of XNU's arm start() on k93ap
(xnu-2107, iOS 6 generation), as reverse engineered in
b1ackmarket/qemu-n90ap ios6-emu/worklog-k93ap.md:

  TTB              = topOfKernelData                 (physical, 16 KB aligned)
                   NOTE: the reference worklog writes "topOfKernelData | 0x4A".
                   That cannot be the L1 base: 0x40DA004A is not 16 KB aligned,
                   an ARMv7 L1 table must be, and TTBR0[13:0] is ignored by
                   hardware anyway. 0x4A is not a section descriptor either -
                   bit 6 is SBZ, TEX lives at bits [14:12], and AP would read 0,
                   i.e. no access at all. It is treated here as a transcription
                   artefact; the flags actually used are 0x40E (see below).
  zero             40 KB at TTB                      (L1 16 KB + L2 24 KB)
  fill loop        for (pa = physBase; memSize -= 1 MB)
                       L1[virtBase>>20 + n] = pa | flags
                   -> VA 0x80000000 + 512 MB -> PA 0x40000000 + 512 MB
                   the compare is SIGNED (cmp r3=1MB, sl=memSize); memSize must
                   stay positive and below 0x80000000 or the kernel drops into
                   the 4 KB L2 slow path, which is wrong
  PC section       L1[pc>>20] = (pc & 0xFFF00000) | flags
  high vectors     L1[0xFFF] = L2 @ (TTB + 0x9000), mapping VA 0xFFFF0000
  TTB0 = TTB1 = TTB; SCTLR |= 0x3800 | 0xD           (M C W Z I V, high vectors)
  args PA -> VA    sub r0, r0, r8 ; add r0, r0, sb
  handoff          bx lr, where lr came from __nl_symbol_ptr as 0x80017C41
                   (thumb bit set -> arm_init prologue)

So the assertions below are not "does my code run" - they are "does my machine
model reproduce what the real kernel does at these addresses".

Run:  python3 -m unittest discover -s tests -v      (from the a5xnu directory)
"""

import os
import struct
import time
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from keystone import KS_ARCH_ARM, KS_MODE_ARM, KS_MODE_THUMB, Ks  # noqa: E402
from unicorn.arm_const import (  # noqa: E402
    UC_ARM_REG_CPSR,
    UC_ARM_REG_LR,
    UC_ARM_REG_PC,
    UC_ARM_REG_R0,
    UC_ARM_REG_R2,
    UC_ARM_REG_R3,
    UC_ARM_REG_R5,
)

from a5xnu.machine import (  # noqa: E402
    A5Machine,
    BOOTARGS_PA,
    BOOTARGS_SIZE,
    DT_VA,
    KERNEL_VA,
    MachOLoadError,
    TOPOFKERNELDATA,
    VIRT_OFFSET,
    build_boot_args,
    load_macho_segments,
    parse_boot_args,
    UART0_PA,
    STALL_THRESHOLD,
    MAX_UNIMPLEMENTED,
)

# Where the stub entry code lives: the worklog gives the real kernel entry as
# VA 0x800860C8 / PA 0x400860C8.
ENTRY_VA = 0x800860C8
ENTRY_PA = ENTRY_VA - VIRT_OFFSET          # 0x400860C8
# __nl_symbol_ptr handoff target, thumb bit set
HANDOFF = 0x80017C41

# 1 MB section descriptor bits, per ARMv7-A short descriptors. Cross-checked
# against Linux arch/arm/include/asm/pgtable-2level-hwdef.h:
#   TYPE_SECT 1<<0 (=0x2), BUFFERABLE 1<<2, CACHEABLE 1<<3, XN 1<<4,
#   AP_WRITE 1<<10, AP_READ 1<<11, TEX(x) x<<12, APX 1<<15, S 1<<16, nG 1<<17
# so 0x40E = section | B | C | AP_WRITE -> write-back cacheable, privileged RW.
SECTION_FLAGS = 0x040E
MEM_SIZE = 0x20000000       # 512 MB of DRAM, per the device tree
VIRT_BASE = 0x80000000
PHYS_BASE = 0x40000000

# The documented start() sequence, assembled for ARMv7 (Cortex-A9 = A5).
# Constants are built with movw/movt so no literal pool is needed.
START_S = """
        ; --- on entry: r0 = boot_args (PA), lr = 0x80017C41 (VA, Thumb bit set) ---
        ;
        ; Register allocation is explicit because the ARM AAPCS aliases bite hard:
        ; r9 == sb, r10 == sl, r11 == fp, r12 == ip.  Reusing r9 for both the TTB
        ; and memSize (as a name-aliased transcription does) silently zeroes
        ; memSize and the fill loop walks off the end of the table.
        ;
        ;   r0  boot_args (PA in, VA out)      r7  L1 index n
        ;   r1  scratch / physBase             r8  section descriptor flags
        ;   r2  scratch / write pointer        r9  memSize remaining
        ;   r3  1 MB step / counters          r10  scratch
        ;   r4  zero value                    r11  virtBase
        ;   r5  zero bound, then entry count  r12  scratch / index
        ;   r6  TTB  (16 KB aligned L1 base)

        ldr     r1, [r0, #0x10]     ; topOfKernelData (PA)
        lsr     r6, r1, #14
        lsl     r6, r6, #14         ; TTB: an ARMv7 L1 table MUST be 16 KB aligned
                                    ; (TTBR0[13:0] is the base field's tail and is
                                    ;  ignored by hardware, so any low bits in the
                                    ;  source value can only misalign the software
                                    ;  zero loop below)

        ; --- zero the 40 KB of page tables at TTB: 16 KB L1 + 24 KB L2 ---
        mov     r2, r6
        mov     r3, #0
        mov     r4, #0
        movw    r5, #0xA000
zloop:  str     r4, [r2], #4
        add     r3, r3, #4
        cmp     r3, r5
        blt     zloop

        ; --- fill 1 MB sections: VA virtBase.. -> PA physBase.. ---
        ldr     r1, [r0, #0x08]     ; physBase
        ldr     r9, [r0, #0x0C]     ; memSize
        ldr     r11, [r0, #0x04]    ; virtBase
        mov     r3, #0x00100000     ; 1 MB
        lsr     r7, r11, #20        ; n = virtBase >> 20  (0x800)
        movw    r8, #0x040E         ; TYPE_SECT|B|C|AP_WRITE: 1 MB section,
                                    ; write-back cacheable, privileged RW
        add     r2, r6, r7, lsl #2  ; &L1[n]
        mov     r5, #0              ; entries written
floop:  orr     r10, r1, r8
        str     r10, [r2], #4
        add     r7, r7, #1
        add     r5, r5, #1
        add     r1, r1, r3
        sub     r9, r9, r3          ; memSize -= 1 MB
        cmp     r3, r9              ; SIGNED: loops while 1 MB <= memSize
        ble     floop

        ; --- the same signed compare decides the 4 KB-page path upstream.  Keep
        ;     the count so a bad memSize is visible instead of silently hanging.
        cmp     r5, #512
        movne   r5, #0              ; poison the count if the fast path was skipped

        ; --- identity-map the 1 MB section containing the PC.  Clearing bits
        ;     [19:0] needs lsr/lsl: #0xFFFFF is not an encodable ARM immediate.
        mov     r1, pc
        lsr     r12, r1, #20        ; section index
        lsl     r10, r12, #20       ; section base == pc & 0xFFF00000
        orr     r10, r10, r8        ; | flags
        str     r10, [r6, r12, lsl #2]

        ; --- high vectors: L1[0xFFF] = (TTB + 0x9000) | 1 -> L2 for VA 0xFFFF0000
        movw    r10, #0x9000
        add     r10, r6, r10        ; L2 base, 1 KB aligned, inside the 40 KB
        orr     r10, r10, #1        ; page-table descriptor: bits[1:0] = 0b01
        movw    r12, #0xFFF
        str     r10, [r6, r12, lsl #2]

        ; --- MMU, caches and high vectors on; both TTBRs point at the same table
        mrc     p15, 0, r1, c1, c0, 0
        movw    r10, #0x380D        ; M | C | W | Z | I | V
        orr     r1, r1, r10
        mcr     p15, 0, r1, c1, c0, 0
        mcr     p15, 0, r6, c2, c0, 0
        mcr     p15, 0, r6, c2, c0, 1

        ; --- boot_args PA -> VA, exactly as start() does it ---
        ldr     r11, [r0, #0x04]    ; virtBase
        ldr     r1,  [r0, #0x08]    ; physBase
        sub     r0, r0, r1
        add     r0, r0, r11

        bx      lr                  ; lr = 0x80017C41 -> Thumb bit ends up SET
"""


def assemble(asm: str, thumb: bool = False, addr: int = 0) -> bytes:
    """
    Assemble an ARM/Thumb listing.

    Keystone's ARM frontend treats ';' as a statement SEPARATOR, not as a
    comment introducer - only '//' starts a comment. A listing annotated with
    ';' therefore parses as "<insn> <random words>", which either raises
    KS_ERR_ASM_MNEMONICFAIL or wedges the two-pass label resolver in an endless
    retry (cost: a test suite that appears to hang for minutes). Both comment
    forms are stripped here so the listings below can stay readable, and an
    empty result is rejected loudly instead of producing a zero-length image.
    """
    clean = []
    for line in asm.splitlines():
        for marker in ("//", ";"):
            cut = line.find(marker)
            if cut != -1:
                line = line[:cut]
        if line.strip():
            clean.append(line)
    if not clean:
        raise AssertionError("assemble() was given no instructions")
    ks = Ks(KS_ARCH_ARM, KS_MODE_THUMB if thumb else KS_MODE_ARM)
    encoding, _ = ks.asm("\n".join(clean), addr)
    return bytes(bytearray(encoding))


class BootArgsTest(unittest.TestCase):
    def test_layout_matches_xnu2107(self):
        args = build_boot_args(
            virt_base=VIRT_BASE, phys_base=PHYS_BASE, mem_size=MEM_SIZE,
            device_tree_length=0x11FD8, command_line="-v rd=md0",
        )
        self.assertEqual(len(args), BOOTARGS_SIZE)
        self.assertEqual(len(args), 0x140)

        # raw offsets, checked independently of parse_boot_args
        self.assertEqual(struct.unpack_from("<H", args, 0x00)[0], 2)
        self.assertEqual(struct.unpack_from("<H", args, 0x02)[0], 0x12)
        self.assertEqual(struct.unpack_from("<I", args, 0x04)[0], VIRT_BASE)
        self.assertEqual(struct.unpack_from("<I", args, 0x08)[0], PHYS_BASE)
        self.assertEqual(struct.unpack_from("<I", args, 0x0C)[0], MEM_SIZE)
        self.assertEqual(struct.unpack_from("<I", args, 0x10)[0], TOPOFKERNELDATA)
        self.assertEqual(args[0x14:0x2C], b"\x00" * 24)          # Video
        self.assertEqual(struct.unpack_from("<I", args, 0x30)[0], DT_VA)
        self.assertEqual(struct.unpack_from("<I", args, 0x34)[0], 0x11FD8)
        self.assertEqual(struct.unpack_from("<I", args, 0x13C)[0], MEM_SIZE)

        p = parse_boot_args(args)
        self.assertEqual(p["CommandLine"], "-v rd=md0")
        self.assertEqual(p["epoch"], 0x12)

    def test_epoch_is_the_field_pe_identify_machine_checks(self):
        """A wrong epoch makes pe_identify_machine hang before locks exist."""
        good = parse_boot_args(build_boot_args())
        self.assertEqual(good["epoch"], 0x12)
        bad = build_boot_args(epoch=0x11)
        self.assertEqual(parse_boot_args(bad)["epoch"], 0x11)

    def test_command_line_is_truncated_not_overflowed(self):
        args = build_boot_args(command_line="x" * 400)
        self.assertEqual(len(args), BOOTARGS_SIZE)
        self.assertEqual(len(parse_boot_args(args)["CommandLine"]), 255)


class StartSequenceTest(unittest.TestCase):
    """The headline experiment: run the documented start() on the A5 model."""

    def setUp(self):
        self.m = A5Machine()
        self.args = build_boot_args(
            virt_base=VIRT_BASE, phys_base=PHYS_BASE, mem_size=MEM_SIZE,
            top_of_kernel_data=TOPOFKERNELDATA, device_tree_length=0x11FD8,
        )
        self.args_pa = self.m.write_boot_args(self.args)
        self.code = assemble(START_S)
        self.m.write(ENTRY_PA, self.code)
        self.m.uc.reg_write(UC_ARM_REG_R0, self.args_pa)
        self.m.uc.reg_write(UC_ARM_REG_LR, HANDOFF)
        self.m.uc.reg_write(UC_ARM_REG_PC, ENTRY_PA)

    def test_stub_assembles_to_armv7(self):
        self.assertGreater(len(self.code), 100)

    def test_kernel_entry_reproduces_the_documented_boot_state(self):
        # stop when the handoff target is reached (bx lr, thumb bit stripped)
        self.m.run(until=HANDOFF & ~1, count=200000)

        self.assertEqual(self.m.pc, HANDOFF & ~1,
                         "entry did not hand off to __nl_symbol_ptr 0x%08x" % HANDOFF)

        ttb = TOPOFKERNELDATA
        l1 = lambda idx: self.m.read32(ttb + idx * 4)   # noqa: E731

        # --- 512 MB window: VA 0x80000000.. -> PA 0x40000000.. -------------
        first = VIRT_BASE >> 20                          # 0x800
        for i in (0, 1, 2, 255, 510, 511):
            got = l1(first + i)
            want = (PHYS_BASE + i * 0x100000) | SECTION_FLAGS
            self.assertEqual(got, want,
                             "L1[0x%x] = 0x%08x, want 0x%08x" % (first + i, got, want))
        # and the entry just past the window must still be zero (40 KB was cleared)
        self.assertEqual(l1(first + 512), 0)

        # --- PC section identity map ---------------------------------------
        # The section base occupies bits [31:20]; masking with 0xFFFF0000 would
        # leak bits [19:16] into SBZ/PAR territory.
        self.assertEqual(l1(ENTRY_PA >> 20),
                         (ENTRY_PA & 0xFFF00000) | SECTION_FLAGS)

        # --- the L1 base really is 16 KB aligned, and L2 1 KB aligned --------
        self.assertEqual(ttb & 0x3FFF, 0, "L1 base must be 16 KB aligned")
        self.assertEqual((ttb + 0x9000) & 0x3FF, 0, "L2 base must be 1 KB aligned")

        # --- the fast path wrote exactly 512 entries (r5, else poisoned to 0)
        self.assertEqual(self.m.uc.reg_read(UC_ARM_REG_R5), 512)

        # --- high vectors -> coarse table at TTB + 0x9000 -------------------
        self.assertEqual(l1(0xFFF), (ttb + 0x9000) | 0x01)

        # --- TTBR0 / TTBR1 / SCTLR -----------------------------------------
        # Unicorn exposes almost none of cp15 as readable registers, so the
        # machine traps MCR/MRC and keeps a shadow bank. That shadow - and the
        # ordered access log - is what we assert against.
        self.assertEqual(self.m.sysreg(A5Machine.TTBR0), ttb)
        self.assertEqual(self.m.sysreg(A5Machine.TTBR1), ttb)

        sctlr = self.m.sysreg(A5Machine.SCTLR)
        # documented: SCTLR |= 0x3800 | 0xD  ->  M(0) C(2) W(3) Z(11) I(12) V(13)
        self.assertEqual(sctlr & 0x380D, 0x380D,
                         "SCTLR = 0x%08x, the 0x380D bits must all be set" % sctlr)
        self.assertTrue(sctlr & 0x0001, "SCTLR.M (MMU enable) not set")
        self.assertTrue(sctlr & 0x2000, "SCTLR.V (high vectors) not set")

        # three MCRs (TTBR0, TTBR1, SCTLR) and one MRC (SCTLR read-modify-write)
        kinds = [e[0] for e in self.m.cp15_log]
        self.assertEqual(kinds.count("mcr"), 3)
        self.assertEqual(kinds.count("mrc"), 1)

        # --- boot_args converted from PA to VA ------------------------------
        expected_va = BOOTARGS_PA - VIRT_OFFSET + VIRT_BASE     # 0x8FF01000
        self.assertEqual(self.m.uc.reg_read(UC_ARM_REG_R0), expected_va)

        # --- thumb bit on the handoff address switched ISA state ------------
        self.assertTrue(self.m.uc.reg_read(UC_ARM_REG_CPSR) & 0x20,
                        "CPSR.T not set after bx to a thumb address")

    def test_signed_compare_is_what_selects_the_1MB_path(self):
        """
        The worklog warns that memSize must stay positive and below
        0x80000000, otherwise the boot mapping is wrong. Reproduce the failure
        side and pin down *how* it is wrong.

        The loop is `sub memSize, memSize, 1MB; cmp 1MB, memSize; ble loop`, so
        the decrement happens BEFORE the signed compare. With memSize =
        0x80000000 (INT_MIN as signed) the first subtract yields 0x7FF00000 -
        a large POSITIVE value - and the loop then runs 2048 times instead of
        exiting early. It writes L1[0x800]..L1[0xFFF], i.e. it maps 2 GB of VA
        starting at virtBase and walks four times past the intended 512 MB
        window. Exactly reaching 0xFFF also means the high-vector descriptor
        would be clobbered if it were written first; it is written after, so it
        survives - order matters here and is asserted below.
        """
        m = A5Machine()
        # memSize = 0x80000000 -> as a SIGNED int32 this is negative
        args = build_boot_args(virt_base=VIRT_BASE, phys_base=PHYS_BASE,
                               mem_size=0x80000000,
                               top_of_kernel_data=TOPOFKERNELDATA)
        pa = m.write_boot_args(args)
        m.write(ENTRY_PA, assemble(START_S))
        m.uc.reg_write(UC_ARM_REG_R0, pa)
        m.uc.reg_write(UC_ARM_REG_LR, HANDOFF)
        m.uc.reg_write(UC_ARM_REG_PC, ENTRY_PA)
        m.run(until=HANDOFF & ~1, count=200000)

        ttb = TOPOFKERNELDATA
        first = VIRT_BASE >> 20
        l1 = lambda idx: m.read32(ttb + idx * 4)   # noqa: E731

        # the fast-path counter was poisoned -> the 512-entry path was not taken
        self.assertEqual(m.uc.reg_read(UC_ARM_REG_R5), 0)

        # 2048 sections written: L1[0x800] .. L1[0xFFF], four times the window
        mapped = sum(1 for i in range(2048) if l1(first + i))
        self.assertEqual(mapped, 2048)
        self.assertGreater(mapped, 512,
                           "a negative memSize must overrun the 512 MB window")
        self.assertEqual(l1(first), PHYS_BASE | SECTION_FLAGS)
        # the last fill entry that survives: first + 2047 is L1[0xFFF], which is
        # the high-vector slot and gets overwritten by the store that follows
        self.assertEqual(l1(first + 2046), (PHYS_BASE + 2046 * 0x100000) | SECTION_FLAGS)
        # the loop stopped on its own: nothing past L1[0xFFF] was touched
        self.assertEqual(m.read32(ttb + 0x1000 * 4), 0)

        # ...and the high-vector entry still holds its descriptor, because it is
        # stored after the fill loop rather than before it
        self.assertEqual(l1(0xFFF), (ttb + 0x9000) | 0x01)


class ConsoleTest(unittest.TestCase):
    def test_uart0_emits_the_kernel_console(self):
        out = []
        m = A5Machine(uart_sink=out.append)
        prog = assemble("""
            movw r1, #0x0000
            movt r1, #0x0250          // r1 = 0x02500000 (UART0)
            movw r2, #0x0044          // 'D'
            str  r2, [r1, #0x20]      // UTXH
            movw r2, #0x0061          // 'a'
            str  r2, [r1, #0x20]
            movw r2, #0x0072          // 'r'
            str  r2, [r1, #0x20]
            movw r2, #0x0077          // 'w'
            str  r2, [r1, #0x20]
            movw r2, #0x0069          // 'i'
            str  r2, [r1, #0x20]
            movw r2, #0x006E          // 'n'
            str  r2, [r1, #0x20]
            movw r2, #0x000A          // newline
            str  r2, [r1, #0x20]
            ldr  r3, [r1, #0x10]      // UTRSTAT must report Tx empty
        """)
        base = 0x40100000
        m.write(base, prog)
        m.uc.reg_write(UC_ARM_REG_PC, base)
        m.run(begin=base, count=200)
        self.assertEqual(m.console(), "Darwin\n")
        self.assertEqual("".join(out), "Darwin\n")
        # UTRSTAT (offset 0x10) bits 1/2 = transmit buffer empty + transmitter
        # empty.  Read it from the device model, not from a CPU register: the
        # point is that the *UART* advertises readiness so a kernel putc loop
        # never blocks.
        self.assertEqual(m.uarts[UART0_PA].read(0x10) & 0x06, 0x06)
        # ...and the CPU must see the same thing through the MMIO read hook.
        # Without the hook the access lands in mapped-but-inert RAM and reads 0,
        # which is exactly how a silent console regression looks.
        self.assertEqual(m.uc.reg_read(UC_ARM_REG_R3) & 0x06, 0x06)


class MachOLoaderTest(unittest.TestCase):
    @staticmethod
    def make_macho(segments):
        """
        segments: [(segname, vmaddr, vmsize, filedata)]

        mach_header_32 is 28 bytes (magic, cputype, cpusubtype, filetype,
        ncmds, sizeofcmds, flags) and segment_command is exactly 56 bytes
        including the fixed 16-byte segname field.
        """
        ncmds = len(segments)
        sizeofcmds = 56 * ncmds
        hdr = struct.pack("<IiiIIII", 0xFEEDFACE, 12, 0, 2, ncmds, sizeofcmds, 0)
        assert len(hdr) == 28
        out = bytearray(hdr)
        fileoff = len(hdr) + sizeofcmds
        for name, va, vms, data in segments:
            nm = name.encode()[:15].ljust(16, b"\x00")
            out += struct.pack("<II", 0x01, 56)          # LC_SEGMENT
            out += nm
            out += struct.pack("<IIII", va, vms, fileoff, len(data))
            out += struct.pack("<IIII", 3, 3, 0, 0)      # maxprot, initprot, nsects, flags
            fileoff += len(data)
        for name, va, vms, data in segments:
            out += data
        return bytes(out)

    def test_segments_are_listed_with_their_vm_addresses(self):
        blob = self.make_macho([
            ("__TEXT", 0x80001000, 0x2000, b"\xAA" * 0x1000),
            ("__DATA", 0x80003000, 0x2000, b"\xBB" * 0x800),
            ("__LINKEDIT", 0x80005000, 0x1000, b"symtab-strings" * 40),
        ])
        segs = load_macho_segments(blob)
        self.assertEqual([s[3] for s in segs], ["__TEXT", "__DATA", "__LINKEDIT"])
        self.assertEqual(segs[0][0], 0x80001000)

    def test_bss_is_not_splashed_by_linkedit(self):
        """
        The documented pitfall: memcpy'ing the whole file to one address throws
        __LINKEDIT over __bss/__common and destroys every zero-initialised
        global. Loading per segment must leave the gap between filesize and
        vmsize zeroed.
        """
        text = b"\xAA" * 0x40                       # filesize 0x40, vmsize 0x2000
        blob = self.make_macho([("__TEXT", 0x80001000, 0x2000, text)])
        m = A5Machine()
        rep = m.load_kernel(blob)
        pa = rep["segments"][0]["pa"]
        self.assertEqual(pa, 0x80001000 - VIRT_OFFSET)
        self.assertEqual(m.read(pa, 0x40), text)
        # everything from filesize up to vmsize must still be zero
        self.assertEqual(m.read(pa + 0x40, 0x2000 - 0x40), b"\x00" * (0x2000 - 0x40))

    def test_rejects_non_macho(self):
        with self.assertRaises(MachOLoadError):
            load_macho_segments(b"not a macho at all, just text" * 4)
        # big-endian/on-disk spelling of the magic reads as 0xCEFAEDFE little
        # endian, which is neither MH_MAGIC nor MH_MAGIC_64
        with self.assertRaises(MachOLoadError):
            load_macho_segments(b"\xFE\xED\xFA\xCE" + b"\x00" * 60)
        with self.assertRaises(MachOLoadError):
            load_macho_segments(b"short")


class UnimplementedDeviceTest(unittest.TestCase):
    def test_touching_an_unmodelled_register_is_logged_not_fatal(self):
        """This log is the evidence used to reverse a peripheral's semantics."""
        m = A5Machine()
        # AIC at 0x0F200000 is mapped but has no register model yet
        prog = assemble("""
            movw r1, #0x0000
            movt r1, #0x0F20        // 0x0F200000 = AIC
            ldr  r2, [r1, #0x20]
            str  r2, [r1, #0x24]
        """)
        base = 0x40200000
        m.write(base, prog)
        # `until` costs nothing measurable (benchmarked: 100k instructions in
        # ~0.35 s either way) but without it the run continues past the program
        # into whatever follows, so PC never lands on the end of the listing.
        m.run(begin=base, until=base + len(prog), count=100)
        self.assertEqual(m.pc, base + len(prog))

        # The block is mapped, so an unmapped-access hook would never fire and
        # these accesses would vanish into inert RAM. They must be logged,
        # because that log is how the register semantics get reverse engineered.
        kinds = [e[0] for e in m.unimplemented]
        self.assertIn("unimp-read:aic", kinds)
        self.assertIn("unimp-write:aic", kinds)
        rd = next(e for e in m.unimplemented if e[0] == "unimp-read:aic")
        wr = next(e for e in m.unimplemented if e[0] == "unimp-write:aic")
        self.assertEqual(rd[1], 0x20)          # AIC register offset, not absolute PA
        self.assertEqual(wr[1], 0x24)
        self.assertEqual(m.unimp_devices.get("aic"), 2)
        # an unmodelled read is served as zero, deterministically
        self.assertEqual(m.uc.reg_read(UC_ARM_REG_R2), 0)


class Cp15DecodeTest(unittest.TestCase):
    """
    The cp15 trap is load-bearing: Unicorn has no readable SCTLR/TTBR0/TTBR1 and
    executing a real MCR to TTBR0 raises a prefetch abort that it then retries
    forever. A wrong mask therefore does not look like a wrong answer, it looks
    like a hung process. These cases are the exact keystone encodings.
    """

    CASES = [
        # (source word, expected match)
        (0xEE111F10, True),    # mrc p15, 0, r1, c1, c0, 0   (SCTLR read)
        (0xEE011F10, True),    # mcr p15, 0, r1, c1, c0, 0   (SCTLR write)
        (0xEE026F10, True),    # mcr p15, 0, r6, c2, c0, 0   (TTBR0)
        (0xEE026F30, True),    # mcr p15, 0, r6, c2, c0, 1   (TTBR1)
        (0xEE011F50, True),    # mcr p15, 0, r1, c1, c0, 2   (TTBCR)
        (0xEE011710, False),   # mcr p7, ...  -> not cp15
        (0xE181A008, False),   # orr r10, r1, r8
        (0xE482A004, False),   # str r10, [r2], #4
        (0xE5901010, False),   # ldr r1, [r0, #0x10]
    ]

    def test_mask_matches_only_cp15_transfers(self):
        m = A5Machine()
        for word, should_match in self.CASES:
            got = m._decode_coproc(word)
            self.assertEqual(got is not None, should_match,
                             "word 0x%08X match=%s, decoded=%s" % (word, should_match, got))

    def test_sctlr_read_modify_write_is_decoded_correctly(self):
        m = A5Machine()
        # mrc p15, 0, r1, c1, c0, 0 -> (is_mrc, opc1, CRn, CRm, opc2, Rt)
        self.assertEqual(m._decode_coproc(0xEE111F10), (True, 0, 1, 0, 0, 1))
        # mcr p15, 0, r6, c2, c0, 1 -> TTBR1
        self.assertEqual(m._decode_coproc(0xEE026F30), (False, 0, 2, 0, 1, 6))

    def test_assembled_listing_encodes_the_same_words(self):
        """Guard against keystone/Unicorn drifting apart on these encodings."""
        for src, want in (("mrc p15, 0, r1, c1, c0, 0", 0xEE111F10),
                          ("mcr p15, 0, r6, c2, c0, 0", 0xEE026F10),
                          ("mcr p15, 0, r6, c2, c0, 1", 0xEE026F30)):
            word = struct.unpack("<I", assemble(src))[0]
            self.assertEqual(word, want, "%s assembled to 0x%08X" % (src, word))


class StallGuardTest(unittest.TestCase):
    """
    An instruction the model does not implement raises the same exception on
    every retry. Without a guard that is an infinite loop plus an unbounded
    access list - observed as 7.5 million exceptions in 20 s. With the guard
    the run stops and leaves the evidence behind.
    """

    def test_undecodable_instruction_is_recorded_not_raised(self):
        """
        Route 1: Unicorn cannot decode the instruction, so the failure surfaces
        as a UcError out of emu_start rather than through the INTR hook.
        """
        m = A5Machine()
        base = 0x40300000
        # UDF #0: cond 0111 1111 imm12 1111 imm4 -> always undefined
        m.write(base, struct.pack("<I", 0xE7F000F0))
        before = time.time()
        m.run(begin=base, count=500000)
        elapsed = time.time() - before

        self.assertIsNotNone(m.fault, "the UcError must be kept, not swallowed silently")
        self.assertIsNotNone(m.stalled)
        self.assertEqual(m.stalled[0], base)
        self.assertLess(elapsed, 10.0, "must fail fast, not retry")
        self.assertTrue(any(e[0] == "emu-error" for e in m.unimplemented))

    def test_repeated_abort_triggers_the_stall_guard(self):
        """
        Route 2: Unicorn decodes the instruction but cannot complete it, so it
        raises the same abort forever. Drive the policy directly - this is what
        the real MCR-to-TTBR0 wedge looked like, at 7.5M exceptions in 20 s.
        """
        m = A5Machine()

        class FakeUc:
            pc = 0x40100010

            def __init__(self):
                self.stopped = False

            def reg_read(self, reg):
                return self.pc

            def emu_stop(self):
                self.stopped = True

        class OtherUc(FakeUc):
            pc = 0x40100099

        fake = FakeUc()
        for _ in range(STALL_THRESHOLD - 1):
            m._on_intr(fake, 3, None)
        self.assertIsNone(m.stalled, "must not give up before the threshold")
        self.assertFalse(fake.stopped)

        m._on_intr(fake, 3, None)
        self.assertIsNotNone(m.stalled)
        self.assertEqual(m.stalled[0], 0x40100010)
        self.assertEqual(m.stalled[1], 3)
        self.assertGreaterEqual(m.stalled[2], STALL_THRESHOLD)
        self.assertTrue(fake.stopped, "the run must actually be stopped")

        # a different PC resets the repeat counter, so unrelated aborts are not
        # conflated with the one that already tripped the guard
        tripped = m.stalled
        other = OtherUc()
        m._on_intr(other, 3, None)
        self.assertEqual(m._exc_repeat, 1)
        self.assertEqual(m.stalled, tripped, "an unrelated PC must not re-trip the guard")
        self.assertFalse(other.stopped)

    def test_unimplemented_log_is_bounded(self):
        m = A5Machine()
        for i in range(MAX_UNIMPLEMENTED + 500):
            m._log_access(("read", 0x1000 + i, 4, 0))
        self.assertEqual(len(m.unimplemented), MAX_UNIMPLEMENTED)
        self.assertEqual(m.unimplemented_total, MAX_UNIMPLEMENTED + 500)
        # the earliest entries - the interesting ones - are what is kept
        self.assertEqual(m.unimplemented[0][1], 0x1000)


class AssemblerCommentTest(unittest.TestCase):
    def test_semicolon_comments_are_stripped_not_parsed_as_separators(self):
        """
        Keystone's ARM frontend treats ';' as a statement separator, not a
        comment. Left in, the trailing words are parsed as mnemonics and the
        listing either fails with KS_ERR_ASM_MNEMONICFAIL or wedges the
        two-pass label resolver - which presented as a test suite that hung for
        minutes while doing nothing.
        """
        with_semi = assemble("mov r0, #1   ; this is a comment")
        with_slash = assemble("mov r0, #1   // this is a comment")
        plain = assemble("mov r0, #1")
        self.assertEqual(with_semi, plain)
        self.assertEqual(with_slash, plain)
        self.assertEqual(len(plain), 4)

    def test_empty_listing_is_rejected_loudly(self):
        with self.assertRaises(AssertionError):
            assemble("; only a comment\n// nothing else")


class MMIOReadTest(unittest.TestCase):
    def test_read_hook_publishes_the_device_value(self):
        """
        UC_HOOK_MEM_READ cannot return a value - the callback's return is
        ignored and the CPU loads whatever the backing page holds. It does fire
        BEFORE the load, so publishing the register into the page is what makes
        MMIO reads observable. Without this, UTRSTAT reads 0 and a kernel putc
        loop spins forever waiting for the transmitter.
        """
        m = A5Machine()
        prog = assemble("""
            movw r1, #0x0000
            movt r1, #0x0250        // UART0
            ldr  r2, [r1, #0x10]    // UTRSTAT
            ldr  r3, [r1, #0x00]    // ULCON: unmodelled, must read 0
        """)
        base = 0x40400000
        m.write(base, prog)
        m.run(begin=base, until=base + len(prog), count=100)
        self.assertEqual(m.pc, base + len(prog))
        self.assertEqual(m.uc.reg_read(UC_ARM_REG_R2) & 0x06, 0x06)
        self.assertEqual(m.uc.reg_read(UC_ARM_REG_R3), 0)


if __name__ == "__main__":
    unittest.main(verbosity=2)
