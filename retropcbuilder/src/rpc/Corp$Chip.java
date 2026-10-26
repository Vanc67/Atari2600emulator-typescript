package rpc;

public class Corp$Chip {
    public int cat;
    public int year;
    public int nodeNm;
    public int transistorsM;
    public int mhz;
    public int cores;
    public int cacheKb;
    public int socketIdx;
    public int busIdx;
    public int vramKb;
    public int bits;
    public int ipc;
    
    public Corp$Chip() {
        this.cat = 0;
        this.year = 1985;
        this.nodeNm = 1500;
        this.transistorsM = 1;
        this.mhz = 8;
        this.cores = 1;
        this.cacheKb = 0;
        this.socketIdx = 0;
        this.busIdx = 0;
        this.vramKb = 0;
        this.bits = 8;
        this.ipc = 100;
    }
    
    public rpc.Corp$Chip copy() {
        rpc.Corp$Chip a = new rpc.Corp$Chip();
        a.cat = this.cat;
        a.year = this.year;
        a.nodeNm = this.nodeNm;
        a.transistorsM = this.transistorsM;
        a.mhz = this.mhz;
        a.cores = this.cores;
        a.cacheKb = this.cacheKb;
        a.socketIdx = this.socketIdx;
        a.busIdx = this.busIdx;
        a.vramKb = this.vramKb;
        a.bits = this.bits;
        a.ipc = this.ipc;
        return a;
    }
}
