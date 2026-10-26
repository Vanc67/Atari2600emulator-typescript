package rpc;

public class Db {
    final public static String[] CAT_LABEL;
    private byte[] data;
    private int n;
    private short[] cat;
    private short[] year;
    private int[] wOff;
    private short[] wLen;
    private int[] partOff;
    public String[] SOC;
    public String[] BUS;
    public String[] RAM;
    public String[] IFC;
    public String[] PST;
    public String[] FRM;
    public String[] CLK;
    public String[] MNT;
    public String[] MSC;
    public String[] PRK;
    public rpc.Db$Level[] levels;
    public int minYear;
    public int maxYear;
    final private rpc.Part[] cust;
    private int custN;
    
    static {
        String[] a = new String[20];
        a[0] = "?";
        a[1] = "CPU";
        a[2] = "GPU";
        a[3] = "\u041c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u0430\u044f \u043f\u043b\u0430\u0442\u0430";
        a[4] = "\u041f\u0430\u043c\u044f\u0442\u044c";
        a[5] = "HDD";
        a[6] = "FDD";
        a[7] = "SSD";
        a[8] = "\u041f\u0440\u0438\u0432\u043e\u0434";
        a[9] = "\u0411\u043b\u043e\u043a \u043f\u0438\u0442\u0430\u043d\u0438\u044f";
        a[10] = "\u041a\u043e\u0440\u043f\u0443\u0441";
        a[11] = "\u041e\u0445\u043b\u0430\u0436\u0434\u0435\u043d\u0438\u0435";
        a[12] = "\u041c\u043e\u043d\u0438\u0442\u043e\u0440";
        a[13] = "\u0417\u0432\u0443\u043a\u043e\u0432\u0430\u044f \u043a\u0430\u0440\u0442\u0430";
        a[14] = "\u0421\u0435\u0442\u0435\u0432\u0430\u044f \u043a\u0430\u0440\u0442\u0430";
        a[15] = "\u041a\u043b\u0430\u0432\u0438\u0430\u0442\u0443\u0440\u0430";
        a[16] = "\u041c\u044b\u0448\u044c";
        a[17] = "\u0413\u0435\u0439\u043c\u043f\u0430\u0434";
        a[18] = "\u041f\u043b\u0430\u0442\u0430 \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043d\u0438\u044f";
        a[19] = "\u0412\u0441\u0442\u0440\u043e\u0435\u043d\u043d\u043e\u0435 \u0432\u0438\u0434\u0435\u043e";
        CAT_LABEL = a;
    }
    
    public Db() {
        this.SOC = new String[0];
        this.BUS = new String[0];
        this.RAM = new String[0];
        this.IFC = new String[0];
        this.PST = new String[0];
        this.FRM = new String[0];
        this.CLK = new String[0];
        this.MNT = new String[0];
        this.MSC = new String[0];
        this.PRK = new String[0];
        this.levels = new rpc.Db$Level[0];
        this.cust = new rpc.Part[32];
        this.custN = 0;
    }
    
    public void load(rpc.Host a) {
        this.data = rpc.Db.readAll(a.open("parts.pak"));
        this.parse();
        this.loadDict(a);
        this.loadLevels(a);
    }
    
    private static byte[] readAll(java.io.InputStream a) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(240000);
        byte[] buf = new byte[8192];
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
    
    private int u8(int i) {
        int i0 = this.data[i];
        return i0 & 255;
    }
    
    private int u16(int i) {
        int i0 = this.data[i];
        int i1 = (i0 & 255) << 8;
        int i2 = this.data[i + 1];
        return i1 | i2 & 255;
    }
    
    private int u32(int i) {
        int i0 = this.data[i];
        int i1 = (i0 & 255) << 24;
        int i2 = this.data[i + 1];
        int i3 = i1 | (i2 & 255) << 16;
        int i4 = this.data[i + 2];
        int i5 = i3 | (i4 & 255) << 8;
        int i6 = this.data[i + 3];
        return i5 | i6 & 255;
    }
    
    private void parse() {
        if (this.data.length >= 12) {
            int i = this.data[0];
            if (i == 82) {
                int i0 = this.data[1];
                if (i0 == 80) {
                    int i1 = this.data[2];
                    if (i1 == 75) {
                        int i2 = this.u16(4);
                        this.wOff = new int[i2];
                        this.wLen = new short[i2];
                        int i3 = 6;
                        int i4 = 0;
                        while(i4 < i2) {
                            int i5 = this.u8(i3);
                            this.wOff[i4] = i3 + 1;
                            short[] a = this.wLen;
                            int i6 = (short)i5;
                            a[i4] = (short)i6;
                            i3 = i3 + (1 + i5);
                            i4 = i4 + 1;
                        }
                        this.n = this.u32(i3);
                        int i7 = i3 + 4;
                        this.partOff = new int[this.n];
                        this.cat = new short[this.n];
                        this.year = new short[this.n];
                        this.minYear = 9999;
                        this.maxYear = 0;
                        int i8 = 0;
                        while(i8 < this.n) {
                            this.partOff[i8] = i7;
                            short[] a0 = this.cat;
                            int i9 = (short)this.u8(i7);
                            a0[i8] = (short)i9;
                            int i10 = this.u16(i7 + 1);
                            short[] a1 = this.year;
                            int i11 = (short)i10;
                            a1[i8] = (short)i11;
                            if (i10 < this.minYear) {
                                this.minYear = i10;
                            }
                            if (i10 > this.maxYear) {
                                this.maxYear = i10;
                            }
                            i7 = i7 + (6 + this.u8(i7 + 5) * 2 + rpc.Db.fieldLen(this.cat[i8]));
                            i8 = i8 + 1;
                        }
                        return;
                    }
                }
            }
        }
        this.n = 0;
        this.cat = new short[0];
        this.year = new short[0];
        this.partOff = new int[0];
    }
    
    private static int fieldLen(int i) {
        switch(i) {
            case 21: case 22: case 23: case 24: {
                return 9;
            }
            case 18: case 20: {
                return 2;
            }
            case 15: case 16: case 17: {
                return 1;
            }
            case 14: {
                return 4;
            }
            case 13: {
                return 4;
            }
            case 12: {
                return 10;
            }
            case 11: {
                return 3;
            }
            case 10: {
                return 2;
            }
            case 9: {
                return 3;
            }
            case 5: case 6: case 7: case 8: {
                return 7;
            }
            case 4: {
                return 7;
            }
            case 3: {
                return 10;
            }
            case 2: case 19: {
                return 13;
            }
            case 1: {
                return 9;
            }
            default: {
                return 0;
            }
        }
    }
    
    private String name(int i) {
        int i0 = this.partOff[i] + 6;
        int i1 = this.u8(this.partOff[i] + 5);
        StringBuffer a = new StringBuffer(32);
        int i2 = 0;
        while(i2 < i1) {
            int i3 = this.u16(i0 + i2 * 2);
            if (i3 < this.wOff.length) {
                if (a.length() > 0) {
                    a.append((char)32);
                }
                a.append(rpc.Util.utf8(this.data, this.wOff[i3], this.wLen[i3]));
            }
            i2 = i2 + 1;
        }
        return a.toString();
    }
    
    private void loadDict(rpc.Host a) {
        byte[] a0 = rpc.Db.readAll(a.open("dict.txt"));
        String[] a1 = rpc.Util.split(rpc.Util.utf8(a0, 0, a0.length), (char)10);
        int i = 0;
        while(i < a1.length) {
            String s = a1[i];
            if (s.length() != 0) {
                int i0 = s.indexOf(61);
                if (i0 >= 0) {
                    String s0 = s.substring(0, i0).trim();
                    String[] a2 = rpc.Util.split(s.substring(i0 + 1).trim(), (char)44);
                    if (s0.equals((Object)"SOC")) {
                        this.SOC = a2;
                    } else if (s0.equals((Object)"BUS")) {
                        this.BUS = a2;
                    } else if (s0.equals((Object)"RAM")) {
                        this.RAM = a2;
                    } else if (s0.equals((Object)"IFC")) {
                        this.IFC = a2;
                    } else if (s0.equals((Object)"PST")) {
                        this.PST = a2;
                    } else if (s0.equals((Object)"FRM")) {
                        this.FRM = a2;
                    } else if (s0.equals((Object)"CLK")) {
                        this.CLK = a2;
                    } else if (s0.equals((Object)"MNT")) {
                        this.MNT = a2;
                    } else if (s0.equals((Object)"MSC")) {
                        this.MSC = a2;
                    } else if (s0.equals((Object)"PRK")) {
                        this.PRK = a2;
                    }
                }
            }
            i = i + 1;
        }
    }
    
    private void loadLevels(rpc.Host a) {
        byte[] a0 = rpc.Db.readAll(a.open("levels.txt"));
        String[] a1 = rpc.Util.split(rpc.Util.utf8(a0, 0, a0.length), (char)10);
        int i = rpc.Util.parseInt(a1[0].trim());
        this.levels = new rpc.Db$Level[i];
        int i0 = 0;
        int i1 = 1;
        while(true) {
            if (i1 < a1.length) {
                int i2 = i0;
                if (i0 < i) {
                    String s = a1[i1].trim();
                    int i3 = s.length();
                    i0 = i2;
                    if (i3 != 0) {
                        s.indexOf(59);
                        int[] a2 = new int[6];
                        int i4 = 0;
                        int i5 = 0;
                        String s0 = "";
                        int i6 = 0;
                        while(true) {
                            int i7 = s.length();
                            String s1 = s0;
                            if (i6 > i7) {
                                break;
                            }
                            int i8 = s.length();
                            s0 = s1;
                            label0: {
                                label1: {
                                    if (i6 == i8) {
                                        break label1;
                                    }
                                    int i9 = s.charAt(i6);
                                    s1 = s0;
                                    if (i9 != 59) {
                                        break label0;
                                    }
                                }
                                s0 = s.substring(i5, i6);
                                if (i4 < 6) {
                                    a2[i4] = rpc.Util.parseInt(s0);
                                    s0 = s1;
                                }
                                i4 = i4 + 1;
                                i5 = i6 + 1;
                            }
                            i6 = i6 + 1;
                        }
                        rpc.Db$Level a3 = new rpc.Db$Level();
                        a3.year = a2[0];
                        a3.budget = a2[1];
                        a3.refC = a2[2];
                        a3.refG = a2[3];
                        a3.refR = a2[4];
                        a3.refD = a2[5];
                        a3.title = s0;
                        rpc.Db$Level[] a4 = this.levels;
                        i0 = i2 + 1;
                        a4[i2] = a3;
                    }
                    i1 = i1 + 1;
                    continue;
                }
            }
            return;
        }
    }
    
    public void addCustom(rpc.Part a) {
        if (a != null && this.custN < this.cust.length) {
            a.index = this.n + this.custN;
            rpc.Part[] a0 = this.cust;
            int i = this.custN;
            this.custN = i + 1;
            a0[i] = a;
            return;
        }
    }
    
    public int customCount() {
        return this.custN;
    }
    
    public int baseCount() {
        return this.n;
    }
    
    public int count() {
        return this.n + this.custN;
    }
    
    public rpc.Part get(int i) {
        if (i < this.n) {
            if (i < 0) {
                rpc.Part a = null;
                return a;
            }
            rpc.Part a0 = new rpc.Part();
            a0.index = i;
            int i0 = this.cat[i];
            a0.cat = i0;
            int i1 = this.year[i];
            a0.year = i1;
            int i2 = this.partOff[i];
            a0.price = this.u16(i2 + 3);
            a0.name = this.name(i);
            int i3 = i2 + 6 + this.u8(i2 + 5) * 2;
            int i4 = this.cat[i];
            switch(i4) {
                case 21: case 22: case 23: case 24: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u16(i3 + 1);
                    a0.f[2] = this.u16(i3 + 3);
                    a0.f[3] = this.u8(i3 + 5);
                    a0.f[4] = this.u16(i3 + 6);
                    a0.f[5] = this.u8(i3 + 8);
                    break;
                }
                case 18: case 20: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u8(i3 + 1);
                    break;
                }
                case 15: case 16: case 17: {
                    a0.f[0] = this.u8(i3);
                    break;
                }
                case 14: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u16(i3 + 1);
                    a0.f[2] = this.u8(i3 + 3);
                    break;
                }
                case 13: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u8(i3 + 1);
                    a0.f[2] = this.u8(i3 + 2);
                    a0.f[3] = this.u8(i3 + 3);
                    break;
                }
                case 12: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u16(i3 + 1);
                    a0.f[2] = this.u16(i3 + 3);
                    a0.f[3] = this.u16(i3 + 5);
                    a0.f[4] = this.u16(i3 + 7);
                    a0.f[5] = this.u8(i3 + 9);
                    break;
                }
                case 11: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u16(i3 + 1);
                    break;
                }
                case 10: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u8(i3 + 1);
                    break;
                }
                case 9: {
                    a0.f[0] = this.u16(i3);
                    a0.f[1] = this.u8(i3 + 2);
                    break;
                }
                case 4: case 5: case 6: case 7: case 8: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u32(i3 + 1);
                    a0.f[2] = this.u16(i3 + 5);
                    break;
                }
                case 3: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u8(i3 + 1);
                    a0.f[2] = this.u8(i3 + 2);
                    a0.f[3] = this.u32(i3 + 3);
                    a0.f[4] = this.u8(i3 + 7);
                    a0.f[5] = this.u16(i3 + 8);
                    break;
                }
                case 2: case 19: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u32(i3 + 1);
                    a0.f[2] = this.u16(i3 + 5);
                    a0.f[3] = this.u16(i3 + 7);
                    a0.f[4] = this.u16(i3 + 9);
                    a0.f[5] = this.u16(i3 + 11);
                    break;
                }
                case 1: {
                    a0.f[0] = this.u8(i3);
                    a0.f[1] = this.u16(i3 + 1);
                    a0.f[2] = this.u8(i3 + 3);
                    a0.f[3] = this.u16(i3 + 4);
                    a0.f[4] = this.u16(i3 + 6);
                    a0.f[5] = this.u8(i3 + 8);
                    break;
                }
            }
            return a0;
        } else {
            rpc.Part a1 = null;
            int i5 = i - this.n;
            label2: {
                label0: {
                    label1: {
                        if (i5 < 0) {
                            break label1;
                        }
                        if (i5 < this.custN) {
                            break label0;
                        }
                    }
                    a1 = null;
                    break label2;
                }
                a1 = this.cust[i5];
            }
            return a1;
        }
    }
    
    public int[] query(int[] a, int i, int i0) {
        int i1 = 0;
        int i2 = 0;
        while(i2 < this.n) {
            int i3 = this.year[i2];
            if (i3 >= i && this.year[i2] <= i0) {
                int i4 = 0;
                while(i4 < a.length) {
                    {
                        if (this.cat[i2] != a[i4]) {
                            i4 = i4 + 1;
                            continue;
                        }
                        i1 = i1 + 1;
                        break;
                    }
                }
            }
            i2 = i2 + 1;
        }
        int i5 = 0;
        while(i5 < this.custN) {
            rpc.Part a0 = this.cust[i5];
            if (a0 != null && a0.year >= i && a0.year <= i0) {
                int i6 = 0;
                while(i6 < a.length) {
                    {
                        if (a0.cat != a[i6]) {
                            i6 = i6 + 1;
                            continue;
                        }
                        i1 = i1 + 1;
                        break;
                    }
                }
            }
            i5 = i5 + 1;
        }
        int[] a1 = new int[i1];
        int i7 = 0;
        int i8 = 0;
        while(true) {
            int i9 = this.n;
            int i10 = i8;
            if (i7 < i9) {
                int i11 = this.year[i7];
                if (i11 < i) {
                    i8 = i10;
                } else {
                    int b = this.year[i7];
                    i8 = i10;
                    if (b <= i0) {
                        int i12 = 0;
                        while(true) {
                            int i13 = a.length;
                            i8 = i10;
                            if (i12 >= i13) {
                                break;
                            }
                            {
                                if (this.cat[i7] != a[i12]) {
                                    i12 = i12 + 1;
                                    continue;
                                }
                                i8 = i10 + 1;
                                a1[i10] = i7;
                                break;
                            }
                        }
                    }
                }
                i7 = i7 + 1;
            } else {
                int i14 = 0;
                while(i14 < this.custN) {
                    rpc.Part a2 = this.cust[i14];
                    int i15 = i8;
                    if (a2 != null) {
                        if (a2.year < i) {
                            i8 = i15;
                        } else {
                            int i16 = a2.year;
                            i8 = i15;
                            if (i16 <= i0) {
                                int i17 = 0;
                                while(true) {
                                    int i18 = a.length;
                                    i8 = i15;
                                    if (i17 >= i18) {
                                        break;
                                    }
                                    {
                                        if (a2.cat != a[i17]) {
                                            i17 = i17 + 1;
                                            continue;
                                        }
                                        i8 = i15 + 1;
                                        a1[i15] = this.n + i14;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    i14 = i14 + 1;
                }
                return a1;
            }
        }
    }
    
    public int countInCat(int i) {
        int i0 = 0;
        int i1 = 0;
        while(i1 < this.n) {
            int i2 = this.cat[i1];
            if (i2 == i) {
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        int i3 = 0;
        while(i3 < this.custN) {
            if (this.cust[i3] != null && this.cust[i3].cat == i) {
                i0 = i0 + 1;
            }
            i3 = i3 + 1;
        }
        return i0;
    }
    
    public String dict(String[] a, int i) {
        if (a != null && i >= 0 && i < a.length) {
            return a[i];
        }
        return "?";
    }
}
