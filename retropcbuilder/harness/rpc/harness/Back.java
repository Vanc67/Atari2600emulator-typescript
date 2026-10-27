package rpc.harness;
import rpc.Game;
public final class Back {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        String[] paths = { "09", "09,04", "04", "04,10", "04,10,03,04", "09,04,03" };
        int[] keys = { 7, 5, 6, 1, 0, 4 };
        for (int p = 0; p < paths.length; p++) {
            Game g = new Game();
            g.init(host, gfx);
            int i = 0;
            while (i < paths[p].length()) {
                g.input((paths[p].charAt(i) - '0') * 10 + (paths[p].charAt(i + 1) - '0'));
                i += 2;
                if (i < paths[p].length() && paths[p].charAt(i) == ',') i++;
            }
            g.paint();
            StringBuffer sb = new StringBuffer();
            sb.append("path " + paths[p] + " mode " + g.currentMode());
            for (int k = 0; k < keys.length; k++) {
                Game t = new Game();
                t.init(host, gfx);
                i = 0;
                while (i < paths[p].length()) {
                    t.input((paths[p].charAt(i) - '0') * 10 + (paths[p].charAt(i + 1) - '0'));
                    i += 2;
                    if (i < paths[p].length() && paths[p].charAt(i) == ',') i++;
                }
                t.paint();
                int before = t.currentMode();
                t.input(keys[k]);
                t.paint();
                sb.append(" | key " + keys[k] + ":" + before + "->" + t.currentMode());
            }
            System.out.println(sb.toString());
        }
    }
}
