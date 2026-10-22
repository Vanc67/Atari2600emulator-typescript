package physicsplayground.game.components;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;

public final class FirearmComponent extends EntityComponent {
    public Definitions.Projectile projectile;
    public String projectileId = "";
    public float fireRate = 6, speed = -1, damage = -1;
    public boolean automatic, triggerHeld;
    public int pellets = 1, shotsFired;
    public float spreadDegrees = 1.5f, recoil = 2;
    public final Vec2 muzzle = new Vec2();
    public float muzzleFlash = 14, cooldown;
    public String sound = "core:gunshot_light", drySound = "core:dry_fire";
    private final Vec2 origin = new Vec2(), forward = new Vec2(), direction = new Vec2(), velocity = new Vec2(), impulse = new Vec2(), offset = new Vec2();

    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        projectileId = Json.string(params.get("projectile"), "");
        fireRate = clamp((float) Json.number(params.get("fire_rate"), 6), 0.1f, 60);
        automatic = Json.bool(params.get("automatic"), false);
        speed = (float) Json.number(params.get("speed"), -1); damage = (float) Json.number(params.get("damage"), -1);
        pellets = (int) clamp(Json.integer(params.get("pellets"), 1), 1, 32);
        spreadDegrees = clamp((float) Json.number(params.get("spread"), 1.5), 0, 90);
        recoil = clamp((float) Json.number(params.get("recoil"), 2), 0, 1000);
        Vector raw = Json.array(params.get("muzzle"));
        if (raw != null && raw.size() == 2) muzzle.set(((Json.Num) raw.elementAt(0)).floatValue(), ((Json.Num) raw.elementAt(1)).floatValue());
        else muzzle.set(definition.boundingWidth() * 0.5f, 0);
        muzzleFlash = Math.max((float) Json.number(params.get("muzzle_flash"), 14), 0);
        sound = Json.string(params.get("sound"), sound); drySound = Json.string(params.get("dry_sound"), drySound);
        projectile = entity.world.content.registry.projectile(projectileId);
    }
    public boolean isActivatable() { return true; }
    public void onActivate(boolean pressed) {
        triggerHeld = pressed;
        if (pressed && cooldown <= 0) tryFire();
    }
    public void onReload() { if (entity.magazine != null) entity.magazine.startReload(); }
    public void tick(float dt) {
        cooldown = Math.max(cooldown - dt, 0);
        if (cooldown <= 0 && triggerHeld && automatic) tryFire();
    }
    public boolean tryFire() {
        if (cooldown > 0 || entity.removed || entity.world.paused || projectile == null) return false;
        cooldown = 1 / fireRate;
        if (entity.magazine != null && !entity.magazine.canFire()) {
            if (!entity.magazine.reloading) entity.world.audio.play(drySound, entity.body.position, -6);
            return false;
        }
        if (entity.magazine != null && !entity.magazine.consume()) return false;
        emitProjectiles(); shotsFired++; return true;
    }
    private void emitProjectiles() {
        entity.body.toWorld(muzzle, origin);
        forward.set(entity.body.cosine, entity.body.sine);
        float muzzleSpeed = (speed > 0 ? speed : projectile.speed) * 100;
        float shotDamage = damage >= 0 ? damage : projectile.damage;
        float halfSpread = (float) Math.toRadians(spreadDegrees) * 0.5f;
        for (int i = 0; i < pellets; i++) {
            direction.set(forward).rotate((entity.world.random.nextFloat() * 2 - 1) * halfSpread);
            float pelletSpeed = muzzleSpeed * (pellets > 1 ? 0.92f + entity.world.random.nextFloat() * 0.08f : 1);
            velocity.set(direction).scale(pelletSpeed).add(entity.body.linearVelocity);
            entity.world.projectiles.fire(projectile, origin, velocity, shotDamage, entity);
        }
        if (muzzleFlash > 0) entity.world.effects.muzzleFlash(origin, forward, muzzleFlash);
        entity.world.audio.play(sound, origin, 0);
        applyRecoil();
    }
    private void applyRecoil() {
        if (entity.body.frozen || recoil <= 0) return;
        float amount = Math.min(recoil * 100, entity.body.mass * 900);
        impulse.set(forward).scale(-amount); offset.set(origin).sub(entity.body.position);
        entity.body.applyImpulse(impulse, offset);
        entity.body.angularVelocity = clamp(entity.body.angularVelocity, -25, 25);
        entity.body.linearVelocity.limit(1800); entity.body.sleeping = false;
    }
    public Json.Map serializeState() { Json.Map state = new Json.Map(); if (triggerHeld) state.put("trigger_held", Json.bool(true)); return state; }
    public void deserializeState(Json.Map data) { triggerHeld = Json.bool(data.get("trigger_held"), false); }
}
