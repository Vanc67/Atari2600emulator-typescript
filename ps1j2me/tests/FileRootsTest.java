import java.util.Enumeration;
import java.util.Vector;

import psx.FileRoots;

/**
 * Desktop tests for storage-root discovery.
 *
 * <p>The JSR-75 shim used by the suite returns {@code null} from
 * {@link javax.microedition.io.file.FileSystemRegistry#listRoots()}, which is
 * exactly what several real handsets do, so the property and probe paths are
 * the interesting ones.</p>
 */
public final class FileRootsTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) throws Exception {
        String base = System.getProperty("java.io.tmpdir") + "/fileRootsTest";
        java.io.File dir = new java.io.File(base);
        delete(dir);
        dir.mkdirs();
        new java.io.File(dir, "games").mkdirs();
        new java.io.File(dir, "readme.txt").createNewFile();

        normalization();
        parents();
        labels();
        usability(dir.getAbsolutePath());
        discovery(dir.getAbsolutePath());

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL FILEROOTS TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("FILEROOTS TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    private static void normalization() {
        same("file:///E:/", FileRoots.normalize("file:///E:/"), "already normal");
        same("file:///E:/", FileRoots.normalize("E:/"), "drive letter");
        same("file:///E:/", FileRoots.normalize("file:///E:"), "missing trailing slash");
        same("file:///sdcard/", FileRoots.normalize("/sdcard"), "absolute path");
        same(null, FileRoots.normalize(""), "empty string");
        same(null, FileRoots.normalize(null), "null");
        check(FileRoots.isDirectoryUrl("file:///E:/") && !FileRoots.isDirectoryUrl("file:///E:/a.bin"),
                "isDirectoryUrl");
        same("file:///E:/games/", FileRoots.join("file:///E:/", "games/"), "join keeps one slash");
        same("file:///E:/x", FileRoots.join("file:///E:", "x"), "join adds slash");
    }

    private static void parents() {
        same("file:///E:/games/", FileRoots.parent("file:///E:/games/rpg/"), "parent of subfolder");
        same(null, FileRoots.parent("file:///E:/"), "no parent at a drive root");
        same(null, FileRoots.parent("file:///E:/games/"), "no parent above the drive root");
        same("file:///root1/", FileRoots.parent("file:///root1/games/"), "symbian style root");
    }

    private static void labels() {
        contains(FileRoots.label("file:///E:/"), "Карта", "E: is a card");
        contains(FileRoots.label("file:///C:/"), "Телефон", "C: is the phone");
        contains(FileRoots.label("file:///root1/"), "Карта", "root1 is a card");
        contains(FileRoots.label("file:///root2/"), "Телефон", "root2 is the phone");
        contains(FileRoots.label("file:///sdcard/"), "Карта", "sdcard is a card");
        check(FileRoots.label("file:///E:/").indexOf("E:") >= 0, "label mentions the drive letter");
    }

    private static void usability(String base) throws Exception {
        String url = base + "/";
        check(FileRoots.isUsable(url), "existing folder is usable");
        check(!FileRoots.isUsable(base + "/nope/"), "missing folder is not usable");
        check(!FileRoots.isUsable(base + "/readme.txt"), "a file is not a usable root");
        check(!FileRoots.isUsable(null), "null is not usable");
    }

    private static void discovery(String base) throws Exception {
        String url = base + "/";
        String normalized = "file://" + url;
        System.setProperty("fileconn.dir.memorycard", url);
        Vector roots = FileRoots.discoverRoots();
        check(roots != null, "discoverRoots never returns null");
        boolean found = false;
        for (int i = 0; i < roots.size(); i++) {
            FileRoots.Root r = (FileRoots.Root) roots.elementAt(i);
            if (normalized.equals(r.url) && r.label != null && r.label.length() > 0) found = true;
        }
        check(found, "root found through fileconn.dir.memorycard");

        /* a property list with separators and a dead path: only live ones stay */
        System.clearProperty("fileconn.dir.memorycard");
        System.setProperty("fileconn.dir.roots.names", base + "/nope;" + url);
        Vector list = FileRoots.discoverRoots();
        boolean oneLive = false;
        int live = 0;
        for (int i = 0; i < list.size(); i++) {
            FileRoots.Root r = (FileRoots.Root) list.elementAt(i);
            if (normalized.equals(r.url)) oneLive = true;
            live++;
        }
        check(oneLive, "separator list keeps the live root");
        System.clearProperty("fileconn.dir.roots.names");

        /* the shim returns null from listRoots(): discovery must not throw */
        check(FileRoots.firstRoot() == null || FileRoots.firstRoot().url != null,
                "firstRoot tolerates a null registry");
    }

    /* ------------------------------------------------------------------ */

    private static void delete(java.io.File f) {
        if (f.isDirectory()) {
            String[] kids = f.list();
            for (int i = 0; kids != null && i < kids.length; i++) delete(new java.io.File(f, kids[i]));
        }
        f.delete();
    }

    private static void contains(String s, String part, String what) {
        check(s != null && s.indexOf(part) >= 0, what + " (" + s + ")");
    }

    private static void same(String expected, String actual, String what) {
        boolean ok = expected == null ? actual == null : expected.equals(actual);
        check(ok, what + " (expected " + expected + ", got " + actual + ")");
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
