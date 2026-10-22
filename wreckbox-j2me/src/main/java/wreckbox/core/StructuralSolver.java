package wreckbox.core;

/** Support connectivity, overload cracking and conversion of detached cells to rigid chunks. */
public final class StructuralSolver {
  private static final int MAX_COMPONENT = 65536;
  private static final int CELLS_PER_CHUNK = 96;
  private final int[] queue = new int[MAX_COMPONENT];
  private final int[] histogram = new int[Materials.COUNT];
  public int collapsedCells;
  public int overloadedCells;
  public int chunksCreated;

  public int solve(CellGrid grid, DebrisSystem debris, float gravity) {
    collapsedCells = overloadedCells = chunksCreated = 0;
    grid.clearMarks();
    for (int start = 0; start < grid.size; start++) {
      int material = grid.material(start);
      if (material == 0 || Materials.FIXED[material] || material == 12 || grid.visited(start)) continue;
      int head = 0, tail = 0;
      queue[tail++] = start; grid.mark(start, true);
      boolean supported = false, overflow = false;
      while (head < tail) {
        int at = queue[head++], x = at % grid.width;
        int up = at - grid.width, down = at + grid.width;
        if (up >= 0) {
          int m = grid.material(up);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(up)) {
            if (tail >= MAX_COMPONENT) { overflow = true; break; }
            grid.mark(up, true); queue[tail++] = up;
          }
        }
        if (down < grid.size) {
          int m = grid.material(down);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(down)) {
            if (tail >= MAX_COMPONENT) { overflow = true; break; }
            grid.mark(down, true); queue[tail++] = down;
          }
        }
        if (x > 0) {
          int n = at - 1, m = grid.material(n);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(n)) {
            if (tail >= MAX_COMPONENT) { overflow = true; break; }
            grid.mark(n, true); queue[tail++] = n;
          }
        }
        if (x + 1 < grid.width) {
          int n = at + 1, m = grid.material(n);
          if (Materials.FIXED[m]) supported = true;
          else if (m != 0 && m != 12 && !grid.visited(n)) {
            if (tail >= MAX_COMPONENT) { overflow = true; break; }
            grid.mark(n, true); queue[tail++] = n;
          }
        }
      }
      if (!supported && !overflow && tail > 0) detach(grid, debris, tail);
    }
    if (gravity > 0.0f) checkOverload(grid, debris, gravity / 450.0f);
    return collapsedCells + overloadedCells;
  }

  private void detach(CellGrid grid, DebrisSystem debris, int count) {
    for (int base = 0; base < count; base += CELLS_PER_CHUNK) {
      int end = Math.min(count, base + CELLS_PER_CHUNK);
      float sx = 0.0f, sy = 0.0f, mass = 0.0f, hp = 0.0f;
      int minX = grid.width, maxX = 0, minY = CellGrid.HEIGHT, maxY = 0;
      int dominant = 1, dominantCount = 0;
      for (int i = 0; i < histogram.length; i++) histogram[i] = 0;
      for (int q = base; q < end; q++) {
        int at = queue[q], m = grid.material(at);
        if (m == 0) continue;
        int x = at % grid.width, y = at / grid.width;
        sx += x * 6.0f + 3.0f; sy += y * 6.0f + 3.0f;
        mass += Materials.DENSITY[m]; hp += grid.health(at);
        if (++histogram[m] > dominantCount) { dominantCount = histogram[m]; dominant = m; }
        if (x < minX) minX = x; if (x > maxX) maxX = x;
        if (y < minY) minY = y; if (y > maxY) maxY = y;
        if ((grid.flags(at) & CellGrid.FLAG_USER) != 0) {
          grid.destroyed++; grid.damageValue += Materials.VALUES[m];
        }
        grid.clear(at); collapsedCells++;
      }
      int actual = end - base;
      if (actual <= 0) continue;
      int slot = debris.spawnChunk(sx / actual, sy / actual, (maxX - minX + 1) * 6,
          (maxY - minY + 1) * 6, dominant, actual, mass, hp);
      if (slot >= 0) chunksCreated++;
    }
  }

  private void checkOverload(CellGrid grid, DebrisSystem debris, float gravityScale) {
    for (int x = 0; x < grid.width; x++) {
      float load = 0.0f;
      for (int y = 0; y < CellGrid.HEIGHT; y++) {
        int at = y * grid.width + x, m = grid.material(at);
        if (m == 0 || m == 12) { load = 0.0f; continue; }
        if (Materials.FIXED[m]) { load = 0.0f; continue; }
        load += Materials.DENSITY[m] * gravityScale;
        float health = Math.max(0.25f, grid.healthFraction(at));
        float capacity = Materials.COMPRESSION[m] * health;
        if ((grid.flags(at) & CellGrid.FLAG_REINFORCED) != 0) capacity *= 2.0f;
        if ((grid.flags(at) & CellGrid.FLAG_JOINT) != 0) capacity *= m == 4 ? 0.45f : 0.8f;
        float ratio = (load + 0.01f) / Math.max(0.01f, capacity);
        if (ratio > 1.05f) {
          // Intact recovered cities are already certified by the original solver. A crack starts
          // where damage or changed support has made this local column exceed its reserve.
          if (grid.healthFraction(at) < 0.995f || ratio > 1.65f) {
            if (grid.damage(at, Materials.HP[m] * 0.12f * ratio)) {
              overloadedCells++;
              debris.spawnDust(x * 6.0f + 3.0f, y * 6.0f + 3.0f, Materials.DUST[m], 2);
            }
          }
        }
      }
    }
  }
}
