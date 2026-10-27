package rpc.harness;
import rpc.Game;

/** Renders the opening screen, the menu it sits on, and the street it opens. */
public final class StartShot {
    public static void main(String[] a) {
        String out = (a.length > 0) ? a[0] : "build/shots/start";
        TestHost host = new TestHost("data");

        // the picker, exactly as a fresh device opens on it
        RasterGfx g1 = new RasterGfx(240, 320);
        Game p = new Game();
        p.init(host, g1);
        for (int i = 0; i < 3; i++) p.paint();
        Bmp.write(out + "/picker.bmp", g1.pixels, 240, 320);

        // the same picker with the year walked and the row moved
        p.input(Game.ACT_NUM1);
        for (int i = 0; i < 9; i++) p.input(Game.ACT_RIGHT);
        p.input(Game.ACT_DOWN);
        p.input(Game.ACT_DOWN);
        p.input(Game.ACT_DOWN);
        for (int i = 0; i < 3; i++) p.paint();
        Bmp.write(out + "/picker_edit.bmp", g1.pixels, 240, 320);

        // the menu, now with the start entry at the top
        RasterGfx g2 = new RasterGfx(240, 320);
        Game m = new Game();
        m.init(host, g2);
        m.input(Game.ACT_BACK);
        for (int i = 0; i < 3; i++) m.paint();
        Bmp.write(out + "/menu.bmp", g2.pixels, 240, 320);

        // the street the picker opens onto: an evening in winter, in the snow
        RasterGfx g3 = new RasterGfx(240, 320);
        Game c = new Game();
        c.init(host, g3);
        c.openStart();
        c.input(Game.ACT_FIRE);
        for (int i = 0; i < 3; i++) c.paint();
        Bmp.write(out + "/street.bmp", g3.pixels, 240, 320);

        // the room behind it, with its own clock on the wall
        RasterGfx g4 = new RasterGfx(240, 320);
        Game r = new Game();
        r.init(host, g4);
        r.openStart();
        r.input(Game.ACT_FIRE);
        r.input(Game.ACT_BACK);
        r.input(Game.ACT_NUM2);
        for (int i = 0; i < 3; i++) r.paint();
        Bmp.write(out + "/room.bmp", g4.pixels, 240, 320);
        System.out.println("shots in " + out);
    }
}
