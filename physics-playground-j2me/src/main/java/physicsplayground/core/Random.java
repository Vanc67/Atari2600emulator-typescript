package physicsplayground.core;

/** Godot-compatible PCG32 generator, including its seeding and randf bit path. */
public final class Random {
    private static final long MULTIPLIER = 6364136223846793005L;
    private static final long DEFAULT_INC = 1442695040888963407L;
    private long state;
    private long inc;
    private long currentSeed;

    public Random() { this(System.currentTimeMillis()); }

    public Random(long seed) {
        inc = DEFAULT_INC;
        seed(seed);
    }

    public void seed(long seed) {
        currentSeed = seed;
        state = 0;
        inc = (DEFAULT_INC << 1) | 1;
        nextInt();
        state += seed;
        nextInt();
    }

    public long getSeed() { return currentSeed; }
    public long getState() { return state; }
    public void setState(long state) { this.state = state; }

    public int nextInt() {
        long old = state;
        state = old * MULTIPLIER + (inc | 1);
        int xorshifted = (int) (((old >>> 18) ^ old) >>> 27);
        int rotation = (int) (old >>> 59);
        return (xorshifted >>> rotation) | (xorshifted << ((-rotation) & 31));
    }

    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound");
        long unsignedBound = bound & 0xffffffffL;
        long threshold = (0x100000000L - unsignedBound) % unsignedBound;
        while (true) {
            long value = nextInt() & 0xffffffffL;
            if (value >= threshold) return (int) (value % unsignedBound);
        }
    }

    public float nextFloat() {
        int proto = nextInt();
        if (proto == 0) return 0.0f;
        long significand = (nextInt() | 0x80000001) & 0xffffffffL;
        int exponent = -32 - leadingZeros(proto);
        return (float) (significand * Math2.pow2(exponent));
    }

    public double nextDouble() {
        int proto = nextInt();
        if (proto == 0) return 0.0;
        long high = nextInt() & 0xffffffffL;
        long low = nextInt() & 0xffffffffL;
        long significand = (high << 32) | low | 0x8000000000000001L;
        int exponent = -64 - leadingZeros(proto);
        /* Convert the unsigned 64-bit significand without signed overflow. */
        double value = (double) (significand & 0x7fffffffffffffffL);
        if (significand < 0) value += 9223372036854775808.0;
        return value * Math2.pow2(exponent);
    }

    public float range(float from, float to) { return nextFloat() * (to - from) + from; }
    public double range(double from, double to) { return nextDouble() * (to - from) + from; }
    public int range(int from, int to) {
        if (from == to) return from;
        int min = Math.min(from, to);
        int max = Math.max(from, to);
        long difference = (long) max - min;
        if (difference == 0xffffffffL) return (int) ((nextInt() & 0xffffffffL) + min);
        return nextInt((int) difference + 1) + min;
    }

    private static int leadingZeros(int value) {
        if (value == 0) return 32;
        int count = 0;
        if ((value & 0xffff0000) == 0) { count += 16; value <<= 16; }
        if ((value & 0xff000000) == 0) { count += 8; value <<= 8; }
        if ((value & 0xf0000000) == 0) { count += 4; value <<= 4; }
        if ((value & 0xc0000000) == 0) { count += 2; value <<= 2; }
        if ((value & 0x80000000) == 0) count++;
        return count;
    }
}
