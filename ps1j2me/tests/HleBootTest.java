import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;

import psx.PS1Machine;
import psx.VirtualCD;

/**
 * End to end test of the disc path: builds a minimal ISO9660 image with
 * SYSTEM.CNF + PSX.EXE, opens it through VirtualCD (rolling stream + sector
 * LRU), boots it with the HLE loader and checks that the program actually
 * painted VRAM through the GPU.
 */
public class HleBootTest {

    static int failures = 0;

    static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "PASS " : "FAIL ") + name + (detail == null ? "" : "  " + detail));
        if (!ok) failures++;
    }

    static final int SECTOR = 2048;

    static void putRecord(byte[] sec, int off, int lba, int size, boolean dir, String name) {
        int nameLen = name.length();
        int len = 33 + nameLen;
        if ((len & 1) != 0) len++;
        sec[off] = (byte) len;
        sec[off + 1] = 0;
        le32(sec, off + 2, lba);
        be32(sec, off + 6, lba);
        le32(sec, off + 10, size);
        be32(sec, off + 14, size);
        sec[off + 25] = (byte) (dir ? 2 : 0);
        sec[off + 28] = 1;
        sec[off + 31] = 1;
        sec[off + 32] = (byte) nameLen;
        for (int i = 0; i < nameLen; i++) sec[off + 33 + i] = (byte) name.charAt(i);
    }

    static void le32(byte[] b, int o, int v) {
        b[o] = (byte) v; b[o + 1] = (byte) (v >>> 8); b[o + 2] = (byte) (v >>> 16); b[o + 3] = (byte) (v >>> 24);
    }

    static void be32(byte[] b, int o, int v) {
        b[o + 3] = (byte) v; b[o + 2] = (byte) (v >>> 8); b[o + 1] = (byte) (v >>> 16); b[o] = (byte) (v >>> 24);
    }

    static byte[] program() {
        int[] code = new int[] {
            0x3C081F80,             // lui t0, 0x1F80
            0x35081810,             // ori t0, t0, 0x1810   (GPU GP0)
            0x3C090200,             // lui t1, 0x0200
            0x352900FF,             // ori t1, t1, 0x00FF   (fill, red)
            0xAD090000,             // sw t1, 0(t0)
            0x34090000,             // ori t1, zero, 0
            0xAD090000,             // sw t1, 0(t0)         (xy 0,0)
            0x3C0900F0,             // lui t1, 0x00F0
            0x35290140,             // ori t1, t1, 0x0140   (320x240)
            0xAD090000,             // sw t1, 0(t0)
            0x1000FFFF,             // beq zero,zero,-1
            0x00000000              // nop
        };
        byte[] b = new byte[code.length * 4];
        for (int i = 0; i < code.length; i++) le32(b, i * 4, code[i]);
        return b;
    }

    static byte[] psxExe(byte[] code) {
        byte[] exe = new byte[0x800 + code.length];
        String id = "PS-X EXE";
        for (int i = 0; i < id.length(); i++) exe[i] = (byte) id.charAt(i);
        le32(exe, 0x10, 0x80010000);   // pc
        le32(exe, 0x14, 0x00000000);   // gp
        le32(exe, 0x18, 0x80010000);   // load address
        le32(exe, 0x1C, code.length);  // size
        le32(exe, 0x30, 0x801FFFF0);   // stack
        System.arraycopy(code, 0, exe, 0x800, code.length);
        return exe;
    }

    static File buildIso(File dir) throws Exception {
        byte[] cnf = "BOOT = cdrom:\\PSX.EXE;1\r\nTCB = 4\r\nEVENT = 10\r\nSTACK = 801FFFF0\r\n".getBytes("ISO-8859-1");
        byte[] code = program();
        byte[] exe = psxExe(code);

        File iso = new File(dir, "test.iso");
        RandomAccessFile raf = new RandomAccessFile(iso, "rw");
        raf.setLength(24 * SECTOR);
        byte[] sec = new byte[SECTOR];

        // PVD
        sec[0] = 1;
        ascii(sec, 1, "CD001");
        sec[6] = 1;
        putRecord(sec, 156, 20, SECTOR, true, "");
        raf.seek(16 * SECTOR);
        raf.write(sec);

        // root directory
        sec = new byte[SECTOR];
        putRecord(sec, 0, 20, SECTOR, true, "");
        int off = sec[0] & 0xFF;
        putRecord(sec, off, 20, SECTOR, true, "");
        off += sec[off] & 0xFF;
        putRecord(sec, off, 21, cnf.length, false, "SYSTEM.CNF;1");
        off += sec[off] & 0xFF;
        putRecord(sec, off, 22, exe.length, false, "PSX.EXE;1");
        raf.seek(20 * SECTOR);
        raf.write(sec);

        raf.seek(21L * SECTOR);
        raf.write(cnf);
        raf.seek(22L * SECTOR);
        raf.write(exe);
        raf.close();
        return iso;
    }

    static void ascii(byte[] b, int o, String s) {
        for (int i = 0; i < s.length(); i++) b[o + i] = (byte) s.charAt(i);
    }

    public static void main(String[] args) throws Exception {
        File dir = new File("build/testtmp");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new Exception("cannot create " + dir);
        File iso = buildIso(dir);

        VirtualCD cd = new VirtualCD();
        cd.open("file://" + iso.getAbsolutePath());
        check("iso layout detected", cd.getLayout() == VirtualCD.LAYOUT_2048, "layout=" + cd.getLayout());
        check("iso sector count", cd.getSectorCount() == 24, "sectors=" + cd.getSectorCount());

        byte[] sec = new byte[2048];
        cd.readSector(16, sec, 0);
        check("iso pvd readable", sec[1] == 'C' && sec[2] == 'D' && sec[3] == '0', "id=" + (char) sec[1] + (char) sec[2] + (char) sec[3]);

        PS1Machine m = new PS1Machine(cd);
        m.setViewport(320, 240);
        boolean booted = m.hleBoot(cd);
        check("hle boot", booted, "bootInfo=" + m.bootInfo);
        check("exe entry point", m.cpu.pc == 0x80010000, "pc=" + Integer.toHexString(m.cpu.pc));

        for (int i = 0; i < 3; i++) m.runFrame();
        int px = m.gpu.vram[10 * 1024 + 10] & 0xFFFF;
        check("game painted VRAM", px == 0x001F, "px=" + Integer.toHexString(px));

        int[] frame = m.getFrame();
        check("frame ready", frame != null && frame[10 * 320 + 10] == 0xFFF80000,
                "frame=" + Integer.toHexString(frame[10 * 320 + 10]));

        // backward seek forces the rolling stream to reopen the file
        byte[] s1 = new byte[2048];
        byte[] s2 = new byte[2048];
        cd.readSector(22, s1, 0);
        cd.readSector(16, s2, 0);
        check("rolling stream random access", s1[0] == 'P' && s2[1] == 'C', "" + (char) s1[0] + (char) s2[1]);

        cd.close();
        System.out.println(failures == 0 ? "ALL HLE TESTS PASSED" : failures + " FAILURES");
        if (failures != 0) System.exit(1);
    }
}
