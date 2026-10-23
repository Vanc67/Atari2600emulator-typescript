package javax.microedition.lcdui;
public class Font {
    public static final int FACE_SYSTEM = 0; public static final int FACE_MONOSPACE = 32; public static final int FACE_PROPORTIONAL = 64;
    public static final int STYLE_PLAIN = 0; public static final int STYLE_BOLD = 1; public static final int STYLE_ITALIC = 2; public static final int STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8; public static final int SIZE_MEDIUM = 0; public static final int SIZE_LARGE = 16;
    public static Font getFont(int face, int style, int size) { return null; }
    public static Font getDefaultFont() { return null; }
    public int getHeight() { return 0; }
    public int getBaselinePosition() { return 0; }
    public int charWidth(char c) { return 0; }
    public int stringWidth(String s) { return 0; }
    public int substringWidth(String s, int o, int l) { return 0; }
}
