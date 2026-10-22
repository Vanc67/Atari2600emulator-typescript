package physicsplayground.game.components;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.game.DamageInfo;
import physicsplayground.game.ExplosionParams;

public final class ExplosiveComponent extends EntityComponent {
    public ExplosionParams params;
    public float fuse = 3, chainFuseMin = 0.05f, chainFuseMax = 0.2f;
    public boolean detonateOnDepleted = true;
    public float detonateTemperature = -1;
    public String fuseSound = "core:fuse_beep";
    public boolean armed, hasDetonated;
    public float fuseRemaining, beepTimer;
    public void configure(Json.Map data, Definitions.Entity definition, Definitions.Material material) {
        params = ExplosionParams.from(data);
        fuse = Math.max((float) Json.number(data.get("fuse"), 3), 0);
        Vector chain = Json.array(data.get("chain_fuse"));
        if (chain != null && chain.size() == 2) {
            chainFuseMin = Math.max(((Json.Num) chain.elementAt(0)).floatValue(), 0);
            chainFuseMax = Math.max(((Json.Num) chain.elementAt(1)).floatValue(), 0);
        }
        detonateOnDepleted = Json.bool(data.get("detonate_on_depleted"), true);
        detonateTemperature = (float) Json.number(data.get("detonate_temperature"), -1);
        fuseSound = Json.string(data.get("fuse_sound"), "core:fuse_beep");
    }
    public boolean isActivatable() { return true; }
    public void onActivate(boolean pressed) { if (pressed) arm(fuse); }
    public void arm(float seconds) {
        if (hasDetonated || (armed && fuseRemaining <= seconds)) return;
        armed = true; fuseRemaining = seconds; beepTimer = 0; entity.armedVisual = true;
    }
    public void disarm() { armed = false; entity.armedVisual = false; }
    public void tick(float dt) {
        if (!armed) return;
        fuseRemaining -= dt; beepTimer -= dt;
        if (beepTimer <= 0 && fuseRemaining > 0.15f) {
            beepTimer = clamp(fuseRemaining * 0.25f, 0.08f, 0.5f);
            entity.world.audio.play(fuseSound, entity.body.position, -10); entity.blink = !entity.blink;
        }
        if (fuseRemaining <= 0) detonate();
    }
    public void detonate() {
        if (hasDetonated || entity.removed) return;
        if (!entity.world.explosions.request(entity.body.position, params, entity)) {
            if (!armed || fuseRemaining > 0) entity.world.explosions.noteDeferredDetonation();
            armed = true; fuseRemaining = 0; return;
        }
        hasDetonated = true; armed = false;
        if (entity.destructible != null) entity.destructible.destroy(true); else entity.requestDestroy();
    }
    public void onDepleted(DamageInfo info) { if (detonateOnDepleted) arm(randomChain(0)); }
    public void onTemperatureChanged(float temperature) {
        if (detonateTemperature > 0 && temperature >= detonateTemperature) arm(randomChain(0.4f));
    }
    private float randomChain(float extra) { return chainFuseMin + entity.world.random.nextFloat() * (chainFuseMax - chainFuseMin) + extra; }
    public Json.Map serializeState() { Json.Map state = new Json.Map(); if (armed) { state.put("armed", Json.bool(true)); state.put("fuse", Json.num(fuseRemaining)); } return state; }
    public void deserializeState(Json.Map data) { if (Json.bool(data.get("armed"), false)) arm(Math.max((float) Json.number(data.get("fuse"), fuse), 0)); }
}
