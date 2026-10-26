package rpc;

public class Game {
    public boolean pcPowered;
    public int bootFrame;
    public int bootTotalFrames;
    public String bootOsName;
    public int bootOsType;
    public int repairSel;
    public int repairTop;
    public rpc.Game$RepairOrder[] repairOrders;
    final public static String BUILD = "4.8";
    final public static int ACT_UP = 0;
    final public static int ACT_DOWN = 1;
    final public static int ACT_LEFT = 2;
    final public static int ACT_RIGHT = 3;
    final public static int ACT_FIRE = 4;
    final public static int ACT_SOFT1 = 5;
    final public static int ACT_SOFT2 = 6;
    final public static int ACT_BACK = 7;
    final public static int ACT_NUM1 = 8;
    final public static int ACT_NUM2 = 9;
    final public static int ACT_NUM3 = 10;
    final public static int ACT_NUM4 = 11;
    final public static int ACT_NUM5 = 12;
    final public static int ACT_NUM6 = 13;
    final public static int ACT_NUM7 = 14;
    final public static int ACT_NUM8 = 15;
    final public static int ACT_NUM9 = 16;
    final public static int ACT_STAR = 17;
    final public static int ACT_POUND = 18;
    final public static int M_MENU = 0;
    final public static int M_MAP = 1;
    final public static int M_BUILD = 2;
    final public static int M_SHOP = 3;
    final public static int M_RESULT = 4;
    final public static int M_MUSEUM = 5;
    final public static int M_ABOUT = 6;
    final public static int M_CATALOG = 7;
    final public static int M_CATPARTS = 8;
    final public static int M_MARKET = 9;
    final public static int M_OFFERS = 10;
    final public static int M_ORDERS = 11;
    final public static int M_BENCH = 12;
    final public static int M_OS = 13;
    final public static int M_OSPROGS = 14;
    final public static int M_EDITOR = 15;
    final public static int M_OSRUN = 16;
    final public static int M_TASKS = 17;
    final public static int M_NET = 18;
    final public static int M_MINE = 19;
    final public static int M_SETUP = 20;
    final public static int M_OSLIST = 21;
    final public static int M_INSTALL = 22;
    final public static int M_BSOD = 23;
    final public static int M_SPEC = 24;
    final public static int M_WEB = 25;
    final public static int M_GAME = 26;
    final public static int M_POST = 27;
    final public static int M_LANG = 28;
    final public static int M_SHELL = 29;
    final public static int M_BROWSE = 30;
    final public static int M_CORP = 31;
    final public static int M_CHIP = 32;
    final public static int M_CORPOSE = 33;
    final public static int M_SOFT = 34;
    final public static int M_PRODS = 35;
    final public static int M_GAMES = 36;
    final public static int M_WINAMP = 37;
    final public static int M_MINES = 38;
    final public static int M_SNAKE = 39;
    final public static int M_PAINT = 40;
    final public static int M_BENCH3D = 41;
    final public static int M_PASTE = 42;
    final public static int M_BOOTANIM = 43;
    final public static int M_REPAIR = 44;
    final public static int M_EXP_LIST = 45;
    final public static int M_EXP_RUN = 46;
    final public static int M_ROOM = 47;
    final public static int M_CITY = 48;
    final public static int M_WORKSHOP_TOOL = 49;
    final public static int M_FLOPPY_INSTALL = 50;
    final public static int M_DEFRAG = 51;
    final public static int M_EVENT_NEWS = 52;
    final public static int M_TEA = 53;
    final public static int M_SLEEP = 54;
    final public static int C_BG = 923168;
    final public static int C_BG2 = 1318956;
    final public static int C_PANEL = 1780536;
    final public static int C_PANEL2 = 2308170;
    final public static int C_LINE = 3033694;
    final public static int C_TEXT = 15003895;
    final public static int C_DIM = 8297134;
    final public static int C_ACC = 3793530;
    final public static int C_ACC2 = 16763196;
    final public static int C_RED = 16739179;
    final public static int C_BLUE = 5879551;
    private rpc.Db db;
    private rpc.Gfx g;
    private rpc.Host host;
    private int mode;
    private int menuSel;
    private int mapSel;
    private int mapTop;
    private int museumSel;
    private int museumTop;
    private int levelIdx;
    private rpc.Part[] slots;
    private int budget;
    private int spent;
    private boolean sandbox;
    private int shopSlot;
    private int shopSel;
    private int shopTop;
    private int[] shopList;
    private boolean shopAllYears;
    private String toast;
    private long toastUntil;
    private rpc.Score$Result result;
    private long resultStart;
    private int resultPhase;
    private int resultFrames;
    final private int RESULT_FRAMES;
    private int cash;
    private int day;
    private int seed;
    private rpc.Table table;
    private int marketSel;
    private int marketTop;
    private int openMarket;
    private int offerSel;
    private int offerTop;
    private int[] offerPart;
    private int[] offerPrice;
    private int[] offerCond;
    private int[] offerDays;
    private int[] offerDust;
    private int[][] offerBundle;
    private int orderSel;
    private int orderTop;
    private int benchSel;
    private int benchTop;
    private int osSel;
    private int osTop;
    private int progSel;
    private int progTop;
    private int editorLang;
    private int editorSel;
    private int editorTop;
    private String[] editorLines;
    private int editorN;
    private rpc.Os$Prog[] progs;
    private rpc.Os$Task[] tasks;
    private int taskSel;
    private int taskTop;
    private rpc.Vm$Run lastRun;
    private String runTitle;
    private int runBack;
    private int minedTotal;
    private int osIndex;
    private rpc.Inst$Os installingOs;
    private int instTotal;
    private int instLeft;
    private int hackLevel;
    private boolean tpmOn;
    private boolean secureOn;
    private boolean uefiOn;
    private boolean xmpOn;
    private int patches;
    private int setupSel;
    private boolean speakerOn;
    private boolean simBought;
    private int bootSel;
    final private static String[] BOOT;
    private int osListSel;
    private int osListTop;
    private int webSel;
    private int webTop;
    private rpc.Part specPart;
    private int specBack;
    private rpc.Diag$Fault[] faults;
    private int lastFault;
    private boolean doomUnlocked;
    private int unlocked;
    private int[] best;
    private int totalXp;
    final private static int ACT_MARKET = 20;
    final private static int ACT_ORDERS = 21;
    final private static int ACT_TABLE = 22;
    final private static int ACT_OS = 23;
    final private static int ACT_WAIT = 24;
    final private static int ACT_BIOS = 25;
    final private static int ACT_TEST = 26;
    final private static int ACT_COUNT = 27;
    public rpc.LifeSim life;
    public rpc.City city;
    public rpc.WorkshopTools workshop;
    public rpc.SoftSim softSim;
    public rpc.ExpStress expStress;
    public boolean[] expPassed;
    public int expTotalPassed;
    public int expTotalXp;
    public int expSel;
    public int expTop;
    public int expFilterPlat;
    public int expFilterKind;
    public int expTab;
    public int[] expFiltered;
    public int expFilteredCount;
    private String[] MENU_ITEMS;
    /** one-word labels printed next to the icons; full text stays in MENU_ITEMS */
    private String[] MENU_TITLES;
    private int catSel;
    private int catTop;
    private int partSel;
    private int partTop;
    private int[] partList;
    final private static int[] CAT_ORDER;
    private int buildSel;
    private int buildTop;
    private int osLang;
    private int langSel;
    private rpc.Inst$Os pendingOs;
    private int pendingHack;
    private rpc.Corp corp;
    private rpc.Corp$Chip chip;
    private rpc.Corp$OsSpec osSpec;
    private rpc.Shell shell;
    private int shellTop;
    private int corpSel;
    private int corpTop;
    private int chipSel;
    private int osSpecSel;
    private int softSel;
    private int softKind;
    private int prodSel;
    private int siteSel;
    private int siteTop;
    private int browseSite;
    private int browsePage;
    private int browseScroll;
    private int gamesYear;
    private int gamesSel;
    private int gamesTop;
    private int gamesFps;
    private int gamesCode;
    private boolean gamesRan;
    private int[] gamesList;
    /** Cursor of the room screen: index into the hotspot list. */
    private int roomCursor;
    /**
     * Navigation history.
     *
     * Every screen change is recorded here by {@link #input}, and BACK pops
     * one entry.  This is what makes navigation reversible: a screen can
     * never send the player to an unrelated place, because going back means
     * literally returning to the screen that was open before.
     */
    private int[] navStack = new int[24];
    private int navDepth;
    /** Set while a BACK press is being handled, so it is not re-recorded. */
    private boolean navPopping;
    /** Global frame counter, advanced a few times a second by paint(). */
    private int frame;
    /** Animation counters of the tea / sleep screens. */
    private int teaTick;
    private int teaBrewed;
    private int sleepTick;
    private int W;
    private int H;
    private int FH0;
    private int FH1;
    private int FH2;
    final private static int[] ALL_CATS;
    private int bsodCode;
    private String bsodName;
    final private static String[] CHIP_FIELDS;
    
    static {
        String[] a = new String[5];
        a[0] = "\u0434\u0438\u0441\u043a";
        a[1] = "CD";
        a[2] = "USB";
        a[3] = "FDD";
        a[4] = "\u0441\u0435\u0442\u044c";
        BOOT = a;
        int[] a0 = new int[19];
        a0[0] = 1;
        a0[1] = 2;
        a0[2] = 3;
        a0[3] = 4;
        a0[4] = 5;
        a0[5] = 7;
        a0[6] = 8;
        a0[7] = 6;
        a0[8] = 9;
        a0[9] = 10;
        a0[10] = 11;
        a0[11] = 12;
        a0[12] = 13;
        a0[13] = 14;
        a0[14] = 15;
        a0[15] = 16;
        a0[16] = 17;
        a0[17] = 18;
        a0[18] = 19;
        CAT_ORDER = a0;
        int[] a1 = new int[16];
        a1[0] = 1;
        a1[1] = 2;
        a1[2] = 3;
        a1[3] = 4;
        a1[4] = 5;
        a1[5] = 7;
        a1[6] = 9;
        a1[7] = 10;
        a1[8] = 11;
        a1[9] = 12;
        a1[10] = 13;
        a1[11] = 14;
        a1[12] = 15;
        a1[13] = 16;
        a1[14] = 8;
        a1[15] = 17;
        ALL_CATS = a1;
        String[] a2 = new String[8];
        a2[0] = "\u0413\u043e\u0434";
        a2[1] = "\u0422\u0435\u0445\u043f\u0440\u043e\u0446\u0435\u0441\u0441";
        a2[2] = "\u0422\u0440\u0430\u043d\u0437\u0438\u0441\u0442\u043e\u0440\u043e\u0432, \u043c\u043b\u043d";
        a2[3] = "\u0427\u0430\u0441\u0442\u043e\u0442\u0430, \u041c\u0413\u0446";
        a2[4] = "\u042f\u0434\u0435\u0440 / IPC";
        a2[5] = "\u041a\u044d\u0448, \u041a\u0411";
        a2[6] = "\u0421\u043e\u043a\u0435\u0442 / \u0448\u0438\u043d\u0430";
        a2[7] = "\u0412\u0438\u0434\u0435\u043e\u043f\u0430\u043c\u044f\u0442\u044c/\u0431\u0438\u0442";
        CHIP_FIELDS = a2;
    }
    
    public Game() {
        this.pcPowered = false;
        this.bootFrame = 0;
        this.bootTotalFrames = 30;
        this.bootOsName = "MS-DOS";
        this.bootOsType = 0;
        this.repairSel = 0;
        this.repairTop = 0;
        this.repairOrders = null;
        this.mode = 0;
        this.menuSel = 0;
        this.mapSel = 0;
        this.mapTop = 0;
        this.museumSel = 0;
        this.museumTop = 0;
        this.levelIdx = 0;
        this.slots = new rpc.Part[20];
        this.budget = 0;
        this.spent = 0;
        this.sandbox = false;
        this.shopSlot = 0;
        this.shopSel = 0;
        this.shopTop = 0;
        this.shopList = new int[0];
        this.shopAllYears = false;
        this.toast = "";
        this.toastUntil = 0L;
        this.resultStart = 0L;
        this.resultPhase = 0;
        this.resultFrames = 0;
        this.RESULT_FRAMES = 6;
        this.cash = 0;
        this.day = 1;
        this.seed = 20260101;
        this.table = new rpc.Table();
        this.marketSel = 0;
        this.marketTop = 0;
        this.openMarket = 0;
        this.offerSel = 0;
        this.offerTop = 0;
        this.offerPart = new int[0];
        this.offerPrice = new int[0];
        this.offerCond = new int[0];
        this.offerDays = new int[0];
        this.offerDust = new int[0];
        this.offerBundle = new int[0][];
        this.orderSel = 0;
        this.orderTop = 0;
        this.benchSel = 0;
        this.benchTop = 0;
        this.osSel = 0;
        this.osTop = 0;
        this.progSel = 0;
        this.progTop = 0;
        this.editorLang = 0;
        this.editorSel = 0;
        this.editorTop = 0;
        this.editorLines = new String[40];
        this.editorN = 0;
        this.progs = new rpc.Os$Prog[0];
        this.tasks = new rpc.Os$Task[0];
        this.taskSel = 0;
        this.taskTop = 0;
        this.lastRun = null;
        this.runTitle = "";
        this.runBack = 13;
        this.minedTotal = 0;
        this.osIndex = -1;
        this.installingOs = null;
        this.instTotal = 0;
        this.instLeft = 0;
        this.hackLevel = 0;
        this.tpmOn = false;
        this.secureOn = false;
        this.uefiOn = false;
        this.xmpOn = false;
        this.patches = 0;
        this.setupSel = 0;
        this.speakerOn = true;
        this.simBought = false;
        this.bootSel = 0;
        this.osListSel = 0;
        this.osListTop = 0;
        this.webSel = 0;
        this.webTop = 0;
        this.specPart = null;
        this.specBack = 2;
        this.faults = new rpc.Diag$Fault[0];
        this.lastFault = 0;
        this.doomUnlocked = false;
        this.unlocked = 1;
        this.best = new int[64];
        this.totalXp = 0;
        this.life = new rpc.LifeSim();
        this.city = new rpc.City();
        this.workshop = new rpc.WorkshopTools();
        this.softSim = new rpc.SoftSim();
        this.expStress = new rpc.ExpStress();
        this.expPassed = new boolean[1000];
        this.expTotalPassed = 0;
        this.expTotalXp = 0;
        this.expSel = 0;
        this.expTop = 0;
        this.expFilterPlat = -1;
        this.expFilterKind = -1;
        this.expTab = 0;
        this.expFiltered = new int[1000];
        this.expFilteredCount = 0;
        String[] a = new String[8];
        a[0] = "\u0423\u044e\u0442\u043d\u0430\u044f \u043a\u043e\u043c\u043d\u0430\u0442\u0430 & \u0416\u0438\u0437\u043d\u044c (Life-Sim)";
        a[1] = "\u041f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430: \u0421\u0431\u043e\u0440\u043a\u0430 \u041f\u041a (1979-2027)";
        a[2] = "\u0412\u044b\u0445\u043e\u0434 \u0432 \u0433\u043e\u0440\u043e\u0434 (\u041c\u0438\u0442\u0438\u043d\u043e, \u041a\u043b\u0443\u0431, \u041a\u0430\u0444\u0435)";
        a[3] = "\u041c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u0430\u044f: \u0418\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442\u044b (\u041f\u0438\u043d\u0446\u0435\u0442, \u041f\u0430\u0439\u043a\u0430)";
        a[4] = "\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u0436\u0435\u043b\u0435\u0437\u0430 & \u041c\u0443\u0437\u0435\u0439";
        a[5] = "\u041b\u0430\u0431\u043e\u0440\u0430\u0442\u043e\u0440\u0438\u044f: 1000 \u042d\u043a\u0441\u043f\u0435\u0440\u0438\u043c\u0435\u043d\u0442\u043e\u0432";
        a[6] = "\u041a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u044c 1980-2027 & \u0414\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u044f";
        a[7] = "\u041e\u0431 \u0438\u0433\u0440\u0435 & \u0412\u044b\u0445\u043e\u0434";
        this.MENU_ITEMS = a;
        String[] t = new String[8];
        t[0] = "\u041a\u043e\u043c\u043d\u0430\u0442\u0430";
        t[1] = "\u0421\u0431\u043e\u0440\u043a\u0430 \u041f\u041a";
        t[2] = "\u0413\u043e\u0440\u043e\u0434";
        t[3] = "\u041c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u0430\u044f";
        t[4] = "\u041a\u0430\u0442\u0430\u043b\u043e\u0433";
        t[5] = "\u041b\u0430\u0431\u043e\u0440\u0430\u0442\u043e\u0440\u0438\u044f";
        t[6] = "\u041a\u0430\u043b\u0435\u043d\u0434\u0430\u0440\u044c";
        t[7] = "\u041e\u0431 \u0438\u0433\u0440\u0435";
        this.MENU_TITLES = t;
        this.catSel = 0;
        this.catTop = 0;
        this.partSel = 0;
        this.partTop = 0;
        this.partList = new int[0];
        this.buildSel = 0;
        this.buildTop = 0;
        this.osLang = 0;
        this.langSel = 0;
        this.pendingOs = null;
        this.pendingHack = 0;
        this.corp = new rpc.Corp();
        this.chip = new rpc.Corp$Chip();
        this.osSpec = new rpc.Corp$OsSpec();
        this.shell = new rpc.Shell();
        this.shellTop = 0;
        this.corpSel = 0;
        this.corpTop = 0;
        this.chipSel = 0;
        this.osSpecSel = 0;
        this.softSel = 0;
        this.softKind = 0;
        this.prodSel = 0;
        this.siteSel = 0;
        this.siteTop = 0;
        this.browseSite = -1;
        this.browsePage = 0;
        this.browseScroll = 0;
        this.gamesYear = 0;
        this.gamesSel = 0;
        this.gamesTop = 0;
        this.gamesFps = 0;
        this.gamesCode = 0;
        this.gamesRan = false;
        this.gamesList = new int[0];
        this.bsodCode = 0;
        this.bsodName = "";
    }
    
    private void initRepairOrders() {
        if (this.repairOrders != null && this.repairOrders.length > 0) {
            return;
        }
        int i = this.levelYear();
        rpc.Game$RepairOrder[] a = new rpc.Game$RepairOrder[6];
        a[0] = this.makeOrder("\u0418\u0432\u0430\u043d (\u043e\u0444\u0438\u0441)", "\u041f\u041a \u043f\u0435\u0440\u0435\u0433\u0440\u0435\u0432\u0430\u0435\u0442\u0441\u044f \u0438 \u0432\u044b\u0440\u0443\u0431\u0430\u0435\u0442\u0441\u044f", 0, 1800, 40, i, "\u041f\u0440\u043e\u0447\u0438\u0441\u0442\u0438\u0442\u044c \u043a\u0443\u043b\u0435\u0440 \u043e\u0442 \u043f\u044b\u043b\u0438 \u0438 \u043d\u0430\u043d\u0435\u0441\u0442\u0438 \u0441\u0432\u0435\u0436\u0443\u044e \u0442\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u0443");
        a[1] = this.makeOrder("\u0410\u043b\u0435\u043a\u0441\u0435\u0439 (\u0433\u0435\u0439\u043c\u0435\u0440)", "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c Windows \u0434\u043b\u044f \u0438\u0433\u0440", 1, 2500, 50, i, "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u0443\u044e \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u043e\u043d\u043d\u0443\u044e \u0441\u0438\u0441\u0442\u0435\u043c\u0443");
        a[2] = this.makeOrder("\u0421\u0435\u0440\u0433\u0435\u0439 (\u0438\u043d\u0436\u0435\u043d\u0435\u0440)", "\u041f\u041a \u043f\u0438\u0449\u0438\u0442 \u0441\u043f\u0438\u043a\u0435\u0440\u043e\u043c \u0438 \u043d\u0435 \u0432\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f", 2, 3400, 65, i, "\u041d\u0430\u0439\u0442\u0438 \u043d\u0435\u0438\u0441\u043f\u0440\u0430\u0432\u043d\u0443\u044e \u043f\u0430\u043c\u044f\u0442\u044c \u0438\u043b\u0438 \u043f\u043b\u0430\u0442\u0443 \u0438 \u043f\u043e\u0447\u0438\u043d\u0438\u0442\u044c");
        a[3] = this.makeOrder("\u041c\u0430\u0440\u0438\u044f (\u0431\u0443\u0445\u0433\u0430\u043b\u0442\u0435\u0440\u0438\u044f)", "\u041d\u0435 \u0432\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043f\u043e\u0441\u043b\u0435 \u0441\u043a\u0430\u0447\u043a\u0430 \u0441\u0435\u0442\u0438", 3, 2900, 55, i, "\u0417\u0430\u043c\u0435\u043d\u0438\u0442\u044c \u043d\u0435\u0438\u0441\u043f\u0440\u0430\u0432\u043d\u044b\u0439 \u0431\u043b\u043e\u043a \u043f\u0438\u0442\u0430\u043d\u0438\u044f");
        a[4] = this.makeOrder("\u0414\u043c\u0438\u0442\u0440\u0438\u0439 (\u0441\u0442\u0443\u0434\u0435\u043d\u0442)", "\u0423\u0441\u043a\u043e\u0440\u0438\u0442\u044c \u0440\u0430\u0431\u043e\u0442\u0443: \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0431\u044b\u0441\u0442\u0440\u044b\u0439 \u0434\u0438\u0441\u043a", 4, 3800, 75, i, "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c SSD/HDD \u0438 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c");
        a[5] = this.makeOrder("\u0420\u0435\u0442\u0440\u043e-\u041a\u043b\u0443\u0431", "\u0410\u043f\u0433\u0440\u0435\u0439\u0434 \u0433\u0440\u0430\u0444\u0438\u043a\u0438 \u0434\u043b\u044f 3D-\u0438\u0433\u0440", 5, 5200, 90, i, "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043c\u043e\u0449\u043d\u0443\u044e \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0443 \u0438 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c 3D-\u0442\u0435\u0441\u0442");
        this.repairOrders = a;
    }
    
    private rpc.Game$RepairOrder makeOrder(String s, String s0, int i, int i0, int i1, int i2, String s1) {
        rpc.Game$RepairOrder a = new rpc.Game$RepairOrder();
        a.client = s;
        a.title = s0;
        a.type = i;
        a.reward = i0;
        a.xp = i1;
        a.targetYear = i2;
        a.done = false;
        a.accepted = false;
        a.desc = s1;
        return a;
    }
    
    public void init(rpc.Host a, rpc.Gfx a0) {
        this.host = a;
        this.g = a0;
        rpc.Assets.bind(a0);
        this.db = new rpc.Db();
        try {
            this.db.load(a);
        } catch(Throwable a1) {
            this.toast = new StringBuffer("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u0431\u0430\u0437\u044b: ").append(a1.toString()).toString();
        }
        rpc.Assets.load(a);
        rpc.Sound.bind(a);
        this.best = new int[this.db.levels.length];
        this.loadProgress();
    }
    
    public rpc.Db db() {
        return this.db;
    }
    
    public int credits() {
        return this.cash;
    }
    
    public int dayNumber() {
        return this.day;
    }
    
    public int tableCount() {
        return this.table.n;
    }
    
    public int offerCount() {
        return this.offerPart.length;
    }
    
    public int pendingOrders() {
        return this.table.pending();
    }
    
    public rpc.Part slotPart(int i) {
        rpc.Part a = null;
        label2: {
            label0: {
                label1: {
                    if (i < 0) {
                        break label1;
                    }
                    if (i < 20) {
                        break label0;
                    }
                }
                a = null;
                break label2;
            }
            a = this.slots[i];
        }
        return a;
    }
    
    public int currentMode() {
        return this.mode;
    }
    
    public int mapSelection() {
        return this.mapSel;
    }
    
    public int buildSelection() {
        return this.buildSel;
    }
    
    public int currentLevel() {
        return this.levelIdx;
    }
    
    public void loadParts(int[] a) {
        int i = 0;
        while(true) {
            if (i < 20 && i < a.length) {
                this.slots[i] = ((a[i]) >= 0) ? this.db.get(a[i]) : null;
                i = i + 1;
                continue;
            }
            this.recalcSpent();
            this.buildSel = 0;
            return;
        }
    }
    
    public boolean animating() {
        if (this.mode == 4 && this.result != null && this.resultFrames < 6) {
            return true;
        }
        return false;
    }
    
    public void loadProgress() {
        String s = this.host.load("save");
        if (s != null && s.length() != 0) {
            label0: {
                try {
                    boolean b = s.startsWith("v48|");
                    label1: {
                        if (b) {
                            break label1;
                        }
                        int[] a = rpc.Util.splitInts(s, (char)44);
                        if (a.length <= 1) {
                            break label0;
                        }
                        this.unlocked = a[0];
                        int i = 0;
                        while(i < this.best.length && i + 1 < a.length) {
                            this.best[i] = a[i + 1];
                            i = i + 1;
                        }
                        if (a.length <= this.best.length + 1) {
                            break label0;
                        }
                        this.totalXp = a[this.best.length + 1];
                        break label0;
                    }
                    String[] a0 = rpc.Util.split(s, (char)124);
                    if (a0.length > 1) {
                        int[] a1 = rpc.Util.splitInts(a0[1], (char)44);
                        if (a1.length >= 8) {
                            this.unlocked = a1[0];
                            this.totalXp = a1[1];
                            this.levelIdx = a1[2];
                            this.cash = a1[3];
                            this.budget = a1[4];
                            this.spent = a1[5];
                            this.day = a1[6];
                            this.osIndex = a1[7];
                            if (a1.length > 8) {
                                this.osLang = a1[8];
                            }
                            if (a1.length > 9) {
                                this.pcPowered = a1[9] != 0;
                            }
                        }
                    }
                    if (a0.length > 2 && a0[2].length() > 0) {
                        int[] a2 = rpc.Util.splitInts(a0[2], (char)44);
                        int i0 = 0;
                        while(i0 < 20 && i0 * 3 + 2 < a2.length) {
                            int i1 = a2[i0 * 3];
                            if (i1 >= 0) {
                                rpc.Part a3 = this.db.get(i1);
                                if (a3 != null) {
                                    this.slots[i0] = a3;
                                    a3.cond = a2[i0 * 3 + 1];
                                    a3.dust = a2[i0 * 3 + 2];
                                }
                            }
                            i0 = i0 + 1;
                        }
                    }
                    if (a0.length > 3 && a0[3].length() > 0) {
                        int[] a4 = rpc.Util.splitInts(a0[3], (char)44);
                        if (a4.length > 0) {
                            int i2 = Math.min(12, a4[0]);
                            this.table.n = 0;
                            int i3 = 0;
                            while(i3 < i2 && i3 * 3 + 3 < a4.length) {
                                boolean b0 = a4[1 + i3 * 3] != 0;
                                boolean b1 = a4[1 + i3 * 3 + 1] != 0;
                                boolean b2 = a4[1 + i3 * 3 + 2] != 0;
                                rpc.Part a5 = this.db.get(b0 ? 1 : 0);
                                if (a5 != null) {
                                    this.table.addToTable(a5, b1 ? 1 : 0, b2 ? 1 : 0);
                                }
                                i3 = i3 + 1;
                            }
                        }
                    }
                    if (a0.length > 4 && a0[4].length() > 0) {
                        this.initRepairOrders();
                        int[] a6 = rpc.Util.splitInts(a0[4], (char)44);
                        int i4 = 0;
                        while(i4 < a6.length && i4 < this.repairOrders.length) {
                            this.repairOrders[i4].done = a6[i4] != 0;
                            i4 = i4 + 1;
                        }
                    }
                    if (a0.length > 5 && a0[5].length() > 0) {
                        int[] a7 = rpc.Util.splitInts(a0[5], (char)44);
                        if (a7.length >= 2) {
                            this.corp.cash = a7[0];
                            this.corp.rd = a7[1];
                        }
                    }
                } catch(Exception ignoredException) {
                    break label0;
                }
                return;
            }
            if (this.unlocked >= this.db.levels.length) {
                this.unlocked = this.db.levels.length - 1;
            }
            if (this.unlocked < 1) {
                this.unlocked = 1;
            }
            return;
        }
    }
    
    public void saveProgress() {
        try {
            StringBuffer a = new StringBuffer();
            a.append("v48|");
            a.append(this.unlocked).append((char)44).append(this.totalXp).append((char)44).append(this.levelIdx).append((char)44).append(this.cash).append((char)44).append(this.budget).append((char)44).append(this.spent).append((char)44).append(this.day).append((char)44).append(this.osIndex).append((char)44).append(this.osLang).append((char)44).append((this.pcPowered) ? 1 : 0);
            a.append((char)124);
            int i = 0;
            while(i < 20) {
                if (i > 0) {
                    a.append((char)44);
                }
                if (this.slots[i] == null) {
                    a.append("-1,100,0");
                } else {
                    a.append(this.slots[i].index).append((char)44).append(this.slots[i].cond).append((char)44).append(this.slots[i].dust);
                }
                i = i + 1;
            }
            a.append((char)124);
            a.append(this.table.n);
            int i0 = 0;
            while(i0 < this.table.n) {
                a.append((char)44);
                if (this.table.parts[i0] == null) {
                    a.append("-1,100,0");
                } else {
                    a.append(this.table.parts[i0].index).append((char)44).append(this.table.cond[i0]).append((char)44).append(this.table.dust[i0]);
                }
                i0 = i0 + 1;
            }
            a.append((char)124);
            this.initRepairOrders();
            int i1 = 0;
            while(i1 < this.repairOrders.length) {
                if (i1 > 0) {
                    a.append((char)44);
                }
                a.append((this.repairOrders[i1].done) ? 1 : 0);
                i1 = i1 + 1;
            }
            a.append((char)124);
            a.append(this.corp.cash).append((char)44).append(this.corp.rd);
            this.host.save("save", a.toString());
        } catch(Exception ignoredException) {
        }
    }
    
    /**
     * Single entry point for every button press.
     *
     * Each screen gets its own handler and only those handlers change
     * {@code mode}, so the "Чай button opens the PC builder" class of bug
     * (screens sharing one action table) cannot happen any more.  Screen
     * specific handlers are called from here, everything that still lives in
     * the legacy table is delegated to {@link #inputLegacy}.
     */
    public void input(int action) {
        if (this.toastActive()) {
            this.toast = "";
        }
        int before = this.mode;
        boolean newScreens = this.mode == M_MENU || this.mode == M_ROOM
                || this.mode == M_TEA || this.mode == M_SLEEP;
        if ((action == ACT_BACK || action == ACT_SOFT2) && !newScreens) {
            // legacy screens: BACK always returns to the previous screen
            this.navBack();
            return;
        }
        switch (this.mode) {
            case M_MENU:
                this.inputMenu(action);
                break;
            case M_ROOM:
                this.inputRoom(action);
                break;
            case M_TEA:
                this.inputTea(action);
                break;
            case M_SLEEP:
                this.inputSleep(action);
                break;
            default:
                this.inputLegacy(action);
                break;
        }
        if (this.mode != before) {
            if (this.navPopping) {
                this.navPopping = false;
            } else {
                this.navPush(before);
            }
        }
    }

    private void navPush(int mode) {
        if (this.navDepth > 0 && this.navStack[this.navDepth - 1] == mode) {
            return;
        }
        if (this.navDepth == this.navStack.length) {
            // drop the oldest entry
            for (int i = 1; i < this.navStack.length; i++) {
                this.navStack[i - 1] = this.navStack[i];
            }
            this.navDepth--;
        }
        this.navStack[this.navDepth++] = mode;
    }

    private void navBack() {
        this.navPopping = true;
        int target = M_MENU;
        while (this.navDepth > 0) {
            target = this.navStack[--this.navDepth];
            if (target != this.mode) {
                break;
            }
        }
        this.mode = (target >= 0 && target < 60) ? target : M_MENU;
        Sound.click();
    }
    
    private void inputLegacy(int i) {
        label0: if (this.mode != 0) {
            if (this.mode != 1) {
                if (this.mode != 2) {
                    if (this.mode != 9) {
                        if (this.mode != 10) {
                            if (this.mode != 11) {
                                if (this.mode != 12) {
                                    if (this.mode != 13) {
                                        if (this.mode != 14) {
                                            if (this.mode != 15) {
                                                if (this.mode != 16) {
                                                    if (this.mode != 17) {
                                                        if (this.mode != 18) {
                                                            if (this.mode != 19) {
                                                                if (this.mode != 20) {
                                                                    if (this.mode != 27) {
                                                                        if (this.mode != 21) {
                                                                            if (this.mode != 28) {
                                                                                if (this.mode != 22) {
                                                                                    if (this.mode != 23) {
                                                                                        if (this.mode != 24) {
                                                                                            if (this.mode != 25) {
                                                                                                if (this.mode != 26) {
                                                                                                    if (this.mode != 3) {
                                                                                                        if (this.mode != 4) {
                                                                                                            if (this.mode != 5) {
                                                                                                                if (this.mode != 7) {
                                                                                                                    if (this.mode != 8) {
                                                                                                                        if (this.mode != 6) {
                                                                                                                            if (this.mode != 47) {
                                                                                                                                if (this.mode != 48) {
                                                                                                                                    if (this.mode != 49) {
                                                                                                                                        if (this.mode != 52) {
                                                                                                                                            if (this.mode != 45) {
                                                                                                                                                if (this.mode != 46) {
                                                                                                                                                    if (this.mode >= 29) {
                                                                                                                                                        this.inputExtra(i);
                                                                                                                                                    }
                                                                                                                                                } else {
                                                                                                                                                    this.inputExpRun(i);
                                                                                                                                                }
                                                                                                                                            } else {
                                                                                                                                                this.inputExpList(i);
                                                                                                                                            }
                                                                                                                                        } else {
                                                                                                                                            this.inputEventNews(i);
                                                                                                                                        }
                                                                                                                                    } else {
                                                                                                                                        this.inputWorkshopTools(i);
                                                                                                                                    }
                                                                                                                                } else {
                                                                                                                                    this.inputCity(i);
                                                                                                                                }
                                                                                                                            } else {
                                                                                                                                this.inputRoom(i);
                                                                                                                            }
                                                                                                                        } else if (i != 4) {
                                                                                                                            if (i != 6) {
                                                                                                                                label44: {
                                                                                                                                    if (i == 7) {
                                                                                                                                        break label44;
                                                                                                                                    }
                                                                                                                                    if (i != 5) {
                                                                                                                                        break label0;
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                this.mode = 0;
                                                                                                                            } else {
                                                                                                                                this.loadProgress();
                                                                                                                                rpc.Sound.ok();
                                                                                                                                this.toastMsg("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0438\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u043e!");
                                                                                                                            }
                                                                                                                        } else {
                                                                                                                            this.saveProgress();
                                                                                                                            rpc.Sound.ok();
                                                                                                                            this.toastMsg("\u0418\u0433\u0440\u0430 \u0441\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u0430 \u0432 \u043f\u0430\u043c\u044f\u0442\u044c!");
                                                                                                                        }
                                                                                                                    } else {
                                                                                                                        int i0 = this.partList.length;
                                                                                                                        if (i0 > 0) {
                                                                                                                            if (i != 0) {
                                                                                                                                if (i != 1) {
                                                                                                                                    if (i != 2) {
                                                                                                                                        if (i == 3) {
                                                                                                                                            this.openSpec(this.db.get(this.partList[this.partSel]), 8);
                                                                                                                                        }
                                                                                                                                    } else {
                                                                                                                                        this.partSel = (this.partSel + i0 - 10 % i0 + i0) % i0;
                                                                                                                                    }
                                                                                                                                } else {
                                                                                                                                    this.partSel = (this.partSel + 1) % i0;
                                                                                                                                }
                                                                                                                            } else {
                                                                                                                                this.partSel = (this.partSel + i0 - 1) % i0;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        label43: {
                                                                                                                            if (i == 7) {
                                                                                                                                break label43;
                                                                                                                            }
                                                                                                                            if (i == 5) {
                                                                                                                                break label43;
                                                                                                                            }
                                                                                                                            if (i != 4) {
                                                                                                                                break label0;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        this.mode = 7;
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    int i1 = CAT_ORDER.length;
                                                                                                                    if (i != 0) {
                                                                                                                        if (i != 1) {
                                                                                                                            if (i != 4) {
                                                                                                                                label42: {
                                                                                                                                    if (i == 7) {
                                                                                                                                        break label42;
                                                                                                                                    }
                                                                                                                                    if (i != 5) {
                                                                                                                                        break label0;
                                                                                                                                    }
                                                                                                                                }
                                                                                                                                this.mode = 0;
                                                                                                                            } else {
                                                                                                                                this.openCategory(CAT_ORDER[this.catSel]);
                                                                                                                            }
                                                                                                                        } else {
                                                                                                                            this.catSel = (this.catSel + 1) % i1;
                                                                                                                        }
                                                                                                                    } else {
                                                                                                                        this.catSel = (this.catSel + i1 - 1) % i1;
                                                                                                                    }
                                                                                                                }
                                                                                                            } else {
                                                                                                                int i2 = this.db.levels.length;
                                                                                                                if (i != 0) {
                                                                                                                    if (i != 1) {
                                                                                                                        label41: {
                                                                                                                            if (i == 7) {
                                                                                                                                break label41;
                                                                                                                            }
                                                                                                                            if (i == 5) {
                                                                                                                                break label41;
                                                                                                                            }
                                                                                                                            if (i != 4) {
                                                                                                                                break label0;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        this.mode = 0;
                                                                                                                    } else {
                                                                                                                        this.museumSel = (this.museumSel + 1) % i2;
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    this.museumSel = (this.museumSel + i2 - 1) % i2;
                                                                                                                }
                                                                                                            }
                                                                                                        } else {
                                                                                                            label40: {
                                                                                                                if (i == 4) {
                                                                                                                    break label40;
                                                                                                                }
                                                                                                                if (i != 7) {
                                                                                                                    break label0;
                                                                                                                }
                                                                                                            }
                                                                                                            if (this.result != null && this.result.ok && this.result.score >= this.db.levels[this.levelIdx].passScore && !this.sandbox) {
                                                                                                                if (this.levelIdx + 1 > this.unlocked - 1 && this.levelIdx + 1 < this.db.levels.length) {
                                                                                                                    this.unlocked = this.levelIdx + 2;
                                                                                                                }
                                                                                                                if (this.unlocked > this.db.levels.length) {
                                                                                                                    this.unlocked = this.db.levels.length;
                                                                                                                }
                                                                                                                this.saveProgress();
                                                                                                            }
                                                                                                            this.mode = (this.sandbox) ? 0 : 1;
                                                                                                        }
                                                                                                    } else {
                                                                                                        int i3 = this.shopList.length;
                                                                                                        if (i != 0) {
                                                                                                            if (i != 1) {
                                                                                                                if (i != 4) {
                                                                                                                    label39: {
                                                                                                                        if (i != 3) {
                                                                                                                            break label39;
                                                                                                                        }
                                                                                                                        if (this.shopList.length <= 0) {
                                                                                                                            break label39;
                                                                                                                        }
                                                                                                                        this.openSpec(this.db.get(this.shopList[this.shopSel]), 3);
                                                                                                                        break label0;
                                                                                                                    }
                                                                                                                    label37: {
                                                                                                                        label38: {
                                                                                                                            if (i == 5) {
                                                                                                                                break label38;
                                                                                                                            }
                                                                                                                            if (i != 7) {
                                                                                                                                break label37;
                                                                                                                            }
                                                                                                                        }
                                                                                                                        this.mode = 2;
                                                                                                                        break label0;
                                                                                                                    }
                                                                                                                    if (i == 6) {
                                                                                                                        this.shopAllYears = !this.shopAllYears;
                                                                                                                        this.rebuildShopList();
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    this.buySelected();
                                                                                                                }
                                                                                                            } else {
                                                                                                                this.shopSel = (this.shopSel + 1) % i3;
                                                                                                            }
                                                                                                        } else {
                                                                                                            this.shopSel = (this.shopSel + i3 - 1) % i3;
                                                                                                        }
                                                                                                    }
                                                                                                } else {
                                                                                                    label36: {
                                                                                                        if (i == 4) {
                                                                                                            break label36;
                                                                                                        }
                                                                                                        if (i == 7) {
                                                                                                            break label36;
                                                                                                        }
                                                                                                        if (i != 5) {
                                                                                                            break label0;
                                                                                                        }
                                                                                                    }
                                                                                                    this.mode = 13;
                                                                                                }
                                                                                            } else {
                                                                                                rpc.Inst$Dl[] a = rpc.Inst.downloads(this.levelYear());
                                                                                                if (i != 0) {
                                                                                                    if (i != 1) {
                                                                                                        if (i != 4) {
                                                                                                            if (i != 6) {
                                                                                                                label35: {
                                                                                                                    if (i == 7) {
                                                                                                                        break label35;
                                                                                                                    }
                                                                                                                    if (i != 5) {
                                                                                                                        break label0;
                                                                                                                    }
                                                                                                                }
                                                                                                                this.mode = 13;
                                                                                                            } else {
                                                                                                                this.buySim();
                                                                                                            }
                                                                                                        } else {
                                                                                                            this.downloadItem(this.webSel);
                                                                                                        }
                                                                                                    } else {
                                                                                                        this.webSel = (this.webSel + 1) % (a.length + 1);
                                                                                                    }
                                                                                                } else {
                                                                                                    this.webSel = (this.webSel + a.length + 1) % (a.length + 1);
                                                                                                }
                                                                                            }
                                                                                        } else {
                                                                                            label34: {
                                                                                                if (i == 4) {
                                                                                                    break label34;
                                                                                                }
                                                                                                if (i == 7) {
                                                                                                    break label34;
                                                                                                }
                                                                                                if (i != 5) {
                                                                                                    break label0;
                                                                                                }
                                                                                            }
                                                                                            this.mode = this.specBack;
                                                                                        }
                                                                                    } else {
                                                                                        label33: {
                                                                                            if (i == 4) {
                                                                                                break label33;
                                                                                            }
                                                                                            if (i == 7) {
                                                                                                break label33;
                                                                                            }
                                                                                            if (i != 5) {
                                                                                                break label0;
                                                                                            }
                                                                                        }
                                                                                        this.mode = 2;
                                                                                        this.toastMsg("\u041f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0430. \u041f\u0440\u043e\u0432\u0435\u0440\u044c\u0442\u0435 \u0436\u0435\u043b\u0435\u0437\u043e \u0432 BIOS/POST");
                                                                                    }
                                                                                } else {
                                                                                    label31: {
                                                                                        label32: {
                                                                                            if (i == 4) {
                                                                                                break label32;
                                                                                            }
                                                                                            if (i != 6) {
                                                                                                break label31;
                                                                                            }
                                                                                        }
                                                                                        this.installStep(10);
                                                                                        break label0;
                                                                                    }
                                                                                    label30: {
                                                                                        if (i == 7) {
                                                                                            break label30;
                                                                                        }
                                                                                        if (i != 5) {
                                                                                            break label0;
                                                                                        }
                                                                                    }
                                                                                    this.mode = 13;
                                                                                }
                                                                            } else {
                                                                                int i4 = rpc.Langs.count(this.langYear());
                                                                                if (i != 0) {
                                                                                    if (i != 1) {
                                                                                        if (i != 4) {
                                                                                            label29: {
                                                                                                if (i == 7) {
                                                                                                    break label29;
                                                                                                }
                                                                                                if (i != 5) {
                                                                                                    break label0;
                                                                                                }
                                                                                            }
                                                                                            this.mode = 21;
                                                                                        } else {
                                                                                            this.osLang = rpc.Langs.nth(this.langYear(), this.langSel);
                                                                                            this.startInstall(this.pendingOs, this.pendingHack);
                                                                                        }
                                                                                    } else {
                                                                                        this.langSel = (this.langSel + 1) % i4;
                                                                                    }
                                                                                } else {
                                                                                    this.langSel = (this.langSel + i4 - 1) % i4;
                                                                                }
                                                                            }
                                                                        } else {
                                                                            int i5 = rpc.Inst.all().length;
                                                                            if (i != 0) {
                                                                                if (i != 1) {
                                                                                    if (i != 4) {
                                                                                        label28: {
                                                                                            if (i == 7) {
                                                                                                break label28;
                                                                                            }
                                                                                            if (i != 5) {
                                                                                                break label0;
                                                                                            }
                                                                                        }
                                                                                        this.mode = 13;
                                                                                    } else {
                                                                                        this.pickOs(this.osListSel);
                                                                                    }
                                                                                } else {
                                                                                    this.osListSel = (this.osListSel + 1) % i5;
                                                                                }
                                                                            } else {
                                                                                this.osListSel = (this.osListSel + i5 - 1) % i5;
                                                                            }
                                                                        }
                                                                    } else {
                                                                        label27: {
                                                                            if (i == 4) {
                                                                                break label27;
                                                                            }
                                                                            if (i == 7) {
                                                                                break label27;
                                                                            }
                                                                            if (i != 5) {
                                                                                break label0;
                                                                            }
                                                                        }
                                                                        this.mode = 20;
                                                                    }
                                                                } else {
                                                                    int i6 = 8 + this.faults.length;
                                                                    if (i != 0) {
                                                                        if (i != 1) {
                                                                            if (i != 4) {
                                                                                if (i != 6) {
                                                                                    label26: {
                                                                                        if (i == 7) {
                                                                                            break label26;
                                                                                        }
                                                                                        if (i != 5) {
                                                                                            break label0;
                                                                                        }
                                                                                    }
                                                                                    this.mode = 2;
                                                                                } else {
                                                                                    this.runPost(true);
                                                                                }
                                                                            } else {
                                                                                this.setupFire();
                                                                            }
                                                                        } else {
                                                                            this.setupSel = (this.setupSel + 1) % i6;
                                                                        }
                                                                    } else {
                                                                        this.setupSel = (this.setupSel + i6 - 1) % i6;
                                                                    }
                                                                }
                                                            } else if (i != 4) {
                                                                if (i != 6) {
                                                                    label25: {
                                                                        if (i == 7) {
                                                                            break label25;
                                                                        }
                                                                        if (i != 5) {
                                                                            break label0;
                                                                        }
                                                                    }
                                                                    this.mode = 13;
                                                                } else {
                                                                    this.mine(7);
                                                                }
                                                            } else {
                                                                this.mine(1);
                                                            }
                                                        } else {
                                                            label24: {
                                                                if (i == 7) {
                                                                    break label24;
                                                                }
                                                                if (i == 5) {
                                                                    break label24;
                                                                }
                                                                if (i != 4) {
                                                                    break label0;
                                                                }
                                                            }
                                                            this.mode = 13;
                                                        }
                                                    } else {
                                                        int i7 = this.tasks.length;
                                                        if (i7 > 0) {
                                                            if (i != 0) {
                                                                if (i == 1) {
                                                                    this.taskSel = (this.taskSel + 1) % i7;
                                                                }
                                                            } else {
                                                                this.taskSel = (this.taskSel + i7 - 1) % i7;
                                                            }
                                                        }
                                                        label22: {
                                                            label23: {
                                                                if (i == 4) {
                                                                    break label23;
                                                                }
                                                                if (i != 6) {
                                                                    break label22;
                                                                }
                                                            }
                                                            if (i7 <= 0) {
                                                                break label22;
                                                            }
                                                            this.checkTask(this.taskSel);
                                                            break label0;
                                                        }
                                                        label21: {
                                                            if (i == 7) {
                                                                break label21;
                                                            }
                                                            if (i != 5) {
                                                                break label0;
                                                            }
                                                        }
                                                        this.mode = 13;
                                                    }
                                                } else {
                                                    label20: {
                                                        if (i == 4) {
                                                            break label20;
                                                        }
                                                        if (i == 7) {
                                                            break label20;
                                                        }
                                                        if (i != 5) {
                                                            break label0;
                                                        }
                                                    }
                                                    this.mode = this.runBack;
                                                }
                                            } else if (i != 0) {
                                                if (i != 1) {
                                                    if (i != 4) {
                                                        if (i != 2) {
                                                            if (i != 3) {
                                                                if (i != 6) {
                                                                    label19: {
                                                                        if (i == 5) {
                                                                            break label19;
                                                                        }
                                                                        if (i != 7) {
                                                                            break label0;
                                                                        }
                                                                    }
                                                                    this.autoSaveProgram();
                                                                    this.mode = 13;
                                                                } else {
                                                                    this.runEditor();
                                                                }
                                                            } else {
                                                                this.insertLine((this.editorSel >= 0) ? this.editorSel : 0);
                                                            }
                                                        } else {
                                                            this.editorUndo();
                                                        }
                                                    } else if (this.editorSel >= 0) {
                                                        this.typeLine(this.editorSel);
                                                    } else {
                                                        this.cycleLang(1);
                                                    }
                                                } else {
                                                    this.editorSel = Math.min(this.editorN, this.editorSel + 1);
                                                }
                                            } else {
                                                this.editorSel = Math.max(-1, this.editorSel - 1);
                                            }
                                        } else {
                                            int i8 = this.progs.length + 1;
                                            if (i != 0) {
                                                if (i != 1) {
                                                    if (i != 4) {
                                                        if (i != 6) {
                                                            label18: {
                                                                if (i == 7) {
                                                                    break label18;
                                                                }
                                                                if (i != 5) {
                                                                    break label0;
                                                                }
                                                            }
                                                            this.mode = 13;
                                                        } else {
                                                            this.openEditor(this.editorLang);
                                                        }
                                                    } else if (this.progSel < this.progs.length) {
                                                        this.runSaved(this.progSel);
                                                    } else {
                                                        this.openEditor(this.editorLang);
                                                    }
                                                } else {
                                                    this.progSel = (this.progSel + 1) % i8;
                                                }
                                            } else {
                                                this.progSel = (this.progSel + i8 - 1) % i8;
                                            }
                                        }
                                    } else {
                                        int i9 = this.osItems();
                                        if (i != 0) {
                                            if (i != 1) {
                                                if (i != 4) {
                                                    label17: {
                                                        if (i == 7) {
                                                            break label17;
                                                        }
                                                        if (i != 5) {
                                                            break label0;
                                                        }
                                                    }
                                                    this.mode = 2;
                                                } else {
                                                    this.osAction();
                                                }
                                            } else {
                                                this.osSel = (this.osSel + 1) % i9;
                                            }
                                        } else {
                                            this.osSel = (this.osSel + i9 - 1) % i9;
                                        }
                                    }
                                } else {
                                    int i10 = this.table.n;
                                    if (i10 > 0) {
                                        if (i != 0) {
                                            if (i == 1) {
                                                this.benchSel = (this.benchSel + 1) % i10;
                                            }
                                        } else {
                                            this.benchSel = (this.benchSel + i10 - 1) % i10;
                                        }
                                    }
                                    label16: {
                                        if (i != 4) {
                                            break label16;
                                        }
                                        if (i10 <= 0) {
                                            break label16;
                                        }
                                        this.installFromBench(this.benchSel);
                                        break label0;
                                    }
                                    label15: {
                                        if (i != 6) {
                                            break label15;
                                        }
                                        if (i10 <= 0) {
                                            break label15;
                                        }
                                        this.repairBench(this.benchSel);
                                        break label0;
                                    }
                                    label14: {
                                        if (i != 2) {
                                            break label14;
                                        }
                                        if (i10 <= 0) {
                                            break label14;
                                        }
                                        this.sellBench(this.benchSel);
                                        break label0;
                                    }
                                    label13: {
                                        if (i == 7) {
                                            break label13;
                                        }
                                        if (i != 5) {
                                            break label0;
                                        }
                                    }
                                    this.mode = 2;
                                }
                            } else {
                                label11: {
                                    label12: {
                                        if (i == 4) {
                                            break label12;
                                        }
                                        if (i != 6) {
                                            break label11;
                                        }
                                    }
                                    this.advanceDay(1);
                                    break label0;
                                }
                                label10: {
                                    if (i == 7) {
                                        break label10;
                                    }
                                    if (i != 5) {
                                        break label0;
                                    }
                                }
                                this.mode = 2;
                            }
                        } else {
                            int i11 = this.offerPart.length;
                            label9: if (i11 > 0) {
                                if (i != 0) {
                                    if (i != 1) {
                                        if (i != 3) {
                                            if (i == 2) {
                                                this.offerSel = (this.offerSel - 3 + i11 * 2) % i11;
                                            }
                                        } else {
                                            label7: {
                                                if (i11 <= 0) {
                                                    break label7;
                                                }
                                                int[][] a0 = this.offerBundle;
                                                label8: {
                                                    if (a0 == null) {
                                                        break label8;
                                                    }
                                                    if (this.offerSel >= this.offerBundle.length) {
                                                        break label8;
                                                    }
                                                    if (this.offerBundle[this.offerSel] != null) {
                                                        break label7;
                                                    }
                                                }
                                                this.openSpec(this.db.get(this.offerPart[this.offerSel]), 10);
                                                break label9;
                                            }
                                            if (i11 > 0) {
                                                this.toastMsg("\u0413\u043e\u0442\u043e\u0432\u044b\u0439 \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u0438\u043a: \u0434\u0435\u0442\u0430\u043b\u0438 \u043f\u0440\u0438\u0435\u0434\u0443\u0442 \u0432\u043c\u0435\u0441\u0442\u0435");
                                            }
                                        }
                                    } else {
                                        this.offerSel = (this.offerSel + 1) % i11;
                                    }
                                } else {
                                    this.offerSel = (this.offerSel + i11 - 1) % i11;
                                }
                            }
                            label6: {
                                if (i != 4) {
                                    break label6;
                                }
                                if (i11 <= 0) {
                                    break label6;
                                }
                                this.buyOffer();
                                break label0;
                            }
                            label5: {
                                if (i != 6) {
                                    break label5;
                                }
                                if (this.openMarket != 3) {
                                    break label5;
                                }
                                this.advanceDay(1);
                                this.rollOffers(3);
                                this.junkFind();
                                break label0;
                            }
                            label4: {
                                if (i == 7) {
                                    break label4;
                                }
                                if (i != 5) {
                                    break label0;
                                }
                            }
                            this.mode = 9;
                        }
                    } else if (i != 0) {
                        if (i != 1) {
                            if (i != 4) {
                                label3: {
                                    if (i == 7) {
                                        break label3;
                                    }
                                    if (i != 5) {
                                        break label0;
                                    }
                                }
                                this.mode = 2;
                            } else if (rpc.Market.available(this.marketSel, this.levelYear())) {
                                this.openMarket(this.marketSel);
                            } else {
                                this.toastMsg(new StringBuffer("\u0417\u0434\u0435\u0441\u044c \u044d\u0442\u043e \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 ").append(rpc.Market.SINCE[this.marketSel]).append(" \u0433\u043e\u0434\u0443").toString());
                            }
                        } else {
                            this.marketSel = (this.marketSel + 1) % 7;
                        }
                    } else {
                        this.marketSel = (this.marketSel + 7 - 1) % 7;
                    }
                } else if (i != 0) {
                    if (i != 1) {
                        if (i != 4) {
                            if (i != 3) {
                                if (i != 2) {
                                    label2: {
                                        if (i == 5) {
                                            break label2;
                                        }
                                        if (i == 7) {
                                            break label2;
                                        }
                                        if (i != 6) {
                                            break label0;
                                        }
                                        this.runTest();
                                        break label0;
                                    }
                                    this.mode = (this.sandbox) ? 0 : 1;
                                } else if (this.buildSel < 20) {
                                    this.clearSlot(this.buildSel);
                                }
                            } else if (this.buildSel < 20) {
                                this.openSpec(this.slots[this.buildSel], 2);
                            }
                        } else if (this.buildSel != 26) {
                            if (this.buildSel != 20) {
                                if (this.buildSel != 21) {
                                    if (this.buildSel != 22) {
                                        if (this.buildSel != 23) {
                                            if (this.buildSel != 24) {
                                                if (this.buildSel != 25) {
                                                    if (this.buildSel != 26) {
                                                        this.openShop(this.buildSel);
                                                    } else if (this.pcPowered) {
                                                        this.mode = 13;
                                                    } else {
                                                        this.powerOnPC();
                                                    }
                                                } else {
                                                    this.openSetup();
                                                }
                                            } else {
                                                this.repairSel = 0;
                                                this.repairTop = 0;
                                                this.mode = 44;
                                            }
                                        } else {
                                            this.openOs();
                                        }
                                    } else {
                                        this.benchSel = 0;
                                        this.mode = 12;
                                    }
                                } else {
                                    this.orderSel = 0;
                                    this.mode = 11;
                                }
                            } else {
                                this.openMarkets();
                            }
                        } else {
                            this.runTest();
                        }
                    } else {
                        this.buildSel = (this.buildSel + 1) % 27;
                    }
                } else {
                    this.buildSel = (this.buildSel + 27 - 1) % 27;
                }
            } else {
                int i12 = this.db.levels.length;
                if (i != 0) {
                    if (i != 1) {
                        if (i != 4) {
                            label1: {
                                if (i == 7) {
                                    break label1;
                                }
                                if (i != 5) {
                                    break label0;
                                }
                            }
                            this.mode = 0;
                        } else if (this.mapSel >= this.unlocked) {
                            this.toastMsg("\u0423\u0440\u043e\u0432\u0435\u043d\u044c \u0437\u0430\u043a\u0440\u044b\u0442: \u043f\u0440\u043e\u0439\u0434\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u044b\u0434\u0443\u0449\u0438\u0435 \u0433\u043e\u0434\u044b");
                        } else {
                            this.startLevel(this.mapSel);
                        }
                    } else {
                        this.mapSel = (this.mapSel + 1) % i12;
                    }
                } else {
                    this.mapSel = (this.mapSel + i12 - 1) % i12;
                }
            }
        } else if (i != 0) {
            if (i != 1) {
                if (i == 4) {
                    this.menuAction();
                }
            } else {
                this.menuSel = (this.menuSel + 1) % this.MENU_ITEMS.length;
            }
        } else {
            this.menuSel = (this.menuSel + this.MENU_ITEMS.length - 1) % this.MENU_ITEMS.length;
        }
    }
    
    /* ==================================================================
     *  main menu
     * ================================================================== */

    private void inputMenu(int action) {
        int n = this.MENU_ITEMS.length;
        if (action == ACT_DOWN) {
            this.menuSel = (this.menuSel + 1) % n;
            Sound.click();
        } else if (action == ACT_UP) {
            this.menuSel = (this.menuSel + n - 1) % n;
            Sound.click();
        } else if (action == ACT_FIRE || action == ACT_SOFT1) {
            Sound.ok();
            this.menuAction();
        } else if (action >= ACT_NUM1 && action < ACT_NUM1 + 8) {
            // number keys jump straight to the entry, they are printed on
            // the left edge of every row
            this.menuSel = action - ACT_NUM1;
            Sound.ok();
            this.menuAction();
        } else if (action == ACT_BACK || action == ACT_SOFT2) {
            // in the menu BACK is "leave the game" - the state is saved first
            this.saveProgress();
            this.host.exitApp();
        }
    }

    /* ==================================================================
     *  the room (life sim hub)
     * ================================================================== */

    private int roomHotspot(int action) {
        /*
         * The cursor moves through the objects that are actually drawn:
         * monitor, mug, cat, lamp, bed, door, tools, radio.  Number keys are
         * the same objects in the same order, so both input styles agree -
         * in 4.8 the printed numbers (5: tea, 6: cat, 7: sleep) were wired
         * to completely different actions.
         */
        if (action == ACT_LEFT || action == ACT_UP) {
            this.roomCursor = (this.roomCursor + Room.HOT_COUNT - 1) % Room.HOT_COUNT;
            Sound.click();
            return -1;
        }
        if (action == ACT_RIGHT || action == ACT_DOWN) {
            this.roomCursor = (this.roomCursor + 1) % Room.HOT_COUNT;
            Sound.click();
            return -1;
        }
        if (action >= ACT_NUM1 && action < ACT_NUM1 + Room.HOT_COUNT) {
            this.roomCursor = action - ACT_NUM1;
            return this.roomCursor;
        }
        if (action == ACT_FIRE) {
            return this.roomCursor;
        }
        return -2;
    }

    private void inputRoom(int action) {
        if (action == ACT_BACK || action == ACT_SOFT2) {
            this.saveProgress();
            this.navBack();
            return;
        }
        if (action == ACT_SOFT1) {
            this.openExpList();
            return;
        }
        int hot = this.roomHotspot(action);
        if (hot == -1 || hot == -2) {
            return;
        }
        this.useRoomObject(hot);
    }

    /** Activates one object of the room. */
    private void useRoomObject(int hot) {
        switch (hot) {
            case Room.HOT_MONITOR:
                this.startSandboxBuild();
                break;
            case Room.HOT_MUG:
                this.mode = M_TEA;
                this.teaTick = 0;
                this.teaBrewed = 0;
                Sound.click();
                break;
            case Room.HOT_CAT:
                this.toastMsg(this.life.petCat());
                break;
            case Room.HOT_LAMP:
                this.toastMsg(this.life.toggleLamp());
                break;
            case Room.HOT_BED:
                this.mode = M_SLEEP;
                this.sleepTick = 0;
                Sound.click();
                break;
            case Room.HOT_DOOR:
                this.mode = M_CITY;
                this.city.travelTo(0, this.life);
                Sound.click();
                break;
            case Room.HOT_TOOLS:
                this.mode = M_WORKSHOP_TOOL;
                this.workshop.initForRepair(0);
                Sound.click();
                break;
            default:
                this.toastMsg(this.life.toggleRadio());
                break;
        }
    }

    /* ==================================================================
     *  tea screen
     * ================================================================== */

    private void inputTea(int action) {
        if (action == ACT_BACK || action == ACT_SOFT2) {
            this.navBack();
            return;
        }
        if (action == ACT_FIRE || action == ACT_SOFT1
                || action == ACT_NUM1) {
            String msg = this.life.drinkTea();
            this.teaTick = 0;
            this.teaBrewed = this.life.teaLevel <= 0 ? 1 : this.teaBrewed;
            this.toastMsg(msg);
            if (this.life.teaLevel <= 0) {
                // the cup is empty: press again to brew a fresh one
                this.teaBrewed = 1;
            }
        } else if (action == ACT_NUM2) {
            this.life.teaLevel = 100;
            this.life.teaHot = true;
            this.teaBrewed = 1;
            this.teaTick = 0;
            Sound.ok();
            this.toastMsg("\u0421\u0432\u0435\u0436\u0438\u0439 \u0447\u0430\u0439 \u0437\u0430\u0432\u0430\u0440\u0435\u043d");
        }
    }

    /* ==================================================================
     *  sleep screen
     * ================================================================== */

    private void inputSleep(int action) {
        if (action == ACT_BACK || action == ACT_SOFT2) {
            this.navBack();
            return;
        }
        if (action == ACT_FIRE || action == ACT_SOFT1) {
            // sleep stays on screen: the player sees the room darken and the
            // energy bar refill, then leaves with BACK
            this.toastMsg(this.life.sleep());
            this.sleepTick = 0;
            Sound.ok();
        }
    }
    
    private void openCategory(int i) {
        rpc.Db a = this.db;
        int[] a0 = new int[1];
        a0[0] = i;
        this.partList = a.query(a0, 0, 9999);
        this.partSel = 0;
        this.partTop = 0;
        this.mode = 8;
    }
    
    private void menuAction() {
        if (this.menuSel != 0) {
            if (this.menuSel != 1) {
                if (this.menuSel != 2) {
                    if (this.menuSel != 3) {
                        if (this.menuSel != 4) {
                            if (this.menuSel != 5) {
                                if (this.menuSel != 6) {
                                    if (this.menuSel == 7) {
                                        this.mode = 6;
                                    }
                                } else {
                                    this.mode = 52;
                                }
                            } else {
                                this.openExpList();
                            }
                        } else {
                            this.mode = 7;
                            this.catSel = 0;
                            this.catTop = 0;
                        }
                    } else {
                        this.mode = 49;
                    }
                } else {
                    this.mode = 48;
                }
            } else {
                this.startSandboxBuild();
            }
        } else {
            this.mode = 47;
        }
    }
    
    public void startSandboxBuild() {
        int i = 0;
        this.sandbox = true;
        rpc.Db a = this.db;
        label2: {
            label0: {
                label1: {
                    if (a == null) {
                        break label1;
                    }
                    if (this.db.levels == null) {
                        break label1;
                    }
                    if (this.db.levels.length > 0) {
                        break label0;
                    }
                }
                i = 0;
                break label2;
            }
            i = this.db.levels.length - 1;
        }
        this.levelIdx = i;
        this.budget = 9999999;
        this.cash = 9999999;
        this.day = 1;
        this.spent = 0;
        this.table = new rpc.Table();
        int i0 = this.life.curYear;
        this.tasks = rpc.Os.tasks(i0);
        this.slots = new rpc.Part[20];
        this.buildSel = 0;
        this.mode = 2;
        this.toastMsg(new StringBuffer("\u041f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430 ").append(i0).append(" \u0433.: \u0441\u0432\u043e\u0431\u043e\u0434\u043d\u0430\u044f \u0441\u0431\u043e\u0440\u043a\u0430, 56750 \u0434\u0435\u0442\u0430\u043b\u0435\u0439 \u0438 9,999,999 \u20bd!").toString());
    }
    
    private void startLevel(int i) {
        this.sandbox = false;
        this.levelIdx = i;
        int i0 = this.db.levels[i].budget;
        this.budget = i0;
        this.cash = i0;
        this.day = 1;
        this.spent = 0;
        this.minedTotal = 0;
        this.slots = new rpc.Part[20];
        this.table = new rpc.Table();
        this.tasks = rpc.Os.tasks(this.db.levels[i].year);
        this.taskSel = 0;
        this.editorN = 0;
        this.editorLang = 0;
        this.buildSel = 0;
        this.mode = 2;
    }
    
    private boolean toastActive() {
        if (this.toast != null && this.toast.length() > 0 && this.host.now() < this.toastUntil) {
            return true;
        }
        return false;
    }
    
    private void toastMsg(String s) {
        this.toast = s;
        this.toastUntil = this.host.now() + 2600L;
    }
    
    private void openShop(int i) {
        this.shopSlot = i;
        this.shopSel = 0;
        this.shopTop = 0;
        this.shopAllYears = false;
        this.rebuildShopList();
        this.mode = 3;
    }
    
    private int[] catsForSlot(int i) {
        if (i == 0) {
            int[] a = new int[1];
            a[0] = 1;
            return a;
        }
        if (i == 1) {
            int[] a0 = new int[1];
            a0[0] = 3;
            return a0;
        }
        if (i >= 2 && i < 6) {
            int[] a1 = new int[1];
            a1[0] = 4;
            return a1;
        }
        if (i == 6) {
            int[] a2 = new int[1];
            a2[0] = 2;
            return a2;
        }
        if (i >= 7 && i < 10) {
            int[] a3 = new int[4];
            a3[0] = 5;
            a3[1] = 7;
            a3[2] = 8;
            a3[3] = 6;
            return a3;
        }
        if (i == 10) {
            int[] a4 = new int[1];
            a4[0] = 9;
            return a4;
        }
        if (i == 11) {
            int[] a5 = new int[1];
            a5[0] = 10;
            return a5;
        }
        if (i == 12) {
            int[] a6 = new int[1];
            a6[0] = 11;
            return a6;
        }
        if (i == 13) {
            int[] a7 = new int[1];
            a7[0] = 12;
            return a7;
        }
        if (i == 14) {
            int[] a8 = new int[1];
            a8[0] = 13;
            return a8;
        }
        if (i == 15) {
            int[] a9 = new int[1];
            a9[0] = 15;
            return a9;
        }
        if (i == 16) {
            int[] a10 = new int[1];
            a10[0] = 16;
            return a10;
        }
        if (i == 17) {
            int[] a11 = new int[1];
            a11[0] = 14;
            return a11;
        }
        if (i == 19) {
            int[] a12 = new int[1];
            a12[0] = 20;
            return a12;
        }
        int[] a13 = new int[2];
        a13[0] = 18;
        a13[1] = 17;
        return a13;
    }
    
    public int[] debugQuery(int i, int i0) {
        rpc.Db a = this.db;
        int[] a0 = new int[1];
        a0[0] = i;
        return a.query(a0, i0 - 3, i0);
    }
    
    public int debugCount() {
        return this.db.count();
    }
    
    public int[] debugRange(int i, int i0, int i1) {
        rpc.Db a = this.db;
        int[] a0 = new int[1];
        a0[0] = i;
        return a.query(a0, i0, i1);
    }
    
    public rpc.Part debugPart(int i) {
        return this.db.get(i);
    }
    
    public String debugShopDump() {
        StringBuffer a = new StringBuffer(new StringBuffer("slot=").append(this.shopSlot).append(" len=").append(this.shopList.length).append(" all=").append(this.shopAllYears).append(" year=").append(this.levelYear()).toString());
        int i = 0;
        while(true) {
            if (i < this.shopList.length && i < 8) {
                rpc.Part a0 = this.db.get(this.shopList[i]);
                a.append("\n  ").append(this.shopList[i]).append((char)32).append((a0 != null) ? new StringBuffer(String.valueOf(a0.year)).append(" ").append(a0.name).append(" ").append(a0.price).toString() : "null");
                i = i + 1;
                continue;
            }
            return a.toString();
        }
    }
    
    private void rebuildShopList() {
        int i = this.levelYear();
        int i0 = (this.shopAllYears) ? 1980 : i - 6;
        if (this.shopAllYears) {
            i = 2026;
        }
        int[] a = this.db.query(this.catsForSlot(this.shopSlot), i0, i);
        int[] a0 = new int[a.length];
        int i1 = 0;
        int i2 = 0;
        while(true) {
            int i3 = a.length;
            int i4 = i1;
            if (i2 < i3) {
                rpc.Part a1 = this.db.get(a[i2]);
                if (a1 == null) {
                    i1 = i4;
                } else {
                    boolean b = rpc.Compat.fits(this.shopSlot, a1, this.slots);
                    i1 = i4;
                    if (b) {
                        i1 = i4 + 1;
                        a0[i4] = a[i2];
                    }
                }
                i2 = i2 + 1;
            } else {
                if (i1 <= 0) {
                    this.shopList = a;
                } else {
                    int[] a2 = new int[i1];
                    System.arraycopy((Object)a0, 0, (Object)a2, 0, i1);
                    this.shopList = a2;
                }
                if (this.shopSel >= this.shopList.length) {
                    this.shopSel = 0;
                }
                if (this.shopSel < 0) {
                    this.shopSel = 0;
                }
                this.shopTop = Math.max(0, this.shopSel - 3);
                return;
            }
        }
    }
    
    private void buySelected() {
        if (this.shopList.length == 0) {
            rpc.Sound.error();
            this.toastMsg("\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e");
            return;
        }
        rpc.Part a = this.db.get(this.shopList[this.shopSel]);
        if (a.price > this.cash) {
            this.toastMsg(new StringBuffer("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432: \u043d\u0443\u0436\u043d\u043e ").append(a.price).append(", \u0435\u0441\u0442\u044c ").append(this.cash).toString());
            this.host.vibrate(60);
            return;
        }
        String s = rpc.Compat.why(this.shopSlot, a, this.slots);
        if (s != null) {
            rpc.Sound.error();
            this.host.vibrate(80);
            this.toastMsg(new StringBuffer("\u041d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442: ").append(s).toString());
            return;
        }
        rpc.Part a0 = this.slots[this.shopSlot];
        this.cash = this.cash - a.price;
        if (a0 != null) {
            this.cash = this.cash + a0.price * 70 / 100;
        }
        this.slots[this.shopSlot] = a;
        this.recalcSpent();
        rpc.Sound.ok();
        this.mode = 2;
    }
    
    private void clearSlot(int i) {
        rpc.Part a = this.slots[i];
        if (a != null) {
            this.cash = this.cash + a.price * 70 / 100;
        }
        this.slots[i] = null;
        this.recalcSpent();
    }
    
    private void recalcSpent() {
        int i = 0;
        int i0 = 0;
        while(i0 < 20) {
            if (this.slots[i0] != null) {
                i = i + this.slots[i0].price;
            }
            i0 = i0 + 1;
        }
        this.spent = i;
    }
    
    private rpc.Db$Level effectiveLevel() {
        if (!this.sandbox) {
            return this.db.levels[this.levelIdx];
        }
        int i = 0;
        int i0 = 0;
        while(i0 < 20) {
            if (this.slots[i0] != null && this.slots[i0].year > i && this.slots[i0].cat != 10) {
                i = this.slots[i0].year;
            }
            i0 = i0 + 1;
        }
        if (i <= 0) {
            return this.db.levels[this.db.levels.length - 1];
        }
        rpc.Db$Level a = this.db.levels[0];
        int i1 = 0;
        while(i1 < this.db.levels.length) {
            if (this.db.levels[i1].year <= i) {
                a = this.db.levels[i1];
            }
            i1 = i1 + 1;
        }
        return a;
    }
    
    private void runTest() {
        this.resultFrames = 6;
        this.result = rpc.Score.evaluate(this.effectiveLevel(), this.slots);
        this.resultStart = this.host.now();
        this.resultPhase = 0;
        this.resultFrames = 0;
        this.mode = 4;
        boolean b = this.result.ok;
        label2: {
            label0: {
                label1: {
                    if (!b) {
                        break label1;
                    }
                    if (!this.sandbox) {
                        break label0;
                    }
                }
                this.host.vibrate(120);
                break label2;
            }
            int b0 = this.best[this.levelIdx];
            if (this.result.score > b0) {
                this.best[this.levelIdx] = this.result.score;
                this.totalXp = this.totalXp + this.result.score;
            }
            this.saveProgress();
        }
    }
    
    public void openExpList() {
        this.filterExps();
        this.expSel = rpc.Util.clamp(this.expSel, 0, Math.max(0, this.expFilteredCount - 1));
        this.expTop = 0;
        this.mode = 45;
    }
    
    private void filterExps() {
        this.expFilteredCount = 0;
        int i = 0;
        while(i < 1000) {
            int i0 = this.expFilterPlat;
            label0: {
                label2: {
                    if (i0 == -1) {
                        break label2;
                    }
                    if (rpc.Exp.platform(i) == this.expFilterPlat) {
                        break label2;
                    }
                    break label0;
                }
                int i1 = this.expFilterKind;
                label1: {
                    if (i1 == -1) {
                        break label1;
                    }
                    if (rpc.Exp.kind(i) != this.expFilterKind) {
                        break label0;
                    }
                }
                int[] a = this.expFiltered;
                int i2 = this.expFilteredCount;
                this.expFilteredCount = i2 + 1;
                a[i2] = i;
            }
            i = i + 1;
        }
        if (this.expFilteredCount == 0) {
            int i3 = 0;
            while(i3 < 1000) {
                this.expFiltered[i3] = i3;
                i3 = i3 + 1;
            }
            this.expFilteredCount = 1000;
        }
    }
    
    public void openExpRun(int i) {
        label0: {
            label1: {
                if (i < 0) {
                    break label1;
                }
                if (i < 1000) {
                    break label0;
                }
            }
            i = 0;
        }
        this.expStress.load(i);
        this.expTab = 0;
        this.mode = 46;
    }
    
    /**
     * Experiment lab index: 1000 experiments, so the row has to carry a lot of
     * information in little width - number, title, platform and reward, with a
     * progress ring and a compact filter bar.
     */
    private void paintExpList() {
        int w = this.W;
        int h = this.H;
        this.header("\u041b\u0430\u0431\u043e\u0440\u0430\u0442\u043e\u0440\u0438\u044f",
                this.expTotalPassed + "/1000  XP " + this.expTotalXp);

        int top = this.FH1 + 12;
        String plat = (this.expFilterPlat != -1)
                ? rpc.Exp.platformName(this.expFilterPlat) : "\u0432\u0441\u0435";
        String kind = (this.expFilterKind != -1)
                ? rpc.Exp.kindName(this.expFilterKind) : "\u0432\u0441\u0435";
        int barH = this.FH0 + 8;
        this.g.setColor(Theme.PANEL);
        this.g.fillRect(4, top, w - 8, barH);
        this.g.setColor(Theme.LINE);
        this.g.drawLine(4, top + barH - 1, w - 4, top + barH - 1);
        Text.draw(this.g, "\u25c4", 8, top + 4, 0, Theme.ACCENT2);
        Text.draw(this.g, Text.fit(plat, 0, w / 2 - 40), 16, top + 4, 0,
                Theme.ACCENT2);
        Text.draw(this.g, "\u25ba", w / 2 - 14, top + 4, 0, Theme.ACCENT2);
        Text.draw(this.g, "\u25c4", w / 2 + 2, top + 4, 0, Theme.ACCENT);
        Text.draw(this.g, Text.fit(kind, 0, w / 2 - 46), w / 2 + 10, top + 4, 0,
                Theme.ACCENT);
        Text.draw(this.g, "\u25ba", w - 14, top + 4, 0, Theme.ACCENT);

        int listTop = top + barH + 3;
        int footerTop = h - this.FH0 - 12;
        int rowH = this.FH0 * 2 + 9;
        int rows = (footerTop - listTop) / rowH;
        if (rows < 1) {
            rows = 1;
        }
        if (this.expSel < this.expTop) {
            this.expTop = this.expSel;
        }
        if (this.expSel >= this.expTop + rows) {
            this.expTop = this.expSel - rows + 1;
        }
        for (int r = 0; r < rows; r++) {
            int idx = this.expTop + r;
            if (idx >= this.expFilteredCount) {
                break;
            }
            int id = this.expFiltered[idx];
            boolean sel = idx == this.expSel;
            boolean passed = this.expPassed[id];
            int ry = listTop + r * rowH;
            this.g.setColor(sel ? Theme.PANEL2 : Theme.PANEL);
            this.g.fillRoundRect(4, ry, w - 8, rowH - 2, 3);
            if (sel) {
                this.g.setColor(Theme.ACCENT);
                this.g.drawRoundRect(4, ry, w - 8, rowH - 2, 3);
                this.g.fillRect(4, ry + 2, 2, rowH - 6);
            }
            // status chip
            this.g.setColor(passed ? Theme.GREEN : Theme.BG);
            this.g.fillRoundRect(8, ry + 4, 12, 12, 2);
            if (passed) {
                this.g.setColor(0x0E1A12);
                this.g.drawLine(11, ry + 10, 13, ry + 13);
                this.g.drawLine(13, ry + 13, 18, ry + 6);
            } else {
                this.g.setColor(Theme.LINE);
                this.g.drawRoundRect(8, ry + 4, 12, 12, 2);
            }
            // line 1: number + title, reward on the right
            String num = "#" + (id + 1);
            Text.draw(this.g, num, 24, ry + 3, 0, passed ? Theme.GREEN : Theme.MUTED);
            String xp = "+" + rpc.Exp.xp(id) + " XP";
            int xw = this.g.textWidth(xp, 0);
            Text.draw(this.g, xp, w - xw - 8, ry + 3, 0,
                    passed ? Theme.MUTED : Theme.ACCENT);
            int tx = 24 + this.g.textWidth("#1000 ", 0);
            Text.draw(this.g, Text.fit(rpc.Exp.title(id), 0, w - tx - xw - 14),
                    tx, ry + 3, 0, sel ? Theme.TEXT : Theme.DIM);
            // line 2: platform and year
            String meta = rpc.Exp.platformName(rpc.Exp.platform(id)) + " \u2022 "
                    + rpc.Exp.year(id) + " \u0433.";
            Text.draw(this.g, Text.fit(meta, 0, w - 32), 24,
                    ry + 4 + this.FH0, 0, Theme.MUTED);
        }
        this.footer("\u0432\u044b\u0431\u043e\u0440", "OK: \u0437\u0430\u043f\u0443\u0441\u043a");
    }

    
    private void inputExpList(int action) {
        // RIGHT/LEFT jump a whole page, 2/3 filter by platform/kind
        if (action == ACT_RIGHT) {
            this.expSel = Math.min(this.expFilteredCount - 1, this.expSel + 10);
            return;
        }
        if (action == ACT_LEFT) {
            this.expSel = Math.max(0, this.expSel - 10);
            return;
        }
        int i = action;
        label0: if (i != 0) {
            if (i != 1) {
                if (i != 2) {
                    if (i != 3) {
                        if (i != 4) {
                            label1: {
                                if (i == 7) {
                                    break label1;
                                }
                                if (i == 5) {
                                    break label1;
                                }
                                if (i != 6) {
                                    break label0;
                                }
                            }
                            this.mode = 0;
                        } else if (this.expFilteredCount > 0 && this.expSel >= 0 && this.expSel < this.expFilteredCount) {
                            this.openExpRun(this.expFiltered[this.expSel]);
                        }
                    } else {
                        this.expFilterKind = this.expFilterKind + 1;
                        if (this.expFilterKind >= 5) {
                            this.expFilterKind = -1;
                        }
                        this.filterExps();
                        this.expSel = 0;
                        this.expTop = 0;
                    }
                } else {
                    this.expFilterPlat = this.expFilterPlat + 1;
                    if (this.expFilterPlat >= 5) {
                        this.expFilterPlat = -1;
                    }
                    this.filterExps();
                    this.expSel = 0;
                    this.expTop = 0;
                }
            } else if (this.expFilteredCount > 0) {
                this.expSel = (this.expSel + 1) % this.expFilteredCount;
            }
        } else if (this.expFilteredCount > 0) {
            this.expSel = (this.expSel + this.expFilteredCount - 1) % this.expFilteredCount;
        }
    }
    
    private void pointerExpList(int i, int i0) {
        int i1 = this.FH1 + 10;
        int i2 = this.FH0 + 9;
        if (i0 >= i1 && i0 < i1 + i2) {
            if (i >= this.W / 2) {
                this.inputExpList(3);
            } else {
                this.inputExpList(2);
            }
            return;
        }
        int i3 = this.FH0 * 2 + 6;
        int i4 = i1 + i2;
        int i5 = this.expTop + (i0 - i4) / i3;
        if (i5 >= 0 && i5 < this.expFilteredCount) {
            if (this.expSel != i5) {
                this.expSel = i5;
            } else {
                this.openExpRun(this.expFiltered[i5]);
            }
        }
    }
    
    private void drawDeviceDisplay(int i, int i0, int i1, int i2, int i3, int i4) {
        int i5 = rpc.Exp.platform(i3);
        int i6 = rpc.Exp.year(i3);
        int i7 = rpc.Exp.kind(i3);
        int i8 = this.expStress.frameCount;
        int i9 = (i6 >= 1995) ? (i6 >= 2008) ? 1974826 : 2764342 : 13944488;
        int i10 = (i6 >= 1995) ? (i6 >= 2008) ? 658706 : 1316380 : 9996396;
        if (i5 != 3) {
            if (i5 != 2) {
                if (i5 != 4) {
                    if (i5 != 0) {
                        int i11 = (i6 >= 2005) ? 1843240 : 13024934;
                        this.g.setColor(i11);
                        this.g.fillRoundRect(i + 8, i0, i1 - 16, i2, 6);
                        this.g.setColor(790036);
                        this.g.drawRoundRect(i + 8, i0, i1 - 16, i2, 6);
                        this.g.setColor(329482);
                        this.g.fillRect(i + i1 / 2 - 1, i0 + 2, 3, 2);
                        this.drawActiveScreen(i + 14, i0 + 6, i1 - 28, i2 - 16, i3, i4, i8, i7, i6);
                        int i12 = i0 + i2 - 8;
                        this.g.setColor(3686476);
                        this.g.fillRoundRect(i + i1 / 2 - 6, i12, 12, 5, 2);
                    } else {
                        int i13 = (i6 >= 2002) ? 2238000 : 8949912;
                        this.g.setColor(i13);
                        this.g.fillRoundRect(i + 10, i0, i1 - 20, i2, 8);
                        this.g.setColor(1053722);
                        this.g.drawRoundRect(i + 10, i0, i1 - 20, i2, 8);
                        this.g.setColor(11581636);
                        this.g.fillRect(i + i1 - 16, i0 - 2, 4, 4);
                        int i14 = i2 * 64 / 100;
                        int i15 = i + 16;
                        int i16 = i0 + 6;
                        this.drawActiveScreen(i15, i16, i1 - 32, i14, i3, i4, i8, i7, i6);
                        int i17 = i16 + i14 + 3;
                        int i18 = i2 - (i17 - i0) - 4;
                        int i19 = (i1 - 48) / 4;
                        int i20 = 0;
                        while(i20 < 4) {
                            int i21 = i + 18 + i20 * (i19 + 3);
                            this.g.setColor(4212820);
                            this.g.fillRoundRect(i21, i17, i19, Math.max(3, i18 / 2), 2);
                            i20 = i20 + 1;
                        }
                        if (i18 > 8) {
                            int i22 = i + i1 / 2 - 5;
                            int i23 = i17 + i18 / 2 + 1;
                            this.g.setColor(6318712);
                            this.g.fillRoundRect(i22, i23, 10, Math.max(3, i18 / 2 - 2), 2);
                            this.g.setColor(2289254);
                            this.g.fillRect(i22 + 4, i23 + 1, 2, 2);
                        }
                    }
                } else {
                    int i24 = 0;
                    int i25 = i1 * 68 / 100;
                    int i26 = i1 - i25 - 6;
                    int i27 = i2 * 82 / 100;
                    int i28 = (i6 >= 1998) ? (i6 >= 2008) ? 1447966 : 2764344 : 14471866;
                    int i29 = (i6 >= 1998) ? 790034 : 9997942;
                    this.g.setColor(i28);
                    this.g.fillRoundRect(i, i0, i25, i27, 4);
                    this.g.setColor(i29);
                    this.g.drawRoundRect(i, i0, i25, i27, 4);
                    this.drawActiveScreen(i + 5, i0 + 5, i25 - 10, i27 - 12, i3, i4, i8, i7, i6);
                    this.g.setColor((i4 != 1) ? 8951944 : 2289220);
                    this.g.fillRect(i + i25 - 10, i0 + i27 - 5, 4, 3);
                    int i30 = i0 + i27;
                    this.g.setColor(i29);
                    this.g.fillRect(i + i25 / 2 - 8, i30, 16, i2 - i27);
                    this.g.fillRect(i + i25 / 2 - 14, i0 + i2 - 2, 28, 2);
                    int i31 = i + i25 + 6;
                    int i32 = i0 + 2;
                    int i33 = i2 - 4;
                    int i34 = (i6 >= 1998) ? (i6 >= 2008) ? 1184794 : 2238000 : 13681832;
                    int i35 = (i6 >= 1998) ? 526862 : 9340010;
                    this.g.setColor(i34);
                    this.g.fillRoundRect(i31, i32, i26, i33, 3);
                    this.g.setColor(i35);
                    this.g.drawRoundRect(i31, i32, i26, i33, 3);
                    int i36 = Math.max(4, i33 / 8);
                    int i37 = 0;
                    while(i37 < 2) {
                        int i38 = i32 + 4 + i37 * (i36 + 3);
                        this.g.setColor(1579552);
                        this.g.fillRect(i31 + 3, i38, i26 - 6, i36);
                        this.g.setColor(4211792);
                        this.g.drawRect(i31 + 3, i38, i26 - 6, i36);
                        if (i37 == 0) {
                            this.g.setColor(2280516);
                            this.g.fillRect(i31 + i26 - 6, i38 + i36 - 2, 2, 1);
                        }
                        i37 = i37 + 1;
                    }
                    int i39 = i32 + i33 * 55 / 100;
                    this.g.setColor(4212820);
                    this.g.fillRoundRect(i31 + 4, i39, i26 - 8, 8, 2);
                    this.g.setColor(2289254);
                    this.g.fillRect(i31 + 6, i39 + 3, 3, 2);
                    rpc.Gfx a = this.g;
                    int i40 = i8 % 3;
                    label8: {
                        label6: {
                            label7: {
                                if (i40 != 0) {
                                    break label7;
                                }
                                if (i4 == 1) {
                                    break label6;
                                }
                            }
                            i24 = 5574929;
                            break label8;
                        }
                        i24 = 16720418;
                    }
                    a.setColor(i24);
                    this.g.fillRect(i31 + 12, i39 + 3, 3, 2);
                    int i41 = i32 + i33 * 72 / 100;
                    int i42 = i33 - (i41 - i32) - 4;
                    if (i42 > 4) {
                        if (i6 < 2017) {
                            this.g.setColor(1579552);
                            this.g.fillRect(i31 + 4, i41, i26 - 8, i42);
                            int i43 = i41 + 2;
                            while(i43 < i41 + i42 - 1) {
                                this.g.setColor(790034);
                                this.g.drawLine(i31 + 5, i43, i31 + i26 - 5, i43);
                                i43 = i43 + 3;
                            }
                        } else {
                            int i44 = (i8 % 3 != 0) ? (i8 % 3 != 1) ? 7798531 : 16711850 : 58879;
                            this.g.setColor(i44);
                            this.g.drawRect(i31 + 4, i41, i26 - 8, i42);
                        }
                    }
                }
            } else {
                int i45 = i2 * 56 / 100;
                int i46 = i2 - i45 - 4;
                int i47 = (i6 >= 2010) ? 2633788 : 15396596;
                this.g.setColor(i47);
                this.g.fillRoundRect(i + 6, i0, i1 - 12, i45, 4);
                this.g.setColor(6320256);
                this.g.drawRoundRect(i + 6, i0, i1 - 12, i45, 4);
                this.g.setColor(8951976);
                int i48 = i0 + 10;
                while(i48 < i0 + i45 - 10) {
                    this.g.fillRect(i + 10, i48, 2, 2);
                    this.g.fillRect(i + i1 - 12, i48, 2, 2);
                    i48 = i48 + 3;
                }
                this.drawActiveScreen(i + 16, i0 + 6, i1 - 32, i45 - 12, i3, i4, i8, i7, i6);
                int i49 = i0 + i45;
                this.g.setColor(4212820);
                this.g.fillRect(i + 12, i49, i1 - 24, 4);
                int i50 = i49 + 4;
                this.g.setColor(i47);
                this.g.fillRoundRect(i + 2, i50, i1 - 4, i46, 4);
                this.g.setColor(6320256);
                this.g.drawRoundRect(i + 2, i50, i1 - 4, i46, 4);
                int i51 = i1 - 20;
                int i52 = Math.max(6, i46 * 48 / 100);
                this.g.setColor(1580066);
                this.g.fillRect(i + 10, i50 + 3, i51, i52);
                int i53 = Math.max(14, i1 / 5);
                int i54 = Math.max(4, i46 - i52 - 7);
                int i55 = i + (i1 - i53) / 2;
                int i56 = i50 + i52 + 4;
                this.g.setColor(13686494);
                this.g.fillRect(i55, i56, i53, i54);
                this.g.setColor(8950946);
                this.g.drawRect(i55, i56, i53, i54);
                this.g.setColor(41727);
                this.g.fillRect(i + 8, i50 + i46 - 4, 3, 2);
            }
        } else {
            int i57 = 0;
            int i58 = 0;
            int i59 = i2 * 58 / 100;
            int i60 = i2 - i59 - 4;
            this.g.setColor(i9);
            this.g.fillRoundRect(i + 4, i0, i1 - 8, i59, 4);
            this.g.setColor(i10);
            this.g.drawRoundRect(i + 4, i0, i1 - 8, i59, 4);
            this.g.setColor(329482);
            this.g.fillRect(i + i1 / 2 - 2, i0 + 2, 4, 3);
            this.g.setColor(2254506);
            this.g.fillRect(i + i1 / 2 - 1, i0 + 3, 2, 1);
            this.drawActiveScreen(i + 10, i0 + 7, i1 - 20, i59 - 12, i3, i4, i8, i7, i6);
            int i61 = i0 + i59;
            this.g.setColor(1580066);
            this.g.fillRect(i + 16, i61, i1 - 32, 4);
            int i62 = i61 + 4;
            this.g.setColor(i9);
            this.g.fillRoundRect(i, i62, i1, i60, 4);
            this.g.setColor(i10);
            this.g.drawRoundRect(i, i62, i1, i60, 4);
            int i63 = i1 - 16;
            int i64 = Math.max(8, i60 * 50 / 100);
            this.g.setColor(1184792);
            this.g.fillRect(i + 8, i62 + 3, i63, i64);
            int i65 = (i63 - 4) / 10;
            int i66 = 0;
            while(i66 < 2) {
                int i67 = 0;
                while(i67 < 10) {
                    int i68 = i + 10 + i67 * i65;
                    int i69 = i62 + 5 + i66 * (i64 / 2);
                    int i70 = (i6 >= 1995) ? 2895930 : 15590094;
                    this.g.setColor(i70);
                    this.g.fillRect(i68, i69, Math.max(1, i65 - 2), Math.max(1, i64 / 2 - 2));
                    i67 = i67 + 1;
                }
                i66 = i66 + 1;
            }
            if (i6 >= 1992 && i6 <= 2012) {
                this.g.setColor(15606306);
                this.g.fillRect(i + i1 / 2 - 1, i62 + 5 + i64 / 4, 3, 3);
            }
            int i71 = Math.max(16, i1 / 4);
            int i72 = Math.max(4, i60 - i64 - 8);
            int i73 = i + (i1 - i71) / 2;
            int i74 = i62 + i64 + 5;
            this.g.setColor(2632758);
            this.g.fillRect(i73, i74, i71, i72);
            this.g.setColor(i10);
            this.g.drawRect(i73, i74, i71, i72);
            rpc.Gfx a0 = this.g;
            int i75 = i8 % 6;
            label5: {
                label3: {
                    label4: {
                        if (i75 >= 3) {
                            break label4;
                        }
                        if (i4 == 1) {
                            break label3;
                        }
                    }
                    i57 = 1140258;
                    break label5;
                }
                i57 = 2289254;
            }
            a0.setColor(i57);
            this.g.fillRect(i + 10, i62 + i60 - 4, 3, 2);
            rpc.Gfx a1 = this.g;
            int i76 = i8 % 4;
            label2: {
                label0: {
                    label1: {
                        if (i76 != 0) {
                            break label1;
                        }
                        if (i4 == 1) {
                            break label0;
                        }
                    }
                    i58 = 6702097;
                    break label2;
                }
                i58 = 16755234;
            }
            a1.setColor(i58);
            this.g.fillRect(i + 15, i62 + i60 - 4, 3, 2);
            this.g.setColor(2263278);
            this.g.fillRect(i + 20, i62 + i60 - 4, 3, 2);
        }
    }
    
    private void drawActiveScreen(int i, int i0, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
        if (i1 > 0 && i2 > 0) {
            int i8 = 0;
            label2: {
                label3: {
                    if (i7 >= 1990) {
                        break label3;
                    }
                    if (i6 != 0) {
                        break label3;
                    }
                    i8 = 0;
                    break label2;
                }
                label0: {
                    label1: {
                        if (i7 >= 1998) {
                            break label1;
                        }
                        if (i4 != 1) {
                            break label1;
                        }
                        if (i6 == 0) {
                            break label0;
                        }
                    }
                    i8 = 528408;
                    break label2;
                }
                i8 = 132;
            }
            this.g.setColor(i8);
            this.g.fillRect(i, i0, i1, i2);
            if (i4 != 0) {
                if (i4 != 1) {
                    if (i4 != 2) {
                        if (i4 != 3) {
                            if (i4 != 4) {
                                if (i4 == 5) {
                                    this.g.setColor(5570560);
                                    this.g.fillRect(i, i0, i1, i2);
                                    this.g.setColor(16724787);
                                    this.g.drawText("! \u0421\u0412\u0415\u0420\u0425\u0412\u041e\u041b\u042c\u0422\u0410\u0416 \u0418 \u041f\u0420\u041e\u0413\u0410\u0420 !", i + 4, i0 + 4, 0);
                                    this.g.setColor(16772829);
                                    this.g.drawText("VCore \u0432\u044b\u0448\u0435 \u043d\u043e\u0440\u043c\u044b: VRM FAIL", i + 4, i0 + 16, 0);
                                }
                            } else {
                                this.g.setColor(4465152);
                                this.g.fillRect(i, i0, i1, i2);
                                this.g.setColor(16755200);
                                this.g.drawText(new StringBuffer("! \u041f\u0415\u0420\u0415\u0413\u0420\u0415\u0412: ").append(this.expStress.temp).append(" C !").toString(), i + 4, i0 + 4, 0);
                                this.g.setColor(16772829);
                                this.g.drawText("\u0421\u0431\u0440\u043e\u0441 \u0447\u0430\u0441\u0442\u043e\u0442\u044b \u0442\u0440\u043e\u0442\u0442\u043b\u0438\u043d\u0433\u043e\u043c", i + 4, i0 + 16, 0);
                            }
                        } else {
                            this.g.setColor(170);
                            this.g.fillRect(i, i0, i1, i2);
                            this.g.setColor(16777215);
                            this.g.drawText("*** STOP: 0x00000101", i + 4, i0 + 4, 0);
                            this.g.drawText("CLOCK_WATCHDOG_TIMEOUT", i + 4, i0 + 16, 0);
                        }
                    } else {
                        this.g.setColor(931872);
                        this.g.fillRect(i, i0, i1, i2);
                        this.g.setColor(3793530);
                        this.g.drawRect(i + 1, i0 + 1, i1 - 2, i2 - 2);
                        this.g.drawText(new StringBuffer("\u2605 \u0423\u0421\u041f\u0415\u0425: ").append(this.expStress.score).append(" pts \u2605").toString(), i + 4, i0 + 4, 0);
                        this.g.setColor(15662984);
                        this.g.drawText(new StringBuffer("+").append(rpc.Exp.xp(i3)).append(" XP \u0412 \u041b\u0410\u0411\u041e\u0420\u0410\u0422\u041e\u0420\u0418\u042e").toString(), i + 4, i0 + 16, 0);
                    }
                } else if (i6 != 0) {
                    if (i6 != 1) {
                        if (i6 != 2) {
                            if (i6 != 4) {
                                this.g.setColor(3793530);
                                this.g.drawText(new StringBuffer("\u0411\u0415\u041d\u0427\u041c\u0410\u0420\u041a: ").append(this.expStress.progress).append("%").toString(), i + 4, i0 + 2, 0);
                                this.g.setColor(8297134);
                                this.g.drawText(new StringBuffer("\u0421\u0447\u0451\u0442: ").append(this.expStress.score).append(" pts").toString(), i + 4, i0 + 13, 0);
                                int i9 = Math.min(8, i1 / 8);
                                if (i9 < 1) {
                                    i9 = 1;
                                }
                                int i10 = (i1 - 10) / i9;
                                int i11 = 0;
                                while(i11 < i9) {
                                    boolean b = i11 * 100 / i9 <= this.expStress.progress;
                                    this.g.setColor(b ? 2271829 : 1451058);
                                    this.g.fillRect(i + 5 + i11 * i10, i0 + Math.max(24, i2 - 10), i10 - 2, 6);
                                    i11 = i11 + 1;
                                }
                            } else {
                                this.g.setColor(16755200);
                                this.g.drawText(new StringBuffer("\u0425\u0410\u041a: ").append(rpc.Exp.targetName(i3)).toString(), i + 4, i0 + 2, 0);
                                int i12 = i + 8;
                                int i13 = i0 + 13;
                                int i14 = Math.min(i1 - 16, 60);
                                int i15 = Math.max(8, i2 - 16);
                                this.g.setColor(1589284);
                                this.g.fillRect(i12, i13, i14, i15);
                                this.g.setColor(2646070);
                                this.g.drawRect(i12, i13, i14, i15);
                                int i16 = i12 + 4;
                                while(i16 < i12 + i14 - 4) {
                                    int i17 = i13 + 3;
                                    while(i17 < i13 + i15 - 2) {
                                        this.g.setColor(13934640);
                                        this.g.fillRect(i16, i17, 2, 2);
                                        i17 = i17 + 4;
                                    }
                                    i16 = i16 + 6;
                                }
                                int i18 = (i5 % 2 != 0) ? 16724736 : 65535;
                                this.g.setColor(i18);
                                this.g.drawLine(i12 + 10, i13 + 5, i12 + i14 - 12, i13 + i15 - 4);
                                this.g.fillRect(i12 + i14 / 2 - 3, i13 + i15 / 2 - 2, 6, 4);
                                this.g.setColor(3793530);
                                this.g.drawText("\u0421\u0418\u0413\u041d\u0410\u041b: OK", i + i14 + 12, i0 + 14, 0);
                                this.g.setColor(8297134);
                                this.g.drawText("\u0411\u0438\u043e\u0441: MOD", i + i14 + 12, i0 + 24, 0);
                            }
                        } else {
                            this.g.setColor(16755200);
                            this.g.drawText(new StringBuffer("\u0422\u0415\u0420\u041c\u041e\u0413\u0420\u0410\u041c\u041c\u0410: ").append(this.expStress.temp).append("\u00b0C").toString(), i + 4, i0 + 2, 0);
                            int i19 = i + 6;
                            int i20 = i0 + 13;
                            int i21 = i1 - 12;
                            int i22 = Math.max(8, i2 - 16);
                            this.g.setColor(2232576);
                            this.g.fillRect(i19, i20, i21, i22);
                            this.g.setColor(4465152);
                            this.g.drawRect(i19, i20, i21, i22);
                            int i23 = Math.min(i21 / 3, 20);
                            int i24 = 0;
                            while(i24 < i23) {
                                int i25 = i19 + i24 * i21 / i23;
                                int i26 = (int)(Math.sin((double)(i5 + i24) * 0.5) * (double)(i22 / 3));
                                int i27 = (this.expStress.temp <= 80) ? (this.expStress.temp <= 60) ? 3793530 : 16755200 : 16720418;
                                int i28 = i20 + i22 / 2 - i26;
                                this.g.setColor(i27);
                                this.g.fillRect(i25, i28, Math.max(2, i21 / i23 - 1), 2);
                                i24 = i24 + 1;
                            }
                        }
                    } else {
                        this.g.setColor(3793530);
                        this.g.drawText(new StringBuffer("FPS: ").append(this.expStress.fps).append(" \u2022 3D RENDER").toString(), i + 4, i0 + 2, 0);
                        int i29 = i + i1 / 2;
                        int i30 = i0 + i2 / 2 + 4;
                        int i31 = Math.min(i1, i2) / 3;
                        this.g.setColor(65416);
                        this.g.drawRect(i29 - i31 / 2, i30 - i31 / 2, i31, i31);
                        this.g.setColor(35071);
                        this.g.drawLine(i29 - i31, i30, i29 + i31, i30);
                        this.g.drawLine(i29, i30 - i31, i29, i30 + i31);
                    }
                } else {
                    this.g.setColor(3793530);
                    this.g.drawText(this.fit(new StringBuffer("\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 ").append(rpc.Exp.targetName(i3)).toString(), i1 - 8, 0), i + 4, i0 + 3, 0);
                    this.g.setColor(5679336);
                    int i32 = i5 % 4;
                    String s = (i32 != 1) ? (i32 != 2) ? (i32 != 3) ? "" : "..." : ".." : ".";
                    this.g.drawText(new StringBuffer("ACPI kernel init").append(s).toString(), i + 4, i0 + 14, 0);
                    int i33 = i1 - 12;
                    int i34 = i33 * this.expStress.progress / 100;
                    this.g.setColor(2109504);
                    this.g.fillRect(i + 6, i0 + Math.max(26, i2 - 10), i33, 6);
                    this.g.setColor(3793530);
                    this.g.fillRect(i + 6, i0 + Math.max(26, i2 - 10), i34, 6);
                }
            } else {
                this.g.setColor(3793530);
                this.g.drawText(new StringBuffer("BIOS POST \u2022 ").append(rpc.Exp.platformName(i3)).toString(), i + 4, i0 + 3, 0);
                this.g.setColor(8297134);
                this.g.drawText(this.fit(new StringBuffer("CPU: ").append(rpc.Exp.baseFreq(i3)).append("MHz | ").append(rpc.Exp.vcore(i3)).append("mV").toString(), i1 - 8, 0), i + 4, i0 + 13, 0);
                this.g.drawText(this.fit(new StringBuffer("\u0426\u0435\u043b\u044c: ").append(rpc.Exp.targetName(i3)).toString(), i1 - 8, 0), i + 4, i0 + 23, 0);
                if (i5 % 4 < 2) {
                    this.g.setColor(3793530);
                    this.g.drawText("READY_ [5/OK: \u0421\u0422\u0410\u0420\u0422]", i + 4, i0 + Math.max(33, i2 - 10), 0);
                }
            }
            return;
        }
    }
    
    void paintExpRun() {
        int i = this.expStress.expId;
        this.header(new StringBuffer(String.valueOf((Object)rpc.Exp.platformName(i))).append(": ").append(rpc.Exp.kindName(i)).toString(), new StringBuffer("#").append(i + 1).append(" \u2022 ").append(rpc.Exp.year(i)).append(" \u0433.").toString());
        int i0 = this.FH1 + 8;
        this.g.setColor(3793530);
        this.g.drawText(this.fit(rpc.Exp.targetName(i), this.W - 12, 1), 6, i0, 1);
        int i1 = i0 + (this.FH1 + 2);
        this.g.setColor(8297134);
        this.g.drawText(this.fit(rpc.Exp.goal(i), this.W - 12, 0), 6, i1, 0);
        int i2 = i1 + (this.FH0 + 3);
        int i3 = Math.max(76, Math.min(130, this.H / 3));
        if (this.expStress.status == 1) {
            this.expStress.tick();
        }
        if (this.expStress.status == 2 && !this.expPassed[i]) {
            this.expPassed[i] = true;
            this.expTotalPassed = this.expTotalPassed + 1;
            this.expTotalXp = this.expTotalXp + rpc.Exp.xp(i);
            this.saveProgress();
            rpc.Sound.ok();
        }
        this.drawDeviceDisplay(4, i2, this.W - 8, i3, i, this.expStress.status);
        int i4 = i2 + (i3 + 4);
        this.g.setColor(1780536);
        this.g.fillRect(4, i4, this.W - 8, this.FH0 * 2 + 5);
        this.g.setColor(3793530);
        this.g.drawText(new StringBuffer("CPU: ").append(this.expStress.curFreq).append(" \u041c\u0413\u0446").toString(), 8, i4 + 2, 0);
        int i5 = (this.expStress.temp <= 80) ? (this.expStress.temp <= 60) ? 3793530 : 14522658 : 16733525;
        this.g.setColor(i5);
        this.g.drawText(new StringBuffer("T: ").append(this.expStress.temp).append(" C (").append(this.expStress.power).append("W)").toString(), 110, i4 + 2, 0);
        this.g.setColor(16763196);
        this.g.drawText(new StringBuffer("VCore: ").append(this.expStress.curVoltage).append(" \u043c\u0412").toString(), 8, i4 + this.FH0 + 3, 0);
        this.g.setColor(8297134);
        this.g.drawText(this.fit(rpc.ExpStress.COOLING_NAMES[this.expStress.curCooling], this.W - 120, 0), 110, i4 + this.FH0 + 3, 0);
        int i6 = i4 + (this.FH0 * 2 + 7);
        String[] a = new String[4];
        a[0] = "\u0420\u0410\u0417\u0413\u041e\u041d";
        a[1] = "\u041a\u0423\u041b\u0415\u0420";
        a[2] = "\u0422\u0412\u0418\u041a\u0418";
        a[3] = "\u041b\u041e\u0413";
        int i7 = (this.W - 8) / a.length;
        int i8 = 0;
        while(i8 < a.length) {
            int i9 = 4 + i8 * i7;
            boolean b = i8 == this.expTab;
            this.g.setColor(b ? 2308170 : 1780536);
            this.g.fillRect(i9, i6, i7 - 2, this.FH0 + 5);
            if (b) {
                this.g.setColor(3793530);
                this.g.drawRect(i9, i6, i7 - 2, this.FH0 + 5);
            }
            this.g.setColor(b ? 3793530 : 8297134);
            this.g.drawText(new StringBuffer(String.valueOf(i8 + 1)).append(":").append(a[i8]).toString(), i9 + 2, i6 + 2, 0);
            i8 = i8 + 1;
        }
        int i10 = i6 + (this.FH0 + 6);
        if (this.expTab != 0) {
            if (this.expTab != 1) {
                if (this.expTab != 2) {
                    if (this.expTab == 3) {
                        int i11 = this.expStress.logSize();
                        int i12 = Math.min(3, i11);
                        int i13 = 0;
                        while(i13 < i12) {
                            String s = this.expStress.getLog(i11 - i12 + i13);
                            this.g.setColor((s.startsWith("!!")) ? 16733525 : (s.startsWith("\u2605\u2605")) ? 2865254 : 8297134);
                            this.g.drawText(this.fit(s, this.W - 16, 0), 8, i10 + i13 * (this.FH0 + 2), 0);
                            i13 = i13 + 1;
                        }
                    }
                } else {
                    this.g.setColor(16763196);
                    this.g.drawText("\u0422\u0432\u0438\u043a\u0438 (\u043d\u0430\u0436\u043c\u0438\u0442\u0435 1-8 \u0438\u043b\u0438 \u0442\u0430\u043f\u0430\u0439\u0442\u0435):", 8, i10, 0);
                    int i14 = 0;
                    while(i14 < 3) {
                        boolean b0 = (this.expStress.activeTweaks & 1 << i14) != 0;
                        this.g.setColor(b0 ? 3793530 : 8297134);
                        this.g.drawText(new StringBuffer(String.valueOf(i14 + 1)).append(":").append(b0 ? "[X] " : "[ ] ").append(this.fit(rpc.Exp.TWEAK_NAMES[i14], 120, 0)).toString(), 8, i10 + (i14 + 1) * (this.FH0 + 2), 0);
                        int i15 = i14 + 3;
                        boolean b1 = (this.expStress.activeTweaks & 1 << i15) != 0;
                        this.g.setColor(b1 ? 3793530 : 8297134);
                        this.g.drawText(new StringBuffer(String.valueOf(i15 + 1)).append(":").append(b1 ? "[X] " : "[ ] ").append(this.fit(rpc.Exp.TWEAK_NAMES[i15], 120, 0)).toString(), 140, i10 + (i14 + 1) * (this.FH0 + 2), 0);
                        i14 = i14 + 1;
                    }
                }
            } else {
                this.g.setColor(15003895);
                this.g.drawText(new StringBuffer("\u041a\u0443\u043b\u0435\u0440: ").append(rpc.ExpStress.COOLING_NAMES[this.expStress.curCooling]).toString(), 8, i10, 0);
                this.g.setColor(8297134);
                this.g.drawText("\u041d\u0430\u0436\u043c\u0438\u0442\u0435 [OK/\u0422\u0430\u043f] \u0434\u043b\u044f \u0441\u043c\u0435\u043d\u044b \u043a\u0443\u043b\u0435\u0440\u0430", 8, i10 + this.FH0 + 3, 0);
            }
        } else {
            this.g.setColor(15003895);
            this.g.drawText("\u0427\u0430\u0441\u0442\u043e\u0442\u0430 CPU: [ -100M ] [ -10M ] [ +10M ] [ +100M ]", 8, i10, 0);
            this.g.drawText("\u0412\u043e\u043b\u044c\u0442\u0430\u0436 VCore: [ -25mV ] [ +25mV ]", 8, i10 + this.FH0 + 3, 0);
        }
        int i16 = this.H - this.FH0 - 22;
        this.g.setColor((this.expStress.status != 1) ? 2308170 : 1980971);
        this.g.fillRoundRect(8, i16, this.W - 16, this.FH0 + 8, 4);
        this.g.setColor(3793530);
        this.g.drawRoundRect(8, i16, this.W - 16, this.FH0 + 8, 4);
        String s0 = (this.expStress.status != 1) ? "\u25ba [\u0417\u0410\u041f\u0423\u0421\u0422\u0418\u0422\u042c \u0422\u0415\u0421\u0422]" : "\u0418\u0414\u0415\u0422 \u0422\u0415\u0421\u0422...";
        this.g.drawText(s0, (this.W - this.g.textWidth(s0, 1)) / 2, i16 + 3, 1);
        this.footer("OK: \u0437\u0430\u043f\u0443\u0441\u043a \u2022 1-4: \u0432\u043a\u043b\u0430\u0434\u043a\u0438", "0: \u043d\u0430\u0437\u0430\u0434 \u043a \u0441\u043f\u0438\u0441\u043a\u0443");
    }
    
    private void inputExpRun(int i) {
        label0: if (i != 4) {
            if (i != 0) {
                if (i != 1) {
                    if (i != 2) {
                        if (i != 3) {
                            label1: {
                                if (i == 7) {
                                    break label1;
                                }
                                if (i == 5) {
                                    break label1;
                                }
                                if (i != 6) {
                                    break label0;
                                }
                            }
                            this.mode = 45;
                        } else if (this.expTab != 0) {
                            this.expTab = (this.expTab + 1) % 4;
                        } else {
                            rpc.ExpStress a = this.expStress;
                            a.curVoltage = a.curVoltage + 25;
                            this.expStress.recalcThermal();
                        }
                    } else if (this.expTab != 0) {
                        this.expTab = (this.expTab + 3) % 4;
                    } else {
                        this.expStress.curVoltage = Math.max(500, this.expStress.curVoltage - 25);
                        this.expStress.recalcThermal();
                    }
                } else if (this.expTab != 0) {
                    if (this.expTab == 1) {
                        this.expStress.curCooling = (this.expStress.curCooling + rpc.ExpStress.COOLING_NAMES.length - 1) % rpc.ExpStress.COOLING_NAMES.length;
                        this.expStress.recalcThermal();
                    }
                } else {
                    this.expStress.curFreq = Math.max(1, this.expStress.curFreq - 50);
                    this.expStress.recalcThermal();
                }
            } else if (this.expTab != 0) {
                if (this.expTab == 1) {
                    this.expStress.curCooling = (this.expStress.curCooling + 1) % rpc.ExpStress.COOLING_NAMES.length;
                    this.expStress.recalcThermal();
                }
            } else {
                rpc.ExpStress a0 = this.expStress;
                a0.curFreq = a0.curFreq + 50;
                this.expStress.recalcThermal();
            }
        } else if (this.expStress.status != 1) {
            this.expStress.start();
        }
    }
    
    private void pointerExpRun(int i, int i0) {
        if (i0 >= this.H - this.FH0 - 22) {
            this.expStress.start();
            return;
        }
        int i1 = Math.max(76, Math.min(130, this.H / 3));
        int i2 = this.FH1 + 8 + this.FH1 + 2 + this.FH0 + 3 + i1 + 4 + this.FH0 * 2 + 5 + 4;
        if (i0 >= i2 && i0 <= i2 + this.FH0 + 8) {
            int i3 = (i - 4) / ((this.W - 8) / 4);
            if (i3 >= 0 && i3 < 4) {
                this.expTab = i3;
            }
            return;
        }
        if (this.expTab != 1) {
            if (this.expTab == 2) {
                int i4 = ((i <= this.W / 2) ? 0 : 3) + (i0 - (i2 + this.FH0 + 8)) / (this.FH0 + 2);
                if (i4 >= 0 && i4 < 8) {
                    rpc.ExpStress a = this.expStress;
                    a.activeTweaks = a.activeTweaks ^ 1 << i4;
                    this.expStress.recalcThermal();
                }
            }
        } else {
            this.expStress.curCooling = (this.expStress.curCooling + 1) % rpc.ExpStress.COOLING_NAMES.length;
            this.expStress.recalcThermal();
        }
    }
    
    /**
     * The room screen: a drawn scene plus the cursor bar.
     *
     * Everything the player can touch is both drawn and listed, and the
     * hint line under the scene always names the object the cursor is on.
     */
    private void paintRoom() {
        this.life.tickTime(1);
        this.life.updateEnvironment();

        String head = "\u0423\u044e\u0442\u043d\u0430\u044f \u043a\u043e\u043c\u043d\u0430\u0442\u0430";
        String clock = "\u0434" + this.life.curDay + " "
                + this.getMonthName(this.life.curMonth) + " "
                + this.life.curYear + " \u2022 "
                + LifeSim.TIME_NAMES[this.life.timeOfDay];
        this.header(head, clock);

        int sceneTop = this.FH1 + 12;
        int sceneH = this.H - this.FH0 * 4 - 24 - sceneTop;
        if (sceneH < 120) {
            sceneH = this.H - sceneTop - 30;
        }
        this.g.setClip(0, sceneTop, this.W, sceneH);
        Room.draw(this.g, this.life, this.life.animationTick, this.pcPowered, sceneH);
        // number badges make the 1..8 shortcut discoverable: one per object,
        // at the top-left corner of the object's own rectangle
        for (int i = 0; i < Room.HOT_COUNT; i++) {
            int[] rr = Room.rect(i, this.W, sceneH);
            int bx = rr[0];
            // rects are in scene space, the badges are drawn in screen space
            int by = this.FH1 + 12 + rr[1];
            if (bx + this.FH0 + 4 > this.W) {
                bx = this.W - this.FH0 - 4;
            }
            if (by + this.FH0 + 4 > this.FH1 + 12 + sceneH) {
                by = this.FH1 + 12 + sceneH - this.FH0 - 4;
            }
            boolean sel = i == this.roomCursor;
            this.g.setColor(sel ? Theme.ACCENT : 0x121620);
            this.g.fillRect(bx, by, this.FH0 + 2, this.FH0 + 2);
            this.g.setColor(sel ? 0x3A2A00 : Theme.PANEL2);
            this.g.fillRect(bx + 1, by + 1, this.FH0, this.FH0);
            Text.draw(this.g, Integer.toString(i + 1), bx + 2, by + 2, 0,
                    sel ? Theme.ACCENT : Theme.MUTED);
        }
        // highlight the object under the cursor
        int[] r = Room.rect(this.roomCursor, this.W, sceneH);
        int oy = this.FH1 + 12;
        this.g.setColor(Theme.ACCENT);
        drawCornerBox(this.g, r[0], oy + r[1], r[2], r[3], 4);
        this.g.setClip(0, 0, this.W, this.H);

        // status strip: three short rows, above the footer
        int footerTop = this.H - this.FH0 - 11;
        int sy = footerTop - this.FH0 * 3 - 12;
        this.g.setColor(Theme.PANEL);
        this.g.fillRect(0, sy, this.W, footerTop - sy);
        this.g.setColor(Theme.LINE);
        this.g.drawLine(0, sy, this.W, sy);
        int row1 = sy + 2;
        int row2 = row1 + this.FH0;
        int row3 = row2 + this.FH0;
        // tea + energy
        Text.draw(this.g, "\u0447\u0430\u0439 " + this.life.teaLevel + "%", 5, row1, 0,
                this.life.teaHot ? Theme.ORANGE : Theme.MUTED);
        int tx = 5 + this.g.textWidth("\u0447\u0430\u0439 100%", 0) + 8;
        Text.draw(this.g, "\u044d\u043d\u0435\u0440\u0433\u0438\u044f "
                + this.life.energy + "%", tx, row1, 0, Theme.GREEN);
        tx += this.g.textWidth("\u044d\u043d\u0435\u0440\u0433\u0438\u044f 100%", 0) + 8;
        if (tx + 30 < this.W) {
            Text.draw(this.g, this.life.teaHot ? "\u0433\u043e\u0440\u044f\u0447\u0438\u0439"
                    : "\u043e\u0441\u0442\u044b\u043b", tx, row1, 0, Theme.ORANGE);
        }
        // mood + weather
        Text.draw(this.g, "\u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 "
                + this.life.mood + "%", 5, row2, 0, Theme.ACCENT2);
        String weather = LifeSim.WEATHER_NAMES[this.life.weather] + ", "
                + LifeSim.SEASON_NAMES[this.life.season];
        Text.draw(this.g, Text.fit(weather, 0, this.W / 2 - 10),
                this.W / 2 + 4, row2, 0, Theme.DIM);
        // radio
        String radio = this.life.radioOn
                ? "\u0440\u0430\u0434\u0438\u043e: " + this.life.radioTracks[this.life.radioStation]
                : "\u0440\u0430\u0434\u0438\u043e \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u043e";
        Text.draw(this.g, Text.fit(radio, 0, this.W - 10), 5, row3, 0,
                this.life.radioOn ? Theme.GREEN : Theme.MUTED);
        this.footer(this.roomHint(), "\u041b\u0421\u041a:\u041b\u0430\u0431");
    }

    /** "OK: <verb> <object>" hint for the current cursor position. */
    private String roomHint() {
        switch (this.roomCursor) {
            case Room.HOT_MONITOR: return "OK: \u0441\u0435\u0441\u0442\u044c \u0437\u0430 \u041f\u041a";
            case Room.HOT_MUG: return "OK: \u0447\u0430\u0439";
            case Room.HOT_CAT: return "OK: \u043f\u043e\u0433\u043b\u0430\u0434\u0438\u0442\u044c";
            case Room.HOT_LAMP: return "OK: \u043b\u0430\u043c\u043f\u0430";
            case Room.HOT_BED: return "OK: \u0441\u043f\u0430\u0442\u044c";
            case Room.HOT_DOOR: return "OK: \u0432 \u0433\u043e\u0440\u043e\u0434";
            case Room.HOT_TOOLS: return "OK: \u043c\u0430\u0441\u0442\u0435\u0440\u0441\u043a\u0430\u044f";
            default: return "OK: \u0440\u0430\u0434\u0438\u043e";
        }
    }

    /** Draws four corner ticks - a selection frame that does not hide art. */
    private void drawCornerBox(Gfx g, int x, int y, int w, int h, int len) {
        if (w < 2 * len + 2 || h < 2 * len + 2) {
            len = 2;
        }
        g.drawLine(x, y, x + len, y);
        g.drawLine(x, y, x, y + len);
        g.drawLine(x + w - len, y, x + w, y);
        g.drawLine(x + w, y, x + w, y + len);
        g.drawLine(x, y + h - len, x, y + h);
        g.drawLine(x, y + h, x + len, y + h);
        g.drawLine(x + w - len, y + h, x + w, y + h);
        g.drawLine(x + w, y + h - len, x + w, y + h);
    }

    /* ==================================================================
     *  tea screen
     * ================================================================== */

    private void paintTea() {
        this.teaTick++;
        int w = this.W;
        int h = this.H;
        this.header("\u0427\u0430\u0435\u043f\u0438\u0442\u0438\u0435",
                "\u0441\u0442\u0430\u043a\u0430\u043d \u2116" + (1 + this.teaBrewed));

        int top = this.FH1 + 12;
        int footerTop = h - this.FH0 - 12;
        int cardH = this.FH0 * 4 + 12;
        int sceneH = footerTop - top - cardH - 8;

        // warm table under a lamp
        this.g.setColor(0x2C1F14);
        this.g.fillRect(0, top, w, sceneH);
        this.g.setColor(0x3E2C1C);
        for (int y = top + 6; y < top + sceneH; y += 11) {
            this.g.drawLine(0, y, w, y);
        }
        // warm ambient light from the desk lamp, as soft bands (a hard
        // triangle read as a floating shape, not as light)
        for (int i = 0; i < 7; i++) {
            int bandW = w * (28 + i * 9) / 100;
            int bandY = top + sceneH * i / 22;
            this.g.setColor(0x38291A);
            this.g.fillRect(w - bandW, bandY, bandW, sceneH / 22 + 1);
        }

        int cupW = Math.min(w * 46 / 100, 96);
        int cupH = cupW * 85 / 100;
        int cx = (w - cupW) / 2 - 8;
        int cy = top + sceneH / 2 - cupH / 2;
        int tea = Math.max(0, Math.min(100, this.life.teaLevel));

        // steam: three short wavy wisps rising from the rim
        if (this.life.teaHot && tea > 0) {
            int steamH = Math.min(sceneH / 3, cy - top - 4);
            for (int wisp = 0; wisp < 3; wisp++) {
                int baseX = cx + cupW / 3 + wisp * (cupW / 4) + 2;
                for (int k = 0; k < steamH; k += 3) {
                    int t = (this.teaTick / 2 + k * 2 + wisp * 9) % 38;
                    int x = baseX + (int) (Math.sin((k + t) * 0.16) * (2 + k / 10));
                    int y = cy - 3 - k;
                    int shade = k * 3 / (steamH + 1);
                    this.g.setColor(shade < 1 ? 0x9A9082
                            : (shade < 2 ? 0x726A5C : 0x4E483C));
                    this.g.fillRect(x, y, 1, 2);
                }
            }
        }

        // cast shadow + saucer (an ellipse built from stacked rows)
        this.g.setColor(0x1E150D);
        this.g.fillRoundRect(cx - 18, cy + cupH + 2, cupW + 36, 9, 5);
        int sW = cupW + 36;
        int sH = 11;
        int sY = cy + cupH - 3;
        for (int row = 0; row < sH; row++) {
            int half = sW / 2 * (sH - Math.abs(row - sH / 2) * 2 + 2) / (sH + 2);
            int rgb = row < 2 ? 0xEDE6D4 : (row < 5 ? 0xCFC3A8 : 0x8E8068);
            this.g.setColor(rgb);
            this.g.fillRect(cx + cupW / 2 - half, sY + row, half * 2, 1);
        }
        this.g.setColor(0xB8AC90);
        this.g.fillRect(cx + 4, sY + 4, cupW - 8, 1);

        // handle: a closed ring.  Outer body, then the hole is punched out,
        // then the body of the cup is drawn over the root.
        int hx = cx + cupW - 8;
        int hy = cy + cupH / 4;
        int hh = cupH / 2;
        int hOuter = 26;
        int hWall = 6;
        this.g.setColor(0xC6BFB0);
        for (int row = 0; row < hh; row++) {
            // taper the ends so the grip looks round, not like a box
            int inset = 0;
            int edge = Math.min(row, hh - 1 - row);
            if (edge < 5) {
                inset = (5 - edge) * hOuter / 12;
            }
            this.g.fillRect(hx + inset, hy + row, hOuter - inset * 2, 1);
        }
        this.g.setColor(0x2C1F14);
        for (int row = hWall; row < hh - hWall; row++) {
            int edge = Math.min(row - hWall, hh - 1 - row - hWall);
            int inset = 0;
            if (edge < 4) {
                inset = (4 - edge) * (hOuter - hWall * 2) / 14;
            }
            this.g.fillRect(hx + hWall + inset, hy + row,
                    hOuter - hWall * 2 - inset * 2 + 2, 1);
        }

        // body: white ceramic, shaded on the right, bright on the left
        this.g.setColor(0xE4DFD4);
        this.g.fillRoundRect(cx, cy, cupW, cupH, 9);
        this.g.setColor(0xAFA898);
        this.g.fillRoundRect(cx + cupW - 10, cy + 5, 10, cupH - 10, 5);
        this.g.setColor(0xE4DFD4);
        this.g.fillRect(cx + cupW - 12, cy + 3, 9, cupH - 6);
        this.g.setColor(0xF6F2E8);
        this.g.fillRect(cx + 3, cy + 5, 3, cupH - 12);

        // tea inside, with a highlight ring
        int innerX = cx + 10;
        int innerY = cy + 9;
        int innerW = cupW - 20;
        int innerH = cupH - 18;
        this.g.setColor(0x2A1A0C);
        this.g.fillRoundRect(innerX, innerY, innerW, innerH, 5);
        int fillH = innerH * tea / 100;
        if (fillH > 0) {
            this.g.setColor(this.life.teaHot ? 0xB4651E : 0x8A4A16);
            this.g.fillRoundRect(innerX, innerY + innerH - fillH, innerW,
                    fillH, 4);
            this.g.setColor(this.life.teaHot ? 0xD8862E : 0xA45C1E);
            this.g.fillRect(innerX + 2, innerY + innerH - fillH, innerW - 4, 2);
            this.g.setColor(0xF0B060);
            this.g.fillRect(innerX + 5, innerY + innerH - fillH + 2, 10, 1);
        }
        // rim + inner shadow along the top edge of the tea hole
        this.g.setColor(0x4A3218);
        this.g.fillRect(innerX, innerY, innerW, 3);
        this.g.setColor(0xF4F0E6);
        this.g.fillRect(cx + 2, cy, cupW - 4, 3);

        // tea bag on its string, swinging
        int swing = (int) (Math.sin(this.teaTick * 0.11) * 5);
        this.g.setColor(0xE8D8A8);
        this.g.drawLine(cx + cupW - 8, cy + 8, cx + cupW + 14 + swing,
                cy + cupH + 6);
        this.g.setColor(0xFFD06A);
        this.g.fillRect(cx + cupW + 10 + swing, cy + cupH + 4, 9, 11);
        this.g.setColor(0xB08A30);
        this.g.fillRect(cx + cupW + 12 + swing, cy + cupH + 7, 5, 2);

        // spoon resting on the saucer
        this.g.setColor(0xB8BCC6);
        this.g.drawLine(cx + cupW + 6, cy + cupH + 5, cx + cupW + 28,
                cy + cupH + 2);
        this.g.setColor(0xD8DCE4);
        this.g.fillRoundRect(cx + cupW + 24, cy + cupH - 1, 10, 6, 3);

        // info card
        int iy = top + sceneH + 6;
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(5, iy, w - 10, cardH, 5);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(5, iy, w - 10, cardH, 5);
        int rowY = iy + 4;
        Text.draw(this.g, "\u0423\u0440\u043e\u0432\u0435\u043d\u044c " + tea + "%",
                10, rowY, 0, Theme.ACCENT);
        // gauge
        int gx = 10 + this.g.textWidth("\u0423\u0440\u043e\u0432\u0435\u043d\u044c 100%", 0) + 6;
        int gw = w - gx - 14;
        this.g.setColor(Theme.BG);
        this.g.fillRect(gx, rowY + 2, gw, this.FH0 - 3);
        this.g.setColor(this.life.teaHot ? 0xB4651E : 0x8A4A16);
        this.g.fillRect(gx, rowY + 2, gw * tea / 100, this.FH0 - 3);
        this.g.setColor(Theme.LINE);
        this.g.drawRect(gx, rowY + 2, gw, this.FH0 - 3);

        rowY += this.FH0 + 1;
        Text.draw(this.g, this.life.teaHot ? "\u0433\u043e\u0440\u044f\u0447\u0438\u0439"
                : "\u043e\u0441\u0442\u044b\u043b", 10, rowY, 0,
                this.life.teaHot ? Theme.ORANGE : Theme.MUTED);
        Text.draw(this.g, "\u044d\u043d\u0435\u0440\u0433\u0438\u044f " + this.life.energy
                + "%  \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u0435 "
                + this.life.mood + "%", 10 + this.g.textWidth(
                "\u0433\u043e\u0440\u044f\u0447\u0438\u0439", 0) + 10, rowY, 0,
                Theme.GREEN);
        rowY += this.FH0 + 1;
        Text.draw(this.g, Text.fit(this.life.lastEventMsg, 0, w - 20), 10,
                rowY, 0, Theme.DIM);
        this.footer("OK: \u043f\u0438\u0442\u044c   2: \u0437\u0430\u0432\u0430\u0440\u0438\u0442\u044c",
                "\u041d\u0430\u0437\u0430\u0434");
    }


    /* ==================================================================
     *  sleep screen
     * ================================================================== */

    private void paintSleep() {
        this.sleepTick++;
        int w = this.W;
        int h = this.H;
        this.g.setColor(0x080A12);
        this.g.fillRect(0, 0, w, h);
        // stars
        for (int i = 0; i < 40; i++) {
            int sx = (i * 53 + 7) % w;
            int sy = (i * 31 + 3) % (h / 2);
            this.g.setColor((this.sleepTick / 8 + i) % 3 == 0 ? 0x8A93A8 : 0xE8EEF8);
            this.g.fillRect(sx, sy, 1, 1);
        }
        // crescent moon: a disc with a darker disc bitten out of it
        int mx = w - 52;
        int my = 22;
        this.g.setColor(0xE8EEDA);
        this.g.fillRoundRect(mx, my, 18, 18, 9);
        this.g.setColor(0x080A12);
        this.g.fillRoundRect(mx + 6, my - 3, 18, 18, 9);
        this.g.setColor(0xDDE6F6);
        this.g.fillRect(mx + 3, my + 12, 3, 3);

        String title = "\u041e\u0442\u0434\u044b\u0445";
        Text.drawAnchored(this.g, title, w / 2, h * 26 / 100, 2, Gfx.HCENTER,
                Theme.ACCENT);
        Text.drawAnchored(this.g, "\u042d\u043d\u0435\u0440\u0433\u0438\u044f: "
                + this.life.energy + "%", w / 2, h * 26 / 100 + this.FH1 + 8, 0,
                Gfx.HCENTER, Theme.DIM);
        Text.drawAnchored(this.g, this.life.timeOfDay == LifeSim.TIME_NIGHT
                ? "\u041d\u043e\u0447\u044c. \u0412 \u043a\u043e\u043c\u043d\u0430\u0442\u0435 \u0442\u0438\u0445\u043e."
                : "\u0414\u0435\u043d\u044c \u043a\u043b\u043e\u043d\u0438\u0442\u0441\u044f \u043a \u0432\u0435\u0447\u0435\u0440\u0443.",
                w / 2, h * 26 / 100 + this.FH1 + 8 + this.FH0 + 4, 0,
                Gfx.HCENTER, Theme.MUTED);

        // bed seen from the side: frame, mattress, duvet, pillow
        int bx = w / 7;
        int bw = w * 5 / 7;
        int by = h * 54 / 100;
        int bh = h * 9 / 100;
        this.g.setColor(0x1E2436);
        this.g.fillRect(bx - 3, by + bh, bw + 6, 3);
        this.g.fillRect(bx - 3, by + bh, 4, h * 10 / 100);
        this.g.fillRect(bx + bw - 1, by + bh, 4, h * 10 / 100);
        this.g.setColor(0x39435C);
        this.g.fillRoundRect(bx, by, bw, bh, 3);
        this.g.setColor(0x4C8DFF);
        this.g.fillRoundRect(bx + bw / 4, by, bw * 3 / 4, bh, 3);
        this.g.setColor(0x6FA8FF);
        this.g.fillRect(bx + bw / 4 + 2, by + 1, bw * 3 / 4 - 4, 2);
        this.g.setColor(0xDDE6F6);
        this.g.fillRoundRect(bx + 3, by + 2, bw / 4 - 6, bh - 4, 2);

        // Zzz
        this.g.setColor(0x9FB0D0);
        for (int i = 0; i < 3; i++) {
            int phase = (this.sleepTick / 7 + i * 5) % 26;
            this.g.drawText("Z", w / 2 + 26 + i * 9, h * 44 / 100 - phase * 2,
                    (i == 1) ? 2 : 1);
        }
        this.footer("OK: \u0441\u043f\u0430\u0442\u044c", "\u041d\u0430\u0437\u0430\u0434");
    }

    
    
    /**
     * City screen: location tabs, an NPC portrait card and numbered replies.
     *
     * The 4.8 version drew two info boxes at the same y coordinate (so the
     * description was buried under the dialogue) and duplicated the option
     * numbers because the labels already contained them.
     */
    private void paintCity() {
        int w = this.W;
        int h = this.H;
        int loc = this.city.curLocation;
        this.header(rpc.City.LOC_NAMES[loc],
                this.life.curHour + ":00  " + this.life.curYear);

        // location tabs
        int top = this.FH1 + 12;
        int tabH = this.FH0 + 7;
        int n = rpc.City.LOC_NAMES.length;
        int tabW = (w - 10) / n;
        for (int i = 0; i < n; i++) {
            int tx = 5 + i * tabW;
            boolean sel = i == loc;
            this.g.setColor(sel ? Theme.ACCENT : Theme.PANEL);
            this.g.fillRoundRect(tx, top, tabW - 2, tabH, 2);
            Text.draw(this.g, Integer.toString(i + 1), tx + 2, top + 4, 0,
                    sel ? 0x2A1E00 : Theme.MUTED);
        }
        String locTag = (loc + 1) + "/" + n;
        Text.draw(this.g, locTag, w - this.g.textWidth(locTag, 0) - 6,
                top + tabH + 4, 0, Theme.MUTED);

        // description card
        int dy = top + tabH + this.FH0 + 9;
        String desc = rpc.City.LOC_DESCS[loc];
        String[] dlines = wrap(desc, w - 18, 0, 2);
        int descH = this.FH0 * dlines.length + 8;
        this.g.setColor(Theme.PANEL2);
        this.g.fillRoundRect(5, dy, w - 10, descH, 3);
        for (int i = 0; i < dlines.length; i++) {
            Text.draw(this.g, dlines[i], 9, dy + 4 + i * this.FH0, 0,
                    Theme.ACCENT2);
        }

        // NPC card with a portrait badge
        int ny = dy + descH + 4;
        int npcH = this.FH0 * 3 + 12;
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(5, ny, w - 10, npcH, 4);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(5, ny, w - 10, npcH, 4);
        int portrait = npcH - 10;
        Icons.drawSized(this.g, Icons.CART, 9, ny + 5, portrait,
                Theme.METAL, Theme.ACCENT, 0x39415A);
        int ptx = 9 + portrait + 6;
        Text.draw(this.g, Text.fit(this.city.dialogueNpc + ":", 0, w - ptx - 8),
                ptx, ny + 3, 0, Theme.ACCENT);
        String[] tlines = wrap(this.city.dialogueText, w - ptx - 10, 0, 2);
        for (int i = 0; i < tlines.length; i++) {
            Text.draw(this.g, tlines[i], ptx, ny + 4 + this.FH0 + i * this.FH0,
                    0, Theme.TEXT);
        }

        // numbered replies
        int oy = ny + npcH + 5;
        int optH = this.FH0 + 6;
        int footerTop = h - this.FH0 - 12;
        for (int i = 0; i < this.city.dialogueOptions.length; i++) {
            int ry = oy + i * (optH + 2);
            if (ry + optH > footerTop) {
                break;
            }
            boolean sel = i == this.city.selectedOption;
            this.g.setColor(sel ? Theme.PANEL2 : Theme.PANEL);
            this.g.fillRoundRect(6, ry, w - 12, optH, 3);
            this.g.setColor(sel ? Theme.ACCENT : Theme.LINE);
            this.g.drawRoundRect(6, ry, w - 12, optH, 3);
            this.g.setColor(sel ? Theme.ACCENT : Theme.MUTED);
            this.g.fillRect(6, ry + 2, 12, optH - 4);
            Text.draw(this.g, Integer.toString(i + 1), 9, ry + 4, 0,
                    sel ? 0x2A1E00 : Theme.DIM);
            Text.draw(this.g, Text.fit(stripNumber(this.city.dialogueOptions[i]),
                    0, w - 30), 22, ry + 4, 0,
                    sel ? Theme.TEXT : Theme.DIM);
        }
        this.footer("OK: \u0440\u0435\u043f\u043b\u0438\u043a\u0430",
                "\u2190\u2192 \u0440\u0430\u0439\u043e\u043d");
    }

    
    /** Removes a leading "3. " that the 4.8 strings carry. */
    private String stripNumber(String s) {
        int i = 0;
        while (i < s.length() && s.charAt(i) >= '0' && s.charAt(i) <= '9') {
            i++;
        }
        if (i > 0 && i + 1 < s.length() && s.charAt(i) == '.'
                && s.charAt(i + 1) == ' ') {
            return s.substring(i + 2);
        }
        return s;
    }
    
    private void inputCity(int action) {
        if (action >= ACT_NUM1 && action < ACT_NUM1 + this.city.dialogueOptions.length) {
            this.city.selectedOption = action - ACT_NUM1;
            this.toastMsg(this.city.executeOption(this.city.selectedOption, this.life, this));
            return;
        }
        int i = action;
        label0: if (i != 0) {
            if (i != 1) {
                if (i != 4) {
                    if (i != 2) {
                        if (i != 3) {
                            label1: {
                                if (i == 7) {
                                    break label1;
                                }
                                if (i == 5) {
                                    break label1;
                                }
                                if (i != 6) {
                                    break label0;
                                }
                            }
                            this.mode = 47;
                        } else {
                            this.city.curLocation = (this.city.curLocation + 1) % rpc.City.LOC_NAMES.length;
                            this.city.setupLocationEvent(this.city.curLocation, this.life.curYear);
                        }
                    } else {
                        this.city.curLocation = (this.city.curLocation + rpc.City.LOC_NAMES.length - 1) % rpc.City.LOC_NAMES.length;
                        this.city.setupLocationEvent(this.city.curLocation, this.life.curYear);
                    }
                } else {
                    this.toastMsg(this.city.executeOption(this.city.selectedOption, this.life, this));
                }
            } else {
                this.city.selectedOption = (this.city.selectedOption + 1) % this.city.dialogueOptions.length;
            }
        } else {
            this.city.selectedOption = (this.city.selectedOption + this.city.dialogueOptions.length - 1) % this.city.dialogueOptions.length;
        }
    }
    
    /**
     * Workshop: six tools on the left, the live "instrument" panel on the right.
     *
     * Each tool paints its own little instrument - a capacitor forest, an
     * oscilloscope trace, a paste thickness gauge, a dust bar - instead of the
     * 4.8 text dump.
     */
    private void paintWorkshopTools() {
        int w = this.W;
        int h = this.H;
        this.header(Workshop.shortName(this.workshop.activeTool),
                "\u0424\u0438\u0437\u0438\u043a\u0430 & \u0440\u0435\u043c\u043e\u043d\u0442");

        int top = this.FH1 + 12;
        int footerTop = h - this.FH0 - 12;
        int toolCol = 66;
        for (int i = 0; i < 6; i++) {
            int ty = top + i * (this.FH0 + 7);
            boolean sel = i == this.workshop.activeTool;
            this.g.setColor(sel ? Theme.PANEL2 : Theme.PANEL);
            this.g.fillRoundRect(4, ty, toolCol, this.FH0 + 5, 3);
            this.g.setColor(sel ? Theme.ACCENT : Theme.LINE);
            this.g.drawRoundRect(4, ty, toolCol, this.FH0 + 5, 3);
            Text.draw(this.g, Integer.toString(i + 1), 8, ty + 3, 0,
                    sel ? Theme.ACCENT : Theme.MUTED);
            Text.draw(this.g, Text.fit(Workshop.shortName(i), 0, toolCol - 20),
                    18, ty + 3, 0, sel ? Theme.TEXT : Theme.DIM);
        }

        int px = toolCol + 8;
        int pw = w - px - 4;
        int cardH = this.FH0 * 4 + 10;
        int ph = footerTop - top - cardH - 8;
        if (ph < 90) {
            ph = 90;
        }
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(px, top, pw, ph, 4);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(px, top, pw, ph, 4);
        Workshop.draw(this.g, this.workshop, px, top, pw, ph, this.frame);

        int cy = top + ph + 4;
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(4, cy, w - 8, cardH, 4);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(4, cy, w - 8, cardH, 4);
        Text.draw(this.g, Text.fit(this.workshop.toolFeedback, 0, w - 18), 9,
                cy + 3, 0, Theme.GREEN);
        int done = Workshop.progress(this.workshop);
        Text.draw(this.g, "\u0413\u043e\u0442\u043e\u0432\u043d\u043e\u0441\u0442\u044c: " + done + "%", 9,
                cy + 3 + this.FH0, 0, done >= 100 ? Theme.GREEN : Theme.ACCENT);
        int bx = 9 + this.g.textWidth("\u0413\u043e\u0442\u043e\u0432\u043d\u043e\u0441\u0442\u044c: 100%", 0) + 6;
        int bw = w - bx - 12;
        this.g.setColor(Theme.BG);
        this.g.fillRect(bx, cy + 5 + this.FH0, bw, this.FH0 - 2);
        this.g.setColor(done >= 100 ? Theme.GREEN : Theme.ACCENT2);
        this.g.fillRect(bx, cy + 5 + this.FH0, bw * done / 100, this.FH0 - 2);
        this.g.setColor(Theme.LINE);
        this.g.drawRect(bx, cy + 5 + this.FH0, bw, this.FH0 - 2);
        this.footer("OK: \u043f\u0440\u0438\u043c\u0435\u043d\u0438\u0442\u044c  1-6: \u0438\u043d\u0441\u0442\u0440\u0443\u043c\u0435\u043d\u0442",
                "BACK");
    }

    private void inputWorkshopTools(int action) {
        if (action >= ACT_NUM1 && action < ACT_NUM1 + 6) {
            this.workshop.activeTool = action - ACT_NUM1;
        }
        int i = action;
        label0: if (i != 2) {
            if (i != 3) {
                if (i != 4) {
                    label2: {
                        label3: {
                            if (i == 0) {
                                break label3;
                            }
                            if (i != 1) {
                                break label2;
                            }
                        }
                        this.workshop.useToolAction(i);
                        break label0;
                    }
                    label1: {
                        if (i == 7) {
                            break label1;
                        }
                        if (i == 5) {
                            break label1;
                        }
                        if (i != 6) {
                            break label0;
                        }
                    }
                    this.mode = 47;
                } else {
                    this.toastMsg(this.workshop.useToolAction(i));
                }
            } else {
                this.workshop.activeTool = (this.workshop.activeTool + 1) % rpc.WorkshopTools.TOOL_NAMES.length;
                rpc.Sound.click();
            }
        } else {
            this.workshop.activeTool = (this.workshop.activeTool + rpc.WorkshopTools.TOOL_NAMES.length - 1) % rpc.WorkshopTools.TOOL_NAMES.length;
            rpc.Sound.click();
        }
    }
    
    /**
     * Calendar / history screen.
     *
     * Was: a header that overflowed its own plate ("ХРОНИКА IT 19802027"),
     * an event box clipped to one line and a text list.  Now a year strip with
     * arrows, the event card with the year rendered big, and achievement
     * cards with a progress bar.
     */
    private void paintEventNews() {
        int w = this.W;
        int h = this.H;
        int year = this.life.curYear;
        this.header("\u0425\u0440\u043e\u043d\u0438\u043a\u0430 IT", year + "");

        // year strip
        int top = this.FH1 + 12;
        int stripH = this.FH2 + 12;
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(5, top, w - 10, stripH, 4);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(5, top, w - 10, stripH, 4);
        this.g.setColor(Theme.ACCENT);
        this.g.fillTriangle(14, top + stripH / 2, 22, top + stripH / 2 - 5,
                22, top + stripH / 2 + 5);
        this.g.fillTriangle(w - 14, top + stripH / 2, w - 22,
                top + stripH / 2 - 5, w - 22, top + stripH / 2 + 5);
        Text.drawAnchored(this.g, Integer.toString(year), w / 2,
                top + (stripH - this.FH2) / 2, 2, Gfx.HCENTER, Theme.ACCENT);
        Text.drawAnchored(this.g, "\u2190 \u0433\u043e\u0434 \u2192", w / 2,
                top + stripH + 2, 0, Gfx.HCENTER, Theme.MUTED);

        // event card
        int ey = top + stripH + this.FH0 + 8;
        int eh = this.FH0 * 4 + 10;
        this.g.setColor(Theme.PANEL2);
        this.g.fillRoundRect(5, ey, w - 10, eh, 4);
        this.g.setColor(Theme.ACCENT2);
        this.g.drawRoundRect(5, ey, w - 10, eh, 4);
        Text.draw(this.g, "\u0421\u043e\u0431\u044b\u0442\u0438\u0435 \u0433\u043e\u0434\u0430", 9, ey + 3, 0,
                Theme.ACCENT2);
        String[] lines = wrap(rpc.CalendarHistory.getEventForYear(year),
                w - 20, 0, 3);
        for (int i = 0; i < lines.length; i++) {
            Text.draw(this.g, lines[i], 9, ey + 4 + this.FH0 + i * this.FH0,
                    0, Theme.TEXT);
        }

        // achievements
        int ay = ey + eh + 6;
        int done = 0;
        for (int i = 0; i < rpc.CalendarHistory.ACHIEVEMENTS.length; i++) {
            if (rpc.CalendarHistory.achDone[i]) {
                done++;
            }
        }
        String head = "\u0414\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u044f: " + done + "/"
                + rpc.CalendarHistory.ACHIEVEMENTS.length;
        Text.draw(this.g, head, 7, ay, 0, Theme.ACCENT);
        // progress bar
        int px = 7 + this.g.textWidth(head, 0) + 6;
        int pw = w - px - 8;
        this.g.setColor(Theme.BG);
        this.g.fillRect(px, ay + 2, pw, this.FH0 - 3);
        this.g.setColor(Theme.ACCENT);
        int fill = pw * done / rpc.CalendarHistory.ACHIEVEMENTS.length;
        if (fill > 0) {
            this.g.fillRect(px, ay + 2, fill, this.FH0 - 3);
        }
        this.g.setColor(Theme.LINE);
        this.g.drawRect(px, ay + 2, pw, this.FH0 - 3);

        int cy = ay + this.FH0 + 5;
        int cardH = this.FH0 * 2 + 6;
        int footerTop = h - this.FH0 - 12;
        int shown = 0;
        for (int i = 0; i < rpc.CalendarHistory.ACHIEVEMENTS.length; i++) {
            if (cy + cardH > footerTop) {
                break;
            }
            boolean ok = rpc.CalendarHistory.achDone[i];
            shown++;
            this.g.setColor(ok ? 0x1E3A2A : Theme.PANEL);
            this.g.fillRoundRect(5, cy, w - 10, cardH, 3);
            this.g.setColor(ok ? Theme.GREEN : Theme.LINE);
            this.g.drawRoundRect(5, cy, w - 10, cardH, 3);
            // check box
            this.g.setColor(ok ? Theme.GREEN : Theme.BG);
            this.g.fillRect(9, cy + cardH / 2 - 4, 8, 8);
            this.g.setColor(ok ? 0x0E1A12 : Theme.LINE);
            this.g.drawRect(9, cy + cardH / 2 - 4, 8, 8);
            if (ok) {
                int bx0 = 9;
                int by0 = cy + cardH / 2 - 4;
                this.g.setColor(0x0E1A12);
                this.g.drawLine(bx0 + 1, by0 + 3, bx0 + 3, by0 + 6);
                this.g.drawLine(bx0 + 3, by0 + 6, bx0 + 7, by0 + 1);
            }
            String[] ach = wrap(rpc.CalendarHistory.ACHIEVEMENTS[i], w - 32, 0, 2);
            for (int k = 0; k < ach.length; k++) {
                Text.draw(this.g, ach[k], 21, cy + 3 + k * this.FH0, 0,
                        ok ? Theme.TEXT : Theme.DIM);
            }
            cy += cardH + 3;
        }
        this.footer("\u2190\u2192 \u0433\u043e\u0434", "OK: \u0432 \u043a\u043e\u043c\u043d\u0430\u0442\u0443");
    }

    /** Greedy word wrap; returns at most {@code maxLines} lines. */
    private String[] wrap(String text, int maxPx, int style, int maxLines) {
        String[] out = new String[maxLines];
        int count = 0;
        if (text == null) {
            return new String[0];
        }
        int start = 0;
        while (start < text.length() && count < maxLines) {
            int end = text.length();
            while (end > start && this.g.textWidth(text.substring(start, end), style) > maxPx) {
                int space = text.lastIndexOf(' ', end - 1);
                if (space <= start) {
                    // a single very long word: hard cut
                    end = start + Math.max(1, maxPx / (6 * Text.scale(style)) - 1);
                    break;
                }
                end = space;
            }
            String piece = text.substring(start, Math.min(end, text.length())).trim();
            if (count == maxLines - 1 && end < text.length()) {
                piece = Text.fit(piece + " " + text.substring(Math.min(end, text.length())).trim(),
                        style, maxPx);
            }
            out[count++] = piece;
            start = end + 1;
        }
        String[] trimmed = new String[count];
        for (int i = 0; i < count; i++) {
            trimmed[i] = out[i];
        }
        return trimmed;
    }

    
    private void inputEventNews(int i) {
        label0: if (i != 2) {
            if (i != 3) {
                label1: {
                    if (i == 7) {
                        break label1;
                    }
                    if (i == 4) {
                        break label1;
                    }
                    if (i != 0) {
                        break label0;
                    }
                }
                this.mode = 47;
            } else {
                this.life.curYear = Math.min(2027, this.life.curYear + 1);
                rpc.Sound.click();
            }
        } else {
            this.life.curYear = Math.max(1980, this.life.curYear - 1);
            rpc.Sound.click();
        }
    }
    
    private String getMonthName(int i) {
        String s = null;
        String[] a = new String[12];
        a[0] = "\u042f\u043d\u0432";
        a[1] = "\u0424\u0435\u0432";
        a[2] = "\u041c\u0430\u0440";
        a[3] = "\u0410\u043f\u0440";
        a[4] = "\u041c\u0430\u0439";
        a[5] = "\u0418\u044e\u043d";
        a[6] = "\u0418\u044e\u043b";
        a[7] = "\u0410\u0432\u0433";
        a[8] = "\u0421\u0435\u043d";
        a[9] = "\u041e\u043a\u0442";
        a[10] = "\u041d\u043e\u044f";
        a[11] = "\u0414\u0435\u043a";
        label2: {
            label0: {
                label1: {
                    if (i < 1) {
                        break label1;
                    }
                    if (i <= 12) {
                        break label0;
                    }
                }
                s = "\u042f\u043d\u0432";
                break label2;
            }
            s = a[i - 1];
        }
        return s;
    }
    
    public void paint() {
        this.W = this.g.width();
        this.H = this.g.height();
        this.FH0 = this.g.textHeight(0);
        this.FH1 = this.g.textHeight(1);
        this.FH2 = this.g.textHeight(2);
        this.frame++;
        this.g.setColor(Theme.BG);
        this.g.fillRect(0, 0, this.W, this.H);
        this.g.setColor(Theme.BG2);
        int i = 0;
        while(i < this.H) {
            this.g.drawLine(0, i, this.W, i);
            i = i + 12;
        }
        switch (this.mode) {
            case M_MENU:
                this.paintMenu();
                break;
            case M_ROOM:
                this.paintRoom();
                break;
            case M_TEA:
                this.paintTea();
                break;
            case M_SLEEP:
                this.paintSleep();
                break;
            default:
                this.paintLegacy();
                break;
        }
        if (this.toastActive()) {
            this.paintToast();
        }
    }
    
    private void paintLegacy() {
        if (this.mode != 0) {
            if (this.mode != 1) {
                if (this.mode != 2) {
                    if (this.mode != 3) {
                        if (this.mode != 4) {
                            if (this.mode != 5) {
                                if (this.mode != 7) {
                                    if (this.mode != 8) {
                                        if (this.mode != 9) {
                                            if (this.mode != 10) {
                                                if (this.mode != 11) {
                                                    if (this.mode != 12) {
                                                        if (this.mode != 13) {
                                                            if (this.mode != 14) {
                                                                if (this.mode != 15) {
                                                                    if (this.mode != 16) {
                                                                        if (this.mode != 17) {
                                                                            if (this.mode != 18) {
                                                                                if (this.mode != 19) {
                                                                                    if (this.mode != 20) {
                                                                                        if (this.mode != 27) {
                                                                                            if (this.mode != 21) {
                                                                                                if (this.mode != 28) {
                                                                                                    if (this.mode != 22) {
                                                                                                        if (this.mode != 23) {
                                                                                                            if (this.mode != 24) {
                                                                                                                if (this.mode != 25) {
                                                                                                                    if (this.mode != 26) {
                                                                                                                        if (this.mode != 47) {
                                                                                                                            if (this.mode != 48) {
                                                                                                                                if (this.mode != 49) {
                                                                                                                                    if (this.mode != 52) {
                                                                                                                                        if (this.mode != 45) {
                                                                                                                                            if (this.mode != 46) {
                                                                                                                                                if (this.mode < 29) {
                                                                                                                                                    this.paintAbout();
                                                                                                                                                } else {
                                                                                                                                                    this.paintExtra();
                                                                                                                                                }
                                                                                                                                            } else {
                                                                                                                                                this.paintExpRun();
                                                                                                                                            }
                                                                                                                                        } else {
                                                                                                                                            this.paintExpList();
                                                                                                                                        }
                                                                                                                                    } else {
                                                                                                                                        this.paintEventNews();
                                                                                                                                    }
                                                                                                                                } else {
                                                                                                                                    this.paintWorkshopTools();
                                                                                                                                }
                                                                                                                            } else {
                                                                                                                                this.paintCity();
                                                                                                                            }
                                                                                                                        } else {
                                                                                                                            this.paintRoom();
                                                                                                                        }
                                                                                                                    } else {
                                                                                                                        this.paintGame();
                                                                                                                    }
                                                                                                                } else {
                                                                                                                    this.paintWeb();
                                                                                                                }
                                                                                                            } else {
                                                                                                                this.paintSpec();
                                                                                                            }
                                                                                                        } else {
                                                                                                            this.paintBsod();
                                                                                                        }
                                                                                                    } else {
                                                                                                        this.paintInstall();
                                                                                                    }
                                                                                                } else {
                                                                                                    this.paintLang();
                                                                                                }
                                                                                            } else {
                                                                                                this.paintOsList();
                                                                                            }
                                                                                        } else {
                                                                                            this.paintPost();
                                                                                        }
                                                                                    } else {
                                                                                        this.paintSetup();
                                                                                    }
                                                                                } else {
                                                                                    this.paintMine();
                                                                                }
                                                                            } else {
                                                                                this.paintNet();
                                                                            }
                                                                        } else {
                                                                            this.paintTasks();
                                                                        }
                                                                    } else {
                                                                        this.paintRun();
                                                                    }
                                                                } else {
                                                                    this.paintEditor();
                                                                }
                                                            } else {
                                                                this.paintOsProgs();
                                                            }
                                                        } else {
                                                            this.paintOs();
                                                        }
                                                    } else {
                                                        this.paintBench();
                                                    }
                                                } else {
                                                    this.paintOrders();
                                                }
                                            } else {
                                                this.paintOffers();
                                            }
                                        } else {
                                            this.paintMarkets();
                                        }
                                    } else {
                                        this.paintCatParts();
                                    }
                                } else {
                                    this.paintCatalog();
                                }
                            } else {
                                this.paintMuseum();
                            }
                        } else {
                            this.paintResult();
                        }
                    } else {
                        this.paintShop();
                    }
                } else {
                    this.paintBuild();
                }
            } else {
                this.paintMap();
            }
        } else {
            this.paintMenu();
        }
    }
    
    private void paintToast() {
        int i = Math.min(this.W - 12, this.g.textWidth(this.toast, 0) + 16);
        int i0 = this.FH0 + 12;
        int i1 = (this.W - i) / 2;
        int i2 = this.H - i0 - this.FH0 - 18;
        this.g.setColor(16763196);
        this.g.fillRoundRect(i1, i2, i, i0, 6);
        this.g.setColor(1056800);
        int i3 = i1 + 8;
        if (this.g.textWidth(this.toast, 0) > i - 16) {
            i3 = i1 + 6;
        }
        this.g.drawText(this.toast, i3, i2 + 6, 0);
    }
    
    private String fit(String s, int i, int i0) {
        if (s == null) {
            return "";
        }
        if (this.g.textWidth(s, i0) <= i) {
            return s;
        }
        int i1 = s.length();
        int i2 = 0;
        while(true) {
            int i3 = i2;
            if (i2 < i1) {
                i2 = (i3 + i1 + 1) / 2;
                if (this.g.textWidth(new StringBuffer(String.valueOf((Object)String.valueOf((Object)s.substring(0, i2)))).append("~").toString(), i0) > i) {
                    i1 = i2 - 1;
                    i2 = i3;
                }
            } else {
                if (i2 < 1) {
                    i2 = 1;
                }
                return new StringBuffer(String.valueOf((Object)String.valueOf((Object)s.substring(0, i2)))).append("~").toString();
            }
        }
    }
    
    private void header(String s, String s0) {
        this.g.setColor(1780536);
        this.g.fillRect(0, 0, this.W, this.FH1 + 10);
        this.g.setColor(3793530);
        this.g.fillRect(0, 0, 3, this.FH1 + 10);
        int right = s0 == null ? 0 : this.g.textWidth(s0, 0) + 14;
        this.g.setColor(Theme.TEXT);
        this.g.drawText(Text.fit(s, 1, this.W - 14 - right), 8, 5, 1);
        if (s0 != null) {
            int i = this.g.textWidth(s0, 0);
            this.g.setColor(Theme.ACCENT);
            this.g.drawText(s0, this.W - i - 10, 7, 0);
        }
        this.g.setColor(3033694);
        this.g.drawLine(0, this.FH1 + 10, this.W, this.FH1 + 10);
    }
    
    private void footer(String s, String s0) {
        int i = this.H - this.FH0 - 8;
        this.g.setColor(1780536);
        this.g.fillRect(0, i - 3, this.W, this.FH0 + 11);
        this.g.setColor(3033694);
        this.g.drawLine(0, i - 3, this.W, i - 3);
        this.g.setColor(8297134);
        this.g.drawText(s, 6, i, 0);
        if (s0 != null) {
            int i0 = this.g.textWidth(s0, 0);
            this.g.setColor(16763196);
            this.g.drawText(s0, this.W - i0 - 10, i, 0);
        }
    }
    
    /**
     * Main menu: one icon + one short line per entry.
     *
     * 4.8 printed eight full sentences (they were clipped mid-word on a
     * 240 px screen) and had no icons at all.  Now every row is an icon, a
     * one-word title and a one-word hint, and the row count is derived from
     * the screen height so the list always fits.
     */
    private void paintMenu() {
        int w = this.W;
        int h = this.H;
        int top = this.FH2 + this.FH0 + 18;

        // title plate
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(6, 3, w - 12, this.FH2 + this.FH0 + 14, 6);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(6, 3, w - 12, this.FH2 + this.FH0 + 14, 6);
        Text.drawAnchored(this.g, "RETRO PC BUILDER", w / 2, 6, 2,
                Gfx.HCENTER, Theme.ACCENT);
        String sub = "1979-2027   " + this.db.count() + " \u0434\u0435\u0442\u0430\u043b\u0435\u0439";
        Text.drawAnchored(this.g, Text.fit(sub, 0, w - 16), w / 2,
                this.FH2 + 10, 0, Gfx.HCENTER, Theme.MUTED);

        int n = this.MENU_ITEMS.length;
        int footer = this.FH0 + 10;
        int avail = h - top - footer - 4;
        int rh = avail / n;
        if (rh > 30) {
            rh = 30;
        }
        if (rh < 16) {
            rh = 16;
        }
        int iconSize = rh - 9;
        if (iconSize > 26) {
            iconSize = 26;
        }
        if (iconSize < 10) {
            iconSize = 10;
        }

        for (int i = 0; i < n; i++) {
            int y = top + i * rh;
            boolean sel = i == this.menuSel;
            int bg = sel ? Theme.PANEL2 : Theme.PANEL;
            this.g.setColor(bg);
            this.g.fillRoundRect(6, y, w - 12, rh - 3, 4);
            if (sel) {
                this.g.setColor(Theme.ACCENT);
                this.g.drawRoundRect(6, y, w - 12, rh - 3, 4);
                this.g.fillRect(6, y + 3, 3, rh - 9);
            }
            int ix = 11;
            int iy = y + (rh - 3 - iconSize) / 2;
            Icons.drawSized(this.g, Icons.forMenu(i), ix, iy, iconSize,
                    sel ? Theme.ACCENT : Theme.METAL,
                    sel ? Theme.ACCENT2 : Theme.DIM,
                    sel ? 0x8A6A20 : 0x39415A);
            int tx = ix + iconSize + 7;
            String title = this.MENU_TITLES[i];
            Text.draw(this.g, Text.fit(title, 0, w - tx - 26), tx,
                    y + (rh - 3 - this.FH0) / 2, 0,
                    sel ? Theme.TEXT : Theme.DIM);
            this.g.setColor(sel ? Theme.ACCENT2 : Theme.LINE);
            String num = Integer.toString(i + 1);
            this.g.drawText(num, w - 14, y + (rh - 3 - this.FH0) / 2, 0);
            this.g.setColor(Theme.LINE);
            if (i < n - 1) {
                this.g.drawLine(10, y + rh - 2, w - 10, y + rh - 2);
            }
        }

        // progress strip
        String prog = "\u041f\u0440\u043e\u0439\u0434\u0435\u043d\u043e: "
                + this.unlocked + "/" + this.db.levels.length
                + "   XP " + this.totalXp;
        int barY = h - footer - this.FH0 - 6;
        this.g.setColor(Theme.PANEL);
        this.g.fillRect(6, barY, w - 12, this.FH0 + 3);
        Text.draw(this.g, Text.fit(prog, 0, w - 16), 10, barY + 2, 0, Theme.MUTED);
        this.footer("\u2191\u2193 \u0432\u044b\u0431\u043e\u0440", "OK");
    }

    
    private void paintMap() {
        this.header("\u041a\u0430\u043c\u043f\u0430\u043d\u0438\u044f", "\u0431\u044e\u0434\u0436\u0435\u0442 \u043f\u043e \u0433\u043e\u0434\u0430\u043c");
        int i = this.FH1 + 8;
        int i0 = this.FH1 + 16;
        int i1 = (this.H - i0 - this.FH0 - 16) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.mapSel < this.mapTop) {
            this.mapTop = this.mapSel;
        }
        if (this.mapSel > this.mapTop + i1 - 1) {
            this.mapTop = this.mapSel - i1 + 1;
        }
        int i2 = 0;
        while(true) {
            if (i2 < i1) {
                int i3 = this.mapTop + i2;
                if (i3 < this.db.levels.length) {
                    rpc.Db$Level a = this.db.levels[i3];
                    int i4 = i0 + i2 * i;
                    boolean b = i3 == this.mapSel;
                    boolean b0 = i3 < this.unlocked;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i4, this.W - 8, i - 2);
                    this.g.setColor(b ? 3793530 : b0 ? 15003895 : 8297134);
                    this.g.drawText(new StringBuffer().append(a.year).toString(), 8, i4 + 4, 1);
                    if (!b0) {
                        this.g.setColor(9067098);
                        int i5 = this.W - 20;
                        int i6 = i4 + 5;
                        this.g.drawRect(i5 + 1, i6 + 4, 6, 5);
                        this.g.drawLine(i5 + 2, i6 + 4, i5 + 2, i6 + 2);
                        this.g.drawLine(i5 + 6, i6 + 4, i5 + 6, i6 + 2);
                        this.g.drawLine(i5 + 2, i6 + 2, i5 + 6, i6 + 2);
                    }
                    this.g.setColor(b0 ? b ? 15003895 : 8297134 : 8297134);
                    this.g.drawText(this.fit(a.title, this.W - 58 - 8, 0), 58, i4 + 4, 0);
                    if ((this.best[i3]) > 0) {
                        String s = new StringBuffer().append(this.best[i3]).toString();
                        this.g.setColor(16763196);
                        this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i4 + 4, 0);
                    }
                    i2 = i2 + 1;
                    continue;
                }
            }
            this.footer((this.sandbox) ? "\u043f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430" : "OK \u2014 \u0441\u0431\u043e\u0440\u043a\u0430", "1: \u043c\u0435\u043d\u044e");
            return;
        }
    }
    
    private int paintScene(int i, int i0) {
        int i1 = rpc.Scene.deskY(i, i0);
        rpc.Scene.room(this.g, 0, i, this.W, i0);
        rpc.Part a = this.slots[1];
        rpc.Part a0 = this.slots[0];
        rpc.Part a1 = this.slots[6];
        rpc.Part a2 = this.slots[13];
        rpc.Part a3 = this.slots[15];
        rpc.Part a4 = this.slots[16];
        rpc.Part a5 = this.slots[14];
        rpc.Part a6 = this.slots[19];
        boolean b = (a2 == null) ? this.levelYear() >= 2005 : a2.f[1] != 0;
        int i2 = i1 - i;
        if (a2 == null) {
            int i3 = Math.min(i2 - 14, this.W / 2 * 3 / 4);
            if (i3 < 20) {
                i3 = 20;
            }
            int i4 = i3 * 4 / 3;
            if (i4 > this.W / 2 - 4) {
                i4 = this.W / 2 - 4;
                i3 = i4 * 3 / 4;
            }
            this.g.setColor(3031638);
            this.g.drawRect(6, i1 - i3 - 9, i4, i3);
            this.g.drawText("\u043c\u043e\u043d\u0438\u0442\u043e\u0440", 10, i1 - i3 / 2 - 9, 0);
        } else {
            int i5 = Math.min(i2 - 14, this.W / 2 * 3 / 4);
            if (i5 < 20) {
                i5 = 20;
            }
            int i6 = i5 * 4 / 3;
            if (i6 > this.W / 2 - 4) {
                i6 = this.W / 2 - 4;
                i5 = i6 * 3 / 4;
            }
            int i7 = i1 - i5 - (b ? 14 : 7);
            int[] a7 = rpc.Scene.monitor(this.g, 6, i7, i6, i5, b);
            rpc.Scene.screenGlow(this.g, a7[0], a7[1], a7[2], a7[3], !b);
            String s = (this.osIndex < 0) ? "\u043d\u0435\u0442 \u041e\u0421" : rpc.Inst.all()[this.osIndex].name;
            this.g.setColor(5953658);
            this.g.drawText(this.fit(s, a7[2] - 4, 0), a7[0] + 2, a7[1] + 2, 0);
            if (this.osIndex >= 0) {
                this.g.setColor(3050058);
                this.g.drawText("C:\\>_", a7[0] + 2, a7[1] + 2 + this.FH0 + 1, 0);
            }
            rpc.Scene.shadow(this.g, 10, i1, i6 - 8);
        }
        int i8 = Math.min(40, this.W / 4);
        int i9 = Math.min(i2 - 10, 58);
        if (i9 < 24) {
            i9 = 24;
        }
        int i10 = this.W - i8 - 6;
        int i11 = i1 - i9;
        label1: {
            label0: {
                if (a != null) {
                    break label0;
                }
                if (a0 != null) {
                    break label0;
                }
                this.g.setColor(3031638);
                this.g.drawRect(i10, i11, i8, i9);
                this.g.drawText("\u043a\u043e\u0440\u043f\u0443\u0441", i10 + 2, i11 + i9 / 2 - this.FH0 / 2, 0);
                break label1;
            }
            rpc.Scene.shadow(this.g, i10 + 2, i1, i8 - 4);
            rpc.Scene.caseUnit(this.g, i10, i11, i8, i9, (a == null) ? 2000 : a.year, this.buildSel == 1);
            if (a0 != null) {
                this.g.setColor(10150048);
                this.g.fillRect(i10 + 3, i11 + 3, 3, 3);
            }
            if (a1 != null) {
                this.g.setColor(10141920);
                this.g.fillRect(i10 + 8, i11 + 3, 3, 3);
            }
        }
        int i12 = Math.min(80, this.W - 44);
        int i13 = i1 + 15;
        if (a3 == null) {
            this.g.setColor(3031638);
            this.g.drawRect(6, i13, i12, 20);
            this.g.drawText("\u043a\u043b\u0430\u0432\u0438\u0430\u0442\u0443\u0440\u0430", 10, i13 + 6, 0);
        } else {
            rpc.Scene.keyboard(this.g, 6, i13, i12, 20, a3.year);
            if (a3.f[1] != 0) {
                this.g.setColor(3854528);
                this.g.drawRect(6, i13, i12, 20);
            }
        }
        if (a4 == null) {
            this.g.setColor(3031638);
            this.g.drawRect(6 + i12 + 6, i13 + 2, 12, 15);
        } else {
            rpc.Scene.mouse(this.g, 6 + i12 + 6, i13 + 2, 12, 15, a4.year);
        }
        if (a5 != null) {
            rpc.Scene.speaker(this.g, this.W - 22, i13 - 6, 14, 22, a5.year < 1996);
            rpc.Scene.speaker(this.g, this.W - 22, i13 + 18, 14, 22, a5.year < 1996);
        }
        if (a6 != null) {
            rpc.Scene.printer(this.g, this.W - 56, i + 30, 44, 28, a6.year);
        }
        return i1 + 38;
    }
    
    /**
     * Assembly screen.
     *
     * top:    the workbench drawing (see {@link BuildView})
     * middle: the selected slot's part with price / condition / sockets
     * bottom: the six action rows as buttons
     */
    private void paintBuild() {
        String title = this.sandbox ? "\u041f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430"
                : "\u0421\u0431\u043e\u0440\u043a\u0430 \u2022 " + this.levelYear();
        this.header(title + " \u2022 \u0434" + this.day, Util.money(this.cash));

        // --- workbench -------------------------------------------------
        int top = this.FH1 + 14;
        int btnH = this.FH0 + 4;
        int btnBlock = 4 * (btnH + 2) + 4;
        int detailH = this.FH0 * 3 + 10;
        int footerTop = this.H - this.FH0 - 11;
        int benchH = footerTop - top - btnBlock - detailH - 10;
        if (benchH < 100) {
            benchH = 100;
        }
        if (benchH > 190) {
            benchH = 190;
        }
        this.g.setClip(4, top, this.W - 8, benchH);
        boolean pcOn = this.pcPowered;
        BuildView.draw(this.g, this.slots, this.buildSel, pcOn, 4, top,
                this.W - 8, benchH, this.frame);
        this.g.setClip(0, 0, this.W, this.H);
        this.g.setColor(Theme.LINE);
        this.g.drawRect(4, top, this.W - 8, benchH);

        // --- detail of the selected slot ---------------------------------
        int dy = top + benchH + 4;
        int dh = detailH;
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(4, dy, this.W - 8, dh, 4);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(4, dy, this.W - 8, dh, 4);
        if (this.buildSel < 20) {
            Part p = this.slots[this.buildSel];
            Text.draw(this.g, BuildView.slotTitle(this.buildSel), 9, dy + 2, 0,
                    Theme.ACCENT);
            if (p == null) {
                Text.draw(this.g, "\u043f\u0443\u0441\u0442\u043e \u2014 OK: \u0432\u044b\u0431\u0440\u0430\u0442\u044c",
                        9, dy + 2 + this.FH0, 0, Theme.MUTED);
                Text.draw(this.g, "\u2190 \u0443\u0431\u0440\u0430\u0442\u044c", 9,
                        dy + 2 + this.FH0 * 2, 0, Theme.MUTED);
            } else {
                Text.draw(this.g, Text.fit(p.name, 0, this.W - 20), 9,
                        dy + 2 + this.FH0, 0, Theme.TEXT);
                String meta = Util.money(p.price) + "   " + p.year
                        + "   \u0441\u043e\u0441\u0442. " + p.cond + "%";
                Text.draw(this.g, Text.fit(meta, 0, this.W - 20), 9,
                        dy + 2 + this.FH0 * 2, 0,
                        p.cond >= 60 ? Theme.GREEN : Theme.RED);
            }
        } else {
            Text.draw(this.g, BuildView.actionTitle(this.buildSel - 20), 9,
                    dy + 2, 0, Theme.ACCENT);
            Text.draw(this.g, BuildView.actionHint(this.buildSel - 20), 9,
                    dy + 2 + this.FH0, 0, Theme.TEXT);
            Text.draw(this.g, "\u2190\u2192 \u0432\u044b\u0431\u043e\u0440 \u2022 OK \u2014 \u043e\u0442\u043a\u0440\u044b\u0442\u044c",
                    9, dy + 2 + this.FH0 * 2, 0, Theme.MUTED);
        }

        // --- action buttons ----------------------------------------------
        int ay = dy + dh + 4;
        int bh = btnH;
        for (int i = 0; i < 7; i++) {
            int col = i % 2;
            int row = i / 2;
            int bw = (this.W - 14) / 2;
            int bx = 5 + col * (bw + 4);
            int by = ay + row * (bh + 2);
            boolean sel = (20 + i) == this.buildSel;
            this.g.setColor(sel ? 0x2A3D2E : Theme.PANEL);
            this.g.fillRoundRect(bx, by, bw, bh, 3);
            this.g.setColor(sel ? Theme.ACCENT : Theme.LINE);
            this.g.drawRoundRect(bx, by, bw, bh, 3);
            Text.draw(this.g, Text.fit(BuildView.actionShort(i), 0, bw - 8),
                    bx + 4, by + (bh - this.FH0) / 2, 0,
                    sel ? Theme.TEXT : Theme.DIM);
        }
        this.footer("\u2191\u2193 \u0441\u043b\u043e\u0442", "OK: \u0434\u0435\u0439\u0441\u0442\u0432\u0438\u0435");
    }

    
    private void paintShop() {
        rpc.Part a = (this.shopList.length <= 0) ? null : this.db.get(this.shopList[this.shopSel]);
        this.header(rpc.Score.SLOT_NAMES[this.shopSlot], (this.shopAllYears) ? "\u0432\u0441\u0435 \u0433\u043e\u0434\u044b" : "\u044d\u043f\u043e\u0445\u0430");
        int i = this.FH1 + 13;
        this.g.setColor(1780536);
        this.g.fillRect(4, i, this.W - 8, 76);
        this.g.setColor(3033694);
        this.g.drawRect(4, i, this.W - 8, 76);
        label0: {
            label1: {
                label2: {
                    if (a == null) {
                        break label2;
                    }
                    if (rpc.Assets.ready) {
                        break label1;
                    }
                }
                if (a == null) {
                    break label0;
                }
                this.g.setColor(8297134);
                this.g.drawText("\u043d\u0435\u0442 \u043a\u0430\u0440\u0442\u0438\u043d\u043a\u0438", 10, i + 8, 0);
                break label0;
            }
            this.g.setColor(791580);
            this.g.fillRect(6, i + 4, 88, 44);
            this.g.fillRect(6, i + 44 + 8, this.W - 12, 20);
            rpc.Assets.drawPart(this.g, a, 6, i + 4, 88, 44);
            rpc.Assets.drawStrip(this.g, a.cat, 6, i + 44 + 8, this.W - 12, 20);
            this.g.setColor(8297134);
            String s = (a.cat != 20) ? new StringBuffer("\u043f\u043e\u043a\u043e\u043b\u0435\u043d\u0438\u044f ").append(a.catName()).append(": ").append(rpc.Assets.editions(a.cat)).toString() : new StringBuffer("\u0432\u0438\u0434\u044b \u0442\u0435\u0445\u043d\u0438\u043a\u0438: ").append(rpc.Assets.editions(a.cat)).toString();
            this.g.drawText(s, 100, i + 6, 0);
            this.g.setColor(16763196);
            this.g.drawText(new StringBuffer().append(a.year).toString(), 100, i + 18, 1);
            this.g.setColor(8297134);
            this.g.drawText(this.fit(a.name, this.W - 88 - 26, 0), 100, i + 34, 0);
        }
        int i0 = this.FH0 + 7;
        int i1 = i + 76 + 6;
        int i2 = this.FH0 * 2 + 10;
        int i3 = (this.H - i1 - i2 - this.FH0 - 14) / i0;
        if (i3 < 1) {
            i3 = 1;
        }
        if (this.shopSel < this.shopTop) {
            this.shopTop = this.shopSel;
        }
        if (this.shopSel > this.shopTop + i3 - 1) {
            this.shopTop = this.shopSel - i3 + 1;
        }
        int i4 = this.budget - this.spent + ((this.slots[this.shopSlot] != null) ? this.slots[this.shopSlot].price : 0);
        int i5 = 0;
        while(true) {
            if (i5 < i3) {
                int i6 = this.shopTop + i5;
                if (i6 < this.shopList.length) {
                    rpc.Part a0 = this.db.get(this.shopList[i6]);
                    int i7 = i1 + i5 * i0;
                    boolean b = i6 == this.shopSel;
                    boolean b0 = a0.price <= i4;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i7, this.W - 8, i0 - 1);
                    if (rpc.Assets.ready) {
                        this.g.setColor(791580);
                        this.g.fillRect(6, i7, 26, i0 - 2);
                        rpc.Assets.drawPart(this.g, a0, 7, i7 + 1, 24, 16);
                    }
                    this.g.setColor(b ? 3793530 : b0 ? 15003895 : 8297134);
                    String s0 = new StringBuffer().append(a0.price).toString();
                    this.g.drawText(this.fit(a0.name, this.W - this.g.textWidth(s0, 0) - 58, 0), 36, i7 + 4, 0);
                    this.g.setColor(b0 ? 16763196 : 16739179);
                    this.g.drawText(s0, this.W - this.g.textWidth(s0, 0) - 8, i7 + 4, 0);
                    i5 = i5 + 1;
                    continue;
                }
            }
            int i8 = this.H - i2 - this.FH0 - 10;
            this.g.setColor(1780536);
            this.g.fillRect(0, i8, this.W, i2);
            this.g.setColor(3033694);
            this.g.drawLine(0, i8, this.W, i8);
            if (a == null) {
                this.g.setColor(8297134);
                this.g.drawText("\u041f\u0443\u0441\u0442\u043e", 8, i8 + 4, 0);
            } else {
                this.g.setColor(3793530);
                this.g.drawText(new StringBuffer(String.valueOf(a.year)).append(" \u2022 ").append(a.catName()).toString(), 8, i8 + 4, 0);
                this.g.setColor(15003895);
                String s1 = this.detailText(a);
                this.g.drawText(this.fit(s1, this.W - 16, 0), 8, i8 + 5 + this.FH0, 0);
            }
            this.footer(new StringBuffer("(").append(this.shopSel + 1).append("/").append(this.shopList.length).append(")").toString(), "3: \u0432\u0441\u0435 \u0433\u043e\u0434\u044b");
            return;
        }
    }
    
    private String detailText(rpc.Part a) {
        StringBuffer a0 = new StringBuffer();
        label1: if (a.cat != 1) {
            if (a.cat != 2) {
                if (a.cat != 3) {
                    if (a.cat != 4) {
                        int i = a.cat;
                        label2: {
                            label3: {
                                if (i == 5) {
                                    break label3;
                                }
                                if (a.cat == 7) {
                                    break label3;
                                }
                                if (a.cat == 8) {
                                    break label3;
                                }
                                if (a.cat != 6) {
                                    break label2;
                                }
                            }
                            a0.append(rpc.Score.ifaceName(a.f[0])).append(" \u2022 ").append(rpc.Util.size(a.f[1]));
                            if ((a.f[2]) <= 0) {
                                break label1;
                            }
                            a0.append(" \u2022 ").append(a.f[2]).append(" rpm");
                            break label1;
                        }
                        if (a.cat != 9) {
                            if (a.cat != 10) {
                                if (a.cat != 11) {
                                    if (a.cat != 12) {
                                        if (a.cat != 13) {
                                            if (a.cat != 14) {
                                                if (a.cat != 20) {
                                                    int i0 = a.cat;
                                                    label0: {
                                                        if (i0 == 15) {
                                                            break label0;
                                                        }
                                                        if (a.cat == 16) {
                                                            break label0;
                                                        }
                                                        if (a.cat == 17) {
                                                            break label0;
                                                        }
                                                        a0.append("\u043f\u043b\u0430\u0442\u0430 \u0440\u0430\u0441\u0448\u0438\u0440\u0435\u043d\u0438\u044f/\u0430\u043a\u0441\u0435\u0441\u0441\u0443\u0430\u0440");
                                                        break label1;
                                                    }
                                                    a0.append("\u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(a.f[0]).append("%");
                                                } else {
                                                    a0.append(this.prnKind(a.f[0])).append(" \u2022 ").append(this.prnPerf(a));
                                                }
                                            } else {
                                                a0.append(this.busName(a.f[0])).append(" \u2022 ").append(a.f[1]).append(" \u041c\u0431\u0438\u0442/\u0441");
                                                if ((a.f[2]) > 0) {
                                                    a0.append(" \u2022 Wi-Fi");
                                                }
                                            }
                                        } else {
                                            a0.append(this.busName(a.f[0])).append(" \u2022 ").append(a.f[1]).append(" \u0431\u0438\u0442");
                                            a0.append(" \u2022 \u043a\u0430\u043d\u0430\u043b\u043e\u0432 ").append(a.f[2]);
                                            a0.append(" \u2022 \u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(a.f[3]).append("%");
                                        }
                                    } else {
                                        a0.append(rpc.Score.monTech(a.f[0])).append(" ").append(a.f[1] / 10).append("\" \u2022 ");
                                        a0.append(a.f[2]).append("x").append(a.f[3]).append(" @").append(a.f[4]).append(" \u0413\u0446");
                                    }
                                } else {
                                    a0.append("\u0434\u043e ").append(a.f[1]).append(" \u0412\u0442 \u2022 ").append(this.coolKind(a.f[0]));
                                }
                            } else {
                                a0.append(rpc.Score.formName(a.f[0])).append(" \u2022 \u043e\u0442\u0441\u0435\u043a\u043e\u0432: ").append(a.f[1]);
                            }
                        } else {
                            a0.append(a.f[0]).append(" \u0412\u0442 \u2022 ").append(this.psuStd(a.f[1]));
                        }
                    } else {
                        a0.append(rpc.Score.ramTypeName(a.f[0])).append(" ").append(rpc.Util.size(a.f[1]));
                        a0.append(" \u2022 ").append(a.f[2]).append(" \u041c\u0422/\u0441");
                    }
                } else {
                    a0.append(this.sockName(a.f[0])).append(" \u2022 ").append(rpc.Score.ramTypeName(a.f[1]));
                    a0.append(" \u00d7").append(a.f[2]).append(" \u2022 \u043c\u0430\u043a\u0441 ").append(rpc.Util.size(a.f[3] * 1024));
                    a0.append(" \u2022 ").append(rpc.Score.formName(a.f[4]));
                }
            } else {
                a0.append(rpc.Util.size(a.f[1])).append(" \u2022 ").append(a.f[2]).append(" \u0431\u0438\u0442 \u2022 ");
                a0.append(a.f[3]).append(" \u041c\u0413\u0446 \u2022 ").append(a.f[4]).append(" \u0412\u0442 \u2022 ").append(this.busName(a.f[0]));
            }
        } else {
            a0.append(a.f[1]).append(" \u041c\u0413\u0446 \u2022 ").append(a.f[2]).append(" \u044f\u0434\u0440. \u2022 ").append(a.f[3]).append(" \u0412\u0442");
            a0.append(" \u2022 ").append(this.sockName(a.f[0]));
        }
        return a0.toString();
    }
    
    private String sockName(int i) {
        return this.db.dict(this.db.SOC, i);
    }
    
    private String busName(int i) {
        return rpc.Score.busName(i);
    }
    
    private String busNameOld(int i) {
        return this.db.dict(this.db.BUS, i);
    }
    
    private String psuStd(int i) {
        return this.db.dict(this.db.PST, i);
    }
    
    private String coolKind(int i) {
        return this.db.dict(this.db.CLK, i);
    }
    
    private void paintResult() {
        if (this.resultFrames < 6) {
            this.resultFrames = this.resultFrames + 1;
        }
        int i = this.resultFrames * 400;
        rpc.Db$Level a = this.effectiveLevel();
        this.header(new StringBuffer("\u0422\u0435\u0441\u0442 ").append(a.year).toString(), (this.sandbox) ? "\u043f\u0435\u0441\u043e\u0447\u043d\u0438\u0446\u0430" : new StringBuffer("\u0446\u0435\u043b\u044c ").append(a.passScore).toString());
        int i0 = this.FH1 + 20;
        this.g.setColor(3793530);
        this.g.drawText("POST ... OK", 8, i0, 1);
        int i1 = i0 + (this.FH1 + 4);
        if (i > 400) {
            i1 = this.unitRow(0, "CPU", i1);
        }
        if (i > 800) {
            i1 = this.unitRow(6, "VGA", i1);
        }
        if (i > 1200) {
            this.g.setColor(8297134);
            this.g.drawText(new StringBuffer("\u041e\u0417\u0423: ").append(rpc.Util.size(rpc.Score.totalRamKb(this.slots))).append("  \u0414\u0438\u0441\u043a: ").append((rpc.Score.firstDisk(this.slots) != null) ? rpc.Util.size((rpc.Score.firstDisk(this.slots).f[1] != 0) ? 1 : 0) : "-").toString(), 8, i1, 0);
            i1 = i1 + (this.FH0 + 8);
        }
        if (i > 1500) {
            this.bar("CPU", this.result.barsC, i1);
            int i2 = this.result.barsG;
            int i3 = i1 + (this.FH0 + 8);
            this.bar("GPU", i2, i3);
            int i4 = this.result.barsR;
            int i5 = i3 + (this.FH0 + 8);
            this.bar("RAM", i4, i5);
            int i6 = this.result.barsD;
            int i7 = i5 + (this.FH0 + 8);
            this.bar("DISK", i6, i7);
            i1 = i7 + (this.FH0 + 12);
        }
        if (this.result.ok) {
            if (i > 2000) {
                String s = new StringBuffer().append(this.result.score).toString();
                this.g.setColor((this.result.score < a.passScore) ? 16763196 : 3793530);
                this.g.drawText(s, this.W / 2 - this.g.textWidth(s, 2) / 2, i1, 2);
                this.g.setColor(8297134);
                String s0 = (this.result.score < a.goldScore) ? (this.result.score < a.passScore) ? "\u041c\u0430\u043b\u043e \u043e\u0447\u043a\u043e\u0432 \u2014 \u043d\u0443\u0436\u0435\u043d \u0430\u043f\u0433\u0440\u0435\u0439\u0434" : "\u0417\u0410\u0427\u0401\u0422. \u0413\u043e\u0434 \u043e\u0442\u043a\u0440\u044b\u0442" : "\u0417\u041e\u041b\u041e\u0422\u041e! \u042d\u0442\u0430\u043b\u043e\u043d \u043f\u0435\u0440\u0435\u043a\u0440\u044b\u0442";
                rpc.Gfx a0 = this.g;
                int i8 = this.W / 2 - this.g.textWidth(s0, 0) / 2;
                int i9 = i1 + (this.FH2 + 6);
                a0.drawText(s0, i8, i9, 0);
                this.g.setColor(8297134);
                String s1 = new StringBuffer("\u0431\u043e\u043d\u0443\u0441 +").append(this.result.bonus).append(" \u2022 \u043f\u0438\u0442\u0430\u043d\u0438\u0435 ").append(this.result.power).append(" \u0412\u0442").toString();
                this.g.drawText(s1, this.W / 2 - this.g.textWidth(s1, 0) / 2, i9 + (this.FH0 + 6), 0);
            }
        } else {
            this.g.setColor(16739179);
            this.g.drawText("\u0421\u0411\u041e\u0420\u041a\u0410 \u041d\u0415 \u0421\u0422\u0410\u0420\u0422\u0423\u0415\u0422:", 8, i1, 1);
            int i10 = i1 + (this.FH1 + 2);
            int i11 = 0;
            while(i11 < this.result.errors.length) {
                {
                    if (i11 < 4) {
                        this.g.setColor(16739179);
                        this.g.drawText(this.fit(new StringBuffer("- ").append(this.result.errors[i11]).toString(), this.W - 16, 0), 8, i10, 0);
                        i10 = i10 + (this.FH0 + 3);
                        i11 = i11 + 1;
                        continue;
                    }
                    break;
                }
            }
        }
        this.footer("OK \u2014 \u0432\u0435\u0440\u043d\u0443\u0442\u044c\u0441\u044f", "");
    }
    
    private int unitRow(int i, String s, int i0) {
        rpc.Part a = this.slots[i];
        if (a != null && rpc.Assets.ready) {
            rpc.Assets.drawPart(this.g, a, 6, i0, 32, 22);
        }
        this.g.setColor(8297134);
        this.g.drawText(s, 44, i0 + 4, 0);
        this.g.setColor(15003895);
        String s0 = (a != null) ? a.name : "-";
        this.g.drawText(this.fit(s0, this.W - 44 - 42, 0), 44 + this.g.textWidth(s, 0) + 6, i0 + 4, 0);
        return i0 + 22 + 2;
    }
    
    private void bar(String s, int i, int i0) {
        int i1 = this.W - 16 - 62;
        this.g.setColor(8297134);
        this.g.drawText(s, 8, i0, 0);
        this.g.setColor(1780536);
        this.g.fillRect(48, i0, i1, this.FH0 - 2);
        int i2 = i1 * Math.min(i, 100) / 100;
        this.g.setColor((i < 100) ? (i < 70) ? 16739179 : 16763196 : 3793530);
        this.g.fillRect(48, i0, i2, this.FH0 - 2);
        this.g.setColor(15003895);
        String s0 = new StringBuffer(String.valueOf((Object)String.valueOf(i))).append("%").toString();
        this.g.drawText(s0, 52 + i1, i0, 0);
    }
    
    private void paintMuseum() {
        this.header("\u041c\u0443\u0437\u0435\u0439 \u0441\u0431\u043e\u0440\u043e\u043a", "\u043b\u0443\u0447\u0448\u0438\u0435 \u043e\u0447\u043a\u0438");
        int i = this.FH0 + 8;
        int i0 = this.FH1 + 16;
        int i1 = (this.H - i0 - this.FH0 - 18) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.museumSel < this.museumTop) {
            this.museumTop = this.museumSel;
        }
        if (this.museumSel > this.museumTop + i1 - 1) {
            this.museumTop = this.museumSel - i1 + 1;
        }
        int dummy = this.best[this.museumSel];
        int i2 = 0;
        while(true) {
            if (i2 < i1) {
                int i3 = this.museumTop + i2;
                if (i3 < this.db.levels.length) {
                    int i4 = i0 + i2 * i;
                    boolean b = i3 == this.museumSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i4, this.W - 8, i - 1);
                    this.g.setColor(b ? 3793530 : 15003895);
                    this.g.drawText(new StringBuffer().append(this.db.levels[i3].year).toString(), 8, i4 + 4, 0);
                    this.g.setColor(8297134);
                    this.g.drawText(this.fit(this.db.levels[i3].title, this.W - 76, 0), 52, i4 + 4, 0);
                    this.g.setColor(((this.best[i3]) <= 0) ? 8297134 : 16763196);
                    String s = ((this.best[i3]) <= 0) ? "\u2014" : new StringBuffer().append(this.best[i3]).toString();
                    this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i4 + 4, 0);
                    i2 = i2 + 1;
                    continue;
                }
            }
            this.footer("\u2191\u2193 \u043b\u0438\u0441\u0442\u0430\u0442\u044c", "\u043b\u044e\u0431\u0430\u044f \u2014 \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintCatalog() {
        int i = 0;
        int i0 = 0;
        while(i0 < CAT_ORDER.length) {
            i = i + this.db.countInCat(CAT_ORDER[i0]);
            i0 = i0 + 1;
        }
        this.header("\u041a\u0430\u0442\u0430\u043b\u043e\u0433 \u0436\u0435\u043b\u0435\u0437\u0430", new StringBuffer().append(i).toString());
        int i1 = this.FH1 + 14;
        int i2 = (this.H - i1 - this.FH0 - 16) / 34;
        if (i2 < 1) {
            i2 = 1;
        }
        if (this.catSel < this.catTop) {
            this.catTop = this.catSel;
        }
        if (this.catSel > this.catTop + i2 - 1) {
            this.catTop = this.catSel - i2 + 1;
        }
        int i3 = 0;
        while(true) {
            if (i3 < i2) {
                int i4 = this.catTop + i3;
                if (i4 < CAT_ORDER.length) {
                    int i5 = CAT_ORDER[i4];
                    int i6 = i1 + i3 * 34;
                    boolean b = i4 == this.catSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i6, this.W - 8, 32);
                    if (b) {
                        this.g.setColor(3793530);
                        this.g.drawRect(4, i6, this.W - 8, 32);
                    }
                    this.g.setColor(791580);
                    this.g.fillRect(6, i6 + 3, 44, 26);
                    if (rpc.Assets.ready) {
                        rpc.Assets.drawEdition(this.g, i5, 2023, 6, i6 + 3, 44, 26);
                    }
                    this.g.setColor(b ? 3793530 : 15003895);
                    this.g.drawText(this.fit(rpc.Db.CAT_LABEL[i5], this.W - 110, 0), 56, i6 + 5, 0);
                    this.g.setColor(8297134);
                    this.g.drawText(new StringBuffer().append(this.db.countInCat(i5)).toString(), this.W - this.g.textWidth("0000", 0) - 10, i6 + 5, 0);
                    if (rpc.Assets.ready) {
                        this.g.setColor(16763196);
                        this.g.drawText(new StringBuffer(String.valueOf(rpc.Assets.editions(i5))).append(" \u043f\u043e\u043a\u043e\u043b\u0435\u043d\u0438\u0439 \u2022 ").append(rpc.Assets.variants()).append(" \u0438\u0441\u043f.").toString(), 56, i6 + 18, 0);
                    }
                    i3 = i3 + 1;
                    continue;
                }
            }
            this.footer("OK \u2014 \u0441\u043c\u043e\u0442\u0440\u0435\u0442\u044c", "1: \u043c\u0435\u043d\u044e");
            return;
        }
    }
    
    private void paintCatParts() {
        int i = CAT_ORDER[this.catSel];
        this.header(rpc.Db.CAT_LABEL[i], new StringBuffer().append(this.partList.length).toString());
        int i0 = this.FH1 + 13;
        label1: if (rpc.Assets.ready) {
            this.g.setColor(791580);
            this.g.fillRect(4, i0, this.W - 8, 26);
            int i1 = (this.partList.length <= 0) ? 2023 : this.db.get(this.partList[this.partSel]).year;
            int i2 = rpc.Assets.lookKind(i);
            label0: {
                if (i2 == 0) {
                    break label0;
                }
                if (i == 20) {
                    break label0;
                }
                rpc.Assets.drawVariants(this.g, i, i1, 6, i0 + 2, this.W - 12, 22);
                break label1;
            }
            rpc.Assets.drawStrip(this.g, i, 6, i0 + 2, this.W - 12, 22);
        }
        int i3 = this.H;
        int i4 = i0 + 26 + 4;
        int i5 = i3 - i4 - this.FH0 - 16;
        int i6 = this.FH0 + 8;
        int i7 = i5 / i6;
        if (i7 < 1) {
            i7 = 1;
        }
        if (this.partSel < this.partTop) {
            this.partTop = this.partSel;
        }
        if (this.partSel > this.partTop + i7 - 1) {
            this.partTop = this.partSel - i7 + 1;
        }
        int i8 = 0;
        while(true) {
            if (i8 < i7) {
                int i9 = this.partTop + i8;
                if (i9 < this.partList.length) {
                    rpc.Part a = this.db.get(this.partList[i9]);
                    int i10 = i4 + i8 * i6;
                    boolean b = i9 == this.partSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i10, this.W - 8, i6 - 1);
                    if (rpc.Assets.ready) {
                        this.g.setColor(791580);
                        this.g.fillRect(6, i10 + 1, 24, 16);
                        rpc.Assets.drawPart(this.g, a, 6, i10 + 1, 24, 16);
                    }
                    this.g.setColor(16763196);
                    this.g.drawText(new StringBuffer().append(a.year).toString(), 34, i10 + 4, 0);
                    this.g.setColor(b ? 15003895 : 8297134);
                    this.g.drawText(this.fit(a.name, this.W - 66 - 40, 0), 66, i10 + 4, 0);
                    String s = new StringBuffer().append(a.price).toString();
                    this.g.setColor(8297134);
                    this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i10 + 4, 0);
                    i8 = i8 + 1;
                    continue;
                }
            }
            this.footer("\u2191\u2193 \u043b\u0438\u0441\u0442\u0430\u0442\u044c", "OK \u2014 \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    /** About / credits card: icon rows, short lines, no wall of text. */
    private void paintAbout() {
        int w = this.W;
        int h = this.H;
        // title block
        this.g.setColor(Theme.PANEL);
        this.g.fillRoundRect(6, 4, w - 12, this.FH1 + 18, 6);
        this.g.setColor(Theme.LINE);
        this.g.drawRoundRect(6, 4, w - 12, this.FH1 + 18, 6);
        Icons.drawSized(this.g, Icons.MONITOR, 11, 8, this.FH1 + 10,
                Theme.ACCENT, Theme.ACCENT2, 0x8A6A20);
        Text.draw(this.g, "RETRO PC BUILDER", 20 + this.FH1 + 10, 8, 1,
                Theme.ACCENT);
        Text.draw(this.g, "v5.0  \u2022  1979-2027", 20 + this.FH1 + 10,
                10 + this.FH1, 0, Theme.MUTED);

        int rowH = this.FH0 + 12;
        int y = 14 + this.FH1 + 18;
        int[] icons = { Icons.CART, Icons.WRENCH, Icons.FLASK, Icons.CAT,
                        Icons.CHIP };
        String[] head = {
            "\u0416\u0435\u043b\u0435\u0437\u043e",
            "\u0420\u0435\u043c\u043e\u043d\u0442",
            "\u042d\u043a\u0441\u043f\u0435\u0440\u0438\u043c\u0435\u043d\u0442\u044b",
            "\u0416\u0438\u0437\u043d\u044c",
            "\u041e\u0411 \u0421 \u0438 \u0441\u0435\u0442\u044c",
        };
        String[] body = {
            this.db.count() + " \u0434\u0435\u0442\u0430\u043b\u0435\u0439 \u2022 \u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0430 \u0441\u0443\u0442\u043a\u0430\u043c\u0438",
            "\u043f\u0430\u0439\u043a\u0430, \u0442\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u0430, \u043f\u044b\u043b\u044c, \u043a\u043e\u043d\u0434\u0435\u043d\u0441\u0430\u0442\u043e\u0440\u044b",
            "1000 \u043e\u043f\u044b\u0442\u043e\u0432 \u2022 Win11 \u043d\u0430 \u0436\u0435\u043b\u0435\u0437\u0435 2005",
            "\u0447\u0430\u0439, \u043a\u043e\u0442, \u043f\u043e\u0433\u043e\u0434\u0430, \u0440\u0430\u0434\u0438\u043e",
            "BASIC / ASM / C \u2022 \u0437\u0430\u043a\u0430\u0437\u044b \u2022 \u043c\u0430\u0439\u043d\u0438\u043d\u0433",
        };
        int iconS = rowH - 10;
        for (int i = 0; i < icons.length && y + rowH < h - this.FH0 - 20; i++) {
            this.g.setColor(i % 2 == 0 ? Theme.PANEL : Theme.PANEL2);
            this.g.fillRoundRect(6, y, w - 12, rowH, 3);
            Icons.drawSized(this.g, icons[i], 11, y + 5, iconS,
                    Theme.ACCENT2, Theme.ACCENT, 0x2A3550);
            int tx = 16 + iconS;
            Text.draw(this.g, head[i], tx, y + 3, 0, Theme.TEXT);
            Text.draw(this.g, Text.fit(body[i], 0, w - tx - 8), tx,
                    y + 5 + this.FH0, 0, Theme.MUTED);
            y += rowH + 3;
        }
        this.footer("\u041d\u0430\u0437\u0430\u0434", "");
    }

    
    public int levelYear() {
        if (this.db != null && this.db.levels != null && this.db.levels.length != 0) {
            if (this.levelIdx >= 0 && this.levelIdx < this.db.levels.length) {
                return this.db.levels[this.levelIdx].year;
            }
            return 2026;
        }
        return 2026;
    }
    
    private String actLabel(int i) {
        if (i == 20) {
            return "\u0420\u044b\u043d\u043a\u0438: \u0431\u0430\u0437\u0430\u0440, \u0431\u0430\u0440\u0430\u0445\u043e\u043b\u043a\u0430, Avito";
        }
        if (i == 21) {
            return "\u0414\u043e\u0441\u0442\u0430\u0432\u043a\u0430: \u0437\u0430\u043a\u0430\u0437\u044b";
        }
        if (i == 22) {
            return "\u0421\u0442\u043e\u043b \u0441\u0431\u043e\u0440\u043a\u0438: \u043f\u0440\u0438\u0432\u0435\u0437\u043b\u0438";
        }
        if (i == 23) {
            return "\u041e\u0421, \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b \u0438 \u0441\u0435\u0442\u044c";
        }
        if (i == 24) {
            return "\u0416\u0434\u0430\u0442\u044c \u0441\u0443\u0442\u043a\u0438 (\u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0430, \u043c\u043e\u043d\u0442\u0430\u0436)";
        }
        if (i != 25) {
            return ">>> \u0417\u0410\u041f\u0423\u0421\u0422\u0418\u0422\u042c \u0422\u0415\u0421\u0422 <<<";
        }
        return "BIOS / POST: \u0434\u0438\u0430\u0433\u043d\u043e\u0441\u0442\u0438\u043a\u0430";
    }
    
    private int rnd(int i) {
        this.seed = this.seed * 1103515245 + 12345;
        int i0 = this.seed >>> 8 & 2147483647;
        return (i > 0) ? i0 % i : 0;
    }
    
    private void openMarkets() {
        this.marketSel = 0;
        this.marketTop = 0;
        this.mode = 9;
    }
    
    private void openMarket(int i) {
        this.openMarket = i;
        this.offerSel = 0;
        this.offerTop = 0;
        if (i == 3) {
            this.advanceDay(1);
        }
        this.rollOffers(i);
        if (i == 3) {
            this.junkFind();
        }
        this.mode = 10;
    }
    
    private void junkFind() {
        if (this.rnd(100) < 25 && this.table.n + 2 <= 12) {
            int i = this.levelYear();
            int[] a = this.db.query(ALL_CATS, Math.max(1979, i - 18), i);
            if (a.length == 0) {
                return;
            }
            String s = "";
            int i0 = 0;
            while(i0 < 2) {
                rpc.Part a0 = this.db.get((a[this.rnd(a.length)] != 0) ? 1 : 0);
                int i1 = 12 + this.rnd(35);
                this.table.addToTable(a0, i1, 45 + this.rnd(45));
                s = new StringBuffer(String.valueOf((Object)String.valueOf((Object)s))).append((i0 <= 0) ? "" : ", ").append(a0.name).toString();
                i0 = i0 + 1;
            }
            this.toastMsg(new StringBuffer("\u041d\u0430\u0448\u0451\u043b \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u0438\u043a: ").append(s).toString());
            return;
        }
    }
    
    private void rollOffers(int i) {
        int i0 = this.levelYear();
        int[] a = rpc.Market.yearWindow(i, i0);
        int[] a0 = this.db.query(ALL_CATS, a[0], a[1]);
        int i1 = (i != 3) ? 16 : 6;
        if (a0.length == 0) {
            i1 = 0;
        }
        this.offerPart = new int[i1];
        this.offerPrice = new int[i1];
        this.offerCond = new int[i1];
        this.offerDays = new int[i1];
        this.offerDust = new int[i1];
        this.offerBundle = new int[i1][];
        int i2 = rpc.Market.pricePct(i);
        int i3 = 0;
        while(i3 < i1) {
            int i4 = 0;
            boolean b = a0[this.rnd(a0.length)] != 0;
            rpc.Part a1 = this.db.get(b ? 1 : 0);
            this.offerPart[i3] = b ? 1 : 0;
            int i5 = a1.price * i2 / 100;
            if (i != 3) {
                i4 = i5 * (85 + this.rnd(31)) / 100;
                if (i4 < 1) {
                    i4 = 1;
                }
            } else {
                i4 = 0;
            }
            this.offerPrice[i3] = i4;
            int i6 = rpc.Market.condMin(i);
            int i7 = rpc.Market.condMax(i);
            this.offerCond[i3] = i6 + this.rnd(i7 - i6 + 1);
            this.offerDust[i3] = this.dustFor(i);
            this.offerDays[i3] = rpc.Market.days(i, this.netSpeed());
            if (i == 4 && this.rnd(100) < 30) {
                this.offerBundle[i3] = this.makeBundle(i0);
                if (this.offerBundle[i3] != null) {
                    int i8 = 0;
                    int i9 = 0;
                    while(i9 < this.offerBundle[i3].length) {
                        i8 = i8 + this.db.get(this.offerBundle[i3][i9]).price;
                        i9 = i9 + 1;
                    }
                    this.offerPrice[i3] = i8 * 55 / 100 + this.rnd(40);
                }
            }
            i3 = i3 + 1;
        }
    }
    
    private int dustFor(int i) {
        if (i != 0 && i != 5) {
            if (i == 1) {
                return 10 + this.rnd(30);
            }
            if (i == 2) {
                return 20 + this.rnd(40);
            }
            if (i == 3) {
                return 40 + this.rnd(50);
            }
            if (i != 6) {
                return 5 + this.rnd(45);
            }
            return 30 + this.rnd(40);
        }
        return 0;
    }
    
    private int[] makeBundle(int i) {
        int i0 = i - this.rnd(7);
        if (i0 < 1979) {
            i0 = 1979;
        }
        rpc.Db a = this.db;
        int[] a0 = new int[1];
        a0[0] = 1;
        int[] a1 = a.query(a0, i0, i0);
        if (a1.length == 0) {
            int[] a2 = null;
            return a2;
        }
        rpc.Part a3 = this.db.get((a1[this.rnd(a1.length)] != 0) ? 1 : 0);
        rpc.Db a4 = this.db;
        int[] a5 = new int[1];
        a5[0] = 3;
        int[] a6 = a4.query(a5, i0, i0 + 1);
        int i1 = 0;
        label0: while(true) {
            int i2 = 0;
            if (i1 < a6.length) {
                if (this.db.get(a6[i1]).f[0] != 0 != (a3.f[0] != 0)) {
                    i1 = i1 + 1;
                    continue label0;
                }
                i2 = a6[i1];
            } else {
                i2 = -1;
            }
            if (i2 < 0) {
                int[] a7 = null;
                return a7;
            }
            rpc.Part a8 = this.db.get(i2);
            rpc.Db a9 = this.db;
            int[] a10 = new int[1];
            a10[0] = 4;
            int[] a11 = a9.query(a10, i0 - 3, i0);
            int i3 = 0;
            while(true) {
                int i4 = 0;
                if (i3 < a11.length) {
                    if (this.db.get(a11[i3]).f[0] != 0 != (a8.f[1] != 0)) {
                        i3 = i3 + 1;
                        continue;
                    }
                    i4 = a11[i3];
                } else {
                    i4 = -1;
                }
                if (i4 < 0) {
                    int[] a12 = null;
                    return a12;
                }
                rpc.Db a13 = this.db;
                int[] a14 = new int[2];
                a14[0] = 5;
                a14[1] = 7;
                int[] a15 = a13.query(a14, i0 - 2, i0);
                if (a15.length == 0) {
                    int[] a16 = null;
                    return a16;
                }
                rpc.Db a17 = this.db;
                int[] a18 = new int[2];
                a18[0] = 2;
                a18[1] = 19;
                int[] a19 = a17.query(a18, i0 - 2, i0);
                int i5 = (a19.length <= 0) ? 4 : 5;
                int[] a20 = new int[i5];
                a20[0] = a3.index;
                a20[1] = i2;
                a20[2] = i4;
                a20[3] = (a15[this.rnd(a15.length)] != 0) ? 1 : 0;
                if (i5 == 5) {
                    a20[4] = (a19[this.rnd(a19.length)] != 0) ? 1 : 0;
                }
                return a20;
            }
        }
    }
    
    private void buyOffer() {
        if (this.offerSel >= 0 && this.offerSel < this.offerPart.length) {
            boolean b = false;
            if (this.table.n >= 12) {
                this.toastMsg("\u0421\u0442\u043e\u043b \u0437\u0430\u0431\u0438\u0442: \u043e\u0441\u0432\u043e\u0431\u043e\u0434\u0438\u0442\u0435 \u043c\u0435\u0441\u0442\u043e (\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0438\u043b\u0438 \u043f\u0440\u043e\u0434\u0430\u0439\u0442\u0435)");
                return;
            }
            int i = this.offerPrice[this.offerSel];
            int i0 = this.openMarket;
            label5: {
                label3: {
                    label4: {
                        if (i0 != 5) {
                            break label4;
                        }
                        if (this.levelYear() >= 2002) {
                            break label3;
                        }
                    }
                    b = false;
                    break label5;
                }
                b = true;
            }
            if (b) {
                i = i * 102 / 100;
            }
            if (i > this.cash) {
                this.toastMsg(new StringBuffer("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442: \u043d\u0443\u0436\u043d\u043e ").append(i).append(", \u0435\u0441\u0442\u044c ").append(this.cash).toString());
                this.host.vibrate(60);
                return;
            }
            this.cash = this.cash - i;
            int i1 = this.offerDays[this.offerSel];
            int i2 = this.offerCond[this.offerSel];
            boolean b0 = this.offerDust[this.offerSel] != 0;
            if (this.offerBundle != null && this.offerSel < this.offerBundle.length && this.offerBundle[this.offerSel] != null) {
                int[] a = this.offerBundle[this.offerSel];
                int i3 = 0;
                while(i3 < a.length) {
                    rpc.Part a0 = this.db.get(a[i3]);
                    int i4 = 35 + this.rnd(50);
                    this.table.order(a0, 0, i4, 20 + this.rnd(60), this.day + i1, this.openMarket);
                    i3 = i3 + 1;
                }
                this.toastMsg(new StringBuffer("\u0413\u043e\u0442\u043e\u0432\u044b\u0439 \u0441\u0438\u0441\u0442\u0435\u043c\u043d\u0438\u043a \u0438\u0437 ").append(a.length).append(" \u0434\u0435\u0442\u0430\u043b\u0435\u0439 \u2014 \u043f\u0440\u0438\u0435\u0434\u0435\u0442 \u0447\u0435\u0440\u0435\u0437 ").append(i1).append(" \u0434\u043d.").append(b ? " (\u043e\u043f\u043b\u0430\u0447\u0435\u043d\u043e \u042f\u043d\u0434\u0435\u043a\u0441.\u0414\u0435\u043d\u044c\u0433\u0430\u043c\u0438)" : "").toString());
                this.mode = 11;
                return;
            }
            rpc.Part a1 = this.db.get(this.offerPart[this.offerSel]);
            if (rpc.Market.scamRisk(this.openMarket) && this.rnd(100) < 12) {
                i2 = i2 / 2;
                a1.name = new StringBuffer(String.valueOf((Object)String.valueOf((Object)a1.name))).append(" (\u043d\u0435 \u0442\u043e, \u0447\u0442\u043e \u043d\u0430 \u0444\u043e\u0442\u043e)").toString();
            }
            label2: if (this.openMarket == 5 && this.rnd(100) < 20) {
                a1.name = new StringBuffer(String.valueOf((Object)String.valueOf((Object)a1.name))).append(" (\u043f\u043e\u0434\u0434\u0435\u043b\u043a\u0430)").toString();
                i2 = 70 + this.rnd(30);
                int i5 = a1.cat;
                label0: {
                    label1: {
                        if (i5 == 2) {
                            break label1;
                        }
                        if (a1.cat != 19) {
                            break label0;
                        }
                    }
                    a1.f[5] = a1.f[5] * 2 / 3;
                    break label2;
                }
                if (a1.cat != 1) {
                    if (a1.cat == 4) {
                        a1.f[1] = a1.f[1] * 2 / 3;
                    }
                } else {
                    a1.f[4] = a1.f[4] * 2 / 3;
                }
            }
            this.table.order(a1, i, i2, b0 ? 1 : 0, this.day + i1, this.openMarket);
            this.toastMsg((i1 > 0) ? new StringBuffer("\u041e\u043f\u043b\u0430\u0447\u0435\u043d\u043e, \u043f\u0440\u0438\u0435\u0434\u0435\u0442 \u0447\u0435\u0440\u0435\u0437 ").append(i1).append(" \u0434\u043d.").toString() : new StringBuffer("\u0417\u0430\u0431\u0440\u0430\u043b \u0441\u0440\u0430\u0437\u0443: ").append(a1.name).toString());
            this.mode = 11;
            return;
        }
    }
    
    private void advanceDay(int i) {
        if (i < 1) {
            i = 1;
        }
        int i0 = 0;
        while(i0 < i) {
            this.day = this.day + 1;
            int i1 = this.table.tick(this.day);
            if (i1 > 0) {
                this.toastMsg(new StringBuffer("\u041d\u0430 \u0441\u0442\u043e\u043b \u043f\u0440\u0438\u0435\u0445\u0430\u043b\u043e \u0434\u0435\u0442\u0430\u043b\u0435\u0439: ").append(i1).toString());
            }
            i0 = i0 + 1;
        }
    }
    
    private int slotForCat(int i) {
        if (i == 1) {
            return 0;
        }
        if (i == 3) {
            return 1;
        }
        if (i != 4) {
            if (i != 2 && i != 19) {
                if (i != 5 && i != 7 && i != 8 && i != 6) {
                    if (i == 9) {
                        return 10;
                    }
                    if (i == 10) {
                        return 11;
                    }
                    if (i == 11) {
                        return 12;
                    }
                    if (i == 12) {
                        return 13;
                    }
                    if (i == 13) {
                        return 14;
                    }
                    if (i == 15) {
                        return 15;
                    }
                    if (i == 16) {
                        return 16;
                    }
                    if (i != 14) {
                        return 18;
                    }
                    return 17;
                }
                int i0 = 0;
                while(i0 < 3) {
                    if (this.slots[7 + i0] == null) {
                        return 7 + i0;
                    }
                    i0 = i0 + 1;
                }
                return 7;
            }
            return 6;
        } else {
            int i1 = 0;
            while(i1 < 4) {
                if (this.slots[2 + i1] == null) {
                    return 2 + i1;
                }
                i1 = i1 + 1;
            }
            return 2;
        }
    }
    
    private void installFromBench(int i) {
        if (i >= 0 && i < this.table.n) {
            rpc.Part a = this.table.parts[i];
            int i0 = this.slotForCat(a.cat);
            String s = rpc.Compat.why(i0, a, this.slots);
            if (s != null) {
                rpc.Sound.error();
                this.host.vibrate(80);
                this.toastMsg(new StringBuffer("\u041d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442 \u0432 ").append(rpc.Score.SLOT_NAMES[i0]).append(": ").append(s).toString());
                return;
            }
            rpc.Part a0 = this.slots[i0];
            if (a0 != null) {
                this.table.addToTable(a0, a0.cond);
                this.cash = this.cash + 0;
            }
            a.cond = this.table.cond[i];
            a.dust = this.table.dust[i];
            this.slots[i0] = a;
            this.table.removeAt(i);
            this.recalcSpent();
            if (this.benchSel >= this.table.n) {
                this.benchSel = (this.table.n <= 0) ? 0 : this.table.n - 1;
            }
            this.toastMsg(new StringBuffer(String.valueOf((Object)String.valueOf((Object)a.name))).append(" \u2192 ").append(rpc.Score.SLOT_NAMES[i0]).append((a.cond >= 100) ? "" : new StringBuffer(" (").append(a.cond).append("%)").toString()).toString());
            return;
        }
    }
    
    private int repairCost(rpc.Part a, int i) {
        if (i >= 100) {
            return 0;
        }
        return 5 + ((a.price <= 0) ? 40 : a.price) * (100 - i) / 200;
    }
    
    private void repairBench(int i) {
        if (i >= 0 && i < this.table.n) {
            int i0 = this.repairCost(this.table.parts[i], this.table.cond[i]);
            if (i0 <= 0) {
                this.toastMsg("\u042d\u0442\u0430 \u0434\u0435\u0442\u0430\u043b\u044c \u0438 \u0442\u0430\u043a \u043a\u0430\u043a \u043d\u043e\u0432\u0430\u044f");
                return;
            }
            if (this.cash < i0) {
                this.toastMsg(new StringBuffer("\u0420\u0435\u043c\u043e\u043d\u0442 \u0441\u0442\u043e\u0438\u0442 ").append(i0).append(", \u0430 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432 ").append(this.cash).toString());
                return;
            }
            this.cash = this.cash - i0;
            this.table.cond[i] = 100;
            this.toastMsg(new StringBuffer("\u041f\u043e\u0447\u0438\u043d\u0438\u043b: ").append(this.table.parts[i].name).append(" (-").append(i0).append(")").toString());
            return;
        }
    }
    
    private void sellBench(int i) {
        if (i >= 0 && i < this.table.n) {
            rpc.Part a = this.table.parts[i];
            int i0 = a.price * this.table.cond[i] / 200;
            if (i0 < 1) {
                i0 = 1;
            }
            this.cash = this.cash + i0;
            this.table.removeAt(i);
            if (this.benchSel >= this.table.n) {
                this.benchSel = (this.table.n <= 0) ? 0 : this.table.n - 1;
            }
            this.toastMsg(new StringBuffer("\u0421\u0434\u0430\u043b ").append(a.name).append(" \u0437\u0430 ").append(i0).toString());
            return;
        }
    }
    
    private rpc.Part slot(int i) {
        return this.slots[i];
    }
    
    private boolean osReady() {
        if (this.slots[0] != null && this.slots[1] != null) {
            boolean b = false;
            int i = 0;
            while(i < 4) {
                if (this.slots[2 + i] != null) {
                    b = true;
                }
                i = i + 1;
            }
            if (!b) {
                return false;
            }
            int i0 = 0;
            while(i0 < 3) {
                if (this.slots[7 + i0] != null) {
                    return true;
                }
                i0 = i0 + 1;
            }
            return false;
        }
        return false;
    }
    
    private void openOs() {
        this.osSel = 0;
        this.mode = 13;
        if (!this.osReady()) {
            this.toastMsg("\u0414\u043b\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u044b \u043d\u0443\u0436\u043d\u044b CPU, \u043f\u043b\u0430\u0442\u0430, \u041e\u0417\u0423 \u0438 \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044c");
        }
    }
    
    private int osItems() {
        return (this.osReady()) ? 11 : 1;
    }
    
    private void osAction() {
        if (!this.osReady()) {
            this.mode = 2;
            return;
        }
        if (this.osSel != 0) {
            if (this.osSel != 1) {
                if (this.osSel != 2) {
                    if (this.osSel != 3) {
                        if (this.osSel != 4) {
                            if (this.osSel != 5) {
                                if (this.osSel != 6) {
                                    if (this.osSel != 7) {
                                        if (this.osSel != 8) {
                                            if (this.osSel != 9) {
                                                this.mode = 2;
                                            } else {
                                                this.runGame();
                                            }
                                        } else {
                                            this.openGames();
                                        }
                                    } else {
                                        this.osListSel = 0;
                                        this.mode = 21;
                                    }
                                } else {
                                    this.mode = 19;
                                }
                            } else {
                                this.openShell();
                            }
                        } else {
                            this.siteSel = 0;
                            this.browseSite = -1;
                            this.mode = 30;
                        }
                    } else {
                        this.mode = 18;
                    }
                } else {
                    this.taskSel = 0;
                    this.mode = 17;
                }
            } else {
                this.openEditor(this.editorLang);
            }
        } else {
            this.progSel = 0;
            this.mode = 14;
        }
    }
    
    private void openEditor(int i) {
        if (!rpc.Os.langAvailable(i, this.levelYear())) {
            this.toastMsg(new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Vm.LANG_NAME[i]))).append(" ").append(rpc.Os.langLocked(i)).toString());
            i = 0;
        }
        this.editorLang = i;
        this.editorSel = 0;
        this.mode = 15;
    }
    
    private void cycleLang(int i) {
        int i0 = this.editorLang;
        int i1 = 0;
        while(i1 < 3) {
            i0 = (i0 + i + 3) % 3;
            if (rpc.Os.langAvailable(i0, this.levelYear())) {
                this.editorLang = i0;
                this.editorSel = 0;
                this.toastMsg(new StringBuffer("\u042f\u0437\u044b\u043a: ").append(rpc.Vm.LANG_NAME[i0]).toString());
                return;
            }
            i1 = i1 + 1;
        }
        this.toastMsg("\u041f\u043e\u043a\u0430 \u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d \u0442\u043e\u043b\u044c\u043a\u043e BASIC-");
    }
    
    private void typeLine(int i) {
        String s = null;
        if (i > this.editorN) {
            i = this.editorN;
        }
        if (i == this.editorN && this.editorN >= 40) {
            this.toastMsg("\u0421\u0442\u0440\u043e\u043a \u0443\u0436\u0435 40 \u2014 \u0434\u0430\u043b\u044c\u0448\u0435 \u043d\u0435\u043a\u0443\u0434\u0430");
            return;
        }
        int i0 = this.editorN;
        label2: {
            label0: {
                label1: {
                    if (i >= i0) {
                        break label1;
                    }
                    if (this.editorLines[i] != null) {
                        break label0;
                    }
                }
                s = "";
                break label2;
            }
            s = this.editorLines[i];
        }
        String s0 = this.host.textInput(new StringBuffer("\u0421\u0442\u0440\u043e\u043a\u0430 ").append(i + 1).append(" \u2022 ").append(rpc.Vm.LANG_NAME[this.editorLang]).toString(), s, 60);
        if (s0 == null) {
            return;
        }
        String s1 = s0.trim();
        if (s1.length() == 0) {
            this.deleteLine(i);
            return;
        }
        if (i < this.editorN) {
            this.editorLines[i] = s1;
        } else {
            this.editorLines[this.editorN] = s1;
            this.editorN = this.editorN + 1;
        }
        this.editorSel = i + 1;
        rpc.Sound.click();
    }
    
    private void insertLine(int i) {
        if (this.editorN >= 40) {
            this.toastMsg("\u0421\u0442\u0440\u043e\u043a \u0443\u0436\u0435 40");
            return;
        }
        if (i > this.editorN) {
            i = this.editorN;
        }
        int i0 = this.editorN;
        while(i0 > i) {
            this.editorLines[i0] = this.editorLines[i0 - 1];
            i0 = i0 + -1;
        }
        this.editorN = this.editorN + 1;
        this.editorLines[i] = null;
        this.editorSel = i;
        this.typeLine(i);
    }
    
    private void deleteLine(int i) {
        if (i >= 0 && i < this.editorN) {
            while(i < this.editorN - 1) {
                this.editorLines[i] = this.editorLines[i + 1];
                i = i + 1;
            }
            this.editorLines[this.editorN - 1] = null;
            this.editorN = this.editorN - 1;
            if (this.editorSel > 0 && this.editorSel > this.editorN) {
                this.editorSel = this.editorN;
            }
            rpc.Sound.error();
            return;
        }
    }
    
    private void editorUndo() {
        this.deleteLine((this.editorSel <= 0) ? 0 : this.editorSel - 1);
    }
    
    private void saveProgram() {
        if (this.editorN == 0) {
            this.toastMsg("\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 \u043f\u0443\u0441\u0442\u0430\u044f");
            return;
        }
        rpc.Os$Prog a = new rpc.Os$Prog();
        a.lang = this.editorLang;
        a.lines = new String[this.editorN];
        System.arraycopy((Object)this.editorLines, 0, (Object)a.lines, 0, this.editorN);
        a.name = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Vm.LANG_NAME[this.editorLang]))).append(" #").append(this.progs.length + 1).toString();
        rpc.Os$Prog[] a0 = new rpc.Os$Prog[this.progs.length + 1];
        System.arraycopy((Object)this.progs, 0, (Object)a0, 0, this.progs.length);
        a0[this.progs.length] = a;
        this.progs = a0;
        this.toastMsg(new StringBuffer("\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e: ").append(a.name).toString());
    }
    
    private void autoSaveProgram() {
        if (this.editorN == 0) {
            return;
        }
        int i = 0;
        while(i < this.progs.length) {
            if (this.progs[i].lang == this.editorLang && rpc.Os.sameLines(this.progs[i].lines, this.editorLines, this.editorN)) {
                return;
            }
            i = i + 1;
        }
        this.saveProgram();
    }
    
    private void runEditor() {
        this.autoSaveProgram();
        this.lastRun = rpc.Vm.run(this.editorLang, this.editorLines);
        this.runTitle = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Vm.LANG_NAME[this.editorLang]))).append(" \u2022 \u0437\u0430\u043f\u0443\u0441\u043a").toString();
        this.runBack = 15;
        this.mode = 16;
    }
    
    private void runSaved(int i) {
        if (i >= 0 && i < this.progs.length) {
            this.lastRun = rpc.Vm.run(this.progs[i].lang, this.progs[i].lines);
            this.runTitle = this.progs[i].name;
            this.runBack = 14;
            this.mode = 16;
            return;
        }
    }
    
    private void checkTask(int i) {
        if (i >= 0 && i < this.tasks.length) {
            rpc.Os$Task a = this.tasks[i];
            if (a.done) {
                this.toastMsg("\u042d\u0442\u043e\u0442 \u0437\u0430\u043a\u0430\u0437 \u0443\u0436\u0435 \u0432\u044b\u043f\u043e\u043b\u043d\u0435\u043d");
                return;
            }
            if (this.editorN == 0) {
                this.toastMsg("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u043d\u0430\u043f\u0438\u0448\u0438\u0442\u0435 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0443 \u0432 \u0440\u0435\u0434\u0430\u043a\u0442\u043e\u0440\u0435");
                return;
            }
            if (this.editorLang != a.lang) {
                this.toastMsg(new StringBuffer("\u0417\u0430\u043a\u0430\u0437 \u043d\u0430 ").append(rpc.Vm.LANG_NAME[a.lang]).append(", \u0430 \u0443 \u0432\u0430\u0441 ").append(rpc.Vm.LANG_NAME[this.editorLang]).toString());
                return;
            }
            rpc.Vm$Run a0 = rpc.Vm.run(a.lang, this.editorLines);
            if (rpc.Vm.solves(a0, a.expect)) {
                a.done = true;
                this.cash = this.cash + a.reward;
                this.toastMsg(new StringBuffer("\u0417\u0430\u043a\u0430\u0437 \u0441\u0434\u0430\u043d! +").append(a.reward).append(" \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432").toString());
                this.host.vibrate(40);
            } else if (a0.ok) {
                this.toastMsg(new StringBuffer("\u0412\u044b\u0432\u043e\u0434 \u043d\u0435 \u0442\u043e\u0442: \u043d\u0443\u0436\u043d\u043e ").append(a.expect).append(", \u043f\u043e\u043b\u0443\u0447\u0438\u043b\u043e\u0441\u044c ").append(a0.lastNum()).toString());
            } else {
                this.toastMsg((a0.error != null) ? a0.error : "\u041e\u0448\u0438\u0431\u043a\u0430 \u0432 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0435");
            }
            return;
        }
    }
    
    private rpc.Part netPart() {
        rpc.Part a = this.slots[17];
        if (a == null) {
            rpc.Part a0 = null;
            return a0;
        }
        if (a.year > this.levelYear()) {
            rpc.Part a1 = null;
            return a1;
        }
        return a;
    }
    
    public static int mobileSpeed(int i) {
        if (i < 2003) {
            return 0;
        }
        if (i < 2005) {
            return 0;
        }
        if (i < 2008) {
            return 1;
        }
        if (i < 2011) {
            return 5;
        }
        if (i < 2014) {
            return 15;
        }
        if (i < 2018) {
            return 40;
        }
        if (i < 2021) {
            return 80;
        }
        if (i >= 2025) {
            return 500;
        }
        return 200;
    }
    
    private int netSpeed() {
        int i = 0;
        rpc.Part a = this.netPart();
        int i0 = (a != null) ? a.f[1] : 0;
        if (this.simBought) {
            i = rpc.Game.mobileSpeed(this.levelYear());
            if (i <= i0) {
                i = i0;
            }
        } else {
            i = i0;
        }
        return i;
    }
    
    private String netKind() {
        rpc.Part a = this.netPart();
        if (a != null) {
            int b = a.f[1];
            if (b < 1) {
                return "dial-up \u0447\u0435\u0440\u0435\u0437 \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u043d\u0443\u044e \u043b\u0438\u043d\u0438\u044e";
            }
            if (b < 20) {
                return "ADSL / \u0432\u044b\u0434\u0435\u043b\u0435\u043d\u043d\u0430\u044f \u043b\u0438\u043d\u0438\u044f";
            }
            if (b < 100) {
                return "\u043a\u0430\u0431\u0435\u043b\u044c\u043d\u044b\u0439 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442";
            }
            if (b >= 1000) {
                return "\u0433\u0438\u0433\u0430\u0431\u0438\u0442 \u0438 Wi-Fi";
            }
            return "\u043e\u043f\u0442\u0438\u043a\u0430 100+ \u041c\u0431\u0438\u0442/\u0441";
        } else {
            if (this.slots[17] == null) {
                return "\u043d\u0435\u0442 \u043c\u043e\u0434\u0435\u043c\u0430";
            }
            return "\u043c\u043e\u0434\u0435\u043c \u0438\u0437 \u0431\u0443\u0434\u0443\u0449\u0435\u0433\u043e \u2014 \u043d\u0435 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442";
        }
    }
    
    private int gpuPerf() {
        rpc.Part a = this.slots[6];
        if (a == null) {
            return 0;
        }
        return a.f[5] * rpc.Score.condOf(a) / 100;
    }
    
    private void mine(int i) {
        int i0 = rpc.Os.mineIncome(this.gpuPerf(), this.levelYear(), this.netSpeed());
        if (i0 <= 0) {
            this.toastMsg((this.levelYear() >= 2010) ? "\u041d\u0443\u0436\u043d\u0430 \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430" : "\u041c\u0430\u0439\u043d\u0438\u043d\u0433\u0430 \u0432 \u044d\u0442\u043e\u043c \u0433\u043e\u0434\u0443 \u0435\u0449\u0451 \u043d\u0435\u0442");
            return;
        }
        if (!this.osReady()) {
            this.toastMsg("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0441\u043e\u0431\u0435\u0440\u0438\u0442\u0435 \u0440\u0430\u0431\u043e\u0447\u0443\u044e \u043c\u0430\u0448\u0438\u043d\u0443");
            return;
        }
        int i1 = i0 * i;
        this.cash = this.cash + i1;
        this.minedTotal = this.minedTotal + i1;
        this.advanceDay(i);
        this.toastMsg(new StringBuffer("\u041d\u0430\u043c\u0430\u0439\u043d\u0438\u043b ").append(i1).append(" \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432 \u0437\u0430 ").append(i).append(" \u0434\u043d.").toString());
    }
    
    private void paintMarkets() {
        this.header("\u0420\u044b\u043d\u043a\u0438 \u0438 \u0431\u0430\u0440\u0430\u0445\u043e\u043b\u043a\u0438", new StringBuffer("\u0434\u0435\u043d\u044c ").append(this.day).toString());
        int i = this.FH1 + 22;
        int i0 = this.FH1 + 18;
        int i1 = (this.H - i0 - this.FH0 - 16) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.marketSel < this.marketTop) {
            this.marketTop = this.marketSel;
        }
        if (this.marketSel > this.marketTop + i1 - 1) {
            this.marketTop = this.marketSel - i1 + 1;
        }
        int i2 = 0;
        while(true) {
            if (i2 < i1) {
                int i3 = this.marketTop + i2;
                if (i3 < 7) {
                    int i4 = i0 + i2 * i;
                    boolean b = i3 == this.marketSel;
                    boolean b0 = rpc.Market.available(i3, this.levelYear());
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i4, this.W - 8, i - 2);
                    this.g.setColor(b ? 3793530 : b0 ? 15003895 : 8297134);
                    this.g.drawText(new StringBuffer(String.valueOf((Object)String.valueOf((Object)(b0 ? "" : "\u00d7 ")))).append(rpc.Market.NAME[i3]).toString(), 10, i4 + 4, 1);
                    this.g.setColor(8297134);
                    this.g.drawText(this.fit(b0 ? rpc.Market.note(i3, this.levelYear()) : new StringBuffer("\u0441 ").append(rpc.Market.SINCE[i3]).append(" \u0433\u043e\u0434\u0430").toString(), this.W - 20, 0), 10, i4 + 6 + this.FH1, 0);
                    i2 = i2 + 1;
                    continue;
                }
            }
            this.footer("\u2191\u2193 \u0432\u044b\u0431\u043e\u0440", "OK \u2014 \u0437\u0430\u0439\u0442\u0438");
            return;
        }
    }
    
    private void paintOffers() {
        this.header(rpc.Market.NAME[this.openMarket], new StringBuffer("\u043a\u0440\u0435\u0434\u0438\u0442\u044b: ").append(rpc.Util.money(this.cash)).toString());
        int i = this.FH0 * 2 + 8;
        int i0 = this.FH1 + 16;
        int i1 = (this.H - i0 - this.FH0 - 26) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.offerSel < this.offerTop) {
            this.offerTop = this.offerSel;
        }
        if (this.offerSel > this.offerTop + i1 - 1) {
            this.offerTop = this.offerSel - i1 + 1;
        }
        if (this.offerPart.length == 0) {
            this.g.setColor(8297134);
            this.g.drawText("\u041f\u0443\u0441\u0442\u043e: \u0432 \u044d\u0442\u043e\u043c \u0433\u043e\u0434\u0443 \u0442\u0443\u0442 \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435\u0442", 10, i0 + 4, 0);
        }
        int i2 = 0;
        while(true) {
            if (i2 < i1) {
                int i3 = this.offerTop + i2;
                if (i3 < this.offerPart.length) {
                    boolean b = false;
                    rpc.Part a = this.db.get(this.offerPart[i3]);
                    int i4 = i0 + i2 * i;
                    boolean b0 = i3 == this.offerSel;
                    boolean b1 = this.offerPrice[i3] <= this.cash;
                    this.g.setColor(b0 ? 2308170 : 1780536);
                    this.g.fillRect(4, i4, this.W - 8, i - 2);
                    if (rpc.Assets.ready) {
                        this.g.setColor(791580);
                        this.g.fillRect(6, i4 + 1, 28, 18);
                        rpc.Assets.drawPart(this.g, a, 6, i4 + 1, 28, 18);
                    }
                    int[][] a0 = this.offerBundle;
                    label2: {
                        label0: {
                            label1: {
                                if (a0 == null) {
                                    break label1;
                                }
                                if (i3 >= this.offerBundle.length) {
                                    break label1;
                                }
                                if (this.offerBundle[i3] != null) {
                                    break label0;
                                }
                            }
                            b = false;
                            break label2;
                        }
                        b = true;
                    }
                    String s = b ? new StringBuffer("\u041f\u041a \u0432 \u0441\u0431\u043e\u0440\u0435 ").append(a.year).append(" (").append(this.offerBundle[i3].length).append(" \u0434\u0435\u0442.)").toString() : a.name;
                    this.g.setColor(b0 ? 3793530 : b1 ? 15003895 : 8297134);
                    String s0 = (this.offerPrice[i3] > 0) ? new StringBuffer().append(this.offerPrice[i3]).toString() : "\u0434\u0430\u0440\u043e\u043c";
                    this.g.drawText(this.fit(s, this.W - 100 - this.g.textWidth(s0, 0), 0), 38, i4 + 4, 0);
                    this.g.setColor(((this.offerCond[i3]) >= 50) ? 16763196 : 16739179);
                    String s1 = new StringBuffer(String.valueOf((Object)String.valueOf(a.year))).append(" \u2022 ").append(b ? "\u043a\u0430\u043a \u0435\u0441\u0442\u044c" : new StringBuffer(String.valueOf((Object)String.valueOf(this.offerCond[i3]))).append("%").toString()).toString();
                    if (!b && this.offerDust[i3] > 0) {
                        s1 = new StringBuffer(String.valueOf((Object)String.valueOf((Object)s1))).append(" \u2022 \u043f\u044b\u043b\u044c ").append(this.offerDust[i3]).append("%").toString();
                    }
                    this.g.drawText(s1, 38, i4 + 5 + this.FH0, 0);
                    this.g.setColor(b1 ? 16763196 : 16739179);
                    this.g.drawText(s0, this.W - this.g.textWidth(s0, 0) - 8, i4 + 4, 0);
                    i2 = i2 + 1;
                    continue;
                }
            }
            String s2 = (this.openMarket != 3) ? new StringBuffer("\u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0430 ").append(rpc.Market.days(this.openMarket, this.netSpeed())).append(" \u0434\u043d.").toString() : "3: \u0438\u0441\u043a\u0430\u0442\u044c \u0435\u0449\u0451 (1 \u0434\u0435\u043d\u044c)";
            this.footer(new StringBuffer("(").append(this.offerSel + 1).append("/").append(this.offerPart.length).append(") ").append(s2).toString(), "OK \u2014 \u0432\u0437\u044f\u0442\u044c");
            return;
        }
    }
    
    private void paintOrders() {
        this.header("\u0414\u043e\u0441\u0442\u0430\u0432\u043a\u0430", new StringBuffer("\u0434\u0435\u043d\u044c ").append(this.day).toString());
        int i = this.FH0 * 2 + 7;
        int i0 = this.FH1 + 16;
        int i1 = this.table.orders.length;
        if (i1 == 0) {
            this.g.setColor(8297134);
            this.g.drawText("\u0417\u0430\u043a\u0430\u0437\u043e\u0432 \u043d\u0435\u0442. \u0417\u0430\u0433\u043b\u044f\u043d\u0438\u0442\u0435 \u043d\u0430 \u0440\u044b\u043d\u043e\u043a.", 10, i0 + 4, 0);
        }
        int i2 = (this.H - i0 - this.FH0 - 30) / i;
        if (i2 < 1) {
            i2 = 1;
        }
        if (this.orderSel < this.orderTop) {
            this.orderTop = this.orderSel;
        }
        if (this.orderSel > this.orderTop + i2 - 1) {
            this.orderTop = this.orderSel - i2 + 1;
        }
        int i3 = 0;
        while(true) {
            if (i3 < i2) {
                int i4 = this.orderTop + i3;
                if (i4 < i1) {
                    rpc.Table$Order a = this.table.orders[i4];
                    int i5 = i0 + i3 * i;
                    boolean b = i4 == this.orderSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i5, this.W - 8, i - 2);
                    if (rpc.Assets.ready) {
                        this.g.setColor(791580);
                        this.g.fillRect(6, i5 + 1, 24, 16);
                        rpc.Assets.drawPart(this.g, a.part, 6, i5 + 1, 24, 16);
                    }
                    this.g.setColor(b ? 3793530 : 15003895);
                    this.g.drawText(this.fit(a.part.name, this.W - 110, 0), 34, i5 + 3, 0);
                    this.g.setColor(8297134);
                    this.g.drawText(new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Market.NAME[a.market]))).append(" \u2022 ").append(a.cond).append("%").toString(), 34, i5 + 4 + this.FH0, 0);
                    String s = new StringBuffer("\u0434\u0435\u043d\u044c ").append(a.arriveDay).toString();
                    this.g.setColor(16763196);
                    this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i5 + 4, 0);
                    i3 = i3 + 1;
                    continue;
                }
            }
            this.footer("OK: +1 \u0441\u0443\u0442\u043a\u0438", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintBench() {
        rpc.Part a = null;
        int i = 0;
        this.header("\u0421\u0442\u043e\u043b \u0441\u0431\u043e\u0440\u043a\u0438", new StringBuffer("\u0434\u0435\u0442\u0430\u043b\u0435\u0439 ").append(this.table.n).append("/").append(12).toString());
        int i0 = this.FH1 + 12;
        int i1 = this.W / 3;
        int i2 = this.FH0 * 3 + 12;
        int i3 = this.H - i0 - 74 - i2 - this.FH0 - 16;
        if (i3 > 92) {
            i3 = 92;
        }
        if (i3 < 34) {
            i3 = 34;
        }
        rpc.Scene.bench(this.g, 0, i0, this.W, 74);
        if (this.table.n == 0) {
            this.g.setColor(15003895);
            this.g.drawText("\u0421\u0442\u043e\u043b \u043f\u0443\u0441\u0442.", 8, i0 + 10, 0);
            this.g.setColor(8297134);
            this.g.drawText("\u0417\u0430\u043a\u0430\u0436\u0438\u0442\u0435 \u0434\u0435\u0442\u0430\u043b\u0438 \u0432 \u0440\u044b\u043d\u043a\u0435", 8, i0 + 12 + this.FH0, 0);
            this.g.drawText("\u0438\u043b\u0438 \u043f\u043e\u0438\u0449\u0438\u0442\u0435 \u043d\u0430 \u0441\u0432\u0430\u043b\u043a\u0435.", 8, i0 + 24 + this.FH0, 0);
            this.footer("1: \u043d\u0430\u0437\u0430\u0434", (String)null);
            return;
        }
        int i4 = this.benchSel / 3;
        if (i4 < this.benchTop) {
            this.benchTop = i4;
        }
        if (i4 > this.benchTop + 2 - 1) {
            this.benchTop = i4 - 2 + 1;
        }
        int i5 = 0;
        while(i5 < 2) {
            int i6 = 0;
            while(true) {
                if (i6 < 3) {
                    int i7 = (this.benchTop + i5) * 3 + i6;
                    if (i7 < this.table.n) {
                        rpc.Part a0 = this.table.parts[i7];
                        int i8 = 2 + i6 * i1;
                        int i9 = i0 + 3 + i5 * 34;
                        int i10 = i1 - 4;
                        boolean b = i7 == this.benchSel;
                        rpc.Scene.slotFrame(this.g, i8, i9, i10, 29, b, 3793530);
                        int i11 = i10 - 6;
                        int i12 = 29 - 6;
                        if (rpc.Assets.ready) {
                            rpc.Assets.drawPart(this.g, a0, i8 + 3, i9 + 3, i11, i12);
                        }
                        this.g.setColor(((this.table.cond[i7]) >= 50) ? ((this.table.cond[i7]) >= 85) ? 7260250 : 16763196 : 16739179);
                        this.g.fillRect(i8 + i10 - 4, i9 + 2, 2, 2);
                        i6 = i6 + 1;
                        continue;
                    }
                }
                i5 = i5 + 1;
                break;
            }
        }
        int[] a1 = new int[20];
        int i13 = 0;
        while(i13 < 20) {
            a1[i13] = (this.slots[i13] != null) ? 1 : 0;
            i13 = i13 + 1;
        }
        int i14 = (this.benchSel < 0) ? -1 : (this.benchSel >= this.table.n) ? -1 : this.slotForCat(this.table.parts[this.benchSel].cat);
        int i15 = i0 + 74 + 2;
        rpc.Scene.caseLayout(this.g, 2, i15, this.W - 4, i3, a1, i14, 3793530);
        int i16 = Math.max(i15 + i3 + 2, this.H - i2 - this.FH0 - 10);
        int i17 = this.benchSel;
        label2: {
            label0: {
                label1: {
                    if (i17 < 0) {
                        break label1;
                    }
                    if (this.benchSel < this.table.n) {
                        break label0;
                    }
                }
                a = null;
                break label2;
            }
            a = this.table.parts[this.benchSel];
        }
        this.g.setColor(1318956);
        this.g.fillRect(0, i16, this.W, this.H - i16 - this.FH0 - 8);
        if (a == null) {
            this.footer("1: \u043d\u0430\u0437\u0430\u0434", (String)null);
            return;
        }
        int b0 = this.table.cond[this.benchSel];
        this.g.setColor(15003895);
        this.g.drawText(this.fit(a.name, this.W - 12, 0), 6, i16 + 2, 0);
        this.g.setColor((b0 >= 50) ? 8297134 : 16739179);
        String s = new StringBuffer(String.valueOf((Object)String.valueOf((Object)a.catName()))).append(" \u2022 ").append(a.year).append(" \u2022 ").append(b0).append("%").toString();
        if (this.table.dust[this.benchSel] > 0) {
            s = new StringBuffer(String.valueOf((Object)String.valueOf((Object)s))).append(" \u2022 \u043f\u044b\u043b\u044c ").append(this.table.dust[this.benchSel]).append("%").toString();
        }
        this.g.drawText(this.fit(s, this.W - 12, 0), 6, i16 + 3 + this.FH0, 0);
        int i18 = this.slotForCat(a.cat);
        String s0 = rpc.Compat.why(i18, a, this.slots);
        if (s0 == null) {
            s0 = new StringBuffer("\u2192 ").append(rpc.Score.SLOT_NAMES[i18]).toString();
            if (b0 < 100) {
                s0 = new StringBuffer(String.valueOf((Object)String.valueOf((Object)s0))).append(" \u2022 \u0440\u0435\u043c\u043e\u043d\u0442 ").append(this.repairCost(a, b0)).toString();
            }
            i = 16763196;
        } else {
            i = 16739179;
        }
        this.g.setColor(i);
        this.g.drawText(this.fit(s0, this.W - 12, 0), 6, i16 + 4 + this.FH0 * 2, 0);
        this.footer("OK: \u0441\u0442\u0430\u0432\u0438\u0442\u044c  3: \u0440\u0435\u043c\u043e\u043d\u0442", "\u2190 \u043f\u0440\u043e\u0434\u0430\u0442\u044c");
    }
    
    private void paintOs() {
        String s = (this.osIndex < 0) ? rpc.Os.name(this.levelYear()) : rpc.Inst.all()[this.osIndex].name;
        this.header(new StringBuffer("\u041e\u0421: ").append(s).toString(), (this.osReady()) ? (this.osIndex < 0) ? "\u0441\u0432\u043e\u044f \u041e\u0421" : "\u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442" : "\u043d\u0435 \u0441\u043e\u0431\u0440\u0430\u043d\u0430");
        int i = this.FH1 + 18;
        if (this.osReady()) {
            String[] a = new String[11];
            a[0] = new StringBuffer(String.valueOf((Object)rpc.Langs.t(this.osLang, 1))).append(" (").append(this.progs.length).append(")").toString();
            a[1] = new StringBuffer(String.valueOf((Object)rpc.Langs.t(this.osLang, 2))).append(" (").append(rpc.Vm.LANG_NAME[this.editorLang]).append(")").toString();
            a[2] = new StringBuffer(String.valueOf((Object)rpc.Langs.t(this.osLang, 4))).append(" (").append(this.openTasks()).append(")").toString();
            a[3] = rpc.Langs.t(this.osLang, 3);
            a[4] = "\u0418\u043d\u0442\u0435\u0440\u043d\u0435\u0442: \u0431\u0440\u0430\u0443\u0437\u0435\u0440";
            a[5] = "\u041e\u0431\u043e\u043b\u043e\u0447\u043a\u0430: DOS / UNIX";
            a[6] = rpc.Langs.t(this.osLang, 15);
            a[7] = rpc.Langs.t(this.osLang, 7);
            a[8] = "\u0418\u0433\u0440\u044b \u0438 \u0442\u0435\u0441\u0442\u044b 1980-2026";
            a[9] = "DOOM";
            a[10] = new StringBuffer("\u25c0 ").append(rpc.Langs.t(this.osLang, 16)).toString();
            int i0 = this.FH1 + 10;
            int i1 = (this.H - this.FH0 * 3 - 40 - i) / i0;
            if (i1 < 3) {
                i1 = 3;
            }
            if (this.osSel < this.osTop) {
                this.osTop = this.osSel;
            }
            if (this.osSel > this.osTop + i1 - 1) {
                this.osTop = this.osSel - i1 + 1;
            }
            int i2 = this.osTop;
            while(true) {
                if (i2 < a.length && i2 < this.osTop + i1) {
                    int i3 = i + (i2 - this.osTop) * i0;
                    boolean b = i2 == this.osSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRoundRect(8, i3, this.W - 16, this.FH1 + 8, 6);
                    if (b) {
                        this.g.setColor(3793530);
                        this.g.drawRoundRect(8, i3, this.W - 16, this.FH1 + 8, 6);
                    }
                    this.g.setColor(b ? 3793530 : 15003895);
                    this.g.drawText(a[i2], 16, i3 + 5, 1);
                    i2 = i2 + 1;
                    continue;
                }
                this.g.setColor(8297134);
                this.g.drawText(this.fit(new StringBuffer("\u0441\u0438\u0441\u0442\u0435\u043c\u0430: ").append((this.osIndex < 0) ? rpc.Os.name(this.levelYear()) : rpc.Inst.all()[this.osIndex].name).toString(), this.W - 16, 0), 10, this.H - this.FH0 * 3 - 34, 0);
                this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Langs.t(this.osLang, 12)))).append(": ").append(rpc.Langs.NAME[this.osLang]).append(" \u2022 ").append(rpc.Langs.CODE[this.osLang]).toString(), this.W - 16, 0), 10, this.H - this.FH0 * 2 - 30, 0);
                this.g.drawText(this.fit(new StringBuffer("\u0441\u0435\u0442\u044c: ").append(this.netKind()).toString(), this.W - 16, 0), 10, this.H - this.FH0 * 2 - 18, 0);
                this.footer("\u2191\u2193 \u0432\u044b\u0431\u043e\u0440", "OK");
                return;
            }
        } else {
            String[] a0 = new String[6];
            a0[0] = "\u0421\u0438\u0441\u0442\u0435\u043c\u0430 \u043d\u0435 \u0437\u0430\u0433\u0440\u0443\u0437\u0438\u0442\u0441\u044f:";
            a0[1] = "\u043d\u0443\u0436\u043d\u044b \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440, \u043f\u043b\u0430\u0442\u0430, \u041e\u0417\u0423";
            a0[2] = "\u0438 \u043b\u044e\u0431\u043e\u0439 \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044c.";
            a0[3] = "";
            a0[4] = "\u0421\u043e\u0431\u0435\u0440\u0438\u0442\u0435 \u0431\u0430\u0437\u0443 \u2014 \u0438 \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0439\u0442\u0435";
            a0[5] = "\u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b, \u0437\u0430\u043a\u0430\u0437\u044b \u0438 \u043c\u0430\u0439\u043d\u0438\u043d\u0433.";
            this.g.setColor(8297134);
            int i4 = 0;
            while(i4 < a0.length) {
                this.g.drawText(a0[i4], 10, i + i4 * (this.FH0 + 4), 0);
                i4 = i4 + 1;
            }
            this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u043d\u0430\u0437\u0430\u0434", "");
            return;
        }
    }
    
    private int openTasks() {
        int i = 0;
        int i0 = 0;
        while(i0 < this.tasks.length) {
            if (!this.tasks[i0].done) {
                i = i + 1;
            }
            i0 = i0 + 1;
        }
        return i;
    }
    
    private String langList() {
        StringBuffer a = new StringBuffer();
        int i = 0;
        while(i < 3) {
            if (i > 0) {
                a.append("  ");
            }
            a.append((rpc.Os.langAvailable(i, this.levelYear())) ? rpc.Vm.LANG_NAME[i] : new StringBuffer("(").append(rpc.Vm.LANG_NAME[i]).append(")").toString());
            i = i + 1;
        }
        return a.toString();
    }
    
    private void paintOsProgs() {
        this.header("\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b", new StringBuffer("\u0432\u0441\u0435\u0433\u043e ").append(this.progs.length).toString());
        int i = this.FH0 + 10;
        int i0 = this.FH1 + 16;
        int i1 = (this.H - i0 - this.FH0 - 20) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.progSel < this.progTop) {
            this.progTop = this.progSel;
        }
        if (this.progSel > this.progTop + i1 - 1) {
            this.progTop = this.progSel - i1 + 1;
        }
        int i2 = this.progs.length + 1;
        int i3 = 0;
        while(true) {
            if (i3 < i1) {
                int i4 = this.progTop + i3;
                if (i4 < i2) {
                    int i5 = i0 + i3 * i;
                    boolean b = i4 == this.progSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i5, this.W - 8, i - 2);
                    if (i4 < this.progs.length) {
                        rpc.Os$Prog a = this.progs[i4];
                        this.g.setColor(b ? 3793530 : 15003895);
                        this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)String.valueOf((Object)a.name))).append(" \u2022 ").append(rpc.Vm.LANG_NAME[a.lang]).append(" \u2022 ").append(a.lines.length).append(" \u0441\u0442\u0440.").toString(), this.W - 16, 0), 10, i5 + 4, 0);
                    } else {
                        this.g.setColor(b ? 3793530 : 16763196);
                        this.g.drawText("+ \u043d\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 (\u0440\u0435\u0434\u0430\u043a\u0442\u043e\u0440)", 10, i5 + 4, 0);
                    }
                    i3 = i3 + 1;
                    continue;
                }
            }
            this.footer("OK \u2014 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c", "3: \u0440\u0435\u0434\u0430\u043a\u0442\u043e\u0440");
            return;
        }
    }
    
    private void paintEditor() {
        this.header(new StringBuffer("\u0420\u0435\u0434\u0430\u043a\u0442\u043e\u0440 ").append(rpc.Vm.LANG_NAME[this.editorLang]).toString(), new StringBuffer("\u0441\u0442\u0440\u043e\u043a ").append(this.editorN).append("/").append(40).toString());
        int i = this.FH1 + 10;
        int i0 = this.FH1 + 14 + i + 4;
        int i1 = (this.H - i0 - this.FH0 - this.FH0 - 30) / i;
        if (i1 < 4) {
            i1 = 4;
        }
        if (this.editorSel < this.editorTop) {
            this.editorTop = this.editorSel;
        }
        if (this.editorSel > this.editorTop + i1 - 1) {
            this.editorTop = this.editorSel - i1 + 1;
        }
        this.g.setColor(528908);
        this.g.fillRect(4, i0 - 4, this.W - 8, i1 * i + 8);
        this.g.setColor(3033694);
        this.g.drawRect(4, i0 - 4, this.W - 8, i1 * i + 8);
        int i2 = 0;
        while(true) {
            label4: if (i2 < i1) {
                String s = null;
                int i3 = this.editorTop + i2;
                int i4 = i0 + i2 * i;
                int i5 = this.editorN;
                label3: {
                    if (i3 < i5) {
                        break label3;
                    }
                    if (i3 <= 0) {
                        break label3;
                    }
                    break label4;
                }
                boolean b = i3 == this.editorSel;
                if (b) {
                    this.g.setColor(1323038);
                    this.g.fillRect(6, i4 - 2, this.W - 12, i - 2);
                }
                String s0 = (i3 + 1 >= 10) ? new StringBuffer().append(i3 + 1).toString() : new StringBuffer(" ").append(i3 + 1).toString();
                this.g.setColor(8297134);
                this.g.drawText(s0, 9, i4 + 3, 0);
                int i6 = this.editorN;
                label2: {
                    label0: {
                        label1: {
                            if (i3 >= i6) {
                                break label1;
                            }
                            if (this.editorLines[i3] != null) {
                                break label0;
                            }
                        }
                        s = "";
                        break label2;
                    }
                    s = this.editorLines[i3];
                }
                if (b) {
                    this.g.setColor(7139466);
                    this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)String.valueOf((Object)s))).append("_").toString(), this.W - 40, 0), 26, i4 + 3, 0);
                } else {
                    this.g.setColor(3459170);
                    this.g.drawText(this.fit(s, this.W - 40, 0), 26, i4 + 3, 0);
                }
                i2 = i2 + 1;
                continue;
            }
            int i7 = i0 - 4 - i - 2;
            boolean b0 = this.editorSel < 0;
            this.g.setColor(b0 ? 1980970 : 793106);
            this.g.fillRect(6, i7, this.W - 12, i - 2);
            this.g.setColor(b0 ? 3793530 : 8297134);
            this.g.drawText("\u044f\u0437\u044b\u043a:", 9, i7 + 3, 0);
            this.g.drawText(new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Vm.LANG_NAME[this.editorLang]))).append(b0 ? "  \u25b2\u25bc" : "").toString(), 44, i7 + 3, 0);
            int i8 = this.H - this.FH0 * 2 - 26;
            this.g.setColor(3033694);
            this.g.drawLine(0, i8 - 6, this.W, i8 - 6);
            this.g.setColor(8297134);
            this.g.drawText(this.fit(this.editorHint(), this.W - 12, 0), 6, i8, 0);
            this.g.drawText(this.fit(rpc.Vm.LANG_NOTE[this.editorLang], this.W - 12, 0), 6, i8 + this.FH0 + 2, 0);
            this.footer("OK \u043f\u0435\u0447\u0430\u0442\u0430\u0442\u044c  \u2190 \u0441\u0442\u0435\u0440\u0435\u0442\u044c", "3: \u0437\u0430\u043f\u0443\u0441\u043a");
            return;
        }
    }
    
    private String editorHint() {
        if (this.editorLang == 0) {
            return "A=1  PRINT A  FOR I=1 TO 5  NEXT  IF A>3";
        }
        if (this.editorLang != 1) {
            return "int a=0;  a=a+2;  print(a);  for(i=0;i<5;i++){  }";
        }
        return "MOV A,5  ADD A,2  OUT A  REP 3  ENDREP";
    }
    
    private String codePreview() {
        if (this.editorN == 0) {
            return "\u043a\u043e\u0434: \u043f\u0443\u0441\u0442\u043e";
        }
        int i = (this.editorN <= 2) ? 0 : this.editorN - 2;
        StringBuffer a = new StringBuffer("\u043a\u043e\u0434: ");
        int i0 = i;
        while(i0 < this.editorN) {
            if (i0 > i) {
                a.append(" | ");
            }
            a.append(this.editorLines[i0]);
            i0 = i0 + 1;
        }
        return a.toString();
    }
    
    private void paintRun() {
        String s = null;
        String s0 = this.runTitle;
        rpc.Vm$Run a = this.lastRun;
        label2: {
            label0: {
                label1: {
                    if (a == null) {
                        break label1;
                    }
                    if (this.lastRun.ok) {
                        break label0;
                    }
                }
                s = "\u043e\u0448\u0438\u0431\u043a\u0430";
                break label2;
            }
            s = "\u0433\u043e\u0442\u043e\u0432\u043e";
        }
        this.header(s0, s);
        int i = this.FH1 + 18;
        this.g.setColor(3793530);
        this.g.drawText("> \u0432\u044b\u0432\u043e\u0434:", 8, i, 0);
        int i0 = i + (this.FH0 + 6);
        if (this.lastRun != null) {
            if (this.lastRun.error == null) {
                int i1 = 0;
                while(i1 < this.lastRun.lines.length && i1 < 12) {
                    this.g.setColor(15003895);
                    this.g.drawText(this.fit(this.lastRun.lines[i1], this.W - 16, 0), 8, i0, 0);
                    i0 = i0 + (this.FH0 + 3);
                    i1 = i1 + 1;
                }
                if (this.lastRun.lines.length == 0) {
                    this.g.setColor(8297134);
                    this.g.drawText("(\u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u0432\u044b\u0432\u0435\u043b\u0430)", 8, i0, 0);
                }
            } else {
                this.g.setColor(16739179);
                this.g.drawText(this.fit(this.lastRun.error, this.W - 16, 0), 8, i0, 0);
            }
        } else {
            this.g.setColor(8297134);
            this.g.drawText("\u043d\u0435\u0442 \u0437\u0430\u043f\u0443\u0441\u043a\u0430", 8, i0, 0);
        }
        this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u043d\u0430\u0437\u0430\u0434", "");
    }
    
    private void paintTasks() {
        this.header("\u0417\u0430\u043a\u0430\u0437\u044b \u043d\u0430 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b", new StringBuffer("\u043a\u0440\u0435\u0434\u0438\u0442\u044b: ").append(rpc.Util.money(this.cash)).toString());
        int i = this.FH0 * 2 + 8;
        int i0 = this.FH1 + 16;
        int i1 = (this.H - i0 - this.FH0 - 26) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.taskSel < this.taskTop) {
            this.taskTop = this.taskSel;
        }
        if (this.taskSel > this.taskTop + i1 - 1) {
            this.taskTop = this.taskSel - i1 + 1;
        }
        int i2 = 0;
        while(true) {
            if (i2 < i1) {
                int i3 = this.taskTop + i2;
                if (i3 < this.tasks.length) {
                    rpc.Os$Task a = this.tasks[i3];
                    int i4 = i0 + i2 * i;
                    boolean b = i3 == this.taskSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i4, this.W - 8, i - 2);
                    this.g.setColor((a.done) ? 8297134 : b ? 3793530 : 15003895);
                    this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)String.valueOf((Object)((a.done) ? "\u221a " : "")))).append(a.title).toString(), this.W - 90, 0), 10, i4 + 3, 0);
                    this.g.setColor(8297134);
                    this.g.drawText(new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Vm.LANG_NAME[a.lang]))).append(" \u2022 \u0432\u044b\u0432\u043e\u0434 ").append(a.expect).toString(), 10, i4 + 4 + this.FH0, 0);
                    String s = new StringBuffer("+").append(a.reward).toString();
                    this.g.setColor((a.done) ? 8297134 : 16763196);
                    this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i4 + 4, 0);
                    i2 = i2 + 1;
                    continue;
                }
            }
            this.footer("OK \u2014 \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043a\u043e\u0434", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintNet() {
        String[] a = null;
        this.header("\u0421\u0435\u0442\u044c \u0438 \u043c\u043e\u0434\u0435\u043c", new StringBuffer("\u0434\u0435\u043d\u044c ").append(this.day).toString());
        int i = this.FH1 + 18;
        rpc.Part a0 = this.slots[17];
        if (a0 != null) {
            a = new String[10];
            a[0] = new StringBuffer("\u0423\u0441\u0442\u0440\u043e\u0439\u0441\u0442\u0432\u043e: ").append(a0.name).toString();
            a[1] = new StringBuffer("\u0413\u043e\u0434: ").append(a0.year).append("   \u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c: ").append(a0.f[1]).append(" \u041c\u0431\u0438\u0442/\u0441").toString();
            a[2] = new StringBuffer("\u0422\u0438\u043f \u043b\u0438\u043d\u0438\u0438: ").append(this.netKind()).toString();
            a[3] = ((a0.f[2]) <= 0) ? "Wi-Fi: \u043d\u0435\u0442" : "Wi-Fi: \u0435\u0441\u0442\u044c";
            a[4] = new StringBuffer("\u0423\u0441\u043b\u043e\u0432\u0438\u0435 \u0433\u043e\u0434\u0430: ").append((a0.year <= this.levelYear()) ? "\u043e\u043a" : "\u0438\u0437 \u0431\u0443\u0434\u0443\u0449\u0435\u0433\u043e!").toString();
            a[5] = "";
            a[6] = new StringBuffer("\u0421\u0435\u0442\u044c \u0432 ").append(this.levelYear()).append(" \u0433\u043e\u0434\u0443: ").append(this.eraNet()).toString();
            a[7] = "";
            a[8] = "\u0412\u043b\u0438\u044f\u043d\u0438\u0435: \u0437\u0430\u043a\u0430\u0437\u044b \u043d\u0430 AliExpress,";
            a[9] = "\u0434\u043e\u0445\u043e\u0434 \u043c\u0430\u0439\u043d\u0438\u043d\u0433\u0430, \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0434\u043e\u0441\u0442\u0430\u0432\u043a\u0438.";
        } else {
            a = new String[5];
            a[0] = "\u041c\u043e\u0434\u0435\u043c \u043d\u0435 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d.";
            a[1] = "";
            a[2] = new StringBuffer("\u0421\u0435\u0442\u044c \u0432 ").append(this.levelYear()).append(" \u0433\u043e\u0434\u0443: ").append(this.eraNet()).toString();
            a[3] = "\u041a\u0443\u043f\u0438\u0442\u0435 \u0441\u0435\u0442\u0435\u0432\u0443\u044e \u043a\u0430\u0440\u0442\u0443/\u043c\u043e\u0434\u0435\u043c";
            a[4] = "\u043d\u0430 \u0440\u044b\u043d\u043a\u0435 \u0432 \u0441\u043b\u043e\u0442\u0435 \u00ab\u0421\u0435\u0442\u044c\u00bb.";
        }
        int i0 = 0;
        while(i0 < a.length) {
            this.g.setColor((i0 != 0) ? 15003895 : 3793530);
            this.g.drawText(this.fit(a[i0], this.W - 16, 0), 10, i, 0);
            i = i + (this.FH0 + 4);
            i0 = i0 + 1;
        }
        this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u043d\u0430\u0437\u0430\u0434", "");
    }
    
    private String eraNet() {
        int i = this.levelYear();
        if (i < 1988) {
            return "BBS \u0438 \u043f\u0440\u044f\u043c\u044b\u0435 \u0437\u0432\u043e\u043d\u043a\u0438, 300-2400 \u0431\u043e\u0434";
        }
        if (i < 1995) {
            return "\u043c\u043e\u0434\u0435\u043c 14.4-28.8k, BBS \u0438 Fidonet";
        }
        if (i < 2000) {
            return "56k dial-up, \u043f\u0435\u0440\u0432\u044b\u0435 \u0441\u0430\u0439\u0442\u044b";
        }
        if (i < 2005) {
            return "ADSL \u0438 \u043b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0435 \u0441\u0435\u0442\u0438";
        }
        if (i < 2010) {
            return "\u043a\u0430\u0431\u0435\u043b\u044c\u043d\u044b\u0439 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442, Wi-Fi N";
        }
        if (i < 2015) {
            return "\u043e\u043f\u0442\u0438\u043a\u0430, Wi-Fi AC, \u043e\u0431\u043b\u0430\u043a\u0430";
        }
        if (i >= 2020) {
            return "2.5G/10G, Wi-Fi 6E/7, 5G";
        }
        return "\u0433\u0438\u0433\u0430\u0431\u0438\u0442, 4G \u0432 \u043a\u0430\u0436\u0434\u043e\u043c \u0442\u0435\u043b\u0435\u0444\u043e\u043d\u0435";
    }
    
    private void paintMine() {
        String[] a = null;
        this.header("\u041c\u0430\u0439\u043d\u0438\u043d\u0433", new StringBuffer("\u0434\u0435\u043d\u044c ").append(this.day).toString());
        int i = this.FH1 + 18;
        int i0 = rpc.Os.mineIncome(this.gpuPerf(), this.levelYear(), this.netSpeed());
        if (this.levelYear() >= 2010) {
            if (i0 > 0) {
                a = new String[6];
                a[0] = new StringBuffer("\u041c\u043e\u0449\u043d\u043e\u0441\u0442\u044c \u043a\u0430\u0440\u0442\u044b: ").append(this.gpuPerf()).append(" \u0443\u0441\u043b. \u0435\u0434.").toString();
                a[1] = new StringBuffer("\u041a\u0443\u0440\u0441 \u0441\u0435\u0442\u0438: ").append(this.netKind()).toString();
                a[2] = new StringBuffer("\u0414\u043e\u0445\u043e\u0434: ").append(i0).append(" \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432 \u0432 \u0441\u0443\u0442\u043a\u0438").toString();
                a[3] = new StringBuffer("\u041d\u0430\u043c\u0430\u0439\u043d\u0435\u043d\u043e \u0432\u0441\u0435\u0433\u043e: ").append(this.minedTotal).toString();
                a[4] = "";
                a[5] = "OK \u2014 1 \u0441\u0443\u0442\u043a\u0438, 3 \u2014 \u043d\u0435\u0434\u0435\u043b\u044f.";
            } else {
                a = new String[2];
                a[0] = "\u041d\u0443\u0436\u043d\u0430 \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430 \u0432 \u0441\u0431\u043e\u0440\u043a\u0435";
                a[1] = "\u0438 \u0440\u0430\u0431\u043e\u0447\u0430\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u0430.";
            }
        } else {
            a = new String[2];
            a[0] = new StringBuffer("\u0412 ").append(this.levelYear()).append(" \u0433\u043e\u0434\u0443 \u043c\u0430\u0439\u043d\u0438\u043d\u0433\u0430 \u0435\u0449\u0451 \u043d\u0435\u0442.").toString();
            a[1] = "\u041e\u043d \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 2010-\u043c.";
        }
        int i1 = 0;
        while(i1 < a.length) {
            this.g.setColor((i1 != 0) ? 15003895 : 3793530);
            this.g.drawText(this.fit(a[i1], this.W - 16, 0), 10, i, 0);
            i = i + (this.FH0 + 5);
            i1 = i1 + 1;
        }
        this.footer("OK: \u0441\u0443\u0442\u043a\u0438", "3: \u043d\u0435\u0434\u0435\u043b\u044f");
    }
    
    private int powerNeed() {
        int i = (this.slots[0] == null) ? 50 : 50 + this.slots[0].f[3];
        if (this.slots[6] != null) {
            i = i + this.slots[6].f[4];
        }
        return i;
    }
    
    private void runPost(boolean b) {
        this.faults = rpc.Diag.check(this.slots, this.powerNeed());
        this.lastFault = (this.faults.length <= 0) ? 0 : this.faults[0].code;
        if (b) {
            boolean b0 = false;
            rpc.Sound.fan((this.slots[12] != null) ? 900 + this.slots[12].f[1] * 4 : 900, rpc.Diag.dustOf(this.slots));
            rpc.Part a = this.slots[7];
            label2: {
                label0: {
                    label1: {
                        if (a == null) {
                            break label1;
                        }
                        if (a.cat == 7) {
                            break label0;
                        }
                    }
                    b0 = false;
                    break label2;
                }
                b0 = true;
            }
            rpc.Sound.disk(b0, this.faults.length == 0);
            if (this.speakerOn) {
                rpc.Sound.postBeep(this.lastFault);
            } else {
                this.toastMsg("\u0421\u043f\u0438\u043a\u0435\u0440 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d \u2014 \u043e\u0448\u0438\u0431\u043a\u0443 \u0432\u0438\u0434\u043d\u043e \u0442\u043e\u043b\u044c\u043a\u043e \u043d\u0430 \u044d\u043a\u0440\u0430\u043d\u0435");
            }
        }
    }
    
    private void openSetup() {
        this.runPost(true);
        this.setupSel = 0;
        this.mode = 20;
    }
    
    private void setupFire() {
        if (this.setupSel == 0) {
            this.runPost(true);
            this.toastMsg(new StringBuffer("POST: ").append(rpc.Diag.beepText(this.lastFault)).toString());
            return;
        }
        if (this.setupSel == 1) {
            this.bootSel = (this.bootSel + 1) % BOOT.length;
            rpc.Sound.click();
            this.toastMsg(new StringBuffer("\u041f\u0435\u0440\u0432\u0430\u044f \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0430: ").append(BOOT[this.bootSel]).toString());
            return;
        }
        if (this.setupSel == 2) {
            this.speakerOn = !this.speakerOn;
            rpc.Sound.click();
            this.toastMsg((this.speakerOn) ? "\u0421\u043f\u0438\u043a\u0435\u0440 \u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "\u0421\u043f\u0438\u043a\u0435\u0440 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d \u2014 \u043f\u0438\u0441\u043a\u043e\u0432 \u043d\u0435 \u0431\u0443\u0434\u0435\u0442");
            return;
        }
        if (this.setupSel == 3) {
            rpc.Sound.click();
            this.toastMsg(new StringBuffer("\u0427\u0430\u0441\u044b BIOS: 12.05.").append((this.slots[1] != null) ? this.slots[1].year : this.levelYear()).toString());
            return;
        }
        if (this.setupSel != 4) {
            if (this.setupSel != 5) {
                if (this.setupSel != 6) {
                    if (this.setupSel != 7) {
                        if (this.setupSel != 8) {
                            if (this.setupSel != 9) {
                                if (this.setupSel == 10) {
                                    this.tpmOn = false;
                                    this.secureOn = false;
                                    this.uefiOn = false;
                                    this.xmpOn = false;
                                    this.speakerOn = true;
                                    this.bootSel = 0;
                                    rpc.Sound.click();
                                    this.toastMsg("\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u044b (Clear CMOS)");
                                    return;
                                }
                                if (this.setupSel == 11) {
                                    this.mode = 2;
                                    return;
                                }
                                if (this.setupSel == 12) {
                                    this.stressTest();
                                    return;
                                }
                                int i = this.setupSel - 13;
                                if (i >= 0 && i < this.faults.length) {
                                    this.fixFault(this.faults[i]);
                                }
                                return;
                            } else {
                                int i0 = 0;
                                int i1 = 0;
                                while(i1 < 20) {
                                    if (this.slots[i1] != null) {
                                        i0 = i0 + rpc.Diag.blow(this.slots[i1]);
                                    }
                                    i1 = i1 + 1;
                                }
                                rpc.Sound.revived();
                                this.runPost(false);
                                this.toastMsg((i0 <= 0) ? "\u041f\u044b\u043b\u0438 \u043f\u043e\u0447\u0442\u0438 \u043d\u0435\u0442" : new StringBuffer("\u041f\u0440\u043e\u0434\u0443\u043b: \u0441\u043d\u044f\u0442\u043e \u043f\u044b\u043b\u0438 ").append(i0).append("%").toString());
                                return;
                            }
                        } else {
                            int i2 = 0;
                            int i3 = 0;
                            while(i3 < 20) {
                                rpc.Part a = this.slots[i3];
                                if (a != null && rpc.Diag.capsLeft(a) > 0) {
                                    i2 = i2 + rpc.Diag.recap(a);
                                }
                                i3 = i3 + 1;
                            }
                            rpc.Sound.click();
                            this.runPost(false);
                            this.toastMsg((i2 <= 0) ? "\u0412\u0437\u0434\u0443\u0442\u044b\u0445 \u0451\u043c\u043a\u043e\u0441\u0442\u0435\u0439 \u043d\u0435\u0442" : new StringBuffer("\u041f\u0435\u0440\u0435\u043f\u0430\u044f\u043b \u0451\u043c\u043a\u043e\u0441\u0442\u0438: +").append(i2).append("% \u043a \u0440\u0435\u0441\u0443\u0440\u0441\u0443").toString());
                            return;
                        }
                    } else {
                        if (!rpc.Setup.hasXmp(this.slots[1])) {
                            this.toastMsg("XMP \u043f\u043e\u044f\u0432\u0438\u043b\u0441\u044f \u0442\u043e\u043b\u044c\u043a\u043e \u0441 2007 \u0433\u043e\u0434\u0430");
                            return;
                        }
                        this.xmpOn = !this.xmpOn;
                        rpc.Sound.click();
                        return;
                    }
                } else {
                    if (!rpc.Setup.hasUefi(this.slots[1])) {
                        this.toastMsg("\u0422\u0443\u0442 \u0442\u043e\u043b\u044c\u043a\u043e Legacy BIOS");
                        return;
                    }
                    this.uefiOn = !this.uefiOn;
                    rpc.Sound.click();
                    this.toastMsg((this.uefiOn) ? "UEFI \u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "Legacy \u0440\u0435\u0436\u0438\u043c");
                    return;
                }
            } else {
                if (!rpc.Setup.hasSecureBoot(this.slots[1])) {
                    this.toastMsg("Secure Boot \u044d\u0442\u0430 \u043f\u043b\u0430\u0442\u0430 \u043d\u0435 \u0443\u043c\u0435\u0435\u0442");
                    return;
                }
                this.secureOn = !this.secureOn;
                rpc.Sound.click();
                return;
            }
        } else {
            if (!rpc.Setup.hasTpm(this.slots[1])) {
                this.toastMsg("\u0412 \u044d\u0442\u043e\u0439 \u043f\u0440\u043e\u0448\u0438\u0432\u043a\u0435 \u043d\u0435\u0442 TPM \u2014 \u0442\u043e\u043b\u044c\u043a\u043e \u043f\u0430\u0442\u0447-\u043e\u0431\u0445\u043e\u0434");
                return;
            }
            this.tpmOn = !this.tpmOn;
            rpc.Sound.click();
            this.toastMsg((this.tpmOn) ? "TPM 2.0 \u0432\u043a\u043b\u044e\u0447\u0451\u043d" : "TPM 2.0 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d");
            return;
        }
    }
    
    private void stressTest() {
        int i = rpc.Diag.stability(this.slots, this.hackLevel);
        rpc.Sound.fan(1800, rpc.Diag.dustOf(this.slots));
        if (i >= 70) {
            this.toastMsg(new StringBuffer("\u0421\u0442\u0440\u0435\u0441\u0441-\u0442\u0435\u0441\u0442: ").append(i).append("% \u2014 \u0434\u0435\u0440\u0436\u0438\u0442\u0441\u044f").toString());
            rpc.Sound.ok();
            return;
        }
        this.toastMsg(new StringBuffer("\u0421\u0442\u0440\u0435\u0441\u0441-\u0442\u0435\u0441\u0442: ").append(i).append("% \u2014 \u0432\u0430\u043b\u0438\u0442\u0441\u044f").toString());
        this.bsod(156, "MACHINE_CHECK_EXCEPTION");
    }
    
    public void debugSet(int i, rpc.Part a) {
        if (i >= 0 && i < 20) {
            this.slots[i] = a;
        }
    }
    
    public void debugSpec(rpc.Part a) {
        this.openSpec(a, 2);
    }
    
    public void debugCrash() {
        this.bsod(123, "INACCESSIBLE_BOOT_DEVICE");
    }
    
    private void fixFault(rpc.Diag$Fault a) {
        if (a.slot >= 0 && a.slot < 20 && this.slots[a.slot] != null) {
            rpc.Part a0 = this.slots[a.slot];
            if (a.code != 2) {
                if (a.code != 5) {
                    if (a.code != 8) {
                        if (a.code != 3) {
                            if (a.code != 1) {
                                if (a.code != 6) {
                                    if (a.code != 4) {
                                        this.toastMsg("\u042d\u0442\u0443 \u043d\u0435\u0438\u0441\u043f\u0440\u0430\u0432\u043d\u043e\u0441\u0442\u044c \u0442\u0430\u043a \u043d\u0435 \u0443\u0431\u0440\u0430\u0442\u044c");
                                    } else {
                                        this.toastMsg("\u0411\u041f \u043d\u0435 \u0442\u044f\u043d\u0435\u0442 \u2014 \u043d\u0443\u0436\u0435\u043d \u043c\u043e\u0449\u043d\u0435\u0435");
                                    }
                                } else {
                                    this.toastMsg("\u0414\u0438\u0441\u043a \u0449\u0451\u043b\u043a\u0430\u0435\u0442 \u2014 \u0442\u043e\u043b\u044c\u043a\u043e \u0437\u0430\u043c\u0435\u043d\u0430");
                                }
                            } else {
                                int i = rpc.Diag.reseat(a0);
                                rpc.Sound.revived();
                                this.toastMsg(new StringBuffer("\u041f\u0435\u0440\u0435\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u043b \u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440: +").append(i).append("%").toString());
                            }
                        } else {
                            int i0 = rpc.Diag.reseat(a0);
                            rpc.Diag.blow(a0);
                            rpc.Sound.revived();
                            this.toastMsg(new StringBuffer("\u041f\u0435\u0440\u0435\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u043b \u043a\u0430\u0440\u0442\u0443 \u0438 \u043f\u0440\u043e\u0434\u0443\u043b: +").append(i0).append("%").toString());
                        }
                    } else {
                        int i1 = rpc.Diag.recap(a0);
                        rpc.Sound.click();
                        this.toastMsg(new StringBuffer("\u041f\u0435\u0440\u0435\u043f\u0430\u044f\u043b \u0451\u043c\u043a\u043e\u0441\u0442\u0438: +").append(i1).append("% (").append(a0.name).append(")").toString());
                    }
                } else {
                    int i2 = rpc.Diag.blow(a0);
                    rpc.Sound.revived();
                    this.toastMsg(new StringBuffer("\u041f\u0440\u043e\u0434\u0443\u043b \u0440\u0430\u0434\u0438\u0430\u0442\u043e\u0440: -").append(i2).append("% \u043f\u044b\u043b\u0438").toString());
                }
            } else {
                int i3 = rpc.Diag.eraser(a0);
                rpc.Sound.revived();
                this.toastMsg(new StringBuffer("\u041f\u0440\u043e\u0442\u0451\u0440 \u043a\u043e\u043d\u0442\u0430\u043a\u0442\u044b \u043b\u0430\u0441\u0442\u0438\u043a\u043e\u043c: +").append(i3).append("% (").append(a0.name).append(")").toString());
            }
            this.runPost(false);
            return;
        }
        this.toastMsg("\u0414\u0435\u0442\u0430\u043b\u044c \u043d\u0443\u0436\u043d\u043e \u0437\u0430\u043c\u0435\u043d\u0438\u0442\u044c");
    }
    
    private void openSpec(rpc.Part a, int i) {
        if (a == null) {
            this.toastMsg("\u0421\u043b\u043e\u0442 \u043f\u0443\u0441\u0442 \u2014 \u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043d\u0435\u0447\u0435\u0433\u043e");
            return;
        }
        this.specPart = a;
        this.specBack = i;
        rpc.Sound.click();
        this.mode = 24;
    }
    
    private void pickOs(int i) {
        rpc.Inst$Os a = rpc.Inst.all()[i];
        if (a.year > this.levelYear()) {
            this.toastMsg(new StringBuffer(String.valueOf((Object)String.valueOf((Object)a.name))).append(" \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 ").append(a.year).append(" \u0433\u043e\u0434\u0443").toString());
            return;
        }
        rpc.Part a0 = this.slots[0];
        rpc.Part a1 = this.slots[1];
        rpc.Part a2 = this.slots[7];
        int i0 = 0;
        int i1 = 0;
        while(i1 < 4) {
            if (this.slots[2 + i1] != null) {
                i0 = i0 + this.slots[2 + i1].f[1];
            }
            i1 = i1 + 1;
        }
        int i2 = rpc.Inst.blocker(a, a0, a1, i0, (long)(a2 != null && a2.f[1] != 0 ? 1 : 0), this.tpmOn, this.secureOn, this.uefiOn, this.patches);
        if (i2 == 0) {
            this.openLang(a, 0);
            return;
        }
        if (!rpc.Inst.patchHelps(a, i2)) {
            this.toastMsg(new StringBuffer("\u041d\u0435 \u0432\u0441\u0442\u0430\u043d\u0435\u0442: ").append(rpc.Inst.blockerText(i2)).toString());
            return;
        }
        int i3 = ((a.patchMask & 4) == 0) ? 1 : 4;
        if ((this.patches & i3) == 0) {
            this.toastMsg("\u041d\u0443\u0436\u0435\u043d \u043f\u0430\u0442\u0447-\u043e\u0431\u0445\u043e\u0434: \u0441\u043a\u0430\u0447\u0430\u0439\u0442\u0435 \u0432 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0435");
            return;
        }
        this.openLang(a, 1);
    }
    
    private int langYear() {
        return (this.pendingOs == null) ? this.levelYear() : this.pendingOs.year;
    }
    
    private void openLang(rpc.Inst$Os a, int i) {
        this.pendingOs = a;
        this.pendingHack = i;
        int i0 = rpc.Langs.count(this.langYear());
        this.langSel = 0;
        int i1 = 0;
        while(i1 < i0) {
            if (rpc.Langs.nth(this.langYear(), i1) == this.osLang) {
                this.langSel = i1;
            }
            i1 = i1 + 1;
        }
        this.mode = 28;
    }
    
    private void startInstall(rpc.Inst$Os a, int i) {
        if (a == null) {
            this.mode = 21;
            return;
        }
        rpc.Part a0 = this.slots[0];
        rpc.Part a1 = this.slots[7];
        if (this.bootSel != 3 && this.bootSel != 4) {
            this.installingOs = a;
            this.hackLevel = i;
            this.instTotal = rpc.Inst.minutes(a, a0, a1);
            int i0 = rpc.Langs.installPenalty(this.osLang);
            if (i0 > 0) {
                this.instTotal = this.instTotal * (100 + i0) / 100;
            }
            this.instLeft = this.instTotal;
            this.mode = 22;
            rpc.Sound.installStep(0);
            this.toastMsg((i <= 0) ? "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u043d\u0430\u0447\u0430\u043b\u0430\u0441\u044c" : "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0441 \u043e\u0431\u0445\u043e\u0434\u043e\u043c \u043f\u0440\u043e\u0432\u0435\u0440\u043e\u043a");
            return;
        }
        rpc.Sound.error();
        this.toastMsg(new StringBuffer("BIOS \u0433\u0440\u0443\u0437\u0438\u0442\u0441\u044f \u0441 \u00ab").append(BOOT[this.bootSel]).append("\u00bb \u2014 \u0434\u043e \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0449\u0438\u043a\u0430 \u043d\u0435 \u0434\u043e\u0439\u0434\u0451\u0442").toString());
    }
    
    private void installStep(int i) {
        if (this.installingOs == null) {
            this.mode = 13;
            return;
        }
        this.instLeft = this.instLeft - i;
        if (this.instLeft > 0) {
            rpc.Sound.installStep(100 - this.instLeft * 100 / ((this.instTotal != 0) ? this.instTotal : 1));
            this.advanceDay((i >= 600) ? 1 : 0);
            return;
        }
        this.instLeft = 0;
        int i0 = rpc.Diag.stability(this.slots, this.hackLevel);
        if (this.rnd(100) > i0) {
            this.failInstall();
            return;
        }
        this.osIndex = rpc.Inst.indexOf(this.installingOs.name);
        rpc.Sound.installDone();
        this.toastMsg(new StringBuffer("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e: ").append(this.installingOs.name).append(" (").append(rpc.Inst.timeText(this.instTotal)).append(")").toString());
        this.installingOs = null;
        this.mode = 13;
    }
    
    private void failInstall() {
        this.installingOs = null;
        this.bsod(123, "INACCESSIBLE_BOOT_DEVICE");
    }
    
    private void bsod(int i, String s) {
        this.bsodCode = i;
        this.bsodName = s;
        this.toast = "";
        rpc.Sound.bsod();
        this.mode = 23;
    }
    
    private void downloadItem(int i) {
        rpc.Inst$Dl[] a = rpc.Inst.downloads(this.levelYear());
        if (i >= a.length) {
            this.mode = 13;
            return;
        }
        rpc.Inst$Dl a0 = a[i];
        int i0 = this.netSpeed();
        if (i0 <= 0) {
            this.toastMsg("\u041d\u0435\u0442 \u0441\u0435\u0442\u0438: \u043d\u0443\u0436\u0435\u043d \u043c\u043e\u0434\u0435\u043c \u044d\u0442\u043e\u0433\u043e \u0433\u043e\u0434\u0430");
            return;
        }
        rpc.Sound.modem(i0);
        int i1 = rpc.Inst.downloadMinutes(a0, i0);
        this.advanceDay(1 + i1 / 600);
        if (a0.patch == 0) {
            if (a0.name.indexOf("DOOM") < 0) {
                this.toastMsg(new StringBuffer("\u0421\u043a\u0430\u0447\u0430\u043d\u043e \u0437\u0430 ").append(rpc.Inst.timeText(i1)).append(": ").append(a0.name).toString());
            } else {
                this.doomUnlocked = true;
                this.toastMsg(new StringBuffer("DOOM \u0441\u043a\u0430\u0447\u0430\u043d \u0437\u0430 ").append(rpc.Inst.timeText(i1)).append(" \u2014 \u043c\u043e\u0436\u043d\u043e \u0437\u0430\u043f\u0443\u0441\u043a\u0430\u0442\u044c").toString());
            }
        } else {
            this.patches = this.patches | a0.patch;
            this.toastMsg(new StringBuffer("\u0421\u043a\u0430\u0447\u0430\u043d\u043e \u0437\u0430 ").append(rpc.Inst.timeText(i1)).append(": ").append(a0.name).append(" (\u043f\u0430\u0442\u0447 \u0433\u043e\u0442\u043e\u0432)").toString());
        }
    }
    
    private void buySim() {
        int i = this.levelYear();
        if (i < 2003) {
            this.toastMsg("\u0421\u0438\u043c-\u043a\u0430\u0440\u0442\u044b \u0434\u043b\u044f \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0430 \u043f\u043e\u044f\u0432\u044f\u0442\u0441\u044f \u043f\u043e\u0441\u043b\u0435 2003");
            return;
        }
        if (this.simBought) {
            rpc.Sound.modem(this.netSpeed());
            this.toastMsg(new StringBuffer("\u0421\u0438\u043c\u043a\u0430 \u0432 \u043c\u043e\u0434\u0435\u043c\u0435: \u0434\u043e ").append(rpc.Game.mobileSpeed(i)).append(" \u041c\u0431\u0438\u0442/\u0441 (").append(this.netKind()).append(")").toString());
            return;
        }
        if (this.cash < 40) {
            rpc.Sound.error();
            this.toastMsg("\u0421\u0438\u043c\u043a\u0430 \u0441\u0442\u043e\u0438\u0442 40 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432");
            return;
        }
        this.cash = this.cash - 40;
        this.spent = this.spent + 40;
        this.simBought = true;
        rpc.Sound.ok();
        this.toastMsg(new StringBuffer("\u041a\u0443\u043f\u043b\u0435\u043d\u0430 \u0441\u0438\u043c-\u043a\u0430\u0440\u0442\u0430: \u043c\u043e\u0431\u0438\u043b\u044c\u043d\u044b\u0439 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442 \u0434\u043e ").append(rpc.Game.mobileSpeed(i)).append(" \u041c\u0431\u0438\u0442/\u0441").toString());
    }
    
    private void runGame() {
        if (this.osReady() && this.osIndex >= 0) {
            if (!this.doomUnlocked) {
                this.toastMsg("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0441\u043a\u0430\u0447\u0430\u0439\u0442\u0435 DOOM \u0432 \u0438\u043d\u0442\u0435\u0440\u043d\u0435\u0442\u0435");
                return;
            }
            rpc.Sound.doom();
            rpc.Sound.disk(false, true);
            this.mode = 26;
            return;
        }
        this.toastMsg("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u041e\u0421 (\u043f\u0443\u043d\u043a\u0442 \u00ab\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u041e\u0421\u00bb)");
    }
    
    private void paintSetup() {
        this.header("BIOS", (this.slots[1] != null) ? new StringBuffer().append(this.slots[1].year).toString() : "");
        int i = this.FH0 + 7;
        int i0 = this.FH1 + 14;
        int i1 = (this.H - i0 - this.FH0 - 20) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        int i2 = this.setupSel;
        int i3 = 13 + this.faults.length;
        if (i2 >= i3) {
            this.setupSel = i3 - 1;
        }
        int i4 = (this.setupSel <= i1 - 1) ? 0 : this.setupSel - i1 + 1;
        int i5 = 0;
        while(true) {
            if (i5 < i1) {
                int i6 = i4 + i5;
                if (i6 < i3) {
                    String s = null;
                    int i7 = i0 + i5 * i;
                    boolean b = i6 == this.setupSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i7, this.W - 8, i - 1);
                    if (i6 != 0) {
                        if (i6 != 1) {
                            if (i6 != 2) {
                                if (i6 != 3) {
                                    if (i6 != 4) {
                                        if (i6 != 5) {
                                            if (i6 != 6) {
                                                if (i6 != 7) {
                                                    s = (i6 != 8) ? (i6 != 9) ? (i6 != 10) ? (i6 != 11) ? (i6 != 12) ? new StringBuffer("! ").append(this.faults[i6 - 13].title).toString() : "\u0421\u0442\u0440\u0435\u0441\u0441-\u0442\u0435\u0441\u0442 \u0441\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e\u0441\u0442\u0438" : "\u0412\u044b\u0439\u0442\u0438 \u0432 \u0441\u0431\u043e\u0440\u043a\u0443" : "\u0421\u0431\u0440\u043e\u0441 \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a (Clear CMOS)" : "\u041f\u0440\u043e\u0434\u0443\u0442\u044c \u0432\u0441\u044e \u0441\u0431\u043e\u0440\u043a\u0443" : "\u041f\u0435\u0440\u0435\u043f\u0430\u044f\u0442\u044c \u0451\u043c\u043a\u043e\u0441\u0442\u0438";
                                                } else {
                                                    s = new StringBuffer("XMP: ").append((rpc.Setup.hasXmp(this.slots[1])) ? (this.xmpOn) ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b" : "\u043d\u0435\u0442").toString();
                                                }
                                            } else {
                                                s = new StringBuffer("\u0420\u0435\u0436\u0438\u043c UEFI: ").append((rpc.Setup.hasUefi(this.slots[1])) ? (this.uefiOn) ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b" : "\u043d\u0435\u0442").toString();
                                            }
                                        } else {
                                            s = new StringBuffer("Secure Boot: ").append((rpc.Setup.hasSecureBoot(this.slots[1])) ? (this.secureOn) ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b" : "\u043d\u0435\u0442").toString();
                                        }
                                    } else {
                                        s = new StringBuffer("TPM 2.0: ").append((rpc.Setup.hasTpm(this.slots[1])) ? (this.tpmOn) ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b" : "\u043d\u0435\u0442").toString();
                                    }
                                } else {
                                    s = new StringBuffer("\u0414\u0430\u0442\u0430: 12.05.").append((this.slots[1] != null) ? this.slots[1].year : this.levelYear()).toString();
                                }
                            } else {
                                s = new StringBuffer("\u0421\u043f\u0438\u043a\u0435\u0440: ").append((this.speakerOn) ? "\u0432\u043a\u043b" : "\u0432\u044b\u043a\u043b").toString();
                            }
                        } else {
                            s = new StringBuffer("\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430: ").append(BOOT[this.bootSel]).toString();
                        }
                    } else {
                        s = "\u041f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c POST \u0437\u0430\u043d\u043e\u0432\u043e";
                    }
                    this.g.setColor(b ? 3793530 : (i6 < 13) ? 15003895 : 16739179);
                    this.g.drawText(this.fit(s, this.W - 16, 0), 8, i7 + 4, 0);
                    i5 = i5 + 1;
                    continue;
                }
            }
            if (this.setupSel >= 13 && this.setupSel - 13 < this.faults.length) {
                this.g.setColor(16763196);
                this.g.drawText(this.fit(this.faults[this.setupSel - 13].how, this.W - 16, 0), 8, this.H - this.FH0 - 18, 0);
            }
            this.footer(this.fit(new StringBuffer("POST: ").append(rpc.Diag.beepText(this.lastFault)).toString(), this.W - this.g.textWidth("1: \u043d\u0430\u0437\u0430\u0434", 0) - 24, 0), "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintPost() {
        this.header("POST", rpc.Diag.beepText(this.lastFault));
        int i = this.FH1 + 20;
        this.g.setColor((this.lastFault != 0) ? 16739179 : 3793530);
        this.g.drawText((this.lastFault != 0) ? "\u041e\u0428\u0418\u0411\u041a\u0410 POST" : "\u041f\u041e\u0421\u0422 \u043f\u0440\u043e\u0439\u0434\u0435\u043d", 10, i, 1);
        int i0 = i + (this.FH1 + 8);
        int i1 = 0;
        while(true) {
            if (i1 < this.faults.length && i1 < 6) {
                this.g.setColor(15003895);
                this.g.drawText(this.fit(this.faults[i1].title, this.W - 20, 0), 10, i0, 0);
                this.g.setColor(8297134);
                this.g.drawText(this.fit(this.faults[i1].how, this.W - 20, 0), 10, i0 + this.FH0 + 2, 0);
                i0 = i0 + (this.FH0 * 2 + 8);
                i1 = i1 + 1;
                continue;
            }
            this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u0432 BIOS", "");
            return;
        }
    }
    
    private void paintSpec() {
        rpc.Part a = this.specPart;
        if (a == null) {
            this.mode = this.specBack;
            return;
        }
        this.header(a.catName(), new StringBuffer().append(a.year).toString());
        int i = this.FH1 + 14;
        if (rpc.Assets.ready) {
            this.g.setColor(791580);
            this.g.fillRect(6, i, 96, 48);
            rpc.Assets.drawPart(this.g, a, 6, i, 96, 48);
        }
        this.g.setColor(15003895);
        this.g.drawText(this.fit(a.name, this.W - 96 - 24, 0), 110, i + 2, 0);
        this.g.setColor(16763196);
        this.g.drawText(new StringBuffer("\u0446\u0435\u043d\u0430: ").append(a.price).toString(), 110, i + this.FH0 + 6, 0);
        this.g.setColor((a.cond >= 60) ? 8297134 : 16739179);
        this.g.drawText(new StringBuffer("\u0441\u043e\u0441\u0442. ").append(a.cond).append("% \u2022 \u043f\u044b\u043b\u044c ").append(a.dust).append("%").toString(), 110, i + this.FH0 * 2 + 10, 0);
        int i0 = i + 58;
        String[] a0 = this.specLines(a);
        int i1 = 0;
        while(true) {
            if (i1 < a0.length && i0 < this.H - this.FH0 - 16) {
                this.g.setColor(8297134);
                this.g.drawText(this.fit(a0[i1], this.W - 16, 0), 8, i0, 0);
                i0 = i0 + (this.FH0 + 3);
                i1 = i1 + 1;
                continue;
            }
            this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u043d\u0430\u0437\u0430\u0434", "");
            return;
        }
    }
    
    private String[] specLines(rpc.Part a) {
        int i = 0;
        int i0 = 0;
        String[] a0 = this.specLinesBase(a);
        String s = rpc.Ports.line(a);
        int i1 = rpc.Acoustics.db(a);
        int i2 = rpc.Diag.capsLeft(a);
        int i3 = ((s.length() <= 0) ? 0 : 1) + ((i1 <= 0) ? 0 : 1) + ((i2 <= 0) ? 0 : 1);
        if (i3 == 0) {
            return a0;
        }
        String[] a1 = new String[a0.length + i3];
        System.arraycopy((Object)a0, 0, (Object)a1, 0, a0.length);
        int i4 = a0.length;
        if (s.length() <= 0) {
            i = i4;
        } else {
            i = i4 + 1;
            a1[i4] = new StringBuffer("\u0420\u0430\u0437\u044a\u0451\u043c\u044b: ").append(s).toString();
        }
        if (i1 <= 0) {
            i0 = i;
        } else {
            i0 = i + 1;
            a1[i] = new StringBuffer("\u0428\u0443\u043c: ").append(i1).append(" \u0434\u0411(\u0410) \u2014 ").append(rpc.Acoustics.word(i1)).toString();
        }
        if (i2 > 0) {
            a1[i0] = new StringBuffer("\u0401\u043c\u043a\u043e\u0441\u0442\u0438: \u0432\u0437\u0434\u0443\u0442\u043e ").append(i2).append(" \u0448\u0442.").toString();
        }
        return a1;
    }
    
    private String prnKind(int i) {
        String s = null;
        String[] a = new String[9];
        a[0] = "\u043c\u0430\u0442\u0440\u0438\u0447\u043d\u044b\u0439 \u043f\u0440\u0438\u043d\u0442\u0435\u0440";
        a[1] = "\u0441\u0442\u0440\u0443\u0439\u043d\u044b\u0439 \u043f\u0440\u0438\u043d\u0442\u0435\u0440";
        a[2] = "\u043b\u0430\u0437\u0435\u0440\u043d\u044b\u0439 \u043f\u0440\u0438\u043d\u0442\u0435\u0440";
        a[3] = "\u0441\u043a\u0430\u043d\u0435\u0440";
        a[4] = "\u043f\u043b\u043e\u0442\u0442\u0435\u0440";
        a[5] = "\u043a\u043e\u043b\u043e\u043d\u043a\u0438";
        a[6] = "\u043d\u0430\u0443\u0448\u043d\u0438\u043a\u0438";
        a[7] = "\u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d";
        a[8] = "\u0432\u0435\u0431-\u043a\u0430\u043c\u0435\u0440\u0430";
        label2: {
            label0: {
                label1: {
                    if (i < 0) {
                        break label1;
                    }
                    if (i < a.length) {
                        break label0;
                    }
                }
                s = "\u0443\u0441\u0442\u0440\u043e\u0439\u0441\u0442\u0432\u043e";
                break label2;
            }
            s = a[i];
        }
        return s;
    }
    
    private String prnPerf(rpc.Part a) {
        if (a.f[0] <= 2) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1]))).append(" \u0441\u0442\u0440/\u043c\u0438\u043d").toString();
        }
        if (a.f[0] == 3) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1] * 100))).append(" dpi").toString();
        }
        if (a.f[0] == 4) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1]))).append(" \u043c\u00b2/\u0447").toString();
        }
        if (a.f[0] == 5) {
            return new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1]))).append(" \u0412\u0442").toString();
        }
        if (a.f[0] != 8) {
            return new StringBuffer("\u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(a.f[1]).toString();
        }
        return (a.f[1] != 0) ? new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1]))).append(" \u041c\u043f").toString() : "0.3 \u041c\u043f";
    }
    
    private String[] specLinesBase(rpc.Part a) {
        if (a.cat == 1) {
            String[] a0 = new String[7];
            a0[0] = new StringBuffer("\u0421\u043e\u043a\u0435\u0442: ").append(this.sockName(a.f[0])).toString();
            a0[1] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append(a.f[1]).append(" \u041c\u0413\u0446").toString();
            a0[2] = new StringBuffer("\u042f\u0434\u0435\u0440: ").append(a.f[2]).toString();
            a0[3] = new StringBuffer("TDP: ").append(a.f[3]).append(" \u0412\u0442").toString();
            a0[4] = new StringBuffer("\u0418\u043d\u0434\u0435\u043a\u0441 IPC: ").append(a.f[4] / 100).append(".").append(a.f[4] % 100).toString();
            a0[5] = new StringBuffer("\u041f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c: ").append((a.f[5] != 0) ? (a.f[5] != 0) ? "AMD" : "\u0434\u0440\u0443\u0433\u043e\u0439" : "Intel").toString();
            a0[6] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
            return a0;
        }
        if (a.cat != 2 && a.cat != 19) {
            if (a.cat == 3) {
                String[] a1 = new String[7];
                a1[0] = new StringBuffer("\u0421\u043e\u043a\u0435\u0442: ").append(this.sockName(a.f[0])).toString();
                a1[1] = new StringBuffer("\u041f\u0430\u043c\u044f\u0442\u044c: ").append(rpc.Score.ramTypeName(a.f[1])).toString();
                a1[2] = new StringBuffer("\u0421\u043b\u043e\u0442\u043e\u0432 \u041e\u0417\u0423: ").append(a.f[2]).toString();
                a1[3] = new StringBuffer("\u041c\u0430\u043a\u0441\u0438\u043c\u0443\u043c \u041e\u0417\u0423: ").append(rpc.Util.size(a.f[3] * 1024)).toString();
                a1[4] = new StringBuffer("\u0424\u043e\u0440\u043c-\u0444\u0430\u043a\u0442\u043e\u0440: ").append(rpc.Score.formName(a.f[4])).toString();
                a1[5] = new StringBuffer("BIOS: ").append(rpc.Setup.name(a)).toString();
                a1[6] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                return a1;
            }
            if (a.cat == 4) {
                String[] a2 = new String[4];
                a2[0] = new StringBuffer("\u0422\u0438\u043f: ").append(rpc.Score.ramTypeName(a.f[0])).toString();
                a2[1] = new StringBuffer("\u041e\u0431\u044a\u0451\u043c: ").append(rpc.Util.size(a.f[1])).toString();
                a2[2] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append(a.f[2]).append(" \u041c\u0422/\u0441").toString();
                a2[3] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                return a2;
            }
            if (a.cat != 5 && a.cat != 7 && a.cat != 6 && a.cat != 8) {
                if (a.cat == 9) {
                    String[] a3 = new String[3];
                    a3[0] = new StringBuffer("\u041c\u043e\u0449\u043d\u043e\u0441\u0442\u044c: ").append(a.f[0]).append(" \u0412\u0442").toString();
                    a3[1] = new StringBuffer("\u0421\u0442\u0430\u043d\u0434\u0430\u0440\u0442: ").append(this.psuStd(a.f[1])).toString();
                    a3[2] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                    return a3;
                }
                if (a.cat == 12) {
                    String[] a4 = new String[6];
                    a4[0] = new StringBuffer("\u0422\u0435\u0445\u043d\u043e\u043b\u043e\u0433\u0438\u044f: ").append(rpc.Score.monTech(a.f[0])).toString();
                    a4[1] = new StringBuffer("\u0414\u0438\u0430\u0433\u043e\u043d\u0430\u043b\u044c: ").append(a.f[1] / 10).append("\"").toString();
                    a4[2] = new StringBuffer("\u0420\u0430\u0437\u0440\u0435\u0448\u0435\u043d\u0438\u0435: ").append(a.f[2]).append("x").append(a.f[3]).toString();
                    a4[3] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append(a.f[4]).append(" \u0413\u0446").toString();
                    a4[4] = new StringBuffer("\u0413\u043b\u0443\u0431\u0438\u043d\u0430 \u0446\u0432\u0435\u0442\u0430: ").append(a.f[5]).append(" \u0431\u0438\u0442").toString();
                    a4[5] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                    return a4;
                }
                if (a.cat == 14) {
                    String[] a5 = new String[4];
                    a5[0] = new StringBuffer("\u0420\u0430\u0437\u044a\u0451\u043c: ").append(this.busName(a.f[0])).toString();
                    a5[1] = new StringBuffer("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c: ").append(a.f[1]).append(" \u041c\u0431\u0438\u0442/\u0441").toString();
                    a5[2] = ((a.f[2]) <= 0) ? "Wi-Fi: \u043d\u0435\u0442" : new StringBuffer("Wi-Fi: \u0435\u0441\u0442\u044c (").append(a.f[2]).append(")").toString();
                    a5[3] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                    return a5;
                }
                if (a.cat == 20) {
                    String[] a6 = new String[5];
                    a6[0] = new StringBuffer("\u0412\u0438\u0434: ").append(this.prnKind(a.f[0])).toString();
                    a6[1] = new StringBuffer("\u041f\u043e\u043a\u0430\u0437\u0430\u0442\u0435\u043b\u044c: ").append(this.prnPerf(a)).toString();
                    a6[2] = new StringBuffer("\u0418\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441: ").append(rpc.Ports.iface(a.year)).toString();
                    a6[3] = new StringBuffer("\u0428\u0443\u043c: ").append((a.f[0] != 5) ? "\u043f\u043e\u0447\u0442\u0438 \u043d\u0435\u0442" : new StringBuffer(String.valueOf((Object)String.valueOf(a.f[1] / 4 + 24))).append(" \u0434\u0411(\u0410)").toString()).toString();
                    a6[4] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                    return a6;
                }
                if (a.cat != 13) {
                    String[] a7 = new String[3];
                    a7[0] = new StringBuffer("\u041a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u044f: ").append(a.catName()).toString();
                    a7[1] = this.detailText(a);
                    a7[2] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                    return a7;
                }
                String[] a8 = new String[5];
                a8[0] = new StringBuffer("\u0420\u0430\u0437\u044a\u0451\u043c: ").append(this.busName(a.f[0])).toString();
                a8[1] = new StringBuffer("\u0420\u0430\u0437\u0440\u044f\u0434\u043d\u043e\u0441\u0442\u044c: ").append(a.f[1]).append(" \u0431\u0438\u0442").toString();
                a8[2] = new StringBuffer("\u041a\u0430\u043d\u0430\u043b\u043e\u0432: ").append(a.f[2]).toString();
                a8[3] = new StringBuffer("\u041a\u0430\u0447\u0435\u0441\u0442\u0432\u043e: ").append(a.f[3]).append("%").toString();
                a8[4] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
                return a8;
            }
            String[] a9 = new String[5];
            a9[0] = new StringBuffer("\u0418\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441: ").append(rpc.Score.ifaceName(a.f[0])).toString();
            a9[1] = new StringBuffer("\u0401\u043c\u043a\u043e\u0441\u0442\u044c: ").append(rpc.Util.size(a.f[1])).toString();
            a9[2] = (a.f[2] <= 0) ? "\u0411\u0435\u0437 \u0434\u0432\u0438\u0436\u0443\u0449\u0438\u0445\u0441\u044f \u0447\u0430\u0441\u0442\u0435\u0439" : new StringBuffer("\u041e\u0431\u043e\u0440\u043e\u0442\u044b: ").append(a.f[2]).append(" rpm").toString();
            a9[3] = (a.cat != 7) ? "\u0422\u0438\u043f: \u043c\u0435\u0445\u0430\u043d\u0438\u0447\u0435\u0441\u043a\u0438\u0439/\u043e\u043f\u0442\u0438\u0447\u0435\u0441\u043a\u0438\u0439" : "\u0422\u0438\u043f: \u0442\u0432\u0435\u0440\u0434\u043e\u0442\u0435\u043b\u044c\u043d\u044b\u0439";
            a9[4] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
            return a9;
        }
        String[] a10 = new String[7];
        a10[0] = new StringBuffer("\u0412\u0438\u0434\u0435\u043e\u043f\u0430\u043c\u044f\u0442\u044c: ").append(rpc.Util.size(a.f[1])).toString();
        a10[1] = new StringBuffer("\u0428\u0438\u043d\u0430: ").append(a.f[2]).append(" \u0431\u0438\u0442").toString();
        a10[2] = new StringBuffer("\u0427\u0430\u0441\u0442\u043e\u0442\u0430: ").append(a.f[3]).append(" \u041c\u0413\u0446").toString();
        a10[3] = new StringBuffer("TDP: ").append(a.f[4]).append(" \u0412\u0442").toString();
        a10[4] = new StringBuffer("\u041f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c: ").append(a.f[5] / 10).append(".").append(a.f[5] % 10).toString();
        a10[5] = new StringBuffer("\u0420\u0430\u0437\u044a\u0451\u043c: ").append(this.busName(a.f[0])).toString();
        a10[6] = new StringBuffer("\u0413\u043e\u0434: ").append(a.year).toString();
        return a10;
    }
    
    private void paintOsList() {
        this.header("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u041e\u0421", (this.osIndex < 0) ? "\u043d\u0435\u0442 \u0441\u0438\u0441\u0442\u0435\u043c\u044b" : new StringBuffer("\u0441\u0442\u043e\u0438\u0442 ").append(rpc.Inst.all()[this.osIndex].name).toString());
        int i = this.FH0 * 2 + 8;
        int i0 = this.FH1 + 14;
        int i1 = (this.H - i0 - this.FH0 - 24) / i;
        if (i1 < 1) {
            i1 = 1;
        }
        rpc.Inst$Os[] a = rpc.Inst.all();
        if (this.osListSel < this.osListTop) {
            this.osListTop = this.osListSel;
        }
        if (this.osListSel > this.osListTop + i1 - 1) {
            this.osListTop = this.osListSel - i1 + 1;
        }
        rpc.Part a0 = this.slots[0];
        rpc.Part a1 = this.slots[1];
        rpc.Part a2 = this.slots[7];
        int i2 = 0;
        int i3 = 0;
        while(i3 < 4) {
            if (this.slots[2 + i3] != null) {
                i2 = i2 + this.slots[2 + i3].f[1];
            }
            i3 = i3 + 1;
        }
        int i4 = 0;
        while(true) {
            if (i4 < i1) {
                int i5 = this.osListTop + i4;
                if (i5 < a.length) {
                    String s = null;
                    rpc.Inst$Os a3 = a[i5];
                    int i6 = i0 + i4 * i;
                    boolean b = i5 == this.osListSel;
                    boolean b0 = a3.year <= this.levelYear();
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i6, this.W - 8, i - 2);
                    this.g.setColor(b0 ? b ? 3793530 : 15003895 : 8297134);
                    this.g.drawText(this.fit(a3.name, this.W - 70, 0), 8, i6 + 3, 0);
                    this.g.setColor(8297134);
                    if (b0) {
                        int i7 = rpc.Inst.blocker(a3, a0, a1, i2, (long)(a2 != null && a2.f[1] != 0 ? 1 : 0), this.tpmOn, this.secureOn, this.uefiOn, this.patches);
                        s = (i7 != 0) ? rpc.Inst.blockerText(i7) : new StringBuffer("\u043c\u043e\u0436\u043d\u043e, ~").append(rpc.Inst.timeText(rpc.Inst.minutes(a3, a0, a2))).toString();
                    } else {
                        s = new StringBuffer("\u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 ").append(a3.year).toString();
                    }
                    this.g.drawText(this.fit(s, this.W - 16, 0), 8, i6 + 4 + this.FH0, 0);
                    String s0 = new StringBuffer().append(a3.year).toString();
                    this.g.setColor(16763196);
                    this.g.drawText(s0, this.W - this.g.textWidth(s0, 0) - 8, i6 + 3, 0);
                    i4 = i4 + 1;
                    continue;
                }
            }
            this.footer("OK \u2014 \u0441\u0442\u0430\u0432\u0438\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintLang() {
        this.header(new StringBuffer("\u042f\u0437\u044b\u043a: ").append((this.pendingOs != null) ? this.pendingOs.name : "?").toString(), rpc.Langs.CODE[this.osLang]);
        int i = rpc.Langs.count(this.langYear());
        int i0 = this.FH1 + 16;
        int i1 = this.FH1 + 8;
        int i2 = 0;
        while(i2 < i) {
            int i3 = rpc.Langs.nth(this.langYear(), i2);
            int i4 = i0 + i2 * i1;
            boolean b = i2 == this.langSel;
            this.g.setColor(b ? 2308170 : 1780536);
            this.g.fillRect(4, i4, this.W - 8, i1 - 2);
            if (b) {
                this.g.setColor(3793530);
                this.g.fillRect(4, i4, 2, i1 - 2);
            }
            this.g.setColor(b ? 16763196 : 8297134);
            this.g.drawText(rpc.Langs.CODE[i3], 10, i4 + 4, 0);
            this.g.setColor(b ? 15003895 : 8297134);
            String s = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Langs.NAME[i3]))).append(" \u00b7 \u0441 ").append(rpc.Langs.SINCE[i3]).toString();
            this.g.drawText(this.fit(s, this.W - 72, 0), 34, i4 + 4, 0);
            this.g.setColor(8297134);
            String s0 = (rpc.Langs.installPenalty(i3) != 0) ? new StringBuffer("+").append(rpc.Langs.installPenalty(i3)).append("%").toString() : "\u0440\u043e\u0434\u043d\u043e\u0439";
            this.g.drawText(s0, this.W - this.g.textWidth(s0, 0) - 8, i4 + 4, 0);
            i2 = i2 + 1;
        }
        int i5 = i0 + i * i1 + 8;
        this.g.setColor(8297134);
        this.g.drawText("\u042f\u0437\u044b\u043a \u043c\u0435\u043d\u044f\u0435\u0442 \u043f\u043e\u0434\u043f\u0438\u0441\u0438 \u0432 \u0441\u0438\u0441\u0442\u0435\u043c\u0435", 8, i5, 0);
        rpc.Gfx a = this.g;
        int i6 = i5 + (this.FH0 + 2);
        a.drawText("\u0438 \u0442\u0435\u043a\u0441\u0442 \u0441\u0438\u043d\u0435\u0433\u043e \u044d\u043a\u0440\u0430\u043d\u0430. \u0421\u0432\u043e\u0439", 8, i6, 0);
        this.g.drawText("\u0441\u0442\u0430\u0432\u0438\u0442\u0441\u044f \u0434\u043e\u043b\u044c\u0448\u0435 (\u0448\u0440\u0438\u0444\u0442\u044b).", 8, i6 + (this.FH0 + 2), 0);
        this.footer("OK: \u0441\u0442\u0430\u0432\u0438\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
    }
    
    private void paintInstall() {
        if (this.installingOs == null) {
            this.mode = 13;
            return;
        }
        this.header(new StringBuffer("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 ").append(this.installingOs.name).toString(), rpc.Inst.timeText(this.instTotal));
        int i = this.FH1 + 24;
        int i0 = 100 - this.instLeft * 100 / ((this.instTotal != 0) ? this.instTotal : 1);
        this.g.setColor(1780536);
        this.g.fillRect(12, i, this.W - 24, 18);
        this.g.setColor(3793530);
        this.g.fillRect(12, i, (this.W - 24) * i0 / 100, 18);
        this.g.setColor(15003895);
        this.g.drawText(new StringBuffer(String.valueOf((Object)String.valueOf(i0))).append("%").toString(), 16, i + 5, 0);
        int i1 = i + 30;
        String[] a = new String[7];
        a[0] = new StringBuffer("\u042d\u0442\u0430\u043f: ").append((i0 >= 20) ? (i0 >= 60) ? (i0 >= 90) ? "\u043f\u0435\u0440\u0432\u0430\u044f \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0430" : "\u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0430 \u0434\u0440\u0430\u0439\u0432\u0435\u0440\u043e\u0432" : "\u0440\u0430\u0441\u043f\u0430\u043a\u043e\u0432\u043a\u0430" : "\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u0438\u0435 \u0444\u0430\u0439\u043b\u043e\u0432").toString();
        a[1] = new StringBuffer("\u041e\u0441\u0442\u0430\u043b\u043e\u0441\u044c: ").append(rpc.Inst.timeText(this.instLeft)).toString();
        a[2] = new StringBuffer("\u0416\u0435\u043b\u0435\u0437\u043e: ").append((this.slots[0] != null) ? new StringBuffer(String.valueOf(this.slots[0].f[1])).append(" \u041c\u0413\u0446").toString() : "?").toString();
        a[3] = new StringBuffer("\u0414\u0438\u0441\u043a: ").append((this.slots[7] != null) ? rpc.Score.ifaceName(this.slots[7].f[0]) : "?").toString();
        a[4] = new StringBuffer("\u042f\u0437\u044b\u043a: ").append(rpc.Langs.NAME[this.osLang]).append((rpc.Langs.installPenalty(this.osLang) <= 0) ? "" : new StringBuffer(" (+").append(rpc.Langs.installPenalty(this.osLang)).append("%)").toString()).toString();
        a[5] = (this.hackLevel <= 0) ? "\u0420\u0435\u0436\u0438\u043c: \u043e\u0431\u044b\u0447\u043d\u044b\u0439" : "\u0420\u0435\u0436\u0438\u043c: \u0441 \u043e\u0431\u0445\u043e\u0434\u043e\u043c \u043f\u0440\u043e\u0432\u0435\u0440\u043e\u043a";
        a[6] = new StringBuffer("\u0421\u0442\u0430\u0431\u0438\u043b\u044c\u043d\u043e\u0441\u0442\u044c: ").append(rpc.Diag.stability(this.slots, this.hackLevel)).append("%").toString();
        int i2 = 0;
        while(i2 < a.length) {
            this.g.setColor(8297134);
            this.g.drawText(this.fit(a[i2], this.W - 16, 0), 10, i1, 0);
            i1 = i1 + (this.FH0 + 4);
            i2 = i2 + 1;
        }
        this.footer("OK \u2014 +10 \u043c\u0438\u043d\u0443\u0442", "1: \u043f\u0440\u0435\u0440\u0432\u0430\u0442\u044c");
    }
    
    private void paintBsod() {
        this.g.setColor(675490);
        this.g.fillRect(0, 0, this.W, this.H);
        int i = this.FH1 + 20;
        this.g.setColor(16777215);
        this.g.drawText(":(", 12, i, 2);
        int i0 = i + (this.FH2 + 14);
        String[] a = new String[11];
        a[0] = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Langs.t(this.osLang, 8)))).append(":").toString();
        a[1] = rpc.Langs.t(this.osLang, 17);
        a[2] = "";
        a[3] = new StringBuffer("\u041a\u043e\u0434: 0x").append(this.hex(this.bsodCode)).toString();
        a[4] = (this.bsodName.length() <= 0) ? "DRIVER_IRQL_NOT_LESS_OR_EQUAL" : this.bsodName;
        a[5] = "";
        a[6] = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Langs.t(this.osLang, 18)))).append(" ").append(30 + this.bsodCode % 70).append("%").toString();
        a[7] = "";
        a[8] = rpc.Langs.t(this.osLang, 19);
        a[9] = "";
        a[10] = new StringBuffer(String.valueOf((Object)String.valueOf((Object)rpc.Langs.t(this.osLang, 9)))).append(" / OK").toString();
        int i1 = 0;
        while(i1 < a.length) {
            int i2 = 0;
            rpc.Gfx a0 = this.g;
            label2: {
                label0: {
                    label1: {
                        if (i1 == 3) {
                            break label1;
                        }
                        if (i1 != 4) {
                            break label0;
                        }
                    }
                    i2 = 16771496;
                    break label2;
                }
                i2 = 16777215;
            }
            a0.setColor(i2);
            this.g.drawText(this.fit(a[i1], this.W - 24, 0), 12, i0, 0);
            i0 = i0 + (this.FH0 + 4);
            i1 = i1 + 1;
        }
    }
    
    private String hex(int i) {
        String s = "";
        int i0 = 28;
        while(i0 >= 0) {
            StringBuffer a = new StringBuffer(String.valueOf((Object)String.valueOf((Object)s)));
            int i1 = "0123456789ABCDEF".charAt(i >> i0 & 15);
            s = a.append((char)i1).toString();
            i0 = i0 + -4;
        }
        return s;
    }
    
    private void paintWeb() {
        this.header(new StringBuffer("\u0418\u043d\u0442\u0435\u0440\u043d\u0435\u0442 ").append(this.levelYear()).toString(), (this.netSpeed() > 0) ? new StringBuffer(String.valueOf((Object)String.valueOf(this.netSpeed()))).append(" \u041c\u0431\u0438\u0442/\u0441").toString() : "\u043d\u0435\u0442 \u0441\u0435\u0442\u0438");
        int i = this.FH1 + 14;
        this.g.setColor(3793530);
        this.g.drawText("\u0421\u0430\u0439\u0442\u044b \u044d\u043f\u043e\u0445\u0438:", 8, i, 0);
        int i0 = i + (this.FH0 + 4);
        String[] a = rpc.Inst.sites(this.levelYear());
        StringBuffer a0 = new StringBuffer();
        int i1 = 0;
        while(i1 < a.length) {
            if (i1 > 0) {
                a0.append(" \u2022 ");
            }
            a0.append(a[i1]);
            i1 = i1 + 1;
        }
        this.g.setColor(8297134);
        this.g.drawText(this.fit(a0.toString(), this.W - 16, 0), 8, i0, 0);
        this.g.setColor(3793530);
        rpc.Gfx a1 = this.g;
        int i2 = i0 + (this.FH0 + 10);
        a1.drawText("\u0427\u0442\u043e \u043c\u043e\u0436\u043d\u043e \u0441\u043a\u0430\u0447\u0430\u0442\u044c:", 8, i2, 0);
        rpc.Inst$Dl[] a2 = rpc.Inst.downloads(this.levelYear());
        int i3 = this.FH0 * 2 + 8;
        int i4 = this.H;
        int i5 = i2 + (this.FH0 + 6);
        int i6 = (i4 - i5 - this.FH0 - 18) / i3;
        if (i6 < 1) {
            i6 = 1;
        }
        if (this.webSel < this.webTop) {
            this.webTop = this.webSel;
        }
        if (this.webSel > this.webTop + i6 - 1) {
            this.webTop = this.webSel - i6 + 1;
        }
        int i7 = 0;
        while(true) {
            if (i7 < i6) {
                int i8 = this.webTop + i7;
                if (i8 < a2.length + 1) {
                    boolean b = i8 == this.webSel;
                    this.g.setColor(b ? 2308170 : 1780536);
                    this.g.fillRect(4, i5 + i7 * i3, this.W - 8, i3 - 2);
                    if (i8 != a2.length) {
                        boolean b0 = false;
                        rpc.Inst$Dl a3 = a2[i8];
                        int i9 = a3.patch;
                        label2: {
                            label0: {
                                label1: {
                                    if (i9 == 0) {
                                        break label1;
                                    }
                                    if ((this.patches & a3.patch) != 0) {
                                        break label0;
                                    }
                                }
                                b0 = false;
                                break label2;
                            }
                            b0 = true;
                        }
                        this.g.setColor(b ? 3793530 : b0 ? 8297134 : 15003895);
                        this.g.drawText(this.fit(a3.name, this.W - 70, 0), 8, i5 + i7 * i3 + 3, 0);
                        this.g.setColor(8297134);
                        String s = b0 ? "\u0443\u0436\u0435 \u0441\u043a\u0430\u0447\u0430\u043d\u043e" : (this.netSpeed() > 0) ? new StringBuffer("~").append(rpc.Inst.timeText(rpc.Inst.downloadMinutes(a3, this.netSpeed()))).toString() : "\u043d\u0443\u0436\u043d\u0430 \u0441\u0435\u0442\u044c";
                        this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)String.valueOf((Object)s))).append(" \u2022 ").append(a3.note).toString(), this.W - 16, 0), 8, i5 + i7 * i3 + 4 + this.FH0, 0);
                        String s0 = new StringBuffer(String.valueOf(a3.kb)).append("\u041a\u0411").toString();
                        this.g.setColor(16763196);
                        this.g.drawText(s0, this.W - this.g.textWidth(s0, 0) - 8, i5 + i7 * i3 + 3, 0);
                    } else {
                        this.g.setColor(b ? 3793530 : 15003895);
                        this.g.drawText("\u0412\u044b\u0445\u043e\u0434", 8, i5 + i7 * i3 + 4, 0);
                    }
                    i7 = i7 + 1;
                    continue;
                }
            }
            this.footer("OK \u2014 \u0441\u043a\u0430\u0447\u0430\u0442\u044c", "3: \u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c \u043c\u043e\u0434\u0435\u043c");
            return;
        }
    }
    
    private void paintGame() {
        this.header("DOOM (\u0434\u0435\u043c\u043e)", "1993");
        int i = this.FH1 + 20;
        int i0 = rpc.Inst.fps(this.slots[0], this.slots[6]);
        this.g.setColor(2763314);
        this.g.fillRect(8, i, this.W - 16, 70);
        this.g.setColor(3947592);
        int i1 = 0;
        while(i1 < 6) {
            int i2 = (this.W - 32) / 6;
            this.g.fillRect(12 + i1 * i2 + (6 - i1), i + 6 + i1 * 3, i2 - 4, 35);
            i1 = i1 + 1;
        }
        this.g.setColor(5905950);
        this.g.fillRect(this.W / 2 - 8, i + 70 - 18, 16, 18);
        int i3 = i + 80;
        String[] a = new String[4];
        a[0] = new StringBuffer("\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440: ").append((this.slots[0] != null) ? new StringBuffer(String.valueOf(this.slots[0].f[1])).append(" \u041c\u0413\u0446").toString() : "?").toString();
        a[1] = new StringBuffer("\u0412\u0438\u0434\u0435\u043e: ").append((this.slots[6] != null) ? new StringBuffer().append(this.slots[6].f[5] / 10).toString() : "?").toString();
        a[2] = rpc.Inst.fpsText(i0);
        a[3] = (i0 >= 12) ? "\u0418\u0433\u0440\u0430 \u0438\u0434\u0451\u0442 \u043d\u0430 \u044d\u0442\u043e\u043c \u0436\u0435\u043b\u0435\u0437\u0435" : "\u041d\u0430 \u044d\u0442\u043e\u043c \u0436\u0435\u043b\u0435\u0437\u0435 \u0438\u0433\u0440\u0430\u0442\u044c \u0431\u043e\u043b\u044c\u043d\u043e";
        int i4 = 0;
        while(i4 < a.length) {
            this.g.setColor((i4 != 2) ? 15003895 : 16763196);
            this.g.drawText(this.fit(a[i4], this.W - 16, 0), 10, i3, 0);
            i3 = i3 + (this.FH0 + 4);
            i4 = i4 + 1;
        }
        this.footer("\u043b\u044e\u0431\u0430\u044f \u2014 \u0432\u044b\u0439\u0442\u0438", "");
    }
    
    public void pointer(int i, int i0) {
        if (this.toastActive()) {
            this.toast = "";
        }
        if (this.mode == 45) {
            this.pointerExpList(i, i0);
            return;
        }
        if (this.mode == 46) {
            this.pointerExpRun(i, i0);
            return;
        }
        if (this.mode != 0) {
            if (this.mode != 1) {
                if (this.mode != 2) {
                    if (this.mode != 3) {
                        if (this.mode != 7) {
                            if (this.mode != 8) {
                                if (this.mode != 9) {
                                    if (this.mode != 10) {
                                        if (this.mode != 12) {
                                            if (this.mode != 13) {
                                                if (this.mode != 14) {
                                                    if (this.mode != 15) {
                                                        if (this.mode != 17) {
                                                            if (this.mode != 20) {
                                                                if (this.mode != 21) {
                                                                    if (this.mode != 25) {
                                                                        if (this.mode == 22) {
                                                                            this.input(4);
                                                                            return;
                                                                        }
                                                                        label23: if (this.mode != 4) {
                                                                            int i1 = this.mode;
                                                                            label24: {
                                                                                if (i1 == 5) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 6) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 18) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 19) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 11) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 16) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 24) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 23) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode == 27) {
                                                                                    break label24;
                                                                                }
                                                                                if (this.mode != 26) {
                                                                                    break label23;
                                                                                }
                                                                            }
                                                                            this.input(7);
                                                                        } else {
                                                                            this.input(4);
                                                                        }
                                                                        return;
                                                                    } else {
                                                                        int i2 = this.FH1 + 14 + this.FH0 + 4 + this.FH0 + 10 + this.FH0 + 6;
                                                                        int i3 = (i0 - i2) / (this.FH0 * 2 + 4);
                                                                        int i4 = this.webTop + i3;
                                                                        if (i0 > i2 && i3 >= 0 && i4 < rpc.Inst.downloads(this.levelYear()).length + 1) {
                                                                            if (i4 != this.webSel) {
                                                                                this.webSel = i4;
                                                                            } else {
                                                                                this.input(4);
                                                                            }
                                                                        }
                                                                        return;
                                                                    }
                                                                } else {
                                                                    int i5 = this.FH0 * 2 + 8;
                                                                    int i6 = this.FH1 + 14;
                                                                    int i7 = (i0 - i6) / i5;
                                                                    int i8 = this.osListTop + i7;
                                                                    if (i0 > i6 && i7 >= 0 && i8 < rpc.Inst.all().length) {
                                                                        if (i8 != this.osListSel) {
                                                                            this.osListSel = i8;
                                                                        } else {
                                                                            this.input(4);
                                                                        }
                                                                    }
                                                                    return;
                                                                }
                                                            } else {
                                                                int i9 = this.FH0 + 7;
                                                                int i10 = this.FH1 + 14;
                                                                int i11 = (i0 - i10) / i9;
                                                                int i12 = 13 + this.faults.length;
                                                                if (i0 > i10 && i11 >= 0 && i11 < i12) {
                                                                    this.setupSel = i11;
                                                                    this.input(4);
                                                                }
                                                                return;
                                                            }
                                                        } else {
                                                            int i13 = this.FH0 + 12;
                                                            int i14 = this.FH1 + 16;
                                                            int i15 = (i0 - i14) / i13;
                                                            int i16 = this.taskTop + i15;
                                                            label20: {
                                                                label21: {
                                                                    label22: {
                                                                        if (i0 <= i14) {
                                                                            break label22;
                                                                        }
                                                                        if (i15 < 0) {
                                                                            break label22;
                                                                        }
                                                                        if (i16 < this.tasks.length) {
                                                                            break label21;
                                                                        }
                                                                    }
                                                                    if (i0 <= i14) {
                                                                        break label20;
                                                                    }
                                                                    this.input(7);
                                                                    break label20;
                                                                }
                                                                if (i16 != this.taskSel) {
                                                                    this.taskSel = i16;
                                                                } else {
                                                                    this.input(4);
                                                                }
                                                            }
                                                            return;
                                                        }
                                                    } else {
                                                        int i17 = this.FH0 + 6;
                                                        int i18 = this.FH1 + 14;
                                                        int i19 = (i0 - i18) / i17;
                                                        int i20 = this.editorTop + i19;
                                                        label19: {
                                                            label18: {
                                                                if (i0 <= i18) {
                                                                    break label18;
                                                                }
                                                                if (i19 < 0) {
                                                                    break label18;
                                                                }
                                                                if (i20 >= rpc.Vm.paletteSize(this.editorLang) + 2) {
                                                                    break label18;
                                                                }
                                                                this.editorSel = i20;
                                                                this.input(4);
                                                                break label19;
                                                            }
                                                            if (i0 > i18) {
                                                                this.input(7);
                                                            }
                                                        }
                                                        return;
                                                    }
                                                } else {
                                                    int i21 = this.FH0 + 10;
                                                    int i22 = this.FH1 + 16;
                                                    int i23 = (i0 - i22) / i21;
                                                    int i24 = this.progTop + i23;
                                                    label15: {
                                                        label16: {
                                                            label17: {
                                                                if (i0 <= i22) {
                                                                    break label17;
                                                                }
                                                                if (i23 < 0) {
                                                                    break label17;
                                                                }
                                                                if (i24 < this.progs.length + 1) {
                                                                    break label16;
                                                                }
                                                            }
                                                            if (i0 <= i22) {
                                                                break label15;
                                                            }
                                                            this.input(7);
                                                            break label15;
                                                        }
                                                        if (i24 != this.progSel) {
                                                            this.progSel = i24;
                                                        } else {
                                                            this.input(4);
                                                        }
                                                    }
                                                    return;
                                                }
                                            } else {
                                                int i25 = this.FH1 + 18;
                                                int i26 = (i0 - i25) / (this.FH1 + 10);
                                                label12: {
                                                    label13: {
                                                        label14: {
                                                            if (i0 <= i25) {
                                                                break label14;
                                                            }
                                                            if (i26 < 0) {
                                                                break label14;
                                                            }
                                                            if (i26 < this.osItems()) {
                                                                break label13;
                                                            }
                                                        }
                                                        if (i0 <= i25) {
                                                            break label12;
                                                        }
                                                        this.input(7);
                                                        break label12;
                                                    }
                                                    if (i26 != this.osSel) {
                                                        this.osSel = i26;
                                                    } else {
                                                        this.input(4);
                                                    }
                                                }
                                                return;
                                            }
                                        } else {
                                            int i27 = this.FH0 + 12;
                                            int i28 = this.FH1 + 16;
                                            int i29 = (i0 - i28) / i27;
                                            int i30 = this.benchTop + i29;
                                            label9: {
                                                label10: {
                                                    label11: {
                                                        if (i0 <= i28) {
                                                            break label11;
                                                        }
                                                        if (i29 < 0) {
                                                            break label11;
                                                        }
                                                        if (i30 < this.table.n) {
                                                            break label10;
                                                        }
                                                    }
                                                    if (i0 <= i28) {
                                                        break label9;
                                                    }
                                                    this.input(7);
                                                    break label9;
                                                }
                                                if (i30 != this.benchSel) {
                                                    this.benchSel = i30;
                                                } else {
                                                    this.input(4);
                                                }
                                            }
                                            return;
                                        }
                                    } else {
                                        int i31 = this.FH0 + 12;
                                        int i32 = this.FH1 + 16;
                                        int i33 = (i0 - i32) / i31;
                                        int i34 = this.offerTop + i33;
                                        label6: {
                                            label7: {
                                                label8: {
                                                    if (i0 <= i32) {
                                                        break label8;
                                                    }
                                                    if (i33 < 0) {
                                                        break label8;
                                                    }
                                                    if (i34 < this.offerPart.length) {
                                                        break label7;
                                                    }
                                                }
                                                if (i0 <= i32) {
                                                    break label6;
                                                }
                                                this.input(7);
                                                break label6;
                                            }
                                            if (i34 != this.offerSel) {
                                                this.offerSel = i34;
                                            } else {
                                                this.input(4);
                                            }
                                        }
                                        return;
                                    }
                                } else {
                                    int i35 = this.FH1 + 16;
                                    int i36 = this.FH1 + 18;
                                    int i37 = (i0 - i36) / i35;
                                    int i38 = this.marketTop + i37;
                                    label3: {
                                        label4: {
                                            label5: {
                                                if (i0 <= i36) {
                                                    break label5;
                                                }
                                                if (i37 < 0) {
                                                    break label5;
                                                }
                                                if (i38 < 7) {
                                                    break label4;
                                                }
                                            }
                                            if (i0 <= i36) {
                                                break label3;
                                            }
                                            this.input(7);
                                            break label3;
                                        }
                                        if (i38 != this.marketSel) {
                                            this.marketSel = i38;
                                        } else {
                                            this.input(4);
                                        }
                                    }
                                    return;
                                }
                            } else {
                                int i39 = this.FH0 + 8;
                                int i40 = this.FH1 + 13 + 26 + 4;
                                int i41 = (i0 - i40) / i39;
                                int i42 = this.partTop + i41;
                                label0: {
                                    label1: {
                                        label2: {
                                            if (i41 < 0) {
                                                break label2;
                                            }
                                            if (i42 >= this.partList.length) {
                                                break label2;
                                            }
                                            if (i0 > i40) {
                                                break label1;
                                            }
                                        }
                                        if (i0 >= i40) {
                                            break label0;
                                        }
                                        this.input(4);
                                        break label0;
                                    }
                                    if (i42 != this.partSel) {
                                        this.partSel = i42;
                                    } else {
                                        this.input(4);
                                    }
                                }
                                return;
                            }
                        } else {
                            int i43 = this.FH1 + 14;
                            int i44 = (i0 - i43) / 34;
                            int i45 = this.catTop + i44;
                            if (i44 >= 0 && i45 < CAT_ORDER.length && i0 > i43) {
                                if (i45 != this.catSel) {
                                    this.catSel = i45;
                                } else {
                                    this.input(4);
                                }
                            }
                            return;
                        }
                    } else {
                        int i46 = this.FH0 + 7;
                        int i47 = this.FH1 + 16;
                        int i48 = (i0 - i47) / i46;
                        int i49 = this.shopTop + i48;
                        if (i48 >= 0 && i49 < this.shopList.length && i0 > i47) {
                            if (i49 != this.shopSel) {
                                this.shopSel = i49;
                            } else {
                                this.input(4);
                            }
                        }
                        return;
                    }
                } else if (i0 >= this.H / 3) {
                    int i50 = this.FH0 + 8;
                    int i51 = this.FH1 + 16;
                    int i52 = (i0 - i51) / i50;
                    int i53 = this.buildTop + i52;
                    if (i52 >= 0 && i53 < 27 && i0 > i51) {
                        if (i53 != this.buildSel) {
                            this.buildSel = i53;
                        } else {
                            this.input(4);
                        }
                    }
                    return;
                } else {
                    if (this.pcPowered) {
                        this.mode = 13;
                    } else {
                        this.powerOnPC();
                    }
                    return;
                }
            } else {
                int i54 = this.FH1 + 8;
                int i55 = this.FH1 + 16;
                int i56 = (i0 - i55) / i54;
                int i57 = this.mapTop + i56;
                if (i56 >= 0 && i57 < this.db.levels.length && i0 > i55) {
                    if (i57 != this.mapSel) {
                        this.mapSel = i57;
                    } else {
                        this.input(4);
                    }
                }
                return;
            }
        } else {
            int i58 = (i0 - this.H / 3) / (this.FH1 + 14);
            if (i58 >= 0 && i58 < this.MENU_ITEMS.length) {
                this.menuSel = i58;
                this.menuAction();
            }
            return;
        }
    }
    
    private void openShell() {
        boolean b = false;
        rpc.Inst$Os a = (this.osIndex < 0) ? null : rpc.Inst.all()[this.osIndex];
        String s = (a == null) ? rpc.Os.name(this.levelYear()) : a.name;
        label2: {
            label0: {
                if (a == null) {
                    break label0;
                }
                boolean b0 = a.gui;
                label1: {
                    if (b0) {
                        break label1;
                    }
                    if (a.name.indexOf("Linux") >= 0) {
                        break label1;
                    }
                    if (a.name.indexOf("Ubuntu") >= 0) {
                        break label1;
                    }
                    if (a.name.indexOf("BeOS") < 0) {
                        break label0;
                    }
                }
                b = true;
                break label2;
            }
            b = a == null && this.levelYear() >= 1994;
        }
        int i = 0;
        int i0 = 0;
        while(i0 < 4) {
            if (this.slots[2 + i0] != null) {
                i = i + this.slots[2 + i0].f[1];
            }
            i0 = i0 + 1;
        }
        if (i <= 0) {
            i = 640;
        }
        int i1 = (this.slots[0] == null) ? 8 : this.slots[0].f[1];
        if (this.slots[7] != null) {
        }
        this.shell.reset(b ? 1 : 0, s, i, i1, 20);
        this.shellTop = 0;
        this.mode = 29;
    }
    
    private void paintShell() {
        this.header(new StringBuffer("\u041e\u0431\u043e\u043b\u043e\u0447\u043a\u0430: ").append((this.shell.osKind() != 0) ? "UNIX" : "DOS").toString(), this.shell.prompt);
        int i = this.FH1 + 8;
        int i0 = (this.H - i - this.FH0 - 14) / (this.FH0 + 1);
        this.g.setColor(329995);
        this.g.fillRect(2, i - 2, this.W - 4, this.H - i - this.FH0 - 8);
        int i1 = 0;
        while(i1 < i0) {
            String s = this.shell.line(i1, i0);
            this.g.setColor((s.startsWith(this.shell.prompt)) ? 7139466 : 3050058);
            if (s.length() > 0) {
                this.g.drawText(this.fit(s, this.W - 10, 0), 5, i, 0);
            }
            i = i + (this.FH0 + 1);
            i1 = i1 + 1;
        }
        this.footer("OK: \u043a\u043e\u043c\u0430\u043d\u0434\u0430  \u2191\u2193 \u043b\u0438\u0441\u0442\u0430\u0442\u044c", "1: \u0432\u044b\u0445\u043e\u0434");
    }
    
    private void shellCommand() {
        String s = this.host.textInput(this.shell.prompt, "", 60);
        if (s == null) {
            return;
        }
        if (this.shell.run(s)) {
            this.mode = 13;
            this.toastMsg("\u0412\u044b\u0445\u043e\u0434 \u0438\u0437 \u043e\u0431\u043e\u043b\u043e\u0447\u043a\u0438");
        }
        rpc.Sound.click();
    }
    
    private void paintBrowse() {
        if (this.browseSite >= 0) {
            String[] a = rpc.Browser.page(this.browseSite, this.browsePage, this.levelYear(), this.db, this.corp);
            this.header(new StringBuffer(String.valueOf((Object)rpc.Browser.NAME[this.browseSite])).append(" ").append(this.browsePage + 1).toString(), rpc.Browser.URL[this.browseSite]);
            int i = this.FH1 + 10;
            int i0 = (this.H - i - this.FH0 - 14) / (this.FH0 + 2);
            int i1 = this.browseScroll;
            int i2 = 0;
            while(true) {
                if (i2 < i0) {
                    int i3 = i1 + i2;
                    if (i3 < a.length) {
                        this.g.setColor((i3 != 0) ? (i3 != 1) ? 15003895 : 16763196 : 3793530);
                        this.g.drawText(this.fit(a[i3], this.W - 10, 0), 5, i + i2 * (this.FH0 + 2), 0);
                        i2 = i2 + 1;
                        continue;
                    }
                }
                this.footer("OK: \u043b\u0438\u0441\u0442\u0430\u0442\u044c 3: \u043a\u0443\u043f\u0438\u0442\u044c", "1: \u043a \u0441\u0430\u0439\u0442\u0430\u043c");
                return;
            }
        } else {
            this.header(new StringBuffer("\u0418\u043d\u0442\u0435\u0440\u043d\u0435\u0442 ").append(this.levelYear()).toString(), new StringBuffer("\u0441\u0430\u0439\u0442\u043e\u0432: ").append(rpc.Browser.count(this.levelYear())).toString());
            int i4 = this.FH1 + 12;
            int i5 = rpc.Browser.count(this.levelYear());
            int i6 = 0;
            while(i6 < i5) {
                int i7 = rpc.Browser.nth(this.levelYear(), i6);
                int i8 = i4 + i6 * (this.FH1 + 8);
                boolean b = i6 == this.siteSel;
                this.g.setColor(b ? 2308170 : 1780536);
                this.g.fillRect(4, i8, this.W - 8, this.FH1 + 6);
                this.g.setColor(b ? 3793530 : 15003895);
                this.g.drawText(rpc.Browser.NAME[i7], 10, i8 + 3, 0);
                this.g.setColor(8297134);
                String s = rpc.Browser.URL[i7];
                this.g.drawText(s, this.W - this.g.textWidth(s, 0) - 8, i8 + 3, 0);
                i6 = i6 + 1;
            }
            this.footer("OK: \u043e\u0442\u043a\u0440\u044b\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void browseAction() {
        boolean b = false;
        int i = this.browseSite;
        label2: {
            label0: {
                label1: {
                    if (i == 6) {
                        break label1;
                    }
                    if (this.browseSite == 7) {
                        break label1;
                    }
                    if (this.browseSite != 5) {
                        break label0;
                    }
                }
                b = true;
                break label2;
            }
            b = false;
        }
        int i0 = rpc.Browser.pages(this.browseSite, this.levelYear(), this.db);
        if (this.browsePage + 1 < i0) {
            this.browsePage = this.browsePage + 1;
            this.browseScroll = 0;
            return;
        }
        this.browsePage = 0;
        this.browseScroll = 0;
        if (b) {
            int i1 = rpc.Browser.offerPart(this.browseSite, this.browsePage, this.levelYear(), this.db);
            if (i1 >= 0) {
                this.buyFromBrowser(i1);
            }
        }
    }
    
    private void buyFromBrowser(int i) {
        rpc.Part a = this.db.get(i);
        if (a == null) {
            return;
        }
        int i0 = rpc.Browser.price(this.browseSite, a);
        if (i0 > this.cash) {
            rpc.Sound.error();
            this.toastMsg(new StringBuffer("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432: ").append(i0).toString());
            return;
        }
        int i1 = (this.browseSite != 6) ? 100 : 55 + this.rnd(40);
        this.cash = this.cash - i0;
        this.table.order(a, i0, i1, (this.browseSite != 6) ? 20 : 5, this.day + 2, (this.browseSite == 6) ? 1 : 0);
        rpc.Sound.ok();
        this.toastMsg(new StringBuffer("\u0417\u0430\u043a\u0430\u0437 \u0441 ").append(rpc.Browser.NAME[this.browseSite]).append(": ").append(i0).toString());
    }
    
    private void paintCorp() {
        if (this.corp.founded) {
            this.header(new StringBuffer(String.valueOf((Object)this.corp.name)).append(" \u2022 ").append(this.corp.year).toString(), new StringBuffer(String.valueOf((Object)rpc.Util.money(this.corp.cash))).append(" \u2022 R&D ").append(this.corp.rd).toString());
            int i = this.FH1 + 8;
            String[] a = new String[11];
            a[0] = "\u041f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440: \u043a\u043e\u043d\u0441\u0442\u0440\u0443\u043a\u0442\u043e\u0440";
            a[1] = "\u0412\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0430: \u043a\u043e\u043d\u0441\u0442\u0440\u0443\u043a\u0442\u043e\u0440";
            a[2] = "\u041e\u043f\u0435\u0440\u0430\u0446\u0438\u043e\u043d\u043d\u0430\u044f \u0441\u0438\u0441\u0442\u0435\u043c\u0430";
            a[3] = "\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430 (\u0441\u043e\u0444\u0442)";
            a[4] = new StringBuffer("\u041c\u043e\u0438 \u043f\u0440\u043e\u0434\u0443\u043a\u0442\u044b (").append(this.corp.n).append(")").toString();
            a[5] = "\u041d\u0430\u0447\u0430\u0442\u044c \u0434\u0435\u043d\u044c (+\u0440\u043e\u044f\u043b\u0442\u0438)";
            a[6] = new StringBuffer("\u041d\u0430\u043d\u044f\u0442\u044c \u0438\u043d\u0436\u0435\u043d\u0435\u0440\u0430 (").append(this.corp.engineers).append(")").toString();
            a[7] = new StringBuffer("\u041d\u0430\u043d\u044f\u0442\u044c \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0438\u0441\u0442\u0430 (").append(this.corp.programmers).append(")").toString();
            a[8] = new StringBuffer("\u0420\u0435\u043a\u043b\u0430\u043c\u0430: \u0443\u0440\u043e\u0432\u0435\u043d\u044c ").append(this.corp.marketing).toString();
            a[9] = new StringBuffer("\u0422\u0435\u0441\u0442\u044b: \u0443\u0440\u043e\u0432\u0435\u043d\u044c ").append(this.corp.testing).toString();
            a[10] = "\u0412\u044b\u0445\u043e\u0434 \u0432 \u0438\u0433\u0440\u0443";
            int i0 = this.FH1 + 4;
            int i1 = (this.H - i - this.FH0 * 2 - 22) / i0;
            if (i1 < 3) {
                i1 = 3;
            }
            if (this.corpSel < this.corpTop) {
                this.corpTop = this.corpSel;
            }
            if (this.corpSel > this.corpTop + i1 - 1) {
                this.corpTop = this.corpSel - i1 + 1;
            }
            int i2 = 0;
            while(i2 < a.length) {
                if (i2 >= this.corpTop && i2 < this.corpTop + i1) {
                    int i3 = i + (i2 - this.corpTop) * i0;
                    boolean b = i2 == this.corpSel;
                    if (b) {
                        this.g.setColor(2308170);
                        this.g.fillRect(3, i3, this.W - 6, this.FH1 + 3);
                        this.g.setColor(3793530);
                        this.g.fillRect(3, i3, 2, this.FH1 + 3);
                    }
                    this.g.setColor(b ? 15003895 : 8297134);
                    this.g.drawText(this.fit(a[i2], this.W - 12, 0), 8, i3 + 2, 0);
                }
                i2 = i2 + 1;
            }
            int i4 = this.H - this.FH0 * 2 - 24;
            this.g.setColor(8297134);
            this.g.drawText(new StringBuffer("\u0440\u0435\u043f\u0443\u0442\u0430\u0446\u0438\u044f ").append(this.corp.reputation).append(" \u2022 \u0441\u043e\u0434\u0435\u0440\u0436\u0430\u043d\u0438\u0435 ").append(this.corp.upkeep()).append("/\u0434\u0435\u043d\u044c").toString(), 6, i4, 0);
            this.g.drawText(new StringBuffer("\u0434\u043e\u0445\u043e\u0434 ").append(this.corp.dailyIncome()).append("/\u0434\u0435\u043d\u044c").toString(), 6, i4 + this.FH0 + 2, 0);
            this.footer("OK: \u0432\u044b\u0431\u0440\u0430\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        } else {
            this.header("\u041a\u043e\u0440\u043f\u043e\u0440\u0430\u0446\u0438\u044f", "\u0444\u0438\u0440\u043c\u0430 \u043d\u0435 \u043e\u0441\u043d\u043e\u0432\u0430\u043d\u0430");
            String[] a0 = new String[11];
            a0[0] = "\u041e\u0441\u043d\u0443\u0439\u0442\u0435 \u0441\u0432\u043e\u044e \u0444\u0438\u0440\u043c\u0443 \u0438 \u043a\u043e\u043d\u043a\u0443\u0440\u0438\u0440\u0443\u0439\u0442\u0435";
            a0[1] = "\u0441 Intel, AMD, NVIDIA \u0438 Microsoft.";
            a0[2] = "";
            a0[3] = "\u0423 \u0432\u0430\u0441 \u0431\u0443\u0434\u0443\u0442: \u043a\u0440\u0435\u0434\u0438\u0442\u044b, \u043e\u0447\u043a\u0438 R&D,";
            a0[4] = "\u0438\u043d\u0436\u0435\u043d\u0435\u0440\u044b-\u0441\u0445\u0435\u043c\u043e\u0442\u0435\u0445\u043d\u0438\u043a\u0438 \u0438 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0438\u0441\u0442\u044b,";
            a0[5] = "\u043b\u0430\u0431\u043e\u0440\u0430\u0442\u043e\u0440\u0438\u044f, \u0440\u0435\u043a\u043b\u0430\u043c\u0430 \u0438 \u0442\u0435\u0441\u0442\u044b.";
            a0[6] = "";
            a0[7] = "\u0412\u044b\u043f\u0443\u0449\u0435\u043d\u043d\u044b\u0435 \u0447\u0438\u043f\u044b \u043f\u043e\u043f\u0430\u0434\u0443\u0442 \u0432 \u043a\u0430\u0442\u0430\u043b\u043e\u0433,";
            a0[8] = "\u0441\u0438\u0441\u0442\u0435\u043c\u044b \u2014 \u0432 \u0441\u043f\u0438\u0441\u043e\u043a \u0434\u043b\u044f \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043a\u0438.";
            a0[9] = "";
            a0[10] = new StringBuffer("OK \u2014 \u043e\u0441\u043d\u043e\u0432\u0430\u0442\u044c \u0444\u0438\u0440\u043c\u0443 \u0432 ").append(this.levelYear()).toString();
            int i5 = 0;
            while(i5 < a0.length) {
                this.g.setColor((i5 != 0) ? 15003895 : 3793530);
                this.g.drawText(this.fit(a0[i5], this.W - 12, 0), 6, this.FH1 + 16 + i5 * (this.FH0 + 4), 0);
                i5 = i5 + 1;
            }
            this.footer("OK: \u043e\u0441\u043d\u043e\u0432\u0430\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void corpAction() {
        if (!this.corp.founded) {
            this.corp.found("\u0421\u0412\u041e\u042f \u0424\u0418\u0420\u041c\u0410", this.levelYear());
            this.corp.year = this.levelYear();
            rpc.Sound.ok();
            this.toastMsg(new StringBuffer("\u0424\u0438\u0440\u043c\u0430 \u043e\u0441\u043d\u043e\u0432\u0430\u043d\u0430: ").append(this.corp.cash).append(" \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432").toString());
            return;
        }
        if (this.corpSel != 0) {
            if (this.corpSel != 1) {
                if (this.corpSel != 2) {
                    if (this.corpSel != 3) {
                        if (this.corpSel != 4) {
                            if (this.corpSel != 5) {
                                if (this.corpSel != 6) {
                                    if (this.corpSel != 7) {
                                        if (this.corpSel != 8) {
                                            if (this.corpSel != 9) {
                                                this.mode = 0;
                                            } else {
                                                this.corp.testing = (this.corp.testing + 1) % 4;
                                            }
                                        } else {
                                            this.corp.marketing = (this.corp.marketing + 1) % 4;
                                        }
                                    } else {
                                        int i = 500 + this.corp.programmers * 180;
                                        if (this.corp.cash < i) {
                                            this.toastMsg(new StringBuffer("\u041d\u0443\u0436\u043d\u043e ").append(i).toString());
                                        } else {
                                            rpc.Corp a = this.corp;
                                            a.cash = a.cash - i;
                                            rpc.Corp a0 = this.corp;
                                            a0.programmers = a0.programmers + 1;
                                            this.toastMsg(new StringBuffer("\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0438\u0441\u0442 \u043d\u0430\u043d\u044f\u0442 (").append(i).append(")").toString());
                                        }
                                    }
                                } else {
                                    int i0 = 600 + this.corp.engineers * 200;
                                    if (this.corp.cash < i0) {
                                        this.toastMsg(new StringBuffer("\u041d\u0443\u0436\u043d\u043e ").append(i0).toString());
                                    } else {
                                        rpc.Corp a1 = this.corp;
                                        a1.cash = a1.cash - i0;
                                        rpc.Corp a2 = this.corp;
                                        a2.engineers = a2.engineers + 1;
                                        this.toastMsg(new StringBuffer("\u0418\u043d\u0436\u0435\u043d\u0435\u0440 \u043d\u0430\u043d\u044f\u0442 (").append(i0).append(")").toString());
                                    }
                                }
                            } else {
                                int i1 = this.corp.tickDay();
                                rpc.Corp a3 = this.corp;
                                a3.year = a3.year + ((this.corp.day % 30 != 0) ? 0 : 1);
                                this.day = this.day + 1;
                                this.toastMsg(new StringBuffer("\u0414\u0435\u043d\u044c ").append(this.corp.day).append(": \u0434\u043e\u0445\u043e\u0434 ").append(i1).toString());
                            }
                        } else {
                            this.prodSel = 0;
                            this.mode = 35;
                        }
                    } else {
                        this.softSel = 0;
                        this.softKind = 0;
                        this.mode = 34;
                    }
                } else {
                    this.osSpec = new rpc.Corp$OsSpec();
                    this.osSpec.year = this.corp.year;
                    this.osSpec.minRamKb = 4096;
                    this.osSpec.minMhz = 33;
                    this.osSpecSel = 0;
                    this.mode = 33;
                }
            } else {
                this.chip = new rpc.Corp$Chip();
                this.chip.cat = 1;
                this.chip.year = this.corp.year;
                this.chip.busIdx = 8;
                this.chip.vramKb = 1024;
                this.chip.bits = 64;
                this.chipSel = 0;
                this.mode = 32;
            }
        } else {
            this.chip = new rpc.Corp$Chip();
            this.chip.cat = 0;
            this.chip.year = this.corp.year;
            this.chip.nodeNm = rpc.Corp.NODES[Math.max(0, rpc.Corp.nodeIndex(1500))];
            this.chip.socketIdx = this.bestSocket();
            this.chipSel = 0;
            this.mode = 32;
        }
    }
    
    private int bestSocket() {
        int i = this.corp.year;
        rpc.Db a = this.db;
        int[] a0 = new int[1];
        a0[0] = 3;
        int[] a1 = a.query(a0, i - 2, i);
        if (a1.length <= 0) {
            return 0;
        }
        return this.db.get(a1[a1.length - 1]).f[0];
    }
    
    private int chipField(int i) {
        switch(i) {
            case 7: {
                return (this.chip.cat != 0) ? this.chip.vramKb : 0;
            }
            case 6: {
                return (this.chip.cat != 0) ? this.chip.busIdx : this.chip.socketIdx;
            }
            case 5: {
                return this.chip.cacheKb;
            }
            case 4: {
                return (this.chip.cat != 0) ? this.chip.ipc : this.chip.cores * 100 + this.chip.ipc;
            }
            case 3: {
                return this.chip.mhz;
            }
            case 2: {
                return this.chip.transistorsM;
            }
            case 1: {
                return this.chip.nodeNm;
            }
            case 0: {
                return this.chip.year;
            }
            default: {
                return 0;
            }
        }
    }
    
    private String chipValue(int i) {
        switch(i) {
            case 7: {
                return (this.chip.cat != 0) ? new StringBuffer(String.valueOf(this.chip.vramKb)).append(" \u041a\u0411 / ").append(this.chip.bits).append(" \u0431\u0438\u0442").toString() : "\u2014";
            }
            case 6: {
                return (this.chip.cat != 0) ? rpc.Score.busName(this.chip.busIdx) : rpc.Score.sockName(this.chip.socketIdx);
            }
            case 5: {
                return new StringBuffer().append(this.chip.cacheKb).toString();
            }
            case 4: {
                return (this.chip.cat != 0) ? new StringBuffer("IPC ").append(this.chip.ipc).toString() : new StringBuffer(String.valueOf(this.chip.cores)).append(" / IPC ").append(this.chip.ipc).toString();
            }
            case 3: {
                return new StringBuffer().append(this.chip.mhz).toString();
            }
            case 2: {
                return new StringBuffer().append(this.chip.transistorsM).toString();
            }
            case 1: {
                return rpc.Corp.nodeText(this.chip.nodeNm);
            }
            case 0: {
                return new StringBuffer().append(this.chip.year).toString();
            }
            default: {
                return "";
            }
        }
    }
    
    private void chipAdjust(int i) {
        int i0 = this.chipSel;
        label1: if (i0 != 0) {
            if (i0 != 1) {
                if (i0 != 2) {
                    if (i0 != 3) {
                        if (i0 != 4) {
                            if (i0 != 5) {
                                if (i0 != 6) {
                                    if (i0 == 7 && this.chip.cat == 1) {
                                        if (i <= 0) {
                                            this.chip.bits = (this.chip.bits > 8) ? this.chip.bits / 2 : 8;
                                        } else {
                                            this.chip.bits = (this.chip.bits < 512) ? this.chip.bits * 2 : 512;
                                        }
                                        rpc.Corp$Chip a = this.chip;
                                        a.vramKb = a.vramKb + i * 1024;
                                        if (this.chip.vramKb < 256) {
                                            this.chip.vramKb = 256;
                                        }
                                    }
                                } else if (this.chip.cat != 0) {
                                    rpc.Corp$Chip a0 = this.chip;
                                    a0.busIdx = a0.busIdx + i;
                                    if (this.chip.busIdx < 0) {
                                        this.chip.busIdx = 0;
                                    }
                                    if (this.chip.busIdx > 21) {
                                        this.chip.busIdx = 21;
                                    }
                                } else {
                                    rpc.Corp$Chip a1 = this.chip;
                                    a1.socketIdx = a1.socketIdx + i;
                                    int i1 = (this.db.SOC.length <= 0) ? 0 : this.db.SOC.length - 1;
                                    if (this.chip.socketIdx < 0) {
                                        this.chip.socketIdx = 0;
                                    }
                                    if (this.chip.socketIdx > i1) {
                                        this.chip.socketIdx = i1;
                                    }
                                }
                            } else {
                                int i2 = (this.chip.cacheKb >= 64) ? (this.chip.cacheKb >= 1024) ? 1024 : 64 : 8;
                                rpc.Corp$Chip a2 = this.chip;
                                a2.cacheKb = a2.cacheKb + i * i2;
                                if (this.chip.cacheKb < 0) {
                                    this.chip.cacheKb = 0;
                                }
                            }
                        } else {
                            int i3 = this.chip.cat;
                            label0: {
                                if (i3 != 0) {
                                    break label0;
                                }
                                if (i <= 0) {
                                    break label0;
                                }
                                if (this.chip.ipc >= 200) {
                                    break label0;
                                }
                                rpc.Corp$Chip a3 = this.chip;
                                a3.ipc = a3.ipc + 5;
                                break label1;
                            }
                            if (this.chip.cat != 0) {
                                rpc.Corp$Chip a4 = this.chip;
                                a4.ipc = a4.ipc + i * 5;
                                if (this.chip.ipc < 50) {
                                    this.chip.ipc = 50;
                                }
                                if (this.chip.ipc > 200) {
                                    this.chip.ipc = 200;
                                }
                            } else if (this.chip.ipc <= 100) {
                                rpc.Corp$Chip a5 = this.chip;
                                a5.cores = a5.cores + i;
                                if (this.chip.cores < 1) {
                                    this.chip.cores = 1;
                                }
                                if (this.chip.cores > 64) {
                                    this.chip.cores = 64;
                                }
                            } else {
                                rpc.Corp$Chip a6 = this.chip;
                                a6.ipc = a6.ipc - 5;
                            }
                        }
                    } else {
                        int i4 = (this.chip.mhz >= 50) ? (this.chip.mhz >= 500) ? (this.chip.mhz >= 3000) ? 200 : 100 : 25 : 1;
                        rpc.Corp$Chip a7 = this.chip;
                        a7.mhz = a7.mhz + i * i4;
                        if (this.chip.mhz < 1) {
                            this.chip.mhz = 1;
                        }
                    }
                } else {
                    int i5 = (this.chip.transistorsM >= 10) ? (this.chip.transistorsM >= 100) ? (this.chip.transistorsM >= 1000) ? 500 : 50 : 5 : 1;
                    rpc.Corp$Chip a8 = this.chip;
                    a8.transistorsM = a8.transistorsM + i * i5;
                    if (this.chip.transistorsM < 1) {
                        this.chip.transistorsM = 1;
                    }
                }
            } else {
                int i6 = rpc.Corp.nodeIndex(this.chip.nodeNm) + i;
                if (i6 < 0) {
                    i6 = 0;
                }
                if (i6 >= rpc.Corp.NODES.length) {
                    i6 = rpc.Corp.NODES.length - 1;
                }
                this.chip.nodeNm = rpc.Corp.NODES[i6];
            }
        } else {
            rpc.Corp$Chip a9 = this.chip;
            a9.year = a9.year + i;
            if (this.chip.year > this.levelYear()) {
                this.chip.year = this.levelYear();
            }
            if (this.chip.year < 1980) {
                this.chip.year = 1980;
            }
        }
    }
    
    private void paintChip() {
        this.header(new StringBuffer("\u041a\u043e\u043d\u0441\u0442\u0440\u0443\u043a\u0442\u043e\u0440: ").append((this.chip.cat != 0) ? "GPU" : "CPU").toString(), new StringBuffer(String.valueOf((Object)rpc.Util.money(this.corp.cash))).append(" \u2022 R&D ").append(this.corp.rd).toString());
        int i = this.FH1 + 10;
        int i0 = (this.chip.cat != 0) ? 8 : 7;
        int i1 = 0;
        while(true) {
            if (i1 < i0 && i1 < CHIP_FIELDS.length) {
                int i2 = i + i1 * (this.FH1 + 4);
                boolean b = i1 == this.chipSel;
                if (b) {
                    this.g.setColor(2308170);
                    this.g.fillRect(3, i2, this.W - 6, this.FH1 + 3);
                    this.g.setColor(3793530);
                    this.g.fillRect(3, i2, 2, this.FH1 + 3);
                }
                this.g.setColor(b ? 15003895 : 8297134);
                this.g.drawText(CHIP_FIELDS[i1], 8, i2 + 2, 0);
                String s = this.chipValue(i1);
                this.g.setColor(b ? 16763196 : 15003895);
                this.g.drawText(this.fit(s, this.W / 2 - 10, 0), this.W / 2, i2 + 2, 0);
                i1 = i1 + 1;
                continue;
            }
            int i3 = rpc.Corp.tdp(this.chip);
            int i4 = rpc.Corp.overclock(this.chip);
            int i5 = rpc.Corp.quality(this.chip, this.corp.testing, this.corp.engineers, rpc.Corp.devRd(this.chip));
            int i6 = i + 8 * (this.FH1 + 4) + 4;
            this.g.setColor((i4 <= 130) ? (i4 <= 105) ? 3793530 : 16763196 : 16739179);
            this.g.drawText(new StringBuffer("TDP ").append(i3).append(" \u0412\u0442 \u2022 \u0447\u0430\u0441\u0442\u043e\u0442\u0430 ").append(i4).append("% \u043e\u0442 \u043d\u043e\u0440\u043c\u044b").toString(), 8, i6, 0);
            this.g.setColor(8297134);
            this.g.drawText(new StringBuffer("\u043f\u0440\u043e\u0438\u0437\u0432\u043e\u0434\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c ").append(rpc.Corp.perf(this.chip)).toString(), 8, i6 + this.FH0 + 2, 0);
            this.g.drawText(new StringBuffer("\u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(i5).append("% \u2022 \u0431\u0440\u0430\u043a ").append(rpc.Corp.bugsOf(i5)).append("%").toString(), 8, i6 + this.FH0 * 2 + 4, 0);
            this.g.drawText(new StringBuffer("\u043d\u0443\u0436\u043d\u043e: R&D ").append(rpc.Corp.devRd(this.chip)).append(" \u2022 ").append(rpc.Corp.devCash(this.chip)).append(" \u043a\u0440 \u2022 ").append(rpc.Corp.devDays(this.chip, this.corp.engineers)).append(" \u0434\u043d").toString(), 8, i6 + this.FH0 * 3 + 6, 0);
            this.footer("OK: \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c", "\u2190\u2192 \u043f\u0440\u0430\u0432\u043a\u0430 1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void chipRelease() {
        int i = rpc.Corp.suggestPrice(this.chip);
        String s = new StringBuffer(String.valueOf((Object)this.corp.name)).append(" ").append((this.chip.cat != 0) ? "GPU" : "CPU").append("-").append(this.chip.mhz).append(" (").append(this.chip.year).append(")").toString();
        rpc.Corp$Prod a = this.corp.releaseChip(this.chip, this.db, s, i);
        if (a == null) {
            rpc.Sound.error();
            this.toastMsg("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 R&D \u0438\u043b\u0438 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432");
            return;
        }
        rpc.Sound.ok();
        this.toastMsg(new StringBuffer("\u0412\u044b\u043f\u0443\u0449\u0435\u043d ").append(s).append(" \u2022 $").append(i).append(" \u2022 \u0431\u0440\u0430\u043a ").append(a.bugs).append("%").toString());
        this.mode = 31;
    }
    
    private void paintCorpOs() {
        String s = null;
        this.header("\u0421\u0432\u043e\u044f \u041e\u0421", new StringBuffer(String.valueOf((Object)rpc.Util.money(this.corp.cash))).append(" \u2022 R&D ").append(this.corp.rd).toString());
        int i = this.FH1 + 10;
        String[] a = new String[6];
        a[0] = "\u0413\u043e\u0434";
        a[1] = "\u041c\u0438\u043d. \u041e\u0417\u0423, \u041a\u0411";
        a[2] = "\u041c\u0438\u043d. \u0447\u0430\u0441\u0442\u043e\u0442\u0430, \u041c\u0413\u0446";
        a[3] = "\u0422\u0438\u043f: \u0442\u0435\u043a\u0441\u0442/\u0433\u0440\u0430\u0444\u0438\u043a\u0430";
        a[4] = "\u0414\u0438\u0441\u043a";
        a[5] = "\u0412\u043e\u0437\u043c\u043e\u0436\u043d\u043e\u0441\u0442\u0438";
        String[] a0 = new String[6];
        a0[0] = new StringBuffer().append(this.osSpec.year).toString();
        a0[1] = new StringBuffer().append(this.osSpec.minRamKb).toString();
        a0[2] = new StringBuffer().append(this.osSpec.minMhz).toString();
        a0[3] = (this.osSpec.gui != 1) ? "\u0442\u0435\u043a\u0441\u0442\u043e\u0432\u0430\u044f" : "\u0433\u0440\u0430\u0444\u0438\u0447\u0435\u0441\u043a\u0430\u044f";
        a0[4] = (this.osSpec.disk != 0) ? (this.osSpec.disk != 1) ? "SSD" : "HDD" : "\u043b\u044e\u0431\u043e\u0439";
        a0[5] = new StringBuffer().append(this.osSpec.features).toString();
        int i0 = 0;
        while(i0 < a.length) {
            int i1 = i + i0 * (this.FH1 + 4);
            boolean b = i0 == this.osSpecSel;
            if (b) {
                this.g.setColor(2308170);
                this.g.fillRect(3, i1, this.W - 6, this.FH1 + 3);
                this.g.setColor(3793530);
                this.g.fillRect(3, i1, 2, this.FH1 + 3);
            }
            this.g.setColor(b ? 15003895 : 8297134);
            this.g.drawText(a[i0], 8, i1 + 2, 0);
            this.g.setColor(b ? 16763196 : 15003895);
            this.g.drawText(this.fit(a0[i0], this.W / 2 - 12, 0), this.W / 2, i1 + 2, 0);
            i0 = i0 + 1;
        }
        int i2 = i + 7 * (this.FH1 + 4);
        int i3 = rpc.Corp.osQuality(this.osSpec, this.corp.testing, this.corp.programmers);
        this.g.setColor(8297134);
        this.g.drawText(new StringBuffer("\u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(i3).append("% \u2022 \u0431\u0440\u0430\u043a ").append(rpc.Corp.bugsOf(i3)).append("%").toString(), 8, i2, 0);
        this.g.drawText(new StringBuffer("\u043d\u0443\u0436\u043d\u043e: R&D ").append(rpc.Corp.osRd(this.osSpec)).append(" \u2022 ").append(rpc.Corp.osCash(this.osSpec)).append(" \u043a\u0440").toString(), 8, i2 + this.FH0 + 2, 0);
        rpc.Gfx a1 = this.g;
        int i4 = this.osSpec.gui;
        label2: {
            label0: {
                label1: {
                    if (i4 != 1) {
                        break label1;
                    }
                    if (this.osSpec.minRamKb < 2048) {
                        break label0;
                    }
                }
                s = "OK \u2014 \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u0442\u0438\u0440\u0430\u0436";
                break label2;
            }
            s = "\u0433\u0440\u0430\u0444\u0438\u043a\u0430 \u043f\u0440\u0438 \u0442\u0430\u043a\u043e\u043c \u041e\u0417\u0423 \u043d\u0435 \u043f\u043e\u0434\u043d\u0438\u043c\u0435\u0442\u0441\u044f";
        }
        a1.drawText(s, 8, i2 + this.FH0 * 2 + 4, 0);
        this.footer("OK: \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c", "\u2190\u2192 \u043f\u0440\u0430\u0432\u043a\u0430 1: \u043d\u0430\u0437\u0430\u0434");
    }
    
    private void corpOsAdjust(int i) {
        if (this.osSpecSel != 0) {
            if (this.osSpecSel != 1) {
                if (this.osSpecSel != 2) {
                    if (this.osSpecSel != 3) {
                        if (this.osSpecSel != 4) {
                            if (this.osSpecSel == 5) {
                                rpc.Corp$OsSpec a = this.osSpec;
                                a.features = a.features + i;
                                if (this.osSpec.features < 1) {
                                    this.osSpec.features = 1;
                                }
                                if (this.osSpec.features > 5) {
                                    this.osSpec.features = 5;
                                }
                            }
                        } else {
                            this.osSpec.disk = (this.osSpec.disk + 1) % 3;
                        }
                    } else {
                        this.osSpec.gui = (this.osSpec.gui != 1) ? 1 : 0;
                    }
                } else {
                    rpc.Corp$OsSpec a0 = this.osSpec;
                    a0.minMhz = a0.minMhz + i * ((this.osSpec.minMhz >= 100) ? (this.osSpec.minMhz >= 1000) ? 200 : 50 : 8);
                    if (this.osSpec.minMhz < 4) {
                        this.osSpec.minMhz = 4;
                    }
                }
            } else {
                rpc.Corp$OsSpec a1 = this.osSpec;
                a1.minRamKb = a1.minRamKb * ((i <= 0) ? 1 : 2);
                if (i < 0) {
                    rpc.Corp$OsSpec a2 = this.osSpec;
                    a2.minRamKb = a2.minRamKb / 2;
                }
                if (this.osSpec.minRamKb < 64) {
                    this.osSpec.minRamKb = 64;
                }
                if (this.osSpec.minRamKb > 8388608) {
                    this.osSpec.minRamKb = 8388608;
                }
            }
        } else {
            rpc.Corp$OsSpec a3 = this.osSpec;
            a3.year = a3.year + i;
            if (this.osSpec.year > this.levelYear()) {
                this.osSpec.year = this.levelYear();
            }
            if (this.osSpec.year < 1980) {
                this.osSpec.year = 1980;
            }
        }
    }
    
    private void corpOsRelease() {
        String s = new StringBuffer(String.valueOf((Object)this.corp.name)).append(" ").append((this.osSpec.gui != 1) ? "DOS" : "OS").append(" ").append(this.osSpec.year).toString();
        int i = 25 + this.osSpec.features * 10 + ((this.osSpec.gui != 1) ? 0 : 25);
        if (this.corp.releaseOs(this.osSpec, rpc.Game$InstHolder.inst(), s, i) == null) {
            rpc.Sound.error();
            this.toastMsg("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 R&D \u0438\u043b\u0438 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432");
            return;
        }
        rpc.Sound.ok();
        this.toastMsg(new StringBuffer("\u0412\u044b\u043f\u0443\u0449\u0435\u043d\u0430 ").append(s).append(": \u043c\u043e\u0436\u043d\u043e \u0441\u0442\u0430\u0432\u0438\u0442\u044c \u0441 \u044d\u043a\u0440\u0430\u043d\u0430 \u041e\u0421").toString());
        this.mode = 31;
    }
    
    private void paintSoft() {
        this.header("\u041f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0430", new StringBuffer("R&D ").append(this.corp.rd).toString());
        int i = this.FH1 + 12;
        int i0 = 0;
        while(i0 < rpc.Corp.SOFT_NAME.length) {
            int i1 = i + i0 * (this.FH1 + 6);
            boolean b = i0 == this.softSel;
            if (b) {
                this.g.setColor(2308170);
                this.g.fillRect(4, i1, this.W - 8, this.FH1 + 4);
            }
            this.g.setColor(b ? 3793530 : 15003895);
            this.g.drawText(rpc.Corp.SOFT_NAME[i0], 10, i1 + 3, 0);
            i0 = i0 + 1;
        }
        int i2 = i + 4 * (this.FH1 + 6) + 6;
        int i3 = 40 + this.corp.programmers * 8 + this.corp.testing * 10;
        if (i3 > 97) {
            i3 = 97;
        }
        this.g.setColor(8297134);
        this.g.drawText(new StringBuffer("\u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(i3).append("% \u2022 \u0431\u0440\u0430\u043a ").append(rpc.Corp.bugsOf(i3)).append("%").toString(), 8, i2, 0);
        this.g.drawText(new StringBuffer("\u0446\u0435\u043d\u0430 ").append(10 + i3 / 2).append(" \u2022 R&D ").append(20 + i3 / 2).toString(), 8, i2 + this.FH0 + 2, 0);
        this.g.drawText("OK \u2014 \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c", 8, i2 + this.FH0 * 2 + 4, 0);
        this.footer("OK: \u0432\u044b\u043f\u0443\u0441\u0442\u0438\u0442\u044c", "1: \u043d\u0430\u0437\u0430\u0434");
    }
    
    private void softRelease() {
        int i = 40 + this.corp.programmers * 8 + this.corp.testing * 10;
        if (i > 97) {
            i = 97;
        }
        String s = new StringBuffer(String.valueOf((Object)this.corp.name)).append(" ").append(rpc.Corp.SOFT_NAME[this.softKind]).append(" 1.0").toString();
        if (this.corp.releaseSoft(this.softKind, i, s, 10 + i / 2) == null) {
            this.toastMsg("\u041d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 R&D \u0438\u043b\u0438 \u043a\u0440\u0435\u0434\u0438\u0442\u043e\u0432");
            return;
        }
        rpc.Sound.ok();
        this.toastMsg(new StringBuffer("\u0412\u044b\u043f\u0443\u0449\u0435\u043d\u043e: ").append(s).toString());
        this.mode = 31;
    }
    
    private void paintProds() {
        this.header("\u041f\u0440\u043e\u0434\u0443\u043a\u0442\u044b \u0444\u0438\u0440\u043c\u044b", new StringBuffer("\u0432\u0441\u0435\u0433\u043e ").append(this.corp.n).toString());
        int i = this.FH1 + 10;
        if (this.corp.n == 0) {
            this.g.setColor(8297134);
            this.g.drawText("\u041f\u043e\u043a\u0430 \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u0432\u044b\u043f\u0443\u0449\u0435\u043d\u043e.", 8, i, 0);
            this.footer("1: \u043d\u0430\u0437\u0430\u0434", (String)null);
            return;
        }
        int i0 = (this.H - i - this.FH0 - 16) / (this.FH0 * 3 + 8);
        int i1 = 0;
        while(true) {
            if (i1 < this.corp.n && i1 < i0) {
                rpc.Corp$Prod a = this.corp.prods[i1];
                int i2 = i + i1 * (this.FH0 * 3 + 8);
                boolean b = i1 == this.prodSel;
                if (b) {
                    this.g.setColor(2308170);
                    this.g.fillRect(3, i2 - 1, this.W - 6, this.FH0 * 3 + 6);
                }
                this.g.setColor(b ? 3793530 : 15003895);
                this.g.drawText(this.fit(this.corp.line(i1), this.W - 12, 0), 7, i2, 0);
                this.g.setColor(8297134);
                this.g.drawText(new StringBuffer(String.valueOf(a.year)).append(" \u2022 $").append(a.price).append(" \u2022 \u043a\u0430\u0447\u0435\u0441\u0442\u0432\u043e ").append(a.quality).append("% \u2022 \u0431\u0430\u0433 ").append(a.bugs).append("%").toString(), 7, i2 + this.FH0 + 2, 0);
                this.g.setColor(16763196);
                this.g.drawText(new StringBuffer("\u0434\u043e\u043b\u044f \u0440\u044b\u043d\u043a\u0430 ").append(this.corp.share(a)).append("% \u2022 \u043f\u0440\u043e\u0434\u0430\u043d\u043e ").append(a.sold).toString(), 7, i2 + this.FH0 * 2 + 4, 0);
                i1 = i1 + 1;
                continue;
            }
            this.footer("OK: \u043f\u0430\u0442\u0447 (R&D)", "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void inputExtra(int i) {
        if (this.mode != 29) {
            if (this.mode != 30) {
                if (this.mode != 31) {
                    if (this.mode != 32) {
                        if (this.mode != 33) {
                            if (this.mode != 34) {
                                if (this.mode != 36) {
                                    if (this.mode != 35) {
                                        if (this.mode != 43) {
                                            if (this.mode != 44) {
                                                return;
                                            }
                                            this.initRepairOrders();
                                            int i0 = this.repairOrders.length;
                                            label25: {
                                                label28: {
                                                    if (i != 0) {
                                                        break label28;
                                                    }
                                                    if (i0 <= 0) {
                                                        break label28;
                                                    }
                                                    this.repairSel = (this.repairSel + i0 - 1) % i0;
                                                    break label25;
                                                }
                                                label27: {
                                                    if (i != 1) {
                                                        break label27;
                                                    }
                                                    if (i0 <= 0) {
                                                        break label27;
                                                    }
                                                    this.repairSel = (this.repairSel + 1) % i0;
                                                    break label25;
                                                }
                                                if (i != 4) {
                                                    label26: {
                                                        if (i == 7) {
                                                            break label26;
                                                        }
                                                        if (i != 5) {
                                                            break label25;
                                                        }
                                                    }
                                                    this.mode = 2;
                                                } else {
                                                    this.actionRepairOrder();
                                                }
                                            }
                                            return;
                                        } else {
                                            label23: {
                                                label24: {
                                                    if (i == 4) {
                                                        break label24;
                                                    }
                                                    if (i == 7) {
                                                        break label24;
                                                    }
                                                    if (i == 5) {
                                                        break label24;
                                                    }
                                                    if (i != 6) {
                                                        break label23;
                                                    }
                                                }
                                                this.pcPowered = true;
                                                rpc.Sound.ok();
                                                this.mode = 13;
                                            }
                                            return;
                                        }
                                    } else {
                                        label19: {
                                            label22: {
                                                if (i != 0) {
                                                    break label22;
                                                }
                                                if (this.corp.n <= 0) {
                                                    break label22;
                                                }
                                                this.prodSel = (this.prodSel + this.corp.n - 1) % this.corp.n;
                                                break label19;
                                            }
                                            label21: {
                                                if (i != 1) {
                                                    break label21;
                                                }
                                                if (this.corp.n <= 0) {
                                                    break label21;
                                                }
                                                this.prodSel = (this.prodSel + 1) % this.corp.n;
                                                break label19;
                                            }
                                            if (i != 4) {
                                                label20: {
                                                    if (i == 7) {
                                                        break label20;
                                                    }
                                                    if (i != 5) {
                                                        break label19;
                                                    }
                                                }
                                                this.mode = 31;
                                            } else if (this.corp.patch(this.prodSel)) {
                                                rpc.Sound.ok();
                                                this.toastMsg("\u041f\u0430\u0442\u0447 \u0432\u044b\u043f\u0443\u0449\u0435\u043d");
                                            } else {
                                                this.toastMsg("R&D \u043d\u0435 \u0445\u0432\u0430\u0442\u0430\u0435\u0442 \u0438\u043b\u0438 \u0431\u0430\u0433\u043e\u0432 \u0443\u0436\u0435 \u043d\u0435\u0442");
                                            }
                                        }
                                        return;
                                    }
                                } else {
                                    int i1 = this.gamesList.length;
                                    label14: if (i != 2) {
                                        if (i != 3) {
                                            label18: {
                                                if (i != 0) {
                                                    break label18;
                                                }
                                                if (i1 <= 0) {
                                                    break label18;
                                                }
                                                this.gamesSel = (this.gamesSel + i1 - 1) % i1;
                                                break label14;
                                            }
                                            label17: {
                                                if (i != 1) {
                                                    break label17;
                                                }
                                                if (i1 <= 0) {
                                                    break label17;
                                                }
                                                this.gamesSel = (this.gamesSel + 1) % i1;
                                                break label14;
                                            }
                                            label16: {
                                                if (i != 4) {
                                                    break label16;
                                                }
                                                if (i1 <= 0) {
                                                    break label16;
                                                }
                                                this.runGameTest();
                                                break label14;
                                            }
                                            label15: {
                                                if (i == 7) {
                                                    break label15;
                                                }
                                                if (i != 5) {
                                                    break label14;
                                                }
                                            }
                                            if (this.gamesRan) {
                                                this.gamesRan = false;
                                            } else {
                                                this.mode = 13;
                                            }
                                        } else {
                                            this.gamesYear = (this.gamesYear < 2026) ? this.gamesYear + 1 : 2026;
                                            this.refillGames();
                                        }
                                    } else {
                                        this.gamesYear = (this.gamesYear > 1980) ? this.gamesYear - 1 : 1980;
                                        this.refillGames();
                                    }
                                    return;
                                }
                            } else {
                                label12: if (i != 0) {
                                    if (i != 1) {
                                        if (i != 4) {
                                            label13: {
                                                if (i == 7) {
                                                    break label13;
                                                }
                                                if (i != 5) {
                                                    break label12;
                                                }
                                            }
                                            this.mode = 31;
                                        } else {
                                            this.softKind = this.softSel;
                                            this.softRelease();
                                        }
                                    } else {
                                        this.softSel = (this.softSel + 1) % rpc.Corp.SOFT_NAME.length;
                                    }
                                } else {
                                    this.softSel = (this.softSel + rpc.Corp.SOFT_NAME.length - 1) % rpc.Corp.SOFT_NAME.length;
                                }
                                return;
                            }
                        } else {
                            label10: if (i != 0) {
                                if (i != 1) {
                                    if (i != 2) {
                                        if (i != 3) {
                                            if (i != 4) {
                                                label11: {
                                                    if (i == 7) {
                                                        break label11;
                                                    }
                                                    if (i != 5) {
                                                        break label10;
                                                    }
                                                }
                                                this.mode = 31;
                                            } else {
                                                this.corpOsRelease();
                                            }
                                        } else {
                                            this.corpOsAdjust(1);
                                        }
                                    } else {
                                        this.corpOsAdjust(-1);
                                    }
                                } else {
                                    this.osSpecSel = (this.osSpecSel + 1) % 6;
                                }
                            } else {
                                this.osSpecSel = (this.osSpecSel + 5) % 6;
                            }
                            return;
                        }
                    } else {
                        int i2 = (this.chip.cat != 0) ? 8 : 7;
                        label8: if (i != 0) {
                            if (i != 1) {
                                if (i != 2) {
                                    if (i != 3) {
                                        if (i != 4) {
                                            label9: {
                                                if (i == 7) {
                                                    break label9;
                                                }
                                                if (i != 5) {
                                                    break label8;
                                                }
                                            }
                                            this.mode = 31;
                                        } else {
                                            this.chipRelease();
                                        }
                                    } else {
                                        this.chipAdjust(1);
                                    }
                                } else {
                                    this.chipAdjust(-1);
                                }
                            } else {
                                this.chipSel = (this.chipSel + 1) % i2;
                            }
                        } else {
                            this.chipSel = (this.chipSel + i2 - 1) % i2;
                        }
                        return;
                    }
                } else {
                    label6: if (i != 0) {
                        if (i != 1) {
                            if (i != 4) {
                                label7: {
                                    if (i == 7) {
                                        break label7;
                                    }
                                    if (i != 5) {
                                        break label6;
                                    }
                                }
                                this.mode = 0;
                            } else {
                                this.corpAction();
                            }
                        } else {
                            this.corpSel = (this.corpSel + 1) % 11;
                        }
                    } else {
                        this.corpSel = (this.corpSel + 10) % 11;
                    }
                    return;
                }
            } else if (this.browseSite >= 0) {
                label4: if (i != 4) {
                    if (i != 0) {
                        if (i != 1) {
                            if (i != 6) {
                                label5: {
                                    if (i == 7) {
                                        break label5;
                                    }
                                    if (i != 5) {
                                        break label4;
                                    }
                                }
                                this.browseSite = -1;
                            } else {
                                int i3 = rpc.Browser.offerPart(this.browseSite, this.browsePage, this.levelYear(), this.db);
                                if (i3 < 0) {
                                    this.toastMsg("\u041d\u0430 \u044d\u0442\u043e\u0439 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0435 \u043f\u043e\u043a\u0443\u043f\u0430\u0442\u044c \u043d\u0435\u0447\u0435\u0433\u043e");
                                } else {
                                    this.buyFromBrowser(i3);
                                }
                            }
                        } else {
                            this.browseScroll = this.browseScroll + 2;
                        }
                    } else {
                        this.browseScroll = Math.max(0, this.browseScroll - 2);
                    }
                } else {
                    this.browseAction();
                }
                return;
            } else {
                int i4 = rpc.Browser.count(this.levelYear());
                if (i4 == 0) {
                    this.mode = 13;
                    return;
                }
                label2: if (i != 0) {
                    if (i != 1) {
                        if (i != 4) {
                            label3: {
                                if (i == 7) {
                                    break label3;
                                }
                                if (i != 5) {
                                    break label2;
                                }
                            }
                            this.mode = 13;
                        } else {
                            this.browseSite = rpc.Browser.nth(this.levelYear(), this.siteSel);
                            this.browsePage = 0;
                            this.browseScroll = 0;
                        }
                    } else {
                        this.siteSel = (this.siteSel + 1) % i4;
                    }
                } else {
                    this.siteSel = (this.siteSel + i4 - 1) % i4;
                }
                return;
            }
        } else {
            label0: if (i != 4) {
                if (i != 0) {
                    if (i != 1) {
                        label1: {
                            if (i == 7) {
                                break label1;
                            }
                            if (i != 5) {
                                break label0;
                            }
                        }
                        this.mode = 13;
                    } else {
                        this.shell.scrollDown(3);
                    }
                } else {
                    this.shell.scrollUp(3);
                }
            } else {
                this.shellCommand();
            }
            return;
        }
    }
    
    private int sysCpuPower() {
        rpc.Part a = this.slots[0];
        if (a == null) {
            return 0;
        }
        int i = (a.f[2] >= 1) ? a.f[2] : 1;
        int i0 = (a.f[4] >= 1) ? a.f[4] : 100;
        return a.f[1] * i * i0 / 10;
    }
    
    private int sysGpuLevel() {
        rpc.Part a = this.slots[6];
        return rpc.Games.gpuLevel(a != null && a.f[5] != 0 ? 1 : 0);
    }
    
    private void openGames() {
        this.gamesYear = this.levelYear();
        if (this.gamesYear < 1980) {
            this.gamesYear = 1980;
        }
        this.gamesRan = false;
        this.gamesSel = 0;
        this.gamesTop = 0;
        this.refillGames();
        this.mode = 36;
    }
    
    private void refillGames() {
        this.gamesList = rpc.Games.byYear(this.gamesYear);
        this.gamesSel = 0;
        this.gamesTop = 0;
        this.gamesRan = false;
    }
    
    private void runGameTest() {
        if (this.gamesList.length == 0) {
            return;
        }
        int[] a = rpc.Games.test(this.gamesList[this.gamesSel], this.sysCpuPower(), rpc.Score.totalRamKb(this.slots), this.sysGpuLevel());
        this.gamesFps = a[0];
        this.gamesCode = a[1];
        this.gamesRan = true;
        if (a[1] != 0) {
            rpc.Sound.error();
        } else {
            rpc.Sound.ok();
        }
    }
    
    public void powerOnPC() {
        int i = this.osIndex;
        label10: {
            label8: {
                label9: {
                    if (i < 0) {
                        break label9;
                    }
                    if (this.osIndex < rpc.Inst.all().length) {
                        break label8;
                    }
                }
                this.bootOsName = rpc.Os.name(this.levelYear());
                break label10;
            }
            this.bootOsName = rpc.Inst.all()[this.osIndex].name;
        }
        String s = this.bootOsName.toLowerCase();
        label1: if (s.indexOf("xp") < 0) {
            int i0 = s.indexOf("95");
            label6: {
                label7: {
                    if (i0 >= 0) {
                        break label7;
                    }
                    if (s.indexOf("98") >= 0) {
                        break label7;
                    }
                    if (s.indexOf("me") < 0) {
                        break label6;
                    }
                }
                this.bootOsType = 2;
                break label1;
            }
            int i1 = s.indexOf("7");
            label4: {
                label5: {
                    if (i1 >= 0) {
                        break label5;
                    }
                    if (s.indexOf("vista") < 0) {
                        break label4;
                    }
                }
                this.bootOsType = 3;
                break label1;
            }
            int i2 = s.indexOf("10");
            label2: {
                label3: {
                    if (i2 >= 0) {
                        break label3;
                    }
                    if (s.indexOf("11") >= 0) {
                        break label3;
                    }
                    if (s.indexOf("8") < 0) {
                        break label2;
                    }
                }
                this.bootOsType = 4;
                break label1;
            }
            int i3 = s.indexOf("linux");
            label0: {
                if (i3 >= 0) {
                    break label0;
                }
                if (s.indexOf("ubuntu") >= 0) {
                    break label0;
                }
                if (s.indexOf("mandrake") >= 0) {
                    break label0;
                }
                this.bootOsType = 0;
                break label1;
            }
            this.bootOsType = 5;
        } else {
            this.bootOsType = 1;
        }
        rpc.Part a = rpc.Score.firstDisk(this.slots);
        int i4 = (a == null) ? 36 : (a.cat != 7) ? (a.cat != 5) ? (a.cat != 6) ? 36 : 55 : (a.f[2] < 7200) ? 42 : 30 : 18;
        if (this.slots[0] != null && this.slots[0].f[1] >= 2000) {
            i4 = Math.max(12, i4 - 10);
        }
        this.bootTotalFrames = i4;
        this.bootFrame = 0;
        this.mode = 43;
        rpc.Sound.boot();
    }
    
    private void paintBootAnim() {
        this.bootFrame = this.bootFrame + 1;
        this.g.setColor(0);
        this.g.fillRect(0, 0, this.W, this.H);
        int i = this.H / 2;
        int i0 = this.W / 2;
        if (this.bootOsType != 1) {
            if (this.bootOsType != 2) {
                if (this.bootOsType != 3) {
                    if (this.bootOsType != 4) {
                        if (this.bootOsType != 5) {
                            this.g.setColor(330760);
                            this.g.fillRect(0, 0, this.W, this.H);
                            this.g.setColor(3859316);
                            this.g.drawText("Phoenix / Award Modular BIOS v4.51PG", 6, 10, 0);
                            this.g.drawText(new StringBuffer("CPU: ").append((this.slots[0] == null) ? "8086/Pentium" : this.slots[0].name).toString(), 6, 22, 0);
                            int i1 = rpc.Score.totalRamKb(this.slots);
                            this.g.drawText(new StringBuffer("Memory Testing : ").append(Math.min(i1, this.bootFrame * i1 / Math.max(1, this.bootTotalFrames - 5))).append("K OK").toString(), 6, 34, 0);
                            if (this.bootFrame > 10) {
                                this.g.drawText(new StringBuffer("Primary Master : ").append((rpc.Score.firstDisk(this.slots) == null) ? "HDD OK" : rpc.Score.firstDisk(this.slots).name).toString(), 6, 48, 0);
                            }
                            if (this.bootFrame > 18) {
                                this.g.setColor(16777215);
                                this.g.drawText(new StringBuffer("Starting ").append(this.bootOsName).append("...").toString(), 6, 64, 0);
                                this.g.drawText("HIMEM: DOS XMS Driver, Version 3.10", 6, 76, 0);
                                this.g.drawText("C:\\>_", 6, 90, 0);
                            }
                        } else {
                            this.g.setColor(0);
                            this.g.fillRect(0, 0, this.W, this.H);
                            this.g.setColor(16763150);
                            this.g.drawText("Linux Boot Loader", 8, 12, 1);
                            this.g.setColor(3859316);
                            int i2 = Math.min(10, this.bootFrame / 2);
                            String[] a = new String[10];
                            a[0] = "[  0.000000] Linux kernel boot on x86_64";
                            a[1] = new StringBuffer("[  0.040000] CPU0: ").append((this.slots[0] == null) ? "CPU OK" : this.slots[0].name).toString();
                            a[2] = new StringBuffer("[  0.080000] Memory: ").append(rpc.Score.totalRamKb(this.slots) / 1024).append("MB available").toString();
                            a[3] = "[  0.120000] PCI: Probing PCI hardware buses";
                            a[4] = new StringBuffer("[  0.180000] IDE/SATA: ").append((rpc.Score.firstDisk(this.slots) == null) ? "Storage OK" : rpc.Score.firstDisk(this.slots).name).toString();
                            a[5] = "[  0.220000] EXT4-fs: mounted root filesystem";
                            a[6] = "[  0.280000] Starting System Services... [ OK ]";
                            a[7] = "[  0.340000] Starting Display Manager... [ OK ]";
                            a[8] = new StringBuffer("[  0.400000] Welcome to ").append(this.bootOsName).append("!").toString();
                            a[9] = "[  0.450000] System Ready.";
                            int i3 = 0;
                            while(i3 < i2) {
                                {
                                    if (i3 < a.length) {
                                        this.g.drawText(a[i3], 8, 30 + i3 * (this.FH0 + 2), 0);
                                        i3 = i3 + 1;
                                        continue;
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        this.g.setColor(0);
                        this.g.fillRect(0, 0, this.W, this.H);
                        int i4 = i0 - 18;
                        int i5 = i - 40;
                        this.g.setColor(44527);
                        this.g.fillRect(i4, i5, 16, 16);
                        this.g.fillRect(i4 + 20, i5, 16, 16);
                        this.g.fillRect(i4, i5 + 20, 16, 16);
                        this.g.fillRect(i4 + 20, i5 + 20, 16, 16);
                        int i6 = 0;
                        while(i6 < 6) {
                            double d = (double)(this.bootFrame * 14 + i6 * 30) * 3.141592653589793 / 180.0;
                            int i7 = (int)((double)i0 + Math.cos(d) * 16.0);
                            int i8 = (int)((double)(i + 24) + Math.sin(d) * 16.0);
                            this.g.setColor(16777215);
                            this.g.fillRect(i7 - 1, i8 - 1, 3, 3);
                            i6 = i6 + 1;
                        }
                    }
                } else {
                    this.g.setColor(0);
                    this.g.fillRect(0, 0, this.W, this.H);
                    double d0 = (double)(this.bootFrame * 12 % 360) * 3.141592653589793 / 180.0;
                    int i9 = (int)((double)i0 + Math.cos(d0) * (double)14);
                    int i10 = (int)((double)(i - 20) + Math.sin(d0) * (double)14);
                    int i11 = (int)((double)i0 + Math.cos(d0 + 1.5707963267948966) * (double)14);
                    int i12 = (int)((double)(i - 20) + Math.sin(d0 + 1.5707963267948966) * (double)14);
                    int i13 = (int)((double)i0 + Math.cos(d0 + 3.141592653589793) * (double)14);
                    int i14 = (int)((double)(i - 20) + Math.sin(d0 + 3.141592653589793) * (double)14);
                    int i15 = (int)((double)i0 + Math.cos(d0 + 4.71238898038469) * (double)14);
                    int i16 = (int)((double)(i - 20) + Math.sin(d0 + 4.71238898038469) * (double)14);
                    this.g.setColor(15539236);
                    this.g.fillRect(i9 - 4, i10 - 4, 8, 8);
                    this.g.setColor(2273612);
                    this.g.fillRect(i11 - 4, i12 - 4, 8, 8);
                    this.g.setColor(41704);
                    this.g.fillRect(i13 - 4, i14 - 4, 8, 8);
                    this.g.setColor(16763150);
                    this.g.fillRect(i15 - 4, i16 - 4, 8, 8);
                    this.g.setColor(16777215);
                    this.g.drawText("\u0417\u0430\u043f\u0443\u0441\u043a Windows", i0 - 48, i + 24, 1);
                }
            } else {
                this.g.setColor(32896);
                this.g.fillRect(0, 0, this.W, this.H);
                this.g.setColor(128);
                this.g.fillRect(0, 0, this.W, 26);
                this.g.setColor(16777215);
                this.g.drawText(new StringBuffer("Microsoft ").append(this.bootOsName).toString(), 10, 6, 1);
                this.g.setColor(16777215);
                this.g.fillRoundRect(i0 - 60, i - 35, 120, 60, 6);
                this.g.setColor(0);
                this.g.drawRoundRect(i0 - 60, i - 35, 120, 60, 6);
                this.g.setColor(128);
                this.g.drawText("Windows", i0 - 38, i - 24, 2);
                int i17 = this.W - 24;
                int i18 = this.H - 32;
                this.g.setColor(0);
                this.g.drawRect(12, i18, i17, 10);
                int i19 = this.bootFrame * 6 % i17;
                this.g.setColor(128);
                this.g.fillRect(13, i18 + 1, i17 - 2, 8);
                this.g.setColor(3859316);
                this.g.fillRect(13 + i19 % (i17 - 20), i18 + 1, 18, 8);
            }
        } else {
            this.g.setColor(328968);
            this.g.fillRect(0, 0, this.W, this.H);
            this.g.setColor(16744231);
            this.g.drawText("Microsoft", i0 - 44, i - 54, 0);
            this.g.setColor(16777215);
            this.g.drawText("Windows", i0 - 44, i - 40, 2);
            this.g.setColor(16729344);
            this.g.drawText("XP", i0 + 52, i - 46, 1);
            int i20 = i0 - 74;
            int i21 = i - 48;
            this.g.setColor(15539236);
            this.g.fillRect(i20, i21, 8, 8);
            this.g.setColor(2273612);
            this.g.fillRect(i20 + 10, i21, 8, 8);
            this.g.setColor(41704);
            this.g.fillRect(i20, i21 + 10, 8, 8);
            this.g.setColor(16763150);
            this.g.fillRect(i20 + 10, i21 + 10, 8, 8);
            int i22 = (this.W - 120) / 2;
            int i23 = i + 24;
            this.g.setColor(1780298);
            this.g.drawRoundRect(i22, i23, 120, 12, 4);
            int i24 = this.bootFrame * 5 % 96;
            this.g.setColor(41704);
            this.g.fillRect(i22 + 2 + i24, i23 + 2, 6, 8);
            this.g.fillRect(i22 + 10 + i24, i23 + 2, 6, 8);
            this.g.fillRect(i22 + 18 + i24, i23 + 2, 6, 8);
            this.g.setColor(6320256);
            this.g.drawText("\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 Microsoft Windows XP...", i0 - 80, i + 50, 0);
        }
        if (this.bootFrame >= this.bootTotalFrames) {
            this.pcPowered = true;
            rpc.Sound.ok();
            this.mode = 13;
        }
        this.footer("OK: \u043f\u0440\u043e\u043f\u0443\u0441\u0442\u0438\u0442\u044c \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0443", "1: \u0432\u044b\u043a\u043b\u044e\u0447\u0438\u0442\u044c");
    }
    
    private void paintRepairOrders() {
        this.initRepairOrders();
        this.header("\u0417\u0430\u043a\u0430\u0437\u044b \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u0432", new StringBuffer("\u0440\u0435\u043f\u0443\u0442\u0430\u0446\u0438\u044f XP: ").append(this.totalXp).toString());
        int i = this.FH1 + 14;
        int i0 = this.FH0 * 3 + 10;
        int i1 = (this.H - i - this.FH0 - 20) / i0;
        if (i1 < 1) {
            i1 = 1;
        }
        if (this.repairSel < this.repairTop) {
            this.repairTop = this.repairSel;
        }
        if (this.repairSel > this.repairTop + i1 - 1) {
            this.repairTop = this.repairSel - i1 + 1;
        }
        int i2 = 0;
        while(true) {
            String s = null;
            if (i2 < i1 && this.repairTop + i2 < this.repairOrders.length) {
                int i3 = this.repairTop + i2;
                rpc.Game$RepairOrder a = this.repairOrders[i3];
                int i4 = i + i2 * i0;
                boolean b = i3 == this.repairSel;
                this.g.setColor(b ? 2308170 : 1582389);
                this.g.fillRect(4, i4, this.W - 8, i0 - 2);
                if (b) {
                    this.g.setColor(3859316);
                    this.g.drawRect(4, i4, this.W - 8, i0 - 2);
                }
                this.g.setColor((a.done) ? 8559794 : b ? 3859316 : 14871281);
                this.g.drawText(this.fit(new StringBuffer(String.valueOf((Object)((a.done) ? "\u2713 " : ""))).append(a.client).append(": ").append(a.title).toString(), this.W - 16, 0), 8, i4 + 3, 0);
                this.g.setColor(8559794);
                this.g.drawText(this.fit(a.desc, this.W - 16, 0), 8, i4 + this.FH0 + 4, 0);
                this.g.setColor((a.done) ? 3859316 : 16760380);
                String s0 = (a.done) ? new StringBuffer("\u0412\u042b\u041f\u041e\u041b\u041d\u0415\u041d\u041e (+").append(a.reward).append(" \u20bd)").toString() : new StringBuffer("\u041d\u0430\u0433\u0440\u0430\u0434\u0430: ").append(a.reward).append(" \u20bd  \u2022 +").append(a.xp).append(" XP").toString();
                this.g.drawText(s0, 8, i4 + this.FH0 * 2 + 5, 0);
                i2 = i2 + 1;
                continue;
            }
            rpc.Game$RepairOrder a0 = this.repairOrders[this.repairSel];
            label2: {
                label0: {
                    label1: {
                        if (a0 == null) {
                            break label1;
                        }
                        if (a0.done) {
                            break label0;
                        }
                    }
                    s = "OK: \u0432\u044b\u043f\u043e\u043b\u043d\u0438\u0442\u044c / \u0441\u0434\u0430\u0442\u044c";
                    break label2;
                }
                s = "OK: \u0441\u0434\u0430\u0442\u044c \u0437\u0430\u043a\u0430\u0437";
            }
            this.footer(s, "1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void actionRepairOrder() {
        this.initRepairOrders();
        if (this.repairSel >= 0 && this.repairSel < this.repairOrders.length) {
            boolean b = false;
            rpc.Game$RepairOrder a = this.repairOrders[this.repairSel];
            if (a.done) {
                this.toastMsg("\u042d\u0442\u043e\u0442 \u0437\u0430\u043a\u0430\u0437 \u0443\u0436\u0435 \u0441\u0434\u0430\u043d \u0438 \u043e\u043f\u043b\u0430\u0447\u0435\u043d!");
                return;
            }
            label3: if (a.type != 0) {
                if (a.type != 1) {
                    int i = a.type;
                    label7: {
                        if (i == 2) {
                            break label7;
                        }
                        if (a.type == 3) {
                            break label7;
                        }
                        if (a.type != 4) {
                            if (a.type != 5) {
                                b = false;
                                break label3;
                            } else {
                                rpc.Part a0 = this.slots[6];
                                label6: {
                                    label4: {
                                        label5: {
                                            if (a0 != null) {
                                                break label5;
                                            }
                                            if (this.slots[2] == null) {
                                                break label4;
                                            }
                                        }
                                        b = true;
                                        break label6;
                                    }
                                    b = false;
                                }
                                if (b) {
                                    break label3;
                                }
                                this.toastMsg("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0432\u0438\u0434\u0435\u043e\u043a\u0430\u0440\u0442\u0443 \u0432 \u0441\u043b\u043e\u0442!");
                                break label3;
                            }
                        } else {
                            b = rpc.Score.firstDisk(this.slots) != null;
                            if (b) {
                                break label3;
                            }
                            this.toastMsg("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0431\u044b\u0441\u0442\u0440\u044b\u0439 \u043d\u0430\u043a\u043e\u043f\u0438\u0442\u0435\u043b\u044c (SSD/HDD)!");
                            break label3;
                        }
                    }
                    rpc.Part a1 = this.slots[0];
                    label2: {
                        label0: {
                            label1: {
                                if (a1 == null) {
                                    break label1;
                                }
                                if (this.slots[1] == null) {
                                    break label1;
                                }
                                if (this.slots[3] == null) {
                                    break label1;
                                }
                                if (this.slots[8] != null) {
                                    break label0;
                                }
                            }
                            b = false;
                            break label2;
                        }
                        b = true;
                    }
                    if (!b) {
                        this.toastMsg("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u0432\u0441\u0435 \u0440\u0430\u0431\u043e\u0447\u0438\u0435 \u0434\u0435\u0442\u0430\u043b\u0438 (CPU, \u043f\u043b\u0430\u0442\u0430, \u043f\u0430\u043c\u044f\u0442\u044c, \u0411\u041f)!");
                    }
                } else {
                    b = this.osIndex >= 0;
                    if (!b) {
                        this.toastMsg("\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u0435 \u043e\u043f\u0435\u0440\u0430\u0446\u0438\u043e\u043d\u043d\u0443\u044e \u0441\u0438\u0441\u0442\u0435\u043c\u0443 \u043d\u0430 \u041f\u041a!");
                    }
                }
            } else {
                b = true;
                int i0 = 0;
                while(i0 < this.slots.length) {
                    if (this.slots[i0] != null && this.slots[i0].dust > 20) {
                        b = false;
                    }
                    i0 = i0 + 1;
                }
                if (!b) {
                    this.toastMsg("\u0421\u043d\u0430\u0447\u0430\u043b\u0430 \u043f\u0440\u043e\u0447\u0438\u0441\u0442\u0438\u0442\u0435 \u043a\u0443\u043b\u0435\u0440 \u0438 \u0434\u0435\u0442\u0430\u043b\u0438 \u043e\u0442 \u043f\u044b\u043b\u0438!");
                }
            }
            if (b) {
                a.done = true;
                this.cash = this.cash + a.reward;
                this.totalXp = this.totalXp + a.xp;
                rpc.Sound.ok();
                this.toastMsg(new StringBuffer("\u0417\u0430\u043a\u0430\u0437 \u0441\u0434\u0430\u043d! \u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043e +").append(a.reward).append(" \u20bd \u0438 +").append(a.xp).append(" XP!").toString());
                this.saveProgress();
            }
            return;
        }
    }
    
    private void paintGames() {
        StringBuffer a = new StringBuffer(String.valueOf(this.gamesYear)).append(" \u2022 ").append(this.gamesList.length).append(" \u0448\u0442.");
        if (this.gamesYear < 1980) {
        }
        this.header("\u0418\u0433\u0440\u044b \u0438 \u0442\u0435\u0441\u0442\u044b", a.append("").toString());
        int i = this.FH1 + 12;
        this.g.setColor(8297134);
        this.g.drawText(new StringBuffer("\u043f\u0440\u043e\u0446\u0435\u0441\u0441\u043e\u0440 ").append(this.sysCpuPower()).append(" \u2022 \u043f\u0430\u043c\u044f\u0442\u044c ").append(rpc.Games.kb(rpc.Score.totalRamKb(this.slots))).append(" \u2022 \u0432\u0438\u0434\u0435\u043e \u0443\u0440.").append(this.sysGpuLevel()).toString(), 8, i, 0);
        int i0 = i + (this.FH0 + 8);
        if (this.gamesRan && this.gamesList.length > 0) {
            boolean b = this.gamesList[this.gamesSel] != 0;
            this.g.setColor((this.gamesCode != 0) ? 16739179 : 3793530);
            this.g.drawText(this.fit(rpc.Games.name(b ? 1 : 0), this.W - 16, 0), 8, i0, 0);
            this.g.setColor(15003895);
            this.g.drawText(this.fit(rpc.Games.why(b ? 1 : 0, this.sysCpuPower(), rpc.Score.totalRamKb(this.slots), this.sysGpuLevel()), this.W - 16, 0), 8, i0 + this.FH0 + 4, 0);
            this.g.setColor(8297134);
            this.g.drawText(new StringBuffer("\u043d\u0443\u0436\u043d\u043e: ").append(rpc.Games.needMhz(b ? 1 : 0)).append(" \u041c\u0413\u0446 \u2022 ").append(rpc.Games.kb(rpc.Games.needRam(b ? 1 : 0))).append(" \u2022 \u0432\u0438\u0434\u0435\u043e ").append(rpc.Games.needGpu(b ? 1 : 0)).toString(), 8, i0 + this.FH0 * 2 + 6, 0);
            i0 = i0 + (this.FH0 * 3 + 12);
        }
        int i1 = this.FH0 + 8;
        int i2 = (this.H - i0 - this.FH0 - 26) / i1;
        if (i2 < 1) {
            i2 = 1;
        }
        if (this.gamesSel < this.gamesTop) {
            this.gamesTop = this.gamesSel;
        }
        if (this.gamesSel > this.gamesTop + i2 - 1) {
            this.gamesTop = this.gamesSel - i2 + 1;
        }
        int i3 = 0;
        while(true) {
            if (i3 < i2 && this.gamesTop + i3 < this.gamesList.length) {
                boolean b0 = this.gamesList[this.gamesTop + i3] != 0;
                boolean b1 = this.gamesTop + i3 == this.gamesSel;
                int i4 = i0 + i3 * i1;
                if (b1) {
                    this.g.setColor(2308170);
                    this.g.fillRect(3, i4 - 1, this.W - 6, i1 - 1);
                }
                this.g.setColor(b1 ? 3793530 : 15003895);
                this.g.drawText(this.fit(rpc.Games.line(b0 ? 1 : 0), this.W - 90, 0), 7, i4, 0);
                this.g.setColor(b1 ? 16763196 : 8297134);
                this.g.drawText(new StringBuffer().append(rpc.Games.needMhz(b0 ? 1 : 0)).append(" \u041c\u0413\u0446").toString(), this.W - 66, i4, 0);
                i3 = i3 + 1;
                continue;
            }
            this.footer("OK: \u043f\u0440\u043e\u0433\u043d\u0430\u0442\u044c", "\u2190\u2192 \u0433\u043e\u0434, 1: \u043d\u0430\u0437\u0430\u0434");
            return;
        }
    }
    
    private void paintExtra() {
        if (this.mode != 29) {
            if (this.mode != 30) {
                if (this.mode != 31) {
                    if (this.mode != 32) {
                        if (this.mode != 33) {
                            if (this.mode != 34) {
                                if (this.mode != 35) {
                                    if (this.mode != 36) {
                                        if (this.mode != 43) {
                                            if (this.mode == 44) {
                                                this.paintRepairOrders();
                                            }
                                        } else {
                                            this.paintBootAnim();
                                        }
                                    } else {
                                        this.paintGames();
                                    }
                                } else {
                                    this.paintProds();
                                }
                            } else {
                                this.paintSoft();
                            }
                        } else {
                            this.paintCorpOs();
                        }
                    } else {
                        this.paintChip();
                    }
                } else {
                    this.paintCorp();
                }
            } else {
                this.paintBrowse();
            }
        } else {
            this.paintShell();
        }
    }
}
