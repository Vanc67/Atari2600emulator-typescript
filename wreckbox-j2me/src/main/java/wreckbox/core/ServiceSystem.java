package wreckbox.core;

/** Automatic fire, ambulance and police response with bounded vehicle/crew state. */
public final class ServiceSystem {
  public static final int NONE = 0, FIRE = 1, MED = 2, POLICE = 3;
  public static final int MAX_UNITS = 6;
  private static final int INBOUND = 1, WORK = 2, LEAVING = 3;

  public final byte[] type = new byte[MAX_UNITS], state = new byte[MAX_UNITS];
  public final float[] x = new float[MAX_UNITS], y = new float[MAX_UNITS];
  public final float[] targetX = new float[MAX_UNITS], targetY = new float[MAX_UNITS];
  public final float[] timer = new float[MAX_UNITS], work = new float[MAX_UNITS];
  public final int[] patient = new int[MAX_UNITS];
  private final boolean[] pending = new boolean[4], sirenPlayed = new boolean[MAX_UNITS];
  private final float[] pendingX = new float[4], pendingY = new float[4];
  private final float[] cooldown = new float[4];
  private float scanClock, actionClock;
  public int saved, firesHandled;

  public void reset() {
    for (int i = 0; i < MAX_UNITS; i++) { type[i] = NONE; state[i] = 0; patient[i] = -1; sirenPlayed[i] = false; }
    for (int i = 0; i < 4; i++) { pending[i] = false; cooldown[i] = 0.0f; }
    scanClock = actionClock = 0.0f; saved = firesHandled = 0;
  }

  public void notifyIncident(int kind, float px, float py) {
    if (kind < FIRE || kind > POLICE) return;
    pending[kind] = true; pendingX[kind] = px; pendingY[kind] = py;
  }

  public int activeCount(int kind) {
    int result = 0;
    for (int i = 0; i < MAX_UNITS; i++) if (type[i] == kind && state[i] != 0) result++;
    return result;
  }

  public void update(Game game, float dt) {
    if (!game.servicesEnabled) return;
    for (int i = 1; i <= POLICE; i++) cooldown[i] = Math.max(0.0f, cooldown[i] - dt);
    scanClock += dt; actionClock += dt;
    boolean scan = scanClock >= 0.8f, action = actionClock >= 0.25f;
    if (scan) { scanClock = 0.0f; scanIncidents(game); }
    if (action) actionClock = 0.0f;
    for (int kind = FIRE; kind <= POLICE; kind++) {
      if (pending[kind] && cooldown[kind] <= 0.0f && activeCount(kind) == 0) dispatch(game, kind);
    }
    for (int i = 0; i < MAX_UNITS; i++) {
      int kind = type[i];
      if (kind == NONE || state[i] == 0) continue;
      timer[i] -= dt;
      if (state[i] == INBOUND) {
        float direction = targetX[i] > x[i] ? 1.0f : -1.0f;
        x[i] += direction * 155.0f * dt;
        // Recovered vehicles use a short local siren while approaching, not
        // a civil alarm and not a second sound on every arrival tick.
        if (!sirenPlayed[i] && timer[i] <= 2.4f) {
          sirenPlayed[i] = true;
          game.sound(kind == MED ? "medSiren" : "serviceSiren", 0.35f);
        }
        if (timer[i] <= 0.0f || Math.abs(targetX[i] - x[i]) < 12.0f) {
          x[i] = targetX[i]; state[i] = WORK; work[i] = 0.0f; timer[i] = 0.0f;
        }
      } else if (state[i] == WORK) {
        work[i] += dt;
        if (action) work(game, i);
        if (work[i] > (kind == POLICE ? 9.0f : 20.0f)) leave(game, i);
      } else if (state[i] == LEAVING) {
        x[i] += (targetX[i] < game.grid.width * 3.0f ? -155.0f : 155.0f) * dt;
        if (timer[i] <= 0.0f) {
          int old = type[i]; type[i] = NONE; state[i] = 0; cooldown[old] = 12.0f;
        }
      }
    }
  }

  private void scanIncidents(Game game) {
    for (int i = 0; i < FireSystem.MAX; i++) if (game.fire.active[i]) {
      int at = game.fire.cell[i];
      notifyIncident(FIRE, (at % game.grid.width) * 6.0f + 3.0f, (at / game.grid.width) * 6.0f + 3.0f);
      break;
    }
    int patient = findPatient(game, game.cameraX, 10000.0f);
    if (patient >= 0) notifyIncident(MED, game.people.centerX(patient), game.people.centerY(patient));
    for (int i = 0; i < game.people.count; i++) if (game.people.state[i] < PeopleStore.DEAD && game.people.panic[i] > 4.0f) {
      notifyIncident(POLICE, game.people.centerX(i), game.people.centerY(i)); break;
    }
  }

  private void dispatch(Game game, int kind) {
    int slot = -1;
    for (int i = 0; i < MAX_UNITS; i++) if (type[i] == NONE) { slot = i; break; }
    if (slot < 0) return;
    pending[kind] = false;
    type[slot] = (byte)kind; state[slot] = (byte)INBOUND; patient[slot] = -1; work[slot] = 0.0f;
    targetX[slot] = pendingX[kind]; targetY[slot] = pendingY[kind];
    int vehicle = findStationVehicle(game, kind);
    if (vehicle >= 0) {
      x[slot] = game.bodies.x[vehicle]; y[slot] = game.bodies.y[vehicle];
      game.bodies.remove(vehicle);
    } else {
      x[slot] = targetX[slot] < game.grid.width * 3.0f ? 20.0f : game.grid.width * 6.0f - 20.0f;
      y[slot] = game.findGround((int)(targetX[slot] / 6.0f)) * 6.0f - 8.0f;
    }
    timer[slot] = 1.2f + Math.abs(targetX[slot] - x[slot]) / 155.0f + (vehicle < 0 ? 5.0f : 0.0f);
    sirenPlayed[slot] = vehicle >= 0;
    game.announce(kind == FIRE ? "Пожарные выехали" : kind == MED ? "Скорая выехала" : "Полиция выехала");
    if (vehicle >= 0) game.sound(kind == MED ? "medSiren" : "serviceSiren", 0.55f);
  }

  private int findStationVehicle(Game game, int kind) {
    String id = kind == FIRE ? "firetruck" : kind == MED ? "ambulance" : "policecar";
    int wanted = Props.find(id), found = -1; float best = 1000000000.0f;
    if (wanted < 0) return -1;
    for (int i = 0; i < game.bodies.count; i++) if (game.bodies.alive(i) && game.bodies.type[i] == wanted) {
      float distance = Math.abs(game.bodies.x[i] - pendingX[kind]);
      if (distance < best) { best = distance; found = i; }
    }
    return found;
  }

  private void work(Game game, int i) {
    int kind = type[i];
    if (kind == FIRE) {
      int burning = nearestFire(game, targetX[i]);
      if (burning >= 0) {
        targetX[i] = (burning % game.grid.width) * 6.0f + 3.0f;
        targetY[i] = (burning / game.grid.width) * 6.0f + 3.0f;
        game.douse(targetX[i], targetY[i], 72.0f);
        game.fluids.add(game.grid, targetX[i], targetY[i] - 8.0f, 24);
      } else if (work[i] > 2.0f) { firesHandled++; leave(game, i); }
    } else if (kind == MED) {
      int found = patient[i];
      if (found < 0 || found >= game.people.count || game.people.state[found] >= PeopleStore.DEAD
          || game.people.hp[found] >= 99.9f && game.people.bleed[found] < 0.02f)
        found = findPatient(game, targetX[i], 600.0f);
      patient[i] = found;
      if (found >= 0) {
        targetX[i] = game.people.centerX(found); targetY[i] = game.people.centerY(found);
        boolean severe = game.people.noGetUp[found] || game.people.hp[found] < 35.0f;
        game.people.heal(found, 9.0f);
        if (game.people.hp[found] >= 85.0f && game.people.bleed[found] < 0.03f) {
          if (severe) game.people.rescue(found);
          saved++; patient[i] = -1;
        }
      } else if (work[i] > 2.0f) leave(game, i);
    } else {
      game.people.evacuateArea(targetX[i], targetY[i], 240.0f);
      if (work[i] > 7.0f) leave(game, i);
    }
  }

  private int nearestFire(Game game, float nearX) {
    int found = -1; float best = 1000000000.0f;
    for (int i = 0; i < FireSystem.MAX; i++) if (game.fire.active[i]) {
      float px = (game.fire.cell[i] % game.grid.width) * 6.0f + 3.0f;
      float distance = Math.abs(px - nearX);
      if (distance < best) { best = distance; found = game.fire.cell[i]; }
    }
    return found;
  }

  private int findPatient(Game game, float nearX, float range) {
    int found = -1; float best = range;
    for (int i = 0; i < game.people.count; i++) {
      if (game.people.state[i] >= PeopleStore.DEAD || game.people.hidden[i]) continue;
      boolean needs = game.people.hp[i] < 85.0f || game.people.bleed[i] > 0.08f
          || game.people.noGetUp[i] || game.people.dose[i] > 3.0f;
      if (!needs) continue;
      float distance = Math.abs(game.people.centerX(i) - nearX);
      if (distance < best) { best = distance; found = i; }
    }
    return found;
  }

  private void leave(Game game, int i) {
    if (state[i] == LEAVING || state[i] == 0) return;
    state[i] = (byte)LEAVING; timer[i] = 7.0f; patient[i] = -1;
  }

  public int memoryBytes() {
    return type.length * 2 + patient.length * 4 + x.length * 6 * 4
        + pending.length + sirenPlayed.length + pendingX.length * 2 * 4 + cooldown.length * 4;
  }
}
