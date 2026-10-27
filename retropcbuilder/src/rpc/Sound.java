package rpc;

import java.util.Random;

public class Sound {
    private static rpc.Host host;
    private static rpc.AudioEngine engine;
    private static final Random rnd = new Random();

    /* MIDI note numbers, the same ones Manager.playTone() takes.  The
     * sequence player reads them raw when the octave byte is 3. */
    private static final int N_C2 = 36, N_D2 = 38, N_E2 = 40, N_F2 = 41, N_G2 = 43;
    private static final int N_A2 = 45, N_B2 = 47;
    private static final int N_C3 = 48, N_D3 = 50, N_E3 = 52, N_F3 = 53, N_G3 = 55;
    private static final int N_A3 = 57, N_B3 = 59;
    private static final int N_C4 = 60, N_D4 = 62, N_E4 = 64, N_F4 = 65, N_G4 = 67;
    private static final int N_A4 = 69, N_B4 = 71;
    private static final int N_C5 = 72, N_E5 = 76;

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
    
    /** A random one of the eight boot personalities. */
    public static void boot() {
        playBootVariant(rnd.nextInt(8));
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
    
    /**
     * Rain on the window.  {@code intensity} 2 is an ordinary shower, 3 is
     * the storm version: more ticks, lower and longer.
     */
    public static void rain(int intensity) {
        boolean storm = intensity >= 3;
        int n = storm ? 40 : 26;
        int dur = storm ? 2 : 3;
        int low = storm ? 84 : 96;
        int[][] a = new int[n][2];
        for (int i = 0; i < n; i++) {
            a[i][0] = dur;
            a[i][1] = low + rnd.nextInt(40);
        }
        engine.playSequenceSafe(seq(260, a));
    }
    
    public static void purr() {
        engine.playCatPurr();
    }
    
    /** The flat's radio: the station's loop, four different songs. */
    public static void radio(int station) {
        switch (Math.abs(station) % 4) {
            case 0:
                radioTrack0();
                break;
            case 1:
                radioTrack1();
                break;
            case 2:
                radioTrack2();
                break;
            default:
                radioTrack3();
                break;
        }
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

    /** A thunderclap: the crack, then the roll going away into the distance. */
    public static void thunder() {
        engine.playSequenceSafe(seq(80, new int[][] {
            {6, 28}, {6, 26}, {6, 24},
            {14, 31}, {14, 28}, {14, 26}, {14, 24},
            {22, 22}, {22, 20}, {30, 18}, {44, 0}}));
    }

    /** Snow: quiet, sparse and high. */
    public static void snow() {
        int[][] a = new int[14][2];
        for (int i = 0; i < a.length; i++) {
            a[i][0] = 6;
            a[i][1] = 108 + rnd.nextInt(30);
        }
        engine.playSequenceSafe(seq(300, a));
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

    /* ==================================================================
     *  sequences
     * ==================================================================
     *  A ToneControl sequence is an octave byte, a tempo byte and then
     *  (duration, note) pairs.  The duration is in 40ths of a second and
     *  the note is a raw MIDI number; octave 3 leaves the numbers alone,
     *  which is what the rest of the game already uses.  A note of 0 is
     *  below the audible range, so it reads as a rest on any phone.
     */

    private static byte[] seq(int tempo, int[][] notes) {
        byte[] b = new byte[2 + notes.length * 2];
        b[0] = 3;
        // a tempo above 255 or a note above 127 would wrap around the byte
        // and land somewhere inaudible, so clamp both into their ranges
        b[1] = (byte) clamp(tempo, 0, 255);
        for (int i = 0; i < notes.length; i++) {
            b[2 + i * 2] = (byte) clamp(notes[i][0], 0, 255);
            b[3 + i * 2] = (byte) clamp(notes[i][1], 0, 127);
        }
        return b;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /* ==================================================================
     *  the PC switching on
     * ==================================================================
     *  Eight BIOS personalities across the 1979-2027 span, picked at random
     *  so the machine never boots the same way twice.
     */

    /** 0 AMI, 1 Award, 2 Phoenix, 3 UEFI, 4 turbo sweep, 5 failed POST,
     *  6 floppy spin-up, 7 late-90s splash. */
    public static void playBootVariant(int k) {
        if (k == 0) {
            // AMI: two short identical beeps
            engine.playSequenceSafe(seq(170, new int[][] {
                {10, N_C5}, {8, 0}, {10, N_C5}, {34, 0}}));
        } else if (k == 1) {
            // Award: one short, one long
            engine.playSequenceSafe(seq(150, new int[][] {
                {10, N_G4}, {10, 0}, {34, N_G4}, {30, 0}}));
        } else if (k == 2) {
            // Phoenix: three quick rising beeps
            engine.playSequenceSafe(seq(190, new int[][] {
                {8, N_C4}, {8, 0}, {8, N_E4}, {8, 0}, {8, N_G4}, {34, 0}}));
        } else if (k == 3) {
            // a modern UEFI chime
            engine.playSequenceSafe(seq(120, new int[][] {
                {16, N_G4}, {12, 0}, {16, N_C5}, {12, 0}, {40, N_E5}, {30, 0}}));
        } else if (k == 4) {
            // an overclocked board spinning up: the sweep rises
            engine.playSequenceSafe(seq(200, new int[][] {
                {8, N_C3}, {8, N_D3}, {8, N_E3}, {8, N_F3}, {8, N_G3},
                {8, N_A3}, {8, N_B3}, {8, N_C4}, {8, N_E4}, {8, N_G4}, {40, 0}}));
        } else if (k == 5) {
            // a failed POST: one long low tone, then the same again
            engine.playSequenceSafe(seq(90, new int[][] {
                {16, N_C2}, {16, 0}, {16, N_C2}, {16, 0}, {40, N_C2}, {30, 0}}));
        } else if (k == 6) {
            // a floppy-era machine: the click, then the drive winding up
            engine.playSequenceSafe(seq(160, new int[][] {
                {4, N_B4}, {6, N_A4}, {8, N_G4}, {8, N_A4}, {8, N_B4},
                {8, N_C5}, {8, N_B4}, {8, N_A4}, {40, 0}}));
        } else {
            // a cheerful late-90s splash tone
            engine.playSequenceSafe(seq(140, new int[][] {
                {12, N_C4}, {10, N_E4}, {10, N_G4}, {10, N_E4},
                {12, N_C4}, {10, N_E4}, {10, N_G4}, {10, N_C5}, {36, 0}}));
        }
    }

    /* ==================================================================
     *  the radio
     * ==================================================================
     *  Four stations, four different lo-fi loops: a bass note per chord and
     *  the chord's notes as short stabs above it.  Nothing here is a single
     *  arpeggio - each station has its own progression and its own pace.
     */

    /** Station 0: Am - F - C - G, the relaxed one. */
    private static void radioTrack0() {
        engine.playSequenceSafe(seq(120, new int[][] {
            {30, N_A2}, {10, N_A3}, {10, N_C4}, {10, N_E4},
            {30, N_F2}, {10, N_F3}, {10, N_A3}, {10, N_C4},
            {30, N_C3}, {10, N_C4}, {10, N_E4}, {10, N_G4},
            {30, N_G2}, {10, N_G3}, {10, N_B3}, {10, N_D4},
            {30, 0}}));
    }

    /** Station 1: Dm - Bb - F - C, a little lower and slower. */
    private static void radioTrack1() {
        engine.playSequenceSafe(seq(110, new int[][] {
            {32, N_D3}, {12, N_D4}, {12, N_F4}, {12, N_A4},
            {30, N_A2}, {12, N_A3}, {12, N_C4}, {12, N_E4},
            {30, N_F2}, {12, N_F3}, {12, N_A3}, {12, N_C4},
            {32, N_C3}, {12, N_C4}, {12, N_E4}, {12, N_G4},
            {32, 0}}));
    }

    /** Station 2: Em - C - G - D, quicker, with a top note on the fourth. */
    private static void radioTrack2() {
        engine.playSequenceSafe(seq(132, new int[][] {
            {24, N_E2}, {8, N_E3}, {8, N_G3}, {8, N_B3}, {8, N_E4},
            {24, N_C3}, {8, N_C4}, {8, N_E4}, {8, N_G4}, {8, 0},
            {24, N_G2}, {8, N_G3}, {8, N_B3}, {8, N_D4}, {8, 0},
            {24, N_D3}, {8, N_D4}, {8, N_F4}, {8, N_A4}, {28, 0}}));
    }

    /** Station 3: Cmaj7 - A7 - Dm - G, the bluesy one. */
    private static void radioTrack3() {
        engine.playSequenceSafe(seq(104, new int[][] {
            {32, N_C3}, {12, N_C4}, {12, N_E4}, {12, N_G4},
            {28, N_A2}, {12, N_A3}, {12, N_C4}, {12, N_E4},
            {28, N_D3}, {12, N_D4}, {12, N_F4}, {12, N_A4},
            {32, N_G2}, {12, N_G3}, {12, N_B3}, {12, N_D4},
            {32, 0}}));
    }

    /* ==================================================================
     *  the street
     * ==================================================================
     *  What a street sounds like when you are standing in it: cars going
     *  past, a horn somewhere, a door.
     */

    /** A car going past - the pitch rises while it approaches, then falls. */
    public static void car() {
        int base = 43 + rnd.nextInt(14);
        engine.playSequenceSafe(seq(210, new int[][] {
            {6, base}, {6, base + 2}, {6, base + 4}, {6, base + 7},
            {6, base + 9}, {6, base + 7}, {6, base + 4}, {6, base + 2},
            {30, 0}}));
    }

    /** A horn from somewhere down the street. */
    public static void horn() {
        engine.playToneSafe(196, 170, 45, true);
        engine.playToneSafe(147, 200, 40, true);
    }

    /** A door slamming in the house opposite. */
    public static void slam() {
        engine.playToneSafe(300, 60, 40, true);
        engine.playToneSafe(180, 90, 30, true);
    }

    /** Whatever the street is doing this moment. */
    public static void street() {
        switch (rnd.nextInt(10)) {
            case 0:
            case 1:
            case 2:
                car();
                break;
            case 3:
                horn();
                break;
            case 4:
                slam();
                break;
            default:
                engine.playToneSafe(110 + rnd.nextInt(70), 45, 22, true);
                break;
        }
    }
}
