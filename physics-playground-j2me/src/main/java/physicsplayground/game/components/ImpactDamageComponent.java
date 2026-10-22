package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;
import physicsplayground.game.DamageInfo;
import physicsplayground.game.Entity;
import physicsplayground.physics.Body;

public final class ImpactDamageComponent extends EntityComponent {
    public static final int MAX_TRACKED = 8;
    public float minImpactSpeed = 7, damagePerSpeed = 12, minImpulse, maxDamagePerHit = 1000, cooldown = 0.15f;
    private final Body[] colliders = new Body[MAX_TRACKED];
    private final float[] impulses = new float[MAX_TRACKED];
    private final float[] changes = new float[MAX_TRACKED];
    private final Vec2[] points = new Vec2[MAX_TRACKED];
    private int tracked;
    private final Body[] cooldownBodies = new Body[32];
    private final float[] cooldownUntil = new float[32];
    private int cooldownCount;

    public ImpactDamageComponent() { for (int i = 0; i < points.length; i++) points[i] = new Vec2(); }
    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        minImpactSpeed = Math.max((float) Json.number(params.get("min_impact_speed"), 7), 0);
        damagePerSpeed = Math.max((float) Json.number(params.get("damage_per_speed"), 12), 0);
        minImpulse = Math.max((float) Json.number(params.get("min_impulse"), 0), 0);
        maxDamagePerHit = Math.max((float) Json.number(params.get("max_damage_per_hit"), 1000), 0);
        cooldown = Math.max((float) Json.number(params.get("cooldown"), 0.15), 0);
    }
    public void beginStep() { tracked = 0; }
    public void contact(Body other, Vec2 point, float normalChange, float impulsePixels) {
        float inverseMass = entity.body.invMass;
        if (inverseMass <= 0) return;
        float threshold = minImpactSpeed * 100;
        float scale = thresholdScale(other);
        if (normalChange < threshold * 0.1f || normalChange < threshold * scale) return;
        int slot = -1;
        for (int i = 0; i < tracked; i++) if (colliders[i] == other) { slot = i; break; }
        if (slot < 0) {
            if (tracked == MAX_TRACKED) return;
            slot = tracked++; colliders[slot] = other; impulses[slot] = 0; changes[slot] = 0;
        }
        impulses[slot] += impulsePixels;
        changes[slot] = Math.max(changes[slot], normalChange);
        points[slot].set(point);
    }
    public void finishStep() {
        float inverseMass = entity.body.invMass;
        if (inverseMass <= 0) return;
        for (int i = 0; i < tracked; i++) {
            float impulse = impulses[i], severity = changes[i];
            if (impulse > 0) {
                severity = Math.min(severity, impulse * inverseMass);
                if (impulse / 100 < minImpulse) continue;
            }
            Body other = colliders[i];
            float scale = thresholdScale(other);
            if (severity < minImpactSpeed * 100 * scale) continue;
            float speed = severity / 100;
            float impulseSi = Math.max(impulse, severity / inverseMass) / 100;
            registerImpact(speed, impulseSi, points[i], other);
        }
    }
    public float computeDamage(float speed, float thresholdScale, float multiplier) {
        return clamp((speed - minImpactSpeed * thresholdScale) * damagePerSpeed * multiplier, 0, maxDamagePerHit);
    }
    private void registerImpact(float speed, float impulse, Vec2 point, Body other) {
        float now = entity.world.simulationTime;
        int slot = -1;
        for (int i = 0; i < cooldownCount; i++) if (cooldownBodies[i] == other) { slot = i; break; }
        if (slot >= 0 && now < cooldownUntil[slot]) return;
        if (slot < 0) {
            if (cooldownCount < cooldownBodies.length) slot = cooldownCount++;
            else {
                slot = 0;
                for (int i = 1; i < cooldownCount; i++) if (cooldownUntil[i] < cooldownUntil[slot]) slot = i;
            }
        }
        cooldownBodies[slot] = other; cooldownUntil[slot] = now + cooldown;
        Entity source = other != null && other.owner instanceof Entity ? (Entity) other.owner : null;
        MeleeComponent melee = source == null ? null : source.melee;
        float scale = melee == null ? 1 : melee.thresholdMultiplier;
        float multiplier = melee == null ? 1 : melee.damageMultiplier;
        float damage = computeDamage(speed, scale, multiplier);
        if (entity.health != null && damage > 0) entity.health.applyDamage(damage,
                DamageInfo.impact(damage, speed, impulse, point, other == null ? 0 : other.id));
    }
    private static float thresholdScale(Body body) {
        if (body != null && body.owner instanceof Entity) {
            MeleeComponent melee = ((Entity) body.owner).melee;
            if (melee != null) return melee.thresholdMultiplier;
        }
        return 1;
    }
}
