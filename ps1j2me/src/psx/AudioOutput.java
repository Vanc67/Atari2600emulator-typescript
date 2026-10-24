package psx;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connection;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.protocol.ContentDescriptor;
import javax.microedition.media.protocol.DataSource;
import javax.microedition.media.protocol.SourceStream;

/**
 * Streams 44.1 kHz / 16 bit / stereo PCM from the SPU ring buffer into the
 * MIDP audio system using a custom DataSource. If the device has no audio
 * support the whole thing degrades to silence without breaking emulation.
 */
public final class AudioOutput {

    private final Spu spu;
    private Player player;
    private volatile boolean running;
    private volatile boolean starting;
    public boolean available;

    /**
     * True when {@link #start()} is still inside Manager/Player. Some devices
     * block for a very long time there, so the UI never waits for audio.
     */
    public boolean isStarting() {
        return starting;
    }

    public boolean isRunning() {
        return running;
    }

    public AudioOutput(Spu spu) {
        this.spu = spu;
    }

    public void start() {
        if (running || starting) return;
        starting = true;
        try {
            DataSource ds = new PcmSource(spu);
            player = Manager.createPlayer(ds);
            player.realize();
            player.prefetch();
            player.setLoopCount(-1);
            player.start();
            running = true;
            available = true;
        } catch (Throwable t) {
            running = false;
            available = false;
        } finally {
            starting = false;
        }
    }

    public void stop() {
        if (player != null) {
            try { player.stop(); } catch (Throwable t) { }
            try { player.deallocate(); } catch (Throwable t) { }
            try { player.close(); } catch (Throwable t) { }
            player = null;
        }
        running = false;
    }

    /** Applies the user volume setting to the SPU master volume. */
    public void setVolume(int percent) {
        spu.setMasterVolume(percent);
    }

    public void setMuted(boolean muted) {
        spu.enabled = !muted;
        if (muted) spu.flush();
    }

    /* ==================================================================== */
    /*  Custom DataSource                                                   */
    /* ==================================================================== */

    static final class PcmSource extends DataSource {
        private final PcmStream stream;
        private boolean connected;

        PcmSource(Spu spu) {
            stream = new PcmStream(spu);
        }

        public void connect() { connected = true; }

        public void disconnect() { stream.close(); }

        public void start() { stream.active = true; }

        public void stop() { stream.active = false; }

        public String getContentType() { return "audio/x-wav"; }

        public SourceStream[] getStreams() {
            SourceStream[] s = new SourceStream[1];
            s[0] = stream;
            return s;
        }

        public javax.microedition.media.Control getControl(String type) { return null; }

        public javax.microedition.media.Control[] getControls() { return new javax.microedition.media.Control[0]; }
    }

    static final class PcmStream implements SourceStream {
        private final Spu spu;
        private final byte[] header;
        private int headerPos;
        private long totalWritten;
        volatile boolean active = true;

        PcmStream(Spu spu) {
            this.spu = spu;
            header = makeWavHeader();
        }

        private static byte[] makeWavHeader() {
            byte[] h = new byte[44];
            int dataSize = 0x7FFFF000;
            putAscii(h, 0, "RIFF");
            put32(h, 4, 36 + dataSize);
            putAscii(h, 8, "WAVE");
            putAscii(h, 12, "fmt ");
            put32(h, 16, 16);
            put16(h, 20, 1);            /* PCM */
            put16(h, 22, 2);            /* stereo */
            put32(h, 24, 44100);
            put32(h, 28, 44100 * 4);
            put16(h, 32, 4);
            put16(h, 34, 16);
            putAscii(h, 36, "data");
            put32(h, 40, dataSize);
            return h;
        }

        public ContentDescriptor getContentDescriptor() {
            return new ContentDescriptor("audio/x-wav");
        }

        /* -1 = unknown: never report a huge length for an endless stream */
        public long getContentLength() { return -1L; }

        public int getSeekType() { return NOT_SEEKABLE; }

        public static final int NOT_SEEKABLE = 0;

        public int seek(long where) { return 0; }

        public long tell() { return totalWritten; }

        public int read(byte[] b, int off, int len) throws IOException {
            int n = 0;
            while (headerPos < header.length && n < len) {
                b[off + n++] = header[headerPos++];
            }
            if (n >= len) return n;
            int got = spu.drain(b, off + n, len - n);
            n += got;
            if (n == 0) {
                /* no audio yet: hand back a little silence instead of blocking */
                int pad = len > 64 ? 64 : len;
                for (int i = 0; i < pad; i++) b[off + i] = 0;
                n = pad;
            }
            totalWritten += n;
            return n;
        }

        public InputStream openInputStream() { return new InputStream() {
            public int read() throws IOException { byte[] one = new byte[1]; int n = PcmStream.this.read(one, 0, 1); return n <= 0 ? -1 : (one[0] & 0xFF); }
            public int read(byte[] b, int o, int l) throws IOException { return PcmStream.this.read(b, o, l); }
        }; }

        public java.io.DataInputStream openDataInputStream() throws IOException {
            return new java.io.DataInputStream(openInputStream());
        }

        public void close() { active = false; }

        private static void putAscii(byte[] b, int o, String s) {
            for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i);
        }

        private static void put32(byte[] b, int o, int v) {
            b[o] = (byte) v;
            b[o + 1] = (byte) (v >>> 8);
            b[o + 2] = (byte) (v >>> 16);
            b[o + 3] = (byte) (v >>> 24);
        }

        private static void put16(byte[] b, int o, int v) {
            b[o] = (byte) v;
            b[o + 1] = (byte) (v >>> 8);
        }
    }
}
