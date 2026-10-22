class_name DragHandle
extends RefCounted


























const NATURAL_FREQUENCY: = 26.0
const DAMPING_RATIO: = 1.0
const MAX_SPRING_ACCELERATION: = 30000.0
const SPIN_DAMPING: = 5.0
const GROUP_FREQUENCY: = 7.0
const GROUP_DAMPING_RATIO: = 1.0
const MAX_GROUP_ACCELERATION: = 2500.0
const SUPPORT_RAMP_TIME: = 0.25

var entity: SandboxEntity

var local_anchor: Vector2
var target_position: Vector2

var _group: Array[SandboxEntity] = []
var _group_mass: = 0.0
var _group_version: = -1
var _support: = 0.0


func _init(grabbed_entity: SandboxEntity, grab_world_position: Vector2) -> void :
	entity = grabbed_entity
	local_anchor = entity.global_transform.affine_inverse() * grab_world_position
	target_position = grab_world_position
	_refresh_group()


func is_valid() -> bool:
	return is_instance_valid(entity) and entity.is_inside_tree()


func get_anchor_world_position() -> Vector2:
	return entity.global_transform * local_anchor



func get_group() -> Array[SandboxEntity]:
	_refresh_group()
	return _group




func update(world_paused: bool, allow_spin: bool, delta: = 1.0 / 120.0) -> void :
	_refresh_group()
	if entity.freeze or world_paused:
		_move_directly(world_paused)
		return

	var state: = PhysicsServer2D.body_get_direct_state(entity.get_rid())
	if state == null or state.inverse_mass <= 0.0:
		return
	var anchor: = state.transform * local_anchor

	var offset: = anchor - state.transform.origin
	var spin: = state.angular_velocity
	var point_velocity: = state.linear_velocity + Vector2( - spin * offset.y, spin * offset.x)
	var effective_mass: = 1.0 / (state.inverse_mass + offset.length_squared() * state.inverse_inertia)
	var error: = target_position - anchor

	var spring_acceleration: = error * (NATURAL_FREQUENCY * NATURAL_FREQUENCY)\
	- point_velocity * (2.0 * DAMPING_RATIO * NATURAL_FREQUENCY)
	var force: = spring_acceleration.limit_length(MAX_SPRING_ACCELERATION) * effective_mass

	if _group.size() > 1:
		_support = minf(_support + delta / SUPPORT_RAMP_TIME, 1.0)
		var momentum: = Vector2.ZERO
		for part in _group:
			momentum += part.linear_velocity * part.mass
		var group_velocity: = momentum / _group_mass
		var group_acceleration: = error * (GROUP_FREQUENCY * GROUP_FREQUENCY)\
		- group_velocity * (2.0 * GROUP_DAMPING_RATIO * GROUP_FREQUENCY)
		force += group_acceleration.limit_length(MAX_GROUP_ACCELERATION) * _group_mass
		var own_mass: = 1.0 / state.inverse_mass
		force -= state.total_gravity * (own_mass + (_group_mass - own_mass) * _support)
	else:
		force -= state.total_gravity / state.inverse_mass
	entity.apply_force(force, offset)

	if not allow_spin and state.inverse_inertia > 0.0:
		entity.apply_torque( - spin * SPIN_DAMPING / state.inverse_inertia)
	for part in _group:
		part.sleeping = false


func _move_directly(world_paused: bool) -> void :
	var shift: = target_position - get_anchor_world_position()
	for part in _group:
		part.global_position += shift
		if world_paused and not part.freeze:

			part.linear_velocity = Vector2.ZERO
			part.angular_velocity = 0.0
			if part is DamageableEntity:
				(part as DamageableEntity).reset_impact_history()


func _refresh_group() -> void :
	if not entity.is_assembly_part():
		if _group.size() != 1 or _group[0] != entity:
			_group = [entity]
			_group_mass = entity.mass
		return
	if entity.assembly.connectivity_version == _group_version:
		return
	_group_version = entity.assembly.connectivity_version
	_group = entity.assembly.get_connected_parts(entity)
	_group_mass = 0.0
	for part in _group:
		_group_mass += part.mass
