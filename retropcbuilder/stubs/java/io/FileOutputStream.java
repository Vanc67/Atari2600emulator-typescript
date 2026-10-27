package java.io;
public class FileOutputStream extends OutputStream {
    public FileOutputStream(String name) throws IOException {}
    public FileOutputStream(File f) throws IOException {}
    public void write(int b) throws IOException {}
    public void write(byte[] b) throws IOException {}
    public void write(byte[] b, int off, int len) throws IOException {}
    public void close() throws IOException {}
}
