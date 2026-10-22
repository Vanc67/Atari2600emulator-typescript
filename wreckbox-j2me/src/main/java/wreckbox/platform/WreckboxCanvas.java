package wreckbox.platform;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import wreckbox.core.Game;
import wreckbox.core.Maps;
import wreckbox.core.Materials;
import wreckbox.core.Tools;

/** K510i keypad interface: original menu pages plus the original simulation toolbar. */
public final class WreckboxCanvas extends Canvas implements Runnable {
  static final int MENU_NONE = 0, MENU_MAIN = 1, MENU_SETTINGS = 2, MENU_HELP = 3,
      MENU_ABOUT = 4, MENU_CONTROL = 5, MENU_MAPS = 6;
  private static final String[] MAIN_ITEMS = {
    "Играть", "Карта", "Настройки", "Как играть", "Об игре", "Полный экран"
  };
  private static final String[] SETTINGS_ITEMS = {
    "Выключить звук", "Проверить громкость взрывом", "Режим физики", "Тяжёлые эффекты",
    "Экстренные службы", "Подсказки на экране", "Кровь", "Замедление в ярких моментах"
  };
  private static final String[] CONTROL_ITEMS = {
    "Пауза", "Замедление времени", "Гравитация", "День / ночь", "Показать нагрузку",
    "Звук", "Кровь", "Убрать обломки", "Перезапуск города", "Справка",
    "Скрыть интерфейс", "Меню и настройки"
  };
  private static final String[] HELP_LINES = {
    "КАМЕРА", "Инструмент «Камера»: стрелки двигают",
    "вид, 7/9 меняют масштаб.", "",
    "ИНСТРУМЕНТЫ", "1/3 — выбор, 7/9 — сила/вариант.",
    "5 — действие; удерживайте и ведите.",
    "Динамит: # подрывает заряды по очереди.",
    "Грави-пушка: схватите и бросьте рывком.",
    "Ядерная бомба: 7/9 — мощность, # — тип",
    "взрыва. Сначала прилетает бомбардировщик.",
    "6 секунд спада радиации = час.", "",
    "ЖИТЕЛИ", "11-точечные тела падают и кувыркаются.",
    "Ранят удары, падения, взрывы и огонь.",
    "При тревоге прячутся; скорая лечит раненых.",
    "Облученные бледнеют, тошнотят, слабеют.",
    "Перекрытия ослабляют радиоактивный след.", "",
    "ФИЗИКА", "В настройках — 3 режима и тяжёлые эффекты.",
    "Вода течёт в ямы, гасит огонь и поднимает тела.", "",
    "СТРОЙКА", "1/3 — кисть, здания, люди и нагрузка.",
    "Кисть рисует выбранным материалом.", "",
    "КЛАВИШИ", "Стрелки — курсор; 5 — применить.",
    "0 — гравитация; # — детонация.",
    "* — панель управления; правый soft — пауза.",
    "Левый soft — меню; H скрывает интерфейс",
    "через панель. Весь контент открыт сразу."
  };
  private static final String[] ABOUT_LINES = {
    "Wreckbox — песочница сноса.", "Каждый этаж давит на нижние;",
    "выбейте опору — здание рушится само.", "",
    "Кувалда, динамит, контролируемый снос",
    "и ядерная бомба до 50 мегатонн.",
    "Землетрясения, торнадо, цунами, метеоры,",
    "молнии и извержение вулкана.",
    "Жители, ragdoll, радиация и экстренные",
    "службы. Вода наполняет ямы и подвалы.", "",
    "Автор оригинала: ItsLamba",
    "Оригинальная версия: 1.8",
    "J2ME: адаптация для Sony Ericsson K510i", "128×160, CLDC 1.1 / MIDP 2.0", "",
    "Есть и секреты. Например, клавиша ~."
  };
  private static final String[] SPLASHES = {
    "Кирпич, бетон, паника.", "Гравитация не берёт выходных.",
    "Сталь гнётся. Стекло — нет.", "Каждый этаж давит на нижний. Проверьте.",
    "Страховка не покрывает ядерные удары.", "Чем выше этаж, тем дольше падать."
  };
  private static final String[] MAP_DESCRIPTIONS = {
    "Кварталы и высотные дома", "Дома, дворы и спокойные улицы",
    "Башни и плотная застройка", "Заводы, трубы и резервуары",
    "Корабли, краны и река", "АЭС и радиационные укрытия",
    "Техника, склады и система ПВО", "Лава и вулканические бомбы"
  };

  private final Game game = new Game();
  private final SoundSynth sound = new SoundSynth();
  private final SettingsStore settings = new SettingsStore();
  private final WreckboxRenderer renderer = new WreckboxRenderer(game);
  private final Font font = createFont();
  private static Font createFont() {
    Font value = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    return value != null ? value : Font.getDefaultFont();
  }
  private Thread thread;
  private boolean running, started;
  private int menu = MENU_MAIN, selection, scroll;
  private int cursorX = 64, cursorY = 78, lastPointerX, lastPointerY;
  private int viewWidth = 128, viewHeight = 160;
  private Image buffer;

  public WreckboxCanvas() {
    setFullScreenMode(true);
    game.listener = sound;
    if (!game.loadMap(0)) System.out.println(game.loadError);
    settings.load(game, sound);
    started = game.time > 0.0f;
    cursorX = viewWidth / 2; cursorY = (viewHeight + WreckboxRenderer.TOP - WreckboxRenderer.BOTTOM) / 2;
  }

  final int menuPage() { return menu; }
  final int selectedMap() { return game.mapIndex; }
  final boolean simulationStarted() { return started; }

  public void start() {
    if (running) return;
    running = true; thread = new Thread(this, "Wreckbox"); thread.start();
  }
  public void stop() {
    running = false;
    if (thread != null) thread.interrupt();
    sound.close(); settings.save(game);
  }
  public void suspend() { game.paused = true; sound.close(); settings.save(game); }

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
    viewWidth = w; viewHeight = h;
    cursorX = Math.max(2, Math.min(w - 3, cursorX));
    cursorY = Math.max(WreckboxRenderer.TOP + 2, Math.min(h - WreckboxRenderer.BOTTOM - 3, cursorY));
    if (!isDoubleBuffered() && w > 0 && h > 0) buffer = Image.createImage(w, h);
  }

  protected void paint(Graphics graphics) {
    if (game.grid == null) { graphics.setColor(0); graphics.fillRect(0, 0, viewWidth, viewHeight); return; }
    Graphics g = graphics;
    if (buffer != null) g = buffer.getGraphics();
    if (game.loadError != null) {
      g.setColor(0x111820); g.fillRect(0, 0, viewWidth, viewHeight); g.setColor(0xffaa88); g.setFont(font);
      g.drawString("Ошибка карты", 4, 20, Graphics.LEFT | Graphics.TOP);
      g.drawString(game.loadError, 4, 36, Graphics.LEFT | Graphics.TOP);
    } else {
      renderer.render(g, viewWidth, viewHeight, cursorX, cursorY);
      if (menu != MENU_NONE) drawMenu(g);
    }
    if (buffer != null) graphics.drawImage(buffer, 0, 0, Graphics.TOP | Graphics.LEFT);
  }

  private float worldX(int screenX) { return game.worldX(screenX, viewWidth); }
  private float worldY(int screenY) { return game.worldY(screenY, WreckboxRenderer.TOP, viewHeight - WreckboxRenderer.TOP - WreckboxRenderer.BOTTOM); }
  private void useCursor() { game.pointerDown(worldX(cursorX), worldY(cursorY)); }

  protected void keyPressed(int keyCode) {
    int action = getGameAction(keyCode);
    if (menu != MENU_NONE) {
      if (keyCode == -6) { if (menu == MENU_MAIN) closeMenu(); else openMenu(MENU_MAIN); return; }
      if (keyCode == -7) { if (menu == MENU_MAIN) closeMenu(); else openMenu(MENU_MAIN); return; }
      if (action == UP || keyCode == KEY_NUM2) { moveSelection(-1); return; }
      if (action == DOWN || keyCode == KEY_NUM8) { moveSelection(1); return; }
      if ((action == LEFT || keyCode == KEY_NUM4) && menu == MENU_MAIN && selection == 1) { changeMap(-1); return; }
      if ((action == RIGHT || keyCode == KEY_NUM6) && menu == MENU_MAIN && selection == 1) { changeMap(1); return; }
      if (action == FIRE || keyCode == KEY_NUM5) { activate(); return; }
      return;
    }
    if (keyCode == -6) { openMenu(MENU_MAIN); return; }
    if (keyCode == -7) { game.paused = !game.paused; return; }
    if (keyCode == KEY_STAR) { openMenu(MENU_CONTROL); return; }
    if (keyCode == KEY_POUND) {
      if ("nuke".equals(Tools.IDS[game.toolIndex]) && game.effects.chargeCount() == 0) game.cycleOption(1);
      else game.detonate();
      return;
    }
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
  protected void keyReleased(int keyCode) {
    if (menu == MENU_NONE && (getGameAction(keyCode) == FIRE || keyCode == KEY_NUM5)) game.pointerUp();
  }

  private void moveCursor(int dx, int dy) {
    if ("camera".equals(Tools.IDS[game.toolIndex])) { game.pan(dx, dy); return; }
    cursorX = Math.max(2, Math.min(viewWidth - 3, cursorX + dx));
    cursorY = Math.max(WreckboxRenderer.TOP + 2, Math.min(viewHeight - WreckboxRenderer.BOTTOM - 3, cursorY + dy));
    if (game.held) game.pointerMove(worldX(cursorX), worldY(cursorY), 1.0f / 15.0f);
  }
  private void cycleTool(int delta) {
    int index = game.toolIndex + delta;
    if (index < 0) index = Tools.COUNT - 1;
    if (index >= Tools.COUNT) index = 0;
    game.selectTool(index);
  }
  private void adjustTool(int delta) {
    if ("camera".equals(Tools.IDS[game.toolIndex])) { game.zoomBy(delta > 0 ? 1.2f : 1.0f / 1.2f); return; }
    if (Tools.SLIDER_MAX[game.toolIndex] > Tools.SLIDER_MIN[game.toolIndex]) game.adjustPower(delta);
    else game.cycleOption(delta);
  }

  protected void pointerPressed(int x, int y) {
    if (menu != MENU_NONE) {
      if (menu == MENU_HELP || menu == MENU_ABOUT) return;
      int row = (y - rowTop()) / rowHeight() + scroll;
      if (row >= 0 && row < menuCount()) { selection = row; activate(); }
      return;
    }
    cursorX = Math.max(0, Math.min(viewWidth - 1, x));
    cursorY = Math.max(WreckboxRenderer.TOP, Math.min(viewHeight - WreckboxRenderer.BOTTOM, y));
    lastPointerX = x; lastPointerY = y; useCursor();
  }
  protected void pointerDragged(int x, int y) {
    if (menu != MENU_NONE) return;
    if ("camera".equals(Tools.IDS[game.toolIndex])) game.pan(x - lastPointerX, y - lastPointerY);
    else {
      cursorX = Math.max(0, Math.min(viewWidth - 1, x));
      cursorY = Math.max(WreckboxRenderer.TOP, Math.min(viewHeight - WreckboxRenderer.BOTTOM, y));
      game.pointerMove(worldX(cursorX), worldY(cursorY), 1.0f / 15.0f);
    }
    lastPointerX = x; lastPointerY = y;
  }
  protected void pointerReleased(int x, int y) { if (menu == MENU_NONE) game.pointerUp(); }

  private void openMenu(int which) {
    if (game.held) game.pointerUp();
    menu = which; selection = 0; scroll = 0;
    if (which == MENU_MAPS) selection = game.mapIndex;
    ensureVisible();
  }
  private void closeMenu() {
    menu = MENU_NONE;
    if (!started) game.paused = false;
    started = true;
  }
  private void changeMap(int delta) {
    int index = game.mapIndex + delta;
    if (index < 0) index = Maps.COUNT - 1;
    if (index >= Maps.COUNT) index = 0;
    game.loadMap(index); settings.save(game);
  }
  private int menuCount() {
    if (menu == MENU_MAIN) return MAIN_ITEMS.length;
    if (menu == MENU_MAPS) return Maps.COUNT;
    if (menu == MENU_SETTINGS) return SETTINGS_ITEMS.length;
    if (menu == MENU_CONTROL) return CONTROL_ITEMS.length;
    if (menu == MENU_HELP) return HELP_LINES.length;
    if (menu == MENU_ABOUT) return ABOUT_LINES.length;
    return 1;
  }
  private int rowTop() { return menu == MENU_MAIN ? 48 : 29; }
  private int rowHeight() { return menu == MENU_HELP || menu == MENU_ABOUT || menu == MENU_MAPS ? 11 : 13; }
  private int visibleRows() {
    if (menu == MENU_MAPS) return 6;
    return Math.max(3, (viewHeight - rowTop() - 17) / rowHeight());
  }
  private void moveSelection(int delta) {
    selection += delta; int count = menuCount();
    if (menu == MENU_HELP || menu == MENU_ABOUT) {
      selection = Math.max(0, Math.min(count - 1, selection));
    } else {
      if (selection < 0) selection = count - 1;
      if (selection >= count) selection = 0;
    }
    ensureVisible();
    if (menu == MENU_MAPS && selection != game.mapIndex) {
      game.loadMap(selection); settings.save(game);
    }
  }
  private void ensureVisible() {
    int rows = visibleRows();
    if (selection < scroll) scroll = selection;
    if (selection >= scroll + rows) scroll = selection - rows + 1;
    if (scroll < 0) scroll = 0;
  }

  private void activate() {
    if (menu == MENU_HELP) { openMenu(MENU_MAIN); return; }
    if (menu == MENU_ABOUT) { openMenu(MENU_MAIN); return; }
    if (menu == MENU_MAPS) { game.loadMap(selection); closeMenu(); settings.save(game); return; }
    if (menu == MENU_SETTINGS) {
      switch (selection) {
        case 0: toggleSound(); break;
        case 1: sound.sound("boom", 0.8f); break;
        case 2: game.cyclePhysics(); settings.save(game); break;
        case 3: game.effectsEnabled = !game.effectsEnabled; settings.save(game); break;
        case 4: game.servicesEnabled = !game.servicesEnabled; settings.save(game); break;
        case 5: game.showToasts = !game.showToasts; settings.save(game); break;
        case 6: game.bloodEnabled = !game.bloodEnabled; settings.save(game); break;
        case 7: game.epicSlow = !game.epicSlow; settings.save(game); break;
        default: break;
      }
      return;
    }
    if (menu == MENU_CONTROL) { activateControl(); return; }
    switch (selection) {
      case 0: closeMenu(); break;
      case 1: openMenu(MENU_MAPS); break;
      case 2: openMenu(MENU_SETTINGS); break;
      case 3: openMenu(MENU_HELP); break;
      case 4: openMenu(MENU_ABOUT); break;
      case 5: setFullScreenMode(true); break;
      default: break;
    }
  }

  private void activateControl() {
    switch (selection) {
      case 0: game.paused = !game.paused; closeMenu(); break;
      case 1: game.slowMotion = !game.slowMotion; settings.save(game); break;
      case 2: game.cycleGravity(); settings.save(game); break;
      case 3: game.night = !game.night; settings.save(game); break;
      case 4:
        game.showStress = !game.showStress;
        if (game.showStress) game.structuralDirty = true;
        settings.save(game); break;
      case 5: toggleSound(); break;
      case 6: game.bloodEnabled = !game.bloodEnabled; settings.save(game); break;
      case 7: game.clearDebris(); closeMenu(); break;
      case 8: game.loadMap(game.mapIndex); closeMenu(); settings.save(game); break;
      case 9: openMenu(MENU_HELP); break;
      case 10: game.uiHidden = !game.uiHidden; closeMenu(); break;
      case 11: openMenu(MENU_MAIN); break;
      default: break;
    }
  }
  private void toggleSound() {
    sound.enabled = !sound.enabled; settings.sound = sound.enabled;
    if (!sound.enabled) sound.close();
    settings.save(game);
  }

  private String menuTitle() {
    if (menu == MENU_SETTINGS) return "Настройки";
    if (menu == MENU_MAPS) return "Выбор карты";
    if (menu == MENU_CONTROL) return "Управление симуляцией";
    if (menu == MENU_HELP) return "Как пользоваться";
    if (menu == MENU_ABOUT) return "Об игре";
    return "";
  }
  private String menuItem(int index) {
    if (menu == MENU_HELP) return HELP_LINES[index];
    if (menu == MENU_ABOUT) return ABOUT_LINES[index];
    if (menu == MENU_MAPS) return (index + 1) + ". " + Maps.NAMES[index];
    if (menu == MENU_SETTINGS) {
      if (index == 0) return SETTINGS_ITEMS[index] + ": " + (sound.enabled ? "нет" : "да");
      if (index == 2) return SETTINGS_ITEMS[index] + ": " + game.physicsText();
      if (index == 3) return SETTINGS_ITEMS[index] + ": " + onOff(game.effectsEnabled);
      if (index == 4) return SETTINGS_ITEMS[index] + ": " + onOff(game.servicesEnabled);
      if (index == 5) return SETTINGS_ITEMS[index] + ": " + onOff(game.showToasts);
      if (index == 6) return SETTINGS_ITEMS[index] + ": " + onOff(game.bloodEnabled);
      if (index == 7) return SETTINGS_ITEMS[index] + ": " + onOff(game.epicSlow);
      return SETTINGS_ITEMS[index];
    }
    if (menu == MENU_CONTROL) {
      if (index == 0) return game.paused ? "Продолжить" : CONTROL_ITEMS[index];
      if (index == 1) return CONTROL_ITEMS[index] + ": " + onOff(game.slowMotion);
      if (index == 2) return CONTROL_ITEMS[index] + ": " + (game.gravityIndex == 0 ? "1g" : game.gravityIndex == 1 ? "Луна" : "0g");
      if (index == 3) return game.night ? "Ночь" : "День";
      if (index == 4) return CONTROL_ITEMS[index] + ": " + onOff(game.showStress);
      if (index == 5 || index == 6) return CONTROL_ITEMS[index] + ": " + onOff(index == 5 ? sound.enabled : game.bloodEnabled);
      if (index == 10 && game.uiHidden) return "Показать интерфейс";
      return CONTROL_ITEMS[index];
    }
    if (index == 0 && started) return "Продолжить";
    if (index == 1) return "Карта: ‹ " + Maps.NAMES[game.mapIndex] + " ›";
    return MAIN_ITEMS[index];
  }
  private String onOff(boolean value) { return value ? "вкл" : "выкл"; }

  private void drawMenu(Graphics g) {
    int w = viewWidth, h = viewHeight;
    g.setFont(font); g.setColor(0x101923); g.fillRect(0, 0, w, h);
    g.setColor(0x263b4c); g.fillRect(0, 0, w, menu == MENU_MAIN ? 43 : 24);
    if (menu == MENU_MAIN) {
      g.setColor(0xf2f2ed); g.drawString("Wreck", w / 2 - 23, 5, Graphics.LEFT | Graphics.TOP);
      g.setColor(0xf0ce62); g.drawString("box", w / 2 + 13, 5, Graphics.LEFT | Graphics.TOP);
      g.setColor(0xaebbc6); g.drawString("песочница сноса", w / 2, 18, Graphics.HCENTER | Graphics.TOP);
      g.setColor(0x8093a1); drawFit(g, SPLASHES[game.mapIndex % SPLASHES.length], w / 2, 31, w - 6, true);
    } else {
      g.setColor(0xf0ce62); g.drawString(menuTitle(), w / 2, 6, Graphics.HCENTER | Graphics.TOP);
    }
    int rows = visibleRows(), top = rowTop(), rowHeight = rowHeight();
    for (int row = 0; row < rows && scroll + row < menuCount(); row++) {
      int index = scroll + row, y = top + row * rowHeight;
      boolean textPage = menu == MENU_HELP || menu == MENU_ABOUT;
      if (!textPage && index == selection) { g.setColor(0x385365); g.fillRect(2, y - 2, w - 4, rowHeight); }
      g.setColor(!textPage && index == selection ? 0xffe595 : textPage && menuItem(index).equals(menuItem(index).toUpperCase()) ? 0xf0ce62 : 0xd4dce2);
      drawFit(g, menuItem(index), textPage ? 4 : 6, y, w - (textPage ? 8 : 12), false);
    }
    if (menu == MENU_MAPS) {
      g.setColor(0x182836); g.fillRect(2, 98, w - 4, 43);
      g.setColor(0xd8e1e6); drawFit(g, MAP_DESCRIPTIONS[game.mapIndex], 6, 103, 76, false);
      g.setColor(0x8fa1ae); g.drawString(Maps.WIDTHS[game.mapIndex] + " × 260", 6, 117, Graphics.LEFT | Graphics.TOP);
      g.setColor(0xf0ce62); g.drawString("5 — играть", 6, 129, Graphics.LEFT | Graphics.TOP);
      drawMapPreview(g, w - 41, 102, 36, 34);
    }
    g.setColor(0x8fa1ae);
    String footer = menu == MENU_MAIN ? "ItsLamba · v1.8 · 5 — выбрать" : "Левый soft: назад · 5: действие";
    if (menu == MENU_HELP || menu == MENU_ABOUT) footer = "2/8 — листать · 5 — назад";
    if (menu == MENU_MAPS) footer = "2/8 — выбор · 5 — играть";
    g.drawString(footer, w / 2, h - 12, Graphics.HCENTER | Graphics.TOP);
  }

  private void drawMapPreview(Graphics g, int x, int y, int w, int h) {
    g.setColor(game.night ? 0x152238 : 0x739dbc); g.fillRect(x, y, w, h);
    for (int gy = 70; gy < 240; gy += 4) for (int gx = 0; gx < game.grid.width; gx += 5) {
      int material = game.grid.materialAt(gx, gy);
      if (material == 0) continue;
      g.setColor(game.night ? (Materials.COLORS[material] & 0x7f7f7f) : Materials.COLORS[material]);
      g.fillRect(x + gx * w / game.grid.width, y + (gy - 70) * h / 170, 1, 1);
    }
    g.setColor(0x587083); g.drawRect(x, y, w - 1, h - 1);
  }

  private void drawFit(Graphics g, String text, int x, int y, int maxWidth, boolean centered) {
    String value = text;
    while (value.length() > 1 && font.stringWidth(value) > maxWidth) value = value.substring(0, value.length() - 1);
    g.drawString(value, x, y, (centered ? Graphics.HCENTER : Graphics.LEFT) | Graphics.TOP);
  }
}
