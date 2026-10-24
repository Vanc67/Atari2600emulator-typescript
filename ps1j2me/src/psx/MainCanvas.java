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
 * Zero-popup JSR-75 browser, built-in demo launcher, settings screen,
 * help screen and the emulator display - all in one Canvas.
 *
 * Startup performs no file system access at all: the memory card root is
 * discovered lazily on a worker thread through
 * {@code System.getProperty("fileconn.dir.memorycard")} (with a registry
 * fallback), so no firmware permission dialog is ever raised.
 */
public final class MainCanvas extends Canvas implements Runnable, CommandListener {

    /* ---- screens -------------------------------------------------------- */
    private static final int MODE_BOOT = 0;
    private static final int MODE_BROWSER = 1;
    private static final int MODE_GAME = 2;
    private static final int MODE_PAUSE = 3;
    private static final int MODE_SETTINGS = 4;
    private static final int MODE_HELP = 5;
    private static final int MODE_DEMOS = 6;
    private static final int MODE_MESSAGE = 7;

    private static final String[] GAME_EXT = { ".bin", ".iso", ".img", ".cue", ".chd", ".mdf" };

    /* ---- palette -------------------------------------------------------- */
    private static final int COLOR_BG = 0x0B1020;
    private static final int COLOR_BAR = 0x1B2540;
    private static final int COLOR_ACCENT = 0x38BDF8;
    private static final int COLOR_TEXT = 0xE2E8F0;
    private static final int COLOR_DIM = 0x94A3B8;
    private static final int COLOR_SEL = 0x2563EB;
    private static final int COLOR_GAME = 0x22C55E;
    private static final int COLOR_DIR = 0xFBBF24;

    /* ---- built-in demos (embedded in the JAR) --------------------------- */
    private static final String[] DEMO_RES = {
        "/res/demos/demo_pong.iso",
        "/res/demos/demo_pad.iso",
        "/res/demos/demo_shapes.iso",
        "/res/demos/demo_tex.iso",
        "/res/demos/demo_logo.iso",
        "/res/demos/demo_3d.iso",
        "/res/demos/demo_sound.iso",
    };
    private static final String[] DEMO_NAME = {
        "Pong (игра)",
        "Тест геймпада",
        "Фигуры (GPU)",
        "Текстуры (GPU)",
        "Логотип (шрифт)",
        "3D куб (GTE)",
        "Звук (SPU)",
    };
    private static final String[] DEMO_DESC = {
        "2/8 - ракетка, играбельно",
        "кнопки подсвечиваются",
        "треугольники и спрайты",
        "CLUT, UV, 4 бита",
        "блочный шрифт, курсор",
        "вращение куба через GTE",
        "ADPCM тон в SPU",
    };

    private static final String[] HELP_TEXT = {
        "PS1J2ME - эмулятор Sony PlayStation 1 для Java ME (CLDC 1.1 / MIDP 2.0).",
        "",
        "ВОЗМОЖНОСТИ",
        "- MIPS R3000A: ленивый кэш декодированных блоков CodeBlock[]",
        "- GTE (COP2): RTPS/RTPT/NCLIP/AVSZ/MVMVA и световые команды",
        "- GPU: программный растеризатор без float, VRAM 1 МиБ,",
        "  текстуры 4/8/15 бит, полупрозрачность, спрайты, линии",
        "- SPU: 24 ADPCM-голоса, ADSR, стерео 44.1 кГц",
        "- CD-ROM: чтение образов 300-700 МБ через Rolling InputStream",
        "  и LRU-кэш секторов, без OutOfMemoryError",
        "- BIOS читается из /res/roms/scph1001.bin через",
        "  getResourceAsStream; если образа нет - HLE-загрузка игры",
        "  (SYSTEM.CNF -> PS-X EXE)",
        "",
        "УПРАВЛЕНИЕ В ИГРЕ",
        "2/4/6/8 или джойстик - крестовина",
        "5 - Cross, 1 - Square, 3 - Triangle, 0 - Circle",
        "7/9 - L1/R1, * - Start, # - Select",
        "левый софткей - меню, правый - звук вкл/выкл",
        "",
        "БРАУЗЕР",
        "5 - открыть папку или запустить игру",
        "0 - на уровень выше, * - обновить список",
        "левый софткей - главное меню, правый - эта справка",
        "",
        "НАСТРОЙКИ",
        "Масштаб: на весь экран, 4:3 с полями или 1:1 пиксель.",
        "Пропуск кадров ускоряет 3D-сцены на слабых телефонах.",
        "Настройки сохраняются в RMS и переживают перезапуск.",
        "",
        "ДЕМО",
        "В JAR встроены семь демо: Pong, тест геймпада, фигуры,",
        "текстуры, логотип, 3D-куб и звук. Запускаются из меню",
        "'Демо' и не требуют файлов на карте памяти.",
        "Pong и тест пада проверяют ввод: если кнопки не",
        "работают, смените раскладку в настройках.",
        "",
        "ЗАПУСК",
        "Загрузка игры и звука идёт в фоне, поэтому меню не",
        "подвисает. Если кадр считается дольше 800 мс, эмулятор",
        "сам включает пропуск кадров. Если звук недоступен,",
        "появляется сообщение, а игра продолжается.",
        "",
        "ТРЕБОВАНИЯ",
        "Куча MIDlet: около 5 МиБ. JSR-75 нужен для игр с карты.",
        "Звук работает, если устройство умеет PCM через MMAPI.",
        "",
        "ОГРАНИЧЕНИЯ",
        "MDEC (видеоролики) не декодируется, реверберация SPU и",
        "CD-DA отсутствуют, карты памяти не эмулируются.",
        "",
        "БИОС",
        "Настоящий SCPH1001.BIN защищён авторским правом и в",
        "репозиторий не входит. Положите свой дамп в",
        "res/roms/scph1001.bin и пересоберите: эмулятор сам",
        "определит настоящий BIOS и запустит его.",
    };

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

    /* ---- emulation ------------------------------------------------------ */
    private PS1Machine machine;
    private CdImage cd;
    private AudioOutput audio;
    private Thread gameThread;
    private volatile boolean running;
    private volatile boolean paused;
    private volatile int padButtons = 0xFFFF;
    private int fps;
    private int framesThisSecond;
    private long lastFpsTime;
    private String overlayMessage;
    private long overlayUntil;
    private volatile boolean booting;
    private volatile boolean sawOutput;      /* at least one non black pixel */
    private volatile String lastError;
    private long audioStartedAt;
    private int errorCount;

    /* ---- settings / help / demos ---------------------------------------- */
    private final Config config = Config.get();
    private int settingsSelection;
    private int helpScroll;
    private int demoSelection;
    private int pauseSelection;

    /* ---- scaling -------------------------------------------------------- */
    private int[] columnMap;
    private int[] rowMap;
    private int mapSrcW, mapSrcH, mapDstW, mapDstH;
    private int[] scaledFrame = new int[0];

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
    /*  memory card discovery (background, never prompts)                   */
    /* ==================================================================== */

    public void run() {
        try {
            String root = System.getProperty("fileconn.dir.memorycard");
            if (root == null) root = System.getProperty("fileconn.dir.memorycard.private");
            if (root == null) {
                Enumeration e = FileSystemRegistry.listRoots();
                if (e != null && e.hasMoreElements()) root = (String) e.nextElement();
            }
            if (root == null) {
                status = "Карта памяти не найдена. Нажмите * для повтора, 5 - встроенные демо.";
                mode = MODE_MESSAGE;
                repaint();
                return;
            }
            if (!root.endsWith("/")) root = root + "/";
            rootUrl = root;
            listDirectory(root, "/");
        } catch (Throwable t) {
            status = "Ошибка доступа к файловой системе: " + t.toString();
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

        switch (mode) {
            case MODE_BROWSER: browserKey(keyCode, action); break;
            case MODE_GAME: gameKey(keyCode, action, true); break;
            case MODE_PAUSE: pauseKey(keyCode, action); break;
            case MODE_SETTINGS: settingsKey(keyCode, action); break;
            case MODE_HELP: helpKey(keyCode, action); break;
            case MODE_DEMOS: demoKey(keyCode, action); break;
            default:
                if (keyCode == KEY_STAR) {
                    mode = MODE_BOOT;
                    repaint();
                    new Thread(this).start();
                } else if (keyCode == KEY_NUM5 || keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY1) {
                    openDemos();
                }
                break;
        }
    }

    protected void keyReleased(int keyCode) {
        if (mode != MODE_GAME) return;
        int action = 0;
        try {
            action = getGameAction(keyCode);
        } catch (Throwable t) { }
        if (action == 0) {
            /* directional / fire keys arrive here as KEY_xxx on many phones */
            if (keyCode == KEY_UP) action = UP;
            else if (keyCode == KEY_DOWN) action = DOWN;
            else if (keyCode == KEY_LEFT) action = LEFT;
            else if (keyCode == KEY_RIGHT) action = RIGHT;
            else if (keyCode == KEY_FIRE) action = FIRE;
        }
        gameKey(keyCode, action, false);
    }

    private static boolean upKey(int keyCode, int action) {
        return action == UP || keyCode == KEY_NUM2;
    }

    private static boolean downKey(int keyCode, int action) {
        return action == DOWN || keyCode == KEY_NUM8;
    }

    private static boolean fireKey(int keyCode, int action) {
        return action == FIRE || keyCode == KEY_NUM5;
    }

    private void browserKey(int keyCode, int action) {
        int size = entries.size();
        if (upKey(keyCode, action)) {
            if (size > 0) selected = (selected + size - 1) % size;
            repaint();
        } else if (downKey(keyCode, action)) {
            if (size > 0) selected = (selected + 1) % size;
            repaint();
        } else if (fireKey(keyCode, action)) {
            activate();
        } else if (keyCode == KEY_NUM0) {
            goUp();
        } else if (keyCode == KEY_STAR) {
            listDirectory(currentUrl == null ? rootUrl : currentUrl, currentTitle);
        } else if (keyCode == KEY_POUND) {
            config.showFps = !config.showFps;
            config.save();
            repaint();
        } else if (keyCode == KEY_SOFTKEY1) {
            openPauseMenu();
        } else if (keyCode == KEY_SOFTKEY2) {
            helpScroll = 0;
            mode = MODE_HELP;
            repaint();
        }
    }

    private void activate() {
        if (selected < 0 || selected >= entries.size()) return;
        Entry e = (Entry) entries.elementAt(selected);
        if (e.isDir) {
            listDirectory(e.url, e.name);
        } else if (e.game) {
            startFileGame(e);
        }
    }

    private void goUp() {
        if (currentUrl == null) {
            openPauseMenu();
            return;
        }
        if (rootUrl != null && currentUrl.equals(rootUrl)) {
            openPauseMenu();
            return;
        }
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

    /* ---- pause menu ------------------------------------------------------ */

    private static final String[] PAUSE_ITEMS = {
        "Продолжить игру",
        "Сброс консоли",
        "Настройки",
        "Демо-программы",
        "Справка",
        "Выход в браузер",
    };

    private void openPauseMenu() {
        pauseSelection = 0;
        if (mode == MODE_GAME) paused = true;
        mode = MODE_PAUSE;
        repaint();
    }

    private void pauseKey(int keyCode, int action) {
        if (upKey(keyCode, action)) {
            pauseSelection = (pauseSelection + PAUSE_ITEMS.length - 1) % PAUSE_ITEMS.length;
            repaint();
        } else if (downKey(keyCode, action)) {
            pauseSelection = (pauseSelection + 1) % PAUSE_ITEMS.length;
            repaint();
        } else if (fireKey(keyCode, action)) {
            switch (pauseSelection) {
                case 0:
                    if (machine != null) {
                        paused = false;
                        mode = MODE_GAME;
                    } else {
                        mode = MODE_BROWSER;
                    }
                    break;
                case 1:
                    if (machine != null) {
                        machine.reset();
                        if (machine.isHleMode() && cd != null) machine.hleBoot(cd);
                        paused = false;
                        mode = MODE_GAME;
                        overlay("Сброс выполнен");
                    }
                    break;
                case 2:
                    settingsSelection = 0;
                    mode = MODE_SETTINGS;
                    break;
                case 3:
                    openDemos();
                    break;
                case 4:
                    helpScroll = 0;
                    mode = MODE_HELP;
                    break;
                default:
                    stopGame();
                    break;
            }
            repaint();
        } else if (keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2) {
            if (machine != null) {
                paused = false;
                mode = MODE_GAME;
            } else {
                mode = MODE_BROWSER;
            }
            repaint();
        }
    }

    /* ---- settings -------------------------------------------------------- */

    private static final String[] SETTINGS_ITEMS = {
        "Масштаб",
        "Пропуск кадров",
        "Звук",
        "Громкость",
        "Счётчик FPS",
        "BIOS / HLE",
        "Сбросить настройки",
        "Назад",
    };

    private void settingsKey(int keyCode, int action) {
        if (upKey(keyCode, action)) {
            settingsSelection = (settingsSelection + SETTINGS_ITEMS.length - 1) % SETTINGS_ITEMS.length;
            repaint();
        } else if (downKey(keyCode, action)) {
            settingsSelection = (settingsSelection + 1) % SETTINGS_ITEMS.length;
            repaint();
        } else if (fireKey(keyCode, action) || keyCode == KEY_STAR) {
            applySetting();
        } else if (keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2) {
            mode = MODE_PAUSE;
            repaint();
        }
    }

    private void applySetting() {
        switch (settingsSelection) {
            case 0:
                config.scaleMode = (config.scaleMode + 1) % 3;
                columnMap = null;
                break;
            case 1:
                config.frameSkip = (config.frameSkip + 1) % 3;
                break;
            case 2:
                config.sound = !config.sound;
                if (audio != null) {
                    if (config.sound) audio.start(); else audio.stop();
                }
                break;
            case 3:
                config.volume = (config.volume + 25) % 125;
                if (audio != null) audio.setVolume(config.volume);
                break;
            case 4:
                config.showFps = !config.showFps;
                break;
            case 5:
                config.useHle = !config.useHle;
                break;
            case 6:
                config.reset();
                break;
            default:
                mode = MODE_PAUSE;
                break;
        }
        config.save();
        repaint();
    }

    private String settingValue(int index) {
        switch (index) {
            case 0: return config.scaleName();
            case 1: return config.frameSkip == 0 ? "выкл" : ("-" + config.frameSkip);
            case 2: return config.sound ? "вкл" : "выкл";
            case 3: return config.volume + "%";
            case 4: return config.showFps ? "вкл" : "выкл";
            case 5: return config.useHle ? "всегда HLE" : "BIOS из JAR";
            default: return "";
        }
    }

    /* ---- help ------------------------------------------------------------ */

    private void helpKey(int keyCode, int action) {
        int max = HELP_TEXT.length - 1;
        if (upKey(keyCode, action) || keyCode == KEY_NUM4) {
            helpScroll = Math.max(0, helpScroll - 1);
            repaint();
        } else if (downKey(keyCode, action) || keyCode == KEY_NUM6) {
            helpScroll = Math.min(max, helpScroll + 1);
            repaint();
        } else if (fireKey(keyCode, action) || keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2) {
            mode = MODE_PAUSE;
            repaint();
        }
    }

    /* ---- demos ----------------------------------------------------------- */

    private void openDemos() {
        demoSelection = 0;
        mode = MODE_DEMOS;
        repaint();
    }

    private void demoKey(int keyCode, int action) {
        int count = DEMO_RES.length + 1;
        if (upKey(keyCode, action)) {
            demoSelection = (demoSelection + count - 1) % count;
            repaint();
        } else if (downKey(keyCode, action)) {
            demoSelection = (demoSelection + 1) % count;
            repaint();
        } else if (fireKey(keyCode, action)) {
            if (demoSelection < DEMO_RES.length) startDemo(demoSelection);
        } else if (keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2) {
            mode = MODE_PAUSE;
            repaint();
        }
    }

    /* ---- input in game ---------------------------------------------------- */

    private void gameKey(int keyCode, int action, boolean pressed) {
        int bit = -1;
        if (action == UP || keyCode == KEY_NUM2) bit = 4;           /* up       */
        else if (action == DOWN || keyCode == KEY_NUM8) bit = 6;    /* down     */
        else if (action == LEFT || keyCode == KEY_NUM4) bit = 7;    /* left     */
        else if (action == RIGHT || keyCode == KEY_NUM6) bit = 5;   /* right    */
        else if (action == FIRE || keyCode == KEY_NUM5) bit = 14;   /* cross    */
        else if (keyCode == KEY_NUM1) bit = 15;                     /* square   */
        else if (keyCode == KEY_NUM3) bit = 12;                     /* triangle */
        else if (keyCode == KEY_NUM0) bit = 13;                     /* circle   */
        else if (keyCode == KEY_NUM7) bit = 10;                     /* L1       */
        else if (keyCode == KEY_NUM9) bit = 11;                     /* R1       */
        else if (keyCode == KEY_STAR) bit = 3;                      /* start    */
        else if (keyCode == KEY_POUND) bit = 0;                     /* select   */

        if (config.padLayout == 1) {
            if (bit == 14) bit = 13;
            else if (bit == 13) bit = 14;
        }

        if (pressed && bit < 0) {
            if (keyCode == KEY_SOFTKEY1) {
                openPauseMenu();
                return;
            }
            if (keyCode == KEY_SOFTKEY2) {
                config.sound = !config.sound;
                config.save();
                if (config.sound) {
                    if (machine != null && audio == null) startAudioAsync(machine);
                } else {
                    if (audio != null) {
                        audio.stop();
                        audio = null;
                    }
                }
                overlay(config.sound ? "Звук включён" : "Звук выключен");
                return;
            }
        }
        if (bit >= 0) {
            if (pressed) padButtons &= ~(1 << bit);
            else padButtons |= (1 << bit);
        }
        PS1Machine m = machine;
        if (m != null) m.setButtons(padButtons);
    }

    /** Shows a short message in the corner of the game screen. */
    private void overlay(String message) {
        overlayMessage = message;
        overlayUntil = System.currentTimeMillis() + 2000;
        repaint();
    }

    /* ---- asynchronous boot ------------------------------------------------
     * Everything that touches storage, the JAR, the heap or the audio system
     * runs on a worker thread. The MIDlet event thread only paints, so a slow
     * memory card or a device that blocks inside Manager.createPlayer() can
     * never freeze the UI (this was the "demo freezes the phone" bug).
     * -------------------------------------------------------------------- */

    private interface ImageFactory {
        CdImage create() throws Exception;
    }

    private void startDemo(final int index) {
        if (booting) {
            overlay("Загрузка уже идёт");
            return;
        }
        /* Anchor the class lookup on MainCanvas itself: getClass() inside the
           anonymous factory below would return the inner class, and some
           devices then fail to find /res/demos/... */
        final Class anchor = getClass();
        beginBoot("Загрузка демо: " + DEMO_NAME[index], new ImageFactory() {
            public CdImage create() {
                MemoryCD mem = MemoryCD.fromResource(anchor, DEMO_RES[index], DEMO_NAME[index]);
                return mem;
            }
        });
    }

    private void startFileGame(final Entry e) {
        if (booting) {
            overlay("Загрузка уже идёт");
            return;
        }
        final String url = resolveImageUrl(e.url);
        final String name = e.name;
        mode = MODE_MESSAGE;
        status = "Открытие " + name + "...";
        repaint();
        beginBoot("Открытие " + name + "...", new ImageFactory() {
            public CdImage create() throws Exception {
                VirtualCD vcd = new VirtualCD();
                vcd.open(url);
                return vcd;
            }
        });
    }

    /** Starts a background boot; the UI keeps repainting meanwhile. */
    private void beginBoot(final String label, final ImageFactory factory) {
        stopGame();
        booting = true;
        errorCount = 0;
        mode = MODE_MESSAGE;
        status = label;
        repaint();
        Thread t = new Thread(new Runnable() {
            public void run() {
                CdImage image = null;
                try {
                    image = factory.create();
                    if (image == null) {
                        failBoot("Образ не найден в JAR. Пересоберите PS1J2ME.jar (./build.sh).");
                        return;
                    }
                    bootNow(image);
                } catch (OutOfMemoryError oom) {
                    freeMemoryHint();
                    failBoot("Не хватает памяти MIDlet. 2 МиБ RAM + 1 МиБ VRAM + 512 КиБ BIOS"
                            + " требуют ~5 МиБ кучи. Закройте другие приложения или уменьшите"
                            + " масштаб экрана в настройках.");
                } catch (Throwable t2) {
                    failBoot("Ошибка запуска: " + t2.toString());
                } finally {
                    booting = false;
                }
            }
        }, "ps1-boot");
        t.start();
    }

    private void failBoot(String message) {
        machine = null;
        status = message;
        mode = MODE_MESSAGE;
        repaint();
    }

    private void bootNow(CdImage image) {
        cd = image;
        PS1Machine m = new PS1Machine(image);
        m.setViewport(320, 240);
        m.spu.setMasterVolume(config.volume);

        boolean real = false;
        if (!config.useHle) {
            InputStream bios = null;
            try {
                bios = getClass().getResourceAsStream("/res/roms/scph1001.bin");
            } catch (Throwable t) { }
            if (bios != null) {
                try {
                    real = m.loadBios(bios);
                } catch (Throwable t) {
                    real = false;
                }
            }
        }
        if (!real && !m.hleBoot(image)) {
            failBoot(m.isBiosLoaded()
                    ? "BIOS загружен, но на диске нет SYSTEM.CNF/PSX.EXE"
                    : "Не найден BIOS и не удалось загрузить игру");
            return;
        }

        machine = m;
        sawOutput = false;
        errorCount = 0;
        padButtons = 0xFFFF;
        m.setButtons(padButtons);
        paused = false;
        running = true;
        mode = MODE_GAME;
        gameThread = new Thread(new GameLoop(), "ps1-game");
        gameThread.start();
        startAudioAsync(m);
        repaint();
    }

    /**
     * Audio is started on its own thread: some devices block for seconds (or
     * forever) inside Manager.createPlayer(), and that must never stall the
     * emulator or the UI.
     */
    private void startAudioAsync(final PS1Machine m) {
        if (!config.sound) {
            audio = null;
            return;
        }
        final AudioOutput a = new AudioOutput(m.spu);
        a.setVolume(config.volume);
        audio = a;
        audioStartedAt = System.currentTimeMillis();
        Thread t = new Thread(new Runnable() {
            public void run() {
                a.start();
                if (!a.available) overlay("Звук недоступен на этом устройстве");
            }
        }, "ps1-audio");
        t.start();
    }

    private void freeMemoryHint() {
        machine = null;
        cd = null;
        columnMap = null;
        rowMap = null;
        scaledFrame = new int[0];
        System.gc();
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
            try { audio.stop(); } catch (Throwable t) { }
            audio = null;
        }
        if (cd != null) {
            try { cd.close(); } catch (Throwable t) { }
            cd = null;
        }
        machine = null;
        if (mode == MODE_GAME || mode == MODE_PAUSE) mode = MODE_BROWSER;
        repaint();
    }

    final class GameLoop implements Runnable {
        public void run() {
            long last = System.currentTimeMillis();
            lastFpsTime = last;
            int skipCounter = 0;
            int slowFrames = 0;
            while (running) {
                PS1Machine m = machine;
                if (m == null) break;
                if (paused) {
                    try { Thread.sleep(40); } catch (InterruptedException e) { }
                    last = System.currentTimeMillis();
                    continue;
                }
                long frameStart = System.currentTimeMillis();
                try {
                    m.runFrame();
                    framesThisSecond++;
                } catch (OutOfMemoryError oom) {
                    abortGame("Не хватает памяти MIDlet для эмуляции.\n"
                            + "Нужно около 5 МиБ. Уменьшите масштаб экрана,\n"
                            + "закройте другие приложения и попробуйте снова.");
                    return;
                } catch (Throwable t) {
                    errorCount++;
                    lastError = t.toString();
                    if (errorCount >= 3) {
                        abortGame("Сбой эмуляции: " + t.toString());
                        return;
                    }
                }
                if (!sawOutput && (m.framesRun & 31) == 0) {
                    sawOutput = hasVisibleOutput(m);
                    if (!sawOutput && m.framesRun > 240) {
                        overlayMessage = "Игра не рисует кадры (нет вывода GPU)";
                        overlayUntil = System.currentTimeMillis() + 6000;
                    }
                }
                long frameTime = System.currentTimeMillis() - frameStart;
                if (frameTime > 800) {
                    slowFrames++;
                    if (slowFrames == 3 && config.frameSkip == 0) {
                        /* the device cannot keep up: skip frames automatically */
                        config.frameSkip = 1;
                        config.save();
                        overlay("Медленное устройство: включён пропуск кадров");
                    }
                }

                long now = System.currentTimeMillis();
                if (now - lastFpsTime >= 1000) {
                    fps = framesThisSecond;
                    framesThisSecond = 0;
                    lastFpsTime = now;
                }
                if (config.frameSkip > 0 && skipCounter < config.frameSkip) {
                    skipCounter++;
                } else {
                    skipCounter = 0;
                    repaint();
                }
                checkAudio();
                long elapsed = System.currentTimeMillis() - last;
                long wait = 16 - elapsed;
                if (wait > 0 && config.frameSkip == 0) {
                    try { Thread.sleep(wait); } catch (InterruptedException e) { }
                }
                last = System.currentTimeMillis();
            }
        }
    }

    /** True when the emulator has drawn something other than black. */
    private static boolean hasVisibleOutput(PS1Machine m) {
        int[] frame = m.getFrame();
        if (frame == null) return false;
        int step = frame.length > 4096 ? 7 : 1;
        for (int i = 0; i < frame.length; i += step) {
            if ((frame[i] & 0xFFFFFF) != 0) return true;
        }
        return false;
    }

    /** Stops emulation and shows a readable message instead of a black screen. */
    private void abortGame(String message) {
        running = false;
        paused = true;
        lastError = message;
        if (audio != null) {
            try { audio.stop(); } catch (Throwable t) { }
            audio = null;
        }
        if (cd != null) {
            try { cd.close(); } catch (Throwable t) { }
            cd = null;
        }
        machine = null;
        status = message;
        mode = MODE_MESSAGE;
        repaint();
    }

    /** Detects an audio system that never came up, without ever blocking. */
    private void checkAudio() {
        AudioOutput a = audio;
        if (a == null || a.isRunning()) return;
        long age = System.currentTimeMillis() - audioStartedAt;
        if (!a.isStarting() && age > 4000) {
            audio = null;
            overlay("Звук недоступен на этом устройстве");
        } else if (a.isStarting() && age > 15000) {
            /* the device is stuck inside Manager: abandon the player */
            audio = null;
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

        Font bold = null;
        Font plain = null;
        int barH = TinyFont.HEIGHT + 10;

        g.setColor(COLOR_BAR);
        g.fillRect(0, 0, viewW, barH);
        g.setColor(COLOR_ACCENT);
        boldFlag = true;
        txt(g, "PS1 J2ME", 6, 5, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_DIM);
        boldFlag = false;
        txt(g, screenTitle(), viewW - 6, 5, Graphics.RIGHT | Graphics.TOP);

        switch (mode) {
            case MODE_BOOT: paintBoot(g, bold, plain); break;
            case MODE_MESSAGE: paintMessage(g, plain, barH); break;
            case MODE_PAUSE: paintList(g, bold, plain, barH, PAUSE_ITEMS, pauseSelection, null, "5 - выбрать  0 - назад"); break;
            case MODE_SETTINGS: paintSettings(g, bold, plain, barH); break;
            case MODE_HELP: paintHelp(g, bold, plain, barH); break;
            case MODE_DEMOS: paintDemos(g, bold, plain, barH); break;
            default: paintBrowser(g, bold, plain, barH); break;
        }
    }

    /** True when the next txt() call should use the pseudo-bold font. */
    private static boolean boldFlag;

    /** Bottom status line: dim, clipped to the screen width. */
    private void hint(Graphics g, String s) {
        g.setColor(COLOR_BAR);
        g.fillRect(0, viewH - 14, viewW, 14);
        g.setColor(COLOR_DIM);
        boldFlag = false;
        txt(g, TinyFont.ellipsize(s, viewW - 8), 4, viewH - 12, Graphics.LEFT | Graphics.TOP);
    }

    /** Draws UI text with the built-in bitmap font (no dependency on device fonts). */
    private static void txt(Graphics g, String s, int x, int y, int anchor) {
        TinyFont.draw(g, s, x, y, anchor, boldFlag);
    }

    private String screenTitle() {
        switch (mode) {
            case MODE_PAUSE: return "МЕНЮ";
            case MODE_SETTINGS: return "НАСТРОЙКИ";
            case MODE_HELP: return "СПРАВКА";
            case MODE_DEMOS: return "ДЕМО";
            case MODE_GAME: return "ИГРА";
            case MODE_BOOT: return "ЗАГРУЗКА";
            case MODE_MESSAGE: return "СООБЩЕНИЕ";
            default: return "Sony PlayStation";
        }
    }

    private void paintBoot(Graphics g, Font bold, Font plain) {
        /* replaced by paintMessage() during boots; kept for the * hotkey */
        int cy = viewH / 2;
        g.setColor(COLOR_ACCENT);
        boldFlag = true;
        txt(g, "Поиск карты памяти", viewW / 2, cy - 20, Graphics.HCENTER | Graphics.BOTTOM);
        g.setColor(COLOR_DIM);
        boldFlag = false;
        txt(g, "без запросов безопасности", viewW / 2, cy, Graphics.HCENTER | Graphics.TOP);
    }

    private void paintMessage(Graphics g, Font plain, int barH) {
        drawWrapped(g, status, 10, barH + 20, viewW - 20, plain, COLOR_TEXT);
        hint(g, "* - снова  5 - демо  0 - выход");
    }

    /** Help text wrapped to the current screen width (the font is monospaced). */
    private String[] helpLines() {
        int maxChars = (viewW - 12) / TinyFont.WIDTH;
        if (maxChars < 12) maxChars = 12;
        Vector out = new Vector();
        for (int i = 0; i < HELP_TEXT.length; i++) {
            String line = HELP_TEXT[i];
            boolean head = line.length() > 0 && line.charAt(0) != '-'
                    && line.charAt(0) != ' ' && line.length() < 24;
            boolean first = true;
            while (line.length() > maxChars) {
                int cut = line.lastIndexOf(' ', maxChars);
                if (cut <= 0) cut = maxChars;
                out.addElement((first && head ? "\u0001" : "  ") + line.substring(0, cut));
                line = line.substring(cut < line.length() ? cut + 1 : cut);
                first = false;
            }
            out.addElement((first && head ? "\u0001" : "") + line);
        }
        String[] res = new String[out.size()];
        for (int i = 0; i < res.length; i++) res[i] = (String) out.elementAt(i);
        return res;
    }

    private void paintHelp(Graphics g, Font bold, Font plain, int barH) {
        int top = barH + 6;
        int lineH = TinyFont.HEIGHT + 2;
        int lines = (viewH - top - 20) / lineH;
        if (lines < 1) lines = 1;
        String[] text = helpLines();
        int maxScroll = text.length - lines;
        if (helpScroll > maxScroll) helpScroll = maxScroll;
        if (helpScroll < 0) helpScroll = 0;
        boldFlag = false;
        for (int i = 0; i < lines; i++) {
            int idx = helpScroll + i;
            if (idx >= text.length) break;
            String line = text[idx];
            boolean head = line.length() > 0 && line.charAt(0) == '\u0001';
            if (head) line = line.substring(1);
            g.setColor(head ? COLOR_ACCENT : COLOR_TEXT);
            boldFlag = head;
            txt(g, line, 6, top + i * lineH, Graphics.LEFT | Graphics.TOP);
        }
        int percent = (int) ((helpScroll * 100L) / Math.max(1, maxScroll));
        hint(g, "2/8 - листать " + percent + "%   0 - назад");
    }

    private void paintSettings(Graphics g, Font bold, Font plain, int barH) {
        int top = barH + 10;
        int rowH = TinyFont.HEIGHT + 10;
        for (int i = 0; i < SETTINGS_ITEMS.length; i++) {
            int y = top + i * rowH;
            if (i == settingsSelection) {
                g.setColor(COLOR_SEL);
                g.fillRect(2, y - 2, viewW - 4, rowH);
            }
            g.setColor(i == settingsSelection ? 0xFFFFFF : COLOR_TEXT);
            boldFlag = false;
            String v = settingValue(i);
            int valueW = v.length() > 0 ? TinyFont.stringWidth(v) + 8 : 0;
            txt(g, TinyFont.ellipsize(SETTINGS_ITEMS[i], viewW - 24 - valueW),
                    10, y + 2, Graphics.LEFT | Graphics.TOP);
            if (v.length() > 0) {
                g.setColor(i == settingsSelection ? 0xDBEAFE : COLOR_ACCENT);
                txt(g, v, viewW - 10, y + 2, Graphics.RIGHT | Graphics.TOP);
            }
        }
        hint(g, "5/* - изменить   0 - назад");
    }

    private void paintDemos(Graphics g, Font bold, Font plain, int barH) {
        int top = barH + 6;
        int rowH = 21;
        for (int i = 0; i <= DEMO_RES.length; i++) {
            int y = top + i * rowH;
            boolean sel = i == demoSelection;
            if (sel) {
                g.setColor(COLOR_SEL);
                g.fillRect(4, y - 4, viewW - 8, rowH);
            }
            boldFlag = true;
            g.setColor(sel ? 0xFFFFFF : COLOR_GAME);
            String title = i < DEMO_RES.length ? DEMO_NAME[i] : "Назад";
            txt(g, title, 12, y, Graphics.LEFT | Graphics.TOP);
            if (i < DEMO_RES.length) {
                boldFlag = false;
                g.setColor(sel ? 0xDBEAFE : COLOR_DIM);
                txt(g, TinyFont.ellipsize(DEMO_DESC[i], viewW - 26), 20, y + 10,
                        Graphics.LEFT | Graphics.TOP);
            }
        }
        hint(g, "5 - запустить   0 - назад");
    }

    private void paintList(Graphics g, Font bold, Font plain, int barH,
                           String[] items, int selection, String[] values, String hint) {
        int top = barH + 20;
        int rowH = TinyFont.HEIGHT + 14;
        for (int i = 0; i < items.length; i++) {
            int y = top + i * rowH;
            if (i == selection) {
                g.setColor(COLOR_SEL);
                g.fillRect(8, y - 4, viewW - 16, rowH);
            }
            g.setColor(i == selection ? 0xFFFFFF : COLOR_TEXT);
            boldFlag = true;
            String val = values != null ? values[i] : null;
            int valueW = val != null ? TinyFont.stringWidth(val) + 8 : 0;
            txt(g, TinyFont.ellipsize(items[i], viewW - 32 - valueW), 18, y,
                    Graphics.LEFT | Graphics.TOP);
            if (values != null && values[i] != null) {
                g.setColor(COLOR_ACCENT);
                boldFlag = false;
                txt(g, values[i], viewW - 14, y + 2, Graphics.RIGHT | Graphics.TOP);
            }
        }
        hint(g, hint);
    }

    private void paintBrowser(Graphics g, Font bold, Font plain, int barH) {
        int rowH = TinyFont.HEIGHT + 10;
        int listTop = barH + TinyFont.HEIGHT + 12;
        int listH = viewH - listTop - 22;

        g.setColor(COLOR_DIM);
        boldFlag = false;
        txt(g, currentTitle, 6, barH + 4, Graphics.LEFT | Graphics.TOP);

        int visible = listH / rowH;
        if (visible < 1) visible = 1;
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + visible) scroll = selected - visible + 1;

        if (entries.size() == 0) {
            g.setColor(COLOR_TEXT);
            txt(g, "Здесь нет файлов игр.", 10, listTop, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_DIM);
            txt(g, "Меню - встроенные демо и настройки.", 10, listTop + rowH,
                    Graphics.LEFT | Graphics.TOP);
        }

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
            boldFlag = true;
            String tag = e.isDir ? "[D]" : (e.game ? "[G]" : "   ");
            txt(g, tag, 8, y + 2, Graphics.LEFT | Graphics.TOP);
            g.setColor(idx == selected ? 0xFFFFFF : COLOR_TEXT);
            boldFlag = false;
            String name = e.name;
            int maxW = viewW - 60;
            while (TinyFont.stringWidth(name) > maxW && name.length() > 3) {
                name = name.substring(0, name.length() - 4) + "...";
            }
            txt(g, name, 34, y + 2, Graphics.LEFT | Graphics.TOP);
            if (e.game && e.size > 0) {
                g.setColor(COLOR_DIM);
                txt(g, sizeText(e.size), viewW - 6, y + 2, Graphics.RIGHT | Graphics.TOP);
            }
        }

        hint(g, "5 - открыть  0 - вверх  меню - демо");
    }

    /* ---- game screen ------------------------------------------------------ */

    private void paintGame(Graphics g) {
        g.setColor(0x000000);
        g.fillRect(0, 0, viewW, viewH);
        if (machine == null) return;

        int[] frame = machine.getFrame();
        int srcW = machine.getFrameWidth();
        int srcH = machine.getFrameHeight();
        if (frame == null || srcW <= 0 || srcH <= 0) return;

        int[] rect = destRect();
        int dstX = rect[0], dstY = rect[1], dstW = rect[2], dstH = rect[3];
        if (dstW <= 0 || dstH <= 0) return;

        if (dstW == srcW && dstH == srcH) {
            try {
                g.drawRGB(frame, 0, srcW, dstX, dstY, srcW, srcH, false);
            } catch (Throwable t) {
                drawFrameFallback(g, frame, srcW, dstX, dstY, dstW, dstH);
            }
        } else {
            if (columnMap == null || mapSrcW != srcW || mapSrcH != srcH
                    || mapDstW != dstW || mapDstH != dstH) {
                buildMaps(srcW, srcH, dstW, dstH);
            }
            int[] dst = scaledFrame;
            for (int y = 0; y < dstH; y++) {
                int srcRow = rowMap[y] * srcW;
                int dstRow = y * dstW;
                for (int x = 0; x < dstW; x++) {
                    dst[dstRow + x] = frame[srcRow + columnMap[x]];
                }
            }
            try {
                g.drawRGB(dst, 0, dstW, dstX, dstY, dstW, dstH, false);
            } catch (Throwable t) {
                drawFrameFallback(g, dst, dstW, dstX, dstY, dstW, dstH);
            }
        }

        boldFlag = false;
        if (config.showFps) {
            g.setColor(0x000000);
            g.fillRect(0, 0, 74, TinyFont.HEIGHT + 2);
            g.setColor(COLOR_ACCENT);
            txt(g, fps + " fps", 3, 1, Graphics.LEFT | Graphics.TOP);
        }
        if (paused) {
            g.setColor(0x000000);
            g.fillRect(viewW / 2 - 34, viewH / 2 - 8, 68, 16);
            g.setColor(0xFFFFFF);
            txt(g, "ПАУЗА", viewW / 2, viewH / 2 - 7, Graphics.HCENTER | Graphics.TOP);
        }
        if (!sawOutput && machine != null && machine.framesRun < 240) {
            g.setColor(0x000000);
            g.fillRect(0, viewH - 26, viewW, 26);
            g.setColor(COLOR_ACCENT);
            txt(g, "PS1J2ME: выполняется " + machine.framesRun + " кадр(ов)",
                    viewW / 2, viewH - 24, Graphics.HCENTER | Graphics.TOP);
            g.setColor(COLOR_DIM);
            txt(g, "старт эмуляции", viewW / 2, viewH - 12, Graphics.HCENTER | Graphics.TOP);
        }
        if (overlayMessage != null && System.currentTimeMillis() < overlayUntil) {
            g.setColor(0x000000);
            g.fillRect(0, viewH - 16, viewW, 16);
            g.setColor(COLOR_TEXT);
            txt(g, overlayMessage, 4, viewH - 15, Graphics.LEFT | Graphics.TOP);
        } else if (overlayMessage != null && System.currentTimeMillis() >= overlayUntil) {
            overlayMessage = null;
        }
    }

    /**
     * Last-resort renderer for devices whose drawRGB() refuses our buffer:
     * paints 4x4 colour blocks with fillRect(). Slow, but never black.
     */
    private void drawFrameFallback(Graphics g, int[] frame, int scan,
                                   int dstX, int dstY, int dstW, int dstH) {
        int step = 4;
        for (int y = 0; y < dstH; y += step) {
            int row = (y * frame.length / dstH / scan) * scan;
            for (int x = 0; x < dstW; x += step) {
                int src = row + (x * 320 / dstW);
                if (src >= frame.length) break;
                g.setColor(frame[src] & 0xFFFFFF);
                g.fillRect(dstX + x, dstY + y, step, step);
            }
        }
    }

    private final int[] rectCache = new int[4];

    /** Destination rectangle for the emulator image, honouring the scale mode. */
    private int[] destRect() {
        int srcW = 320, srcH = 240;
        int w, h, x, y;
        switch (config.scaleMode) {
            case Config.SCALE_STRETCH:
                w = viewW;
                h = viewH;
                x = 0;
                y = 0;
                break;
            case Config.SCALE_1_1:
                w = Math.min(srcW, viewW);
                h = Math.min(srcH, viewH);
                x = (viewW - w) / 2;
                y = (viewH - h) / 2;
                break;
            default: {
                /* 4:3 letterbox */
                int availW = viewW;
                int availH = viewH;
                if (availW * 3 <= availH * 4) {
                    w = availW;
                    h = (availW * 3) / 4;
                } else {
                    h = availH;
                    w = (availH * 4) / 3;
                }
                x = (viewW - w) / 2;
                y = (viewH - h) / 2;
                break;
            }
        }
        rectCache[0] = x;
        rectCache[1] = y;
        rectCache[2] = w;
        rectCache[3] = h;
        return rectCache;
    }

    private void buildMaps(int srcW, int srcH, int dstW, int dstH) {
        columnMap = new int[dstW];
        rowMap = new int[dstH];
        for (int x = 0; x < dstW; x++) columnMap[x] = (x * srcW) / dstW;
        for (int y = 0; y < dstH; y++) rowMap[y] = (y * srcH) / dstH;
        scaledFrame = new int[dstW * dstH];
        mapSrcW = srcW;
        mapSrcH = srcH;
        mapDstW = dstW;
        mapDstH = dstH;
    }

    private void drawWrapped(Graphics g, String text, int x, int y, int maxW, Font f, int color) {
        if (text == null) return;
        boldFlag = false;
        g.setColor(color);
        int lineH = TinyFont.HEIGHT + 2;
        int start = 0;
        int i = 0;
        while (i < text.length()) {
            if (TinyFont.stringWidth(text.substring(start, i + 1)) > maxW) {
                int cut = text.lastIndexOf(" ", i);
                if (cut <= start) cut = i;
                txt(g, text.substring(start, cut), x, y, Graphics.LEFT | Graphics.TOP);
                y += lineH;
                start = cut + 1;
                if (start > text.length()) break;
            }
            i++;
        }
        if (start < text.length()) txt(g, text.substring(start), x, y, Graphics.LEFT | Graphics.TOP);
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
        /* Deliberately does not pause: several devices deliver hideNotify()
           during start-up, which used to leave the game frozen forever. The
           player pauses explicitly from the menu. */
        repaint();
    }

    protected void showNotify() {
        repaint();
    }

    public void shutdown() {
        running = false;
        if (audio != null) {
            audio.stop();
            audio = null;
        }
        if (cd != null) {
            cd.close();
            cd = null;
        }
    }
}
