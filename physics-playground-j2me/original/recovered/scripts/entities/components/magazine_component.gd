class_name MagazineComponent
extends EntityComponent









signal ammo_changed(rounds: int, capacity: int)
signal reload_started
signal reload_finished

var capacity: = 12
var rounds: = 12
var reload_time: = 1.5
var per_round: = false
var infinite: = false
var reload_sound: = "core:reload"

var is_reloading: = false
var _reload_progress: = 0.0


func get_component_type() -> StringName:
	return &"magazine"


func configure(params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	capacity = maxi(int(params.get("capacity", capacity)), 1)
	rounds = capacity
	reload_time = maxf(float(params.get("reload_time", reload_time)), 0.0)
	per_round = bool(params.get("per_round", false))
	infinite = bool(params.get("infinite", false))
	reload_sound = str(params.get("reload_sound", reload_sound))
	set_physics_process(false)


func has_rounds(count: = 1) -> bool:
	return infinite or rounds >= count



func consume() -> bool:
	if not has_rounds():
		return false
	if is_reloading and per_round:
		_stop_reload()
	if not infinite:
		rounds -= 1
		ammo_changed.emit(rounds, capacity)
	return true


func can_fire() -> bool:
	return not (is_reloading and not per_round) and has_rounds()


func start_reload() -> void :
	if is_reloading or rounds >= capacity or infinite:
		return
	is_reloading = true
	_reload_progress = 0.0
	set_physics_process(true)
	_play(reload_sound)
	reload_started.emit()


func on_reload() -> void :
	start_reload()


func _physics_process(delta: float) -> void :
	if not is_reloading:
		set_physics_process(false)
		return
	_reload_progress += delta
	if _reload_progress < reload_time:
		return
	if per_round:
		_reload_progress = 0.0
		rounds = mini(rounds + 1, capacity)
		ammo_changed.emit(rounds, capacity)
		if rounds < capacity:
			_play(reload_sound)
			return
	else:
		rounds = capacity
		ammo_changed.emit(rounds, capacity)
	_stop_reload()
	reload_finished.emit()


func _stop_reload() -> void :
	is_reloading = false
	_reload_progress = 0.0
	set_physics_process(false)


func _play(sound_id: String) -> void :
	if entity and entity.is_inside_tree():
		Audio.play(sound_id, entity.global_position, -3.0)


func serialize_state() -> Dictionary:
	var state: = {}
	if rounds != capacity:
		state["rounds"] = rounds
	if is_reloading:
		state["reload_progress"] = _reload_progress
	return state


func deserialize_state(data: Dictionary) -> void :
	rounds = clampi(int(data.get("rounds", rounds)), 0, capacity)
	if data.has("reload_progress"):
		is_reloading = true
		_reload_progress = float(data["reload_progress"])
		set_physics_process(true)
	ammo_changed.emit(rounds, capacity)


func get_inspector_lines() -> PackedStringArray:
	var line: = "Ammo: ∞" if infinite else "Ammo: %d / %d" % [rounds, capacity]
	if is_reloading:
		line += "  (reloading)"
	return PackedStringArray([line])
