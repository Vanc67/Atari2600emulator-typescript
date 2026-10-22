class_name ExplosiveComponent
extends EntityComponent
















signal armed(fuse: float)
signal detonated

var params: = ExplosionParams.new()
var fuse: = 3.0
var chain_fuse: = Vector2(0.05, 0.2)
var detonate_on_depleted: = true
var detonate_temperature: = -1.0
var fuse_sound: = "core:fuse_beep"

var is_armed: = false
var fuse_remaining: = 0.0
var has_detonated: = false
var _beep_timer: = 0.0


func get_component_type() -> StringName:
	return &"explosive"


func is_activatable() -> bool:
	return true


func configure(component_params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	params = ExplosionParams.from_dict(component_params)
	fuse = maxf(float(component_params.get("fuse", fuse)), 0.0)
	var chain: Variant = component_params.get("chain_fuse")
	if chain is Array and chain.size() == 2:
		chain_fuse = Vector2(maxf(float(chain[0]), 0.0), maxf(float(chain[1]), 0.0))
	detonate_on_depleted = bool(component_params.get("detonate_on_depleted", true))
	detonate_temperature = float(component_params.get("detonate_temperature", -1.0))
	fuse_sound = str(component_params.get("fuse_sound", fuse_sound))
	set_physics_process(false)


func _ready() -> void :
	var health: = entity.get_health()
	if health:
		health.depleted.connect(_on_depleted)
	var thermal: = entity.get_thermal()
	if thermal and detonate_temperature > 0.0:
		thermal.temperature_changed.connect(_on_temperature_changed)
	if is_armed:
		set_physics_process(true)


func on_activate(pressed: bool) -> void :
	if pressed:
		arm(fuse)



func arm(seconds: float) -> void :
	if has_detonated:
		return
	if is_armed and fuse_remaining <= seconds:
		return
	is_armed = true
	fuse_remaining = seconds
	_beep_timer = 0.0
	set_physics_process(true)
	if entity and entity.visual:
		entity.visual.armed = true
	armed.emit(seconds)


func disarm() -> void :
	is_armed = false
	set_physics_process(false)
	if entity and entity.visual:
		entity.visual.armed = false


func _physics_process(delta: float) -> void :
	if not is_armed:
		set_physics_process(false)
		return
	fuse_remaining -= delta
	_beep_timer -= delta
	if _beep_timer <= 0.0 and fuse_remaining > 0.15:

		_beep_timer = clampf(fuse_remaining * 0.25, 0.08, 0.5)
		Audio.play(fuse_sound, entity.global_position, -10.0)
		entity.visual.blink = not entity.visual.blink
	if fuse_remaining <= 0.0:
		detonate()





func detonate() -> void :
	if has_detonated or entity == null or entity.world == null:
		return
	if not entity.world.explosions.request(entity.global_position, params, entity):
		if not is_armed or fuse_remaining > 0.0:
			entity.world.explosions.note_deferred_detonation()
		is_armed = true
		fuse_remaining = 0.0
		set_physics_process(true)
		return
	has_detonated = true
	is_armed = false
	set_physics_process(false)
	detonated.emit()
	var destructible: = entity.get_component(&"destructible") as DestructibleComponent
	if destructible:
		destructible.destroy(true)
	else:
		entity.request_destroy()


func _on_depleted(_info: DamageInfo) -> void :
	if detonate_on_depleted:
		arm(randf_range(chain_fuse.x, chain_fuse.y))


func _on_temperature_changed(temperature: float) -> void :
	if temperature >= detonate_temperature:
		arm(randf_range(chain_fuse.x, chain_fuse.y) + 0.4)


func serialize_state() -> Dictionary:
	return {"armed": true, "fuse": fuse_remaining} if is_armed else {}


func deserialize_state(data: Dictionary) -> void :
	if bool(data.get("armed", false)):
		arm(maxf(float(data.get("fuse", fuse)), 0.0))


func get_inspector_lines() -> PackedStringArray:
	if is_armed:
		return PackedStringArray(["Explosive: ARMED (%.1f s)" % maxf(fuse_remaining, 0.0)])
	return PackedStringArray(["Explosive: %.0f px radius · fuse %.1f s" % [params.radius, fuse]])
