class_name HealthComponent
extends EntityComponent











signal damaged(info: DamageInfo)
signal healed(amount: float)
signal health_changed(current: float, maximum: float)
signal depleted(info: DamageInfo)

signal restored

const ON_DEPLETED_NONE: = "none"
const ON_DEPLETED_REMOVE: = "remove"

var max_health: = 100.0
var current_health: = 100.0
var on_depleted: = ON_DEPLETED_NONE

var invulnerable: = false
var damage_resistance: = 0.0
var damage_multipliers: Dictionary = {}


func get_component_type() -> StringName:
	return &"health"


func configure(params: Dictionary, _definition: EntityDefinition, material: MaterialDefinition) -> void :
	var base_health: = float(params.get("max_health", 100.0))
	var durability: = material.durability if material else 1.0
	max_health = maxf(base_health * durability, 1.0)
	current_health = max_health
	var behaviour: = str(params.get("on_depleted", ON_DEPLETED_NONE))
	on_depleted = behaviour if behaviour in [ON_DEPLETED_NONE, ON_DEPLETED_REMOVE] else ON_DEPLETED_NONE
	damage_resistance = clampf(float(params.get("damage_resistance", 0.0)), 0.0, 1.0)
	var multipliers: Variant = params.get("damage_multipliers", {})
	if multipliers is Dictionary:
		for kind_name: Variant in multipliers:
			damage_multipliers[str(kind_name)] = maxf(float(multipliers[kind_name]), 0.0)





func apply_damage(amount: float, info: DamageInfo = null) -> float:
	if amount <= 0.0 or invulnerable or is_depleted():
		return 0.0
	if info == null:
		info = DamageInfo.generic(amount)
	amount *= (1.0 - damage_resistance) * float(damage_multipliers.get(info.get_kind_name(), 1.0))
	if amount <= 0.0:
		return 0.0
	var applied: = minf(amount, current_health)
	current_health -= applied
	info.applied = applied
	damaged.emit(info)
	_notify_changed()
	if is_depleted():
		depleted.emit(info)
		if on_depleted == ON_DEPLETED_REMOVE and entity:
			entity.request_destroy()
	return applied



func heal(amount: float) -> float:
	if amount <= 0.0:
		return 0.0
	var was_depleted: = is_depleted()
	var healed_amount: = minf(amount, max_health - current_health)
	if healed_amount <= 0.0:
		return 0.0
	current_health += healed_amount
	healed.emit(healed_amount)
	_notify_changed()
	if was_depleted:
		restored.emit()
	return healed_amount


func heal_full() -> float:
	return heal(max_health)


func is_depleted() -> bool:
	return current_health <= 0.0



func is_destroyed() -> bool:
	return is_depleted()


func is_damaged() -> bool:
	return current_health < max_health


func get_health_fraction() -> float:
	return current_health / max_health


func get_damage_fraction() -> float:
	return 1.0 - get_health_fraction()


func serialize_state() -> Dictionary:
	if is_equal_approx(current_health, max_health):
		return {}
	return {"current": current_health}


func deserialize_state(data: Dictionary) -> void :
	current_health = clampf(float(data.get("current", max_health)), 0.0, max_health)
	_notify_changed()


func get_inspector_lines() -> PackedStringArray:
	var lines: = PackedStringArray([
		"Health: %d / %d" % [ceili(current_health), roundi(max_health)], 
		"Damage: %d%%" % roundi(get_damage_fraction() * 100.0), 
	])
	if is_depleted():
		lines.append("Destroyed")
	return lines


func _notify_changed() -> void :
	health_changed.emit(current_health, max_health)
	if entity:
		entity.set_damage_level(get_damage_fraction())
