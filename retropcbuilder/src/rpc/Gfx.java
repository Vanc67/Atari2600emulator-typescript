package rpc;

/**
 * Platform independent 2D drawing surface.
 *
 * The game never touches javax.microedition.lcdui directly: the MIDlet
 * implements this interface on top of LCDUI Graphics, the desktop test
 * harness implements it on top of an int[] framebuffer, so any other port
 * (Android, web, ...) only has to implement these methods.
 *
 * Coordinates are pixels, colours are 0xRRGGBB.  Text is always drawn with
 * the built in 5x7 bitmap font ({@link BitmapFont}), which keeps the look
 * identical on every handset and - unlike the device font - is guaranteed to
 * have Cyrillic glyphs.
 */
public interface Gfx {

    /* ---- geometry -------------------------------------------------- */

    int width();

    int height();

    /* ---- state ----------------------------------------------------- */

    void setColor(int rgb);

    /* ---- primitives ------------------------------------------------ */

    void fillRect(int x, int y, int w, int h);

    void drawRect(int x, int y, int w, int h);

    void drawLine(int x1, int y1, int x2, int y2);

    void drawRoundRect(int x, int y, int w, int h, int r);

    void fillRoundRect(int x, int y, int w, int h, int r);

    void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3);

    /** Clipping rectangle in screen coordinates. */
    void setClip(int x, int y, int w, int h);

    /* ---- text ------------------------------------------------------ */

    /** Draws text at (x, y), top left corner, {@code size} 1..3. */
    void drawText(String s, int x, int y, int size);

    /**
     * Draws text with an anchor.
     *
     * @param anchor bit mask: {@link #LEFT}, {@link #RIGHT}, {@link #HCENTER},
     *               {@link #TOP}, {@link #BOTTOM}, {@link #VCENTER}
     */
    void drawText(String s, int x, int y, int size, int anchor);

    int textWidth(String s, int size);

    int textHeight(int size);

    /* ---- images ---------------------------------------------------- */

    /** Creates an off-screen image from ARGB pixels (may return null). */
    Object newImage(int[] argb, int w, int h);

    /** Draws an image produced by {@link #newImage} or {@link #decodePng}. */
    void blit(Object image, int x, int y);

    /** Legacy 4 coordinate variant used by the recovered 4.8 sources. */
    void drawImage(Object image, int x, int y, int w, int h);

    /** Decodes a PNG file; returns null when the platform lacks PNG support. */
    Images$Img decodePng(byte[] data);

    /* ---- anchor constants (mirror of the LCDUI ones) ---------------- */

    int LEFT = 4;
    int RIGHT = 8;
    int HCENTER = 1;
    int TOP = 16;
    int BOTTOM = 32;
    int VCENTER = 2;
}
