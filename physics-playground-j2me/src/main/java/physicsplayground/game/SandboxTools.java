package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;

public final class SandboxTools {
    public static final int DRAG = 0, INSPECT = 1, FREEZE = 2, DELETE = 3, HEAL = 4, DAMAGE = 5, IGNITE = 6, EXTINGUISH = 7, EXPLODE = 8;
    public static final String[] NAMES = {"Drag", "Inspect", "Freeze", "Delete", "Heal", "Damage", "Ignite", "Extinguish", "Explode"};
    private SandboxTools() {}
    public static void freezeToggle(SandboxWorld world, Entity entity) {
        boolean frozen = !entity.isFrozen(); Vector group = world.connectedGroup(entity);
        for (int i = 0; i < group.size(); i++) ((Entity) group.elementAt(i)).setFrozen(frozen);
    }
    public static void delete(SandboxWorld world, Entity entity) {
        Vector group = world.connectedGroup(entity); for (int i = 0; i < group.size(); i++) world.removeEntity((Entity) group.elementAt(i));
    }
    public static void heal(Entity entity) {
        if (entity.assembly != null) {
            entity.assembly.healAll(); for (int i = 0; i < entity.assembly.parts.size(); i++) extinguishEntity((Entity) entity.assembly.parts.elementAt(i));
        } else { if (entity.health != null) entity.health.healFull(); extinguishEntity(entity); }
    }
    public static float damage(Entity entity, float amount) { return entity.health == null ? 0 : entity.health.applyDamage(amount, DamageInfo.generic(amount)); }
    public static boolean ignite(Entity entity) { return entity.thermal != null && entity.thermal.ignite(); }
    public static void extinguish(SandboxWorld world, Entity entity) {
        Vector group = world.connectedGroup(entity); for (int i = 0; i < group.size(); i++) extinguishEntity((Entity) group.elementAt(i));
    }
    public static void extinguishEntity(Entity entity) {
        if (entity.thermal != null && (entity.thermal.burning || entity.thermal.temperature > entity.thermal.restTemperature + 1)) {
            entity.thermal.extinguish(); entity.thermal.temperature = entity.thermal.restTemperature; entity.thermal.thermalTick(0);
        }
    }
    public static void explodeAt(SandboxWorld world, Vec2 at) {
        Json.Map params = new Json.Map(); params.put("radius", Json.num(260)); params.put("force", Json.num(1100));
        params.put("damage", Json.num(160)); params.put("falloff", Json.num(1.4)); params.put("ignite", Json.bool(true));
        world.explosions.request(at, ExplosionParams.from(params), null);
    }
}
