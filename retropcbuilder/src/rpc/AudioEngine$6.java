package rpc;

class AudioEngine$6 implements Runnable {
    final rpc.AudioEngine this$0;
    final private int val$speedKbps;
    
    AudioEngine$6(rpc.AudioEngine a, int i) {
        this.this$0 = a;
        this.val$speedKbps = i;
    }
    
    public void run() {
        try {
            int i = rpc.AudioEngine.access$0(this.this$0) * 85 / 100;
            javax.microedition.media.Manager.playTone(69, 300, i);
            Thread.sleep(120L);
            int[] a = new int[7];
            a[0] = 72;
            a[1] = 77;
            a[2] = 74;
            a[3] = 79;
            a[4] = 76;
            a[5] = 72;
            a[6] = 81;
            int i0 = 0;
            while(i0 < a.length) {
                javax.microedition.media.Manager.playTone(a[i0], 50, i);
                Thread.sleep(35L);
                i0 = i0 + 1;
            }
            Thread.sleep(200L);
            javax.microedition.media.Manager.playTone(96, 450, i);
            Thread.sleep(60L);
            if (this.val$speedKbps > 14) {
                if (this.val$speedKbps > 33) {
                    javax.microedition.media.Manager.playTone(89, 120, i);
                    javax.microedition.media.Manager.playTone(95, 140, i);
                    int i1 = 0;
                    while(i1 < 3) {
                        javax.microedition.media.Manager.playTone(70 + i1 * 6 % 15, 25, i);
                        Thread.sleep(15L);
                        i1 = i1 + 1;
                    }
                } else {
                    javax.microedition.media.Manager.playTone(88, 180, i);
                    javax.microedition.media.Manager.playTone(93, 200, i);
                    int i2 = 0;
                    while(i2 < 4) {
                        javax.microedition.media.Manager.playTone(62 + i2 * 9 % 18, 30, i);
                        Thread.sleep(20L);
                        i2 = i2 + 1;
                    }
                }
            } else {
                javax.microedition.media.Manager.playTone(84, 250, i);
                javax.microedition.media.Manager.playTone(91, 300, i);
                int i3 = 0;
                while(i3 < 6) {
                    javax.microedition.media.Manager.playTone(55 + i3 * 7 % 20, 40, i);
                    Thread.sleep(30L);
                    i3 = i3 + 1;
                }
            }
        } catch(Throwable ignoredException) {
        }
    }
}
