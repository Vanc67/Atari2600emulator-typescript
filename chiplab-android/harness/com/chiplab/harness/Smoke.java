package com.chiplab.harness;

import com.chiplab.ChipBin;
import com.chiplab.Cpu;
import com.chiplab.DieLayout;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;

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
        ChipBin customMax = ChipBin.buildCustom(ChipBin.TYPE_CPU, 16, 16, 1024, 64, 64, "ТЕСТ-MAX");
        ChipBin customMaxParsed = ChipBin.parse(customMax.toBytes());
        Cpu cm = new Cpu(customMaxParsed);
        cm.run(100000);
        boolean blockTrOk = customMaxParsed.transistors <= 65535;
        for (int i = 0; i < customMaxParsed.blocks.length; i++) {
            if (customMaxParsed.blocks[i].transistors > 65535) blockTrOk = false;
        }
        check(blockTrOk && cm.litPixels() > 1000 && !cm.halted,
                "maximum constructor chip (16-bit/16 regs/1024/64x64) works");

        // Bad files are rejected with a message, not a crash of the parser caller
        boolean rejected = false;
        try { ChipBin.parse(new byte[]{1, 2, 3}); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "parser rejects garbage");

        // Expanded bundled library: every .bin in assets must parse, run
        // without leaving its program, and light its screen (if it has one).
        File[] assetFiles = new File(dir).listFiles();
        Arrays.sort(assetFiles);
        ArrayList<ChipBin> bundled = new ArrayList<ChipBin>();
        boolean allRunOk = true, allScreensOk = true;
        String badChip = "";
        int screenChips = 0;
        for (int i = 0; i < assetFiles.length; i++) {
            File f = assetFiles[i];
            if (!f.getName().endsWith(".bin")) continue;
            try {
                ChipBin chip = ChipBin.parse(read(f));
                bundled.add(chip);
                if (chip.name.length() == 0 || chip.blocksOrDefault().length == 0) {
                    allRunOk = false;
                    badChip = f.getName();
                }
                Cpu sim = new Cpu(chip);
                if (chip.isCpuLike()) {
                    sim.run(chip.hasScreen() ? 30000 : 5000);
                    if (!sim.halted && (sim.pc < 0 || sim.pc >= chip.program.length)) {
                        allRunOk = false;
                        badChip = f.getName() + " pc=" + sim.pc;
                    }
                    if (chip.hasScreen()) {
                        screenChips++;
                        if (sim.litPixels() == 0) {
                            allScreensOk = false;
                            badChip = f.getName() + " blank screen";
                        }
                    }
                } else if (sim.mem.length != chip.memSize) {
                    allRunOk = false;
                    badChip = f.getName();
                }
            } catch (Exception e) {
                allRunOk = false;
                badChip = f.getName() + ": " + e.getMessage();
            }
        }
        check(bundled.size() >= 80, "expanded library has 80+ bundled chips (" + bundled.size() + ")");
        check(allRunOk, "every bundled chip parses and runs" + (badChip.length() == 0 ? "" : " BAD=" + badChip));
        check(allScreensOk && screenChips >= 30, "every bundled screen lights pixels (screens=" + screenChips + ")");

        // Interactive devices consume the gamepad cell 250 in their own CPU.
        ChipBin tetris = ChipBin.parse(read(new File(dir, "57_tetris.bin")));
        Cpu tet = new Cpu(tetris);
        tet.mem[250] = 2; // right
        tet.run(1000);
        check(tet.mem[32] == 8 && tet.mem[250] == 0,
                "Tetris gamepad moves block and clears input (x=" + tet.mem[32] + ")");
        ChipBin tamaBin = ChipBin.parse(read(new File(dir, "58_tamagotchi.bin")));
        Cpu tama = new Cpu(tamaBin);
        tama.mem[250] = 16; // A = feed
        tama.run(1000);
        check(tama.mem[34] == 14 && tama.mem[250] == 0,
                "Tamagotchi A button feeds pet (hunger=" + tama.mem[34] + ")");
        ChipBin snakeBin = ChipBin.parse(read(new File(dir, "59_snake.bin")));
        Cpu snake = new Cpu(snakeBin);
        snake.mem[250] = 4; // up
        snake.run(1000);
        check(snake.mem[33] == 7 && snake.mem[250] == 0,
                "Snake gamepad turns head up (y=" + snake.mem[33] + ")");
        ChipBin calcBin = ChipBin.parse(read(new File(dir, "66_calculator.bin")));
        Cpu calc = new Cpu(calcBin);
        calc.mem[250] = 16; // A = +1
        calc.run(1000);
        check(calc.mem[32] == 1 && calc.mem[250] == 0,
                "Calculator A button increments value (v=" + calc.mem[32] + ")");

        // DieLayout: realistic silicon geometry exists for every chip, is
        // deterministic, stays on the die, fills the floorplan and reacts
        // to the actual simulated state.
        bundled.add(custom);
        bundled.add(customScr);
        bundled.add(customMaxParsed);
        ChipBin[] all = bundled.toArray(new ChipBin[bundled.size()]);
        boolean countsOk = true, boundsOk = true, partOk = true, detOk = true;
        int minSegs = Integer.MAX_VALUE, maxSegs = 0;
        float minOcc = 1f;
        for (int i = 0; i < all.length; i++) {
            DieLayout d = DieLayout.wholeDie(all[i]);
            minSegs = Math.min(minSegs, d.segs.size());
            maxSegs = Math.max(maxSegs, d.segs.size());
            if (d.segs.size() < 3000 || d.segs.size() > 150000) countsOk = false;
            if (d.staticSegs.size() + d.dynamicSegs.size() != d.segs.size()) partOk = false;
            if (d.dynamicSegs.size() == 0) partOk = false;
            boolean[] tiles = new boolean[32 * 32];
            for (int j = 0; j < d.segs.size(); j++) {
                DieLayout.Seg sg = d.segs.get(j);
                if (sg.x < -0.01f || sg.y < -0.01f || sg.x + sg.w > d.widthU + 0.01f
                        || sg.y + sg.h > d.heightU + 0.01f || sg.w <= 0 || sg.h <= 0) boundsOk = false;
                int tx0 = Math.max(0, Math.min(31, (int) (sg.x / d.widthU * 32)));
                int ty0 = Math.max(0, Math.min(31, (int) (sg.y / d.heightU * 32)));
                int tx1 = Math.max(0, Math.min(31, (int) ((sg.x + sg.w) / d.widthU * 32)));
                int ty1 = Math.max(0, Math.min(31, (int) ((sg.y + sg.h) / d.heightU * 32)));
                for (int ty = ty0; ty <= ty1; ty++)
                    for (int tx = tx0; tx <= tx1; tx++) tiles[ty * 32 + tx] = true;
            }
            int marked = 0;
            for (int j = 0; j < tiles.length; j++) if (tiles[j]) marked++;
            minOcc = Math.min(minOcc, marked / (float) tiles.length);
            if (i % 8 == 0 && DieLayout.wholeDie(all[i]).segs.size() != d.segs.size()) detOk = false;
        }
        check(countsOk, "DieLayout segment counts sane (min=" + minSegs + ", max=" + maxSegs + ")");
        check(partOk, "DieLayout static+dynamic partition complete");
        check(boundsOk, "DieLayout all segments inside die bounds");
        check(detOk, "DieLayout deterministic for same chip");
        check(minOcc > 0.80f, "DieLayout floorplan occupancy >80% (min=" + minOcc + ")");

        DieLayout d8 = DieLayout.wholeDie(c8);
        Cpu live = new Cpu(c8);
        DieLayout.Ctx ctxA = new DieLayout.Ctx(c8, live);
        int[] before = new int[d8.dynamicSegs.size()];
        for (int j = 0; j < before.length; j++) before[j] = DieLayout.colorFor(d8.dynamicSegs.get(j), ctxA);
        live.run(300);
        DieLayout.Ctx ctxB = new DieLayout.Ctx(c8, live);
        int changed = 0;
        for (int j = 0; j < d8.dynamicSegs.size(); j++) {
            if (before[j] != DieLayout.colorFor(d8.dynamicSegs.get(j), ctxB)) changed++;
        }
        check(changed > 20, "DieLayout colours react to simulation (changed=" + changed + ")");

        ChipBin.Block aluBlk = null, regsBlk = null;
        ChipBin.Block[] bs8 = c8.blocksOrDefault();
        for (int i = 0; i < bs8.length; i++) {
            if (bs8[i].kind == ChipBin.KIND_ALU && aluBlk == null) aluBlk = bs8[i];
            if (bs8[i].kind == ChipBin.KIND_REGS && regsBlk == null) regsBlk = bs8[i];
        }
        check(aluBlk != null && DieLayout.blockDetail(c8, aluBlk, true).segs.size() > 300,
                "DieLayout ALU macro detail generated");
        boolean regColourOk = false;
        if (regsBlk != null) {
            DieLayout rd = DieLayout.blockDetail(c8, regsBlk, true);
            Cpu rc = new Cpu(c8);
            DieLayout.Ctx cOff = new DieLayout.Ctx(c8, rc);
            int[] off = new int[rd.dynamicSegs.size()];
            for (int j = 0; j < off.length; j++) off[j] = DieLayout.colorFor(rd.dynamicSegs.get(j), cOff);
            rc.regs[0] = c8.mask();
            rc.lastRegWrite = 0;
            DieLayout.Ctx cOn = new DieLayout.Ctx(c8, rc);
            for (int j = 0; j < rd.dynamicSegs.size() && !regColourOk; j++) {
                if (off[j] != DieLayout.colorFor(rd.dynamicSegs.get(j), cOn)) regColourOk = true;
            }
        }
        check(regColourOk, "DieLayout register cells light up with real bits");

        System.out.println(failed == 0 ? "[Smoke] ALL PASS" : "[Smoke] " + failed + " FAILURES");
        System.exit(failed == 0 ? 0 : 1);
    }
}
