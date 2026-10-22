class_name ThermalComponent
extends EntityComponent



















signal ignited
signal extinguished
signal temperature_changed(temperature: float)

const AMBIENT_TEMPERATURE: = 20.0

const REST_THRESHOLD: = 1.0

const FULL_CHAR_TIME: = 20.0

var temperature: = AMBIENT_TEMPERATURE

var rest_temperature: = AMBIENT_TEMPERATURE
var conductivity: = 0.0

var ignition_temperature: = -1.0
var flammability: = 0.0
var heat_resistance: = 0.0
var burn_damage: = 8.0
var burn_duration: = 10.0
var burn_temperature: = 700.0
var heat_radius: = 70.0
var heat_output: = 160.0
var cooling: = 0.08

var burning: = false
var fuel_remaining: = 0.0

var char_level: = 0.0


func get_component_type() -> StringName:
	return &"thermal"


func configure(params: Dictionary, _definition: EntityDefinition, material: MaterialDefinition) -> void :
	temperature = float(params.get("initial_temperature", AMBIENT_TEMPERATURE))
	rest_temperature = temperature
	conductivity = material.thermal_conductivity if material else 0.0
	var material_ignition: = material.ignition_temperature if material else -1.0
	var material_flammability: = material.flammability if material else 0.0
	ignition_temperature = float(params.get("ignition_temperature", material_ignition))
	flammability = clampf(float(params.get("flammability", material_flammability)), 0.0, 1.0)
	heat_resistance = clampf(float(params.get("heat_resistance", 0.0)), 0.0, 1.0)
	burn_damage = maxf(float(params.get("burn_damage", burn_damage)), 0.0)
	burn_duration = float(params.get("burn_duration", burn_duration))
	burn_temperature = float(params.get("burn_temperature", burn_temperature))
	heat_radius = maxf(float(params.get("heat_radius", heat_radius)), 0.0)
	heat_output = maxf(float(params.get("heat_output", heat_output)), 0.0)
	cooling = clampf(float(params.get("cooling", 0.08 * (1.0 + conductivity))), 0.0, 10.0)


func _ready() -> void :

	if burning or absf(temperature - rest_temperature) > REST_THRESHOLD:
		_wake()


func can_ignite() -> bool:
	return ignition_temperature >= 0.0 and flammability > 0.0



func add_heat(amount: float) -> void :
	if amount <= 0.0:
		return
	temperature += amount * (1.0 - heat_resistance)
	_wake()
	if not burning and can_ignite() and temperature >= ignition_temperature and randf() < flammability:
		ignite()
	temperature_changed.emit(temperature)



func try_ignite(chance_scale: = 1.0) -> bool:
	if burning:
		return true
	if can_ignite() and randf() < flammability * chance_scale:
		ignite()
	return burning



func ignite() -> bool:
	if burning or not can_ignite():
		return burning
	burning = true
	fuel_remaining = burn_duration
	temperature = maxf(temperature, burn_temperature)
	_wake()
	_update_visuals()
	if entity and entity.world:
		entity.world.effects.set_burning(entity, true)
	ignited.emit()
	temperature_changed.emit(temperature)
	return true


func extinguish() -> void :
	var was_burning: = burning
	burning = false
	temperature = minf(temperature, maxf(rest_temperature + 30.0, ignition_temperature * 0.5))
	if entity and entity.world:
		entity.world.effects.set_burning(entity, false)
	_update_visuals()
	if was_burning:
		extinguished.emit()
	temperature_changed.emit(temperature)




func thermal_tick(delta: float) -> bool:
	if burning:
		temperature = maxf(temperature, burn_temperature)
		char_level = minf(char_level + delta / FULL_CHAR_TIME, 1.0)
		if burn_duration >= 0.0:
			fuel_remaining -= delta
			if fuel_remaining <= 0.0:
				extinguish()
		var health: = entity.get_health() if entity else null
		if health and burn_damage > 0.0:
			var damage: = burn_damage * delta * (1.0 - heat_resistance)
			health.apply_damage(damage, DamageInfo.fire(damage))
	else:
		temperature += (rest_temperature - temperature) * minf(cooling * delta, 1.0)
	_update_visuals()
	temperature_changed.emit(temperature)
	return burning or absf(temperature - rest_temperature) > REST_THRESHOLD


func serialize_state() -> Dictionary:
	var state: = {}
	if absf(temperature - rest_temperature) > REST_THRESHOLD:
		state["temperature"] = temperature
	if burning:
		state["burning"] = true
		state["fuel"] = fuel_remaining
	if char_level > 0.0:
		state["char"] = char_level
	return state


func deserialize_state(data: Dictionary) -> void :
	temperature = float(data.get("temperature", temperature))
	char_level = clampf(float(data.get("char", char_level)), 0.0, 1.0)
	if bool(data.get("burning", false)) and can_ignite():
		ignite()
		fuel_remaining = float(data.get("fuel", burn_duration))
	_wake()
	_update_visuals()


func get_inspector_lines() -> PackedStringArray:
	var line: = "Temperature: %.0f °C" % temperature
	if burning:
		line += "  (burning)"
	elif can_ignite():
		line += "  (ignites at %.0f °C)" % ignition_temperature
	return PackedStringArray([line])


func _wake() -> void :
	if entity and entity.world and entity.is_inside_tree():
		entity.world.thermal.register(self)


func _update_visuals() -> void :
	if entity == null or entity.visual == null:
		return
	entity.visual.burning = burning
	entity.visual.char_level = char_level

	entity.visual.heat_glow = 0.0 if burning else clampf((temperature - 120.0) / 580.0, 0.0, 1.0)
