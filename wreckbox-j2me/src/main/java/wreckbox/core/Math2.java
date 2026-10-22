package wreckbox.core;

/** Transcendental functions missing from CLDC 1.1, with float wrappers for the game. */
public final class Math2 {
  private static final double LN2 = 0.6931471805599453094;
  private static final double HALF_PI = Math.PI * 0.5;
  private static final double QUARTER_PI = Math.PI * 0.25;
  private Math2() {}
  public static float clamp(float v, float min, float max) { return v < min ? min : v > max ? max : v; }
  public static float sqrt(float v) { return (float)Math.sqrt(v); }
  public static float sin(float v) { return (float)Math.sin(v); }
  public static float cos(float v) { return (float)Math.cos(v); }
  public static int round(float v) { return (int)Math.floor(v + 0.5); }
  public static float log10(float v) { return (float)(log(v) / 2.302585092994046); }
  public static float pow(float v, float p) {
    if (v == 0.0f) return p == 0.0f ? 1.0f : 0.0f;
    return (float)exp(p * log(v));
  }
  public static float cbrt(float v) { return pow(v, 1.0f / 3.0f); }
  public static float atan2(float y, float x) {
    if (x > 0.0f) return atan(y / x);
    if (x < 0.0f) return (float)(y >= 0.0f ? atan(y / x) + Math.PI : atan(y / x) - Math.PI);
    if (y > 0.0f) return (float)HALF_PI;
    if (y < 0.0f) return (float)-HALF_PI;
    return 0.0f;
  }
  private static float atan(float value) {
    boolean negative = value < 0.0f; float x = negative ? -value : value;
    float result = x > 1.0f ? (float)(HALF_PI - atanUnit(1.0f / x)) : (float)atanUnit(x);
    return negative ? -result : result;
  }
  private static double atanUnit(double x) {
    double offset = 0.0;
    if (x > 0.41421356237309503) { x = (x - 1.0) / (x + 1.0); offset = QUARTER_PI; }
    double square = x * x, power = x, sum = x;
    for (int denominator = 3; denominator <= 21; denominator += 2) {
      power *= square; double term = power / denominator;
      if ((denominator & 2) == 0) sum += term; else sum -= term;
    }
    return offset + sum;
  }
  private static double log(double value) {
    if (value <= 0.0) return value == 0.0 ? Double.NEGATIVE_INFINITY : Double.NaN;
    int exponent = 0; double mantissa = value;
    while (mantissa >= 1.0) { mantissa *= 0.5; exponent++; }
    while (mantissa < 0.5) { mantissa *= 2.0; exponent--; }
    if (mantissa < 0.7071067811865476) { mantissa *= 2.0; exponent--; }
    double z = (mantissa - 1.0) / (mantissa + 1.0), square = z * z, power = z, sum = z;
    for (int denominator = 3; denominator <= 25; denominator += 2) { power *= square; sum += power / denominator; }
    return exponent * LN2 + 2.0 * sum;
  }
  private static double exp(double value) {
    if (value > 709.0) return Double.POSITIVE_INFINITY;
    if (value < -745.0) return 0.0;
    int power = (int)Math.floor(value / LN2 + 0.5);
    double remainder = value - power * LN2, sum = 1.0, term = 1.0;
    for (int i = 1; i <= 14; i++) { term *= remainder / i; sum += term; }
    return sum * pow2(power);
  }
  private static double pow2(int exponent) {
    if (exponent > 1023) return Double.POSITIVE_INFINITY;
    if (exponent < -1074) return 0.0;
    if (exponent >= -1022) return Double.longBitsToDouble(((long)(exponent + 1023)) << 52);
    return Double.longBitsToDouble(1L << (exponent + 1074));
  }
}
