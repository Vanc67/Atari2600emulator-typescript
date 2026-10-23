package javax.microedition.media.protocol;
public interface SourceStream extends javax.microedition.io.InputConnection {
    ContentDescriptor getContentDescriptor();
    long getContentLength();
    int getSeekType();
    int seek(long where) throws java.io.IOException;
    long tell();
    int READABLE = 1; int RANDOM_ACCESS = 2; int WRITEABLE = 4;
}
