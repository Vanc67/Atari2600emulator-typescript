package psx.droid;

import android.content.Context;
import android.content.SharedPreferences;

/** Everything the user can change, persisted with SharedPreferences. */
public final class Settings {

    public static final int SCALE_FIT = 0;       /* keep the 4:3-ish shape */
    public static final int SCALE_STRETCH = 1;   /* fill the screen        */
    public static final int SCALE_1X = 2;        /* one device pixel = one emulated pixel */

    public boolean sound = true;
    public boolean touchPad = true;
    public boolean showFps = true;
    public int frameskip = 0;                    /* 0..3, 0 = every frame */
    public int scaleMode = SCALE_FIT;
    public int volume = 100;
    public String lastImage = "";
    public String lastDir = "";

    private SharedPreferences prefs;

    public void load(Context c) {
        prefs = c.getSharedPreferences("psx", Context.MODE_PRIVATE);
        sound = prefs.getBoolean("sound", sound);
        touchPad = prefs.getBoolean("touchPad", touchPad);
        showFps = prefs.getBoolean("showFps", showFps);
        frameskip = prefs.getInt("frameskip", frameskip);
        scaleMode = prefs.getInt("scaleMode", scaleMode);
        volume = prefs.getInt("volume", volume);
        lastImage = prefs.getString("lastImage", lastImage);
        lastDir = prefs.getString("lastDir", lastDir);
        if (frameskip < 0) frameskip = 0;
        if (frameskip > 3) frameskip = 3;
        if (volume < 0) volume = 0;
        if (volume > 100) volume = 100;
    }

    public void save() {
        if (prefs == null) return;
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("sound", sound);
        e.putBoolean("touchPad", touchPad);
        e.putBoolean("showFps", showFps);
        e.putInt("frameskip", frameskip);
        e.putInt("scaleMode", scaleMode);
        e.putInt("volume", volume);
        e.putString("lastImage", lastImage);
        e.putString("lastDir", lastDir);
        e.commit();
    }
}
