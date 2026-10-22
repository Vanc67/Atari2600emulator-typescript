class_name WeaponArt
extends RefCounted








static func draw_firearm(canvas: CanvasItem, def: EntityDefinition) -> void :
	var w: = def.size.x
	var h: = def.size.y
	var body: = def.fill_color
	var dark: = def.outline_color
	var metal: = def.detail_color
	match def.visual_style:
		"pistol":
			_poly(canvas, [[-0.5, -0.5], [0.5, -0.5], [0.5, -0.05], [-0.05, -0.05], [-0.18, 0.5], [-0.46, 0.5], [-0.36, -0.05], [-0.5, -0.05]], w, h, body, dark)
			_bar(canvas, -0.45, 0.48, -0.42, -0.3, w, h, metal)
			_bar(canvas, -0.08, 0.06, -0.05, 0.12, w, h, dark)
		"revolver":
			_poly(canvas, [[-0.5, -0.32], [-0.18, -0.32], [-0.18, -0.5], [0.15, -0.5], [0.15, -0.22], [0.5, -0.22], [0.5, 0.02], [-0.05, 0.02], 
				[-0.2, 0.5], [-0.46, 0.5], [-0.38, 0.0], [-0.5, 0.0]], w, h, body, dark)
			canvas.draw_circle(Vector2(-0.01 * w, -0.24 * h), h * 0.27, metal, true, -1.0, true)
			canvas.draw_arc(Vector2(-0.01 * w, -0.24 * h), h * 0.27, 0.0, TAU, 16, dark, 1.2, true)
		"rifle":
			_poly(canvas, [[-0.5, -0.3], [-0.22, -0.32], [-0.22, -0.18], [0.5, -0.18], [0.5, 0.02], [0.12, 0.02], [0.12, 0.1], [-0.05, 0.1], 
				[-0.1, 0.5], [-0.2, 0.5], [-0.2, 0.12], [-0.5, 0.38]], w, h, body, dark)
			_poly(canvas, [[0.0, 0.08], [0.1, 0.08], [0.06, 0.5], [-0.03, 0.5]], w, h, metal, dark)
			_bar(canvas, -0.15, 0.2, -0.45, -0.3, w, h, metal)
		"shotgun":
			_poly(canvas, [[-0.5, -0.1], [-0.25, -0.3], [0.5, -0.3], [0.5, -0.02], [-0.18, -0.02], [-0.22, 0.12], [-0.5, 0.45]], w, h, body, dark)
			_bar(canvas, 0.05, 0.35, 0.02, 0.28, w, h, metal)
			_bar(canvas, -0.15, 0.5, -0.3, -0.18, w, h, dark)
		"smg":
			_poly(canvas, [[-0.5, -0.2], [-0.32, -0.32], [0.5, -0.32], [0.5, -0.02], [0.08, -0.02], [0.02, 0.2], [-0.12, 0.2], [-0.16, -0.02], 
				[-0.32, -0.02], [-0.5, 0.15]], w, h, body, dark)
			_poly(canvas, [[0.12, -0.02], [0.24, -0.02], [0.22, 0.5], [0.12, 0.5]], w, h, metal, dark)


static func draw_melee(canvas: CanvasItem, def: EntityDefinition) -> void :
	var w: = def.size.x
	var h: = def.size.y
	match def.visual_style:
		"bat":
			_poly(canvas, [[-0.5, -0.18], [-0.1, -0.24], [0.42, -0.5], [0.5, -0.35], [0.5, 0.35], [0.42, 0.5], [-0.1, 0.24], [-0.5, 0.18]], 
				w, h, def.fill_color, def.outline_color)
			_bar(canvas, -0.5, -0.38, -0.3, 0.3, w, h, Color(0.15, 0.15, 0.17))
		"hammer":
			_bar(canvas, -0.5, 0.25, -0.18, 0.18, w, h, def.fill_color)
			_poly(canvas, [[0.18, -0.5], [0.5, -0.5], [0.5, 0.5], [0.18, 0.5]], w, h, def.detail_color, def.outline_color)
			_bar(canvas, -0.5, 0.18, -0.18, 0.18, w, h, Color(0, 0, 0, 0))
			canvas.draw_rect(Rect2(Vector2(-0.5 * w, -0.18 * h), Vector2(0.68 * w, 0.36 * h)), def.fill_color.darkened(0.35), false, 1.0)
		"crowbar":
			_bar(canvas, -0.5, 0.4, -0.5, 0.5, w, h, def.fill_color)
			_poly(canvas, [[0.4, -0.5], [0.5, -1.4], [0.56, -1.3], [0.48, 0.5], [0.4, 0.5]], w, h, def.fill_color, def.outline_color)
			_bar(canvas, -0.5, -0.44, -0.5, 0.5, w, h, def.detail_color)
		"pipe":
			_bar(canvas, -0.5, 0.5, -0.5, 0.5, w, h, def.fill_color)
			_bar(canvas, -0.5, 0.5, -0.5, -0.15, w, h, def.detail_color)
			_bar(canvas, -0.5, -0.44, -0.5, 0.5, w, h, def.outline_color)
			_bar(canvas, 0.44, 0.5, -0.5, 0.5, w, h, def.outline_color)
			canvas.draw_rect(Rect2( - def.size * 0.5, def.size), def.outline_color, false, 1.0)


static func draw_hazard_marks(canvas: CanvasItem, def: EntityDefinition) -> void :
	var w: = def.size.x
	var h: = def.size.y
	var band: = Rect2( - w * 0.5 + 2.0, - h * 0.12, w - 4.0, h * 0.24)
	canvas.draw_rect(band, def.detail_color)
	var stripe: = Color(0.12, 0.1, 0.08)
	var step: = band.size.y * 1.1
	var x: = band.position.x - step
	while x < band.end.x:
		var points: = PackedVector2Array([
			Vector2(clampf(x, band.position.x, band.end.x), band.end.y), 
			Vector2(clampf(x + step * 0.5, band.position.x, band.end.x), band.end.y), 
			Vector2(clampf(x + step * 0.5 + band.size.y, band.position.x, band.end.x), band.position.y), 
			Vector2(clampf(x + band.size.y, band.position.x, band.end.x), band.position.y)])
		canvas.draw_colored_polygon(points, stripe)
		x += step

	canvas.draw_circle(Vector2(0, - h * 0.32), w * 0.12, def.detail_color, true, -1.0, true)


static func draw_bomb(canvas: CanvasItem, def: EntityDefinition) -> void :
	if def.is_circle():
		var r: = def.radius
		canvas.draw_circle(Vector2.ZERO, r, def.fill_color, true, -1.0, true)
		canvas.draw_circle(Vector2( - r * 0.3, - r * 0.3), r * 0.3, def.fill_color.lightened(0.25), true, -1.0, true)
		canvas.draw_rect(Rect2(Vector2( - r * 0.25, - r * 1.15), Vector2(r * 0.5, r * 0.35)), def.outline_color)
		canvas.draw_line(Vector2(0, - r * 1.15), Vector2(r * 0.4, - r * 1.55), def.detail_color, 2.0, true)
		canvas.draw_arc(Vector2.ZERO, r - 0.75, 0.0, TAU, 32, def.outline_color, 1.5, true)
		return
	var rect: = Rect2( - def.size * 0.5, def.size)
	var cap: = rect.size.y * 0.5
	canvas.draw_rect(rect.grow_individual( - cap, 0, - cap, 0), def.fill_color)
	canvas.draw_circle(Vector2(rect.position.x + cap, 0), cap, def.fill_color, true, -1.0, true)
	canvas.draw_colored_polygon(PackedVector2Array([Vector2(rect.end.x - cap, rect.position.y), Vector2(rect.end.x, - cap * 0.35), 
		Vector2(rect.end.x, cap * 0.35), Vector2(rect.end.x - cap, rect.end.y)]), def.fill_color.darkened(0.2))
	canvas.draw_rect(Rect2(Vector2( - rect.size.x * 0.05, rect.position.y), Vector2(rect.size.x * 0.1, rect.size.y)), def.detail_color)
	canvas.draw_rect(rect, def.outline_color, false, 1.5)


static func draw_glass(canvas: CanvasItem, def: EntityDefinition) -> void :
	var rect: = Rect2( - def.size * 0.5, def.size)
	canvas.draw_rect(rect, Color(def.fill_color, 0.38))
	var shine: = Color(def.detail_color, 0.6)
	canvas.draw_line(rect.position + Vector2(rect.size.x * 0.3, rect.size.y * 0.08), 
		rect.position + Vector2(rect.size.x * 0.3, rect.size.y * 0.45), shine, maxf(rect.size.x * 0.2, 1.0))
	canvas.draw_rect(rect, Color(def.outline_color, 0.9), false, 1.5)



static func _poly(canvas: CanvasItem, points: Array, w: float, h: float, fill: Color, outline: Color) -> void :
	var scaled: = PackedVector2Array()
	for point: Array in points:
		scaled.append(Vector2(float(point[0]) * w, float(point[1]) * h))
	canvas.draw_colored_polygon(scaled, fill)
	scaled.append(scaled[0])
	canvas.draw_polyline(scaled, outline, 1.2, true)


static func _bar(canvas: CanvasItem, x0: float, x1: float, y0: float, y1: float, w: float, h: float, color: Color) -> void :
	if color.a <= 0.0:
		return
	canvas.draw_rect(Rect2(Vector2(x0 * w, y0 * h), Vector2((x1 - x0) * w, (y1 - y0) * h)), color)
