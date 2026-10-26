package rpc;

/**
 * The pixel-art room the life-sim half of the game lives in.
 *
 * This replaces the 4.8 "room", which was one orange rectangle with two lines
 * drawn through it.  Everything here is drawn procedurally with rectangles,
 * lines and triangles, so it runs on any MIDP handset and scales with the
 * screen:
 *
 *   wallpaper, window with sky/weather/season/city, curtains
 *   desk, CRT monitor (screen shows what the PC is doing),
 *   tower case with a spinning fan, keyboard, mouse, mug with steam,
 *   desk lamp with light cone, cat (asleep / awake), poster, wall clock,
 *   plant, door and the dust that hangs in the light beam.
 */
public final class Room {

    /* hotspot ids - also the order the cursor cycles through */
    public static final int HOT_MONITOR = 0;
    public static final int HOT_MUG = 1;
    public static final int HOT_CAT = 2;
    public static final int HOT_LAMP = 3;
    public static final int HOT_BED = 4;
    public static final int HOT_DOOR = 5;
    public static final int HOT_TOOLS = 6;
    public static final int HOT_RADIO = 7;
    public static final int HOT_COUNT = 8;

    private Room() {
    }

    /**
     * Draws the whole room.
     *
     * @param g      target surface
     * @param life   the simulation state (time, weather, cat, tea, ...)
     * @param frame  animation frame counter
     * @param pcOn   whether the desktop PC is switched on
     * @param viewH  height of the clipped scene viewport (not the screen!)
     */
    public static void draw(Gfx g, LifeSim life, int frame, boolean pcOn, int viewH) {
        int w = g.width();
        int h = viewH;
        int wallH = h * 55 / 100;
        int night = life.timeOfDay == LifeSim.TIME_NIGHT ? 1 : 0;
        int dusk = life.timeOfDay == LifeSim.TIME_SUNSET ? 1 : 0;

        /* ---------------- wall --------------------------------------- */
        int wall = night == 1 ? 0x232C42 : (dusk == 1 ? 0x4A3E62 : 0x415274);
        int stripe = night == 1 ? 0x2A3550 : (dusk == 1 ? 0x564872 : 0x4A5C82);
        g.setColor(wall);
        g.fillRect(0, 0, w, wallH);
        g.setColor(stripe);
        for (int x = 0; x < w; x += 12) {
            g.fillRect(x, 0, 4, wallH);
        }
        // wallpaper skirting
        g.setColor(night == 1 ? 0x121725 : 0x232C40);
        g.fillRect(0, wallH - 6, w, 6);
        g.setColor(night == 1 ? 0x2A3348 : 0x4C5A7A);
        g.fillRect(0, wallH - 8, w, 2);

        /* ---------------- floor -------------------------------------- */
        int floor = night == 1 ? 0x1A1712 : 0x2E2820;
        g.setColor(floor);
        g.fillRect(0, wallH, w, h - wallH);
        g.setColor(night == 1 ? 0x221E17 : 0x3A3226);
        for (int y = wallH + 8; y < h; y += 10) {
            g.drawLine(0, y, w, y);
        }

        /* ---------------- window ------------------------------------- */
        int wx = w * 7 / 100;
        int wy = h * 5 / 100;
        int ww = w * 38 / 100;
        int wh = wallH * 56 / 100;
        drawWindow(g, wx, wy, ww, wh, life, frame);

        /* ---------------- poster + clock ----------------------------- */
        int px = w * 55 / 100;
        int py = h * 9 / 100;
        drawPoster(g, px, py, w * 26 / 100, h * 15 / 100, night);
        drawClock(g, w - w * 14 / 100, h * 3 / 100, w * 10 / 100, life);

        /* ---------------- door (right wall) -------------------------- */
        int dw = w * 15 / 100;
        int dh = wallH * 66 / 100;
        drawDoor(g, w - dw - 5, wallH - dh, dw, dh, night);

        /* ---------------- plant -------------------------------------- */
        drawPlant(g, w * 3 / 100, wallH - h * 10 / 100, h * 10 / 100, life);

        /* ---------------- desk + hardware ---------------------------- */
        int deskY = wallH + 4;
        int deskH = h * 7 / 100;
        g.setColor(night == 1 ? 0x241C14 : 0x3E3122);
        g.fillRect(w * 4 / 100, deskY + deskH, 6, h * 14 / 100);
        g.fillRect(w * 66 / 100, deskY + deskH, 6, h * 14 / 100);
        g.setColor(night == 1 ? 0x352A1E : 0x5A4630);
        g.fillRect(0, deskY, w * 72 / 100, deskH);
        g.setColor(night == 1 ? 0x4A3A28 : 0x7A5F42);
        g.fillRect(0, deskY, w * 72 / 100, 3);

        /* monitor on the desk, facing the player */
        int monW = w * 34 / 100;
        int monH = monW * 72 / 100;
        int monX = w * 8 / 100;
        int monY = deskY - monH - 2;
        drawMonitor(g, monX, monY, monW, monH, life, frame, pcOn);

        /* tower case next to it */
        int caseW = w * 16 / 100;
        int caseH = monH + h * 5 / 100;
        int caseX = monX + monW + 4;
        int caseY = deskY - caseH - 2;
        drawCase(g, caseX, caseY, caseW, caseH, frame, pcOn);

        /* keyboard + mouse in front of the monitor */
        int kbY = deskY + deskH + 4;
        drawKeyboard(g, w * 6 / 100, kbY, w * 42 / 100, h * 5 / 100);
        drawMouse(g, w * 50 / 100, kbY + 2, w * 6 / 100, h * 4 / 100);

        /* mug - the star of the tea screen, visible here too */
        int mugX = w * 56 / 100;
        int mugY = deskY - h * 7 / 100;
        drawMug(g, mugX, mugY, w * 9 / 100, h * 7 / 100, life, frame);

        /* desk lamp with a light cone */
        drawLamp(g, w * 64 / 100, deskY - h * 17 / 100, h * 17 / 100, life, night);

        /* ---------------- bed / futon in the foreground -------------- */
        drawBed(g, w, h, wallH, night, life);
        drawCat(g, life, w, h, wallH, frame);


        /* dust motes in the beam of the lamp */
        if (life.lampOn) {
            drawDust(g, w, h, frame, life);
        }
    }

    /* ------------------------------------------------------------------
     *  window
     * ------------------------------------------------------------------ */

    private static void drawWindow(Gfx g, int x, int y, int w, int h,
                                   LifeSim life, int frame) {
        // frame
        g.setColor(0x8A8F9E);
        g.fillRect(x - 3, y - 3, w + 6, h + 6);
        g.setColor(0x5C6274);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);

        // sky gradient by time of day
        int top;
        int bottom;
        switch (life.timeOfDay) {
            case LifeSim.TIME_MORNING:
                top = 0x6E8EC2;
                bottom = 0xE8B27A;
                break;
            case LifeSim.TIME_DAY:
                top = 0x4C8FD6;
                bottom = 0xA8D2F0;
                break;
            case LifeSim.TIME_SUNSET:
                top = 0x3A3A78;
                bottom = 0xE8773C;
                break;
            default:
                top = 0x0B1030;
                bottom = 0x1D2A55;
                break;
        }
        if (life.season == LifeSim.SEASON_WINTER && life.weather != LifeSim.WEATHER_STORM) {
            bottom = life.timeOfDay == LifeSim.TIME_NIGHT ? 0x233153 : 0xB9C7DA;
        }
        int bands = 6;
        for (int i = 0; i < bands; i++) {
            int c = mix(top, bottom, i * 100 / (bands - 1));
            g.setColor(c);
            g.fillRect(x, y + h * i / bands, w, h / bands + 1);
        }

        // sun / moon
        int cx = x + w * 74 / 100;
        int cy = y + h * 22 / 100;
        if (life.timeOfDay == LifeSim.TIME_NIGHT) {
            g.setColor(0xE8EEDA);
            g.fillRect(cx - 5, cy - 5, 10, 10);
            g.setColor(0x0B1030);
            g.fillRect(cx - 1, cy - 6, 8, 10);
            for (int i = 0; i < 12; i++) {
                int sx = x + (i * 37 + 11) % w;
                int sy = y + (i * 23 + 5) % (h / 2);
                g.setColor((frame / 12 + i) % 2 == 0 ? 0xFFFFFF : 0x9FB0D0);
                g.fillRect(sx, sy, 1, 1);
            }
        } else {
            g.setColor(life.timeOfDay == LifeSim.TIME_SUNSET ? 0xFFB05A : 0xFFE9A8);
            g.fillRect(cx - 6, cy - 6, 12, 12);
            g.setColor(life.timeOfDay == LifeSim.TIME_SUNSET ? 0xFFD9A0 : 0xFFF6D0);
            g.fillRect(cx - 4, cy - 4, 8, 8);
        }

        // clouds
        g.setColor(life.timeOfDay == LifeSim.TIME_NIGHT ? 0x33405E : 0xE8EEF8);
        int drift = (frame / 4) % (w + 30);
        int cloudY = y + h * 16 / 100;
        for (int c = 0; c < 2; c++) {
            int cxp = x + (drift + c * (w / 2)) % (w + 20) - 10;
            g.fillRect(cxp, cloudY + c * 6, 12, 3);
            g.fillRect(cxp + 3, cloudY + c * 6 - 2, 7, 3);
        }

        // weather
        int tick = life.animationTick;
        if (life.weather == LifeSim.WEATHER_RAIN || life.weather == LifeSim.WEATHER_STORM) {
            g.setColor(life.weather == LifeSim.WEATHER_STORM ? 0xBFD4F0 : 0x9BC0E8);
            for (int i = 0; i < 26; i++) {
                int rx = x + (i * 29 + tick * 3) % w;
                int ry = y + (i * 17 + tick * 9) % h;
                g.drawLine(rx, ry, rx - 2, ry + 5);
            }
            if (life.weather == LifeSim.WEATHER_STORM) {
                g.setColor(0x28304A);
                g.fillRect(x, y, w, h / 4);
                if (life.lightningActive) {
                    g.setColor(0xFFFFFF);
                    int lx = x + w / 2;
                    g.drawLine(lx, y + 4, lx - 5, y + h / 3);
                    g.drawLine(lx - 5, y + h / 3, lx + 2, y + h / 3 + 4);
                }
            }
        } else if (life.weather == LifeSim.WEATHER_SNOW) {
            g.setColor(0xFFFFFF);
            for (int i = 0; i < 30; i++) {
                int sx = x + (i * 31 + tick) % w;
                int sy = y + (i * 13 + tick * 2) % h;
                g.fillRect(sx, sy, 2, 2);
            }
        } else if (life.weather == LifeSim.WEATHER_LEAVES) {
            g.setColor(0xD08A3C);
            for (int i = 0; i < 12; i++) {
                int lx = x + (i * 23 + tick * 2) % w;
                int ly = y + (i * 19 + tick) % h;
                g.fillRect(lx, ly, 2, 1);
            }
        }

        // city skyline along the bottom of the window
        int cw = w / 9;
        int lights = life.timeOfDay == LifeSim.TIME_NIGHT || life.timeOfDay == LifeSim.TIME_SUNSET ? 1 : 0;
        for (int i = 0; i < 9; i++) {
            int bh = h * (10 + (i * 37) % 26) / 100;
            int bx = x + i * cw;
            g.setColor(lights == 1 ? 0x161C2C : 0x6A748C);
            g.fillRect(bx, y + h - bh, cw - 1, bh);
            if (lights == 1) {
                for (int wy = 0; wy < bh - 4; wy += 5) {
                    for (int wx2 = 0; wx2 < cw - 3; wx2 += 4) {
                        if (((i + wy + wx2) & 3) == 0) {
                            g.setColor(0xFFD98A);
                            g.fillRect(bx + wx2 + 1, y + h - bh + wy + 2, 1, 1);
                        }
                    }
                }
            }
        }

        // mullions
        g.setColor(0x6E7488);
        g.fillRect(x + w / 2 - 1, y, 3, h);
        g.fillRect(x, y + h / 2 - 1, w, 3);
        g.setColor(0xA8AEC0);
        g.fillRect(x - 4, y + h + 3, w + 8, 3);

        // curtains
        g.setColor(life.timeOfDay == LifeSim.TIME_NIGHT ? 0x4A2036 : 0x7A3350);
        g.fillRect(x - 7, y - 4, 6, h + 10);
        g.fillRect(x + w + 1, y - 4, 6, h + 10);
    }

    /* ------------------------------------------------------------------
     *  furniture
     * ------------------------------------------------------------------ */

    private static void drawMonitor(Gfx g, int x, int y, int w, int h,
                                    LifeSim life, int frame, boolean on) {
        int bezel = on ? 0xB9BECB : 0x7E8391;
        g.setColor(0x4E5462);
        g.fillRect(x, y, w, h);
        g.setColor(bezel);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(0x2A2E38);
        int sx = x + w / 12;
        int sy = y + h / 10;
        int sw = w - 2 * (w / 12);
        int sh = h * 7 / 10;
        g.fillRect(sx, sy, sw, sh);

        if (on) {
            drawScreenContent(g, sx, sy, sw, sh, frame, life);
        } else {
            g.setColor(0x14181F);
            g.fillRect(sx + 1, sy + 1, sw - 2, sh - 2);
            g.setColor(0x30384A);
            g.fillRect(sx + sw / 2 - 1, sy + sh / 2 - 1, 2, 2);
        }

        // power LED + brand
        g.setColor(on ? 0x63E6A0 : 0x2C3240);
        g.fillRect(x + w - 8, y + h - 6, 3, 2);
        g.setColor(on ? 0x3A4152 : 0x3A4152);
        g.fillRect(x + 5, y + h - 6, w / 3, 2);

        // stand
        g.setColor(0x6E7482);
        g.fillRect(x + w / 3, y + h, w / 3, 3);
        g.setColor(0x8A90A0);
        g.fillRect(x + w / 5, y + h + 3, w * 3 / 5, 3);
    }

    private static void drawScreenContent(Gfx g, int x, int y, int w, int h,
                                          int frame, LifeSim life) {
        g.setColor(0x1B3A5A);
        g.fillRect(x, y, w, h);
        // fake desktop with a window and a taskbar
        g.setColor(0x2D6FA8);
        g.fillRect(x + 1, y + 1, w - 2, 4);
        g.setColor(0xE9F2FF);
        g.fillRect(x + 3, y + h - 6, w - 6, 3);
        g.setColor(0x9CC3E8);
        g.fillRect(x + 6, y + h - 5, 6, 1);
        g.fillRect(x + 15, y + h - 5, 5, 1);
        g.setColor(0xF0F5FF);
        g.fillRect(x + 3, y + 7, w - 6, h - 15);
        g.setColor(0x2D6FA8);
        g.fillRect(x + 3, y + 7, w - 6, 3);
        // scrolling "code" lines
        for (int i = 0; i < (h - 18) / 3; i++) {
            int len = (i * 7 + frame / 6) % (w - 12);
            g.setColor((i & 1) == 0 ? 0x9AA8BC : 0x6F7E94);
            g.fillRect(x + 5, y + 12 + i * 3, len, 1);
        }
        // blinking cursor
        if ((frame / 6) % 2 == 0) {
            g.setColor(0x2D6FA8);
            g.fillRect(x + 5, y + 12 + ((h - 18) / 3) * 3, 3, 1);
        }
    }

    private static void drawCase(Gfx g, int x, int y, int w, int h,
                                 int frame, boolean on) {
        g.setColor(0x3A3F4C);
        g.fillRect(x, y, w, h);
        g.setColor(0x8A90A0);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(0x6E7482);
        g.fillRect(x + 3, y + 3, w - 6, h - 6);
        // drive bays
        g.setColor(0x4A5060);
        for (int i = 0; i < 3; i++) {
            g.fillRect(x + 5, y + 6 + i * 5, w - 10, 3);
            g.setColor(0x9AA0B0);
            g.fillRect(x + 6, y + 7 + i * 5, w - 12, 1);
            g.setColor(0x4A5060);
        }
        // fan with rotating blades
        int cx = x + w / 2;
        int cy = y + h * 68 / 100;
        int r = Math.min(w, h) / 5;
        g.setColor(0x30363F);
        g.fillRect(cx - r, cy - r, r * 2, r * 2);
        if (on) {
            int phase = (frame * 30) % 360;
            g.setColor(0x76809A);
            for (int b = 0; b < 4; b++) {
                double a = Math.toRadians(phase + b * 90);
                int bx = cx + (int) (Math.cos(a) * (r - 2));
                int by = cy + (int) (Math.sin(a) * (r - 2));
                g.fillRect(bx, by, 2, 2);
                g.drawLine(cx, cy, bx, by);
            }
        } else {
            g.setColor(0x4A5060);
            g.fillRect(cx - r + 2, cy - 1, r * 2 - 4, 2);
        }
        // power LED
        g.setColor(on ? 0x63E6A0 : 0x2C3240);
        g.fillRect(x + w - 7, y + 4, 2, 2);
        g.setColor(0xFFC24B);
        g.fillRect(x + w - 7, y + h - 7, 2, 2);
    }

    private static void drawKeyboard(Gfx g, int x, int y, int w, int h) {
        g.setColor(0x3E4452);
        g.fillRect(x, y, w, h);
        g.setColor(0xB0B6C4);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(0xE2E8F2);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < w / 6; c++) {
                g.fillRect(x + 3 + c * 6, y + 2 + r * 3, 4, 2);
            }
        }
        g.setColor(0xA0A6B4);
        g.fillRect(x + w / 3, y + h - 4, w / 3, 2);
    }

    private static void drawMouse(Gfx g, int x, int y, int w, int h) {
        g.setColor(0xB0B6C4);
        g.fillRect(x, y, w, h);
        g.setColor(0xE2E8F2);
        g.fillRect(x + 1, y + 1, w - 2, h - 2);
        g.setColor(0x7E8494);
        g.fillRect(x + w / 2 - 1, y + 1, 2, 2);
    }

    private static void drawMug(Gfx g, int x, int y, int w, int h, LifeSim life,
                               int frame) {
        int body = life.teaLevel > 0 ? 0xE8EEF8 : 0x9AA0B0;
        g.setColor(0x3A4152);
        g.fillRect(x + w / 5, y + h - 2, w * 3 / 5, 2);
        g.setColor(body);
        g.fillRect(x + w / 6, y + h / 4, w * 2 / 3, h * 3 / 4 - 2);
        // handle
        g.setColor(body);
        g.fillRect(x + w * 5 / 6, y + h / 2, w / 6, h / 3);
        g.setColor(0x3A4152);
        g.fillRect(x + w * 5 / 6 + 1, y + h / 2 + 1, w / 6 - 2, h / 3 - 2);
        // tea inside
        if (life.teaLevel > 0) {
            g.setColor(life.teaHot ? 0xB4651E : 0x8A4A16);
            g.fillRect(x + w / 6 + 1, y + h / 4 + 2, w * 2 / 3 - 2,
                    (h * 3 / 4 - 4) * life.teaLevel / 100);
        }
        // steam while hot
        if (life.teaHot && life.teaLevel > 0) {
            g.setColor(0xE8F0FF);
            for (int i = 0; i < 3; i++) {
                int sx = x + w / 3 + i * (w / 5);
                int sy = y - (frame / 3 + i * 3) % (h / 2);
                g.fillRect(sx, y + h / 4 - 2 - (frame / 3 + i * 4) % (h / 3), 1, 3);
            }
        }
    }

    private static void drawLamp(Gfx g, int x, int y, int h, LifeSim life, int night) {
        int shadeW = Math.max(10, h / 3);
        int shadeH = Math.max(5, h / 5);
        int lx = x + shadeW / 2;
        int baseY = y + h;
        /* the beam is a stack of widening bands at low contrast - drawing it
         * as one solid triangle looked like a rocket, not light */
        if (life.lampOn) {
            int beamTop = y + shadeH + 2;
            int beamH = baseY - beamTop;
            int bands = Math.max(4, beamH / 6);
            for (int i = 0; i < bands; i++) {
                int t = i * 100 / bands;
                int c = mix(night == 1 ? 0x2A2416 : 0x3E3420, 0x0, t / 2);
                g.setColor(c);
                int half = shadeW / 2 + (shadeW * 3 / 2) * i / bands;
                g.fillRect(lx - half, beamTop + i * beamH / bands, half * 2,
                        beamH / bands + 1);
            }
            g.setColor(0xFFD06A);
            g.fillRect(lx - shadeW / 2, y + shadeH - 1, shadeW, 2);
        }
        /* pole + base */
        g.setColor(0x2A303C);
        g.fillRect(lx - 1, y + shadeH, 2, h - shadeH);
        g.setColor(0x3A4152);
        g.fillRect(lx - 8, baseY, 16, 3);
        /* shade */
        g.setColor(life.lampOn ? 0xD8A340 : 0x6E7482);
        g.fillTriangle(lx - shadeW / 2, y + shadeH, lx + shadeW / 2, y + shadeH,
                lx, y);
        g.setColor(life.lampOn ? 0xFFE9A8 : 0x9AA0B0);
        g.fillRect(lx - shadeW / 2, y + shadeH - 1, shadeW, 1);
    }

    /** Low futon in the foreground: pillow, duvet, wooden frame. */
    private static void drawBed(Gfx g, int w, int h, int wallH, int night,
                                LifeSim life) {
        int x = w * 2 / 100;
        int y = wallH + h * 26 / 100;
        int bw = w * 40 / 100;
        int bh = h * 12 / 100;
        if (y + bh > h * 88 / 100) {
            bh = h * 88 / 100 - y;
        }
        // frame
        g.setColor(night == 1 ? 0x2A2018 : 0x4A3A26);
        g.fillRect(x - 2, y + 2, bw + 4, bh + 3);
        // mattress
        g.setColor(night == 1 ? 0x4A5468 : 0xE2E8F2);
        g.fillRect(x, y, bw, bh);
        // duvet
        g.setColor(night == 1 ? 0x2E4A6A : 0x3E6FA8);
        g.fillRect(x + bw / 3, y, bw * 2 / 3, bh);
        g.setColor(night == 1 ? 0x3A5A80 : 0x5A8FD0);
        g.fillRect(x + bw / 3, y, bw * 2 / 3, 2);
        // pillow
        g.setColor(night == 1 ? 0x9AA4B8 : 0xFFFFFF);
        g.fillRect(x + 2, y + 1, bw / 3 - 4, bh - 3);
        g.setColor(night == 1 ? 0x6E7A90 : 0xC8D2E0);
        g.fillRect(x + 2, y + bh - 3, bw / 3 - 4, 1);
        // legs
        g.setColor(night == 1 ? 0x1A1410 : 0x2E2418);
        g.fillRect(x, y + bh + 3, 4, 4);
        g.fillRect(x + bw - 4, y + bh + 3, 4, 4);
    }

    private static void drawCat(Gfx g, LifeSim life, int w, int h, int wallH,
                                int frame) {
        int cx;
        int cy;
        if (life.catState == 0) {
            // asleep on the desk, next to the keyboard
            cx = w * 46 / 100;
            cy = wallH + h * 9 / 100;
        } else if (life.catState == 3) {
            // on the keyboard (the classic)
            cx = w * 26 / 100;
            cy = wallH + h * 13 / 100;
        } else {
            // walking on the floor, tail twitching
            cx = w * (58 + (frame / 40) % 6) / 100;
            cy = h * 80 / 100;
        }
        int s = Math.max(2, w / 60);
        int body = 0xE8A24B;
        int dark = 0x8A5420;

        g.setColor(body);
        g.fillRect(cx, cy, s * 7, s * 3);
        g.fillRect(cx + s * 5, cy - s, s * 3, s * 3);
        // ears
        g.setColor(dark);
        g.fillRect(cx + s * 5, cy - s * 2, s, s);
        g.fillRect(cx + s * 7, cy - s * 2, s, s);
        // eyes (closed while asleep)
        g.setColor(0x2C2118);
        if (life.catState == 0) {
            g.fillRect(cx + s * 6, cy, s, 1);
            g.fillRect(cx + s * 7, cy, s, 1);
        } else {
            g.fillRect(cx + s * 6, cy, 1, 1);
            g.fillRect(cx + s * 7, cy, 1, 1);
        }
        // tail
        g.setColor(body);
        int wag = (frame / 8) % 3;
        g.fillRect(cx - s * 2, cy + s + wag, s * 2, 1);
        g.fillRect(cx - s * 3, cy + s + 1 + wag, s, 2);
        // z's while sleeping
        if (life.catState == 0) {
            g.setColor(0x9FB0D0);
            int z = (frame / 10) % 3;
            g.drawText("z", cx + s * 9, cy - s * 3 - z * 2, 1);
        }
    }

    private static void drawDust(Gfx g, int w, int h, int frame, LifeSim life) {
        g.setColor(0xE8DCB0);
        for (int i = 0; i < 14; i++) {
            int x = (i * 37 + frame / 3) % w;
            int y = h * 30 / 100 + (i * 23 + frame / 5) % (h * 40 / 100);
            g.fillRect(x, y, 1, 1);
        }
    }

    private static void drawPoster(Gfx g, int x, int y, int w, int h, int night) {
        g.setColor(0x1A1E28);
        g.fillRect(x - 1, y - 1, w + 2, h + 2);
        g.setColor(night == 1 ? 0x2A3550 : 0x3E4A68);
        g.fillRect(x, y, w, h);
        g.setColor(0xFFC24B);
        g.drawText("486", x + w / 2, y + 2, 2, Gfx.HCENTER);
        g.setColor(0x9AA8C0);
        g.drawText("DX2-66", x + w / 2, y + h - 9, 1, Gfx.HCENTER);
    }

    private static void drawClock(Gfx g, int x, int y, int d, LifeSim life) {
        g.setColor(0x2A2E38);
        g.fillRect(x, y, d, d);
        g.setColor(0xE8EEF8);
        g.fillRect(x + 1, y + 1, d - 2, d - 2);
        int cx = x + d / 2;
        int cy = y + d / 2;
        int hour = life.curHour % 12;
        int minute = life.curMinute;
        double ha = Math.toRadians((hour + minute / 60.0) * 30 - 90);
        double ma = Math.toRadians(minute * 6 - 90);
        g.setColor(0x2A2E38);
        int hr = Math.max(2, d / 2 - 3);
        g.drawLine(cx, cy, cx + (int) (Math.cos(ha) * hr),
                cy + (int) (Math.sin(ha) * hr));
        g.drawLine(cx, cy, cx + (int) (Math.cos(ma) * (hr + 1)),
                cy + (int) (Math.sin(ma) * (hr + 1)));
        // tick marks so the face reads as a clock even at 16 px
        g.fillRect(cx, y + 1, 1, 2);
        g.fillRect(cx, y + d - 3, 1, 2);
        g.fillRect(x + 1, cy, 2, 1);
        g.fillRect(x + d - 3, cy, 2, 1);
        g.setColor(0xC03A3A);
        g.fillRect(cx - 1, cy - 1, 2, 2);
    }

    private static void drawDoor(Gfx g, int x, int y, int w, int h, int night) {
        g.setColor(0x2A2018);
        g.fillRect(x - 2, y - 2, w + 4, h + 2);
        g.setColor(night == 1 ? 0x5A3A22 : 0x8A5A32);
        g.fillRect(x, y, w, h);
        g.setColor(night == 1 ? 0x43290F : 0x6B4423);
        g.fillRect(x + 3, y + 4, w - 6, h * 35 / 100);
        g.fillRect(x + 3, y + h * 50 / 100, w - 6, h * 35 / 100);
        g.setColor(0xFFD06A);
        g.fillRect(x + w - 6, y + h / 2, 2, 2);
        g.setColor(0x1A1410);
        g.fillRect(x + w / 2 - 1, y + h - 1, 3, 2);
    }

    private static void drawPlant(Gfx g, int x, int y, int size, LifeSim life) {
        g.setColor(0x7A4A2A);
        g.fillRect(x, y + size / 2, size, size / 2);
        g.setColor(0x3E9A5A);
        int wet = life.plantWater;
        int leaves = 3 + wet / 25;
        for (int i = 0; i < leaves; i++) {
            int lx = x + size / (leaves + 1) * (i + 1);
            int ly = y + size / 2 - (i % 2 == 0 ? size / 3 : size / 4);
            g.fillTriangle(lx, ly, lx - 3, ly + 5, lx + 3, ly + 5);
        }
    }

    /** 0xRRGGBB interpolation. */
    static int mix(int a, int b, int t) {
        int ar = (a >> 16) & 0xFF;
        int ag = (a >> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >> 16) & 0xFF;
        int bg = (b >> 8) & 0xFF;
        int bb = b & 0xFF;
        int r = ar + (br - ar) * t / 100;
        int gg = ag + (bg - ag) * t / 100;
        int bl = ab + (bb - ab) * t / 100;
        return (r << 16) | (gg << 8) | bl;
    }

    /* ------------------------------------------------------------------
     *  hotspot geometry used for the cursor highlight
     * ------------------------------------------------------------------ */

    /** Screen rectangle of a hotspot, in the current graphics size. */
    public static int[] rect(int hotspot, int w, int h) {
        int wallH = h * 55 / 100;
        int deskY = wallH + 4;
        int monW = w * 34 / 100;
        int monH = monW * 72 / 100;
        int bedY = wallH + h * 26 / 100;
        switch (hotspot) {
            case HOT_MONITOR:
                return new int[] { w * 5 / 100, deskY - monH - 3,
                        monW + 6, monH + 6 };
            case HOT_MUG:
                return new int[] { w * 55 / 100, deskY - h * 8 / 100,
                        w * 11 / 100, h * 9 / 100 };
            case HOT_CAT:
                return new int[] { w * 52 / 100, h * 70 / 100,
                        w * 22 / 100, h * 16 / 100 };
            case HOT_LAMP:
                return new int[] { w * 62 / 100, deskY - h * 18 / 100,
                        w * 16 / 100, h * 19 / 100 };
            case HOT_BED:
                return new int[] { w * 1 / 100, bedY - 2,
                        w * 42 / 100, h * 14 / 100 };
            case HOT_DOOR:
                return new int[] { w * 80 / 100, wallH - wallH * 66 / 100,
                        w * 18 / 100, wallH * 66 / 100 };
            case HOT_TOOLS:
                return new int[] { w * 6 / 100, deskY + h * 8 / 100,
                        w * 46 / 100, h * 8 / 100 };
            default: /* HOT_RADIO: the tower case on the desk */
                return new int[] { w * 36 / 100, deskY - monH - 3,
                        w * 16 / 100, monH + 4 };
        }
    }
}
