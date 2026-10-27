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

    // --- the small sounds of everyday life ------------------------------
    // Built from the same tone primitive as the rest, so they cost nothing
    // on a feature phone and always play.

    /** two bright notes: money changed hands */
    public static void coin() {
        engine.playToneSafe(1318, 14, 65, false);
        engine.playToneSafe(1760, 24, 65, false);
    }

    /** leaflet / paper handled */
    public static void paper() {
        engine.playToneSafe(320, 10, 40, false);
        engine.playToneSafe(250, 14, 35, false);
    }

    /** door of the flat */
    public static void door() {
        engine.playToneSafe(190, 30, 60, false);
        engine.playToneSafe(120, 44, 55, false);
    }

    /** chair / floorboard under someone */
    public static void creak() {
        engine.playToneSafe(200, 26, 45, true);
    }

    /** wall clock, once per second */
    public static void tick() {
        engine.playToneSafe(2100, 5, 30, false);
    }

    /** alarm clock */
    public static void alarm() {
        engine.playToneSafe(880, 90, 70, false);
        engine.playToneSafe(660, 90, 70, false);
    }

    /** one bubble out of the fish tank */
    public static void bubble() {
        engine.playToneSafe(680, 12, 45, false);
        engine.playToneSafe(980, 16, 40, false);
    }

    /** thunder rolling in */
    public static void thunder() {
        engine.playToneSafe(72, 320, 80, false);
        engine.playToneSafe(54, 260, 70, false);
    }

    /** tea going into the cup */
    public static void pour() {
        engine.playToneSafe(620, 40, 45, false);
        engine.playToneSafe(500, 40, 40, false);
    }

    /** camera shutter */
    public static void shutter() {
        engine.playToneSafe(1500, 6, 60, false);
        engine.playToneSafe(900, 8, 50, false);
    }

    /** coin dropping into a slot / ticket stamped */
    public static void stamp() {
        engine.playToneSafe(1046, 8, 60, false);
        engine.playToneSafe(1397, 10, 55, false);
    }
}
