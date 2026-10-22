package physicsplayground.render;

/** Reusable modulate filter for debris fading and valid/invalid placement ghosts. */
public final class TintDraw implements Draw {
    private final Draw target;
    private float red = 1, green = 1, blue = 1, alpha = 1;
    public TintDraw(Draw target) { this.target = target; }
    public void setTint(float red, float green, float blue, float alpha) { this.red = red; this.green = green; this.blue = blue; this.alpha = alpha; }
    public void setTransform(float x, float y, float c, float s) { target.setTransform(x, y, c, s); }
    public void fillRect(float x, float y, float w, float h, int color) { target.fillRect(x, y, w, h, tint(color)); }
    public void drawRect(float x, float y, float w, float h, float width, int color) { target.drawRect(x, y, w, h, width, tint(color)); }
    public void fillCircle(float x, float y, float r, int color) { target.fillCircle(x, y, r, tint(color)); }
    public void drawArc(float x, float y, float r, float start, float end, int segments, float width, int color) { target.drawArc(x, y, r, start, end, segments, width, tint(color)); }
    public void drawLine(float x1, float y1, float x2, float y2, float width, int color) { target.drawLine(x1, y1, x2, y2, width, tint(color)); }
    public void fillPolygon(float[] points, int vertices, int color) { target.fillPolygon(points, vertices, tint(color)); }
    public void drawPolyline(float[] points, int vertices, boolean closed, float width, int color) { target.drawPolyline(points, vertices, closed, width, tint(color)); }
    private int tint(int color) {
        int a = Math.min(255, (int) ((color >>> 24) * alpha + .5f));
        int r = Math.min(255, (int) ((color >>> 16 & 255) * red + .5f)), g = Math.min(255, (int) ((color >>> 8 & 255) * green + .5f)), b = Math.min(255, (int) ((color & 255) * blue + .5f));
        return a << 24 | r << 16 | g << 8 | b;
    }
}
