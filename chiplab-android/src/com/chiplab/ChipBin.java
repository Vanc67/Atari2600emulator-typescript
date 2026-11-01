package com.chiplab;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

/**
 * CHIPBIN1 - the one-file chip format of ChipLab.
 *
 * Every part in the game (CPU, RAM, GPU, display) is a single .bin file:
 * a 32 byte header with all the electric numbers, a name, a human
 * description, the physical layout of the die (blocks with coordinates and
 * transistor counts - this is what the microscope draws), an ISA help text
 * and the program (for RAM chips: the initial cell contents).
 *
 * The format is fully documented in assets/prompt.txt so that any AI can
 * generate new chips as Base64 that the game imports. Pure Java, no Android
 * imports: the desktop harness tests this class directly.
 */
public final class ChipBin {
    public static final int TYPE_CPU = 1;
    public static final int TYPE_RAM = 2;
    public static final int TYPE_GPU = 3;
    public static final int TYPE_DISPLAY = 4;
    public static final int TYPE_ROM = 5;

    public static final int KIND_LOGIC = 0;
    public static final int KIND_REGS = 1;
    public static final int KIND_RAM = 2;
    public static final int KIND_ALU = 3;
    public static final int KIND_BUS = 4;
    public static final int KIND_SCREEN = 5;
    public static final int KIND_CONTROL = 6;

    public static final class Block {
        public String name;
        public int x, y, w, h;
        public int transistors;
        public int kind;
        public int param;

        public Block(String name, int x, int y, int w, int h, int transistors, int kind, int param) {
            this.name = name;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.transistors = transistors; this.kind = kind; this.param = param;
        }
    }

    public int type;
    public int bits;
    public int regs;
    public int memSize;
    public int clockKhz;
    public int screenW;
    public int screenH;
    public int transistors;
    public String name = "";
    public String desc = "";
    public String isa = "";
    public Block[] blocks = new Block[0];
    public byte[] program = new byte[0];
    /** Original file bytes (parse) or last toBytes() result. For hex dump/export. */
    public byte[] raw;

    public ChipBin() {
    }

    // ------------------------------------------------------------ parsing

    private static int u8(byte[] d, int off) {
        return d[off] & 0xFF;
    }

    private static int u16(byte[] d, int off) {
        return (d[off] & 0xFF) | ((d[off + 1] & 0xFF) << 8);
    }

    private static String utf8(byte[] d, int off, int len) {
        try {
            return new String(d, off, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return new String(d, off, len);
        }
    }

    private static byte[] utf8bytes(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s.getBytes();
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    public static ChipBin parse(byte[] data) {
        if (data == null || data.length < 32) {
            throw new IllegalArgumentException("Файл слишком маленький (нужно минимум 32 байта заголовка)");
        }
        String magic = utf8(data, 0, 8);
        if (!"CHIPBIN1".equals(magic)) {
            throw new IllegalArgumentException("Это не CHIPBIN1: нет магии CHIPBIN1 в начале файла");
        }
        int version = u8(data, 8);
        if (version != 1) {
            throw new IllegalArgumentException("Неизвестная версия формата: " + version + " (игра понимает 1)");
        }
        ChipBin c = new ChipBin();
        c.type = u8(data, 9);
        c.bits = clamp(u8(data, 10), 1, 16);
        c.regs = clamp(u8(data, 11), 1, 16);
        c.memSize = clamp(u16(data, 12), 1, 1024);
        c.clockKhz = u16(data, 14);
        c.screenW = clamp(u8(data, 16), 0, 64);
        c.screenH = clamp(u8(data, 17), 0, 64);
        c.transistors = u16(data, 18);
        int progLen = u16(data, 20);
        int nameLen = u16(data, 22);
        int descLen = u16(data, 24);
        int blocksLen = u16(data, 26);
        int isaLen = u16(data, 28);
        long total = 32L + nameLen + descLen + blocksLen + isaLen + progLen;
        if (total > data.length) {
            throw new IllegalArgumentException("Файл обрезан: заголовок обещает " + total
                    + " байт, а в файле " + data.length);
        }
        int pos = 32;
        c.name = utf8(data, pos, nameLen); pos += nameLen;
        c.desc = utf8(data, pos, descLen); pos += descLen;
        // blocks
        ArrayList<Block> list = new ArrayList<Block>();
        int blocksEnd = pos + blocksLen;
        while (pos < blocksEnd) {
            int nl = u8(data, pos); pos += 1;
            if (pos + nl + 8 > blocksEnd) {
                throw new IllegalArgumentException("Секция блоков повреждена");
            }
            String bn = utf8(data, pos, nl); pos += nl;
            int x = u8(data, pos), y = u8(data, pos + 1), w = u8(data, pos + 2), h = u8(data, pos + 3);
            int tr = u16(data, pos + 4);
            int kind = u8(data, pos + 6), param = u8(data, pos + 7);
            pos += 8;
            list.add(new Block(bn, clamp(x, 0, 15), clamp(y, 0, 15),
                    clamp(w, 1, 16), clamp(h, 1, 16), tr, clamp(kind, 0, 6), param));
            if (list.size() > 64) {
                throw new IllegalArgumentException("Слишком много блоков");
            }
        }
        c.blocks = list.toArray(new Block[list.size()]);
        c.isa = utf8(data, pos, isaLen); pos += isaLen;
        c.program = new byte[progLen];
        System.arraycopy(data, pos, c.program, 0, progLen);
        c.raw = data;
        if (c.name.length() == 0) c.name = "БЕЗ ИМЕНИ";
        return c;
    }

    // ------------------------------------------------------------ writing

    private static void put16(ByteArrayOutputStream out, int v) {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }

    public byte[] toBytes() {
        ByteArrayOutputStream blocksOut = new ByteArrayOutputStream();
        for (int i = 0; i < blocks.length; i++) {
            Block b = blocks[i];
            byte[] nb = utf8bytes(b.name == null ? "" : b.name);
            blocksOut.write(nb.length & 0xFF);
            blocksOut.write(nb, 0, nb.length);
            blocksOut.write(b.x & 0xFF); blocksOut.write(b.y & 0xFF);
            blocksOut.write(b.w & 0xFF); blocksOut.write(b.h & 0xFF);
            put16(blocksOut, b.transistors);
            blocksOut.write(b.kind & 0xFF); blocksOut.write(b.param & 0xFF);
        }
        byte[] nameB = utf8bytes(name == null ? "" : name);
        byte[] descB = utf8bytes(desc == null ? "" : desc);
        byte[] isaB = utf8bytes(isa == null ? "" : isa);
        byte[] blocksB = blocksOut.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] magic = utf8bytes("CHIPBIN1");
        out.write(magic, 0, 8);
        out.write(1);
        out.write(type & 0xFF);
        out.write(bits & 0xFF);
        out.write(regs & 0xFF);
        put16(out, memSize);
        put16(out, clockKhz);
        out.write(screenW & 0xFF);
        out.write(screenH & 0xFF);
        put16(out, transistors);
        put16(out, program.length);
        put16(out, nameB.length);
        put16(out, descB.length);
        put16(out, blocksB.length);
        put16(out, isaB.length);
        put16(out, 0);
        out.write(nameB, 0, nameB.length);
        out.write(descB, 0, descB.length);
        out.write(blocksB, 0, blocksB.length);
        out.write(isaB, 0, isaB.length);
        out.write(program, 0, program.length);
        raw = out.toByteArray();
        return raw;
    }

    // ------------------------------------------------------------ helpers

    public int mask() {
        return bits >= 31 ? 0xFFFFFFFF : (bits <= 0 ? 0 : ((1 << bits) - 1));
    }

    public boolean hasScreen() {
        return screenW > 0 && screenH > 0;
    }

    public boolean isCpuLike() {
        return type == TYPE_CPU || type == TYPE_GPU || type == TYPE_DISPLAY;
    }

    public static String typeName(int t) {
        switch (t) {
            case TYPE_CPU: return "ПРОЦЕССОР";
            case TYPE_RAM: return "ПАМЯТЬ (ОЗУ)";
            case TYPE_GPU: return "ВИДЕОКАРТА";
            case TYPE_DISPLAY: return "МОНИТОР / ЭКРАН";
            case TYPE_ROM: return "ПЗУ (ROM)";
            default: return "ЧИП #" + t;
        }
    }

    public static String kindName(int k) {
        switch (k) {
            case KIND_REGS: return "регистры";
            case KIND_RAM: return "память";
            case KIND_ALU: return "АЛУ";
            case KIND_BUS: return "шина";
            case KIND_SCREEN: return "экран/видеопамять";
            case KIND_CONTROL: return "управление";
            default: return "логика";
        }
    }

    public Block[] blocksOrDefault() {
        if (blocks != null && blocks.length > 0) return blocks;
        return defaultBlocks(type, bits, regs, memSize, hasScreen());
    }

    /** Standard die layout for chips without a BLOCKS section / constructor chips. */
    public static Block[] defaultBlocks(int type, int bits, int regs, int memSize, boolean screen) {
        int aluT = 28 * bits + 40;
        int regT = 6 * bits * regs + 16;
        int ramT = 6 * bits * memSize;
        int ctrlT = 120 + 30 * bits;
        if (type == TYPE_RAM || type == TYPE_ROM) {
            return new Block[]{
                    new Block("ДЕШИФРАТОР АДРЕСА", 0, 0, 15, 2, 64 + memSize, KIND_CONTROL, 0),
                    new Block("МАССИВ ЯЧЕЕК ПАМЯТИ", 0, 3, 11, 9, ramT, KIND_RAM, 0),
                    new Block("УСИЛИТЕЛИ ЧТЕНИЯ", 12, 3, 3, 9, 48, KIND_LOGIC, 0),
                    new Block("ШИНА ДАННЫХ", 0, 13, 15, 1, 16, KIND_BUS, 0),
            };
        }
        ArrayList<Block> l = new ArrayList<Block>();
        l.add(new Block("УСТРОЙСТВО УПРАВЛЕНИЯ", 0, 0, 7, 3, ctrlT, KIND_CONTROL, 0));
        l.add(new Block("АЛУ", 8, 0, 7, 3, aluT, KIND_ALU, 0));
        l.add(new Block("РЕГИСТРЫ", 0, 4, 7, 5, regT, KIND_REGS, 0));
        l.add(new Block(screen ? "ВИДЕОПАМЯТЬ" : "ОЗУ", 8, 4, 7, 5, ramT,
                screen ? KIND_SCREEN : KIND_RAM, 0));
        l.add(new Block("ШИНА ДАННЫХ", 0, 10, 15, 1, 24, KIND_BUS, 0));
        l.add(new Block("ВЫВОДЫ / ПИНЫ", 0, 12, 15, 2, 16, KIND_LOGIC, 0));
        return l.toArray(new Block[l.size()]);
    }

    public static int estimateTransistors(int type, int bits, int regs, int memSize, boolean screen) {
        Block[] bs = defaultBlocks(type, bits, regs, memSize, screen);
        int sum = 0;
        for (int i = 0; i < bs.length; i++) sum += bs[i].transistors;
        return sum;
    }

    /** Full numeric description: header, blocks, program disassembly (for the BIN tab). */
    public String describe() {
        StringBuffer sb = new StringBuffer();
        sb.append("=== ЗАГОЛОВОК CHIPBIN1 (32 байта) ===\n");
        sb.append("Тип: ").append(typeName(type)).append(" (код ").append(type).append(")\n");
        sb.append("Разрядность: ").append(bits).append(" бит, значения 0..").append(mask()).append("\n");
        sb.append("Регистров: ").append(regs).append(" (R0..R").append(regs - 1).append(")\n");
        sb.append("Память: ").append(memSize).append(" ячеек x ").append(bits).append(" бит = ")
                .append(memSize * bits).append(" бит\n");
        sb.append("Частота: ").append(clockKhz >= 1000 ? (clockKhz / 1000.0) + " МГц" : clockKhz + " кГц").append("\n");
        sb.append("Экран: ").append(hasScreen() ? (screenW + "x" + screenH + " пикселей") : "нет").append("\n");
        sb.append("Транзисторов: ").append(transistors).append("\n");
        sb.append("Программа/данные: ").append(program.length).append(" байт\n");
        sb.append("Размер файла: ").append(raw != null ? raw.length : 0).append(" байт\n");
        sb.append("\n=== БЛОКИ КРИСТАЛЛА (сетка 16x16) ===\n");
        Block[] bs = blocksOrDefault();
        for (int i = 0; i < bs.length; i++) {
            Block b = bs[i];
            sb.append(i + 1).append(". ").append(b.name)
                    .append(" [").append(kindName(b.kind)).append("]")
                    .append(" x=").append(b.x).append(" y=").append(b.y)
                    .append(" w=").append(b.w).append(" h=").append(b.h)
                    .append(", транзисторов: ").append(b.transistors).append("\n");
        }
        if (isa != null && isa.length() > 0) {
            sb.append("\n=== СИСТЕМА КОМАНД (секция ISA) ===\n").append(isa).append("\n");
        }
        if (isCpuLike()) {
            sb.append("\n=== ПРОГРАММА (разобранная) ===\n");
            int pc = 0;
            int guard = 0;
            while (pc < program.length && guard < 300) {
                sb.append(hex2(pc)).append(": ").append(Cpu.disasm(program, pc)).append("\n");
                pc += Cpu.instrLen(program[pc] & 0xFF);
                guard++;
            }
        } else {
            sb.append("\n=== НАЧАЛЬНЫЕ ДАННЫЕ ЯЧЕЕК ===\n");
            sb.append(program.length).append(" байт, по байту на ячейку (обрезаются до ")
                    .append(bits).append(" бит).\n");
        }
        return sb.toString();
    }

    public String hexDump() {
        byte[] d = raw != null ? raw : toBytes();
        StringBuffer sb = new StringBuffer();
        for (int off = 0; off < d.length; off += 16) {
            sb.append(hex4(off)).append("  ");
            for (int i = 0; i < 16; i++) {
                if (off + i < d.length) sb.append(hex2(d[off + i] & 0xFF)).append(' ');
                else sb.append("   ");
                if (i == 7) sb.append(' ');
            }
            sb.append(" |");
            for (int i = 0; i < 16 && off + i < d.length; i++) {
                int ch = d[off + i] & 0xFF;
                sb.append(ch >= 32 && ch < 127 ? (char) ch : '.');
            }
            sb.append("|\n");
            if (sb.length() > 60000) { sb.append("... (обрезано)\n"); break; }
        }
        return sb.toString();
    }

    static String hex2(int v) {
        String s = Integer.toHexString(v & 0xFF).toUpperCase();
        return s.length() < 2 ? "0" + s : s;
    }

    static String hex4(int v) {
        String s = Integer.toHexString(v & 0xFFFF).toUpperCase();
        while (s.length() < 4) s = "0" + s;
        return s;
    }

    // ------------------------------------------------------------ constructor

    /** Build a chip from the in-game constructor parameters. */
    public static ChipBin buildCustom(int type, int bits, int regs, int memSize,
                                      int screenW, int screenH, String name) {
        ChipBin c = new ChipBin();
        c.type = type;
        c.bits = bits;
        c.regs = regs;
        c.memSize = memSize;
        c.screenW = screenW;
        c.screenH = screenH;
        c.clockKhz = 1 + bits * memSize;
        c.name = (name == null || name.length() == 0) ? "МОЙ ЧИП" : name;
        c.blocks = defaultBlocks(type, bits, regs, memSize, screenW > 0);
        c.transistors = estimateTransistors(type, bits, regs, memSize, screenW > 0);
        c.isa = DEFAULT_ISA;
        if (type == TYPE_RAM || type == TYPE_ROM) {
            c.desc = "Чип памяти из конструктора ChipLab: " + memSize + " ячеек x " + bits
                    + " бит. Каждая ячейка SRAM - 6 транзисторов. Начальный узор: ячейка N = (N*3+1).";
            c.program = new byte[memSize];
            for (int i = 0; i < memSize; i++) c.program[i] = (byte) ((i * 3 + 1) & c.mask());
        } else if (screenW > 0 && screenH > 0) {
            c.desc = "Чип с экраном " + screenW + "x" + screenH + " из конструктора ChipLab. "
                    + "Демо-программа рисует узор: цвет пикселя = X xor Y xor ВРЕМЯ (OUT рисует пиксели по порядку).";
            c.program = screenDemoProgram(screenW, screenH);
        } else {
            c.desc = "Процессор из конструктора ChipLab: " + bits + " бит, " + regs + " регистров, "
                    + memSize + " ячеек памяти. Демо-программа считает вверх, выводит числа (OUT) "
                    + "и записывает их в память (STORER).";
            c.program = counterProgram();
        }
        c.toBytes();
        return c;
    }

    private static byte[] counterProgram() {
        // LDI R0,0 / loop: OUT R0 / STORER R0,R0 / INC R0 / JNZ loop / HALT
        return new byte[]{
                0x01, 0, 0,
                0x13, 0,
                0x17, 0, 0,
                0x0B, 0,
                0x11, 3, 0,
                (byte) 0xFF,
        };
    }

    private static byte[] screenDemoProgram(int w, int h) {
        // Same shape as the bundled GPU demo; needs mem[60],mem[61] for W/H.
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        // LDI R3,w; STORE 60,R3; LDI R3,h; STORE 61,R3
        p.write(0x01); p.write(3); p.write(w);
        p.write(0x0E); p.write(60); p.write(3);
        p.write(0x01); p.write(3); p.write(h);
        p.write(0x0E); p.write(61); p.write(3);
        int frame = p.size();
        // IN R0; LDI R2,0
        p.write(0x14); p.write(0);
        p.write(0x01); p.write(2); p.write(0);
        int yloop = p.size();
        // LDI R1,0
        p.write(0x01); p.write(1); p.write(0);
        int xloop = p.size();
        // MOV R3,R1; XOR R3,R2; XOR R3,R0; OUT R3; INC R1; LOAD R3,60; CMP R1,R3; JNZ xloop
        p.write(0x02); p.write(3); p.write(1);
        p.write(0x07); p.write(3); p.write(2);
        p.write(0x07); p.write(3); p.write(0);
        p.write(0x13); p.write(3);
        p.write(0x0B); p.write(1);
        p.write(0x0D); p.write(3); p.write(60);
        p.write(0x15); p.write(1); p.write(3);
        p.write(0x11); put16(p, xloop);
        // INC R2; LOAD R3,61; CMP R2,R3; JNZ yloop; JMP frame
        p.write(0x0B); p.write(2);
        p.write(0x0D); p.write(3); p.write(61);
        p.write(0x15); p.write(2); p.write(3);
        p.write(0x11); put16(p, yloop);
        p.write(0x0F); put16(p, frame);
        return p.toByteArray();
    }

    public static final String DEFAULT_ISA =
            "00 NOP / 01 LDI Rd,n / 02 MOV / 03 ADD / 04 SUB / 05 AND / 06 OR / 07 XOR\n"
            + "08 NOT / 09 SHL / 0A SHR / 0B INC / 0C DEC / 0D LOAD Rd,a / 0E STORE a,Rs\n"
            + "0F JMP / 10 JZ / 11 JNZ / 12 JC (адрес - 2 байта) / 13 OUT Rs / 14 IN Rd\n"
            + "15 CMP / 16 LOADR Rd,Rs / 17 STORER Ra,Rv / FF HALT. Z - ноль, C - перенос.";
}
