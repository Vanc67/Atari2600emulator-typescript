package wreckbox.core;

/** Phone-side generator for user-placed buildings; map snapshots remain the original oracle data. */
public final class BuildingGenerator {
  private static final String[] HOME_PROPS = {"sofa", "bed", "tv", "fridge", "stove", "dtable"};
  private static final String[] WORK_PROPS = {"desk", "ochair", "filing", "rack", "counter2"};
  private static final String[] MED_PROPS = {"hbed", "medcab", "optable"};

  public int build(CellGrid grid, BodyStore bodies, int selection, int center, int ground, Random random) {
    int width = random.rangeInt(Buildings.MIN_WIDTH[selection], Buildings.MAX_WIDTH[selection]);
    int x0 = Math.max(8, Math.min(grid.width - width - 9, center - width / 2));
    int height = Buildings.HEIGHT[selection] + 8;
    for (int x = Math.max(0, x0 - 2); x < Math.min(grid.width, x0 + width + 2); x++)
      for (int y = Math.max(0, ground - height); y < ground; y++) {
        int at = grid.index(x, y);
        if (!Materials.FIXED[grid.material(at)]) grid.clear(at);
      }
    String id = Buildings.IDS[selection];
    int floors = floors(id, random), floorHeight = id.equals("factory") ? 14 : 8;
    int wall = wallMaterial(id), base = ground - 1, top = base - floors * floorHeight;
    if (id.equals("house") || id.equals("cottage")) {
      for (int y = top; y <= base; y++) for (int x = x0; x < x0 + width; x++)
        if (y == base || x == x0 || x == x0 + width - 1 || (y - top) % floorHeight == 0)
          set(grid, x, y, wall, id.equals("cottage") ? 15 : 0, 1);
      int roofY = top - 1, middle = x0 + width / 2;
      for (int x = x0 - 2; x <= x0 + width + 1; x++) {
        int rise = Math.abs(x - middle) * 7 / Math.max(1, width / 2);
        for (int y = roofY - (7 - rise); y <= roofY; y++) if (y >= 0 && (y == roofY || y == roofY - (7 - rise)))
          set(grid, x, y, 10, 0, 1);
      }
    } else if (id.equals("gas")) {
      top = base - 10;
      rect(grid, x0, top, width - 1, 1, 14, 15, 3);
      for (int x = x0 + 3; x < x0 + width - 2; x += 12) rect(grid, x, top + 1, 1, 9, 4, 6, 1);
      addProp(bodies, "fuelpump", x0 + 9, base, random);
      addProp(bodies, "fuelpump", x0 + 22, base, random);
      addProp(bodies, "fueltank", x0 + width - 12, base, random);
    } else {
      int frame = id.equals("office") || id.equals("sky") || id.equals("factory") ? 4 : wall;
      for (int f = 0; f <= floors; f++) {
        int y = base - f * floorHeight;
        rect(grid, x0, y, width, 1, id.equals("panel") ? 14 : frame, id.equals("panel") ? 15 : 0, id.equals("panel") ? 3 : 1);
      }
      for (int x = x0; x < x0 + width; x++) {
        boolean edge = x == x0 || x == x0 + width - 1;
        boolean column = edge || ((x - x0) % (id.equals("factory") ? 16 : 10) == 0);
        if (column) rect(grid, x, top, 1, base - top + 1, frame, frame == 4 ? 6 : 0, 1);
      }
      for (int f = 0; f < floors; f++) {
        int y0 = base - (f + 1) * floorHeight + 2;
        for (int x = x0 + 2; x < x0 + width - 2; x++) {
          if ((x - x0) % 6 < 3) rect(grid, x, y0, 1, Math.max(2, floorHeight - 4), 5, 0, 1);
          if (id.equals("factory") && f == 0 && (x - x0) % 12 == 0) addProp(bodies, "lathe", x, base, random);
        }
      }
      if (id.equals("panel")) for (int y = top; y <= base; y += 8) for (int x = x0; x < x0 + width; x += 10) {
        int at = grid.index(x, y); if (grid.material(at) != 0) grid.setFlags(at, grid.flags(at) | CellGrid.FLAG_JOINT);
      }
    }
    // Doors and ground-floor glazing are real cells and participate in support connectivity.
    int door = x0 + width / 2;
    for (int y = base - 5; y < base; y++) set(grid, door, y, id.equals("office") ? 5 : 1, 0, 1);
    int props = id.equals("sky") || id.equals("factory") ? 5 : 3;
    for (int i = 0; i < props; i++) {
      String prop;
      if (id.equals("hospital") || id.equals("clinic")) prop = MED_PROPS[random.nextInt(MED_PROPS.length)];
      else if (id.equals("office") || id.equals("sky")) prop = WORK_PROPS[random.nextInt(WORK_PROPS.length)];
      else if (id.equals("firestation")) prop = "firetruck";
      else if (id.equals("police")) prop = "policecar";
      else prop = HOME_PROPS[random.nextInt(HOME_PROPS.length)];
      addProp(bodies, prop, x0 + 5 + random.nextInt(Math.max(1, width - 10)), base - (i % floors) * floorHeight, random);
    }
    return x0;
  }

  private int floors(String id, Random random) {
    if (id.equals("house")) return random.rangeInt(1, 3);
    if (id.equals("cottage")) return random.rangeInt(1, 2);
    if (id.equals("shop")) return random.rangeInt(2, 3);
    if (id.equals("brick")) return random.rangeInt(3, 7);
    if (id.equals("panel")) return random.chance(0.5f) ? 5 : 9;
    if (id.equals("office")) return random.rangeInt(4, 10);
    if (id.equals("sky")) return random.rangeInt(14, 22);
    if (id.equals("factory") || id.equals("firestation") || id.equals("police")) return 2;
    if (id.equals("hospital")) return 3;
    return 1;
  }

  private int wallMaterial(String id) {
    if (id.equals("house")) return 1;
    if (id.equals("cottage") || id.equals("clinic")) return 15;
    if (id.equals("panel")) return 14;
    if (id.equals("police") || id.equals("hospital")) return 3;
    if (id.equals("office") || id.equals("sky")) return 5;
    if (id.equals("factory")) return 16;
    return 2;
  }

  private void set(CellGrid grid, int x, int y, int material, int tint, int flags) {
    if (grid.inside(x, y)) grid.set(grid.index(x, y), material, tint, flags);
  }
  private void rect(CellGrid grid, int x, int y, int w, int h, int material, int tint, int flags) {
    for (int yy = y; yy < y + h; yy++) for (int xx = x; xx < x + w; xx++) set(grid, xx, yy, material, tint, flags);
  }
  private void addProp(BodyStore bodies, String id, int cellX, int groundY, Random random) {
    int type = Props.find(id); if (type < 0) return;
    int slot = bodies.add(1, type, cellX * 6.0f + 3.0f, groundY * 6.0f - Props.HEIGHT[type] * 0.5f,
        Props.WIDTH[type], Props.HEIGHT[type], Props.MASS[type], Props.HP[type]);
    if (slot >= 0) { bodies.flags[slot] = BodyStore.F_SLEEPING; bodies.variant[slot] = (byte)random.nextInt(Math.max(1, Props.VARIANTS[type])); }
  }
}
