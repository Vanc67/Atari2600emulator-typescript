extends Node





























const MAIN_SCENE: = preload("res://scenes/main.tscn")
const WARMUP_TICKS: = 120
const MEASURE_TICKS: = 480
const SCENARIO_BUDGET_MSEC: = 90000


class TickProbe extends Node:
	var mark: = 0
	var is_start: = true
	var on_physics: Callable
	var on_process: Callable
	func _physics_process(_delta: float) -> void :
		on_physics.call(Time.get_ticks_usec())
	func _process(_delta: float) -> void :
		if is_start:
			on_process.call(Time.get_ticks_usec())

var _main: Node
var _world: SandboxWorld
var _physics_start: = 0
var _physics_end: = 0
var _process_start: = 0
var _last_frame_usec: = 0
var _frame_intervals: = PackedFloat64Array()
var _collect_frames: = false
var _rendered: = false
var _report: Array = []


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(4242)
	var args: = OS.get_cmdline_user_args()
	_rendered = args.has("--rendered")
	var only: = ""
	for arg in args:
		if arg.begins_with("--only="):
			only = arg.substr(7)
	if _rendered:
		DisplayServer.window_set_vsync_mode(DisplayServer.VSYNC_DISABLED)
	_main = MAIN_SCENE.instantiate()
	add_child(_main)
	if args.has("--no-effects"):
		(_main.world as SandboxWorld).effects.visible = false
	var first: = TickProbe.new()
	first.process_mode = Node.PROCESS_MODE_ALWAYS
	first.process_physics_priority = -1000000
	first.process_priority = -1000000
	first.on_physics = func(t: int) -> void : _physics_start = t
	first.on_process = func(t: int) -> void :
		if _collect_frames and _last_frame_usec > 0:
			_frame_intervals.append(float(t - _last_frame_usec) / 1000.0)
		_last_frame_usec = t
		_process_start = t
	add_child(first)
	var last: = TickProbe.new()
	last.process_mode = Node.PROCESS_MODE_ALWAYS
	last.process_physics_priority = 1000000
	last.is_start = false
	last.on_physics = func(t: int) -> void : _physics_end = t
	add_child(last)
	_world = _main.world
	await get_tree().physics_frame
	print("Benchmark: %s, Godot %s, %s, %d logical CPUs" % ["rendered" if _rendered else "headless", 
		Engine.get_version_info()["string"], OS.get_model_name(), OS.get_processor_count()])
	var scenarios: = [
		["ragdolls_20", _setup_ragdolls.bind(20)], 
		["ragdolls_50", _setup_ragdolls.bind(50)], 
		["ragdolls_100", _setup_ragdolls.bind(100)], 
		["props_200", _setup_props.bind(200)], 
		["firearms_8_into_200_props", _setup_firearms], 
		["large_explosions_50_ragdolls", _setup_explosions], 
		["mixed_weapons_scene", _setup_mixed], 
	]
	for scenario: Array in scenarios:
		if not only.is_empty() and not only.split(",").has(scenario[0]):
			continue
		await _measure(scenario[0], scenario[1])
		_world.clear_entities()
		await get_tree().physics_frame
	_write_report()
	await Audio.prepare_for_quit()
	get_tree().quit()




func _setup_ragdolls(count: int) -> Callable:
	for index in count:
		_world.spawn_assembly("core:human", Vector2(-5600 + (index % 50) * 224.0, -400 - (index / 50) * 260.0))
	return Callable()


func _setup_props(count: int) -> Callable:
	var types: = ["core:wooden_box", "core:steel_box", "core:metal_barrel", "core:rubber_ball", "core:wooden_plank", "core:concrete_block"]
	for index in count:
		_world.spawn_entity(types[index % types.size()], Vector2(-5000 + (index % 50) * 200.0, -300 - (index / 50) * 150.0))
	return Callable()


func _setup_firearms() -> Callable:
	_setup_props(200)
	for index in 8:
		var gun: = _world.spawn_entity("core:assault_rifle", Vector2(-5800, -100 - index * 70.0), 0.02)
		gun.set_frozen(true)
		(gun.get_component(&"magazine") as MagazineComponent).infinite = true
		gun.activate(true)
	return Callable()


func _setup_explosions() -> Callable:
	_setup_ragdolls(50)
	var bombs: Array[SandboxEntity] = []
	for index in 10:
		bombs.append(_world.spawn_entity("core:large_bomb", Vector2(-5000 + index * 1100.0, -40)))

	var state: = {"tick": 0}
	return func() -> void :
		state["tick"] += 1
		var index: int = state["tick"] / 40
		if state["tick"] %40 == 0 and index < bombs.size() and is_instance_valid(bombs[index]):
			(bombs[index].get_component(&"explosive") as ExplosiveComponent).arm(0.0)


func _setup_mixed() -> Callable:
	for index in 20:
		_world.spawn_assembly("core:human", Vector2(-2400 + index * 240.0, -300))
	var props: = ["core:wooden_box", "core:steel_box", "core:metal_barrel", "core:rubber_ball", "core:wooden_plank"]
	for index in 100:
		_world.spawn_entity(props[index % props.size()], Vector2(-2500 + (index % 25) * 200.0, -700 - (index / 25) * 120.0))
	for index in 4:
		var gun: = _world.spawn_entity("core:assault_rifle", Vector2(-2600, -150 - index * 60.0), 0.03)
		gun.set_frozen(true)
		(gun.get_component(&"magazine") as MagazineComponent).infinite = true
		gun.activate(true)
	var state: = {"tick": 0}
	return func() -> void :
		state["tick"] += 1
		if state["tick"] %180 == 0:
			SandboxTools.explode_at(_world, Vector2(randf_range(-2000, 2000), -100))




func _measure(name: String, setup: Callable) -> void :
	var started: = Time.get_ticks_msec()
	var per_tick: Variant = setup.call()
	for index in WARMUP_TICKS:
		if per_tick is Callable and (per_tick as Callable).is_valid():
			(per_tick as Callable).call()
		await get_tree().physics_frame
	var wall: = PackedFloat64Array()
	var physics: = PackedFloat64Array()
	var scripts: = PackedFloat64Array()
	var peak_queue: = 0
	var peak_projectiles: = 0
	var peak_particles: = 0
	var peak_memory: = 0
	var truncated: = false
	var last: = Time.get_ticks_usec()
	_frame_intervals.clear()
	_last_frame_usec = 0
	_collect_frames = _rendered
	for index in MEASURE_TICKS:
		if per_tick is Callable and (per_tick as Callable).is_valid():
			(per_tick as Callable).call()
		await get_tree().physics_frame
		var now: = Time.get_ticks_usec()
		wall.append(float(now - last) / 1000.0)
		last = now

		if _process_start > _physics_end:
			physics.append(float(_process_start - _physics_end) / 1000.0)
		scripts.append(float(_physics_end - _physics_start) / 1000.0)
		peak_queue = maxi(peak_queue, _world.explosions.get_queue_size())
		peak_projectiles = maxi(peak_projectiles, _world.projectiles.get_count())
		peak_particles = maxi(peak_particles, _world.effects.get_particle_count())
		peak_memory = maxi(peak_memory, int(Performance.get_monitor(Performance.MEMORY_STATIC)))
		if Time.get_ticks_msec() - started > SCENARIO_BUDGET_MSEC:
			truncated = true
			break
	_collect_frames = false
	var joints: = 0
	for assembly in _world.get_assemblies():
		joints += assembly.get_joints().filter( func(joint: AssemblyJoint) -> bool: return not joint.broken).size()
	var entry: = {
		"scenario": name, 
		"ticks_measured": wall.size(), 
		"stopped_early": truncated, 
		"tick_wall_ms": _stats(wall), 
		"physics_step_ms": _stats(physics), 
		"physics_scripts_ms": _stats(scripts), 
		"bodies": _world.get_entity_count(), 
		"active_bodies": int(Performance.get_monitor(Performance.PHYSICS_2D_ACTIVE_OBJECTS)), 
		"collision_pairs": int(Performance.get_monitor(Performance.PHYSICS_2D_COLLISION_PAIRS)), 
		"joints": joints, 
		"peak_projectiles": peak_projectiles, 
		"peak_particles": peak_particles, 
		"peak_explosion_queue": peak_queue, 
		"burning": _world.thermal.get_burning_count(), 
		"static_memory_mb": snappedf(peak_memory / 1048576.0, 0.1), 
		"realtime_120hz": _stats(wall)["median"] <= 1000.0 / 120.0, 
	}
	if _rendered:
		entry["frame_ms"] = _stats(_frame_intervals)
		entry["tick_wall_note"] = "paced at real time in rendered mode; see frame_ms"
	_report.append(entry)
	var w: Dictionary = entry["tick_wall_ms"]
	var p: Dictionary = entry["physics_step_ms"]
	var sc: Dictionary = entry["physics_scripts_ms"]
	print("%-28s bodies %4d (active %4d) joints %4d | tick wall avg %6.2f med %6.2f p95 %6.2f p99 %6.2f max %7.2f | step med %6.2f p95 %6.2f | scripts med %5.2f | proj %3d part %4d q %3d mem %5.1f MB%s" % [
		name, entry["bodies"], entry["active_bodies"], joints, w["avg"], w["median"], w["p95"], w["p99"], w["max"], 
		p["median"], p["p95"], sc["median"], peak_projectiles, peak_particles, peak_queue, entry["static_memory_mb"], 
		"  [STOPPED EARLY: over time budget]" if truncated else ""])
	if _rendered:
		var f: Dictionary = entry["frame_ms"]
		print("%-28s rendered frame ms avg %6.2f med %6.2f p95 %6.2f p99 %6.2f max %7.2f (%d frames; tick wall above is real-time pacing)" % [
			name, f["avg"], f["median"], f["p95"], f["p99"], f["max"], _frame_intervals.size()])


static func _stats(samples: PackedFloat64Array) -> Dictionary:
	if samples.is_empty():
		return {"avg": 0.0, "median": 0.0, "p95": 0.0, "p99": 0.0, "max": 0.0}
	var sorted: = samples.duplicate()
	sorted.sort()
	var total: = 0.0
	for value in sorted:
		total += value
	var pick: = func(fraction: float) -> float:
		return snappedf(sorted[mini(int(fraction * (sorted.size() - 1)), sorted.size() - 1)], 0.01)
	return {"avg": snappedf(total / sorted.size(), 0.01), "median": pick.call(0.5), "p95": pick.call(0.95), 
		"p99": pick.call(0.99), "max": snappedf(sorted[sorted.size() - 1], 0.01)}


func _write_report() -> void :
	var path: = "user://benchmarks/benchmark_%s_%s.json" % ["rendered" if _rendered else "headless", 
		Time.get_datetime_string_from_system(true).replace(":", "-")]
	DirAccess.make_dir_recursive_absolute(path.get_base_dir())
	var file: = FileAccess.open(path, FileAccess.WRITE)
	if file:
		file.store_string(JSON.stringify({"godot": Engine.get_version_info()["string"], "cpu": OS.get_processor_name(), 
			"logical_cpus": OS.get_processor_count(), "scenarios": _report}, "\t"))
		file.close()
		print("Report written to %s" % ProjectSettings.globalize_path(path))
