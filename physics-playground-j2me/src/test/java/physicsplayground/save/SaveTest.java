package physicsplayground.save;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import physicsplayground.core.Content;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;
import physicsplayground.game.*;

public final class SaveTest {
    private static int checks, failures;
    private static void check(boolean value, String label) { checks++; if (!value) { failures++; System.out.println("FAIL " + label); } }
    private static void steps(SandboxWorld world, int count) { for (int i = 0; i < count; i++) world.step(1f / 120f); }
    private static String resource(String path) throws Exception { InputStream in = SaveTest.class.getResourceAsStream(path); try { return Json.readUtf8(in); } finally { in.close(); } }

    private static final class MemoryStore implements SaveStore {
        final Hashtable values = new Hashtable();
        public boolean exists(String slot, String suffix) { return values.containsKey(slot + suffix); }
        public String read(String slot, String suffix) { return (String) values.get(slot + suffix); }
        public void write(String slot, String text) {
            String old = (String) values.get(slot); if (old != null) values.put(slot + BACKUP, old);
            values.put(slot + PENDING, text); if (!text.equals(values.get(slot + PENDING))) throw new RuntimeException("readback");
            values.put(slot, text); values.remove(slot + PENDING);
        }
        public void promotePending(String slot) { values.put(slot, values.get(slot + PENDING)); values.remove(slot + PENDING); }
    }

    public static void main(String[] args) throws Exception {
        Content content = Content.loadDefault(); SandboxWorld world = new SandboxWorld(content, true, 303); CameraController camera = new CameraController();
        MemoryStore store = new MemoryStore(); SaveManager saves = new SaveManager(store);
        Json.Map current = new Json.Map(); current.put("format_version", Json.num(1)); check(SaveMigrator.migrate(current, 1).ok, "current migration version passes");
        current.put("format_version", Json.num(99)); check(!SaveMigrator.migrate(current, 3).ok, "future migration version rejected");
        check(!SaveMigrator.migrate(new Json.Map(), 3).ok, "missing migration version rejected");

        Entity pistol = world.spawnEntity("core:pistol", new Vec2(0, -400), .4f); pistol.setFrozen(true); pistol.magazine.rounds = 5;
        Entity revolver = world.spawnEntity("core:revolver", new Vec2(100, -400), 0); revolver.setFrozen(true); revolver.magazine.rounds = 2; revolver.reload();
        Entity bomb = world.spawnEntity("core:small_bomb", new Vec2(-600, -14), 0);
        Entity crate = world.spawnEntity("core:wooden_box", new Vec2(400, -32), 0);
        Entity barrel = world.spawnEntity("core:explosive_barrel", new Vec2(-300, -42), 0);
        EntityAssembly human = world.spawnAssembly("core:human", new Vec2(800, -110), 0, 0);
        steps(world, 10); bomb.activate(true); SandboxTools.ignite(crate); barrel.thermal.addHeat(80); human.part("head").health.applyDamage(40, null); human.breakJointByName("elbow_right");
        Entity debrisCrate = world.spawnEntity("core:wooden_box", new Vec2(-900, -32), 0); steps(world, 2); debrisCrate.health.applyDamage(1000, null); steps(world, 60);
        long pistolUid = pistol.uid, revolverUid = revolver.uid, bombUid = bomb.uid, crateUid = crate.uid, barrelUid = barrel.uid, humanUid = human.uid;
        float fuse = bomb.explosive.fuseRemaining, temperature = barrel.thermal.temperature; long nextUid = world.nextUid();
        check(saves.saveToSlot("roundtrip", world, camera), "v3 save succeeds " + saves.lastErrorMessage);
        String text = store.read("roundtrip", SaveStore.PRIMARY);
        check(text.indexOf("\"format_version\": 3") >= 0, "save uses format version 3"); check(text.indexOf("core:wood_chunk") < 0, "transient debris not saved");
        world.clearEntities(); SaveManager.LoadResult loaded = saves.loadFromSlot("roundtrip", world, camera);
        check(loaded.ok, "v3 loads: " + loaded.message + " " + loaded.errors); check(loaded.loadedCount == 16 && loaded.assemblyCount == 1, "all bodies and assembly restored " + loaded.loadedCount);
        Entity loadedPistol = world.entityByUid(pistolUid), loadedRevolver = world.entityByUid(revolverUid), loadedBomb = world.entityByUid(bombUid), loadedCrate = world.entityByUid(crateUid), loadedBarrel = world.entityByUid(barrelUid);
        check(loadedPistol != null && loadedPistol.magazine.rounds == 5 && Math.abs(loadedPistol.body.rotation - .4f) < .001f, "weapon rotation and ammo restored");
        check(loadedRevolver != null && loadedRevolver.magazine.reloading, "reload progress restored");
        check(loadedBomb != null && loadedBomb.explosive.armed && Math.abs(loadedBomb.explosive.fuseRemaining - fuse) < .05f, "armed fuse restored");
        check(loadedCrate != null && loadedCrate.thermal.burning && loadedCrate.health.isDamaged(), "burning and health restored");
        check(loadedBarrel != null && Math.abs(loadedBarrel.thermal.temperature - temperature) < 1, "temperature restored");
        EntityAssembly loadedHuman = world.assemblyByUid(humanUid);
        check(loadedHuman != null && loadedHuman.brokenJointNames().contains("elbow_right"), "broken joint restored");
        check(loadedHuman != null && Math.abs(loadedHuman.part("head").health.currentHealth - 60) < .01f, "limb health restored");
        check(world.nextUid() >= nextUid, "next uid preserved"); check(world.thermal.activeCount() > 0, "restored heat remains active");
        steps(world, (int) (120 * (fuse + .5f))); check(loadedBomb.removed, "restored bomb still explodes");

        String[] fixtures = {"/fixtures/saves/v0_1_world.json", "/fixtures/saves/v0_2_world.json"};
        for (int i = 0; i < fixtures.length; i++) {
            world.clearEntities(); store.values.put("compat", resource(fixtures[i])); loaded = saves.loadFromSlot("compat", world, camera);
            check(loaded.ok && loaded.skippedTypes.isEmpty(), fixtures[i] + " loads: " + loaded.message + " " + loaded.errors);
            if (i == 1) {
                check(world.assemblies.size() == 1, "v0.2 contains one human"); human = (EntityAssembly) world.assemblies.elementAt(0);
                check(human.brokenJointNames().size() == 1 && human.brokenJointNames().contains("elbow_right"), "v0.2 broken elbow restored");
                check(human.part("thigh_left").health.isDamaged(), "v0.2 thigh damage restored"); steps(world, 120);
                check(human.brokenJointNames().size() == 1, "restored ragdoll remains stable");
            }
        }

        world.clearEntities(); Entity keeper = world.spawnEntity("core:wooden_box", new Vec2(0, -100), 0);
        Json.Map malformed = new Json.Map(); Vector duplicateEntities = new Vector();
        for (int i = 0; i < 2; i++) { Json.Map entry = new Json.Map(); entry.put("uid", Json.num(1)); entry.put("type", "core:wooden_box"); Vector pos = new Vector(); pos.addElement(Json.num(0)); pos.addElement(Json.num(-100)); entry.put("position", pos); duplicateEntities.addElement(entry); }
        malformed.put("entities", duplicateEntities); malformed.put("assemblies", new Vector()); loaded = saves.applySaveData(malformed, world, camera);
        check(!loaded.ok && !loaded.worldModified, "duplicate uid rejected before world mutation"); check(world.entityByUid(keeper.uid) == keeper, "invalid save keeps current world");
        Json.Map unknown = new Json.Map(); unknown.put("entities", duplicateEntities); unknown.put("assemblies", new Vector());
        ((Json.Map) duplicateEntities.elementAt(1)).put("uid", Json.num(2)); ((Json.Map) duplicateEntities.elementAt(0)).put("type", "missing_mod:thing");
        loaded = saves.applySaveData(unknown, world, camera); check(loaded.ok && loaded.skippedTypes.contains("missing_mod:thing"), "unknown type skipped and reported");

        world.clearEntities(); world.spawnEntity("core:wooden_box", new Vec2(0, -100), 0); saves.saveToSlot("recovery", world, camera);
        String backup = store.read("recovery", SaveStore.PRIMARY); world.clearEntities(); world.spawnEntity("core:rubber_ball", new Vec2(200, -100), 0); saves.saveToSlot("recovery", world, camera);
        store.values.put("recovery", "not json"); loaded = saves.loadFromSlot("recovery", world, camera);
        check(loaded.ok && "backup".equals(loaded.recoveredFrom), "corrupt primary recovers backup"); check(store.read("recovery", SaveStore.BACKUP).equals(backup), "backup remains intact");
        store.values.remove("recovery"); store.values.put("recovery" + SaveStore.PENDING, backup); loaded = saves.loadFromSlot("recovery", world, camera);
        check(loaded.ok && "pending".equals(loaded.recoveredFrom), "pending save recovers interrupted write"); check(store.read("recovery", SaveStore.PRIMARY).equals(backup), "pending promoted to primary");

        System.out.println("SaveTest: " + checks + " checks, " + failures + " failure(s)"); if (failures > 0) System.exit(1);
    }
}
