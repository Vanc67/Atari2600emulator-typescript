package javax.microedition.lcdui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;

/** Software Canvas implementation of the LCDUI operations used by Wreckbox. */
public final class Graphics {
  public static final int HCENTER = 1, VCENTER = 2, LEFT = 4, RIGHT = 8;
  public static final int TOP = 16, BOTTOM = 32, BASELINE = 64;
  public static final int SOLID = 0, DOTTED = 1;
  private final Canvas canvas;
  private final Paint paint = new Paint();
  private final Rect source = new Rect(), destination = new Rect();
  private final RectF arc = new RectF();
  private final Paint.FontMetrics textMetrics = new Paint.FontMetrics();
  private final Path triangle = new Path();
  private Font font = Font.getDefaultFont();
  private int color, clipSave = -1;

  public Graphics(Canvas canvas) {
    this.canvas = canvas;
    paint.setAntiAlias(false); paint.setStrokeWidth(1.0f); setColor(0);
  }
  public int getColor() { return color; }
  public int getRedComponent() { return (color >> 16) & 255; }
  public int getGreenComponent() { return (color >> 8) & 255; }
  public int getBlueComponent() { return color & 255; }
  public void setColor(int red, int green, int blue) {
    setColor(((red & 255) << 16) | ((green & 255) << 8) | (blue & 255));
  }
  public void setColor(int rgb) {
    color = rgb & 0xffffff;
    paint.setColor(0xff000000 | color); paint.setAlpha(255);
  }
  public void setGrayScale(int value) { setColor(value, value, value); }
  public int getGrayScale() { return (getRedComponent() + getGreenComponent() + getBlueComponent()) / 3; }
  public Font getFont() { return font; }
  public void setFont(Font value) { font = value == null ? Font.getDefaultFont() : value; }
  public void setStrokeStyle(int style) { }
  public int getStrokeStyle() { return SOLID; }
  public void setClip(int x, int y, int width, int height) {
    if (clipSave >= 0) canvas.restoreToCount(clipSave);
    clipSave = canvas.save();
    canvas.clipRect(x, y, x + width, y + height);
  }
  public void clipRect(int x, int y, int width, int height) { canvas.clipRect(x, y, x + width, y + height); }
  public void drawLine(int x1, int y1, int x2, int y2) {
    paint.setStyle(Paint.Style.STROKE); canvas.drawLine(x1, y1, x2, y2, paint);
  }
  public void fillRect(int x, int y, int width, int height) {
    if (width <= 0 || height <= 0) return;
    paint.setStyle(Paint.Style.FILL); canvas.drawRect(x, y, x + width, y + height, paint);
  }
  public void drawRect(int x, int y, int width, int height) {
    if (width < 0 || height < 0) return;
    paint.setStyle(Paint.Style.STROKE); canvas.drawRect(x, y, x + width, y + height, paint);
  }
  public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    arc.set(x, y, x + width, y + height); paint.setStyle(Paint.Style.FILL);
    canvas.drawArc(arc, -startAngle, -arcAngle, true, paint);
  }
  public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
    arc.set(x, y, x + width, y + height); paint.setStyle(Paint.Style.STROKE);
    canvas.drawArc(arc, -startAngle, -arcAngle, false, paint);
  }
  public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
    triangle.reset(); triangle.moveTo(x1, y1); triangle.lineTo(x2, y2); triangle.lineTo(x3, y3); triangle.close();
    paint.setStyle(Paint.Style.FILL); canvas.drawPath(triangle, paint);
  }
  public void drawString(String text, int x, int y, int anchor) {
    drawText(text, 0, text.length(), x, y, anchor);
  }
  public void drawSubstring(String text, int offset, int length, int x, int y, int anchor) {
    drawText(text, offset, offset + length, x, y, anchor);
  }
  public void drawChar(char value, int x, int y, int anchor) {
    char[] chars = {value}; drawChars(chars, 0, 1, x, y, anchor);
  }
  public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
    drawString(new String(data, offset, length), x, y, anchor);
  }
  private void drawText(String text, int start, int end, int x, int y, int anchor) {
    paint.setStyle(Paint.Style.FILL); paint.setTypeface(font.typeface); paint.setTextSize(font.textSize);
    paint.setUnderlineText(font.isUnderlined());
    float textWidth = paint.measureText(text, start, end);
    float drawX = (anchor & HCENTER) != 0 ? x - textWidth * 0.5f : (anchor & RIGHT) != 0 ? x - textWidth : x;
    paint.getFontMetrics(textMetrics);
    float baseline = (anchor & BASELINE) != 0 ? y : (anchor & BOTTOM) != 0 ? y - textMetrics.descent
        : (anchor & VCENTER) != 0 ? y - (textMetrics.ascent + textMetrics.descent) * 0.5f : y - textMetrics.ascent;
    canvas.drawText(text, start, end, drawX, baseline, paint);
  }
  public void drawImage(Image image, int x, int y, int anchor) {
    int drawX = (anchor & HCENTER) != 0 ? x - image.getWidth() / 2 : (anchor & RIGHT) != 0 ? x - image.getWidth() : x;
    int drawY = (anchor & VCENTER) != 0 ? y - image.getHeight() / 2 : (anchor & BOTTOM) != 0 ? y - image.getHeight() : y;
    paint.setStyle(Paint.Style.FILL); canvas.drawBitmap(image.bitmap, drawX, drawY, paint);
  }
  public void drawRegion(Image image, int sx, int sy, int width, int height, int transform,
      int x, int y, int anchor) {
    if (transform != 0) throw new IllegalArgumentException("only TRANS_NONE is used");
    int drawX = (anchor & HCENTER) != 0 ? x - width / 2 : (anchor & RIGHT) != 0 ? x - width : x;
    int drawY = (anchor & VCENTER) != 0 ? y - height / 2 : (anchor & BOTTOM) != 0 ? y - height : y;
    source.set(sx, sy, sx + width, sy + height); destination.set(drawX, drawY, drawX + width, drawY + height);
    paint.setStyle(Paint.Style.FILL); canvas.drawBitmap(image.bitmap, source, destination, paint);
  }
  public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y,
      int width, int height, boolean processAlpha) {
    paint.setStyle(Paint.Style.FILL);
    canvas.drawBitmap(rgbData, offset, scanlength, x, y, width, height, processAlpha, paint);
  }
  public int getDisplayColor(int rgb) { return rgb & 0xffffff; }
}
