extends Node





const MAIN_SCENE: = preload("res://scenes/main.tscn")
const FIXTURE_MODS_PATH: = "res://tests/fixtures/mods"
const CORE_IDS: Array[String] = [
	"core:wooden_box", "core:steel_box", "core:metal_barrel", "core:concrete_block", 
	"core:rubber_ball", "core:metal_sphere", "core:wooden_plank", 
]

var _checks: = 0
var _failures: PackedStringArray = []


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	_test_registry()
	_test_mod_loader_with_fixture()
	_test_save_migrator()
	var main: = MAIN_SCENE.instantiate()
	add_child(main)
	await _physics_frames(2)
	await _test_spawn_and_settle(main)
	await _test_drag(main)
	await _test_rubber_bounce(main)
	await _test_freeze_duplicate_delete(main)
	await _test_save_load_roundtrip(main)
	await _test_pause(main)
	await _test_spawn_placement(main)
	main.queue_free()
	await _physics_frames(1)

	print("")
	print("Smoke test: %d checks, %d failure(s)" % [_checks, _failures.size()])
	for failure in _failures:
		print("  FAIL: " + failure)

	await Audio.prepare_for_quit()
	get_tree().quit(0 if _failures.is_empty() else 1)


func _check(condition: bool, label: String) -> void :
	_checks += 1
	if condition:
		print("  ok   " + label)
	else:
		print("  FAIL " + label)
		_failures.append(label)


func _physics_frames(count: int) -> void :
	for index in count:
		await get_tree().physics_frame


func _test_registry() -> void :
	print("[registry]")
	_check(Content.load_errors.is_empty(), "core content loads without errors %s" % [Content.load_errors])
	for id in CORE_IDS:
		_check(Content.registry.has_entity(id), "registry has %s" % id)
	var categories: = Content.registry.get_categories()
	var expected_categories: Array[String] = ["Ragdolls", "Firearms", "Melee", "Explosives", "Boxes", "Barrels", 
		"Construction", "Spheres"]
	_check(categories == expected_categories, "core categories in order: %s" % [categories])
	for definition in Content.registry.get_all_entities():
		if definition.is_assembly():
			_check(definition.compute_mass(null) > 0.0, "%s (assembly) has positive total mass" % definition.id)
			continue
		var material: = Content.registry.get_material(definition.material_id)
		_check(material != null and definition.compute_mass(material) > 0.0, 
			"%s has material and positive mass" % definition.id)


func _test_mod_loader_with_fixture() -> void :
	print("[mod loader]")
	var registry: = EntityRegistry.new()
	var loader: = ModLoader.new([ModLoader.BUILTIN_MODS_PATH, FIXTURE_MODS_PATH])
	loader.load_all(registry)
	_check(loader.errors.is_empty(), "fixture mod loads without errors %s" % [loader.errors])
	_check(registry.get_mods()[0].id == "core", "core mod loads first")
	_check(registry.has_entity("example:ice_block"), "example:ice_block registered")
	var ice: = registry.get_entity_definition("example:ice_block")
	_check(ice != null and ice.material_id == "example:ice", "mod entity resolves its own namespaced material")
	_check(ice != null and ice.properties.get("melts_at") == 0.0, "mod-defined properties preserved")
	_check( not ice.components.has("electrical"), "components limited to those declared")

	var bad_reader: = DataReader.new("inline")
	var bad: = EntityDefinition.from_dict({"id": "Bad Id", "shape": {"type": "hexagon"}}, "test", bad_reader)
	_check(bad_reader.errors.size() >= 3, "invalid definition reports errors (%d)" % bad_reader.errors.size())
	_check(bad.id == "test:Bad Id", "invalid definition still returns an object for reporting")


func _test_save_migrator() -> void :
	print("[save migrator]")
	_check(SaveMigrator.migrate({"format_version": 1}, 1)["ok"], "current version passes")
	_check( not SaveMigrator.migrate({"format_version": 99}, 1)["ok"], "future version rejected")
	_check( not SaveMigrator.migrate({}, 1)["ok"], "missing version rejected")


func _test_spawn_and_settle(main: Node) -> void :
	print("[spawn + settle]")
	var world: SandboxWorld = main.world
	var spawned: Array[SandboxEntity] = []
	var x: = -600.0
	for id in CORE_IDS:
		var entity: = world.spawn_entity(id, Vector2(x, -200.0))
		_check(entity != null, "spawned %s" % id)
		if entity:
			spawned.append(entity)
		x += 200.0
	_check(world.get_entity_count() == CORE_IDS.size(), "entity count = %d" % world.get_entity_count())
	var uids: = {}
	for entity in spawned:
		uids[entity.entity_uid] = true
	_check(uids.size() == spawned.size(), "entity uids are unique")
	await _physics_frames(1)
	var count_label: = (main.hud as Hud).get_node("%CountLabel") as Label
	_check(count_label.text == "Objects: %d  ·  Bodies: %d" % [CORE_IDS.size(), CORE_IDS.size()], 
		"HUD object counter updates (%s)" % count_label.text)
	for action in ["rotate_cw", "toggle_freeze", "duplicate_selected", "toggle_pause", "quick_save"]:
		_check(InputMap.has_action(action), "input action '%s' registered" % action)
	await _physics_frames(120 * 4)
	for entity in spawned:
		var half_height: = entity.definition.get_bounding_radius()
		var resting: = entity.global_position.y < world.floor_y and entity.global_position.y > world.floor_y - half_height - 2.0
		_check(resting, "%s rests on the floor (y=%.1f)" % [entity.get_type_id(), entity.global_position.y])
	world.clear_entities()
	await _physics_frames(1)
	_check(world.get_entity_count() == 0, "clear_entities empties the world")


func _test_drag(main: Node) -> void :
	print("[drag]")
	var world: SandboxWorld = main.world
	var box: = world.spawn_entity("core:concrete_block", Vector2(0, -32))
	await _physics_frames(30)
	var grab_point: = box.global_position + Vector2(20, 0)
	var drag: = DragHandle.new(box, grab_point)
	drag.target_position = grab_point + Vector2(0, -400)
	for index in 120 * 2:
		drag.update(false, false)
		await get_tree().physics_frame
	var error: = drag.get_anchor_world_position().distance_to(drag.target_position)
	_check(error < 8.0, "spring drag holds a 147 kg block at the target (error %.2f px)" % error)

	drag.target_position += Vector2(300, 0)
	for index in 12:
		drag.update(false, false)
		await get_tree().physics_frame
	var throw_speed: = box.linear_velocity.length()
	_check(throw_speed > 100.0, "moving the target gives the body velocity (%.0f px/s) for throwing" % throw_speed)
	world.remove_entity(box)


func _test_rubber_bounce(main: Node) -> void :
	print("[materials]")
	var world: SandboxWorld = main.world
	var ball: = world.spawn_entity("core:rubber_ball", Vector2(0, -500))
	var sphere: = world.spawn_entity("core:metal_sphere", Vector2(300, -500))
	var ball_peak: = 0.0
	var sphere_peak: = 0.0
	var ball_hit_floor: = false
	var sphere_hit_floor: = false
	for index in 120 * 3:
		await get_tree().physics_frame
		if ball.global_position.y > -25.0:
			ball_hit_floor = true
		elif ball_hit_floor:
			ball_peak = maxf(ball_peak, - ball.global_position.y)
		if sphere.global_position.y > -25.0:
			sphere_hit_floor = true
		elif sphere_hit_floor:
			sphere_peak = maxf(sphere_peak, - sphere.global_position.y)
	_check(ball_peak > 150.0, "rubber ball bounces high (peak %.0f px)" % ball_peak)
	_check(sphere_peak < ball_peak * 0.5, "metal sphere bounces much less (peak %.0f px)" % sphere_peak)
	world.clear_entities()


func _test_freeze_duplicate_delete(main: Node) -> void :
	print("[freeze / duplicate / delete]")
	var world: SandboxWorld = main.world
	var interaction: InteractionController = main.interaction
	var box: = world.spawn_entity("core:wooden_box", Vector2(0, -600))
	interaction.select(box)
	interaction.toggle_freeze_selected()
	await _physics_frames(60)
	_check(box.is_frozen() and is_equal_approx(box.global_position.y, -600.0), "frozen box hangs in the air")
	(box.get_component(&"health") as HealthComponent).apply_damage(25.0)
	interaction.duplicate_selected()
	var copy: = interaction.selected_entity
	_check(copy != null and copy != box, "duplicate is selected")
	_check(copy.is_frozen(), "duplicate copies frozen state")
	_check(copy.entity_uid != box.entity_uid, "duplicate gets a new uid")
	var copy_health: = copy.get_component(&"health") as HealthComponent
	_check(is_equal_approx(copy_health.current_health, 75.0), "duplicate copies component state")
	var copy_start_y: = copy.global_position.y
	interaction.toggle_freeze_selected()
	await _physics_frames(60)
	_check( not copy.is_frozen() and copy.global_position.y > copy_start_y + 20.0, 
		"unfrozen duplicate falls onto the frozen original (dy %.1f)" % (copy.global_position.y - copy_start_y))
	interaction.delete_selected()
	await _physics_frames(1)
	_check( not is_instance_valid(copy) or not copy.is_inside_tree(), "deleted entity leaves the tree")
	_check(interaction.selected_entity == null, "selection cleared after delete")
	world.clear_entities()


func _test_save_load_roundtrip(main: Node) -> void :
	print("[save / load]")
	var world: SandboxWorld = main.world
	var save_manager: = SaveManager.new()
	var a: = world.spawn_entity("core:steel_box", Vector2(-100, -300), 0.5)
	var b: = world.spawn_entity("core:wooden_plank", Vector2(200, -400), -0.25)
	b.linear_velocity = Vector2(120, -40)
	b.angular_velocity = 1.5
	a.set_frozen(true)
	var expected: = {}
	for entity in world.get_entities():
		expected[entity.entity_uid] = EntitySerializer.serialize(entity)
	var next_uid: = world.get_next_uid()

	var error: = save_manager.save_to_slot("smoke_test", world, world.camera, Content.registry)
	_check(error == OK, "save_to_slot returns OK")
	var text: = FileAccess.get_file_as_string(save_manager.get_slot_path("smoke_test"))
	_check(text.contains("\"format_version\": %d" % SaveManager.FORMAT_VERSION) and text.contains("core:wooden_plank"), 
		"save file is versioned JSON")

	world.clear_entities()
	world.spawn_entity("core:rubber_ball", Vector2(0, -100))
	var result: = save_manager.load_from_slot("smoke_test", world, world.camera, Content.registry)
	_check(result.ok and result.loaded_count == 2, "load_from_slot restores 2 entities (%s)" % result.message)
	_check(world.get_entity_count() == 2, "load replaces previous world contents")
	_check(world.get_next_uid() >= next_uid, "next uid preserved")
	for uid: int in expected:
		var entity: = world.get_entity_by_uid(uid)
		var saved: Dictionary = expected[uid]
		_check(entity != null and entity.get_type_id() == saved["type"], "uid %d keeps type %s" % [uid, saved["type"]])
		if entity == null:
			continue
		var position: = EntitySerializer.array_to_vector2(saved["position"], Vector2.INF)
		_check(entity.global_position.distance_to(position) < 0.01, "uid %d position restored" % uid)
		_check(absf(entity.global_rotation - float(saved["rotation"])) < 0.0001, "uid %d rotation restored" % uid)
		var velocity: = EntitySerializer.array_to_vector2(saved["linear_velocity"], Vector2.INF)
		_check(entity.linear_velocity.distance_to(velocity) < 0.01, "uid %d velocity restored" % uid)
		_check(entity.is_frozen() == bool(saved["frozen"]), "uid %d frozen state restored" % uid)



	var bad: = save_manager.apply_save_data({"entities": [{"uid": 1, "type": "missing_mod:thing", "position": [0, 0]}]}, 
		world, world.camera, Content.registry)
	_check(bad.ok and bad.skipped_types.has("missing_mod:thing"), "unknown entity types are skipped and reported")
	DirAccess.remove_absolute(save_manager.get_slot_path("smoke_test"))
	world.clear_entities()


func _test_pause(main: Node) -> void :
	print("[pause]")
	var world: SandboxWorld = main.world
	var box: = world.spawn_entity("core:wooden_box", Vector2(0, -800))
	await _physics_frames(5)
	GameState.set_paused(true)
	var paused_y: = box.global_position.y
	await _physics_frames(60)
	_check(is_equal_approx(box.global_position.y, paused_y), "bodies do not move while paused")
	_check(world.pick_entity_at(box.global_position) == box, "picking works while paused")
	GameState.set_paused(false)
	await _physics_frames(30)
	_check(box.global_position.y > paused_y + 10.0, "bodies resume after unpausing")
	GameState.set_time_scale(0.5)
	_check(is_equal_approx(Engine.time_scale, 0.5), "time scale applied")
	GameState.set_time_scale(1.0)
	world.clear_entities()


func _test_spawn_placement(main: Node) -> void :
	print("[spawn placement]")
	var world: SandboxWorld = main.world
	var definition: = Content.registry.get_entity_definition("core:wooden_box")
	_check(world.can_place(definition, Vector2(0, -300), 0.0), "free spot is placeable")
	_check( not world.can_place(definition, Vector2(0, 10), 0.0), "spot inside the floor is rejected")
	var box: = world.spawn_entity("core:wooden_box", Vector2(500, -300))
	box.set_frozen(true)
	await _physics_frames(2)
	_check( not world.can_place(definition, Vector2(510, -300), 0.0), "overlapping spot is rejected")
	world.clear_entities()
