class_name SaveMigrator
extends RefCounted








static func migrate(data: Dictionary, target_version: int) -> Dictionary:
	var version: = int(data.get("format_version", 0))
	if version < 1:
		return {"ok": false, "data": data, "error": "Save file has no valid format_version."}
	if version > target_version:
		return {"ok": false, "data": data, 
			"error": "Save was made by a newer game version (format %d, this build supports %d)." % [version, target_version]}
	var migrated: = data.duplicate(true)
	while version < target_version:
		var upgraded: = _migrate_step(version, migrated)
		if upgraded.is_empty():
			return {"ok": false, "data": data, "error": "No migration from save format %d." % version}
		migrated = upgraded
		version += 1
		migrated["format_version"] = version
	return {"ok": true, "data": migrated, "error": ""}


static func _migrate_step(from_version: int, data: Dictionary) -> Dictionary:
	match from_version:
		1:
			return _migrate_v1_to_v2(data)
		2:
			return _migrate_v2_to_v3(data)
	return {}





static func _migrate_v1_to_v2(data: Dictionary) -> Dictionary:
	if not data.get("assemblies") is Array:
		data["assemblies"] = []
	return data






static func _migrate_v2_to_v3(data: Dictionary) -> Dictionary:
	return data
