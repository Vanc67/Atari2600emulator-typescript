import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.microedition.lcdui.Graphics;

import psx.MainCanvas;

/**
 * Rasterises the UI screens into BMP files so the layout (and the built-in
 * bitmap font) can be eyeballed without a handset. Test-only helper: it uses a
 * Graphics subclass that paints into an int[] instead of the display.
 *
 * Usage: java -cp build/tests:build/tests/ui:. UiRender build/shots
 */
public class UiRender {

    static final int W = 176;
    static final int H = 208;

    static final class Pix extends Graphics {
        int[] buf;
        int color = 0x000000;

        Pix(int[] buf) { this.buf = buf; }

        public void setColor(int rgb) { color = rgb & 0xFFFFFF; }

        public void fillRect(int x, int y, int w, int h) {
            if (w <= 0 || h <= 0) return;
            int x0 = Math.max(0, x), y0 = Math.max(0, y);
            int x1 = Math.min(W, x + w), y1 = Math.min(H, y + h);
            for (int yy = y0; yy < y1; yy++) {
                int row = yy * W;
                for (int xx = x0; xx < x1; xx++) buf[row + xx] = color;
            }
        }

        public void drawRGB(int[] rgb, int off, int scan, int x, int y, int w, int h, boolean alpha) {
            for (int yy = 0; yy < h; yy++) {
                int dy = y + yy;
                if (dy < 0 || dy >= H) continue;
                for (int xx = 0; xx < w; xx++) {
                    int dx = x + xx;
                    if (dx < 0 || dx >= W) continue;
                    buf[dy * W + dx] = rgb[off + yy * scan + xx] & 0xFFFFFF;
                }
            }
        }

        public void drawRect(int x, int y, int w, int h) {
            fillRect(x, y, w, 1);
            fillRect(x, y + h - 1, w, 1);
            fillRect(x, y, 1, h);
            fillRect(x + w - 1, y, 1, h);
        }

        public void drawLine(int x1, int y1, int x2, int y2) {
            int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
            int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
            int err = dx - dy;
            while (true) {
                fillRect(x1, y1, 1, 1);
                if (x1 == x2 && y1 == y2) break;
                int e2 = err << 1;
                if (e2 > -dy) { err -= dy; x1 += sx; }
                if (e2 < dx) { err += dx; y1 += sy; }
            }
        }
    }

    static void setInt(Object o, String name, int v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(o, v);
    }

    static void set(Object o, String name, Object v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }

    static Object get(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void PaintGame(MainCanvas canvas, int[] pix) throws Exception {
        Method paintGame = MainCanvas.class.getDeclaredMethod("paintGame",
                new Class[] { Graphics.class });
        paintGame.setAccessible(true);
        paintGame.invoke(canvas, new Object[] { new Pix(pix) });
    }

    static void writeBmp(String path, int[] pix) throws IOException {
        int rowBytes = (W * 3 + 3) / 4 * 4;
        int size = 54 + rowBytes * H;
        byte[] out = new byte[size];
        out[0] = 'B'; out[1] = 'M';
        putLE(out, 2, size);
        putLE(out, 10, 54);
        putLE(out, 14, 40);
        putLE(out, 18, W);
        putLE(out, 22, H);
        out[26] = 1;
        out[28] = 24;
        putLE(out, 34, rowBytes * H);
        putLE(out, 38, 2835);
        putLE(out, 42, 2835);
        for (int y = 0; y < H; y++) {
            int src = (H - 1 - y) * W;
            int dst = 54 + y * rowBytes;
            for (int x = 0; x < W; x++) {
                int c = pix[src + x];
                out[dst + x * 3] = (byte) (c & 0xFF);
                out[dst + x * 3 + 1] = (byte) ((c >> 8) & 0xFF);
                out[dst + x * 3 + 2] = (byte) ((c >> 16) & 0xFF);
            }
        }
        FileOutputStream fo = new FileOutputStream(path);
        fo.write(out);
        fo.close();
    }

    static void putLE(byte[] b, int off, int v) {
        b[off] = (byte) (v & 0xFF);
        b[off + 1] = (byte) ((v >> 8) & 0xFF);
        b[off + 2] = (byte) ((v >> 16) & 0xFF);
        b[off + 3] = (byte) ((v >> 24) & 0xFF);
    }

    public static void main(String[] args) throws Exception {
        String dir = args.length > 0 ? args[0] : "build/shots";
        javax.microedition.lcdui.Displayable.STUB_WIDTH = W;
        javax.microedition.lcdui.Displayable.STUB_HEIGHT = H;
        MainCanvas canvas = new MainCanvas(null);
        setInt(canvas, "viewW", W);
        setInt(canvas, "viewH", H);
        /* The memory card scan runs on a worker thread; without waiting the
           first screens can flip to the "no card" message halfway through. */
        long deadline = System.currentTimeMillis() + 10000;
        while (!canvas.isDiscovered() && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(5); } catch (InterruptedException ie) { }
        }
        System.out.println("card scan finished: " + canvas.isDiscovered());

        Method paint = MainCanvas.class.getDeclaredMethod("paint", new Class[] { Graphics.class });
        paint.setAccessible(true);

        String[] files = { "ui_browser", "ui_demos", "ui_settings", "ui_help", "ui_pause",
                           "ui_message", "ui_info" };
        int[] modes = { 1, 6, 4, 5, 3, 7, 8 };
        String[] status = { "", "", "", "", "", "Не хватает памяти MIDlet для эмуляции.\nНужно около 5 МиБ. Уменьшите масштаб экрана и попробуйте снова.", "" };
        int[] sel = { 2, 0, 1, 0, 1, 0, 0 };
        for (int i = 0; i < modes.length; i++) {
            setInt(canvas, "mode", modes[i]);
            setInt(canvas, "selected", sel[i]);
            setInt(canvas, "demoSelection", sel[i]);
            setInt(canvas, "settingsSelection", sel[i]);
            setInt(canvas, "pauseSelection", sel[i]);
            setInt(canvas, "helpScroll", 0);
            set(canvas, "status", status[i]);
            set(canvas, "currentTitle", "/res/demos");
            int[] pix = new int[W * H];
            paint.invoke(canvas, new Object[] { new Pix(pix) });
            writeBmp(dir + "/" + files[i] + ".bmp", pix);
            System.out.println("wrote " + files[i] + ".bmp");
        }
        /* a real folder listing and the device picker */
        try {
            String base = System.getProperty("java.io.tmpdir") + "/uiRenderFiles";
            /* the names make FileRoots print the labels a real phone shows */
            java.io.File card = new java.io.File(base, "memorycard");
            java.io.File phone = new java.io.File(base, "root2");
            card.mkdirs();
            phone.mkdirs();
            new java.io.File(card, "rpg").mkdirs();
            new java.io.File(card, "Crash Bandicoot.bin").createNewFile();
            new java.io.File(card, "Final Fantasy VII.cue").createNewFile();
            new java.io.File(card, "Tekken 3.iso").createNewFile();
            new java.io.File(card, "readme.txt").createNewFile();

            Method listDirectory = MainCanvas.class.getDeclaredMethod("listDirectory",
                    new Class[] { String.class, String.class });
            listDirectory.setAccessible(true);
            listDirectory.invoke(canvas, new Object[] { card.getAbsolutePath() + "/", "games" });
            setInt(canvas, "selected", 2);
            int[] fpix = new int[W * H];
            paint.invoke(canvas, new Object[] { new Pix(fpix) });
            writeBmp(dir + "/ui_files.bmp", fpix);
            System.out.println("wrote ui_files.bmp");

            System.setProperty("fileconn.dir.memorycard", card.getAbsolutePath() + "/");
            System.setProperty("fileconn.dir.private", phone.getAbsolutePath() + "/");
            java.util.Vector roots = psx.FileRoots.discoverRoots();
            set(canvas, "roots", roots);
            Method openRoots = MainCanvas.class.getDeclaredMethod("openRoots", new Class[0]);
            openRoots.setAccessible(true);
            openRoots.invoke(canvas, new Object[0]);
            setInt(canvas, "selected", 0);
            int[] rpix = new int[W * H];
            paint.invoke(canvas, new Object[] { new Pix(rpix) });
            writeBmp(dir + "/ui_roots.bmp", rpix);
            System.out.println("wrote ui_roots.bmp (" + roots.size() + " roots)");
        } catch (Throwable t) {
            System.out.println("folder/roots render skipped: " + t);
        }

        /* the game screen must show real emulator output, not a black rectangle */
        /* the desktop has no MMAPI audio: keep the screenshot clean */
        psx.Config.get().sound = false;
        Method startDemo = MainCanvas.class.getDeclaredMethod("startDemo", new Class[] { int.class });
        startDemo.setAccessible(true);
        startDemo.invoke(canvas, new Object[] { new Integer(0) });
        Object mach = null;
        for (int wait = 0; wait < 200; wait++) {
            mach = get(canvas, "machine");
            if (mach != null && mach.getClass().getField("framesRun").getInt(mach) >= 60) break;
            Thread.sleep(50);
        }
        if (mach != null) {
            /* let the FPS counter settle so the screenshot shows a real value */
            Thread.sleep(700);
            int[] pix = new int[W * H];
            PaintGame(canvas, pix);
            writeBmp(dir + "/ui_game.bmp", pix);
            System.out.println("wrote ui_game.bmp");
        }
        System.out.println("UI RENDER DONE");
        System.exit(0);
    }
}
