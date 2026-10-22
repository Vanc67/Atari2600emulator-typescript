package physicsplayground.physics;

import java.util.Vector;
import physicsplayground.core.Vec2;

/** Allocation-stable rigid-body step, joints, contact impulses and ray queries in screen pixels. */
public final class PhysicsWorld {
    public interface ImpactListener {
        void impact(Body body, Body other, Vec2 point, Vec2 normal, float normalChange, float impulsePixels);
    }

    public static final class RayHit {
        public Body body;
        public final Vec2 point = new Vec2();
        public final Vec2 normal = new Vec2();
        public float fraction;
        public void clear() { body = null; point.zero(); normal.zero(); fraction = 1; }
    }

    private static final class Contact {
        Body a, b;
        final Vec2 normal = new Vec2();
        final Vec2[] points = { new Vec2(), new Vec2() };
        final float[] penetration = new float[2];
        final float[] normalImpulse = new float[2];
        final float[] tangentImpulse = new float[2];
        final float[] velocityBias = new float[2];
        int count;
        float friction;
        float bounce;
    }

    public final Vector bodies = new Vector();
    public final Vector joints = new Vector();
    public final Vec2 gravity = new Vec2(0, 980);
    public ImpactListener impactListener;
    private Contact[] contacts = new Contact[256];
    private int contactCount;
    private final Contact queryContact = new Contact();
    private final Vec2 jointImpulse = new Vec2();
    private final Vec2 candidateNormal = new Vec2();
    private final Vec2 scratch2 = new Vec2();
    private final Vec2 impulse = new Vec2();
    private final float[] pointX = new float[16];
    private final float[] pointY = new float[16];
    private int pointCount;

    public void addBody(Body body) { if (!bodies.contains(body)) bodies.addElement(body); }
    public void removeBody(Body body) { bodies.removeElement(body); body.removed = true; }
    public void addJoint(PinJoint joint) { if (!joints.contains(joint)) joints.addElement(joint); }
    public void removeJoint(PinJoint joint) { joints.removeElement(joint); }

    public void clear() {
        bodies.removeAllElements(); joints.removeAllElements(); contactCount = 0;
    }

    public void step(float dt) {
        contactCount = 0;
        int bodyCount = bodies.size();
        for (int i = 0; i < bodyCount; i++) {
            Body body = (Body) bodies.elementAt(i);
            if (body.removed) continue;
            body.previousLinear.set(body.linearVelocity);
            body.previousAngular = body.angularVelocity;
            body.priorLinearChange.set(body.lastLinearChange);
            body.priorAngularChange = body.lastAngularChange;
            if (body.invMass == 0 || body.sleeping) { body.force.zero(); body.torque = 0; continue; }
            body.linearVelocity.scaleAdd(gravity, dt);
            body.linearVelocity.scaleAdd(body.force, body.invMass * dt);
            body.angularVelocity += body.torque * body.invInertia * dt;
            float linearFactor = Math.max(0, 1 - dt * body.linearDamp);
            float angularFactor = Math.max(0, 1 - dt * body.angularDamp);
            body.linearVelocity.scale(linearFactor);
            body.angularVelocity *= angularFactor;
            body.integrate(dt);
            body.force.zero(); body.torque = 0;
        }

        for (int i = 0; i < bodyCount; i++) {
            Body a = (Body) bodies.elementAt(i);
            if (a.removed) continue;
            for (int j = i + 1; j < bodyCount; j++) {
                Body b = (Body) bodies.elementAt(j);
                if (b.removed || (a.invMass == 0 && b.invMass == 0)) continue;
                if ((a.layer & b.mask) == 0 && (b.layer & a.mask) == 0) continue;
                if (a.collisionGroup != null && a.collisionGroup == b.collisionGroup) continue;
                if ((a.sleeping || a.invMass == 0) && (b.sleeping || b.invMass == 0)) continue;
                if (a.maxX < b.minX || a.minX > b.maxX || a.maxY < b.minY || a.minY > b.maxY) continue;
                Contact contact = obtainContact();
                if (contact == null) continue;
                if (!collideBodies(a, b, contact)) { contactCount--; continue; }
            }
        }

        for (int iteration = 0; iteration < 8; iteration++) {
            for (int i = 0; i < contactCount; i++) solveContactVelocity(contacts[i], iteration == 0);
            for (int i = 0; i < joints.size(); i++) solveJointVelocity((PinJoint) joints.elementAt(i), dt);
        }
        for (int i = 0; i < contactCount; i++) solveContactPosition(contacts[i]);
        for (int iteration = 0; iteration < 3; iteration++) {
            for (int i = 0; i < joints.size(); i++) solveJointPosition((PinJoint) joints.elementAt(i));
        }
        for (int i = 0; i < bodyCount; i++) {
            Body body = (Body) bodies.elementAt(i);
            body.updateTransform();
        }

        for (int i = 0; i < bodyCount; i++) {
            Body body = (Body) bodies.elementAt(i);
            if (body.invMass == 0) {
                body.lastLinearChange.zero(); body.lastAngularChange = 0;
                continue;
            }
            body.lastLinearChange.set(body.linearVelocity).sub(body.previousLinear).scaleAdd(gravity, -dt);
            body.lastAngularChange = body.angularVelocity - body.previousAngular;
            if (body.sleeping) continue;
            if (body.linearVelocity.lengthSquared() < 64 && Math.abs(body.angularVelocity) < 0.08f && body.force.lengthSquared() == 0) {
                body.sleepTime += dt;
                if (body.sleepTime >= 0.5f) {
                    body.sleeping = true; body.linearVelocity.zero(); body.angularVelocity = 0;
                }
            } else body.sleepTime = 0;
        }
        if (impactListener != null) emitImpacts();
    }

    private Contact obtainContact() {
        if (contactCount == contacts.length) {
            Contact[] expanded = new Contact[contacts.length * 2];
            System.arraycopy(contacts, 0, expanded, 0, contacts.length);
            contacts = expanded;
        }
        Contact contact = contacts[contactCount];
        if (contact == null) { contact = new Contact(); contacts[contactCount] = contact; }
        contactCount++;
        contact.count = 0;
        contact.normalImpulse[0] = contact.normalImpulse[1] = 0;
        contact.tangentImpulse[0] = contact.tangentImpulse[1] = 0;
        return contact;
    }

    private boolean collideBodies(Body a, Body b, Contact contact) {
        contact.a = a; contact.b = b;
        contact.friction = (float) Math.sqrt(Math.max(0, a.friction * b.friction));
        contact.bounce = Math.max(a.bounce, b.bounce);
        if (a.shape == 1 && b.shape == 1) return collideCircles(a, b, contact);
        if (a.shape == 1) return collideCirclePolygon(a, b, contact, true);
        if (b.shape == 1) return collideCirclePolygon(b, a, contact, false);
        return collidePolygons(a, b, contact);
    }

    private boolean collideCircles(Body a, Body b, Contact contact) {
        float dx = b.position.x - a.position.x, dy = b.position.y - a.position.y;
        float radius = a.radius + b.radius;
        float distanceSquared = dx * dx + dy * dy;
        if (distanceSquared >= radius * radius) return false;
        float distance = (float) Math.sqrt(distanceSquared);
        if (distance > 0.00001f) contact.normal.set(dx / distance, dy / distance);
        else contact.normal.set(1, 0);
        contact.penetration[0] = radius - distance;
        contact.points[0].set(a.position.x + contact.normal.x * (a.radius - contact.penetration[0] * 0.5f),
                a.position.y + contact.normal.y * (a.radius - contact.penetration[0] * 0.5f));
        contact.count = 1;
        return true;
    }

    private boolean collideCirclePolygon(Body circle, Body polygon, Contact contact, boolean circleIsA) {
        float cx = circle.position.x, cy = circle.position.y;
        float bestDistance = Float.POSITIVE_INFINITY;
        float bestX = 0, bestY = 0;
        int count = polygon.vertexCount();
        for (int i = 0; i < count; i++) {
            int next = i + 1 == count ? 0 : i + 1;
            float ax = polygon.vertexX(i), ay = polygon.vertexY(i);
            float bx = polygon.vertexX(next), by = polygon.vertexY(next);
            float dx = bx - ax, dy = by - ay;
            float length = dx * dx + dy * dy;
            float t = length > 0 ? ((cx - ax) * dx + (cy - ay) * dy) / length : 0;
            t = clamp(t, 0, 1);
            float x = ax + dx * t, y = ay + dy * t;
            float distance = (cx - x) * (cx - x) + (cy - y) * (cy - y);
            if (distance < bestDistance) { bestDistance = distance; bestX = x; bestY = y; }
        }
        boolean inside = pointInPolygon(cx, cy, polygon);
        float distance = (float) Math.sqrt(bestDistance);
        if (!inside && distance >= circle.radius) return false;
        float nx, ny, penetration;
        if (distance > 0.00001f) {
            nx = (bestX - cx) / distance; ny = (bestY - cy) / distance;
        } else { nx = 1; ny = 0; }
        if (inside) { nx = -nx; ny = -ny; penetration = circle.radius + distance; }
        else penetration = circle.radius - distance;
        if (circleIsA) contact.normal.set(nx, ny);
        else contact.normal.set(-nx, -ny);
        contact.points[0].set(bestX, bestY);
        contact.penetration[0] = penetration;
        contact.count = 1;
        return true;
    }

    private boolean collidePolygons(Body a, Body b, Contact contact) {
        float bestOverlap = Float.POSITIVE_INFINITY;
        float bestX = 0, bestY = 0;
        for (int which = 0; which < 2; which++) {
            Body shape = which == 0 ? a : b;
            int count = shape.vertexCount();
            for (int i = 0; i < count; i++) {
                int next = i + 1 == count ? 0 : i + 1;
                float dx = shape.vertexX(next) - shape.vertexX(i);
                float dy = shape.vertexY(next) - shape.vertexY(i);
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length == 0) continue;
                float nx = dy / length, ny = -dx / length;
                float direction = (b.center.x - a.center.x) * nx + (b.center.y - a.center.y) * ny;
                if (direction < 0) { nx = -nx; ny = -ny; }
                float minA = Float.POSITIVE_INFINITY, maxA = Float.NEGATIVE_INFINITY;
                float minB = Float.POSITIVE_INFINITY, maxB = Float.NEGATIVE_INFINITY;
                for (int v = 0; v < a.vertexCount(); v++) {
                    float projection = a.vertexX(v) * nx + a.vertexY(v) * ny;
                    minA = Math.min(minA, projection); maxA = Math.max(maxA, projection);
                }
                for (int v = 0; v < b.vertexCount(); v++) {
                    float projection = b.vertexX(v) * nx + b.vertexY(v) * ny;
                    minB = Math.min(minB, projection); maxB = Math.max(maxB, projection);
                }
                float overlap = Math.min(maxA, maxB) - Math.max(minA, minB);
                if (overlap <= 0) return false;
                if (overlap < bestOverlap) { bestOverlap = overlap; bestX = nx; bestY = ny; }
            }
        }
        contact.normal.set(bestX, bestY);
        pointCount = 0;
        for (int i = 0; i < b.vertexCount(); i++) {
            float x = b.vertexX(i), y = b.vertexY(i);
            if (pointInPolygon(x, y, a)) addCollisionPoint(x, y);
        }
        for (int i = 0; i < a.vertexCount(); i++) {
            float x = a.vertexX(i), y = a.vertexY(i);
            if (pointInPolygon(x, y, b)) addCollisionPoint(x, y);
        }
        if (pointCount == 0) {
            float ax = a.vertexX(0), ay = a.vertexY(0), bx = b.vertexX(0), by = b.vertexY(0);
            float bestA = -Float.MAX_VALUE, bestB = Float.MAX_VALUE;
            for (int i = 0; i < a.vertexCount(); i++) {
                float projection = a.vertexX(i) * bestX + a.vertexY(i) * bestY;
                if (projection > bestA) { bestA = projection; ax = a.vertexX(i); ay = a.vertexY(i); }
            }
            for (int i = 0; i < b.vertexCount(); i++) {
                float projection = b.vertexX(i) * bestX + b.vertexY(i) * bestY;
                if (projection < bestB) { bestB = projection; bx = b.vertexX(i); by = b.vertexY(i); }
            }
            addCollisionPoint((ax + bx) * 0.5f, (ay + by) * 0.5f);
        }
        int first = 0, second = -1;
        if (pointCount > 1) {
            float farthest = -1;
            for (int i = 0; i < pointCount; i++) for (int j = i + 1; j < pointCount; j++) {
                float dx = pointX[i] - pointX[j], dy = pointY[i] - pointY[j];
                float distance = dx * dx + dy * dy;
                if (distance > farthest) { farthest = distance; first = i; second = j; }
            }
        }
        contact.points[0].set(pointX[first], pointY[first]); contact.penetration[0] = bestOverlap;
        contact.count = 1;
        if (second >= 0) {
            contact.points[1].set(pointX[second], pointY[second]); contact.penetration[1] = bestOverlap;
            contact.count = 2;
        }
        return true;
    }

    private void addCollisionPoint(float x, float y) {
        if (pointCount == pointX.length) return;
        pointX[pointCount] = x; pointY[pointCount] = y; pointCount++;
    }

    private static boolean pointInPolygon(float x, float y, Body body) {
        int winding = 0;
        for (int i = 0; i < body.vertexCount(); i++) {
            int next = i + 1 == body.vertexCount() ? 0 : i + 1;
            float cross = (body.vertexX(next) - body.vertexX(i)) * (y - body.vertexY(i))
                    - (body.vertexY(next) - body.vertexY(i)) * (x - body.vertexX(i));
            if (cross > 0.0001f) { if (winding < 0) return false; winding = 1; }
            else if (cross < -0.0001f) { if (winding > 0) return false; winding = -1; }
        }
        return true;
    }

    private void solveContactVelocity(Contact contact, boolean initialize) {
        Body a = contact.a, b = contact.b;
        if (initialize) {
            if (a.sleeping && b.invMass > 0 && (b.linearVelocity.lengthSquared() > 400 || Math.abs(b.angularVelocity) > 0.5f)) { a.sleeping = false; a.sleepTime = 0; }
            if (b.sleeping && a.invMass > 0 && (a.linearVelocity.lengthSquared() > 400 || Math.abs(a.angularVelocity) > 0.5f)) { b.sleeping = false; b.sleepTime = 0; }
        }
        for (int i = 0; i < contact.count; i++) {
            Vec2 point = contact.points[i], normal = contact.normal;
            float rax = point.x - a.center.x, ray = point.y - a.center.y;
            float rbx = point.x - b.center.x, rby = point.y - b.center.y;
            float rvx = b.linearVelocity.x - b.angularVelocity * rby - a.linearVelocity.x + a.angularVelocity * ray;
            float rvy = b.linearVelocity.y + b.angularVelocity * rbx - a.linearVelocity.y - a.angularVelocity * rax;
            float normalVelocity = rvx * normal.x + rvy * normal.y;
            if (initialize) contact.velocityBias[i] = normalVelocity < -20 ? -contact.bounce * normalVelocity : 0;
            float crossA = rax * normal.y - ray * normal.x;
            float crossB = rbx * normal.y - rby * normal.x;
            float mass = a.invMass + b.invMass + crossA * crossA * a.invInertia + crossB * crossB * b.invInertia;
            if (mass <= 0) continue;
            float lambda = -(normalVelocity - contact.velocityBias[i]) / mass;
            float old = contact.normalImpulse[i];
            contact.normalImpulse[i] = Math.max(0, old + lambda);
            lambda = contact.normalImpulse[i] - old;
            impulse.set(normal.x * lambda, normal.y * lambda);
            applyPairImpulse(a, b, impulse, rax, ray, rbx, rby);

            float tx = -normal.y, ty = normal.x;
            rvx = b.linearVelocity.x - b.angularVelocity * rby - a.linearVelocity.x + a.angularVelocity * ray;
            rvy = b.linearVelocity.y + b.angularVelocity * rbx - a.linearVelocity.y - a.angularVelocity * rax;
            float tangentVelocity = rvx * tx + rvy * ty;
            crossA = rax * ty - ray * tx; crossB = rbx * ty - rby * tx;
            mass = a.invMass + b.invMass + crossA * crossA * a.invInertia + crossB * crossB * b.invInertia;
            lambda = -tangentVelocity / mass;
            float limit = contact.friction * contact.normalImpulse[i];
            old = contact.tangentImpulse[i];
            contact.tangentImpulse[i] = clamp(old + lambda, -limit, limit);
            lambda = contact.tangentImpulse[i] - old;
            impulse.set(tx * lambda, ty * lambda);
            applyPairImpulse(a, b, impulse, rax, ray, rbx, rby);
        }
    }

    private static void applyPairImpulse(Body a, Body b, Vec2 impulse, float rax, float ray, float rbx, float rby) {
        if (a.invMass > 0) {
            a.linearVelocity.x -= impulse.x * a.invMass; a.linearVelocity.y -= impulse.y * a.invMass;
            a.angularVelocity -= (rax * impulse.y - ray * impulse.x) * a.invInertia;
        }
        if (b.invMass > 0) {
            b.linearVelocity.x += impulse.x * b.invMass; b.linearVelocity.y += impulse.y * b.invMass;
            b.angularVelocity += (rbx * impulse.y - rby * impulse.x) * b.invInertia;
        }
    }

    private static void solveContactPosition(Contact contact) {
        Body a = contact.a, b = contact.b;
        float total = a.invMass + b.invMass;
        if (total <= 0) return;
        float penetration = contact.penetration[0];
        if (contact.count > 1) penetration = Math.max(penetration, contact.penetration[1]);
        float correction = Math.max(0, penetration - 0.02f) * 0.8f / total;
        if (a.invMass > 0) {
            a.position.x -= contact.normal.x * correction * a.invMass;
            a.position.y -= contact.normal.y * correction * a.invMass;
            a.center.x -= contact.normal.x * correction * a.invMass;
            a.center.y -= contact.normal.y * correction * a.invMass;
        }
        if (b.invMass > 0) {
            b.position.x += contact.normal.x * correction * b.invMass;
            b.position.y += contact.normal.y * correction * b.invMass;
            b.center.x += contact.normal.x * correction * b.invMass;
            b.center.y += contact.normal.y * correction * b.invMass;
        }
    }

    private void solveJointVelocity(PinJoint joint, float dt) {
        if (joint.broken) return;
        Body a = joint.a, b = joint.b;
        float ax = a.position.x + joint.localA.x * a.cosine - joint.localA.y * a.sine;
        float ay = a.position.y + joint.localA.x * a.sine + joint.localA.y * a.cosine;
        float bx = b.position.x + joint.localB.x * b.cosine - joint.localB.y * b.sine;
        float by = b.position.y + joint.localB.x * b.sine + joint.localB.y * b.cosine;
        float rax = ax - a.center.x, ray = ay - a.center.y;
        float rbx = bx - b.center.x, rby = by - b.center.y;
        float vx = b.linearVelocity.x - b.angularVelocity * rby - a.linearVelocity.x + a.angularVelocity * ray;
        float vy = b.linearVelocity.y + b.angularVelocity * rbx - a.linearVelocity.y - a.angularVelocity * rax;
        float bias = 0.5f / dt;
        vx += (bx - ax) * bias; vy += (by - ay) * bias;
        float inertiaA = a.invInertia, inertiaB = b.invInertia;
        float k11 = a.invMass + b.invMass + inertiaA * ray * ray + inertiaB * rby * rby;
        float k22 = a.invMass + b.invMass + inertiaA * rax * rax + inertiaB * rbx * rbx;
        float k12 = -inertiaA * rax * ray - inertiaB * rbx * rby;
        float determinant = k11 * k22 - k12 * k12;
        if (Math.abs(determinant) < 0.00000001f) return;
        float ix = -(k22 * vx - k12 * vy) / determinant;
        float iy = -(k11 * vy - k12 * vx) / determinant;
        jointImpulse.set(ix, iy);
        applyPairImpulse(a, b, jointImpulse, rax, ray, rbx, rby);
    }

    private static void solveJointPosition(PinJoint joint) {
        if (joint.broken) return;
        Body a = joint.a, b = joint.b;
        float ax = a.position.x + joint.localA.x * a.cosine - joint.localA.y * a.sine;
        float ay = a.position.y + joint.localA.x * a.sine + joint.localA.y * a.cosine;
        float bx = b.position.x + joint.localB.x * b.cosine - joint.localB.y * b.sine;
        float by = b.position.y + joint.localB.x * b.sine + joint.localB.y * b.cosine;
        float total = a.invMass + b.invMass;
        if (total <= 0) return;
        float cx = (bx - ax) * 0.2f / total, cy = (by - ay) * 0.2f / total;
        if (a.invMass > 0) {
            a.position.x += cx * a.invMass; a.position.y += cy * a.invMass;
            a.center.x += cx * a.invMass; a.center.y += cy * a.invMass;
        }
        if (b.invMass > 0) {
            b.position.x -= cx * b.invMass; b.position.y -= cy * b.invMass;
            b.center.x -= cx * b.invMass; b.center.y -= cy * b.invMass;
        }
    }

    private void emitImpacts() {
        for (int i = 0; i < contactCount; i++) {
            Contact contact = contacts[i];
            for (int p = 0; p < contact.count; p++) {
                float impulse = contact.normalImpulse[p];
                if (impulse <= 0) continue;
                emitImpact(contact.a, contact.b, contact.points[p], contact.normal, -1, impulse);
                emitImpact(contact.b, contact.a, contact.points[p], contact.normal, 1, impulse);
            }
        }
    }

    private void emitImpact(Body body, Body other, Vec2 point, Vec2 normal, float sign, float impulse) {
        if (body.invMass == 0) return;
        float nx = normal.x * sign, ny = normal.y * sign;
        float rx = point.x - body.center.x, ry = point.y - body.center.y;
        float nowX = body.lastLinearChange.x - body.lastAngularChange * ry;
        float nowY = body.lastLinearChange.y + body.lastAngularChange * rx;
        float beforeX = body.priorLinearChange.x - body.priorAngularChange * ry;
        float beforeY = body.priorLinearChange.y + body.priorAngularChange * rx;
        float now = nowX * nx + nowY * ny;
        float before = beforeX * nx + beforeY * ny;
        float change = Math.max(now, before);
        if (change > 0.01f) {
            scratch2.set(nx, ny);
            impactListener.impact(body, other, point, scratch2, change, impulse);
        }
    }

    public boolean intersectRay(Vec2 from, Vec2 to, int mask, Body excluded, Body excluded2, RayHit hit, boolean hitFromInside) {
        hit.clear();
        float dx = to.x - from.x, dy = to.y - from.y;
        for (int i = 0; i < bodies.size(); i++) {
            Body body = (Body) bodies.elementAt(i);
            if (body == excluded || body == excluded2 || (body.layer & mask) == 0) continue;
            float fraction = body.shape == 1 ? rayCircle(from, dx, dy, body, hitFromInside, candidateNormal)
                    : rayPolygon(from, dx, dy, body, hitFromInside, candidateNormal);
            if (fraction >= 0 && fraction <= hit.fraction) {
                hit.fraction = fraction; hit.body = body;
                hit.point.set(from.x + dx * fraction, from.y + dy * fraction);
                hit.normal.set(candidateNormal);
            }
        }
        return hit.body != null;
    }

    private static float rayCircle(Vec2 from, float dx, float dy, Body body, boolean hitFromInside, Vec2 normal) {
        float ox = from.x - body.position.x, oy = from.y - body.position.y;
        float c = ox * ox + oy * oy - body.radius * body.radius;
        if (c < 0) {
            if (!hitFromInside) return -1;
            normal.zero(); return 0;
        }
        double a = dx * dx + dy * dy;
        if (a == 0) return -1;
        double b = 2 * (ox * dx + oy * dy);
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) return -1;
        double t = (-b - Math.sqrt(discriminant)) / (2 * a);
        if (t < 0 || t > 1) return -1;
        float x = (float) (from.x + dx * t) - body.position.x;
        float y = (float) (from.y + dy * t) - body.position.y;
        float length = (float) Math.sqrt(x * x + y * y);
        if (length > 0) normal.set(x / length, y / length); else normal.zero();
        return (float) t;
    }

    private static float rayPolygon(Vec2 from, float dx, float dy, Body body, boolean hitFromInside, Vec2 normal) {
        if (pointInPolygon(from.x, from.y, body)) {
            if (!hitFromInside) return -1;
            normal.zero(); return 0;
        }
        float best = Float.MAX_VALUE, bestNx = 0, bestNy = 0;
        for (int i = 0; i < body.vertexCount(); i++) {
            int next = i + 1 == body.vertexCount() ? 0 : i + 1;
            float ax = body.vertexX(i), ay = body.vertexY(i);
            float ex = body.vertexX(next) - ax, ey = body.vertexY(next) - ay;
            float denominator = dx * ey - dy * ex;
            if (Math.abs(denominator) < 0.000001f) continue;
            float ox = ax - from.x, oy = ay - from.y;
            float t = (ox * ey - oy * ex) / denominator;
            float u = (ox * dy - oy * dx) / denominator;
            if (t < 0 || t > 1 || u < 0 || u > 1 || t >= best) continue;
            float length = (float) Math.sqrt(ex * ex + ey * ey);
            float nx = ey / length, ny = -ex / length;
            if (nx * dx + ny * dy > 0) { nx = -nx; ny = -ny; }
            best = t; bestNx = nx; bestNy = ny;
        }
        if (best == Float.MAX_VALUE) return -1;
        normal.set(bestNx, bestNy); return best;
    }

    public boolean overlaps(Body query, int mask, Body excluded) {
        for (int i = 0; i < bodies.size(); i++) {
            Body body = (Body) bodies.elementAt(i);
            if (body == query || body == excluded || (body.layer & mask) == 0) continue;
            if (query.collisionGroup != null && query.collisionGroup == body.collisionGroup) continue;
            if (query.maxX < body.minX || query.minX > body.maxX || query.maxY < body.minY || query.minY > body.maxY) continue;
            if (collideBodies(query, body, queryContact)) return true;
        }
        return false;
    }

    public void queryCircle(Vec2 center, float radius, int mask, Body excluded, Vector output) {
        output.removeAllElements();
        for (int i = 0; i < bodies.size(); i++) {
            Body body = (Body) bodies.elementAt(i);
            if (body == excluded || (body.layer & mask) == 0) continue;
            float dx = body.position.x - center.x, dy = body.position.y - center.y;
            float total = radius + body.boundingRadius;
            if (dx * dx + dy * dy > total * total) continue;
            if (body.shape == 1) {
                float sum = radius + body.radius;
                if (dx * dx + dy * dy <= sum * sum) output.addElement(body);
            } else if (circleTouchesPolygon(center, radius, body)) output.addElement(body);
        }
    }

    private static boolean circleTouchesPolygon(Vec2 center, float radius, Body body) {
        if (pointInPolygon(center.x, center.y, body)) return true;
        float best = Float.POSITIVE_INFINITY;
        for (int i = 0; i < body.vertexCount(); i++) {
            int next = i + 1 == body.vertexCount() ? 0 : i + 1;
            float ax = body.vertexX(i), ay = body.vertexY(i);
            float dx = body.vertexX(next) - ax, dy = body.vertexY(next) - ay;
            float length = dx * dx + dy * dy;
            float t = length > 0 ? ((center.x - ax) * dx + (center.y - ay) * dy) / length : 0;
            t = clamp(t, 0, 1);
            float x = ax + dx * t - center.x, y = ay + dy * t - center.y;
            best = Math.min(best, x * x + y * y);
        }
        return best <= radius * radius;
    }


    public Body pick(Vec2 point, int mask) { return pick(point, mask, null); }

    public Body pick(Vec2 point, int mask, Body excluded) {
        for (int i = bodies.size() - 1; i >= 0; i--) {
            Body body = (Body) bodies.elementAt(i);
            if (body == excluded || (body.layer & mask) == 0) continue;
            if (body.shape == 1) {
                float dx = point.x - body.position.x, dy = point.y - body.position.y;
                if (dx * dx + dy * dy <= body.radius * body.radius) return body;
            } else if (pointInPolygon(point.x, point.y, body)) return body;
        }
        return null;
    }

    private static float clamp(float value, float minimum, float maximum) { return value < minimum ? minimum : (value > maximum ? maximum : value); }
}
