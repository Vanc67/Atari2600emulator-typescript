package physicsplayground.core;

/** Immutable RGBA colour, independent of MIDP so content remains testable on Java SE. */
public final class ColorValue {
    public static final ColorValue GRAY = new ColorValue(140, 140, 140, 255);
    public final int r;
    public final int g;
    public final int b;
    public final int a;

    public ColorValue(int r, int g, int b) { this(r, g, b, 255); }
    public ColorValue(int r, int g, int b, int a) {
        this.r = clamp(r); this.g = clamp(g); this.b = clamp(b); this.a = clamp(a);
    }

    public int rgb() { return (r << 16) | (g << 8) | b; }
    public int argb() { return (a << 24) | rgb(); }
    public ColorValue alpha(int alpha) { return new ColorValue(r, g, b, alpha); }
    public ColorValue darkened(float amount) {
        float scale = 1.0f - amount;
        return new ColorValue((int) (r * scale), (int) (g * scale), (int) (b * scale), a);
    }
    public ColorValue lightened(float amount) {
        return new ColorValue((int) (r + (255 - r) * amount), (int) (g + (255 - g) * amount), (int) (b + (255 - b) * amount), a);
    }

    public static ColorValue html(String text) {
        if (text == null) return null;
        String value = text.charAt(0) == '#' ? text.substring(1) : text;
        if (!(value.length() == 3 || value.length() == 4 || value.length() == 6 || value.length() == 8)) return null;
        for (int i = 0; i < value.length(); i++) if (Character.digit(value.charAt(i), 16) < 0) return null;
        if (value.length() <= 4) {
            int r = Character.digit(value.charAt(0), 16) * 17;
            int g = Character.digit(value.charAt(1), 16) * 17;
            int b = Character.digit(value.charAt(2), 16) * 17;
            int a = value.length() == 4 ? Character.digit(value.charAt(3), 16) * 17 : 255;
            return new ColorValue(r, g, b, a);
        }
        int r = Integer.parseInt(value.substring(0, 2), 16);
        int g = Integer.parseInt(value.substring(2, 4), 16);
        int b = Integer.parseInt(value.substring(4, 6), 16);
        int a = value.length() == 8 ? Integer.parseInt(value.substring(6, 8), 16) : 255;
        return new ColorValue(r, g, b, a);
    }

    private static int clamp(int value) { return value < 0 ? 0 : (value > 255 ? 255 : value); }
}
