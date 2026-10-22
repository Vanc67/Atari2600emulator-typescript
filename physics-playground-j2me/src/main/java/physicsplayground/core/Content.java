package physicsplayground.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

/** Resource-based counterpart of ModLoader. Additional mods use the same JSON schema and an index file. */
public final class Content {
    public static final int MAX_MODS = 64;
    public static final int MAX_FILE_BYTES = 1024 * 1024;
    public static final int MAX_DEFINITIONS_PER_MOD = 1000;
    public static final int MAX_AUDIO_BYTES = 32 * 1024 * 1024;

    public final Definitions.Registry registry = new Definitions.Registry();
    public final Vector errors = new Vector();
    public final Vector warnings = new Vector();
    private final Hashtable definitionCounts = new Hashtable();
    private int audioBytes;

    public static Content loadDefault() throws IOException {
        Content content = new Content();
        content.loadResources("/mods/core.index");
        return content;
    }

    public void loadResources(String indexPath) throws IOException {
        InputStream in = Content.class.getResourceAsStream(indexPath);
        if (in == null) throw new IOException("Missing mod index " + indexPath);
        String index;
        try { index = Json.readUtf8(in); } finally { in.close(); }
        Vector paths = new Vector();
        int lineStart = 0;
        while (lineStart < index.length()) {
            int lineEnd = index.indexOf('\n', lineStart);
            if (lineEnd < 0) lineEnd = index.length();
            String path = index.substring(lineStart, lineEnd).trim();
            if (path.length() > 0 && !path.startsWith("#")) paths.addElement(path);
            lineStart = lineEnd + 1;
        }
        Vector mods = discover(paths);
        for (int i = 0; i < mods.size(); i++) registry.registerMod((Definitions.Mod) mods.elementAt(i));
        for (int i = 0; i < mods.size(); i++) {
            Definitions.Mod mod = (Definitions.Mod) mods.elementAt(i);
            loadKind(mod, paths, "materials", "materials", "material");
        }
        for (int i = 0; i < mods.size(); i++) {
            Definitions.Mod mod = (Definitions.Mod) mods.elementAt(i);
            loadKind(mod, paths, "projectiles", "projectiles", "projectile");
            loadKind(mod, paths, "sounds", "sounds", "sound");
        }
        for (int i = 0; i < mods.size(); i++) {
            Definitions.Mod mod = (Definitions.Mod) mods.elementAt(i);
            loadKind(mod, paths, "entities", "entities", "entity");
        }
        linkAssemblies();
        linkComponents();
    }

    private Vector discover(Vector paths) throws IOException {
        Vector result = new Vector();
        Hashtable seen = new Hashtable();
        for (int i = 0; i < paths.size(); i++) {
            String path = (String) paths.elementAt(i);
            if (!path.endsWith("/mod.json")) continue;
            Object data = readJson(path);
            if (!(data instanceof Json.Map)) { errors.addElement(path + ": manifest must be a JSON object"); continue; }
            String directory = path.substring(0, path.length() - "mod.json".length() - 1);
            ContentReader reader = new ContentReader(path);
            Definitions.Mod mod = Definitions.Mod.from((Json.Map) data, "res://" + directory, reader);
            collect(reader);
            if (reader.hasErrors()) continue;
            if (seen.containsKey(mod.id)) { errors.addElement(directory + ": duplicate mod id '" + mod.id + "' (already loaded from " + seen.get(mod.id) + ")"); continue; }
            if (Ids.BUILTIN.equals(mod.id) && !mod.builtin) { errors.addElement(directory + ": the mod id 'core' is reserved for built-in content"); continue; }
            if (result.size() >= MAX_MODS) { errors.addElement(directory + ": more than " + MAX_MODS + " mods installed; skipped"); continue; }
            seen.put(mod.id, directory);
            result.addElement(mod);
        }
        for (int i = 0; i < result.size(); i++) {
            Definitions.Mod mod = (Definitions.Mod) result.elementAt(i);
            if (Ids.BUILTIN.equals(mod.id) && i > 0) {
                result.removeElementAt(i);
                result.insertElementAt(mod, 0);
                break;
            }
        }
        return result;
    }

    private void loadKind(Definitions.Mod mod, Vector paths, String folder, String listKey, String kind) throws IOException {
        String resourceDirectory = mod.directory.startsWith("res://") ? mod.directory.substring(6) : mod.directory;
        String prefix = resourceDirectory + "/" + folder + "/";
        for (int i = 0; i < paths.size(); i++) {
            String path = (String) paths.elementAt(i);
            if (!path.startsWith(prefix) || !path.endsWith(".json")) continue;
            Object data = readJson(path);
            Vector entries = entries(data, listKey);
            for (int j = 0; j < entries.size(); j++) {
                Object raw = entries.elementAt(j);
                ContentReader reader = new ContentReader(context(mod, path, kind, j, raw));
                if (!withinLimit(mod, reader)) { collect(reader); break; }
                if (!(raw instanceof Json.Map)) { reader.error("entry must be a JSON object"); collect(reader); continue; }
                Json.Map entry = (Json.Map) raw;
                boolean registered = true;
                String id = "";
                if ("material".equals(kind)) {
                    Definitions.Material value = Definitions.Material.from(entry, mod.id, reader);
                    id = value.id;
                    if (!reader.hasErrors()) registered = registry.registerMaterial(value);
                } else if ("projectile".equals(kind)) {
                    Definitions.Projectile value = Definitions.Projectile.from(entry, mod.id, reader);
                    id = value.id;
                    if (!reader.hasErrors()) registered = registry.registerProjectile(value);
                } else if ("sound".equals(kind)) {
                    Definitions.Sound value = Definitions.Sound.from(entry, mod.id, reader);
                    id = value.id;
                    int bytes = (int) (value.duration * 22050) * 2;
                    if (audioBytes + bytes > MAX_AUDIO_BYTES) reader.error("synthesized audio budget exceeded (32 MiB total); sound not loaded");
                    if (!reader.hasErrors()) {
                        registered = registry.registerSound(value);
                        if (registered) audioBytes += bytes;
                    }
                } else {
                    Definitions.Entity value = Definitions.Entity.from(entry, mod.id, reader);
                    id = value.id;
                    validateEntity(value, reader);
                    if (!reader.hasErrors()) registered = registry.registerEntity(value);
                }
                collect(reader);
                if (!registered) errors.addElement(path + ": duplicate " + kind + " id '" + id + "'");
            }
        }
    }

    private Object readJson(String path) throws IOException {
        Object data = Json.readResource("/" + path);
        return data;
    }

    private static Vector entries(Object data, String key) {
        if (data instanceof Vector) return (Vector) data;
        Vector result = new Vector();
        if (data instanceof Json.Map) {
            Object listed = ((Json.Map) data).get(key);
            if (listed instanceof Vector) return (Vector) listed;
            result.addElement(data);
        }
        return result;
    }

    private void validateEntity(Definitions.Entity entity, ContentReader reader) {
        if (entity.isAssembly()) {
            String controller = entity.assembly.controllerType;
            if (controller.length() > 0 && !"human".equals(controller)) {
                reader.warn("unknown assembly controller '" + controller + "' (ignored)");
                entity.assembly.controllerType = "";
            }
            return;
        }
        if (!registry.hasMaterial(entity.materialId)) reader.error("unknown material '" + entity.materialId + "'");
        Schemas.validate(entity.components, entity.sourceMod, registry, reader);
        if (!knownVisual(entity.visualStyle)) reader.warn("unknown visual style '" + entity.visualStyle + "' (drawn as 'plain')");
    }

    private void linkAssemblies() {
        Vector assemblies = registry.assemblies();
        for (int i = 0; i < assemblies.size(); i++) {
            Definitions.Entity entity = (Definitions.Entity) assemblies.elementAt(i);
            ContentReader reader = new ContentReader(entity.id + " (assembly)");
            entity.assembly.link(registry, reader);
            collect(reader);
            if (reader.hasErrors()) registry.unregisterEntity(entity.id);
        }
    }

    private void linkComponents() {
        Vector all = registry.allEntities();
        for (int i = 0; i < all.size(); i++) {
            Definitions.Entity entity = (Definitions.Entity) all.elementAt(i);
            Object raw = entity.components.get("destructible");
            if (raw instanceof Json.Map) {
                Json.Map destructible = (Json.Map) raw;
                Object debris = destructible.get("debris");
                if (debris instanceof String && ((String) debris).length() > 0 && !registry.hasEntity((String) debris)) {
                    ContentReader reader = new ContentReader(entity.id + " (components)");
                    reader.warn("destructible.debris: unknown entity '" + debris + "' (no debris will spawn)");
                    collect(reader);
                    destructible.put("debris", "");
                }
            }
        }
    }

    private boolean withinLimit(Definitions.Mod mod, ContentReader reader) {
        Integer old = (Integer) definitionCounts.get(mod.id);
        int count = old == null ? 1 : old.intValue() + 1;
        definitionCounts.put(mod.id, new Integer(count));
        if (count > MAX_DEFINITIONS_PER_MOD) {
            if (count == MAX_DEFINITIONS_PER_MOD + 1) reader.error("mod exceeds " + MAX_DEFINITIONS_PER_MOD + " definitions; the rest are skipped");
            return false;
        }
        return true;
    }

    private static String context(Definitions.Mod mod, String path, String kind, int index, Object entry) {
        String label = kind + " #" + index;
        if (entry instanceof Json.Map && ((Json.Map) entry).get("id") instanceof String) label = kind + " '" + Ids.make(mod.id, (String) ((Json.Map) entry).get("id")) + "'";
        String file = path.substring(path.lastIndexOf('/') + 1);
        return "mod '" + mod.id + "' · " + file + " · " + label;
    }

    private void collect(ContentReader reader) { reader.collectInto(errors, warnings); }

    public static boolean knownVisual(String style) {
        String[] styles = {"plain", "crate", "plated", "barrel", "concrete", "ball", "sphere", "plank", "limb", "head", "pistol", "revolver", "rifle", "shotgun", "smg", "bat", "hammer", "crowbar", "pipe", "hazard_barrel", "bomb", "large_bomb", "glass"};
        for (int i = 0; i < styles.length; i++) if (styles[i].equals(style)) return true;
        return false;
    }
}
