package psx;

/**
 * SIO0 controller port. Implements just enough of the digital pad protocol
 * for games and the BIOS to detect a standard controller.
 */
public final class Pad {

    private int stat = 0x00000005;
    private int mode;
    private int baud;
    private int control;
    private int rx, tx;
    private int step;
    private int buttons1, buttons2;
    private int ackCycles;

    /* button state: 1 = pressed (active low on the wire) */
    public int buttons = 0xFFFF;

    public void reset() {
        stat = 0x00000005;
        mode = 0;
        baud = 0;
        control = 0;
        step = 0;
        buttons1 = 0xFF;
        buttons2 = 0xFF;
        ackCycles = 0;
    }

    public void setButtons(int b) {
        buttons = b;
    }

    public int read(int a) {
        switch (a & 0xF) {
            case 0x0: return rx;
            case 0x4: return stat;
            case 0x8: return mode;
            case 0xA: return control;
            case 0xE: return baud;
            default: return 0;
        }
    }

    public void write(int a, int v) {
        switch (a & 0xF) {
            case 0x0:
                tx = v & 0xFF;
                process(tx);
                break;
            case 0x8: mode = v & 0xFFFF; break;
            case 0xA:
                control = v & 0xFFFF;
                if ((v & 0x40) != 0) {          /* ACK */
                    stat &= ~0x80;
                }
                if ((v & 0x10) != 0) {          /* reset */
                    step = 0;
                    stat = 0x00000005;
                }
                if ((v & 0x02) != 0) {          /* select */
                    step = 0;
                }
                break;
            case 0xE: baud = v & 0xFFFF; break;
            default: break;
        }
    }

    private void process(int b) {
        switch (step) {
            case 0:
                if (b == 0x01) { rx = 0xFF; stat |= 0x80; }
                else rx = 0xFF;
                break;
            case 1:
                if (b == 0x42) { rx = 0x41; stat |= 0x80; }
                else rx = 0xFF;
                break;
            case 2:
                rx = 0x5A;
                stat |= 0x80;
                break;
            case 3:
                rx = buttons & 0xFF;
                stat |= 0x80;
                break;
            case 4:
                rx = (buttons >>> 8) & 0xFF;
                stat |= 0x80;
                break;
            default:
                rx = 0xFF;
                stat |= 0x80;
                break;
        }
        step++;
        if (step > 8) step = 0;
        stat |= 0x01; /* TX ready */
    }

    public void step(int cycles) {
        if (ackCycles > 0) {
            ackCycles -= cycles;
            if (ackCycles <= 0) stat &= ~0x80;
        }
    }
}
