package wreckbox.core;

/** Fixed-capacity rotating collapse fragments, secondary impacts and material dust. */
public final class DebrisSystem {
  public static final int MAX_CHUNKS = 160;
  public static final int MAX_SHAPE_CELLS = 64;
  public static final int MAX_DUST = 320;
  public int chunkCount;
  public final float[] x = new float[MAX_CHUNKS], y = new float[MAX_CHUNKS];
  public final float[] vx = new float[MAX_CHUNKS], vy = new float[MAX_CHUNKS];
  public final float[] width = new float[MAX_CHUNKS], height = new float[MAX_CHUNKS];
  public final float[] mass = new float[MAX_CHUNKS], hp = new float[MAX_CHUNKS], maxHp = new float[MAX_CHUNKS];
  public final float[] angle = new float[MAX_CHUNKS], spin = new float[MAX_CHUNKS], restTime = new float[MAX_CHUNKS];
  public final int[] material = new int[MAX_CHUNKS], cells = new int[MAX_CHUNKS];
  /** Local cell masks: x/y are unsigned bytes packed in one short. */
  public final byte[] shapeCount = new byte[MAX_CHUNKS];
  public final short[] shapePos = new short[MAX_CHUNKS * MAX_SHAPE_CELLS];
  public final short[] shapeMatTint = new short[MAX_CHUNKS * MAX_SHAPE_CELLS];
  public final float[] shapeOriginX = new float[MAX_CHUNKS], shapeOriginY = new float[MAX_CHUNKS];
  public final boolean[] active = new boolean[MAX_CHUNKS], resting = new boolean[MAX_CHUNKS];
  public int dustCursor;
  public final float[] dustX = new float[MAX_DUST], dustY = new float[MAX_DUST];
  public final float[] dustVx = new float[MAX_DUST], dustVy = new float[MAX_DUST];
  public final float[] dustLife = new float[MAX_DUST], dustMax = new float[MAX_DUST];
  public final int[] dustColor = new int[MAX_DUST];
  public final byte[] dustSize = new byte[MAX_DUST];
  private final Random random = new Random(0x51f15e);
  private final boolean[] explosionEligible = new boolean[MAX_CHUNKS];
  private final short[] brokenPos = new short[MAX_SHAPE_CELLS], brokenMatTint = new short[MAX_SHAPE_CELLS];
  private int brokenCount;
  private float brokenOriginX, brokenOriginY;
  private float contactX, contactY, penetration;
  private int contactGX, contactGY;
  public int quality = 1;
  public int maxDust = MAX_DUST;

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
    active[slot] = true; resting[slot] = false; restTime[slot] = 0.0f; shapeCount[slot] = 0;
    this.x[slot] = x; this.y[slot] = y;
    this.width[slot] = Math.max(2.0f, width); this.height[slot] = Math.max(2.0f, height);
    this.material[slot] = material; this.cells[slot] = Math.max(1, cells);
    this.mass[slot] = Math.max(0.1f, mass); this.hp[slot] = Math.max(1.0f, hp); maxHp[slot] = Math.max(1.0f, hp);
    // Oa/Ga in the original starts a detached structure with zero velocity
    // and angular velocity. Impacts and explosions, not random spawn jitter,
    // supply the subsequent motion.
    vx[slot] = 0.0f; vy[slot] = 0.0f;
    angle[slot] = 0.0f; spin[slot] = 0.0f;
    return slot;
  }

  public void setShapeCell(int chunk, int index, int localX, int localY, int cellMaterial, int tint) {
    if (chunk < 0 || index < 0 || index >= MAX_SHAPE_CELLS) return;
    int at = chunk * MAX_SHAPE_CELLS + index;
    shapePos[at] = (short)((localX & 255) | ((localY & 255) << 8));
    shapeMatTint[at] = (short)((cellMaterial & 31) | ((tint & 31) << 5));
    if (index + 1 > shapeCount[chunk]) shapeCount[chunk] = (byte)(index + 1);
  }

  public void setShapeOrigin(int chunk, float originX, float originY) {
    if (chunk >= 0) { shapeOriginX[chunk] = originX; shapeOriginY[chunk] = originY; }
  }

  public void spawnDust(float x, float y, int color, int count) {
    if (maxDust <= 0) return;
    for (int n = 0; n < count; n++) {
      int i = dustCursor++ % maxDust;
      dustX[i] = x + random.range(-2.0f, 2.0f); dustY[i] = y + random.range(-2.0f, 2.0f);
      dustVx[i] = random.range(-72.0f, 72.0f); dustVy[i] = random.range(-105.0f, 18.0f);
      dustMax[i] = dustLife[i] = random.range(0.45f, 1.5f); dustColor[i] = color;
      dustSize[i] = (byte)(1 + random.nextInt(3));
    }
  }

  /** Returns true when an impact changed structural HP and the solver should run again. */
  public boolean update(CellGrid grid, PeopleStore people, float dt, float gravity) {
    return update(grid, null, null, people, dt, gravity);
  }

  public boolean update(CellGrid grid, FluidSystem fluids, MapMetadata metadata, PeopleStore people, float dt, float gravity) {
    boolean changed = false;
    int limit = chunkCount;
    for (int i = 0; i < limit; i++) {
      if (!active[i]) continue;
      float submerged = fluids == null ? 0.0f : fluids.submersion(grid, metadata, x[i], y[i] + height[i] * 0.25f);
      if (resting[i]) {
        if (submerged < 0.05f && hasSupport(grid, i)) continue;
        resting[i] = false; restTime[i] = 0.0f;
      }
      if (submerged > 0.0f) {
        float buoyancy = Math2.clamp(1.1f / Math.max(0.25f, Materials.DENSITY[material[i]]), 0.0f, 1.35f);
        vy[i] += gravity * dt * (1.0f - submerged * buoyancy);
        float drag = Math.min(0.85f, dt * (0.4f + submerged * 2.4f));
        vx[i] *= 1.0f - drag; vy[i] *= 1.0f - drag; spin[i] *= 1.0f - drag;
        vx[i] += fluids.flow(grid, x[i], y[i]) * 42.0f * dt;
      } else vy[i] += gravity * dt;
      float startX = x[i], startY = y[i], startAngle = angle[i], integrationSpin = spin[i];
      float travel = Math.max(Math.abs(vx[i]), Math.abs(vy[i])) * dt;
      int steps = Math.max(1, Math.min(quality <= 0 ? 2 : quality >= 2 ? 6 : 4, (int)Math.ceil(travel / 5.0f)));
      spin[i] *= 1.0f - Math.min(0.35f, 0.08f * dt);
      float worldWidth = grid.width * 6.0f, cs = 1.0f, sn = 0.0f;
      boolean contact = false;
      for (int step = 1; step <= steps; step++) {
        float fraction = (float)step / steps;
        x[i] = startX + vx[i] * dt * fraction; y[i] = startY + vy[i] * dt * fraction;
        angle[i] = startAngle + integrationSpin * dt * fraction;
        if (x[i] < 2.0f) { x[i] = 2.0f; vx[i] = Math.abs(vx[i]) * 0.3f; }
        else if (x[i] > worldWidth - 2.0f) { x[i] = worldWidth - 2.0f; vx[i] = -Math.abs(vx[i]) * 0.3f; }
        cs = Math2.cos(angle[i]); sn = Math2.sin(angle[i]);
        if (findFloorContact(grid, i, cs, sn)) { contact = true; break; }
      }
      if (contact) {
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
    if (shapeCount[i] > 0) {
      int base = i * MAX_SHAPE_CELLS, count = shapeCount[i];
      for (int n = 0; n < count; n++) {
        int position = shapePos[base + n];
        float localX = (position & 255) * 6.0f + 3.0f - shapeOriginX[i];
        float localY = ((position >>> 8) & 255) * 6.0f + 6.0f - shapeOriginY[i];
        float px = x[i] + localX * cs - localY * sn, py = y[i] + localX * sn + localY * cs;
        if (grid.solidPixel(px, py + 1.0f)) return true;
      }
      return false;
    }
    float ex = Math.abs(cs) * width[i] * 0.5f + Math.abs(sn) * height[i] * 0.5f;
    float ey = Math.abs(sn) * width[i] * 0.5f + Math.abs(cs) * height[i] * 0.5f;
    return grid.solidPixel(x[i], y[i] + ey + 1.0f)
        || grid.solidPixel(x[i] - ex * 0.65f, y[i] + ey)
        || grid.solidPixel(x[i] + ex * 0.65f, y[i] + ey);
  }

  private boolean findFloorContact(CellGrid grid, int i, float cs, float sn) {
    penetration = 0.0f;
    if (shapeCount[i] > 0) return findShapedFloorContact(grid, i, cs, sn);
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

  private boolean findShapedFloorContact(CellGrid grid, int i, float cs, float sn) {
    boolean found = false; int base = i * MAX_SHAPE_CELLS, count = shapeCount[i];
    int stride = quality <= 0 ? 3 : quality == 1 ? 2 : 1;
    for (int n = 0; n < count; n++) {
      if (stride > 1 && n % stride != 0 && n != count - 1) continue;
      int position = shapePos[base + n];
      float centerX = (position & 255) * 6.0f + 3.0f - shapeOriginX[i];
      float centerY = ((position >>> 8) & 255) * 6.0f + 3.0f - shapeOriginY[i];
      // The centre and, in detailed mode, both lower corners follow the real
      // cell mask. No invisible bounding-box corner can hold a fragment up.
      for (int corner = quality >= 2 ? -1 : 0; corner <= (quality >= 2 ? 1 : 0); corner++) {
        float localX = centerX + corner * 3.0f, localY = centerY + 3.0f;
        float px = x[i] + localX * cs - localY * sn, py = y[i] + localX * sn + localY * cs;
        int gx = (int)Math.floor(px / 6.0f), gy = (int)Math.floor(py / 6.0f);
        if (!grid.solid(gx, gy)) continue;
        float depth = py - gy * 6.0f;
        if (!found || depth > penetration) {
          found = true; penetration = depth; contactX = px; contactY = py; contactGX = gx; contactGY = gy;
        }
      }
    }
    return found;
  }

  private void breakChunk(int index) {
    if (!active[index]) return;
    float oldX = x[index], oldY = y[index], oldVX = vx[index], oldVY = vy[index], oldAngle = angle[index];
    float oldWidth = width[index], oldHeight = height[index], oldMass = mass[index], oldMaxHp = maxHp[index];
    int oldCells = cells[index], oldMaterial = material[index];
    brokenCount = shapeCount[index];
    if (brokenCount > 0) {
      int base = index * MAX_SHAPE_CELLS;
      for (int n = 0; n < brokenCount; n++) {
        brokenPos[n] = shapePos[base + n]; brokenMatTint[n] = shapeMatTint[base + n];
      }
      brokenOriginX = shapeOriginX[index]; brokenOriginY = shapeOriginY[index];
    }
    active[index] = false;
    spawnDust(oldX, oldY, Materials.DUST[oldMaterial], Math.min(16, 4 + oldCells / 3));
    if (oldCells < 5 || oldWidth < 4.0f && oldHeight < 4.0f) return;
    int children = oldMaterial == 5 || oldMaterial == 17 ? 4 : oldCells > 18 ? 3 : 2;
    if (brokenCount > 0) {
      breakShapedChunk(oldX, oldY, oldVX, oldVY, oldAngle, oldMass, oldMaxHp, oldCells, oldMaterial, children);
      return;
    }
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

  private void breakShapedChunk(float oldX, float oldY, float oldVX, float oldVY, float oldAngle,
      float oldMass, float oldMaxHp, int oldCells, int oldMaterial, int children) {
    float cs = Math2.cos(oldAngle), sn = Math2.sin(oldAngle);
    for (int child = 0; child < children; child++) {
      int assigned = 0, minX = 255, maxX = 0, minY = 255, maxY = 0;
      float sumX = 0.0f, sumY = 0.0f;
      for (int n = child; n < brokenCount; n += children) {
        int px = brokenPos[n] & 255, py = (brokenPos[n] >>> 8) & 255;
        sumX += px * 6.0f + 3.0f; sumY += py * 6.0f + 3.0f; assigned++;
        if (px < minX) minX = px; if (px > maxX) maxX = px;
        if (py < minY) minY = py; if (py > maxY) maxY = py;
      }
      if (assigned == 0) continue;
      float centerX = sumX / assigned, centerY = sumY / assigned;
      float offsetX = centerX - brokenOriginX, offsetY = centerY - brokenOriginY;
      float worldX = oldX + offsetX * cs - offsetY * sn;
      float worldY = oldY + offsetX * sn + offsetY * cs;
      float childMass = Math.max(0.1f, oldMass * assigned / oldCells);
      int slot = spawnChunk(worldX, worldY, (maxX - minX + 1) * 6, (maxY - minY + 1) * 6,
          oldMaterial, assigned, childMass, Math.max(1.0f, oldMaxHp * 0.22f));
      if (slot < 0) continue;
      setShapeOrigin(slot, centerX - minX * 6.0f, centerY - minY * 6.0f);
      int shapeIndex = 0;
      for (int n = child; n < brokenCount; n += children) {
        int px = brokenPos[n] & 255, py = (brokenPos[n] >>> 8) & 255, encoded = brokenMatTint[n];
        setShapeCell(slot, shapeIndex++, px - minX, py - minY, encoded & 31, (encoded >>> 5) & 31);
      }
      vx[slot] = oldVX + random.range(-45.0f, 45.0f); vy[slot] = oldVY - random.range(5.0f, 45.0f);
      angle[slot] = oldAngle; spin[slot] = random.range(-2.2f, 2.2f);
    }
  }

  public int memoryBytes() { return MAX_CHUNKS * 59 + MAX_DUST * 29
      + MAX_CHUNKS * MAX_SHAPE_CELLS * 4 + MAX_CHUNKS * 9 + MAX_SHAPE_CELLS * 4; }

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
