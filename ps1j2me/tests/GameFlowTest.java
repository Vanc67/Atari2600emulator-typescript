import psx.MemoryCD;
import psx.PS1Machine;

/**
 * End to end test of the real game path: a shipped demo image (bootable
 * ISO9660 with SYSTEM.CNF + PS-X EXE) is opened from the JAR resources, booted
 * through the HLE loader and driven like a player would drive it.
 *
 * <p>What it proves, in one run:</p>
 * <ul>
 *   <li>the disc image is read through the rolling stream and the sector cache;</li>
 *   <li>the HLE BIOS loads the program and the CPU really starts executing it;</li>
 *   <li>the GPU output changes between frames (the game is animating, not stuck);</li>
 *   <li>the pad bits travel the whole way: SIO0 → game logic → GPU → VRAM;</li>
 *   <li>the game keeps reading the pad every frame, so a held button is seen.</li>
 * </ul>
 */
public final class GameFlowTest {

    private static int passed;
    private static int failed;

    /* Pong draws the player paddle as a white 8x40 bar at x = 24..31. */
    private static final int PADDLE_X = 26;
    private static final int WHITE = 0xFFFFFF;

    /* Wire bits of the pad (active low). */
    private static final int BIT_UP = 4;
    private static final int BIT_DOWN = 6;

    public static void main(String[] args) throws Exception {
        MemoryCD image = MemoryCD.fromResource(GameFlowTest.class,
                "res/demos/demo_pong.iso", "pong");
        check(image != null, "demo image is on the classpath");
        if (image == null) {
            System.out.println("GAME FLOW TESTS FAILED: no image");
            System.exit(1);
            return;
        }

        PS1Machine m = new PS1Machine(image);
        m.setViewport(320, 240);
        boolean booted = m.hleBoot(image);
        check(booted, "hle boot of the demo image");
        if (!booted) {
            System.out.println("GAME FLOW TESTS FAILED: " + m.bootInfo);
            System.exit(1);
            return;
        }

        /* Let the program settle: the first frames run the GPU setup. */
        runFrames(m, 10);
        check(m.framesRun >= 10, "frames were executed (" + m.framesRun + ")");
        check(m.getPixelsWritten() > 1000, "GPU wrote pixels (" + m.getPixelsWritten() + ")");

        int paddleTop = paddleTop(m);
        check(paddleTop > 0, "player paddle is on screen (top y=" + paddleTop + ")");

        /* ---- the picture must change by itself (the ball moves) ---------- */
        int[] frameA = copy(m.getFrame());
        runFrames(m, 12);
        int[] frameB = copy(m.getFrame());
        check(differs(frameA, frameB), "the picture changes between frames (animation)");

        /* ---- holding DOWN must move the player paddle down --------------- */
        m.setButtons(0xFFFF & ~(1 << BIT_DOWN));
        runFrames(m, 8);
        int afterDown = paddleTop(m);
        check(afterDown > paddleTop,
                "holding pad DOWN moves the paddle (" + paddleTop + " -> " + afterDown + ")");

        /* ---- holding UP must move it back -------------------------------- */
        m.setButtons(0xFFFF & ~(1 << BIT_UP));
        runFrames(m, 8);
        int afterUp = paddleTop(m);
        check(afterUp < afterDown,
                "holding pad UP moves the paddle back (" + afterDown + " -> " + afterUp + ")");

        /* ---- releasing must stop the movement ---------------------------- */
        m.setButtons(0xFFFF);
        runFrames(m, 2);
        int released = paddleTop(m);
        runFrames(m, 6);
        int stillThere = paddleTop(m);
        check(released == stillThere,
                "released pad stops the paddle (" + released + " -> " + stillThere + ")");

        /* ---- the program must never take a bogus exception --------------- */
        check(m.getExceptionCount() == 0,
                "no exceptions during playback (got " + m.getExceptionCount()
                        + ", last code " + m.getLastExceptionCode() + ")");

        /* ---- the emulator itself must stay healthy ----------------------- */
        check(m.cpu.gpr[0] == 0, "register zero stays zero");
        int pc = m.cpu.pc;
        check((pc & 0xFF000000) == 0x80000000, "pc stays in KSEG0 (0x" + Integer.toHexString(pc) + ")");
        check(!m.halted, "the program did not halt");

        String[] lines = m.bootInfo == null ? new String[0] : new String[] { m.bootInfo };
        if (lines.length > 0 && lines[0] != null) {
            System.out.println("  boot: " + lines[0]);
        }

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL GAME FLOW TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("GAME FLOW TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    /** Topmost white pixel of the player paddle column, or -1. */
    private static int paddleTop(PS1Machine m) {
        short[] vram = m.gpu.vram;
        for (int y = 0; y < 240; y++) {
            int px = vram[y * 1024 + PADDLE_X] & 0xFFFF;
            /* 15 bit VRAM: 0x7FFF is white; the paddle is drawn white */
            if (px == (WHITE & 0x7FFF)) return y;
        }
        return -1;
    }

    private static void runFrames(PS1Machine m, int n) {
        for (int i = 0; i < n; i++) m.runFrame();
    }

    private static int[] copy(int[] frame) {
        int[] out = new int[frame.length];
        System.arraycopy(frame, 0, out, 0, frame.length);
        return out;
    }

    private static boolean differs(int[] a, int[] b) {
        if (a.length != b.length) return true;
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) diff++;
        }
        return diff > 50;
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what);
        } else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }
}
