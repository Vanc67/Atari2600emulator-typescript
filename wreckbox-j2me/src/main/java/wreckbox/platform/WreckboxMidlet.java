package wreckbox.platform;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

public final class WreckboxMidlet extends MIDlet {
  private WreckboxCanvas canvas;
  protected void startApp() {
    if (canvas == null) canvas = new WreckboxCanvas();
    Display.getDisplay(this).setCurrent(canvas);
    canvas.start();
  }
  protected void pauseApp() { if (canvas != null) canvas.suspend(); }
  protected void destroyApp(boolean unconditional) { if (canvas != null) canvas.stop(); }
}
