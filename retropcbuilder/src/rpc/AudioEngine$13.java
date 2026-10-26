package rpc;

class AudioEngine$13 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$intensity;
    
    AudioEngine$13(rpc.AudioEngine a, int i) {
        this.this$0 = a;
        this.val$intensity = i;
    }
    
    public void run() {
        try {
            int i = Math.min(60, rpc.AudioEngine.access$0(this.this$0) * this.val$intensity / 100);
            int i0 = 0;
            while(i0 < 4) {
                javax.microedition.media.Manager.playTone(24 + i0 * 3 % 7, 30, i);
                Thread.sleep(20L);
                i0 = i0 + 1;
            }
        } catch(Throwable ignoredException) {
        }
    }
}
