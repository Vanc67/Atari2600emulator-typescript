package wreckbox.platform;

import android.content.Context;
import android.content.SharedPreferences;
import wreckbox.core.Game;

/** Persistent original-style preferences; there is no unlock/progression state. */
public final class SettingsStore {
  private static SharedPreferences preferences;
  public boolean sound = true;

  public static void configure(Context context) {
    preferences = context.getSharedPreferences("wreckbox18.settings", Context.MODE_PRIVATE);
  }
  public void load(Game game, SoundSynth synth) {
    if (preferences == null) return;
    try {
      game.night = preferences.getBoolean("night", game.night);
      game.slowMotion = preferences.getBoolean("slowMotion", game.slowMotion);
      game.bloodEnabled = preferences.getBoolean("blood", game.bloodEnabled);
      game.servicesEnabled = preferences.getBoolean("services", game.servicesEnabled);
      game.epicSlow = preferences.getBoolean("epicSlow", game.epicSlow);
      game.showToasts = preferences.getBoolean("toasts", game.showToasts);
      game.showStress = preferences.getBoolean("stress", game.showStress);
      game.effectsEnabled = preferences.getBoolean("effects", game.effectsEnabled);
      int physics = preferences.getInt("physics", game.physicsMode);
      if (physics >= 0 && physics < 3) game.physicsMode = physics;
      int gravity = preferences.getInt("gravity", game.gravityIndex);
      if (gravity >= 0 && gravity < Game.GRAVITIES.length) {
        game.gravityIndex = gravity; game.gravity = Game.GRAVITIES[gravity];
      }
      sound = preferences.getBoolean("sound", true); synth.enabled = sound;
      game.selectTool(preferences.getInt("tool", game.toolIndex));
      game.loadMap(preferences.getInt("map", game.mapIndex));
    } catch (RuntimeException ignored) { }
  }
  public void save(Game game) {
    if (preferences == null) return;
    try {
      preferences.edit()
          .putInt("map", game.mapIndex).putInt("tool", game.toolIndex).putInt("gravity", game.gravityIndex)
          .putBoolean("night", game.night).putBoolean("slowMotion", game.slowMotion).putBoolean("sound", sound)
          .putBoolean("blood", game.bloodEnabled).putBoolean("services", game.servicesEnabled)
          .putBoolean("epicSlow", game.epicSlow).putBoolean("toasts", game.showToasts)
          .putBoolean("stress", game.showStress).putInt("physics", game.physicsMode)
          .putBoolean("effects", game.effectsEnabled).commit();
    } catch (RuntimeException ignored) { }
  }
}
