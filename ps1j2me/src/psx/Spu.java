package psx;

/**
 * PlayStation SPU: 24 ADPCM voices, ADSR envelopes, noise, main volume and
 * the 512 KiB sample RAM. Reverb is skipped (the mix bus is dry) which keeps
 * the CPU cost acceptable on a J2ME handset.
 *
 * Samples are produced at 44100 Hz into a stereo ring buffer that
 * {@link AudioOutput} drains through a MIDP custom DataSource.
 */
public final class Spu {

    public static final int RAM_SIZE = 512 * 1024;
    public static final int SAMPLE_RATE = 44100;

    public final byte[] ram = new byte[RAM_SIZE];

    private static final int VOICES = 24;
    private final int[] volL = new int[VOICES];
    private final int[] volR = new int[VOICES];
    private final int[] pitch = new int[VOICES];
    private final int[] startAddr = new int[VOICES];
    private final int[] adsrLo = new int[VOICES];
    private final int[] adsrHi = new int[VOICES];
    private final int[] repeatAddr = new int[VOICES];
    private final int[] curAddr = new int[VOICES];
    private final int[] counter = new int[VOICES];
    private final boolean[] active = new boolean[VOICES];
    private final int[] envLevel = new int[VOICES];
    private final int[] envPhase = new int[VOICES];   /* 0 off 1 att 2 dec 3 sus 4 rel */
    private final int[] envCounter = new int[VOICES];
    private final int[] blockBuf = new int[28 * VOICES];
    private final int[] blockPos = new int[VOICES];
    private final int[] prev1 = new int[VOICES];
    private final int[] prev2 = new int[VOICES];
    private final boolean[] loopEnd = new boolean[VOICES];
    private final boolean[] endx = new boolean[VOICES];

    private int mainVolL = 0x3FFF, mainVolR = 0x3FFF;
    private int cdVolL = 0, cdVolR = 0;
    private int spuCnt = 0;
    private int spuStat = 0;
    private int transferAddr, transferCur;
    private int noiseTimer;
    private int noiseLevel = 1;
    private int irqAddr;
    private int cycleAcc;
    private int clockDivider = 768;      /* 33.8688 MHz / 44100 */

    /* audio ring buffer (stereo, 16 bit little endian) */
    public static final int OUT_SIZE = 8192;
    public final byte[] out = new byte[OUT_SIZE];
    private int outWrite, outRead;
    public volatile boolean enabled = true;
    public int lastSampleL, lastSampleR;

    public void reset() {
        for (int i = 0; i < ram.length; i++) ram[i] = 0;
        for (int v = 0; v < VOICES; v++) {
            volL[v] = volR[v] = 0;
            pitch[v] = 0;
            startAddr[v] = 0;
            adsrLo[v] = adsrHi[v] = 0;
            curAddr[v] = 0;
            counter[v] = 0;
            active[v] = false;
            envLevel[v] = 0;
            envPhase[v] = 0;
            blockPos[v] = 0;
            prev1[v] = prev2[v] = 0;
        }
        mainVolL = mainVolR = 0x3FFF;
        spuCnt = 0;
        spuStat = 0;
        transferAddr = transferCur = 0;
        cycleAcc = 0;
        outWrite = outRead = 0;
    }

    /* ==================================================================== */
    /*  register access                                                     */
    /* ==================================================================== */

    public int read(int a) {
        int off = (a - 0x1F801C00) & 0x3FF;
        if (off < 0x180) {
            int v = off / 16;
            switch (off & 0xF) {
                case 0: return volL[v] & 0xFFFF;
                case 2: return volR[v] & 0xFFFF;
                case 4: return pitch[v] & 0xFFFF;
                case 6: return startAddr[v] & 0xFFFF;
                case 8: return adsrLo[v] & 0xFFFF;
                case 0xA: return adsrHi[v] & 0xFFFF;
                case 0xC: return envLevel[v] & 0x7FFF;
                case 0xE: return repeatAddr[v] & 0xFFFF;
                default: return 0;
            }
        }
        switch (off) {
            case 0x180: return mainVolL;
            case 0x182: return mainVolR;
            case 0x184: return 0;
            case 0x188: case 0x18A: return 0;
            case 0x18C: case 0x18E: return 0;
            case 0x190: case 0x192: return 0;
            case 0x194: return 0;
            case 0x196: return 0;
            case 0x198: case 0x19A: return 0;
            case 0x19C: case 0x19E: return endxLow();
            case 0x1A2: return 0;
            case 0x1A4: return irqAddr;
            case 0x1A6: return transferAddr & 0xFFFF;
            case 0x1A8: return readTransferWord();
            case 0x1AA: return spuCnt;
            case 0x1AC: return 0;
            case 0x1AE: return spuStat;
            case 0x1B0: return cdVolL & 0xFFFF;
            case 0x1B2: return cdVolR & 0xFFFF;
            default: return 0;
        }
    }

    private int endxLow() {
        int v = 0;
        for (int i = 0; i < 16; i++) if (endx[i]) v |= 1 << i;
        return v;
    }

    private int endxHigh() {
        int v = 0;
        for (int i = 16; i < 24; i++) if (endx[i]) v |= 1 << (i - 16);
        return v;
    }

    public void write(int a, int v) {
        int off = (a - 0x1F801C00) & 0x3FF;
        if (off < 0x180) {
            int ch = off / 16;
            switch (off & 0xF) {
                case 0: volL[ch] = v & 0xFFFF; break;
                case 2: volR[ch] = v & 0xFFFF; break;
                case 4: pitch[ch] = v & 0x3FFF; break;
                case 6: startAddr[ch] = v & 0xFFFF; curAddr[ch] = startAddr[ch] * 8; break;
                case 8: adsrLo[ch] = v & 0xFFFF; break;
                case 0xA: adsrHi[ch] = v & 0xFFFF; break;
                case 0xE: repeatAddr[ch] = v & 0xFFFF; break;
                default: break;
            }
            return;
        }
        switch (off) {
            case 0x180: mainVolL = v & 0xFFFF; break;
            case 0x182: mainVolR = v & 0xFFFF; break;
            case 0x188: /* key on low */
                for (int i = 0; i < 16; i++) if ((v & (1 << i)) != 0) keyOn(i);
                break;
            case 0x18A:
                for (int i = 16; i < 24; i++) if ((v & (1 << (i - 16))) != 0) keyOn(i);
                break;
            case 0x18C:
                for (int i = 0; i < 16; i++) if ((v & (1 << i)) != 0) keyOff(i);
                break;
            case 0x18E:
                for (int i = 16; i < 24; i++) if ((v & (1 << (i - 16))) != 0) keyOff(i);
                break;
            case 0x19C: case 0x19E:
                for (int i = 0; i < 24; i++) endx[i] = false;
                break;
            case 0x1A4: irqAddr = v & 0xFFFF; break;
            case 0x1A6: transferAddr = v & 0xFFFF; transferCur = transferAddr * 8; break;
            case 0x1A8: writeTransferWord(v); break;
            case 0x1AA:
                spuCnt = v & 0xFFFF;
                enabled = (v & 0x8000) != 0;
                if ((v & 0x40) != 0) {
                    irqAddr = 0;
                    spuStat = 0;
                }
                break;
            case 0x1AC: break;
            case 0x1B0: cdVolL = v & 0xFFFF; break;
            case 0x1B2: cdVolR = v & 0xFFFF; break;
            default: break;
        }
    }

    private void writeTransferWord(int w) {
        int addr = transferCur & (RAM_SIZE - 1);
        ram[addr] = (byte) (w & 0xFF);
        ram[(addr + 1) & (RAM_SIZE - 1)] = (byte) ((w >>> 8) & 0xFF);
        ram[(addr + 2) & (RAM_SIZE - 1)] = (byte) ((w >>> 16) & 0xFF);
        ram[(addr + 3) & (RAM_SIZE - 1)] = (byte) ((w >>> 24) & 0xFF);
        transferCur = (transferCur + 4) & (RAM_SIZE - 1);
    }

    private int readTransferWord() {
        int addr = transferCur & (RAM_SIZE - 1);
        int v = (ram[addr] & 0xFF) | ((ram[(addr + 1) & (RAM_SIZE - 1)] & 0xFF) << 8)
                | ((ram[(addr + 2) & (RAM_SIZE - 1)] & 0xFF) << 16)
                | ((ram[(addr + 3) & (RAM_SIZE - 1)] & 0xFF) << 24);
        transferCur = (transferCur + 4) & (RAM_SIZE - 1);
        return v;
    }

    /** DMA channel 4 writes 32 bit words into sample RAM. */
    public void writeWord(int w) {
        writeTransferWord(w);
    }

    public int readWord() {
        return readTransferWord();
    }

    private void keyOn(int v) {
        active[v] = true;
        envPhase[v] = 1;
        envLevel[v] = 0;
        envCounter[v] = 0;
        curAddr[v] = startAddr[v] * 8;
        blockPos[v] = 28;
        prev1[v] = prev2[v] = 0;
        endx[v] = false;
    }

    private void keyOff(int v) {
        if (envPhase[v] != 0) envPhase[v] = 4;
    }

    /* ==================================================================== */
    /*  mixing                                                              */
    /* ==================================================================== */

    public void step(int cpuCycles) {
        cycleAcc += cpuCycles;
        while (cycleAcc >= clockDivider) {
            cycleAcc -= clockDivider;
            mixSample();
        }
    }

    private void mixSample() {
        int mixL = 0, mixR = 0;
        if (enabled) {
            for (int v = 0; v < VOICES; v++) {
                if (!active[v]) continue;
                int adv = pitch[v];
                counter[v] += adv;
                while (counter[v] >= 0x1000) {
                    counter[v] -= 0x1000;
                    blockPos[v]++;
                    if (blockPos[v] >= 28) {
                        blockPos[v] = 0;
                        fetchBlock(v);
                    }
                }
                int sample = blockBuf[v * 28 + blockPos[v]];
                int env = updateEnv(v);
                int l = (int) (((long) sample * env >> 15) * (short) volL[v] >> 15);
                int r = (int) (((long) sample * env >> 15) * (short) volR[v] >> 15);
                mixL += l;
                mixR += r;
            }
            /* noise channel */
            noiseTimer += 1;
            if (noiseTimer >= 0x40) {
                noiseTimer = 0;
                noiseLevel = (noiseLevel * 1103515245 + 12345) >>> 16;
            }
        }
        int l = (mixL * (short) mainVolL) >> 15;
        int r = (mixR * (short) mainVolR) >> 15;
        if (l > 32767) l = 32767; else if (l < -32768) l = -32768;
        if (r > 32767) r = 32767; else if (r < -32768) r = -32768;
        lastSampleL = l;
        lastSampleR = r;
        push(l, r);
    }

    private int updateEnv(int v) {
        int level = envLevel[v];
        switch (envPhase[v]) {
            case 1: /* attack */
                level += attackStep(v);
                if (level >= 0x7FFF) {
                    level = 0x7FFF;
                    envPhase[v] = 2;
                }
                break;
            case 2: /* decay */
                level -= decayStep(v);
                if (level <= sustainLevel(v)) {
                    level = sustainLevel(v);
                    envPhase[v] = 3;
                }
                break;
            case 3: /* sustain */
                break;
            case 4: /* release */
                level -= releaseStep(v);
                if (level <= 0) {
                    level = 0;
                    envPhase[v] = 0;
                    active[v] = false;
                    endx[v] = true;
                }
                break;
            default:
                level = 0;
                break;
        }
        if (level < 0) level = 0;
        if (level > 0x7FFF) level = 0x7FFF;
        envLevel[v] = level;
        return level;
    }

    private int attackStep(int v) {
        int rate = adsrHi[v] & 0x7F;
        int exp = (adsrHi[v] >>> 8) & 0x7F;
        int step = (rate >> 2) + 1;
        if (exp != 0) step += step >> 1;
        return step;
    }

    private int decayStep(int v) {
        int rate = (adsrLo[v] >>> 4) & 0xF;
        return (rate << 2) + 1;
    }

    private int sustainLevel(int v) {
        int sl = (adsrLo[v] >>> 8) & 0xF;
        return (0xF - sl) << 11;
    }

    private int releaseStep(int v) {
        int rate = adsrLo[v] & 0x1F;
        return (rate << 2) + 1;
    }

    /** Decodes one 16 byte / 28 sample ADPCM block into blockBuf[v]. */
    private void fetchBlock(int v) {
        int addr = curAddr[v] & (RAM_SIZE - 1);
        int header = ram[addr] & 0xFF;
        int shift = header & 0x0F;
        int filter = (header >>> 4) & 0x07;
        boolean loop = (header & 0x04) != 0;      /* flags in the high nibble */
        boolean end = (header & 0x01) != 0;

        int f0 = 0, f1 = 0;
        switch (filter) {
            case 1: f0 = 60; f1 = 0; break;
            case 2: f0 = 115; f1 = -52; break;
            case 3: f0 = 98; f1 = -55; break;
            case 4: f0 = 122; f1 = -60; break;
            default: break;
        }
        int base = v * 28;
        int s1 = prev1[v], s2 = prev2[v];
        for (int i = 0; i < 28; i++) {
            int b = ram[(addr + 1 + (i >> 1)) & (RAM_SIZE - 1)] & 0xFF;
            int nib = ((i & 1) == 0) ? (b & 0xF) : ((b >>> 4) & 0xF);
            if (nib > 7) nib -= 16;
            int s = (nib << 12);
            s >>= shift;
            s += (s1 * f0 + s2 * f1 + 32) >> 6;
            if (s > 32767) s = 32767;
            if (s < -32768) s = -32768;
            s2 = s1;
            s1 = s;
            blockBuf[base + i] = s;
        }
        prev1[v] = s1;
        prev2[v] = s2;
        curAddr[v] = (addr + 16) & (RAM_SIZE - 1);
        if (end || loop) {
            if (end) endx[v] = true;
            curAddr[v] = repeatAddr[v] * 8;
            prev1[v] = prev2[v] = 0;
            if (!loop) {
                active[v] = false;
                envPhase[v] = 0;
            }
        }
    }

    /* ==================================================================== */
    /*  output ring buffer                                                  */
    /* ==================================================================== */

    private void push(int l, int r) {
        int next = (outWrite + 4) % OUT_SIZE;
        if (next == outRead) {
            /* overflow: drop the oldest frame */
            outRead = (outRead + 4) % OUT_SIZE;
        }
        out[outWrite] = (byte) (l & 0xFF);
        out[(outWrite + 1) % OUT_SIZE] = (byte) ((l >> 8) & 0xFF);
        out[(outWrite + 2) % OUT_SIZE] = (byte) (r & 0xFF);
        out[(outWrite + 3) % OUT_SIZE] = (byte) ((r >> 8) & 0xFF);
        outWrite = next;
    }

    /** Copies up to {@code len} bytes of PCM into {@code dst}; returns count. */
    public int drain(byte[] dst, int off, int len) {
        int n = 0;
        while (n < len && outRead != outWrite) {
            dst[off + n] = out[outRead];
            outRead = (outRead + 1) % OUT_SIZE;
            n++;
        }
        return n;
    }

    public int available() {
        return (outWrite - outRead + OUT_SIZE) % OUT_SIZE;
    }

    public void flush() {
        outRead = outWrite;
    }

    public int getVoiceEnvelope(int v) { return envLevel[v]; }
}
