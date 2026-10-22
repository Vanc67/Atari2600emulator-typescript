class_name EntitySerializer
extends RefCounted



static func serialize(entity: SandboxEntity) -> Dictionary:
	var data: = {
		"uid": entity.entity_uid, 
		"type": entity.get_type_id(), 
		"position": vector2_to_array(entity.global_position), 
		"rotation": entity.global_rotation, 
		"linear_velocity": vector2_to_array(entity.linear_velocity), 
		"angular_velocity": entity.angular_velocity, 
		"frozen": entity.is_frozen(), 
	}
	var component_states: = {}
	for component in entity.get_components():
		var state: = component.serialize_state()
		if not state.is_empty():
			component_states[String(component.get_component_type())] = state
	if not component_states.is_empty():
		data["components"] = component_states
	if not entity.properties.is_empty():
		data["properties"] = entity.properties.duplicate(true)
	if entity.is_assembly_part():
		data["assembly"] = {"uid": entity.assembly.assembly_uid, "part": entity.assembly_part_name}
	return data





static func apply_state(entity: SandboxEntity, data: Dictionary, include_motion: bool) -> void :
	var frozen: = bool(data.get("frozen", false))
	if include_motion and not frozen:
		entity.linear_velocity = array_to_vector2(data.get("linear_velocity"), Vector2.ZERO)
		entity.angular_velocity = float(data.get("angular_velocity", 0.0))
		if entity is DamageableEntity:
			(entity as DamageableEntity).reset_impact_history()
	entity.set_frozen(frozen)

	var saved_properties: Variant = data.get("properties")
	if saved_properties is Dictionary:
		entity.properties.merge(saved_properties.duplicate(true), true)

	var component_states: Variant = data.get("components")
	if component_states is Dictionary:
		for component_type: Variant in component_states:
			var component: = entity.get_component(StringName(str(component_type)))
			var state: Variant = component_states[component_type]
			if component and state is Dictionary:
				component.deserialize_state(state)


static func vector2_to_array(value: Vector2) -> Array:
	return [value.x, value.y]


static func array_to_vector2(value: Variant, fallback: Vector2) -> Vector2:
	if value is Array and value.size() == 2:
		return Vector2(float(value[0]), float(value[1]))
	return fallback
