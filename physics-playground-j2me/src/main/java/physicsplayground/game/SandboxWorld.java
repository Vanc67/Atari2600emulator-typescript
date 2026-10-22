package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.core.Random;
import physicsplayground.core.Vec2;
import physicsplayground.physics.Body;
import physicsplayground.physics.PhysicsWorld;

/** Platform-independent counterpart of the recovered SandboxWorld scene and its systems. */
public final class SandboxWorld {
    public static final int BOUNDS_CHECK_INTERVAL_TICKS = 30, MAX_DEBRIS = 60;
    public static final class DebrisRequest {
        String type; final Vec2 center = new Vec2(), baseVelocity = new Vec2(); float rotation, speed, extentX, extentY; int count;
        public DebrisRequest(String type, Vec2 center, float rotation, int count, float speed, Vec2 baseVelocity, float extentX, float extentY) {
            this.type = type; this.center.set(center); this.rotation = rotation; this.count = count; this.speed = speed;
            this.baseVelocity.set(baseVelocity); this.extentX = extentX; this.extentY = extentY;
        }
    }
    public final Content content;
    public final PhysicsWorld physics = new PhysicsWorld();
    public final Vector entities = new Vector(), assemblies = new Vector(), debris = new Vector();
    public final Vector debrisRequests = new Vector();
    private final Vector destroyQueue = new Vector();
    public final Random random;
    public final Effects effects;
    public final ProjectileSystem projectiles;
    public final ExplosionSystem explosions;
    public final ThermalSystem thermal;
    public AudioSink audio = AudioSink.NONE;
    public float simulationTime;
    public boolean paused;
    public float halfWidth = 6000, floorY = 0, wallHeight = 5000, escapeMargin = 3000;
    private long nextUid = 1;
    private int ticksSinceBoundsCheck;
    private final Vec2 scratch = new Vec2(), scratch2 = new Vec2(), spawnPosition = new Vec2();

    public SandboxWorld(Content content) { this(content, true, 20261016); }
    public SandboxWorld(Content content, boolean geometry, long seed) {
        this.content = content; random = new Random(seed); effects = new Effects(random);
        projectiles = new ProjectileSystem(this); explosions = new ExplosionSystem(this); thermal = new ThermalSystem(this);
        physics.impactListener = new PhysicsWorld.ImpactListener() {
            public void impact(Body body, Body other, Vec2 point, Vec2 normal, float normalChange, float impulsePixels) {
                if (body.owner instanceof Entity) {
                    Entity entity = (Entity) body.owner;
                    if (entity.impactDamage != null) entity.impactDamage.contact(other, point, normalChange, impulsePixels);
                }
            }
        };
        if (geometry) buildGeometry();
    }
    public void buildGeometry() {
        float outer = halfWidth + 200;
        Body floor = Body.staticRectangle(0, floorY + 3000, outer * 2, 6000, 1); floor.id = -1; physics.addBody(floor);
        Body left = Body.staticRectangle(-outer + 100, floorY - wallHeight * .5f, 200, wallHeight, 1); left.id = -2; physics.addBody(left);
        Body right = Body.staticRectangle(outer - 100, floorY - wallHeight * .5f, 200, wallHeight, 1); right.id = -3; physics.addBody(right);
    }
    public Object spawn(String type, Vec2 position, float rotation) {
        Definitions.Entity definition = content.registry.entity(type); if (definition == null) return null;
        if (definition.isAssembly()) return spawnAssembly(type, position, rotation, 0);
        return spawnEntity(type, position, rotation, 0);
    }
    public Entity spawnEntity(String type, Vec2 position, float rotation) { return spawnEntity(type, position, rotation, 0); }
    public Entity spawnEntity(String type, Vec2 position, float rotation, long requestedUid) {
        Definitions.Entity definition = content.registry.entity(type);
        if (definition == null || definition.isAssembly() || content.registry.material(definition.materialId) == null) return null;
        long uid = claimUid(requestedUid); Entity entity = new Entity(this, definition, uid);
        entity.body.setTransform(position.x, position.y, rotation); entities.addElement(entity); physics.addBody(entity.body); return entity;
    }
    public EntityAssembly spawnAssembly(String type, Vec2 origin, float rotation, long requestedUid) {
        Definitions.Entity definition = content.registry.entity(type); if (definition == null || !definition.isAssembly()) return null;
        Vector parts = new Vector();
        for (int i = 0; i < definition.assembly.parts.size(); i++) {
            Definitions.Part part = (Definitions.Part) definition.assembly.parts.elementAt(i);
            spawnPosition.set(part.position).rotate(rotation).add(origin);
            Entity entity = spawnEntity(part.entityId, spawnPosition, rotation + part.rotation);
            if (entity == null) { while (!parts.isEmpty()) removeEntity((Entity) parts.elementAt(0)); return null; }
            entity.assemblyPartName = part.name; parts.addElement(entity);
        }
        float nudge = definition.assembly.spawnNudge;
        if (nudge > 0) {
            float sign = random.nextFloat() < .5f ? 1 : -1;
            for (int i = 0; i < definition.assembly.parts.size(); i++) {
                Definitions.Part part = (Definitions.Part) definition.assembly.parts.elementAt(i);
                if (part.position.y < 0) {
                    Entity entity = (Entity) parts.elementAt(i);
                    scratch.set(nudge * sign * (-part.position.y / 100), 0).rotate(rotation); entity.body.linearVelocity.set(scratch);
                }
            }
        }
        return registerAssembly(definition, requestedUid, parts, new Vector(), new Json.Map());
    }
    public EntityAssembly restoreAssembly(String type, long requestedUid, Vector parts, Vector brokenNames, Json.Map controllerState) {
        Definitions.Entity definition = content.registry.entity(type);
        if (definition == null || !definition.isAssembly() || parts.isEmpty()) return null;
        return registerAssembly(definition, requestedUid, parts, brokenNames, controllerState);
    }
    private EntityAssembly registerAssembly(Definitions.Entity definition, long requestedUid, Vector parts, Vector brokenNames, Json.Map controllerState) {
        long uid = claimUid(requestedUid); EntityAssembly assembly = new EntityAssembly(this, definition, uid);
        assembly.attachParts(parts, brokenNames); if (controllerState != null && !controllerState.isEmpty()) assembly.controller.deserializeState(controllerState);
        assemblies.addElement(assembly); return assembly;
    }
    private long claimUid(long requested) {
        long uid = requested;
        if (uid <= 0 || entityByUid(uid) != null || assemblyByUid(uid) != null) uid = nextUid;
        while (entityByUid(uid) != null || assemblyByUid(uid) != null) uid++;
        nextUid = Math.max(nextUid, uid + 1); return uid;
    }
    public void requestDestroy(Entity entity) { if (!destroyQueue.contains(entity)) destroyQueue.addElement(entity); }
    public void removeEntity(Entity entity) {
        if (entity == null || entity.removed || !entities.contains(entity)) return;
        entities.removeElement(entity); entity.removed = true; physics.removeBody(entity.body); debris.removeElement(entity); effects.setBurning(entity, false);
        EntityAssembly assembly = entity.assembly;
        if (assembly != null) {
            assembly.onPartRemoved(entity);
            if (assembly.parts.isEmpty()) assemblies.removeElement(assembly);
        }
    }
    public void removeAssembly(EntityAssembly assembly) { if (assembly == null) return; while (!assembly.parts.isEmpty()) removeEntity((Entity) assembly.parts.elementAt(0)); }
    public void clearEntities() {
        while (!entities.isEmpty()) removeEntity((Entity) entities.elementAt(0));
        debris.removeAllElements(); destroyQueue.removeAllElements(); debrisRequests.removeAllElements();
        projectiles.clear(); explosions.clear(); thermal.clear(); effects.clear();
    }
    public void step(float dt) {
        if (paused) return;
        simulationTime += dt;
        ticksSinceBoundsCheck++;
        if (ticksSinceBoundsCheck >= BOUNDS_CHECK_INTERVAL_TICKS) {
            ticksSinceBoundsCheck = 0;
            for (int i = entities.size() - 1; i >= 0; i--) {
                Entity entity = (Entity) entities.elementAt(i); if (!isInsideEscapeBounds(entity.body.position)) removeEntity(entity);
            }
        }
        for (int i = 0; i < entities.size(); i++) ((Entity) entities.elementAt(i)).tick(dt);
        for (int i = 0; i < assemblies.size(); i++) ((EntityAssembly) assemblies.elementAt(i)).tick(dt);
        projectiles.step(dt); explosions.step(dt); thermal.step(dt);
        for (int i = 0; i < entities.size(); i++) { Entity entity = (Entity) entities.elementAt(i); if (entity.impactDamage != null) entity.impactDamage.beginStep(); }
        physics.step(dt);
        for (int i = 0; i < entities.size(); i++) { Entity entity = (Entity) entities.elementAt(i); if (entity.impactDamage != null) entity.impactDamage.finishStep(); }
        effects.tick(dt); flushDeferred();
    }
    private void flushDeferred() {
        while (!debrisRequests.isEmpty()) {
            DebrisRequest request = (DebrisRequest) debrisRequests.elementAt(0); debrisRequests.removeElementAt(0);
            spawnDebris(request.type, request.center, request.rotation, request.count, request.speed, request.baseVelocity, request.extentX, request.extentY);
        }
        while (!destroyQueue.isEmpty()) { Entity entity = (Entity) destroyQueue.elementAt(0); destroyQueue.removeElementAt(0); removeEntity(entity); }
    }
    public void spawnDebris(String type, Vec2 center, float baseRotation, int count, float scatterSpeed, Vec2 baseVelocity, float extentX, float extentY) {
        for (int i = 0; i < count; i++) {
            while (debris.size() >= MAX_DEBRIS) removeEntity((Entity) debris.elementAt(0));
            scratch.set((random.nextFloat() * 2 - 1) * extentX, (random.nextFloat() * 2 - 1) * extentY).rotate(baseRotation);
            spawnPosition.set(center).add(scratch);
            Entity piece = spawnEntity(type, spawnPosition, baseRotation + (random.nextFloat() * 2 - 1) * (float) Math.PI);
            if (piece == null) return;
            if (scratch.lengthSquared() > .01f) scratch2.set(scratch).normalize();
            else scratch2.set(0, -1).rotate(random.nextFloat() * (float) (Math.PI * 2));
            scratch2.y -= .6f;
            piece.body.linearVelocity.set(baseVelocity).scaleAdd(scratch2, scatterSpeed * (.5f + random.nextFloat() * .7f));
            piece.body.angularVelocity = (random.nextFloat() * 2 - 1) * 12; debris.addElement(piece);
        }
    }
    public int debrisCount() { return debris.size(); }
    public int objectCount() {
        int parts = 0; for (int i = 0; i < assemblies.size(); i++) parts += ((EntityAssembly) assemblies.elementAt(i)).parts.size();
        return entities.size() - parts + assemblies.size();
    }
    public Entity entityByUid(long uid) { for (int i = 0; i < entities.size(); i++) { Entity entity = (Entity) entities.elementAt(i); if (entity.uid == uid) return entity; } return null; }
    public EntityAssembly assemblyByUid(long uid) { for (int i = 0; i < assemblies.size(); i++) { EntityAssembly assembly = (EntityAssembly) assemblies.elementAt(i); if (assembly.uid == uid) return assembly; } return null; }
    public Vector connectedGroup(Entity entity) { if (entity.assembly != null) return entity.assembly.connectedParts(entity); Vector result = new Vector(); result.addElement(entity); return result; }
    public long nextUid() { return nextUid; }
    public void setNextUid(long value) { nextUid = Math.max(value, 1); }
    public boolean isInsideEscapeBounds(Vec2 point) {
        float top = floorY - wallHeight - 20000, left = -halfWidth - escapeMargin, right = halfWidth + escapeMargin, bottom = floorY + escapeMargin;
        return point.x >= left && point.x < right && point.y >= top && point.y < bottom;
    }
    public boolean isInsidePlayArea(Vec2 point) { return point.x > -halfWidth && point.x < halfWidth && point.y < floorY; }
    public boolean isAreaFree(Definitions.Entity definition, Vec2 at, float rotation) {
        Body query = new Body(definition, content.registry.material(definition.materialId)); query.setTransform(at.x, at.y, rotation);
        return !physics.overlaps(query, Body.WORLD | Body.ENTITIES, null);
    }
    public boolean canPlace(Definitions.Entity definition, Vec2 at, float rotation) {
        if (!definition.isAssembly()) return isInsidePlayArea(at) && isAreaFree(definition, at, rotation);
        for (int i = 0; i < definition.assembly.parts.size(); i++) {
            Definitions.Part part = (Definitions.Part) definition.assembly.parts.elementAt(i); scratch.set(part.position).rotate(rotation).add(at);
            if (!isInsidePlayArea(scratch) || !isAreaFree(part.definition, scratch, rotation + part.rotation)) return false;
        }
        return true;
    }
    public Entity pickEntityAt(Vec2 point) {
        Body body = physics.pick(point, Body.ENTITIES); return body != null && body.owner instanceof Entity ? (Entity) body.owner : null;
    }
}
