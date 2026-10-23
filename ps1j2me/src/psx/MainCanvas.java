package psx;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Zero-popup JSR-75 file browser + emulator screen.
 *
 * <p>On startup nothing is asked to the user: the memory card root is found
 * lazily through {@code System.getProperty("fileconn.dir.memorycard")} on a
 * background thread, the folder is listed with a plain FileConnection and
 * game images (*.bin, *.iso, *.img, *.cue) are filtered and sorted to the top.
 * Selecting one starts emulation immediately.</p>
 */
public final class MainCanvas extends Canvas implements Runnable, CommandListener {

    private static final int MODE_BOOT = 0;
    private static final int MODE_BROWSER = 1;
    private static final int MODE_GAME = 2;
    private static final int MODE_MENU = 3;
    private static final int MODE_MESSAGE = 4;

    private static final String[] GAME_EXT = { ".bin", ".iso", ".img", ".cue", ".chd", ".mdf" };

    private static final int COLOR_BG = 0x0B1020;
    private static final int COLOR_BAR = 0x1B2540;
    private static final int COLOR_ACCENT = 0x38BDF8;
    private static final int COLOR_TEXT = 0xE2E8F0;
    private static final int COLOR_DIM = 0x94A3B8;
    private static final int COLOR_SEL = 0x2563EB;
    private static final int COLOR_GAME = 0x22C55E;
    private static final int COLOR_DIR = 0xFBBF24;

    private final PS1Midlet midlet;
    private int mode = MODE_BOOT;
    private String status = "Поиск карты памяти...";
    private String rootUrl;
    private String currentUrl;
    private String currentTitle = "/";

    private final Vector entries = new Vector();
    private int selected;
    private int scroll;
    private int viewW, viewH;

    private PS1Machine machine;
    private VirtualCD cd;
    private AudioOutput audio;
    private Thread gameThread;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile boolean soundOn = true;
    private volatile int padButtons = 0xFFFF;
    private int fps;
    private int framesThisSecond;
    private long lastFpsTime;
    private boolean showFps = true;
    private String overlayMessage;

    private final Command exitCommand = new Command("Выход", Command.EXIT, 1);

    public MainCanvas(PS1Midlet midlet) {
        this.midlet = midlet;
        addCommand(exitCommand);
        setCommandListener(this);
        viewW = getWidth();
        viewH = getHeight();
        new Thread(this).start();
    }

    public void commandAction(Command c, Displayable d) {
        if (c == exitCommand) midlet.quit();
    }

    /* ==================================================================== */
    /*  background root discovery                                           */
    /* ==================================================================== */

    public void run() {
        /* browser mode thread: find the card, never pop up a permission box */
        try {
            String root = System.getProperty("fileconn.dir.memorycard");
            if (root == null) root = System.getProperty("fileconn.dir.memorycard.private");
            if (root == null) {
                Enumeration e = FileSystemRegistry.listRoots();
                if (e != null && e.hasMoreElements()) root = (String) e.nextElement();
            }
            if (root == null) {
                status = "Карта памяти не найдена. Нажмите * для повтора.";
                mode = MODE_MESSAGE;
                repaint();
                return;
            }
            if (!root.endsWith("/")) root = root + "/";
            rootUrl = root;
            listDirectory(root, "/");
        } catch (Throwable t) {
            status = "Ошибка доступа: " + t.getClass().getName();
            mode = MODE_MESSAGE;
            repaint();
        }
    }

    private void listDirectory(String url, String title) {
        entries.removeAllElements();
        selected = 0;
        scroll = 0;
        try {
            FileConnection dir = (FileConnection) Connector.open(url, Connector.READ);
            try {
                Enumeration list = dir.list();
                Vector dirs = new Vector();
                Vector games = new Vector();
                Vector others = new Vector();
                String base = url.endsWith("/") ? url : url + "/";
                while (list != null && list.hasMoreElements()) {
                    String name = (String) list.nextElement();
                    boolean isDir = name.endsWith("/");
                    if (name.startsWith(".")) continue;
                    Entry e = new Entry();
                    e.name = isDir ? name.substring(0, name.length() - 1) : name;
                    e.isDir = isDir;
                    e.url = base + name;
                    if (isDir) {
                        dirs.addElement(e);
                    } else if (isGame(name)) {
                        e.game = true;
                        try {
                            FileConnection f = (FileConnection) Connector.open(e.url, Connector.READ);
                            e.size = f.fileSize();
                            f.close();
                        } catch (Throwable t) {
                            e.size = -1;
                        }
                        games.addElement(e);
                    } else {
                        others.addElement(e);
                    }
                }
                addSorted(dirs);
                addSorted(games);
                addSorted(others);
            } finally {
                dir.close();
            }
            currentUrl = url;
            currentTitle = title;
            mode = MODE_BROWSER;
            status = entries.size() == 0 ? "Пустая папка" : entries.size() + " элементов";
        } catch (Throwable t) {
            status = "Не удалось открыть: " + title;
            mode = MODE_MESSAGE;
        }
        repaint();
    }

    private void addSorted(Vector v) {
        /* insertion sort by name, the lists are small */
        for (int i = 1; i < v.size(); i++) {
            Entry key = (Entry) v.elementAt(i);
            int j = i - 1;
            while (j >= 0 && compare(((Entry) v.elementAt(j)).name, key.name) > 0) {
                v.setElementAt(v.elementAt(j), j + 1);
                j--;
            }
            v.setElementAt(key, j + 1);
        }
        for (int i = 0; i < v.size(); i++) entries.addElement(v.elementAt(i));
    }

    private static int compare(String a, String b) {
        return a.toLowerCase().compareTo(b.toLowerCase());
    }

    private static boolean isGame(String name) {
        String lower = name.toLowerCase();
        for (int i = 0; i < GAME_EXT.length; i++) {
            if (lower.endsWith(GAME_EXT[i])) return true;
        }
        return false;
    }

    static final class Entry {
        String name;
        String url;
        boolean isDir;
        boolean game;
        long size;
    }

    /* ==================================================================== */
    /*  input                                                               */
    /* ==================================================================== */

    protected void keyPressed(int keyCode) {
        int action = 0;
        try {
            action = getGameAction(keyCode);
        } catch (Throwable t) { }

        if (mode == MODE_BROWSER) {
            browserKey(keyCode, action);
        } else if (mode == MODE_GAME) {
            gameKey(keyCode, action, true);
        } else if (mode == MODE_MENU) {
            menuKey(keyCode, action);
        } else if (mode == MODE_MESSAGE) {
            if (keyCode == KEY_STAR) {
                mode = MODE_BOOT;
                repaint();
                new Thread(this).start();
            } else if (keyCode == KEY_NUM0) {
                midlet.quit();
            }
        }
    }

    protected void keyReleased(int keyCode) {
        if (mode == MODE_GAME) gameKey(keyCode, 0, false);
    }

    private void browserKey(int keyCode, int action) {
        int size = entries.size();
        if (action == UP || keyCode == KEY_NUM2) {
            if (size > 0) selected = (selected + size - 1) % size;
            repaint();
        } else if (action == DOWN || keyCode == KEY_NUM8) {
            if (size > 0) selected = (selected + 1) % size;
            repaint();
        } else if (action == FIRE || keyCode == KEY_NUM5) {
            activate();
        } else if (keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2) {
            goUp();
        } else if (keyCode == KEY_STAR) {
            listDirectory(currentUrl == null ? rootUrl : currentUrl, currentTitle);
        } else if (keyCode == KEY_POUND) {
            showFps = !showFps;
            repaint();
        }
    }

    private void activate() {
        if (selected < 0 || selected >= entries.size()) return;
        Entry e = (Entry) entries.elementAt(selected);
        if (e.isDir) {
            listDirectory(e.url, e.name);
        } else if (e.game) {
            startGame(e);
        }
    }

    private void goUp() {
        if (currentUrl == null) return;
        if (rootUrl != null && currentUrl.equals(rootUrl)) return;
        String url = currentUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        int slash = url.lastIndexOf('/');
        if (slash <= 6) {
            listDirectory(rootUrl, "/");
        } else {
            String parent = url.substring(0, slash + 1);
            String name = parent.substring(0, parent.length() - 1);
            int ps = name.lastIndexOf('/');
            listDirectory(parent, ps >= 0 ? name.substring(ps + 1) : "/");
        }
    }

    private void menuKey(int keyCode, int action) {
        if (action == UP || keyCode == KEY_NUM2) {
            menuSelection = (menuSelection + 3) % 4;
            repaint();
        } else if (action == DOWN || keyCode == KEY_NUM8) {
            menuSelection = (menuSelection + 1) % 4;
            repaint();
        } else if (action == FIRE || keyCode == KEY_NUM5) {
            doMenu();
        } else if (keyCode == KEY_SOFTKEY2 || keyCode == KEY_NUM0) {
            mode = MODE_GAME;
            paused = false;
            repaint();
        }
    }

    private int menuSelection;

    private void doMenu() {
        switch (menuSelection) {
            case 0:
                mode = MODE_GAME;
                paused = false;
                break;
            case 1:
                if (machine != null) {
                    machine.reset();
                    if (machine.isHleMode()) machine.hleBoot(cd);
                }
                mode = MODE_GAME;
                paused = false;
                break;
            case 2:
                soundOn = !soundOn;
                if (audio != null) audio.setMuted(!soundOn);
                mode = MODE_GAME;
                paused = false;
                break;
            default:
                stopGame();
                break;
        }
        repaint();
    }

    private void gameKey(int keyCode, int action, boolean pressed) {
        int bit = -1;
        if (action == UP || keyCode == KEY_NUM2) bit = 4;
        else if (action == DOWN || keyCode == KEY_NUM8) bit = 6;
        else if (action == LEFT || keyCode == KEY_NUM4) bit = 7;
        else if (action == RIGHT || keyCode == KEY_NUM6) bit = 5;
        else if (action == FIRE || keyCode == KEY_NUM5) bit = 14;   /* Cross  */
        else if (keyCode == KEY_NUM1) bit = 15;                     /* Square */
        else if (keyCode == KEY_NUM3) bit = 12;                     /* Triangle */
        else if (keyCode == KEY_NUM0) bit = 13;                     /* Circle */
        else if (keyCode == KEY_NUM7) bit = 10;                     /* L1 */
        else if (keyCode == KEY_NUM9) bit = 11;                     /* R1 */
        else if (keyCode == KEY_STAR) bit = 3;                      /* Start */
        else if (keyCode == KEY_POUND) bit = 0;                     /* Select */

        if (pressed && bit < 0) {
            if (keyCode == KEY_SOFTKEY1) {
                paused = true;
                menuSelection = 0;
                mode = MODE_MENU;
                repaint();
                return;
            }
            if (keyCode == KEY_SOFTKEY2) {
                soundOn = !soundOn;
                if (audio != null) audio.setMuted(!soundOn);
                overlayMessage = soundOn ? "Звук вкл" : "Звук выкл";
                repaint();
                return;
            }
        }
        if (bit >= 0) {
            if (pressed) padButtons &= ~(1 << bit);
            else padButtons |= (1 << bit);
        }
        if (machine != null) machine.setButtons(padButtons);
    }

    /* ==================================================================== */
    /*  emulation                                                           */
    /* ==================================================================== */

    private void startGame(Entry e) {
        stopGame();
        status = "Открытие " + e.name + "...";
        mode = MODE_MESSAGE;
        repaint();
        try {
            cd = new VirtualCD();
            cd.open(resolveImageUrl(e.url));
            machine = new PS1Machine(cd);
            machine.setViewport(viewW, viewH);
            InputStream bios = null;
            try {
                bios = getClass().getResourceAsStream("/res/roms/scph1001.bin");
            } catch (Throwable t) { }
            boolean real = false;
            if (bios != null) real = machine.loadBios(bios);
            if (!real) {
                if (!machine.hleBoot(cd)) {
                    status = "Не удалось загрузить BIOS/игру";
                    mode = MODE_MESSAGE;
                    repaint();
                    return;
                }
            }
            audio = new AudioOutput(machine.spu);
            if (soundOn) audio.start();
            padButtons = 0xFFFF;
            machine.setButtons(padButtons);
            paused = false;
            mode = MODE_GAME;
            running = true;
            gameThread = new Thread(new GameLoop());
            gameThread.start();
        } catch (Throwable t) {
            status = "Ошибка запуска: " + t.toString();
            mode = MODE_MESSAGE;
        }
        repaint();
    }

    /** A .cue sheet is a text index; the payload lives in the sibling .bin. */
    private String resolveImageUrl(String url) {
        String lower = url.toLowerCase();
        if (!lower.endsWith(".cue")) return url;
        String base = url.substring(0, url.length() - 4);
        String[] candidates = { base + ".bin", base + ".BIN", base + ".img", base + ".iso" };
        for (int i = 0; i < candidates.length; i++) {
            try {
                FileConnection f = (FileConnection) Connector.open(candidates[i], Connector.READ);
                boolean exists = f.exists();
                f.close();
                if (exists) return candidates[i];
            } catch (Throwable t) {
                /* try the next candidate */
            }
        }
        return url;
    }

    private void stopGame() {
        running = false;
        gameThread = null;
        if (audio != null) {
            audio.stop();
            audio = null;
        }
        if (cd != null) {
            cd.close();
            cd = null;
        }
        machine = null;
        if (mode == MODE_GAME || mode == MODE_MENU) {
            mode = MODE_BROWSER;
        }
        repaint();
    }

    final class GameLoop implements Runnable {
        public void run() {
            long last = System.currentTimeMillis();
            lastFpsTime = last;
            while (running) {
                if (paused || !isShown()) {
                    try { Thread.sleep(40); } catch (InterruptedException e) { }
                    last = System.currentTimeMillis();
                    continue;
                }
                try {
                    machine.runFrame();
                    framesThisSecond++;
                } catch (Throwable t) {
                    overlayMessage = "Сбой: " + t.toString();
                    paused = true;
                }
                long now = System.currentTimeMillis();
                if (now - lastFpsTime >= 1000) {
                    fps = framesThisSecond;
                    framesThisSecond = 0;
                    lastFpsTime = now;
                }
                repaint();
                long elapsed = now - last;
                long wait = 16 - elapsed;
                if (wait > 0) {
                    try { Thread.sleep(wait); } catch (InterruptedException e) { }
                }
                last = System.currentTimeMillis();
            }
        }
    }

    /* ==================================================================== */
    /*  painting                                                            */
    /* ==================================================================== */

    protected void paint(Graphics g) {
        viewW = getWidth();
        viewH = getHeight();
        if (mode == MODE_GAME) {
            paintGame(g);
            return;
        }
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, viewW, viewH);

        Font bold = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_MEDIUM);
        Font plain = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
        int barH = bold.getHeight() + 10;

        g.setColor(COLOR_BAR);
        g.fillRect(0, 0, viewW, barH);
        g.setColor(COLOR_ACCENT);
        g.setFont(bold);
        g.drawString("PS1 J2ME", 6, 5, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_DIM);
        g.setFont(plain);
        String right = (mode == MODE_MENU) ? "МЕНЮ" : "Sony PlayStation";
        g.drawString(right, viewW - 6, 8, Graphics.RIGHT | Graphics.TOP);

        if (mode == MODE_BOOT) {
            int cy = viewH / 2;
            g.setColor(COLOR_ACCENT);
            g.setFont(bold);
            g.drawString("Поиск карты памяти", viewW / 2, cy - 20, Graphics.HCENTER | Graphics.BOTTOM);
            g.setColor(COLOR_DIM);
            g.setFont(plain);
            g.drawString("Без запросов безопасности", viewW / 2, cy, Graphics.HCENTER | Graphics.TOP);
            return;
        }

        if (mode == MODE_MESSAGE) {
            drawWrapped(g, status, 10, barH + 20, viewW - 20, plain, COLOR_TEXT);
            g.setColor(COLOR_DIM);
            g.drawString("* - повторить, 0 - выход", viewW / 2, viewH - 20, Graphics.HCENTER | Graphics.TOP);
            return;
        }

        if (mode == MODE_MENU) {
            paintMenu(g, bold, plain, barH);
            return;
        }

        paintBrowser(g, bold, plain, barH);
    }

    private void paintBrowser(Graphics g, Font bold, Font plain, int barH) {
        int rowH = plain.getHeight() + 10;
        int listTop = barH + plain.getHeight() + 12;
        int listH = viewH - listTop - 22;

        g.setColor(COLOR_DIM);
        g.setFont(plain);
        g.drawString(currentTitle, 6, barH + 4, Graphics.LEFT | Graphics.TOP);

        int visible = listH / rowH;
        if (visible < 1) visible = 1;
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + visible) scroll = selected - visible + 1;

        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            if (idx >= entries.size()) break;
            Entry e = (Entry) entries.elementAt(idx);
            int y = listTop + i * rowH;
            if (idx == selected) {
                g.setColor(COLOR_SEL);
                g.fillRect(2, y - 2, viewW - 4, rowH);
            }
            g.setColor(e.isDir ? COLOR_DIR : (e.game ? COLOR_GAME : COLOR_TEXT));
            g.setFont(bold);
            String tag = e.isDir ? "[D]" : (e.game ? "[G]" : "   ");
            g.drawString(tag, 8, y + 2, Graphics.LEFT | Graphics.TOP);
            g.setColor(idx == selected ? 0xFFFFFF : COLOR_TEXT);
            g.setFont(plain);
            String name = e.name;
            int maxW = viewW - 60;
            while (plain.stringWidth(name) > maxW && name.length() > 3) {
                name = name.substring(0, name.length() - 4) + "...";
            }
            g.drawString(name, 34, y + 2, Graphics.LEFT | Graphics.TOP);
            if (e.game && e.size > 0) {
                g.setColor(COLOR_DIM);
                g.drawString(sizeText(e.size), viewW - 6, y + 2, Graphics.RIGHT | Graphics.TOP);
            }
        }

        g.setColor(COLOR_BAR);
        g.fillRect(0, viewH - 18, viewW, 18);
        g.setColor(COLOR_DIM);
        g.setFont(plain);
        g.drawString("5 - открыть  0 - вверх  * - обновить", 4, viewH - 16, Graphics.LEFT | Graphics.TOP);
    }

    private void paintMenu(Graphics g, Font bold, Font plain, int barH) {
        String[] items = { "Продолжить", "Сброс консоли", soundOn ? "Звук: вкл" : "Звук: выкл", "Выход в браузер" };
        int top = barH + 30;
        int rowH = bold.getHeight() + 16;
        for (int i = 0; i < items.length; i++) {
            int y = top + i * rowH;
            if (i == menuSelection) {
                g.setColor(COLOR_SEL);
                g.fillRect(10, y - 4, viewW - 20, rowH);
            }
            g.setColor(i == menuSelection ? 0xFFFFFF : COLOR_TEXT);
            g.setFont(bold);
            g.drawString(items[i], 20, y, Graphics.LEFT | Graphics.TOP);
        }
        g.setColor(COLOR_DIM);
        g.setFont(plain);
        g.drawString("5 - выбрать", 6, viewH - 20, Graphics.LEFT | Graphics.TOP);
    }

    private void paintGame(Graphics g) {
        if (machine == null) {
            g.setColor(COLOR_BG);
            g.fillRect(0, 0, viewW, viewH);
            return;
        }
        int[] frame = machine.getFrame();
        int w = machine.getFrameWidth();
        int h = machine.getFrameHeight();
        if (w != viewW || h != viewH) {
            machine.setViewport(viewW, viewH);
            w = viewW;
            h = viewH;
        }
        if (frame != null && frame.length >= w * h) {
            g.drawRGB(frame, 0, w, 0, 0, w, h, false);
        }
        if (showFps || overlayMessage != null) {
            Font f = Font.getFont(Font.FACE_MONOSPACE, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(f);
            g.setColor(0x000000);
            g.fillRect(0, 0, 90, f.getHeight() + 2);
            g.setColor(COLOR_ACCENT);
            g.drawString(fps + " fps", 3, 1, Graphics.LEFT | Graphics.TOP);
            if (overlayMessage != null) {
                g.setColor(0x000000);
                g.fillRect(0, viewH - 16, viewW, 16);
                g.setColor(COLOR_TEXT);
                g.drawString(overlayMessage, 4, viewH - 15, Graphics.LEFT | Graphics.TOP);
            }
        }
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxW, Font f, int color) {
        g.setFont(f);
        g.setColor(color);
        int lineH = f.getHeight() + 2;
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            if (f.stringWidth(text.substring(start, i + 1)) > maxW) {
                int cut = text.lastIndexOf(" ", i);
                if (cut <= start) cut = i;
                g.drawString(text.substring(start, cut), x, y, Graphics.LEFT | Graphics.TOP);
                y += lineH;
                start = cut + 1;
                if (start > text.length()) break;
            }
            i++;
        }
        if (start < text.length()) g.drawString(text.substring(start), x, y, Graphics.LEFT | Graphics.TOP);
    }

    private static String sizeText(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) return (bytes / (1024L * 1024L * 1024L)) + "G";
        if (bytes >= 1024L * 1024L) return (bytes / (1024L * 1024L)) + "M";
        if (bytes >= 1024L) return (bytes / 1024L) + "K";
        return bytes + "B";
    }

    /* ==================================================================== */
    /*  lifecycle                                                           */
    /* ==================================================================== */

    protected void hideNotify() {
        paused = (mode == MODE_GAME);
    }

    protected void showNotify() {
        repaint();
    }

    public void shutdown() {
        running = false;
        if (audio != null) audio.stop();
        if (cd != null) cd.close();
    }
}
