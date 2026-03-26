import { CPU6507 } from './CPU6507';
import { TIA } from './TIA';
import { Joystick } from './Joystick';
import { RIOT } from './RIOT';
import { AudioEngine } from './AudioEngine';

export class Atari2600 {
  memory: Uint8Array;
  cpu: CPU6507;
  tia: TIA;
  joystick: Joystick;
  riot: RIOT;
  audio: AudioEngine;
  
  isRunning: boolean = false;
  animationFrameId: number = 0;
  
  scanlineCycles = 0;
  cpuStartCycles = 0;
  scanline = 0;
  lastFrameTime = 0;
  readonly frameDelay = 1000 / 60;

  constructor(canvas: HTMLCanvasElement) {
    this.memory = new Uint8Array(8192);
    
    this.joystick = new Joystick();
    this.riot = new RIOT(this.joystick);
    this.audio = new AudioEngine();

    this.tia = new TIA(
      canvas, 
      () => { this.cpu.halted = true; }, 
      () => this.scanlineCycles + (this.cpu.cycles - this.cpuStartCycles),
      this.joystick,
      this.audio
    );

    this.cpu = new CPU6507(
      this.memoryRead.bind(this),
      this.memoryWrite.bind(this)
    );
  }

  memoryRead(address: number): number {
    address &= 0x1FFF;
    if (address & 0x1000) {
      return this.memory[address]; // ROM
    } else if (address & 0x0080) {
      if (address & 0x0200) {
        return this.riot.read(address); // RIOT
      } else {
        return this.memory[address & 0x00FF]; // RAM
      }
    } else {
      return this.tia.readRegister(address); // TIA
    }
  }

  memoryWrite(address: number, value: number) {
    address &= 0x1FFF;
    if (address & 0x1000) {
      // ROM write (usually ignored, or bank switching)
    } else if (address & 0x0080) {
      if (address & 0x0200) {
        this.riot.write(address, value); // RIOT
      } else {
        this.memory[address & 0x00FF] = value; // RAM
      }
    } else {
      this.tia.writeRegister(address, value); // TIA
    }
  }

  loadROM(romData: Uint8Array) {
    const startAddress = 0x1000;
    for (let i = 0; i < romData.length && i < 4096; i++) {
      this.memory[startAddress + i] = romData[i];
      // Mirror 2K ROMs
      if (romData.length === 2048) {
        this.memory[startAddress + i + 2048] = romData[i];
      }
    }
    this.cpu.reset();
  }

  start() {
    if (this.isRunning) return;
    this.isRunning = true;
    this.lastFrameTime = 0;
    this.animationFrameId = requestAnimationFrame(this.loop);
  }

  stop() {
    this.isRunning = false;
    this.audio.stop();
    cancelAnimationFrame(this.animationFrameId);
  }

  loop = (timestamp: number) => {
    if (!this.isRunning) return;

    if (!this.lastFrameTime) this.lastFrameTime = timestamp;
    const elapsed = timestamp - this.lastFrameTime;

    if (elapsed >= this.frameDelay) {
      this.lastFrameTime = timestamp - (elapsed % this.frameDelay);
      this.renderFrame();
    }
    this.animationFrameId = requestAnimationFrame(this.loop);
  }

  renderFrame() {
    let frameComplete = false;
    let linesInThisFrame = 0;

    while (!frameComplete && linesInThisFrame < 312) { // 312 is PAL limit, NTSC is 262
      this.cpu.halted = false;
      this.tia.beginScanline(this.scanline);
      
      while (this.scanlineCycles < 76 && !this.cpu.halted) {
        this.cpuStartCycles = this.cpu.cycles;
        const cycles = this.cpu.step();
        this.scanlineCycles += cycles;
        this.riot.step(cycles);
      }
      
      if (this.cpu.halted) {
        if (this.scanlineCycles < 76) {
          this.riot.step(76 - this.scanlineCycles);
          this.scanlineCycles = 76;
        }
      }
      
      this.tia.endScanline();
      this.scanlineCycles -= 76;
      this.scanline++;
      linesInThisFrame++;

      if (this.tia.vsyncChangedToOff) {
        this.tia.vsyncChangedToOff = false;
        this.scanline = 0;
        frameComplete = true;
      } else if (this.scanline >= 262) {
        // NTSC fallback
        this.scanline = 0;
        frameComplete = true;
      }
    }
    
    this.tia.render();
  }
}
