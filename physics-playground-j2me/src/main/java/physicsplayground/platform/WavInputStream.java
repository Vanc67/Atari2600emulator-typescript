package physicsplayground.platform;

import java.io.IOException;
import java.io.InputStream;

/** WAV header streamed before a shared PCM body; no concatenated sound buffer is allocated. */
public final class WavInputStream extends InputStream {
    private final byte[] header = new byte[44], pcm;
    private int position;
    public WavInputStream(byte[] pcm, int sampleRate) {
        this.pcm = pcm; int data = pcm.length, rate = Math.max(1000, sampleRate);
        text(0, "RIFF"); integer(4, 36 + data); text(8, "WAVE"); text(12, "fmt "); integer(16, 16); half(20, 1); half(22, 1);
        integer(24, rate); integer(28, rate * 2); half(32, 2); half(34, 16); text(36, "data"); integer(40, data);
    }
    public int read() { return position >= header.length + pcm.length ? -1 : (position < 44 ? header[position++] : pcm[position++ - 44]) & 255; }
    public int read(byte[] target, int offset, int length) throws IOException {
        if (target == null) throw new NullPointerException(); if (offset < 0 || length < 0 || offset + length > target.length) throw new IndexOutOfBoundsException();
        if (length == 0) return 0; int available = header.length + pcm.length - position; if (available <= 0) return -1; length = Math.min(length, available);
        for (int i = 0; i < length; i++) target[offset + i] = position < 44 ? header[position++] : pcm[position++ - 44]; return length;
    }
    public int available() { return header.length + pcm.length - position; }
    private void text(int at, String value) { for (int i = 0; i < 4; i++) header[at + i] = (byte) value.charAt(i); }
    private void integer(int at, int value) { header[at] = (byte) value; header[at + 1] = (byte) (value >>> 8); header[at + 2] = (byte) (value >>> 16); header[at + 3] = (byte) (value >>> 24); }
    private void half(int at, int value) { header[at] = (byte) value; header[at + 1] = (byte) (value >>> 8); }
}
