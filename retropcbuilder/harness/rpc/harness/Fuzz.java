package rpc.harness;

import rpc.Game;

/**
 * Random walk over every screen: keys 0..16 and taps anywhere, checking
 * after each step that no index ran out of its list and that neither
 * input() nor paint() threw.
 */
public final class Fuzz {

    static long seed = 20260101L;
    static int fails = 0;

    static int rnd() {
        seed = seed * 1103515245L + 12345L;
        return (int) ((seed >> 16) & 0x7FFF);
    }

    public static void main(String[] args) throws Exception {
        int steps = args.length > 0 ? Integer.parseInt(args[0]) : 4000;
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        for (int s = 0; s < steps; s++) {
            try {
                if ((rnd() & 3) != 0) {
                    g.input(rnd() % 17);
                } else {
                    g.pointer(rnd() % 240, rnd() % 320);
                }
                g.paint();
                check(g, s);
            } catch (Throwable t) {
                fails++;
                System.out.println("STEP " + s + " mode=" + g.currentMode()
                        + " THREW " + t);
                t.printStackTrace();
                if (fails > 6) {
                    break;
                }
            }
        }
        System.out.println(fails == 0
                ? "FUZZ CLEAN: " + steps + " steps" : fails + " FAILURES");
    }

    static void check(Game g, int step) {
        int m = g.currentMode();
        if (m < 0 || m > 56) {
            throw new RuntimeException("mode out of range: " + m);
        }
        if (m == 47) {
            if (g.roomCursor() < 0 || g.roomCursor() >= 8) {
                throw new RuntimeException("roomCursor " + g.roomCursor());
            }
        }
        if (m == 55) {
            if (g.funcsSel() < 0 || g.funcsSel() >= g.funcsIdxN()) {
                throw new RuntimeException("funcsSel " + g.funcsSel()
                        + " of " + g.funcsIdxN());
            }
            if (g.funcsTop() < 0 || g.funcsTop() >= g.funcsIdxN()) {
                throw new RuntimeException("funcsTop " + g.funcsTop()
                        + " of " + g.funcsIdxN());
            }
        }
        if (m == 48) {
            if (g.citySel() < 0 || g.citySel() >= g.cityOpts()) {
                throw new RuntimeException("citySel " + g.citySel()
                        + " of " + g.cityOpts());
            }
        }
        if (m == 0) {
            if (g.menuSel() < 0 || g.menuSel() >= g.menuItems()) {
                throw new RuntimeException("menuSel " + g.menuSel()
                        + " of " + g.menuItems());
            }
        }
        if (g.partListLen() > 0 && (g.partSel() < 0
                || g.partSel() >= g.partListLen())) {
            throw new RuntimeException("partSel " + g.partSel()
                    + " of " + g.partListLen());
        }
    }
}
