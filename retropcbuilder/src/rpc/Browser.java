package rpc;

public class Browser {
    final public static int S_FORUM = 0;
    final public static int S_MUSEUM = 1;
    final public static int S_NEWS = 2;
    final public static int S_WIKI = 3;
    final public static int S_VENDOR = 4;
    final public static int S_MARKET = 5;
    final public static int S_AVITO = 6;
    final public static int S_ALI = 7;
    final public static int S_PRESS = 8;
    final public static int S_SEARCH = 9;
    final public static int SITES = 10;
    final public static String[] NAME;
    final public static String[] URL;
    final public static int[] SINCE;
    
    static {
        String[] a = new String[10];
        a[0] = "\u0424\u043e\u0440\u0443\u043c \u044d\u043d\u0442\u0443\u0437\u0438\u0430\u0441\u0442\u043e\u0432";
        a[1] = "\u041c\u0443\u0437\u0435\u0439 \u0436\u0435\u043b\u0435\u0437\u0430";
        a[2] = "\u041d\u043e\u0432\u043e\u0441\u0442\u0438 IT";
        a[3] = "\u0412\u0438\u043a\u0438\u043f\u0435\u0434\u0438\u044f";
        a[4] = "\u0421\u0430\u0439\u0442 \u043f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044f";
        a[5] = "\u041c\u0430\u0440\u043a\u0435\u0442\u043f\u043b\u0435\u0439\u0441";
        a[6] = "Avito";
        a[7] = "AliExpress";
        a[8] = "\u041f\u0440\u0435\u0441\u0441\u0430 \u043e \u0444\u0438\u0440\u043c\u0430\u0445";
        a[9] = "\u041f\u043e\u0438\u0441\u043a";
        NAME = a;
        String[] a0 = new String[10];
        a0[0] = "forum.retro-pc.ru";
        a0[1] = "museum.chip.ru";
        a0[2] = "news.it.ru";
        a0[3] = "wiki.ru";
        a0[4] = "vendor.com";
        a0[5] = "market.ru";
        a0[6] = "avito.ru";
        a0[7] = "aliexpress.com";
        a0[8] = "press.it.ru";
        a0[9] = "search.ru";
        URL = a0;
        int[] a1 = new int[10];
        a1[0] = 1994;
        a1[1] = 1996;
        a1[2] = 1995;
        a1[3] = 2001;
        a1[4] = 1993;
        a1[5] = 1999;
        a1[6] = 2014;
        a1[7] = 2014;
        a1[8] = 1990;
        a1[9] = 1997;
        SINCE = a1;
    }
    
    public Browser() {
    }
    
    public static boolean available(int i, int i0) {
        if (i0 < SINCE[i]) {
            return false;
        }
        return true;
    }
    
    public static int count(int i) {
        int i0 = 0;
        int i1 = 0;
        while(i1 < 10) {
            if (rpc.Browser.available(i1, i)) {
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        return i0;
    }
    
    public static int nth(int i, int i0) {
        int i1 = 0;
        int i2 = 0;
        while(i2 < 10) {
            if (rpc.Browser.available(i2, i)) {
                if (i1 == i0) {
                    return i2;
                }
                i1 = i1 + 1;
            }
            i2 = i2 + 1;
        }
        return 0;
    }
    
    public static int pages(int i, int i0, rpc.Db a) {
        int[] a0 = null;
        if (i != 5 && i != 6 && i != 7) {
            if (i == 3) {
                return 8;
            }
            if (i == 8) {
                return 4;
            }
            if (i != 9) {
                return 5;
            }
            return 1;
        }
        if (i != 6) {
            a0 = new int[2];
            a0[0] = 1;
            a0[1] = 2;
        } else {
            a0 = new int[3];
            a0[0] = 1;
            a0[1] = 2;
            a0[2] = 3;
        }
        int i1 = 6 + a.query(a0, i0 - 6, i0).length / 4;
        if (i1 > 30) {
            i1 = 30;
        }
        return i1;
    }
    
    public static String[] page(int i, int i0, int i1, rpc.Db a, rpc.Corp a0) {
        if (i == 0) {
            return rpc.Browser.forum(i0, i1, a);
        }
        if (i == 1) {
            return rpc.Browser.museum(i0, i1, a);
        }
        if (i == 2) {
            return rpc.Browser.news(i0, i1);
        }
        if (i == 3) {
            return rpc.Browser.wiki(i0, i1, a);
        }
        if (i == 4) {
            return rpc.Browser.vendor(i0, i1, a);
        }
        if (i == 8) {
            return rpc.Browser.press(i0, i1, a0);
        }
        if (i == 9) {
            return rpc.Browser.search(i1, a);
        }
        return rpc.Browser.shop(i0, i1, a, i);
    }
    
    public static int offerPart(int i, int i0, int i1, rpc.Db a) {
        if (i != 5 && i != 6 && i != 7) {
            return -1;
        }
        int[] a0 = rpc.Browser.offerList(i, i1, a);
        if (i0 >= 1 && i0 <= a0.length) {
            return a0[i0 - 1];
        }
        return -1;
    }
    
    public static int[] offerList(int i, int i0, rpc.Db a) {
        int[] a0 = null;
        if (i != 6) {
            a0 = new int[2];
            a0[0] = 1;
            a0[1] = 2;
        } else {
            a0 = new int[3];
            a0[0] = 1;
            a0[1] = 2;
            a0[2] = 3;
        }
        return a.query(a0, i0 - 6, i0);
    }
    
    public static int price(int i, rpc.Part a) {
        int i0 = a.price;
        if (i == 6) {
            return i0 * 55 / 100 + 5;
        }
        if (i != 7) {
            return i0;
        }
        return i0 * 45 / 100 + 5;
    }
    
    public static String priceNote(int i) {
        if (i == 6) {
            return "\u0442\u043e\u0440\u0433, \u0431/\u0443, \u0440\u0438\u0441\u043a \u00ab\u043d\u0435 \u0442\u043e \u043d\u0430 \u0444\u043e\u0442\u043e\u00bb";
        }
        if (i != 7) {
            return "\u043d\u043e\u0432\u0430\u044f, \u0433\u0430\u0440\u0430\u043d\u0442\u0438\u044f";
        }
        return "\u0434\u0435\u0448\u0435\u0432\u043e, 20% \u043f\u043e\u0434\u0434\u0435\u043b\u043e\u043a";
    }
    
    private static String[] forum(int i, int i0, rpc.Db a) {
        if (i == 0) {
            String[] a0 = new String[8];
            a0[0] = "\u0424\u041e\u0420\u0423\u041c \u042d\u041d\u0422\u0423\u0417\u0418\u0410\u0421\u0422\u041e\u0412";
            a0[1] = new StringBuffer("\u0422\u0435\u043c \u043d\u0430 \u0444\u043e\u0440\u0443\u043c\u0435: ").append(120 + i0 % 90).toString();
            a0[2] = "";
            a0[3] = new StringBuffer("1. \u041a\u0430\u043a\u043e\u0439 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 \u0431\u0440\u0430\u0442\u044c \u0432 ").append(i0).append("?").toString();
            a0[4] = "2. \u041a\u0443\u043b\u0435\u0440 \u0433\u0443\u0434\u0438\u0442 \u2014 \u0447\u0442\u043e \u0434\u0435\u043b\u0430\u0442\u044c";
            a0[5] = "3. \u0420\u0430\u0437\u0433\u043e\u043d: \u0441\u0442\u043e\u0438\u0442 \u043b\u0438 \u043e\u043d\u043e \u0442\u043e\u0433\u043e";
            a0[6] = "4. \u041f\u044b\u043b\u044c \u0438 \u0451\u043c\u043a\u043e\u0441\u0442\u0438: \u043e\u043f\u044b\u0442 \u0440\u0435\u043c\u043e\u043d\u0442\u0430";
            a0[7] = "5. \u0412\u0435\u0442\u043a\u0430 \u00ab\u043f\u043e\u043a\u0430\u0436\u0438\u0442\u0435 \u0441\u0432\u043e\u044e \u0441\u0431\u043e\u0440\u043a\u0443\u00bb";
            return a0;
        }
        if (i != 1) {
            if (i == 2) {
                String[] a1 = new String[10];
                a1[0] = "\u0422\u0415\u041c\u0410: \u041a\u0443\u043b\u0435\u0440 \u0433\u0443\u0434\u0438\u0442";
                a1[1] = "";
                a1[2] = "1. \u041f\u0440\u043e\u0434\u0443\u0442\u044c \u0440\u0430\u0434\u0438\u0430\u0442\u043e\u0440 \u043e\u0442 \u043f\u044b\u043b\u0438.";
                a1[3] = "2. \u041f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043e\u0431\u043e\u0440\u043e\u0442\u044b (BIOS).";
                a1[4] = "3. \u0421\u043c\u0430\u0437\u0430\u0442\u044c \u0438\u043b\u0438 \u043c\u0435\u043d\u044f\u0442\u044c \u0432\u0435\u043d\u0442\u0438\u043b\u044c.";
                a1[5] = "4. \u0421\u043b\u0438\u0448\u043a\u043e\u043c \u0433\u043e\u0440\u044f\u0447\u0438\u0439 \u043a\u0430\u043c\u0435\u043d\u044c \u2014";
                a1[6] = "   \u0431\u0430\u0448\u043d\u044f \u043d\u0435 \u0441\u043f\u0440\u0430\u0432\u043b\u044f\u0435\u0442\u0441\u044f,";
                a1[7] = "   \u0431\u0443\u0434\u0435\u0442 \u043f\u0435\u0440\u0435\u0433\u0440\u0435\u0432 \u0438 BSOD.";
                a1[8] = "";
                a1[9] = new StringBuffer("\u041f\u043e\u043c\u043e\u0433\u043b\u043e: ").append(3 + i0 % 12).append(" \u043b\u044e\u0434\u044f\u043c").toString();
                return a1;
            }
            if (i == 3) {
                String[] a2 = new String[11];
                a2[0] = "\u0422\u0415\u041c\u0410: \u0420\u0430\u0437\u0433\u043e\u043d";
                a2[1] = "";
                a2[2] = "\u00ab\u041f\u043e\u0434\u043d\u044f\u043b \u0447\u0430\u0441\u0442\u043e\u0442\u0443 \u043d\u0430 20% \u2014";
                a2[3] = "\u043f\u043e\u043b\u0443\u0447\u0438\u043b +15% \u043f\u043e\u043f\u0443\u0433\u0430\u0435\u0432 \u0438";
                a2[4] = "+40% \u0442\u0435\u043f\u043b\u0430. \u041e\u043d\u043e \u043d\u0430\u0434\u043e?\u00bb";
                a2[5] = "";
                a2[6] = "\u0421\u0442\u0430\u0440\u044b\u0439 \u0442\u0435\u0445\u043f\u0440\u043e\u0446\u0435\u0441\u0441 \u0447\u0430\u0441\u0442\u043e\u0442\u0443";
                a2[7] = "\u043d\u0435 \u043b\u044e\u0431\u0438\u0442: TDP \u0440\u0430\u0441\u0442\u0451\u0442";
                a2[8] = "\u043a\u0432\u0430\u0434\u0440\u0430\u0442\u0438\u0447\u043d\u043e.";
                a2[9] = "";
                a2[10] = new StringBuffer("\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u0430\u043d\u0438\u0435: \u0434\u0430 ").append(20 + i0 % 40).append("% / \u043d\u0435\u0442 ").append(40 + i0 % 30).append("%").toString();
                return a2;
            }
            String[] a3 = new String[10];
            a3[0] = "\u0422\u0415\u041c\u0410: \u041f\u043e\u043a\u0430\u0436\u0438\u0442\u0435 \u0441\u0432\u043e\u044e \u0441\u0431\u043e\u0440\u043a\u0443";
            a3[1] = "";
            a3[2] = "\u0424\u043e\u0442\u043e: (\u043d\u0435\u0442 \u043a\u0430\u0440\u0442\u0438\u043d\u043a\u0438)";
            a3[3] = new StringBuffer("\u0413\u043e\u0434: ").append(i0).toString();
            a3[4] = new StringBuffer("\u041a\u043e\u0440\u043f\u0443\u0441: ").append(rpc.Browser.caseOf(i0, a)).toString();
            a3[5] = "";
            a3[6] = "\u00ab\u0421\u043e\u0431\u0438\u0440\u0430\u043b \u0442\u0440\u0438 \u0432\u0435\u0447\u0435\u0440\u0430,";
            a3[7] = "\u0437\u0430\u0431\u044b\u043b \u0441\u043d\u044f\u0442\u044c \u043f\u043b\u0451\u043d\u043a\u0443 \u0441 \u043a\u0443\u043b\u0435\u0440\u0430\u00bb";
            a3[8] = "";
            a3[9] = new StringBuffer("\u041b\u0430\u0439\u043a\u043e\u0432: ").append(15 + i0 % 70).toString();
            return a3;
        } else {
            int[] a4 = new int[1];
            a4[0] = 1;
            int[] a5 = a.query(a4, i0 - 1, i0);
            String s = "\u2014";
            int i1 = -1;
            int i2 = 0;
            while(true) {
                int i3 = a5.length;
                int i4 = i1;
                if (i2 < i3) {
                    if (i2 < 400) {
                        rpc.Part a6 = a.get(a5[i2]);
                        i1 = a6.f[1] * a6.f[2] * a6.f[4] / 10;
                        if (i1 <= i4) {
                            i1 = i4;
                        } else {
                            s = a6.name;
                        }
                        i2 = i2 + 1;
                        continue;
                    } else {
                        i1 = i4;
                    }
                }
                String[] a7 = new String[10];
                a7[0] = "\u0422\u0415\u041c\u0410: \u041a\u0430\u043a\u043e\u0439 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 \u0431\u0440\u0430\u0442\u044c";
                a7[1] = new StringBuffer("\u0413\u043e\u0434: ").append(i0).toString();
                a7[2] = "";
                a7[3] = new StringBuffer("\u0421\u043e\u0432\u0435\u0442 \u0444\u043e\u0440\u0443\u043c\u0430: ").append(s).toString();
                a7[4] = new StringBuffer("\u041f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c: ").append(i1).toString();
                a7[5] = "";
                a7[6] = "\u00ab\u041d\u0435 \u0431\u0435\u0440\u0438\u0442\u0435 \u0441\u0430\u043c\u044b\u0439 \u0434\u0435\u0448\u0451\u0432\u044b\u0439 \u2014";
                a7[7] = "\u043f\u043e\u0436\u0430\u043b\u0435\u0435\u0442\u0435 \u043d\u0430 \u043e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u0438\u00bb";
                a7[8] = "";
                a7[9] = new StringBuffer("\u041e\u0442\u0432\u0435\u0442\u043e\u0432: ").append(7 + i0 % 30).toString();
                return a7;
            }
        }
    }
    
    private static String caseOf(int i, rpc.Db a) {
        int[] a0 = new int[1];
        a0[0] = 10;
        int[] a1 = a.query(a0, i - 2, i);
        return (a1.length <= 0) ? "\u0441\u0430\u043c\u043e\u0434\u0435\u043b\u044c\u043d\u044b\u0439" : a.get(a1[0]).name;
    }
    
    private static String[] museum(int i, int i0, rpc.Db a) {
        int[] a0 = new int[1];
        a0[0] = 1;
        int[] a1 = a.query(a0, i0 - 2, i0);
        int[] a2 = new int[2];
        a2[0] = 2;
        a2[1] = 19;
        int[] a3 = a.query(a2, i0 - 2, i0);
        int[] a4 = new int[1];
        a4[0] = 4;
        int[] a5 = a.query(a4, i0 - 2, i0);
        String s = (a1.length <= 0) ? "\u2014" : a.get(a1[a1.length - 1]).name;
        String s0 = (a3.length <= 0) ? "\u2014" : a.get(a3[a3.length - 1]).name;
        String s1 = (a5.length <= 0) ? "\u2014" : a.get(a5[0]).name;
        if (i == 0) {
            String[] a6 = new String[10];
            a6[0] = "\u041c\u0423\u0417\u0415\u0419 \u0416\u0415\u041b\u0415\u0417\u0410";
            a6[1] = new StringBuffer("\u0417\u0430\u043b: ").append(i0).append(" \u0433\u043e\u0434").toString();
            a6[2] = "";
            a6[3] = "\u042d\u043a\u0441\u043f\u043e\u043d\u0430\u0442\u044b \u0437\u0430\u043b\u0430:";
            a6[4] = new StringBuffer("CPU: ").append(s).toString();
            a6[5] = new StringBuffer("GPU: ").append(s0).toString();
            a6[6] = new StringBuffer("RAM: ").append(s1).toString();
            a6[7] = "";
            a6[8] = "\u0412\u0441\u0435\u0433\u043e \u0432 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0435 \u043c\u0443\u0437\u0435\u044f:";
            a6[9] = new StringBuffer().append(a.count()).append(" \u0434\u0435\u0442\u0430\u043b\u0435\u0439 1979-2026").toString();
            return a6;
        }
        if (i == 1) {
            String[] a7 = new String[8];
            a7[0] = "\u0417\u0410\u041b: \u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u044b";
            a7[1] = new StringBuffer("\u042d\u043a\u0441\u043f\u043e\u043d\u0430\u0442: ").append(s).toString();
            a7[2] = "";
            a7[3] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append((a1.length > 0 && a.get(a1[a1.length - 1]).f[1] != 0) ? 1 : 0).append(" \u041c\u0413\u0446").toString();
            a7[4] = new StringBuffer("\u042f\u0434\u0435\u0440: ").append((a1.length > 0 && a.get(a1[a1.length - 1]).f[2] != 0) ? 1 : 0).toString();
            a7[5] = new StringBuffer("\u0421\u043e\u043a\u0435\u0442: ").append((a1.length <= 0) ? "\u2014" : rpc.Score.sockName((a.get(a1[a1.length - 1]).f[0] != 0) ? 1 : 0)).toString();
            a7[6] = "";
            a7[7] = "\u0422\u0430\u0431\u043b\u0438\u0447\u043a\u0430: \u00ab\u0420\u0430\u0431\u043e\u0442\u0430\u043b \u0431\u0435\u0437";
            return a7;
        }
        if (i == 2) {
            String[] a8 = new String[9];
            a8[0] = "\u0417\u0410\u041b: \u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u044b";
            a8[1] = new StringBuffer("\u042d\u043a\u0441\u043f\u043e\u043d\u0430\u0442: ").append(s0).toString();
            a8[2] = "";
            a8[3] = new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c: ").append((a3.length > 0 && a.get(a3[a3.length - 1]).f[1] != 0) ? 1 : 0).append(" \u041a\u0411").toString();
            a8[4] = new StringBuffer("\u0428\u0438\u043d\u0430: ").append((a3.length <= 0) ? "\u2014" : rpc.Score.busName((a.get(a3[a3.length - 1]).f[0] != 0) ? 1 : 0)).toString();
            a8[5] = "";
            a8[6] = "\u00ab\u0412 1998-\u043c \u044d\u0442\u043e \u0442\u044f\u043d\u0443\u043b\u043e Quake\u00bb";
            a8[7] = "";
            a8[8] = "\u041f\u043e\u0441\u0435\u0442\u0438\u0442\u0435\u043b\u0435\u0439 \u0441\u0435\u0433\u043e\u0434\u043d\u044f: 12";
            return a8;
        }
        if (i != 3) {
            String[] a9 = new String[10];
            a9[0] = "\u041c\u0423\u0417\u0415\u0419: \u044d\u043a\u0441\u043a\u0443\u0440\u0441\u0438\u044f";
            a9[1] = "";
            a9[2] = "\u00ab\u0416\u0435\u043b\u0435\u0437\u043e \u0441\u0442\u0430\u0440\u0435\u0435\u0442 \u0431\u044b\u0441\u0442\u0440\u0435\u0435";
            a9[3] = "\u043b\u044e\u0434\u0435\u0439. \u0412 1985 \u0433\u043e\u0434\u0443 8 \u041c\u0413\u0446";
            a9[4] = "\u043a\u0430\u0437\u0430\u043b\u0438\u0441\u044c \u043a\u043e\u0441\u043c\u043e\u0441\u043e\u043c, \u0430";
            a9[5] = "\u0441\u0435\u0433\u043e\u0434\u043d\u044f \u044d\u0442\u043e \u0447\u0430\u0441\u044b \u0432 \u0442\u043e\u0441\u0442\u0435\u0440\u0435\u00bb.";
            a9[6] = "";
            a9[7] = new StringBuffer("\u0421\u043c\u043e\u0442\u0440\u0438\u0442\u0435\u043b\u044c \u043c\u0443\u0437\u0435\u044f, ").append(i0).toString();
            a9[8] = "";
            a9[9] = "\u0411\u0438\u043b\u0435\u0442: \u0431\u0435\u0441\u043f\u043b\u0430\u0442\u043d\u043e";
            return a9;
        }
        String[] a10 = new String[10];
        a10[0] = "\u0417\u0410\u041b: \u041f\u0430\u043c\u044f\u0442\u044c";
        a10[1] = new StringBuffer("\u042d\u043a\u0441\u043f\u043e\u043d\u0430\u0442: ").append(s1).toString();
        a10[2] = "";
        a10[3] = new StringBuffer("\u0422\u0438\u043f: ").append((a5.length <= 0) ? "\u2014" : rpc.Score.ramTypeName((a.get(a5[0]).f[0] != 0) ? 1 : 0)).toString();
        a10[4] = new StringBuffer("\u041e\u0431\u044a\u0451\u043c: ").append((a5.length > 0 && a.get(a5[0]).f[1] != 0) ? 1 : 0).append(" \u041a\u0411").toString();
        a10[5] = "";
        a10[6] = "\u041a\u043e\u043d\u0442\u0430\u043a\u0442\u044b \u043e\u043a\u0438\u0441\u043b\u0438\u043b\u0438\u0441\u044c \u2014";
        a10[7] = "\u043f\u0440\u043e\u0442\u0435\u0440\u0435\u0442\u044c \u043b\u0430\u0441\u0442\u0438\u043a\u043e\u043c \u0438 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442.";
        a10[8] = "";
        a10[9] = "\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435: \u043c\u0443\u0437\u0435\u0439\u043d\u043e\u0435";
        return a10;
    }
    
    private static String[] news(int i, int i0) {
        String[] a = rpc.Browser.headlines(i0);
        if (i == 0) {
            String[] a0 = new String[8];
            a0[0] = new StringBuffer("\u041d\u041e\u0412\u041e\u0421\u0422\u0418 IT \u2014 ").append(i0).toString();
            a0[1] = "";
            a0[2] = new StringBuffer("\u2022 ").append(a[0]).toString();
            a0[3] = new StringBuffer("\u2022 ").append(a[1]).toString();
            a0[4] = new StringBuffer("\u2022 ").append(a[2]).toString();
            a0[5] = "";
            a0[6] = "\u041a\u0443\u0440\u0441 \u043a\u0440\u0435\u0434\u0438\u0442\u0430: \u0441\u0442\u0430\u0431\u0438\u043b\u0435\u043d";
            a0[7] = "\u0420\u0435\u043a\u043b\u0430\u043c\u0430: \u0441\u043e\u0431\u0438\u0440\u0430\u0439 \u043f\u0440\u0430\u0432\u0438\u043b\u044c\u043d\u043e";
            return a0;
        }
        if (i == 1) {
            String[] a1 = new String[10];
            a1[0] = "\u041d\u041e\u0412\u041e\u0421\u0422\u042c \u0414\u041d\u042f";
            a1[1] = "";
            a1[2] = a[0];
            a1[3] = "";
            a1[4] = "\u0410\u043d\u0430\u043b\u0438\u0442\u0438\u043a\u0438 \u0441\u0447\u0438\u0442\u0430\u044e\u0442, \u0447\u0442\u043e";
            a1[5] = "\u0433\u043e\u043d\u043a\u0430 \u0447\u0430\u0441\u0442\u043e\u0442 \u043f\u0440\u043e\u0434\u043e\u043b\u0436\u0438\u0442\u0441\u044f,";
            a1[6] = "\u0430 \u0432\u043e\u0442 \u043e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u0435 \u0437\u0430 \u043d\u0435\u0439";
            a1[7] = "\u043d\u0435 \u0443\u0441\u043f\u0435\u0432\u0430\u0435\u0442.";
            a1[8] = "";
            a1[9] = new StringBuffer("\u041e\u0431\u0441\u0443\u0436\u0434\u0435\u043d\u0438\u0435: ").append(30 + i0 % 90).append(" \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439").toString();
            return a1;
        }
        if (i == 2) {
            String[] a2 = new String[10];
            a2[0] = "\u0420\u042b\u041d\u041e\u041a";
            a2[1] = "";
            a2[2] = a[1];
            a2[3] = "";
            a2[4] = "\u0426\u0435\u043d\u044b \u043d\u0430 \u043f\u0430\u043c\u044f\u0442\u044c:";
            a2[5] = "\u0440\u0430\u0441\u0442\u0443\u0442 \u0432\u0442\u043e\u0440\u043e\u0439 \u043a\u0432\u0430\u0440\u0442\u0430\u043b \u043f\u043e\u0434\u0440\u044f\u0434.";
            a2[6] = "";
            a2[7] = "\u00ab\u041b\u0443\u0447\u0448\u0435 \u0431\u0440\u0430\u0442\u044c \u0441\u0435\u0439\u0447\u0430\u0441\u00bb, \u2014";
            a2[8] = "\u0441\u043e\u0432\u0435\u0442\u0443\u044e\u0442 \u043f\u0440\u043e\u0434\u0430\u0432\u0446\u044b.";
            a2[9] = "\u041f\u0440\u043e\u0434\u0430\u0432\u0446\u0430\u043c \u0432\u0435\u0440\u0438\u0442\u044c \u043d\u0435 \u043f\u0440\u0438\u043d\u044f\u0442\u043e.";
            return a2;
        }
        if (i == 3) {
            String[] a3 = new String[7];
            a3[0] = "\u0422\u0415\u0425\u041d\u041e\u041b\u041e\u0413\u0418\u0418";
            a3[1] = "";
            a3[2] = a[2];
            a3[3] = "";
            a3[4] = "\u0418\u043d\u0436\u0435\u043d\u0435\u0440\u044b \u043e\u0431\u0435\u0449\u0430\u044e\u0442:";
            a3[5] = "\u043c\u0435\u043d\u044c\u0448\u0435 \u0442\u0435\u043f\u043b\u0430, \u0431\u043e\u043b\u044c\u0448\u0435 \u0447\u0430\u0441\u0442\u043e\u0442.";
            a3[6] = "\u041a\u0430\u043a \u043e\u0431\u044b\u0447\u043d\u043e, \u00ab\u0441\u043a\u043e\u0440\u043e\u00bb.";
            return a3;
        }
        String[] a4 = new String[10];
        a4[0] = "\u041f\u0418\u0421\u042c\u041c\u0410 \u0427\u0418\u0422\u0410\u0422\u0415\u041b\u0415\u0419";
        a4[1] = "";
        a4[2] = "\u00ab\u0421\u043e\u0431\u0440\u0430\u043b \u043f\u043e \u0432\u0430\u0448\u0435\u043c\u0443 \u0433\u0430\u0439\u0434\u0443,";
        a4[3] = "\u0437\u0430\u0431\u044b\u043b \u043f\u0440\u043e \u0441\u043e\u043a\u0435\u0442 \u2014 \u043d\u0435 \u0432\u043b\u0435\u0437\u043b\u043e.";
        a4[4] = "\u0427\u0438\u0442\u0430\u0439\u0442\u0435 \u0445\u0430\u0440\u0430\u043a\u0442\u0435\u0440\u0438\u0441\u0442\u0438\u043a\u0438!\u00bb";
        a4[5] = "";
        a4[6] = new StringBuffer("\u2014 \u0447\u0438\u0442\u0430\u0442\u0435\u043b\u044c \u0438\u0437 ").append(1980 + i0 % 40).toString();
        a4[7] = "";
        a4[8] = "\u0420\u0435\u0434\u0430\u043a\u0446\u0438\u044f: \u0434\u0430, \u0441\u043e\u043a\u0435\u0442 \u2014";
        a4[9] = "\u044d\u0442\u043e \u043d\u0435 \u043c\u0435\u043b\u043e\u0447\u044c.";
        return a4;
    }
    
    private static String[] headlines(int i) {
        String s = null;
        String s0 = null;
        String s1 = null;
        if (i > 1983) {
            if (i > 1989) {
                if (i > 1994) {
                    if (i > 1999) {
                        if (i > 2004) {
                            if (i > 2009) {
                                if (i > 2014) {
                                    if (i > 2019) {
                                        s = "\u0413\u0438\u0431\u0440\u0438\u0434\u043d\u044b\u0435 \u044f\u0434\u0440\u0430 \u0438 3-\u043d\u043c \u0442\u0435\u0445\u043f\u0440\u043e\u0446\u0435\u0441\u0441";
                                        s0 = "DDR5 \u0434\u0435\u0448\u0435\u0432\u0435\u0435\u0442, PCIe 5.0 \u043f\u0440\u0438\u0445\u043e\u0434\u0438\u0442";
                                        s1 = "\u0411\u043b\u043e\u043a\u0438 \u043f\u0438\u0442\u0430\u043d\u0438\u044f \u043f\u043e \u043d\u043e\u0432\u043e\u043c\u0443 \u0441\u0442\u0430\u043d\u0434\u0430\u0440\u0442\u0443";
                                    } else {
                                        s = "\u041c\u043d\u043e\u0433\u043e\u044f\u0434\u0435\u0440\u043d\u043e\u0441\u0442\u044c \u0434\u043b\u044f \u0432\u0441\u0435\u0445";
                                        s0 = "\u0412\u0438\u0434\u0435\u043e\u043f\u0430\u043c\u044f\u0442\u044c \u0434\u043e\u0440\u043e\u0436\u0430\u0435\u0442 \u0438\u0437-\u0437\u0430 \u043c\u0430\u0439\u043d\u0438\u043d\u0433\u0430";
                                        s1 = "NVMe: \u0434\u0438\u0441\u043a\u0438 \u0431\u044b\u0441\u0442\u0440\u0435\u0435 \u0448\u0438\u043d\u044b";
                                    }
                                } else {
                                    s = "\u0412\u0441\u0442\u0440\u043e\u0435\u043d\u043d\u043e\u0435 \u0432\u0438\u0434\u0435\u043e \u0434\u043e\u0433\u043d\u0430\u043b\u043e \u0431\u044e\u0434\u0436\u0435\u0442\u043d\u044b\u0435 \u043a\u0430\u0440\u0442\u044b";
                                    s0 = "DDR3 \u043d\u0430 \u043f\u0438\u043a\u0435, DDR4 \u043d\u0430 \u043f\u043e\u0440\u043e\u0433\u0435";
                                    s1 = "M.2: \u043c\u0430\u043b\u0435\u043d\u044c\u043a\u0438\u0439 \u0441\u043b\u043e\u0442, \u0431\u043e\u043b\u044c\u0448\u043e\u0439 \u0441\u043c\u044b\u0441\u043b";
                                }
                            } else {
                                s = "\u0414\u0432\u0443\u0445\u044a\u044f\u0434\u0435\u0440\u043d\u044b\u0435 \u2014 \u044d\u0442\u043e \u043d\u043e\u0440\u043c\u0430";
                                s0 = "SSD \u043e\u0431\u0435\u0449\u0430\u044e\u0442 \u0437\u0430\u043c\u0435\u043d\u0438\u0442\u044c \u0432\u0438\u043d\u0447\u0435\u0441\u0442\u0435\u0440\u044b";
                                s1 = "PCIe 2.0: \u0448\u0438\u043d\u0430 \u0441\u043d\u043e\u0432\u0430 \u0431\u044b\u0441\u0442\u0440\u0435\u0435";
                            }
                        } else {
                            s = "\u0413\u0438\u0433\u0430\u0433\u0435\u0440\u0446 \u0432\u0437\u044f\u0442, \u0442\u0435\u043f\u0435\u0440\u044c \u044f\u0434\u0440\u0430";
                            s0 = "DDR \u043f\u0440\u043e\u0442\u0438\u0432 Rambus: \u043f\u043e\u0431\u0435\u0434\u0438\u043b\u0430 \u0434\u0435\u0448\u0435\u0432\u0438\u0437\u043d\u0430";
                            s1 = "SATA \u043f\u0440\u0438\u0445\u043e\u0434\u0438\u0442 \u043d\u0430 \u0441\u043c\u0435\u043d\u0443 IDE";
                        }
                    } else {
                        s = "3D-\u0443\u0441\u043a\u043e\u0440\u0438\u0442\u0435\u043b\u0438 \u043c\u0435\u043d\u044f\u044e\u0442 \u0438\u0433\u0440\u044b";
                        s0 = "SDRAM \u0434\u0435\u0448\u0435\u0432\u0435\u0435\u0442, \u0432\u0438\u043d\u0447\u0435\u0441\u0442\u0435\u0440\u044b \u0440\u0430\u0441\u0442\u0443\u0442";
                        s1 = "AGP: \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430\u043c \u0441\u0442\u0430\u043b\u043e \u0442\u0435\u0441\u043d\u043e \u0432 PCI";
                    }
                } else {
                    s = "486 \u0438 Pentium: \u0433\u043e\u043d\u043a\u0430 \u0447\u0430\u0441\u0442\u043e\u0442 \u043d\u0430\u0447\u0430\u043b\u0430\u0441\u044c";
                    s0 = "\u0417\u0432\u0443\u043a: Sound Blaster \u0441\u0442\u0430\u043b \u0441\u0442\u0430\u043d\u0434\u0430\u0440\u0442\u043e\u043c";
                    s1 = "CD-ROM: 150 \u041a\u0411/\u0441 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0432\u0441\u0435\u043c";
                }
            } else {
                s = "286 \u043f\u0440\u043e\u0442\u0438\u0432 386: \u043a\u043e\u043c\u0443 \u043d\u0443\u0436\u043d\u0430 \u0437\u0430\u0449\u0438\u0442\u0430?";
                s0 = "\u0416\u0451\u0441\u0442\u043a\u0438\u0439 \u0434\u0438\u0441\u043a \u043d\u0430 40 \u041c\u0411 \u2014 \u0433\u0438\u0433\u0430\u043d\u0442";
                s1 = "VGA: 256 \u0446\u0432\u0435\u0442\u043e\u0432 \u0443\u0436\u0435 \u0440\u0435\u0430\u043b\u044c\u043d\u043e\u0441\u0442\u044c";
            }
        } else {
            s = "IBM PC \u0437\u0430\u0434\u0430\u0451\u0442 \u0441\u0442\u0430\u043d\u0434\u0430\u0440\u0442: 8088 \u0438 ISA";
            s0 = "\u041f\u0430\u043c\u044f\u0442\u044c \u0434\u043e\u0440\u043e\u0436\u0430\u0435\u0442: 64 \u041a\u0411 \u2014 \u0440\u043e\u0441\u043a\u043e\u0448\u044c";
            s1 = "\u041c\u043e\u043d\u0438\u0442\u043e\u0440\u044b: \u0437\u0435\u043b\u0451\u043d\u044b\u0439 \u0438\u043b\u0438 \u044f\u043d\u0442\u0430\u0440\u043d\u044b\u0439?";
        }
        String[] a = new String[3];
        a[0] = s;
        a[1] = s0;
        a[2] = s1;
        return a;
    }
    
    private static String[] wiki(int i, int i0, rpc.Db a) {
        String s = null;
        if (i == 0) {
            String[] a0 = new String[10];
            a0[0] = "\u0412\u0418\u041a\u0418\u041f\u0415\u0414\u0418\u042f \u2014 \u0433\u043b\u0430\u0432\u043d\u0430\u044f";
            a0[1] = "";
            a0[2] = "\u0421\u0442\u0430\u0442\u044c\u0438 \u043f\u043e \u0433\u043e\u0434\u0430\u043c:";
            a0[3] = "1. \u041f\u0435\u0440\u0441\u043e\u043d\u0430\u043b\u044c\u043d\u044b\u0439 \u043a\u043e\u043c\u043f\u044c\u044e\u0442\u0435\u0440";
            a0[4] = "2. \u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440";
            a0[5] = "3. \u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430";
            a0[6] = "4. \u041e\u043f\u0435\u0440\u0430\u0442\u0438\u0432\u043d\u0430\u044f \u043f\u0430\u043c\u044f\u0442\u044c";
            a0[7] = "5. \u041c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u0430\u044f \u043f\u043b\u0430\u0442\u0430";
            a0[8] = "6. \u041d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u0438";
            a0[9] = "7. \u0421\u043e\u043a\u0435\u0442 (\u0440\u0430\u0437\u044a\u0451\u043c)";
            return a0;
        }
        String[] a1 = new String[7];
        a1[0] = "\u041f\u0415\u0420\u0421\u041e\u041d\u0410\u041b\u042c\u041d\u042b\u0419 \u041a\u041e\u041c\u041f\u042c\u042e\u0422\u0415\u0420";
        a1[1] = "\u041f\u0420\u041e\u0426\u0415\u0421\u0421\u041e\u0420";
        a1[2] = "\u0412\u0418\u0414\u0415\u041e\u041a\u0410\u0420\u0422\u0410";
        a1[3] = "\u041e\u041f\u0415\u0420\u0410\u0422\u0418\u0412\u041d\u0410\u042f \u041f\u0410\u041c\u042f\u0422\u042c";
        a1[4] = "\u041c\u0410\u0422\u0415\u0420\u0418\u041d\u0421\u041a\u0410\u042f \u041f\u041b\u0410\u0422\u0410";
        a1[5] = "\u041d\u0410\u041a\u041e\u041f\u0418\u0422\u0415\u041b\u0418";
        a1[6] = "\u0421\u041e\u041a\u0415\u0422";
        String[] a2 = new String[7];
        a2[0] = "\u041c\u0430\u0448\u0438\u043d\u0430 \u0434\u043b\u044f \u043b\u0438\u0447\u043d\u043e\u0433\u043e \u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d\u0438\u044f,";
        a2[1] = "\u0423\u0441\u0442\u0440\u043e\u0439\u0441\u0442\u0432\u043e, \u0432\u044b\u043f\u043e\u043b\u043d\u044f\u044e\u0449\u0435\u0435 \u043a\u043e\u043c\u0430\u043d\u0434\u044b,";
        a2[2] = "\u041f\u043b\u0430\u0442\u0430, \u0432\u044b\u0432\u043e\u0434\u044f\u0449\u0430\u044f \u0438\u0437\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435,";
        a2[3] = "\u041f\u0430\u043c\u044f\u0442\u044c \u0441 \u043f\u0440\u043e\u0438\u0437\u0432\u043e\u043b\u044c\u043d\u044b\u043c \u0434\u043e\u0441\u0442\u0443\u043f\u043e\u043c,";
        a2[4] = "\u041e\u0441\u043d\u043e\u0432\u0430 \u0441\u0431\u043e\u0440\u043a\u0438: \u0441\u043e\u043a\u0435\u0442, \u0441\u043b\u043e\u0442\u044b,";
        a2[5] = "\u0414\u043e\u043b\u0433\u043e\u0432\u0440\u0435\u043c\u0435\u043d\u043d\u043e\u0435 \u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435 \u0434\u0430\u043d\u043d\u044b\u0445:";
        a2[6] = "\u0420\u0430\u0437\u044a\u0451\u043c \u0434\u043b\u044f \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0438 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u0430:";
        String[] a3 = new String[7];
        a3[0] = "\u0441\u043e\u0431\u0440\u0430\u043d\u043d\u0430\u044f \u0438\u0437 \u0441\u043e\u0432\u043c\u0435\u0441\u0442\u0438\u043c\u044b\u0445 \u0434\u0435\u0442\u0430\u043b\u0435\u0439.";
        a3[1] = "\u0441\u043e\u0434\u0435\u0440\u0436\u0438\u0442 \u044f\u0434\u0440\u0430 \u0438 \u043a\u044d\u0448.";
        a3[2] = "\u0438\u043c\u0435\u0435\u0442 \u0441\u043e\u0431\u0441\u0442\u0432\u0435\u043d\u043d\u0443\u044e \u043f\u0430\u043c\u044f\u0442\u044c.";
        a3[3] = "\u044d\u043d\u0435\u0440\u0433\u043e\u0437\u0430\u0432\u0438\u0441\u0438\u043c\u0430 \u0438 \u043d\u0443\u0436\u043d\u0430 \u0432\u0441\u0435\u0433\u0434\u0430.";
        a3[4] = "\u0448\u0438\u043d\u044b \u0438 \u043f\u043e\u0440\u0442\u044b \u0432\u0432\u043e\u0434\u0430-\u0432\u044b\u0432\u043e\u0434\u0430.";
        a3[5] = "\u0436\u0451\u0441\u0442\u043a\u0438\u0435 \u0434\u0438\u0441\u043a\u0438, SSD, \u043e\u043f\u0442\u0438\u043a\u0430.";
        a3[6] = "\u0444\u043e\u0440\u043c-\u0444\u0430\u043a\u0442\u043e\u0440\u044b \u0438 \u043a\u043b\u044e\u0447\u0438.";
        int i1 = i - 1;
        if (i1 < 0) {
            i1 = 0;
        }
        if (i1 > 6) {
            i1 = 6;
        }
        if (i1 != 1) {
            if (i1 != 2) {
                s = "";
            } else {
                int[] a4 = new int[1];
                a4[0] = 2;
                int[] a5 = a.query(a4, i0 - 1, i0);
                s = (a5.length <= 0) ? "" : new StringBuffer("\u041f\u0440\u0438\u043c\u0435\u0440 ").append(i0).append(": ").append(a.get(a5[a5.length - 1]).name).toString();
            }
        } else {
            int[] a6 = new int[1];
            a6[0] = 1;
            int[] a7 = a.query(a6, i0 - 1, i0);
            s = (a7.length <= 0) ? "" : new StringBuffer("\u041f\u0440\u0438\u043c\u0435\u0440 ").append(i0).append(": ").append(a.get(a7[a7.length - 1]).name).toString();
        }
        String[] a8 = new String[10];
        a8[0] = "\u0412\u0418\u041a\u0418\u041f\u0415\u0414\u0418\u042f";
        a8[1] = a1[i1];
        a8[2] = "";
        a8[3] = a2[i1];
        a8[4] = a3[i1];
        a8[5] = "";
        a8[6] = new StringBuffer("\u0413\u043e\u0434 \u0441\u0442\u0430\u0442\u044c\u0438: ").append(i0).toString();
        a8[7] = s;
        a8[8] = "";
        a8[9] = new StringBuffer("\u0421\u0442\u0430\u0442\u044c\u044f \u043c\u0435\u043d\u044f\u043b\u0430\u0441\u044c ").append(10 + i0 % 40).append(" \u0440\u0430\u0437").toString();
        return a8;
    }
    
    private static String[] vendor(int i, int i0, rpc.Db a) {
        int[] a0 = new int[1];
        a0[0] = 1;
        int[] a1 = a.query(a0, i0, i0);
        int[] a2 = new int[1];
        a2[0] = 2;
        int[] a3 = a.query(a2, i0, i0);
        String s = (a1.length <= 0) ? "\u2014" : a.get(a1[a1.length - 1]).name;
        String s0 = (a3.length <= 0) ? "\u2014" : a.get(a3[a3.length - 1]).name;
        if (i == 0) {
            String[] a4 = new String[10];
            a4[0] = "\u0421\u0410\u0419\u0422 \u041f\u0420\u041e\u0418\u0417\u0412\u041e\u0414\u0418\u0422\u0415\u041b\u042f";
            a4[1] = new StringBuffer("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 ").append(i0).append(" \u0433\u043e\u0434\u0430").toString();
            a4[2] = "";
            a4[3] = "\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u044b:";
            a4[4] = new StringBuffer("  ").append(s).toString();
            a4[5] = "";
            a4[6] = "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u044b:";
            a4[7] = new StringBuffer("  ").append(s0).toString();
            a4[8] = "";
            a4[9] = "\u0421\u043a\u0430\u0447\u0430\u0442\u044c \u0434\u0440\u0430\u0439\u0432\u0435\u0440\u044b";
            return a4;
        }
        if (i == 1) {
            String[] a5 = new String[10];
            a5[0] = new StringBuffer("\u041f\u0420\u041e\u0426\u0415\u0421\u0421\u041e\u0420\u042b ").append(i0).toString();
            a5[1] = "";
            a5[2] = s;
            a5[3] = "";
            a5[4] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append((a1.length > 0 && a.get(a1[a1.length - 1]).f[1] != 0) ? 1 : 0).append(" \u041c\u0413\u0446").toString();
            a5[5] = new StringBuffer("\u0421\u043e\u043a\u0435\u0442: ").append((a1.length <= 0) ? "\u2014" : rpc.Score.sockName((a.get(a1[a1.length - 1]).f[0] != 0) ? 1 : 0)).toString();
            a5[6] = new StringBuffer("TDP: ").append((a1.length > 0 && a.get(a1[a1.length - 1]).f[3] != 0) ? 1 : 0).append(" \u0412\u0442").toString();
            a5[7] = "";
            a5[8] = "\u00ab\u0420\u0435\u043a\u043e\u043c\u0435\u043d\u0434\u0443\u0435\u043c \u043c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u0438\u0435";
            a5[9] = "\u043f\u043b\u0430\u0442\u044b \u043d\u0430\u0448\u0435\u0433\u043e \u043f\u0430\u0440\u0442\u043d\u0451\u0440\u0430\u00bb";
            return a5;
        }
        if (i != 2) {
            String[] a6 = new String[11];
            a6[0] = "\u041f\u041e\u0414\u0414\u0415\u0420\u0416\u041a\u0410";
            a6[1] = "";
            a6[2] = "\u0427\u0430\u0441\u0442\u044b\u0435 \u0432\u043e\u043f\u0440\u043e\u0441\u044b:";
            a6[3] = "\u2022 \u043f\u043e\u0447\u0435\u043c\u0443 \u0433\u0440\u0435\u0435\u0442\u0441\u044f?";
            a6[4] = "\u2022 \u043a\u0430\u043a\u043e\u0439 \u043a\u0443\u043b\u0435\u0440 \u043f\u043e\u0434\u043e\u0439\u0434\u0451\u0442?";
            a6[5] = "\u2022 \u043f\u043e\u0434\u043e\u0439\u0434\u0451\u0442 \u043b\u0438 \u043a \u043c\u043e\u0435\u0439 \u043f\u043b\u0430\u0442\u0435?";
            a6[6] = "";
            a6[7] = "\u041e\u0442\u0432\u0435\u0442: \u0441\u043c\u043e\u0442\u0440\u0438\u0442\u0435 \u0441\u043e\u043a\u0435\u0442";
            a6[8] = "\u0438 TDP. \u0418 \u043f\u0440\u043e\u0434\u0443\u0432\u0430\u0439\u0442\u0435 \u043f\u044b\u043b\u044c.";
            a6[9] = "";
            a6[10] = "\u0413\u0430\u0440\u0430\u043d\u0442\u0438\u044f: 12 \u043c\u0435\u0441\u044f\u0446\u0435\u0432";
            return a6;
        }
        String[] a7 = new String[9];
        a7[0] = new StringBuffer("\u0412\u0418\u0414\u0415\u041e\u041a\u0410\u0420\u0422\u042b ").append(i0).toString();
        a7[1] = "";
        a7[2] = s0;
        a7[3] = "";
        a7[4] = new StringBuffer("\u0428\u0438\u043d\u0430: ").append((a3.length <= 0) ? "\u2014" : rpc.Score.busName((a.get(a3[a3.length - 1]).f[0] != 0) ? 1 : 0)).toString();
        a7[5] = new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c: ").append((a3.length > 0 && a.get(a3[a3.length - 1]).f[1] != 0) ? 1 : 0).append(" \u041a\u0411").toString();
        a7[6] = "";
        a7[7] = "\u0414\u0440\u0430\u0439\u0432\u0435\u0440 1.0 \u2014 1,4 \u041c\u0411";
        a7[8] = "\u0414\u0440\u0430\u0439\u0432\u0435\u0440 1.1 \u2014 2,1 \u041c\u0411 (\u0431\u0435\u0442\u0430)";
        return a7;
    }
    
    private static String[] press(int i, int i0, rpc.Corp a) {
        if (a != null && a.founded && a.n != 0) {
            rpc.Corp$Prod[] a0 = a.prods;
            if (i >= a.n) {
                i = a.n - 1;
            }
            rpc.Corp$Prod a1 = a0[i];
            if (a1 == null) {
                a1 = a.prods[0];
            }
            String s = (a1.bugs <= 40) ? (a1.quality <= 75) ? "\u043d\u0430 \u0442\u0440\u043e\u0435\u0447\u043a\u0443" : "\u0420\u0415\u041a\u041e\u041c\u0415\u041d\u0414\u0423\u0415\u041c" : "\u0421\u042b\u0420\u041e\u0419";
            String[] a2 = new String[12];
            a2[0] = new StringBuffer("\u041e\u0411\u0417\u041e\u0420: ").append(a1.name).toString();
            a2[1] = "";
            a2[2] = new StringBuffer("\u0424\u0438\u0440\u043c\u0430: ").append(a.name).toString();
            a2[3] = new StringBuffer("\u0413\u043e\u0434: ").append(a1.year).toString();
            a2[4] = new StringBuffer("\u041a\u0430\u0447\u0435\u0441\u0442\u0432\u043e: ").append(a1.quality).append("%").toString();
            a2[5] = new StringBuffer("\u0411\u0430\u0433\u043e\u0432: ").append(a1.bugs).append("%").toString();
            a2[6] = new StringBuffer("\u0414\u043e\u043b\u044f \u0440\u044b\u043d\u043a\u0430: ").append(a.share(a1)).append("%").toString();
            a2[7] = "";
            a2[8] = "\u0412\u0435\u0440\u0434\u0438\u043a\u0442 \u0440\u0435\u0434\u0430\u043a\u0446\u0438\u0438:";
            a2[9] = s;
            a2[10] = "";
            a2[11] = (a1.bugs <= 40) ? "\u00ab\u0413\u043e\u0434\u0438\u0442\u0441\u044f \u0432 \u0441\u0431\u043e\u0440\u043a\u0443\u00bb" : "\u00ab\u041f\u0430\u0442\u0447\u0438 \u0436\u0434\u0451\u043c\u00bb";
            return a2;
        }
        String[] a3 = new String[8];
        a3[0] = "\u041f\u0420\u0415\u0421\u0421\u0410 \u041e \u0424\u0418\u0420\u041c\u0410\u0425";
        a3[1] = "";
        a3[2] = "\u041e\u0431\u0437\u043e\u0440\u044b \u044d\u0442\u043e\u0433\u043e \u0433\u043e\u0434\u0430:";
        a3[3] = "Intel, AMD, NVIDIA, Microsoft.";
        a3[4] = "";
        a3[5] = "\u0412\u0430\u0448\u0435\u0439 \u0444\u0438\u0440\u043c\u044b \u0432 \u043d\u043e\u0432\u043e\u0441\u0442\u044f\u0445";
        a3[6] = "\u043f\u043e\u043a\u0430 \u043d\u0435\u0442 \u2014 \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u0435";
        a3[7] = "\u043f\u0435\u0440\u0432\u044b\u0439 \u043f\u0440\u043e\u0434\u0443\u043a\u0442.";
        return a3;
    }
    
    private static String[] search(int i, rpc.Db a) {
        int[] a0 = new int[1];
        a0[0] = 1;
        int[] a1 = a.query(a0, i, i);
        int[] a2 = new int[1];
        a2[0] = 2;
        int[] a3 = a.query(a2, i, i);
        int[] a4 = new int[1];
        a4[0] = 4;
        int[] a5 = a.query(a4, i, i);
        String[] a6 = new String[10];
        a6[0] = "\u041f\u041e\u0418\u0421\u041a \u041f\u041e \u0421\u0415\u0422\u0418";
        a6[1] = "";
        a6[2] = new StringBuffer("\u041d\u0430\u0439\u0434\u0435\u043d\u043e \u0432 ").append(i).append(" \u0433\u043e\u0434\u0443:").toString();
        a6[3] = new StringBuffer("\u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u043e\u0432: ").append(a1.length).toString();
        a6[4] = new StringBuffer("\u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442: ").append(a3.length).toString();
        a6[5] = new StringBuffer("\u043c\u043e\u0434\u0443\u043b\u0435\u0439 \u043f\u0430\u043c\u044f\u0442\u0438: ").append(a5.length).toString();
        a6[6] = new StringBuffer("\u0432\u0441\u0435\u0433\u043e \u0432 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0435: ").append(a.count()).toString();
        a6[7] = "";
        a6[8] = "\u041f\u043e\u043f\u0443\u043b\u044f\u0440\u043d\u044b\u0439 \u0437\u0430\u043f\u0440\u043e\u0441:";
        a6[9] = new StringBuffer("\u00ab\u043a\u0430\u043a\u043e\u0439 \u0441\u043e\u043a\u0435\u0442 \u0443 ").append((a1.length <= 0) ? "\u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u0430" : a.get(a1[0]).name).append("\u00bb").toString();
        return a6;
    }
    
    private static String[] shop(int i, int i0, rpc.Db a, int i1) {
        int[] a0 = rpc.Browser.offerList(i1, i0, a);
        if (i != 0 && a0.length != 0) {
            int i2 = i - 1;
            if (i2 >= a0.length) {
                i2 = a0.length - 1;
            }
            rpc.Part a1 = a.get(a0[i2]);
            int i3 = rpc.Browser.price(i1, a1);
            String[] a2 = new String[10];
            a2[0] = NAME[i1].toUpperCase();
            a2[1] = new StringBuffer("\u041b\u043e\u0442 ").append(i2 + 1).append(" \u0438\u0437 ").append(a0.length).toString();
            a2[2] = "";
            a2[3] = a1.name;
            a2[4] = new StringBuffer("\u0413\u043e\u0434: ").append(a1.year).toString();
            a2[5] = new StringBuffer("\u0421\u043e\u0441\u0442\u043e\u044f\u043d\u0438\u0435: ").append((i1 != 6) ? (i1 != 7) ? 100 : 100 : 40 + i2 % 50).append("%").toString();
            a2[6] = new StringBuffer("\u0426\u0435\u043d\u0430: ").append(i3).append(" (\u0432 \u043c\u0430\u0433\u0430\u0437\u0438\u043d\u0435 ").append(a1.price).append(")").toString();
            a2[7] = "";
            a2[8] = "OK \u2014 \u0441\u043b\u0435\u0434\u0443\u044e\u0449\u0438\u0439 \u043b\u043e\u0442";
            a2[9] = "3 \u2014 \u043a\u0443\u043f\u0438\u0442\u044c \u0438 \u043d\u0430 \u0441\u0442\u043e\u043b";
            return a2;
        }
        String[] a3 = new String[8];
        a3[0] = NAME[i1].toUpperCase();
        a3[1] = (i1 != 6) ? (i1 != 7) ? "\u043d\u043e\u0432\u043e\u0435, \u0441 \u0433\u0430\u0440\u0430\u043d\u0442\u0438\u0435\u0439" : "\u0434\u0435\u0448\u0435\u0432\u043e \u0438 \u0438\u0437 \u041a\u0438\u0442\u0430\u044f" : "\u0431/\u0443 \u0436\u0435\u043b\u0435\u0437\u043e, \u0442\u043e\u0440\u0433 \u0443 \u043a\u0430\u043f\u043e\u0442\u0430";
        a3[2] = "";
        a3[3] = new StringBuffer("\u041e\u0431\u044a\u044f\u0432\u043b\u0435\u043d\u0438\u0439: ").append(a0.length).toString();
        a3[4] = new StringBuffer("\u0426\u0435\u043d\u044b: ").append(rpc.Browser.priceNote(i1)).toString();
        a3[5] = "";
        a3[6] = "OK \u2014 \u043b\u0438\u0441\u0442\u0430\u0442\u044c \u043b\u043e\u0442\u044b";
        a3[7] = "3 \u2014 \u043a\u0443\u043f\u0438\u0442\u044c \u0432\u044b\u0431\u0440\u0430\u043d\u043d\u043e\u0435";
        return a3;
    }
}
