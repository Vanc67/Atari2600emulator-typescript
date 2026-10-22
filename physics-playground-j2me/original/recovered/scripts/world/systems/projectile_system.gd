class_name ProjectileSystem
extends Node2D


































enum Outcome{STOP, PENETRATE, RICOCHET}

const MAX_PROJECTILES: = 600

const MAX_SEGMENTS_PER_TICK: = 6

const MAX_CARRY_TIME: = 0.25
const THICKNESS_UNIT: = 40.0

const MAX_PUSH_SPEED: = 600.0

const MIN_SPEED: = 2500.0
const RICOCHET_MIN_GRAZING: = 0.45
const RICOCHET_SPEED_KEEP: = 0.6
const RICOCHET_DAMAGE_FRACTION: = 0.3

class Projectile:
	var definition: ProjectileDefinition
	var position: = Vector2.ZERO
	var trail_start: = Vector2.ZERO
	var velocity: = Vector2.ZERO
	var initial_speed: = 1.0
	var base_damage: = 0.0
	var penetration_left: = 0.0
	var life: = 0.0
	var source_id: = 0
	var shooter_rid: = RID()

	var shooter_grace: = 1

	var carry_time: = 0.0

	var last_hit_id: = 0

	var check_start: = true


var stats: = {"fired": 0, "hits": 0, "penetrations": 0, "ricochets": 0, "stopped": 0, "expired": 0, 
	"budget_carries": 0, "stalled": 0}

var _world: SandboxWorld
var _surface_material: MaterialDefinition
var _projectiles: Array[Projectile] = []
var _query: = PhysicsRayQueryParameters2D.new()
var _point_query: = PhysicsPointQueryParameters2D.new()
var _exclude: Array[RID] = []
var _gravity: = Vector2(0, 980)


func setup(world: SandboxWorld, registry: EntityRegistry) -> void :
	_world = world

	_surface_material = registry.get_material("core:concrete")
	_query.collision_mask = CollisionLayers.WORLD | CollisionLayers.ENTITIES
	_query.collide_with_areas = false
	_query.hit_from_inside = false
	_point_query.collision_mask = CollisionLayers.WORLD | CollisionLayers.ENTITIES
	_gravity = Vector2(0, float(ProjectSettings.get_setting("physics/2d/default_gravity", 980.0)))



func fire(definition: ProjectileDefinition, from: Vector2, velocity: Vector2, damage: float, 
		source: SandboxEntity = null) -> bool:
	if _projectiles.size() >= MAX_PROJECTILES:
		return false
	var projectile: = Projectile.new()
	projectile.definition = definition
	projectile.position = from
	projectile.trail_start = from
	projectile.velocity = velocity
	projectile.initial_speed = maxf(velocity.length(), 1.0)
	projectile.base_damage = damage
	projectile.penetration_left = definition.penetration
	projectile.life = definition.lifetime
	if source:
		projectile.shooter_rid = source.get_rid()
		projectile.source_id = source.get_instance_id()
	_projectiles.append(projectile)
	stats["fired"] += 1
	return true


func get_count() -> int:
	return _projectiles.size()



func get_positions() -> PackedVector2Array:
	var positions: = PackedVector2Array()
	for projectile in _projectiles:
		positions.append(projectile.position)
	return positions


func clear() -> void :
	_projectiles.clear()
	queue_redraw()


func _physics_process(delta: float) -> void :
	if _projectiles.is_empty():
		return
	var bounds: = _world.get_escape_bounds()
	for index in range(_projectiles.size() - 1, -1, -1):
		var projectile: = _projectiles[index]
		projectile.life -= delta
		if projectile.life <= 0.0 or not bounds.has_point(projectile.position):
			_remove(index, "expired")
			continue
		projectile.velocity += _gravity * projectile.definition.gravity_scale * delta
		if projectile.definition.drag > 0.0:
			projectile.velocity *= maxf(1.0 - projectile.definition.drag * delta, 0.0)
		if projectile.velocity.length_squared() < MIN_SPEED * MIN_SPEED:
			_remove(index, "expired")
			continue
		projectile.trail_start = projectile.position
		var travel_time: = delta + projectile.carry_time
		projectile.carry_time = 0.0
		if not _advance(projectile, travel_time):
			_remove(index, "stopped")
		elif projectile.carry_time > MAX_CARRY_TIME:
			stats["stalled"] += 1
			_remove(index, "stalled")
		elif projectile.shooter_grace > 0:
			projectile.shooter_grace -= 1
	queue_redraw()





func _advance(projectile: Projectile, travel_time: float) -> bool:
	var space: = get_world_2d().direct_space_state
	var remaining: = travel_time
	var temporary_exclusion: = RID()
	if projectile.check_start:
		projectile.check_start = false
		if _check_start_overlap(projectile, space):
			return false
	for segment in MAX_SEGMENTS_PER_TICK:
		var target: = projectile.position + projectile.velocity * remaining
		_exclude.clear()
		if projectile.shooter_grace > 0 and projectile.shooter_rid.is_valid():
			_exclude.append(projectile.shooter_rid)
		if temporary_exclusion.is_valid():
			_exclude.append(temporary_exclusion)
		temporary_exclusion = RID()
		_query.from = projectile.position
		_query.to = target
		_query.exclude = _exclude
		var hit: = space.intersect_ray(_query)
		if hit.is_empty():
			projectile.position = target
			return true
		var hit_point: Vector2 = hit["position"]
		var full: = projectile.position.distance_to(target)
		if full > 0.0:
			remaining *= 1.0 - projectile.position.distance_to(hit_point) / full
		var outcome: = _resolve_hit(projectile, hit["collider"] as SandboxEntity, hit_point, hit["normal"])
		match outcome:
			Outcome.STOP:
				projectile.position = hit_point
				return false
			Outcome.PENETRATE:


				projectile.position = hit_point + projectile.velocity.normalized() * 0.5
			Outcome.RICOCHET:
				projectile.position = hit_point + (hit["normal"] as Vector2) * 0.5
				temporary_exclusion = hit["rid"]

	if remaining > 0.0:
		projectile.carry_time = remaining
		stats["budget_carries"] += 1
	return true





func _check_start_overlap(projectile: Projectile, space: PhysicsDirectSpaceState2D) -> bool:
	_point_query.position = projectile.position
	_exclude.clear()
	if projectile.shooter_rid.is_valid():
		_exclude.append(projectile.shooter_rid)
	_point_query.exclude = _exclude
	var hits: = space.intersect_point(_point_query, 4)
	if hits.is_empty():
		return false
	var direction: = projectile.velocity.normalized()
	var outcome: = _resolve_hit(projectile, hits[0].get("collider") as SandboxEntity, projectile.position, - direction)
	if outcome == Outcome.STOP:
		return true
	if outcome == Outcome.PENETRATE:
		projectile.position += direction * 0.5
	return false


func _resolve_hit(projectile: Projectile, entity: SandboxEntity, hit_point: Vector2, normal: Vector2) -> Outcome:
	stats["hits"] += 1
	var direction: = projectile.velocity.normalized()
	var material: = entity.material_definition if entity else _surface_material
	var speed_fraction: = projectile.velocity.length() / projectile.initial_speed
	var damage: = projectile.base_damage * speed_fraction

	var grazing: = 1.0 - absf(direction.dot(normal))
	var ricochet: = material != null and material.ricochet_chance > 0.0 and grazing > RICOCHET_MIN_GRAZING\
	and randf() < material.ricochet_chance * (grazing - RICOCHET_MIN_GRAZING) / (1.0 - RICOCHET_MIN_GRAZING) * 1.5

	var hit_id: = entity.get_instance_id() if entity else 0
	if entity and hit_id != projectile.last_hit_id:
		_apply_hit(projectile, entity, hit_point, direction, 
			damage * (RICOCHET_DAMAGE_FRACTION if ricochet else 1.0))
	projectile.last_hit_id = hit_id

	var effects: = _world.effects
	if material:
		effects.burst(material.impact_effect, hit_point, normal, 0.8)
		Audio.play(material.impact_sound, hit_point, -4.0)
	if ricochet:
		projectile.velocity = projectile.velocity.bounce(normal) * RICOCHET_SPEED_KEEP
		projectile.base_damage *= 0.6
		stats["ricochets"] += 1
		effects.burst("sparks", hit_point, projectile.velocity.normalized(), 1.0)
		Audio.play("core:ricochet", hit_point, -6.0)
		return Outcome.RICOCHET

	if entity == null or material == null:
		stats["stopped"] += 1
		return Outcome.STOP
	var cost: = material.penetration_resistance * entity.definition.get_min_thickness() / THICKNESS_UNIT
	if projectile.penetration_left > cost:
		var before: = projectile.penetration_left
		projectile.penetration_left -= cost
		projectile.velocity *= sqrt(projectile.penetration_left / before)
		stats["penetrations"] += 1
		return Outcome.PENETRATE
	stats["stopped"] += 1
	return Outcome.STOP


func _apply_hit(projectile: Projectile, entity: SandboxEntity, hit_point: Vector2, direction: Vector2, 
		damage: float) -> void :
	var health: = entity.get_health()
	if health and damage > 0.0:
		health.apply_damage(damage, DamageInfo.projectile(damage, hit_point, direction, projectile.source_id))
	if entity.freeze:
		return

	var push: = projectile.definition.mass_grams / 1000.0 * projectile.velocity.length()\
	* projectile.definition.impulse_multiplier
	push = minf(push, entity.mass * MAX_PUSH_SPEED)
	entity.apply_impulse(direction * push, hit_point - entity.global_position)


func _remove(index: int, reason: String) -> void :
	_projectiles.remove_at(index)
	if reason == "expired":
		stats["expired"] += 1


func _draw() -> void :
	for projectile in _projectiles:
		var definition: = projectile.definition
		var tail: = projectile.position - projectile.velocity * definition.trail_time


		if tail.distance_squared_to(projectile.position) > projectile.trail_start.distance_squared_to(projectile.position):
			tail = projectile.trail_start
		draw_line(tail, projectile.position, definition.trail_color, definition.trail_width)
