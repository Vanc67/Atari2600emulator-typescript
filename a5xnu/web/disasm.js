// Minimal ARM/Thumb disassembler for the trace view. It covers the encodings
// the interpreter implements, which is enough to watch the kernel entry
// sequence execute instruction by instruction. Anything unrecognised is shown
// as .long/.word so a gap is visible rather than silently wrong.

var __g2 = (typeof globalThis !== "undefined") ? globalThis
         : (typeof window !== "undefined") ? window
         : (typeof global !== "undefined") ? global : this;
var A5 = __g2.A5 || (__g2.A5 = {});

(function (A5) {
    "use strict";

    var COND = ["eq", "ne", "cs", "cc", "mi", "pl", "vs", "vc",
                "hi", "ls", "ge", "lt", "gt", "le", "", ""];
    var DP = ["and", "eor", "sub", "rsb", "add", "adc", "sbc", "rsc",
              "tst", "teq", "cmp", "cmn", "orr", "mov", "bic", "mvn"];
    var SHIFT = ["lsl", "lsr", "asr", "ror"];
    var REG = ["r0", "r1", "r2", "r3", "r4", "r5", "r6", "r7",
               "r8", "r9", "sl", "fp", "ip", "sp", "lr", "pc"];

    function hex(v) { return "0x" + (v >>> 0).toString(16); }

    function rotImm(imm8, rot) {
        var r = (rot * 2) & 31;
        return r === 0 ? imm8 >>> 0 : ((imm8 >>> r) | (imm8 << (32 - r))) >>> 0;
    }

    function regList(list) {
        var out = [];
        for (var i = 0; i < 16; i++) if (list & (1 << i)) out.push(REG[i]);
        return "{" + out.join(", ") + "}";
    }

    function shifterOperand(insn) {
        if (insn & (1 << 25)) {
            var rot = (insn >>> 8) & 0xF, imm8 = insn & 0xFF;
            return "#" + hex(rotImm(imm8, rot));
        }
        var rm = REG[insn & 0xF];
        if (insn & (1 << 4)) {
            return rm + ", " + SHIFT[(insn >>> 5) & 3] + " " + REG[(insn >>> 8) & 0xF];
        }
        var imm5 = (insn >>> 7) & 0x1F, type = (insn >>> 5) & 3;
        if (type === 0 && imm5 === 0) return rm;
        var amt = imm5 === 0 ? ((type === 1 || type === 2) ? 32 : 0) : imm5;
        if (type === 3 && amt === 0) return rm + ", rrx";
        return rm + ", " + SHIFT[type] + " #" + amt;
    }

    A5.disasmArm = function (insn, addr) {
        insn >>>= 0;
        var c = COND[(insn >>> 28) & 0xF];

        if ((insn & 0x0F000010) === 0x0E000010 && ((insn >>> 8) & 0xF) === 15) {
            var opc1 = (insn >>> 21) & 7, L = (insn >>> 20) & 1;
            var crn = (insn >>> 16) & 0xF, rt = (insn >>> 12) & 0xF;
            var opc2 = (insn >>> 5) & 7, crm = insn & 0xF;
            return (L ? "mrc" : "mcr") + c + " p15, #" + opc1 + ", " + REG[rt] +
                   ", c" + crn + ", c" + crm + ", #" + opc2;
        }
        if ((insn & 0x0F000000) === 0x0F000000) return "svc" + c + " #" + hex(insn & 0xFFFFFF);
        if ((insn & 0x0FF000F0) === 0x07F000F0) return "udf #" + hex(insn & 0xFFF);
        if ((insn & 0x0FFFFFF0) === 0x012FFF10) return "bx" + c + " " + REG[insn & 0xF];
        if ((insn & 0x0FFFFFF0) === 0x012FFF30) return "blx" + c + " " + REG[insn & 0xF];
        if ((insn & 0x0FB0FFF0) === 0x010F0000) {
            return "mrs" + c + " " + REG[(insn >>> 12) & 0xF] +
                   ", " + (((insn >>> 22) & 1) ? "spsr" : "cpsr");
        }
        if ((insn & 0x0DB0F000) === 0x0120F000) {
            return "msr" + c + " " + (((insn >>> 22) & 1) ? "spsr" : "cpsr") + ", ...";
        }

        if ((insn & 0x0E000000) === 0x0A000000) {
            var off = insn & 0xFFFFFF;
            if (off & 0x800000) off -= 0x1000000;
            var tgt = (addr + 8 + (off << 2)) >>> 0;
            if (((insn >>> 28) & 0xF) === 0xF) {
                tgt = (tgt + (((insn >>> 24) & 1) << 1)) >>> 0;
                return "blx " + hex(tgt);
            }
            return (((insn >>> 24) & 1) ? "bl" : "b") + c + " " + hex(tgt);
        }

        if ((insn & 0x0E000090) === 0x00000090) {
            var op = (insn >>> 21) & 7;
            if (((insn >>> 4) & 0xF) === 9 && (op & 4) === 0) {
                var mn = ["mul", "mla", "?", "?", "umull", "umlal", "smull", "smlal"][op];
                return mn + c + " " + REG[(insn >>> 16) & 0xF] + ", " + REG[(insn >>> 12) & 0xF] +
                       ", " + REG[insn & 0xF] + ", " + REG[(insn >>> 8) & 0xF];
            }
        }

        if ((insn & 0x0E000000) === 0x08000000) {
            var isL = (insn >>> 20) & 1;
            var mode = ((insn >>> 24) & 1) ? (((insn >>> 23) & 1) ? "ib" : "db")
                                           : (((insn >>> 23) & 1) ? "ia" : "da");
            return (isL ? "ldm" : "stm") + c + mode + " " + REG[(insn >>> 16) & 0xF] +
                   ((insn >>> 21) & 1 ? "!" : "") + ", " + regList(insn & 0xFFFF);
        }

        if ((insn & 0x0C000000) === 0x04000000) {
            var L2 = (insn >>> 20) & 1, B = (insn >>> 22) & 1;
            var P = (insn >>> 24) & 1, U = (insn >>> 23) & 1, W = (insn >>> 21) & 1;
            var rn = REG[(insn >>> 16) & 0xF], rt = REG[(insn >>> 12) & 0xF];
            var mn2 = (L2 ? "ldr" : "str") + c + (B ? "b" : "");
            var offStr;
            if (insn & (1 << 25)) {
                var rmv = REG[insn & 0xF];
                var sh = (insn >>> 7) & 0x1F, ty = (insn >>> 5) & 3;
                offStr = (U ? "" : "-") + rmv + ((sh || ty) ? ", " + SHIFT[ty] + " #" + sh : "");
            } else {
                var imm = insn & 0xFFF;
                offStr = (imm === 0) ? "" : ((U ? "#" : "#-") + imm);
            }
            if (P) {
                return mn2 + " " + rt + ", [" + rn + (offStr ? ", " + offStr : "") + "]" +
                       (W ? "!" : "");
            }
            return mn2 + " " + rt + ", [" + rn + "], " + (offStr || "#0");
        }

        if ((insn & 0x0FB00000) === 0x03000000) {
            var imm16 = (((insn >>> 16) & 0xF) << 12) | (insn & 0xFFF);
            return ((insn >>> 22) & 1 ? "movt" : "movw") + c + " " +
                   REG[(insn >>> 12) & 0xF] + ", #" + hex(imm16);
        }

        if ((insn & 0x0C000000) === 0x00000000) {
            var op2 = (insn >>> 21) & 0xF, S = (insn >>> 20) & 1;
            var rd = REG[(insn >>> 12) & 0xF], rnn = REG[(insn >>> 16) & 0xF];
            // TST/TEQ/CMP/CMN always set flags, so the "s" suffix is redundant
            // (and capstone does not print it).
            var implicitS = (op2 >= 0x8 && op2 <= 0xB);
            var name = DP[op2] + c + ((S && !implicitS) ? "s" : "");
            if (op2 === 0xD || op2 === 0xF) return name + " " + rd + ", " + shifterOperand(insn);
            if (op2 >= 0x8 && op2 <= 0xB) return name + " " + rnn + ", " + shifterOperand(insn);
            return name + " " + rd + ", " + rnn + ", " + shifterOperand(insn);
        }

        return ".long " + hex(insn);
    };

    A5.disasmThumb = function (hw, addr, next) {
        hw &= 0xFFFF;
        var top5 = hw >>> 11;
        var rd = hw & 7, rn = (hw >>> 3) & 7;
        switch (top5) {
            case 0x00: return "lsls " + REG[rd] + ", " + REG[rn] + ", #" + ((hw >>> 6) & 0x1F);
            case 0x01: return "lsrs " + REG[rd] + ", " + REG[rn] + ", #" + (((hw >>> 6) & 0x1F) || 32);
            case 0x02: return "asrs " + REG[rd] + ", " + REG[rn] + ", #" + (((hw >>> 6) & 0x1F) || 32);
            case 0x03: {
                var sub = (hw >>> 9) & 1;
                if ((hw >>> 10) & 1) return (sub ? "subs " : "adds ") + REG[rd] + ", " + REG[rn] + ", #" + ((hw >>> 6) & 7);
                return (sub ? "subs " : "adds ") + REG[rd] + ", " + REG[rn] + ", " + REG[(hw >>> 6) & 7];
            }
            case 0x04: return "movs " + REG[(hw >>> 8) & 7] + ", #" + (hw & 0xFF);
            case 0x05: return "cmp " + REG[(hw >>> 8) & 7] + ", #" + (hw & 0xFF);
            case 0x06: return "adds " + REG[(hw >>> 8) & 7] + ", #" + (hw & 0xFF);
            case 0x07: return "subs " + REG[(hw >>> 8) & 7] + ", #" + (hw & 0xFF);
            case 0x08: {
                if (((hw >>> 10) & 1) === 0) return ".word " + hex(hw) + "  ; thumb data-proc";
                var op = (hw >>> 8) & 3;
                var dn = (hw >>> 7) & 1, rdn = REG[(dn << 3) | (hw & 7)], rm = REG[(hw >>> 3) & 0xF];
                if (op === 0) return "add " + rdn + ", " + rm;
                if (op === 1) return "cmp " + rdn + ", " + rm;
                if (op === 2) return "mov " + rdn + ", " + rm;
                return ((hw & 0x80) ? "blx " : "bx ") + rm;
            }
            case 0x09: return "ldr " + REG[(hw >>> 8) & 7] + ", [pc, #" + ((hw & 0xFF) << 2) + "]";
            case 0x12: return "str " + REG[(hw >>> 8) & 7] + ", [sp, #" + ((hw & 0xFF) << 2) + "]";
            case 0x13: return "ldr " + REG[(hw >>> 8) & 7] + ", [sp, #" + ((hw & 0xFF) << 2) + "]";
            case 0x14: return "add " + REG[(hw >>> 8) & 7] + ", pc, #" + ((hw & 0xFF) << 2);
            case 0x15: return "add " + REG[(hw >>> 8) & 7] + ", sp, #" + ((hw & 0xFF) << 2);
            case 0x18: return "stmia " + REG[(hw >>> 8) & 7] + "!, " + regList(hw & 0xFF);
            case 0x19: return "ldmia " + REG[(hw >>> 8) & 7] + "!, " + regList(hw & 0xFF);
            case 0x1C: {
                var ob = hw & 0x7FF; if (ob & 0x400) ob -= 0x800;
                return "b " + hex((addr + 4 + (ob << 1)) >>> 0);
            }
            default: break;
        }
        if (top5 === 0x16 || top5 === 0x17) {
            var bits11_9 = (hw >>> 9) & 7;
            if ((hw & 0xFF00) === 0xB000) return ((hw & 0x80) ? "sub sp, #" : "add sp, #") + ((hw & 0x7F) << 2);
            if ((hw & 0xFE00) === 0xB400) return "push " + regList((hw & 0xFF) | (((hw >>> 8) & 1) << 14));
            if ((hw & 0xFE00) === 0xBC00) return "pop " + regList((hw & 0xFF) | (((hw >>> 8) & 1) << 15));
            return ".word " + hex(hw) + "  ; thumb misc " + bits11_9;
        }
        if (top5 === 0x1A || top5 === 0x1B) {
            var cc = (hw >>> 8) & 0xF;
            if (cc === 0xF) return "svc #" + (hw & 0xFF);
            var oc = hw & 0xFF; if (oc & 0x80) oc -= 0x100;
            return "b" + COND[cc] + " " + hex((addr + 4 + (oc << 1)) >>> 0);
        }
        if ((hw & 0xF800) === 0xF000 && next !== undefined) return ".word bl/blx 32-bit";
        return ".word " + hex(hw);
    };

    A5.disasm = function (bus, addr, thumb) {
        if (thumb) return A5.disasmThumb(bus.read16(addr), addr);
        return A5.disasmArm(bus.read32(addr), addr);
    };

})(A5);

if (typeof module !== "undefined" && module.exports) module.exports = A5;
