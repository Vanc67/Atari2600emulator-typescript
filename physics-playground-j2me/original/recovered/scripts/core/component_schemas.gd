class_name ComponentSchemas
extends RefCounted

















const DAMAGE_KINDS: = ["generic", "impact", "projectile", "explosion", "fire"]

const SCHEMAS: = {
	"health": {"params": {
		"max_health": {"type": "float", "min": 1.0, "max": 1000000.0}, 
		"on_depleted": {"type": "enum", "values": ["none", "remove"]}, 
		"damage_resistance": {"type": "float", "min": 0.0, "max": 1.0}, 
		"damage_multipliers": {"type": "kind_floats", "min": 0.0, "max": 100.0}, 
	}}, 
	"thermal": {"params": {
		"initial_temperature": {"type": "float", "min": -273.15, "max": 5000.0}, 
		"ignition_temperature": {"type": "float", "min": -1.0, "max": 100000.0}, 
		"flammability": {"type": "float", "min": 0.0, "max": 1.0}, 
		"heat_resistance": {"type": "float", "min": 0.0, "max": 1.0}, 
		"burn_damage": {"type": "float", "min": 0.0, "max": 10000.0}, 
		"burn_duration": {"type": "float", "min": -1.0, "max": 3600.0}, 
		"burn_temperature": {"type": "float", "min": -273.15, "max": 100000.0}, 
		"heat_radius": {"type": "float", "min": 0.0, "max": 2000.0}, 
		"heat_output": {"type": "float", "min": 0.0, "max": 100000.0}, 
		"cooling": {"type": "float", "min": 0.0, "max": 10.0}, 
	}}, 
	"electrical": {"params": {
		"conductivity": {"type": "float", "min": 0.0, "max": 1.0}, 
	}}, 
	"impact_damage": {"requires": ["health"], "params": {
		"min_impact_speed": {"type": "float", "min": 0.0, "max": 1000.0}, 
		"damage_per_speed": {"type": "float", "min": 0.0, "max": 10000.0}, 
		"min_impulse": {"type": "float", "min": 0.0, "max": 1000000.0}, 
		"max_damage_per_hit": {"type": "float", "min": 0.0, "max": 1000000.0}, 
		"cooldown": {"type": "float", "min": 0.0, "max": 60.0}, 
	}}, 
	"magazine": {"params": {
		"capacity": {"type": "int", "min": 1, "max": 1000}, 
		"reload_time": {"type": "float", "min": 0.0, "max": 60.0}, 
		"per_round": {"type": "bool"}, 
		"infinite": {"type": "bool"}, 
		"reload_sound": {"type": "sound"}, 
	}}, 
	"firearm": {"conflicts": ["debris"], "params": {
		"projectile": {"type": "projectile", "required": true}, 
		"fire_rate": {"type": "float", "min": 0.1, "max": 60.0}, 
		"automatic": {"type": "bool"}, 
		"speed": {"type": "float", "min": 1.0, "max": 3000.0}, 
		"damage": {"type": "float", "min": 0.0, "max": 100000.0}, 
		"pellets": {"type": "int", "min": 1, "max": 32}, 
		"spread": {"type": "float", "min": 0.0, "max": 90.0}, 
		"recoil": {"type": "float", "min": 0.0, "max": 1000.0}, 
		"muzzle": {"type": "vector2", "min": -4000.0, "max": 4000.0}, 
		"muzzle_flash": {"type": "float", "min": 0.0, "max": 500.0}, 
		"sound": {"type": "sound"}, 
		"dry_sound": {"type": "sound"}, 
	}}, 
	"explosive": {"conflicts": ["debris"], "params": {
		"radius": {"type": "float", "min": 10.0, "max": 3000.0}, 
		"force": {"type": "float", "min": 0.0, "max": 100000.0}, 
		"damage": {"type": "float", "min": 0.0, "max": 100000.0}, 
		"falloff": {"type": "float", "min": 0.25, "max": 4.0}, 
		"ignite": {"type": "bool"}, 
		"heat": {"type": "float", "min": 0.0, "max": 5000.0}, 
		"fuse": {"type": "float", "min": 0.0, "max": 600.0}, 
		"chain_fuse": {"type": "range2", "min": 0.0, "max": 60.0}, 
		"detonate_on_depleted": {"type": "bool"}, 
		"detonate_temperature": {"type": "float", "min": -1.0, "max": 100000.0}, 
		"sound": {"type": "sound"}, 
		"fuse_sound": {"type": "sound"}, 
	}}, 
	"destructible": {"requires": ["health"], "params": {
		"threshold": {"type": "float", "min": 0.0, "max": 0.99}, 
		"debris": {"type": "entity"}, 
		"debris_count": {"type": "int", "min": 0, "max": 16}, 
		"debris_speed": {"type": "float", "min": 0.0, "max": 5000.0}, 
		"effect": {"type": "enum", "values": ["sparks", "dust", "splinters", "blood", "glass", "metal", "smoke", "debris"]}, 
		"sound": {"type": "sound"}, 
	}}, 
	"debris": {"conflicts": ["firearm", "magazine", "explosive", "destructible"], "params": {
		"lifetime": {"type": "float", "min": 0.1, "max": 120.0}, 
	}}, 
	"melee": {"params": {
		"damage_multiplier": {"type": "float", "min": 0.0, "max": 20.0}, 
		"threshold_multiplier": {"type": "float", "min": 0.1, "max": 1.0}, 
	}}, 
}


static func has_schema(component_type: String) -> bool:
	return SCHEMAS.has(component_type)





static func validate_components(components: Dictionary, mod_id: String, registry: EntityRegistry, 
		reader: DataReader) -> void :
	for component_type: String in components.keys():
		var at: = "component '%s'" % component_type
		if not SCHEMAS.has(component_type):
			reader.error("%s: unsupported component type (known: %s)" % [at, ", ".join(SCHEMAS.keys())])
			continue
		var schema: Dictionary = SCHEMAS[component_type]
		var params: Dictionary = components[component_type]
		for required: String in schema.get("requires", []):
			if not components.has(required):
				reader.error("%s requires the '%s' component" % [at, required])
		for conflict: String in schema.get("conflicts", []):
			if components.has(conflict):
				reader.error("%s cannot be combined with '%s'" % [at, conflict])
		var specs: Dictionary = schema["params"]
		for name: String in specs:
			if specs[name].get("required", false) and not params.has(name):
				reader.error("%s: missing required parameter '%s' (%s)" % [at, name, describe(specs[name])])
		for name: Variant in params.keys():
			if not specs.has(name):
				reader.warn("%s: unknown parameter '%s' (ignored)" % [at, name])
				continue
			var problem: = _check(params, str(name), specs[name], mod_id, registry, reader, at)
			if not problem.is_empty():
				reader.error("%s · '%s': expected %s, got %s" % [at, name, describe(specs[name]), problem])
	var explosive: Variant = components.get("explosive")
	if explosive is Dictionary and bool(explosive.get("detonate_on_depleted", true)) and not components.has("health"):
		reader.warn("component 'explosive': detonate_on_depleted has no effect without a 'health' component")



static func describe(spec: Dictionary) -> String:
	var low: = _fmt(spec.get("min", 0))
	var high: = _fmt(spec.get("max", 0))
	match str(spec["type"]):
		"float":
			return "a number in [%s, %s]" % [low, high]
		"int":
			return "a whole number in [%s, %s]" % [low, high]
		"bool":
			return "true or false"
		"string":
			return "a string"
		"enum":
			return "one of %s" % ", ".join(spec["values"])
		"vector2":
			return "[x, y] with numbers in [%s, %s]" % [low, high]
		"range2":
			return "[min, max] with min ≤ max in [%s, %s]" % [low, high]
		"projectile":
			return "a projectile id"
		"sound":
			return "a sound id"
		"entity":
			return "an entity id"
		"kind_floats":
			return "an object mapping %s to numbers in [%s, %s]" % ["/".join(DAMAGE_KINDS), low, high]
	return "a valid value"



static func _check(params: Dictionary, name: String, spec: Dictionary, mod_id: String, 
		registry: EntityRegistry, reader: DataReader, at: String) -> String:
	var value: Variant = params[name]
	match str(spec["type"]):
		"float":
			if not _is_number(value) or float(value) < float(spec["min"]) or float(value) > float(spec["max"]):
				return _render(value)
		"int":
			if not _is_integral(value) or float(value) < float(spec["min"]) or float(value) > float(spec["max"]):
				return _render(value)
		"bool":
			if not value is bool:
				return _render(value)
		"string":
			if not value is String:
				return _render(value)
		"enum":
			if not value is String or not (spec["values"] as Array).has(value):
				return _render(value)
		"vector2", "range2":
			if not value is Array or value.size() != 2:
				return _render(value)
			for element: Variant in value:
				if not _is_number(element) or float(element) < float(spec["min"]) or float(element) > float(spec["max"]):
					return _render(value)
			if spec["type"] == "range2" and float(value[0]) > float(value[1]):
				return _render(value)
		"projectile":
			if not value is String or str(value).is_empty():
				return _render(value)
			params[name] = NamespacedId.qualify(value, mod_id)
			if not registry.has_projectile(params[name]):
				return "unknown projectile '%s'" % params[name]
		"sound":
			if not value is String:
				return _render(value)
			if not str(value).is_empty():
				params[name] = NamespacedId.qualify(value, mod_id)
				if not registry.has_sound(params[name]):
					reader.warn("%s: unknown sound '%s' (silent)" % [at, params[name]])
		"entity":
			if not value is String:
				return _render(value)
			if not str(value).is_empty():
				params[name] = NamespacedId.qualify(value, mod_id)
		"kind_floats":
			if not value is Dictionary:
				return _render(value)
			for kind: Variant in value:
				if not DAMAGE_KINDS.has(kind):
					return "unknown damage kind '%s'" % kind
				var factor: Variant = value[kind]
				if not _is_number(factor) or float(factor) < float(spec["min"]) or float(factor) > float(spec["max"]):
					return "%s: %s" % [kind, _render(factor)]
	return ""



static func _fmt(value: Variant) -> String:
	var number: = float(value)
	return str(int(number)) if floorf(number) == number and absf(number) < 1e+15 else str(number)


static func _is_number(value: Variant) -> bool:
	return (value is float and is_finite(value)) or value is int


static func _is_integral(value: Variant) -> bool:
	return value is int or (value is float and is_finite(value) and floorf(value) == value)


static func _render(value: Variant) -> String:
	var text: = JSON.stringify(value)
	return text if text.length() <= 40 else text.substr(0, 37) + "..."
