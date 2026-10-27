package rpc.harness;
import rpc.Game;
import rpc.Room;
import rpc.LifeSim;

/** Renders the room window at every season / time of day into BMP frames. */
public final class RoomShot {
    static final int[] SEASONS = {0, 1, 2, 3};
    static final int[] TIMES = {0, 1, 2, 3};
    static final String[] SN = {"winter", "spring", "summer", "autumn"};
    static final String[] TN = {"day", "sunset", "night", "morning"};

    public static void main(String[] a) {
        String out = (a.length > 0) ? a[0] : "build/shots/room";
        TestHost host = new TestHost("data");
        for (int s = 0; s < 4; s++) {
            for (int t = 0; t < 4; t++) {
                RasterGfx gfx = new RasterGfx(240, 320);
                Game g = new Game();
                g.init(host, gfx);
                g.life.season = SEASONS[s];
                g.life.timeOfDay = TIMES[t];
                g.life.weather = w(s, t);
                g.life.animationTick = 60 + s * 7;
                Room.draw(gfx, g.life, g.life.animationTick, true, 320);
                String name = out + "/" + SN[s] + "_" + TN[t];
                Bmp.write(name + ".bmp", gfx.pixels, 240, 320);
                System.out.println("wrote " + name + ".bmp");
            }
        }
    }

    static int w(int s, int t) {
        if (s == 0) return 3;   // winter: snow
        if (s == 2 && t == 2) return 1;  // summer night: rain
        return 0;
    }
}
