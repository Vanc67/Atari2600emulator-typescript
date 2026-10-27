package rpc.harness;
import rpc.Db; import rpc.Part; import rpc.Inst;
public final class Dbg32 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        rpc.Game g = new rpc.Game();
        g.init(host, gfx);
        Db db = g.db();
        for (int c = 0; c < 24; c++) {
            int n = 0; Part best = null;
            for (int i = 0; i < db.count(); i++) { Part p = db.get(i); if (p.cat == c) { n++; if (best == null || (p.f[1] & 0xFFFFFFFFL) * Math.max(1, p.f[2]) > (best.f[1] & 0xFFFFFFFFL) * Math.max(1, best.f[2])) best = p; } }
            if (n > 0) System.out.println("cat " + c + " n=" + n + " best f1=" + (best.f[1] & 0xFFFFFFFFL) + " f2=" + best.f[2] + " f0=" + best.f[0] + " :: " + best.name);
        }
    }
}
