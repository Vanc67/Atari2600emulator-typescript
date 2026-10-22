class_name FirearmComponent
extends EntityComponent























const MAX_RECOIL_SPEED: = 900.0
const MAX_RECOIL_SPIN: = 25.0

signal fired(shots: int)

var projectile_id: = ""
var fire_rate: = 6.0
var automatic: = false
var speed: = -1.0
var damage: = -1.0
var pellets: = 1
var spread_degrees: = 1.5
var recoil: = 2.0
var muzzle: = Vector2.INF
var muzzle_flash: = 14.0
var sound: = "core:gunshot_light"
var dry_sound: = "core:dry_fire"

var trigger_held: = false
var shots_fired: = 0

var _projectile: ProjectileDefinition
var _magazine: MagazineComponent
var _cooldown: = 0.0


func get_component_type() -> StringName:
	return &"firearm"


func is_activatable() -> bool:
	return true


func configure(params: Dictionary, definition: EntityDefinition, _material: MaterialDefinition) -> void :
	projectile_id = str(params.get("projectile", ""))
	fire_rate = clampf(float(params.get("fire_rate", fire_rate)), 0.1, 60.0)
	automatic = bool(params.get("automatic", false))
	speed = float(params.get("speed", -1.0))
	damage = float(params.get("damage", -1.0))
	pellets = clampi(int(params.get("pellets", 1)), 1, 32)
	spread_degrees = clampf(float(params.get("spread", spread_degrees)), 0.0, 90.0)
	recoil = clampf(float(params.get("recoil", recoil)), 0.0, 1000.0)
	var muzzle_data: Variant = params.get("muzzle")
	if muzzle_data is Array and muzzle_data.size() == 2:
		muzzle = Vector2(float(muzzle_data[0]), float(muzzle_data[1]))
	elif definition:
		muzzle = Vector2(definition.get_bounding_size().x * 0.5, 0.0)
	else:
		muzzle = Vector2.ZERO
	muzzle_flash = maxf(float(params.get("muzzle_flash", muzzle_flash)), 0.0)
	sound = str(params.get("sound", sound))
	dry_sound = str(params.get("dry_sound", dry_sound))
	_projectile = Content.registry.get_projectile(projectile_id)
	set_physics_process(false)


func _ready() -> void :
	_magazine = entity.get_component(&"magazine") as MagazineComponent


func get_projectile() -> ProjectileDefinition:
	return _projectile


func on_activate(pressed: bool) -> void :
	trigger_held = pressed
	if not pressed:
		return

	if _cooldown <= 0.0:
		try_fire()
	if automatic or _cooldown > 0.0:
		set_physics_process(true)


func on_reload() -> void :
	if _magazine:
		_magazine.start_reload()


func _physics_process(delta: float) -> void :
	_cooldown = maxf(_cooldown - delta, 0.0)
	if _cooldown > 0.0:
		return
	if trigger_held and automatic:
		try_fire()
	if not (trigger_held and automatic) and _cooldown <= 0.0:
		set_physics_process(false)



func try_fire() -> bool:
	if _cooldown > 0.0 or entity == null or entity.world == null or _projectile == null:
		return false
	if entity.get_tree().paused:
		return false
	_cooldown = 1.0 / fire_rate
	set_physics_process(true)
	if _magazine and not _magazine.can_fire():
		if _magazine.is_reloading:
			return false
		Audio.play(dry_sound, entity.global_position, -6.0)
		return false
	if _magazine and not _magazine.consume():
		return false
	_emit_projectiles()
	shots_fired += 1
	fired.emit(pellets)
	return true


func _emit_projectiles() -> void :
	var transform: = entity.global_transform
	var origin: = transform * muzzle
	var forward: = transform.x.normalized()
	var muzzle_speed: = (speed if speed > 0.0 else _projectile.speed) * WorldUnits.PIXELS_PER_METER
	var shot_damage: = damage if damage >= 0.0 else _projectile.damage
	var half_spread: = deg_to_rad(spread_degrees) * 0.5
	for index in pellets:
		var direction: = forward.rotated(randf_range( - half_spread, half_spread))

		var pellet_speed: = muzzle_speed * (randf_range(0.92, 1.0) if pellets > 1 else 1.0)
		entity.world.projectiles.fire(_projectile, origin, direction * pellet_speed + entity.linear_velocity, 
			shot_damage, entity)
	if muzzle_flash > 0.0:
		entity.world.effects.muzzle_flash(origin, forward, muzzle_flash)
	Audio.play(sound, origin)
	_apply_recoil(forward, origin)








func _apply_recoil(forward: Vector2, origin: Vector2) -> void :
	if entity.freeze or recoil <= 0.0:
		return
	var state: = PhysicsServer2D.body_get_direct_state(entity.get_rid())
	if state == null:
		return
	var impulse: = minf(recoil * WorldUnits.PIXELS_PER_METER, entity.mass * MAX_RECOIL_SPEED)
	state.apply_impulse( - forward * impulse, origin - entity.global_position)
	state.angular_velocity = clampf(state.angular_velocity, - MAX_RECOIL_SPIN, MAX_RECOIL_SPIN)
	state.linear_velocity = state.linear_velocity.limit_length(MAX_RECOIL_SPEED * 2.0)
	entity.sleeping = false


func serialize_state() -> Dictionary:
	return {"trigger_held": true} if trigger_held else {}


func deserialize_state(data: Dictionary) -> void :

	if bool(data.get("trigger_held", false)):
		trigger_held = true
		set_physics_process(true)


func get_inspector_lines() -> PackedStringArray:
	var mode: = "automatic" if automatic else "semi-automatic"
	var shot_damage: = damage if damage >= 0.0 else (_projectile.damage if _projectile else 0.0)
	var line: = "%s · %.0f rounds/min · %.0f dmg" % [mode.capitalize(), fire_rate * 60.0, shot_damage]
	if pellets > 1:
		line += " × %d" % pellets
	return PackedStringArray([line])
