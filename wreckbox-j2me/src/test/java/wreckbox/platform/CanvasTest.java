package wreckbox.platform;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/** Exercises real K510i menu/key handlers against the headless MIDP stubs. */
public final class CanvasTest {
  private int checks, failures;
  private WreckboxCanvas canvas;
  private final Graphics graphics = new Graphics();

  public static void main(String[] args) {
    CanvasTest test = new CanvasTest(); test.run();
    System.out.println("Wreckbox CanvasTest: " + test.checks + " checks, " + test.failures + " failures");
    if (test.failures != 0) throw new RuntimeException("FAILED " + test.failures);
  }
  private void check(boolean condition, String message) {
    checks++; if (!condition) { failures++; System.out.println("FAIL " + message); }
  }
  private void eq(int expected, int actual, String message) {
    check(expected == actual, message + " expected=" + expected + " actual=" + actual);
  }
  private void press(int key) { canvas.keyPressed(key); }
  private void paint(String name) {
    try { canvas.paint(graphics); check(true, name); }
    catch (RuntimeException e) { check(false, name + " " + e); }
  }
  private void down(int count) { for (int i = 0; i < count; i++) press(Canvas.KEY_NUM8); }

  private void run() {
    try {
      canvas = new WreckboxCanvas(); canvas.sizeChanged(128, 160); check(true, "canvas construct");
    } catch (RuntimeException e) { check(false, "canvas construct " + e); return; }
    eq(WreckboxCanvas.MENU_MAIN, canvas.menuPage(), "startup opens main menu");
    check(!canvas.simulationStarted(), "simulation waits for menu");
    paint("startup main menu 128x160");

    down(1); press(Canvas.KEY_NUM5);
    eq(WreckboxCanvas.MENU_MAPS, canvas.menuPage(), "map item opens map browser");
    for (int i = 0; i < 8; i++) {
      eq(i, canvas.selectedMap(), "map browser selection " + i);
      paint("real map preview " + i);
      if (i < 7) down(1);
    }
    press(Canvas.KEY_NUM5);
    eq(WreckboxCanvas.MENU_NONE, canvas.menuPage(), "map fire closes browser");
    eq(7, canvas.selectedMap(), "selected map is loaded");
    check(canvas.simulationStarted(), "map fire starts simulation");
    paint("selected map gameplay");

    press(-6); down(2); press(Canvas.KEY_NUM5);
    eq(WreckboxCanvas.MENU_SETTINGS, canvas.menuPage(), "settings page opens");
    paint("settings page");
    for (int i = 0; i < 8; i++) { press(Canvas.KEY_NUM5); press(Canvas.KEY_NUM8); }
    paint("all settings toggles");

    press(-6); down(3); press(Canvas.KEY_NUM5); paint("help page");
    down(35); paint("help scroll to end");
    press(Canvas.KEY_NUM5); down(4); press(Canvas.KEY_NUM5); paint("about page");
    down(25); paint("about scroll to end");
    press(Canvas.KEY_NUM5); press(-6); press(Canvas.KEY_STAR);
    eq(WreckboxCanvas.MENU_CONTROL, canvas.menuPage(), "simulation toolbar opens");
    paint("simulation toolbar");
    down(4); press(Canvas.KEY_NUM5); paint("stress toggle");
    down(2); press(Canvas.KEY_NUM5); paint("blood toggle");
    down(1); press(Canvas.KEY_NUM5); paint("clear debris command");

    press(-6); press(-6); press(Canvas.KEY_STAR); down(11); press(Canvas.KEY_NUM5);
    eq(WreckboxCanvas.MENU_MAIN, canvas.menuPage(), "toolbar returns to main menu");
    paint("toolbar to main menu");
    press(-6);
    eq(WreckboxCanvas.MENU_NONE, canvas.menuPage(), "return to game");
    paint("final gameplay");

    press(-6); down(5); press(Canvas.KEY_NUM5);
    eq(WreckboxCanvas.MENU_TOOLS, canvas.menuPage(), "main menu opens all tools");
    canvas.menuTouchStart(64, 100);
    check(canvas.menuTouchDrag(40), "tool browser drag scrolls down");
    eq(5, canvas.selectedTool(), "tool drag changes selection");
    canvas.menuTouchStart(64, 40);
    check(canvas.menuTouchDrag(100), "tool browser drag scrolls up");
    eq(1, canvas.selectedTool(), "tool drag restores hammer");
    down(5); paint("tool browser selects nuke");
    press(Canvas.KEY_NUM5);
    eq(6, canvas.selectedTool(), "tool browser applies nuke");
    eq(WreckboxCanvas.MENU_NONE, canvas.menuPage(), "tool browser returns to game");

    canvas.pointerPressed(64, 140);
    eq(WreckboxCanvas.MENU_TOOLS, canvas.menuPage(), "touch tool name opens browser");
    press(Canvas.KEY_NUM5);
    canvas.pointerPressed(2, 140);
    eq(5, canvas.selectedTool(), "touch left toolbar selects previous tool");
    canvas.pointerPressed(126, 140);
    eq(6, canvas.selectedTool(), "touch right toolbar selects next tool");
    canvas.pointerPressed(2, 2);
    eq(WreckboxCanvas.MENU_MAIN, canvas.menuPage(), "touch top panel opens main menu");
    press(-6);
    canvas.pointerPressed(126, 2);
    eq(WreckboxCanvas.MENU_CONTROL, canvas.menuPage(), "touch top right opens controls");
    paint("touch controls with detonate command");
    press(-6); press(-6);
    paint("final touch gameplay");
  }
}
