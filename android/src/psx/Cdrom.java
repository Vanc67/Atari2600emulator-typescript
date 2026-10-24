package psx;

import java.io.IOException;

/**
 * CD-ROM controller (0x1F801800..0x1F801803).
 *
 * Implements the command/response protocol, the interrupt queue (INT1/INT2/
 * INT3/INT5), Setloc/Seek/ReadN with the data FIFO feeding DMA channel 3,
 * and returns a plausible TOC/GetID so the BIOS accepts the disc.
 */
public final class Cdrom {

    private int index;
    private int stat = 0x02;
    private int irqEnable;
    private int irqFlags;

    private final byte[] param = new byte[16];
    private int paramCount;
    private final byte[] response = new byte[16];
    private int responseLen, responsePos;
    private final byte[] data = new byte[2352];
    private int dataLen, dataPos;

    private int currentLba = 150;     /* sector 0 in absolute MSF */
    private int seekLba;
    private int setlocPending;
    private int setlocMm, setlocSs, setlocFf;
    private boolean reading;
    private boolean bufferRequested;
    private boolean motorOn = true;
    private int mode;

    private int pendingInt = -1;
    private int pendingDelay;
    private int sectorCycles;
    public Irq irq;
    public CdImage cd;
    private final byte[] sectorBuf = new byte[2048];

    public void reset() {
        index = 0;
        stat = 0x02;
        irqEnable = 0;
        irqFlags = 0;
        paramCount = 0;
        responseLen = responsePos = 0;
        dataLen = dataPos = 0;
        currentLba = 150;
        seekLba = 150;
        reading = false;
        bufferRequested = false;
        motorOn = true;
        mode = 0;
        pendingInt = -1;
    }

    /* ==================================================================== */
    /*  register interface                                                  */
    /* ==================================================================== */

    /**
     * Register reads, bank by bank (psx-spx: cdromdrive.md).
     *
     * <pre>
     *   bank 0: 0 status | 1 RESULT  | 2 RDDATA | 3 HINTMSK
     *   bank 1: 0 status | 1 RESULT  | 2 RDDATA | 3 HINTSTS
     *   bank 2: 0 status | 1 RESULT  | 2 RDDATA | 3 HINTMSK
     *   bank 3: 0 status | 1 RESULT  | 2 RDDATA | 3 HINTSTS
     * </pre>
     *
     * RESULT and RDDATA are readable from every bank; the masks live in banks
     * 0/2 and the flags in banks 1/3. The previous code returned the interrupt
     * enable for banks 1 and 3 of register 1 and 2, so a game that polled its
     * HINTSTS there saw nonsense.
     */
    public int read(int a) {
        int reg = a & 3;
        if (reg == 0) {
            int st = index & 3;
            if (responseLen > 0) st |= 0x20;    /* RSLRRDY: result FIFO not empty */
            if (dataPos < dataLen) st |= 0x40;   /* DRQSTS:  data request          */
            if (paramCount == 0) st |= 0x08;     /* PRMEMPT: parameter FIFO empty   */
            st |= 0x10;                          /* PRMWRDY: parameter write ready  */
            if (pendingInt >= 0) st |= 0x80;     /* BUSYSTS: command in progress    */
            if ((irqFlags & 0x07) != 0) st |= 0x01; /* INTSTS mirror                 */
            return st;
        }
        if (reg == 1) {
            if (responsePos < responseLen) return response[responsePos++] & 0xFF;
            return 0x00;
        }
        if (reg == 2) {
            return dataPos < dataLen ? (data[dataPos++] & 0xFF) : 0;
        }
        /* reg 3: banks 0/2 hide the enable mask, banks 1/3 show the flags */
        if (index == 0 || index == 2) return irqEnable;
        return 0xE0 | (irqFlags & 0x1F);
    }

    public void write(int a, int v) {
        int reg = a & 3;
        if (reg == 0) {
            index = v & 3;
            return;
        }
        if (reg == 1) {
            if (index == 0) command(v & 0xFF);
            return;
        }
        if (reg == 2) {
            if (index == 0) {
                /* PARAMETER: the FIFO is written before the command byte. */
                if (paramCount < param.length) param[paramCount++] = (byte) v;
            } else if (index == 1) {
                /* HINTMSK (bank 1): which of the flags may raise IRQ0. */
                irqEnable = v & 0x1F;
            }
            return;
        }
        /* reg 3 */
        if (index == 0) {
            /* HCHPCTL: bit7 = BFRD (sector buffer read), bit6 = BFWR. */
            bufferRequested = (v & 0x80) != 0;
        } else if (index == 1) {
            /* HCLRCTL: acknowledge the flags and optionally clear FIFOs. */
            irqFlags &= ~(v & 0x07);
            if ((v & 0x40) != 0) paramCount = 0;
            if ((v & 0x80) != 0) {                       /* CHPRST */
                reading = false;
                dataLen = dataPos = 0;
                responseLen = responsePos = 0;
            }
        }
        /* banks 2/3: ATV1/ATV3 CD audio volumes - not mixed here */
    }

    /**
     * Word read used by DMA channel 3 (little endian).
     *
     * A partly filled word keeps the bytes it has instead of collapsing to
     * zero, which is what the hardware does for the tail of a 800h-byte
     * transfer requested as whole words.
     */
    public int readWord() {
        int v = 0;
        for (int i = 0; i < 4; i++) {
            if (dataPos < dataLen) v |= (data[dataPos++] & 0xFF) << (i * 8);
        }
        return v;
    }

    /** True after HCHPCTL asked for a sector buffer read (bit 7). */
    public boolean isBufferRequested() { return bufferRequested; }

    public boolean hasData() {
        return dataPos < dataLen;
    }

    /* ==================================================================== */
    /*  command engine                                                      */
    /* ==================================================================== */

    private void command(int cmd) {
        /* A new command drops whatever the previous one left in the result
           FIFO. Without this the answers piled up and a game reading GetID
           after GetStat saw the old status byte first. */
        responsePos = 0;
        responseLen = 0;
        int need = paramWords(cmd);
        /* The BIOS sends parameters before the command byte in index 0 mode;
           we accumulate them here and execute on the command write. */
        switch (cmd) {
            case 0x00:
                response(0x01);
                interrupt(3, 2000);
                break;
            case 0x01: /* GetStat */
                response(buildStat());
                interrupt(3, 2000);
                break;
            case 0x02: /* Setloc mm,ss,ff (BCD) */
                setlocMm = paramCount > 0 ? hex(param[0]) : 0;
                setlocSs = paramCount > 1 ? hex(param[1]) : 0;
                setlocFf = paramCount > 2 ? hex(param[2]) : 0;
                {
                    int lba = (setlocMm * 60 + setlocSs) * 75 + setlocFf - 150;
                    if (lba < 0) lba = 0;
                    seekLba = lba;
                }
                stat = buildStat();
                interrupt(3, 2000);
                break;
            case 0x03: /* Play */
                motorOn = true;
                stat |= 0x80;
                interrupt(3, 2000);
                break;
            case 0x04: case 0x05:
                interrupt(3, 2000);
                break;
            case 0x06: /* ReadN */
                reading = true;
                motorOn = true;
                stat |= 0x20;
                currentLba = seekLba;
                sectorCycles = 0;
                interrupt(3, 2000);
                break;
            case 0x07: /* MotorOn */
                motorOn = true;
                stat |= 0x02;
                interrupt(3, 2000);
                break;
            case 0x08: /* Stop */
                reading = false;
                motorOn = false;
                stat &= ~0x20;
                interrupt(3, 2000);
                break;
            case 0x09: /* Pause */
                reading = false;
                stat &= ~0x20;
                interrupt(3, 2000);
                break;
            case 0x0A: /* Init */
                mode = 0x20;
                stat = 0x02;
                reading = false;
                paramCount = 0;
                interrupt(3, 4000);
                break;
            case 0x0B: case 0x0C: /* Mute / Demute */
                interrupt(3, 2000);
                break;
            case 0x0D: /* SetFilter */
                interrupt(3, 2000);
                break;
            case 0x0E: /* Setmode */
                if (paramCount > 0) mode = param[0] & 0xFF;
                interrupt(3, 2000);
                break;
            case 0x0F: /* Getparam */
                response(stat);
                response(mode);
                response(0);
                response(0);
                response(0);
                interrupt(3, 2000);
                break;
            case 0x10: /* GetlocL */
                response(0x41);
                response(0x01);
                response(0x01);
                response(0x01);
                response(0);
                response(0);
                response(0);
                response(0);
                interrupt(3, 2000);
                break;
            case 0x11: /* GetlocP */
                response(0x01);
                response(0x01);
                response(0x01);
                response(0x01);
                response(0x01);
                response(0x01);
                response(0x01);
                response(0x01);
                interrupt(3, 2000);
                break;
            case 0x13: /* GetTN */
                response(0x01);
                response(0x01);
                response(0x01);
                interrupt(3, 2000);
                break;
            case 0x14: /* GetTD */
                response(0x01);
                response(0x00);
                response(0x00);
                response(0x02);
                interrupt(3, 2000);
                break;
            case 0x15: /* SeekL */
                currentLba = seekLba;
                stat |= 0x40;
                interrupt(3, 2000);
                break;
            case 0x16: /* SeekP */
                currentLba = seekLba;
                interrupt(3, 2000);
                break;
            case 0x19: /* Test */
                testCommand();
                break;
            case 0x1A: /* GetID */
                response(0x02);
                response(0x00);
                response(0x20);
                response(0x00);
                response('S');
                response('C');
                response('E');
                response('A');
                interrupt(3, 3000);
                break;
            case 0x1E: /* ReadTOC */
                response(0x01);
                response(0x00);
                response(0xA0);
                response(0x00);
                interrupt(3, 3000);
                break;
            default:
                response(0x40);
                interrupt(5, 2000);
                break;
        }
        paramCount = 0;
    }

    private void testCommand() {
        int sub = paramCount > 0 ? param[0] & 0xFF : 0;
        if (sub == 0x20) {
            response(0x97);
            response(0x06);
            response(0x10);
            response(0x00);
        } else if (sub == 0x04) {
            response(0x01);
            response(0x00);
        } else {
            response(0x00);
        }
        interrupt(3, 2000);
    }

    private static int hex(byte b) {
        int hi = (b >> 4) & 0xF;
        int lo = b & 0xF;
        return hi * 10 + lo;
    }

    private int paramWords(int cmd) {
        switch (cmd) {
            case 0x02: return 3;
            case 0x0D: return 2;
            case 0x0E: return 1;
            case 0x14: return 1;
            case 0x19: return 1;
            default: return 0;
        }
    }

    private int buildStat() {
        int s = 0x02;
        if (motorOn) s |= 0x02;
        if (reading) s |= 0x20;
        return s;
    }

    private void response(int v) {
        if (responseLen < response.length) response[responseLen++] = (byte) v;
    }

    private void interrupt(int type, int delay) {
        pendingInt = type;
        pendingDelay = delay;
    }

    private void fireIrq(int type) {
        irqFlags = (irqFlags & 0xE0) | type;
        if ((irqEnable & type) != 0 && irq != null) irq.raise(Irq.CDROM);
    }

    /* ==================================================================== */
    /*  timing                                                              */
    /* ==================================================================== */

    public void step(int cycles) {
        if (pendingInt >= 0) {
            pendingDelay -= cycles;
            if (pendingDelay <= 0) {
                fireIrq(pendingInt);
                pendingInt = -1;
            }
        }
        if (!reading) return;
        sectorCycles += cycles;
        if (sectorCycles < 33868800 / 75) return;   /* one sector per 1/75 s */
        sectorCycles -= 33868800 / 75;
        loadSector();
    }

    private void loadSector() {
        if (cd == null || !cd.isOpen()) return;
        int need = (mode & 0x20) != 0 ? 2340 : 2048;
        try {
            if (need > 2048) {
                byte[] raw = new byte[2352];
                cd.readRawSector(currentLba, raw);
                System.arraycopy(raw, 12, data, 0, 2340);
            } else {
                cd.readSector(currentLba, sectorBuf, 0);
                System.arraycopy(sectorBuf, 0, data, 0, 2048);
            }
        } catch (IOException e) {
            for (int i = 0; i < need; i++) data[i] = 0;
        }
        dataLen = need;
        dataPos = 0;
        currentLba++;
        fireIrq(1);
    }

    public int getCurrentLba() { return currentLba; }
}
