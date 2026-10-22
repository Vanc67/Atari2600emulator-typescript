package wreckbox.platform;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import javax.microedition.rms.RecordStore;
import wreckbox.core.Game;

/** Original-style persistent preferences, not an artificial progression system. */
public final class SettingsStore {
  private static final String NAME = "wreckbox18.settings";
  public boolean sound = true;

  public void load(Game game, SoundSynth synth) {
    RecordStore store = null;
    try {
      store = RecordStore.openRecordStore(NAME, true);
      if (store.getNumRecords() > 0) {
        byte[] data = store.getRecord(1);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        int version = in.readUnsignedByte();
        if (version == 1 || version == 2 || version == 3) {
          int map = in.readUnsignedByte(), tool = in.readUnsignedByte(), gravity = in.readUnsignedByte();
          game.night = in.readBoolean(); game.slowMotion = in.readBoolean(); sound = in.readBoolean();
          if (version >= 2) {
            game.bloodEnabled = in.readBoolean(); game.servicesEnabled = in.readBoolean();
            game.epicSlow = in.readBoolean(); game.showToasts = in.readBoolean(); game.showStress = in.readBoolean();
          }
          if (version >= 3) {
            int physics = in.readUnsignedByte(); if (physics < 3) game.physicsMode = physics;
            game.effectsEnabled = in.readBoolean();
          }
          synth.enabled = sound;
          if (gravity < 3) { game.gravityIndex = gravity; game.gravity = Game.GRAVITIES[gravity]; }
          game.selectTool(tool);
          game.loadMap(map);
        }
        in.close();
      }
    } catch (Exception ignored) {
    } finally { close(store); }
  }

  public void save(Game game) {
    RecordStore store = null;
    try {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream out = new DataOutputStream(bytes);
      out.writeByte(3); out.writeByte(game.mapIndex); out.writeByte(game.toolIndex); out.writeByte(game.gravityIndex);
      out.writeBoolean(game.night); out.writeBoolean(game.slowMotion); out.writeBoolean(sound);
      out.writeBoolean(game.bloodEnabled); out.writeBoolean(game.servicesEnabled); out.writeBoolean(game.epicSlow);
      out.writeBoolean(game.showToasts); out.writeBoolean(game.showStress); out.writeByte(game.physicsMode);
      out.writeBoolean(game.effectsEnabled); out.flush();
      byte[] data = bytes.toByteArray();
      store = RecordStore.openRecordStore(NAME, true);
      if (store.getNumRecords() == 0) store.addRecord(data, 0, data.length);
      else store.setRecord(1, data, 0, data.length);
      out.close();
    } catch (Exception ignored) {
    } finally { close(store); }
  }

  private void close(RecordStore store) { if (store != null) try { store.closeRecordStore(); } catch (Exception ignored) {} }
}
