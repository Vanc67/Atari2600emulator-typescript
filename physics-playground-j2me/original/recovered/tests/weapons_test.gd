extends Node









const MAIN_SCENE: = preload("res://scenes/main.tscn")
const V1_SAVE: = "res://tests/fixtures/saves/v0_1_world.json"
const V2_SAVE: = "res://tests/fixtures/saves/v0_2_world.json"
const FIREARMS: Array[String] = ["core:pistol", "core:revolver", "core:assault_rifle", "core:shotgun", "core:smg"]
const MELEE: Array[String] = ["core:baseball_bat", "core:hammer", "core:crowbar", "core:metal_pipe"]
const EXPLOSIVES: Array[String] = ["core:explosive_barrel", "core:small_bomb", "core:large_bomb"]

var _checks: = 0
var _failures: PackedStringArray = []
var _main: Node
var _world: SandboxWorld
var _interaction: InteractionController


func _ready() -> void :
	_run.call_deferred()


func _run() -> void :
	seed(303)
	_main = MAIN_SCENE.instantiate()
	add_child(_main)
	_world = _main.world
	_interaction = _main.interaction
	await _frames(3)
	print("== Phase A: weapon architecture, pistol, projectiles, ragdoll damage")
	_test_registry()
	await _test_pistol_basics()
	await _test_projectile_ragdoll_damage()
	await _test_detached_limb_damage()
	print("== Phase B: firearms, recoil, ammunition")
	await _test_fire_modes_and_ammo()
	await _test_recoil()
	await _test_penetration_and_materials()
	await _test_ricochet_and_world()
	await _test_shotgun_range()
	print("== Phase C: explosions")
	await _test_explosion_basics()
	await _test_explosion_line_of_sight()
	await _test_chain_reaction()
	await _test_bombs_and_ragdolls()
	print("== Phase D: melee and destruction")
	await _test_melee()
	await _test_destructibles()
	print("== Phase E: fire, effects, audio")
	await _test_fire()
	await _test_effects_and_audio()
	print("== Phase F: tools, saves, performance")
	await _test_tools()
	await _test_save_load_v3()
	await _test_old_saves()
	await _test_performance()
	_main.queue_free()
	await _frames(1)

	print("")
	print("Weapons test: %d checks, %d failure(s)" % [_checks, _failures.size()])
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


func _frozen(type_id: String, at: Vector2, angle: = 0.0) -> SandboxEntity:
	var entity: = _world.spawn_entity(type_id, at, angle)
	entity.set_frozen(true)
	return entity


func _shoot(weapon: SandboxEntity) -> void :
	weapon.activate(true)
	weapon.activate(false)




func _damage_taken(candidate: Variant) -> float:
	if not is_instance_valid(candidate):
		return INF
	var health: = (candidate as SandboxEntity).get_health()
	return health.max_health - health.current_health


func _firearm(entity: SandboxEntity) -> FirearmComponent:
	return entity.get_component(&"firearm") as FirearmComponent


func _magazine(entity: SandboxEntity) -> MagazineComponent:
	return entity.get_component(&"magazine") as MagazineComponent


func _frozen_human(origin: Vector2) -> EntityAssembly:
	var human: = _world.spawn_assembly("core:human", origin)
	for part in human.get_parts():
		part.set_frozen(true)
	return human


func _gone(candidate: Variant) -> bool:
	return not is_instance_valid(candidate) or not (candidate as SandboxEntity).is_inside_tree()


func _reset() -> void :
	_world.clear_entities()
	await _frames(2)




func _test_registry() -> void :
	print("[registry]")
	var registry: = Content.registry
	_check(Content.load_errors.is_empty(), "all content loads without errors %s" % [Content.load_errors])
	for id in FIREARMS + MELEE + EXPLOSIVES + ["core:glass_panel"]:
		_check(registry.has_entity(id), "registry has %s" % id)
	for id in FIREARMS:
		var definition: = registry.get_entity_definition(id)
		_check(definition.components.has("firearm") and definition.components.has("magazine") and definition.category == "Firearms", 
			"%s is a data-defined firearm in the Firearms category" % id)
	for id in ["core:pistol_round", "core:revolver_round", "core:rifle_round", "core:smg_round", "core:buckshot"]:
		_check(registry.has_projectile(id), "projectile %s registered" % id)
	var categories: = registry.get_categories()
	_check(categories.has("Firearms") and categories.has("Melee") and categories.has("Explosives"), "new spawn menu categories %s" % [categories])
	_check( not categories.has("Debris"), "debris pieces are hidden from the spawn menu")
	_check(registry.get_material("core:glass") != null and registry.get_material("core:steel").ricochet_chance > 0.0, 
		"materials carry ballistic properties")


func _test_pistol_basics() -> void :
	print("[pistol]")
	var pistol: = _frozen("core:pistol", Vector2(0, -300))
	var target: = _frozen("core:steel_box", Vector2(400, -300))
	var magazine: = _magazine(pistol)
	_check(magazine.rounds == 12 and pistol.is_activatable(), "pistol spawns with a full 12-round magazine")
	var fired_before: int = _world.projectiles.stats["fired"]
	pistol.activate(true)
	_check(_world.projectiles.get_count() == 1 and magazine.rounds == 11, "activating fires one projectile and uses one round")
	await _frames(120)
	_check(_firearm(pistol).shots_fired == 1, "semi-automatic: holding the trigger fires only once")
	pistol.activate(false)
	_check(_world.projectiles.get_count() == 0, "projectiles are removed after hitting something")
	_check(_world.projectiles.stats["fired"] - fired_before == 1, "projectile statistics track shots")
	pistol.activate(true)
	pistol.activate(false)
	_check(_firearm(pistol).shots_fired == 2, "a new trigger press fires again after the cooldown")
	pistol.activate(true)
	pistol.activate(false)
	_check(_firearm(pistol).shots_fired == 2, "fire rate cooldown blocks an immediate extra shot")
	_check(is_instance_valid(target), "steel box survives pistol rounds")
	await _reset()


func _test_projectile_ragdoll_damage() -> void :
	print("[projectile ragdoll damage]")
	var human: = _frozen_human(Vector2(400, -110))
	var head: = human.get_part("head")
	var pistol: = _frozen("core:pistol", Vector2(head.global_position.x - 400, head.global_position.y + 3))
	_shoot(pistol)
	await _frames(10)
	var damage: = _damage_taken(head)
	_check(damage > 45.0 and damage < 52.0, "a pistol headshot deals 32 × 1.6 head multiplier (%.1f)" % damage)
	_check(human.get_part("upper_torso").get_health().current_health == 200.0, "only the struck limb is damaged")
	var hits_on_head: = 0
	await _frames(25)
	_shoot(pistol)
	await _frames(25)
	_shoot(pistol)
	await _frames(10)
	_check(head.get_health().is_depleted(), "three headshots destroy the head")
	_check(human.controller.get_state_name() == "dead", "a destroyed head kills (existing death rules)")
	_check(head.visual.damage_level == 1.0, "limb damage visuals update")
	await _reset()


func _test_detached_limb_damage() -> void :
	print("[detached limb]")
	var human: = _world.spawn_assembly("core:human", Vector2(400, -110))
	await _frames(2)
	human.break_joint_by_name("elbow_right")
	var forearm: = human.get_part("forearm_right")
	forearm.set_frozen(true)
	forearm.global_position = Vector2(400, -600)
	await _frames(2)
	var pistol: = _frozen("core:pistol", Vector2(forearm.global_position.x - 300, forearm.global_position.y))
	_shoot(pistol)
	await _frames(10)
	var damage: = _damage_taken(forearm)
	_check(damage > 25.0 and damage < 33.0, "a detached limb still takes projectile damage once (%.1f)" % damage)
	await _reset()




func _test_fire_modes_and_ammo() -> void :
	print("[fire modes + ammunition]")
	var y: = -600.0
	for id in FIREARMS:
		var gun: = _frozen(id, Vector2(0, y))
		_shoot(gun)
		_check(_firearm(gun).shots_fired == 1 and _magazine(gun).rounds == _magazine(gun).capacity - 1, "%s fires and uses ammunition" % id)
		y -= 60.0
	await _reset()

	var rifle: = _frozen("core:assault_rifle", Vector2(0, -600), -0.6)
	rifle.activate(true)
	await _frames(120)
	rifle.activate(false)
	var rifle_shots: = _firearm(rifle).shots_fired
	_check(rifle_shots >= 10 and rifle_shots <= 12, "assault rifle fires automatically at ~11 rounds/s (%d in 1 s)" % rifle_shots)
	var smg: = _frozen("core:smg", Vector2(0, -700), -0.6)
	smg.activate(true)
	await _frames(120)
	smg.activate(false)
	var smg_shots: = _firearm(smg).shots_fired
	_check(smg_shots >= 14 and smg_shots <= 16, "SMG fires faster, ~15 rounds/s (%d in 1 s)" % smg_shots)

	var shotgun: = _frozen("core:shotgun", Vector2(0, -800), -0.6)
	var before: = _world.projectiles.get_count()
	_shoot(shotgun)
	_check(_world.projectiles.get_count() - before == 9, "a shotgun shell fires 9 pellets")
	_check(_magazine(shotgun).rounds == 5, "shotgun holds 6 shells")

	var pistol: = _frozen("core:pistol", Vector2(0, -900), -0.6)
	_magazine(pistol).rounds = 1
	_shoot(pistol)
	await _frames(30)
	var count_before: = _world.projectiles.get_count()
	_shoot(pistol)
	_check(_magazine(pistol).rounds == 0 and _world.projectiles.get_count() == count_before, "an empty weapon dry-fires (no projectile)")
	pistol.reload()
	_check(_magazine(pistol).is_reloading, "reloading starts")
	await _frames(30)
	_shoot(pistol)
	_check(_firearm(pistol).shots_fired == 1, "weapons cannot fire mid-reload")
	await _frames(120 * 2)
	_check(_magazine(pistol).rounds == 12 and not _magazine(pistol).is_reloading, "a magazine reload refills all 12 rounds")

	var revolver: = _frozen("core:revolver", Vector2(0, -1000), -0.6)
	_magazine(revolver).rounds = 0
	revolver.reload()
	await _frames(70)
	_check(_magazine(revolver).rounds == 1, "the revolver reloads one round at a time (%d after 0.58 s)" % _magazine(revolver).rounds)
	await _frames(120 * 3)
	_check(_magazine(revolver).rounds == 6, "the revolver cylinder fills to 6")
	await _reset()


func _test_recoil() -> void :
	print("[recoil]")
	for id in FIREARMS:
		var gun: = _world.spawn_entity(id, Vector2(0, -2000))
		await _frames(1)
		var before: = gun.linear_velocity
		_shoot(gun)
		await _frames(1)
		var kick: = gun.linear_velocity - before - Vector2(0, 980.0 / 120.0)
		_check(kick.x < -20.0, "%s recoils backwards as a physics impulse (%.0f px/s)" % [id, kick.x])
		_check(gun.linear_velocity.length() <= FirearmComponent.MAX_RECOIL_SPEED * 2.0 + 1.0
			and absf(gun.angular_velocity) <= FirearmComponent.MAX_RECOIL_SPIN + 0.01, 
			"%s recoil stays within the stability caps" % id)
		_world.remove_entity(gun)
	var revolver: = _world.spawn_entity("core:revolver", Vector2(0, -2000))
	var pistol: = _world.spawn_entity("core:pistol", Vector2(300, -2000))
	await _frames(1)
	_shoot(revolver)
	_shoot(pistol)
	await _frames(1)
	_check(absf(revolver.linear_velocity.x) > absf(pistol.linear_velocity.x), "the revolver kicks harder than the pistol")
	await _reset()


func _test_penetration_and_materials() -> void :
	print("[penetration + materials]")
	var plank: = _frozen("core:wooden_plank", Vector2(200, -400), PI * 0.5)
	var behind_plank: = _frozen("core:wooden_box", Vector2(400, -400))
	_shoot(_frozen("core:pistol", Vector2(0, -397)))
	await _frames(10)
	_check(_damage_taken(plank) > 0.0 and _damage_taken(behind_plank) > 0.0, "a pistol round passes through a wooden plank and hits the box behind")
	_check(_damage_taken(behind_plank) < _damage_taken(plank), "the round loses energy passing through wood")

	var steel: = _frozen("core:steel_box", Vector2(200, -700))
	var behind_steel: = _frozen("core:wooden_box", Vector2(400, -700))
	_shoot(_frozen("core:pistol", Vector2(0, -697)))
	await _frames(10)
	_check(_damage_taken(behind_steel) == 0.0, "a steel box stops pistol rounds")
	var rifle: = _frozen("core:assault_rifle", Vector2(-50, -996))
	var thin: = _frozen("core:metal_barrel", Vector2(200, -1000))
	var behind_barrel: = _frozen("core:wooden_box", Vector2(400, -1000))
	_shoot(rifle)
	await _frames(10)
	_check(_damage_taken(thin) > 0.0, "rifle rounds damage a metal barrel (reduced by its 0.6 projectile multiplier)")

	var glass: = _frozen("core:glass_panel", Vector2(200, -1300))
	var behind_glass: = _frozen("core:wooden_box", Vector2(400, -1300))
	var debris_before: = _world.get_debris_count()
	_shoot(_frozen("core:pistol", Vector2(0, -1297)))
	await _frames(10)
	_check(_gone(glass), "a bullet shatters a glass panel")
	_check(_world.get_debris_count() > debris_before, "shattered glass spawns shards")
	_check(_damage_taken(behind_glass) > 25.0, "the bullet continues through glass almost unslowed (%.1f)" % _damage_taken(behind_glass))

	var box: = _world.spawn_entity("core:wooden_box", Vector2(600, -1600))
	await _frames(1)
	_shoot(_frozen("core:revolver", Vector2(400, -1600)))
	await _frames(3)
	_check(box.linear_velocity.x > 10.0, "a hit pushes the target with a small impulse (%.0f px/s)" % box.linear_velocity.x)
	await _reset()


func _test_ricochet_and_world() -> void :
	print("[ricochets + world boundaries]")
	var rifle: = _frozen("core:assault_rifle", Vector2(0, -40), 0.08)
	_magazine(rifle).infinite = true
	var stats: = _world.projectiles.stats
	var ricochets_before: int = stats["ricochets"]
	var stopped_before: int = stats["stopped"]
	rifle.activate(true)
	await _frames(120 * 3)
	rifle.activate(false)
	_check(int(stats["ricochets"]) - ricochets_before > 0, "grazing shots ricochet off the concrete floor (%d)" % (int(stats["ricochets"]) - ricochets_before))
	_check(int(stats["stopped"]) - stopped_before > 0, "projectiles stop in world geometry")
	var sky: = _frozen("core:pistol", Vector2(0, -400), - PI * 0.5)
	_shoot(sky)
	await _frames(int(120 * 1.6))
	_check(_world.projectiles.get_count() == 0, "projectiles that hit nothing expire after their lifetime")
	await _reset()


func _test_shotgun_range() -> void :
	print("[shotgun range]")
	var near: = _frozen("core:wooden_box", Vector2(150, -400))
	var near_gun: = _frozen("core:shotgun", Vector2(-60, -400))
	near.get_health().invulnerable = false
	_shoot(near_gun)
	await _frames(20)
	var near_damage: = _damage_taken(near)
	await _reset()
	var far: = _frozen("core:wooden_box", Vector2(1500, -400))
	_shoot(_frozen("core:shotgun", Vector2(-60, -400)))
	await _frames(60)
	var far_damage: = _damage_taken(far)
	_check(near_damage > 2.5 * far_damage, "the shotgun is far stronger up close (%.0f near vs %.0f at 15 m)" % [near_damage, far_damage])
	await _reset()




func _test_explosion_basics() -> void :
	print("[explosions]")
	var near: = _world.spawn_entity("core:rubber_ball", Vector2(80, -100))
	var far: = _world.spawn_entity("core:rubber_ball", Vector2(200, -100))
	await _frames(1)
	var count_before: = _world.explosions.explosion_count
	SandboxTools.explode_at(_world, Vector2(0, -100))
	await _frames(2)
	_check(_world.explosions.explosion_count == count_before + 1, "an explosion is processed")
	_check(near.linear_velocity.x > far.linear_velocity.x and far.linear_velocity.x > 0.0, "force pushes objects away and falls off with distance")
	_check(_damage_taken(near) > _damage_taken(far) and _damage_taken(far) > 0.0, "damage falls off with distance (%.0f vs %.0f)" % [_damage_taken(near), _damage_taken(far)])
	_check(near.linear_velocity.length() <= ExplosionSystem.MAX_SPEED_CHANGE + 50.0, "explosion speed change is capped (%.0f px/s)" % near.linear_velocity.length())
	await _reset()


func _test_explosion_line_of_sight() -> void :
	print("[line of sight]")
	_frozen("core:concrete_block", Vector2(-100, -32))
	var open: = _world.spawn_entity("core:rubber_ball", Vector2(190, -40))
	var shielded: = _world.spawn_entity("core:rubber_ball", Vector2(-190, -40))
	await _frames(1)
	SandboxTools.explode_at(_world, Vector2(0, -40))
	await _frames(2)
	_check(_damage_taken(shielded) < _damage_taken(open) * 0.6, "a concrete block shields from the blast (%.0f vs %.0f)" % [_damage_taken(shielded), _damage_taken(open)])
	await _reset()


func _test_chain_reaction() -> void :
	print("[chain reaction]")
	var barrels: Array[SandboxEntity] = []
	for index in 3:
		barrels.append(_world.spawn_entity("core:explosive_barrel", Vector2(index * 160.0, -42)))
	await _frames(5)
	var before: = _world.explosions.explosion_count
	barrels[0].activate(true)
	_check((barrels[0].get_component(&"explosive") as ExplosiveComponent).is_armed, "activating an explosive barrel lights its fuse")
	await _frames(120 * 3)
	_check(barrels.all( func(barrel: Variant) -> bool: return _gone(barrel)), "one barrel sets off the whole row")
	_check(_world.explosions.explosion_count - before == 3, "each explosive detonates exactly once (%d explosions)" % (_world.explosions.explosion_count - before))
	_check(_world.explosions.get_queue_size() == 0, "no explosions left queued (no infinite loop)")
	var shot_barrel: = _world.spawn_entity("core:explosive_barrel", Vector2(400, -42))
	var gun: = _frozen("core:revolver", Vector2(0, -50))
	for index in 3:
		_shoot(gun)
		await _frames(50)
	await _frames(60)
	_check(_gone(shot_barrel), "shooting an explosive barrel makes it explode")
	await _reset()


func _test_bombs_and_ragdolls() -> void :
	print("[bombs + ragdolls]")
	var bomb: = _world.spawn_entity("core:small_bomb", Vector2(0, -14))
	var before: = _world.explosions.explosion_count
	bomb.activate(true)
	await _frames(int(120 * 2.7))
	_check( not _gone(bomb) and bomb.visual.armed, "a small bomb waits for its 3 s fuse (armed indicator on)")
	await _frames(int(120 * 0.5))
	_check(_gone(bomb) and _world.explosions.explosion_count == before + 1, "the small bomb explodes when the fuse runs out")
	await _reset()
	var human: = _world.spawn_assembly("core:human", Vector2(0, -110))
	await _frames(5)
	var large: = _world.spawn_entity("core:large_bomb", Vector2(160, -20))
	(large.get_component(&"explosive") as ExplosiveComponent).arm(0.0)
	await _frames(120)
	var all_hurt: = true
	var finite: = true
	for part in human.get_parts():
		if _damage_taken(part) <= 0.0:
			all_hurt = false
		if not part.global_position.is_finite():
			finite = false
	_check(all_hurt, "a large bomb damages every limb of a nearby ragdoll")
	_check(human.controller.get_state_name() == "dead", "a point-blank large bomb kills")
	_check(finite, "the ragdoll stays numerically stable after the blast")
	await _reset()




func _test_melee() -> void :
	print("[melee]")
	for id in MELEE:
		var definition: = Content.registry.get_entity_definition(id)
		_check(definition.components.has("melee") and definition.category == "Melee", "%s is a physical melee weapon" % id)
	var sensor: = ImpactDamageComponent.new()
	sensor.configure({"min_impact_speed": 7.0, "damage_per_speed": 12.0}, null, null)
	_check(sensor.compute_damage(6.0) == 0.0 and sensor.compute_damage(6.0, 0.45, 2.2) > 0.0, 
		"melee lowers the impact threshold and multiplies damage")
	sensor.free()


	var human: = _world.spawn_assembly("core:human", Vector2(300, -110))
	await _frames(2)
	var head: = human.get_part("head")
	var hammer: = _world.spawn_entity("core:hammer", head.global_position + Vector2(-80, 0))
	hammer.linear_velocity = Vector2(1100, 0)
	await _frames(30)
	var swing_damage: = _damage_taken(head)
	_check(swing_damage > 0.0, "a swung hammer damages the head it hits (%.0f)" % swing_damage)
	await _frames(120 * 2)
	_check(_damage_taken(head) - swing_damage < 20.0, "no damage keeps accumulating after the hit")

	var crate: = _frozen("core:wooden_box", Vector2(-400, -32))
	var resting: = _world.spawn_entity("core:metal_pipe", Vector2(-400, -70))
	await _frames(120 * 3)
	_check(_damage_taken(crate) == 0.0 and is_instance_valid(resting), "a melee weapon resting on a crate causes no damage")
	await _reset()


func _test_destructibles() -> void :
	print("[destructibles]")
	var crate: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
	await _frames(2)
	crate.get_health().apply_damage(1000.0)
	await _frames(3)
	_check(_gone(crate) and _world.get_debris_count() == 4, "a destroyed crate breaks into 4 wood chunks")
	var piece: SandboxEntity = null
	for entity in _world.get_entities():
		if entity.is_transient():
			piece = entity
	_check(piece != null and piece.collision_layer == CollisionLayers.DEBRIS and piece.collision_mask == CollisionLayers.WORLD, 
		"debris only collides with the world")
	await _frames(120 * 7)
	_check(_world.get_debris_count() == 0, "debris removes itself after its lifetime")
	for index in 25:
		var box: = _world.spawn_entity("core:wooden_box", Vector2(-1200 + index * 90.0, -32))
		box.get_health().apply_damage(1000.0)
	await _frames(5)
	_check(_world.get_debris_count() <= SandboxWorld.MAX_DEBRIS, "debris is capped (%d alive after 25 crates)" % _world.get_debris_count())
	var barrel: = _world.spawn_entity("core:metal_barrel", Vector2(0, -42))
	await _frames(1)
	barrel.get_health().apply_damage(10000.0)
	await _frames(3)
	_check(_gone(barrel), "metal barrels are destructible")
	var plank: = _world.spawn_entity("core:wooden_plank", Vector2(300, -10))
	await _frames(1)
	plank.get_health().apply_damage(1000.0)
	await _frames(3)
	_check(_gone(plank), "wooden planks are destructible")
	await _reset()




func _test_fire() -> void :
	print("[fire]")
	for index in 6:
		_world.spawn_entity("core:wooden_box", Vector2(-1500 + index * 70.0, -32))
	_world.spawn_assembly("core:human", Vector2(-800, -110))
	await _frames(120 * 3)
	_check(_world.thermal.get_active_count() == 0, "cold props and resting humans cost nothing to the heat system")
	await _reset()

	var a: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
	var b: = _world.spawn_entity("core:wooden_box", Vector2(66, -32))
	var steel: = _world.spawn_entity("core:steel_box", Vector2(-80, -32))
	await _frames(5)
	_check(SandboxTools.ignite(a) and a.get_thermal().burning, "a wooden crate can be ignited")
	_check( not SandboxTools.ignite(steel), "steel cannot be ignited")
	_check(_world.effects.get_burning_emitter_count() == 1 and a.visual.burning, "burning shows fire effects")
	await _frames(120 * 6)
	_check(_damage_taken(a) > 20.0, "burning deals damage over time (%.0f)" % _damage_taken(a))
	_check(b.get_thermal().burning, "fire spreads to the neighbouring crate")
	_check(steel.get_thermal().temperature > 60.0, "heat transfers to nearby non-flammable objects (%.0f °C)" % steel.get_thermal().temperature)
	SandboxTools.extinguish(_world, b)
	_check( not b.get_thermal().burning, "extinguishing stops the fire")
	await _reset()

	var human: = _world.spawn_assembly("core:human", Vector2(0, -110))
	await _frames(120 * 2)
	SandboxTools.ignite(human.get_part("upper_torso"))
	await _frames(120 * 4)
	_check(_damage_taken(human.get_part("upper_torso")) > 10.0, "a burning human takes fire damage")
	SandboxTools.extinguish(_world, human.get_part("head"))
	_check(human.get_parts().all( func(part: SandboxEntity) -> bool: return not part.get_thermal().burning), 
		"extinguishing one limb puts out the whole body")
	await _reset()

	var barrel: = _world.spawn_entity("core:explosive_barrel", Vector2(0, -42))
	var fire_left: = _world.spawn_entity("core:wooden_box", Vector2(-62, -32))
	var fire_right: = _world.spawn_entity("core:wooden_box", Vector2(62, -32))
	await _frames(5)
	SandboxTools.ignite(fire_left)
	SandboxTools.ignite(fire_right)
	var exploded: = false
	for second in 25:
		await _frames(120)
		if _gone(barrel):
			exploded = true
			break
	_check(exploded, "an explosive barrel heated by fire detonates")
	await _reset()


func _test_effects_and_audio() -> void :
	print("[effects + audio]")
	await _frames(int(120 * 2.5))
	_check(_world.effects.get_particle_count() == 0, "effects layer starts empty")
	SandboxTools.explode_at(_world, Vector2(0, -200))
	await _frames(3)
	var particles: = _world.effects.get_particle_count()
	_check(particles > 20 and particles <= EffectsLayer.MAX_PARTICLES, "an explosion emits particles (%d)" % particles)
	for index in 400:
		_world.effects.burst("sparks", Vector2(0, -200), Vector2.UP, 2.0)
	_check(_world.effects.get_particle_count() <= EffectsLayer.MAX_PARTICLES, "particles are capped (%d)" % _world.effects.get_particle_count())
	await _frames(120 * 3)
	_check(_world.effects.get_particle_count() == 0 and _world.effects.get_flash_count() == 0, "effects clean themselves up")

	var all_built: = true
	for sound in Content.registry.get_sounds():
		if not Audio.has_stream(sound.id):
			all_built = false
	_check(all_built, "every sound is synthesized (%d sounds)" % Content.registry.get_sounds().size())
	_check(Audio.play("core:explosion", Vector2.ZERO), "sounds play through the audio manager")
	_check( not Audio.play("core:explosion", Vector2.ZERO), "rapid repeats of the same sound are rate-limited")
	_check( not Audio.play("core:does_not_exist", Vector2.ZERO), "unknown sound ids are ignored safely")
	await _reset()




func _test_tools() -> void :
	print("[tools]")
	var crate: = _world.spawn_entity("core:wooden_box", Vector2(0, -32))
	var steel: = _world.spawn_entity("core:steel_box", Vector2(200, -32))
	await _frames(5)
	_interaction.set_tool(SandboxTools.Tool.FREEZE)
	_interaction.apply_tool_at(crate.global_position)
	_check(crate.is_frozen(), "Freeze tool freezes")
	_interaction.apply_tool_at(crate.global_position)
	_check( not crate.is_frozen(), "Freeze tool unfreezes")
	_interaction.set_tool(SandboxTools.Tool.DAMAGE)
	_interaction.tool_damage_amount = 25.0
	_interaction.apply_tool_at(crate.global_position)
	_check(is_equal_approx(_damage_taken(crate), 25.0), "Damage tool deals the configured amount")
	_interaction.set_tool(SandboxTools.Tool.HEAL)
	_interaction.apply_tool_at(crate.global_position)
	_check(_damage_taken(crate) == 0.0, "Heal tool restores health")
	_interaction.set_tool(SandboxTools.Tool.IGNITE)
	_interaction.apply_tool_at(crate.global_position)
	_check(crate.get_thermal().burning, "Ignite tool sets objects on fire")
	_interaction.apply_tool_at(steel.global_position)
	_check( not steel.get_thermal().burning, "Ignite tool leaves non-flammable objects alone")
	_interaction.set_tool(SandboxTools.Tool.EXTINGUISH)
	_interaction.apply_tool_at(crate.global_position)
	_check( not crate.get_thermal().burning, "Extinguish tool puts out fires")
	_interaction.set_tool(SandboxTools.Tool.INSPECT)
	_interaction.select(steel)
	_check(_interaction.selected_entity == steel, "Inspect tool selects")
	_interaction.set_tool(SandboxTools.Tool.DELETE)
	_interaction.apply_tool_at(steel.global_position)
	await _frames(1)
	_check(_gone(steel), "Delete tool removes objects")
	var before: = _world.explosions.explosion_count
	_interaction.set_tool(SandboxTools.Tool.EXPLODE)
	_interaction.apply_tool_at(Vector2(600, -300))
	await _frames(2)
	_check(_world.explosions.explosion_count == before + 1, "Explode tool creates an explosion at the cursor")
	_interaction.set_tool(SandboxTools.Tool.DRAG)
	var pistol: = _world.spawn_entity("core:pistol", Vector2(0, -500))
	_interaction.select(pistol)
	_interaction.activate_selected(true)
	_interaction.activate_selected(false)
	_check(_firearm(pistol).shots_fired == 1, "the activation key fires the selected weapon")
	_interaction.reload_selected()
	_check(_magazine(pistol).is_reloading, "the reload key reloads the selected weapon")
	await _reset()


func _test_save_load_v3() -> void :
	print("[save / load v3]")
	var save_manager: = SaveManager.new()
	var pistol: = _frozen("core:pistol", Vector2(0, -400), 0.4)
	_magazine(pistol).rounds = 5
	var revolver: = _frozen("core:revolver", Vector2(100, -400))
	_magazine(revolver).rounds = 2
	revolver.reload()
	var bomb: = _world.spawn_entity("core:small_bomb", Vector2(-600, -14))
	var crate: = _world.spawn_entity("core:wooden_box", Vector2(400, -32))
	var barrel: = _world.spawn_entity("core:explosive_barrel", Vector2(-300, -42))
	var human: = _world.spawn_assembly("core:human", Vector2(800, -110))
	await _frames(10)
	bomb.activate(true)
	SandboxTools.ignite(crate)
	barrel.get_thermal().add_heat(80.0)
	human.get_part("head").get_health().apply_damage(40.0)
	var debris_crate: = _world.spawn_entity("core:wooden_box", Vector2(-900, -32))
	await _frames(2)
	debris_crate.get_health().apply_damage(1000.0)
	await _frames(int(120 * 0.5))
	var uids: = {"pistol": pistol.entity_uid, "revolver": revolver.entity_uid, "bomb": bomb.entity_uid, 
		"crate": crate.entity_uid, "barrel": barrel.entity_uid}
	var barrel_temperature: = barrel.get_thermal().temperature
	var fuse_left: = (bomb.get_component(&"explosive") as ExplosiveComponent).fuse_remaining
	_check(save_manager.save_to_slot("weapons_test", _world, _world.camera, Content.registry) == OK, "save succeeds")
	var text: = FileAccess.get_file_as_string(save_manager.get_slot_path("weapons_test"))
	_check(text.contains("\"format_version\": 3"), "saves use format version 3")
	_check( not text.contains("core:wood_chunk"), "debris is not saved")

	_world.clear_entities()
	var result: = save_manager.load_from_slot("weapons_test", _world, _world.camera, Content.registry)
	_check(result.ok, "save loads (%s)" % result.message)
	var loaded_pistol: = _world.get_entity_by_uid(uids["pistol"])
	var loaded_revolver: = _world.get_entity_by_uid(uids["revolver"])
	var loaded_bomb: = _world.get_entity_by_uid(uids["bomb"])
	var loaded_crate: = _world.get_entity_by_uid(uids["crate"])
	var loaded_barrel: = _world.get_entity_by_uid(uids["barrel"])
	_check(loaded_pistol != null and _magazine(loaded_pistol).rounds == 5 and absf(loaded_pistol.global_rotation - 0.4) < 0.001, 
		"weapon type, rotation and ammunition are restored")
	_check(loaded_revolver != null and _magazine(loaded_revolver).is_reloading, "an in-progress reload is restored")
	var loaded_fuse: = (loaded_bomb.get_component(&"explosive") as ExplosiveComponent) if loaded_bomb else null
	_check(loaded_fuse != null and loaded_fuse.is_armed and absf(loaded_fuse.fuse_remaining - fuse_left) < 0.05, 
		"an armed bomb keeps its fuse (activation state)")
	_check(loaded_crate != null and loaded_crate.get_thermal().burning and _damage_taken(loaded_crate) > 0.0, 
		"burning state and health are restored")
	_check(loaded_barrel != null and absf(loaded_barrel.get_thermal().temperature - barrel_temperature) < 1.0, 
		"temperature is restored (%.0f °C)" % (loaded_barrel.get_thermal().temperature if loaded_barrel else 0.0))
	_check(_world.thermal.get_active_count() > 0, "restored fires keep simulating")
	await _frames(int(120 * (fuse_left + 0.5)))
	_check(_gone(loaded_bomb), "the restored bomb still explodes")
	DirAccess.remove_absolute(save_manager.get_slot_path("weapons_test"))
	await _reset()


func _test_old_saves() -> void :
	print("[v0.1 / v0.2 saves]")
	var save_manager: = SaveManager.new()
	DirAccess.make_dir_recursive_absolute(SaveManager.SAVE_DIRECTORY)
	for fixture: String in [V1_SAVE, V2_SAVE]:
		DirAccess.copy_absolute(fixture, save_manager.get_slot_path("compat"))
		var result: = save_manager.load_from_slot("compat", _world, _world.camera, Content.registry)
		_check(result.ok and result.skipped_types.is_empty(), "%s loads (%s)" % [fixture.get_file(), result.message])
		if fixture == V2_SAVE:
			var human: = _world.get_assemblies()[0] if _world.get_assembly_count() == 1 else null
			_check(human != null and human.get_broken_joint_names() == PackedStringArray(["elbow_right"])
				and human.get_part("thigh_left").get_health().is_damaged(), "a real v0.2 ragdoll save restores joints and limb health")
			await _frames(120)
			_check(human != null and human.get_broken_joint_names().size() == 1, "the restored v0.2 ragdoll is stable")
		_world.clear_entities()
		DirAccess.remove_absolute(save_manager.get_slot_path("compat"))
		await _frames(2)


func _test_performance() -> void :
	print("[performance: 20 ragdolls, 100 props, 4 firearms, explosions]")
	for index in 20:
		_world.spawn_assembly("core:human", Vector2(-2400 + index * 240.0, -300))
	var props: Array[String] = ["core:wooden_box", "core:steel_box", "core:metal_barrel", "core:rubber_ball", "core:wooden_plank"]
	for index in 100:
		_world.spawn_entity(props[index % props.size()], Vector2(-2500 + (index % 25) * 200.0, -700 - (index / 25) * 120.0))
	var guns: Array[SandboxEntity] = []
	for index in 4:
		var gun: = _frozen("core:assault_rifle", Vector2(-2600, -150 - index * 60.0), 0.03)
		_magazine(gun).infinite = true
		gun.activate(true)
		guns.append(gun)
	await _frames(120 * 2)
	var worst: = 0.0
	var total: = 0.0
	var ticks: = 120 * 5
	var last: = Time.get_ticks_usec()
	for index in ticks:
		if index % 180 == 0:
			SandboxTools.explode_at(_world, Vector2(randf_range(-2000, 2000), -100))
		await get_tree().physics_frame
		var now: = Time.get_ticks_usec()
		var milliseconds: = float(now - last) / 1000.0
		last = now
		total += milliseconds
		worst = maxf(worst, milliseconds)
	for gun in guns:
		gun.activate(false)
	var average: = total / ticks
	print("       bodies=%d, projectiles in flight=%d, particles=%d, burning=%d" % [_world.get_entity_count(), 
		_world.projectiles.get_count(), _world.effects.get_particle_count(), _world.thermal.get_burning_count()])
	print("       average %.2f ms per physics tick, worst %.2f ms (headless, includes script and physics)" % [average, worst])




	print("       (timing is informational; see tests/benchmark.tscn for throughput benchmarks)")
	var stable: = true
	for entity in _world.get_entities():
		if not entity.global_position.is_finite():
			stable = false
	_check(stable, "the whole scene stays numerically stable")
	await _reset()
