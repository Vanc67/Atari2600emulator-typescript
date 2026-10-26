package rpc;

public class Os {
    public Os() {
    }
    
    public static boolean sameLines(String[] a, String[] a0, int i) {
        if (a != null && a.length >= i) {
            int i0 = 0;
            while(i0 < i) {
                if (!((a[i0] != null) ? a[i0] : "").equals((Object)((a0[i0] != null) ? a0[i0] : ""))) {
                    return false;
                }
                i0 = i0 + 1;
            }
            return true;
        }
        return false;
    }
    
    public static String name(int i) {
        if (i < 1990) {
            return "RC-DOS 3";
        }
        if (i < 1995) {
            return "RC-DOS 6";
        }
        if (i < 2000) {
            return "RPC-OS 95";
        }
        if (i < 2007) {
            return "RPC-OS XP";
        }
        if (i < 2013) {
            return "RPC-OS 7";
        }
        if (i < 2018) {
            return "RPC-OS 10";
        }
        if (i >= 2024) {
            return "RPC-OS 12";
        }
        return "RPC-OS 11";
    }
    
    public static boolean langAvailable(int i, int i0) {
        if (i0 < rpc.Vm.LANG_YEAR[i]) {
            return false;
        }
        return true;
    }
    
    public static String langLocked(int i) {
        return new StringBuffer("\u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 ").append(rpc.Vm.LANG_YEAR[i]).append(" \u0433\u043e\u0434\u0443").toString();
    }
    
    public static rpc.Os$Task[] tasks(int i) {
        int i0 = 0;
        rpc.Os$Task[] a = new rpc.Os$Task[8];
        a[0] = rpc.Os.task(0, 8, "\u0421\u043b\u043e\u0436\u0438 5 \u0438 3", 30 + (i - 1980) * 2);
        a[1] = rpc.Os.task(0, 16, "\u0423\u0434\u0432\u043e\u0439 1 \u0447\u0435\u0442\u044b\u0440\u0435 \u0440\u0430\u0437\u0430", 40 + (i - 1980) * 2);
        a[2] = rpc.Os.task(0, 21, "\u0421\u0443\u043c\u043c\u0430 1..6 \u0447\u0435\u0440\u0435\u0437 \u0446\u0438\u043a\u043b", 60 + (i - 1980) * 3);
        if (i < 1985) {
            i0 = 3;
        } else {
            a[3] = rpc.Os.task(1, 6, "10 \u043c\u0438\u043d\u0443\u0441 4 \u0432 \u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0435", 50 + (i - 1985) * 2);
            a[4] = rpc.Os.task(1, 100, "\u0423\u043c\u043d\u043e\u0436\u044c 25 \u043d\u0430 4", 80 + (i - 1985) * 3);
            i0 = 5;
        }
        if (i >= 1990) {
            int i1 = i0 + 1;
            a[i0] = rpc.Os.task(2, 12, "\u0423\u043c\u043d\u043e\u0436\u044c 3 \u043d\u0430 4", 70 + (i - 1990) * 3);
            i0 = i1 + 1;
            a[i1] = rpc.Os.task(2, 55, "\u0421\u0443\u043c\u043c\u0430 1..10 \u0446\u0438\u043a\u043b\u043e\u043c", 120 + (i - 1990) * 4);
        }
        rpc.Os$Task[] a0 = new rpc.Os$Task[i0];
        System.arraycopy((Object)a, 0, (Object)a0, 0, i0);
        return a0;
    }
    
    private static rpc.Os$Task task(int i, int i0, String s, int i1) {
        rpc.Os$Task a = new rpc.Os$Task();
        a.lang = i;
        a.expect = i0;
        a.title = s;
        a.reward = i1;
        return a;
    }
    
    public static int mineIncome(int i, int i0, int i1) {
        if (i > 0 && i0 >= 2010) {
            int i2 = i * 40 / (60 + (i0 - 2010) * 15);
            if (i2 < 1) {
                i2 = 1;
            }
            return i2 * ((i1 > 0) ? (i1 >= 1) ? (i1 >= 20) ? (i1 < 100) ? 100 : 110 : 85 : 60 : 30) / 100;
        }
        return 0;
    }
}
