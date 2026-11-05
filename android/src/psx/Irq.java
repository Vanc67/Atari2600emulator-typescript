package psx;

/** Interrupt controller (I_STAT / I_MASK at 0x1F801070). */
public final class Irq {

    public static final int VBLANK = 0, GPU = 1, CDROM = 2, DMA = 3,
            TIMER0 = 4, TIMER1 = 5, TIMER2 = 6, PAD = 7, SIO = 8, SPU = 9, PIO = 10;

    public int stat, mask;
    public Cpu cpu;

    public void reset() {
        stat = 0;
        mask = 0;
    }

    public void raise(int bit) {
        stat |= 1 << bit;
        update();
    }

    public void acknowledge(int bits) {
        stat &= ~bits;
        update();
    }

    public void update() {
        if (cpu != null) cpu.irqLine = (stat & mask) != 0;
    }

    public int read(int a) {
        if ((a & 0xF) == 0) return stat & 0x7FF;
        return mask & 0x7FF;
    }

    public void write(int a, int v) {
        if ((a & 0xF) == 0) stat &= v & 0x7FF;      /* write 0 acknowledges */
        else mask = v & 0x7FF;
        update();
    }
}
