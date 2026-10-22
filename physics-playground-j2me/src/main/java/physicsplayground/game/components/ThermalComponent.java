package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.game.DamageInfo;

public final class ThermalComponent extends EntityComponent {
    public static final float AMBIENT = 20;
    public float temperature = AMBIENT, restTemperature = AMBIENT, conductivity;
    public float ignitionTemperature = -1, flammability, heatResistance;
    public float burnDamage = 8, burnDuration = 10, burnTemperature = 700;
    public float heatRadius = 70, heatOutput = 160, cooling = 0.08f;
    public boolean burning;
    public float fuelRemaining;
    public float charLevel;

    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        temperature = (float) Json.number(params.get("initial_temperature"), AMBIENT); restTemperature = temperature;
        conductivity = material == null ? 0 : material.thermalConductivity;
        ignitionTemperature = (float) Json.number(params.get("ignition_temperature"), material == null ? -1 : material.ignitionTemperature);
        flammability = clamp((float) Json.number(params.get("flammability"), material == null ? 0 : material.flammability), 0, 1);
        heatResistance = clamp((float) Json.number(params.get("heat_resistance"), 0), 0, 1);
        burnDamage = Math.max((float) Json.number(params.get("burn_damage"), 8), 0);
        burnDuration = (float) Json.number(params.get("burn_duration"), 10);
        burnTemperature = (float) Json.number(params.get("burn_temperature"), 700);
        heatRadius = Math.max((float) Json.number(params.get("heat_radius"), 70), 0);
        heatOutput = Math.max((float) Json.number(params.get("heat_output"), 160), 0);
        cooling = clamp((float) Json.number(params.get("cooling"), 0.08 * (1 + conductivity)), 0, 10);
    }
    public void ready() { if (burning || Math.abs(temperature - restTemperature) > 1) wake(); }
    public boolean canIgnite() { return ignitionTemperature >= 0 && flammability > 0; }
    public void addHeat(float amount) {
        if (amount <= 0) return;
        temperature += amount * (1 - heatResistance); wake();
        if (!burning && canIgnite() && temperature >= ignitionTemperature && entity.world.random.nextFloat() < flammability) ignite();
        entity.onTemperatureChanged();
    }
    public boolean tryIgnite(float chanceScale) {
        if (burning) return true;
        if (canIgnite() && entity.world.random.nextFloat() < flammability * chanceScale) ignite();
        return burning;
    }
    public boolean ignite() {
        if (burning || !canIgnite()) return burning;
        burning = true; fuelRemaining = burnDuration; temperature = Math.max(temperature, burnTemperature);
        wake(); entity.world.effects.setBurning(entity, true); entity.onTemperatureChanged(); return true;
    }
    public void extinguish() {
        burning = false;
        temperature = Math.min(temperature, Math.max(restTemperature + 30, ignitionTemperature * 0.5f));
        entity.world.effects.setBurning(entity, false); entity.onTemperatureChanged();
    }
    public boolean thermalTick(float dt) {
        if (burning) {
            temperature = Math.max(temperature, burnTemperature);
            charLevel = Math.min(charLevel + dt / 20, 1);
            if (burnDuration >= 0) { fuelRemaining -= dt; if (fuelRemaining <= 0) extinguish(); }
            if (entity.health != null && burnDamage > 0) {
                float damage = burnDamage * dt * (1 - heatResistance);
                entity.health.applyDamage(damage, DamageInfo.fire(damage));
            }
        } else temperature += (restTemperature - temperature) * Math.min(cooling * dt, 1);
        entity.onTemperatureChanged();
        return burning || Math.abs(temperature - restTemperature) > 1;
    }
    private void wake() { if (!entity.removed) entity.world.thermal.register(this); }
    public Json.Map serializeState() {
        Json.Map state = new Json.Map();
        if (Math.abs(temperature - restTemperature) > 1) state.put("temperature", Json.num(temperature));
        if (burning) { state.put("burning", Json.bool(true)); state.put("fuel", Json.num(fuelRemaining)); }
        if (charLevel > 0) state.put("char", Json.num(charLevel));
        return state;
    }
    public void deserializeState(Json.Map data) {
        temperature = (float) Json.number(data.get("temperature"), temperature);
        charLevel = clamp((float) Json.number(data.get("char"), charLevel), 0, 1);
        if (Json.bool(data.get("burning"), false) && canIgnite()) { ignite(); fuelRemaining = (float) Json.number(data.get("fuel"), burnDuration); }
        wake();
    }
}
