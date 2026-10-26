package rpc.j2me;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import rpc.Text;
import rpc.Gfx;
import rpc.Images$Img;

/**
 * {@link Gfx} implementation on top of LCDUI.
 *
 * All drawing goes through one Graphics object that the Canvas hands us for
 * the current frame, so there is no per-frame allocation here.
 */
final class GfxImpl implements Gfx {

    private Graphics g;
    private int w;
    private int h;
    private int color;

    void bind(Graphics graphics, int width, int height) {
        this.g = graphics;
        this.w = width;
        this.h = height;
        this.g.setColor(0);
    }

    public int width() {
        return w;
    }

    public int height() {
        return h;
    }

    public void setColor(int rgb) {
        color = rgb & 0xFFFFFF;
        g.setColor(color);
    }

    public void fillRect(int x, int y, int w, int h) {
        if (w > 0 && h > 0) {
            g.fillRect(x, y, w, h);
        }
    }

    public void drawRect(int x, int y, int w, int h) {
        g.drawRect(x, y, w - 1, h - 1);
    }

    public void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    public void drawRoundRect(int x, int y, int w, int h, int r) {
        g.drawRoundRect(x, y, w - 1, h - 1, r * 2, r * 2);
    }

    public void fillRoundRect(int x, int y, int w, int h, int r) {
        g.fillRoundRect(x, y, w, h, r * 2, r * 2);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        g.fillTriangle(x1, y1, x2, y2, x3, y3);
    }

    public void setClip(int x, int y, int w, int h) {
        if (w < 0) {
            w = 0;
        }
        if (h < 0) {
            h = 0;
        }
        g.setClip(x, y, w, h);
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

    public Object newImage(int[] argb, int w, int h) {
        try {
            return Image.createRGBImage(argb, w, h, true);
        } catch (Throwable t) {
            return null;
        }
    }

    public void blit(Object image, int x, int y) {
        if (image != null) {
            g.drawImage((Image) image, x, y, Gfx.LEFT | Gfx.TOP);
        }
    }

    public void drawImage(Object image, int x, int y, int w, int h) {
        blit(image, x, y);
    }

    public Images$Img decodePng(byte[] data) {
        try {
            Image img = Image.createImage(data, 0, data.length);
            int iw = img.getWidth();
            int ih = img.getHeight();
            Images$Img out = new Images$Img(iw, ih);
            img.getRGB(out.argb, 0, iw, 0, 0, iw, ih);
            return out;
        } catch (Throwable t) {
            return null;
        }
    }
}
