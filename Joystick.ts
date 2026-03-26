export class Joystick {
  up: boolean = false;
  down: boolean = false;
  left: boolean = false;
  right: boolean = false;
  button: boolean = false;

  // Read SWCHA (Port A data register for joysticks)
  readSWCHA(): number {
    let val = 0xFF; // Active low
    if (this.up) val &= ~0x10;
    if (this.down) val &= ~0x20;
    if (this.left) val &= ~0x40;
    if (this.right) val &= ~0x80;
    return val;
  }

  // Read INPT4 (Player 1 button)
  readINPT4(): number {
    return this.button ? 0x00 : 0x80; // Active low, bit 7
  }
}
