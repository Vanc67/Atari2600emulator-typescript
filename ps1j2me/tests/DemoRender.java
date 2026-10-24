import java.io.File;
import java.io.FileOutputStream;

import psx.PS1Machine;
import psx.CdImage;
import psx.MemoryCD;
import psx.VirtualCD;

/**
 * Renders the bundled demo discs and writes BMP snapshots so the GPU output
 * can be inspected without a device.
 *
 *   java -cp build/tests DemoRender res/demos build/shots
 */
public class DemoRender {

    static void writeBmp(File out, int[] rgb, int w, int h) throws Exception {
        int rowSize = ((w * 3 + 3) / 4) * 4;
        int dataSize = rowSize * h;
        byte[] header = new byte[54];
        header[0] = 'B';
        header[1] = 'M';
        le(header, 2, 54 + dataSize);
        le(header, 10, 54);
        le(header, 14, 40);
        le(header, 18, w);
        le(header, 22, h);
        header[26] = 1;
        header[28] = 24;
        le(header, 34, dataSize);
        FileOutputStream fos = new FileOutputStream(out);
        fos.write(header);
        byte[] row = new byte[rowSize];
        for (int y = h - 1; y >= 0; y--) {
            int o = 0;
            for (int x = 0; x < w; x++) {
                int c = rgb[y * w + x];
                /* frame is 0xAARRGGBB, BMP rows are BGR */
                row[o++] = (byte) (c & 0xFF);
                row[o++] = (byte) ((c >>> 8) & 0xFF);
                row[o++] = (byte) ((c >>> 16) & 0xFF);
            }
            fos.write(row);
        }
        fos.close();
    }

    static byte[] readAll(File f) throws Exception {
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        byte[] buf = new byte[(int) f.length()];
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n <= 0) break;
            off += n;
        }
        in.close();
        return buf;
    }

    static void le(byte[] b, int o, int v) {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >>> 8);
        b[o + 2] = (byte) (v >>> 16);
        b[o + 3] = (byte) (v >>> 24);
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "res/demos";
        String outDir = args.length > 1 ? args[1] : "build/shots";
        new File(outDir).mkdirs();
        String[] names = { "demo_pong", "demo_pad", "demo_shapes", "demo_tex",
                           "demo_logo", "demo_3d", "demo_sound" };
        int failures = 0;
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            File iso = new File(dir, name + ".iso");
            if (!iso.isFile()) {
                System.out.println("SKIP " + name + ": no image");
                continue;
            }
            CdImage cd = new VirtualCD();
            ((VirtualCD) cd).open("file://" + iso.getAbsolutePath());
            if (name.equals("demo_pong")) {
                /* also cover the in-memory image used for JAR resources */
                byte[] raw = readAll(iso);
                cd = new MemoryCD(raw, name);
            }
            PS1Machine m = new PS1Machine(cd);
            m.setViewport(320, 240);
            boolean ok = m.hleBoot(cd);
            if (!ok) {
                System.out.println("FAIL " + name + ": boot failed");
                failures++;
                cd.close();
                continue;
            }
            long t0 = System.currentTimeMillis();
            for (int f = 0; f < 40; f++) m.runFrame();
            long dt = System.currentTimeMillis() - t0;
            int[] frame = m.getFrame();
            int nonBg = 0;
            for (int p = 0; p < frame.length; p++) {
                if ((frame[p] & 0xFFFFFF) != 0) nonBg++;
            }
            writeBmp(new File(outDir, name + ".bmp"), frame, 320, 240);
            String extra = "";
            if (name.equals("demo_sound")) {
                extra = " spuBytes=" + m.spu.available();
            }
            System.out.println((nonBg > 500 ? "PASS " : "WARN ") + name
                    + " drawnPixels=" + nonBg + " " + dt + "ms/40f" + extra);
            if (nonBg <= 500) failures++;
            cd.close();
        }
        System.out.println(failures == 0 ? "ALL DEMOS RENDERED" : failures + " DEMO FAILURES");
        if (failures != 0) System.exit(1);
    }
}
