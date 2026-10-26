package rpc;

public class SoftSim {
    final public static int APP_FLOPPY = 0;
    final public static int APP_DEFRAG = 1;
    final public static int APP_MODEM = 2;
    final public static int APP_PIPES = 3;
    final public static int APP_REGEDIT = 4;
    public int activeApp;
    public int floppyCurrent;
    public int floppyTotal;
    public int floppyProgress;
    public boolean floppyError;
    public String floppyStatus;
    public int[] clusterMap;
    public int defragProgress;
    public int curScanCluster;
    public int pipeTick;
    public int downloadKb;
    public int downloadTotalKb;
    public int modemSpeedKbps;
    public boolean lineBusy;
    public String[] regKeys;
    public int selRegKey;
    
    public SoftSim() {
        this.activeApp = 0;
        this.floppyCurrent = 1;
        this.floppyTotal = 20;
        this.floppyProgress = 0;
        this.floppyError = false;
        this.floppyStatus = "\u0412\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u0434\u0438\u0441\u043a\u0435\u0442\u0443 #1 \u00abMicrosoft Setup Disk 1\u00bb \u0432 \u043f\u0440\u0438\u0432\u043e\u0434 A:";
        this.clusterMap = new int[96];
        this.defragProgress = 0;
        this.curScanCluster = 0;
        this.pipeTick = 0;
        this.downloadKb = 0;
        this.downloadTotalKb = 1440;
        this.modemSpeedKbps = 33;
        this.lineBusy = false;
        String[] a = new String[5];
        a[0] = "HKLM\\System\\CurrentControlSet\\Services\\VxD";
        a[1] = "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
        a[2] = "HKCU\\Control Panel\\Desktop\\ScreenSaveActive = 1";
        a[3] = "HKLM\\Hardware\\Description\\System\\CentralProcessor\\0";
        a[4] = "HKLM\\System\\Overclock\\VCore_Offset_Mv = +50";
        this.regKeys = a;
        this.selRegKey = 0;
        this.initDefrag();
    }
    
    public void initFloppyInstall() {
        this.activeApp = 0;
        this.floppyCurrent = 1;
        this.floppyProgress = 0;
        this.floppyError = false;
        this.floppyStatus = "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 Windows 95: \u0447\u0442\u0435\u043d\u0438\u0435 \u0434\u0438\u0441\u043a\u0435\u0442\u044b #1 (IO.SYS / MSDOS.SYS)...";
        rpc.Sound.disk(false, true);
    }
    
    public void tickFloppy() {
        if (this.floppyProgress < 100) {
            this.floppyProgress = this.floppyProgress + 10;
            if (this.floppyProgress % 30 == 0) {
                rpc.Sound.disk(false, false);
            }
            if (this.floppyProgress >= 100) {
                if (this.floppyCurrent >= this.floppyTotal) {
                    this.floppyStatus = "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 Windows 95 \u0443\u0441\u043f\u0435\u0448\u043d\u043e \u0437\u0430\u0432\u0435\u0440\u0448\u0435\u043d\u0430! \u0421\u0438\u0441\u0442\u0435\u043c\u0430 \u0433\u043e\u0442\u043e\u0432\u0430 \u043a \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0435.";
                    rpc.Sound.ok();
                } else {
                    this.floppyStatus = new StringBuffer("\u2713 \u0414\u0438\u0441\u043a\u0435\u0442\u0430 #").append(this.floppyCurrent).append(" \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u043d\u0430! \u0412\u0441\u0442\u0430\u0432\u044c\u0442\u0435 \u0434\u0438\u0441\u043a\u0435\u0442\u0443 #").append(this.floppyCurrent + 1).append(" [\u041d\u0430\u0436\u043c\u0438\u0442\u0435 5/OK]").toString();
                    rpc.Sound.ok();
                }
            }
        }
    }
    
    public void nextFloppyDisk() {
        if (this.floppyCurrent < this.floppyTotal && this.floppyProgress >= 100) {
            this.floppyCurrent = this.floppyCurrent + 1;
            this.floppyProgress = 0;
            this.floppyStatus = new StringBuffer("\u0427\u0442\u0435\u043d\u0438\u0435 \u0444\u0430\u0439\u043b\u043e\u0432 \u0434\u0438\u0441\u043a\u0435\u0442\u044b #").append(this.floppyCurrent).append(" \u0438\u0437 ").append(this.floppyTotal).append("...").toString();
            rpc.Sound.disk(false, true);
        }
    }
    
    public void initDefrag() {
        int i = 0;
        while(i < this.clusterMap.length) {
            if (i >= 8) {
                if (i % 3 != 0) {
                    if (i % 5 != 0) {
                        this.clusterMap[i] = 2;
                    } else {
                        this.clusterMap[i] = 0;
                    }
                } else {
                    this.clusterMap[i] = 1;
                }
            } else {
                this.clusterMap[i] = 3;
            }
            i = i + 1;
        }
        this.defragProgress = 35;
        this.curScanCluster = 8;
    }
    
    public void tickDefrag() {
        if (this.curScanCluster < this.clusterMap.length) {
            if (this.clusterMap[this.curScanCluster] != 0) {
                this.clusterMap[this.curScanCluster] = 2;
                rpc.Sound.disk(false, true);
            }
            this.curScanCluster = this.curScanCluster + 1;
            this.defragProgress = Math.min(100, this.curScanCluster * 100 / this.clusterMap.length);
            if (this.defragProgress >= 100) {
                rpc.Sound.ok();
            }
        }
    }
    
    public void tickDownload() {
        if (!this.lineBusy && this.downloadKb < this.downloadTotalKb) {
            this.downloadKb = this.downloadKb + Math.max(2, this.modemSpeedKbps / 8);
            if (this.downloadKb >= this.downloadTotalKb) {
                this.downloadKb = this.downloadTotalKb;
                rpc.Sound.ok();
            }
        }
    }
    
    public void triggerPhoneInterference() {
        this.lineBusy = true;
        rpc.Sound.error();
    }
}
