package rdl.platform;

/** Tiny allocation-free 3x5 font for deterministic 128x160 rendering. */
public final class PixelFont {
    private static final String UNKNOWN = "111001010000010";

    /* ASCII 32..90. Entries not needed by the game are null. */
    private static final String[] GLYPHS = {
        "               ", /* 32: space */
        "010010010000010", /* 33: ! */
        null,             /* 34: quote */
        "101111101111101", /* 35: # */
        null, null, null, null, null, null, /* 36..41 */
        "101010111010101", /* 42: * */
        "000010111010000", /* 43: + */
        null,             /* 44: comma */
        "000000111000000", /* 45: - */
        "000000000000010", /* 46: . */
        "001001010100100", /* 47: / */
        "111101101101111", /* 48: 0 */
        "010110010010111", /* 49: 1 */
        "111001111100111", /* 50: 2 */
        "111001111001111", /* 51: 3 */
        "101101111001001", /* 52: 4 */
        "111100111001111", /* 53: 5 */
        "111100111101111", /* 54: 6 */
        "111001010010010", /* 55: 7 */
        "111101111101111", /* 56: 8 */
        "111101111001111", /* 57: 9 */
        "000010000010000", /* 58: : */
        null,             /* 59: semicolon */
        "001010100010001", /* 60: < */
        "000111000111000", /* 61: = */
        "100010001010100", /* 62: > */
        "111001010000010", /* 63: ? */
        null,             /* 64: @ */
        "010101111101101", /* A */
        "110101110101110", /* B */
        "111100100100111", /* C */
        "110101101101110", /* D */
        "111100111100111", /* E */
        "111100111100100", /* F */
        "111100101101111", /* G */
        "101101111101101", /* H */
        "111010010010111", /* I */
        "001001001101111", /* J */
        "101101110101101", /* K */
        "100100100100111", /* L */
        "101111111101101", /* M */
        "101111111111101", /* N */
        "111101101101111", /* O */
        "111101111100100", /* P */
        "111101101111001", /* Q */
        "110101110101101", /* R */
        "111100111001111", /* S */
        "111010010010010", /* T */
        "101101101101111", /* U */
        "101101101101010", /* V */
        "101101111111101", /* W */
        "101101010101101", /* X */
        "101101010010010", /* Y */
        "111001010100111"  /* Z */
    };

    private PixelFont() {
    }

    public static int width(String text, int scale) {
        if (text.length() == 0) {
            return 0;
        }
        return (text.length() * 4 - 1) * scale;
    }

    public static void drawCenter(int[] target, int targetWidth, int targetHeight,
            int y, int scale, String text, int color) {
        int x = (targetWidth - width(text, scale)) / 2;
        draw(target, targetWidth, targetHeight, x, y, scale, text, color);
    }

    public static void draw(int[] target, int targetWidth, int targetHeight,
            int x, int y, int scale, String text, int color) {
        int i;
        for (i = 0; i < text.length(); i++) {
            drawChar(target, targetWidth, targetHeight, x, y, scale, text.charAt(i), color);
            x += 4 * scale;
        }
    }

    public static void drawNumber(int[] target, int targetWidth, int targetHeight,
            int x, int y, int scale, int value, int color) {
        if (value < 0) {
            drawChar(target, targetWidth, targetHeight, x, y, scale, '-', color);
            value = -value;
            x += 4 * scale;
        }
        if (value == 0) {
            drawChar(target, targetWidth, targetHeight, x, y, scale, '0', color);
            return;
        }
        int divisor = 1000000000;
        while (divisor > 1 && value < divisor) {
            divisor /= 10;
        }
        while (divisor > 0) {
            int digit = value / divisor;
            drawChar(target, targetWidth, targetHeight, x, y, scale,
                    (char) ('0' + digit), color);
            value -= digit * divisor;
            divisor /= 10;
            x += 4 * scale;
        }
    }

    private static void drawChar(int[] target, int targetWidth, int targetHeight,
            int x, int y, int scale, char character, int color) {
        if (character >= 'a' && character <= 'z') {
            character = (char) (character - 'a' + 'A');
        }
        String glyph = UNKNOWN;
        if (character >= 32 && character <= 90) {
            String candidate = GLYPHS[character - 32];
            if (candidate != null) {
                glyph = candidate;
            }
        }
        int row;
        int col;
        for (row = 0; row < 5; row++) {
            for (col = 0; col < 3; col++) {
                if (glyph.charAt(row * 3 + col) == '1') {
                    fill(target, targetWidth, targetHeight,
                            x + col * scale, y + row * scale, scale, scale, color);
                }
            }
        }
    }

    private static void fill(int[] target, int targetWidth, int targetHeight,
            int x, int y, int width, int height, int color) {
        if (x >= targetWidth || y >= targetHeight || x + width <= 0 || y + height <= 0) {
            return;
        }
        int x0 = x < 0 ? 0 : x;
        int y0 = y < 0 ? 0 : y;
        int x1 = x + width > targetWidth ? targetWidth : x + width;
        int y1 = y + height > targetHeight ? targetHeight : y + height;
        int py;
        int px;
        for (py = y0; py < y1; py++) {
            int offset = py * targetWidth;
            for (px = x0; px < x1; px++) {
                target[offset + px] = color;
            }
        }
    }
}
