package psx;

import javax.microedition.lcdui.Canvas;

/**
 * Translates raw handset key codes into emulator actions.
 *
 * <p>There is no portable way to read a game pad on MIDP. Depending on the
 * firmware a key arrives as</p>
 *
 * <ul>
 *   <li>a {@link Canvas#getGameAction game action} (UP/DOWN/LEFT/RIGHT/FIRE),</li>
 *   <li>one of the {@code Canvas.KEY_*} pseudo codes ({@code KEY_NUM2} for up, ...),</li>
 *   <li>a raw digit (50 for '2' on most Chinese MTK phones),</li>
 *   <li>or a vendor specific negative/odd code that maps to nothing at all.</li>
 * </ul>
 *
 * <p>This class accepts all of them at once, so a single build works on a
 * Nokia, a Sony Ericsson, an MTK "China phone" and a J2ME emulator without any
 * configuration. The only option a user may need is swapping Cross/Circle
 * (Japanese layout) or forcing the digit mapping when a phone reports a bogus
 * game action for its joystick.</p>
 *
 * <p>Everything is static, allocation free and CLDC 1.1 safe.</p>
 */
public final class KeyMap {

    /* ---- semantic actions ------------------------------------------------ */
    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;
    public static final int CROSS = 5;
    public static final int CIRCLE = 6;
    public static final int SQUARE = 7;
    public static final int TRIANGLE = 8;
    public static final int L1 = 9;
    public static final int R1 = 10;
    public static final int L2 = 11;
    public static final int R2 = 12;
    public static final int START = 13;
    public static final int SELECT = 14;
    /** Left soft key: opens the in-game menu. */
    public static final int MENU = 15;
    /** Right soft key: toggles sound. */
    public static final int SOUND = 16;
    /** Numeric "OK"/"fire" alias used by list screens. */
    public static final int OK = 17;

    /* ---- layouts --------------------------------------------------------- */
    /** Everything: actions, pseudo keys, digits, vendor codes. */
    public static final int LAYOUT_STANDARD = 0;
    /** Standard, but Cross and Circle are swapped (Japanese button layout). */
    public static final int LAYOUT_SWAPPED = 1;
    /** Only the digit block is used (phones that report a broken game action). */
    public static final int LAYOUT_DIGITS = 2;

    private KeyMap() { }

    /* ---- pad bits -------------------------------------------------------- */
    /* Index order of the digital pad shift register (1 = pressed, active low
       in the 16 bit word: bit 4 = up, bit 5 = right, ...). */
    public static final int BIT_SELECT = 0;
    public static final int BIT_START = 3;
    public static final int BIT_UP = 4;
    public static final int BIT_RIGHT = 5;
    public static final int BIT_DOWN = 6;
    public static final int BIT_LEFT = 7;
    public static final int BIT_L2 = 8;
    public static final int BIT_R2 = 9;
    public static final int BIT_L1 = 10;
    public static final int BIT_R1 = 11;
    public static final int BIT_TRIANGLE = 12;
    public static final int BIT_CIRCLE = 13;
    public static final int BIT_CROSS = 14;
    public static final int BIT_SQUARE = 15;

    /** @return the pad shift register bit of an action, or -1. */
    public static int padBit(int action) {
        switch (action) {
            case UP: return BIT_UP;
            case DOWN: return BIT_DOWN;
            case LEFT: return BIT_LEFT;
            case RIGHT: return BIT_RIGHT;
            case CROSS: return BIT_CROSS;
            case CIRCLE: return BIT_CIRCLE;
            case SQUARE: return BIT_SQUARE;
            case TRIANGLE: return BIT_TRIANGLE;
            case L1: return BIT_L1;
            case R1: return BIT_R1;
            case L2: return BIT_L2;
            case R2: return BIT_R2;
            case START: return BIT_START;
            case SELECT: return BIT_SELECT;
            default: return -1;
        }
    }

    /** Human readable name (used by the help and diagnostics screens). */
    public static String name(int action) {
        switch (action) {
            case UP: return "вверх";
            case DOWN: return "вниз";
            case LEFT: return "влево";
            case RIGHT: return "вправо";
            case CROSS: return "крест";
            case CIRCLE: return "круг";
            case SQUARE: return "квадрат";
            case TRIANGLE: return "треугольник";
            case L1: return "L1";
            case R1: return "R1";
            case L2: return "L2";
            case R2: return "R2";
            case START: return "старт";
            case SELECT: return "селект";
            case MENU: return "меню";
            case SOUND: return "звук";
            case OK: return "ок";
            default: return "-";
        }
    }

    /* ---- the mapping ----------------------------------------------------- */

    /**
     * Converts a key event into an emulator action.
     *
     * @param keyCode   raw code from {@code keyPressed()}/{@code keyReleased()}
     * @param action    value of {@code getGameAction(keyCode)} (0 when unknown)
     * @param layout    {@link #LAYOUT_STANDARD}, {@link #LAYOUT_SWAPPED} or
     *                  {@link #LAYOUT_DIGITS}
     * @return one of the action constants, or {@link #NONE}
     */
    public static int action(int keyCode, int action, int layout) {
        /* Soft keys are checked first and never suppressed by a layout: on a
           phone they are the only way into the pause menu. */
        int a = softAction(keyCode);
        if (a == NONE) a = digitAction(keyCode);
        if (a == NONE && layout != LAYOUT_DIGITS) a = actionOf(keyCode, action);
        if (layout == LAYOUT_SWAPPED) {
            if (a == CROSS) a = CIRCLE;
            else if (a == CIRCLE) a = CROSS;
        }
        return a;
    }

    /** Maps the numeric key block, which works on virtually every handset. */
    private static int digitAction(int keyCode) {
        switch (keyCode) {
            case Canvas.KEY_NUM2: return UP;
            case Canvas.KEY_NUM8: return DOWN;
            case Canvas.KEY_NUM4: return LEFT;
            case Canvas.KEY_NUM6: return RIGHT;
            case Canvas.KEY_NUM5: return CROSS;
            case Canvas.KEY_NUM1: return SQUARE;
            case Canvas.KEY_NUM3: return TRIANGLE;
            case Canvas.KEY_NUM0: return CIRCLE;
            case Canvas.KEY_NUM7: return L1;
            case Canvas.KEY_NUM9: return R1;
            case Canvas.KEY_STAR: return START;
            case Canvas.KEY_POUND: return SELECT;
            default: break;
        }
        /* Raw ASCII digits: some firmware reports '2' (50) instead of KEY_NUM2.
           The Canvas.KEY_NUM* constants already equal these values on MIDP,
           but a few vendors send the ASCII code of other digits. */
        if (keyCode >= 48 && keyCode <= 57) {
            switch (keyCode - 48) {
                case 2: return UP;
                case 8: return DOWN;
                case 4: return LEFT;
                case 6: return RIGHT;
                case 5: return CROSS;
                case 1: return SQUARE;
                case 3: return TRIANGLE;
                case 0: return CIRCLE;
                case 7: return L1;
                case 9: return R1;
                default: break;
            }
        }
        return NONE;
    }

    /**
     * Left / middle / right soft key.
     *
     * <p>In MIDP 2.0 the soft keys share their codes with {@code GAME_A..C}
     * ({@code -6..-8}), so a code arrives without telling which key it was.
     * The soft keys win, because without them the pause menu of a phone that
     * has no other free button would be unreachable. {@code KEY_FIRE} (-5)
     * is deliberately not listed here: it is a real fire button on the phones
     * that report it and it must reach the pad.</p>
     */
    private static int softAction(int keyCode) {
        if (keyCode == Canvas.KEY_SOFTKEY1) return MENU;
        if (keyCode == Canvas.KEY_SOFTKEY2) return SOUND;
        if (keyCode == Canvas.KEY_SOFTKEY3) return OK;
        return NONE;
    }

    /** Maps game actions and arrow pseudo codes. */
    private static int actionOf(int keyCode, int action) {
        switch (action) {
            case Canvas.UP: return UP;
            case Canvas.DOWN: return DOWN;
            case Canvas.LEFT: return LEFT;
            case Canvas.RIGHT: return RIGHT;
            case Canvas.FIRE: return CROSS;
            case Canvas.GAME_A: return TRIANGLE;
            case Canvas.GAME_B: return CIRCLE;
            case Canvas.GAME_C: return SQUARE;
            case Canvas.GAME_D: return L1;
            default: break;
        }
        /* Phones whose joystick only reports the KEY_UP..KEY_RIGHT codes. */
        if (keyCode == Canvas.KEY_UP) return UP;
        if (keyCode == Canvas.KEY_DOWN) return DOWN;
        if (keyCode == Canvas.KEY_LEFT) return LEFT;
        if (keyCode == Canvas.KEY_RIGHT) return RIGHT;
        if (keyCode == Canvas.KEY_FIRE) return CROSS;
        return NONE;
    }

    /* ---- helpers used by the UI ------------------------------------------ */

    public static boolean isUp(int a) { return a == UP; }

    public static boolean isDown(int a) { return a == DOWN; }

    public static boolean isLeft(int a) { return a == LEFT; }

    public static boolean isRight(int a) { return a == RIGHT; }

    public static boolean isDirection(int a) {
        return a == UP || a == DOWN || a == LEFT || a == RIGHT;
    }

    /** A key that confirms / activates in menus (5 / fire / OK). */
    public static boolean isFire(int a) {
        return a == CROSS || a == OK;
    }

    /** A key that goes one level up in menus (0 / "#" / left soft key). */
    public static boolean isBack(int a) {
        return a == CIRCLE || a == SELECT || a == MENU;
    }

    /**
     * Resolves a menu key that may arrive without any game action, e.g. an
     * 8-way joystick reported as raw digits.
     */
    public static int menuAction(int keyCode, int action, int layout) {
        int a = action(keyCode, action, layout);
        if (a == NONE && keyCode == Canvas.KEY_NUM0) return SELECT;
        return a;
    }

    /** Description of a layout, for the settings screen. */
    public static String layoutName(int layout) {
        switch (layout) {
            case LAYOUT_SWAPPED: return "крест/круг";
            case LAYOUT_DIGITS: return "только цифры";
            default: return "стандарт";
        }
    }
}
