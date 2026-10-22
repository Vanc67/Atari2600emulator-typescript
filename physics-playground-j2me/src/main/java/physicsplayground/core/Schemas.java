package physicsplayground.core;

import java.util.Hashtable;
import java.util.Vector;

/** Typed component schemas from component_schemas.gd. */
public final class Schemas {
    private static final String[] DAMAGE_KINDS = {"generic", "impact", "projectile", "explosion", "fire"};
    private static final String[] TYPE_ORDER = {"health", "thermal", "electrical", "impact_damage", "magazine", "firearm", "explosive", "destructible", "debris", "melee"};
    private static final Hashtable SCHEMAS = new Hashtable();

    private static final class Spec {
        String name;
        String type;
        double min;
        double max;
        boolean required;
        String[] values;
        Spec(String name, String type, double min, double max) { this(name, type, min, max, false, null); }
        Spec(String name, String type, double min, double max, boolean required, String[] values) {
            this.name = name; this.type = type; this.min = min; this.max = max; this.required = required; this.values = values;
        }
    }

    private static final class Schema {
        String[] requires;
        String[] conflicts;
        Vector params = new Vector();
        Schema(String[] requires, String[] conflicts) { this.requires = requires; this.conflicts = conflicts; }
        Schema add(Spec spec) { params.addElement(spec); return this; }
        Spec spec(String name) {
            for (int i = 0; i < params.size(); i++) { Spec spec = (Spec) params.elementAt(i); if (spec.name.equals(name)) return spec; }
            return null;
        }
    }

    static {
        Schema health = schema(null, null);
        health.add(new Spec("max_health", "float", 1, 1000000, false, null));
        health.add(new Spec("on_depleted", "enum", 0, 0, false, new String[]{"none", "remove"}));
        health.add(new Spec("damage_resistance", "float", 0, 1, false, null));
        health.add(new Spec("damage_multipliers", "kind_floats", 0, 100, false, null));
        SCHEMAS.put("health", health);

        Schema thermal = schema(null, null);
        thermal.add(new Spec("initial_temperature", "float", -273.15, 5000, false, null));
        thermal.add(new Spec("ignition_temperature", "float", -1, 100000, false, null));
        thermal.add(new Spec("flammability", "float", 0, 1, false, null));
        thermal.add(new Spec("heat_resistance", "float", 0, 1, false, null));
        thermal.add(new Spec("burn_damage", "float", 0, 10000, false, null));
        thermal.add(new Spec("burn_duration", "float", -1, 3600, false, null));
        thermal.add(new Spec("burn_temperature", "float", -273.15, 100000, false, null));
        thermal.add(new Spec("heat_radius", "float", 0, 2000, false, null));
        thermal.add(new Spec("heat_output", "float", 0, 100000, false, null));
        thermal.add(new Spec("cooling", "float", 0, 10, false, null));
        SCHEMAS.put("thermal", thermal);

        SCHEMAS.put("electrical", schema(null, null).add(new Spec("conductivity", "float", 0, 1, false, null)));

        Schema impact = schema(new String[]{"health"}, null);
        impact.add(new Spec("min_impact_speed", "float", 0, 1000, false, null));
        impact.add(new Spec("damage_per_speed", "float", 0, 10000, false, null));
        impact.add(new Spec("min_impulse", "float", 0, 1000000, false, null));
        impact.add(new Spec("max_damage_per_hit", "float", 0, 1000000, false, null));
        impact.add(new Spec("cooldown", "float", 0, 60, false, null));
        SCHEMAS.put("impact_damage", impact);

        Schema magazine = schema(null, null);
        magazine.add(new Spec("capacity", "int", 1, 1000, false, null));
        magazine.add(new Spec("reload_time", "float", 0, 60, false, null));
        magazine.add(new Spec("per_round", "bool", 0, 0, false, null));
        magazine.add(new Spec("infinite", "bool", 0, 0, false, null));
        magazine.add(new Spec("reload_sound", "sound", 0, 0, false, null));
        SCHEMAS.put("magazine", magazine);

        Schema firearm = schema(null, new String[]{"debris"});
        firearm.add(new Spec("projectile", "projectile", 0, 0, true, null));
        firearm.add(new Spec("fire_rate", "float", 0.1, 60, false, null));
        firearm.add(new Spec("automatic", "bool", 0, 0, false, null));
        firearm.add(new Spec("speed", "float", 1, 3000, false, null));
        firearm.add(new Spec("damage", "float", 0, 100000, false, null));
        firearm.add(new Spec("pellets", "int", 1, 32, false, null));
        firearm.add(new Spec("spread", "float", 0, 90, false, null));
        firearm.add(new Spec("recoil", "float", 0, 1000, false, null));
        firearm.add(new Spec("muzzle", "vector2", -4000, 4000, false, null));
        firearm.add(new Spec("muzzle_flash", "float", 0, 500, false, null));
        firearm.add(new Spec("sound", "sound", 0, 0, false, null));
        firearm.add(new Spec("dry_sound", "sound", 0, 0, false, null));
        SCHEMAS.put("firearm", firearm);

        Schema explosive = schema(null, new String[]{"debris"});
        explosive.add(new Spec("radius", "float", 10, 3000, false, null));
        explosive.add(new Spec("force", "float", 0, 100000, false, null));
        explosive.add(new Spec("damage", "float", 0, 100000, false, null));
        explosive.add(new Spec("falloff", "float", 0.25, 4, false, null));
        explosive.add(new Spec("ignite", "bool", 0, 0, false, null));
        explosive.add(new Spec("heat", "float", 0, 5000, false, null));
        explosive.add(new Spec("fuse", "float", 0, 600, false, null));
        explosive.add(new Spec("chain_fuse", "range2", 0, 60, false, null));
        explosive.add(new Spec("detonate_on_depleted", "bool", 0, 0, false, null));
        explosive.add(new Spec("detonate_temperature", "float", -1, 100000, false, null));
        explosive.add(new Spec("sound", "sound", 0, 0, false, null));
        explosive.add(new Spec("fuse_sound", "sound", 0, 0, false, null));
        SCHEMAS.put("explosive", explosive);

        Schema destructible = schema(new String[]{"health"}, null);
        destructible.add(new Spec("threshold", "float", 0, 0.99, false, null));
        destructible.add(new Spec("debris", "entity", 0, 0, false, null));
        destructible.add(new Spec("debris_count", "int", 0, 16, false, null));
        destructible.add(new Spec("debris_speed", "float", 0, 5000, false, null));
        destructible.add(new Spec("effect", "enum", 0, 0, false, new String[]{"sparks", "dust", "splinters", "blood", "glass", "metal", "smoke", "debris"}));
        destructible.add(new Spec("sound", "sound", 0, 0, false, null));
        SCHEMAS.put("destructible", destructible);

        SCHEMAS.put("debris", schema(null, new String[]{"firearm", "magazine", "explosive", "destructible"}).add(new Spec("lifetime", "float", 0.1, 120, false, null)));
        Schema melee = schema(null, null);
        melee.add(new Spec("damage_multiplier", "float", 0, 20, false, null));
        melee.add(new Spec("threshold_multiplier", "float", 0.1, 1, false, null));
        SCHEMAS.put("melee", melee);
    }

    private Schemas() {}
    private static Schema schema(String[] requires, String[] conflicts) { return new Schema(requires, conflicts); }
    public static boolean hasSchema(String type) { return SCHEMAS.containsKey(type); }
    public static String[] types() { return TYPE_ORDER; }

    public static void validate(Json.Map components, String mod, Definitions.Registry registry, ContentReader reader) {
        for (int i = 0; i < components.orderedKeys().size(); i++) {
            String type = (String) components.orderedKeys().elementAt(i);
            String at = "component '" + type + "'";
            Schema schema = (Schema) SCHEMAS.get(type);
            if (schema == null) {
                reader.error(at + ": unsupported component type (known: " + knownTypes() + ")");
                continue;
            }
            Json.Map params = (Json.Map) components.get(type);
            if (schema.requires != null) for (int j = 0; j < schema.requires.length; j++) if (!components.containsKey(schema.requires[j])) reader.error(at + " requires the '" + schema.requires[j] + "' component");
            if (schema.conflicts != null) for (int j = 0; j < schema.conflicts.length; j++) if (components.containsKey(schema.conflicts[j])) reader.error(at + " cannot be combined with '" + schema.conflicts[j] + "'");
            for (int j = 0; j < schema.params.size(); j++) {
                Spec spec = (Spec) schema.params.elementAt(j);
                if (spec.required && !params.containsKey(spec.name)) reader.error(at + ": missing required parameter '" + spec.name + "' (" + describe(spec) + ")");
            }
            for (int j = 0; j < params.orderedKeys().size(); j++) {
                String name = (String) params.orderedKeys().elementAt(j);
                Spec spec = schema.spec(name);
                if (spec == null) { reader.warn(at + ": unknown parameter '" + name + "' (ignored)"); continue; }
                String problem = check(params, name, spec, mod, registry, reader, at);
                if (problem.length() > 0) reader.error(at + " · '" + name + "': expected " + describe(spec) + ", got " + problem);
            }
        }
        Object explosive = components.get("explosive");
        if (explosive instanceof Json.Map && Json.bool(((Json.Map) explosive).get("detonate_on_depleted"), true) && !components.containsKey("health")) {
            reader.warn("component 'explosive': detonate_on_depleted has no effect without a 'health' component");
        }
    }

    private static String check(Json.Map params, String name, Spec spec, String mod, Definitions.Registry registry, ContentReader reader, String at) {
        Object value = params.get(name);
        String type = spec.type;
        if ("float".equals(type)) {
            if (!ContentReader.number(value) || ((Json.Num) value).doubleValue() < spec.min || ((Json.Num) value).doubleValue() > spec.max) return render(value);
        } else if ("int".equals(type)) {
            if (!ContentReader.integral(value) || ((Json.Num) value).doubleValue() < spec.min || ((Json.Num) value).doubleValue() > spec.max) return render(value);
        } else if ("bool".equals(type)) {
            if (!(value instanceof Boolean)) return render(value);
        } else if ("string".equals(type)) {
            if (!(value instanceof String)) return render(value);
        } else if ("enum".equals(type)) {
            if (!(value instanceof String) || !contains(spec.values, (String) value)) return render(value);
        } else if ("vector2".equals(type) || "range2".equals(type)) {
            Vector array = Json.array(value);
            if (array == null || array.size() != 2) return render(value);
            for (int i = 0; i < 2; i++) {
                Object element = array.elementAt(i);
                if (!ContentReader.number(element) || ((Json.Num) element).doubleValue() < spec.min || ((Json.Num) element).doubleValue() > spec.max) return render(value);
            }
            if ("range2".equals(type) && ((Json.Num) array.elementAt(0)).doubleValue() > ((Json.Num) array.elementAt(1)).doubleValue()) return render(value);
        } else if ("projectile".equals(type)) {
            if (!(value instanceof String) || ((String) value).length() == 0) return render(value);
            String id = Ids.qualify((String) value, mod);
            params.put(name, id);
            if (!registry.hasProjectile(id)) return "unknown projectile '" + id + "'";
        } else if ("sound".equals(type)) {
            if (!(value instanceof String)) return render(value);
            if (((String) value).length() > 0) {
                String id = Ids.qualify((String) value, mod);
                params.put(name, id);
                if (!registry.hasSound(id)) reader.warn(at + ": unknown sound '" + id + "' (silent)");
            }
        } else if ("entity".equals(type)) {
            if (!(value instanceof String)) return render(value);
            if (((String) value).length() > 0) params.put(name, Ids.qualify((String) value, mod));
        } else if ("kind_floats".equals(type)) {
            if (!(value instanceof Json.Map)) return render(value);
            Json.Map map = (Json.Map) value;
            for (int i = 0; i < map.orderedKeys().size(); i++) {
                Object key = map.orderedKeys().elementAt(i);
                if (!contains(DAMAGE_KINDS, key.toString())) return "unknown damage kind '" + key + "'";
                Object factor = map.get(key);
                if (!ContentReader.number(factor) || ((Json.Num) factor).doubleValue() < spec.min || ((Json.Num) factor).doubleValue() > spec.max) return key + ": " + render(factor);
            }
        }
        return "";
    }

    private static String describe(Spec spec) {
        String low = format(spec.min), high = format(spec.max);
        if ("float".equals(spec.type)) return "a number in [" + low + ", " + high + "]";
        if ("int".equals(spec.type)) return "a whole number in [" + low + ", " + high + "]";
        if ("bool".equals(spec.type)) return "true or false";
        if ("string".equals(spec.type)) return "a string";
        if ("enum".equals(spec.type)) return "one of " + join(spec.values, ", ");
        if ("vector2".equals(spec.type)) return "[x, y] with numbers in [" + low + ", " + high + "]";
        if ("range2".equals(spec.type)) return "[min, max] with min ≤ max in [" + low + ", " + high + "]";
        if ("projectile".equals(spec.type)) return "a projectile id";
        if ("sound".equals(spec.type)) return "a sound id";
        if ("entity".equals(spec.type)) return "an entity id";
        if ("kind_floats".equals(spec.type)) return "an object mapping " + join(DAMAGE_KINDS, "/") + " to numbers in [" + low + ", " + high + "]";
        return "a valid value";
    }

    private static String knownTypes() { return join(TYPE_ORDER, ", "); }
    private static boolean contains(String[] values, String value) { for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return true; return false; }
    private static String join(String[] values, String separator) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < values.length; i++) { if (i > 0) out.append(separator); out.append(values[i]); }
        return out.toString();
    }
    private static String format(double value) { return Math.floor(value) == value && Math.abs(value) < 1e15 ? Long.toString((long) value) : Double.toString(value); }
    private static String render(Object value) { String text = Json.write(value, false); return text.length() <= 40 ? text : text.substring(0, 37) + "..."; }
}
