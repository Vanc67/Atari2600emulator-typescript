package physicsplayground.game;

import physicsplayground.core.Json;

public final class ExplosionParams {
    public float radius = 250, force = 800, damage = 120, falloff = 1.5f;
    public boolean ignite;
    public float heat;
    public String sound = "core:explosion";
    public static ExplosionParams from(Json.Map data) {
        ExplosionParams p = new ExplosionParams();
        p.radius = clamp((float) Json.number(data.get("radius"), 250), 10, 3000);
        p.force = clamp((float) Json.number(data.get("force"), 800), 0, 100000);
        p.damage = clamp((float) Json.number(data.get("damage"), 120), 0, 100000);
        p.falloff = clamp((float) Json.number(data.get("falloff"), 1.5), 0.25f, 4);
        p.ignite = Json.bool(data.get("ignite"), false);
        p.heat = clamp((float) Json.number(data.get("heat"), p.ignite ? 400 : 0), 0, 5000);
        p.sound = Json.string(data.get("sound"), "core:explosion"); return p;
    }
    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }
}
