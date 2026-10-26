package rpc;

class AudioEngine$11 implements Runnable {
    final rpc.AudioEngine this$0;
    
    AudioEngine$11(rpc.AudioEngine a) {
        this.this$0 = a;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0);
            javax.microedition.media.Manager.playTone(33, 400, i);
            javax.microedition.media.Manager.playTone(28, 500, i);
        } catch(Throwable ignoredException) {
        }
    }
}
