package javax.microedition.io.file;

import java.util.Enumeration;

public final class FileSystemRegistry {
    public static Enumeration listRoots() { return null; }
    public static boolean addFileSystemListener(FileSystemListener l) { return false; }
    public static boolean removeFileSystemListener(FileSystemListener l) { return false; }
}
