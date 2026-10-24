package wreckbox.test;

import java.io.InputStream;
import wreckbox.core.BodyStore;
import wreckbox.core.Buildings;
import wreckbox.core.CellGrid;
import wreckbox.core.DebrisSystem;
import wreckbox.core.EffectSystem;
import wreckbox.core.FireSystem;
import wreckbox.core.FluidSystem;
import wreckbox.core.Game;
import wreckbox.core.GameListener;
import wreckbox.core.Maps;
import wreckbox.core.Materials;
import wreckbox.core.Math2;
import wreckbox.core.PeopleStore;
import wreckbox.core.Props;
import wreckbox.core.RadiationSystem;
import wreckbox.core.RagdollStore;
import wreckbox.core.ResourceLoader;
import wreckbox.core.StructuralSolver;
import wreckbox.core.ServiceSystem;
import wreckbox.core.Tools;

public final class CoreTest {
  private static int checks, failures;
  private static final int[] SOLID = {68453, 71537, 101898, 70082, 57964, 83814, 71283, 85081};
  private static final int[] USER = {11716, 6649, 26395, 8280, 7641, 13367, 5390, 2791};
  private static final int[] BODIES = {738, 414, 2030, 437, 489, 568, 346, 159};
  private static final int[] PEOPLE = {63, 63, 97, 63, 73, 75, 56, 25};
  private static final int[] HASHES = {0x8e7344c8, 0x2534cc78, 0x90b403ed, 0xf21c1dee, 0x2a5c4f18, 0xca89cf9a, 0x87e85369, 0x4f6b17e0};
  private static final int[] TINT_HASHES = {0x0b777286, 0x21a86b22, 0x218e87aa, 0x9001a2f3, 0xb1cad0f9, 0x0c6a9629, 0xf0ed4208, 0x9980d3cf};

  public static void main(String[] args) {
    content(); mathAndCells(); maps(); initialPhysics(); naturalStability(); structure(); debris(); strikeOracle(); effects(); radiation(); reactorEmergency(); sirenRouting(); residents(); fluidsAndServices(); hudCaching();
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
    eq(grid.packed(10), grid.packedAt(2, 1), "row/column packed access");
    grid.damage(10, 32.0f); near(294.4f, grid.health(10), 0.07f, "steel damage multiplier");
    grid.set(11, 14, 15, CellGrid.FLAG_JOINT); grid.damage(11, 10.0f); near(144.0f, grid.health(11), 0.07f, "panel joint weakness");
    grid.set(12, 9, 0, 0); check(!grid.damage(12, 100000.0f), "bedrock indestructible");
    grid.set(13, 12, 0, 0); eq(1, grid.lavaCells, "lava count tracks insertion");
    grid.clear(13); eq(0, grid.lavaCells, "lava count tracks removal");

    CellGrid cow = new CellGrid(8); int emptyBytes = cow.memoryBytes();
    cow.set(10, 4, 0, 0); check(cow.memoryBytes() >= emptyBytes + 8 * 4, "changed row expands on demand");
    cow.clear(10); eq(emptyBytes, cow.memoryBytes(), "fully cleared row releases storage");

    CellGrid sparse = new CellGrid(256);
    sparse.beginLoad(1); sparse.addLoadRun(4 * 256, 256, 3, 0, 0); sparse.finishLoad();
    int sparseEmptyBytes = sparse.memoryBytes();
    for (int x = 8; x < 16; x++) sparse.setHealth(sparse.index(x, 4), 40.0f + x);
    int sparseBytes = sparse.memoryBytes();
    check(sparseBytes - sparseEmptyBytes < 256 * 2, "sparse HP overlay stays compact");
    sparse.compactRows();
    for (int x = 0; x < 256; x++) {
      float expected = x >= 8 && x < 16 ? 40.0f + x : 150.0f;
      near(expected, sparse.health(sparse.index(x, 4)), 0.07f, "sparse HP value " + x);
    }
    for (int x = 0; x < 256; x++) sparse.clear(sparse.index(x, 4));
    sparse.compactRows(); eq(0, sparse.countSolid(), "compacted rows clear completely");
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
      eq(TINT_HASHES[i], tintHash(game.grid), "oracle tint " + Maps.IDS[i]);
      check(game.grid.memoryBytes() < 170000, "compressed grid memory " + Maps.IDS[i] + " bytes=" + game.grid.memoryBytes());
      check(game.power.nodeCount > 0, "power nodes " + Maps.IDS[i]);
      int unknown = 0;
      for (int b = 0; b < game.bodies.count; b++) if (game.bodies.type[b] < 0) unknown++;
      System.out.println("MAP " + Maps.IDS[i] + " solid=" + SOLID[i] + " bodies=" + BODIES[i] + " people=" + PEOPLE[i] + " generic=" + unknown + " grid=" + game.grid.memoryBytes() + "B");
    }
    final int[] opens = {0};
    Game hooked = new Game();
    hooked.resources = new ResourceLoader() {
      public InputStream open(String path) { opens[0]++; return CoreTest.class.getResourceAsStream(path); }
    };
    check(hooked.loadMap(0), "platform resource hook loads city");
    eq(1, opens[0], "platform resource hook called once");
  }

  private static void initialPhysics() {
    for (int mode = 0; mode < 3; mode++) for (int map = 0; map < Maps.COUNT; map++) {
      Game game = new Game(); game.loadMap(map);
      long before = hpTotal(game.grid); int solid = game.grid.countSolid();
      game.structure.solve(game.grid, game.debris, 450.0f, false, mode, game.metadata);
      eq(0, game.structure.collapsedCells, "initial collapse " + Maps.IDS[map] + " mode=" + mode);
      eq(0, game.structure.overloadedCells, "initial overload " + Maps.IDS[map] + " mode=" + mode);
      check(before == hpTotal(game.grid) && solid == game.grid.countSolid(), "initial HP/solid stability " + Maps.IDS[map] + " mode=" + mode);
    }
  }

  private static void naturalStability() {
    for (int mode = 0; mode < 3; mode++) for (int map = 0; map < Maps.COUNT; map++) {
      Game game = new Game(); game.loadMap(map); game.physicsMode = mode;
      long before = hpTotal(game.grid); int solid = game.grid.countSolid();
      for (int tick = 0; tick < 900; tick++) game.update(0.05f);
      eq(0, game.people.deaths, "natural residents " + Maps.IDS[map] + " mode=" + mode);
      check(before == hpTotal(game.grid) && solid == game.grid.countSolid(),
          "natural grid/HP stability " + Maps.IDS[map] + " mode=" + mode);
      eq(0, game.debris.chunkCount, "natural debris " + Maps.IDS[map] + " mode=" + mode);
    }
  }

  private static long hpTotal(CellGrid grid) {
    long total = 0; for (int i = 0; i < grid.size; i++) total += grid.hp16(i); return total;
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

  private static int tintHash(CellGrid grid) {
    int hash = 0x811c9dc5;
    for (int i = 0; i < grid.size; i++) { hash ^= grid.tint(i); hash *= 16777619; }
    return hash;
  }

  private static void structure() {
    CellGrid grid = new CellGrid(32); DebrisSystem debris = new DebrisSystem(); StructuralSolver solver = new StructuralSolver();
    for (int x = 0; x < 32; x++) grid.set(grid.index(x, 259), 9, 0, 0);
    for (int y = 250; y <= 258; y++) grid.set(grid.index(16, y), 2, 0, 1);
    solver.solve(grid, debris, 450.0f);
    eq(0, solver.collapsedCells, "supported pillar stable");
    check(solver.memoryBytes() < 16384, "solver queue starts compact");
    grid.clear(grid.index(16, 258));
    solver.solve(grid, debris, 450.0f);
    eq(8, solver.collapsedCells, "unsupported pillar collapses");
    check(debris.chunkCount > 0, "rigid chunk created");
    eq(8, debris.shapeCount[0], "detached chunk retains its cell mask");
    near(0.0f, debris.vx[0], 0.0f, "detached chunk starts without random horizontal impulse");
    near(0.0f, debris.spin[0], 0.0f, "detached chunk starts without random spin");
    eq(0, grid.materialAt(16, 250), "collapsed cell removed");

    CellGrid weak = new CellGrid(24); DebrisSystem weakDebris = new DebrisSystem(); solver = new StructuralSolver();
    for (int x = 0; x < 24; x++) weak.set(weak.index(x, 259), 9, 0, 0);
    weak.set(weak.index(12, 258), 7, 0, 1);
    for (int y = 200; y <= 257; y++) weak.set(weak.index(12, y), 4, 6, 1);
    boolean fractured = false;
    for (int i = 0; i < 8; i++) {
      solver.solve(weak, weakDebris, 450.0f);
      if (solver.overloadedCells > 0 || solver.collapsedCells > 0) fractured = true;
    }
    check(fractured, "weight overload fractures weak support");
    solver.solve(weak, weakDebris, 450.0f, true); eq((weak.size + 3) / 4, solver.stress.length, "optional two-bit stress grid");
    solver.solve(weak, weakDebris, 450.0f, false); eq(0, solver.stress.length, "stress grid released when hidden");

    CellGrid large = new CellGrid(80); DebrisSystem largeDebris = new DebrisSystem(); StructuralSolver growing = new StructuralSolver();
    for (int y = 0; y < 63; y++) for (int x = 0; x < 80; x++) large.set(large.index(x, y), 2, 0, 1);
    for (int x = 0; x < 80; x++) large.set(large.index(x, 63), 9, 0, 0);
    growing.solve(large, largeDebris, 450.0f);
    eq(0, growing.collapsedCells, "large supported component stable");
    eq(8192 * 3 + Materials.COUNT * 4, growing.memoryBytes(), "solver queue grows on demand");

    // A weakened floor supported at both ends is not a free cantilever.
    CellGrid beam = new CellGrid(50); DebrisSystem beamDebris = new DebrisSystem();
    for (int x = 0; x < 50; x++) beam.set(beam.index(x, 255), 9, 0, 0);
    for (int y = 240; y < 255; y++) {
      beam.set(beam.index(10, y), 3, 0, 0); beam.set(beam.index(30, y), 3, 0, 0);
    }
    for (int x = 10; x <= 30; x++) {
      beam.set(beam.index(x, 239), 3, 0, 0); beam.set(beam.index(x, 238), 3, 0, 0);
    }
    int beamMiddle = beam.index(20, 239); beam.setHealth(beamMiddle, 0.5f);
    StructuralSolver detailed = new StructuralSolver();
    detailed.solve(beam, beamDebris, 450.0f, false, StructuralSolver.DETAILED);
    eq(0, detailed.collapsedCells, "supported detailed beam remains attached");
    eq(0, detailed.overloadedCells, "supported detailed beam is not sheared");
    check(beam.material(beamMiddle) != 0, "supported detailed beam keeps weakened cell");

    Game port = new Game(); check(port.loadMap(4), "port stability map");
    StructuralSolver portSolver = new StructuralSolver();
    portSolver.solve(port.grid, port.debris, 450.0f, false, StructuralSolver.DETAILED, port.metadata);
    check(port.metadata.sea, "port sea support metadata");
    eq(0, portSolver.collapsedCells, "port ship remains water supported");
  }

  private static void debris() {
    DebrisSystem pool = new DebrisSystem();
    for (int i = 0; i < DebrisSystem.MAX_CHUNKS + 8; i++) pool.spawnChunk(30.0f, 30.0f, 6.0f, 6.0f, 2, 1, 1.0f, 10.0f);
    eq(DebrisSystem.MAX_CHUNKS, pool.chunkCount, "chunk pool hard cap");
    eq(DebrisSystem.MAX_CHUNKS, activeChunks(pool), "chunk pool remains fully active");
    pool.spawnDust(30.0f, 30.0f, 0xa08070, DebrisSystem.MAX_DUST + 20);
    int dust = 0; boolean dustData = true;
    for (int i = 0; i < DebrisSystem.MAX_DUST; i++) {
      if (pool.dustLife[i] > 0.0f) dust++;
      if (pool.dustLife[i] != pool.dustMax[i] || pool.dustSize[i] < 1 || pool.dustSize[i] > 3) dustData = false;
    }
    eq(DebrisSystem.MAX_DUST, dust, "dust ring hard cap"); check(dustData, "dust lifetime and size initialized");
    pool.reset(); eq(0, pool.chunkCount, "debris reset chunks"); eq(0, activeDust(pool), "debris reset dust");

    CellGrid empty = new CellGrid(32); DebrisSystem rotating = new DebrisSystem();
    int body = rotating.spawnChunk(60.0f, 100.0f, 12.0f, 6.0f, 2, 12, 4.0f, 50.0f);
    rotating.vx[body] = rotating.vy[body] = 0.0f; rotating.spin[body] = 1.5f;
    rotating.update(empty, null, 0.1f, 450.0f);
    near(0.15f, rotating.angle[body], 0.0001f, "fragment angular integration");
    check(rotating.y[body] > 100.0f, "fragment gravity integration");
    float beforeBlastVy = rotating.vy[body];
    rotating.applyExplosion(60.0f, rotating.y[body], 50.0f, 100.0f);
    check(!rotating.resting[body] && rotating.vy[body] < beforeBlastVy, "explosion wakes and launches fragment");

    CellGrid floor = new CellGrid(32);
    for (int x = 0; x < floor.width; x++) floor.set(floor.index(x, 200), 9, 0, 0);
    DebrisSystem landing = new DebrisSystem();
    int landed = landing.spawnChunk(48.0f, 1190.0f, 12.0f, 6.0f, 4, 6, 5.0f, 10000.0f);
    landing.vx[landed] = landing.vy[landed] = landing.spin[landed] = 0.0f;
    for (int step = 0; step < 240; step++) landing.update(floor, null, 1.0f / 30.0f, 450.0f);
    check(landing.active[landed] && landing.resting[landed], "fragment settles on support");
    float cs = Math2.cos(landing.angle[landed]), sn = Math2.sin(landing.angle[landed]);
    float bottom = landing.y[landed] + Math.abs(sn) * landing.width[landed] * 0.5f + Math.abs(cs) * landing.height[landed] * 0.5f;
    check(bottom <= 1200.2f && bottom > 1180.0f, "contact resolves upward without tunneling");

    CellGrid breakable = new CellGrid(32);
    for (int x = 0; x < breakable.width; x++) breakable.set(breakable.index(x, 200), 2, 0, 0);
    int floorCell = breakable.index(2, 200); float oldFloorHp = breakable.health(floorCell);
    PeopleStore people = new PeopleStore(); people.allocate(1);
    int person = people.add(18.0f, 1200.0f, 100.0f, 22.0f, 85.0f, 1, 0, 0);
    DebrisSystem impact = new DebrisSystem();
    int projectile = impact.spawnChunk(18.0f, 1197.0f, 6.0f, 6.0f, 3, 8, 10.0f, 10000.0f);
    impact.vx[projectile] = 0.0f; impact.vy[projectile] = 300.0f; impact.spin[projectile] = 0.0f;
    boolean changed = impact.update(breakable, people, 0.01f, 450.0f);
    check(changed, "partial secondary impact marks structure dirty");
    check(breakable.health(floorCell) < oldFloorHp && breakable.material(floorCell) == 2, "secondary impact damages floor HP");
    check(people.hp[person] < 100.0f && people.panic[person] > 0.0f, "falling fragment injures and scares resident");

    DebrisSystem fracture = new DebrisSystem();
    int parent = fracture.spawnChunk(50.0f, 50.0f, 12.0f, 12.0f, 2, 20, 4.0f, 5.0f);
    int freeLaterSlot = fracture.spawnChunk(500.0f, 500.0f, 6.0f, 6.0f, 2, 1, 1.0f, 5.0f);
    fracture.active[freeLaterSlot] = false;
    fracture.applyExplosion(50.0f, 50.0f, 50.0f, 200.0f);
    eq(3, activeChunks(fracture), "impact fractures chunk into children");
    int cells = 0; float mass = 0.0f; boolean smaller = true;
    for (int i = 0; i < fracture.chunkCount; i++) if (fracture.active[i]) {
      cells += fracture.cells[i]; mass += fracture.mass[i];
      if (fracture.width[i] >= 12.0f || fracture.height[i] >= 12.0f) smaller = false;
    }
    eq(20, cells, "fracture conserves cells"); near(4.0f, mass, 0.0001f, "fracture conserves mass");
    check(smaller, "fracture children are smaller");

    DebrisSystem shaped = new DebrisSystem();
    int shapedParent = shaped.spawnChunk(80.0f, 80.0f, 18.0f, 12.0f, 2, 6, 12.0f, 5.0f);
    shaped.setShapeOrigin(shapedParent, 9.0f, 6.0f);
    for (int n = 0; n < 6; n++) shaped.setShapeCell(shapedParent, n, n % 3, n / 3, 2, n);
    shaped.hp[shapedParent] = 1.0f; shaped.applyExplosion(80.0f, 80.0f, 50.0f, 200.0f);
    int shapedCells = 0, shapedChildren = 0; float shapedMass = 0.0f;
    for (int i = 0; i < shaped.chunkCount; i++) if (shaped.active[i]) {
      shapedCells += shaped.cells[i]; shapedMass += shaped.mass[i];
      if (shaped.shapeCount[i] > 0) shapedChildren++;
    }
    eq(6, shapedCells, "shaped fracture conserves cells"); near(12.0f, shapedMass, 0.0001f, "shaped fracture conserves mass");
    check(shapedChildren >= 2, "shaped fracture children keep masks");
  }

  private static int activeChunks(DebrisSystem debris) {
    int count = 0; for (int i = 0; i < debris.chunkCount; i++) if (debris.active[i]) count++; return count;
  }
  private static int activeDust(DebrisSystem debris) {
    int count = 0; for (int i = 0; i < DebrisSystem.MAX_DUST; i++) if (debris.dustLife[i] > 0.0f) count++; return count;
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

    // A nuclear air burst must not detonate on the first solid pixel it
    // crosses; only its computed target altitude (or a ground-burst impact)
    // may trigger it.
    Game air = new Game(); check(air.loadMap(0), "air-burst map");
    int terrain = findDestructible(air.grid); check(terrain >= 0, "air-burst terrain");
    x = (terrain % air.grid.width) * 6.0f + 3.0f; y = (terrain / air.grid.width) * 6.0f + 3.0f;
    check(air.grid.solidPixel(x, y), "air-burst starts inside terrain");
    int bomb = air.effects.spawn(EffectSystem.BOMB, x, y, 5.0f);
    check(bomb >= 0, "air-burst fixture");
    air.effects.flags[bomb] = 101; air.effects.ty[bomb] = y + 60.0f;
    air.effects.radius[bomb] = 20.0f; air.effects.power[bomb] = 100.0f;
    air.effects.update(air, 0.01f);
    eq(EffectSystem.BOMB, air.effects.type[bomb], "air burst ignores premature terrain impact");
    eq(0, air.radiationSystem.count, "premature impact creates no nuclear source");
    air.effects.ty[bomb] = y - 60.0f; air.effects.y[bomb] = air.effects.ty[bomb]; air.effects.update(air, 0.01f);
    eq(EffectSystem.NONE, air.effects.type[bomb], "air burst detonates at target altitude");
    eq(1, air.radiationSystem.count, "target air burst creates radiation source");
    Game ground = new Game(); check(ground.loadMap(0), "ground-burst map");
    int groundBomb = ground.effects.spawn(EffectSystem.BOMB, x, y, 5.0f);
    ground.effects.flags[groundBomb] = 101 | EffectSystem.NUKE_GROUND_BURST;
    ground.effects.ty[groundBomb] = y + 60.0f; ground.effects.radius[groundBomb] = 20.0f; ground.effects.power[groundBomb] = 100.0f;
    ground.effects.update(ground, 0.01f);
    eq(EffectSystem.NONE, ground.effects.type[groundBomb], "ground burst still detonates on terrain");
    check(ground.radiationSystem.count == 1 && ground.radiationSystem.D0[0] > air.radiationSystem.D0[0],
        "ground-burst bit remains distinct from air-burst flags");
  }

  private static void reactorEmergency() {
    Game npp = new Game(); SoundProbe probe = new SoundProbe(); npp.listener = probe;
    check(npp.loadMap(5), "NPP emergency map");
    for (int i = 0; i < 10; i++) npp.update(0.05f);
    eq(0, npp.reactorAlarmLevel, "healthy reactor has no alarm");
    near(320.0f, npp.reactorTemperature, 0.01f, "recovered normal reactor temperature");
    Game turbineFailure = new Game(); turbineFailure.loadMap(5);
    int turbine = Props.find("turbine");
    for (int i = 0; i < turbineFailure.bodies.count; i++) if (turbineFailure.bodies.type[i] == turbine) {
      turbineFailure.bodies.kill(i); break;
    }
    for (int i = 0; i < 8; i++) turbineFailure.update(0.05f);
    eq(1, turbineFailure.reactorAlarmLevel, "turbine damage trips the original SCRAM chain");
    int core = Props.find("gcn");
    for (int i = 0; i < npp.bodies.count; i++) if (npp.bodies.type[i] == core) { npp.bodies.kill(i); break; }
    for (int i = 0; i < 8; i++) npp.update(0.05f);
    eq(1, npp.reactorAlarmLevel, "cooling loss triggers SCRAM warning");
    check(npp.reactorRedLight && npp.reactorScram, "SCRAM enables red emergency lighting");
    check(probe.sirens > 0, "SCRAM uses original NPP siren");
    check(probe.civil > 0, "SCRAM raises separate civil-defence siren");
    npp.people.hidden[0] = true; npp.people.shelterStage[0] = 1; npp.people.mode[0] = PeopleStore.HIDE; npp.people.stateTime[0] = 10.0f;
    npp.reactorTemperature = 900.0f;
    for (int i = 0; i < 8; i++) npp.update(0.05f);
    eq(2, npp.reactorAlarmLevel, "radioactive release raises civil alarm");
    check(npp.reactorRelease && npp.radiationSystem.count == 1, "NPP release creates one cloud");
    int fleeing = 0;
    for (int i = 0; i < npp.people.count; i++) if (npp.people.mode[i] == PeopleStore.FLEE && !npp.people.hidden[i]) fleeing++;
    check(fleeing > 0, "release evacuates residents instead of hiding them");
    check(!npp.people.hidden[0] && npp.people.mode[0] == PeopleStore.FLEE, "sheltered residents leave for evacuation");
    check(probe.sirens >= 2, "release repeats the reactor siren");
    check(probe.civil >= 2, "release repeats the civil-defence siren");
  }

  private static void sirenRouting() {
    Game ordinary = new Game(); SoundProbe ordinaryProbe = new SoundProbe(); ordinary.listener = ordinaryProbe;
    ordinary.loadMap(0); ordinary.explode(ordinary.cameraX, ordinary.cameraY, 90.0f, 100.0f, 0.0f);
    eq(0, ordinaryProbe.civil, "ordinary explosion has no civil siren");

    Game service = new Game(); SoundProbe serviceProbe = new SoundProbe(); service.listener = serviceProbe;
    service.loadMap(0); service.services.notifyIncident(ServiceSystem.FIRE, service.cameraX, service.cameraY);
    service.services.update(service, 0.9f);
    check(serviceProbe.service > 0, "fire dispatch has separate service sound");
    check("fireSiren24".equals(serviceProbe.lastService), "recovered 2.4 second dispatch siren phase");
    eq(0, serviceProbe.civil, "fire dispatch does not raise civil siren");
    for (int i = 0; i < 200 && (!serviceProbe.fire32 || !serviceProbe.fire22); i++) service.services.update(service, 0.25f);
    check(serviceProbe.fire32, "recovered 3.2 second approaching siren phase");
    check(serviceProbe.fire22, "recovered 2.2 second departure siren phase");
    for (int i = 0; i < 100; i++) service.services.update(service, 0.25f);
    eq(0, service.services.activeCount(ServiceSystem.FIRE), "expired fire incident does not recall a unit");

    Game nuclear = new Game(); SoundProbe nuclearProbe = new SoundProbe(); nuclear.listener = nuclearProbe;
    nuclear.loadMap(0); nuclear.nuclearExplode(nuclear.cameraX, nuclear.cameraY, 1.0f, 30.0f, 100.0f, 0.0f, false);
    eq(1, nuclearProbe.civil, "nuclear event raises one civil warning");
    nuclear.nuclearExplode(nuclear.cameraX, nuclear.cameraY, 1.0f, 30.0f, 100.0f, 0.0f, false);
    eq(1, nuclearProbe.civil, "nuclear warning cooldown prevents duplicates");
  }

  private static final class SoundProbe implements GameListener {
    int sirens, civil, service, med;
    String lastService; boolean fire32, fire22;
    public void sound(String id, float volume) {
      if (id.equals("reactorSiren")) sirens++;
      if (id.equals("siren")) civil++;
      if (id.equals("serviceSiren") || id.startsWith("fireSiren") || id.startsWith("policeSiren")) {
        service++; lastService = id;
        if (id.equals("fireSiren32")) fire32 = true;
        if (id.equals("fireSiren22")) fire22 = true;
      }
      if (id.equals("medSiren") || id.startsWith("medSiren")) med++;
    }
    public void message(String text) { }
  }

  private static void radiation() {
    RadiationSystem air = new RadiationSystem(), ground = new RadiationSystem();
    air.addNuclear(1000.0f, 0.0f, 15.0f, false, 460.0f);
    ground.addNuclear(1000.0f, 0.0f, 15.0f, true, 460.0f);
    near(19.2f, air.D0[0], 0.01f, "air-burst fallout scale");
    near(1600.0f, ground.D0[0], 0.01f, "ground-burst fallout scale");
    near(858.0f, air.L[0], 0.01f, "fallout length");
    near(40000.0f, air.prompt[0], 0.01f, "prompt scale");
    check(air.rate(1000.0f, 1.0f) > 1000.0f, "dosimeter prompt pulse");
    near(0.0f, air.falloutRate(1000.0f, 0.5f), 0.0f, "no fallout before 0.8 seconds");
    check(air.falloutRate(1000.0f, 6.0f) > 0.0f, "fallout after ramp starts");
    check(air.falloutRate(1010.0f, 6.0f) != air.falloutRate(1010.0f, 30.0f), "fallout decays and drifts");

    RadiationSystem npp = new RadiationSystem();
    npp.addNppRelease(500.0f, 0.0f, 1.0f, -1);
    npp.addNppRelease(500.0f, 1.0f, 1.0f, 1);
    npp.addNppRelease(500.0f, 2.0f, 2.0f, 1);
    eq(1, npp.count, "single NPP cloud");
    near(60.0f, npp.D0[0], 0.0f, "NPP release cap");
    near(1.0f, npp.core[0], 0.0f, "NPP core cap");
    check(npp.rate(400.0f, 8.0f) > RadiationSystem.BACKGROUND, "NPP wind cloud");

    for (int i = 0; i < 8; i++) air.addNuclear(2000.0f + i, i, 1.0f, true, 100.0f);
    eq(RadiationSystem.MAX_SOURCES, air.count, "six radiation sources maximum");
    near(2002.0f, air.x[0], 0.0f, "oldest radiation source shifted out");

    CellGrid shield = new CellGrid(8);
    shield.set(shield.index(3, 100), 3, 0, 0);
    near(0.5f, ground.shielding(shield, 18.0f, 103 * 6.0f), 0.0001f, "overhead concrete attenuation");
    near(RadiationSystem.BACKGROUND, new RadiationSystem().rate(shield, 18.0f, 103 * 6.0f, 0.0f), 0.0000001f,
        "empty radiation keeps constant background without shielding scan");
    check(RadiationSystem.formatDose(0.000015f).endsWith("Р/ч"), "dosimeter units");
  }

  private static void residents() {
    CellGrid grid = new CellGrid(64);
    for (int x = 0; x < grid.width; x++) grid.set(grid.index(x, 200), 9, 0, 0);
    PeopleStore people = new PeopleStore(); people.allocate(4);
    FireSystem fire = new FireSystem(); RadiationSystem radiation = new RadiationSystem();
    int person = people.add(18.0f, 1198.0f, 100.0f, 22.0f, 85.0f, 1, 0, 0);
    people.hold(person, 24.0f, 1160.0f);
    check(people.held[person] && people.rag.active[person], "grab creates physical body");
    people.release(person, 20.0f, -10.0f); check(!people.held[person], "grab release");
    people.hurt(person, 60.0f, 0.0f, 0.0f);
    check(people.rag.active[person] && people.state[person] == PeopleStore.DOWN, "injury starts ragdoll");
    boolean finite = true;
    for (int step = 0; step < 20; step++) people.update(grid, fire, 1.0f / 30.0f, 450.0f, radiation, step / 30.0f);
    for (int n = 0; n < RagdollStore.POINTS; n++) {
      int at = person * RagdollStore.POINTS + n;
      if (Float.isNaN(people.rag.x[at]) || Float.isInfinite(people.rag.y[at])) finite = false;
    }
    check(finite, "ragdoll finite positions");
    people.hurt(person, 100.0f, 0.0f, 0.0f);
    eq(PeopleStore.DEAD, people.state[person], "lethal injury");
    eq(1, people.deaths, "death counted once");

    int getup = people.add(42.0f, 1198.0f, 100.0f, 22.0f, 85.0f, 1, 1, 0);
    people.startRagdoll(getup, 0.0f, 0.0f, 0.0f);
    float time = 1.0f;
    for (int step = 0; step < 240; step++) { time += 1.0f / 30.0f; people.update(grid, fire, 1.0f / 30.0f, 450.0f, radiation, time); }
    check(!people.rag.active[getup] && people.state[getup] == PeopleStore.ALIVE, "resident gets up after settling");

    int spawn = people.spawnAt(grid, 60.0f, 1100.0f);
    check(spawn >= 0 && people.y[spawn] <= 1200.0f, "people tool finds floor and clearance");
    people.ignite(spawn); check(people.burnTime[spawn] > 0.0f, "resident ignition");
    people.douse(people.x[spawn], people.y[spawn], 20.0f); near(0.0f, people.burnTime[spawn], 0.0f, "resident douse");

    int exposed = people.add(90.0f, 1198.0f, 100.0f, 22.0f, 85.0f, 1, 2, 0);
    radiation.addNppRelease(90.0f, time, 1.0f, 1);
    for (int step = 0; step < 500; step++) { time += 0.05f; people.update(grid, fire, 0.05f, 450.0f, radiation, time); }
    check(people.dose[exposed] > 0.8f && people.sick[exposed] > 0.0f, "resident accumulated dose and sickness");
  }

  private static void fluidsAndServices() {
    Game fluidGame = new Game(); check(fluidGame.loadMap(0), "fluid test map");
    FluidSystem fluid = new FluidSystem();
    int added = fluid.add(fluidGame.grid, 15.0f, 15.0f, 255);
    eq(255, added, "water source volume");
    for (int i = 0; i < 300; i++) fluid.update(fluidGame, 0.11f, StructuralSolver.NORMAL);
    check(fluid.count > 1 && fluid.rejected == 0, "water falls and levels into neighboring cells");
    eq(255, totalFluid(fluid), "water volume conserved without lava");

    // A full bounded table must report a rejected insertion as zero. Otherwise
    // transfer() can subtract water from a source that never delivered it.
    CellGrid fullGrid = new CellGrid(64);
    FluidSystem full = new FluidSystem();
    for (int i = 0; i < FluidSystem.MAX_CELLS; i++)
      full.add(fullGrid, (i % fullGrid.width) * 6.0f + 3.0f, (i / fullGrid.width) * 6.0f + 3.0f, 1);
    int fullVolume = totalFluid(full), newCell = FluidSystem.MAX_CELLS;
    int refused = full.add(fullGrid, (newCell % fullGrid.width) * 6.0f + 3.0f,
        (newCell / fullGrid.width) * 6.0f + 3.0f, 20);
    eq(0, refused, "full fluid table rejects new volume");
    eq(0, full.amountAt(newCell), "rejected fluid is not stored");
    eq(fullVolume, totalFluid(full), "rejected fluid preserves total volume");
    boolean wet = false;
    for (int i = 0; i < FluidSystem.TABLE; i++) if (fluid.amount[i] != 0) {
      int at = fluid.cell[i];
      wet = fluid.submersion(fluidGame.grid, fluidGame.metadata, (at % fluidGame.grid.width) * 6.0f + 3.0f,
          (at / fluidGame.grid.width) * 6.0f + 5.5f) > 0.0f;
      if (wet) break;
    }
    check(wet, "fluid submersion query");

    CellGrid fireGrid = new CellGrid(8);
    int burning = fireGrid.index(3, 3); fireGrid.set(burning, 1, 0, 0);
    FireSystem fire = new FireSystem(); check(fire.ignite(fireGrid, burning), "fluid fire fixture ignites");
    FluidSystem dousing = new FluidSystem(); dousing.add(fireGrid, 27.0f, 21.0f, 255);
    fire.update(fireGrid, dousing, new DebrisSystem(), 0.1f);
    check(!fire.active[0], "adjacent water extinguishes fire");

    Game bodyGame = new Game(); bodyGame.loadMap(0); bodyGame.bodies.allocate(1);
    bodyGame.bodies.add(0, -1, 15.0f, 15.0f, 6.0f, 6.0f, 0.2f, 10.0f);
    FluidSystem buoyancy = new FluidSystem(); buoyancy.add(bodyGame.grid, 15.0f, 15.0f, 255);
    bodyGame.bodies.update(bodyGame.grid, buoyancy, bodyGame.metadata, bodyGame.debris, 0.05f, 450.0f);
    check(bodyGame.bodies.vy[0] < 0.0f, "light body receives buoyancy");
    eq(-1, bodyGame.bodies.damageArea(15.0f, 15.0f, 30.0f, 100.0f, 0.0f),
        "generic body damage has no prop explosion");
    check(!bodyGame.bodies.alive(0), "generic body can be destroyed safely");

    CellGrid stackGrid = new CellGrid(16);
    for (int x = 0; x < 16; x++) stackGrid.set(stackGrid.index(x, 200), 9, 0, 0);
    int stackFloor = stackGrid.index(8, 200), stackFloorHp = stackGrid.hp16(stackFloor);
    BodyStore stack = new BodyStore(); stack.allocate(2); DebrisSystem stackDebris = new DebrisSystem();
    int lower = stack.add(1, -1, 48.0f, 1191.0f, 48.0f, 18.0f, 22.0f, 260.0f);
    int upper = stack.add(1, -1, 48.0f, 1173.0f, 48.0f, 18.0f, 22.0f, 260.0f);
    stack.flags[lower] = BodyStore.F_SLEEPING; stack.flags[upper] = BodyStore.F_SLEEPING;
    for (int i = 0; i < 40; i++) stack.update(stackGrid, null, null, stackDebris, 0.05f, 450.0f);
    near(1173.0f, stack.y[upper], 0.02f, "sleeping body stack remains supported");
    eq(stackFloorHp, stackGrid.hp16(stackFloor), "sleeping stack does not damage terrain");
    stack.wake(upper); stack.vy[upper] = 200.0f;
    for (int i = 0; i < 10; i++) stack.update(stackGrid, null, null, stackDebris, 0.05f, 450.0f);
    check(stack.y[upper] <= 1173.01f && stack.y[upper] > 1160.0f, "falling body lands on lower body");
    eq(stackFloorHp, stackGrid.hp16(stackFloor), "body-to-body impact does not tunnel into terrain");

    Game serviceGame = new Game(); serviceGame.loadMap(0);
    serviceGame.services.notifyIncident(ServiceSystem.FIRE, serviceGame.cameraX, serviceGame.cameraY);
    serviceGame.services.notifyIncident(ServiceSystem.MED, serviceGame.people.x[0], serviceGame.people.y[0]);
    serviceGame.services.notifyIncident(ServiceSystem.POLICE, serviceGame.cameraX, serviceGame.cameraY);
    serviceGame.services.update(serviceGame, 0.9f);
    check(serviceGame.services.activeCount(ServiceSystem.FIRE) == 1
        && serviceGame.services.activeCount(ServiceSystem.MED) == 1
        && serviceGame.services.activeCount(ServiceSystem.POLICE) == 1, "automatic emergency dispatch");
    int serviceSolid = serviceGame.grid.countSolid();
    for (int i = 0; i < 4; i++) serviceGame.update(0.05f);
    eq(serviceSolid, serviceGame.grid.countSolid(), "departing service vehicles do not explode");
    for (int i = 0; i < 12; i++) serviceGame.services.notifyIncident(ServiceSystem.FIRE, serviceGame.cameraX, serviceGame.cameraY);
    serviceGame.services.update(serviceGame, 0.9f);
    check(serviceGame.services.activeCount(ServiceSystem.FIRE) == 1 && serviceGame.services.memoryBytes() < 1024,
        "service pool remains bounded");

    Game minor = new Game(); minor.loadMap(0); minor.people.hp[0] = 40.0f;
    minor.services.update(minor, 0.9f);
    eq(0, minor.services.activeCount(ServiceSystem.MED), "walking minor injury is not an ambulance patient");
    minor.people.startRagdoll(0, 0.0f, 0.0f, 0.0f); minor.people.downTime[0] = 2.1f;
    minor.services.update(minor, 0.9f);
    eq(1, minor.services.activeCount(ServiceSystem.MED), "recovered ragdoll medical eligibility");

    Game recovery = new Game(); recovery.loadMap(0);
    recovery.people.hurt(0, 1000.0f, 0.0f, 0.0f); recovery.people.rag.sleeping[0] = true;
    recovery.people.downTime[0] = 2.1f;
    recovery.services.notifyIncident(ServiceSystem.MED, recovery.people.centerX(0), recovery.people.centerY(0));
    recovery.services.update(recovery, 0.9f); recovery.services.update(recovery, 100.0f);
    for (int i = 0; i < 12; i++) recovery.services.update(recovery, 0.25f);
    eq(PeopleStore.GONE, recovery.people.state[0], "sleeping dead ragdoll is recovered by ambulance");
    eq(1, recovery.people.deaths, "dead recovery does not add a death or saved-life statistic");
    eq(0, recovery.services.saved, "dead recovery is not counted as a saved life");

    Game bodyFire = new Game(); bodyFire.loadMap(0);
    int burningBody = -1, firetruck = Props.find("firetruck");
    for (int i = 0; i < bodyFire.bodies.count; i++) if (bodyFire.bodies.alive(i) && bodyFire.bodies.type[i] != firetruck) {
      burningBody = i; break;
    }
    check(burningBody >= 0, "burning prop service fixture");
    if (burningBody >= 0) {
      bodyFire.cameraX = bodyFire.bodies.x[burningBody] + 1000.0f; bodyFire.cameraY = bodyFire.bodies.y[burningBody];
      bodyFire.bodies.flags[burningBody] |= BodyStore.F_BURNING;
      bodyFire.services.update(bodyFire, 0.9f);
      eq(1, bodyFire.services.activeCount(ServiceSystem.FIRE), "burning prop requests fire service");
      bodyFire.services.update(bodyFire, 100.0f); bodyFire.services.update(bodyFire, 0.25f);
      eq(0, bodyFire.bodies.flags[burningBody] & BodyStore.F_BURNING, "fire crew targets and douses burning prop");
    }

    Game raid = new Game(); raid.loadMap(0);
    raid.people.airRaid(raid.grid, raid.metadata, raid.cameraX, raid.cameraY, 8.0f);
    for (int i = 0; i < 160; i++) raid.update(0.05f);
    int sheltered = 0;
    for (int i = 0; i < raid.people.count; i++) if (raid.people.shelterStage[i] >= 2) sheltered++;
    check(sheltered > 0, "residents reach simulated shelter during air raid");

    Game farRaid = new Game(); farRaid.loadMap(0); farRaid.selectTool(Tools.find("nuke"));
    farRaid.toolValues[farRaid.toolIndex] = 1;
    farRaid.pointerDown(farRaid.grid.width * 6.0f - 20.0f, 1000.0f); farRaid.pointerUp();
    for (int i = 0; i < 900 && farRaid.radiationSystem.count == 0; i++) farRaid.update(0.05f);
    check(farRaid.radiationSystem.count > 0, "bomber reaches distant nuclear target before expiring");
  }

  private static void hudCaching() {
    Game game = new Game(); game.loadMap(0);
    String power = game.powerText(), status = game.statusText();
    check(power == game.powerText(), "steady power HUD does not allocate a new string");
    check(status == game.statusText(), "steady status HUD does not allocate a new string");
    game.adjustPower(1);
    check(power != game.powerText(), "power HUD invalidates after slider change");
    game.people.hurt(0, 1.0f, 0.0f, 0.0f);
    check(status != game.statusText(), "status HUD invalidates after injury");
  }

  private static int totalFluid(FluidSystem fluid) {
    int total = 0; for (int i = 0; i < FluidSystem.TABLE; i++) total += fluid.amount[i] & 255; return total;
  }

  private static int findDestructible(CellGrid grid) {
    for (int y = 80; y < 210; y++) for (int x = grid.width / 2; x < grid.width - 1; x++) {
      int at = grid.index(x, y), m = grid.material(at);
      if (m != 0 && !Materials.FIXED[m]) return at;
    }
    return -1;
  }
}
