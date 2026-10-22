package physicsplayground.game;

import physicsplayground.core.Definitions;
import physicsplayground.core.Math2;
import physicsplayground.core.Vec2;

public final class SpawnPlacer {
    private final SandboxWorld world;
    public Definitions.Entity definition;
    public float placementRotation;
    public boolean placementValid;
    public final Vec2 position = new Vec2();
    public SpawnPlacer(SandboxWorld world) { this.world = world; }
    public boolean isActive() { return definition != null; }
    public void begin(Definitions.Entity value) { definition = value; placementRotation = 0; placementValid = false; }
    public void cancel() { definition = null; placementValid = false; }
    public void rotateGhost(float amount) { placementRotation = Math2.wrapAngle(placementRotation + amount); }
    public void updatePreview(Vec2 worldPosition) {
        if (!isActive()) return; position.set(worldPosition); placementValid = world.canPlace(definition, worldPosition, placementRotation);
    }
}
