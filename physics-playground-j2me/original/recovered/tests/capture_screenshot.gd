extends Node



const MAIN_SCENE: = preload("res://scenes/main.tscn")


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(7)
	var args: = OS.get_cmdline_user_args()
	var output_path: = args[0] if not args.is_empty() else "user://screenshot.png"
	var main: = MAIN_SCENE.instantiate()
	add_child(main)
	await get_tree().physics_frame
	var world: SandboxWorld = main.world
	var interaction: InteractionController = main.interaction


	for index in 3:
		world.spawn_entity("core:wooden_box", Vector2(420.0, -32.0 - index * 66.0))
	var healthy: = world.spawn_assembly("core:human", Vector2(-80, -110))
	var battered: = world.spawn_assembly("core:human", Vector2(180, -110))
	var dead: = world.spawn_assembly("core:human", Vector2(-330, -110))
	world.spawn_entity("core:steel_box", Vector2(-520, -32))
	world.spawn_entity("core:metal_barrel", Vector2(620, -42))
	for index in 120 * 2:
		await get_tree().physics_frame
	battered.get_part("upper_torso").get_health().apply_damage(110.0)
	battered.get_part("head").get_health().apply_damage(45.0)
	battered.get_part("thigh_right").get_health().apply_damage(100.0)
	battered.break_joint_by_name("elbow_right")
	dead.get_part("head").get_health().apply_damage(100.0)
	dead.break_joint_by_name("knee_left")
	var sphere: = world.spawn_entity("core:metal_sphere", Vector2(-700, -60))
	sphere.linear_velocity = Vector2(600, 0)
	for index in 120 * 2:
		await get_tree().physics_frame

	var with_panels: = args.size() > 1 and args[1] == "panels"
	if with_panels:
		interaction.select(battered.get_part("thigh_right"))
		((main.hud as Hud).get_node("%SpawnMenu").get_node("Content/CategoryBar").get_child(1) as Button).button_pressed = true
		world.camera.apply_state({"position": [80.0, -120.0], "zoom": 1.9})
	else:
		(main.hud as Hud).toggle_spawn_menu()
		world.camera.apply_state({"position": [40.0, -110.0], "zoom": 1.45})
	await get_tree().create_timer(0.6).timeout
	await RenderingServer.frame_post_draw
	var image: = get_viewport().get_texture().get_image()
	image.save_png(output_path)
	print("Saved ", output_path, " ", image.get_size(), " healthy=", healthy.controller.get_state_name(), 
		" battered=", battered.controller.get_state_name(), " dead=", dead.controller.get_state_name())
	get_tree().quit()
