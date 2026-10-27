package rpc.harness;

import rpc.Game;

/**
 * Taps the real screens and checks that the thing under the finger is the
 * thing that reacts: the experiment list rows and the overclocking tabs.
 */
public final class Ptr {

    static TestHost host = new TestHost("data");
    static RasterGfx gfx = new RasterGfx(240, 320);

    static Game go(String path) {
        Game g = new Game();
        g.init(host, gfx);
        int i = 0;
        while (i < path.length()) {
            int a = (path.charAt(i) - '0') * 10 + (path.charAt(i + 1) - '0');
            g.input(a);
            i += 2;
            if (i < path.length() && path.charAt(i) == ',') {
                i++;
            }
        }
        g.paint();
        return g;
    }

    public static void main(String[] args) throws Exception {
        // --- the lab: tap row r, the highlight must land on row r ---
        Game g = go("13");                       // menu item 6 -> exp list
        if (g.currentMode() != 45) {
            System.out.println("mode " + g.currentMode() + ", not 45");
            return;
        }
        int fh0 = g.FH0();
        int fh1 = g.FH1();
        int top = fh1 + 12;
        int barH = fh0 + 8;
        int rowH = fh0 * 2 + 9;
        int listTop = top + barH + 3;
        int footerTop = 320 - fh0 - 12;
        int rows = (footerTop - listTop) / rowH;
        System.out.println("lab: expSel=" + g.expSel() + " count="
                + g.expCount() + " rows=" + rows);
        int bad = 0;
        for (int r = 0; r < rows; r++) {
            Game t = go("13");
            int y = listTop + r * rowH + rowH / 2;
            int before = t.expSel();
            t.pointer(120, y);
            t.paint();
            if (t.expSel() != r) {
                bad++;
                System.out.println("  row " + r + " tapped at y=" + y
                        + " -> expSel=" + t.expSel() + " (was " + before + ")");
            }
        }
        System.out.println(bad == 0 ? "LAB TAP OK" : "LAB TAP BAD: " + bad);

        // --- the overclocking bench: tap tab t, expTab must become t ---
        String oc = args.length > 0 ? args[0] : null;
        if (oc == null) {
            return;
        }
        Game o = go(oc);
        System.out.println("oc: mode=" + o.currentMode() + " expTab="
                + o.expTab);
        int i1 = Math.max(76, Math.min(130, o.H() / 3));
        int i2 = o.FH1() + 8 + o.FH1() + 2 + o.FH0() + 3 + i1 + 4 + o.FH0() * 2 + 5 + 4;
        int i7 = (o.W() - 8) / 4;
        int bad2 = 0;
        for (int t = 0; t < 4; t++) {
            Game q = go(oc);
            int x = 4 + t * i7 + i7 / 2;
            int y = i2 + 6;
            q.pointer(x, y);
            q.paint();
            if (q.expTab != t) {
                bad2++;
                System.out.println("  tab " + t + " tapped at (" + x + ","
                        + y + ") -> expTab=" + q.expTab);
            }
        }
        System.out.println(bad2 == 0 ? "OC TAB OK" : "OC TAB BAD: " + bad2);
    }
}
