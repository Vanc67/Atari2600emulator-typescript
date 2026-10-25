#!/usr/bin/env python3
"""
Run the A5 (S5L8940x / k93ap) kernel-entry sequence on Unicorn and print the
resulting boot state.

This is the artefact the test suite asserts on, made visible: a real ARMv7
instruction stream is executed on a model of the Apple A5 board, and what comes
out the other end is compared against what XNU's start() is documented to leave
behind. No Apple firmware is used, needed, fetched or bundled - the instruction
stream is assembled here from the published boot protocol.

    python3 demo_boot.py            # needs the venv described in README.md
"""

import os
import struct
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "tests"))

from keystone import KS_ARCH_ARM, KS_MODE_ARM, Ks            # noqa: E402
from unicorn.arm_const import (                              # noqa: E402
    UC_ARM_REG_CPSR,
    UC_ARM_REG_LR,
    UC_ARM_REG_PC,
    UC_ARM_REG_R0,
    UC_ARM_REG_R5,
)

from a5xnu.machine import (                                  # noqa: E402
    A5Machine,
    BOOTARGS_PA,
    TOPOFKERNELDATA,
    VIRT_OFFSET,
    build_boot_args,
    parse_boot_args,
)

ENTRY_VA = 0x800860C8          # XNU arm start(), VA on k93ap
ENTRY_PA = ENTRY_VA - VIRT_OFFSET
HANDOFF = 0x80017C41           # from __nl_symbol_ptr; thumb bit set
VIRT_BASE = 0x80000000
PHYS_BASE = 0x40000000
MEM_SIZE = 0x20000000          # 512 MB
SECTION_FLAGS = 0x040E         # section | B | C | AP_WRITE

# Re-exported from the test module so the demo and the tests cannot drift apart.
from test_a5_boot import START_S                              # noqa: E402


def assemble(src, thumb=False):
    """Keystone treats ';' as a statement separator, so comments are stripped."""
    clean = []
    for line in src.splitlines():
        for marker in ("//", ";"):
            cut = line.find(marker)
            if cut != -1:
                line = line[:cut]
        if line.strip():
            clean.append(line)
    ks = Ks(KS_ARCH_ARM, KS_MODE_THUMB if thumb else KS_MODE_ARM)
    encoding, _ = ks.asm("\n".join(clean), 0)
    return bytes(bytearray(encoding))


def hr(title):
    print("\n" + title)
    print("-" * len(title))


def main():
    hr("A5 / S5L8940x (k93ap) kernel entry on Unicorn")
    m = A5Machine()
    print("RAM        PA 0x%08X .. 0x%08X  (VA = PA + 0x%08X)"
          % (0x40000000, 0x40000000 + 0x80000000, VIRT_OFFSET))
    print("MMIO       %d device windows mapped, %d with a register model"
          % (len(list(m.uc.mem_regions())), len(m.uarts)))

    hr("boot_args (xnu-2107, ARM32, 0x%X bytes)" % 0x140)
    args = build_boot_args(virt_base=VIRT_BASE, phys_base=PHYS_BASE,
                           mem_size=MEM_SIZE, top_of_kernel_data=TOPOFKERNELDATA,
                           device_tree_p=0x8F000000, device_tree_length=0x11FD8,
                           command_line="-v")
    pa = m.write_boot_args(args)
    parsed = parse_boot_args(args)
    for k in sorted(parsed):
        print("  %-20s %s" % (k, parsed[k]))
    print("  r0 handed to the kernel = PA 0x%08X" % pa)

    hr("assembling the documented start() sequence")
    code = assemble(START_S)
    print("  %d bytes = %d ARMv7 instructions at PA 0x%08X (VA 0x%08X)"
          % (len(code), len(code) // 4, ENTRY_PA, ENTRY_VA))
    m.write(ENTRY_PA, code)
    m.uc.reg_write(UC_ARM_REG_R0, pa)
    m.uc.reg_write(UC_ARM_REG_LR, HANDOFF)
    m.uc.reg_write(UC_ARM_REG_PC, ENTRY_PA)

    hr("executing")
    t = time.time()
    m.run(until=HANDOFF & ~1, count=200000)
    dt = time.time() - t
    print("  %d instructions in %.3f s (%.0f insn/s)"
          % (m.instruction_count, dt, m.instruction_count / dt if dt else 0))
    print("  pc = 0x%08X   (handoff target 0x%08X)" % (m.pc, HANDOFF & ~1))
    print("  lr = 0x%08X" % m.lr)
    print("  stalled = %s   fault = %s" % (m.stalled, m.fault))

    hr("resulting state")
    ttb = TOPOFKERNELDATA
    ok = lambda cond: "OK " if cond else "BAD"   # noqa: E731
    checks = []

    r0 = m.uc.reg_read(UC_ARM_REG_R0)
    checks.append(("r0 = boot_args as VA", r0 == BOOTARGS_PA - VIRT_OFFSET + VIRT_BASE,
                   "0x%08X (want 0x%08X)" % (r0, BOOTARGS_PA - VIRT_OFFSET + VIRT_BASE)))

    cpsr = m.uc.reg_read(UC_ARM_REG_CPSR)
    checks.append(("CPSR.T set (bx to a thumb address)", bool(cpsr & 0x20),
                   "CPSR=0x%08X" % cpsr))

    r5 = m.uc.reg_read(UC_ARM_REG_R5)
    checks.append(("fill loop took the 512-entry fast path", r5 == 512, "r5=%d" % r5))

    checks.append(("TTB is 16 KB aligned", (ttb & 0x3FFF) == 0, "TTB=0x%08X" % ttb))

    # NOTE: `VIRT_BASE >> 20 + n` parses as `VIRT_BASE >> (20 + n)` in Python,
    # which silently reads L1[0] instead of the intended entry. Compute the
    # first index once.
    first = VIRT_BASE >> 20

    e0 = m.read32(ttb + first * 4)
    checks.append(("L1[0x800] = physBase | flags", e0 == PHYS_BASE | SECTION_FLAGS,
                   "0x%08X (want 0x%08X)" % (e0, PHYS_BASE | SECTION_FLAGS)))

    e511 = m.read32(ttb + (first + 511) * 4)
    want511 = (PHYS_BASE + 511 * 0x100000) | SECTION_FLAGS
    checks.append(("L1[0x9FF] = last of 512 sections", e511 == want511,
                   "0x%08X (want 0x%08X)" % (e511, want511)))

    checks.append(("L1[0xA00] still zero (40 KB was cleared)",
                   m.read32(ttb + (first + 512) * 4) == 0, ""))

    epc = m.read32(ttb + (ENTRY_PA >> 20) * 4)
    checks.append(("L1[pc>>20] identity-maps the PC section",
                   epc == (ENTRY_PA & 0xFFF00000) | SECTION_FLAGS,
                   "0x%08X (want 0x%08X)" % (epc, (ENTRY_PA & 0xFFF00000) | SECTION_FLAGS)))

    ehv = m.read32(ttb + 0xFFF * 4)
    checks.append(("L1[0xFFF] -> coarse table for high vectors",
                   ehv == (ttb + 0x9000) | 1, "0x%08X (want 0x%08X)" % (ehv, (ttb + 0x9000) | 1)))

    checks.append(("TTBR0 == TTBR1 == TTB",
                   m.sysreg(A5Machine.TTBR0) == ttb and m.sysreg(A5Machine.TTBR1) == ttb,
                   "TTBR0=0x%08X TTBR1=0x%08X"
                   % (m.sysreg(A5Machine.TTBR0), m.sysreg(A5Machine.TTBR1))))

    sctlr = m.sysreg(A5Machine.SCTLR)
    checks.append(("SCTLR |= M|C|W|Z|I|V (0x380D)", (sctlr & 0x380D) == 0x380D,
                   "SCTLR=0x%08X" % sctlr))

    kinds = [e[0] for e in m.cp15_log]
    checks.append(("cp15 traffic trapped (3 MCR, 1 MRC)",
                   kinds.count("mcr") == 3 and kinds.count("mrc") == 1,
                   " ".join(kinds)))

    for name, passed, detail in checks:
        print("  [%s] %-46s %s" % (ok(passed), name, detail))

    hr("cp15 access log (the shadow bank is the only readable view)")
    for kind, addr, crn, crm, opc1, opc2, rt, value in m.cp15_log:
        key = (opc1, crn, crm, opc2)
        print("  %-4s 0x%08X  S(%d,%d,%d,%d) r%-2d = 0x%08X   %s"
              % (kind, addr, opc1, crn, crm, opc2, rt, value,
                 A5Machine.SYSREG_NAMES.get(key, "")))

    bad = [c for c in checks if not c[1]]
    print("\n%d/%d checks passed" % (len(checks) - len(bad), len(checks)))
    if bad:
        print("FAILED:", ", ".join(c[0] for c in bad))
        return 1
    print("The machine model reproduces the documented XNU start() boot state.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
