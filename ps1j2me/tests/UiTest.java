import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.microedition.lcdui.Graphics;

import psx.Config;
import psx.MainCanvas;

/**
 * Headless smoke test for the menu system. It drives every screen through the
 * key handlers and checks that painting never throws, that settings persist
 * their defaults and that the demo list is wired to the packaged images.
 */
public class UiTest {

    static int failures;

    static void check(boolean ok, String what) {
        System.out.println((ok ? "PASS " : "FAIL ") + what);
        if (!ok) failures++;
    }

    static Object field(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static void set(Object o, String name, Object v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, v);
    }

    static void setInt(Object o, String name, int v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(o, v);
    }

    static int intField(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(o);
    }

    public static void main(String[] args) throws Exception {
        /* Config must be usable without RMS and keep its defaults */
        Config cfg = Config.get();
        check(cfg.volume >= 0 && cfg.volume <= 100, "config default volume " + cfg.volume);
        check(cfg.scaleName() != null, "config scale name " + cfg.scaleName());
        cfg.frameSkip = 2;
        cfg.save();
        java.lang.reflect.Method fresh = Config.class.getDeclaredMethod("loadFresh", new Class[0]);
        fresh.setAccessible(true);
        Config loaded = (Config) fresh.invoke(null, new Object[0]);
        check(loaded.frameSkip == 2, "config survives save/load round trip");
        cfg.reset();
        check(Config.get().frameSkip == 0, "config reset");

        /* give the stub canvas a real screen size so paint() lays out properly */
        javax.microedition.lcdui.Displayable.STUB_WIDTH = 176;
        javax.microedition.lcdui.Displayable.STUB_HEIGHT = 208;

        /* Cyrillic literals must survive compilation (build.sh passes -encoding UTF-8) */
        String demo0 = ((String[]) field(new MainCanvas(null), "DEMO_NAME"))[0];
        boolean cyrillic = demo0.indexOf('\u0438') >= 0 && demo0.indexOf('\ufffd') < 0;
        check(cyrillic, "demo names keep Cyrillic (" + demo0.length() + " chars)");

        /* the bitmap font must know every character used by the UI */
        String help = ((String[]) field(new MainCanvas(null), "HELP_TEXT"))[0];
        check(psx.TinyFont.stringWidth(help) == help.length() * psx.TinyFont.WIDTH,
                "bitmap font width for help text");
        check(psx.TinyFont.stringWidth("Привет PS1") == 10 * psx.TinyFont.WIDTH,
                "bitmap font covers Cyrillic");

        MainCanvas canvas = new MainCanvas(null);
        Graphics g = new Graphics();

        String[] modes = { "MODE_BOOT", "MODE_BROWSER", "MODE_PAUSE", "MODE_SETTINGS",
                "MODE_HELP", "MODE_DEMOS", "MODE_MESSAGE", "MODE_INFO" };
        int[] expected = { 0, 1, 3, 4, 5, 6, 7, 8 };
        Method paint = MainCanvas.class.getDeclaredMethod("paint", new Class[] { Graphics.class });
        paint.setAccessible(true);
        for (int i = 0; i < modes.length; i++) {
            setInt(canvas, "mode", expected[i]);
            setInt(canvas, "viewW", 176);
            setInt(canvas, "viewH", 208);
            try {
                paint.invoke(canvas, new Object[] { g });
                check(true, "paint " + modes[i]);
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                check(false, "paint " + modes[i] + " -> " + cause);
            }
        }

        /* settings: every row must be cycle-able without an exception */
        setInt(canvas, "mode", 4);
        Method settingsKey = MainCanvas.class.getDeclaredMethod("settingsKey",
                new Class[] { int.class, int.class });
        settingsKey.setAccessible(true);
        Method applySetting = MainCanvas.class.getDeclaredMethod("applySetting", new Class[0]);
        applySetting.setAccessible(true);
        boolean settingsOk = true;
        for (int row = 0; row < 8; row++) {
            setInt(canvas, "settingsSelection", row);
            try {
                applySetting.invoke(canvas, new Object[0]);
            } catch (Throwable t) {
                settingsOk = false;
                System.out.println("  settings row " + row + " failed: " + t.getCause());
            }
        }
        check(settingsOk, "settings rows apply");

        /* help screen scrolls through the whole text */
        setInt(canvas, "mode", 5);
        int lines = ((String[]) field(canvas, "HELP_TEXT")).length;
        check(lines > 20, "help text lines " + lines);
        try {
            for (int i = 0; i < lines + 5; i++) settingsKey.invoke(canvas, new Object[] { new Integer(56), new Integer(0) });
            check(true, "help scroll");
        } catch (Throwable t) {
            check(false, "help scroll -> " + t.getCause());
        }

        /* demo table must point at the packaged resources */
        String[] res = (String[]) field(canvas, "DEMO_RES");
        String[] names = (String[]) field(canvas, "DEMO_NAME");
        String[] desc = (String[]) field(canvas, "DEMO_DESC");
        check(res.length == 7 && names.length == res.length && desc.length == res.length,
                "demo tables " + res.length);
        boolean resourcesOk = true;
        for (int i = 0; i < res.length; i++) {
            java.io.InputStream in = UiTest.class.getResourceAsStream(res[i]);
            if (in == null) {
                resourcesOk = false;
                System.out.println("  missing resource " + res[i]);
            } else {
                in.close();
            }
        }
        check(resourcesOk, "demo resources on the classpath");

        /* the demo launcher must boot the embedded image and paint a frame */
        Method startDemo = MainCanvas.class.getDeclaredMethod("startDemo", new Class[] { int.class });
        startDemo.setAccessible(true);
        try {
            long t0 = System.currentTimeMillis();
            startDemo.invoke(canvas, new Object[] { new Integer(0) });
            long callTime = System.currentTimeMillis() - t0;
            /* the event thread must return immediately: boot happens on a worker */
            check(callTime < 1000, "demo launch does not block the UI (" + callTime + " ms)");
            Object machine = null;
            for (int wait = 0; wait < 200 && machine == null; wait++) {
                Thread.sleep(50);
                machine = field(canvas, "machine");
            }
            check(machine != null, "demo machine booted");
            int mode = intField(canvas, "mode");
            check(mode == 2, "demo switched to game mode (" + mode + ")");
            Method paintGame = MainCanvas.class.getDeclaredMethod("paintGame",
                    new Class[] { Graphics.class });
            paintGame.setAccessible(true);
            paintGame.invoke(canvas, new Object[] { g });
            check(true, "paint game screen");

            /* the demo must actually produce visible pixels (catches a black screen) */
            Object mach = field(canvas, "machine");
            for (int wait = 0; wait < 60; wait++) {
                int fr = mach.getClass().getField("framesRun").getInt(mach);
                if (fr >= 8) break;
                Thread.sleep(50);
            }
            int[] frame = (int[]) mach.getClass().getMethod("getFrame", new Class[0])
                    .invoke(mach, new Object[0]);
            int nonBlack = 0;
            for (int i = 0; i < frame.length; i++) {
                if ((frame[i] & 0xFFFFFF) != 0) nonBlack++;
            }
            check(nonBlack > 1000, "demo frame is not black (" + nonBlack + " px)");
            int pixels = ((Integer) mach.getClass().getMethod("getPixelsWritten", new Class[0])
                    .invoke(mach, new Object[0])).intValue();
            check(pixels > 1000, "GPU wrote pixels (" + pixels + ")");
            setInt(canvas, "viewW", 176);
            setInt(canvas, "viewH", 208);
            cfg.scaleMode = Config.SCALE_4_3;
            Method destRect = MainCanvas.class.getDeclaredMethod("destRect", new Class[0]);
            destRect.setAccessible(true);
            int[] rect = (int[]) destRect.invoke(canvas, new Object[0]);
            check(rect.length == 4 && rect[2] > 0 && rect[3] > 0,
                    "scale rect " + rect[2] + "x" + rect[3]);
        } catch (Throwable t) {
            check(false, "demo launch -> " + (t.getCause() != null ? t.getCause() : t));
        }

        /* stop the emulation thread started by the demo launch, then leave
           hard: the demo game loop is a non-daemon thread */
        canvas.shutdown();
        System.out.println(failures == 0 ? "ALL UI TESTS PASSED" : failures + " UI FAILURES");
        System.out.flush();
        Runtime.getRuntime().halt(failures == 0 ? 0 : 1);
    }
}
