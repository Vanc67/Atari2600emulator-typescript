package physicsplayground.game;

import java.util.Vector;
import physicsplayground.game.components.ThermalComponent;
import physicsplayground.physics.Body;

public final class ThermalSystem {
    public static final float TICK_INTERVAL = 0.25f;
    public static final int MAX_NEIGHBOURS = 24;
    public int tickCount;
    private final SandboxWorld world;
    private final Vector active = new Vector();
    private final Vector neighbours = new Vector();
    private float accumulator, crackleTimer;
    public ThermalSystem(SandboxWorld world) { this.world = world; }
    public void register(ThermalComponent component) { if (!active.contains(component)) active.addElement(component); }
    public int activeCount() { return active.size(); }
    public int burningCount() {
        int count = 0; for (int i = 0; i < active.size(); i++) if (((ThermalComponent) active.elementAt(i)).burning) count++; return count;
    }
    public void clear() { active.removeAllElements(); accumulator = crackleTimer = 0; }
    public void step(float dt) {
        if (active.isEmpty()) return;
        accumulator += dt; if (accumulator < TICK_INTERVAL) return;
        float step = accumulator; accumulator = 0; tickCount++; crackleTimer += step;
        boolean crackle = crackleTimer >= 1.2f; if (crackle) crackleTimer = 0;
        int initialCount = active.size(), index = 0, processed = 0;
        while (index < active.size() && processed < initialCount) {
            ThermalComponent component = (ThermalComponent) active.elementAt(index);
            processed++;
            if (component.entity.removed) { active.removeElementAt(index); continue; }
            if (component.burning) {
                spreadHeat(component, step);
                if (crackle) world.audio.play("core:fire_crackle", component.entity.body.position, -8);
            }
            if (!component.thermalTick(step)) { active.removeElementAt(index); continue; }
            index++;
        }
    }
    private void spreadHeat(ThermalComponent source, float step) {
        Entity entity = source.entity;
        float reach = source.heatRadius + entity.definition.boundingRadius();
        world.physics.queryCircle(entity.body.position, reach, Body.ENTITIES, entity.body, neighbours);
        int count = Math.min(neighbours.size(), MAX_NEIGHBOURS);
        for (int i = 0; i < count; i++) {
            Body body = (Body) neighbours.elementAt(i);
            if (!(body.owner instanceof Entity)) continue;
            Entity neighbour = (Entity) body.owner;
            ThermalComponent thermal = neighbour.thermal;
            if (thermal == null || thermal.burning) continue;
            float distance = Math.max(entity.body.position.distanceTo(neighbour.body.position) - neighbour.definition.boundingRadius(), 0);
            float falloff = clamp(1 - distance / reach, 0, 1);
            if (falloff > 0) thermal.addHeat(source.heatOutput * falloff * step);
        }
    }
    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
}
