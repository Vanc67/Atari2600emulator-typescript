package com.chiplab.harness;

import com.chiplab.ChipBin;
import com.chiplab.Cpu;

import java.io.File;
import java.io.FileInputStream;

/** Desktop Smoke test: parses every bundled .bin and actually runs the CPUs. */
public class Smoke {
    static int failed = 0;

    static void check(boolean cond, String msg) {
        System.out.println((cond ? "  ok   " : "  FAIL ") + msg);
        if (!cond) failed++;
    }

    static byte[] read(File f) throws Exception {
        FileInputStream in = new FileInputStream(f);
        byte[] data = new byte[(int) f.length()];
        int off = 0, n;
        while (off < data.length && (n = in.read(data, off, data.length - off)) > 0) off += n;
        in.close();
        return data;
    }

    static boolean outContains(Cpu cpu, int v) {
        for (int i = 0; i < cpu.outLogSize; i++) if (cpu.outLog[i] == v) return true;
        return false;
    }

    static boolean memContains(Cpu cpu, int v) {
        for (int i = 0; i < cpu.mem.length; i++) if (cpu.mem[i] == v) return true;
        return false;
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "assets";
        System.out.println("[Smoke] assets dir: " + dir);

        // 1-bit: toggles, carry happens, OUT saw both 0 and 1
        ChipBin c1 = ChipBin.parse(read(new File(dir, "01_cpu1.bin")));
        System.out.println("[Smoke] " + c1.name + " bits=" + c1.bits + " prog=" + c1.program.length);
        Cpu cpu1 = new Cpu(c1);
        boolean sawCarry = false;
        for (int i = 0; i < 200; i++) { cpu1.step(); if (cpu1.flagC) sawCarry = true; }
        check(c1.bits == 1 && c1.type == ChipBin.TYPE_CPU, "cpu1 header");
        check(outContains(cpu1, 0) && outContains(cpu1, 1), "cpu1 OUT toggles 0/1");
        check(sawCarry, "cpu1 carry flag seen (1+1=0,C=1)");

        // 4-bit: counts to 15 then HALT, mem[i]=i
        ChipBin c4 = ChipBin.parse(read(new File(dir, "02_cpu4.bin")));
        Cpu cpu4 = new Cpu(c4);
        cpu4.run(1000);
        check(cpu4.halted, "cpu4 halted");
        check(outContains(cpu4, 15), "cpu4 counted to 15");
        check(cpu4.mem[7] == 7 && cpu4.mem[15] == 15, "cpu4 mem[i]=i");

        // 8-bit: Fibonacci into RAM
        ChipBin c8 = ChipBin.parse(read(new File(dir, "03_cpu8.bin")));
        Cpu cpu8 = new Cpu(c8);
        cpu8.run(5000);
        check(cpu8.halted, "cpu8 halted");
        check(cpu8.mem[6] == 8 && cpu8.mem[7] == 13 && cpu8.mem[11] == 89,
                "cpu8 Fibonacci in RAM (mem[7]=" + cpu8.mem[7] + ", mem[11]=" + cpu8.mem[11] + ")");
        check(outContains(cpu8, 13), "cpu8 OUT 13");

        // 16-bit: sum 1..100 = 5050
        ChipBin c16 = ChipBin.parse(read(new File(dir, "04_cpu16.bin")));
        Cpu cpu16 = new Cpu(c16);
        cpu16.run(5000);
        check(cpu16.halted && cpu16.mem[0] == 5050, "cpu16 sum=5050 (got " + cpu16.mem[0] + ")");
        check(outContains(cpu16, 5050), "cpu16 OUT 5050");

        // RAM chips: initial data loaded, no CPU run
        ChipBin r1 = ChipBin.parse(read(new File(dir, "05_ram16x4.bin")));
        Cpu ram = new Cpu(r1);
        check(r1.type == ChipBin.TYPE_RAM && ram.mem[9] == 9 && ram.mem.length == 16, "ram16x4 init data");
        ChipBin r2 = ChipBin.parse(read(new File(dir, "06_sram256.bin")));
        Cpu ram2 = new Cpu(r2);
        check(ram2.mem.length == 256 && ram2.mem[2] == 14, "sram256 init pattern (2*7=14)");

        // GPU: animated framebuffer fills up
        ChipBin g = ChipBin.parse(read(new File(dir, "07_gpu32.bin")));
        Cpu gpu = new Cpu(g);
        gpu.run(30000);
        check(g.hasScreen() && gpu.framebuffer.length == 512, "gpu framebuffer 32x16");
        check(gpu.litPixels() > 100, "gpu lit pixels=" + gpu.litPixels());

        // Display
        ChipBin s = ChipBin.parse(read(new File(dir, "08_screen16.bin")));
        Cpu scr = new Cpu(s);
        scr.run(20000);
        check(scr.litPixels() > 50, "screen lit pixels=" + scr.litPixels());

        // Roundtrip: parse -> toBytes -> parse keeps identity
        byte[] again = c8.toBytes();
        ChipBin c8b = ChipBin.parse(again);
        check(c8b.name.equals(c8.name) && c8b.program.length == c8.program.length
                && c8b.blocks.length == c8.blocks.length && c8b.bits == 8, "roundtrip cpu8");

        // Constructor chips run too
        ChipBin custom = ChipBin.buildCustom(ChipBin.TYPE_CPU, 8, 4, 64, 0, 0, "ТЕСТ-8");
        Cpu cc = new Cpu(ChipBin.parse(custom.toBytes()));
        cc.run(2000);
        check(cc.halted && cc.lastOut == 255 && cc.mem[0] == 192, "custom counter chip runs (lastOut=" + cc.lastOut + ")");
        ChipBin customScr = ChipBin.buildCustom(ChipBin.TYPE_GPU, 4, 4, 64, 16, 16, "ТЕСТ-GPU");
        Cpu cg = new Cpu(ChipBin.parse(customScr.toBytes()));
        cg.run(20000);
        check(cg.litPixels() > 30, "custom screen chip draws (" + cg.litPixels() + ")");

        // Bad files are rejected with a message, not a crash of the parser caller
        boolean rejected = false;
        try { ChipBin.parse(new byte[]{1, 2, 3}); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "parser rejects garbage");

        System.out.println(failed == 0 ? "[Smoke] ALL PASS" : "[Smoke] " + failed + " FAILURES");
        System.exit(failed == 0 ? 0 : 1);
    }
}
