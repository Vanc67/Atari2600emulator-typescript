package com.chiplab.harness;

import com.chiplab.ChipBin;
import com.chiplab.Cpu;
import com.chiplab.DieLayout;

import java.io.FileOutputStream;

/**
 * Rasterises DieLayout geometry to PPM files (same colours as the game),
 * so the die graphics can be eyeballed on a desktop without a phone.
 * Usage: RenderDie <assetsDir> <outDir>
 */
public class RenderDie {
    static int blendOver(int dst, int src) {
        int sa = (src >>> 24) & 0xFF;
        if (sa == 0) return dst;
        if (sa == 255) return src | 0xFF000000;
        int da = 255 - sa;
        int r = (((src >> 16) & 0xFF) * sa + ((dst >> 16) & 0xFF) * da) / 255;
        int g = (((src >> 8) & 0xFF) * sa + ((dst >> 8) & 0xFF) * da) / 255;
        int b = ((src & 0xFF) * sa + (dst & 0xFF) * da) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static void render(DieLayout layout, DieLayout.Ctx ctx, int W, String outPath) throws Exception {
        float scale = W / layout.widthU;
        int H = Math.max(2, (int) (layout.heightU * scale));
        int[] px = new int[W * H];
        for (int i = 0; i < px.length; i++) px[i] = 0xFF05030F;
        for (DieLayout.Seg s : layout.segs) {
            int col = DieLayout.colorFor(s, ctx);
            int x0 = Math.max(0, (int) (s.x * scale));
            int y0 = Math.max(0, (int) (s.y * scale));
            int x1 = Math.min(W, Math.max(x0 + 1, (int) ((s.x + s.w) * scale)));
            int y1 = Math.min(H, Math.max(y0 + 1, (int) ((s.y + s.h) * scale)));
            for (int y = y0; y < y1; y++) {
                int off = y * W;
                for (int x = x0; x < x1; x++) px[off + x] = blendOver(px[off + x], col);
            }
        }
        FileOutputStream fos = new FileOutputStream(outPath);
        fos.write(("P6\n" + W + " " + H + "\n255\n").getBytes("ASCII"));
        byte[] row = new byte[W * 3];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int c = px[y * W + x];
                row[x * 3] = (byte) ((c >> 16) & 0xFF);
                row[x * 3 + 1] = (byte) ((c >> 8) & 0xFF);
                row[x * 3 + 2] = (byte) (c & 0xFF);
            }
            fos.write(row);
        }
        fos.close();
        System.out.println("[render] " + outPath + " segs=" + layout.segs.size() + " " + W + "x" + H);
    }

    static void renderScreen(ChipBin chip, Cpu cpu, int scale, String outPath) throws Exception {
        int W = chip.screenW * scale, H = chip.screenH * scale;
        FileOutputStream fos = new FileOutputStream(outPath);
        fos.write(("P6\n" + W + " " + H + "\n255\n").getBytes("ASCII"));
        byte[] row = new byte[W * 3];
        for (int y = 0; y < H; y++) {
            int sy = y / scale;
            for (int x = 0; x < W; x++) {
                int sx = x / scale;
                int v = cpu.framebuffer[sy * chip.screenW + sx] & 15;
                int c = DieLayout.SCREEN_PALETTE[v];
                row[x * 3] = (byte) ((c >> 16) & 0xFF);
                row[x * 3 + 1] = (byte) ((c >> 8) & 0xFF);
                row[x * 3 + 2] = (byte) (c & 0xFF);
            }
            fos.write(row);
        }
        fos.close();
        System.out.println("[render] " + outPath + " screen " + W + "x" + H
                + " lit=" + cpu.litPixels() + " cycles=" + cpu.cycles);
    }

    static void pressAndRun(Cpu cpu, int mask, int steps) {
        if (cpu.mem.length > 250) cpu.mem[250] = mask;
        cpu.run(steps);
    }

    /** Run to the next framebuffer wrap so screenshots are complete frames. */
    static void finishFrame(Cpu cpu) {
        if (cpu.framebuffer == null) return;
        int guard = 0;
        while (cpu.fbCursor != 0 && guard++ < 5000) cpu.run(100);
    }

    static ChipBin load(String dir, String name) throws Exception {
        java.io.File f = new java.io.File(dir, name);
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        byte[] data = new byte[(int) f.length()];
        int off = 0, n;
        while (off < data.length && (n = in.read(data, off, data.length - off)) > 0) off += n;
        in.close();
        return ChipBin.parse(data);
    }

    static ChipBin.Block findKind(ChipBin c, int kind) {
        ChipBin.Block[] bs = c.blocksOrDefault();
        for (ChipBin.Block b : bs) if (b.kind == kind) return b;
        return bs[0];
    }

    public static void main(String[] args) throws Exception {
        String dir = args[0], out = args[1];
        new java.io.File(out).mkdirs();

        ChipBin cpu8 = load(dir, "03_cpu8.bin");
        Cpu c8 = new Cpu(cpu8);
        c8.run(120);
        render(DieLayout.wholeDie(cpu8), new DieLayout.Ctx(cpu8, c8), 900, out + "/die_cpu8.ppm");
        render(DieLayout.blockDetail(cpu8, findKind(cpu8, ChipBin.KIND_ALU), true),
                new DieLayout.Ctx(cpu8, c8), 900, out + "/die_cpu8_alu_big.ppm");
        render(DieLayout.blockDetail(cpu8, findKind(cpu8, ChipBin.KIND_RAM), true),
                new DieLayout.Ctx(cpu8, c8), 900, out + "/die_cpu8_ram_big.ppm");
        render(DieLayout.blockDetail(cpu8, findKind(cpu8, ChipBin.KIND_CONTROL), false),
                new DieLayout.Ctx(cpu8, c8), 900, out + "/die_cpu8_ctrl.ppm");

        ChipBin gpu = load(dir, "07_gpu32.bin");
        Cpu cg = new Cpu(gpu);
        cg.run(15000);
        render(DieLayout.wholeDie(gpu), new DieLayout.Ctx(gpu, cg), 900, out + "/die_gpu.ppm");

        ChipBin ram = load(dir, "06_sram256.bin");
        Cpu cr = new Cpu(ram);
        cr.lastMemAddr = 0x5A;
        cr.lastMemWrite = true;
        render(DieLayout.wholeDie(ram), new DieLayout.Ctx(ram, cr), 900, out + "/die_sram256.ppm");

        // Interactive devices: staged gamepad presses through memory cell 250,
        // exactly like MainActivity.pressPad does on the phone.
        ChipBin tetris = load(dir, "57_tetris.bin");
        Cpu tet = new Cpu(tetris);
        int[] tetMoves = {1, 1, 8, 2, 2, 2, 8, 16, 1, 8, 2, 1};
        for (int i = 0; i < 96; i++) pressAndRun(tet, tetMoves[i % tetMoves.length], 20000);
        finishFrame(tet);
        renderScreen(tetris, tet, 28, out + "/screen_tetris.ppm");
        render(DieLayout.wholeDie(tetris), new DieLayout.Ctx(tetris, tet), 900, out + "/die_tetris.ppm");

        ChipBin tama = load(dir, "58_tamagotchi.bin");
        Cpu pet = new Cpu(tama);
        int[] petMoves = {16, 2, 32, 1, 16, 32};
        for (int i = 0; i < 48; i++) pressAndRun(pet, petMoves[i % petMoves.length], 12000);
        finishFrame(pet);
        renderScreen(tama, pet, 34, out + "/screen_tamagotchi.ppm");
        render(DieLayout.wholeDie(tama), new DieLayout.Ctx(tama, pet), 900, out + "/die_tamagotchi.ppm");

        ChipBin snake = load(dir, "59_snake.bin");
        Cpu sn = new Cpu(snake);
        int[] snakeMoves = {2, 2, 4, 4, 1, 1, 8, 8, 2, 4};
        for (int i = 0; i < snakeMoves.length; i++) pressAndRun(sn, snakeMoves[i], 25000);
        finishFrame(sn);
        renderScreen(snake, sn, 34, out + "/screen_snake.ppm");

        ChipBin pc = load(dir, "71_mini_pc.bin");
        Cpu desktop = new Cpu(pc);
        int[] pcMoves = {2, 2, 8, 2, 8, 1};
        for (int i = 0; i < pcMoves.length; i++) pressAndRun(desktop, pcMoves[i], 30000);
        finishFrame(desktop);
        renderScreen(pc, desktop, 22, out + "/screen_mini_pc.ppm");
        render(DieLayout.wholeDie(pc), new DieLayout.Ctx(pc, desktop), 900, out + "/die_mini_pc.ppm");
    }
}
