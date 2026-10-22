package physicsplayground.game;

import physicsplayground.core.Json;

public class AssemblyController {
    public EntityAssembly assembly;
    public Json.Map params = new Json.Map();
    public void setup(EntityAssembly target, Json.Map controllerParams) { assembly = target; params = controllerParams; configure(controllerParams); }
    protected void configure(Json.Map params) {}
    public void evaluate() {}
    public void onPartDamaged(String partName, DamageInfo info) { evaluate(); }
    public void onJointBroken(AssemblyJoint joint) { evaluate(); }
    public void onPartRemoved(String partName) { evaluate(); }
    public void revive() { evaluate(); }
    public String stateName() { return ""; }
    public void applyStateToPart(Entity part) {}
    public Json.Map serializeState() { return new Json.Map(); }
    public void deserializeState(Json.Map data) {}
}
