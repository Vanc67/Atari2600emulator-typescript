package wreckbox.core;

/** Original residents plus phone-budget locomotion, panic, injury and ragdoll states. */
public final class PeopleStore {
  public static final int ALIVE = 0, DOWN = 1, DEAD = 2, GONE = 3;
  public int count, deaths, injured;
  public int capacity;
  public float[] x, y, vx, vy, hp, speed, runSpeed, stateTime, panic;
  public byte[] direction, variant, role, state;
  private final Random random = new Random(0x715e);
  private int tick;

  public void allocate(int capacity) {
    this.capacity = capacity; count = 0; deaths = injured = 0;
    x = new float[capacity]; y = new float[capacity]; vx = new float[capacity]; vy = new float[capacity];
    hp = new float[capacity]; speed = new float[capacity]; runSpeed = new float[capacity];
    stateTime = new float[capacity]; panic = new float[capacity];
    direction = new byte[capacity]; variant = new byte[capacity]; role = new byte[capacity]; state = new byte[capacity];
  }

  public int add(float x, float y, float hp, float speed, float runSpeed, int direction, int variant, int role) {
    if (count >= capacity) return -1;
    int i = count++;
    this.x[i] = x; this.y[i] = y; this.hp[i] = hp; this.speed[i] = speed; this.runSpeed[i] = runSpeed;
    this.direction[i] = (byte)(direction < 0 ? -1 : 1); this.variant[i] = (byte)variant; this.role[i] = (byte)role;
    state[i] = ALIVE; stateTime[i] = random.range(0.2f, 3.0f); panic[i] = 0.0f; vx[i] = vy[i] = 0.0f;
    return i;
  }

  public void scare(float cx, float cy, float radius, float damage) {
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) {
      if (state[i] >= DEAD) continue;
      float dx = x[i] - cx, dy = y[i] - cy, d2 = dx * dx + dy * dy;
      if (d2 > r2) continue;
      float scale = 1.0f - (float)Math.sqrt(d2) / Math.max(1.0f, radius);
      panic[i] = 6.0f;
      direction[i] = (byte)(dx < 0.0f ? -1 : 1);
      if (damage > 0.0f) hurt(i, damage * scale, dx * 2.0f, -Math.abs(damage) * 0.4f);
    }
  }

  public void hurt(int i, float damage, float impulseX, float impulseY) {
    if (state[i] >= DEAD) return;
    hp[i] -= damage;
    vx[i] += impulseX; vy[i] += impulseY;
    injured++;
    if (hp[i] <= 0.0f) { hp[i] = 0.0f; state[i] = DEAD; deaths++; }
    else if (damage > 18.0f || hp[i] < 35.0f) state[i] = DOWN;
  }

  public int nearest(float px, float py, float range) {
    int found = -1; float best = range * range;
    for (int i = 0; i < count; i++) if (state[i] < GONE) {
      float dx = x[i] - px, dy = y[i] - py, d = dx * dx + dy * dy;
      if (d < best) { best = d; found = i; }
    }
    return found;
  }

  public void update(CellGrid grid, float dt, float gravity) {
    tick++;
    for (int i = 0; i < count; i++) {
      if (state[i] == GONE) continue;
      stateTime[i] -= dt;
      if (panic[i] > 0.0f) panic[i] -= dt;
      int gx = (int)(x[i] / 6.0f);
      int groundY = (int)((y[i] + 2.0f) / 6.0f);
      boolean grounded = grid.solid(gx, groundY);
      if (!grounded || state[i] >= DOWN) {
        vy[i] += gravity * dt;
      } else if (vy[i] > 0.0f) vy[i] = 0.0f;
      if (state[i] == ALIVE && grounded) {
        if (stateTime[i] <= 0.0f) {
          stateTime[i] = random.range(1.0f, 4.0f);
          if (random.chance(0.35f)) direction[i] = (byte)-direction[i];
        }
        float desired = panic[i] > 0.0f ? runSpeed[i] : (stateTime[i] > 0.7f ? speed[i] : 0.0f);
        vx[i] += (direction[i] * desired - vx[i]) * Math.min(1.0f, dt * 8.0f);
        int ahead = (int)((x[i] + direction[i] * 4.0f) / 6.0f);
        if (grid.solid(ahead, (int)((y[i] - 5.0f) / 6.0f)) || !grid.solid(ahead, groundY)) {
          if (panic[i] > 0.0f && grid.solid(ahead, groundY)) vy[i] = -85.0f;
          else { direction[i] = (byte)-direction[i]; vx[i] = 0.0f; }
        }
      } else if (state[i] >= DOWN) vx[i] *= 1.0f - Math.min(0.9f, dt * 2.0f);
      x[i] += vx[i] * dt; y[i] += vy[i] * dt;
      int gy = (int)((y[i] + 2.0f) / 6.0f);
      if (grid.solid((int)(x[i] / 6.0f), gy) && vy[i] >= 0.0f) {
        float impact = vy[i];
        y[i] = gy * 6.0f - 2.05f; vy[i] = 0.0f;
        if (impact > 155.0f) hurt(i, (impact - 130.0f) * 0.3f, 0.0f, 0.0f);
      }
      if (x[i] < 2.0f || x[i] > grid.width * 6.0f - 2.0f || y[i] > 1610.0f) state[i] = GONE;
    }
  }
}
