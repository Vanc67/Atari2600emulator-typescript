package psx.droid;

import java.io.File;
import java.util.Vector;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * The whole user interface of the emulator, drawn by hand on a SurfaceView.
 *
 * <p>Three screens live here: the file browser, the running game and the
 * pause menu.  Nothing is shown in an Android dialog - a 2.3 phone with a
 * small screen and a slow CPU is much better served by one full screen
 * surface that we control completely.</p>
 *
 * <p>All emulation happens on the thread this class starts; the UI thread
 * only sets flags through the {@code keyDown}/{@code touch} entry points.</p>
 */
public final class EmuView extends SurfaceView implements SurfaceHolder.Callback,
        Runnable {

    public static final int MODE_BROWSER = 0;
    public static final int MODE_GAME = 1;
    public static final int MODE_MENU = 2;

    private static final long FRAME_NS = 1000000000L / 60L;

    private static final int BG = 0xFF101418;
    private static final int FG = 0xFFE8ECF0;
    private static final int DIM = 0xFF8090A0;
    private static final int ACCENT = 0xFFFFC040;
    private static final int SEL_BG = 0xFF204060;

    private final Settings settings;
    private final Engine engine;
    private final SurfaceHolder holder;

    private Thread thread;
    private volatile boolean running;
    private volatile boolean surfaceReady;
    private volatile boolean paused;

    private int mode = MODE_BROWSER;

    private Bitmap bitmap;
    private int bitmapW, bitmapH;
    private final Paint blitPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint fill = new Paint();

    /* browser state */
    private File dir;
    private String[] labels = new String[0];
    private int[] kinds = new int[0];       /* 0 up, 1 dir, 2 image, 3 demo, 4 sd */
    private int cursor;
    private int top;
    private boolean showAll;
    private String notice = "";

    /* menu state */
    private int menuIndex;
    private static final int MI_RESUME = 0;
    private static final int MI_DISC = 1;
    private static final int MI_SOUND = 2;
    private static final int MI_SKIP = 3;
    private static final int MI_SCALE = 4;
    private static final int MI_TOUCH = 5;
    private static final int MI_VOLUME = 6;
    private static final int MI_ABOUT = 7;
    private static final int MENU_ITEMS = 8;

    /* input */
    private int hardwarePad;
    private int touchPad;
    private int touchPointer = -1;

    /* touch pad geometry, computed when the surface size changes */
    private int w, h;
    private int padCx, padCy, padR;
    private int faceCx, faceCy, faceR;

    /* frame timing */
    private int fps, fpsCount;
    private long fpsStart;
    private int skipped;

    private final Activity activity;

    public EmuView(Activity context, Settings settings) {
        super(context);
        this.activity = context;
        this.settings = settings;
        this.engine = new Engine(settings, context.getAssets());
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
        textPaint.setAntiAlias(false);
        textPaint.setTypeface(Typeface.MONOSPACE);
        textPaint.setColor(FG);
        fill.setStyle(Paint.Style.FILL);
        blitPaint.setFilterBitmap(false);
        blitPaint.setDither(false);
        openBrowser(defaultDir());
    }

    /* ==================================================================== */
    /*  surface lifecycle                                                   */
    /* ==================================================================== */

    public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        startThread();
    }

    public void surfaceChanged(SurfaceHolder h, int format, int width, int height) {
        this.w = width;
        this.h = height;
        layoutPad();
    }

    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
        stopThread();
    }

    private void startThread() {
        if (running) return;
        running = true;
        thread = new Thread(this, "psx-emu");
        thread.setPriority(Thread.MAX_PRIORITY - 2);
        thread.start();
    }

    private void stopThread() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try { t.join(1500); } catch (InterruptedException e) { }
        }
    }

    public void shutdown() {
        stopThread();
        engine.shutdown();
    }

    /* ==================================================================== */
    /*  main loop                                                           */
    /* ==================================================================== */

    public void run() {
        long next = System.nanoTime();
        while (running) {
            if (!surfaceReady) {
                sleepMs(20);
                continue;
            }
            if (mode == MODE_GAME && !paused && engine.isRunning()) {
                engine.runFrame();
                skipped++;
                boolean draw = skipped > settings.frameskip;
                if (draw) {
                    skipped = 0;
                    fpsCount++;
                    render();
                }
                next += FRAME_NS;
                long now = System.nanoTime();
                long wait = next - now;
                if (wait > 0) {
                    sleepMs(wait / 1000000L);
                } else if (-wait > 4 * FRAME_NS) {
                    next = now;             /* far behind: do not over-sleep */
                }
                if (System.nanoTime() - fpsStart > 1000000000L) {
                    fps = fpsCount;
                    fpsCount = 0;
                    fpsStart = System.nanoTime();
                }
            } else {
                render();
                sleepMs(60);
            }
        }
    }

    private void sleepMs(long ms) {
        if (ms <= 0) return;
        try { Thread.sleep(ms); } catch (InterruptedException e) { }
    }

    /* ==================================================================== */
    /*  rendering                                                           */
    /* ==================================================================== */

    private void render() {
        if (!surfaceReady) return;
        Canvas c = null;
        try {
            c = holder.lockCanvas();
            if (c == null) return;
            if (w == 0) {
                w = c.getWidth();
                h = c.getHeight();
                layoutPad();
            }
            fill.setColor(BG);
            c.drawRect(0, 0, w, h, fill);
            if (mode == MODE_BROWSER) {
                drawBrowser(c);
            } else {
                drawGame(c);
                if (mode == MODE_MENU) drawMenu(c);
            }
        } catch (Throwable t) {
            /* a dead surface must never kill the emulator */
        } finally {
            if (c != null) {
                try { holder.unlockCanvasAndPost(c); } catch (Throwable t) { }
            }
        }
    }

    private void drawGame(Canvas c) {
        int[] frame = engine.frame();
        int fw = engine.frameWidth();
        int fh = engine.frameHeight();
        if (frame == null || fw <= 0 || fh <= 0) {
            drawCentered(c, "\u043d\u0435\u0442 \u043a\u0430\u0434\u0440\u0430", 16);
            return;
        }
        if (bitmap == null || bitmapW != fw || bitmapH != fh) {
            bitmap = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888);
            bitmapW = fw;
            bitmapH = fh;
        }
        bitmap.setPixels(frame, 0, fw, 0, 0, fw, fh);

        Rect dst = destRect(fw, fh);
        c.drawBitmap(bitmap, new Rect(0, 0, fw, fh), dst, blitPaint);

        if (settings.touchPad) drawPad(c);
        if (settings.showFps) {
            textPaint.setTextSize(12);
            textPaint.setColor(ACCENT);
            c.drawText(fps + " fps", 4, 14, textPaint);
        }
    }

    private Rect destRect(int fw, int fh) {
        int dw = w;
        int dh = h;
        if (settings.scaleMode == Settings.SCALE_STRETCH) {
            return new Rect(0, 0, dw, dh);
        }
        if (settings.scaleMode == Settings.SCALE_1X) {
            int x = (w - fw) / 2;
            int y = (h - fh) / 2;
            return new Rect(x, y, x + fw, y + fh);
        }
        /* fit: keep the pixel aspect of the emulated video mode */
        int scaledH = dw * fh / fw;
        if (scaledH <= dh) {
            int y = (dh - scaledH) / 2;
            return new Rect(0, y, dw, y + scaledH);
        }
        int scaledW = dh * fw / fh;
        int x = (dw - scaledW) / 2;
        return new Rect(x, 0, x + scaledW, dh);
    }

    /* ---- touch pad ---- */

    private void layoutPad() {
        int size = w < h ? w : h;
        padR = size / 6;
        if (padR < 26) padR = 26;
        if (padR > 64) padR = 64;
        padCx = padR + 8;
        padCy = h - padR - 8;
        faceR = padR * 2 / 3;
        if (faceR < 18) faceR = 18;
        faceCx = w - faceR * 2 - 8;
        faceCy = h - faceR * 2 - 8;
    }

    private void drawPad(Canvas c) {
        fill.setColor(0x60FFFFFF);
        int r = padR / 3;
        if (r < 10) r = 10;
        blob(c, padCx, padCy - padR, r);
        blob(c, padCx, padCy + padR, r);
        blob(c, padCx - padR, padCy, r);
        blob(c, padCx + padR, padCy, r);

        int d = faceR;
        blob(c, faceCx, faceCy - d, faceR / 2 + 4);
        blob(c, faceCx, faceCy + d, faceR / 2 + 4);
        blob(c, faceCx - d, faceCy, faceR / 2 + 4);
        blob(c, faceCx + d, faceCy, faceR / 2 + 4);

        fill.setColor(0x50FFFFFF);
        c.drawRect(w / 2 - 46, h - 30, w / 2 - 8, h - 8, fill);
        c.drawRect(w / 2 + 8, h - 30, w / 2 + 46, h - 8, fill);
        c.drawRect(6, 26, 44, 48, fill);
        c.drawRect(w - 44, 26, w - 6, 48, fill);

        textPaint.setTextSize(11);
        textPaint.setColor(0xC0FFFFFF);
        c.drawText("SEL", w / 2 - 44, h - 16, textPaint);
        c.drawText("STA", w / 2 + 12, h - 16, textPaint);
        c.drawText("L1", 14, 43, textPaint);
        c.drawText("R1", w - 36, 43, textPaint);
    }

    private void blob(Canvas c, int x, int y, int r) {
        c.drawCircle(x, y, r, fill);
    }

    /** Maps a screen point to a pad bit, or 0. */
    private int hitPad(int x, int y) {
        int r = padR / 3;
        if (r < 12) r = 12;
        r += 6;
        if (dist(x, y, padCx, padCy - padR) < r) return Keys.UP;
        if (dist(x, y, padCx, padCy + padR) < r) return Keys.DOWN;
        if (dist(x, y, padCx - padR, padCy) < r) return Keys.LEFT;
        if (dist(x, y, padCx + padR, padCy) < r) return Keys.RIGHT;
        int d = faceR;
        int fr = faceR / 2 + 8;
        if (dist(x, y, faceCx, faceCy - d) < fr) return Keys.TRIANGLE;
        if (dist(x, y, faceCx, faceCy + d) < fr) return Keys.CROSS;
        if (dist(x, y, faceCx - d, faceCy) < fr) return Keys.SQUARE;
        if (dist(x, y, faceCx + d, faceCy) < fr) return Keys.CIRCLE;
        if (x >= w / 2 - 52 && x <= w / 2 - 2 && y >= h - 36) return Keys.SELECT;
        if (x >= w / 2 + 2 && x <= w / 2 + 52 && y >= h - 36) return Keys.START;
        if (x >= 0 && x <= 52 && y >= 20 && y <= 54) return Keys.L1;
        if (x >= w - 52 && x <= w && y >= 20 && y <= 54) return Keys.R1;
        return 0;
    }

    private static int dist(int x, int y, int cx, int cy) {
        int dx = x - cx;
        int dy = y - cy;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    /* ---- browser ---- */

    private void drawBrowser(Canvas c) {
        textPaint.setTextSize(13);
        textPaint.setColor(ACCENT);
        c.drawText(showAll ? "\u0412\u0441\u0435 \u0444\u0430\u0439\u043b\u044b" : "\u041e\u0431\u0440\u0430\u0437\u044b \u0434\u0438\u0441\u043a\u043e\u0432",
                6, 18, textPaint);
        textPaint.setColor(DIM);
        textPaint.setTextSize(11);
        String path = dir == null ? "\u0434\u0435\u043c\u043e \u0432 APK" : dir.getPath();
        c.drawText(clip(path, w - 12), 6, 34, textPaint);

        int rowH = 18;
        int top0 = 42;
        int rows = (h - top0 - 22) / rowH;
        if (rows < 1) rows = 1;
        if (cursor < top) top = cursor;
        if (cursor >= top + rows) top = cursor - rows + 1;

        textPaint.setTextSize(13);
        for (int i = 0; i < rows; i++) {
            int idx = top + i;
            if (idx >= labels.length) break;
            int y = top0 + i * rowH;
            if (idx == cursor) {
                fill.setColor(SEL_BG);
                c.drawRect(0, y - 13, w, y + 4, fill);
            }
            textPaint.setColor(kinds[idx] == 2 || kinds[idx] == 3 ? FG : DIM);
            if (idx == cursor) textPaint.setColor(ACCENT);
            c.drawText(labels[idx], 6, y, textPaint);
        }

        textPaint.setTextSize(11);
        textPaint.setColor(DIM);
        String hint = "\u0432\u0432\u0435\u0440\u0445/\u0432\u043d\u0438\u0437 - \u0432\u044b\u0431\u043e\u0440   OK - \u043e\u0442\u043a\u0440\u044b\u0442\u044c   \u041c\u0415\u041d\u042e - \u0444\u0438\u043b\u044c\u0442\u0440";
        c.drawText(clip(hint, w - 12), 6, h - 6, textPaint);
        if (notice.length() > 0) {
            textPaint.setColor(0xFFFF6060);
            c.drawText(clip(notice, w - 12), 6, h - 20, textPaint);
        }
    }

    /* ---- menu ---- */

    private void drawMenu(Canvas c) {
        fill.setColor(0xC0000000);
        c.drawRect(0, 0, w, h, fill);
        textPaint.setTextSize(14);
        textPaint.setColor(ACCENT);
        c.drawText("\u041f\u0410\u0423\u0417\u0410", 10, 24, textPaint);
        textPaint.setColor(DIM);
        textPaint.setTextSize(11);
        c.drawText(clip(engine.title, w - 20), 10, 40, textPaint);

        int rowH = 20;
        int y = 64;
        textPaint.setTextSize(13);
        for (int i = 0; i < MENU_ITEMS; i++) {
            if (i == menuIndex) {
                fill.setColor(SEL_BG);
                c.drawRect(0, y - 14, w, y + 5, fill);
                textPaint.setColor(ACCENT);
            } else {
                textPaint.setColor(FG);
            }
            c.drawText(menuLabel(i), 10, y, textPaint);
            y += rowH;
        }
        if (notice.length() > 0) {
            textPaint.setColor(0xFFFF6060);
            textPaint.setTextSize(11);
            c.drawText(clip(notice, w - 20), 10, h - 8, textPaint);
        }
    }

    private String menuLabel(int i) {
        switch (i) {
            case MI_RESUME: return "\u041f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u044c";
            case MI_DISC: return "\u0421\u043c\u0435\u043d\u0438\u0442\u044c \u0434\u0438\u0441\u043a";
            case MI_SOUND: return "\u0417\u0432\u0443\u043a: " + (settings.sound ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b");
            case MI_SKIP: return "\u041f\u0440\u043e\u043f\u0443\u0441\u043a \u043a\u0430\u0434\u0440\u043e\u0432: " + settings.frameskip;
            case MI_SCALE: return "\u042d\u043a\u0440\u0430\u043d: " + scaleName();
            case MI_TOUCH: return "\u041a\u043d\u043e\u043f\u043a\u0438 \u043d\u0430 \u044d\u043a\u0440\u0430\u043d\u0435: "
                    + (settings.touchPad ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b");
            case MI_VOLUME: return "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c: " + settings.volume + "%";
            default: return "\u041e \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0435";
        }
    }

    private String scaleName() {
        if (settings.scaleMode == Settings.SCALE_STRETCH) return "\u0440\u0430\u0441\u0442\u044f\u043d\u0443\u0442\u044c";
        if (settings.scaleMode == Settings.SCALE_1X) return "1:1";
        return "\u043f\u043e \u0440\u0430\u0437\u043c\u0435\u0440\u0443";
    }

    /* ==================================================================== */
    /*  browser                                                             */
    /* ==================================================================== */

    private static File defaultDir() {
        try {
            File sd = Environment.getExternalStorageDirectory();
            if (sd != null && sd.exists()) return sd;
        } catch (Throwable t) { }
        return new File("/sdcard");
    }

    public void openBrowser(File start) {
        dir = start;
        cursor = 0;
        top = 0;
        notice = "";
        listDir();
        mode = MODE_BROWSER;
    }

    private void openDemoList() {
        dir = null;
        cursor = 0;
        top = 0;
        notice = "";
        String[] demos = DEMOS;
        labels = new String[demos.length + 1];
        kinds = new int[demos.length + 1];
        labels[0] = "\u2026 SD-\u043a\u0430\u0440\u0442\u0430";
        kinds[0] = 5;
        for (int i = 0; i < demos.length; i++) {
            labels[i + 1] = demos[i];
            kinds[i + 1] = 3;
        }
        mode = MODE_BROWSER;
    }

    private static final String[] DEMOS = {
        "demo_pong.iso", "demo_pad.iso", "demo_shapes.iso", "demo_3d.iso",
        "demo_tex.iso", "demo_logo.iso", "demo_sound.iso"
    };

    private void listDir() {
        notice = "";
        if (dir == null) {
            openDemoList();
            return;
        }
        File[] files = dir.listFiles();
        Vector<String> names = new Vector<String>();
        Vector<Integer> type = new Vector<Integer>();
        if (dir.getParentFile() != null) {
            names.addElement("\u2026 \u0432\u0432\u0435\u0440\u0445");
            type.addElement(new Integer(0));
        }
        names.addElement("\u25c8 \u0434\u0435\u043c\u043e \u0432 APK");
        type.addElement(new Integer(4));
        if (files == null) {
            notice = "\u043d\u0435\u0442 \u0434\u043e\u0441\u0442\u0443\u043f\u0430 \u043a " + dir.getPath();
        } else {
            String[] dirNames = new String[files.length];
            String[] fileNames = new String[files.length];
            int nd = 0, nf = 0;
            for (int i = 0; i < files.length; i++) {
                File f = files[i];
                String nm = f.getName();
                if (nm.startsWith(".")) continue;
                if (f.isDirectory()) {
                    dirNames[nd++] = nm;
                } else if (showAll || isImage(nm)) {
                    fileNames[nf++] = nm;
                }
            }
            sort(dirNames, nd);
            sort(fileNames, nf);
            if (nd == 0 && nf == 0) {
                names.addElement("\u2026 \u0432\u0432\u0435\u0440\u0445");
                type.addElement(new Integer(0));
            }
            for (int i = 0; i < nd; i++) {
                names.addElement(dirNames[i] + "/");
                type.addElement(new Integer(1));
            }
            for (int i = 0; i < nf; i++) {
                names.addElement(fileNames[i]);
                type.addElement(new Integer(2));
            }
        }
        labels = toArray(names);
        kinds = toInts(type);
        if (cursor >= labels.length) cursor = labels.length - 1;
        if (cursor < 0) cursor = 0;
    }

    private static void sort(String[] a, int n) {
        /* tiny insertion sort: these lists are short and this avoids pulling
           in a comparator object per entry */
        for (int i = 1; i < n; i++) {
            String v = a[i];
            int j = i - 1;
            while (j >= 0 && a[j].compareToIgnoreCase(v) > 0) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = v;
        }
    }

    private static String[] toArray(Vector<String> v) {
        String[] out = new String[v.size()];
        for (int i = 0; i < out.length; i++) out[i] = v.elementAt(i);
        return out;
    }

    private static int[] toInts(Vector<Integer> v) {
        int[] out = new int[v.size()];
        for (int i = 0; i < out.length; i++) out[i] = v.elementAt(i).intValue();
        return out;
    }

    private static boolean isImage(String nm) {
        String low = nm.toLowerCase();
        return low.endsWith(".iso") || low.endsWith(".bin")
                || low.endsWith(".img") || low.endsWith(".cue");
    }

    private void browserSelect() {
        if (labels.length == 0) return;
        int kind = kinds[cursor];
        String label = labels[cursor];
        if (kind == 0) {
            File parent = dir == null ? null : dir.getParentFile();
            if (parent == null) {
                openDemoList();
            } else {
                dir = parent;
                cursor = 0;
                listDir();
            }
            return;
        }
        if (kind == 4) {
            openDemoList();
            return;
        }
        if (kind == 5) {
            openBrowser(defaultDir());
            return;
        }
        if (kind == 1) {
            dir = new File(dir, label.substring(0, label.length() - 1));
            cursor = 0;
            listDir();
            return;
        }
        if (kind == 3) {
            if (engine.loadDemo(label)) {
                settings.lastImage = "demo:" + label;
                mode = MODE_GAME;
                paused = false;
            } else {
                notice = engine.message;
            }
            return;
        }
        File f = new File(dir, label);
        if (engine.loadDisc(f)) {
            settings.lastImage = f.getPath();
            settings.lastDir = dir.getPath();
            settings.save();
            mode = MODE_GAME;
            paused = false;
        } else {
            notice = engine.message;
        }
    }

    private void browserUp() {
        if (dir == null) {
            activity.finish();
            return;
        }
        File parent = dir.getParentFile();
        if (parent == null) {
            openDemoList();
            return;
        }
        dir = parent;
        cursor = 0;
        listDir();
    }

    /* ==================================================================== */
    /*  input                                                               */
    /* ==================================================================== */

    public boolean keyDown(int code, int repeat) {
        if (mode == MODE_BROWSER) return browserKey(code);
        if (mode == MODE_MENU) return menuKey(code);
        return gameKey(code, true);
    }

    public boolean keyUp(int code) {
        if (mode == MODE_GAME) return gameKey(code, false);
        return false;
    }

    private static int hardwareBit(int code) {
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: return Keys.UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return Keys.DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return Keys.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return Keys.RIGHT;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER: return Keys.START;
            case KeyEvent.KEYCODE_BUTTON_A: return Keys.CROSS;
            case KeyEvent.KEYCODE_BUTTON_B: return Keys.CIRCLE;
            case KeyEvent.KEYCODE_BUTTON_C: return Keys.SQUARE;
            case KeyEvent.KEYCODE_BUTTON_X: return Keys.TRIANGLE;
            case KeyEvent.KEYCODE_BUTTON_Y: return Keys.L1;
            case KeyEvent.KEYCODE_BUTTON_Z: return Keys.R1;
            case KeyEvent.KEYCODE_BUTTON_L1: return Keys.L1;
            case KeyEvent.KEYCODE_BUTTON_R1: return Keys.R1;
            case KeyEvent.KEYCODE_BUTTON_L2: return Keys.L2;
            case KeyEvent.KEYCODE_BUTTON_R2: return Keys.R2;
            case KeyEvent.KEYCODE_BUTTON_START: return Keys.START;
            case KeyEvent.KEYCODE_BUTTON_SELECT: return Keys.SELECT;
            case KeyEvent.KEYCODE_SHIFT_LEFT: return Keys.SELECT;
            case KeyEvent.KEYCODE_Z: return Keys.CROSS;
            case KeyEvent.KEYCODE_X: return Keys.CIRCLE;
            case KeyEvent.KEYCODE_A: return Keys.SQUARE;
            case KeyEvent.KEYCODE_S: return Keys.TRIANGLE;
            case KeyEvent.KEYCODE_Q: return Keys.L1;
            case KeyEvent.KEYCODE_W: return Keys.R1;
            default: return 0;
        }
    }

    private boolean gameKey(int code, boolean down) {
        if (code == KeyEvent.KEYCODE_MENU) {
            if (down) openMenu();
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK) {
            if (down) openMenu();
            return true;
        }
        int bit = hardwareBit(code);
        if (bit == 0) return false;
        if (down) {
            hardwarePad |= bit;
        } else {
            hardwarePad &= ~bit;
        }
        sendPad();
        return true;
    }

    private void sendPad() {
        engine.setPad(0xFFFF & ~(hardwarePad | touchPad));
    }

    private boolean browserKey(int code) {
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            if (cursor > 0) cursor--;
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (cursor + 1 < labels.length) cursor++;
            return true;
        }
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            browserSelect();
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK) {
            browserUp();
            return true;
        }
        if (code == KeyEvent.KEYCODE_MENU) {
            showAll = !showAll;
            notice = showAll ? "\u043f\u043e\u043a\u0430\u0437\u0430\u043d\u044b \u0432\u0441\u0435 \u0444\u0430\u0439\u043b\u044b"
                    : "\u0442\u043e\u043b\u044c\u043a\u043e \u043e\u0431\u0440\u0430\u0437\u044b";
            listDir();
            return true;
        }
        return false;
    }

    private boolean menuKey(int code) {
        if (code == KeyEvent.KEYCODE_DPAD_UP) {
            menuIndex = (menuIndex + MENU_ITEMS - 1) % MENU_ITEMS;
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
            menuIndex = (menuIndex + 1) % MENU_ITEMS;
            return true;
        }
        if (code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT) {
            int step = code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1;
            adjust(menuIndex, step);
            return true;
        }
        if (code == KeyEvent.KEYCODE_ENTER || code == KeyEvent.KEYCODE_DPAD_CENTER) {
            activate(menuIndex);
            return true;
        }
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_MENU) {
            resume();
            return true;
        }
        return true;
    }

    private void adjust(int item, int step) {
        if (item == MI_SKIP) {
            settings.frameskip += step;
            if (settings.frameskip < 0) settings.frameskip = 0;
            if (settings.frameskip > 3) settings.frameskip = 3;
        } else if (item == MI_VOLUME) {
            settings.volume += step * 10;
            if (settings.volume < 0) settings.volume = 0;
            if (settings.volume > 100) settings.volume = 100;
            engine.applyVolume();
        } else if (item == MI_SCALE) {
            settings.scaleMode = (settings.scaleMode + 3 + step) % 3;
        } else {
            activate(item);
            return;
        }
        settings.save();
    }

    private void activate(int item) {
        if (item == MI_RESUME) {
            resume();
        } else if (item == MI_DISC) {
            resume();
            openBrowser(dir != null ? dir : defaultDir());
        } else if (item == MI_SOUND) {
            settings.sound = !settings.sound;
            if (settings.sound) {
                engine.applyVolume();
                engine.startAudioIfNeeded();
            } else {
                engine.stopAudio();
            }
            settings.save();
        } else if (item == MI_SKIP) {
            adjust(MI_SKIP, 1);
        } else if (item == MI_SCALE) {
            adjust(MI_SCALE, 1);
        } else if (item == MI_TOUCH) {
            settings.touchPad = !settings.touchPad;
            settings.save();
        } else if (item == MI_VOLUME) {
            adjust(MI_VOLUME, 1);
        } else if (item == MI_ABOUT) {
            notice = "PSX Android " + EmuActivity.VERSION;
        }
    }

    private void openMenu() {
        mode = MODE_MENU;
        menuIndex = MI_RESUME;
        notice = "";
        hardwarePad = 0;
        touchPad = 0;
        sendPad();
    }

    private void resume() {
        mode = MODE_GAME;
        paused = false;
        notice = "";
        render();
    }

    /* ---- touch ---- */

    public boolean onTouchEvent(MotionEvent e) {
        touch(e);
        return true;
    }

    public boolean touch(MotionEvent e) {
        int action = e.getAction();
        if (mode == MODE_BROWSER) {
            if (action == MotionEvent.ACTION_DOWN) {
                int rowH = 18;
                int top0 = 42;
                int rows = (h - top0 - 22) / rowH;
                int i = (int) ((e.getY() - top0 + 13) / rowH);
                if (i >= 0 && i < rows && top + i < labels.length) {
                    cursor = top + i;
                    browserSelect();
                    render();
                }
                return true;
            }
            return true;
        }
        if (mode == MODE_MENU) {
            if (action == MotionEvent.ACTION_DOWN) {
                int rowH = 20;
                int y = 64;
                for (int i = 0; i < MENU_ITEMS; i++) {
                    if (e.getY() >= y - 16 && e.getY() <= y + 6) {
                        menuIndex = i;
                        activate(i);
                        render();
                        return true;
                    }
                    y += rowH;
                }
            }
            return true;
        }
        /* game: on-screen pad */
        if (!settings.touchPad) return true;
        if (action == MotionEvent.ACTION_DOWN) {
            touchPointer = e.getPointerId(0);
            touchPad = hitPad((int) e.getX(), (int) e.getY());
            sendPad();
            return true;
        }
        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            touchPointer = -1;
            touchPad = 0;
            sendPad();
            return true;
        }
        if (action == MotionEvent.ACTION_POINTER_UP
                || action == MotionEvent.ACTION_POINTER_DOWN) {
            int idx = (action & MotionEvent.ACTION_POINTER_ID_MASK)
                    >> MotionEvent.ACTION_POINTER_ID_SHIFT;
            if (e.getPointerId(idx) == touchPointer) {
                touchPointer = -1;
                touchPad = 0;
                sendPad();
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            touchPad = hitPad((int) e.getX(), (int) e.getY());
            sendPad();
            return true;
        }
        return true;
    }

    /* ==================================================================== */
    /*  misc                                                                */
    /* ==================================================================== */

    public void pause() {
        paused = true;
        hardwarePad = 0;
        touchPad = 0;
        if (engine.isRunning()) sendPad();
        engine.stopAudio();
        settings.save();
    }

    public void unpause() {
        paused = false;
        if (settings.sound) engine.startAudioIfNeeded();
    }

    public int getMode() { return mode; }

    public void loadLast() {
        if (settings.lastImage == null || settings.lastImage.length() == 0) return;
        if (settings.lastImage.startsWith("demo:")) {
            engine.loadDemo(settings.lastImage.substring(5));
            mode = MODE_GAME;
        }
    }

    private void drawCentered(Canvas c, String s, int size) {
        textPaint.setTextSize(size);
        textPaint.setColor(FG);
        int tw = (int) textPaint.measureText(s);
        c.drawText(s, (w - tw) / 2, h / 2, textPaint);
    }

    private String clip(String s, int width) {
        if (s == null) return "";
        int len = s.length();
        while (len > 0 && textPaint.measureText(s, 0, len) > width) {
            len -= 2;
        }
        if (len >= s.length()) return s;
        if (len < 3) len = 3;
        return s.substring(0, len) + "\u2026";
    }
}
