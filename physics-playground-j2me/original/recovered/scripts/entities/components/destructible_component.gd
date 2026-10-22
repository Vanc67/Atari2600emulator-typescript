class_name DestructibleComponent
extends EntityComponent
















signal destroyed

var threshold: = 0.0
var debris_id: = ""
var debris_count: = 4
var debris_speed: = 180.0
var effect: = "debris"
var sound: = ""

var is_destroyed: = false


func get_component_type() -> StringName:
	return &"destructible"


func configure(params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	threshold = clampf(float(params.get("threshold", 0.0)), 0.0, 0.99)
	debris_id = str(params.get("debris", ""))
	debris_count = clampi(int(params.get("debris_count", 4)), 0, 16)
	debris_speed = maxf(float(params.get("debris_speed", debris_speed)), 0.0)
	effect = str(params.get("effect", effect))
	sound = str(params.get("sound", ""))


func _ready() -> void :
	var health: = entity.get_health()
	if health:
		health.health_changed.connect(_on_health_changed)


func _on_health_changed(current: float, maximum: float) -> void :
	if is_destroyed or current > maximum * threshold:
		return

	var explosive: = entity.get_component(&"explosive") as ExplosiveComponent
	if explosive and explosive.detonate_on_depleted and not explosive.has_detonated:
		return
	destroy()




func destroy(from_explosion: = false) -> void :
	if is_destroyed or entity == null:
		return
	is_destroyed = true
	destroyed.emit()
	var world: = entity.world
	if world:
		var center: = entity.global_position
		if not from_explosion:
			world.effects.burst(effect, center, Vector2.ZERO, clampf(entity.definition.get_bounding_radius() / 30.0, 0.6, 3.0))
		if not sound.is_empty():
			Audio.play(sound, center)
		if not debris_id.is_empty() and debris_count > 0:
			world.spawn_debris.call_deferred(debris_id, center, entity.global_rotation, debris_count, 
				debris_speed, entity.linear_velocity, entity.definition.get_bounding_size() * 0.35)
	entity.request_destroy()


func get_inspector_lines() -> PackedStringArray:
	return PackedStringArray(["Destructible (breaks at %d%% health)" % roundi(threshold * 100.0)])
