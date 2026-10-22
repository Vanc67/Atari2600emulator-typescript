package wreckbox.test;

import wreckbox.core.EffectSystem;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Tools;

/** Optional constrained-heap smoke test; run through lowheap-test.sh, not the desktop-only suite. */
public final class LowHeapTest {
  public static void main(String[] args) {
    mapSwitching(); solve(0); solve(2); nuclearBlast();
    System.out.println("Wreckbox LowHeapTest: PASS");
  }

  private static void mapSwitching() {
    Game game = new Game();
    for (int i = 0; i < Maps.COUNT * 2; i++) load(game, i % Maps.COUNT);
    System.out.println("lowheap maps: 16 loads, last grid=" + game.grid.memoryBytes());
  }

  private static void solve(int map) {
    Game game = new Game(); load(game, map);
    game.structure.solve(game.grid, game.debris, 450.0f, false);
    System.out.println("lowheap solve " + Maps.IDS[map] + ": grid=" + game.grid.memoryBytes()
        + " solver=" + game.structure.memoryBytes());
  }

  private static void nuclearBlast() {
    Game game = new Game(); load(game, 0); game.selectTool(Tools.find("nuke"));
    game.toolValues[game.toolIndex] = 10;
    game.pointerDown(3603.0f, 1281.0f); game.pointerUp();
    for (int i = 0; i < EffectSystem.MAX; i++) if (game.effects.type[i] == EffectSystem.BOMBER) {
      game.effects.x[i] = game.effects.tx[i];
      break;
    }
    game.update(0.02f);
    for (int i = 0; i < EffectSystem.MAX; i++) if (game.effects.type[i] == EffectSystem.BOMB) {
      game.effects.y[i] = game.effects.ty[i];
      break;
    }
    game.update(0.02f);
    for (int i = 0; i < 40; i++) game.update(0.05f);
    if (game.grid.destroyed != 11716) throw new RuntimeException("Unexpected destruction " + game.grid.destroyed);
    System.out.println("lowheap 50 Mt: grid=" + game.grid.memoryBytes() + " solver=" + game.structure.memoryBytes());
  }

  private static void load(Game game, int map) {
    if (!game.loadMap(map)) throw new RuntimeException(game.loadError);
  }
}
