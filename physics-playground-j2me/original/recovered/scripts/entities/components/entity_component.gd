class_name EntityComponent
extends Node











var entity: SandboxEntity



func get_component_type() -> StringName:
	return &""



func configure(_params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	pass



func serialize_state() -> Dictionary:
	return {}


func deserialize_state(_data: Dictionary) -> void :
	pass




func on_activate(_pressed: bool) -> void :
	pass



func on_reload() -> void :
	pass



func is_activatable() -> bool:
	return false



func get_inspector_lines() -> PackedStringArray:
	return PackedStringArray()
