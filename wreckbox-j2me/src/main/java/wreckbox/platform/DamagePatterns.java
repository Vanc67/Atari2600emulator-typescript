package wreckbox.platform;

/** Recovered Le[1..3][6] 6 px damage masks and q() pattern selection. */
public final class DamagePatterns {
  private static final byte[] MASKS = new byte[18 * 36];

  private static final class Rng {
    int state;
    Rng(int seed) { state = seed; }
    double next() {
      state = 1831565813 + state;
      int e = (state ^ (state >>> 15)) * (1 | state);
      e = (e + ((e ^ (e >>> 7)) * (61 | e))) ^ e;
      return ((e ^ (e >>> 14)) & 0xffffffffL) / 4294967296.0;
    }
  }

  static {
    Rng random = new Rng(1337);
    for (int level = 1; level <= 3; level++) for (int pattern = 0; pattern < 6; pattern++) {
      int base = ((level - 1) * 6 + pattern) * 36;
      for (int crack = 0; crack < level; crack++) {
        int x, y, dx, dy;
        int side = (int)(4.0 * random.next());
        if (side == 0) { x = (int)(6.0 * random.next()); y = 0; dx = 0; dy = 1; }
        else if (side == 1) { x = (int)(6.0 * random.next()); y = 5; dx = 0; dy = -1; }
        else if (side == 2) { x = 0; y = (int)(6.0 * random.next()); dx = 1; dy = 0; }
        else { x = 5; y = (int)(6.0 * random.next()); dx = -1; dy = 0; }
        int length = 3 + 2 * level + (int)(3.0 * random.next());
        for (int step = 0; step < length && x >= 0 && y >= 0 && x <= 5 && y <= 5; step++) {
          MASKS[base + 6 * y + x] = 1;
          if (random.next() < 0.45) {
            if (dx == 0) x += random.next() < 0.5 ? -1 : 1;
            else y += random.next() < 0.5 ? -1 : 1;
          } else { x += dx; y += dy; }
        }
      }
      if (level == 3) {
        int x = random.next() < 0.5 ? 0 : 5;
        int y = random.next() < 0.5 ? 0 : 5;
        MASKS[base + 6 * y + x] = 2;
        MASKS[base + 6 * y + (x == 0 ? 1 : 4)] = 2;
        MASKS[base + 6 * (y == 0 ? 1 : 4) + x] = 2;
      }
    }
  }

  private DamagePatterns() { }

  private static double noise(int x, int y) {
    int n = x * 374761393 + y * 668265263;
    n = (n ^ (n >>> 13)) * 1274126177;
    n ^= n >>> 16;
    return (n & 0xffffffffL) / 4294967296.0;
  }

  public static int pattern(int cellX, int cellY) {
    return (int)(6.0 * noise(7 * cellX + 3, 13 * cellY + 1));
  }

  /** 0=intact, 1=dark crack, 2=transparent severe-damage hole. */
  public static int mask(int level, int pattern, int x, int y) {
    if (level < 1 || level > 3 || pattern < 0 || pattern > 5 || x < 0 || x > 5 || y < 0 || y > 5) return 0;
    return MASKS[((level - 1) * 6 + pattern) * 36 + 6 * y + x];
  }
}
