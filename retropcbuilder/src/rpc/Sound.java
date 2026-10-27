package rpc;

public class Sound {
    private static rpc.Host host;
    private static rpc.AudioEngine engine;
    
    static {
        engine = rpc.AudioEngine.getInstance();
    }
    
    public Sound() {
    }
    
    public static void bind(rpc.Host a) {
        host = a;
    }
    
    public static rpc.AudioEngine getEngine() {
        return engine;
    }
    
    public static void click() {
        engine.playKeyClick();
    }
    
    public static void ok() {
        engine.playSuccess();
    }
    
    public static void error() {
        engine.playError();
    }
    
    public static void boot() {
        engine.playStartupSound();
    }
    
    public static void postOk() {
        engine.playPostBeep(0);
    }
    
    public static void postBeep(int i) {
        engine.playPostBeep(i);
    }
    
    public static void bsod() {
        engine.playBsod();
    }
    
    public static void fan(int i, int i0) {
        engine.playFanNoise(i, 2000, i0);
    }
    
    public static void fanWithYear(int i, int i0, int i1) {
        engine.playFanNoise(i, i0, i1);
    }
    
    public static void disk(boolean b, boolean b0) {
        if (b) {
            engine.playDiskAccess(7, 0, 2020, b0);
        } else {
            engine.playDiskAccess(5, 7200, 2000, b0);
        }
    }
    
    public static void diskAdvanced(int i, int i0, int i1, boolean b) {
        engine.playDiskAccess(i, i0, i1, b);
    }
    
    public static void modem(int i) {
        engine.playModemHandshake(i);
    }
    
    public static void overheat() {
        engine.playPostBeep(4);
    }
    
    public static void installStep(int i) {
        engine.playToneSafe(600 + i * 6, 25, 75, true);
    }
    
    public static void installDone() {
        engine.playSuccess();
    }
    
    public static void revived() {
        engine.playStartupSound();
    }
    
    public static void doom() {
        engine.playDoomRiff();
    }
    
    public static void rain(int i) {
        engine.playRainNoise(i);
    }
    
    public static void purr() {
        engine.playCatPurr();
    }
    
    public static void radio(int i) {
        engine.playRadioChiptune(i);
    }
    
    public static void tool(int i) {
        engine.playToolSound(i);
    }
    
    public static void floppy() {
        engine.playFloppyStep();
    }
}
