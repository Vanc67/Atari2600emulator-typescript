package physicsplayground.core;

import java.util.Vector;

/** Typed and ranged JSON field reader mirroring the original DataReader diagnostics. */
public final class ContentReader {
    public final String context;
    public final Vector errors = new Vector();
    public final Vector warnings = new Vector();

    public ContentReader(String context) { this.context = context; }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public void error(String message) { errors.addElement(context + ": " + message); }
    public void warn(String message) { warnings.addElement(context + ": " + message); }

    public String string(Json.Map data, String key, String fallback, boolean required) {
        if (!data.containsKey(key)) {
            if (required) error("missing required field '" + key + "'");
            return fallback;
        }
        Object value = data.get(key);
        if (value instanceof String) return (String) value;
        error("field '" + key + "' must be a string");
        return fallback;
    }

    public String string(Json.Map data, String key, String fallback) { return string(data, key, fallback, false); }

    public double floating(Json.Map data, String key, double fallback, double minimum, double maximum) {
        if (!data.containsKey(key)) return fallback;
        Object value = data.get(key);
        if (!number(value)) {
            error("field '" + key + "' must be a number");
            return fallback;
        }
        double number = ((Json.Num) value).doubleValue();
        if (number < minimum || number > maximum) {
            error("field '" + key + "' = " + number + " is outside the allowed range [" + minimum + ", " + maximum + "]");
            return Math.max(minimum, Math.min(maximum, number));
        }
        return number;
    }

    public double floating(Json.Map data, String key, double fallback) {
        return floating(data, key, fallback, -Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public int integer(Json.Map data, String key, int fallback) {
        if (!data.containsKey(key)) return fallback;
        Object value = data.get(key);
        if (!number(value)) {
            error("field '" + key + "' must be an integer");
            return fallback;
        }
        return ((Json.Num) value).intValue();
    }

    public boolean bool(Json.Map data, String key, boolean fallback) {
        if (!data.containsKey(key)) return fallback;
        Object value = data.get(key);
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        error("field '" + key + "' must be true or false");
        return fallback;
    }

    public Vec2 vector(Json.Map data, String key, Vec2 fallback) {
        if (!data.containsKey(key)) return fallback == null ? null : fallback.copy();
        Object value = data.get(key);
        Vector array = Json.array(value);
        if (array != null && array.size() == 2 && number(array.elementAt(0)) && number(array.elementAt(1))) {
            return new Vec2(((Json.Num) array.elementAt(0)).floatValue(), ((Json.Num) array.elementAt(1)).floatValue());
        }
        error("field '" + key + "' must be an array of two numbers");
        return fallback == null ? null : fallback.copy();
    }

    public ColorValue color(Json.Map data, String key, ColorValue fallback) {
        if (!data.containsKey(key)) return fallback;
        Object value = data.get(key);
        ColorValue color = value instanceof String ? ColorValue.html((String) value) : null;
        if (color != null) return color;
        error("field '" + key + "' must be a hex color string such as \"#8a6a44\"");
        return fallback;
    }

    public Json.Map map(Json.Map data, String key) {
        if (!data.containsKey(key)) return new Json.Map();
        Object value = data.get(key);
        if (value instanceof Json.Map) return (Json.Map) value;
        error("field '" + key + "' must be an object");
        return new Json.Map();
    }

    public Vector strings(Json.Map data, String key) {
        Vector result = new Vector();
        if (!data.containsKey(key)) return result;
        Vector value = Json.array(data.get(key));
        if (value == null) {
            error("field '" + key + "' must be an array of strings");
            return result;
        }
        for (int i = 0; i < value.size(); i++) {
            Object item = value.elementAt(i);
            if (item instanceof String) result.addElement(item);
            else error("field '" + key + "' must contain only strings");
        }
        return result;
    }

    public static boolean number(Object value) {
        if (!(value instanceof Json.Num)) return false;
        double number = ((Json.Num) value).doubleValue();
        return !Double.isNaN(number) && !Double.isInfinite(number);
    }

    public static boolean integral(Object value) {
        if (!number(value)) return false;
        if (value instanceof Json.Num && ((Json.Num) value).integral) return true;
        double number = ((Json.Num) value).doubleValue();
        return Math.floor(number) == number;
    }

    public void collectInto(Vector errorTarget, Vector warningTarget) {
        for (int i = 0; i < errors.size(); i++) errorTarget.addElement(errors.elementAt(i));
        for (int i = 0; i < warnings.size(); i++) warningTarget.addElement(warnings.elementAt(i));
    }
}
