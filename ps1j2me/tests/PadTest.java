import psx.Pad;

/**
 * Desktop tests for the SIO0 pad (the only input path of the emulator).
 *
 * <p>The digital pad answers {@code FF 41 5A <lo> <hi>} and the two button
 * bytes are active low. Real games differ in how they drive the port: most
 * deselect it between reads, some keep it selected and just repeat the
 * {@code 01 42 00 00 00} frame. Both have to work, and a held direction plus a
 * face button must not cancel each other out.</p>
 */
public final class PadTest {

    private static int passed;
    private static int failed;

    /* Bit positions on the wire (active low). */
    private static final int SELECT = 0;
    private static final int START = 3;
    private static final int UP = 4;
    private static final int RIGHT = 5;
    private static final int DOWN = 6;
    private static final int LEFT = 7;
    private static final int L1 = 10;
    private static final int R1 = 11;
    private static final int TRIANGLE = 12;
    private static final int CIRCLE = 13;
    private static final int CROSS = 14;
    private static final int SQUARE = 15;

    public static void main(String[] args) {
        idleFrame();
        singleButton();
        twoButtonsAtOnce();
        releaseIsSeen();
        repeatedFrameWithoutDeselect();
        ackPulses();
        rxFifoClears();
        resetPort();
        noDevice();
        allButtons();

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL PAD TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("PAD TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    /** The five byte exchange with nothing pressed. */
    private static void idleFrame() {
        Pad pad = new Pad();
        pad.reset();
        int[] r = exchange(pad);
        same(0xFF, r[0], "byte 0 echo");
        same(0x41, r[1], "byte 1 pad id");
        same(0x5A, r[2], "byte 2 ready");
        same(0xFF, r[3], "byte 3 buttons low (nothing pressed)");
        same(0xFF, r[4], "byte 4 buttons high");
    }

    private static void singleButton() {
        Pad pad = new Pad();
        pad.reset();
        pad.setButtons(press(CROSS));
        int[] r = exchange(pad);
        /* bits 0..7 are the low byte, 8..15 the high one: cross is bit 14 */
        check((r[4] & (1 << 6)) == 0, "cross clears bit 14 (high byte bit 6)");
        check((r[4] & 0xBF) == 0xBF, "no other high bit is touched by cross");
        same(0xFF, r[3], "cross does not touch the low byte");

        pad.reset();
        pad.setButtons(press(UP));
        r = exchange(pad);
        check((r[3] & (1 << 4)) == 0, "up clears bit 4");
        pad.reset();
        pad.setButtons(press(START));
        r = exchange(pad);
        check((r[3] & (1 << 3)) == 0, "start clears bit 3");
        pad.reset();
        pad.setButtons(press(TRIANGLE));
        r = exchange(pad);
        check((r[4] & (1 << 4)) == 0, "triangle clears bit 12 (high byte bit 4)");
    }

    /** Direction and fire at the same time: the classic "run and jump". */
    private static void twoButtonsAtOnce() {
        Pad pad = new Pad();
        pad.reset();
        pad.setButtons(press(RIGHT) & press(CROSS));
        int[] r = exchange(pad);
        check((r[3] & (1 << 5)) == 0, "right is held while cross is held");
        check((r[4] & (1 << 6)) == 0, "cross is held while right is held");
        check((r[3] & (1 << 4)) != 0, "up stays released");
        check((r[3] & (1 << 7)) != 0, "left stays released");

        pad.reset();
        pad.setButtons(press(L1) & press(R1) & press(UP));
        r = exchange(pad);
        check((r[4] & (1 << 2)) == 0 && (r[4] & (1 << 3)) == 0, "both shoulders held");
        check((r[3] & (1 << 4)) == 0, "up held together with the shoulders");
    }

    private static void releaseIsSeen() {
        Pad pad = new Pad();
        pad.reset();
        pad.setButtons(press(CROSS));
        same(0xFF & ~(1 << 6), exchange(pad)[4], "cross pressed");
        pad.setButtons(0xFFFF);
        same(0xFF, exchange(pad)[3], "cross released");
    }

    /**
     * Some games keep the port selected and repeat the frame without writing
     * JOY_CTRL in between. The pad has to notice the 0x01 address byte.
     */
    private static void repeatedFrameWithoutDeselect() {
        Pad pad = new Pad();
        pad.reset();
        pad.write(0xA, 0x0003);                 /* select + TX/RX enable */
        pad.setButtons(press(SQUARE));
        int[] first = exchange(pad);
        int[] second = exchange(pad);
        same(0x41, first[1], "first frame id");
        same(0x41, second[1], "second frame id");
        same(first[4], second[4], "second frame reports the same buttons");
        check((second[4] & (1 << 7)) == 0, "square still visible in the second frame");
    }

    private static void ackPulses() {
        Pad pad = new Pad();
        pad.reset();
        pad.write(0xA, 0x0003);
        pad.write(0x0, 0x01);
        check((pad.read(4) & 0x80) != 0, "ACK is asserted after a byte");
        pad.step(100);
        check((pad.read(4) & 0x80) != 0, "ACK is still there after 100 cycles");
        pad.step(200);
        check((pad.read(4) & 0x80) == 0, "ACK clears itself after the pulse");
    }

    private static void rxFifoClears() {
        Pad pad = new Pad();
        pad.reset();
        pad.write(0xA, 0x0003);
        pad.write(0x0, 0x42);
        check((pad.read(4) & 0x02) != 0, "RX not empty after a byte");
        pad.read(0);
        check((pad.read(4) & 0x02) == 0, "reading the data register empties RX");
    }

    private static void resetPort() {
        Pad pad = new Pad();
        pad.reset();
        pad.write(0xA, 0x0003);
        pad.write(0x0, 0x01);
        pad.write(0x0, 0x42);
        pad.write(0xA, 0x0010);                 /* reset */
        int[] r = exchange(pad);
        same(0x41, r[1], "port answers correctly after a reset");
    }

    private static void noDevice() {
        Pad pad = new Pad();
        pad.reset();
        pad.write(0xA, 0x0003);
        pad.write(0x0, 0x01);
        pad.write(0x0, 0x99);                   /* wrong second byte */
        same(0xFF, pad.read(0), "unknown device answers FF");
    }

    /** Every button of the wire, one at a time, must reach the right bit. */
    private static void allButtons() {
        int[] bits = { SELECT, START, UP, RIGHT, DOWN, LEFT,
                       L1, R1, TRIANGLE, CIRCLE, CROSS, SQUARE };
        boolean ok = true;
        for (int i = 0; i < bits.length; i++) {
            Pad pad = new Pad();
            pad.reset();
            pad.setButtons(press(bits[i]));
            int[] r = exchange(pad);
            int word = (r[4] << 8) | r[3];
            int expected = 0xFFFF & ~(1 << bits[i]);
            if (word != expected) {
                ok = false;
                System.out.println("  bit " + bits[i] + ": expected "
                        + Integer.toHexString(expected) + " got " + Integer.toHexString(word));
            }
        }
        check(ok, "all twelve buttons report their own bit");
    }

    /* ------------------------------------------------------------------ */

    /** Sends the standard 01 42 00 00 00 frame and returns the answers. */
    private static int[] exchange(Pad pad) {
        int[] out = new int[5];
        pad.write(0x0, 0x01);
        out[0] = pad.read(0x0);
        pad.write(0x0, 0x42);
        out[1] = pad.read(0x0);
        for (int i = 2; i < 5; i++) {
            pad.write(0x0, 0x00);
            out[i] = pad.read(0x0);
        }
        return out;
    }

    private static int press(int bit) {
        return 0xFFFF & ~(1 << bit);
    }

    private static void same(int expected, int actual, String what) {
        check(expected == actual, what + " (expected " + Integer.toHexString(expected)
                + ", got " + Integer.toHexString(actual) + ")");
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
