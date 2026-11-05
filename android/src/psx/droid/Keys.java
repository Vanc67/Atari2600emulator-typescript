package psx.droid;

/**
 * PlayStation pad bit layout, straight from the SIO0 shift order: the low
 * byte is select/start/d-pad, the high byte is the shoulders and faces.
 *
 * <p>The pad is active low, so a pressed button clears its bit; the emulator
 * sends {@code 0xFFFF &amp; ~pressed} to {@link psx.PS1Machine#setButtons}.</p>
 */
public final class Keys {

    public static final int SELECT = 0x0001;
    public static final int L3 = 0x0002;
    public static final int R3 = 0x0004;
    public static final int START = 0x0008;
    public static final int UP = 0x0010;
    public static final int RIGHT = 0x0020;
    public static final int DOWN = 0x0040;
    public static final int LEFT = 0x0080;

    public static final int L2 = 0x0100;
    public static final int R2 = 0x0200;
    public static final int L1 = 0x0400;
    public static final int R1 = 0x0800;
    public static final int TRIANGLE = 0x1000;
    public static final int CIRCLE = 0x2000;
    public static final int CROSS = 0x4000;
    public static final int SQUARE = 0x8000;

    private Keys() { }
}
