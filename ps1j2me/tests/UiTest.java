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

    static boolean boolField(Object o, String name) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getBoolean(o);
    }

    static void setBool(Object o, String name, boolean v) throws Exception {
        Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(o, v);
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

        /* The memory card scan finishes on a worker thread and must report
           that it is done, otherwise a phone can be left on a blank screen. */
        long deadline = System.currentTimeMillis() + 10000;
        while (!canvas.isDiscovered() && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(5); } catch (InterruptedException ie) { }
        }
        check(canvas.isDiscovered(), "background memory card scan finishes");

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
        String[] items = (String[]) field(canvas, "SETTINGS_ITEMS");
        boolean settingsOk = true;
        for (int row = 0; row < items.length; row++) {
            setInt(canvas, "settingsSelection", row);
            try {
                applySetting.invoke(canvas, new Object[0]);
            } catch (Throwable t) {
                settingsOk = false;
                System.out.println("  settings row " + row + " failed: " + t.getCause());
            }
        }
        check(settingsOk, "settings rows apply (" + items.length + " rows)");

        /* the key layout row must cycle through all three layouts */
        int layoutRow = -1;
        for (int i = 0; i < items.length; i++) {
            if (items[i].indexOf("Раскладка") >= 0) layoutRow = i;
        }
        check(layoutRow >= 0, "settings has a key layout row");
        if (layoutRow >= 0) {
            cfg.keyLayout = 0;
            boolean cycleOk = true;
            setInt(canvas, "settingsSelection", layoutRow);
            for (int i = 0; i < 3; i++) {
                try {
                    applySetting.invoke(canvas, new Object[0]);
                } catch (Throwable t) {
                    cycleOk = false;
                }
                if (cfg.keyLayout != (i + 1) % 3) cycleOk = false;
            }
            check(cycleOk, "key layout cycles 0-1-2-0");
            check(cfg.keyLayoutName() != null && cfg.keyLayoutName().length() > 3,
                    "key layout name " + cfg.keyLayoutName());
        }

        /* the whole config (including the last folder, commas and all) must
           survive a RecordStore round trip */
        cfg.keyLayout = 2;
        cfg.showAllFiles = true;
        cfg.lastUrl = "file:///E:/games/ps1,cd/";
        cfg.save();
        Config again = (Config) fresh.invoke(null, new Object[0]);
        check(again.keyLayout == 2, "config keeps the key layout");
        check(again.showAllFiles, "config keeps showAllFiles");
        check("file:///E:/games/ps1,cd/".equals(again.lastUrl),
                "config keeps a folder name with a comma (" + again.lastUrl + ")");
        cfg.reset();

        /* a real folder listing through the JSR-75 shim */
        try {
            String base = System.getProperty("java.io.tmpdir") + "/uiBrowserTest";
            java.io.File dir = new java.io.File(base);
            dir.mkdirs();
            new java.io.File(dir, "sub").mkdirs();
            new java.io.File(dir, "Final Fantasy VII.bin").createNewFile();
            new java.io.File(dir, "metal gear.cue").createNewFile();
            new java.io.File(dir, "notes.txt").createNewFile();
            new java.io.File(dir, "empty.iso").createNewFile();
            String url = dir.getAbsolutePath() + "/";

            cfg.showAllFiles = false;
            Method listDirectory = MainCanvas.class.getDeclaredMethod("listDirectory",
                    new Class[] { String.class, String.class });
            listDirectory.setAccessible(true);
            listDirectory.invoke(canvas, new Object[] { url, "Test" });
            java.util.Vector entries = (java.util.Vector) field(canvas, "entries");
            int dirs = 0, games = 0, others = 0;
            String order = "";
            for (int i = 0; i < entries.size(); i++) {
                Object e = entries.elementAt(i);
                boolean isDir = ((Boolean) field(e, "isDir")).booleanValue();
                boolean game = ((Boolean) field(e, "game")).booleanValue();
                boolean isRoot = ((Boolean) field(e, "root")).booleanValue();
                String name = (String) field(e, "name");
                long size = ((Long) field(e, "size")).longValue();
                if (isRoot) others++;
                else if (isDir) dirs++;
                else if (game) games++;
                else others++;
                order += (isDir ? "D" : (game ? "G" : "F"));
                check(!name.endsWith("/"), "entry name has no trailing slash: " + name);
                check(isDir || size >= 0, "file size known for " + name);
            }
            check(dirs == 1, "one folder listed (" + dirs + ")");
            check(games == 3, "three game images listed (" + games + ")");
            check(others == 0, "non-game files hidden by default (" + others + ")");
            check(entries.size() == 4, "hidden files not listed (" + entries.size() + ")");
            check(order.startsWith("D"), "folders are listed first (" + order + ")");
            check("игра".equals(MainCanvas.gamesWord(1))
                    && "игры".equals(MainCanvas.gamesWord(3))
                    && "игр".equals(MainCanvas.gamesWord(5))
                    && "игр".equals(MainCanvas.gamesWord(11))
                    && "игра".equals(MainCanvas.gamesWord(21))
                    && "игры".equals(MainCanvas.gamesWord(22))
                    && "игр".equals(MainCanvas.gamesWord(0)), "russian plural for games");

            cfg.showAllFiles = true;
            listDirectory.invoke(canvas, new Object[] { url, "Test" });
            entries = (java.util.Vector) field(canvas, "entries");
            check(entries.size() == 5, "showAllFiles lists everything (" + entries.size() + ")");

            /* a missing folder must not throw, it shows a message instead */
            cfg.showAllFiles = false;
            listDirectory.invoke(canvas, new Object[] { url + "nope/", "Nope" });
            check(intField(canvas, "mode") == 7, "missing folder shows a message");
            String status = (String) field(canvas, "status");
            check(status != null && status.length() > 0, "missing folder status: " + status);
            cfg.reset();
            canvas.shutdown();
        } catch (Throwable t) {
            check(false, "folder listing -> " + (t.getCause() != null ? t.getCause() : t));
        }

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

        /* ---- regressions found in 1.7.0 ------------------------------- */

        Method keyPressed = MainCanvas.class.getDeclaredMethod("keyPressed",
                new Class[] { int.class });
        keyPressed.setAccessible(true);
        Method keyReleased = MainCanvas.class.getDeclaredMethod("keyReleased",
                new Class[] { int.class });
        keyReleased.setAccessible(true);
        Method listDirectory = MainCanvas.class.getDeclaredMethod("listDirectory",
                new Class[] { String.class, String.class });
        listDirectory.setAccessible(true);

        try {
            String base = System.getProperty("java.io.tmpdir") + "/uiKeyLayout";
            java.io.File dir = new java.io.File(base);
            dir.mkdirs();
            new java.io.File(dir, "game.iso").createNewFile();
            new java.io.File(dir, "readme.txt").createNewFile();
            new java.io.File(dir, "sub").mkdirs();
            String url = dir.getAbsolutePath() + "/";

            /* 1. menus must not follow the "cross/circle" pad layout:
                  with CIRCLE swapped to CROSS, "0" used to launch the game */
            cfg.keyLayout = psx.KeyMap.LAYOUT_SWAPPED;
            cfg.showAllFiles = false;
            setInt(canvas, "mode", 1);
            listDirectory.invoke(canvas, new Object[] { url, "games" });
            setInt(canvas, "selected", 0);
            setBool(canvas, "booting", false);
            keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM0) });
            check(!boolField(canvas, "booting"), "0 does not launch a game in the cross/circle layout");
            check(intField(canvas, "mode") == 3, "0 goes back (pause menu) in the cross/circle layout");

            /* "5" still confirms in every layout (a folder is used on purpose:
               launching a game here would start a boot that races with the
               demo launch further down) */
            boolean fireOk = true;
            int[] layouts = { 0, 1, 2 };
            for (int i = 0; i < layouts.length; i++) {
                cfg.keyLayout = layouts[i];
                listDirectory.invoke(canvas, new Object[] { url, "games" });
                int sub = -1;
                java.util.Vector list = (java.util.Vector) field(canvas, "entries");
                for (int k = 0; k < list.size(); k++) {
                    if ("sub".equals((String) field(list.elementAt(k), "name"))) sub = k;
                }
                if (sub < 0) { fireOk = false; continue; }
                setInt(canvas, "mode", 1);
                setInt(canvas, "selected", sub);
                keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM5) });
                String opened = (String) field(canvas, "currentUrl");
                if (opened == null || opened.indexOf("/sub/") < 0) fireOk = false;
            }
            check(fireOk, "5 confirms in every pad layout");
            check(!boolField(canvas, "booting"), "no background boot after the key checks");

            /* 2. a tap on a non-game file must answer AND the answer must be
                  painted (the message used to be drawn on the game screen only) */
            cfg.keyLayout = 0;
            cfg.showAllFiles = true;
            listDirectory.invoke(canvas, new Object[] { url, "games" });
            int nonGame = -1;
            java.util.Vector entries2 = (java.util.Vector) field(canvas, "entries");
            for (int i = 0; i < entries2.size(); i++) {
                Object e = entries2.elementAt(i);
                if (!((Boolean) field(e, "isDir")).booleanValue()
                        && !((Boolean) field(e, "game")).booleanValue()) nonGame = i;
            }
            check(nonGame >= 0, "all-files mode lists the text file");
            if (nonGame >= 0) {
                setInt(canvas, "selected", nonGame);
                setBool(canvas, "booting", false);
                keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM5) });
                String msg = (String) field(canvas, "overlayMessage");
                check(msg != null && msg.length() > 0, "tapping a text file shows a hint");
                check(intField(canvas, "mode") == 1, "tapping a text file stays in the browser");
                /* the strip must reach the screen of the browser */
                int[] pix = new int[176 * 208];
                setInt(canvas, "viewW", 176);
                setInt(canvas, "viewH", 208);
                setInt(canvas, "mode", 1);
                paint.invoke(canvas, new Object[] { new UiRender.Pix(pix) });
                /* the strip is black, the glyphs are COLOR_TEXT (0xE2E8F0) */
                int strip = 0;
                for (int y = 208 - 14; y < 208; y++) {
                    for (int x = 0; x < 176; x++) {
                        int rgb = pix[y * 176 + x] & 0xFFFFFF;
                        if (rgb != 0x000000 && rgb != 0x0B1020) strip++;
                    }
                }
                check(strip > 20, "overlay text is painted in the browser (" + strip + " px)");
            }
            cfg.showAllFiles = false;

            /* 3. toggling "show all files" must not leave the settings screen */
            setInt(canvas, "mode", 4);
            int allFilesRow = -1;
            for (int i = 0; i < items.length; i++) {
                if (items[i].indexOf("Показывать") >= 0) allFilesRow = i;
            }
            check(allFilesRow >= 0, "settings has an all-files row");
            if (allFilesRow >= 0) {
                setInt(canvas, "settingsSelection", allFilesRow);
                applySetting.invoke(canvas, new Object[0]);
                check(intField(canvas, "mode") == 4, "all-files toggle stays on the settings screen");
                setInt(canvas, "settingsSelection", allFilesRow);
                applySetting.invoke(canvas, new Object[0]);
            }

            /* 4. with a single storage root "up" from the card root is the menu,
                  not a dead device list */
            set(canvas, "roots", new java.util.Vector());
            listDirectory.invoke(canvas, new Object[] { url, "games" });
            setInt(canvas, "mode", 1);
            setBool(canvas, "showingRoots", false);
            keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM0) });
            check(!boolField(canvas, "showingRoots"),
                    "up from a lone card root does not open the device list");

            /* 5. pressing * while a game is booting must not start a second scan */
            setInt(canvas, "mode", 7);
            setBool(canvas, "booting", true);
            keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_STAR) });
            check(intField(canvas, "mode") == 7, "no second scan while a game is booting");
            String bootMsg = (String) field(canvas, "overlayMessage");
            check(bootMsg != null && bootMsg.length() > 0, "booting hint is shown: " + bootMsg);
            setBool(canvas, "booting", false);

            /* 6. a key released while the pause menu is open must reach the pad,
                  otherwise the direction stays pressed after "resume" */
            Object mach = field(canvas, "machine");
            if (mach != null) {
                setInt(canvas, "mode", 3);
                keyPressed.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM6) });
                setInt(canvas, "mode", 2);
                keyReleased.invoke(canvas, new Object[] { new Integer(javax.microedition.lcdui.Canvas.KEY_NUM6) });
                int pads = intField(canvas, "padButtons");
                check((pads & (1 << 5)) != 0, "released key is not stuck on the pad");
            } else {
                check(true, "pad release check skipped (no machine)");
            }
            cfg.reset();
        } catch (Throwable t) {
            check(false, "regressions 1.7.0 -> " + (t.getCause() != null ? t.getCause() : t));
        }

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

            /* a phone call pauses the MIDlet: the emulation must stop, the pad
               must be released and the pause menu must be shown */
            try {
                Method onPause = MainCanvas.class.getDeclaredMethod("onAppPause", new Class[0]);
                onPause.setAccessible(true);
                Method onResume = MainCanvas.class.getDeclaredMethod("onAppResume", new Class[0]);
                onResume.setAccessible(true);
                int before = intField(canvas, "mode");
                check(before == 2, "game screen before the phone call (" + before + ")");
                onPause.invoke(canvas, new Object[0]);
                check(intField(canvas, "mode") == 3, "phone call shows the pause menu");
                check(boolField(canvas, "paused"), "phone call pauses the emulation");
                check(intField(canvas, "padButtons") == 0xFFFF, "phone call releases the pad");
                onResume.invoke(canvas, new Object[0]);
                check(intField(canvas, "mode") == 3, "returning keeps the pause menu");
                check(boolField(canvas, "paused"), "returning does not silently resume");
                /* "Продолжить игру" gets back into the game */
                setInt(canvas, "pauseSelection", 0);
                Method pauseKey = MainCanvas.class.getDeclaredMethod("pauseKey",
                        new Class[] { int.class, int.class });
                pauseKey.setAccessible(true);
                pauseKey.invoke(canvas, new Object[] { new Integer(53), new Integer(0) });
                check(intField(canvas, "mode") == 2, "resume from the pause menu works");
                check(!boolField(canvas, "paused"), "resume clears the pause flag");
            } catch (Throwable t) {
                check(false, "app pause/resume -> " + (t.getCause() != null ? t.getCause() : t));
            }

            /* the demo must actually produce visible pixels (catches a black screen) */
            Object mach = field(canvas, "machine");
            check(mach != null, "demo machine available for the frame check");
            if (mach == null) {
                System.out.println("  boot failed: " + field(canvas, "status"));
                throw new IllegalStateException("no machine");
            }
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
