package rpc;

public class Inst$Os {
    public String name;
    public int year;
    public int minMhz;
    public int minRamKb;
    public int minDiskKb;
    public int baseMin;
    public boolean needTpm;
    public boolean needSecureBoot;
    public boolean needUefi;
    public int patchMask;
    public int patchMinRamKb;
    public int patchMinMhz;
    public boolean custom;
    public boolean gui;
    public String vendor;
    
    public Inst$Os() {
        this.vendor = "";
    }
}
