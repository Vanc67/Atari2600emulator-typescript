package physicsplayground.physics;

import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;

/** Rigid body with Godot-compatible pixel units and procedural rectangle/circle/capsule shapes. */
public final class Body {
    public static final int WORLD = 1;
    public static final int ENTITIES = 2;
    public static final int DEBRIS = 4;

    public Definitions.Entity definition;
    public long id;
    public Object owner;
    public Object collisionGroup;
    public int layer = ENTITIES;
    public int mask = WORLD | ENTITIES;
    public final Vec2 position = new Vec2();
    public final Vec2 center = new Vec2();
    public final Vec2 linearVelocity = new Vec2();
    public final Vec2 force = new Vec2();
    public Vec2 centerOfMass = new Vec2();
    public float rotation;
    public float angularVelocity;
    public float torque;
    public float cosine = 1;
    public float sine;
    public float mass;
    public float inertia;
    public float invMass;
    public float invInertia;
    public float dynamicInvMass;
    public float dynamicInvInertia;
    public float friction = 0.5f;
    public float bounce;
    public float linearDamp = 0.05f;
    public float angularDamp = 0.3f;
    public boolean frozen;
    public boolean sleeping;
    public float sleepTime;
    public boolean removed;
    public boolean dynamic = true;
    public int shape;
    public float radius;
    public final Vec2 size = new Vec2();
    public float minX, minY, maxX, maxY;
    public float boundingRadius;
    public final Vec2 previousLinear = new Vec2();
    public final Vec2 priorLinearChange = new Vec2();
    public final Vec2 lastLinearChange = new Vec2();
    public float priorAngularChange;
    public float previousAngular;
    public float lastAngularChange;
    private float[] localX;
    private float[] localY;
    private float[] worldX;
    private float[] worldY;
    private int vertices;

    public Body(Definitions.Entity definition, Definitions.Material material) {
        this.definition = definition;
        this.shape = definition.isCircle() ? 1 : (definition.isCapsule() ? 2 : 0);
        this.radius = definition.radius;
        this.size.set(definition.size);
        this.boundingRadius = definition.boundingRadius();
        this.mass = definition.computeMass(material);
        this.inertia = definition.computeInertia(mass);
        dynamicInvMass = mass > 0 ? 1.0f / mass : 0;
        dynamicInvInertia = inertia > 0 ? 1.0f / inertia : 0;
        invMass = dynamicInvMass;
        invInertia = dynamicInvInertia;
        if (material != null) { friction = material.friction; bounce = material.bounce; }
        if (definition.linearDamp >= 0) linearDamp = definition.linearDamp;
        if (definition.angularDamp >= 0) angularDamp = definition.angularDamp;
        if (definition.centerOfMass != null) centerOfMass.set(definition.centerOfMass);
        if (shape != 1) buildPolygon();
        updateTransform();
    }

    private Body(float width, float height) {
        dynamic = false;
        mass = 0;
        inertia = 0;
        invMass = invInertia = dynamicInvMass = dynamicInvInertia = 0;
        size.set(width, height);
        radius = 0;
        shape = 0;
        layer = WORLD;
        mask = 0;
        friction = 1;
        bounce = 0;
        boundingRadius = (float) Math.sqrt(width * width + height * height) * 0.5f;
        buildPolygon();
        updateTransform();
    }

    public static Body staticRectangle(float x, float y, float width, float height, float friction) {
        Body body = new Body(width, height);
        body.position.set(x, y);
        body.friction = friction;
        body.updateTransform();
        return body;
    }

    private void buildPolygon() {
        if (shape == 2) {
            int segments = 8;
            vertices = (segments + 1) * 2;
            localX = new float[vertices]; localY = new float[vertices];
            float halfStraight = Math.max(size.y * 0.5f - radius, 0);
            int at = 0;
            for (int step = 0; step <= segments; step++) {
                float angle = (float) (Math.PI + Math.PI * step / segments);
                localX[at] = (float) Math.cos(angle) * radius;
                localY[at] = (float) Math.sin(angle) * radius - halfStraight;
                at++;
            }
            for (int step = 0; step <= segments; step++) {
                float angle = (float) (Math.PI * step / segments);
                localX[at] = (float) Math.cos(angle) * radius;
                localY[at] = (float) Math.sin(angle) * radius + halfStraight;
                at++;
            }
        } else {
            vertices = 4;
            localX = new float[4]; localY = new float[4];
            float halfX = size.x * 0.5f, halfY = size.y * 0.5f;
            localX[0] = -halfX; localY[0] = -halfY;
            localX[1] = halfX; localY[1] = -halfY;
            localX[2] = halfX; localY[2] = halfY;
            localX[3] = -halfX; localY[3] = halfY;
        }
        worldX = new float[vertices]; worldY = new float[vertices];
    }

    public void setTransform(float x, float y, float angle) {
        position.set(x, y);
        rotation = angle;
        sleeping = false;
        updateTransform();
    }

    public void updateTransform() {
        cosine = (float) Math.cos(rotation);
        sine = (float) Math.sin(rotation);
        float comX = centerOfMass.x * cosine - centerOfMass.y * sine;
        float comY = centerOfMass.x * sine + centerOfMass.y * cosine;
        center.set(position.x + comX, position.y + comY);
        if (shape == 1) {
            minX = position.x - radius; maxX = position.x + radius;
            minY = position.y - radius; maxY = position.y + radius;
            return;
        }
        minX = minY = Float.POSITIVE_INFINITY;
        maxX = maxY = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < vertices; i++) {
            float x = position.x + localX[i] * cosine - localY[i] * sine;
            float y = position.y + localX[i] * sine + localY[i] * cosine;
            worldX[i] = x; worldY[i] = y;
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
        }
    }

    public void integrate(float dt) {
        center.scaleAdd(linearVelocity, dt);
        rotation += angularVelocity * dt;
        cosine = (float) Math.cos(rotation);
        sine = (float) Math.sin(rotation);
        float comX = centerOfMass.x * cosine - centerOfMass.y * sine;
        float comY = centerOfMass.x * sine + centerOfMass.y * cosine;
        position.set(center.x - comX, center.y - comY);
        updateTransform();
    }

    public void toWorld(Vec2 local, Vec2 out) {
        out.set(position.x + local.x * cosine - local.y * sine, position.y + local.x * sine + local.y * cosine);
    }

    public void toLocal(Vec2 world, Vec2 out) {
        float dx = world.x - position.x, dy = world.y - position.y;
        out.set(dx * cosine + dy * sine, -dx * sine + dy * cosine);
    }

    public void applyImpulse(Vec2 impulse, Vec2 offsetFromOrigin) {
        if (invMass == 0) return;
        linearVelocity.scaleAdd(impulse, invMass);
        float rx = offsetFromOrigin.x - (center.x - position.x);
        float ry = offsetFromOrigin.y - (center.y - position.y);
        angularVelocity += (rx * impulse.y - ry * impulse.x) * invInertia;
        sleeping = false;
        sleepTime = 0;
    }

    public void applyImpulseAt(Vec2 impulse, Vec2 worldPoint) {
        if (invMass == 0) return;
        linearVelocity.scaleAdd(impulse, invMass);
        float rx = worldPoint.x - center.x, ry = worldPoint.y - center.y;
        angularVelocity += (rx * impulse.y - ry * impulse.x) * invInertia;
        sleeping = false;
        sleepTime = 0;
    }

    public void applyForce(Vec2 value, Vec2 offsetFromOrigin) {
        if (invMass == 0) return;
        force.add(value);
        float rx = offsetFromOrigin.x - (center.x - position.x);
        float ry = offsetFromOrigin.y - (center.y - position.y);
        torque += rx * value.y - ry * value.x;
        sleeping = false;
        sleepTime = 0;
    }

    public void applyTorque(float value) {
        if (invInertia == 0) return;
        torque += value;
        sleeping = false;
        sleepTime = 0;
    }

    public void setFrozen(boolean value) {
        if (frozen == value) return;
        frozen = value;
        if (value) {
            linearVelocity.zero(); angularVelocity = 0;
            invMass = invInertia = 0;
        } else {
            invMass = dynamicInvMass; invInertia = dynamicInvInertia;
            sleeping = false;
        }
    }

    public int vertexCount() { return vertices; }
    public float vertexX(int index) { return worldX[index]; }
    public float vertexY(int index) { return worldY[index]; }
}
