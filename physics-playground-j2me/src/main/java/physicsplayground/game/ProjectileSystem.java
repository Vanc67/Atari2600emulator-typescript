package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;
import physicsplayground.physics.Body;
import physicsplayground.physics.PhysicsWorld;

/** Ray/projectile simulation with the original speed, penetration, ricochet and work budgets. */
public final class ProjectileSystem {
    public static final int MAX_PROJECTILES = 600, MAX_SEGMENTS_PER_TICK = 6;
    public static final float MAX_CARRY_TIME = .25f, THICKNESS_UNIT = 40, MAX_PUSH_SPEED = 600, MIN_SPEED = 2500;
    public static final float RICOCHET_MIN_GRAZING = .45f, RICOCHET_SPEED_KEEP = .6f, RICOCHET_DAMAGE_FRACTION = .3f;
    private static final int STOP = 0, PENETRATE = 1, RICOCHET = 2;

    public static final class Projectile {
        public Definitions.Projectile definition;
        public final Vec2 position = new Vec2(), trailStart = new Vec2(), velocity = new Vec2();
        public float initialSpeed = 1, baseDamage, penetrationLeft, life, carryTime;
        public long sourceId, lastHitId;
        public Entity shooter;
        public Body lastHitBody;
        public int shooterGrace;
        public boolean checkStart;
    }
    private final SandboxWorld world;
    private final Definitions.Material surfaceMaterial;
    private final Projectile[] active = new Projectile[MAX_PROJECTILES];
    private final Vector free = new Vector();
    private int count;
    private final PhysicsWorld.RayHit hit = new PhysicsWorld.RayHit();
    private final Vec2 target = new Vec2(), direction = new Vec2(), shotDirection = new Vec2(), push = new Vec2(), offset = new Vec2();
    public int fired, hits, penetrations, ricochets, stopped, expired, budgetCarries, stalled;

    public ProjectileSystem(SandboxWorld world) {
        this.world = world; surfaceMaterial = world.content.registry.material("core:concrete");
        for (int i = 0; i < MAX_PROJECTILES; i++) free.addElement(new Projectile());
    }
    public int count() { return count; }
    public Projectile projectileAt(int index) { return active[index]; }
    public boolean fire(Definitions.Projectile definition, Vec2 from, Vec2 velocity, float damage, Entity source) {
        if (count >= MAX_PROJECTILES) return false;
        Projectile p = (Projectile) free.lastElement(); free.removeElementAt(free.size() - 1);
        p.definition = definition; p.position.set(from); p.trailStart.set(from); p.velocity.set(velocity);
        p.initialSpeed = Math.max(velocity.length(), 1); p.baseDamage = damage; p.penetrationLeft = definition.penetration;
        p.life = definition.lifetime; p.sourceId = source == null ? 0 : source.uid; p.shooter = source;
        p.shooterGrace = source == null ? 0 : 1; p.carryTime = 0; p.lastHitId = 0; p.lastHitBody = null; p.checkStart = true;
        active[count++] = p; fired++; return true;
    }
    public void clear() {
        while (count > 0) { Projectile p = active[--count]; active[count] = null; free.addElement(p); }
    }
    public void step(float dt) {
        for (int index = count - 1; index >= 0; index--) {
            Projectile p = active[index]; p.life -= dt;
            if (p.life <= 0 || !world.isInsideEscapeBounds(p.position)) { remove(index, "expired"); continue; }
            p.velocity.y += 980 * p.definition.gravityScale * dt;
            if (p.definition.drag > 0) p.velocity.scale(Math.max(1 - p.definition.drag * dt, 0));
            if (p.velocity.lengthSquared() < MIN_SPEED * MIN_SPEED) { remove(index, "expired"); continue; }
            p.trailStart.set(p.position);
            float travelTime = dt + p.carryTime; p.carryTime = 0;
            if (!advance(p, travelTime)) remove(index, "stopped");
            else if (p.carryTime > MAX_CARRY_TIME) { stalled++; remove(index, "stalled"); }
            else if (p.shooterGrace > 0) p.shooterGrace--;
        }
    }
    private boolean advance(Projectile p, float travelTime) {
        float remaining = travelTime;
        Body temporaryExclusion = null;
        if (p.checkStart) {
            p.checkStart = false;
            Body body = world.physics.pick(p.position, Body.WORLD | Body.ENTITIES, p.shooter == null ? null : p.shooter.body);
            if (body != null) {
                direction.set(p.velocity).normalize().negate();
                int outcome = resolveHit(p, body, p.position, direction);
                if (outcome == STOP) return false;
                if (outcome == PENETRATE) { direction.negate(); p.position.scaleAdd(direction, .5f); }
            }
        }
        for (int segment = 0; segment < MAX_SEGMENTS_PER_TICK; segment++) {
            target.set(p.position).scaleAdd(p.velocity, remaining);
            Body excluded = p.shooterGrace > 0 && p.shooter != null ? p.shooter.body : null;
            if (!world.physics.intersectRay(p.position, target, Body.WORLD | Body.ENTITIES, excluded, temporaryExclusion, hit, false)) {
                p.position.set(target); return true;
            }
            temporaryExclusion = null;
            float full = p.position.distanceTo(target);
            if (full > 0) remaining *= 1 - p.position.distanceTo(hit.point) / full;
            int outcome = resolveHit(p, hit.body, hit.point, hit.normal);
            if (outcome == STOP) { p.position.set(hit.point); return false; }
            if (outcome == PENETRATE) { direction.set(p.velocity).normalize(); p.position.set(hit.point).scaleAdd(direction, .5f); }
            else { p.position.set(hit.point).scaleAdd(hit.normal, .5f); temporaryExclusion = hit.body; }
        }
        if (remaining > 0) { p.carryTime = remaining; budgetCarries++; }
        return true;
    }
    private int resolveHit(Projectile p, Body body, Vec2 point, Vec2 normal) {
        hits++;
        shotDirection.set(p.velocity).normalize();
        Entity entity = body != null && body.owner instanceof Entity ? (Entity) body.owner : null;
        Definitions.Material material = entity == null ? surfaceMaterial : entity.material;
        float speedFraction = p.velocity.length() / p.initialSpeed;
        float damage = p.baseDamage * speedFraction;
        float grazing = 1 - Math.abs(shotDirection.dot(normal));
        boolean ricochet = material != null && material.ricochetChance > 0 && grazing > RICOCHET_MIN_GRAZING
                && world.random.nextFloat() < material.ricochetChance * (grazing - RICOCHET_MIN_GRAZING) / (1 - RICOCHET_MIN_GRAZING) * 1.5f;
        if (entity != null && body != p.lastHitBody) applyHit(p, entity, point, shotDirection, damage * (ricochet ? RICOCHET_DAMAGE_FRACTION : 1));
        p.lastHitBody = entity == null ? null : body; p.lastHitId = entity == null ? 0 : entity.uid;
        if (material != null) {
            world.effects.burst(material.impactEffect, point, normal, .8f);
            world.audio.play(material.impactSound, point, -4);
        }
        if (ricochet) {
            float projection = 2 * p.velocity.dot(normal);
            p.velocity.x -= projection * normal.x; p.velocity.y -= projection * normal.y;
            p.velocity.scale(RICOCHET_SPEED_KEEP); p.baseDamage *= .6f; ricochets++;
            shotDirection.set(p.velocity).normalize(); world.effects.burst("sparks", point, direction, 1);
            world.audio.play("core:ricochet", point, -6); return RICOCHET;
        }
        if (entity == null || material == null) { stopped++; return STOP; }
        float cost = material.penetrationResistance * entity.definition.minThickness() / THICKNESS_UNIT;
        if (p.penetrationLeft > cost) {
            float before = p.penetrationLeft; p.penetrationLeft -= cost;
            p.velocity.scale((float) Math.sqrt(p.penetrationLeft / before)); penetrations++; return PENETRATE;
        }
        stopped++; return STOP;
    }
    private void applyHit(Projectile p, Entity entity, Vec2 point, Vec2 direction, float damage) {
        if (entity.health != null && damage > 0) entity.health.applyDamage(damage, DamageInfo.projectile(damage, point, direction, p.sourceId));
        if (entity.body.frozen) return;
        float amount = p.definition.massGrams / 1000 * p.velocity.length() * p.definition.impulseMultiplier;
        amount = Math.min(amount, entity.body.mass * MAX_PUSH_SPEED);
        push.set(direction).scale(amount); offset.set(point).sub(entity.body.position); entity.body.applyImpulse(push, offset);
    }
    private void remove(int index, String reason) {
        Projectile p = active[index];
        for (int i = index; i < count - 1; i++) active[i] = active[i + 1];
        active[--count] = null; free.addElement(p);
        if ("expired".equals(reason)) expired++;
    }
}
