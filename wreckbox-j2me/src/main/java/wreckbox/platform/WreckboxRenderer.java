package wreckbox.platform;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import wreckbox.core.BodyStore;
import wreckbox.core.CellGrid;
import wreckbox.core.DebrisSystem;
import wreckbox.core.EffectSystem;
import wreckbox.core.FireSystem;
import wreckbox.core.Game;
import wreckbox.core.Materials;
import wreckbox.core.Math2;
import wreckbox.core.Maps;
import wreckbox.core.PeopleStore;
import wreckbox.core.PowerGrid;
import wreckbox.core.Props;
import wreckbox.core.RadiationSystem;
import wreckbox.core.RagdollStore;
import wreckbox.core.Tools;

/** MIDP 2.0 software renderer, intentionally independent from simulation. */
public final class WreckboxRenderer {
  public static final int TOP = 18;
  public static final int BOTTOM = 22;
  private final Game game;
  private final Font small = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
  private int width, height, viewHeight;
  private float offsetX, offsetY;
  private static final int[] FABRIC_COLORS = {0x8e3b46, 0x3f5f8a, 0x5d7a4a, 0x7a5a3c, 0x6b6f78};

  public WreckboxRenderer(Game game) { this.game = game; }

  public void render(Graphics g, int width, int height, int cursorX, int cursorY) {
    this.width = width; this.height = height; viewHeight = height - TOP - BOTTOM;
    offsetX = game.shakeX(); offsetY = game.shakeY();
    g.setColor(0x111722); g.fillRect(0, 0, width, height);
    drawSky(g);
    g.setClip(0, TOP, width, viewHeight);
    drawRiver(g);
    drawPower(g);
    drawCells(g);
    drawBodies(g);
    drawPeople(g);
    drawDebris(g);
    drawFire(g);
    drawFallout(g);
    drawEffects(g);
    drawDisasters(g);
    drawCursor(g, cursorX, cursorY);
    g.setClip(0, 0, width, height);
    if (!game.uiHidden) drawHud(g);
  }

  private int sx(float worldX) { return (int)((worldX - game.cameraX) * game.zoom + width * 0.5f + offsetX); }
  private int sy(float worldY) { return (int)((worldY - game.cameraY) * game.zoom + TOP + viewHeight * 0.5f + offsetY); }
  private int size(float worldSize) { return Math.max(1, (int)(worldSize * game.zoom + 0.5f)); }

  private void drawSky(Graphics g) {
    int bands = 8;
    for (int i = 0; i < bands; i++) {
      int r = game.night ? 12 + i : 92 + i * 9;
      int gg = game.night ? 20 + i * 2 : 151 + i * 7;
      int b = game.night ? 42 + i * 3 : 215 + i * 4;
      g.setColor(r, Math.min(255, gg), Math.min(255, b));
      int y0 = TOP + i * viewHeight / bands, y1 = TOP + (i + 1) * viewHeight / bands;
      g.fillRect(0, y0, width, y1 - y0 + 1);
    }
    if (game.night) {
      g.setColor(220, 226, 218); g.fillArc(width - 24, TOP + 7, 9, 9, 0, 360);
      g.setColor(20, 28, 50); g.fillArc(width - 21, TOP + 5, 8, 8, 0, 360);
      g.setColor(190, 205, 220);
      for (int i = 0; i < 18; i++) g.fillRect((i * 47 + 13) % width, TOP + 5 + (i * 29) % Math.max(10, viewHeight / 2), 1, 1);
    } else {
      g.setColor(255, 238, 174); g.fillArc(width - 28, TOP + 8, 12, 12, 0, 360);
      g.setColor(222, 237, 244);
      g.fillRect(12, TOP + 17, 30, 3); g.fillRect(19, TOP + 14, 15, 3);
      g.fillRect(75, TOP + 34, 35, 3); g.fillRect(85, TOP + 31, 15, 3);
    }
  }

  private void drawRiver(Graphics g) {
    if (game.metadata == null || !game.metadata.river) return;
    int x0 = sx(game.metadata.riverX0 * 6.0f), x1 = sx((game.metadata.riverX1 + 1) * 6.0f);
    int y = sy(game.metadata.riverWaterY);
    g.setColor(game.night ? 0x163d62 : 0x2f78a8);
    g.fillRect(x0, y, Math.max(1, x1 - x0), TOP + viewHeight - y);
    g.setColor(game.night ? 0x50749a : 0x9fd6e8);
    g.drawLine(x0, y, x1, y);
    for (int x = x0 + 5; x < x1; x += 17) g.drawLine(x, y + 3, Math.min(x1, x + 7), y + 3);
  }

  private void drawCells(Graphics g) {
    CellGrid grid = game.grid;
    float halfW = width / (2.0f * game.zoom), halfH = viewHeight / (2.0f * game.zoom);
    int minX = Math.max(0, (int)((game.cameraX - halfW) / 6.0f) - 1);
    int maxX = Math.min(grid.width - 1, (int)((game.cameraX + halfW) / 6.0f) + 1);
    int minY = Math.max(0, (int)((game.cameraY - halfH) / 6.0f) - 1);
    int maxY = Math.min(CellGrid.HEIGHT - 1, (int)((game.cameraY + halfH) / 6.0f) + 1);
    int cellSize = Math.max(1, (int)Math.ceil(6.0f * game.zoom));
    for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
      int at = grid.index(x, y), m = grid.material(at);
      if (m == 0) continue;
      int color = Materials.COLORS[m];
      int tint = grid.tint(at);
      if (tint != 0) color = tintColor(color, tint, x, y);
      float health = grid.healthFraction(at);
      if (health < 0.75f) color = scaleColor(color, 0.55f + 0.45f * health);
      if (game.night && m != 12) color = scaleColor(color, 0.55f);
      g.setColor(color);
      int px = sx(x * 6.0f), py = sy(y * 6.0f);
      g.fillRect(px, py, cellSize, cellSize);
      if (health < 0.5f && cellSize >= 3) {
        g.setColor(scaleColor(color, 0.45f)); g.drawLine(px, py, px + cellSize - 1, py + cellSize - 1);
      }
      if (game.showStress && at < game.structure.stress.length && game.structure.stress[at] > 0.0f) {
        float stress = game.structure.stress[at];
        g.setColor(stress > 1.0f ? 0xe84c3d : stress > 0.7f ? 0xe5b83f : 0x58b96b);
        if (cellSize >= 3) g.drawRect(px, py, cellSize - 1, cellSize - 1); else g.fillRect(px, py, cellSize, cellSize);
      }
      if ((grid.flags(at) & CellGrid.FLAG_REINFORCED) != 0 && cellSize >= 4) {
        g.setColor(0x775540); g.drawRect(px, py, cellSize - 1, cellSize - 1);
      }
    }
  }

  private int tintColor(int color, int tint, int x, int y) {
    int variation = ((x * 17 + y * 31 + tint * 13) & 15) - 7;
    int r = Math.max(0, Math.min(255, ((color >> 16) & 255) + variation));
    int g = Math.max(0, Math.min(255, ((color >> 8) & 255) + variation));
    int b = Math.max(0, Math.min(255, (color & 255) + variation));
    return (r << 16) | (g << 8) | b;
  }
  private int scaleColor(int color, float scale) {
    int r = Math.min(255, (int)(((color >> 16) & 255) * scale));
    int g = Math.min(255, (int)(((color >> 8) & 255) * scale));
    int b = Math.min(255, (int)((color & 255) * scale));
    return (r << 16) | (g << 8) | b;
  }

  private void drawPower(Graphics g) {
    PowerGrid power = game.power;
    g.setColor(game.night ? 0x242b35 : 0x3b4148);
    for (int i = 0; i < power.spanCount; i++) {
      int a = power.spanA[i], b = power.spanB[i];
      if (a < 0 || b < 0 || a >= power.nodeCount || b >= power.nodeCount || (power.spanFlags[i] & 1) == 0) continue;
      int x0 = sx(power.nodeX[a] * 6.0f), y0 = sy(power.nodeTop[a] * 6.0f);
      int x1 = sx(power.nodeX[b] * 6.0f), y1 = sy(power.nodeTop[b] * 6.0f);
      if (Math.max(x0, x1) < 0 || Math.min(x0, x1) >= width) continue;
      int midX = (x0 + x1) / 2, midY = Math.max(y0, y1) + size(9);
      g.drawLine(x0, y0, midX, midY); g.drawLine(midX, midY, x1, y1);
    }
    for (int i = 0; i < power.nodeCount; i++) {
      int x = sx(power.nodeX[i] * 6.0f), y = sy(power.nodeTop[i] * 6.0f);
      if (x < -3 || x > width + 3) continue;
      g.setColor(power.powered[i] ? 0xe4c44e : 0x4a4a4a); g.fillRect(x, y, 2, 2);
    }
  }

  private void drawBodies(Graphics g) {
    BodyStore bodies = game.bodies;
    for (int i = 0; i < bodies.count; i++) {
      if (!bodies.alive(i)) continue;
      int x = sx(bodies.x[i]), y = sy(bodies.y[i]);
      int w = Math.max(1, size(bodies.width[i])), h = Math.max(1, size(bodies.height[i]));
      if (x + w < 0 || x - w > width || y + h < TOP || y - h > TOP + viewHeight) continue;
      int type = bodies.type[i];
      String id = type >= 0 ? Props.IDS[type] : "";
      int color = propColor(type, bodies.variant[i]);
      if (game.night) color = scaleColor(color, 0.65f);
      if (id.equals("tree")) {
        g.setColor(0x6c492e); g.fillRect(x - Math.max(1, w / 8), y - h / 4, Math.max(1, w / 4), h / 2);
        g.setColor(game.night ? 0x25462d : 0x3f783c); g.fillArc(x - w / 2, y - h / 2, w, Math.max(2, h * 2 / 3), 0, 360);
      } else if (type >= 0 && (Props.FLAGS[type] & 1) != 0) {
        g.setColor(0x202329); g.fillArc(x - w / 2 + w / 8, y + h / 4 - 1, Math.max(2, h / 3), Math.max(2, h / 3), 0, 360);
        g.fillArc(x + w / 2 - w / 4, y + h / 4 - 1, Math.max(2, h / 3), Math.max(2, h / 3), 0, 360);
        g.setColor(color); g.fillRect(x - w / 2, y - h / 2, w, Math.max(2, h * 3 / 4));
        g.setColor(0xa8c9d8); g.fillRect(x - w / 5, y - h / 2 + 1, Math.max(1, w * 2 / 5), Math.max(1, h / 4));
      } else {
        g.setColor(scaleColor(color, 0.65f)); g.fillRect(x - w / 2 + 1, y + h / 2 - 1, w, 2);
        g.setColor(color); g.fillRect(x - w / 2, y - h / 2, w, h);
        g.setColor(scaleColor(color, 1.2f)); g.drawLine(x - w / 2, y - h / 2, x + w / 2 - 1, y - h / 2);
        if (w > 4 && h > 4) {
          g.setColor(scaleColor(color, 0.65f)); g.drawRect(x - w / 2, y - h / 2, w - 1, h - 1);
          if (type >= 0 && Props.EXPLOSION_RADIUS[type] > 0.0f) { g.setColor(0xf0c44c); g.fillRect(x - 1, y - 1, 3, 2); }
        }
      }
      if ((bodies.flags[i] & BodyStore.F_BURNING) != 0) {
        g.setColor(0xff8a28); g.fillTriangle(x - 2, y - h / 2, x + 2, y - h / 2, x, y - h / 2 - 5);
      }
    }
  }

  private int propColor(int type, int variant) {
    if (type < 0) return 0x8b8176;
    String debris = Props.DEBRIS[type], id = Props.IDS[type];
    if (id.equals("tree")) return 0x3f783c;
    if (id.equals("gascyl")) return 0x4b9a68;
    if (id.equals("barrel") || id.equals("fueltruck")) return 0xb84e35;
    if (id.equals("tnt") || id.equals("dynamite") || id.equals("ammobox")) return 0xa83e32;
    if (id.equals("sam") || id.equals("tank") || id.equals("btr") || id.equals("mtruck") || id.equals("aagun")) return 0x596343;
    if (debris.equals("splinter")) return 0x9c7048;
    if (debris.equals("metal")) return 0x77828a;
    if (debris.equals("shard")) return 0x9cc7d8;
    if (debris.equals("fabric")) return FABRIC_COLORS[(variant & 255) % FABRIC_COLORS.length];
    return 0xa39887;
  }

  private void drawPeople(Graphics g) {
    PeopleStore people = game.people;
    for (int i = 0; i < people.count; i++) {
      if (people.state[i] == PeopleStore.GONE) continue;
      int skin = (people.variant[i] & 1) == 0 ? 0xd9a077 : 0x9b6547;
      int shirt = (people.variant[i] & 2) == 0 ? 0x4675a0 : 0xa34e4e;
      int pants = (people.variant[i] & 4) == 0 ? 0x333b4c : 0x51463e;
      if (people.sick[i] > 0.0f) skin = people.sick[i] > 0.5f ? 0x91a85c : 0xb5ac70;
      if (people.charred[i] > 0.0f) {
        float scale = 1.0f - people.charred[i] * 0.8f;
        skin = scaleColor(skin, scale); shirt = scaleColor(shirt, scale); pants = scaleColor(pants, scale);
      }
      if (people.state[i] == PeopleStore.DEAD) { skin = scaleColor(skin, 0.65f); shirt = scaleColor(shirt, 0.7f); }
      if (people.rag.active[i]) drawRagdoll(g, people, i, skin, shirt, pants);
      else drawStandingPerson(g, people, i, skin, shirt, pants);
      if (people.burnTime[i] > 0.0f) {
        int x = sx(people.centerX(i)), y = sy(people.centerY(i)) - 5;
        g.setColor(0xff6b24); g.fillTriangle(x - 3, y + 5, x + 3, y + 5, x + ((i & 1) == 0 ? 1 : -1), y - 4);
        g.setColor(0xffd44a); g.fillTriangle(x - 1, y + 4, x + 1, y + 4, x, y - 1);
      }
    }
  }

  private void drawStandingPerson(Graphics g, PeopleStore p, int i, int skin, int shirt, int pants) {
    int x = sx(p.x[i]), feet = sy(p.y[i]);
    if (x < -8 || x > width + 8 || feet < TOP - 16 || feet > TOP + viewHeight + 16) return;
    int headY = sy(p.y[i] - 20.8f), neckY = sy(p.y[i] - 16.0f), hipY = sy(p.y[i] - 8.0f);
    int headSize = Math.max(2, size(4.6f)), direction = p.direction[i];
    float swing = p.mode[i] == PeopleStore.WALK || p.mode[i] == PeopleStore.FLEE || p.mode[i] == PeopleStore.BURN ? Math2.sin(p.phase[i]) : 0.0f;
    int leg = Math.max(1, size(3.8f * swing)), arm = Math.max(1, size(4.0f));
    g.setColor(skin); g.fillArc(x - headSize / 2, headY - headSize / 2, headSize, headSize, 0, 360);
    g.setColor(shirt); g.drawLine(x, neckY, x, hipY);
    int shoulderY = sy(p.y[i] - 15.5f), handY = sy(p.y[i] - 9.0f);
    if (p.mode[i] == PeopleStore.COWER) handY = sy(p.y[i] - 22.0f);
    else if (p.mode[i] == PeopleStore.ILL) handY = sy(p.y[i] - 13.0f);
    g.drawLine(x, shoulderY, x - arm, handY); g.drawLine(x, shoulderY, x + arm, handY + (int)(leg * 0.4f));
    g.setColor(pants);
    g.drawLine(x, hipY, x - 1 - leg / 2, feet); g.drawLine(x, hipY, x + 1 + leg / 2, feet);
    if (p.mode[i] == PeopleStore.ILL) { g.setColor(0x7b9b45); g.fillRect(x + direction * 3, headY + 3, 2, 2); }
  }

  private void drawRagdoll(Graphics g, PeopleStore p, int i, int skin, int shirt, int pants) {
    RagdollStore rag = p.rag; int base = i * RagdollStore.POINTS;
    for (int n = 0; n < RagdollStore.JOINT_A.length; n++) {
      int a = base + RagdollStore.JOINT_A[n], b = base + RagdollStore.JOINT_B[n];
      int ax = sx(rag.x[a]), ay = sy(rag.y[a]), bx = sx(rag.x[b]), by = sy(rag.y[b]);
      if ((ax < -10 && bx < -10) || (ax > width + 10 && bx > width + 10)) continue;
      g.setColor(n < 5 ? shirt : n < 7 ? skin : pants); g.drawLine(ax, ay, bx, by);
    }
    int head = base + RagdollStore.HEAD, x = sx(rag.x[head]), y = sy(rag.y[head]), d = Math.max(2, size(4.6f));
    g.setColor(skin); g.fillArc(x - d / 2, y - d / 2, d, d, 0, 360);
  }

  private void drawDebris(Graphics g) {
    DebrisSystem debris = game.debris;
    for (int i = 0; i < debris.chunkCount; i++) if (debris.active[i]) {
      int x = sx(debris.x[i]), y = sy(debris.y[i]), w = size(debris.width[i]), h = size(debris.height[i]);
      if (x + w < 0 || x - w > width || y + h < TOP || y - h > TOP + viewHeight) continue;
      g.setColor(Materials.COLORS[debris.material[i]]); g.fillRect(x - w / 2, y - h / 2, Math.max(1, w), Math.max(1, h));
      g.setColor(Materials.DUST[debris.material[i]]); g.drawRect(x - w / 2, y - h / 2, Math.max(1, w) - 1, Math.max(1, h) - 1);
    }
    for (int i = 0; i < DebrisSystem.MAX_DUST; i++) if (debris.dustLife[i] > 0.0f) {
      int x = sx(debris.dustX[i]), y = sy(debris.dustY[i]);
      if (x >= 0 && x < width && y >= TOP && y < TOP + viewHeight) { g.setColor(debris.dustColor[i]); g.fillRect(x, y, 1, 1); }
    }
  }

  private void drawFire(Graphics g) {
    FireSystem fire = game.fire;
    for (int i = 0; i < FireSystem.MAX; i++) if (fire.active[i]) {
      int at = fire.cell[i], x = sx((at % game.grid.width) * 6.0f + 3.0f), y = sy((at / game.grid.width) * 6.0f + 5.0f);
      if (x < 0 || x >= width || y < TOP || y >= TOP + viewHeight) continue;
      g.setColor(0xffc13b); g.fillTriangle(x - 2, y, x + 2, y, x + ((i & 1) == 0 ? 1 : -1), y - 5);
      g.setColor(0xff6226); g.fillTriangle(x - 1, y, x + 1, y, x, y - 3);
    }
  }

  private void drawFallout(Graphics g) {
    RadiationSystem radiation = game.radiationSystem;
    for (int i = 0; i < radiation.count; i++) if (radiation.D0[i] > 0.0f || radiation.prompt[i] > 0.0f) {
      float age = game.time - radiation.t0[i];
      if (age < 0.0f || (!radiation.npp[i] && age > 60.0f)) continue;
      float front = Math.min(age * 22.0f, 14000.0f);
      float center = radiation.npp[i] ? radiation.x[i] + radiation.direction[i] * front * 0.5f : radiation.x[i] + 6.0f * Math.min(age, 120.0f);
      float length = radiation.npp[i] ? Math.max(200.0f, front * 0.5f + radiation.L[i] * 0.5f) : radiation.L[i];
      int x = sx(center), y = sy(1300.0f), rx = Math.max(2, size(length)), ry = Math.max(3, size(120.0f));
      if (x + rx < 0 || x - rx > width) continue;
      g.setColor(0x71834b); g.drawArc(x - rx, y - ry, rx * 2, ry * 2, 0, 360);
      for (int n = 0; n < 12; n++) {
        int px = x + ((n * 37 + i * 19) % Math.max(2, rx * 2)) - rx;
        int py = y + ((n * 23 + i * 11) % Math.max(2, ry * 2)) - ry;
        if (px >= 0 && px < width && py >= TOP && py < TOP + viewHeight) g.fillRect(px, py, 1, 1);
      }
    }
  }

  private void drawEffects(Graphics g) {
    EffectSystem e = game.effects;
    for (int i = 0; i < EffectSystem.MAX; i++) {
      int type = e.type[i]; if (type == EffectSystem.NONE) continue;
      int x = sx(e.x[i]), y = sy(e.y[i]);
      if (x < -100 || x > width + 100 || y < TOP - 100 || y > TOP + viewHeight + 100) continue;
      if (type == EffectSystem.CHARGE) {
        g.setColor(0xc53d32); g.fillRect(x - 2, y - 1, 5, 3); g.setColor(0xf2d35c); g.fillRect(x, y - 3, 1, 2);
      } else if (type == EffectSystem.BOMB || type == EffectSystem.METEOR || type == EffectSystem.VOLCANIC) {
        g.setColor(type == EffectSystem.BOMB ? 0x343b42 : 0x5d4a3d); g.fillArc(x - 2, y - 4, 5, 8, 0, 360);
        if (type != EffectSystem.BOMB) { g.setColor(0xff9b35); g.drawLine(x - 3, y - 5, x - 8, y - 12); }
      } else if (type == EffectSystem.ROCKET) {
        g.setColor(0xe3e0d5); g.fillRect(x - 1, y - 3, 3, 6); g.setColor(0xff8a2c); g.fillRect(x - 1, y + 3, 3, 3);
      } else if (type == EffectSystem.MOLOTOV) {
        g.setColor(0x6b8c54); g.fillRect(x - 1, y - 3, 3, 6); g.setColor(0xffb13b); g.fillRect(x, y - 5, 1, 2);
      } else if (type == EffectSystem.LAVA) {
        g.setColor(0xff7a1e); g.fillRect(x - 1, y - 1, 3, 3);
      } else if (type == EffectSystem.WATER) {
        g.setColor(0x6cbbe3); g.fillRect(x, y, 2, 2);
      } else if (type == EffectSystem.FLASH) {
        float fraction = e.life[i] / Math.max(0.01f, e.maxLife[i]);
        int r = Math.max(4, size(e.radius[i] * (1.2f - 0.2f * fraction)));
        g.setColor(fraction > 0.45f ? 0xffffe8 : 0xffc34d); g.fillArc(x - r, y - r, r * 2, r * 2, 0, 360);
      } else if (type == EffectSystem.BLAST) {
        float fraction = 1.0f - e.life[i] / e.maxLife[i];
        int r = Math.max(2, size(e.radius[i] * fraction));
        g.setColor(fraction < 0.35f ? 0xfff0a0 : fraction < 0.7f ? 0xff9a32 : 0x8c8178);
        g.drawArc(x - r, y - r, r * 2, r * 2, 0, 360);
        if (r < 20) g.fillArc(x - r / 2, y - r / 2, r, r, 0, 360);
      } else if (type == EffectSystem.SMOKE) {
        int r = Math.max(2, size(e.radius[i] * (1.2f - e.life[i] / e.maxLife[i])));
        g.setColor(0x777773); g.drawArc(x - r, y - r, r * 2, r * 2, 0, 360);
      } else if (type == EffectSystem.FIRE) {
        g.setColor(0xffad32); g.fillTriangle(x - 2, y + 2, x + 2, y + 2, x, y - size(e.radius[i]));
      } else if (type == EffectSystem.LIGHTNING) {
        g.setColor(0xf5f2c6); int top = Math.max(TOP, y - size(e.radius[i]));
        g.drawLine(x - 2, top, x + 2, top + (y - top) / 3); g.drawLine(x + 2, top + (y - top) / 3, x - 1, top + (y - top) * 2 / 3); g.drawLine(x - 1, top + (y - top) * 2 / 3, x, y);
      }
    }
  }

  private void drawDisasters(Graphics g) {
    if (game.disasterActive(1)) {
      int x = sx(game.disasterX(1)), ground = sy(game.disasterY(1));
      g.setColor(0x747b83);
      for (int i = 0; i < 5; i++) { int yy = ground - i * 8; int r = 3 + i * 3; g.drawArc(x - r, yy - 4, r * 2, 8, 0, 360); }
    }
    if (game.disasterActive(2)) {
      int x = sx(game.disasterX(2)), base = sy(game.disasterY(2)), h = size(game.disasterStrength(2));
      g.setColor(0x3b8fb2); g.fillRect(x - 4, base - h, 12, h);
      g.setColor(0xb0e1e8); g.drawArc(x - 8, base - h - 5, 18, 10, 0, 180);
    }
    if (game.disasterActive(3)) {
      int x = sx(game.disasterX(3)), y = sy(game.disasterY(3)), r = Math.max(4, size(12 + (9 - game.disasterStrength(3)) * 0.1f));
      g.setColor(0x050508); g.fillArc(x - r, y - r, r * 2, r * 2, 0, 360); g.setColor(0x72519b); g.drawArc(x - r - 2, y - r - 2, r * 2 + 4, r * 2 + 4, 0, 360);
    }
  }

  private void drawCursor(Graphics g, int x, int y) {
    if (game.uiHidden) return;
    g.setColor(0x151b24); g.drawLine(x - 5, y, x + 5, y); g.drawLine(x, y - 5, x, y + 5);
    g.setColor(0xffd45c); g.drawLine(x - 4, y, x + 4, y); g.drawLine(x, y - 4, x, y + 4);
  }

  private void drawHud(Graphics g) {
    g.setFont(small);
    g.setColor(game.night ? 0x111722 : 0x17222e); g.fillRect(0, 0, width, TOP); g.fillRect(0, height - BOTTOM, width, BOTTOM);
    g.setColor(0xe6c34e); g.drawString("WRECKBOX", 2, 1, Graphics.LEFT | Graphics.TOP);
    boolean showDose = game.radiationSystem.count > 0 && (game.doseRate > 0.00006f
        || game.time - game.radiationSystem.t0[game.radiationSystem.count - 1] < 30.0f);
    g.setColor(showDose && game.doseRate > 0.001f ? 0xb9e36b : 0xe8edf0);
    g.drawString(showDose ? RadiationSystem.formatDose(game.doseRate) : game.destructionPercent() + "%", width - 2, 1, Graphics.RIGHT | Graphics.TOP);
    String map = Maps.NAMES[game.mapIndex];
    g.setColor(0xaebbc6); g.drawString(map, 2, 10, Graphics.LEFT | Graphics.TOP);
    String status = game.statusText(); if (status.length() > 24) status = status.substring(0, 24);
    g.drawString(status, width - 2, 10, Graphics.RIGHT | Graphics.TOP);
    String tool = Tools.NAMES[game.toolIndex];
    g.setColor(0xf0f2f3); g.drawString(tool, width / 2, height - BOTTOM + 2, Graphics.HCENTER | Graphics.TOP);
    String power = game.powerText(); if (power.length() > 28) power = power.substring(0, 28);
    g.setColor(0xe8c65c); g.drawString(power, width / 2, height - BOTTOM + 12, Graphics.HCENTER | Graphics.TOP);
    g.setColor(0x9aa8b4); g.drawString("1<", 2, height - BOTTOM + 2, Graphics.LEFT | Graphics.TOP); g.drawString(">3", width - 2, height - BOTTOM + 2, Graphics.RIGHT | Graphics.TOP);
    if (game.messageTime > 0.0f && game.showToasts && !game.uiHidden) {
      String text = game.message; if (text.length() > 34) text = text.substring(0, 34);
      int y = TOP + viewHeight - 13;
      g.setColor(0x17222e); g.fillRect(2, y - 1, width - 4, 13);
      g.setColor(0xf2e4b0); g.drawString(text, width / 2, y, Graphics.HCENTER | Graphics.TOP);
    }
    if (game.paused) {
      g.setColor(0x111923); g.fillRect(width / 2 - 28, TOP + viewHeight / 2 - 8, 56, 16);
      g.setColor(0xf1d166); g.drawString("ПАУЗА", width / 2, TOP + viewHeight / 2 - 5, Graphics.HCENTER | Graphics.TOP);
    }
  }
}
