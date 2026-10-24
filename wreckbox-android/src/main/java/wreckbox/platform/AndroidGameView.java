package wreckbox.platform;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

/** Logical 160-line surface, touch/key mapping and lifecycle for Gingerbread. */
public final class AndroidGameView extends View {
  private final Activity activity;
  private final WreckboxCanvas gameCanvas;
  private final Paint scalePaint = new Paint();
  private final Rect destination = new Rect();
  private Bitmap frame;
  private Graphics frameGraphics;
  private int logicalWidth = 128, logicalHeight = 160;
  private float scale = 1.0f;
  private boolean pinching, menuTouch, menuMoved;
  private float pinchDistance;

  public AndroidGameView(Activity activity) {
    super(activity);
    this.activity = activity;
    setFocusable(true); setFocusableInTouchMode(true);
    scalePaint.setFilterBitmap(false); scalePaint.setDither(false);
    gameCanvas = new WreckboxCanvas(new AndroidResourceLoader(activity.getAssets()));
    gameCanvas.setRepaintView(this);
  }
  public void startGame() { gameCanvas.start(); requestFocus(); }
  public void stopGame() { gameCanvas.stop(); }

  protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
    super.onSizeChanged(width, height, oldWidth, oldHeight);
    if (width <= 0 || height <= 0) return;
    int wanted = Math.max(128, Math.min(240, (width * logicalHeight + height / 2) / height));
    if (frame == null || wanted != logicalWidth) {
      logicalWidth = wanted;
      if (frame != null) frame.recycle();
      frame = Bitmap.createBitmap(logicalWidth, logicalHeight, Bitmap.Config.ARGB_8888);
      frameGraphics = new Graphics(new android.graphics.Canvas(frame));
      gameCanvas.sizeChanged(logicalWidth, logicalHeight);
    }
    scale = Math.min(width / (float)logicalWidth, height / (float)logicalHeight);
    int scaledWidth = Math.max(1, (int)(logicalWidth * scale + 0.5f));
    int scaledHeight = Math.max(1, (int)(logicalHeight * scale + 0.5f));
    destination.set((width - scaledWidth) / 2, (height - scaledHeight) / 2,
        (width + scaledWidth) / 2, (height + scaledHeight) / 2);
    scale = Math.min(destination.width() / (float)logicalWidth, destination.height() / (float)logicalHeight);
  }
  protected void onDraw(android.graphics.Canvas canvas) {
    canvas.drawColor(0xff05080b);
    if (frame == null || frameGraphics == null) return;
    frameGraphics.setClip(0, 0, logicalWidth, logicalHeight);
    gameCanvas.paint(frameGraphics);
    canvas.drawBitmap(frame, null, destination, scalePaint);
  }

  private int logicalX(MotionEvent event) {
    return (int)((event.getX() - destination.left) / Math.max(0.001f, scale));
  }
  private int logicalY(MotionEvent event) {
    return (int)((event.getY() - destination.top) / Math.max(0.001f, scale));
  }
  private boolean outside(MotionEvent event) {
    return event.getX() < destination.left || event.getX() >= destination.right
        || event.getY() < destination.top || event.getY() >= destination.bottom;
  }
  private float distance(MotionEvent event) {
    float dx = event.getX(0) - event.getX(1), dy = event.getY(0) - event.getY(1);
    return (float)Math.sqrt(dx * dx + dy * dy);
  }
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getAction() & MotionEvent.ACTION_MASK;
    if (action == MotionEvent.ACTION_POINTER_DOWN && event.getPointerCount() >= 2
        && gameCanvas.menuPage() == WreckboxCanvas.MENU_NONE && gameCanvas.selectedTool() == 0) {
      pinching = true; pinchDistance = distance(event); return true;
    }
    if (action == MotionEvent.ACTION_MOVE && pinching && event.getPointerCount() >= 2) {
      float next = distance(event);
      if (next > 2.0f && pinchDistance > 2.0f) gameCanvas.zoomBy(next / pinchDistance);
      pinchDistance = next; return true;
    }
    if (action == MotionEvent.ACTION_POINTER_UP) {
      if (event.getPointerCount() <= 2) pinching = false;
      return true;
    }
    if (pinching) {
      if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) pinching = false;
      return true;
    }
    if (action == MotionEvent.ACTION_DOWN) {
      requestFocus();
      menuTouch = gameCanvas.menuPage() != WreckboxCanvas.MENU_NONE; menuMoved = false;
      if (menuTouch) gameCanvas.menuTouchStart(logicalX(event), logicalY(event));
      else if (!outside(event)) gameCanvas.pointerPressed(logicalX(event), logicalY(event));
      return true;
    }
    if (action == MotionEvent.ACTION_MOVE) {
      if (menuTouch) { if (gameCanvas.menuTouchDrag(logicalY(event))) menuMoved = true; }
      else gameCanvas.pointerDragged(logicalX(event), logicalY(event));
      return true;
    }
    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      if (menuTouch) {
        if (action == MotionEvent.ACTION_UP && !menuMoved && !outside(event))
          gameCanvas.pointerPressed(logicalX(event), logicalY(event));
        menuTouch = false;
      } else gameCanvas.pointerReleased(logicalX(event), logicalY(event));
      return true;
    }
    return true;
  }

  private int mapKey(int keyCode) {
    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) return Canvas.KEY_NUM2;
    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) return Canvas.KEY_NUM8;
    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return Canvas.KEY_NUM4;
    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return Canvas.KEY_NUM6;
    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
        || keyCode == KeyEvent.KEYCODE_SPACE) return Canvas.KEY_NUM5;
    if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
      return Canvas.KEY_NUM0 + keyCode - KeyEvent.KEYCODE_0;
    if (keyCode == KeyEvent.KEYCODE_STAR) return Canvas.KEY_STAR;
    if (keyCode == KeyEvent.KEYCODE_POUND) return Canvas.KEY_POUND;
    if (keyCode == KeyEvent.KEYCODE_MENU) return -6;
    if (keyCode == KeyEvent.KEYCODE_P) return -7;
    return 0;
  }
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
      if (event.getRepeatCount() == 0) {
        if (gameCanvas.menuPage() == WreckboxCanvas.MENU_MAIN) activity.finish();
        else gameCanvas.keyPressed(-6);
      }
      return true;
    }
    int mapped = mapKey(keyCode);
    if (mapped == 0) return super.onKeyDown(keyCode, event);
    if (event.getRepeatCount() == 0) gameCanvas.keyPressed(mapped);
    else gameCanvas.keyRepeated(mapped);
    return true;
  }
  public boolean onKeyUp(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) return true;
    int mapped = mapKey(keyCode);
    if (mapped == 0) return super.onKeyUp(keyCode, event);
    gameCanvas.keyReleased(mapped); return true;
  }
}
