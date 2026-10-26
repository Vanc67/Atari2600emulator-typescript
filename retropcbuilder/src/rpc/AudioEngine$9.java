package rpc;

class AudioEngine$9 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$9(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0);
            javax.microedition.media.Manager.playTone(45, 110, i);
            Thread.sleep(30L);
            javax.microedition.media.Manager.playTone(38, 140, i);
        } catch(Throwable ignoredException) {
        }
    }
}
