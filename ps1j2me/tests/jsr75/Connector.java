package javax.microedition.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/** Desktop JSR-75 shim used only by the test-suite (never packaged). */
public final class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    private Connector() { }

    public static Connection open(String name, int mode) throws IOException {
        return new javax.microedition.io.file.FileConnectionImpl(name);
    }

    public static Connection open(String name) throws IOException {
        return open(name, READ);
    }

    public static InputStream openInputStream(String name) throws IOException {
        return new java.io.FileInputStream(toPath(name));
    }

    public static DataInputStream openDataInputStream(String name) throws IOException {
        return new DataInputStream(openInputStream(name));
    }

    public static String toPath(String url) {
        String p = url;
        if (p.startsWith("file://")) p = p.substring(7);
        else if (p.startsWith("file:")) p = p.substring(5);
        return p;
    }
}
