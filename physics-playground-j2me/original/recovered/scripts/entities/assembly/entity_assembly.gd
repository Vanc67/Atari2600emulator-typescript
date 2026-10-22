class_name EntityAssembly
extends Node2D


















signal joint_broken(joint: AssemblyJoint)
signal state_changed(state_name: String)


const STRAIN_TICKS_TO_BREAK: = 3

const INSTANT_BREAK_FACTOR: = 2.5

const DRAG_PROTECTION_MULTIPLIER: = 2.5

const MIN_JOINT_STRENGTH: = 0.35





const LIMIT_KP: = 1600.0
const LIMIT_KD: = 80.0
const LIMIT_KI: = 30000.0
const LIMIT_MAX_INTEGRAL: = 6000.0


const LIMIT_MARGIN: = 0.12
const LIMIT_INTEGRAL_DECAY: = 0.9


const JOINT_BIAS: = 0.6

var assembly_uid: = 0
var definition: EntityDefinition
var controller: AssemblyController

var connectivity_version: = 0

var _parts: Dictionary = {}
var _joints: Array[AssemblyJoint] = []
var _drag_holds: = 0


func setup(assembly_definition: EntityDefinition, uid: int, assembly_controller: AssemblyController) -> void :
	definition = assembly_definition
	assembly_uid = uid
	name = "%s_%d" % [NamespacedId.get_local_name(definition.id), uid]
	controller = assembly_controller





func attach_parts(parts: Dictionary, broken_joint_names: PackedStringArray) -> void :
	for part_name: String in parts:
		var part: SandboxEntity = parts[part_name]
		var part_definition: = definition.assembly.get_part(part_name)
		if part_definition == null:
			continue
		_parts[part_name] = part
		part.assembly = self
		part.assembly_part_name = part_name
		part.z_index = part_definition.z_index
		var health: = part.get_health()
		if health:
			health.damaged.connect(_on_part_damaged.bind(part_name))
			health.health_changed.connect(_on_part_health_changed.bind(part_name))



	var part_list: = get_parts()
	for i in part_list.size():
		for j in range(i + 1, part_list.size()):
			part_list[i].add_collision_exception_with(part_list[j])

	for joint_definition in definition.assembly.joints:
		var joint: = AssemblyJoint.new()
		joint.definition = joint_definition
		joint.body_a = _parts.get(joint_definition.parent) as SandboxEntity
		joint.body_b = _parts.get(joint_definition.child) as SandboxEntity
		_joints.append(joint)
		if joint.body_a == null or joint.body_b == null or broken_joint_names.has(joint_definition.name):
			_mark_broken(joint)
		else:
			_create_pin(joint)
			_update_joint_strength(joint)

	controller.setup(self, definition.assembly.controller_params)
	for part in part_list:
		controller.apply_state_to_part(part)
	controller.evaluate()




func get_display_name() -> String:
	return "%s #%d" % [definition.display_name, assembly_uid]


func get_part(part_name: String) -> SandboxEntity:
	return _parts.get(part_name) as SandboxEntity


func get_parts() -> Array[SandboxEntity]:
	var result: Array[SandboxEntity] = []
	result.assign(_parts.values())
	return result


func get_part_count() -> int:
	return _parts.size()


func get_part_display_name(part_name: String) -> String:
	var part_definition: = definition.assembly.get_part(part_name)
	return part_definition.display_name if part_definition else part_name


func get_joints() -> Array[AssemblyJoint]:
	return _joints.duplicate()


func get_joint(joint_name: String) -> AssemblyJoint:
	for joint in _joints:
		if joint.definition.name == joint_name:
			return joint
	return null


func get_broken_joint_names() -> PackedStringArray:
	var names: = PackedStringArray()
	for joint in _joints:
		if joint.broken:
			names.append(joint.definition.name)
	return names



func is_part_attached(part: SandboxEntity) -> bool:
	for joint in _joints:
		if not joint.broken and joint.connects(part):
			return true
	return false



func get_connected_parts(part: SandboxEntity) -> Array[SandboxEntity]:
	var result: Array[SandboxEntity] = [part]
	var index: = 0
	while index < result.size():
		var current: = result[index]
		for joint in _joints:
			if joint.broken or not joint.connects(current):
				continue
			var other: = joint.get_other(current)
			if not result.has(other):
				result.append(other)
		index += 1
	return result




func get_total_health_fraction() -> float:
	var total_max: = definition.assembly.get_total_max_health()
	if total_max <= 0.0:
		return 1.0
	var total_current: = 0.0
	for part in _parts.values():
		var health: = (part as SandboxEntity).get_health()
		if health:
			total_current += health.current_health
	return clampf(total_current / total_max, 0.0, 1.0)


func get_inspector_lines(part: SandboxEntity) -> PackedStringArray:
	var connection: = "attached" if is_part_attached(part) else "detached"
	var lines: = PackedStringArray(["Body part: %s (%s)" % [get_part_display_name(part.assembly_part_name), connection]])
	lines.append_array(controller.get_inspector_lines())
	var broken: = get_broken_joint_names()
	if not broken.is_empty():
		lines.append("Broken joints: %s" % ", ".join(broken))
	return lines




func break_joint(joint: AssemblyJoint) -> void :
	if joint.broken:
		return
	_mark_broken(joint)
	joint_broken.emit(joint)
	controller.on_joint_broken(joint)


func break_joint_by_name(joint_name: String) -> void :
	var joint: = get_joint(joint_name)
	if joint:
		break_joint(joint)




func heal_all() -> void :
	for part in get_parts():
		var health: = part.get_health()
		if health:
			health.heal_full()
	controller.revive()



func on_part_removed(part: SandboxEntity) -> void :
	if _parts.get(part.assembly_part_name) != part:
		return
	for joint in _joints:
		if joint.connects(part) and not joint.broken:
			_mark_broken(joint)
	_parts.erase(part.assembly_part_name)
	controller.on_part_removed(part.assembly_part_name)




func begin_drag_protection() -> void :
	_drag_holds += 1


func end_drag_protection() -> void :
	_drag_holds = maxi(_drag_holds - 1, 0)




func _physics_process(delta: float) -> void :
	var protection: = DRAG_PROTECTION_MULTIPLIER if _drag_holds > 0 else 1.0
	for joint in _joints:
		if joint.broken:
			continue
		var a: = joint.body_a
		var b: = joint.body_b
		if (a.sleeping and b.sleeping) or a.freeze or b.freeze:
			joint.strain_ticks = 0
			continue
		if joint.definition.has_angle_limits:
			_apply_angle_limit(joint, a, b, delta)
		_check_strain(joint, a, b, protection)





func _apply_angle_limit(joint: AssemblyJoint, a: SandboxEntity, b: SandboxEntity, delta: float) -> void :
	var limits: = joint.definition
	var relative: = wrapf(b.rotation - a.rotation - limits.rest_angle, - PI, PI)
	var depth: = relative - limits.upper_angle
	var inward: = -1.0
	if depth <= - LIMIT_MARGIN:
		depth = limits.lower_angle - relative
		inward = 1.0
		if depth <= - LIMIT_MARGIN:

			joint.limit_integral *= LIMIT_INTEGRAL_DECAY
			return



	var error: = depth + LIMIT_MARGIN * 0.5
	joint.limit_integral = clampf(joint.limit_integral + LIMIT_KI * error * delta, 0.0, LIMIT_MAX_INTEGRAL)
	var outward_speed: = - (b.angular_velocity - a.angular_velocity) * inward

	var acceleration: = maxf(LIMIT_KP * maxf(error, 0.0) + LIMIT_KD * outward_speed + joint.limit_integral, 0.0)
	if acceleration <= 0.0:
		return
	var torque: = inward * acceleration * joint.effective_inertia
	b.apply_torque(torque)
	a.apply_torque( - torque)






func _check_strain(joint: AssemblyJoint, a: SandboxEntity, b: SandboxEntity, protection: float) -> void :
	var strain: = (a.global_transform * joint.definition.local_anchor_parent).distance_to(
		b.global_transform * joint.definition.local_anchor_child)
	joint.last_strain = strain
	joint.peak_strain = maxf(joint.peak_strain, strain)
	var threshold: = joint.definition.break_strain * joint.strength * protection
	if strain <= threshold:
		joint.strain_ticks = 0
		return
	joint.strain_ticks += 1
	if joint.strain_ticks >= STRAIN_TICKS_TO_BREAK or strain > threshold * INSTANT_BREAK_FACTOR:
		break_joint(joint)


func _create_pin(joint: AssemblyJoint) -> void :
	var a: = joint.body_a
	var b: = joint.body_b
	var pin: = PinJoint2D.new()
	pin.name = "Joint_" + joint.definition.name


	pin.position = a.global_transform * joint.definition.local_anchor_parent
	pin.node_a = a.get_path()
	pin.node_b = b.get_path()
	pin.disable_collision = true
	pin.softness = 0.0
	pin.bias = JOINT_BIAS
	add_child(pin)
	joint.pin = pin
	var inertia_a: = a.definition.compute_inertia(a.mass)
	var inertia_b: = b.definition.compute_inertia(b.mass)
	joint.effective_inertia = (inertia_a * inertia_b) / (inertia_a + inertia_b)


func _mark_broken(joint: AssemblyJoint) -> void :
	var was_connected: = not joint.broken and joint.pin != null
	joint.broken = true
	joint.strain_ticks = 0
	if joint.pin:

		remove_child(joint.pin)
		joint.pin.queue_free()
		joint.pin = null
	if joint.body_a and is_instance_valid(joint.body_a):
		joint.body_a.visual.add_wound(joint.definition.local_anchor_parent)
	if joint.body_b and is_instance_valid(joint.body_b):
		joint.body_b.visual.add_wound(joint.definition.local_anchor_child)
	if was_connected:
		connectivity_version += 1


func _update_joint_strength(joint: AssemblyJoint) -> void :
	var health: = joint.body_b.get_health() if joint.body_b else null
	var fraction: = health.get_health_fraction() if health else 1.0
	joint.strength = lerpf(MIN_JOINT_STRENGTH, 1.0, fraction)


func _on_part_damaged(info: DamageInfo, part_name: String) -> void :

	if info.can_tear():
		for joint in _joints:
			if joint.broken or joint.definition.child != part_name:
				continue
			var tear_speed: = joint.definition.break_impact_speed
			if tear_speed > 0.0 and info.impact_speed >= tear_speed * joint.strength:
				break_joint(joint)
	controller.on_part_damaged(part_name, info)


func _on_part_health_changed(_current: float, _maximum: float, part_name: String) -> void :
	for joint in _joints:
		if not joint.broken and joint.definition.child == part_name:
			_update_joint_strength(joint)
	controller.evaluate()
