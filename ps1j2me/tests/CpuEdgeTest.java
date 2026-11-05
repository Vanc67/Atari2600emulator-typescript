import psx.Bus;
import psx.Cpu;
import psx.PS1Machine;
import psx.VirtualCD;

/**
 * Edge cases of the R3000A core that real games depend on.
 *
 * Unaligned access (games copy strings and structures with lwl/lwr/swl/swr),
 * division corner cases (the PSX does not trap on them), immediates that must
 * be zero extended rather than sign extended, sign extension of 16 bit loads,
 * the load delay slot, branch delay slots and the exact state an exception
 * leaves behind (EPC, BD flag, ExcCode).
 */
public class CpuEdgeTest {

    static int passed;
    static int failed;

    static final int CODE = 0x80010000;
    static final int DATA = 0x80020000;

    /** Result register used by most programs: $t2. */
    static final int T2 = 10;

    static PS1Machine m;
    static Bus bus;
    static Cpu cpu;

    static void check(String what, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what + (detail == null ? "" : "  " + detail));
        } else {
            failed++;
            System.out.println("FAIL " + what + (detail == null ? "" : "  " + detail));
        }
    }

    static void eq(String what, int expected, int actual) {
        check(what, expected == actual,
                "expected 0x" + Integer.toHexString(expected) + ", got 0x" + Integer.toHexString(actual));
    }

    /* ---- small assembler ------------------------------------------------ */

    static int R(int op, int rs, int rt, int rd, int sa, int fn) {
        return (op << 26) | (rs << 21) | (rt << 16) | (rd << 11) | (sa << 6) | fn;
    }

    static int I(int op, int rs, int rt, int imm) {
        return (op << 26) | (rs << 21) | (rt << 16) | (imm & 0xFFFF);
    }

    static int LUI(int rt, int imm) { return I(15, 0, rt, imm); }
    /** ori rt, zero, imm - a 16 bit constant with the upper half cleared. */
    static int LI(int rt, int imm) { return I(13, 0, rt, imm); }
    /** ori rt, rs, imm. */
    static int ORI(int rt, int rs, int imm) { return I(13, rs, rt, imm); }
    /** A full 32 bit constant. */
    static int[] CONST(int rt, int value) {
        return new int[] { LUI(rt, (value >>> 16) & 0xFFFF), ORI(rt, rt, value & 0xFFFF) };
    }
    static int ADDIU(int rt, int rs, int imm) { return I(9, rs, rt, imm); }

    /** The little-endian word stored at an unaligned address. */
    static int wordAt(int addr) {
        return (bus.read8(addr) & 0xFF) | ((bus.read8(addr + 1) & 0xFF) << 8)
                | ((bus.read8(addr + 2) & 0xFF) << 16) | ((bus.read8(addr + 3) & 0xFF) << 24);
    }
    static int NOP() { return 0; }
    static int SPIN() { return I(4, 0, 0, -1); }        /* beq zero,zero,-1 */

    static int[] concat(int[] a, int[] b) {
        int[] out = new int[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    /** Prefixes a program with "at = DATA". */
    static int[] withBase(int[] body) {
        return concat(concat(CONST(1, DATA), CONST(T2, 0xAAAA5555)), body);
    }

    /**
     * Runs a program and leaves the CPU spinning right after it.
     *
     * The CPU is reset first: the previous program ended inside a branch, so
     * jumping straight into a new one would execute the old delay slot.
     */
    static void run(int[] words) {
        cpu.reset();
        cpu.gpr[29] = 0x801FFF00;
        for (int i = 0; i < words.length; i++) bus.write32(CODE + i * 4, words[i]);
        bus.write32(CODE + words.length * 4, SPIN());
        bus.write32(CODE + words.length * 4 + 4, NOP());
        cpu.pc = CODE;
        cpu.run(4000);
    }

    public static void main(String[] args) {
        m = new PS1Machine(new VirtualCD());
        bus = m.bus;
        cpu = m.cpu;

        /* ---- LWL, all four alignments ---------------------------------- */
        /* the word at DATA is 11 22 33 44; $t2 is seeded with AAAA5555 */
        bus.write32(DATA, 0x11223344);
        int[] expectLwl = { 0x44AA5555, 0x33445555, 0x22334455, 0x11223344 };
        for (int align = 0; align < 4; align++) {
            run(withBase(new int[] {
                I(34, 1, T2, align),                    /* lwl t2, align(at) */
                NOP(),
            }));
            eq("lwl at alignment " + align, expectLwl[align], cpu.gpr[T2]);
        }

        /* ---- LWR, all four alignments ---------------------------------- */
        int[] expectLwr = { 0x11223344, 0xAA112233, 0xAAAA1122, 0xAAAA5511 };
        for (int align = 0; align < 4; align++) {
            run(withBase(new int[] {
                I(38, 1, T2, align),                    /* lwr t2, align(at) */
                NOP(),
            }));
            eq("lwr at alignment " + align, expectLwr[align], cpu.gpr[T2]);
        }

        /* ---- the real unaligned-load idiom ----------------------------- */
        for (int i = 0; i < 8; i++) bus.write8(DATA + i, 0x10 + i * 0x11);
        for (int off = 0; off < 4; off++) {
            run(withBase(new int[] {
                I(34, 1, T2, off + 3),                   /* lwl t2, off+3(at) */
                I(38, 1, T2, off),                       /* lwr t2, off(at)   */
                NOP(),
                NOP(),
            }));
            eq("lwl+lwr loads an unaligned word at +" + off, wordAt(DATA + off), cpu.gpr[T2]);
        }

        /* ---- SWL / SWR write back the same bytes ----------------------- */
        int val = 0x11223344;
        for (int off = 0; off < 4; off++) {
            for (int i = 0; i < 8; i++) bus.write8(DATA + 8 + i, 0);
            run(withBase(concat(CONST(T2, val), new int[] {
                I(42, 1, T2, off + 8 + 3),               /* swl t2, 8+off+3(at) */
                I(46, 1, T2, off + 8),                   /* swr t2, 8+off(at)   */
            })));
            eq("swl+swr stores an unaligned word at +" + off, val, wordAt(DATA + 8 + off));
        }

        /* ---- sign / zero extension ------------------------------------- */
        run(new int[] {
            ADDIU(2, 0, -1),
            I(12, 2, 3, 0xFFFF),                         /* andi t1, v0, FFFF */
            I(13, 0, 4, 0xFFFF),                         /* ori  */
            I(14, 0, 5, 0xFFFF),                         /* xori */
            I(11, 0, 6, 0xFFFF),                         /* sltiu */
            I(10, 0, 7, 0xFFFF),                         /* slti  */
            LI(8, 0x1234), ADDIU(9, 8, -1),
        });
        eq("addiu sign extends", -1, cpu.gpr[2]);
        eq("andi zero extends", 0xFFFF, cpu.gpr[3]);
        eq("ori zero extends", 0xFFFF, cpu.gpr[4]);
        eq("xori zero extends", 0xFFFF, cpu.gpr[5]);
        eq("sltiu compares unsigned", 1, cpu.gpr[6]);
        eq("slti compares signed", 0, cpu.gpr[7]);
        eq("addiu with a negative immediate", 0x1233, cpu.gpr[9]);

        /* ---- loads sign extend, unsigned loads do not ------------------ */
        bus.write16(DATA + 0x40, 0x8001);
        bus.write32(DATA + 0x44, 0x87654321);        /* bytes 21 43 65 87 */
        run(concat(CONST(1, DATA), new int[] {
            I(33, 1, 2, 0x40),                           /* lh  */
            I(37, 1, 3, 0x40),                           /* lhu */
            I(32, 1, 4, 0x47),                           /* lb  */
            I(36, 1, 5, 0x44),                           /* lbu */
        }));
        eq("lh sign extends", 0xFFFF8001, cpu.gpr[2]);
        eq("lhu zero extends", 0x8001, cpu.gpr[3]);
        eq("lb sign extends", 0xFFFFFF87, cpu.gpr[4]);
        eq("lbu zero extends", 0x21, cpu.gpr[5]);

        /* two loads in a row: both values must land (compilers emit this) */
        bus.write32(DATA + 0x70, 0x11111111);
        bus.write32(DATA + 0x74, 0x22222222);
        run(concat(CONST(1, DATA), new int[] {
            I(35, 1, 2, 0x70),                           /* lw v0 */
            I(35, 1, 3, 0x74),                           /* lw v1 (back to back) */
            NOP(),
        }));
        eq("the first of two adjacent loads lands", 0x11111111, cpu.gpr[2]);
        eq("and so does the second", 0x22222222, cpu.gpr[3]);

        /* ---- load delay slot ------------------------------------------- */
        bus.write32(DATA + 0x50, 0x11223344);
        run(concat(concat(CONST(1, DATA), CONST(T2, 0x0BADF00D)), new int[] {
            I(35, 1, T2, 0x50),                          /* lw t2, 0x50(at)   */
            R(0, T2, T2, 3, 0, 0x21),                    /* addu t3,t2,t2: delay slot */
            R(0, T2, 0, 4, 0, 0x21),                     /* addu t4,t2,zero   */
        }));
        eq("a load is not visible in its own delay slot", 0x175BE01A, cpu.gpr[3]);
        eq("but it is visible right after", 0x11223344, cpu.gpr[4]);

        /* ---- division corner cases ------------------------------------- */
        run(new int[] {
            LI(8, 7), LI(9, 0),
            R(0, 8, 9, 0, 0, 0x1A),                      /* div 7, 0 */
        });
        eq("div by zero: lo = -1", -1, cpu.lo);
        eq("div by zero: hi = dividend", 7, cpu.hi);

        run(new int[] {
            ADDIU(8, 0, -7), LI(9, 0),
            R(0, 8, 9, 0, 0, 0x1A),
        });
        eq("div by zero with a negative dividend: lo = 1", 1, cpu.lo);
        eq("div by zero with a negative dividend: hi = dividend", -7, cpu.hi);

        run(new int[] {
            LUI(8, 0x8000), ADDIU(9, 0, -1),
            R(0, 8, 9, 0, 0, 0x1A),                      /* div 0x80000000, -1 */
        });
        eq("div overflow: lo = 0x80000000", 0x80000000, cpu.lo);
        eq("div overflow: hi = 0", 0, cpu.hi);

        run(new int[] {
            LI(8, 100), LI(9, 7),
            R(0, 8, 9, 0, 0, 0x1B),                      /* divu */
        });
        eq("divu 100/7 = 14", 14, cpu.lo);
        eq("divu 100%7 = 2", 2, cpu.hi);

        /* ---- multiply --------------------------------------------------- */
        run(new int[] {
            LI(8, 0x1000), LI(9, 0x1000),
            R(0, 8, 9, 0, 0, 0x18),
            ADDIU(10, 0, -3), LI(11, 5),
            R(0, 10, 11, 0, 0, 0x18),                    /* mult -3*5 */
        });
        eq("a negative product sign extends into hi", -1, cpu.hi);
        eq("a negative product lands in lo", -15, cpu.lo);

        run(new int[] {
            LUI(8, 0x7FFF), ORI(8, 8, 0xFFFF), LI(9, 1),
            R(0, 8, 8, 0, 0, 0x18),                      /* mult max * max */
        });
        eq("mult overflow keeps the low word", 1, cpu.lo);
        eq("mult overflow keeps the high word", 0x3FFFFFFF, cpu.hi);

        run(new int[] {
            ADDIU(8, 0, -1), ADDIU(9, 0, -1),
            R(0, 8, 9, 0, 0, 0x19),                      /* multu 0FFFFFFFFh^2 */
        });
        eq("multu treats the operands as unsigned, lo", 1, cpu.lo);
        eq("multu treats the operands as unsigned, hi", 0xFFFFFFFE, cpu.hi);

        /* ---- shifts mask the amount ------------------------------------ */
        bus.write32(DATA + 0x60, 0x80000003);
        run(concat(CONST(1, DATA), new int[] {
            I(35, 1, 2, 0x60),                           /* lw v0, 0x60(at) */
            NOP(),
            LI(3, 33),
            R(0, 3, 2, 4, 0, 0x04),                      /* sllv a0,v0,a1: value rt, shift rs */
            R(0, 0, 2, 5, 31, 0x03),                     /* sra a1,v0,31  */
            R(0, 0, 2, 6, 31, 0x02),                     /* srl a2,v0,31  */
        }));
        eq("sllv masks the shift amount", 6, cpu.gpr[4]);
        eq("sra duplicates the sign bit", -1, cpu.gpr[5]);
        eq("srl shifts in zeros", 1, cpu.gpr[6]);

        /* ---- branches and their delay slots ---------------------------- */
        run(new int[] {
            LI(2, 0), LI(3, 0),
            I(4, 0, 0, 2),                               /* beq zero,zero,+2 */
            ADDIU(2, 0, 1),                              /* delay slot runs  */
            ADDIU(3, 0, 9),                              /* skipped          */
            NOP(),
        });
        eq("a taken branch runs its delay slot", 1, cpu.gpr[2]);
        eq("a taken branch skips the next instruction", 0, cpu.gpr[3]);

        run(new int[] {
            LI(2, 0), LI(3, 0),
            I(5, 0, 0, 2),                               /* bne zero,zero: never taken */
            ADDIU(2, 0, 1),                              /* delay slot      */
            ADDIU(3, 0, 5),
        });
        eq("a not-taken branch still runs its delay slot", 1, cpu.gpr[2]);
        eq("and continues with the next instruction", 5, cpu.gpr[3]);

        /* ---- jr -------------------------------------------------------- */
        {
            int target = 0x80010200;
            bus.write32(target, SPIN());
            bus.write32(target + 4, NOP());
            run(concat(CONST(2, target), new int[] {
                R(0, 2, 0, 0, 0, 0x08),                  /* jr v0 */
                NOP(),
            }));
            check("jr reaches the target", cpu.pc == target || cpu.pc == target + 4,
                    "pc=0x" + Integer.toHexString(cpu.pc));
        }

        /* ---- address errors -------------------------------------------- */
        {
            int[] prog = concat(CONST(1, DATA), new int[] {
                I(35, 1, 2, 2),                          /* lw v0, 2(at) */
                NOP(),
            });
            loadOnly(prog);
            cpu.pc = CODE;
            cpu.run(40);
            check("an unaligned lw raises an address error",
                    4 == ((cpu.cop0[13] >>> 2) & 0x1F), "cause=0x" + Integer.toHexString(cpu.cop0[13]));
            check("EPC points at the faulting load", CODE + 8 == cpu.cop0[14],
                    "epc=0x" + Integer.toHexString(cpu.cop0[14]));
            check("the CPU jumps to the exception vector", (cpu.pc & 0x1FFFFFFF) >= 0x80,
                    "pc=0x" + Integer.toHexString(cpu.pc));
        }
        {
            loadOnly(concat(CONST(1, DATA), new int[] {
                I(43, 1, 2, 2),                          /* sw v0, 2(at) */
                NOP(),
            }));
            cpu.pc = CODE;
            cpu.run(40);
            check("an unaligned sw raises a store address error",
                    5 == ((cpu.cop0[13] >>> 2) & 0x1F), "cause=0x" + Integer.toHexString(cpu.cop0[13]));
        }

        /* ---- an exception inside a delay slot sets BD ------------------ */
        {
            int branchPc = CODE + 3 * 4;                 /* two word prefix, then li, then bne */
            loadOnly(concat(CONST(1, DATA), new int[] {
                LI(3, 1),
                I(5, 3, 0, 2),                           /* bne v1,zero,+2 (taken) */
                I(35, 1, 2, 2),                          /* delay slot: unaligned lw */
                NOP(),
            }));
            cpu.pc = CODE;
            cpu.run(60);
            check("a fault in a delay slot sets the BD flag",
                    (cpu.cop0[13] & 0x80000000) != 0,
                    "cause=0x" + Integer.toHexString(cpu.cop0[13]));
            check("EPC points at the branch, not at the delay slot", branchPc == cpu.cop0[14],
                    "epc=0x" + Integer.toHexString(cpu.cop0[14])
                            + " expected 0x" + Integer.toHexString(branchPc));
        }

        /* ---- addi overflow traps --------------------------------------- */
        {
            loadOnly(new int[] {
                LUI(8, 0x7FFF), ORI(8, 8, 0xFFFF),
                I(8, 8, 9, 1),                           /* addi t1,t0,1 */
                NOP(),
            });
            cpu.pc = CODE;
            cpu.run(30);
            check("addi overflow raises an overflow exception",
                    12 == ((cpu.cop0[13] >>> 2) & 0x1F), "cause=0x" + Integer.toHexString(cpu.cop0[13]));
            eq("the destination is not written on overflow", 0, cpu.gpr[9]);
        }

        eq("exactly four exceptions were taken", 4, cpu.exceptions);

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL CPU EDGE TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("CPU EDGE TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /** Resets the CPU, writes a program and stops right after it. */
    static void loadOnly(int[] words) {
        cpu.reset();
        cpu.gpr[29] = 0x801FFF00;
        for (int i = 0; i < words.length; i++) bus.write32(CODE + i * 4, words[i]);
        bus.write32(CODE + words.length * 4, SPIN());
        bus.write32(CODE + words.length * 4 + 4, NOP());
    }
}
