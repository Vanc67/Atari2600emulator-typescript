package javax.microedition.io.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

/** java.io backed FileConnection used by the desktop test-suite. */
public final class FileConnectionImpl implements FileConnection {
    private final File file;
    private boolean open = true;

    public FileConnectionImpl(String url) {
        this.file = new File(javax.microedition.io.Connector.toPath(url));
    }

    public boolean isOpen() { return open; }
    public long fileSize() throws IOException { return file.length(); }
    public boolean canRead() { return file.canRead(); }
    public boolean canWrite() { return file.canWrite(); }
    public boolean isHidden() { return file.isHidden(); }
    public boolean isDirectory() {
        /* Some firmwares throw here instead of returning a value; the flag lets
           the suite reproduce that behaviour on a desktop JVM. */
        if (System.getProperty("jsr75.stub.isDirectoryBroken") != null) {
            throw new RuntimeException("stub: isDirectory is not supported");
        }
        return file.isDirectory();
    }
    public void mkdir() throws IOException { if (!file.mkdir()) throw new IOException("mkdir"); }
    public void create() throws IOException { if (!file.createNewFile()) throw new IOException("create"); }
    public String getName() { return file.getName(); }
    public String getPath() { return file.getPath(); }
    public String getURL() { return "file://" + file.getPath(); }
    public boolean exists() { return file.exists(); }
    public long lastModified() { return file.lastModified(); }
    public void setFileConnection(String name) throws IOException { }
    public void delete() throws IOException { if (!file.delete()) throw new IOException("delete"); }
    public void rename(String newName) throws IOException {
        if (!file.renameTo(new File(file.getParentFile(), newName))) throw new IOException("rename");
    }
    public void truncate(long byteOffset) throws IOException { }
    public void close() { open = false; }

    public Enumeration list() throws IOException {
        String[] names = file.list();
        if (names == null) throw new IOException("list");
        Vector v = new Vector();
        for (int i = 0; i < names.length; i++) {
            File f = new File(file, names[i]);
            v.addElement(f.isDirectory() ? names[i] + "/" : names[i]);
        }
        return v.elements();
    }

    public Enumeration list(String filter, boolean includeHidden) throws IOException { return list(); }
    public InputStream openInputStream() throws IOException { return new java.io.FileInputStream(file); }
    public java.io.DataInputStream openDataInputStream() throws IOException {
        return new java.io.DataInputStream(openInputStream());
    }
    public OutputStream openOutputStream() throws IOException { return new java.io.FileOutputStream(file); }
    public OutputStream openOutputStream(long offset) throws IOException { return new java.io.FileOutputStream(file); }
}
