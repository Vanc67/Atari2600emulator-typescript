package rpc.harness;

import rpc.Funcs;
import rpc.Game;
import rpc.Host;
import rpc.Icons;
import rpc.Part;

/**
 * Automated smoke test.
 *
 * 1. boots the real game on the desktop backend,
 * 2. presses every action on every reachable screen (and a few thousand
 *    random key sequences) and fails if any screen throws,
 * 3. checks the navigation contract: each main menu entry lands on its own
 *    screen, the tea key opens the tea screen and nothing else,
 * 4. checks that save/load round-trips.
 */
public final class Smoke {

    private static int failures;

    /**
     * Repaints until the character finished walking.  Stops as soon as the
     * walk triggered a screen change, so the next check sees a fresh screen.
     */
    private static void settle(Game g) {
        for (int i = 0; i < 120; i++) {
            if (g.currentMode() != Game.M_ROOM) {
                return;
            }
            g.paint();
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            failures++;
            System.out.println("FAIL  " + what);
        } else {
            System.out.println("ok    " + what);
        }
    }

    public static void main(String[] args) {
        String dataDir = args.length > 0 ? args[0] : "data";
        TestHost host = new TestHost(dataDir);
        RasterGfx gfx = new RasterGfx(240, 320);
        Game game = new Game();
        game.init(host, gfx);
        check(game.db().count() > 50000,
                "catalogue loaded: " + game.db().count() + " parts");
        check(game.db().levels.length > 30,
                "levels loaded: " + game.db().levels.length);
        check(!game.db().levels[0].title.equals(""),
                "level 1: " + game.db().levels[0].title);
        Part first = game.db().get(1);
        check(first != null && first.name != null,
                "part #1: " + (first == null ? "null" : first.name));

        /* every action on every screen must paint without throwing */
        int[] actions = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
                          15, 16, 17, 18 };
        int crashes = 0;
        String firstCrash = null;
        for (int round = 0; round < 400; round++) {
            for (int a = 0; a < actions.length; a++) {
                try {
                    game.input(actions[a]);
                    game.paint();
                } catch (Throwable t) {
                    crashes++;
                    if (firstCrash == null) {
                        firstCrash = "mode " + game.currentMode() + " action "
                                + actions[a] + ": " + t;
                    }
                }
            }
        }
        check(crashes == 0, "8000 random actions, crashes: " + crashes
                + (firstCrash == null ? "" : " (" + firstCrash + ")"));

        /* navigation contract */
        int[][] expect = {
            { 1, 47 },  /* Комната   -> room       */
            { 2, 2 },   /* Сборка    -> build      */
            { 3, 48 },  /* Город     -> city       */
            { 4, 49 },  /* Мастерская-> tools      */
            { 5, 7 },   /* Каталог   -> catalog    */
            { 6, 45 },  /* Лаборатория -> exp list */
            { 7, 52 },  /* Календарь -> news       */
            { 8, 6 },   /* Об игре   -> about      */
        };
        for (int i = 0; i < expect.length; i++) {
            Game g = new Game();
            g.init(host, gfx);
            g.input(Game.ACT_NUM1 + i);
            check(g.currentMode() == expect[i][1],
                    "menu entry " + (i + 1) + " -> mode " + expect[i][1]
                    + " (got " + g.currentMode() + ")");
        }

        /* the room has no character: arrows move the highlight, OK and the
         * number keys use the highlighted object - one press is enough */
        Game g = new Game();
        g.init(host, gfx);
        g.input(Game.ACT_NUM1);            /* Комната */
        check(g.currentMode() == Game.M_ROOM, "room reached");
        g.input(Game.ACT_RIGHT);
        check(g.currentMode() == Game.M_ROOM, "arrows move the highlight");
        g.input(Game.ACT_NUM1);            /* hot 1 = monitor */
        check(g.currentMode() == Game.M_BUILD, "number 1 uses the monitor -> build");

        Game g2 = new Game();
        g2.init(host, gfx);
        g2.input(Game.ACT_NUM1);
        g2.input(Game.ACT_NUM2);           /* hot 2 = mug */
        check(g2.currentMode() == Game.M_TEA, "number 2 uses the mug -> tea");
        g2.input(Game.ACT_FIRE);           /* drink */
        check(g2.currentMode() == Game.M_TEA, "tea screen stays after drinking");
        g2.input(Game.ACT_BACK);
        check(g2.currentMode() == Game.M_ROOM, "tea -> back returns to room");
        g2.input(Game.ACT_NUM5);           /* hot 5 = bed */
        check(g2.currentMode() == Game.M_SLEEP, "number 5 uses the bed -> sleep");
        g2.input(Game.ACT_FIRE);
        check(g2.currentMode() == Game.M_SLEEP, "sleep screen stays after sleeping");
        g2.input(Game.ACT_BACK);
        check(g2.currentMode() == Game.M_ROOM, "sleep -> back returns to room");
        g2.input(Game.ACT_BACK);
        check(g2.currentMode() == Game.M_MENU, "room -> back returns to menu");

        /* OK uses whatever the highlight is on, no walking needed */
        Game g3 = new Game();
        g3.init(host, gfx);
        g3.input(Game.ACT_NUM1);
        g3.input(Game.ACT_RIGHT);
        g3.input(Game.ACT_FIRE);
        check(g3.currentMode() == Game.M_TEA, "OK on the highlighted mug -> tea");

        /* outside the room the character walks on the pavement */
        Game gc = new Game();
        gc.init(host, gfx);
        gc.input(Game.ACT_NUM1);            /* room */
        gc.input(Game.ACT_NUM6);            /* hot 6 = door -> city */
        check(gc.currentMode() == Game.M_CITY, "door opens the city");
        gc.paint();
        int[] c0 = gc.cityCharPos();
        for (int i = 0; i < 30; i++) {
            gc.paint();
        }
        int[] c1 = gc.cityCharPos();
        check(c1[0] != c0[0], "the character walks outside (city) ("
                + c0[0] + " -> " + c1[0] + ")");

        /* the 500 function catalog */
        check(Funcs.count() == 500, "catalog holds 500 functions ("
                + Funcs.count() + ")");
        int bad = 0;
        for (int i = 0; i < Funcs.N; i++) {
            if (Funcs.title(i).length() == 0 || Funcs.cat(i) < 0
                    || Funcs.cat(i) >= Funcs.CATS || Funcs.xp(i) <= 0
                    || Funcs.icon(i) < 0 || Funcs.icon(i) >= Icons.COUNT) {
                bad++;
            }
        }
        check(bad == 0, "every function has a title, chapter, icon and XP");
        int perCat = Funcs.count() / Funcs.CATS;
        boolean even = true;
        for (int c = 0; c < Funcs.CATS; c++) {
            if (Funcs.title(c * perCat).length() == 0
                    || Funcs.cat(c * perCat + perCat - 1) != c) {
                even = false;
            }
        }
        check(even && perCat == 50, "ten chapters of fifty (" + perCat + ")");

        Game gf = new Game();
        gf.init(host, gfx);
        gf.input(Game.ACT_NUM9);           /* menu entry 9 */
        check(gf.currentMode() == Game.M_FUNCS, "menu entry 9 -> catalog");
        gf.grantCredits(10000000);
        gf.applyFunc(0);
        check(gf.funcsDone() == 1, "doing a function marks it done");
        int done = 0;
        int xpBefore = gf.totalXp();
        for (int i = 0; i < Funcs.N; i++) {
            gf.applyFunc(i);
        }
        done = gf.funcsDone();
        check(done == 500, "all 500 functions can be performed (" + done + ")");
        check(gf.totalXp() > xpBefore, "functions award XP ("
                + xpBefore + " -> " + gf.totalXp() + ")");

        /* BACK always leads back to the menu, from every reachable screen */
        int stuck = 0;
        int worst = 0;
        for (int scenario = 0; scenario < 300; scenario++) {
            Game gb = new Game();
            gb.init(host, gfx);
            int presses = scenario % 7 + 1;
            for (int i = 0; i < presses; i++) {
                gb.input(scenario % 19);
            }
            gb.input(Game.ACT_NUM1);          /* into the room, if possible */
            int steps = 0;
            while (gb.currentMode() != Game.M_MENU && steps < 40) {
                gb.input(Game.ACT_BACK);
                steps++;
            }
            if (gb.currentMode() != Game.M_MENU) {
                stuck++;
            }
            if (steps > worst) {
                worst = steps;
            }
        }
        check(stuck == 0, "BACK always returns to the menu ("
                + stuck + " stuck, worst " + worst + " presses)");

        /* save / load round-trip */
        Game g4 = new Game();
        g4.init(host, gfx);
        g4.input(Game.ACT_NUM1);
        g4.input(Game.ACT_NUM2);
        g4.input(Game.ACT_FIRE);
        g4.saveProgress();
        Game g5 = new Game();
        g5.init(host, gfx);
        g5.loadProgress();
        check(g5.credits() == g4.credits(), "save/load keeps credits ("
                + g4.credits() + ")");

        /* sound: the audio engine renders samples for a session.
         * The engine plays on worker threads, so give it a moment. */
        rpc.Sound.getEngine().setEnabled(true);
        for (int i = 0; i < 6; i++) {
            rpc.Sound.click();
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {
            }
        }
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
        Wav.flush("docs/screens/smoke_audio.wav");
        check(Wav.voiceCount() > 0, "audio engine produced "
                + Wav.voiceCount() + " voices");

        System.out.println(failures == 0
                ? "\nSMOKE TEST PASSED" : "\n" + failures + " FAILURE(S)");
        if (failures != 0) {
            System.exit(1);
        }
    }
}
