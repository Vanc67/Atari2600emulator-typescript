class_name ProjectileDefinition
extends RefCounted









var id: String
var source_mod: String
var display_name: String

var mass_grams: = 8.0

var speed: = 350.0

var damage: = 30.0

var penetration: = 1.0

var lifetime: = 1.5

var gravity_scale: = 0.15

var drag: = 0.0

var impulse_multiplier: = 2.0
var trail_color: = Color(1.0, 0.85, 0.45, 0.9)
var trail_width: = 2.0

var trail_time: = 0.012


static func from_dict(data: Dictionary, mod_id: String, reader: DataReader) -> ProjectileDefinition:
	var projectile: = ProjectileDefinition.new()
	var local_id: = reader.read_string(data, "id", "", true)
	if not local_id.is_empty() and not NamespacedId.is_valid_segment(local_id):
		reader.error("id '%s' may only contain lowercase letters, digits and underscores" % local_id)
	projectile.id = NamespacedId.make(mod_id, local_id)
	projectile.source_mod = mod_id
	projectile.display_name = reader.read_string(data, "display_name", local_id.capitalize())
	projectile.mass_grams = reader.read_float(data, "mass_grams", 8.0, 0.1, 10000.0)
	projectile.speed = reader.read_float(data, "speed", 350.0, 1.0, 3000.0)
	projectile.damage = reader.read_float(data, "damage", 30.0, 0.0, 100000.0)
	projectile.penetration = reader.read_float(data, "penetration", 1.0, 0.0, 1000.0)
	projectile.lifetime = reader.read_float(data, "lifetime", 1.5, 0.01, 30.0)
	projectile.gravity_scale = reader.read_float(data, "gravity_scale", 0.15, -10.0, 10.0)
	projectile.drag = reader.read_float(data, "drag", 0.0, 0.0, 50.0)
	projectile.impulse_multiplier = reader.read_float(data, "impulse_multiplier", 2.0, 0.0, 100.0)
	projectile.trail_color = reader.read_color(data, "trail_color", projectile.trail_color)
	projectile.trail_width = reader.read_float(data, "trail_width", 2.0, 0.1, 20.0)
	projectile.trail_time = reader.read_float(data, "trail_time", 0.012, 0.0, 0.5)
	return projectile
