package javax.microedition.lcdui;
public abstract class Displayable {
    public void addCommand(Command c) {}
    public void removeCommand(Command c) {}
    public void setCommandListener(CommandListener l) {}
    public String getTitle() { return null; }
    public void setTitle(String t) {}
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public boolean isShown() { return false; }
    public Display getDisplay() { return null; }
}
