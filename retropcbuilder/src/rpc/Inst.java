package rpc;

public class Inst {
    final public static int P_NONE = 0;
    final public static int P_WIN7MINI = 1;
    final public static int P_CPUCHK = 2;
    final public static int P_TPM = 4;
    final private static rpc.Inst$Os[] CUST;
    private static int custN;
    
    static {
        CUST = new rpc.Inst$Os[16];
        custN = 0;
    }
    
    public Inst() {
    }
    
    private static rpc.Inst$Os os(String s, int i, int i0, int i1, int i2, int i3) {
        rpc.Inst$Os a = new rpc.Inst$Os();
        a.name = s;
        a.year = i;
        a.minMhz = i0;
        a.minRamKb = i1;
        a.minDiskKb = i2;
        a.baseMin = i3;
        a.patchMask = 0;
        a.patchMinRamKb = i1;
        a.patchMinMhz = i0;
        return a;
    }
    
    public static rpc.Inst$Os[] catalog() {
        rpc.Inst$Os[] a = new rpc.Inst$Os[27];
        a[0] = rpc.Inst.os("MS-DOS 1.0", 1981, 4, 64, 160, 6);
        a[1] = rpc.Inst.os("CP/M-86", 1982, 4, 128, 320, 8);
        a[2] = rpc.Inst.os("MS-DOS 3.3", 1987, 8, 256, 1024, 10);
        a[3] = rpc.Inst.os("Windows 3.0", 1990, 12, 1024, 6144, 20);
        a[4] = rpc.Inst.os("MS-DOS 5.0", 1991, 8, 640, 2048, 12);
        a[5] = rpc.Inst.os("Windows 3.1", 1992, 16, 2048, 10240, 25);
        a[6] = rpc.Inst.os("Windows 3.11", 1993, 16, 2048, 10240, 25);
        a[7] = rpc.Inst.os("RPC-Linux 0.9", 1994, 33, 4096, 102400, 35);
        a[8] = rpc.Inst.os("OS/2 Warp 3", 1994, 33, 8192, 102400, 40);
        a[9] = rpc.Inst.os("Windows 95", 1995, 33, 4096, 51200, 45);
        a[10] = rpc.Inst.os("Windows NT 4.0", 1996, 66, 16384, 1179648, 60);
        a[11] = rpc.Inst.os("BeOS 4", 1998, 133, 32768, 512000, 40);
        a[12] = rpc.Inst.os("Windows 98 SE", 1999, 66, 16384, 204800, 55);
        a[13] = rpc.Inst.os("Mandrake Linux 7", 2000, 133, 32768, 512000, 55);
        a[14] = rpc.Inst.os("Windows 2000", 2000, 166, 65536, 1048576, 80);
        a[15] = rpc.Inst.os("Windows Me", 2000, 150, 65536, 512000, 60);
        a[16] = rpc.Inst.os("Windows XP", 2001, 233, 65536, 1572864, 70);
        a[17] = rpc.Inst.os("Kolibri-DOS 0.7", 2004, 100, 8192, 30720, 20);
        a[18] = rpc.Inst.os("Ubuntu 4.10", 2004, 300, 131072, 2097152, 75);
        a[19] = rpc.Inst.os("Windows Vista", 2007, 800, 524288, 15728640, 90);
        a[20] = rpc.Inst.os("Windows 7", 2009, 1000, 1048576, 16777216, 65);
        a[21] = rpc.Inst.os("Ubuntu 10.04", 2010, 700, 393216, 4718592, 55);
        a[22] = rpc.Inst.os("Windows 8", 2012, 1000, 1048576, 20971520, 60);
        a[23] = rpc.Inst.os("Windows 8.1", 2013, 1000, 2097152, 20971520, 55);
        a[24] = rpc.Inst.os("Windows 10", 2015, 1000, 4194304, 33554432, 55);
        a[25] = rpc.Inst.os("ReactOS 0.4", 2016, 300, 262144, 524288, 40);
        a[26] = rpc.Inst.os("Windows 11", 2021, 1000, 4194304, 67108864, 60);
        a[20].patchMask = 1;
        a[20].patchMinRamKb = 131072;
        a[20].patchMinMhz = 25;
        a[22].patchMask = 3;
        a[22].patchMinRamKb = 524288;
        a[22].patchMinMhz = 200;
        a[23].patchMask = 3;
        a[23].patchMinRamKb = 524288;
        a[23].patchMinMhz = 200;
        a[24].patchMask = 3;
        a[24].patchMinRamKb = 524288;
        a[24].patchMinMhz = 200;
        a[26].patchMask = 6;
        a[26].patchMinRamKb = 1048576;
        a[26].patchMinMhz = 400;
        a[26].needTpm = true;
        a[26].needSecureBoot = true;
        a[26].needUefi = true;
        return a;
    }
    
    public static void addCustom(rpc.Inst$Os a) {
        if (a != null && custN < CUST.length) {
            rpc.Inst$Os[] a0 = CUST;
            int i = custN;
            custN = i + 1;
            a0[i] = a;
            return;
        }
    }
    
    public static int customCount() {
        return custN;
    }
    
    public static rpc.Inst$Os[] all() {
        rpc.Inst$Os[] a = rpc.Inst.catalog();
        if (custN == 0) {
            return a;
        }
        rpc.Inst$Os[] a0 = new rpc.Inst$Os[a.length + custN];
        System.arraycopy((Object)a, 0, (Object)a0, 0, a.length);
        System.arraycopy((Object)CUST, 0, (Object)a0, a.length, custN);
        return a0;
    }
    
    public static int indexOf(String s) {
        rpc.Inst$Os[] a = rpc.Inst.all();
        int i = 0;
        while(i < a.length) {
            if (a[i].name.equals((Object)s)) {
                return i;
            }
            i = i + 1;
        }
        return -1;
    }
    
    public static int blocker(rpc.Inst$Os a, rpc.Part a0, rpc.Part a1, int i, long j, boolean b, boolean b0, boolean b1, int i0) {
        int i1 = 0;
        int i2 = 0;
        boolean b2 = (i0 & 1) != 0;
        boolean b3 = (i0 & 2) != 0;
        boolean b4 = (i0 & 4) != 0;
        label5: {
            label3: {
                label4: {
                    if (!b2) {
                        break label4;
                    }
                    if ((a.patchMask & 1) != 0) {
                        break label3;
                    }
                }
                i1 = a.minMhz;
                break label5;
            }
            i1 = a.patchMinMhz;
        }
        label2: {
            label0: {
                label1: {
                    if (!b2) {
                        break label1;
                    }
                    if ((a.patchMask & 1) != 0) {
                        break label0;
                    }
                }
                i2 = a.minRamKb;
                break label2;
            }
            i2 = a.patchMinRamKb;
        }
        if (b3 && (a.patchMask & 2) != 0) {
            if (i1 > a.patchMinMhz) {
                i1 = a.patchMinMhz;
            }
            if (a.patchMinRamKb < i2) {
                i2 = a.patchMinRamKb;
            }
        }
        if (a0 == null) {
            return 1;
        }
        if ((long)a0.f[1] * (long)Math.max(1, a0.f[2]) < (long)i1) {
            return 1;
        }
        if (i < i2) {
            return 2;
        }
        if (j < (long)a.minDiskKb) {
            return 3;
        }
        if (a.needTpm && !b4 && !b) {
            return 4;
        }
        if (a.needSecureBoot && !b4 && !b0) {
            return 5;
        }
        if (a.needUefi && !b4 && !b1) {
            return 6;
        }
        return 0;
    }
    
    public static String blockerText(int i) {
        if (i == 0) {
            return "\u0442\u0440\u0435\u0431\u043e\u0432\u0430\u043d\u0438\u044f \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d\u044b";
        }
        if (i == 1) {
            return "\u0441\u043b\u0430\u0431\u044b\u0439 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440";
        }
        if (i == 2) {
            return "\u043c\u0430\u043b\u043e \u043e\u043f\u0435\u0440\u0430\u0442\u0438\u0432\u043d\u043e\u0439 \u043f\u0430\u043c\u044f\u0442\u0438";
        }
        if (i == 3) {
            return "\u043c\u0430\u043b\u043e \u043c\u0435\u0441\u0442\u0430 \u043d\u0430 \u0434\u0438\u0441\u043a\u0435";
        }
        if (i == 4) {
            return "\u043d\u0435\u0442 TPM 2.0 (\u0432\u043a\u043b\u044e\u0447\u0438\u0442\u0435 \u0432 BIOS)";
        }
        if (i == 5) {
            return "\u043d\u0443\u0436\u0435\u043d Secure Boot";
        }
        if (i != 6) {
            return "?";
        }
        return "\u043d\u0443\u0436\u0435\u043d \u0440\u0435\u0436\u0438\u043c UEFI";
    }
    
    public static boolean patchHelps(rpc.Inst$Os a, int i) {
        if (i != 1 && i != 2) {
            if (i != 4 && i != 5 && i != 6) {
                return false;
            }
            if ((a.patchMask & 4) == 0) {
                return false;
            }
            return true;
        }
        if ((a.patchMask & 3) == 0) {
            return false;
        }
        return true;
    }
    
    public static int minutes(rpc.Inst$Os a, rpc.Part a0, rpc.Part a1) {
        long j = 0L;
        int i = 0;
        if (a0 != null) {
            j = (long)a0.f[1] * (long)Math.max(1, a0.f[2]);
        } else {
            j = 100L;
        }
        if (j < 800L) {
            j = 800L;
        }
        int i0 = (int)(300000L / j);
        if (i0 < 40) {
            i0 = 40;
        }
        if (i0 > 1200) {
            i0 = 1200;
        }
        if (a1 == null) {
            i = 200;
        } else if (a1.cat != 7) {
            if (a1.cat != 6) {
                if (a1.cat != 8) {
                    int i1 = a1.f[2];
                    i = (i1 < 15000) ? (i1 < 10000) ? (i1 < 7200) ? (i1 <= 0) ? 400 : 250 : 160 : 120 : 100;
                } else {
                    i = 600;
                }
            } else {
                i = 800;
            }
        } else {
            i = 45;
        }
        int i2 = a.baseMin * i0 * i / 10000;
        if (i2 < 2) {
            i2 = 2;
        }
        return i2;
    }
    
    public static String timeText(int i) {
        if (i < 60) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(i))).append(" \u043c\u0438\u043d").toString();
        }
        return new StringBuffer(String.valueOf((Object)String.valueOf(i / 60))).append(" \u0447 ").append(i % 60).append(" \u043c\u0438\u043d").toString();
    }
    
    private static rpc.Inst$Dl dl(String s, int i, int i0, int i1, int i2, String s0) {
        rpc.Inst$Dl a = new rpc.Inst$Dl();
        a.name = s;
        a.kb = i;
        a.year = i0;
        a.patch = i1;
        a.kind = i2;
        a.note = s0;
        return a;
    }
    
    public static rpc.Inst$Dl[] downloads(int i) {
        int i0 = 0;
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        rpc.Inst$Dl[] a = new rpc.Inst$Dl[10];
        if (i < 1993) {
            i0 = 0;
        } else {
            a[0] = rpc.Inst.dl("DOOM (\u0434\u0435\u043c\u043e)", 2400, 1993, 0, 0, "\u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0435\u0442\u0441\u044f \u043d\u0430 \u0436\u0435\u043b\u0435\u0437\u0435 \u043f\u043e \u0441\u043f\u0435\u0446\u0438\u0444\u0438\u043a\u0430\u0446\u0438\u0438");
            i0 = 1;
        }
        if (i < 1991) {
            i1 = i0;
        } else {
            i1 = i0 + 1;
            a[i0] = rpc.Inst.dl("\u0414\u0440\u0430\u0439\u0432\u0435\u0440\u044b \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442", 900, 1991, 0, 2, "\u0447\u0443\u0442\u044c \u0431\u043e\u043b\u044c\u0448\u0435 FPS \u0432 \u0438\u0433\u0440\u0430\u0445");
        }
        if (i < 2009) {
            i2 = i1;
        } else {
            i2 = i1 + 1;
            a[i1] = rpc.Inst.dl("\u041f\u0430\u0442\u0447 Win7-mini", 3200, 2009, 1, 1, "\u0441\u0435\u043c\u0451\u0440\u043a\u0430 \u043d\u0430 128 \u041c\u0411 \u041e\u0417\u0423");
        }
        if (i < 2015) {
            i3 = i2;
        } else {
            i3 = i2 + 1;
            a[i2] = rpc.Inst.dl("\u041f\u0430\u0442\u0447 \u043e\u0431\u0445\u043e\u0434\u0430 CPU", 1500, 2015, 2, 1, "10/11 \u043d\u0430 \u0441\u0442\u0430\u0440\u043e\u043c \u043a\u0430\u043c\u043d\u0435");
        }
        if (i < 2021) {
            i4 = i3;
        } else {
            i4 = i3 + 1;
            a[i3] = rpc.Inst.dl("\u041e\u0431\u0445\u043e\u0434 TPM 2.0", 2100, 2021, 4, 1, "Windows 11 \u0431\u0435\u0437 TPM");
        }
        if (i < 1996) {
            i5 = i4;
        } else {
            i5 = i4 + 1;
            a[i4] = rpc.Inst.dl("\u0410\u043d\u0442\u0438\u0432\u0438\u0440\u0443\u0441", 1800, 1996, 0, 0, "\u0447\u0438\u0441\u0442\u0438\u0442 \u0438 \u043d\u0430\u0445\u043e\u0434\u0438\u0442 \u00ab\u0437\u0430\u0440\u0430\u0436\u0451\u043d\u043d\u044b\u0435\u00bb \u0444\u0430\u0439\u043b\u044b");
        }
        if (i < 2000) {
            i6 = i5;
        } else {
            i6 = i5 + 1;
            a[i5] = rpc.Inst.dl("\u0410\u0440\u0445\u0438\u0432 \u0434\u0440\u0430\u0439\u0432\u0435\u0440\u043e\u0432", 5400, 2000, 0, 2, "\u0440\u0435\u0434\u043a\u0438\u0435 \u0437\u0432\u0443\u043a\u043e\u0432\u044b\u0435 \u0438 \u0441\u0435\u0442\u0435\u0432\u044b\u0435");
        }
        rpc.Inst$Dl[] a0 = new rpc.Inst$Dl[i6];
        System.arraycopy((Object)a, 0, (Object)a0, 0, i6);
        return a0;
    }
    
    public static int downloadMinutes(rpc.Inst$Dl a, int i) {
        if (i <= 0) {
            return -1;
        }
        int i0 = (int)((long)a.kb * 1024L * 8L / ((long)i * 1000L * 60L));
        if (i0 < 1) {
            i0 = 1;
        }
        return i0;
    }
    
    public static String[] sites(int i) {
        if (i < 1988) {
            String[] a = new String[3];
            a[0] = "BBS \u043f\u043e \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0443";
            a[1] = "Fidonet-\u044d\u0445\u043e";
            a[2] = "\u0434\u043e\u0441\u043a\u0430 \u043e\u0431\u044a\u044f\u0432\u043b\u0435\u043d\u0438\u0439";
            return a;
        }
        if (i < 1993) {
            String[] a0 = new String[4];
            a0[0] = "BBS";
            a0[1] = "Fidonet";
            a0[2] = "Gopher-\u0441\u0435\u0440\u0432\u0435\u0440\u044b";
            a0[3] = "\u043f\u0435\u0440\u0432\u0430\u044f \u043f\u043e\u0447\u0442\u0430";
            return a0;
        }
        if (i < 1996) {
            String[] a1 = new String[4];
            a1[0] = "Fidonet";
            a1[1] = "Gopher";
            a1[2] = "AltaVista-\u043f\u043e\u0438\u0441\u043a";
            a1[3] = "\u0434\u043e\u043c\u0430\u0448\u043d\u0438\u0435 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b";
            return a1;
        }
        if (i < 1999) {
            String[] a2 = new String[4];
            a2[0] = "\u043f\u043e\u0438\u0441\u043a\u043e\u0432\u0438\u043a\u0438";
            a2[1] = "GeoCities-\u0441\u0442\u0440\u0430\u043d\u0438\u0446\u044b";
            a2[2] = "\u0444\u043e\u0440\u0443\u043c\u044b \u0436\u0435\u043b\u0435\u0437\u044f\u0447\u043d\u0438\u043a\u043e\u0432";
            a2[3] = "FTP \u0441 \u0434\u0440\u0430\u0439\u0432\u0435\u0440\u0430\u043c\u0438";
            return a2;
        }
        if (i < 2003) {
            String[] a3 = new String[4];
            a3[0] = "\u043f\u043e\u0440\u0442\u0430\u043b\u044b";
            a3[1] = "\u0444\u043e\u0440\u0443\u043c\u044b";
            a3[2] = "FTP-\u0430\u0440\u0445\u0438\u0432\u044b";
            a3[3] = "\u043f\u0435\u0440\u0432\u044b\u0435 ICQ-\u0447\u0430\u0442\u044b";
            return a3;
        }
        if (i < 2007) {
            String[] a4 = new String[4];
            a4[0] = "\u043f\u043e\u0438\u0441\u043a";
            a4[1] = "\u0431\u043b\u043e\u0433\u0438";
            a4[2] = "\u0444\u0430\u0439\u043b\u043e\u043e\u0431\u043c\u0435\u043d\u043d\u0438\u043a\u0438";
            a4[3] = "\u0441\u043e\u0446\u0441\u0435\u0442\u043e\u0447\u043d\u044b\u0439 \u0441\u0442\u0430\u0440\u0442";
            return a4;
        }
        if (i < 2011) {
            String[] a5 = new String[4];
            a5[0] = "\u0432\u0438\u0434\u0435\u043e";
            a5[1] = "\u0441\u043e\u0446\u0441\u0435\u0442\u0438";
            a5[2] = "\u0444\u043e\u0440\u0443\u043c\u044b \u043e\u0432\u0435\u0440\u043a\u043b\u043e\u043a\u0435\u0440\u043e\u0432";
            a5[3] = "\u0442\u043e\u0440\u0440\u0435\u043d\u0442-\u0442\u0440\u0435\u043a\u0435\u0440\u044b";
            return a5;
        }
        if (i < 2015) {
            String[] a6 = new String[4];
            a6[0] = "\u0441\u043e\u0446\u0441\u0435\u0442\u0438";
            a6[1] = "\u0432\u0438\u0434\u0435\u043e-\u0445\u043e\u0441\u0442\u0438\u043d\u0433";
            a6[2] = "\u043e\u0431\u043b\u0430\u043a\u0430";
            a6[3] = "\u043c\u0430\u0440\u043a\u0435\u0442\u043f\u043b\u0435\u0439\u0441\u044b";
            return a6;
        }
        if (i >= 2020) {
            String[] a7 = new String[4];
            a7[0] = "\u043c\u0430\u0440\u043a\u0435\u0442\u043f\u043b\u0435\u0439\u0441\u044b";
            a7[1] = "\u043c\u0435\u0441\u0441\u0435\u043d\u0434\u0436\u0435\u0440\u044b";
            a7[2] = "\u0441\u0442\u0440\u0438\u043c\u0438\u043d\u0433";
            a7[3] = "\u043d\u0435\u0439\u0440\u043e-\u0441\u0435\u0440\u0432\u0438\u0441\u044b";
            return a7;
        }
        String[] a8 = new String[4];
        a8[0] = "\u043c\u0430\u0440\u043a\u0435\u0442\u043f\u043b\u0435\u0439\u0441\u044b";
        a8[1] = "\u043c\u0435\u0441\u0441\u0435\u043d\u0434\u0436\u0435\u0440\u044b";
        a8[2] = "\u0432\u0438\u0434\u0435\u043e";
        a8[3] = "\u0441\u0442\u0440\u0438\u043c\u0438\u043d\u0433";
        return a8;
    }
    
    public static int fps(rpc.Part a, rpc.Part a0) {
        if (a != null && a0 != null) {
            long j = (long)a.f[1] * (long)Math.max(1, a.f[2]) * (long)a.f[4] / 1000L / 2500L + (long)a0.f[5] / 8L;
            if (j < 1L) {
                j = 1L;
            }
            if (j > 240L) {
                j = 240L;
            }
            return (int)j;
        }
        return 0;
    }
    
    public static String fpsText(int i) {
        if (i >= 120) {
            return new StringBuffer("\u043f\u043b\u0430\u0432\u043d\u043e, ").append(i).append(" FPS").toString();
        }
        if (i >= 30) {
            return new StringBuffer("\u0438\u0433\u0440\u0430\u0431\u0435\u043b\u044c\u043d\u043e, ").append(i).append(" FPS").toString();
        }
        if (i < 12) {
            return new StringBuffer("\u0441\u043b\u0430\u0439\u0434-\u0448\u043e\u0443, ").append(i).append(" FPS").toString();
        }
        return new StringBuffer("\u0442\u043e\u0440\u043c\u043e\u0437\u0438\u0442, ").append(i).append(" FPS").toString();
    }
}
