package rpc.j2me;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Retro PC Builder - MIDlet entry point.
 */
public final class RetroPcMIDlet extends MIDlet {

    static AppCanvas canvas;

    private Display display;
    private HostImpl host;

    protected void startApp() throws MIDletStateChangeException {
        if (display == null) {
            display = Display.getDisplay(this);
        }
        if (host == null) {
            host = new HostImpl(this, display);
        }
        if (canvas == null) {
            canvas = new AppCanvas(this, host);
            try {
                canvas.setFullScreenMode(true);
            } catch (Throwable ignored) {
            }
            display.setCurrent(canvas);
        }
        canvas.start();
    }

    protected void pauseApp() {
        if (canvas != null) {
            canvas.stop();
        }
    }

    protected void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
            canvas.save();
        }
        notifyDestroyed();
    }

    void exit() {
        try {
            destroyApp(true);
        } catch (Throwable ignored) {
        }
    }
}
