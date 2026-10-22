package physicsplayground.game;

import physicsplayground.core.Content;
import physicsplayground.core.Vec2;

public final class InteractionTest {
    private static int checks, failures;
    private static SandboxWorld world;
    private static InteractionController interaction;
    private static void check(boolean value, String label) { checks++; if (!value) { failures++; System.out.println("FAIL " + label); } }
    private static void steps(int count) { for (int i = 0; i < count; i++) world.step(1f / 120f); }
    private static void reset() { world.clearEntities(); interaction.releaseActivation(); interaction.endDrag(); interaction.clearSelection(); steps(2); }
    public static void main(String[] args) throws Exception {
        world = new SandboxWorld(Content.loadDefault(), true, 303); interaction = new InteractionController(world);
        Entity box = world.spawnEntity("core:concrete_block", new Vec2(0, -32), 0); steps(30);
        Vec2 grab = box.body.position.copy().add(20, 0); DragHandle drag = new DragHandle(box, grab); drag.targetPosition.set(grab).add(0, -400);
        for (int i = 0; i < 240; i++) { drag.update(false, false, 1f / 120f); world.step(1f / 120f); }
        float error = drag.anchorWorld().distanceTo(drag.targetPosition); check(error < 8, "spring drag holds 147 kg block error=" + error);
        drag.targetPosition.add(300, 0); for (int i = 0; i < 12; i++) { drag.update(false, false, 1f / 120f); world.step(1f / 120f); }
        check(box.body.linearVelocity.length() > 100, "moving drag target creates throw velocity " + box.body.linearVelocity.length()); reset();

        box = world.spawnEntity("core:wooden_box", new Vec2(0, -600), 0); interaction.select(box); interaction.toggleFreezeSelected(); steps(60);
        check(box.body.frozen && box.body.position.y == -600, "frozen box hangs in air"); box.health.applyDamage(25, null); interaction.duplicateSelected();
        Entity copy = interaction.selectedEntity; check(copy != null && copy != box, "duplicate selected"); check(copy.body.frozen, "duplicate copies frozen state");
        check(copy.uid != box.uid, "duplicate gets new uid"); check(copy.health.currentHealth == 75, "duplicate copies health");
        float startY = copy.body.position.y; interaction.toggleFreezeSelected(); steps(60);
        check(!copy.body.frozen && copy.body.position.y > startY + 20, "unfrozen copy falls onto original dy=" + (copy.body.position.y - startY));
        interaction.deleteSelected(); check(copy.removed, "deleted entity removed"); check(interaction.selectedEntity == null, "selection cleared after delete"); reset();

        Entity crate = world.spawnEntity("core:wooden_box", new Vec2(0, -32), 0), steel = world.spawnEntity("core:steel_box", new Vec2(200, -32), 0); steps(5);
        interaction.setTool(SandboxTools.FREEZE); interaction.applyToolAt(crate.body.position); check(crate.body.frozen, "freeze tool freezes");
        interaction.applyToolAt(crate.body.position); check(!crate.body.frozen, "freeze tool unfreezes");
        interaction.setTool(SandboxTools.DAMAGE); interaction.applyToolAt(crate.body.position); check(crate.health.maxHealth - crate.health.currentHealth == 25, "damage tool configured amount");
        interaction.setTool(SandboxTools.HEAL); interaction.applyToolAt(crate.body.position); check(!crate.health.isDamaged(), "heal tool restores health");
        interaction.setTool(SandboxTools.IGNITE); interaction.applyToolAt(crate.body.position); check(crate.thermal.burning, "ignite tool sets fire");
        interaction.applyToolAt(steel.body.position); check(!steel.thermal.burning, "ignite tool rejects steel");
        interaction.setTool(SandboxTools.EXTINGUISH); interaction.applyToolAt(crate.body.position); check(!crate.thermal.burning, "extinguish tool");
        interaction.setTool(SandboxTools.INSPECT); interaction.select(steel); check(interaction.selectedEntity == steel, "inspect selection");
        interaction.setTool(SandboxTools.DELETE); interaction.applyToolAt(steel.body.position); check(steel.removed, "delete tool removes");
        int explosions = world.explosions.explosionCount; interaction.setTool(SandboxTools.EXPLODE); interaction.applyToolAt(new Vec2(600, -300)); steps(2);
        check(world.explosions.explosionCount == explosions + 1, "explode tool creates blast");
        interaction.setTool(SandboxTools.DRAG); Entity pistol = world.spawnEntity("core:pistol", new Vec2(0, -500), 0); interaction.select(pistol);
        interaction.activateSelected(true); interaction.activateSelected(false); check(pistol.firearm.shotsFired == 1, "activation fires selected weapon");
        interaction.reloadSelected(); check(pistol.magazine.reloading, "reload selected weapon"); reset();

        interaction.beginSpawn("core:wooden_box"); Vec2 at = new Vec2(0, -300); interaction.spawnPlacer.updatePreview(at);
        check(interaction.isSpawning() && interaction.spawnPlacer.placementValid, "spawn preview accepts free space"); interaction.physicsStep(1f / 120f, 1f / 120f, 1, at, false);
        check(interaction.spawnPlacer.placementRotation > 0, "ghost rotation"); check(interaction.tryPlaceSpawn(at), "spawn placement succeeds"); check(world.entities.size() == 1, "placed entity exists");
        interaction.cancelSpawn(); check(!interaction.isSpawning(), "cancel spawn mode"); reset();

        EntityAssembly human = world.spawnAssembly("core:human", new Vec2(0, -110), 0, 0); human.breakJointByName("elbow_right"); interaction.select(human.part("head")); interaction.duplicateSelected();
        check(world.assemblies.size() == 2, "connected ragdoll duplicated as assembly"); EntityAssembly duplicated = interaction.selectedEntity.assembly;
        check(duplicated != null && duplicated != human && duplicated.parts.size() == 10, "duplicate has the ten connected parts"); check(duplicated.brokenJointNames().contains("elbow_right"), "duplicate preserves broken joints"); reset();
        System.out.println("InteractionTest: " + checks + " checks, " + failures + " failure(s)"); if (failures > 0) System.exit(1);
    }
}
