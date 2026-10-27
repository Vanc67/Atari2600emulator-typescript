package rpc.harness;
import rpc.Db;
import rpc.Part;
import rpc.Inst;
public final class Dbg31 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        rpc.Game g = new rpc.Game();
        g.init(host, gfx);
        Db db = g.db();
        // the biggest disk in the catalogue
        Part best = null;
        for (int i = 0; i < db.count(); i++) {
            Part p = db.get(i);
            if ((p.cat == 5 || p.cat == 6) && (best == null || (p.f[1] & 0xFFFFFFFFL) > (best.f[1] & 0xFFFFFFFFL))) best = p;
        }
        System.out.println("disk=" + best.name + " f[1]=" + best.f[1] + " -> " + (best.f[1] & 0xFFFFFFFFL) + " KB");
        Part mb = null, cpu = null, ram = null;
        for (int i = 0; i < db.count(); i++) {
            Part p = db.get(i);
            if (p.cat == 3 && (mb == null || (p.f[1] & 0xFFFFFFFFL) * Math.max(1, p.f[2]) > (mb.f[1] & 0xFFFFFFFFL) * Math.max(1, mb.f[2]))) mb = p;
            if (p.cat == 1 && (cpu == null || (p.f[1] & 0xFFFFFFFFL) > (cpu.f[1] & 0xFFFFFFFFL))) cpu = p;
            if (p.cat == 4 && (ram == null || (p.f[1] & 0xFFFFFFFFL) > (ram.f[1] & 0xFFFFFFFFL))) ram = p;
        }
        Part[] slots = new Part[8];
        slots[0] = cpu;
        slots[1] = mb;
        for (int k = 0; k < 4; k++) slots[2 + k] = ram;
        slots[1] = cpu;
        slots[7] = best;
        rpc.Inst$Os os = rpc.Inst.all()[Inst.all().length - 1];
        long ramTotal = 0L;
        for (int k = 0; k < 4; k++) ramTotal += (slots[2 + k].f[1] & 0xFFFFFFFFL);
        int b = Inst.blocker(os, slots[0], slots[1], (int)(ramTotal > 2000000000L ? 2000000000L : ramTotal), (long)(best.f[1] & 0xFFFFFFFFL), true, true, true, 1);
        System.out.println("mb=" + mb.name + " f1*f2=" + ((mb.f[1] & 0xFFFFFFFFL) * Math.max(1, mb.f[2])) + " os.minMhz=" + os.minMhz + " patchMinMhz=" + os.patchMinMhz + " minRamKb=" + os.minRamKb);
        System.out.println("OS=" + os.name + " year=" + os.year + " ramKB=" + ramTotal
                + " diskKB=" + (best.f[1] & 0xFFFFFFFFL) + " -> blocker=" + b + " ("
                + (b == 0 ? "installs" : Inst.blockerText(b)) + ")");
        // same with a 5MB disk
        Part tiny = null;
        for (int i = 0; i < db.count(); i++) {
            Part p = db.get(i);
            if (p.cat == 5 && (p.f[1] & 0xFFFFFFFFL) < 65536L && tiny == null) tiny = p;
        }
        if (tiny != null) {
            int b2 = Inst.blocker(os, slots[0], slots[1], (int)(ramTotal > 2000000000L ? 2000000000L : ramTotal), (long)(tiny.f[1] & 0xFFFFFFFFL), true, true, true, 1);
            System.out.println("with tiny disk: blocker=" + b2 + " (" + (b2 == 0 ? "installs" : Inst.blockerText(b2)) + ")");
        }
    }
}
