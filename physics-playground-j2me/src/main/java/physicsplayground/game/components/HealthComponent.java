package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.game.DamageInfo;

public final class HealthComponent extends EntityComponent {
    public float maxHealth = 100, currentHealth = 100;
    public String onDepleted = "none";
    public boolean invulnerable;
    public float damageResistance;
    public Json.Map damageMultipliers = new Json.Map();

    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        float base = (float) Json.number(params.get("max_health"), 100);
        float durability = material == null ? 1 : material.durability;
        maxHealth = Math.max(base * durability, 1); currentHealth = maxHealth;
        String behaviour = Json.string(params.get("on_depleted"), "none");
        onDepleted = "remove".equals(behaviour) ? "remove" : "none";
        damageResistance = clamp((float) Json.number(params.get("damage_resistance"), 0), 0, 1);
        Json.Map multipliers = Json.object(params.get("damage_multipliers"));
        if (multipliers != null) damageMultipliers = (Json.Map) Json.deepCopy(multipliers);
    }
    public float applyDamage(float amount, DamageInfo info) {
        if (amount <= 0 || invulnerable || isDepleted()) return 0;
        if (info == null) info = DamageInfo.generic(amount);
        amount *= (1 - damageResistance) * (float) Json.number(damageMultipliers.get(info.kindName()), 1);
        if (amount <= 0) return 0;
        float applied = Math.min(amount, currentHealth);
        currentHealth -= applied; info.applied = applied;
        entity.onDamaged(info);
        entity.onHealthChanged();
        if (isDepleted()) {
            entity.onDepleted(info);
            if ("remove".equals(onDepleted)) entity.requestDestroy();
        }
        return applied;
    }
    public float heal(float amount) {
        if (amount <= 0) return 0;
        boolean depleted = isDepleted();
        float healed = Math.min(amount, maxHealth - currentHealth);
        if (healed <= 0) return 0;
        currentHealth += healed;
        entity.onHealthChanged();
        return healed;
    }
    public float healFull() { return heal(maxHealth); }
    public boolean isDepleted() { return currentHealth <= 0; }
    public boolean isDamaged() { return currentHealth < maxHealth; }
    public float healthFraction() { return currentHealth / maxHealth; }
    public float damageFraction() { return 1 - healthFraction(); }
    public Json.Map serializeState() {
        Json.Map state = new Json.Map();
        if (Math.abs(currentHealth - maxHealth) > 0.00001f) state.put("current", Json.num(currentHealth));
        return state;
    }
    public void deserializeState(Json.Map data) {
        currentHealth = clamp((float) Json.number(data.get("current"), maxHealth), 0, maxHealth);
        entity.onHealthChanged();
    }
}
