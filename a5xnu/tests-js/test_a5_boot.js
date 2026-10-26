"use strict";
// Cross-implementation test: the JavaScript ARMv7 interpreter must reproduce
// exactly the state Unicorn produces for the same byte-identical kernel-entry
// code. The expected values are the ones the Python/Unicorn suite asserts, so
// agreement is a real check on the hand-written core rather than on itself.
//
// Run: node tests-js/test_a5_boot.js

const path = require("path");
require(path.join(__dirname, "..", "web", "arm.js"));       // Bus, ArmCpu
const A5 = require(path.join(__dirname, "..", "web", "a5.js")); // machine model
const ENTRY = require(path.join(__dirname, "..", "web", "entrycode.js"));
const TC = require(path.join(__dirname, "..", "web", "testcode.js")).TESTCODE;

let pass = 0, fail = 0;
function check(name, cond, detail) {
    if (cond) { pass++; console.log("  [OK ] " + name + (detail ? "   " + detail : "")); }
    else { fail++; console.log("  [BAD] " + name + (detail ? "   " + detail : "")); }
}
function eq(name, got, want) {
    check(name, got === want,
          "got " + (typeof got === "number" ? "0x" + (got >>> 0).toString(16) : got) +
          " want " + (typeof want === "number" ? "0x" + (want >>> 0).toString(16) : want));
}

// ---------------------------------------------------------------- boot_args
console.log("\nboot_args (xnu-2107, ARM32)");
const VIRT_BASE = 0x80000000, PHYS_BASE = 0x40000000, MEM_SIZE = 0x20000000;
const args = A5.buildBootArgs({
    virtBase: VIRT_BASE, physBase: PHYS_BASE, memSize: MEM_SIZE,
    topOfKernelData: A5.TOPOFKERNELDATA,
    deviceTreeP: A5.DT_VA, deviceTreeLength: A5.DT_LEN, commandLine: "-v"
});
eq("size is 0x140", args.length, 0x140);
const parsed = A5.parseBootArgs(args);
eq("revision", parsed.revision, 2);
eq("epoch is 0x12 (pe_identify_machine validates it)", parsed.epoch, 0x12);
eq("virtBase", parsed.virtBase, VIRT_BASE);
eq("physBase", parsed.physBase, PHYS_BASE);
eq("memSize", parsed.memSize, MEM_SIZE);
eq("topOfKernelData", parsed.topOfKernelData, A5.TOPOFKERNELDATA);
eq("deviceTreeP is a VA", parsed.deviceTreeP, A5.DT_VA);
eq("deviceTreeLength", parsed.deviceTreeLength, A5.DT_LEN);
eq("CommandLine", parsed.CommandLine, "-v");
eq("memSizeActual mirrors memSize", parsed.memSizeActual, MEM_SIZE);

// truncation, not overflow
const longCmd = "x".repeat(400);
const p2 = A5.parseBootArgs(A5.buildBootArgs({ commandLine: longCmd }));
eq("over-long CommandLine is truncated at 255", p2.CommandLine.length, 255);
check("boot_args stays 0x140 bytes", A5.buildBootArgs({ commandLine: longCmd }).length === 0x140);

// ---------------------------------------------------------------- UART
console.log("\nSamsungUart");
const u = new A5.SamsungUart("uart0", null);
for (const c of "Darwin\n") u.write(0x20, c.charCodeAt(0));
eq("UTXH bytes reach the console", u.text(), "Darwin\n");
eq("UTRSTAT advertises Tx empty (bits 1|2)", u.read(0x10) & 0x06, 0x06);
u.rx.push(0x41);
// UTRSTAT bit 0 = receive buffer data ready; check it BEFORE draining, since
// reading URXH pops the FIFO and the bit legitimately drops afterwards.
eq("UTRSTAT bit 0 set while rx is pending", u.read(0x10) & 1, 1);
eq("URXH pops the receive FIFO", u.read(0x24), 0x41);
eq("UTRSTAT bit 0 clears once rx is drained", u.read(0x10) & 1, 0);
eq("a drained URXH reads 0 rather than undefined", u.read(0x24), 0);

// ---------------------------------------------------------------- MMIO logging
console.log("\nMMIO access log");
{
    const m = new A5.A5Machine();
    // AIC at 0x0F200000 is mapped but has no register model
    const k = new A5.ArmCpu(m.bus);
    m.cpu = k;
    const base = 0x40200000;
    const prog = TC.mmio_log.bytes;
    m.write(base, prog);
    k.r[15] = base;
    m.run({ count: 100, stopAt: (base + prog.length) >>> 0 });
    const kinds = m.unimplemented.map(e => e.kind);
    check("AIC read is logged", kinds.includes("unimp-read:aic"), kinds.join(","));
    check("AIC write is logged", kinds.includes("unimp-write:aic"));
    const rd = m.unimplemented.find(e => e.kind === "unimp-read:aic");
    const wr = m.unimplemented.find(e => e.kind === "unimp-write:aic");
    eq("logged read offset is relative to the block", rd.addr, 0x20);
    eq("logged write offset is relative to the block", wr.addr, 0x24);
    eq("unmodelled read is served as zero", k.r[2], 0);
    eq("per-device counter", m.unimpDevices["aic"], 2);
}

// ---------------------------------------------------------------- cp15 decode
console.log("\ncp15 trapping");
{
    const m = new A5.A5Machine();
    const base = 0x40300000;
    // mrc p15,0,r1,c1,c0,0 ; mcr p15,0,r1,c1,c0,0 ; mcr p15,0,r6,c2,c0,0 ; mcr p15,0,r6,c2,c0,1
    const prog = TC.cp15_trap.bytes;
    m.write(base, prog);
    m.cpu.r[15] = base;
    m.cpu.r[6] = A5.TOPOFKERNELDATA;
    eq("SCTLR shadow is 0 before anything runs", m.cpu.cp15Read(0, 1, 0, 0), 0);
    // without stopAt the run walks off the end of the listing into zero words
    m.run({ count: 100, stopAt: (base + prog.length) >>> 0 });
    const lg = m.cpu.cp15Log;
    eq("four cp15 transfers trapped", lg.length, 4);
    eq("first is the MRC", lg[0].kind, "mrc");
    eq("the MRC targeted SCTLR S(0,1,0,0)", lg[0].crn * 64 + lg[0].opc2, 1 * 64 + 0);
    // the read-modify-write only lands on 0x380D if the MRC returned 0
    eq("SCTLR after the read-modify-write", m.cpu.cp15Read(0, 1, 0, 0), 0x380D);
    eq("TTBR0 shadow", m.sysreg(m.TTBR0), A5.TOPOFKERNELDATA);
    eq("TTBR1 shadow", m.sysreg(m.TTBR1), A5.TOPOFKERNELDATA);
    eq("execution continued past the traps", m.cpu.r[0], 1);
    eq("halted at the end of the program", m.cpu.stopReason, "handoff");
}


// ---------------------------------------------------------------- UART console
console.log("\nUART0 kernel console");
{
    const m = new A5.A5Machine();
    const prog = TC.uart_console.bytes;
    const base = 0x40400000;
    m.write(base, prog);
    m.cpu.r[15] = base;
    m.run({ count: 200, stopAt: (base + prog.length) >>> 0 });
    eq("seven characters reached the console", m.console(), "Darwin\n");
    eq("UTRSTAT read back through the MMIO hook (r3)", m.cpu.r[3] & 0x06, 0x06);
    eq("uart0 device is registered", Object.keys(m.uarts).length, 3);
    eq("device UTRSTAT reports Tx empty", m.uarts[A5.UART0_PA].read(0x10) & 0x06, 0x06);
}

// ---------------------------------------------------------------- Thumb decoder
console.log("\nThumb decode");
{
    const m = new A5.A5Machine();
    const prog = TC.thumb_smoke.bytes;
    const base = 0x40500000;
    m.write(base, prog);
    // start in Thumb state; POP {pc} returns to LR, which is the stop address
    m.cpu.cpsr |= 0x20;
    m.cpu.r[15] = base | 0;
    m.cpu.r[14] = ((base + prog.length) | 1) | 0;
    const reason = m.run({ count: 100, stopAt: (base + prog.length) >>> 0 });
    eq("thumb program ran to the return address", reason, "handoff");
    eq("movs r0, #7", m.cpu.r[0], 7);
    eq("lsls r1, r0, #4", m.cpu.r[1], 7 << 4);
    eq("adds r2, r1, #3", m.cpu.r[2], (7 << 4) + 3);
    eq("subs r3, r2, r0", m.cpu.r[3], (7 << 4) + 3 - 7);
    eq("mov r4, r3 (high-register form)", m.cpu.r[4], (7 << 4) + 3 - 7);
    eq("push/pop round-tripped through memory", m.cpu.r[13], 0);
    check("still in Thumb state", (m.cpu.cpsr & 0x20) !== 0);
    check("no unimplemented thumb encodings", m.cpu.unimplementedCount === 0,
          "count=" + m.cpu.unimplementedCount);
}

// ---------------------------------------------------------------- the real thing
console.log("\nXNU arm start() on the A5 model");
const m = new A5.A5Machine();
const argsPa = m.writeBootArgs(args);
eq("boot_args landed at PA 0x4FF01000", argsPa, A5.BOOTARGS_PA);

eq("entry code is 50 instructions", ENTRY.ENTRY_CODE.length, 200);
m.write(ENTRY.ENTRY_PA, ENTRY.ENTRY_CODE);
m.cpu.r[0] = argsPa;
m.cpu.r[14] = ENTRY.HANDOFF;
m.cpu.r[15] = ENTRY.ENTRY_PA;

const t0 = process.hrtime.bigint();
const reason = m.run({ count: 200000, stopAt: (ENTRY.HANDOFF & ~1) >>> 0 });
const ms = Number(process.hrtime.bigint() - t0) / 1e6;

console.log("  %d instructions in %s ms, stop reason: %s",
            m.cpu.instrCount, ms.toFixed(1), reason);

eq("stop reason is the handoff", reason, "handoff");
eq("pc reached 0x80017C40", m.cpu.r[15] >>> 0, (ENTRY.HANDOFF & ~1) >>> 0);
eq("instruction count matches Unicorn (45094)", m.cpu.instrCount, 45094);

const TTB = A5.TOPOFKERNELDATA;
const l1 = idx => m.read32((TTB + idx * 4) >>> 0);

eq("r0 = boot_args converted PA -> VA", m.cpu.r[0] >>> 0, 0x8FF01000);
eq("CPSR matches Unicorn exactly", m.cpu.cpsr >>> 0, 0x600001F3);
check("CPSR.T set by bx to a thumb address", (m.cpu.cpsr & 0x20) !== 0);
eq("fill loop took the 512-entry fast path (r5)", m.cpu.r[5], 512);
check("TTB is 16 KB aligned", (TTB & 0x3FFF) === 0, "TTB=0x" + TTB.toString(16));

const first = VIRT_BASE >>> 20;
eq("L1[0x800] = physBase | flags", l1(first), (PHYS_BASE | A5.SECTION_FLAGS) >>> 0);
eq("L1[0x801]", l1(first + 1), ((PHYS_BASE + 0x100000) | A5.SECTION_FLAGS) >>> 0);
eq("L1[0x8FF] (middle of the window)", l1(first + 255),
   ((PHYS_BASE + 255 * 0x100000) | A5.SECTION_FLAGS) >>> 0);
eq("L1[0x9FE]", l1(first + 510), ((PHYS_BASE + 510 * 0x100000) | A5.SECTION_FLAGS) >>> 0);
eq("L1[0x9FF] = last of 512 sections", l1(first + 511),
   ((PHYS_BASE + 511 * 0x100000) | A5.SECTION_FLAGS) >>> 0);
eq("L1[0xA00] still zero (the 40 KB was cleared)", l1(first + 512), 0);
eq("L1[pc>>20] identity-maps the PC section", l1(ENTRY.ENTRY_PA >>> 20),
   ((ENTRY.ENTRY_PA & 0xFFF00000) | A5.SECTION_FLAGS) >>> 0);
eq("L1[0xFFF] -> coarse table for high vectors", l1(0xFFF), ((TTB + 0x9000) | 1) >>> 0);
check("L2 base is 1 KB aligned", ((TTB + 0x9000) & 0x3FF) === 0);

eq("TTBR0", m.sysreg(m.TTBR0), TTB);
eq("TTBR1", m.sysreg(m.TTBR1), TTB);
check("SCTLR has M|C|W|Z|I|V (0x380D)", (m.sysreg(m.SCTLR) & 0x380D) === 0x380D,
      "SCTLR=0x" + m.sysreg(m.SCTLR).toString(16));
const kinds = m.cpu.cp15Log.map(e => e.kind);
eq("three MCRs", kinds.filter(k => k === "mcr").length, 3);
eq("one MRC", kinds.filter(k => k === "mrc").length, 1);
check("no unmodelled instructions were hit", m.cpu.unimplementedCount === 0,
      "count=" + m.cpu.unimplementedCount);

// ---------------------------------------------------------------- the negative case
console.log("\nmemSize = 0x80000000 (signed-negative) overruns the window");
{
    const m2 = new A5.A5Machine();
    const bad = A5.buildBootArgs({ virtBase: VIRT_BASE, physBase: PHYS_BASE,
                                   memSize: 0x80000000, topOfKernelData: A5.TOPOFKERNELDATA });
    const pa2 = m2.writeBootArgs(bad);
    m2.write(ENTRY.ENTRY_PA, ENTRY.ENTRY_CODE);
    m2.cpu.r[0] = pa2; m2.cpu.r[14] = ENTRY.HANDOFF; m2.cpu.r[15] = ENTRY.ENTRY_PA;
    const r2 = m2.run({ count: 300000, stopAt: (ENTRY.HANDOFF & ~1) >>> 0 });
    const l1b = idx => m2.read32((TTB + idx * 4) >>> 0);
    eq("still reaches the handoff", r2, "handoff");
    eq("fast-path counter poisoned to 0", m2.cpu.r[5], 0);
    let mapped = 0;
    for (let i = 0; i < 2048; i++) if (l1b(first + i)) mapped++;
    eq("2048 sections written, not 512", mapped, 2048);
    eq("first entry is still correct", l1b(first), (PHYS_BASE | A5.SECTION_FLAGS) >>> 0);
    eq("last surviving fill entry (L1[0x9FE])", l1b(first + 2046),
       ((PHYS_BASE + 2046 * 0x100000) | A5.SECTION_FLAGS) >>> 0);
    eq("high-vector entry survives because it is written last", l1b(0xFFF), ((TTB + 0x9000) | 1) >>> 0);
}

console.log("\n%d passed, %d failed", pass, fail);
process.exit(fail ? 1 : 0);
