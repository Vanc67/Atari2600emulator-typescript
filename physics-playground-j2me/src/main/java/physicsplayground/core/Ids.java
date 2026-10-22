package physicsplayground.core;

public final class Ids {
    public static final String BUILTIN = "core";
    private Ids() {}

    public static boolean validSegment(String segment) {
        if (segment == null || segment.length() == 0) return false;
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_')) return false;
        }
        return true;
    }

    public static boolean valid(String id) {
        if (id == null) return false;
        int separator = id.indexOf(':');
        return separator > 0 && separator == id.lastIndexOf(':') && separator < id.length() - 1
            && validSegment(id.substring(0, separator)) && validSegment(id.substring(separator + 1));
    }

    public static String make(String namespace, String local) { return namespace + ":" + local; }
    public static String qualify(String id, String namespace) { return id != null && id.indexOf(':') >= 0 ? id : make(namespace, id == null ? "" : id); }
    public static String namespace(String id) { int at = id.indexOf(':'); return at < 0 ? id : id.substring(0, at); }
    public static String local(String id) { int at = id.indexOf(':'); return at < 0 ? id : id.substring(at + 1); }
}
