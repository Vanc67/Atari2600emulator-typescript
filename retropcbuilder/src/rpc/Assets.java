package rpc;

public class Assets {
    final private static int[] BRAND;
    private static byte[][] png;
    private static int[] off;
    private static rpc.Images$Img[] mont;
    private static int[] montTick;
    private static int tick;
    final private static int CACHE = 24;
    private static String[] key;
    private static Object[] img;
    private static int[] cacheTick;
    public static boolean ready;
    private static rpc.Gfx gfx;
    
    static {
        int[] a = new int[8];
        a[0] = 14044736;
        a[1] = 4097234;
        a[2] = 15779390;
        a[3] = 5687436;
        a[4] = 11559132;
        a[5] = 15633466;
        a[6] = 7260396;
        a[7] = 14738152;
        BRAND = a;
        png = new byte[21][];
        off = new int[22];
        mont = new rpc.Images$Img[21];
        montTick = new int[21];
        key = new String[24];
        img = new Object[24];
        cacheTick = new int[24];
        ready = false;
    }
    
    public Assets() {
    }
    
    public static boolean load(rpc.Host a) {
        label1: {
            label0: try {
                byte[] a0 = rpc.Assets.readAll(a.open("assets.bin"));
                if (a0.length < 8) {
                    break label0;
                }
                int i = a0[0];
                if (i != 82) {
                    break label0;
                }
                int i0 = a0[1];
                if (i0 != 80) {
                    break label0;
                }
                int i1 = a0[2];
                if (i1 != 65) {
                    break label0;
                }
                int i2 = a0[4];
                int i3 = (i2 & 255) << 8;
                int i4 = a0[5];
                int i5 = i3 | i4 & 255;
                int i6 = 0;
                while(i6 <= i5 && i6 < off.length) {
                    off[i6] = rpc.Assets.readInt(a0, 6 + i6 * 4);
                    i6 = i6 + 1;
                }
                int i7 = 1;
                while(i7 <= i5 && i7 < png.length) {
                    int i8 = off[i7 - 1];
                    int i9 = off[i7];
                    if (i9 > i8 && i9 <= a0.length) {
                        byte[] a1 = new byte[i9 - i8];
                        System.arraycopy((Object)a0, i8, (Object)a1, 0, a1.length);
                        png[i7] = a1;
                    }
                    i7 = i7 + 1;
                }
                int i10 = 0;
                while(i10 < 24) {
                    key[i10] = null;
                    img[i10] = null;
                    i10 = i10 + 1;
                }
                int i11 = 0;
                while(i11 < mont.length) {
                    mont[i11] = null;
                    i11 = i11 + 1;
                }
                break label1;
            } catch(Throwable ignoredException) {
                ready = false;
                return false;
            }
            return false;
        }
        ready = true;
        return true;
    }
    
    private static byte[] readAll(java.io.InputStream a) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(32768);
        byte[] buf = new byte[4096];
        try {
            while (true) {
                int n = a.read(buf);
                if (n <= 0) {
                    break;
                }
                out.write(buf, 0, n);
            }
            a.close();
        } catch (java.io.IOException ignored) {
            return null;
        }
        return out.toByteArray();
    }
    
    private static int readInt(byte[] a, int i) {
        int i0 = a[i];
        int i1 = (i0 & 255) << 24;
        int i2 = a[i + 1];
        int i3 = i1 | (i2 & 255) << 16;
        int i4 = a[i + 2];
        int i5 = i3 | (i4 & 255) << 8;
        int i6 = a[i + 3];
        return i5 | i6 & 255;
    }
    
    public static void bind(rpc.Gfx a) {
        gfx = a;
    }
    
    public static int editions(int i) {
        return rpc.SpriteAtlas.editions(i);
    }
    
    public static int variants() {
        return 6;
    }
    
    private static rpc.Images$Img montage(int i) {
        if (i > 0 && i < png.length && png[i] != null) {
            rpc.Images$Img a = mont[i];
            if (a == null) {
                if (gfx == null) {
                    rpc.Images$Img a0 = null;
                    return a0;
                }
                a = gfx.decodePng(png[i]);
                int i0 = 0;
                int i1 = -1;
                int i2 = 2147483647;
                int i3 = 1;
                while(i3 < mont.length) {
                    if (mont[i3] != null) {
                        i0 = i0 + 1;
                        if ((montTick[i3]) < i2) {
                            i2 = montTick[i3];
                            i1 = i3;
                        }
                    }
                    i3 = i3 + 1;
                }
                if (i0 >= 4 && i1 > 0) {
                    mont[i1] = null;
                }
                mont[i] = a;
            }
            int[] a1 = montTick;
            int i4 = tick + 1;
            tick = i4;
            a1[i] = i4;
            return a;
        }
        rpc.Images$Img a2 = null;
        return a2;
    }
    
    public static int lookKind(int i) {
        if (i != 1 && i != 2 && i != 3 && i != 4 && i != 7 && i != 13 && i != 14 && i != 19 && i != 18) {
            if (i == 20) {
                return 2;
            }
            if (i != 12 && i != 15 && i != 16 && i != 17 && i != 10) {
                return 0;
            }
            return 2;
        }
        return 1;
    }
    
    public static int variantOf(rpc.Part a) {
        int i = (a.index * 7 + a.year * 31 + a.name.hashCode()) * -1640531535;
        return ((i ^ i >>> 15) % 12 + 12) % 12;
    }
    
    public static int accentOf(rpc.Part a) {
        int i = (a.index * 7 + a.name.length() + a.year) % BRAND.length;
        int[] a0 = BRAND;
        if (i < 0) {
            i = i + BRAND.length;
        }
        return a0[i];
    }
    
    public static void drawEdition(rpc.Gfx a, int i, int i0, int i1, int i2, int i3, int i4) {
        rpc.Assets.drawCell(a, i, rpc.SpriteAtlas.edition(i, i0), 0, -1, 0, 0, i1, i2, i3, i4, new StringBuffer("e").append(i).append("_").append(i0).append("_").append(i3).append("x").append(i4).toString(), 0);
    }
    
    public static void drawPart(rpc.Gfx a, rpc.Part a0, int i, int i0, int i1, int i2) {
        if (a0 == null) {
            return;
        }
        int i3 = (a0.cat != 20) ? rpc.SpriteAtlas.edition(a0.cat, a0.year) : rpc.SpriteAtlas.prnEdition(a0.f[0], a0.year);
        int i4 = (a0.year >= 2000) ? 0 : (2000 - a0.year) * 3 / 2;
        if (i4 > 45) {
            i4 = 45;
        }
        int i5 = (100 - a0.cond) / 2;
        rpc.Assets.drawCell(a, a0.cat, i3, rpc.Assets.variantOf(a0), rpc.Assets.accentOf(a0), i4, i5, i, i0, i1, i2, new StringBuffer("p").append(a0.index).append("_").append(i1).append("x").append(i2).toString(), a0.index);
    }
    
    private static void drawCell(rpc.Gfx a, int i, int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7, int i8, String s, int i9) {
        rpc.Images$Img a0 = rpc.Assets.montage(i);
        if (a0 != null && i7 > 0 && i8 > 0) {
            int i10 = i0 * 32;
            if (i10 + 32 > a0.h) {
                i10 = Math.max(0, a0.h - 32);
            }
            int i11 = Math.min(48, a0.w);
            int i12 = Math.min(32, a0.h - i10);
            int[] a1 = rpc.Assets.fit(i11, i12, i7, i8);
            int i13 = a1[0];
            int i14 = a1[1];
            int i15 = i5 + (i7 - i13) / 2;
            int i16 = i6 + (i8 - i14) / 2;
            if (i13 * i14 > 8192) {
                int[] a2 = rpc.Images.scale(a0, 0, i10, i11, i12, i13, i14);
                rpc.Images.recolor(a2, i13, i14, i1, rpc.Assets.lookKind(i));
                if (i9 != 0) {
                    i1 = rpc.Images.detailSeed(i9 * 31 + i1);
                }
                rpc.Images.detail(a2, i13, i14, i1, rpc.Assets.lookKind(i));
                rpc.Images.age(a2, i13, i14, i3, i4);
                if (i2 >= 0) {
                    rpc.Assets.markAccent(a2, i13, i14, i2);
                }
                if (i9 != 0) {
                    rpc.Images.mark(a2, i13, i14, i9);
                }
                a.drawImage(a.newImage(a2, i13, i14), i15, i16, i13, i14);
            } else {
                Object a3 = rpc.Assets.cacheGet(s);
                if (a3 == null) {
                    int[] a4 = rpc.Images.scale(a0, 0, i10, i11, i12, i13, i14);
                    rpc.Images.recolor(a4, i13, i14, i1, rpc.Assets.lookKind(i));
                    if (i9 != 0) {
                        i1 = rpc.Images.detailSeed(i9 * 31 + i1);
                    }
                    rpc.Images.detail(a4, i13, i14, i1, rpc.Assets.lookKind(i));
                    rpc.Images.age(a4, i13, i14, i3, i4);
                    if (i2 >= 0) {
                        rpc.Assets.markAccent(a4, i13, i14, i2);
                    }
                    if (i9 != 0) {
                        rpc.Images.mark(a4, i13, i14, i9);
                    }
                    a3 = a.newImage(a4, i13, i14);
                    rpc.Assets.cachePut(s, a3);
                }
                a.drawImage(a3, i15, i16, i13, i14);
            }
            return;
        }
    }
    
    public static void drawStrip(rpc.Gfx a, int i, int i0, int i1, int i2, int i3) {
        Object a0 = null;
        rpc.Images$Img a1 = rpc.Assets.montage(i);
        if (a1 == null) {
            return;
        }
        int i4 = rpc.SpriteAtlas.editions(i);
        if (i4 < 1) {
            return;
        }
        String s = new StringBuffer("s").append(i).append("_").append(i2).append("x").append(i3).toString();
        Object a2 = rpc.Assets.cacheGet(s);
        if (a2 != null) {
            a0 = a;
        } else {
            int i5 = i2 / i4;
            if (i5 < 2) {
                i5 = 2;
            }
            int[] a3 = new int[i5 * i4 * i3];
            a0 = a;
            int i6 = 0;
            while(i6 < i4) {
                int[] a4 = rpc.Images.scale(a1, 0, i6 * 32, 48, 32, i5, i3);
                int i7 = i6 * i5;
                int i8 = 0;
                while(i8 < i3) {
                    int i9 = i8 * i5;
                    int i10 = i8 * (i5 * i4) + i7;
                    int i11 = 0;
                    while(i11 < i5) {
                        a3[i10 + i11] = a4[i9 + i11];
                        i11 = i11 + 1;
                    }
                    i8 = i8 + 1;
                }
                i6 = i6 + 1;
            }
            a2 = ((rpc.Gfx)a0).newImage(a3, i5 * i4, i3);
            rpc.Assets.cachePut(s, a2);
        }
        int i12 = i2 / i4;
        if (i12 < 2) {
            i12 = 2;
        }
        ((rpc.Gfx)a0).drawImage(a2, i0 + (i2 - i12 * i4) / 2, i1, i12 * i4, i3);
    }
    
    public static void drawVariants(rpc.Gfx a, int i, int i0, int i1, int i2, int i3, int i4) {
        Object a0 = null;
        rpc.Images$Img a1 = rpc.Assets.montage(i);
        if (a1 == null) {
            return;
        }
        int i5 = rpc.SpriteAtlas.edition(i, i0);
        String s = new StringBuffer("v").append(i).append("_").append(i5).append("_").append(i3).append("x").append(i4).toString();
        Object a2 = rpc.Assets.cacheGet(s);
        int i6 = i3 / 24;
        if (i6 < 2) {
            i6 = 2;
        }
        if (a2 != null) {
            a0 = a;
        } else {
            int[] a3 = new int[i6 * 24 * i4];
            a0 = a;
            int i7 = 0;
            while(i7 < 24) {
                int[] a4 = rpc.Images.scale(a1, 0, i5 * 32, 48, 32, i6, i4);
                rpc.Images.recolor(a4, i6, i4, i7, rpc.Assets.lookKind(i));
                int i8 = i7 * i6;
                int i9 = 0;
                while(i9 < i4) {
                    int i10 = i9 * i6;
                    int i11 = i9 * (i6 * 24) + i8;
                    int i12 = 0;
                    while(i12 < i6) {
                        a3[i11 + i12] = a4[i10 + i12];
                        i12 = i12 + 1;
                    }
                    i9 = i9 + 1;
                }
                i7 = i7 + 1;
            }
            a2 = ((rpc.Gfx)a0).newImage(a3, i6 * 24, i4);
            rpc.Assets.cachePut(s, a2);
        }
        ((rpc.Gfx)a0).drawImage(a2, i1 + (i3 - i6 * 24) / 2, i2, i6 * 24, i4);
    }
    
    private static int[] fit(int i, int i0, int i1, int i2) {
        if ((long)i * (long)i2 <= (long)i0 * (long)i1) {
            i1 = (int)((long)i * (long)i2 / (long)i0);
        } else {
            i2 = (int)((long)i0 * (long)i1 / (long)i);
        }
        if (i1 < 1) {
            i1 = 1;
        }
        if (i2 < 1) {
            i2 = 1;
        }
        int[] a = new int[2];
        a[0] = i1;
        a[1] = i2;
        return a;
    }
    
    private static void markAccent(int[] a, int i, int i0, int i1) {
        if (i >= 5 && i0 >= 4) {
            int i2 = (i < 40) ? 2 : 3;
            int i3 = -16777216 | i1;
            int i4 = -16777216 | i1 & 8355711;
            int i5 = i - 1 - i2;
            int i6 = 0;
            while(i6 < i2) {
                int i7 = i5 + i6;
                if (i7 >= 0 && i7 < i) {
                    a[1 * i + i7] = i3;
                }
                if (i7 >= 0 && i7 < i && 2 < i0) {
                    a[2 * i + i7] = i4;
                }
                i6 = i6 + 1;
            }
            int i8 = i0 - 3;
            if (i8 > 3) {
                int i9 = 0;
                while(i9 < i2) {
                    int i10 = i5 + i9;
                    if (i10 >= 0 && i10 < i) {
                        a[i8 * i + i10] = i3;
                    }
                    i9 = i9 + 1;
                }
            }
            return;
        }
    }
    
    private static Object cacheGet(String s) {
        int i = 0;
        while(i < 24) {
            if (key[i] != null && key[i].equals((Object)s)) {
                int[] a = cacheTick;
                int i0 = tick + 1;
                tick = i0;
                a[i] = i0;
                return img[i];
            }
            i = i + 1;
        }
        Object a0 = null;
        return a0;
    }
    
    private static void cachePut(String s, Object a) {
        int i = -1;
        int i0 = 2147483647;
        int i1 = 0;
        while(true) {
            if (i1 < 24) {
                if (key[i1] != null) {
                    if ((cacheTick[i1]) < i0) {
                        i0 = cacheTick[i1];
                        i = i1;
                    }
                    i1 = i1 + 1;
                    continue;
                }
                i = i1;
            }
            key[i] = s;
            img[i] = a;
            int[] a0 = cacheTick;
            int i2 = tick + 1;
            tick = i2;
            a0[i] = i2;
            return;
        }
    }
}
