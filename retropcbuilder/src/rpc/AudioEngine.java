package rpc;

public class AudioEngine {
    private static rpc.AudioEngine instance;
    private boolean soundEnabled;
    private int masterVolume;
    private javax.microedition.media.Player fanPlayer;
    private javax.microedition.media.Player seekPlayer;
    final private Object lock;
    final private byte[] toneSeqBuf;
    
    public static synchronized rpc.AudioEngine getInstance() {
        if (instance == null) {
            instance = new rpc.AudioEngine();
        }
        return instance;
    }
    
    public AudioEngine() {
        this.soundEnabled = true;
        this.masterVolume = 85;
        this.fanPlayer = null;
        this.seekPlayer = null;
        this.lock = new Object();
        this.toneSeqBuf = new byte[128];
    }
    
    public void setEnabled(boolean b) {
        this.soundEnabled = b;
        if (!b) {
            this.stopAll();
        }
    }
    
    public boolean isEnabled() {
        return this.soundEnabled;
    }
    
    public void setMasterVolume(int i) {
        if (i < 0) {
            i = 0;
        }
        if (i > 100) {
            i = 100;
        }
        this.masterVolume = i;
    }
    
    public int getMasterVolume() {
        return this.masterVolume;
    }
    
    public static int hzToMidiNote(int i) {
        if (i <= 20) {
            return 21;
        }
        if (i >= 4186) {
            return 108;
        }
        if (i < 65) {
            return 24 + (i - 32) * 12 / 33;
        }
        if (i < 130) {
            return 36 + (i - 65) * 12 / 65;
        }
        if (i < 261) {
            return 48 + (i - 130) * 12 / 131;
        }
        if (i < 523) {
            return 60 + (i - 261) * 12 / 262;
        }
        if (i < 1046) {
            return 72 + (i - 523) * 12 / 523;
        }
        if (i < 2093) {
            return 84 + (i - 1046) * 12 / 1047;
        }
        return Math.min(108, 96 + (i - 2093) * 12 / 2093);
    }
    
    public void playToneSafe(int i, int i0, int i1, boolean b) {
        if (this.soundEnabled && this.masterVolume > 0 && i0 > 0) {
            new Thread((Runnable)(Object)new rpc.AudioEngine$1(this, b, i, i1, i0)).start();
            return;
        }
    }
    
    public void playSequenceSafe(byte[] a) {
        if (this.soundEnabled && this.masterVolume > 0 && a != null && a.length >= 2) {
            new Thread((Runnable)(Object)new rpc.AudioEngine$2(this, a)).start();
            return;
        }
    }
    
    private static void safeClosePlayer(javax.microedition.media.Player a) {
        if (a == null) {
            return;
        }
        try {
            if (a.getState() == 400) {
                a.stop();
            }
            a.deallocate();
            a.close();
        } catch(Throwable ignoredException) {
        }
    }
    
    public void stopAll() {
        synchronized (this.lock) {
            if (this.fanPlayer != null) {
                rpc.AudioEngine.safeClosePlayer(this.fanPlayer);
                this.fanPlayer = null;
            }
            if (this.seekPlayer != null) {
                rpc.AudioEngine.safeClosePlayer(this.seekPlayer);
                this.seekPlayer = null;
            }
        }
    }
    
    public void playFanNoise(int i, int i0, int i1) {
        if (!this.soundEnabled) {
            return;
        }
        int i2 = (i0 >= 1995) ? (i0 >= 2012) ? 180 + i / 40 : 100 + i / 50 : 45 + i / 80;
        if (i1 > 0) {
            i2 = Math.max(30, i2 - i1 * 25 / 100);
        }
        new Thread((Runnable)(Object)new rpc.AudioEngine$3(this, i1, rpc.AudioEngine.hzToMidiNote(i2), i0)).start();
    }
    
    public void playDiskAccess(int i, int i0, int i1, boolean b) {
        boolean b0 = false;
        if (!this.soundEnabled) {
            return;
        }
        if (i == 7) {
            return;
        }
        if (i == 6) {
            this.playFloppySeek();
            return;
        }
        label2: {
            label0: {
                label1: {
                    if (i1 < 1992) {
                        break label1;
                    }
                    if (i0 > 4200) {
                        break label0;
                    }
                }
                b0 = true;
                break label2;
            }
            b0 = false;
        }
        new Thread((Runnable)(Object)new rpc.AudioEngine$4(this, b0, i0 >= 7200)).start();
    }
    
    private void playFloppySeek() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$5(this)).start();
    }
    
    public void playModemHandshake(int i) {
        if (!this.soundEnabled) {
            return;
        }
        new Thread((Runnable)(Object)new rpc.AudioEngine$6(this, i)).start();
    }
    
    public void playPostBeep(int i) {
        if (!this.soundEnabled) {
            return;
        }
        new Thread((Runnable)(Object)new rpc.AudioEngine$7(this, i)).start();
    }
    
    public void playKeyClick() {
        this.playToneSafe(92, 12, 70, false);
    }
    
    public void playSuccess() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$8(this)).start();
    }
    
    public void playError() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$9(this)).start();
    }
    
    public void playStartupSound() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$10(this)).start();
    }
    
    public void playBsod() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$11(this)).start();
    }
    
    public void playDoomRiff() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$12(this)).start();
    }
    
    public void playRainNoise(int i) {
        new Thread((Runnable)(Object)new rpc.AudioEngine$13(this, i)).start();
    }
    
    public void playCatPurr() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$14(this)).start();
    }
    
    public void playRadioChiptune(int i) {
        new Thread((Runnable)(Object)new rpc.AudioEngine$15(this, i)).start();
    }
    
    public void playToolSound(int i) {
        new Thread((Runnable)(Object)new rpc.AudioEngine$16(this, i)).start();
    }
    
    public void playFloppyStep() {
        new Thread((Runnable)(Object)new rpc.AudioEngine$17(this)).start();
    }
    
    static int access$0(rpc.AudioEngine a) {
        return a.masterVolume;
    }
    
    static void access$1(javax.microedition.media.Player a) {
        rpc.AudioEngine.safeClosePlayer(a);
    }
}
