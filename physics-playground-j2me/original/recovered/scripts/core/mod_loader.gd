class_name ModLoader
extends RefCounted












const BUILTIN_MODS_PATH: = "res://mods"
const USER_MODS_PATH: = "user://mods"
const MANIFEST_FILE_NAME: = "mod.json"
const MATERIALS_FOLDER: = "materials"
const ENTITIES_FOLDER: = "entities"
const PROJECTILES_FOLDER: = "projectiles"
const SOUNDS_FOLDER: = "sounds"



const MAX_MODS: = 64
const MAX_FILE_BYTES: = 1024 * 1024
const MAX_FILES_PER_FOLDER: = 500
const MAX_DEFINITIONS_PER_MOD: = 1000

const MAX_AUDIO_BYTES: = 32 * 1024 * 1024

var search_paths: PackedStringArray
var errors: PackedStringArray = []
var warnings: PackedStringArray = []

var _definition_counts: Dictionary = {}
var _audio_bytes: = 0


func _init(mod_search_paths: Array = []) -> void :
	if mod_search_paths.is_empty():
		search_paths = PackedStringArray([BUILTIN_MODS_PATH, USER_MODS_PATH])
	else:
		search_paths = PackedStringArray(mod_search_paths)


func load_all(registry: EntityRegistry) -> void :
	errors.clear()
	warnings.clear()
	_definition_counts.clear()
	_audio_bytes = 0
	var manifests: = discover_mods()
	for manifest in manifests:
		registry.register_mod(manifest)
	for manifest in manifests:
		_load_materials(manifest, registry)
		_load_simple(manifest, registry, PROJECTILES_FOLDER, "projectiles", "projectile")
		_load_simple(manifest, registry, SOUNDS_FOLDER, "sounds", "sound")
	for manifest in manifests:
		_load_entities(manifest, registry)
	_link_assemblies(registry)
	_link_component_references(registry)


func discover_mods() -> Array[ModManifest]:
	var manifests: Array[ModManifest] = []
	var seen_ids: = {}
	for root_path in search_paths:
		if root_path.begins_with("user://"):

			DirAccess.make_dir_recursive_absolute(root_path)
		if not DirAccess.dir_exists_absolute(root_path):
			continue
		var folder_names: = DirAccess.get_directories_at(root_path)
		folder_names.sort()
		for folder_name in folder_names:
			var manifest: = _read_manifest(root_path.path_join(folder_name))
			if manifest == null:
				continue
			if seen_ids.has(manifest.id):
				errors.append("%s: duplicate mod id '%s' (already loaded from %s)" % [
					manifest.directory, manifest.id, seen_ids[manifest.id]])
				continue
			if manifest.id == NamespacedId.BUILTIN_NAMESPACE and not manifest.is_builtin:
				errors.append("%s: the mod id 'core' is reserved for built-in content" % manifest.directory)
				continue
			if manifests.size() >= MAX_MODS:
				errors.append("%s: more than %d mods installed; skipped" % [manifest.directory, MAX_MODS])
				continue
			seen_ids[manifest.id] = manifest.directory
			manifests.append(manifest)

	for index in manifests.size():
		if manifests[index].id == NamespacedId.BUILTIN_NAMESPACE:
			var core_manifest: ModManifest = manifests.pop_at(index)
			manifests.push_front(core_manifest)
			break
	return manifests


func _read_manifest(folder: String) -> ModManifest:
	var manifest_path: = folder.path_join(MANIFEST_FILE_NAME)
	if not FileAccess.file_exists(manifest_path):
		warnings.append("Skipping '%s': no %s found" % [folder, MANIFEST_FILE_NAME])
		return null
	var data: Variant = _read_json(manifest_path)
	if data == null:
		return null
	if not data is Dictionary:
		errors.append("%s: manifest must be a JSON object" % manifest_path)
		return null
	var reader: = DataReader.new(manifest_path)
	var manifest: = ModManifest.from_dict(data, folder, reader)
	_collect(reader)
	return null if reader.has_errors() else manifest


func _load_materials(manifest: ModManifest, registry: EntityRegistry) -> void :
	for path in _list_json_files(manifest.directory.path_join(MATERIALS_FOLDER)):
		var data: Variant = _read_json(path)
		if data == null:
			continue
		var entries: = _entries_from(data, "materials")
		for index in entries.size():
			var entry: Variant = entries[index]
			var reader: = DataReader.new(_context(manifest, path, "material", index, entry))
			if not _within_definition_limit(manifest, reader):
				_collect(reader)
				break
			if not entry is Dictionary:
				reader.error("entry must be a JSON object")
				_collect(reader)
				continue
			var material: = MaterialDefinition.from_dict(entry, manifest.id, reader)
			_collect(reader)
			if reader.has_errors():
				continue
			if not registry.register_material(material):
				errors.append("%s: duplicate material id '%s'" % [path, material.id])



func _load_simple(manifest: ModManifest, registry: EntityRegistry, folder: String, 
		list_key: String, kind: String) -> void :
	for path in _list_json_files(manifest.directory.path_join(folder)):
		var data: Variant = _read_json(path)
		if data == null:
			continue
		var entries: = _entries_from(data, list_key)
		for index in entries.size():
			var entry: Variant = entries[index]
			var reader: = DataReader.new(_context(manifest, path, kind, index, entry))
			if not _within_definition_limit(manifest, reader):
				_collect(reader)
				break
			if not entry is Dictionary:
				reader.error("entry must be a JSON object")
				_collect(reader)
				continue
			var registered: = true
			var id: = ""
			if kind == "projectile":
				var projectile: = ProjectileDefinition.from_dict(entry, manifest.id, reader)
				id = projectile.id
				if not reader.has_errors():
					registered = registry.register_projectile(projectile)
			else:
				var sound: = SoundDefinition.from_dict(entry, manifest.id, reader)
				id = sound.id
				var sound_bytes: = int(sound.duration * SoundSynth.MIX_RATE) * 2
				if _audio_bytes + sound_bytes > MAX_AUDIO_BYTES:
					reader.error("synthesized audio budget exceeded (%d MiB total); sound not loaded" % (MAX_AUDIO_BYTES / 1048576))
				if not reader.has_errors():
					registered = registry.register_sound(sound)
					if registered:
						_audio_bytes += sound_bytes
			_collect(reader)
			if not registered:
				errors.append("%s: duplicate %s id '%s'" % [path, kind, id])


func _load_entities(manifest: ModManifest, registry: EntityRegistry) -> void :
	for path in _list_json_files(manifest.directory.path_join(ENTITIES_FOLDER)):
		var data: Variant = _read_json(path)
		if data == null:
			continue
		var entries: = _entries_from(data, "entities")
		for index in entries.size():
			var entry: Variant = entries[index]
			var reader: = DataReader.new(_context(manifest, path, "entity", index, entry))
			if not _within_definition_limit(manifest, reader):
				_collect(reader)
				break
			if not entry is Dictionary:
				reader.error("entry must be a JSON object")
				_collect(reader)
				continue
			var definition: = EntityDefinition.from_dict(entry, manifest.id, reader)
			_validate_entity(definition, registry, reader)
			_collect(reader)
			if reader.has_errors():
				continue
			if not registry.register_entity(definition):
				errors.append("%s: duplicate entity id '%s' (definition rejected)" % [reader.context, definition.id])


func _validate_entity(definition: EntityDefinition, registry: EntityRegistry, reader: DataReader) -> void :
	if definition.is_assembly():
		var controller_type: = definition.assembly.controller_type
		if not controller_type.is_empty() and not AssemblyControllerRegistry.has_type(controller_type):
			reader.warn("unknown assembly controller '%s' (ignored)" % controller_type)
			definition.assembly.controller_type = ""
		return
	if not registry.has_material(definition.material_id):
		reader.error("unknown material '%s'" % definition.material_id)


	ComponentSchemas.validate_components(definition.components, definition.source_mod, registry, reader)
	if not EntityVisual.is_known_style(definition.visual_style):
		reader.warn("unknown visual style '%s' (drawn as 'plain')" % definition.visual_style)




func _link_assemblies(registry: EntityRegistry) -> void :
	for definition in registry.get_assembly_definitions():
		var reader: = DataReader.new("%s (assembly)" % definition.id)
		definition.assembly.link(registry, reader)
		_collect(reader)
		if reader.has_errors():
			registry.unregister_entity(definition.id)




func _link_component_references(registry: EntityRegistry) -> void :
	for definition in registry.get_all_entities():
		var reader: = DataReader.new("%s (components)" % definition.id)
		ComponentRegistry.link_params(definition.components, registry, reader)
		_collect(reader)



func _context(manifest: ModManifest, path: String, kind: String, index: int, entry: Variant) -> String:
	var label: = "%s #%d" % [kind, index]
	if entry is Dictionary and entry.get("id") is String:
		label = "%s '%s'" % [kind, NamespacedId.make(manifest.id, entry["id"])]
	return "mod '%s' · %s · %s" % [manifest.id, path.get_file(), label]


func _within_definition_limit(manifest: ModManifest, reader: DataReader) -> bool:
	var count: int = _definition_counts.get(manifest.id, 0) + 1
	_definition_counts[manifest.id] = count
	if count > MAX_DEFINITIONS_PER_MOD:
		if count == MAX_DEFINITIONS_PER_MOD + 1:
			reader.error("mod exceeds %d definitions; the rest are skipped" % MAX_DEFINITIONS_PER_MOD)
		return false
	return true


func _read_json(path: String) -> Variant:
	var file: = FileAccess.open(path, FileAccess.READ)
	if file == null:
		errors.append("%s: cannot open file (error %d)" % [path, FileAccess.get_open_error()])
		return null
	if file.get_length() > MAX_FILE_BYTES:
		errors.append("%s: file is %d KiB, over the %d KiB limit; skipped" % [path, file.get_length() / 1024, MAX_FILE_BYTES / 1024])
		return null
	var json: = JSON.new()
	if json.parse(file.get_as_text()) != OK:
		errors.append("%s:%d: %s" % [path, json.get_error_line(), json.get_error_message()])
		return null
	return json.data



func _entries_from(data: Variant, list_key: String) -> Array:
	if data is Array:
		return data
	if data is Dictionary:
		if data.has(list_key) and data[list_key] is Array:
			return data[list_key]
		return [data]
	return []


func _list_json_files(folder: String) -> PackedStringArray:
	var paths: = PackedStringArray()
	if not DirAccess.dir_exists_absolute(folder):
		return paths
	var file_names: = DirAccess.get_files_at(folder)
	file_names.sort()
	if file_names.size() > MAX_FILES_PER_FOLDER:
		errors.append("%s: more than %d files; only the first %d are read" % [folder, MAX_FILES_PER_FOLDER, MAX_FILES_PER_FOLDER])
		file_names = file_names.slice(0, MAX_FILES_PER_FOLDER)
	for file_name in file_names:
		if file_name.get_extension().to_lower() == "json":
			paths.append(folder.path_join(file_name))
	return paths


func _collect(reader: DataReader) -> void :
	errors.append_array(reader.errors)
	warnings.append_array(reader.warnings)
