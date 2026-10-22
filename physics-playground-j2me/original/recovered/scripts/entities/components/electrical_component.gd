class_name ElectricalComponent
extends EntityComponent






const CONDUCTOR_THRESHOLD: = 0.5

var conductivity: = 0.0

var charge: = 0.0


func get_component_type() -> StringName:
	return &"electrical"


func configure(params: Dictionary, _definition: EntityDefinition, material: MaterialDefinition) -> void :
	var material_conductivity: = material.electrical_conductivity if material else 0.0
	conductivity = clampf(float(params.get("conductivity", material_conductivity)), 0.0, 1.0)


func is_conductor() -> bool:
	return conductivity >= CONDUCTOR_THRESHOLD


func serialize_state() -> Dictionary:
	if is_zero_approx(charge):
		return {}
	return {"charge": charge}


func deserialize_state(data: Dictionary) -> void :
	charge = float(data.get("charge", 0.0))


func get_inspector_lines() -> PackedStringArray:
	return PackedStringArray(["Conductive: %s" % ("yes" if is_conductor() else "no")])
