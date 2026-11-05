package psx.droid;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;

/**
 * The one and only activity: a full screen surface plus key routing.
 *
 * <p>No dialogs, no menus inflated from resources, nothing that could pop up
 * over the game - the whole UI is painted by {@link EmuView}.</p>
 */
public final class EmuActivity extends Activity {

    public static final String VERSION = "2.0.0";

    private final Settings settings = new Settings();
    private EmuView view;

    public void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        settings.load(this);
        view = new EmuView(this, settings);
        setContentView(view);
    }

    public boolean onKeyDown(int code, KeyEvent e) {
        if (view != null && view.keyDown(code, e.getRepeatCount())) return true;
        return super.onKeyDown(code, e);
    }

    public boolean onKeyUp(int code, KeyEvent e) {
        if (view != null && view.keyUp(code)) return true;
        return super.onKeyUp(code, e);
    }

    protected void onPause() {
        super.onPause();
        if (view != null) view.pause();
    }

    protected void onResume() {
        super.onResume();
        if (view != null) view.unpause();
    }

    protected void onDestroy() {
        super.onDestroy();
        if (view != null) view.shutdown();
    }

    /* The system "menu" button opens our own overlay, so the platform menu
       must never appear. */
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        return false;
    }
}
