package rpc.harness;

import rpc.Game;

/**
 * Hold-a-key fuzz: every reachable screen is reopened and each key is then
 * pressed many times in a row.  This is what triggers the wrap-around and
 * divide-by-zero paths (a list scrolled past its end, a filter that empties
 * a list, a counter cycled past its bound) - the bugs a single press never
 * reaches.  Any thrown Throwable is a real crash on the device.
 */
public final class Hold {

    static TestHost host = new TestHost("data");
    static RasterGfx gfx = new RasterGfx(240, 320);

    static Game replay(String path) {
        Game g = new Game();
        g.init(host, gfx);
        if (path != null && path.length() > 0) {
            int i = 0;
            while (i < path.length()) {
                int a = (path.charAt(i) - '0') * 10
                        + (path.charAt(i + 1) - '0');
                g.input(a);
                i += 2;
                if (i < path.length() && path.charAt(i) == ',') {
                    i++;
                }
            }
            g.paint();
        }
        return g;
    }

    public static void main(String[] args) throws Exception {
        int rounds = args.length > 0 ? Integer.parseInt(args[0]) : 60;
        int[] modes = new int[200];
        String[] table = Audit.explore(modes, new int[1]);
        int n = modes[0];
        System.out.println("screens: " + n + ", hold rounds: " + rounds);
        int bad = 0;
        for (int i = 1; i <= n; i++) {
            int m = modes[i];
            String path = table[m];
            for (int a = 0; a < 17; a++) {
                Game g = replay(path);
                if (g.currentMode() != m) {
                    continue;
                }
                try {
                    for (int r = 0; r < rounds; r++) {
                        g.input(a);
                        g.paint();
                        int nm = g.currentMode();
                        if (nm < 0 || nm > 56) {
                            throw new IllegalStateException("mode out of range: "
                                    + nm);
                        }
                    }
                } catch (Throwable t) {
                    bad++;
                    System.out.println("mode " + m + " key " + a
                            + " threw after the press: " + t);
                }
            }
            // alternating UP/DOWN and LEFT/RIGHT, then FIRE+BACK chains
            for (int pair = 0; pair < 4; pair++) {
                Game g = replay(path);
                if (g.currentMode() != m) {
                    continue;
                }
                try {
                    for (int r = 0; r < rounds; r++) {
                        g.input(pair % 2);
                        g.paint();
                        g.input(pair % 2 + 2);
                        g.paint();
                    }
                } catch (Throwable t) {
                    bad++;
                    System.out.println("mode " + m + " pair " + pair
                            + " threw: " + t);
                }
            }
        }
        System.out.println(bad == 0 ? "HOLD CLEAN" : ("HOLD FAILURES: " + bad));
    }
}
