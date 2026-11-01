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
    }
}
