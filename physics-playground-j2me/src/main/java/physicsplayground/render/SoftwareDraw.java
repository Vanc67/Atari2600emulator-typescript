package physicsplayground.render;

/** MIDP-friendly ARGB rasterizer: one 128x160 int buffer, no Java2D and no per-primitive objects. */
public final class SoftwareDraw implements Draw {
    public final int width, height;
    public final int[] pixels;
    private float tx, ty, cosine = 1, sine, scale = 1;
    private final float[] transformed = new float[128];
    private final float[] intersections = new float[64];
    private final float[] quad = new float[8];

    public SoftwareDraw(int width, int height) { this.width = width; this.height = height; pixels = new int[width * height]; clear(0xff000000); }
    public void clear(int argb) { for (int i = 0; i < pixels.length; i++) pixels[i] = argb | 0xff000000; }
    public void setTransform(float translateX, float translateY, float cosine, float sine) {
        tx = translateX; ty = translateY; this.cosine = cosine; this.sine = sine; scale = (float) Math.sqrt(cosine * cosine + sine * sine);
    }
    public void setIdentity() { setTransform(0, 0, 1, 0); }
    public void fillRect(float x, float y, float width, float height, int argb) {
        quad[0] = x; quad[1] = y; quad[2] = x + width; quad[3] = y; quad[4] = x + width; quad[5] = y + height; quad[6] = x; quad[7] = y + height;
        fillPolygon(quad, 4, argb);
    }
    public void drawRect(float x, float y, float width, float height, float lineWidth, int argb) {
        quad[0] = x; quad[1] = y; quad[2] = x + width; quad[3] = y; quad[4] = x + width; quad[5] = y + height; quad[6] = x; quad[7] = y + height;
        drawPolyline(quad, 4, true, lineWidth, argb);
    }
    public void fillCircle(float x, float y, float radius, int argb) {
        float cx = tx + x * cosine - y * sine, cy = ty + x * sine + y * cosine, r = Math.max(radius * scale, .35f);
        int minX = clamp((int) Math.floor(cx - r), 0, width - 1), maxX = clamp((int) Math.floor(cx + r), 0, width - 1);
        int minY = clamp((int) Math.floor(cy - r), 0, height - 1), maxY = clamp((int) Math.floor(cy + r), 0, height - 1);
        float rr = r * r;
        for (int py = minY; py <= maxY; py++) for (int px = minX; px <= maxX; px++) {
            float dx = px + .5f - cx, dy = py + .5f - cy; if (dx * dx + dy * dy <= rr) blend(px, py, argb);
        }
    }
    public void drawArc(float x, float y, float radius, float startAngle, float endAngle, int segments, float lineWidth, int argb) {
        float screenRadius = radius * scale;
        if (screenRadius > 3) segments = Math.min(segments, Math.max(8, (int) (screenRadius * 1.5f)));
        segments = Math.max(segments, 3);
        float previousX = x + (float) Math.cos(startAngle) * radius, previousY = y + (float) Math.sin(startAngle) * radius;
        for (int i = 1; i <= segments; i++) {
            float angle = startAngle + (endAngle - startAngle) * i / segments;
            float nextX = x + (float) Math.cos(angle) * radius, nextY = y + (float) Math.sin(angle) * radius;
            drawLine(previousX, previousY, nextX, nextY, lineWidth, argb); previousX = nextX; previousY = nextY;
        }
    }
    public void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int argb) {
        float ax = tx + x1 * cosine - y1 * sine, ay = ty + x1 * sine + y1 * cosine;
        float bx = tx + x2 * cosine - y2 * sine, by = ty + x2 * sine + y2 * cosine;
        float radius = Math.max(lineWidth * scale * .5f, .35f), dx = bx - ax, dy = by - ay, length = dx * dx + dy * dy;
        int minX = clamp((int) Math.floor(Math.min(ax, bx) - radius), 0, width - 1), maxX = clamp((int) Math.ceil(Math.max(ax, bx) + radius), 0, width - 1);
        int minY = clamp((int) Math.floor(Math.min(ay, by) - radius), 0, height - 1), maxY = clamp((int) Math.ceil(Math.max(ay, by) + radius), 0, height - 1);
        float rr = radius * radius;
        for (int py = minY; py <= maxY; py++) for (int px = minX; px <= maxX; px++) {
            float ox = px + .5f - ax, oy = py + .5f - ay;
            float t = length > 0 ? (ox * dx + oy * dy) / length : 0; t = t < 0 ? 0 : (t > 1 ? 1 : t);
            float ex = ox - dx * t, ey = oy - dy * t; if (ex * ex + ey * ey <= rr) blend(px, py, argb);
        }
    }
    public void fillPolygon(float[] points, int vertices, int argb) {
        if (vertices < 3) return; vertices = Math.min(vertices, 64);
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertices; i++) {
            float x = points[i * 2], y = points[i * 2 + 1];
            transformed[i * 2] = tx + x * cosine - y * sine; transformed[i * 2 + 1] = ty + x * sine + y * cosine;
            minY = Math.min(minY, transformed[i * 2 + 1]); maxY = Math.max(maxY, transformed[i * 2 + 1]);
        }
        int startY = Math.max(0, (int) Math.floor(minY)), endY = Math.min(height - 1, (int) Math.ceil(maxY));
        for (int y = startY; y <= endY; y++) {
            float sampleY = y + .5f; int count = 0;
            for (int i = 0; i < vertices; i++) {
                int next = i + 1 == vertices ? 0 : i + 1;
                float ay = transformed[i * 2 + 1], by = transformed[next * 2 + 1];
                if ((ay <= sampleY && by > sampleY) || (by <= sampleY && ay > sampleY)) {
                    float ax = transformed[i * 2], bx = transformed[next * 2];
                    intersections[count++] = ax + (sampleY - ay) * (bx - ax) / (by - ay);
                }
            }
            for (int i = 1; i < count; i++) { float value = intersections[i]; int at = i; while (at > 0 && intersections[at - 1] > value) { intersections[at] = intersections[at - 1]; at--; } intersections[at] = value; }
            for (int i = 0; i + 1 < count; i += 2) fillSpan(y, intersections[i], intersections[i + 1], argb);
        }
    }
    public void drawPolyline(float[] points, int vertices, boolean closed, float lineWidth, int argb) {
        for (int i = 0; i + 1 < vertices; i++) drawLine(points[i * 2], points[i * 2 + 1], points[i * 2 + 2], points[i * 2 + 3], lineWidth, argb);
        if (closed && vertices > 1) drawLine(points[(vertices - 1) * 2], points[(vertices - 1) * 2 + 1], points[0], points[1], lineWidth, argb);
    }
    private void fillSpan(int y, float left, float right, int argb) {
        int start = Math.max(0, (int) Math.ceil(Math.min(left, right) - .5f)), end = Math.min(width - 1, (int) Math.floor(Math.max(left, right) - .5f));
        for (int x = start; x <= end; x++) blend(x, y, argb);
    }
    private void blend(int x, int y, int argb) {
        int alpha = argb >>> 24; if (alpha == 0) return; int index = y * width + x; if (alpha == 255) { pixels[index] = argb; return; }
        int dst = pixels[index], inverse = 255 - alpha;
        int r = ((argb >>> 16 & 255) * alpha + (dst >>> 16 & 255) * inverse + 127) / 255;
        int g = ((argb >>> 8 & 255) * alpha + (dst >>> 8 & 255) * inverse + 127) / 255;
        int b = ((argb & 255) * alpha + (dst & 255) * inverse + 127) / 255;
        pixels[index] = 0xff000000 | r << 16 | g << 8 | b;
    }
    private static int clamp(int value, int min, int max) { return value < min ? min : (value > max ? max : value); }
}
