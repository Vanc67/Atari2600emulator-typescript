package physicsplayground.save;

import physicsplayground.core.Json;

public final class SaveMigrator {
    public static final class Result {
        public boolean ok; public Json.Map data; public String error = "";
    }
    private SaveMigrator() {}
    public static Result migrate(Json.Map data, int targetVersion) {
        Result result = new Result(); result.data = data;
        int version = Json.integer(data.get("format_version"), 0);
        if (version < 1) { result.error = "Save file has no valid format_version."; return result; }
        if (version > targetVersion) { result.error = "Save was made by a newer game version (format " + version + ", this build supports " + targetVersion + ")."; return result; }
        Json.Map migrated = (Json.Map) Json.deepCopy(data);
        while (version < targetVersion) {
            if (version == 1 && !(migrated.get("assemblies") instanceof java.util.Vector)) migrated.put("assemblies", new java.util.Vector());
            else if (version != 1 && version != 2) { result.error = "No migration from save format " + version + "."; result.data = data; return result; }
            version++; migrated.put("format_version", Json.num(version));
        }
        result.ok = true; result.data = migrated; return result;
    }
}
