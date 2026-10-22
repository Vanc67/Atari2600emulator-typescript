class_name DebrisComponent
extends EntityComponent









var lifetime: = 6.0
var _age: = 0.0


func get_component_type() -> StringName:
	return &"debris"


func configure(params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	lifetime = clampf(float(params.get("lifetime", lifetime)), 0.1, 120.0)


func _ready() -> void :
	entity.collision_layer = CollisionLayers.DEBRIS
	entity.collision_mask = CollisionLayers.WORLD
	entity.continuous_cd = RigidBody2D.CCD_MODE_DISABLED


func _physics_process(delta: float) -> void :
	_age += delta
	if _age >= lifetime:
		set_physics_process(false)
		entity.request_destroy()
	elif _age > lifetime - 1.0:
		entity.visual.modulate.a = clampf(lifetime - _age, 0.0, 1.0)
