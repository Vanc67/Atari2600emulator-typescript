package rpc;

class AudioEngine$1 implements Runnable {
    final rpc.AudioEngine this$0;
    final private boolean val$isHz;
    final private int val$noteOrHz;
    final private int val$volume;
    final private int val$durationMs;
    
    AudioEngine$1(rpc.AudioEngine a, boolean b, int i, int i0, int i1) {
        this.this$0 = a;
        this.val$isHz = b;
        this.val$noteOrHz = i;
        this.val$volume = i0;
        this.val$durationMs = i1;
    }
    
    public void run() {
        try {
            int i = (this.val$isHz) ? rpc.AudioEngine.hzToMidiNote(this.val$noteOrHz) : this.val$noteOrHz;
            if (i < 0) {
                i = 0;
            }
            if (i > 127) {
                i = 127;
            }
            int i0 = this.val$volume * rpc.AudioEngine.access$0(this.this$0) / 100;
            if (i0 < 0) {
                i0 = 0;
            }
            if (i0 > 100) {
                i0 = 100;
            }
            javax.microedition.media.Manager.playTone(i, Math.max(10, this.val$durationMs), i0);
        } catch(Throwable ignoredException) {
        }
    }
}
