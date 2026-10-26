package rpc;

/**
 * Central palette.  Every screen pulls its colours from here, which is what
 * keeps the UI looking like one product instead of 53 different prototypes.
 */
public final class Theme {

    private Theme() {
    }

    /* background / panels */
    public static final int BG        = 0x0B0E14;
    public static final int BG2       = 0x141A26;
    public static final int PANEL     = 0x1B2333;
    public static final int PANEL2    = 0x232D42;
    public static final int PANEL3    = 0x2C3852;
    public static final int LINE      = 0x3A4763;
    public static final int LINE_HI   = 0x55658A;

    /* text */
    public static final int TEXT      = 0xE6EDF8;
    public static final int DIM       = 0x8A9AB5;
    public static final int MUTED     = 0x5D6B85;

    /* accents */
    public static final int ACCENT    = 0xFFC24B; /* amber CRT         */
    public static final int ACCENT2   = 0x59D3FF; /* cyan highlight    */
    public static final int GREEN     = 0x63E6A0;
    public static final int RED       = 0xFF6B6B;
    public static final int VIOLET    = 0xB58CFF;
    public static final int ORANGE    = 0xFF9A3D;
    public static final int BLUE      = 0x4C8DFF;

    /* scene specific */
    public static final int WALL_DAY  = 0x2A3550;
    public static final int WALL_NIGHT= 0x141B2B;
    public static final int DESK      = 0x4A3B2E;
    public static final int DESK_HI   = 0x6B5643;
    public static final int METAL     = 0x8C97AB;
    public static final int METAL_DK  = 0x5A6478;
    public static final int SCREEN    = 0x0E1620;
    public static final int SCREEN_ON = 0x17263A;
    public static final int STEEL     = 0x39415A;
}
