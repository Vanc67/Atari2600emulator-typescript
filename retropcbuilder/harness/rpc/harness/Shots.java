package rpc.harness;

import rpc.Game;

/**
 * Renders scripted sessions of the real game into BMP frames.
 *
 *   java -cp build/classes:build/harness-classes rpc.harness.Shots out/
 *
 * The frames are the exact pixels the handset would show: the game draws
 * through the same Gfx interface, only the backend differs.
 */
public final class Shots {

    private static final int W = 240;
    private static final int H = 320;

    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int LEFT = 2;
    private static final int RIGHT = 3;
    private static final int FIRE = 4;
    private static final int SOFT1 = 5;
    private static final int SOFT2 = 6;
    private static final int BACK = 7;
    private static final int NUM = 8; /* NUM + (n-1) */

    private static int[] cat(int[] a, int[] b) {
        int[] r = new int[a.length + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length, b.length);
        return r;
    }

    private static int[] rep(int v, int n) {
        int[] r = new int[n];
        for (int i = 0; i < n; i++) {
            r[i] = v;
        }
        return r;
    }

    /** Each scenario: name, then the keys pressed before the frame. */
    private static final Object[][] SCRIPT = {
        { "01_menu", new int[] {} },
        { "02_menu_sel", new int[] { DOWN, DOWN } },
        { "03_room", new int[] { FIRE } },
        { "04_room_tea", new int[] { FIRE, NUM + 1 } },
        { "05_build", new int[] { NUM + 1 } },
        { "05b_room_sleep", new int[] { FIRE, NUM + 4, FIRE } },
        { "05c_room_cursor", new int[] { FIRE, NUM + 3 } },
        { "05d_room_radio", new int[] { FIRE, NUM + 7 } },
        { "06_build_shop", new int[] { NUM + 1, FIRE } },
        { "07_lab", new int[] { NUM + 5 } },
        { "08_city", new int[] { FIRE, NUM + 5, FIRE } },
        { "09_tools", new int[] { FIRE, NUM + 6 } },
        { "09b_tools_scope", new int[] { FIRE, NUM + 6, NUM + 4 } },
        { "09c_tools_caps", new int[] { FIRE, NUM + 6, NUM + 1 } },
        { "09d_tools_air", new int[] { FIRE, NUM + 6, NUM + 3 } },
        { "10_catalog", new int[] { NUM + 4 } },
        { "11_calendar", new int[] { NUM + 6 } },
        { "12_about", new int[] { NUM + 7 } },
        { "13_funcs", new int[] { NUM + 8 } },
        { "13b_funcs_chapter", new int[] { NUM + 8, RIGHT, RIGHT } },
        { "13c_funcs_done", new int[] { NUM + 8, FIRE, FIRE, DOWN, FIRE } },
        { "14_room_walk", new int[] { FIRE, RIGHT, RIGHT, RIGHT } },
        { "14b_room_walk2", new int[] { FIRE, LEFT, LEFT, LEFT, LEFT, DOWN, DOWN } },
        { "14c_room_near", new int[] { FIRE, LEFT, LEFT } },
        { "15_city_bazaar", new int[] { FIRE, NUM + 5, RIGHT, RIGHT } },
        { "15b_city_shop", new int[] { FIRE, NUM + 5, LEFT } },
        { "16_ending", cat(new int[] { NUM + 6 },
                cat(rep(RIGHT, 48), new int[] { UP })) },
    };

    public static void main(String[] args) {
        String outDir = args.length > 0 ? args[0] : "docs/screens";
        String dataDir = "data";
        if (args.length > 1) {
            dataDir = args[1];
        }
        TestHost host = new TestHost(dataDir);
        RasterGfx gfx = new RasterGfx(W, H);
        long t0 = System.currentTimeMillis();
        Game warm = new Game();
        warm.init(host, gfx);
        System.out.println("[shots] init " + (System.currentTimeMillis() - t0) + " ms");

        for (int i = 0; i < SCRIPT.length; i++) {
            String name = (String) SCRIPT[i][0];
            int[] keys = (int[]) SCRIPT[i][1];
            // every scenario starts from a fresh session in the main menu
            Game game = new Game();
            game.init(host, gfx);
            for (int k = 0; k < keys.length; k++) {
                try {
                    game.input(keys[k]);
                } catch (Throwable t) {
                    System.out.println("[shots] " + name + " input " + keys[k]
                            + " failed: " + t);
                }
                // repaint a few frames so animations settle before the next
                // key lands on the right screen
                for (int f = 0; f < 6; f++) {
                    gfx.setClip(0, 0, W, H);
                    try {
                        game.paint();
                    } catch (Throwable t) {
                        System.out.println("[shots] " + name + " paint failed: " + t);
                        break;
                    }
                }
            }
            gfx.setClip(0, 0, W, H);
            game.paint();
            Bmp.write(outDir + "/" + name + ".bmp", gfx.pixels, W, H);
            System.out.println("[shots] " + name + " mode=" + game.currentMode());
        }
        Wav.flush(outDir + "/audio.wav");
        System.out.println("[shots] done");
    }
}
