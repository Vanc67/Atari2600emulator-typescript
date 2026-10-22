class_name Hud
extends CanvasLayer




signal save_requested
signal load_requested

const TOAST_DURATION_MSEC: = 2600
const STATS_INTERVAL_MSEC: = 250
const HINT_DEFAULT: = "LMB drag/throw  ·  T fire/activate (hold)  ·  R reload  ·  Q/E rotate  ·  F freeze  ·  %s+D duplicate  ·  "\
+ "Shift+R heal  ·  Del/X delete  ·  1–9 tools  ·  MMB/RMB pan  ·  Wheel zoom  ·  Space pause  ·  Tab menu  ·  G grid  ·  H help"
const HINT_SPAWNING: = "LMB place  ·  Q/E rotate  ·  RMB/Esc stop placing"

var _world: SandboxWorld
var _interaction: InteractionController
var _clear_dialog: ConfirmationDialog
var _toast_expire_msec: = 0
var _next_stats_msec: = 0
var _hint_enabled: = true

@onready var _root: Control = $Root
@onready var _spawn_button: Button = %SpawnButton
@onready var _pause_button: Button = %PauseButton
@onready var _speed_slider: HSlider = %SpeedSlider
@onready var _speed_value: Label = %SpeedValue
@onready var _speed_reset: Button = %SpeedReset
@onready var _grid_button: Button = %GridButton
@onready var _clear_button: Button = %ClearButton
@onready var _save_button: Button = %SaveButton
@onready var _load_button: Button = %LoadButton
@onready var _count_label: Label = %CountLabel
@onready var _fps_label: Label = %FpsLabel
@onready var _spawn_menu: SpawnMenu = %SpawnMenu
@onready var _inspector: InspectorPanel = %Inspector
@onready var _toast: Label = %Toast
@onready var _paused_badge: Label = %PausedBadge
@onready var _hint_label: Label = %HintLabel
@onready var _tool_row: HBoxContainer = %ToolRow

var _tool_buttons: Array[Button] = []
var _damage_amount: SpinBox


func _ready() -> void :
	var theme: = UiTheme.build()
	_root.theme = theme
	_disable_focus(_root)
	_toast.visible = false
	_paused_badge.visible = false

	_clear_dialog = ConfirmationDialog.new()
	_clear_dialog.theme = theme
	_clear_dialog.title = "Clear World"
	_clear_dialog.dialog_text = "Remove every object from the world?"
	_clear_dialog.ok_button_text = "Clear"
	add_child(_clear_dialog)


func setup(world: SandboxWorld, interaction: InteractionController, registry: EntityRegistry) -> void :
	_world = world
	_interaction = interaction
	_spawn_menu.setup(registry, interaction)
	_inspector.setup(interaction)

	_spawn_button.button_pressed = _spawn_menu.visible
	_spawn_button.toggled.connect( func(pressed: bool) -> void : _spawn_menu.visible = pressed)
	_pause_button.pressed.connect(GameState.toggle_paused)
	_speed_slider.min_value = GameState.MIN_TIME_SCALE
	_speed_slider.max_value = GameState.MAX_TIME_SCALE
	_speed_slider.set_value_no_signal(GameState.time_scale)
	_speed_slider.value_changed.connect(GameState.set_time_scale)
	_speed_reset.pressed.connect(GameState.set_time_scale.bind(1.0))
	_grid_button.button_pressed = world.is_grid_visible()
	_grid_button.toggled.connect(world.set_grid_visible)
	_clear_button.pressed.connect(_clear_dialog.popup_centered)
	_clear_dialog.confirmed.connect(_on_clear_confirmed)
	_save_button.pressed.connect(save_requested.emit)
	_load_button.pressed.connect(load_requested.emit)

	GameState.paused_changed.connect(_on_paused_changed)
	GameState.time_scale_changed.connect(_on_time_scale_changed)
	world.entity_count_changed.connect(_on_entity_count_changed)
	world.assembly_spawned.connect( func(_assembly: EntityAssembly) -> void : _on_entity_count_changed(0))
	world.assembly_removed.connect( func(_assembly: EntityAssembly) -> void : _on_entity_count_changed(0))
	world.grid_visibility_changed.connect(_grid_button.set_pressed_no_signal)
	interaction.spawn_mode_changed.connect( func(_type_id: String) -> void : _update_hint())
	interaction.message_requested.connect(show_message)
	interaction.tool_changed.connect(_on_tool_changed)
	_build_tool_bar()

	_on_paused_changed(GameState.is_paused)
	_on_time_scale_changed(GameState.time_scale)
	_on_entity_count_changed(world.get_entity_count())
	_update_hint()


func _process(_delta: float) -> void :
	var now: = Time.get_ticks_msec()
	if now >= _next_stats_msec:
		_next_stats_msec = now + STATS_INTERVAL_MSEC
		_fps_label.text = "FPS: %d" % Engine.get_frames_per_second()
	if _toast.visible and now >= _toast_expire_msec:
		_toast.visible = false


func show_message(text: String, is_error: = false) -> void :
	_toast.text = text
	_toast.add_theme_color_override("font_color", UiTheme.ERROR if is_error else UiTheme.TEXT)
	_toast.visible = true
	_toast_expire_msec = Time.get_ticks_msec() + TOAST_DURATION_MSEC


func toggle_spawn_menu() -> void :
	_spawn_button.button_pressed = not _spawn_button.button_pressed


func toggle_help() -> void :
	_hint_enabled = not _hint_enabled
	_update_hint()


func _build_tool_bar() -> void :
	var group: = ButtonGroup.new()
	for index in SandboxTools.TOOL_NAMES.size():
		var button: = Button.new()
		button.text = "%d %s" % [index + 1, SandboxTools.TOOL_NAMES[index]]
		button.tooltip_text = SandboxTools.TOOL_HINTS[index]
		button.toggle_mode = true
		button.button_group = group
		button.focus_mode = Control.FOCUS_NONE
		button.pressed.connect(_interaction.set_tool.bind(index))
		_tool_row.add_child(button)
		_tool_buttons.append(button)
	_damage_amount = SpinBox.new()
	_damage_amount.min_value = 1
	_damage_amount.max_value = 500
	_damage_amount.step = 5
	_damage_amount.value = _interaction.tool_damage_amount
	_damage_amount.tooltip_text = "Damage dealt by the Damage tool"
	_damage_amount.custom_minimum_size = Vector2(72, 0)
	_damage_amount.value_changed.connect( func(value: float) -> void : _interaction.tool_damage_amount = value)
	_damage_amount.get_line_edit().focus_mode = Control.FOCUS_CLICK
	_tool_row.add_child(_damage_amount)
	_on_tool_changed(_interaction.current_tool)


func _on_tool_changed(tool: int) -> void :

	for index in _tool_buttons.size():
		_tool_buttons[index].set_pressed_no_signal(index == tool)
	_update_hint()


func _update_hint() -> void :
	_hint_label.visible = _hint_enabled
	if _interaction and _interaction.is_spawning():
		_hint_label.text = HINT_SPAWNING
	elif _interaction and _interaction.current_tool != SandboxTools.Tool.DRAG:
		_hint_label.text = SandboxTools.TOOL_HINTS[_interaction.current_tool] + "  ·  1 returns to Drag  ·  T fire/activate  ·  R reload"
	else:
		_hint_label.text = HINT_DEFAULT % ("Cmd" if OS.get_name() == "macOS" else "Ctrl")


func _on_clear_confirmed() -> void :
	_world.clear_entities()
	show_message("World cleared.")


func _on_paused_changed(is_paused: bool) -> void :
	_pause_button.text = "Resume" if is_paused else "Pause"
	_paused_badge.visible = is_paused


func _on_time_scale_changed(time_scale: float) -> void :
	_speed_slider.set_value_no_signal(time_scale)
	_speed_value.text = "%.2f×" % time_scale


func _on_entity_count_changed(_count: int) -> void :
	_count_label.text = "Objects: %d  ·  Bodies: %d" % [_world.get_object_count(), _world.get_entity_count()]




func _disable_focus(node: Node) -> void :
	if node is Control:
		(node as Control).focus_mode = Control.FOCUS_NONE
	for child in node.get_children():
		_disable_focus(child)
