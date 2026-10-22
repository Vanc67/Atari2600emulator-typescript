package physicsplayground.game;

import physicsplayground.core.Random;
import physicsplayground.core.Vec2;

/** Original procedural effect presets in flat primitive pools, suitable for a small CLDC heap. */
public final class Effects {
    public static final int DOT = 0, STREAK = 1, PUFF = 2, SHARD = 3, FLAME = 4;
    public static final int MAX_PARTICLES = 2400, MAX_FLASHES = 64, MAX_BURN_EMITTERS = 96;
    public final float[] x = new float[MAX_PARTICLES], y = new float[MAX_PARTICLES];
    public final float[] vx = new float[MAX_PARTICLES], vy = new float[MAX_PARTICLES];
    public final float[] life = new float[MAX_PARTICLES], maxLife = new float[MAX_PARTICLES];
    public final float[] size = new float[MAX_PARTICLES], gravity = new float[MAX_PARTICLES], drag = new float[MAX_PARTICLES];
    public final byte[] shapes = new byte[MAX_PARTICLES];
    public final int[] colors = new int[MAX_PARTICLES];
    public int alive;
    private int overwriteCursor;
    public final float[] flashX = new float[MAX_FLASHES], flashY = new float[MAX_FLASHES];
    public final float[] flashDirX = new float[MAX_FLASHES], flashDirY = new float[MAX_FLASHES];
    public final float[] flashRadius = new float[MAX_FLASHES], flashLife = new float[MAX_FLASHES], flashMaxLife = new float[MAX_FLASHES];
    public final byte[] flashKind = new byte[MAX_FLASHES];
    public int flashCount;
    private final Entity[] burners = new Entity[MAX_BURN_EMITTERS];
    private int burnerCount;
    private float flameTimer;
    private final Random random;
    private final Vec2 scratch = new Vec2(), scratch2 = new Vec2();

    private static final class Preset {
        int count, shape, colorA, colorB;
        float speedMin, speedMax, lifeMin, lifeMax, sizeMin, sizeMax, gravity, drag, cone;
    }
    private static final Preset SPARKS = preset(10, 180, 520, .12f, .35f, 1.5f, 2.5f, .6f, 2, STREAK, 1.4f, 0xffffd866, 0xffff9933);
    private static final Preset DUST = preset(7, 40, 140, .3f, .7f, 3, 7, -.05f, 3, PUFF, 1.8f, 0xb39e998f, 0x9973706b);
    private static final Preset SPLINTERS = preset(9, 90, 300, .35f, .8f, 2, 4, 1, 1, SHARD, 1.6f, 0xffb8874d, 0xff80592e);
    private static final Preset BLOOD = preset(8, 60, 220, .25f, .6f, 2, 3.5f, 1, 1.5f, DOT, 1.2f, 0xff9e0d0d, 0xff6b0508);
    private static final Preset GLASS = preset(14, 80, 360, .4f, .9f, 2, 4.5f, 1, .8f, SHARD, 3.2f, 0xd9bfe6ff, 0xe6e6f7ff);
    private static final Preset METAL = preset(8, 120, 380, .3f, .7f, 2, 3.5f, 1, 1, SHARD, 2.4f, 0xff99a3ad, 0xff666e75);
    private static final Preset SMOKE = preset(6, 20, 70, .8f, 1.6f, 8, 16, -.12f, 1.2f, PUFF, 3.2f, 0x8c4d4d52, 0x80333338);
    private static final Preset DEBRIS = preset(10, 120, 420, .4f, 1, 2, 5, 1, .6f, SHARD, 3.2f, 0xff736b61, 0xff4d4742);
    private static Preset preset(int count, float s0, float s1, float l0, float l1, float z0, float z1, float g, float d, int shape, float cone, int a, int b) {
        Preset p = new Preset(); p.count = count; p.speedMin = s0; p.speedMax = s1; p.lifeMin = l0; p.lifeMax = l1;
        p.sizeMin = z0; p.sizeMax = z1; p.gravity = g; p.drag = d; p.shape = shape; p.cone = cone; p.colorA = a; p.colorB = b; return p;
    }

    public Effects(Random random) { this.random = random; }
    private static Preset preset(String name) {
        if ("sparks".equals(name)) return SPARKS; if ("splinters".equals(name)) return SPLINTERS;
        if ("blood".equals(name)) return BLOOD; if ("glass".equals(name)) return GLASS;
        if ("metal".equals(name)) return METAL; if ("smoke".equals(name)) return SMOKE;
        if ("debris".equals(name)) return DEBRIS; return DUST;
    }
    public void burst(String name, Vec2 at, Vec2 direction, float scale) {
        Preset p = preset(name);
        int count = physicsplayground.core.Math2.round(p.count * clamp(scale, .3f, 4));
        float base = direction != null && direction.lengthSquared() > 0 ? direction.angle() : 0;
        float spread = direction != null && direction.lengthSquared() > 0 ? p.cone : (float) (Math.PI * 2);
        for (int i = 0; i < count; i++) {
            float angle = base + (random.nextFloat() * 2 - 1) * spread * .5f;
            float speed = range(p.speedMin, p.speedMax) * (float) Math.sqrt(scale);
            scratch2.set((float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed);
            emit(at.x, at.y, scratch2.x, scratch2.y, range(p.lifeMin, p.lifeMax), range(p.sizeMin, p.sizeMax) * (float) Math.sqrt(scale),
                    p.gravity, p.drag, p.shape, random.nextInt(2) == 0 ? p.colorA : p.colorB);
        }
    }
    public void muzzleFlash(Vec2 at, Vec2 direction, float radius) {
        addFlash(at.x, at.y, direction.x, direction.y, radius, .05f, 0);
        scratch.set(at.x + direction.x * radius * .5f, at.y + direction.y * radius * .5f);
        burst("smoke", scratch, direction, .4f);
    }
    public void explosion(Vec2 at, float radius) {
        float scale = clamp(radius / 200, .4f, 3);
        addFlash(at.x, at.y, 0, 0, radius * .55f, .12f, 1);
        addFlash(at.x, at.y, 0, 0, radius, .35f, 2);
        int count = physicsplayground.core.Math2.round(26 * scale);
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * (float) (Math.PI * 2);
            float speed = range(150, 700) * scale;
            int green = (int) (range(.45f, .8f) * 255);
            emit(at.x, at.y, (float) Math.cos(angle) * speed, (float) Math.sin(angle) * speed,
                    range(.2f, .5f), range(4, 9) * scale, -.2f, 3, FLAME, 0xe6000000 | (255 << 16) | (green << 8) | 38);
        }
        burst("smoke", at, null, 2.5f * scale); burst("sparks", at, null, 2 * scale);
    }
    public void setBurning(Entity entity, boolean burning) {
        int found = -1;
        for (int i = 0; i < burnerCount; i++) if (burners[i] == entity) { found = i; break; }
        if (burning) { if (found < 0 && burnerCount < burners.length) burners[burnerCount++] = entity; }
        else if (found >= 0) { burners[found] = burners[--burnerCount]; burners[burnerCount] = null; }
    }
    public void tick(float dt) {
        if (alive == 0 && flashCount == 0 && burnerCount == 0) return;
        flameTimer += dt;
        if (flameTimer >= .05f) { flameTimer = 0; emitFlames(); }
        int i = alive - 1;
        while (i >= 0) {
            float remaining = life[i] - dt;
            if (remaining <= 0) { alive--; move(alive, i); life[alive] = 0; }
            else {
                life[i] = remaining; vy[i] += 980 * gravity[i] * dt;
                float factor = Math.max(1 - drag[i] * dt, 0); vx[i] *= factor; vy[i] *= factor;
                x[i] += vx[i] * dt; y[i] += vy[i] * dt;
            }
            i--;
        }
        for (i = flashCount - 1; i >= 0; i--) {
            flashLife[i] -= dt;
            if (flashLife[i] <= 0) { flashCount--; if (i != flashCount) copyFlash(flashCount, i); }
        }
    }
    private void emitFlames() {
        int i = 0;
        while (i < burnerCount) {
            Entity entity = burners[i];
            if (entity == null || entity.removed || entity.thermal == null || !entity.thermal.burning) {
                burners[i] = burners[--burnerCount]; burners[burnerCount] = null; continue;
            }
            float ex = entity.definition.boundingWidth() * .5f, ey = entity.definition.boundingHeight() * .5f;
            for (int j = 0; j < 2; j++) {
                float ox = range(-ex, ex), oy = range(-ey, ey) * .6f;
                scratch.set(ox, oy).rotate(entity.body.cosine, entity.body.sine).add(entity.body.position);
                int green = (int) (range(.35f, .75f) * 255);
                emit(scratch.x, scratch.y, range(-20, 20), range(-140, -60), range(.25f, .5f),
                        range(3, 6) * clamp(ex / 20, .7f, 2), -.15f, 1.5f, FLAME, 0xd9000000 | (255 << 16) | (green << 8) | 25);
            }
            if (random.nextFloat() < .2f) {
                scratch.set(entity.body.position.x, entity.body.position.y - ey); scratch2.set(0, -1); burst("smoke", scratch, scratch2, .3f);
            }
            i++;
        }
    }
    private void emit(float px, float py, float velX, float velY, float duration, float particleSize, float gravityFactor, float damping, int shape, int color) {
        int slot = alive;
        if (alive < MAX_PARTICLES) alive++; else { slot = overwriteCursor; overwriteCursor = (overwriteCursor + 1) % MAX_PARTICLES; }
        x[slot] = px; y[slot] = py; vx[slot] = velX; vy[slot] = velY; life[slot] = maxLife[slot] = duration;
        size[slot] = particleSize; gravity[slot] = gravityFactor; drag[slot] = damping; shapes[slot] = (byte) shape; colors[slot] = color;
    }
    private void move(int from, int to) {
        if (from == to) return;
        x[to] = x[from]; y[to] = y[from]; vx[to] = vx[from]; vy[to] = vy[from]; life[to] = life[from]; maxLife[to] = maxLife[from];
        size[to] = size[from]; gravity[to] = gravity[from]; drag[to] = drag[from]; shapes[to] = shapes[from]; colors[to] = colors[from];
    }
    private void addFlash(float px, float py, float dx, float dy, float radius, float duration, int kind) {
        if (flashCount == MAX_FLASHES) { for (int i = 1; i < MAX_FLASHES; i++) copyFlash(i, i - 1); flashCount--; }
        int slot = flashCount++; flashX[slot] = px; flashY[slot] = py; flashDirX[slot] = dx; flashDirY[slot] = dy;
        flashRadius[slot] = radius; flashLife[slot] = flashMaxLife[slot] = duration; flashKind[slot] = (byte) kind;
    }
    private void copyFlash(int from, int to) {
        flashX[to] = flashX[from]; flashY[to] = flashY[from]; flashDirX[to] = flashDirX[from]; flashDirY[to] = flashDirY[from];
        flashRadius[to] = flashRadius[from]; flashLife[to] = flashLife[from]; flashMaxLife[to] = flashMaxLife[from]; flashKind[to] = flashKind[from];
    }
    public int burningEmitterCount() { return burnerCount; }
    public void clear() {
        alive = flashCount = burnerCount = 0; overwriteCursor = 0; flameTimer = 0;
        for (int i = 0; i < burners.length; i++) burners[i] = null;
    }
    private float range(float a, float b) { return a + random.nextFloat() * (b - a); }
    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
}
