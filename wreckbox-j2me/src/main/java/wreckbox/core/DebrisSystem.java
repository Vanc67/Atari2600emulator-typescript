package wreckbox.core;

/** Fixed-capacity rigid collapse chunks and dust; no gameplay-loop allocation. */
public final class DebrisSystem {
  public static final int MAX_CHUNKS = 120;
  public static final int MAX_DUST = 240;
  public int chunkCount;
  public final float[] x = new float[MAX_CHUNKS], y = new float[MAX_CHUNKS];
  public final float[] vx = new float[MAX_CHUNKS], vy = new float[MAX_CHUNKS];
  public final float[] width = new float[MAX_CHUNKS], height = new float[MAX_CHUNKS];
  public final float[] mass = new float[MAX_CHUNKS], hp = new float[MAX_CHUNKS];
  public final float[] angle = new float[MAX_CHUNKS], spin = new float[MAX_CHUNKS];
  public final int[] material = new int[MAX_CHUNKS], cells = new int[MAX_CHUNKS];
  public final boolean[] active = new boolean[MAX_CHUNKS], resting = new boolean[MAX_CHUNKS];
  public int dustCursor;
  public final float[] dustX = new float[MAX_DUST], dustY = new float[MAX_DUST];
  public final float[] dustVx = new float[MAX_DUST], dustVy = new float[MAX_DUST];
  public final float[] dustLife = new float[MAX_DUST], dustMax = new float[MAX_DUST];
  public final int[] dustColor = new int[MAX_DUST];
  private final Random random = new Random(0x51f15e);

  public void reset() {
    chunkCount = 0; dustCursor = 0;
    for (int i = 0; i < MAX_CHUNKS; i++) active[i] = false;
    for (int i = 0; i < MAX_DUST; i++) dustLife[i] = 0.0f;
  }

  public int spawnChunk(float x, float y, float width, float height, int material, int cells, float mass, float hp) {
    int slot = -1;
    for (int i = 0; i < MAX_CHUNKS; i++) if (!active[i]) { slot = i; break; }
    if (slot < 0) {
      spawnDust(x, y, Materials.DUST[material], Math.min(12, 2 + cells / 8));
      return -1;
    }
    if (slot >= chunkCount) chunkCount = slot + 1;
    active[slot] = true; resting[slot] = false;
    this.x[slot] = x; this.y[slot] = y; this.width[slot] = Math.max(2.0f, width); this.height[slot] = Math.max(2.0f, height);
    this.material[slot] = material; this.cells[slot] = cells; this.mass[slot] = Math.max(0.1f, mass); this.hp[slot] = hp;
    vx[slot] = random.range(-10.0f, 10.0f); vy[slot] = 0.0f; angle[slot] = 0.0f; spin[slot] = random.range(-0.5f, 0.5f);
    return slot;
  }

  public void spawnDust(float x, float y, int color, int count) {
    for (int n = 0; n < count; n++) {
      int i = dustCursor++ % MAX_DUST;
      dustX[i] = x; dustY[i] = y; dustVx[i] = random.range(-65.0f, 65.0f); dustVy[i] = random.range(-95.0f, 15.0f);
      dustMax[i] = dustLife[i] = random.range(0.35f, 1.1f); dustColor[i] = color;
    }
  }

  public void update(CellGrid grid, float dt, float gravity) {
    for (int i = 0; i < chunkCount; i++) {
      if (!active[i] || resting[i]) continue;
      vy[i] += gravity * dt; x[i] += vx[i] * dt; y[i] += vy[i] * dt; angle[i] += spin[i] * dt;
      int gx = (int)(x[i] / 6.0f), gy = (int)((y[i] + height[i] * 0.5f) / 6.0f);
      if (grid.solid(gx, gy)) {
        float impact = vy[i];
        y[i] = gy * 6.0f - height[i] * 0.5f - 0.01f;
        if (impact > 105.0f && gx >= 0 && gx < grid.width) {
          int index = grid.index(gx, Math.min(CellGrid.HEIGHT - 1, gy));
          grid.damage(index, mass[i] * (impact - 70.0f) * 0.025f);
          hp[i] -= impact * Materials.BRITTLE[material[i]] * 0.08f;
          spawnDust(x[i], y[i] + height[i] * 0.5f, Materials.DUST[material[i]], Math.min(8, 1 + cells[i] / 10));
        }
        vy[i] = -impact * 0.1f; vx[i] *= 0.66f; spin[i] *= 0.6f;
        if (Math.abs(vy[i]) < 10.0f) vy[i] = 0.0f;
        if (Math.abs(vx[i]) + Math.abs(vy[i]) < 8.0f) {
          resting[i] = true; vx[i] = vy[i] = spin[i] = 0.0f;
        }
        if (hp[i] <= 0.0f) { active[i] = false; spawnDust(x[i], y[i], Materials.DUST[material[i]], 8); }
      }
      if (y[i] > 1620.0f) active[i] = false;
    }
    for (int i = 0; i < MAX_DUST; i++) {
      if (dustLife[i] <= 0.0f) continue;
      dustLife[i] -= dt;
      dustVy[i] += gravity * 0.25f * dt; dustX[i] += dustVx[i] * dt; dustY[i] += dustVy[i] * dt;
      dustVx[i] *= 1.0f - Math.min(0.8f, dt * 1.4f);
    }
  }

  public int nearest(float px, float py, float range) {
    int found = -1; float best = range * range;
    for (int i = 0; i < chunkCount; i++) if (active[i]) {
      float dx = x[i] - px, dy = y[i] - py, d = dx * dx + dy * dy;
      if (d < best) { best = d; found = i; }
    }
    return found;
  }

  public void applyExplosion(float cx, float cy, float radius, float impulse) {
    float r2 = radius * radius;
    for (int i = 0; i < chunkCount; i++) if (active[i]) {
      float dx = x[i] - cx, dy = y[i] - cy, d2 = dx * dx + dy * dy;
      if (d2 > r2) continue;
      float distance = (float)Math.sqrt(d2), scale = 1.0f - distance / Math.max(radius, 1.0f);
      float inv = 1.0f / Math.max(4.0f, distance);
      resting[i] = false;
      vx[i] += dx * inv * impulse * scale / mass[i];
      vy[i] += dy * inv * impulse * scale / mass[i] - impulse * 0.12f * scale;
      hp[i] -= impulse * 0.05f * scale;
      if (hp[i] <= 0.0f) { active[i] = false; spawnDust(x[i], y[i], Materials.DUST[material[i]], 4); }
    }
  }
}
