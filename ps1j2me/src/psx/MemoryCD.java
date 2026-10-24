package psx;

import java.io.IOException;
import java.io.InputStream;

/**
 * CD image kept entirely in memory (used for the demo discs embedded in the
 * JAR, a few dozen KiB each). Same interface as {@link VirtualCD}.
 */
public final class MemoryCD implements CdImage {

    private final byte[] data;
    private final String name;
    private int layout = VirtualCD.LAYOUT_2048;
    private int sectorCount;
    private boolean open = true;

    public MemoryCD(byte[] image, String name) {
        this.data = image;
        this.name = name;
        detect();
    }

    /** Loads an image from a JAR resource; returns null if missing. */
    public static MemoryCD fromResource(Class anchor, String resource, String name) {
        try {
            InputStream in = anchor.getResourceAsStream(resource);
            if (in == null) return null;
            byte[] buf = new byte[64 * 1024];
            int len = 0;
            for (;;) {
                if (len == buf.length) {
                    byte[] bigger = new byte[buf.length * 2];
                    System.arraycopy(buf, 0, bigger, 0, len);
                    buf = bigger;
                }
                int n = in.read(buf, len, buf.length - len);
                if (n <= 0) break;
                len += n;
            }
            in.close();
            if (len <= 0) return null;
            byte[] img = new byte[len];
            System.arraycopy(buf, 0, img, 0, len);
            return new MemoryCD(img, name);
        } catch (Throwable t) {
            return null;
        }
    }

    private void detect() {
        int size = data.length;
        if (size >= 16 * 2352 && check(16 * 2352 + 24)) {
            layout = VirtualCD.LAYOUT_2352_MODE2;
        } else if (size >= 16 * 2352 && check(16 * 2352 + 16)) {
            layout = VirtualCD.LAYOUT_2352_MODE1;
        } else if (size >= 16 * 2336 && check(16 * 2336 + 8)) {
            layout = VirtualCD.LAYOUT_2336_MODE2;
        } else {
            layout = VirtualCD.LAYOUT_2048;
        }
        int sb = sectorBytes();
        sectorCount = Math.max(1, size / sb);
    }

    private boolean check(int off) {
        return off + 6 <= data.length && data[off + 1] == 'C' && data[off + 2] == 'D'
                && data[off + 3] == '0' && data[off + 4] == '0' && data[off + 5] == '1';
    }

    private int sectorBytes() {
        switch (layout) {
            case VirtualCD.LAYOUT_2352_MODE1:
            case VirtualCD.LAYOUT_2352_MODE2:
                return 2352;
            case VirtualCD.LAYOUT_2336_MODE2:
                return 2336;
            default:
                return 2048;
        }
    }

    private int userOffset() {
        switch (layout) {
            case VirtualCD.LAYOUT_2352_MODE1: return 16;
            case VirtualCD.LAYOUT_2352_MODE2: return 24;
            case VirtualCD.LAYOUT_2336_MODE2: return 8;
            default: return 0;
        }
    }

    public boolean isOpen() { return open; }

    public int getSectorCount() { return sectorCount; }

    public int getLayout() { return layout; }

    public String getUrl() { return name; }

    public void readSector(int lba, byte[] dst, int off) {
        int sb = sectorBytes();
        int pos = lba * sb + userOffset();
        for (int i = 0; i < 2048; i++) {
            int p = pos + i;
            dst[off + i] = (p >= 0 && p < data.length) ? data[p] : 0;
        }
    }

    public void readRawSector(int lba, byte[] dst) {
        int sb = sectorBytes();
        int pos = lba * sb;
        if (sb == 2352) {
            for (int i = 0; i < dst.length; i++) {
                int p = pos + i;
                dst[i] = (p >= 0 && p < data.length) ? data[p] : 0;
            }
            return;
        }
        byte[] user = new byte[2048];
        readSector(lba, user, 0);
        for (int i = 0; i < dst.length; i++) dst[i] = 0;
        System.arraycopy(user, 0, dst, 16, Math.min(2048, dst.length - 16));
        dst[1] = (byte) 0xFF;
        dst[2] = (byte) 0xFF;
        for (int i = 3; i <= 10; i++) dst[i] = (byte) 0xFF;
        dst[11] = 0;
        dst[12] = 0;
        dst[13] = 2;
    }

    public int readUser(int lba, int offset, byte[] dst, int off, int len) throws IOException {
        byte[] sec = new byte[2048];
        int done = 0;
        while (done < len) {
            readSector(lba + ((offset + done) / 2048), sec, 0);
            int inSector = (offset + done) % 2048;
            int n = Math.min(2048 - inSector, len - done);
            System.arraycopy(sec, inSector, dst, off + done, n);
            done += n;
        }
        return done;
    }

    public void close() { open = false; }
}
