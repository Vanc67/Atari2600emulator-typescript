package rpc;

public class Score {
    final public static int SLOT_CPU = 0;
    final public static int SLOT_MB = 1;
    final public static int SLOT_RAM0 = 2;
    final public static int RAM_N = 4;
    final public static int SLOT_GPU = 6;
    final public static int SLOT_D0 = 7;
    final public static int DISK_N = 3;
    final public static int SLOT_PSU = 10;
    final public static int SLOT_CASE = 11;
    final public static int SLOT_COOL = 12;
    final public static int SLOT_MON = 13;
    final public static int SLOT_SND = 14;
    final public static int SLOT_KB = 15;
    final public static int SLOT_MOUSE = 16;
    final public static int SLOT_NET = 17;
    final public static int SLOT_MISC = 18;
    final public static int SLOT_PRN = 19;
    final public static int SLOTS = 20;
    final public static String[] SLOT_NAMES;
    final public static String[] SLOT_SHORT;
    final private static String[][] CASE_OK;
    final public static String[] SOCK_LIST;
    final private static String[] BUS_LIST;
    final private static String[] IFC_LIST;
    final private static String[] FORM_LIST;
    final private static String[] RAM_LIST;
    final private static String[] MON_LIST;
    
    static {
        String[] a = new String[20];
        a[0] = "\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440";
        a[1] = "\u041c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u0430\u044f \u043f\u043b\u0430\u0442\u0430";
        a[2] = "\u041e\u0417\u0423 1";
        a[3] = "\u041e\u0417\u0423 2";
        a[4] = "\u041e\u0417\u0423 3";
        a[5] = "\u041e\u0417\u0423 4";
        a[6] = "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430";
        a[7] = "\u0414\u0438\u0441\u043a 1";
        a[8] = "\u0414\u0438\u0441\u043a 2";
        a[9] = "\u0414\u0438\u0441\u043a 3";
        a[10] = "\u0411\u043b\u043e\u043a \u043f\u0438\u0442\u0430\u043d\u0438\u044f";
        a[11] = "\u041a\u043e\u0440\u043f\u0443\u0441";
        a[12] = "\u041e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u0435";
        a[13] = "\u041c\u043e\u043d\u0438\u0442\u043e\u0440";
        a[14] = "\u0417\u0432\u0443\u043a";
        a[15] = "\u041a\u043b\u0430\u0432\u0438\u0430\u0442\u0443\u0440\u0430";
        a[16] = "\u041c\u044b\u0448\u044c";
        a[17] = "\u0421\u0435\u0442\u044c";
        a[18] = "\u041f\u043b\u0430\u0442\u0430 \u0440\u0430\u0441\u0448.";
        a[19] = "\u041f\u0435\u0440\u0438\u0444\u0435\u0440\u0438\u044f";
        SLOT_NAMES = a;
        String[] a0 = new String[20];
        a0[0] = "CPU";
        a0[1] = "\u041f\u043b\u0430\u0442\u0430";
        a0[2] = "\u041e\u0417\u04231";
        a0[3] = "\u041e\u0417\u04232";
        a0[4] = "\u041e\u0417\u04233";
        a0[5] = "\u041e\u0417\u04234";
        a0[6] = "GPU";
        a0[7] = "\u0414\u0438\u0441\u043a1";
        a0[8] = "\u0414\u0438\u0441\u043a2";
        a0[9] = "\u0414\u0438\u0441\u043a3";
        a0[10] = "\u0411\u041f";
        a0[11] = "\u041a\u043e\u0440\u043f\u0443\u0441";
        a0[12] = "\u041a\u0443\u043b\u0435\u0440";
        a0[13] = "\u041c\u043e\u043d\u0438\u0442\u043e\u0440";
        a0[14] = "\u0417\u0432\u0443\u043a";
        a0[15] = "\u041a\u043b\u0430\u0432\u0430";
        a0[16] = "\u041c\u044b\u0448\u044c";
        a0[17] = "\u0421\u0435\u0442\u044c";
        a0[18] = "\u041f\u043b\u0430\u0442\u0430 \u0440.";
        a0[19] = "\u041f\u0440\u0438\u043d\u0442\u0435\u0440";
        SLOT_SHORT = a0;
        String[][] a1 = new String[8][];
        String[] a2 = new String[2];
        a2[0] = "XT";
        a2[1] = "AT";
        a1[0] = a2;
        String[] a3 = new String[3];
        a3[0] = "AT";
        a3[1] = "BABY-AT";
        a3[2] = "ATX";
        a1[1] = a3;
        String[] a4 = new String[2];
        a4[0] = "BABY-AT";
        a4[1] = "ATX";
        a1[2] = a4;
        String[] a5 = new String[2];
        a5[0] = "ATX";
        a5[1] = "E-ATX";
        a1[3] = a5;
        String[] a6 = new String[3];
        a6[0] = "MICRO-ATX";
        a6[1] = "ATX";
        a6[2] = "E-ATX";
        a1[4] = a6;
        String[] a7 = new String[4];
        a7[0] = "MINI-ITX";
        a7[1] = "MICRO-ATX";
        a7[2] = "ATX";
        a7[3] = "E-ATX";
        a1[5] = a7;
        String[] a8 = new String[1];
        a8[0] = "E-ATX";
        a1[6] = a8;
        String[] a9 = new String[2];
        a9[0] = "BTX";
        a9[1] = "ATX";
        a1[7] = a9;
        CASE_OK = a1;
        String[] a10 = new String[45];
        a10[0] = "DIP40";
        a10[1] = "PLCC68";
        a10[2] = "PGA386";
        a10[3] = "486-1";
        a10[4] = "486-2";
        a10[5] = "486-3";
        a10[6] = "S4";
        a10[7] = "S5";
        a10[8] = "S3";
        a10[9] = "S7";
        a10[10] = "SS7";
        a10[11] = "SLOT1";
        a10[12] = "SLOT2";
        a10[13] = "SLOTA";
        a10[14] = "S370";
        a10[15] = "S423";
        a10[16] = "S478";
        a10[17] = "S775";
        a10[18] = "S1156";
        a10[19] = "S1155";
        a10[20] = "S1150";
        a10[21] = "S1151";
        a10[22] = "S1200";
        a10[23] = "S1700";
        a10[24] = "S754";
        a10[25] = "S939";
        a10[26] = "S940";
        a10[27] = "AM2";
        a10[28] = "AM2+";
        a10[29] = "AM3";
        a10[30] = "AM3+";
        a10[31] = "FM1";
        a10[32] = "FM2";
        a10[33] = "FM2+";
        a10[34] = "AM1";
        a10[35] = "AM4";
        a10[36] = "AM5";
        a10[37] = "S462";
        a10[38] = "S1366";
        a10[39] = "S2011";
        a10[40] = "S2066";
        a10[41] = "TR4";
        a10[42] = "SP5";
        a10[43] = "S8";
        a10[44] = "S1851";
        SOCK_LIST = a10;
        String[] a11 = new String[22];
        a11[0] = "ISA8";
        a11[1] = "ISA16";
        a11[2] = "VLB";
        a11[3] = "PCI";
        a11[4] = "AGP";
        a11[5] = "AGP2X";
        a11[6] = "AGP4X";
        a11[7] = "AGP8X";
        a11[8] = "PCIe";
        a11[9] = "PCIe2";
        a11[10] = "PCIe3";
        a11[11] = "PCIe4";
        a11[12] = "PCIe5";
        a11[13] = "USB";
        a11[14] = "MIDI";
        a11[15] = "LPT";
        a11[16] = "MINIPCI";
        a11[17] = "MINIPCIe";
        a11[18] = "M.2";
        a11[19] = "LOM";
        a11[20] = "RS232";
        a11[21] = "Ethernet";
        BUS_LIST = a11;
        String[] a12 = new String[11];
        a12[0] = "MFM";
        a12[1] = "RLL";
        a12[2] = "IDE";
        a12[3] = "SCSI";
        a12[4] = "SATA";
        a12[5] = "NVME";
        a12[6] = "FLOPPY";
        a12[7] = "ZIP";
        a12[8] = "CDROM";
        a12[9] = "DVD";
        a12[10] = "BD";
        IFC_LIST = a12;
        String[] a13 = new String[8];
        a13[0] = "XT";
        a13[1] = "AT";
        a13[2] = "BABY-AT";
        a13[3] = "ATX";
        a13[4] = "MICRO-ATX";
        a13[5] = "MINI-ITX";
        a13[6] = "E-ATX";
        a13[7] = "BTX";
        FORM_LIST = a13;
        String[] a14 = new String[12];
        a14[0] = "DIP";
        a14[1] = "SIPP";
        a14[2] = "SIMM30";
        a14[3] = "SIMM72";
        a14[4] = "DIMM";
        a14[5] = "RDRAM";
        a14[6] = "SDR";
        a14[7] = "DDR";
        a14[8] = "DDR2";
        a14[9] = "DDR3";
        a14[10] = "DDR4";
        a14[11] = "DDR5";
        RAM_LIST = a14;
        String[] a15 = new String[11];
        a15[0] = "MDA";
        a15[1] = "CGA";
        a15[2] = "EGA";
        a15[3] = "VGA-CRT";
        a15[4] = "SVGA-CRT";
        a15[5] = "LCD-TN";
        a15[6] = "LCD-IPS";
        a15[7] = "LCD-VA";
        a15[8] = "LCD-PLS";
        a15[9] = "OLED";
        a15[10] = "PLASMA";
        MON_LIST = a15;
    }
    
    public Score() {
    }
    
    public static boolean busFits(rpc.Part a, rpc.Part a0) {
        return rpc.Score.busFits0(a, a0);
    }
    
    public static boolean caseFits(int i, int i0) {
        if (i >= 0 && i < CASE_OK.length) {
            String s = rpc.Score.formName(i0);
            String[] a = CASE_OK[i];
            int i1 = 0;
            while(i1 < a.length) {
                if (a[i1].equals((Object)s)) {
                    return true;
                }
                i1 = i1 + 1;
            }
            return false;
        }
        return true;
    }
    
    private static boolean busFits0(rpc.Part a, rpc.Part a0) {
        int i = a0.f[5];
        String s = rpc.Score.dictSafe(a0, a.f[0]);
        int i0 = rpc.Score.busBit(s);
        if (i0 < 0) {
            return false;
        }
        if ((i & i0) != 0) {
            return true;
        }
        if (s.startsWith("PCIe") && (i & 256) != 0) {
            return true;
        }
        if (s.startsWith("AGP") && (i & 16) != 0) {
            return true;
        }
        return false;
    }
    
    public static String busName(int i) {
        if (i >= 0 && i < BUS_LIST.length) {
            return BUS_LIST[i];
        }
        return "?";
    }
    
    public static String sockName(int i) {
        if (i >= 0 && i < SOCK_LIST.length) {
            return SOCK_LIST[i];
        }
        return "?";
    }
    
    private static int busBit(String s) {
        int i = 0;
        while(i < BUS_LIST.length) {
            if (BUS_LIST[i].equals((Object)s)) {
                return 1 << i;
            }
            i = i + 1;
        }
        return -1;
    }
    
    private static String dictSafe(rpc.Part a, int i) {
        if (i >= 0 && i < BUS_LIST.length) {
            return BUS_LIST[i];
        }
        return "?";
    }
    
    public static boolean ifaceSupported(int i, String s) {
        if (!s.equals((Object)"MFM") && !s.equals((Object)"RLL")) {
            if (s.equals((Object)"IDE")) {
                if (i >= 1989 && i <= 2012) {
                    return true;
                }
                return false;
            } else if (s.equals((Object)"SATA")) {
                if (i < 2003) {
                    return false;
                }
                return true;
            } else if (s.equals((Object)"NVME")) {
                if (i < 2013) {
                    return false;
                }
                return true;
            } else {
                if (!s.equals((Object)"SCSI")) {
                    return true;
                }
                if (i > 2010) {
                    return false;
                }
                return true;
            }
        }
        if (i > 1996) {
            return false;
        }
        return true;
    }
    
    public static String ifaceName(int i) {
        if (i >= 0 && i < IFC_LIST.length) {
            return IFC_LIST[i];
        }
        return "?";
    }
    
    public static String ramTypeName(int i) {
        if (i >= 0 && i < RAM_LIST.length) {
            return RAM_LIST[i];
        }
        return "?";
    }
    
    public static String formName(int i) {
        if (i >= 0 && i < FORM_LIST.length) {
            return FORM_LIST[i];
        }
        return "?";
    }
    
    public static boolean formFits(String s, String s0) {
        int i = 0;
        while(i < FORM_LIST.length) {
            if (FORM_LIST[i].equals((Object)s0)) {
                int i0 = 0;
                while(i0 < CASE_OK[i].length) {
                    if (CASE_OK[i][i0].equals((Object)s)) {
                        return true;
                    }
                    i0 = i0 + 1;
                }
            }
            i = i + 1;
        }
        return false;
    }
    
    public static int needPower(rpc.Part a, rpc.Part a0) {
        int i = (a == null) ? 50 : 50 + a.f[3];
        if (a0 != null) {
            i = i + a0.f[4];
        }
        return i;
    }
    
    public static int totalRamKb(rpc.Part[] a) {
        int i = 0;
        int i0 = 0;
        while(i0 < 4) {
            rpc.Part a0 = a[2 + i0];
            if (a0 != null) {
                i = i + a0.f[1];
            }
            i0 = i0 + 1;
        }
        return i;
    }
    
    public static int ramCount(rpc.Part[] a) {
        int i = 0;
        int i0 = 0;
        while(i0 < 4) {
            if (a[2 + i0] != null) {
                i = i + 1;
            }
            i0 = i0 + 1;
        }
        return i;
    }
    
    public static rpc.Part firstDisk(rpc.Part[] a) {
        int i = 0;
        while(i < 3) {
            if (a[7 + i] != null) {
                return a[7 + i];
            }
            i = i + 1;
        }
        rpc.Part a0 = null;
        return a0;
    }
    
    public static String[] validate(rpc.Part[] a) {
        int i = 0;
        int i0 = 0;
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        int i8 = 0;
        String[] a0 = new String[10];
        rpc.Part a1 = a[0];
        rpc.Part a2 = a[1];
        rpc.Part a3 = a[6];
        rpc.Part a4 = a[10];
        rpc.Part a5 = a[11];
        rpc.Part a6 = a[12];
        rpc.Part a7 = a[13];
        if (a1 != null) {
            i = 0;
        } else {
            a0[0] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440";
            i = 1;
        }
        if (a2 != null) {
            i0 = i;
        } else {
            i0 = i + 1;
            a0[i] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d\u0430 \u043c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u0430\u044f \u043f\u043b\u0430\u0442\u0430";
        }
        if (a3 != null) {
            i1 = i0;
        } else {
            i1 = i0 + 1;
            a0[i0] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d\u0430 \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430";
        }
        if (a4 != null) {
            i2 = i1;
        } else {
            i2 = i1 + 1;
            a0[i1] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d \u0431\u043b\u043e\u043a \u043f\u0438\u0442\u0430\u043d\u0438\u044f";
        }
        if (a5 != null) {
            i3 = i2;
        } else {
            i3 = i2 + 1;
            a0[i2] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d \u043a\u043e\u0440\u043f\u0443\u0441";
        }
        if (a6 != null) {
            i4 = i3;
        } else {
            i4 = i3 + 1;
            a0[i3] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d\u043e \u043e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u0435";
        }
        if (a7 != null) {
            i5 = i4;
        } else {
            i5 = i4 + 1;
            a0[i4] = "\u041d\u0435 \u0432\u044b\u0431\u0440\u0430\u043d \u043c\u043e\u043d\u0438\u0442\u043e\u0440";
        }
        if (rpc.Score.ramCount(a) != 0) {
            i6 = i5;
        } else {
            i6 = i5 + 1;
            a0[i5] = "\u041d\u0435\u0442 \u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u043f\u0430\u043c\u044f\u0442\u0438";
        }
        if (rpc.Score.firstDisk(a) != null) {
            i7 = i6;
        } else {
            i7 = i6 + 1;
            a0[i6] = "\u041d\u0435\u0442 \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044f";
        }
        if (i7 > 0) {
            return rpc.Score.shrink(a0, i7);
        }
        if (a1.f[0] != 0 == (a2.f[0] != 0)) {
            i8 = i7;
        } else {
            i8 = i7 + 1;
            a0[i7] = new StringBuffer("\u0421\u043e\u043a\u0435\u0442 \u043d\u0435 \u043f\u043e\u0434\u0445\u043e\u0434\u0438\u0442: CPU ").append(rpc.Score.sockName(a1.f[0])).append(", \u043f\u043b\u0430\u0442\u0430 ").append(rpc.Score.sockName(a2.f[0])).toString();
        }
        boolean b = a2.f[1] != 0;
        int i9 = 0;
        int i10 = 0;
        int i11 = 0;
        while(true) {
            int i12 = i8;
            if (i11 < 4) {
                rpc.Part a8 = a[2 + i11];
                if (a8 == null) {
                    i8 = i12;
                } else {
                    i10 = i10 + 1;
                    i9 = i9 + a8.f[1];
                    boolean b0 = a8.f[0] != 0;
                    i8 = i12;
                    if (b0 != b) {
                        i8 = i12 + 1;
                        a0[i12] = new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c ").append(rpc.Score.ramTypeName(a8.f[0])).append(" \u043d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442 \u0432 \u0441\u043b\u043e\u0442 ").append(rpc.Score.ramTypeName(b ? 1 : 0)).toString();
                    }
                }
                i11 = i11 + 1;
            } else {
                int i13 = 0;
                int i14 = 0;
                int i15 = 0;
                int i16 = 0;
                int i17 = 0;
                int i18 = 0;
                if ((i10) <= (a2.f[2])) {
                    i13 = i8;
                } else {
                    i13 = i8 + 1;
                    a0[i8] = new StringBuffer("\u0421\u043b\u043e\u0442\u043e\u0432 \u043f\u0430\u043c\u044f\u0442\u0438 \u0432\u0441\u0435\u0433\u043e ").append(a2.f[2]).append(", \u0430 \u043c\u043e\u0434\u0443\u043b\u0435\u0439 ").append(i10).toString();
                }
                if ((long)i9 <= (long)(a2.f[3]) * 1024L) {
                    i14 = i13;
                } else {
                    i14 = i13 + 1;
                    a0[i13] = new StringBuffer("\u041f\u043b\u0430\u0442\u0430 \u043d\u0435 \u0432\u0438\u0434\u0438\u0442 \u0441\u0442\u043e\u043b\u044c\u043a\u043e \u041e\u0417\u0423 (\u043c\u0430\u043a\u0441 ").append(rpc.Util.size(a2.f[3] * 1024)).append(")").toString();
                }
                if (rpc.Score.busFits0(a3, a2)) {
                    i15 = i14;
                } else {
                    i15 = i14 + 1;
                    a0[i14] = "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430 \u043d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442: \u0448\u0438\u043d\u0430 \u043d\u0435 \u043f\u043e\u0434\u0434\u0435\u0440\u0436\u0438\u0432\u0430\u0435\u0442\u0441\u044f \u043f\u043b\u0430\u0442\u043e\u0439";
                }
                int b1 = a4.f[0];
                int i19 = rpc.Score.needPower(a1, a3);
                if (b1 >= i19) {
                    i16 = i15;
                } else {
                    i16 = i15 + 1;
                    a0[i15] = new StringBuffer("\u0411\u041f \u0441\u043b\u0430\u0431\u043e\u0432\u0430\u0442: \u043d\u0443\u0436\u043d\u043e ").append(i19).append(" \u0412\u0442, \u0435\u0441\u0442\u044c ").append(a4.f[0]).append(" \u0412\u0442").toString();
                }
                label4: if ((a6.f[1]) >= (a1.f[3])) {
                    i17 = i16;
                } else {
                    boolean b2 = a6.f[1] != 0;
                    label5: {
                        if (b2) {
                            break label5;
                        }
                        if ((a1.f[3]) <= 10) {
                            i17 = i16;
                            break label4;
                        }
                    }
                    i17 = i16 + 1;
                    a0[i16] = new StringBuffer("\u041a\u0443\u043b\u0435\u0440 \u043d\u0435 \u0442\u044f\u043d\u0435\u0442 CPU (").append(a6.f[1]).append(" \u0412\u0442 < ").append(a1.f[3]).append(" \u0412\u0442)").toString();
                }
                if (rpc.Score.formFits(rpc.Score.formName(a5.f[0]), rpc.Score.formName(a2.f[4]))) {
                    i18 = i17;
                } else {
                    i18 = i17 + 1;
                    a0[i17] = new StringBuffer("\u041f\u043b\u0430\u0442\u0430 ").append(rpc.Score.formName(a2.f[4])).append(" \u043d\u0435 \u043b\u0435\u0437\u0435\u0442 \u0432 \u043a\u043e\u0440\u043f\u0443\u0441 ").append(rpc.Score.formName(a5.f[0])).toString();
                }
                int i20 = 0;
                while(true) {
                    if (i20 < 3) {
                        String s = null;
                        rpc.Part a9 = a[7 + i20];
                        label2: {
                            label3: {
                                if (a9 == null) {
                                    break label3;
                                }
                                int i21 = a2.year;
                                s = rpc.Score.ifaceName(a9.f[0]);
                                if (!rpc.Score.ifaceSupported(i21, s)) {
                                    break label2;
                                }
                            }
                            i20 = i20 + 1;
                            continue;
                        }
                        int i22 = i18 + 1;
                        a0[i18] = new StringBuffer("\u041f\u043b\u0430\u0442\u0430 ").append(a2.year).append(" \u0433. \u043d\u0435 \u0443\u043c\u0435\u0435\u0442 ").append(s).toString();
                        i18 = i22;
                    }
                    if (a7 != null) {
                        int i23 = 0;
                        int i24 = 0;
                        long j = (long)(a7.f[2]) * (long)(a7.f[3]) * (long)(a7.f[5]) / 8L / 1024L;
                        if ((long)(a3.f[1]) >= j) {
                            i23 = i18;
                        } else if ((a7.f[5]) <= 8) {
                            i23 = i18;
                        } else {
                            i23 = i18 + 1;
                            a0[i18] = new StringBuffer("\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0435 \u043d\u0435 \u0445\u0432\u0430\u0442\u0438\u0442 \u0432\u0438\u0434\u0435\u043e\u043f\u0430\u043c\u044f\u0442\u0438 \u043d\u0430 ").append(a7.f[2]).append("x").append(a7.f[3]).toString();
                        }
                        String s0 = rpc.Score.monTech(a7.f[0]);
                        boolean b3 = s0.equals((Object)"MDA");
                        label0: {
                            label1: {
                                if (b3) {
                                    break label1;
                                }
                                if (s0.equals((Object)"CGA")) {
                                    break label1;
                                }
                                if (!s0.equals((Object)"EGA")) {
                                    i24 = i23;
                                    break label0;
                                }
                            }
                            if ((a3.f[1]) <= 1024) {
                                i24 = i23;
                            } else {
                                i24 = i23 + 1;
                                a0[i23] = new StringBuffer("\u0421\u0442\u0430\u0440\u044b\u0439 ").append(s0).append("-\u043c\u043e\u043d\u0438\u0442\u043e\u0440 \u043d\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043a \u043d\u043e\u0432\u043e\u0439 \u043a\u0430\u0440\u0442\u0435").toString();
                            }
                        }
                        if (a3.year <= a7.year + 6) {
                            i18 = i24;
                        } else {
                            i18 = i24 + 1;
                            a0[i24] = "\u041a\u0430\u0440\u0442\u0430 \u0441\u043b\u0438\u0448\u043a\u043e\u043c \u043d\u043e\u0432\u0430\u044f \u0434\u043b\u044f \u044d\u0442\u043e\u0433\u043e \u043c\u043e\u043d\u0438\u0442\u043e\u0440\u0430 (\u043d\u0435\u0442 \u0440\u0430\u0437\u044a\u0451\u043c\u0430)";
                        }
                    }
                    return rpc.Score.shrink(a0, i18);
                }
            }
        }
    }
    
    public static String monTech(int i) {
        if (i >= 0 && i < MON_LIST.length) {
            return MON_LIST[i];
        }
        return "?";
    }
    
    private static String[] shrink(String[] a, int i) {
        String[] a0 = new String[i];
        System.arraycopy((Object)a, 0, (Object)a0, 0, i);
        return a0;
    }
    
    public static int condOf(rpc.Part a) {
        if (a == null) {
            return 100;
        }
        return (a.cond >= 5) ? (a.cond <= 100) ? a.cond : 100 : 5;
    }
    
    public static rpc.Score$Result evaluate(rpc.Db$Level a, rpc.Part[] a0) {
        rpc.Score$Result a1 = new rpc.Score$Result();
        a1.errors = rpc.Score.validate(a0);
        a1.ok = a1.errors.length == 0;
        rpc.Part a2 = a0[0];
        rpc.Part a3 = a0[6];
        if (a2 != null && a3 != null) {
            int i = 0;
            long j = 0L;
            int i0 = (int)((long)(a2.f[1] * a2.f[2] * a2.f[4] / 10) * (long)rpc.Score.condOf(a2) / 100L);
            int i1 = (int)((long)(a3.f[5]) * (long)rpc.Score.condOf(a3) / 100L);
            int i2 = 0;
            int i3 = 0;
            while(i3 < 4) {
                rpc.Part a4 = a0[2 + i3];
                if (a4 != null) {
                    i2 = i2 + (int)((long)rpc.Util.isqrt(a4.f[1]) * (long)(a4.f[2]) / 100L * (long)rpc.Score.condOf(a4) / 100L);
                }
                i3 = i3 + 1;
            }
            int i4 = 0;
            int i5 = 0;
            while(i5 < 3) {
                rpc.Part a5 = a0[7 + i5];
                if (a5 != null) {
                    i4 = i4 + (int)((long)((a5.cat != 7) ? rpc.Util.isqrt(a5.f[1]) : rpc.Util.isqrt(a5.f[1]) * 2) * (long)rpc.Score.condOf(a5) / 100L);
                }
                i5 = i5 + 1;
            }
            a1.barsC = rpc.Score.percent(i0, a.refC);
            a1.barsG = rpc.Score.percent(i1, a.refG);
            a1.barsR = rpc.Score.percent(i2, a.refR);
            a1.barsD = rpc.Score.percent(i4, a.refD);
            if (a.refC <= 0) {
                i = 0;
                j = 0L;
            } else {
                j = 0L + 35L * (long)i0 * 100L / (long)a.refC;
                i = 35;
            }
            if (a.refG > 0) {
                j = j + 40L * (long)i1 * 100L / (long)a.refG;
                i = i + 40;
            }
            if (a.refR > 0) {
                j = j + 15L * (long)i2 * 100L / (long)a.refR;
                i = i + 15;
            }
            if (a.refD > 0) {
                j = j + 10L * (long)i4 * 100L / (long)a.refD;
                i = i + 10;
            }
            long j0 = (i <= 0) ? 0L : j / (long)i;
            rpc.Part a6 = a0[12];
            rpc.Part a7 = a0[10];
            rpc.Part a8 = a0[13];
            int i6 = (a6 == null) ? 0 : (a2 == null) ? 0 : (a6.f[1] < a2.f[3] + 30) ? (a6.f[1] < a2.f[3]) ? 0 : 1 : 3;
            if (a7 != null && a2 != null && a3 != null && a7.f[0] >= rpc.Score.needPower(a2, a3) + 100) {
                i6 = i6 + 2;
            }
            boolean b = false;
            int i7 = 0;
            while(i7 < 3) {
                if (a0[7 + i7] != null && a0[7 + i7].cat == 7) {
                    b = true;
                }
                i7 = i7 + 1;
            }
            if (b) {
                i6 = i6 + 2;
            }
            rpc.Part a9 = a0[14];
            if (a9 != null) {
                i6 = i6 + Math.min(4, a9.f[3] / 25);
            }
            rpc.Part a10 = a0[15];
            rpc.Part a11 = a0[16];
            if (a10 != null && a11 != null) {
                i6 = i6 + Math.min(4, (a10.f[0] + a11.f[0]) / 50);
            }
            if (a8 != null && a8.year >= a.year - 1) {
                i6 = i6 + 2;
            }
            if (i6 > 15) {
                i6 = 15;
            }
            a1.bonus = i6;
            a1.score = (int)j0 + i6;
            int i8 = rpc.Score.needPower(a2, a3);
            a1.power = i8;
            a1.needPower = i8;
            return a1;
        }
        a1.ok = false;
        return a1;
    }
    
    private static int percent(int i, int i0) {
        if (i0 <= 0) {
            return 0;
        }
        int i1 = (int)((long)i * 100L / (long)i0);
        if (i1 > 200) {
            i1 = 200;
        }
        return i1;
    }
}
