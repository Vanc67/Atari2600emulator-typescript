package psx.droid;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import android.content.res.AssetManager;

import psx.CdImage;
import psx.MemoryCD;
import psx.PS1Machine;

/**
 * Owns the machine and everything around it: the disc image, the audio sink
 * and the boot sequence.  The UI thread only ever calls into this class, and
 * only the emulation thread runs frames.
 */
public final class Engine {

    /** Name shown in the UI while there is no game. */
    public String title = "";

    /** Human readable result of the last load, shown on screen. */
    public String message = "";

    private final PS1Machine machine;
    private final Settings settings;
    private final AssetManager assets;
    private AndroidAudio audio;
    private Disc disc;

    public Engine(Settings settings, AssetManager assets) {
        this.settings = settings;
        this.assets = assets;
        this.machine = new PS1Machine(null);
    }

    public PS1Machine machine() { return machine; }

    public boolean isRunning() {
        return machine.isRunning();
    }

    public int[] frame() { return machine.getFrame(); }

    public int frameWidth() { return machine.getFrameWidth(); }

    public int frameHeight() { return machine.getFrameHeight(); }

    public int frames() { return machine.framesRun; }

    public String status() { return machine.statusLine(); }

    public void setPad(int bits) { machine.setButtons(bits); }

    /* ==================================================================== */
    /*  loading                                                             */
    /* ==================================================================== */

    /** Boots one of the demo discs that ship inside the APK. */
    public boolean loadDemo(String assetName) {
        try {
            MemoryCD cd = new MemoryCD(readAsset(assetName), assetName);
            start(cd, assetName);
            return true;
        } catch (Throwable t) {
            message = "demo: " + t;
            return false;
        }
    }

    /** Boots an image or cue sheet from the SD card. */
    public boolean loadDisc(File f) {
        Disc d = new Disc();
        try {
            d.open(f);
        } catch (Throwable t) {
            message = "\u043d\u0435 \u043e\u0442\u043a\u0440\u044b\u0442\u044c: " + t;
            d.close();
            return false;
        }
        closeDisc();
        disc = d;
        start(d, f.getName());
        return true;
    }

    private void start(CdImage cd, String what) {
        stop();
        message = "";
        String info = machine.hleBoot(cd) ? "HLE" : "";
        if (!machine.isRunning()) {
            message = "\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u044c " + what;
            return;
        }
        title = what + (info.length() > 0 ? " \u00b7 " + info : "");
        machine.spu.setMasterVolume(settings.sound ? settings.volume : 0);
        startAudio();
    }

    private void startAudio() {
        stopAudio();
        if (!settings.sound) return;
        audio = new AndroidAudio(machine.spu);
        audio.start();
    }

    public void startAudioIfNeeded() {
        if (audio == null && settings.sound) startAudio();
    }

    public void stopAudio() {
        AndroidAudio a = audio;
        audio = null;
        if (a != null) a.stop();
    }

    public boolean audioRunning() {
        AndroidAudio a = audio;
        return a != null && a.isRunning();
    }

    public void applyVolume() {
        machine.spu.setMasterVolume(settings.sound ? settings.volume : 0);
    }

    public void stop() {
        machine.halt();
        stopAudio();
    }

    public void closeDisc() {
        if (disc != null) {
            disc.close();
            disc = null;
        }
    }

    public void shutdown() {
        stop();
        closeDisc();
    }

    /* ==================================================================== */
    /*  running                                                             */
    /* ==================================================================== */

    public void runFrame() {
        machine.runFrame();
    }

    private byte[] readAsset(String name) throws IOException {
        InputStream in = null;
        try {
            in = assets.open("demos/" + name);
            ByteArrayOutputStream out = new ByteArrayOutputStream(70000);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return out.toByteArray();
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable t) { }
            }
        }
    }
}
