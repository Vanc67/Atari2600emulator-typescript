class_name EntityVisual
extends Node2D










const STYLES: PackedStringArray = ["plain", "crate", "plated", "barrel", "concrete", "ball", "sphere", "plank", "limb", "head", 
	"pistol", "revolver", "rifle", "shotgun", "smg", "bat", "hammer", "crowbar", "pipe", "hazard_barrel", "bomb", 
	"large_bomb", "glass"]
const SELECTION_COLOR: = Color(0.89, 0.64, 0.23)
const FROZEN_FILL: = Color(0.45, 0.75, 1.0, 0.25)
const FROZEN_OUTLINE: = Color(0.62, 0.86, 1.0, 0.95)
const OUTLINE_WIDTH: = 2.0

const DAMAGE_TINT_MAX: = 0.6

const DAMAGE_MARKS_LIGHT: = 0.4
const DAMAGE_MARKS_HEAVY: = 0.75
const WOUND_COLOR: = Color(0.45, 0.06, 0.06)
const LIFELESS_TINT: = Color(0.55, 0.6, 0.62, 0.35)
const BURNING_TINT: = Color(1.0, 0.45, 0.1, 0.35)
const HEAT_TINT: = Color(1.0, 0.3, 0.1)
const CHAR_COLOR: = Color(0.08, 0.06, 0.05)
const ARMED_LIGHT: = Color(1.0, 0.15, 0.1)
const CAPSULE_SEGMENTS: = 8

enum Vitality{ALIVE, INCAPACITATED, DEAD}

var definition: EntityDefinition
var seed_value: = 1

var is_selected: = false:
	set(value):
		if is_selected != value:
			is_selected = value
			queue_redraw()

var is_frozen: = false:
	set(value):
		if is_frozen != value:
			is_frozen = value
			queue_redraw()


var damage_level: = 0.0:
	set(value):
		value = clampf(value, 0.0, 1.0)
		if absf(value - damage_level) >= 0.01 or (value != damage_level and (value == 0.0 or value == 1.0)):
			damage_level = value
			queue_redraw()

var vitality: int = Vitality.ALIVE:
	set(value):
		if vitality != value:
			vitality = value
			queue_redraw()

var burning: = false:
	set(value):
		if burning != value:
			burning = value
			queue_redraw()


var char_level: = 0.0:
	set(value):
		value = clampf(value, 0.0, 1.0)
		if absf(value - char_level) >= 0.02 or (value != char_level and value == 0.0):
			char_level = value
			queue_redraw()


var heat_glow: = 0.0:
	set(value):
		value = clampf(value, 0.0, 1.0)
		if absf(value - heat_glow) >= 0.03 or (value != heat_glow and value == 0.0):
			heat_glow = value
			queue_redraw()


var armed: = false:
	set(value):
		if armed != value:
			armed = value
			queue_redraw()

var blink: = false:
	set(value):
		if blink != value:
			blink = value
			queue_redraw()


var wound_points: = PackedVector2Array()


static func is_known_style(style: String) -> bool:
	return STYLES.has(style)


func configure(entity_definition: EntityDefinition, visual_seed: int) -> void :
	definition = entity_definition
	seed_value = visual_seed
	queue_redraw()


func add_wound(local_position: Vector2) -> void :
	for existing in wound_points:
		if existing.distance_to(local_position) < 1.0:
			return
	wound_points.append(local_position)
	queue_redraw()


func _draw() -> void :
	if definition == null:
		return
	draw_definition(self, definition, seed_value, vitality)
	if damage_level > 0.0:
		draw_damage(self, definition, damage_level, seed_value)
	if vitality == Vitality.DEAD:
		draw_shape_fill(self, definition, LIFELESS_TINT, 0.0)
	if char_level > 0.0:
		draw_shape_fill(self, definition, Color(CHAR_COLOR, char_level * 0.75), 0.0)
	if heat_glow > 0.0:
		draw_shape_fill(self, definition, Color(HEAT_TINT, heat_glow * 0.5), 0.0)
	if burning:
		draw_shape_fill(self, definition, BURNING_TINT, 0.0)
	if armed:
		var light: = ARMED_LIGHT if blink else ARMED_LIGHT.darkened(0.6)
		draw_circle(Vector2(0, - definition.get_bounding_size().y * 0.25), 3.5, light, true, -1.0, true)
	for wound in wound_points:
		var wound_radius: = minf(definition.get_bounding_size().x, definition.get_bounding_size().y) * 0.32
		draw_circle(wound, wound_radius, WOUND_COLOR, true, -1.0, true)
	if is_frozen:
		draw_shape_fill(self, definition, FROZEN_FILL, 0.0)
		draw_shape_outline(self, definition, FROZEN_OUTLINE, OUTLINE_WIDTH, 0.0)
	if is_selected:
		draw_shape_outline(self, definition, SELECTION_COLOR, OUTLINE_WIDTH, 4.0)





static func draw_definition(canvas: CanvasItem, def: EntityDefinition, visual_seed: int, 
		visual_vitality: int = Vitality.ALIVE) -> void :
	match def.visual_style:
		"crate":
			_draw_crate(canvas, def)
		"plated":
			_draw_plated(canvas, def)
		"barrel":
			_draw_barrel(canvas, def)
		"concrete":
			_draw_concrete(canvas, def, visual_seed)
		"ball":
			_draw_ball(canvas, def)
		"sphere":
			_draw_sphere(canvas, def)
		"plank":
			_draw_plank(canvas, def)
		"limb":
			_draw_limb(canvas, def)
		"head":
			_draw_head(canvas, def, visual_vitality)
		"pistol", "revolver", "rifle", "shotgun", "smg":
			WeaponArt.draw_firearm(canvas, def)
		"bat", "hammer", "crowbar", "pipe":
			WeaponArt.draw_melee(canvas, def)
		"hazard_barrel":
			_draw_barrel(canvas, def)
			WeaponArt.draw_hazard_marks(canvas, def)
		"bomb", "large_bomb":
			WeaponArt.draw_bomb(canvas, def)
		"glass":
			WeaponArt.draw_glass(canvas, def)
		_:
			_draw_plain(canvas, def)



static func draw_assembly(canvas: CanvasItem, def: EntityDefinition, origin: Vector2, scale_factor: float) -> void :
	var parts: = def.assembly.parts.duplicate()
	parts.sort_custom( func(a: AssemblyDefinition.PartDefinition, b: AssemblyDefinition.PartDefinition) -> bool:
		return a.z_index < b.z_index)
	for part: AssemblyDefinition.PartDefinition in parts:
		if part.definition == null:
			continue
		canvas.draw_set_transform(origin + part.position * scale_factor, part.rotation, Vector2.ONE * scale_factor)
		draw_definition(canvas, part.definition, 1)
	canvas.draw_set_transform_matrix(Transform2D.IDENTITY)


static func draw_shape_fill(canvas: CanvasItem, def: EntityDefinition, color: Color, grow: float) -> void :
	if def.is_circle():
		canvas.draw_circle(Vector2.ZERO, def.radius + grow, color, true, -1.0, true)
	elif def.is_capsule():
		canvas.draw_colored_polygon(_capsule_points(def.radius + grow, def.size.y * 0.5 - def.radius), color)
	else:
		canvas.draw_rect(_body_rect(def).grow(grow), color)


static func draw_shape_outline(canvas: CanvasItem, def: EntityDefinition, color: Color, 
		width: float, grow: float) -> void :
	if def.is_circle():
		canvas.draw_arc(Vector2.ZERO, def.radius + grow, 0.0, TAU, 48, color, width, true)
	elif def.is_capsule():
		var points: = _capsule_points(def.radius + grow, def.size.y * 0.5 - def.radius)
		points.append(points[0])
		canvas.draw_polyline(points, color, width, true)
	else:
		canvas.draw_rect(_body_rect(def).grow(grow), color, false, width)




static func draw_damage(canvas: CanvasItem, def: EntityDefinition, level: float, visual_seed: int) -> void :
	draw_shape_fill(canvas, def, Color(def.damage_color, level * DAMAGE_TINT_MAX), -1.0)
	if level < DAMAGE_MARKS_LIGHT:
		return
	var mark_count: = 2 if level < DAMAGE_MARKS_HEAVY else 5
	var extent: = def.get_bounding_size() * 0.5
	if def.is_circle() or def.is_capsule():
		extent = Vector2(extent.x * 0.6, maxf(extent.y - def.radius * 0.4, extent.x * 0.6))
	else:
		extent *= 0.8
	var rng: = RandomNumberGenerator.new()
	rng.seed = hash(visual_seed * 7919)
	var mark_color: = def.damage_color.darkened(0.4)
	var width: = maxf(minf(extent.x, extent.y) * 0.12, 1.2)
	for index in mark_count:
		var start: = Vector2(rng.randf_range( - extent.x, extent.x), rng.randf_range( - extent.y, extent.y))
		var bend: = start + Vector2(rng.randf_range(-1, 1), rng.randf_range(-1, 1)) * minf(extent.x, extent.y) * 0.6
		var finish: = bend + Vector2(rng.randf_range(-1, 1), rng.randf_range(-1, 1)) * minf(extent.x, extent.y) * 0.6
		bend = bend.clamp( - extent, extent)
		finish = finish.clamp( - extent, extent)
		canvas.draw_polyline(PackedVector2Array([start, bend, finish]), mark_color, width, true)


static func _body_rect(def: EntityDefinition) -> Rect2:
	return Rect2( - def.size * 0.5, def.size)



static func _capsule_points(cap_radius: float, half_straight: float) -> PackedVector2Array:
	var points: = PackedVector2Array()
	half_straight = maxf(half_straight, 0.0)
	for step in CAPSULE_SEGMENTS + 1:
		var angle: = PI + PI * float(step) / float(CAPSULE_SEGMENTS)
		points.append(Vector2(cos(angle), sin(angle)) * cap_radius + Vector2(0, - half_straight))
	for step in CAPSULE_SEGMENTS + 1:
		var angle: = PI * float(step) / float(CAPSULE_SEGMENTS)
		points.append(Vector2(cos(angle), sin(angle)) * cap_radius + Vector2(0, half_straight))
	return points


static func _draw_plain(canvas: CanvasItem, def: EntityDefinition) -> void :
	draw_shape_fill(canvas, def, def.fill_color, 0.0)
	draw_shape_outline(canvas, def, def.outline_color, OUTLINE_WIDTH, -1.0)


static func _draw_limb(canvas: CanvasItem, def: EntityDefinition) -> void :
	draw_shape_fill(canvas, def, def.fill_color, 0.0)
	if def.is_capsule():

		var highlight: = _capsule_points(def.radius * 0.35, def.size.y * 0.5 - def.radius)
		var offset: = Vector2( - def.radius * 0.35, 0)
		for index in highlight.size():
			highlight[index] += offset
		canvas.draw_colored_polygon(highlight, Color(def.detail_color, 0.5))
	draw_shape_outline(canvas, def, def.outline_color, 1.5, -0.5)


static func _draw_head(canvas: CanvasItem, def: EntityDefinition, visual_vitality: int) -> void :
	var r: = def.radius
	canvas.draw_circle(Vector2.ZERO, r, def.fill_color, true, -1.0, true)
	canvas.draw_arc(Vector2.ZERO, r - 0.75, 0.0, TAU, 32, def.outline_color, 1.5, true)

	var eye: = Vector2(r * 0.42, - r * 0.18)
	var eye_size: = r * 0.16
	match visual_vitality:
		Vitality.DEAD:
			canvas.draw_line(eye + Vector2( - eye_size, - eye_size), eye + Vector2(eye_size, eye_size), def.outline_color, 1.5, true)
			canvas.draw_line(eye + Vector2( - eye_size, eye_size), eye + Vector2(eye_size, - eye_size), def.outline_color, 1.5, true)
		Vitality.INCAPACITATED:
			canvas.draw_line(eye + Vector2( - eye_size, 0), eye + Vector2(eye_size, 0), def.outline_color, 1.5, true)
		_:
			canvas.draw_circle(eye, eye_size, def.outline_color, true, -1.0, true)
	canvas.draw_line(Vector2(r * 0.2, r * 0.42), Vector2(r * 0.55, r * 0.38), def.detail_color, 1.2, true)


static func _draw_crate(canvas: CanvasItem, def: EntityDefinition) -> void :
	var rect: = _body_rect(def)
	var inset: = minf(rect.size.x, rect.size.y) * 0.13
	var inner: = rect.grow( - inset)
	canvas.draw_rect(rect, def.fill_color)
	var plank_count: = 3
	for index in range(1, plank_count):
		var y: = inner.position.y + inner.size.y * float(index) / float(plank_count)
		canvas.draw_line(Vector2(inner.position.x, y), Vector2(inner.end.x, y), def.detail_color, 1.5)
	canvas.draw_line(inner.position, inner.end, def.detail_color, inset * 0.7)
	canvas.draw_rect(inner, def.outline_color, false, 1.5)
	canvas.draw_rect(rect.grow(-1.0), def.outline_color, false, OUTLINE_WIDTH)


static func _draw_plated(canvas: CanvasItem, def: EntityDefinition) -> void :
	var rect: = _body_rect(def)
	var inset: = minf(rect.size.x, rect.size.y) * 0.16
	canvas.draw_rect(rect, def.fill_color)

	var light: = def.fill_color.lightened(0.25)
	var dark: = def.fill_color.darkened(0.3)
	canvas.draw_line(rect.position, Vector2(rect.end.x, rect.position.y), light, 3.0)
	canvas.draw_line(rect.position, Vector2(rect.position.x, rect.end.y), light, 3.0)
	canvas.draw_line(Vector2(rect.position.x, rect.end.y), rect.end, dark, 3.0)
	canvas.draw_line(Vector2(rect.end.x, rect.position.y), rect.end, dark, 3.0)
	canvas.draw_rect(rect.grow( - inset), def.detail_color, false, 1.5)
	var rivet_offset: = inset * 0.5
	var rivet_radius: = maxf(inset * 0.16, 1.5)
	for corner in [Vector2(-1, -1), Vector2(1, -1), Vector2(1, 1), Vector2(-1, 1)]:
		var rivet_position: Vector2 = (rect.size * 0.5 - Vector2.ONE * rivet_offset) * corner
		canvas.draw_circle(rivet_position, rivet_radius, def.detail_color, true, -1.0, true)
	canvas.draw_rect(rect.grow(-1.0), def.outline_color, false, OUTLINE_WIDTH)


static func _draw_barrel(canvas: CanvasItem, def: EntityDefinition) -> void :
	var rect: = _body_rect(def)
	var corner_radius: = minf(rect.size.x, rect.size.y) * 0.2
	var points: = _rounded_rect_points(rect, corner_radius, 5)
	canvas.draw_colored_polygon(points, def.fill_color)
	var highlight: = Rect2(rect.position.x + rect.size.x * 0.16, rect.position.y + corner_radius * 0.6, 
		rect.size.x * 0.12, rect.size.y - corner_radius * 1.2)
	canvas.draw_rect(highlight, def.fill_color.lightened(0.18))
	for fraction in [0.22, 0.78]:
		var y: float = rect.position.y + rect.size.y * fraction
		canvas.draw_line(Vector2(rect.position.x + 1.0, y), Vector2(rect.end.x - 1.0, y), def.detail_color, 3.0)
	points.append(points[0])
	canvas.draw_polyline(points, def.outline_color, OUTLINE_WIDTH, true)


static func _draw_concrete(canvas: CanvasItem, def: EntityDefinition, visual_seed: int) -> void :
	var rect: = _body_rect(def)
	canvas.draw_rect(rect, def.fill_color)
	var rng: = RandomNumberGenerator.new()
	rng.seed = hash(visual_seed)
	var speckle_count: = int(rect.get_area() / 300.0)
	for index in speckle_count:
		var speckle_size: = rng.randf_range(1.5, 3.5)
		var speckle_position: = Vector2(
			rng.randf_range(rect.position.x + 3.0, rect.end.x - 3.0 - speckle_size), 
			rng.randf_range(rect.position.y + 3.0, rect.end.y - 3.0 - speckle_size))
		canvas.draw_rect(Rect2(speckle_position, Vector2.ONE * speckle_size), def.detail_color)
	canvas.draw_line(Vector2(rect.position.x, rect.end.y - 2.0), Vector2(rect.end.x, rect.end.y - 2.0), 
		def.fill_color.darkened(0.15), 4.0)
	canvas.draw_rect(rect.grow(-1.0), def.outline_color, false, OUTLINE_WIDTH)


static func _draw_ball(canvas: CanvasItem, def: EntityDefinition) -> void :
	var r: = def.radius
	canvas.draw_circle(Vector2.ZERO, r, def.fill_color, true, -1.0, true)

	canvas.draw_line(Vector2( - r * 0.9, 0.0), Vector2(r * 0.9, 0.0), def.detail_color, r * 0.3)
	canvas.draw_arc(Vector2.ZERO, r - 1.0, 0.0, TAU, 40, def.outline_color, OUTLINE_WIDTH, true)


static func _draw_sphere(canvas: CanvasItem, def: EntityDefinition) -> void :
	var r: = def.radius
	canvas.draw_circle(Vector2.ZERO, r, def.fill_color, true, -1.0, true)
	canvas.draw_circle(Vector2( - r * 0.3, - r * 0.3), r * 0.38, Color(def.detail_color, 0.55), true, -1.0, true)
	canvas.draw_line(Vector2(0.0, r * 0.45), Vector2(0.0, r * 0.85), def.outline_color, 2.0, true)
	canvas.draw_arc(Vector2.ZERO, r - 1.0, 0.0, TAU, 40, def.outline_color, OUTLINE_WIDTH, true)


static func _draw_plank(canvas: CanvasItem, def: EntityDefinition) -> void :
	var rect: = _body_rect(def)
	canvas.draw_rect(rect, def.fill_color)
	var along_x: = rect.size.x >= rect.size.y
	for fraction in [0.33, 0.66]:
		if along_x:
			var y: float = rect.position.y + rect.size.y * fraction
			canvas.draw_line(Vector2(rect.position.x + 8.0, y), Vector2(rect.end.x - 8.0, y), def.detail_color, 1.0)
		else:
			var x: float = rect.position.x + rect.size.x * fraction
			canvas.draw_line(Vector2(x, rect.position.y + 8.0), Vector2(x, rect.end.y - 8.0), def.detail_color, 1.0)
	canvas.draw_rect(rect.grow(-1.0), def.outline_color, false, OUTLINE_WIDTH)


static func _rounded_rect_points(rect: Rect2, corner_radius: float, segments: int) -> PackedVector2Array:
	var points: = PackedVector2Array()
	var corners: = [
		[Vector2(rect.end.x - corner_radius, rect.position.y + corner_radius), - PI * 0.5], 
		[Vector2(rect.end.x - corner_radius, rect.end.y - corner_radius), 0.0], 
		[Vector2(rect.position.x + corner_radius, rect.end.y - corner_radius), PI * 0.5], 
		[Vector2(rect.position.x + corner_radius, rect.position.y + corner_radius), PI], 
	]
	for corner: Array in corners:
		var center: Vector2 = corner[0]
		var start_angle: float = corner[1]
		for step in segments + 1:
			var angle: = start_angle + (PI * 0.5) * float(step) / float(segments)
			points.append(center + Vector2(cos(angle), sin(angle)) * corner_radius)
	return points
