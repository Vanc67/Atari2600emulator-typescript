package rdl.platform;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;
import rdl.core.World;

/** MIDP input, fixed-rate game loop and presentation of the software framebuffer. */
public final class LabCanvas extends GameCanvas implements Runnable {
    private static final int SCREEN_TITLE = 0;
    private static final int SCREEN_GAME = 1;
    private static final int SCREEN_HELP = 2;

    private static final int CMD_START = 1;
    private static final int CMD_PREVIOUS_MATERIAL = 1 << 1;
    private static final int CMD_NEXT_MATERIAL = 1 << 2;
    private static final int CMD_NEXT_TOOL = 1 << 3;
    private static final int CMD_PAUSE = 1 << 4;
    private static final int CMD_SPEED = 1 << 5;
    private static final int CMD_RESET = 1 << 6;
    private static final int CMD_NEXT_SCENE = 1 << 7;
    private static final int CMD_MENU = 1 << 8;

    private static final int TOOL_BUILD = 0;
    private static final int TOOL_ERASE = 1;
    private static final int TOOL_SPARK = 2;
    private static final int TOOL_BLAST = 3;

    private static final byte[] MATERIALS = {
        World.STEEL, World.CONCRETE, World.WOOD, World.GLASS,
        World.TNT, World.FUEL, World.WATER
    };

    private final World world = new World();
    private final PixelRenderer renderer = new PixelRenderer();
    private final Graphics graphics;

    private volatile boolean alive;
    private volatile boolean suspended;
    private Thread thread;

    private volatile int screen = SCREEN_TITLE;
    private volatile int commands;
    private int selectedScene = World.SCENE_TOWER;
    private int materialIndex;
    private int tool = TOOL_BUILD;
    private int cursorX = 16;
    private int cursorY = 11;
    private int speed = 1;
    private int animationTick;
    private int frame;
    private int moveDelay;
    private int fireRepeat = -1;
    private volatile boolean fireHeld;
    private boolean fireLock;
    private int fps;
    private int fpsFrames;
    private long fpsStarted;
    private int lastExplosionCount;
    private int slowFrames;
    private int fastFrames;

    public LabCanvas() {
        super(false);
        setFullScreenMode(true);
        graphics = getGraphics();
    }

    public void start() {
        if (alive) {
            return;
        }
        alive = true;
        suspended = false;
        fpsStarted = System.currentTimeMillis();
        thread = new Thread(this, "RDL-loop");
        thread.start();
    }

    public void stop() {
        alive = false;
        suspended = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public void suspendGame() {
        suspended = true;
        fireHeld = false;
    }

    public void resumeGame() {
        suspended = false;
        fpsStarted = System.currentTimeMillis();
        fpsFrames = 0;
    }

    public void run() {
        long frameDuration = 66; /* 15 Hz ceiling; physics remains stable on slower devices. */
        while (alive) {
            long started = System.currentTimeMillis();
            if (!suspended) {
                tickFrame();
                long elapsed = System.currentTimeMillis() - started;
                adaptQuality(elapsed);
                long sleep = frameDuration - elapsed;
                if (sleep < 1) {
                    sleep = 1;
                }
                sleep(sleep);
            } else {
                sleep(80);
            }
        }
    }

    private void tickFrame() {
        processCommands(takeCommands());
        frame++;
        animationTick++;
        if (screen == SCREEN_GAME) {
            pollHeldInput();
            if (speed == 1 || frame % speed == 0) {
                world.step();
            }
            if (world.explosionCount != lastExplosionCount) {
                lastExplosionCount = world.explosionCount;
                Sound.blast();
            }
            renderer.renderGame(world, cursorX, cursorY,
                    MATERIALS[materialIndex], tool, speed, fps);
        } else if (screen == SCREEN_TITLE) {
            renderer.renderTitle(selectedScene, Sound.isEnabled(), animationTick);
        } else {
            renderer.renderHelp();
        }

        int width = getWidth();
        int height = getHeight();
        if (width != PixelRenderer.SCREEN_WIDTH || height != PixelRenderer.SCREEN_HEIGHT) {
            graphics.setColor(0x000000);
            graphics.fillRect(0, 0, width, height);
        }
        graphics.drawRGB(renderer.pixels, 0, PixelRenderer.SCREEN_WIDTH,
                (width - PixelRenderer.SCREEN_WIDTH) / 2,
                (height - PixelRenderer.SCREEN_HEIGHT) / 2,
                PixelRenderer.SCREEN_WIDTH, PixelRenderer.SCREEN_HEIGHT, false);
        flushGraphics();

        fpsFrames++;
        long now = System.currentTimeMillis();
        long span = now - fpsStarted;
        if (span >= 1000) {
            fps = (int) ((fpsFrames * 1000) / span);
            fpsFrames = 0;
            fpsStarted = now;
        }
    }

    private void processCommands(int pending) {
        if (pending == 0) {
            return;
        }
        if ((pending & CMD_START) != 0) {
            startSelectedScene();
        }
        if (screen != SCREEN_GAME) {
            return;
        }
        if ((pending & CMD_PREVIOUS_MATERIAL) != 0) {
            materialIndex--;
            if (materialIndex < 0) {
                materialIndex = MATERIALS.length - 1;
            }
        }
        if ((pending & CMD_NEXT_MATERIAL) != 0) {
            materialIndex++;
            if (materialIndex >= MATERIALS.length) {
                materialIndex = 0;
            }
        }
        if ((pending & CMD_NEXT_TOOL) != 0) {
            tool = (tool + 1) & 3;
            fireRepeat = -1;
        }
        if ((pending & CMD_PAUSE) != 0) {
            world.running = !world.running;
        }
        if ((pending & CMD_SPEED) != 0) {
            speed = speed == 1 ? 2 : speed == 2 ? 4 : 1;
        }
        if ((pending & CMD_RESET) != 0) {
            world.reset(selectedScene);
            lastExplosionCount = world.explosionCount;
        }
        if ((pending & CMD_NEXT_SCENE) != 0) {
            selectedScene++;
            if (selectedScene >= World.SCENE_COUNT) {
                selectedScene = 0;
            }
            world.reset(selectedScene);
            lastExplosionCount = world.explosionCount;
        }
        if ((pending & CMD_MENU) != 0) {
            fireHeld = false;
            screen = SCREEN_TITLE;
        }
    }

    private synchronized void postCommand(int command) {
        commands |= command;
    }

    private synchronized int takeCommands() {
        int pending = commands;
        commands = 0;
        return pending;
    }

    private void pollHeldInput() {
        int keys = getKeyStates();
        int dx = 0;
        int dy = 0;
        if ((keys & LEFT_PRESSED) != 0) {
            dx--;
        }
        if ((keys & RIGHT_PRESSED) != 0) {
            dx++;
        }
        if ((keys & UP_PRESSED) != 0) {
            dy--;
        }
        if ((keys & DOWN_PRESSED) != 0) {
            dy++;
        }
        if (dx != 0 || dy != 0) {
            if (moveDelay == 0) {
                cursorX += dx;
                cursorY += dy;
                if (cursorX < 0) {
                    cursorX = 0;
                } else if (cursorX >= World.WIDTH) {
                    cursorX = World.WIDTH - 1;
                }
                if (cursorY < 0) {
                    cursorY = 0;
                } else if (cursorY >= World.HEIGHT) {
                    cursorY = World.HEIGHT - 1;
                }
                moveDelay = 1;
            } else {
                moveDelay--;
            }
        } else {
            moveDelay = 0;
        }

        boolean down = fireHeld || (keys & FIRE_PRESSED) != 0;
        if (fireLock) {
            if (!down) {
                fireLock = false;
            }
            down = false;
        }
        if (down) {
            if (fireRepeat < 0) {
                useTool();
                fireRepeat = toolRepeatDelay();
            } else if (fireRepeat == 0) {
                useTool();
                fireRepeat = toolRepeatDelay();
            } else {
                fireRepeat--;
            }
        } else {
            fireRepeat = -1;
        }
    }

    private int toolRepeatDelay() {
        if (tool == TOOL_BUILD || tool == TOOL_ERASE) {
            return 2;
        }
        if (tool == TOOL_SPARK) {
            return 8;
        }
        return 12;
    }

    private void useTool() {
        byte material = MATERIALS[materialIndex];
        if (tool == TOOL_BUILD) {
            world.place(cursorX, cursorY, material);
        } else if (tool == TOOL_ERASE) {
            world.erase(cursorX, cursorY);
        } else if (tool == TOOL_SPARK) {
            world.ignite(cursorX, cursorY);
            Sound.spark();
        } else {
            world.impact(cursorX, cursorY);
        }
    }

    protected void keyPressed(int keyCode) {
        int action = gameAction(keyCode);
        if (screen == SCREEN_TITLE) {
            if (keyCode == KEY_NUM1 || action == LEFT) {
                selectedScene--;
                if (selectedScene < 0) {
                    selectedScene = World.SCENE_COUNT - 1;
                }
            } else if (keyCode == KEY_NUM3 || action == RIGHT) {
                selectedScene++;
                if (selectedScene >= World.SCENE_COUNT) {
                    selectedScene = 0;
                }
            } else if (keyCode == KEY_NUM7) {
                screen = SCREEN_HELP;
            } else if (keyCode == KEY_NUM0) {
                Sound.toggle();
            } else if (keyCode == KEY_NUM5 || action == FIRE) {
                postCommand(CMD_START);
            }
            return;
        }
        if (screen == SCREEN_HELP) {
            if (keyCode == KEY_NUM5 || action == FIRE || keyCode == KEY_NUM7
                    || keyCode == -6 || keyCode == -7) {
                screen = SCREEN_TITLE;
            }
            return;
        }

        if (keyCode == KEY_NUM1) {
            postCommand(CMD_PREVIOUS_MATERIAL);
        } else if (keyCode == KEY_NUM3) {
            postCommand(CMD_NEXT_MATERIAL);
        } else if (keyCode == KEY_NUM7) {
            postCommand(CMD_NEXT_TOOL);
        } else if (keyCode == KEY_NUM9 || keyCode == -6) {
            postCommand(CMD_PAUSE);
        } else if (keyCode == KEY_STAR) {
            postCommand(CMD_SPEED);
        } else if (keyCode == KEY_POUND) {
            postCommand(CMD_RESET);
        } else if (keyCode == KEY_NUM0) {
            postCommand(CMD_NEXT_SCENE);
        } else if (keyCode == -7) {
            postCommand(CMD_MENU);
        } else if (keyCode == KEY_NUM5 || action == FIRE) {
            fireHeld = true;
        }
    }

    protected void keyReleased(int keyCode) {
        int action = gameAction(keyCode);
        if (keyCode == KEY_NUM5 || action == FIRE) {
            fireHeld = false;
        }
    }

    private void startSelectedScene() {
        world.reset(selectedScene);
        world.running = true;
        lastExplosionCount = world.explosionCount;
        cursorX = 16;
        cursorY = 11;
        speed = 1;
        fireHeld = false;
        fireLock = true;
        fireRepeat = -1;
        screen = SCREEN_GAME;
    }

    private int gameAction(int keyCode) {
        try {
            return getGameAction(keyCode);
        } catch (IllegalArgumentException ignored) {
            return 0;
        }
    }

    private void adaptQuality(long elapsed) {
        if (elapsed > 88) {
            slowFrames++;
            fastFrames = 0;
        } else if (elapsed < 58) {
            fastFrames++;
            slowFrames = 0;
        } else {
            slowFrames = 0;
            fastFrames = 0;
        }
        if (slowFrames >= 18) {
            int limit = world.getParticleLimit();
            if (limit > 64) {
                world.setParticleLimit(limit - 16);
            } else {
                renderer.setDetail(0);
            }
            slowFrames = 0;
        }
        if (fastFrames >= 180) {
            int limit = world.getParticleLimit();
            if (limit < 144) {
                world.setParticleLimit(limit + 16);
            } else {
                renderer.setDetail(1);
            }
            fastFrames = 0;
        }
    }

    private static void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignored) {
            /* The lifecycle thread uses interruption only during shutdown. */
        }
    }
}
