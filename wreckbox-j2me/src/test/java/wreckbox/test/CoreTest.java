package wreckbox.test;

import wreckbox.core.BodyStore;
import wreckbox.core.Buildings;
import wreckbox.core.CellGrid;
import wreckbox.core.DebrisSystem;
import wreckbox.core.EffectSystem;
import wreckbox.core.FireSystem;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Materials;
import wreckbox.core.Math2;
import wreckbox.core.PeopleStore;
import wreckbox.core.Props;
import wreckbox.core.RadiationSystem;
import wreckbox.core.RagdollStore;
import wreckbox.core.StructuralSolver;
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
    content(); mathAndCells(); maps(); structure(); debris(); strikeOracle(); effects(); radiation(); residents();
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

    CellGrid cow = new CellGrid(8); int emptyBytes = cow.memoryBytes();
    cow.set(10, 4, 0, 0); check(cow.memoryBytes() >= emptyBytes + 8 * 4, "changed row expands on demand");
    cow.clear(10); eq(emptyBytes, cow.memoryBytes(), "fully cleared row releases storage");
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
    solver.solve(weak, weakDebris, 450.0f, true); eq((weak.size + 3) / 4, solver.stress.length, "optional two-bit stress grid");
    solver.solve(weak, weakDebris, 450.0f, false); eq(0, solver.stress.length, "stress grid released when hidden");

    CellGrid large = new CellGrid(80); DebrisSystem largeDebris = new DebrisSystem(); StructuralSolver growing = new StructuralSolver();
    for (int y = 0; y < 63; y++) for (int x = 0; x < 80; x++) large.set(large.index(x, y), 2, 0, 1);
    for (int x = 0; x < 80; x++) large.set(large.index(x, 63), 9, 0, 0);
    growing.solve(large, largeDebris, 450.0f);
    eq(0, growing.collapsedCells, "large supported component stable");
    eq(8192 * 3 + Materials.COUNT * 4, growing.memoryBytes(), "solver queue grows on demand");
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

  private static int findDestructible(CellGrid grid) {
    for (int y = 80; y < 210; y++) for (int x = grid.width / 2; x < grid.width - 1; x++) {
      int at = grid.index(x, y), m = grid.material(at);
      if (m != 0 && !Materials.FIXED[m]) return at;
    }
    return -1;
  }
}
