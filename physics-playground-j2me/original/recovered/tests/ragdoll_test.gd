extends Node










const MAIN_SCENE: = preload("res://scenes/main.tscn")
const HUMAN: = "core:human"
const V1_SAVE_FIXTURE: = "res://tests/fixtures/saves/v0_1_world.json"
const PART_NAMES: Array[String] = [
	"pelvis", "upper_torso", "head", "upper_arm_left", "forearm_left", "upper_arm_right", 
	"forearm_right", "thigh_left", "lower_leg_left", "thigh_right", "lower_leg_right", 
]
const EXPECTED_HEALTH: = {
	"head": 100, "upper_torso": 200, "pelvis": 150, "upper_arm_left": 100, "forearm_left": 80, 
	"thigh_left": 120, "lower_leg_left": 100, 
}

var _checks: = 0
var _failures: PackedStringArray = []
var _world: SandboxWorld
var _interaction: InteractionController
var _main: Node


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(20261016)
	_test_definitions()
	_test_health_component_api()
	_test_impact_damage_formula()
	_test_invalid_assembly_rejected()
	_main = MAIN_SCENE.instantiate()
	add_child(_main)
	_world = _main.world
	_interaction = _main.interaction
	await _frames(2)
	await _test_spawn_structure()
	await _test_settle_stability()
	await _test_limb_drag()
	await _test_paused_group_drag()
	await _test_interaction_with_objects()
	await _test_fall_damage()
	await _test_head_impact_kills()
	await _test_impact_detachment()
	await _test_strain_detachment()
	await _test_group_commands_and_inspector()
	await _test_breakable_box()
	await _test_save_load_ragdolls()
	await _test_v1_save_compatibility()
	await _test_many_ragdolls()
	_main.queue_free()
	await _frames(1)

	print("")
	print("Ragdoll test: %d checks, %d failure(s)" % [_checks, _failures.size()])
	for failure in _failures:
		print("  FAIL: " + failure)

	await Audio.prepare_for_quit()
	get_tree().quit(0 if _failures.is_empty() else 1)




func _check(condition: bool, label: String) -> void :
	_checks += 1
	if condition:
		print("  ok   " + label)
	else:
		print("  FAIL " + label)
		_failures.append(label)


func _frames(count: int) -> void :
	for index in count:
		await get_tree().physics_frame


func _spawn_human(origin: = Vector2(0, -110)) -> EntityAssembly:
	return _world.spawn_assembly(HUMAN, origin)


func _total_damage(human: EntityAssembly) -> float:
	var total: = 0.0
	for part in human.get_parts():
		var health: = part.get_health()
		total += health.max_health - health.current_health
	return total


func _worst_limit_violation_degrees(human: EntityAssembly) -> float:
	var worst: = 0.0
	for joint in human.get_joints():
		var limits: = joint.definition
		if joint.broken or not limits.has_angle_limits:
			continue
		var relative: = wrapf(joint.body_b.rotation - joint.body_a.rotation - limits.rest_angle, - PI, PI)
		worst = maxf(worst, maxf(relative - limits.upper_angle, limits.lower_angle - relative))
	return rad_to_deg(worst)


func _pin_count(human: EntityAssembly) -> int:
	return human.get_children().filter( func(node: Node) -> bool: return node is PinJoint2D).size()


func _all_finite(human: EntityAssembly) -> bool:
	for part in human.get_parts():
		if not part.global_position.is_finite() or not part.linear_velocity.is_finite():
			return false
	return true




func _test_definitions() -> void :
	print("[definitions]")
	var registry: = Content.registry
	_check(Content.load_errors.is_empty(), "content loads without errors %s" % [Content.load_errors])
	var human: = registry.get_entity_definition(HUMAN)
	_check(human != null and human.is_assembly(), "core:human is registered as an assembly")
	if human == null:
		return
	_check(human.category == "Ragdolls" and registry.get_categories()[0] == "Ragdolls", "Ragdolls category exists and is listed first")
	_check(registry.get_entities_in_category("Ragdolls").size() == 1, "spawn menu lists only the human, not its limbs")
	_check(human.assembly.parts.size() == 11 and human.assembly.joints.size() == 10, "11 body parts and 10 joints")
	for part_name in PART_NAMES:
		var part: = human.assembly.get_part(part_name)
		_check(part != null and part.definition != null and part.definition.hidden, 
			"part '%s' resolves to a hidden limb definition" % part_name)
	for part_name: String in EXPECTED_HEALTH:
		var part: = human.assembly.get_part(part_name)
		_check(is_equal_approx(part.max_health, float(EXPECTED_HEALTH[part_name])), 
			"%s max health is %d" % [part_name, EXPECTED_HEALTH[part_name]])
	_check(absf(human.compute_mass(null) - 72.0) < 0.01, "total human mass is 72 kg (%.1f)" % human.compute_mass(null))
	var limb: = registry.get_entity_definition("core:human_forearm_left")
	_check(limb.is_capsule() and limb.get_shape() is CapsuleShape2D, "limbs use capsule collision shapes")
	_check(limb.components.has("impact_damage") and limb.components.has("health"), "limbs have health and impact damage")


func _test_health_component_api() -> void :
	print("[health component]")
	var health: = HealthComponent.new()
	health.configure({"max_health": 100}, null, null)
	var events: = {"damaged": 0, "depleted": 0, "restored": 0, "healed": 0.0, "changed": 0}
	health.damaged.connect( func(_info: DamageInfo) -> void : events["damaged"] += 1)
	health.depleted.connect( func(_info: DamageInfo) -> void : events["depleted"] += 1)
	health.restored.connect( func() -> void : events["restored"] += 1)
	health.healed.connect( func(amount: float) -> void : events["healed"] += amount)
	health.health_changed.connect( func(_current: float, _maximum: float) -> void : events["changed"] += 1)
	_check(health.apply_damage(30.0) == 30.0 and health.current_health == 70.0, "apply_damage removes health")
	_check(is_equal_approx(health.get_damage_fraction(), 0.3), "damage fraction query")
	_check(health.apply_damage(500.0) == 70.0 and health.is_depleted() and health.is_destroyed(), "damage clamps at zero and depletes")
	_check(health.apply_damage(10.0) == 0.0, "no further damage once destroyed")
	_check(health.heal(40.0) == 40.0 and not health.is_depleted(), "heal restores health")
	_check(health.heal_full() == 60.0 and health.current_health == 100.0, "heal_full restores to maximum")
	_check(events["damaged"] == 2 and events["depleted"] == 1 and events["restored"] == 1 and events["healed"] == 100.0, 
		"damage, depleted, restored and healed events fire (%s)" % [events])
	_check(events["changed"] == 4, "health_changed fires on every change")
	health.invulnerable = true
	_check(health.apply_damage(10.0) == 0.0, "invulnerable ignores damage")
	health.free()


func _test_impact_damage_formula() -> void :
	print("[impact damage formula]")
	var sensor: = ImpactDamageComponent.new()
	sensor.configure({"min_impact_speed": 7.0, "damage_per_speed": 12.0, "max_damage_per_hit": 150.0}, null, null)
	_check(sensor.compute_damage(6.9) == 0.0, "below the threshold does no damage")
	_check(is_equal_approx(sensor.compute_damage(10.0), 36.0), "damage grows linearly above the threshold")
	_check(sensor.compute_damage(100.0) == 150.0, "damage per hit is capped")
	sensor.free()


func _test_invalid_assembly_rejected() -> void :
	print("[assembly validation]")
	var reader: = DataReader.new("inline")
	var data: = {"id": "broken", "assembly": {"parts": [{"name": "a", "entity": "does_not_exist"}], 
		"joints": [{"name": "j", "parent": "a", "child": "missing"}]}}
	var definition: = EntityDefinition.from_dict(data, "test", reader)
	definition.assembly.link(Content.registry, reader)
	_check(reader.errors.size() >= 2, "unknown parts and joint references are reported (%d errors)" % reader.errors.size())




func _test_spawn_structure() -> void :
	print("[spawn]")
	var human: = _spawn_human()
	_check(human != null, "spawn_assembly creates a human")
	_check(_world.get_entity_count() == 11 and _world.get_assembly_count() == 1, "11 bodies and 1 assembly in the world")
	_check(_world.get_object_count() == 1, "the human counts as one object")
	_check(_pin_count(human) == 10, "10 PinJoint2D pivots created")
	var parts_ok: = true
	for part_name in PART_NAMES:
		var part: = human.get_part(part_name)
		if part == null or part.assembly != human or part.assembly_part_name != part_name:
			parts_ok = false
	_check(parts_ok, "every part knows its assembly and part name")
	var head: = human.get_part("head")
	var exceptions_ok: = true
	var parts: = human.get_parts()
	for i in parts.size():
		for j in range(i + 1, parts.size()):

			if not (parts[i].get_collision_exceptions().has(parts[j]) or parts[j].get_collision_exceptions().has(parts[i])):
				exceptions_ok = false
	_check(exceptions_ok, "self-collision is disabled between every pair of parts")
	_check(head is DamageableEntity and head.contact_monitor, "limbs use the impact-sensing entity variant")
	var uids: = {}
	for part in human.get_parts():
		uids[part.entity_uid] = true
	_check(uids.size() == 11 and not uids.has(human.assembly_uid), "parts and assembly have unique uids")
	_check(human.controller is HumanController and human.controller.get_state_name() == "alive", "new human is alive")
	_world.clear_entities()
	await _frames(1)
	_check(_world.get_assembly_count() == 0, "clearing the world frees the assembly")


func _test_settle_stability() -> void :
	print("[settle stability]")
	var human: = _spawn_human()
	await _frames(120 * 10)
	var lowest_ok: = true
	for part in human.get_parts():
		if part.global_position.y > _world.floor_y:
			lowest_ok = false
	_check(lowest_ok, "no part falls through the floor")
	_check(human.get_broken_joint_names().is_empty(), "collapsing does not break joints")
	var peak: = 0.0
	for joint in human.get_joints():
		peak = maxf(peak, joint.last_strain)
	_check(peak < 3.0, "joints hold together at rest (max separation %.2f px)" % peak)
	var violation: = _worst_limit_violation_degrees(human)
	_check(violation < 4.0, "angle limits hold at rest (worst violation %.1f°)" % violation)
	var sleeping: = human.get_parts().filter( func(part: SandboxEntity) -> bool: return part.sleeping).size()
	_check(sleeping == 11, "collapsed ragdoll comes to rest and sleeps (%d/11 asleep, no jitter)" % sleeping)
	_check(_total_damage(human) == 0.0, "collapsing to the floor causes no damage (%.1f)" % _total_damage(human))
	_world.clear_entities()


func _test_limb_drag() -> void :
	print("[limb dragging]")
	var human: = _spawn_human()
	await _frames(120 * 3)
	var hand: = human.get_part("forearm_right")
	var grab: = hand.global_transform * Vector2(0, 12)
	human.begin_drag_protection()
	var drag: = DragHandle.new(hand, grab)
	_check(drag.get_group().size() == 11, "grabbing a hand moves the whole connected body")
	drag.target_position = Vector2(hand.global_position.x, -450)
	var max_strain: = 0.0
	for index in 120 * 3:
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
		for joint in human.get_joints():
			max_strain = maxf(max_strain, joint.last_strain)
	var error: = drag.get_anchor_world_position().distance_to(drag.target_position)
	_check(error < 25.0, "a 72 kg body is lifted by the hand (grab point %.1f px from the cursor)" % error)
	_check(human.get_part("pelvis").global_position.y < -200.0, "the body hangs below the hand, off the floor")
	_check(human.get_broken_joint_names().is_empty(), "lifting by a limb does not break joints (peak strain %.1f px)" % max_strain)
	for index in 90:
		drag.target_position = Vector2(300.0 * sin(index / 90.0 * TAU), -450)
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
	_check(human.get_broken_joint_names().is_empty(), "fast swinging while dragging does not break joints")
	human.end_drag_protection()
	await _frames(120 * 3)
	_check(_all_finite(human) and human.get_broken_joint_names().is_empty(), "released body falls and lands intact")
	_world.clear_entities()


func _test_paused_group_drag() -> void :
	print("[paused drag]")
	var human: = _spawn_human(Vector2(0, -300))
	await _frames(5)
	GameState.set_paused(true)
	var head: = human.get_part("head")
	var pelvis_before: = human.get_part("pelvis").global_position
	var drag: = DragHandle.new(head, head.global_position)
	drag.target_position = head.global_position + Vector2(200, -100)
	drag.update(true, false)
	var shift: = human.get_part("pelvis").global_position - pelvis_before
	_check(shift.distance_to(Vector2(200, -100)) < 0.5, "dragging while paused moves the whole body rigidly")
	GameState.set_paused(false)
	await _frames(30)
	_check(human.get_broken_joint_names().is_empty(), "joints stay intact after a paused move")
	_world.clear_entities()


func _test_interaction_with_objects() -> void :
	print("[interaction with objects]")
	var human: = _spawn_human()
	await _frames(120 * 4)
	var torso: = human.get_part("upper_torso")
	var box: = _world.spawn_entity("core:steel_box", Vector2(torso.global_position.x, -400))
	var touched: = false
	for index in 120 * 3:
		await get_tree().physics_frame
		for part in human.get_parts():
			if part.get_colliding_bodies().has(box):
				touched = true
	_check(touched, "a falling steel box collides with the ragdoll's limbs")
	_check(_all_finite(human) and human.get_broken_joint_names().is_empty(), "ragdoll stays stable under the box")
	_world.clear_entities()


func _test_fall_damage() -> void :
	print("[fall damage]")
	var low: = _spawn_human(Vector2(0, -400))
	await _frames(120 * 4)
	_check(_total_damage(low) == 0.0, "a 3 m fall is harmless (%.0f damage)" % _total_damage(low))
	_world.clear_entities()
	var medium: = _spawn_human(Vector2(0, -1000))
	await _frames(120 * 4)
	var leg_damage: = medium.get_part("lower_leg_left").get_health().get_damage_fraction()
	_check(leg_damage > 0.2, "a 9 m fall injures the legs (%.0f%% damage)" % (leg_damage * 100.0))
	_check(medium.get_broken_joint_names().is_empty(), "a 9 m fall does not tear limbs off")
	_world.clear_entities()
	var high: = _spawn_human(Vector2(0, -2500))
	await _frames(120 * 5)
	var state: = high.controller.get_state_name()
	_check(state != "alive", "a 24 m fall incapacitates or kills (%s)" % state)
	var visual_ok: = true
	for part in high.get_parts():
		var health: = part.get_health()
		if health.get_damage_fraction() > 0.01 and part.visual.damage_level <= 0.0:
			visual_ok = false
	_check(visual_ok, "damaged limbs show damage visuals")
	_world.clear_entities()


func _test_head_impact_kills() -> void :
	print("[head impact]")
	var human: = _spawn_human()
	await _frames(10)
	var head: = human.get_part("head")
	var sphere: = _world.spawn_entity("core:metal_sphere", head.global_position + Vector2(-300, 0))
	sphere.linear_velocity = Vector2(1800, 0)
	await _frames(60)
	_check(head.get_health().is_depleted(), "an 18 m/s steel sphere destroys the head")
	_check(human.controller.get_state_name() == "dead", "destroying the head kills the human")
	_check(head.visual.vitality == EntityVisual.Vitality.DEAD, "dead humans are drawn lifeless")
	var lines: = "\n".join(human.get_inspector_lines(head))
	_check(lines.contains("Dead") and lines.contains("Head"), "inspector reports state and cause")
	_world.clear_entities()


func _test_impact_detachment() -> void :
	print("[impact detachment]")
	var human: = _spawn_human()
	await _frames(10)
	var forearm: = human.get_part("forearm_right")
	var sphere: = _world.spawn_entity("core:metal_sphere", forearm.global_position + Vector2(-300, 0))
	sphere.linear_velocity = Vector2(4500, 0)
	await _frames(30)
	var elbow: = human.get_joint("elbow_right")
	_check(elbow.broken, "a violent impact tears the forearm off")
	_check( not human.is_part_attached(forearm), "the forearm is detached")
	var broken_count: = human.get_broken_joint_names().size()
	_check(_pin_count(human) == 10 - broken_count, "broken joints' PinJoint2D nodes are removed (%d broken)" % broken_count)
	_check(forearm.visual.wound_points.size() == 1, "detached limb shows a wound")
	await _frames(120 * 3)
	_check(is_instance_valid(forearm) and forearm.global_position.y < _world.floor_y, "detached limb keeps colliding and rests on the floor")
	_check(forearm.get_health().is_damaged(), "detached limb keeps its health state")
	var drag: = DragHandle.new(forearm, forearm.global_position)
	_check(drag.get_group().size() == 1, "a detached limb is dragged on its own")
	drag.target_position = forearm.global_position + Vector2(0, -300)
	for index in 120:
		drag.update(false, false, 1.0 / 120.0)
		await get_tree().physics_frame
	_check(drag.get_anchor_world_position().distance_to(drag.target_position) < 10.0, "detached limb follows the cursor")
	_check(_world.pick_entity_at(forearm.global_position) == forearm, "detached limb can be picked")
	_interaction.select(forearm)
	_interaction.delete_selected()
	await _frames(1)
	_check( not is_instance_valid(forearm) or not forearm.is_inside_tree(), "deleting a detached limb removes it")
	_check(human.get_part_count() == 10 and _world.get_assembly_count() == 1, "the rest of the body is unaffected")
	_world.clear_entities()


func _test_strain_detachment() -> void :
	print("[strain detachment]")
	var human: = _spawn_human(Vector2(0, -400))
	await _frames(5)
	var head: = human.get_part("head")
	var neck: = human.get_joint("neck")


	for index in 10:
		if neck.broken:
			break
		head.apply_central_force(Vector2(0, -2000000) * head.mass)
		await get_tree().physics_frame
	_check(neck.broken, "an extreme sustained pull overstretches and tears the neck (peak %.0f px)" % neck.peak_strain)
	_check(human.controller.get_state_name() == "dead", "a severed neck kills (%s)" % human.controller.state_reason)
	_world.clear_entities()


func _test_group_commands_and_inspector() -> void :
	print("[group commands + inspector]")
	var human: = _spawn_human()
	await _frames(120 * 2)
	var thigh: = human.get_part("thigh_left")
	_interaction.select(thigh)
	await _frames(1)
	var details: = ((_main.hud as Hud).get_node("%InspectorDetails") as Label).text
	var title: = ((_main.hud as Hud).get_node("%InspectorTitle") as Label).text
	for expected in ["Body part: Left Thigh", "Health: 120 / 120", "Damage: 0%", "Human state: Alive", "Mass: 8.0 kg", "Velocity:"]:
		_check(details.contains(expected), "inspector shows '%s'" % expected)
	_check(title == "Human #%d" % human.assembly_uid, "inspector title names the human (%s)" % title)

	_interaction.toggle_freeze_selected()
	var all_frozen: = human.get_parts().all( func(part: SandboxEntity) -> bool: return part.is_frozen())
	_check(all_frozen, "freezing a limb freezes the whole connected body")
	_interaction.toggle_freeze_selected()

	thigh.get_health().apply_damage(50.0)
	_interaction.duplicate_selected()
	await _frames(1)
	_check(_world.get_assembly_count() == 2 and _world.get_entity_count() == 22, "duplicating a limb duplicates the whole human")
	var copy: = _interaction.selected_entity
	_check(copy != thigh and copy.assembly != human and copy.assembly_part_name == "thigh_left", "the copy's matching limb is selected")
	_check(is_equal_approx(copy.get_health().current_health, 70.0), "duplicate keeps limb health")
	_check(_pin_count(copy.assembly) == 10, "duplicate has its own joints")

	_interaction.heal_selected()
	_check( not copy.get_health().is_damaged(), "heal restores the human")
	_interaction.delete_selected()
	await _frames(1)
	_check(_world.get_assembly_count() == 1 and _world.get_entity_count() == 11, "deleting a limb deletes its connected body")
	_world.clear_entities()


func _test_breakable_box() -> void :
	print("[breakable objects]")
	var box: = _world.spawn_entity("core:wooden_box", Vector2(0, -100))
	box.get_health().apply_damage(1000.0)
	await _frames(2)
	_check( not is_instance_valid(box) or not box.is_inside_tree(), "a wooden box is removed when its health is depleted")
	var ball: = _world.spawn_entity("core:rubber_ball", Vector2(200, -100))
	ball.get_health().apply_damage(1000.0)
	await _frames(2)
	_check(is_instance_valid(ball) and ball.is_inside_tree() and ball.visual.damage_level == 1.0, 
		"objects with on_depleted 'none' and no destructible stay, showing full damage")
	_world.clear_entities()


func _test_save_load_ragdolls() -> void :
	print("[save / load ragdolls]")
	var save_manager: = SaveManager.new()
	var human: = _spawn_human(Vector2(-300, -110))
	var other: = _spawn_human(Vector2(300, -400))
	await _frames(120)
	human.break_joint_by_name("elbow_left")
	human.get_part("head").get_health().apply_damage(100.0)
	other.get_part("thigh_right").get_health().apply_damage(40.0)
	other.get_part("pelvis").set_frozen(true)
	await _frames(20)
	var expected: = {}
	for entity in _world.get_entities():
		expected[entity.entity_uid] = EntitySerializer.serialize(entity)
	var human_uid: = human.assembly_uid
	var other_uid: = other.assembly_uid
	_check(save_manager.save_to_slot("ragdoll_test", _world, _world.camera, Content.registry) == OK, "save with ragdolls succeeds")
	var text: = FileAccess.get_file_as_string(save_manager.get_slot_path("ragdoll_test"))
	_check(text.contains("\"assemblies\"") and text.contains("\"connected\": false"), "save stores assemblies and joint connection states")

	_world.clear_entities()
	var result: = save_manager.load_from_slot("ragdoll_test", _world, _world.camera, Content.registry)
	_check(result.ok and result.assembly_count == 2, "both ragdolls are reconstructed (%s)" % result.message)
	var loaded: = _world.get_assembly_by_uid(human_uid)
	var loaded_other: = _world.get_assembly_by_uid(other_uid)
	_check(loaded != null and loaded_other != null, "ragdoll identities (uids) are preserved")
	if loaded == null or loaded_other == null:
		return
	_check(loaded.get_broken_joint_names() == PackedStringArray(["elbow_left"]), "broken joints are restored")
	_check(_pin_count(loaded) == 9 and _pin_count(loaded_other) == 10, "joints are created exactly once (no duplicates)")
	_check(loaded.controller.get_state_name() == "dead" and loaded_other.controller.get_state_name() == "alive", "human states are restored")
	_check( not loaded.is_part_attached(loaded.get_part("forearm_left")), "detached limb stays detached")
	var all_match: = true
	for uid: int in expected:
		var entity: = _world.get_entity_by_uid(uid)
		var saved: Dictionary = expected[uid]
		if entity == null:
			all_match = false
			continue
		var position_ok: = entity.global_position.distance_to(EntitySerializer.array_to_vector2(saved["position"], Vector2.INF)) < 0.01
		var rotation_ok: = absf(entity.global_rotation - float(saved["rotation"])) < 0.0001
		var velocity_ok: = entity.linear_velocity.distance_to(EntitySerializer.array_to_vector2(saved["linear_velocity"], Vector2.INF)) < 0.01
		var health_ok: = is_equal_approx(entity.get_health().current_health, 
			float(saved.get("components", {}).get("health", {}).get("current", entity.get_health().max_health)))
		if not (position_ok and rotation_ok and velocity_ok and health_ok and entity.is_frozen() == bool(saved["frozen"])):
			all_match = false
	_check(all_match, "every limb's position, rotation, velocity, health and freeze state are restored")
	await _frames(120)
	_check(loaded.get_broken_joint_names().size() == 1 and loaded_other.get_broken_joint_names().is_empty(), 
		"restored ragdolls stay stable (no joints break after loading)")
	DirAccess.remove_absolute(save_manager.get_slot_path("ragdoll_test"))
	_world.clear_entities()


func _test_v1_save_compatibility() -> void :
	print("[v0.1 save compatibility]")
	var save_manager: = SaveManager.new()
	DirAccess.make_dir_recursive_absolute(SaveManager.SAVE_DIRECTORY)
	DirAccess.copy_absolute(V1_SAVE_FIXTURE, save_manager.get_slot_path("v1_fixture"))
	var result: = save_manager.load_from_slot("v1_fixture", _world, _world.camera, Content.registry)
	_check(result.ok and result.loaded_count == 3, "a version 0.1 save loads (%s)" % result.message)
	var box: = _world.get_entity_by_uid(2)
	_check(box != null and box.get_type_id() == "core:wooden_box" and box.is_frozen()
		and is_equal_approx(box.get_health().current_health, 60.0), "v0.1 entity state is preserved")
	_check(_world.get_next_uid() >= 4, "v0.1 uid counter is preserved")
	DirAccess.remove_absolute(save_manager.get_slot_path("v1_fixture"))
	_world.clear_entities()


func _test_many_ragdolls() -> void :
	print("[20 ragdolls]")
	for index in 20:
		_spawn_human(Vector2(-1900 + index * 200, -400 - (index % 3) * 300))
	_check(_world.get_assembly_count() == 20 and _world.get_entity_count() == 220, "20 humans = 220 bodies")
	var start: = Time.get_ticks_usec()
	await _frames(120 * 4)
	var milliseconds: = float(Time.get_ticks_usec() - start) / (120 * 4) / 1000.0
	print("       average frame time with 20 falling ragdolls: %.2f ms per physics tick" % milliseconds)
	var stable: = true
	var broken: = 0
	for human in _world.get_assemblies():
		stable = stable and _all_finite(human)
		broken += human.get_broken_joint_names().size()
	_check(stable, "all 20 ragdolls stay numerically stable")
	_check(broken == 0, "no limbs torn off by ordinary falls (%d broken joints)" % broken)
	_check(milliseconds < 8.0, "20 ragdolls simulate within budget (%.2f ms/tick, headless)" % milliseconds)
	_world.clear_entities()
