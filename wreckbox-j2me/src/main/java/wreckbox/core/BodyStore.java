package wreckbox.core;

/** Structure-of-arrays storage for original props and lightweight rigid bodies. */
public final class BodyStore {
  public static final int F_PINNED = 1;
  public static final int F_SLEEPING = 2;
  public static final int F_EXPLOSIVE = 4;
  public static final int F_CHARGE = 8;
  public static final int F_HANGING = 16;
  public static final int F_DEAD = 32;
  public static final int F_BURNING = 64;
  public static final int F_HELD = 128;

  public int count;
  public int capacity;
  public byte[] kind;
  public short[] type;
  public float[] x, y, vx, vy, angle, angleVelocity;
  public float[] mass, hp, width, height;
  public byte[] variant, flags;
  public float[] restTime, fuse;
  public int quality = 1;
  private int tick;

  public void allocate(int count) {
    this.capacity = count;
    this.count = 0;
    kind = new byte[count]; type = new short[count];
    x = new float[count]; y = new float[count]; vx = new float[count]; vy = new float[count];
    angle = new float[count]; angleVelocity = new float[count];
    mass = new float[count]; hp = new float[count]; width = new float[count]; height = new float[count];
    variant = new byte[count]; flags = new byte[count]; restTime = new float[count]; fuse = new float[count];
  }

  public int add(int kind, int type, float x, float y, float width, float height, float mass, float hp) {
    if (count >= capacity) return -1;
    int i = count++;
    this.kind[i] = (byte)kind; this.type[i] = (short)type;
    this.x[i] = x; this.y[i] = y; this.width[i] = width; this.height[i] = height;
    this.mass[i] = mass; this.hp[i] = hp;
    vx[i] = vy[i] = angle[i] = angleVelocity[i] = restTime[i] = fuse[i] = 0.0f;
    variant[i] = 0; flags[i] = 0;
    return i;
  }

  public boolean alive(int i) { return (flags[i] & F_DEAD) == 0; }
  public boolean sleeping(int i) { return (flags[i] & F_SLEEPING) != 0; }
  public void wake(int i) {
    if (!alive(i)) return;
    flags[i] = (byte)((flags[i] & ~F_SLEEPING) & 255);
    restTime[i] = 0.0f;
  }
  public void kill(int i) { flags[i] |= F_DEAD; hp[i] = 0.0f; }

  public void wakeArea(CellGrid grid, float cx, float cy, float radius) {
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) {
      if (!alive(i)) continue;
      float dx = x[i] - cx, dy = y[i] - cy;
      if (dx * dx + dy * dy <= r2) wake(i);
    }
  }

  public int nearest(float px, float py, float range) {
    int found = -1; float best = range * range;
    for (int i = 0; i < count; i++) {
      if (!alive(i) || (flags[i] & F_PINNED) != 0) continue;
      float dx = x[i] - px, dy = y[i] - py, d = dx * dx + dy * dy;
      if (d < best) { best = d; found = i; }
    }
    return found;
  }

  public void update(CellGrid grid, DebrisSystem debris, float dt, float gravity) {
    update(grid, null, null, debris, dt, gravity);
  }

  public void update(CellGrid grid, FluidSystem fluids, MapMetadata metadata, DebrisSystem debris, float dt, float gravity) {
    tick++;
    for (int i = 0; i < count; i++) {
      if (!alive(i) || (flags[i] & F_HELD) != 0) continue;
      boolean boat = type[i] >= 0 && (Props.FLAGS[type[i]] & 32) != 0;
      float oldBottom = y[i] + height[i] * 0.5f;
      float submerged = fluids == null ? 0.0f : fluids.submersion(grid, metadata, x[i], oldBottom - height[i] * 0.25f);
      boolean floating = submerged > 0.05f;
      if ((flags[i] & F_PINNED) != 0 && !floating) continue;
      if (sleeping(i)) {
        if (floating || ((tick + i) & 15) == 0 && !hasSupport(grid, i)) wake(i);
        else continue;
      }
      if ((flags[i] & F_BURNING) != 0) {
        hp[i] -= 3.0f * dt;
        if (type[i] >= 0 && Props.EXPLOSION_FUSE[type[i]] > 0.0f) {
          fuse[i] += dt;
          if (fuse[i] >= Props.EXPLOSION_FUSE[type[i]]) hp[i] = 0.0f;
        }
        if (hp[i] <= 0.0f) { kill(i); continue; }
      }
      if (submerged > 0.0f) {
        float density = mass[i] / Math.max(0.3f, width[i] * height[i] / 36.0f);
        float buoyancy = boat ? 1.45f : Math2.clamp(1.1f / Math.max(0.2f, density), 0.0f, 1.3f);
        vy[i] += gravity * dt * (1.0f - submerged * buoyancy);
        float drag = Math.min(0.85f, dt * (0.35f + submerged * 2.2f));
        vx[i] *= 1.0f - drag; vy[i] *= 1.0f - drag; angleVelocity[i] *= 1.0f - drag;
        vx[i] += fluids.flow(grid, x[i], oldBottom - height[i] * 0.25f) * 36.0f * dt;
        if ((flags[i] & F_BURNING) != 0 && submerged > 0.2f) flags[i] &= ~F_BURNING;
      } else vy[i] += gravity * dt;
      x[i] += vx[i] * dt; y[i] += vy[i] * dt;
      angle[i] += angleVelocity[i] * dt;
      vx[i] *= 1.0f - Math.min(0.8f, 0.08f * dt);
      angleVelocity[i] *= 1.0f - Math.min(0.8f, 0.2f * dt);
      if (x[i] < width[i] * 0.5f) { x[i] = width[i] * 0.5f; vx[i] = Math.abs(vx[i]) * 0.2f; }
      float right = grid.width * 6.0f - width[i] * 0.5f;
      if (x[i] > right) { x[i] = right; vx[i] = -Math.abs(vx[i]) * 0.2f; }
      float bottom = y[i] + height[i] * 0.5f;
      int gx = (int)(x[i] / 6.0f), gy = -1;
      if (vy[i] >= 0.0f) {
        int first = Math.max(0, (int)Math.floor(oldBottom / 6.0f));
        int last = Math.min(CellGrid.HEIGHT - 1, (int)Math.floor(bottom / 6.0f));
        for (int scan = first; scan <= last; scan++) if (grid.solid(gx, scan)) { gy = scan; break; }
      }
      if (gy >= 0) {
        float impact = vy[i];
        y[i] = gy * 6.0f - height[i] * 0.5f - 0.01f;
        if (impact > 115.0f) {
          int index = grid.index(gx, Math.min(CellGrid.HEIGHT - 1, gy));
          grid.damage(index, (impact - 80.0f) * mass[i] * 0.035f);
          hp[i] -= (impact - 80.0f) * fragile(i) * 0.06f;
          if (hp[i] <= 0.0f) { kill(i); debris.spawnDust(x[i], y[i], 0x888888, 5); continue; }
        }
        vy[i] = -impact * (submerged > 0.2f ? 0.04f : 0.12f);
        vx[i] *= 0.72f; angleVelocity[i] *= 0.65f;
        if (Math.abs(vy[i]) < 9.0f) vy[i] = 0.0f;
        if (Math.abs(vx[i]) + Math.abs(vy[i]) < 7.0f) restTime[i] += dt;
        else restTime[i] = 0.0f;
        if (restTime[i] > 0.65f && !floating) { flags[i] |= F_SLEEPING; vx[i] = vy[i] = angleVelocity[i] = 0.0f; }
      } else restTime[i] = 0.0f;
      if (y[i] > 1620.0f) kill(i);
    }
  }

  private boolean hasSupport(CellGrid grid, int i) {
    float bottom = y[i] + height[i] * 0.5f + 1.0f;
    return grid.solidPixel(x[i], bottom) || grid.solidPixel(x[i] - width[i] * 0.35f, bottom)
        || grid.solidPixel(x[i] + width[i] * 0.35f, bottom);
  }

  private float fragile(int i) { return type[i] >= 0 ? Props.FRAGILE[type[i]] : 0.5f; }
  public int memoryBytes() { return capacity * 53; }

  /** Returns explosive prop id or -1; actual blast is dispatched by Game. */
  public int damageArea(float cx, float cy, float radius, float damage, float impulse) {
    int exploded = -1;
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) {
      if (!alive(i)) continue;
      float dx = x[i] - cx, dy = y[i] - cy, d2 = dx * dx + dy * dy;
      if (d2 > r2) continue;
      wake(i);
      float distance = (float)Math.sqrt(d2), scale = 1.0f - distance / Math.max(1.0f, radius);
      hp[i] -= damage * scale * fragile(i);
      float inv = 1.0f / Math.max(4.0f, distance);
      vx[i] += dx * inv * impulse * scale / Math.max(0.3f, mass[i]);
      vy[i] += dy * inv * impulse * scale / Math.max(0.3f, mass[i]) - impulse * 0.15f * scale;
      if (hp[i] <= 0.0f) {
        if (Props.EXPLOSION_RADIUS[type[i]] > 0.0f && exploded < 0) exploded = i;
        else kill(i);
      }
    }
    return exploded;
  }
}
