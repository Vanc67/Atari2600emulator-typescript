class_name SandboxTools
extends RefCounted







enum Tool{DRAG, INSPECT, FREEZE, DELETE, HEAL, DAMAGE, IGNITE, EXTINGUISH, EXPLODE}

const TOOL_NAMES: = ["Drag", "Inspect", "Freeze", "Delete", "Heal", "Damage", "Ignite", "Extinguish", "Explode"]
const TOOL_HINTS: = [
	"Drag: grab, throw and select objects", 
	"Inspect: select objects without moving them", 
	"Freeze: click to freeze or unfreeze (whole ragdoll)", 
	"Delete: click to remove (whole ragdoll if attached)", 
	"Heal: click to restore health (revives ragdolls)", 
	"Damage: click to deal the configured damage to one part", 
	"Ignite: click to set flammable objects on fire", 
	"Extinguish: click to put out fires (whole ragdoll)", 
	"Explode: click anywhere to create an explosion", 
]

const TOOL_EXPLOSION: = {"radius": 260.0, "force": 1100.0, "damage": 160.0, "falloff": 1.4, "ignite": true}


static func freeze_toggle(world: SandboxWorld, entity: SandboxEntity) -> void :
	var frozen: = not entity.is_frozen()
	for member in world.get_connected_group(entity):
		member.set_frozen(frozen)


static func delete(world: SandboxWorld, entity: SandboxEntity) -> void :
	for member in world.get_connected_group(entity):
		world.remove_entity(member)


static func heal(entity: SandboxEntity) -> void :
	if entity.is_assembly_part():
		entity.assembly.heal_all()
		for part in entity.assembly.get_parts():
			extinguish_entity(part)
	else:
		var health: = entity.get_health()
		if health:
			health.heal_full()
		extinguish_entity(entity)



static func damage(entity: SandboxEntity, amount: float) -> float:
	var health: = entity.get_health()
	if health == null:
		return 0.0
	return health.apply_damage(amount, DamageInfo.generic(amount))



static func ignite(entity: SandboxEntity) -> bool:
	var thermal: = entity.get_thermal()
	if thermal == null:
		return false
	return thermal.ignite()


static func extinguish(world: SandboxWorld, entity: SandboxEntity) -> void :
	for member in world.get_connected_group(entity):
		extinguish_entity(member)


static func extinguish_entity(entity: SandboxEntity) -> void :
	var thermal: = entity.get_thermal()
	if thermal and (thermal.burning or thermal.temperature > thermal.rest_temperature + 1.0):
		thermal.extinguish()
		thermal.temperature = thermal.rest_temperature
		thermal.thermal_tick(0.0)


static func explode_at(world: SandboxWorld, at_position: Vector2) -> void :
	world.explosions.request(at_position, ExplosionParams.from_dict(TOOL_EXPLOSION))
