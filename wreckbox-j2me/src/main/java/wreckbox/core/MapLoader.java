package wreckbox.core;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Streaming reader for deterministic WBMP/1 snapshots produced by the original game. */
public final class MapLoader {
  public final MapMetadata metadata = new MapMetadata();
  private DataInputStream in;

  public void load(InputStream stream, CellGrid grid, BodyStore bodies, PeopleStore people, PowerGrid power) throws IOException {
    in = new DataInputStream(stream);
    if (in.readUnsignedByte() != 'W' || in.readUnsignedByte() != 'B'
        || in.readUnsignedByte() != 'M' || in.readUnsignedByte() != 'P')
      throw new IOException("Not WBMP");
    int version = in.readUnsignedByte();
    if (version != 1) throw new IOException("WBMP version " + version);
    metadata.mapIndex = in.readUnsignedByte();
    int width = in.readUnsignedShort(), height = in.readUnsignedShort();
    if (width != grid.width || height != CellGrid.HEIGHT) throw new IOException("WBMP dimensions");
    int runs = in.readInt(), bodyCount = in.readUnsignedShort(), peopleCount = in.readUnsignedShort();
    int typeCount = in.readUnsignedShort(), nodes = in.readUnsignedShort(), spans = in.readUnsignedShort();
    int patches = in.readInt();
    metadata.riverX0 = in.readFloat(); metadata.riverX1 = in.readFloat();
    metadata.riverBottom = in.readFloat(); metadata.riverWaterY = in.readFloat();
    metadata.volcanoX = in.readFloat(); metadata.volcanoCraterY = in.readFloat();
    metadata.volcanoX0 = in.readFloat(); metadata.volcanoHeight = in.readFloat();
    int special = in.readInt();
    metadata.river = (special & 1) != 0; metadata.volcano = (special & 2) != 0;
    metadata.npp = (special & 4) != 0; metadata.base = (special & 8) != 0;

    int position = 0;
    grid.beginLoad(runs);
    for (int r = 0; r < runs; r++) {
      position += readVInt();
      int length = readVInt(), material = in.readUnsignedByte(), tint = in.readUnsignedByte(), flags = in.readUnsignedByte();
      if (position < 0 || position + length > grid.size) throw new IOException("WBMP run bounds");
      grid.addLoadRun(position, length, material, tint, flags);
      position += length;
    }
    grid.finishLoad();
    for (int i = 0; i < patches; i++) grid.setHealth(readVInt(), in.readFloat());

    String[] types = new String[typeCount];
    for (int i = 0; i < typeCount; i++) types[i] = readAscii();
    bodies.allocate(bodyCount + 96);
    for (int i = 0; i < bodyCount; i++) {
      int kind = in.readUnsignedByte(), typeIndex = in.readUnsignedShort();
      float x = in.readFloat(), y = in.readFloat(), angle = in.readFloat(), mass = in.readFloat();
      float hp = in.readFloat(), w = in.readFloat(), h = in.readFloat();
      int variant = in.readUnsignedByte(), flags = in.readUnsignedByte();
      int type = typeIndex < types.length && !types[typeIndex].startsWith("@") ? Props.find(types[typeIndex]) : -1;
      int slot = bodies.add(kind, type, x, y, w, h, mass, hp);
      if (slot >= 0) {
        bodies.angle[slot] = angle; bodies.variant[slot] = (byte)variant; bodies.flags[slot] = (byte)flags;
      }
    }
    people.allocate(peopleCount + 32);
    for (int i = 0; i < peopleCount; i++) {
      float x = in.readFloat(), y = in.readFloat(), hp = in.readFloat(), speed = in.readFloat(), run = in.readFloat();
      int dir = in.readUnsignedByte(); if (dir == 255) dir = -1;
      int variant = in.readUnsignedByte(), role = in.readUnsignedByte();
      people.add(x, y, hp, speed, run, dir, variant, role);
    }
    power.allocate(nodes, spans);
    for (int i = 0; i < nodes; i++) {
      power.nodeX[i] = in.readFloat(); power.nodeTop[i] = in.readFloat(); power.attachment[i] = in.readInt();
      power.nodeFlags[i] = (byte)in.readUnsignedByte();
    }
    for (int i = 0; i < spans; i++) {
      power.spanA[i] = in.readUnsignedShort(); power.spanB[i] = in.readUnsignedShort();
      power.spanType[i] = (byte)in.readUnsignedByte(); power.spanFlags[i] = (byte)in.readUnsignedByte();
    }
    power.recompute(grid);
    grid.initialUserCells = grid.countUserCells();
    in.close();
  }

  private int readVInt() throws IOException {
    int value = 0, shift = 0;
    while (true) {
      int b = in.readUnsignedByte();
      value |= (b & 127) << shift;
      if ((b & 128) == 0) return value;
      shift += 7;
      if (shift > 28) throw new IOException("WBMP varint");
    }
  }

  private String readAscii() throws IOException {
    int length = in.readUnsignedByte();
    StringBuffer out = new StringBuffer(length);
    for (int i = 0; i < length; i++) out.append((char)in.readUnsignedByte());
    return out.toString();
  }
}
