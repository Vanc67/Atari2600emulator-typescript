package physicsplayground.physics;

import physicsplayground.core.Content;
import physicsplayground.core.ContentReader;
import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;

public final class PhysicsTest {
    private static int checks;
    private static int failures;
    private static void check(boolean condition, String name) {
        checks++; if (!condition) { failures++; System.out.println("FAIL " + name); }
    }
    private static void near(float actual, float expected, float tolerance, String name) {
        check(Math.abs(actual - expected) <= tolerance, name + " (actual=" + actual + ", expected=" + expected + ")");
    }
    private static void steps(PhysicsWorld world, int count) {
        for (int i = 0; i < count; i++) world.step(1.0f / 120.0f);
    }
    private static Body body(Content content, String id, float x, float y) {
        Definitions.Entity definition = content.registry.entity(id);
        Body body = new Body(definition, content.registry.material(definition.materialId));
        body.setTransform(x, y, 0);
        return body;
    }
    private static boolean finite(Body body) {
        return !Float.isNaN(body.position.x + body.position.y + body.rotation + body.linearVelocity.x + body.linearVelocity.y + body.angularVelocity)
                && !Float.isInfinite(body.position.x + body.position.y + body.rotation + body.linearVelocity.x + body.linearVelocity.y + body.angularVelocity);
    }

    public static void main(String[] args) throws Exception {
        Content content = Content.loadDefault();
        check(content.errors.isEmpty(), "core loads for physics tests: " + content.errors);
        PhysicsWorld world = new PhysicsWorld();
        Body floor = Body.staticRectangle(400, 600, 800, 40, 1);
        world.addBody(floor);
        Body box = body(content, "core:wooden_box", 400, 500);
        world.addBody(box);
        int restTick = -1;
        for (int tick = 0; tick < 300; tick++) {
            world.step(1.0f / 120.0f);
            if (restTick < 0 && box.sleeping) restTick = tick;
            check(finite(box), "finite box tick " + tick);
            if (failures > 0) break;
        }
        check(restTick >= 0 && restTick < 144, "box falls asleep within 1.2 seconds, tick=" + restTick);
        near(box.position.y, 548, 1.5f, "rectangle rests on floor top");
        near(box.linearVelocity.length(), 0, 0.01f, "sleeping box linear velocity zero");

        PhysicsWorld circleWorld = new PhysicsWorld();
        Body circleFloor = Body.staticRectangle(400, 600, 800, 40, 1);
        Body capsule = body(content, "core:human_pelvis", 400, 510);
        circleWorld.addBody(circleFloor); circleWorld.addBody(capsule);
        steps(circleWorld, 240);
        check(capsule.sleeping, "capsule falls asleep");
        near(capsule.position.y, 562, 2, "capsule rests on floor");
        check(finite(capsule), "capsule remains finite");

        PhysicsWorld rayWorld = new PhysicsWorld();
        Body ball = body(content, "core:metal_sphere", 300, 300);
        ball.setFrozen(true);
        rayWorld.addBody(ball);
        PhysicsWorld.RayHit hit = new PhysicsWorld.RayHit();
        check(rayWorld.intersectRay(new Vec2(0, 300), new Vec2(500, 300), Body.ENTITIES, null, null, hit, false), "ray hits circle");
        near(hit.point.x, 280, 0.01f, "circle ray entry");
        near(hit.normal.x, -1, 0.001f, "circle ray outward normal");
        Body picked = rayWorld.pick(new Vec2(300, 300), Body.ENTITIES);
        check(picked == ball, "point pick finds circle");
        check(rayWorld.pick(new Vec2(100, 100), Body.ENTITIES) == null, "point pick misses empty space");

        PhysicsWorld jointWorld = new PhysicsWorld();
        Body a = body(content, "core:wooden_box", 300, 300);
        Body b = body(content, "core:wooden_box", 380, 300);
        Object group = new Object(); a.collisionGroup = group; b.collisionGroup = group;
        jointWorld.addBody(a); jointWorld.addBody(b);
        PinJoint joint = new PinJoint(a, b, new Vec2(32, 0), new Vec2(-32, 0));
        jointWorld.addJoint(joint);
        steps(jointWorld, 300);
        Vec2 anchorA = new Vec2(), anchorB = new Vec2();
        a.toWorld(joint.localA, anchorA); b.toWorld(joint.localB, anchorB);
        check(anchorA.distanceTo(anchorB) < 3, "pin joint anchors remain coincident, distance=" + anchorA.distanceTo(anchorB));
        check(finite(a) && finite(b), "jointed bodies remain finite");
        check(a.position.distanceTo(b.position) > 50, "joint does not collapse body origins");

        PhysicsWorld stackWorld = new PhysicsWorld();
        stackWorld.addBody(Body.staticRectangle(400, 600, 800, 40, 1));
        Body lower = body(content, "core:wooden_box", 400, 548);
        Body upper = body(content, "core:wooden_box", 400, 500);
        stackWorld.addBody(lower); stackWorld.addBody(upper);
        steps(stackWorld, 360);
        check(finite(lower) && finite(upper), "stack remains finite");
        near(lower.position.y, 548, 2, "lower stacked box supported");
        near(upper.position.y, 484, 3, "upper stacked box supported");
        System.out.println("PhysicsTest: " + checks + " checks, " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
    }
}
