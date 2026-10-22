class_name ImpactDamageComponent
extends EntityComponent



































const MAX_TRACKED_COLLIDERS: = 8
const COOLDOWN_CACHE_LIMIT: = 32

const MIN_MELEE_THRESHOLD: = 0.1

var min_impact_speed: = 7.0
var damage_per_speed: = 12.0
var min_impulse: = 0.0
var max_damage_per_hit: = 1000.0

var cooldown: = 0.15

var _cooldown_until: Dictionary = {}

var _collider_ids: = PackedInt64Array()
var _collider_impulses: = PackedFloat64Array()
var _collider_changes: = PackedFloat64Array()
var _collider_points: = PackedVector2Array()


func get_component_type() -> StringName:
	return &"impact_damage"


func configure(params: Dictionary, _definition: EntityDefinition, _material: MaterialDefinition) -> void :
	min_impact_speed = maxf(float(params.get("min_impact_speed", min_impact_speed)), 0.0)
	damage_per_speed = maxf(float(params.get("damage_per_speed", damage_per_speed)), 0.0)
	min_impulse = maxf(float(params.get("min_impulse", min_impulse)), 0.0)
	max_damage_per_hit = maxf(float(params.get("max_damage_per_hit", max_damage_per_hit)), 0.0)
	cooldown = maxf(float(params.get("cooldown", 0.15)), 0.0)
	_collider_ids.resize(MAX_TRACKED_COLLIDERS)
	_collider_impulses.resize(MAX_TRACKED_COLLIDERS)
	_collider_changes.resize(MAX_TRACKED_COLLIDERS)
	_collider_points.resize(MAX_TRACKED_COLLIDERS)





func evaluate_contacts(state: PhysicsDirectBodyState2D, linear_change: Vector2, angular_change: float, 
		previous_linear_change: Vector2, previous_angular_change: float) -> void :
	var inverse_mass: = state.inverse_mass
	if inverse_mass <= 0.0:
		return
	var threshold_px: = min_impact_speed * WorldUnits.PIXELS_PER_METER
	var origin: = state.transform.origin
	var tracked: = 0
	for index in state.get_contact_count():
		var point: = state.get_contact_local_position(index)
		var normal: = state.get_contact_local_normal(index)
		var r: = point - origin
		var change_now: = linear_change + Vector2( - angular_change * r.y, angular_change * r.x)
		var change_before: = previous_linear_change + Vector2( - previous_angular_change * r.y, previous_angular_change * r.x)
		var normal_change: = maxf(change_now.dot(normal), change_before.dot(normal))
		if normal_change < threshold_px * MIN_MELEE_THRESHOLD:
			continue
		if normal_change < threshold_px * _threshold_scale(state.get_contact_collider_object(index)):
			continue
		var collider_id: = state.get_contact_collider_id(index)
		var slot: = -1
		for existing in tracked:
			if _collider_ids[existing] == collider_id:
				slot = existing
				break
		if slot == -1:
			if tracked == MAX_TRACKED_COLLIDERS:
				continue
			slot = tracked
			tracked += 1
			_collider_ids[slot] = collider_id
			_collider_impulses[slot] = 0.0
			_collider_changes[slot] = 0.0
			_collider_points[slot] = point
		_collider_impulses[slot] += state.get_contact_impulse(index).length()
		_collider_changes[slot] = maxf(_collider_changes[slot], normal_change)

	for slot in tracked:
		var impulse_px: float = _collider_impulses[slot]
		var severity_px: float = _collider_changes[slot]
		if impulse_px > 0.0:
			severity_px = minf(severity_px, impulse_px * inverse_mass)
			if impulse_px / WorldUnits.PIXELS_PER_METER < min_impulse:
				continue
		var collider: = instance_from_id(_collider_ids[slot])
		if severity_px < threshold_px * _threshold_scale(collider):
			continue
		var speed: = severity_px / WorldUnits.PIXELS_PER_METER
		var impulse_si: = maxf(impulse_px, severity_px / inverse_mass) / WorldUnits.PIXELS_PER_METER
		_register_impact.call_deferred(speed, impulse_si, _collider_points[slot], _collider_ids[slot])


func compute_damage(speed: float, threshold_scale: = 1.0, multiplier: = 1.0) -> float:
	return clampf((speed - min_impact_speed * threshold_scale) * damage_per_speed * multiplier, 
		0.0, max_damage_per_hit)



static func _melee_of(collider: Object) -> MeleeComponent:
	var other: = collider as SandboxEntity
	return other.get_component(&"melee") as MeleeComponent if other else null


static func _threshold_scale(collider: Object) -> float:
	var melee: = _melee_of(collider)
	return melee.threshold_multiplier if melee else 1.0


func _register_impact(speed: float, impulse: float, point: Vector2, collider_id: int) -> void :
	var now: = GameState.simulation_time
	if now < float(_cooldown_until.get(collider_id, -1.0)):
		return
	if _cooldown_until.size() > COOLDOWN_CACHE_LIMIT:


		for key: int in _cooldown_until.keys():
			if float(_cooldown_until[key]) <= now:
				_cooldown_until.erase(key)
	_cooldown_until[collider_id] = now + cooldown
	var health: = entity.get_component(&"health") as HealthComponent if entity else null
	var melee: = _melee_of(instance_from_id(collider_id))
	var damage: = compute_damage(speed, melee.threshold_multiplier if melee else 1.0, 
		melee.damage_multiplier if melee else 1.0)
	if health and damage > 0.0:
		health.apply_damage(damage, DamageInfo.impact(damage, speed, impulse, point, collider_id))
