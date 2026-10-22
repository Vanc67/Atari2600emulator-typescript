class_name AssemblyDefinition
extends RefCounted











class PartDefinition:
	var name: String
	var display_name: String
	var entity_id: String
	var position: = Vector2.ZERO
	var rotation: = 0.0
	var z_index: = 0

	var definition: EntityDefinition
	var max_health: = 0.0


class JointDefinition:
	var name: String
	var parent: String
	var child: String

	var anchor: = Vector2.ZERO
	var has_angle_limits: = false

	var lower_angle: = 0.0
	var upper_angle: = 0.0

	var break_strain: = 12.0


	var break_impact_speed: = 0.0

	var rest_angle: = 0.0
	var local_anchor_parent: = Vector2.ZERO
	var local_anchor_child: = Vector2.ZERO



const MAX_PARTS: = 32
const MAX_JOINTS: = 64

var parts: Array[PartDefinition] = []
var joints: Array[JointDefinition] = []
var controller_type: = ""
var controller_params: Dictionary = {}



var spawn_nudge: = 0.0

var total_mass: = 0.0
var bounds: = Rect2()

var _parts_by_name: Dictionary = {}


static func from_dict(data: Dictionary, mod_id: String, reader: DataReader) -> AssemblyDefinition:
	var assembly: = AssemblyDefinition.new()
	var part_entries: Variant = data.get("parts", [])
	if not part_entries is Array or part_entries.is_empty():
		reader.error("assembly needs a non-empty 'parts' array")
		part_entries = []
	elif part_entries.size() > MAX_PARTS:
		reader.error("assembly has %d parts; the limit is %d" % [part_entries.size(), MAX_PARTS])
		part_entries = []
	for entry: Variant in part_entries:
		if not entry is Dictionary:
			reader.error("assembly parts must be objects")
			continue
		var part: = PartDefinition.new()
		part.name = reader.read_string(entry, "name", "", true)
		if assembly._parts_by_name.has(part.name):
			reader.error("duplicate part name '%s'" % part.name)
			continue
		part.display_name = reader.read_string(entry, "display_name", part.name.capitalize())
		part.entity_id = NamespacedId.qualify(reader.read_string(entry, "entity", "", true), mod_id)
		part.position = reader.read_vector2(entry, "position", Vector2.ZERO)
		part.rotation = deg_to_rad(reader.read_float(entry, "rotation", 0.0, -360.0, 360.0))
		part.z_index = reader.read_int(entry, "z_index", 0)
		assembly.parts.append(part)
		assembly._parts_by_name[part.name] = part

	var joint_entries: Variant = data.get("joints", [])
	if not joint_entries is Array:
		reader.error("'joints' must be an array")
		joint_entries = []
	elif joint_entries.size() > MAX_JOINTS:
		reader.error("assembly has %d joints; the limit is %d" % [joint_entries.size(), MAX_JOINTS])
		joint_entries = []
	var joint_names: = {}
	for entry: Variant in joint_entries:
		if not entry is Dictionary:
			reader.error("assembly joints must be objects")
			continue
		var joint: = JointDefinition.new()
		joint.name = reader.read_string(entry, "name", "", true)
		if joint_names.has(joint.name):
			reader.error("duplicate joint name '%s'" % joint.name)
			continue
		joint_names[joint.name] = true
		joint.parent = reader.read_string(entry, "parent", "", true)
		joint.child = reader.read_string(entry, "child", "", true)
		for part_name in [joint.parent, joint.child]:
			if not assembly._parts_by_name.has(part_name):
				reader.error("joint '%s' references unknown part '%s'" % [joint.name, part_name])
		if joint.parent == joint.child:
			reader.error("joint '%s' connects a part to itself" % joint.name)
		joint.anchor = reader.read_vector2(entry, "anchor", Vector2.ZERO)
		if entry.has("angle_limits") and entry["angle_limits"] != null:
			var limits: = reader.read_vector2(entry, "angle_limits", Vector2.ZERO)
			joint.has_angle_limits = true
			joint.lower_angle = deg_to_rad(minf(limits.x, limits.y))
			joint.upper_angle = deg_to_rad(maxf(limits.x, limits.y))
		joint.break_strain = reader.read_float(entry, "break_strain", 12.0, 0.5, 10000.0)
		joint.break_impact_speed = reader.read_float(entry, "break_impact_speed", 0.0, 0.0, 10000.0)
		assembly.joints.append(joint)

	assembly.spawn_nudge = reader.read_float(data, "spawn_nudge", 0.0, 0.0, 1000.0)
	var controller: = reader.read_dictionary(data, "controller")
	assembly.controller_type = reader.read_string(controller, "type", "")
	assembly.controller_params = controller.duplicate(true)
	return assembly



func link(registry: EntityRegistry, reader: DataReader) -> void :
	total_mass = 0.0
	var has_bounds: = false
	for part in parts:
		part.definition = registry.get_entity_definition(part.entity_id)
		if part.definition == null:
			reader.error("part '%s' references unknown entity '%s'" % [part.name, part.entity_id])
			continue
		if part.definition.is_assembly():
			reader.error("part '%s' cannot itself be an assembly" % part.name)
			continue
		var material: = registry.get_material(part.definition.material_id)
		total_mass += part.definition.compute_mass(material)
		var health_params: Variant = part.definition.components.get("health")
		if health_params is Dictionary:
			part.max_health = float(health_params.get("max_health", 100.0)) * (material.durability if material else 1.0)
		var part_bounds: = _rotated_bounds(part.definition.get_bounding_size(), part.position, part.rotation)
		bounds = part_bounds if not has_bounds else bounds.merge(part_bounds)
		has_bounds = true

	for joint in joints:
		var parent: = get_part(joint.parent)
		var child: = get_part(joint.child)
		if parent == null or child == null:
			continue
		joint.rest_angle = child.rotation - parent.rotation
		joint.local_anchor_parent = Transform2D(parent.rotation, parent.position).affine_inverse() * joint.anchor
		joint.local_anchor_child = Transform2D(child.rotation, child.position).affine_inverse() * joint.anchor


func get_part(part_name: String) -> PartDefinition:
	return _parts_by_name.get(part_name) as PartDefinition


func get_joint(joint_name: String) -> JointDefinition:
	for joint in joints:
		if joint.name == joint_name:
			return joint
	return null


func get_total_max_health() -> float:
	var total: = 0.0
	for part in parts:
		total += part.max_health
	return total


static func _rotated_bounds(extent: Vector2, center: Vector2, angle: float) -> Rect2:
	var transform: = Transform2D(angle, center)
	var half: = extent * 0.5
	var result: = Rect2(transform * - half, Vector2.ZERO)
	for corner in [Vector2(half.x, - half.y), half, Vector2( - half.x, half.y)]:
		result = result.expand(transform * corner)
	return result
