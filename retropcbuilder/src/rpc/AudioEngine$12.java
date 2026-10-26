package rpc;

class AudioEngine$12 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$12(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0);
            int[] a = new int[10];
            a[0] = 52;
            a[1] = 52;
            a[2] = 64;
            a[3] = 52;
            a[4] = 62;
            a[5] = 52;
            a[6] = 60;
            a[7] = 52;
            a[8] = 58;
            a[9] = 59;
            int i0 = 0;
            while(i0 < a.length) {
                javax.microedition.media.Manager.playTone(a[i0], 65, i);
                Thread.sleep(35L);
                i0 = i0 + 1;
            }
        } catch(Throwable ignoredException) {
        }
    }
}
