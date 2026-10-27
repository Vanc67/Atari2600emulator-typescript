package rpc;

public class SpriteAtlas {
    final public static int BIG_W = 48;
    final public static int BIG_H = 32;
    final public static int SMALL_W = 24;
    final public static int SMALL_H = 16;
    final public static int FRAMES = 130;
    final public static int VARIANTS = 24;
    final private static int[][] YEARS;
    
    static {
        int[][] a = new int[25][];
        int[] a0 = new int[7];
        a0[0] = 1981;
        a0[1] = 1985;
        a0[2] = 1993;
        a0[3] = 1999;
        a0[4] = 2004;
        a0[5] = 2017;
        a0[6] = 2023;
        a[1] = a0;
        int[] a1 = new int[8];
        a1[0] = 1981;
        a1[1] = 1987;
        a1[2] = 1995;
        a1[3] = 1997;
        a1[4] = 2003;
        a1[5] = 2010;
        a1[6] = 2016;
        a1[7] = 2022;
        a[2] = a1;
        int[] a2 = new int[6];
        a2[0] = 1981;
        a2[1] = 1990;
        a2[2] = 1998;
        a2[3] = 2008;
        a2[4] = 2015;
        a2[5] = 2021;
        a[3] = a2;
        int[] a3 = new int[8];
        a3[0] = 1982;
        a3[1] = 1985;
        a3[2] = 1994;
        a3[3] = 1999;
        a3[4] = 2005;
        a3[5] = 2013;
        a3[6] = 2016;
        a3[7] = 2021;
        a[4] = a3;
        int[] a4 = new int[4];
        a4[0] = 1985;
        a4[1] = 1993;
        a4[2] = 2003;
        a4[3] = 2010;
        a[5] = a4;
        int[] a5 = new int[3];
        a5[0] = 1984;
        a5[1] = 1987;
        a5[2] = 1994;
        a[6] = a5;
        int[] a6 = new int[3];
        a6[0] = 2010;
        a6[1] = 2016;
        a6[2] = 2021;
        a[7] = a6;
        int[] a7 = new int[3];
        a7[0] = 1993;
        a7[1] = 2004;
        a7[2] = 2009;
        a[8] = a7;
        int[] a8 = new int[4];
        a8[0] = 1981;
        a8[1] = 1996;
        a8[2] = 2012;
        a8[3] = 2020;
        a[9] = a8;
        int[] a9 = new int[5];
        a9[0] = 1981;
        a9[1] = 1992;
        a9[2] = 2002;
        a9[3] = 2014;
        a9[4] = 2022;
        a[10] = a9;
        int[] a10 = new int[5];
        a10[0] = 1981;
        a10[1] = 1995;
        a10[2] = 2007;
        a10[3] = 2016;
        a10[4] = 2021;
        a[11] = a10;
        int[] a11 = new int[7];
        a11[0] = 1981;
        a11[1] = 1993;
        a11[2] = 2001;
        a11[3] = 2005;
        a11[4] = 2013;
        a11[5] = 2019;
        a11[6] = 2022;
        a[12] = a11;
        int[] a12 = new int[4];
        a12[0] = 1987;
        a12[1] = 1998;
        a12[2] = 2012;
        a12[3] = 2020;
        a[13] = a12;
        int[] a13 = new int[4];
        a13[0] = 1987;
        a13[1] = 2000;
        a13[2] = 2010;
        a13[3] = 2019;
        a[14] = a13;
        int[] a14 = new int[4];
        a14[0] = 1984;
        a14[1] = 2000;
        a14[2] = 2014;
        a14[3] = 2021;
        a[15] = a14;
        int[] a15 = new int[4];
        a15[0] = 1990;
        a15[1] = 2000;
        a15[2] = 2016;
        a15[3] = 2023;
        a[16] = a15;
        int[] a16 = new int[2];
        a16[0] = 1992;
        a16[1] = 2020;
        a[17] = a16;
        int[] a17 = new int[9];
        a17[0] = 1985;
        a17[1] = 1998;
        a17[2] = 1994;
        a17[3] = 2005;
        a17[4] = 2015;
        a17[5] = 1989;
        a17[6] = 1981;
        a17[7] = 1981;
        a17[8] = 2008;
        a[18] = a17;
        int[] a18 = new int[3];
        a18[0] = 1995;
        a18[1] = 2011;
        a18[2] = 2020;
        a[19] = a18;
        int[] a19 = new int[18];
        a19[0] = 1979;
        a19[1] = 2010;
        a19[2] = 1979;
        a19[3] = 2010;
        a19[4] = 1979;
        a19[5] = 2010;
        a19[6] = 1979;
        a19[7] = 2010;
        a19[8] = 1979;
        a19[9] = 2010;
        a19[10] = 1979;
        a19[11] = 2010;
        a19[12] = 1979;
        a19[13] = 2010;
        a19[14] = 1979;
        a19[15] = 2010;
        a19[16] = 1979;
        a19[17] = 2010;
        a[20] = a19;
        int[] a20 = new int[7];
        a20[0] = 1985;
        a20[1] = 1991;
        a20[2] = 1997;
        a20[3] = 2003;
        a20[4] = 2010;
        a20[5] = 2018;
        a20[6] = 2024;
        a[21] = a20;
        int[] a21 = new int[4];
        a21[0] = 2005;
        a21[1] = 2009;
        a21[2] = 2013;
        a21[3] = 2020;
        a[22] = a21;
        int[] a22 = new int[4];
        a22[0] = 1996;
        a22[1] = 2001;
        a22[2] = 2004;
        a22[3] = 2008;
        a[23] = a22;
        int[] a23 = new int[4];
        a23[0] = 1993;
        a23[1] = 2002;
        a23[2] = 2010;
        a23[3] = 2018;
        a[24] = a23;
        YEARS = a;
    }
    
    private SpriteAtlas() {
    }
    
    public static int editions(int i) {
        if (i > 0 && i < YEARS.length) {
            return YEARS[i].length;
        }
        return 0;
    }
    
    public static int edition(int i, int i0) {
        if (i > 0 && i < YEARS.length) {
            int[] a = YEARS[i];
            int i1 = 0;
            int i2 = 0;
            while(i2 < a.length) {
                if (a[i2] <= i0) {
                    i1 = i2;
                }
                i2 = i2 + 1;
            }
            return i1;
        }
        return 0;
    }
    
    public static int prnEdition(int i, int i0) {
        int i1 = i * 2 + ((i0 < 2010) ? 0 : 1);
        if (i1 >= rpc.SpriteAtlas.editions(20)) {
            i1 = 0;
        }
        return i1;
    }
}
