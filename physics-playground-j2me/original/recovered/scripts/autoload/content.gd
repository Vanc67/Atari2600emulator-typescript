extends Node




signal content_reloaded

var registry: = EntityRegistry.new()
var load_errors: PackedStringArray = []
var load_warnings: PackedStringArray = []


func _init() -> void :
	reload()


func reload() -> void :
	registry.clear()
	var loader: = ModLoader.new()
	loader.load_all(registry)
	load_errors = loader.errors
	load_warnings = loader.warnings
	for message in load_warnings:
		push_warning("[Content] " + message)
	for message in load_errors:
		push_error("[Content] " + message)
	print("[Content] Loaded %d mod(s), %d entity definition(s), %d error(s)." % [
		registry.get_mods().size(), registry.get_entity_ids().size(), load_errors.size()])
	content_reloaded.emit()
