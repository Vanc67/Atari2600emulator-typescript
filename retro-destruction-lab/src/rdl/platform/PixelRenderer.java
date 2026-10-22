package rdl.platform;

import rdl.core.World;

/** Software renderer. The only platform object needed by LabCanvas is drawRGB. */
public final class PixelRenderer {
    public static final int SCREEN_WIDTH = 128;
    public static final int SCREEN_HEIGHT = 160;
    public static final int WORLD_Y = 10;
    public static final int WORLD_HEIGHT = 128;

    private static final int BG = 0x0b0e17;
    private static final int GRID = 0x151b29;
    private static final int PANEL = 0x171f30;
    private static final int PANEL_LIGHT = 0x26344a;
    private static final int TEXT = 0xd5e0e8;
    private static final int MUTED = 0x718198;
    private static final int YELLOW = 0xf7c948;
    private static final int ORANGE = 0xf07b32;
    private static final int RED = 0xef4756;
    private static final int GREEN = 0x55d68b;
    private static final int CYAN = 0x52d6d3;

    private static final int[] BASE = {
        0x000000, 0x71839e, 0x9b9b96, 0xa85c36, 0x226b78,
        0xc93545, 0xd59b24, 0x2369bd, 0x28364a
    };
    private static final int[] LIGHT = {
        0x000000, 0xb1c2d4, 0xd3d0c5, 0xdb8950, 0x63d9df,
        0xf16a62, 0xffd258, 0x56b4ef, 0x41546d
    };
    private static final int[] DARK = {
        0x000000, 0x3d4d64, 0x5e6266, 0x64351f, 0x153c50,
        0x77233a, 0x78511d, 0x173f78, 0x172235
    };

    private static final String[] MATERIAL_NAMES = {
        "EMPTY", "STEEL", "CONCRETE", "WOOD", "GLASS",
        "TNT", "FUEL", "WATER", "ANCHOR"
    };
    private static final String[] TOOL_NAMES = {
        "BUILD", "ERASE", "SPARK", "BLAST"
    };
    private static final int[] TOOL_COLORS = {
        GREEN, RED, YELLOW, ORANGE
    };
    private static final String[] SCENE_NAMES = {
        "TOWER", "BRIDGE", "EMPTY"
    };

    public final int[] pixels = new int[SCREEN_WIDTH * SCREEN_HEIGHT];
    private int detail = 1;

    public void setDetail(int value) {
        detail = value;
    }

    public int getDetail() {
        return detail;
    }

    public void renderGame(World world, int cursorX, int cursorY,
            int material, int tool, int speed, int fps) {
        clear(BG);
        fill(0, 0, SCREEN_WIDTH, WORLD_Y, PANEL);
        fill(0, WORLD_Y + WORLD_HEIGHT, SCREEN_WIDTH,
                SCREEN_HEIGHT - WORLD_Y - WORLD_HEIGHT, PANEL);

        int x;
        int y;
        for (y = 0; y < WORLD_HEIGHT; y++) {
            int offset = (WORLD_Y + y) * SCREEN_WIDTH;
            for (x = 0; x < SCREEN_WIDTH; x++) {
                pixels[offset + x] = ((x & 3) == 0 && (y & 3) == 0) ? GRID : BG;
            }
        }

        int shakeX = 0;
        int shakeY = 0;
        if (world.shake > 0) {
            shakeX = (world.tick & 1) == 0 ? -1 : 1;
            shakeY = (world.tick & 2) == 0 ? 1 : -1;
        }

        int i;
        for (i = 0; i < World.SIZE; i++) {
            byte cell = world.cell[i];
            if (cell != World.EMPTY) {
                drawCell(world, i, cell, shakeX, shakeY);
            }
        }

        int particleLimit = world.getParticleLimit();
        for (i = 0; i < particleLimit; i++) {
            int life = world.particleLife[i] & 255;
            if (life == 0) {
                continue;
            }
            int kind = world.particleKind[i];
            int materialId = world.particleMaterial[i];
            int color;
            if (kind == 1) {
                color = (life & 2) == 0 ? YELLOW : ORANGE;
            } else if (kind == 2) {
                color = materialId == World.WATER ? LIGHT[World.WATER] : MUTED;
            } else {
                color = (life & 1) == 0 ? BASE[materialId] : LIGHT[materialId];
            }
            int px = (world.particleX[i] >> World.FP_SHIFT) + shakeX;
            int py = WORLD_Y + (world.particleY[i] >> World.FP_SHIFT) + shakeY;
            if (kind == 0) {
                worldFill(px, py, 2, 2, color);
            } else {
                worldPixel(px, py, color);
            }
        }

        drawCursor(cursorX, cursorY, TOOL_COLORS[tool], shakeX, shakeY);

        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                2, 2, 1, "RDL", YELLOW);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                33, 2, 1, "FPS", MUTED);
        PixelFont.drawNumber(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                47, 2, 1, fps, fps < 10 ? RED : TEXT);
        String runText = world.running ? "RUN" : "STOP";
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                world.running ? 105 : 101, 2, 1, runText,
                world.running ? GREEN : RED);

        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                2, 141, 1, "MAT", MUTED);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                20, 141, 1, MATERIAL_NAMES[material], LIGHT[material]);
        String sceneName = SCENE_NAMES[world.scene];
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                126 - PixelFont.width(sceneName, 1), 141, 1, sceneName, MUTED);

        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                2, 151, 1, "TOOL", MUTED);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                24, 151, 1, TOOL_NAMES[tool], TOOL_COLORS[tool]);
        String speedText = speed == 1 ? "1X" : speed == 2 ? "1/2" : "1/4";
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                76, 151, 1, speedText, speed == 1 ? TEXT : YELLOW);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                98, 151, 1, "B", MUTED);
        PixelFont.drawNumber(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                105, 151, 1, world.blockCount, TEXT);
    }

    private void drawCell(World world, int index, byte material,
            int shakeX, int shakeY) {
        int gridX = index % World.WIDTH;
        int gridY = index / World.WIDTH;
        int x = gridX * World.TILE + shakeX;
        int y = WORLD_Y + gridY * World.TILE + shakeY;
        int base = BASE[material];
        int light = LIGHT[material];
        int dark = DARK[material];

        if (material == World.WATER) {
            worldFill(x, y, 4, 4, base);
            int shimmer = (world.tick + gridX + gridY) & 3;
            worldPixel(x + shimmer, y, light);
            worldPixel(x + ((shimmer + 2) & 3), y + 2, DARK[material]);
            return;
        }

        worldFill(x, y, 4, 4, base);
        worldFill(x, y, 4, 1, light);
        worldFill(x, y + 3, 4, 1, dark);

        if (material == World.STEEL) {
            worldPixel(x, y + 1, dark);
            worldPixel(x + 3, y + 2, light);
        } else if (material == World.CONCRETE) {
            worldPixel(x + (index & 3), y + 2, dark);
        } else if (material == World.WOOD) {
            worldFill(x + 1, y + 1, 1, 2, dark);
            worldPixel(x + 3, y + 1, light);
        } else if (material == World.GLASS) {
            worldPixel(x + 1, y + 1, light);
            worldPixel(x + 2, y + 2, light);
        } else if (material == World.TNT) {
            worldFill(x + 1, y + 1, 2, 2, dark);
            worldPixel(x + 1, y + 1, YELLOW);
        } else if (material == World.FUEL) {
            worldPixel(x + 1, y + 1, dark);
            worldPixel(x + 2, y + 2, dark);
            worldPixel(x + 2, y + 1, light);
        } else if (material == World.ANCHOR) {
            worldPixel(x + (gridX & 3), y + 2, dark);
        }

        if (detail > 0 && World.isBlock(material)
                && (world.hp[index] & 255) < World.MAX_HP[material] / 2) {
            worldPixel(x + ((index >> 2) & 3), y + 1, BG);
            worldPixel(x + ((index + 1) & 3), y + 2, dark);
        }
        int heat = world.heat[index] & 255;
        if (heat > 45) {
            worldPixel(x + ((world.tick + index) & 3), y + 1,
                    heat > 130 ? YELLOW : ORANGE);
            if (heat > 105) {
                worldPixel(x + ((world.tick + index + 2) & 3), y + 2, RED);
            }
        }
    }

    private void drawCursor(int cellX, int cellY, int color,
            int shakeX, int shakeY) {
        int x = cellX * World.TILE - 1 + shakeX;
        int y = WORLD_Y + cellY * World.TILE - 1 + shakeY;
        worldPixel(x, y, color);
        worldPixel(x + 1, y, color);
        worldPixel(x, y + 1, color);
        worldPixel(x + 5, y, color);
        worldPixel(x + 4, y, color);
        worldPixel(x + 5, y + 1, color);
        worldPixel(x, y + 5, color);
        worldPixel(x + 1, y + 5, color);
        worldPixel(x, y + 4, color);
        worldPixel(x + 5, y + 5, color);
        worldPixel(x + 4, y + 5, color);
        worldPixel(x + 5, y + 4, color);
    }

    public void renderTitle(int scene, boolean soundEnabled, int animationTick) {
        clear(BG);
        int i;
        for (i = 0; i < SCREEN_WIDTH; i += 4) {
            pixels[(animationTick % WORLD_HEIGHT + WORLD_Y) * SCREEN_WIDTH + i] = GRID;
        }
        fill(3, 4, 122, 75, PANEL);
        fill(3, 4, 122, 1, PANEL_LIGHT);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                10, 3, "RETRO", ORANGE);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                31, 2, "DESTRUCTION", TEXT);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                47, 3, "LAB", CYAN);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                69, 1, "TRUE PHYSICS SANDBOX", MUTED);

        String sceneText = scene == World.SCENE_TOWER ? "< TOWER >"
                : scene == World.SCENE_BRIDGE ? "< BRIDGE >" : "< EMPTY >";
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                85, 2, sceneText, YELLOW);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                102, 1, "1/3 CHANGE SCENE", MUTED);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                114, 1, "5 START", GREEN);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                125, 1, "7 FIELD MANUAL", TEXT);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                136, 1, soundEnabled ? "0 SOUND ON" : "0 SOUND OFF", MUTED);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                151, 1, "K510I 128X160", PANEL_LIGHT);
    }

    public void renderHelp() {
        clear(BG);
        fill(2, 2, 124, 15, PANEL);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                7, 2, "FIELD MANUAL", YELLOW);
        int x = 4;
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 24, 1,
                "2/4/6/8 MOVE CURSOR", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 33, 1,
                "5 USE TOOL", GREEN);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 42, 1,
                "1/3 SELECT MATERIAL", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 51, 1,
                "7 NEXT TOOL", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 60, 1,
                "9 RUN OR PAUSE", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 69, 1,
                "* SPEED 1X 1/2 1/4", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 78, 1,
                "# RESET SCENE", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 87, 1,
                "0 NEXT SCENE", TEXT);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 96, 1,
                "SOFT1 PAUSE SOFT2 MENU", MUTED);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 111, 1,
                "WOOD BURNS TNT CHAINS", ORANGE);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 120, 1,
                "WATER STOPS FIRE", CYAN);
        PixelFont.draw(pixels, SCREEN_WIDTH, SCREEN_HEIGHT, x, 129, 1,
                "BUILD WHILE PAUSED", MUTED);
        PixelFont.drawCenter(pixels, SCREEN_WIDTH, SCREEN_HEIGHT,
                148, 1, "5 BACK", GREEN);
    }

    private void clear(int color) {
        int i;
        for (i = 0; i < pixels.length; i++) {
            pixels[i] = color;
        }
    }

    private void fill(int x, int y, int width, int height, int color) {
        int x0 = x < 0 ? 0 : x;
        int y0 = y < 0 ? 0 : y;
        int x1 = x + width > SCREEN_WIDTH ? SCREEN_WIDTH : x + width;
        int y1 = y + height > SCREEN_HEIGHT ? SCREEN_HEIGHT : y + height;
        if (x0 >= x1 || y0 >= y1) {
            return;
        }
        int py;
        int px;
        for (py = y0; py < y1; py++) {
            int offset = py * SCREEN_WIDTH;
            for (px = x0; px < x1; px++) {
                pixels[offset + px] = color;
            }
        }
    }

    private void worldFill(int x, int y, int width, int height, int color) {
        int x0 = x < 0 ? 0 : x;
        int y0 = y < WORLD_Y ? WORLD_Y : y;
        int x1 = x + width > SCREEN_WIDTH ? SCREEN_WIDTH : x + width;
        int y1 = y + height > WORLD_Y + WORLD_HEIGHT
                ? WORLD_Y + WORLD_HEIGHT : y + height;
        if (x0 >= x1 || y0 >= y1) {
            return;
        }
        int py;
        int px;
        for (py = y0; py < y1; py++) {
            int offset = py * SCREEN_WIDTH;
            for (px = x0; px < x1; px++) {
                pixels[offset + px] = color;
            }
        }
    }

    private void worldPixel(int x, int y, int color) {
        if (x >= 0 && x < SCREEN_WIDTH && y >= WORLD_Y
                && y < WORLD_Y + WORLD_HEIGHT) {
            pixels[y * SCREEN_WIDTH + x] = color;
        }
    }
}
