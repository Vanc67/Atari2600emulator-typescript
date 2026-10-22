package physicsplayground.platform;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class PlayMidlet extends MIDlet {
    private PlayCanvas canvas;
    public void startApp() {
        if (canvas == null) {
            try { canvas = new PlayCanvas(this); }
            catch (Exception error) {
                Alert alert = new Alert("Physics Playground", "Start failed: " + (error.getMessage() == null ? error.toString() : error.getMessage()), null, AlertType.ERROR);
                alert.setTimeout(Alert.FOREVER); Display.getDisplay(this).setCurrent(alert); return;
            }
        }
        Display.getDisplay(this).setCurrent(canvas); canvas.start(); canvas.resumeGame();
    }
    public void pauseApp() { if (canvas != null) canvas.suspendGame(); }
    public void destroyApp(boolean unconditional) { if (canvas != null) { canvas.stop(); canvas = null; } }
    public void exit() { destroyApp(true); notifyDestroyed(); }
}
