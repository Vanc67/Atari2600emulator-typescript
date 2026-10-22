package rdl.platform;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class LabMidlet extends MIDlet {
    private LabCanvas canvas;

    public void startApp() {
        if (canvas == null) {
            canvas = new LabCanvas();
            canvas.start();
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.resumeGame();
    }

    public void pauseApp() {
        if (canvas != null) {
            canvas.suspendGame();
        }
    }

    public void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
            canvas = null;
        }
    }
}
