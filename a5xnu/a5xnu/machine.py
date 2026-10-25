"""
A5 (S5L8940x) machine for Unicorn - the k93ap / iPad 2 board, which is the same
SoC generation as the iPhone 4s.

Why this exists
---------------
Booting iOS on an A5 device needs somebody to have written a machine model for
that SoC. b1ackmarket/qemu-n90ap has one for QEMU (hw/arm/k93ap.c) and got the
XNU kernel as far as the kext-constructor phase; its Layer 2 harness runs real
ARMv7 userland on Unicorn. This module reimplements the *machine* side on
Unicorn so the bring-up can be exercised without building QEMU - which matters
in environments where glib/pixman are unavailable.

Everything hard-coded here is taken from the published reverse-engineering of
that board, not guessed:

  DRAM            PA 0x40000000, 2 GB window (VA 0x80..0xFF -> PA 0x40..0xBF)
  kernel VA       0x80001000      (Mach-O vmaddr IS the VA)
  device tree VA  0x8F000000      (PA 0x4F000000)
  boot_args PA    0x4FF01000      (r0 = this physical address)
  topOfKernelData 0x40DA0000      (boot page tables go here)
  AIC             0x0F200000 len 0x10000   "aic,1" -> AppleInterruptController
  UART0           0x02500000 len 0x100     "uart-1,samsung" (exynos layout), irq 21
  UART3/UART5     0x02800000 / 0x02A00000
  PMGR            0x0F100000 len 0x7000, plus 0x08C/8D/8E/8F/A5/A600000
  WDT             0x0F103020 len 0x10      irq 4
  GPIO            0x0FA00000
  NAND (FMI)      0x01200000 len 0x190000  "fmi,s5l8940x", irq 33/34
  I2C0/1/2        0x03200000/0x03300000/0x03400000  (PMU d1946 on i2c0 @ 0x3C)
  SGX             0x05100000      CLCD 0x0A100000    MIPI 0x09500000

No Apple firmware is included, referenced by URL, or downloaded by this module.
"""

import struct
from typing import Callable, Dict, List, Optional, Tuple

from unicorn import (
    UC_ARCH_ARM,
    UC_HOOK_CODE,
    UC_HOOK_INTR,
    UC_HOOK_MEM_READ_UNMAPPED,
    UC_HOOK_MEM_WRITE_UNMAPPED,
    UC_MODE_ARM,
    UC_PROT_ALL,
    Uc,
    UcError,
)
from unicorn.arm_const import (
    UC_ARM_REG_C1_C0_2,   # CPACR  (mcr p15, 0, rX, c1, c0, 2)
    UC_ARM_REG_FPEXC,
    UC_ARM_REG_LR,
    UC_ARM_REG_PC,
    UC_ARM_REG_R0,
    UC_ARM_REG_R1,
    UC_ARM_REG_R2,
    UC_ARM_REG_R3,
    UC_ARM_REG_R4,
    UC_ARM_REG_R5,
    UC_ARM_REG_R6,
    UC_ARM_REG_R7,
    UC_ARM_REG_R8,
    UC_ARM_REG_R9,
    UC_ARM_REG_R10,
    UC_ARM_REG_R11,
    UC_ARM_REG_R12,
    UC_ARM_REG_SP,
)

# Explicit, not arithmetic: relying on the enum values of R0..R12 being
# consecutive is exactly the kind of assumption that breaks silently.
R0_TO_R12 = [
    UC_ARM_REG_R0, UC_ARM_REG_R1, UC_ARM_REG_R2, UC_ARM_REG_R3,
    UC_ARM_REG_R4, UC_ARM_REG_R5, UC_ARM_REG_R6, UC_ARM_REG_R7,
    UC_ARM_REG_R8, UC_ARM_REG_R9, UC_ARM_REG_R10, UC_ARM_REG_R11,
    UC_ARM_REG_R12,
]

PAGE = 0x1000

# ---------------------------------------------------------------------------
# Board geometry (k93ap / A5)
# ---------------------------------------------------------------------------

RAM_BASE = 0x40000000
UART0_PA = 0x02500000      # "uart-1,samsung" (exynos register layout), irq 21

# An unmodelled instruction raises the same exception on every retry, so a
# wedged run looks like an infinite loop rather than a failure. Stop after this
# many consecutive exceptions at one PC and keep the evidence.
STALL_THRESHOLD = 8
MAX_UNIMPLEMENTED = 4096
RAM_SIZE = 0x80000000          # 2 GB so the whole VA window has backing store
VIRT_OFFSET = 0x40000000       # gVirtBase: va = pa + 0x40000000
KERNEL_VA = 0x80001000
DT_VA = 0x8F000000
DT_PA = 0x4F000000
BOOTARGS_PA = 0x4FF01000
TOPOFKERNELDATA = 0x40DA0000

# MMIO regions that the kernel touches during early boot. Anything not listed
# is logged as an unimplemented access rather than faulting, which is how the
# reverse engineering is done: the access log *is* the evidence.
MMIO_DEVICES: List[Tuple[str, int, int]] = [
    ("aic",      0x0F200000, 0x10000),
    ("pmgr",     0x0F100000, 0x7000),
    ("pmgr2",    0x08C00000, 0x1000),
    ("pmgr3",    0x08D00000, 0x1000),
    ("pmgr4",    0x08E00000, 0x1000),
    ("pmgr5",    0x08F00000, 0x1000),
    ("pmgr6",    0x0A500000, 0x1000),
    ("pmgr7",    0x0A600000, 0x1000),
    ("wdt",      0x0F103020, 0x10),
    ("gpio",     0x0FA00000, 0x1000),
    ("uart0",    UART0_PA,   0x100),
    ("uart3",    0x02800000, 0x100),
    ("uart5",    0x02A00000, 0x100),
    ("i2c0",     0x03200000, 0x1000),
    ("i2c1",     0x03300000, 0x1000),
    ("i2c2",     0x03400000, 0x1000),
    ("fmi",      0x01200000, 0x190000),
    ("spi1",     0x02100000, 0x1000),
    ("sha2",     0x02000000, 0x1000),
    ("pke",      0x03100000, 0x1000),
    ("sgx",      0x05100000, 0x10000),
    ("clcd",     0x0A100000, 0x10000),
    ("mipi",     0x09500000, 0x10000),
    ("armio",    0x3FB00000, 0x200000),
]

# ---------------------------------------------------------------------------
# boot_args (xnu-2107, ARM32)
# ---------------------------------------------------------------------------

BOOTARGS_SIZE = 0x140


def build_boot_args(
    virt_base: int = 0x80000000,
    phys_base: int = 0x80000000,
    mem_size: int = 0x20000000,
    top_of_kernel_data: int = TOPOFKERNELDATA,
    device_tree_p: int = DT_VA,
    device_tree_length: int = 0,
    command_line: str = "-v",
    machine_type: int = 0,
    boot_flags: int = 0,
    mem_size_actual: Optional[int] = None,
    revision: int = 2,
    epoch: int = 0x12,
) -> bytes:
    """
    Lay out boot_args exactly as xnu-2107 expects it.

    +0x000 u16 Revision          = 2
    +0x002 u16 Version/"Epoch"   = 0x12   <- pe_identify_machine validates this.
                                            A mismatch prints "pe_identify_machine:
                                            Epoch Mismatch" and then hangs later,
                                            before the locks are initialised.
    +0x004 u32 virtBase
    +0x008 u32 physBase
    +0x00C u32 memSize
    +0x010 u32 topOfKernelData   (PHYSICAL - the boot page tables go here)
    +0x014 24 bytes Video        (all zero is accepted)
    +0x02C u32 machineType
    +0x030 u32 deviceTreeP       (VIRTUAL)
    +0x034 u32 deviceTreeLength
    +0x038 char CommandLine[256]
    +0x138 u32 bootFlags
    +0x13C u32 memSizeActual
    """
    buf = bytearray(BOOTARGS_SIZE)
    struct.pack_into("<HH", buf, 0x00, revision, epoch)
    struct.pack_into("<IIII", buf, 0x04, virt_base, phys_base, mem_size, top_of_kernel_data)
    # +0x14 .. +0x2B: Video, 24 bytes, left zero
    struct.pack_into("<I", buf, 0x2C, machine_type)
    struct.pack_into("<II", buf, 0x30, device_tree_p, device_tree_length)
    cmd = command_line.encode("utf-8")[:255]
    buf[0x38:0x38 + len(cmd)] = cmd
    struct.pack_into("<I", buf, 0x138, boot_flags)
    struct.pack_into("<I", buf, 0x13C, mem_size if mem_size_actual is None else mem_size_actual)
    return bytes(buf)


def parse_boot_args(data: bytes) -> Dict[str, int]:
    """Inverse of build_boot_args - used by the tests to check the layout."""
    if len(data) < BOOTARGS_SIZE:
        raise ValueError("boot_args too short: %d < %d" % (len(data), BOOTARGS_SIZE))
    rev, epoch = struct.unpack_from("<HH", data, 0x00)
    virt, phys, mem, top = struct.unpack_from("<IIII", data, 0x04)
    mtype, = struct.unpack_from("<I", data, 0x2C)
    dtp, dtlen = struct.unpack_from("<II", data, 0x30)
    cmdline = data[0x38:0x138].split(b"\x00", 1)[0].decode("utf-8", "replace")
    bflags, = struct.unpack_from("<I", data, 0x138)
    memactual, = struct.unpack_from("<I", data, 0x13C)
    return {
        "revision": rev, "epoch": epoch, "virtBase": virt, "physBase": phys,
        "memSize": mem, "topOfKernelData": top, "machineType": mtype,
        "deviceTreeP": dtp, "deviceTreeLength": dtlen, "CommandLine": cmdline,
        "bootFlags": bflags, "memSizeActual": memactual,
    }


# ---------------------------------------------------------------------------
# exynos4210-style UART ("uart-1,samsung" in the A5 device tree)
# ---------------------------------------------------------------------------

class SamsungUart:
    """
    Minimal register model: enough for a kernel console to emit characters.
    ULCON 0x00, UCON 0x04, UFCON 0x08, UTRSTAT 0x10, UTXH 0x20, URXH 0x24,
    UBRDIV 0x28. UTRSTAT bit 1 = transmit buffer empty, bit 2 = transmitter
    empty; both are always set here so a kernel putc loop never blocks.
    """

    def __init__(self, name: str, on_char: Optional[Callable[[str], None]] = None):
        self.name = name
        self.on_char = on_char
        self.tx: List[int] = []
        self.rx: List[int] = []
        self.regs: Dict[int, int] = {}

    def read(self, offset: int) -> int:
        if offset == 0x10:            # UTRSTAT: Tx empty + Tx buffer empty
            return 0x00000006 | (0x01 if self.rx else 0x00)
        if offset == 0x24:            # URXH
            return self.rx.pop(0) if self.rx else 0x00
        return self.regs.get(offset, 0)

    def write(self, offset: int, value: int) -> None:
        if offset == 0x20:            # UTXH
            b = value & 0xFF
            self.tx.append(b)
            if self.on_char:
                self.on_char(chr(b))
            return
        self.regs[offset] = value

    def text(self) -> str:
        return bytes(self.tx).decode("utf-8", "replace")


# ---------------------------------------------------------------------------
# Mach-O loader
# ---------------------------------------------------------------------------

MH_MAGIC = 0xFEEDFACE
MH_MAGIC_64 = 0xFEEDFACF
LC_SEGMENT = 0x01
LC_SEGMENT_64 = 0x19


class MachOLoadError(Exception):
    pass


def load_macho_segments(data: bytes) -> List[Tuple[int, int, bytes, str]]:
    """
    Return [(vmaddr, vmsize, file_bytes, segname)] for every LC_SEGMENT.

    Segments MUST be loaded individually. Copying the whole file to one address
    splashes __LINKEDIT (symtab, string tables) over __bss/__common and destroys
    every zero-initialised global - the device-tree state, the spin locks and
    PE_state all end up garbage, and the symptom is a string like "Sour" being
    dereferenced as a pointer.
    """
    if len(data) < 32:
        raise MachOLoadError("too short to be a Mach-O")
    magic, = struct.unpack_from("<I", data, 0)
    if magic == MH_MAGIC:
        is64 = False
    elif magic == MH_MAGIC_64:
        is64 = True
    else:
        raise MachOLoadError("bad Mach-O magic 0x%08x (byte-swapped or not a kernelcache)" % magic)

    ncmds, = struct.unpack_from("<I", data, 16)
    # mach_header is 28 bytes; mach_header_64 adds a 4-byte reserved field.
    off = 32 if is64 else 28
    out = []
    for _ in range(ncmds):
        if off + 8 > len(data):
            break
        cmd, cmdsize = struct.unpack_from("<II", data, off)
        if cmdsize == 0:
            break
        if cmd in (LC_SEGMENT, LC_SEGMENT_64):
            segname = data[off + 8:off + 24].split(b"\x00", 1)[0].decode("ascii", "replace")
            if cmd == LC_SEGMENT:
                vmaddr, vmsize, fileoff, filesize = struct.unpack_from("<IIII", data, off + 24)
            else:
                vmaddr, vmsize, fileoff, filesize = struct.unpack_from("<QQQQ", data, off + 24)
            chunk = data[fileoff:fileoff + filesize] if filesize else b""
            if vmsize:
                out.append((vmaddr, vmsize, chunk, segname))
        off += cmdsize
    return out


# ---------------------------------------------------------------------------
# The machine
# ---------------------------------------------------------------------------

class A5Machine:
    """
    An A5 / k93ap board on Unicorn.

    The point of this class is to be *testable without firmware*: map_memory(),
    write_boot_args() and the MMIO/exception hooks can all be driven from
    assembled ARM code, so the memory map and the boot protocol can be verified
    against the published kernel entry sequence before any real kernelcache
    exists on disk.
    """

    def __init__(self, ram_size: int = RAM_SIZE, uart_sink: Optional[Callable[[str], None]] = None):
        self.uc = Uc(UC_ARCH_ARM, UC_MODE_ARM)
        self.unimplemented: List[Tuple[str, int, int, int]] = []   # (kind, addr, size, value)
        self.uarts: Dict[int, SamsungUart] = {}
        self.sink = uart_sink or (lambda ch: None)

        # VFP/NEON are OFF by default in Unicorn: without CPACR |= 0xF00000 and
        # FPEXC.EN the first vst1 raises UC_ERR_INSN_INVALID.
        self.uc.reg_write(UC_ARM_REG_C1_C0_2, self.uc.reg_read(UC_ARM_REG_C1_C0_2) | 0xF00000)
        self.uc.reg_write(UC_ARM_REG_FPEXC, 0x40000000)

        # Unicorn exposes almost none of cp15 as readable registers (2.1.4 has
        # CPACR, FPEXC, TPIDRURO/TPIDRURW and little else), and an MCR to an
        # unmodelled system register can abort emulation outright. A bring-up
        # harness therefore traps the instruction instead: every cp15 access is
        # decoded, logged and satisfied from a shadow bank. The log is also the
        # primary evidence when reverse engineering what a boot stage expects.
        self.cp15: Dict[Tuple[int, int, int, int], int] = {}
        self.cp15_log: List[Tuple[str, int, int, int, int, int, int]] = []
        self.instruction_count = 0
        self.unimplemented_total = 0
        self.unimp_devices: Dict[str, int] = {}
        self.stalled: Optional[Tuple[int, int, int]] = None
        self.fault: Optional[UcError] = None
        self._last_exc_pc = -1
        self._last_exc_intno = -1
        self._exc_repeat = 0

        self.map_memory(ram_size)
        self._map_mmio()

        self.uc.hook_add(UC_HOOK_CODE, self._on_code)
        self.uc.hook_add(UC_HOOK_MEM_READ_UNMAPPED, self._on_unmapped, "read")
        self.uc.hook_add(UC_HOOK_MEM_WRITE_UNMAPPED, self._on_unmapped, "write")
        self.uc.hook_add(UC_HOOK_INTR, self._on_intr)

    # -- memory ------------------------------------------------------------

    def map_memory(self, ram_size: int = RAM_SIZE) -> None:
        self.uc.mem_map(RAM_BASE, ram_size, UC_PROT_ALL)

    def _map_mmio(self) -> None:
        for name, base, size in MMIO_DEVICES:
            # Unicorn maps whole pages, but the device tree describes registers
            # with their real extents (UART0 is 0x100 bytes). Round out to pages
            # and keep the register offsets relative to the unrounded base.
            page_base = base & ~0xFFF
            page_size = ((base + size - page_base) + 0xFFF) & ~0xFFF
            # Overlapping or already-backed regions are skipped: several PMGR
            # blocks and the watchdog at 0x0F103020 sit inside a bigger window.
            if self._is_mapped(page_base, page_size):
                continue
            try:
                self.uc.mem_map(page_base, page_size, UC_PROT_ALL)
            except Exception:
                continue
            if name.startswith("uart"):
                uart = SamsungUart(name, self.sink)
                self.uarts[base] = uart
                self._hook_mmio(base, size, uart)
            else:
                self._hook_unimplemented(name, base, size)

    def _is_mapped(self, start: int, size: int) -> bool:
        # mem_regions() is a generator, so materialise it before walking it
        for lo, hi, _perms in list(self.uc.mem_regions()):
            if start >= lo and (start + size - 1) <= hi:
                return True
        return False

    def _hook_mmio(self, base: int, size: int, uart: SamsungUart) -> None:
        from unicorn import UC_HOOK_MEM_READ, UC_HOOK_MEM_WRITE

        def on_read(uc, access, address, sz, value, user_data):
            # Unicorn's UC_HOOK_MEM_READ cannot supply a value: the callback's
            # return is ignored and the CPU loads whatever the backing page
            # holds. The hook does, however, fire BEFORE the load, so publishing
            # the device's register into the mapped page is what makes an MMIO
            # read observable. (Verified empirically: hook-only -> stale value,
            # hook + mem_write -> device value.)
            v = uart.read(address - base) & ((1 << (8 * sz)) - 1)
            try:
                uc.mem_write(address, v.to_bytes(sz, "little"))
            except Exception:
                pass

        def on_write(uc, access, address, sz, value, user_data):
            uart.write(address - base, value)
            return True

        self.uc.hook_add(UC_HOOK_MEM_READ, on_read, begin=base, end=base + size - 1)
        self.uc.hook_add(UC_HOOK_MEM_WRITE, on_write, begin=base, end=base + size - 1)

    def _hook_unimplemented(self, name: str, base: int, size: int) -> None:
        """
        Log every access to a mapped-but-unmodelled peripheral.

        This is the equivalent of QEMU's create_unimplemented_device plus
        "-d unimp", and it is not optional: the block is already backed by a
        mapping, so an unmapped-access hook would never fire and the accesses
        would be silently absorbed by inert RAM. The ordered access log is the
        evidence used to derive a peripheral's register semantics - for the A5
        the first target is the AIC at 0x0F200000, whose behaviour has to be
        recovered from exactly this log.

        Reads are published as zero (deterministic, and what an unmodelled
        device most often looks like) rather than left to whatever the backing
        page happens to hold.
        """
        from unicorn import UC_HOOK_MEM_READ, UC_HOOK_MEM_WRITE

        def on_read(uc, access, address, sz, value, user_data):
            self._log_access(("unimp-read:" + name, address - base, sz, 0))
            self.unimp_devices[name] = self.unimp_devices.get(name, 0) + 1
            try:
                uc.mem_write(address, bytes(sz))
            except Exception:
                pass

        def on_write(uc, access, address, sz, value, user_data):
            self._log_access(("unimp-write:" + name, address - base, sz, value))
            self.unimp_devices[name] = self.unimp_devices.get(name, 0) + 1

        self.uc.hook_add(UC_HOOK_MEM_READ, on_read, begin=base, end=base + size - 1)
        self.uc.hook_add(UC_HOOK_MEM_WRITE, on_write, begin=base, end=base + size - 1)

    # -- access helpers ----------------------------------------------------

    def write(self, addr: int, data: bytes) -> None:
        self.uc.mem_write(addr, data)

    def read(self, addr: int, size: int) -> bytes:
        return bytes(self.uc.mem_read(addr, size))

    def write32(self, addr: int, value: int) -> None:
        self.uc.mem_write(addr, struct.pack("<I", value & 0xFFFFFFFF))

    def read32(self, addr: int) -> int:
        return struct.unpack("<I", bytes(self.uc.mem_read(addr, 4)))[0]

    # -- boot protocol -----------------------------------------------------

    def write_boot_args(self, args: bytes, pa: int = BOOTARGS_PA) -> int:
        self.write(pa, args)
        return pa

    def load_kernel(self, data: bytes, va_base: int = KERNEL_VA) -> Dict[str, object]:
        """Load a kernelcache segment-by-segment. Returns a small report."""
        segs = load_macho_segments(data)
        if not segs:
            raise MachOLoadError("no LC_SEGMENT commands found")
        report = {"segments": [], "entry": None}
        for vmaddr, vmsize, chunk, segname in segs:
            # The Mach-O vmaddr IS the kernel virtual address; translate to the
            # physical backing store so the identity window covers it.
            pa = vmaddr - VIRT_OFFSET if vmaddr >= 0x80000000 else vmaddr
            buf = bytearray(vmsize)
            buf[:len(chunk)] = chunk
            self.write(pa, bytes(buf))
            report["segments"].append({"name": segname, "va": vmaddr, "pa": pa,
                                       "vmsize": vmsize, "filesize": len(chunk)})
        return report

    def set_entry(self, pc: int, boot_args_pa: int = BOOTARGS_PA, sp: Optional[int] = None) -> None:
        """r0 = PHYSICAL address of boot_args, pc = kernel entry."""
        self.uc.reg_write(UC_ARM_REG_R0, boot_args_pa)
        self.uc.reg_write(UC_ARM_REG_PC, pc)
        self.uc.reg_write(UC_ARM_REG_SP, sp if sp is not None else (RAM_BASE + 0x00100000))

    # -- cp15 / system registers -------------------------------------------

    # (opc1, CRn, CRm, opc2) for the registers the early boot path touches.
    SCTLR = (0, 1, 0, 0)
    CPACR_REG = (0, 1, 0, 2)
    TTBR0 = (0, 2, 0, 0)
    TTBR1 = (0, 2, 0, 1)
    TTBCR = (0, 2, 0, 2)
    DACR = (0, 3, 0, 0)
    DFSR = (0, 5, 1, 0)
    IFSR = (0, 5, 1, 1)
    FAR = (0, 6, 0, 0)
    CONTEXTIDR = (0, 13, 0, 1)
    TPIDRURO = (0, 13, 0, 3)

    SYSREG_NAMES = {
        SCTLR: "SCTLR", CPACR_REG: "CPACR", TTBR0: "TTBR0", TTBR1: "TTBR1",
        TTBCR: "TTBCR", DACR: "DACR", DFSR: "DFSR", IFSR: "IFSR", FAR: "FAR",
        CONTEXTIDR: "CONTEXTIDR", TPIDRURO: "TPIDRURO",
    }

    def sysreg(self, key: Tuple[int, int, int, int]) -> int:
        """Last value written to a cp15 register, 0 if never written."""
        return self.cp15.get(key, 0)

    def _decode_coproc(self, insn: int) -> Optional[Tuple[bool, int, int, int, int, int]]:
        """
        Decode MCR/MRC. ARMv7-A A1 encoding:

            31..28 cond | 27..24 1110 | 23..21 opc1 | 20 L | 19..16 CRn
            15..12 Rt   | 11..8  coproc | 7..5 opc2 | 4 1  | 3..0 CRm

        So the fixed bits are 1110 at [27:24] and 1 at [4]; the coprocessor
        number lives in [11:8] and must be tested separately. The mask used to
        be 0x0F000F10 against 0x0E000B10, which demanded coproc == 0xB and
        then demanded coproc == 15 on the next line - an impossible pair, so no
        cp15 instruction was ever trapped. The real MCRs then executed, the
        TTBR0 write raised a prefetch abort (intno 3) and Unicorn retried it
        forever: 7.5 million exceptions in 20 s.

        Verified against keystone encodings: mrc p15,0,r1,c1,c0,0 -> 0xEE111F10,
        mcr p15,0,r6,c2,c0,0 -> 0xEE026F10, both & 0x0F000010 == 0x0E000010 with
        coproc 15; mcr p7,... -> 0xEE011710 is correctly rejected.

        Returns (is_mrc, opc1, CRn, CRm, opc2, Rt) or None.
        """
        if (insn & 0x0F000010) != 0x0E000010:
            return None
        coproc = (insn >> 8) & 0xF
        if coproc != 15:
            return None
        is_mrc = bool(insn & (1 << 20))
        opc1 = (insn >> 21) & 7
        crn = (insn >> 16) & 0xF
        rt = (insn >> 12) & 0xF
        opc2 = (insn >> 5) & 7
        crm = insn & 0xF
        return (is_mrc, opc1, crn, crm, opc2, rt)

    def _on_code(self, uc, address, size, user_data):
        self.instruction_count += 1
        try:
            insn = struct.unpack("<I", bytes(uc.mem_read(address, 4)))[0]
        except Exception:
            return
        dec = self._decode_coproc(insn)
        if dec is None:
            return

        is_mrc, opc1, crn, crm, opc2, rt = dec
        key = (opc1, crn, crm, opc2)
        name = self.SYSREG_NAMES.get(key, "S(%d,%d,%d,%d)" % key)

        if is_mrc:
            value = self.cp15.get(key, 0)
            uc.reg_write(R0_TO_R12[rt], value)
            self.cp15_log.append(("mrc", address, crn, crm, opc1, opc2, rt, value))
        else:
            value = uc.reg_read(R0_TO_R12[rt]) & 0xFFFFFFFF
            self.cp15[key] = value
            self.cp15_log.append(("mcr", address, crn, crm, opc1, opc2, rt, value))
            # Keep Unicorn's own view of CPACR consistent so VFP stays enabled.
            if key == self.CPACR_REG:
                try:
                    uc.reg_write(UC_ARM_REG_C1_C0_2, value)
                except Exception:
                    pass

        # Satisfy the access here and skip the instruction: Unicorn has no MMU
        # in this mode, so letting it execute the real MCR would either be a
        # no-op or abort the run.
        uc.reg_write(UC_ARM_REG_PC, address + 4)

    # -- exceptions --------------------------------------------------------

    def _on_intr(self, uc, intno, user_data):
        # Unicorn reports SVC as an interrupt. On entry PC has already advanced
        # past the SVC - do NOT step it forward again.
        pc = uc.reg_read(UC_ARM_REG_PC)
        self._log_access(("intr", intno, pc, 0))

        # An instruction we do not model raises the same exception every time it
        # is retried, so the run wedges instead of failing: observed as intno 3
        # at one PC, millions of times, while the access list grows without
        # bound. Detect the repeat and stop, leaving the evidence behind.
        if pc == self._last_exc_pc and intno == self._last_exc_intno:
            self._exc_repeat += 1
            if self._exc_repeat >= STALL_THRESHOLD:
                self.stalled = (pc, intno, self._exc_repeat)
                uc.emu_stop()
        else:
            self._last_exc_pc, self._last_exc_intno, self._exc_repeat = pc, intno, 1

    def _log_access(self, entry) -> None:
        """Append to the unimplemented-access log, bounded.

        The log is the primary reverse-engineering evidence (it is how QEMU's
        -d unimp is used), but an unbounded list turns a wedged run into an
        out-of-memory run. Past MAX_UNIMPLEMENTED entries only the counter
        grows, and the first entries - the interesting ones, since they are the
        earliest thing the firmware touched - are kept.
        """
        if len(self.unimplemented) < MAX_UNIMPLEMENTED:
            self.unimplemented.append(entry)
        self.unimplemented_total += 1

    def _on_unmapped(self, uc, access, address, size, value, user_data):
        kind = "read" if user_data == "read" else "write"
        self._log_access((kind, address, size, value))
        # Map a page on demand so the boot continues and the access log stays
        # the evidence, exactly like QEMU's create_unimplemented_device.
        page = address & ~0xFFF
        try:
            uc.mem_map(page, PAGE, UC_PROT_ALL)
        except Exception:
            pass
        return True

    # -- run ---------------------------------------------------------------

    def run(self, begin: Optional[int] = None, until: Optional[int] = None,
            count: int = 0, timeout: int = 0) -> None:
        """
        Execute until `until` is reached, `count` instructions retire, or the
        model hits something it cannot execute.

        Faults never escape as exceptions. They arrive by two different routes
        and a bring-up harness has to survive both:

          * as an INTR hook - a prefetch/data abort on an instruction Unicorn
            can decode but not complete (the real MCR to TTBR0 does this, and
            Unicorn retries it forever unless the cp15 trap intercepts it);
          * as a UcError out of emu_start - an instruction Unicorn cannot
            decode at all, e.g. UDF raises UC_ERR_INSN_INVALID.

        Either way the evidence is recorded in `unimplemented` and `stalled`,
        and the call returns. Exploding would lose the register state that
        explains the fault, which is the only thing worth having at this stage.
        """
        pc = begin if begin is not None else self.uc.reg_read(UC_ARM_REG_PC)
        try:
            self.uc.emu_start(pc, until if until is not None else 0,
                              timeout=timeout, count=count)
        except UcError as e:
            errno = getattr(e, "errno", 0)
            fault_pc = self.uc.reg_read(UC_ARM_REG_PC)
            self.fault = e
            self.stalled = (fault_pc, errno, 1)
            self._log_access(("emu-error", fault_pc, errno, 0))

    @property
    def pc(self) -> int:
        return self.uc.reg_read(UC_ARM_REG_PC)

    @property
    def lr(self) -> int:
        return self.uc.reg_read(UC_ARM_REG_LR)

    def console(self) -> str:
        u0 = self.uarts.get(UART0_PA)
        return u0.text() if u0 else ""
