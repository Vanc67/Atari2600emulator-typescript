package rpc;

class AudioEngine$7 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$c;
    
    AudioEngine$7(rpc.AudioEngine a, int i) {
        this.this$0 = a;
        this.val$c = i;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0);
            if (this.val$c != 0) {
                if (this.val$c != 2) {
                    if (this.val$c != 3) {
                        if (this.val$c != 4) {
                            if (this.val$c != 1) {
                                javax.microedition.media.Manager.playTone(81, 100, i);
                                Thread.sleep(70L);
                                javax.microedition.media.Manager.playTone(74, 120, i);
                            } else {
                                int i0 = 0;
                                while(i0 < 5) {
                                    javax.microedition.media.Manager.playTone(90, 70, i);
                                    Thread.sleep(50L);
                                    i0 = i0 + 1;
                                }
                            }
                        } else {
                            int i1 = 0;
                            while(i1 < 4) {
                                javax.microedition.media.Manager.playTone(58, 220, i);
                                Thread.sleep(40L);
                                i1 = i1 + 1;
                            }
                        }
                    } else {
                        javax.microedition.media.Manager.playTone(67, 450, i);
                        Thread.sleep(90L);
                        javax.microedition.media.Manager.playTone(67, 140, i);
                    }
                } else {
                    javax.microedition.media.Manager.playTone(76, 350, i);
                    Thread.sleep(80L);
                    javax.microedition.media.Manager.playTone(76, 100, i);
                    Thread.sleep(60L);
                    javax.microedition.media.Manager.playTone(76, 100, i);
                }
            } else {
                javax.microedition.media.Manager.playTone(84, 110, i);
            }
        } catch(Throwable ignoredException) {
        }
    }
}
