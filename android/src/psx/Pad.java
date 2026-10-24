package psx;

/**
 * SIO0 controller port with one digital pad (SCPH-1080 style).
 *
 * <p>Protocol: the CPU selects the port, then shifts out {@code 01 42 00 00 00}
 * and reads a byte back after every write. The answer is
 * {@code FF 41 5A <lo> <hi>}, where the two button bytes are active low.</p>
 *
 * <p>Two details matter for real games, and both are handled here:</p>
 * <ul>
 *   <li>a frame starts again whenever {@code 0x01} is written, so games that
 *       keep the port selected and simply repeat the sequence keep working;</li>
 *   <li>the ACK bit in {@link #stat} is a pulse, not a constant, and the
 *       "RX not empty" bit clears when the data register is read.</li>
 * </ul>
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

    /** Length of the ACK pulse in CPU cycles (a real pad answers in ~100). */
    private static final int ACK_PULSE = 200;

    /** Bit 7: /ACK input level (1 = idle). Bit 1: RX FIFO not empty. */
    private static final int STAT_ACK = 0x80;
    private static final int STAT_RX_READY = 0x02;

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

    /** Updates the pad state; callable from any thread. */
    public void setButtons(int b) {
        buttons = b;
    }

    /** True while the port is selected (JOY_CTRL bit 1 low). */
    public boolean isSelected() {
        return (control & 0x0002) == 0;
    }

    public int read(int a) {
        switch (a & 0xF) {
            case 0x0:
                /* reading the data register empties the RX FIFO */
                stat &= ~STAT_RX_READY;
                return rx;
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
                {
                    int old = control;
                    control = v & 0xFFFF;
                    if ((v & 0x40) != 0) {      /* acknowledge the interrupt */
                        stat &= ~STAT_ACK;
                    }
                    if ((v & 0x10) != 0) {      /* reset the port */
                        step = 0;
                        ackCycles = 0;
                        stat = 0x00000005;
                    }
                    /* A falling edge on /JOYn starts a new transfer. Some games
                       keep the line low and rely on the 0x01 byte instead. */
                    if ((old & 0x0002) != 0 && (v & 0x0002) == 0) {
                        step = 0;
                        stat |= STAT_ACK;
                    }
                    break;
                }
            case 0xE: baud = v & 0xFFFF; break;
            default: break;
        }
    }

    private void process(int b) {
        /* 0x01 is the address byte: it always starts a frame, even when the
           port was never deselected between two reads. */
        if (step > 0 && b == 0x01) step = 0;
        switch (step) {
            case 0:
                rx = 0xFF;
                break;
            case 1:
                rx = (b == 0x42) ? 0x41 : 0xFF;   /* pad id, or "no device" */
                break;
            case 2:
                rx = 0x5A;                        /* ready */
                break;
            case 3:
                rx = buttons & 0xFF;              /* select / start / dpad */
                break;
            case 4:
                rx = (buttons >>> 8) & 0xFF;      /* faces / shoulders */
                break;
            default:
                rx = 0xFF;
                break;
        }
        step++;
        if (step > 8) step = 0;
        stat |= 0x01;                             /* TX ready      */
        if (step > 0 && step <= 5) {
            stat |= STAT_ACK;                     /* ACK pulse     */
            stat |= STAT_RX_READY;
            ackCycles = ACK_PULSE;
        }
    }

    /** Advances the ACK timer; called once per emulated instruction. */
    public void step(int cycles) {
        if (ackCycles > 0) {
            ackCycles -= cycles;
            if (ackCycles <= 0) {
                ackCycles = 0;
                stat &= ~STAT_ACK;
            }
        }
    }
}
