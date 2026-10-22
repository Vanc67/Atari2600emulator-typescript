class_name EntityFactory
extends RefCounted








const ENTITY_SCENE: = preload("res://scenes/entities/sandbox_entity.tscn")

const DAMAGEABLE_ENTITY_SCENE: = preload("res://scenes/entities/damageable_entity.tscn")

var _registry: EntityRegistry


func _init(registry: EntityRegistry) -> void :
	_registry = registry


func get_registry() -> EntityRegistry:
	return _registry


func get_definition(type_id: String) -> EntityDefinition:
	return _registry.get_entity_definition(type_id)



func create(type_id: String, uid: int) -> SandboxEntity:
	var definition: = _registry.get_entity_definition(type_id)
	if definition == null:
		push_error("EntityFactory: unknown entity type '%s'" % type_id)
		return null
	if definition.is_assembly():
		push_error("EntityFactory: '%s' is an assembly; use SandboxWorld.spawn_assembly()" % type_id)
		return null
	var material: = _registry.get_material(definition.material_id)
	if material == null:
		push_error("EntityFactory: entity '%s' references missing material '%s'" % [type_id, definition.material_id])
		return null

	var scene: = DAMAGEABLE_ENTITY_SCENE if definition.components.has("impact_damage") else ENTITY_SCENE
	var entity: = scene.instantiate() as SandboxEntity
	entity.setup(definition, material, uid)
	for component_type: String in definition.components:
		var component: = ComponentRegistry.create(component_type)
		if component == null:
			push_warning("EntityFactory: unknown component '%s' on '%s'" % [component_type, type_id])
			continue
		component.configure(definition.components[component_type], definition, material)
		entity.add_component(component)
	return entity



func create_assembly(type_id: String, uid: int) -> EntityAssembly:
	var definition: = _registry.get_entity_definition(type_id)
	if definition == null or not definition.is_assembly():
		push_error("EntityFactory: '%s' is not an assembly definition" % type_id)
		return null
	var assembly: = EntityAssembly.new()
	assembly.setup(definition, uid, AssemblyControllerRegistry.create(definition.assembly.controller_type))
	return assembly
