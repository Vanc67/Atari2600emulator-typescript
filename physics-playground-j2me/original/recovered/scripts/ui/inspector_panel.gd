class_name InspectorPanel
extends PanelContainer





const REFRESH_INTERVAL_MSEC: = 150

var _interaction: InteractionController
var _entity: SandboxEntity = null
var _next_refresh_msec: = 0

@onready var _title: Label = %InspectorTitle
@onready var _details: Label = %InspectorDetails
@onready var _freeze_button: Button = %FreezeButton
@onready var _duplicate_button: Button = %DuplicateButton
@onready var _delete_button: Button = %DeleteButton
@onready var _heal_button: Button = %HealButton


func setup(interaction: InteractionController) -> void :
	_interaction = interaction
	_interaction.selection_changed.connect(_on_selection_changed)
	_freeze_button.pressed.connect(_interaction.toggle_freeze_selected)
	_duplicate_button.pressed.connect(_interaction.duplicate_selected)
	_delete_button.pressed.connect(_interaction.delete_selected)
	_heal_button.pressed.connect(_interaction.heal_selected)
	visible = false


func _process(_delta: float) -> void :
	if not visible:
		return
	if not is_instance_valid(_entity):
		_on_selection_changed(null)
		return
	if Time.get_ticks_msec() >= _next_refresh_msec:
		_refresh()


func _on_selection_changed(entity: SandboxEntity) -> void :
	_entity = entity
	visible = entity != null
	if entity:
		_refresh()


func _refresh() -> void :
	_next_refresh_msec = Time.get_ticks_msec() + REFRESH_INTERVAL_MSEC
	var physics_state: = "Frozen" if _entity.is_frozen() else ("Sleeping" if _entity.sleeping else "Active")
	var lines: = PackedStringArray()
	if _entity.is_assembly_part():
		_title.text = _entity.assembly.get_display_name()
		lines.append_array(_entity.assembly.get_inspector_lines(_entity))
	else:
		_title.text = _entity.get_display_name()
	lines.append("%s  ·  #%d" % [_entity.get_type_id(), _entity.entity_uid])
	var health: = _entity.get_health()
	if health:
		lines.append_array(health.get_inspector_lines())
	lines.append("Material: %s" % _entity.material_definition.display_name)
	lines.append("Mass: %.1f kg" % _entity.mass)
	lines.append("Velocity: %.2f m/s" % WorldUnits.pixels_to_meters(_entity.linear_velocity.length()))
	lines.append("Physics: %s" % physics_state)
	for component in _entity.get_components():
		if component != health:
			lines.append_array(component.get_inspector_lines())
	_details.text = "\n".join(lines)
	_freeze_button.text = "Unfreeze" if _entity.is_frozen() else "Freeze"
