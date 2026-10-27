package rpc.j2me;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextBox;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import rpc.AudioEngine;
import rpc.Host;

/**
 * {@link Host} implementation: jar resources, persistent storage (RMS),
 * vibration, audio and the text input dialog.
 *
 * The 4.8 source had two bugs here that are fixed below:
 *   - {@code load()} read a jar resource named like the save slot instead of
 *     the record store, so progress was written but never read back;
 *   - the text input lock was used without a monitor, which could spin.
 */
final class HostImpl implements Host {

    private static final String STORE = "retropcbuilder";

    private final MIDlet midlet;
    private final Display display;
    private RecordStore store;

    HostImpl(MIDlet midlet, Display display) {
        this.midlet = midlet;
        this.display = display;
    }

    private RecordStore store() {
        if (store == null) {
            try {
                store = RecordStore.openRecordStore(STORE, true);
            } catch (Throwable t) {
                store = null;
            }
        }
        return store;
    }

    public InputStream open(String name) {
        try {
            return getClass().getResourceAsStream("/" + name);
        } catch (Throwable t) {
            return null;
        }
    }

    public String load(String key) {
        RecordStore rs = store();
        if (rs == null) {
            return null;
        }
        try {
            if (rs.getNumRecords() < 1) {
                return null;
            }
            byte[] data = rs.getRecord(1);
            if (data == null) {
                return null;
            }
            return new String(data, "UTF-8");
        } catch (Throwable t) {
            return null;
        }
    }

    public void save(String key, String value) {
        RecordStore rs = store();
        if (rs == null || value == null) {
            return;
        }
        try {
            byte[] data = value.getBytes("UTF-8");
            if (rs.getNumRecords() < 1) {
                rs.addRecord(data, 0, data.length);
            } else {
                rs.setRecord(1, data, 0, data.length);
            }
        } catch (Throwable ignored) {
            /* device without free RMS space - the game keeps running */
        }
    }

    public void vibrate(int ms) {
        try {
            display.vibrate(ms);
        } catch (Throwable ignored) {
        }
    }

    public void exitApp() {
        try {
            midlet.notifyDestroyed();
        } catch (Throwable ignored) {
        }
    }

    public long now() {
        return System.currentTimeMillis();
    }

    public void tone(int note, int duration) {
        try {
            AudioEngine.getInstance().playToneSafe(note, duration, 80, false);
        } catch (Throwable ignored) {
        }
    }

    public void noise(int kind, int duration) {
        try {
            AudioEngine engine = AudioEngine.getInstance();
            if (kind == 0) {
                engine.playFanNoise(1200, 1990, 0);
            } else {
                engine.playDiskAccess(40, 0, 0, true);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * Modal text input. Returns null when the player cancels. The caller is
     * blocked on the lock object that the command listener notifies.
     */
    public String textInput(String title, String text, int maxSize) {
        final Object lock = new Object();
        final String[] result = new String[] { null };
        TextBox box;
        try {
            box = new TextBox(title, text, maxSize, 0);
        } catch (Throwable t) {
            return text;
        }
        final TextBox fbox = box;
        final Command ok = new Command("OK", Command.OK, 1);
        final Command cancel = new Command("\u041e\u0442\u043c\u0435\u043d\u0430", Command.CANCEL, 2);
        box.addCommand(ok);
        box.addCommand(cancel);
        box.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == ok) {
                    result[0] = fbox.getString();
                }
                display.setCurrent((Displayable) RetroPcMIDlet.canvas);
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        });
        display.setCurrent(box);
        synchronized (lock) {
            try {
                lock.wait(120000L);
            } catch (InterruptedException ignored) {
            }
        }
        return result[0];
    }
}
