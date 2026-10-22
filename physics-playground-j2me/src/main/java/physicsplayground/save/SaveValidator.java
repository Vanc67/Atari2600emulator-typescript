package physicsplayground.save;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import physicsplayground.core.ContentReader;
import physicsplayground.core.Definitions;
import physicsplayground.core.Ids;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;

/** Validates the complete reconstruction plan before the live world is touched. */
public final class SaveValidator {
    public static final int MAX_ENTITIES = 20000, MAX_ASSEMBLIES = 5000, MAX_REPORTED = 25;
    public static final float MAX_COORDINATE = 10000000, MAX_SPEED = 1000000;
    public static final class EntityPlan {
        public long uid, assemblyUid; public String typeId = "", partName = "";
        public final Vec2 position = new Vec2(); public float rotation; public Json.Map data;
    }
    public static final class AssemblyPlan {
        public long uid; public String typeId = ""; public Vector brokenJoints = new Vector(); public Json.Map controllerState = new Json.Map();
    }
    public static final class LoadPlan {
        public final Vector errors = new Vector(), warnings = new Vector(), missingTypes = new Vector(), entities = new Vector(), assemblies = new Vector();
        public long nextUid = 1; public Json.Map camera = new Json.Map();
        public boolean isValid() { return errors.isEmpty(); }
        public void error(String message) { if (errors.size() < MAX_REPORTED) errors.addElement(message); else if (errors.size() == MAX_REPORTED) errors.addElement("... further errors omitted"); }
        public void warn(String message) { if (warnings.size() < MAX_REPORTED) warnings.addElement(message); }
    }
    private SaveValidator() {}
    public static LoadPlan validate(Json.Map data, Definitions.Registry registry) {
        LoadPlan plan = new LoadPlan(); Vector rawEntities = Json.array(data.get("entities"));
        if (rawEntities == null) { plan.error("'entities' must be an array"); return plan; }
        Vector rawAssemblies = data.containsKey("assemblies") ? Json.array(data.get("assemblies")) : new Vector();
        if (rawAssemblies == null) { plan.error("'assemblies' must be an array"); return plan; }
        if (rawEntities.size() > MAX_ENTITIES || rawAssemblies.size() > MAX_ASSEMBLIES) {
            plan.error("save exceeds limits (" + rawEntities.size() + " entities, " + rawAssemblies.size() + " assemblies; max " + MAX_ENTITIES + " / " + MAX_ASSEMBLIES + ")"); return plan;
        }
        Hashtable usedUids = new Hashtable(), assemblyTypes = new Hashtable();
        validateAssemblies(rawAssemblies, registry, plan, usedUids, assemblyTypes);
        validateEntities(rawEntities, registry, plan, usedUids, assemblyTypes);
        validateWorld(data, registry, plan, usedUids); return plan;
    }
    private static void validateAssemblies(Vector entries, Definitions.Registry registry, LoadPlan plan, Hashtable used, Hashtable types) {
        for (int index = 0; index < entries.size(); index++) {
            String where = "assemblies[" + index + "]"; Json.Map entry = Json.object(entries.elementAt(index));
            if (entry == null) { plan.error(where + " must be an object"); continue; }
            long uid = readUid(entry, where, plan, used); String type = Json.string(entry.get("type"), null);
            if (!Ids.valid(type)) { plan.error(where + ".type must be a namespaced id such as \"core:human\""); continue; }
            Json.Map joints = entry.containsKey("joints") ? Json.object(entry.get("joints")) : new Json.Map();
            if (joints == null) { plan.error(where + ".joints must be an object"); continue; }
            Vector broken = new Vector(), names = joints.orderedKeys(); boolean valid = true;
            for (int i = 0; i < names.size(); i++) {
                String name = (String) names.elementAt(i); Json.Map state = Json.object(joints.get(name));
                Object connected = state == null ? null : (state.containsKey("connected") ? state.get("connected") : Boolean.TRUE);
                if (state == null || !(connected instanceof Boolean)) { plan.error(where + ".joints." + name + " must be {\"connected\": true|false}"); valid = false; }
                else if (!Json.bool(connected, true)) broken.addElement(name);
            }
            Json.Map controller = entry.containsKey("controller") ? Json.object(entry.get("controller")) : new Json.Map();
            if (controller == null) { plan.error(where + ".controller must be an object"); continue; }
            if (uid <= 0 || !valid) continue;
            Definitions.Entity definition = registry.entity(type);
            if (definition == null) { noteMissing(plan, type); continue; }
            if (!definition.isAssembly()) { plan.error(where + ".type '" + type + "' is not an assembly"); continue; }
            for (int i = 0; i < broken.size(); i++) if (definition.assembly.joint((String) broken.elementAt(i)) == null) plan.warn(where + ": unknown joint '" + broken.elementAt(i) + "' ignored");
            AssemblyPlan assembly = new AssemblyPlan(); assembly.uid = uid; assembly.typeId = type; assembly.brokenJoints = broken; assembly.controllerState = controller;
            plan.assemblies.addElement(assembly); types.put(new Long(uid), type);
        }
    }
    private static void validateEntities(Vector entries, Definitions.Registry registry, LoadPlan plan, Hashtable used, Hashtable assemblyTypes) {
        Hashtable claimed = new Hashtable();
        for (int index = 0; index < entries.size(); index++) {
            String where = "entities[" + index + "]"; Json.Map entry = Json.object(entries.elementAt(index));
            if (entry == null) { plan.error(where + " must be an object"); continue; }
            long uid = readUid(entry, where, plan, used); String type = Json.string(entry.get("type"), null);
            if (!Ids.valid(type)) { plan.error(where + ".type must be a namespaced id such as \"core:wooden_box\""); continue; }
            Vec2 position = readVector(entry, "position", where, plan, true, MAX_COORDINATE);
            float rotation = (float) readNumber(entry, "rotation", where, plan, 0, false);
            readVector(entry, "linear_velocity", where, plan, false, MAX_SPEED); readNumber(entry, "angular_velocity", where, plan, 0, false);
            if (entry.containsKey("frozen") && !(entry.get("frozen") instanceof Boolean)) plan.error(where + ".frozen must be true or false");
            if (entry.containsKey("properties") && Json.object(entry.get("properties")) == null) plan.error(where + ".properties must be an object");
            Definitions.Entity definition = registry.entity(type); validateComponentStates(entry, where, definition, plan);
            long assemblyUid = 0; String partName = "";
            if (entry.containsKey("assembly")) {
                Json.Map membership = Json.object(entry.get("assembly"));
                if (membership == null || !isIntegral(membership.get("uid")) || !(membership.get("part") instanceof String) || ((String) membership.get("part")).length() == 0) {
                    plan.error(where + ".assembly must be {\"uid\": <integer>, \"part\": <name>}");
                } else {
                    assemblyUid = ((Json.Num) membership.get("uid")).longValue(); partName = (String) membership.get("part");
                    String claim = assemblyUid + "/" + partName;
                    if (claimed.containsKey(claim)) plan.error(where + ": assembly " + assemblyUid + " part '" + partName + "' is claimed by two entities");
                    claimed.put(claim, Boolean.TRUE);
                }
            }
            if (uid <= 0 || !plan.isValid()) continue;
            if (definition == null) { noteMissing(plan, type); continue; }
            if (definition.isAssembly()) { plan.error(where + ".type '" + type + "' is an assembly; entities must be single bodies"); continue; }
            if (assemblyUid > 0) {
                String assemblyType = (String) assemblyTypes.get(new Long(assemblyUid));
                if (assemblyType == null) { plan.warn(where + ": assembly " + assemblyUid + " is missing; '" + partName + "' loads as a standalone body"); assemblyUid = 0; }
                else if (registry.entity(assemblyType).assembly.part(partName) == null) { plan.warn(where + ": '" + assemblyType + "' has no part '" + partName + "'; it loads as a standalone body"); assemblyUid = 0; }
            }
            EntityPlan entity = new EntityPlan(); entity.uid = uid; entity.typeId = type; entity.position.set(position); entity.rotation = rotation; entity.data = entry;
            entity.assemblyUid = assemblyUid; entity.partName = assemblyUid > 0 ? partName : ""; plan.entities.addElement(entity);
        }
    }
    private static void validateComponentStates(Json.Map entry, String where, Definitions.Entity definition, LoadPlan plan) {
        if (!entry.containsKey("components")) return;
        Json.Map states = Json.object(entry.get("components"));
        if (states == null) { plan.error(where + ".components must be an object"); return; }
        Vector components = states.orderedKeys();
        for (int i = 0; i < components.size(); i++) {
            String type = (String) components.elementAt(i), at = where + ".components." + type; Json.Map state = Json.object(states.get(type));
            if (state == null) { plan.error(at + " must be an object"); continue; }
            Vector keys = state.orderedKeys();
            for (int k = 0; k < keys.size(); k++) {
                String key = (String) keys.elementAt(k), rule = rule(type, key);
                if (rule == null) { plan.warn(at + "." + key + " is not a known state field (ignored)"); continue; }
                String problem = checkValue(state.get(key), rule); if (problem.length() > 0) plan.error(at + "." + key + " " + problem);
            }
            if (definition != null && !definition.isAssembly() && !definition.components.containsKey(type)) plan.warn(at + ": '" + definition.id + "' no longer has this component (state ignored)");
        }
    }
    private static String rule(String type, String key) {
        if ("health".equals(type) && "current".equals(key)) return "nonneg";
        if ("thermal".equals(type)) {
            if ("temperature".equals(key)) return "temperature"; if ("burning".equals(key)) return "bool";
            if ("fuel".equals(key)) return "number"; if ("char".equals(key)) return "unit";
        }
        if ("magazine".equals(type)) { if ("rounds".equals(key)) return "count"; if ("reload_progress".equals(key)) return "nonneg"; }
        if ("firearm".equals(type) && "trigger_held".equals(key)) return "bool";
        if ("explosive".equals(type)) { if ("armed".equals(key)) return "bool"; if ("fuse".equals(key)) return "nonneg"; }
        if ("electrical".equals(type) && "charge".equals(key)) return "number";
        return null;
    }
    private static void validateWorld(Json.Map data, Definitions.Registry registry, LoadPlan plan, Hashtable used) {
        long highest = 0; Enumeration keys = used.keys();
        while (keys.hasMoreElements()) highest = Math.max(highest, ((Long) keys.nextElement()).longValue());
        plan.nextUid = highest + 1;
        Json.Map world = data.containsKey("world") ? Json.object(data.get("world")) : new Json.Map();
        if (world == null) { plan.error("'world' must be an object"); return; }
        if (world.containsKey("next_entity_uid")) {
            if (!isIntegral(world.get("next_entity_uid")) || ((Json.Num) world.get("next_entity_uid")).longValue() < 1) plan.error("world.next_entity_uid must be a positive integer");
            else plan.nextUid = Math.max(plan.nextUid, ((Json.Num) world.get("next_entity_uid")).longValue());
        }
        Json.Map camera = world.containsKey("camera") ? Json.object(world.get("camera")) : new Json.Map();
        if (camera == null) plan.error("world.camera must be an object");
        else {
            readVector(camera, "position", "world.camera", plan, false, MAX_COORDINATE);
            double zoom = readNumber(camera, "zoom", "world.camera", plan, 1, false); if (zoom <= 0) plan.error("world.camera.zoom must be positive"); plan.camera = camera;
        }
        Json.Map mods = Json.object(data.get("mods"));
        if (mods != null) {
            Json.Map installed = registry.modVersions(); Vector ids = mods.orderedKeys();
            for (int i = 0; i < ids.size(); i++) if (!installed.containsKey(ids.elementAt(i))) plan.warn("the save was made with mod '" + ids.elementAt(i) + "', which is not installed");
        }
    }
    private static long readUid(Json.Map entry, String where, LoadPlan plan, Hashtable used) {
        Object raw = entry.get("uid");
        if (!isIntegral(raw) || ((Json.Num) raw).doubleValue() < 1 || ((Json.Num) raw).doubleValue() > 9.00719925474099e15) { plan.error(where + ".uid must be a positive integer"); return 0; }
        long uid = ((Json.Num) raw).longValue(); Long key = new Long(uid);
        if (used.containsKey(key)) { plan.error(where + ".uid " + uid + " is used more than once"); return 0; }
        used.put(key, Boolean.TRUE); return uid;
    }
    private static Vec2 readVector(Json.Map entry, String key, String where, LoadPlan plan, boolean required, float limit) {
        if (!entry.containsKey(key)) { if (required) plan.error(where + "." + key + " is missing"); return new Vec2(); }
        Vector raw = Json.array(entry.get(key));
        if (raw == null || raw.size() != 2 || !ContentReader.number(raw.elementAt(0)) || !ContentReader.number(raw.elementAt(1))) { plan.error(where + "." + key + " must be [x, y] with two finite numbers"); return new Vec2(); }
        Vec2 vector = new Vec2(((Json.Num) raw.elementAt(0)).floatValue(), ((Json.Num) raw.elementAt(1)).floatValue());
        if (Math.abs(vector.x) > limit || Math.abs(vector.y) > limit) plan.error(where + "." + key + " is out of range (limit " + limit + ")"); return vector;
    }
    private static double readNumber(Json.Map entry, String key, String where, LoadPlan plan, double fallback, boolean required) {
        if (!entry.containsKey(key)) { if (required) plan.error(where + "." + key + " is missing"); return fallback; }
        if (!ContentReader.number(entry.get(key))) { plan.error(where + "." + key + " must be a finite number"); return fallback; }
        return ((Json.Num) entry.get(key)).doubleValue();
    }
    private static String checkValue(Object value, String rule) {
        if ("bool".equals(rule)) return value instanceof Boolean ? "" : "must be true or false";
        if ("number".equals(rule)) return ContentReader.number(value) ? "" : "must be a finite number";
        if ("nonneg".equals(rule)) return ContentReader.number(value) && ((Json.Num) value).doubleValue() >= 0 ? "" : "must be a finite number >= 0";
        if ("unit".equals(rule)) { double n = ContentReader.number(value) ? ((Json.Num) value).doubleValue() : -1; return n >= 0 && n <= 1 ? "" : "must be a number in [0, 1]"; }
        if ("count".equals(rule)) return isIntegral(value) && ((Json.Num) value).doubleValue() >= 0 ? "" : "must be a whole number >= 0";
        if ("temperature".equals(rule)) { double n = ContentReader.number(value) ? ((Json.Num) value).doubleValue() : -1000; return n >= -273.15 && n <= 100000 ? "" : "must be a temperature in [-273.15, 100000] C"; }
        return "";
    }
    private static boolean isIntegral(Object value) { return ContentReader.integral(value); }
    private static void noteMissing(LoadPlan plan, String type) { if (!plan.missingTypes.contains(type)) plan.missingTypes.addElement(type); }
}
