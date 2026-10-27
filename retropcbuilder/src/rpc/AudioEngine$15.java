package rpc;

class AudioEngine$15 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$station;
    
    AudioEngine$15(rpc.AudioEngine a, int i) {
        this.this$0 = a;
        this.val$station = i;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0) * 65 / 100;
            int i0 = 60 + this.val$station * 4 % 12;
            int[] a = new int[6];
            a[0] = i0;
            a[1] = i0 + 4;
            a[2] = i0 + 7;
            a[3] = i0 + 12;
            a[4] = i0 + 7;
            a[5] = i0 + 4;
            int i1 = 0;
            while(i1 < a.length) {
                javax.microedition.media.Manager.playTone(a[i1], 60, i);
                Thread.sleep(25L);
                i1 = i1 + 1;
            }
        } catch(Throwable ignoredException) {
        }
    }
}
