package wreckbox.core;

import java.io.IOException;
import java.io.InputStream;

/** Portable Wreckbox simulation. MIDP code only supplies input, Graphics and sound. */
public final class Game {
  public static final float EARTH_GRAVITY = 450.0f;
  public static final float[] GRAVITIES = {450.0f, 75.0f, 0.0f};
  public static final float[] NUKE_KT = {0.02f, 0.5f, 2.0f, 15.0f, 21.0f, 100.0f, 500.0f, 1000.0f, 15000.0f, 50000.0f};
  public static final String[] NUKE_NAMES = {"Дэви Крокетт", "", "", "Малыш", "Толстяк", "", "", "", "Кастл Браво", "Царь-бомба"};
  private static final float[] BLAST_MULTIPLIER = {0, 1.15f, 0.8f, 0.7f, 0.35f, 4.0f, 1.7f, 0.45f, 0, 0, 1.5f, 1.0f, 0.6f, 1.0f, 0.7f, 1.25f, 1.6f, 2.2f, 0.4f, 0.6f, 0.85f};
  private static final boolean[] CRACKABLE = {false, false, true, true, false, false, false, false, false, false, true, false, false, true, true, true, true, true, false, true, true};
  public static final int[] BUILD_MATERIAL = {1, 2, 3, 3, 4, 5, 6, 10, 14, 15, 20, 19, 18, 17, 16};
  public static final int[] BUILD_TINT = {0, 0, 0, 15, 6, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0};
  public static final int[] BUILD_FLAGS = {1, 1, 1, 3, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1, 1};

  public CellGrid grid;
  public final BodyStore bodies = new BodyStore();
  public final PeopleStore people = new PeopleStore();
  public final PowerGrid power = new PowerGrid();
  public final DebrisSystem debris = new DebrisSystem();
  public final FireSystem fire = new FireSystem();
  public final EffectSystem effects = new EffectSystem();
  public final FluidSystem fluids = new FluidSystem();
  public final ServiceSystem services = new ServiceSystem();
  public final RadiationSystem radiationSystem = new RadiationSystem();
  public final StructuralSolver structure = new StructuralSolver();
  public final BuildingGenerator buildingGenerator = new BuildingGenerator();
  public final Random random = new Random(0x1a8b0);
  public GameListener listener;
  /** Optional Android/host asset source; null keeps ordinary JAR resources. */
  public ResourceLoader resources;
  public MapMetadata metadata;

  public int mapIndex, toolIndex = Tools.find("hammer"), gravityIndex, physicsMode = StructuralSolver.NORMAL;
  public final int[] toolValues = new int[Tools.COUNT];
  public int buildMaterial = 1, propSelection = Props.find("sofa"), buildingSelection;
  public float gravity = EARTH_GRAVITY;
  public float cameraX, cameraY, zoom = 0.34f;
  public float pointerX, pointerY, pointerVelocityX, pointerVelocityY;
  public boolean held, paused, slowMotion, night, buildMode, uiHidden;
  public boolean nuclearGroundBurst, showStress, bloodEnabled = true, servicesEnabled = true, epicSlow = true, showToasts = true, effectsEnabled = true;
  public boolean structuralDirty;
  public float time, shake, radiation, doseRate = RadiationSystem.BACKGROUND, reactorTemperature = 320.0f, reactorHydrogen;
  /** 0 = normal, 1 = SCRAM/civil warning, 2 = radioactive release/evacuation. */
  public int reactorAlarmLevel;
  public boolean reactorRedLight, reactorScram, reactorRelease;
  public float reactorSirenClock, reactorEvacuationClock, incidentSirenClock, alarmFlash;
  private float reactorBackupTime;
  public String message = "", loadError;
  public float messageTime;

  private float structureClock, continuousClock, specialClock, lavaClock, epicTimer;
  private float quakeTime, quakeStrength, tornadoTime, tornadoX, tornadoY, tornadoStrength;
  private float tsunamiTime, tsunamiDirection, tsunamiHeight, holeTime, holeX, holeY, holeRadius;
  private float eruptionTime, eruptionStrength;
  private int heldKind = -1, heldIndex = -1; // 0 body, 1 debris, 2 person
  private boolean initialLoad = true;
  // HUD values are requested on every paint. Cache their strings so a steady
  // scene does not create short-lived objects and trigger phone GC pauses.
  private String cachedPowerText, cachedStatusText;
  private int cachedPowerTool = -1, cachedPowerValue;
  private int cachedStatusMap = -1, cachedStatusAlarm = -1;
  private int cachedStatusTemperature, cachedStatusHydrogen;
  private int cachedStatusInjured = -1, cachedStatusSick = -1, cachedStatusLiving = -1;
  private boolean cachedStatusNpp;

  public Game() {
    for (int i = 0; i < toolValues.length; i++) toolValues[i] = Tools.SLIDER_DEFAULT[i];
  }

  public boolean loadMap(int index) {
    if (index < 0) index = Maps.COUNT - 1;
    if (index >= Maps.COUNT) index = 0;
    mapIndex = index;
    String path = "/maps/" + Maps.IDS[index] + ".wbmp";
    InputStream stream = resources != null ? resources.open(path) : getClass().getResourceAsStream(path);
    if (stream == null) { loadError = "Нет ресурса " + path; return false; }
    grid = new CellGrid(Maps.WIDTHS[index]);
    MapLoader loader = new MapLoader();
    try {
      loader.load(stream, grid, bodies, people, power);
      metadata = loader.metadata;
    } catch (IOException e) {
      loadError = e.toString();
      try { stream.close(); } catch (IOException ignored) {}
      return false;
    }
    debris.reset(); fire.reset(); effects.reset(); fluids.reset(); services.reset(); radiationSystem.reset(); doseRate = RadiationSystem.BACKGROUND;
    people.debris = debris; people.bloodEnabled = bloodEnabled; people.physicsMode = physicsMode;
    structuralDirty = false; structureClock = 0.0f; epicTimer = 0.0f; time = 0.0f;
    quakeTime = tornadoTime = tsunamiTime = holeTime = eruptionTime = 0.0f;
    radiation = reactorHydrogen = 0.0f; reactorTemperature = 320.0f;
    reactorAlarmLevel = 0; reactorRedLight = reactorScram = reactorRelease = false;
    reactorSirenClock = reactorEvacuationClock = incidentSirenClock = alarmFlash = reactorBackupTime = 0.0f;
    heldKind = -1; heldIndex = -1; held = false;
    loadError = null; initialLoad = true;
    resetCamera();
    setMessage(Maps.NAMES[index] + " · Wreckbox 1.8");
    return true;
  }

  public void resetCamera() {
    int bestY = 200, bestX = grid.width / 2;
    for (int x = 8; x < grid.width - 8; x += 4) {
      for (int y = 20; y < 205; y++) {
        int m = grid.materialAt(x, y);
        if (m != 0 && !Materials.FIXED[m] && m != 12) {
          if (y < bestY) { bestY = y; bestX = x; }
          break;
        }
      }
    }
    cameraX = bestX * 6.0f;
    cameraY = metadata != null && metadata.base ? 1190.0f : 1215.0f;
    zoom = metadata != null && (metadata.base || metadata.npp) ? 0.25f : 0.34f;
  }

  public void pan(float screenDx, float screenDy) {
    cameraX -= screenDx / zoom; cameraY -= screenDy / zoom;
    clampCamera();
  }
  public void zoomBy(float factor) { zoom = Math2.clamp(zoom * factor, 0.12f, 2.0f); clampCamera(); }
  private void clampCamera() {
    cameraX = Math2.clamp(cameraX, 0.0f, grid.width * 6.0f);
    cameraY = Math2.clamp(cameraY, 100.0f, 1500.0f);
  }
  public float worldX(float screenX, int width) { return cameraX + (screenX - width * 0.5f) / zoom; }
  public float worldY(float screenY, int top, int height) { return cameraY + (screenY - (top + height * 0.5f)) / zoom; }

  public void selectTool(int index) {
    if (index < 0 || index >= Tools.COUNT) return;
    if (heldKind >= 0) pointerUp();
    toolIndex = index; held = false; heldKind = -1;
    buildMode = "build".equals(Tools.MODES[index]);
    if ("both".equals(Tools.MODES[index])) buildMode = false;
    setMessage(Tools.NAMES[index]);
  }
  public void adjustPower(int delta) {
    int min = Tools.SLIDER_MIN[toolIndex], max = Tools.SLIDER_MAX[toolIndex];
    if (max <= min) return;
    toolValues[toolIndex] += delta;
    if (toolValues[toolIndex] < min) toolValues[toolIndex] = min;
    if (toolValues[toolIndex] > max) toolValues[toolIndex] = max;
    setMessage(powerText());
  }
  public String powerText() {
    int value = toolValues[toolIndex];
    if (cachedPowerText != null && cachedPowerTool == toolIndex && cachedPowerValue == value) return cachedPowerText;
    cachedPowerTool = toolIndex; cachedPowerValue = value;
    if ("nuke".equals(Tools.IDS[toolIndex])) {
      float kt = NUKE_KT[value - 1];
      String text = kt >= 1000.0f ? (kt / 1000.0f) + " Мт" : kt + " кт";
      cachedPowerText = NUKE_NAMES[value - 1].length() > 0 ? text + " · " + NUKE_NAMES[value - 1] : text;
    } else cachedPowerText = Tools.SLIDER_LABELS[toolIndex] + ": " + value;
    return cachedPowerText;
  }
  public void cycleGravity() { gravityIndex = (gravityIndex + 1) % 3; gravity = GRAVITIES[gravityIndex]; setMessage(gravityIndex == 0 ? "1g" : gravityIndex == 1 ? "Луна" : "0g"); }
  public void cyclePhysics() {
    physicsMode = (physicsMode + 1) % 3;
    setMessage("Физика: " + (physicsMode == 0 ? "экономная" : physicsMode == 1 ? "нормальная" : "точная"));
  }
  public String physicsText() { return physicsMode == 0 ? "экономная" : physicsMode == 1 ? "нормальная" : "точная"; }
  public void detonate() { effects.detonateCharges(); setMessage("Подрыв зарядов"); }

  public void pointerDown(float x, float y) {
    pointerX = x; pointerY = y; pointerVelocityX = pointerVelocityY = 0.0f; held = true; continuousClock = 0.0f;
    String id = Tools.IDS[toolIndex];
    if (id.equals("hammer")) { hammer(x, y); continuousClock = 0.17f; }
    else if (id.equals("ball")) dropBall(x, y);
    else if (id.equals("dynamite")) placeCharge(x, y, false);
    else if (id.equals("demolish")) placeCharge(x, y, true);
    else if (id.equals("bomb")) dropBomb(x, y, false);
    else if (id.equals("nuke")) dropBomb(x, y, true);
    else if (id.equals("rocket")) fireRocket(x, y);
    else if (id.equals("molotov")) throwMolotov(x, y);
    else if (id.equals("volcano")) startVolcano();
    else if (id.equals("laser")) laser(x, y);
    else if (id.equals("quake")) startQuake();
    else if (id.equals("tornado")) startTornado(x, y);
    else if (id.equals("tsunami")) startTsunami(x);
    else if (id.equals("meteor")) dropMeteor(x, y);
    else if (id.equals("lightning")) lightning(x, y);
    else if (id.equals("hole")) startHole(x, y);
    else if (id.equals("gravgun")) grab(x, y);
    else if (id.equals("eraser")) erase(x, y);
    else if (id.equals("brush")) paint(x, y);
    else if (id.equals("furniture") || id.equals("explosives")) placeProp(x, y);
    else if (id.equals("people")) spawnPerson(x, y);
    else if (id.equals("building")) placeBuilding(x);
  }

  public void pointerMove(float x, float y, float dt) {
    if (dt > 0.0f) {
      pointerVelocityX = 0.65f * pointerVelocityX + 0.35f * (x - pointerX) / dt;
      pointerVelocityY = 0.65f * pointerVelocityY + 0.35f * (y - pointerY) / dt;
    }
    pointerX = x; pointerY = y;
    if (heldKind == 0 && heldIndex >= 0 && heldIndex < bodies.count && bodies.alive(heldIndex)) {
      bodies.x[heldIndex] = x; bodies.y[heldIndex] = y; bodies.vx[heldIndex] = bodies.vy[heldIndex] = 0.0f;
    } else if (heldKind == 1 && heldIndex >= 0 && debris.active[heldIndex]) {
      debris.x[heldIndex] = x; debris.y[heldIndex] = y; debris.vx[heldIndex] = debris.vy[heldIndex] = 0.0f;
    } else if (heldKind == 2 && heldIndex >= 0 && heldIndex < people.count) {
      people.moveHold(heldIndex, x, y);
    }
  }

  public void pointerUp() {
    if (heldKind == 0 && heldIndex >= 0 && heldIndex < bodies.count) {
      bodies.flags[heldIndex] &= ~BodyStore.F_HELD; bodies.wake(heldIndex);
      bodies.vx[heldIndex] = clampVelocity(pointerVelocityX); bodies.vy[heldIndex] = clampVelocity(pointerVelocityY);
    } else if (heldKind == 1 && heldIndex >= 0 && debris.active[heldIndex]) {
      debris.resting[heldIndex] = false; debris.vx[heldIndex] = clampVelocity(pointerVelocityX); debris.vy[heldIndex] = clampVelocity(pointerVelocityY);
    } else if (heldKind == 2 && heldIndex >= 0 && heldIndex < people.count) {
      people.release(heldIndex, clampVelocity(pointerVelocityX), clampVelocity(pointerVelocityY));
    }
    heldKind = -1; heldIndex = -1; held = false;
  }
  private float clampVelocity(float value) { return Math2.clamp(value, -1800.0f, 1800.0f); }

  public void update(float realDt) {
    if (realDt <= 0.0f) return;
    if (realDt > 0.08f) realDt = 0.08f;
    if (messageTime > 0.0f) messageTime -= realDt;
    if (paused) return;
    if (epicTimer > 0.0f) epicTimer -= realDt;
    float dt = slowMotion || epicSlow && epicTimer > 0.0f ? realDt * 0.25f : realDt;
    time += dt;
    updateAlarms(dt);
    if (held) updateContinuous(dt);
    effects.update(this, dt);
    processExplosiveBodies();
    fluids.update(this, dt, physicsMode);
    fire.update(grid, fluids, debris, dt);
    bodies.quality = physicsMode; debris.quality = physicsMode; people.physicsMode = physicsMode;
    debris.maxDust = !effectsEnabled ? 0 : physicsMode == StructuralSolver.ECONOMY ? 64 : physicsMode == StructuralSolver.NORMAL ? 160 : DebrisSystem.MAX_DUST;
    bodies.update(grid, fluids, metadata, debris, dt, gravity);
    people.bloodEnabled = bloodEnabled;
    people.update(grid, fire, fluids, metadata, dt, gravity, radiationSystem, time);
    radiationSystem.update(time);
    doseRate = radiationSystem.rate(grid, cameraX, cameraY + 12.0f, time);
    if (debris.update(grid, fluids, metadata, people, dt, gravity)) { structuralDirty = true; sound("debris", 0.2f); }
    services.update(this, dt);
    updateDisasters(dt);
    specialClock += dt;
    if (specialClock >= 0.25f) { updateSpecial(specialClock); specialClock = 0.0f; }
    lavaClock += dt;
    if (lavaClock >= 0.4f) { coolLava(); lavaClock = 0.0f; }
    structureClock += dt;
    float structureInterval = physicsMode == StructuralSolver.ECONOMY ? 0.48f : physicsMode == StructuralSolver.DETAILED ? 0.20f : 0.32f;
    if (structuralDirty && structureClock >= structureInterval) {
      structuralDirty = false; structureClock = 0.0f;
      int changed = structure.solve(grid, debris, gravity, showStress, physicsMode, metadata);
      if (changed > 0) {
        structuralDirty = true;
        bodies.wakeArea(grid, cameraX, cameraY, 420.0f / zoom);
        power.recompute(grid); sound("creak", 0.25f);
      }
    }
    if (shake > 0.0f) shake = Math.max(0.0f, shake - dt * 1.8f);
    if (radiation > 0.0f) radiation = Math.max(0.0f, radiation - dt * 0.002f);
  }

  private void updateContinuous(float dt) {
    continuousClock -= dt;
    if (continuousClock > 0.0f) return;
    String id = Tools.IDS[toolIndex];
    if (id.equals("hammer")) { hammer(pointerX, pointerY); continuousClock = 0.17f; }
    else if (id.equals("flame")) {
      float radius = 6.0f * (1.0f + 0.45f * toolValues[toolIndex]);
      fire.igniteArea(grid, bodies, pointerX, pointerY, radius);
      scarePeople(pointerX, pointerY, radius * 1.5f, 1.0f);
      spawnFireVisual(pointerX, pointerY, radius); sound("flame", 0.25f); continuousClock = 0.07f;
    } else if (id.equals("lava") || id.equals("water")) {
      int count = id.equals("lava") ? toolValues[toolIndex] : 2 + toolValues[toolIndex];
      for (int i = 0; i < count; i++) {
        int effect = effects.spawn(id.equals("lava") ? EffectSystem.LAVA : EffectSystem.WATER,
            pointerX + random.range(-4.0f, 4.0f), pointerY + random.range(-4.0f, 4.0f), 2.0f);
        if (effect >= 0) {
          effects.vx[effect] = random.range(-45.0f, 45.0f); effects.vy[effect] = random.range(-70.0f, 5.0f);
        }
      }
      if (id.equals("water")) { douse(pointerX, pointerY, 6.0f * (2.0f + 0.5f * toolValues[toolIndex])); sound("water", 0.3f); }
      continuousClock = 0.08f;
    } else if (id.equals("eraser")) { erase(pointerX, pointerY); continuousClock = 0.05f; }
    else if (id.equals("brush")) { paint(pointerX, pointerY); continuousClock = 0.05f; }
    else continuousClock = 0.1f;
  }

  private void hammer(float x, float y) {
    int level = toolValues[toolIndex];
    float radiusCells = 0.8f + 0.3f * level, damage = 60.0f + 26.0f * level;
    int first = damageCircle(x, y, radiusCells * 6.0f, damage, false);
    if (first >= 0 && CRACKABLE[first]) {
      int cracks = 1 + (level > 5 ? 1 : 0) + (level > 8 ? 1 : 0);
      propagateCracks(x, y, cracks, 2.0f + 0.6f * level, 0.12f + 0.015f * level, 1.0f);
    }
    debris.spawnDust(x, y, 0xb0a090, 3 + level / 2);
    shake = Math.max(shake, 0.05f + 0.012f * level);
    sound("hit", 0.5f + 0.05f * level);
  }

  /** Original hammer fracture propagation (rh/ah), including its 40% HP floor. */
  private void propagateCracks(float x, float y, int count, float length, float damage, float direction) {
    int cx = (int)Math.floor(x / 6.0f), cy = (int)Math.floor(y / 6.0f);
    if (!grid.inside(cx, cy)) return;
    int start = grid.index(cx, cy), m = grid.material(start);
    if (m == 0 || Materials.FIXED[m]) {
      start = -1;
      for (int ring = 1; ring <= 3 && start < 0; ring++)
        for (int yy = -ring; yy <= ring && start < 0; yy++)
          for (int xx = -ring; xx <= ring && start < 0; xx++) {
            int gx = cx + xx, gy = cy + yy;
            if (!grid.inside(gx, gy)) continue;
            int at = grid.index(gx, gy), material = grid.material(at);
            if (material != 0 && !Materials.FIXED[material] && material != 5 && (grid.flags(at) & CellGrid.FLAG_DETACHED) == 0) start = at;
          }
      if (start < 0) return;
    }
    for (int crack = 0; crack < count; crack++) {
      float angle = 6.283f * random.nextFloat();
      float dx = Math2.cos(angle), dy = Math2.sin(angle);
      float bias = dx * direction;
      int actualLength = Math.max(2, Math2.round(length * (0.55f + 0.45f * random.nextFloat()) * (1.0f + 0.5f * Math.max(0.0f, bias))));
      propagateCrack(start, dx, dy, actualLength, damage);
    }
  }

  private void propagateCrack(int start, float dx, float dy, int length, float damage) {
    int width = grid.width, startX = start % width, startY = start / width;
    float x = startX + 0.5f, y = startY + 0.5f;
    int previous = start, completed = 0, startMaterial = grid.material(start);
    boolean walk = startMaterial == 2 || startMaterial == 20 || startMaterial == 15 || startMaterial == 17;
    float norm = Math2.sqrt(dx * dx + dy * dy); if (norm == 0.0f) norm = 1.0f;
    dx /= norm; dy /= norm;
    for (int step = 0; step < 2 * length && completed < length; step++) {
      if (walk && random.chance(0.6f)) {
        if (random.nextFloat() < Math.abs(dx) / (Math.abs(dx) + Math.abs(dy) + 0.000001f)) x += sign(dx);
        else y += sign(dy);
      } else {
        float jitter = 0.8f * (random.nextFloat() - 0.5f), cs = Math2.cos(jitter), sn = Math2.sin(jitter);
        float nx = dx * cs - dy * sn; dy = dx * sn + dy * cs; dx = nx;
        x += dx; y += dy;
      }
      int gx = (int)Math.floor(x), gy = (int)Math.floor(y);
      if (!grid.inside(gx, gy)) break;
      int at = grid.index(gx, gy); if (at == previous) continue;
      previous = at;
      int material = grid.material(at);
      if (material == 0 || Materials.FIXED[material] || material == 5 || material == 12 || material == 7 || (grid.flags(at) & CellGrid.FLAG_DETACHED) != 0) break;
      float hp = Materials.HP[material], old = grid.health(at);
      float joint = (grid.flags(at) & CellGrid.FLAG_JOINT) != 0 && material != 4 ? 1.6f : 1.0f;
      float next = Math.max(hp * 0.4f, old - hp * damage * joint * (1.0f - (float)completed / (length + 2)));
      if (next < old) { grid.setHealth(at, next); completed++; }
    }
  }
  private float sign(float value) { return value < 0.0f ? -1.0f : value > 0.0f ? 1.0f : 0.0f; }

  private void dropBall(float x, float y) {
    int level = toolValues[toolIndex];
    int i = effects.spawn(EffectSystem.METEOR, x, Math.max(30.0f, y - 300.0f), 4.0f);
    if (i >= 0) {
      effects.vy[i] = 40.0f; effects.tx[i] = x; effects.ty[i] = y;
      effects.radius[i] = 18.0f + 5.0f * level; effects.power[i] = 260.0f + 60.0f * level;
      effects.flags[i] = 77;
    }
  }

  private void placeCharge(float x, float y, boolean planned) {
    int level = toolValues[toolIndex];
    int count = planned ? 8 : 1;
    for (int n = 0; n < count; n++) {
      float px = x, py = y;
      if (planned) {
        int ground = findGround((int)(x / 6.0f));
        px = x + (n - 3.5f) * 12.0f; py = (ground - 2 - (n % 3) * 8) * 6.0f;
      }
      int i = effects.spawn(EffectSystem.CHARGE, px, py, 9999.0f);
      if (i >= 0) {
        effects.radius[i] = planned ? 8.0f : 16.0f + 5.0f * level;
        effects.power[i] = planned ? 240.0f : 210.0f + 42.0f * level;
      }
    }
    setMessage(planned ? "Заряды установлены · # детонация" : "Заряд установлен · # детонация");
    sound("click", 0.5f);
  }

  private void dropBomb(float x, float y, boolean nuclear) {
    int level = toolValues[toolIndex];
    float targetY = y, kt = 0.0f, blastRadius = 0.0f;
    if (nuclear) {
      kt = NUKE_KT[level - 1]; blastRadius = 460.0f * Math2.cbrt(kt / 15.0f);
      float groundY = nuclearGroundBurst ? findGround((int)(x / 6.0f)) * 6.0f : firstSolidY(x);
      targetY = nuclearGroundBurst ? groundY - 1.0f : Math.max(-40.0f, Math.min(y, Math.min(1284.0f - 0.5f * blastRadius, groundY - 24.0f)));
    }
    float startY = Math.max(-250.0f, Math.min(targetY - 30.0f, y - (nuclear ? 500.0f : 260.0f)));
    if (nuclear) {
      float direction = x >= cameraX ? 1.0f : -1.0f;
      float startX = cameraX - direction * 220.0f / zoom;
      startY = Math.max(-160.0f, Math.min(targetY - 120.0f, y - 420.0f));
      float speed = 230.0f + 12.0f * level, e = kt / 15.0f;
      // The original maps are much wider than a 20-second flight at the
      // release target. Keep the bomber alive until the actual drop point;
      // otherwise distant nuclear clicks silently lose the bomb.
      float flightLife = Math.abs(x - startX) / speed + Math.abs(targetY - startY) / 68.0f + 8.0f;
      int plane = effects.spawn(EffectSystem.BOMBER, startX, startY, Math.max(20.0f, flightLife));
      if (plane < 0) return;
      effects.vx[plane] = direction * speed; effects.tx[plane] = x; effects.ty[plane] = targetY;
      effects.radius[plane] = blastRadius;
      effects.power[plane] = 900.0f + 120.0f * Math2.pow(Math.max(0.02f, e), 0.35f);
      effects.flags[plane] = 100 + level + (nuclearGroundBurst ? EffectSystem.NUKE_GROUND_BURST : 0);
      float warning = 4.5f + Math.abs(x - startX) / speed + Math.abs(targetY - startY) / 68.0f;
      people.airRaid(grid, metadata, x, targetY, warning);
      setMessage("Бомбардировщик на подлёте · " + powerText());
      sound("bomber", 0.65f); sound("siren", 0.45f);
      // One civil-defence warning for the actual nuclear sortie. Ordinary
      // bombs, charges and collapsing buildings must not raise it.
      incidentSirenClock = Math.max(incidentSirenClock, warning + 12.0f);
      return;
    }
    int i = effects.spawn(EffectSystem.BOMB, x, startY, 8.0f);
    if (i < 0) return;
    effects.vy[i] = 260.0f; effects.vx[i] = random.range(-8.0f, 8.0f); effects.ty[i] = targetY;
    effects.radius[i] = 28.0f + 8.0f * level; effects.power[i] = 360.0f + 52.0f * level;
    setMessage("Бомба сброшена");
  }

  private float firstSolidY(float x) {
    int gx = Math.max(0, Math.min(grid.width - 1, (int)(x / 6.0f)));
    for (int y = 0; y < CellGrid.HEIGHT; y++) if (grid.solid(gx, y)) return 6.0f * y;
    return 1554.0f;
  }

  private void fireRocket(float x, float y) {
    int level = toolValues[toolIndex];
    float sx = cameraX - 75.0f / zoom, sy = Math.min(y + 50.0f, cameraY + 45.0f / zoom);
    float dx = x - sx, dy = y - sy, length = Math2.sqrt(dx * dx + dy * dy);
    if (length < 1.0f) length = 1.0f;
    int i = effects.spawn(EffectSystem.ROCKET, sx, sy, 5.0f);
    if (i >= 0) {
      effects.vx[i] = dx / length * 520.0f; effects.vy[i] = dy / length * 520.0f;
      effects.radius[i] = 16.0f + 4.2f * level; effects.power[i] = 280.0f + 40.0f * level;
    }
    sound("rocket", 0.7f);
  }

  private void throwMolotov(float x, float y) {
    int i = effects.spawn(EffectSystem.MOLOTOV, x - 140.0f, y - 100.0f, 3.0f);
    if (i >= 0) {
      effects.vx[i] = 280.0f; effects.vy[i] = -30.0f; effects.radius[i] = 22.0f + 4.0f * toolValues[toolIndex];
    }
  }

  private void laser(float x, float y) {
    int thickness = 1 + toolValues[toolIndex] / 4;
    for (int yy = -30; yy <= 30; yy++) for (int xx = -thickness; xx <= thickness; xx++) {
      int gx = (int)(x / 6.0f) + xx, gy = (int)(y / 6.0f) + yy;
      if (grid.inside(gx, gy)) grid.damage(grid.index(gx, gy), 10000.0f);
    }
    int i = effects.spawn(EffectSystem.LIGHTNING, x, y, 0.4f); if (i >= 0) effects.radius[i] = 60.0f;
    bodies.wakeArea(grid, x, y, 40.0f); structuralDirty = true; sound("laser", 0.8f);
  }

  private void startQuake() {
    quakeStrength = 0.15f + 0.085f * toolValues[toolIndex]; quakeTime = Math.min(12.0f, quakeTime + 6.0f);
    setMessage("Землетрясение M " + (4.0f + 0.5f * toolValues[toolIndex])); sound("rumble", 0.8f);
  }
  private void startTornado(float x, float y) {
    tornadoX = x; tornadoY = y; tornadoStrength = toolValues[toolIndex]; tornadoTime = 13.0f;
    setMessage("Торнадо EF" + Math2.round((toolValues[toolIndex] - 1) * 5.0f / 9.0f));
  }
  private void startTsunami(float x) {
    tsunamiDirection = x < cameraX ? 1.0f : -1.0f; tsunamiHeight = 60.0f + 45.0f * toolValues[toolIndex]; tsunamiTime = 12.0f;
    setMessage("Предупреждение о цунами");
  }
  private void dropMeteor(float x, float y) {
    int level = toolValues[toolIndex];
    int i = effects.spawn(EffectSystem.METEOR, x - 360.0f, Math.max(10.0f, y - 900.0f), 5.0f);
    if (i >= 0) {
      float dx = 360.0f, dy = y - effects.y[i], len = Math2.sqrt(dx * dx + dy * dy);
      effects.vx[i] = dx / len * 720.0f; effects.vy[i] = dy / len * 720.0f;
      effects.radius[i] = 34.0f + 12.0f * level; effects.power[i] = 500.0f + 160.0f * level;
    }
  }
  private void lightning(float x, float y) {
    int gx = (int)(x / 6.0f), hitY = 0;
    for (int yy = 0; yy < CellGrid.HEIGHT; yy++) if (grid.materialAt(gx, yy) != 0) { hitY = yy; break; }
    float hy = hitY * 6.0f + 3.0f;
    int i = effects.spawn(EffectSystem.LIGHTNING, gx * 6.0f + 3.0f, hy, 0.3f); if (i >= 0) effects.radius[i] = hy;
    explode(gx * 6.0f + 3.0f, hy, 12.0f + 2.0f * toolValues[toolIndex], 160.0f + 30.0f * toolValues[toolIndex], 0.6f);
  }
  private void startHole(float x, float y) {
    holeX = x; holeY = y; holeRadius = 150.0f + 26.0f * toolValues[toolIndex]; holeTime = 9.0f;
    setMessage("Чёрная дыра"); sound("hole", 0.8f);
  }
  private void startVolcano() {
    if (mapIndex != Maps.find("volcano")) { loadMap(Maps.find("volcano")); setMessage("Коснитесь ещё раз — извержение"); return; }
    eruptionStrength = 0.5f + 0.12f * toolValues[toolIndex]; eruptionTime = 15.0f;
    setMessage("Извержение вулкана");
  }

  private void grab(float x, float y) {
    float range = 10.0f + 8.0f * toolValues[toolIndex];
    int body = bodies.nearest(x, y, range), chunk = debris.nearest(x, y, range), person = people.nearest(x, y, range);
    float best = range * range;
    if (body >= 0) { float dx = bodies.x[body] - x, dy = bodies.y[body] - y; float d = dx * dx + dy * dy; if (d < best) { best = d; heldKind = 0; heldIndex = body; } }
    if (chunk >= 0) { float dx = debris.x[chunk] - x, dy = debris.y[chunk] - y; float d = dx * dx + dy * dy; if (d < best) { best = d; heldKind = 1; heldIndex = chunk; } }
    if (person >= 0) { float dx = people.x[person] - x, dy = people.y[person] - y; float d = dx * dx + dy * dy; if (d < best) { heldKind = 2; heldIndex = person; } }
    if (heldKind == 0) { bodies.wake(heldIndex); bodies.flags[heldIndex] |= BodyStore.F_HELD; }
    if (heldKind == 1) debris.resting[heldIndex] = false;
    if (heldKind == 2) people.hold(heldIndex, x, y);
    if (heldKind >= 0) setMessage("Захвачено");
  }

  private void erase(float x, float y) {
    float radius = 0.8f * toolValues[toolIndex] * 6.0f, r2 = radius * radius;
    int cx = (int)(x / 6.0f), cy = (int)(y / 6.0f), r = (int)Math.ceil(radius / 6.0f);
    for (int yy = -r; yy <= r; yy++) for (int xx = -r; xx <= r; xx++) {
      if (xx * xx + yy * yy > r * r) continue;
      int gx = cx + xx, gy = cy + yy;
      if (grid.inside(gx, gy)) {
        int at = grid.index(gx, gy); if (grid.material(at) != 9) grid.clear(at);
      }
    }
    for (int i = 0; i < bodies.count; i++) if (bodies.alive(i)) {
      float dx = bodies.x[i] - x, dy = bodies.y[i] - y; if (dx * dx + dy * dy < r2) bodies.remove(i);
    }
    for (int i = 0; i < debris.chunkCount; i++) if (debris.active[i]) {
      float dx = debris.x[i] - x, dy = debris.y[i] - y; if (dx * dx + dy * dy < r2) debris.active[i] = false;
    }
    structuralDirty = true;
  }

  private void paint(float x, float y) {
    int level = toolValues[toolIndex], mat = BUILD_MATERIAL[buildMaterial];
    float radiusCells = 0.75f * (level - 1) + 0.3f;
    int cx = (int)(x / 6.0f), cy = (int)(y / 6.0f), r = (int)Math.ceil(radiusCells);
    for (int yy = -r; yy <= r; yy++) for (int xx = -r; xx <= r; xx++) {
      if (xx * xx + yy * yy > radiusCells * radiusCells + 0.5f) continue;
      int gx = cx + xx, gy = cy + yy;
      if (grid.inside(gx, gy)) {
        int at = grid.index(gx, gy);
        if (grid.material(at) == 0) grid.set(at, mat, BUILD_TINT[buildMaterial], BUILD_FLAGS[buildMaterial]);
      }
    }
    structuralDirty = true;
  }

  private void placeProp(float x, float y) {
    int type = propSelection;
    if (type < 0) return;
    int i = bodies.add(1, type, x, y, Props.WIDTH[type], Props.HEIGHT[type], Props.MASS[type], Props.HP[type]);
    if (i >= 0) { bodies.variant[i] = (byte)random.nextInt(Math.max(1, Props.VARIANTS[type])); sound("click", 0.4f); }
  }

  private void spawnPerson(float x, float y) {
    int person = people.spawnAt(grid, x, y);
    setMessage(person >= 0 ? "Житель добавлен" : "Здесь тесно — человек не поместится");
    if (person >= 0) sound("click", 0.35f);
  }

  private void placeBuilding(float x) {
    int ground = findGround((int)(x / 6.0f));
    int x0 = buildingGenerator.build(grid, bodies, buildingSelection, (int)(x / 6.0f), ground, random);
    cameraX = (x0 + Buildings.MIN_WIDTH[buildingSelection] * 0.5f) * 6.0f;
    structuralDirty = true; setMessage("Построено: " + Buildings.NAMES[buildingSelection]);
  }

  public int findGround(int cellX) {
    cellX = Math.max(0, Math.min(grid.width - 1, cellX));
    for (int y = 180; y < CellGrid.HEIGHT; y++) if (Materials.FIXED[grid.materialAt(cellX, y)]) return y;
    return 214;
  }

  private int damageCircle(float x, float y, float radius, float damage, boolean blast) {
    int first = -1;
    float radiusCells = radius / 6.0f;
    int cx = (int)(x / 6.0f), cy = (int)(y / 6.0f), r = (int)Math.ceil(radiusCells);
    int minX = Math.max(0, cx - r), maxX = Math.min(grid.width - 1, cx + r);
    int minY = Math.max(0, cy - r), maxY = Math.min(CellGrid.HEIGHT - 1, cy + r);
    for (int gy = minY; gy <= maxY; gy++) for (int gx = minX; gx <= maxX; gx++) {
      int xx = gx - cx, yy = gy - cy;
      float distance = Math2.sqrt(xx * xx + yy * yy);
      if (distance > radiusCells + 0.5f) continue;
      int at = grid.index(gx, gy), m = grid.material(at);
      if (m == 0) continue;
      if (first < 0) first = m;
      float amount = damage * (1.0f - distance / (radiusCells + 1.5f));
      if (blast) amount *= BLAST_MULTIPLIER[m];
      if (grid.damage(at, amount) && random.chance(0.12f)) debris.spawnDust(gx * 6.0f + 3.0f, gy * 6.0f + 3.0f, Materials.DUST[m], 1);
    }
    bodies.wakeArea(grid, x, y, radius * 1.5f);
    if (blast) grid.compactRows();
    structuralDirty = true;
    return first;
  }

  public void explode(float x, float y, float radius, float power, float fireAmount) {
    if (epicSlow && (radius > 180.0f || power > 900.0f)) epicTimer = Math.max(epicTimer, 1.1f);
    damageCircle(x, y, radius, power, true);
    if (metadata != null && metadata.npp && !reactorScram && radius >= 20.0f) {
      float dx = x - reactorX(), dy = y - reactorY();
      float tripRadius = radius + 90.0f;
      if (dx * dx + dy * dy < tripRadius * tripRadius) scramReactor("внешний удар");
    }
    bodies.damageArea(x, y, radius * 1.25f, power * 0.42f, power * 1.4f);
    debris.applyExplosion(x, y, radius * 1.4f, power * 1.6f);
    scarePeople(x, y, radius * 1.7f, power * 0.13f);
    this.power.damageArea(grid, x, y, radius);
    if (fireAmount > 0.0f) {
      fire.igniteArea(grid, bodies, x, y, radius * fireAmount);
      people.igniteArea(x, y, radius * fireAmount);
    }
    effects.spawnBlast(x, y, radius, radius > 250.0f ? 2.2f : 0.65f);
    alarmFlash = Math.max(alarmFlash, radius > 250.0f ? 0.42f : 0.18f);
    int smoke = effects.spawn(EffectSystem.SMOKE, x, y, 1.5f);
    if (smoke >= 0) { effects.vy[smoke] = -30.0f; effects.radius[smoke] = radius * 0.5f; }
    shake = Math.max(shake, Math.min(1.2f, radius / 180.0f));
    if (radius > 250.0f) radiation = Math.max(radiation, radius / 800.0f);
    structuralDirty = true;
    // The recovered blast incident ignores sub-18 px pops; larger blasts can
    // still bring police, but never the civil-defence siren by themselves.
    if (radius >= 18.0f) services.notifyIncident(ServiceSystem.POLICE, x, y);
    sound(radius > 250.0f ? "nuke" : "boom", Math.min(1.0f, 0.3f + radius / 300.0f));
  }

  public void nuclearExplode(float x, float y, float kt, float radius, float power, float fireAmount) {
    nuclearExplode(x, y, kt, radius, power, fireAmount, nuclearGroundBurst);
  }
  public void nuclearExplode(float x, float y, float kt, float radius, float power, float fireAmount, boolean groundBurst) {
    explode(x, y, radius, power, fireAmount);
    if (incidentSirenClock <= 0.0f) {
      // A nuclear blast without a preceding bomber still gets one warning;
      // the sortie path above already supplied it and must not double-play it.
      sound("siren", 0.55f);
      incidentSirenClock = 20.0f;
    }
    float fireball = Math2.clamp(92.0f * Math2.pow(kt / 15.0f, 0.4f), 7.0f, 1100.0f);
    boolean touch = groundBurst || firstSolidY(x) - y < 0.9f * fireball;
    radiationSystem.addNuclear(x, time, kt, touch, radius);
    people.finishRaid(3.0f);
    doseRate = radiationSystem.rate(grid, cameraX, cameraY + 12.0f, time);
    int flash = effects.spawn(EffectSystem.FLASH, x, y, Math2.clamp(0.32f * Math2.pow(kt / 15.0f, 0.2f), 0.08f, 1.5f));
    if (flash >= 0) effects.radius[flash] = fireball;
  }

  private void processExplosiveBodies() {
    int chains = 0;
    for (int i = 0; i < bodies.count && chains < 12; i++) {
      if (bodies.alive(i) && bodies.hp[i] <= 0.0f) bodies.kill(i);
      if (!bodies.alive(i) && bodies.type[i] >= 0 && Props.EXPLOSION_RADIUS[bodies.type[i]] > 0.0f && bodies.fuse[i] > -900.0f) {
        bodies.fuse[i] = -999.0f; chains++;
        explode(bodies.x[i], bodies.y[i], Props.EXPLOSION_RADIUS[bodies.type[i]], Props.EXPLOSION_POWER[bodies.type[i]],
            Math.max(0.15f, Props.EXPLOSION_FIRE[bodies.type[i]] * 0.4f));
      }
    }
  }

  public void scarePeople(float x, float y, float radius, float damage) {
    people.scare(x, y, radius, damage);
    if (radius > 45.0f) sound(damage > 12.0f ? "scream" : "groan", Math.min(0.5f, radius / 500.0f));
  }
  public void douse(float x, float y, float radius) {
    fire.douse(grid, x, y, radius); people.douse(x, y, radius);
    for (int i = 0; i < bodies.count; i++) if (bodies.alive(i)) {
      float dx = bodies.x[i] - x, dy = bodies.y[i] - y;
      if (dx * dx + dy * dy < radius * radius) bodies.flags[i] &= ~BodyStore.F_BURNING;
    }
  }
  public void spawnFireVisual(float x, float y, float radius) {
    for (int n = 0; n < 4; n++) {
      int i = effects.spawn(EffectSystem.FIRE, x + random.range(-radius * 0.4f, radius * 0.4f), y + random.range(-radius * 0.3f, radius * 0.3f), random.range(0.3f, 0.7f));
      if (i >= 0) { effects.vx[i] = random.range(-25.0f, 25.0f); effects.vy[i] = random.range(-70.0f, -25.0f); effects.radius[i] = random.range(3.0f, 7.0f); }
    }
  }

  private void updateDisasters(float dt) {
    if (quakeTime > 0.0f) {
      quakeTime -= dt; shake = Math.max(shake, quakeStrength * 0.45f);
      if (random.chance(dt * quakeStrength * 8.0f)) {
        int gx = random.nextInt(grid.width), ground = findGround(gx);
        float x = gx * 6.0f, y = (ground - random.nextInt(30)) * 6.0f;
        damageCircle(x, y, 12.0f + quakeStrength * 15.0f, 10.0f + quakeStrength * 24.0f, false);
        bodies.wakeArea(grid, x, y, 180.0f);
      }
    }
    if (tornadoTime > 0.0f) {
      tornadoTime -= dt; tornadoX += Math2.sin(time * 0.7f) * 18.0f * dt;
      float width = 24.0f + tornadoStrength * 7.0f;
      for (int n = 0; n < 3 + (int)tornadoStrength; n++) {
        float x = tornadoX + random.range(-width, width), y = tornadoY - random.nextInt(55) * 6.0f;
        damageCircle(x, y, 8.0f + tornadoStrength, 4.0f + tornadoStrength * 2.0f, false);
      }
      for (int i = 0; i < bodies.count; i++) if (bodies.alive(i) && Math.abs(bodies.x[i] - tornadoX) < width * 2.0f) {
        bodies.wake(i); bodies.vx[i] += (tornadoX - bodies.x[i]) * dt * 2.0f; bodies.vy[i] -= (60.0f + tornadoStrength * 15.0f) * dt;
      }
      scarePeople(tornadoX, tornadoY - 100.0f, width * 2.0f, 2.0f);
    }
    if (tsunamiTime > 0.0f) {
      tsunamiTime -= dt;
      float progress = (12.0f - tsunamiTime) / 12.0f;
      float front = tsunamiDirection > 0.0f ? progress * grid.width * 6.0f : grid.width * 6.0f * (1.0f - progress);
      float baseY = metadata.river ? metadata.riverWaterY : 1284.0f;
      for (int n = 0; n < 12; n++) damageCircle(front + random.range(-12.0f, 12.0f), baseY - random.nextInt((int)(tsunamiHeight / 6.0f) + 1) * 6.0f,
          12.0f, 6.0f + tsunamiHeight * 0.08f, false);
      for (int i = 0; i < bodies.count; i++) if (bodies.alive(i) && Math.abs(bodies.x[i] - front) < 45.0f && bodies.y[i] > baseY - tsunamiHeight) {
        bodies.wake(i); bodies.vx[i] += tsunamiDirection * 220.0f * dt; bodies.vy[i] -= 60.0f * dt;
      }
      douse(front, baseY - tsunamiHeight * 0.5f, tsunamiHeight);
    }
    if (holeTime > 0.0f) {
      holeTime -= dt;
      float eat = (9.0f + 1.3f * toolValues[Tools.find("hole")]) * (1.0f + (9.0f - holeTime) * 0.12f);
      if (random.chance(dt * 12.0f)) {
        erase(holeX + random.range(-eat, eat), holeY + random.range(-eat, eat));
        for (int p = 0; p < people.count; p++) if (people.state[p] < PeopleStore.DEAD) {
          float dx = holeX - people.centerX(p), dy = holeY - people.centerY(p), distance = Math2.sqrt(dx * dx + dy * dy) + 1.0f;
          if (distance < holeRadius) {
            people.hurt(p, dt * 90.0f * (1.0f - distance / holeRadius), dx / distance * 80.0f, dy / distance * 80.0f, PeopleStore.CAUSE_HOLE);
            if (distance < eat) people.remove(p);
          }
        }
      }
      for (int i = 0; i < bodies.count; i++) if (bodies.alive(i)) {
        float dx = holeX - bodies.x[i], dy = holeY - bodies.y[i], d2 = dx * dx + dy * dy;
        if (d2 < holeRadius * holeRadius) {
          bodies.wake(i); float d = Math2.sqrt(d2) + 2.0f;
          bodies.vx[i] += dx / d * 240.0f * dt; bodies.vy[i] += dy / d * 240.0f * dt;
          if (d < eat) bodies.remove(i);
        }
      }
      for (int i = 0; i < debris.chunkCount; i++) if (debris.active[i]) {
        float dx = holeX - debris.x[i], dy = holeY - debris.y[i], d = Math2.sqrt(dx * dx + dy * dy) + 2.0f;
        if (d < holeRadius) { debris.resting[i] = false; debris.vx[i] += dx / d * 280.0f * dt; debris.vy[i] += dy / d * 280.0f * dt; if (d < eat) debris.active[i] = false; }
      }
    }
    if (eruptionTime > 0.0f) {
      eruptionTime -= dt;
      if (random.chance(dt * eruptionStrength * 5.0f)) {
        float x = metadata.volcanoX * 6.0f, y = metadata.volcanoCraterY * 6.0f;
        int i = effects.spawn(EffectSystem.VOLCANIC, x + random.range(-20.0f, 20.0f), y, 6.0f);
        if (i >= 0) {
          effects.vx[i] = random.range(-170.0f, 170.0f); effects.vy[i] = random.range(-330.0f, -180.0f);
          effects.radius[i] = 25.0f + 20.0f * eruptionStrength; effects.power[i] = 300.0f + 200.0f * eruptionStrength;
        }
      }
    }
  }

  private void updateAlarms(float dt) {
    if (incidentSirenClock > 0.0f) incidentSirenClock -= dt;
    if (alarmFlash > 0.0f) alarmFlash = Math.max(0.0f, alarmFlash - dt);
    if (reactorAlarmLevel <= 0) return;
    reactorSirenClock -= dt;
    if (reactorSirenClock <= 0.0f) {
      sound("reactorSiren", reactorAlarmLevel >= 2 ? 0.75f : 0.55f);
      reactorSirenClock = reactorAlarmLevel >= 2 ? 12.0f : 18.0f;
    }
    if (reactorAlarmLevel >= 2) {
      reactorEvacuationClock -= dt;
      if (reactorEvacuationClock <= 0.0f) {
        people.evacuateFrom(reactorX(), reactorY(), grid.width * 6.0f + 600.0f);
        reactorEvacuationClock = 2.5f;
      }
    }
  }

  private void startReactorAlarm(int level, String text) {
    if (level > reactorAlarmLevel) {
      reactorAlarmLevel = level;
      reactorRedLight = true;
      reactorSirenClock = level >= 2 ? 12.0f : 18.0f;
      reactorEvacuationClock = 0.0f;
      sound("reactorSiren", level >= 2 ? 0.75f : 0.55f);
      // AM(1)/AM(2) also raise the civil-defence siren, distinct from the
      // plant's own sirenC loop.
      sound("siren", level >= 2 ? 0.55f : 0.4f);
    }
    setMessage(text);
  }

  private void scramReactor(String cause) {
    if (reactorScram) return;
    reactorScram = true;
    startReactorAlarm(1, "АЭС: SCRAM · " + cause);
    float x = reactorX(), y = reactorY();
    // Civil level 1 in the recovered game: shelter before a release is known.
    people.airRaid(grid, metadata, x, y, 12.0f);
  }

  private void releaseReactor(float x, float y) {
    if (reactorRelease) return;
    reactorRelease = true; reactorScram = true;
    startReactorAlarm(2, "АЭС: ВЫБРОС · эвакуация!");
    // Civil level 2: residents leave the station/plume rather than remain in
    // shelter.  Repeat from updateAlarms so newly active residents also flee.
    people.evacuateFrom(x, y, grid.width * 6.0f + 600.0f);
    services.notifyIncident(ServiceSystem.POLICE, x, y);
    services.notifyIncident(ServiceSystem.MED, x, y);
  }

  public float reactorX() { return propCenterX("gcn"); }
  public float reactorY() { return propCenterY("gcn"); }

  private void updateSpecial(float dt) {
    if (metadata.npp) {
      boolean coolant = aliveProp("gcn") && aliveProp("condenser");
      boolean diesel = aliveProp("diesel"), steam = aliveProp("steamgen");
      if (!reactorScram) {
        // The recovered plant trips on the turbine/transformer chain as well
        // as on the primary cooling pump and steam generator.
        if (!aliveProp("turbine")) scramReactor("турбина повреждена");
        else if (!aliveProp("bigtrafo")) scramReactor("блочный трансформатор повреждён");
        else if (!coolant || !steam) scramReactor(!coolant ? "потеря охлаждения" : "парогенератор повреждён");
      }
      if (coolant && steam) {
        reactorBackupTime = Math.max(0.0f, reactorBackupTime - dt * 2.0f);
        // Recovered initial reactor state is 320 C; SCRAM cooling brings it
        // back down instead of silently treating a damaged plant as normal.
        reactorTemperature += ((reactorScram ? 85.0f : 320.0f) - reactorTemperature) * dt * 0.25f;
      } else if (diesel && reactorBackupTime < 18.0f) {
        reactorBackupTime += dt;
        reactorTemperature += (120.0f - reactorTemperature) * dt * 0.08f;
      } else {
        reactorBackupTime += dt;
        reactorTemperature += dt * 18.0f;
      }
      if (reactorTemperature > 250.0f && (!coolant || !steam || reactorScram && reactorTemperature > 400.0f))
        reactorHydrogen += dt * (reactorTemperature - 220.0f) * 0.015f;
      else reactorHydrogen = Math.max(0.0f, reactorHydrogen - dt * 2.0f);
      if (!reactorRelease && (reactorTemperature > 850.0f || reactorHydrogen > 100.0f)) {
        float x = reactorX(), y = reactorY();
        explode(x, y, 120.0f, 650.0f, 0.5f);
        radiation = Math.max(radiation, 1.5f);
        radiationSystem.addNppRelease(x, time, 1.0f, random.chance(0.7f) ? -1 : 1);
        reactorTemperature = 400.0f; reactorHydrogen = 0.0f;
        releaseReactor(x, y);
      }
    }
    if (metadata.base) {
      boolean defence = aliveProp("sam") || aliveProp("aagun");
      if (defence) for (int i = 0; i < EffectSystem.MAX; i++) if ((effects.type[i] == EffectSystem.BOMB || effects.type[i] == EffectSystem.BOMBER) && effects.y[i] < effects.ty[i] - 50.0f) {
        float sx = propCenterX("sam"), sy = propCenterY("sam") - 20.0f;
        int rocket = effects.spawn(EffectSystem.ROCKET, sx, sy, 1.0f);
        if (rocket >= 0) {
          float dx = effects.x[i] - sx, dy = effects.y[i] - sy, len = Math2.sqrt(dx * dx + dy * dy) + 1.0f;
          effects.vx[rocket] = dx / len * 700.0f; effects.vy[rocket] = dy / len * 700.0f; effects.radius[rocket] = 20.0f; effects.power[rocket] = 180.0f;
        }
        explode(effects.x[i], effects.y[i], 24.0f, 140.0f, 0.0f); effects.type[i] = EffectSystem.NONE;
        people.finishRaid(2.0f); setMessage("ПВО перехватило бомбу"); break;
      }
    }
  }

  private boolean aliveProp(String id) {
    int type = Props.find(id); if (type < 0) return false;
    for (int i = 0; i < bodies.count; i++) if (bodies.alive(i) && bodies.type[i] == type) return true;
    return false;
  }
  private float propCenterX(String id) {
    int type = Props.find(id); float sum = 0.0f; int count = 0;
    for (int i = 0; i < bodies.count; i++) if (bodies.type[i] == type) { sum += bodies.x[i]; count++; }
    return count > 0 ? sum / count : cameraX;
  }
  private float propCenterY(String id) {
    int type = Props.find(id); float sum = 0.0f; int count = 0;
    for (int i = 0; i < bodies.count; i++) if (bodies.type[i] == type) { sum += bodies.y[i]; count++; }
    return count > 0 ? sum / count : cameraY;
  }

  private void coolLava() {
    if (grid.lavaCells == 0) return;
    // Cooling is evaluated only in the active camera region, leaving distant simulation dormant.
    int center = (int)(cameraX / 6.0f), range = (int)(90.0f / zoom) + 20;
    int min = Math.max(0, center - range), max = Math.min(grid.width - 1, center + range);
    for (int y = 0; y < CellGrid.HEIGHT; y++) for (int x = min; x <= max; x++) {
      int at = grid.index(x, y);
      if (grid.material(at) == 12 && random.chance(0.08f)) { grid.set(at, 13, 0, grid.flags(at)); structuralDirty = true; }
    }
  }

  public void cycleOption(int delta) {
    String id = Tools.IDS[toolIndex];
    if (id.equals("nuke")) { nuclearGroundBurst = !nuclearGroundBurst; setMessage(nuclearGroundBurst ? "Наземный взрыв" : "Воздушный взрыв"); }
    else if (id.equals("brush")) { buildMaterial += delta; if (buildMaterial < 0) buildMaterial = BUILD_MATERIAL.length - 1; if (buildMaterial >= BUILD_MATERIAL.length) buildMaterial = 0; setMessage(Materials.NAMES[BUILD_MATERIAL[buildMaterial]]); }
    else if (id.equals("building")) { buildingSelection += delta; if (buildingSelection < 0) buildingSelection = Buildings.COUNT - 1; if (buildingSelection >= Buildings.COUNT) buildingSelection = 0; setMessage(Buildings.NAMES[buildingSelection]); }
    else if (id.equals("furniture") || id.equals("explosives")) {
      boolean explosive = id.equals("explosives");
      for (int n = 0; n < Props.COUNT; n++) {
        propSelection += delta; if (propSelection < 0) propSelection = Props.COUNT - 1; if (propSelection >= Props.COUNT) propSelection = 0;
        boolean match = explosive ? Props.EXPLOSION_RADIUS[propSelection] > 0.0f : Props.CATEGORIES[propSelection].equals("furniture");
        if (match) break;
      }
      setMessage(Props.NAMES[propSelection]);
    }
  }

  public void clearDebris() {
    if (heldKind >= 0) pointerUp();
    debris.reset();
    for (int i = 0; i < people.count; i++) if (people.state[i] == PeopleStore.DEAD) people.remove(i);
    setMessage("Обломки и тела убраны");
  }

  public int destructionPercent() {
    int total = Math.max(1, grid.initialUserCells);
    return Math.min(100, (int)(grid.destroyed * 100.0f / total));
  }
  public String statusText() {
    boolean npp = metadata != null && metadata.npp;
    if (npp) {
      int temperature = Math2.round(reactorTemperature), hydrogen = Math2.round(reactorHydrogen);
      if (cachedStatusText != null && cachedStatusNpp && cachedStatusMap == mapIndex
          && cachedStatusAlarm == reactorAlarmLevel && cachedStatusTemperature == temperature
          && cachedStatusHydrogen == hydrogen) return cachedStatusText;
      cachedStatusNpp = true; cachedStatusMap = mapIndex; cachedStatusAlarm = reactorAlarmLevel;
      cachedStatusTemperature = temperature; cachedStatusHydrogen = hydrogen;
      if (reactorAlarmLevel >= 2) cachedStatusText = "АЭС ВЫБРОС · бегите!";
      else if (reactorAlarmLevel == 1) cachedStatusText = "АЭС АВАРИЯ · укрытие";
      else cachedStatusText = "АЭС " + temperature + "°C · H " + hydrogen + "%";
      return cachedStatusText;
    }
    int injured = people.injuredCount(), sick = people.sickCount(), living = people.livingCount();
    if (cachedStatusText != null && !cachedStatusNpp && cachedStatusMap == mapIndex
        && cachedStatusInjured == injured && cachedStatusSick == sick && cachedStatusLiving == living) return cachedStatusText;
    cachedStatusNpp = false; cachedStatusMap = mapIndex; cachedStatusAlarm = 0;
    cachedStatusInjured = injured; cachedStatusSick = sick; cachedStatusLiving = living;
    cachedStatusText = "ранено " + injured + (sick > 0 ? " · ☢ " + sick : "") + " · живы " + living;
    return cachedStatusText;
  }
  public boolean disasterActive(int type) {
    if (type == 0) return quakeTime > 0.0f;
    if (type == 1) return tornadoTime > 0.0f;
    if (type == 2) return tsunamiTime > 0.0f;
    if (type == 3) return holeTime > 0.0f;
    return eruptionTime > 0.0f;
  }
  public float disasterX(int type) {
    if (type == 1) return tornadoX;
    if (type == 2) { float p = (12.0f - tsunamiTime) / 12.0f; return tsunamiDirection > 0.0f ? p * grid.width * 6.0f : grid.width * 6.0f * (1.0f - p); }
    if (type == 3) return holeX;
    return metadata.volcanoX * 6.0f;
  }
  public float disasterY(int type) { return type == 1 ? tornadoY : type == 3 ? holeY : metadata.river ? metadata.riverWaterY : 1284.0f; }
  public float disasterStrength(int type) { return type == 0 ? quakeStrength : type == 1 ? tornadoStrength : type == 2 ? tsunamiHeight : holeRadius; }
  public float shakeX() { return shake > 0.0f ? Math2.sin(time * 71.0f) * shake * 4.0f : 0.0f; }
  public float shakeY() { return shake > 0.0f ? Math2.cos(time * 83.0f) * shake * 3.0f : 0.0f; }

  public int simulationMemoryBytes() {
    return grid.memoryBytes() + structure.memoryBytes() + fluids.memoryBytes() + services.memoryBytes()
        + bodies.memoryBytes() + people.memoryBytes() + debris.memoryBytes() + effects.memoryBytes()
        + fire.memoryBytes() + radiationSystem.memoryBytes() + power.memoryBytes();
  }

  public void announce(String text) { setMessage(text); }
  private void setMessage(String text) { message = text; messageTime = 3.0f; if (listener != null) listener.message(text); }
  public void sound(String id, float volume) { if (listener != null) listener.sound(id, volume); }
}
