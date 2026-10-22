class_name SpawnMenu
extends PanelContainer



const ALL_CATEGORY: = "All"
const ITEM_HEIGHT: = 52.0
const ICON_SIZE: = 40.0

var _registry: EntityRegistry
var _interaction: InteractionController
var _current_category: = ""
var _category_group: = ButtonGroup.new()
var _item_buttons: Dictionary = {}

@onready var _category_bar: HFlowContainer = %CategoryBar
@onready var _item_list: VBoxContainer = %ItemList
@onready var _description: Label = %Description


func setup(registry: EntityRegistry, interaction: InteractionController) -> void :
	_registry = registry
	_interaction = interaction
	_interaction.spawn_mode_changed.connect(_on_spawn_mode_changed)
	_build_categories()


func _build_categories() -> void :
	for child in _category_bar.get_children():
		child.queue_free()
	var categories: Array[String] = [ALL_CATEGORY]
	categories.append_array(_registry.get_categories())
	for category in categories:
		var button: = Button.new()
		button.text = category
		button.toggle_mode = true
		button.button_group = _category_group
		button.focus_mode = Control.FOCUS_NONE
		button.pressed.connect(_show_category.bind(category))
		_category_bar.add_child(button)
	if not categories.is_empty():
		(_category_bar.get_child(0) as Button).button_pressed = true
		_show_category(categories[0])


func _show_category(category: String) -> void :
	_current_category = category
	for child in _item_list.get_children():
		child.queue_free()
	_item_buttons.clear()
	var definitions: = _registry.get_spawnable_entities() if category == ALL_CATEGORY\
	else _registry.get_entities_in_category(category)
	for definition in definitions:
		var button: = _create_item_button(definition)
		_item_list.add_child(button)
		_item_buttons[definition.id] = button
	_on_spawn_mode_changed(_interaction.get_spawn_type_id())


func _create_item_button(definition: EntityDefinition) -> Button:
	var button: = Button.new()
	button.toggle_mode = true
	button.focus_mode = Control.FOCUS_NONE
	button.custom_minimum_size = Vector2(0, ITEM_HEIGHT)
	button.tooltip_text = definition.description
	button.pressed.connect(_on_item_pressed.bind(definition.id))
	button.mouse_entered.connect(_on_item_hovered.bind(definition))
	button.mouse_exited.connect(_on_item_hovered.bind(null))

	var row: = HBoxContainer.new()
	row.mouse_filter = Control.MOUSE_FILTER_IGNORE
	row.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
	row.offset_left = 6
	row.offset_right = -6
	row.add_theme_constant_override("separation", 10)
	button.add_child(row)

	var icon: = EntityPreviewIcon.new()
	icon.custom_minimum_size = Vector2.ONE * ICON_SIZE
	icon.size_flags_vertical = Control.SIZE_SHRINK_CENTER
	icon.mouse_filter = Control.MOUSE_FILTER_IGNORE
	icon.definition = definition
	row.add_child(icon)

	var text_column: = VBoxContainer.new()
	text_column.mouse_filter = Control.MOUSE_FILTER_IGNORE
	text_column.size_flags_vertical = Control.SIZE_SHRINK_CENTER
	text_column.add_theme_constant_override("separation", 0)
	row.add_child(text_column)

	var name_label: = Label.new()
	name_label.text = definition.display_name
	text_column.add_child(name_label)

	var detail_label: = Label.new()
	detail_label.theme_type_variation = &"DimLabel"
	if definition.is_assembly():
		detail_label.text = "%d parts · %.0f kg" % [definition.assembly.parts.size(), definition.compute_mass(null)]
	else:
		var material: = _registry.get_material(definition.material_id)
		detail_label.text = "%s · %.1f kg" % [material.display_name, definition.compute_mass(material)]
	text_column.add_child(detail_label)
	return button


func _on_item_pressed(type_id: String) -> void :
	if _interaction.get_spawn_type_id() == type_id:
		_interaction.cancel_spawn()
	else:
		_interaction.begin_spawn(type_id)


func _on_item_hovered(definition: EntityDefinition) -> void :
	if definition:
		_description.text = definition.description
	else:
		_description.text = "Pick an object, then click in the world to place it."


func _on_spawn_mode_changed(type_id: String) -> void :
	for id: String in _item_buttons:
		(_item_buttons[id] as Button).set_pressed_no_signal(id == type_id)
