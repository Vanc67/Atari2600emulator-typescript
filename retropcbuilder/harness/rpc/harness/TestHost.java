package rpc.harness;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;

import rpc.Host;

/** {@link Host} for the desktop harness: reads the jar resources from data/. */
public final class TestHost implements Host {

    private final String dataDir;
    private String save;

    public TestHost(String dataDir) {
        this.dataDir = dataDir;
    }

    public InputStream open(String name) {
        try {
            return new FileInputStream(dataDir + "/" + name);
        } catch (Throwable t) {
            return null;
        }
    }

    public String load(String key) {
        return save;
    }

    public void save(String key, String value) {
        save = value;
    }

    public void vibrate(int ms) {
    }

    public void exitApp() {
    }

    public long now() {
        return System.currentTimeMillis();
    }

    /** Tone calls are rendered to a wav file so the sound can be checked. */
    public void tone(int note, int duration) {
        Wav.note(note, duration);
    }

    public void noise(int kind, int duration) {
        Wav.noise(kind, duration);
    }

    public String textInput(String title, String text, int maxSize) {
        return text;
    }

    /** Reads a resource fully; used by tools/tests. */
    public static byte[] readAll(InputStream in) {
        if (in == null) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        } catch (Throwable t) {
            return null;
        }
    }
}
