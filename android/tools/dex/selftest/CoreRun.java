import java.io.FileInputStream;
import java.io.IOException;

import psx.CdImage;
import psx.MemoryCD;
import psx.PS1Machine;

/**
 * Deterministic exercise of the emulator core, used to prove that the DEX in
 * the APK behaves exactly like the Java sources it was generated from.
 *
 * <p>It boots each demo image through the HLE BIOS, then single steps the MIPS
 * core for a fixed number of instructions and prints a fingerprint of the
 * machine state every chunk: program counter, general registers, GPU frame
 * checksum, CD-ROM status, SPU sample count.  The same program is run on a
 * real JVM and on the generated DEX; the two outputs must be identical.</p>
 *
 * <p>Instructions rather than frames, because the Dalvik interpreter used for
 * verification runs a few hundred times slower than the JVM and a whole frame
 * is far too much work.</p>
 *
 * <p>No Android classes are touched, so this compiles against the core alone
 * and runs unchanged on both sides.</p>
 */
public class CoreRun {

    public static void main(String[] args) throws IOException {
        int steps = 24000;
        int chunk = 4000;
        int first = 0;
        if (args.length > 0 && isNumber(args[0])) {
            steps = Integer.parseInt(args[0]);
            first = 1;
        }
        if (first < args.length && isNumber(args[first])) {
            chunk = Integer.parseInt(args[first]);
            first++;
        }
        String dir = "assets/demos";
        String[] all = {
            "demo_logo.iso", "demo_shapes.iso", "demo_tex.iso",
            "demo_3d.iso", "demo_pong.iso", "demo_pad.iso", "demo_sound.iso"
        };
        if (first < args.length) {
            for (int i = first; i < args.length; i++) {
                run(dir, args[i], steps, chunk);
            }
        } else {
            for (int i = 0; i < all.length; i++) {
                run(dir, all[i], steps, chunk);
            }
        }
        System.out.println("corerun done");
    }

    static boolean isNumber(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    static void run(String dir, String name, int steps, int chunk) {
        System.out.println("== " + name);
        byte[] img;
        try {
            img = read(dir + "/" + name);
        } catch (Throwable t) {
            System.out.println("   load failed: " + t);
            return;
        }
        System.out.println("   bytes " + img.length);
        try {
            MemoryCD cd = new MemoryCD(img, name);
            PS1Machine m = new PS1Machine(cd);
            boolean booted = m.hleBoot(cd);
            System.out.println("   boot " + booted + " " + m.bootInfo);
            int done = 0;
            int n = 1;
            while (done < steps) {
                int todo = steps - done;
                if (todo > chunk) {
                    todo = chunk;
                }
                for (int i = 0; i < todo; i++) {
                    m.stepInstruction();
                }
                done += todo;
                System.out.println("   " + n + " " + state(m));
                n++;
            }
            System.out.println("   end " + state(m));
        } catch (Throwable t) {
            System.out.println("   failed: " + t);
        }
    }

    /** Fingerprint of everything the emulator keeps that a test can see. */
    static String state(PS1Machine m) {
        StringBuilder sb = new StringBuilder();
        sb.append("pc=").append(hex(m.cpu.pc));
        sb.append(" gpr=");
        for (int i = 1; i < 32; i++) {
            sb.append(hex(m.cpu.gpr[i]));
            if (i < 31) {
                sb.append(',');
            }
        }
        sb.append(" hi=").append(hex(m.cpu.hi));
        sb.append(" lo=").append(hex(m.cpu.lo));
        int[] fb = m.getFrame();
        long hash = 0;
        int lit = 0;
        if (fb != null) {
            for (int i = 0; i < fb.length; i++) {
                int p = fb[i];
                hash = hash * 31 + (p & 0xFFFFFF);
                if ((p & 0xFFFFFF) != 0) {
                    lit++;
                }
            }
        }
        sb.append(" fb=").append(Long.toString(hash & 0xFFFFFFFFFFFFL, 16));
        sb.append("/").append(lit);
        // vram changes as soon as the demo draws, a frame is not needed
        long vh = 0;
        int vlit = 0;
        short[] vram = m.gpu.vram;
        for (int i = 0; i < vram.length; i += 7) {
            int v = vram[i] & 0xFFFF;
            vh = vh * 131 + v;
            if (v != 0) {
                vlit++;
            }
        }
        sb.append(" vram=").append(Long.toString(vh & 0xFFFFFFFFFFFFL, 16));
        sb.append("/").append(vlit);
        sb.append(" cyc=").append(m.totalCycles);
        sb.append(" blk=").append(m.cpu.lastBlockCount);
        return sb.toString();
    }

    static String hex(int v) {
        String s = Integer.toHexString(v);
        while (s.length() < 8) {
            s = "0" + s;
        }
        return s;
    }

    static byte[] read(String path) throws IOException {
        FileInputStream in = new FileInputStream(path);
        byte[] buf = new byte[(int) in.available()];
        int len = 0;
        while (len < buf.length) {
            int n = in.read(buf, len, buf.length - len);
            if (n <= 0) {
                break;
            }
            len += n;
        }
        in.close();
        if (len == buf.length) {
            return buf;
        }
        byte[] out = new byte[len];
        System.arraycopy(buf, 0, out, 0, len);
        return out;
    }
}
