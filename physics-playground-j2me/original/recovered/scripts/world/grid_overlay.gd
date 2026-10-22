class_name GridOverlay
extends Node2D



const BASE_SPACING: = 100.0
const MIN_SCREEN_SPACING: = 22.0
const MAJOR_EVERY: = 5
const MINOR_COLOR: = Color(1, 1, 1, 0.035)
const MAJOR_COLOR: = Color(1, 1, 1, 0.075)

var camera: CameraController


func _process(_delta: float) -> void :
	if visible and camera:
		queue_redraw()


func _draw() -> void :
	if camera == null:
		return
	var zoom_level: = camera.zoom.x
	var spacing: = BASE_SPACING
	while spacing * zoom_level < MIN_SCREEN_SPACING:
		spacing *= 2.0
	while spacing * zoom_level > MIN_SCREEN_SPACING * 4.0 and spacing > BASE_SPACING / 8.0:
		spacing *= 0.5

	var view: = camera.get_visible_world_rect().grow(spacing)
	var minor_lines: = PackedVector2Array()
	var major_lines: = PackedVector2Array()
	for index in range(floori(view.position.x / spacing), ceili(view.end.x / spacing) + 1):
		var x: = index * spacing
		var target: = major_lines if posmod(index, MAJOR_EVERY) == 0 else minor_lines
		target.append(Vector2(x, view.position.y))
		target.append(Vector2(x, view.end.y))
	for index in range(floori(view.position.y / spacing), ceili(view.end.y / spacing) + 1):
		var y: = index * spacing
		var target: = major_lines if posmod(index, MAJOR_EVERY) == 0 else minor_lines
		target.append(Vector2(view.position.x, y))
		target.append(Vector2(view.end.x, y))

	if not minor_lines.is_empty():
		draw_multiline(minor_lines, MINOR_COLOR, -1.0)
	if not major_lines.is_empty():
		draw_multiline(major_lines, MAJOR_COLOR, -1.0)
