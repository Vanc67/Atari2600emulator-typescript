import psx.BiosHle;
import psx.PS1Machine;

/**
 * Tests the BIOS kernel replacement (A0/B0/C0 tables).
 *
 * The vector trap is exercised the way a real game does it: a small MIPS
 * routine in RAM loads the function number into r9 and performs
 * {@code jal 0xA0}, which the CPU must answer without ever executing BIOS
 * code. If any of this breaks, retail games hang or jump into the exception
 * vector instead of running.
 */
public class HleBiosTest {

    static int failures;
    static final int CODE = 0x80010000;
    static final int PARK = 0x80010800;   /* j self: keeps the CPU away after a call */

    static void check(String what, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + what + (detail == null ? "" : "  " + detail));
        if (!ok) failures++;
    }

    static void put(PS1Machine m, int addr, int word) {
        m.bus.write32(addr, word);
    }

    static void str(PS1Machine m, int addr, String s) {
        for (int i = 0; i < s.length(); i++) m.bus.write8(addr + i, s.charAt(i));
        m.bus.write8(addr + s.length(), 0);
    }

    static String read(PS1Machine m, int addr) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 256; i++) {
            int c = m.bus.read8(addr + i) & 0xFF;
            if (c == 0) break;
            sb.append((char) c);
        }
        return sb.toString();
    }

    /** Invokes a BIOS function through the A0 vector and returns v0. */
    static int bios(PS1Machine m, int table, int func, int a0, int a1, int a2, int a3,
                    int stack0, int stack1) {
        m.cpu.gpr[4] = a0;
        m.cpu.gpr[5] = a1;
        m.cpu.gpr[6] = a2;
        m.cpu.gpr[7] = a3;
        m.cpu.gpr[29] = 0x801FFF00;
        m.bus.write32(0x801FFF10, stack0);
        m.bus.write32(0x801FFF14, stack1);
        m.cpu.gpr[9] = func;
        m.cpu.gpr[31] = PARK;
        m.cpu.pc = table;
        m.cpu.run(8);
        return m.cpu.gpr[2];
    }

    /** Builds "jal target" for a call inside the low 256 MiB. */
    static int jal(int target) {
        return 0x0C000000 | ((target >>> 2) & 0x03FFFFFF);
    }

    public static void main(String[] args) {
        PS1Machine m = new PS1Machine(null);
        m.installHle();
        /* a parking loop so the CPU does not wander off after an HLE call */
        put(m, PARK, 0x1000FFFF);        /* beq zero, zero, -1 */
        put(m, PARK + 4, 0x00000000);
        BiosHle hle = m.biosHle;
        check("HLE kernel installed", hle != null, null);

        /* ---- the vector trap ------------------------------------------- */
        str(m, 0x80010200, "HELLO");
        put(m, CODE, 0x3C048001);        /* lui   a0, 0x8001        */
        put(m, CODE + 4, 0x34840200);    /* ori   a0, a0, 0x0200    */
        put(m, CODE + 8, 0x2409001B);    /* addiu t1, zero, 0x1B    */
        put(m, CODE + 12, jal(0xA0));    /* jal   0xA0  (strlen)    */
        put(m, CODE + 16, 0x00000000);   /* nop (delay slot)        */
        put(m, CODE + 20, 0x2408002A);   /* addiu t0, zero, 42      */
        m.cpu.pc = CODE;
        m.cpu.gpr[29] = 0x801FFF00;
        m.cpu.run(64);
        check("A0(1Bh) strlen answered", m.cpu.gpr[2] == 5, "v0=" + m.cpu.gpr[2]);
        check("returned after the call", (m.cpu.pc & 0x1FFFFFFF) >= CODE + 16,
                "pc=" + Integer.toHexString(m.cpu.pc));

        /* ---- printf ----------------------------------------------------- */
        hle.clearOutput();
        str(m, 0x80010300, "n=%d s=%s x=%x");
        str(m, 0x80010340, "ok");
        bios(m, 0xA0, 0x3F, 0x80010300, 1234, 0x80010340, 0xBEEF, 0, 0);
        String out = hle.getOutput();
        check("printf formats %d/%s/%x", "n=1234 s=ok x=beef".equals(out), "'" + out + "'");

        /* ---- heap ------------------------------------------------------- */
        hle.reset();
        bios(m, 0xA0, 0x39, 0x80180000, 0x10000, 0, 0, 0, 0);    /* InitHeap */
        int p1 = bios(m, 0xA0, 0x33, 0x40, 0, 0, 0, 0, 0);       /* malloc   */
        int p2 = bios(m, 0xA0, 0x33, 0x40, 0, 0, 0, 0, 0);       /* malloc   */
        check("malloc returns heap pointers", p1 == 0x80180000 && p2 == 0x80180040,
                Integer.toHexString(p1) + " " + Integer.toHexString(p2));

        /* ---- strings ---------------------------------------------------- */
        str(m, 0x80010400, "abc");
        str(m, 0x80010440, "abd");
        int cmp = bios(m, 0xA0, 0x17, 0x80010400, 0x80010440, 0, 0, 0, 0);
        check("strcmp orders strings", cmp < 0, "v0=" + cmp);

        bios(m, 0xA0, 0x2B, 0x80010500, 0xA5, 16, 0, 0, 0);      /* memset */
        check("memset writes memory", (m.bus.read32(0x80010500) & 0xFF) == 0xA5
                && (m.bus.read8(0x8001050F) & 0xFF) == 0xA5, null);

        /* ---- events ----------------------------------------------------- */
        int ev = bios(m, 0xB0, 0x08, 0xF0000001, 0x0002, 1, 0, 0, 0);   /* OpenEvent */
        check("OpenEvent returns a handle", ev != 0, Integer.toHexString(ev));

        bios(m, 0xB0, 0x07, 0xF0000001, 0x0002, 0, 0, 0, 0);     /* DeliverEvent */
        int delivered = bios(m, 0xB0, 0x0B, ev, 0, 0, 0, 0, 0);  /* TestEvent */
        check("DeliverEvent marks the event", delivered == 1, "v0=" + delivered);

        long t0 = System.currentTimeMillis();
        bios(m, 0xB0, 0x0A, ev, 0, 0, 0, 0, 0);                  /* WaitEvent */
        check("WaitEvent returns", System.currentTimeMillis() - t0 < 1000, null);

        /* ---- unknown functions and exit ---------------------------------- */
        int before = hle.unknownCalls;
        int unknownResult = bios(m, 0xB0, 0xB7, 0, 0, 0, 0, 0, 0);   /* not implemented */
        check("unknown B function is harmless", hle.unknownCalls == before + 1
                && unknownResult == 0, hle.getLastUnknown());

        bios(m, 0xA0, 0x06, 0, 0, 0, 0, 0, 0);                      /* exit */
        check("exit halts the machine", m.halted && hle.hasExited(), null);
        m.runFrame();
        check("halted machine does no work", m.framesRun == 0, "frames=" + m.framesRun);

        System.out.println(failures == 0 ? "ALL HLE BIOS TESTS PASSED" : failures + " HLE BIOS FAILURES");
        System.exit(failures == 0 ? 0 : 1);
    }
}
