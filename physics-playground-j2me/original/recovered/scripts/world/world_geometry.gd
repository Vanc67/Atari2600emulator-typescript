class_name WorldGeometry
extends Node2D



const FLOOR_DEPTH: = 6000.0
const WALL_THICKNESS: = 200.0
const SURFACE_COLOR: = Color("34373c")
const EDGE_COLOR: = Color("5b6068")
const EDGE_WIDTH: = 3.0

var floor_rect: Rect2
var wall_rects: Array[Rect2] = []
var _surface_material: PhysicsMaterial


func build(half_width: float, floor_y: float, wall_height: float) -> void :
	for child in get_children():
		child.queue_free()


	_surface_material = PhysicsMaterial.new()
	_surface_material.friction = 1.0
	_surface_material.bounce = 0.0

	var outer_half_width: = half_width + WALL_THICKNESS
	floor_rect = Rect2( - outer_half_width, floor_y, outer_half_width * 2.0, FLOOR_DEPTH)
	wall_rects = [
		Rect2( - outer_half_width, floor_y - wall_height, WALL_THICKNESS, wall_height), 
		Rect2(half_width, floor_y - wall_height, WALL_THICKNESS, wall_height), 
	]
	_add_static_rect(floor_rect, "Floor")
	_add_static_rect(wall_rects[0], "LeftWall")
	_add_static_rect(wall_rects[1], "RightWall")
	queue_redraw()


func _add_static_rect(rect: Rect2, body_name: String) -> void :
	var body: = StaticBody2D.new()
	body.name = body_name
	body.position = rect.get_center()
	body.collision_layer = CollisionLayers.WORLD
	body.collision_mask = 0
	body.physics_material_override = _surface_material
	var shape: = RectangleShape2D.new()
	shape.size = rect.size
	var collision: = CollisionShape2D.new()
	collision.shape = shape
	body.add_child(collision)
	add_child(body)


func _draw() -> void :
	draw_rect(floor_rect, SURFACE_COLOR)
	for wall in wall_rects:
		draw_rect(wall, SURFACE_COLOR)
	if wall_rects.size() < 2:
		return

	var left_inner: = wall_rects[0].end.x
	var right_inner: = wall_rects[1].position.x
	var top: = wall_rects[0].position.y
	draw_line(Vector2(left_inner, floor_rect.position.y), Vector2(right_inner, floor_rect.position.y), EDGE_COLOR, EDGE_WIDTH)
	draw_line(Vector2(left_inner, top), Vector2(left_inner, floor_rect.position.y), EDGE_COLOR, EDGE_WIDTH)
	draw_line(Vector2(right_inner, top), Vector2(right_inner, floor_rect.position.y), EDGE_COLOR, EDGE_WIDTH)
