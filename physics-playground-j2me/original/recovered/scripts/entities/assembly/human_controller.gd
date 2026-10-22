class_name HumanController
extends AssemblyController




















enum State{ALIVE, INCAPACITATED, DEAD}

const STATE_NAMES: = ["alive", "incapacitated", "dead"]
const STATE_LABELS: = ["Alive", "Incapacitated", "Dead"]

var state: State = State.ALIVE
var vital_parts: = PackedStringArray()
var fatal_joints: = PackedStringArray()
var incapacitating_parts: = PackedStringArray()
var incapacitated_below: = 0.55
var vital_critical_fraction: = 0.0
var incapacitated_destroyed_parts: = 0

var state_reason: = ""


func _configure(controller_params: Dictionary) -> void :
	vital_parts = _string_list(controller_params.get("vital_parts", []))
	fatal_joints = _string_list(controller_params.get("fatal_joints", []))
	incapacitating_parts = _string_list(controller_params.get("incapacitating_parts", []))
	incapacitated_below = clampf(float(controller_params.get("incapacitated_below", incapacitated_below)), 0.0, 1.0)
	vital_critical_fraction = clampf(float(controller_params.get("vital_critical_fraction", 0.0)), 0.0, 1.0)
	incapacitated_destroyed_parts = maxi(int(controller_params.get("incapacitated_destroyed_parts", 0)), 0)


func evaluate() -> void :
	if state == State.DEAD:
		return
	for part_name in vital_parts:
		var part: = assembly.get_part(part_name)
		if part == null:
			_set_state(State.DEAD, "%s lost" % _label(part_name))
			return
		var health: = part.get_health()
		if health and health.get_health_fraction() <= vital_critical_fraction:
			_set_state(State.DEAD, "%s destroyed" % _label(part_name))
			return
	for joint_name in fatal_joints:
		var joint: = assembly.get_joint(joint_name)
		if joint and joint.broken:
			_set_state(State.DEAD, "%s severed" % joint_name.capitalize())
			return
	for part_name in incapacitating_parts:
		var part: = assembly.get_part(part_name)
		var health: = part.get_health() if part else null
		if part == null or (health and health.is_depleted()):
			_set_state(State.INCAPACITATED, "%s destroyed" % _label(part_name))
			return
	if incapacitated_destroyed_parts > 0 and _count_destroyed_parts() >= incapacitated_destroyed_parts:
		_set_state(State.INCAPACITATED, "%d parts destroyed" % _count_destroyed_parts())
		return
	if assembly.get_total_health_fraction() < incapacitated_below:
		_set_state(State.INCAPACITATED, "severe injuries")
		return
	_set_state(State.ALIVE, "")


func revive() -> void :
	state = State.ALIVE
	state_reason = ""
	_apply_visuals()
	evaluate()


func get_state_name() -> String:
	return STATE_NAMES[state]


func is_alive() -> bool:
	return state == State.ALIVE


func is_dead() -> bool:
	return state == State.DEAD


func apply_state_to_part(part: SandboxEntity) -> void :
	part.visual.vitality = state


func serialize_state() -> Dictionary:
	return {"state": get_state_name(), "reason": state_reason}


func deserialize_state(data: Dictionary) -> void :
	var index: = STATE_NAMES.find(str(data.get("state", "alive")))
	state = index as State if index >= 0 else State.ALIVE
	state_reason = str(data.get("reason", ""))
	_apply_visuals()
	evaluate()


func get_inspector_lines() -> PackedStringArray:
	var label: String = STATE_LABELS[state]
	if not state_reason.is_empty():
		label += " (%s)" % state_reason
	return PackedStringArray([
		"Human state: %s" % label, 
		"Overall health: %d%%" % roundi(assembly.get_total_health_fraction() * 100.0), 
	])


func _count_destroyed_parts() -> int:
	var count: = 0
	for part in assembly.get_parts():
		var health: = part.get_health()
		if health and health.is_depleted():
			count += 1
	return count


func _set_state(new_state: State, reason: String) -> void :
	state_reason = reason
	if new_state == state:
		return
	state = new_state
	_apply_visuals()
	assembly.state_changed.emit(get_state_name())


func _apply_visuals() -> void :
	for part in assembly.get_parts():
		apply_state_to_part(part)


func _label(part_name: String) -> String:
	var part: = assembly.definition.assembly.get_part(part_name)
	return part.display_name if part else part_name.capitalize()


static func _string_list(value: Variant) -> PackedStringArray:
	var result: = PackedStringArray()
	if value is Array:
		for item: Variant in value:
			result.append(str(item))
	return result
