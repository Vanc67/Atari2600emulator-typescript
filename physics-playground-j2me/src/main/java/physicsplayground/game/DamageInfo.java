package physicsplayground.game;

import physicsplayground.core.Vec2;

/** Same damage kinds and payload as the original DamageInfo. */
public final class DamageInfo {
    public static final int GENERIC = 0, IMPACT = 1, PROJECTILE = 2, EXPLOSION = 3, FIRE = 4;
    private static final String[] NAMES = {"generic", "impact", "projectile", "explosion", "fire"};
    public int kind;
    public float amount;
    public float applied;
    public float impactSpeed;
    public float impulse;
    public final Vec2 worldPosition = new Vec2();
    public long sourceId;
    public final Vec2 direction = new Vec2();

    public DamageInfo(int kind, float amount) { this.kind = kind; this.amount = amount; }
    public static DamageInfo generic(float amount) { return new DamageInfo(GENERIC, amount); }
    public static DamageInfo impact(float amount, float speed, float impulse, Vec2 point, long source) {
        DamageInfo info = new DamageInfo(IMPACT, amount); info.impactSpeed = speed; info.impulse = impulse;
        info.worldPosition.set(point); info.sourceId = source; return info;
    }
    public static DamageInfo projectile(float amount, Vec2 point, Vec2 direction, long source) {
        DamageInfo info = new DamageInfo(PROJECTILE, amount); info.worldPosition.set(point); info.direction.set(direction); info.sourceId = source; return info;
    }
    public static DamageInfo explosion(float amount, float speed, Vec2 point, Vec2 direction, long source) {
        DamageInfo info = new DamageInfo(EXPLOSION, amount); info.impactSpeed = speed; info.worldPosition.set(point); info.direction.set(direction); info.sourceId = source; return info;
    }
    public static DamageInfo fire(float amount) { return new DamageInfo(FIRE, amount); }
    public boolean canTear() { return kind == IMPACT || kind == EXPLOSION; }
    public String kindName() { return NAMES[kind]; }
}
