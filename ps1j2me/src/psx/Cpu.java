package psx;

/**
 * MIPS R3000A (PSX CPU core) with a lazy, page based block cache.
 *
 * The interpreter never decodes an instruction on every tick: the hot path
 * walks a pre-decoded {@link CodeBlock} (op key + raw word per instruction).
 * Blocks are built on first execution of a 4 KiB code page and invalidated
 * when the CPU (or a DMA transfer) writes into that page.
 *
 * All arithmetic is plain 32 bit integer math, no float/double, no java.nio,
 * so the class is safe for CLDC 1.1 devices.
 */
public final class Cpu {

    /* ---- dispatch keys -------------------------------------------------- */
    static final int K_SLL = 0x000, K_SRL = 0x002, K_SRA = 0x003;
    static final int K_SLLV = 0x004, K_SRLV = 0x006, K_SRAV = 0x007;
    static final int K_JR = 0x008, K_JALR = 0x009, K_SYSCALL = 0x00C, K_BREAK = 0x00D;
    static final int K_MFHI = 0x010, K_MTHI = 0x011, K_MFLO = 0x012, K_MTLO = 0x013;
    static final int K_MULT = 0x018, K_MULTU = 0x019, K_DIV = 0x01A, K_DIVU = 0x01B;
    static final int K_ADD = 0x020, K_ADDU = 0x021, K_SUB = 0x022, K_SUBU = 0x023;
    static final int K_AND = 0x024, K_OR = 0x025, K_XOR = 0x026, K_NOR = 0x027;
    static final int K_SLT = 0x02A, K_SLTU = 0x02B;

    static final int K_BLTZ = 0x0100, K_BGEZ = 0x0101, K_BLTZAL = 0x0110, K_BGEZAL = 0x0111;
    static final int K_J = 0x0200, K_JAL = 0x0300;
    static final int K_BEQ = 0x0400, K_BNE = 0x0500, K_BLEZ = 0x0600, K_BGTZ = 0x0700;
    static final int K_ADDI = 0x0800, K_ADDIU = 0x0900, K_SLTI = 0x0A00, K_SLTIU = 0x0B00;
    static final int K_ANDI = 0x0C00, K_ORI = 0x0D00, K_XORI = 0x0E00, K_LUI = 0x0F00;
    static final int K_COP0 = 0x1000, K_COP2 = 0x1200, K_COP3 = 0x1300;
    static final int K_LB = 0x2000, K_LH = 0x2100, K_LWL = 0x2200, K_LW = 0x2300;
    static final int K_LBU = 0x2400, K_LHU = 0x2500, K_LWR = 0x2600;
    static final int K_SB = 0x2800, K_SH = 0x2900, K_SWL = 0x2A00, K_SW = 0x2B00, K_SWR = 0x2E00;
    static final int K_LWC2 = 0x3200, K_SWC2 = 0x3A00;

    /* ---- exception codes ------------------------------------------------ */
    public static final int EXC_INT = 0, EXC_ADEL = 4, EXC_ADES = 5, EXC_SYS = 8,
            EXC_BP = 9, EXC_RI = 10, EXC_CPU = 11, EXC_OV = 12;

    public final int[] gpr = new int[32];
    /** COP0 register file: SR, Cause, EPC, BadVaddr, ... */
    public final int[] cop0 = new int[32];
    public int hi, lo;
    public int pc;
    public int branchTarget;
    public boolean branchPending;
    public int cycles;
    public boolean irqLine;

    private int pendingLoadReg = -1;
    private int pendingLoadValue;
    private int lastPc;
    private int exceptionPc;
    private boolean inDelaySlot;

    private final Bus bus;
    private final Gte gte;
    private final CodeCache cache;

    /**
     * BIOS kernel replacement (null when a real BIOS image is running).
     * When set, calls into the A0/B0/C0 vectors are answered in Java instead
     * of executing the (missing) BIOS code.
     */
    public BiosHle hle;

    /** Simple execution statistics, shown in the on screen OSD. */
    public int lastBlockCount;
    public int lastInstrCount;
    /** Exceptions taken so far. A healthy program takes none after boot. */
    public int exceptions;
    /** ExcCode of the last exception, for diagnostics. */
    public int lastExcCode;
    boolean codeDirty;
    private int currentBlockStart;

    public Cpu(Bus bus, Gte gte) {
        this.bus = bus;
        this.gte = gte;
        this.cache = bus.codeCache;
        gpr[0] = 0;
        reset();
    }

    public final void reset() {
        for (int i = 0; i < 32; i++) {
            gpr[i] = 0;
            cop0[i] = 0;
        }
        cop0[12] = 0x10900000; /* BEV=1, CU0, cache config */
        cop0[15] = 0x00000002; /* PRID: R3000A */
        pc = 0xBFC00000;
        branchPending = false;
        pendingLoadReg = -1;
        exceptionTaken = false;
        cycles = 0;
        if (cache != null) cache.flush();
    }

    /* ---- decode --------------------------------------------------------- */

    /** Maps a raw instruction word to a compact dispatch key. */
    public static int decodeKey(int raw) {
        int op = (raw >>> 26) & 0x3F;
        if (op == 0) return raw & 0x3F;                       /* SPECIAL  */
        if (op == 1) return 0x0100 | ((raw >>> 16) & 0x1F);   /* REGIMM   */
        if (op == 0x12) return 0x1200 | ((raw >>> 21) & 0x1F);/* COP2     */
        if (op == 0x10) return 0x1000 | ((raw >>> 21) & 0x1F);/* COP0     */
        return op << 8;
    }

    static boolean isTerminator(int raw, int key) {
        int op = (raw >>> 26) & 0x3F;
        if (op == 2 || op == 3 || op == 1) return true;             /* j, jal, regimm */
        if (op >= 0x10 && op <= 0x17) return true;                  /* branches      */
        if (op == 0) {
            int f = raw & 0x3F;
            if (f == 8 || f == 9 || f == 0x0C || f == 0x0D) return true;
        }
        if (op == 0x10) { /* eret / rfe are terminators only when they leave code flow */
            int f = raw & 0x3F;
            if (f == 0x18) return true;
        }
        return false;
    }

    /* ---- execution ------------------------------------------------------ */

    /**
     * Runs the CPU for at most {@code cycleBudget} cycles.
     *
     * @return number of cycles consumed
     */
    public int run(int cycleBudget) {
        int start = cycles;
        while (cycles - start < cycleBudget) {
            if (irqLine && (cop0[12] & 1) != 0 && (cop0[12] & 0x400) != 0) {
                exception(EXC_INT);
                continue;
            }
            if (branchPending) {
                delaySlotStep();
                continue;
            }
            if (hle != null) {
                int vec = pc & 0x1FFFFFFF;
                if (vec == 0xA0 || vec == 0xB0 || vec == 0xC0) {
                    int ra = gpr[31];
                    hle.call(vec, gpr[9]);
                    gpr[0] = 0;
                    pc = ra;
                    branchPending = false;
                    cycles += 32;
                    continue;
                }
            }
            codeDirty = false;
            CodeBlock b = cache.blockAt(pc);
            currentBlockStart = b.startPc;
            int n = b.exec(this);
            lastBlockCount++;
            lastInstrCount += n;
            if (n == 0) cycles += 2;
        }
        return cycles - start;
    }

    /** Executes the single instruction at {@code pc} (a branch delay slot). */
    private void delaySlotStep() {
        int target = branchTarget;
        branchPending = false;
        inDelaySlot = true;
        int raw = bus.read32(pc);
        lastPc = pc;
        int key = decodeKey(raw);
        execOp(key, raw);
        inDelaySlot = false;
        if (!branchPending) pc = target;
    }

    /**
     * Executes one already decoded instruction. Returns non zero on control
     * flow.
     *
     * <p>MIPS I load delay: a loaded value becomes visible only after the
     * instruction that follows the load has run, and that instruction still
     * reads the old contents. Two loads in a row is therefore perfectly legal
     * (compilers emit it all the time) and the first value must not be lost:
     * the pending slot is handed over before the instruction executes and the
     * previous value is committed afterwards.</p>
     */
    final int execOp(int key, int raw) {
        int commitReg = pendingLoadReg;
        int commitValue = pendingLoadValue;
        pendingLoadReg = -1;
        exceptionTaken = false;
        /* LWL and LWR merge into the register they load, so the documented
           "lwl rt, A+3 / lwr rt, A" pair must see the first half even though
           it sits in the load delay slot. */
        if ((key == K_LWL || key == K_LWR) && commitReg > 0
                && ((raw >>> 16) & 0x1F) == commitReg) {
            gpr[commitReg] = commitValue;
            commitReg = -1;
        }
        int result = execSwitch(key, raw);
        if (commitReg > 0 && !exceptionTaken) gpr[commitReg] = commitValue;
        gpr[0] = 0;
        return result;
    }

    /** Set when an instruction raised an exception; its load is discarded. */
    private boolean exceptionTaken;

    private int execSwitch(int key, int raw) {
        lastPc = pc;
        exceptionPc = pc;
        pc += 4;
        cycles += 2;

        int rs = (raw >>> 21) & 0x1F;
        int rt = (raw >>> 16) & 0x1F;
        int rd = (raw >>> 11) & 0x1F;
        int sa = (raw >>> 6) & 0x1F;

        switch (key) {
            /* ---------------- SPECIAL ---------------- */
            case K_SLL:  if (raw == 0) return 0; gpr[rd] = gpr[rt] << sa; return 0;
            case K_SRL:  gpr[rd] = gpr[rt] >>> sa; return 0;
            case K_SRA:  gpr[rd] = gpr[rt] >> sa; return 0;
            case K_SLLV: gpr[rd] = gpr[rt] << (gpr[rs] & 0x1F); return 0;
            case K_SRLV: gpr[rd] = gpr[rt] >>> (gpr[rs] & 0x1F); return 0;
            case K_SRAV: gpr[rd] = gpr[rt] >> (gpr[rs] & 0x1F); return 0;
            case K_JR:   branchTarget = gpr[rs]; branchPending = true; return 1;
            case K_JALR: {
                int t = gpr[rs];
                gpr[rd] = pc + 4;
                gpr[0] = 0;
                branchTarget = t;
                branchPending = true;
                return 1;
            }
            case K_SYSCALL:
                if (hle != null) {
                    int ra = gpr[31];
                    hle.syscall(gpr[4]);
                    gpr[0] = 0;
                    pc = ra;
                    branchPending = false;
                    cycles += 16;
                    return 1;
                }
                exception(EXC_SYS);
                return 1;
            case K_BREAK:   exception(EXC_BP); return 1;
            case K_MFHI: gpr[rd] = hi; return 0;
            case K_MTHI: hi = gpr[rs]; return 0;
            case K_MFLO: gpr[rd] = lo; return 0;
            case K_MTLO: lo = gpr[rs]; return 0;
            case K_MULT: {
                long r = (long) gpr[rs] * (long) gpr[rt];
                lo = (int) r;
                hi = (int) (r >> 32);
                cycles += 8;
                return 0;
            }
            case K_MULTU: {
                long a = ((long) gpr[rs]) & 0xFFFFFFFFL;
                long b = ((long) gpr[rt]) & 0xFFFFFFFFL;
                long r = a * b;
                lo = (int) r;
                hi = (int) (r >>> 32);
                cycles += 8;
                return 0;
            }
            case K_DIV: {
                int a = gpr[rs], b = gpr[rt];
                if (b == 0) {
                    lo = (a >= 0) ? -1 : 1;
                    hi = a;
                } else if (a == 0x80000000 && b == -1) {
                    lo = 0x80000000;
                    hi = 0;
                } else {
                    lo = a / b;
                    hi = a % b;
                }
                cycles += 20;
                return 0;
            }
            case K_DIVU: {
                int a = gpr[rs], b = gpr[rt];
                if (b == 0) {
                    lo = -1;
                    hi = a;
                } else {
                    int q = (int) ((((long) a) & 0xFFFFFFFFL) / (((long) b) & 0xFFFFFFFFL));
                    int r = (int) ((((long) a) & 0xFFFFFFFFL) % (((long) b) & 0xFFFFFFFFL));
                    lo = q;
                    hi = r;
                }
                cycles += 20;
                return 0;
            }
            case K_ADD: {
                int r = gpr[rs] + gpr[rt];
                if (((gpr[rs] ^ r) & (gpr[rt] ^ r)) < 0) { exception(EXC_OV); return 1; }
                gpr[rd] = r;
                return 0;
            }
            case K_ADDU: gpr[rd] = gpr[rs] + gpr[rt]; return 0;
            case K_SUB: {
                int r = gpr[rs] - gpr[rt];
                if (((gpr[rs] ^ gpr[rt]) & (gpr[rs] ^ r)) < 0) { exception(EXC_OV); return 1; }
                gpr[rd] = r;
                return 0;
            }
            case K_SUBU: gpr[rd] = gpr[rs] - gpr[rt]; return 0;
            case K_AND:  gpr[rd] = gpr[rs] & gpr[rt]; return 0;
            case K_OR:   gpr[rd] = gpr[rs] | gpr[rt]; return 0;
            case K_XOR:  gpr[rd] = gpr[rs] ^ gpr[rt]; return 0;
            case K_NOR:  gpr[rd] = ~(gpr[rs] | gpr[rt]); return 0;
            case K_SLT:  gpr[rd] = (gpr[rs] < gpr[rt]) ? 1 : 0; return 0;
            case K_SLTU: gpr[rd] = ((gpr[rs] ^ 0x80000000) < (gpr[rt] ^ 0x80000000)) ? 1 : 0; return 0;

            /* ---------------- REGIMM ---------------- */
            case K_BLTZ: {
                if (gpr[rs] < 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            }
            case K_BGEZ: {
                if (gpr[rs] >= 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            }
            case K_BLTZAL: {
                gpr[31] = pc + 4;
                if (gpr[rs] < 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            }
            case K_BGEZAL: {
                gpr[31] = pc + 4;
                if (gpr[rs] >= 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            }

            /* ---------------- jumps ---------------- */
            case K_J:  branchTarget = (pc & 0xF0000000) | ((raw & 0x03FFFFFF) << 2); branchPending = true; return 1;
            case K_JAL: {
                branchTarget = (pc & 0xF0000000) | ((raw & 0x03FFFFFF) << 2);
                gpr[31] = pc + 4;
                branchPending = true;
                return 1;
            }

            /* ---------------- branches ---------------- */
            case K_BEQ:
                if (gpr[rs] == gpr[rt]) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            case K_BNE:
                if (gpr[rs] != gpr[rt]) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            case K_BLEZ:
                if (gpr[rs] <= 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;
            case K_BGTZ:
                if (gpr[rs] > 0) { branchTarget = pc + (((short) raw) << 2); branchPending = true; }
                return 1;

            /* ---------------- immediate ---------------- */
            case K_ADDI: {
                int imm = (short) raw;
                int r = gpr[rs] + imm;
                if (((gpr[rs] ^ r) & (imm ^ r)) < 0) { exception(EXC_OV); return 1; }
                gpr[rt] = r;
                return 0;
            }
            case K_ADDIU: gpr[rt] = gpr[rs] + (short) raw; return 0;
            case K_SLTI:  gpr[rt] = (gpr[rs] < (short) raw) ? 1 : 0; return 0;
            case K_SLTIU: gpr[rt] = ((gpr[rs] ^ 0x80000000) < ((raw & 0xFFFF) ^ 0x80000000)) ? 1 : 0; return 0;
            case K_ANDI:  gpr[rt] = gpr[rs] & (raw & 0xFFFF); return 0;
            case K_ORI:   gpr[rt] = gpr[rs] | (raw & 0xFFFF); return 0;
            case K_XORI:  gpr[rt] = gpr[rs] ^ (raw & 0xFFFF); return 0;
            case K_LUI:   gpr[rt] = (raw & 0xFFFF) << 16; return 0;

            /* ---------------- COP0 ---------------- */
            case 0x1000: gpr[rt] = cop0[rd]; return 0;                 /* mfc0 */
            case 0x1004: cop0[rd] = gpr[rt]; return 0;                 /* mtc0 */
            case 0x1010: {                                             /* rfe / eret */
                int f = raw & 0x3F;
                if (f == 0x10) { /* rfe */
                    int sr = cop0[12];
                    sr = (sr & 0xFFFFFFC0) | ((sr >> 2) & 0x0F);
                    cop0[12] = sr;
                } else if (f == 0x18) { /* eret */
                    pc = cop0[14];
                    branchPending = false;
                }
                return 1;
            }

            /* ---------------- COP2 / GTE ---------------- */
            case 0x1200: gpr[rt] = gte.readData(rd); return 0;          /* mfc2 */
            case 0x1202: gpr[rt] = gte.readControl(rd); return 0;       /* cfc2 */
            case 0x1204: gte.writeData(rd, gpr[rt]); return 0;          /* mtc2 */
            case 0x1206: gte.writeControl(rd, gpr[rt]); return 0;       /* ctc2 */
            case 0x1210: gte.execute(raw); return 0;
            case K_LWC2: {
                int v = bus.read32(gpr[rs] + (short) raw);
                gte.writeData(rt, v);
                return 0;
            }
            case K_SWC2: {
                bus.write32(gpr[rs] + (short) raw, gte.readData(rt));
                return 0;
            }

            /* ---------------- loads ---------------- */
            case K_LB: {
                int addr = gpr[rs] + (short) raw;
                setLoad(rt, (byte) bus.read8(addr));
                cycles += 2;
                return 0;
            }
            case K_LBU: {
                int addr = gpr[rs] + (short) raw;
                setLoad(rt, bus.read8(addr));
                cycles += 2;
                return 0;
            }
            case K_LH: {
                int addr = gpr[rs] + (short) raw;
                if ((addr & 1) != 0) { exception(EXC_ADEL); return 1; }
                setLoad(rt, (short) bus.read16(addr));
                cycles += 2;
                return 0;
            }
            case K_LHU: {
                int addr = gpr[rs] + (short) raw;
                if ((addr & 1) != 0) { exception(EXC_ADEL); return 1; }
                setLoad(rt, bus.read16(addr));
                cycles += 2;
                return 0;
            }
            case K_LW: {
                int addr = gpr[rs] + (short) raw;
                if ((addr & 3) != 0) { exception(EXC_ADEL); return 1; }
                setLoad(rt, bus.read32(addr));
                cycles += 2;
                return 0;
            }
            case K_LWL: {
                int addr = gpr[rs] + (short) raw;
                int cur = rt == 0 ? 0 : gpr[rt];
                int mem = bus.read32(addr & ~3);
                int v;
                switch (addr & 3) {
                    case 0: v = (cur & 0x00FFFFFF) | (mem << 24); break;
                    case 1: v = (cur & 0x0000FFFF) | (mem << 16); break;
                    case 2: v = (cur & 0x000000FF) | (mem << 8); break;
                    default: v = mem; break;
                }
                setLoad(rt, v);
                return 0;
            }
            case K_LWR: {
                int addr = gpr[rs] + (short) raw;
                int cur = rt == 0 ? 0 : gpr[rt];
                int mem = bus.read32(addr & ~3);
                int v;
                switch (addr & 3) {
                    case 0: v = mem; break;
                    case 1: v = (cur & 0xFF000000) | (mem >>> 8); break;
                    case 2: v = (cur & 0xFFFF0000) | (mem >>> 16); break;
                    default: v = (cur & 0xFFFFFF00) | (mem >>> 24); break;
                }
                setLoad(rt, v);
                return 0;
            }

            /* ---------------- stores ---------------- */
            case K_SB:
                bus.write8(gpr[rs] + (short) raw, gpr[rt]);
                return 0;
            case K_SH: {
                int addr = gpr[rs] + (short) raw;
                if ((addr & 1) != 0) { exception(EXC_ADES); return 1; }
                bus.write16(addr, gpr[rt]);
                return 0;
            }
            case K_SW: {
                int addr = gpr[rs] + (short) raw;
                if ((addr & 3) != 0) { exception(EXC_ADES); return 1; }
                bus.write32(addr, gpr[rt]);
                return 0;
            }
            case K_SWL: {
                int addr = gpr[rs] + (short) raw;
                int mem = bus.read32(addr & ~3);
                int v;
                switch (addr & 3) {
                    case 0: v = (mem & 0xFFFFFF00) | (gpr[rt] >>> 24); break;
                    case 1: v = (mem & 0xFFFF0000) | (gpr[rt] >>> 16); break;
                    case 2: v = (mem & 0xFF000000) | (gpr[rt] >>> 8); break;
                    default: v = gpr[rt]; break;
                }
                bus.write32(addr & ~3, v);
                return 0;
            }
            case K_SWR: {
                int addr = gpr[rs] + (short) raw;
                int mem = bus.read32(addr & ~3);
                int v;
                switch (addr & 3) {
                    case 0: v = gpr[rt]; break;
                    case 1: v = (mem & 0x000000FF) | (gpr[rt] << 8); break;
                    case 2: v = (mem & 0x0000FFFF) | (gpr[rt] << 16); break;
                    default: v = (mem & 0x00FFFFFF) | (gpr[rt] << 24); break;
                }
                bus.write32(addr & ~3, v);
                return 0;
            }

            default:
                /* Unimplemented / illegal opcode: raise RI but keep running
                   (a PSX game that hits this usually continues after BIOS
                   installed a handler). */
                exception(EXC_RI);
                return 1;
        }
    }

    /** Called by the bus after a RAM write; drops the block if it owns the page. */
    final void noteCodeWrite(int physAddr) {
        if ((physAddr >>> 12) == (currentBlockStart >>> 12)) codeDirty = true;
    }

    private void setLoad(int reg, int value) {
        if (reg == 0) return;
        pendingLoadReg = reg;
        pendingLoadValue = value;
    }

    /* ---- exceptions ----------------------------------------------------- */

    public void exception(int code) {
        exceptions++;
        exceptionTaken = true;
        lastExcCode = code;
        int sr = cop0[12];
        int cause = cop0[13] & ~0x7C;
        cause |= (code & 0x1F) << 2;
        if (code == EXC_INT) cause |= 0x400;   /* IP2: hardware interrupt */
        int epc;
        if (inDelaySlot) {
            epc = exceptionPc - 4;
            cause |= 0x80000000;
        } else {
            epc = exceptionPc;
            cause &= 0x7FFFFFFF;
        }
        cop0[13] = cause;
        cop0[14] = epc;
        if (inDelaySlot) pc = exceptionPc - 4;
        sr = (sr & ~0x3F) | ((sr & 0x0F) << 2); /* push KU/IE stack */
        cop0[12] = sr;
        branchPending = false;
        pendingLoadReg = -1;
        pc = ((sr & 0x00400000) != 0) ? 0xBFC00180 : 0x80000080;
        cycles += 4;
    }

    public void triggerIrq() {
        irqLine = true;
    }

    public int getLastPc() {
        return lastPc;
    }
}
