package javax.microedition.io.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public interface FileConnection extends javax.microedition.io.InputConnection {
    boolean isOpen();
    long fileSize() throws IOException;
    boolean canRead();
    boolean canWrite();
    boolean isHidden();
    boolean isDirectory();
    void mkdir() throws IOException;
    void create() throws IOException;
    String getName();
    String getPath();
    String getURL();
    boolean exists();
    long lastModified();
    void setFileConnection(String name) throws IOException;
    void delete() throws IOException;
    void rename(String newName) throws IOException;
    void truncate(long byteOffset) throws IOException;
    Enumeration list() throws IOException;
    Enumeration list(String filter, boolean includeHidden) throws IOException;
    OutputStream openOutputStream() throws IOException;
    OutputStream openOutputStream(long offset) throws IOException;
}
