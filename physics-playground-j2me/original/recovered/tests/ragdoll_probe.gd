extends Node



const MAIN_SCENE: = preload("res://scenes/main.tscn")

func _ready() -> void :
	_run.call_deferred()

func _frames(n: int) -> void :
	for i in n:
		await get_tree().physics_frame

func _report(label: String, human: EntityAssembly) -> void :
	var strains: = []
	for j in human.get_joints():
		strains.append("%s=%.1f%s" % [j.get_name(), j.peak_strain, "X" if j.broken else ""])
		j.peak_strain = 0.0
	var dmg: = []
	for p in human.get_parts():
		var h: = p.get_health()
		if h.is_damaged():
			dmg.append("%s:%d" % [p.assembly_part_name, roundi(h.max_health - h.current_health)])
	print(label, " state=", human.controller.get_state_name(), " peak strain: ", ", ".join(strains))
	print("   damage: ", dmg)

func _max_speed(human: EntityAssembly) -> Array:
	var lin: = 0.0; var ang: = 0.0
	for p in human.get_parts():
		lin = maxf(lin, p.linear_velocity.length());ang = maxf(ang, absf(p.angular_velocity))
	return [lin, ang]

func _run() -> void :
	var main: = MAIN_SCENE.instantiate()
	add_child(main)
	await _frames(2)
	var world: SandboxWorld = main.world
	var args: = OS.get_cmdline_user_args()
	seed(int(args[1]) if args.size() > 1 else 1)
	if not args.is_empty():
		PhysicsServer2D.space_set_param(world.get_world_2d().space, PhysicsServer2D.SPACE_PARAM_SOLVER_ITERATIONS, int(args[0]))
		print("solver iterations: ", args[0])

	var human: = world.spawn_assembly("core:human", Vector2(0, -110))
	print("parts=", human.get_part_count(), " joints=", human.get_joints().size(), " bodies=", world.get_entity_count())
	await _frames(120 * 4)
	_report("settle 4s", human)
	print("   max speed after settle (px/s, rad/s): ", _max_speed(human))
	await _frames(120 * 2)
	print("   max speed after 6s: ", _max_speed(human), " sleeping=", human.get_parts().filter( func(p): return p.sleeping).size())

	for j in human.get_joints():
		if j.definition.has_angle_limits:
			var rel: = rad_to_deg(wrapf(j.body_b.rotation - j.body_a.rotation - j.definition.rest_angle, - PI, PI))
			print("   ", j.get_name(), " angle ", snappedf(rel, 0.1), " limits ", snappedf(rad_to_deg(j.definition.lower_angle), 1), "..", snappedf(rad_to_deg(j.definition.upper_angle), 1))


	var hand: = human.get_part("forearm_right")
	var grab: = hand.global_transform * Vector2(0, 12)
	human.begin_drag_protection()
	var drag: = DragHandle.new(hand, grab)
	drag.target_position = Vector2(0, -500)
	for i in 120 * 3:
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
	print("drag hand: error=", drag.get_anchor_world_position().distance_to(drag.target_position), " pelvis y=", human.get_part("pelvis").global_position.y)
	_report("   drag hold", human)

	for i in 60:
		drag.target_position = Vector2(400 * sin(i / 60.0 * TAU), -500)
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
	_report("   drag swing", human)

	for i in 15:
		drag.target_position += Vector2(60, -20)
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
	human.end_drag_protection()
	print("   release speed: ", _max_speed(human))
	await _frames(120 * 3)
	_report("after throw+land", human)


	world.clear_entities()
	var h2: = world.spawn_assembly("core:human", Vector2(0, -1000))
	await _frames(120 * 3)
	_report("drop 9m", h2)
	world.clear_entities()
	var h3: = world.spawn_assembly("core:human", Vector2(0, -2500))
	await _frames(120 * 4)
	_report("drop 24m", h3)


	world.clear_entities()
	var h4: = world.spawn_assembly("core:human", Vector2(0, -110))
	await _frames(10)
	var s: = world.spawn_entity("core:metal_sphere", Vector2(-300, -196))
	s.linear_velocity = Vector2(1800, 0)
	await _frames(60)
	_report("sphere 18m/s at head", h4)
	await _frames(240)

	var block: = world.spawn_entity("core:concrete_block", Vector2(0, -900))
	await _frames(240)
	_report("concrete from 9m", h4)


	world.clear_entities()
	await _frames(2)
	for i in 20:
		world.spawn_assembly("core:human", Vector2(-1900 + i * 200, -400 - (i % 3) * 300))
	await _frames(10)
	var t0: = Time.get_ticks_usec()
	for i in 120 * 3:
		await get_tree().physics_frame
	var per_frame: = float(Time.get_ticks_usec() - t0) / (120 * 3) / 1000.0
	print("20 ragdolls falling: %.2f ms per physics frame (bodies=%d)" % [per_frame, world.get_entity_count()])
	t0 = Time.get_ticks_usec()
	for i in 120 * 2:
		await get_tree().physics_frame
	print("20 ragdolls resting: %.2f ms per physics frame" % (float(Time.get_ticks_usec() - t0) / 240 / 1000.0))
	var broken: = 0
	for a in world.get_assemblies():
		broken += a.get_broken_joint_names().size()
	print("broken joints across 20 drops: ", broken)
	get_tree().quit()
