package physicsplayground.physics;

import physicsplayground.core.Vec2;

/** Two-body pivot solved together with contacts; metadata and tearing remain in EntityAssembly. */
public final class PinJoint {
    public final Body a;
    public final Body b;
    public final Vec2 localA;
    public final Vec2 localB;
    public Object owner;
    public boolean broken;

    public PinJoint(Body a, Body b, Vec2 localA, Vec2 localB) {
        this.a = a; this.b = b;
        this.localA = localA.copy(); this.localB = localB.copy();
    }
}
