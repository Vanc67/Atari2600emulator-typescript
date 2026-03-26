export class CPU6507 {
  A = 0; X = 0; Y = 0; PC = 0; SP = 0xFF;
  N = false; V = false; B = false; D = false; I = true; Z = false; C = false;
  
  cycles = 0;
  halted = false;
  
  readFn: (addr: number) => number;
  writeFn: (addr: number, val: number) => void;

  constructor(readFn: (addr: number) => number, writeFn: (addr: number, val: number) => void) {
    this.readFn = readFn;
    this.writeFn = writeFn;
  }

  reset() {
    this.A = 0; this.X = 0; this.Y = 0; this.SP = 0xFD; this.I = true;
    this.PC = this.read16(0xFFFC);
    this.cycles = 0;
    this.halted = false;
  }

  read(addr: number) { return this.readFn(addr); }
  write(addr: number, val: number) { this.writeFn(addr, val); }
  read16(addr: number) { return this.read(addr) | (this.read(addr + 1) << 8); }
  
  push(val: number) { this.write(0x0100 | this.SP, val); this.SP = (this.SP - 1) & 0xFF; }
  pop() { this.SP = (this.SP + 1) & 0xFF; return this.read(0x0100 | this.SP); }
  push16(val: number) { this.push((val >> 8) & 0xFF); this.push(val & 0xFF); }
  pop16() { const lo = this.pop(); const hi = this.pop(); return (hi << 8) | lo; }

  getFlags() {
    return (this.N ? 0x80 : 0) | (this.V ? 0x40 : 0) | 0x20 | (this.B ? 0x10 : 0) |
           (this.D ? 0x08 : 0) | (this.I ? 0x04 : 0) | (this.Z ? 0x02 : 0) | (this.C ? 0x01 : 0);
  }
  setFlags(val: number) {
    this.N = (val & 0x80) !== 0; this.V = (val & 0x40) !== 0; this.B = (val & 0x10) !== 0;
    this.D = (val & 0x08) !== 0; this.I = (val & 0x04) !== 0; this.Z = (val & 0x02) !== 0; this.C = (val & 0x01) !== 0;
  }
  updateNZ(val: number) { this.Z = (val === 0); this.N = (val & 0x80) !== 0; }

  IMM() { const a = this.PC; this.PC = (this.PC + 1) & 0xFFFF; return a; }
  ZP() { return this.read(this.IMM()); }
  ZPX() { return (this.ZP() + this.X) & 0xFF; }
  ZPY() { return (this.ZP() + this.Y) & 0xFF; }
  ABS() { const a = this.read16(this.PC); this.PC = (this.PC + 2) & 0xFFFF; return a; }
  ABSX(checkPage = false) { 
    const base = this.ABS(); 
    const addr = (base + this.X) & 0xFFFF; 
    if (checkPage && (base & 0xFF00) !== (addr & 0xFF00)) this.cycles++; 
    return addr; 
  }
  ABSY(checkPage = false) { 
    const base = this.ABS(); 
    const addr = (base + this.Y) & 0xFFFF; 
    if (checkPage && (base & 0xFF00) !== (addr & 0xFF00)) this.cycles++; 
    return addr; 
  }
  INDX() { const ptr = (this.read(this.IMM()) + this.X) & 0xFF; return this.read(ptr) | (this.read((ptr + 1) & 0xFF) << 8); }
  INDY(checkPage = false) { 
    const ptr = this.read(this.IMM()); 
    const base = this.read(ptr) | (this.read((ptr + 1) & 0xFF) << 8);
    const addr = (base + this.Y) & 0xFFFF;
    if (checkPage && (base & 0xFF00) !== (addr & 0xFF00)) this.cycles++;
    return addr; 
  }
  REL() { const offset = this.read(this.IMM()); return offset < 0x80 ? offset : offset - 0x100; }

  step(): number {
    if (this.halted) return 1;
    const startCycles = this.cycles;
    const opcode = this.read(this.PC);
    this.PC = (this.PC + 1) & 0xFFFF;
    this.execute(opcode);
    return this.cycles - startCycles;
  }

  branch(cond: boolean, offset: number) {
    if (cond) {
      this.cycles++;
      const oldPC = this.PC;
      const signedOffset = offset > 127 ? offset - 256 : offset;
      this.PC = (this.PC + signedOffset) & 0xFFFF;
      if ((oldPC & 0xFF00) !== (this.PC & 0xFF00)) this.cycles++;
    }
  }

  execute(op: number) {
    let addr = 0, val = 0, tmp = 0;
    this.cycles += 2;

    switch (op) {
      // LDA
      case 0xA9: this.A = this.read(this.IMM()); this.updateNZ(this.A); break;
      case 0xA5: this.A = this.read(this.ZP()); this.updateNZ(this.A); this.cycles++; break;
      case 0xB5: this.A = this.read(this.ZPX()); this.updateNZ(this.A); this.cycles+=2; break;
      case 0xAD: this.A = this.read(this.ABS()); this.updateNZ(this.A); this.cycles+=2; break;
      case 0xBD: this.A = this.read(this.ABSX(true)); this.updateNZ(this.A); this.cycles+=2; break;
      case 0xB9: this.A = this.read(this.ABSY(true)); this.updateNZ(this.A); this.cycles+=2; break;
      case 0xA1: this.A = this.read(this.INDX()); this.updateNZ(this.A); this.cycles+=4; break;
      case 0xB1: this.A = this.read(this.INDY(true)); this.updateNZ(this.A); this.cycles+=3; break;

      // LDX
      case 0xA2: this.X = this.read(this.IMM()); this.updateNZ(this.X); break;
      case 0xA6: this.X = this.read(this.ZP()); this.updateNZ(this.X); this.cycles++; break;
      case 0xB6: this.X = this.read(this.ZPY()); this.updateNZ(this.X); this.cycles+=2; break;
      case 0xAE: this.X = this.read(this.ABS()); this.updateNZ(this.X); this.cycles+=2; break;
      case 0xBE: this.X = this.read(this.ABSY(true)); this.updateNZ(this.X); this.cycles+=2; break;

      // LDY
      case 0xA0: this.Y = this.read(this.IMM()); this.updateNZ(this.Y); break;
      case 0xA4: this.Y = this.read(this.ZP()); this.updateNZ(this.Y); this.cycles++; break;
      case 0xB4: this.Y = this.read(this.ZPX()); this.updateNZ(this.Y); this.cycles+=2; break;
      case 0xAC: this.Y = this.read(this.ABS()); this.updateNZ(this.Y); this.cycles+=2; break;
      case 0xBC: this.Y = this.read(this.ABSX(true)); this.updateNZ(this.Y); this.cycles+=2; break;

      // STA
      case 0x85: addr = this.ZP(); this.cycles++; this.write(addr, this.A); break;
      case 0x95: addr = this.ZPX(); this.cycles+=2; this.write(addr, this.A); break;
      case 0x8D: addr = this.ABS(); this.cycles+=2; this.write(addr, this.A); break;
      case 0x9D: addr = this.ABSX(); this.cycles+=3; this.write(addr, this.A); break;
      case 0x99: addr = this.ABSY(); this.cycles+=3; this.write(addr, this.A); break;
      case 0x81: addr = this.INDX(); this.cycles+=4; this.write(addr, this.A); break;
      case 0x91: addr = this.INDY(); this.cycles+=4; this.write(addr, this.A); break;

      // STX
      case 0x86: addr = this.ZP(); this.cycles++; this.write(addr, this.X); break;
      case 0x96: addr = this.ZPY(); this.cycles+=2; this.write(addr, this.X); break;
      case 0x8E: addr = this.ABS(); this.cycles+=2; this.write(addr, this.X); break;

      // STY
      case 0x84: addr = this.ZP(); this.cycles++; this.write(addr, this.Y); break;
      case 0x94: addr = this.ZPX(); this.cycles+=2; this.write(addr, this.Y); break;
      case 0x8C: addr = this.ABS(); this.cycles+=2; this.write(addr, this.Y); break;

      // ADC
      case 0x69: case 0x65: case 0x75: case 0x6D: case 0x7D: case 0x79: case 0x61: case 0x71:
        if(op===0x69) val = this.read(this.IMM());
        else if(op===0x65) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0x75) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0x6D) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0x7D) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0x79) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0x61) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0x71) { val = this.read(this.INDY(true)); this.cycles+=3; }
        
        if (this.D) {
          let al = (this.A & 0x0F) + (val & 0x0F) + (this.C ? 1 : 0);
          let ah = (this.A >> 4) + (val >> 4) + (al > 0x09 ? 1 : 0);
          tmp = this.A + val + (this.C ? 1 : 0);
          this.Z = (tmp & 0xFF) === 0;
          this.N = (ah & 0x08) !== 0;
          this.V = ((this.A ^ tmp) & (val ^ tmp) & 0x80) !== 0; // V flag is based on binary result
          if (al > 0x09) al += 0x06;
          if (ah > 0x09) ah += 0x06;
          this.C = ah > 0x0F;
          this.A = ((ah << 4) | (al & 0x0F)) & 0xFF;
        } else {
          tmp = this.A + val + (this.C ? 1 : 0);
          this.V = ((this.A ^ tmp) & (val ^ tmp) & 0x80) !== 0;
          this.C = tmp > 0xFF;
          this.A = tmp & 0xFF;
          this.updateNZ(this.A);
        }
        break;

      // SBC
      case 0xE9: case 0xE5: case 0xF5: case 0xED: case 0xFD: case 0xF9: case 0xE1: case 0xF1:
        if(op===0xE9) val = this.read(this.IMM());
        else if(op===0xE5) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0xF5) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0xED) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0xFD) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0xF9) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0xE1) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0xF1) { val = this.read(this.INDY(true)); this.cycles+=3; }
        
        if (this.D) {
          let al = (this.A & 0x0F) - (val & 0x0F) - (this.C ? 0 : 1);
          let ah = (this.A >> 4) - (val >> 4) - (al < 0 ? 1 : 0);
          tmp = this.A - val - (this.C ? 0 : 1);
          this.Z = (tmp & 0xFF) === 0;
          this.N = (tmp & 0x80) !== 0;
          this.V = ((this.A ^ tmp) & (this.A ^ val) & 0x80) !== 0;
          if (al < 0) al -= 0x06;
          if (ah < 0) ah -= 0x06;
          this.C = tmp >= 0;
          this.A = ((ah << 4) | (al & 0x0F)) & 0xFF;
        } else {
          val = val ^ 0xFF;
          tmp = this.A + val + (this.C ? 1 : 0);
          this.V = ((this.A ^ tmp) & (val ^ tmp) & 0x80) !== 0;
          this.C = tmp > 0xFF;
          this.A = tmp & 0xFF;
          this.updateNZ(this.A);
        }
        break;

      // CMP
      case 0xC9: case 0xC5: case 0xD5: case 0xCD: case 0xDD: case 0xD9: case 0xC1: case 0xD1:
        if(op===0xC9) val = this.read(this.IMM());
        else if(op===0xC5) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0xD5) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0xCD) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0xDD) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0xD9) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0xC1) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0xD1) { val = this.read(this.INDY(true)); this.cycles+=3; }
        tmp = this.A - val;
        this.C = this.A >= val;
        this.updateNZ(tmp & 0xFF);
        break;

      // CPX
      case 0xE0: val = this.read(this.IMM()); tmp = this.X - val; this.C = this.X >= val; this.updateNZ(tmp & 0xFF); break;
      case 0xE4: val = this.read(this.ZP()); this.cycles++; tmp = this.X - val; this.C = this.X >= val; this.updateNZ(tmp & 0xFF); break;
      case 0xEC: val = this.read(this.ABS()); this.cycles+=2; tmp = this.X - val; this.C = this.X >= val; this.updateNZ(tmp & 0xFF); break;

      // CPY
      case 0xC0: val = this.read(this.IMM()); tmp = this.Y - val; this.C = this.Y >= val; this.updateNZ(tmp & 0xFF); break;
      case 0xC4: val = this.read(this.ZP()); this.cycles++; tmp = this.Y - val; this.C = this.Y >= val; this.updateNZ(tmp & 0xFF); break;
      case 0xCC: val = this.read(this.ABS()); this.cycles+=2; tmp = this.Y - val; this.C = this.Y >= val; this.updateNZ(tmp & 0xFF); break;

      // AND
      case 0x29: case 0x25: case 0x35: case 0x2D: case 0x3D: case 0x39: case 0x21: case 0x31:
        if(op===0x29) val = this.read(this.IMM());
        else if(op===0x25) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0x35) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0x2D) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0x3D) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0x39) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0x21) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0x31) { val = this.read(this.INDY(true)); this.cycles+=3; }
        this.A &= val; this.updateNZ(this.A); break;

      // ORA
      case 0x09: case 0x05: case 0x15: case 0x0D: case 0x1D: case 0x19: case 0x01: case 0x11:
        if(op===0x09) val = this.read(this.IMM());
        else if(op===0x05) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0x15) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0x0D) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0x1D) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0x19) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0x01) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0x11) { val = this.read(this.INDY(true)); this.cycles+=3; }
        this.A |= val; this.updateNZ(this.A); break;

      // EOR
      case 0x49: case 0x45: case 0x55: case 0x4D: case 0x5D: case 0x59: case 0x41: case 0x51:
        if(op===0x49) val = this.read(this.IMM());
        else if(op===0x45) { val = this.read(this.ZP()); this.cycles++; }
        else if(op===0x55) { val = this.read(this.ZPX()); this.cycles+=2; }
        else if(op===0x4D) { val = this.read(this.ABS()); this.cycles+=2; }
        else if(op===0x5D) { val = this.read(this.ABSX(true)); this.cycles+=2; }
        else if(op===0x59) { val = this.read(this.ABSY(true)); this.cycles+=2; }
        else if(op===0x41) { val = this.read(this.INDX()); this.cycles+=4; }
        else if(op===0x51) { val = this.read(this.INDY(true)); this.cycles+=3; }
        this.A ^= val; this.updateNZ(this.A); break;

      // BIT
      case 0x24: addr = this.ZP(); val = this.read(addr); this.Z = (this.A & val) === 0; this.N = (val & 0x80) !== 0; this.V = (val & 0x40) !== 0; this.cycles++; break;
      case 0x2C: addr = this.ABS(); val = this.read(addr); this.Z = (this.A & val) === 0; this.N = (val & 0x80) !== 0; this.V = (val & 0x40) !== 0; this.cycles+=2; break;

      // INC
      case 0xE6: addr = this.ZP(); val = (this.read(addr) + 1) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0xF6: addr = this.ZPX(); val = (this.read(addr) + 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0xEE: addr = this.ABS(); val = (this.read(addr) + 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0xFE: addr = this.ABSX(); val = (this.read(addr) + 1) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // DEC
      case 0xC6: addr = this.ZP(); val = (this.read(addr) - 1) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0xD6: addr = this.ZPX(); val = (this.read(addr) - 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0xCE: addr = this.ABS(); val = (this.read(addr) - 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0xDE: addr = this.ABSX(); val = (this.read(addr) - 1) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // ASL
      case 0x0A: this.C = (this.A & 0x80) !== 0; this.A = (this.A << 1) & 0xFF; this.updateNZ(this.A); break;
      case 0x06: addr = this.ZP(); val = this.read(addr); this.C = (val & 0x80) !== 0; val = (val << 1) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0x16: addr = this.ZPX(); val = this.read(addr); this.C = (val & 0x80) !== 0; val = (val << 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x0E: addr = this.ABS(); val = this.read(addr); this.C = (val & 0x80) !== 0; val = (val << 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x1E: addr = this.ABSX(); val = this.read(addr); this.C = (val & 0x80) !== 0; val = (val << 1) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // LSR
      case 0x4A: this.C = (this.A & 0x01) !== 0; this.A = (this.A >> 1) & 0xFF; this.updateNZ(this.A); break;
      case 0x46: addr = this.ZP(); val = this.read(addr); this.C = (val & 0x01) !== 0; val = (val >> 1) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0x56: addr = this.ZPX(); val = this.read(addr); this.C = (val & 0x01) !== 0; val = (val >> 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x4E: addr = this.ABS(); val = this.read(addr); this.C = (val & 0x01) !== 0; val = (val >> 1) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x5E: addr = this.ABSX(); val = this.read(addr); this.C = (val & 0x01) !== 0; val = (val >> 1) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // ROL
      case 0x2A: tmp = this.C ? 1 : 0; this.C = (this.A & 0x80) !== 0; this.A = ((this.A << 1) | tmp) & 0xFF; this.updateNZ(this.A); break;
      case 0x26: addr = this.ZP(); val = this.read(addr); tmp = this.C ? 1 : 0; this.C = (val & 0x80) !== 0; val = ((val << 1) | tmp) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0x36: addr = this.ZPX(); val = this.read(addr); tmp = this.C ? 1 : 0; this.C = (val & 0x80) !== 0; val = ((val << 1) | tmp) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x2E: addr = this.ABS(); val = this.read(addr); tmp = this.C ? 1 : 0; this.C = (val & 0x80) !== 0; val = ((val << 1) | tmp) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x3E: addr = this.ABSX(); val = this.read(addr); tmp = this.C ? 1 : 0; this.C = (val & 0x80) !== 0; val = ((val << 1) | tmp) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // ROR
      case 0x6A: tmp = this.C ? 0x80 : 0; this.C = (this.A & 0x01) !== 0; this.A = ((this.A >> 1) | tmp) & 0xFF; this.updateNZ(this.A); break;
      case 0x66: addr = this.ZP(); val = this.read(addr); tmp = this.C ? 0x80 : 0; this.C = (val & 0x01) !== 0; val = ((val >> 1) | tmp) & 0xFF; this.cycles+=3; this.write(addr, val); this.updateNZ(val); break;
      case 0x76: addr = this.ZPX(); val = this.read(addr); tmp = this.C ? 0x80 : 0; this.C = (val & 0x01) !== 0; val = ((val >> 1) | tmp) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x6E: addr = this.ABS(); val = this.read(addr); tmp = this.C ? 0x80 : 0; this.C = (val & 0x01) !== 0; val = ((val >> 1) | tmp) & 0xFF; this.cycles+=4; this.write(addr, val); this.updateNZ(val); break;
      case 0x7E: addr = this.ABSX(); val = this.read(addr); tmp = this.C ? 0x80 : 0; this.C = (val & 0x01) !== 0; val = ((val >> 1) | tmp) & 0xFF; this.cycles+=5; this.write(addr, val); this.updateNZ(val); break;

      // Branches
      case 0x90: this.branch(!this.C, this.REL()); break; // BCC
      case 0xB0: this.branch(this.C, this.REL()); break;  // BCS
      case 0xF0: this.branch(this.Z, this.REL()); break;  // BEQ
      case 0x30: this.branch(this.N, this.REL()); break;  // BMI
      case 0xD0: this.branch(!this.Z, this.REL()); break; // BNE
      case 0x10: this.branch(!this.N, this.REL()); break; // BPL
      case 0x50: this.branch(!this.V, this.REL()); break; // BVC
      case 0x70: this.branch(this.V, this.REL()); break;  // BVS

      // Jumps & Calls
      case 0x4C: this.PC = this.ABS(); this.cycles++; break; // JMP ABS
      case 0x6C: // JMP IND
        addr = this.ABS();
        if ((addr & 0xFF) === 0xFF) this.PC = this.read(addr) | (this.read(addr - 0xFF) << 8);
        else this.PC = this.read(addr) | (this.read(addr + 1) << 8);
        this.cycles+=3;
        break;
      case 0x20: // JSR
        addr = this.ABS();
        this.push16(this.PC - 1);
        this.PC = addr;
        this.cycles+=4;
        break;
      case 0x60: // RTS
        this.PC = (this.pop16() + 1) & 0xFFFF;
        this.cycles+=4;
        break;
      case 0x40: // RTI
        this.setFlags(this.pop());
        this.PC = this.pop16();
        this.cycles+=4;
        break;

      // Register Instructions
      case 0xAA: this.X = this.A; this.updateNZ(this.X); break; // TAX
      case 0xA8: this.Y = this.A; this.updateNZ(this.Y); break; // TAY
      case 0x8A: this.A = this.X; this.updateNZ(this.A); break; // TXA
      case 0x98: this.A = this.Y; this.updateNZ(this.A); break; // TYA
      case 0xBA: this.X = this.SP; this.updateNZ(this.X); break; // TSX
      case 0x9A: this.SP = this.X; break; // TXS
      case 0xE8: this.X = (this.X + 1) & 0xFF; this.updateNZ(this.X); break; // INX
      case 0xC8: this.Y = (this.Y + 1) & 0xFF; this.updateNZ(this.Y); break; // INY
      case 0xCA: this.X = (this.X - 1) & 0xFF; this.updateNZ(this.X); break; // DEX
      case 0x88: this.Y = (this.Y - 1) & 0xFF; this.updateNZ(this.Y); break; // DEY

      // Stack Instructions
      case 0x48: this.push(this.A); this.cycles++; break; // PHA
      case 0x08: this.push(this.getFlags() | 0x10); this.cycles++; break; // PHP
      case 0x68: this.A = this.pop(); this.updateNZ(this.A); this.cycles+=2; break; // PLA
      case 0x28: this.setFlags(this.pop()); this.cycles+=2; break; // PLP

      // Flag Instructions
      case 0x18: this.C = false; break; // CLC
      case 0x38: this.C = true; break;  // SEC
      case 0x58: this.I = false; break; // CLI
      case 0x78: this.I = true; break;  // SEI
      case 0xB8: this.V = false; break; // CLV
      case 0xD8: this.D = false; break; // CLD
      case 0xF8: this.D = true; break;  // SED

      // System
      case 0x00: // BRK
        this.PC++;
        this.push16(this.PC);
        this.push(this.getFlags() | 0x10);
        this.I = true;
        this.PC = this.read16(0xFFFE);
        this.cycles+=5;
        break;
      case 0xEA: // NOP
        break;

      // Illegal Opcodes
      // LAX
      case 0xA7: val = this.read(this.ZP()); this.cycles++; this.A = val; this.X = val; this.updateNZ(val); break;
      case 0xB7: val = this.read(this.ZPY()); this.cycles+=2; this.A = val; this.X = val; this.updateNZ(val); break;
      case 0xAF: val = this.read(this.ABS()); this.cycles+=2; this.A = val; this.X = val; this.updateNZ(val); break;
      case 0xBF: val = this.read(this.ABSY(true)); this.cycles+=2; this.A = val; this.X = val; this.updateNZ(val); break;
      case 0xA3: val = this.read(this.INDX()); this.cycles+=4; this.A = val; this.X = val; this.updateNZ(val); break;
      case 0xB3: val = this.read(this.INDY(true)); this.cycles+=3; this.A = val; this.X = val; this.updateNZ(val); break;

      // SAX
      case 0x87: addr = this.ZP(); this.cycles++; this.write(addr, this.A & this.X); break;
      case 0x97: addr = this.ZPY(); this.cycles+=2; this.write(addr, this.A & this.X); break;
      case 0x8F: addr = this.ABS(); this.cycles+=2; this.write(addr, this.A & this.X); break;
      case 0x83: addr = this.INDX(); this.cycles+=4; this.write(addr, this.A & this.X); break;

      // DCP
      case 0xC7: case 0xD7: case 0xCF: case 0xDF: case 0xDB: case 0xC3: case 0xD3:
        if(op===0xC7) { addr = this.ZP(); this.cycles+=3; }
        else if(op===0xD7) { addr = this.ZPX(); this.cycles+=4; }
        else if(op===0xCF) { addr = this.ABS(); this.cycles+=4; }
        else if(op===0xDF) { addr = this.ABSX(); this.cycles+=5; }
        else if(op===0xDB) { addr = this.ABSY(); this.cycles+=5; }
        else if(op===0xC3) { addr = this.INDX(); this.cycles+=6; }
        else if(op===0xD3) { addr = this.INDY(); this.cycles+=6; }
        val = (this.read(addr) - 1) & 0xFF;
        this.write(addr, val);
        tmp = this.A - val;
        this.C = this.A >= val;
        this.updateNZ(tmp & 0xFF);
        break;

      // ISC
      case 0xE7: case 0xF7: case 0xEF: case 0xFF: case 0xFB: case 0xE3: case 0xF3:
        if(op===0xE7) { addr = this.ZP(); this.cycles+=3; }
        else if(op===0xF7) { addr = this.ZPX(); this.cycles+=4; }
        else if(op===0xEF) { addr = this.ABS(); this.cycles+=4; }
        else if(op===0xFF) { addr = this.ABSX(); this.cycles+=5; }
        else if(op===0xFB) { addr = this.ABSY(); this.cycles+=5; }
        else if(op===0xE3) { addr = this.INDX(); this.cycles+=6; }
        else if(op===0xF3) { addr = this.INDY(); this.cycles+=6; }
        val = (this.read(addr) + 1) & 0xFF;
        this.write(addr, val);
        val ^= 0xFF;
        tmp = this.A + val + (this.C ? 1 : 0);
        this.V = ((this.A ^ tmp) & (val ^ tmp) & 0x80) !== 0;
        this.C = tmp > 0xFF;
        this.A = tmp & 0xFF;
        this.updateNZ(this.A);
        break;

      default:
        break;
    }
  }
}
