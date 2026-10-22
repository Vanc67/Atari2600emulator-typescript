class_name AssemblyJoint
extends RefCounted



var definition: AssemblyDefinition.JointDefinition

var body_a: SandboxEntity
var body_b: SandboxEntity

var pin: PinJoint2D
var broken: = false

var strength: = 1.0

var effective_inertia: = 1.0

var strain_ticks: = 0

var limit_integral: = 0.0

var last_strain: = 0.0

var peak_strain: = 0.0


func get_name() -> String:
	return definition.name


func connects(entity: SandboxEntity) -> bool:
	return body_a == entity or body_b == entity


func get_other(entity: SandboxEntity) -> SandboxEntity:
	return body_b if body_a == entity else body_a
