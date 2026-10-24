import psx.MemoryCD;
import psx.PS1Machine;

/**
 * Drives the CD-ROM controller the way a game does: through the register
 * interface at 0x1F801800, not through the BIOS file calls.
 *
 * <p>Everything here is observable behaviour that a retail disc depends on:</p>
 * <ul>
 *   <li>the register banks (RESULT/RDDATA readable from every bank, HINTMSK in
 *       banks 0/2, HINTSTS in banks 1/3);</li>
 *   <li>Setmode/Setloc/SeekL/ReadN sequencing and the INT3 &#8594; INT1 order;</li>
 *   <li>the sector contents delivered through RDDATA byte reads;</li>
 *   <li>the same sector arriving in RAM through DMA channel 3;</li>
 *   <li>acknowledging through HCLRCTL and the parameter FIFO clear.</li>
 * </ul>
 */
public class CdromRegTest {

    static int passed;
    static int failed;

    static final int CD = 0x1F801800;
    static final int SECTOR_CYCLES = 33868800 / 75;

    static void check(String what, boolean ok) { check(what, ok, null); }

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

    static void bank(PS1Machine m, int b) { m.cdrom.write(CD, b); }
    static int status(PS1Machine m) { return m.cdrom.read(CD) & 0xFF; }
    static int hintStat(PS1Machine m) { return m.cdrom.read(CD + 3) & 0xFF; }
    static int result(PS1Machine m) { return m.cdrom.read(CD + 1) & 0xFF; }
    static int rddat(PS1Machine m) { return m.cdrom.read(CD + 2) & 0xFF; }

    /** Sends a command with its parameters and waits for the acknowledge. */
    static void command(PS1Machine m, int cmd, int[] params) {
        bank(m, 0);
        for (int i = 0; i < params.length; i++) m.cdrom.write(CD + 2, params[i]);
        m.cdrom.write(CD + 1, cmd);
    }

    /** Steps the drive until an interrupt of the wanted type is pending. */
    static boolean waitIrq(PS1Machine m, int type, int maxSteps) {
        for (int i = 0; i < maxSteps; i++) {
            bank(m, 1);
            int st = hintStat(m) & 0x07;
            if (st == type) return true;
            if (st != 0 && st != type) return false;      /* wrong interrupt first */
            m.cdrom.step(SECTOR_CYCLES / 100);
        }
        return false;
    }

    static void ack(PS1Machine m) {
        bank(m, 1);
        m.cdrom.write(CD + 3, 0x07);
    }

    /** BCD helper: 16 -> 0x16. */
    static int bcd(int v) { return ((v / 10) << 4) | (v % 10); }

    public static void main(String[] args) throws Exception {
        MemoryCD image = MemoryCD.fromResource(CdromRegTest.class,
                "res/demos/demo_pong.iso", "pong");
        check("disc image is available", image != null);
        if (image == null) {
            System.out.println("CDROM TESTS FAILED: no image");
            System.exit(1);
            return;
        }

        PS1Machine m = new PS1Machine(image);
        m.cdrom.cd = image;

        /* ---- register banks -------------------------------------------- */
        bank(m, 0);
        eq("status reports bank 0", 0, status(m) & 3);
        bank(m, 3);
        eq("status reports bank 3", 3, status(m) & 3);
        eq("parameter FIFO starts empty", 0x08, status(m) & 0x08);
        bank(m, 0);
        m.cdrom.write(CD + 2, 0x11);
        eq("writing a parameter clears PRMEMPT", 0, status(m) & 0x08);
        m.cdrom.write(CD + 3, 0x00);
        eq("bank 0 register 3 is HCHPCTL (no enable change)", 0, readEnable(m));
        bank(m, 1);
        m.cdrom.write(CD + 2, 0x1F);
        eq("bank 1 register 2 writes HINTMSK", 0x1F, readEnable(m));
        bank(m, 0);
        eq("the mask is readable from bank 0 register 3", 0x1F, m.cdrom.read(CD + 3) & 0xFF);
        bank(m, 2);
        eq("and from bank 2 register 3", 0x1F, m.cdrom.read(CD + 3) & 0xFF);
        bank(m, 1);
        eq("bank 1 register 3 reads HINTSTS with the high bits set",
                0xE0, hintStat(m) & 0xE0);
        bank(m, 0);
        eq("RESULT is readable from bank 0 register 1", result(m), m.cdrom.read(CD + 1) & 0xFF);

        /* ---- GetStat: INT3, response, ack ------------------------------ */
        ack(m);
        command(m, 0x01, new int[0]);
        check("GetStat raises INT3", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        bank(m, 0);
        int stat = result(m);
        check("GetStat returns a status byte with the motor on",
                (stat & 0x02) != 0, "stat=0x" + Integer.toHexString(stat));
        ack(m);
        eq("HCLRCTL clears the interrupt flags", 0, hintStat(m) & 0x07);

        /* ---- GetID returns the region string --------------------------- */
        command(m, 0x1A, new int[0]);
        check("GetID raises INT3", waitIrq(m, 3, 300), "irq=" + (hintStat(m) & 7));
        bank(m, 0);
        int[] id = new int[8];
        for (int i = 0; i < 8; i++) id[i] = result(m);
        check("GetID response is a licensed disc with a region string",
                id[0] == 0x02 && id[4] == 'S' && id[5] == 'C' && id[6] == 'E' && id[7] == 'A',
                "id=" + hex(id));
        ack(m);

        /* ---- Setmode 0: 2048 byte sectors ------------------------------ */
        command(m, 0x0E, new int[] { 0x00 });
        check("Setmode acknowledges", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        ack(m);

        /* ---- Setloc in BCD, then SeekL --------------------------------- */
        int lba = 16;                                  /* the volume descriptor */
        int abs = lba + 150;
        int[] msf = { bcd(abs / (60 * 75)), bcd((abs / 75) % 60), bcd(abs % 75) };
        command(m, 0x02, msf);
        check("Setloc acknowledges", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        ack(m);
        command(m, 0x15, new int[0]);                  /* SeekL */
        check("SeekL acknowledges", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        ack(m);

        /* ---- ReadN: INT3, then one sector per 1/75 s as INT1 ----------- */
        command(m, 0x06, new int[0]);
        check("ReadN acknowledges with INT3", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        ack(m);
        check("the first sector arrives as INT1", waitIrq(m, 1, 400), "irq=" + (hintStat(m) & 7));
        eq("DRQSTS is set while data is waiting", 0x40, status(m) & 0x40);
        eq("BUSYSTS is clear once the command is acknowledged", 0, status(m) & 0x80);

        /* the sector must contain the ISO9660 volume descriptor */
        bank(m, 0);
        byte[] got = new byte[2048];
        for (int i = 0; i < 2048; i++) got[i] = (byte) rddat(m);
        byte[] want = new byte[2048];
        image.readSector(lba, want, 0);
        check("RDDATA returns the sector byte for byte", same(got, want),
                "first bytes " + hex(new int[] { got[0] & 0xFF, got[1] & 0xFF, got[2] & 0xFF, got[3] & 0xFF }));
        check("the sector really is the ISO9660 descriptor",
                got[1] == 'C' && got[2] == 'D' && got[3] == '0' && got[4] == '0' && got[5] == '1',
                "id=" + (char) got[1] + (char) got[2] + (char) got[3] + (char) got[4] + (char) got[5]);
        eq("reading past the end of the sector returns 0", 0, rddat(m));
        ack(m);
        eq("HCHPCTL BFRD bit is remembered", 0, m.cdrom.isBufferRequested() ? 1 : 0);
        bank(m, 0);
        m.cdrom.write(CD + 3, 0x80);
        check("HCHPCTL bit 7 asks for a buffer read", m.cdrom.isBufferRequested());

        /* ---- the next sector keeps coming, and is the next LBA --------- */
        check("the drive keeps reading sectors", waitIrq(m, 1, 400), "irq=" + (hintStat(m) & 7));
        bank(m, 0);
        byte[] got2 = new byte[2048];
        for (int i = 0; i < 2048; i++) got2[i] = (byte) rddat(m);
        byte[] want2 = new byte[2048];
        image.readSector(lba + 1, want2, 0);
        check("the second sector is LBA+1", same(got2, want2));
        ack(m);

        /* ---- the same sector through DMA channel 3 --------------------- */
        check("a third sector arrives", waitIrq(m, 1, 400), "irq=" + (hintStat(m) & 7));
        int[] ram = ram(m);
        for (int i = 0; i < 4096; i++) ram[i] = 0;
        m.bus.write32(0x1F8010B0, 0x00100000);        /* MADR: RAM 0x100000 */
        m.bus.write32(0x1F8010B4, 512);               /* 512 words = 2048 bytes */
        m.bus.write32(0x1F8010B8, 0x11000000);        /* start, from device, step 0 */
        byte[] got3 = new byte[2048];
        for (int i = 0; i < 2048; i++) got3[i] = (byte) m.bus.read8(0x80100000 + i);
        byte[] want3 = new byte[2048];
        image.readSector(lba + 2, want3, 0);
        check("DMA3 copies the sector into RAM", same(got3, want3),
                "chcr=0x" + Integer.toHexString(m.bus.read32(0x1F8010B8)));
        eq("DMA3 clears its busy flag", 0, m.bus.read32(0x1F8010B8) & 0x01000000);
        eq("the FIFO is empty after the transfer", 0, status(m) & 0x40);
        ack(m);

        /* ---- Pause stops the stream ------------------------------------ */
        command(m, 0x09, new int[0]);
        check("Pause acknowledges", waitIrq(m, 3, 200), "irq=" + (hintStat(m) & 7));
        ack(m);
        boolean quiet = true;
        for (int i = 0; i < 600; i++) {
            m.cdrom.step(SECTOR_CYCLES / 100);
            if ((hintStat(m) & 0x07) != 0) { quiet = false; break; }
        }
        check("no sectors arrive after Pause", quiet, "irq=" + (hintStat(m) & 7));

        /* ---- parameter FIFO clear via HCLRCTL -------------------------- */
        bank(m, 0);
        m.cdrom.write(CD + 2, 0x12);
        m.cdrom.write(CD + 2, 0x34);
        eq("two parameters are queued", 0, status(m) & 0x08);
        bank(m, 1);
        m.cdrom.write(CD + 3, 0x40);                  /* CLRPRM */
        bank(m, 0);
        eq("CLRPRM empties the parameter FIFO", 0x08, status(m) & 0x08);

        /* ---- an unknown command reports a disk error ------------------- */
        command(m, 0x55, new int[0]);
        check("an unknown command raises INT5", waitIrq(m, 5, 200), "irq=" + (hintStat(m) & 7));
        ack(m);

        /* ---- and the CPU never had to take an exception ---------------- */
        eq("no CPU exceptions during the whole run", 0, m.getExceptionCount());

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL CDROM REGISTER TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("CDROM TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    static int readEnable(PS1Machine m) {
        bank(m, 0);
        return m.cdrom.read(CD + 3) & 0x1F;
    }

    static int[] ram(PS1Machine m) { return m.bus.ram; }

    static boolean same(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
        return true;
    }

    static String hex(int[] v) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Integer.toHexString(v[i] & 0xFF));
        }
        return sb.toString();
    }
}
