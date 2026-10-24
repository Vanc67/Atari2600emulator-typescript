package javax.microedition.lcdui;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.IOException;
import java.io.InputStream;

/** Bitmap-backed immutable/mutable MIDP image, including PNG asset loading. */
public final class Image {
  private static AssetManager assets;
  final Bitmap bitmap;
  private final boolean mutable;

  private Image(Bitmap bitmap, boolean mutable) { this.bitmap = bitmap; this.mutable = mutable; }
  public static void configure(AssetManager manager) { assets = manager; }
  public static Image createImage(int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("image size");
    return new Image(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888), true);
  }
  public static Image createImage(String path) throws IOException {
    if (assets == null) throw new IOException("AssetManager is not configured");
    String name = path.startsWith("/") ? path.substring(1) : path;
    InputStream stream = assets.open(name, AssetManager.ACCESS_STREAMING);
    try {
      BitmapFactory.Options options = new BitmapFactory.Options();
      options.inScaled = false;
      Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
      if (bitmap == null) throw new IOException("Cannot decode " + path);
      return new Image(bitmap, false);
    } finally { stream.close(); }
  }
  public static Image createImage(byte[] data, int offset, int length) {
    Bitmap bitmap = BitmapFactory.decodeByteArray(data, offset, length);
    if (bitmap == null) throw new IllegalArgumentException("encoded image");
    return new Image(bitmap, false);
  }
  public static Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
    if (rgb == null || width <= 0 || height <= 0 || rgb.length < width * height) throw new IllegalArgumentException("rgb");
    int[] pixels = rgb;
    if (!processAlpha) {
      pixels = new int[rgb.length];
      for (int i = 0; i < rgb.length; i++) pixels[i] = rgb[i] | 0xff000000;
    }
    return new Image(Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888), false);
  }
  public int getWidth() { return bitmap.getWidth(); }
  public int getHeight() { return bitmap.getHeight(); }
  public Graphics getGraphics() {
    if (!mutable) throw new IllegalStateException("immutable image");
    return new Graphics(new android.graphics.Canvas(bitmap));
  }
  public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
    bitmap.getPixels(rgbData, offset, scanlength, x, y, width, height);
  }
}
