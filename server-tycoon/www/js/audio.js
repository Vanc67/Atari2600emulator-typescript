/* Локальная CC0-музыка и синтезированные WebAudio-эффекты. Сеть не используется. */
(function () {
  'use strict';
  class TycoonAudio {
    constructor(game) {
      this.game = game;
      this.audio = null;
      this.trackId = null;
      this.context = null;
      this.unlocked = false;
    }

    settings() {
      if (!this.game.state.settings) {
        this.game.state.settings = { music: true, sfx: true, volume: 0.32, track: 'electro' };
      }
      return this.game.state.settings;
    }

    unlock() {
      this.unlocked = true;
      if (this.settings().sfx || this.settings().serverSounds) this.ensureContext();
      if (this.settings().music) this.play(this.settings().track);
      if (this.settings().serverSounds && window.tycoonSoundscape) window.tycoonSoundscape.sync();
    }

    ensureContext() {
      if (this.context) {
        if (this.context.state === 'suspended') {
          const resumed = this.context.resume();
          if (resumed && typeof resumed.catch === 'function') resumed.catch(() => {});
        }
        return this.context;
      }
      const AudioContext = window.AudioContext || window.webkitAudioContext;
      if (!AudioContext) return null;
      try {
        this.context = new AudioContext();
        if (this.context.state === 'suspended') {
          const resumed = this.context.resume();
          if (resumed && typeof resumed.catch === 'function') resumed.catch(() => {});
        }
      } catch (_) {
        this.context = null;
      }
      return this.context;
    }

    play(trackId = 'electro') {
      if (!this.unlocked || !this.settings().music || typeof window.Audio === 'undefined') return;
      const track = (window.DATA.MUSIC_TRACKS || []).find(item => item.id === trackId) || (window.DATA.MUSIC_TRACKS || [])[0];
      if (!track) return;
      if (this.audio && this.trackId === track.id) {
        this.audio.volume = this.settings().volume;
        if (this.audio.paused) {
          const resumed = this.audio.play();
          if (resumed && typeof resumed.catch === 'function') resumed.catch(() => {});
        }
        return;
      }
      if (this.audio) {
        this.audio.pause();
        this.audio.src = '';
      }
      this.trackId = track.id;
      this.settings().track = track.id;
      this.audio = new window.Audio(track.file);
      this.audio.loop = true;
      this.audio.preload = 'auto';
      this.audio.volume = this.settings().volume;
      const started = this.audio.play();
      if (started && typeof started.catch === 'function') started.catch(() => {
        // WebView может потребовать ещё один жест; следующий tap повторит запуск.
      });
    }

    toggle() {
      const settings = this.settings();
      settings.music = !settings.music;
      if (!settings.music && this.audio) this.audio.pause();
      if (settings.music) {
        this.unlocked = true;
        this.play(settings.track);
      }
      this.game.save();
      return settings.music;
    }

    setTrack(trackId) {
      if (!(window.DATA.MUSIC_TRACKS || []).some(track => track.id === trackId)) return;
      if (this.audio) {
        this.audio.pause();
        this.audio = null;
      }
      this.trackId = null;
      this.settings().track = trackId;
      this.settings().music = true;
      this.unlocked = true;
      this.play(trackId);
      this.game.save();
    }

    setVolume(value) {
      const volume = Math.max(0, Math.min(1, Number(value)));
      this.settings().volume = volume;
      if (this.audio) this.audio.volume = volume;
      this.game.save();
    }

    toggleSfx() {
      this.settings().sfx = !this.settings().sfx;
      if (this.settings().sfx) {
        this.unlocked = true;
        this.ensureContext();
        this.sfx('success');
      }
      this.game.save();
      return this.settings().sfx;
    }

    pauseForBackground() {
      if (this.audio && !this.audio.paused) {
        this.audio.pause();
        this.resumeAfterBackground = true;
      } else {
        this.resumeAfterBackground = false;
      }
    }

    resumeFromBackground() {
      if (this.resumeAfterBackground && this.settings().music) this.play(this.settings().track);
      this.resumeAfterBackground = false;
    }

    tone(frequency, start, duration, type = 'sine', gainValue = 0.05) {
      const context = this.ensureContext();
      if (!context) return;
      const oscillator = context.createOscillator();
      const gain = context.createGain();
      oscillator.type = type;
      oscillator.frequency.setValueAtTime(frequency, context.currentTime + start);
      gain.gain.setValueAtTime(0.0001, context.currentTime + start);
      gain.gain.exponentialRampToValueAtTime(gainValue, context.currentTime + start + 0.012);
      gain.gain.exponentialRampToValueAtTime(0.0001, context.currentTime + start + duration);
      oscillator.connect(gain);
      gain.connect(context.destination);
      oscillator.start(context.currentTime + start);
      oscillator.stop(context.currentTime + start + duration + 0.02);
    }

    sfx(kind = 'click') {
      if (!this.unlocked || !this.settings().sfx) return;
      if (kind === 'success') {
        this.tone(520, 0, .08, 'sine', .045);
        this.tone(780, .07, .13, 'sine', .04);
      } else if (kind === 'error') {
        this.tone(145, 0, .18, 'sawtooth', .055);
        this.tone(105, .12, .24, 'square', .035);
      } else if (kind === 'warning') {
        this.tone(330, 0, .08, 'square', .035);
        this.tone(330, .12, .08, 'square', .03);
      } else if (kind === 'power') {
        this.tone(110, 0, .16, 'sawtooth', .04);
        this.tone(440, .08, .18, 'sine', .035);
      } else {
        this.tone(620, 0, .035, 'sine', .018);
      }
    }
  }
  window.TycoonAudio = TycoonAudio;
})();
