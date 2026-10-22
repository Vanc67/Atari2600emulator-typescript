package wreckbox.core;

/** Eleven-point resident bodies: original joints, braces, grid collision and sleep. */
public final class RagdollStore {
  public static final int POINTS = 11, HEAD = 0, CHEST = 1, PELVIS = 2;
  public static final int[] JOINT_A = {0, 1, 2, 3, 2, 5, 1, 7, 1, 9};
  public static final int[] JOINT_B = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
  private static final float[] JOINT_L = {3.2f, 8.5f, 6.0f, 6.0f, 6.0f, 6.0f, 5.0f, 5.0f, 5.0f, 5.0f};
  private static final int[] BRACE_A = {0, 2, 2, 1, 1};
  private static final int[] BRACE_B = {2, 4, 6, 8, 10};
  private static final float[] BRACE_L = {10.53f, 6.0f, 6.0f, 3.5f, 3.5f};
  private static final float[] MASS = {1.1f, 0.55f, 0.45f, 1.0f, 1.25f, 1.0f, 1.25f, 1.3f, 1.6f, 1.3f, 1.6f};
  private static final float[] IMPACT = {1.6f, 1.1f, 1.0f, 0.5f, 0.35f, 0.5f, 0.35f, 0.4f, 0.3f, 0.4f, 0.3f};

  public int capacity;
  public boolean[] active, sleeping;
  public float[] x, y, vx, vy, px, py, oldX, oldY, still;
  private byte[] contact;
  public int quality = 1;
  private boolean escapeUsed;
  private float hitX, hitY;

  public void allocate(int people) {
    capacity = people; int size = people * POINTS;
    active = new boolean[people]; sleeping = new boolean[people]; still = new float[people];
    x = new float[size]; y = new float[size]; vx = new float[size]; vy = new float[size];
    px = new float[size]; py = new float[size]; oldX = new float[size]; oldY = new float[size];
    contact = new byte[size];
  }
  private int base(int person) { return person * POINTS; }
  public int memoryBytes() { return capacity * 6 + capacity * POINTS * 33; }
  public float centerX(int person) { return x[base(person) + PELVIS]; }
  public float centerY(int person) { return y[base(person) + PELVIS]; }
  public float headX(int person) { return x[base(person) + HEAD]; }
  public float headY(int person) { return y[base(person) + HEAD]; }

  private void point(int person, int index, float x, float y) {
    int at = base(person) + index;
    this.x[at] = this.oldX[at] = x; this.y[at] = this.oldY[at] = y;
  }
  private void leg(int person, int knee, int foot, float x, float y, int direction, float a, float b) {
    float kx = x + Math2.sin(a) * 6.0f * direction, ky = y + Math2.cos(a) * 6.0f;
    point(person, knee, kx, ky);
    point(person, foot, kx + Math2.sin(a - b) * 6.0f * direction, ky + Math2.cos(a - b) * 6.0f);
  }
  private void arm(int person, int elbow, int hand, float x, float y, int direction, float a, float b) {
    float ex = x + Math2.sin(a) * 5.0f * direction, ey = y + 1.0f + Math2.cos(a) * 5.0f;
    point(person, elbow, ex, ey);
    point(person, hand, ex + Math2.sin(a + b) * 5.0f * direction, ey + Math2.cos(a + b) * 5.0f);
  }

  public void start(PeopleStore owner, int person, float impulseX, float impulseY, float spin) {
    if (active[person]) { addImpulse(person, impulseX, impulseY, spin); return; }
    int direction = owner.direction[person] < 0 ? -1 : 1;
    float f = 0.06f, h = -0.06f, p = 0.5f;
    float leftLength = Math2.cos(f) * 12.0f, rightLength = Math2.cos(h) * 12.0f;
    float baseY = owner.y[person] - Math.max(leftLength, rightLength) - 0.2f;
    float baseX = owner.x[person];
    point(person, PELVIS, baseX, baseY);
    leg(person, 3, 4, baseX, baseY, direction, f, 0.0f);
    leg(person, 5, 6, baseX, baseY, direction, h, 0.0f);
    float chestX = baseX + p * direction, chestY = baseY - 8.5f;
    point(person, CHEST, chestX, chestY);
    point(person, HEAD, chestX + 0.35f * p * direction, chestY - 3.2f);
    arm(person, 7, 8, chestX, chestY, direction, 0.08f, 0.3f);
    arm(person, 9, 10, chestX, chestY, direction, -0.05f, 0.25f);
    int base = base(person); float meanX = 0.0f, meanY = 0.0f;
    for (int n = 0; n < POINTS; n++) { meanX += x[base + n]; meanY += y[base + n]; }
    meanX /= POINTS; meanY /= POINTS;
    for (int n = 0; n < POINTS; n++) {
      int at = base + n;
      vx[at] = impulseX - spin * (y[at] - meanY) + owner.random.range(-8.0f, 8.0f);
      vy[at] = impulseY + spin * (x[at] - meanX) + owner.random.range(-8.0f, 8.0f);
    }
    active[person] = true; sleeping[person] = false; still[person] = 0.0f;
  }

  public void addImpulse(int person, float impulseX, float impulseY, float spin) {
    if (!active[person]) return;
    sleeping[person] = false; still[person] = 0.0f;
    int base = base(person), pelvis = base + PELVIS;
    for (int n = 0; n < POINTS; n++) {
      int at = base + n;
      vx[at] += impulseX - spin * (y[at] - y[pelvis]);
      vy[at] += impulseY + spin * (x[at] - x[pelvis]);
    }
  }

  public void update(PeopleStore owner, CellGrid grid, int person, float dt, float gravity, float time) {
    update(owner, grid, null, null, person, dt, gravity, time);
  }

  public void update(PeopleStore owner, CellGrid grid, FluidSystem fluids, MapMetadata metadata,
      int person, float dt, float gravity, float time) {
    int base = base(person);
    if (owner.held[person]) {
      sleeping[person] = false;
      int chest = base + CHEST;
      float targetX = 12.0f * (owner.holdX[person] - x[chest]);
      float targetY = 12.0f * (owner.holdY[person] - y[chest]);
      vx[chest] += (targetX - vx[chest]) * Math.min(1.0f, 9.0f * dt);
      vy[chest] += (targetY - vy[chest]) * Math.min(1.0f, 9.0f * dt);
    }
    if (sleeping[person]) {
      int pelvis = base + PELVIS;
      boolean inWater = fluids != null && fluids.dynamicSubmersion(grid, x[pelvis], y[pelvis]) > 0.1f;
      if (inWater || !grid.solidPixel(x[pelvis], y[pelvis] + 3.0f) && !grid.solidPixel(x[base + CHEST], y[base + CHEST] + 3.0f)) sleeping[person] = false;
      else { owner.updateDown(grid, person, dt, gravity, time); return; }
    }
    float maxSpeed2 = 0.0f;
    for (int n = 0; n < POINTS; n++) { int at = base + n; maxSpeed2 = Math.max(maxSpeed2, vx[at] * vx[at] + vy[at] * vy[at]); }
    if (maxSpeed2 > 2250000.0f) {
      float scale = 1500.0f / Math2.sqrt(maxSpeed2);
      for (int n = 0; n < POINTS; n++) { int at = base + n; vx[at] *= scale; vy[at] *= scale; }
      maxSpeed2 = 2250000.0f;
    }
    int steps = Math.max(1, Math.min(6, (int)Math.ceil(Math2.sqrt(maxSpeed2) * dt / 3.0f)));
    float step = dt / steps, impact = 0.0f;
    for (int s = 0; s < steps; s++) {
      for (int n = 0; n < POINTS; n++) {
        int at = base + n;
        float water = fluids == null ? 0.0f : fluids.dynamicSubmersion(grid, x[at], y[at]);
        if (water > 0.0f) {
          vy[at] += gravity * step * (1.0f - water * 0.96f);
          float drag = Math.min(0.75f, step * (0.5f + water * 2.5f));
          vx[at] *= 1.0f - drag; vy[at] *= 1.0f - drag;
          vx[at] += fluids.flow(grid, x[at], y[at]) * 24.0f * step;
        } else vy[at] += gravity * step;
        px[at] = x[at] + vx[at] * step; py[at] = y[at] + vy[at] * step; contact[at] = 0;
      }
      escapeUsed = false;
      int iterations = quality <= 0 ? 2 : quality >= 2 ? 6 : 4;
      for (int iteration = 0; iteration < iterations; iteration++) {
        for (int n = 0; n < JOINT_A.length; n++) constraint(base, JOINT_A[n], JOINT_B[n], JOINT_L[n], false);
        for (int n = 0; n < BRACE_A.length; n++) constraint(base, BRACE_A[n], BRACE_B[n], BRACE_L[n], true);
        collideMidpoint(grid, base, 1, 2); collideMidpoint(grid, base, 2, 3); collideMidpoint(grid, base, 2, 5);
        for (int n = 0; n < POINTS; n++) collidePoint(grid, person, base + n);
      }
      float oldEnergy = 0.0f, newEnergy = 0.0f, oldVX = 0.0f, oldVY = 0.0f, newVX = 0.0f, newVY = 0.0f, massSum = 0.0f;
      for (int n = 0; n < POINTS; n++) {
        int at = base + n;
        float nvx = (px[at] - x[at]) / step, nvy = (py[at] - y[at]) / step, inverseMass = 1.0f / MASS[n];
        oldVX += inverseMass * vx[at]; oldVY += inverseMass * vy[at];
        oldEnergy += inverseMass * (vx[at] * vx[at] + vy[at] * vy[at]); massSum += inverseMass;
        int normal = contact[at];
        if (normal != 0) {
          float nx = normal == 1 ? -1.0f : normal == 2 ? 1.0f : 0.0f;
          float ny = normal == 3 ? -1.0f : normal == 4 ? 1.0f : 0.0f;
          float normalSpeed = -(vx[at] * nx + vy[at] * ny);
          if (normalSpeed > 0.0f) impact = Math.max(impact, normalSpeed * IMPACT[n]);
          float projected = nvx * nx + nvy * ny, allowed = 0.1f * Math.max(0.0f, normalSpeed);
          if (projected > allowed) { nvx -= (projected - allowed) * nx; nvy -= (projected - allowed) * ny; }
          if (ny != 0.0f) nvx *= 0.6f; else nvy *= 0.8f;
        }
        vx[at] = nvx; vy[at] = nvy; x[at] = px[at]; y[at] = py[at];
        newVX += inverseMass * nvx; newVY += inverseMass * nvy; newEnergy += inverseMass * (nvx * nvx + nvy * nvy);
      }
      float correctX = momentumClamp(oldVX / massSum, newVX / massSum);
      float correctY = momentumClamp(oldVY / massSum, newVY / massSum);
      if (correctX != 0.0f || correctY != 0.0f) for (int n = 0; n < POINTS; n++) { int at = base + n; vx[at] += correctX; vy[at] += correctY; }
      if (newEnergy > 1.02f * oldEnergy + 400.0f) {
        float scale = Math2.sqrt((1.02f * oldEnergy + 400.0f) / newEnergy);
        for (int n = 0; n < POINTS; n++) { int at = base + n; vx[at] *= scale; vy[at] *= scale; }
      }
    }
    if (impact > 150.0f) {
      float severity = impact * impact / 900.0f / 15.0f;
      if (severity > 3.0f && owner.state[person] != PeopleStore.DEAD)
        owner.hurt(person, 9.0f * (severity - 3.0f) + 10.0f * Math.max(0.0f, severity - 10.0f), 0.0f, 0.0f, PeopleStore.CAUSE_FALL);
    }
    int pelvis = base + PELVIS;
    owner.x[person] = x[pelvis]; owner.y[person] = y[pelvis];
    if (y[pelvis] > 1660.0f || x[pelvis] < -300.0f || x[pelvis] > grid.width * 6.0f + 300.0f) { owner.remove(person); return; }
    float maxVelocity = 0.0f, displacement = 0.0f;
    for (int n = 0; n < POINTS; n++) {
      int at = base + n;
      maxVelocity = Math.max(maxVelocity, Math.abs(vx[at]) + Math.abs(vy[at]));
      displacement = Math.max(displacement, Math.max(Math.abs(x[at] - oldX[at]), Math.abs(y[at] - oldY[at])));
    }
    if (maxVelocity < 120.0f && displacement < 1.5f && !owner.held[person] && gravity > 0.0f) {
      still[person] += dt;
      if (still[person] > 0.6f) {
        sleeping[person] = true;
        for (int n = 0; n < POINTS; n++) { int at = base + n; vx[at] = vy[at] = 0.0f; }
      }
    } else {
      still[person] = 0.0f;
      for (int n = 0; n < POINTS; n++) { int at = base + n; oldX[at] = x[at]; oldY[at] = y[at]; }
    }
    owner.updateDown(grid, person, dt, gravity, time);
  }

  private float momentumClamp(float before, float after) {
    float high = Math.max(before, 0.0f) + 0.1f * Math.max(-before, 0.0f) + 15.0f;
    float low = Math.min(before, 0.0f) - 0.1f * Math.max(before, 0.0f) - 15.0f;
    return after > high ? high - after : after < low ? low - after : 0.0f;
  }
  private void constraint(int base, int a, int b, float length, boolean maximumOnly) {
    int ia = base + a, ib = base + b;
    float dx = px[ib] - px[ia], dy = py[ib] - py[ia], distance = Math2.sqrt(dx * dx + dy * dy);
    if (distance < 0.0001f) distance = 0.0001f;
    if (maximumOnly && distance >= length) return;
    float scale = (distance - length) / (distance * (MASS[a] + MASS[b]));
    px[ia] += dx * scale * MASS[a]; py[ia] += dy * scale * MASS[a];
    px[ib] -= dx * scale * MASS[b]; py[ib] -= dy * scale * MASS[b];
  }

  private boolean free(CellGrid grid, int x, int y) {
    return x >= 0 && x < grid.width && (y < 0 || (y < CellGrid.HEIGHT && !grid.solid(x, y)));
  }
  private int normal(CellGrid grid, float x, float y, float oldX, float oldY) {
    int gx = (int)Math.floor(x / 6.0f), gy = (int)Math.floor(y / 6.0f);
    int ox = (int)Math.floor(oldX / 6.0f), oy = (int)Math.floor(oldY / 6.0f);
    float left = gx * 6.0f, top = gy * 6.0f, best = 1000000000.0f; int direction = 0;
    if (ox != gx || oy != gy) {
      if (oy < gy && free(grid, gx, gy - 1)) { best = y - top; direction = 3; }
      else if (oy > gy && free(grid, gx, gy + 1)) { best = top + 6.0f - y; direction = 4; }
      if (ox < gx && free(grid, gx - 1, gy) && x - left < best) { best = x - left; direction = 1; }
      else if (ox > gx && free(grid, gx + 1, gy) && left + 6.0f - x < best) { best = left + 6.0f - x; direction = 2; }
    }
    if (direction == 0) {
      if (free(grid, gx, gy - 1) && y - top < best) { best = y - top; direction = 3; }
      if (free(grid, gx - 1, gy) && x - left < best) { best = x - left; direction = 1; }
      if (free(grid, gx + 1, gy) && left + 6.0f - x < best) { best = left + 6.0f - x; direction = 2; }
      if (free(grid, gx, gy + 1) && top + 6.0f - y < best) direction = 4;
    }
    hitX = direction == 1 ? left - 0.05f : direction == 2 ? left + 6.05f : x;
    hitY = direction == 3 ? top - 0.05f : direction == 4 ? top + 6.05f : y;
    return direction;
  }

  private void collidePoint(CellGrid grid, int person, int at) {
    float x = px[at], y = py[at];
    int gx = (int)Math.floor(x / 6.0f), gy = (int)Math.floor(y / 6.0f);
    if (gx < 0 || gx >= grid.width || gy < 0 || (gy < CellGrid.HEIGHT && !grid.solid(gx, gy))) return;
    int direction = normal(grid, x, y, this.x[at], this.y[at]);
    if (direction != 0) { px[at] = hitX; py[at] = hitY; contact[at] = (byte)direction; return; }
    float best = 1000000000.0f, bestX = x, bestY = y;
    for (int yy = -3; yy <= 3; yy++) for (int xx = -3; xx <= 3; xx++) {
      if (!free(grid, gx + xx, gy + yy)) continue;
      float cx = Math2.clamp(x, (gx + xx) * 6.0f + 0.05f, (gx + xx) * 6.0f + 5.95f);
      float cy = Math2.clamp(y, (gy + yy) * 6.0f + 0.05f, (gy + yy) * 6.0f + 5.95f);
      float dx = cx - x, dy = cy - y, distance = dx * dx + dy * dy * (yy > 0 ? 2.0f : 1.0f);
      if (distance < best) { best = distance; bestX = cx; bestY = cy; }
    }
    if (best < 1000000000.0f && !escapeUsed) {
      escapeUsed = true; float dx = bestX - x, dy = bestY - y; int base = base(person);
      for (int n = 0; n < POINTS; n++) { int p = base + n; px[p] += dx; py[p] += dy; this.x[p] += dx; this.y[p] += dy; }
      contact[at] = (byte)(dy < -0.01f ? 3 : dy > 0.01f ? 4 : dx < 0.0f ? 1 : 2);
    } else { px[at] = this.x[at]; py[at] = this.y[at]; contact[at] = 3; }
  }

  private void collideMidpoint(CellGrid grid, int base, int a, int b) {
    int ia = base + a, ib = base + b;
    float x = 0.5f * (px[ia] + px[ib]), y = 0.5f * (py[ia] + py[ib]);
    int gx = (int)Math.floor(x / 6.0f), gy = (int)Math.floor(y / 6.0f);
    if (gx < 0 || gx >= grid.width || gy < 0 || gy >= CellGrid.HEIGHT || !grid.solid(gx, gy)) return;
    int direction = normal(grid, x, y, 0.5f * (this.x[ia] + this.x[ib]), 0.5f * (this.y[ia] + this.y[ib]));
    if (direction == 0) return;
    float dx = hitX - x, dy = hitY - y;
    px[ia] += dx; py[ia] += dy; px[ib] += dx; py[ib] += dy;
    if (contact[ia] == 0) contact[ia] = (byte)direction;
    if (contact[ib] == 0) contact[ib] = (byte)direction;
  }
}
