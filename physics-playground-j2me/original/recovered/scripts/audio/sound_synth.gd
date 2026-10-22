class_name SoundSynth
extends RefCounted







const MIX_RATE: = 22050
const PRESETS: PackedStringArray = [
	"gunshot", "explosion", "impact_metal", "impact_wood", "impact_flesh", "impact_generic", 
	"glass_break", "reload", "click", "ricochet", "fire_crackle", "beep", "swoosh", 
]


static func has_preset(preset: String) -> bool:
	return PRESETS.has(preset)




static func build(sound: SoundDefinition) -> AudioStreamWAV:
	var rng: = RandomNumberGenerator.new()
	rng.seed = hash(sound.id)
	var count: = maxi(int(sound.duration * MIX_RATE), 64)
	var samples: = PackedFloat32Array()
	samples.resize(count)
	samples.fill(0.0)
	var brightness: = float(sound.params.get("brightness", 1.0))
	match sound.synth:
		"gunshot":
			_noise_burst(samples, rng, 0.0, 0.035 * sound.duration / 0.35, 1.0, 0.55 * brightness)
			_tone(samples, 0.0, 95.0, 0.06, 0.8)
			_noise_burst(samples, rng, 0.0, 0.18 * sound.duration / 0.35, 0.35, 0.12)
		"explosion":
			_noise_burst(samples, rng, 0.0, sound.duration * 0.35, 1.0, 0.06 * brightness)
			_tone(samples, 0.0, 42.0, sound.duration * 0.4, 0.9)
			_noise_burst(samples, rng, 0.0, 0.05, 0.6, 0.5)
		"impact_metal":
			for partial: Array in [[523.0, 0.5], [1307.0, 0.3], [2210.0, 0.22], [3150.0, 0.12]]:
				_tone(samples, 0.0, partial[0] * brightness, sound.duration * 0.35, partial[1])
			_noise_burst(samples, rng, 0.0, 0.01, 0.5, 0.7)
		"impact_wood":
			_noise_burst(samples, rng, 0.0, 0.03, 0.8, 0.25 * brightness)
			_tone(samples, 0.0, 190.0, 0.05, 0.6)
			_tone(samples, 0.0, 420.0, 0.03, 0.25)
		"impact_flesh":
			_noise_burst(samples, rng, 0.0, 0.05, 0.7, 0.08)
			_tone(samples, 0.0, 85.0, 0.07, 0.6)
		"impact_generic":
			_noise_burst(samples, rng, 0.0, 0.04, 0.8, 0.2 * brightness)
		"glass_break":
			_noise_burst(samples, rng, 0.0, 0.03, 0.8, 0.9)
			for index in 14:
				var start: = rng.randf_range(0.0, sound.duration * 0.6)
				_tone(samples, start, rng.randf_range(2500.0, 6500.0), rng.randf_range(0.03, 0.12), rng.randf_range(0.15, 0.4))
		"reload":
			_noise_burst(samples, rng, 0.0, 0.012, 0.9, 0.8)
			_tone(samples, 0.0, 1800.0, 0.02, 0.3)
			_noise_burst(samples, rng, sound.duration * 0.55, 0.015, 1.0, 0.7)
			_tone(samples, sound.duration * 0.55, 1300.0, 0.03, 0.35)
		"click":
			_noise_burst(samples, rng, 0.0, 0.008, 1.0, 0.85)
			_tone(samples, 0.0, 2400.0, 0.012, 0.3)
		"ricochet":
			_sweep(samples, 3600.0 * brightness, 1100.0, sound.duration, 0.45)
			_noise_burst(samples, rng, 0.0, 0.01, 0.6, 0.8)
		"fire_crackle":
			for index in 18:
				_noise_burst(samples, rng, rng.randf_range(0.0, sound.duration * 0.95), rng.randf_range(0.003, 0.012), 
					rng.randf_range(0.2, 0.7), 0.6)
			_noise_burst(samples, rng, 0.0, sound.duration * 0.6, 0.12, 0.04)
		"beep":
			_tone(samples, 0.0, 880.0 * brightness, sound.duration, 0.5, false)
		"swoosh":
			_noise_burst(samples, rng, 0.0, sound.duration * 0.5, 0.5, 0.15, true)
	return _to_stream(samples)






static func _noise_burst(samples: PackedFloat32Array, rng: RandomNumberGenerator, start: float, 
		decay: float, amplitude: float, cutoff: float, swell: = false) -> void :
	var first: = int(start * MIX_RATE)
	var filtered: = 0.0
	var coefficient: = clampf(cutoff, 0.001, 1.0)
	for index in range(first, samples.size()):
		var t: = float(index - first) / MIX_RATE
		var envelope: = exp( - t / maxf(decay, 0.0005))
		if swell:
			envelope *= minf(t / maxf(decay * 0.5, 0.0005), 1.0)
		if envelope < 0.001:
			break
		filtered += (rng.randf_range(-1.0, 1.0) - filtered) * coefficient
		samples[index] += filtered * envelope * amplitude



static func _tone(samples: PackedFloat32Array, start: float, frequency: float, decay: float, 
		amplitude: float, decaying: = true) -> void :
	var first: = int(start * MIX_RATE)
	for index in range(first, samples.size()):
		var t: = float(index - first) / MIX_RATE
		var envelope: = exp( - t / maxf(decay, 0.0005)) if decaying else minf(minf(t, decay - t) * 200.0, 1.0)
		if decaying and envelope < 0.001:
			break
		if not decaying and t > decay:
			break
		samples[index] += sin(TAU * frequency * t) * envelope * amplitude


static func _sweep(samples: PackedFloat32Array, from_frequency: float, to_frequency: float, 
		duration: float, amplitude: float) -> void :
	var phase: = 0.0
	for index in mini(int(duration * MIX_RATE), samples.size()):
		var t: = float(index) / MIX_RATE
		var frequency: = lerpf(from_frequency, to_frequency, t / duration)
		phase += TAU * frequency / MIX_RATE
		samples[index] += sin(phase) * exp( - t / (duration * 0.4)) * amplitude


static func _to_stream(samples: PackedFloat32Array) -> AudioStreamWAV:
	var peak: = 0.0
	for value in samples:
		peak = maxf(peak, absf(value))
	var gain: = 0.9 / peak if peak > 0.0 else 1.0
	var bytes: = PackedByteArray()
	bytes.resize(samples.size() * 2)
	for index in samples.size():
		bytes.encode_s16(index * 2, clampi(roundi(samples[index] * gain * 32767.0), -32768, 32767))
	var stream: = AudioStreamWAV.new()
	stream.format = AudioStreamWAV.FORMAT_16_BITS
	stream.mix_rate = MIX_RATE
	stream.stereo = false
	stream.data = bytes
	return stream
