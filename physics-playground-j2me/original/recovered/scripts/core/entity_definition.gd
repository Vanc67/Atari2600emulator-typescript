class_name EntityDefinition
extends RefCounted










const SHAPE_RECTANGLE: = "rectangle"
const SHAPE_CIRCLE: = "circle"

const SHAPE_CAPSULE: = "capsule"

var id: String
var source_mod: String
var display_name: String
var description: String
var category: String
var tags: PackedStringArray


var hidden: = false
var material_id: String

var shape_type: String = SHAPE_RECTANGLE

var size: = Vector2(64, 64)

var radius: = 24.0

var mass_override: = 0.0

var linear_damp: = -1.0
var angular_damp: = -1.0

var center_of_mass: = Vector2.INF

var visual_style: = "plain"
var fill_color: = Color.GRAY
var outline_color: = Color.DIM_GRAY
var detail_color: = Color.DARK_GRAY

var damage_color: = Color.BLACK


var components: Dictionary = {}

var properties: Dictionary = {}

var assembly: AssemblyDefinition = null

var _shape: Shape2D = null


static func from_dict(data: Dictionary, mod_id: String, reader: DataReader) -> EntityDefinition:
	var definition: = EntityDefinition.new()
	var local_id: = reader.read_string(data, "id", "", true)
	if not local_id.is_empty() and not NamespacedId.is_valid_segment(local_id):
		reader.error("id '%s' may only contain lowercase letters, digits and underscores" % local_id)
	definition.id = NamespacedId.make(mod_id, local_id)
	definition.source_mod = mod_id
	definition.display_name = reader.read_string(data, "display_name", local_id.capitalize())
	definition.description = reader.read_string(data, "description", "")
	definition.category = reader.read_string(data, "category", "Misc")
	definition.tags = reader.read_string_array(data, "tags")
	definition.hidden = reader.read_bool(data, "hidden", false)
	definition.properties = reader.read_dictionary(data, "properties").duplicate(true)

	if data.has("assembly"):
		definition.assembly = AssemblyDefinition.from_dict(reader.read_dictionary(data, "assembly"), mod_id, reader)
		return definition

	var material_reference: = reader.read_string(data, "material", "", true)
	definition.material_id = NamespacedId.qualify(material_reference, mod_id)

	var shape: = reader.read_dictionary(data, "shape")
	definition.shape_type = reader.read_string(shape, "type", SHAPE_RECTANGLE)
	match definition.shape_type:
		SHAPE_RECTANGLE:
			definition.size = reader.read_vector2(shape, "size", Vector2(64, 64))
			if definition.size.x < 4.0 or definition.size.y < 4.0\
			or definition.size.x > 4000.0 or definition.size.y > 4000.0:
				reader.error("rectangle size must be between 4 and 4000 pixels per side")
				definition.size = definition.size.clamp(Vector2(4, 4), Vector2(4000, 4000))
		SHAPE_CIRCLE:
			definition.radius = reader.read_float(shape, "radius", 24.0, 2.0, 2000.0)
		SHAPE_CAPSULE:
			definition.radius = reader.read_float(shape, "radius", 8.0, 2.0, 1000.0)
			var height: = reader.read_float(shape, "height", 40.0, 4.0, 4000.0)
			if height < definition.radius * 2.0:
				reader.error("capsule height must be at least twice its radius")
				height = definition.radius * 2.0
			definition.size = Vector2(definition.radius * 2.0, height)
		_:
			reader.error("unknown shape type '%s' (expected 'rectangle', 'circle' or 'capsule')" % definition.shape_type)

	definition.mass_override = reader.read_float(data, "mass", 0.0, 0.0, 100000.0)

	var physics: = reader.read_dictionary(data, "physics")
	definition.linear_damp = reader.read_float(physics, "linear_damp", -1.0, -1.0, 100.0)
	definition.angular_damp = reader.read_float(physics, "angular_damp", -1.0, -1.0, 100.0)
	definition.center_of_mass = reader.read_vector2(physics, "center_of_mass", Vector2.INF)

	var visual: = reader.read_dictionary(data, "visual")
	definition.visual_style = reader.read_string(visual, "style", "plain")
	definition.fill_color = reader.read_color(visual, "fill_color", Color(0.55, 0.55, 0.55))
	definition.outline_color = reader.read_color(visual, "outline_color", definition.fill_color.darkened(0.45))
	definition.detail_color = reader.read_color(visual, "detail_color", definition.fill_color.darkened(0.2))
	definition.damage_color = reader.read_color(visual, "damage_color", definition.fill_color.darkened(0.65))

	var component_data: = reader.read_dictionary(data, "components")
	for component_type: Variant in component_data:
		var params: Variant = component_data[component_type]
		if params is Dictionary:
			definition.components[String(component_type)] = params.duplicate(true)
		else:
			reader.error("component '%s' must map to an object of parameters" % component_type)
	return definition


func is_assembly() -> bool:
	return assembly != null



func get_shape() -> Shape2D:
	if _shape == null:
		match shape_type:
			SHAPE_CIRCLE:
				var circle: = CircleShape2D.new()
				circle.radius = radius
				_shape = circle
			SHAPE_CAPSULE:
				var capsule: = CapsuleShape2D.new()
				capsule.radius = radius
				capsule.height = size.y
				_shape = capsule
			_:
				var rectangle: = RectangleShape2D.new()
				rectangle.size = size
				_shape = rectangle
	return _shape


func is_circle() -> bool:
	return shape_type == SHAPE_CIRCLE


func is_capsule() -> bool:
	return shape_type == SHAPE_CAPSULE



func get_bounding_size() -> Vector2:
	if is_assembly():
		return assembly.bounds.size
	if is_circle():
		return Vector2.ONE * radius * 2.0
	return size



func get_bounding_radius() -> float:
	if is_assembly():
		return assembly.bounds.size.length() * 0.5
	if is_circle():
		return radius
	if is_capsule():
		return size.y * 0.5
	return size.length() * 0.5


func get_area_square_meters() -> float:
	match shape_type:
		SHAPE_CIRCLE:
			return WorldUnits.pixel_area_to_square_meters(PI * radius * radius)
		SHAPE_CAPSULE:
			var straight: = maxf(size.y - radius * 2.0, 0.0)
			return WorldUnits.pixel_area_to_square_meters(PI * radius * radius + straight * radius * 2.0)
	return WorldUnits.pixel_area_to_square_meters(size.x * size.y)



func get_min_thickness() -> float:
	if is_circle():
		return radius * 2.0
	return minf(size.x, size.y)


func compute_mass(material: MaterialDefinition) -> float:
	if is_assembly():
		return assembly.total_mass
	if mass_override > 0.0:
		return mass_override
	if material == null:
		return 1.0
	return maxf(get_area_square_meters() * material.density, 0.05)



func compute_inertia(body_mass: float) -> float:
	if is_circle():
		return body_mass * radius * radius * 0.5
	return body_mass * (size.x * size.x + size.y * size.y) / 12.0
