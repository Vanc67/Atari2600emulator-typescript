"use strict";
// Cross-implementation test: the JavaScript ARMv7 interpreter must reproduce
// exactly the state Unicorn produces for the same byte-identical kernel-entry
// code. The expected values are the ones the Python/Unicorn suite asserts, so
// agreement is a real check on the hand-written core rather than on itself.
//
// Run: node tests-js/test_a5_boot.js

const path = require("path");
require(path.join(__dirname, "..", "web", "arm.js"));       // Bus, ArmCpu
require(path.join(__dirname, "..", "web", "mmu.js"));       // ARMv7 translation
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
    // mcr TTBR0 ; mcr TTBR1 ; mrc SCTLR ; movw ; orr ; mcr SCTLR ; mov r0,#1
    const prog = TC.cp15_trap.bytes;
    m.write(base, prog);
    m.cpu.r[15] = base;
    m.cpu.r[6] = A5.TOPOFKERNELDATA;
    // The program's last MCR switches the MMU on, so the section it is running
    // from has to be in the table before that happens -- the same thing start()
    // does with L1[pc>>20]. Without it the fetch of `mov r0, #1` faults, and the
    // test would report "four cp15 transfers trapped: got 2" instead of saying
    // why.
    const TTB = A5.TOPOFKERNELDATA;
    m.write32((TTB + ((base >>> 20) * 4)) >>> 0, ((base & 0xFFF00000) | A5.SECTION_FLAGS) >>> 0);
    eq("identity section for the program is in L1", m.read32((TTB + ((base >>> 20) * 4)) >>> 0),
       ((base & 0xFFF00000) | A5.SECTION_FLAGS) >>> 0);
    eq("SCTLR shadow is 0 before anything runs", m.cpu.cp15Read(0, 1, 0, 0), 0);
    // without stopAt the run walks off the end of the listing into zero words
    m.run({ count: 100, stopAt: (base + prog.length) >>> 0 });
    const lg = m.cpu.cp15Log;
    eq("four cp15 transfers trapped", lg.length, 4);
    eq("first two are the TTBR writes, before SCTLR.M", lg[0].crn + "," + lg[1].crn, "2,2");
    eq("the MRC is third", lg[2].kind, "mrc");
    eq("the MRC targeted SCTLR S(0,1,0,0)", lg[2].crn * 64 + lg[2].opc2, 1 * 64 + 0);
    eq("SCTLR.M is set by the last MCR", lg[3].kind, "mcr");
    eq("no abort was taken while the MMU came up", m.cpu.exceptionCount, 0);
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

eq("entry code is 52 instructions", ENTRY.ENTRY_CODE.length, 208);
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
eq("instruction count matches Unicorn (45096)", m.cpu.instrCount, 45096);

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


// ------------------------------------------------- MMU translation past handoff
//
// Everything above stops exactly at the handoff, so no instruction is ever
// fetched or executed with SCTLR.M set. This section runs straight through it:
// the Thumb stub lives at PA 0x40017C40 and is reached as VA 0x80017C40, so
// every fetch and every load/store in it has to survive a real table walk.
console.log("\nMMU translation after the handoff");
{
    const m = new A5.A5Machine();
    const post = TC.thumb_mmu_post.bytes;
    const HANDOFF_VA = (ENTRY.HANDOFF & ~1) >>> 0;          // 0x80017C40
    const POST_PA = (HANDOFF_VA - A5.VIRT_OFFSET) >>> 0;    // 0x40017C40

    const pa = m.writeBootArgs(args);
    m.write(ENTRY.ENTRY_PA, ENTRY.ENTRY_CODE);
    m.write(POST_PA, post);
    m.cpu.r[0] = pa; m.cpu.r[14] = ENTRY.HANDOFF; m.cpu.r[15] = ENTRY.ENTRY_PA;
    // stopAt is the END of the stub, not the handoff: execution flows through.
    const reason = m.run({ count: 400000, stopAt: (HANDOFF_VA + post.length) >>> 0 });

    eq("ran through the handoff into the Thumb stub", reason, "handoff");
    eq("stopped at the end of the stub", m.cpu.r[15] >>> 0, (HANDOFF_VA + post.length) >>> 0);
    eq("no exception was taken", m.cpu.exceptionCount, 0);
    check("the MMU actually walked tables", m.cpu.mmu.walks > 0, "walks=" + m.cpu.mmu.walks);

    // The stores were issued to VA 0x80020000; read them back PHYSICALLY. A
    // wrong walk cannot fake this, and neither can the old 2 GB alias window.
    eq("byte store at VA 0x80020000 landed at PA 0x40020000", m.bus.read8(0x40020000), 0xAB);
    eq("word store at VA 0x80020004 landed at PA 0x40020004", m.read32(0x40020004), 0xAB);
    eq("Epoch low byte read from boot_args VA and stored back", m.bus.read8(0x40020008), 0x12);
    eq("Epoch high byte read from boot_args VA and stored back", m.bus.read8(0x40020009), 0x00);
    eq("byte read back through the MMU (r3)", m.cpu.r[3] & 0xFF, 0xAB);
    eq("word read back through the MMU (r7)", m.cpu.r[7] >>> 0, 0xAB);

    // The walk itself, queried directly.
    const t1 = m.translate(0x80020000, "read");
    check("VA 0x80020000 -> PA 0x40020000", t1.ok && t1.pa === 0x40020000, JSON.stringify(t1));
    const t2 = m.translate(0x8FF01000, "read");
    check("boot_args VA 0x8FF01000 -> PA 0x4FF01000", t2.ok && t2.pa === 0x4FF01000, JSON.stringify(t2));
    const t3 = m.translate(HANDOFF_VA, "exec");
    check("fetch VA 0x80017C40 -> PA 0x40017C40", t3.ok && t3.pa === POST_PA, JSON.stringify(t3));

    // VA 0xC0000000 is deliberately NOT in the boot table (arm_vm_init maps it
    // much later). Under the old alias window an access there silently worked.
    const t4 = m.translate(0xC0000000, "read");
    check("VA 0xC0000000 is unmapped by the boot page table", t4.ok === false, JSON.stringify(t4));
    eq("...with a level-1 translation fault", t4.fsr, A5.FSR.TRANS_LEVEL1);

    // Negative control: with SCTLR.M cleared the same VA must NOT translate.
    const saved = m.sysreg(m.SCTLR);
    m.cpu.cp15Write(0, 1, 0, 0, saved & ~1);
    const t5 = m.translate(0x80020000, "read");
    check("with SCTLR.M clear the walk is the identity", t5.ok && t5.pa === 0x80020000,
          JSON.stringify(t5));
    m.cpu.cp15Write(0, 1, 0, 0, saved);
}

// ------------------------------------------------------ data abort + high vectors
//
// Exercises the exception model end to end: an unmapped access publishes DFSR
// and FAR, vectors to 0xFFFF0010 (SCTLR.V is set by start()), switches to Abort
// mode with its own banked r13/r14, saves SPSR, and puts the faulting
// instruction's address + 8 in LR_abt.
console.log("\ndata abort through the high vectors");
{
    const m = new A5.A5Machine();
    const probe = TC.thumb_abort_probe.bytes;
    const handler = TC.abt_handler.bytes;
    const HANDOFF_VA = (ENTRY.HANDOFF & ~1) >>> 0;
    const POST_PA = (HANDOFF_VA - A5.VIRT_OFFSET) >>> 0;
    // The vector page lives inside the 1 MB section start() identity-maps for
    // its own PC. The handler is written at VEC_PA + 0x10 so that its first
    // instruction sits exactly on the data-abort vector: VA 0xFFFF0010 has to
    // land on the handler's entry point, not 16 bytes before it.
    const VEC_PA = 0x40031000;
    const SCRATCH_PA = 0x40030200;          // where the handler writes its findings
    const STOP_VA = 0x80020200;             // where the handler leaves via bx

    const pa = m.writeBootArgs(args);
    m.write(ENTRY.ENTRY_PA, ENTRY.ENTRY_CODE);
    m.write(POST_PA, probe);
    m.write((VEC_PA + 0x10) >>> 0, handler);

    // start() points L1[0xFFF] at a coarse table for VA 0xFFF00000.. but leaves
    // that table zeroed, so VA 0xFFFF0000 faults. Install the small-page entry
    // the handler needs. A coarse table indexes on VA[19:12], which for
    // 0xFFFF0000 is 0xF0 -- not 0xFF; the top nibble of the VA belongs to the
    // L1 index. Descriptor = PA | AP[1:0]=0b01 (privileged RW) | type 0b10.
    const VEC_VA = 0xFFFF0000;
    const L2 = (TTB + 0x9000) >>> 0;
    const l2idx = (VEC_VA >>> 12) & 0xFF;
    const vecDesc = ((VEC_PA & 0xFFFFF000) | 0x12) >>> 0;

    m.cpu.r[0] = pa; m.cpu.r[14] = ENTRY.HANDOFF; m.cpu.r[15] = ENTRY.ENTRY_PA;

    // Stage 1: boot to the handoff and stop there.
    const r0reason = m.run({ count: 400000, stopAt: HANDOFF_VA });
    eq("stage 1 reached the handoff", r0reason, "handoff");

    // The vector descriptor has to go in NOW. start() zeroes 40 KB at TTB, which
    // covers L2 at TTB+0x9000; writing the entry beforehand leaves it zeroed and
    // the first exception taken prefetch-aborts on VA 0xFFFF0010 -- a fault
    // storm that looks exactly like a broken handler.
    m.write32((L2 + l2idx * 4) >>> 0, vecDesc);
    eq("the coarse-table index for VA 0xFFFF0000 is VA[19:12]", l2idx, 0xF0);
    eq("the high-vector L2 entry is installed", m.read32((L2 + l2idx * 4) >>> 0), vecDesc);

    // Stage 2: run the stub, take the abort, land in the handler.
    m.resume({ stopAt: STOP_VA });
    const reason = m.run({ count: 400000 });

    eq("the handler ran and left through a mapped VA", reason, "handoff");
    eq("stopped where the handler branched to", m.cpu.r[15] >>> 0, STOP_VA);
    eq("exactly one data abort", m.cpu.aborts.data, 1);
    eq("no prefetch abort", m.cpu.aborts.prefetch, 0);

    // What the architecture guarantees the handler will find.
    eq("DFSR = level-1 translation fault", m.read32(SCRATCH_PA + 0), A5.FSR.TRANS_LEVEL1);
    eq("FAR = the faulting VA", m.read32(SCRATCH_PA + 4), 0xC0000000);
    // CPSR at the abort: Z set by `movs r2,#0`, N/C cleared by the shifts,
    // Thumb and IRQ/FIQ masks inherited from the boot state, mode SVC.
    eq("SPSR_abt = CPSR as it was in SVC/Thumb", m.read32(SCRATCH_PA + 8), 0x400001F3);
    eq("LR_abt = faulting instruction + 8", m.read32(SCRATCH_PA + 12),
       (HANDOFF_VA + 6 + 8) >>> 0);
    const cpsrAbt = m.read32(SCRATCH_PA + 16) >>> 0;
    eq("the handler ran in Abort mode", cpsrAbt & 0x1F, 0x17);
    check("IRQ was masked on entry", (cpsrAbt & 0x80) !== 0, "CPSR=0x" + cpsrAbt.toString(16));
    check("Thumb was cleared on entry", (cpsrAbt & 0x20) === 0, "CPSR=0x" + cpsrAbt.toString(16));

    // The faulting load must not have written its destination, and the
    // instruction after it must never have executed. r2 is read from the value
    // the handler saved at entry, not from the live register: the handler's own
    // `mrc ... c6` reuses r2 for FAR, and r0..r12 are not banked between SVC and
    // ABT, so the live r2 is the handler's, not the interrupted program's.
    eq("the aborted load did not write its destination", m.read32(SCRATCH_PA + 24), 0);
    eq("FAR really did land in the live r2", m.cpu.r[2] >>> 0, 0xC0000000);
    check("execution never resumed past the faulting load", m.cpu.r[0] !== 0xEE,
          "r0=0x" + (m.cpu.r[0] >>> 0).toString(16));

    // Still in Abort mode, with the SVC bank intact underneath it.
    eq("still in Abort mode afterwards", m.cpu.mode(), 0x17);
    check("r13_abt is banked separately from r13_svc",
          m.cpu.bankAbt[0] !== m.cpu.bankSvc[0] || m.cpu.bankAbt[0] === 0,
          "abt=" + (m.cpu.bankAbt[0] >>> 0).toString(16) +
          " svc=" + (m.cpu.bankSvc[0] >>> 0).toString(16));
    eq("the vector page walk used the coarse table start() installed",
       m.read32((L2 + l2idx * 4) >>> 0), vecDesc);
    const tvec = m.translate((VEC_VA + 0x10) >>> 0, "exec");
    check("VA 0xFFFF0010 -> the handler's first instruction",
          tvec.ok && tvec.pa === ((VEC_PA + 0x10) >>> 0), JSON.stringify(tvec));
}

console.log("\n%d passed, %d failed", pass, fail);
process.exit(fail ? 1 : 0);
