package wreckbox.core;

/**
 * Sparse cell fluid for the phone heap budget. One byte is the fill level of an
 * otherwise empty 6 px cell; a small open-addressed table replaces the original
 * full-map float array. Water falls, levels out, extinguishes fire and provides
 * buoyancy/flow queries to the other fixed-capacity systems.
 */
public final class FluidSystem {
  public static final int TABLE = 4096;
  public static final int MAX_CELLS = 2048;
  public static final int FULL = 255;
  private static final int EMPTY = -1, TOMBSTONE = -2;

  /** Public for the allocation-free renderer; only entries with amount != 0 are visible. */
  public final int[] cell = new int[TABLE];
  public final byte[] amount = new byte[TABLE];
  private final byte[] stamp = new byte[TABLE];
  public int count, rejected;
  private float clock;
  private int tick;

  public FluidSystem() { reset(); }

  public void reset() {
    for (int i = 0; i < TABLE; i++) { cell[i] = EMPTY; amount[i] = 0; stamp[i] = 0; }
    count = rejected = 0; clock = 0.0f; tick = 0;
  }

  private int hash(int at) { return (at * 1103515245 + 12345) & (TABLE - 1); }

  private int find(int at) {
    int slot = hash(at);
    for (int n = 0; n < TABLE; n++) {
      int key = cell[slot];
      if (key == at) return slot;
      if (key == EMPTY) return -1;
      slot = (slot + 1) & (TABLE - 1);
    }
    return -1;
  }

  private int insertSlot(int at) {
    int slot = hash(at), free = -1;
    for (int n = 0; n < TABLE; n++) {
      int key = cell[slot];
      if (key == at) return slot;
      if (key < 0 && free < 0) free = slot;
      if (key == EMPTY) break;
      slot = (slot + 1) & (TABLE - 1);
    }
    if (free < 0 || count >= MAX_CELLS) { rejected++; return -1; }
    cell[free] = at; amount[free] = 0; count++;
    return free;
  }

  public int amountAt(int at) {
    int slot = find(at);
    return slot < 0 ? 0 : amount[slot] & 255;
  }

  public int amountNear(CellGrid grid, int at) {
    int result = amountAt(at), x = at % grid.width;
    if (x > 0) result = Math.max(result, amountAt(at - 1));
    if (x + 1 < grid.width) result = Math.max(result, amountAt(at + 1));
    if (at >= grid.width) result = Math.max(result, amountAt(at - grid.width));
    if (at + grid.width < grid.size) result = Math.max(result, amountAt(at + grid.width));
    return result;
  }

  private int put(int at, int value) {
    if (value <= 0) {
      int slot = find(at);
      if (slot >= 0) { amount[slot] = 0; cell[slot] = TOMBSTONE; count--; }
      return 0;
    }
    int slot = find(at);
    if (slot < 0) slot = insertSlot(at);
    if (slot < 0) return 0;
    int next = Math.min(FULL, value);
    amount[slot] = (byte)next;
    return next;
  }

  private int change(int at, int delta) {
    int old = amountAt(at), next = Math.max(0, Math.min(FULL, old + delta));
    if (next != old) put(at, next);
    return next - old;
  }

  /** Adds water at a world position, searching a few cells upward if the target is solid. */
  public int add(CellGrid grid, float px, float py, int volume) {
    int gx = (int)Math.floor(px / 6.0f), gy = (int)Math.floor(py / 6.0f);
    if (!grid.inside(gx, gy)) return 0;
    for (int n = 0; n < CellGrid.HEIGHT; n++) {
      int y = gy - n;
      if (y < 0) break;
      int at = grid.index(gx, y);
      if (grid.material(at) == 0) return change(at, volume);
    }
    return 0;
  }

  /** One bounded simulation tick; no arrays are allocated after reset. */
  public void update(Game game, float dt, int physicsMode) {
    clock += dt;
    float interval = physicsMode <= 0 ? 0.14f : physicsMode >= 2 ? 0.075f : 0.10f;
    if (clock < interval) return;
    clock = 0.0f;
    tick = (tick + 1) & 255; if (tick == 0) tick = 1;
    CellGrid grid = game.grid;
    int start = (tick * 137) & (TABLE - 1);
    for (int n = 0; n < TABLE; n++) {
      int slot = (start + n) & (TABLE - 1), at = cell[slot];
      if (at < 0) continue;
      int available = amount[slot] & 255;
      if (available == 0 || (stamp[slot] & 255) == tick) continue;
      stamp[slot] = (byte)tick;
      int x = at % grid.width, y = at / grid.width;
      if (grid.material(at) != 0) { put(at, 0); continue; }

      if (y + 1 < CellGrid.HEIGHT) {
        int down = at + grid.width, material = grid.material(down);
        if (material == 12) {
          change(at, -48);
          grid.set(down, 13, 0, grid.flags(down));
          game.structuralDirty = true;
          continue;
        }
        if (material == 0) {
          int moved = transfer(grid, at, down, Math.min(available, FULL - amountAt(down)));
          available -= moved;
          if (moved > 0 && available == 0) continue;
        }
      }
      if (available <= 3) continue;
      int direction = ((at + tick) & 1) == 0 ? -1 : 1;
      boolean moved = false;
      for (int side = 0; side < 2 && !moved; side++) {
        int nx = x + direction;
        if (nx >= 0 && nx < grid.width) {
          int to = at + direction;
          if (grid.material(to) == 0) {
            int destination = amountAt(to);
            int flow = Math.min(72, (available - destination) / 2);
            if (flow > 2) { transfer(grid, at, to, flow); moved = true; }
          }
        }
        direction = -direction;
      }
    }
  }

  private int transfer(CellGrid grid, int from, int to, int volume) {
    if (volume <= 0 || grid.material(to) != 0) return 0;
    int actual = Math.min(volume, FULL - amountAt(to));
    if (actual <= 0) return 0;
    change(to, actual); change(from, -actual);
    int destination = find(to);
    if (destination >= 0) stamp[destination] = (byte)tick;
    return actual;
  }

  /** Submersion 0..1, including the recovered river/sea volume. */
  public float submersion(CellGrid grid, MapMetadata metadata, float px, float py) {
    if (metadata != null && metadata.river) {
      float x0 = metadata.riverX0 * 6.0f, x1 = (metadata.riverX1 + 1.0f) * 6.0f;
      if (px >= x0 && px <= x1 && py >= metadata.riverWaterY) return 1.0f;
    }
    int gx = (int)Math.floor(px / 6.0f), gy = (int)Math.floor(py / 6.0f);
    if (!grid.inside(gx, gy)) return 0.0f;
    int level = amountAt(grid.index(gx, gy));
    if (level <= 0) return 0.0f;
    float surface = gy * 6.0f + 6.0f * (1.0f - level / 255.0f);
    return Math2.clamp((py - surface) / 6.0f + 0.25f, 0.0f, 1.0f);
  }

  /** Horizontal pressure gradient used to push light bodies and fragments. */
  public float flow(CellGrid grid, float px, float py) {
    int gx = (int)Math.floor(px / 6.0f), gy = (int)Math.floor(py / 6.0f);
    if (!grid.inside(gx, gy)) return 0.0f;
    int at = grid.index(gx, gy);
    if (amountAt(at) == 0) return 0.0f;
    int left = gx > 0 ? amountAt(at - 1) : 0, right = gx + 1 < grid.width ? amountAt(at + 1) : 0;
    return Math2.clamp((left - right) / 255.0f, -1.0f, 1.0f);
  }

  public int memoryBytes() { return TABLE * 6; }
}
