package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;

public final class MagazineComponent extends EntityComponent {
    public int capacity = 12, rounds = 12;
    public float reloadTime = 1.5f;
    public boolean perRound, infinite, reloading;
    public float reloadProgress;
    public String reloadSound = "core:reload";
    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        capacity = Math.max(Json.integer(params.get("capacity"), 12), 1); rounds = capacity;
        reloadTime = Math.max((float) Json.number(params.get("reload_time"), 1.5), 0);
        perRound = Json.bool(params.get("per_round"), false); infinite = Json.bool(params.get("infinite"), false);
        reloadSound = Json.string(params.get("reload_sound"), "core:reload");
    }
    public boolean hasRounds(int count) { return infinite || rounds >= count; }
    public boolean consume() {
        if (!hasRounds(1)) return false;
        if (reloading && perRound) stopReload();
        if (!infinite) rounds--;
        return true;
    }
    public boolean canFire() { return !(reloading && !perRound) && hasRounds(1); }
    public void startReload() {
        if (reloading || rounds >= capacity || infinite) return;
        reloading = true; reloadProgress = 0; play();
    }
    public void onReload() { startReload(); }
    public void tick(float dt) {
        if (!reloading) return;
        reloadProgress += dt;
        if (reloadProgress < reloadTime) return;
        if (perRound) {
            reloadProgress = 0; rounds = Math.min(rounds + 1, capacity);
            if (rounds < capacity) { play(); return; }
        } else rounds = capacity;
        stopReload();
    }
    private void stopReload() { reloading = false; reloadProgress = 0; }
    private void play() { entity.world.audio.play(reloadSound, entity.body.position, -3); }
    public Json.Map serializeState() {
        Json.Map state = new Json.Map();
        if (rounds != capacity) state.put("rounds", Json.num(rounds));
        if (reloading) state.put("reload_progress", Json.num(reloadProgress));
        return state;
    }
    public void deserializeState(Json.Map data) {
        rounds = (int) clamp(Json.integer(data.get("rounds"), rounds), 0, capacity);
        if (data.containsKey("reload_progress")) { reloading = true; reloadProgress = (float) Json.number(data.get("reload_progress"), 0); }
    }
}
