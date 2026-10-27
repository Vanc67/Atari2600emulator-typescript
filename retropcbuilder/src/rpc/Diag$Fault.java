package rpc;

public class Diag$Fault {
    public int code;
    public int slot;
    public String title;
    public String how;
    
    Diag$Fault(int i, int i0, String s, String s0) {
        this.code = i;
        this.slot = i0;
        this.title = s;
        this.how = s0;
    }
}
