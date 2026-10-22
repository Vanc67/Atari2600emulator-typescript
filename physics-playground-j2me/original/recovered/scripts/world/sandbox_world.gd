class_name SandboxWorld
extends Node2D








signal entity_spawned(entity: SandboxEntity)
signal entity_removed(entity: SandboxEntity)
signal entity_count_changed(count: int)
signal assembly_spawned(assembly: EntityAssembly)
signal assembly_removed(assembly: EntityAssembly)
signal grid_visibility_changed(shown: bool)

const BOUNDS_CHECK_INTERVAL_TICKS: = 30

const MAX_DEBRIS: = 60


@export var half_width: = 6000.0

@export var floor_y: = 0.0
@export var wall_height: = 5000.0

@export var escape_margin: = 3000.0

var factory: EntityFactory

@onready var grid_overlay: GridOverlay = $GridOverlay
@onready var geometry: WorldGeometry = $Geometry
@onready var camera: CameraController = $Camera
@onready var _entity_container: Node2D = $Entities
@onready var _assembly_container: Node2D = $Assemblies
@onready var projectiles: ProjectileSystem = $Projectiles
@onready var effects: EffectsLayer = $Effects
@onready var explosions: ExplosionSystem = $Explosions
@onready var thermal: ThermalSystem = $Thermal

var _entities_by_uid: Dictionary = {}
var _assemblies_by_uid: Dictionary = {}
var _next_uid: = 1
var _ticks_since_bounds_check: = 0
var _debris: Array[SandboxEntity] = []


func _ready() -> void :
	geometry.build(half_width, floor_y, wall_height)
	camera.bounds = Rect2( - half_width - 1000.0, floor_y - wall_height - 2000.0, 
		(half_width + 1000.0) * 2.0, wall_height + 3500.0)
	grid_overlay.camera = camera


func setup(entity_factory: EntityFactory) -> void :
	factory = entity_factory
	projectiles.setup(self, entity_factory.get_registry())
	explosions.setup(self)
	thermal.setup(self)


func _physics_process(_delta: float) -> void :
	_ticks_since_bounds_check += 1
	if _ticks_since_bounds_check < BOUNDS_CHECK_INTERVAL_TICKS:
		return
	_ticks_since_bounds_check = 0
	var limits: = get_escape_bounds()
	var removed_any: = false
	for entity in get_entities():
		if not limits.has_point(entity.global_position):
			_remove_internal(entity)
			removed_any = true
	if removed_any:
		entity_count_changed.emit(get_entity_count())






func spawn(type_id: String, spawn_position: Vector2, spawn_rotation: = 0.0) -> Node:
	var definition: = factory.get_definition(type_id)
	if definition == null:
		push_error("SandboxWorld: unknown type '%s'" % type_id)
		return null
	if definition.is_assembly():
		return spawn_assembly(type_id, spawn_position, spawn_rotation)
	return spawn_entity(type_id, spawn_position, spawn_rotation)




func spawn_entity(type_id: String, spawn_position: Vector2, spawn_rotation: = 0.0, uid: = 0) -> SandboxEntity:
	assert (factory != null, "SandboxWorld.setup() must be called before spawning")
	uid = _claim_uid(uid)
	var entity: = factory.create(type_id, uid)
	if entity == null:
		return null
	entity.position = spawn_position
	entity.rotation = spawn_rotation
	entity.world = self
	entity.destroy_requested.connect(_on_entity_destroy_requested)
	_entity_container.add_child(entity)
	_entities_by_uid[uid] = entity
	entity_spawned.emit(entity)
	entity_count_changed.emit(get_entity_count())
	return entity



func spawn_assembly(type_id: String, origin: Vector2, spawn_rotation: = 0.0, uid: = 0) -> EntityAssembly:
	var definition: = factory.get_definition(type_id)
	if definition == null or not definition.is_assembly():
		push_error("SandboxWorld: '%s' is not an assembly" % type_id)
		return null
	var parts: = {}
	for part in definition.assembly.parts:
		var entity: = spawn_entity(part.entity_id, origin + part.position.rotated(spawn_rotation), 
			spawn_rotation + part.rotation)
		if entity == null:
			for spawned: SandboxEntity in parts.values():
				remove_entity(spawned)
			return null
		parts[part.name] = entity
	var nudge: = definition.assembly.spawn_nudge
	if nudge > 0.0:
		var push: = Vector2(nudge * (1.0 if randf() < 0.5 else -1.0), 0.0).rotated(spawn_rotation)
		for part in definition.assembly.parts:
			if part.position.y < 0.0:
				(parts[part.name] as SandboxEntity).linear_velocity = push * ( - part.position.y / 100.0)
	return _register_assembly(definition, uid, parts, PackedStringArray(), {})




func restore_assembly(type_id: String, uid: int, parts: Dictionary, 
		broken_joint_names: PackedStringArray, controller_state: Dictionary) -> EntityAssembly:
	var definition: = factory.get_definition(type_id)
	if definition == null or not definition.is_assembly() or parts.is_empty():
		return null
	return _register_assembly(definition, uid, parts, broken_joint_names, controller_state)




func duplicate_group(group: Array[SandboxEntity], offset: Vector2) -> EntityAssembly:
	var source: = group[0].assembly
	var parts: = {}
	for original in group:
		var copy: = spawn_entity(original.get_type_id(), original.global_position + offset, original.global_rotation)
		if copy == null:
			continue
		EntitySerializer.apply_state(copy, EntitySerializer.serialize(original), false)
		parts[original.assembly_part_name] = copy
	return restore_assembly(source.definition.id, 0, parts, source.get_broken_joint_names(), 
		source.controller.serialize_state())


func _register_assembly(definition: EntityDefinition, uid: int, parts: Dictionary, 
		broken_joint_names: PackedStringArray, controller_state: Dictionary) -> EntityAssembly:
	uid = _claim_uid(uid)
	var assembly: = factory.create_assembly(definition.id, uid)
	if assembly == null:
		return null
	_assembly_container.add_child(assembly)
	assembly.attach_parts(parts, broken_joint_names)
	if not controller_state.is_empty():
		assembly.controller.deserialize_state(controller_state)
	_assemblies_by_uid[uid] = assembly
	assembly_spawned.emit(assembly)
	entity_count_changed.emit(get_entity_count())
	return assembly


func _claim_uid(requested: int) -> int:
	var uid: = requested
	if uid <= 0 or _entities_by_uid.has(uid) or _assemblies_by_uid.has(uid):
		if uid > 0:
			push_warning("SandboxWorld: uid %d already in use; assigning a new one" % uid)
		uid = _next_uid
	_next_uid = maxi(_next_uid, uid + 1)
	return uid




func remove_entity(entity: SandboxEntity) -> void :
	if _remove_internal(entity):
		entity_count_changed.emit(get_entity_count())


func remove_assembly(assembly: EntityAssembly) -> void :
	if not is_instance_valid(assembly):
		return
	for part in assembly.get_parts():
		_remove_internal(part)
	entity_count_changed.emit(get_entity_count())




func clear_entities() -> void :
	for entity in get_entities():
		_remove_internal(entity)
	_debris.clear()
	projectiles.clear()
	explosions.clear()
	thermal.clear()
	effects.clear()
	entity_count_changed.emit(0)




func spawn_debris(type_id: String, center: Vector2, base_rotation: float, count: int, 
		scatter_speed: float, base_velocity: Vector2, extent: Vector2) -> void :
	var alive: Array[SandboxEntity] = []
	for piece: Variant in _debris:
		if is_instance_valid(piece) and (piece as SandboxEntity).is_inside_tree():
			alive.append(piece)
	_debris = alive
	for index in count:
		while _debris.size() >= MAX_DEBRIS:
			remove_entity(_debris.pop_front())
		var offset: = Vector2(randf_range( - extent.x, extent.x), randf_range( - extent.y, extent.y)).rotated(base_rotation)
		var piece: = spawn_entity(type_id, center + offset, base_rotation + randf_range( - PI, PI))
		if piece == null:
			return
		var outward: = offset.normalized() if offset.length_squared() > 0.01 else Vector2.from_angle(randf() * TAU)
		piece.linear_velocity = base_velocity + (outward + Vector2(0, -0.6)) * scatter_speed * randf_range(0.5, 1.2)
		piece.angular_velocity = randf_range(-12.0, 12.0)
		_debris.append(piece)


func get_debris_count() -> int:
	var count: = 0
	for piece: Variant in _debris:
		if is_instance_valid(piece) and (piece as SandboxEntity).is_inside_tree():
			count += 1
	return count


func _remove_internal(entity: SandboxEntity) -> bool:
	if not is_instance_valid(entity) or _entities_by_uid.get(entity.entity_uid) != entity:
		return false
	_entities_by_uid.erase(entity.entity_uid)
	var assembly: = entity.assembly if entity.is_assembly_part() else null
	if assembly:

		assembly.on_part_removed(entity)
	entity_removed.emit(entity)


	_entity_container.remove_child(entity)
	entity.queue_free()
	if assembly and assembly.get_part_count() == 0:
		_assemblies_by_uid.erase(assembly.assembly_uid)
		assembly_removed.emit(assembly)
		_assembly_container.remove_child(assembly)
		assembly.queue_free()
	return true


func _on_entity_destroy_requested(entity: SandboxEntity) -> void :

	remove_entity.call_deferred(entity)




func get_entities() -> Array[SandboxEntity]:
	var result: Array[SandboxEntity] = []
	result.assign(_entities_by_uid.values())
	return result



func get_entity_count() -> int:
	return _entities_by_uid.size()



func get_object_count() -> int:
	var parts: = 0
	for assembly: EntityAssembly in _assemblies_by_uid.values():
		parts += assembly.get_part_count()
	return _entities_by_uid.size() - parts + _assemblies_by_uid.size()


func get_entity_by_uid(uid: int) -> SandboxEntity:
	return _entities_by_uid.get(uid) as SandboxEntity


func get_assemblies() -> Array[EntityAssembly]:
	var result: Array[EntityAssembly] = []
	result.assign(_assemblies_by_uid.values())
	return result


func get_assembly_count() -> int:
	return _assemblies_by_uid.size()


func get_assembly_by_uid(uid: int) -> EntityAssembly:
	return _assemblies_by_uid.get(uid) as EntityAssembly



func get_connected_group(entity: SandboxEntity) -> Array[SandboxEntity]:
	if entity.is_assembly_part():
		return entity.assembly.get_connected_parts(entity)
	var single: Array[SandboxEntity] = [entity]
	return single


func get_next_uid() -> int:
	return _next_uid


func set_next_uid(value: int) -> void :
	_next_uid = maxi(value, 1)




func set_grid_visible(shown: bool) -> void :
	grid_overlay.visible = shown
	grid_visibility_changed.emit(shown)


func toggle_grid() -> void :
	set_grid_visible( not grid_overlay.visible)


func is_grid_visible() -> bool:
	return grid_overlay.visible




func get_escape_bounds() -> Rect2:
	var top: = floor_y - wall_height - 20000.0
	return Rect2( - half_width - escape_margin, top, 
		(half_width + escape_margin) * 2.0, floor_y + escape_margin - top)


func is_inside_play_area(point: Vector2) -> bool:
	return point.x > - half_width and point.x < half_width and point.y < floor_y



func is_area_free(definition: EntityDefinition, at_position: Vector2, at_rotation: float, 
		exclude: Array[RID] = []) -> bool:
	var query: = PhysicsShapeQueryParameters2D.new()
	query.shape = definition.get_shape()
	query.transform = Transform2D(at_rotation, at_position)
	query.collision_mask = CollisionLayers.WORLD | CollisionLayers.ENTITIES
	query.exclude = exclude
	return get_world_2d().direct_space_state.intersect_shape(query, 1).is_empty()



func can_place(definition: EntityDefinition, at_position: Vector2, at_rotation: float) -> bool:
	if not definition.is_assembly():
		return is_inside_play_area(at_position) and is_area_free(definition, at_position, at_rotation)
	for part in definition.assembly.parts:
		var part_position: = at_position + part.position.rotated(at_rotation)
		if not is_inside_play_area(part_position)\
		or not is_area_free(part.definition, part_position, at_rotation + part.rotation):
			return false
	return true



func pick_entity_at(point: Vector2) -> SandboxEntity:
	var query: = PhysicsPointQueryParameters2D.new()
	query.position = point
	query.collision_mask = CollisionLayers.ENTITIES
	var best: SandboxEntity = null
	for hit in get_world_2d().direct_space_state.intersect_point(query, 32):
		var entity: = hit.get("collider") as SandboxEntity
		if entity == null:
			continue
		if best == null or entity.z_index > best.z_index\
		or (entity.z_index == best.z_index and entity.get_index() > best.get_index()):
			best = entity
	return best
