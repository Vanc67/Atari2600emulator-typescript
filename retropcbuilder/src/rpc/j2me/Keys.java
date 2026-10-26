package rpc.j2me;

import javax.microedition.lcdui.Canvas;
import rpc.Game;

/**
 * Maps LCDUI key codes/game actions to the game's action ids.
 *
 * The 4.8 build only forwarded the d-pad, fire and the two soft keys; the
 * number keys 1..8 that the room screen advertised ("5: Попить чай", ...)
 * were never delivered, so those labels silently did nothing.  Here every
 * key of a phone keypad gets an action id of its own.
 */
final class Keys {

    private Keys() {
    }

    /** @return an action id for {@code Game.input()}, or -1 to ignore */
    static int action(int keyCode, int gameAction) {
        switch (gameAction) {
            case Canvas.UP:
                return Game.ACT_UP;
            case Canvas.DOWN:
                return Game.ACT_DOWN;
            case Canvas.LEFT:
                return Game.ACT_LEFT;
            case Canvas.RIGHT:
                return Game.ACT_RIGHT;
            case Canvas.FIRE:
                return Game.ACT_FIRE;
            case Canvas.GAME_A:
                return Game.ACT_SOFT1;
            case Canvas.GAME_B:
                return Game.ACT_SOFT2;
            default:
                break;
        }
        if (keyCode == Canvas.KEY_NUM0 || keyCode == Canvas.KEY_SOFTKEY3
                || keyCode == -8 /* KEY_CLEAR on SE/Nokia */) {
            return Game.ACT_BACK;
        }
        if (keyCode >= Canvas.KEY_NUM1 && keyCode <= Canvas.KEY_NUM9) {
            return Game.ACT_NUM1 + (keyCode - Canvas.KEY_NUM1);
        }
        if (keyCode == Canvas.KEY_STAR) {
            return Game.ACT_STAR;
        }
        if (keyCode == Canvas.KEY_POUND) {
            return Game.ACT_POUND;
        }
        return -1;
    }

    /** Human readable name, used by the help screen. */
    static String name(int action) {
        switch (action) {
            case Game.ACT_UP: return "UP";
            case Game.ACT_DOWN: return "DOWN";
            case Game.ACT_LEFT: return "LEFT";
            case Game.ACT_RIGHT: return "RIGHT";
            case Game.ACT_FIRE: return "FIRE";
            case Game.ACT_SOFT1: return "SOFT1";
            case Game.ACT_SOFT2: return "SOFT2";
            case Game.ACT_BACK: return "BACK";
            default:
                if (action >= Game.ACT_NUM1 && action <= Game.ACT_NUM9) {
                    return Integer.toString(action - Game.ACT_NUM1 + 1);
                }
                return "?";
        }
    }
}
