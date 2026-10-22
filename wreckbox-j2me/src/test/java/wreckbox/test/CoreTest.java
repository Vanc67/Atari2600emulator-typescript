package wreckbox.test;

import wreckbox.core.BodyStore;
import wreckbox.core.Buildings;
import wreckbox.core.CellGrid;
import wreckbox.core.DebrisSystem;
import wreckbox.core.EffectSystem;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Materials;
import wreckbox.core.Math2;
import wreckbox.core.PeopleStore;
import wreckbox.core.Props;
import wreckbox.core.StructuralSolver;
import wreckbox.core.Tools;

public final class CoreTest {
  private static int checks, failures;
  private static final int[] SOLID = {68453, 71537, 101898, 70082, 57964, 83814, 71283, 85081};
  private static final int[] USER = {11716, 6649, 26395, 8280, 7641, 13367, 5390, 2791};
  private static final int[] BODIES = {738, 414, 2030, 437, 489, 568, 346, 159};
  private static final int[] PEOPLE = {63, 63, 97, 63, 73, 75, 56, 25};
  private static final int[] HASHES = {0x8e7344c8, 0x2534cc78, 0x90b403ed, 0xf21c1dee, 0x2a5c4f18, 0xca89cf9a, 0x87e85369, 0x4f6b17e0};

  public static void main(String[] args) {
    content(); mathAndCells(); maps(); structure(); strikeOracle(); effects();
    System.out.println("Wreckbox CoreTest: " + checks + " checks, " + failures + " failures");
    if (failures != 0) throw new RuntimeException("FAILED " + failures);
  }
  private static void check(boolean condition, String message) {
    checks++; if (!condition) { failures++; System.out.println("FAIL " + message); }
  }
  private static void eq(int expected, int actual, String message) { check(expected == actual, message + " expected=" + expected + " actual=" + actual); }
  private static void near(float expected, float actual, float epsilon, String message) { check(Math.abs(expected - actual) <= epsilon, message + " expected=" + expected + " actual=" + actual); }

  private static void content() {
    eq(21, Materials.COUNT, "materials"); eq(103, Props.COUNT, "props"); eq(27, Tools.COUNT, "tools");
    eq(13, Buildings.COUNT, "buildings"); eq(8, Maps.COUNT, "maps");
    eq(0, Props.find("sofa"), "first prop"); check(Props.find("sam") >= 0, "SAM present"); check(Props.find("gcn") >= 0, "reactor pump present");
    eq(320, (int)Materials.HP[4], "steel HP"); eq(2400, (int)Materials.COMPRESSION[14], "panel compression");
    near(4.8f, Materials.DENSITY[18], 0.0001f, "lead density"); near(2.5f, Materials.BRITTLE[17], 0.0001f, "glass blocks brittle");
    check(Tools.find("nuke") >= 0 && Tools.find("building") >= 0, "sandbox tools");
    eq(50000, (int)Game.NUKE_KT[9], "50 Mt");
  }

  private static void mathAndCells() {
    near(2.0f, Math2.cbrt(8.0f), 0.0001f, "cbrt"); near(3.0f, Math2.pow(9.0f, 0.5f), 0.0001f, "pow");
    near(2.0f, Math2.log10(100.0f), 0.0001f, "log10");
    CellGrid grid = new CellGrid(8);
    grid.set(10, 4, 6, CellGrid.FLAG_USER | CellGrid.FLAG_REINFORCED);
    eq(4, grid.material(10), "packed material"); eq(6, grid.tint(10), "packed tint"); eq(3, grid.flags(10), "packed flags");
    near(320.0f, grid.health(10), 0.0f, "packed hp");
    grid.damage(10, 32.0f); near(294.4f, grid.health(10), 0.07f, "steel damage multiplier");
    grid.set(11, 14, 15, CellGrid.FLAG_JOINT); grid.damage(11, 10.0f); near(144.0f, grid.health(11), 0.07f, "panel joint weakness");
    grid.set(12, 9, 0, 0); check(!grid.damage(12, 100000.0f), "bedrock indestructible");
  }

  private static void maps() {
    Game game = new Game();
    for (int i = 0; i < Maps.COUNT; i++) {
      check(game.loadMap(i), "load " + Maps.IDS[i] + " " + game.loadError);
      eq(Maps.WIDTHS[i], game.grid.width, "width " + Maps.IDS[i]);
      eq(SOLID[i], game.grid.countSolid(), "solid " + Maps.IDS[i]);
      eq(USER[i], game.grid.initialUserCells, "user cells " + Maps.IDS[i]);
      eq(BODIES[i], game.bodies.count, "bodies " + Maps.IDS[i]);
      eq(PEOPLE[i], game.people.count, "people " + Maps.IDS[i]);
      eq(HASHES[i], mapHash(game.grid), "oracle hash " + Maps.IDS[i]);
      check(game.power.nodeCount > 0, "power nodes " + Maps.IDS[i]);
      int unknown = 0;
      for (int b = 0; b < game.bodies.count; b++) if (game.bodies.type[b] < 0) unknown++;
      System.out.println("MAP " + Maps.IDS[i] + " solid=" + SOLID[i] + " bodies=" + BODIES[i] + " people=" + PEOPLE[i] + " generic=" + unknown);
    }
  }

  private static int mapHash(CellGrid grid) {
    int hash = 0x811c9dc5;
    for (int i = 0; i < grid.size; i++) {
      int m = grid.material(i);
      int hp = m == 0 ? 0 : (int)(Materials.HP[m] * 16.0f);
      hash ^= m | (hp << 8) | (grid.flags(i) << 24);
      hash *= 16777619;
    }
    return hash;
  }

  private static void structure() {
    CellGrid grid = new CellGrid(32); DebrisSystem debris = new DebrisSystem(); StructuralSolver solver = new StructuralSolver();
    for (int x = 0; x < 32; x++) grid.set(grid.index(x, 259), 9, 0, 0);
    for (int y = 250; y <= 258; y++) grid.set(grid.index(16, y), 2, 0, 1);
    solver.solve(grid, debris, 450.0f);
    eq(0, solver.collapsedCells, "supported pillar stable");
    grid.clear(grid.index(16, 258));
    solver.solve(grid, debris, 450.0f);
    eq(8, solver.collapsedCells, "unsupported pillar collapses");
    check(debris.chunkCount > 0, "rigid chunk created");
    eq(0, grid.materialAt(16, 250), "collapsed cell removed");

    CellGrid weak = new CellGrid(24); DebrisSystem weakDebris = new DebrisSystem(); solver = new StructuralSolver();
    for (int x = 0; x < 24; x++) weak.set(weak.index(x, 259), 9, 0, 0);
    weak.set(weak.index(12, 258), 6, 0, 1);
    for (int y = 200; y <= 257; y++) weak.set(weak.index(12, y), 4, 6, 1);
    boolean fractured = false;
    for (int i = 0; i < 8; i++) {
      solver.solve(weak, weakDebris, 450.0f);
      if (solver.overloadedCells > 0 || solver.collapsedCells > 0) fractured = true;
    }
    check(fractured, "weight overload fractures weak support");
  }

  private static void strikeOracle() {
    Game game = new Game(); check(game.loadMap(0), "strike map");
    float before = healthSum(game.grid);
    game.selectTool(Tools.find("hammer")); game.random.seed(-597774427);
    game.pointerDown(600 * 6.0f + 3.0f, 213 * 6.0f + 3.0f); game.pointerUp();
    float damage = before - healthSum(game.grid);
    near(1129.18f, damage, 2.0f, "recovered JS hammer differential");
    eq(68453, game.grid.countSolid(), "oracle strike destroys no cells");
    int[] indices = {254998, 254999, 255000, 255001, 255002, 256198, 256199, 256200, 256201, 256202};
    float[] health = {60.0f, 30.710678f, 10.0f, 30.710678f, 71.803398f, 248.0f, 145.6f, 168.0f, 208.0f, 248.0f};
    for (int i = 0; i < indices.length; i++) near(health[i], game.grid.health(indices[i]), 0.07f, "strike cell " + indices[i]);
  }

  private static float healthSum(CellGrid grid) {
    float sum = 0.0f;
    for (int i = 0; i < grid.size; i++) if (grid.material(i) != 0) sum += grid.health(i);
    return sum;
  }

  private static void effects() {
    Game game = new Game(); check(game.loadMap(0), "effects map");
    int target = findDestructible(game.grid);
    check(target >= 0, "hammer target");
    int before = game.grid.hp16(target);
    float x = (target % game.grid.width) * 6.0f + 3.0f, y = (target / game.grid.width) * 6.0f + 3.0f;
    game.selectTool(Tools.find("hammer")); game.pointerDown(x, y); game.pointerUp();
    check(game.grid.hp16(target) < before || game.grid.material(target) == 0, "hammer damages real cell");
    game.selectTool(Tools.find("dynamite")); game.pointerDown(x, y); game.pointerUp();
    eq(1, game.effects.chargeCount(), "dynamite placed"); game.detonate();
    for (int i = 0; i < 20; i++) game.update(0.05f);
    eq(0, game.effects.chargeCount(), "dynamite detonated");
    game.selectTool(Tools.find("people")); int people = game.people.count; game.pointerDown(x, y); game.pointerUp();
    eq(people + 1, game.people.count, "people tool");
    game.selectTool(Tools.find("brush")); int paintX = 5, paintY = 5, at = game.grid.index(paintX, paintY);
    game.grid.clear(at); game.pointerDown(5 * 6.0f + 3.0f, 5 * 6.0f + 3.0f); game.pointerUp();
    check(game.grid.material(at) != 0, "brush builds cell");
    game.selectTool(Tools.find("eraser")); game.pointerDown(5 * 6.0f + 3.0f, 5 * 6.0f + 3.0f); game.pointerUp();
    eq(0, game.grid.material(at), "eraser removes cell");
  }

  private static int findDestructible(CellGrid grid) {
    for (int y = 80; y < 210; y++) for (int x = grid.width / 2; x < grid.width - 1; x++) {
      int at = grid.index(x, y), m = grid.material(at);
      if (m != 0 && !Materials.FIXED[m]) return at;
    }
    return -1;
  }
}
