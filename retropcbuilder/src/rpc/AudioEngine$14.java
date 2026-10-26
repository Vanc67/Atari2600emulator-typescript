package rpc;

class AudioEngine$14 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$14(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0) * 50 / 100;
            int i0 = 0;
            while(i0 < 3) {
                javax.microedition.media.Manager.playTone(28, 45, i);
                Thread.sleep(15L);
                javax.microedition.media.Manager.playTone(26, 45, i);
                Thread.sleep(25L);
                i0 = i0 + 1;
            }
        } catch(Throwable ignoredException) {
        }
    }
}
