package physicsplayground.game;

import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;
import physicsplayground.physics.Body;

/** Behavioral scenarios ported from the original smoke/weapons suites. */
public final class GameTest {
    private static int checks, failures;
    private static SandboxWorld world;
    private static void check(boolean condition, String label) { checks++; if (!condition) { failures++; System.out.println("FAIL " + label); } }
    private static void steps(int count) { for (int i = 0; i < count; i++) world.step(1f / 120f); }
    private static Entity frozen(String type, float x, float y, float angle) { Entity e = world.spawnEntity(type, new Vec2(x, y), angle); e.setFrozen(true); return e; }
    private static void shoot(Entity gun) { gun.activate(true); gun.activate(false); }
    private static float damage(Entity e) { return e == null || e.health == null ? 0 : e.health.maxHealth - e.health.currentHealth; }
    private static void reset() { world.clearEntities(); steps(2); }
    private static boolean finite(Entity e) { return e.body.position.isFinite() && e.body.linearVelocity.isFinite() && !Float.isNaN(e.body.rotation + e.body.angularVelocity) && !Float.isInfinite(e.body.rotation + e.body.angularVelocity); }

    public static void main(String[] args) throws Exception {
        world = new SandboxWorld(Content.loadDefault(), true, 303);
        spawnAndSettle(); pistolAndHeadshot(); fireModesAndPenetration(); explosionsAndDestruction(); fireAndEffects(); pauseAndPlacement();
        System.out.println("GameTest: " + checks + " checks, " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
    }
    private static void spawnAndSettle() {
        String[] ids = {"core:wooden_box", "core:steel_box", "core:metal_barrel", "core:concrete_block", "core:rubber_ball", "core:metal_sphere", "core:wooden_plank"};
        Entity[] spawned = new Entity[ids.length]; float x = -600;
        for (int i = 0; i < ids.length; i++) { spawned[i] = world.spawnEntity(ids[i], new Vec2(x, -200), 0); check(spawned[i] != null, "spawn " + ids[i]); x += 200; }
        check(world.entities.size() == ids.length, "spawn entity count"); steps(480);
        for (int i = 0; i < spawned.length; i++) {
            Entity e = spawned[i]; float r = e.definition.boundingRadius();
            check(e.body.position.y < 0 && e.body.position.y > -r - 2, ids[i] + " rests on floor y=" + e.body.position.y);
            check(finite(e), ids[i] + " finite after settling");
        }
        reset(); check(world.entities.isEmpty(), "clear_entities empties world");
    }
    private static void pistolAndHeadshot() {
        Entity pistol = frozen("core:pistol", 0, -300, 0); Entity target = frozen("core:steel_box", 400, -300, 0);
        check(pistol.magazine.rounds == 12 && pistol.isActivatable(), "pistol full magazine and activatable");
        pistol.activate(true); check(world.projectiles.count() == 1 && pistol.magazine.rounds == 11, "trigger fires one round"); steps(120);
        check(pistol.firearm.shotsFired == 1, "semi automatic held trigger fires once"); pistol.activate(false);
        check(world.projectiles.count() == 0, "projectile removed after hit"); shoot(pistol); check(pistol.firearm.shotsFired == 2, "new press after cooldown fires");
        shoot(pistol); check(pistol.firearm.shotsFired == 2, "cooldown blocks immediate shot"); check(!target.removed, "steel box survives pistol");
        reset();
        EntityAssembly human = world.spawnAssembly("core:human", new Vec2(400, -110), 0, 0);
        for (int i = 0; i < human.parts.size(); i++) ((Entity) human.parts.elementAt(i)).setFrozen(true);
        Entity head = human.part("head"); pistol = frozen("core:pistol", head.body.position.x - 400, head.body.position.y + 3, 0);
        shoot(pistol); steps(10); float headDamage = damage(head);
        check(headDamage > 45 && headDamage < 52, "pistol headshot 32 x 1.6, actual=" + headDamage);
        check(damage(human.part("upper_torso")) == 0, "only struck limb damaged");
        steps(25); shoot(pistol); steps(25); shoot(pistol); steps(10);
        check(head.health.isDepleted(), "three headshots deplete head"); check("dead".equals(human.controller.stateName()), "destroyed head kills human");
        check(head.damageLevel == 1, "damage visual level reaches one"); reset();
    }
    private static void fireModesAndPenetration() {
        Entity rifle = frozen("core:assault_rifle", 0, -600, -.6f); rifle.activate(true); steps(120); rifle.activate(false);
        check(rifle.firearm.shotsFired >= 10 && rifle.firearm.shotsFired <= 12, "assault rifle automatic rate=" + rifle.firearm.shotsFired);
        Entity smg = frozen("core:smg", 0, -700, -.6f); smg.activate(true); steps(120); smg.activate(false);
        check(smg.firearm.shotsFired >= 14 && smg.firearm.shotsFired <= 16, "SMG automatic rate=" + smg.firearm.shotsFired);
        Entity shotgun = frozen("core:shotgun", 0, -800, -.6f); int before = world.projectiles.count(); shoot(shotgun);
        check(world.projectiles.count() - before == 9, "shotgun fires nine pellets"); check(shotgun.magazine.rounds == 5, "shotgun capacity six");
        Entity pistol = frozen("core:pistol", 0, -900, -.6f); pistol.magazine.rounds = 1; shoot(pistol); steps(30);
        before = world.projectiles.count(); shoot(pistol); check(pistol.magazine.rounds == 0 && world.projectiles.count() == before, "empty weapon dry fires");
        pistol.reload(); check(pistol.magazine.reloading, "reload starts"); steps(30); shoot(pistol); check(pistol.firearm.shotsFired == 1, "cannot fire during magazine reload");
        steps(240); check(pistol.magazine.rounds == 12 && !pistol.magazine.reloading, "magazine reload refills twelve"); reset();
        Entity revolver = frozen("core:revolver", 0, -1000, -.6f); revolver.magazine.rounds = 0; revolver.reload(); steps(70);
        check(revolver.magazine.rounds == 1, "revolver reloads one round"); steps(360); check(revolver.magazine.rounds == 6, "revolver fills six"); reset();
        Entity plank = frozen("core:wooden_plank", 200, -400, (float) (Math.PI * .5)); Entity behind = frozen("core:wooden_box", 400, -400, 0);
        shoot(frozen("core:pistol", 0, -397, 0)); steps(10);
        check(damage(plank) > 0 && damage(behind) > 0, "pistol penetrates wooden plank and hits box");
        check(damage(behind) < damage(plank), "penetration loses energy " + damage(behind) + " vs " + damage(plank)); reset();
        Entity steel = frozen("core:steel_box", 200, -700, 0); behind = frozen("core:wooden_box", 400, -700, 0);
        shoot(frozen("core:pistol", 0, -697, 0)); steps(10); check(damage(behind) == 0, "steel stops pistol"); reset();
        Entity glass = frozen("core:glass_panel", 200, -1300, 0); behind = frozen("core:wooden_box", 400, -1300, 0);
        shoot(frozen("core:pistol", 0, -1297, 0)); steps(10);
        check(glass.removed, "bullet shatters glass"); check(world.debrisCount() > 0, "glass creates shards");
        check(damage(behind) > 25, "bullet continues through glass damage=" + damage(behind)); reset();
    }
    private static void explosionsAndDestruction() {
        Entity near = world.spawnEntity("core:rubber_ball", new Vec2(80, -100), 0), far = world.spawnEntity("core:rubber_ball", new Vec2(200, -100), 0);
        steps(1); int count = world.explosions.explosionCount; SandboxTools.explodeAt(world, new Vec2(0, -100)); steps(2);
        check(world.explosions.explosionCount == count + 1, "explosion processed");
        check(near.body.linearVelocity.x > far.body.linearVelocity.x && far.body.linearVelocity.x > 0, "explosion force falls off");
        check(damage(near) > damage(far) && damage(far) > 0, "explosion damage falls off " + damage(near) + " vs " + damage(far));
        check(near.body.linearVelocity.length() <= 4050, "explosion speed capped"); reset();
        frozen("core:concrete_block", -100, -32, 0); Entity open = world.spawnEntity("core:rubber_ball", new Vec2(190, -40), 0), shielded = world.spawnEntity("core:rubber_ball", new Vec2(-190, -40), 0);
        steps(1); SandboxTools.explodeAt(world, new Vec2(0, -40)); steps(2);
        check(damage(shielded) < damage(open) * .6f, "concrete shields blast " + damage(shielded) + " vs " + damage(open)); reset();
        Entity crate = world.spawnEntity("core:wooden_box", new Vec2(0, -32), 0); steps(2); crate.health.applyDamage(1000, null); steps(3);
        check(crate.removed && world.debrisCount() == 4, "crate breaks into four chunks");
        Entity piece = null; for (int i = 0; i < world.entities.size(); i++) { Entity e = (Entity) world.entities.elementAt(i); if (e.isTransient()) piece = e; }
        check(piece != null && piece.body.layer == Body.DEBRIS && piece.body.mask == Body.WORLD, "debris collision layers"); steps(840);
        check(world.debrisCount() == 0, "debris expires"); reset();
        EntityAssembly human = world.spawnAssembly("core:human", new Vec2(0, -110), 0, 0); Entity bomb = world.spawnEntity("core:large_bomb", new Vec2(160, -20), 0);
        steps(5); bomb.explosive.arm(0); steps(120); boolean allHurt = true, stable = true;
        for (int i = 0; i < human.parts.size(); i++) { Entity part = (Entity) human.parts.elementAt(i); if (damage(part) <= 0) allHurt = false; if (!finite(part)) stable = false; }
        check(allHurt, "large bomb damages every limb"); check("dead".equals(human.controller.stateName()), "point blank large bomb kills"); check(stable, "ragdoll stable after blast"); reset();
    }
    private static void fireAndEffects() {
        Entity a = world.spawnEntity("core:wooden_box", new Vec2(0, -32), 0), b = world.spawnEntity("core:wooden_box", new Vec2(66, -32), 0), steel = world.spawnEntity("core:steel_box", new Vec2(-80, -32), 0);
        steps(5); check(SandboxTools.ignite(a) && a.thermal.burning, "wood ignites"); check(!SandboxTools.ignite(steel), "steel cannot ignite");
        check(world.effects.burningEmitterCount() == 1, "burning effect emitter registered"); steps(720);
        check(damage(a) > 20, "burning damages over time " + damage(a)); check(b.thermal.burning, "fire spreads to neighbouring crate");
        check(steel.thermal.temperature > 60, "heat transfers to steel " + steel.thermal.temperature); SandboxTools.extinguish(world, b); check(!b.thermal.burning, "extinguish stops fire"); reset();
        check(world.thermal.activeCount() == 0, "cold props do not remain in thermal registry");
        check(world.effects.alive == 0, "effects start empty after reset"); SandboxTools.explodeAt(world, new Vec2(0, -200)); steps(3);
        check(world.effects.alive > 20 && world.effects.alive <= Effects.MAX_PARTICLES, "explosion emits particles " + world.effects.alive);
        for (int i = 0; i < 400; i++) world.effects.burst("sparks", new Vec2(0, -200), new Vec2(0, -1), 2);
        check(world.effects.alive <= Effects.MAX_PARTICLES, "particle pool capped"); steps(360);
        check(world.effects.alive == 0 && world.effects.flashCount == 0, "effects clean themselves"); reset();
    }
    private static void pauseAndPlacement() {
        Entity box = world.spawnEntity("core:wooden_box", new Vec2(0, -800), 0); steps(5); world.paused = true; float y = box.body.position.y; steps(60);
        check(box.body.position.y == y, "paused bodies do not move"); check(world.pickEntityAt(box.body.position) == box, "picking works while paused");
        world.paused = false; steps(30); check(box.body.position.y > y + 10, "unpause resumes physics"); reset();
        Definitions.Entity definition = world.content.registry.entity("core:wooden_box");
        check(world.canPlace(definition, new Vec2(0, -300), 0), "free spot placeable"); check(!world.canPlace(definition, new Vec2(0, 10), 0), "floor placement rejected");
        box = frozen("core:wooden_box", 500, -300, 0); steps(2); check(!world.canPlace(definition, new Vec2(510, -300), 0), "overlap placement rejected"); reset();
    }
}
