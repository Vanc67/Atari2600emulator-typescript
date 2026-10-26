package rpc.j2me;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import rpc.Game;
import rpc.Theme;

/**
 * The game screen: owns the {@link Game} instance, the animation loop and the
 * key routing.  Loading the 56k part catalogue takes a moment on a handset, so
 * it happens on a background thread behind a boot animation instead of
 * blocking the first paint.
 */
final class AppCanvas extends Canvas implements Runnable {

    private static final int LOOP_MS = 70;

    private final RetroPcMIDlet midlet;
    private final HostImpl host;
    final GfxImpl gfx = new GfxImpl();

    private Game game;
    private Thread loader;
    private Thread loop;
    private volatile boolean running;
    private volatile boolean loaded;
    private volatile String fatal;
    private int splashTick;
    private int lastKeyAction = -1;
    private int lastKeyTick;

    AppCanvas(RetroPcMIDlet midlet, HostImpl host) {
        this.midlet = midlet;
        this.host = host;
    }

    void start() {
        running = true;
        if (loop == null) {
            loop = new Thread(this);
            loop.start();
        }
        if (!loaded && fatal == null && loader == null) {
            loader = new Thread(new Runnable() {
                public void run() {
                    loadGame();
                }
            });
            loader.start();
        }
        repaint();
    }

    void stop() {
        running = false;
        loop = null;
    }

    void save() {
        if (game != null) {
            try {
                game.saveProgress();
            } catch (Throwable ignored) {
            }
        }
    }

    private void loadGame() {
        try {
            Game instance = new Game();
            instance.init(host, gfx);
            instance.loadProgress();
            game = instance;
            loaded = true;
        } catch (Throwable t) {
            fatal = t.toString();
        }
        repaint();
    }

    public void run() {
        while (running) {
            try {
                Thread.sleep(LOOP_MS);
            } catch (InterruptedException e) {
                return;
            }
            Game g = game;
            if (g != null && g.animating()) {
                repaint();
            }
        }
    }

    protected void paint(Graphics graphics) {
        int w = getWidth();
        int h = getHeight();
        gfx.bind(graphics, w, h);
        Game g = game;
        if (g != null) {
            g.paint();
            return;
        }
        drawSplash(graphics, w, h);
    }

    /** Boot animation shown while the catalogue is being parsed. */
    private void drawSplash(Graphics graphics, int w, int h) {
        graphics.setColor(Theme.BG);
        graphics.fillRect(0, 0, w, h);
        gfx.setColor(Theme.BG2);
        int top = h / 3;
        gfx.fillRoundRect(10, top, w - 20, 64, 6);
        gfx.setColor(Theme.LINE);
        gfx.drawRoundRect(10, top, w - 20, 64, 6);
        gfx.setColor(Theme.ACCENT);
        gfx.drawText("RETRO PC BUILDER", w / 2, top + 10, 2, rpc.Gfx.HCENTER);
        gfx.setColor(Theme.DIM);
        gfx.drawText("1979 - 2027", w / 2, top + 32, 1, rpc.Gfx.HCENTER);

        int bx = 24;
        int by = top + 78;
        int bw = w - 48;
        gfx.setColor(Theme.PANEL);
        gfx.fillRect(bx, by, bw, 8);
        int pct = fatal != null ? 100 : (splashTick * 7 % 100);
        gfx.setColor(fatal != null ? Theme.RED : Theme.ACCENT);
        gfx.fillRect(bx, by, bw * pct / 100, 8);
        gfx.setColor(Theme.LINE);
        gfx.drawRect(bx, by, bw, 8);
        gfx.setColor(Theme.DIM);
        if (fatal != null) {
            gfx.setColor(Theme.RED);
            gfx.drawText("ERR: " + fatal, 8, by + 18, 1);
            gfx.setColor(Theme.DIM);
            gfx.drawText("see README - save data / jar layout", 8, by + 32, 1);
        } else {
            gfx.drawText("\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043a\u0430\u0442\u0430\u043b\u043e\u0433\u0430...", w / 2, by + 18, 1, rpc.Gfx.HCENTER);
        }
        splashTick++;
        // a few "flying pixels" so the screen is alive even on a slow handset
        gfx.setColor(Theme.ACCENT2);
        for (int i = 0; i < 10; i++) {
            int px = (splashTick * 3 + i * 17) % (w - 8) + 4;
            int py = top + 100 + (i * 13 + splashTick) % 24;
            gfx.fillRect(px, py, 2, 2);
        }
    }

    protected void keyPressed(int keyCode) {
        Game g = game;
        if (g == null) {
            return;
        }
        int action = Keys.action(keyCode, getGameAction(keyCode));
        if (action < 0) {
            return;
        }
        // debounce accidental double delivery of the same key on some
        // Motorola/Nokia firmwares
        int now = splashTick;
        if (action == lastKeyAction && now - lastKeyTick < 1) {
            return;
        }
        lastKeyAction = action;
        lastKeyTick = now;
        try {
            g.input(action);
        } catch (Throwable t) {
            fatal = t.toString();
        }
        repaint();
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    protected void pointerPressed(int x, int y) {
        Game g = game;
        if (g != null) {
            g.pointer(x, y);
            repaint();
        }
    }
}
