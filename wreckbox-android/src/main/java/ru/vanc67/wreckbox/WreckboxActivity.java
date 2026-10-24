package ru.vanc67.wreckbox;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import javax.microedition.lcdui.Image;
import wreckbox.platform.AndroidGameView;
import wreckbox.platform.SettingsStore;
import wreckbox.platform.SoundSynth;

/** Native API-10 entry point; no WebView, network or account services. */
public final class WreckboxActivity extends Activity {
  private AndroidGameView gameView;

  protected void onCreate(Bundle state) {
    super.onCreate(state);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    SettingsStore.configure(this);
    SoundSynth.configure(getAssets());
    Image.configure(getAssets());
    gameView = new AndroidGameView(this);
    setContentView(gameView);
  }
  protected void onResume() { super.onResume(); if (gameView != null) gameView.startGame(); }
  protected void onPause() { if (gameView != null) gameView.stopGame(); super.onPause(); }
  protected void onDestroy() { if (gameView != null) gameView.stopGame(); super.onDestroy(); }
}
