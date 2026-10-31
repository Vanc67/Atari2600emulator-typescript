/* Ultimate 7.0: 20 процедурных lo-fi треков × 30 минут = 10 часов без сети. */
(function () {
  'use strict';
  const TRACK_SECONDS = 1800;
  const titles = [
    'Паяльник в полночь','Пыль на клавише Turbo','Дождь над системным блоком','Кофе и 4 МБ','Скрип дисковода',
    'Тёплый кинескоп','Windows закрывается','Ночной дефрагментатор','Проводной интернет','Чиптюн без чипа',
    'Соседский Pentium','После последнего клиента','Тихий BIOS','Картонная коробка драйверов','Синий экран, тёплый чай',
    'DDR на подоконнике','Лампа над мусоркой','Воскресный MemTest','Сектора засыпают','Десять часов спустя'
  ];
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const hash = value => { let h = 2166136261; for (const char of String(value)) h = Math.imul(h ^ char.charCodeAt(0), 16777619); return h >>> 0; };
  const randomFor = seed => { let x = seed >>> 0; return () => { x += 0x6D2B79F5; let t = x; t = Math.imul(t ^ t >>> 15, t | 1); t ^= t + Math.imul(t ^ t >>> 7, t | 61); return ((t ^ t >>> 14) >>> 0) / 4294967296; }; };
  class LongLofiRadio {
    constructor(audio) {
      this.audio = audio;
      this.tracks = titles.map((title, index) => ({ id: `lofi-${String(index + 1).padStart(2, '0')}`, title, duration: TRACK_SECONDS, bpm: 62 + index % 9 * 2, root: [110, 98, 123.47, 130.81, 146.83][index % 5] }));
      this.totalDuration = this.tracks.reduce((sum, track) => sum + track.duration, 0);
      this.index = 0; this.position = 0; this.startedAt = 0; this.playing = false;
      this.nextBarTime = 0; this.barIndex = 0; this.bus = null; this.noise = null; this.lastError = null;
    }
    status() {
      const context = this.audio.context;
      const position = this.playing && context ? this.position + Math.max(0, context.currentTime - this.startedAt) : this.position;
      return { playing: this.playing, index: this.index, track: this.tracks[this.index], position: position % TRACK_SECONDS,
        playlistPosition: this.index * TRACK_SECONDS + position % TRACK_SECONDS, totalDuration: this.totalDuration, tracks: this.tracks.length };
    }
    start() {
      const context = this.audio.ensureContext();
      if (!context) return false;
      if (this.audio.audio) { this.audio.audio.pause(); this.audio.audio.src = ''; this.audio.audio = null; this.audio.trackId = null; }
      if (this.playing) { this.tick(); return true; }
      this.playing = true; this.startedAt = context.currentTime; this.makeBus(); this.schedule();
      return true;
    }
    makeBus() {
      const context = this.audio.context;
      if (this.bus) { try { this.bus.disconnect(); } catch (_) {} }
      this.bus = context.createGain();
      this.bus.gain.value = clamp(this.audio.settings().volume * .75, 0, .6);
      this.bus.connect(context.destination);
      this.makeVinyl();
    }
    makeVinyl() {
      const context = this.audio.context;
      if (this.noise) { try { this.noise.stop(); } catch (_) {} }
      const length = Math.floor(context.sampleRate * 2);
      const buffer = context.createBuffer(1, length, context.sampleRate);
      const data = buffer.getChannelData(0); const random = randomFor(hash(this.tracks[this.index].id));
      for (let i = 0; i < length; i += 1) data[i] = (random() * 2 - 1) * (random() > .997 ? .7 : .018);
      const source = context.createBufferSource(); source.buffer = buffer; source.loop = true;
      const filter = context.createBiquadFilter(); filter.type = 'highpass'; filter.frequency.value = 2400;
      const gain = context.createGain(); gain.gain.value = .045;
      source.connect(filter); filter.connect(gain); gain.connect(this.bus); source.start(); this.noise = source;
    }
    stop() {
      if (this.audio.context && this.playing) this.position = (this.position + Math.max(0, this.audio.context.currentTime - this.startedAt)) % TRACK_SECONDS;
      this.playing = false;
      if (this.noise) { try { this.noise.stop(); } catch (_) {} this.noise = null; }
      if (this.bus) { try { this.bus.disconnect(); } catch (_) {} this.bus = null; }
    }
    setVolume() { if (this.bus) this.bus.gain.value = clamp(this.audio.settings().volume * .75, 0, .6); }
    selectTrack(index) {
      index = ((Number(index) || 0) % this.tracks.length + this.tracks.length) % this.tracks.length;
      const wasPlaying = this.playing; if (wasPlaying) this.stop();
      this.index = index; this.position = 0; this.barIndex = 0; this.nextBarTime = 0;
      if (wasPlaying) this.start();
      return this.tracks[this.index];
    }
    next() { return this.selectTrack(this.index + 1); }
    previous() { return this.selectTrack(this.index - 1); }
    tick() {
      if (!this.playing || !this.audio.context) return;
      const elapsed = this.position + Math.max(0, this.audio.context.currentTime - this.startedAt);
      if (elapsed >= TRACK_SECONDS) {
        const skipped = Math.floor(elapsed / TRACK_SECONDS);
        const remainder = elapsed % TRACK_SECONDS;
        this.position = 0; this.index = (this.index + skipped) % this.tracks.length;
        this.startedAt = this.audio.context.currentTime - remainder;
        this.barIndex = Math.floor((this.index * TRACK_SECONDS + remainder) / 3);
        this.makeBus();
      }
      this.schedule();
    }
    oscillator(frequency, time, duration, type, volume, destination = this.bus, detune = 0) {
      const context = this.audio.context;
      const oscillator = context.createOscillator(); const gain = context.createGain();
      oscillator.type = type; oscillator.frequency.setValueAtTime(frequency, time); oscillator.detune.value = detune;
      gain.gain.setValueAtTime(.0001, time); gain.gain.exponentialRampToValueAtTime(Math.max(.0002, volume), time + .025);
      gain.gain.exponentialRampToValueAtTime(.0001, time + duration);
      oscillator.connect(gain); gain.connect(destination); oscillator.start(time); oscillator.stop(time + duration + .04);
    }
    drum(kind, time, volume) {
      const context = this.audio.context;
      if (kind === 'kick') {
        const oscillator = context.createOscillator(); const gain = context.createGain();
        oscillator.frequency.setValueAtTime(115, time); oscillator.frequency.exponentialRampToValueAtTime(45, time + .13);
        gain.gain.setValueAtTime(volume, time); gain.gain.exponentialRampToValueAtTime(.0001, time + .18);
        oscillator.connect(gain); gain.connect(this.bus); oscillator.start(time); oscillator.stop(time + .2);
      } else {
        const length = Math.floor(context.sampleRate * .09); const buffer = context.createBuffer(1, length, context.sampleRate); const data = buffer.getChannelData(0);
        const random = randomFor(hash(`${this.index}:${this.barIndex}:${kind}:${time}`));
        for (let i = 0; i < length; i += 1) data[i] = random() * 2 - 1;
        const source = context.createBufferSource(); source.buffer = buffer;
        const filter = context.createBiquadFilter(); filter.type = kind === 'snare' ? 'bandpass' : 'highpass'; filter.frequency.value = kind === 'snare' ? 1700 : 6500;
        const gain = context.createGain(); gain.gain.setValueAtTime(volume, time); gain.gain.exponentialRampToValueAtTime(.0001, time + (kind === 'snare' ? .11 : .045));
        source.connect(filter); filter.connect(gain); gain.connect(this.bus); source.start(time); source.stop(time + .12);
      }
    }
    schedule() {
      const context = this.audio.context; if (!context || !this.bus || !this.playing) return;
      if (!this.nextBarTime || this.nextBarTime < context.currentTime - 1) this.nextBarTime = context.currentTime + .05;
      const track = this.tracks[this.index]; const beat = 60 / track.bpm; const barLength = beat * 4;
      let safety = 0;
      while (this.nextBarTime < context.currentTime + 8 && safety < 5) {
        const time = this.nextBarTime; const random = randomFor(hash(`${track.id}:${this.barIndex}`));
        const progression = [0, 5, 3, 4][Math.floor(this.barIndex / 2) % 4];
        const root = track.root * Math.pow(2, progression / 12);
        const chord = this.barIndex % 4 === 3 ? [0, 3, 7, 10] : [0, 3, 7, 12];
        chord.forEach((semitone, index) => this.oscillator(root * Math.pow(2, semitone / 12), time + index * .025, barLength * .88, 'triangle', .026 / (1 + index * .25), this.bus, (random() - .5) * 9));
        this.oscillator(root / 2, time, beat * 1.65, 'sine', .055);
        this.drum('kick', time, .09); if (random() > .25) this.drum('kick', time + beat * 2.5, .055);
        this.drum('snare', time + beat, .032); this.drum('snare', time + beat * 3, .032);
        for (let i = 0; i < 8; i += 1) if (random() > .14) this.drum('hat', time + beat * i / 2, .009 + random() * .009);
        if (this.barIndex % 2 === 1 || random() > .55) {
          const scale = [0, 3, 5, 7, 10, 12];
          for (let i = 0; i < 3; i += 1) {
            const note = scale[Math.floor(random() * scale.length)];
            this.oscillator(root * 2 * Math.pow(2, note / 12), time + beat * (.5 + i), beat * .65, 'sine', .018 + random() * .01, this.bus, -6);
          }
        }
        this.barIndex += 1; this.nextBarTime += barLength; safety += 1;
      }
    }
  }
  window.LongLofiRadio = LongLofiRadio;
  window.LOFI_TRACK_SECONDS = TRACK_SECONDS;
})();
