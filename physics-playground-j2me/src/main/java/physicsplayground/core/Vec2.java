package physicsplayground.core;

/** Mutable 2D vector. The mutating API avoids temporary objects in the physics loop. */
public final class Vec2 {
    public static final float PIXELS_PER_METER = 100.0f;
    public float x;
    public float y;

    public Vec2() {}
    public Vec2(float x, float y) { this.x = x; this.y = y; }
    public Vec2(Vec2 other) { this(other.x, other.y); }

    public Vec2 set(float x, float y) { this.x = x; this.y = y; return this; }
    public Vec2 set(Vec2 other) { return set(other.x, other.y); }
    public Vec2 copy() { return new Vec2(this); }
    public Vec2 zero() { return set(0, 0); }
    public Vec2 add(Vec2 other) { x += other.x; y += other.y; return this; }
    public Vec2 add(float x, float y) { this.x += x; this.y += y; return this; }
    public Vec2 sub(Vec2 other) { x -= other.x; y -= other.y; return this; }
    public Vec2 sub(float x, float y) { this.x -= x; this.y -= y; return this; }
    public Vec2 scale(float factor) { x *= factor; y *= factor; return this; }
    public Vec2 scaleAdd(Vec2 other, float factor) { x += other.x * factor; y += other.y * factor; return this; }
    public Vec2 negate() { x = -x; y = -y; return this; }
    public Vec2 orthogonal() { float oldX = x; x = -y; y = oldX; return this; }
    public float dot(Vec2 other) { return x * other.x + y * other.y; }
    public float cross(Vec2 other) { return x * other.y - y * other.x; }
    public static float cross(Vec2 a, Vec2 b) { return a.x * b.y - a.y * b.x; }
    public static float cross(float scalar, Vec2 vector) { return scalar * vector.x - scalar * vector.y; }
    public float lengthSquared() { return x * x + y * y; }
    public float length() { return (float) Math.sqrt(lengthSquared()); }
    public float distanceTo(Vec2 other) { float dx = x - other.x; float dy = y - other.y; return (float) Math.sqrt(dx * dx + dy * dy); }
    public float distanceSquaredTo(Vec2 other) { float dx = x - other.x; float dy = y - other.y; return dx * dx + dy * dy; }
    public float angle() { return (float) Math2.atan2(y, x); }

    public Vec2 normalize() {
        float length = length();
        if (length > 0.0000001f) scale(1.0f / length);
        return this;
    }

    public Vec2 limit(float maximum) {
        float squared = lengthSquared();
        if (squared > maximum * maximum && squared > 0) scale(maximum / (float) Math.sqrt(squared));
        return this;
    }

    public Vec2 rotate(float radians) {
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        return rotate(cosine, sine);
    }

    public Vec2 rotate(float cosine, float sine) {
        float oldX = x;
        x = oldX * cosine - y * sine;
        y = oldX * sine + y * cosine;
        return this;
    }

    public Vec2 rotated(float radians) { return copy().rotate(radians); }
    public Vec2 lerp(Vec2 target, float amount) { x += (target.x - x) * amount; y += (target.y - y) * amount; return this; }
    public boolean isFinite() { return !Float.isNaN(x) && !Float.isNaN(y) && !Float.isInfinite(x) && !Float.isInfinite(y); }
    public String toString() { return "(" + x + ", " + y + ")"; }

    public static Vec2 fromAngle(float radians) { return new Vec2((float) Math.cos(radians), (float) Math.sin(radians)); }
    public static float crossAt(Vec2 radius, Vec2 vector) { return radius.x * vector.y - radius.y * vector.x; }
}
