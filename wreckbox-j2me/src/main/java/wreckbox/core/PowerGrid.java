package wreckbox.core;

/** Recovered power poles/transformers and span graph, disconnected by real damage. */
public final class PowerGrid {
  public int nodeCount, spanCount;
  public float[] nodeX, nodeTop;
  public int[] attachment;
  public byte[] nodeFlags;
  public int[] spanA, spanB;
  public byte[] spanType, spanFlags;
  public boolean[] powered;

  public void allocate(int nodes, int spans) {
    nodeCount = nodes; spanCount = spans;
    nodeX = new float[nodes]; nodeTop = new float[nodes]; attachment = new int[nodes]; nodeFlags = new byte[nodes];
    spanA = new int[spans]; spanB = new int[spans]; spanType = new byte[spans]; spanFlags = new byte[spans];
    powered = new boolean[nodes];
  }

  public int memoryBytes() { return nodeCount * 14 + spanCount * 10; }

  public void recompute(CellGrid grid) {
    for (int i = 0; i < nodeCount; i++) {
      int at = attachment[i];
      if (at >= 0 && at < grid.size && grid.material(at) == 0) nodeFlags[i] &= ~1;
      powered[i] = (nodeFlags[i] & 4) != 0 && (nodeFlags[i] & 1) != 0;
    }
    for (int pass = 0; pass < nodeCount; pass++) {
      boolean changed = false;
      for (int i = 0; i < spanCount; i++) {
        int a = spanA[i], b = spanB[i];
        if (a < 0 || b < 0 || a >= nodeCount || b >= nodeCount || (spanFlags[i] & 1) == 0) continue;
        if (powered[a] && !powered[b] && (nodeFlags[b] & 1) != 0) { powered[b] = true; changed = true; }
        if (powered[b] && !powered[a] && (nodeFlags[a] & 1) != 0) { powered[a] = true; changed = true; }
      }
      if (!changed) break;
    }
  }

  public int damageArea(CellGrid grid, float x, float y, float radius) {
    int cut = 0;
    float r2 = radius * radius;
    for (int i = 0; i < nodeCount; i++) {
      float dx = nodeX[i] * 6.0f - x, dy = nodeTop[i] * 6.0f - y;
      if (dx * dx + dy * dy < r2 && (nodeFlags[i] & 1) != 0) {
        nodeFlags[i] &= ~1; cut++;
      }
    }
    if (cut > 0) recompute(grid);
    return cut;
  }
}
