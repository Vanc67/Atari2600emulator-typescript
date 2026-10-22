class_name ExplosionSystem
extends Node





























const MAX_STARTS_PER_TICK: = 3
const MAX_QUEUE: = 256
const VICTIM_BUDGET_PER_TICK: = 256
const MAX_VICTIMS_PER_EXPLOSION: = 2048

const MAX_SPEED_CHANGE: = 4000.0
const HEAVY_OBSTACLE_MASS: = 20.0
const WALL_BLOCKING: = 0.15
const HEAVY_BLOCKING: = 0.4
const LIGHT_BLOCKING: = 0.8


const TEAR_FACTOR: = 0.7

signal exploded(at_position: Vector2, params: ExplosionParams)


var explosion_count: = 0

class Job:
	var center: = Vector2.ZERO
	var params: ExplosionParams
	var source_rid: = RID()
	var source_id: = 0
	var victims: Array[SandboxEntity] = []
	var next_victim: = 0

var _world: SandboxWorld
var _queue: Array = []
var _jobs: Array[Job] = []
var _circle: = CircleShape2D.new()
var _shape_query: = PhysicsShapeQueryParameters2D.new()
var _ray_query: = PhysicsRayQueryParameters2D.new()
var _diagnostics: = {"accepted": 0, "rejected": 0, "peak_queue": 0, "victims_processed": 0, 
	"victims_truncated": 0, "deferred_detonations": 0}
var _last_saturation_warning_msec: = -100000


func setup(world: SandboxWorld) -> void :
	_world = world
	_shape_query.shape = _circle
	_shape_query.collision_mask = CollisionLayers.ENTITIES | CollisionLayers.DEBRIS
	_ray_query.collision_mask = CollisionLayers.WORLD | CollisionLayers.ENTITIES





func request(at_position: Vector2, params: ExplosionParams, source: SandboxEntity = null) -> bool:
	if _queue.size() >= MAX_QUEUE:
		_diagnostics["rejected"] += 1
		var now: = Time.get_ticks_msec()
		if now - _last_saturation_warning_msec > 1000:
			_last_saturation_warning_msec = now
			push_warning("ExplosionSystem: queue full (%d); request refused" % MAX_QUEUE)
		return false
	_queue.append([at_position, params, source.get_rid() if is_instance_valid(source) else RID(), 
		source.get_instance_id() if is_instance_valid(source) else 0])
	_diagnostics["accepted"] += 1
	_diagnostics["peak_queue"] = maxi(_diagnostics["peak_queue"], _queue.size())
	return true



func note_deferred_detonation() -> void :
	_diagnostics["deferred_detonations"] += 1


func get_queue_size() -> int:
	return _queue.size()



func get_active_job_count() -> int:
	return _jobs.size()



func is_busy() -> bool:
	return not _queue.is_empty() or not _jobs.is_empty()


func get_diagnostics() -> Dictionary:
	var report: = _diagnostics.duplicate()
	report["queued"] = _queue.size()
	report["active_jobs"] = _jobs.size()
	return report


func clear() -> void :
	_queue.clear()
	_jobs.clear()


func _physics_process(_delta: float) -> void :
	var started: = 0
	while not _queue.is_empty() and started < MAX_STARTS_PER_TICK:
		var entry: Array = _queue.pop_front()
		_start(entry[0], entry[1], entry[2], entry[3])
		started += 1
	if _jobs.is_empty():
		return
	var space: = _world.get_world_2d().direct_space_state
	var budget: = VICTIM_BUDGET_PER_TICK
	while budget > 0 and not _jobs.is_empty():
		var job: = _jobs[0]
		while budget > 0 and job.next_victim < job.victims.size():
			var victim: = job.victims[job.next_victim]
			job.next_victim += 1
			budget -= 1
			if is_instance_valid(victim) and victim.is_inside_tree():
				_affect(victim, job.center, job.params, space, job.source_rid, job.source_id)
				_diagnostics["victims_processed"] += 1
		if job.next_victim >= job.victims.size():
			_jobs.pop_front()



func _start(center: Vector2, params: ExplosionParams, source_rid: RID, source_id: int) -> void :
	explosion_count += 1
	_world.effects.explosion(center, params.radius)
	Audio.play(params.sound, center)
	exploded.emit(center, params)

	var space: = _world.get_world_2d().direct_space_state
	_circle.radius = params.radius
	_shape_query.transform = Transform2D(0.0, center)
	var exclude: Array[RID] = []
	if source_rid.is_valid():
		exclude.append(source_rid)
	_shape_query.exclude = exclude
	var hits: = space.intersect_shape(_shape_query, MAX_VICTIMS_PER_EXPLOSION)
	if hits.size() >= MAX_VICTIMS_PER_EXPLOSION:
		_diagnostics["victims_truncated"] += 1
		push_warning("ExplosionSystem: more than %d bodies in one blast; extra bodies ignored" % MAX_VICTIMS_PER_EXPLOSION)
	var job: = Job.new()
	job.center = center
	job.params = params
	job.source_rid = source_rid
	job.source_id = source_id
	var seen: = {}
	for hit in hits:
		var entity: = hit.get("collider") as SandboxEntity
		if entity == null or seen.has(entity.get_instance_id()):
			continue
		seen[entity.get_instance_id()] = true
		job.victims.append(entity)
	job.victims.sort_custom( func(a: SandboxEntity, b: SandboxEntity) -> bool:
		var da: = a.global_position.distance_squared_to(center)
		var db: = b.global_position.distance_squared_to(center)
		return da < db or (da == db and a.get_instance_id() < b.get_instance_id()))
	if not job.victims.is_empty():
		_jobs.append(job)


func _affect(entity: SandboxEntity, center: Vector2, params: ExplosionParams, 
		space: PhysicsDirectSpaceState2D, source_rid: RID, source_id: int) -> void :
	var offset: = entity.global_position - center
	var distance: = maxf(offset.length() - entity.definition.get_bounding_radius() * 0.5, 0.0)
	if distance >= params.radius:
		return
	var strength: = pow(1.0 - distance / params.radius, params.falloff)
	strength *= _line_of_sight(entity, center, space, source_rid)
	if strength <= 0.01:
		return
	var direction: = offset.normalized() if offset.length_squared() > 1.0 else Vector2.UP

	var speed_change: = 0.0
	if not entity.freeze:
		var impulse: = minf(params.force * WorldUnits.PIXELS_PER_METER * strength, entity.mass * MAX_SPEED_CHANGE)
		speed_change = impulse / entity.mass

		var lever: = Vector2(randf_range(-1, 1), randf_range(-1, 1)) * entity.definition.get_bounding_radius() * 0.3
		entity.apply_impulse(direction * impulse, lever)

	var health: = entity.get_health()
	if health and params.damage > 0.0:
		var damage: = params.damage * strength
		health.apply_damage(damage, DamageInfo.explosion(damage, 
			speed_change / WorldUnits.PIXELS_PER_METER * TEAR_FACTOR, center, direction, source_id))

	var thermal: = entity.get_thermal()
	if thermal and params.heat > 0.0:
		thermal.add_heat(params.heat * strength)
		if params.ignite and strength > 0.25:
			thermal.try_ignite(strength)



func _line_of_sight(entity: SandboxEntity, center: Vector2, space: PhysicsDirectSpaceState2D, 
		source_rid: RID) -> float:
	_ray_query.from = center
	_ray_query.to = entity.global_position
	var exclude: Array[RID] = [entity.get_rid()]
	if source_rid.is_valid():
		exclude.append(source_rid)
	_ray_query.exclude = exclude
	var hit: = space.intersect_ray(_ray_query)
	if hit.is_empty():
		return 1.0
	var blocker: = hit.get("collider") as SandboxEntity
	if blocker == null:
		return WALL_BLOCKING

	if blocker.is_assembly_part() and entity.is_assembly_part() and blocker.assembly == entity.assembly:
		return 1.0
	return HEAVY_BLOCKING if blocker.mass >= HEAVY_OBSTACLE_MASS else LIGHT_BLOCKING
