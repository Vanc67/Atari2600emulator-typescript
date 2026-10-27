package rpc.harness;
import rpc.Game;
public final class MenuWalk {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        Game g = new Game();
        g.init(host, new RasterGfx(240, 320));
        g.input(Game.ACT_BACK);            // cancel the first-run picker
        System.out.println("menu items=" + g.menuItems());
        for (int i = 0; i < 11; i++) {
            g.input(Game.ACT_DOWN);
            g.paint();
        }
        System.out.println("after 11 DOWN menuSel wraps, mode=" + g.mode());
        // every number key jumps to its row and acts
        for (int k = 8; k <= 16; k++) {
            Game m = new Game();
            m.init(host, new RasterGfx(240, 320));
            m.input(Game.ACT_BACK);
            m.input(k);
            m.paint();
            System.out.println("key " + (k - 8) + " -> mode " + m.mode());
        }
    }
}
