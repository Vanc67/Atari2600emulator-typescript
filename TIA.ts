import { Joystick } from './Joystick';
import { AudioEngine } from './AudioEngine';

export class TIA {
  canvas: HTMLCanvasElement;
  ctx: CanvasRenderingContext2D;
  imageData: ImageData;
  
  width = 160;
  height = 210;

  // Registers
  vsync = 0;
  vblank = 0;
  vsyncChangedToOff = false;
  colubk = 0x00; colupf = 0x00; colup0 = 0x00; colup1 = 0x00;
  ctrlpf = 0x00;
  pf0 = 0x00; pf1 = 0x00; pf2 = 0x00;
  grp0 = 0x00; grp1 = 0x00;
  old_grp0 = 0x00; old_grp1 = 0x00;
  enam0 = 0; enam1 = 0; enabl = 0;
  old_enabl = 0;
  hmp0 = 0; hmp1 = 0; hmm0 = 0; hmm1 = 0; hmbl = 0;
  vdeltp0 = 0; vdeltp1 = 0; vdelbl = 0;
  refp0 = 0; refp1 = 0;
  nusiz0 = 0; nusiz1 = 0;
  
  // Audio
  audc0 = 0; audc1 = 0;
  audf0 = 0; audf1 = 0;
  audv0 = 0; audv1 = 0;
  
  // Positions (0-159)
  p0_x = 0; p1_x = 0; m0_x = 0; m1_x = 0; bl_x = 0;
  
  // Collisions
  cxm0p = 0; cxm1p = 0; cxp0fb = 0; cxp1fb = 0; cxm0fb = 0; cxm1fb = 0; cxblpf = 0; cxppmm = 0;

  hmoveBlank = false;
  
  scanline = 0;
  colorClock = 0;

  cpuHaltCallback: () => void;
  getCycles: () => number;
  joystick: Joystick;
  audio: AudioEngine;

  resmp0 = 0; resmp1 = 0;
  
  static NTSC_PALETTE: number[][] = [];

  constructor(canvas: HTMLCanvasElement, cpuHaltCallback: () => void, getCycles: () => number, joystick: Joystick, audio: AudioEngine) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d', { willReadFrequently: true })!;
    this.canvas.width = this.width;
    this.canvas.height = this.height;
    this.imageData = this.ctx.createImageData(this.width, this.height);
    this.cpuHaltCallback = cpuHaltCallback;
    this.getCycles = getCycles;
    this.joystick = joystick;
    this.audio = audio;
    
    if (TIA.NTSC_PALETTE.length === 0) {
      for (let i = 0; i < 256; i++) {
        TIA.NTSC_PALETTE[i] = this.getNTSCColor(i);
      }
    }
    
    this.clearScreen();
  }

  beginScanline(scanline: number) {
    this.scanline = scanline;
    this.colorClock = 0;
  }

  endScanline() {
    this.catchUp(228);
    this.hmoveBlank = false;
  }

  catchUp(targetClock: number) {
    if (targetClock > 228) targetClock = 228;
    
    // NTSC active area typically starts around line 40-44
    const startLine = 44;
    if (this.scanline < startLine || this.scanline >= startLine + this.height) {
      this.colorClock = targetClock;
      return;
    }
    
    const y = this.scanline - startLine;
    const offset = y * this.width * 4;

    while (this.colorClock < targetClock) {
      // 68 is standard HBLANK. We add a small offset to center the image.
      if (this.colorClock >= 68) {
        const x = this.colorClock - 68;
        if (x < 160) {
          this.drawPixel(x, y, offset);
        }
      }
      this.colorClock++;
    }
  }

  writeRegister(address: number, value: number) {
    const currentClock = this.getCycles() * 3;
    this.catchUp(currentClock);

    const addr = address & 0x3F;
    switch (addr) {
      case 0x00: {
        const newVsync = value & 2;
        if (this.vsync && !newVsync) {
          this.vsyncChangedToOff = true;
        }
        this.vsync = newVsync;
        break;
      }
      case 0x01: this.vblank = value & 2; break; // VBLANK
      case 0x02: this.cpuHaltCallback(); break; // WSYNC
      case 0x03: this.colorClock = 0; break; // RSYNC
      case 0x04: this.nusiz0 = value; break;
      case 0x05: this.nusiz1 = value; break;
      case 0x06: this.colup0 = value; break;
      case 0x07: this.colup1 = value; break;
      case 0x08: this.colupf = value; break;
      case 0x09: this.colubk = value; break;
      case 0x0A: this.ctrlpf = value; break;
      case 0x0B: this.refp0 = value; break;
      case 0x0C: this.refp1 = value; break;
      case 0x0D: this.pf0 = value; break;
      case 0x0E: this.pf1 = value; break;
      case 0x0F: this.pf2 = value; break;
      case 0x10: this.p0_x = this.getPixelPos(); if (this.resmp0) this.m0_x = (this.p0_x + 4) % 160; break; // RESP0
      case 0x11: this.p1_x = this.getPixelPos(); if (this.resmp1) this.m1_x = (this.p1_x + 4) % 160; break; // RESP1
      case 0x12: this.m0_x = this.getPixelPos(); break; // RESM0
      case 0x13: this.m1_x = this.getPixelPos(); break; // RESM1
      case 0x14: this.bl_x = this.getPixelPos(); break; // RESBL
      case 0x15: this.audc0 = value & 0x0F; this.audio.update(0, this.audc0, this.audf0, this.audv0); break;
      case 0x16: this.audc1 = value & 0x0F; this.audio.update(1, this.audc1, this.audf1, this.audv1); break;
      case 0x17: this.audf0 = value & 0x1F; this.audio.update(0, this.audc0, this.audf0, this.audv0); break;
      case 0x18: this.audf1 = value & 0x1F; this.audio.update(1, this.audc1, this.audf1, this.audv1); break;
      case 0x19: this.audv0 = value & 0x0F; this.audio.update(0, this.audc0, this.audf0, this.audv0); break;
      case 0x1A: this.audv1 = value & 0x0F; this.audio.update(1, this.audc1, this.audf1, this.audv1); break;
      case 0x1B: this.old_grp1 = this.grp1; this.grp0 = value; break;
      case 0x1C: this.old_grp0 = this.grp0; this.old_enabl = this.enabl; this.grp1 = value; break;
      case 0x1D: this.enam0 = value & 2; break;
      case 0x1E: this.enam1 = value & 2; break;
      case 0x1F: this.enabl = value & 2; break;
      case 0x20: this.hmp0 = value >> 4; break;
      case 0x21: this.hmp1 = value >> 4; break;
      case 0x22: this.hmm0 = value >> 4; break;
      case 0x23: this.hmm1 = value >> 4; break;
      case 0x24: this.hmbl = value >> 4; break;
      case 0x25: this.vdeltp0 = value; break;
      case 0x26: this.vdeltp1 = value; break;
      case 0x27: this.vdelbl = value; break;
      case 0x28: this.resmp0 = value & 2; if (this.resmp0) this.m0_x = (this.p0_x + 4) % 160; break;
      case 0x29: this.resmp1 = value & 2; if (this.resmp1) this.m1_x = (this.p1_x + 4) % 160; break;
      case 0x2A: this.applyHMOVE(); break; // HMOVE
      case 0x2B: this.hmp0 = this.hmp1 = this.hmm0 = this.hmm1 = this.hmbl = 0; break; // HMCLR
      case 0x2C: this.cxm0p = this.cxm1p = this.cxp0fb = this.cxp1fb = this.cxm0fb = this.cxm1fb = this.cxblpf = this.cxppmm = 0; break; // CXCLR
    }
  }

  readRegister(address: number): number {
    const addr = address & 0x0F;
    switch (addr) {
      case 0x00: return this.cxm0p;
      case 0x01: return this.cxm1p;
      case 0x02: return this.cxp0fb;
      case 0x03: return this.cxp1fb;
      case 0x04: return this.cxm0fb;
      case 0x05: return this.cxm1fb;
      case 0x06: return this.cxblpf;
      case 0x07: return this.cxppmm;
      case 0x08: return 0x80; // INPT0
      case 0x09: return 0x80; // INPT1
      case 0x0A: return 0x80; // INPT2
      case 0x0B: return 0x80; // INPT3
      case 0x0C: return this.joystick.readINPT4(); // INPT4 (P1 Fire)
      case 0x0D: return 0x80; // INPT5 (P2 Fire)
    }
    return 0;
  }

  getPixelPos() {
    // 1 CPU cycle = 3 color clocks. Horizontal blanking is 68 color clocks.
    // TIA delay is typically 4-5 clocks. We add 5.
    let pos = this.colorClock - 68 + 5;
    while (pos < 0) pos += 160;
    return pos % 160;
  }

  drawPixel(x: number, y: number, offset: number) {
    if (this.vblank) {
      this.imageData.data[offset + x * 4] = 0;
      this.imageData.data[offset + x * 4 + 1] = 0;
      this.imageData.data[offset + x * 4 + 2] = 0;
      return;
    }

    if (this.hmoveBlank && x < 8) {
      this.imageData.data[offset + x * 4] = 0;
      this.imageData.data[offset + x * 4 + 1] = 0;
      this.imageData.data[offset + x * 4 + 2] = 0;
      return;
    }

    let isPF = false;
    let pfIndex = Math.floor(x / 4);
    const reflectPF = (this.ctrlpf & 0x01) !== 0;
    if (pfIndex >= 20) {
      pfIndex = reflectPF ? 39 - pfIndex : pfIndex - 20;
    }
    
    if (pfIndex < 4) {
      isPF = (this.pf0 & (1 << (4 + pfIndex))) !== 0;
    } else if (pfIndex < 12) {
      isPF = (this.pf1 & (1 << (7 - (pfIndex - 4)))) !== 0;
    } else {
      isPF = (this.pf2 & (1 << (pfIndex - 12))) !== 0;
    }

    const scoreMode = (this.ctrlpf & 0x02) !== 0;
    const priority = (this.ctrlpf & 0x04) !== 0;

    const bgCol = TIA.NTSC_PALETTE[this.colubk];
    const pfCol = TIA.NTSC_PALETTE[this.colupf];
    const p0Col = TIA.NTSC_PALETTE[this.colup0];
    const p1Col = TIA.NTSC_PALETTE[this.colup1];

    const current_grp0 = (this.vdeltp0 & 1) ? this.old_grp0 : this.grp0;
    const current_grp1 = (this.vdeltp1 & 1) ? this.old_grp1 : this.grp1;
    const current_enabl = (this.vdelbl & 1) ? this.old_enabl : this.enabl;
    
    // Players
    const isP0 = this.isPlayerPixel(x, this.p0_x, this.nusiz0, current_grp0, this.refp0);
    const isP1 = this.isPlayerPixel(x, this.p1_x, this.nusiz1, current_grp1, this.refp1);

    // Missiles & Ball
    const isM0 = this.isMissilePixel(x, this.m0_x, this.nusiz0, this.enam0, this.resmp0);
    const isM1 = this.isMissilePixel(x, this.m1_x, this.nusiz1, this.enam1, this.resmp1);
    const isBL = this.isBallPixel(x, this.bl_x, this.ctrlpf, current_enabl);

    // Collisions
    if (isM0 && isP0) this.cxm0p |= 0x80;
    if (isM0 && isP1) this.cxm0p |= 0x40;
    if (isM1 && isP0) this.cxm1p |= 0x80;
    if (isM1 && isP1) this.cxm1p |= 0x40;
    if (isP0 && isPF) this.cxp0fb |= 0x80;
    if (isP0 && isBL) this.cxp0fb |= 0x40;
    if (isP1 && isPF) this.cxp1fb |= 0x80;
    if (isP1 && isBL) this.cxp1fb |= 0x40;
    if (isM0 && isPF) this.cxm0fb |= 0x80;
    if (isM0 && isBL) this.cxm0fb |= 0x40;
    if (isM1 && isPF) this.cxm1fb |= 0x80;
    if (isM1 && isBL) this.cxm1fb |= 0x40;
    if (isBL && isPF) this.cxblpf |= 0x80;
    if (isP0 && isP1) this.cxppmm |= 0x80;
    if (isM0 && isM1) this.cxppmm |= 0x40;

    // Color selection based on priority
    let r = bgCol[0], g = bgCol[1], b = bgCol[2];

    if (priority) {
      // PF/BL have priority over Players/Missiles
      if (isPF || isBL) {
        if (scoreMode && x < 80) { r = p0Col[0]; g = p0Col[1]; b = p0Col[2]; }
        else if (scoreMode && x >= 80) { r = p1Col[0]; g = p1Col[1]; b = p1Col[2]; }
        else { r = pfCol[0]; g = pfCol[1]; b = pfCol[2]; }
      } else if (isP0 || isM0) {
        r = p0Col[0]; g = p0Col[1]; b = p0Col[2];
      } else if (isP1 || isM1) {
        r = p1Col[0]; g = p1Col[1]; b = p1Col[2];
      }
    } else {
      // Players/Missiles have priority over PF/BL
      if (isP0 || isM0) {
        r = p0Col[0]; g = p0Col[1]; b = p0Col[2];
      } else if (isP1 || isM1) {
        r = p1Col[0]; g = p1Col[1]; b = p1Col[2];
      } else if (isPF || isBL) {
        if (scoreMode && x < 80) { r = p0Col[0]; g = p0Col[1]; b = p0Col[2]; }
        else if (scoreMode && x >= 80) { r = p1Col[0]; g = p1Col[1]; b = p1Col[2]; }
        else { r = pfCol[0]; g = pfCol[1]; b = pfCol[2]; }
      }
    }

    this.imageData.data[offset + x * 4] = r;
    this.imageData.data[offset + x * 4 + 1] = g;
    this.imageData.data[offset + x * 4 + 2] = b;
  }

  isPlayerPixel(x: number, p_x: number, nusiz: number, grp: number, refp: number): boolean {
    const n = nusiz & 7;
    const size = (n === 5) ? 2 : (n === 7) ? 4 : 1;
    const copies = (n === 1 || n === 2 || n === 4) ? 2 : (n === 3 || n === 6) ? 3 : 1;
    const spacing = (n === 1 || n === 3) ? 16 : (n === 2 || n === 6) ? 32 : (n === 4) ? 64 : 0;

    for (let i = 0; i < copies; i++) {
      const startX = (p_x + i * spacing) % 160;
      const dx = (x - startX + 160) % 160;
      if (dx < 8 * size) {
        const px = Math.floor(dx / size);
        const bit = (refp & 8) ? px : 7 - px;
        if ((grp & (1 << bit)) !== 0) return true;
      }
    }
    return false;
  }

  isMissilePixel(x: number, m_x: number, nusiz: number, enam: number, resmp: number): boolean {
    if (!enam || resmp) return false;
    const size = 1 << ((nusiz >> 4) & 3); // 1, 2, 4, 8
    const n = nusiz & 7;
    const copies = (n === 1 || n === 2 || n === 4) ? 2 : (n === 3 || n === 6) ? 3 : 1;
    const spacing = (n === 1 || n === 3) ? 16 : (n === 2 || n === 6) ? 32 : (n === 4) ? 64 : 0;

    for (let i = 0; i < copies; i++) {
      const startX = (m_x + i * spacing) % 160;
      const dx = (x - startX + 160) % 160;
      if (dx < size) return true;
    }
    return false;
  }

  isBallPixel(x: number, bl_x: number, ctrlpf: number, enabl: number): boolean {
    if (!enabl) return false;
    const size = 1 << ((ctrlpf >> 4) & 3); // 1, 2, 4, 8
    const dx = (x - bl_x + 160) % 160;
    return dx < size;
  }

  applyHMOVE() {
    // HMOVE shifts objects based on their HMxx registers (4-bit signed, -8 to +7)
    // TIA shifts are weird: positive values shift LEFT, negative shift RIGHT.
    const shift = (val: number) => {
      let s = val;
      if (s > 7) s -= 16;
      return -s; // Invert because TIA logic
    };
    this.p0_x = (this.p0_x + shift(this.hmp0) + 160) % 160;
    this.p1_x = (this.p1_x + shift(this.hmp1) + 160) % 160;
    this.m0_x = this.resmp0 ? (this.p0_x + 4) % 160 : (this.m0_x + shift(this.hmm0) + 160) % 160;
    this.m1_x = this.resmp1 ? (this.p1_x + 4) % 160 : (this.m1_x + shift(this.hmm1) + 160) % 160;
    this.bl_x = (this.bl_x + shift(this.hmbl) + 160) % 160;
    
    this.hmoveBlank = true;
  }

  clearScreen() {
    this.imageData.data.fill(0);
    for (let i = 3; i < this.imageData.data.length; i += 4) this.imageData.data[i] = 255;
    this.ctx.putImageData(this.imageData, 0, 0);
  }

  render() {
    this.ctx.putImageData(this.imageData, 0, 0);
  }

  getNTSCColor(col: number): [number, number, number] {
    // Atari 2600 NTSC Palette approximation
    const palette = [
      [0,0,0], [68,68,68], [136,136,136], [204,204,204], [255,255,255], [255,255,255], [255,255,255], [255,255,255],
      [68,17,0], [102,34,0], [136,51,0], [170,68,0], [204,85,0], [238,102,0], [255,119,0], [255,136,17],
      [85,0,0], [119,0,0], [153,0,0], [187,0,0], [221,0,0], [255,0,0], [255,34,34], [255,68,68],
      [85,0,34], [119,0,51], [153,0,68], [187,0,85], [221,0,102], [255,0,119], [255,34,136], [255,68,153],
      [68,0,68], [102,0,102], [136,0,136], [170,0,170], [204,0,204], [238,0,238], [255,34,255], [255,68,255],
      [34,0,85], [51,0,119], [68,0,153], [85,0,187], [102,0,221], [119,0,255], [136,34,255], [153,68,255],
      [0,0,102], [0,0,136], [0,0,170], [0,0,204], [0,0,238], [0,0,255], [34,34,255], [68,68,255],
      [0,17,102], [0,34,136], [0,51,170], [0,68,204], [0,85,238], [0,102,255], [34,119,255], [68,136,255],
      [0,34,85], [0,51,119], [0,68,153], [0,85,187], [0,102,221], [0,119,255], [34,136,255], [68,153,255],
      [0,51,68], [0,68,102], [0,85,136], [0,102,170], [0,119,204], [0,136,238], [34,153,255], [68,170,255],
      [0,68,34], [0,85,51], [0,102,68], [0,119,85], [0,136,102], [0,153,119], [34,170,136], [68,187,153],
      [0,68,0], [0,102,0], [0,136,0], [0,170,0], [0,204,0], [0,238,0], [34,255,34], [68,255,68],
      [17,68,0], [34,102,0], [51,136,0], [68,170,0], [85,204,0], [102,238,0], [119,255,34], [136,255,68],
      [34,68,0], [51,102,0], [68,136,0], [85,170,0], [102,204,0], [119,238,0], [136,255,34], [153,255,68],
      [51,68,0], [68,102,0], [85,136,0], [102,170,0], [119,204,0], [136,238,0], [153,255,34], [170,255,68],
      [68,51,0], [102,68,0], [136,85,0], [170,102,0], [204,119,0], [238,136,0], [255,153,34], [255,170,68]
    ];
    
    // col is 0-255. The upper 4 bits are the hue (0-15), the lower 4 bits are the luminance (0-15).
    // The palette above has 16 hues, each with 8 luminances.
    // We map the 16 luminances to the 8 available by dividing by 2.
    const hue = (col & 0xF0) >> 4;
    const luma = (col & 0x0E) >> 1; // 0-7
    const index = hue * 8 + luma;
    
    if (index >= 0 && index < 128) {
      const c = palette[index];
      return [c[0], c[1], c[2]];
    }
    return [0, 0, 0];
  }
}
