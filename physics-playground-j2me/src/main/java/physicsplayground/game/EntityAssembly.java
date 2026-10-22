package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Math2;
import physicsplayground.core.Vec2;
import physicsplayground.physics.PinJoint;

/** Ragdoll joints, health-scaled tearing, PID angle limits and connected-group queries. */
public final class EntityAssembly {
    public static final int STRAIN_TICKS_TO_BREAK = 3;
    public static final float INSTANT_BREAK_FACTOR = 2.5f, DRAG_PROTECTION_MULTIPLIER = 2.5f, MIN_JOINT_STRENGTH = .35f;
    public static final float LIMIT_KP = 1600, LIMIT_KD = 80, LIMIT_KI = 30000, LIMIT_MAX_INTEGRAL = 6000, LIMIT_MARGIN = .12f, LIMIT_INTEGRAL_DECAY = .9f;
    public final SandboxWorld world;
    public final Definitions.Entity definition;
    public final long uid;
    public final Vector parts = new Vector(), joints = new Vector();
    public AssemblyController controller;
    public int connectivityVersion;
    private int dragHolds;
    private final Vec2 anchorA = new Vec2(), anchorB = new Vec2();
    public EntityAssembly(SandboxWorld world, Definitions.Entity definition, long uid) { this.world = world; this.definition = definition; this.uid = uid; }
    public void attachParts(Vector parts, Vector brokenNames) {
        for (int i = 0; i < parts.size(); i++) {
            Entity part = (Entity) parts.elementAt(i); this.parts.addElement(part); part.assembly = this; part.body.collisionGroup = this;
            Definitions.Part definitionPart = definition.assembly.part(part.assemblyPartName); if (definitionPart != null) part.zIndex = definitionPart.zIndex;
        }
        for (int i = 0; i < definition.assembly.joints.size(); i++) {
            Definitions.Joint definitionJoint = (Definitions.Joint) definition.assembly.joints.elementAt(i);
            AssemblyJoint joint = new AssemblyJoint(); joint.definition = definitionJoint;
            joint.bodyA = part(definitionJoint.parent); joint.bodyB = part(definitionJoint.child); joints.addElement(joint);
            if (joint.bodyA == null || joint.bodyB == null || brokenNames.contains(definitionJoint.name)) markBroken(joint);
            else { createPin(joint); updateStrength(joint); }
        }
        controller = "human".equals(definition.assembly.controllerType) ? new HumanController() : new AssemblyController();
        controller.setup(this, definition.assembly.controllerParams);
        for (int i = 0; i < parts.size(); i++) controller.applyStateToPart((Entity) parts.elementAt(i));
        controller.evaluate();
    }
    public Entity part(String name) { for (int i = 0; i < parts.size(); i++) { Entity part = (Entity) parts.elementAt(i); if (part.assemblyPartName.equals(name)) return part; } return null; }
    public AssemblyJoint joint(String name) { for (int i = 0; i < joints.size(); i++) { AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (joint.definition.name.equals(name)) return joint; } return null; }
    public Vector brokenJointNames() { Vector names = new Vector(); for (int i = 0; i < joints.size(); i++) { AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (joint.broken) names.addElement(joint.definition.name); } return names; }
    public boolean isPartAttached(Entity part) { for (int i = 0; i < joints.size(); i++) { AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (!joint.broken && joint.connects(part)) return true; } return false; }
    public Vector connectedParts(Entity start) {
        Vector result = new Vector(); result.addElement(start); int index = 0;
        while (index < result.size()) {
            Entity current = (Entity) result.elementAt(index);
            for (int i = 0; i < joints.size(); i++) {
                AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (joint.broken || !joint.connects(current)) continue;
                Entity other = joint.other(current); if (other != null && !result.contains(other)) result.addElement(other);
            }
            index++;
        }
        return result;
    }
    public float totalHealthFraction() {
        float maximum = definition.assembly.totalMaxHealth(); if (maximum <= 0) return 1;
        float current = 0; for (int i = 0; i < parts.size(); i++) { Entity part = (Entity) parts.elementAt(i); if (part.health != null) current += part.health.currentHealth; }
        return clamp(current / maximum, 0, 1);
    }
    public void breakJoint(AssemblyJoint joint) { if (joint.broken) return; markBroken(joint); controller.onJointBroken(joint); }
    public void breakJointByName(String name) { AssemblyJoint joint = joint(name); if (joint != null) breakJoint(joint); }
    public void healAll() { for (int i = 0; i < parts.size(); i++) { Entity part = (Entity) parts.elementAt(i); if (part.health != null) part.health.healFull(); } controller.revive(); }
    public void onPartRemoved(Entity part) {
        for (int i = 0; i < joints.size(); i++) { AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (joint.connects(part) && !joint.broken) markBroken(joint); }
        parts.removeElement(part); controller.onPartRemoved(part.assemblyPartName);
    }
    public void beginDragProtection() { dragHolds++; }
    public void endDragProtection() { dragHolds = Math.max(dragHolds - 1, 0); }
    public void tick(float dt) {
        float protection = dragHolds > 0 ? DRAG_PROTECTION_MULTIPLIER : 1;
        for (int i = 0; i < joints.size(); i++) {
            AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (joint.broken) continue;
            Entity a = joint.bodyA, b = joint.bodyB;
            if ((a.body.sleeping && b.body.sleeping) || a.body.frozen || b.body.frozen) { joint.strainTicks = 0; continue; }
            if (joint.definition.hasAngleLimits) applyAngleLimit(joint, a, b, dt);
            checkStrain(joint, a, b, protection);
        }
    }
    private void applyAngleLimit(AssemblyJoint joint, Entity a, Entity b, float dt) {
        Definitions.Joint limits = joint.definition;
        float relative = Math2.wrapAngle(b.body.rotation - a.body.rotation - limits.restAngle);
        float depth = relative - limits.upperAngle, inward = -1;
        if (depth <= -LIMIT_MARGIN) {
            depth = limits.lowerAngle - relative; inward = 1;
            if (depth <= -LIMIT_MARGIN) { joint.limitIntegral *= LIMIT_INTEGRAL_DECAY; return; }
        }
        float error = depth + LIMIT_MARGIN * .5f;
        joint.limitIntegral = clamp(joint.limitIntegral + LIMIT_KI * error * dt, 0, LIMIT_MAX_INTEGRAL);
        float outwardSpeed = -(b.body.angularVelocity - a.body.angularVelocity) * inward;
        float acceleration = Math.max(LIMIT_KP * Math.max(error, 0) + LIMIT_KD * outwardSpeed + joint.limitIntegral, 0);
        if (acceleration <= 0) return;
        float torque = inward * acceleration * joint.effectiveInertia; b.body.applyTorque(torque); a.body.applyTorque(-torque);
    }
    private void checkStrain(AssemblyJoint joint, Entity a, Entity b, float protection) {
        a.body.toWorld(joint.definition.localAnchorParent, anchorA); b.body.toWorld(joint.definition.localAnchorChild, anchorB);
        float strain = anchorA.distanceTo(anchorB); joint.lastStrain = strain; joint.peakStrain = Math.max(joint.peakStrain, strain);
        float threshold = joint.definition.breakStrain * joint.strength * protection;
        if (strain <= threshold) { joint.strainTicks = 0; return; }
        joint.strainTicks++;
        if (joint.strainTicks >= STRAIN_TICKS_TO_BREAK || strain > threshold * INSTANT_BREAK_FACTOR) breakJoint(joint);
    }
    private void createPin(AssemblyJoint joint) {
        joint.pin = new PinJoint(joint.bodyA.body, joint.bodyB.body, joint.definition.localAnchorParent, joint.definition.localAnchorChild);
        joint.pin.owner = joint; world.physics.addJoint(joint.pin);
        float a = joint.bodyA.definition.computeInertia(joint.bodyA.body.mass), b = joint.bodyB.definition.computeInertia(joint.bodyB.body.mass);
        joint.effectiveInertia = a * b / (a + b);
    }
    private void markBroken(AssemblyJoint joint) {
        boolean connected = !joint.broken && joint.pin != null; joint.broken = true; joint.strainTicks = 0;
        if (joint.pin != null) { world.physics.removeJoint(joint.pin); joint.pin = null; }
        if (joint.bodyA != null) joint.bodyA.addWound(joint.definition.localAnchorParent);
        if (joint.bodyB != null) joint.bodyB.addWound(joint.definition.localAnchorChild);
        if (connected) connectivityVersion++;
    }
    private void updateStrength(AssemblyJoint joint) {
        float fraction = joint.bodyB != null && joint.bodyB.health != null ? joint.bodyB.health.healthFraction() : 1;
        joint.strength = MIN_JOINT_STRENGTH + (1 - MIN_JOINT_STRENGTH) * fraction;
    }
    public void onPartDamaged(Entity part, DamageInfo info) {
        if (info.canTear()) for (int i = 0; i < joints.size(); i++) {
            AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i);
            if (joint.broken || !joint.definition.child.equals(part.assemblyPartName)) continue;
            float speed = joint.definition.breakImpactSpeed;
            if (speed > 0 && info.impactSpeed >= speed * joint.strength) breakJoint(joint);
        }
        controller.onPartDamaged(part.assemblyPartName, info);
    }
    public void onPartHealthChanged(Entity part) {
        for (int i = 0; i < joints.size(); i++) { AssemblyJoint joint = (AssemblyJoint) joints.elementAt(i); if (!joint.broken && joint.definition.child.equals(part.assemblyPartName)) updateStrength(joint); }
        controller.evaluate();
    }
    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
}
