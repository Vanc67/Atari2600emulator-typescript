class_name WorldUnits
extends RefCounted






const PIXELS_PER_METER: = 100.0


static func pixels_to_meters(pixels: float) -> float:
	return pixels / PIXELS_PER_METER


static func pixel_area_to_square_meters(area_pixels: float) -> float:
	return area_pixels / (PIXELS_PER_METER * PIXELS_PER_METER)
