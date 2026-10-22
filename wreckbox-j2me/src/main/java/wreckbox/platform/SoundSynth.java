package wreckbox.platform;

import javax.microedition.media.Manager;
import javax.microedition.media.MediaException;
import wreckbox.core.GameListener;

/** Low-latency MIDP tone fallback; recovered original MP3 assets stay outside the phone JAR. */
public final class SoundSynth implements GameListener {
  public boolean enabled = true;
  private long lastTone;
  public void message(String text) { }
  public void sound(String id, float volume) {
    if (!enabled) return;
    long now = System.currentTimeMillis();
    if (now - lastTone < 35) return;
    lastTone = now;
    int note = 60, duration = 70;
    if (id.equals("boom")) { note = 52; duration = 260; }
    else if (id.equals("nuke") || id.equals("rumble")) { note = 40; duration = 480; }
    else if (id.equals("rocket")) { note = 72; duration = 180; }
    else if (id.equals("laser")) { note = 88; duration = 180; }
    else if (id.equals("hole")) { note = 45; duration = 350; }
    else if (id.equals("hit")) { note = 68; duration = 90; }
    else if (id.equals("click")) { note = 82; duration = 35; }
    try { Manager.playTone(note, duration, Math.max(1, Math.min(100, (int)(volume * 55)))); }
    catch (MediaException ignored) { }
    catch (RuntimeException ignored) { }
  }
}
