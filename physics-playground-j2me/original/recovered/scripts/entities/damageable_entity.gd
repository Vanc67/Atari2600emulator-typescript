class_name DamageableEntity
extends SandboxEntity











const MAX_REPORTED_CONTACTS: = 6

var impact_sensor: ImpactDamageComponent

var _previous_linear_velocity: = Vector2.ZERO
var _previous_angular_velocity: = 0.0
var _last_linear_change: = Vector2.ZERO
var _last_angular_change: = 0.0


func add_component(component: EntityComponent) -> void :
	super (component)
	if component is ImpactDamageComponent:
		impact_sensor = component
		contact_monitor = true
		max_contacts_reported = MAX_REPORTED_CONTACTS




func reset_impact_history() -> void :
	_previous_linear_velocity = linear_velocity
	_previous_angular_velocity = angular_velocity
	_last_linear_change = Vector2.ZERO
	_last_angular_change = 0.0


func _integrate_forces(state: PhysicsDirectBodyState2D) -> void :
	var linear: = state.linear_velocity
	var angular: = state.angular_velocity
	var linear_change: = linear - _previous_linear_velocity - state.total_gravity * state.step
	var angular_change: = angular - _previous_angular_velocity
	if impact_sensor and state.get_contact_count() > 0:
		impact_sensor.evaluate_contacts(state, linear_change, angular_change, 
			_last_linear_change, _last_angular_change)
	_previous_linear_velocity = linear
	_previous_angular_velocity = angular
	_last_linear_change = linear_change
	_last_angular_change = angular_change
