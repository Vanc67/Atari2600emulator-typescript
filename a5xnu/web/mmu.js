// ARMv7-A short-descriptor MMU.
//
// Until this existed the bus was reached with whatever address the instruction
// carried, and a5.js faked the kernel's virtual address space by backing a
// 2 GB window that happened to cover both PA 0x40000000.. and VA 0x80000000..
// That is an alias, not a translation: it cannot fault, cannot enforce
// permissions, and would happily let code touch VA 0xC0000000 which the boot
// page table deliberately leaves unmapped.
//
// This walks the real tables that XNU's start() builds -- L1 section and
// coarse-page descriptors, L2 large/small/tiny pages -- checks the domain and
// AP/APX/XN permissions against DACR and the current mode, and reports ARMv7
// fault status codes so the CPU can take a genuine prefetch or data abort.
//
// Unicorn does exactly this natively, which is what makes the post-handoff test
// in tests/test_a5_boot.py a real cross-check rather than a self-consistency
// exercise: two independent implementations of the same walk must agree on
// every translated address and on every fault.

var __g = (typeof globalThis !== "undefined") ? globalThis
        : (typeof window !== "undefined") ? window
        : (typeof global !== "undefined") ? global : this;
var A5 = __g.A5 || (__g.A5 = {});

(function (A5) {
    "use strict";

    // cp15 keys, spelled the way arm.js/arm.js's shadow bank spells them.
    var K_SCTLR = "0,1,0,0";
    var K_TTBR0 = "0,2,0,0";
    var K_TTBR1 = "0,2,0,1";
    var K_TTBCR = "0,2,0,2";
    var K_DACR  = "0,3,0,0";

    // SCTLR bits this walk cares about.
    var M = 1 << 0;      // MMU enable
    var A = 1 << 1;      // alignment check enable
    var C = 1 << 2;      // cacheable override (no cache model, so inert)
    var XP = 1 << 23;    // subpage AP bits disabled (ARMv7 always 1)
    var V  = 1 << 13;    // high exception vectors

    // Fault status codes, ARMv7-A B4.1.31 (DFSR/IFSR share the encoding).
    A5.FSR = {
        ALIGNMENT:       0x01,
        TRANS_LEVEL1:    0x05,
        TRANS_LEVEL2:    0x07,
        ACCESS_LEVEL1:   0x03,
        ACCESS_LEVEL2:   0x06,
        DOMAIN_LEVEL1:   0x09,
        DOMAIN_LEVEL2:   0x0B,
        PERM_LEVEL1:     0x0D,
        PERM_LEVEL2:     0x0F,
        EXTERNAL_LEVEL1: 0x04,
        EXTERNAL_LEVEL2: 0x06
    };

    // Domain access values in DACR, two bits per domain.
    var DOM_NO_ACCESS = 0x0;
    var DOM_CLIENT    = 0x1;
    var DOM_MANAGER   = 0x3;

    function Mmu(cpu, bus) {
        this.cpu = cpu;
        this.bus = bus;
        this.walks = 0;          // number of table walks performed
        this.faults = [];        // bounded log of translation failures
        this.maxFaults = 256;
        // The A5 is a Cortex-A9 and has no unified TLB we would need to model
        // for correctness; walks go straight to memory. A cache would change
        // timing only, and nothing here measures cycles.
    }

    Mmu.prototype.reg = function (key) {
        var v = this.cpu.cp15[key];
        return (v === undefined) ? 0 : v >>> 0;
    };

    Mmu.prototype.sctlr = function () { return this.reg(K_SCTLR); };
    Mmu.prototype.enabled = function () { return (this.sctlr() & M) !== 0; };
    Mmu.prototype.alignCheck = function () { return (this.sctlr() & A) !== 0; };
    Mmu.prototype.highVectors = function () { return (this.sctlr() & V) !== 0; };

    // TTBCR.N selects how many top VA bits route to TTBR1. N == 0 means
    // TTBR0 covers the whole space and TTBR1 is unused -- which is what XNU
    // leaves it at (it writes the same value to both anyway).
    Mmu.prototype.baseFor = function (va) {
        var n = this.reg(K_TTBCR) & 0x7;
        if (n === 0) return { base: this.reg(K_TTBR0) & 0xFFFFC000, mask: 0xFFFFC000 };
        var allOnes = (1 << n) - 1;
        var useTtb1 = ((va >>> (32 - n)) & 0xFFFFFFFF) === allOnes;
        var base = useTtb1 ? this.reg(K_TTBR1) : this.reg(K_TTBR0);
        var mask = (0xFFFFFFFF << (14 - n)) >>> 0;
        return { base: (base & mask) >>> 0, mask: mask };
    };

    Mmu.prototype._logFault = function (va, kind, fsr, domain) {
        if (this.faults.length < this.maxFaults) {
            this.faults.push({ va: va >>> 0, kind: kind, fsr: fsr, domain: domain });
        }
    };

    function fail(fsr, level, domain) {
        return { ok: false, fsr: fsr, level: level, domain: domain };
    }

    // Permission check for one access, ARMv7-A B3.7.1 Table B3-8.
    //
    // ARMv7-A always behaves as if SCTLR.XP == 1, so the three permission bits
    // form a single field AP[2:0] = {APX, AP[1], AP[0]} and the subpage AP0..AP3
    // groups of large/small pages collapse onto the subpage being touched.
    //
    // This matters for the k93ap descriptor: 0x40E sets bit 10 only, so
    // AP[2:0] = 0b001, which is read/write at *both* levels. Reading it as
    // "AP_WRITE|AP_READ" (both bits 10 and 11) would give 0b010 and silently
    // make the kernel's own mappings user-accessible, which is not what
    // Linux's PMD_SECT_AP_WRITE / PMD_SECT_AP_READ names suggest at a glance.
    Mmu.prototype._perm = function (ap, apX, kind, privileged, xn) {
        if (kind === "exec" && xn) return false;

        var ap210 = ((apX & 1) << 2) | (ap & 3);
        var privWrite, userAccess, userWrite;
        switch (ap210) {
            case 0x0: privWrite = true;  userAccess = false; userWrite = false; break;
            case 0x1: privWrite = true;  userAccess = true;  userWrite = true;  break;
            case 0x2: privWrite = true;  userAccess = true;  userWrite = false; break;
            case 0x3: privWrite = false; userAccess = true;  userWrite = false; break;
            case 0x5: privWrite = false; userAccess = true;  userWrite = false; break;
            case 0x6: privWrite = false; userAccess = true;  userWrite = false; break;
            case 0x7: privWrite = false; userAccess = true;  userWrite = false; break;
            default:  return false;                       // 0x4 is reserved
        }

        if (privileged) return kind !== "write" || privWrite;
        if (!userAccess) return false;
        return kind !== "write" || userWrite;
    };

    // Walk the tables.
    //   kind: "read" | "write" | "exec"
    // Returns {ok:true, pa:..} or {ok:false, fsr:.., level:.., domain:..}.
    Mmu.prototype.translate = function (va, kind) {
        va >>>= 0;
        if (!this.enabled()) return { ok: true, pa: va };
        this.walks++;

        var cpu = this.cpu;
        var privileged = cpu.privileged();
        var b = this.baseFor(va);
        var l1idx = va >>> 20;
        var l1desc = this.bus.read32((b.base + l1idx * 4) >>> 0) >>> 0;
        var type = l1desc & 0x3;
        var domain = (l1desc >>> 5) & 0xF;

        // DACR is consulted before any AP check, and a domain that is not
        // "client" or "manager" faults without looking at permissions.
        var dacr = this.reg(K_DACR);
        var dacc = (dacr >>> (domain * 2)) & 0x3;
        if (dacc !== DOM_CLIENT && dacc !== DOM_MANAGER) {
            return fail(A5.FSR.DOMAIN_LEVEL1, 1, domain);
        }
        var checkAp = (dacc === DOM_CLIENT);

        var pa, ap, apX, xn;

        if (type === 0x0) {
            this._logFault(va, kind, A5.FSR.TRANS_LEVEL1, domain);
            return fail(A5.FSR.TRANS_LEVEL1, 1, domain);       // fault
        } else if (type === 0x2) {
            // Section (1 MB), or supersection (16 MB) when bit 18 is set.
            if (l1desc & (1 << 18)) {
                pa = ((l1desc & 0xFF000000) | (va & 0x00FFFFFF)) >>> 0;
            } else {
                pa = ((l1desc & 0xFFF00000) | (va & 0x000FFFFF)) >>> 0;
            }
            ap  = (l1desc >>> 10) & 0x3;
            apX = (l1desc >>> 15) & 0x1;
            xn  = (l1desc >>> 4) & 0x1;
            var level = 1;
            if (checkAp && !this._perm(ap, apX, kind, privileged, xn)) {
                return fail(A5.FSR.PERM_LEVEL1, level, domain);
            }
            return { ok: true, pa: pa };
        } else if (type === 0x1) {
            // Coarse page table: 256 entries of 4 bytes, 1 KB aligned.
            var l2base = l1desc & 0xFFFFFC00;
            var l2idx = (va >>> 12) & 0xFF;
            var l2desc = this.bus.read32((l2base + l2idx * 4) >>> 0) >>> 0;
            var t2 = l2desc & 0x3;

            if (t2 === 0x0) {
                return fail(A5.FSR.TRANS_LEVEL2, 2, domain);
            } else if (t2 === 0x1) {
                // Large page, 64 KB. Four subpage AP fields selected by
                // VA[15:14]. APX is bit 9 (Linux PTE_EXT_APX); bit 0 is taken
                // by the type field, so a large page carries XN at bit 15.
                // Neither Linux nor XNU builds 64 KB pages here, so this branch
                // is defensive rather than load-bearing.
                pa = ((l2desc & 0xFFFF0000) | (va & 0x0000FFFF)) >>> 0;
                var sub = (va >>> 14) & 0x3;
                ap  = (l2desc >>> (4 + sub * 2)) & 0x3;
                apX = (l2desc >>> 9) & 0x1;
                xn  = (l2desc >>> 15) & 0x1;
            } else {
                // Small page, 4 KB. t2 == 0b10 and t2 == 0b11 are the SAME page
                // type: Linux builds a non-executable PTE as
                // PTE_TYPE_SMALL | PTE_EXT_XN == 2 | 1 == 3, so bit 0 is XN and
                // the type field's low bit doubles as the execute-never flag.
                // Reading 0b11 as ARMv6's "tiny page" instead would misplace
                // both the output address and the permissions.
                pa = ((l2desc & 0xFFFFF000) | (va & 0x00000FFF)) >>> 0;
                var sub2 = (va >>> 10) & 0x3;
                ap  = (l2desc >>> (4 + sub2 * 2)) & 0x3;
                apX = (l2desc >>> 9) & 0x1;
                xn  = l2desc & 0x1;
            }
            if (checkAp && !this._perm(ap, apX, kind, privileged, xn)) {
                return fail(A5.FSR.PERM_LEVEL2, 2, domain);
            }
            return { ok: true, pa: pa };
        }

        // type == 0x3 is reserved in the ARMv7 short-descriptor format.
        return fail(A5.FSR.TRANS_LEVEL1, 1, domain);
    };

    A5.Mmu = Mmu;
    A5.MMU_SCTLR = { M: M, A: A, C: C, XP: XP, V: V };

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
