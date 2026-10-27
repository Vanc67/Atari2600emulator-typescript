package rpc;

public class Util {
    public Util() {
    }
    
    public static int isqrt(int i) {
        if (i <= 0) {
            return 0;
        }
        long j = 1L;
        while(j * j <= (long)i) {
            {
                j = j << 1;
            }
        }
        long j0 = j >> 1;
        while(j0 + 1L < j) {
            long j1 = j0 + j >> 1;
            if (j1 * j1 > (long)i) {
                j = j1;
            } else {
                j0 = j1;
            }
        }
        return (int)j0;
    }
    
    public static int[] splitInts(String s, char a) {
        int i = a;
        int i0 = 1;
        int i1 = 0;
        while(i1 < s.length()) {
            int i2 = s.charAt(i1);
            if (i2 == i) {
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        int[] a0 = new int[i0];
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while(true) {
            int i6 = s.length();
            int i7 = i3;
            if (i5 > i6) {
                int[] a1 = new int[i3];
                System.arraycopy((Object)a0, 0, (Object)a1, 0, i3);
                return a1;
            }
            int i8 = s.length();
            i3 = i7;
            label0: {
                label1: {
                    if (i5 == i8) {
                        break label1;
                    }
                    int i9 = s.charAt(i5);
                    i7 = i3;
                    if (i9 != i) {
                        break label0;
                    }
                }
                i3 = i7;
                if (i7 < i0) {
                    i3 = i7 + 1;
                    a0[i7] = rpc.Util.parseInt(s.substring(i4, i5));
                }
                i4 = i5 + 1;
            }
            i5 = i5 + 1;
        }
    }
    
    public static String[] split(String s, char a) {
        int i = a;
        int i0 = 1;
        int i1 = 0;
        while(i1 < s.length()) {
            int i2 = s.charAt(i1);
            if (i2 == i) {
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        String[] a0 = new String[i0];
        int i3 = 0;
        int i4 = 0;
        int i5 = 0;
        while(true) {
            int i6 = s.length();
            int i7 = i3;
            if (i5 <= i6) {
                int i8 = s.length();
                label0: {
                    label1: {
                        if (i5 == i8) {
                            break label1;
                        }
                        int i9 = s.charAt(i5);
                        i3 = i7;
                        if (i9 != i) {
                            break label0;
                        }
                    }
                    i3 = i7 + 1;
                    a0[i7] = s.substring(i4, i5);
                    i4 = i5 + 1;
                }
                i5 = i5 + 1;
            } else {
                if (i3 == i0) {
                    return a0;
                }
                String[] a1 = new String[i3];
                System.arraycopy((Object)a0, 0, (Object)a1, 0, i3);
                return a1;
            }
        }
    }
    
    public static int parseInt(String s) {
        int i = 0;
        int i0 = 0;
        int i1 = 0;
        label0: if (s.length() <= 0) {
            i = 0;
            i0 = 0;
            i1 = 1;
        } else {
            int i2 = s.charAt(0);
            label1: {
                if (i2 == 45) {
                    break label1;
                }
                int i3 = s.charAt(0);
                if (i3 != 43) {
                    i = 0;
                    i0 = 0;
                    i1 = 1;
                    break label0;
                }
            }
            int i4 = s.charAt(0);
            i1 = (i4 != 45) ? 1 : -1;
            i = 0;
            i0 = 1;
        }
        while(true) {
            if (i0 < s.length()) {
                int i5 = s.charAt(i0);
                if (i5 >= 48 && i5 <= 57) {
                    i = i * 10 + (i5 - 48);
                    i0 = i0 + 1;
                    continue;
                }
            }
            return i * i1;
        }
    }
    
    public static String money(int i) {
        boolean b = i < 0;
        if (b) {
            i = -i;
        }
        String s = new StringBuffer().append(i).toString();
        StringBuffer a = new StringBuffer();
        int i0 = s.length() - 1;
        int i1 = 0;
        while(i0 >= 0) {
            int i2 = s.charAt(i0);
            a.append((char)i2);
            i1 = i1 + 1;
            if (i1 % 3 == 0 && i0 > 0) {
                a.append((char)32);
            }
            i0 = i0 + -1;
        }
        a.reverse();
        return b ? new StringBuffer("-").append(a.toString()).toString() : a.toString();
    }
    
    public static String size(int i) {
        if (i < 1024) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(i))).append(" \u041a\u0411").toString();
        }
        if (i < 1048576) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(i / 1024))).append(" \u041c\u0411").toString();
        }
        if (i < 8388608) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(i / 1048576))).append(" \u0413\u0411").toString();
        }
        return new StringBuffer(String.valueOf((Object)String.valueOf(i / 1048576 / 1024))).append(" \u0422\u0411").toString();
    }
    
    public static String truncate(String s, int i) {
        if (s == null) {
            return "";
        }
        if (s.length() <= i) {
            return s;
        }
        return new StringBuffer(String.valueOf((Object)String.valueOf((Object)s.substring(0, i - 1)))).append("~").toString();
    }
    
    public static String pad(String s, int i) {
        StringBuffer a = new StringBuffer(s);
        while(a.length() < i) {
            a.append((char)32);
        }
        return a.toString();
    }
    
    public static String utf8(byte[] a, int i, int i0) {
        StringBuffer a0 = new StringBuffer(i0);
        int i1 = i + i0;
        while(i < i1) {
            int i2 = a[i];
            int i3 = i2 & 255;
            if (i3 >= 128) {
                if ((i3 & 224) == 192 && i + 1 < i1) {
                    int i4 = a[i + 1];
                    int i5 = (char)((i3 & 31) << 6 | i4 & 63);
                    a0.append((char)i5);
                    i = i + 2;
                    continue;
                }
                if ((i3 & 240) == 224 && i + 2 < i1) {
                    int i6 = a[i + 1];
                    int i7 = i6 & 63;
                    int i8 = a[i + 2];
                    int i9 = (char)((i3 & 15) << 12 | i7 << 6 | i8 & 63);
                    a0.append((char)i9);
                    i = i + 3;
                    continue;
                }
                if ((i3 & 248) == 240 && i + 3 < i1) {
                    int i10 = a[i + 1];
                    int i11 = i10 & 63;
                    int i12 = a[i + 2];
                    int i13 = i12 & 63;
                    int i14 = a[i + 3];
                    int i15 = ((i3 & 7) << 18 | i11 << 12 | i13 << 6 | i14 & 63) - 65536;
                    int i16 = (char)(55296 + (i15 >> 10));
                    a0.append((char)i16);
                    int i17 = (char)(56320 + (i15 & 1023));
                    a0.append((char)i17);
                    i = i + 4;
                    continue;
                }
                i = i + 1;
            } else {
                int i18 = (char)i3;
                a0.append((char)i18);
                i = i + 1;
            }
        }
        return a0.toString();
    }
    
    public static int bitCount(int i) {
        int n = 0;
        while (i != 0) {
            n = n + (i & 1);
            i = i >>> 1;
        }
        return n;
    }
    
    public static int clamp(int i, int i0, int i1) {
        if (i >= i0) {
            if (i > i1) {
                i = i1;
            }
        } else {
            i = i0;
        }
        return i;
    }
}
