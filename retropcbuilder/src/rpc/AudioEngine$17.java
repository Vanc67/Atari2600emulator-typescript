package rpc;

class AudioEngine$17 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$17(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0) * 70 / 100;
            javax.microedition.media.Manager.playTone(34, 20, i);
            Thread.sleep(10L);
            javax.microedition.media.Manager.playTone(30, 25, i);
        } catch(Throwable ignoredException) {
        }
    }
}
