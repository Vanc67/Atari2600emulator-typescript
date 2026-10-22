class_name AssemblyController
extends RefCounted







var assembly: EntityAssembly
var params: Dictionary = {}


func setup(target_assembly: EntityAssembly, controller_params: Dictionary) -> void :
	assembly = target_assembly
	params = controller_params
	_configure(controller_params)



func _configure(_controller_params: Dictionary) -> void :
	pass




func evaluate() -> void :
	pass


func on_part_damaged(_part_name: String, _info: DamageInfo) -> void :
	evaluate()


func on_joint_broken(_joint: AssemblyJoint) -> void :
	evaluate()


func on_part_removed(_part_name: String) -> void :
	evaluate()



func revive() -> void :
	evaluate()



func get_state_name() -> String:
	return ""



func apply_state_to_part(_part: SandboxEntity) -> void :
	pass


func serialize_state() -> Dictionary:
	return {}


func deserialize_state(_data: Dictionary) -> void :
	pass


func get_inspector_lines() -> PackedStringArray:
	return PackedStringArray()
