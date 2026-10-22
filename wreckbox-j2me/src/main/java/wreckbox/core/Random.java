package wreckbox.core;

/** Small deterministic PRNG shared by simulation systems. */
public final class Random {
  private int state;
  public Random(int seed) { this.state = seed; }
  public void seed(int seed) { state = seed; }
  public int next() {
    state = (state + 0x6d2b79f5);
    int t = state;
    t = (t ^ (t >>> 15)) * (t | 1);
    t ^= t + ((t ^ (t >>> 7)) * (t | 61));
    return t ^ (t >>> 14);
  }
  public float nextFloat() { return (next() >>> 8) / 16777216.0f; }
  public float range(float min, float max) { return min + (max - min) * nextFloat(); }
  public int nextInt(int bound) { return bound <= 0 ? 0 : (next() >>> 1) % bound; }
  public int rangeInt(int min, int max) { return min + nextInt(max - min + 1); }
  public boolean chance(float probability) { return nextFloat() < probability; }
}
