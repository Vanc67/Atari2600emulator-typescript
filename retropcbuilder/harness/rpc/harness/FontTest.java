package rpc.harness;

import rpc.BitmapFont;

/** Renders the whole bitmap font so missing/broken glyphs are visible. */
public final class FontTest {

    public static void main(String[] args) {
        String out = args.length > 0 ? args[0] : "docs/screens/font.bmp";
        int cols = 32;
        int cw = 6 * 2 + 2;
        int rows = (BitmapFont.CHARS.length() + cols - 1) / cols;
        int w = cols * cw + 4;
        int h = rows * (9 * 2 + 8) + 8;
        RasterGfx gfx = new RasterGfx(w, h);
        gfx.setColor(0x101418);
        gfx.fillRect(0, 0, w, h);
        for (int i = 0; i < BitmapFont.CHARS.length(); i++) {
            int cx = 2 + (i % cols) * cw;
            int cy = 4 + (i / cols) * (9 * 2 + 8);
            gfx.setColor(0x556070);
            gfx.drawText(Integer.toHexString(i), cx, cy, 1);
            gfx.setColor(0xFFD890);
            gfx.drawText(String.valueOf(BitmapFont.CHARS.charAt(i)), cx, cy + 10, 2);
        }
        Bmp.write(out, gfx.pixels, w, h);
        System.out.println("[font] " + BitmapFont.CHARS.length() + " glyphs -> " + out);
    }
}
