package psx.droid;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import psx.Spu;

/**
 * Pulls 16 bit stereo PCM out of the SPU ring buffer and hands it to
 * AudioTrack.
 *
 * <p>The writing thread must never be able to stall emulation, so it copies
 * through a small private buffer and simply drops audio when the device
 * cannot keep up. If AudioTrack refuses to start the emulator keeps running
 * silently instead of failing.</p>
 */
public final class AndroidAudio implements Runnable {

    private final Spu spu;
    private volatile boolean running;
    private Thread thread;
    private AudioTrack track;
    public boolean available;

    public AndroidAudio(Spu spu) {
        this.spu = spu;
    }

    public boolean isRunning() { return running; }

    public void start() {
        if (running) return;
        try {
            int min = AudioTrack.getMinBufferSize(Spu.SAMPLE_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) min = 16384;
            int buf = min < 16384 ? 16384 : min;
            track = new AudioTrack(AudioManager.STREAM_MUSIC, Spu.SAMPLE_RATE,
                    AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, buf,
                    AudioTrack.MODE_STREAM);
            if (track.getState() != AudioTrack.STATE_INITIALIZED) {
                release();
                available = false;
                return;
            }
            track.play();
            available = true;
            running = true;
            thread = new Thread(this, "psx-audio");
            thread.setPriority(Thread.NORM_PRIORITY + 1);
            thread.start();
        } catch (Throwable t) {
            release();
            available = false;
        }
    }

    public void stop() {
        running = false;
        Thread t = thread;
        thread = null;
        if (t != null) {
            try { t.join(400); } catch (InterruptedException e) { }
        }
        release();
    }

    private void release() {
        AudioTrack a = track;
        track = null;
        if (a != null) {
            try { a.stop(); } catch (Throwable t) { }
            try { a.release(); } catch (Throwable t) { }
        }
    }

    public void run() {
        byte[] buf = new byte[4096];
        AudioTrack a = track;
        while (running && a != null) {
            int n = spu.drain(buf, 0, buf.length);
            if (n <= 0) {
                try { Thread.sleep(4); } catch (InterruptedException e) { }
                continue;
            }
            if (n < 4) n = 4;
            if ((n & 3) != 0) n -= (n & 3);
            try {
                a.write(buf, 0, n);
            } catch (Throwable t) {
                running = false;
            }
        }
    }
}
