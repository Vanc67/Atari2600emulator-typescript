import { Joystick } from './Joystick';

export class RIOT {
  timer = 0;
  timerShift = 0;
  cycles = 0;
  joystick: Joystick;
  
  constructor(joystick: Joystick) {
    this.joystick = joystick;
  }

  read(address: number): number {
    const addr = address & 0x1F;
    switch (addr) {
      case 0x00: return this.joystick.readSWCHA(); // SWCHA
      case 0x02: return 0x0B; // SWCHB (Color, P0 Diff B, P1 Diff B, Reset off, Select off)
      case 0x04: 
      case 0x14: return this.timer; // INTIM
      case 0x05: 
      case 0x15: return 0; // TIMINT
    }
    return 0;
  }

  write(address: number, value: number) {
    const addr = address & 0x1F;
    switch (addr) {
      case 0x14: this.timer = value; this.timerShift = 0; this.cycles = 0; break;
      case 0x15: this.timer = value; this.timerShift = 3; this.cycles = 0; break;
      case 0x16: this.timer = value; this.timerShift = 6; this.cycles = 0; break;
      case 0x17: this.timer = value; this.timerShift = 10; this.cycles = 0; break;
    }
  }

  step(cpuCycles: number) {
    this.cycles += cpuCycles;
    const divider = 1 << this.timerShift;
    while (this.cycles >= divider) {
      this.cycles -= divider;
      this.timer--;
      if (this.timer < 0) {
        this.timer = 0xFF;
        this.timerShift = 0;
      }
    }
  }
}
