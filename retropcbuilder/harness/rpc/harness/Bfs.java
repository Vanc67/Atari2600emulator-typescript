package rpc.harness;

import rpc.Game;
import rpc.LifeSim;

/**
 * Breadth-first sweep: every key sequence up to the state cap is replayed
 * on a fresh game and the snapshot (mode + all selections) is used to
 * deduplicate.  This reaches the screens a random walk never finds:
 * BIOS, editor, the in-game games, the finale.
 */
public final class Bfs {

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
        int cap = args.length > 0 ? Integer.parseInt(args[0]) : 400;
        int perMode = args.length > 1 ? Integer.parseInt(args[1]) : 3;
        // Two parallel queues: the action path and the mode it ends in.
        StrQueue paths = new StrQueue(200000);
        int[] qMode = new int[200000];
        int[] qTail = new int[1];
        int[] count = new int[200];
        int[] bestLen = new int[200];
        String[] bestPath = new String[200];
        int[] hits = new int[200];
        for (int i = 0; i < 200; i++) {
            bestLen[i] = 1 << 30;
        }
        paths.add("");
        qMode[0] = 0;
        qTail[0] = 1;
        int expanded = 0;
        long t0 = System.currentTimeMillis();
        int qi = 0;
        while (qi < qTail[0] && expanded < cap) {
            String path = paths.poll();
            if (path == null) {
                break;
            }
            int mode = qMode[qi];
            qi++;
            if (count[mode] >= perMode) {
                continue;
            }
            count[mode]++;
            expanded++;
            if (path.length() / 3 < bestLen[mode]) {
                bestLen[mode] = path.length() / 3;
                bestPath[mode] = path;
            }
            for (int a = 0; a < 17; a++) {
                String np = path.length() == 0 ? enc(a) : path + "," + enc(a);
                Game g = fresh();
                try {
                    apply(g, np);
                } catch (Throwable t) {
                    continue;
                }
                int nm = g.currentMode();
                if (nm >= 0 && nm < 200 && count[nm] < perMode
                        && qTail[0] < qMode.length) {
                    paths.add(np);
                    qMode[qTail[0]] = nm;
                    qTail[0]++;
                    hits[nm]++;
                }
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
        System.out.println("--- expanded " + expanded + ", screens reached: "
                + found + ", " + (System.currentTimeMillis() - t0) + " ms");
    }
}
/** Tiny open-addressed string set: CLDC 1.1 has no java.util.HashSet. */
final class StrSet {
    String[] keys;
    int[] next;
    int[] head;
    int count;

    StrSet(int capacity) {
        this.keys = new String[capacity];
        this.next = new int[capacity];
        this.head = new int[1024];
        for (int i = 0; i < 1024; i++) {
            this.head[i] = -1;
        }
    }

    int hash(String s) {
        int h = 0;
        for (int i = 0; i < s.length(); i++) {
            h = (h * 31 + s.charAt(i)) & 0x7FFFFFFF;
        }
        return h % 1024;
    }

    boolean add(String s) {
        if (this.count >= this.keys.length) {
            return false;
        }
        int b = hash(s);
        for (int i = this.head[b]; i >= 0; i = this.next[i]) {
            if (this.keys[i].equals(s)) {
                return false;
            }
        }
        int idx = this.count++;
        this.keys[idx] = s;
        this.next[idx] = this.head[b];
        this.head[b] = idx;
        return true;
    }
}

/** Array queue of strings. */
final class StrQueue {
    String[] a;
    int head;
    int tail;

    StrQueue(int n) {
        this.a = new String[n];
    }

    void add(String s) {
        this.a[this.tail++] = s;
    }

    String poll() {
        if (this.head >= this.tail) {
            return null;
        }
        return this.a[this.head++];
    }

    int size() {
        return this.tail - this.head;
    }
}
