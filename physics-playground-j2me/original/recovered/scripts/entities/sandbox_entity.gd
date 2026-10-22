class_name SandboxEntity
extends RigidBody2D











signal destroy_requested(entity: SandboxEntity)


var entity_uid: int = 0
var definition: EntityDefinition
var material_definition: MaterialDefinition

var properties: Dictionary = {}
var collision_shape: CollisionShape2D
var visual: EntityVisual


var assembly: EntityAssembly = null

var assembly_part_name: = ""



var world: SandboxWorld = null

var is_selected: = false:
	set(value):
		is_selected = value
		if visual:
			visual.is_selected = value

var _components: Dictionary = {}




func setup(entity_definition: EntityDefinition, material: MaterialDefinition, uid: int) -> void :
	definition = entity_definition
	material_definition = material
	entity_uid = uid
	name = "%s_%d" % [NamespacedId.get_local_name(definition.id), uid]

	collision_shape = $CollisionShape2D as CollisionShape2D
	collision_shape.shape = definition.get_shape()
	visual = $Visual as EntityVisual
	visual.configure(definition, uid)

	mass = definition.compute_mass(material)
	physics_material_override = material.physics_material
	collision_layer = CollisionLayers.ENTITIES
	collision_mask = CollisionLayers.WORLD | CollisionLayers.ENTITIES

	continuous_cd = RigidBody2D.CCD_MODE_CAST_SHAPE

	freeze_mode = RigidBody2D.FREEZE_MODE_KINEMATIC
	if definition.linear_damp >= 0.0:
		linear_damp = definition.linear_damp
	if definition.angular_damp >= 0.0:
		angular_damp = definition.angular_damp
	if definition.center_of_mass != Vector2.INF:

		center_of_mass_mode = RigidBody2D.CENTER_OF_MASS_MODE_CUSTOM
		center_of_mass = definition.center_of_mass
	properties = definition.properties.duplicate(true)


func get_type_id() -> String:
	return definition.id


func get_display_name() -> String:
	return definition.display_name


func is_assembly_part() -> bool:
	return assembly != null and is_instance_valid(assembly)


func get_health() -> HealthComponent:
	return _components.get(&"health") as HealthComponent



func set_damage_level(level: float) -> void :
	if visual:
		visual.damage_level = level


func request_destroy() -> void :
	destroy_requested.emit(self)



func activate(pressed: bool) -> void :
	for component in get_components():
		component.on_activate(pressed)


func reload() -> void :
	for component in get_components():
		component.on_reload()


func is_activatable() -> bool:
	for component in get_components():
		if component.is_activatable():
			return true
	return false


func get_thermal() -> ThermalComponent:
	return _components.get(&"thermal") as ThermalComponent



func is_transient() -> bool:
	return _components.has(&"debris")


func add_component(component: EntityComponent) -> void :
	var component_type: = component.get_component_type()
	assert ( not _components.has(component_type), "Duplicate component: %s" % component_type)
	component.entity = self
	component.name = String(component_type).to_pascal_case() + "Component"
	_components[component_type] = component
	add_child(component)


func get_component(component_type: StringName) -> EntityComponent:
	return _components.get(component_type) as EntityComponent


func has_component(component_type: StringName) -> bool:
	return _components.has(component_type)


func get_components() -> Array[EntityComponent]:
	var result: Array[EntityComponent] = []
	result.assign(_components.values())
	return result


func is_frozen() -> bool:
	return freeze


func set_frozen(value: bool) -> void :
	if value == freeze:
		return
	if value:

		linear_velocity = Vector2.ZERO
		angular_velocity = 0.0
	freeze = value
	if not value:
		sleeping = false
	visual.is_frozen = value
