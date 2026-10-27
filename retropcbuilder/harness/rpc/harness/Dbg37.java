package rpc.harness;
import rpc.Game;
import rpc.Funcs;

/** The 500 entries must be a loop, not a checklist to tick off. */
public final class Dbg37 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        g.grantCredits(100000);

        int id = 13;                       // something in the middle
        int cost = Funcs.cost(id);
        int before = g.credits();
        int xp0 = g.totalXp();
        g.applyFunc(id);
        int afterFirst = g.credits();
        int xp1 = g.totalXp();
        System.out.println("id=" + id + " cost=" + cost + " wage=" + Funcs.wage(id)
                + " xp=" + Funcs.xp(id) + " repeatXp=" + Funcs.repeatXp(id));
        System.out.println("first : cash " + before + " -> " + afterFirst
                + "  xp " + xp0 + " -> " + xp1);

        g.applyFunc(id);
        int afterSecond = g.credits();
        int xp2 = g.totalXp();
        System.out.println("repeat: cash " + afterFirst + " -> " + afterSecond
                + "  xp " + xp1 + " -> " + xp2);

        System.out.println("paid wage on repeat: " + (afterSecond == afterFirst + Funcs.wage(id)));
        System.out.println("xp on repeat: " + (xp2 - xp1) + " (want " + Funcs.repeatXp(id) + ")");
        System.out.println("funcs done: " + g.funcsDone());

        // the footer promises "1: выход"
        g.openFuncs();
        System.out.println("funcs mode=" + g.currentMode());
        g.input(8);            // NUM1
        System.out.println("after NUM1: mode=" + g.currentMode() + " (0 = menu, back)");
        g.openFuncs();
        g.input(9);            // NUM2 -> chapter 0
        System.out.println("after NUM2: funcsTab=" + g.funcsTab() + " (want 0)");
        g.input(11);           // NUM4 -> chapter 2
        System.out.println("after NUM4: funcsTab=" + g.funcsTab() + " (want 2)");
    }
}
