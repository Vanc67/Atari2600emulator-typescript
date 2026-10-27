package rpc.harness;
import rpc.Inst;
public final class Dbg33 {
    public static void main(String[] a) {
        rpc.Inst$Os[] all = rpc.Inst.all();
        for (int i = 0; i < all.length; i++) {
            System.out.println(all[i].name + " y=" + all[i].year + " minDiskKb=" + all[i].minDiskKb);
        }
    }
}
