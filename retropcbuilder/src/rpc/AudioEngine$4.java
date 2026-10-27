package rpc;

class AudioEngine$4 implements Runnable {
    final rpc.AudioEngine this$0;
    final private boolean val$oldMfm;
    final private boolean val$fastHdd;
    
    AudioEngine$4(rpc.AudioEngine a, boolean b, boolean b0) {
        this.this$0 = a;
        this.val$oldMfm = b;
        this.val$fastHdd = b0;
    }
    
    public void run() {
        try {
            if (this.val$oldMfm) {
                int[] a = new int[5];
                a[0] = 34;
                a[1] = 38;
                a[2] = 31;
                a[3] = 40;
                a[4] = 36;
                int i = 0;
                while(i < a.length) {
                    javax.microedition.media.Manager.playTone(a[i], 35, rpc.AudioEngine.access$0(this.this$0) * 90 / 100);
                    Thread.sleep(20L);
                    i = i + 1;
                }
            } else if (this.val$fastHdd) {
                int[] a0 = new int[6];
                a0[0] = 48;
                a0[1] = 55;
                a0[2] = 50;
                a0[3] = 58;
                a0[4] = 47;
                a0[5] = 53;
                int i0 = 0;
                while(i0 < a0.length) {
                    javax.microedition.media.Manager.playTone(a0[i0], 16, rpc.AudioEngine.access$0(this.this$0) * 65 / 100);
                    Thread.sleep(12L);
                    i0 = i0 + 1;
                }
            } else {
                int[] a1 = new int[4];
                a1[0] = 42;
                a1[1] = 47;
                a1[2] = 44;
                a1[3] = 49;
                int i1 = 0;
                while(i1 < a1.length) {
                    javax.microedition.media.Manager.playTone(a1[i1], 24, rpc.AudioEngine.access$0(this.this$0) * 75 / 100);
                    Thread.sleep(18L);
                    i1 = i1 + 1;
                }
            }
        } catch(Throwable ignoredException) {
        }
    }
}
