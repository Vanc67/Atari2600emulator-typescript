package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Math2;
import physicsplayground.core.Vec2;
import physicsplayground.physics.Body;
import physicsplayground.physics.PhysicsWorld;

public final class ExplosionSystem {
    public static final int MAX_STARTS_PER_TICK = 3, MAX_QUEUE = 256, VICTIM_BUDGET_PER_TICK = 256, MAX_VICTIMS_PER_EXPLOSION = 2048;
    public static final float MAX_SPEED_CHANGE = 4000, HEAVY_OBSTACLE_MASS = 20;
    public static final float WALL_BLOCKING = .15f, HEAVY_BLOCKING = .4f, LIGHT_BLOCKING = .8f, TEAR_FACTOR = .7f;
    private static final class Request {
        final Vec2 center = new Vec2(); ExplosionParams params; Entity source;
    }
    private static final class Job {
        final Vec2 center = new Vec2(); ExplosionParams params; Entity source;
        final Vector victims = new Vector(); int nextVictim;
    }
    private final SandboxWorld world;
    private final Vector queue = new Vector(), jobs = new Vector(), query = new Vector();
    private final PhysicsWorld.RayHit hit = new PhysicsWorld.RayHit();
    private final Vec2 offset = new Vec2(), direction = new Vec2(), impulse = new Vec2(), lever = new Vec2();
    public int explosionCount, accepted, rejected, peakQueue, victimsProcessed, victimsTruncated, deferredDetonations;
    public ExplosionSystem(SandboxWorld world) { this.world = world; }
    public boolean request(Vec2 at, ExplosionParams params, Entity source) {
        if (queue.size() >= MAX_QUEUE) { rejected++; return false; }
        Request request = new Request(); request.center.set(at); request.params = params; request.source = source;
        queue.addElement(request); accepted++; peakQueue = Math.max(peakQueue, queue.size()); return true;
    }
    public void noteDeferredDetonation() { deferredDetonations++; }
    public int queueSize() { return queue.size(); }
    public int activeJobCount() { return jobs.size(); }
    public boolean isBusy() { return !queue.isEmpty() || !jobs.isEmpty(); }
    public void clear() { queue.removeAllElements(); jobs.removeAllElements(); }
    public void step(float dt) {
        int started = 0;
        while (!queue.isEmpty() && started < MAX_STARTS_PER_TICK) {
            Request request = (Request) queue.elementAt(0); queue.removeElementAt(0);
            start(request.center, request.params, request.source); started++;
        }
        int budget = VICTIM_BUDGET_PER_TICK;
        while (budget > 0 && !jobs.isEmpty()) {
            Job job = (Job) jobs.elementAt(0);
            while (budget > 0 && job.nextVictim < job.victims.size()) {
                Entity victim = (Entity) job.victims.elementAt(job.nextVictim++); budget--;
                if (!victim.removed) { affect(victim, job); victimsProcessed++; }
            }
            if (job.nextVictim >= job.victims.size()) jobs.removeElementAt(0);
        }
    }
    private void start(Vec2 center, ExplosionParams params, Entity source) {
        explosionCount++; world.effects.explosion(center, params.radius); world.audio.play(params.sound, center, 0);
        world.physics.queryCircle(center, params.radius, Body.ENTITIES | Body.DEBRIS, source == null ? null : source.body, query);
        if (query.size() >= MAX_VICTIMS_PER_EXPLOSION) victimsTruncated++;
        Job job = new Job(); job.center.set(center); job.params = params; job.source = source;
        int count = Math.min(query.size(), MAX_VICTIMS_PER_EXPLOSION);
        for (int i = 0; i < count; i++) {
            Body body = (Body) query.elementAt(i); if (!(body.owner instanceof Entity)) continue;
            Entity entity = (Entity) body.owner;
            int insert = job.victims.size();
            while (insert > 0) {
                Entity prior = (Entity) job.victims.elementAt(insert - 1);
                float a = prior.body.position.distanceSquaredTo(center), b = entity.body.position.distanceSquaredTo(center);
                if (a < b || (a == b && prior.uid < entity.uid)) break;
                insert--;
            }
            job.victims.insertElementAt(entity, insert);
        }
        if (!job.victims.isEmpty()) jobs.addElement(job);
    }
    private void affect(Entity entity, Job job) {
        ExplosionParams params = job.params;
        offset.set(entity.body.position).sub(job.center);
        float distance = Math.max(offset.length() - entity.definition.boundingRadius() * .5f, 0);
        if (distance >= params.radius) return;
        float strength = (float) Math2.pow(1 - distance / params.radius, params.falloff) * lineOfSight(entity, job);
        if (strength <= .01f) return;
        if (offset.lengthSquared() > 1) direction.set(offset).normalize(); else direction.set(0, -1);
        float speedChange = 0;
        if (!entity.body.frozen) {
            float amount = Math.min(params.force * 100 * strength, entity.body.mass * MAX_SPEED_CHANGE);
            speedChange = amount / entity.body.mass;
            lever.set(world.random.nextFloat() * 2 - 1, world.random.nextFloat() * 2 - 1).scale(entity.definition.boundingRadius() * .3f);
            impulse.set(direction).scale(amount); entity.body.applyImpulse(impulse, lever);
        }
        if (entity.health != null && params.damage > 0) {
            float damage = params.damage * strength;
            entity.health.applyDamage(damage, DamageInfo.explosion(damage, speedChange / 100 * TEAR_FACTOR, job.center, direction, job.source == null ? 0 : job.source.uid));
        }
        if (entity.thermal != null && params.heat > 0) {
            entity.thermal.addHeat(params.heat * strength);
            if (params.ignite && strength > .25f) entity.thermal.tryIgnite(strength);
        }
    }
    private float lineOfSight(Entity entity, Job job) {
        boolean found = world.physics.intersectRay(job.center, entity.body.position, Body.WORLD | Body.ENTITIES,
                entity.body, job.source == null ? null : job.source.body, hit, false);
        if (!found) return 1;
        if (!(hit.body.owner instanceof Entity)) return WALL_BLOCKING;
        Entity blocker = (Entity) hit.body.owner;
        if (blocker.assembly != null && blocker.assembly == entity.assembly) return 1;
        return blocker.body.mass >= HEAVY_OBSTACLE_MASS ? HEAVY_BLOCKING : LIGHT_BLOCKING;
    }
}
