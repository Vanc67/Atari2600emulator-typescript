package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;

public final class MeleeComponent extends EntityComponent {
    public float damageMultiplier = 1.5f;
    public float thresholdMultiplier = 0.6f;
    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        damageMultiplier = clamp((float) Json.number(params.get("damage_multiplier"), 1.5), 0, 20);
        thresholdMultiplier = clamp((float) Json.number(params.get("threshold_multiplier"), 0.6), 0.1f, 1);
    }
}
