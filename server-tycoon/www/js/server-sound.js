/* Ultimate 7.1: многослойный ROAR-синтез серверной стойки, без внешних аудиофайлов. */
(function () {
  'use strict';
  const Base = window.ServerSoundscape;
  if (!Base || Base.prototype.roarLoaded) return;
  const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
  const round = value => Math.round(value);
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const randomFor = seed => { let x = seed >>> 0; return () => { x += 0x6D2B79F5; let t = x; t = Math.imul(t ^ t >>> 15, t | 1); t ^= t + Math.imul(t ^ t >>> 7, t | 61); return ((t ^ t >>> 14) >>> 0) / 4294967296; }; };
  const set = (param, value, now, time = .12) => { if (param && param.setTargetAtTime) param.setTargetAtTime(value, now, time); else if (param) param.value = value; };

  class RoarSoundscape extends Base {
    constructor(game, audio) {
      super(game, audio);
      this.noiseBuffers = {};
      this.compressor = null;
      this.lastActive = 0;
    }
    settings() {
      const settings = this.game.state.settings;
      if (settings.serverSounds === undefined) settings.serverSounds = false;
      if (!Number.isFinite(Number(settings.serverVolume))) settings.serverVolume = .28;
      else settings.serverVolume = clamp(Number(settings.serverVolume), 0, 1);
      if (!settings.serverMuted || typeof settings.serverMuted !== 'object') settings.serverMuted = {};
      if (typeof settings.serverSolo !== 'string') settings.serverSolo = null;
      return settings;
    }
    ensure() {
      if (!this.audio) return null;
      this.audio.unlocked = true;
      this.context = this.audio.ensureContext();
      if (!this.context) return null;
      const c = this.context;
      if (!this.master) {
        this.master = c.createGain();
        if (typeof c.createDynamicsCompressor === 'function') {
          this.compressor = c.createDynamicsCompressor();
          this.compressor.threshold.value = -20;
          this.compressor.knee.value = 16;
          this.compressor.ratio.value = 5;
          this.compressor.attack.value = .004;
          this.compressor.release.value = .22;
          this.master.connect(this.compressor); this.compressor.connect(c.destination);
        } else this.master.connect(c.destination);
      }
      set(this.master.gain, this.settings().serverVolume * .95, c.currentTime, .04);
      return c;
    }
    setVolume(value) {
      const settings = this.settings();
      settings.serverVolume = clamp(Number(value) || 0, 0, 1);
      if (this.master && this.context) set(this.master.gain, settings.serverVolume * .95, this.context.currentTime, .04);
      this.game.save();
    }
    profile(server) {
      const m = this.game.metrics(server);
      const seed = hash(server.id);
      const chassis = server.chassis;
      const cooler = server.parts.cooler;
      const coolerNoise = Number(cooler && cooler.noise) || 24;
      const chassisNoise = Number(chassis.baseNoise) || 30;
      const disks = server.parts.disks.filter(Boolean);
      const hdd = disks.filter(part => part.type === 'hdd').length;
      const ssd = disks.filter(part => part.type === 'ssd').length;
      const nvme = disks.filter(part => /nvme/i.test(`${part.name} ${part.short}`)).length;
      const report = this.game.hardwareReport(server);
      const gpuCount = report.gpuCount || 0;
      const load = clamp(m.rawLoad / 100, 0, 1.5);
      const year = chassis.year || 2004;
      const age = Math.max(0, this.game.currentYear() - year);
      const role = `${chassis.name} ${server.name}`.toLowerCase();
      const rack = chassis.icon === 'server' || /rack|blade|1u|2u|proliant dl/.test(role);
      const legacy = year < 2000;
      const storage = hdd >= 4 || /storage|disk|raid|хранилище/i.test(role);
      const fanProfile = server.fanProfile || 'balanced';
      const fanFactor = fanProfile === 'silent' ? .78 : fanProfile === 'turbo' ? 1.24 : 1;
      const kind = storage ? 'storage' : gpuCount ? 'gpu' : legacy ? 'legacy' : rack ? 'rack' : 'tower';
      const fanCount = kind === 'rack' ? 4 + seed % 5 : kind === 'storage' ? 3 + seed % 4 : kind === 'gpu' ? 3 + seed % 3 : legacy ? 1 + seed % 2 : 2 + seed % 2;
      const baseRpm = kind === 'rack' ? 6500 : kind === 'storage' ? 4800 : kind === 'gpu' ? 4200 : legacy ? 2300 : 3200;
      const rpm = round((baseRpm + seed % (kind === 'rack' ? 2800 : 1500)) * (.8 + load * .38) * fanFactor * (.9 + coolerNoise / 200));
      const fanHz = rpm / 60 * (.94 + (seed % 120) / 1000);
      const bladeHz = fanHz * (kind === 'rack' ? 3 + seed % 3 : 2 + seed % 2);
      const ageDust = clamp(age / 35, 0, 1);
      const noiseGain = clamp(.13 + fanCount * .014 + load * .115 + (rack ? .035 : 0) + coolerNoise * .0008 + chassisNoise * .0005, .1, .52);
      const labels = {
        rack: `Rack · ${fanCount} высокооборотных FAN`, tower: `Tower · ${fanCount} крупных FAN`,
        storage: `Storage · ${hdd} HDD + вентиляторная стена`, gpu: `GPU · blower + PSU coil`, legacy: `Legacy · старый подшипник${hdd ? ' + HDD' : ''}`
      };
      return {
        seed, kind, label: labels[kind], fanCount, rpm, fanHz, bladeHz, harmonicHz: bladeHz * (1.97 + (seed % 30) / 1000),
        hdd, ssd, nvme, gpuCount, load, age, fanProfile, coolerNoise, chassisNoise,
        filterHz: (kind === 'rack' ? 1550 : kind === 'legacy' ? 750 : 1050) + load * 2700 + seed % 450,
        highpassHz: kind === 'rack' ? 170 : 90 + seed % 70,
        turbulenceHz: 420 + seed % 1300 + load * 900,
        noiseGain, airGain: noiseGain,
        turbulenceGain: .065 + load * .075 + fanCount * .005,
        rumbleGain: .025 + fanCount * .004 + load * .025 + ageDust * .012,
        fanGain: .003 + load * .003 + ageDust * .002,
        harmonicGain: .0015 + ageDust * .0025 + (kind === 'rack' ? .001 : 0),
        bearingGain: ageDust * .003 + (kind === 'rack' ? .0015 : .0005),
        coilGain: .0001 + (gpuCount ? .003 + load * .004 : load > .75 ? .002 : 0) + (server.overclock || 0) * .00008,
        coilHz: 2300 + seed % 2700 + gpuCount * 170,
        clickGain: .025 + load * .035 + Math.min(4, hdd) * .006,
        clickRate: hdd ? .3 + load * 2.6 + hdd * .08 : 0,
        seekRate: hdd ? .06 + load * .38 + Math.min(4, hdd) * .025 : 0,
        voiceGain: .9 + Math.min(.12, load * .1),
        playbackRate: .76 + (seed % 480) / 1000 + load * .13,
        pan: ((seed % 200) / 100 - 1) * .82,
        muted: Boolean(this.game.state.settings.serverMuted && this.game.state.settings.serverMuted[server.id])
      };
    }
    makeNoise(kind = 'white') {
      if (this.noiseBuffers[kind]) return this.noiseBuffers[kind];
      if (kind === 'white' && this.noiseBuffer) { this.noiseBuffers.white = this.noiseBuffer; return this.noiseBuffer; }
      const length = Math.floor(this.context.sampleRate * 2);
      const buffer = this.context.createBuffer(1, length, this.context.sampleRate);
      const data = buffer.getChannelData(0); const random = randomFor(hash(`roar-${kind}`)); let brown = 0;
      for (let i = 0; i < length; i += 1) {
        const white = random() * 2 - 1;
        if (kind === 'brown') { brown = (brown + white * .045) / 1.025; data[i] = clamp(brown * 3.4, -1, 1); }
        else data[i] = white * .82;
      }
      this.noiseBuffers[kind] = buffer;
      if (kind === 'white') this.noiseBuffer = buffer;
      return buffer;
    }
    noiseSource(kind, rate, offset) {
      const source = this.context.createBufferSource();
      source.buffer = this.makeNoise(kind); source.loop = true;
      if (source.playbackRate) source.playbackRate.value = rate;
      return { source, offset };
    }
    createSession(server, p) {
      const c = this.context; const now = c.currentTime;
      const output = c.createGain(); output.gain.value = .0001;
      let panner = null;
      if (typeof c.createStereoPanner === 'function') { panner = c.createStereoPanner(); output.connect(panner); panner.connect(this.master); }
      else output.connect(this.master);
      const sources = [];
      const air = c.createBufferSource(); air.buffer = this.makeNoise('white'); air.loop = true; air.playbackRate.value = p.playbackRate;
      const highpass = c.createBiquadFilter(); highpass.type = 'highpass'; highpass.frequency.value = p.highpassHz; highpass.Q.value = .45;
      const filter = c.createBiquadFilter(); filter.type = 'lowpass'; filter.frequency.value = p.filterHz; filter.Q.value = .55;
      const noiseGain = c.createGain(); noiseGain.gain.value = p.airGain;
      air.connect(highpass); highpass.connect(filter); filter.connect(noiseGain); noiseGain.connect(output); sources.push(air);
      const turbulence = c.createBufferSource(); turbulence.buffer = this.makeNoise('white'); turbulence.loop = true; turbulence.playbackRate.value = p.playbackRate * 1.37;
      const turbulenceFilter = c.createBiquadFilter(); turbulenceFilter.type = 'bandpass'; turbulenceFilter.frequency.value = p.turbulenceHz; turbulenceFilter.Q.value = .55;
      const turbulenceGain = c.createGain(); turbulenceGain.gain.value = p.turbulenceGain;
      turbulence.connect(turbulenceFilter); turbulenceFilter.connect(turbulenceGain); turbulenceGain.connect(output); sources.push(turbulence);
      const rumble = c.createBufferSource(); rumble.buffer = this.makeNoise('brown'); rumble.loop = true; rumble.playbackRate.value = .65 + p.load * .15;
      const rumbleFilter = c.createBiquadFilter(); rumbleFilter.type = 'lowpass'; rumbleFilter.frequency.value = 155 + p.fanCount * 8;
      const rumbleGain = c.createGain(); rumbleGain.gain.value = p.rumbleGain;
      rumble.connect(rumbleFilter); rumbleFilter.connect(rumbleGain); rumbleGain.connect(output); sources.push(rumble);
      const fan = c.createOscillator(); fan.type = 'triangle'; fan.frequency.value = p.bladeHz;
      const fanGain = c.createGain(); fanGain.gain.value = p.fanGain; fan.connect(fanGain); fanGain.connect(output); sources.push(fan);
      const harmonic = c.createOscillator(); harmonic.type = 'sine'; harmonic.frequency.value = p.harmonicHz;
      const harmonicGain = c.createGain(); harmonicGain.gain.value = p.harmonicGain; harmonic.connect(harmonicGain); harmonicGain.connect(output); sources.push(harmonic);
      const bearing = c.createOscillator(); bearing.type = 'sawtooth'; bearing.frequency.value = p.fanHz * (5 + p.seed % 4);
      const bearingGain = c.createGain(); bearingGain.gain.value = p.bearingGain; bearing.connect(bearingGain); bearingGain.connect(output); sources.push(bearing);
      const coil = c.createOscillator(); coil.type = 'sine'; coil.frequency.value = p.coilHz;
      const coilGain = c.createGain(); coilGain.gain.value = p.coilGain; coil.connect(coilGain); coilGain.connect(output); sources.push(coil);
      const random = randomFor(p.seed ^ 0x9e3779b9);
      const session = { output, panner, air, noise: air, highpass, filter, noiseGain, turbulence, turbulenceFilter, turbulenceGain,
        rumble, rumbleFilter, rumbleGain, fan, fanGain, harmonic, harmonicGain, bearing, bearingGain, coil, coilGain,
        sources, seed: p.seed, random, profile: p, createdAt: now,
        nextClick: now + .08 + random() * .8, nextSeek: now + .7 + random() * 2.4 };
      this.sessions.set(server.id, session);
      sources.forEach((source, index) => {
        if (source.buffer) source.start(now, (p.seed % 997 + index * 131) / 997 * 1.5);
        else source.start(now);
      });
      this.updateSession(session, p);
      this.mechanical(session, p, now + .015, 'relay');
      return session;
    }
    updateSession(session, p) {
      const now = this.context.currentTime;
      set(session.fan.frequency, p.bladeHz, now, .16);
      set(session.harmonic.frequency, p.harmonicHz, now, .16);
      set(session.bearing.frequency, p.fanHz * (5 + p.seed % 4), now, .16);
      set(session.coil.frequency, p.coilHz, now, .16);
      set(session.fanGain.gain, p.fanGain, now, .18);
      set(session.harmonicGain.gain, p.harmonicGain, now, .18);
      set(session.bearingGain.gain, p.bearingGain, now, .18);
      set(session.coilGain.gain, p.coilGain, now, .12);
      set(session.noiseGain.gain, p.airGain, now, .18);
      set(session.turbulenceGain.gain, p.turbulenceGain, now, .18);
      set(session.rumbleGain.gain, p.rumbleGain, now, .18);
      set(session.filter.frequency, p.filterHz, now, .2);
      set(session.highpass.frequency, p.highpassHz, now, .2);
      set(session.turbulenceFilter.frequency, p.turbulenceHz, now, .2);
      set(session.air.playbackRate, p.playbackRate, now, .2);
      set(session.turbulence.playbackRate, p.playbackRate * 1.37, now, .2);
      if (session.panner) set(session.panner.pan, p.pan, now, .2);
      // Намеренно нет деления на количество машин: одиночный сервер уже звучит на полную.
      set(session.output.gain, p.voiceGain, now, .12);
      session.profile = p;
    }
    mechanical(session, p, when, kind = 'click') {
      const c = this.context; if (!c || !session.output) return;
      const relay = kind === 'relay'; const seek = kind === 'seek';
      const duration = relay ? .12 : seek ? .035 : .022 + session.random() * .025;
      const source = c.createBufferSource(); source.buffer = this.makeNoise('white'); source.loop = false;
      source.playbackRate.value = relay ? .42 : .8 + session.random() * 1.5;
      const filter = c.createBiquadFilter(); filter.type = 'bandpass';
      filter.frequency.value = relay ? 230 : 750 + session.random() * (seek ? 2600 : 1500); filter.Q.value = relay ? .8 : 1.4;
      const gain = c.createGain(); const peak = relay ? .09 : p.clickGain * (seek ? .65 : 1);
      gain.gain.setValueAtTime(.0001, when); gain.gain.exponentialRampToValueAtTime(Math.max(.0002, peak), when + .002);
      gain.gain.exponentialRampToValueAtTime(.0001, when + duration);
      source.connect(filter); filter.connect(gain); gain.connect(session.output);
      source.start(when, session.random() * 1.4, duration + .02); source.stop(when + duration + .03);
      const oscillator = c.createOscillator(); const lowGain = c.createGain(); oscillator.type = relay ? 'square' : 'triangle';
      oscillator.frequency.setValueAtTime(relay ? 105 : 180 + session.random() * 260, when);
      oscillator.frequency.exponentialRampToValueAtTime(relay ? 48 : 75, when + duration);
      lowGain.gain.setValueAtTime(relay ? .055 : .009 + p.load * .008, when); lowGain.gain.exponentialRampToValueAtTime(.0001, when + duration);
      oscillator.connect(lowGain); lowGain.connect(session.output); oscillator.start(when); oscillator.stop(when + duration + .02);
      source.onended = () => { try { source.disconnect(); filter.disconnect(); gain.disconnect(); } catch (_) {} };
      oscillator.onended = () => { try { oscillator.disconnect(); lowGain.disconnect(); } catch (_) {} };
    }
    click(session, p) { this.mechanical(session, p || session.profile, this.context.currentTime, 'click'); }
    scheduleMechanical(session, p) {
      const now = this.context.currentTime;
      if (!p.hdd || p.load < .035) return;
      if (now >= session.nextClick && this.clickBudget > 0) {
        const count = 1 + Math.floor(session.random() * Math.min(3, p.hdd));
        for (let i = 0; i < count; i += 1) this.mechanical(session, p, now + i * (.025 + session.random() * .045), 'click');
        session.nextClick = now + (.25 + session.random() * 1.2) / Math.max(.15, p.clickRate);
        this.clickBudget -= 1;
      }
      if (now >= session.nextSeek && this.clickBudget > 0) {
        const count = 3 + Math.floor(session.random() * 5);
        for (let i = 0; i < count; i += 1) this.mechanical(session, p, now + i * (.018 + session.random() * .027), 'seek');
        session.nextSeek = now + (1.2 + session.random() * 4.5) / Math.max(.08, p.seekRate * 4);
        this.clickBudget -= 1;
      }
    }
    sync() {
      const settings = this.settings();
      if (!settings.serverSounds || (typeof document !== 'undefined' && document.hidden)) { this.stopAll(); return; }
      if (!this.audio || !this.audio.unlocked || !this.ensure()) return;
      let active = this.game.state.servers.filter(server => server.powered && !this.game.state.powerOut);
      if (settings.serverSolo && !active.some(server => server.id === settings.serverSolo)) settings.serverSolo = null;
      if (settings.serverSolo) active = active.filter(server => server.id === settings.serverSolo);
      active = active.filter(server => !settings.serverMuted[server.id]);
      const alive = new Set(active.map(server => server.id));
      for (const [id, session] of this.sessions) if (!alive.has(id)) this.stopSession(id, session);
      this.clickBudget = 10;
      active.forEach(server => {
        const profile = this.profile(server);
        const session = this.sessions.get(server.id) || this.createSession(server, profile);
        this.updateSession(session, profile);
        this.scheduleMechanical(session, profile);
      });
      this.lastActive = active.length;
    }
    toggleServerMute(id) {
      const settings = this.settings();
      settings.serverMuted[id] = !settings.serverMuted[id];
      if (settings.serverMuted[id] && settings.serverSolo === id) settings.serverSolo = null;
      this.game.save(); this.sync(); return settings.serverMuted[id];
    }
    soloServer(id) {
      const settings = this.settings();
      settings.serverSolo = settings.serverSolo === id ? null : id;
      if (settings.serverSolo) settings.serverMuted[id] = false;
      this.game.save(); this.sync(); return settings.serverSolo;
    }
    stopSession(id, session) {
      (session.sources || ['fan', 'harmonic', 'noise']).forEach(source => { try { source.stop(); } catch (_) {} });
      try { session.output.disconnect(); if (session.panner) session.panner.disconnect(); } catch (_) {}
      this.sessions.delete(id);
    }
  }
  RoarSoundscape.prototype.roarLoaded = true;
  window.ServerSoundscape = RoarSoundscape;
})();
