package javax.microedition.media.protocol;
public abstract class DataSource implements javax.microedition.media.Controllable {
    public abstract void connect() throws java.io.IOException;
    public abstract void disconnect();
    public abstract void start() throws java.io.IOException;
    public abstract void stop() throws java.io.IOException;
    public abstract String getContentType();
    public abstract SourceStream[] getStreams();
    public javax.microedition.media.Control getControl(String t) { return null; }
    public javax.microedition.media.Control[] getControls() { return null; }
}
