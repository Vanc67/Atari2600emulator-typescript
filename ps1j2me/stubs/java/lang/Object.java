package java.lang;
public class Object {
    public Object() {}
    public boolean equals(Object o) { return this == o; }
    public int hashCode() { return 0; }
    public String toString() { return ""; }
    public final Class getClass() { return null; }
    protected Object clone() throws CloneNotSupportedException { return null; }
    protected void finalize() {}
}
