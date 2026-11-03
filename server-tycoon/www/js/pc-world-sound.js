/* Additive PC audio: исходные fan/coil/HDD остаются, поверх — БП, GPU, память и воздух. */
(function () {
  'use strict';
  const Sound = window.PCSoundscape;
  if (!Sound || Sound.prototype.worldAudioLoaded) return;
  const proto = Sound.prototype;
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const target = (param, value, now, time = .25) => { if (param && param.setTargetAtTime) param.setTargetAtTime(value, now, time); else if (param) param.value = value; };
  function addWorldNodes(sound, machine) {
    const context = sound.audio.context; const nodes = sound.nodes; if (!context || !nodes || nodes.worldSources) return;
    const seed = hash(machine.id); const bus = nodes.bus; const worldSources = [];
    const psu = context.createOscillator(); psu.type = 'sine'; psu.frequency.value = 98 + seed % 9;
    const psuGain = context.createGain(); psuGain.gain.value = .0015; psu.connect(psuGain); psuGain.connect(bus); psu.start(); worldSources.push(psu);
    const gpu = context.createOscillator(); gpu.type = 'triangle'; gpu.frequency.value = 620 + seed % 170;
    const gpuFilter = context.createBiquadFilter(); gpuFilter.type = 'lowpass'; gpuFilter.frequency.value = 2400;
    const gpuGain = context.createGain(); gpuGain.gain.value = .00001; gpu.connect(gpuFilter); gpuFilter.connect(gpuGain); gpuGain.connect(bus); gpu.start(); worldSources.push(gpu);
    const memory = context.createOscillator(); memory.type = 'sine'; memory.frequency.value = 47 + seed % 5;
    const memoryGain = context.createGain(); memoryGain.gain.value = .0007; memory.connect(memoryGain); memoryGain.connect(bus); memory.start(); worldSources.push(memory);
    const length = Math.max(1, Math.floor(context.sampleRate * 2)); const buffer = context.createBuffer(1, length, context.sampleRate); const data = buffer.getChannelData(0);
    let last = 0; let random = seed || 1; for (let i = 0; i < length; i += 1) { random = (Math.imul(random, 1664525) + 1013904223) >>> 0; const white = random / 4294967296 * 2 - 1; last = (last + .025 * white) / 1.025; data[i] = last * 3.2; }
    const air = context.createBufferSource(); air.buffer = buffer; air.loop = true; const airFilter = context.createBiquadFilter(); airFilter.type = 'bandpass'; airFilter.Q.value = .45;
    const airGain = context.createGain(); airGain.gain.value = .001; air.connect(airFilter); airFilter.connect(airGain); airGain.connect(bus); air.start(); worldSources.push(air);
    const lfo = context.createOscillator(); lfo.type = 'sine'; lfo.frequency.value = .32 + (seed % 7) * .035; const lfoGain = context.createGain(); lfoGain.gain.value = 1.8;
    lfo.connect(lfoGain); lfoGain.connect(psu.frequency); lfo.start(); worldSources.push(lfo);
    Object.assign(nodes, { worldSources, psu, psuGain, gpu, gpuGain, gpuFilter, memory, memoryGain, air, airFilter, airGain, lfo, lfoGain, worldSeed: seed });
  }
  const baseCreate = proto.create;
  proto.create = function (machine, metrics) {
    const result = baseCreate.call(this, machine, metrics); if (this.nodes && this.audio.context) { addWorldNodes(this, machine); this.sync(machine, metrics); } return result;
  };
  const baseSync = proto.sync;
  proto.sync = function (machine, metrics) {
    const result = baseSync.call(this, machine, metrics); const nodes = this.nodes; if (!result || !nodes || !nodes.worldSources) return result;
    const now = this.audio.context.currentTime; const plan = machine.osWorld && machine.osWorld.powerPlan || 'balanced'; const games = machine.osWorld ? Object.values(machine.osWorld.games || {}).filter(game => game.running).length : 0;
    const laptop = Boolean(machine.laptop); const load = Math.max(0, Math.min(100, metrics.cpuLoad || 0)); const gpuLoad = Math.max(0, Math.min(100, metrics.gpuLoad || 0));
    target(nodes.psu.frequency, 96 + nodes.worldSeed % 11 + load * .035, now, .5); target(nodes.psuGain.gain, (laptop ? .00045 : .001) + load * .000012, now);
    target(nodes.gpu.frequency, 520 + nodes.worldSeed % 210 + gpuLoad * 7 + (plan === 'performance' ? 90 : 0), now, .3);
    target(nodes.gpuFilter.frequency, 1500 + gpuLoad * 18, now, .4); target(nodes.gpuGain.gain, .00001 + Math.max(0, gpuLoad - 12) * .000018 + games * .00035, now, .35);
    target(nodes.memory.frequency, 45 + nodes.worldSeed % 7 + (metrics.ramUsedMb && metrics.ramMb ? metrics.ramUsedMb / metrics.ramMb * 8 : 0), now, .7);
    target(nodes.memoryGain.gain, .00035 + load * .000004, now, .6); target(nodes.airFilter.frequency, machine.storageType === 'HDD' ? 1150 + load * 7 : 1850 + load * 9, now, .45);
    target(nodes.airGain.gain, (laptop ? .00035 : .0006) + Math.max(0, metrics.noiseDb || metrics.noise || 0) * .000022 + (plan === 'eco' ? -.00015 : 0), now, .5);
    return result;
  };
  const baseClick = proto.click;
  proto.click = function (machine, loud = false) {
    baseClick.call(this, machine, loud); if (!this.nodes || !this.audio.context) return; const context = this.audio.context; const now = context.currentTime;
    const head = context.createOscillator(); const gain = context.createGain(); head.type = 'sine'; head.frequency.setValueAtTime(machine.storageType === 'HDD' ? 92 : 250, now); head.frequency.exponentialRampToValueAtTime(48, now + .045);
    gain.gain.setValueAtTime(loud ? .009 : .0035, now); gain.gain.exponentialRampToValueAtTime(.0001, now + .05); head.connect(gain); gain.connect(this.nodes.bus); head.start(now); head.stop(now + .055);
  };
  const baseStop = proto.stop;
  proto.stop = function () {
    if (this.nodes && this.nodes.worldSources) this.nodes.worldSources.forEach(source => { try { source.stop(); } catch (_) {} try { source.disconnect(); } catch (_) {} });
    return baseStop.call(this);
  };
  proto.worldAudioLoaded = true;
})();
