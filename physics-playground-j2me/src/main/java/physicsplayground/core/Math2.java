package physicsplayground.core;

/** Transcendental functions absent from CLDC 1.1's deliberately small Math API. */
public final class Math2 {
    private static final double LN2 = 0.6931471805599453094;
    private static final double HALF_PI = Math.PI * 0.5;
    private static final double QUARTER_PI = Math.PI * 0.25;

    private Math2() {}

    public static double atan(double value) {
        boolean negative = value < 0;
        double x = negative ? -value : value;
        double result;
        if (x > 1.0) result = HALF_PI - atanUnit(1.0 / x);
        else result = atanUnit(x);
        return negative ? -result : result;
    }

    private static double atanUnit(double x) {
        double offset = 0;
        if (x > 0.41421356237309503) {
            x = (x - 1.0) / (x + 1.0);
            offset = QUARTER_PI;
        }
        double square = x * x;
        double power = x;
        double sum = x;
        for (int denominator = 3; denominator <= 21; denominator += 2) {
            power *= square;
            double term = power / denominator;
            if ((denominator & 2) == 0) sum += term;
            else sum -= term;
        }
        return offset + sum;
    }

    public static double atan2(double y, double x) {
        if (x > 0) return atan(y / x);
        if (x < 0) {
            if (y >= 0) return atan(y / x) + Math.PI;
            return atan(y / x) - Math.PI;
        }
        if (y > 0) return HALF_PI;
        if (y < 0) return -HALF_PI;
        return 0;
    }

    public static double exp(double value) {
        if (value > 709.0) return Double.POSITIVE_INFINITY;
        if (value < -745.0) return 0;
        int power = (int) Math.floor(value / LN2 + 0.5);
        double remainder = value - power * LN2;
        double sum = 1.0;
        double term = 1.0;
        for (int i = 1; i <= 14; i++) {
            term *= remainder / i;
            sum += term;
        }
        return sum * pow2(power);
    }

    public static double log(double value) {
        if (value <= 0) return value == 0 ? Double.NEGATIVE_INFINITY : Double.NaN;
        int exponent = 0;
        double mantissa = value;
        while (mantissa >= 1.0) { mantissa *= 0.5; exponent++; }
        while (mantissa < 0.5) { mantissa *= 2.0; exponent--; }
        if (mantissa < 0.7071067811865476) { mantissa *= 2.0; exponent--; }
        double z = (mantissa - 1.0) / (mantissa + 1.0);
        double square = z * z;
        double power = z;
        double sum = z;
        for (int denominator = 3; denominator <= 25; denominator += 2) {
            power *= square;
            sum += power / denominator;
        }
        return exponent * LN2 + 2.0 * sum;
    }

    public static double pow(double base, double exponent) {
        if (base == 0) return exponent == 0 ? 1 : 0;
        if (base > 0) return exp(exponent * log(base));
        int integer = (int) exponent;
        if (integer != exponent) return Double.NaN;
        double result = exp(exponent * log(-base));
        return (integer & 1) == 0 ? result : -result;
    }

    public static double ldexp(double value, int exponent) { return value * pow2(exponent); }

    public static double pow2(int exponent) {
        if (exponent > 1023) return Double.POSITIVE_INFINITY;
        if (exponent < -1074) return 0;
        if (exponent >= -1022) return Double.longBitsToDouble(((long) (exponent + 1023)) << 52);
        return Double.longBitsToDouble(1L << (exponent + 1074));
    }

    public static int round(float value) { return (int) Math.floor(value + 0.5); }

    public static float wrapAngle(float angle) {
        while (angle > Math.PI) angle -= Math.PI * 2;
        while (angle < -Math.PI) angle += Math.PI * 2;
        return angle;
    }
}
