class_name UiTheme
extends RefCounted



const ACCENT: = Color("e3a33b")
const TEXT: = Color("d6d8db")
const TEXT_DIM: = Color("8b9096")
const ERROR: = Color("e0664f")
const PANEL_BG: = Color(0.11, 0.115, 0.125, 0.94)
const PANEL_BORDER: = Color("373b41")
const BUTTON_BG: = Color("2a2e33")
const BUTTON_HOVER: = Color("343940")
const BUTTON_PRESSED: = Color("3d3322")
const TRACK_BG: = Color("1a1c1f")


static func build() -> Theme:
	var theme: = Theme.new()
	theme.default_font_size = 14

	var panel: = _box(PANEL_BG, PANEL_BORDER, 1, 4)
	panel.set_content_margin_all(8)
	theme.set_stylebox("panel", "PanelContainer", panel)
	theme.set_stylebox("panel", "TooltipPanel", panel)

	var normal: = _box(BUTTON_BG, PANEL_BORDER, 1, 3)
	var hover: = _box(BUTTON_HOVER, PANEL_BORDER.lightened(0.15), 1, 3)
	var pressed: = _box(BUTTON_PRESSED, ACCENT, 1, 3)
	var disabled: = _box(BUTTON_BG.darkened(0.2), PANEL_BORDER, 1, 3)
	for box in [normal, hover, pressed, disabled]:
		(box as StyleBoxFlat).set_content_margin(SIDE_LEFT, 10)
		(box as StyleBoxFlat).set_content_margin(SIDE_RIGHT, 10)
		(box as StyleBoxFlat).set_content_margin(SIDE_TOP, 5)
		(box as StyleBoxFlat).set_content_margin(SIDE_BOTTOM, 5)
	theme.set_stylebox("normal", "Button", normal)
	theme.set_stylebox("hover", "Button", hover)
	theme.set_stylebox("pressed", "Button", pressed)
	theme.set_stylebox("hover_pressed", "Button", pressed)
	theme.set_stylebox("disabled", "Button", disabled)
	theme.set_stylebox("focus", "Button", StyleBoxEmpty.new())
	theme.set_color("font_color", "Button", TEXT)
	theme.set_color("font_hover_color", "Button", Color.WHITE)
	theme.set_color("font_pressed_color", "Button", ACCENT)
	theme.set_color("font_hover_pressed_color", "Button", ACCENT)
	theme.set_color("font_focus_color", "Button", TEXT)
	theme.set_color("font_disabled_color", "Button", TEXT_DIM)

	theme.set_color("font_color", "Label", TEXT)
	theme.set_type_variation("DimLabel", "Label")
	theme.set_color("font_color", "DimLabel", TEXT_DIM)
	theme.set_font_size("font_size", "DimLabel", 12)
	theme.set_type_variation("HeaderLabel", "Label")
	theme.set_color("font_color", "HeaderLabel", ACCENT)
	theme.set_font_size("font_size", "HeaderLabel", 13)

	var track: = _box(TRACK_BG, PANEL_BORDER, 1, 2)
	track.set_content_margin(SIDE_TOP, 3)
	track.set_content_margin(SIDE_BOTTOM, 3)
	theme.set_stylebox("slider", "HSlider", track)
	var fill: = _box(ACCENT.darkened(0.35), ACCENT.darkened(0.35), 0, 2)
	theme.set_stylebox("grabber_area", "HSlider", fill)
	theme.set_stylebox("grabber_area_highlight", "HSlider", fill)

	var separator: = StyleBoxLine.new()
	separator.color = PANEL_BORDER
	separator.thickness = 1
	theme.set_stylebox("separator", "HSeparator", separator)
	var vertical_separator: = StyleBoxLine.new()
	vertical_separator.color = PANEL_BORDER
	vertical_separator.thickness = 1
	vertical_separator.vertical = true
	theme.set_stylebox("separator", "VSeparator", vertical_separator)
	return theme


static func _box(background: Color, border: Color, border_width: int, corner_radius: int) -> StyleBoxFlat:
	var box: = StyleBoxFlat.new()
	box.bg_color = background
	box.border_color = border
	box.set_border_width_all(border_width)
	box.set_corner_radius_all(corner_radius)
	return box
