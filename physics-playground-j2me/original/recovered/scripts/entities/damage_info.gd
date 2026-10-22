class_name DamageInfo
extends RefCounted



enum Kind{GENERIC, IMPACT, PROJECTILE, EXPLOSION, FIRE}


const KIND_NAMES: PackedStringArray = ["generic", "impact", "projectile", "explosion", "fire"]

var kind: Kind = Kind.GENERIC

var amount: = 0.0

var applied: = 0.0

var impact_speed: = 0.0

var impulse: = 0.0
var world_position: = Vector2.ZERO

var source_id: = 0

var direction: = Vector2.ZERO


static func generic(damage_amount: float) -> DamageInfo:
	var info: = DamageInfo.new()
	info.amount = damage_amount
	return info


static func impact(damage_amount: float, speed: float, impulse_amount: float, 
		position: Vector2, source_instance_id: int) -> DamageInfo:
	var info: = DamageInfo.new()
	info.kind = Kind.IMPACT
	info.amount = damage_amount
	info.impact_speed = speed
	info.impulse = impulse_amount
	info.world_position = position
	info.source_id = source_instance_id
	return info


static func projectile(damage_amount: float, position: Vector2, travel_direction: Vector2, 
		source_instance_id: int) -> DamageInfo:
	var info: = DamageInfo.new()
	info.kind = Kind.PROJECTILE
	info.amount = damage_amount
	info.world_position = position
	info.direction = travel_direction
	info.source_id = source_instance_id
	return info


static func explosion(damage_amount: float, speed_change: float, position: Vector2, 
		push_direction: Vector2, source_instance_id: int) -> DamageInfo:
	var info: = DamageInfo.new()
	info.kind = Kind.EXPLOSION
	info.amount = damage_amount
	info.impact_speed = speed_change
	info.world_position = position
	info.direction = push_direction
	info.source_id = source_instance_id
	return info


static func fire(damage_amount: float) -> DamageInfo:
	var info: = DamageInfo.new()
	info.kind = Kind.FIRE
	info.amount = damage_amount
	return info


func is_impact() -> bool:
	return kind == Kind.IMPACT



func can_tear() -> bool:
	return kind == Kind.IMPACT or kind == Kind.EXPLOSION


func get_kind_name() -> String:
	return KIND_NAMES[kind]
