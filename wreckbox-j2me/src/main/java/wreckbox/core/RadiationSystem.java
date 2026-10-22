package wreckbox.core;

/** Original prompt radiation, wind-borne fallout and NPP releases in fixed storage. */
public final class RadiationSystem {
  public static final int MAX_SOURCES = 6;
  public static final float BACKGROUND = 0.000015f;
  private static final float[] SHIELD = {
    0.7f, 0.85f, 0.6f, 0.5f, 0.32f, 1.0f, 0.9f, 0.95f, 0.55f, 0.45f, 0.8f,
    0.62f, 0.5f, 0.45f, 0.5f, 0.75f, 0.8f, 0.75f, 0.04f, 0.35f, 0.5f
  };

  public int count;
  public final boolean[] npp = new boolean[MAX_SOURCES];
  public final float[] x = new float[MAX_SOURCES], t0 = new float[MAX_SOURCES];
  public final float[] L = new float[MAX_SOURCES], D0 = new float[MAX_SOURCES];
  public final float[] prompt = new float[MAX_SOURCES], Rp = new float[MAX_SOURCES];
  public final float[] core = new float[MAX_SOURCES];
  public final int[] direction = new int[MAX_SOURCES];

  public int memoryBytes() { return MAX_SOURCES * 33; }
  public void reset() { count = 0; }
  private int slot() {
    if (count < MAX_SOURCES) return count++;
    for (int i = 1; i < count; i++) {
      npp[i - 1] = npp[i]; x[i - 1] = x[i]; t0[i - 1] = t0[i]; L[i - 1] = L[i];
      D0[i - 1] = D0[i]; prompt[i - 1] = prompt[i]; Rp[i - 1] = Rp[i];
      core[i - 1] = core[i]; direction[i - 1] = direction[i];
    }
    return count - 1;
  }

  public void addNuclear(float x, float time, float kt, boolean touch, float blastRadius) {
    int i = slot();
    npp[i] = false; this.x[i] = x; t0[i] = time;
    L[i] = 1.3f * blastRadius + 260.0f;
    D0[i] = 1600.0f * Math2.pow(kt / 15.0f, 0.6f) * (touch ? 1.0f : 0.012f);
    prompt[i] = 40000.0f * Math2.sqrt(kt / 15.0f);
    Rp[i] = 1.4f * blastRadius; core[i] = 0.0f; direction[i] = 1;
  }

  public void addNppRelease(float x, float time, float amount, int windDirection) {
    int i = -1;
    for (int n = 0; n < count; n++) if (npp[n]) { i = n; break; }
    if (i < 0) {
      i = slot(); npp[i] = true; this.x[i] = x; t0[i] = time; L[i] = 2800.0f;
      D0[i] = 0.0f; prompt[i] = 0.0f; Rp[i] = 300.0f; core[i] = 0.0f;
      direction[i] = windDirection < 0 ? -1 : 1;
    }
    D0[i] = Math.min(60.0f, D0[i] + 22.0f * amount);
    core[i] = Math.min(1.0f, core[i] + 0.5f * amount);
  }

  private float sourceRate(int i, float atX, float time) {
    float age = time - t0[i];
    if (npp[i]) {
      if (age < 0.5f) return 0.0f;
      float along = (atX - x[i]) * direction[i];
      float front = Math.min(age * 22.0f, 14000.0f), spatial;
      if (along >= 0.0f) spatial = Math2.exp(-along / L[i]) * (along <= front ? 1.0f : Math2.exp(-(along - front) / 140.0f));
      else spatial = Math2.exp(along / 180.0f);
      float decay = 0.35f + 0.65f * Math2.exp(-age / 480.0f);
      float coreDistance = (atX - x[i]) / 420.0f;
      return D0[i] * Math2.clamp(age / 6.0f, 0.0f, 1.0f) * spatial * decay
          + 90.0f * core[i] * Math2.exp(-Math.abs(atX - x[i]) / 380.0f) / (1.0f + coreDistance * coreDistance);
    }
    float result = 0.0f;
    if (age < 3.0f) {
      float dx = (atX - x[i]) / Rp[i];
      result += prompt[i] * Math2.exp(-3.0f * age) / (1.0f + 4.0f * dx * dx);
    }
    result += nuclearFallout(i, atX, age);
    return result;
  }

  private float nuclearFallout(int i, float atX, float age) {
    if (age <= 0.8f) return 0.0f;
    float hours = Math.max(0.25f, age / 6.0f);
    float ramp = Math2.clamp((age - 0.8f) / 5.0f, 0.0f, 1.0f);
    float dx = atX - x[i] - 6.0f * Math.min(age, 120.0f);
    dx /= dx < 0.0f ? 0.45f * L[i] : L[i];
    return D0[i] * Math2.pow(hours, -1.2f) * ramp / (1.0f + dx * dx);
  }

  /** Bm(): accumulated resident dose excludes the separate three-second dosimeter prompt pulse. */
  public float falloutRate(float atX, float time) {
    float result = 0.0f;
    for (int i = 0; i < count; i++) result += npp[i] ? sourceRate(i, atX, time) : nuclearFallout(i, atX, time - t0[i]);
    return result;
  }
  public float falloutRate(CellGrid grid, float atX, float atY, float time) {
    // No source means no fallout dose; avoid scanning every overhead floor for
    // every resident on an otherwise quiet map.
    if (count == 0) return 0.0f;
    return falloutRate(atX, time) * shielding(grid, atX, atY);
  }

  /** Unshielded dose rate in R/h at a world x coordinate. */
  public float rate(float atX, float time) {
    float result = BACKGROUND;
    for (int i = 0; i < count; i++) result += sourceRate(i, atX, time);
    return result;
  }

  /** Original Om(): forty-one possible floors overhead and underground attenuation. */
  public float shielding(CellGrid grid, float atX, float atY) {
    float result = 1.0f;
    int gx = (int)Math.floor(atX / 6.0f), gy = (int)Math.floor(atY / 6.0f);
    if (gx >= 0 && gx < grid.width) {
      for (int y = gy - 3; y > gy - 44 && y >= 0; y--) {
        int material = grid.materialAt(gx, y);
        if (material != 0) result *= SHIELD[material];
      }
    }
    // Fm(): deep underground receives the additional shelter factor.
    if (gy >= 0 && gx >= 0 && gx < grid.width) {
      int ground = 0;
      while (ground < CellGrid.HEIGHT && !Materials.FIXED[grid.materialAt(gx, ground)]) ground++;
      if (ground < CellGrid.HEIGHT && atY > 6.0f * (ground + 2)) result *= 0.25f;
    }
    return Math.max(0.0001f, result);
  }

  public float rate(CellGrid grid, float atX, float atY, float time) {
    if (count == 0) return BACKGROUND;
    return rate(atX, time) * shielding(grid, atX, atY);
  }

  public void update(float time) {
    for (int i = count - 1; i >= 0; i--) {
      float age = time - t0[i];
      if (!npp[i] && age > 60.0f && D0[i] * Math2.pow(age / 6.0f, -1.2f) < 0.00001f) remove(i);
    }
  }
  private void remove(int index) {
    for (int i = index + 1; i < count; i++) {
      npp[i - 1] = npp[i]; x[i - 1] = x[i]; t0[i - 1] = t0[i]; L[i - 1] = L[i];
      D0[i - 1] = D0[i]; prompt[i - 1] = prompt[i]; Rp[i - 1] = Rp[i];
      core[i - 1] = core[i]; direction[i - 1] = direction[i];
    }
    count--;
  }

  /** Formatting shared with the original dosimeter (R/h, mR/h, µR/h). */
  public static String formatDose(float value) {
    if (value >= 99999.0f) return "> 99 999 Р/ч";
    if (value >= 100.0f) return Math2.round(value) + " Р/ч";
    if (value >= 1.0f) return formatOne(value) + " Р/ч";
    if (value >= 0.001f) return value < 0.01f ? formatOne(value * 1000.0f) + " мР/ч" : Math2.round(value * 1000.0f) + " мР/ч";
    return Math2.round(value * 1000000.0f) + " мкР/ч";
  }
  private static String formatOne(float value) {
    float rounded = Math2.round(value * 10.0f) / 10.0f;
    int whole = (int)rounded;
    return Math.abs(rounded - whole) < 0.001f ? String.valueOf(whole) : String.valueOf(rounded);
  }
}
