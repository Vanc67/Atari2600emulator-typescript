package physicsplayground.platform;

import java.io.IOException;
import java.util.Vector;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;
import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.Math2;
import physicsplayground.core.Vec2;
import physicsplayground.game.CameraController;
import physicsplayground.game.Entity;
import physicsplayground.game.InteractionController;
import physicsplayground.game.SandboxTools;
import physicsplayground.game.SandboxWorld;
import physicsplayground.render.SoftwareDraw;
import physicsplayground.render.WorldRenderer;
import physicsplayground.save.SaveManager;

/** K510i presentation, keypad adaptation and fixed-timestep lifecycle; gameplay remains in Core. */
public final class PlayCanvas extends GameCanvas implements Runnable {
    private static final int WIDTH = 128, HEIGHT = 160, WORLD_TOP = 14, WORLD_BOTTOM = 133;
    private static final int WORLD = 0, TOOLS = 1, SPAWN = 2, ACTIONS = 3, HELP = 4, CLEAR = 5;
    private static final float DT = 1f / 120f;
    private static final int EVENT_KEY = 0, POINTER_DOWN = 1, POINTER_MOVE = 2, POINTER_UP = 3;
    private static final class InputEvent { int type, key, x, y; boolean pressed; }
    private static final String[] ACTION_LABELS = {"Reload selected","Heal selected","Freeze / unfreeze","Duplicate group","Delete group","Camera pan mode","Camera reset","Damage amount -5","Damage amount +5","Speed -0.1","Speed +0.1","Reset speed","Quick save","Quick load","Toggle grid","Clear world","Sound on / off","Help","Back","Exit"};
    private static final String[] HELP_LINES = {"PHYSICS PLAYGROUND 0.3.1","DPAD: MOVE CURSOR","5 / FIRE: DRAG, PLACE, TOOL","HOLD 1 / 3: ROTATE","HOLD 7 / 9: ZOOM OUT / IN","HOLD *: FIRE WEAPON / ARM","# PAUSE    0 SPAWN","LEFT SOFT: ALL 9 TOOLS","RIGHT SOFT: ACTIONS","","CAMERA PAN: ACTIONS > CAMERA","CAMERA MODE: DPAD PAN, 5 HOME","SPAWN: 5 PLACE, 1/3 ROTATE","TOOLS: NUMBER KEYS 1-9","INSPECT: SELECT WITH 5","ACTIONS: RELOAD HEAL FREEZE","DUPLICATE DELETE SAVE LOAD","SPEED GRID CLEAR SOUND EXIT","","TRUE SANDBOX: ALL UNLOCKED","FIRE / BACK RETURNS"};
    private final PlayMidlet owner;
    private final Content content;
    private final SandboxWorld world;
    private final InteractionController interaction;
    private final CameraController camera = new CameraController();
    private final SoftwareDraw framebuffer = new SoftwareDraw(WIDTH, HEIGHT);
    private final WorldRenderer renderer;
    private final SaveManager saves;
    private final MidpAudio audio;
    private final Graphics graphics;
    private final Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private final Vector events = new Vector(), spawnable;
    private final String[] toolLabels = new String[9], spawnLabels;
    private final String[] spawnCategories;
    private final Vec2 cursorWorld = new Vec2(), cursorScreen = new Vec2();
    private volatile boolean alive, suspended, resetInput;
    private Thread thread;
    private int screen = WORLD, selected, scroll, helpScroll, cursorX = 64, cursorY = 80;
    private boolean leftHeld, rightHeld, upHeld, downHeld, fireHeld, activationHeld, rotateLeft, rotateRight, zoomOut, zoomIn, cameraMode;
    private float accumulator, timeScale = 1;
    private long lastFrame, fpsStarted, toastExpires;
    private int fpsFrames, fps, slowFrames, fastFrames;
    private String stats = "", toast = "";
    private long nextStats;

    public PlayCanvas(PlayMidlet owner) throws IOException {
        super(false); this.owner = owner; setFullScreenMode(true); graphics = getGraphics();
        content = Content.loadDefault(); world = new SandboxWorld(content, true, System.currentTimeMillis()); interaction = new InteractionController(world);
        saves = new SaveManager(new RmsSaveStore()); audio = new MidpAudio(content); world.audio = audio;
        camera.setViewport(WIDTH, HEIGHT); camera.homePosition.set(0, -60); camera.position.set(camera.homePosition); camera.targetPosition.set(camera.homePosition);
        camera.homeZoom = .75f; camera.zoom = camera.targetZoom = .75f; renderer = new WorldRenderer(world, camera, framebuffer);
        spawnable = content.registry.spawnableEntities(); spawnLabels = new String[spawnable.size()];
        Vector categories = content.registry.categories(); spawnCategories = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) spawnCategories[i] = (String) categories.elementAt(i);
        for (int i = 0; i < spawnable.size(); i++) spawnLabels[i] = ((Definitions.Entity) spawnable.elementAt(i)).displayName;
        for (int i = 0; i < 9; i++) toolLabels[i] = (i + 1) + " " + SandboxTools.NAMES[i];
    }
    public void start() {
        if (alive) return; alive = true; suspended = false; lastFrame = 0; fpsStarted = System.currentTimeMillis(); audio.start();
        thread = new Thread(this, "PhysicsPlayground-game"); thread.start();
    }
    public void stop() { alive = false; suspended = false; if (thread != null) thread.interrupt(); audio.close(); }
    public void suspendGame() { suspended = true; resetInput = true; audio.stopAll(); }
    public void resumeGame() { suspended = false; resetInput = true; lastFrame = 0; }
    public void run() {
        while (alive) {
            long started = System.currentTimeMillis();
            if (!suspended) {
                tick(started); long elapsed = System.currentTimeMillis() - started; adaptQuality(elapsed); sleep(Math.max(1, 66 - elapsed));
            } else sleep(80);
        }
        thread = null;
    }
    private void tick(long now) {
        float realDelta = lastFrame == 0 ? 1f / 15f : Math.min((now - lastFrame) / 1000f, .1f); lastFrame = now;
        if (resetInput) { resetInput = false; clearHeld(); interaction.endDrag(); interaction.activateSelected(false); }
        processEvents(); updateHeld(realDelta); updateSimulation(realDelta);
        if (interaction.isSpawning()) interaction.spawnPlacer.updatePreview(cursorWorld);
        audio.setListener(camera.position); renderer.render(framebuffer); present(now);
    }
    private void updateHeld(float dt) {
        int dx = (rightHeld ? 1 : 0) - (leftHeld ? 1 : 0), dy = (downHeld ? 1 : 0) - (upHeld ? 1 : 0), zoom = (zoomIn ? 1 : 0) - (zoomOut ? 1 : 0);
        if (screen == WORLD && cameraMode) {
            if (dx != 0 || dy != 0 || zoom != 0) camera.update(dt, dx, dy, zoom); else camera.update(dt, 0, 0, 0);
        } else {
            if (screen == WORLD) { cursorX = clamp(cursorX + Math2.round(dx * 85 * dt), 0, WIDTH - 1); cursorY = clamp(cursorY + Math2.round(dy * 85 * dt), WORLD_TOP, WORLD_BOTTOM); }
            camera.update(dt, 0, 0, screen == WORLD ? zoom : 0);
        }
camera.screenToWorld(cursorScreen.set(cursorX, cursorY), cursorWorld);
    }
    private void updateSimulation(float realDelta) {
        float rotate = (rotateRight ? 1 : 0) - (rotateLeft ? 1 : 0);
        if (world.paused) {
            accumulator = 0; interaction.physicsStep(realDelta, DT, screen == WORLD ? rotate : 0, cursorWorld, fireHeld); return;
        }
        accumulator += realDelta * timeScale; int steps = 0; float interactionRealDelta = DT / timeScale;
        while (accumulator >= DT && steps < 12) {
            interaction.physicsStep(interactionRealDelta, DT, screen == WORLD ? rotate : 0, cursorWorld, fireHeld);
            world.step(DT); accumulator -= DT; steps++;
        }
        if (steps == 12 && accumulator > DT) accumulator = DT;
    }
    private void processEvents() {
        while (true) {
            InputEvent event;
            synchronized (events) { if (events.isEmpty()) break; event = (InputEvent) events.elementAt(0); events.removeElementAt(0); }
            if (event.type == EVENT_KEY) handleKey(event.key, event.pressed);
            else handlePointer(event.type, event.x, event.y);
        }
    }
    private void handleKey(int key, boolean pressed) {
        int action = gameAction(key), direction = direction(key, action);
        if (screen == TOOLS && pressed && key >= KEY_NUM1 && key <= KEY_NUM9) { chooseTool(key - KEY_NUM1); return; }
        if (direction != 0) {
            setDirection(direction, pressed);
            if (pressed && screen != WORLD) menuDirection(direction); return;
        }
        if (!pressed) {
            if (isFire(key, action)) primaryRelease(); else if (key == KEY_STAR && activationHeld) { activationHeld = false; interaction.activateSelected(false); }
            if (key == KEY_NUM1) rotateLeft = false; if (key == KEY_NUM3) rotateRight = false; if (key == KEY_NUM7) zoomOut = false; if (key == KEY_NUM9) zoomIn = false; return;
        }
        if (screen != WORLD) { menuKey(key, action); return; }
        if (isFire(key, action)) primaryPress();
        else if (key == KEY_NUM0) openScreen(SPAWN);
        else if (key == KEY_STAR) { activationHeld = true; interaction.activateSelected(true); }
        else if (key == KEY_NUM1) rotateLeft = true; else if (key == KEY_NUM3) rotateRight = true;
        else if (key == KEY_NUM7) zoomOut = true; else if (key == KEY_NUM9) zoomIn = true;
        else if (key == KEY_POUND) { world.paused = !world.paused; accumulator = 0; }
        else if (key == -6) openScreen(TOOLS); else if (key == -7) openScreen(ACTIONS);
    }
    private void primaryPress() {
        if (cameraMode) { camera.resetView(); return; }
        fireHeld = true;
        if (interaction.isSpawning()) { if (!interaction.tryPlaceSpawn(cursorWorld) && interaction.lastMessage.length() > 0) showToast(interaction.lastMessage); return; }
        if (interaction.currentTool == SandboxTools.DRAG) {
            Entity entity = world.pickEntityAt(cursorWorld); interaction.select(entity); if (entity != null) interaction.beginDrag(entity, cursorWorld);
        } else if (interaction.currentTool == SandboxTools.INSPECT) interaction.select(world.pickEntityAt(cursorWorld));
        else { interaction.applyToolAt(cursorWorld); if (interaction.lastMessage.length() > 0) showToast(interaction.lastMessage); }
    }
    private void primaryRelease() { fireHeld = false; interaction.endDrag(); }
    private void handlePointer(int type, int x, int y) {
        if (screen != WORLD) return;
        int offsetX = (getWidth() - WIDTH) / 2, offsetY = (getHeight() - HEIGHT) / 2;
        cursorX = clamp(x - offsetX, 0, WIDTH - 1); cursorY = clamp(y - offsetY, WORLD_TOP, WORLD_BOTTOM); camera.screenToWorld(cursorScreen.set(cursorX, cursorY), cursorWorld);
        if (type == POINTER_DOWN) primaryPress(); else if (type == POINTER_UP) primaryRelease();
    }
    private void openScreen(int target) {
        interaction.endDrag(); interaction.activateSelected(false); clearHeld(); screen = target; selected = 0; scroll = 0;
        if (target == TOOLS) selected = interaction.currentTool;
        if (target == SPAWN && interaction.isSpawning()) {
            String id = interaction.spawnTypeId(); for (int i = 0; i < spawnable.size(); i++) if (((Definitions.Entity) spawnable.elementAt(i)).id.equals(id)) selected = i;
        }
    }
    private void menuDirection(int direction) {
        if (screen == HELP) { helpScroll = clamp(helpScroll + (direction == DOWN ? 1 : direction == UP ? -1 : 0), 0, Math.max(0, HELP_LINES.length - menuRows())); return; }
        if (screen == CLEAR || screen == WORLD) return;
        int count = screen == TOOLS ? 9 : screen == SPAWN ? spawnable.size() : ACTION_LABELS.length;
        if (direction == UP) selected = (selected + count - 1) % count; else if (direction == DOWN) selected = (selected + 1) % count;
        else if (screen == SPAWN && (direction == LEFT || direction == RIGHT)) jumpCategory(direction == RIGHT ? 1 : -1);
    }
    private void menuKey(int key, int action) {
        if (key == -6 || key == -7 || key == KEY_NUM0) { screen = WORLD; clearHeld(); return; }
        if (screen == CLEAR) { if (isFire(key, action)) { world.clearEntities(); interaction.clearSelection(); screen = WORLD; showToast("World cleared."); } return; }
        if (screen == HELP) { if (isFire(key, action) || key == KEY_POUND) screen = WORLD; return; }
        if (!isFire(key, action)) return;
        if (screen == TOOLS) chooseTool(selected); else if (screen == SPAWN) chooseSpawn(); else if (screen == ACTIONS) performAction(selected);
    }
    private void chooseTool(int index) { interaction.setTool(index); screen = WORLD; clearHeld(); showToast(SandboxTools.NAMES[index] + " tool"); }
    private void chooseSpawn() {
        if (selected < 0 || selected >= spawnable.size()) return; Definitions.Entity definition = (Definitions.Entity) spawnable.elementAt(selected);
        interaction.beginSpawn(definition.id); interaction.spawnPlacer.updatePreview(cursorWorld); screen = WORLD; clearHeld(); showToast("Place " + definition.displayName);
    }
    private void performAction(int action) {
        screen = WORLD; clearHeld();
        if (action <= 4 && !interaction.hasSelection()) { showToast("Nothing selected."); return; }
        if (action == 0) interaction.reloadSelected(); else if (action == 1) interaction.healSelected();
        else if (action == 2) interaction.toggleFreezeSelected(); else if (action == 3) interaction.duplicateSelected(); else if (action == 4) interaction.deleteSelected();
        else if (action == 5) { cameraMode = true; cursorX = WIDTH / 2; cursorY = HEIGHT / 2; showToast("Camera: d-pad pans, 5 resets"); }
        else if (action == 6) camera.resetView();
        else if (action == 7) interaction.toolDamageAmount = Math.max(1, interaction.toolDamageAmount - 5);
        else if (action == 8) interaction.toolDamageAmount = Math.min(500, interaction.toolDamageAmount + 5);
        else if (action == 9) timeScale = Math.max(.1f, Math2.round(timeScale * 10 - 1) / 10f);
        else if (action == 10) timeScale = Math.min(2, Math2.round(timeScale * 10 + 1) / 10f);
        else if (action == 11) timeScale = 1;
        else if (action == 12) quickSave(); else if (action == 13) quickLoad(); else if (action == 14) world.toggleGrid();
        else if (action == 15) screen = CLEAR; else if (action == 16) { audio.setEnabled(!audio.isEnabled()); showToast(audio.isEnabled() ? "Sound on" : "Sound off"); }
        else if (action == 17) { screen = HELP; helpScroll = 0; } else if (action == 18) screen = WORLD;
        else if (action == 19) owner.exit();
    }
    private void quickSave() {
        if (saves.saveToSlot("quicksave", world, camera)) showToast("Saved " + world.objectCount() + " object(s)."); else showToast("Save failed: " + saves.lastErrorMessage);
    }
    private void quickLoad() {
        interaction.endDrag(); interaction.cancelSpawn(); interaction.activateSelected(false); accumulator = 0;
        SaveManager.LoadResult result = saves.loadFromSlot("quicksave", world, camera); showToast(result.message);
    }
    private void jumpCategory(int direction) {
        String current = ((Definitions.Entity) spawnable.elementAt(selected)).category; int index = 0;
        for (int i = 0; i < spawnCategories.length; i++) if (spawnCategories[i].equals(current)) index = i;
        index = (index + direction + spawnCategories.length) % spawnCategories.length; String wanted = spawnCategories[index];
        for (int i = 0; i < spawnable.size(); i++) if (((Definitions.Entity) spawnable.elementAt(i)).category.equals(wanted)) { selected = i; return; }
    }
    private void present(long now) {
        int actualWidth = getWidth(), actualHeight = getHeight(), ox = (actualWidth - WIDTH) / 2, oy = (actualHeight - HEIGHT) / 2;
        if (actualWidth != WIDTH || actualHeight != HEIGHT) { graphics.setColor(0); graphics.fillRect(0, 0, actualWidth, actualHeight); }
        graphics.drawRGB(framebuffer.pixels, 0, WIDTH, ox, oy, WIDTH, HEIGHT, false); graphics.setClip(ox, oy, WIDTH, HEIGHT); graphics.translate(ox, oy); graphics.setFont(font);
        if (screen == WORLD) { drawHud(now); drawCursor(); if (interaction.currentTool == SandboxTools.INSPECT && interaction.hasSelection()) drawInspector(); }
        else if (screen == TOOLS) drawMenu("TOOLS", toolLabels, 9);
        else if (screen == SPAWN) drawMenu("SPAWN " + (selected + 1) + "/" + spawnable.size(), spawnLabels, spawnable.size());
        else if (screen == ACTIONS) drawMenu("ACTIONS", ACTION_LABELS, ACTION_LABELS.length);
        else if (screen == HELP) drawHelp(); else if (screen == CLEAR) drawClear();
        graphics.translate(-ox, -oy); flushGraphics(); updateFps(now);
    }
    private void drawHud(long now) {
        graphics.setColor(0x1a1c1f); graphics.fillRect(0, 0, WIDTH, WORLD_TOP); graphics.fillRect(0, 134, WIDTH, 26);
        graphics.setColor(0x373b41); graphics.drawLine(0, 13, WIDTH, 13); graphics.drawLine(0, 134, WIDTH, 134);
        graphics.setColor(world.paused ? 0xe0664f : 0xe3a33b); clipped((interaction.currentTool + 1) + " " + SandboxTools.NAMES[interaction.currentTool] + (world.paused ? " PAUSED" : ""), 2, 3, 90);
        graphics.setColor(0x8b9096); graphics.drawString(fps + "FPS", 126, 3, Graphics.RIGHT | Graphics.TOP);
        if (now >= nextStats) { nextStats = now + 250; stats = world.objectCount() + " obj " + world.entities.size() + " bodies " + one(timeScale) + "x D" + (int) interaction.toolDamageAmount; }
        graphics.setColor(0xd6d8db);
        if (toast.length() > 0 && now < toastExpires) { clipped(toast, 2, 136, 124); graphics.setColor(0x8b9096); clipped("Physics Playground", 2, 147, 124); }
        else {
            clipped(stats, 2, 136, 124); graphics.setColor(0x8b9096);
            clipped(cameraMode ? "DPAD PAN 7/9 ZOOM 5 HOME" : interaction.isSpawning() ? "5 PLACE 1/3 ROTATE" : "5 USE *ACT 0 SPAWN #PAUSE", 2, 147, 124);
        }
    }
    private void drawCursor() {
        graphics.setColor(interaction.isSpawning() ? interaction.spawnPlacer.placementValid ? 0x75c98b : 0xe0664f : 0xe3a33b);
        graphics.drawLine(cursorX - 4, cursorY, cursorX - 1, cursorY); graphics.drawLine(cursorX + 1, cursorY, cursorX + 4, cursorY);
        graphics.drawLine(cursorX, cursorY - 4, cursorX, cursorY - 1); graphics.drawLine(cursorX, cursorY + 1, cursorX, cursorY + 4);
    }
    private void drawInspector() {
        Entity entity = interaction.selectedEntity; graphics.setColor(0x1a1c1f); graphics.fillRect(2, 17, 124, 64); graphics.setColor(0x373b41); graphics.drawRect(2, 17, 124, 64);
        graphics.setColor(0xe3a33b); clipped(entity.assembly == null ? entity.definition.displayName : entity.assembly.definition.displayName, 5, 20, 118);
        graphics.setColor(0xd6d8db); int y = 31;
        clipped(entity.health == null ? entity.material.displayName : "HP " + one(entity.health.currentHealth) + "/" + one(entity.health.maxHealth), 5, y, 118); y += 10;
        if (entity.thermal != null) clipped("Temp " + (int) entity.thermal.temperature + " C " + (entity.thermal.burning ? "BURNING" : ""), 5, y, 118);
        else if (entity.magazine != null) clipped("Ammo " + entity.magazine.rounds + "/" + entity.magazine.capacity + (entity.magazine.reloading ? " reload" : ""), 5, y, 118);
        else clipped("Mass " + one(entity.body.mass) + " kg", 5, y, 118); y += 10;
        if (entity.explosive != null && entity.explosive.armed) clipped("Fuse " + one(entity.explosive.fuseRemaining) + " s", 5, y, 118);
        else if (entity.assembly != null) clipped("Parts " + entity.assembly.parts.size() + " broken " + entity.assembly.brokenJointCount(), 5, y, 118);
        else clipped("Speed " + one(entity.body.linearVelocity.length() / 100) + " m/s", 5, y, 118); y += 10;
        clipped(entity.definition.id + " #" + entity.uid, 5, y, 118); y += 10;
        graphics.setColor(0x8b9096); clipped(entity.body.frozen ? "Frozen" : entity.body.sleeping ? "Sleeping" : "Active", 5, y, 118);
    }
    private int menuRows() { return Math.max(3, (HEIGHT - 48) / Math.max(11, font.getHeight() + 2)); }
    private void drawMenu(String title, String[] labels, int count) {
        int row = Math.max(11, font.getHeight() + 2), rows = menuRows(); if (selected < scroll) scroll = selected; if (selected >= scroll + rows) scroll = selected - rows + 1;
        scroll = clamp(scroll, 0, Math.max(0, count - rows)); graphics.setColor(0x1a1c1f); graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setColor(0xe3a33b); clipped(title, 3, 3, 122); graphics.setColor(0x373b41); graphics.drawLine(0, 16, WIDTH, 16);
        for (int line = 0; line < rows && scroll + line < count; line++) {
            int index = scroll + line, y = 20 + line * row; if (index == selected) { graphics.setColor(0x3d3322); graphics.fillRect(1, y - 1, 126, row); }
            graphics.setColor(index == selected ? 0xe3a33b : 0xd6d8db); clipped((index == selected ? "> " : "  ") + labels[index], 3, y + 1, 122);
        }
        graphics.setColor(0x8b9096); clipped("5 SELECT  0/BACK", 3, 149, 122);
    }
    private void drawHelp() {
        int rows = menuRows() + 1, row = Math.max(10, font.getHeight() + 1); graphics.setColor(0x1a1c1f); graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setColor(0xe3a33b); graphics.drawString("HELP " + (helpScroll + 1) + "/" + HELP_LINES.length, 3, 3, Graphics.LEFT | Graphics.TOP);
        graphics.setColor(0xd6d8db); for (int i = 0; i < rows && helpScroll + i < HELP_LINES.length; i++) clipped(HELP_LINES[helpScroll + i], 3, 17 + i * row, 122);
        graphics.setColor(0x8b9096); graphics.drawString("5/BACK", 3, 150, Graphics.LEFT | Graphics.TOP);
    }
    private void drawClear() {
        graphics.setColor(0x1a1c1f); graphics.fillRect(0, 0, WIDTH, HEIGHT); graphics.setColor(0xe0664f); graphics.drawString("CLEAR WORLD?", 4, 42, Graphics.LEFT | Graphics.TOP);
        graphics.setColor(0xd6d8db); clipped("Remove every object?", 4, 62, 120); graphics.drawString("5 CLEAR", 4, 94, Graphics.LEFT | Graphics.TOP); graphics.drawString("0/BACK CANCEL", 4, 110, Graphics.LEFT | Graphics.TOP);
    }
    private void clipped(String text, int x, int y, int width) {
        if (font.stringWidth(text) <= width) { graphics.drawString(text, x, y, Graphics.LEFT | Graphics.TOP); return; }
        String value = text; while (value.length() > 1 && font.stringWidth(value + "..") > width) value = value.substring(0, value.length() - 1); graphics.drawString(value + "..", x, y, Graphics.LEFT | Graphics.TOP);
    }
    private void showToast(String message) { toast = message == null ? "" : message; toastExpires = System.currentTimeMillis() + 2600; }
    private void clearHeld() {
        leftHeld = rightHeld = upHeld = downHeld = fireHeld = activationHeld = rotateLeft = rotateRight = zoomOut = zoomIn = false;
    }
    private void updateFps(long now) {
        fpsFrames++; long span = now - fpsStarted; if (span >= 1000) { fps = (int) (fpsFrames * 1000 / span); fpsFrames = 0; fpsStarted = now; }
    }
    private void adaptQuality(long elapsed) {
        if (elapsed > 88) { slowFrames++; fastFrames = 0; } else if (elapsed < 48) { fastFrames++; slowFrames = 0; } else { slowFrames = fastFrames = 0; }
        if (slowFrames >= 18) { renderer.effectStride = Math.min(3, renderer.effectStride + 1); slowFrames = 0; }
        if (fastFrames >= 180) { renderer.effectStride = Math.max(1, renderer.effectStride - 1); fastFrames = 0; }
    }
    private int gameAction(int key) { try { return getGameAction(key); } catch (IllegalArgumentException ignored) { return 0; } }
    private static boolean isFire(int key, int action) { return action == FIRE || key == KEY_NUM5; }
    private static int direction(int key, int action) {
        if (action == UP || key == KEY_NUM2) return UP; if (action == DOWN || key == KEY_NUM8) return DOWN;
        if (action == LEFT || key == KEY_NUM4) return LEFT; if (action == RIGHT || key == KEY_NUM6) return RIGHT; return 0;
    }
    private void setDirection(int direction, boolean held) { if (direction == UP) upHeld = held; else if (direction == DOWN) downHeld = held; else if (direction == LEFT) leftHeld = held; else if (direction == RIGHT) rightHeld = held; }
    private static int clamp(int value, int min, int max) { return value < min ? min : value > max ? max : value; }
    private static String one(float value) { return String.valueOf(Math2.round(value * 10) / 10f); }
    private static void sleep(long duration) { try { Thread.sleep(duration); } catch (InterruptedException ignored) {} }
    private void post(int type, int key, int x, int y, boolean pressed) {
        InputEvent event = new InputEvent(); event.type = type; event.key = key; event.x = x; event.y = y; event.pressed = pressed;
        synchronized (events) { if (events.size() > 48) events.removeElementAt(0); events.addElement(event); }
    }
    protected void keyPressed(int keyCode) { post(EVENT_KEY, keyCode, 0, 0, true); }
    protected void keyReleased(int keyCode) { post(EVENT_KEY, keyCode, 0, 0, false); }
    protected void keyRepeated(int keyCode) { if (screen != WORLD) post(EVENT_KEY, keyCode, 0, 0, true); }
    protected void pointerPressed(int x, int y) { post(POINTER_DOWN, 0, x, y, true); }
    protected void pointerDragged(int x, int y) { post(POINTER_MOVE, 0, x, y, true); }
    protected void pointerReleased(int x, int y) { post(POINTER_UP, 0, x, y, false); }
}
