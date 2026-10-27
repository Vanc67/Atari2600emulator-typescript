package rpc.harness;
import rpc.Game;

/** The разгон tabs: number keys must do what the screen labels say. */
public final class Dbg38 {
    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        Game g = new Game();
        g.init(host, gfx);
        g.openExpRun(0);


        // footer "1-4: вкладки"
        g.expTab = 0;
        g.input(11);                 // NUM4
        System.out.println("NUM4 on tab 0 -> expTab=" + g.expTab + " (want 3)");
        g.input(8);                  // NUM1
        System.out.println("NUM1 -> expTab=" + g.expTab + " (want 0)");

        // the tweak list is labelled "press 1-6"
        g.expTab = 2;
        int before = g.expStress.activeTweaks;
        g.input(8);                  // NUM1 toggles tweak 0
        System.out.println("NUM1 on tab 2: activeTweaks " + before + " -> "
                + g.expStress.activeTweaks + " (want bit0 toggled)");
        int one = g.expStress.activeTweaks;
        g.input(13);                 // NUM6 toggles tweak 5
        System.out.println("NUM6 on tab 2: bit5 now "
                + (((g.expStress.activeTweaks >> 5) & 1) == 1) + ", bit0 kept "
                + (((g.expStress.activeTweaks >> 0) & 1) == ((one >> 0) & 1)));

        // the tap path still toggles the same way
        g.input(9);                  // NUM2 toggles tweak 1
        System.out.println("NUM2 on tab 2: bit1 "
                + (((g.expStress.activeTweaks >> 1) & 1) == 1));
    }
}
