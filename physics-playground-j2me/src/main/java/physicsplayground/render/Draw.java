package physicsplayground.render;

/** Float-coordinate drawing bridge shared by procedural art and the tiny software rasterizer. */
public interface Draw {
    void setTransform(float translateX, float translateY, float cosine, float sine);
    void fillRect(float x, float y, float width, float height, int argb);
    void drawRect(float x, float y, float width, float height, float lineWidth, int argb);
    void fillCircle(float x, float y, float radius, int argb);
    void drawArc(float x, float y, float radius, float startAngle, float endAngle, int segments, float lineWidth, int argb);
    void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int argb);
    void fillPolygon(float[] points, int vertices, int argb);
    void drawPolyline(float[] points, int vertices, boolean closed, float lineWidth, int argb);
}
