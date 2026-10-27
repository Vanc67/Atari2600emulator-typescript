package java.lang;
public class Object {
    public Object() {}
    public boolean equals(Object o) { return this == o; }
    public int hashCode() { return 0; }
    public String toString() { return ""; }
    public final void wait(long ms) throws InterruptedException {}
    public final void wait() throws InterruptedException {}
    public final void notify() {}
    public final void notifyAll() {}
    public final Class getClass() { return null; }
    protected Object clone() throws CloneNotSupportedException { return null; }
    protected void finalize() {}
}
