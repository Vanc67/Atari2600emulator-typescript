package rpc.harness;
import rpc.Game;
public final class M9 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        Game g = new Game();
        g.init(host, new RasterGfx(240, 320));
        g.input(Game.ACT_BACK);
        for (int i = 0; i < 9; i++) g.input(Game.ACT_DOWN);
        g.input(Game.ACT_FIRE);
        System.out.println("9 DOWN + FIRE -> mode " + g.mode() + " (6 = about)");
    }
}
