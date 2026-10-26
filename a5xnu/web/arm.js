// ARMv7-A interpreter and paged memory bus, written from scratch for the A5
// harness. No dependency on Unicorn or any other emulation library: the same
// kernel-entry bytes are executed here and under Unicorn, and the two are
// required to agree (see tests-js/test_a5_boot.js).
//
// Scope: ARM mode is implemented broadly enough to be a usable general-purpose
// core (data processing with the full shifter, load/store incl. LDM/STM and
// post-index, multiply incl. the 64-bit forms, branches, BX/BLX, MSR/MRS,
// SVC, UDF, coprocessor transfers). Thumb mode covers the common 16-bit forms
// and the BL/BLX pair; anything unimplemented stops the run with a reason
// rather than silently doing the wrong thing.

// Resolve the namespace from the global object so that every file shares one
// A5. In a browser `var A5` at top level already does this; under Node/CommonJS
// each module has its own scope, so without this arm.js and a5.js end up with
// two different A5 objects and `new A5.Bus(...)` fails.
var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    // ------------------------------------------------------------------
    // Paged memory + MMIO dispatch
    // ------------------------------------------------------------------

    var PAGE_BITS = 16;
    var PAGE_SIZE = 1 << PAGE_BITS;
    var PAGE_MASK = PAGE_SIZE - 1;

    function Bus(opts) {
        opts = opts || {};
        this.pages = new Map();          // page index -> Uint8Array
        this.mmio = [];                  // {begin, end, name, read, write}
        this.ramBegin = opts.ramBegin >>> 0;
        this.ramEnd = opts.ramEnd >>> 0;
        this.faults = [];                // bounded log of unmapped accesses
        this.faultTotal = 0;
        this.maxFaults = opts.maxFaults || 4096;
        this.autoMap = opts.autoMap !== false;
    }

    Bus.prototype._logFault = function (kind, addr, size) {
        if (this.faults.length < this.maxFaults) {
            this.faults.push({ kind: kind, addr: addr >>> 0, size: size });
        }
        this.faultTotal++;
    };

    Bus.prototype._page = function (idx) {
        var p = this.pages.get(idx);
        if (p === undefined) {
            p = new Uint8Array(PAGE_SIZE);
            this.pages.set(idx, p);
        }
        return p;
    };

    // MMIO only lives outside the RAM window, so the common case skips the
    // device scan entirely.
    Bus.prototype._mmioAt = function (addr) {
        if (addr >= this.ramBegin && addr < this.ramEnd) return null;
        var list = this.mmio;
        for (var i = 0; i < list.length; i++) {
            var d = list[i];
            if (addr >= d.begin && addr <= d.end) return d;
        }
        return null;
    };

    Bus.prototype.addMMIO = function (name, begin, size, read, write) {
        this.mmio.push({
            name: name,
            begin: begin >>> 0,
            end: (begin + size - 1) >>> 0,
            read: read,
            write: write
        });
    };

    Bus.prototype.read8 = function (addr) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) return (d.read ? d.read(addr - d.begin, 1) : 0) & 0xFF;
        var p = this.pages.get(addr >>> PAGE_BITS);
        if (p === undefined) {
            this._logFault("read", addr, 1);
            p = this.autoMap ? this._page(addr >>> PAGE_BITS) : null;
            if (!p) return 0;
        }
        return p[addr & PAGE_MASK];
    };

    Bus.prototype.write8 = function (addr, v) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) { if (d.write) d.write(addr - d.begin, 1, v & 0xFF); return; }
        var p = this.pages.get(addr >>> PAGE_BITS);
        if (p === undefined) {
            this._logFault("write", addr, 1);
            p = this.autoMap ? this._page(addr >>> PAGE_BITS) : null;
            if (!p) return;
        }
        p[addr & PAGE_MASK] = v & 0xFF;
    };

    Bus.prototype.read32 = function (addr) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) return (d.read ? d.read(addr - d.begin, 4) : 0) | 0;
        var p = this.pages.get(addr >>> PAGE_BITS);
        if (p === undefined) {
            this._logFault("read", addr, 4);
            p = this.autoMap ? this._page(addr >>> PAGE_BITS) : null;
            if (!p) return 0;
        }
        var o = addr & PAGE_MASK;
        if (o <= PAGE_SIZE - 4) {
            return (p[o] | (p[o + 1] << 8) | (p[o + 2] << 16) | (p[o + 3] << 24)) | 0;
        }
        // straddles a page boundary
        return (this.read8(addr) | (this.read8(addr + 1) << 8) |
                (this.read8(addr + 2) << 16) | (this.read8(addr + 3) << 24)) | 0;
    };

    Bus.prototype.write32 = function (addr, v) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) { if (d.write) d.write(addr - d.begin, 4, v | 0); return; }
        var p = this.pages.get(addr >>> PAGE_BITS);
        if (p === undefined) {
            this._logFault("write", addr, 4);
            p = this.autoMap ? this._page(addr >>> PAGE_BITS) : null;
            if (!p) return;
        }
        var o = addr & PAGE_MASK;
        if (o <= PAGE_SIZE - 4) {
            p[o] = v & 0xFF;
            p[o + 1] = (v >>> 8) & 0xFF;
            p[o + 2] = (v >>> 16) & 0xFF;
            p[o + 3] = (v >>> 24) & 0xFF;
            return;
        }
        this.write8(addr, v);
        this.write8(addr + 1, v >>> 8);
        this.write8(addr + 2, v >>> 16);
        this.write8(addr + 3, v >>> 24);
    };

    Bus.prototype.read16 = function (addr) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) return (d.read ? d.read(addr - d.begin, 2) : 0) & 0xFFFF;
        return (this.read8(addr) | (this.read8(addr + 1) << 8)) & 0xFFFF;
    };

    Bus.prototype.write16 = function (addr, v) {
        addr >>>= 0;
        var d = this._mmioAt(addr);
        if (d) { if (d.write) d.write(addr - d.begin, 2, v & 0xFFFF); return; }
        this.write8(addr, v);
        this.write8(addr + 1, v >>> 8);
    };

    Bus.prototype.writeBytes = function (addr, bytes) {
        for (var i = 0; i < bytes.length; i++) this.write8((addr + i) >>> 0, bytes[i]);
    };

    Bus.prototype.readBytes = function (addr, n) {
        var out = new Uint8Array(n);
        for (var i = 0; i < n; i++) out[i] = this.read8((addr + i) >>> 0);
        return out;
    };

    A5.Bus = Bus;

    // ------------------------------------------------------------------
    // Shifter
    // ------------------------------------------------------------------

    // type: 0 = LSL, 1 = LSR, 2 = ASR, 3 = ROR (or RRX when the immediate
    // encoding says amount 0). Returns {v, c} with v unsigned.
    function shiftImm(type, value, amount, carryIn) {
        value >>>= 0;
        if (type === 0) {                                    // LSL
            if (amount === 0) return { v: value, c: carryIn };
            if (amount < 32) return { v: (value << amount) >>> 0, c: (value >>> (32 - amount)) & 1 };
            if (amount === 32) return { v: 0, c: value & 1 };
            return { v: 0, c: 0 };
        }
        if (type === 1) {                                    // LSR
            if (amount === 0) amount = 32;
            if (amount < 32) return { v: value >>> amount, c: (value >>> (amount - 1)) & 1 };
            if (amount === 32) return { v: 0, c: (value >>> 31) & 1 };
            return { v: 0, c: 0 };
        }
        if (type === 2) {                                    // ASR
            if (amount === 0) amount = 32;
            if (amount < 32) return { v: ((value | 0) >> amount) >>> 0, c: (value >>> (amount - 1)) & 1 };
            return { v: ((value | 0) >> 31) >>> 0, c: (value >>> 31) & 1 };
        }
        if (amount === 0) {                                  // RRX
            return { v: ((carryIn << 31) | (value >>> 1)) >>> 0, c: value & 1 };
        }
        var a = amount & 31;
        if (a === 0) return { v: value, c: (value >>> 31) & 1 };
        return { v: ((value >>> a) | (value << (32 - a))) >>> 0, c: (value >>> (a - 1)) & 1 };
    }

    function shiftReg(type, value, amount, carryIn) {
        value >>>= 0;
        amount &= 0xFF;
        if (amount === 0) return { v: value, c: carryIn };
        if (type === 0) {
            if (amount < 32) return { v: (value << amount) >>> 0, c: (value >>> (32 - amount)) & 1 };
            if (amount === 32) return { v: 0, c: value & 1 };
            return { v: 0, c: 0 };
        }
        if (type === 1) {
            if (amount < 32) return { v: value >>> amount, c: (value >>> (amount - 1)) & 1 };
            if (amount === 32) return { v: 0, c: (value >>> 31) & 1 };
            return { v: 0, c: 0 };
        }
        if (type === 2) {
            if (amount < 32) return { v: ((value | 0) >> amount) >>> 0, c: (value >>> (amount - 1)) & 1 };
            return { v: ((value | 0) >> 31) >>> 0, c: (value >>> 31) & 1 };
        }
        var a = amount & 31;
        if (a === 0) return { v: value, c: (value >>> 31) & 1 };
        return { v: ((value >>> a) | (value << (32 - a))) >>> 0, c: (value >>> (a - 1)) & 1 };
    }

    // ------------------------------------------------------------------
    // CPU
    // ------------------------------------------------------------------

    // -- processor modes -------------------------------------------------
    var MODE_USR = 0x10, MODE_FIQ = 0x11, MODE_IRQ = 0x12, MODE_SVC = 0x13,
        MODE_MON = 0x16, MODE_ABT = 0x17, MODE_HYP = 0x1A, MODE_UND = 0x1B,
        MODE_SYS = 0x1F;

    // Exception vector offsets from the vector base.
    var VEC_RESET = 0x00, VEC_UNDEF = 0x04, VEC_SVC = 0x08, VEC_PABT = 0x0C,
        VEC_DABT = 0x10, VEC_IRQ = 0x18, VEC_FIQ = 0x1C;

    /**
     * The CPU's view of memory. The Bus holds *physical* pages; every address
     * an instruction carries is *virtual*, and this is the layer that turns one
     * into the other, raising a prefetch or data abort when the walk fails.
     *
     * It exposes the same read/write surface as the Bus so the decode paths do
     * not have to know whether translation is on. `fetch*` differ only in that
     * they return null on an abort instead of a value, because an aborted fetch
     * must not be decoded.
     */
    function CpuMem(cpu) { this.cpu = cpu; }

    CpuMem.prototype._pa = function (va, kind) {
        var cpu = this.cpu;
        va >>>= 0;
        var t = cpu.mmu ? cpu.mmu.translate(va, kind) : { ok: true, pa: va };
        if (!t.ok) {
            cpu.takeMemFault(kind, va, t.fsr, t.level, t.domain);
            return null;
        }
        // SCTLR.A makes an unaligned word or halfword access fault instead of
        // being silently truncated down to its aligned base.
        if (cpu.mmu && cpu.mmu.alignCheck()) {
            var need = (kind === "byte") ? 0 : (va & 3) ? 4 : 0;
            if (need === 4 && kind !== "half") {
                cpu.takeMemFault(kind, va, A5.FSR.ALIGNMENT, 0, 0);
                return null;
            }
        }
        return t.pa;
    };

    CpuMem.prototype.read32 = function (va) {
        var pa = this._pa(va, "read");
        return pa === null ? 0 : this.cpu.bus.read32(pa);
    };
    CpuMem.prototype.read16 = function (va) {
        var pa = this._pa(va, "read");
        return pa === null ? 0 : this.cpu.bus.read16(pa);
    };
    CpuMem.prototype.read8 = function (va) {
        var pa = this._pa(va, "read");
        return pa === null ? 0 : this.cpu.bus.read8(pa);
    };
    CpuMem.prototype.write32 = function (va, v) {
        var pa = this._pa(va, "write");
        if (pa !== null) this.cpu.bus.write32(pa, v);
    };
    CpuMem.prototype.write16 = function (va, v) {
        var pa = this._pa(va, "write");
        if (pa !== null) this.cpu.bus.write16(pa, v);
    };
    CpuMem.prototype.write8 = function (va, v) {
        var pa = this._pa(va, "write");
        if (pa !== null) this.cpu.bus.write8(pa, v);
    };
    CpuMem.prototype.fetch32 = function (va) {
        var pa = this._pa(va, "exec");
        return pa === null ? null : this.cpu.bus.read32(pa);
    };
    CpuMem.prototype.fetch16 = function (va) {
        var pa = this._pa(va, "exec");
        return pa === null ? null : this.cpu.bus.read16(pa);
    };

    function ArmCpu(bus, opts) {
        opts = opts || {};
        this.bus = bus;
        this.r = new Int32Array(16);
        // Unicorn's initial CPSR for an ARM-mode start: SVC mode, IRQ/FIQ
        // masked, thumb clear. Ending on 0x600001F3 is one of the cross-checks
        // against the reference implementation.
        this.cpsr = opts.cpsr !== undefined ? opts.cpsr : 0x000001D3;
        this.instrCount = 0;
        this.halted = false;
        this.stopReason = null;      // "handoff" | "insn-invalid" | "unimplemented" | ...
        this.stoppedAt = 0;
        this.cp15 = {};              // "opc1,crn,crm,opc2" -> value
        this.cp15Log = [];
        this.syscallLog = [];
        this.onStop = opts.onStop || null;
        this.onStep = opts.onStep || null;   // (pc, thumb) before each instruction
        this.stopAt = opts.stopAt || null;   // address that ends the run
        this._branched = false;
        this.unimplementedCount = 0;

        // Banked registers. r8-r12 have two banks (FIQ and everything else);
        // r13-r14 have one per mode, except USR and SYS, which share.
        this.bankUsr = new Int32Array(7);   // [0..4] = r8..r12, [5],[6] = r13,r14
        this.bankFiq = new Int32Array(7);   // same layout, FIQ's own r8..r14
        this.bankSvc = new Int32Array(2);   // r13, r14
        this.bankAbt = new Int32Array(2);
        this.bankIrq = new Int32Array(2);
        this.bankUnd = new Int32Array(2);
        this.spsr = {};                     // mode -> saved CPSR
        this.exceptionCount = 0;
        this.exceptionLog = [];
        this.aborts = { prefetch: 0, data: 0 };
        this._curPc = 0;                    // address of the instruction in flight
        this.barrierCount = 0;
        this.vfp = {};                // VFP system register shadow (cp10)
        this.vfpLog = [];
        this.interruptController = null;   // set by the machine (an A5.Aic)
        this.irqCount = 0;
        this.fiqCount = 0;
        this.haltOnUndefined = opts.haltOnUndefined !== false;
        this.mmu = opts.mmu || null;
        this.mem = new CpuMem(this);
    }

    ArmCpu.prototype.get = function (n) {
        if (n === 15) return (this.r[15] + (this.isThumb() ? 4 : 8)) | 0;
        return this.r[n];
    };

    ArmCpu.prototype.isThumb = function () {
        return (this.cpsr & 0x20) !== 0;
    };

    ArmCpu.prototype.branchTo = function (target) {
        target >>>= 0;
        var t = target & 0x1;
        if (t) this.cpsr |= 0x20; else this.cpsr &= ~0x20;
        this.r[15] = (target & ~0x1) | 0;
        this._branched = true;
    };

    ArmCpu.prototype.setFlagsNZ = function (result) {
        result >>>= 0;
        if (result & 0x80000000) this.cpsr |= 0x80000000; else this.cpsr &= 0x7FFFFFFF;
        if (result === 0) this.cpsr |= 0x40000000; else this.cpsr &= 0xBFFFFFFF;
    };

    ArmCpu.prototype.setC = function (c) {
        if (c & 1) this.cpsr |= 0x20000000; else this.cpsr &= 0xDFFFFFFF;
    };

    ArmCpu.prototype.setV = function (v) {
        if (v & 1) this.cpsr |= 0x10000000; else this.cpsr &= 0xEFFFFFFF;
    };

    ArmCpu.prototype.getC = function () { return (this.cpsr >>> 29) & 1; };
    ArmCpu.prototype.getV = function () { return (this.cpsr >>> 28) & 1; };
    ArmCpu.prototype.getN = function () { return (this.cpsr >>> 31) & 1; };
    ArmCpu.prototype.getZ = function () { return (this.cpsr >>> 30) & 1; };

    ArmCpu.prototype.checkCond = function (cond) {
        var n = this.getN(), z = this.getZ(), c = this.getC(), v = this.getV();
        switch (cond) {
            case 0x0: return z === 1;                       // EQ
            case 0x1: return z === 0;                       // NE
            case 0x2: return c === 1;                       // CS/HS
            case 0x3: return c === 0;                       // CC/LO
            case 0x4: return n === 1;                       // MI
            case 0x5: return n === 0;                       // PL
            case 0x6: return v === 1;                       // VS
            case 0x7: return v === 0;                       // VC
            case 0x8: return c === 1 && z === 0;            // HI
            case 0x9: return c === 0 || z === 1;            // LS
            case 0xA: return n === v;                       // GE
            case 0xB: return n !== v;                       // LT
            case 0xC: return z === 0 && n === v;            // GT
            case 0xD: return z === 1 || n !== v;            // LE
            case 0xE: return true;                          // AL
            default: return true;                           // NV treated as AL on ARMv7
        }
    };

    /**
     * Take a pending IRQ or FIQ before executing the instruction at pc.
     * Returns true if an exception was taken, in which case PC already points at
     * the vector and the instruction at pc must not run.
     *
     * FIQ has priority and additionally masks FIQ on entry. The only gate is the
     * CPSR mask bit: real hardware takes an IRQ in IRQ mode too if the handler
     * re-enabled them, so there is no mode check here. A handler that fails to
     * clear its source recurses, and the machine's stall guard is what catches
     * that -- which is the honest outcome, since infinite recursion is what the
     * hardware would do too.
     */
    ArmCpu.prototype.checkInterrupts = function (pc) {
        var ic = this.interruptController;
        if (!ic) return false;
        var cpsr = this.cpsr >>> 0;
        if (ic.fiqPending() && !(cpsr & 0x40)) {
            this.fiqCount++;
            this.takeException(VEC_FIQ, MODE_FIQ, (pc + 4) >>> 0, true);
            return true;
        }
        if (ic.irqPending() && !(cpsr & 0x80)) {
            this.irqCount++;
            this.takeException(VEC_IRQ, MODE_IRQ, (pc + 4) >>> 0, false);
            return true;
        }
        return false;
    };

    // -- modes, banked registers and exceptions -------------------------

    ArmCpu.prototype.mode = function () { return this.cpsr & 0x1F; };

    ArmCpu.prototype.privileged = function () {
        // USR is the only unprivileged mode at EL0; SYS shares its registers
        // but runs privileged.
        return this.mode() !== MODE_USR;
    };

    ArmCpu.prototype._bank1314 = function (mode) {
        switch (mode) {
            case MODE_FIQ: return this.bankFiq;   // r13/r14 live at [5],[6]
            case MODE_SVC: return this.bankSvc;
            case MODE_ABT: return this.bankAbt;
            case MODE_IRQ: return this.bankIrq;
            case MODE_UND: return this.bankUnd;
            default:       return this.bankUsr;   // USR and SYS share
        }
    };

    ArmCpu.prototype.switchMode = function (newMode) {
        var old = this.mode();
        if (old === newMode) return;
        var i, b;
        if (old === MODE_FIQ) {
            for (i = 8; i <= 14; i++) this.bankFiq[i - 8] = this.r[i];
        } else {
            for (i = 8; i <= 12; i++) this.bankUsr[i - 8] = this.r[i];
            b = this._bank1314(old);
            if (old === MODE_USR || old === MODE_SYS) { b[5] = this.r[13]; b[6] = this.r[14]; }
            else { b[0] = this.r[13]; b[1] = this.r[14]; }
        }
        if (newMode === MODE_FIQ) {
            for (i = 8; i <= 14; i++) this.r[i] = this.bankFiq[i - 8];
        } else {
            for (i = 8; i <= 12; i++) this.r[i] = this.bankUsr[i - 8];
            b = this._bank1314(newMode);
            if (newMode === MODE_USR || newMode === MODE_SYS) { this.r[13] = b[5]; this.r[14] = b[6]; }
            else { this.r[13] = b[0]; this.r[14] = b[1]; }
        }
        this.cpsr = ((this.cpsr & ~0x1F) | newMode) | 0;
    };

    ArmCpu.prototype.spsrRead = function () {
        var m = this.mode();
        if (m === MODE_USR || m === MODE_SYS) return this.cpsr | 0;   // no SPSR: UNPREDICTABLE
        return (this.spsr[m] | 0);
    };

    ArmCpu.prototype.spsrWrite = function (v) {
        var m = this.mode();
        if (m === MODE_USR || m === MODE_SYS) return;
        this.spsr[m] = v | 0;
    };

    ArmCpu.prototype.vectorBase = function () {
        return (this.cp15Read(0, 1, 0, 0) & (1 << 13)) ? 0xFFFF0000 : 0x00000000;
    };

    /**
     * Take an exception. SPSR gets the CPSR as it was *before* anything here
     * changed, the target mode's bank is switched in, IRQ is masked (FIQ too
     * where the architecture requires it), Thumb is cleared, LR gets the
     * mode-specific return address and PC the vector.
     */
    ArmCpu.prototype.takeException = function (vec, newMode, lr, maskF) {
        var cpsr = this.cpsr >>> 0;
        var old = cpsr & 0x1F;
        this.switchMode(newMode);
        // SPSR_<new mode> receives the CPSR as it was in the *interrupted* mode.
        // Saving into spsr[old] instead leaves the handler's `mrs Rd, spsr`
        // reading an untouched zero while the interrupted CPSR sits in a bank
        // nobody looks at -- the abort still vectors correctly, so the only
        // symptom is a handler that cannot recover the mode and flags it
        // interrupted.
        this.spsr[newMode] = cpsr | 0;
        var n = (cpsr | 0x80);                    // mask IRQ
        if (maskF) n |= 0x40;                     // mask FIQ as well
        n &= ~0x20;                               // exceptions are entered in ARM state
        n = (n & ~0x1F) | newMode;
        this.cpsr = n | 0;
        this.r[14] = lr | 0;
        this.r[15] = (this.vectorBase() + vec) | 0;
        this._branched = true;
        this._aborted = true;
        this.exceptionCount++;
        if (this.exceptionLog.length < 4096) {
            this.exceptionLog.push({
                vec: vec, mode: newMode, from: old,
                pc: this._curPc >>> 0, lr: lr >>> 0, spsr: cpsr >>> 0
            });
        }
    };

    // Publish DFSR/IFSR + FAR and vector, exactly as the architecture requires.
    /**
     * Exception return: a write of r15 with the S bit set. CPSR is restored from
     * SPSR_<current mode>, which is how a handler puts back the mode it
     * interrupted, the Thumb state and the interrupt masks it inherited -- this
     * is what `subs pc, lr, #4` at the end of an IRQ handler actually does, and
     * what `ldmfd sp!, {..., pc}^` does at the end of an abort handler.
     *
     * The bank swap has to happen before CPSR is overwritten, because switchMode
     * finds the old mode by reading the current CPSR.
     *
     * In USR/SYS there is no SPSR to restore from; that case is UNPREDICTABLE in
     * the architecture, so the branch is still taken and CPSR is left alone.
     */
    ArmCpu.prototype.exceptionReturn = function (target) {
        var spsr = this.spsr[this.mode()];
        if (spsr === undefined) { this.branchTo(target); return; }
        this.switchMode(spsr & 0x1F);
        this.cpsr = spsr | 0;
        this.r[15] = target | 0;
        this._branched = true;
    };

    ArmCpu.prototype.takeMemFault = function (kind, va, fsr, level, domain) {
        if (kind === "exec") {
            this.cp15Write(0, 5, 0, 1, fsr);      // IFSR
            this.cp15Write(0, 6, 0, 0, va);       // FAR
            this.aborts.prefetch++;
            // LR_abt = address of the aborted instruction + 4
            this.takeException(VEC_PABT, MODE_ABT, (this._curPc + 4) >>> 0, false);
        } else {
            this.cp15Write(0, 5, 0, 0, fsr);      // DFSR
            this.cp15Write(0, 6, 0, 0, va);       // FAR
            this.aborts.data++;
            // LR_abt = address of the instruction that made the access + 8
            this.takeException(VEC_DABT, MODE_ABT, (this._curPc + 8) >>> 0, false);
        }
    };

    ArmCpu.prototype.takeUndefined = function (pc) {
        this.takeException(VEC_UNDEF, MODE_UND, (pc + 4) >>> 0, false);
    };

    ArmCpu.prototype.halt = function (reason, addr) {
        this.halted = true;
        this.stopReason = reason;
        this.stoppedAt = addr >>> 0;
        if (this.onStop) this.onStop(reason, addr >>> 0);
    };

    // -- cp15 -----------------------------------------------------------

    ArmCpu.prototype.cp15Key = function (opc1, crn, crm, opc2) {
        return opc1 + "," + crn + "," + crm + "," + opc2;
    };

    ArmCpu.prototype.cp15Read = function (opc1, crn, crm, opc2) {
        var v = this.cp15[this.cp15Key(opc1, crn, crm, opc2)];
        return (v === undefined) ? 0 : v | 0;
    };

    ArmCpu.prototype.cp15Write = function (opc1, crn, crm, opc2, value) {
        this.cp15[this.cp15Key(opc1, crn, crm, opc2)] = value | 0;
    };

    // -- main step ------------------------------------------------------

    ArmCpu.prototype.step = function () {
        if (this.halted) return false;
        var pc = this.r[15] >>> 0;

        if (this.stopAt !== null && pc === this.stopAt) {
            this.halt("handoff", pc);
            return false;
        }

        this._branched = false;
        this._aborted = false;
        this.instrCount++;
        this._curPc = pc;
        // Checked before the fetch: the interrupted instruction must not execute,
        // and LR_irq is its address + 4, which is only true if we vector from
        // here rather than after decoding.
        if (this.checkInterrupts(pc)) return !this.halted;
        var wasThumb = this.isThumb();
        if (this.onStep) this.onStep(pc, wasThumb);

        if (this.isThumb()) {
            this.stepThumb(pc);
            if (this._aborted) return !this.halted;
            // Thumb instructions are 2 bytes; the only 32-bit forms implemented
            // (BL/BLX) always branch, so they never reach this advance. Without
            // it PC never moves in Thumb state and the run wedges on the very
            // first instruction, which the stall guard then reports as "stalled"
            // with all later registers still zero.
            if (!this._branched) this.r[15] = (pc + 2) | 0;
        } else {
            var insn = this.mem.fetch32(pc);
            // A translation fault on the fetch has already vectored to the
            // prefetch-abort handler; decoding the returned zero word would
            // execute a spurious AND in the middle of taking the exception.
            if (insn === null) return !this.halted;
            var cond = (insn >>> 28) & 0xF;
            if (!this.checkCond(cond)) {
                this.r[15] = (pc + 4) | 0;
                return true;
            }
            if (!this.execArm(insn, pc)) return false;
            if (!this._branched) this.r[15] = (pc + 4) | 0;
        }
        return !this.halted;
    };

    /**
     * Clear a halt so execution can continue from the current PC. Used to run a
     * second stage after the handoff: start() zeroes 40 KB of page tables, so
     * anything a test wants to install *inside* that region -- the high-vector
     * L2, for instance -- has to be written after the boot has finished, not
     * before it.
     */
    ArmCpu.prototype.resume = function (stopAt) {
        this.halted = false;
        this.stopReason = null;
        this.stoppedAt = 0;
        if (stopAt !== undefined && stopAt !== null) this.stopAt = stopAt >>> 0;
    };

    ArmCpu.prototype.run = function (maxInstr) {
        var n = 0;
        var limit = maxInstr || 1e9;
        while (!this.halted && n < limit) {
            this.step();
            n++;
        }
        return !this.halted ? "budget" : this.stopReason;
    };

    // -- ARM mode -------------------------------------------------------

    ArmCpu.prototype.execArm = function (insn, pc) {
        insn >>>= 0;

        // coprocessor register transfer: cond 1110 opc1 L CRn Rt coproc opc2 1 CRm
        if ((insn & 0x0F000010) === 0x0E000010) {
            var coproc = (insn >>> 8) & 0xF;
            if (coproc === 15) return this.execMcrMrc(insn, pc);
            // Coprocessor 10 is VFP, and `vmrs Rt, fpexc` / `fmxr fpexc, Rt`
            // arrive here as MRC/MCR p10, #7. Bringing the FPU up needs exactly
            // this path: CPACR gets cp10/cp11 unblocked, then FPEXC.EN is set.
            if (coproc === 10 && ((insn >>> 21) & 7) === 7) {
                return this.execVfpSysreg(insn, pc);
            }
            this.unimplementedCount++;
            this.halt("unimplemented-coproc-" + coproc, pc);
            return false;
        }

        var hi = (insn >>> 25) & 0x7;

        // SWI / SVC. Recorded and then taken for real: the kernel's Mach trap
        // path is entered through this vector, so stepping over it would hide
        // every trap the guest makes.
        if ((insn & 0x0F000000) === 0x0F000000) {
            var imm24 = insn & 0xFFFFFF;
            this.syscallLog.push({ pc: pc, imm: imm24 });
            this.takeException(VEC_SVC, MODE_SVC, (pc + 4) >>> 0, false);
            return true;
        }

        // Memory barriers. keystone encodes DSB/DMB/ISB in their hint form
        // (0xF57FF04F / 0xF57FF05F / 0xF57FF06F), not as the `mcr p15, 0, Rt,
        // c7, ...` aliases, so they never reach execMcrMrc and would otherwise
        // fall through to "unimplemented".
        //
        // They are no-ops here, and legitimately so: there is no cache model, no
        // write buffer, no second observer of memory, and the MMU reads its
        // translation state live on every walk -- which is precisely the
        // guarantee an ISB exists to provide on real hardware.
        // The >>> 0 is load-bearing. JavaScript's & yields a *signed* 32-bit
        // integer, so 0xF57FF04F & 0xFFFFFFF0 is negative, while the literal
        // 0xF57FF040 is a positive double, and the comparison can never be
        // true. Every other test in this decoder masks bit 31 away and is
        // therefore safe; this is the only one comparing against a value at or
        // above 0x80000000. Without the coercion DSB decoded as
        // `ldrb pc, [pc, #79]` and jumped to a byte of memory.
        var bar = (insn & 0xFFFFFFF0) >>> 0;
        if (bar === 0xF57FF040 || bar === 0xF57FF050 || bar === 0xF57FF060) {
            this.barrierCount++;
            return true;
        }

        // UDF. A real Undefined Instruction exception; the harness can instead
        // stop the run, which is far more useful while bringing code up.
        if ((insn & 0x0FF000F0) === 0x07F000F0) {
            if (this.haltOnUndefined) { this.halt("insn-invalid", pc); return false; }
            this.takeUndefined(pc);
            return true;
        }

        // branch / branch-with-link. BL and the BLX immediate share bits
        // [27:25] = 101; BLX is distinguished by the condition field being 0xF,
        // NOT by bit 24 (bit 24 is its H bit), so the condition has to be tested
        // first or BLX with H=0 silently becomes a plain branch.
        if ((insn & 0x0E000000) === 0x0A000000) {
            var condB = (insn >>> 28) & 0xF;
            var off = insn & 0xFFFFFF;
            if (off & 0x800000) off -= 0x1000000;
            var target = (pc + 8 + (off << 2)) >>> 0;

            if (condB === 0xF) {                          // BLX (immediate)
                var H = (insn >>> 24) & 1;
                this.r[14] = (pc + 4) | 0;
                this.branchTo(((target + (H << 1)) | 1) >>> 0);
                return true;
            }
            if ((insn >>> 24) & 1) this.r[14] = (pc + 4) | 0;   // BL
            this.branchTo(target);
            return true;
        }

        // BX / BLX register, MSR/MRS and the rest of the "misc" space
        if ((insn & 0x0FFFFFF0) === 0x012FFF10) {          // BX Rm
            this.branchTo(this.get(insn & 0xF) >>> 0);
            return true;
        }
        if ((insn & 0x0FFFFFF0) === 0x012FFF30) {          // BLX Rm
            this.r[14] = (pc + 4) | 0;
            this.branchTo(this.get(insn & 0xF) >>> 0);
            return true;
        }
        // MRS Rd, CPSR/SPSR: cond 0001 0 R 00 1111 Rd 000000000000.
        // The mask has to check bits[19:16] == 0b1111 and leave Rd free. It used
        // to be 0x0FB0FFF0, which does the opposite: Rd (bits[15:12]) was forced
        // to 0 and bits[19:16] were ignored, so only `mrs r0, cpsr` decoded and
        // every other MRS fell through to the data-processing decoder. With
        // opcode bits[24:21] == 0b1010 that made `mrs r3, spsr` execute as CMP:
        // it set flags, wrote nothing, and the Abort handler reported the
        // leftover boot-loop value of r3 (0x00100000, the 1 MB step) as SPSR.
        if ((insn & 0x0FBF0FFF) === 0x010F0000) {          // MRS Rd, CPSR/SPSR
            var isSpsr = (insn >>> 22) & 1;
            this.r[(insn >>> 12) & 0xF] = isSpsr ? this.spsrRead() : (this.cpsr | 0);
            return true;
        }
        if ((insn & 0x0DB0F000) === 0x0120F000) {          // MSR CPSR/SPSR, <operand>
            return this.execMsr(insn, pc);
        }

        // multiply: cond 0000 00 op S RdHi RdLo Rs 1001 Rm  (bits[7:4] = 1001)
        if ((insn & 0x0FC000F0) === 0x00000090) {
            return this.execMultiply(insn, pc);
        }

        // SWP / SWPB: cond 0001 0 B 00 Rn Rt 0000 1001 Rt2.
        // Shares bits[7:4] == 1001 with the multiply space but has bit 24 set,
        // so the multiply test above does not catch it. Left unhandled it fell
        // through to the data-processing decoder, where bits[24:21] == 0b0000
        // made `swp r0, r2, [r1]` execute as an AND: no memory was touched and
        // nothing signalled that anything was wrong.
        if ((insn & 0x0FB00FF0) === 0x01000090) return this.execSwap(insn, pc);

        // LDM / STM
        if ((insn & 0x0E000000) === 0x08000000) {
            return this.execLdmStm(insn, pc);
        }

        // load/store
        if ((insn & 0x0C000000) === 0x04000000) {
            return this.execLoadStore(insn, pc);
        }

        // extra load/store (halfword, signed byte): cond 000 P U x W L Rn Rt
        // xxxx 1 SH 1 xxxx with SH != 00. SH == 00 with bits[7:4] == 1001 is the
        // multiply/SWP space, which was already handled above, so this must be
        // tested on the sh field and not on the bit pattern alone.
        if ((insn & 0x0E000090) === 0x00000090 && ((insn >>> 5) & 3) !== 0) {
            return this.execHalfword(insn, pc);
        }

        // MOVW / MOVT: cond 0011 0 W 0 0 imm4 Rd imm12
        // bits[27:23] = 00110 and bits[21:20] = 00 identify both; bit 22 is W
        // and must be masked OUT. Testing with 0x0FF00000 instead only matched
        // MOVW, so MOVT fell through to the data-processing decoder and was
        // executed as a CMP - the register kept its low half and every
        // subsequent MMIO access went to the wrong address.
        if ((insn & 0x0FB00000) === 0x03000000) {
            var imm16 = (((insn >>> 16) & 0xF) << 12) | (insn & 0xFFF);
            var rd = (insn >>> 12) & 0xF;
            if ((insn >>> 22) & 1) {                       // MOVT
                this.r[rd] = ((this.r[rd] & 0xFFFF) | (imm16 << 16)) | 0;
            } else {                                       // MOVW
                this.r[rd] = imm16 | 0;
            }
            if (rd === 15) this._branched = true;
            return true;
        }

        // data processing
        if ((insn & 0x0C000000) === 0x00000000) {
            return this.execDataProc(insn, pc);
        }

        this.unimplementedCount++;
        this.halt("unimplemented-arm-0x" + insn.toString(16), pc);
        return false;
    };

    ArmCpu.prototype.execMcrMrc = function (insn, pc) {
        var opc1 = (insn >>> 21) & 7;
        var isMrc = (insn >>> 20) & 1;
        var crn = (insn >>> 16) & 0xF;
        var rt = (insn >>> 12) & 0xF;
        var opc2 = (insn >>> 5) & 7;
        var crm = insn & 0xF;

        this.cp15Log.push({
            kind: isMrc ? "mrc" : "mcr",
            pc: pc, crn: crn, crm: crm, opc1: opc1, opc2: opc2, rt: rt
        });

        if (isMrc) {
            var v = this.cp15Read(opc1, crn, crm, opc2);
            this.r[rt] = v;
            if (rt === 15) {
                // MRC to PC writes the flags; keep it simple and just set them
                this.cpsr = (this.cpsr & 0x0FFFFFFF) | (v & 0xF0000000);
                this._branched = false;
            }
        } else {
            var src = (rt === 15) ? ((pc + 8) | 0) : this.r[rt];
            this.cp15Write(opc1, crn, crm, opc2, src);
        }
        return true;
    };

    /**
     * VFP system register access (vmrs / fmxr). Values round-trip through a
     * shadow keyed the same way as cp15's, so an enable sequence written by
     * guest code is observable. The read-only identification registers (FPSID,
     * MVFR0/MVFR1) return whatever the guest last wrote and zero otherwise:
     * the A5's exact VFP identification values are not in any source consulted
     * here, and inventing a part number would be worse than returning zero.
     */
    ArmCpu.prototype.execVfpSysreg = function (insn, pc) {
        var isRead = (insn >>> 20) & 1;
        var crn = (insn >>> 16) & 0xF;
        var rt = (insn >>> 12) & 0xF;
        var opc2 = (insn >>> 5) & 7;
        var crm = insn & 0xF;
        var key = "vfp:" + crn + "," + crm + "," + opc2;
        if (!this.vfpLog) this.vfpLog = [];
        if (this.vfpLog.length < 4096) {
            this.vfpLog.push({ kind: isRead ? "vmrs" : "fmxr", pc: pc, crn: crn, crm: crm, opc2: opc2, rt: rt });
        }
        if (isRead) {
            var v = this.vfp[key];
            this.r[rt] = (v === undefined ? 0 : v) | 0;
        } else {
            if (!this.vfp) this.vfp = {};
            this.vfp[key] = this.r[rt] | 0;
        }
        return true;
    };

    ArmCpu.prototype.execMsr = function (insn, pc) {
        var byImm = (insn >>> 25) & 1;
        var mask = (insn >>> 16) & 0xF;
        var val;
        if (byImm) {
            var rot = ((insn >>> 8) & 0xF) * 2;
            var imm8 = insn & 0xFF;
            val = rot ? (((imm8 >>> rot) | (imm8 << (32 - rot))) >>> 0) : imm8;
        } else {
            val = this.get(insn & 0xF) >>> 0;
        }
        var m = 0;
        if (mask & 1) m |= 0x000000FF;      // control
        if (mask & 2) m |= 0x0000FF00;      // extension
        if (mask & 4) m |= 0x00FF0000;      // status
        if (mask & 8) m |= 0xFF000000;      // flags
        if ((insn >>> 22) & 1) this.spsrWrite(((this.spsrRead() & ~m) | (val & m)) >>> 0);
        else this.cpsr = ((this.cpsr & ~m) | (val & m)) >>> 0;
        return true;
    };

    ArmCpu.prototype.execMultiply = function (insn, pc) {
        var op = (insn >>> 21) & 7;
        var S = (insn >>> 20) & 1;
        var rd = (insn >>> 16) & 0xF;
        var rn = (insn >>> 12) & 0xF;
        var rs = (insn >>> 8) & 0xF;
        var rm = insn & 0xF;
        var a = this.get(rm) >>> 0, b = this.get(rs) >>> 0;

        if (op === 0 || op === 1) {                      // MUL / MLA
            var res = Math.imul(a | 0, b | 0) | 0;
            if (op === 1) res = (res + this.get(rn)) | 0;
            this.r[rd] = res;
            if (rd === 15) this._branched = true;
            if (S) {
                this.setFlagsNZ(res >>> 0);
                // ARMv7: C is UNPREDICTABLE for MUL/MLA; leave it alone
            }
            return true;
        }
        if (op >= 4 && op <= 7) {                        // 64-bit forms
            var signed = (op >= 6);
            var x = signed ? BigInt(a | 0) : BigInt(a >>> 0);
            var y = signed ? BigInt(b | 0) : BigInt(b >>> 0);
            var prod = x * y;
            if (op === 5 || op === 7) {                  // UMLAL / SMLAL accumulate
                prod += (BigInt(this.get(rn) >>> 0) | (BigInt(this.get(rd) >>> 0) << 32n));
            }
            var mask64 = (1n << 64n) - 1n;
            prod &= mask64;
            this.r[rn] = Number(prod & 0xFFFFFFFFn) | 0;
            this.r[rd] = Number((prod >> 32n) & 0xFFFFFFFFn) | 0;
            if (rn === 15 || rd === 15) this._branched = true;
            if (S) {
                var hi = Number((prod >> 32n) & 0xFFFFFFFFn) >>> 0;
                this.setFlagsNZ((prod === 0n) ? 0 : 1);
                if (prod === 0n) this.cpsr |= 0x40000000; else this.cpsr &= 0xBFFFFFFF;
                if (hi & 0x80000000) this.cpsr |= 0x80000000; else this.cpsr &= 0x7FFFFFFF;
                this.setV((prod >> 63n) & 1n);
            }
            return true;
        }
        this.unimplementedCount++;
        this.halt("unimplemented-multiply-op-" + op, pc);
        return false;
    };

    ArmCpu.prototype.execDataProc = function (insn, pc) {
        var op = (insn >>> 21) & 0xF;
        var S = (insn >>> 20) & 1;
        var rn = (insn >>> 16) & 0xF;
        var rd = (insn >>> 12) & 0xF;

        var op2, shifterC;
        if (insn & (1 << 25)) {                          // immediate
            var rot = ((insn >>> 8) & 0xF) * 2;
            var imm8 = insn & 0xFF;
            if (rot === 0) {
                op2 = imm8 >>> 0;
                shifterC = this.getC();
            } else {
                op2 = ((imm8 >>> rot) | (imm8 << (32 - rot))) >>> 0;
                shifterC = (op2 >>> 31) & 1;
            }
        } else {                                         // register (+ shift)
            var rm = insn & 0xF;
            var type = (insn >>> 5) & 3;
            var rmVal;
            if (insn & (1 << 4)) {                       // shift amount in a register
                var rs = (insn >>> 8) & 0xF;
                var amt = this.get(rs) & 0xFF;
                rmVal = (rm === 15) ? ((pc + 8 + ((rs === 15) ? 4 : 0)) >>> 0) : (this.r[rm] >>> 0);
                var sr = shiftReg(type, rmVal, amt, this.getC());
                op2 = sr.v; shifterC = sr.c;
            } else {
                var imm5 = (insn >>> 7) & 0x1F;
                rmVal = (rm === 15) ? ((pc + 8) >>> 0) : (this.r[rm] >>> 0);
                var si = shiftImm(type, rmVal, imm5, this.getC());
                op2 = si.v; shifterC = si.c;
            }
        }

        // MOV and MVN ignore Rn completely; every other opcode reads it, and a
        // read of r15 yields PC + 8. Excluding ADD/TST/TEQ/ORR/BIC here (as an
        // earlier revision did) silently makes them compute against zero.
        var a = (op === 0xD || op === 0xF) ? 0 : (this.get(rn) >>> 0);
        var result = 0, cout = shifterC, vout = this.getV();

        switch (op) {
            case 0x0: result = (a & op2) >>> 0; break;                       // AND
            case 0x1: result = (a ^ op2) >>> 0; break;                       // EOR
            case 0x2: {                                                      // SUB
                var diff = (a - op2) >>> 0;
                cout = (a >= op2) ? 1 : 0;
                vout = (((a ^ op2) & (a ^ diff)) >>> 31) & 1;
                result = diff; break;
            }
            case 0x3: {                                                      // RSB
                var diff2 = (op2 - a) >>> 0;
                cout = (op2 >= a) ? 1 : 0;
                vout = (((op2 ^ a) & (op2 ^ diff2)) >>> 31) & 1;
                result = diff2; break;
            }
            case 0x4: {                                                      // ADD
                var sum = a + op2;
                cout = (sum > 0xFFFFFFFF) ? 1 : 0;
                result = sum >>> 0;
                vout = ((~(a ^ op2) & (a ^ result)) >>> 31) & 1;
                break;
            }
            case 0x5: {                                                      // ADC
                var cin = this.getC();
                var sum2 = a + op2 + cin;
                cout = (sum2 > 0xFFFFFFFF) ? 1 : 0;
                result = sum2 >>> 0;
                vout = ((~(a ^ op2) & (a ^ result)) >>> 31) & 1;
                break;
            }
            case 0x6: {                                                      // SBC
                var cin2 = this.getC();
                var nb = (~op2) >>> 0;
                var sum3 = a + nb + cin2;
                cout = (sum3 > 0xFFFFFFFF) ? 1 : 0;
                result = sum3 >>> 0;
                vout = (((a ^ nb) & (a ^ result)) >>> 31) & 1;
                break;
            }
            case 0x7: {                                                      // RSC
                var cin3 = this.getC();
                var na = (~a) >>> 0;
                var sum4 = op2 + na + cin3;
                cout = (sum4 > 0xFFFFFFFF) ? 1 : 0;
                result = sum4 >>> 0;
                vout = (((op2 ^ na) & (op2 ^ result)) >>> 31) & 1;
                break;
            }
            case 0x8: result = (a & op2) >>> 0; cout = shifterC; break;      // TST
            case 0x9: result = (a ^ op2) >>> 0; cout = shifterC; break;      // TEQ
            case 0xA: {                                                      // CMP
                var d = (a - op2) >>> 0;
                cout = (a >= op2) ? 1 : 0;
                vout = (((a ^ op2) & (a ^ d)) >>> 31) & 1;
                result = d; break;
            }
            case 0xB: {                                                      // CMN
                var s = a + op2;
                cout = (s > 0xFFFFFFFF) ? 1 : 0;
                result = s >>> 0;
                vout = ((~(a ^ op2) & (a ^ result)) >>> 31) & 1;
                break;
            }
            case 0xC: result = (a | op2) >>> 0; break;                       // ORR
            case 0xD: result = op2 >>> 0; break;                             // MOV
            case 0xE: result = (a & ((~op2) >>> 0)) >>> 0; break;            // BIC
            case 0xF: result = (~op2) >>> 0; break;                          // MVN
        }

        // TST/TEQ/CMP/CMN set flags but do not write Rd.
        var writesRd = (op < 0x8) || (op >= 0xC);
        // Logical opcodes take C from the shifter; arithmetic ones take C and V
        // from the adder. Getting this the wrong way round is invisible on the
        // boot path but destroys every later carry chain.
        var logical = (op === 0x0 || op === 0x1 || op === 0x8 || op === 0x9 ||
                       op === 0xC || op === 0xD || op === 0xE || op === 0xF);

        if (writesRd && rd !== 15) this.r[rd] = result | 0;

        if (S) {
            if (rd === 15) {
                this.exceptionReturn(result);
            } else {
                this.setFlagsNZ(result);
                this.setC(logical ? shifterC : cout);
                if (!logical) this.setV(vout);
            }
        } else if (writesRd && rd === 15) {
            this.branchTo(result);
        }
        return true;
    };

    ArmCpu.prototype._lsAddress = function (insn, pc) {
        var rn = (insn >>> 16) & 0xF;
        var base = this.get(rn) >>> 0;
        var U = (insn >>> 23) & 1;
        var P = (insn >>> 24) & 1;
        var W = (insn >>> 21) & 1;
        var offset;

        if (insn & (1 << 25)) {                          // register offset
            var rm = insn & 0xF;
            var type = (insn >>> 5) & 3;
            var imm5 = (insn >>> 7) & 0x1F;
            var rmVal = (rm === 15) ? ((pc + 8) >>> 0) : (this.r[rm] >>> 0);
            offset = shiftImm(type, rmVal, imm5, this.getC()).v;
        } else {
            offset = (insn & 0xFFF) >>> 0;
        }
        var signed = U ? offset : ((-offset) | 0);
        var addr = (base + signed) >>> 0;
        var writeback = null;
        if (P) {
            if (W) writeback = addr;                     // pre-index with writeback
        } else {
            writeback = addr;                            // post-index always writes back
            addr = base;
        }
        return { addr: addr, writeback: writeback, rn: rn };
    };

    ArmCpu.prototype.execLoadStore = function (insn, pc) {
        var rt = (insn >>> 12) & 0xF;
        var L = (insn >>> 20) & 1;
        var B = (insn >>> 22) & 1;
        var t = this._lsAddress(insn, pc);

        if (L) {
            var v = B ? this.mem.read8(t.addr) : this.mem.read32(t.addr);
            if (rt === 15) this.branchTo(v >>> 0);
            else this.r[rt] = v | 0;
        } else {
            var src = (rt === 15) ? ((pc + 8) | 0) : this.r[rt];
            if (B) this.mem.write8(t.addr, src);
            else this.mem.write32(t.addr, src);
        }
        if (t.writeback !== null && t.rn !== 15) {
            if (!(L && rt === t.rn)) this.r[t.rn] = t.writeback | 0;
        }
        return true;
    };

    ArmCpu.prototype.execHalfword = function (insn, pc) {
        var rn = (insn >>> 16) & 0xF;
        var rt = (insn >>> 12) & 0xF;
        var L = (insn >>> 20) & 1;
        var W = (insn >>> 21) & 1;
        var U = (insn >>> 23) & 1;
        var P = (insn >>> 24) & 1;
        var sh = (insn >>> 5) & 3;                       // 1=H, 2=SB, 3=SH
        var offset;
        if (insn & (1 << 22)) offset = (((insn >>> 8) & 0xF) << 4) | (insn & 0xF);
        else offset = this.get(insn & 0xF) >>> 0;
        var base = this.get(rn) >>> 0;
        var addr = (base + (U ? offset : (-offset | 0))) >>> 0;

        // bits[6:5] selects the size, bit 20 the direction, and the pairing is
        // not what the size field alone suggests -- confirmed by assembling each
        // form with keystone and reading it back with capstone:
        //   sh=1 (bits[7:4]=0xB)  L=1 LDRH    L=0 STRH
        //   sh=2 (bits[7:4]=0xD)  L=1 LDRSB   L=0 LDRD
        //   sh=3 (bits[7:4]=0xF)  L=1 LDRSH   L=0 STRD
        // Treating L as "load" throughout makes LDRD a load and STRD a store,
        // but the doubleword forms sit on the L=0 side of sh=2 and the L=0 side
        // of sh=3 respectively.
        if (sh === 2 && !L) return this.execDoubleword(insn, pc, addr, true);
        if (sh === 3 && !L) return this.execDoubleword(insn, pc, addr, false);

        var value;
        if (L) {
            if (sh === 1) value = this.mem.read16(addr) >>> 0;
            else if (sh === 2) value = this.mem.read8(addr) | 0;
            else { value = this.mem.read16(addr); if (value & 0x8000) value |= 0xFFFF0000; value |= 0; }
            if (rt === 15) this.branchTo(value >>> 0); else this.r[rt] = value | 0;
        } else if (sh === 1) {
            this.mem.write16(addr, this.r[rt]);
        } else {
            this.unimplementedCount++;
        }
        if (P && W && rn !== 15) this.r[rn] = addr | 0;
        if (!P && rn !== 15) this.r[rn] = addr | 0;
        return true;
    };

    /**
     * LDRD / STRD. Rt2 is *implicit* in the immediate form (bit 22 set): the
     * architecture takes it as Rt+1 and bits[11:8] are the high half of the
     * offset. Only the register form (bit 22 clear) puts Rt2 in bits[11:8].
     * keystone confirms this: `ldrd r0, r1, [r2, #8]` is 0xE1C200D8, where
     * bits[3:0] == 8 is the offset's low half, not a register number.
     */
    ArmCpu.prototype.execDoubleword = function (insn, pc, addr, isLoad) {
        var rt = (insn >>> 12) & 0xF;
        var rt2 = (insn & (1 << 22)) ? ((rt + 1) & 0xF) : ((insn >>> 8) & 0xF);
        if (rt === 15 || rt2 === 15 || rt === rt2) {
            this.unimplementedCount++;
            this.halt("ldrstrd-unpredictable-r" + rt + "-r" + rt2, pc);
            return false;
        }
        if (isLoad) {
            this.r[rt] = this.mem.read32(addr) | 0;
            this.r[rt2] = this.mem.read32((addr + 4) >>> 0) | 0;
        } else {
            this.mem.write32(addr, this.r[rt]);
            this.mem.write32((addr + 4) >>> 0, this.r[rt2]);
        }
        var rn = (insn >>> 16) & 0xF;
        var W = (insn >>> 21) & 1, P = (insn >>> 24) & 1;
        if (P && W && rn !== 15) this.r[rn] = addr | 0;         // pre-indexed writeback
        if (!P && rn !== 15) this.r[rn] = addr | 0;             // post-indexed
        return true;
    };

    /** SWP / SWPB: Rt = [Rn]; [Rn] = Rt2. Single-copy atomic on real hardware. */
    ArmCpu.prototype.execSwap = function (insn, pc) {
        var rn = (insn >>> 16) & 0xF;
        var rt = (insn >>> 12) & 0xF;
        var rt2 = insn & 0xF;
        var byte = (insn >>> 22) & 1;
        var addr = this.get(rn) >>> 0;
        var loaded = byte ? (this.mem.read8(addr) & 0xFF) : (this.mem.read32(addr) >>> 0);
        if (byte) this.mem.write8(addr, this.r[rt2]);
        else this.mem.write32(addr, this.r[rt2]);
        this.r[rt] = loaded | 0;
        return true;
    };

    ArmCpu.prototype.execLdmStm = function (insn, pc) {
        var rn = (insn >>> 16) & 0xF;
        var L = (insn >>> 20) & 1;
        var W = (insn >>> 21) & 1;
        var U = (insn >>> 23) & 1;
        var P = (insn >>> 24) & 1;
        var S = (insn >>> 22) & 1;
        var list = insn & 0xFFFF;
        var n = 0;
        for (var i = 0; i < 16; i++) if (list & (1 << i)) n++;
        if (n === 0) { this.halt("unimplemented-ldm-empty", pc); return false; }

        var base = this.get(rn) >>> 0;
        // IA: start = base            IB: start = base + 4
        // DA: start = base - 4n + 4   DB: start = base - 4n
        var start, wb;
        if (U) {
            start = P ? (base + 4) >>> 0 : base;
            wb = (base + n * 4) >>> 0;
        } else {
            start = P ? (base - n * 4) >>> 0 : (base - n * 4 + 4) >>> 0;
            wb = (base - n * 4) >>> 0;
        }

        var cur = start;
        var loaded15 = false;
        for (var j = 0; j < 16; j++) {
            if (!(list & (1 << j))) continue;
            if (L) {
                var v = this.mem.read32(cur >>> 0);
                if (j === 15) {
                    loaded15 = true;
                    if (L && S) {
                        // `ldm..., {..., pc}^` -- the exception return form. CPSR
                        // comes from SPSR, not from bit 0 of the loaded value, so
                        // the Thumb-bit juggling below must not run.
                        this.exceptionReturn(v & ~1);
                    } else {
                        this.r[15] = v & ~1;
                        if (v & 1) this.cpsr |= 0x20; else this.cpsr &= ~0x20;
                        this._branched = true;
                    }
                }
                else this.r[j] = v | 0;
            } else {
                this.mem.write32(cur >>> 0, j === 15 ? (pc + 8) : this.r[j]);
            }
            cur = (cur + 4) >>> 0;
        }
        if (W && rn !== 15 && !(L && loaded15 && (list & (1 << rn)))) {
            this.r[rn] = wb | 0;
        }
        return true;
    };

    // -- Thumb mode -----------------------------------------------------
    //
    // Dispatch is on bits[15:11], which partitions the 16-bit encodings without
    // overlap. An earlier version used a pile of hand-written masks that
    // overlapped (e.g. 0xF800/0x1800 matched both ADD-register and part of the
    // immediate group), so the wrong handler ran depending on test order.

    ArmCpu.prototype.stepThumb = function (pc) {
        var hwF = this.mem.fetch16(pc);
        if (hwF === null) return;
        var hw = hwF & 0xFFFF;
        var top5 = hw >>> 11;
        var r = this.r;
        var self = this;

        function nz(v) { self.setFlagsNZ(v >>> 0); }
        function addFlags(a, b, res) {
            nz(res);
            self.setC((a + b) > 0xFFFFFFFF ? 1 : 0);
            self.setV(((~(a ^ b) & (a ^ res)) >>> 31) & 1);
        }
        function subFlags(a, b, res) {
            nz(res);
            self.setC(a >= b ? 1 : 0);
            self.setV((((a ^ b) & (a ^ res)) >>> 31) & 1);
        }

        switch (top5) {

        case 0x1E: {                                    // BL / BLX (32-bit)
            var hw2F = this.mem.fetch16((pc + 2) >>> 0);
            if (hw2F === null) return;
            var hw2 = hw2F & 0xFFFF;
            var S = (hw >>> 10) & 1;
            var imm10 = hw & 0x3FF;
            var J1 = (hw2 >>> 13) & 1, J2 = (hw2 >>> 11) & 1;
            var imm11 = hw2 & 0x7FF;
            var I1 = (J1 ^ S ^ 1) & 1, I2 = (J2 ^ S ^ 1) & 1;
            var off = ((S << 24) | (I1 << 23) | (I2 << 22) | (imm10 << 12) | (imm11 << 1));
            if (S) off -= (1 << 25);
            var target = (pc + 4 + off) >>> 0;
            r[14] = ((pc + 4) | 1) | 0;
            // second half bit 12: 1 = BL (stay in Thumb), 0 = BLX (switch to ARM)
            this.branchTo((hw2 & 0x1000) ? (target | 1) >>> 0 : (target & ~0x3) >>> 0);
            return;
        }

        case 0x00: case 0x01: case 0x02: {              // LSL / LSR / ASR imm5
            var imm5 = (hw >>> 6) & 0x1F;
            var rm = (hw >>> 3) & 7, rd = hw & 7;
            var sh = shiftImm(top5, r[rm] >>> 0, imm5, this.getC());
            r[rd] = sh.v | 0;
            nz(sh.v); this.setC(sh.c);
            return;
        }

        case 0x03: {                                    // ADD / SUB register or imm3
            var isSub = (hw >>> 9) & 1;
            var rn = (hw >>> 3) & 7, rd3 = hw & 7;
            var a = r[rn] >>> 0;
            if ((hw >>> 10) & 1) {                      // 3-bit immediate
                var imm3 = (hw >>> 6) & 7;
                var o3 = isSub ? (a - imm3) >>> 0 : (a + imm3) >>> 0;
                r[rd3] = o3 | 0;
                if (isSub) subFlags(a, imm3, o3); else addFlags(a, imm3, o3);
            } else {                                    // register
                var b = r[(hw >>> 6) & 7] >>> 0;
                var o = isSub ? (a - b) >>> 0 : (a + b) >>> 0;
                r[rd3] = o | 0;
                if (isSub) subFlags(a, b, o); else addFlags(a, b, o);
            }
            return;
        }

        case 0x04: case 0x05: case 0x06: case 0x07: {   // MOV / CMP / ADD / SUB imm8
            var rd8 = (hw >>> 8) & 7, imm8 = hw & 0xFF;
            var x = r[rd8] >>> 0;
            if (top5 === 0x04) { r[rd8] = imm8; nz(imm8); return; }
            if (top5 === 0x05) { subFlags(x, imm8, (x - imm8) >>> 0); return; }
            if (top5 === 0x06) { var s6 = (x + imm8) >>> 0; r[rd8] = s6 | 0; addFlags(x, imm8, s6); return; }
            var d7 = (x - imm8) >>> 0; r[rd8] = d7 | 0; subFlags(x, imm8, d7);
            return;
        }

        case 0x08: {                                    // data-proc reg / special / BX
            if (((hw >>> 10) & 1) === 0) return this.tDataProc(hw);
            // 0100 01 op D Rm Rdn : op is bits[9:8] (two bits) and bit 7 is D,
            // the high bit of the destination register. Taking four bits here
            // folds D into the opcode and every "MOV (high registers)" decodes
            // as opcode 8 instead of 2.
            var op = (hw >>> 8) & 0x3;
            if (op === 0x0) {                           // ADD high registers
                var dn = (hw >>> 7) & 1;
                var rdh = (dn << 3) | (hw & 7);
                var sum = ((r[rdh] >>> 0) + (r[(hw >>> 3) & 0xF] >>> 0)) >>> 0;
                if (rdh === 15) { this.r[15] = ((sum + (this.isThumb() ? 2 : 0)) & ~1) | 0; this._branched = true; }
                else r[rdh] = sum | 0;
                return;
            }
            if (op === 0x1) {                           // CMP high registers
                var dn2 = (hw >>> 7) & 1;
                var a2 = r[(dn2 << 3) | (hw & 7)] >>> 0;
                var b2 = r[(hw >>> 3) & 0xF] >>> 0;
                subFlags(a2, b2, (a2 - b2) >>> 0);
                return;
            }
            if (op === 0x2) {                           // MOV high registers
                var dn3 = (hw >>> 7) & 1;
                var rd3h = (dn3 << 3) | (hw & 7);
                var v3 = r[(hw >>> 3) & 0xF];
                if (rd3h === 15) { this.r[15] = (v3 & ~1) | 0; if (!(v3 & 1)) this.cpsr &= ~0x20; this._branched = true; }
                else r[rd3h] = v3;
                return;
            }
            if (op === 0x3) {                           // BX / BLX register
                var link = (hw >>> 7) & 1;
                if (link) r[14] = ((pc + 4) | 1) | 0;
                this.branchTo(r[(hw >>> 3) & 0xF] >>> 0);
                return;
            }
            break;
        }

        case 0x09: {                                    // LDR Rt, [PC, #imm8*4]
            var rt9 = (hw >>> 8) & 7;
            var a9 = ((((pc + 4) >>> 0) & ~3) + ((hw & 0xFF) << 2)) >>> 0;
            r[rt9] = this.mem.read32(a9) | 0;
            return;
        }

        case 0x0A: case 0x0B:                           // load/store, register offset
            return this.tLdStReg(hw);

        case 0x0C: case 0x0D:                           // LDR/STR imm5 (word)
        case 0x0E: case 0x0F: {                         // LDRB/STRB imm5
            var byteOp = top5 >= 0x0E;
            var isLoadL = top5 & 1;
            var imm5L = (hw >>> 6) & 0x1F;
            var rnL = (hw >>> 3) & 7, rtL = hw & 7;
            var addrL = ((r[rnL] >>> 0) + (imm5L << (byteOp ? 0 : 2))) >>> 0;
            if (isLoadL) r[rtL] = (byteOp ? this.mem.read8(addrL) : this.mem.read32(addrL)) | 0;
            else if (byteOp) this.mem.write8(addrL, r[rtL]);
            else this.mem.write32(addrL, r[rtL]);
            return;
        }

        case 0x10: case 0x11: {                         // LDRH/STRH imm5
            var addrH = ((r[(hw >>> 3) & 7] >>> 0) + (((hw >>> 6) & 0x1F) << 1)) >>> 0;
            if (top5 === 0x11) r[hw & 7] = this.mem.read16(addrH) | 0;
            else this.mem.write16(addrH, r[hw & 7]);
            return;
        }

        case 0x12: case 0x13: {                         // LDR/STR SP-relative
            var addrS = ((r[13] >>> 0) + ((hw & 0xFF) << 2)) >>> 0;
            if (top5 === 0x13) r[(hw >>> 8) & 7] = this.mem.read32(addrS) | 0;
            else this.mem.write32(addrS, r[(hw >>> 8) & 7]);
            return;
        }

        case 0x14: case 0x15: {                         // ADD Rd, PC/SP, #imm8*4
            var baseA = (top5 === 0x14) ? ((((pc + 4) >>> 0) & ~3)) : (r[13] >>> 0);
            r[(hw >>> 8) & 7] = (baseA + ((hw & 0xFF) << 2)) | 0;
            return;
        }

        case 0x16: case 0x17:                           // misc: ADD SP, PUSH, POP
            return this.tMisc(hw, pc);

        case 0x18: case 0x19: {                         // STMIA / LDMIA
            var rbM = (hw >>> 8) & 7;
            var listM = hw & 0xFF;
            var curM = r[rbM] >>> 0;
            var isLdm = top5 === 0x19;
            for (var q = 0; q < 8; q++) {
                if (!(listM & (1 << q))) continue;
                if (isLdm) r[q] = this.mem.read32(curM) | 0;
                else this.mem.write32(curM, r[q]);
                curM = (curM + 4) >>> 0;
            }
            if (!(listM & (1 << rbM))) r[rbM] = curM | 0;
            return;
        }

        case 0x1A: case 0x1B: {                         // conditional branch / SVC
            var cond = (hw >>> 8) & 0xF;
            if (cond === 0xE) { this.halt("insn-invalid", pc); return; }
            if (cond === 0xF) { this.syscallLog.push({ pc: pc, imm: hw & 0xFF }); return; }
            if (this.checkCond(cond)) {
                var offC = hw & 0xFF;
                if (offC & 0x80) offC -= 0x100;
                this.branchTo(((pc + 4 + (offC << 1)) | 1) >>> 0);
            }
            return;
        }

        case 0x1C: {                                    // B imm11
            var offB = hw & 0x7FF;
            if (offB & 0x400) offB -= 0x800;
            this.branchTo(((pc + 4 + (offB << 1)) | 1) >>> 0);
            return;
        }

        default: break;
        }

        this.unimplementedCount++;
        this.halt("unimplemented-thumb-0x" + hw.toString(16), pc);
    };

    ArmCpu.prototype.tDataProc = function (hw) {
        var op = (hw >>> 6) & 0xF;
        var rm = (hw >>> 3) & 7, rdn = hw & 7;
        var x = this.r[rdn] >>> 0, y = this.r[rm] >>> 0, res;
        switch (op) {
            case 0x0: res = (x & y) >>> 0; break;
            case 0x1: res = (x ^ y) >>> 0; break;
            case 0x2: res = shiftImm(0, x, y & 0xFF, this.getC()).v; break;
            case 0x3: res = shiftImm(1, x, y & 0xFF, this.getC()).v; break;
            case 0x4: res = shiftImm(2, x, y & 0xFF, this.getC()).v; break;
            case 0x5: res = (x + y + this.getC()) >>> 0; break;
            case 0x6: res = (x - y) >>> 0; break;
            case 0x7: res = ((x << (y & 0xFF)) | ((this.getC() ? 1 : 0) >>> (32 - (y & 0xFF)))) >>> 0; break; // ROR
            case 0x8: res = (x + y) >>> 0; break;
            case 0x9: res = (x - y) >>> 0; break;
            case 0xA: this.setFlagsNZ((x - y) >>> 0); this.setC(x >= y ? 1 : 0);
                      this.setV((((x ^ y) & (x ^ ((x - y) >>> 0))) >>> 31) & 1); return;
            case 0xB: this.setFlagsNZ((x + y) >>> 0); this.setC((x + y) > 0xFFFFFFFF ? 1 : 0);
                      this.setV(((~(x ^ y) & (x ^ ((x + y) >>> 0))) >>> 31) & 1); return;
            case 0xC: res = (x | y) >>> 0; break;
            case 0xD: res = Math.imul(x | 0, y | 0) >>> 0; break;
            case 0xE: res = (x & (~y >>> 0)) >>> 0; break;
            case 0xF: res = (~y) >>> 0; break;
            default: this.unimplementedCount++; this.halt("unimplemented-thumb-dp-" + op, this.r[15] >>> 0); return;
        }
        this.r[rdn] = res | 0;
        if (op <= 0x1 || op >= 0xC) this.setFlagsNZ(res);
        else if (op >= 0x2 && op <= 0x4) this.setFlagsNZ(res);
        else if (op === 0x5 || op === 0x8) { this.setFlagsNZ(res); this.setC((x + y) > 0xFFFFFFFF ? 1 : 0); }
        else if (op === 0x6 || op === 0x9) { this.setFlagsNZ(res); this.setC(x >= y ? 1 : 0); }
        else if (op === 0x7) this.setFlagsNZ(res);
    };

    ArmCpu.prototype.tLdStReg = function (hw) {
        var op = (hw >>> 9) & 7;
        var rm = (hw >>> 6) & 7, rn = (hw >>> 3) & 7, rt = hw & 7;
        var base = (this.r[rn] >>> 0) + (this.r[rm] >>> 0);
        var addr;
        switch (op) {
            case 0x0: addr = base >>> 0; this.mem.write32(addr, this.r[rt]); return;              // STR
            case 0x1: addr = base >>> 0; this.mem.write16(addr, this.r[rt]); return;              // STRH
            case 0x2: addr = base >>> 0; this.mem.write8(addr, this.r[rt]); return;               // STRB
            case 0x3: { var v = this.mem.read8(base >>> 0); if (v & 0x80) v |= 0xFFFFFF00;        // LDRSB
                        this.r[rt] = v | 0; return; }
            case 0x4: this.r[rt] = this.mem.read32(base >>> 0) | 0; return;                       // LDR
            case 0x5: this.r[rt] = this.mem.read16(base >>> 0) | 0; return;                       // LDRH
            case 0x6: this.r[rt] = this.mem.read8(base >>> 0) | 0; return;                        // LDRB
            case 0x7: { var w = this.mem.read16(base >>> 0); if (w & 0x8000) w |= 0xFFFF0000;     // LDRSH
                        this.r[rt] = w | 0; return; }
            default: break;
        }
        this.unimplementedCount++;
        this.halt("unimplemented-thumb-ldst-" + op, this.r[15] >>> 0);
    };

    ArmCpu.prototype.tMisc = function (hw, pc) {
        var r = this.r;

        // ADD/SUB SP, #imm7*4 : 1011 0000 x imm7
        if ((hw & 0xFF00) === 0xB000) {
            var imm7 = (hw & 0x7F) << 2;
            r[13] = ((hw & 0x80) ? ((r[13] >>> 0) - imm7) : ((r[13] >>> 0) + imm7)) | 0;
            return;
        }

        // PUSH {regs[, lr]} : 1011 010 M list
        // POP  {regs[, pc]} : 1011 110 P list
        // Bit 11 is what separates them. Testing bits[10:8] instead (as an
        // earlier revision did) makes `pop {r4, pc}` = 0xBD10 land in the PUSH
        // handler, because both encodings give 5 there.
        if ((hw & 0xFE00) === 0xB400) {
            var listP = hw & 0xFF;
            var lrP = (hw >>> 8) & 1;
            var cntP = lrP;
            for (var i = 0; i < 8; i++) if (listP & (1 << i)) cntP++;
            var spP = ((r[13] >>> 0) - cntP * 4) >>> 0;
            var curP = spP;
            for (var j = 0; j < 8; j++) if (listP & (1 << j)) { this.mem.write32(curP, r[j]); curP = (curP + 4) >>> 0; }
            if (lrP) this.mem.write32(curP, r[14]);
            r[13] = spP | 0;
            return;
        }
        if ((hw & 0xFE00) === 0xBC00) {
            var listQ = hw & 0xFF;
            var pcQ = (hw >>> 8) & 1;
            var curQ = r[13] >>> 0;
            for (var k = 0; k < 8; k++) if (listQ & (1 << k)) { r[k] = this.mem.read32(curQ) | 0; curQ = (curQ + 4) >>> 0; }
            if (pcQ) {
                var vq = this.mem.read32(curQ) >>> 0;
                curQ = (curQ + 4) >>> 0;
                this.branchTo(vq);
            }
            r[13] = curQ | 0;
            return;
        }

        this.unimplementedCount++;
        this.halt("unimplemented-thumb-misc-0x" + hw.toString(16), pc);
    };

    A5.ArmCpu = ArmCpu;
    A5.shiftImm = shiftImm;
    A5.shiftReg = shiftReg;

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
