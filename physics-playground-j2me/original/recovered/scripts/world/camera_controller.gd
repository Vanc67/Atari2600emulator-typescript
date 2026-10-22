class_name CameraController
extends Camera2D








@export var min_zoom: = 0.15
@export var max_zoom: = 3.0
@export var wheel_zoom_step: = 1.15
@export var keyboard_zoom_speed: = 1.8
@export var smoothing: = 14.0
@export var keyboard_pan_speed: = 1200.0
@export var trackpad_pan_speed: = 30.0
@export var home_position: = Vector2(0, -220)


var bounds: = Rect2(-100000, -100000, 200000, 200000)

var _target_position: Vector2
var _target_zoom: = 1.0
var _zoom_anchor_screen: = Vector2.ZERO
var _is_dragging_view: = false
var _last_tick_usec: = 0


func _ready() -> void :
	process_mode = Node.PROCESS_MODE_ALWAYS
	position_smoothing_enabled = false
	position = home_position
	_target_position = position
	_target_zoom = zoom.x
	_last_tick_usec = Time.get_ticks_usec()
	make_current()


func _unhandled_input(event: InputEvent) -> void :
	var mouse_button: = event as InputEventMouseButton
	if mouse_button:
		match mouse_button.button_index:
			MOUSE_BUTTON_WHEEL_UP:
				if mouse_button.pressed:
					zoom_toward(mouse_button.position, wheel_zoom_step)
					get_viewport().set_input_as_handled()
			MOUSE_BUTTON_WHEEL_DOWN:
				if mouse_button.pressed:
					zoom_toward(mouse_button.position, 1.0 / wheel_zoom_step)
					get_viewport().set_input_as_handled()
			MOUSE_BUTTON_MIDDLE, MOUSE_BUTTON_RIGHT:
				_is_dragging_view = mouse_button.pressed
				get_viewport().set_input_as_handled()
		return

	var motion: = event as InputEventMouseMotion
	if motion and _is_dragging_view:

		var world_delta: = motion.relative / zoom.x
		position -= world_delta
		_target_position -= world_delta
		_clamp_to_bounds()
		get_viewport().set_input_as_handled()
		return

	var magnify: = event as InputEventMagnifyGesture
	if magnify:
		zoom_toward(magnify.position, magnify.factor)
		get_viewport().set_input_as_handled()
		return

	var pan: = event as InputEventPanGesture
	if pan:
		_target_position += pan.delta * trackpad_pan_speed / zoom.x
		_clamp_to_bounds()
		get_viewport().set_input_as_handled()


func _process(_delta: float) -> void :
	var now: = Time.get_ticks_usec()
	var real_delta: = minf(float(now - _last_tick_usec) / 1000000.0, 0.1)
	_last_tick_usec = now

	if _is_dragging_view and not (Input.is_mouse_button_pressed(MOUSE_BUTTON_MIDDLE)
			or Input.is_mouse_button_pressed(MOUSE_BUTTON_RIGHT)):
		_is_dragging_view = false
	_apply_keyboard_input(real_delta)

	var blend: = 1.0 - exp( - smoothing * real_delta)
	if not is_equal_approx(zoom.x, _target_zoom):

		var next_zoom: = exp(lerpf(log(zoom.x), log(_target_zoom), blend))
		if absf(next_zoom - _target_zoom) < 0.0005:
			next_zoom = _target_zoom
		_set_zoom_keeping_anchor(next_zoom)
	position = position.lerp(_target_position, blend)



func zoom_toward(screen_position: Vector2, factor: float) -> void :
	_zoom_anchor_screen = screen_position
	_target_zoom = clampf(_target_zoom * factor, min_zoom, max_zoom)


func reset_view() -> void :
	_target_position = home_position
	_zoom_anchor_screen = get_viewport_rect().size * 0.5
	_target_zoom = 1.0


func screen_to_world(screen_position: Vector2) -> Vector2:
	return position + (screen_position - get_viewport_rect().size * 0.5) / zoom.x


func get_visible_world_rect() -> Rect2:
	var half_extent: = get_viewport_rect().size * 0.5 / zoom.x
	return Rect2(position - half_extent, half_extent * 2.0)


func serialize_state() -> Dictionary:
	return {"position": [_target_position.x, _target_position.y], "zoom": _target_zoom}


func apply_state(data: Dictionary) -> void :
	var saved_position: Variant = data.get("position")
	if saved_position is Array and saved_position.size() == 2:
		_target_position = Vector2(float(saved_position[0]), float(saved_position[1]))
	_zoom_anchor_screen = get_viewport_rect().size * 0.5
	_target_zoom = clampf(float(data.get("zoom", _target_zoom)), min_zoom, max_zoom)
	_clamp_to_bounds()


func _apply_keyboard_input(real_delta: float) -> void :

	if Input.is_key_pressed(KEY_META) or Input.is_key_pressed(KEY_CTRL):
		return
	var direction: = Input.get_vector("camera_left", "camera_right", "camera_up", "camera_down")
	if direction != Vector2.ZERO:
		_target_position += direction * keyboard_pan_speed * real_delta / zoom.x
		_clamp_to_bounds()
	var zoom_input: = Input.get_axis("camera_zoom_out", "camera_zoom_in")
	if zoom_input != 0.0:
		zoom_toward(get_viewport_rect().size * 0.5, pow(keyboard_zoom_speed, zoom_input * real_delta))
	if Input.is_action_just_pressed("camera_reset"):
		reset_view()


func _set_zoom_keeping_anchor(new_zoom: float) -> void :
	var world_before: = screen_to_world(_zoom_anchor_screen)
	zoom = Vector2.ONE * new_zoom
	var correction: = world_before - screen_to_world(_zoom_anchor_screen)
	position += correction
	_target_position += correction
	_clamp_to_bounds()


func _clamp_to_bounds() -> void :
	_target_position = _target_position.clamp(bounds.position, bounds.end)
	position = position.clamp(bounds.position, bounds.end)
