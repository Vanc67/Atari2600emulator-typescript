package wreckbox.platform;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.SoundPool;
import java.io.IOException;
import java.util.HashMap;
import wreckbox.core.GameListener;

/** Lazy SoundPool playback of the recovered AMR effects, safe on API 8+. */
public final class SoundSynth implements GameListener, SoundPool.OnLoadCompleteListener {
  private static AssetManager assets;
  public boolean enabled = true;
  private long lastTone, lastClip;
  private int clipCounter;
  private SoundPool pool;
  private final HashMap samples = new HashMap(), pending = new HashMap();

  private static final class Pending {
    final String file; float volume;
    Pending(String file, float volume) { this.file = file; this.volume = volume; }
  }
  public static void configure(AssetManager manager) { assets = manager; }
  public void message(String text) { }

  public synchronized void sound(String id, float volume) {
    if (!enabled) return;
    long now = System.currentTimeMillis();
    boolean repetitive = id.equals("water") || id.equals("flame") || id.equals("debris")
        || id.equals("creak") || id.equals("hit") || id.equals("splash");
    if (repetitive && now - lastClip < 45) return;
    String file = audioFile(id);
    if (file == null) return;
    lastClip = now;
    playClip(file, volume);
    lastTone = now;
  }

  private boolean ensurePool() {
    if (pool != null) return true;
    if (assets == null) return false;
    try {
      pool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
      pool.setOnLoadCompleteListener(this);
      return true;
    } catch (RuntimeException unavailable) { pool = null; return false; }
  }
  private boolean playClip(String file, float volume) {
    if (!ensurePool()) return false;
    Integer cached = (Integer)samples.get(file);
    if (cached != null) {
      Pending loading = (Pending)pending.get(cached);
      if (loading != null) { loading.volume = volume; return true; }
      return playSample(cached.intValue(), volume);
    }
    AssetFileDescriptor descriptor = null;
    try {
      descriptor = assets.openFd("audio/" + file + ".amr");
      int sample = pool.load(descriptor, 1);
      if (sample == 0) return false;
      Integer key = new Integer(sample);
      samples.put(file, key); pending.put(key, new Pending(file, volume));
      return true;
    } catch (IOException unavailable) {
      return false;
    } catch (RuntimeException unavailable) {
      return false;
    } finally {
      if (descriptor != null) try { descriptor.close(); } catch (IOException ignored) { }
    }
  }
  private boolean playSample(int sample, float volume) {
    try {
      float level = Math.max(0.0f, Math.min(1.0f, volume * 0.55f));
      return pool.play(sample, level, level, 1, 0, 1.0f) != 0;
    } catch (RuntimeException unavailable) { return false; }
  }
  public synchronized void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
    if (soundPool != pool) return;
    Integer key = new Integer(sampleId);
    Pending request = (Pending)pending.remove(key);
    if (request == null) return;
    if (status == 0) playSample(sampleId, request.volume);
    else { samples.remove(request.file); try { pool.unload(sampleId); } catch (RuntimeException ignored) { } }
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

  public synchronized void close() {
    if (pool != null) {
      try { pool.setOnLoadCompleteListener(null); pool.release(); }
      catch (RuntimeException ignored) { }
    }
    pool = null; samples.clear(); pending.clear();
  }
}
