package rpc;

public class ExpStress {
    final public static int ST_IDLE = 0;
    final public static int ST_RUNNING = 1;
    final public static int ST_SUCCESS = 2;
    final public static int ST_BSOD = 3;
    final public static int ST_THROTTLE = 4;
    final public static int ST_SMOKE = 5;
    final public static String[] COOLING_NAMES;
    public int expId;
    public int status;
    public int curCooling;
    public int activeTweaks;
    public int curFreq;
    public int curVoltage;
    public int temp;
    public int power;
    public int score;
    public int fps;
    public int frameCount;
    public int progress;
    public int stockTemp;
    public int optMask;
    public int diff;
    private String[] logs;
    private int logCount;
    
    static {
        String[] a = new String[5];
        a[0] = "\u0411\u043e\u043a\u0441\u043e\u0432\u044b\u0439 \u043a\u0443\u043b\u0435\u0440";
        a[1] = "\u041c\u0435\u0434\u043d\u0430\u044f \u0431\u0430\u0448\u043d\u044f";
        a[2] = "\u0421\u0416\u041e 360\u043c\u043c";
        a[3] = "\u041a\u0430\u0441\u0442\u043e\u043c\u043d\u0430\u044f \u0421\u0412\u041e";
        a[4] = "\u0416\u0438\u0434\u043a\u0438\u0439 \u0430\u0437\u043e\u0442 LN2 (-196\u00b0C)";
        COOLING_NAMES = a;
    }
    
    public ExpStress() {
        this.logs = new String[32];
        this.status = 0;
        this.curCooling = 0;
        this.activeTweaks = 0;
        this.logs = new String[32];
        this.logCount = 0;
    }
    
    public void load(int i) {
        label0: {
            label1: {
                if (i < 0) {
                    break label1;
                }
                if (i < 1000) {
                    break label0;
                }
            }
            i = 0;
        }
        this.expId = i;
        this.status = 0;
        this.curFreq = rpc.Exp.baseFreq(i);
        this.curVoltage = rpc.Exp.vcore(i);
        this.stockTemp = rpc.Exp.stockTemp(i);
        this.optMask = rpc.Exp.optMask(i);
        this.diff = rpc.Exp.diff(i);
        this.curCooling = 0;
        this.activeTweaks = 0;
        this.frameCount = 0;
        this.progress = 0;
        this.score = 0;
        this.fps = 0;
        this.logCount = 0;
        this.addLog(new StringBuffer("\u0418\u043d\u0438\u0446\u0438\u0430\u043b\u0438\u0437\u0430\u0446\u0438\u044f \u0441\u0442\u0435\u043d\u0434\u0430: ").append(rpc.Exp.title(i)).toString());
        this.addLog(new StringBuffer("\u041f\u043b\u0430\u0442\u0444\u043e\u0440\u043c\u0430: ").append(rpc.Exp.platformName(i)).append(" | \u0411\u0430\u0437\u0430: ").append(this.curFreq).append("MHz, ").append(this.curVoltage).append("mV").toString());
        this.recalcThermal();
    }
    
    public void recalcThermal() {
        int i = 0;
        int i0 = 0;
        int i1 = Math.max(1, rpc.Exp.baseFreq(this.expId));
        int i2 = Math.max(1, rpc.Exp.vcore(this.expId));
        int i3 = this.curFreq * 100 / i1;
        int i4 = this.curVoltage * 100 / i2;
        int i5 = Math.max(5, this.stockTemp * 2 / 3) * i3 / 100 * i4 / 100 * i4 / 100;
        if ((this.activeTweaks & 1) != 0) {
            i5 = i5 * 85 / 100;
        }
        if ((this.activeTweaks & 4) != 0) {
            i5 = i5 * 90 / 100;
        }
        this.power = Math.max(2, i5);
        switch(this.curCooling) {
            case 4: {
                i = -180;
                break;
            }
            case 3: {
                i = -50;
                break;
            }
            case 2: {
                i = -35;
                break;
            }
            case 1: {
                i = -18;
                break;
            }
            default: {
                i = 0;
            }
        }
        if ((this.activeTweaks & 32) != 0 && this.curCooling != 4) {
            i = i + -40;
        }
        int i6 = this.stockTemp + this.power / 4 + i;
        this.temp = (this.curCooling != 4) ? Math.max(15, i6) : Math.max(-196, Math.min(25, -120 + this.power / 3));
        if (rpc.Exp.kind(this.expId) != 1) {
            i0 = Math.min(120, this.curFreq * 60 / i1);
        } else {
            i0 = this.curFreq * 60 / Math.max(1, rpc.Exp.maxFreq(this.expId));
            if ((this.activeTweaks & 1) != 0) {
                i0 = i0 + 20;
            }
            if ((this.activeTweaks & 8) != 0) {
                i0 = i0 + 15;
            }
            if ((this.activeTweaks & 128) != 0) {
                i0 = i0 + 40;
            }
        }
        this.fps = Math.max(1, i0);
    }
    
    public void start() {
        if (this.status == 1) {
            return;
        }
        this.status = 1;
        this.frameCount = 0;
        this.progress = 0;
        this.score = 0;
        this.addLog(">>> \u0417\u0430\u043f\u0443\u0441\u043a \u0441\u0442\u0440\u0435\u0441\u0441-\u0442\u0435\u0441\u0442\u0430 \u043d\u0430 \u0441\u0442\u0435\u043d\u0434\u0435...");
        this.recalcThermal();
    }
    
    public void tick() {
        if (this.status != 1) {
            return;
        }
        this.frameCount = this.frameCount + 1;
        this.progress = Math.min(100, this.frameCount * 5);
        int i = this.frameCount * 3 / 2;
        if (this.curCooling != 4) {
            this.temp = this.temp + ((this.frameCount % 2 != 0) ? 0 : 1);
        } else {
            this.temp = Math.min(10, -110 + this.power / 2 + i);
        }
        int i0 = rpc.Exp.vcore(this.expId) * 16 / 10;
        if (this.curVoltage > i0 && (this.activeTweaks & 16) == 0) {
            this.status = 5;
            this.addLog(new StringBuffer("\u041a\u0420\u0418\u0422\u0418\u0427\u0415\u0421\u041a\u0418\u0419 \u0421\u0411\u041e\u0419: \u041f\u0440\u043e\u0433\u0430\u0440 \u0446\u0435\u043f\u0435\u0439 \u043f\u0438\u0442\u0430\u043d\u0438\u044f VCore (").append(this.curVoltage).append("mV)!").toString());
            rpc.Sound.overheat();
            return;
        }
        if (this.temp >= 95 && this.curCooling < 3) {
            this.status = 4;
            this.addLog(new StringBuffer("\u041f\u0415\u0420\u0415\u0413\u0420\u0415\u0412: \u0414\u043e\u0441\u0442\u0438\u0433\u043d\u0443\u0442\u0430 \u0442\u0435\u043c\u043f\u0435\u0440\u0430\u0442\u0443\u0440\u0430 ").append(this.temp).append("\u00b0C! \u0421\u0440\u0430\u0431\u043e\u0442\u0430\u043b \u0442\u0440\u043e\u0442\u0442\u043b\u0438\u043d\u0433.").toString());
            rpc.Sound.overheat();
            return;
        }
        if (this.diff >= 3 && this.frameCount >= 10 && this.curCooling < 4 && (this.optMask & (this.activeTweaks ^ -1)) != 0 && (this.activeTweaks & 4) == 0) {
            this.status = 3;
            this.addLog(new StringBuffer("BSOD: \u041d\u0435\u0445\u0432\u0430\u0442\u043a\u0430 \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u044b\u0445 \u0440\u0435\u0441\u0443\u0440\u0441\u043e\u0432! \u0422\u0440\u0435\u0431\u0443\u044e\u0442\u0441\u044f \u0442\u0432\u0438\u043a\u0438 (\u043c\u0430\u0441\u043a\u0430: 0x").append(Integer.toHexString(this.optMask)).append(").").toString());
            rpc.Sound.bsod();
            return;
        }
        int i1 = this.frameCount;
        label0: {
            label1: {
                if (i1 >= 20) {
                    break label1;
                }
                if (this.progress < 100) {
                    break label0;
                }
            }
            this.status = 2;
            this.progress = 100;
            this.score = Math.max(100, this.curFreq * 10 / Math.max(1, this.diff) + rpc.Util.bitCount(this.activeTweaks) * 50 + rpc.Exp.xp(this.expId) * 3);
            this.addLog(new StringBuffer("\u0423\u0421\u041f\u0415\u0425! \u0421\u0442\u0440\u0435\u0441\u0441-\u0442\u0435\u0441\u0442 \u043f\u0440\u043e\u0439\u0434\u0435\u043d \u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e. \u0420\u0435\u0437\u0443\u043b\u044c\u0442\u0430\u0442: ").append(this.score).append(" pts").toString());
            rpc.Sound.ok();
        }
    }
    
    public void addLog(String s) {
        if (s == null) {
            return;
        }
        if (this.logCount >= this.logs.length) {
            int i = 0;
            while(i < this.logs.length - 1) {
                this.logs[i] = this.logs[i + 1];
                i = i + 1;
            }
            this.logs[this.logs.length - 1] = s;
        } else {
            String[] a = this.logs;
            int i0 = this.logCount;
            this.logCount = i0 + 1;
            a[i0] = s;
        }
    }
    
    public int logSize() {
        return this.logCount;
    }
    
    public String getLog(int i) {
        if (i >= 0 && i < this.logCount) {
            return this.logs[i];
        }
        return "";
    }
}
