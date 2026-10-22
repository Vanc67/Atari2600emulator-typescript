package wreckbox.core;

/** Support connectivity, overload cracking and conversion of detached cells to rigid chunks. */
public final class StructuralSolver {
  public static final int ECONOMY = 0, NORMAL = 1, DETAILED = 2;
  private static final int MAX_COMPONENT = 65536;
  private static final int INITIAL_COMPONENT = 4096;
  private static final int CELLS_PER_CHUNK = DebrisSystem.MAX_SHAPE_CELLS;
  private static final byte[] NO_STRESS = new byte[0];
  private byte[] queue = new byte[0];
  private final int[] histogram = new int[Materials.COUNT];
  public int collapsedCells;
  public byte[] stress = NO_STRESS;
  public int overloadedCells;
  public int chunksCreated;

  public int solve(CellGrid grid, DebrisSystem debris, float gravity) { return solve(grid, debris, gravity, false); }
  public int solve(CellGrid grid, DebrisSystem debris, float gravity, boolean showStress) {
    return solve(grid, debris, gravity, showStress, NORMAL);
  }
  public int solve(CellGrid grid, DebrisSystem debris, float gravity, boolean showStress, int physicsMode) {
    return solve(grid, debris, gravity, showStress, physicsMode, null);
  }

  /** The metadata-aware path preserves the recovered port's floating ship. */
  public int solve(CellGrid grid, DebrisSystem debris, float gravity, boolean showStress, int physicsMode, MapMetadata metadata) {
    if (queue.length < INITIAL_COMPONENT * 3) queue = new byte[INITIAL_COMPONENT * 3];
    if (showStress && stress.length != (grid.size + 3) / 4) stress = new byte[(grid.size + 3) / 4];
    if (!showStress) stress = NO_STRESS;
    else for (int i = 0; i < stress.length; i++) stress[i] = 0;
    collapsedCells = overloadedCells = chunksCreated = 0;
    grid.clearMarks();
    for (int start = 0; start < grid.size; start++) {
      int material = grid.material(start);
      if (material == 0 || Materials.FIXED[material] || material == 12 || grid.visited(start)) continue;
      int head = 0, tail = 0;
      put(tail++, start); grid.mark(start, true);
      boolean supported = false, overflow = false;
      int floatingCells = 0;
      while (head < tail) {
        int at = get(head++), x = at % grid.width, y = at / grid.width;
        int currentMaterial = grid.material(at);
        if (isFloatingHull(metadata, grid, x, y, currentMaterial)) floatingCells++;
        int up = at - grid.width, down = at + grid.width;
        if (up >= 0) {
          int m = grid.material(up);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(up)) {
            if (!ensure(tail)) { overflow = true; break; }
            grid.mark(up, true); put(tail++, up);
          }
        }
        if (down < grid.size) {
          int m = grid.material(down);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(down)) {
            if (!ensure(tail)) { overflow = true; break; }
            grid.mark(down, true); put(tail++, down);
          }
        }
        if (x > 0) {
          int n = at - 1, m = grid.material(n);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(n)) {
            if (!ensure(tail)) { overflow = true; break; }
            grid.mark(n, true); put(tail++, n);
          }
        }
        if (x + 1 < grid.width) {
          int n = at + 1, m = grid.material(n);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(n)) {
            if (!ensure(tail)) { overflow = true; break; }
            grid.mark(n, true); put(tail++, n);
          }
        }
      }
      // The original port marks hull cells as water-supported (ea/oa), not
      // as bedrock-connected. Keep at least a small hull raft afloat while
      // allowing isolated upper works and damaged fragments to fall.
      if (!supported && !overflow && tail > 0 && floatingCells < 3) detach(grid, debris, tail);
    }
    if (gravity > 0.0f && physicsMode > ECONOMY) checkOverload(grid, debris, gravity / 450.0f, showStress);
    if (gravity > 0.0f && physicsMode >= DETAILED) checkDamagedCantilevers(grid, debris);
    grid.compactRows();
    return collapsedCells + overloadedCells;
  }

  private boolean isFloatingHull(MapMetadata metadata, CellGrid grid, int x, int y, int material) {
    if (metadata == null || !metadata.sea || (material != 4 && material != 5 && material != 14 && material != 15)) return false;
    return x >= (int)metadata.riverX0 - 2 && x <= (int)metadata.riverX1 + 2
        && y * 6.0f + 6.0f >= metadata.riverWaterY && y * 6.0f <= metadata.riverBottom * 6.0f;
  }

  private boolean ensure(int index) {
    int capacity = queue.length / 3;
    if (index < capacity) return true;
    if (index >= MAX_COMPONENT) return false;
    int expandedCapacity = Math.min(MAX_COMPONENT, Math.max(index + 1, capacity * 2));
    byte[] expanded = new byte[expandedCapacity * 3];
    System.arraycopy(queue, 0, expanded, 0, queue.length);
    queue = expanded;
    return true;
  }

  public int memoryBytes() { return queue.length + stress.length + histogram.length * 4; }

  private int get(int index) {
    int at = index * 3;
    return (queue[at] & 255) | ((queue[at + 1] & 255) << 8) | ((queue[at + 2] & 255) << 16);
  }
  private void put(int index, int value) {
    int at = index * 3;
    queue[at] = (byte)value; queue[at + 1] = (byte)(value >>> 8); queue[at + 2] = (byte)(value >>> 16);
  }

  /** Four packed levels: empty, normal, loaded and overloaded. */
  public float stressAt(int index) {
    int at = index >> 2;
    if (at < 0 || at >= stress.length) return 0.0f;
    int level = (stress[at] >>> ((index & 3) * 2)) & 3;
    return level == 3 ? 2.0f : level == 2 ? 1.0f : level == 1 ? 0.5f : 0.0f;
  }
  private void setStress(int index, float ratio) {
    int level = ratio > 1.2f ? 3 : ratio > 0.75f ? 2 : ratio > 0.0f ? 1 : 0;
    int at = index >> 2, shift = (index & 3) * 2;
    stress[at] = (byte)((stress[at] & ~(3 << shift)) | (level << shift));
  }

  private void detach(CellGrid grid, DebrisSystem debris, int count) {
    for (int base = 0; base < count; base += CELLS_PER_CHUNK) {
      int end = Math.min(count, base + CELLS_PER_CHUNK);
      float sx = 0.0f, sy = 0.0f, mass = 0.0f, hp = 0.0f;
      int minX = grid.width, maxX = 0, minY = CellGrid.HEIGHT, maxY = 0, actual = 0;
      int dominant = 1, dominantCount = 0;
      for (int i = 0; i < histogram.length; i++) histogram[i] = 0;
      // First measure the original cells. The old implementation cleared them
      // in this pass and then drew one filled bounding rectangle, losing the
      // actual ship/building silhouette. Keep the mask until the body exists.
      for (int q = base; q < end; q++) {
        int at = get(q), m = grid.material(at);
        if (m == 0) continue;
        int x = at % grid.width, y = at / grid.width;
        sx += x * 6.0f + 3.0f; sy += y * 6.0f + 3.0f; actual++;
        mass += Materials.DENSITY[m]; hp += grid.health(at);
        if (++histogram[m] > dominantCount) { dominantCount = histogram[m]; dominant = m; }
        if (x < minX) minX = x; if (x > maxX) maxX = x;
        if (y < minY) minY = y; if (y > maxY) maxY = y;
      }
      if (actual <= 0) continue;
      float centerX = sx / actual, centerY = sy / actual;
      int slot = debris.spawnChunk(centerX, centerY, (maxX - minX + 1) * 6,
          (maxY - minY + 1) * 6, dominant, actual, mass, hp);
      if (slot >= 0) debris.setShapeOrigin(slot, centerX - minX * 6.0f, centerY - minY * 6.0f);
      int shapeIndex = 0;
      for (int q = base; q < end; q++) {
        int at = get(q), m = grid.material(at);
        if (m == 0) continue;
        int x = at % grid.width, y = at / grid.width;
        if (slot >= 0) debris.setShapeCell(slot, shapeIndex++, x - minX, y - minY, m, grid.tint(at));
        if ((grid.flags(at) & CellGrid.FLAG_USER) != 0) {
          grid.destroyed++; grid.damageValue += Materials.VALUES[m];
        }
        grid.clear(at); collapsedCells++;
      }
      if (slot >= 0) {
        chunksCreated++;
        debris.spawnDust(centerX, centerY, Materials.DUST[dominant], Math.min(7, 1 + actual / 5));
      }
    }
  }

  private void checkOverload(CellGrid grid, DebrisSystem debris, float gravityScale, boolean showStress) {
    for (int x = 0; x < grid.width; x++) {
      float load = 0.0f;
      for (int y = 0; y < CellGrid.HEIGHT; y++) {
        int at = y * grid.width + x, m = grid.material(at);
        if (m == 0 || m == 12) { if (showStress) setStress(at, 0.0f); load = 0.0f; continue; }
        if (Materials.FIXED[m]) { if (showStress) setStress(at, 0.0f); load = 0.0f; continue; }
        load += Materials.DENSITY[m] * gravityScale;
        float health = Math.max(0.25f, grid.healthFraction(at));
        float capacity = Materials.COMPRESSION[m] * health;
        if ((grid.flags(at) & CellGrid.FLAG_REINFORCED) != 0) capacity *= 2.0f;
        if ((grid.flags(at) & CellGrid.FLAG_JOINT) != 0) capacity *= m == 4 ? 0.45f : 0.8f;
        float ratio = (load + 0.01f) / Math.max(0.01f, capacity);
        if (showStress) setStress(at, ratio);
        if (ratio > 1.05f) {
          // Intact recovered cities are already certified by the original solver. A crack starts
          // where damage or changed support has made this local column exceed its reserve.
          if (grid.healthFraction(at) < 0.995f || ratio > 1.65f && Materials.BEARING[m]) {
            if (grid.damage(at, Materials.HP[m] * 0.12f * ratio)) {
              overloadedCells++;
              debris.spawnDust(x * 6.0f + 3.0f, y * 6.0f + 3.0f, Materials.DUST[m], 2);
            }
          }
        }
      }
    }
  }

  /** Detailed mode adds local shear on already damaged, unsupported horizontal spans. */
  private void checkDamagedCantilevers(CellGrid grid, DebrisSystem debris) {
    for (int y = 1; y < CellGrid.HEIGHT; y++) for (int x = 0; x < grid.width; x++) {
      int at = y * grid.width + x, material = grid.material(at);
      if (material == 0 || Materials.FIXED[material] || material == 12 || grid.healthFraction(at) >= 0.995f) continue;
      if (y + 1 >= CellGrid.HEIGHT || grid.material(at + grid.width) != 0) continue;
      int span = 1;
      boolean leftSupported = false, rightSupported = false;
      for (int n = 1; n <= 32 && x - n >= 0; n++) {
        int side = at - n;
        if (grid.material(side) == 0) break;
        if (grid.material(side + grid.width) != 0) { leftSupported = true; break; }
        span++;
      }
      for (int n = 1; n <= 32 && x + n < grid.width; n++) {
        int side = at + n;
        if (grid.material(side) == 0) break;
        if (grid.material(side + grid.width) != 0) { rightSupported = true; break; }
        span++;
      }
      // A beam supported at both ends is not a cantilever. The old detailed
      // path damaged every weakened beam regardless of its two real supports,
      // making intact buildings shed floors in a way the original solver does
      // not. Keep local shear only for a genuinely free end.
      if (leftSupported && rightSupported) continue;
      if (span >= 5 && grid.damage(at, Materials.HP[material] * (0.025f + 0.008f * span))) {
        overloadedCells++; debris.spawnDust(x * 6.0f + 3.0f, y * 6.0f + 3.0f, Materials.DUST[material], 2);
      }
    }
  }
}
