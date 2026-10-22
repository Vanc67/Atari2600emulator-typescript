package wreckbox.core;

/** Sparse burning-cell set: ignition, burn-through, spread and water suppression. */
public final class FireSystem {
  public static final int MAX = 384;
  public final int[] cell = new int[MAX];
  public final float[] age = new float[MAX], nextDamage = new float[MAX];
  public final boolean[] active = new boolean[MAX];
  private final Random random = new Random(0xf1ae);
  private float spreadClock;

  public int memoryBytes() { return MAX * 13; }

  public void reset() {
    for (int i = 0; i < MAX; i++) active[i] = false;
    spreadClock = 0.0f;
  }

  public boolean ignite(CellGrid grid, int index) {
    int m = grid.material(index);
    if (m == 0 || Materials.FLAMMABILITY[m] <= 0.0f || Materials.FIXED[m]) return false;
    for (int i = 0; i < MAX; i++) if (active[i] && cell[i] == index) return true;
    for (int i = 0; i < MAX; i++) if (!active[i]) {
      active[i] = true; cell[i] = index; age[i] = 0.0f; nextDamage[i] = 0.35f;
      return true;
    }
    return false;
  }

  public void igniteArea(CellGrid grid, BodyStore bodies, float x, float y, float radius) {
    int cx = (int)(x / 6.0f), cy = (int)(y / 6.0f), r = (int)Math.ceil(radius / 6.0f);
    for (int yy = -r; yy <= r; yy++) for (int xx = -r; xx <= r; xx++) {
      if (xx * xx + yy * yy > r * r || !grid.inside(cx + xx, cy + yy) || !random.chance(0.42f)) continue;
      ignite(grid, grid.index(cx + xx, cy + yy));
    }
    for (int i = 0; i < bodies.count; i++) if (bodies.alive(i)) {
      float dx = bodies.x[i] - x, dy = bodies.y[i] - y;
      if (dx * dx + dy * dy < radius * radius && bodies.type[i] >= 0
          && (Props.FLAMMABILITY[bodies.type[i]] > 0.0f || Props.EXPLOSION_RADIUS[bodies.type[i]] > 0.0f))
        bodies.flags[i] |= BodyStore.F_BURNING;
    }
  }

  public void douse(CellGrid grid, float x, float y, float radius) {
    float r2 = radius * radius;
    for (int i = 0; i < MAX; i++) if (active[i]) {
      float dx = (cell[i] % grid.width) * 6.0f + 3.0f - x;
      float dy = (cell[i] / grid.width) * 6.0f + 3.0f - y;
      if (dx * dx + dy * dy <= r2) active[i] = false;
    }
  }

  public void update(CellGrid grid, DebrisSystem debris, float dt) { update(grid, null, debris, dt); }

  public void update(CellGrid grid, FluidSystem fluids, DebrisSystem debris, float dt) {
    spreadClock += dt;
    boolean spread = spreadClock >= 0.28f;
    if (spread) spreadClock = 0.0f;
    for (int i = 0; i < MAX; i++) {
      if (!active[i]) continue;
      int at = cell[i], m = grid.material(at);
      if (m == 0 || Materials.FLAMMABILITY[m] <= 0.0f) { active[i] = false; continue; }
      if (fluids != null && fluids.amountNear(grid, at) > 12) { active[i] = false; continue; }
      age[i] += dt; nextDamage[i] -= dt;
      if (nextDamage[i] <= 0.0f) {
        nextDamage[i] = 0.35f;
        float damage = Materials.HP[m] / Math.max(2.0f, Materials.BURN_TIME[m]) * 0.35f;
        if (grid.damage(at, damage)) {
          debris.spawnDust((at % grid.width) * 6.0f + 3.0f, (at / grid.width) * 6.0f + 3.0f, Materials.DUST[m], 2);
          active[i] = false; continue;
        }
      }
      if (spread && random.chance(Materials.FLAMMABILITY[m] * 0.3f)) {
        int x = at % grid.width, direction = random.nextInt(4);
        int nx = x + (direction == 0 ? -1 : direction == 1 ? 1 : 0);
        int ny = at / grid.width + (direction == 2 ? -1 : direction == 3 ? 1 : 0);
        if (grid.inside(nx, ny)) ignite(grid, grid.index(nx, ny));
      }
      if (age[i] > Math.max(6.0f, Materials.BURN_TIME[m] * 2.5f)) active[i] = false;
    }
  }
}
