extends Node










const MAIN_SCENE: = preload("res://scenes/main.tscn")
const V1_SAVE: = "res://tests/fixtures/saves/v0_1_world.json"
const V2_SAVE: = "res://tests/fixtures/saves/v0_2_world.json"

var _checks: = 0
var _failures: PackedStringArray = []
var _main: Node
var _world: SandboxWorld
var _temp_root: = ""


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(31)
	_temp_root = "user://reliability_test_%d" % Time.get_ticks_usec()
	_main = MAIN_SCENE.instantiate()
	add_child(_main)
	_world = _main.world
	await _frames(3)
	print("== Phase 1: save file reliability")
	await _test_save_files()
	print("== Phase 2: validated loading")
	await _test_load_validation()
	await _run_later_phases()
	_main.queue_free()
	_delete_tree(_temp_root)
	await Audio.prepare_for_quit()
	print("")
	print("Reliability test: %d checks, %d failure(s)" % [_checks, _failures.size()])
	for failure in _failures:
		print("  FAIL: " + failure)
	get_tree().quit(0 if _failures.is_empty() else 1)


func _run_later_phases() -> void :
	print("== Phase 3: projectile collisions")
	await _test_projectile_budget()
	await _test_ricochet_exclusions()
	print("== Phase 4: explosion saturation")
	await _test_explosion_saturation()
	print("== Phase 5: mod validation")
	_test_mod_validation()
	print("== Phase 6: audio voice limits")
	await _test_audio_limits()
	print("== Phase 7: simulation-time damage cooldowns")
	await _test_simulation_time_cooldowns()
	print("== Phase 8: object lifecycle")
	await _test_object_lifecycle()




func _check(condition: bool, label: String) -> void :
	_checks += 1
	if condition:
		print("  ok   " + label)
	else:
		print("  FAIL " + label)
		_failures.append(label)


func _frames(count: int) -> void :
	for index in count:
		await get_tree().physics_frame


func _reset() -> void :
	_world.clear_entities()
	await _frames(2)


func _manager(folder: String) -> SaveManager:
	return SaveManager.new(_temp_root.path_join(folder))


func _write(path: String, text: String) -> void :
	DirAccess.make_dir_recursive_absolute(path.get_base_dir())
	var file: = FileAccess.open(path, FileAccess.WRITE)
	file.store_string(text)
	file.close()


func _entity_count_in(path: String) -> int:
	var parsed: Variant = JSON.parse_string(FileAccess.get_file_as_string(path))
	return (parsed["entities"] as Array).size() if parsed is Dictionary else -1


func _snapshot() -> Dictionary:
	var state: = {}
	for entity in _world.get_entities():
		state[entity.entity_uid] = [entity.get_type_id(), entity.global_position]
	return state


func _delete_tree(path: String) -> void :
	if not DirAccess.dir_exists_absolute(path):
		return
	for file_name in DirAccess.get_files_at(path):
		DirAccess.remove_absolute(path.path_join(file_name))
	for folder in DirAccess.get_directories_at(path):
		_delete_tree(path.path_join(folder))
	DirAccess.remove_absolute(path)


func _valid_save(entities: Array, assemblies: Variant = []) -> Dictionary:
	return {"format": SaveManager.FORMAT_ID, "format_version": SaveManager.FORMAT_VERSION, 
		"entities": entities, "assemblies": assemblies, "world": {"next_entity_uid": 1000}}


func _box(uid: int, x: float) -> Dictionary:
	return {"uid": uid, "type": "core:wooden_box", "position": [x, -32.0], "rotation": 0.0}




func _test_save_files() -> void :
	var manager: = _manager("missing/nested/folder")
	_world.spawn_entity("core:wooden_box", Vector2(0, -32))
	var error: = manager.save_to_slot("slot", _world, _world.camera, Content.registry)
	var path: = manager.get_slot_path("slot")
	_check(error == OK and FileAccess.file_exists(path), "save creates a missing save folder and writes the file")
	_check( not FileAccess.file_exists(path + ".tmp"), "no temporary file is left behind after a normal save")
	_check(_entity_count_in(path) == 1, "the save file is complete, parseable JSON")

	_world.spawn_entity("core:steel_box", Vector2(200, -32))
	error = manager.save_to_slot("slot", _world, _world.camera, Content.registry)
	_check(error == OK and _entity_count_in(path) == 2, "overwriting replaces the save with the new data")
	_check(_entity_count_in(path + ".bak") == 1, "the previous good save is kept as a backup")


	DirAccess.make_dir_recursive_absolute(path + ".tmp")
	_world.spawn_entity("core:rubber_ball", Vector2(400, -100))
	error = manager.save_to_slot("slot", _world, _world.camera, Content.registry)
	_check(error != OK and manager.last_error_message.contains("temporary"), "a failed temporary write is reported (%s)" % manager.last_error_message)
	_check(_entity_count_in(path) == 2, "a failed write leaves the existing save untouched")
	DirAccess.remove_absolute(path + ".tmp")


	DirAccess.remove_absolute(path + ".bak")
	DirAccess.make_dir_recursive_absolute(path + ".bak/blocker")
	error = manager.save_to_slot("slot", _world, _world.camera, Content.registry)
	_check(error != OK and manager.last_error_message.contains("back up"), "a failed backup aborts the replacement (%s)" % manager.last_error_message)
	_check(_entity_count_in(path) == 2, "the existing save survives a failed replacement")
	_delete_tree(path + ".bak")


	var blocked: = _manager("blocked")
	DirAccess.make_dir_recursive_absolute(blocked.get_slot_path("slot") + "/blocker")
	error = blocked.save_to_slot("slot", _world, _world.camera, Content.registry)
	_check(error != OK and blocked.last_error_message.contains("replace"), "a failed final replacement is reported (%s)" % blocked.last_error_message)


	error = manager.save_to_slot("slot", _world, _world.camera, Content.registry)
	_check(error == OK and _entity_count_in(path + ".bak") == 2, "a new save backs up the previous one")
	_write(path, "{ this is not json")
	var result: = manager.load_from_slot("slot", _world, _world.camera, Content.registry)
	_check(result.ok and result.recovered_from == "backup" and _world.get_entity_count() == 2, 
		"a damaged save is recovered from the backup (%s)" % result.message)


	var interrupted: = _manager("interrupted")
	var slot_path: = interrupted.get_slot_path("slot")
	_write(slot_path + ".tmp", JSON.stringify(_valid_save([_box(1, 0.0), _box(2, 100.0), _box(3, 200.0)])))
	_write(slot_path + ".bak", JSON.stringify(_valid_save([_box(1, 0.0)])))
	result = interrupted.load_from_slot("slot", _world, _world.camera, Content.registry)
	_check(result.ok and result.recovered_from == "pending" and _world.get_entity_count() == 3, 
		"an interrupted replacement loads the finished new save")
	_check(FileAccess.file_exists(slot_path) and not FileAccess.file_exists(slot_path + ".tmp"), 
		"recovery completes the interrupted replacement")


	_write(slot_path + ".tmp", "{\"format\": \"physics_playground.world_sa")
	result = interrupted.load_from_slot("slot", _world, _world.camera, Content.registry)
	_check(result.ok and result.recovered_from == "primary" and _world.get_entity_count() == 3, 
		"a half-written temporary file is ignored in favour of the intact save")
	DirAccess.remove_absolute(slot_path)
	result = interrupted.load_from_slot("slot", _world, _world.camera, Content.registry)
	_check(result.ok and result.recovered_from == "backup", "with no usable save or temporary file, the backup is used")
	_check( not manager.has_slot("never_saved") and manager.load_from_slot("never_saved", _world, _world.camera, 
		Content.registry).status == SaveManager.Status.NOT_FOUND, "a missing slot reports NOT_FOUND")
	await _reset()




func _expect_rejected(data: Dictionary, label: String, expected: = SaveManager.Status.MALFORMED) -> void :
	var before: = _snapshot()
	var result: = SaveManager.new(_temp_root).apply_save_data(data, _world, _world.camera, Content.registry)
	_check( not result.ok and result.status == expected and _snapshot() == before, 
		"%s is rejected and the world is kept (%s)" % [label, result.errors[0] if not result.errors.is_empty() else result.message])


func _test_load_validation() -> void :
	for index in 3:
		_world.spawn_entity("core:wooden_box", Vector2(index * 80.0, -32))
	await _frames(2)
	var manager: = SaveManager.new(_temp_root)
	var before: = _snapshot()
	var result: = manager.apply_save_data({"entities": "wrong type"}, _world, _world.camera, Content.registry)
	_check( not result.ok and result.status == SaveManager.Status.MALFORMED, "{\"entities\": \"wrong type\"} is reported as malformed")
	_check(_world.get_entity_count() == 3 and _snapshot() == before, "the reviewer's malformed save no longer clears the world")

	_expect_rejected({}, "a save without an entity list")
	_expect_rejected(_valid_save([_box(1, 0.0)], "nope"), "a non-array assembly list")
	_expect_rejected(_valid_save(["not an object"]), "a non-object entity entry")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:wooden_box", "position": [1.0]}]), "a one-element position")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:wooden_box", "position": ["a", "b"]}]), "a non-numeric position")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:wooden_box", "position": [INF, 0.0]}]), "an infinite position")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:wooden_box", "position": [0.0, 0.0], "linear_velocity": [NAN, 0.0]}]), "a NaN velocity")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:wooden_box", "position": [0.0, 0.0], "rotation": "left"}]), "a text rotation")
	_expect_rejected(_valid_save([_box(5, 0.0), _box(5, 100.0)]), "duplicate entity identities")
	_expect_rejected(_valid_save([{"uid": -3, "type": "core:wooden_box", "position": [0.0, 0.0]}]), "a negative uid")
	_expect_rejected(_valid_save([{"uid": 1, "type": "not namespaced", "position": [0.0, 0.0]}]), "an invalid type id")
	var bad_health: = _box(1, 0.0)
	bad_health["components"] = {"health": {"current": "lots"}}
	_expect_rejected(_valid_save([bad_health]), "a text health value")
	var negative_health: = _box(1, 0.0)
	negative_health["components"] = {"health": {"current": -50.0}}
	_expect_rejected(_valid_save([negative_health]), "a negative health value")
	var cold: = _box(1, 0.0)
	cold["components"] = {"thermal": {"temperature": -500.0}}
	_expect_rejected(_valid_save([cold]), "a temperature below absolute zero")
	var bad_rounds: = {"uid": 1, "type": "core:pistol", "position": [0.0, -100.0], "components": {"magazine": {"rounds": 2.5}}}
	_expect_rejected(_valid_save([bad_rounds]), "a fractional ammunition count")
	_expect_rejected(_valid_save([{"uid": 1, "type": "core:human", "position": [0.0, 0.0]}]), "an assembly type used as a body")
	var part_a: = {"uid": 2, "type": "core:human_head", "position": [0.0, -100.0], "assembly": {"uid": 1, "part": "head"}}
	var part_b: = {"uid": 3, "type": "core:human_head", "position": [50.0, -100.0], "assembly": {"uid": 1, "part": "head"}}
	_expect_rejected(_valid_save([part_a, part_b], [{"uid": 1, "type": "core:human", "joints": {}}]), "two bodies claiming the same ragdoll part")
	_expect_rejected(_valid_save([part_a], [{"uid": 1, "type": "core:human", "joints": {"neck": "broken"}}]), "a malformed joint state")
	_expect_rejected(_valid_save([part_a], [{"uid": 2, "type": "core:human", "joints": {}}]), "an assembly uid shared with an entity")
	_expect_rejected(_valid_save([_box(1, 0.0)], [{"uid": 9, "type": "core:wooden_box", "joints": {}}]), "a body type used as an assembly")
	var future: = _valid_save([_box(1, 0.0)])
	future["format_version"] = 99
	_write(_temp_root.path_join("future.json"), JSON.stringify(future))
	var before_future: = _snapshot()
	result = manager.load_from_slot("future", _world, _world.camera, Content.registry)
	_check(result.status == SaveManager.Status.UNSUPPORTED_VERSION and _snapshot() == before_future, 
		"a save from a newer version is reported as unsupported and changes nothing")


	var orphan: = {"uid": 2, "type": "core:human_head", "position": [0.0, -100.0], "assembly": {"uid": 77, "part": "head"}}
	var missing_mod: = {"uid": 3, "type": "othermod:laser", "position": [100.0, -100.0]}
	var with_mods: = _valid_save([orphan, missing_mod, _box(4, 200.0)])
	with_mods["mods"] = {"core": "0.3.1", "othermod": "1.0"}
	result = manager.apply_save_data(with_mods, _world, _world.camera, Content.registry)
	_check(result.ok and result.status == SaveManager.Status.OK_WITH_WARNINGS and result.skipped_types.has("othermod:laser"), 
		"missing mod content is skipped and reported (%s)" % result.message)
	_check(_world.get_entity_count() == 2 and _world.get_entity_by_uid(2) != null and not _world.get_entity_by_uid(2).is_assembly_part(), 
		"a part whose assembly is missing loads as a standalone body")
	_check(result.warnings.size() >= 2, "warnings explain the missing assembly and mod (%d)" % result.warnings.size())


	await _reset()
	for index in 3:
		_world.spawn_entity("core:wooden_box", Vector2(index * 80.0, -32))
	await _frames(2)
	var broken: = EntityDefinition.new()
	broken.id = "test:broken_body"
	broken.material_id = "test:no_such_material"
	Content.registry.register_entity(broken)
	before = _snapshot()
	result = manager.apply_save_data(_valid_save([_box(10, 0.0), {"uid": 11, "type": "test:broken_body", "position": [0.0, -50.0]}]), 
		_world, _world.camera, Content.registry)
	Content.registry.unregister_entity("test:broken_body")
	_check(result.status == SaveManager.Status.RECONSTRUCTION_FAILED and not result.ok, "an unexpected rebuild failure is reported (%s)" % result.message)
	_check(_snapshot() == before, "the previous world is restored after a failed rebuild")


	for fixture: String in [V1_SAVE, V2_SAVE]:
		DirAccess.copy_absolute(fixture, _temp_root.path_join("compat.json"))
		result = manager.load_from_slot("compat", _world, _world.camera, Content.registry)
		_check(result.ok and result.skipped_types.is_empty(), "%s loads (%s)" % [fixture.get_file(), result.message])
	var v2_human: = _world.get_assemblies()[0] if _world.get_assembly_count() == 1 else null
	_check(v2_human != null and v2_human.get_broken_joint_names() == PackedStringArray(["elbow_right"]), "the v0.2 ragdoll is rebuilt with its torn elbow")
	var human: = _world.spawn_assembly("core:human", Vector2(600, -110))
	human.get_part("head").get_health().apply_damage(30.0)
	var pistol: = _world.spawn_entity("core:pistol", Vector2(800, -300))
	(pistol.get_component(&"magazine") as MagazineComponent).rounds = 4
	_check(manager.save_to_slot("current", _world, _world.camera, Content.registry) == OK, "a version 0.3.1 save is written")
	var count: = _world.get_entity_count()
	result = manager.load_from_slot("current", _world, _world.camera, Content.registry)
	_check(result.ok and _world.get_entity_count() == count and _world.get_assembly_count() == 2, "a version 0.3.1 save loads (%s)" % result.message)
	await _reset()




func _gone(candidate: Variant) -> bool:
	return not is_instance_valid(candidate) or not (candidate as SandboxEntity).is_inside_tree()


func _shoot(weapon_id: String, at: Vector2, angle: = 0.0) -> SandboxEntity:
	var gun: = _world.spawn_entity(weapon_id, at, angle)
	gun.set_frozen(true)
	gun.activate(true)
	gun.activate(false)
	return gun


func _panel_row(count: int, start_x: float, spacing: float, y: float) -> Array:
	var panels: = []
	for index in count:
		var panel: = _world.spawn_entity("core:glass_panel", Vector2(start_x + index * spacing, y))
		panel.set_frozen(true)
		panels.append(panel)
	return panels


func _test_projectile_budget() -> void :
	for count in [5, 10]:
		var panels: = _panel_row(count, 200.0, 12.0, -400.0)
		await _frames(2)
		var carries_before: int = _world.projectiles.stats["budget_carries"]
		_shoot("core:assault_rifle", Vector2(0, -400))
		await _frames(20)
		var shattered: = panels.filter( func(panel: Variant) -> bool: return _gone(panel)).size()
		_check(shattered == count, "one rifle round hits all %d closely spaced glass panels (%d shattered)" % [count, shattered])
		if count == 10:
			_check(int(_world.projectiles.stats["budget_carries"]) > carries_before, 
				"exceeding the per-tick collision budget carries travel into the next tick")
		await _reset()
	var slow_panels: = _panel_row(10, 200.0, 12.0, -400.0)
	await _frames(2)
	_shoot("core:pistol", Vector2(0, -400))
	await _frames(20)


	_check(slow_panels.all( func(panel: Variant) -> bool: return _gone(panel) or (panel as SandboxEntity).get_health().is_damaged()), 
		"a pistol round also reaches all 10 panels")
	await _reset()

	var planks: = []
	for index in 3:
		var plank: = _world.spawn_entity("core:wooden_plank", Vector2(200 + index * 30.0, -400), PI * 0.5)
		plank.set_frozen(true)
		planks.append(plank)
	await _frames(2)
	_shoot("core:assault_rifle", Vector2(0, -397))
	await _frames(10)
	var damaged: = planks.filter( func(plank: Variant) -> bool: return _gone(plank) or (plank as SandboxEntity).get_health().is_damaged()).size()
	_check(damaged == 3, "a rifle round penetrates three planks in a row (%d hit)" % damaged)
	await _reset()

	var wall: = _world.spawn_entity("core:wooden_box", Vector2(40, -400))
	wall.set_frozen(true)
	await _frames(2)

	_shoot("core:pistol", Vector2(0, -400))
	await _frames(5)
	_check(wall.get_health().is_damaged(), "a muzzle pressed into an object hits it instead of tunnelling through")
	await _reset()


func _test_ricochet_exclusions() -> void :
	var steel: = Content.registry.get_material("core:steel")
	var saved_chance: = steel.ricochet_chance
	steel.ricochet_chance = 1.0


	var wall: = EntityDefinition.new()
	wall.id = "test:long_steel_wall"
	wall.material_id = "core:steel"
	wall.size = Vector2(4000, 24)
	wall.mass_override = 500.0
	Content.registry.register_entity(wall)
	_world.spawn_entity("test:long_steel_wall", Vector2(2100, -200)).set_frozen(true)
	_world.spawn_entity("test:long_steel_wall", Vector2(2100, -40)).set_frozen(true)
	await _frames(2)
	var ricochets_before: int = _world.projectiles.stats["ricochets"]

	_shoot("core:assault_rifle", Vector2(40, -120), -0.17)
	var escaped: = false
	for tick in 120:
		await get_tree().physics_frame
		for point in _world.projectiles.get_positions():

			if point.x > 100.0 and point.x < 4100.0 and (point.y < -190.0 or point.y > -50.0):
				escaped = true
	var bounces: = int(_world.projectiles.stats["ricochets"]) - ricochets_before
	_check(bounces >= 3, "the round bounces back and forth between the same two walls (%d ricochets)" % bounces)
	_check( not escaped, "a ricocheting round never passes through a surface it bounced off before")
	await _reset()
	Content.registry.unregister_entity("test:long_steel_wall")


	for index in 6:
		_world.spawn_entity("core:steel_box", Vector2(100 + index * 64.0, -32)).set_frozen(true)
	var glass: = _world.spawn_entity("core:glass_panel", Vector2(560, -130))
	glass.set_frozen(true)
	await _frames(2)
	ricochets_before = _world.projectiles.stats["ricochets"]
	var penetrations_before: int = _world.projectiles.stats["penetrations"]
	_shoot("core:assault_rifle", Vector2(40, -90), 0.12)
	await _frames(10)
	_check(int(_world.projectiles.stats["ricochets"]) > ricochets_before
		and int(_world.projectiles.stats["penetrations"]) > penetrations_before
		and (_gone(glass) or glass.get_health().is_damaged()), 
		"a round can ricochet and then penetrate glass")
	steel.ricochet_chance = saved_chance
	await _reset()




func _ball_grid(count: int, columns: int, origin: Vector2, spacing: float) -> Array:
	var balls: = []
	for index in count:
		var ball: = _world.spawn_entity("core:rubber_ball", origin + Vector2((index % columns) * spacing, - (index / columns) * spacing))
		ball.set_frozen(true)
		balls.append(ball)
	return balls


func _damaged_count(entities: Array) -> int:
	return entities.filter( func(entity: Variant) -> bool:
		return _gone(entity) or (entity as SandboxEntity).get_health().is_damaged()).size()


func _wait_for_explosions(max_ticks: int) -> void :
	for tick in max_ticks:
		if not _world.explosions.is_busy():
			return
		await get_tree().physics_frame


func _test_explosion_saturation() -> void :
	var explosions: = _world.explosions
	var soft: = ExplosionParams.from_dict({"radius": 700.0, "damage": 50.0, "force": 5.0})
	var balls: = _ball_grid(160, 16, Vector2(-300, -60), 40.0)
	await _frames(2)
	_check(explosions.request(Vector2(0, -250), soft), "a single explosion request is accepted")
	await _wait_for_explosions(30)
	_check(_damaged_count(balls) == 160, "every one of 160 bodies in the radius is affected (was capped at 96)")
	await _reset()

	balls = _ball_grid(400, 20, Vector2(-380, -60), 40.0)
	await _frames(2)
	explosions.request(Vector2(0, -400), soft)
	await _frames(1)
	var first_tick: = _damaged_count(balls)
	_check(first_tick == ExplosionSystem.VICTIM_BUDGET_PER_TICK and explosions.get_active_job_count() == 1, 
		"a 400-body blast is spread over several ticks (%d affected in the first tick)" % first_tick)
	await _wait_for_explosions(30)
	_check(_damaged_count(balls) == 400, "all 400 bodies are eventually affected")
	await _reset()

	var tiny: = ExplosionParams.from_dict({"radius": 20.0, "damage": 1.0, "force": 1.0})
	var started: = explosions.explosion_count
	var accepted: = 0
	for index in 100:
		if explosions.request(Vector2(-4000 + index * 40.0, -2000), tiny):
			accepted += 1
	_check(accepted == 100, "100 simultaneous requests are all accepted (old cap: 64)")
	await _wait_for_explosions(200)
	_check(explosions.explosion_count - started == 100, "all 100 queued explosions are processed")

	var rejected_before: int = explosions.get_diagnostics()["rejected"]
	accepted = 0
	for index in ExplosionSystem.MAX_QUEUE + 44:
		if explosions.request(Vector2(-4000 + (index % 200) * 40.0, -2000), tiny):
			accepted += 1
	_check(accepted == ExplosionSystem.MAX_QUEUE and int(explosions.get_diagnostics()["rejected"]) - rejected_before == 44, 
		"a full queue refuses requests explicitly and counts them (%d accepted)" % accepted)


	var barrel: = _world.spawn_entity("core:explosive_barrel", Vector2(0, -42))
	await _frames(1)
	while explosions.get_queue_size() < ExplosionSystem.MAX_QUEUE:
		explosions.request(Vector2(4000, -2000), tiny)
	var before_barrel: = explosions.explosion_count
	var explosive: = barrel.get_component(&"explosive") as ExplosiveComponent
	explosive.detonate()
	_check( not _gone(barrel) and explosive.is_armed and not explosive.has_detonated, 
		"an explosive refused by a full queue stays armed instead of disappearing")
	_check(int(explosions.get_diagnostics()["deferred_detonations"]) > 0, "deferred detonations are reported")
	await _wait_for_explosions(400)
	await _frames(5)
	_check(_gone(barrel), "the deferred explosive detonates once the queue drains")
	_check(explosions.explosion_count - before_barrel == ExplosionSystem.MAX_QUEUE + 1, 
		"its explosion really happened (%d processed)" % (explosions.explosion_count - before_barrel))
	await _reset()


	var barrels: = []
	for index in 60:
		barrels.append(_world.spawn_entity("core:explosive_barrel", Vector2(-1800 + (index % 30) * 120.0, -42 - (index / 30) * 90.0)))
	await _frames(10)
	var chain_start: = explosions.explosion_count
	(barrels[0] as SandboxEntity).activate(true)
	for second in 20:
		await _frames(120)
		if barrels.all( func(item: Variant) -> bool: return _gone(item)) and not explosions.is_busy():
			break
	_check(barrels.all( func(item: Variant) -> bool: return _gone(item)), "a 60-barrel chain reaction detonates every barrel")
	_check(explosions.explosion_count - chain_start == 60 and not explosions.is_busy(), 
		"each barrel explodes exactly once and the chain ends (%d explosions)" % (explosions.explosion_count - chain_start))
	await _reset()


	var inside: = _world.spawn_entity("core:rubber_ball", Vector2(5900, -40))
	var outside: = _world.spawn_entity("core:rubber_ball", Vector2(6260, -40))
	inside.set_frozen(true)
	outside.set_frozen(true)
	await _frames(2)
	explosions.request(Vector2(5960, -40), ExplosionParams.from_dict({"radius": 400.0, "damage": 100.0}))
	await _wait_for_explosions(10)
	var inside_damage: = inside.get_health().max_health - inside.get_health().current_health
	var outside_damage: = outside.get_health().max_health - outside.get_health().current_health
	_check(inside_damage > 0.0 and outside_damage < inside_damage * 0.3, 
		"an explosion at the boundary wall works and the wall shields the far side (%.0f vs %.0f)" % [inside_damage, outside_damage])
	await _reset()


	var human: = _world.spawn_assembly("core:human", Vector2(0, -110))
	for part in human.get_parts():
		part.set_frozen(true)
	_ball_grid(150, 15, Vector2(-500, -400), 40.0)
	await _frames(2)
	explosions.request(Vector2(0, -150), ExplosionParams.from_dict({"radius": 600.0, "damage": 20.0, "force": 5.0}))
	await _wait_for_explosions(30)
	_check(human.get_parts().all( func(part: SandboxEntity) -> bool: return part.get_health().is_damaged()), 
		"every limb of a ragdoll inside a 150-body crowd is affected")
	await _reset()




func _gun(local_id: String, firearm: Dictionary, extra: = {}) -> Dictionary:
	var entity: = {"id": local_id, "display_name": local_id, "category": "Test", "material": "core:steel", 
		"mass": 1.0, "shape": {"type": "rectangle", "size": [30, 12]}, 
		"components": {"magazine": {"capacity": 6}, "firearm": firearm}}
	entity.merge(extra, true)
	return entity




func _load_test_mod(mod_id: String, entities: Array, extra_files: = {}) -> Dictionary:
	var root: = _temp_root.path_join("mods_%s" % mod_id)
	var folder: = root.path_join(mod_id)
	_write(folder.path_join("mod.json"), JSON.stringify({"mod_format": 1, "id": mod_id, "name": mod_id, "version": "1.0.0"}))
	if not entities.is_empty():
		_write(folder.path_join("entities/test_entities.json"), JSON.stringify({"entities": entities}))
	for relative_path: String in extra_files:
		_write(folder.path_join(relative_path), extra_files[relative_path])
	var registry: = EntityRegistry.new()
	var loader: = ModLoader.new([ModLoader.BUILTIN_MODS_PATH, root])
	loader.load_all(registry)
	return {"errors": loader.errors, "warnings": loader.warnings, "registry": registry}


func _rejected(result: Dictionary, entity_id: String, fragments: Array) -> bool:
	var registry: EntityRegistry = result["registry"]
	if registry.has_entity(entity_id):
		return false
	for error: String in result["errors"]:
		var matches: = true
		for fragment: String in fragments:
			if not error.contains(fragment):
				matches = false
		if matches:
			return true
	return false


func _test_mod_validation() -> void :
	for component_type in ComponentRegistry.get_types():
		_check(ComponentSchemas.has_schema(component_type), "component '%s' has a typed schema" % component_type)
	var built_in: = _load_test_mod("noop", [])
	_check((built_in["errors"] as PackedStringArray).is_empty(), "all built-in definitions pass schema validation %s" % [built_in["errors"]])
	var example_registry: = EntityRegistry.new()
	var example_loader: = ModLoader.new([ModLoader.BUILTIN_MODS_PATH, "res://tests/fixtures/mods"])
	example_loader.load_all(example_registry)
	_check(example_loader.errors.is_empty() and example_registry.has_entity("example:ice_block"), "the example mod still loads")

	var result: = _load_test_mod("badnum", [_gun("fast_gun", {"projectile": "core:pistol_round", "fire_rate": "fast"})])
	_check(_rejected(result, "badnum:fast_gun", ["mod 'badnum'", "test_entities.json", "entity 'badnum:fast_gun'", 
		"component 'firearm'", "'fire_rate'", "a number in [0.1, 60]", "\"fast\""]), 
		"a text fire rate is rejected with mod, file, entity, component, parameter and expected range")
	result = _load_test_mod("range", [_gun("pellet_storm", {"projectile": "core:buckshot", "pellets": 500})])
	_check(_rejected(result, "range:pellet_storm", ["'pellets'", "whole number in [1, 32]"]), "an out-of-range pellet count is rejected")
	result = _load_test_mod("infinite", [_gun("inf_gun", {"projectile": "core:pistol_round", "recoil": INF})])
	_check(_rejected(result, "infinite:inf_gun", ["'recoil'"]), "an infinite number (1e999) is rejected")
	var nested: = _gun("nested", {"projectile": "core:pistol_round"})
	nested["components"]["health"] = {"max_health": 50, "damage_multipliers": {"projectile": "double"}}
	result = _load_test_mod("nested", [nested])
	_check(_rejected(result, "nested:nested", ["component 'health'", "'damage_multipliers'"]), "an invalid nested dictionary is rejected")
	var bad_kind: = _gun("kind", {"projectile": "core:pistol_round"})
	bad_kind["components"]["health"] = {"damage_multipliers": {"lasers": 2.0}}
	result = _load_test_mod("kind", [bad_kind])
	_check(_rejected(result, "kind:kind", ["unknown damage kind 'lasers'"]), "an unknown damage kind is rejected")
	result = _load_test_mod("missing", [_gun("no_ammo_type", {"fire_rate": 5})])
	_check(_rejected(result, "missing:no_ammo_type", ["missing required parameter 'projectile'"]), "a missing required parameter is rejected")
	result = _load_test_mod("badref", [_gun("ghost_gun", {"projectile": "plasma_bolt"})])
	_check(_rejected(result, "badref:ghost_gun", ["unknown projectile 'badref:plasma_bolt'"]), "an unknown projectile reference is rejected")
	var enum_bad: = _gun("enum", {"projectile": "core:pistol_round"})
	enum_bad["components"]["health"] = {"on_depleted": "explode"}
	result = _load_test_mod("enumbad", [enum_bad])
	_check(_rejected(result, "enumbad:enum", ["'on_depleted'", "one of none, remove"]), "an invalid enumeration value is rejected")
	var unsupported: = _gun("laser", {"projectile": "core:pistol_round"})
	unsupported["components"]["laser_beam"] = {"power": 9000}
	result = _load_test_mod("unsupported", [unsupported])
	_check(_rejected(result, "unsupported:laser", ["component 'laser_beam'", "unsupported component type"]), 
		"an unsupported behaviour (component type) is rejected, not silently ignored")
	var combo: = _gun("debris_gun", {"projectile": "core:pistol_round"})
	combo["components"]["debris"] = {"lifetime": 5}
	result = _load_test_mod("combo", [combo])
	_check(_rejected(result, "combo:debris_gun", ["cannot be combined with 'debris'"]), "an invalid component combination is rejected")
	var dependency: = {"id": "fragile", "material": "core:wood", "shape": {"type": "rectangle", "size": [20, 20]}, 
		"components": {"impact_damage": {"min_impact_speed": 5}}}
	result = _load_test_mod("depends", [dependency])
	_check(_rejected(result, "depends:fragile", ["requires the 'health' component"]), "a missing component dependency is rejected")
	var duplicate: = {"id": "twin", "material": "core:wood", "shape": {"type": "rectangle", "size": [20, 20]}}
	result = _load_test_mod("dupes", [duplicate, duplicate.duplicate(true)])
	var registry: EntityRegistry = result["registry"]
	var duplicate_reported: = false
	for error: String in result["errors"]:
		if error.contains("duplicate entity id 'dupes:twin'"):
			duplicate_reported = true
	_check(duplicate_reported and registry.has_entity("dupes:twin"), "a duplicate entity id is reported and only the first is kept")
	var parts: = []
	for index in 40:
		parts.append({"name": "part_%d" % index, "entity": "core:wooden_box"})
	result = _load_test_mod("giant", [{"id": "colossus", "assembly": {"parts": parts, "joints": []}}])
	_check(_rejected(result, "giant:colossus", ["40 parts", "limit is 32"]), "an excessive assembly is rejected")
	var unknown_param: = _gun("typo", {"projectile": "core:pistol_round", "fire_rat": 5})
	result = _load_test_mod("typo", [unknown_param])
	var typo_warned: = false
	for warning: String in result["warnings"]:
		if warning.contains("unknown parameter 'fire_rat'"):
			typo_warned = true
	_check(typo_warned and (result["registry"] as EntityRegistry).has_entity("typo:typo"), "an unknown parameter is warned about without rejecting the definition")

	var big: = "{\"entities\": [" + ",".join(PackedStringArray(range(13000).map( func(i: int) -> String:
		return "{\"id\": \"padding_%d\", \"material\": \"core:wood\", \"shape\": {\"type\": \"rectangle\", \"size\": [20, 20]}}" % i))) + "]}"
	result = _load_test_mod("huge", [], {"entities/huge.json": big})
	var size_reported: = false
	for error: String in result["errors"]:
		if error.contains("over the 1024 KiB limit"):
			size_reported = true
	_check(size_reported and not (result["registry"] as EntityRegistry).has_entity("huge:padding_0"), "an oversized definition file is skipped")
	var many: = []
	for index in ModLoader.MAX_DEFINITIONS_PER_MOD + 5:
		many.append({"id": "item_%d" % index, "material": "core:wood", "shape": {"type": "rectangle", "size": [20, 20]}})
	result = _load_test_mod("many", many)
	registry = result["registry"]
	_check(registry.has_entity("many:item_0") and not registry.has_entity("many:item_%d" % ModLoader.MAX_DEFINITIONS_PER_MOD), 
		"definitions beyond the per-mod limit are refused")
	var sounds: = []

	for index in 220:
		sounds.append({"id": "long_%d" % index, "synth": "explosion", "duration": 4.0})
	result = _load_test_mod("loud", [], {"sounds/loud.json": JSON.stringify({"sounds": sounds})})
	var audio_reported: = false
	for error: String in result["errors"]:
		if error.contains("audio budget exceeded"):
			audio_reported = true
	_check(audio_reported, "the generated-audio memory budget is enforced")




func _test_sound(id: String, max_instances: int, min_interval: = 0.0) -> String:
	var sound: = SoundDefinition.new()
	sound.id = id
	sound.synth = "click"
	sound.duration = 0.5
	sound.max_instances = max_instances
	sound.min_interval = min_interval
	Content.registry.register_sound(sound)
	return id


func _test_audio_limits() -> void :
	var one: = _test_sound("test:limit_one", 1)
	var two: = _test_sound("test:limit_two", 2)
	var many: = _test_sound("test:limit_many", 100)
	var spaced: = _test_sound("test:spaced", 4, 0.5)
	Audio.stop_all()
	for index in 3:
		Audio.play(one, Vector2.ZERO)
	_check(Audio.get_active_instances(one) == 1, "max_instances = 1 allows one voice (%d playing)" % Audio.get_active_instances(one))
	for index in 3:
		Audio.play(two, Vector2.ZERO)
	_check(Audio.get_active_instances(two) == 2, "max_instances = 2 allows two voices (%d playing)" % Audio.get_active_instances(two))
	_check(Audio.get_active_instances(one) == 1, "limits are tracked per sound type")
	Audio.stop_all()
	for index in Audio.VOICE_COUNT + 6:
		Audio.play(many, Vector2.ZERO)
	_check(Audio.get_active_voice_count() == Audio.VOICE_COUNT, "the global pool saturates at %d voices" % Audio.VOICE_COUNT)
	_check(Audio.play(one, Vector2.ZERO) and Audio.get_active_instances(one) == 1
		and Audio.get_active_voice_count() == Audio.VOICE_COUNT, "a saturated pool reuses (steals) a voice without exceeding the global limit")
	Audio.stop_all()
	_check(Audio.play(spaced, Vector2.ZERO) and not Audio.play(spaced, Vector2.ZERO), "rapid repeats within min_interval are refused")
	Audio.stop_all()







func _cooldown_scenario(time_scale: float) -> int:
	GameState.set_time_scale(time_scale)
	var box: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
	box.set_frozen(true)
	await _frames(1)
	var sensor: = box.get_component(&"impact_damage") as ImpactDamageComponent
	var health: = box.get_health()
	var hits: = 0
	for wait in [0.0, 0.1, 0.15]:
		var target: float = GameState.simulation_time + wait
		while GameState.simulation_time < target - 0.0001:
			await get_tree().physics_frame
		var before: = health.current_health
		sensor._register_impact(12.0, 1.0, Vector2.ZERO, 4242)
		if health.current_health < before:
			hits += 1
	_world.remove_entity(box)
	GameState.set_time_scale(1.0)
	return hits


func _test_simulation_time_cooldowns() -> void :
	var results: = {}
	for scale in [0.1, 0.5, 1.0, 2.0]:
		results[scale] = await _cooldown_scenario(scale)
	_check(results.values().all( func(hits: int) -> bool: return hits == 2), 
		"impacts 0.1 s apart (simulated) are blocked and 0.25 s apart are not, at 0.1×, 0.5×, 1× and 2× %s" % [results])

	var box: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
	box.set_frozen(true)
	await _frames(1)
	var sensor: = box.get_component(&"impact_damage") as ImpactDamageComponent
	var health: = box.get_health()
	sensor._register_impact(12.0, 1.0, Vector2.ZERO, 99)
	var after_first: = health.current_health
	OS.delay_msec(300)
	sensor._register_impact(12.0, 1.0, Vector2.ZERO, 99)
	_check(health.current_health == after_first, "wall-clock time passing does not end a cooldown")
	GameState.set_paused(true)
	var paused_time: = GameState.simulation_time
	await _frames(120)
	_check(GameState.simulation_time == paused_time, "simulation time stops while paused")
	sensor._register_impact(12.0, 1.0, Vector2.ZERO, 99)
	_check(health.current_health == after_first, "cooldowns do not expire while paused")
	GameState.set_paused(false)
	await _reset()

	for scale in [0.5, 2.0]:
		GameState.set_time_scale(scale)
		var crate: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
		_world.spawn_entity("core:steel_box", Vector2(0, -98))
		await _frames(int(120 * 3 / scale))
		_check( not crate.get_health().is_damaged(), "a steel box resting on a crate does no damage at %.1f× speed" % scale)
		GameState.set_time_scale(1.0)
		await _reset()





func _lifecycle_cycle(manager: SaveManager) -> void :
	for index in 30:
		_world.spawn_entity("core:wooden_box", Vector2(-1500 + index * 70.0, -32))
	_world.spawn_assembly("core:human", Vector2(800, -110))
	var rifle: = _world.spawn_entity("core:assault_rifle", Vector2(-1700, -300), 0.4)
	rifle.set_frozen(true)
	(rifle.get_component(&"magazine") as MagazineComponent).infinite = true
	rifle.activate(true)
	await _frames(60)
	rifle.activate(false)
	SandboxTools.explode_at(_world, Vector2(-1000, -60))
	SandboxTools.explode_at(_world, Vector2(-400, -60))
	await _frames(30)
	manager.save_to_slot("lifecycle", _world, _world.camera, Content.registry)
	manager.load_from_slot("lifecycle", _world, _world.camera, Content.registry)
	await _frames(30)
	_world.clear_entities()
	await _frames(120 * 8)


func _test_object_lifecycle() -> void :
	var manager: = _manager("lifecycle")
	await _lifecycle_cycle(manager)
	var baseline: = int(Performance.get_monitor(Performance.OBJECT_COUNT))
	var counts: Array[int] = []
	for cycle in 3:
		await _lifecycle_cycle(manager)
		counts.append(int(Performance.get_monitor(Performance.OBJECT_COUNT)))
	print("       live objects: baseline %d, after each cycle %s" % [baseline, counts])
	_check(counts.all( func(count: int) -> bool: return count <= baseline + 25), 
		"repeated spawn/fire/explode/save/load/clear cycles do not accumulate objects")
	_check(_world.projectiles.get_count() == 0 and _world.effects.get_particle_count() == 0
		and _world.get_debris_count() == 0 and _world.get_assembly_count() == 0, 
		"projectiles, particles, debris and assemblies are all cleaned up")
