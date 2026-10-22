extends Node



const QUICKSAVE_SLOT: = "quicksave"

var _save_manager: = SaveManager.new()

@onready var world: SandboxWorld = $World
@onready var interaction: InteractionController = $Interaction
@onready var hud: Hud = $HUD


func _enter_tree() -> void :
	InputBindings.register_defaults()


func _ready() -> void :
	world.setup(EntityFactory.new(Content.registry))
	interaction.setup(world, Content.registry)
	hud.setup(world, interaction, Content.registry)
	hud.save_requested.connect(quick_save)
	hud.load_requested.connect(quick_load)
	_report_content_status()


func _unhandled_input(event: InputEvent) -> void :
	if event.is_action_pressed("toggle_pause"):
		GameState.toggle_paused()
	elif event.is_action_pressed("toggle_spawn_menu"):
		hud.toggle_spawn_menu()
	elif event.is_action_pressed("toggle_grid"):
		world.toggle_grid()
	elif event.is_action_pressed("toggle_help"):
		hud.toggle_help()
	elif event.is_action_pressed("quick_save"):
		quick_save()
	elif event.is_action_pressed("quick_load"):
		quick_load()
	else:
		return
	get_viewport().set_input_as_handled()


func quick_save() -> void :
	var error: = _save_manager.save_to_slot(QUICKSAVE_SLOT, world, world.camera, Content.registry)
	if error == OK:
		hud.show_message("Saved %d object(s)." % world.get_entity_count())
	else:
		hud.show_message("Save failed: %s" % _save_manager.last_error_message, true)


func quick_load() -> void :
	interaction.end_drag()
	interaction.cancel_spawn()
	var result: = _save_manager.load_from_slot(QUICKSAVE_SLOT, world, world.camera, Content.registry)
	hud.show_message(result.message, not result.ok)


func _report_content_status() -> void :
	if not Content.load_errors.is_empty():
		hud.show_message("%d content error(s) while loading mods; see the output log." % Content.load_errors.size(), true)
