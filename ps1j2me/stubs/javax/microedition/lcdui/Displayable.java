package javax.microedition.lcdui;
public abstract class Displayable {
    public void addCommand(Command c) {}
    public void removeCommand(Command c) {}
    public void setCommandListener(CommandListener l) {}
    public String getTitle() { return null; }
    public void setTitle(String t) {}
    /* Test-only screen size, so headless painters exercise a real layout. */
    public static int STUB_WIDTH = 176;
    public static int STUB_HEIGHT = 208;
    public int getWidth() { return STUB_WIDTH; }
    public int getHeight() { return STUB_HEIGHT; }
    public boolean isShown() { return false; }
    public Display getDisplay() { return null; }
}
