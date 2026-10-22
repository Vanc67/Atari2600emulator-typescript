class_name EntityRegistry
extends RefCounted





var _mods: Array[ModManifest] = []
var _materials: Dictionary = {}
var _entities: Dictionary = {}
var _projectiles: Dictionary = {}
var _sounds: Dictionary = {}
var _entity_order: Array[String] = []
var _category_order: Array[String] = []


func clear() -> void :
	_mods.clear()
	_materials.clear()
	_entities.clear()
	_projectiles.clear()
	_sounds.clear()
	_entity_order.clear()
	_category_order.clear()


func register_mod(manifest: ModManifest) -> void :
	_mods.append(manifest)
	for category in manifest.categories:
		if not _category_order.has(category):
			_category_order.append(category)



func register_material(material: MaterialDefinition) -> bool:
	if _materials.has(material.id):
		return false
	_materials[material.id] = material
	return true



func register_entity(definition: EntityDefinition) -> bool:
	if _entities.has(definition.id):
		return false
	_entities[definition.id] = definition
	_entity_order.append(definition.id)
	if not _category_order.has(definition.category):
		_category_order.append(definition.category)
	return true


func register_projectile(projectile: ProjectileDefinition) -> bool:
	if _projectiles.has(projectile.id):
		return false
	_projectiles[projectile.id] = projectile
	return true


func get_projectile(id: String) -> ProjectileDefinition:
	return _projectiles.get(id) as ProjectileDefinition


func has_projectile(id: String) -> bool:
	return _projectiles.has(id)


func register_sound(sound: SoundDefinition) -> bool:
	if _sounds.has(sound.id):
		return false
	_sounds[sound.id] = sound
	return true


func get_sound(id: String) -> SoundDefinition:
	return _sounds.get(id) as SoundDefinition


func has_sound(id: String) -> bool:
	return _sounds.has(id)


func get_sounds() -> Array[SoundDefinition]:
	var result: Array[SoundDefinition] = []
	result.assign(_sounds.values())
	return result



func unregister_entity(id: String) -> void :
	_entities.erase(id)
	_entity_order.erase(id)


func has_material(id: String) -> bool:
	return _materials.has(id)


func get_material(id: String) -> MaterialDefinition:
	return _materials.get(id) as MaterialDefinition


func has_entity(id: String) -> bool:
	return _entities.has(id)


func get_entity_definition(id: String) -> EntityDefinition:
	return _entities.get(id) as EntityDefinition


func get_entity_ids() -> Array[String]:
	return _entity_order.duplicate()


func get_all_entities() -> Array[EntityDefinition]:
	var result: Array[EntityDefinition] = []
	for id in _entity_order:
		result.append(_entities[id])
	return result



func get_spawnable_entities() -> Array[EntityDefinition]:
	var result: Array[EntityDefinition] = []
	for id in _entity_order:
		var definition: EntityDefinition = _entities[id]
		if not definition.hidden:
			result.append(definition)
	return result


func get_assembly_definitions() -> Array[EntityDefinition]:
	var result: Array[EntityDefinition] = []
	for id in _entity_order:
		var definition: EntityDefinition = _entities[id]
		if definition.is_assembly():
			result.append(definition)
	return result



func get_categories() -> Array[String]:
	var result: Array[String] = []
	for category in _category_order:
		for id in _entity_order:
			var definition: EntityDefinition = _entities[id]
			if definition.category == category and not definition.hidden:
				result.append(category)
				break
	return result



func get_entities_in_category(category: String) -> Array[EntityDefinition]:
	var result: Array[EntityDefinition] = []
	for id in _entity_order:
		var definition: EntityDefinition = _entities[id]
		if definition.category == category and not definition.hidden:
			result.append(definition)
	return result


func get_mods() -> Array[ModManifest]:
	return _mods.duplicate()



func get_mod_versions() -> Dictionary:
	var versions: = {}
	for manifest in _mods:
		versions[manifest.id] = manifest.version
	return versions
