class_name SpawnPlacer
extends Node2D




const VALID_TINT: = Color(0.75, 1.0, 0.8, 0.55)
const INVALID_TINT: = Color(1.0, 0.4, 0.35, 0.55)

var definition: EntityDefinition = null
var placement_rotation: = 0.0
var is_placement_valid: = false

var _world: SandboxWorld


func setup(world: SandboxWorld) -> void :
	_world = world
	visible = false


func is_active() -> bool:
	return definition != null


func begin(entity_definition: EntityDefinition) -> void :
	definition = entity_definition
	placement_rotation = 0.0
	_rebuild_ghosts()
	visible = true


func cancel() -> void :
	definition = null
	visible = false
	_clear_ghosts()


func rotate_ghost(amount: float) -> void :
	placement_rotation = wrapf(placement_rotation + amount, - PI, PI)


func update_preview(world_position: Vector2) -> void :
	if not is_active():
		return
	global_position = world_position
	rotation = placement_rotation
	is_placement_valid = _world.can_place(definition, world_position, placement_rotation)
	modulate = VALID_TINT if is_placement_valid else INVALID_TINT


func _rebuild_ghosts() -> void :
	_clear_ghosts()
	if definition.is_assembly():
		for part in definition.assembly.parts:
			_add_ghost(part.definition, part.position, part.rotation, part.z_index)
	else:
		_add_ghost(definition, Vector2.ZERO, 0.0, 0)


func _add_ghost(part_definition: EntityDefinition, offset: Vector2, angle: float, layer: int) -> void :
	var ghost: = EntityVisual.new()
	ghost.configure(part_definition, 1)
	ghost.position = offset
	ghost.rotation = angle
	ghost.z_index = layer
	add_child(ghost)


func _clear_ghosts() -> void :
	for child in get_children():
		remove_child(child)
		child.queue_free()
