import javax.microedition.lcdui.Canvas;

import psx.KeyMap;

/**
 * Desktop tests for the key translation layer.
 *
 * <p>The point of {@link KeyMap} is that one physical key can arrive in five
 * different ways depending on the handset: as a MIDP {@code KEY_NUM*} code, as
 * the raw ASCII digit, as a game action ({@code UP/FIRE/...}), as an arrow
 * pseudo code ({@code KEY_UP}) or as a soft key. Every one of those paths is
 * checked here, plus the three pad layouts.</p>
 */
public final class KeyMapTest {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        digits();
        asciiDigits();
        gameActions();
        arrowKeys();
        softKeys();
        layouts();
        padBits();
        names();

        System.out.println();
        if (failed == 0) {
            System.out.println("ALL KEYMAP TESTS PASSED (" + passed + " checks)");
        } else {
            System.out.println("KEYMAP TESTS FAILED: " + failed + " of " + (passed + failed));
            System.exit(1);
        }
    }

    /* ------------------------------------------------------------------ */

    private static void digits() {
        int L = KeyMap.LAYOUT_STANDARD;
        same(KeyMap.UP, KeyMap.action(Canvas.KEY_NUM2, 0, L), "NUM2 -> up");
        same(KeyMap.DOWN, KeyMap.action(Canvas.KEY_NUM8, 0, L), "NUM8 -> down");
        same(KeyMap.LEFT, KeyMap.action(Canvas.KEY_NUM4, 0, L), "NUM4 -> left");
        same(KeyMap.RIGHT, KeyMap.action(Canvas.KEY_NUM6, 0, L), "NUM6 -> right");
        same(KeyMap.CROSS, KeyMap.action(Canvas.KEY_NUM5, 0, L), "NUM5 -> cross");
        same(KeyMap.SQUARE, KeyMap.action(Canvas.KEY_NUM1, 0, L), "NUM1 -> square");
        same(KeyMap.TRIANGLE, KeyMap.action(Canvas.KEY_NUM3, 0, L), "NUM3 -> triangle");
        same(KeyMap.CIRCLE, KeyMap.action(Canvas.KEY_NUM0, 0, L), "NUM0 -> circle");
        same(KeyMap.L1, KeyMap.action(Canvas.KEY_NUM7, 0, L), "NUM7 -> L1");
        same(KeyMap.R1, KeyMap.action(Canvas.KEY_NUM9, 0, L), "NUM9 -> R1");
        same(KeyMap.START, KeyMap.action(Canvas.KEY_STAR, 0, L), "STAR -> start");
        same(KeyMap.SELECT, KeyMap.action(Canvas.KEY_POUND, 0, L), "POUND -> select");
        same(KeyMap.NONE, KeyMap.action(0, 0, L), "unknown code -> none");
    }

    private static void asciiDigits() {
        int L = KeyMap.LAYOUT_STANDARD;
        for (int d = 0; d <= 9; d++) {
            int act = KeyMap.action(48 + d, 0, L);
            same(expectForDigit(d), act, "ascii '" + d + "'");
        }
    }

    private static int expectForDigit(int d) {
        switch (d) {
            case 0: return KeyMap.CIRCLE;
            case 1: return KeyMap.SQUARE;
            case 2: return KeyMap.UP;
            case 3: return KeyMap.TRIANGLE;
            case 4: return KeyMap.LEFT;
            case 5: return KeyMap.CROSS;
            case 6: return KeyMap.RIGHT;
            case 7: return KeyMap.L1;
            case 8: return KeyMap.DOWN;
            case 9: return KeyMap.R1;
            default: return KeyMap.NONE;
        }
    }

    /** A joystick that only reports game actions (Nokia, Sony Ericsson). */
    private static void gameActions() {
        int L = KeyMap.LAYOUT_STANDARD;
        same(KeyMap.UP, KeyMap.action(-1, Canvas.UP, L), "action UP");
        same(KeyMap.DOWN, KeyMap.action(-1, Canvas.DOWN, L), "action DOWN");
        same(KeyMap.LEFT, KeyMap.action(-1, Canvas.LEFT, L), "action LEFT");
        same(KeyMap.RIGHT, KeyMap.action(-1, Canvas.RIGHT, L), "action RIGHT");
        same(KeyMap.CROSS, KeyMap.action(-1, Canvas.FIRE, L), "action FIRE");
        same(KeyMap.TRIANGLE, KeyMap.action(-1, Canvas.GAME_A, L), "action GAME_A");
        same(KeyMap.CIRCLE, KeyMap.action(-1, Canvas.GAME_B, L), "action GAME_B");
        same(KeyMap.SQUARE, KeyMap.action(-1, Canvas.GAME_C, L), "action GAME_C");
        same(KeyMap.L1, KeyMap.action(-1, Canvas.GAME_D, L), "action GAME_D");
    }

    /** Phones whose joystick reports the KEY_UP..KEY_FIRE pseudo codes only. */
    private static void arrowKeys() {
        int L = KeyMap.LAYOUT_STANDARD;
        same(KeyMap.UP, KeyMap.action(Canvas.KEY_UP, 0, L), "KEY_UP");
        same(KeyMap.DOWN, KeyMap.action(Canvas.KEY_DOWN, 0, L), "KEY_DOWN");
        same(KeyMap.LEFT, KeyMap.action(Canvas.KEY_LEFT, 0, L), "KEY_LEFT");
        same(KeyMap.RIGHT, KeyMap.action(Canvas.KEY_RIGHT, 0, L), "KEY_RIGHT");
        same(KeyMap.CROSS, KeyMap.action(Canvas.KEY_FIRE, 0, L), "KEY_FIRE");
    }

    /** Soft keys must work in every layout, otherwise the menu is unreachable. */
    private static void softKeys() {
        int[] layouts = { KeyMap.LAYOUT_STANDARD, KeyMap.LAYOUT_SWAPPED, KeyMap.LAYOUT_DIGITS };
        for (int i = 0; i < layouts.length; i++) {
            same(KeyMap.MENU, KeyMap.action(Canvas.KEY_SOFTKEY1, 0, layouts[i]),
                    "softkey1 in layout " + i);
            same(KeyMap.SOUND, KeyMap.action(Canvas.KEY_SOFTKEY2, 0, layouts[i]),
                    "softkey2 in layout " + i);
            same(KeyMap.OK, KeyMap.action(Canvas.KEY_SOFTKEY3, 0, layouts[i]),
                    "softkey3 in layout " + i);
        }
    }

    private static void layouts() {
        /* standard: cross stays cross, circle stays circle */
        same(KeyMap.CROSS, KeyMap.action(Canvas.KEY_NUM5, 0, KeyMap.LAYOUT_STANDARD),
                "standard NUM5");
        same(KeyMap.CIRCLE, KeyMap.action(Canvas.KEY_NUM0, 0, KeyMap.LAYOUT_STANDARD),
                "standard NUM0");

        /* swapped: cross and circle trade places for the game pad */
        same(KeyMap.CIRCLE, KeyMap.action(Canvas.KEY_NUM5, 0, KeyMap.LAYOUT_SWAPPED),
                "swapped NUM5 -> circle");
        same(KeyMap.CROSS, KeyMap.action(Canvas.KEY_NUM0, 0, KeyMap.LAYOUT_SWAPPED),
                "swapped NUM0 -> cross");
        same(KeyMap.UP, KeyMap.action(Canvas.KEY_NUM2, 0, KeyMap.LAYOUT_SWAPPED),
                "swapped NUM2 still up");

        /* digits only: the numeric block keeps working, the joystick is off */
        same(KeyMap.UP, KeyMap.action(Canvas.KEY_NUM2, 0, KeyMap.LAYOUT_DIGITS),
                "digits NUM2");
        same(KeyMap.NONE, KeyMap.action(Canvas.KEY_UP, Canvas.UP, KeyMap.LAYOUT_DIGITS),
                "digits ignores joystick");

        check(KeyMap.layoutName(KeyMap.LAYOUT_STANDARD).length() > 3, "layout name standard");
        check(KeyMap.layoutName(KeyMap.LAYOUT_SWAPPED).length() > 3, "layout name swapped");
        check(KeyMap.layoutName(KeyMap.LAYOUT_DIGITS).length() > 3, "layout name digits");
    }

    /** Bit positions must match the SIO0 pad shift register. */
    private static void padBits() {
        same(0, KeyMap.padBit(KeyMap.SELECT), "bit select");
        same(3, KeyMap.padBit(KeyMap.START), "bit start");
        same(4, KeyMap.padBit(KeyMap.UP), "bit up");
        same(5, KeyMap.padBit(KeyMap.RIGHT), "bit right");
        same(6, KeyMap.padBit(KeyMap.DOWN), "bit down");
        same(7, KeyMap.padBit(KeyMap.LEFT), "bit left");
        same(8, KeyMap.padBit(KeyMap.L2), "bit l2");
        same(9, KeyMap.padBit(KeyMap.R2), "bit r2");
        same(10, KeyMap.padBit(KeyMap.L1), "bit l1");
        same(11, KeyMap.padBit(KeyMap.R1), "bit r1");
        same(12, KeyMap.padBit(KeyMap.TRIANGLE), "bit triangle");
        same(13, KeyMap.padBit(KeyMap.CIRCLE), "bit circle");
        same(14, KeyMap.padBit(KeyMap.CROSS), "bit cross");
        same(15, KeyMap.padBit(KeyMap.SQUARE), "bit square");
        same(-1, KeyMap.padBit(KeyMap.MENU), "no bit for menu");
        same(-1, KeyMap.padBit(KeyMap.NONE), "no bit for none");
    }

    private static void names() {
        int[] all = { KeyMap.UP, KeyMap.DOWN, KeyMap.LEFT, KeyMap.RIGHT, KeyMap.CROSS,
                KeyMap.CIRCLE, KeyMap.SQUARE, KeyMap.TRIANGLE, KeyMap.L1, KeyMap.R1,
                KeyMap.L2, KeyMap.R2, KeyMap.START, KeyMap.SELECT };
        for (int i = 0; i < all.length; i++) {
            String n = KeyMap.name(all[i]);
            check(n != null && n.length() > 0, "name of action " + all[i]);
        }
        /* isFire / isBack drive the menus, they must not overlap */
        check(KeyMap.isFire(KeyMap.CROSS) && KeyMap.isFire(KeyMap.OK), "isFire cross/ok");
        check(!KeyMap.isFire(KeyMap.CIRCLE), "isFire not circle");
        check(KeyMap.isBack(KeyMap.CIRCLE), "isBack circle");
        check(!KeyMap.isBack(KeyMap.CROSS), "isBack not cross");
        check(KeyMap.isDirection(KeyMap.LEFT) && !KeyMap.isDirection(KeyMap.CROSS),
                "isDirection");
    }

    /* ------------------------------------------------------------------ */

    private static void same(int expected, int actual, String what) {
        check(expected == actual, what + " (expected " + expected + ", got " + actual + ")");
    }

    private static void check(boolean ok, String what) {
        if (ok) {
            passed++;
            System.out.println("PASS " + what);
        } else {
            failed++;
            System.out.println("FAIL " + what);
        }
    }
}
