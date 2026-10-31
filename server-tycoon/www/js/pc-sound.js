/* Ultimate 7.0: отдельный от серверной фермы синтезатор домашнего ПК. */
(function () {
  'use strict';
  class PCSoundscape {
    constructor(audio) { this.audio = audio; this.machineId = null; this.nodes = null; this.profile = null; this.nextClick = 0; }
    enabled() { return this.audio.unlocked && this.audio.settings().pcSounds !== false && this.audio.context; }
    sync(machine, metrics) {
      if (!machine || !machine.powered || !this.enabled()) { this.stop(); return false; }
      if (this.machineId !== machine.id || !this.nodes) this.create(machine, metrics);
      const context = this.audio.context; const now = context.currentTime;
      this.profile = { fanHz: 38 + metrics.watts * .14, hdd: machine.storageType === 'HDD', ssd: machine.storageType !== 'HDD',
        coilWhine: metrics.gpuLoad > 65, noiseDb: metrics.noise, cpuLoad: metrics.cpuLoad };
      this.nodes.fan.frequency.setTargetAtTime(this.profile.fanHz, now, .4);
      this.nodes.harmonic.frequency.setTargetAtTime(this.profile.fanHz * 2.07, now, .4);
      this.nodes.fanGain.gain.setTargetAtTime(.012 + metrics.cpuLoad * .00012, now, .4);
      this.nodes.coilGain.gain.setTargetAtTime(this.profile.coilWhine ? .0025 : .00001, now, .25);
      return this.profile;
    }
    create(machine, metrics) {
      this.stop(); const context = this.audio.context; const bus = context.createGain(); bus.gain.value = .7; bus.connect(context.destination);
      const fan = context.createOscillator(); fan.type = 'triangle'; fan.frequency.value = 45;
      const harmonic = context.createOscillator(); harmonic.type = 'sine'; harmonic.frequency.value = 93;
      const fanGain = context.createGain(); fanGain.gain.value = .014;
      const harmonicGain = context.createGain(); harmonicGain.gain.value = .006;
      const coil = context.createOscillator(); coil.type = 'sine'; coil.frequency.value = 2850 + machine.year % 7 * 110;
      const coilGain = context.createGain(); coilGain.gain.value = .00001;
      fan.connect(fanGain); harmonic.connect(harmonicGain); fanGain.connect(bus); harmonicGain.connect(bus);
      coil.connect(coilGain); coilGain.connect(bus); fan.start(); harmonic.start(); coil.start();
      this.machineId = machine.id; this.nodes = { bus, fan, harmonic, fanGain, harmonicGain, coil, coilGain };
      this.nextClick = context.currentTime + .2; this.sync(machine, metrics);
    }
    click(machine, loud = false) {
      if (!this.nodes || !this.audio.context) return;
      const context = this.audio.context; const now = context.currentTime;
      const oscillator = context.createOscillator(); const gain = context.createGain();
      oscillator.type = 'square'; oscillator.frequency.setValueAtTime(machine.storageType === 'HDD' ? 180 : 420, now);
      oscillator.frequency.exponentialRampToValueAtTime(70, now + .025);
      gain.gain.setValueAtTime(loud ? .025 : .009, now); gain.gain.exponentialRampToValueAtTime(.0001, now + .035);
      oscillator.connect(gain); gain.connect(this.nodes.bus); oscillator.start(now); oscillator.stop(now + .04);
    }
    tick(machine, metrics) {
      if (!machine || !machine.powered || !this.nodes || this.machineId !== machine.id) return;
      const now = this.audio.context.currentTime;
      if (now >= this.nextClick) {
        if (machine.storageType === 'HDD') { this.click(machine, metrics.cpuLoad > 65); if (metrics.cpuLoad > 40) setTimeout(() => this.click(machine), 45); }
        else if (metrics.cpuLoad > 75 && Math.random() > .65) this.click(machine);
        this.nextClick = now + .35 + Math.random() * (machine.storageType === 'HDD' ? 1.5 : 4);
      }
    }
    stop() {
      if (this.nodes) {
        ['fan', 'harmonic', 'coil'].forEach(key => { try { this.nodes[key].stop(); } catch (_) {} });
        try { this.nodes.bus.disconnect(); } catch (_) {}
      }
      this.nodes = null; this.machineId = null; this.profile = null;
    }
  }
  window.PCSoundscape = PCSoundscape;
})();
