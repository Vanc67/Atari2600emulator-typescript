package rdl.platform;

import javax.microedition.media.Manager;

/** Rate-limited MMAPI tones; audio failure never stops the simulation. */
public final class Sound {
    private static volatile boolean enabled = true;
    private static long lastTone;

    private Sound() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static void spark() {
        tone(720, 35, 24);
    }

    public static void blast() {
        tone(75, 90, 42);
    }

    private static void tone(int note, int duration, int volume) {
        if (!enabled) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastTone < 100) {
            return;
        }
        lastTone = now;
        try {
            Manager.playTone(note, duration, volume);
        } catch (Exception ignored) {
            enabled = false;
        } catch (NoClassDefFoundError ignored) {
            enabled = false;
        }
    }
}
