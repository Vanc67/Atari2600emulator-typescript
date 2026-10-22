class_name InputBindings
extends RefCounted






static func register_defaults() -> void :
	_bind("camera_left", [_key(KEY_A), _key(KEY_LEFT)])
	_bind("camera_right", [_key(KEY_D), _key(KEY_RIGHT)])
	_bind("camera_up", [_key(KEY_W), _key(KEY_UP)])
	_bind("camera_down", [_key(KEY_S), _key(KEY_DOWN)])
	_bind("camera_zoom_in", [_key(KEY_EQUAL), _key(KEY_KP_ADD)])
	_bind("camera_zoom_out", [_key(KEY_MINUS), _key(KEY_KP_SUBTRACT)])
	_bind("camera_reset", [_key(KEY_HOME)])

	_bind("rotate_ccw", [_key(KEY_Q)])
	_bind("rotate_cw", [_key(KEY_E)])
	_bind("toggle_freeze", [_key(KEY_F)])
	_bind("delete_selected", [_key(KEY_DELETE), _key(KEY_BACKSPACE), _key(KEY_X)])
	_bind("duplicate_selected", [_key(KEY_D, true)])
	_bind("heal_selected", [_key_shift(KEY_R)])
	_bind("reload_selected", [_key(KEY_R)])

	_bind("activate_selected", [_key(KEY_T)])
	for index in 9:
		_bind("tool_%d" % (index + 1), [_key((KEY_1 + index) as Key)])
	_bind("cancel", [_key(KEY_ESCAPE)])

	_bind("toggle_pause", [_key(KEY_SPACE), _key(KEY_P)])
	_bind("toggle_spawn_menu", [_key(KEY_TAB)])
	_bind("toggle_grid", [_key(KEY_G)])
	_bind("toggle_help", [_key(KEY_H), _key(KEY_F1)])
	_bind("quick_save", [_key(KEY_F5), _key(KEY_S, true)])
	_bind("quick_load", [_key(KEY_F9), _key(KEY_O, true)])


static func _bind(action: StringName, events: Array) -> void :
	if InputMap.has_action(action):
		return
	InputMap.add_action(action)
	for event: InputEvent in events:
		InputMap.action_add_event(action, event)


static func _key_shift(keycode: Key) -> InputEventKey:
	var event: = _key(keycode)
	event.shift_pressed = true
	return event



static func _key(keycode: Key, with_command: = false) -> InputEventKey:
	var event: = InputEventKey.new()
	event.physical_keycode = keycode
	event.command_or_control_autoremap = with_command
	return event
