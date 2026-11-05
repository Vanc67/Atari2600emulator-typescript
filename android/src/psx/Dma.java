package psx;

/** DMA controller: seven channels, block/manual/linked-list modes. */
public final class Dma {

    private final int[] madr = new int[7];
    private final int[] bcr = new int[7];
    private final int[] chcr = new int[7];
    public int dpcr, dicr;
    public Bus bus;
    public Irq irq;

    public void reset() {
        for (int i = 0; i < 7; i++) {
            madr[i] = 0;
            bcr[i] = 0;
            chcr[i] = 0;
        }
        dpcr = 0x07654321;
        dicr = 0;
    }

    public int read(int a) {
        int off = (a - 0x1F801080) & 0x7F;
        if (off >= 0x70) {
            return off == 0x70 ? dpcr : dicr;
        }
        int ch = off >> 4;
        switch (off & 0xF) {
            case 0: return madr[ch];
            case 4: return bcr[ch];
            case 8: return chcr[ch];
            default: return 0;
        }
    }

    public void write(int a, int v) {
        int off = (a - 0x1F801080) & 0x7F;
        if (off >= 0x70) {
            if (off == 0x70) dpcr = v;
            else dicr = v;
            return;
        }
        int ch = off >> 4;
        switch (off & 0xF) {
            case 0: madr[ch] = v & 0x00FFFFFF; break;
            case 4: bcr[ch] = v; break;
            case 8:
                chcr[ch] = v;
                if ((v & 0x01000000) != 0) run(ch);
                break;
            default: break;
        }
    }

    private void run(int ch) {
        int mad = madr[ch] & 0x1FFFFC;
        int cfg = chcr[ch];
        int sync = (cfg >>> 9) & 3;
        int words;
        if (ch == 2 && sync == 2) {
            /* linked list */
            int guard = 0;
            while ((mad & 0x800000) == 0 && guard++ < 65536) {
                int header = bus.read32(mad & 0x1FFFFC);
                int count = header >>> 24;
                int next = header & 0xFFFFFF;
                for (int i = 0; i < count; i++) {
                    mad = (mad + 4) & 0x1FFFFC;
                    bus.gpu.writeGP0(bus.read32(mad));
                }
                mad = next & 0x1FFFFC;
            }
            finish(ch);
            return;
        }
        int blk = bcr[ch];
        int blocks = (blk >>> 16) & 0xFFFF;
        int size = blk & 0xFFFF;
        if (blocks == 0) words = size;
        else words = blocks * size;
        if (words <= 0) { finish(ch); return; }

        boolean toRam = (cfg & 0x02) == 0;
        switch (ch) {
            case 2:
                if (toRam) bus.dmaFromGpu(madr[ch], words);
                else bus.dmaToGpu(madr[ch], words);
                break;
            case 3:
                if (toRam) bus.dmaFromCdrom(madr[ch], words);
                break;
            case 4:
                bus.dmaToSpu(madr[ch], words, !toRam);
                break;
            case 6:
                /* ordering table: writes backwards pointers */
                for (int i = 0; i < words; i++) {
                    int addr = (madr[ch] + i * 4) & 0x1FFFFC;
                    int next = (i == words - 1) ? 0x00FFFFFF : ((addr - 4) & 0x1FFFFF);
                    bus.write32(addr, next);
                }
                break;
            default:
                break;
        }
        finish(ch);
    }

    private void finish(int ch) {
        chcr[ch] &= ~0x01000000;
        if ((dicr & (1 << (16 + ch))) != 0 && (dicr & (1 << ch)) != 0) {
            if (irq != null) irq.raise(Irq.DMA);
        }
        if (irq != null && (dicr & 0x80000000) != 0) irq.raise(Irq.DMA);
    }
}
