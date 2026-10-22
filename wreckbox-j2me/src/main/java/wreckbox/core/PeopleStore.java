package wreckbox.core;

/** Residents, daily states, injury, radiation sickness and eleven-point ragdolls. */
public final class PeopleStore {
  public static final int ALIVE = 0, DOWN = 1, DEAD = 2, GONE = 3;
  public static final int IDLE = 0, WALK = 1, FLEE = 2, COWER = 3, ILL = 4, BURN = 5, GETUP = 6;
  public static final int CAUSE_BLUNT = 0, CAUSE_FALL = 1, CAUSE_FIRE = 2, CAUSE_RADIATION = 3,
      CAUSE_CRUSH = 4, CAUSE_EXPLOSION = 5, CAUSE_BLOOD = 6, CAUSE_HOLE = 7;

  public int count, deaths, injured, capacity;
  public float[] x, y, vx, vy, hp, speed, runSpeed, stateTime, panic, phase, fallY;
  public float[] dose, sick, radThreshold, radCollapse, radDeath, radStart, vomitTime;
  public float[] burnTime, charred, blood, bleed, downTime, waterTime, holdX, holdY;
  public byte[] direction, variant, role, state, mode, cause;
  public boolean[] held, noGetUp, onGround;
  public final RagdollStore rag = new RagdollStore();
  public DebrisSystem debris;
  public boolean bloodEnabled = true;
  final Random random = new Random(0x715e);
  private int tick;
  private float radiationClock;

  public void allocate(int capacity) {
    this.capacity = capacity; count = 0; deaths = injured = 0; radiationClock = 0.0f;
    x = new float[capacity]; y = new float[capacity]; vx = new float[capacity]; vy = new float[capacity];
    hp = new float[capacity]; speed = new float[capacity]; runSpeed = new float[capacity];
    stateTime = new float[capacity]; panic = new float[capacity]; phase = new float[capacity]; fallY = new float[capacity];
    dose = new float[capacity]; sick = new float[capacity]; radThreshold = new float[capacity];
    radCollapse = new float[capacity]; radDeath = new float[capacity]; radStart = new float[capacity]; vomitTime = new float[capacity];
    burnTime = new float[capacity]; charred = new float[capacity]; blood = new float[capacity]; bleed = new float[capacity];
    downTime = new float[capacity]; waterTime = new float[capacity]; holdX = new float[capacity]; holdY = new float[capacity];
    direction = new byte[capacity]; variant = new byte[capacity]; role = new byte[capacity];
    state = new byte[capacity]; mode = new byte[capacity]; cause = new byte[capacity];
    held = new boolean[capacity]; noGetUp = new boolean[capacity]; onGround = new boolean[capacity];
    rag.allocate(capacity);
  }

  public int add(float x, float y, float hp, float speed, float runSpeed, int direction, int variant, int role) {
    if (count >= capacity) return -1;
    int i = count++;
    this.x[i] = x; this.y[i] = y; this.hp[i] = hp; this.speed[i] = speed; this.runSpeed[i] = runSpeed;
    this.direction[i] = (byte)(direction < 0 ? -1 : 1); this.variant[i] = (byte)variant; this.role[i] = (byte)role;
    state[i] = ALIVE; mode[i] = IDLE; stateTime[i] = random.range(0.3f, 3.0f); phase[i] = random.range(0.0f, 6.28f);
    panic[i] = 0.0f; vx[i] = vy[i] = 0.0f; fallY[i] = y; blood[i] = 100.0f;
    float logistic = random.range(0.02f, 0.98f);
    radThreshold[i] = Math2.clamp(4.5f + 0.6f * Math2.ln(logistic / (1.0f - logistic)), 2.6f, 8.0f);
    return i;
  }

  /** Original rm(): find a free column, descend to a floor and check body clearance. */
  public int spawnAt(CellGrid grid, float px, float py) {
    if (count >= capacity) return -1;
    int cellX = (int)Math.floor(px / 6.0f), cellY = Math.max(0, Math.min(259, (int)Math.floor(py / 6.0f)));
    int chosen = -1, startY = cellY;
    int[] offsets = {0, -1, 1, -2, 2, -3, 3};
    for (int n = 0; n < offsets.length; n++) {
      int gx = cellX + offsets[n];
      if (gx >= 1 && gx < grid.width - 1 && !grid.solid(gx, cellY)) { chosen = gx; break; }
    }
    if (chosen < 0) {
      chosen = Math.max(1, Math.min(grid.width - 2, cellX));
      for (int n = 0; n < 40 && startY > 0 && grid.solid(chosen, startY); n++) startY--;
      if (grid.solid(chosen, startY)) return -1;
    }
    int floor = startY;
    for (int n = 0; n < 120 && floor < 259 && !grid.solid(chosen, floor + 1); n++) floor++;
    if (floor >= 259 || !grid.solid(chosen, floor + 1)) return -1;
    float x = 6.0f * (chosen + 0.5f), y = 6.0f * (floor + 1);
    if (!hasClearance(grid, x, y)) return -1;
    return add(x, y, 100.0f, random.range(19.0f, 29.0f), random.range(78.0f, 98.0f), random.chance(0.5f) ? -1 : 1, random.nextInt(256), 0);
  }
  private boolean hasClearance(CellGrid grid, float x, float y) {
    return !grid.solidPixel(x, y - 3.0f) && !grid.solidPixel(x, y - 9.0f) && !grid.solidPixel(x, y - 15.0f)
        && !grid.solidPixel(x, y - 21.0f) && !grid.solidPixel(x, y - 25.0f);
  }

  public void scare(float cx, float cy, float radius, float damage) {
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) {
      if (state[i] >= DEAD) continue;
      float px = centerX(i), py = centerY(i), dx = px - cx, dy = py - cy, d2 = dx * dx + dy * dy;
      if (d2 > r2) continue;
      float scale = 1.0f - Math2.sqrt(d2) / Math.max(1.0f, radius);
      panic[i] = Math.max(panic[i], random.range(3.0f, 7.0f));
      direction[i] = (byte)(dx < 0.0f ? -1 : 1);
      if (!rag.active[i]) mode[i] = FLEE;
      if (damage > 0.0f) {
        float impulseX = dx * 2.0f, impulseY = -Math.abs(damage) * 0.4f;
        if (rag.active[i]) rag.addImpulse(i, impulseX, impulseY, random.range(-1.0f, 1.0f));
        hurt(i, damage * scale, impulseX, impulseY, CAUSE_EXPLOSION);
      }
    }
  }

  public void hurt(int i, float damage, float impulseX, float impulseY) { hurt(i, damage, impulseX, impulseY, CAUSE_BLUNT); }
  public void hurt(int i, float damage, float impulseX, float impulseY, int damageCause) {
    if (i < 0 || i >= count || state[i] >= DEAD || damage <= 0.0f) return;
    hp[i] -= damage;
    if (damage > 6.0f) {
      bleed[i] = Math.min(1.6f, bleed[i] + damage / 110.0f);
      if (bloodEnabled && debris != null) debris.spawnDust(centerX(i), centerY(i), 0x8e241f, Math.min(8, 1 + (int)(damage / 12.0f)));
    }
    if (hp[i] <= 0.0f) { die(i, damageCause); return; }
    if (hp[i] < 45.0f && !rag.active[i]) startRagdoll(i, impulseX, impulseY - 30.0f, random.range(-1.0f, 1.0f));
    else if (rag.active[i] && (impulseX != 0.0f || impulseY != 0.0f)) rag.addImpulse(i, impulseX, impulseY, 0.0f);
    injured = injuredCount();
  }
  private void die(int i, int damageCause) {
    if (state[i] >= DEAD) return;
    hp[i] = 0.0f; state[i] = DEAD; cause[i] = (byte)damageCause; deaths++;
    if (damageCause != CAUSE_FIRE) burnTime[i] = 0.0f;
    if (!rag.active[i]) startRagdoll(i, vx[i], vy[i], random.range(-1.5f, 1.5f));
  }
  public void startRagdoll(int i, float impulseX, float impulseY, float spin) {
    if (state[i] == GONE) return;
    if (state[i] == ALIVE) state[i] = DOWN;
    downTime[i] = 0.0f; rag.start(this, i, impulseX, impulseY, spin);
  }
  public void remove(int i) {
    if (state[i] == GONE) return;
    if (state[i] != DEAD) deaths++;
    state[i] = GONE; held[i] = false; rag.active[i] = false;
  }

  public float centerX(int i) { return rag.active[i] ? rag.centerX(i) : x[i]; }
  public float centerY(int i) { return rag.active[i] ? rag.centerY(i) : y[i] - 12.0f; }
  public float doseX(int i) { return rag.active[i] ? rag.headX(i) : x[i]; }
  public float doseY(int i) { return rag.active[i] ? rag.headY(i) : y[i] - 15.0f; }

  public int nearest(float px, float py, float range) {
    int found = -1; float best = range * range;
    for (int i = 0; i < count; i++) if (state[i] < GONE) {
      if (rag.active[i]) {
        int base = i * RagdollStore.POINTS;
        for (int n = 0; n < RagdollStore.POINTS; n++) {
          float dx = rag.x[base + n] - px, dy = rag.y[base + n] - py, distance = dx * dx + dy * dy;
          if (distance < best) { best = distance; found = i; }
        }
      } else {
        float dx = x[i] - px, dy = y[i] - 12.0f - py, distance = dx * dx + dy * dy;
        if (distance < best) { best = distance; found = i; }
      }
    }
    return found;
  }
  public void hold(int i, float x, float y) {
    held[i] = true; holdX[i] = x; holdY[i] = y;
    if (!rag.active[i]) startRagdoll(i, 0.0f, 0.0f, 0.0f);
    rag.sleeping[i] = false;
  }
  public void moveHold(int i, float x, float y) { holdX[i] = x; holdY[i] = y; }
  public void release(int i, float velocityX, float velocityY) {
    held[i] = false;
    if (rag.active[i]) rag.addImpulse(i, velocityX, velocityY, 0.0f);
    else { vx[i] = velocityX; vy[i] = velocityY; }
  }

  public void igniteArea(float cx, float cy, float radius) {
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) if (state[i] < DEAD) {
      float dx = centerX(i) - cx, dy = centerY(i) - cy;
      if (dx * dx + dy * dy <= r2) ignite(i);
    }
  }
  public void ignite(int i) {
    if (state[i] >= DEAD && burnTime[i] > 0.0f) return;
    if (burnTime[i] <= 0.0f) burnTime[i] = random.range(7.0f, 11.0f);
    if (state[i] == ALIVE) mode[i] = BURN;
  }
  public void douse(float cx, float cy, float radius) {
    float r2 = radius * radius;
    for (int i = 0; i < count; i++) {
      float dx = centerX(i) - cx, dy = centerY(i) - cy;
      if (dx * dx + dy * dy <= r2) burnTime[i] = 0.0f;
    }
  }

  public void update(CellGrid grid, FireSystem fire, float dt, float gravity, RadiationSystem radiation, float time) {
    tick++; radiationClock += dt;
    if (radiationClock >= 0.5f) {
      float step = radiationClock; radiationClock = 0.0f;
      for (int i = 0; i < count; i++) if (state[i] < DEAD)
        dose[i] += radiation.falloutRate(grid, doseX(i), doseY(i), time) * (step / 6.0f) * 0.0096f;
    }
    for (int i = 0; i < count; i++) {
      if (state[i] == GONE) continue;
      stateTime[i] -= dt; if (panic[i] > 0.0f) panic[i] -= dt;
      updateEnvironment(grid, fire, i, dt);
      updateRadiation(i, time);
      if (state[i] == GONE) continue;
      if (rag.active[i]) rag.update(this, grid, i, dt, gravity, time);
      else if (state[i] != DEAD) updateWalking(grid, i, dt, gravity);
    }
    injured = injuredCount();
  }

  private void updateEnvironment(CellGrid grid, FireSystem fire, int i, float dt) {
    if ((tick + (variant[i] & 255)) % 6 == 0 && burnTime[i] <= 0.0f) {
      float px = centerX(i), py = centerY(i);
      for (int f = 0; f < FireSystem.MAX; f++) if (fire.active[f]) {
        int at = fire.cell[f];
        float fx = (at % grid.width) * 6.0f + 3.0f, fy = (at / grid.width) * 6.0f + 3.0f;
        if (Math.abs(fx - px) < 8.0f && Math.abs(fy - py) < 16.0f) { ignite(i); break; }
      }
    }
    if (burnTime[i] > 0.0f) {
      burnTime[i] -= dt * (rag.active[i] && state[i] != DEAD ? 1.5f : 1.0f);
      charred[i] = Math.min(1.0f, charred[i] + 0.075f * dt);
      if (state[i] != DEAD) {
        hp[i] -= 9.0f * dt;
        if (hp[i] <= 0.0f || charred[i] > 0.62f) die(i, CAUSE_FIRE);
        else if (hp[i] < 45.0f && !rag.active[i]) startRagdoll(i, 0.5f * vx[i], -20.0f, random.range(-1.0f, 1.0f));
      }
    }
    if (bleed[i] > 0.02f) {
      blood[i] -= bleed[i] * dt * 3.2f;
      bleed[i] *= Math2.exp(-dt / (state[i] == DEAD ? 30.0f : 22.0f));
      if (state[i] != DEAD && blood[i] < 35.0f) die(i, CAUSE_BLOOD);
    }
  }

  private void updateRadiation(int i, float time) {
    if (dose[i] <= 0.8f || state[i] == DEAD || state[i] == GONE) return;
    sick[i] = Math2.clamp((dose[i] - 0.8f) / 6.0f, 0.0f, 1.0f);
    if (radStart[i] == 0.0f) radStart[i] = time;
    if (dose[i] >= 50.0f) {
      if (radCollapse[i] == 0.0f || radCollapse[i] > time + 3.0f) {
        radCollapse[i] = time + random.range(0.8f, 2.5f); radDeath[i] = time + random.range(4.0f, 9.0f);
      }
    } else if (dose[i] >= 10.0f) {
      if (radDeath[i] <= 0.0f || radDeath[i] > time + 30.0f) {
        radCollapse[i] = time + random.range(6.0f, 14.0f); radDeath[i] = radCollapse[i] + random.range(6.0f, 14.0f);
      }
    } else if (dose[i] >= radThreshold[i] && radDeath[i] <= 0.0f) {
      radDeath[i] = time + random.range(24.0f, 50.0f) * Math2.clamp(radThreshold[i] / dose[i], 0.6f, 1.0f);
      radCollapse[i] = radDeath[i] - random.range(6.0f, 12.0f);
    }
    if (dose[i] > 2.0f && !rag.active[i] && mode[i] != BURN && mode[i] != ILL) {
      if (vomitTime[i] != 0.0f) {
        if (time > vomitTime[i]) {
          vomitTime[i] = time + random.range(7.0f, 16.0f) * Math2.clamp(4.0f / dose[i], 0.3f, 1.5f);
          mode[i] = ILL; stateTime[i] = random.range(1.2f, 2.2f); vx[i] = 0.0f;
        }
      } else vomitTime[i] = time + random.range(1.5f, 5.0f) * Math2.clamp(6.0f / dose[i], 0.25f, 2.0f);
    }
    if (radCollapse[i] > 0.0f && time >= radCollapse[i] && !noGetUp[i]) {
      noGetUp[i] = true; if (!rag.active[i]) startRagdoll(i, 0.0f, 10.0f, random.range(-0.6f, 0.6f));
    }
    if (radDeath[i] > 0.0f && time >= radDeath[i]) die(i, CAUSE_RADIATION);
  }

  private void updateWalking(CellGrid grid, int i, float dt, float gravity) {
    int gx = (int)(x[i] / 6.0f), groundY = (int)((y[i] + 2.0f) / 6.0f);
    onGround[i] = grid.solid(gx, groundY);
    if (!onGround[i]) vy[i] += gravity * dt;
    else if (vy[i] > 0.0f) vy[i] = 0.0f;
    float desired = 0.0f;
    if (mode[i] == FLEE || mode[i] == BURN) {
      desired = runSpeed[i] * (mode[i] == BURN ? 1.05f : 1.0f);
      if (mode[i] == FLEE && panic[i] <= 0.0f) { mode[i] = IDLE; stateTime[i] = random.range(0.5f, 2.0f); }
    } else if (mode[i] == WALK) desired = speed[i];
    if (stateTime[i] <= 0.0f) {
      if (mode[i] == ILL || mode[i] == COWER || mode[i] == GETUP) { mode[i] = IDLE; stateTime[i] = random.range(0.5f, 2.0f); }
      else if (mode[i] == FLEE || mode[i] == BURN) stateTime[i] = random.range(0.5f, 1.5f);
      else {
        if (mode[i] == WALK || random.chance(0.45f)) { mode[i] = IDLE; stateTime[i] = random.range(0.5f, 2.5f); }
        else { mode[i] = WALK; stateTime[i] = random.range(1.0f, 4.0f); }
        if (random.chance(0.35f)) direction[i] = (byte)-direction[i];
      }
    }
    if (mode[i] == ILL || mode[i] == COWER || mode[i] == GETUP) desired = 0.0f;
    vx[i] += (direction[i] * desired - vx[i]) * Math.min(1.0f, dt * 8.0f);
    if (onGround[i] && desired > 0.0f) {
      int ahead = (int)((x[i] + direction[i] * 4.0f) / 6.0f);
      if (grid.solid(ahead, (int)((y[i] - 5.0f) / 6.0f)) || !grid.solid(ahead, groundY)) {
        if (mode[i] == FLEE && grid.solid(ahead, groundY)) vy[i] = -85.0f;
        else {
          direction[i] = (byte)-direction[i]; vx[i] = 0.0f;
          if (mode[i] == FLEE) { mode[i] = COWER; stateTime[i] = random.range(2.0f, 4.0f); }
          else if (mode[i] == WALK) { mode[i] = IDLE; stateTime[i] = random.range(1.0f, 3.0f); }
        }
      }
    }
    x[i] += vx[i] * dt; y[i] += vy[i] * dt; phase[i] += dt * (2.0f + Math.abs(vx[i]) * 0.22f);
    int gy = (int)((y[i] + 2.0f) / 6.0f);
    if (grid.solid((int)(x[i] / 6.0f), gy) && vy[i] >= 0.0f) {
      float impact = vy[i]; y[i] = gy * 6.0f - 2.05f; vy[i] = 0.0f; onGround[i] = true;
      float severity = Math.max((y[i] - fallY[i]) / 15.0f, impact * impact / 900.0f / 15.0f);
      if (severity > 3.0f) hurt(i, 9.0f * (severity - 3.0f) + 10.0f * Math.max(0.0f, severity - 10.0f), 0.5f * vx[i], -20.0f, CAUSE_FALL);
      fallY[i] = y[i];
    } else if (!onGround[i]) fallY[i] = Math.min(fallY[i], y[i]);
    else fallY[i] = y[i];
    if (x[i] < 2.0f || x[i] > grid.width * 6.0f - 2.0f || y[i] > 1610.0f) remove(i);
  }

  void updateDown(CellGrid grid, int i, float dt, float gravity, float time) {
    if (state[i] == DEAD || state[i] == GONE) return;
    downTime[i] += dt;
    if (held[i] || noGetUp[i] || burnTime[i] > 0.0f || hp[i] < 25.0f || gravity <= 0.0f) return;
    float delay = 1.6f + (100.0f - hp[i]) / 30.0f;
    if (downTime[i] <= delay || (!rag.sleeping[i] && rag.still[i] <= 0.25f)) return;
    float px = rag.centerX(i); int floor = (int)Math.floor(rag.centerY(i) / 6.0f);
    for (int n = 0; n < 6 && floor < CellGrid.HEIGHT && !grid.solid((int)(px / 6.0f), floor); n++) floor++;
    float y = 6.0f * floor;
    if (!hasClearance(grid, px, y)) { downTime[i] = delay - 1.0f; return; }
    x[i] = px; this.y[i] = y; vx[i] = vy[i] = 0.0f; fallY[i] = y; onGround[i] = true;
    direction[i] = (byte)(rag.headX(i) > rag.centerX(i) ? 1 : -1);
    rag.active[i] = false; state[i] = ALIVE; mode[i] = GETUP; stateTime[i] = 0.6f;
    panic[i] = Math.max(panic[i], random.range(3.0f, 6.0f));
  }

  public int livingCount() {
    int result = 0; for (int i = 0; i < count; i++) if (state[i] < DEAD) result++; return result;
  }
  public int injuredCount() {
    int result = 0; for (int i = 0; i < count; i++) if (state[i] < DEAD && hp[i] < 99.9f) result++; return result;
  }
  public int sickCount() {
    int result = 0; for (int i = 0; i < count; i++) if (state[i] < DEAD && dose[i] >= 0.8f) result++; return result;
  }
}
