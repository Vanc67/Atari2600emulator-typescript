package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;

public final class DestructibleComponent extends EntityComponent {
    public float threshold;
    public String debrisId = "";
    public int debrisCount = 4;
    public float debrisSpeed = 180;
    public String effect = "debris";
    public String sound = "";
    public boolean destroyed;

    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        threshold = clamp((float) Json.number(params.get("threshold"), 0), 0, 0.99f);
        debrisId = Json.string(params.get("debris"), "");
        debrisCount = (int) clamp(Json.integer(params.get("debris_count"), 4), 0, 16);
        debrisSpeed = Math.max((float) Json.number(params.get("debris_speed"), 180), 0);
        effect = Json.string(params.get("effect"), "debris");
        sound = Json.string(params.get("sound"), "");
    }
    public void onHealthChanged() {
        if (destroyed || entity.health.currentHealth > entity.health.maxHealth * threshold) return;
        ExplosiveComponent explosive = entity.explosive;
        if (explosive != null && explosive.detonateOnDepleted && !explosive.hasDetonated) return;
        destroy(false);
    }
    public void destroy(boolean fromExplosion) {
        if (destroyed || entity.removed) return;
        destroyed = true;
        if (!fromExplosion) entity.world.effects.burst(effect, entity.body.position, null, clamp(entity.definition.boundingRadius() / 30, 0.6f, 3));
        if (sound.length() > 0) entity.world.audio.play(sound, entity.body.position, 0);
        if (debrisId.length() > 0 && debrisCount > 0) {
            entity.world.debrisRequests.addElement(new physicsplayground.game.SandboxWorld.DebrisRequest(
                    debrisId, entity.body.position, entity.body.rotation, debrisCount, debrisSpeed,
                    entity.body.linearVelocity, entity.definition.size.x * 0.35f, entity.definition.size.y * 0.35f));
        }
        entity.requestDestroy();
    }
}
