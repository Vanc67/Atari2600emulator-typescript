package wreckbox.core;

/** Projectiles, fluids and short-lived visual effects in fixed storage. */
public final class EffectSystem {
  public static final int NONE = 0, CHARGE = 1, BOMB = 2, ROCKET = 3, MOLOTOV = 4,
      METEOR = 5, LAVA = 6, WATER = 7, BLAST = 8, FLASH = 9, SMOKE = 10,
      FIRE = 11, LIGHTNING = 12, VOLCANIC = 13, BOMBER = 14;
  // These are bit flags, not decimal sentinels: 2000/1000 overlapped the
  // low-level nuclear code and made every air burst look like a ground burst.
  public static final int NUKE_CHUTE = 2048, NUKE_GROUND_BURST = 4096, BOMBER_DROPPED = 16384;
  public static final int MAX = 384;
  public final int[] type = new int[MAX], flags = new int[MAX];
  public final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
  public final float[] tx = new float[MAX], ty = new float[MAX];
  public final float[] life = new float[MAX], maxLife = new float[MAX], radius = new float[MAX], power = new float[MAX];
  private final Random random = new Random(0xeffec7);

  public void reset() { for (int i = 0; i < MAX; i++) type[i] = NONE; }
  public int memoryBytes() { return MAX * 48; }

  public int spawn(int type, float x, float y, float life) {
    for (int i = 0; i < MAX; i++) if (this.type[i] == NONE) {
      this.type[i] = type; this.x[i] = this.tx[i] = x; this.y[i] = this.ty[i] = y;
      vx[i] = vy[i] = radius[i] = power[i] = 0.0f; flags[i] = 0;
      this.life[i] = this.maxLife[i] = life;
      return i;
    }
    return -1;
  }

  public void spawnBlast(float x, float y, float radius, float duration) {
    int i = spawn(BLAST, x, y, duration);
    if (i >= 0) this.radius[i] = radius;
  }

  public void detonateCharges() {
    int delay = 0;
    for (int i = 0; i < MAX; i++) if (type[i] == CHARGE) {
      life[i] = 0.08f + 0.07f * delay++;
      flags[i] |= 1;
    }
  }

  public int chargeCount() {
    int count = 0;
    for (int i = 0; i < MAX; i++) if (type[i] == CHARGE) count++;
    return count;
  }

  public void update(Game game, float dt) {
    CellGrid grid = game.grid;
    for (int i = 0; i < MAX; i++) {
      int t = type[i];
      if (t == NONE) continue;
      life[i] -= dt;
      if (t == BLAST || t == FLASH || t == SMOKE || t == FIRE || t == LIGHTNING) {
        if (t == SMOKE || t == FIRE) { x[i] += vx[i] * dt; y[i] += vy[i] * dt; vy[i] -= 8.0f * dt; }
        if (life[i] <= 0.0f) type[i] = NONE;
        continue;
      }
      if (t == CHARGE) {
        if (life[i] <= 0.0f && (flags[i] & 1) != 0) {
          game.explode(x[i], y[i], radius[i], power[i], 0.35f);
          type[i] = NONE;
        }
        continue;
      }
      if (t == BOMBER) {
        x[i] += vx[i] * dt; y[i] += vy[i] * dt;
        boolean passed = vx[i] > 0.0f ? x[i] >= tx[i] : x[i] <= tx[i];
        if ((flags[i] & BOMBER_DROPPED) == 0 && passed) {
          int bomb = spawn(BOMB, x[i], y[i] + 10.0f, 15.0f);
          if (bomb >= 0) {
            flags[bomb] = (flags[i] & ~BOMBER_DROPPED) | NUKE_CHUTE;
            radius[bomb] = radius[i]; power[bomb] = power[i]; ty[bomb] = ty[i];
            vx[bomb] = vx[i] * 0.16f; vy[bomb] = 38.0f;
            flags[i] |= BOMBER_DROPPED;
            game.sound("chute", 0.45f); game.sound("whistle", 0.35f);
          } else life[i] = 0.0f;
        }
        if (life[i] <= 0.0f || x[i] < -500.0f || x[i] > grid.width * 6.0f + 500.0f) type[i] = NONE;
        continue;
      }
      if (t == BOMB || t == METEOR || t == VOLCANIC || t == MOLOTOV || t == LAVA || t == WATER) {
        boolean chute = t == BOMB && (flags[i] & NUKE_CHUTE) != 0;
        vy[i] += game.gravity * dt * (t == WATER ? 0.6f : chute ? 0.12f : 1.0f);
        if (chute) {
          vx[i] *= 1.0f - Math.min(0.8f, dt * 1.4f);
          if (vy[i] > 68.0f) vy[i] = 68.0f;
        }
      }
      x[i] += vx[i] * dt; y[i] += vy[i] * dt;
      boolean impact = grid.solidPixel(x[i], y[i]);
      if (t == WATER) {
        game.douse(x[i], y[i], 18.0f);
        if (impact || life[i] <= 0.0f) {
          game.fluids.add(grid, x[i], y[i], 220);
          game.sound("splash", 0.18f);
          type[i] = NONE; continue;
        }
      } else if (t == LAVA) {
        game.fire.igniteArea(grid, game.bodies, x[i], y[i], 8.0f);
        if (impact || life[i] <= 0.0f) {
          int gx = (int)(x[i] / 6.0f), gy = (int)(y[i] / 6.0f) - 1;
          if (grid.inside(gx, gy) && grid.materialAt(gx, gy) == 0) grid.set(grid.index(gx, gy), 12, 0, 0);
          type[i] = NONE; game.structuralDirty = true; continue;
        }
      } else {
        boolean nuclearAirburst = t == BOMB && (flags[i] & 255) >= 100 && (flags[i] & NUKE_GROUND_BURST) == 0;
        if ((impact && !nuclearAirburst) || life[i] <= 0.0f || (t == BOMB && y[i] >= ty[i])) {
          if (t == MOLOTOV) {
            game.fire.igniteArea(grid, game.bodies, x[i], y[i], radius[i]);
            game.scarePeople(x[i], y[i], radius[i] * 1.5f, 4.0f);
            game.spawnFireVisual(x[i], y[i], radius[i]);
          } else if (t == BOMB && (flags[i] & 255) >= 100) {
            int level = Math.max(1, Math.min(10, (flags[i] & 255) - 100));
            game.nuclearExplode(x[i], y[i], Game.NUKE_KT[level - 1], radius[i], power[i], 0.2f, (flags[i] & NUKE_GROUND_BURST) != 0);
          } else {
            float multiplier = t == METEOR || t == VOLCANIC ? 1.35f : 1.0f;
            game.explode(x[i], y[i], radius[i], power[i] * multiplier, t == METEOR ? 0.5f : 0.2f);
          }
          type[i] = NONE;
          continue;
        }
      }
      if (x[i] < -100.0f || x[i] > grid.width * 6.0f + 100.0f || y[i] > 1620.0f) type[i] = NONE;
      if (t == ROCKET && random.chance(0.7f)) {
        int smoke = spawn(SMOKE, x[i], y[i], 0.5f);
        if (smoke >= 0) { vx[smoke] = -vx[i] * 0.05f; vy[smoke] = -vy[i] * 0.05f; radius[smoke] = 3.0f; }
      }
    }
  }
}
