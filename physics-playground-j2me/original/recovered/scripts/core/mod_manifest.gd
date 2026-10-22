class_name ModManifest
extends RefCounted



const SUPPORTED_FORMAT: = 1

var id: String
var display_name: String
var version: String
var description: String
var authors: PackedStringArray

var categories: PackedStringArray

var directory: String
var mod_format: int
var is_builtin: bool


static func from_dict(data: Dictionary, directory_path: String, reader: DataReader) -> ModManifest:
	var manifest: = ModManifest.new()
	manifest.directory = directory_path
	manifest.is_builtin = directory_path.begins_with("res://")
	manifest.mod_format = reader.read_int(data, "mod_format", 1)
	if manifest.mod_format > SUPPORTED_FORMAT:
		reader.error("requires mod format %d but this game supports up to %d" % [manifest.mod_format, SUPPORTED_FORMAT])
	manifest.id = reader.read_string(data, "id", "", true)
	if not manifest.id.is_empty() and not NamespacedId.is_valid_segment(manifest.id):
		reader.error("mod id '%s' may only contain lowercase letters, digits and underscores" % manifest.id)
	manifest.display_name = reader.read_string(data, "name", manifest.id)
	manifest.version = reader.read_string(data, "version", "0.0.0")
	manifest.description = reader.read_string(data, "description", "")
	manifest.authors = reader.read_string_array(data, "authors")
	manifest.categories = reader.read_string_array(data, "categories")
	return manifest
