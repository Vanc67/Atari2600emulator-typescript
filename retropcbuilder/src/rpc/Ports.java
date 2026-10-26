package rpc;

public class Ports {
    public Ports() {
    }
    
    public static String[] of(rpc.Part a) {
        String[] a0 = null;
        if (a == null) {
            return new String[0];
        }
        switch(a.cat) {
            case 20: {
                a0 = new String[1];
                a0[0] = (a.year >= 1998) ? "USB 2.0" : "LPT (IEEE-1284)";
                break;
            }
            case 17: {
                a0 = new String[1];
                a0[0] = (a.year >= 1998) ? "USB" : "Game port";
                break;
            }
            case 16: {
                a0 = rpc.Ports.mouse(a.year);
                break;
            }
            case 15: {
                a0 = rpc.Ports.keyboard(a.year);
                break;
            }
            case 14: {
                a0 = rpc.Ports.network(a);
                break;
            }
            case 13: {
                a0 = rpc.Ports.sound(a.year);
                break;
            }
            case 12: {
                a0 = rpc.Ports.monitor(a.year);
                break;
            }
            case 11: {
                a0 = new String[1];
                a0[0] = new StringBuffer("\u0432\u0435\u043d\u0442\u0438\u043b\u044f\u0442\u043e\u0440 ").append(a.f[1]).append(" \u043c\u043c").toString();
                break;
            }
            case 10: {
                a0 = rpc.Ports.casePorts(a.year);
                break;
            }
            case 9: {
                a0 = rpc.Ports.psu(a.year);
                break;
            }
            case 8: {
                a0 = new String[1];
                a0[0] = rpc.Score.ifaceName(a.f[0]);
                break;
            }
            case 7: {
                a0 = new String[1];
                a0[0] = rpc.Score.ifaceName(a.f[0]);
                break;
            }
            case 6: {
                a0 = new String[1];
                a0[0] = "FDD 34-pin";
                break;
            }
            case 5: {
                a0 = new String[1];
                a0[0] = rpc.Score.ifaceName(a.f[0]);
                break;
            }
            case 3: {
                a0 = rpc.Ports.motherboard(a.year);
                break;
            }
            case 2: {
                a0 = rpc.Ports.video(a.year);
                break;
            }
            default: {
                return new String[0];
            }
        }
        return a0;
    }
    
    public static String line(rpc.Part a) {
        String[] a0 = rpc.Ports.of(a);
        if (a0.length == 0) {
            return "";
        }
        StringBuffer a1 = new StringBuffer();
        int i = 0;
        while(i < a0.length) {
            if (i > 0) {
                a1.append(", ");
            }
            a1.append(a0[i]);
            i = i + 1;
        }
        return a1.toString();
    }
    
    public static String[] motherboard(int i) {
        if (i < 1995) {
            String[] a = new String[6];
            a[0] = new StringBuffer("ISA x").append((i >= 1990) ? 3 : 5).toString();
            a[1] = "VLB x2";
            a[2] = "COM x2";
            a[3] = "LPT";
            a[4] = "PS/2";
            a[5] = "DIN";
            return a;
        }
        if (i < 1998) {
            String[] a0 = new String[7];
            a0[0] = "ISA x4";
            a0[1] = "PCI x3";
            a0[2] = "AGP";
            a0[3] = "COM x2";
            a0[4] = "LPT";
            a0[5] = "PS/2 x2";
            a0[6] = "USB 1.0 x2";
            return a0;
        }
        if (i < 2001) {
            String[] a1 = new String[7];
            a1[0] = "PCI x5";
            a1[1] = "AGP4X";
            a1[2] = "USB 1.1 x2";
            a1[3] = "PS/2 x2";
            a1[4] = "COM x2";
            a1[5] = "LPT";
            a1[6] = "IDE x2";
            return a1;
        }
        if (i < 2004) {
            String[] a2 = new String[8];
            a2[0] = "PCI x5";
            a2[1] = "AGP8X";
            a2[2] = "USB 2.0 x4";
            a2[3] = "PS/2 x2";
            a2[4] = "IDE x2";
            a2[5] = "SATA";
            a2[6] = "LPT";
            a2[7] = "LAN RJ-45";
            return a2;
        }
        if (i < 2008) {
            String[] a3 = new String[8];
            a3[0] = "PCI x3";
            a3[1] = "PCIe x16";
            a3[2] = "PCIe x1 x2";
            a3[3] = "USB 2.0 x6";
            a3[4] = "SATA x4";
            a3[5] = "PS/2 x2";
            a3[6] = "LAN RJ-45";
            a3[7] = "\u0437\u0432\u0443\u043a 3.5 \u043c\u043c";
            return a3;
        }
        if (i < 2013) {
            String[] a4 = new String[8];
            a4[0] = "PCIe x16";
            a4[1] = "PCIe x1 x3";
            a4[2] = "USB 2.0 x6";
            a4[3] = "USB 3.0 x2";
            a4[4] = "SATA x6";
            a4[5] = "LAN RJ-45";
            a4[6] = "\u0437\u0432\u0443\u043a 3.5 \u043c\u043c";
            a4[7] = "HDMI";
            return a4;
        }
        if (i < 2018) {
            String[] a5 = new String[8];
            a5[0] = "PCIe 3.0 x16";
            a5[1] = "PCIe x1 x3";
            a5[2] = "USB 3.0 x6";
            a5[3] = "SATA x6";
            a5[4] = "M.2";
            a5[5] = "LAN RJ-45";
            a5[6] = "\u0437\u0432\u0443\u043a 3.5 \u043c\u043c";
            a5[7] = "HDMI";
            return a5;
        }
        String[] a6 = new String[10];
        a6[0] = "PCIe 4.0 x16";
        a6[1] = "PCIe x1 x3";
        a6[2] = "USB 3.1 x6";
        a6[3] = "USB-C";
        a6[4] = "SATA x6";
        a6[5] = "M.2 NVMe";
        a6[6] = "LAN RJ-45";
        a6[7] = "\u0437\u0432\u0443\u043a 3.5 \u043c\u043c";
        a6[8] = "HDMI";
        a6[9] = "DisplayPort";
        return a6;
    }
    
    public static String[] video(int i) {
        if (i < 1990) {
            String[] a = new String[2];
            a[0] = "CGA/EGA 9-pin";
            a[1] = "RCA";
            return a;
        }
        if (i < 1995) {
            String[] a0 = new String[2];
            a0[0] = "VGA";
            a0[1] = "RCA";
            return a0;
        }
        if (i < 2000) {
            String[] a1 = new String[2];
            a1[0] = "VGA";
            a1[1] = "S-Video";
            return a1;
        }
        if (i < 2004) {
            String[] a2 = new String[3];
            a2[0] = "VGA";
            a2[1] = "S-Video";
            a2[2] = "DVI";
            return a2;
        }
        if (i < 2009) {
            String[] a3 = new String[3];
            a3[0] = "VGA";
            a3[1] = "DVI";
            a3[2] = "TV-out";
            return a3;
        }
        if (i < 2015) {
            String[] a4 = new String[3];
            a4[0] = "DVI";
            a4[1] = "HDMI";
            a4[2] = "VGA";
            return a4;
        }
        if (i >= 2020) {
            String[] a5 = new String[2];
            a5[0] = "HDMI 2.1";
            a5[1] = "DisplayPort 1.4";
            return a5;
        }
        String[] a6 = new String[3];
        a6[0] = "HDMI";
        a6[1] = "DisplayPort";
        a6[2] = "DVI";
        return a6;
    }
    
    public static String[] monitor(int i) {
        if (i < 1988) {
            String[] a = new String[2];
            a[0] = "RGB 9-pin";
            a[1] = "\u043a\u043e\u043c\u043f\u043e\u0437\u0438\u0442";
            return a;
        }
        if (i < 1995) {
            String[] a0 = new String[1];
            a0[0] = "VGA 15-pin";
            return a0;
        }
        if (i < 2004) {
            String[] a1 = new String[1];
            a1[0] = "VGA 15-pin";
            return a1;
        }
        if (i < 2010) {
            String[] a2 = new String[2];
            a2[0] = "VGA";
            a2[1] = "DVI";
            return a2;
        }
        if (i >= 2016) {
            String[] a3 = new String[3];
            a3[0] = "HDMI";
            a3[1] = "DisplayPort";
            a3[2] = "USB-\u0445\u0430\u0431";
            return a3;
        }
        String[] a4 = new String[3];
        a4[0] = "VGA";
        a4[1] = "DVI";
        a4[2] = "HDMI";
        return a4;
    }
    
    public static String[] keyboard(int i) {
        if (i < 1988) {
            String[] a = new String[1];
            a[0] = "DIN-5";
            return a;
        }
        if (i < 1998) {
            String[] a0 = new String[1];
            a0[0] = "PS/2";
            return a0;
        }
        if (i >= 2006) {
            String[] a1 = new String[1];
            a1[0] = "USB";
            return a1;
        }
        String[] a2 = new String[2];
        a2[0] = "PS/2";
        a2[1] = "USB";
        return a2;
    }
    
    public static String[] mouse(int i) {
        if (i < 1990) {
            String[] a = new String[1];
            a[0] = "COM 9-pin";
            return a;
        }
        if (i < 1997) {
            String[] a0 = new String[2];
            a0[0] = "COM 9-pin";
            a0[1] = "PS/2";
            return a0;
        }
        if (i >= 2004) {
            String[] a1 = new String[1];
            a1[0] = "USB";
            return a1;
        }
        String[] a2 = new String[2];
        a2[0] = "PS/2";
        a2[1] = "USB";
        return a2;
    }
    
    public static String[] network(rpc.Part a) {
        if (a.year < 1990) {
            String[] a0 = new String[1];
            a0[0] = "BNC (\u043a\u043e\u0430\u043a\u0441\u0438\u0430\u043b)";
            return a0;
        }
        if (a.year < 1996) {
            String[] a1 = new String[2];
            a1[0] = "BNC";
            a1[1] = "AUI";
            return a1;
        }
        if (a.year < 2000) {
            String[] a2 = new String[2];
            a2[0] = "RJ-45";
            a2[1] = "BNC";
            return a2;
        }
        if (a.f[2] < 2) {
            String[] a3 = new String[1];
            a3[0] = "RJ-45";
            return a3;
        }
        String[] a4 = new String[1];
        a4[0] = new StringBuffer("RJ-45 x").append(a.f[2]).toString();
        return a4;
    }
    
    public static String[] sound(int i) {
        if (i < 1993) {
            String[] a = new String[2];
            a[0] = "jack 3.5";
            a[1] = "MIDI/Game port";
            return a;
        }
        if (i >= 2001) {
            String[] a0 = new String[3];
            a0[0] = "jack 3.5 (Line out)";
            a0[1] = "jack 3.5 (Mic)";
            a0[2] = "MIDI/Game port";
            return a0;
        }
        String[] a1 = new String[3];
        a1[0] = "jack 3.5";
        a1[1] = "MIDI/Game port";
        a1[2] = "CD-in";
        return a1;
    }
    
    public static String[] casePorts(int i) {
        if (i < 1996) {
            String[] a = new String[3];
            a[0] = "\u043a\u043d\u043e\u043f\u043a\u0430 Power";
            a[1] = "Turbo";
            a[2] = "LED";
            return a;
        }
        if (i < 2004) {
            String[] a0 = new String[2];
            a0[0] = "USB x2";
            a0[1] = "LED Power/HDD";
            return a0;
        }
        String[] a1 = new String[3];
        a1[0] = "USB x2";
        a1[1] = "jack 3.5";
        a1[2] = "LED Power/HDD";
        return a1;
    }
    
    public static String[] psu(int i) {
        if (i < 1996) {
            String[] a = new String[2];
            a[0] = "AT (P8/P9)";
            a[1] = "\u043c\u043e\u043b\u0435\u043a\u0441 x4";
            return a;
        }
        if (i < 2004) {
            String[] a0 = new String[3];
            a0[0] = "ATX 20-pin";
            a0[1] = "\u043c\u043e\u043b\u0435\u043a\u0441 x5";
            a0[2] = "FDD";
            return a0;
        }
        if (i >= 2012) {
            String[] a1 = new String[4];
            a1[0] = "ATX 24-pin";
            a1[1] = "8-pin CPU";
            a1[2] = "SATA x6";
            a1[3] = "PCIe 8-pin x2";
            return a1;
        }
        String[] a2 = new String[4];
        a2[0] = "ATX 24-pin";
        a2[1] = "4-pin CPU";
        a2[2] = "SATA x4";
        a2[3] = "PCIe 6-pin";
        return a2;
    }
    
    public static String iface(int i) {
        if (i < 1998) {
            return "LPT";
        }
        if (i >= 2004) {
            return "USB 2.0";
        }
        return "USB 1.1";
    }
    
    public static String join(rpc.Part a, rpc.Part a0) {
        if (a != null && a0 != null) {
            if (a.cat == 12 && a0.cat == 2) {
                return rpc.Ports.link(a, a0);
            }
            if (a.cat == 2 && a0.cat == 12) {
                return rpc.Ports.link(a0, a);
            }
            if (a.cat == 20 && a0.cat == 3) {
                String[] a1 = rpc.Ports.of(a0);
                if (a.year < 1998 && !rpc.Ports.has(a1, "LPT")) {
                    return "\u043f\u0440\u0438\u043d\u0442\u0435\u0440\u0443 \u043d\u0443\u0436\u0435\u043d LPT (\u043d\u0435\u0442 \u043d\u0430 \u043f\u043b\u0430\u0442\u0435)";
                }
                if (a.year >= 1998 && !rpc.Ports.has(a1, "USB")) {
                    return "\u043f\u0440\u0438\u043d\u0442\u0435\u0440\u0443 \u043d\u0443\u0436\u0435\u043d USB (\u043d\u0435\u0442 \u043d\u0430 \u043f\u043b\u0430\u0442\u0435)";
                }
            }
            String s = null;
            return s;
        }
        String s0 = null;
        return s0;
    }
    
    private static String link(rpc.Part a, rpc.Part a0) {
        String[] a1 = rpc.Ports.of(a);
        String[] a2 = rpc.Ports.of(a0);
        int i = 0;
        while(i < a1.length) {
            String s = rpc.Ports.base(a1[i]);
            int i0 = 0;
            while(i0 < a2.length) {
                if (s.equals((Object)rpc.Ports.base(a2[i0]))) {
                    String s0 = null;
                    return s0;
                }
                i0 = i0 + 1;
            }
            i = i + 1;
        }
        return new StringBuffer("\u043d\u0435\u0442 \u043e\u0431\u0449\u0435\u0433\u043e \u0440\u0430\u0437\u044a\u0451\u043c\u0430: ").append(rpc.Ports.of(a)[0]).append(" \u2260 ").append(rpc.Ports.of(a0)[0]).toString();
    }
    
    private static String base(String s) {
        int i = s.indexOf(32);
        if (i > 0) {
            s = s.substring(0, i);
        }
        return s;
    }
    
    private static boolean has(String[] a, String s) {
        int i = 0;
        while(i < a.length) {
            if (a[i].indexOf(s) >= 0) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }
}
