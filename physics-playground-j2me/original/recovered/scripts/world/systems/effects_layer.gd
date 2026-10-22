class_name EffectsLayer
extends Node2D











enum Shape{DOT, STREAK, PUFF, SHARD, FLAME}

const MAX_PARTICLES: = 2400
const MAX_FLASHES: = 64
const MAX_BURN_EMITTERS: = 96

const FLAME_INTERVAL: = 0.05




const PRESETS: = {
	"sparks": {"count": 10, "speed": [180.0, 520.0], "life": [0.12, 0.35], "size": [1.5, 2.5], 
		"colors": [Color(1.0, 0.85, 0.4), Color(1.0, 0.6, 0.2)], "gravity": 0.6, "drag": 2.0, "shape": Shape.STREAK, "cone": 1.4}, 
	"dust": {"count": 7, "speed": [40.0, 140.0], "life": [0.3, 0.7], "size": [3.0, 7.0], 
		"colors": [Color(0.62, 0.6, 0.56, 0.7), Color(0.45, 0.44, 0.42, 0.6)], "gravity": -0.05, "drag": 3.0, "shape": Shape.PUFF, "cone": 1.8}, 
	"splinters": {"count": 9, "speed": [90.0, 300.0], "life": [0.35, 0.8], "size": [2.0, 4.0], 
		"colors": [Color(0.72, 0.53, 0.3), Color(0.5, 0.35, 0.18)], "gravity": 1.0, "drag": 1.0, "shape": Shape.SHARD, "cone": 1.6}, 
	"blood": {"count": 8, "speed": [60.0, 220.0], "life": [0.25, 0.6], "size": [2.0, 3.5], 
		"colors": [Color(0.62, 0.05, 0.05), Color(0.42, 0.02, 0.03)], "gravity": 1.0, "drag": 1.5, "shape": Shape.DOT, "cone": 1.2}, 
	"glass": {"count": 14, "speed": [80.0, 360.0], "life": [0.4, 0.9], "size": [2.0, 4.5], 
		"colors": [Color(0.75, 0.9, 1.0, 0.85), Color(0.9, 0.97, 1.0, 0.9)], "gravity": 1.0, "drag": 0.8, "shape": Shape.SHARD, "cone": 3.2}, 
	"metal": {"count": 8, "speed": [120.0, 380.0], "life": [0.3, 0.7], "size": [2.0, 3.5], 
		"colors": [Color(0.6, 0.64, 0.68), Color(0.4, 0.43, 0.46)], "gravity": 1.0, "drag": 1.0, "shape": Shape.SHARD, "cone": 2.4}, 
	"smoke": {"count": 6, "speed": [20.0, 70.0], "life": [0.8, 1.6], "size": [8.0, 16.0], 
		"colors": [Color(0.3, 0.3, 0.32, 0.55), Color(0.2, 0.2, 0.22, 0.5)], "gravity": -0.12, "drag": 1.2, "shape": Shape.PUFF, "cone": 3.2}, 
	"debris": {"count": 10, "speed": [120.0, 420.0], "life": [0.4, 1.0], "size": [2.0, 5.0], 
		"colors": [Color(0.45, 0.42, 0.38), Color(0.3, 0.28, 0.26)], "gravity": 1.0, "drag": 0.6, "shape": Shape.SHARD, "cone": 3.2}, 
}


var _positions: = PackedVector2Array()
var _velocities: = PackedVector2Array()
var _life: = PackedFloat32Array()
var _max_life: = PackedFloat32Array()
var _sizes: = PackedFloat32Array()
var _gravity: = PackedFloat32Array()
var _drag: = PackedFloat32Array()
var _shapes: = PackedByteArray()
var _colors: = PackedColorArray()

var _alive: = 0
var _overwrite_cursor: = 0


var _flashes: Array = []
var _burn_emitters: Dictionary = {}
var _flame_timer: = 0.0
var _gravity_vector: = Vector2(0, 980)


func _ready() -> void :

	_positions.resize(MAX_PARTICLES)
	_velocities.resize(MAX_PARTICLES)
	_life.resize(MAX_PARTICLES)
	_max_life.resize(MAX_PARTICLES)
	_sizes.resize(MAX_PARTICLES)
	_gravity.resize(MAX_PARTICLES)
	_drag.resize(MAX_PARTICLES)
	_shapes.resize(MAX_PARTICLES)
	_colors.resize(MAX_PARTICLES)
	_life.fill(0.0)
	_gravity_vector = Vector2(0, float(ProjectSettings.get_setting("physics/2d/default_gravity", 980.0)))





func burst(preset_name: String, at_position: Vector2, direction: = Vector2.ZERO, scale_factor: = 1.0) -> void :
	var preset: Dictionary = PRESETS.get(preset_name, PRESETS["dust"])
	var count: = roundi(float(preset["count"]) * clampf(scale_factor, 0.3, 4.0))
	var speed_range: Array = preset["speed"]
	var life_range: Array = preset["life"]
	var size_range: Array = preset["size"]
	var colors: Array = preset["colors"]
	var cone: float = preset["cone"]
	var base_angle: = direction.angle() if direction != Vector2.ZERO else 0.0
	var spread: = cone if direction != Vector2.ZERO else TAU
	for index in count:
		var angle: = base_angle + randf_range( - spread * 0.5, spread * 0.5)
		var speed: = randf_range(speed_range[0], speed_range[1]) * sqrt(scale_factor)
		_emit(at_position, Vector2.from_angle(angle) * speed, randf_range(life_range[0], life_range[1]), 
			randf_range(size_range[0], size_range[1]) * sqrt(scale_factor), preset["gravity"], preset["drag"], 
			preset["shape"], colors[randi() % colors.size()])


func muzzle_flash(at_position: Vector2, direction: Vector2, size: float) -> void :
	_add_flash(at_position, direction, size, 0.05, 0)
	burst("smoke", at_position + direction * size * 0.5, direction, 0.4)


func explosion(at_position: Vector2, radius: float) -> void :
	var scale_factor: = clampf(radius / 200.0, 0.4, 3.0)
	_add_flash(at_position, Vector2.ZERO, radius * 0.55, 0.12, 1)
	_add_flash(at_position, Vector2.ZERO, radius, 0.35, 2)
	for index in roundi(26 * scale_factor):
		var angle: = randf() * TAU
		_emit(at_position, Vector2.from_angle(angle) * randf_range(150.0, 700.0) * scale_factor, 
			randf_range(0.2, 0.5), randf_range(4.0, 9.0) * scale_factor, -0.2, 3.0, Shape.FLAME, 
			Color(1.0, randf_range(0.45, 0.8), 0.15, 0.9))
	burst("smoke", at_position, Vector2.ZERO, 2.5 * scale_factor)
	burst("sparks", at_position, Vector2.ZERO, 2.0 * scale_factor)



func set_burning(entity: SandboxEntity, burning: bool) -> void :
	var key: = entity.get_instance_id()
	if burning and _burn_emitters.size() < MAX_BURN_EMITTERS:
		_burn_emitters[key] = entity
	else:
		_burn_emitters.erase(key)


func get_particle_count() -> int:
	return _alive


func get_flash_count() -> int:
	return _flashes.size()


func get_burning_emitter_count() -> int:
	return _burn_emitters.size()


func clear() -> void :
	_life.fill(0.0)
	_alive = 0
	_flashes.clear()
	_burn_emitters.clear()
	queue_redraw()




func _process(delta: float) -> void :
	if _alive == 0 and _flashes.is_empty() and _burn_emitters.is_empty():
		return
	_flame_timer += delta
	if _flame_timer >= FLAME_INTERVAL:
		_flame_timer = 0.0
		_emit_flames()
	var index: = _alive - 1
	while index >= 0:
		var life: = _life[index] - delta
		if life <= 0.0:
			_alive -= 1
			_move_particle(_alive, index)
			_life[_alive] = 0.0
		else:
			_life[index] = life
			var velocity: = _velocities[index]
			velocity += _gravity_vector * _gravity[index] * delta
			velocity *= maxf(1.0 - _drag[index] * delta, 0.0)
			_velocities[index] = velocity
			_positions[index] += velocity * delta
		index -= 1
	for flash_index in range(_flashes.size() - 1, -1, -1):
		var flash: Array = _flashes[flash_index]
		flash[3] -= delta
		if flash[3] <= 0.0:
			_flashes.remove_at(flash_index)
	queue_redraw()


func _emit_flames() -> void :
	for key: int in _burn_emitters.keys():
		var candidate: Variant = _burn_emitters[key]
		if not is_instance_valid(candidate) or not (candidate as SandboxEntity).is_inside_tree():
			_burn_emitters.erase(key)
			continue
		var entity: = candidate as SandboxEntity
		var extent: = entity.definition.get_bounding_size() * 0.5
		for index in 2:
			var offset: = Vector2(randf_range( - extent.x, extent.x), randf_range( - extent.y, extent.y) * 0.6).rotated(entity.global_rotation)
			_emit(entity.global_position + offset, Vector2(randf_range(-20, 20), randf_range(-140, -60)), 
				randf_range(0.25, 0.5), randf_range(3.0, 6.0) * clampf(extent.x / 20.0, 0.7, 2.0), -0.15, 1.5, Shape.FLAME, 
				Color(1.0, randf_range(0.35, 0.75), 0.1, 0.85))
		if randf() < 0.2:
			burst("smoke", entity.global_position + Vector2(0, - extent.y), Vector2.UP, 0.3)


func _emit(at_position: Vector2, velocity: Vector2, life: float, size: float, gravity_factor: float, 
		drag: float, shape: int, color: Color) -> void :
	var slot: = _alive
	if _alive < MAX_PARTICLES:
		_alive += 1
	else:
		slot = _overwrite_cursor
		_overwrite_cursor = (_overwrite_cursor + 1) % MAX_PARTICLES
	_positions[slot] = at_position
	_velocities[slot] = velocity
	_life[slot] = life
	_max_life[slot] = life
	_sizes[slot] = size
	_gravity[slot] = gravity_factor
	_drag[slot] = drag
	_shapes[slot] = shape
	_colors[slot] = color



func _move_particle(from: int, to: int) -> void :
	if from == to:
		return
	_positions[to] = _positions[from]
	_velocities[to] = _velocities[from]
	_life[to] = _life[from]
	_max_life[to] = _max_life[from]
	_sizes[to] = _sizes[from]
	_gravity[to] = _gravity[from]
	_drag[to] = _drag[from]
	_shapes[to] = _shapes[from]
	_colors[to] = _colors[from]


func _add_flash(at_position: Vector2, direction: Vector2, radius: float, life: float, kind: int) -> void :
	if _flashes.size() >= MAX_FLASHES:
		_flashes.pop_front()
	_flashes.append([at_position, direction, radius, life, life, kind])


func _draw() -> void :
	for index in _alive:
		var life: = _life[index]
		var t: = life / _max_life[index]
		var color: = _colors[index]
		var point: = _positions[index]
		var size: = _sizes[index]
		match _shapes[index]:
			Shape.STREAK:
				color.a *= t
				draw_line(point, point - _velocities[index] * 0.02, color, size)
			Shape.PUFF:
				color.a *= t
				draw_circle(point, size * (1.6 - t * 0.6), color)
			Shape.SHARD:
				color.a *= minf(t * 2.0, 1.0)
				draw_rect(Rect2(point - Vector2.ONE * size * 0.5, Vector2.ONE * size), color)
			Shape.FLAME:
				color.a *= t
				color.g *= 0.6 + t * 0.4
				draw_circle(point, size * (0.5 + t * 0.5), color)
			_:
				color.a *= minf(t * 2.0, 1.0)
				draw_circle(point, size * 0.6, color)
	for flash: Array in _flashes:
		var point: Vector2 = flash[0]
		var direction: Vector2 = flash[1]
		var radius: float = flash[2]
		var t: float = flash[3] / flash[4]
		match int(flash[5]):
			0:
				var side: = direction.orthogonal() * radius * 0.35
				var tip: = point + direction * radius
				draw_colored_polygon(PackedVector2Array([point + side, tip, point - side]), Color(1.0, 0.85, 0.4, 0.9 * t))
				draw_circle(point, radius * 0.3, Color(1.0, 0.95, 0.7, t))
			1:
				draw_circle(point, radius * (1.2 - t * 0.2), Color(1.0, 0.8, 0.4, 0.85 * t))
			2:
				draw_arc(point, radius * (1.0 - t * 0.7), 0.0, TAU, 48, Color(1.0, 0.9, 0.7, 0.5 * t), 4.0 * t + 1.0, true)
