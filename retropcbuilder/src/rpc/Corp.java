package rpc;

public class Corp {
    final public static int MAX_PROD = 16;
    final public static int K_CPU = 0;
    final public static int K_GPU = 1;
    final public static int K_OS = 2;
    final public static int K_SOFT = 3;
    final public static String[] KIND_NAME;
    final public static int SOFT_EDITOR = 0;
    final public static int SOFT_BENCH = 1;
    final public static int SOFT_GAME = 2;
    final public static int SOFT_MINE = 3;
    final public static String[] SOFT_NAME;
    public boolean founded;
    public String name;
    public int cash;
    public int rd;
    public int engineers;
    public int programmers;
    public int marketing;
    public int testing;
    public int reputation;
    public int day;
    public int year;
    public rpc.Corp$Prod[] prods;
    public int n;
    final public static int[] NODES;
    final public static int[] NODE_MHZ;
    
    static {
        String[] a = new String[4];
        a[0] = "\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440";
        a[1] = "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430";
        a[2] = "\u0421\u0438\u0441\u0442\u0435\u043c\u0430";
        a[3] = "\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430";
        KIND_NAME = a;
        String[] a0 = new String[4];
        a0[0] = "\u0422\u0435\u043a\u0441\u0442\u043e\u0432\u044b\u0439 \u0440\u0435\u0434\u0430\u043a\u0442\u043e\u0440";
        a0[1] = "\u0411\u0435\u043d\u0447\u043c\u0430\u0440\u043a";
        a0[2] = "\u0418\u0433\u0440\u0430";
        a0[3] = "\u041c\u0430\u0439\u043d\u0435\u0440";
        SOFT_NAME = a0;
        int[] a1 = new int[20];
        a1[0] = 3000;
        a1[1] = 2000;
        a1[2] = 1500;
        a1[3] = 1000;
        a1[4] = 800;
        a1[5] = 600;
        a1[6] = 350;
        a1[7] = 250;
        a1[8] = 180;
        a1[9] = 130;
        a1[10] = 90;
        a1[11] = 65;
        a1[12] = 45;
        a1[13] = 32;
        a1[14] = 22;
        a1[15] = 14;
        a1[16] = 10;
        a1[17] = 7;
        a1[18] = 5;
        a1[19] = 3;
        NODES = a1;
        int[] a2 = new int[20];
        a2[0] = 5;
        a2[1] = 8;
        a2[2] = 12;
        a2[3] = 20;
        a2[4] = 25;
        a2[5] = 33;
        a2[6] = 66;
        a2[7] = 100;
        a2[8] = 200;
        a2[9] = 450;
        a2[10] = 700;
        a2[11] = 1000;
        a2[12] = 2000;
        a2[13] = 3000;
        a2[14] = 3400;
        a2[15] = 4000;
        a2[16] = 4500;
        a2[17] = 5000;
        a2[18] = 5500;
        a2[19] = 6000;
        NODE_MHZ = a2;
    }
    
    public Corp() {
        this.founded = false;
        this.name = "\u0421\u0412\u041e\u042f \u0424\u0418\u0420\u041c\u0410";
        this.cash = 4000;
        this.rd = 120;
        this.engineers = 2;
        this.programmers = 1;
        this.marketing = 1;
        this.testing = 1;
        this.reputation = 50;
        this.day = 1;
        this.year = 1985;
        this.prods = new rpc.Corp$Prod[16];
        this.n = 0;
    }
    
    private static int eraTdp(int i) {
        if (i <= 1981) {
            return 3;
        }
        if (i <= 1985) {
            return 5;
        }
        if (i <= 1990) {
            return 10;
        }
        if (i <= 1995) {
            return 20;
        }
        if (i <= 1999) {
            return 35;
        }
        if (i <= 2004) {
            return 90;
        }
        if (i <= 2009) {
            return 95;
        }
        if (i <= 2015) {
            return 85;
        }
        if (i > 2020) {
            return 125;
        }
        return 105;
    }
    
    private static int eraTransistors(int i) {
        if (i <= 1981) {
            return 1;
        }
        if (i <= 1985) {
            return 2;
        }
        if (i <= 1990) {
            return 5;
        }
        if (i <= 1995) {
            return 20;
        }
        if (i <= 1999) {
            return 60;
        }
        if (i <= 2004) {
            return 300;
        }
        if (i <= 2009) {
            return 1500;
        }
        if (i <= 2015) {
            return 4000;
        }
        if (i > 2020) {
            return 30000;
        }
        return 12000;
    }
    
    public static int nodeIndex(int i) {
        int i0 = 0;
        int i1 = 0;
        while(i1 < NODES.length) {
            if (NODES[i1] >= i) {
                i0 = i1;
            }
            i1 = i1 + 1;
        }
        return i0;
    }
    
    public static String nodeText(int i) {
        if (i < 1000) {
            return new StringBuffer(String.valueOf(i)).append(" \u043d\u043c").toString();
        }
        int i0 = i / 1000;
        int i1 = i % 1000 / 100;
        return (i1 != 0) ? new StringBuffer(String.valueOf(i0)).append(",").append(i1).append(" \u043c\u043a\u043c").toString() : new StringBuffer(String.valueOf(i0)).append(" \u043c\u043a\u043c").toString();
    }
    
    public static int tdp(rpc.Corp$Chip a) {
        int i = 0;
        int i0 = rpc.Corp.nodeIndex(a.nodeNm);
        int i1 = NODE_MHZ[i0];
        int i2 = rpc.Corp.eraTdp(a.year);
        int i3 = rpc.Corp.eraTransistors(a.year);
        int i4 = 100 * a.transistorsM;
        if (i3 <= 0) {
            i3 = 1;
        }
        int i5 = i4 / i3;
        if (i5 < 40) {
            i5 = 40;
        }
        if (i5 > 400) {
            i5 = 400;
        }
        int i6 = 60 + i5 * 2 / 5;
        int i7 = a.mhz * 100;
        if (i1 <= 0) {
            i1 = 1;
        }
        int i8 = i7 / i1;
        if (i8 > 100) {
            int i9 = i8 - 100;
            i = 100 + i9 * 3 / 2 + i9 * i9 / 60;
            if (i > 700) {
                i = 700;
            }
        } else {
            i = 70 + i8 * 30 / 100;
        }
        int i10 = i2 * i6 / 100 * i / 100 * (100 + (((a.cat != 0) ? 1 : (a.cores >= 1) ? a.cores : 1) - 1) * 35) / 100;
        if (a.cat == 1) {
            i10 = i10 * 3 / 2;
        }
        int i11 = i10 + a.cacheKb / 256;
        if (i11 < 1) {
            i11 = 1;
        }
        return i11;
    }
    
    public static int overclock(rpc.Corp$Chip a) {
        int i = NODE_MHZ[rpc.Corp.nodeIndex(a.nodeNm)];
        int i0 = a.mhz * 100;
        if (i <= 0) {
            i = 1;
        }
        return i0 / i;
    }
    
    public static int perf(rpc.Corp$Chip a) {
        int i = (a.cat != 0) ? 1 : (a.cores >= 1) ? a.cores : 1;
        long j = (long)a.mhz * (long)i * (long)a.ipc / 100L;
        if (a.cat == 1) {
            long j0 = (long)a.vramKb * (long)a.bits / 1024L;
            long j1 = j / 4L;
            if (j0 > 2000000000L) {
                j0 = 2000000000L;
            }
            j = j1 + (long)(rpc.Util.isqrt((int)j0) * 8);
        }
        if (a.cacheKb > 0) {
            j = j + j * (long)(a.cacheKb / 64) / 100L;
        }
        if (j > 2000000000L) {
            j = 2000000000L;
        }
        return (int)j;
    }
    
    public static int devRd(rpc.Corp$Chip a) {
        long j = 30L + (long)a.transistorsM / 4L + (long)(a.mhz / 8) + (long)(a.cacheKb / 32);
        if (a.cat == 1) {
            j = j * 3L / 2L;
        }
        if (j > 200000L) {
            j = 200000L;
        }
        return (int)j;
    }
    
    public static int devCash(rpc.Corp$Chip a) {
        long j = 400L + (long)a.transistorsM * 6L + (long)(a.mhz * 2);
        if (a.cat == 1) {
            j = j * 3L / 2L;
        }
        if (j > 400000L) {
            j = 400000L;
        }
        return (int)j;
    }
    
    public static int devDays(rpc.Corp$Chip a) {
        return rpc.Corp.devDays(a, 2);
    }
    
    public static int devDays(rpc.Corp$Chip a, int i) {
        if (i < 1) {
            i = 1;
        }
        int i0 = 8 + a.transistorsM / 20 + a.mhz / 60;
        if (a.cat == 1) {
            i0 = i0 + i0 / 2;
        }
        return i0 * 2 / (1 + i / 2);
    }
    
    public static int quality(rpc.Corp$Chip a, int i, int i0, int i1) {
        int i2 = 40 + i * 14 + i0 * 4 + i1 / 4;
        int i3 = rpc.Corp.overclock(a);
        if (i3 > 100) {
            i2 = i2 - (i3 - 100) / 2;
        }
        if (i2 < 5) {
            i2 = 5;
        }
        if (i2 > 98) {
            i2 = 98;
        }
        return i2;
    }
    
    public static int bugsOf(int i) {
        int i0 = (100 - i) * 3 / 2;
        if (i0 >= 0) {
            if (i0 > 96) {
                i0 = 96;
            }
        } else {
            i0 = 0;
        }
        return i0;
    }
    
    public static int suggestPrice(rpc.Corp$Chip a) {
        long j = (10L + (long)a.transistorsM / 8L + (long)(a.mhz / 4)) * 3L / 2L + (long)rpc.Corp.perf(a) / 40L;
        if (j < 15L) {
            j = 15L;
        }
        if (j > 60000L) {
            j = 60000L;
        }
        return (int)j;
    }
    
    public static int rivalCpuPerf(int i) {
        return (int)((long)((i > 1981) ? (i > 1990) ? (i > 1995) ? (i > 2000) ? (i > 2005) ? (i > 2012) ? (i > 2019) ? 5500 : 4200 : 3400 : 3000 : 800 : 66 : 20 : 8) * (long)((i > 2004) ? (i > 2012) ? (i > 2019) ? 16 : 8 : 4 : 1));
    }
    
    public static int rivalGpuPerf(int i) {
        return (int)(long)((i > 1995) ? (i > 2000) ? (i > 2005) ? (i > 2010) ? (i > 2016) ? (i > 2021) ? 2600000 : 1600000 : 900000 : 200000 : 20000 : 2000 : 200);
    }
    
    public static String rivalBrand(int i, int i0) {
        if (i == 0) {
            return (i0 >= 2006) ? (i0 >= 2017) ? "Intel / AMD" : "Intel" : "Intel / AMD";
        }
        if (i == 1) {
            return (i0 >= 1997) ? (i0 >= 2006) ? "NVIDIA / AMD" : "3dfx / NVIDIA" : "S3 / Matrox";
        }
        if (i != 2) {
            return "\u043c\u043d\u043e\u0436\u0435\u0441\u0442\u0432\u043e \u0441\u0442\u0443\u0434\u0438\u0439";
        }
        return (i0 >= 2000) ? "Microsoft / Linux" : "Microsoft / IBM";
    }
    
    public int share(rpc.Corp$Prod a) {
        if (a != null && a.released) {
            int i = 0;
            int i0 = (a.kind != 0) ? rpc.Corp.rivalGpuPerf(a.year) : rpc.Corp.rivalCpuPerf(a.year);
            int i1 = (a.kind != 0) ? 80 + i0 / 300 : 60 + i0 / 30;
            if (i1 < 20) {
                i1 = 20;
            }
            int i2 = a.kind;
            label1: {
                label0: {
                    if (i2 == 0) {
                        break label0;
                    }
                    if (a.kind == 1) {
                        break label0;
                    }
                    i = 100 + a.quality / 2;
                    break label1;
                }
                int i3 = a.perf * 100;
                if (i0 <= 0) {
                    i0 = 1;
                }
                i = i3 / i0;
                if (i > 400) {
                    i = 400;
                }
                if (i < 10) {
                    i = 10;
                }
            }
            int i4 = 100 * i1 / ((a.price > 0) ? a.price : 1);
            if (i4 > 400) {
                i4 = 400;
            }
            long j = (long)i * (long)i4 / 100L * (long)(100 - a.bugs) / 100L * (long)(60 + this.reputation) / 100L * (long)(100 + this.marketing * 25) / 100L;
            if (j > 100L) {
                j = 100L;
            }
            if (j < 0L) {
                j = 0L;
            }
            return (int)j;
        }
        return 0;
    }
    
    public int dailyIncome() {
        long j = 0L;
        int i = 0;
        while(i < this.n) {
            rpc.Corp$Prod a = this.prods[i];
            if (a != null && a.released) {
                int i0 = this.share(a);
                if (i0 > 0) {
                    int i1 = 0;
                    int i2 = a.kind;
                    label2: {
                        label0: {
                            label1: {
                                if (i2 == 0) {
                                    break label1;
                                }
                                if (a.kind != 1) {
                                    break label0;
                                }
                            }
                            i1 = 20 + a.perf / 25;
                            break label2;
                        }
                        i1 = (a.kind != 2) ? 18 + a.quality / 4 : 30 + a.quality / 3;
                    }
                    int i3 = i1 * i0 / 100;
                    if (i3 < 1) {
                        i3 = 1;
                    }
                    j = j + (long)i3 * (long)a.price / 12L;
                    a.sold = a.sold + i3;
                }
            }
            i = i + 1;
        }
        if (j > 2000000000L) {
            j = 2000000000L;
        }
        return (int)j;
    }
    
    public int upkeep() {
        return 30 + this.engineers * 18 + this.programmers * 14 + this.marketing * 25 + (this.testing + 1) * 15 + this.n * 6;
    }
    
    public int tickDay() {
        this.day = this.day + 1;
        if (!this.founded) {
            return 0;
        }
        int i = this.dailyIncome();
        this.cash = this.cash + i;
        this.cash = this.cash - this.upkeep();
        this.rd = this.rd + (2 + this.engineers + this.programmers / 2);
        if (this.cash < 0) {
            this.cash = 0;
            this.reputation = this.reputation - 2;
            if (this.reputation < 0) {
                this.reputation = 0;
            }
        }
        return i;
    }
    
    public void found(String s, int i) {
        this.founded = true;
        this.name = s;
        this.year = i;
        this.cash = 3000 + (i - 1980) * 120;
        this.rd = 60 + (i - 1980) * 4;
        this.engineers = 1;
        this.programmers = 1;
        this.marketing = 1;
        this.testing = 1;
        this.reputation = 45;
        this.n = 0;
        this.day = 1;
    }
    
    public boolean canAfford(rpc.Corp$Chip a) {
        if (this.rd >= rpc.Corp.devRd(a) && this.cash >= rpc.Corp.devCash(a)) {
            return true;
        }
        return false;
    }
    
    public rpc.Corp$Prod releaseChip(rpc.Corp$Chip a, rpc.Db a0, String s, int i) {
        if (this.n >= 16) {
            rpc.Corp$Prod a1 = null;
            return a1;
        }
        int i0 = rpc.Corp.devRd(a);
        int i1 = rpc.Corp.devCash(a);
        if (this.rd >= i0 && this.cash >= i1) {
            this.rd = this.rd - i0;
            this.cash = this.cash - i1;
            rpc.Part a2 = new rpc.Part();
            a2.cat = (a.cat != 0) ? 2 : 1;
            a2.year = a.year;
            a2.name = s;
            a2.price = i;
            a2.cond = 100;
            a2.dust = 0;
            if (a.cat != 0) {
                a2.f[0] = a.busIdx;
                a2.f[1] = a.vramKb;
                a2.f[2] = a.bits;
                a2.f[3] = a.mhz;
                a2.f[4] = rpc.Corp.tdp(a);
                a2.f[5] = rpc.Corp.perf(a);
            } else {
                a2.f[0] = a.socketIdx;
                a2.f[1] = a.mhz;
                a2.f[2] = (a.cores >= 1) ? a.cores : 1;
                a2.f[3] = rpc.Corp.tdp(a);
                a2.f[4] = a.ipc;
                a2.f[5] = 3;
            }
            a0.addCustom(a2);
            rpc.Corp$Prod a3 = new rpc.Corp$Prod();
            a3.kind = a.cat;
            a3.name = s;
            a3.year = a.year;
            a3.price = i;
            a3.perf = rpc.Corp.perf(a);
            a3.quality = rpc.Corp.quality(a, this.testing, this.engineers, i0);
            a3.bugs = rpc.Corp.bugsOf(a3.quality);
            a3.dev = i0;
            a3.part = a2;
            a3.released = true;
            rpc.Corp$Prod[] a4 = this.prods;
            int i2 = this.n;
            this.n = i2 + 1;
            a4[i2] = a3;
            this.reputation = this.reputation + a3.quality / 20;
            return a3;
        }
        rpc.Corp$Prod a5 = null;
        return a5;
    }
    
    public static int osRd(rpc.Corp$OsSpec a) {
        return 40 + a.features * 25 + ((a.gui != 1) ? 0 : 60) + a.minRamKb / 512;
    }
    
    public static int osCash(rpc.Corp$OsSpec a) {
        return 600 + a.features * 250 + ((a.gui != 1) ? 0 : 900);
    }
    
    public static int osQuality(rpc.Corp$OsSpec a, int i, int i0) {
        int i1 = 35 + i * 13 + i0 * 5 + a.features * 3;
        if (a.gui == 1) {
            i1 = i1 + -8;
        }
        if (a.minRamKb > 65536) {
            i1 = i1 + -6;
        }
        if (i1 < 5) {
            i1 = 5;
        }
        if (i1 > 97) {
            i1 = 97;
        }
        return i1;
    }
    
    public rpc.Corp$Prod releaseOs(rpc.Corp$OsSpec a, rpc.Inst a0, String s, int i) {
        if (this.n >= 16) {
            rpc.Corp$Prod a1 = null;
            return a1;
        }
        int i0 = rpc.Corp.osRd(a);
        int i1 = rpc.Corp.osCash(a);
        if (this.rd >= i0 && this.cash >= i1) {
            this.rd = this.rd - i0;
            this.cash = this.cash - i1;
            rpc.Inst$Os a2 = new rpc.Inst$Os();
            a2.name = s;
            a2.year = a.year;
            a2.minMhz = a.minMhz;
            a2.minRamKb = a.minRamKb;
            a2.minDiskKb = (a.gui != 1) ? 1024 : 10240;
            a2.baseMin = 25 + a.features * 6 + ((a.gui != 1) ? 0 : 25);
            a2.custom = true;
            a2.gui = a.gui == 1;
            a2.vendor = this.name;
            rpc.Inst.addCustom(a2);
            rpc.Corp$Prod a3 = new rpc.Corp$Prod();
            a3.kind = 2;
            a3.name = s;
            a3.year = a.year;
            a3.price = i;
            a3.quality = rpc.Corp.osQuality(a, this.testing, this.programmers);
            a3.bugs = rpc.Corp.bugsOf(a3.quality);
            a3.dev = i0;
            a3.os = a2;
            a3.lang = a.gui;
            a3.released = true;
            rpc.Corp$Prod[] a4 = this.prods;
            int i2 = this.n;
            this.n = i2 + 1;
            a4[i2] = a3;
            this.reputation = this.reputation + a3.quality / 25;
            return a3;
        }
        rpc.Corp$Prod a5 = null;
        return a5;
    }
    
    public rpc.Corp$Prod releaseSoft(int i, int i0, String s, int i1) {
        if (this.n >= 16) {
            rpc.Corp$Prod a = null;
            return a;
        }
        int i2 = 20 + i0 / 2;
        int i3 = 200 + i0 * 6;
        if (this.rd >= i2 && this.cash >= i3) {
            this.rd = this.rd - i2;
            this.cash = this.cash - i3;
            rpc.Corp$Prod a0 = new rpc.Corp$Prod();
            a0.kind = 3;
            a0.softKind = i;
            a0.name = s;
            a0.year = this.year;
            a0.price = i1;
            if (i0 >= 5) {
                if (i0 > 97) {
                    i0 = 97;
                }
            } else {
                i0 = 5;
            }
            a0.quality = i0;
            a0.bugs = rpc.Corp.bugsOf(a0.quality);
            a0.dev = i2;
            a0.released = true;
            rpc.Corp$Prod[] a1 = this.prods;
            int i4 = this.n;
            this.n = i4 + 1;
            a1[i4] = a0;
            return a0;
        }
        rpc.Corp$Prod a2 = null;
        return a2;
    }
    
    public boolean patch(int i) {
        if (i >= 0 && i < this.n && this.prods[i] != null) {
            rpc.Corp$Prod a = this.prods[i];
            if (a.bugs <= 0) {
                return false;
            }
            int i0 = 20 + a.bugs / 2;
            if (this.rd < i0) {
                return false;
            }
            this.rd = this.rd - i0;
            a.bugs = a.bugs / 3;
            a.quality = a.quality + (100 - a.quality) / 3;
            if (a.quality > 98) {
                a.quality = 98;
            }
            this.reputation = this.reputation + 2;
            return true;
        }
        return false;
    }
    
    public int totalRevenue() {
        long j = 0L;
        int i = 0;
        while(i < this.n) {
            if (this.prods[i] != null) {
                j = j + this.prods[i].revenue;
            }
            i = i + 1;
        }
        return (int)Math.min(j, 2000000000L);
    }
    
    public String line(int i) {
        if (i >= 0 && i < this.n && this.prods[i] != null) {
            rpc.Corp$Prod a = this.prods[i];
            if (a.kind != 3) {
                return new StringBuffer(String.valueOf((Object)KIND_NAME[a.kind])).append(": ").append(a.name).toString();
            }
            return new StringBuffer(String.valueOf((Object)SOFT_NAME[a.softKind])).append(": ").append(a.name).toString();
        }
        return "";
    }
}
