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
    private static final int MODE_INFO = 8;

    public static final String VERSION = "1.7.2";

    /* CHD/MDF/NRG need a decompressor or a proprietary header: they are not
       listed as games so a tap on them cannot fail with an empty screen. */
    private static final String[] GAME_EXT = { ".bin", ".iso", ".img", ".cue" };

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
        "PS1J2ME - эмулятор Sony",
        "PlayStation 1 для Java ME",
        "(CLDC 1.1 / MIDP 2.0).",
        "",
        "КАК ЗАПУСТИТЬ ИГРУ С КАРТЫ",
        "",
        "",
        "1. Вставьте карту в",
        "телефон.",
        "",
        "2. Скопируйте образ игры в",
        "папку games: пара GAME.BIN",
        "+ GAME.CUE или один файл",
        ".iso / .img.",
        "",
        "3. Откройте эмулятор.",
        "Сначала покажется список",
        "устройств: карта памяти",
        "(E:) и память телефона",
        "(C:). Выберите карту.",
        "",
        "4. Войдите в папку с игрой",
        "и нажмите 5.",
        "Файл .cue запускается",
        "сразу: имя образа берётся",
        "из листа, даже если там",
        "путь вида",
        "D:\\GAMES\\GAME.BIN.",
        "",
        "БРАУЗЕР ФАЙЛОВ",
        "",
        "5 - открыть папку или",
        "запустить игру. 0 - на",
        "уровень выше, с корня - в",
        "меню. * - обновить список,",
        "# - показать все файлы.",
        "Левый софткей - меню,",
        "правый - эта справка. Папки",
        "помечены [D], игры [G],",
        "устройства [*].",
        "Если карта не видна,",
        "нажмите * на экране",
        "сообщения: поиск устройств",
        "повторится.",
        "",
        "УПРАВЛЕНИЕ В ИГРЕ",
        "",
        "2/4/6/8 или джойстик -",
        "крестовина. 5 - Cross, 1 -",
        "Square, 3 - Triangle, 0 -",
        "Circle. 7/9 - L1/R1, * -",
        "Start, # - Select.",
        "Левый софткей - меню,",
        "правый - звук вкл/выкл,",
        "средний работает как Cross.",
        "",
        "ЕСЛИ КНОПКИ НЕ РАБОТАЮТ",
        "",
        "Настройки - Раскладка",
        "кнопок. Три варианта:",
        "стандарт - обычная",
        "раскладка; крест/круг -",
        "Cross и Circle поменяны",
        "местами; только цифры -",
        "джойстик отключён, работает",
        "блок цифр, помогает на MTK",
        "и китайских телефонах с",
        "кривыми кодами.",
        "В меню кнопка 5 всегда",
        "подтверждает, 0 всегда",
        "назад, поэтому раскладка не",
        "мешает навигации.",
        "",
        "ВОЗМОЖНОСТИ",
        "",
        "- MIPS R3000A: ленивый кэш",
        "декодированных блоков",
        "CodeBlock[]",
        "- GTE (COP2):",
        "RTPS/RTPT/NCLIP/AVSZ/MVMVA",
        "и световые команды, всё в",
        "fixed-point, без float",
        "- GPU: программный",
        "растеризатор, VRAM 1 МиБ,",
        "текстуры 4/8/15 бит,",
        "полупрозрачность, спрайты,",
        "линии",
        "- SPU: 24 ADPCM-голоса,",
        "ADSR, стерео 44.1 кГц",
        "- CD-ROM: образы 300-700 МБ",
        "читаются потоком и через",
        "LRU-кэш секторов, без",
        "OutOfMemoryError",
        "- BIOS из",
        "/res/roms/scph1001.bin",
        "через getResourceAsStream,",
        "без образа - HLE-загрузка",
        "(SYSTEM.CNF -> PS-X EXE)",
        "",
        "НАСТРОЙКИ",
        "",
        "Масштаб: на весь экран, 4:3",
        "с полями или 1:1 пиксель.",
        "Пропуск кадров ускоряет 3D",
        "на слабых телефонах.",
        "Показывать все файлы -",
        "браузер не скрывает не-",
        "игры. Настройки сохраняются",
        "в RMS вместе с последней",
        "папкой.",
        "",
        "ДЕМО",
        "",
        "В JAR встроены семь демо:",
        "Pong, тест геймпада,",
        "фигуры, текстуры, логотип,",
        "3D-куб и звук. Они не",
        "требуют файлов на карте.",
        "Pong и тест пада проверяют",
        "ввод: если кнопки не",
        "работают, смените раскладку",
        "в настройках.",
        "",
        "ЗАГРУЗКА",
        "",
        "Загрузка игры и звука идёт",
        "в фоне, поэтому меню не",
        "подвисает. Если кадр",
        "считается дольше 800 мс,",
        "эмулятор сам включает",
        "пропуск кадров. Если звук",
        "недоступен, появляется",
        "сообщение, а игра",
        "продолжается.",
        "",
        "ТРЕБОВАНИЯ",
        "",
        "Куча MIDlet: около 5 МиБ.",
        "JSR-75 нужен для игр с",
        "карты. Звук работает, если",
        "устройство умеет PCM через",
        "MMAPI. J2ME Loader: сборка",
        "запускается без запросов",
        "прав, карта хоста видна как",
        "/sdcard.",
        "",
        "ОГРАНИЧЕНИЯ",
        "",
        "MDEC (видеоролики) не",
        "декодируется, реверберация",
        "SPU и CD-DA отсутствуют,",
        "карты памяти не",
        "эмулируются, CHD/MDF не",
        "читаются.",
        "",
        "БИОС",
        "",
        "Настоящий SCPH1001.BIN",
        "защищён авторским правом и",
        "в репозиторий не входит.",
        "Положите свой дамп в",
        "res/roms/scph1001.bin и",
        "пересоберите: эмулятор сам",
        "определит настоящий BIOS и",
        "запустит его.",
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
    /** Storage roots found on this handset (see FileRoots). */
    private Vector roots = new Vector();
    /**
     * True once the background memory card scan has finished one way or the
     * other. The UI renders before the scan returns otherwise, which used to
     * make screenshots (and the first frame on a slow phone) flip between the
     * browser and the "no card" message.
     */
    private boolean discovered;
    /** True while the browser shows the list of storage devices. */
    private boolean showingRoots;
    /** Set while the settings screen re-reads the folder: keeps MODE_SETTINGS. */
    private boolean keepModeOnList;
    /** Games found in the current folder, for the header counter. */
    private int gameCountNow;
    /** True when the folder had more entries than MAX_ENTRIES. */
    private boolean listTruncated;
    private static final int MAX_ENTRIES = 512;
    private int fps;
    private int framesThisSecond;
    private long lastFpsTime;
    private String overlayMessage;
    private long overlayUntil;
    private volatile boolean booting;
    private volatile boolean sawOutput;      /* at least one non black pixel */
    private volatile boolean rawTried;       /* VRAM-origin scanout attempted */
    private volatile boolean hintShown;
    private volatile String lastError;
    private long audioStartedAt;
    private int errorCount;

    /* ---- settings / help / demos ---------------------------------------- */
    private final Config config = Config.get();
    private int settingsSelection;
    private int helpScroll;
    private int demoSelection;
    private int pauseSelection;
    private int settingsScroll;
    private int infoScroll;
    private String[] infoLines;

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
            discover();
        } finally {
            discovered = true;
        }
    }

    /** Used by the desktop tests to wait for the background scan. */
    public boolean isDiscovered() { return discovered; }

    private void discover() {
        try {
            roots = FileRoots.discoverRoots();
            if (roots.size() == 0) {
                showingRoots = false;
                currentUrl = null;
                status = "Карта памяти не найдена. Нажмите * для повтора, 5 - встроенные демо.";
                mode = MODE_MESSAGE;
                repaint();
                return;
            }
            /* Re-open the folder the player used last time when it still exists. */
            String last = config.lastUrl;
            if (last != null && last.length() > 0 && FileRoots.isUsable(last)) {
                rootUrl = (String) ((FileRoots.Root) roots.elementAt(0)).url;
                listDirectory(last, nameOf(last));
                return;
            }
            if (roots.size() == 1) {
                FileRoots.Root r = (FileRoots.Root) roots.elementAt(0);
                rootUrl = r.url;
                listDirectory(r.url, r.label);
                return;
            }
            openRoots();
        } catch (Throwable t) {
            showingRoots = false;
            currentUrl = null;
            status = "Ошибка доступа к файловой системе: " + t.toString();
            mode = MODE_MESSAGE;
            repaint();
        }
    }

    /** Shows the list of storage devices (card, phone memory, ...). */
    private void openRoots() {
        entries.removeAllElements();
        selected = 0;
        scroll = 0;
        for (int i = 0; i < roots.size(); i++) {
            FileRoots.Root r = (FileRoots.Root) roots.elementAt(i);
            Entry e = new Entry();
            e.name = r.label;
            e.url = r.url;
            e.isDir = true;
            e.root = true;
            e.size = -1;
            entries.addElement(e);
        }
        showingRoots = true;
        currentUrl = null;
        currentTitle = "Устройства";
        mode = MODE_BROWSER;
        status = entries.size() + " устройств(а)";
        repaint();
    }

    /** Last path segment, used as a folder title. */
    private static String nameOf(String url) {
        if (url == null) return "/";
        String u = url;
        if (u.endsWith("/")) u = u.substring(0, u.length() - 1);
        int slash = u.lastIndexOf('/');
        String n = slash >= 0 ? u.substring(slash + 1) : u;
        return n.length() == 0 ? "/" : n;
    }

    private void listDirectory(String url, String title) {
        entries.removeAllElements();
        selected = 0;
        scroll = 0;
        showingRoots = false;
        boolean ok = false;
        int gameCount = 0;
        listTruncated = false;
        gameCountNow = 0;
        try {
            FileConnection dir = (FileConnection) Connector.open(url, Connector.READ);
            try {
                Enumeration list = null;
                try {
                    list = dir.list();
                } catch (Throwable t) {
                    /* some firmwares need the explicit wildcard variant */
                    list = dir.list("*", false);
                }
                Vector dirs = new Vector();
                Vector games = new Vector();
                Vector others = new Vector();
                String base = url.endsWith("/") ? url : url + "/";
                int total = 0;
                while (list != null && list.hasMoreElements()) {
                    if (total >= MAX_ENTRIES) {
                        /* a card can hold thousands of files: say so instead of
                           silently showing a short list */
                        listTruncated = true;
                        break;
                    }
                    String raw = (String) list.nextElement();
                    if (raw == null) continue;
                    total++;
                    if (raw.equals(".") || raw.equals("..")) continue;
                    if (raw.startsWith(".")) continue;          /* hidden / system */
                    boolean isDir = raw.endsWith("/") || raw.endsWith("\\");
                    String name = isDir ? raw.substring(0, raw.length() - 1) : raw;
                    if (name.length() == 0) continue;
                    Entry e = new Entry();
                    e.name = name;
                    e.isDir = isDir;
                    e.url = base + raw;
                    if (!isDir && name.indexOf('.') < 0) {
                        /* no extension: a few phones do not append the slash */
                        e.isDir = isDirectory(e.url);
                    }
                    if (e.isDir) {
                        isDir = true;
                        dirs.addElement(e);
                    } else if (isGame(name)) {
                        e.game = true;
                        e.size = fileSize(e.url);
                        games.addElement(e);
                    } else if (config.showAllFiles) {
                        e.size = fileSize(e.url);
                        others.addElement(e);
                    }
                }
                addSorted(dirs);
                addSorted(games);
                addSorted(others);
                gameCount = games.size();
                gameCountNow = gameCount;
                ok = true;
            } finally {
                dir.close();
            }
            currentUrl = baseOf(url);
            currentTitle = title;
            rootUrl = rootOf(currentUrl);
            showingRoots = false;
            if (!keepModeOnList) mode = MODE_BROWSER;
            if (entries.size() == 0) {
                status = "Папка пуста";
            } else {
                status = entries.size() + " файлов, игр: " + gameCount;
            }
            /* keep the last folder, but do not hit RMS on every repaint */
            if (config.lastUrl == null || !config.lastUrl.equals(currentUrl)) {
                config.lastUrl = currentUrl;
                config.save();
            }
        } catch (Throwable t) {
            if (!ok) {
                status = "Не удалось открыть: " + title;
                mode = MODE_MESSAGE;
            }
        }
        repaint();
    }

    private static String baseOf(String url) {
        return url.endsWith("/") ? url : url + "/";
    }

    private static String rootOf(String url) {
        if (url == null) return null;
        int scheme = url.indexOf("://");
        if (scheme < 0) return url;
        int slash = url.indexOf('/', scheme + 3);
        return slash < 0 ? url : url.substring(0, slash + 1);
    }

    private static boolean isDirectory(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            return fc.isDirectory();
        } catch (Throwable t) {
            return false;
        } finally {
            FileRoots.close(fc);
        }
    }

    private static long fileSize(String url) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(url, Connector.READ);
            return fc.fileSize();
        } catch (Throwable t) {
            return -1;
        } finally {
            FileRoots.close(fc);
        }
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
        boolean root;
        long size;
    }

    /* ==================================================================== */
    /*  input                                                               */
    /* ==================================================================== */

    protected void keyPressed(int keyCode) {
        if (mode == MODE_GAME) {
            gameKey(keyCode, translate(keyCode), true);
            return;
        }
        /* Every other screen uses the layout independent translation: "5" must
           confirm and "0" must go back in all three pad layouts, otherwise the
           "cross/circle" layout turns "0" into "launch the selected game". */
        int act = translateUi(keyCode);
        switch (mode) {
            case MODE_BROWSER: browserKey(keyCode, act); break;
            case MODE_PAUSE: pauseKey(keyCode, act); break;
            case MODE_SETTINGS: settingsKey(keyCode, act); break;
            case MODE_HELP: helpKey(keyCode, act); break;
            case MODE_DEMOS: demoKey(keyCode, act); break;
            case MODE_INFO: infoKey(keyCode, act); break;
            default:
                if (act == KeyMap.START || keyCode == KEY_STAR) {
                    if (booting) {
                        /* a second search would race with the running boot */
                        overlay("Загрузка уже идёт, подождите...");
                    } else {
                        stopGame();
                        mode = MODE_BOOT;
                        status = "Поиск карты памяти...";
                        repaint();
                        new Thread(this, "ps1-scan").start();
                    }
                } else if (uiFire(keyCode, act)) {
                    openDemos();
                }
                break;
        }
    }

    protected void keyReleased(int keyCode) {
        /* Releases must be delivered even when the pause menu is open: a key
           held while the menu appeared would otherwise stay pressed on the pad
           and the character would run away after "Продолжить игру". */
        if (machine == null) return;
        gameKey(keyCode, translate(keyCode), false);
    }

    /** Menu translation: same keys, but the pad layout is not applied. */
    private int translateUi(int keyCode) {
        return translateAs(keyCode, KeyMap.LAYOUT_STANDARD);
    }

    /** Translates a raw MIDP key code into a KeyMap action, never throwing. */
    private int translate(int keyCode) {
        return translateAs(keyCode, config.keyLayout);
    }

    private int translateAs(int keyCode, int layout) {
        int action = 0;
        try {
            action = getGameAction(keyCode);
        } catch (Throwable t) {
            /* some firmwares throw for unknown codes */
        }
        try {
            return KeyMap.action(keyCode, action, layout);
        } catch (Throwable t) {
            return KeyMap.NONE;
        }
    }

    /* Menu navigation deliberately ignores the game pad layout: "5" always
       confirms and "0" always goes back, so a player who swapped Cross and
       Circle for a game still finds the menus where they left them. */

    private static boolean uiUp(int keyCode, int act) {
        return KeyMap.isUp(act) || keyCode == KEY_NUM2;
    }

    private static boolean uiDown(int keyCode, int act) {
        return KeyMap.isDown(act) || keyCode == KEY_NUM8;
    }

    private static boolean uiFire(int keyCode, int act) {
        return act == KeyMap.CROSS || act == KeyMap.OK || act == KeyMap.TRIANGLE
                || keyCode == KEY_NUM5 || keyCode == KEY_FIRE;
    }

    private static boolean uiBack(int keyCode, int act) {
        return KeyMap.isBack(act) || act == KeyMap.SOUND
                || keyCode == KEY_NUM0 || keyCode == KEY_SOFTKEY2;
    }

    private void browserKey(int keyCode, int act) {
        int size = entries.size();
        if (uiUp(keyCode, act)) {
            if (size > 0) selected = (selected + size - 1) % size;
            repaint();
        } else if (uiDown(keyCode, act)) {
            if (size > 0) selected = (selected + 1) % size;
            repaint();
        } else if (uiFire(keyCode, act)) {
            activate();
        } else if (act == KeyMap.MENU) {
            openPauseMenu();
        } else if (act == KeyMap.SOUND) {
            helpScroll = 0;
            mode = MODE_HELP;
            repaint();
        } else if (act == KeyMap.START) {
            /* refresh the current folder (or rescan the devices) */
            if (showingRoots) rescanRoots();
            else listDirectory(currentUrl, currentTitle);
        } else if (act == KeyMap.SELECT) {
            /* non-game files stay hidden unless explicitly asked for */
            config.showAllFiles = !config.showAllFiles;
            config.save();
            if (!showingRoots && currentUrl != null) listDirectory(currentUrl, currentTitle);
            else repaint();
        } else if (uiBack(keyCode, act)) {
            goUp();
        }
    }

    /** Re-runs storage discovery (card inserted/removed while running). */
    private void rescanRoots() {
        Vector found = FileRoots.discoverRoots();
        if (found.size() == 0) {
            overlay("Карта памяти не найдена");
            repaint();
            return;
        }
        roots = found;
        openRoots();
    }

    private void activate() {
        if (selected < 0 || selected >= entries.size()) return;
        Entry e = (Entry) entries.elementAt(selected);
        if (e.isDir) {
            listDirectory(e.url, e.name);
        } else if (e.game) {
            startFileGame(e);
        } else {
            /* shown only in the "all files" mode: never stay silent */
            overlay("Это не образ игры: " + e.name);
        }
    }

    private void goUp() {
        if (showingRoots || currentUrl == null) {
            openPauseMenu();
            return;
        }
        for (int i = 0; i < roots.size(); i++) {
            if (currentUrl.equals(((FileRoots.Root) roots.elementAt(i)).url)) {
                leaveRootFolder();
                return;
            }
        }
        String parent = FileRoots.parent(currentUrl);
        if (parent == null || parent.equals(currentUrl)) {
            leaveRootFolder();
            return;
        }
        if (rootUrl != null && parent.equals(rootUrl)) {
            leaveRootFolder();
            return;
        }
        listDirectory(parent, nameOf(parent));
    }

    /** Goes one level above a device: the picker, or the menu if there is one. */
    private void leaveRootFolder() {
        if (roots.size() > 1) openRoots();
        else openPauseMenu();
    }

    /* ---- pause menu ------------------------------------------------------ */

    private static final String[] PAUSE_ITEMS = {
        "Продолжить игру",
        "Сброс консоли",
        "Настройки",
        "Демо-программы",
        "Справка",
        "Диагностика",
        "Сменить папку с играми",
        "Выход в браузер",
    };

    private void openPauseMenu() {
        pauseSelection = 0;
        releasePad();
        if (mode == MODE_GAME) paused = true;
        mode = MODE_PAUSE;
        repaint();
    }

    private void pauseKey(int keyCode, int action) {
        if (uiUp(keyCode, action)) {
            pauseSelection = (pauseSelection + PAUSE_ITEMS.length - 1) % PAUSE_ITEMS.length;
            repaint();
        } else if (uiDown(keyCode, action)) {
            pauseSelection = (pauseSelection + 1) % PAUSE_ITEMS.length;
            repaint();
        } else if (uiFire(keyCode, action)) {
            switch (pauseSelection) {
                case 0:
                    if (machine != null) {
                        paused = false;
                        mode = MODE_GAME;
                        /* the phone call / task switch may have stopped it */
                        if (config.sound && audio == null) startAudioAsync(machine);
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
                    settingsScroll = 0;
                    mode = MODE_SETTINGS;
                    break;
                case 3:
                    openDemos();
                    break;
                case 4:
                    helpScroll = 0;
                    mode = MODE_HELP;
                    break;
                case 5:
                    infoScroll = 0;
                    infoLines = null;
                    mode = MODE_INFO;
                    break;
                case 6:
                    stopGame();
                    rescanRoots();
                    break;
                default:
                    stopGame();
                    break;
            }
            repaint();
        } else if (uiBack(keyCode, action)) {
            if (machine != null) {
                paused = false;
                mode = MODE_GAME;
            } else if (currentUrl != null) {
                mode = MODE_BROWSER;
            } else {
                mode = MODE_DEMOS;
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
        "Раскладка кнопок",
        "Показывать все файлы",
        "Сбросить настройки",
        "Назад",
    };

    private void settingsKey(int keyCode, int action) {
        if (uiUp(keyCode, action)) {
            settingsSelection = (settingsSelection + SETTINGS_ITEMS.length - 1) % SETTINGS_ITEMS.length;
            repaint();
        } else if (uiDown(keyCode, action)) {
            settingsSelection = (settingsSelection + 1) % SETTINGS_ITEMS.length;
            repaint();
        } else if (uiFire(keyCode, action) || action == KeyMap.TRIANGLE) {
            applySetting();
        } else if (uiBack(keyCode, action)) {
            config.save();
            backFromSettings();
        }
    }

    /** Returns to wherever the settings screen was opened from. */
    private void backFromSettings() {
        if (machine != null) {
            mode = MODE_PAUSE;
        } else if (currentUrl != null || showingRoots) {
            mode = MODE_BROWSER;
        } else {
            mode = MODE_DEMOS;
        }
        repaint();
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
                config.keyLayout = (config.keyLayout + 1) % 3;
                config.padLayout = config.keyLayout == KeyMap.LAYOUT_SWAPPED ? 1 : 0;
                break;
            case 7:
                config.showAllFiles = !config.showAllFiles;
                /* refresh the folder but stay on this screen */
                if (currentUrl != null && !showingRoots) {
                    keepModeOnList = true;
                    try {
                        listDirectory(currentUrl, currentTitle);
                    } finally {
                        keepModeOnList = false;
                    }
                    mode = MODE_SETTINGS;
                }
                break;
            case 8:
                config.reset();
                break;
            default:
                backFromSettings();
                return;
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
            case 6: return config.keyLayoutName();
            case 7: return config.showAllFiles ? "вкл" : "только игры";
            default: return "";
        }
    }

    /* ---- help ------------------------------------------------------------ */

    private void helpKey(int keyCode, int action) {
        int max = HELP_TEXT.length - 1;
        if (uiUp(keyCode, action) || keyCode == KEY_NUM4) {
            helpScroll = Math.max(0, helpScroll - 1);
            repaint();
        } else if (uiDown(keyCode, action) || keyCode == KEY_NUM6) {
            helpScroll = Math.min(max, helpScroll + 1);
            repaint();
        } else if (uiBack(keyCode, action) || uiFire(keyCode, action)) {
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
        if (uiUp(keyCode, action)) {
            demoSelection = (demoSelection + count - 1) % count;
            repaint();
        } else if (uiDown(keyCode, action)) {
            demoSelection = (demoSelection + 1) % count;
            repaint();
        } else if (uiFire(keyCode, action)) {
            if (demoSelection < DEMO_RES.length) startDemo(demoSelection);
        } else if (uiBack(keyCode, action)) {
            mode = MODE_PAUSE;
            repaint();
        }
    }

    /* ---- input in game ---------------------------------------------------- */

    private void gameKey(int keyCode, int act, boolean pressed) {
        if (act == KeyMap.MENU) {
            if (pressed) openPauseMenu();
            return;
        }
        if (act == KeyMap.SOUND) {
            if (pressed) toggleSoundInGame();
            return;
        }
        /* the middle soft key is the fire button on phones that have one */
        if (act == KeyMap.OK) act = KeyMap.CROSS;
        int bit = KeyMap.padBit(act);
        if (bit >= 0) {
            if (pressed) padButtons &= ~(1 << bit);
            else padButtons |= (1 << bit);
        }
        PS1Machine m = machine;
        if (m != null) m.setButtons(padButtons);
    }

    /** Releases every pad button (no stuck directions after pausing). */
    private void releasePad() {
        if (padButtons == 0xFFFF) return;
        padButtons = 0xFFFF;
        PS1Machine m = machine;
        if (m != null) m.setButtons(padButtons);
    }

    /** Soft key 2 during a game: sound on / off without leaving the screen. */
    private void toggleSoundInGame() {
        config.sound = !config.sound;
        config.save();
        if (config.sound) {
            if (machine != null && audio == null) startAudioAsync(machine);
        } else if (audio != null) {
            audio.stop();
            audio = null;
        }
        overlay(config.sound ? "Звук включён" : "Звук выключен");
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
        final String url = e.url;
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
                    /* The bundled image is a header + zeros placeholder: loadBios()
                       detects that and switches the machine to HLE mode. Treat it
                       as "no BIOS", otherwise the CPU executes zeros and the
                       screen stays black forever. */
                    if (real && m.isHleMode()) real = false;
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
        rawTried = false;
        hintShown = false;
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
                if (!a.available) overlay("Звук недоступен");
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
                if (m.halted) {
                    running = false;
                    overlay("Программа завершилась - 0 выход, 5 демо");
                    repaint();
                    break;
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
                if (!sawOutput && (m.framesRun & 15) == 0) {
                    sawOutput = hasVisibleOutput(m);
                    if (!sawOutput && !rawTried && m.framesRun >= 120) {
                        /* The display area may be programmed in a way we do not
                           decode yet: show VRAM from its origin and check again. */
                        rawTried = true;
                        m.setRawScanout(true);
                        sawOutput = hasVisibleOutput(m);
                    }
                    if (!sawOutput && !hintShown && m.framesRun >= 900) {
                        hintShown = true;
                        overlay("Пока нет вывода: кадр " + m.framesRun
                                + ", пикселей GPU " + m.getPixelsWritten());
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
            overlay("Звук недоступен");
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
            case MODE_INFO: paintInfo(g, bold, plain, barH); break;
            default: paintBrowser(g, bold, plain, barH); break;
        }
        paintOverlay(g);
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

    /** Shortens a file name but keeps its extension, which matters for games. */
    private static String shortenName(String name, int maxW) {
        if (TinyFont.stringWidth(name) <= maxW) return name;
        int dot = name.lastIndexOf('.');
        String ext = dot > 0 ? name.substring(dot) : "";
        if (ext.length() > 5) ext = "";
        int keep = maxW / TinyFont.WIDTH - 3 - ext.length();
        if (keep < 3) return TinyFont.ellipsize(name, maxW);
        return name.substring(0, keep) + "..." + ext;
    }

    /** Thin vertical scroll bar shown when a list is longer than the screen. */
    private void scrollBar(Graphics g, int top, int height, int first, int visible, int total) {
        if (total <= visible || height <= 0) return;
        int x = viewW - 3;
        g.setColor(COLOR_BAR);
        g.fillRect(x, top, 3, height);
        int h = height * visible / total;
        if (h < 6) h = 6;
        int span = total - visible;
        int y = top + (height - h) * first / span;
        g.setColor(COLOR_DIM);
        g.fillRect(x, y, 3, h);
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
            case MODE_INFO: return "ДИАГНОСТИКА";
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
        hint(g, "* - снова  5 - демо");
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
        hint(g, "2/8 - листать " + percent + "%");
    }

    /** Short counter shown in the header: "7 игр" or "список обрезан". */
    private String browserCount() {
        if (showingRoots) return "";
        if (listTruncated) return "обрезан";
        int n = gameCountNow;
        if (n <= 0) return "";
        return n + " " + gamesWord(n);
    }

    /** Russian plural for "игра" (1 игра, 2 игры, 5 игр). */
    public static String gamesWord(int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 14) return "игр";
        switch (n % 10) {
            case 1: return "игра";
            case 2: case 3: case 4: return "игры";
            default: return "игр";
        }
    }

    private void paintSettings(Graphics g, Font bold, Font plain, int barH) {
        int top = barH + 10;
        int rowH = TinyFont.HEIGHT + 10;
        int visible = (viewH - 16 - top) / rowH;
        if (visible < 1) visible = 1;
        if (settingsSelection < settingsScroll) settingsScroll = settingsSelection;
        if (settingsSelection >= settingsScroll + visible) settingsScroll = settingsSelection - visible + 1;
        if (settingsScroll > SETTINGS_ITEMS.length - visible) {
            settingsScroll = SETTINGS_ITEMS.length - visible;
        }
        if (settingsScroll < 0) settingsScroll = 0;
        scrollBar(g, top - 2, visible * rowH, settingsScroll, visible, SETTINGS_ITEMS.length);
        for (int i = settingsScroll; i < SETTINGS_ITEMS.length && i < settingsScroll + visible; i++) {
            int y = top + (i - settingsScroll) * rowH;
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
        int visible = (viewH - 18 - top) / rowH;
        if (visible < 1) visible = 1;
        int first = 0;
        if (selection >= visible) first = selection - visible + 1;
        if (first > items.length - visible) first = items.length - visible;
        if (first < 0) first = 0;
        scrollBar(g, top - 2, visible * rowH, first, visible, items.length);
        for (int i = first; i < items.length && i < first + visible; i++) {
            int y = top + (i - first) * rowH;
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

    /* ---- diagnostics ----------------------------------------------------- */

    /** Snapshot of the emulator state, one line per metric. */
    /** Disc layout in words, for the diagnostics screen. */
    private static String layoutName(CdImage image) {
        if (!(image instanceof VirtualCD)) return "демо в RAM";
        switch (((VirtualCD) image).getLayout()) {
            case VirtualCD.LAYOUT_2352_MODE1: return "2352 Mode1";
            case VirtualCD.LAYOUT_2352_MODE2: return "2352 Mode2";
            case VirtualCD.LAYOUT_2336_MODE2: return "2336 Mode2";
            default: return "2048 ISO";
        }
    }

    private String[] buildInfo() {
        Vector v = new Vector();
        PS1Machine m = machine;
        v.addElement("Версия " + VERSION);
        v.addElement("Экран " + viewW + "x" + viewH);
        v.addElement("Память " + (Runtime.getRuntime().freeMemory() / 1024) + " КиБ свободно");
        v.addElement("Режим " + (m == null ? "нет машины"
                : (m.isHleMode() ? "HLE BIOS" : "BIOS из JAR")));
        v.addElement("Кадров " + (m == null ? 0 : m.framesRun) + "  fps " + fps);
        v.addElement("GPU пикселей " + (m == null ? 0 : m.getPixelsWritten()));
        v.addElement("Вывод GPU " + (sawOutput ? "есть" : "нет"));
        v.addElement("Размер кадра " + (m == null ? 0 : m.getFrameWidth())
                + "x" + (m == null ? 0 : m.getFrameHeight()));
        v.addElement("Диск " + (cd == null ? "нет" : cd.getUrl()));
        v.addElement("Секторов " + (cd == null ? 0 : cd.getSectorCount())
                + "  формат " + (cd == null ? "нет" : layoutName(cd)));
        if (cd instanceof VirtualCD) {
            VirtualCD vcd = (VirtualCD) cd;
            if (vcd.getCueUrl() != null) {
                v.addElement("CUE " + nameOf(vcd.getCueUrl()));
                v.addElement("Образ " + vcd.getCueFile());
            }
        }
        v.addElement("Звук " + (audio != null && audio.available ? "есть" : "нет"));
        v.addElement("Масштаб " + config.scaleName() + "  пропуск " + config.frameSkip);
        v.addElement("Кнопки " + config.keyLayoutName());
        if (m != null && m.biosHle != null) {
            v.addElement("BIOS вызовов " + m.biosHle.calls
                    + "  неизвестных " + m.biosHle.unknownCalls);
            String last = m.biosHle.getLastLine();
            if (last.length() > 0) v.addElement("BIOS: " + last);
            String unk = m.biosHle.getLastUnknown();
            if (unk.length() > 0) v.addElement("Не реализовано: " + unk);
        }
        String[] out = new String[v.size()];
        for (int i = 0; i < out.length; i++) out[i] = (String) v.elementAt(i);
        return out;
    }

    private String[] infoWrapped() {
        int maxChars = (viewW - 12) / TinyFont.WIDTH;
        if (maxChars < 12) maxChars = 12;
        String[] src = buildInfo();
        Vector out = new Vector();
        for (int i = 0; i < src.length; i++) {
            String line = src[i];
            while (line.length() > maxChars) {
                int cut = line.lastIndexOf(' ', maxChars);
                if (cut <= 0) cut = maxChars;
                out.addElement(line.substring(0, cut));
                line = line.substring(cut + 1);
                /* wrapped continuations start flush left */
                int lead = 0;
                while (lead < line.length() && line.charAt(lead) == ' ') lead++;
                line = line.substring(lead);
            }
            out.addElement(line);
        }
        String[] res = new String[out.size()];
        for (int i = 0; i < res.length; i++) res[i] = (String) out.elementAt(i);
        return res;
    }

    private void infoKey(int keyCode, int action) {
        String[] lines = infoWrapped();
        int lineH = TinyFont.HEIGHT + 2;
        int visible = (viewH - (TinyFont.HEIGHT + 16) - 20) / lineH;
        if (visible < 1) visible = 1;
        int maxScroll = lines.length - visible;
        if (maxScroll < 0) maxScroll = 0;
        if (uiUp(keyCode, action)) {
            infoScroll -= visible - 1;
            if (infoScroll < 0) infoScroll = 0;
        } else if (uiDown(keyCode, action)) {
            infoScroll += visible - 1;
            if (infoScroll > maxScroll) infoScroll = maxScroll;
        } else if (uiBack(keyCode, action) || uiFire(keyCode, action)) {
            mode = MODE_PAUSE;
        } else if (action == KeyMap.START) {
            infoLines = null;
            infoScroll = 0;
        }
        repaint();
    }

    private void paintInfo(Graphics g, Font bold, Font plain, int barH) {
        int top = barH + 6;
        int lineH = TinyFont.HEIGHT + 2;
        int visible = (viewH - top - 20) / lineH;
        if (visible < 1) visible = 1;
        String[] lines = infoWrapped();
        int maxScroll = lines.length - visible;
        if (maxScroll < 0) maxScroll = 0;
        if (infoScroll > maxScroll) infoScroll = maxScroll;
        if (infoScroll < 0) infoScroll = 0;
        for (int i = 0; i < visible; i++) {
            int idx = infoScroll + i;
            if (idx >= lines.length) break;
            g.setColor(i == 0 ? COLOR_ACCENT : COLOR_TEXT);
            boldFlag = false;
            txt(g, lines[idx], 6, top + i * lineH, Graphics.LEFT | Graphics.TOP);
        }
        hint(g, "2/8 - листать   0 - выход");
    }

    private void paintBrowser(Graphics g, Font bold, Font plain, int barH) {
        int rowH = TinyFont.HEIGHT + 10;
        int listTop = barH + TinyFont.HEIGHT + 12;
        int listH = viewH - listTop - 22;

        g.setColor(COLOR_DIM);
        boldFlag = false;
        String count = browserCount();
        int countW = count.length() > 0 ? TinyFont.stringWidth(count) + 10 : 0;
        txt(g, TinyFont.ellipsize(currentTitle, viewW - 12 - countW), 6, barH + 4,
                Graphics.LEFT | Graphics.TOP);
        if (count.length() > 0) {
            g.setColor(listTruncated ? COLOR_ACCENT : COLOR_DIM);
            txt(g, count, viewW - 6, barH + 4, Graphics.RIGHT | Graphics.TOP);
        }

        int visible = listH / rowH;
        if (visible < 1) visible = 1;
        if (selected < scroll) scroll = selected;
        if (selected >= scroll + visible) scroll = selected - visible + 1;

        if (entries.size() == 0) {
            g.setColor(COLOR_TEXT);
            txt(g, showingRoots ? "Устройства не найдены." : "Здесь нет файлов игр.", 10, listTop,
                    Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_DIM);
            txt(g, showingRoots ? "Нажмите 0 - меню." : "Меню - демо, настройки, справка.", 10,
                    listTop + rowH, Graphics.LEFT | Graphics.TOP);
            if (!showingRoots) {
                txt(g, "Игры кладутся в папку games на карте.", 10, listTop + 2 * rowH,
                        Graphics.LEFT | Graphics.TOP);
            }
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
            g.setColor(e.root ? COLOR_GAME : (e.isDir ? COLOR_DIR : (e.game ? COLOR_GAME : COLOR_TEXT)));
            boldFlag = true;
            String tag = e.root ? "[*]" : (e.isDir ? "[D]" : (e.game ? "[G]" : "   "));
            txt(g, tag, 8, y + 2, Graphics.LEFT | Graphics.TOP);
            g.setColor(idx == selected ? 0xFFFFFF : COLOR_TEXT);
            boldFlag = false;
            String name = e.isDir ? e.name : shortenName(e.name, viewW - 60);
            txt(g, name, 34, y + 2, Graphics.LEFT | Graphics.TOP);
            if (!e.isDir && e.size > 0) {
                g.setColor(COLOR_DIM);
                txt(g, sizeText(e.size), viewW - 6, y + 2, Graphics.RIGHT | Graphics.TOP);
            }
        }

        if (showingRoots) {
            hint(g, "5 - выбрать  0 - меню");
        } else {
            hint(g, "5 - открыть  0 - вверх");
        }
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
        if (!sawOutput && machine != null) {
            g.setColor(0x000000);
            g.fillRect(0, viewH - 26, viewW, 26);
            g.setColor(COLOR_ACCENT);
            boldFlag = false;
            txt(g, "кадр " + machine.framesRun + "  " + fps + " fps  "
                    + machine.getPixelsWritten() + " пикс GPU",
                    viewW / 2, viewH - 24, Graphics.HCENTER | Graphics.TOP);
            g.setColor(COLOR_DIM);
            String note = rawTried ? "показ VRAM напрямую" : "старт эмуляции";
            if (machine.biosHle != null) {
                String line = machine.biosHle.getLastLine();
                if (line.length() > 0) note = line;
            }
            txt(g, TinyFont.ellipsize(note, viewW - 8), viewW / 2, viewH - 12,
                    Graphics.HCENTER | Graphics.TOP);
        }
        paintOverlay(g);
    }

    /** Transient message strip; painted on the game screen and in the menus. */
    private void paintOverlay(Graphics g) {
        if (overlayMessage == null) return;
        if (System.currentTimeMillis() >= overlayUntil) {
            overlayMessage = null;
            return;
        }
        g.setColor(0x000000);
        g.fillRect(0, viewH - 16, viewW, 16);
        g.setColor(COLOR_TEXT);
        boldFlag = false;
        txt(g, TinyFont.ellipsize(overlayMessage, viewW - 8), 4, viewH - 14,
                Graphics.LEFT | Graphics.TOP);
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

    /**
     * Called when the MIDlet is paused by the system (incoming call, task
     * switch, screen lock). The emulation thread is stopped instead of being
     * left to burn the battery in the background, and the pause menu is shown
     * so that returning to the game is a deliberate action.
     */
    public void onAppPause() {
        releasePad();
        if (machine != null && mode == MODE_GAME) {
            paused = true;
            pauseSelection = 0;
            mode = MODE_PAUSE;
        }
        if (audio != null) {
            try { audio.stop(); } catch (Throwable t) { }
            audio = null;
        }
        repaint();
    }

    /** Called when the MIDlet comes back to the foreground. */
    public void onAppResume() {
        /* nothing is resumed automatically: whichever screen was open stays,
           the player decides whether to continue. */
        repaint();
    }

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
        columnMap = null;
        rowMap = null;
        scaledFrame = new int[0];
        System.gc();
    }
}
