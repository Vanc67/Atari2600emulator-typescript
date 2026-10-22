package wreckbox.core;

/**
 * Wreckbox 6 px cell grid with lossless compressed WBMP runs and copy-on-write rows.
 * HP-only changes use short overlays; material/flag changes expand a row, and empty rows release it.
 * Traversal marks use a small bit set instead of forcing every compressed row to expand.
 */
public final class CellGrid {
  public static final int HEIGHT = 260;
  public static final int CELL_PIXELS = 6;
  public static final int FLAG_USER = 1;
  public static final int FLAG_REINFORCED = 2;
  public static final int FLAG_DETACHED = 4;
  public static final int FLAG_VERTICAL = 16;
  public static final int FLAG_JOINT = 32;
  private static final int MATERIAL_MASK = 31;
  private static final int HP_MASK = 8191;
  private static final int FLAGS_MASK = 255;
  private static final int TINT_MASK = 31;
  private static final int[] NO_INTS = new int[0];
  private static final int[] EMPTY_ROW = new int[0];

  public final int width;
  public final int size;
  private final int[][] rows = new int[HEIGHT][];
  /* 0 = direct row, 2 = row-local (start,value) runs. */
  private final byte[] rowFormat = new byte[HEIGHT];
  private boolean rowsDirty;
  private final short[][] healthRows = new short[HEIGHT][];
  /* 0 = direct HP row, 2 = sorted (cell,hp+1) pairs. */
  private final byte[] healthFormat = new byte[HEIGHT];
  private boolean healthDirty;
  private final short[] healthUsed = new short[HEIGHT];
  private final short[] rowSolid = new short[HEIGHT];
  private final int[] rowRun = new int[HEIGHT + 1];
  private int[] runStart = NO_INTS, runLength = NO_INTS, runValue = NO_INTS;
  private int runCount, lastRun = -1;
  private byte[] marks;
  public int destroyed;
  /** Live lava count lets the idle simulation skip full-map cooling scans. */
  public int lavaCells;
  public int damageValue;
  public int initialUserCells;

  public CellGrid(int width) {
    this.width = width;
    this.size = width * HEIGHT;
  }

  public static int pack(int material, int hp16, int tint, int flags) {
    if (hp16 > HP_MASK) hp16 = HP_MASK;
    return (material & MATERIAL_MASK) | ((hp16 & HP_MASK) << 5)
        | ((flags & FLAGS_MASK) << 18) | ((tint & TINT_MASK) << 26);
  }

  /** Starts a streaming, read-only baseline. Runs must arrive in ascending cell order. */
  public void beginLoad(int runs) {
    runStart = new int[runs]; runLength = new int[runs]; runValue = new int[runs];
    runCount = 0; lastRun = -1; lavaCells = 0;
  }

  /** Adds one solid WBMP run without expanding its cells into the Java heap. */
  public void addLoadRun(int position, int length, int material, int tint, int flags) {
    if (length <= 0 || material == 0) return;
    int at = runCount++;
    runStart[at] = position; runLength[at] = length;
    int hp = material == 9 ? HP_MASK : (int)(Materials.HP[material] * 16.0f + 0.5f);
    runValue[at] = pack(material, hp, tint, flags);
    if (material == 12) lavaCells += length;
    int remaining = length, cell = position;
    while (remaining > 0) {
      int y = cell / width, x = cell - y * width;
      int part = Math.min(remaining, width - x);
      rowSolid[y] += part;
      cell += part; remaining -= part;
    }
  }

  public void finishLoad() {
    int run = 0;
    for (int y = 0; y < HEIGHT; y++) {
      int start = y * width;
      while (run < runCount && runStart[run] + runLength[run] <= start) run++;
      rowRun[y] = run;
    }
    rowRun[HEIGHT] = runCount;
  }

  public int index(int x, int y) { return y * width + x; }
  public boolean inside(int x, int y) { return x >= 0 && y >= 0 && x < width && y < HEIGHT; }
  public int material(int index) { return value(index) & MATERIAL_MASK; }
  public int materialAt(int x, int y) {
    if (y >= HEIGHT) return 9;
    return inside(x, y) ? valueAt(x, y) & MATERIAL_MASK : 0;
  }
  public int packed(int index) { return value(index); }
  /** Row/column access for the renderer and collision code: avoids a second
   * integer division after the caller has already calculated the row. */
  public int packedAt(int x, int y) { return valueAt(x, y); }
  public int hp16(int index) { return (value(index) >>> 5) & HP_MASK; }
  public float health(int index) { return hp16(index) / 16.0f; }
  public int flags(int index) { return (value(index) >>> 18) & FLAGS_MASK; }
  public int tint(int index) { return (value(index) >>> 26) & TINT_MASK; }
  public boolean fixed(int index) { return Materials.FIXED[material(index)]; }
  public boolean solid(int x, int y) { return materialAt(x, y) != 0; }
  public boolean solidPixel(float x, float y) {
    return solid((int)Math.floor(x / 6.0f), (int)Math.floor(y / 6.0f));
  }

  public void set(int index, int material, int tint, int flags) {
    int hp = material == 9 ? HP_MASK : (int)(Materials.HP[material] * 16.0f + 0.5f);
    write(index, pack(material, hp, tint, flags));
  }

  public void clear(int index) { write(index, 0); }

  public void setFlags(int index, int flags) {
    int old = value(index);
    write(index, (old & ~(FLAGS_MASK << 18)) | ((flags & 255) << 18));
  }

  public void setHealth(int index, float health) {
    int hp = (int)(health * 16.0f + 0.5f);
    if (hp < 0) hp = 0;
    if (hp > HP_MASK) hp = HP_MASK;
    int old = value(index);
    write(index, (old & ~(HP_MASK << 5)) | (hp << 5));
  }

  /** Applies original material damage multiplier and panel-joint weakness. */
  public boolean damage(int index, float amount) {
    int oldValue = value(index), material = oldValue & MATERIAL_MASK;
    if (material == 0 || Materials.INDESTRUCTIBLE[material]) return false;
    float multiplier = Materials.DAMAGE_MULTIPLIER[material];
    if (((oldValue >>> 18) & FLAG_JOINT) != 0 && material != 4) multiplier *= 1.6f;
    int old = (oldValue >>> 5) & HP_MASK;
    int loss = (int)(amount * multiplier * 16.0f);
    if (loss < 1 && amount > 0.0f) loss = 1;
    int next = old - loss;
    if (next > 0) {
      write(index, (oldValue & ~(HP_MASK << 5)) | (next << 5));
      return false;
    }
    if (((oldValue >>> 18) & FLAG_USER) != 0) {
      destroyed++;
      damageValue += Materials.VALUES[material];
    }
    clear(index);
    return true;
  }

  public boolean visited(int index) {
    return marks != null && (marks[index >> 3] & (1 << (index & 7))) != 0;
  }
  public void mark(int index, boolean value) {
    if (value) {
      if (marks == null) marks = new byte[(size + 7) >> 3];
      marks[index >> 3] |= 1 << (index & 7);
    } else if (marks != null) marks[index >> 3] &= ~(1 << (index & 7));
  }
  public void clearMarks() {
    if (marks != null) for (int i = 0; i < marks.length; i++) marks[i] = 0;
  }

  public int countSolid() {
    int count = 0;
    for (int i = 0; i < HEIGHT; i++) count += rowSolid[i];
    return count;
  }

  public int countUserCells() {
    int count = 0;
    for (int i = 0; i < size; i++) {
      int value = value(i);
      if ((value & MATERIAL_MASK) != 0 && ((value >>> 18) & FLAG_USER) != 0) count++;
    }
    return count;
  }

  public float healthFraction(int index) {
    int material = material(index);
    return material == 0 ? 0.0f : health(index) / Materials.HP[material];
  }

  /** Approximate primitive array payload, excluding JVM object headers. */
  public int memoryBytes() {
    int bytes = runStart.length * 12 + HEIGHT * 18 + 4 + (marks == null ? 0 : marks.length);
    for (int i = 0; i < HEIGHT; i++) {
      if (rows[i] != null && rows[i].length != 0) bytes += rows[i].length * 4;
      if (healthRows[i] != null) bytes += healthRows[i].length * 2;
    }
    return bytes;
  }

  private int value(int index) {
    int y = index / width, x = index - y * width;
    return valueAt(x, y);
  }

  private int valueAt(int x, int y) {
    int[] row = rows[y];
    if (row != null) {
      if (rowFormat[y] == 2) return encodedValue(row, x);
      return row.length == 0 ? 0 : row[x];
    }
    int index = y * width + x;
    int value = baselineValue(index, y);
    short[] health = healthRows[y];
    if (health != null) {
      int encoded = healthFormat[y] == 2 ? sparseHealthAt(y, x) : health[x];
      if (encoded != 0) value = (value & ~(HP_MASK << 5)) | ((encoded - 1) << 5);
    }
    return value;
  }

  private int baselineValue(int index) { return baselineValue(index, index / width); }

  private int baselineValue(int index, int y) {
    if (runCount == 0 || index < runStart[0] || index >= runStart[runCount - 1] + runLength[runCount - 1]) return 0;
    int cached = lastRun;
    if (cached >= 0 && cached < runCount && index >= runStart[cached]) {
      if (index < runStart[cached] + runLength[cached]) return runValue[cached];
      if (cached + 1 >= runCount || index < runStart[cached + 1]) return 0;
    }
    int low = rowRun[y], high = Math.min(runCount - 1, rowRun[y + 1]);
    while (low <= high) {
      int middle = (low + high) >>> 1, start = runStart[middle];
      if (index < start) high = middle - 1;
      else if (index >= start + runLength[middle]) low = middle + 1;
      else {
        lastRun = middle;
        return runValue[middle];
      }
    }
    lastRun = high;
    return 0;
  }

  private void write(int index, int newValue) {
    int y = index / width, x = index - y * width;
    int oldValue = valueAt(x, y);
    if (oldValue == newValue) return;
    if (rows[y] == null && (oldValue & MATERIAL_MASK) != 0 && ((oldValue ^ newValue) & ~(HP_MASK << 5)) == 0) {
      writeHealth(index, y, x, newValue);
      return;
    }
    int[] row = rows[y];
    if (row == null) {
      row = new int[width];
      for (int i = 0; i < width; i++) row[i] = valueAt(i, y);
      rows[y] = row;
    } else if (rowFormat[y] == 2) {
      int[] expanded = new int[width];
      for (int i = 0; i < width; i++) expanded[i] = encodedValue(row, i);
      row = expanded; rows[y] = row; rowFormat[y] = 0;
    } else if (row.length == 0) {
      row = new int[width];
      rows[y] = row;
    }
    healthRows[y] = null; healthFormat[y] = 0; healthUsed[y] = 0;
    row[x] = newValue;
    rowsDirty = true;
    int oldMaterial = oldValue & MATERIAL_MASK, newMaterial = newValue & MATERIAL_MASK;
    if (oldMaterial == 12 && newMaterial != 12) lavaCells--;
    else if (oldMaterial != 12 && newMaterial == 12) lavaCells++;
    if (oldMaterial == 0 && newMaterial != 0) rowSolid[y]++;
    else if (oldMaterial != 0 && newMaterial == 0) rowSolid[y]--;
    if (rowSolid[y] == 0) { rows[y] = EMPTY_ROW; rowFormat[y] = 0; }
  }

  /** Recompresses changed direct rows after a destruction pass. This keeps
   * the lossless WBMP representation instead of retaining one int[width]
   * allocation for every scratched row. No cell data or HP is discarded. */
  public void compactRows() {
    if (!rowsDirty && !healthDirty) return;
    for (int y = 0; y < HEIGHT; y++) {
      int[] row = rows[y];
      if (row == null || row.length == 0 || rowFormat[y] == 2) continue;
      int runs = 1;
      for (int x = 1; x < row.length; x++) if (row[x] != row[x - 1]) runs++;
      if (runs * 2 >= row.length) continue;
      int[] encoded = new int[runs * 2];
      int run = 0, start = 0;
      for (int x = 1; x <= row.length; x++) {
        if (x == row.length || row[x] != row[start]) {
          encoded[run * 2] = start; encoded[run * 2 + 1] = row[start];
          run++; start = x;
        }
      }
      rows[y] = encoded; rowFormat[y] = 2;
    }
    for (int y = 0; y < HEIGHT; y++) {
      short[] full = healthRows[y];
      int count = healthUsed[y];
      if (full == null || healthFormat[y] == 2 || count <= 0 || count * 4 >= width * 2) continue;
      short[] sparse = new short[count * 2]; int at = 0;
      for (int x = 0; x < width; x++) if (full[x] != 0) {
        sparse[at * 2] = (short)x; sparse[at * 2 + 1] = full[x]; at++;
      }
      healthRows[y] = sparse; healthFormat[y] = 2;
    }
    rowsDirty = false; healthDirty = false;
  }

  private int encodedValue(int[] row, int x) {
    int low = 0, high = (row.length >>> 1) - 1;
    while (low < high) {
      int middle = (low + high + 1) >>> 1;
      if (row[middle * 2] <= x) low = middle;
      else high = middle - 1;
    }
    return row[low * 2 + 1];
  }

  private void writeHealth(int index, int y, int x, int newValue) {
    int hp = (newValue >>> 5) & HP_MASK, baselineHp = (baselineValue(index, y) >>> 5) & HP_MASK;
    int encoded = hp == baselineHp ? 0 : hp + 1;
    short[] health = healthRows[y];
    if (health == null) {
      if (encoded == 0) return;
      int capacity = Math.min(width, 32);
      health = new short[Math.max(1, capacity) * 2];
      healthRows[y] = health; healthFormat[y] = 2; healthUsed[y] = 0;
    }
    if (healthFormat[y] == 2) writeSparseHealth(y, x, encoded);
    else writeFullHealth(y, x, encoded);
  }

  private int sparseHealthAt(int y, int x) {
    short[] row = healthRows[y]; int count = healthUsed[y];
    if (row == null || count == 0 || x < row[0] || x > row[(count - 1) * 2]) return 0;
    int low = 0, high = count - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1, cell = row[middle * 2];
      if (cell < x) low = middle + 1;
      else if (cell > x) high = middle - 1;
      else return row[middle * 2 + 1];
    }
    return 0;
  }

  private void writeSparseHealth(int y, int x, int encoded) {
    short[] row = healthRows[y]; int count = healthUsed[y];
    int low = 0, high = count - 1, found = -1;
    while (low <= high) {
      int middle = (low + high) >>> 1, cell = row[middle * 2];
      if (cell < x) low = middle + 1;
      else if (cell > x) high = middle - 1;
      else { found = middle; break; }
    }
    if (found >= 0) {
      int at = found * 2, old = row[at + 1];
      if (old == encoded) return;
      if (encoded != 0) row[at + 1] = (short)encoded;
      else {
        for (int i = found + 1; i < count; i++) {
          row[(i - 1) * 2] = row[i * 2]; row[(i - 1) * 2 + 1] = row[i * 2 + 1];
        }
        count--; healthUsed[y] = (short)count;
        if (count == 0) { healthRows[y] = null; healthFormat[y] = 0; }
      }
      healthDirty = true;
      return;
    }
    if (encoded == 0) return;
    // Four bytes per sparse entry versus two for a direct HP row. Promote
    // dense damage instead of allowing sparse storage to become a pessimization.
    if (count + 1 >= (width + 1) / 2) {
      promoteHealth(y);
      writeFullHealth(y, x, encoded);
      return;
    }
    int capacity = row.length >>> 1;
    if (count >= capacity) {
      int expandedCapacity = Math.min((width - 1) / 2, Math.max(count + 1, capacity * 2));
      if (expandedCapacity <= capacity) {
        promoteHealth(y); writeFullHealth(y, x, encoded); return;
      }
      short[] expanded = new short[expandedCapacity * 2];
      System.arraycopy(row, 0, expanded, 0, count * 2);
      row = expanded; healthRows[y] = row;
    }
    for (int i = count; i > low; i--) {
      row[i * 2] = row[(i - 1) * 2]; row[i * 2 + 1] = row[(i - 1) * 2 + 1];
    }
    row[low * 2] = (short)x; row[low * 2 + 1] = (short)encoded;
    healthUsed[y] = (short)(count + 1); healthDirty = true;
  }

  private void promoteHealth(int y) {
    short[] sparse = healthRows[y], full = new short[width];
    int count = healthUsed[y];
    for (int i = 0; i < count; i++) full[sparse[i * 2]] = sparse[i * 2 + 1];
    healthRows[y] = full; healthFormat[y] = 0;
  }

  private void writeFullHealth(int y, int x, int encoded) {
    short[] health = healthRows[y]; int old = health[x];
    if (old == encoded) return;
    health[x] = (short)encoded;
    if (old == 0) healthUsed[y]++;
    else if (encoded == 0) healthUsed[y]--;
    if (healthUsed[y] == 0) { healthRows[y] = null; healthFormat[y] = 0; }
    healthDirty = true;
  }
}
