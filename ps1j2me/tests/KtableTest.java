import psx.BiosHle;
import psx.PS1Machine;
import psx.Timers;

/**
 * Tests the parts of the BIOS kernel replacement that a game actually calls
 * during boot and while playing: the C table (interrupt and device setup),
 * the B table timer functions (which must program the root counters, not just
 * answer "fine") and the kernel bookkeeping they expose.
 *
 * Every call goes through the real vector trap: r9 holds the function number
 * and the CPU jumps to 0xA0/0xB0/0xC0, exactly like a retail game does.
 */
public class KtableTest {

    static int passed;
    static int failed;

    static final int PARK = 0x80010800;

    static void check(String what, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what + (detail == null ? "" : "  " + detail));
        } else {
            failed++;
            System.out.println("FAIL " + what + (detail == null ? "" : "  " + detail));
        }
    }

    static void eq(String what, int expected, int actual) {
        check(what, expected == actual,
                "expected 0x" + Integer.toHexString(expected) + ", got 0x" + Integer.toHexString(actual));
    }

    /** Calls a kernel function through its vector and returns v0. */
    static int bios(PS1Machine m, int table, int func, int a0, int a1, int a2, int a3) {
        m.cpu.gpr[4] = a0;
        m.cpu.gpr[5] = a1;
        m.cpu.gpr[6] = a2;
        m.cpu.gpr[7] = a3;
        m.cpu.gpr[29] = 0x801FFF00;
        m.cpu.gpr[9] = func;
        m.cpu.gpr[31] = PARK;
        m.cpu.pc = table;
        m.cpu.run(8);
        return m.cpu.gpr[2];
    }

    static int C(PS1Machine m, int func, int a0, int a1) { return bios(m, 0xC0, func, a0, a1, 0, 0); }
    static int B(PS1Machine m, int func, int a0, int a1, int a2) { return bios(m, 0xB0, func, a0, a1, a2, 0); }

    static void str(PS1Machine m, int addr, String s) {
        for (int i = 0; i < s.length(); i++) m.bus.write8(addr + i, s.charAt(i));
        m.bus.write8(addr + s.length(), 0);
    }

    public static void main(String[] args) {
        PS1Machine m = new PS1Machine(null);
        m.installHle();
        m.bus.write32(PARK, 0x1000FFFF);          /* beq zero, zero, -1 */
        m.bus.write32(PARK + 4, 0x00000000);
        BiosHle hle = m.biosHle;

        /* ---- C table: setup calls report success ------------------------ */
        eq("C(00) EnqueueTimerAndVblankIrqs", 1, C(m, 0x00, 1, 0));
        eq("C(01) EnqueueSyscallHandler", 1, C(m, 0x01, 0, 0));
        eq("C(02) SysEnqIntRP", 1, C(m, 0x02, 1, 0x80100000));
        eq("C(03) SysDeqIntRP", 1, C(m, 0x03, 1, 0x80100000));
        eq("C(04) get_free_EvCB_slot", 1, C(m, 0x04, 0, 0));
        eq("C(05) get_free_TCB_slot", 1, C(m, 0x05, 0, 0));
        eq("C(0C) InitDefInt", 1, C(m, 0x0C, 3, 0));
        eq("C(07) InstallExceptionHandlers", 0, C(m, 0x07, 0, 0));
        eq("C(0D) SetIrqAutoAck", 0, C(m, 0x0D, 2, 1));
        eq("C(12) InstallDevices", 0, C(m, 0x12, 1, 0));
        eq("C(13) FlushStdInOutPut", 0, C(m, 0x13, 0, 0));
        eq("C(0E) serial stub returns 0", 0, C(m, 0x0E, 0, 0));
        eq("C(14) serial stub returns 0", 0, C(m, 0x14, 0, 0));
        eq("C(17) _circgetc on empty buffer", -1, C(m, 0x17, 0, 0));

        /* ---- C(0A) ChangeClearRCnt returns the *previous* flag ----------- */
        eq("C(0A) first flag is 0", 0, C(m, 0x0A, 3, 1));
        eq("C(0A) stores the new flag", 1, C(m, 0x0A, 3, 1));
        eq("C(0A) clears it again", 1, C(m, 0x0A, 3, 0));
        eq("C(0A) now 0", 0, C(m, 0x0A, 3, 0));
        eq("C(0A) per-timer flags are separate", 0, C(m, 0x0A, 0, 1));

        /* ---- C(1A)/C(1D) card find mode round-trip ---------------------- */
        eq("C(1A) set_card_find_mode", 0, C(m, 0x1A, 1, 0));
        eq("C(1D) get_card_find_mode reads it back", 1, C(m, 0x1D, 0, 0));
        C(m, 0x1A, 0, 0);
        eq("C(1D) and returns to normal", 0, C(m, 0x1D, 0, 0));

        /* ---- C(08)/C(09) reset the kernel state ------------------------- */
        C(m, 0x0A, 2, 1);
        C(m, 0x1A, 1, 0);
        eq("C(09) SysInitKernelVariables", 0, C(m, 0x09, 0, 0));
        eq("C(09) cleared the ChangeClearRCnt flag", 0, C(m, 0x0A, 2, 0));
        eq("C(09) cleared the card find mode", 0, C(m, 0x1D, 0, 0));
        eq("C(08) SysInitMemory", 0, C(m, 0x08, 0x80180000, 0x1000));
        /* the kernel memory allocator must move into that region */
        eq("B(00) alloc_kernel_memory starts at the new region",
                0x80180000, B(m, 0x00, 64, 0, 0));
        eq("  and walks forward inside it", 0x80180040, B(m, 0x00, 64, 0, 0));
        eq("C(08) a region too small for the request fails",
                0, B(m, 0x00, 0x1000, 0, 0));
        C(m, 0x08, 0, 0);

        /* ---- unknown C entries are reported, not swallowed -------------- */
        int before = hle.unknownCalls;
        C(m, 0x1E, 0, 0);
        check("C(1E) is reported as unknown", hle.unknownCalls == before + 1,
                hle.getLastUnknown());
        check("the report names the C table", "C(1e)".equals(hle.getLastUnknown()),
                hle.getLastUnknown());
        before = hle.unknownCalls;
        C(m, 0x7F, 0, 0);
        check("C(7F) is reported too", hle.unknownCalls == before + 1, hle.getLastUnknown());

        /* ---- B table timers program the root counters ------------------- */
        Timers timers = m.timers;
        eq("B(02) init_timer(0, 100, 0)", 1, B(m, 0x02, 0, 100, 0));
        eq("  reload went to the target register", 100, timers.read(0x1F801108) & 0xFFFF);
        int mode = timers.read(0x1F801104) & 0xFFFF;
        check("  mode is 0x148 (sync 0x48 + clock source 0x100)", mode == 0x148,
                "mode=0x" + Integer.toHexString(mode));
        eq("B(02) init_timer(1, 5, 0x11)", 1, B(m, 0x02, 1, 5, 0x11));
        mode = timers.read(0x1F801114) & 0xFFFF;
        check("  flags bit4 sets sync mode 1 and bit0 keeps the clock source",
                mode == 0x49, "mode=0x" + Integer.toHexString(mode));
        eq("B(02) init_timer(2, 7, 0x1000) sets IRQ on target", 1, B(m, 0x02, 2, 7, 0x1000));
        mode = timers.read(0x1F801124) & 0xFFFF;
        check("  mode has the IRQ bit", (mode & 0x10) != 0, "mode=0x" + Integer.toHexString(mode));
        eq("B(02) rejects timer 3", 0, B(m, 0x02, 3, 1, 0));

        /* the counter must really count, and get_timer must report it */
        for (int i = 0; i < 40; i++) m.timers.step(100);
        int now = B(m, 0x03, 1, 0, 0);
        check("B(03) get_timer returns a running counter", now > 100, "value=" + now);
        eq("B(06) restart_timer", 1, B(m, 0x06, 1, 0, 0));
        eq("B(03) after restart the counter is zero", 0, B(m, 0x03, 1, 0, 0));
        eq("B(06) rejects timer 3", 0, B(m, 0x06, 3, 0, 0));
        eq("B(03) rejects timer 3", 0, B(m, 0x03, 3, 0, 0));

        /* ---- B(04)/B(05) timer and vblank IRQ enables -------------------- */
        m.bus.write16(0x1F801074, 0);
        eq("B(04) enable timer 0", 1, B(m, 0x04, 0, 0, 0));
        eq("  I_MASK bit 4 is set", 0x10, m.bus.read16(0x1F801074) & 0xFFFF);
        eq("B(04) enable timer 2", 1, B(m, 0x04, 2, 0, 0));
        eq("  I_MASK bit 6 is set as well", 0x50, m.bus.read16(0x1F801074) & 0xFFFF);
        eq("B(04) enable vblank returns 0", 0, B(m, 0x04, 3, 0, 0));
        eq("  I_MASK bit 0 is set", 0x51, m.bus.read16(0x1F801074) & 0xFFFF);
        eq("B(05) disable timer 0", 1, B(m, 0x05, 0, 0, 0));
        eq("  I_MASK bit 4 is clear", 0x41, m.bus.read16(0x1F801074) & 0xFFFF);
        eq("B(05) disable vblank", 1, B(m, 0x05, 3, 0, 0));
        eq("  I_MASK bit 0 is clear", 0x40, m.bus.read16(0x1F801074) & 0xFFFF);
        eq("B(05) disable bogus timer still reports success", 1, B(m, 0x05, 9, 0, 0));

        /* ---- B table events, pads, devices ------------------------------ */
        int ev = B(m, 0x08, 0xF0000010, 0x0004, 0x2000);
        check("B(08) OpenEvent hands out a descriptor", (ev & 0xFF000000) == 0xF1000000,
                "event=0x" + Integer.toHexString(ev));
        eq("B(0D) DisableEvent always returns 1", 1, B(m, 0x0D, 0x12345678, 0, 0));
        eq("B(0C) EnableEvent always returns 1", 1, B(m, 0x0C, 0x12345678, 0, 0));
        eq("B(09) CloseEvent", 1, B(m, 0x09, ev, 0, 0));

        eq("B(15) PAD_init2 accepts 0x20000000", 2, B(m, 0x15, 0x20000000, 0, 0));
        eq("B(15) PAD_init2 accepts 0x20000001", 2, B(m, 0x15, 0x20000001, 0, 0));
        eq("B(15) PAD_init2 rejects anything else", 0, B(m, 0x15, 0x20000002, 0, 0));

        str(m, 0x80100000, "cdrom");
        eq("B(59) testdevice(cdrom)", 1, B(m, 0x59, 0x80100000, 0, 0));
        str(m, 0x80100000, "tty");
        eq("B(59) testdevice(tty)", 1, B(m, 0x59, 0x80100000, 0, 0));
        str(m, 0x80100000, "bu00:");
        eq("B(59) testdevice(memory card) is absent", 0, B(m, 0x59, 0x80100000, 0, 0));
        str(m, 0x80100000, "nonsense");
        eq("B(59) testdevice(unknown)", 0, B(m, 0x59, 0x80100000, 0, 0));

        /* ---- B table "SystemError" stubs are not unknown calls ---------- */
        before = hle.unknownCalls;
        B(m, 0x1A, 0, 0, 0);
        B(m, 0x52, 0, 0, 0);
        B(m, 0x5A, 0, 0, 0);
        eq("B(1A/52/5A) do not count as unknown", before, hle.unknownCalls);
        eq("B(51) Krom2RawAdd reports -1 (no BIOS font)", -1, B(m, 0x51, 0x8140, 0, 0));

        /* ---- the CPU must have taken no exceptions along the way -------- */
        eq("no exceptions during the kernel calls", 0, m.getExceptionCount());

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL KERNEL TABLE TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("KERNEL TABLE TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }
}
