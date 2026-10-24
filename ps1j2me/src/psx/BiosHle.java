package psx;

/**
 * High level emulation of the PlayStation BIOS kernel (the A0/B0/C0 tables).
 *
 * A retail game starts with the BIOS kernel already resident and calls it all
 * the time: {@code printf} for debug output, {@code OpenEvent}/{@code WaitEvent}
 * for synchronisation, {@code malloc} for memory, {@code open}/{@code read} for
 * streaming files off the CD, and finally {@code exit}. Without these calls a
 * game either crashes into the exception vector or spins forever, which is why
 * a pure HLE boot used to end on a black screen even though the disc code had
 * been loaded correctly.
 *
 * Instead of executing the real (copyrighted) BIOS we intercept the three
 * call vectors in {@link Cpu}: the game performs {@code jal 0xA0} with the
 * function number in {@code r9}, we answer in {@code v0} and return to
 * {@code ra}. Everything is plain integer code - CLDC 1.1 safe.
 */
public final class BiosHle {

    /* ---- console ------------------------------------------------------- */
    private static final int OUTPUT_MAX = 6144;
    private final StringBuffer output = new StringBuffer();

    /* ---- heap ---------------------------------------------------------- */
    private static final int DEFAULT_HEAP = 0x80100000;
    private static final int HEAP_LIMIT = 0x801F0000;
    private int heapBase, heapTop, heapEnd;
    private int kHeapTop = 0x801E0000;

    /* ---- events -------------------------------------------------------- */
    private static final int EV_MAX = 32;
    private final int[] evClass = new int[EV_MAX];
    private final int[] evSpec = new int[EV_MAX];
    private final int[] evMode = new int[EV_MAX];
    private final int[] evStatus = new int[EV_MAX];
    private final boolean[] evUsed = new boolean[EV_MAX];
    private int nextEvent = 0xF1000001;

    /* ---- files --------------------------------------------------------- */
    private static final int FD_MAX = 8;
    private final int[] fdLba = new int[FD_MAX];
    private final int[] fdSize = new int[FD_MAX];
    private final int[] fdPos = new int[FD_MAX];
    private final boolean[] fdUsed = new boolean[FD_MAX];
    private final byte[] sector = new byte[2048];

    /* ---- pads ---------------------------------------------------------- */
    private int padBuf1, padSize1, padBuf2, padSize2;

    /* ---- misc ---------------------------------------------------------- */
    private int errno;
    private int randState = 1;
    private boolean exited;
    public int calls, unknownCalls;
    private final StringBuffer lastUnknown = new StringBuffer();

    private final Cpu cpu;
    private final Bus bus;
    private final PS1Machine machine;

    public BiosHle(PS1Machine machine) {
        this.machine = machine;
        this.bus = machine.bus;
        this.cpu = machine.cpu;
        this.heapBase = DEFAULT_HEAP;
        this.heapTop = DEFAULT_HEAP;
        this.heapEnd = HEAP_LIMIT;
    }

    public void reset() {
        output.setLength(0);
        for (int i = 0; i < EV_MAX; i++) {
            evUsed[i] = false;
            evStatus[i] = 0;
        }
        for (int i = 0; i < FD_MAX; i++) fdUsed[i] = false;
        heapBase = DEFAULT_HEAP;
        heapTop = DEFAULT_HEAP;
        heapEnd = HEAP_LIMIT;
        kHeapTop = 0x801E0000;
        nextEvent = 0xF1000001;
        errno = 0;
        exited = false;
        calls = 0;
        unknownCalls = 0;
    }

    public boolean hasExited() { return exited; }

    /** Console text produced by printf()/puts()/putchar(). */
    public String getOutput() { return output.toString(); }

    public void clearOutput() { output.setLength(0); }

    /** Last console line, handy for an on screen debug line. */
    public String getLastLine() {
        int end = output.length();
        if (end == 0) return "";
        while (end > 0 && (output.charAt(end - 1) == '\n' || output.charAt(end - 1) == '\r')) end--;
        int start = end;
        while (start > 0 && output.charAt(start - 1) != '\n') start--;
        if (end - start > 32) start = end - 32;
        StringBuffer sb = new StringBuffer();
        for (int i = start; i < end; i++) sb.append(output.charAt(i));
        return sb.toString();
    }

    /* ==================================================================== */
    /*  entry points                                                        */
    /* ==================================================================== */

    /**
     * Handles a call into one of the BIOS vector tables.
     *
     * @param table 0xA0, 0xB0 or 0xC0 (already masked to 28 bits)
     * @param func  the function number the caller put in r9
     */
    public void call(int table, int func) {
        calls++;
        int result;
        try {
            if (table == 0xA0) result = callA(func);
            else if (table == 0xB0) result = callB(func);
            else result = callC(func);
        } catch (Throwable t) {
            /* never let a broken HLE call kill the emulated machine */
            result = 0;
            errno = 5;
        }
        cpu.gpr[2] = result;
        cpu.gpr[0] = 0;
    }

    /** Syscall instruction (EnterCriticalSection and friends). */
    public void syscall(int func) {
        calls++;
        int r;
        if (func == 0x01 || func == 0x02) r = 0;          /* critical section */
        else if (func == 0x03) r = 0;                     /* ChangeThreadSubFunction */
        else r = deliverEvent(0xF0000010, 0x4000);
        cpu.gpr[2] = r;
        cpu.gpr[0] = 0;
    }

    /* ==================================================================== */
    /*  A table - C library, console, GPU helpers                           */
    /* ==================================================================== */

    private int callA(int f) {
        switch (f) {
            /* ---- files ---------------------------------------------------- */
            case 0x00: return open(path(gpr(4)), gpr(5));
            case 0x01: return lseek(gpr(4), gpr(5), gpr(6));
            case 0x02: return read(gpr(4), gpr(5), gpr(6));
            case 0x03: return write(gpr(4), gpr(5), gpr(6));
            case 0x04: return close(gpr(4));
            case 0x05: return 0;                                  /* ioctl */
            case 0x06: return exit(gpr(4));
            case 0x07: return isatty(gpr(4));
            case 0x08: return getc(gpr(4));
            case 0x09: return putc(gpr(4), gpr(5));

            /* ---- string / memory ----------------------------------------- */
            case 0x0A: {                                          /* todigit */
                char c = (char) (gpr(4) & 0xFF);
                return (c >= '0' && c <= '9') ? (c - '0') : 0;
            }
            case 0x0C: return strtol(gpr(4), gpr(5), gpr(6), false);
            case 0x0D: return strtol(gpr(4), gpr(5), gpr(6), true);
            case 0x0E: return Math.abs(gpr(4));
            case 0x0F: return Math.abs(gpr(4));
            case 0x10: return strtol(gpr(4), 0, 10, true);
            case 0x11: return strtol(gpr(4), 0, 10, true);
            case 0x15: return strcat(gpr(4), gpr(5));
            case 0x16: return strncat(gpr(4), gpr(5), gpr(6));
            case 0x17: return strcmp(gpr(4), gpr(5), -1);
            case 0x18: return strcmp(gpr(4), gpr(5), gpr(6));
            case 0x19: return strcpy(gpr(4), gpr(5), -1);
            case 0x1A: return strcpy(gpr(4), gpr(5), gpr(6));
            case 0x1B: return strlen(gpr(4));
            case 0x1C:
            case 0x1E: return strchr(gpr(4), gpr(5), false);
            case 0x1D:
            case 0x1F: return strchr(gpr(4), gpr(5), true);
            case 0x20: return strpbrk(gpr(4), gpr(5));
            case 0x21: return strspn(gpr(4), gpr(5), true);
            case 0x22: return strspn(gpr(4), gpr(5), false);
            case 0x23: return 0;                                  /* strtok: rare */
            case 0x24: return strstr(gpr(4), gpr(5));
            case 0x25: {
                int c = gpr(4) & 0xFF;
                return (c >= 'a' && c <= 'z') ? c - 32 : c;
            }
            case 0x26: {
                int c = gpr(4) & 0xFF;
                return (c >= 'A' && c <= 'Z') ? c + 32 : c;
            }
            case 0x27: return memcpy(gpr(5), gpr(4), gpr(6));      /* bcopy(src,dst,n) */
            case 0x28: return memset(gpr(4), 0, gpr(5));
            case 0x29: return memcmp(gpr(4), gpr(5), gpr(6));
            case 0x2A: return memcpy(gpr(4), gpr(5), gpr(6));
            case 0x2B: return memset(gpr(4), gpr(5), gpr(6));
            case 0x2C: return memcpy(gpr(4), gpr(5), gpr(6));
            case 0x2D: return memcmp(gpr(4), gpr(5), gpr(6));
            case 0x2E: return memchr(gpr(4), gpr(5), gpr(6));

            /* ---- random / heap ------------------------------------------- */
            case 0x2F: {
                randState = randState * 1103515245 + 12345;
                return (randState >>> 16) & 0x7FFF;
            }
            case 0x30: randState = gpr(4); return 0;
            case 0x33: return malloc(gpr(4));
            case 0x34: return 0;                                  /* free */
            case 0x37: return calloc(gpr(4), gpr(5));
            case 0x38: return realloc(gpr(4), gpr(5));
            case 0x39: return initHeap(gpr(4), gpr(5));

            /* ---- console ------------------------------------------------- */
            case 0x3A: return exit(gpr(4));
            case 0x3B: return -1;                                 /* getchar */
            case 0x3C: return putchar(gpr(4));
            case 0x3D: return 0;                                  /* gets */
            case 0x3E: return puts(gpr(4));
            case 0x3F: return printf(gpr(4));

            /* ---- loader / cache ------------------------------------------ */
            case 0x41: return -1;                                 /* LoadTest   */
            case 0x42: return -1;                                 /* Load       */
            case 0x43: return -1;                                 /* Exec       */
            case 0x44: return 0;                                  /* FlushCache */
            case 0x45: return 0;                                  /* init vectors */
            case 0x51: return -1;                                 /* LoadExec   */
            case 0x52: return cpu.gpr[29];                        /* GetSysSp   */

            /* ---- GPU helpers --------------------------------------------- */
            case 0x46: return gpuDw(gpr(4), gpr(5), gpr(6), gpr(7), arg(4));
            case 0x47: return gpuSendDma(gpr(4), gpr(5), gpr(6), gpr(7), arg(4));
            case 0x48: machine.gpu.writeGP1(gpr(4)); return 0;
            case 0x49: machine.gpu.writeGP0(gpr(4)); return 0;
            case 0x4A: {
                int src = gpr(4), n = gpr(5);
                for (int i = 0; i < n; i++) machine.gpu.writeGP0(rd32(src + i * 4));
                return 0;
            }
            case 0x4B: return sendGpuList(gpr(4));
            case 0x4C: return 0;                                  /* gpu_abort_dma */
            case 0x4D: return machine.gpu.read(0x1F801814);
            case 0x4E: return 0;                                  /* gpu_sync   */

            /* ---- interrupts / devices ------------------------------------ */
            case 0x40:
            case 0x4F:
            case 0x50:
            case 0x53: return 0;                                  /* SystemError */
            case 0x54:
            case 0x55:
            case 0x56:
            case 0x70:
            case 0x71:
            case 0x72: return 0;                                  /* cdrom / bu init */
            case 0x57:
            case 0x58:
            case 0x59:
            case 0x5A:
            case 0x73:
            case 0x74:
            case 0x75:
            case 0x76:
            case 0x77: return 0;
            case 0x9C: return 0;                                  /* SetConf  */
            case 0x9D: return 0;                                  /* GetConf  */
            case 0x9F: return 0;                                  /* SetMem   */
            case 0xB4: return 0;                                  /* GetSystemInfo */
            default:
                unknown('A', f);
                return 0;
        }
    }

    /* ==================================================================== */
    /*  B table - kernel                                                    */
    /* ==================================================================== */

    private int callB(int f) {
        switch (f) {
            case 0x00: return allocKernelMemory(gpr(4));
            case 0x01: return 0;
            case 0x02: return 0;                                  /* init_timer */
            case 0x03: return 0;                                  /* get_timer  */
            case 0x04: return 0;                                  /* enable t irq */
            case 0x05: return 0;
            case 0x06: return 0;
            case 0x07: return deliverEvent(gpr(4), gpr(5));
            case 0x08: return openEvent(gpr(4), gpr(5), gpr(6), gpr(7));
            case 0x09: return closeEvent(gpr(4));
            case 0x0A: return waitEvent(gpr(4));
            case 0x0B: return testEvent(gpr(4));
            case 0x0C:
            case 0x0D: return 0;                                  /* enable / disable */
            case 0x0E: return 1;                                  /* OpenTh  */
            case 0x0F: return 0;                                  /* CloseTh */
            case 0x10: return 1;                                  /* ChangeTh */
            case 0x12:                                            /* InitPAD2 */
                padBuf1 = gpr(4);
                padSize1 = gpr(5);
                padBuf2 = gpr(6);
                padSize2 = gpr(7);
                return 0;
            case 0x13:
            case 0x14:
            case 0x15:
            case 0x16: return 0;                                  /* pad control */
            case 0x17: return 0;                                  /* ReturnFromException */
            case 0x18:
            case 0x19: return 0;                                  /* entry int hooks */
            case 0x20: return undeLiverEvent(gpr(4), gpr(5));
            case 0x32: return open(path(gpr(4)), gpr(5));
            case 0x33: return lseek(gpr(4), gpr(5), gpr(6));
            case 0x34: return read(gpr(4), gpr(5), gpr(6));
            case 0x35: return write(gpr(4), gpr(5), gpr(6));
            case 0x36: return close(gpr(4));
            case 0x37: return 0;
            case 0x38: return exit(gpr(4));
            case 0x39: return isatty(gpr(4));
            case 0x3A: return getc(gpr(4));
            case 0x3B: return putc(gpr(4), gpr(5));
            case 0x3C: return -1;
            case 0x3D: return putchar(gpr(4));
            case 0x3E: return 0;
            case 0x3F: return puts(gpr(4));
            case 0x40:
            case 0x41:
            case 0x42:
            case 0x43:
            case 0x44:
            case 0x45:
            case 0x46:
            case 0x47:
            case 0x48:
            case 0x49: return 0;
            case 0x4A:
            case 0x4B:
            case 0x4C:
            case 0x4D:
            case 0x4E:
            case 0x4F: return 0;                                  /* memory card */
            case 0x50: return 1;                                  /* _new_card */
            case 0x54: return errno;                              /* _get_errno */
            case 0x55: return 0;                                  /* _get_error */
            case 0x56: return 0xC0;                               /* GetC0Table */
            case 0x57: return 0xB0;                               /* GetB0Table */
            case 0x58: return 0;
            case 0x59: return 0;                                  /* testdevice */
            case 0x5B: return 0;                                  /* ChangeClearPAD */
            case 0x5C: return 1;                                  /* _card_status */
            case 0x5D: return 0;                                  /* _card_wait   */
            default:
                unknown('B', f);
                return 0;
        }
    }

    /* ==================================================================== */
    /*  C table - low level kernel                                          */
    /* ==================================================================== */

    private int callC(int f) {
        switch (f) {
            case 0x04: return 1;                                  /* free EvCB slot */
            case 0x05: return 1;                                  /* free TCB slot  */
            case 0x1D: return 0;
            default:
                return 0;
        }
    }

    /* ==================================================================== */
    /*  events                                                              */
    /* ==================================================================== */

    private int openEvent(int clazz, int spec, int mode, int func) {
        for (int i = 0; i < EV_MAX; i++) {
            if (evUsed[i]) continue;
            evUsed[i] = true;
            evClass[i] = clazz;
            evSpec[i] = spec;
            evMode[i] = mode;
            evStatus[i] = 0;
            return nextEvent + i;
        }
        errno = 0x0B;                                             /* too many events */
        return 0;
    }

    private int closeEvent(int handle) {
        int i = handle - nextEvent;
        if (i < 0 || i >= EV_MAX || !evUsed[i]) {
            errno = 9;
            return 0;
        }
        evUsed[i] = false;
        return 1;
    }

    private int deliverEvent(int clazz, int spec) {
        for (int i = 0; i < EV_MAX; i++) {
            if (!evUsed[i]) continue;
            if (evClass[i] != clazz) continue;
            if (spec != 0 && evSpec[i] != spec) continue;
            evStatus[i] = 1;
        }
        return 1;
    }

    private int undeLiverEvent(int clazz, int spec) {
        for (int i = 0; i < EV_MAX; i++) {
            if (!evUsed[i]) continue;
            if (evClass[i] != clazz) continue;
            if (spec != 0 && evSpec[i] != spec) continue;
            evStatus[i] = 0;
        }
        return 1;
    }

    private int waitEvent(int handle) {
        int i = handle - nextEvent;
        if (i < 0 || i >= EV_MAX || !evUsed[i]) {
            errno = 9;
            return 0;
        }
        evStatus[i] = 0;
        return 1;
    }

    private int testEvent(int handle) {
        int i = handle - nextEvent;
        if (i < 0 || i >= EV_MAX || !evUsed[i]) {
            errno = 9;
            return 0;
        }
        return evStatus[i] != 0 ? 1 : 0;
    }

    /* ==================================================================== */
    /*  files                                                               */
    /* ==================================================================== */

    private int open(String name, int mode) {
        if (name == null) {
            errno = 2;
            return -1;
        }
        String lower = name.toLowerCase();
        if (lower.indexOf("tty") >= 0 || lower.startsWith("con")) {
            return -1;                                            /* console fd */
        }
        int colon = lower.indexOf(':');
        String dev = colon > 0 ? lower.substring(0, colon) : "";
        if (!dev.startsWith("cdrom") && !dev.startsWith("cd")) {
            errno = 2;                                            /* no memory card */
            return -1;
        }
        String file = name.substring(colon + 1);
        while (file.startsWith("\\") || file.startsWith("/")) file = file.substring(1);
        if (file.length() == 0) {
            errno = 2;
            return -1;
        }
        CdImage cd = machine.cdrom == null ? null : machine.cdrom.cd;
        if (cd == null || !cd.isOpen()) {
            errno = 2;
            return -1;
        }
        try {
            int[] entry = Iso.findEntry(cd, simpleName(file));
            if (entry == null) {
                errno = 2;
                return -1;
            }
            for (int i = 0; i < FD_MAX; i++) {
                if (fdUsed[i]) continue;
                fdUsed[i] = true;
                fdLba[i] = entry[0];
                fdSize[i] = entry[1];
                fdPos[i] = 0;
                return i + 3;
            }
        } catch (Throwable t) {
            errno = 5;
        }
        errno = 0x18;                                             /* too many files */
        return -1;
    }

    private static String simpleName(String path) {
        int s = path.lastIndexOf('\\');
        int s2 = path.lastIndexOf('/');
        if (s2 > s) s = s2;
        return s >= 0 ? path.substring(s + 1) : path;
    }

    private int handleIndex(int fd) {
        int i = fd - 3;
        if (i < 0 || i >= FD_MAX || !fdUsed[i]) return -1;
        return i;
    }

    private int read(int fd, int dst, int len) {
        if (fd >= 0 && fd <= 2) return -1;                        /* stdin */
        int i = handleIndex(fd);
        if (i < 0) {
            errno = 9;
            return -1;
        }
        if (len <= 0) return 0;
        CdImage cd = machine.cdrom == null ? null : machine.cdrom.cd;
        if (cd == null) {
            errno = 9;
            return -1;
        }
        int done = 0;
        try {
            while (done < len && fdPos[i] < fdSize[i]) {
                int lba = fdLba[i] + fdPos[i] / 2048;
                int off = fdPos[i] % 2048;
                cd.readSector(lba, sector, 0);
                int chunk = 2048 - off;
                if (chunk > len - done) chunk = len - done;
                if (chunk > fdSize[i] - fdPos[i]) chunk = fdSize[i] - fdPos[i];
                for (int k = 0; k < chunk; k++) {
                    bus.write8(dst + done + k, sector[off + k] & 0xFF);
                }
                done += chunk;
                fdPos[i] += chunk;
            }
        } catch (Throwable t) {
            errno = 5;
            return done > 0 ? done : -1;
        }
        return done;
    }

    private int write(int fd, int src, int len) {
        if (fd < 0) {
            errno = 9;
            return -1;
        }
        int i = handleIndex(fd);
        if (fd <= 2 && i < 0) {
            for (int k = 0; k < len; k++) append((char) (rd8(src + k) & 0xFF));
            return len;
        }
        if (i < 0) {
            errno = 9;
            return -1;
        }
        for (int k = 0; k < len; k++) append((char) (rd8(src + k) & 0xFF));
        return len;
    }

    private int close(int fd) {
        if (fd >= 0 && fd <= 2) return 0;
        int i = handleIndex(fd);
        if (i < 0) {
            errno = 9;
            return -1;
        }
        fdUsed[i] = false;
        return 0;
    }

    private int lseek(int fd, int offset, int whence) {
        int i = handleIndex(fd);
        if (i < 0) {
            errno = 9;
            return -1;
        }
        int base;
        if (whence == 0) base = 0;
        else if (whence == 1) base = fdPos[i];
        else if (whence == 2) base = fdSize[i];
        else {
            errno = 0x16;
            return -1;
        }
        int pos = base + offset;
        if (pos < 0) pos = 0;
        fdPos[i] = pos;
        return pos;
    }

    private int isatty(int fd) { return (fd >= 0 && fd <= 2) ? 1 : 0; }

    private int getc(int fd) { return -1; }

    private int putc(int c, int fd) {
        append((char) (c & 0xFF));
        return c & 0xFF;
    }

    private int putchar(int c) {
        append((char) (c & 0xFF));
        return c & 0xFF;
    }

    private int puts(int addr) {
        String s = readString(addr, 4096);
        if (s == null) return -1;
        append(s);
        append('\n');
        return s.length() + 1;
    }

    private int exit(int code) {
        exited = true;
        machine.halt();
        return code;
    }

    /* ==================================================================== */
    /*  printf                                                              */
    /* ==================================================================== */

    private int printf(int fmtAddr) {
        String fmt = readString(fmtAddr, 2048);
        if (fmt == null) return -1;
        int written = 0;
        int argIndex = 0;
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (i < fmt.length()) {
            char c = fmt.charAt(i++);
            if (c != '%') {
                sb.append(c);
                written++;
                continue;
            }
            if (i >= fmt.length()) break;
            boolean left = false, zero = false, plus = false;
            int width = 0, prec = -1;
            while (i < fmt.length()) {
                char f = fmt.charAt(i);
                if (f == '-') { left = true; i++; }
                else if (f == '0') { zero = true; i++; }
                else if (f == '+') { plus = true; i++; }
                else if (f == ' ') { i++; }
                else if (f == '#') { i++; }
                else break;
            }
            while (i < fmt.length() && fmt.charAt(i) >= '0' && fmt.charAt(i) <= '9') {
                width = width * 10 + (fmt.charAt(i) - '0');
                i++;
            }
            if (i < fmt.length() && fmt.charAt(i) == '.') {
                i++;
                prec = 0;
                while (i < fmt.length() && fmt.charAt(i) >= '0' && fmt.charAt(i) <= '9') {
                    prec = prec * 10 + (fmt.charAt(i) - '0');
                    i++;
                }
            }
            if (i >= fmt.length()) break;
            char conv = fmt.charAt(i++);
            String text;
            if (conv == '%') {
                text = "%";
            } else if (conv == 'c') {
                text = String.valueOf((char) (arg(argIndex++ + 1) & 0xFF));
            } else if (conv == 's') {
                String s = readString(arg(argIndex++ + 1), 1024);
                text = s == null ? "(null)" : s;
                if (prec >= 0 && text.length() > prec) text = text.substring(0, prec);
            } else if (conv == 'd' || conv == 'i' || conv == 'u'
                    || conv == 'x' || conv == 'X' || conv == 'o' || conv == 'p') {
                int v = arg(argIndex++ + 1);
                boolean unsigned = (conv == 'u' || conv == 'x' || conv == 'X' || conv == 'o' || conv == 'p');
                text = formatNumber(v, conv, unsigned, plus);
            } else if (conv == 'f' || conv == 'e' || conv == 'g') {
                /* no floating point hardware in this emulator core */
                text = "0";
            } else {
                text = "%" + conv;
            }
            if (width > text.length()) {
                StringBuffer pad = new StringBuffer();
                int n = width - text.length();
                char pc = zero && !left ? '0' : ' ';
                for (int k = 0; k < n; k++) pad.append(pc);
                if (left) text = text + pad.toString();
                else text = pad.toString() + text;
            }
            sb.append(text);
            written += text.length();
        }
        append(sb.toString());
        return written;
    }

    /** o32 vararg fetch: a0..a3 then the stack at sp+16. */
    private int arg(int index) {
        if (index < 4) return cpu.gpr[4 + index];
        int sp = cpu.gpr[29];
        return rd32(sp + 16 + (index - 4) * 4);
    }

    private static String toOctal(int v) {
        if (v == 0) return "0";
        char[] buf = new char[12];
        int n = 0;
        while (v != 0) {
            buf[n++] = (char) ('0' + (v & 7));
            v = v >>> 3;
        }
        StringBuffer sb = new StringBuffer();
        for (int i = n - 1; i >= 0; i--) sb.append(buf[i]);
        return sb.toString();
    }

    private static String formatNumber(int v, char conv, boolean unsigned, boolean plus) {
        if (conv == 'c') return String.valueOf((char) (v & 0xFF));
        String digits;
        if (conv == 'x') digits = Integer.toHexString(v);
        else if (conv == 'X') digits = Integer.toHexString(v).toUpperCase();
        else if (conv == 'o') digits = toOctal(v);
        else if (conv == 'u') {
            long l = v & 0xFFFFFFFFL;
            return Long.toString(l);
        } else digits = Integer.toString(v);
        if (plus && v >= 0 && (conv == 'd' || conv == 'i')) digits = "+" + digits;
        return digits;
    }

    /* ==================================================================== */
    /*  strings / memory                                                    */
    /* ==================================================================== */

    private int strlen(int addr) {
        int n = 0;
        while (n < 0x10000 && rd8(addr + n) != 0) n++;
        return n;
    }

    private String readString(int addr, int max) {
        if (addr == 0) return null;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < max; i++) {
            int c = rd8(addr + i);
            if (c == 0) break;
            sb.append((char) c);
        }
        return sb.toString();
    }

    private int strcpy(int dst, int src, int max) {
        int i = 0;
        while (i < 0x10000) {
            int c = rd8(src + i);
            if (max >= 0 && i >= max) {
                if (max > 0) wr8(dst + max - 1, 0);
                return max;
            }
            wr8(dst + i, c);
            if (c == 0) break;
            i++;
        }
        return dst;
    }

    private int strcat(int dst, int src) {
        int d = dst + strlen(dst);
        int i = 0;
        while (i < 0x10000) {
            int c = rd8(src + i);
            wr8(d + i, c);
            if (c == 0) break;
            i++;
        }
        return dst;
    }

    private int strncat(int dst, int src, int n) {
        int d = dst + strlen(dst);
        for (int i = 0; i < n; i++) {
            int c = rd8(src + i);
            wr8(d + i, c);
            if (c == 0) return dst;
        }
        wr8(d + n, 0);
        return dst;
    }

    private int strcmp(int a, int b, int max) {
        int i = 0;
        while (max < 0 || i < max) {
            int ca = rd8(a + i), cb = rd8(b + i);
            if (ca != cb) return ca - cb;
            if (ca == 0) return 0;
            i++;
        }
        return 0;
    }

    private int strchr(int addr, int ch, boolean last) {
        int n = strlen(addr);
        int found = 0;
        for (int i = 0; i <= n; i++) {
            if ((rd8(addr + i) & 0xFF) == (ch & 0xFF)) {
                if (!last) return addr + i;
                found = addr + i;
            }
        }
        return found;
    }

    private int strpbrk(int addr, int list) {
        int n = strlen(addr);
        for (int i = 0; i < n; i++) {
            if (strchr(list, rd8(addr + i), false) != 0) return addr + i;
        }
        return 0;
    }

    private int strspn(int addr, int list, boolean span) {
        int n = strlen(addr);
        int i = 0;
        while (i < n) {
            boolean in = strchr(list, rd8(addr + i), false) != 0;
            if (in != span) break;
            i++;
        }
        return i;
    }

    private int strstr(int hay, int needle) {
        int hn = strlen(hay), nn = strlen(needle);
        if (nn == 0) return hay;
        for (int i = 0; i + nn <= hn; i++) {
            int k = 0;
            while (k < nn && rd8(hay + i + k) == rd8(needle + k)) k++;
            if (k == nn) return hay + i;
        }
        return 0;
    }

    private int memcpy(int dst, int src, int len) {
        if (len < 0) len = 0;
        if (len > 0x100000) len = 0x100000;
        for (int i = 0; i < len; i++) wr8(dst + i, rd8(src + i));
        return dst;
    }

    private int memset(int dst, int value, int len) {
        if (len < 0) len = 0;
        if (len > 0x100000) len = 0x100000;
        for (int i = 0; i < len; i++) wr8(dst + i, value & 0xFF);
        return dst;
    }

    private int memcmp(int a, int b, int len) {
        for (int i = 0; i < len; i++) {
            int ca = rd8(a + i), cb = rd8(b + i);
            if (ca != cb) return ca - cb;
        }
        return 0;
    }

    private int memchr(int addr, int ch, int len) {
        for (int i = 0; i < len; i++) {
            if ((rd8(addr + i) & 0xFF) == (ch & 0xFF)) return addr + i;
        }
        return 0;
    }

    private int strtol(int addr, int endAddr, int base, boolean signed) {
        if (base <= 1) base = 10;
        int p = addr;
        while (rd8(p) == ' ' || rd8(p) == '\t') p++;
        boolean neg = false;
        if (rd8(p) == '-') { neg = true; p++; }
        else if (rd8(p) == '+') p++;
        long value = 0;
        while (true) {
            int c = rd8(p);
            int d;
            if (c >= '0' && c <= '9') d = c - '0';
            else if (c >= 'a' && c <= 'z') d = c - 'a' + 10;
            else if (c >= 'A' && c <= 'Z') d = c - 'A' + 10;
            else break;
            if (d >= base) break;
            value = value * base + d;
            p++;
        }
        if (endAddr != 0) wr32(endAddr, p);
        int v = (int) value;
        return neg ? -v : v;
    }

    /* ==================================================================== */
    /*  heap                                                                */
    /* ==================================================================== */

    private int initHeap(int addr, int size) {
        if (addr == 0 || size <= 0) {
            heapBase = DEFAULT_HEAP;
            heapTop = DEFAULT_HEAP;
            heapEnd = HEAP_LIMIT;
            return 0;
        }
        heapBase = addr;
        heapTop = addr;
        heapEnd = addr + size;
        if (heapEnd > 0x80200000) heapEnd = 0x80200000;
        return 0;
    }

    private int malloc(int size) {
        if (size <= 0) return 0;
        size = (size + 15) & ~15;
        if (heapTop + size > heapEnd) {
            /* grow the heap up to the top of RAM like the real kernel does */
            errno = 0x0C;
            return 0;
        }
        int p = heapTop;
        heapTop += size;
        return p;
    }

    private int calloc(int n, int size) {
        int total = n * size;
        int p = malloc(total);
        if (p != 0) memset(p, 0, total);
        return p;
    }

    private int realloc(int ptr, int size) {
        int p = malloc(size);
        if (p != 0 && ptr != 0) memcpy(p, ptr, size);
        return p;
    }

    private int allocKernelMemory(int size) {
        if (size <= 0) return 0;
        size = (size + 15) & ~15;
        int p = kHeapTop;
        if (p + size > 0x801FF000) return 0;
        kHeapTop += size;
        return p;
    }

    /* ==================================================================== */
    /*  GPU helpers                                                         */
    /* ==================================================================== */

    /** A(46h) GPU_dw: uploads a rectangle of VRAM (Xdst,Ydst,Xsiz,Ysiz,src). */
    private int gpuDw(int x, int y, int w, int h, int src) {
        int words = (w * h + 1) / 2;
        machine.gpu.writeGP0(0xA0000000);
        machine.gpu.writeGP0((y << 16) | x);
        machine.gpu.writeGP0((h << 16) | w);
        for (int i = 0; i < words; i++) machine.gpu.writeGP0(rd32(src + i * 4));
        return 0;
    }

    /** A(47h) gpu_send_dma - same as GPU_dw but sourced from a DMA list. */
    private int gpuSendDma(int x, int y, int w, int h, int src) {
        return gpuDw(x, y, w, h, src);
    }

    /** A(4Bh) send_gpu_linked_list - each word is (length<<24)|address. */
    private int sendGpuList(int addr) {
        int guard = 0;
        int p = addr & 0x1FFFFC;
        while (guard++ < 0x2000) {
            int word = rd32(p);
            if (word == 0xFFFFFF) break;
            int len = (word >>> 24) & 0xFF;
            int src = word & 0xFFFFFF;
            for (int i = 0; i < len; i++) machine.gpu.writeGP0(rd32((src & 0x1FFFFC) + i * 4));
            if (len == 0) break;
            p = ((src + len * 4) & 0x1FFFFC);
        }
        return 0;
    }

    /* ==================================================================== */
    /*  helpers                                                             */
    /* ==================================================================== */

    private int gpr(int n) { return cpu.gpr[n]; }

    private int rd32(int a) { return bus.read32(a); }

    private void wr32(int a, int v) { bus.write32(a, v); }

    private int rd8(int a) { return bus.read8(a) & 0xFF; }

    private void wr8(int a, int v) { bus.write8(a, v & 0xFF); }

    private String path(int addr) { return readString(addr, 256); }

    private void append(char c) {
        if (output.length() >= OUTPUT_MAX) output.setLength(0);
        output.append(c);
    }

    private void append(String s) {
        if (s == null) return;
        if (output.length() + s.length() > OUTPUT_MAX) output.setLength(0);
        output.append(s);
    }

    private void unknown(char table, int func) {
        unknownCalls++;
        lastUnknown.setLength(0);
        lastUnknown.append(table).append('(').append(Integer.toHexString(func)).append(')');
    }

    public String getLastUnknown() { return lastUnknown.toString(); }
}
