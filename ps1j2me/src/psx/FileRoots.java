package psx;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 * Finds the storage devices of a handset without ever showing a permission
 * dialog.
 *
 * <p>MIDP only guarantees {@link FileSystemRegistry#listRoots()}, and quite a
 * few firmwares return {@code null}, an empty list, or roots that cannot be
 * opened at all. The {@code fileconn.dir.*} system properties are much more
 * useful but their names differ between vendors (Nokia, Samsung, Motorola,
 * Sony Ericsson, MTK "China phones"). This class tries every known source,
 * validates each candidate by actually listing it, removes duplicates and
 * gives every root a human readable Russian label.</p>
 *
 * <p>Nothing here runs at start-up: {@link #discoverRoots()} is called from a
 * worker thread when the player opens the file browser.</p>
 */
public final class FileRoots {

    /** A storage device: {@code file:///E:/} plus a label like "Карта памяти". */
    public static final class Root {
        public final String url;
        public final String label;

        Root(String url, String label) {
            this.url = url;
            this.label = label;
        }
    }

    /** Properties that point at a card / phone memory, most specific first. */
    private static final String[] DIR_PROPERTIES = {
        "fileconn.dir.memorycard",
        "fileconn.dir.memorycard.private",
        "fileconn.dir.roots.names",
        "fileconn.dir.games",
        "fileconn.dir.private",
        "fileconn.dir.photos",
        "fileconn.dir.music",
        "fileconn.dir.videos",
        "fileconn.dir.recordings",
        "fileconn.dir.sounds",
        "fileconn.dir.graphics",
    };

    /** Last resort: paths used by common chipsets. Probed only when needed. */
    private static final String[] PROBES = {
        "file:///E:/",
        "file:///F:/",
        "file:///D:/",
        "file:///C:/",
        "file:///root1/",
        "file:///root2/",
        "file:///sdcard/",
        "file:///SDCard/",
        "file:///mmc/",
        "file:///MemoryCard/",
        "file:///A:/",
        "file:///B:/",
    };

    private FileRoots() { }

    /* ---- public API ------------------------------------------------------ */

    /**
     * Discovers every usable storage root.
     *
     * @return a Vector of {@link Root}; never null, may be empty
     */
    public static Vector discoverRoots() {
        Vector out = new Vector();
        Vector seen = new Vector();

        for (int i = 0; i < DIR_PROPERTIES.length; i++) {
            String value = property(DIR_PROPERTIES[i]);
            if (value == null) continue;
            addCandidates(out, seen, value);
        }

        try {
            Enumeration e = FileSystemRegistry.listRoots();
            while (e != null && e.hasMoreElements()) {
                addCandidate(out, seen, (String) e.nextElement());
            }
        } catch (Throwable t) {
            /* some firmwares throw SecurityException here */
        }

        if (out.size() == 0) {
            for (int i = 0; i < PROBES.length; i++) {
                addCandidate(out, seen, PROBES[i]);
            }
        }
        return out;
    }

    /** First usable root or null. */
    public static Root firstRoot() {
        Vector v = discoverRoots();
        return v.size() > 0 ? (Root) v.elementAt(0) : null;
    }

    /** True when the URL ends with a slash (i.e. names a directory). */
    public static boolean isDirectoryUrl(String url) {
        return url != null && (url.endsWith("/") || url.endsWith("\\"));
    }

    /** Ensures a trailing slash and the {@code file://} scheme. */
    public static String normalize(String url) {
        if (url == null) return null;
        String u = url.trim();
        if (u.length() == 0) return null;
        if (!u.startsWith("file:")) {
            if (u.startsWith("/")) u = "file://" + u;
            else if (u.length() > 1 && u.charAt(1) == ':') u = "file:///" + u;
            else u = "file:///" + u;
        }
        if (!u.endsWith("/")) u = u + "/";
        return u;
    }

    /** Joins a directory URL and a child name, keeping exactly one slash. */
    public static String join(String base, String name) {
        if (base == null) return name;
        if (name == null) name = "";
        if (base.length() > 0 && !base.endsWith("/")) base = base + "/";
        return base + name;
    }

    /** @return the parent directory URL, or null at the top of a device. */
    public static String parent(String url) {
        if (url == null) return null;
        String u = url;
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        int slash = u.lastIndexOf('/');
        if (slash < 0) return null;
        /* keep "file:///E:" style roots as they are */
        int scheme = u.indexOf("://");
        if (scheme >= 0 && slash <= scheme + 3) return null;
        String parent = u.substring(0, slash + 1);
        if (parent.endsWith(":/")) return null;
        if (parent.length() <= 8) return null;
        return parent;
    }

    /** Friendly name for a root URL. */
    public static String label(String url) {
        if (url == null) return "?";
        String lower = url.toLowerCase();
        String drive = driveOf(lower);
        if (lower.indexOf("memorycard") >= 0) return "Карта памяти" + suffix(drive);
        if (lower.indexOf("sdcard") >= 0) return "Карта памяти" + suffix(drive);
        if (lower.indexOf("mmc") >= 0) return "Карта памяти" + suffix(drive);
        if (lower.indexOf("root1") >= 0) return "Карта памяти";
        if (lower.indexOf("root2") >= 0) return "Телефон";
        if (lower.indexOf("root") >= 0) return "Память" + suffix(drive);
        if (drive.length() > 0) {
            if (drive.equals("C:") || drive.equals("A:") || drive.equals("B:")) {
                return "Телефон (" + drive + ")";
            }
            return "Карта памяти (" + drive + ")";
        }
        String name = lastSegment(url);
        return name.length() > 0 ? name : url;
    }

    /* ---- internals ------------------------------------------------------- */

    private static void addCandidates(Vector out, Vector seen, String value) {
        int start = 0;
        while (start <= value.length()) {
            int comma = value.indexOf(',', start);
            int semi = value.indexOf(';', start);
            int cut = comma;
            if (cut < 0 || (semi >= 0 && semi < cut)) cut = semi;
            String part = cut < 0 ? value.substring(start) : value.substring(start, cut);
            addCandidate(out, seen, part);
            if (cut < 0) break;
            start = cut + 1;
        }
    }

    private static void addCandidate(Vector out, Vector seen, String raw) {
        if (raw == null) return;
        String url = normalize(raw);
        if (url == null) return;
        for (int i = 0; i < seen.size(); i++) {
            if (url.equals(seen.elementAt(i))) return;
        }
        if (!isUsable(url)) return;
        seen.addElement(url);
        out.addElement(new Root(url, label(url)));
    }

    /**
     * Opens and lists a directory; false on any error, never throws.
     *
     * <p>The only trustworthy test is "it opened and it can be listed". A few
     * firmwares leave {@code exists()} at false for a mounted card, or throw
     * from {@code isDirectory()}; rejecting such a root would hide the player's
     * memory card entirely, so both calls are treated as hints only.</p>
     */
    public static boolean isUsable(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            if (fc == null) return false;
            try {
                if (!fc.isDirectory()) return false;
            } catch (Throwable ignore) {
                /* firmware does not implement it: keep going */
            }
            Enumeration e;
            try {
                e = fc.list();
            } catch (Throwable t) {
                e = fc.list("*", false);
            }
            return e != null;
        } catch (Throwable t) {
            return false;
        } finally {
            close(fc);
        }
    }

    static void close(FileConnection fc) {
        if (fc == null) return;
        try {
            fc.close();
        } catch (Throwable t) {
            /* ignore */
        }
    }

    private static String property(String name) {
        try {
            return System.getProperty(name);
        } catch (Throwable t) {
            return null;
        }
    }

    private static String driveOf(String lower) {
        int scheme = lower.indexOf("://");
        if (scheme < 0) return "";
        int p = scheme + 3;
        while (p < lower.length() && lower.charAt(p) == '/') p++;
        if (p + 1 < lower.length() && lower.charAt(p + 1) == ':') {
            return lower.substring(p, p + 2).toUpperCase();
        }
        return "";
    }

    private static String suffix(String drive) {
        return drive.length() > 0 ? " (" + drive + ")" : "";
    }

    private static String lastSegment(String url) {
        String u = url;
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        int slash = u.lastIndexOf('/');
        return slash >= 0 ? u.substring(slash + 1) : u;
    }
}
