package rpc;

class AudioEngine$2 implements Runnable {
    final rpc.AudioEngine this$0;
    final private byte[] val$seq;
    
    AudioEngine$2(rpc.AudioEngine a, byte[] a0) {
        this.this$0 = a;
        this.val$seq = a0;
    }
    
    public void run() {
        Object a = null;
        try {
            a = null;
            javax.microedition.media.Player a0 = javax.microedition.media.Manager.createPlayer("device://tone");
            a = a0;
            a0.realize();
            javax.microedition.media.Control a1 = a0.getControl("ToneControl");
            if (a1 == null) {
                a = a0;
            } else {
                a = a0;
                ((javax.microedition.media.control.ToneControl)(Object)a1).setSequence(this.val$seq);
                javax.microedition.media.Control a2 = a0.getControl("VolumeControl");
                if (a2 != null) {
                    a = a0;
                    ((javax.microedition.media.control.VolumeControl)(Object)a2).setLevel(rpc.AudioEngine.access$0(this.this$0));
                }
                a = a0;
                a0.start();
                a = a0;
                while(a != null) {
                    {
                        if (((javax.microedition.media.Player)a).getState() == 400) {
                            try {
                                Thread.sleep(50L);
                                continue;
                            } catch(InterruptedException ignoredException) {
                            }
                        }
                        break;
                    }
                }
            }
        } catch(Throwable ignoredException0) {
        }
        rpc.AudioEngine.access$1((javax.microedition.media.Player)a);
    }
}
