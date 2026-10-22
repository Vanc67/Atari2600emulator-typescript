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
    paint("game 128x160");
    press(-6); paint("original main menu");
    press(Canvas.KEY_NUM8); press(Canvas.KEY_NUM6); paint("map arrows rebuild selection");
    press(Canvas.KEY_NUM8); press(Canvas.KEY_NUM5); paint("settings page");
    for (int i = 0; i < 6; i++) { press(Canvas.KEY_NUM5); press(Canvas.KEY_NUM8); }
    paint("all settings toggles");
    press(-6); down(3); press(Canvas.KEY_NUM5); paint("help page");
    down(35); paint("help scroll to end");
    press(Canvas.KEY_NUM5); down(4); press(Canvas.KEY_NUM5); paint("about page");
    down(25); paint("about scroll to end");
    press(Canvas.KEY_NUM5); press(-7); press(Canvas.KEY_STAR); paint("simulation toolbar");
    down(4); press(Canvas.KEY_NUM5); paint("stress toggle");
    down(2); press(Canvas.KEY_NUM5); paint("blood toggle");
    down(1); press(Canvas.KEY_NUM5); paint("clear debris command");
    press(Canvas.KEY_STAR); down(11); press(Canvas.KEY_NUM5); paint("toolbar to main menu");
    press(-7); paint("return to game");
  }
}
