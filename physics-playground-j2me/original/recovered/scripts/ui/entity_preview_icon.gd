class_name EntityPreviewIcon
extends Control



var definition: EntityDefinition:
	set(value):
		definition = value
		queue_redraw()


func _draw() -> void :
	if definition == null:
		return
	var bounds: = definition.get_bounding_size()
	var fit: = minf(minf(size.x / bounds.x, size.y / bounds.y) * 0.9, 1.5)
	if definition.is_assembly():
		var center: = definition.assembly.bounds.get_center()
		EntityVisual.draw_assembly(self, definition, size * 0.5 - center * fit, fit)
		return
	draw_set_transform(size * 0.5, 0.0, Vector2.ONE * fit)
	EntityVisual.draw_definition(self, definition, 1)
	draw_set_transform_matrix(Transform2D.IDENTITY)
