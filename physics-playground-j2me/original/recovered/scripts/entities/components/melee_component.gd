class_name MeleeComponent
extends EntityComponent













var damage_multiplier: = 1.5
var threshold_multiplier: = 0.6


func get_component_type() -> StringName:
	return &"melee"


func configure(params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	damage_multiplier = clampf(float(params.get("damage_multiplier", damage_multiplier)), 0.0, 20.0)
	threshold_multiplier = clampf(float(params.get("threshold_multiplier", threshold_multiplier)), 0.1, 1.0)


func get_inspector_lines() -> PackedStringArray:
	return PackedStringArray(["Melee: ×%.1f impact damage" % damage_multiplier])
