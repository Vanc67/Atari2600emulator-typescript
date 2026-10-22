extends Node



const MAIN_SCENE: = preload("res://scenes/main.tscn")


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(11)
	var args: = OS.get_cmdline_user_args()
	var output_path: = args[0] if not args.is_empty() else "user://weapons.png"
	var main: = MAIN_SCENE.instantiate()
	add_child(main)
	await get_tree().physics_frame
	var world: SandboxWorld = main.world
	var interaction: InteractionController = main.interaction
	(main.hud as Hud).toggle_spawn_menu()


	for index in 9:
		world.spawn_entity("core:steel_box", Vector2(-600 + index * 64.0, -232)).set_frozen(true)
	var x: = -600.0
	for id in ["core:pistol", "core:revolver", "core:smg", "core:assault_rifle", "core:shotgun"]:
		world.spawn_entity(id, Vector2(x + 20.0, -282))
		x += 110.0 if id in ["core:pistol", "core:revolver"] else 130.0
	x = -560.0
	for id in ["core:baseball_bat", "core:hammer", "core:crowbar", "core:metal_pipe"]:
		world.spawn_entity(id, Vector2(x, -300))
		x += 125.0
	world.spawn_entity("core:explosive_barrel", Vector2(-420, -42))
	world.spawn_entity("core:small_bomb", Vector2(-340, -13))
	world.spawn_entity("core:large_bomb", Vector2(-260, -20))
	for index in 2:
		world.spawn_entity("core:wooden_box", Vector2(-100 + index * 66.0, -32))
	var glass: = world.spawn_entity("core:glass_panel", Vector2(330, -66))
	glass.set_frozen(true)
	var human: = world.spawn_assembly("core:human", Vector2(520, -110))
	for part in human.get_parts():
		part.set_frozen(true)
	for index in 120:
		await get_tree().physics_frame
	for entity in world.get_entities():
		if entity.get_type_id() == "core:wooden_box":
			SandboxTools.ignite(entity)
	var rifle: = world.spawn_entity("core:assault_rifle", Vector2(120, -80), 0.0)
	rifle.set_frozen(true)
	(rifle.get_component(&"magazine") as MagazineComponent).infinite = true
	for index in 120 * 2:
		await get_tree().physics_frame
	var bomb: = world.spawn_entity("core:small_bomb", Vector2(-260, -60))
	bomb.activate(true)
	interaction.select(rifle)
	interaction.set_tool(SandboxTools.Tool.IGNITE)
	rifle.activate(true)
	for index in 9:
		await get_tree().physics_frame
	world.camera.apply_state({"position": [0.0, -180.0], "zoom": 1.25})
	await get_tree().create_timer(0.5).timeout
	await RenderingServer.frame_post_draw
	get_tree().paused = true
	await RenderingServer.frame_post_draw
	var image: = get_viewport().get_texture().get_image()
	image.save_png(output_path)
	print("Saved ", output_path, " glass_alive=", is_instance_valid(glass), " burning=", world.thermal.get_burning_count())
	get_tree().quit()
