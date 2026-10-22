package physicsplayground.save;

import java.io.IOException;
import java.util.Calendar;
import java.util.Vector;
import physicsplayground.core.Json;
import physicsplayground.game.CameraController;
import physicsplayground.game.Entity;
import physicsplayground.game.EntityAssembly;
import physicsplayground.game.SandboxWorld;

public final class SaveManager {
    public static final String FORMAT_ID = "physics_playground.world_save";
    public static final int FORMAT_VERSION = 3, MAX_FILE_BYTES = 64 * 1024 * 1024;
    public static final int OK = 0, OK_WITH_WARNINGS = 1, NOT_FOUND = 2, CORRUPT = 3, UNSUPPORTED_VERSION = 4, MALFORMED = 5, RECONSTRUCTION_FAILED = 6;
    public static final class LoadResult {
        public boolean ok, worldModified;
        public int status = NOT_FOUND, loadedCount, assemblyCount;
        public String message = "", recoveredFrom = "";
        public final Vector skippedTypes = new Vector(), errors = new Vector(), warnings = new Vector();
    }
    private static final class PartGroup {
        long uid; String type; final Vector parts = new Vector();
    }
    private final SaveStore store;
    public String lastErrorMessage = "";
    public SaveManager(SaveStore store) { this.store = store; }
    public boolean hasSlot(String slot) { return store != null && (store.exists(slot, SaveStore.PRIMARY) || store.exists(slot, SaveStore.PENDING) || store.exists(slot, SaveStore.BACKUP)); }
    public Json.Map buildSaveData(SandboxWorld world, CameraController camera) {
        Vector entities = copy(world.entities), assemblies = copy(world.assemblies); sortEntities(entities); sortAssemblies(assemblies);
        Vector entityEntries = new Vector(), assemblyEntries = new Vector();
        for (int i = 0; i < entities.size(); i++) { Entity entity = (Entity) entities.elementAt(i); if (!entity.isTransient()) entityEntries.addElement(EntitySerializer.serialize(entity)); }
        for (int i = 0; i < assemblies.size(); i++) assemblyEntries.addElement(AssemblySerializer.serialize((EntityAssembly) assemblies.elementAt(i)));
        Json.Map worldState = new Json.Map(); worldState.put("next_entity_uid", Json.num(world.nextUid())); worldState.put("camera", camera.serializeState());
        Json.Map data = new Json.Map(); data.put("format", FORMAT_ID); data.put("format_version", Json.num(FORMAT_VERSION));
        data.put("game_version", "0.3.1"); data.put("saved_at_utc", utcNow()); data.put("mods", world.content.registry.modVersions());
        data.put("world", worldState); data.put("entities", entityEntries); data.put("assemblies", assemblyEntries); return data;
    }
    public boolean saveToSlot(String slot, SandboxWorld world, CameraController camera) {
        lastErrorMessage = ""; if (store == null) { lastErrorMessage = "no save store"; return false; }
        String text = Json.write(buildSaveData(world, camera), true);
        if (!isSaveText(text)) { lastErrorMessage = "serialization produced invalid data"; return false; }
        try { store.write(slot, text); return true; }
        catch (IOException e) { lastErrorMessage = e.getMessage() == null ? "save write failed" : e.getMessage(); return false; }
    }
    public LoadResult loadFromSlot(String slot, SandboxWorld world, CameraController camera) {
        LoadResult result = new LoadResult();
        if (store == null || !hasSlot(slot)) { result.message = "No save found in slot '" + slot + "'."; return result; }
        String[] suffixes = {SaveStore.PRIMARY, SaveStore.PENDING, SaveStore.BACKUP};
        String[] sources = {"primary", "pending", "backup"}; String text = null; int found = -1;
        try {
            for (int i = 0; i < suffixes.length; i++) {
                if (!store.exists(slot, suffixes[i])) continue;
                String candidate = store.read(slot, suffixes[i]);
                if (candidate != null && isSaveText(candidate)) {
                    text = candidate; found = i;
                    if (i == 1) { store.promotePending(slot); result.warnings.addElement("recovered an interrupted save"); }
                    else if (i == 2) result.warnings.addElement("the save file was missing or damaged; loaded the previous backup");
                    break;
                }
                result.warnings.addElement("ignored unusable save candidate " + sources[i]);
            }
        } catch (IOException e) { result.warnings.addElement(e.getMessage() == null ? "save read failed" : e.getMessage()); }
        if (found < 0) { result.status = CORRUPT; result.message = "The save in slot '" + slot + "' is damaged and no usable backup exists."; return result; }
        result.recoveredFrom = sources[found];
        try {
            Object parsed = Json.parse(text);
            SaveMigrator.Result migration = SaveMigrator.migrate((Json.Map) parsed, FORMAT_VERSION);
            if (!migration.ok) {
                int version = Json.integer(((Json.Map) parsed).get("format_version"), 0);
                result.status = version > FORMAT_VERSION ? UNSUPPORTED_VERSION : MALFORMED; result.message = migration.error; result.errors.addElement(migration.error); return result;
            }
            LoadResult applied = applySaveData(migration.data, world, camera); applied.recoveredFrom = result.recoveredFrom;
            for (int i = 0; i < result.warnings.size(); i++) applied.warnings.insertElementAt(result.warnings.elementAt(i), i);
            if (!result.warnings.isEmpty() && applied.ok) { applied.status = OK_WITH_WARNINGS; applied.message += " (storage recovery warning)"; }
            return applied;
        } catch (Exception e) {
            result.status = MALFORMED; result.message = e.getMessage() == null ? "Malformed save" : e.getMessage(); result.errors.addElement(result.message); return result;
        }
    }
    public LoadResult applySaveData(Json.Map data, SandboxWorld world, CameraController camera) {
        LoadResult result = new LoadResult(); SaveValidator.LoadPlan plan = SaveValidator.validate(data, world.content.registry);
        copy(plan.warnings, result.warnings); copy(plan.missingTypes, result.skippedTypes);
        if (!plan.isValid()) {
            result.status = MALFORMED; copy(plan.errors, result.errors);
            result.message = "The save is invalid and was not loaded (the current world was kept): " + (plan.errors.isEmpty() ? "unknown error" : plan.errors.elementAt(0)); return result;
        }
        Json.Map snapshot = buildSaveData(world, camera); String failure;
        try { failure = reconstruct(plan, world, camera, result); }
        catch (RuntimeException e) { failure = e.getMessage() == null ? "reconstruction exception" : e.getMessage(); }
        result.worldModified = true;
        if (failure.length() > 0) {
            SaveValidator.LoadPlan restore = SaveValidator.validate(snapshot, world.content.registry);
            try { reconstruct(restore, world, camera, new LoadResult()); } catch (RuntimeException ignored) {}
            result.status = RECONSTRUCTION_FAILED; result.errors.addElement(failure); result.message = "Loading failed (" + failure + "); the previous world was restored."; return result;
        }
        result.ok = true; result.status = plan.missingTypes.isEmpty() && plan.warnings.isEmpty() ? OK : OK_WITH_WARNINGS;
        result.message = "Loaded " + world.objectCount() + " object(s).";
        if (!plan.missingTypes.isEmpty()) result.message += " Skipped unknown types: " + join(plan.missingTypes); return result;
    }
    private static String reconstruct(SaveValidator.LoadPlan plan, SandboxWorld world, CameraController camera, LoadResult result) {
        world.clearEntities(); Vector groups = new Vector();
        for (int i = 0; i < plan.entities.size(); i++) {
            SaveValidator.EntityPlan entry = (SaveValidator.EntityPlan) plan.entities.elementAt(i);
            Entity entity = world.spawnEntity(entry.typeId, entry.position, entry.rotation, entry.uid);
            if (entity == null) return "could not create '" + entry.typeId + "' (uid " + entry.uid + ")";
            EntitySerializer.applyState(entity, entry.data, true); result.loadedCount++;
            if (entry.assemblyUid > 0) {
                entity.assemblyPartName = entry.partName; PartGroup group = null;
                for (int g = 0; g < groups.size(); g++) if (((PartGroup) groups.elementAt(g)).uid == entry.assemblyUid) group = (PartGroup) groups.elementAt(g);
                if (group == null) { group = new PartGroup(); group.uid = entry.assemblyUid; groups.addElement(group); }
                group.parts.addElement(entity);
            }
        }
        for (int i = 0; i < plan.assemblies.size(); i++) {
            SaveValidator.AssemblyPlan entry = (SaveValidator.AssemblyPlan) plan.assemblies.elementAt(i); PartGroup group = null;
            for (int g = 0; g < groups.size(); g++) if (((PartGroup) groups.elementAt(g)).uid == entry.uid) group = (PartGroup) groups.elementAt(g);
            if (group == null || group.parts.isEmpty()) { result.warnings.addElement("assembly " + entry.uid + " has no surviving parts and was skipped"); continue; }
            EntityAssembly assembly = world.restoreAssembly(entry.typeId, entry.uid, group.parts, entry.brokenJoints, entry.controllerState);
            if (assembly == null) return "could not rebuild assembly " + entry.uid + " ('" + entry.typeId + "')"; result.assemblyCount++;
        }
        world.setNextUid(Math.max(world.nextUid(), plan.nextUid)); if (!plan.camera.isEmpty()) camera.applyState(plan.camera); return "";
    }
    public static boolean isSaveText(String text) {
        if (text == null || text.length() == 0 || text.length() > MAX_FILE_BYTES) return false;
        try { Object value = Json.parse(text); return value instanceof Json.Map && FORMAT_ID.equals(((Json.Map) value).get("format")); }
        catch (Exception e) { return false; }
    }
    private static String utcNow() {
        Calendar calendar = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        return calendar.get(Calendar.YEAR) + "-" + two(calendar.get(Calendar.MONTH) + 1) + "-" + two(calendar.get(Calendar.DAY_OF_MONTH))
                + "T" + two(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + two(calendar.get(Calendar.MINUTE)) + ":" + two(calendar.get(Calendar.SECOND));
    }
    private static String two(int value) { return value < 10 ? "0" + value : String.valueOf(value); }
    private static Vector copy(Vector source) { Vector result = new Vector(); for (int i = 0; i < source.size(); i++) result.addElement(source.elementAt(i)); return result; }
    private static void copy(Vector source, Vector target) { for (int i = 0; i < source.size(); i++) target.addElement(source.elementAt(i)); }
    private static void sortEntities(Vector values) {
        for (int i = 1; i < values.size(); i++) { Entity value = (Entity) values.elementAt(i); int at = i;
            while (at > 0 && ((Entity) values.elementAt(at - 1)).uid > value.uid) { values.setElementAt(values.elementAt(at - 1), at); at--; } values.setElementAt(value, at); }
    }
    private static void sortAssemblies(Vector values) {
        for (int i = 1; i < values.size(); i++) { EntityAssembly value = (EntityAssembly) values.elementAt(i); int at = i;
            while (at > 0 && ((EntityAssembly) values.elementAt(at - 1)).uid > value.uid) { values.setElementAt(values.elementAt(at - 1), at); at--; } values.setElementAt(value, at); }
    }
    private static String join(Vector values) { StringBuffer out = new StringBuffer(); for (int i = 0; i < values.size(); i++) { if (i > 0) out.append(", "); out.append(values.elementAt(i)); } return out.toString(); }
}
