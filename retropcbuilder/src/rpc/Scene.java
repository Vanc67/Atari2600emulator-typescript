package rpc;

public class Scene {
    final private static int WALL = 1713202;
    final private static int WALL2 = 2240580;
    final private static int WINDOW = 2905720;
    final private static int SKY1 = 1982046;
    final private static int SKY2 = 2775678;
    final private static int DESK_TOP = 7031342;
    final private static int DESK_EDGE = 4862496;
    final private static int DESK_DARK = 3351578;
    final private static int CASE_BODY = 13156524;
    final private static int CASE_DARK = 9209462;
    final private static int CASE_EDGE = 6183502;
    final private static int SCREEN_BEZEL = 11840664;
    final private static int SCREEN_DARK = 727570;
    final private static int GLASS = 1323038;
    final private static int KEY_TOP = 12103840;
    final private static int KEY_SIDE = 9077880;
    final private static int METAL = 10134186;
    
    public Scene() {
    }
    
    public static void room(rpc.Gfx a, int i, int i0, int i1, int i2) {
        a.setColor(1713202);
        a.fillRect(i, i0, i1, i2);
        a.setColor(2240580);
        Object a0 = a;
        int i3 = 0;
        while(i3 < i1) {
            ((rpc.Gfx)a0).fillRect(i + i3, i0, 8, i2);
            i3 = i3 + 16;
        }
        int i4 = Math.min(56, i1 * 32 / 100);
        int i5 = Math.min(38, i2 * 38 / 100);
        int i6 = i + i1 * 38 / 100;
        int i7 = i0 + 6;
        ((rpc.Gfx)a0).setColor(924196);
        ((rpc.Gfx)a0).fillRect(i6 - 4, i7 - 4, i4 + 8, i5 + 8);
        ((rpc.Gfx)a0).setColor(1982046);
        ((rpc.Gfx)a0).fillRect(i6, i7, i4, i5);
        ((rpc.Gfx)a0).setColor(2775678);
        ((rpc.Gfx)a0).fillRect(i6, i7 + i5 / 2, i4, i5 - i5 / 2);
        ((rpc.Gfx)a0).setColor(1454666);
        ((rpc.Gfx)a0).fillRect(i6 + 4, i7 + i5 / 2, 12, i5 / 2);
        ((rpc.Gfx)a0).fillRect(i6 + 20, i7 + i5 / 2 + 8, 10, i5 / 2 - 8);
        ((rpc.Gfx)a0).fillRect(i6 + 34, i7 + i5 / 2 + 3, 14, i5 / 2 - 3);
        ((rpc.Gfx)a0).setColor(5918270);
        ((rpc.Gfx)a0).drawRect(i6 - 1, i7 - 1, i4 + 2, i5 + 2);
        ((rpc.Gfx)a0).drawLine(i6 + i4 / 2, i7, i6 + i4 / 2, i7 + i5);
        ((rpc.Gfx)a0).drawLine(i6, i7 + i5 / 2, i6 + i4, i7 + i5 / 2);
        ((rpc.Gfx)a0).setColor(3031638);
        ((rpc.Gfx)a0).fillRect(i + 4, i0 + i2 * 30 / 100, 12, 10);
        ((rpc.Gfx)a0).fillRect(i + 18, i0 + i2 * 32 / 100, 14, 8);
        int i8 = i0 + i2 * 55 / 100;
        ((rpc.Gfx)a0).setColor(7031342);
        ((rpc.Gfx)a0).fillRect(i, i8, i1, 8);
        ((rpc.Gfx)a0).setColor(8280632);
        ((rpc.Gfx)a0).fillRect(i, i8, i1, 2);
        ((rpc.Gfx)a0).setColor(4862496);
        ((rpc.Gfx)a0).fillRect(i, i8 + 8, i1, 5);
        ((rpc.Gfx)a0).setColor(3351578);
        ((rpc.Gfx)a0).fillRect(i, i8 + 13, i1, i0 + i2 - i8 - 13);
    }
    
    public static int deskY(int i, int i0) {
        return i + i0 * 55 / 100;
    }
    
    public static int[] monitor(rpc.Gfx a, int i, int i0, int i1, int i2, boolean b) {
        int i3 = b ? 4 : 6;
        a.setColor(11840664);
        a.fillRect(i, i0, i1, i2);
        a.setColor(b ? 2764342 : 6183502);
        a.drawRect(i, i0, i1, i2);
        int i4 = i + i3;
        int i5 = i0 + i3;
        int i6 = i1 - i3 * 2;
        int i7 = i2 - i3 * 2 - 4;
        a.setColor(727570);
        a.fillRect(i4, i5, i6, i7);
        if (b) {
            a.setColor(3817030);
            a.fillRect(i + i1 / 2 - 3, i0 + i2, 6, 12);
            a.fillRect(i + i1 / 2 - 16, i0 + i2 + 12, 32, 4);
        } else {
            a.setColor(9209462);
            a.fillRect(i + i1 / 2 - 10, i0 + i2, 20, 8);
            a.fillRect(i + 8, i0 + i2 + 8, i1 - 16, 5);
        }
        a.setColor(7260250);
        a.fillRect(i + i1 - i3 - 4, i0 + i2 - i3, 3, 3);
        int[] a0 = new int[4];
        a0[0] = i4;
        a0[1] = i5;
        a0[2] = i6;
        a0[3] = i7;
        return a0;
    }
    
    public static void screenGlow(rpc.Gfx a, int i, int i0, int i1, int i2, boolean b) {
        a.setColor(1323038);
        a.fillRect(i, i0, i1, i2);
        if (!b) {
            return;
        }
        a.setColor(1853996);
        int i3 = i0 + 1;
        Object a0 = a;
        while(i3 < i0 + i2) {
            ((rpc.Gfx)a0).fillRect(i, i3, i1, 1);
            i3 = i3 + 3;
        }
        ((rpc.Gfx)a0).setColor(3041860);
        ((rpc.Gfx)a0).fillRect(i, i0, i1, 1);
    }
    
    public static void caseUnit(rpc.Gfx a, int i, int i0, int i1, int i2, int i3, boolean b) {
        Object a0 = null;
        a.setColor(13156524);
        a.fillRect(i, i0, i1, i2);
        a.setColor(6183502);
        a.drawRect(i, i0, i1, i2);
        a.setColor(9209462);
        a.fillRect(i + i1 - 5, i0 + 1, 4, i2 - 2);
        if (b) {
            a.setColor(1318956);
            a.fillRect(i + 3, i0 + 3, i1 - 11, i2 - 6);
            a.setColor(1980970);
            a.fillRect(i + 6, i0 + i2 - 40, i1 - 18, 34);
            a.setColor(2764342);
            a.fillRect(i + 8, i0 + i2 - 36, 16, 12);
            a.setColor(10134186);
            a.fillRect(i + 8, i0 + i2 - 38, 16, 3);
            a.setColor(3820122);
            a0 = a;
            int i4 = 0;
            while(i4 < 3) {
                ((rpc.Gfx)a0).fillRect(i + 28 + i4 * 5, i0 + i2 - 36, 3, 16);
                i4 = i4 + 1;
            }
            ((rpc.Gfx)a0).setColor(2240572);
            ((rpc.Gfx)a0).fillRect(i + 6, i0 + 8, i1 - 18, 10);
            ((rpc.Gfx)a0).setColor(3029574);
            ((rpc.Gfx)a0).fillRect(i + 8, i0 + 22, i1 - 22, 9);
        } else {
            a.setColor(9209462);
            a.fillRect(i + 4, i0 + 6, i1 - 12, 7);
            a.fillRect(i + 4, i0 + 16, i1 - 12, 9);
            a.setColor(7236190);
            a.drawRect(i + 4, i0 + 6, i1 - 12, 7);
            a.drawRect(i + 4, i0 + 16, i1 - 12, 9);
            a.setColor(13156524);
            a.fillRect(i + 5, i0 + 7, i1 - 14, 2);
            a.setColor(9209462);
            a.fillRect(i + 5, i0 + 27, i1 - 14, 3);
            a.setColor(1710622);
            a.fillRect(i + 5, i0 + 27, i1 - 14, 1);
            a.setColor(2764342);
            a.fillRect(i + 6, i0 + 18, i1 - 16, 8);
            a.setColor(7260250);
            a.fillRect(i + 6, i0 + 8, 3, 3);
            a.setColor(13127232);
            a.fillRect(i + 12, i0 + 8, 3, 3);
            a.setColor((i3 >= 1995) ? 3817030 : 14735552);
            a.fillRect(i + i1 / 2 - 5, i0 + i2 - 16, 10, 8);
            a0 = a;
        }
        ((rpc.Gfx)a0).setColor(2763310);
        ((rpc.Gfx)a0).fillRect(i + 4, i0 + i2, 6, 3);
        ((rpc.Gfx)a0).fillRect(i + i1 - 12, i0 + i2, 6, 3);
    }
    
    public static void keyboard(rpc.Gfx a, int i, int i0, int i1, int i2, int i3) {
        a.setColor((i3 >= 1995) ? (i3 >= 2005) ? 1974824 : 8683631 : 10130564);
        a.fillRect(i, i0, i1, i2);
        a.setColor(6183502);
        a.drawRect(i, i0, i1, i2);
        a.setColor((i3 >= 2005) ? 4870232 : 14472382);
        int i4 = (i1 - 8) / 14;
        int i5 = (i2 - 8) / 4;
        Object a0 = a;
        int i6 = 0;
        while(i6 < 4) {
            int i7 = 0;
            while(i7 < 14) {
                label0: {
                    label1: {
                        if (i6 != 3) {
                            break label1;
                        }
                        if (i7 <= 3) {
                            break label1;
                        }
                        if (i7 < 9) {
                            break label0;
                        }
                    }
                    ((rpc.Gfx)a0).fillRect(i + 4 + i7 * i4, i0 + 4 + i6 * i5, i4 - 1, i5 - 1);
                }
                i7 = i7 + 1;
            }
            i6 = i6 + 1;
        }
        ((rpc.Gfx)a0).fillRect(i + 4 + 4 * i4, i0 + 4 + 3 * i5, i4 * 5, i5 - 1);
    }
    
    public static void mouse(rpc.Gfx a, int i, int i0, int i1, int i2, int i3) {
        a.setColor((i3 >= 1998) ? 3290686 : 12893350);
        a.fillRoundRect(i, i0, i1, i2, 6);
        a.setColor((i3 >= 1998) ? 1974824 : 8288360);
        a.drawLine(i + i1 / 2, i0 + 2, i + i1 / 2, i0 + i2 / 2);
        a.fillRect(i + i1 / 2 - 1, i0 + i2 / 3, 2, 3);
    }
    
    public static void speaker(rpc.Gfx a, int i, int i0, int i1, int i2, boolean b) {
        a.setColor(b ? 7031346 : 2764342);
        a.fillRect(i, i0, i1, i2);
        a.setColor(1316378);
        int i3 = i1 / 3;
        a.fillRoundRect(i + i1 / 2 - i3, i0 + i2 / 2 - i3, i3 * 2, i3 * 2, 4);
        a.setColor(10134186);
        a.fillRect(i + i1 / 2 - 1, i0 + i2 / 2 - 1, 2, 2);
    }
    
    public static void printer(rpc.Gfx a, int i, int i0, int i1, int i2, int i3) {
        a.setColor((i3 >= 2000) ? 3817030 : 12893350);
        a.fillRect(i, i0 + i2 / 3, i1, i2 - i2 / 3);
        a.setColor(15591640);
        a.fillRect(i + 4, i0, i1 - 8, i2 / 3);
        a.setColor((i3 >= 2000) ? 2237996 : 9209462);
        a.fillRect(i + 4, i0 + i2 - 5, i1 - 8, 4);
        a.setColor(7260250);
        a.fillRect(i + i1 - 6, i0 + i2 / 3 + 2, 2, 2);
    }
    
    public static void lamp(rpc.Gfx a, int i, int i0) {
        a.setColor(3817030);
        a.fillRect(i + 6, i0 + 16, 3, 22);
        a.fillRect(i - 2, i0 + 36, 14, 3);
        a.setColor(15257696);
        a.fillRect(i, i0 + 8, 16, 9);
        a.setColor(9075248);
        a.fillRect(i, i0 + 8, 16, 2);
    }
    
    public static void mug(rpc.Gfx a, int i, int i0) {
        a.setColor(14736590);
        a.fillRect(i, i0, 12, 14);
        a.setColor(9078390);
        a.fillRect(i + 12, i0 + 4, 3, 6);
        a.setColor(4860440);
        a.fillRect(i + 1, i0 + 1, 10, 3);
    }
    
    public static void shadow(rpc.Gfx a, int i, int i0, int i1) {
        a.setColor(2496783);
        a.fillRect(i, i0, i1, 3);
    }
    
    public static void bench(rpc.Gfx a, int i, int i0, int i1, int i2) {
        a.setColor(5453855);
        a.fillRect(i, i0, i1, i2);
        a.setColor(6176803);
        int i3 = i0 + 3;
        Object a0 = a;
        while(i3 < i0 + i2) {
            ((rpc.Gfx)a0).drawLine(i, i3, i + i1, i3);
            i3 = i3 + 7;
        }
        ((rpc.Gfx)a0).setColor(1585738);
        ((rpc.Gfx)a0).fillRect(i + 3, i0 + 3, i1 - 6, i2 - 6);
        ((rpc.Gfx)a0).setColor(2244700);
        ((rpc.Gfx)a0).drawRect(i + 3, i0 + 3, i1 - 6, i2 - 6);
        ((rpc.Gfx)a0).setColor(9079440);
        int i4 = 0;
        while(i4 < 4) {
            ((rpc.Gfx)a0).fillRect(i + 5 + i4 * 4, i0 + i2 - 7, 2, 2);
            i4 = i4 + 1;
        }
        ((rpc.Gfx)a0).setColor(13148224);
        ((rpc.Gfx)a0).fillRect(i + i1 - 12, i0 + i2 - 14, 3, 8);
        ((rpc.Gfx)a0).setColor(10134186);
        ((rpc.Gfx)a0).fillRect(i + i1 - 12, i0 + i2 - 8, 3, 5);
    }
    
    public static void slotFrame(rpc.Gfx a, int i, int i0, int i1, int i2, boolean b, int i3) {
        a.setColor(b ? 1978432 : 1187880);
        a.fillRoundRect(i, i0, i1, i2, 4);
        if (!b) {
            i3 = 2768976;
        }
        a.setColor(i3);
        a.drawRoundRect(i, i0, i1, i2, 4);
    }
    
    public static void caseLayout(rpc.Gfx a, int i, int i0, int i1, int i2, int[] a0, int i3, int i4) {
        a.setColor(2764600);
        a.fillRect(i, i0, i1, i2);
        a.setColor(3818062);
        a.drawRect(i, i0, i1, i2);
        int i5 = i + 4;
        int i6 = i0 + 4;
        int i7 = i1 - 46;
        int i8 = i2 - 10;
        a.setColor(1985070);
        a.fillRect(i5, i6, i7, i8);
        a.setColor(3041860);
        a.drawRect(i5, i6, i7, i8);
        int i9 = i5 + 4;
        int i10 = i6 + 4;
        rpc.Scene.fillSlot(a, i9, i10, 14, 14, a0[2] != 0, i4, i3 == 2);
        Object a1 = a;
        int i11 = 0;
        while(i11 < 4) {
            rpc.Scene.fillSlot((rpc.Gfx)a1, i9 + 17 + i11 * 5, i10, 3, 14, a0[3 + i11] != 0, i4, i3 == 3 + i11);
            i11 = i11 + 1;
        }
        int i12 = 0;
        while(i12 < 3) {
            rpc.Scene.fillSlot((rpc.Gfx)a1, i9, i10 + 17 + i12 * 5, 22, 3, a0[6 + i12] != 0, i4, i3 == 6 + i12);
            i12 = i12 + 1;
        }
        rpc.Scene.fillSlot((rpc.Gfx)a1, i9 + 17, i10 + 17, 12, 12, a0[12] != 0, i4, i3 == 12);
        int i13 = i5 + i7 - 22;
        int i14 = 0;
        while(i14 < 3) {
            rpc.Scene.fillSlot((rpc.Gfx)a1, i13, i6 + 4 + i14 * 11, 18, 8, a0[11 + i14] != 0, i4, i3 == 11 + i14);
            i14 = i14 + 1;
        }
        rpc.Scene.fillSlot((rpc.Gfx)a1, i + i1 - 38, i0 + i2 - 16, 34, 12, a0[10] != 0, i4, i3 == 10);
        ((rpc.Gfx)a1).setColor(5923954);
        ((rpc.Gfx)a1).fillRect(i + 2, i0 + 2, 1, i2 - 4);
        ((rpc.Gfx)a1).fillRect(i + i1 - 2, i0 + 2, 1, i2 - 4);
    }
    
    private static void fillSlot(rpc.Gfx a, int i, int i0, int i1, int i2, boolean b, int i3, boolean b0) {
        a.setColor(b ? 5151324 : 1054752);
        a.fillRect(i, i0, i1, i2);
        if (!b0) {
            i3 = 3822144;
        }
        a.setColor(i3);
        a.drawRect(i, i0, i1, i2);
        if (b0) {
            a.drawRect(i - 1, i0 - 1, i1 + 2, i2 + 2);
        }
    }
}
