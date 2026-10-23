package java.io;
public class DataInputStream extends InputStream {
    public DataInputStream(InputStream in) {}
    public int read() throws IOException { return 0; }
    public int read(byte[] b, int o, int l) throws IOException { return 0; }
    public final int readUnsignedByte() throws IOException { return 0; }
    public final int readUnsignedShort() throws IOException { return 0; }
    public final int readInt() throws IOException { return 0; }
    public final void readFully(byte[] b) throws IOException {}
    public final void readFully(byte[] b, int o, int l) throws IOException {}
    public long skip(long n) throws IOException { return 0L; }
    public int available() throws IOException { return 0; }
    public void close() throws IOException {}
}
