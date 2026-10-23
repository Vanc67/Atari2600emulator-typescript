package javax.microedition.lcdui;
public class Graphics {
    public static final int HCENTER = 1; public static final int VCENTER = 2;
    public static final int LEFT = 4; public static final int RIGHT = 8;
    public static final int TOP = 16; public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0; public static final int DOTTED = 1;
    public void setColor(int rgb) {}
    public int getColor() { return 0; }
    public void setColor(int r, int g, int b) {}
    public int getRedComponent() { return 0; }
    public int getGreenComponent() { return 0; }
    public int getBlueComponent() { return 0; }
    public void setGrayScale(int v) {}
    public int getGrayScale() { return 0; }
    public void setFont(Font f) {}
    public Font getFont() { return null; }
    public void setStrokeStyle(int s) {}
    public int getStrokeStyle() { return 0; }
    public void translate(int x, int y) {}
    public int getTranslateX() { return 0; }
    public int getTranslateY() { return 0; }
    public int getClipX() { return 0; }
    public int getClipY() { return 0; }
    public int getClipWidth() { return 0; }
    public int getClipHeight() { return 0; }
    public void setClip(int x, int y, int w, int h) {}
    public void clipRect(int x, int y, int w, int h) {}
    public void fillRect(int x, int y, int w, int h) {}
    public void drawRect(int x, int y, int w, int h) {}
    public void drawRoundRect(int x, int y, int w, int h, int aw, int ah) {}
    public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) {}
    public void fillArc(int x, int y, int w, int h, int a, int ar) {}
    public void drawArc(int x, int y, int w, int h, int a, int ar) {}
    public void drawLine(int x1, int y1, int x2, int y2) {}
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {}
    public void drawString(String s, int x, int y, int a) {}
    public void drawSubstring(String s, int o, int l, int x, int y, int a) {}
    public void drawChar(char c, int x, int y, int a) {}
    public void drawChars(char[] c, int o, int l, int x, int y, int a) {}
    public void drawImage(Image i, int x, int y, int a) {}
    public void drawRGB(int[] rgb, int off, int scan, int x, int y, int w, int h, boolean alpha) {}
    public void copyArea(int x, int y, int w, int h, int dx, int dy, int a) {}
    public int getDisplayColor(int c) { return 0; }
}
