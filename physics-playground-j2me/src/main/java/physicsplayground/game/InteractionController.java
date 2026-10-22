package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;
import physicsplayground.save.EntitySerializer;

public final class InteractionController {
    public static final float ROTATE_TARGET_SPEED = 5, ROTATE_RESPONSE = 12, ROTATE_DIRECT_SPEED = 2.5f, GHOST_ROTATE_SPEED = 2.5f, DUPLICATE_GAP = 6;
    private final SandboxWorld world;
    public Entity selectedEntity, activatedEntity;
    public int currentTool = SandboxTools.DRAG;
    public float toolDamageAmount = 25;
    public DragHandle drag;
    public EntityAssembly dragProtectedAssembly;
    public final SpawnPlacer spawnPlacer;
    public String lastMessage = "";
    public InteractionController(SandboxWorld world) { this.world = world; spawnPlacer = new SpawnPlacer(world); world.interaction = this; }
    public boolean isSpawning() { return spawnPlacer.isActive(); }
    public String spawnTypeId() { return isSpawning() ? spawnPlacer.definition.id : ""; }
    public void beginSpawn(String type) { Definitions.Entity definition = world.content.registry.entity(type); if (definition == null) return; endDrag(); spawnPlacer.begin(definition); }
    public void cancelSpawn() { spawnPlacer.cancel(); }
    public void select(Entity entity) {
        if (entity == selectedEntity) return; if (selectedEntity != null) selectedEntity.selected = false;
        selectedEntity = entity; if (entity != null) entity.selected = true;
    }
    public void clearSelection() { select(null); }
    public boolean hasSelection() { return selectedEntity != null && !selectedEntity.removed; }
    public Vector selectedGroup() { if (!hasSelection()) return new Vector(); return world.connectedGroup(selectedEntity); }
    public void deleteSelected() { Vector group = selectedGroup(); for (int i = 0; i < group.size(); i++) world.removeEntity((Entity) group.elementAt(i)); }
    public void toggleFreezeSelected() { if (!hasSelection()) return; boolean frozen = !selectedEntity.body.frozen; Vector group = selectedGroup(); for (int i = 0; i < group.size(); i++) ((Entity) group.elementAt(i)).setFrozen(frozen); }
    public void healSelected() { if (hasSelection()) SandboxTools.heal(selectedEntity); }
    public void setTool(int tool) { if (tool == currentTool) return; currentTool = tool; endDrag(); cancelSpawn(); }
    public void activateSelected(boolean pressed) {
        if (pressed) { releaseActivation(); if (hasSelection() && selectedEntity.isActivatable()) { activatedEntity = selectedEntity; activatedEntity.activate(true); } }
        else releaseActivation();
    }
    public void releaseActivation() { if (activatedEntity != null && !activatedEntity.removed) activatedEntity.activate(false); activatedEntity = null; }
    public void reloadSelected() { if (hasSelection()) selectedEntity.reload(); }
    public void applyToolAt(Vec2 position) {
        lastMessage = "";
        if (currentTool == SandboxTools.EXPLODE) { SandboxTools.explodeAt(world, position); return; }
        Entity entity = world.pickEntityAt(position); select(entity); if (entity == null) return;
        if (currentTool == SandboxTools.FREEZE) SandboxTools.freezeToggle(world, entity);
        else if (currentTool == SandboxTools.DELETE) SandboxTools.delete(world, entity);
        else if (currentTool == SandboxTools.HEAL) SandboxTools.heal(entity);
        else if (currentTool == SandboxTools.DAMAGE) SandboxTools.damage(entity, toolDamageAmount);
        else if (currentTool == SandboxTools.IGNITE && !SandboxTools.ignite(entity)) lastMessage = entity.definition.displayName + " is not flammable.";
        else if (currentTool == SandboxTools.EXTINGUISH) SandboxTools.extinguish(world, entity);
    }
    public void duplicateSelected() {
        if (!hasSelection()) return; Vector group = selectedGroup(); Vec2 offset = findFreeOffset(group);
        if (group.size() > 1) {
            EntityAssembly copy = world.duplicateGroup(group, offset);
            if (copy != null) select(copy.part(selectedEntity.assemblyPartName)); return;
        }
        Entity source = selectedEntity; Vec2 at = source.body.position.copy().add(offset);
        Entity copy = world.spawnEntity(source.definition.id, at, source.body.rotation);
        if (copy != null) { EntitySerializer.applyState(copy, EntitySerializer.serialize(source), false); select(copy); }
    }
    public void beginDrag(Entity entity, Vec2 at) {
        endDrag(); drag = new DragHandle(entity, at);
        if (entity.assembly != null) { dragProtectedAssembly = entity.assembly; dragProtectedAssembly.beginDragProtection(); }
    }
    public void endDrag() { drag = null; if (dragProtectedAssembly != null) dragProtectedAssembly.endDragProtection(); dragProtectedAssembly = null; }
    private Vec2 findFreeOffset(Vector group) {
        Entity first = (Entity) group.elementAt(0); float minX = first.body.position.x, minY = first.body.position.y, maxX = minX, maxY = minY;
        for (int i = 0; i < group.size(); i++) {
            Entity entity = (Entity) group.elementAt(i); float r = entity.definition.boundingRadius();
            minX = Math.min(minX, entity.body.position.x - r); maxX = Math.max(maxX, entity.body.position.x + r);
            minY = Math.min(minY, entity.body.position.y - r); maxY = Math.max(maxY, entity.body.position.y + r);
        }
        float width = maxX - minX, height = maxY - minY;
        Vec2[] candidates = {new Vec2(0, -(height + DUPLICATE_GAP)), new Vec2(width + DUPLICATE_GAP, 0), new Vec2(-(width + DUPLICATE_GAP), 0), new Vec2(0, -(height + DUPLICATE_GAP) * 2)};
        for (int c = 0; c < candidates.length; c++) {
            boolean fits = true;
            for (int i = 0; i < group.size(); i++) {
                Entity entity = (Entity) group.elementAt(i); Vec2 at = entity.body.position.copy().add(candidates[c]);
                if (!world.canPlace(entity.definition, at, entity.body.rotation)) { fits = false; break; }
            }
            if (fits) return candidates[c];
        }
        return candidates[0];
    }
    public boolean tryPlaceSpawn(Vec2 at) {
        spawnPlacer.updatePreview(at);
        if (!spawnPlacer.placementValid) { lastMessage = "Can't place here: the space is occupied or outside the play area."; return false; }
        world.spawn(spawnPlacer.definition.id, at, spawnPlacer.placementRotation); return true;
    }
    public void physicsStep(float realDelta, float dt, float rotateInput, Vec2 cursorWorld, boolean dragHeld) {
        if (selectedEntity != null && selectedEntity.removed) select(null);
        if (activatedEntity != null && activatedEntity.removed) activatedEntity = null;
        if (drag != null) {
            if (!drag.isValid() || !dragHeld) endDrag();
            else { drag.targetPosition.set(cursorWorld); drag.update(world.paused, rotateInput != 0, dt); }
        }
        if (rotateInput == 0) return;
        if (isSpawning()) spawnPlacer.rotateGhost(rotateInput * GHOST_ROTATE_SPEED * realDelta);
        else if (hasSelection()) rotateEntity(selectedEntity, rotateInput, realDelta);
    }
    private void rotateEntity(Entity entity, float direction, float realDelta) {
        if (entity.body.frozen || world.paused) { entity.body.rotation += direction * ROTATE_DIRECT_SPEED * realDelta; entity.body.updateTransform(); return; }
        if (entity.body.invInertia <= 0) return;
        float inertia = 1 / entity.body.invInertia, target = direction * ROTATE_TARGET_SPEED;
        entity.body.applyTorque((target - entity.body.angularVelocity) * inertia * ROTATE_RESPONSE); entity.body.sleeping = false;
    }
    public void onEntityRemoved(Entity entity) {
        if (activatedEntity == entity) activatedEntity = null; if (drag != null && drag.entity == entity) endDrag(); if (selectedEntity == entity) select(null);
    }
}
