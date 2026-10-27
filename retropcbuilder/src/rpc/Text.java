package rpc;

/**
 * Text metrics for the built in bitmap font.
 *
 * The game passes a *style index* (0 = small, 1 = normal, 2 = large) while
 * {@link BitmapFont} works with a pixel scale.  Every backend must map the
 * same way, otherwise text drawn with style 0 would be invisible - that was
 * one of the "empty screen" bugs of the 4.8 build.
 */
public final class Text {

    private Text() {
    }

    /** style index -> pixel scale (1 or 2) */
    public static int scale(int style) {
        return style >= 2 ? 2 : 1;
    }

    public static int width(String s, int style) {
        String t = sanitize(s);
        return BitmapFont.textWidth(t, scale(style));
    }

    /**
     * Removes characters the bitmap font has no glyph for (emoji and other
     * symbols that crept in from the 4.8 UI strings).  Without this they were
     * rendered as empty boxes.
     */
    public static String sanitize(String s) {
        if (s == null) {
            return "";
        }
        boolean clean = true;
        for (int i = 0; i < s.length(); i++) {
            if (BitmapFont.index(s.charAt(i)) < 0) {
                clean = false;
                break;
            }
        }
        if (clean) {
            return s;
        }
        StringBuffer out = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (BitmapFont.index(c) >= 0) {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static int height(int style) {
        return BitmapFont.textHeight(scale(style));
    }

    /** Convenience overload with style 0 used on dark instrument panels. */
    public static void small(Gfx g, String s, int x, int y, int color) {
        draw(g, s, x, y, 0, color);
    }

    public static void draw(Gfx g, String s, int x, int y, int style, int color) {
        if (s == null) {
            return;
        }
        g.setColor(color);
        BitmapFont.draw(g, sanitize(s), x, y, scale(style), color);
    }

    public static void drawAnchored(Gfx g, String s, int x, int y, int style,
                                    int anchor, int color) {
        if (s == null) {
            return;
        }
        int tx = x;
        int ty = y;
        if ((anchor & Gfx.HCENTER) != 0) {
            tx -= width(s, style) / 2;
        } else if ((anchor & Gfx.RIGHT) != 0) {
            tx -= width(s, style);
        }
        if ((anchor & Gfx.VCENTER) != 0) {
            ty -= height(style) / 2;
        } else if ((anchor & Gfx.BOTTOM) != 0) {
            ty -= height(style);
        }
        draw(g, s, tx, ty, style, color);
    }

    /** Shortens {@code s} with an ellipsis until it fits into {@code maxPx}. */
    public static String fit(String s, int style, int maxPx) {
        if (s == null) {
            return "";
        }
        if (width(s, style) <= maxPx) {
            return s;
        }
        int chars = maxPx / (6 * scale(style)) - 1;
        if (chars <= 0) {
            return "";
        }
        return s.substring(0, Math.min(chars, s.length())) + "\u2026";
    }
}
