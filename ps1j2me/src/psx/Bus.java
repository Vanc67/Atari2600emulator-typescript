package psx;

/**
 * PlayStation memory bus: 2 MiB main RAM, 512 KiB BIOS, 1 KiB scratchpad and
 * the whole 0x1F801000 I/O block (DMA, timers, GPU, SPU, CD-ROM, pads).
 *
 * Address decode is done once per access with cheap range tests; there is no
 * java.nio and no dynamic memory - all buffers are allocated up front.
 */
public final class Bus {

    public final int[] ram = new int[0x80000];      /* 2 MiB                */
    public final int[] bios = new int[0x20000];     /* 512 KiB              */
    public final int[] scratch = new int[256];      /* 1 KiB                */

    public final CodeCache codeCache;

    public Gpu gpu;
    public Spu spu;
    public Cdrom cdrom;
    public Dma dma;
    public Timers timers;
    public Pad pad;
    public Mdec mdec;
    public Irq irq;
    public Cpu cpu;

    /* memory control registers, kept only so that games can read them back */
    private final int[] memCtrl = new int[8];
    public int ramSizeReg = 0x00000B88;
    public int cacheControl = 0;

    private int lastLoadAddr;
    private int lastLoadValue;

    public Bus() {
        codeCache = new CodeCache(this);
        memCtrl[0] = 0x1F000000;
        memCtrl[1] = 0x1F802000;
        memCtrl[2] = 0x1F800000;
        memCtrl[3] = 0x1F801000;
        memCtrl[4] = 0x00000000;
        memCtrl[5] = 0x1F000000;
        memCtrl[6] = 0x00000000;
        memCtrl[7] = 0x00000000;
    }

    public void reset() {
        for (int i = 0; i < ram.length; i++) ram[i] = 0;
        for (int i = 0; i < scratch.length; i++) scratch[i] = 0;
        codeCache.flush();
        if (gpu != null) gpu.reset();
        if (spu != null) spu.reset();
        if (cdrom != null) cdrom.reset();
        if (dma != null) dma.reset();
        if (timers != null) timers.reset();
        if (pad != null) pad.reset();
        if (irq != null) irq.reset();
        if (mdec != null) mdec.reset();
        cacheControl = 0;
    }

    /* ---- 32 bit ---------------------------------------------------------- */

    public int read32(int addr) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) return ram[(a & 0x1FFFFF) >>> 2];
        if (a >= 0x1F801000 && a < 0x1F802000) return ioRead32(a & ~3);
        if (a >= 0x1F800000 && a < 0x1F800400) return scratch[(a - 0x1F800000) >>> 2];
        if (a >= 0x1FC00000) return bios[((a - 0x1FC00000) & 0x7FFFF) >>> 2];
        return 0xFFFFFFFF;
    }

    public void write32(int addr, int value) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int idx = (a & 0x1FFFFF) >>> 2;
            if (ram[idx] != value) {
                ram[idx] = value;
                codeCache.invalidate(a);
                if (cpu != null) cpu.noteCodeWrite(a);
            }
            return;
        }
        if (a >= 0x1F801000 && a < 0x1F802000) { ioWrite32(a & ~3, value); return; }
        if (a >= 0x1F800000 && a < 0x1F800400) { scratch[(a - 0x1F800000) >>> 2] = value; return; }
        /* BIOS writes are ignored */
    }

    /* ---- 16 bit ---------------------------------------------------------- */

    public int read16(int addr) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int w = ram[(a & 0x1FFFFF) >>> 2];
            return (a & 2) == 0 ? (w & 0xFFFF) : (w >>> 16) & 0xFFFF;
        }
        if (a >= 0x1F801000 && a < 0x1F802000) {
            int w = ioRead32(a & ~3);
            return (a & 2) == 0 ? (w & 0xFFFF) : (w >>> 16) & 0xFFFF;
        }
        if (a >= 0x1F800000 && a < 0x1F800400) {
            int w = scratch[(a - 0x1F800000) >>> 2];
            return (a & 2) == 0 ? (w & 0xFFFF) : (w >>> 16) & 0xFFFF;
        }
        if (a >= 0x1FC00000) {
            int w = bios[((a - 0x1FC00000) & 0x7FFFF) >>> 2];
            return (a & 2) == 0 ? (w & 0xFFFF) : (w >>> 16) & 0xFFFF;
        }
        return 0xFFFF;
    }

    public void write16(int addr, int value) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int idx = (a & 0x1FFFFF) >>> 2;
            int w = ram[idx];
            if ((a & 2) == 0) w = (w & 0xFFFF0000) | (value & 0xFFFF);
            else w = (w & 0xFFFF) | ((value & 0xFFFF) << 16);
            ram[idx] = w;
            codeCache.invalidate(a);
            if (cpu != null) cpu.noteCodeWrite(a);
            return;
        }
        if (a >= 0x1F801000 && a < 0x1F802000) {
            int w = ioRead32(a & ~3);
            if ((a & 2) == 0) w = (w & 0xFFFF0000) | (value & 0xFFFF);
            else w = (w & 0xFFFF) | ((value & 0xFFFF) << 16);
            ioWrite32(a & ~3, w);
            return;
        }
        if (a >= 0x1F800000 && a < 0x1F800400) {
            int idx = (a - 0x1F800000) >>> 2;
            int w = scratch[idx];
            if ((a & 2) == 0) w = (w & 0xFFFF0000) | (value & 0xFFFF);
            else w = (w & 0xFFFF) | ((value & 0xFFFF) << 16);
            scratch[idx] = w;
        }
    }

    /* ---- 8 bit ----------------------------------------------------------- */

    public int read8(int addr) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int w = ram[(a & 0x1FFFFF) >>> 2];
            return (w >>> ((a & 3) * 8)) & 0xFF;
        }
        if (a >= 0x1F801000 && a < 0x1F802000) {
            int w = ioRead32(a & ~3);
            return (w >>> ((a & 3) * 8)) & 0xFF;
        }
        if (a >= 0x1F800000 && a < 0x1F800400) {
            int w = scratch[(a - 0x1F800000) >>> 2];
            return (w >>> ((a & 3) * 8)) & 0xFF;
        }
        if (a >= 0x1FC00000) {
            int w = bios[((a - 0x1FC00000) & 0x7FFFF) >>> 2];
            return (w >>> ((a & 3) * 8)) & 0xFF;
        }
        return 0xFF;
    }

    public void write8(int addr, int value) {
        int a = addr & 0x1FFFFFFF;
        if (a < 0x00800000) {
            int idx = (a & 0x1FFFFF) >>> 2;
            int shift = (a & 3) * 8;
            int w = ram[idx];
            w = (w & ~(0xFF << shift)) | ((value & 0xFF) << shift);
            ram[idx] = w;
            codeCache.invalidate(a);
            if (cpu != null) cpu.noteCodeWrite(a);
            return;
        }
        if (a >= 0x1F801000 && a < 0x1F802000) {
            int w = ioRead32(a & ~3);
            int shift = (a & 3) * 8;
            w = (w & ~(0xFF << shift)) | ((value & 0xFF) << shift);
            ioWrite32(a & ~3, w);
            return;
        }
        if (a >= 0x1F800000 && a < 0x1F800400) {
            int idx = (a - 0x1F800000) >>> 2;
            int shift = (a & 3) * 8;
            int w = scratch[idx];
            w = (w & ~(0xFF << shift)) | ((value & 0xFF) << shift);
            scratch[idx] = w;
        }
    }

    /* ---- DMA block helpers (bypass per-word bus decode) ------------------ */

    /** Copies {@code words} 32 bit words from RAM to the GPU (GP0). */
    public void dmaToGpu(int addr, int words) {
        int a = (addr & 0x1FFFFF) >>> 2;
        for (int i = 0; i < words; i++) {
            gpu.writeGP0(ram[(a + i) & 0x7FFFF]);
        }
    }

    public void dmaFromGpu(int addr, int words) {
        int a = (addr & 0x1FFFFF) >>> 2;
        for (int i = 0; i < words; i++) {
            ram[(a + i) & 0x7FFFF] = gpu.readGPUREAD();
        }
        codeCache.invalidateRange(addr, words * 4);
    }

    public void dmaFromCdrom(int addr, int words) {
        int a = (addr & 0x1FFFFF) >>> 2;
        for (int i = 0; i < words; i++) {
            ram[(a + i) & 0x7FFFF] = cdrom.readWord();
        }
        codeCache.invalidateRange(addr, words * 4);
    }

    public void dmaToSpu(int addr, int words, boolean toSpu) {
        int a = (addr & 0x1FFFFF) >>> 2;
        for (int i = 0; i < words; i++) {
            if (toSpu) spu.writeWord(ram[(a + i) & 0x7FFFF]);
            else ram[(a + i) & 0x7FFFF] = spu.readWord();
        }
        if (!toSpu) codeCache.invalidateRange(addr, words * 4);
    }

    /* ---- I/O ------------------------------------------------------------- */

    public int ioRead32(int a) {
        if (a < 0x1F801040) return memCtrl[(a - 0x1F801000) >>> 2 & 7];
        if (a < 0x1F801060) return pad != null ? pad.read(a) : 0xFF;
        if (a < 0x1F801070) return ramSizeReg;
        if (a < 0x1F801080) return irq != null ? irq.read(a) : 0;
        if (a < 0x1F801100) return dma != null ? dma.read(a) : 0;
        if (a < 0x1F801800) {
            if (a >= 0x1F801100 && a < 0x1F801130) return timers != null ? timers.read(a) : 0;
            return 0xFFFFFFFF;
        }
        if (a < 0x1F801810) return cdrom != null ? cdrom.read(a) : 0;
        if (a < 0x1F801818) return gpu != null ? gpu.read(a) : 0;
        if (a < 0x1F801820) return mdec != null ? mdec.read(a) : 0;
        if (a >= 0x1F801C00 && a < 0x1F801E00) return spu != null ? spu.read(a) : 0;
        return 0xFFFFFFFF;
    }

    public void ioWrite32(int a, int v) {
        if (a < 0x1F801040) { memCtrl[(a - 0x1F801000) >>> 2 & 7] = v; return; }
        if (a < 0x1F801060) { if (pad != null) pad.write(a, v); return; }
        if (a < 0x1F801070) { ramSizeReg = v; return; }
        if (a < 0x1F801080) { if (irq != null) irq.write(a, v); return; }
        if (a < 0x1F801100) { if (dma != null) dma.write(a, v); return; }
        if (a < 0x1F801800) {
            if (a >= 0x1F801100 && a < 0x1F801130 && timers != null) timers.write(a, v);
            return;
        }
        if (a < 0x1F801810) { if (cdrom != null) cdrom.write(a, v); return; }
        if (a < 0x1F801818) {
            if (gpu != null) {
                if ((a & 0xF) == 0) gpu.writeGP0(v);
                else gpu.writeGP1(v);
            }
            return;
        }
        if (a < 0x1F801820) { if (mdec != null) mdec.write(a, v); return; }
        if (a >= 0x1F801C00 && a < 0x1F801E00) { if (spu != null) spu.write(a, v); return; }
    }

    public void setLastLoad(int addr, int value) {
        lastLoadAddr = addr;
        lastLoadValue = value;
    }

    public int getLastLoadAddr() { return lastLoadAddr; }

    public int getLastLoadValue() { return lastLoadValue; }

    /** Copies a byte array into emulated RAM (little endian). */
    public void loadToRam(int dstAddr, byte[] src, int off, int len) {
        int a = (dstAddr & 0x1FFFFF) >>> 2;
        int i = off;
        int end = off + len;
        while (i < end) {
            int w = 0;
            for (int b = 0; b < 4 && i < end; b++, i++) {
                w |= (src[i] & 0xFF) << (b * 8);
            }
            ram[a++] = w;
        }
        codeCache.invalidateRange(dstAddr, len);
    }
}
