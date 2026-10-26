package rpc;

public class Images {
    final public static int PALETTES = 12;
    final private static int[] PCB;
    final private static int[] PLASTIC;
    final private static int[] PHOSPHOR;
    
    static {
        int[] a = new int[12];
        a[1] = 1982072;
        a[2] = 1711136;
        a[3] = 7873058;
        a[4] = 1989240;
        a[5] = 2764342;
        a[6] = 3029806;
        a[7] = 1976888;
        a[8] = 5913106;
        a[9] = 936490;
        a[10] = 3811930;
        a[11] = 6314520;
        PCB = a;
        int[] a0 = new int[12];
        a0[1] = 4211788;
        a0[2] = 13422806;
        a0[3] = 7764610;
        a0[4] = 3817544;
        a0[5] = 5790820;
        a0[6] = 1710622;
        a0[7] = 15263976;
        a0[8] = 9181979;
        a0[9] = 1785740;
        a0[10] = 2911020;
        a0[11] = 14263838;
        PLASTIC = a0;
        int[] a1 = new int[12];
        a1[1] = 13145644;
        a1[2] = 3694752;
        a1[3] = 4624474;
        a1[4] = 2924724;
        a1[5] = 3817544;
        a1[6] = 4864526;
        a1[7] = 3815994;
        a1[8] = 928330;
        a1[9] = 2756154;
        a1[10] = 932410;
        a1[11] = 3811854;
        PHOSPHOR = a1;
    }
    
    public Images() {
    }
    
    public static void recolor(int[] a, int i, int i0, int i1, int i2) {
        if (i1 > 0 && i1 < PCB.length && i2 != 0) {
            boolean b = i2 == 1;
            boolean b0 = b ? PCB[i1] != 0 : PHOSPHOR[i1] != 0;
            boolean b1 = !b && PLASTIC[i1] != 0;
            int i3 = 0;
            while(i3 < a.length) {
                int i4 = a[i3];
                int i5 = i4 >>> 24;
                label1: if (i5 != 0) {
                    int i6 = i4 >> 16 & 255;
                    int i7 = i4 >> 8 & 255;
                    int i8 = i4 & 255;
                    int i9 = (i6 <= i7) ? (i7 <= i8) ? i8 : i7 : (i6 <= i8) ? 0 : 0;
                    int i10 = (i6 >= i7) ? (i7 >= i8) ? i8 : i7 : (i6 >= i8) ? 0 : 0;
                    int i11 = i6 + 12;
                    label0: {
                        if (i7 <= i11) {
                            break label0;
                        }
                        if (i7 <= i8 + 10) {
                            break label0;
                        }
                        a[i3] = rpc.Images.shade(b0 ? 1 : 0, i6, i7, i8, i5);
                        break label1;
                    }
                    if (b1 && i6 > 120 && i9 - i10 < 44) {
                        a[i3] = rpc.Images.shade(b1 ? 1 : 0, i6, i7, i8, i5);
                    }
                }
                i3 = i3 + 1;
            }
            return;
        }
    }
    
    private static int shade(int i, int i0, int i1, int i2, int i3) {
        int i4 = 110 + (i0 * 3 + i1 * 5 + i2 * 2) / 10 * 160 / 255;
        int i5 = (i >> 16 & 255) * i4 / 255;
        int i6 = (i >> 8 & 255) * i4 / 255;
        int i7 = (i & 255) * i4 / 255;
        if (i5 > 255) {
            i5 = 255;
        }
        if (i6 > 255) {
            i6 = 255;
        }
        if (i7 > 255) {
            i7 = 255;
        }
        return i3 << 24 | i5 << 16 | i6 << 8 | i7;
    }
    
    public static int markSig(int i) {
        int i0 = (i ^ i >>> 13) * -1640531535;
        int i1 = i0 ^ i0 >>> 15;
        return (i1 >>> 3) % 6 * 100000 + (i1 >>> 7) % 97 * 1000 + (i1 >>> 11) % 61 * 8 + (i1 >>> 5 & 63);
    }
    
    public static void mark(int[] a, int i, int i0, int i1) {
        if (i >= 8 && i0 >= 6 && i1 != 0) {
            int i2 = (i1 ^ i1 >>> 13) * -1640531535;
            int i3 = i2 ^ i2 >>> 15;
            int i4 = (i3 >>> 3) % 6;
            int i5 = 1 + (i3 >>> 7) % Math.max(1, i - 6);
            int i6 = 1 + (i3 >>> 11) % Math.max(1, i0 - 5);
            int i7 = -16777216 | (i3 >>> 5 & 63) << 18 | (i3 >>> 9 & 127) << 9 | i3 >>> 15 & 127;
            if (i4 != 0) {
                if (i4 != 1) {
                    if (i4 != 2) {
                        if (i4 != 3) {
                            if (i4 != 4) {
                                rpc.Images.fill(a, i, i0, i5, i6, i5 + 1, i6, -15724528);
                                rpc.Images.fill(a, i, i0, i5, i6 + 2, i5 + 1, i6 + 2, -15724528);
                            } else {
                                rpc.Images.fill(a, i, i0, 1, i6, i - 2, i6, i7);
                            }
                        } else {
                            rpc.Images.fill(a, i, i0, i5, i0 - 3, i5, i0 - 3, ((i3 >>> 12 & 1) != 0) ? -24544 : -13049800);
                        }
                    } else {
                        int i8 = ((i3 & 1) != 0) ? 2 : i - 3;
                        rpc.Images.fill(a, i, i0, i8, 2, i8, 2, -3618616);
                        rpc.Images.fill(a, i, i0, i8, 1, i8, 1, -9803158);
                    }
                } else {
                    int i9 = 0;
                    while(i9 < 3) {
                        rpc.Images.fill(a, i, i0, i5 + i9, i6, i5 + i9, i6 + 2, (i9 % 2 != 0) ? -1513240 : -14671840);
                        i9 = i9 + 1;
                    }
                }
            } else {
                rpc.Images.fill(a, i, i0, i5, i6, i5 + 2, i6 + 1, i7);
                rpc.Images.fill(a, i, i0, i5, i6, i5 + 2, i6, i7 | 2105376);
            }
            return;
        }
    }
    
    public static int detailSeed(int i) {
        int i0 = i * 1103515245 + 12345;
        int i1 = i0 ^ i0 >>> 16;
        if (i1 < 0) {
            i1 = -i1;
        }
        return 1 + i1 % 5;
    }
    
    public static void detail(int[] a, int i, int i0, int i1, int i2) {
        if (i1 > 0 && i >= 6 && i0 >= 5) {
            int i3 = i1 % 5;
            if (i2 != 2) {
                if (i3 != 1) {
                    if (i3 != 2) {
                        if (i3 != 3) {
                            if (i3 != 4) {
                                int i4 = 0;
                                while(i4 < i) {
                                    rpc.Images.fill(a, i, i0, i4, 0, i4, 0, (i4 * 2 >= i) ? (i4 * 3 >= i * 2) ? -9516966 : -11613984 : -2077094);
                                    i4 = i4 + 1;
                                }
                                rpc.Images.fill(a, i, i0, 2, i0 - 5, i / 3, i0 - 3, -1645864);
                            } else {
                                int i5 = (i < 20) ? 2 : 3;
                                int i6 = i / (i5 + 2);
                                int i7 = 0;
                                while(i7 < i5) {
                                    int i8 = 2 + i7 * (i6 + 1);
                                    rpc.Images.fill(a, i, i0, i8, i0 - 3, i8 + i6 - 1, i0 - 1, -14012874);
                                    rpc.Images.fill(a, i, i0, i8, i0 - 3, i8 + i6 - 1, i0 - 3, -2709444);
                                    i7 = i7 + 1;
                                }
                            }
                        } else {
                            rpc.Images.fill(a, i, i0, 0, 0, 1, i0 - 1, -3748138);
                            rpc.Images.fill(a, i, i0, i - 2, 0, i - 1, i0 - 1, -6643030);
                            int i9 = 3;
                            while(i9 < i - 3) {
                                rpc.Images.fill(a, i, i0, i9, i0 - 2, i9, i0 - 1, -2709444);
                                i9 = i9 + 2;
                            }
                        }
                    } else {
                        int i10 = (i0 >= 12) ? 5 : 3;
                        int i11 = i - i10 - 2;
                        int i12 = i0 / 2;
                        rpc.Images.disc(a, i, i0, i11, i12, i10, -12960186);
                        rpc.Images.disc(a, i, i0, i11, i12, i10 - 1, -14670806);
                        rpc.Images.disc(a, i, i0, i11, i12, 1, -4800826);
                        int i13 = 0;
                        while(i13 < 4) {
                            int i14 = 0;
                            int i15 = 0;
                            label5: {
                                label3: {
                                    label4: {
                                        if (i13 == 0) {
                                            break label4;
                                        }
                                        if (i13 != 2) {
                                            break label3;
                                        }
                                    }
                                    i14 = 0;
                                    break label5;
                                }
                                i14 = (i13 != 1) ? -1 : 1;
                            }
                            int i16 = i11 + i14 * (i10 - 2);
                            label2: {
                                label0: {
                                    label1: {
                                        if (i13 == 1) {
                                            break label1;
                                        }
                                        if (i13 != 3) {
                                            break label0;
                                        }
                                    }
                                    i15 = 0;
                                    break label2;
                                }
                                i15 = (i13 != 0) ? 1 : -1;
                            }
                            int i17 = i12 + i15 * (i10 - 2);
                            rpc.Images.fill(a, i, i0, i16, i17, i16, i17, -7695718);
                            i13 = i13 + 1;
                        }
                    }
                } else {
                    int i18 = i / 3;
                    int i19 = i - 2;
                    int i20 = i0 / 2;
                    rpc.Images.fill(a, i, i0, i18, 1, i19, i20, -4800826);
                    int i21 = i18 + 1;
                    while(i21 < i19) {
                        rpc.Images.vline(a, i, i0, i21, 1, i20, -8485234);
                        i21 = i21 + 2;
                    }
                }
                if (i2 == 2 && i3 == 5) {
                    int i22 = i0 - 6;
                    while(i22 < i0 - 2) {
                        rpc.Images.fill(a, i, i0, i - 6, i22, i - 2, i22, -15460838);
                        i22 = i22 + 2;
                    }
                }
                return;
            } else {
                if (i3 != 1) {
                    if (i3 != 2) {
                        if (i3 != 3) {
                            if (i3 != 4) {
                                if (i3 == 0 && i1 == 5) {
                                    int i23 = 0;
                                    while(i23 < i) {
                                        rpc.Images.fill(a, i, i0, i23, i0 - 1, i23, i0 - 1, (i23 * 2 >= i) ? (i23 * 3 >= i * 2) ? -9516966 : -11613984 : -2077094);
                                        i23 = i23 + 1;
                                    }
                                }
                            } else {
                                rpc.Images.fill(a, i, i0, i / 4, i0 - 3, i * 3 / 4, i0 - 2, -14012874);
                            }
                        } else {
                            rpc.Images.fill(a, i, i0, 1, 1, 1, 1, -6643030);
                            rpc.Images.fill(a, i, i0, i - 2, 1, i - 2, 1, -6643030);
                            rpc.Images.fill(a, i, i0, 1, i0 - 2, 1, i0 - 2, -6643030);
                            rpc.Images.fill(a, i, i0, i - 2, i0 - 2, i - 2, i0 - 2, -6643030);
                        }
                    } else {
                        rpc.Images.fill(a, i, i0, i - 4, i0 - 4, i - 3, i0 - 3, -9516966);
                    }
                } else {
                    rpc.Images.fill(a, i, i0, 2, i0 - 6, i / 3, i0 - 4, -1645864);
                    rpc.Images.fill(a, i, i0, 3, i0 - 5, i / 4, i0 - 5, -9803150);
                }
                return;
            }
        }
    }
    
    public static void age(int[] a, int i, int i0, int i1, int i2) {
        if (i1 <= 0 && i2 <= 0) {
            return;
        }
        int i3 = 0;
        while(i3 < a.length) {
            int i4 = a[i3];
            int i5 = i4 >>> 24;
            if (i5 != 0) {
                int i6 = i4 >> 16 & 255;
                int i7 = i4 >> 8 & 255;
                int i8 = i4 & 255;
                if (i1 > 0 && (i6 * 3 + i7 * 5 + i8 * 2) / 10 > 120) {
                    i6 = i6 + i1 / 4;
                    i7 = i7 + i1 / 6;
                    i8 = i8 - i1 / 5;
                }
                if (i2 > 0) {
                    i6 = i6 * (100 - i2) / 100 + 12 * i2 / 100;
                    i7 = i7 * (100 - i2) / 100 + 12 * i2 / 100;
                    i8 = i8 * (100 - i2) / 100 + 14 * i2 / 100;
                }
                if (i6 > 255) {
                    i6 = 255;
                }
                if (i7 > 255) {
                    i7 = 255;
                }
                if (i8 > 255) {
                    i8 = 255;
                }
                if (i6 < 0) {
                    i6 = 0;
                }
                if (i7 < 0) {
                    i7 = 0;
                }
                if (i8 < 0) {
                    i8 = 0;
                }
                a[i3] = i5 << 24 | i6 << 16 | i7 << 8 | i8;
            }
            i3 = i3 + 1;
        }
    }
    
    private static void fill(int[] a, int i, int i0, int i1, int i2, int i3, int i4, int i5) {
        int i6 = 0;
        int i7 = 0;
        if (i1 <= i3) {
            i6 = i1;
            i1 = i3;
        } else {
            i6 = i3;
        }
        if (i2 <= i4) {
            i7 = i2;
            i2 = i4;
        } else {
            i7 = i4;
        }
        if (i6 < 0) {
            i6 = 0;
        }
        if (i7 < 0) {
            i7 = 0;
        }
        if (i1 >= i) {
            i1 = i - 1;
        }
        if (i2 >= i0) {
            i2 = i0 - 1;
        }
        while(i7 <= i2) {
            int i8 = i7 * i;
            int i9 = i6;
            while(i9 <= i1) {
                a[i8 + i9] = i5;
                i9 = i9 + 1;
            }
            i7 = i7 + 1;
        }
    }
    
    private static void vline(int[] a, int i, int i0, int i1, int i2, int i3, int i4) {
        rpc.Images.fill(a, i, i0, i1, i2, i1, i3, i4);
    }
    
    private static void disc(int[] a, int i, int i0, int i1, int i2, int i3, int i4) {
        int i5 = i3 * i3;
        int i6 = -i3;
        while(i6 <= i3) {
            int i7 = -i3;
            while(i7 <= i3) {
                if (i7 * i7 + i6 * i6 <= i5) {
                    int i8 = i1 + i7;
                    int i9 = i2 + i6;
                    if (i8 >= 0 && i9 >= 0 && i8 < i && i9 < i0) {
                        a[i9 * i + i8] = i4;
                    }
                }
                i7 = i7 + 1;
            }
            i6 = i6 + 1;
        }
    }
    
    public static int[] scale(rpc.Images$Img a, int i, int i0, int i1, int i2, int i3, int i4) {
        int[] a0 = new int[i3 * i4];
        if (i3 > 0 && i4 > 0 && i1 > 0 && i2 > 0) {
            int i5 = 0;
            while(i5 < i4) {
                int i6 = i0 + i5 * i2 / i4;
                if (i6 >= a.h) {
                    i6 = a.h - 1;
                }
                int i7 = i6 * a.w;
                int i8 = i5 * i3;
                int i9 = 0;
                while(i9 < i3) {
                    int i10 = i + i9 * i1 / i3;
                    if (i10 >= a.w) {
                        i10 = a.w - 1;
                    }
                    a0[i8 + i9] = a.argb[i7 + i10];
                    i9 = i9 + 1;
                }
                i5 = i5 + 1;
            }
            return a0;
        }
        return a0;
    }
}
