extends Node









signal paused_changed(is_paused: bool)
signal time_scale_changed(time_scale: float)

const GAME_VERSION: = "0.3.1"
const MIN_TIME_SCALE: = 0.1
const MAX_TIME_SCALE: = 2.0

var is_paused: = false
var time_scale: = 1.0




var simulation_time: = 0.0


func _ready() -> void :
	process_mode = Node.PROCESS_MODE_ALWAYS


func _physics_process(delta: float) -> void :
	if not get_tree().paused:
		simulation_time += delta


func set_paused(value: bool) -> void :
	if is_paused == value:
		return
	is_paused = value
	get_tree().paused = value
	paused_changed.emit(value)


func toggle_paused() -> void :
	set_paused( not is_paused)


func set_time_scale(value: float) -> void :
	value = clampf(value, MIN_TIME_SCALE, MAX_TIME_SCALE)
	if is_equal_approx(value, time_scale):
		return
	time_scale = value

	Engine.time_scale = value
	time_scale_changed.emit(value)
