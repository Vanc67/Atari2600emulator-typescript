package javax.microedition.io;
public class Connector {
    public static final int READ = 1; public static final int WRITE = 2; public static final int READ_WRITE = 3;
    public static Connection open(String name) throws java.io.IOException { return null; }
    public static Connection open(String name, int mode) throws java.io.IOException { return null; }
    public static Connection open(String name, int mode, boolean timeouts) throws java.io.IOException { return null; }
    public static java.io.DataInputStream openDataInputStream(String name) throws java.io.IOException { return null; }
    public static java.io.DataOutputStream openDataOutputStream(String name) throws java.io.IOException { return null; }
    public static java.io.InputStream openInputStream(String name) throws java.io.IOException { return null; }
    public static java.io.OutputStream openOutputStream(String name) throws java.io.IOException { return null; }
}
