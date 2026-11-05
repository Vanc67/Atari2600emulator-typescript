package psx;

import java.io.IOException;
import java.io.InputStream;
import java.io.DataInputStream;
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
public final class VirtualCD implements CdImage {

    public static final int LAYOUT_2048 = 0;
    public static final int LAYOUT_2352_MODE1 = 1;
    public static final int LAYOUT_2352_MODE2 = 2;
    public static final int LAYOUT_2336_MODE2 = 3;

    private String url;
    private String cueUrl;          /* .cue sheet that was used, if any */
    private String cueFile;         /* payload named inside the sheet   */
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

    /** Opens an ISO/BIN/IMG file or a .cue sheet. */
    public void open(String fileUrl) throws IOException {
        if (isCue(fileUrl)) {
            openCue(fileUrl);
        } else {
            openImage(fileUrl, -1);
        }
    }

    private static boolean isCue(String u) {
        return u != null && u.toLowerCase().endsWith(".cue");
    }

    /** The .cue sheet that was parsed, or null when a plain image was opened. */
    public String getCueUrl() { return cueUrl; }

    /** The payload file named inside the .cue sheet, or null. */
    public String getCueFile() { return cueFile; }

    /* ==================================================================== */
    /*  CUE sheet support                                                   */
    /* ==================================================================== */

    /**
     * Parses a CUE sheet and opens the payload of its first data track.
     *
     * <p>Only what a PlayStation disc needs is supported: {@code FILE} lines
     * and the {@code TRACK nn MODE1/2352|MODE2/2352|MODE1/2048|MODE2/2336}
     * that follows. Everything else (indexes, pregaps, ISRC) is ignored.</p>
     */
    private void openCue(String cueUrl) throws IOException {
        String dir = parentOf(cueUrl);
        String text = readText(cueUrl, 64 * 1024);
        String currentFile = null;      /* the FILE the following TRACKs belong to */
        String dataFile = null;         /* FILE of the first data track            */
        String firstFile = null;        /* fallback: the first FILE of the sheet   */
        int mode = -1;
        String[] lines = splitLines(text);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.charAt(0) == ';') continue;
            String upper = line.toUpperCase();
            if (upper.startsWith("FILE")) {
                String name = quoted(line);
                if (name == null) {
                    /* FILE name BINARY (unquoted) */
                    int sp = line.indexOf(' ');
                    if (sp > 0) {
                        int sp2 = line.indexOf(' ', sp + 1);
                        name = sp2 > sp ? line.substring(sp + 1, sp2) : line.substring(sp + 1);
                    }
                }
                if (name != null && name.length() > 0) {
                    currentFile = name;
                    if (firstFile == null) firstFile = name;
                }
            } else if (upper.startsWith("TRACK")) {
                int m = trackMode(upper);
                if (m >= 0) {
                    /* a mixed mode sheet often starts with a CD-DA track:
                       take the file the data track belongs to, not the first */
                    mode = m;
                    dataFile = currentFile;
                    break;
                }
            }
        }
        String fileName = dataFile != null ? dataFile : firstFile;
        if (fileName == null) throw new IOException("cue: no FILE");

        String found = findPayload(dir, fileName);
        if (found == null && firstFile != null && !firstFile.equals(fileName)) {
            /* the sheet names a payload that is not here: last chance */
            found = findPayload(dir, firstFile);
            if (found != null) fileName = firstFile;
        }
        if (found == null) {
            throw new IOException("cue: нет файла " + baseName(fileName));
        }
        openImage(found, mode);
        this.cueUrl = cueUrl;
        this.cueFile = baseName(fileName);
    }

    /** Looks for the payload next to the sheet, trying the usual extensions. */
    private static String findPayload(String dir, String name) {
        String payload = dir + baseName(name);
        String[] alt = { payload, swapExt(payload, ".bin"), swapExt(payload, ".BIN"),
                         swapExt(payload, ".img"), swapExt(payload, ".iso"),
                         swapExt(payload, ".IMG"), swapExt(payload, ".ISO") };
        for (int i = 0; i < alt.length; i++) {
            if (exists(alt[i])) return alt[i];
        }
        return null;
    }

    private static String[] splitLines(String s) {
        int count = 1;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '\n') count++;
        String[] out = new String[count];
        int n = 0, start = 0;
        for (int i = 0; i <= s.length(); i++) {
            if (i == s.length() || s.charAt(i) == '\n') {
                out[n++] = s.substring(start, i);
                start = i + 1;
            }
        }
        return out;
    }

    /** Text inside the first pair of double quotes, or null. */
    private static String quoted(String line) {
        int a = line.indexOf('"');
        if (a < 0) return null;
        int b = line.indexOf('"', a + 1);
        if (b <= a) return null;
        return line.substring(a + 1, b);
    }

    /** Bitmap of a CUE track mode, encoded as VirtualCD layout, or -1. */
    private static int trackMode(String upper) {
        if (upper.indexOf("MODE1/2352") >= 0) return LAYOUT_2352_MODE1;
        if (upper.indexOf("MODE2/2352") >= 0) return LAYOUT_2352_MODE2;
        if (upper.indexOf("MODE2/2336") >= 0) return LAYOUT_2336_MODE2;
        if (upper.indexOf("MODE1/2048") >= 0) return LAYOUT_2048;
        return -1;
    }

    /** Last path element; CUE sheets often carry Windows absolute paths. */
    private static String baseName(String path) {
        int a = path.lastIndexOf('\\');
        int b = path.lastIndexOf('/');
        int cut = a > b ? a : b;
        return cut >= 0 ? path.substring(cut + 1) : path;
    }

    private static String parentOf(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(0, slash + 1) : url;
    }

    private static String swapExt(String name, String ext) {
        int dot = name.lastIndexOf('.');
        int slash = name.lastIndexOf('/');
        if (dot <= slash) return name + ext;
        return name.substring(0, dot) + ext;
    }

    private static boolean exists(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            return fc.exists();
        } catch (Throwable t) {
            return false;
        } finally {
            closeQuietly(fc);
        }
    }

    private static void closeQuietly(FileConnection fc) {
        if (fc != null) {
            try { fc.close(); } catch (Throwable t) { }
        }
    }

    private static String readText(String url, int max) throws IOException {
        FileConnection fc = (FileConnection) Connector.open(url, Connector.READ);
        DataInputStream in = null;
        try {
            in = fc.openDataInputStream();
            long size = fc.fileSize();
            int n = (int) (size > 0 && size < max ? size : max);
            byte[] buf = new byte[n];
            int got = 0;
            while (got < n) {
                int r = in.read(buf, got, n - got);
                if (r <= 0) break;
                got += r;
            }
            return new String(buf, 0, got);
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable t) { }
            }
            closeQuietly(fc);
        }
    }

    /**
     * Opens a plain image file.
     *
     * @param hint layout suggested by a CUE sheet, or -1 to auto-detect
     */
    private void openImage(String fileUrl, int hint) throws IOException {
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
        if (hint >= 0) {
            layout = hint;
            if (!checkSignature(new byte[2352])) hint = -1;
        }
        if (hint < 0) detectLayout();
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
        cueUrl = null;
        cueFile = null;
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
