class_name AssemblyControllerRegistry
extends RefCounted




const CONTROLLER_SCRIPTS: = {
	"human": preload("res://scripts/entities/assembly/human_controller.gd"), 
}


static func has_type(controller_type: String) -> bool:
	return CONTROLLER_SCRIPTS.has(controller_type)




static func create(controller_type: String) -> AssemblyController:
	if has_type(controller_type):
		var script: GDScript = CONTROLLER_SCRIPTS[controller_type]
		return script.new() as AssemblyController
	return AssemblyController.new()
