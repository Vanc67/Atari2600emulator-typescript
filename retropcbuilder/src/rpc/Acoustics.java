package rpc;

public class Acoustics {
    public Acoustics() {
    }
    
    public static int db(rpc.Part a) {
        int i = 0;
        if (a == null) {
            return 0;
        }
        label0: if (a.cat != 11) {
            if (a.cat != 9) {
                if (a.cat != 2) {
                    if (a.cat != 5) {
                        if (a.cat != 7) {
                            if (a.cat != 10) {
                                if (a.cat != 1) {
                                    int i0 = a.cat;
                                    label1: {
                                        if (i0 == 8) {
                                            break label1;
                                        }
                                        if (a.cat != 6) {
                                            i = 0;
                                            break label0;
                                        }
                                    }
                                    i = 0;
                                } else {
                                    i = 0;
                                }
                            } else {
                                i = (a.year >= 1995) ? 3 : 6;
                            }
                        } else {
                            i = 0;
                        }
                    } else {
                        i = ((a.f[2] < 7200) ? 27 : 32) + a.dust / 10;
                    }
                } else {
                    i = 18 + (((a.f[4]) <= 250) ? 2 : 3) * 4 + a.f[4] / 60 + a.dust / 10;
                }
            } else {
                i = 22 + a.f[0] / 120 + a.dust / 9;
            }
        } else {
            int i1 = a.f[1];
            i = ((i1 < 140) ? (i1 < 120) ? (i1 < 92) ? 33 : 28 : 24 : 20) + a.dust / 8 + (100 - a.cond) / 10;
        }
        if (i < 0) {
            i = 0;
        }
        return i;
    }
    
    public static int total(rpc.Part[] a) {
        if (a == null) {
            return 0;
        }
        long j = 0L;
        int i = 0;
        int i0 = 0;
        while(true) {
            int i1 = a.length;
            int i2 = i;
            if (i0 < i1) {
                i = rpc.Acoustics.db(a[i0]);
                if (i <= 0) {
                    i = i2;
                } else {
                    j = j + (long)i * (long)i;
                    if (i <= i2) {
                        i = i2;
                    }
                }
                i0 = i0 + 1;
            } else {
                int i3 = rpc.Acoustics.isqrt(j);
                if (i3 < i) {
                    i3 = i;
                }
                return i3;
            }
        }
    }
    
    public static String word(int i) {
        if (i <= 0) {
            return "\u0442\u0438\u0448\u0438\u043d\u0430";
        }
        if (i < 28) {
            return "\u0442\u0438\u0445\u043e";
        }
        if (i < 34) {
            return "\u0441\u043b\u044b\u0448\u043d\u043e";
        }
        if (i >= 42) {
            return "\u043a\u0430\u043a \u043f\u044b\u043b\u0435\u0441\u043e\u0441";
        }
        return "\u0433\u0443\u0434\u0438\u0442";
    }
    
    public static int fanRpm(rpc.Part[] a) {
        rpc.Part a0 = null;
        int i = 0;
        while(i < a.length) {
            if (a[i] != null && a[i].cat == 11) {
                a0 = a[i];
            }
            i = i + 1;
        }
        if (a0 == null) {
            return 0;
        }
        int i0 = 900 + a0.dust * 8 + (100 - a0.cond) * 4;
        if (i0 > 3200) {
            i0 = 3200;
        }
        return i0;
    }
    
    public static int dustTotal(rpc.Part[] a) {
        return rpc.Diag.dustOf(a);
    }
    
    private static int isqrt(long j) {
        if (j <= 0L) {
            return 0;
        }
        long j0 = (j + 1L) / 2L;
        long j1 = j;
        while(j0 < j1) {
            long j2 = (j0 + j / j0) / 2L;
            j1 = j0;
            j0 = j2;
        }
        return (int)j1;
    }
}
