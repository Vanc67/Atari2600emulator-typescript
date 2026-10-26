package rpc;

class AudioEngine$16 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$tool;
    
    AudioEngine$16(rpc.AudioEngine a, int i) {
        this.this$0 = a;
        this.val$tool = i;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0) * 75 / 100;
            if (this.val$tool != 0) {
                if (this.val$tool != 1) {
                    if (this.val$tool != 3) {
                        javax.microedition.media.Manager.playTone(70, 30, i);
                    } else {
                        javax.microedition.media.Manager.playTone(30, 80, i);
                        javax.microedition.media.Manager.playTone(28, 90, i);
                    }
                } else {
                    javax.microedition.media.Manager.playTone(36, 40, i);
                    javax.microedition.media.Manager.playTone(38, 50, i);
                }
            } else {
                javax.microedition.media.Manager.playTone(92, 15, i);
                Thread.sleep(5L);
                javax.microedition.media.Manager.playTone(88, 20, i);
            }
        } catch(Throwable ignoredException) {
        }
    }
}
