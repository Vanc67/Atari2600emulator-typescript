package rpc.harness;

import rpc.Text;
import rpc.Gfx;
import rpc.Images$Img;

/**
 * Software implementation of {@link Gfx} used by the desktop test harness.
 *
 * Everything is rasterised into an int[] framebuffer so the harness can dump
 * exact frames of the real game (same drawing code the handset runs) and the
 * screenshots can be checked without a phone emulator.
 */
public final class RasterGfx implements Gfx {

    private final int w;
    private final int h;
    public final int[] pixels;
    private int color = 0xFFFFFF;
    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;

    public RasterGfx(int w, int h) {
        this.w = w;
        this.h = h;
        this.pixels = new int[w * h];
        this.clipW = w;
        this.clipH = h;
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
    }

    public int getColor() {
        return color;
    }

    private void px(int x, int y) {
        if (x < clipX || y < clipY || x >= clipX + clipW || y >= clipY + clipH) {
            return;
        }
        if (x < 0 || y < 0 || x >= w || y >= h) {
            return;
        }
        pixels[y * w + x] = color;
    }

    public void fillRect(int x, int y, int iw, int ih) {
        int x0 = Math.max(x, clipX);
        int y0 = Math.max(y, clipY);
        int x1 = Math.min(x + iw, clipX + clipW);
        int y1 = Math.min(y + ih, clipY + clipH);
        if (x1 > w) {
            x1 = w;
        }
        if (y1 > h) {
            y1 = h;
        }
        for (int yy = y0; yy < y1; yy++) {
            int row = yy * w;
            for (int xx = x0; xx < x1; xx++) {
                pixels[row + xx] = color;
            }
        }
    }

    public void drawRect(int x, int y, int iw, int ih) {
        if (iw <= 0 || ih <= 0) {
            return;
        }
        fillRect(x, y, iw, 1);
        fillRect(x, y + ih - 1, iw, 1);
        fillRect(x, y, 1, ih);
        fillRect(x + iw - 1, y, 1, ih);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) {
            px(x1, y1);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            px(x1 + dx * i / steps, y1 + dy * i / steps);
        }
    }

    public void drawRoundRect(int x, int y, int iw, int ih, int r) {
        if (iw < 2 * r || ih < 2 * r) {
            drawRect(x, y, iw, ih);
            return;
        }
        fillRect(x + r, y, iw - 2 * r, 1);
        fillRect(x + r, y + ih - 1, iw - 2 * r, 1);
        fillRect(x, y + r, 1, ih - 2 * r);
        fillRect(x + iw - 1, y + r, 1, ih - 2 * r);
        for (int i = 0; i < r; i++) {
            int o = r - i;
            int dy = r - (int) Math.sqrt((double) (r * r - o * o));
            fillRect(x + i, y + dy, 1, 1);
            fillRect(x + iw - 1 - i, y + dy, 1, 1);
            fillRect(x + i, y + ih - 1 - dy, 1, 1);
            fillRect(x + iw - 1 - i, y + ih - 1 - dy, 1, 1);
        }
    }

    public void fillRoundRect(int x, int y, int iw, int ih, int r) {
        if (iw < 2 * r || ih < 2 * r) {
            fillRect(x, y, iw, ih);
            return;
        }
        fillRect(x + r, y, iw - 2 * r, ih);
        fillRect(x, y + r, iw, ih - 2 * r);
        for (int i = 0; i < r; i++) {
            int o = r - i;
            int dy = r - (int) Math.sqrt((double) (r * r - o * o));
            fillRect(x + i, y + dy, 1, ih - 2 * dy);
            fillRect(x + iw - 1 - i, y + dy, 1, ih - 2 * dy);
        }
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        int minX = Math.min(x1, Math.min(x2, x3));
        int maxX = Math.max(x1, Math.max(x2, x3));
        int minY = Math.min(y1, Math.min(y2, y3));
        int maxY = Math.max(y1, Math.max(y2, y3));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                int d1 = (x - x2) * (y1 - y2) - (x1 - x2) * (y - y2);
                int d2 = (x - x3) * (y2 - y3) - (x2 - x3) * (y - y3);
                int d3 = (x - x1) * (y3 - y1) - (x3 - x1) * (y - y1);
                boolean neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
                boolean pos = (d1 > 0) || (d2 > 0) || (d3 > 0);
                if (!(neg && pos)) {
                    px(x, y);
                }
            }
        }
    }

    public void setClip(int x, int y, int cw, int ch) {
        clipX = x;
        clipY = y;
        clipW = cw;
        clipH = ch;
    }

    public void drawText(String s, int x, int y, int size) {
        Text.draw(this, s, x, y, size, color);
    }

    public void drawText(String s, int x, int y, int size, int anchor) {
        Text.drawAnchored(this, s, x, y, size, anchor, color);
    }

    public int textWidth(String s, int size) {
        return Text.width(s, size);
    }

    public int textHeight(int size) {
        return Text.height(size);
    }

    public Object newImage(int[] argb, int iw, int ih) {
        Images$Img img = new Images$Img(iw, ih);
        System.arraycopy(argb, 0, img.argb, 0, Math.min(argb.length, img.argb.length));
        return img;
    }

    public void blit(Object image, int x, int y) {
        if (!(image instanceof Images$Img)) {
            return;
        }
        Images$Img img = (Images$Img) image;
        for (int yy = 0; yy < img.h; yy++) {
            for (int xx = 0; xx < img.w; xx++) {
                int argb = img.argb[yy * img.w + xx];
                if ((argb >>> 24) == 0) {
                    continue;
                }
                int old = color;
                color = argb & 0xFFFFFF;
                px(x + xx, y + yy);
                color = old;
            }
        }
    }

    public void drawImage(Object image, int x, int y, int iw, int ih) {
        blit(image, x, y);
    }

    public Images$Img decodePng(byte[] data) {
        return Png.decode(data);
    }
}
