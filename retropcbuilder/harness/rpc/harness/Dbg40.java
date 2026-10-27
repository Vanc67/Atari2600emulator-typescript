package rpc.harness;
import rpc.Game;
import rpc.Sound;

/** The new sound set: every entry has to survive a call without throwing. */
public final class Dbg40 {
    static int bad = 0;
    static int calls = 0;

    public static void main(String[] a) {
        TestHost host = new TestHost("data");
        RasterGfx gfx = new RasterGfx(240, 320);
        final Game g = new Game();
        g.init(host, gfx);

        for (int k = 0; k < 8; k++) {
            for (int i = 0; i < 5; i++) {
                try { Sound.playBootVariant(k); ok(); }
                catch (Throwable t) { fail("bootVariant" + k, t); }
            }
        }
        for (int i = 0; i < 40; i++) {
            try { Sound.boot(); ok(); } catch (Throwable t) { fail("boot", t); }
        }
        for (int st = 0; st < 4; st++) {
            for (int i = 0; i < 6; i++) {
                try { Sound.radio(st, 2020); ok(); } catch (Throwable t) { fail("radio" + st, t); }
            }
        }
        for (int i = 0; i < 30; i++) {
            try { Sound.car(); ok(); } catch (Throwable t) { fail("car", t); }
            try { Sound.horn(); ok(); } catch (Throwable t) { fail("horn", t); }
            try { Sound.slam(); ok(); } catch (Throwable t) { fail("slam", t); }
            try { Sound.street(); ok(); } catch (Throwable t) { fail("street", t); }
        }
        for (int i = 0; i < 20; i++) {
            try { Sound.rain(2); ok(); } catch (Throwable t) { fail("rain2", t); }
            try { Sound.rain(3); ok(); } catch (Throwable t) { fail("rain3", t); }
            try { Sound.snow(); ok(); } catch (Throwable t) { fail("snow", t); }
            try { Sound.thunder(); ok(); } catch (Throwable t) { fail("thunder", t); }
        }
        g.life.radioOn = false;
        try { g.life.toggleRadio(); ok(); } catch (Throwable t) { fail("radio on", t); }
        System.out.println("calls=" + calls + " failures=" + bad);
        System.out.println((bad == 0) ? "SOUND OK" : "SOUND BROKEN");
    }

    static void ok() { calls++; }
    static void fail(String what, Throwable t) {
        bad++;
        calls++;
        System.out.println("THREW " + what + ": " + t);
    }
}
