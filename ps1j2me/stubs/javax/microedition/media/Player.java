package javax.microedition.media;
public interface Player extends Controllable {
    int UNREALIZED = 0; int REALIZED = 100; int PREFETCHED = 200; int STARTED = 400; int CLOSED = 500;
    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    void setLoopCount(int n);
    long setMediaTime(long t);
    long getMediaTime();
    long getDuration();
    int getState();
    void setPlayerListener(PlayerListener l);
}
