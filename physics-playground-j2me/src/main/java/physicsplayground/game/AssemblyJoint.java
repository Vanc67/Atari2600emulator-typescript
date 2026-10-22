package physicsplayground.game;

import physicsplayground.core.Definitions;
import physicsplayground.physics.PinJoint;

public final class AssemblyJoint {
    public Definitions.Joint definition;
    public Entity bodyA, bodyB;
    public PinJoint pin;
    public boolean broken;
    public float strength = 1, effectiveInertia = 1;
    public int strainTicks;
    public float limitIntegral, lastStrain, peakStrain;
    public boolean connects(Entity entity) { return bodyA == entity || bodyB == entity; }
    public Entity other(Entity entity) { return bodyA == entity ? bodyB : bodyA; }
}
