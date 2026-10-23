package psx;

/**
 * PlayStation Geometry Transformation Engine (COP2).
 *
 * All math is two's complement fixed point (16.16 / 4.12) using 32 bit
 * integers with 64 bit intermediates - no floating point at all.
 *
 * Control register layout (the real one):
 *   0..4   rotation matrix  (5 regs, 9 packed 16 bit coefficients)
 *   5..7   TRX, TRY, TRZ   (32 bit)
 *   8..12  light matrix     (packed)
 *   13..15 RBK, GBK, BBK
 *   16..20 light colour matrix (packed)
 *   21..23 RFC, GFC, BFC
 *   24..25 OFX, OFY (32 bit)
 *   26     H  (16 bit)
 *   27..28 DQA, DQB (16 bit)
 *   29..30 ZSF3, ZSF4 (16 bit)
 *   31     FLAG
 */
public final class Gte {

    public final int[] data = new int[32];
    public final int[] ctrl = new int[32];

    /* data registers */
    public static final int VXY0 = 0, VZ0 = 1, VXY1 = 2, VZ1 = 3, VXY2 = 4, VZ2 = 5,
            RGBC = 6, OTZ = 7, IR0 = 8, IR1 = 9, IR2 = 10, IR3 = 11,
            SXY0 = 12, SXY1 = 13, SXY2 = 14, SXYP = 15,
            SZ0 = 16, SZ1 = 17, SZ2 = 18, SZ3 = 19,
            RGB0 = 20, RGB1 = 21, RGB2 = 22, RES1 = 23,
            MAC0 = 24, MAC1 = 25, MAC2 = 26, MAC3 = 27,
            IRGB = 28, ORGB = 29, LZCS = 30, LZCR = 31;

    /* control registers */
    public static final int RT = 0, TRX = 5, TRY = 6, TRZ = 7,
            LM = 8, RBK = 13, GBK = 14, BBK = 15,
            LCM = 16, RFC = 21, GFC = 22, BFC = 23,
            OFX = 24, OFY = 25, H = 26, DQA = 27, DQB = 28,
            ZSF3 = 29, ZSF4 = 30, FLAG = 31;

    private int flag;

    private long mac1, mac2, mac3;
    private int ir1, ir2, ir3;

    public void reset() {
        for (int i = 0; i < 32; i++) {
            data[i] = 0;
            ctrl[i] = 0;
        }
        flag = 0;
    }

    /* ---- register access ------------------------------------------------ */

    public int readData(int r) {
        return data[r & 31];
    }

    public void writeData(int r, int v) {
        switch (r & 31) {
            case IR0:
                data[IR0] = (short) v;
                return;
            case IR1: case IR2: case IR3:
                data[r & 31] = (short) v;
                return;
            case LZCS:
                data[LZCS] = v;
                data[LZCR] = countLeading(v);
                return;
            default:
                data[r & 31] = v;
        }
    }

    public int readControl(int r) {
        int i = r & 31;
        if (i == FLAG) return flag;
        return ctrl[i];
    }

    public void writeControl(int r, int v) {
        int i = r & 31;
        switch (i) {
            case H: case DQA: case DQB: case ZSF3: case ZSF4:
                ctrl[i] = (short) v;
                return;
            case FLAG:
                flag = v & 0x7F87E000;
                return;
            default:
                ctrl[i] = v;
        }
    }

    /* ---- helpers -------------------------------------------------------- */

    private static int countLeading(int v) {
        int n = 0;
        for (int i = 31; i >= 0; i--) {
            if ((v & (1 << i)) != 0) break;
            n++;
        }
        return n;
    }

    private static int satIr(long v, Gte g) {
        if (v > 0x7FFF) { g.flag |= (1 << 24); return 0x7FFF; }
        if (v < -0x8000) { g.flag |= (1 << 24); return -0x8000; }
        return (int) v;
    }

    private static long satMac(long v, Gte g) {
        if (v > 0x7FFFFFFFFFFL) { g.flag |= (1 << 27); return 0x7FFFFFFFFFFL; }
        if (v < -0x80000000000L) { g.flag |= (1 << 27); return -0x80000000000L; }
        return v;
    }

    /** 44 bit fixed point divide used by RTPS / RTPT / DPCS / INTPL. */
    private long unrDiv(long n, long d) {
        if (d == 0) {
            flag |= (1 << 17);
            return (n >= 0) ? 0x7FFFFFFFFFFL : -0x7FFFFFFFFFFL;
        }
        long r = (n << 16) / d;
        if (r > 0x7FFFFFFFFFFL) { flag |= (1 << 17); return 0x7FFFFFFFFFFL; }
        if (r < -0x80000000000L) { flag |= (1 << 17); return -0x80000000000L; }
        return r;
    }

    private static int sx(int v) { return (short) (v & 0xFFFF); }
    private static int sy(int v) { return (short) ((v >>> 16) & 0xFFFF); }
    private static int packXY(int x, int y) { return (x & 0xFFFF) | ((y & 0xFFFF) << 16); }

    private void pushSxy(int x, int y) {
        data[SXY0] = data[SXY1];
        data[SXY1] = data[SXY2];
        data[SXY2] = packXY(x, y);
        data[SXYP] = data[SXY2];
    }

    private void pushSz(int z) {
        data[SZ0] = data[SZ1];
        data[SZ1] = data[SZ2];
        data[SZ2] = z & 0xFFFF;
    }

    private void pushRgb(int r, int g, int b, int code) {
        data[RGB0] = data[RGB1];
        data[RGB1] = data[RGB2];
        data[RGB2] = (code & 0xFF000000) | ((b & 0xFF) << 16) | ((g & 0xFF) << 8) | (r & 0xFF);
    }

    /** Reads one of the nine packed 16 bit coefficients of a 3x3 matrix. */
    private int mtx(int base, int idx) {
        int reg = ctrl[base + (idx >> 1)];
        return (short) (((idx & 1) == 0) ? (reg & 0xFFFF) : (reg >>> 16));
    }

    /* ---- matrix / vector multiply --------------------------------------- */

    private void mulVec(int mtxSel, int vecSel, int trSel, boolean sf, boolean lm) {
        int base = (mtxSel == 0) ? RT : ((mtxSel == 1) ? LM : LCM);
        int m0 = mtx(base, 0), m1 = mtx(base, 1), m2 = mtx(base, 2);
        int m3 = mtx(base, 3), m4 = mtx(base, 4), m5 = mtx(base, 5);
        int m6 = mtx(base, 6), m7 = mtx(base, 7), m8 = mtx(base, 8);

        long vx, vy, vz;
        switch (vecSel) {
            case 0: vx = sx(data[VXY0]); vy = sy(data[VXY0]); vz = data[VZ0] & 0xFFFF; break;
            case 1: vx = sx(data[VXY1]); vy = sy(data[VXY1]); vz = data[VZ1] & 0xFFFF; break;
            case 2: vx = sx(data[VXY2]); vy = sy(data[VXY2]); vz = data[VZ2] & 0xFFFF; break;
            default: vx = (short) data[IR1]; vy = (short) data[IR2]; vz = (short) data[IR3]; break;
        }

        long tx = 0, ty = 0, tz = 0;
        if (trSel == 0) {
            tx = ctrl[TRX]; ty = ctrl[TRY]; tz = ctrl[TRZ];
        } else if (trSel == 1) {
            tx = (short) ctrl[RBK]; ty = (short) ctrl[GBK]; tz = (short) ctrl[BBK];
        } else if (trSel == 2) {
            tx = (short) ctrl[RFC]; ty = (short) ctrl[GFC]; tz = (short) ctrl[BFC];
        }

        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; tx <<= 12; ty <<= 12; tz <<= 12; }

        mac1 = satMac(tx + (m0 * vx + m1 * vy + m2 * vz), this);
        mac2 = satMac(ty + (m3 * vx + m4 * vy + m5 * vz), this);
        mac3 = satMac(tz + (m6 * vx + m7 * vy + m8 * vz), this);

        if (sf) {
            ir1 = satIr(mac1 >> 12, this);
            ir2 = satIr(mac2 >> 12, this);
            ir3 = satIr(mac3 >> 12, this);
            data[IR1] = ir1;
            data[IR2] = ir2;
            data[IR3] = ir3;
        }
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
    }

    /* ---- command dispatch ----------------------------------------------- */

    public void execute(int cmd) {
        flag = 0;
        int op = cmd & 0x3F;
        boolean sf = (cmd & (1 << 19)) != 0;
        boolean lm = (cmd & (1 << 10)) != 0;
        int mx = (cmd >> 17) & 3;
        int vx = (cmd >> 15) & 3;
        int cv = (cmd >> 13) & 3;
        switch (op) {
            case 0x01: rtps(0); break;
            case 0x30: rtps(0); rtps(1); rtps(2); break;
            case 0x06: nclip(); break;
            case 0x0C: op(sf, lm); break;
            case 0x10: dpcs(sf, lm); break;
            case 0x11: intpl(sf); break;
            case 0x12: mulVec(mx, vx, cv, sf, lm); break;
            case 0x13: ncds(sf); break;
            case 0x14: cdp(sf, lm); break;
            case 0x16: ncdt(sf); break;
            case 0x1B: nccs(sf); break;
            case 0x1C: cc(sf, lm); break;
            case 0x1E: ncs(sf); break;
            case 0x20: nct(sf); break;
            case 0x28: sqr(sf, lm); break;
            case 0x29: dcpl(sf, lm); break;
            case 0x2A: dpct(sf, lm); break;
            case 0x2B: avsz(3); break;
            case 0x2C: avsz(4); break;
            case 0x3D: gpf(sf, lm); break;
            case 0x3E: gpl(sf, lm); break;
            case 0x3F: ncct(sf); break;
            default: flag |= 0;
        }
        if (flag != 0) flag |= 0x80000000;
        ctrl[FLAG] = flag;
    }

    /* ---- individual commands -------------------------------------------- */

    private void rtps(int idx) {
        int vxy;
        int vz;
        switch (idx) {
            case 0: vxy = data[VXY0]; vz = data[VZ0] & 0xFFFF; break;
            case 1: vxy = data[VXY1]; vz = data[VZ1] & 0xFFFF; break;
            default: vxy = data[VXY2]; vz = data[VZ2] & 0xFFFF; break;
        }
        int vX = sx(vxy), vY = sy(vxy), vZ = vz;

        long m0 = mtx(RT, 0), m1 = mtx(RT, 1), m2 = mtx(RT, 2);
        long m3 = mtx(RT, 3), m4 = mtx(RT, 4), m5 = mtx(RT, 5);
        long m6 = mtx(RT, 6), m7 = mtx(RT, 7), m8 = mtx(RT, 8);

        mac1 = satMac(((long) ctrl[TRX] << 12) + (m0 * vX + m1 * vY + m2 * vZ), this);
        mac2 = satMac(((long) ctrl[TRY] << 12) + (m3 * vX + m4 * vY + m5 * vZ), this);
        mac3 = satMac(((long) ctrl[TRZ] << 12) + (m6 * vX + m7 * vY + m8 * vZ), this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;

        ir1 = satIr(mac1 >> 12, this);
        ir2 = satIr(mac2 >> 12, this);
        ir3 = satIr(mac3 >> 12, this);
        data[IR1] = ir1;
        data[IR2] = ir2;
        data[IR3] = ir3;

        int z = (int) (mac3 >> 12);
        if (z < 0) { z = 0; flag |= (1 << 18); }
        if (z > 0xFFFF) { z = 0xFFFF; flag |= (1 << 18); }
        pushSz(z);
        data[SZ3] = z;

        long h = ctrl[H] & 0xFFFF;
        long div = unrDiv(h, z);
        data[MAC0] = (int) (div >> 16);

        long sxv = (long) ctrl[OFX] + ((div * ir1) >> 16);
        long syv = (long) ctrl[OFY] + ((div * ir2) >> 16);
        if (sxv < -1024) { sxv = -1024; flag |= (1 << 14); }
        if (sxv > 1023) { sxv = 1023; flag |= (1 << 14); }
        if (syv < -1024) { syv = -1024; flag |= (1 << 13); }
        if (syv > 1023) { syv = 1023; flag |= (1 << 13); }
        pushSxy((int) sxv, (int) syv);
        data[OTZ] = z;
    }

    private void nclip() {
        int x0 = sx(data[SXY0]), y0 = sy(data[SXY0]);
        int x1 = sx(data[SXY1]), y1 = sy(data[SXY1]);
        int x2 = sx(data[SXY2]), y2 = sy(data[SXY2]);
        long v = (long) x0 * (y1 - y2) + (long) x1 * (y2 - y0) + (long) x2 * (y0 - y1);
        data[MAC0] = (int) v;
    }

    private void avsz(int n) {
        long sum;
        if (n == 3) {
            sum = (data[SZ0] & 0xFFFF) + (data[SZ1] & 0xFFFF) + (data[SZ2] & 0xFFFF);
            sum *= (short) ctrl[ZSF3];
        } else {
            sum = (data[SZ0] & 0xFFFF) + (data[SZ1] & 0xFFFF) + (data[SZ2] & 0xFFFF) + (data[SZ3] & 0xFFFF);
            sum *= (short) ctrl[ZSF4];
        }
        data[MAC0] = (int) sum;
        long otz = sum >> 12;
        if (otz < 0) { otz = 0; flag |= (1 << 18); }
        if (otz > 0xFFFF) { otz = 0xFFFF; flag |= (1 << 18); }
        data[OTZ] = (int) otz;
    }

    private void op(boolean sf, boolean lm) {
        long vx = (short) data[IR1], vy = (short) data[IR2], vz = (short) data[IR3];
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac(mtx(RT, 0) * vx + mtx(RT, 1) * vy + mtx(RT, 2) * vz, this);
        mac2 = satMac(mtx(RT, 3) * vx + mtx(RT, 4) * vy + mtx(RT, 5) * vz, this);
        mac3 = satMac(mtx(RT, 6) * vx + mtx(RT, 7) * vy + mtx(RT, 8) * vz, this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        if (sf) {
            data[IR1] = satIr(mac1 >> 12, this);
            data[IR2] = satIr(mac2 >> 12, this);
            data[IR3] = satIr(mac3 >> 12, this);
        }
    }

    private void sqr(boolean sf, boolean lm) {
        long vx = (short) data[IR1], vy = (short) data[IR2], vz = (short) data[IR3];
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac(mtx(RT, 0) * vx, this);
        mac2 = satMac(mtx(RT, 1) * vy, this);
        mac3 = satMac(mtx(RT, 2) * vz, this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        if (sf) {
            data[IR1] = satIr(mac1 >> 12, this);
            data[IR2] = satIr(mac2 >> 12, this);
            data[IR3] = satIr(mac3 >> 12, this);
        }
    }

    private void gpf(boolean sf, boolean lm) {
        long vx = (short) data[IR1], vy = (short) data[IR2], vz = (short) data[IR3];
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac((long) (short) ctrl[RFC] * vx, this);
        mac2 = satMac((long) (short) ctrl[GFC] * vy, this);
        mac3 = satMac((long) (short) ctrl[BFC] * vz, this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        if (sf) {
            data[IR1] = satIr(mac1 >> 12, this);
            data[IR2] = satIr(mac2 >> 12, this);
            data[IR3] = satIr(mac3 >> 12, this);
        }
    }

    private void gpl(boolean sf, boolean lm) {
        long vx = (short) data[IR1], vy = (short) data[IR2], vz = (short) data[IR3];
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac(((long) (short) ctrl[RFC] * vx << 12) + ((long) ctrl[TRX] << 12), this);
        mac2 = satMac(((long) (short) ctrl[GFC] * vy << 12) + ((long) ctrl[TRY] << 12), this);
        mac3 = satMac(((long) (short) ctrl[BFC] * vz << 12) + ((long) ctrl[TRZ] << 12), this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        if (sf) {
            data[IR1] = satIr(mac1 >> 12, this);
            data[IR2] = satIr(mac2 >> 12, this);
            data[IR3] = satIr(mac3 >> 12, this);
        }
    }

    private void intpl(boolean sf) {
        long base = (long) ctrl[RFC] << 12;
        mac1 = satMac(base + ((long) (short) data[IR1] << 12), this);
        mac2 = satMac(base + ((long) (short) data[IR2] << 12), this);
        mac3 = satMac(base + ((long) (short) data[IR3] << 12), this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        if (sf) {
            data[IR1] = satIr(mac1 >> 12, this);
            data[IR2] = satIr(mac2 >> 12, this);
            data[IR3] = satIr(mac3 >> 12, this);
        }
    }

    private void dpcs(boolean sf, boolean lm) {
        long vx = data[RGBC] & 0xFF, vy = (data[RGBC] >>> 8) & 0xFF, vz = (data[RGBC] >>> 16) & 0xFF;
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac(((long) ctrl[RFC] << 12) + (vx << 4), this);
        mac2 = satMac(((long) ctrl[GFC] << 12) + (vy << 4), this);
        mac3 = satMac(((long) ctrl[BFC] << 12) + (vz << 4), this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        int r = satIr(mac1 >> 12, this), g = satIr(mac2 >> 12, this), b = satIr(mac3 >> 12, this);
        if (sf) {
            data[IR1] = r;
            data[IR2] = g;
            data[IR3] = b;
        }
        pushRgb(r, g, b, data[RGBC] & 0xFF000000);
    }

    private void dcpl(boolean sf, boolean lm) {
        long vx = data[RGBC] & 0xFF, vy = (data[RGBC] >>> 8) & 0xFF, vz = (data[RGBC] >>> 16) & 0xFF;
        if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
        mac1 = satMac((long) (short) ctrl[RFC] * vx << 4, this);
        mac2 = satMac((long) (short) ctrl[GFC] * vy << 4, this);
        mac3 = satMac((long) (short) ctrl[BFC] * vz << 4, this);
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        int r = satIr(mac1 >> 12, this), g = satIr(mac2 >> 12, this), b = satIr(mac3 >> 12, this);
        if (sf) {
            data[IR1] = r;
            data[IR2] = g;
            data[IR3] = b;
        }
        pushRgb(r, g, b, data[RGBC] & 0xFF000000);
    }

    private void dpct(boolean sf, boolean lm) {
        for (int i = 0; i < 3; i++) {
            int rgb = data[RGB0];
            long vx = rgb & 0xFF, vy = (rgb >>> 8) & 0xFF, vz = (rgb >>> 16) & 0xFF;
            if (lm) { vx <<= 12; vy <<= 12; vz <<= 12; }
            mac1 = satMac(((long) ctrl[RFC] << 12) + (vx << 4), this);
            mac2 = satMac(((long) ctrl[GFC] << 12) + (vy << 4), this);
            mac3 = satMac(((long) ctrl[BFC] << 12) + (vz << 4), this);
            int r = satIr(mac1 >> 12, this), g = satIr(mac2 >> 12, this), b = satIr(mac3 >> 12, this);
            if (sf) {
                data[IR1] = r;
                data[IR2] = g;
                data[IR3] = b;
            }
            pushRgb(r, g, b, rgb & 0xFF000000);
            data[RGB0] = data[RGB1];
            data[RGB1] = data[RGB2];
        }
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
    }

    /* Colour + light commands: light matrix * vector, then modulate with RGBC
       and add the far colour, exactly like NCS/NCD pipelined on hardware. */

    private void ncs(boolean sf) {
        mulVec(1, 0, 1, false, false);
        long r = (data[RGBC] & 0xFF) * (long) (short) data[IR1];
        long g = ((data[RGBC] >>> 8) & 0xFF) * (long) (short) data[IR2];
        long b = ((data[RGBC] >>> 16) & 0xFF) * (long) (short) data[IR3];
        mac1 = satMac((r << 4) + ((long) ctrl[RFC] << 12), this);
        mac2 = satMac((g << 4) + ((long) ctrl[GFC] << 12), this);
        mac3 = satMac((b << 4) + ((long) ctrl[BFC] << 12), this);
        int rr = satIr(mac1 >> 12, this), gg = satIr(mac2 >> 12, this), bb = satIr(mac3 >> 12, this);
        if (sf) {
            data[IR1] = rr;
            data[IR2] = gg;
            data[IR3] = bb;
        }
        data[MAC1] = (int) mac1;
        data[MAC2] = (int) mac2;
        data[MAC3] = (int) mac3;
        pushRgb(rr, gg, bb, data[RGBC] & 0xFF000000);
    }

    private void nct(boolean sf) { ncs(sf); ncs(sf); ncs(sf); }
    private void nccs(boolean sf) { ncs(sf); }
    private void ncct(boolean sf) { ncs(sf); ncs(sf); ncs(sf); }
    private void ncds(boolean sf) { ncs(sf); }
    private void ncdt(boolean sf) { ncs(sf); ncs(sf); ncs(sf); }
    private void cdp(boolean sf, boolean lm) { dpcs(sf, lm); }
    private void cc(boolean sf, boolean lm) { dpcs(sf, lm); }
}
