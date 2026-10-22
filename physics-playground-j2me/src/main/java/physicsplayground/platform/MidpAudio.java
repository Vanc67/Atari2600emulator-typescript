package physicsplayground.platform;

import java.util.Hashtable;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;
import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.Math2;
import physicsplayground.core.Random;
import physicsplayground.core.Vec2;
import physicsplayground.game.AudioSink;

/** Asynchronous MMAPI boundary with bounded request queue, cached synthesis and four reusable voice slots. */
public final class MidpAudio implements AudioSink, Runnable {
    private static final int QUEUE = 32, VOICES = 4;
    private static final class Voice { Player player; String soundId = ""; long started, ends; }
    private final Content content;
    private final Hashtable samples = new Hashtable(), lastPlayed = new Hashtable();
    private final String[] ids = new String[QUEUE]; private final float[] xs = new float[QUEUE], ys = new float[QUEUE], volumes = new float[QUEUE], distances = new float[QUEUE];
    private final Voice[] voices = new Voice[VOICES]; private final Random random = new Random(20261022);
    private int head, queued; private volatile boolean alive, enabled = true, stopRequested; private Thread thread;
    private float listenerX, listenerY; private String currentId; private float currentVolume, currentDistance;
    public volatile int playsStarted, playsSkipped, failures; public volatile String lastError = "";
    public MidpAudio(Content content) { this.content = content; for (int i = 0; i < VOICES; i++) voices[i] = new Voice(); }
    public void start() { if (alive) return; alive = true; thread = new Thread(this, "PhysicsPlayground-audio"); thread.start(); }
    public void close() { alive = false; if (thread != null) thread.interrupt(); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; if (!value) stopRequested = true; }
    public void stopAll() { stopRequested = true; }
    public void setListener(Vec2 position) { listenerX = position.x; listenerY = position.y; }
    public int activeVoiceCount() { long now = System.currentTimeMillis(); int count = 0; for (int i = 0; i < VOICES; i++) if (voices[i].player != null && voices[i].ends > now) count++; return count; }
    public void play(String id, Vec2 position, float volumeDb) {
        if (!enabled || !alive || id == null || id.length() == 0) return; Definitions.Sound sound = content.registry.sound(id); if (sound == null) { playsSkipped++; return; }
        long now = System.currentTimeMillis(); Long previous = (Long) lastPlayed.get(id);
        if (previous != null && now - previous.longValue() < (long) (sound.minInterval * 1000)) { playsSkipped++; return; }
        float dx = position.x - listenerX, dy = position.y - listenerY, distance = (float) Math.sqrt(dx * dx + dy * dy); if (distance >= 4000) { playsSkipped++; return; }
        synchronized (this) {
            if (queued == QUEUE) { playsSkipped++; return; } int slot = (head + queued) % QUEUE;
            ids[slot] = id; xs[slot] = position.x; ys[slot] = position.y; volumes[slot] = volumeDb; distances[slot] = distance; queued++; lastPlayed.put(id, new Long(now)); notify();
        }
    }
    public void run() {
        try {
            while (alive) {
                if (stopRequested) { stopRequested = false; closeVoices(); }
                if (!take()) { sleep(25); continue; }
                if (!enabled) continue; playCurrent();
            }
        } finally { closeVoices(); thread = null; }
    }
    private synchronized boolean take() {
        if (queued == 0) return false; currentId = ids[head]; currentVolume = volumes[head]; currentDistance = distances[head]; ids[head] = null; head = (head + 1) % QUEUE; queued--; return true;
    }
    private void playCurrent() {
        Definitions.Sound sound = content.registry.sound(currentId); if (sound == null) return;
        try {
            byte[] pcm = (byte[]) samples.get(sound.id); if (pcm == null) { pcm = SoundSynth.build(sound); samples.put(sound.id, pcm); }
            int index = pickVoice(sound, System.currentTimeMillis()); if (index < 0) { playsSkipped++; return; } Voice voice = voices[index]; closeVoice(voice);
            float pitch = Math.max(.05f, sound.pitch * (1 + random.range(-sound.pitchVariation, sound.pitchVariation))); int rate = Math.max(1000, (int) (22050 * pitch + .5f));
            Player player = createPlayer(pcm, rate); voice.player = player; voice.soundId = sound.id; voice.started = System.currentTimeMillis(); voice.ends = voice.started + (long) (pcm.length * 500 / rate) + 80;
            VolumeControl control = (VolumeControl) player.getControl("VolumeControl");
            if (control != null) {
                double gain = Math2.pow(10, (sound.volumeDb + currentVolume) / 20) * Math.max(0, 1 - currentDistance / 4000); control.setLevel((int) Math.max(0, Math.min(100, gain * 70)));
            }
            player.start(); playsStarted++;
        } catch (Exception error) { fail(error); } catch (NoClassDefFoundError error) { enabled = false; lastError = "MMAPI unavailable"; }
    }
    private Player createPlayer(byte[] pcm, int rate) throws Exception {
        Player player;
        try { player = Manager.createPlayer(new WavInputStream(pcm, rate), "audio/x-wav"); }
        catch (Exception first) { player = Manager.createPlayer(new WavInputStream(pcm, rate), "audio/wav"); }
        player.realize(); player.prefetch(); return player;
    }
    private int pickVoice(Definitions.Sound sound, long now) {
        int same = 0, oldestSame = -1, oldestAny = -1, idle = -1;
        for (int i = 0; i < VOICES; i++) {
            Voice voice = voices[i]; if (voice.player == null || voice.ends <= now) { if (idle < 0) idle = i; continue; }
            if (sound.id.equals(voice.soundId)) { same++; if (oldestSame < 0 || voice.started < voices[oldestSame].started) oldestSame = i; }
            if (oldestAny < 0 || voice.started < voices[oldestAny].started) oldestAny = i;
        }
        if (same >= Math.max(sound.maxInstances, 1)) return oldestSame; if (idle >= 0) return idle; return oldestAny;
    }
    private void fail(Exception error) { failures++; lastError = error.getMessage() == null ? error.toString() : error.getMessage(); if (failures >= 3) enabled = false; }
    private void closeVoices() { for (int i = 0; i < VOICES; i++) closeVoice(voices[i]); }
    private static void closeVoice(Voice voice) {
        if (voice.player != null) { try { voice.player.stop(); } catch (Exception ignored) {} try { voice.player.deallocate(); } catch (Exception ignored) {} voice.player.close(); }
        voice.player = null; voice.soundId = ""; voice.ends = 0;
    }
    private static void sleep(long time) { try { Thread.sleep(time); } catch (InterruptedException ignored) {} }
}
