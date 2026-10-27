package rpc.harness;
import rpc.Game;
import rpc.Part;
import rpc.Sound;

/** The machine must sound like the machine that was built. */
public final class Dbg41 {
    static TestHost host = new TestHost("data");

    public static void main(String[] a) {
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        g.grantCredits(9999999);

        // what the categories look like, so the noise formula is calibrated
        for (int c = 0; c < 14; c++) {
            Part p = first(g, c);
            if (p != null) {
                System.out.println("cat " + c + " f[0]=" + p.f[0] + " f[1]=" + p.f[1] + " f[3]=" + p.f[3] + " f[4]=" + p.f[4]
                        + " :: " + p.name);
            }
        }

        // ---- a quiet machine: water cooling, solid state, low power ----
        Part cpuQuiet = best(g, 1, 3);          // lowest TDP CPU
        Part gpuQuiet = best(g, 2, 4);          // lowest power GPU
        Part ssd = first(g, 7);
        Part water = first(g, 11);
        Part psu = first(g, 9);
        Part caseP = first(g, 10);
        g.debugSet(0, cpuQuiet);
        g.debugSet(6, gpuQuiet);
        g.debugSet(7, ssd);
        g.debugSet(12, water);
        g.debugSet(10, psu);
        g.debugSet(11, caseP);
        System.out.println("quiet  : noise=" + g.pcNoise()
                + " spinning=" + g.pcSpinningDisk());

        // ---- a loud machine: small fan, spinning disk, hot parts, dusty ----
        Part cpuHot = worst(g, 1, 3);
        Part gpuHot = worst(g, 2, 4);
        Part hdd = first(g, 5);
        Part smallFan = worst(g, 11, 1);
        g.debugSet(0, cpuHot);
        g.debugSet(6, gpuHot);
        g.debugSet(7, hdd);
        g.debugSet(12, smallFan);
        System.out.println("loud   : noise=" + g.pcNoise()
                + " spinning=" + g.pcSpinningDisk());

        // ---- nothing built at all ----
        Game empty = new Game();
        empty.init(host, new RasterGfx(240, 320));
        System.out.println("empty  : noise=" + empty.pcNoise()
                + " spinning=" + empty.pcSpinningDisk());

        // every station in every era has to survive
        int bad = 0;
        for (int st = 0; st < 4; st++) {
            for (int yr = 1981; yr <= 2027; yr += 10) {
                for (int i = 0; i < 3; i++) {
                    try { Sound.radio(st, yr); } catch (Throwable t) { bad++; }
                }
            }
        }
        for (int k = 0; k < 8; k++) {
            try { Sound.playBootVariant(k); } catch (Throwable t) { bad++; }
        }
        for (int k = 0; k < 4; k++) {
            try { Sound.bootFor(1983 + k * 12); } catch (Throwable t) { bad++; }
        }
        for (int n = 0; n <= 100; n += 10) {
            try { Sound.pcSystem(n, 2020, n > 50); } catch (Throwable t) { bad++; }
        }
        System.out.println("audio failures: " + bad);
    }

    static Part first(Game g, int cat) {
        for (int i = 0; i < g.db().count(); i++) {
            Part p = g.db().get(i);
            if (p.cat == cat) return p;
        }
        return null;
    }

    static Part best(Game g, int cat, int field) {
        Part b = null;
        for (int i = 0; i < g.db().count(); i++) {
            Part p = g.db().get(i);
            if (p.cat == cat && (b == null || p.f[field] < b.f[field])) b = p;
        }
        return b;
    }

    static Part worst(Game g, int cat, int field) {
        Part b = null;
        for (int i = 0; i < g.db().count(); i++) {
            Part p = g.db().get(i);
            if (p.cat == cat && (b == null || p.f[field] > b.f[field])) b = p;
        }
        return b;
    }
}
