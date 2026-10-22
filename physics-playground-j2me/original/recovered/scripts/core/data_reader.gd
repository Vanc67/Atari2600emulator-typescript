class_name DataReader
extends RefCounted





var context: String
var errors: PackedStringArray = []
var warnings: PackedStringArray = []


func _init(error_context: String) -> void :
	context = error_context


func has_errors() -> bool:
	return not errors.is_empty()


func error(message: String) -> void :
	errors.append("%s: %s" % [context, message])


func warn(message: String) -> void :
	warnings.append("%s: %s" % [context, message])


func read_string(data: Dictionary, key: String, default_value: = "", required: = false) -> String:
	if not data.has(key):
		if required:
			error("missing required field '%s'" % key)
		return default_value
	var value: Variant = data[key]
	if value is String:
		return value
	error("field '%s' must be a string" % key)
	return default_value


func read_float(data: Dictionary, key: String, default_value: float, 
		min_value: = - INF, max_value: = INF) -> float:
	if not data.has(key):
		return default_value
	var value: Variant = data[key]
	if not _is_number(value):
		error("field '%s' must be a number" % key)
		return default_value
	var number: = float(value)
	if number < min_value or number > max_value:
		error("field '%s' = %s is outside the allowed range [%s, %s]" % [key, number, min_value, max_value])
		return clampf(number, min_value, max_value)
	return number


func read_int(data: Dictionary, key: String, default_value: int) -> int:
	if not data.has(key):
		return default_value
	var value: Variant = data[key]
	if not _is_number(value):
		error("field '%s' must be an integer" % key)
		return default_value
	return int(value)


func read_bool(data: Dictionary, key: String, default_value: bool) -> bool:
	if not data.has(key):
		return default_value
	var value: Variant = data[key]
	if value is bool:
		return value
	error("field '%s' must be true or false" % key)
	return default_value


func read_vector2(data: Dictionary, key: String, default_value: Vector2) -> Vector2:
	if not data.has(key):
		return default_value
	var value: Variant = data[key]
	if value is Array and value.size() == 2 and _is_number(value[0]) and _is_number(value[1]):
		return Vector2(float(value[0]), float(value[1]))
	error("field '%s' must be an array of two numbers" % key)
	return default_value


func read_color(data: Dictionary, key: String, default_value: Color) -> Color:
	if not data.has(key):
		return default_value
	var value: Variant = data[key]
	if value is String and Color.html_is_valid(value):
		return Color.html(value)
	error("field '%s' must be a hex color string such as \"#8a6a44\"" % key)
	return default_value


func read_dictionary(data: Dictionary, key: String) -> Dictionary:
	if not data.has(key):
		return {}
	var value: Variant = data[key]
	if value is Dictionary:
		return value
	error("field '%s' must be an object" % key)
	return {}


func read_string_array(data: Dictionary, key: String) -> PackedStringArray:
	var result: = PackedStringArray()
	if not data.has(key):
		return result
	var value: Variant = data[key]
	if not value is Array:
		error("field '%s' must be an array of strings" % key)
		return result
	for item: Variant in value:
		if item is String:
			result.append(item)
		else:
			error("field '%s' must contain only strings" % key)
	return result



static func _is_number(value: Variant) -> bool:
	return (value is float and is_finite(value)) or value is int
