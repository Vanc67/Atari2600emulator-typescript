package rpc;

class AudioEngine$5 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$5(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            javax.microedition.media.Manager.playTone(28, 70, rpc.AudioEngine.access$0(this.this$0) * 85 / 100);
            Thread.sleep(40L);
            javax.microedition.media.Manager.playTone(24, 60, rpc.AudioEngine.access$0(this.this$0) * 90 / 100);
            Thread.sleep(30L);
            int i = 0;
            while(i < 4) {
                javax.microedition.media.Manager.playTone(33 + i % 2 * 3, 25, rpc.AudioEngine.access$0(this.this$0) * 80 / 100);
                Thread.sleep(35L);
                i = i + 1;
            }
        } catch(Throwable ignoredException) {
        }
    }
}
