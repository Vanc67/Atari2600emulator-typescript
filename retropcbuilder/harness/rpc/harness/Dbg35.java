package rpc.harness;
import rpc.Game; import rpc.Room; import rpc.LifeSim;
public final class Dbg35 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        g.life.season = 2; g.life.timeOfDay = 1; g.life.weather = 0;
        Room.draw(gfx, g.life, 60, true, 320);
        int[] xy = {20*240+50, 30*240+50, 50*240+50, 80*240+50, 100*240+50, 150*240+50};
        int[] xy2 = {16*240+50, 16*240+16, 16*240+100, 320-1, 240-1};
        for (int i = 0; i < xy.length; i++) System.out.println("(" + (xy[i] % 240) + "," + (xy[i]/240) + ") = #" + h(gfx.pixels[xy[i]]));
        for (int i = 0; i < xy2.length; i++) System.out.println("(" + (xy2[i] % 240) + "," + (xy2[i]/240) + ") = #" + h(gfx.pixels[xy2[i]]));
    }
    static String h(int p) {
        String s = "0123456789ABCDEF";
        StringBuffer b = new StringBuffer();
        b.append(s.charAt(((p >> 16) & 255) >> 4)).append(s.charAt(((p >> 16) & 255) & 15));
        b.append(s.charAt(((p >> 8) & 255) >> 4)).append(s.charAt(((p >> 8) & 255) & 15));
        b.append(s.charAt(((p & 255) >> 4))).append(s.charAt((p & 255) & 15));
        return b.toString();
    }
}
