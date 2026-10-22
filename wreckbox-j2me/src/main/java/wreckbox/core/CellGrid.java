package wreckbox.core;

/**
 * Wreckbox 6 px cell grid. One int stores material, 1/16 HP, flags and tint.
 * Bit 31 is a transient traversal mark, avoiding full-size scratch arrays.
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
  private static final int VISITED = 0x80000000;

  public final int width;
  public final int size;
  public final int[] cells;
  public int destroyed;
  public int damageValue;
  public int initialUserCells;

  public CellGrid(int width) {
    this.width = width;
    this.size = width * HEIGHT;
    this.cells = new int[size];
  }

  public static int pack(int material, int hp16, int tint, int flags) {
    if (hp16 > HP_MASK) hp16 = HP_MASK;
    return (material & MATERIAL_MASK) | ((hp16 & HP_MASK) << 5)
        | ((flags & FLAGS_MASK) << 18) | ((tint & TINT_MASK) << 26);
  }

  public int index(int x, int y) { return y * width + x; }
  public boolean inside(int x, int y) { return x >= 0 && y >= 0 && x < width && y < HEIGHT; }
  public int material(int index) { return cells[index] & MATERIAL_MASK; }
  public int materialAt(int x, int y) {
    if (y >= HEIGHT) return 9;
    return inside(x, y) ? cells[y * width + x] & MATERIAL_MASK : 0;
  }
  public int hp16(int index) { return (cells[index] >>> 5) & HP_MASK; }
  public float health(int index) { return hp16(index) / 16.0f; }
  public int flags(int index) { return (cells[index] >>> 18) & FLAGS_MASK; }
  public int tint(int index) { return (cells[index] >>> 26) & TINT_MASK; }
  public boolean fixed(int index) { return Materials.FIXED[material(index)]; }
  public boolean solid(int x, int y) { return materialAt(x, y) != 0; }
  public boolean solidPixel(float x, float y) {
    return solid((int)Math.floor(x / 6.0f), (int)Math.floor(y / 6.0f));
  }

  public void set(int index, int material, int tint, int flags) {
    int hp = material == 9 ? HP_MASK : (int)(Materials.HP[material] * 16.0f + 0.5f);
    cells[index] = pack(material, hp, tint, flags);
  }

  public void clear(int index) { cells[index] = 0; }

  public void setFlags(int index, int flags) {
    cells[index] = (cells[index] & ~(FLAGS_MASK << 18)) | ((flags & 255) << 18);
  }

  public void setHealth(int index, float health) {
    int hp = (int)(health * 16.0f + 0.5f);
    if (hp < 0) hp = 0;
    if (hp > HP_MASK) hp = HP_MASK;
    cells[index] = (cells[index] & ~(HP_MASK << 5)) | (hp << 5);
  }

  /** Applies original material damage multiplier and panel-joint weakness. */
  public boolean damage(int index, float amount) {
    int material = material(index);
    if (material == 0 || Materials.INDESTRUCTIBLE[material]) return false;
    float multiplier = Materials.DAMAGE_MULTIPLIER[material];
    if ((flags(index) & FLAG_JOINT) != 0 && material != 4) multiplier *= 1.6f;
    int old = hp16(index);
    int loss = (int)(amount * multiplier * 16.0f);
    if (loss < 1 && amount > 0.0f) loss = 1;
    int next = old - loss;
    if (next > 0) {
      cells[index] = (cells[index] & ~(HP_MASK << 5)) | (next << 5);
      return false;
    }
    if ((flags(index) & FLAG_USER) != 0) {
      destroyed++;
      damageValue += Materials.VALUES[material];
    }
    clear(index);
    return true;
  }

  public boolean visited(int index) { return (cells[index] & VISITED) != 0; }
  public void mark(int index, boolean value) {
    if (value) cells[index] |= VISITED;
    else cells[index] &= ~VISITED;
  }
  public void clearMarks() { for (int i = 0; i < size; i++) cells[i] &= ~VISITED; }

  public int countSolid() {
    int count = 0;
    for (int i = 0; i < size; i++) if ((cells[i] & MATERIAL_MASK) != 0) count++;
    return count;
  }

  public int countUserCells() {
    int count = 0;
    for (int i = 0; i < size; i++)
      if ((cells[i] & MATERIAL_MASK) != 0 && (flags(i) & FLAG_USER) != 0) count++;
    return count;
  }

  public float healthFraction(int index) {
    int material = material(index);
    return material == 0 ? 0.0f : health(index) / Materials.HP[material];
  }
}
