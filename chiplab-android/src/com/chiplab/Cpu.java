package com.chiplab;

/**
 * The ChipLab simulation engine: a tiny but honest CPU parametrised by the
 * CHIPBIN1 header (1/4/8/16 bit, N registers, M memory cells, optional
 * pixel screen). Every step updates real registers/memory/flags, and the
 * microscope renders exactly this state - the ALU operands, the memory cell
 * being read/written, the framebuffer pixels.
 *
 * Pure Java, no Android imports (tested by the desktop harness).
 */
public final class Cpu {
    public ChipBin chip;

    public int[] regs;
    public int[] mem;
    public int pc;
    public boolean flagZ;
    public boolean flagC;
    public long cycles;
    public boolean halted;

    /** Framebuffer: screenW*screenH palette indices 0..15, or null. */
    public int[] framebuffer;
    public int fbCursor;

    /** Last OUT value and a small history of outputs. */
    public int lastOut = -1;
    public int outCount;
    public final int[] outLog = new int[64];
    public int outLogSize;

    /** Activity markers for the microscope (which block is hot right now). */
    public int lastOpcode = -1;
    public int lastRegWrite = -1;
    public int lastRegRead = -1;
    public int lastMemAddr = -1;
    public boolean lastMemWrite;
    public long lastMemCycle = -1;
    public int aluA;
    public int aluB;
    public int aluResult;
    public int aluOp = -1; // opcode that used the ALU last
    public int activeKind = ChipBin.KIND_CONTROL;

    /** IN port: a free-running time counter (bumps every 512 cycles). */
    public int inputCounter;

    public Cpu(ChipBin chip) {
        this.chip = chip;
        reset();
    }

    public void reset() {
        regs = new int[Math.max(1, chip.regs)];
        mem = new int[Math.max(1, chip.memSize)];
        pc = 0;
        flagZ = false;
        flagC = false;
        cycles = 0;
        halted = false;
        fbCursor = 0;
        lastOut = -1;
        outCount = 0;
        outLogSize = 0;
        lastOpcode = -1;
        lastRegWrite = -1;
        lastRegRead = -1;
        lastMemAddr = -1;
        lastMemCycle = -1;
        aluOp = -1;
        inputCounter = 0;
        framebuffer = chip.hasScreen() ? new int[chip.screenW * chip.screenH] : null;
        if (!chip.isCpuLike()) {
            // RAM/ROM: program section = initial cell contents
            int m = chip.mask();
            for (int i = 0; i < mem.length && i < chip.program.length; i++) {
                mem[i] = chip.program[i] & m;
            }
        }
    }

    private int mask() {
        return chip.mask();
    }

    private int fetch8(int addr) {
        if (addr < 0 || addr >= chip.program.length) return 0;
        return chip.program[addr] & 0xFF;
    }

    private int reg(int idx) {
        lastRegRead = idx;
        if (idx < 0 || idx >= regs.length) return 0;
        return regs[idx];
    }

    private void setReg(int idx, int value) {
        value &= mask();
        lastRegWrite = idx;
        if (idx >= 0 && idx < regs.length) regs[idx] = value;
        flagZ = (value == 0);
    }

    private int memRead(int addr) {
        addr %= mem.length;
        if (addr < 0) addr += mem.length;
        lastMemAddr = addr;
        lastMemWrite = false;
        lastMemCycle = cycles;
        activeKind = chip.hasScreen() && chip.type != ChipBin.TYPE_CPU
                ? ChipBin.KIND_SCREEN : ChipBin.KIND_RAM;
        return mem[addr];
    }

    private void memWrite(int addr, int value) {
        addr %= mem.length;
        if (addr < 0) addr += mem.length;
        mem[addr] = value & mask();
        lastMemAddr = addr;
        lastMemWrite = true;
        lastMemCycle = cycles;
        activeKind = ChipBin.KIND_RAM;
    }

    private void alu(int op, int a, int b, int fullResult) {
        aluOp = op;
        aluA = a;
        aluB = b;
        aluResult = fullResult & mask();
        activeKind = ChipBin.KIND_ALU;
    }

    private void out(int value) {
        lastOut = value & mask();
        outCount++;
        if (outLogSize < outLog.length) {
            outLog[outLogSize++] = lastOut;
        } else {
            System.arraycopy(outLog, 1, outLog, 0, outLog.length - 1);
            outLog[outLog.length - 1] = lastOut;
        }
        if (framebuffer != null) {
            framebuffer[fbCursor] = lastOut & 15;
            fbCursor = (fbCursor + 1) % framebuffer.length;
            activeKind = ChipBin.KIND_SCREEN;
        }
    }

    /** Execute one instruction. Returns false when halted. */
    public boolean step() {
        if (halted) return false;
        if (!chip.isCpuLike()) { // RAM chips have no program counter
            halted = true;
            return false;
        }
        if (pc < 0 || pc >= chip.program.length) {
            halted = true;
            return false;
        }
        int op = fetch8(pc);
        lastOpcode = op;
        lastRegWrite = -1;
        activeKind = ChipBin.KIND_CONTROL;
        int next = pc + instrLen(op);
        cycles++;
        if ((cycles & 0x1FF) == 0) inputCounter++;
        int m = mask();
        switch (op) {
            case 0x00: // NOP
                break;
            case 0x01: // LDI Rd, imm
                setReg(fetch8(pc + 1), fetch8(pc + 2));
                activeKind = ChipBin.KIND_REGS;
                break;
            case 0x02: // MOV Rd, Rs
                setReg(fetch8(pc + 1), reg(fetch8(pc + 2)));
                activeKind = ChipBin.KIND_REGS;
                break;
            case 0x03: { // ADD
                int rd = fetch8(pc + 1);
                int a = reg(rd), b = reg(fetch8(pc + 2));
                int full = a + b;
                flagC = full > m;
                alu(op, a, b, full);
                setReg(rd, full);
                break;
            }
            case 0x04: { // SUB
                int rd = fetch8(pc + 1);
                int a = reg(rd), b = reg(fetch8(pc + 2));
                int full = a - b;
                flagC = full < 0;
                alu(op, a, b, full);
                setReg(rd, full);
                break;
            }
            case 0x05: { // AND
                int rd = fetch8(pc + 1);
                int a = reg(rd), b = reg(fetch8(pc + 2));
                alu(op, a, b, a & b);
                setReg(rd, a & b);
                break;
            }
            case 0x06: { // OR
                int rd = fetch8(pc + 1);
                int a = reg(rd), b = reg(fetch8(pc + 2));
                alu(op, a, b, a | b);
                setReg(rd, a | b);
                break;
            }
            case 0x07: { // XOR
                int rd = fetch8(pc + 1);
                int a = reg(rd), b = reg(fetch8(pc + 2));
                alu(op, a, b, a ^ b);
                setReg(rd, a ^ b);
                break;
            }
            case 0x08: { // NOT
                int rd = fetch8(pc + 1);
                int a = reg(rd);
                alu(op, a, 0, ~a);
                setReg(rd, ~a);
                break;
            }
            case 0x09: { // SHL
                int rd = fetch8(pc + 1);
                int a = reg(rd);
                flagC = chip.bits > 0 && ((a >> (chip.bits - 1)) & 1) != 0;
                alu(op, a, 0, a << 1);
                setReg(rd, a << 1);
                break;
            }
            case 0x0A: { // SHR
                int rd = fetch8(pc + 1);
                int a = reg(rd);
                flagC = (a & 1) != 0;
                alu(op, a, 0, a >> 1);
                setReg(rd, a >> 1);
                break;
            }
            case 0x0B: { // INC
                int rd = fetch8(pc + 1);
                int a = reg(rd);
                int full = a + 1;
                flagC = full > m;
                alu(op, a, 1, full);
                setReg(rd, full);
                break;
            }
            case 0x0C: { // DEC
                int rd = fetch8(pc + 1);
                int a = reg(rd);
                int full = a - 1;
                flagC = full < 0;
                alu(op, a, 1, full);
                setReg(rd, full);
                break;
            }
            case 0x0D: // LOAD Rd, addr
                setReg(fetch8(pc + 1), memRead(fetch8(pc + 2)));
                break;
            case 0x0E: // STORE addr, Rs
                memWrite(fetch8(pc + 1), reg(fetch8(pc + 2)));
                break;
            case 0x0F: // JMP addr16
                next = fetch8(pc + 1) | (fetch8(pc + 2) << 8);
                break;
            case 0x10: // JZ
                if (flagZ) next = fetch8(pc + 1) | (fetch8(pc + 2) << 8);
                break;
            case 0x11: // JNZ
                if (!flagZ) next = fetch8(pc + 1) | (fetch8(pc + 2) << 8);
                break;
            case 0x12: // JC
                if (flagC) next = fetch8(pc + 1) | (fetch8(pc + 2) << 8);
                break;
            case 0x13: // OUT Rs
                out(reg(fetch8(pc + 1)));
                break;
            case 0x14: // IN Rd
                setReg(fetch8(pc + 1), inputCounter);
                activeKind = ChipBin.KIND_BUS;
                break;
            case 0x15: { // CMP Rd, Rs (flags only)
                int a = reg(fetch8(pc + 1)), b = reg(fetch8(pc + 2));
                int full = a - b;
                flagC = full < 0;
                flagZ = ((full & m) == 0);
                alu(op, a, b, full);
                break;
            }
            case 0x16: // LOADR Rd, Rs  Rd = MEM[Rs]
                setReg(fetch8(pc + 1), memRead(reg(fetch8(pc + 2))));
                break;
            case 0x17: // STORER Ra, Rv  MEM[Ra] = Rv
                memWrite(reg(fetch8(pc + 1)), reg(fetch8(pc + 2)));
                break;
            case 0xFF: // HALT
                halted = true;
                break;
            default:
                // Unknown opcode: treat as NOP (games from AI may be creative)
                break;
        }
        pc = next;
        return !halted;
    }

    /** Run up to n steps (stops early on HALT). Returns steps actually run. */
    public int run(int n) {
        int done = 0;
        while (done < n && step()) done++;
        return done;
    }

    public int litPixels() {
        if (framebuffer == null) return 0;
        int c = 0;
        for (int i = 0; i < framebuffer.length; i++) if (framebuffer[i] != 0) c++;
        return c;
    }

    // ------------------------------------------------------------ disasm

    public static int instrLen(int opcode) {
        switch (opcode) {
            case 0x00: case 0xFF:
                return 1;
            case 0x08: case 0x09: case 0x0A: case 0x0B: case 0x0C:
            case 0x13: case 0x14:
                return 2;
            default:
                return 3;
        }
    }

    public static String disasm(byte[] prog, int at) {
        if (at < 0 || at >= prog.length) return "???";
        int op = prog[at] & 0xFF;
        int b1 = at + 1 < prog.length ? prog[at + 1] & 0xFF : 0;
        int b2 = at + 2 < prog.length ? prog[at + 2] & 0xFF : 0;
        int a16 = b1 | (b2 << 8);
        switch (op) {
            case 0x00: return "NOP";
            case 0x01: return "LDI R" + b1 + ", " + b2;
            case 0x02: return "MOV R" + b1 + ", R" + b2;
            case 0x03: return "ADD R" + b1 + ", R" + b2;
            case 0x04: return "SUB R" + b1 + ", R" + b2;
            case 0x05: return "AND R" + b1 + ", R" + b2;
            case 0x06: return "OR R" + b1 + ", R" + b2;
            case 0x07: return "XOR R" + b1 + ", R" + b2;
            case 0x08: return "NOT R" + b1;
            case 0x09: return "SHL R" + b1;
            case 0x0A: return "SHR R" + b1;
            case 0x0B: return "INC R" + b1;
            case 0x0C: return "DEC R" + b1;
            case 0x0D: return "LOAD R" + b1 + ", [" + b2 + "]";
            case 0x0E: return "STORE [" + b1 + "], R" + b2;
            case 0x0F: return "JMP " + a16;
            case 0x10: return "JZ " + a16;
            case 0x11: return "JNZ " + a16;
            case 0x12: return "JC " + a16;
            case 0x13: return "OUT R" + b1;
            case 0x14: return "IN R" + b1;
            case 0x15: return "CMP R" + b1 + ", R" + b2;
            case 0x16: return "LOADR R" + b1 + ", [R" + b2 + "]";
            case 0x17: return "STORER [R" + b1 + "], R" + b2;
            case 0xFF: return "HALT";
            default: return "DB " + ChipBin.hex2(op) + " (неизвестная команда)";
        }
    }
}
