package psx;

/** The three root counters at 0x1F801100. */
public final class Timers {

    private final int[] value = new int[3];
    private final int[] mode = new int[3];
    private final int[] target = new int[3];
    private final int[] frac = new int[3];
    private final int[] last = new int[3];
    public Irq irq;

    public void reset() {
        for (int i = 0; i < 3; i++) {
            value[i] = 0;
            mode[i] = 0;
            target[i] = 0;
            frac[i] = 0;
            last[i] = 0;
        }
    }

    public int read(int a) {
        int ch = (a >> 4) & 3;
        if (ch > 2) return 0;
        switch (a & 0xF) {
            case 0: return value[ch] & 0xFFFF;
            case 4: return mode[ch] & 0x7FF;
            case 8: return target[ch] & 0xFFFF;
            default: return 0;
        }
    }

    public void write(int a, int v) {
        int ch = (a >> 4) & 3;
        if (ch > 2) return;
        switch (a & 0xF) {
            case 0: value[ch] = v & 0xFFFF; break;
            case 4:
                mode[ch] = v & 0x7FF;
                if ((v & 0x10) != 0) irqCheckTarget(ch);
                break;
            case 8: target[ch] = v & 0xFFFF; break;
            default: break;
        }
    }

    private void irqCheckTarget(int ch) {
        /* target IRQ is raised by step() when the counter matches */
    }

    /** Advances all counters by {@code cpuCycles} CPU clocks. */
    public void step(int cpuCycles) {
        for (int ch = 0; ch < 3; ch++) {
            int m = mode[ch];
            int clockSrc = (m >>> 8) & 3;
            int inc;
            if (clockSrc == 3) {
                /* dot clock / 8: approximate with 1 per 8 cpu cycles */
                frac[ch] += cpuCycles;
                inc = frac[ch] >> 3;
                frac[ch] &= 7;
            } else {
                inc = cpuCycles;
            }
            if (inc == 0) continue;
            int old = value[ch];
            int nv = old + inc;
            boolean wrapped = nv > 0xFFFF;
            value[ch] = nv & 0xFFFF;
            if (target[ch] != 0 && old < target[ch] && value[ch] >= target[ch]) {
                if ((m & 0x10) != 0) {
                    irq.raise(Irq.TIMER0 + ch);
                    if ((m & 0x400) != 0) value[ch] = 0;
                }
            }
            if (wrapped) {
                if ((m & 0x20) != 0) irq.raise(Irq.TIMER0 + ch);
                if ((m & 0x08) != 0) value[ch] = 0;
            }
        }
    }
}
