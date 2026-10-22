package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Json;

public final class HumanController extends AssemblyController {
    public static final int ALIVE = 0, INCAPACITATED = 1, DEAD = 2;
    private static final String[] NAMES = {"alive", "incapacitated", "dead"};
    public int state;
    public String stateReason = "";
    private final Vector vitalParts = new Vector(), fatalJoints = new Vector(), incapacitatingParts = new Vector();
    private float incapacitatedBelow = .55f, vitalCriticalFraction;
    private int incapacitatedDestroyedParts;
    protected void configure(Json.Map params) {
        strings(params.get("vital_parts"), vitalParts); strings(params.get("fatal_joints"), fatalJoints); strings(params.get("incapacitating_parts"), incapacitatingParts);
        incapacitatedBelow = clamp((float) Json.number(params.get("incapacitated_below"), .55), 0, 1);
        vitalCriticalFraction = clamp((float) Json.number(params.get("vital_critical_fraction"), 0), 0, 1);
        incapacitatedDestroyedParts = Math.max(Json.integer(params.get("incapacitated_destroyed_parts"), 0), 0);
    }
    public void evaluate() {
        if (state == DEAD) return;
        for (int i = 0; i < vitalParts.size(); i++) {
            String name = (String) vitalParts.elementAt(i); Entity part = assembly.part(name);
            if (part == null) { setState(DEAD, label(name) + " lost"); return; }
            if (part.health != null && part.health.healthFraction() <= vitalCriticalFraction) { setState(DEAD, label(name) + " destroyed"); return; }
        }
        for (int i = 0; i < fatalJoints.size(); i++) {
            AssemblyJoint joint = assembly.joint((String) fatalJoints.elementAt(i));
            if (joint != null && joint.broken) { setState(DEAD, capitalize(joint.definition.name) + " severed"); return; }
        }
        for (int i = 0; i < incapacitatingParts.size(); i++) {
            String name = (String) incapacitatingParts.elementAt(i); Entity part = assembly.part(name);
            if (part == null || (part.health != null && part.health.isDepleted())) { setState(INCAPACITATED, label(name) + " destroyed"); return; }
        }
        int destroyed = destroyedParts();
        if (incapacitatedDestroyedParts > 0 && destroyed >= incapacitatedDestroyedParts) { setState(INCAPACITATED, destroyed + " parts destroyed"); return; }
        if (assembly.totalHealthFraction() < incapacitatedBelow) { setState(INCAPACITATED, "severe injuries"); return; }
        setState(ALIVE, "");
    }
    public void revive() { state = ALIVE; stateReason = ""; applyVisuals(); evaluate(); }
    public String stateName() { return NAMES[state]; }
    public boolean isAlive() { return state == ALIVE; }
    public boolean isDead() { return state == DEAD; }
    public void applyStateToPart(Entity part) { part.vitality = state; }
    public Json.Map serializeState() { Json.Map data = new Json.Map(); data.put("state", stateName()); data.put("reason", stateReason); return data; }
    public void deserializeState(Json.Map data) {
        String value = Json.string(data.get("state"), "alive"); state = ALIVE;
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(value)) state = i;
        stateReason = Json.string(data.get("reason"), ""); applyVisuals(); evaluate();
    }
    private int destroyedParts() { int count = 0; for (int i = 0; i < assembly.parts.size(); i++) { Entity part = (Entity) assembly.parts.elementAt(i); if (part.health != null && part.health.isDepleted()) count++; } return count; }
    private void setState(int value, String reason) { stateReason = reason; if (state == value) return; state = value; applyVisuals(); }
    private void applyVisuals() { for (int i = 0; i < assembly.parts.size(); i++) applyStateToPart((Entity) assembly.parts.elementAt(i)); }
    private String label(String name) { DefinitionsPart: { physicsplayground.core.Definitions.Part part = assembly.definition.assembly.part(name); if (part != null) return part.displayName; } return capitalize(name); }
    private static String capitalize(String value) { return value.length() == 0 ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1); }
    private static void strings(Object value, Vector output) { Vector array = Json.array(value); if (array == null) return; for (int i = 0; i < array.size(); i++) output.addElement(String.valueOf(array.elementAt(i))); }
    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
}
