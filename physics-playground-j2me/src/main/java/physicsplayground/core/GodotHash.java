package physicsplayground.core;

/** Hash functions used by Godot's global hash() for Int and ASCII String variants. */
public final class GodotHash {
    private GodotHash() {}
    public static int integer(long value) {
        long hash = (~value) + (value << 18); hash ^= hash >>> 31; hash *= 21;
        hash ^= hash >>> 11; hash += hash << 6; hash ^= hash >>> 22; return (int) hash;
    }
    public static int string(String value) {
        int hash = 5381;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c < 128) hash = (hash << 5) + hash ^ c;
            else if (c < 2048) { hash = (hash << 5) + hash ^ (192 | c >>> 6); hash = (hash << 5) + hash ^ (128 | c & 63); }
            else { hash = (hash << 5) + hash ^ (224 | c >>> 12); hash = (hash << 5) + hash ^ (128 | c >>> 6 & 63); hash = (hash << 5) + hash ^ (128 | c & 63); }
        }
        return hash;
    }
}
