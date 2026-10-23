package wreckbox.platform;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.VolumeControl;
import wreckbox.core.GameListener;

/**
 * Original Wreckbox effects are stored as tiny AMR-NB resources and streamed,
 * rather than decoded into a heap-sized PCM bank.  MIDP implementations without
 * AMR support still get the old low-latency tone fallback.
 */
public final class SoundSynth implements GameListener, PlayerListener {
  public boolean enabled = true;
  private long lastTone, lastClip;
  private int clipCounter;
  private final Player[] players = new Player[3];
  private final long[] started = new long[3];
  private final boolean[] alarm = new boolean[3];

  public void message(String text) { }

  public synchronized void sound(String id, float volume) {
    if (!enabled) return;
    long now = System.currentTimeMillis();
    // Do not let a stream of debris/water sounds suppress an explosion or a
    // warning siren.  Only repetitive effects need the old global throttle.
    boolean repetitive = id.equals("water") || id.equals("flame") || id.equals("debris")
        || id.equals("creak") || id.equals("hit") || id.equals("splash");
    if (!repetitive || now - lastClip >= 45) {
      String file = audioFile(id);
      if (file != null) {
        lastClip = now;
        if (playClip(file, volume)) return;
      }
    }
    if (repetitive && now - lastTone < 55) return;
    lastTone = now;
    int note = 60, duration = 70;
    if (id.equals("boom")) { note = 52; duration = 260; }
    else if (id.equals("nuke") || id.equals("rumble")) { note = 40; duration = 480; }
    else if (id.equals("rocket")) { note = 72; duration = 180; }
    else if (id.equals("laser")) { note = 88; duration = 180; }
    else if (id.equals("hole")) { note = 45; duration = 350; }
    else if (id.equals("hit") || id.equals("debris")) { note = 68; duration = 90; }
    else if (id.equals("click")) { note = 82; duration = 35; }
    else if (id.equals("serviceSiren") || id.startsWith("fireSiren") || id.startsWith("policeSiren")) { note = 76; duration = 180; }
    else if (id.equals("medSiren") || id.startsWith("medSiren") || id.equals("siren2")) { note = 84; duration = 140; }
    try { Manager.playTone(note, duration, Math.max(1, Math.min(100, (int)(volume * 55)))); }
    catch (MediaException ignored) { }
    catch (RuntimeException ignored) { }
  }

  private boolean playClip(String file, float volume) {
    InputStream stream = null;
    Player player = null;
    int slot = -1;
    boolean playing = false;
    try {
      stream = getClass().getResourceAsStream("/audio/" + file + ".amr");
      if (stream == null) return false;
      for (int i = 0; i < players.length; i++) if (players[i] == null) { slot = i; break; }
      if (slot < 0) {
        // Keep an alarm audible when a blast, scream and collapsing wall all
        // arrive in the same frame.  Prefer stealing an ordinary effect slot.
        slot = -1;
        for (int i = 0; i < players.length; i++) if (!alarm[i]
            && (slot < 0 || started[i] < started[slot])) slot = i;
        if (slot < 0) {
          slot = 0;
          for (int i = 1; i < players.length; i++) if (started[i] < started[slot]) slot = i;
        }
        closeSlot(slot);
      }
      player = Manager.createPlayer(stream, "audio/amr");
      // The desktop MIDP stubs return null; a real phone returns a Player here.
      if (player == null) return false;
      player.addPlayerListener(this);
      player.realize();
      try {
        VolumeControl control = (VolumeControl)player.getControl("VolumeControl");
        if (control != null) control.setLevel(Math.max(1, Math.min(100, (int)(volume * 55))));
      } catch (RuntimeException ignored) { }
      players[slot] = player; started[slot] = System.currentTimeMillis();
      alarm[slot] = file.startsWith("siren") || file.indexOf("Siren") >= 0;
      player.start();
      playing = true;
      return true;
    } catch (IOException ignored) {
    } catch (MediaException ignored) {
    } catch (RuntimeException ignored) {
    } finally {
      if (stream != null) try { stream.close(); } catch (IOException ignored) { }
      if (player != null && !playing) {
        if (slot >= 0 && players[slot] == player) closeSlot(slot);
        else try { player.close(); } catch (RuntimeException ignored) { }
      }
    }
    return false;
  }

  private String audioFile(String id) {
    if (id.equals("boom")) return (clipCounter++ & 1) == 0 ? "boom_00" : "boom_01";
    if (id.equals("nuke")) return "boom_low_00";
    if (id.equals("rumble")) return "rumbleL";
    if (id.equals("rocket")) return "rocketL";
    if (id.equals("laser")) return "laser";
    if (id.equals("hole")) return "hole";
    if (id.equals("hit")) return "hit_soft_00";
    if (id.equals("click")) return "ui_click_00";
    if (id.equals("debris")) return "brk_concrete_00";
    if (id.equals("creak")) return "creak_00";
    if (id.equals("water")) return (clipCounter++ & 1) == 0 ? "pour_00" : "pour_01";
    if (id.equals("flame")) return "flameS";
    if (id.equals("bomber")) return "bomber_00";
    if (id.equals("siren")) return "sirenS";
    if (id.equals("reactorSiren")) return "sirenC";
    if (id.equals("serviceSiren")) return "fireSiren24";
    if (id.equals("medSiren")) return "medSiren24";
    if (id.startsWith("fireSiren") || id.startsWith("medSiren") || id.startsWith("policeSiren")) return id;
    if (id.equals("chute")) return "chute_00";
    if (id.equals("whistle")) return "bomb_whistle_00";
    if (id.equals("scream")) return (clipCounter++ & 1) == 0 ? "scream_m_00" : "scream_f_00";
    if (id.equals("groan")) return "groan_00";
    if (id.equals("thunder")) return "thunder_00";
    if (id.equals("glass")) return "glass_00";
    if (id.equals("metal")) return "brk_metal_00";
    if (id.equals("wood")) return "brk_wood_00";
    if (id.equals("brick")) return "brk_brick_00";
    if (id.equals("splash")) return "splash_small_00";
    if (id.equals("shot")) return "shot_00";
    if (id.equals("arc")) return "arc_00";
    return null;
  }

  public synchronized void playerUpdate(Player player, String event, Object data) {
    if (PlayerListener.END_OF_MEDIA.equals(event) || PlayerListener.ERROR.equals(event) || PlayerListener.CLOSED.equals(event)) {
      for (int i = 0; i < players.length; i++) if (players[i] == player) { closeSlot(i); break; }
    }
  }

  private void closeSlot(int slot) {
    Player player = players[slot]; players[slot] = null; started[slot] = 0L; alarm[slot] = false;
    if (player == null) return;
    try { player.stop(); } catch (MediaException ignored) { } catch (RuntimeException ignored) { }
    try { player.deallocate(); } catch (RuntimeException ignored) { }
    try { player.close(); } catch (RuntimeException ignored) { }
  }

  public synchronized void close() { for (int i = 0; i < players.length; i++) closeSlot(i); }
}
