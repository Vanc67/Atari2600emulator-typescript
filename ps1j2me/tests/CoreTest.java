import psx.*;

public class CoreTest {
    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (detail == null ? "" : "  " + detail));
        if (!ok) failures++;
    }

    static void put(Bus bus, int addr, int[] words) {
        for (int i = 0; i < words.length; i++) bus.write32(addr + i * 4, words[i]);
    }

    // helpers to build MIPS
    static int R(int op, int rs, int rt, int rd, int sa, int fn) {
        return (op << 26) | (rs << 21) | (rt << 16) | (rd << 11) | (sa << 6) | fn;
    }
    static int I(int op, int rs, int rt, int imm) {
        return (op << 26) | (rs << 21) | (rt << 16) | (imm & 0xFFFF);
    }
    static int J(int op, int target) { return (op << 26) | ((target >> 2) & 0x03FFFFFF); }

    static void putSpin(Bus bus, int addr, int[] words) {
        put(bus, addr, words);
        bus.write32(addr + words.length * 4, 0x1000FFFF);
        bus.write32(addr + words.length * 4 + 4, 0);
    }

    static void runLoop(PS1Machine m, int frames) {
        for (int i = 0; i < frames; i++) m.runFrame();
    }

    public static void main(String[] a) throws Exception {
        // ---------- CPU: sum 1..100 in a loop --------------------------------
        {
            PS1Machine m = new PS1Machine(new VirtualCD());
            Bus bus = m.bus;
            Cpu cpu = m.cpu;
            int base = 0x80010000;
            int[] prog = {
                I(9, 0, 8, 0),          // addiu t0, zero, 0
                I(9, 0, 9, 1),          // addiu t1, zero, 1
                // loop:
                R(0, 8, 9, 8, 0, 0x21), // addu t0,t0,t1
                I(9, 9, 9, 1),          // addiu t1,t1,1
                I(10, 9, 10, 101),      // slti t2,t1,101
                I(5, 10, 0, -5),        // bne t2,zero, loop  (offset -5 -> back 3 insns? compute below)
                R(0, 0, 0, 0, 0, 0),    // nop (delay slot)
                R(0, 0, 0, 0, 0, 0),    // nop
            };
            // branch is at index 5, target loop (index 2): offset = (2 - (5+1)) = -4
            prog[5] = I(5, 10, 0, -4);
            putSpin(bus, base, prog);
            cpu.pc = base;
            cpu.gpr[29] = 0x801FFFF0;
            runLoop(m, 1);
            check("cpu loop sum 1..100 == 5050", cpu.gpr[8] == 5050, "got " + cpu.gpr[8]);

        // ---------- CPU: loads, stores and load delay ------------------------
            int data = 0x80020000;
            int[] prog2 = {
                I(9, 0, 8, 0x1234),      // addiu t0, zero, 0x1234
                I(11, 0, 9, 0), prog2_dummy(),
            };
            int[] p2 = new int[8];
            p2[0] = I(9, 0, 8, 0x1234);      // t0 = 0x1234
            p2[1] = I(11, 0, 0, (data >> 16) & 0xFFFF); // lui zero? no: lui at, addr
            p2[1] = I(15, 0, 1, (data >>> 16) & 0xFFFF); // lui at, hi
            p2[2] = I(13, 1, 1, data & 0xFFFF);          // ori at, at, lo
            p2[3] = I(11, 0, 2, 0);                      // sw t0, 0(at)  (op 0x2B below)
            p2[3] = I(0x2B, 1, 8, 0);                    // sw t0,0(at)
            p2[4] = I(0x23, 1, 10, 0);                   // lw t2,0(at)
            p2[5] = I(9, 10, 11, 5);                     // addiu t3,t2,5  (uses stale t2 -> 0)
            p2[6] = I(9, 10, 12, 0);                     // addiu t4,t2,0  (after delay)
            p2[7] = 0;
            putSpin(bus, base, p2);
            cpu.reset();
            cpu.pc = base;
            runLoop(m, 1);
            check("store/load word", bus.read32(data) == 0x1234, "mem=" + Integer.toHexString(bus.read32(data)));
            check("load delay slot sees old value", cpu.gpr[11] == 5, "t3=" + cpu.gpr[11]);
            check("load value visible after delay", cpu.gpr[12] == 0x1234, "t4=" + Integer.toHexString(cpu.gpr[12]));

        // ---------- CPU: JAL / JR --------------------------------------------
            int[] p3 = new int[8];
            p3[0] = J(3, base + 0x20);                   // jal sub
            p3[1] = I(9, 0, 5, 7);                       // delay: li a1,7
            p3[2] = I(9, 0, 6, 1);                       // after return: li a2,1
            p3[3] = 0x1000FFFF;
            p3[4] = 0;
            p3[5] = 0;
            p3[6] = 0;
            p3[7] = 0;
            int[] sub = new int[4];
            sub[0] = I(9, 5, 5, 1);                      // addiu a1,a1,1
            sub[1] = R(0, 31, 0, 0, 0, 8);               // jr ra
            sub[2] = I(9, 0, 7, 3);                      // delay: li a3,3
            sub[3] = 0;
            putSpin(bus, base, p3);
            putSpin(bus, base + 0x20, sub);
            cpu.reset();
            cpu.pc = base;
            runLoop(m, 1);
            check("jal/jr + delay", cpu.gpr[5] == 8 && cpu.gpr[6] == 1 && cpu.gpr[7] == 3,
                    "a1=" + cpu.gpr[5] + " a2=" + cpu.gpr[6] + " a3=" + cpu.gpr[7]);

        // ---------- GTE: RTPS -------------------------------------------------
            Gte gte = new Gte();
            gte.reset();
            gte.writeControl(Gte.RT, 0x00001000);   // r11=0x1000, r12=0   (1.0 in 4.12)
            gte.writeControl(Gte.RT + 1, 0);
            gte.writeControl(Gte.RT + 2, 0x00001000);
            gte.writeControl(Gte.RT + 3, 0);
            gte.writeControl(Gte.RT + 4, 0x00001000);
            gte.writeControl(Gte.TRX, 0);
            gte.writeControl(Gte.TRY, 0);
            gte.writeControl(Gte.TRZ, 0);
            gte.writeControl(Gte.H, 0x0100);
            gte.writeControl(Gte.OFX, 160);
            gte.writeControl(Gte.OFY, 120);
            gte.writeData(Gte.VXY0, (0) | (0 << 16));
            gte.writeData(Gte.VZ0, 0x1000);
            gte.execute(0x00080001);                // RTPS, sf=1
            int sxy = gte.readData(Gte.SXY2);
            int sz = gte.readData(Gte.SZ3);
            check("gte rtps sz", sz == 0x1000, "sz=" + Integer.toHexString(sz));
            check("gte rtps screen coords", (short) (sxy & 0xFFFF) == 160 && (short) (sxy >>> 16) == 120,
                    "sxy=" + Integer.toHexString(sxy) + " ir1=" + gte.readData(Gte.IR1));

        // ---------- GPU: fill, triangle, scanout ------------------------------
            Gpu gpu = new Gpu();
            gpu.reset();
            gpu.setViewport(320, 240);
            gpu.writeGP1(0x03000000);               // display on
            gpu.writeGP1(0x05000000);               // display start 0,0
            gpu.writeGP1(0x08000001);               // 320x240
            gpu.writeGP0(0xE3000000);               // draw area tl
            gpu.writeGP0(0xE4000000 | (239 << 10) | 319);
            gpu.writeGP0(0xE5000000);               // offset 0,0
            gpu.writeGP0(0x020000FF);               // fill rect colour (red)
            gpu.writeGP0(0x00000000);
            gpu.writeGP0(0x00F00140);               // x=0,y=0,w=240,h=320
            int px = gpu.vram[10 * 1024 + 10] & 0xFFFF;
            check("gpu fill rect", px == 0x001F, "px=" + Integer.toHexString(px));

            gpu.writeGP0(0xE1000000);               // texpage 0
            gpu.writeGP0(0x600000FF);               // rectangle 1x1? (variable 1x1 colour)
            gpu.writeGP0(100 | (100 << 16));        // vertex
            gpu.writeGP0(1 | (1 << 16));            // w/h
            int rx = gpu.vram[100 * 1024 + 100] & 0xFFFF;
            check("gpu rect", rx == 0x001F, "rx=" + Integer.toHexString(rx));

            // flat triangle (20h): colour in command word, 3 vertices
            gpu.writeGP0(0x20FF0000);               // blue? 0x00FF00 = green (BGR order: r=FF)
            gpu.writeGP0(0 | (0 << 16));
            gpu.writeGP0(200 | (0 << 16));
            gpu.writeGP0(200 | (200 << 16));
            // pixel inside (e.g. 150,20) should be painted
            int triPx = gpu.vram[20 * 1024 + 150] & 0xFFFF;
            check("gpu triangle rasterises", triPx != 0, "px=" + Integer.toHexString(triPx));

            int[] frame = gpu.scanout();
            check("gpu scanout frame", frame.length == 320 * 240 && frame[230 * 320 + 250] == 0xFFF80000,
                    "frame[0]=" + Integer.toHexString(frame[230 * 320 + 250]));

        // ---------- CPU: self modifying code (code cache invalidation) ------
            int base3 = 0x80030000;
            int[] p = new int[12];
            p[0] = I(0x0F, 0, 1, (base3 >>> 16) & 0xFFFF);
            p[1] = I(0x0D, 1, 1, base3 & 0xFFFF);
            p[2] = I(9, 0, 9, 0x1234);
            p[3] = I(0x2B, 1, 9, 0x2C);
            for (int i = 4; i < 11; i++) p[i] = 0;
            p[11] = 0x240A0000;
            putSpin(bus, base3, p);
            bus.write32(base3 + 0x2C, 0x240A1234);
            cpu.reset();
            cpu.pc = base3;
            runLoop(m, 1);
            check("self modifying code patched", cpu.gpr[10] == 0x1234, "t2=" + cpu.gpr[10]);

        // ---------- CPU: syscall exception -----------------------------------
            int[] pe = new int[4];
            pe[0] = R(0, 0, 0, 0, 0, 0x0C);          // syscall
            pe[1] = I(9, 0, 5, 42);                  // delay slot
            pe[2] = 0;
            pe[3] = 0;
            putSpin(bus, base, pe);
            cpu.reset();
            cpu.pc = base;
            runLoop(m, 1);
            check("syscall exception", cpu.cop0[14] == base && ((cpu.cop0[13] >> 2) & 0x1F) == 8,
                    "epc=" + Integer.toHexString(cpu.cop0[14]) + " cause=" + Integer.toHexString(cpu.cop0[13]));

        // ---------- SPU -------------------------------------------------------
            Spu spu = new Spu();
            spu.reset();
            for (int i = 0; i < 64; i++) {
                spu.ram[i * 16] = 0x00;             // shift 0, filter 0
                for (int j = 0; j < 14; j++) spu.ram[i * 16 + 1 + j] = (byte) (j * 2);
            }
            spu.write(0x1F801C06, 0);               // start addr voice0 = 0
            spu.write(0x1F801C00, 0x3FFF);          // vol L
            spu.write(0x1F801C02, 0x3FFF);          // vol R
            spu.write(0x1F801C04, 0x1000);          // pitch = 1.0
            spu.write(0x1F801C08, 0x00FF);          // adsr lo
            spu.write(0x1F801C0A, 0x7F00);          // adsr hi (fast attack)
            spu.write(0x1F80188A, 0);
            spu.write(0x1F80188C, 0);
            spu.write(0x1F801D80, 0x3FFF);
            spu.write(0x1F801D82, 0x3FFF);
            spu.write(0x1F801DAA, 0x8000);          // SPU enable
            spu.write(0x1F801D88, 0x0001);          // key on voice 0
            for (int i = 0; i < 200; i++) spu.step(768);
            check("spu produces audio", spu.available() > 0, "bytes=" + spu.available());
        }

        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " FAILURES");
        if (failures != 0) System.exit(1);
    }

    static int prog2_dummy() { return 0; }
}
