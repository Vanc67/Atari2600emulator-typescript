class_name SoundDefinition
extends RefCounted






var id: String
var source_mod: String

var synth: = "click"
var duration: = 0.3
var pitch: = 1.0

var pitch_variation: = 0.06
var volume_db: = 0.0

var max_instances: = 4

var min_interval: = 0.03

var params: Dictionary = {}


static func from_dict(data: Dictionary, mod_id: String, reader: DataReader) -> SoundDefinition:
	var sound: = SoundDefinition.new()
	var local_id: = reader.read_string(data, "id", "", true)
	if not local_id.is_empty() and not NamespacedId.is_valid_segment(local_id):
		reader.error("id '%s' may only contain lowercase letters, digits and underscores" % local_id)
	sound.id = NamespacedId.make(mod_id, local_id)
	sound.source_mod = mod_id
	sound.synth = reader.read_string(data, "synth", "click")
	if not SoundSynth.has_preset(sound.synth):
		reader.error("unknown synth preset '%s'" % sound.synth)
	sound.duration = reader.read_float(data, "duration", 0.3, 0.02, 4.0)
	sound.pitch = reader.read_float(data, "pitch", 1.0, 0.1, 8.0)
	sound.pitch_variation = reader.read_float(data, "pitch_variation", 0.06, 0.0, 1.0)
	sound.volume_db = reader.read_float(data, "volume_db", 0.0, -60.0, 12.0)
	sound.max_instances = reader.read_int(data, "max_instances", 4)
	sound.min_interval = reader.read_float(data, "min_interval", 0.03, 0.0, 5.0)
	sound.params = reader.read_dictionary(data, "params").duplicate(true)
	return sound
