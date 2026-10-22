package wreckbox.test;

import javax.microedition.lcdui.Graphics;
import wreckbox.core.EffectSystem;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Tools;
import wreckbox.platform.WreckboxRenderer;

/** Headless MIDP stub smoke suite: executes rendering and every tool on the packaged JAR. */
public final class PlatformTest {
  private static int checks, failures;
  public static void main(String[] args) {
    Graphics graphics = new Graphics();
    renderMaps(graphics); renderDestruction(graphics); exerciseTools(graphics); maximumNuke(graphics);
    System.out.println("Wreckbox PlatformTest: " + checks + " checks, " + failures + " failures");
    if (failures != 0) throw new RuntimeException("FAILED " + failures);
  }
  private static void check(boolean condition, String message) {
    checks++; if (!condition) { failures++; System.out.println("FAIL " + message); }
  }
  private static void renderMaps(Graphics graphics) {
    for (int i = 0; i < Maps.COUNT; i++) {
      Game game = new Game(); check(game.loadMap(i), "platform load " + Maps.IDS[i]);
      WreckboxRenderer renderer = new WreckboxRenderer(game);
      try {
        renderer.render(graphics, 128, 160, 64, 80);
        game.night = true; renderer.render(graphics, 128, 160, 64, 80);
        check(true, "render " + Maps.IDS[i]);
      } catch (RuntimeException e) { check(false, "render " + Maps.IDS[i] + " " + e); }
    }
  }
  private static void renderDestruction(Graphics graphics) {
    Game game = new Game(); game.loadMap(0); game.zoom = 0.65f;
    for (int i = 0; i < 12; i++) {
      int slot = game.debris.spawnChunk(game.cameraX + (i - 6) * 9.0f, game.cameraY - 25.0f + (i % 3) * 9.0f,
          6.0f + (i % 3) * 2.0f, 5.0f + (i % 4), 1 + i % 7, 12, 4.0f, 40.0f);
      game.debris.angle[slot] = i * 0.37f; game.debris.hp[slot] = 12.0f;
    }
    game.debris.spawnDust(game.cameraX, game.cameraY, 0xa09080, 80);
    try {
      WreckboxRenderer renderer = new WreckboxRenderer(game);
      renderer.render(graphics, 128, 160, 64, 80);
      game.night = true; renderer.render(graphics, 128, 160, 64, 80);
      check(true, "rotated damaged debris and dust day/night");
    } catch (RuntimeException e) { check(false, "destruction renderer " + e); }
  }

  private static void exerciseTools(Graphics graphics) {
    for (int tool = 0; tool < Tools.COUNT; tool++) {
      Game game = new Game(); game.loadMap(0); WreckboxRenderer renderer = new WreckboxRenderer(game);
      try {
        game.selectTool(tool);
        float x = 600 * 6.0f + 3.0f, y = 213 * 6.0f + 3.0f;
        game.pointerDown(x, y);
        for (int i = 0; i < 3; i++) game.update(1.0f / 60.0f);
        game.pointerMove(x + 2.0f, y - 1.0f, 1.0f / 60.0f);
        game.pointerUp();
        for (int i = 0; i < 9; i++) game.update(1.0f / 60.0f);
        renderer.render(graphics, 128, 160, 64, 80);
        check(true, "tool " + Tools.IDS[tool]);
      } catch (RuntimeException e) { check(false, "tool " + Tools.IDS[tool] + " " + e); }
    }
  }
  private static void maximumNuke(Graphics graphics) {
    Game game = new Game(); game.loadMap(0); game.selectTool(Tools.find("nuke"));
    game.toolValues[game.toolIndex] = 10;
    game.pointerDown(3603.0f, 1281.0f); game.pointerUp();
    int bomb = -1;
    for (int i = 0; i < EffectSystem.MAX; i++) if (game.effects.type[i] == EffectSystem.BOMB) { bomb = i; break; }
    check(bomb >= 0, "50 Mt bomb created");
    if (bomb >= 0) {
      check(game.effects.radius[bomb] > 6000.0f, "50 Mt original radius scale");
      long start = System.currentTimeMillis();
      game.effects.y[bomb] = game.effects.ty[bomb];
      game.update(0.02f);
      check(game.grid.destroyed > 0 && game.radiation > 0.0f && game.radiationSystem.count == 1, "50 Mt simulated damage and fallout");
      check(game.doseRate > 0.000015f, "50 Mt dosimeter response");
      game.showStress = true;
      new WreckboxRenderer(game).render(graphics, 128, 160, 64, 80);
      System.out.println("50 Mt explosion ms=" + (System.currentTimeMillis() - start) + " destroyed=" + game.grid.destroyed);
    }
  }
}
