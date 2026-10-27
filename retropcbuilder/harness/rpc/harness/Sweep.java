package rpc.harness;
import rpc.Game;

/**
 * Sends every key to every screen it can reach and reports the keys that
 * change nothing at all - no mode change, no toast, no state - and any key
 * that throws.
 */
public final class Sweep {
    static final int KEYS = 17;

    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        for (int trial = 0; trial < PATHS.length; trial++) {
            RasterGfx gfx = new RasterGfx(240, 320);
            Game g = new Game();
            g.init(host, gfx);
            int[] path = PATHS[trial];
            for (int i = 0; i < path.length; i++) {
                g.input(path[i]);
            }
            int mode = g.currentMode();
            StringBuffer dead = new StringBuffer();
            StringBuffer errs = new StringBuffer();
            for (int k = 0; k < KEYS; k++) {
                // a fresh game per key keeps the sweep independent
                RasterGfx g2 = new RasterGfx(240, 320);
                Game n = new Game();
                n.init(host, g2);
                for (int i = 0; i < path.length; i++) {
                    n.input(path[i]);
                }
                String t0 = n.toastText();
                int m0 = n.currentMode();
                String snap0 = snap(n);
                try {
                    n.input(k);
                } catch (Throwable t) {
                    errs.append("  key " + k + " THREW " + t.getClass().getName());
                    continue;
                }
                if (n.currentMode() == m0 && n.toastText().equals(t0)
                        && snap(n).equals(snap0) && n.currentMode() != 0) {
                    dead.append(" " + k);
                }
            }
            System.out.println("mode " + pad(mode) + " " + names(mode)
                    + " dead:" + (dead.length() == 0 ? "-" : dead.toString())
                    + (errs.length() == 0 ? "" : "  ERRORS:" + errs));
        }
    }

    static String snap(Game g) {
        StringBuffer b = new StringBuffer();
        b.append(g.credits()).append(',').append(g.totalXp()).append(',')
         .append(g.dayNumber()).append(',').append(g.funcsDone()).append(',')
         .append(g.expTab).append(',').append(g.funcsTab()).append(',')
         .append(g.buildSelection()).append(',').append(g.roomCursor()).append(',')
         .append(g.mapSelection()).append(',').append(g.tableCount()).append(',')
         .append(g.funcsSel()).append(',').append(g.partListLen()).append(',')
         .append(g.offerCount()).append(',').append(g.roomCursor());
        return b.toString();
    }

    static String pad(int m) {
        if (m < 10) return " " + m + " ";
        return String.valueOf(m) + " ";
    }

    static String names(int m) {
        String[] n = {"menu","map","build","shop","result","museum","about","catalog",
            "catparts","market","offers","orders","bench","os","osprogs","editor",
            "osrun","tasks","net","mine","setup","oslist","install","bsod","spec",
            "web","game","post","lang","shell","browse","corp","chip","corpose",
            "soft","prods","games","winamp","mines","snake","paint","bench3d",
            "paste","bootanim","repair","lab","overdrive","room","city",
            "workshop","floppy","defrag","news","tea","sleep","funcs","ending"};
        return (m >= 0 && m < n.length) ? n[m] : "?";
    }

    static final int[][] PATHS = {
        {},                                  // 0 menu
        {9},                                  // 1 build
        {9, 4},                               // shop
        {8},                                  // room
        {13},                                 // lab list
        {13, 4},                              // overdrive
        {16},                                 // funcs
        {11},                                 // calendar / news
        {12},                                 // about
        {10},                                 // catalog
        {10, 4},                              // catparts
        {9, 4, 4},                            // spec
    };
}
