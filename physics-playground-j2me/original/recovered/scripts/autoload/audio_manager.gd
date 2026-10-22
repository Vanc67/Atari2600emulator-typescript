extends Node








const VOICE_COUNT: = 24
const MAX_DISTANCE: = 4000.0

const QUIT_SETTLE_MSEC: = 300

var plays_started: = 0
var plays_skipped: = 0

var _voices: Array[AudioStreamPlayer2D] = []
var _voice_sound: Array[String] = []
var _voice_started: = PackedInt64Array()
var _streams: Dictionary = {}
var _last_played: Dictionary = {}


func _ready() -> void :
	process_mode = Node.PROCESS_MODE_ALWAYS

	get_tree().auto_accept_quit = false
	for index in VOICE_COUNT:
		var voice: = AudioStreamPlayer2D.new()
		voice.name = "Voice%d" % index
		voice.max_distance = MAX_DISTANCE
		voice.attenuation = 1.0
		add_child(voice)
		_voices.append(voice)
		_voice_sound.append("")
	_voice_started.resize(VOICE_COUNT)
	_voice_started.fill(0)
	_prepare_streams.call_deferred()





func _notification(what: int) -> void :
	if what == NOTIFICATION_WM_CLOSE_REQUEST:
		_quit_cleanly()


func _quit_cleanly() -> void :
	await prepare_for_quit()
	get_tree().quit()


func play(sound_id: String, at_position: Vector2, volume_offset_db: = 0.0) -> bool:
	if sound_id.is_empty():
		return false
	var sound: = Content.registry.get_sound(sound_id)
	if sound == null:
		return false
	var now: = Time.get_ticks_msec()
	if now - int(_last_played.get(sound_id, -100000)) < int(sound.min_interval * 1000.0):
		plays_skipped += 1
		return false
	var voice_index: = _pick_voice(sound)
	if voice_index < 0:
		plays_skipped += 1
		return false
	_last_played[sound_id] = now
	var voice: = _voices[voice_index]
	voice.stream = _get_stream(sound)
	voice.global_position = at_position
	voice.volume_db = sound.volume_db + volume_offset_db
	voice.pitch_scale = maxf(sound.pitch * (1.0 + randf_range( - sound.pitch_variation, sound.pitch_variation)), 0.05)
	voice.play()
	_voice_sound[voice_index] = sound_id


	_voice_started[voice_index] = Time.get_ticks_usec()
	plays_started += 1
	return true


func get_active_voice_count() -> int:
	var count: = 0
	for voice in _voices:
		if voice.playing:
			count += 1
	return count


func has_stream(sound_id: String) -> bool:
	return _streams.has(sound_id)


func stop_all() -> void :
	for voice in _voices:
		voice.stop()











func prepare_for_quit() -> void :
	stop_all()
	for voice in _voices:
		voice.stream = null
	var start: = Time.get_ticks_msec()
	while Time.get_ticks_msec() - start < QUIT_SETTLE_MSEC:
		await get_tree().process_frame



func _exit_tree() -> void :
	for voice in _voices:
		voice.stop()
		voice.stream = null
	_streams.clear()








func _pick_voice(sound: SoundDefinition) -> int:
	var same_count: = 0
	var oldest_same: = -1
	var oldest_any: = -1
	var idle: = -1
	for index in VOICE_COUNT:
		if not _voices[index].playing:
			if idle < 0:
				idle = index
			continue
		if _voice_sound[index] == sound.id:
			same_count += 1
			if oldest_same < 0 or _voice_started[index] < _voice_started[oldest_same]:
				oldest_same = index
		if oldest_any < 0 or _voice_started[index] < _voice_started[oldest_any]:
			oldest_any = index
	if same_count >= maxi(sound.max_instances, 1):
		return oldest_same
	if idle >= 0:
		return idle
	return oldest_any



func get_active_instances(sound_id: String) -> int:
	var count: = 0
	for index in VOICE_COUNT:
		if _voices[index].playing and _voice_sound[index] == sound_id:
			count += 1
	return count


func _get_stream(sound: SoundDefinition) -> AudioStreamWAV:
	if not _streams.has(sound.id):
		_streams[sound.id] = SoundSynth.build(sound)
	return _streams[sound.id]


func _prepare_streams() -> void :
	for sound in Content.registry.get_sounds():
		_get_stream(sound)
