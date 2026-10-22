class_name NamespacedId
extends RefCounted






const SEPARATOR: = ":"
const BUILTIN_NAMESPACE: = "core"

static var _segment_pattern: RegEx



static func is_valid_segment(segment: String) -> bool:
	if _segment_pattern == null:
		_segment_pattern = RegEx.create_from_string("^[a-z0-9_]+$")
	return _segment_pattern.search(segment) != null


static func is_valid(id: String) -> bool:
	var parts: = id.split(SEPARATOR)
	return parts.size() == 2 and is_valid_segment(parts[0]) and is_valid_segment(parts[1])


static func make(namespace_name: String, local_name: String) -> String:
	return namespace_name + SEPARATOR + local_name



static func qualify(id: String, default_namespace: String) -> String:
	if id.contains(SEPARATOR):
		return id
	return make(default_namespace, id)


static func get_namespace(id: String) -> String:
	return id.get_slice(SEPARATOR, 0)


static func get_local_name(id: String) -> String:
	if id.contains(SEPARATOR):
		return id.get_slice(SEPARATOR, 1)
	return id
