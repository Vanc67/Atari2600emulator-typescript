package rpc;

public class Setup {
    public Setup() {
    }
    
    public static String name(rpc.Part a) {
        int i = (a != null) ? a.year : 1990;
        String[] a0 = new String[6];
        a0[0] = "Award";
        a0[1] = "AMI";
        a0[2] = "Phoenix";
        a0[3] = "Award";
        a0[4] = "AMI";
        a0[5] = "UEFI";
        int i0 = (i >= 1990) ? (i >= 1996) ? (i >= 2000) ? (i >= 2005) ? (i >= 2012) ? 5 : 4 : 3 : 2 : 1 : 0;
        String s = a0[i0];
        if (i0 == 5) {
            return "UEFI (2012+)";
        }
        return new StringBuffer(String.valueOf((Object)String.valueOf((Object)s))).append(" BIOS ").append((i >= 1996) ? (i >= 2005) ? "8.0" : "6.00" : "4.51").toString();
    }
    
    public static boolean hasUefi(rpc.Part a) {
        if (a != null && a.year >= 2012) {
            return true;
        }
        return false;
    }
    
    public static boolean hasTpm(rpc.Part a) {
        if (a != null && a.year >= 2014) {
            return true;
        }
        return false;
    }
    
    public static boolean hasSecureBoot(rpc.Part a) {
        if (a != null && a.year >= 2012) {
            return true;
        }
        return false;
    }
    
    public static boolean hasXmp(rpc.Part a) {
        if (a != null && a.year >= 2007) {
            return true;
        }
        return false;
    }
    
    public static String[] screen(rpc.Part a) {
        boolean b = rpc.Setup.hasUefi(a);
        boolean b0 = rpc.Setup.hasTpm(a);
        boolean b1 = rpc.Setup.hasSecureBoot(a);
        String[] a0 = new String[9];
        a0[0] = new StringBuffer("BIOS: ").append(rpc.Setup.name(a)).toString();
        a0[1] = new StringBuffer("\u0414\u0430\u0442\u0430: ").append((a != null) ? new StringBuffer().append(a.year).toString() : "?").toString();
        a0[2] = b ? "\u0420\u0435\u0436\u0438\u043c: UEFI" : "\u0420\u0435\u0436\u0438\u043c: Legacy BIOS";
        a0[3] = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430: [\u0434\u0438\u0441\u043a] [CD] [USB] [FDD]";
        a0[4] = (rpc.Setup.hasXmp(a)) ? "XMP/EXPO: \u0432\u044b\u043a\u043b" : "XMP: \u043d\u0435\u0442 \u0432 \u044d\u0442\u043e\u0439 \u043f\u0440\u043e\u0448\u0438\u0432\u043a\u0435";
        a0[5] = b0 ? "TPM 2.0: \u0432\u044b\u043a\u043b" : "TPM: \u043c\u043e\u0434\u0443\u043b\u044f \u043d\u0435\u0442";
        a0[6] = b1 ? "Secure Boot: \u0432\u044b\u043a\u043b" : "Secure Boot: \u043d\u0435\u0442";
        a0[7] = "Virtualization: \u0432\u044b\u043a\u043b";
        a0[8] = "Fan control: \u0430\u0432\u0442\u043e";
        return a0;
    }
    
    public static String hint(int i) {
        if (i == 3) {
            return "\u043f\u043e\u0440\u044f\u0434\u043e\u043a \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438: \u0441 \u0447\u0435\u0433\u043e \u0441\u0442\u0430\u0432\u0438\u043c \u041e\u0421";
        }
        if (i == 4) {
            return "XMP \u0443\u0441\u043a\u043e\u0440\u044f\u0435\u0442 \u043f\u0430\u043c\u044f\u0442\u044c, \u043d\u043e \u043d\u0435\u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e";
        }
        if (i == 5) {
            return "Windows 11 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 TPM 2.0";
        }
        if (i == 6) {
            return "Secure Boot \u0431\u043b\u043e\u043a\u0438\u0440\u0443\u0435\u0442 \u0441\u0442\u0430\u0440\u044b\u0435 \u0437\u0430\u0433\u0440\u0443\u0437\u0447\u0438\u043a\u0438";
        }
        if (i != 7) {
            return "\u0441\u0442\u0440\u0435\u043b\u043a\u0438 \u2014 \u0432\u044b\u0431\u043e\u0440, OK \u2014 \u0438\u0437\u043c\u0435\u043d\u0438\u0442\u044c";
        }
        return "\u0432\u0438\u0440\u0442\u0443\u0430\u043b\u043a\u0438 \u0438 64-\u0431\u0438\u0442\u043d\u044b\u0435 \u0433\u043e\u0441\u0442\u0438";
    }
}
