class_name AssemblySerializer
extends RefCounted




static func serialize(assembly: EntityAssembly) -> Dictionary:
	var joints: = {}
	for joint in assembly.get_joints():
		joints[joint.get_name()] = {"connected": not joint.broken}
	var data: = {
		"uid": assembly.assembly_uid, 
		"type": assembly.definition.id, 
		"joints": joints, 
	}
	var controller_state: = assembly.controller.serialize_state()
	if not controller_state.is_empty():
		data["controller"] = controller_state
	return data


static func get_broken_joint_names(data: Dictionary) -> PackedStringArray:
	var broken: = PackedStringArray()
	var joints: Variant = data.get("joints")
	if joints is Dictionary:
		for joint_name: Variant in joints:
			var joint_state: Variant = joints[joint_name]
			if joint_state is Dictionary and not bool(joint_state.get("connected", true)):
				broken.append(str(joint_name))
	return broken
