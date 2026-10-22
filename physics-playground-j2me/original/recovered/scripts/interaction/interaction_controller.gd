class_name InteractionController
extends Node2D











signal selection_changed(entity: SandboxEntity)
signal spawn_mode_changed(type_id: String)
signal message_requested(text: String)
signal tool_changed(tool: int)

const ROTATE_TARGET_SPEED: = 5.0
const ROTATE_RESPONSE: = 12.0
const ROTATE_DIRECT_SPEED: = 2.5
const GHOST_ROTATE_SPEED: = 2.5
const DUPLICATE_GAP: = 6.0
const DRAG_LINE_COLOR: = Color(0.89, 0.64, 0.23, 0.8)

var selected_entity: SandboxEntity = null
var current_tool: int = SandboxTools.Tool.DRAG

var tool_damage_amount: = 25.0

var _world: SandboxWorld
var _registry: EntityRegistry
var _drag: DragHandle = null
var _drag_protected_assembly: EntityAssembly = null
var _cursor_shape: = Input.CURSOR_ARROW


var _activated_entity: SandboxEntity = null

@onready var _spawn_placer: SpawnPlacer = $SpawnPlacer


func setup(world: SandboxWorld, registry: EntityRegistry) -> void :
	_world = world
	_registry = registry
	_world.entity_removed.connect(_on_entity_removed)
	_spawn_placer.setup(world)




func is_spawning() -> bool:
	return _spawn_placer.is_active()


func get_spawn_type_id() -> String:
	return _spawn_placer.definition.id if is_spawning() else ""


func begin_spawn(type_id: String) -> void :
	var definition: = _registry.get_entity_definition(type_id)
	if definition == null:
		return
	end_drag()
	_spawn_placer.begin(definition)
	_spawn_placer.update_preview(get_global_mouse_position())
	spawn_mode_changed.emit(type_id)


func cancel_spawn() -> void :
	if not is_spawning():
		return
	_spawn_placer.cancel()
	spawn_mode_changed.emit("")


func select(entity: SandboxEntity) -> void :
	if entity == selected_entity:
		return
	if is_instance_valid(selected_entity):
		selected_entity.is_selected = false
	selected_entity = entity
	if entity:
		entity.is_selected = true
	selection_changed.emit(entity)


func clear_selection() -> void :
	select(null)


func has_selection() -> bool:
	return is_instance_valid(selected_entity)


func get_selected_group() -> Array[SandboxEntity]:
	if not has_selection():
		var empty: Array[SandboxEntity] = []
		return empty
	return _world.get_connected_group(selected_entity)


func delete_selected() -> void :
	for entity in get_selected_group():
		_world.remove_entity(entity)


func toggle_freeze_selected() -> void :
	if not has_selection():
		return
	var frozen: = not selected_entity.is_frozen()
	for entity in get_selected_group():
		entity.set_frozen(frozen)
	selection_changed.emit(selected_entity)




func heal_selected() -> void :
	if not has_selection():
		return
	SandboxTools.heal(selected_entity)
	selection_changed.emit(selected_entity)


func set_tool(tool: int) -> void :
	if tool == current_tool:
		return
	current_tool = tool
	end_drag()
	cancel_spawn()
	tool_changed.emit(tool)



func activate_selected(pressed: bool) -> void :
	if pressed:
		release_activation()
		if has_selection() and selected_entity.is_activatable():
			_activated_entity = selected_entity
			_activated_entity.activate(true)
	else:
		release_activation()


func release_activation() -> void :
	if is_instance_valid(_activated_entity):
		_activated_entity.activate(false)
	_activated_entity = null


func reload_selected() -> void :
	if has_selection():
		selected_entity.reload()
		selection_changed.emit(selected_entity)




func apply_tool_at(world_position: Vector2) -> void :
	if current_tool == SandboxTools.Tool.EXPLODE:
		SandboxTools.explode_at(_world, world_position)
		return
	var entity: = _world.pick_entity_at(world_position)
	select(entity)
	if entity == null:
		return
	match current_tool:
		SandboxTools.Tool.FREEZE:
			SandboxTools.freeze_toggle(_world, entity)
		SandboxTools.Tool.DELETE:
			SandboxTools.delete(_world, entity)
		SandboxTools.Tool.HEAL:
			SandboxTools.heal(entity)
		SandboxTools.Tool.DAMAGE:
			SandboxTools.damage(entity, tool_damage_amount)
		SandboxTools.Tool.IGNITE:
			if not SandboxTools.ignite(entity):
				message_requested.emit("%s is not flammable." % entity.get_display_name())
		SandboxTools.Tool.EXTINGUISH:
			SandboxTools.extinguish(_world, entity)
	if is_instance_valid(entity) and entity == selected_entity:
		selection_changed.emit(entity)


func duplicate_selected() -> void :
	if not has_selection():
		return
	var group: = get_selected_group()
	var offset: = _find_free_offset(group)
	if group.size() > 1:
		var copy_assembly: = _world.duplicate_group(group, offset)
		if copy_assembly:
			select(copy_assembly.get_part(selected_entity.assembly_part_name))
		return
	var source: = selected_entity
	var copy: = _world.spawn_entity(source.get_type_id(), source.global_position + offset, source.global_rotation)
	if copy == null:
		return
	EntitySerializer.apply_state(copy, EntitySerializer.serialize(source), false)
	select(copy)


func end_drag() -> void :
	_drag = null
	if is_instance_valid(_drag_protected_assembly):
		_drag_protected_assembly.end_drag_protection()
	_drag_protected_assembly = null
	queue_redraw()


func _begin_drag(entity: SandboxEntity, world_position: Vector2) -> void :
	end_drag()
	_drag = DragHandle.new(entity, world_position)
	if entity.is_assembly_part():
		_drag_protected_assembly = entity.assembly
		_drag_protected_assembly.begin_drag_protection()




func _find_free_offset(group: Array[SandboxEntity]) -> Vector2:
	var bounds: = Rect2(group[0].global_position, Vector2.ZERO)
	for entity in group:
		var reach: = Vector2.ONE * entity.definition.get_bounding_radius()
		bounds = bounds.merge(Rect2(entity.global_position - reach, reach * 2.0))
	var candidates: Array[Vector2] = [
		Vector2(0, - (bounds.size.y + DUPLICATE_GAP)), 
		Vector2(bounds.size.x + DUPLICATE_GAP, 0), 
		Vector2( - (bounds.size.x + DUPLICATE_GAP), 0), 
		Vector2(0, - (bounds.size.y + DUPLICATE_GAP) * 2.0), 
	]
	for candidate in candidates:
		var fits: = true
		for entity in group:
			if not _world.can_place(entity.definition, entity.global_position + candidate, entity.global_rotation):
				fits = false
				break
		if fits:
			return candidate
	return candidates[0]




func _unhandled_input(event: InputEvent) -> void :
	var mouse_button: = event as InputEventMouseButton
	if mouse_button:
		_handle_mouse_button(mouse_button)
		return
	if event.is_action_pressed("cancel"):
		if is_spawning():
			cancel_spawn()
		elif has_selection():
			clear_selection()
		else:
			return
	elif event.is_action_pressed("delete_selected"):
		delete_selected()
	elif event.is_action_pressed("toggle_freeze"):
		toggle_freeze_selected()
	elif event.is_action_pressed("duplicate_selected"):
		duplicate_selected()
	elif event.is_action_pressed("heal_selected", false, true):
		heal_selected()
	elif event.is_action_pressed("reload_selected", false, true):
		reload_selected()
	elif event.is_action_pressed("activate_selected"):
		activate_selected(true)
	elif event.is_action_released("activate_selected"):
		activate_selected(false)
	else:
		for index in SandboxTools.Tool.size():
			if event.is_action_pressed("tool_%d" % (index + 1), false, true):
				set_tool(index)
				get_viewport().set_input_as_handled()
				return
		return
	get_viewport().set_input_as_handled()


func _handle_mouse_button(event: InputEventMouseButton) -> void :
	var world_position: = get_global_mouse_position()
	if is_spawning():
		if event.pressed and event.button_index == MOUSE_BUTTON_LEFT:
			_try_place_spawn(world_position)
			get_viewport().set_input_as_handled()
		elif event.pressed and event.button_index == MOUSE_BUTTON_RIGHT:
			cancel_spawn()
			get_viewport().set_input_as_handled()
		return

	if event.button_index != MOUSE_BUTTON_LEFT:
		return
	if current_tool != SandboxTools.Tool.DRAG:
		if event.pressed:
			if current_tool == SandboxTools.Tool.INSPECT:
				select(_world.pick_entity_at(world_position))
			else:
				apply_tool_at(world_position)
		get_viewport().set_input_as_handled()
		return
	if event.pressed:
		var entity: = _world.pick_entity_at(world_position)
		select(entity)
		if entity:
			_begin_drag(entity, world_position)
	else:
		end_drag()
	get_viewport().set_input_as_handled()


func _try_place_spawn(world_position: Vector2) -> void :
	_spawn_placer.update_preview(world_position)
	if not _spawn_placer.is_placement_valid:
		message_requested.emit("Can't place here: the space is occupied or outside the play area.")
		return
	_world.spawn(_spawn_placer.definition.id, world_position, _spawn_placer.placement_rotation)




func _process(_delta: float) -> void :
	if is_spawning():
		_spawn_placer.update_preview(get_global_mouse_position())
	_update_cursor()


func _physics_process(delta: float) -> void :

	var real_delta: = delta / maxf(Engine.time_scale, 0.001)
	var world_paused: = get_tree().paused
	var rotate_input: = Input.get_axis("rotate_ccw", "rotate_cw")

	if _drag:
		if not _drag.is_valid() or not Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
			end_drag()
		else:
			_drag.target_position = get_global_mouse_position()
			_drag.update(world_paused, rotate_input != 0.0, delta)
			queue_redraw()

	if rotate_input == 0.0:
		return
	if is_spawning():
		_spawn_placer.rotate_ghost(rotate_input * GHOST_ROTATE_SPEED * real_delta)
	elif has_selection():
		_rotate_entity(selected_entity, rotate_input, real_delta, world_paused)


func _rotate_entity(entity: SandboxEntity, direction: float, real_delta: float, world_paused: bool) -> void :
	if entity.freeze or world_paused:
		entity.rotation += direction * ROTATE_DIRECT_SPEED * real_delta
		return
	var state: = PhysicsServer2D.body_get_direct_state(entity.get_rid())
	if state == null or state.inverse_inertia <= 0.0:
		return
	var inertia: = 1.0 / state.inverse_inertia
	var target_speed: = direction * ROTATE_TARGET_SPEED
	entity.apply_torque((target_speed - state.angular_velocity) * inertia * ROTATE_RESPONSE)
	entity.sleeping = false


func _update_cursor() -> void :
	var shape: = Input.CURSOR_ARROW
	if _drag:
		shape = Input.CURSOR_DRAG
	elif is_spawning():
		shape = Input.CURSOR_CROSS
	elif get_viewport().gui_get_hovered_control() == null\
	and _world.pick_entity_at(get_global_mouse_position()) != null:
		shape = Input.CURSOR_POINTING_HAND
	if shape != _cursor_shape:
		_cursor_shape = shape
		Input.set_default_cursor_shape(shape)


func _draw() -> void :
	if _drag == null or not _drag.is_valid():
		return

	var screen_scale: = get_viewport().get_canvas_transform().get_scale().x
	var width: = 2.0 / maxf(screen_scale, 0.001)
	var anchor: = _drag.get_anchor_world_position()
	draw_line(anchor, _drag.target_position, DRAG_LINE_COLOR, width, true)
	draw_circle(anchor, width * 2.0, DRAG_LINE_COLOR, true, -1.0, true)


func _on_entity_removed(entity: SandboxEntity) -> void :
	if entity == _activated_entity:
		_activated_entity = null
	if _drag and _drag.entity == entity:
		end_drag()
	if entity == selected_entity:
		selected_entity = null
		selection_changed.emit(null)
