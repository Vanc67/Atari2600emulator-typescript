package javax.microedition.lcdui;
public class Display {
    public static Display getDisplay(javax.microedition.midlet.MIDlet m) { return null; }
    public void setCurrent(Displayable d) {}
    public void setCurrent(Alert a, Displayable d) {}
    public Displayable getCurrent() { return null; }
    public void callSerially(Runnable r) {}
    public boolean vibrate(int ms) { return false; }
    public static final int KEY_NUM0 = 48; public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50; public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52; public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54; public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56; public static final int KEY_NUM9 = 57;
    public static final int KEY_STAR = 42; public static final int KEY_POUND = 35;
}
