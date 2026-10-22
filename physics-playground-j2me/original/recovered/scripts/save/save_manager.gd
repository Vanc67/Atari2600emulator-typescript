class_name SaveManager
extends RefCounted



















const FORMAT_ID: = "physics_playground.world_save"
const FORMAT_VERSION: = 3
const SAVE_DIRECTORY: = "user://saves"
const FILE_EXTENSION: = ".json"

const MAX_FILE_BYTES: = 64 * 1024 * 1024

enum Status{
	OK, 
	OK_WITH_WARNINGS, 
	NOT_FOUND, 
	CORRUPT, 
	UNSUPPORTED_VERSION, 
	MALFORMED, 
	RECONSTRUCTION_FAILED, 
}


class LoadResult:
	var ok: = false
	var status: Status = Status.NOT_FOUND
	var message: = ""
	var loaded_count: = 0
	var assembly_count: = 0
	var skipped_types: PackedStringArray = []
	var errors: PackedStringArray = []
	var warnings: PackedStringArray = []

	var recovered_from: = ""


	var world_modified: = false



var save_directory: = SAVE_DIRECTORY

var last_error_message: = ""

var _store: = SaveFileStore.new()


func _init(directory: = SAVE_DIRECTORY) -> void :
	save_directory = directory


func get_slot_path(slot_name: String) -> String:
	return save_directory.path_join(slot_name.validate_filename() + FILE_EXTENSION)


func has_slot(slot_name: String) -> bool:
	return _store.exists(get_slot_path(slot_name))


func build_save_data(world: SandboxWorld, camera: CameraController, registry: EntityRegistry) -> Dictionary:
	var entity_entries: Array = []
	var entities: = world.get_entities()
	entities.sort_custom( func(a: SandboxEntity, b: SandboxEntity) -> bool: return a.entity_uid < b.entity_uid)
	for entity in entities:

		if not entity.is_transient():
			entity_entries.append(EntitySerializer.serialize(entity))
	var assembly_entries: Array = []
	var assemblies: = world.get_assemblies()
	assemblies.sort_custom( func(a: EntityAssembly, b: EntityAssembly) -> bool: return a.assembly_uid < b.assembly_uid)
	for assembly in assemblies:
		assembly_entries.append(AssemblySerializer.serialize(assembly))
	return {
		"format": FORMAT_ID, 
		"format_version": FORMAT_VERSION, 
		"game_version": GameState.GAME_VERSION, 
		"saved_at_utc": Time.get_datetime_string_from_system(true), 
		"mods": registry.get_mod_versions(), 
		"world": {
			"next_entity_uid": world.get_next_uid(), 
			"camera": camera.serialize_state(), 
		}, 
		"entities": entity_entries, 
		"assemblies": assembly_entries, 
	}


func save_to_slot(slot_name: String, world: SandboxWorld, camera: CameraController, registry: EntityRegistry) -> Error:
	last_error_message = ""
	var text: = JSON.stringify(build_save_data(world, camera, registry), "\t", false, true)


	if not _is_save_text(text):
		last_error_message = "serialization produced invalid data"
		return ERR_INVALID_DATA
	var error: = _store.write(get_slot_path(slot_name), text)
	last_error_message = _store.last_error_message
	return error


func load_from_slot(slot_name: String, world: SandboxWorld, camera: CameraController, registry: EntityRegistry) -> LoadResult:
	var result: = LoadResult.new()
	var path: = get_slot_path(slot_name)
	if not _store.exists(path):
		result.status = Status.NOT_FOUND
		result.message = "No save found in slot '%s'." % slot_name
		return result
	var found: = _store.read(path, _is_save_text)
	result.warnings.append_array(found["notes"])
	if str(found["source"]).is_empty():
		result.status = Status.CORRUPT
		result.message = "The save in slot '%s' is damaged and no usable backup exists." % slot_name
		return result
	result.recovered_from = found["source"]
	var json: = JSON.new()
	json.parse(found["text"])
	var data: Dictionary = json.data
	var migration: = SaveMigrator.migrate(data, FORMAT_VERSION)
	if not migration["ok"]:
		var version: = int(data.get("format_version", 0)) if data.get("format_version") is float or data.get("format_version") is int else 0
		result.status = Status.UNSUPPORTED_VERSION if version > FORMAT_VERSION else Status.MALFORMED
		result.message = migration["error"]
		result.errors.append(migration["error"])
		return result
	var applied: = apply_save_data(migration["data"], world, camera, registry)
	applied.recovered_from = result.recovered_from
	applied.warnings = result.warnings + applied.warnings
	if not result.warnings.is_empty() and applied.ok:
		applied.status = Status.OK_WITH_WARNINGS
		applied.message += " (%s)" % "; ".join(result.warnings)
	return applied




func apply_save_data(data: Dictionary, world: SandboxWorld, camera: CameraController, registry: EntityRegistry) -> LoadResult:
	var result: = LoadResult.new()
	var plan: = SaveValidator.validate(data, registry)
	result.warnings = plan.warnings.duplicate()
	result.skipped_types = plan.missing_types.duplicate()
	if not plan.is_valid():
		result.status = Status.MALFORMED
		result.errors = plan.errors
		result.message = "The save is invalid and was not loaded (the current world was kept): %s" % plan.errors[0]
		return result

	var snapshot: = build_save_data(world, camera, registry)
	var failure: = _reconstruct(plan, world, camera, result)
	result.world_modified = true
	if not failure.is_empty():

		var restore_plan: = SaveValidator.validate(snapshot, registry)
		_reconstruct(restore_plan, world, camera, LoadResult.new())
		result.status = Status.RECONSTRUCTION_FAILED
		result.errors.append(failure)
		result.message = "Loading failed (%s); the previous world was restored." % failure
		return result

	result.ok = true
	result.status = Status.OK if plan.missing_types.is_empty() and plan.warnings.is_empty() else Status.OK_WITH_WARNINGS
	result.message = "Loaded %d object(s)." % world.get_object_count()
	if not plan.missing_types.is_empty():
		result.message += " Skipped unknown types: %s" % ", ".join(plan.missing_types)
	return result




func _reconstruct(plan: SaveValidator.LoadPlan, world: SandboxWorld, camera: CameraController, result: LoadResult) -> String:
	world.clear_entities()
	var parts_by_assembly: = {}
	for entry in plan.entities:
		var entity: = world.spawn_entity(entry.type_id, entry.position, entry.rotation, entry.uid)
		if entity == null:
			return "could not create '%s' (uid %d)" % [entry.type_id, entry.uid]
		EntitySerializer.apply_state(entity, entry.data, true)
		result.loaded_count += 1
		if entry.assembly_uid > 0:
			if not parts_by_assembly.has(entry.assembly_uid):
				parts_by_assembly[entry.assembly_uid] = {}
			parts_by_assembly[entry.assembly_uid][entry.part_name] = entity
	for assembly_entry in plan.assemblies:
		var parts: Dictionary = parts_by_assembly.get(assembly_entry.uid, {})
		if parts.is_empty():
			result.warnings.append("assembly %d has no surviving parts and was skipped" % assembly_entry.uid)
			continue
		var assembly: = world.restore_assembly(assembly_entry.type_id, assembly_entry.uid, parts, 
			assembly_entry.broken_joints, assembly_entry.controller_state)
		if assembly == null:
			return "could not rebuild assembly %d ('%s')" % [assembly_entry.uid, assembly_entry.type_id]
		result.assembly_count += 1
	world.set_next_uid(maxi(world.get_next_uid(), plan.next_uid))
	if not plan.camera.is_empty():
		camera.apply_state(plan.camera)
	return ""



func _is_save_text(text: String) -> bool:
	if text.is_empty() or text.length() > MAX_FILE_BYTES:
		return false


	var json: = JSON.new()
	if json.parse(text) != OK:
		return false
	return json.data is Dictionary and json.data.get("format") == FORMAT_ID
