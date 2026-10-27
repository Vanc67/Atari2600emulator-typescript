package javax.microedition.media;

/** Minimal desktop Player: the harness renders tones instead of streams. */
public interface Player {
    int UNREALIZED = 100;
    int REALIZED = 200;
    int PREFETCHED = 300;
    int STARTED = 400;
    int CLOSED = 0;

    int getState();

    void realize();

    void prefetch();

    void start();

    void stop();

    void deallocate();

    void close();

    void setLoopCount(int count);

    long getMediaTime();

    long setMediaTime(long now);
}
