package psx;

import java.io.IOException;

/** Minimal ISO9660 directory reader used by the HLE boot path. */
public final class Iso {

    private Iso() { }

    public static int rootDirLba(CdImage cd) throws IOException {
        byte[] pvd = new byte[2048];
        cd.readSector(16, pvd, 0);
        if (pvd[1] != 'C' || pvd[2] != 'D' || pvd[3] != '0') return -1;
        return le32(pvd, 156 + 2);
    }

    public static int rootDirSize(CdImage cd) throws IOException {
        byte[] pvd = new byte[2048];
        cd.readSector(16, pvd, 0);
        return le32(pvd, 156 + 10);
    }

    /** Reads a file from the root directory; returns null when not found. */
    public static byte[] readFile(CdImage cd, String name) throws IOException {
        int[] entry = findEntry(cd, name);
        if (entry == null) return null;
        int lba = entry[0];
        int size = entry[1];
        if (size <= 0 || size > 8 * 1024 * 1024) return null;
        byte[] out = new byte[size];
        cd.readUser(lba, 0, out, 0, size);
        return out;
    }

    /** @return {lba, size} of a root directory entry or null. */
    public static int[] findEntry(CdImage cd, String name) throws IOException {
        int rootLba = rootDirLba(cd);
        if (rootLba < 0) return null;
        int size = rootDirSize(cd);
        int sectors = (size + 2047) / 2048;
        byte[] sec = new byte[2048];
        String wanted = stripVersion(name).toUpperCase();
        for (int s = 0; s < sectors; s++) {
            cd.readSector(rootLba + s, sec, 0);
            int off = 0;
            while (off < 2048) {
                int len = sec[off] & 0xFF;
                if (len == 0) break;
                int nameLen = sec[off + 32] & 0xFF;
                String entry = new String(sec, off + 33, nameLen);
                String clean = stripVersion(entry).toUpperCase();
                if (clean.equals(wanted)) {
                    return new int[] { le32(sec, off + 2), le32(sec, off + 10) };
                }
                off += len;
            }
        }
        return null;
    }

    public static int findFile(CdImage cd, String name) throws IOException {
        int[] e = findEntry(cd, name);
        return e == null ? -1 : e[0];
    }

    private static String stripVersion(String s) {
        int i = s.indexOf(';');
        if (i >= 0) s = s.substring(0, i);
        return s.trim();
    }

    private static int le32(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
    }
}
