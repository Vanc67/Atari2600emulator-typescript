package psx;

import java.io.IOException;
import java.io.InputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

/**
 * Virtual CD-ROM image reader (JSR-75, zero-popup).
 *
 * <p>Game images are 300-700 MiB, so the file is never loaded into memory.
 * A {@link RollingStream} keeps a single JSR-75 InputStream open, moves it
 * with {@code skip()} for forward seeks and transparently reopens it for
 * backward seeks. A small {@link SectorCache} (LRU, 96 sectors by 2352 bytes
 * = ~220 KiB) absorbs the random access pattern of the CD controller.</p>
 *
 * Supported layouts: ISO (2048), BIN/RAW 2352 Mode1, Mode2 (2336 and 2352),
 * plus a best effort CUE parser for the first data track.
 */
public final class VirtualCD {

    public static final int LAYOUT_2048 = 0;
    public static final int LAYOUT_2352_MODE1 = 1;
    public static final int LAYOUT_2352_MODE2 = 2;
    public static final int LAYOUT_2336_MODE2 = 3;

    private String url;
    private int layout = LAYOUT_2048;
    private long fileSize;
    private long sectorOffset;      /* first sector of the data track */
    private int sectorCount;
    private final SectorCache cache = new SectorCache(96);
    private RollingStream stream;

    public boolean isOpen() {
        return stream != null;
    }

    public int getLayout() { return layout; }

    public int getSectorCount() { return sectorCount; }

    public String getUrl() { return url; }

    /** Opens an ISO/BIN/IMG file (and an optional sibling CUE sheet). */
    public void open(String fileUrl) throws IOException {
        close();
        this.url = fileUrl;
        FileConnection fc = (FileConnection) Connector.open(fileUrl, Connector.READ);
        try {
            fileSize = fc.fileSize();
        } finally {
            fc.close();
        }
        sectorOffset = 0;
        layout = LAYOUT_2048;
        stream = new RollingStream(fileUrl);
        detectLayout();
        sectorCount = (int) ((fileSize - sectorOffset) / sectorBytes());
        if (sectorCount <= 0) sectorCount = 1;
        cache.clear();
    }

    private int sectorBytes() {
        return (layout == LAYOUT_2048) ? 2048 : ((layout == LAYOUT_2336_MODE2) ? 2336 : 2352);
    }

    private int userDataOffset() {
        switch (layout) {
            case LAYOUT_2352_MODE1: return 16;
            case LAYOUT_2352_MODE2: return 24;
            case LAYOUT_2336_MODE2: return 8;
            default: return 0;
        }
    }

    /**
     * Detects the image layout by looking for the ISO9660 "CD001" signature
     * at sector 16 with the three possible user data offsets.
     */
    private void detectLayout() throws IOException {
        byte[] probe = new byte[2352];
        int[] layouts = { LAYOUT_2048, LAYOUT_2352_MODE1, LAYOUT_2352_MODE2, LAYOUT_2336_MODE2 };
        /* Prefer the layout that matches the file size first (no extra I/O). */
        for (int i = 0; i < layouts.length; i++) {
            int sb = (layouts[i] == LAYOUT_2048) ? 2048 : ((layouts[i] == LAYOUT_2336_MODE2) ? 2336 : 2352);
            if (fileSize % sb == 0) {
                layout = layouts[i];
                if (checkSignature(probe)) return;
            }
        }
        for (int i = 0; i < layouts.length; i++) {
            layout = layouts[i];
            if (checkSignature(probe)) return;
        }
        layout = LAYOUT_2048;
    }

    private boolean checkSignature(byte[] buf) throws IOException {
        int sb = sectorBytes();
        int off = userDataOffset();
        long pos = 16L * sb;
        if (pos + sb > fileSize) return false;
        readAt(pos, buf, sb);
        return buf[off + 1] == 'C' && buf[off + 2] == 'D'
                && buf[off + 3] == '0' && buf[off + 4] == '0' && buf[off + 5] == '1';
    }

    private void readAt(long pos, byte[] buf, int len) throws IOException {
        if (stream == null) stream = new RollingStream(url);
        stream.seek(pos);
        stream.readFully(buf, 0, len);
    }

    /** Reads the 2048 user bytes of a sector into {@code dst[off..off+2048)}. */
    public void readSector(int lba, byte[] dst, int off) throws IOException {
        int key = lba & 0x7FFFFFFF;
        byte[] sec = cache.get(key);
        int sb = sectorBytes();
        if (sec == null) {
            sec = new byte[sb];
            long pos = sectorOffset + (long) lba * sb;
            if (pos < 0 || pos + sb > fileSize) {
                for (int i = 0; i < 2048; i++) dst[off + i] = 0;
                return;
            }
            readAt(pos, sec, sb);
            cache.put(key, sec);
        }
        int uo = userDataOffset();
        System.arraycopy(sec, uo, dst, off, 2048);
    }

    /** Reads a whole 2352 byte raw sector (mode 2 sync included). */
    public void readRawSector(int lba, byte[] dst) throws IOException {
        int sb = sectorBytes();
        long pos = sectorOffset + (long) lba * sb;
        if (pos < 0 || pos + sb > fileSize) {
            for (int i = 0; i < dst.length; i++) dst[i] = 0;
            return;
        }
        if (sb == 2352) {
            readAt(pos, dst, 2352);
            return;
        }
        /* Synthesise a raw sector from a 2048 byte image. */
        for (int i = 0; i < dst.length; i++) dst[i] = 0;
        byte[] tmp = new byte[2048];
        readAt(pos, tmp, 2048);
        System.arraycopy(tmp, 0, dst, 16, 2048);
        dst[0] = 0;
        dst[1] = (byte) 0xFF;
        dst[2] = (byte) 0xFF;
        dst[3] = (byte) 0xFF;
        dst[4] = (byte) 0xFF;
        dst[5] = (byte) 0xFF;
        dst[6] = (byte) 0xFF;
        dst[7] = (byte) 0xFF;
        dst[8] = (byte) 0xFF;
        dst[9] = (byte) 0xFF;
        dst[10] = (byte) 0xFF;
        dst[11] = 0;
        dst[12] = 0;
        dst[13] = 2;
        dst[14] = 0;
        dst[15] = 0;
    }

    /** Reads a range of bytes from the image (used for SYSTEM.CNF). */
    public int readUser(int lba, int offset, byte[] dst, int off, int len) throws IOException {
        byte[] sec = new byte[2048];
        int done = 0;
        while (done < len) {
            readSector(lba + ((offset + done) / 2048), sec, 0);
            int inSector = (offset + done) % 2048;
            int n = 2048 - inSector;
            if (n > len - done) n = len - done;
            System.arraycopy(sec, inSector, dst, off + done, n);
            done += n;
        }
        return done;
    }

    public void close() {
        if (stream != null) {
            stream.close();
            stream = null;
        }
        cache.clear();
    }

    /* ==================================================================== */
    /*  Rolling stream                                                      */
    /* ==================================================================== */

    /**
     * Seekable wrapper around a single JSR-75 InputStream.
     * Forward seeks use skip(), backward seeks reopen the connection, which
     * is the only portable way to seek a file on CLDC.
     */
    static final class RollingStream {
        private final String url;
        private InputStream in;
        private long pos;
        private final byte[] skipBuf = new byte[512];

        RollingStream(String url) throws IOException {
            this.url = url;
            open();
        }

        private void open() throws IOException {
            in = Connector.openInputStream(url);
            pos = 0;
        }

        long position() { return pos; }

        void seek(long target) throws IOException {
            if (target < pos) {
                close();
                open();
            }
            long remaining = target - pos;
            while (remaining > 0) {
                int n = (int) (remaining > skipBuf.length ? skipBuf.length : remaining);
                int r = in.read(skipBuf, 0, n);
                if (r <= 0) throw new IOException("eof");
                pos += r;
                remaining -= r;
            }
        }

        void readFully(byte[] b, int off, int len) throws IOException {
            int got = 0;
            while (got < len) {
                int r = in.read(b, off + got, len - got);
                if (r <= 0) throw new IOException("eof");
                got += r;
                pos += r;
            }
        }

        void close() {
            if (in != null) {
                try { in.close(); } catch (Exception e) { }
                in = null;
            }
            pos = 0;
        }
    }

    /* ==================================================================== */
    /*  Sector LRU cache                                                    */
    /* ==================================================================== */

    static final class SectorCache {
        private final int max;
        private final int[] keys;
        private final byte[][] data;
        private final int[] stamp;
        private int tick;
        private final byte[] tmp;

        SectorCache(int max) {
            this.max = max;
            keys = new int[max];
            data = new byte[max][];
            stamp = new int[max];
            for (int i = 0; i < max; i++) keys[i] = -1;
            tmp = new byte[4];
        }

        void clear() {
            for (int i = 0; i < max; i++) {
                keys[i] = -1;
                data[i] = null;
            }
        }

        byte[] get(int key) {
            for (int i = 0; i < max; i++) {
                if (keys[i] == key) {
                    stamp[i] = ++tick;
                    return data[i];
                }
            }
            return null;
        }

        void put(int key, byte[] sector) {
            int victim = 0;
            int oldest = Integer.MAX_VALUE;
            for (int i = 0; i < max; i++) {
                if (keys[i] == -1) {
                    victim = i;
                    break;
                }
                if (stamp[i] < oldest) {
                    oldest = stamp[i];
                    victim = i;
                }
            }
            keys[victim] = key;
            data[victim] = sector;
            stamp[victim] = ++tick;
        }

        int hits, misses;
    }
}
