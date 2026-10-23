package psx;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * MIDlet entry point. Deliberately does nothing but create the canvas: no
 * file system access, no permission dialogs, no audio until a game starts.
 */
public final class PS1Midlet extends MIDlet {

    private MainCanvas canvas;
    private Display display;

    public PS1Midlet() { }

    protected void startApp() throws MIDletStateChangeException {
        if (display == null) {
            display = Display.getDisplay(this);
        }
        if (canvas == null) {
            canvas = new MainCanvas(this);
            display.setCurrent(canvas);
        } else {
            display.setCurrent(canvas);
        }
    }

    protected void pauseApp() { }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        if (canvas != null) canvas.shutdown();
        canvas = null;
    }

    void quit() {
        try {
            canvas.shutdown();
        } catch (Throwable t) { }
        notifyDestroyed();
    }
}
