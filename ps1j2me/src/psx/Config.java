package psx;

import javax.microedition.rms.RecordStore;

/**
 * Persistent settings, stored as one CSV record in RMS.
 *
 * Every field has a sane default and the whole class degrades silently to
 * in-memory values when RMS is unavailable, so the MIDlet never fails to
 * start because of storage problems.
 */
public final class Config {

    public static final int SCALE_STRETCH = 0;
    public static final int SCALE_4_3 = 1;
    public static final int SCALE_1_1 = 2;

    private static final String STORE = "ps1j2me-cfg";

    public int scaleMode = SCALE_4_3;
    public int frameSkip = 0;        /* 0..2 frames skipped per redraw */
    public boolean sound = true;
    public int volume = 80;          /* 0..100 */
    public boolean showFps = true;
    public boolean useHle = false;   /* false = prefer the real BIOS if present */
    public int padLayout = 0;        /* 0 = standard, 1 = swapped fire/circle */

    private static Config instance;

    public static Config get() {
        if (instance == null) instance = new Config();
        return instance;
    }

    private Config() {
        load();
    }

    /** Fresh instance reading the stored record (used by tests). */
    static Config loadFresh() {
        return new Config();
    }

    /* ---- persistence ----------------------------------------------------- */

    public void load() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            if (rs.getNumRecords() > 0) {
                byte[] data = rs.getRecord(1);
                if (data != null) parse(new String(data));
            }
        } catch (Throwable t) {
            /* keep defaults */
        } finally {
            close(rs);
        }
    }

    public void save() {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore(STORE, true);
            byte[] data = serialize().getBytes();
            if (rs.getNumRecords() == 0) rs.addRecord(data, 0, data.length);
            else rs.setRecord(1, data, 0, data.length);
        } catch (Throwable t) {
            /* ignore */
        } finally {
            close(rs);
        }
    }

    private static void close(RecordStore rs) {
        if (rs != null) {
            try { rs.closeRecordStore(); } catch (Throwable t) { }
        }
    }

    private String serialize() {
        return scaleMode + "," + frameSkip + "," + (sound ? 1 : 0) + "," + volume + ","
                + (showFps ? 1 : 0) + "," + (useHle ? 1 : 0) + "," + padLayout;
    }

    private void parse(String s) {
        try {
            int[] v = new int[8];
            int n = 0;
            int start = 0;
            for (int i = 0; i <= s.length() && n < 8; i++) {
                if (i == s.length() || s.charAt(i) == ',') {
                    v[n++] = Integer.parseInt(s.substring(start, i).trim());
                    start = i + 1;
                }
            }
            if (n > 0) scaleMode = clamp(v[0], 0, 2);
            if (n > 1) frameSkip = clamp(v[1], 0, 2);
            if (n > 2) sound = v[2] != 0;
            if (n > 3) volume = clamp(v[3], 0, 100);
            if (n > 4) showFps = v[4] != 0;
            if (n > 5) useHle = v[5] != 0;
            if (n > 6) padLayout = clamp(v[6], 0, 1);
        } catch (Throwable t) {
            /* partial / corrupt record: keep whatever parsed */
        }
    }

    private static int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    public void reset() {
        scaleMode = SCALE_4_3;
        frameSkip = 0;
        sound = true;
        volume = 80;
        showFps = true;
        useHle = false;
        padLayout = 0;
        save();
    }

    public String scaleName() {
        switch (scaleMode) {
            case SCALE_STRETCH: return "На весь экран";
            case SCALE_1_1: return "1:1 пиксель";
            default: return "4:3 с полями";
        }
    }
}
