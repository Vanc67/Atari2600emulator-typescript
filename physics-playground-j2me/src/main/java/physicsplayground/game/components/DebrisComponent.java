package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.physics.Body;

public final class DebrisComponent extends EntityComponent {
    public float lifetime = 6;
    public float age;
    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        lifetime = clamp((float) Json.number(params.get("lifetime"), 6), 0.1f, 120);
    }
    public void ready() { entity.body.layer = Body.DEBRIS; entity.body.mask = Body.WORLD; }
    public void tick(float dt) {
        age += dt;
        if (age >= lifetime) entity.requestDestroy();
        else if (age > lifetime - 1) entity.alpha = clamp(lifetime - age, 0, 1);
    }
    public Json.Map serializeState() {
        Json.Map state = new Json.Map(); if (age > 0) state.put("age", Json.num(age)); return state;
    }
    public void deserializeState(Json.Map data) { age = Math.max(0, (float) Json.number(data.get("age"), 0)); }
}
