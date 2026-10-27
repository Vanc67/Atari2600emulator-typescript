package rpc.harness;
import rpc.Db;
import rpc.Part;
public final class Dbg30 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        rpc.Game g = new rpc.Game();
        g.init(host, gfx);
        Db db = g.db();
        int hdd = 0, ssd = 0;
        for (int i = 0; i < db.count(); i++) {
            Part p = db.get(i);
            if (p.cat == 5 && hdd < 5) {
                System.out.println("HDD cat5 f[1]=" + p.f[1] + " f[2]=" + p.f[2]
                        + " (" + (p.f[1] & 0xFFFFFFFFL) + " as long) name=" + p.name);
                hdd++;
            }
            if (p.cat == 6 && ssd < 3) {
                System.out.println("cat6 f[1]=" + p.f[1] + " (" + (p.f[1] & 0xFFFFFFFFL) + ") name=" + p.name);
                ssd++;
            }
        }
        System.out.println("minDiskKb check: 1024 vs 10240");
    }
}
