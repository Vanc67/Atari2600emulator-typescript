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
require(path.join(__dirname, "..", "web", "aic.js"));       // AIC v1 + private timer
require(path.join(__dirname, "..", "web", "img3.js"));      // IMG3 + complzss
require(path.join(__dirname, "..", "web", "firmware.js"));  // Mach-O segment loading
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


// ------------------------------------------------------- LDRD/STRD and SWP
console.log("\nLDRD / STRD / SWP / SWPB");
{
    const m = new A5.A5Machine();
    const prog = TC.doubleword_swap.bytes;
    const base = 0x40070000;
    m.write(base, prog);
    m.cpu.r[15] = base;
    m.run({ count: 200, stopAt: (base + prog.length) >>> 0 });
    eq("ran to the end", m.cpu.stopReason, "handoff");
    eq("nothing unimplemented was hit", m.cpu.unimplementedCount, 0);
    eq("STRD wrote the low word", m.read32(0x40060010), 0x9ABC1234);
    eq("STRD wrote the high word (Rt2 = Rt+1)", m.read32(0x40060014), 0xDEF05678);
    eq("LDRD restored Rt", m.cpu.r[2] >>> 0, 0x9ABC1234);
    eq("LDRD restored Rt2", m.cpu.r[3] >>> 0, 0xDEF05678);
    eq("SWP loaded the old value into Rt", m.cpu.r[5] >>> 0, 0x0000BEEF);
    eq("SWPB loaded the low byte", m.cpu.r[7] & 0xFF, 0xAA);
    // 0x5555AAAA from SWP, then byte 0 overwritten with 0xFF by SWPB.
    eq("SWP stored Rt2 and SWPB overwrote byte 0", m.read32(0x40060000), 0x5555AAFF);
}

// ------------------------------------------------------------ VFP bring-up
console.log("\nVFP bring-up (CPACR + FPEXC through cp10)");
{
    const m = new A5.A5Machine();
    const prog = TC.vfp_enable.bytes;
    const base = 0x40071000;
    m.write(base, prog);
    m.cpu.r[15] = base;
    m.run({ count: 200, stopAt: (base + prog.length) >>> 0 });
    eq("ran to the end", m.cpu.stopReason, "handoff");
    eq("nothing unimplemented was hit", m.cpu.unimplementedCount, 0);
    eq("CPACR unblocks cp10 and cp11", m.cpu.cp15Read(0, 1, 0, 2) & 0x00F00000, 0x00F00000);
    eq("FPEXC.EN is set in the VFP shadow", m.cpu.vfp["vfp:8,0,0"] >>> 0, 0x40000000);
    eq("vmrs read FPEXC back into r4", m.cpu.r[4] >>> 0, 0x40000000);
    const kinds = m.cpu.vfpLog.map(e => e.kind);
    eq("two VFP system register accesses", kinds.length, 2);
    eq("the write came first", kinds[0], "fmxr");
    eq("then the read", kinds[1], "vmrs");
    eq("both targeted FPEXC at CRn 8", m.cpu.vfpLog[0].crn + "," + m.cpu.vfpLog[1].crn, "8,8");
    eq("the ISB between them was decoded", m.cpu.barrierCount, 1);
}


// ---------------------------------------------------------------- Mach-O loader
console.log("\nMach-O segment loading (synthetic fixtures, no firmware)");
{
    const mk = (n, fill) => { const a = new Uint8Array(n); a.fill(fill); return a; };
    const text = mk(64, 0x11);
    const data = mk(256, 0x22);
    const linkedit = mk(128, 0xEE);
    const img = A5.buildFakeMacho([
        { segname: "__TEXT",     vmaddr: 0x80001000, vmsize: 0x1000, bytes: text },
        { segname: "__DATA",     vmaddr: 0x80002000, vmsize: 0x2000, bytes: data },
        { segname: "__LINKEDIT", vmaddr: 0x80004000, vmsize: 0x1000, bytes: linkedit }
    ]);

    eq("magic is the little-endian 32-bit value", A5.loadMachoSegments(img).length, 3);
    const segs = A5.loadMachoSegments(img);
    eq("segments keep their order", segs.map(s => s.segname).join(","),
       "__TEXT,__DATA,__LINKEDIT");
    eq("__TEXT vmaddr is the kernel VA", segs[0].vmaddr, 0x80001000);
    eq("__DATA vmsize exceeds filesize (the rest is __bss)",
       segs[1].vmsize > segs[1].filesize, true);

    const m = new A5.A5Machine();
    const loaded = A5.loadMacho(m, img, { virtBase: 0x80000000, physBase: 0x40000000 });
    eq("three segments placed", loaded.length, 3);
    eq("__TEXT PA = VA - virtBase + physBase", loaded[0].pa, 0x40001000);
    eq("__DATA PA", loaded[1].pa, 0x40002000);
    eq("__LINKEDIT PA", loaded[2].pa, 0x40004000);
    eq("__TEXT content is in place", m.bus.read8(0x40001000), 0x11);
    eq("__TEXT last byte is in place", m.bus.read8(0x40001000 + 63), 0x11);
    eq("__DATA file-backed part is in place", m.bus.read8(0x40002000 + 255), 0x22);
    // The reason segments must be loaded individually: __DATA declares 0x2000 of
    // VM but carries 0x100 of file. Copying the whole image to one address puts
    // __LINKEDIT's symbol tables over that gap and every zero-initialised global
    // turns into garbage.
    eq("__bss past filesize stayed zero", m.bus.read8(0x40002000 + 256), 0x00);
    eq("__bss last byte stayed zero", m.bus.read8(0x40002000 + 0x1FFF), 0x00);
    eq("__LINKEDIT did not land inside __DATA", m.bus.read8(0x40002000 + 256), 0x00);
    eq("__LINKEDIT is where its own vmaddr says", m.bus.read8(0x40004000), 0xEE);

    // Rejection fixtures.
    function throws(fn, label) {
        try { fn(); return null; }
        catch (e) { return e.name === "MachOLoadError" ? e.message : ("wrong error: " + e); }
    }
    const swapped = img.slice();
    new DataView(swapped.buffer).setUint32(0, A5.MH_CIGAM, true);
    check("byte-swapped magic 0xCEFAEDFE is rejected",
          /byte-swapped/.test(throws(() => A5.loadMachoSegments(swapped)) || ""),
          String(throws(() => A5.loadMachoSegments(swapped))));
    const junk = new Uint8Array(64); junk.fill(0x41);
    check("non-Mach-O bytes are rejected",
          /bad Mach-O magic/.test(throws(() => A5.loadMachoSegments(junk)) || ""),
          String(throws(() => A5.loadMachoSegments(junk))));
    check("a 4-byte file is rejected as too short",
          /too short/.test(throws(() => A5.loadMachoSegments(new Uint8Array(4))) || ""),
          String(throws(() => A5.loadMachoSegments(new Uint8Array(4)))));

    // The 64-bit magic parses too, with the header being four bytes longer.
    const img64 = img.slice();
    new DataView(img64.buffer).setUint32(0, A5.MH_MAGIC_64, true);
    let ok64 = true;
    try { A5.loadMachoSegments(img64); } catch (e) { ok64 = false; }
    check("64-bit magic is accepted without throwing", ok64);
    eq("mach_header_32 is 28 bytes", A5.MACH_HEADER_32_SIZE, 28);
    eq("mach_header_64 is 32 bytes", A5.MACH_HEADER_64_SIZE, 32);
}


// ------------------------------------------------- AIC v1 register semantics
// Offsets are derived, not guessed: Linux's aic_init() case 1 starts at
// TARGET_CPU (0x3000), adds 4 * MAX_IRQ (0x400) to reach SW_SET, then walks
// four 128-byte blocks. The one inferred semantic is that reading AIC_EVENT
// consumes the event, because aic_handle_irq() loops `while (event)`.
console.log("\nAIC v1 register map");
{
    const R = A5.AIC;
    const aic = new A5.Aic();
    eq("SW_SET = TARGET_CPU + 4*MAX_IRQ", R.SW_SET, 0x4000);
    eq("SW_CLR is one 128-byte block later", R.SW_CLR, 0x4080);
    eq("MASK_SET", R.MASK_SET, 0x4100);
    eq("MASK_CLR", R.MASK_CLR, 0x4180);
    eq("HW_STATE", R.HW_STATE, 0x4200);
    eq("the whole map fits k93ap's 0x10000 window", R.HW_STATE + R.BLOCK_BYTES <= 0x10000, true);
    eq("AIC_INFO reports the implemented line count", aic.read(R.INFO, 4), 32);

    aic.write(R.SW_SET, 4, 1 << 5);
    eq("a masked interrupt is not pending", aic.irqPending(), false);
    eq("EVENT reports 0 while masked", aic.read(R.EVENT, 4), 0);
    aic.write(R.MASK_CLR, 4, 1 << 5);
    eq("unmasked: now pending", aic.irqPending(), true);
    eq("EVENT reports type IRQ, number 5", aic.read(R.EVENT, 4), ((R.EVENT_TYPE_IRQ << 16) | 5) >>> 0);
    eq("reading EVENT consumed it", aic.irqPending(), false);
    eq("so the driver's read-until-zero loop terminates", aic.read(R.EVENT, 4), 0);

    aic.write(R.MASK_CLR, 4, (1 << 9) | (1 << 3));
    aic.write(R.SW_SET, 4, (1 << 9) | (1 << 3));
    eq("lowest-numbered pending IRQ first", aic.read(R.EVENT, 4) & 0xFFFF, 3);
    eq("then the next", aic.read(R.EVENT, 4) & 0xFFFF, 9);

    aic.write(R.MASK_CLR, 4, 1 << 7);
    aic.raise(7);
    eq("HW_STATE reflects a raised device line", aic.read(R.HW_STATE, 4) & (1 << 7), (1 << 7) | 0);
    eq("a hardware line is reported as type IRQ too", aic.read(R.EVENT, 4), ((1 << 16) | 7) >>> 0);
    aic.write(R.TARGET_CPU + 7 * 4, 4, 1);
    eq("TARGET_CPU is per-IRQ", aic.read(R.TARGET_CPU + 7 * 4, 4), 1);

    aic.write(R.IPI_SEND, 4, 1);
    eq("IPI_SEND asserts", aic.irqPending(), true);
    eq("an IPI is reported as type 4", (aic.read(R.EVENT, 4) >>> 16) & 0xFF, R.EVENT_TYPE_IPI);
    eq("and consumed by the read", aic.irqPending(), false);

    eq("an offset nobody has reverse-engineered reads as null", aic.read(0x20, 4), null);
    eq("and is not silently accepted for writing", aic.write(0x20, 4, 1), false);
}

// ------------------------------------------- Cortex-A9 private timer semantics
console.log("\nCortex-A9 private timer / watchdog");
{
    const T = A5.PRIVTIMER;
    const aic = new A5.Aic();
    const t = new A5.A9PrivateTimer({ intc: aic, irq: 7 });
    t.write(T.LOAD, 4, 50);
    eq("writing LOAD also loads COUNTER", t.counter, 50);
    eq("the line is not asserted before CONTROL enables it", aic.hw[0] & (1 << 7), 0);
    t.write(T.CONTROL, 4, 0x07);                 // enable | auto-reload | irq
    t.tick(49);
    eq("49 ticks: not expired yet", t.intStatus, 0);
    t.tick(1);
    eq("50 ticks: expired", t.intStatus, 1);
    eq("the AIC line is asserted", aic.hw[0] & (1 << 7), (1 << 7) | 0);
    eq("auto-reload restarted the counter from LOAD", t.counter, 50);
    t.write(T.INT_STATUS, 4, 1);
    eq("write-1-to-clear drops INT_STATUS", t.intStatus, 0);
    eq("and deasserts the AIC line", aic.hw[0] & (1 << 7), 0);

    const t2 = new A5.A9PrivateTimer({ intc: aic, irq: 8 });
    t2.write(T.LOAD, 4, 10);
    t2.write(T.CONTROL, 4, 0x05);                // enable | irq, no reload
    t2.tick(10);
    eq("expired once", t2.expiries, 1);
    t2.write(T.INT_STATUS, 4, 1);
    t2.tick(1000);
    eq("without auto-reload it cannot expire again", t2.expiries, 1);

    t2.write(T.WDOG_LOAD, 4, 4);
    t2.write(T.WDOG_CONTROL, 4, 0x09);           // enable | reset-enable
    t2.tick(4);
    eq("the watchdog expired", t2.wdogExpiries, 1);
    eq("it recorded a reset request rather than resetting", t2.wdogResetRequests, 1);
    t2.write(T.WDOG_CONTROL, 4, 0x01);
    t2.write(T.WDOG_DISABLE, 4, 0x1);
    eq("a stray write does not disable the watchdog", t2.wdogControl & 1, 1);
    t2.write(T.WDOG_DISABLE, 4, 0x12345678);
    eq("the magic sequence does", t2.wdogControl, 0);
}

// ------------------------------------------------------ IRQ taken by the CPU
console.log("\nIRQ delivery: software interrupt through the AIC");
{
    const m = new A5.A5Machine();
    const probe = TC.arm_irq_probe.bytes;
    const handler = TC.arm_irq_handler.bytes;
    const base = 0x40080000, SCRATCH = 0x40060000;
    m.write(base, probe);
    // SCTLR.V is clear on a fresh machine, so the vector base is 0 and the IRQ
    // vector is at 0x18.
    m.write(0x18, handler);
    m.cpu.r[15] = base;
    const reason = m.run({ count: 4000, stopAt: (base + probe.length) >>> 0 });

    eq("the probe ran to completion", reason, "handoff");
    eq("execution resumed after the handler returned", m.cpu.r[0] & 0xFF, 0x5A);
    eq("exactly one IRQ was taken", m.cpu.irqCount, 1);
    eq("no FIQ", m.cpu.fiqCount, 0);
    eq("SPSR_irq holds the interrupted CPSR, in SVC mode",
       m.read32(SCRATCH + 4) & 0x1F, 0x13);
    eq("...with CPSR.I already cleared by the probe", m.read32(SCRATCH + 4) & 0x80, 0);
    eq("LR_irq = interrupted instruction + 4", m.read32(SCRATCH + 8), (base + 0x28 + 4) >>> 0);
    eq("the handler ran in IRQ mode", m.read32(SCRATCH + 12) & 0x1F, 0x12);
    eq("AIC_EVENT named IRQ 5 as type IRQ", m.read32(SCRATCH + 20), ((1 << 16) | 5) >>> 0);
    eq("subs pc, lr, #4 restored CPSR from SPSR_irq", m.cpu.mode(), 0x13);
    eq("and left CPSR.I clear, as it was when interrupted", m.cpu.cpsr & 0x80, 0);
    eq("reading EVENT consumed the software interrupt", m.aic.swSet[0], 0);
    eq("the instruction after the interrupt point then executed",
       m.read32(SCRATCH + 24), 0x20);
}

console.log("\nIRQ delivery: Cortex-A9 private timer");
{
    const m = new A5.A5Machine();
    const probe = TC.arm_timer_probe.bytes;
    const handler = TC.arm_timer_irq_handler.bytes;
    const base = 0x40080000, SCRATCH = 0x40060000;
    const timer = m.attachTimer(0x0F300000, 0x100, 7);
    m.write(base, probe);
    m.write(0x18, handler);
    m.cpu.r[15] = base;
    const reason = m.run({ count: 4000, stopAt: (base + probe.length) >>> 0 });

    eq("the probe ran to completion", reason, "handoff");
    eq("LOAD reached the timer through MMIO", timer.load, 20);
    eq("CONTROL = enable | irq-enable", timer.control, 0x05);
    eq("the timer expired exactly once", timer.expiries, 1);
    eq("exactly one IRQ was taken", m.cpu.irqCount, 1);
    eq("AIC_EVENT named IRQ 7", m.read32(SCRATCH + 20), ((1 << 16) | 7) >>> 0);
    eq("the handler cleared INT_STATUS", timer.intStatus, 0);
    eq("so the AIC line is deasserted", m.aic.hw[0] & (1 << 7), 0);
    eq("execution resumed after the handler", m.cpu.r[8] & 0xFF, 0xA5);
    eq("back in SVC mode", m.cpu.mode(), 0x13);
}


// ----------------------------------------------------------------- IMG3 image
// The layout is unambiguous: a 20-byte header then a chain of 12-byte-header
// tags. Every size is cross-checked against the buffer, so a truncated or
// self-inconsistent file is rejected instead of being read past its end. No key
// material is used anywhere here -- a KBAG is reported, never acted on.
console.log("\nIMG3 container");
{
    const I = A5.IMG3;
    eq("IMG3 magic on disk reads '3gmI'", A5.Img3.fourcc(I.MAGIC), "3gmI");
    eq("the header is 20 bytes", I.HEADER_SIZE, 20);
    eq("a tag header is 12 bytes", I.TAG_HEADER_SIZE, 12);

    const payload = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8]);
    const img = A5.Img3.build({ imgType: "krnl", tags: [
        { magic: "DATA", data: payload },
        { magic: "SHSH", data: new Uint8Array([0xAA, 0xBB]) },
        { magic: "CERT", data: new Uint8Array([9]) }
    ]});
    const p = A5.Img3.parse(img);
    eq("a built image parses", p.ok, true);
    eq("imgType survives", p.imgTypeName, "krnl");
    eq("three tags", p.tags.length, 3);
    eq("tag names in order", p.tags.map(function (t) { return t.name; }).join(","),
       "DATA,SHSH,CERT");
    eq("the DATA payload survives byte for byte",
       Array.from(p.data).join(","), Array.from(payload).join(","));
    eq("totalSize covers the header and every tag",
       p.totalSize, 20 + (12 + 8) + (12 + 2) + (12 + 1));
    eq("shshOffset is measured from the start of the header",
       p.shshOffset, 12 + 8);
    eq("an image without a KBAG is not encrypted", p.encrypted, false);

    const enc = A5.Img3.build({ imgType: "illb", tags: [
        { magic: "DATA", data: new Uint8Array(4) },
        { magic: "KBAG", data: new Uint8Array(4 + 16 + 16) }
    ]});
    eq("a KBAG marks the image encrypted", A5.Img3.parse(enc).encrypted, true);
    eq("and the KBAG is found", A5.Img3.parse(enc).kbagTags.length, 1);

    function corrupt(fn) {
        const b = new Uint8Array(img);
        fn(b);
        return A5.Img3.parse(b);
    }
    const swapped = new Uint8Array(img);
    swapped[0] = "I".charCodeAt(0); swapped[1] = "m".charCodeAt(0);
    swapped[2] = "g".charCodeAt(0); swapped[3] = "3".charCodeAt(0);
    const sp = A5.Img3.parse(swapped);
    eq("a byte-swapped magic is named as such", sp.ok, false);
    eq("...and says why", sp.error.indexOf("byte-swapped") >= 0, true);
    eq("a truncated header is rejected", A5.Img3.parse(img.slice(0, 12)).ok, false);
    eq("a totalSize beyond the buffer is rejected",
       corrupt(function (b) { b[4] = 0xFF; b[5] = 0xFF; }).ok, false);
    eq("a dataSize larger than the tag area is rejected",
       corrupt(function (b) { b[8] = 0xFF; b[9] = 0x00; }).ok, false);
    // The tag header begins at byte 20: magic 20..23, totalSize 24..27,
    // dataSize 28..31. Corrupting byte 20 renames the tag rather than breaking
    // its size, which parses fine and says nothing about the check under test.
    eq("a tag whose totalSize is below its own header is rejected",
       corrupt(function (b) { b[24] = 4; b[25] = 0; b[26] = 0; b[27] = 0; }).ok, false);
    eq("a tag that runs past the end of the image is rejected",
       corrupt(function (b) { b[24] = 0xFF; b[25] = 0; b[26] = 0; b[27] = 0; }).ok, false);
    eq("a tag whose dataSize exceeds its payload is rejected",
       corrupt(function (b) { b[28] = 0x40; }).ok, false);
}

// --------------------------------------------------------------- complzss
// The header and the ring parameters are exact. The two-byte match encoding is
// not: implementations disagree on byte order and on whether the position field
// is an absolute ring index or a distance back. Both conventions are therefore
// implemented and both are round-tripped, and neither is claimed to match
// Apple's encoder -- that would need a real compressed image, which is not
// obtainable here.
console.log("\ncomplzss container and LZSS codec");
{
    const C = A5.COMPLZSS, K = A5.Complzss;
    function same(a, b) {
        if (a.length !== b.length) return false;
        for (let i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
        return true;
    }
    eq("the header is 0x180 bytes", C.HEADER_SIZE, 0x180);
    eq("the ring is 4096 bytes", C.N, 4096);
    eq("F is 18", C.F, 18);
    eq("THRESHOLD is 2", C.THRESHOLD, 2);
    eq("the documented dictptr 4078 is exactly N - F", C.DICT_START, C.N - C.F);
    eq("the ring is filled with spaces", C.FILL, 0x20);

    const hdr = K.parseHeader(K.buildHeader(1234, 567, 0xDEADBEEF));
    eq("header magic", hdr.ok && hdr.magic, "complzss");
    eq("header round-trips uncompressedSize", hdr.uncompressedSize, 1234);
    eq("header round-trips compressedSize", hdr.compressedSize, 567);
    eq("header round-trips the checksum", hdr.checksum, 0xDEADBEEF);
    eq("a zero-filled buffer of header size has the wrong magic",
       K.parseHeader(new Uint8Array(C.HEADER_SIZE)).ok, false);
    eq("a file shorter than the header is rejected",
       K.parseHeader(new Uint8Array(16)).ok, false);

    const text = new TextEncoder().encode(
        "Darwin Kernel Version 13.0.0 Darwin Kernel Version 13.0.0 aaabbbccc");
    const rep = new Uint8Array(3000); rep.fill(0x5A);
    const wrap = new Uint8Array(9000);
    for (let i = 0; i < wrap.length; i++) wrap[i] = i % 97;
    const prng = new Uint8Array(4096);
    let st = 12345;
    for (let i = 0; i < prng.length; i++) { st = (st * 1103515245 + 12345) >>> 0; prng[i] = (st >>> 16) & 0xFF; }
    const cases = [
        ["one byte", new Uint8Array([0x41])],
        ["highly repetitive", rep],
        ["text with spaces and repeats", text],
        ["pseudo-random", prng],
        ["crossing the ring wrap", wrap]
    ];

    for (const opts of [{}, { absolutePosition: true }]) {
        const label = opts.absolutePosition ? "absolute position" : "distance back";
        for (const [name, src] of cases) {
            const packed = K.pack(src, opts);
            eq(label + ": " + name + " header is valid", K.parseHeader(packed).ok, true);
            eq(label + ": " + name + " header records the real size",
               K.parseHeader(packed).uncompressedSize, src.length);
            const un = K.unpack(packed, opts);
            eq(label + ": " + name + " round-trips exactly", un.ok && same(un.data, src), true);
        }
        // The regression that the bounded search fixes: text containing ".0 "
        // used to find a 3-byte match two bytes back whose third byte was an
        // unwritten space slot, and the decoder -- which had by then written the
        // real byte there -- returned ".0." instead.
        eq(label + ": the space padding is not matched against",
           same(K.unpack(K.pack(text, opts), opts).data, text), true);
    }
    eq("repetitive input shrinks well below its own size",
       K.parseHeader(K.pack(rep, {})).compressedSize < 1000, true);
    eq("input crossing the ring wrap still shrinks",
       K.parseHeader(K.pack(wrap, {})).compressedSize < wrap.length, true);
    // LZSS expands incompressible input, and occasionally finds a repeat inside
    // pseudo-random data. Requiring a fixed expansion would assert something the
    // algorithm does not promise, so only the round-trip is required there.
    eq("pseudo-random input still round-trips through a real file",
       same(K.unpack(K.pack(prng, {}), {}).data, prng), true);
    eq("unpacking a file with a bad header reports the header error",
       K.unpack(new Uint8Array(C.HEADER_SIZE), {}).ok, false);
}

console.log("\n%d passed, %d failed", pass, fail);
process.exit(fail ? 1 : 0);
