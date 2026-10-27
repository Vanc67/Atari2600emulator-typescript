package rpc.harness;

import rpc.Game;

/**
 * Finds every screen the game can show: long biased random walks with
 * restarts, no painting (input dispatch does not depend on the framebuffer).
 * Prints one line per reachable mode: mode, how many times it was seen and
 * the shortest action path that reached it.
 */
public final class Explore {

    static TestHost host = new TestHost("data");
    static RasterGfx gfx = new RasterGfx(240, 320);

    static String enc(int a) {
        return (char) ('0' + a / 10) + "" + (char) ('0' + a % 10);
    }

    static Game fresh() {
        Game g = new Game();
        g.init(host, gfx);
        return g;
    }

    static void apply(Game g, String path) {
        int i = 0;
        while (i < path.length()) {
            int a = (path.charAt(i) - '0') * 10 + (path.charAt(i + 1) - '0');
            g.input(a);
            i += 2;
            if (i < path.length() && path.charAt(i) == ',') {
                i++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        int restarts = args.length > 0 ? Integer.parseInt(args[0]) : 400;
        int steps = args.length > 1 ? Integer.parseInt(args[1]) : 200;
        int[] bestLen = new int[200];
        String[] bestPath = new String[200];
        int[] hits = new int[200];
        for (int i = 0; i < 200; i++) {
            bestLen[i] = 1 << 30;
        }
        long seed = 1234567L;
        Game g = fresh();
        for (int r = 0; r < restarts; r++) {
            g = fresh();
            StringBuffer path = new StringBuffer();
            for (int s = 0; s < steps; s++) {
                seed = seed * 1103515245L + 12345L;
                int v = (int) ((seed >> 16) & 0x7FFF);
                int roll = v % 100;
                int a;
                if (roll < 40) {
                    a = 4;                       // FIRE: descend into screens
                } else if (roll < 50) {
                    a = 7;                       // BACK
                } else if (roll < 60) {
                    a = 5 + v % 2;               // SOFT1 / SOFT2
                } else if (roll < 75) {
                    a = v % 4;                   // d-pad
                } else {
                    a = 8 + v % 9;               // number keys
                }
                if (path.length() == 0) {
                    path.append(enc(a));
                } else {
                    path.append(',').append(enc(a));
                }
                g.input(a);
                int m = g.currentMode();
                if (m >= 0 && m < 200 && path.length() / 3 < bestLen[m]) {
                    bestLen[m] = path.length() / 3;
                    bestPath[m] = path.toString();
                }
                hits[m]++;
            }
        }
        int found = 0;
        for (int m = 0; m < 200; m++) {
            if (hits[m] > 0) {
                found++;
                System.out.println("mode " + m + " hits " + hits[m]
                        + " len " + bestLen[m] + " path " + bestPath[m]);
            }
        }
        System.out.println("--- screens reached: " + found);
    }
}
