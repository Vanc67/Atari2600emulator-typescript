package rpc;

public class Market {
    final public static int SHOP = 0;
    final public static int STREET = 1;
    final public static int FLEA = 2;
    final public static int JUNK = 3;
    final public static int AVITO = 4;
    final public static int ALI = 5;
    final public static int FARM = 6;
    final public static int COUNT = 7;
    final public static String[] NAME;
    final public static int[] SINCE;
    final private static int[] PRICE_PCT;
    final private static int[] COND_MIN;
    final private static int[] COND_MAX;
    final private static int[] DAYS;
    
    static {
        String[] a = new String[7];
        a[0] = "\u041c\u0430\u0433\u0430\u0437\u0438\u043d (\u043d\u043e\u0432\u043e\u0435)";
        a[1] = "\u0423\u043b\u0438\u0447\u043d\u044b\u0439 \u0440\u044b\u043d\u043e\u043a";
        a[2] = "\u0411\u0430\u0440\u0430\u0445\u043e\u043b\u043a\u0430";
        a[3] = "\u041c\u0443\u0441\u043e\u0440\u043a\u0430";
        a[4] = "Avito";
        a[5] = "AliExpress";
        a[6] = "\u041c\u0430\u0439\u043d\u0438\u043d\u0433-\u0444\u0435\u0440\u043c\u0430";
        NAME = a;
        int[] a0 = new int[7];
        a0[0] = 1980;
        a0[1] = 1980;
        a0[2] = 1985;
        a0[3] = 1980;
        a0[4] = 2014;
        a0[5] = 2014;
        a0[6] = 2010;
        SINCE = a0;
        int[] a1 = new int[7];
        a1[0] = 100;
        a1[1] = 70;
        a1[2] = 45;
        a1[4] = 60;
        a1[5] = 38;
        a1[6] = 40;
        PRICE_PCT = a1;
        int[] a2 = new int[7];
        a2[0] = 100;
        a2[1] = 55;
        a2[2] = 35;
        a2[3] = 10;
        a2[4] = 50;
        a2[5] = 85;
        a2[6] = 30;
        COND_MIN = a2;
        int[] a3 = new int[7];
        a3[0] = 100;
        a3[1] = 90;
        a3[2] = 65;
        a3[3] = 55;
        a3[4] = 95;
        a3[5] = 100;
        a3[6] = 60;
        COND_MAX = a3;
        int[] a4 = new int[7];
        a4[0] = 1;
        a4[1] = 1;
        a4[2] = 1;
        a4[4] = 3;
        a4[5] = 18;
        a4[6] = 2;
        DAYS = a4;
    }
    
    public Market() {
    }
    
    public static boolean available(int i, int i0) {
        if (i >= 0 && i < 7 && i0 >= SINCE[i]) {
            return true;
        }
        return false;
    }
    
    public static String note(int i, int i0) {
        if (i == 0) {
            return "\u043d\u043e\u0432\u043e\u0435, \u0433\u0430\u0440\u0430\u043d\u0442\u0438\u044f, \u043f\u043e\u043b\u043d\u0430\u044f \u0446\u0435\u043d\u0430";
        }
        if (i == 1) {
            return "\u043b\u043e\u0442\u043a\u0438 \u0441 \u0440\u0443\u043a: \u0442\u043e\u0440\u0433, \u043d\u043e \u0431\u0435\u0437 \u0433\u0430\u0440\u0430\u043d\u0442\u0438\u0438";
        }
        if (i == 2) {
            return "\u0431\u0430\u0440\u0430\u0445\u043e\u043b\u043a\u0430: \u0434\u0451\u0448\u0435\u0432\u043e, \u0441\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435 \u043a\u0430\u043a \u043f\u043e\u0432\u0435\u0437\u0451\u0442";
        }
        if (i == 3) {
            return "\u0441\u0432\u0430\u043b\u043a\u0430: \u0438\u0449\u0438 \u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e, \u043f\u043e\u0442\u043e\u043c \u0440\u0435\u043c\u043e\u043d\u0442\u0438\u0440\u0443\u0439";
        }
        if (i == 4) {
            return "\u043e\u0431\u044a\u044f\u0432\u043b\u0435\u043d\u0438\u044f \u0441 2014: \u0432\u044b\u0431\u043e\u0440 \u043b\u044e\u0431\u043e\u0439 \u0433\u043e\u0434";
        }
        if (i != 5) {
            return "\u043a\u0430\u0440\u0442\u044b \u043f\u043e\u0441\u043b\u0435 \u043c\u0430\u0439\u043d\u0438\u043d\u0433\u0430: \u0434\u0451\u0448\u0435\u0432\u043e \u0438 \u0443\u0448\u0430\u0442\u0430\u043d\u043d\u043e";
        }
        return "\u043f\u043e\u0441\u044b\u043b\u043a\u0430 \u0438\u0437 \u041a\u0438\u0442\u0430\u044f: \u0434\u0451\u0448\u0435\u0432\u043e, \u043d\u043e 2-4 \u043d\u0435\u0434\u0435\u043b\u0438";
    }
    
    public static int pricePct(int i) {
        int[] a = PRICE_PCT;
        label0: {
            label1: {
                if (i < 0) {
                    break label1;
                }
                if (i < 7) {
                    break label0;
                }
            }
            i = 0;
        }
        return a[i];
    }
    
    public static int condMin(int i) {
        return COND_MIN[i];
    }
    
    public static int condMax(int i) {
        return COND_MAX[i];
    }
    
    public static int baseDays(int i) {
        return DAYS[i];
    }
    
    public static int days(int i, int i0) {
        int i1 = DAYS[i];
        if (i != 5) {
            if (i == 4 && i0 >= 1) {
                return i1 - 1;
            }
            return i1;
        } else {
            if (i0 <= 0) {
                return i1 * 2;
            }
            if (i0 < 1) {
                return i1 * 2;
            }
            if (i0 < 20) {
                return i1 + 8;
            }
            if (i0 < 100) {
                return i1;
            }
            return i1 - 6;
        }
    }
    
    public static int[] yearWindow(int i, int i0) {
        if (i == 0) {
            int[] a = new int[2];
            a[0] = i0 - 6;
            a[1] = i0;
            return a;
        }
        if (i == 1) {
            int[] a0 = new int[2];
            a0[0] = i0 - 9;
            a0[1] = i0;
            return a0;
        }
        if (i == 2) {
            int[] a1 = new int[2];
            a1[0] = Math.max(1979, i0 - 16);
            a1[1] = i0;
            return a1;
        }
        if (i == 3) {
            int[] a2 = new int[2];
            a2[0] = Math.max(1979, i0 - 18);
            a2[1] = i0;
            return a2;
        }
        if (i == 4) {
            int[] a3 = new int[2];
            a3[0] = 1979;
            a3[1] = i0;
            return a3;
        }
        if (i == 5) {
            int[] a4 = new int[2];
            a4[0] = Math.max(1995, i0 - 10);
            a4[1] = i0;
            return a4;
        }
        int[] a5 = new int[2];
        a5[0] = Math.max(2008, i0 - 10);
        a5[1] = i0 - 1;
        return a5;
    }
    
    public static boolean scamRisk(int i) {
        if (i != 4 && i != 2) {
            return false;
        }
        return true;
    }
}
