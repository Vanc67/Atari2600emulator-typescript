class_name SaveValidator
extends RefCounted













const MAX_ENTITIES: = 20000
const MAX_ASSEMBLIES: = 5000
const MAX_COORDINATE: = 10000000.0
const MAX_SPEED: = 1000000.0
const MAX_REPORTED: = 25




const STATE_RULES: = {
	"health": {"current": "nonneg"}, 
	"thermal": {"temperature": "temperature", "burning": "bool", "fuel": "number", "char": "unit"}, 
	"magazine": {"rounds": "count", "reload_progress": "nonneg"}, 
	"firearm": {"trigger_held": "bool"}, 
	"explosive": {"armed": "bool", "fuse": "nonneg"}, 
	"electrical": {"charge": "number"}, 
}


class EntityPlan:
	var uid: = 0
	var type_id: = ""
	var position: = Vector2.ZERO
	var rotation: = 0.0

	var data: Dictionary = {}
	var assembly_uid: = 0
	var part_name: = ""


class AssemblyPlan:
	var uid: = 0
	var type_id: = ""
	var broken_joints: = PackedStringArray()
	var controller_state: Dictionary = {}


class LoadPlan:
	var errors: = PackedStringArray()
	var warnings: = PackedStringArray()
	var missing_types: = PackedStringArray()
	var entities: Array[EntityPlan] = []
	var assemblies: Array[AssemblyPlan] = []
	var next_uid: = 1
	var camera: Dictionary = {}

	func is_valid() -> bool:
		return errors.is_empty()

	func error(message: String) -> void :
		if errors.size() < MAX_REPORTED:
			errors.append(message)
		elif errors.size() == MAX_REPORTED:
			errors.append("... further errors omitted")

	func warn(message: String) -> void :
		if warnings.size() < MAX_REPORTED:
			warnings.append(message)


static func validate(data: Dictionary, registry: EntityRegistry) -> LoadPlan:
	var plan: = LoadPlan.new()
	var raw_entities: Variant = data.get("entities")
	if not raw_entities is Array:
		plan.error("'entities' must be an array (found %s)" % type_string(typeof(raw_entities)))
		return plan
	var raw_assemblies: Variant = data.get("assemblies", [])
	if not raw_assemblies is Array:
		plan.error("'assemblies' must be an array (found %s)" % type_string(typeof(raw_assemblies)))
		return plan
	if raw_entities.size() > MAX_ENTITIES or raw_assemblies.size() > MAX_ASSEMBLIES:
		plan.error("save exceeds limits (%d entities, %d assemblies; max %d / %d)" % [
			raw_entities.size(), raw_assemblies.size(), MAX_ENTITIES, MAX_ASSEMBLIES])
		return plan

	var used_uids: = {}
	var assembly_types: = {}
	_validate_assemblies(raw_assemblies, registry, plan, used_uids, assembly_types)
	_validate_entities(raw_entities, registry, plan, used_uids, assembly_types)
	_validate_world(data, registry, plan, used_uids)
	return plan


static func _validate_assemblies(entries: Array, registry: EntityRegistry, plan: LoadPlan, 
		used_uids: Dictionary, assembly_types: Dictionary) -> void :
	for index in entries.size():
		var where: = "assemblies[%d]" % index
		var entry: Variant = entries[index]
		if not entry is Dictionary:
			plan.error("%s must be an object" % where)
			continue
		var uid: = _read_uid(entry, where, plan, used_uids)
		var type_id: Variant = entry.get("type")
		if not type_id is String or not NamespacedId.is_valid(type_id):
			plan.error("%s.type must be a namespaced id such as \"core:human\"" % where)
			continue
		var joints: Variant = entry.get("joints", {})
		if not joints is Dictionary:
			plan.error("%s.joints must be an object" % where)
			continue
		var broken: = PackedStringArray()
		for joint_name: Variant in joints:
			var state: Variant = joints[joint_name]
			if not state is Dictionary or not state.get("connected", true) is bool:
				plan.error("%s.joints.%s must be {\"connected\": true|false}" % [where, joint_name])
			elif not state.get("connected", true):
				broken.append(str(joint_name))
		var controller: Variant = entry.get("controller", {})
		if not controller is Dictionary:
			plan.error("%s.controller must be an object" % where)
			continue
		if uid <= 0:
			continue
		var definition: = registry.get_entity_definition(type_id)
		if definition == null:
			_note_missing(plan, type_id)
			continue
		if not definition.is_assembly():
			plan.error("%s.type '%s' is not an assembly" % [where, type_id])
			continue
		for joint_name in broken:
			if definition.assembly.get_joint(joint_name) == null:
				plan.warn("%s: unknown joint '%s' ignored" % [where, joint_name])
		var assembly: = AssemblyPlan.new()
		assembly.uid = uid
		assembly.type_id = type_id
		assembly.broken_joints = broken
		assembly.controller_state = controller
		plan.assemblies.append(assembly)
		assembly_types[uid] = type_id


static func _validate_entities(entries: Array, registry: EntityRegistry, plan: LoadPlan, 
		used_uids: Dictionary, assembly_types: Dictionary) -> void :
	var claimed_parts: = {}
	for index in entries.size():
		var where: = "entities[%d]" % index
		var entry: Variant = entries[index]
		if not entry is Dictionary:
			plan.error("%s must be an object" % where)
			continue
		var uid: = _read_uid(entry, where, plan, used_uids)
		var type_id: Variant = entry.get("type")
		if not type_id is String or not NamespacedId.is_valid(type_id):
			plan.error("%s.type must be a namespaced id such as \"core:wooden_box\"" % where)
			continue
		var position: = _read_vector(entry, "position", where, plan, true, MAX_COORDINATE)
		var rotation: = _read_number(entry, "rotation", where, plan, 0.0, false)
		_read_vector(entry, "linear_velocity", where, plan, false, MAX_SPEED)
		_read_number(entry, "angular_velocity", where, plan, 0.0, false)
		if entry.has("frozen") and not entry["frozen"] is bool:
			plan.error("%s.frozen must be true or false" % where)
		if entry.has("properties") and not entry["properties"] is Dictionary:
			plan.error("%s.properties must be an object" % where)
		var definition: = registry.get_entity_definition(type_id)
		_validate_component_states(entry, where, definition, plan)

		var assembly_uid: = 0
		var part_name: = ""
		if entry.has("assembly"):
			var membership: Variant = entry["assembly"]
			if not membership is Dictionary or not _is_integral(membership.get("uid"))\
			or not membership.get("part") is String or str(membership.get("part")).is_empty():
				plan.error("%s.assembly must be {\"uid\": <integer>, \"part\": <name>}" % where)
			else:
				assembly_uid = int(membership["uid"])
				part_name = membership["part"]
				var claim: = "%d/%s" % [assembly_uid, part_name]
				if claimed_parts.has(claim):
					plan.error("%s: assembly %d part '%s' is claimed by two entities" % [where, assembly_uid, part_name])
				claimed_parts[claim] = true

		if uid <= 0 or not plan.is_valid():
			continue
		if definition == null:
			_note_missing(plan, type_id)
			continue
		if definition.is_assembly():
			plan.error("%s.type '%s' is an assembly; entities must be single bodies" % [where, type_id])
			continue
		if assembly_uid > 0:
			var assembly_type: String = assembly_types.get(assembly_uid, "")
			if assembly_type.is_empty():
				plan.warn("%s: assembly %d is missing; '%s' loads as a standalone body" % [where, assembly_uid, part_name])
				assembly_uid = 0
			elif registry.get_entity_definition(assembly_type).assembly.get_part(part_name) == null:
				plan.warn("%s: '%s' has no part '%s'; it loads as a standalone body" % [where, assembly_type, part_name])
				assembly_uid = 0
		var entity: = EntityPlan.new()
		entity.uid = uid
		entity.type_id = type_id
		entity.position = position
		entity.rotation = rotation
		entity.data = entry
		entity.assembly_uid = assembly_uid
		entity.part_name = part_name if assembly_uid > 0 else ""
		plan.entities.append(entity)


static func _validate_component_states(entry: Dictionary, where: String, definition: EntityDefinition, 
		plan: LoadPlan) -> void :
	if not entry.has("components"):
		return
	var states: Variant = entry["components"]
	if not states is Dictionary:
		plan.error("%s.components must be an object" % where)
		return
	for component_type: Variant in states:
		var state: Variant = states[component_type]
		var at: = "%s.components.%s" % [where, component_type]
		if not state is Dictionary:
			plan.error("%s must be an object" % at)
			continue
		var rules: Dictionary = STATE_RULES.get(str(component_type), {})
		for key: Variant in state:
			if not rules.has(key):
				plan.warn("%s.%s is not a known state field (ignored)" % [at, key])
				continue
			var problem: = _check_value(state[key], rules[key])
			if not problem.is_empty():
				plan.error("%s.%s %s" % [at, key, problem])
		if definition and not definition.is_assembly() and not definition.components.has(str(component_type)):
			plan.warn("%s: '%s' no longer has this component (state ignored)" % [at, definition.id])


static func _validate_world(data: Dictionary, registry: EntityRegistry, plan: LoadPlan, used_uids: Dictionary) -> void :
	var highest: = 0
	for uid: int in used_uids:
		highest = maxi(highest, uid)
	plan.next_uid = highest + 1
	var world: Variant = data.get("world", {})
	if not world is Dictionary:
		plan.error("'world' must be an object")
		return
	if world.has("next_entity_uid"):
		if not _is_integral(world["next_entity_uid"]) or int(world["next_entity_uid"]) < 1:
			plan.error("world.next_entity_uid must be a positive integer")
		else:
			plan.next_uid = maxi(plan.next_uid, int(world["next_entity_uid"]))
	var camera: Variant = world.get("camera", {})
	if not camera is Dictionary:
		plan.error("world.camera must be an object")
	else:
		_read_vector(camera, "position", "world.camera", plan, false, MAX_COORDINATE)
		var zoom: = _read_number(camera, "zoom", "world.camera", plan, 1.0, false)
		if zoom <= 0.0:
			plan.error("world.camera.zoom must be positive")
		plan.camera = camera
	var mods: Variant = data.get("mods", {})
	if mods is Dictionary:
		var installed: = registry.get_mod_versions()
		for mod_id: Variant in mods:
			if not installed.has(mod_id):
				plan.warn("the save was made with mod '%s', which is not installed" % mod_id)




static func _read_uid(entry: Dictionary, where: String, plan: LoadPlan, used_uids: Dictionary) -> int:
	var raw: Variant = entry.get("uid")
	if not _is_integral(raw) or float(raw) < 1.0 or float(raw) > 9.00719925474099e+15:
		plan.error("%s.uid must be a positive integer" % where)
		return 0
	var uid: = int(raw)
	if used_uids.has(uid):
		plan.error("%s.uid %d is used more than once" % [where, uid])
		return 0
	used_uids[uid] = true
	return uid


static func _read_vector(entry: Dictionary, key: String, where: String, plan: LoadPlan, 
		required: bool, limit: float) -> Vector2:
	if not entry.has(key):
		if required:
			plan.error("%s.%s is missing" % [where, key])
		return Vector2.ZERO
	var raw: Variant = entry[key]
	if not raw is Array or raw.size() != 2 or not _is_finite_number(raw[0]) or not _is_finite_number(raw[1]):
		plan.error("%s.%s must be [x, y] with two finite numbers" % [where, key])
		return Vector2.ZERO
	var vector: = Vector2(float(raw[0]), float(raw[1]))
	if absf(vector.x) > limit or absf(vector.y) > limit:
		plan.error("%s.%s is out of range (limit %s)" % [where, key, limit])
	return vector


static func _read_number(entry: Dictionary, key: String, where: String, plan: LoadPlan, 
		default_value: float, required: bool) -> float:
	if not entry.has(key):
		if required:
			plan.error("%s.%s is missing" % [where, key])
		return default_value
	if not _is_finite_number(entry[key]):
		plan.error("%s.%s must be a finite number" % [where, key])
		return default_value
	return float(entry[key])



static func _check_value(value: Variant, rule: String) -> String:
	match rule:
		"bool":
			return "" if value is bool else "must be true or false"
		"number":
			return "" if _is_finite_number(value) else "must be a finite number"
		"nonneg":
			return "" if _is_finite_number(value) and float(value) >= 0.0 else "must be a finite number ≥ 0"
		"unit":
			return "" if _is_finite_number(value) and float(value) >= 0.0 and float(value) <= 1.0 else "must be a number in [0, 1]"
		"count":
			return "" if _is_integral(value) and float(value) >= 0.0 else "must be a whole number ≥ 0"
		"temperature":
			return "" if _is_finite_number(value) and float(value) >= -273.15 and float(value) <= 100000.0\
			else "must be a temperature in [-273.15, 100000] °C"
	return ""


static func _is_finite_number(value: Variant) -> bool:
	return (value is float and is_finite(value)) or value is int


static func _is_integral(value: Variant) -> bool:
	return value is int or (value is float and is_finite(value) and floorf(value) == value)


static func _note_missing(plan: LoadPlan, type_id: String) -> void :
	if not plan.missing_types.has(type_id):
		plan.missing_types.append(type_id)
