package java.lang;
public class Runtime {
    private static final Runtime INSTANCE = new Runtime();
    public static Runtime getRuntime() { return INSTANCE; }
    public long freeMemory() { return 0; }
    public long totalMemory() { return 0; }
    public long maxMemory() { return 0; }
    public void gc() { }
    public void halt(int status) { }
    public void exit(int status) { }
    public void runFinalization() { }
}
