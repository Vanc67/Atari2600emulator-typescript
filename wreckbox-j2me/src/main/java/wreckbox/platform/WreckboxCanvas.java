package wreckbox.platform;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import wreckbox.core.Buildings;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Materials;
import wreckbox.core.Props;
import wreckbox.core.Tools;

public final class WreckboxCanvas extends Canvas implements Runnable {
  private static final int MENU_NONE = 0, MENU_MAIN = 1, MENU_TOOLS = 2, MENU_MAPS = 3, MENU_OPTIONS = 4, MENU_HELP = 5;
  private static final String[] MAIN_ITEMS = {
    "Продолжить", "Инструменты", "Карты", "Новый город", "Материал / предмет",
    "Гравитация", "Slow motion", "День / ночь", "Звук", "Скрыть / показать HUD", "Управление"
  };
  private final Game game = new Game();
  private final SoundSynth sound = new SoundSynth();
  private final SettingsStore settings = new SettingsStore();
  private final WreckboxRenderer renderer = new WreckboxRenderer(game);
  private final int[] optionMap = new int[Props.COUNT];
  private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
  private Thread thread;
  private boolean running;
  private int menu = MENU_NONE, selection, scroll, optionCount;
  private int cursorX = 64, cursorY = 78, lastPointerX, lastPointerY;
  private Image buffer;

  public WreckboxCanvas() {
    setFullScreenMode(true);
    game.listener = sound;
    if (!game.loadMap(0)) System.out.println(game.loadError);
    settings.load(game, sound);
    cursorX = getWidth() / 2; cursorY = (getHeight() + WreckboxRenderer.TOP - WreckboxRenderer.BOTTOM) / 2;
  }

  public void start() {
    if (running) return;
    running = true; thread = new Thread(this, "Wreckbox"); thread.start();
  }
  public void stop() {
    running = false;
    if (thread != null) thread.interrupt();
    settings.save(game);
  }
  public void suspend() { game.paused = true; settings.save(game); }

  public void run() {
    long last = System.currentTimeMillis();
    while (running) {
      long now = System.currentTimeMillis();
      float dt = (now - last) / 1000.0f; last = now;
      if (menu == MENU_NONE && !game.paused) game.update(dt);
      repaint();
      long elapsed = System.currentTimeMillis() - now;
      try { Thread.sleep(Math.max(8L, 66L - elapsed)); }
      catch (InterruptedException e) { if (!running) break; }
    }
  }

  protected void sizeChanged(int w, int h) {
    cursorX = Math.max(2, Math.min(w - 3, cursorX)); cursorY = Math.max(WreckboxRenderer.TOP + 2, Math.min(h - WreckboxRenderer.BOTTOM - 3, cursorY));
    if (!isDoubleBuffered() && w > 0 && h > 0) buffer = Image.createImage(w, h);
  }

  protected void paint(Graphics graphics) {
    if (game.grid == null) { graphics.setColor(0); graphics.fillRect(0, 0, getWidth(), getHeight()); return; }
    Graphics g = graphics;
    if (buffer != null) g = buffer.getGraphics();
    if (game.loadError != null) {
      g.setColor(0x111820); g.fillRect(0, 0, getWidth(), getHeight()); g.setColor(0xffaa88); g.setFont(font);
      g.drawString("Ошибка карты", 4, 20, Graphics.LEFT | Graphics.TOP); g.drawString(game.loadError, 4, 36, Graphics.LEFT | Graphics.TOP);
    } else {
      renderer.render(g, getWidth(), getHeight(), cursorX, cursorY);
      if (menu != MENU_NONE) drawMenu(g);
    }
    if (buffer != null) graphics.drawImage(buffer, 0, 0, Graphics.TOP | Graphics.LEFT);
  }

  private float worldX(int screenX) { return game.worldX(screenX, getWidth()); }
  private float worldY(int screenY) { return game.worldY(screenY, WreckboxRenderer.TOP, getHeight() - WreckboxRenderer.TOP - WreckboxRenderer.BOTTOM); }
  private void useCursor() { if (!game.paused) game.pointerDown(worldX(cursorX), worldY(cursorY)); }

  protected void keyPressed(int keyCode) {
    int action = getGameAction(keyCode);
    if (menu != MENU_NONE) {
      if (keyCode == -6) { if (menu == MENU_MAIN) menu = MENU_NONE; else menu = MENU_MAIN; ensureVisible(); return; }
      if (keyCode == -7) { menu = MENU_NONE; return; }
      if (action == UP || keyCode == KEY_NUM2) { moveSelection(-1); return; }
      if (action == DOWN || keyCode == KEY_NUM8) { moveSelection(1); return; }
      if (action == FIRE || keyCode == KEY_NUM5) { activate(); return; }
      return;
    }
    if (keyCode == -6) { openMenu(MENU_MAIN); return; }
    if (keyCode == -7 || keyCode == KEY_STAR && game.paused) { game.paused = !game.paused; return; }
    if (keyCode == KEY_STAR) { game.slowMotion = !game.slowMotion; settings.save(game); return; }
    if (keyCode == KEY_POUND) { game.detonate(); return; }
    if (keyCode == KEY_NUM0) { game.cycleGravity(); settings.save(game); return; }
    if (keyCode == KEY_NUM1) { cycleTool(-1); return; }
    if (keyCode == KEY_NUM3) { cycleTool(1); return; }
    if (keyCode == KEY_NUM7) { adjustTool(-1); return; }
    if (keyCode == KEY_NUM9) { adjustTool(1); return; }
    if (action == FIRE || keyCode == KEY_NUM5) { useCursor(); return; }
    if (action == UP || keyCode == KEY_NUM2) moveCursor(0, -6);
    else if (action == DOWN || keyCode == KEY_NUM8) moveCursor(0, 6);
    else if (action == LEFT || keyCode == KEY_NUM4) moveCursor(-6, 0);
    else if (action == RIGHT || keyCode == KEY_NUM6) moveCursor(6, 0);
  }

  protected void keyRepeated(int keyCode) {
    int action = getGameAction(keyCode);
    if (action != FIRE && keyCode != KEY_NUM5) keyPressed(keyCode);
  }
  protected void keyReleased(int keyCode) { if (getGameAction(keyCode) == FIRE || keyCode == KEY_NUM5) game.pointerUp(); }

  private void moveCursor(int dx, int dy) {
    if ("camera".equals(Tools.IDS[game.toolIndex])) { game.pan(dx, dy); return; }
    cursorX = Math.max(2, Math.min(getWidth() - 3, cursorX + dx));
    cursorY = Math.max(WreckboxRenderer.TOP + 2, Math.min(getHeight() - WreckboxRenderer.BOTTOM - 3, cursorY + dy));
    if (game.held) game.pointerMove(worldX(cursorX), worldY(cursorY), 1.0f / 15.0f);
  }
  private void cycleTool(int delta) {
    int index = game.toolIndex + delta; if (index < 0) index = Tools.COUNT - 1; if (index >= Tools.COUNT) index = 0;
    game.selectTool(index);
  }
  private void adjustTool(int delta) {
    if ("camera".equals(Tools.IDS[game.toolIndex])) { game.zoomBy(delta > 0 ? 1.2f : 1.0f / 1.2f); return; }
    if (Tools.SLIDER_MAX[game.toolIndex] > Tools.SLIDER_MIN[game.toolIndex]) game.adjustPower(delta);
    else game.cycleOption(delta);
  }

  protected void pointerPressed(int x, int y) {
    if (menu != MENU_NONE) {
      int row = (y - 27) / 13 + scroll;
      if (row >= 0 && row < menuCount()) { selection = row; activate(); }
      return;
    }
    if (game.paused) { game.paused = false; return; }
    cursorX = Math.max(0, Math.min(getWidth() - 1, x)); cursorY = Math.max(WreckboxRenderer.TOP, Math.min(getHeight() - WreckboxRenderer.BOTTOM, y));
    lastPointerX = x; lastPointerY = y; useCursor();
  }
  protected void pointerDragged(int x, int y) {
    if (menu != MENU_NONE || game.paused) return;
    if ("camera".equals(Tools.IDS[game.toolIndex])) game.pan(x - lastPointerX, y - lastPointerY);
    else {
      cursorX = Math.max(0, Math.min(getWidth() - 1, x)); cursorY = Math.max(WreckboxRenderer.TOP, Math.min(getHeight() - WreckboxRenderer.BOTTOM, y));
      game.pointerMove(worldX(cursorX), worldY(cursorY), 1.0f / 15.0f);
    }
    lastPointerX = x; lastPointerY = y;
  }
  protected void pointerReleased(int x, int y) { if (menu == MENU_NONE) game.pointerUp(); }

  private void openMenu(int which) {
    menu = which; selection = 0; scroll = 0;
    if (which == MENU_TOOLS) selection = game.toolIndex;
    if (which == MENU_MAPS) selection = game.mapIndex;
    if (which == MENU_OPTIONS) buildOptions();
    ensureVisible();
  }
  private int menuCount() {
    if (menu == MENU_MAIN) return MAIN_ITEMS.length;
    if (menu == MENU_TOOLS) return Tools.COUNT;
    if (menu == MENU_MAPS) return Maps.COUNT;
    if (menu == MENU_OPTIONS) return optionCount;
    return 1;
  }
  private void moveSelection(int delta) {
    selection += delta; int count = menuCount();
    if (selection < 0) selection = count - 1; if (selection >= count) selection = 0;
    ensureVisible();
  }
  private int visibleRows() { return Math.max(3, (getHeight() - 43) / 13); }
  private void ensureVisible() {
    int rows = visibleRows();
    if (selection < scroll) scroll = selection;
    if (selection >= scroll + rows) scroll = selection - rows + 1;
    if (scroll < 0) scroll = 0;
  }

  private void buildOptions() {
    optionCount = 0;
    String id = Tools.IDS[game.toolIndex];
    if (id.equals("brush")) {
      for (int i = 0; i < Game.BUILD_MATERIAL.length; i++) optionMap[optionCount++] = i;
      selection = game.buildMaterial;
    } else if (id.equals("building")) {
      for (int i = 0; i < Buildings.COUNT; i++) optionMap[optionCount++] = i;
      selection = game.buildingSelection;
    } else if (id.equals("furniture") || id.equals("explosives")) {
      boolean explosives = id.equals("explosives");
      for (int i = 0; i < Props.COUNT; i++) {
        boolean match = explosives ? Props.EXPLOSION_RADIUS[i] > 0.0f : Props.CATEGORIES[i].equals("furniture");
        if (match) { if (i == game.propSelection) selection = optionCount; optionMap[optionCount++] = i; }
      }
    } else optionMap[optionCount++] = -1;
  }

  private void activate() {
    if (menu == MENU_HELP) { menu = MENU_MAIN; return; }
    if (menu == MENU_TOOLS) { game.selectTool(selection); menu = MENU_NONE; return; }
    if (menu == MENU_MAPS) { game.loadMap(selection); menu = MENU_NONE; settings.save(game); return; }
    if (menu == MENU_OPTIONS) {
      String id = Tools.IDS[game.toolIndex]; int value = optionMap[selection];
      if (value >= 0) {
        if (id.equals("brush")) game.buildMaterial = value;
        else if (id.equals("building")) game.buildingSelection = value;
        else game.propSelection = value;
      }
      menu = MENU_NONE; return;
    }
    switch (selection) {
      case 0: menu = MENU_NONE; break;
      case 1: openMenu(MENU_TOOLS); break;
      case 2: openMenu(MENU_MAPS); break;
      case 3: game.loadMap(game.mapIndex); menu = MENU_NONE; break;
      case 4: openMenu(MENU_OPTIONS); break;
      case 5: game.cycleGravity(); settings.save(game); break;
      case 6: game.slowMotion = !game.slowMotion; settings.save(game); break;
      case 7: game.night = !game.night; settings.save(game); break;
      case 8: sound.enabled = !sound.enabled; settings.sound = sound.enabled; settings.save(game); break;
      case 9: game.uiHidden = !game.uiHidden; menu = MENU_NONE; break;
      case 10: openMenu(MENU_HELP); break;
      default: break;
    }
  }

  private String menuTitle() {
    if (menu == MENU_TOOLS) return "ИНСТРУМЕНТЫ";
    if (menu == MENU_MAPS) return "КАРТЫ 1.8";
    if (menu == MENU_OPTIONS) return "ВАРИАНТ";
    if (menu == MENU_HELP) return "УПРАВЛЕНИЕ";
    return "WRECKBOX 1.8";
  }
  private String menuItem(int index) {
    if (menu == MENU_TOOLS) return Tools.NAMES[index];
    if (menu == MENU_MAPS) return Maps.NAMES[index];
    if (menu == MENU_OPTIONS) {
      int value = optionMap[index]; String id = Tools.IDS[game.toolIndex];
      if (value < 0) return "Нет вариантов";
      if (id.equals("brush")) return Materials.NAMES[Game.BUILD_MATERIAL[value]];
      if (id.equals("building")) return Buildings.NAMES[value];
      return Props.NAMES[value];
    }
    if (menu == MENU_HELP) return "5 / огонь — назад";
    if (index == 5) return MAIN_ITEMS[index] + ": " + (game.gravityIndex == 0 ? "1g" : game.gravityIndex == 1 ? "Луна" : "0g");
    if (index == 6) return MAIN_ITEMS[index] + ": " + (game.slowMotion ? "вкл" : "выкл");
    if (index == 7) return game.night ? "Ночь" : "День";
    if (index == 8) return MAIN_ITEMS[index] + ": " + (sound.enabled ? "вкл" : "выкл");
    return MAIN_ITEMS[index];
  }

  private void drawMenu(Graphics g) {
    int w = getWidth(), h = getHeight();
    g.setFont(font); g.setColor(0x101923); g.fillRect(0, 0, w, h);
    g.setColor(0x263b4c); g.fillRect(0, 0, w, 23);
    g.setColor(0xf0ce62); g.drawString(menuTitle(), w / 2, 5, Graphics.HCENTER | Graphics.TOP);
    int rows = visibleRows();
    for (int row = 0; row < rows && scroll + row < menuCount(); row++) {
      int index = scroll + row, y = 27 + row * 13;
      if (index == selection) { g.setColor(0x385365); g.fillRect(2, y - 2, w - 4, 13); }
      g.setColor(index == selection ? 0xffe595 : 0xd4dce2);
      String text = menuItem(index); if (text.length() > 27) text = text.substring(0, 27);
      g.drawString(text, 6, y, Graphics.LEFT | Graphics.TOP);
    }
    if (menu == MENU_HELP) {
      g.setColor(0xcbd5dc);
      g.drawString("1/3 инструмент  7/9 сила", 4, 43, Graphics.LEFT | Graphics.TOP);
      g.drawString("2/4/6/8 курсор  5 действие", 4, 57, Graphics.LEFT | Graphics.TOP);
      g.drawString("# детонация  0 гравитация", 4, 71, Graphics.LEFT | Graphics.TOP);
      g.drawString("* slow motion  soft пауза", 4, 85, Graphics.LEFT | Graphics.TOP);
      g.drawString("Камера: 7/9 зум, стрелки пан", 4, 99, Graphics.LEFT | Graphics.TOP);
      g.drawString("Весь контент открыт сразу", 4, 113, Graphics.LEFT | Graphics.TOP);
    }
    g.setColor(0x8fa1ae); g.drawString("Лево: назад   Право: закрыть", w / 2, h - 13, Graphics.HCENTER | Graphics.TOP);
  }
}
