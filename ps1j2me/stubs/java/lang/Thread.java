package java.lang;
public class Thread implements Runnable {
    public Thread() {}
    public Thread(Runnable r) {}
    public Thread(Runnable r, String name) {}
    public void start() {}
    public void run() {}
    public void interrupt() {}
    public final void join() throws InterruptedException {}
    public static void sleep(long ms) throws InterruptedException {}
    public static Thread currentThread() { return null; }
    public void setPriority(int p) {}
    public static final int MAX_PRIORITY = 10;
    public static final int NORM_PRIORITY = 5;
    public static final int MIN_PRIORITY = 1;
}
