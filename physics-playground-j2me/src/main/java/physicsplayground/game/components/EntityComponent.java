package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.game.Entity;

public abstract class EntityComponent {
    public Entity entity;
    public abstract void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material);
    public void ready() {}
    public void tick(float dt) {}
    public void onActivate(boolean pressed) {}
    public void onReload() {}
    public boolean isActivatable() { return false; }
    public Json.Map serializeState() { return new Json.Map(); }
    public void deserializeState(Json.Map data) {}
    protected static float clamp(float value, float min, float max) { return value < min ? min : (value > max ? max : value); }
}
