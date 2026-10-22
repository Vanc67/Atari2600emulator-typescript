package physicsplayground.core;

import java.util.Hashtable;
import java.util.Vector;

/** Data model used by both the Godot original and this runtime. No MIDP types are referenced. */
public final class Definitions {
    private Definitions() {}

    public static final class Mod {
        public String id;
        public String displayName;
        public String version;
        public String description;
        public String directory;
        public int format = 1;
        public boolean builtin;
        public Vector authors = new Vector();
        public Vector categories = new Vector();

        static Mod from(Json.Map data, String directory, ContentReader reader) {
            Mod mod = new Mod();
            mod.directory = directory;
            mod.builtin = directory.startsWith("res://");
            mod.format = reader.integer(data, "mod_format", 1);
            if (mod.format > 1) reader.error("requires mod format " + mod.format + " but this game supports up to 1");
            mod.id = reader.string(data, "id", "", true);
            if (!mod.id.equals("") && !Ids.validSegment(mod.id)) reader.error("mod id '" + mod.id + "' may only contain lowercase letters, digits and underscores");
            mod.displayName = reader.string(data, "name", mod.id);
            mod.version = reader.string(data, "version", "0.0.0");
            mod.description = reader.string(data, "description", "");
            mod.authors = reader.strings(data, "authors");
            mod.categories = reader.strings(data, "categories");
            return mod;
        }
    }

    public static final class Material {
        public String id;
        public String sourceMod;
        public String displayName;
        public float density;
        public float friction;
        public float bounce;
        public float durability;
        public float thermalConductivity;
        public float electricalConductivity;
        public float penetrationResistance;
        public float ricochetChance;
        public String impactEffect;
        public String impactSound;
        public float flammability;
        public float ignitionTemperature;
        public Vector tags = new Vector();

        static Material from(Json.Map data, String mod, ContentReader reader) {
            Material value = new Material();
            String local = reader.string(data, "id", "", true);
            if (!local.equals("") && !Ids.validSegment(local)) reader.error("id '" + local + "' may only contain lowercase letters, digits and underscores");
            value.id = Ids.make(mod, local);
            value.sourceMod = mod;
            value.displayName = reader.string(data, "display_name", capitalize(local));
            value.density = (float) reader.floating(data, "density", 100, 0.1, 100000);
            value.friction = (float) reader.floating(data, "friction", 0.5, 0, 1);
            value.bounce = (float) reader.floating(data, "bounce", 0, 0, 1);
            value.durability = (float) reader.floating(data, "durability", 1, 0, 1000);
            value.thermalConductivity = (float) reader.floating(data, "thermal_conductivity", 0, 0, 1);
            value.electricalConductivity = (float) reader.floating(data, "electrical_conductivity", 0, 0, 1);
            value.tags = reader.strings(data, "tags");
            value.penetrationResistance = (float) reader.floating(data, "penetration_resistance", 1, 0, 1000);
            value.ricochetChance = (float) reader.floating(data, "ricochet_chance", 0, 0, 1);
            value.impactEffect = reader.string(data, "impact_effect", "dust");
            value.impactSound = Ids.qualify(reader.string(data, "impact_sound", "core:impact_generic"), mod);
            value.flammability = (float) reader.floating(data, "flammability", 0, 0, 1);
            value.ignitionTemperature = (float) reader.floating(data, "ignition_temperature", -1, -1, 100000);
            return value;
        }
    }

    public static final class Projectile {
        public String id;
        public String sourceMod;
        public String displayName;
        public float massGrams;
        public float speed;
        public float damage;
        public float penetration;
        public float lifetime;
        public float gravityScale;
        public float drag;
        public float impulseMultiplier;
        public ColorValue trailColor = new ColorValue(255, 216, 122, 230);
        public float trailWidth;
        public float trailTime;

        static Projectile from(Json.Map data, String mod, ContentReader reader) {
            Projectile value = new Projectile();
            String local = reader.string(data, "id", "", true);
            if (!local.equals("") && !Ids.validSegment(local)) reader.error("id '" + local + "' may only contain lowercase letters, digits and underscores");
            value.id = Ids.make(mod, local);
            value.sourceMod = mod;
            value.displayName = reader.string(data, "display_name", capitalize(local));
            value.massGrams = (float) reader.floating(data, "mass_grams", 8, 0.1, 10000);
            value.speed = (float) reader.floating(data, "speed", 350, 1, 3000);
            value.damage = (float) reader.floating(data, "damage", 30, 0, 100000);
            value.penetration = (float) reader.floating(data, "penetration", 1, 0, 1000);
            value.lifetime = (float) reader.floating(data, "lifetime", 1.5, 0.01, 30);
            value.gravityScale = (float) reader.floating(data, "gravity_scale", 0.15, -10, 10);
            value.drag = (float) reader.floating(data, "drag", 0, 0, 50);
            value.impulseMultiplier = (float) reader.floating(data, "impulse_multiplier", 2, 0, 100);
            value.trailColor = reader.color(data, "trail_color", value.trailColor);
            value.trailWidth = (float) reader.floating(data, "trail_width", 2, 0.1, 20);
            value.trailTime = (float) reader.floating(data, "trail_time", 0.012, 0, 0.5);
            return value;
        }
    }

    public static final class Sound {
        public String id;
        public String sourceMod;
        public String synth;
        public float duration;
        public float pitch;
        public float pitchVariation;
        public float volumeDb;
        public int maxInstances;
        public float minInterval;
        public Json.Map params = new Json.Map();

        static Sound from(Json.Map data, String mod, ContentReader reader) {
            Sound value = new Sound();
            String local = reader.string(data, "id", "", true);
            if (!local.equals("") && !Ids.validSegment(local)) reader.error("id '" + local + "' may only contain lowercase letters, digits and underscores");
            value.id = Ids.make(mod, local);
            value.sourceMod = mod;
            value.synth = reader.string(data, "synth", "click");
            if (!knownSynth(value.synth)) reader.error("unknown synth preset '" + value.synth + "'");
            value.duration = (float) reader.floating(data, "duration", 0.3, 0.02, 4);
            value.pitch = (float) reader.floating(data, "pitch", 1, 0.1, 8);
            value.pitchVariation = (float) reader.floating(data, "pitch_variation", 0.06, 0, 1);
            value.volumeDb = (float) reader.floating(data, "volume_db", 0, -60, 12);
            value.maxInstances = reader.integer(data, "max_instances", 4);
            value.minInterval = (float) reader.floating(data, "min_interval", 0.03, 0, 5);
            value.params = (Json.Map) Json.deepCopy(reader.map(data, "params"));
            return value;
        }

        static boolean knownSynth(String id) {
            String[] names = {"gunshot", "explosion", "impact_metal", "impact_wood", "impact_flesh", "impact_generic", "glass_break", "reload", "click", "ricochet", "fire_crackle", "beep", "swoosh"};
            for (int i = 0; i < names.length; i++) if (names[i].equals(id)) return true;
            return false;
        }
    }

    public static final class Part {
        public String name;
        public String displayName;
        public String entityId;
        public Vec2 position = new Vec2();
        public float rotation;
        public int zIndex;
        public Entity definition;
        public float maxHealth;
    }

    public static final class Joint {
        public String name;
        public String parent;
        public String child;
        public Vec2 anchor = new Vec2();
        public boolean hasAngleLimits;
        public float lowerAngle;
        public float upperAngle;
        public float breakStrain = 12;
        public float breakImpactSpeed;
        public float restAngle;
        public Vec2 localAnchorParent = new Vec2();
        public Vec2 localAnchorChild = new Vec2();
    }

    public static final class Assembly {
        public static final int MAX_PARTS = 32;
        public static final int MAX_JOINTS = 64;
        public Vector parts = new Vector();
        public Vector joints = new Vector();
        public String controllerType = "";
        public Json.Map controllerParams = new Json.Map();
        public float spawnNudge;
        public float totalMass;
        public float boundsX;
        public float boundsY;
        public float boundsWidth;
        public float boundsHeight;
        public float boundingRadius;
        private final Hashtable byName = new Hashtable();

        static Assembly from(Json.Map data, String mod, ContentReader reader) {
            Assembly assembly = new Assembly();
            Object rawParts = data.get("parts");
            Vector partEntries = Json.array(rawParts);
            if (partEntries == null || partEntries.isEmpty()) {
                reader.error("assembly needs a non-empty 'parts' array");
                partEntries = new Vector();
            } else if (partEntries.size() > MAX_PARTS) {
                reader.error("assembly has " + partEntries.size() + " parts; the limit is " + MAX_PARTS);
                partEntries = new Vector();
            }
            for (int i = 0; i < partEntries.size(); i++) {
                Object raw = partEntries.elementAt(i);
                if (!(raw instanceof Json.Map)) { reader.error("assembly parts must be objects"); continue; }
                Json.Map entry = (Json.Map) raw;
                Part part = new Part();
                part.name = reader.string(entry, "name", "", true);
                if (assembly.byName.containsKey(part.name)) { reader.error("duplicate part name '" + part.name + "'"); continue; }
                part.displayName = reader.string(entry, "display_name", capitalize(part.name));
                part.entityId = Ids.qualify(reader.string(entry, "entity", "", true), mod);
                part.position = reader.vector(entry, "position", new Vec2());
                part.rotation = (float) Math.toRadians(reader.floating(entry, "rotation", 0, -360, 360));
                part.zIndex = reader.integer(entry, "z_index", 0);
                assembly.parts.addElement(part);
                assembly.byName.put(part.name, part);
            }

            Object rawJoints = data.get("joints");
            Vector jointEntries = rawJoints == null ? new Vector() : Json.array(rawJoints);
            if (jointEntries == null) { reader.error("'joints' must be an array"); jointEntries = new Vector(); }
            else if (jointEntries.size() > MAX_JOINTS) { reader.error("assembly has " + jointEntries.size() + " joints; the limit is " + MAX_JOINTS); jointEntries = new Vector(); }
            Hashtable jointNames = new Hashtable();
            for (int i = 0; i < jointEntries.size(); i++) {
                Object raw = jointEntries.elementAt(i);
                if (!(raw instanceof Json.Map)) { reader.error("assembly joints must be objects"); continue; }
                Json.Map entry = (Json.Map) raw;
                Joint joint = new Joint();
                joint.name = reader.string(entry, "name", "", true);
                if (jointNames.containsKey(joint.name)) { reader.error("duplicate joint name '" + joint.name + "'"); continue; }
                jointNames.put(joint.name, Boolean.TRUE);
                joint.parent = reader.string(entry, "parent", "", true);
                joint.child = reader.string(entry, "child", "", true);
                if (!assembly.byName.containsKey(joint.parent)) reader.error("joint '" + joint.name + "' references unknown part '" + joint.parent + "'");
                if (!assembly.byName.containsKey(joint.child)) reader.error("joint '" + joint.name + "' references unknown part '" + joint.child + "'");
                if (joint.parent.equals(joint.child)) reader.error("joint '" + joint.name + "' connects a part to itself");
                joint.anchor = reader.vector(entry, "anchor", new Vec2());
                if (entry.containsKey("angle_limits") && entry.get("angle_limits") != Json.NULL) {
                    Vec2 limits = reader.vector(entry, "angle_limits", new Vec2());
                    joint.hasAngleLimits = true;
                    joint.lowerAngle = (float) Math.toRadians(Math.min(limits.x, limits.y));
                    joint.upperAngle = (float) Math.toRadians(Math.max(limits.x, limits.y));
                }
                joint.breakStrain = (float) reader.floating(entry, "break_strain", 12, 0.5, 10000);
                joint.breakImpactSpeed = (float) reader.floating(entry, "break_impact_speed", 0, 0, 10000);
                assembly.joints.addElement(joint);
            }
            assembly.spawnNudge = (float) reader.floating(data, "spawn_nudge", 0, 0, 1000);
            Json.Map controller = reader.map(data, "controller");
            assembly.controllerType = reader.string(controller, "type", "");
            assembly.controllerParams = (Json.Map) Json.deepCopy(controller);
            return assembly;
        }

        public Part part(String name) { return (Part) byName.get(name); }
        public Joint joint(String name) {
            for (int i = 0; i < joints.size(); i++) { Joint joint = (Joint) joints.elementAt(i); if (joint.name.equals(name)) return joint; }
            return null;
        }

        void link(Registry registry, ContentReader reader) {
            totalMass = 0;
            boolean hasBounds = false;
            float minX = 0, minY = 0, maxX = 0, maxY = 0;
            for (int i = 0; i < parts.size(); i++) {
                Part part = (Part) parts.elementAt(i);
                part.definition = registry.entity(part.entityId);
                if (part.definition == null) { reader.error("part '" + part.name + "' references unknown entity '" + part.entityId + "'"); continue; }
                if (part.definition.isAssembly()) { reader.error("part '" + part.name + "' cannot itself be an assembly"); continue; }
                Material material = registry.material(part.definition.materialId);
                totalMass += part.definition.computeMass(material);
                Object health = part.definition.components.get("health");
                if (health instanceof Json.Map) part.maxHealth = (float) Json.number(((Json.Map) health).get("max_health"), 100) * (material == null ? 1 : material.durability);
                float width = part.definition.boundingWidth();
                float height = part.definition.boundingHeight();
                float cosine = (float) Math.cos(part.rotation), sine = (float) Math.sin(part.rotation);
                float halfW = (Math.abs(width * cosine) + Math.abs(height * sine)) * 0.5f;
                float halfH = (Math.abs(width * sine) + Math.abs(height * cosine)) * 0.5f;
                float left = part.position.x - halfW, right = part.position.x + halfW;
                float top = part.position.y - halfH, bottom = part.position.y + halfH;
                if (!hasBounds) { minX = left; maxX = right; minY = top; maxY = bottom; hasBounds = true; }
                else { minX = Math.min(minX, left); maxX = Math.max(maxX, right); minY = Math.min(minY, top); maxY = Math.max(maxY, bottom); }
            }
            if (hasBounds) { boundsX = minX; boundsY = minY; boundsWidth = maxX - minX; boundsHeight = maxY - minY; }
            boundingRadius = (float) Math.sqrt(boundsWidth * boundsWidth + boundsHeight * boundsHeight) * 0.5f;
            for (int i = 0; i < joints.size(); i++) {
                Joint joint = (Joint) joints.elementAt(i);
                Part parent = part(joint.parent), child = part(joint.child);
                if (parent == null || child == null) continue;
                joint.restAngle = child.rotation - parent.rotation;
                joint.localAnchorParent.set(joint.anchor).sub(parent.position).rotate(-parent.rotation);
                joint.localAnchorChild.set(joint.anchor).sub(child.position).rotate(-child.rotation);
            }
        }

        public float totalMaxHealth() {
            float total = 0;
            for (int i = 0; i < parts.size(); i++) total += ((Part) parts.elementAt(i)).maxHealth;
            return total;
        }
    }

    public static final class Entity {
        public static final String RECTANGLE = "rectangle";
        public static final String CIRCLE = "circle";
        public static final String CAPSULE = "capsule";
        public String id;
        public String sourceMod;
        public String displayName;
        public String description;
        public String category;
        public Vector tags = new Vector();
        public boolean hidden;
        public String materialId;
        public String shapeType = RECTANGLE;
        public Vec2 size = new Vec2(64, 64);
        public float radius = 24;
        public float massOverride;
        public float linearDamp = -1;
        public float angularDamp = -1;
        public Vec2 centerOfMass;
        public String visualStyle = "plain";
        public ColorValue fillColor = ColorValue.GRAY;
        public ColorValue outlineColor;
        public ColorValue detailColor;
        public ColorValue damageColor;
        public Json.Map components = new Json.Map();
        public Json.Map properties = new Json.Map();
        public Assembly assembly;

        static Entity from(Json.Map data, String mod, ContentReader reader) {
            Entity value = new Entity();
            String local = reader.string(data, "id", "", true);
            if (!local.equals("") && !Ids.validSegment(local)) reader.error("id '" + local + "' may only contain lowercase letters, digits and underscores");
            value.id = Ids.make(mod, local);
            value.sourceMod = mod;
            value.displayName = reader.string(data, "display_name", capitalize(local));
            value.description = reader.string(data, "description", "");
            value.category = reader.string(data, "category", "Misc");
            value.tags = reader.strings(data, "tags");
            value.hidden = reader.bool(data, "hidden", false);
            value.properties = (Json.Map) Json.deepCopy(reader.map(data, "properties"));
            if (data.containsKey("assembly")) {
                value.assembly = Assembly.from(reader.map(data, "assembly"), mod, reader);
                return value;
            }
            value.materialId = Ids.qualify(reader.string(data, "material", "", true), mod);
            Json.Map shape = reader.map(data, "shape");
            value.shapeType = reader.string(shape, "type", RECTANGLE);
            if (RECTANGLE.equals(value.shapeType)) {
                value.size = reader.vector(shape, "size", new Vec2(64, 64));
                if (value.size.x < 4 || value.size.y < 4 || value.size.x > 4000 || value.size.y > 4000) {
                    reader.error("rectangle size must be between 4 and 4000 pixels per side");
                    value.size.x = clamp(value.size.x, 4, 4000); value.size.y = clamp(value.size.y, 4, 4000);
                }
            } else if (CIRCLE.equals(value.shapeType)) {
                value.radius = (float) reader.floating(shape, "radius", 24, 2, 2000);
                value.size.set(value.radius * 2, value.radius * 2);
            } else if (CAPSULE.equals(value.shapeType)) {
                value.radius = (float) reader.floating(shape, "radius", 8, 2, 1000);
                float height = (float) reader.floating(shape, "height", 40, 4, 4000);
                if (height < value.radius * 2) { reader.error("capsule height must be at least twice its radius"); height = value.radius * 2; }
                value.size.set(value.radius * 2, height);
            } else reader.error("unknown shape type '" + value.shapeType + "' (expected 'rectangle', 'circle' or 'capsule')");
            value.massOverride = (float) reader.floating(data, "mass", 0, 0, 100000);
            Json.Map physics = reader.map(data, "physics");
            value.linearDamp = (float) reader.floating(physics, "linear_damp", -1, -1, 100);
            value.angularDamp = (float) reader.floating(physics, "angular_damp", -1, -1, 100);
            if (physics.containsKey("center_of_mass")) value.centerOfMass = reader.vector(physics, "center_of_mass", null);
            Json.Map visual = reader.map(data, "visual");
            value.visualStyle = reader.string(visual, "style", "plain");
            value.fillColor = reader.color(visual, "fill_color", new ColorValue(140, 140, 140));
            value.outlineColor = reader.color(visual, "outline_color", value.fillColor.darkened(0.45f));
            value.detailColor = reader.color(visual, "detail_color", value.fillColor.darkened(0.2f));
            value.damageColor = reader.color(visual, "damage_color", value.fillColor.darkened(0.65f));
            Json.Map components = reader.map(data, "components");
            value.components = (Json.Map) Json.deepCopy(components);
            for (int i = 0; i < components.orderedKeys().size(); i++) {
                Object key = components.orderedKeys().elementAt(i);
                if (!(components.get(key) instanceof Json.Map)) reader.error("component '" + key + "' must map to an object of parameters");
            }
            return value;
        }

        public boolean isAssembly() { return assembly != null; }
        public boolean isCircle() { return CIRCLE.equals(shapeType); }
        public boolean isCapsule() { return CAPSULE.equals(shapeType); }
        public float boundingWidth() { return isAssembly() ? assembly.boundsWidth : (isCircle() ? radius * 2 : size.x); }
        public float boundingHeight() { return isAssembly() ? assembly.boundsHeight : (isCircle() ? radius * 2 : size.y); }
        public float boundingRadius() {
            if (isAssembly()) return assembly.boundingRadius;
            if (isCircle()) return radius;
            if (isCapsule()) return size.y * 0.5f;
            return size.length() * 0.5f;
        }
        public float minThickness() { return isCircle() ? radius * 2 : Math.min(size.x, size.y); }
        public float areaSquareMeters() {
            if (isCircle()) return (float) (Math.PI * radius * radius / 10000.0);
            if (isCapsule()) {
                float straight = Math.max(size.y - radius * 2, 0);
                return (float) ((Math.PI * radius * radius + straight * radius * 2) / 10000.0);
            }
            return size.x * size.y / 10000.0f;
        }
        public float computeMass(Material material) {
            if (isAssembly()) return assembly.totalMass;
            if (massOverride > 0) return massOverride;
            if (material == null) return 1;
            return Math.max(areaSquareMeters() * material.density, 0.05f);
        }
        public float computeInertia(float mass) {
            if (isCircle()) return mass * radius * radius * 0.5f;
            return mass * (size.x * size.x + size.y * size.y) / 12.0f;
        }
    }

    public static final class Registry {
        private final Vector mods = new Vector();
        private final Hashtable materials = new Hashtable();
        private final Hashtable entities = new Hashtable();
        private final Hashtable projectiles = new Hashtable();
        private final Hashtable sounds = new Hashtable();
        private final Vector entityOrder = new Vector();
        private final Vector soundOrder = new Vector();
        private final Vector categoryOrder = new Vector();

        public void registerMod(Mod mod) {
            mods.addElement(mod);
            for (int i = 0; i < mod.categories.size(); i++) if (!categoryOrder.contains(mod.categories.elementAt(i))) categoryOrder.addElement(mod.categories.elementAt(i));
        }
        public Vector mods() { return copy(mods); }
        public boolean registerMaterial(Material value) { if (materials.containsKey(value.id)) return false; materials.put(value.id, value); return true; }
        public boolean registerEntity(Entity value) {
            if (entities.containsKey(value.id)) return false;
            entities.put(value.id, value); entityOrder.addElement(value.id);
            if (!categoryOrder.contains(value.category)) categoryOrder.addElement(value.category);
            return true;
        }
        public boolean registerProjectile(Projectile value) { if (projectiles.containsKey(value.id)) return false; projectiles.put(value.id, value); return true; }
        public boolean registerSound(Sound value) { if (sounds.containsKey(value.id)) return false; sounds.put(value.id, value); soundOrder.addElement(value.id); return true; }
        public Material material(String id) { return (Material) materials.get(id); }
        public Entity entity(String id) { return (Entity) entities.get(id); }
        public Projectile projectile(String id) { return (Projectile) projectiles.get(id); }
        public Sound sound(String id) { return (Sound) sounds.get(id); }
        public boolean hasMaterial(String id) { return materials.containsKey(id); }
        public boolean hasEntity(String id) { return entities.containsKey(id); }
        public boolean hasProjectile(String id) { return projectiles.containsKey(id); }
        public boolean hasSound(String id) { return sounds.containsKey(id); }
        public void unregisterEntity(String id) { entities.remove(id); entityOrder.removeElement(id); }
        public Vector entityIds() { return copy(entityOrder); }
        public Vector soundIds() { return copy(soundOrder); }
        public Vector allSounds() { Vector result = new Vector(); for (int i = 0; i < soundOrder.size(); i++) result.addElement(sounds.get(soundOrder.elementAt(i))); return result; }
        public Vector allEntities() {
            Vector result = new Vector();
            for (int i = 0; i < entityOrder.size(); i++) result.addElement(entities.get(entityOrder.elementAt(i)));
            return result;
        }
        public Vector spawnableEntities() { return filter(null); }
        public Vector entitiesInCategory(String category) { return filter(category); }
        private Vector filter(String category) {
            Vector result = new Vector();
            for (int i = 0; i < entityOrder.size(); i++) {
                Entity value = (Entity) entities.get(entityOrder.elementAt(i));
                if (!value.hidden && (category == null || category.equals(value.category))) result.addElement(value);
            }
            return result;
        }
        public Vector categories() {
            Vector result = new Vector();
            for (int i = 0; i < categoryOrder.size(); i++) {
                String category = (String) categoryOrder.elementAt(i);
                if (!filter(category).isEmpty()) result.addElement(category);
            }
            return result;
        }
        public Vector assemblies() {
            Vector result = new Vector();
            for (int i = 0; i < entityOrder.size(); i++) {
                Entity value = (Entity) entities.get(entityOrder.elementAt(i));
                if (value.isAssembly()) result.addElement(value);
            }
            return result;
        }
        public Vector sounds() {
            Vector result = new Vector();
            for (java.util.Enumeration e = sounds.elements(); e.hasMoreElements();) result.addElement(e.nextElement());
            return result;
        }
        public Json.Map modVersions() {
            Json.Map result = new Json.Map();
            for (int i = 0; i < mods.size(); i++) { Mod mod = (Mod) mods.elementAt(i); result.put(mod.id, mod.version); }
            return result;
        }
    }

    private static Vector copy(Vector source) {
        Vector result = new Vector(source.size());
        for (int i = 0; i < source.size(); i++) result.addElement(source.elementAt(i));
        return result;
    }

    public static String capitalize(String text) {
        if (text == null || text.length() == 0) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
    public static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
