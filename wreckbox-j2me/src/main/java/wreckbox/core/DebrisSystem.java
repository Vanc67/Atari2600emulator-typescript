package wreckbox.core;

/** Fixed-capacity rotating collapse fragments, secondary impacts and material dust. */
public final class DebrisSystem {
  public static final int MAX_CHUNKS = 160;
  public static final int MAX_DUST = 320;
  public int chunkCount;
  public final float[] x = new float[MAX_CHUNKS], y = new float[MAX_CHUNKS];
  public final float[] vx = new float[MAX_CHUNKS], vy = new float[MAX_CHUNKS];
  public final float[] width = new float[MAX_CHUNKS], height = new float[MAX_CHUNKS];
  public final float[] mass = new float[MAX_CHUNKS], hp = new float[MAX_CHUNKS], maxHp = new float[MAX_CHUNKS];
  public final float[] angle = new float[MAX_CHUNKS], spin = new float[MAX_CHUNKS], restTime = new float[MAX_CHUNKS];
  public final int[] material = new int[MAX_CHUNKS], cells = new int[MAX_CHUNKS];
  public final boolean[] active = new boolean[MAX_CHUNKS], resting = new boolean[MAX_CHUNKS];
  public int dustCursor;
  public final float[] dustX = new float[MAX_DUST], dustY = new float[MAX_DUST];
  public final float[] dustVx = new float[MAX_DUST], dustVy = new float[MAX_DUST];
  public final float[] dustLife = new float[MAX_DUST], dustMax = new float[MAX_DUST];
  public final int[] dustColor = new int[MAX_DUST];
  public final byte[] dustSize = new byte[MAX_DUST];
  private final Random random = new Random(0x51f15e);
  private final boolean[] explosionEligible = new boolean[MAX_CHUNKS];
  private float contactX, contactY, penetration;
  private int contactGX, contactGY;

  public void reset() {
    chunkCount = 0; dustCursor = 0;
    for (int i = 0; i < MAX_CHUNKS; i++) { active[i] = false; restTime[i] = 0.0f; }
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
    active[slot] = true; resting[slot] = false; restTime[slot] = 0.0f;
    this.x[slot] = x; this.y[slot] = y;
    this.width[slot] = Math.max(2.0f, width); this.height[slot] = Math.max(2.0f, height);
    this.material[slot] = material; this.cells[slot] = Math.max(1, cells);
    this.mass[slot] = Math.max(0.1f, mass); this.hp[slot] = Math.max(1.0f, hp); maxHp[slot] = Math.max(1.0f, hp);
    vx[slot] = random.range(-14.0f, 14.0f); vy[slot] = random.range(-4.0f, 2.0f);
    angle[slot] = 0.0f; spin[slot] = random.range(-1.1f, 1.1f);
    return slot;
  }

  public void spawnDust(float x, float y, int color, int count) {
    for (int n = 0; n < count; n++) {
      int i = dustCursor++ % MAX_DUST;
      dustX[i] = x + random.range(-2.0f, 2.0f); dustY[i] = y + random.range(-2.0f, 2.0f);
      dustVx[i] = random.range(-72.0f, 72.0f); dustVy[i] = random.range(-105.0f, 18.0f);
      dustMax[i] = dustLife[i] = random.range(0.45f, 1.5f); dustColor[i] = color;
      dustSize[i] = (byte)(1 + random.nextInt(3));
    }
  }

  /** Returns true when an impact changed structural HP and the solver should run again. */
  public boolean update(CellGrid grid, PeopleStore people, float dt, float gravity) {
    boolean changed = false;
    int limit = chunkCount;
    for (int i = 0; i < limit; i++) {
      if (!active[i]) continue;
      if (resting[i]) {
        if (hasSupport(grid, i)) continue;
        resting[i] = false; restTime[i] = 0.0f;
      }
      vy[i] += gravity * dt;
      x[i] += vx[i] * dt; y[i] += vy[i] * dt; angle[i] += spin[i] * dt;
      spin[i] *= 1.0f - Math.min(0.35f, 0.08f * dt);
      float worldWidth = grid.width * 6.0f;
      if (x[i] < 2.0f) { x[i] = 2.0f; vx[i] = Math.abs(vx[i]) * 0.3f; }
      else if (x[i] > worldWidth - 2.0f) { x[i] = worldWidth - 2.0f; vx[i] = -Math.abs(vx[i]) * 0.3f; }

      float cs = Math2.cos(angle[i]), sn = Math2.sin(angle[i]);
      if (findFloorContact(grid, i, cs, sn)) {
        y[i] -= penetration + 0.02f;
        float impact = Math.max(0.0f, vy[i]) + Math.abs(spin[i]) * Math.max(width[i], height[i]) * 0.18f;
        if (impact > 85.0f) {
          float damage = mass[i] * (impact - 65.0f) * 0.018f;
          if (contactGX >= 0 && contactGX < grid.width) {
            int index = grid.index(contactGX, Math.min(CellGrid.HEIGHT - 1, contactGY));
            int oldHp = grid.hp16(index);
            if (grid.damage(index, damage) || grid.hp16(index) != oldHp) changed = true;
          }
          hp[i] -= impact * Materials.BRITTLE[material[i]] * (impact > 220.0f ? 0.12f : 0.075f);
          spawnDust(contactX, contactY, Materials.DUST[material[i]], Math.min(12, 2 + cells[i] / 5));
          if (people != null && impact > 110.0f) {
            float radius = Math.min(54.0f, 12.0f + width[i] * 0.25f + Math2.sqrt(mass[i]));
            float injury = Math.min(75.0f, (impact - 90.0f) * mass[i] * 0.0025f);
            if (injury > 0.5f) people.scare(contactX, contactY, radius, injury);
          }
        }
        float offset = (contactX - x[i]) / Math.max(4.0f, width[i]);
        spin[i] = Math2.clamp(spin[i] + offset * impact * 0.012f, -4.0f, 4.0f);
        vy[i] = -impact * 0.08f; vx[i] *= 0.62f; spin[i] *= 0.72f;
        if (Math.abs(vx[i]) + Math.abs(vy[i]) + Math.abs(spin[i]) * 4.0f < 14.0f) {
          restTime[i] += dt;
          if (restTime[i] > 0.22f) { resting[i] = true; vx[i] = vy[i] = spin[i] = 0.0f; }
        } else restTime[i] = 0.0f;
        if (hp[i] <= 0.0f) breakChunk(i);
      } else restTime[i] = 0.0f;
      if (y[i] > 1620.0f) active[i] = false;
    }

    for (int i = 0; i < MAX_DUST; i++) {
      if (dustLife[i] <= 0.0f) continue;
      dustLife[i] -= dt;
      dustVy[i] += gravity * 0.2f * dt;
      dustX[i] += dustVx[i] * dt; dustY[i] += dustVy[i] * dt;
      dustVx[i] *= 1.0f - Math.min(0.8f, dt * 1.4f);
      if (dustVy[i] > 0.0f && grid.solidPixel(dustX[i], dustY[i])) {
        dustY[i] -= 1.0f; dustVy[i] *= -0.12f; dustVx[i] *= 0.55f;
      }
    }
    return changed;
  }

  private boolean hasSupport(CellGrid grid, int i) {
    float cs = Math2.cos(angle[i]), sn = Math2.sin(angle[i]);
    float ex = Math.abs(cs) * width[i] * 0.5f + Math.abs(sn) * height[i] * 0.5f;
    float ey = Math.abs(sn) * width[i] * 0.5f + Math.abs(cs) * height[i] * 0.5f;
    return grid.solidPixel(x[i], y[i] + ey + 1.0f)
        || grid.solidPixel(x[i] - ex * 0.65f, y[i] + ey)
        || grid.solidPixel(x[i] + ex * 0.65f, y[i] + ey);
  }

  private boolean findFloorContact(CellGrid grid, int i, float cs, float sn) {
    penetration = 0.0f;
    boolean found = false;
    for (int n = -1; n <= 1; n++) {
      float localX = n * width[i] * 0.45f, localY = height[i] * 0.5f;
      float px = x[i] + localX * cs - localY * sn;
      float py = y[i] + localX * sn + localY * cs;
      int gx = (int)Math.floor(px / 6.0f), gy = (int)Math.floor(py / 6.0f);
      if (!grid.solid(gx, gy)) continue;
      float depth = py - gy * 6.0f;
      if (!found || depth > penetration) {
        found = true; penetration = depth; contactX = px; contactY = py; contactGX = gx; contactGY = gy;
      }
    }
    return found;
  }

  private void breakChunk(int index) {
    if (!active[index]) return;
    float oldX = x[index], oldY = y[index], oldVX = vx[index], oldVY = vy[index], oldAngle = angle[index];
    float oldWidth = width[index], oldHeight = height[index], oldMass = mass[index], oldMaxHp = maxHp[index];
    int oldCells = cells[index], oldMaterial = material[index];
    active[index] = false;
    spawnDust(oldX, oldY, Materials.DUST[oldMaterial], Math.min(16, 4 + oldCells / 3));
    if (oldCells < 5 || oldWidth < 4.0f && oldHeight < 4.0f) return;
    int children = oldMaterial == 5 || oldMaterial == 17 ? 4 : oldCells > 18 ? 3 : 2;
    for (int n = 0; n < children; n++) {
      int childCells = oldCells / children + (n < oldCells % children ? 1 : 0);
      float childMass = Math.max(0.1f, oldMass * childCells / oldCells);
      int slot = spawnChunk(oldX + random.range(-3.0f, 3.0f), oldY + random.range(-3.0f, 3.0f),
          Math.max(2.0f, oldWidth * 0.55f), Math.max(2.0f, oldHeight * 0.55f), oldMaterial,
          childCells, childMass, Math.max(1.0f, oldMaxHp * 0.22f));
      if (slot >= 0) {
        vx[slot] = oldVX + random.range(-45.0f, 45.0f); vy[slot] = oldVY - random.range(5.0f, 45.0f);
        angle[slot] = oldAngle; spin[slot] = random.range(-2.2f, 2.2f);
      }
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
    int limit = chunkCount;
    for (int i = 0; i < limit; i++) explosionEligible[i] = active[i];
    for (int i = 0; i < limit; i++) if (explosionEligible[i] && active[i]) {
      float dx = x[i] - cx, dy = y[i] - cy, d2 = dx * dx + dy * dy;
      if (d2 > r2) continue;
      float distance = Math2.sqrt(d2), scale = 1.0f - distance / Math.max(radius, 1.0f);
      float inv = 1.0f / Math.max(4.0f, distance);
      resting[i] = false; restTime[i] = 0.0f;
      vx[i] += dx * inv * impulse * scale / mass[i];
      vy[i] += dy * inv * impulse * scale / mass[i] - impulse * 0.12f * scale;
      spin[i] += random.range(-1.0f, 1.0f) * scale * impulse / Math.max(40.0f, mass[i]);
      hp[i] -= impulse * 0.05f * scale;
      if (hp[i] <= 0.0f) breakChunk(i);
    }
  }
}
