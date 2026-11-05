package psx.droid;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import psx.CdImage;

/**
 * A disc image on the SD card, read with {@link RandomAccessFile}.
 *
 * <p>Supported: plain 2048 byte ISO, 2352 byte BIN (mode 1 and mode 2) and
 * 2336 byte mode 2 images, either opened directly or through a .cue sheet.
 * The layout is detected by looking for the ISO9660 "CD001" signature at
 * sector 16 with every possible user data offset, preferring the layout whose
 * sector size divides the file size exactly.</p>
 *
 * <p>Reading goes through a tiny direct mapped sector cache because the
 * emulated CD-ROM spins through the same sectors (directory, SYSTEM.CNF)
 * over and over, and every real read costs a seek on the card.</p>
 */
public final class Disc implements CdImage {

    /** Slots in the sector cache. 64 sectors is 128 KiB of RAM at worst. */
    private static final int CACHE_SLOTS = 64;

    private RandomAccessFile file;
    private long fileSize;
    private int layout = CdImage.LAYOUT_2048;
    private int sectorCount;
    private String url = "";
    private String cueUrl;
    private String cueFile;

    private final int[] cacheKey = new int[CACHE_SLOTS];
    private final byte[][] cacheData = new byte[CACHE_SLOTS][];
    private final int[] cacheStamp = new int[CACHE_SLOTS];
    private int cacheClock;

    public boolean isOpen() { return file != null; }

    public int getLayout() { return layout; }

    public int getSectorCount() { return sectorCount; }

    public String getUrl() { return url; }

    public String getCueUrl() { return cueUrl; }

    public String getCueFile() { return cueFile; }

    /* ==================================================================== */
    /*  opening                                                             */
    /* ==================================================================== */

    public void open(File f) throws IOException {
        close();
        String name = f.getName().toLowerCase();
        if (name.endsWith(".cue")) {
            openCue(f);
        } else {
            openImage(f, -1);
        }
    }

    private void openCue(File cue) throws IOException {
        File dir = cue.getParentFile();
        String text = readText(cue, 64 * 1024);
        String currentFile = null;
        String dataFile = null;
        String firstFile = null;
        int mode = -1;
        int pos = 0;
        int len = text.length();
        while (pos < len) {
            int nl = text.indexOf('\n', pos);
            if (nl < 0) nl = len;
            String line = text.substring(pos, nl).trim();
            pos = nl + 1;
            if (line.length() == 0 || line.charAt(0) == ';') continue;
            String upper = line.toUpperCase();
            if (upper.startsWith("FILE")) {
                String nm = quoted(line);
                if (nm == null) {
                    int sp = line.indexOf(' ');
                    if (sp > 0) {
                        int sp2 = line.indexOf(' ', sp + 1);
                        nm = sp2 > sp ? line.substring(sp + 1, sp2)
                                : line.substring(sp + 1);
                    }
                }
                nm = nm == null ? null : nm.trim();
                if (nm != null && nm.length() > 0) {
                    currentFile = nm;
                    if (firstFile == null) firstFile = nm;
                }
            } else if (upper.startsWith("TRACK")) {
                int m = trackMode(upper);
                if (m >= 0) {
                    mode = m;
                    dataFile = currentFile;
                    break;
                }
            }
        }
        String want = dataFile != null ? dataFile : firstFile;
        if (want == null) throw new IOException("cue: no FILE");
        File found = findPayload(dir, want);
        if (found == null && firstFile != null) {
            found = findPayload(dir, firstFile);
            if (found != null) want = firstFile;
        }
        if (found == null) throw new IOException("cue: \u043d\u0435\u0442 \u0444\u0430\u0439\u043b\u0430 " + baseName(want));
        openImage(found, mode);
        this.cueUrl = cue.getName();
        this.cueFile = baseName(want);
    }

    private static File findPayload(File dir, String name) {
        String base = baseName(name);
        int dot = base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        String[] exts = { "", ".bin", ".BIN", ".img", ".IMG", ".iso", ".ISO" };
        for (int i = 0; i < exts.length; i++) {
            File f = new File(dir, stem + exts[i]);
            if (f.exists() && f.isFile()) return f;
        }
        return null;
    }

    private static int trackMode(String upper) {
        if (upper.indexOf("MODE1/2352") >= 0) return CdImage.LAYOUT_2352_MODE1;
        if (upper.indexOf("MODE2/2352") >= 0) return CdImage.LAYOUT_2352_MODE2;
        if (upper.indexOf("MODE2/2336") >= 0) return CdImage.LAYOUT_2336_MODE2;
        if (upper.indexOf("MODE1/2048") >= 0) return CdImage.LAYOUT_2048;
        return -1;
    }

    private static String quoted(String line) {
        int a = line.indexOf('"');
        if (a < 0) return null;
        int b = line.indexOf('"', a + 1);
        if (b <= a) return null;
        return line.substring(a + 1, b);
    }

    private static String baseName(String path) {
        int a = path.lastIndexOf('\\');
        int b = path.lastIndexOf('/');
        int cut = a > b ? a : b;
        return cut >= 0 ? path.substring(cut + 1) : path;
    }

    private static String readText(File f, int max) throws IOException {
        RandomAccessFile in = new RandomAccessFile(f, "r");
        try {
            long size = in.length();
            int n = (int) (size > 0 && size < max ? size : max);
            byte[] buf = new byte[n];
            in.readFully(buf);
            return new String(buf, "UTF-8");
        } finally {
            closeQuietly(in);
        }
    }

    private void openImage(File f, int hint) throws IOException {
        close();
        file = new RandomAccessFile(f, "r");
        fileSize = file.length();
        url = f.getName();
        layout = CdImage.LAYOUT_2048;
        if (hint >= 0) {
            layout = hint;
            if (!checkSignature()) hint = -1;
        }
        if (hint < 0) detectLayout();
        sectorCount = (int) (fileSize / sectorBytes());
        if (sectorCount <= 0) sectorCount = 1;
        clearCache();
    }

    private int sectorBytes() {
        return layout == CdImage.LAYOUT_2048 ? 2048
                : (layout == CdImage.LAYOUT_2336_MODE2 ? 2336 : 2352);
    }

    private int userDataOffset() {
        if (layout == CdImage.LAYOUT_2352_MODE1) return 16;
        if (layout == CdImage.LAYOUT_2352_MODE2) return 24;
        if (layout == CdImage.LAYOUT_2336_MODE2) return 8;
        return 0;
    }

    private void detectLayout() throws IOException {
        int[] layouts = { CdImage.LAYOUT_2048, CdImage.LAYOUT_2352_MODE1, CdImage.LAYOUT_2352_MODE2,
                          CdImage.LAYOUT_2336_MODE2 };
        for (int i = 0; i < layouts.length; i++) {
            int sb = layouts[i] == CdImage.LAYOUT_2048 ? 2048
                    : (layouts[i] == CdImage.LAYOUT_2336_MODE2 ? 2336 : 2352);
            if (fileSize % sb == 0) {
                layout = layouts[i];
                if (checkSignature()) return;
            }
        }
        for (int i = 0; i < layouts.length; i++) {
            layout = layouts[i];
            if (checkSignature()) return;
        }
        layout = CdImage.LAYOUT_2048;
    }

    private boolean checkSignature() throws IOException {
        int sb = sectorBytes();
        int off = userDataOffset();
        long pos = 16L * sb;
        if (pos + sb > fileSize) return false;
        byte[] buf = new byte[sb];
        readAt(pos, buf, sb);
        return buf[off + 1] == 'C' && buf[off + 2] == 'D'
                && buf[off + 3] == '0' && buf[off + 4] == '0'
                && buf[off + 5] == '1';
    }

    private void readAt(long pos, byte[] buf, int len) throws IOException {
        file.seek(pos);
        file.readFully(buf, 0, len);
    }

    /* ==================================================================== */
    /*  CdImage                                                             */
    /* ==================================================================== */

    public void readSector(int lba, byte[] dst, int off) throws IOException {
        int sb = sectorBytes();
        long pos = (long) lba * sb;
        if (lba < 0 || pos + sb > fileSize) {
            for (int i = 0; i < 2048; i++) dst[off + i] = 0;
            return;
        }
        byte[] sec = cachedSector(lba, pos, sb);
        System.arraycopy(sec, userDataOffset(), dst, off, 2048);
    }

    public void readRawSector(int lba, byte[] dst) throws IOException {
        int sb = sectorBytes();
        long pos = (long) lba * sb;
        if (lba < 0 || pos + sb > fileSize) {
            for (int i = 0; i < dst.length; i++) dst[i] = 0;
            return;
        }
        if (sb == 2352) {
            readAt(pos, dst, 2352);
            return;
        }
        for (int i = 0; i < dst.length; i++) dst[i] = 0;
        byte[] sec = cachedSector(lba, pos, sb);
        System.arraycopy(sec, userDataOffset(), dst, 16, 2048);
        dst[0] = 0;
        for (int i = 1; i <= 10; i++) dst[i] = (byte) 0xFF;
        dst[11] = 0;
        dst[12] = 0;
        dst[13] = 2;
        dst[14] = 0;
        dst[15] = 0;
    }

    public int readUser(int lba, int offset, byte[] dst, int off, int len)
            throws IOException {
        byte[] sec = new byte[2048];
        int done = 0;
        while (done < len) {
            readSector(lba + (offset + done) / 2048, sec, 0);
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
        closeQuietly(file);
        file = null;
        clearCache();
    }

    /* ==================================================================== */
    /*  sector cache                                                        */
    /* ==================================================================== */

    private byte[] cachedSector(int lba, long pos, int sb) throws IOException {
        int slot = (lba & 0x7FFFFFFF) % CACHE_SLOTS;
        if (cacheData[slot] != null && cacheKey[slot] == lba
                && cacheData[slot].length == sb) {
            cacheStamp[slot] = ++cacheClock;
            return cacheData[slot];
        }
        byte[] buf = new byte[sb];
        readAt(pos, buf, sb);
        cacheData[slot] = buf;
        cacheKey[slot] = lba;
        cacheStamp[slot] = ++cacheClock;
        return buf;
    }

    private void clearCache() {
        for (int i = 0; i < CACHE_SLOTS; i++) {
            cacheData[i] = null;
            cacheKey[i] = -1;
        }
    }

    private static void closeQuietly(RandomAccessFile f) {
        if (f != null) {
            try { f.close(); } catch (Throwable t) { }
        }
    }
}
