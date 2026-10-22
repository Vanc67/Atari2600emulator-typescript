package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Vec2;

public final class DragHandle {
    public static final float NATURAL_FREQUENCY = 26, DAMPING_RATIO = 1, MAX_SPRING_ACCELERATION = 30000, SPIN_DAMPING = 5;
    public static final float GROUP_FREQUENCY = 7, GROUP_DAMPING_RATIO = 1, MAX_GROUP_ACCELERATION = 2500, SUPPORT_RAMP_TIME = .25f;
    public final Entity entity;
    public final Vec2 localAnchor = new Vec2(), targetPosition = new Vec2();
    private final Vec2 anchor = new Vec2(), offset = new Vec2(), pointVelocity = new Vec2(), error = new Vec2(), acceleration = new Vec2(), force = new Vec2(), groupVelocity = new Vec2(), shift = new Vec2();
    private Vector group = new Vector();
    private float groupMass, support;
    private int groupVersion = -1;
    public DragHandle(Entity grabbed, Vec2 grabWorld) {
        entity = grabbed; grabbed.body.toLocal(grabWorld, localAnchor); targetPosition.set(grabWorld); refreshGroup();
    }
    public boolean isValid() { return !entity.removed; }
    public Vec2 anchorWorld() { entity.body.toWorld(localAnchor, anchor); return anchor; }
    public Vector group() { refreshGroup(); return group; }
    public void update(boolean worldPaused, boolean allowSpin, float dt) {
        refreshGroup();
        if (entity.body.frozen || worldPaused) { moveDirectly(worldPaused); return; }
        if (entity.body.invMass <= 0) return;
        entity.body.toWorld(localAnchor, anchor); offset.set(anchor).sub(entity.body.position);
        float spin = entity.body.angularVelocity;
        pointVelocity.set(entity.body.linearVelocity.x - spin * offset.y, entity.body.linearVelocity.y + spin * offset.x);
        float effectiveMass = 1 / (entity.body.invMass + offset.lengthSquared() * entity.body.invInertia);
        error.set(targetPosition).sub(anchor);
        acceleration.set(error).scale(NATURAL_FREQUENCY * NATURAL_FREQUENCY).scaleAdd(pointVelocity, -2 * DAMPING_RATIO * NATURAL_FREQUENCY);
        force.set(acceleration.limit(MAX_SPRING_ACCELERATION)).scale(effectiveMass);
        if (group.size() > 1) {
            support = Math.min(support + dt / SUPPORT_RAMP_TIME, 1); groupVelocity.zero();
            for (int i = 0; i < group.size(); i++) { Entity part = (Entity) group.elementAt(i); groupVelocity.scaleAdd(part.body.linearVelocity, part.body.mass); }
            groupVelocity.scale(1 / groupMass);
            acceleration.set(error).scale(GROUP_FREQUENCY * GROUP_FREQUENCY).scaleAdd(groupVelocity, -2 * GROUP_DAMPING_RATIO * GROUP_FREQUENCY);
            force.scaleAdd(acceleration.limit(MAX_GROUP_ACCELERATION), groupMass);
            float ownMass = 1 / entity.body.invMass;
            force.scaleAdd(entity.world.physics.gravity, -(ownMass + (groupMass - ownMass) * support));
        } else force.scaleAdd(entity.world.physics.gravity, -1 / entity.body.invMass);
        entity.body.applyForce(force, offset);
        if (!allowSpin && entity.body.invInertia > 0) entity.body.applyTorque(-spin * SPIN_DAMPING / entity.body.invInertia);
        for (int i = 0; i < group.size(); i++) ((Entity) group.elementAt(i)).body.sleeping = false;
    }
    private void moveDirectly(boolean worldPaused) {
        shift.set(targetPosition).sub(anchorWorld());
        for (int i = 0; i < group.size(); i++) {
            Entity part = (Entity) group.elementAt(i); part.body.position.add(shift); part.body.updateTransform();
            if (worldPaused && !part.body.frozen) { part.body.linearVelocity.zero(); part.body.angularVelocity = 0; part.resetImpactHistory(); }
        }
    }
    private void refreshGroup() {
        if (entity.assembly == null) {
            if (group.size() != 1 || group.elementAt(0) != entity) { group = new Vector(); group.addElement(entity); groupMass = entity.body.mass; }
            return;
        }
        if (entity.assembly.connectivityVersion == groupVersion) return;
        groupVersion = entity.assembly.connectivityVersion; group = entity.assembly.connectedParts(entity); groupMass = 0;
        for (int i = 0; i < group.size(); i++) groupMass += ((Entity) group.elementAt(i)).body.mass;
    }
}
