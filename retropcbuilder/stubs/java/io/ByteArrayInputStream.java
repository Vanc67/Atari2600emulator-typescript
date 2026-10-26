package java.io;
public class ByteArrayInputStream extends InputStream {
    public ByteArrayInputStream(byte[] buf) {}
    public ByteArrayInputStream(byte[] buf, int off, int len) {}
    public int read() { return 0; }
    public int read(byte[] b, int off, int len) { return 0; }
    public int available() { return 0; }
    public void close() throws IOException {}
}
