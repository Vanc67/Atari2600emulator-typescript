package psx;

import java.io.IOException;
import java.io.InputStream;

/**
 * The PlayStation machine: bus + CPU + GTE + GPU + SPU + CD-ROM + DMA +
 * timers + pads, wired together and stepped one video frame at a time.
 *
 * The BIOS image is loaded from the JAR resource /res/roms/scph1001.bin via
 * getResourceAsStream(). If that image is the bundled placeholder (empty code
 * area) the machine falls back to an HLE boot that locates SYSTEM.CNF on the
 * disc and starts the PS-EXE directly - no copyrighted BIOS required.
 */
public final class PS1Machine {

    public static final int CPU_CLOCK = 33868800;
    public static final int FRAME_CYCLES = 33868800 / 60;

    public final Bus bus = new Bus();
    public final Gte gte = new Gte();
    public final Cpu cpu;
    public final Gpu gpu = new Gpu();
    public final Spu spu = new Spu();
    public final Cdrom cdrom = new Cdrom();
    public final Dma dma = new Dma();
    public final Timers timers = new Timers();
    public final Pad pad = new Pad();
    public final Mdec mdec = new Mdec();
    public final Irq irq = new Irq();

    private boolean biosLoaded;
    private boolean hleMode;
    /** BIOS kernel replacement, active in HLE mode. */
    public BiosHle biosHle;
    /** Set by exit()/halt() - the running program finished. */
    public boolean halted;
    private int frameCounter;
    public int framesRun;
    public long totalCycles;
    public String bootInfo = "";

    public PS1Machine(CdImage cd) {
        cpu = new Cpu(bus, gte);

        bus.cpu = cpu;
        bus.gpu = gpu;
        bus.spu = spu;
        bus.cdrom = cdrom;
        bus.dma = dma;
        bus.timers = timers;
        bus.pad = pad;
        bus.mdec = mdec;
        bus.irq = irq;

        dma.bus = bus;
        dma.irq = irq;
        timers.irq = irq;
        mdec.irq = irq;
        irq.cpu = cpu;
        cdrom.irq = irq;
        cdrom.cd = cd;
        gte.reset();
        reset();
    }

    public void reset() {
        bus.reset();
        gte.reset();
        cpu.reset();
        cdrom.reset();
        dma.reset();
        timers.reset();
        pad.reset();
        irq.reset();
        mdec.reset();
        framesRun = 0;
        totalCycles = 0;
        frameCounter = 0;
    }

    /* ==================================================================== */
    /*  BIOS                                                                */
    /* ==================================================================== */

    /**
     * Loads the BIOS image (512 KiB) into emulated memory.
     *
     * @return true when a real, executable BIOS was loaded
     */
    public boolean loadBios(InputStream in) throws IOException {
        if (in == null) return false;
        byte[] header = new byte[16];
        int total = 0;
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk, 0, chunk.length)) > 0) {
            int words = (n + 3) / 4;
            for (int i = 0; i < words; i++) {
                int off = i * 4;
                int v = 0;
                for (int b = 0; b < 4; b++) {
                    if (off + b < n) v |= (chunk[off + b] & 0xFF) << (b * 8);
                }
                if (total + off < 0x80000) {
                    bus.bios[(total + off) >>> 2] = v;
                }
            }
            if (total == 0) System.arraycopy(chunk, 0, header, 0, 16);
            total += n;
            if (total >= 0x80000) break;
        }
        in.close();
        biosLoaded = total >= 0x80000;
        if (biosLoaded) {
            /* The placeholder image produced by tools/mkbios_stub.py has the
               Sony header but no code. Detect it through its marker or by the
               absence of real code in the first 4 KiB. */
            boolean marker = false;
            for (int i = 0; i < 0x1000 - 8; i++) {
                if (byteAt(i) != 'H') continue;
                if (byteAt(i + 1) == 'L' && byteAt(i + 2) == 'E' && byteAt(i + 3) == '-'
                        && byteAt(i + 4) == 'S' && byteAt(i + 5) == 'T'
                        && byteAt(i + 6) == 'U' && byteAt(i + 7) == 'B') {
                    marker = true;
                    break;
                }
            }
            hleMode = marker || !hasCode();
            bootInfo = hleMode ? "HLE boot" : "BIOS SCPH1001";
        }
        return biosLoaded;
    }

    private char byteAt(int biosOffset) {
        int w = bus.bios[(biosOffset & 0x7FFFF) >>> 2];
        return (char) ((w >>> ((biosOffset & 3) * 8)) & 0xFF);
    }

    private boolean hasCode() {
        /* a real BIOS has plenty of non-zero words in the first 4 KiB */
        int nz = 0;
        for (int i = 0; i < 1024; i++) {
            if (bus.bios[i] != 0) nz++;
        }
        return nz > 64;
    }

    public boolean isHleMode() { return hleMode; }

    /** Installs the BIOS kernel replacement so A0/B0/C0 calls work. */
    public void installHle() {
        if (biosHle == null) biosHle = new BiosHle(this);
        else biosHle.reset();
        cpu.hle = biosHle;
    }

    /** Stops the emulated program (BIOS exit()/halt()). */
    public void halt() { halted = true; }

    public boolean isBiosLoaded() { return biosLoaded; }

    /** HLE boot: start the executable indicated by SYSTEM.CNF on the CD. */
    public boolean hleBoot(CdImage cd) {
        if (cd == null || !cd.isOpen()) return false;
        try {
            byte[] cnf = Iso.readFile(cd, "SYSTEM.CNF;1");
            if (cnf == null) cnf = Iso.readFile(cd, "SYSTEM.CNF");
            String boot = "PSX.EXE";
            int stack = 0x801FFFF0;
            if (cnf != null) {
                String text = new String(cnf);
                boot = parseBoot(text);
                stack = parseStack(text, stack);
            }
            byte[] exe = Iso.readFile(cd, boot);
            if (exe == null) {
                String alt = boot.toUpperCase();
                exe = Iso.readFile(cd, alt);
            }
            if (exe == null) return false;
            return loadExe(exe, stack);
        } catch (IOException e) {
            return false;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static String parseBoot(String text) {
        int i = text.indexOf("BOOT");
        if (i < 0) return "PSX.EXE";
        int eq = text.indexOf('=', i);
        if (eq < 0) return "PSX.EXE";
        int end = eq;
        while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') end++;
        String v = text.substring(eq + 1, end).trim();
        int semi = v.indexOf(';');
        if (semi >= 0) v = v.substring(0, semi);
        int slash = v.lastIndexOf("\\");
        if (slash < 0) slash = v.lastIndexOf("/");
        if (slash >= 0) v = v.substring(slash + 1);
        int colon = v.indexOf(':');
        if (colon >= 0) v = v.substring(colon + 1);
        v = v.trim();
        if (v.length() == 0) return "PSX.EXE";
        return v;
    }

    private static int parseStack(String text, int def) {
        int i = text.indexOf("STACK");
        if (i < 0) return def;
        int eq = text.indexOf('=', i);
        if (eq < 0) return def;
        int end = eq;
        while (end < text.length() && text.charAt(end) != '\n' && text.charAt(end) != '\r') end++;
        String v = text.substring(eq + 1, end).trim();
        try {
            if (v.startsWith("0x") || v.startsWith("0X")) v = v.substring(2);
            return (int) Long.parseLong(v, 16);
        } catch (Exception e) {
            return def;
        }
    }

    /** Parses a PS-X EXE header and jumps into the entry point. */
    private boolean loadExe(byte[] exe, int stack) {
        if (exe.length < 0x800) return false;
        if (exe[0] != 'P' || exe[1] != 'S' || exe[2] != '-' || exe[3] != 'X') return false;
        int pc = le32(exe, 0x10);
        int gp = le32(exe, 0x14);
        int tAddr = le32(exe, 0x18);
        int tSize = le32(exe, 0x1C);
        if (tSize <= 0 || tSize > 0x180000) return false;
        if (tAddr < 0x80010000 || tAddr + tSize > 0x80200000) return false;
        int dst = tAddr & 0x1FFFFF;
        for (int i = 0; i < tSize; i++) {
            int b = (0x800 + i < exe.length) ? (exe[0x800 + i] & 0xFF) : 0;
            int a = (dst + i) & 0x1FFFFF;
            int shift = (a & 3) * 8;
            bus.ram[a >> 2] = (bus.ram[a >> 2] & ~(0xFF << shift)) | (b << shift);
        }
        bus.codeCache.invalidateRange(tAddr, tSize);
        cpu.reset();
        cpu.pc = pc;
        cpu.gpr[28] = gp;
        cpu.gpr[29] = stack;
        cpu.gpr[30] = stack;
        hleMode = true;
        installHle();
        bootInfo = "HLE " + Integer.toHexString(pc);
        return true;
    }

    private static int le32(byte[] b, int o) {
        return (b[o] & 0xFF) | ((b[o + 1] & 0xFF) << 8) | ((b[o + 2] & 0xFF) << 16) | ((b[o + 3] & 0xFF) << 24);
    }

    /* ==================================================================== */
    /*  execution                                                           */
    /* ==================================================================== */

    /** Runs one PAL/NTSC video frame worth of cycles. */
    public void runFrame() {
        if (halted) return;
        int budget = FRAME_CYCLES;
        while (budget > 0) {
            int slice = budget > 2000 ? 2000 : budget;
            int used = cpu.run(slice);
            if (used <= 0) used = 2;
            budget -= used;
            totalCycles += used;
            timers.step(used);
            cdrom.step(used);
            spu.step(used);
            pad.step(used);
            gpu.step(used);
            if (gpu.irqRequested) irq.raise(Irq.GPU);
        }
        irq.raise(Irq.VBLANK);
        gpu.scanout();
        framesRun++;
        frameCounter++;
    }

    /** Steps a single instruction (debugging / single step mode). */
    public void stepInstruction() {
        int used = cpu.run(2);
        totalCycles += used;
        timers.step(used);
        spu.step(used);
        cdrom.step(used);
        gpu.step(used);
        if (gpu.irqRequested) irq.raise(Irq.GPU);
    }

    public boolean isRunning() {
        return biosLoaded || hleMode;
    }

    public int[] getFrame() {
        return gpu.getFrame();
    }

    public int getFrameWidth() { return gpu.getViewportWidth(); }

    /** Emergency mode: show VRAM from its origin instead of the display area. */
    public void setRawScanout(boolean raw) { gpu.setRawScanout(raw); }

    /** Pixels written by the GPU so far - proves whether it is drawing at all. */
    public int getPixelsWritten() { return gpu.pixelsWritten; }

    public int getFrameHeight() { return gpu.getViewportHeight(); }

    /** Exceptions the CPU took; used by tests to catch bogus memory access. */
    public int getExceptionCount() { return cpu.exceptions; }

    /** ExcCode of the last exception (4 = ADEL, 5 = ADES, 8 = Syscall, ...). */
    public int getLastExceptionCode() { return cpu.lastExcCode; }

    public void setViewport(int w, int h) {
        gpu.setViewport(w, h);
    }

    public void setButtons(int b) {
        pad.setButtons(b);
    }

    public String statusLine() {
        return bootInfo + " " + framesRun + "f " + (cpu.lastBlockCount & 0xFFFF);
    }
}
