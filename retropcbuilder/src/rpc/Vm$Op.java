package rpc;

class Vm$Op {
    int order;
    int a;
    int v;
    int ve;
    int vei;
    String text;
    
    Vm$Op(int i, int i0, int i1, int i2, int i3, String s) {
        this.order = i;
        this.a = i0;
        this.v = i1;
        this.ve = i2;
        this.vei = i3;
        this.text = s;
    }
}
