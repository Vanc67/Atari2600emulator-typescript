import java.io.File;
import java.io.FileOutputStream;

import psx.VirtualCD;

/**
 * Desktop tests for disc image opening, including CUE sheets.
 *
 * <p>Real cards are usually filled with {@code game.bin} + {@code game.cue}
 * pairs, and the sheet often carries an absolute Windows path such as
 * {@code FILE "D:\GAMES\GAME.BIN" BINARY}. The browser can only show the .cue,
 * so everything has to be resolved from the sheet itself.</p>
 */
public final class CueSheetTest {

    private static int passed;
    private static int failed;
    private static File dir;

    public static void main(String[] args) throws Exception {
        dir = new File(System.getProperty("java.io.tmpdir"), "cueSheetTest");
        delete(dir);
        dir.mkdirs();

        plainIso();
        cueWithAbsoluteWindowsPath();
        cueMode1Raw();
        cueMixedMode();
        cueMissingPayload();
        cueWithoutTrack();

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL CUESHEET TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("CUESHEET TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    private static void plainIso() throws Exception {
        String iso = newIso("plain.iso", 17, 2048, 0);
        VirtualCD cd = new VirtualCD();
        cd.open(iso);
        same(VirtualCD.LAYOUT_2048, cd.getLayout(), "iso layout 2048");
        same(17, cd.getSectorCount(), "iso sector count");
        check(cd.getCueUrl() == null, "plain image has no cue");
        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check(signature(sec), "iso sector 16 carries CD001");
        cd.close();
        check(!cd.isOpen(), "close() closes the stream");
    }

    private static void cueWithAbsoluteWindowsPath() throws Exception {
        String iso = newIso("MYGAME.BIN", 17, 2048, 0);
        String cue = writeCue("MYGAME.CUE",
                "REM dumped by a Chinese burner\r\n"
                + "FILE \"D:\\GAMES\\PSX\\MYGAME.BIN\" BINARY\r\n"
                + "  TRACK 01 MODE1/2048\r\n"
                + "    INDEX 01 00:00:00\r\n");
        VirtualCD cd = new VirtualCD();
        cd.open(cue);
        same(VirtualCD.LAYOUT_2048, cd.getLayout(), "cue layout from MODE1/2048");
        same(17, cd.getSectorCount(), "cue sector count");
        check(cue.equals(cd.getCueUrl()), "cue url remembered");
        check("MYGAME.BIN".equals(cd.getCueFile()), "payload name remembered");
        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check(signature(sec), "cue payload readable");
        cd.close();
        check(iso != null, "payload file existed");
    }

    private static void cueMode1Raw() throws Exception {
        /* a raw mode 1 image: 16 sync/header bytes then the user data */
        String bin = newIso("RAW.BIN", 17, 2352, 16);
        String cue = writeCue("RAW.CUE",
                "FILE \"RAW.BIN\" BINARY\nTRACK 01 MODE1/2352\nINDEX 01 00:00:00\n");
        VirtualCD cd = new VirtualCD();
        cd.open(cue);
        same(VirtualCD.LAYOUT_2352_MODE1, cd.getLayout(), "cue layout from MODE1/2352");
        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check(signature(sec), "raw sector user data at offset 16");
        cd.close();
        check(bin != null, "raw payload existed");
    }

    /**
     * A pressed disc starts with two seconds of CD-DA, so the sheet has a
     * FILE/TRACK AUDIO pair before the data track. Opening the first FILE
     * would read silence and the game would never boot.
     */
    private static void cueMixedMode() throws Exception {
        newIso("AUDIO.BIN", 20, 2352, 0);              /* zeros: no CD001 */
        newIso("DATA.BIN", 20, 2352, 16);              /* real data track */
        String cue = writeCue("MIXED.CUE",
                "FILE \"AUDIO.BIN\" BINARY\r\n"
                + "  TRACK 01 AUDIO\r\n"
                + "    INDEX 01 00:00:00\r\n"
                + "FILE \"DATA.BIN\" BINARY\r\n"
                + "  TRACK 02 MODE1/2352\r\n"
                + "    INDEX 01 00:00:00\r\n");
        VirtualCD cd = new VirtualCD();
        cd.open(cue);
        check("DATA.BIN".equals(cd.getCueFile()), "data track file chosen, not the CD-DA track");
        same(VirtualCD.LAYOUT_2352_MODE1, cd.getLayout(), "mixed sheet layout");
        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check(signature(sec), "mixed sheet reads the data track");
        cd.close();
    }

    private static void cueMissingPayload() {
        try {
            String cue = writeCue("LOST.CUE",
                    "FILE \"LOST.BIN\" BINARY\nTRACK 01 MODE1/2352\n");
            new VirtualCD().open(cue);
            check(false, "missing payload must fail");
        } catch (Exception e) {
            check(true, "missing payload reports an error");
        }
    }

    private static void cueWithoutTrack() throws Exception {
        /* no TRACK line: the layout has to be sniffed from the payload */
        newIso("NOTRACK.BIN", 17, 2352, 16);
        String cue = writeCue("NOTRACK.CUE", "FILE \"NOTRACK.BIN\" BINARY\n");
        VirtualCD cd = new VirtualCD();
        cd.open(cue);
        same(VirtualCD.LAYOUT_2352_MODE1, cd.getLayout(), "layout sniffed without TRACK");
        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check(signature(sec), "sniffed payload readable");
        /* reading past the end must yield zeros, not an exception */
        cd.readSector(9999, sec, 0);
        boolean zeros = true;
        for (int i = 0; i < sec.length; i++) if (sec[i] != 0) zeros = false;
        check(zeros, "sector past the end is zero filled");
        cd.close();
    }

    /* ------------------------------------------------------------------ */

    /** Writes a fake ISO9660 image with "CD001" in the primary volume descriptor. */
    private static String newIso(String name, int sectors, int sectorBytes, int dataOffset)
            throws Exception {
        File f = new File(dir, name);
        FileOutputStream out = new FileOutputStream(f);
        try {
            byte[] sector = new byte[sectorBytes];
            for (int s = 0; s < sectors; s++) {
                for (int i = 0; i < sector.length; i++) sector[i] = 0;
                if (s == 16) {
                    sector[dataOffset] = 1;                 /* descriptor type */
                    sector[dataOffset + 1] = 'C';
                    sector[dataOffset + 2] = 'D';
                    sector[dataOffset + 3] = '0';
                    sector[dataOffset + 4] = '0';
                    sector[dataOffset + 5] = '1';
                } else if (s == 17) {
                    sector[0] = 'd';                        /* marker sector */
                }
                out.write(sector);
            }
        } finally {
            out.close();
        }
        return f.getAbsolutePath();
    }

    private static String writeCue(String name, String body) {
        try {
            File f = new File(dir, name);
            FileOutputStream out = new FileOutputStream(f);
            try {
                out.write(body.getBytes("ISO-8859-1"));
            } finally {
                out.close();
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            throw new RuntimeException(e.toString());
        }
    }

    private static boolean signature(byte[] sec) {
        return sec[1] == 'C' && sec[2] == 'D' && sec[3] == '0' && sec[4] == '0' && sec[5] == '1';
    }

    private static void delete(File f) {
        if (f.isDirectory()) {
            String[] kids = f.list();
            for (int i = 0; kids != null && i < kids.length; i++) delete(new File(f, kids[i]));
        }
        f.delete();
    }

    private static void same(int expected, int actual, String what) {
        check(expected == actual, what + " (expected " + expected + ", got " + actual + ")");
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what);
        } else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }
}
