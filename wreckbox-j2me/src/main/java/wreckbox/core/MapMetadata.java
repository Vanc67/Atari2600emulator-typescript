package wreckbox.core;

public final class MapMetadata {
  public int mapIndex;
  public float riverX0, riverX1, riverBottom, riverWaterY;
  public float volcanoX, volcanoCraterY, volcanoX0, volcanoHeight;
  public float shelterEntranceX, shelterEntranceY, shelterInsideX, shelterInsideY;
  public boolean river, volcano, npp, base, shelter;
  /** Open sea, as opposed to a narrow city river; supports ship hulls. */
  public boolean sea;
}
