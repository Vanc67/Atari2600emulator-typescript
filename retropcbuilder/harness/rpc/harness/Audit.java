package rpc.harness;

import rpc.Game;
import rpc.LifeSim;

/**
 * Sweeps the whole game: finds every screen it can reach, then for each
 * screen presses every key and reports the ones that change nothing.
 */
public final class Audit {

    static TestHost host = new TestHost("data");
    static RasterGfx gfx = new RasterGfx(240, 320);

    static String snap(Game g) {
        LifeSim l = g.life();
        return g.currentMode() + "|" + g.menuSel() + "|" + g.roomCursor()
                + "|" + g.funcsSel() + "|" + g.funcsTop() + "|"
                + g.funcsIdxN() + "|" + g.partSel() + "|" + g.partListLen()
                + "|" + g.citySel() + "|" + g.cityOpts() + "|"
                + l.teaLevel + "|" + l.energy + "|" + l.mood + "|"
                + l.curHour + "|" + l.curDay + "|" + l.curMonth + "|"
                + l.curYear + "|" + g.credits() + "|"
                + g.cityLoc() + "|" + g.teaSel() + "|" + g.funcsTab() + "|"
                + g.catSel() + "|" + g.shopSel() + "|" + g.taskSel() + "|"
                + g.progSel() + "|" + g.editorSel() + "|" + g.setupSel()
                + "|" + g.offerSel() + "|" + g.langSel() + "|"
                + g.expSel() + "|" + g.expTop() + "|" + g.expFilterPlat() + "|"
                + g.expFilterKind() + "|" + g.museumSel() + "|"
                + g.workshopTool() + "|" + g.expVoltage() + "|"
                + g.expFreq() + "|" + g.expCooling() + "|" + g.expStatus()
                + "|" + (g.toastText() == null ? "" : g.toastText());
    }

    /**
     * Random walks with restarts: every screen the game can show is
     * recorded together with the keys that opened it.
     */
    static String[] explore(int[] modes, int[] count) {
        String[] table = new String[200];
        int[] seen = new int[200];
        int found = 0;
        long seed = 987654321L;
        for (int restart = 0; restart < 40 && found < 140; restart++) {
            Game g = new Game();
            g.init(host, gfx);
            StringBuffer path = new StringBuffer();
            int m0 = g.currentMode();
            if (seen[m0] == 0) {
                seen[m0] = 1;
                table[m0] = "";
                found++;
            }
            for (int step = 0; step < 70; step++) {
                seed = seed * 1103515245L + 12345L;
                int r = (int) ((seed >> 16) & 0x7FFF);
                int a;
                if (r % 10 < 4) {
                    a = 4 + (r / 10) % 13;      // fire + number keys
                } else if (r % 10 < 6) {
                    a = 7;                      // back
                } else {
                    a = r % 8;                  // d-pad / soft
                }
                if (path.length() == 0) {
                    path.append((char) ('0' + a / 10))
                            .append((char) ('0' + a % 10));
                } else {
                    path.append(',').append((char) ('0' + a / 10))
                            .append((char) ('0' + a % 10));
                }
                g.input(a);
                g.paint();
                int m = g.currentMode();
                if (m >= 0 && m < 200 && seen[m] == 0) {
                    seen[m] = 1;
                    table[m] = path.toString();
                    found++;
                }
            }
        }
        int n = 0;
        for (int i = 0; i < 200; i++) {
            if (seen[i] != 0) {
                modes[n++] = i;
            }
        }
        modes[0] = n;
        return table;
    }

    static int last;

    static Game replay(String path) {
        last = -1;
        Game g = new Game();
        g.init(host, gfx);
        if (path != null && path.length() > 0) {
            for (int i = 0; i < path.length(); i++) {
                if (path.charAt(i) == ',') {
                    g.input(Integer.parseInt(path.substring(
                            last + 1, i)));
                    g.paint();
                    last = i;
                }
            }
            g.input(Integer.parseInt(path.substring(last + 1)));
            g.paint();
        }
        return g;
    }

    public static void main(String[] args) throws Exception {
        int[] modes = new int[200];
        String[] table = explore(modes, new int[1]);
        int n = modes[0];
        for (int i = 2; i <= n; i++) {
            int key = modes[i];
            int j = i - 1;
            while (j >= 1 && modes[j] > key) {
                modes[j + 1] = modes[j];
                j--;
            }
            modes[j + 1] = key;
        }
        System.out.println("reachable screens: " + n);
        int deadScreens = 0;
        for (int i = 1; i <= n; i++) {
            int m = modes[i];
            String path = table[m];
            Game g = replay(path);
            if (g.currentMode() != m) {
                System.out.println("mode " + m + " not reproducible (now "
                        + g.currentMode() + "), skipped");
                continue;
            }
            // every key gets its own fresh replay, so one key cannot
            // change the state another key is judged on
            StringBuffer deadFor = new StringBuffer();
            for (int a = 0; a < 17; a++) {
                Game t = replay(path);
                String before = snap(t);
                t.input(a);
                t.paint();
                if (snap(t).equals(before)) {
                    deadFor.append(a).append(' ');
                }
            }
            System.out.println("mode " + m + "  dead keys: ["
                    + deadFor.toString().trim() + "]");
            if (deadFor.length() > 0) {
                deadScreens++;
            }
        }
        System.out.println("--- screens with at least one dead key: "
                + deadScreens + " ---");
    }
}
