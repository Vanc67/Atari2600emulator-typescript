export class AudioEngine {
  private ctx: AudioContext | null = null;
  private oscillators: { osc: OscillatorNode; gain: GainNode }[] = [];
  private enabled = false;

  constructor() {
    // AudioContext must be started after user interaction
  }

  private init() {
    if (this.ctx) return;
    this.ctx = new (window.AudioContext || (window as any).webkitAudioContext)();
    
    for (let i = 0; i < 2; i++) {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      
      osc.type = 'square';
      osc.frequency.setValueAtTime(0, this.ctx.currentTime);
      gain.gain.setValueAtTime(0, this.ctx.currentTime);
      
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      
      this.oscillators.push({ osc, gain });
    }
    this.enabled = true;
  }

  public resume() {
    if (!this.ctx) {
      this.init();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }

  public update(channel: number, control: number, frequency: number, volume: number) {
    if (!this.enabled || !this.ctx) return;

    const { osc, gain } = this.oscillators[channel];
    
    // Atari 2600 frequency is a divider: f = 30000 / (frequency + 1)
    // NTSC TIA clock is ~31400 Hz for audio
    const freq = 31400 / (frequency + 1);
    
    // Volume is 0-15
    const vol = (volume / 15) * 0.1; // Keep it low to avoid clipping

    // AUDC determines the waveform. 
    // This is a simplified mapping:
    // 0: Off
    // 1, 2, 3, 6, 7, 9, 10, 11, 12, 13, 14, 15: Various noise/distorted square
    // 4, 5: Pure square (div by 2)
    // 8: Pure square (div by 1)
    
    if (volume === 0 || control === 0) {
      gain.gain.setTargetAtTime(0, this.ctx.currentTime, 0.01);
      return;
    }

    // Simplified waveform mapping
    if (control === 4 || control === 5 || control === 8 || control === 12) {
      osc.type = 'square';
    } else {
      // Noise approximation using sawtooth or high-freq square
      // Real TIA uses LFSRs for noise, which is hard to do with standard oscillators
      osc.type = 'sawtooth';
    }

    osc.frequency.setTargetAtTime(freq, this.ctx.currentTime, 0.01);
    gain.gain.setTargetAtTime(vol, this.ctx.currentTime, 0.01);
  }

  public stop() {
    if (this.ctx && this.ctx.state === 'running') {
      this.ctx.suspend();
    }
  }
}
