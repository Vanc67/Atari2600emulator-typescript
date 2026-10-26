package rpc;

public class WorkshopTools {
    final public static int TOOL_TWEEZERS = 0;
    final public static int TOOL_SOLDER = 1;
    final public static int TOOL_PASTE = 2;
    final public static int TOOL_AIR = 3;
    final public static int TOOL_METER = 4;
    final public static int TOOL_CMOS = 5;
    final public static String[] TOOL_NAMES;
    public int activeTool;
    public int[] cpuPins;
    public int selPin;
    public int[] caps;
    public int selCap;
    public int pasteThickness;
    public int pasteQuality;
    public int dustRemaining;
    public float v12;
    public float v5;
    public float v33;
    public float vCore;
    public int rippleMv;
    public boolean cmosResetDone;
    public String toolFeedback;
    
    static {
        String[] a = new String[6];
        a[0] = "\u041f\u0438\u043d\u0446\u0435\u0442 (\u041d\u043e\u0436\u043a\u0438 CPU)";
        a[1] = "\u041f\u0430\u044f\u043b\u044c\u043d\u0438\u043a (\u041a\u043e\u043d\u0434\u0435\u043d\u0441\u0430\u0442\u043e\u0440\u044b)";
        a[2] = "\u0422\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u0430 (\u0421\u043b\u043e\u0439 \u041a\u041f\u0422-8)";
        a[3] = "\u0421\u0436\u0430\u0442\u044b\u0439 \u0432\u043e\u0437\u0434\u0443\u0445 (\u041f\u044b\u043b\u044c)";
        a[4] = "\u26a1 \u041c\u0443\u043b\u044c\u0442\u0438\u043c\u0435\u0442\u0440 (\u041d\u0430\u043f\u0440\u044f\u0436\u0435\u043d\u0438\u044f)";
        a[5] = "\u0411\u0430\u0442\u0430\u0440\u0435\u0439\u043a\u0430 CMOS (\u0421\u0431\u0440\u043e\u0441)";
        TOOL_NAMES = a;
    }
    
    public WorkshopTools() {
        this.activeTool = 0;
        int[] a = new int[16];
        a[1] = 1;
        a[3] = 2;
        a[6] = 1;
        a[8] = 2;
        a[11] = 1;
        a[14] = 2;
        this.cpuPins = a;
        this.selPin = 0;
        int[] a0 = new int[6];
        a0[1] = 1;
        a0[3] = 2;
        a0[5] = 1;
        this.caps = a0;
        this.selCap = 1;
        this.pasteThickness = 45;
        this.pasteQuality = 90;
        this.dustRemaining = 80;
        this.v12 = 12.08f;
        this.v5 = 5.02f;
        this.v33 = 3.31f;
        this.vCore = 1.285f;
        this.rippleMv = 18;
        this.cmosResetDone = false;
        this.toolFeedback = "\u0418\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u044b \u0433\u043e\u0442\u043e\u0432\u044b \u043a \u0440\u0430\u0431\u043e\u0442\u0435. \u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435.";
    }
    
    public void initForRepair(int i) {
        if (i != 0) {
            if (i != 2) {
                if (i != 3) {
                    this.activeTool = 0;
                    int[] a = new int[16];
                    a[0] = 1;
                    a[2] = 2;
                    a[4] = 1;
                    a[5] = 2;
                    a[8] = 1;
                    a[11] = 2;
                    a[13] = 1;
                    this.cpuPins = a;
                    this.toolFeedback = "\u041d\u0435\u0441\u043a\u043e\u043b\u044c\u043a\u043e \u043d\u043e\u0436\u0435\u043a \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440\u0430 \u043f\u043e\u0433\u043d\u0443\u0442\u044b. \u0410\u043a\u043a\u0443\u0440\u0430\u0442\u043d\u043e \u0432\u044b\u043f\u0440\u044f\u043c\u0438\u0442\u0435 \u0438\u0445 \u043f\u0438\u043d\u0446\u0435\u0442\u043e\u043c.";
                } else {
                    this.activeTool = 4;
                    this.v12 = 10.85f;
                    this.rippleMv = 140;
                    this.toolFeedback = "\u041b\u0438\u043d\u0438\u044f +12V \u043f\u0440\u043e\u0441\u0430\u0436\u0435\u043d\u0430 \u0434\u043e 10.85V, \u043f\u0443\u043b\u044c\u0441\u0430\u0446\u0438\u0438 140\u043c\u0412. \u0411\u041f \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0440\u0435\u043c\u043e\u043d\u0442\u0430.";
                }
            } else {
                this.activeTool = 1;
                int[] a0 = new int[6];
                a0[0] = 1;
                a0[2] = 2;
                a0[4] = 1;
                this.caps = a0;
                this.toolFeedback = "\u041d\u0430 \u043c\u0430\u0442\u0435\u0440\u0438\u043d\u0441\u043a\u043e\u0439 \u043f\u043b\u0430\u0442\u0435 \u043e\u0431\u043d\u0430\u0440\u0443\u0436\u0435\u043d\u044b \u0432\u0437\u0434\u0443\u0442\u044b\u0435 \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043b\u0438\u0442\u044b. \u0412\u044b\u043f\u0430\u044f\u0439\u0442\u0435 \u0438 \u0437\u0430\u043c\u0435\u043d\u0438\u0442\u0435 \u0438\u0445.";
            }
        } else {
            this.activeTool = 3;
            this.dustRemaining = 85;
            this.pasteThickness = 15;
            this.toolFeedback = "\u0420\u0430\u0434\u0438\u0430\u0442\u043e\u0440 \u0437\u0430\u0431\u0438\u0442 \u0432\u043e\u0439\u043b\u043e\u043a\u043e\u043c \u043f\u044b\u043b\u0438. \u041f\u0440\u043e\u0434\u0443\u0439\u0442\u0435 \u0431\u0430\u043b\u043b\u043e\u043d\u043e\u043c \u0438 \u043d\u0430\u043d\u0435\u0441\u0438\u0442\u0435 \u0441\u0432\u0435\u0436\u0443\u044e \u043f\u0430\u0441\u0442\u0443.";
        }
    }
    
    public String useToolAction(int i) {
        if (this.activeTool != 0) {
            if (this.activeTool != 1) {
                if (this.activeTool != 2) {
                    if (this.activeTool != 3) {
                        if (this.activeTool != 4) {
                            this.cmosResetDone = true;
                            rpc.Sound.ok();
                            this.toolFeedback = "\u2713 \u0411\u0430\u0442\u0430\u0440\u0435\u0439\u043a\u0430 CR2032 \u0437\u0430\u043c\u0435\u043d\u0435\u043d\u0430, \u0434\u0436\u0430\u043c\u043f\u0435\u0440 CLR_CMOS \u0437\u0430\u043c\u043a\u043d\u0443\u0442: \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u044b!";
                            return this.toolFeedback;
                        }
                        this.v12 = 12.05f;
                        this.v5 = 5.01f;
                        this.v33 = 3.3f;
                        this.rippleMv = 15;
                        rpc.Sound.ok();
                        this.toolFeedback = new StringBuffer("\u2713 \u041b\u0438\u043d\u0438\u0438 \u043f\u0438\u0442\u0430\u043d\u0438\u044f \u0432 \u043d\u043e\u0440\u043c\u0435: +12V (").append(this.v12).append("V), +5V (").append(this.v5).append("V), \u043f\u0443\u043b\u044c\u0441\u0430\u0446\u0438\u0438 ").append(this.rippleMv).append("\u043c\u0412 (OK).").toString();
                        return this.toolFeedback;
                    } else {
                        this.dustRemaining = Math.max(0, this.dustRemaining - 25);
                        rpc.Sound.fanWithYear(3000, 1995, this.dustRemaining);
                        if (this.dustRemaining != 0) {
                            this.toolFeedback = new StringBuffer("\u041f\u044b\u043b\u044c \u0441\u0434\u0443\u0432\u0430\u0435\u0442\u0441\u044f... \u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c ").append(this.dustRemaining).append("% \u043f\u044b\u043b\u0438 \u0432 \u0440\u0435\u0431\u0440\u0430\u0445.").toString();
                        } else {
                            this.toolFeedback = "\u2713 \u0420\u0430\u0434\u0438\u0430\u0442\u043e\u0440\u044b \u0438 \u043b\u043e\u043f\u0430\u0441\u0442\u0438 \u043a\u0443\u043b\u0435\u0440\u0430 \u0438\u0434\u0435\u0430\u043b\u044c\u043d\u043e \u043e\u0447\u0438\u0449\u0435\u043d\u044b \u043e\u0442 \u043f\u044b\u043b\u0438!";
                            rpc.Sound.ok();
                        }
                        return this.toolFeedback;
                    }
                } else {
                    label3: {
                        label4: {
                            label5: {
                                if (i == 0) {
                                    break label5;
                                }
                                if (i != 3) {
                                    break label4;
                                }
                            }
                            this.pasteThickness = Math.min(100, this.pasteThickness + 10);
                            break label3;
                        }
                        label2: {
                            if (i == 1) {
                                break label2;
                            }
                            if (i == 2) {
                                break label2;
                            }
                            this.pasteThickness = 50;
                            break label3;
                        }
                        this.pasteThickness = Math.max(0, this.pasteThickness - 10);
                    }
                    rpc.Sound.click();
                    int i0 = this.pasteThickness;
                    label1: {
                        label0: {
                            if (i0 < 40) {
                                break label0;
                            }
                            if (this.pasteThickness > 60) {
                                break label0;
                            }
                            this.toolFeedback = new StringBuffer("\u2713 \u0418\u0434\u0435\u0430\u043b\u044c\u043d\u044b\u0439 \u0441\u043b\u043e\u0439 \u0442\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u044b (").append(this.pasteThickness).append("%). \u0422\u0435\u043f\u043b\u043e\u043f\u0440\u043e\u0432\u043e\u0434\u043d\u043e\u0441\u0442\u044c \u043c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u0430!").toString();
                            break label1;
                        }
                        if (this.pasteThickness <= 60) {
                            this.toolFeedback = new StringBuffer("! \u0421\u043b\u0438\u0448\u043a\u043e\u043c \u0442\u043e\u043d\u043a\u0438\u0439 \u0441\u043b\u043e\u0439 (").append(this.pasteThickness).append("%). \u0412\u043e\u0437\u043c\u043e\u0436\u043d\u044b \u0432\u043e\u0437\u0434\u0443\u0448\u043d\u044b\u0435 \u043a\u0430\u0432\u0435\u0440\u043d\u044b.").toString();
                        } else {
                            this.toolFeedback = new StringBuffer("! \u0421\u043b\u0438\u0448\u043a\u043e\u043c \u0442\u043e\u043b\u0441\u0442\u044b\u0439 \u0441\u043b\u043e\u0439 (").append(this.pasteThickness).append("%). \u0422\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u0430 \u0432\u044b\u0434\u0430\u0432\u0438\u0442\u0441\u044f \u043d\u0430 \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b \u0441\u043e\u043a\u0435\u0442\u0430.").toString();
                        }
                    }
                    return this.toolFeedback;
                }
            } else {
                if (this.caps[this.selCap] != 0) {
                    this.caps[this.selCap] = 0;
                    rpc.Sound.ok();
                    this.toolFeedback = new StringBuffer("\u2713 \u0412\u0437\u0434\u0443\u0442\u044b\u0439 \u044d\u043b\u0435\u043a\u0442\u0440\u043e\u043b\u0438\u0442 C").append(this.selCap + 1).append(" \u0432\u044b\u043f\u0430\u044f\u043d, \u0437\u0430\u043f\u0430\u044f\u043d \u043d\u043e\u0432\u044b\u0439 Low-ESR 105\u00b0C!").toString();
                } else {
                    rpc.Sound.click();
                    this.toolFeedback = new StringBuffer("\u041a\u043e\u043d\u0434\u0435\u043d\u0441\u0430\u0442\u043e\u0440 C").append(this.selCap + 1).append(" \u0438\u0441\u043f\u0440\u0430\u0432\u0435\u043d, \u043f\u0430\u0439\u043a\u0430 \u0431\u043b\u0435\u0441\u0442\u0438\u0442.").toString();
                }
                this.selCap = (this.selCap + 1) % this.caps.length;
                return this.toolFeedback;
            }
        } else {
            if (this.cpuPins[this.selPin] != 0) {
                this.cpuPins[this.selPin] = 0;
                rpc.Sound.ok();
                this.toolFeedback = new StringBuffer("\u2713 \u041f\u0438\u043d #").append(this.selPin + 1).append(" \u0438\u0434\u0435\u0430\u043b\u044c\u043d\u043e \u0432\u044b\u043f\u0440\u044f\u043c\u043b\u0435\u043d \u043f\u043e \u043a\u043e\u043d\u0434\u0443\u043a\u0442\u043e\u0440\u0443 \u0441\u043e\u043a\u0435\u0442\u0430!").toString();
            } else {
                rpc.Sound.click();
                this.toolFeedback = new StringBuffer("\u041f\u0438\u043d #").append(this.selPin + 1).append(" \u0443\u0436\u0435 \u043f\u0440\u044f\u043c\u043e\u0439.").toString();
            }
            this.selPin = (this.selPin + 1) % this.cpuPins.length;
            return this.toolFeedback;
        }
    }
    
    public boolean isRepairComplete() {
        int i = 0;
        while(i < this.cpuPins.length) {
            if (this.cpuPins[i] != 0) {
                return false;
            }
            i = i + 1;
        }
        int i0 = 0;
        while(i0 < this.caps.length) {
            if (this.caps[i0] != 0) {
                return false;
            }
            i0 = i0 + 1;
        }
        if (this.dustRemaining > 20) {
            return false;
        }
        if (this.pasteThickness >= 30 && this.pasteThickness <= 75) {
            return true;
        }
        return false;
    }
}
