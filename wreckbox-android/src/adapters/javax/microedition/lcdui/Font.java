package javax.microedition.lcdui;

import android.graphics.Paint;
import android.graphics.Typeface;

/** Logical-pixel system font with the MIDP metrics used by the shared menus. */
public final class Font {
  public static final int STYLE_PLAIN = 0, STYLE_BOLD = 1, STYLE_ITALIC = 2, STYLE_UNDERLINED = 4;
  public static final int SIZE_SMALL = 8, SIZE_MEDIUM = 0, SIZE_LARGE = 16;
  public static final int FACE_SYSTEM = 0, FACE_MONOSPACE = 32, FACE_PROPORTIONAL = 64;
  public static final int FONT_STATIC_TEXT = 0, FONT_INPUT_TEXT = 1;
  private static final Font SMALL = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_SMALL);
  private static final Font DEFAULT = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
  final int face, style, size;
  final float textSize;
  final Typeface typeface;
  private final Paint measure = new Paint();
  private final Paint.FontMetricsInt metrics = new Paint.FontMetricsInt();

  private Font(int face, int style, int size) {
    this.face = face; this.style = style; this.size = size;
    textSize = size == SIZE_SMALL ? 10.0f : size == SIZE_LARGE ? 14.0f : 12.0f;
    int typefaceStyle = (style & STYLE_BOLD) != 0 && (style & STYLE_ITALIC) != 0 ? Typeface.BOLD_ITALIC
        : (style & STYLE_BOLD) != 0 ? Typeface.BOLD : (style & STYLE_ITALIC) != 0 ? Typeface.ITALIC : Typeface.NORMAL;
    typeface = Typeface.create(face == FACE_MONOSPACE ? Typeface.MONOSPACE : Typeface.DEFAULT, typefaceStyle);
    measure.setAntiAlias(false); measure.setTypeface(typeface); measure.setTextSize(textSize);
    measure.setUnderlineText((style & STYLE_UNDERLINED) != 0);
    measure.getFontMetricsInt(metrics);
  }
  public static Font getFont(int font) { return DEFAULT; }
  public static Font getDefaultFont() { return DEFAULT; }
  public static Font getFont(int face, int style, int size) {
    if (face == FACE_SYSTEM && style == STYLE_PLAIN && size == SIZE_SMALL) return SMALL;
    if (face == FACE_SYSTEM && style == STYLE_PLAIN && size == SIZE_MEDIUM) return DEFAULT;
    return new Font(face, style, size);
  }
  public int getStyle() { return style; }
  public int getSize() { return size; }
  public int getFace() { return face; }
  public boolean isPlain() { return style == STYLE_PLAIN; }
  public boolean isBold() { return (style & STYLE_BOLD) != 0; }
  public boolean isItalic() { return (style & STYLE_ITALIC) != 0; }
  public boolean isUnderlined() { return (style & STYLE_UNDERLINED) != 0; }
  public int getHeight() { return metrics.descent - metrics.ascent; }
  public int getBaselinePosition() { return -metrics.ascent; }
  public int charWidth(char value) { return (int)Math.ceil(measure.measureText(new char[] {value}, 0, 1)); }
  public int charsWidth(char[] values, int offset, int length) { return (int)Math.ceil(measure.measureText(values, offset, length)); }
  public int stringWidth(String value) { return (int)Math.ceil(measure.measureText(value)); }
  public int substringWidth(String value, int offset, int length) { return (int)Math.ceil(measure.measureText(value, offset, offset + length)); }
}
