package rpc.harness;
import rpc.Game;

/** An accidental exit to the menu must not lose the PC being built. */
public final class Dbg36 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        g.grantCredits(5000);
        System.out.println("credits after grant: " + g.credits());

        g.input(8);            // menu item 0 = Комната
        System.out.println("after NUM1 on the menu: mode=" + g.currentMode());
        g.input(7);            // BACK out of the room
        System.out.println("after BACK: mode=" + g.currentMode() + " (menu=0)");

        String saved = host.load("save");
        System.out.println("save written: " + (saved != null && saved.length() > 0)
                + " len=" + (saved == null ? 0 : saved.length())
                + " head=" + (saved == null ? "-" : saved.substring(0, Math.min(24, saved.length()))));

        Game g2 = new Game();
        g2.init(host, new RasterGfx(240, 320));
        System.out.println("fresh game credits=" + g2.credits() + " day=" + g2.dayNumber());
        System.out.println(g2.credits() == g.credits() ? "SAVE OK" : "SAVE LOST");
    }
}
