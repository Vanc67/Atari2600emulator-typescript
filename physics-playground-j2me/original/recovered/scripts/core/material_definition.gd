class_name MaterialDefinition
extends RefCounted






var id: String
var source_mod: String
var display_name: String

var density: float
var friction: float
var bounce: float

var durability: float

var thermal_conductivity: float

var electrical_conductivity: float


var penetration_resistance: float

var ricochet_chance: float

var impact_effect: String

var impact_sound: String

var flammability: float

var ignition_temperature: float
var tags: PackedStringArray

var physics_material: PhysicsMaterial


static func from_dict(data: Dictionary, mod_id: String, reader: DataReader) -> MaterialDefinition:
	var material: = MaterialDefinition.new()
	var local_id: = reader.read_string(data, "id", "", true)
	if not local_id.is_empty() and not NamespacedId.is_valid_segment(local_id):
		reader.error("id '%s' may only contain lowercase letters, digits and underscores" % local_id)
	material.id = NamespacedId.make(mod_id, local_id)
	material.source_mod = mod_id
	material.display_name = reader.read_string(data, "display_name", local_id.capitalize())
	material.density = reader.read_float(data, "density", 100.0, 0.1, 100000.0)
	material.friction = reader.read_float(data, "friction", 0.5, 0.0, 1.0)
	material.bounce = reader.read_float(data, "bounce", 0.0, 0.0, 1.0)
	material.durability = reader.read_float(data, "durability", 1.0, 0.0, 1000.0)
	material.thermal_conductivity = reader.read_float(data, "thermal_conductivity", 0.0, 0.0, 1.0)
	material.electrical_conductivity = reader.read_float(data, "electrical_conductivity", 0.0, 0.0, 1.0)
	material.tags = reader.read_string_array(data, "tags")
	material.penetration_resistance = reader.read_float(data, "penetration_resistance", 1.0, 0.0, 1000.0)
	material.ricochet_chance = reader.read_float(data, "ricochet_chance", 0.0, 0.0, 1.0)
	material.impact_effect = reader.read_string(data, "impact_effect", "dust")
	material.impact_sound = NamespacedId.qualify(reader.read_string(data, "impact_sound", "core:impact_generic"), mod_id)
	material.flammability = reader.read_float(data, "flammability", 0.0, 0.0, 1.0)
	material.ignition_temperature = reader.read_float(data, "ignition_temperature", -1.0, -1.0, 100000.0)

	material.physics_material = PhysicsMaterial.new()
	material.physics_material.friction = material.friction
	material.physics_material.bounce = material.bounce
	return material
