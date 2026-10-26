package rpc;

class AudioEngine$10 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$10(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0);
            javax.microedition.media.Manager.playTone(67, 70, i);
            Thread.sleep(20L);
            javax.microedition.media.Manager.playTone(72, 70, i);
            Thread.sleep(20L);
            javax.microedition.media.Manager.playTone(76, 90, i);
            Thread.sleep(20L);
            javax.microedition.media.Manager.playTone(84, 180, i);
        } catch(Throwable ignoredException) {
        }
    }
}
