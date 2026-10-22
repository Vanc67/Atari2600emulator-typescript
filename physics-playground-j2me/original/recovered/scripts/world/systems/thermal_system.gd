class_name ThermalSystem
extends Node









@export var tick_interval: = 0.25
const MAX_NEIGHBOURS: = 24

const CRACKLE_INTERVAL: = 1.2

var tick_count: = 0

var _world: SandboxWorld
var _active: Dictionary = {}
var _accumulator: = 0.0
var _circle: = CircleShape2D.new()
var _query: = PhysicsShapeQueryParameters2D.new()
var _crackle_timer: = 0.0


func setup(world: SandboxWorld) -> void :
	_world = world
	_query.shape = _circle
	_query.collision_mask = CollisionLayers.ENTITIES


func register(component: ThermalComponent) -> void :
	_active[component.get_instance_id()] = component


func get_active_count() -> int:
	return _active.size()


func get_burning_count() -> int:
	var count: = 0
	for component: Variant in _active.values():
		if is_instance_valid(component) and (component as ThermalComponent).burning:
			count += 1
	return count


func clear() -> void :
	_active.clear()


func _physics_process(delta: float) -> void :
	if _active.is_empty():
		return
	_accumulator += delta
	if _accumulator < tick_interval:
		return
	var step: = _accumulator
	_accumulator = 0.0
	tick_count += 1
	_crackle_timer += step
	var crackle_due: = _crackle_timer >= CRACKLE_INTERVAL
	if crackle_due:
		_crackle_timer = 0.0

	for key: int in _active.keys():

		var candidate: Variant = _active.get(key)
		if not is_instance_valid(candidate):
			_active.erase(key)
			continue
		var component: = candidate as ThermalComponent
		if not is_instance_valid(component.entity) or not component.entity.is_inside_tree():
			_active.erase(key)
			continue
		if component.burning:
			_spread_heat(component, step)
			if crackle_due:
				Audio.play("core:fire_crackle", component.entity.global_position, -8.0)
		if not component.thermal_tick(step):
			_active.erase(key)


func _spread_heat(source: ThermalComponent, step: float) -> void :
	var entity: = source.entity
	var reach: = source.heat_radius + entity.definition.get_bounding_radius()
	_circle.radius = reach
	_query.transform = Transform2D(0.0, entity.global_position)
	var exclude: Array[RID] = [entity.get_rid()]
	_query.exclude = exclude
	var space: = _world.get_world_2d().direct_space_state
	for hit in space.intersect_shape(_query, MAX_NEIGHBOURS):
		var neighbour: = hit.get("collider") as SandboxEntity
		if neighbour == null:
			continue
		var thermal: = neighbour.get_thermal()
		if thermal == null or thermal.burning:
			continue
		var distance: = maxf(entity.global_position.distance_to(neighbour.global_position)
			- neighbour.definition.get_bounding_radius(), 0.0)
		var falloff: = clampf(1.0 - distance / reach, 0.0, 1.0)
		if falloff > 0.0:
			thermal.add_heat(source.heat_output * falloff * step)
