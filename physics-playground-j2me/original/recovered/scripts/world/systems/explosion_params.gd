class_name ExplosionParams
extends RefCounted




var radius: = 250.0

var force: = 800.0

var damage: = 120.0

var falloff: = 1.5

var ignite: = false

var heat: = 0.0
var sound: = "core:explosion"


static func from_dict(data: Dictionary) -> ExplosionParams:
	var params: = ExplosionParams.new()
	params.radius = clampf(float(data.get("radius", params.radius)), 10.0, 3000.0)
	params.force = clampf(float(data.get("force", params.force)), 0.0, 100000.0)
	params.damage = clampf(float(data.get("damage", params.damage)), 0.0, 100000.0)
	params.falloff = clampf(float(data.get("falloff", params.falloff)), 0.25, 4.0)
	params.ignite = bool(data.get("ignite", params.ignite))
	params.heat = clampf(float(data.get("heat", 400.0 if params.ignite else 0.0)), 0.0, 5000.0)
	params.sound = str(data.get("sound", params.sound))
	return params
