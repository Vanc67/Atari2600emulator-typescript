#!/usr/bin/env python3
"""
Generate web/testcode.js: small ARM programs for the JavaScript tests,
assembled by keystone so nobody has to hand-encode a machine word.

Hand-encoding is how bugs enter a test suite quietly - the first revision of
tests-js encoded `movw r1, #0x0000` as 0xE30F1000, which is really
`movw r1, #0xF000`, and the test still "passed" for the wrong reason.

    python3 tools/gen_testcode_js.py [--check]
"""

import json
import os
import sys

from jsns import PREAMBLE

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = os.path.join(ROOT, "web", "testcode.js")

PROGRAMS = {
    # Touch the AIC, which is mapped but has no register model: one read and
    # one write, both of which must land in the access log.
    "mmio_log": """
        movw r1, #0x0000
        movt r1, #0x0F20        // 0x0F200000 = AIC
        ldr  r2, [r1, #0x20]
        str  r2, [r1, #0x24]
    """,

    # SCTLR read-modify-write plus TTBR0/TTBR1, then a normal instruction to
    # prove execution continued past the trapped transfers.
    # r6 must hold a TTBR that points at a table mapping this program's own
    # section: the last MCR turns the MMU on, and the instruction after it is
    # fetched through the tables. Writing TTBR0 *after* SCTLR.M -- which is what
    # this listing used to do, copied from the same mistake in start() -- makes
    # the very next fetch translate through TTBR0's reset value and take a
    # prefetch abort.
    "cp15_trap": """
        mcr  p15, 0, r6, c2, c0, 0
        mcr  p15, 0, r6, c2, c0, 1
        mrc  p15, 0, r1, c1, c0, 0
        movw r10, #0x380D
        orr  r1, r1, r10
        mcr  p15, 0, r1, c1, c0, 0
        mov  r0, #1
    """,

    # Write "Darwin\\n" to UART0 UTXH, then read UTRSTAT back through the MMIO
    # read hook - the path that silently returns 0 if the hook cannot publish.
    "uart_console": """
        movw r1, #0x0000
        movt r1, #0x0250        // 0x02500000 = UART0
        movw r2, #0x0044        // 'D'
        str  r2, [r1, #0x20]
        movw r2, #0x0061        // 'a'
        str  r2, [r1, #0x20]
        movw r2, #0x0072        // 'r'
        str  r2, [r1, #0x20]
        movw r2, #0x0077        // 'w'
        str  r2, [r1, #0x20]
        movw r2, #0x0069        // 'i'
        str  r2, [r1, #0x20]
        movw r2, #0x006E        // 'n'
        str  r2, [r1, #0x20]
        movw r2, #0x000A        // newline
        str  r2, [r1, #0x20]
        ldr  r3, [r1, #0x10]    // UTRSTAT must report Tx empty
    """,

    # Thumb smoke test: enough of the 16-bit encodings to prove the decoder is
    # wired to the right handlers, ending in a bx back to ARM.
    # Runs at VA 0x80017C40 immediately after start() hands off, i.e. with
    # SCTLR.M set and TTBR0 pointing at the table start() just built. r0 is
    # inherited and holds boot_args as a VA. Thumb-2 is not implemented, so the
    # 0x80020000 target has to be constructed out of shifts and adds rather than
    # movw/movt.
    #
    # Every access here is a virtual address. The stores land on PA 0x40020000
    # only if the walk through L1[0x800] is correct, and the test reads them back
    # through the *physical* bus, so a wrong translation cannot hide.
    "thumb_mmu_post": """
        movs r4, #2
        lsls r4, r4, #28        // 0x20000000
        lsls r4, r4, #2         // 0x80000000
        movs r5, #1
        lsls r5, r5, #17        // 0x00020000
        adds r4, r4, r5         // r4 = VA 0x80020000
        movs r6, #0xAB
        strb r6, [r4, #0]
        str  r6, [r4, #4]
        ldrb r1, [r0, #2]       // boot_args.Epoch, low byte  (VA 0x8FF01000)
        ldrb r2, [r0, #3]       //                    high byte
        strb r1, [r4, #8]
        strb r2, [r4, #9]
        ldrb r3, [r4, #0]
        ldr  r7, [r4, #4]
    """,

    # Touches VA 0xC0000000, which the boot page table deliberately leaves
    # unmapped (arm_vm_init maps it much later). Must take a data abort.
    "thumb_abort_probe": """
        movs r1, #12
        lsls r1, r1, #28        // VA 0xC0000000
        movs r2, #0
        ldr  r2, [r1, #0]       // unmapped -> data abort, r2 stays 0
        movs r0, #0xEE          // must never execute
    """,

    # Data-abort handler, installed at VA 0xFFFF0010 through the high-vector
    # coarse table start() points L1[0xFFF] at. Records what the architecture
    # promises it will find, then leaves through a mapped VA so the test can
    # inspect the results instead of chasing an abort storm.
    #
    # The scratch area at 0x40030200 is reachable because start() identity-maps
    # the 1 MB section containing its own PC, and 0x40030200 is inside it.
    "abt_handler": """
        movw r5, #0x0200
        movt r5, #0x4003                // PA 0x40030200, identity mapped
        str  r2, [r5, #24]              // r2 at entry: the aborted load's
                                        // destination, which must still be 0.
                                        // Saved first because the MRC below
                                        // reuses r2 for FAR -- r0..r12 are not
                                        // banked between SVC and ABT, so a
                                        // handler that does not save them
                                        // destroys the context it interrupted.
        mrc  p15, 0, r1, c5, c0, 0      // DFSR
        mrc  p15, 0, r2, c6, c0, 0      // FAR
        mrs  r3, spsr                   // CPSR as it was before the abort
        mov  r4, r14                    // LR_abt = faulting instruction + 8
        str  r1, [r5, #0]
        str  r2, [r5, #4]
        str  r3, [r5, #8]
        str  r4, [r5, #12]
        mrs  r6, cpsr                   // now in Abort mode
        str  r6, [r5, #16]
        mov  r7, r13                    // r13_abt, banked separately from r13_svc
        str  r7, [r5, #20]
        movw r8, #0x0200
        movt r8, #0x8002                // VA 0x80020200, the test stop point
        bx   r8
    """,

    "thumb_smoke": """
        movs r0, #7
        lsls r1, r0, #4
        adds r2, r1, #3
        subs r3, r2, r0
        cmp  r3, #4
        mov  r4, r3
        push {r4, lr}
        pop  {r4, pc}
    """,
}


def assemble(src, thumb=False):
    from keystone import KS_ARCH_ARM, KS_MODE_ARM, KS_MODE_THUMB, Ks

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


def build():
    out = {}
    for name, src in PROGRAMS.items():
        thumb = name.startswith("thumb")
        code = assemble(src, thumb=thumb)
        out[name] = {
            "thumb": thumb,
            "bytes": list(code),
            "source": src.strip(),
        }
    return out


def render(out):
    body = []
    for name in sorted(out):
        p = out[name]
        rows = []
        b = p["bytes"]
        for off in range(0, len(b), 16):
            rows.append("        " + ", ".join("0x%02X" % x for x in b[off:off + 16]) + ",")
        body.append(
            "    %s: {\n" % json.dumps(name)
            + "        thumb: %s,\n" % ("true" if p["thumb"] else "false")
            + "        source: %s,\n" % json.dumps(p["source"])
            + "        bytes: new Uint8Array([\n" + "\n".join(rows) + "\n        ])\n"
            + "    },"
        )
    return (
        "// GENERATED by tools/gen_testcode_js.py - do not edit by hand.\n"
        "//\n"
        "// Small ARM/Thumb programs for the JavaScript tests, assembled by keystone\n"
        "// so no machine word is ever hand-encoded in a test.\n\n"
        + PREAMBLE + "\n\n"
        "A5.TESTCODE = {\n" + "\n".join(body) + "\n};\n\n"
        "if (typeof module !== 'undefined' && module.exports) module.exports = A5;\n"
    )


def main():
    check = "--check" in sys.argv
    text = render(build())
    if check:
        old = ""
        if os.path.exists(OUT):
            with open(OUT, encoding="utf-8") as fh:
                old = fh.read()
        if old != text:
            sys.stderr.write("%s is out of date; run tools/gen_testcode_js.py\n" % OUT)
            return 1
        print("%s is up to date" % OUT)
        return 0
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write(text)
    for name, p in sorted(build().items()):
        print("  %-14s %-6s %d bytes" % (name, "thumb" if p["thumb"] else "arm", len(p["bytes"])))
    print("wrote %s" % OUT)
    return 0


if __name__ == "__main__":
    sys.exit(main())
