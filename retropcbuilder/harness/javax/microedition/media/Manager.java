package javax.microedition.media;

import rpc.harness.Wav;

/**
 * Desktop stand-in for the MIDP media manager.
 *
 * The JAR itself is compiled against the CLDC/MIDP stubs (where playTone is
 * a no-op); the harness needs a real one so the procedural audio can be
 * rendered to a WAV file and listened to.  This class lives only in the
 * harness build, never in the JAR.
 */
public class Manager {

    public static final String TONE_DEVICE_LOCATOR = "device://tone";

    public static Player createPlayer(javax.microedition.media.protocol.DataSource ds) {
        return null;
    }

    public static Player createPlayer(java.io.InputStream s, String t) {
        return null;
    }

    public static Player createPlayer(String loc) {
        return null;
    }

    public static void playTone(int note, int dur, int vol) {
        if (vol > 0) {
            Wav.note(note, Math.max(10, dur), vol);
        }
    }
}
