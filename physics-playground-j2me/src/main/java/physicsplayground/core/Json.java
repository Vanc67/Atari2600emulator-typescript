package physicsplayground.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

/** Small, allocation-conscious JSON reader/writer for CLDC 1.1.
 * Objects retain insertion order so save files and content diagnostics are deterministic.
 */
public final class Json {
    public static final Object NULL = new Object();

    public static final class Num {
        public final double value;
        public final boolean integral;
        private final String raw;

        public Num(double value) {
            this(value, Double.toString(value));
        }

        public Num(long value) {
            this.value = value;
            this.integral = true;
            this.raw = Long.toString(value);
        }

        private Num(double value, String raw) {
            this.value = value;
            this.raw = raw;
            this.integral = raw.indexOf('.') < 0 && raw.indexOf('e') < 0 && raw.indexOf('E') < 0;
        }

        public int intValue() { return (int) value; }
        public long longValue() { return (long) value; }
        public float floatValue() { return (float) value; }
        public double doubleValue() { return value; }
        public String toString() { return raw; }
    }

    /** Hashtable with Godot-Dictionary-like insertion order. */
    public static final class Map extends Hashtable {
        private final Vector order = new Vector();

        public Map() { super(); }
        public Map(int capacity) { super(capacity); }

        public Object put(Object key, Object value) {
            if (!containsKey(key)) order.addElement(key);
            return super.put(key, value);
        }

        public Object remove(Object key) {
            order.removeElement(key);
            return super.remove(key);
        }

        public Vector orderedKeys() { return order; }

        public Map orderedPut(String key, Object value) {
            put(key, value);
            return this;
        }
    }

    public static final class Exception extends IOException {
        public final int line;
        public final int column;

        Exception(String message, String source, int at) {
            super(message + " at line " + lineOf(source, at) + ", column " + columnOf(source, at));
            this.line = lineOf(source, at);
            this.column = columnOf(source, at);
        }

        private static int lineOf(String s, int at) {
            int line = 1;
            for (int i = 0; i < at && i < s.length(); i++) if (s.charAt(i) == '\n') line++;
            return line;
        }

        private static int columnOf(String s, int at) {
            int column = 1;
            for (int i = 0; i < at && i < s.length(); i++) {
                if (s.charAt(i) == '\n') column = 1;
                else column++;
            }
            return column;
        }
    }

    private static final class Parser {
        final String source;
        int at;

        Parser(String source) { this.source = source; }

        Object parse() throws Exception {
            skipSpace();
            Object value = readValue();
            skipSpace();
            if (at != source.length()) throw error("Unexpected trailing data");
            return value;
        }

        Object readValue() throws Exception {
            if (at >= source.length()) throw error("Expected a value");
            char c = source.charAt(at);
            if (c == '{') return readObject();
            if (c == '[') return readArray();
            if (c == '"') return readString();
            if (c == 't') { literal("true"); return Boolean.TRUE; }
            if (c == 'f') { literal("false"); return Boolean.FALSE; }
            if (c == 'n') { literal("null"); return NULL; }
            if (c == '-' || (c >= '0' && c <= '9')) return readNumber();
            throw error("Expected a value");
        }

        Object readObject() throws Exception {
            at++;
            Map result = new Map();
            skipSpace();
            if (take('}')) return result;
            while (true) {
                skipSpace();
                if (at >= source.length() || source.charAt(at) != '"') throw error("Expected an object key");
                String key = readString();
                skipSpace();
                if (!take(':')) throw error("Expected ':'");
                skipSpace();
                result.put(key, readValue());
                skipSpace();
                if (take('}')) return result;
                if (!take(',')) throw error("Expected ',' or '}'");
            }
        }

        Vector readArray() throws Exception {
            at++;
            Vector result = new Vector();
            skipSpace();
            if (take(']')) return result;
            while (true) {
                skipSpace();
                result.addElement(readValue());
                skipSpace();
                if (take(']')) return result;
                if (!take(',')) throw error("Expected ',' or ']'");
            }
        }

        String readString() throws Exception {
            at++;
            StringBuffer out = new StringBuffer();
            while (at < source.length()) {
                char c = source.charAt(at++);
                if (c == '"') return out.toString();
                if (c == '\\') {
                    if (at >= source.length()) throw error("Unfinished escape");
                    char escaped = source.charAt(at++);
                    switch (escaped) {
                        case '"': out.append('"'); break;
                        case '\\': out.append('\\'); break;
                        case '/': out.append('/'); break;
                        case 'b': out.append('\b'); break;
                        case 'f': out.append('\f'); break;
                        case 'n': out.append('\n'); break;
                        case 'r': out.append('\r'); break;
                        case 't': out.append('\t'); break;
                        case 'u':
                            if (at + 4 > source.length()) throw error("Short Unicode escape");
                            int value = 0;
                            for (int i = 0; i < 4; i++) {
                                char h = source.charAt(at++);
                                int digit = Character.digit(h, 16);
                                if (digit < 0) throw error("Invalid Unicode escape");
                                value = (value << 4) | digit;
                            }
                            out.append((char) value);
                            break;
                        default: throw error("Invalid escape");
                    }
                } else {
                    if (c < 0x20) throw error("Control character in string");
                    out.append(c);
                }
            }
            throw error("Unterminated string");
        }

        Num readNumber() throws Exception {
            int start = at;
            if (source.charAt(at) == '-') at++;
            if (at >= source.length()) throw error("Invalid number");
            if (source.charAt(at) == '0') at++;
            else {
                if (!digit()) throw error("Invalid number");
                while (at < source.length() && digit()) at++;
            }
            if (at < source.length() && source.charAt(at) == '.') {
                at++;
                if (at >= source.length() || !digit()) throw error("Invalid fraction");
                while (at < source.length() && digit()) at++;
            }
            if (at < source.length() && (source.charAt(at) == 'e' || source.charAt(at) == 'E')) {
                at++;
                if (at < source.length() && (source.charAt(at) == '+' || source.charAt(at) == '-')) at++;
                if (at >= source.length() || !digit()) throw error("Invalid exponent");
                while (at < source.length() && digit()) at++;
            }
            String raw = source.substring(start, at);
            double value;
            try { value = Double.parseDouble(raw); }
            catch (NumberFormatException e) { throw error("Invalid number"); }
            if (Double.isNaN(value) || Double.isInfinite(value)) throw error("Non-finite number");
            return new Num(value, raw);
        }

        boolean digit() { return source.charAt(at) >= '0' && source.charAt(at) <= '9'; }

        void literal(String text) throws Exception {
            if (!source.regionMatches(false, at, text, 0, text.length())) throw error("Invalid literal");
            at += text.length();
        }

        void skipSpace() {
            while (at < source.length()) {
                char c = source.charAt(at);
                if (c == ' ' || c == '\t' || c == '\r' || c == '\n') at++;
                else break;
            }
        }

        boolean take(char c) {
            if (at < source.length() && source.charAt(at) == c) { at++; return true; }
            return false;
        }

        Exception error(String message) { return new Exception(message, source, at); }
    }

    private Json() {}

    public static Object parse(String source) throws Exception {
        if (source == null) throw new NullPointerException("source");
        return new Parser(source).parse();
    }

    public static Object readResource(String path) throws IOException {
        InputStream in = Json.class.getResourceAsStream(path);
        if (in == null) throw new IOException("Missing resource " + path);
        try {
            return parse(readUtf8(in));
        } catch (Exception e) {
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException(path + ": " + e.getMessage());
        } finally {
            in.close();
        }
    }

    public static String readUtf8(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, "UTF-8");
        StringBuffer text = new StringBuffer();
        char[] buffer = new char[2048];
        int count;
        while ((count = reader.read(buffer)) >= 0) text.append(buffer, 0, count);
        return text.toString();
    }

    public static byte[] readBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int count;
        while ((count = in.read(buffer)) >= 0) out.write(buffer, 0, count);
        return out.toByteArray();
    }

    public static Map object(Object value) { return value instanceof Map ? (Map) value : null; }
    public static Vector array(Object value) { return value instanceof Vector ? (Vector) value : null; }
    public static Object get(Object object, String key) {
        Map map = object(object);
        return map == null ? null : map.get(key);
    }
    public static Map requireObject(Object value) throws Exception {
        Map map = object(value);
        if (map == null) throw new Exception("Expected an object", "", 0);
        return map;
    }
    public static String string(Object value, String fallback) { return value instanceof String ? (String) value : fallback; }
    public static double number(Object value, double fallback) { return value instanceof Json.Num ? ((Json.Num) value).doubleValue() : fallback; }
    public static int integer(Object value, int fallback) { return value instanceof Json.Num ? ((Json.Num) value).intValue() : fallback; }
    public static boolean bool(Object value, boolean fallback) { return value instanceof Boolean ? ((Boolean) value).booleanValue() : fallback; }
    public static Num num(double value) { return new Num(value); }
    public static Num num(long value) { return new Num(value); }
    public static Object bool(boolean value) { return value ? Boolean.TRUE : Boolean.FALSE; }

    public static Object deepCopy(Object value) {
        if (value instanceof Map) {
            Map source = (Map) value;
            Map copy = new Map(source.size());
            for (int i = 0; i < source.order.size(); i++) {
                Object key = source.order.elementAt(i);
                copy.put(key, deepCopy(source.get(key)));
            }
            return copy;
        }
        if (value instanceof Vector) {
            Vector source = (Vector) value;
            Vector copy = new Vector(source.size());
            for (int i = 0; i < source.size(); i++) copy.addElement(deepCopy(source.elementAt(i)));
            return copy;
        }
        return value;
    }

    public static String write(Object value) { return write(value, true); }

    public static String write(Object value, boolean pretty) {
        StringBuffer out = new StringBuffer();
        writeValue(value, out, pretty, 0);
        return out.toString();
    }

    private static void writeValue(Object value, StringBuffer out, boolean pretty, int depth) {
        if (value == null || value == NULL) { out.append("null"); return; }
        if (value instanceof String) { quote((String) value, out); return; }
        if (value instanceof Boolean || value instanceof Json.Num) { out.append(value.toString()); return; }
        if (value instanceof Map) {
            Map map = (Map) value;
            out.append('{');
            for (int i = 0; i < map.order.size(); i++) {
                if (i > 0) out.append(',');
                if (pretty) newline(out, depth + 1);
                Object key = map.order.elementAt(i);
                quote(key.toString(), out);
                out.append(pretty ? ": " : ":");
                writeValue(map.get(key), out, pretty, depth + 1);
            }
            if (pretty && !map.order.isEmpty()) newline(out, depth);
            out.append('}');
            return;
        }
        if (value instanceof Vector) {
            Vector array = (Vector) value;
            out.append('[');
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) out.append(',');
                if (pretty) newline(out, depth + 1);
                writeValue(array.elementAt(i), out, pretty, depth + 1);
            }
            if (pretty && !array.isEmpty()) newline(out, depth);
            out.append(']');
            return;
        }
        quote(value.toString(), out);
    }

    private static void newline(StringBuffer out, int depth) {
        out.append('\n');
        for (int i = 0; i < depth * 2; i++) out.append(' ');
    }

    private static void quote(String text, StringBuffer out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        out.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) out.append('0');
                        out.append(hex);
                    } else out.append(c);
            }
        }
        out.append('"');
    }
}
