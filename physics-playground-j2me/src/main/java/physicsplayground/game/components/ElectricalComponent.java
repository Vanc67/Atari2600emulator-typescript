package physicsplayground.game.components;

import physicsplayground.core.Definitions;
import physicsplayground.core.Json;

public final class ElectricalComponent extends EntityComponent {
    public float conductivity;
    public float charge;
    public void configure(Json.Map params, Definitions.Entity definition, Definitions.Material material) {
        conductivity = clamp((float) Json.number(params.get("conductivity"), material == null ? 0 : material.electricalConductivity), 0, 1);
    }
    public boolean isConductor() { return conductivity >= 0.5f; }
    public Json.Map serializeState() { Json.Map state = new Json.Map(); if (Math.abs(charge) > 0.00001f) state.put("charge", Json.num(charge)); return state; }
    public void deserializeState(Json.Map data) { charge = (float) Json.number(data.get("charge"), 0); }
}
