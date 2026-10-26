package rpc;

public class Diag {
    final public static int F_NONE = 0;
    final public static int F_CPU = 1;
    final public static int F_RAM = 2;
    final public static int F_GPU = 3;
    final public static int F_PSU = 4;
    final public static int F_DUST = 5;
    final public static int F_DISK = 6;
    final public static int F_BOARD = 7;
    final public static int F_CAPS = 8;
    final public static int F_CMOS = 9;
    
    public Diag() {
    }
    
    public static String beepText(int i) {
        if (i == 0) {
            return "1 \u043a\u043e\u0440\u043e\u0442\u043a\u0438\u0439 \u2014 \u043e\u043a";
        }
        if (i == 1) {
            return "5 \u043a\u043e\u0440\u043e\u0442\u043a\u0438\u0445 \u2014 \u0426\u041f";
        }
        if (i == 2) {
            return "3 \u043a\u043e\u0440\u043e\u0442\u043a\u0438\u0445 \u2014 \u041e\u0417\u0423";
        }
        if (i == 3) {
            return "1 \u0434\u043b\u0438\u043d\u043d\u044b\u0439 + 1 \u043a\u043e\u0440\u043e\u0442\u043a\u0438\u0439 \u2014 \u0432\u0438\u0434\u0435\u043e";
        }
        if (i == 4) {
            return "\u0434\u043b\u0438\u043d\u043d\u044b\u0439 \u0433\u0443\u0434\u043e\u043a \u2014 \u0411\u041f";
        }
        if (i == 5) {
            return "\u043f\u0438\u0441\u043a \u2014 \u043f\u0435\u0440\u0435\u0433\u0440\u0435\u0432";
        }
        if (i == 6) {
            return "\u0449\u0435\u043b\u0447\u043a\u0438 \u2014 \u0434\u0438\u0441\u043a";
        }
        if (i == 8) {
            return "2 \u043a\u043e\u0440\u043e\u0442\u043a\u0438\u0445 \u2014 \u0451\u043c\u043a\u043e\u0441\u0442\u0438";
        }
        if (i != 9) {
            return "1 \u0434\u043b\u0438\u043d\u043d\u044b\u0439 \u2014 \u043f\u043b\u0430\u0442\u0430";
        }
        return "1 \u0434\u043b\u0438\u043d\u043d\u044b\u0439 + 3 \u2014 \u0431\u0430\u0442\u0430\u0440\u0435\u0439\u043a\u0430 CMOS";
    }
    
    public static rpc.Diag$Fault[] check(rpc.Part[] a, int i) {
        int i0 = 0;
        int i1 = 0;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        int i6 = 0;
        int i7 = 0;
        rpc.Diag$Fault[] a0 = new rpc.Diag$Fault[12];
        rpc.Part a1 = a[0];
        rpc.Part a2 = a[1];
        rpc.Part a3 = a[6];
        rpc.Part a4 = a[10];
        rpc.Part a5 = a[12];
        boolean b = false;
        int i8 = 0;
        while(i8 < 4) {
            if (a[2 + i8] != null) {
                b = true;
            }
            i8 = i8 + 1;
        }
        boolean b0 = false;
        int i9 = 0;
        while(i9 < 3) {
            if (a[7 + i9] != null) {
                b0 = true;
            }
            i9 = i9 + 1;
        }
        if (a1 != null) {
            if (a1.cond >= 30) {
                i0 = 0;
            } else {
                a0[0] = new rpc.Diag$Fault(1, 0, new StringBuffer("\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 \u0441\u044b\u043f\u0435\u0442\u0441\u044f (").append(a1.cond).append("%)").toString(), "\u041f\u0435\u0440\u0435\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435, \u043f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0441\u043e\u043a\u0435\u0442, \u0437\u0430\u043c\u0435\u043d\u0438\u0442\u0435");
                i0 = 1;
            }
        } else {
            a0[0] = new rpc.Diag$Fault(1, 0, "\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u0430 \u043d\u0435\u0442", "\u041f\u043e\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 \u0432 \u0441\u043e\u043a\u0435\u0442");
            i0 = 1;
        }
        if (a2 != null) {
            i1 = i0;
        } else {
            i1 = i0 + 1;
            a0[i0] = new rpc.Diag$Fault(7, 1, "\u041f\u043b\u0430\u0442\u044b \u043d\u0435\u0442", "\u0411\u0435\u0437 \u043c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u043e\u0439 \u043f\u043b\u0430\u0442\u044b \u043d\u0435 \u0441\u0442\u0430\u0440\u0442\u0443\u0435\u0442");
        }
        if (b) {
            int i10 = 0;
            while(true) {
                if (i10 < 4) {
                    rpc.Part a6 = a[2 + i10];
                    label4: {
                        label5: {
                            if (a6 == null) {
                                break label5;
                            }
                            if (a6.cond < 45) {
                                break label4;
                            }
                            if (a6.dust > 70) {
                                break label4;
                            }
                        }
                        i10 = i10 + 1;
                        continue;
                    }
                    i2 = i1 + 1;
                    a0[i1] = new rpc.Diag$Fault(2, 2 + i10, new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c ").append(a6.cond).append("% / \u043f\u044b\u043b\u044c ").append(a6.dust).append("%").toString(), "\u041f\u0440\u043e\u0442\u0435\u0440\u0435\u0442\u044c \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b \u043b\u0430\u0441\u0442\u0438\u043a\u043e\u043c");
                    break;
                } else {
                    i2 = i1;
                    break;
                }
            }
        } else {
            i2 = i1 + 1;
            a0[i1] = new rpc.Diag$Fault(2, 2, "\u041e\u0417\u0423 \u043d\u0435 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e", "\u0412\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u043c\u043e\u0434\u0443\u043b\u044c \u043f\u0430\u043c\u044f\u0442\u0438");
        }
        label2: if (a3 != null) {
            int i11 = a3.cond;
            label3: {
                if (i11 < 45) {
                    break label3;
                }
                if (a3.dust <= 75) {
                    i3 = i2;
                    break label2;
                }
            }
            i3 = i2 + 1;
            a0[i2] = new rpc.Diag$Fault(3, 6, new StringBuffer("\u0412\u0438\u0434\u0435\u043e ").append(a3.cond).append("% / \u043f\u044b\u043b\u044c ").append(a3.dust).append("%").toString(), "\u041f\u0440\u043e\u0434\u0443\u0442\u044c \u0440\u0430\u0434\u0438\u0430\u0442\u043e\u0440, \u043f\u0435\u0440\u0435\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043a\u0430\u0440\u0442\u0443");
        } else {
            i3 = i2 + 1;
            a0[i2] = new rpc.Diag$Fault(3, 6, "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u044b \u043d\u0435\u0442", "\u041d\u0443\u0436\u043d\u0430 \u043a\u0430\u0440\u0442\u0430 \u0438\u043b\u0438 \u0432\u0441\u0442\u0440\u043e\u0439\u043a\u0430");
        }
        if (a4 != null) {
            if ((a4.f[0]) >= i) {
                i4 = i3;
            } else {
                i4 = i3 + 1;
                a0[i3] = new rpc.Diag$Fault(4, 10, new StringBuffer("\u0411\u041f ").append(a4.f[0]).append(" \u0412\u0442, \u0430 \u043d\u0443\u0436\u043d\u043e ").append(i).append(" \u0412\u0442").toString(), "\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u0435 \u043d\u0430 \u0431\u043e\u043b\u0435\u0435 \u043c\u043e\u0449\u043d\u044b\u0439");
            }
        } else {
            i4 = i3 + 1;
            a0[i3] = new rpc.Diag$Fault(4, 10, "\u041d\u0435\u0442 \u0431\u043b\u043e\u043a\u0430 \u043f\u0438\u0442\u0430\u043d\u0438\u044f", "\u041f\u043e\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u0411\u041f");
        }
        if (a5 != null) {
            if (a5.dust <= 55) {
                i5 = i4;
            } else {
                i5 = i4 + 1;
                a0[i4] = new rpc.Diag$Fault(5, 12, new StringBuffer("\u041a\u0443\u043b\u0435\u0440 \u0437\u0430\u0431\u0438\u0442 \u043f\u044b\u043b\u044c\u044e (").append(a5.dust).append("%)").toString(), "\u041f\u0440\u043e\u0434\u0443\u0442\u044c \u0440\u0430\u0434\u0438\u0430\u0442\u043e\u0440");
            }
        } else {
            i5 = i4 + 1;
            a0[i4] = new rpc.Diag$Fault(5, 12, "\u041d\u0435\u0442 \u043e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u044f CPU", "\u041f\u043e\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u043a\u0443\u043b\u0435\u0440");
        }
        if (b0) {
            int i12 = 0;
            while(i12 < 3) {
                rpc.Part a7 = a[7 + i12];
                {
                    label0: {
                        label1: {
                            if (a7 == null) {
                                break label1;
                            }
                            if (a7.cond < 40) {
                                break label0;
                            }
                        }
                        i12 = i12 + 1;
                        continue;
                    }
                    int i13 = i5 + 1;
                    a0[i5] = new rpc.Diag$Fault(6, 7 + i12, new StringBuffer("\u0414\u0438\u0441\u043a ").append(a7.cond).append("% \u2014 \u0449\u0451\u043b\u043a\u0430\u0435\u0442").toString(), "\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u044c \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044c");
                    i5 = i13;
                    break;
                }
            }
        }
        if (a2 == null) {
            i6 = i5;
        } else if (rpc.Diag.capsLeft(a2) <= 0) {
            i6 = i5;
        } else {
            i6 = i5 + 1;
            a0[i5] = new rpc.Diag$Fault(8, 1, new StringBuffer("\u0412\u0437\u0434\u0443\u043b\u0438\u0441\u044c \u043a\u043e\u043d\u0434\u0435\u043d\u0441\u0430\u0442\u043e\u0440\u044b (").append(rpc.Diag.capsLeft(a2)).append(" \u0448\u0442.)").toString(), "\u041f\u0435\u0440\u0435\u043f\u0430\u044f\u0442\u044c \u0451\u043c\u043a\u043e\u0441\u0442\u0438: \u043f\u0430\u044f\u043b\u044c\u043d\u0438\u043a \u0438 \u0434\u043e\u043d\u043e\u0440");
        }
        if (a4 == null) {
            i7 = i6;
        } else if (a4.year >= 2006) {
            i7 = i6;
        } else if (a4.cond >= 45) {
            i7 = i6;
        } else {
            i7 = i6 + 1;
            a0[i6] = new rpc.Diag$Fault(8, 10, new StringBuffer("\u0411\u041f: \u0451\u043c\u043a\u043e\u0441\u0442\u0438 \u0432\u044b\u0441\u043e\u0445\u043b\u0438 (").append(a4.cond).append("%)").toString(), "\u041f\u0435\u0440\u0435\u043f\u0430\u044f\u0442\u044c \u0451\u043c\u043a\u043e\u0441\u0442\u0438 \u0438\u043b\u0438 \u0441\u043c\u0435\u043d\u0438\u0442\u044c \u0411\u041f");
        }
        rpc.Diag$Fault[] a8 = new rpc.Diag$Fault[i7];
        System.arraycopy((Object)a0, 0, (Object)a8, 0, i7);
        return a8;
    }
    
    public static int capsLeft(rpc.Part a) {
        if (a == null) {
            return 0;
        }
        if (a.cat != 3 && a.cat != 9) {
            return 0;
        }
        int i = (2026 - a.year) * 2 + (100 - a.cond);
        if (a.year >= 1999 && a.year <= 2007) {
            i = i + 25;
        }
        int i0 = i + a.dust / 5;
        if (i0 < 55) {
            return 0;
        }
        int i1 = (i0 - 50) / 18;
        if (i1 > 6) {
            i1 = 6;
        }
        return i1;
    }
    
    public static int recap(rpc.Part a) {
        int i = a.cond;
        a.cond = (a.cond + 22 <= 100) ? a.cond + 22 : 100;
        return a.cond - i;
    }
    
    public static int stability(rpc.Part[] a, int i) {
        int i0 = 100;
        int i1 = 0;
        while(i1 < 20) {
            rpc.Part a0 = a[i1];
            if (a0 != null) {
                int i2 = rpc.Diag.capsLeft(a0);
                int i3 = a0.cond;
                int i4 = i0;
                if (i3 < 90) {
                    i4 = i0 - (90 - a0.cond) / 3;
                }
                if (i2 > 0) {
                    i4 = i4 - i2 * 6;
                }
                int i5 = a0.dust;
                i0 = i4;
                if (i5 > 40) {
                    i0 = i4 - (a0.dust - 40) / 4;
                }
            }
            i1 = i1 + 1;
        }
        int i6 = i0 - i * 18;
        if (i6 < 5) {
            i6 = 5;
        }
        if (i6 > 100) {
            i6 = 100;
        }
        return i6;
    }
    
    public static int eraser(rpc.Part a) {
        int i = a.cond;
        a.cond = (a.cond + 15 <= 100) ? a.cond + 15 : 100;
        a.dust = (a.dust <= 10) ? 0 : a.dust - 10;
        return a.cond - i;
    }
    
    public static int blow(rpc.Part a) {
        int i = a.dust;
        a.dust = (a.dust <= 45) ? 0 : a.dust - 45;
        if (a.dust == 0 && a.cond < 95) {
            a.cond = a.cond + 3;
        }
        return i - a.dust;
    }
    
    public static int reseat(rpc.Part a) {
        int i = a.cond;
        a.cond = (a.cond + 8 <= 100) ? a.cond + 8 : 100;
        return a.cond - i;
    }
    
    public static int dustOf(rpc.Part[] a) {
        int i = 0;
        int i0 = 0;
        int i1 = 0;
        while(i1 < 20) {
            if (a[i1] != null) {
                i = i + a[i1].dust;
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        return (i0 != 0) ? i / i0 : 0;
    }
}
