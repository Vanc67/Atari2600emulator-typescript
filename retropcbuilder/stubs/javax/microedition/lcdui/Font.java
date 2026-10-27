package javax.microedition.lcdui;
/**
 * Compile-time stub of the MIDP Font class. The desktop test harness needs
 * usable metrics, so getFont() returns a simple monospace-like font.
 */
public class Font {
    public static final int FACE_SYSTEM = 0; public static final int FACE_MONOSPACE = 32; public static final int FACE_PROPORTIONAL = 64;
    public static final int STYLE_PLAIN = 0; public static final int STYLE_BOLD = 1; public static final int STYLE_ITALIC = 2; public static final int STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8; public static final int SIZE_MEDIUM = 0; public static final int SIZE_LARGE = 16;

    private static final Font SMALL = new Font(9);
    private static final Font MEDIUM = new Font(12);
    private static final Font LARGE = new Font(16);

    private final int height;

    private Font(int height) { this.height = height; }

    public static Font getFont(int face, int style, int size) {
        if (size == SIZE_SMALL) return SMALL;
        if (size == SIZE_LARGE) return LARGE;
        return MEDIUM;
    }

    public static Font getDefaultFont() { return MEDIUM; }
    public int getHeight() { return height; }
    public int getBaselinePosition() { return height - 2; }
    public int charWidth(char c) { return 6; }
    public int stringWidth(String s) { return s == null ? 0 : s.length() * 6; }
    public int substringWidth(String s, int o, int l) { return l * 6; }
}
