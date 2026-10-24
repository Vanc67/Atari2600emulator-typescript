package javax.microedition.lcdui;

import android.view.View;

/** Small Android-backed LCDUI surface contract used by the shared Wreckbox UI. */
public abstract class Canvas {
  public static final int UP = 1, DOWN = 6, LEFT = 2, RIGHT = 5, FIRE = 8;
  public static final int GAME_A = 9, GAME_B = 10, GAME_C = 11, GAME_D = 12;
  public static final int KEY_NUM0 = 48, KEY_NUM1 = 49, KEY_NUM2 = 50, KEY_NUM3 = 51;
  public static final int KEY_NUM4 = 52, KEY_NUM5 = 53, KEY_NUM6 = 54, KEY_NUM7 = 55;
  public static final int KEY_NUM8 = 56, KEY_NUM9 = 57, KEY_STAR = 42, KEY_POUND = 35;
  private View repaintView;

  protected Canvas() { }
  public boolean isDoubleBuffered() { return true; }
  public boolean hasPointerEvents() { return true; }
  public boolean hasPointerMotionEvents() { return true; }
  public boolean hasRepeatEvents() { return true; }
  public int getKeyCode(int gameAction) {
    if (gameAction == UP) return KEY_NUM2;
    if (gameAction == DOWN) return KEY_NUM8;
    if (gameAction == LEFT) return KEY_NUM4;
    if (gameAction == RIGHT) return KEY_NUM6;
    if (gameAction == FIRE) return KEY_NUM5;
    return 0;
  }
  public String getKeyName(int keyCode) { return String.valueOf(keyCode); }
  public int getGameAction(int keyCode) {
    if (keyCode == KEY_NUM2) return UP;
    if (keyCode == KEY_NUM8) return DOWN;
    if (keyCode == KEY_NUM4) return LEFT;
    if (keyCode == KEY_NUM6) return RIGHT;
    if (keyCode == KEY_NUM5) return FIRE;
    return 0;
  }
  public void setFullScreenMode(boolean mode) { }
  public void setRepaintView(View view) { repaintView = view; }
  public final void repaint(int x, int y, int width, int height) { repaint(); }
  public final void repaint() { if (repaintView != null) repaintView.postInvalidate(); }
  public final void serviceRepaints() { repaint(); }
  protected void keyPressed(int keyCode) { }
  protected void keyRepeated(int keyCode) { }
  protected void keyReleased(int keyCode) { }
  protected void pointerPressed(int x, int y) { }
  protected void pointerReleased(int x, int y) { }
  protected void pointerDragged(int x, int y) { }
  protected void showNotify() { }
  protected void hideNotify() { }
  protected abstract void paint(Graphics graphics);
  protected void sizeChanged(int width, int height) { }
}
