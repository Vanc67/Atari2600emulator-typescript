package rpc.harness;
import rpc.Game; import rpc.Room; import rpc.LifeSim;
public final class Dbg34 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        for (int season = 0; season < 4; season++) {
            for (int t = 0; t < 4; t++) {
                RasterGfx gfx = new RasterGfx(240, 320);
                Game g = new Game();
                g.init(host, gfx);
                g.life.season = season; g.life.timeOfDay = t; g.life.weather = 0;
                Room.draw(gfx, g.life, 60, true, 320);
                // the window rect: wx=16, wy=16, ww=91, wh=98
                int wx = 16, wy = 16, ww = 91, wh = 98;
                StringBuffer sb = new StringBuffer();
                sb.append(name(season) + " " + tname(t) + ": ");
                int[] ys = {20, 30, 40, 50, 60, 70, 80, 90, 105, 115};
                for (int i = 0; i < ys.length; i++) {
                    int y = ys[i];
                    if (y < wy || y >= wy + wh) continue;
                    int s = 0; int n = 0;
                    for (int x = wx + 6; x < wx + ww - 6; x += 2) {
                        s += gfx.pixels[y * 240 + x]; n++;
                    }
                    sb.append(hex(s / n)).append(' ');
                }
                System.out.println(sb.toString());
            }
        }
    }
    static String name(int s) { return new String[]{"win","spr","sum","aut"}[s]; }
    static String tname(int t) { return new String[]{"day","sun","ngt","morn"}[t]; }
    static String hex(int p) {
        String h = "0123456789ABCDEF";
        StringBuffer b = new StringBuffer("#");
        b.append(h.charAt(((p >> 16) & 255) >> 4));
        b.append(h.charAt(((p >> 16) & 255) & 15));
        b.append(h.charAt((((p >> 8) & 255) >> 4)));
        b.append(h.charAt(((p >> 8) & 255) & 15));
        b.append(h.charAt(((p & 255) >> 4)));
        b.append(h.charAt((p & 255) & 15));
        return b.toString();
    }
}
