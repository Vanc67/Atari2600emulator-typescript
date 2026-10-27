package rpc;

public class Table {
    final public static int MAX = 12;
    public rpc.Table$Order[] orders;
    public rpc.Part[] parts;
    public int[] cond;
    public int[] dust;
    public int n;
    
    public Table() {
        this.orders = new rpc.Table$Order[0];
        this.parts = new rpc.Part[12];
        this.cond = new int[12];
        this.dust = new int[12];
        this.n = 0;
    }
    
    public void order(rpc.Part a, int i, int i0, int i1, int i2) {
        this.order(a, i, i0, 0, i1, i2);
    }
    
    public void order(rpc.Part a, int i, int i0, int i1, int i2, int i3) {
        rpc.Table$Order a0 = new rpc.Table$Order();
        a0.part = a;
        a0.price = i;
        a0.cond = i0;
        a0.dust = i1;
        a0.arriveDay = i2;
        a0.market = i3;
        rpc.Table$Order[] a1 = new rpc.Table$Order[this.orders.length + 1];
        System.arraycopy((Object)this.orders, 0, (Object)a1, 0, this.orders.length);
        a1[this.orders.length] = a0;
        this.orders = a1;
    }
    
    public int pending() {
        return this.orders.length;
    }
    
    public int soonestDay() {
        int i = -1;
        int i0 = 0;
        while(i0 < this.orders.length) {
            label0: {
                label1: {
                    if (i < 0) {
                        break label1;
                    }
                    if (this.orders[i0].arriveDay >= i) {
                        break label0;
                    }
                }
                i = this.orders[i0].arriveDay;
            }
            i0 = i0 + 1;
        }
        return i;
    }
    
    public int tick(int i) {
        if (this.orders.length == 0) {
            return 0;
        }
        int i0 = 0;
        int i1 = 0;
        while(i1 < this.orders.length) {
            if (this.orders[i1].arriveDay <= i && this.n < 12) {
                this.addToTable(this.orders[i1].part, this.orders[i1].cond, this.orders[i1].dust);
                this.orders[i1] = null;
                i0 = i0 + 1;
            }
            i1 = i1 + 1;
        }
        if (i0 > 0) {
            int i2 = 0;
            int i3 = 0;
            while(i3 < this.orders.length) {
                if (this.orders[i3] != null) {
                    i2 = i2 + 1;
                }
                i3 = i3 + 1;
            }
            rpc.Table$Order[] a = new rpc.Table$Order[i2];
            int i4 = 0;
            int i5 = 0;
            while(i5 < this.orders.length) {
                if (this.orders[i5] != null) {
                    int i6 = i4 + 1;
                    a[i4] = this.orders[i5];
                    i4 = i6;
                }
                i5 = i5 + 1;
            }
            this.orders = a;
        }
        return i0;
    }
    
    public void addToTable(rpc.Part a, int i) {
        this.addToTable(a, i, 0);
    }
    
    public void addToTable(rpc.Part a, int i, int i0) {
        if (a != null && this.n < 12) {
            this.parts[this.n] = a;
            this.cond[this.n] = i;
            this.dust[this.n] = i0;
            this.n = this.n + 1;
            return;
        }
    }
    
    public void removeAt(int i) {
        if (i >= 0 && i < this.n) {
            while(i + 1 < this.n) {
                this.parts[i] = this.parts[i + 1];
                this.cond[i] = this.cond[i + 1];
                this.dust[i] = this.dust[i + 1];
                i = i + 1;
            }
            this.parts[this.n - 1] = null;
            this.cond[this.n - 1] = 100;
            this.dust[this.n - 1] = 0;
            this.n = this.n - 1;
            return;
        }
    }
}
