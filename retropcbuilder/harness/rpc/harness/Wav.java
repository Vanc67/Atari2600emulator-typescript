package rpc.harness;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

/**
 * Renders the procedural audio of a session into WAV files so the sound
 * engine can be listened to without a phone.  PCM 22.05 kHz mono.
 */
public final class Wav {

    private static final int RATE = 22050;
    private static final ByteArrayOutputStream BUF = new ByteArrayOutputStream();
    private static int voices;

    private Wav() {
    }

    public static synchronized void note(int midi, int ms) {
        note(midi, ms, 75);
    }

    public static synchronized void note(int midi, int ms, int vol) {
        double freq = 440.0 * Math.pow(2.0, (midi - 69) / 12.0);
        int n = Math.max(1, RATE * Math.min(ms, 800) / 1000);
        double amp = 0.34 * Math.max(1, Math.min(100, vol)) / 100.0;
        for (int i = 0; i < n; i++) {
            double t = (double) i / RATE;
            double env = Math.min(1.0, (double) i / 220.0);
            env *= Math.min(1.0, (double) (n - i) / 440.0);
            double v = Math.sin(2 * Math.PI * freq * t) * amp * env;
            BUF.write((byte) ((int) (v * 127)));
        }
        voices++;
    }

    public static synchronized void noise(int kind, int ms) {
        int n = Math.max(1, RATE * Math.min(ms, 600) / 1000);
        int lfsr = 0x7FFF;
        for (int i = 0; i < n; i++) {
            int bit = ((lfsr ^ (lfsr >> 1)) & 1);
            lfsr = (lfsr >> 1) | (bit << 14);
            double env = Math.min(1.0, (double) (n - i) / 300.0);
            int v = (lfsr & 0xFF) - 128;
            BUF.write((byte) (int) (v * 0.35 * env));
        }
        voices++;
    }

    public static synchronized void flush(String path) {
        byte[] pcm = BUF.toByteArray();
        if (pcm.length == 0) {
            System.out.println("[wav] nothing to write: " + path);
            return;
        }
        try {
            FileOutputStream out = new FileOutputStream(path);
            int dataLen = pcm.length;
            out.write("RIFF".getBytes());
            le32(out, 36 + dataLen);
            out.write("WAVEfmt ".getBytes());
            le32(out, 16);
            le16(out, 1);
            le16(out, 1);
            le32(out, RATE);
            le32(out, RATE * 2);
            le16(out, 2);
            le16(out, 16);
            out.write("data".getBytes());
            le32(out, dataLen);
            for (int i = 0; i < pcm.length; i++) {
                int s = pcm[i];
                le16(out, s);
            }
            out.close();
        } catch (Throwable t) {
            System.out.println("wav flush failed: " + t);
        }
        BUF.reset();
    }

    public static synchronized int voiceCount() {
        return voices;
    }

    private static void le32(FileOutputStream out, int v) throws java.io.IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
        out.write((v >> 16) & 0xFF);
        out.write((v >> 24) & 0xFF);
    }

    private static void le16(FileOutputStream out, int v) throws java.io.IOException {
        out.write(v & 0xFF);
        out.write((v >> 8) & 0xFF);
    }
}
