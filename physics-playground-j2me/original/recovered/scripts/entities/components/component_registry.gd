class_name ComponentRegistry
extends RefCounted









const COMPONENT_SCRIPTS: = {
	"health": preload("res://scripts/entities/components/health_component.gd"), 
	"thermal": preload("res://scripts/entities/components/thermal_component.gd"), 
	"electrical": preload("res://scripts/entities/components/electrical_component.gd"), 
	"impact_damage": preload("res://scripts/entities/components/impact_damage_component.gd"), 
	"magazine": preload("res://scripts/entities/components/magazine_component.gd"), 
	"firearm": preload("res://scripts/entities/components/firearm_component.gd"), 
	"explosive": preload("res://scripts/entities/components/explosive_component.gd"), 
	"destructible": preload("res://scripts/entities/components/destructible_component.gd"), 
	"debris": preload("res://scripts/entities/components/debris_component.gd"), 
	"melee": preload("res://scripts/entities/components/melee_component.gd"), 
}


static func has_type(component_type: String) -> bool:
	return COMPONENT_SCRIPTS.has(component_type)


static func get_types() -> PackedStringArray:
	return PackedStringArray(COMPONENT_SCRIPTS.keys())


static func create(component_type: String) -> EntityComponent:
	if not has_type(component_type):
		return null
	var script: GDScript = COMPONENT_SCRIPTS[component_type]
	var component: = script.new() as EntityComponent
	assert (component.get_component_type() == StringName(component_type), 
		"Component script type name does not match its registry key: %s" % component_type)
	return component







static func link_params(components: Dictionary, registry: EntityRegistry, reader: DataReader) -> void :
	var destructible: Variant = components.get("destructible")
	if destructible is Dictionary and destructible.get("debris") is String:
		var debris_id: String = destructible["debris"]
		if not debris_id.is_empty() and not registry.has_entity(debris_id):
			reader.warn("destructible.debris: unknown entity '%s' (no debris will spawn)" % debris_id)
			destructible["debris"] = ""
