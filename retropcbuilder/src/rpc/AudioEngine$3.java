package rpc;

class AudioEngine$3 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$dust;
    final private int val$note;
    final private int val$eraYear;
    
    AudioEngine$3(rpc.AudioEngine a, int i, int i0, int i1) {
        this.this$0 = a;
        this.val$dust = i;
        this.val$note = i0;
        this.val$eraYear = i1;
    }
    
    public void run() {
        try {
            if (this.val$dust <= 40) {
                if (this.val$eraYear >= 1995) {
                    javax.microedition.media.Manager.playTone(this.val$note, 60, rpc.AudioEngine.access$0(this.this$0) * 50 / 100);
                } else {
                    javax.microedition.media.Manager.playTone(this.val$note, 90, rpc.AudioEngine.access$0(this.this$0) * 75 / 100);
                }
            } else {
                int i = (this.val$dust <= 75) ? 2 : 4;
                int i0 = 0;
                while(i0 < i) {
                    javax.microedition.media.Manager.playTone((i0 % 2 != 0) ? Math.max(21, this.val$note - 3) : this.val$note, 40, rpc.AudioEngine.access$0(this.this$0) * 70 / 100);
                    Thread.sleep(25L);
                    i0 = i0 + 1;
                }
            }
        } catch(Throwable ignoredException) {
        }
    }
}
