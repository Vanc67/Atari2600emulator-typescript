package rpc;

/**
 * Procedural picture of the city.
 *
 * 4.8 showed the city as a text list of eight locations.  Here the street is
 * drawn: buildings with windows and roofs, a shop front, market stalls, a
 * club with a neon sign, a cafe with an awning, the scrapyard, the post
 * office and the IT tower - and the era changes what you see: in 1979 the
 * town has low panel blocks, in the 1990s the signs go neon, after 2000 the
 * facades turn to glass and LEDs.  Weather, day/night, holidays and a
 * moving bus make it feel like a place.
 *
 * Everything is rectangles, lines and triangles - no image files.
 */
public final class CityArt {

    private CityArt() {
    }

    /** era index from the year: 0 before 1989, 1 the 90s, 2 the 2000s, 3 today */
    public static int era(int year) {
        if (year < 1989) {
            return 0;
        }
        if (year < 2000) {
            return 1;
        }
        if (year < 2012) {
            return 2;
        }
        return 3;
    }

    /** true when the whole scene should be dark */
    public static int night(LifeSim life) {
        return life.timeOfDay == LifeSim.TIME_NIGHT ? 1 : 0;
    }

    /** Draws the street with the facade of the given location. */
    public static void draw(Gfx g, LifeSim life, int loc, int frame) {
        int w = g.width();
        int h = g.height();
        int era = era(life.curYear);
        int night = night(life);

        // ---- sky (weather takes over when it rains) ----------------
        int skyTop = night == 1 ? 0x0E1730 : 0x7FB4E8;
        int skyBot = night == 1 ? 0x2A3C63 : 0xCFE6F7;
        if (life.weather == LifeSim.WEATHER_RAIN || life.weather == LifeSim.WEATHER_STORM) {
            skyTop = night == 1 ? 0x0A1124 : 0x5A6E86;
            skyBot = night == 1 ? 0x1B2A45 : 0x8C9AAE;
        } else if (life.weather == LifeSim.WEATHER_SNOW) {
            skyTop = night == 1 ? 0x14203A : 0x9FB6CE;
            skyBot = night == 1 ? 0x2B3A58 : 0xE4ECF4;
        }
        for (int y = 0; y < h * 62 / 100; y++) {
            g.setColor(mix(skyTop, skyBot, y * 100 / (h * 62 / 100)));
            g.fillRect(0, y, w, 1);
        }
        // distant haze band where the town starts
        g.setColor(night == 1 ? 0x1E2C48 : 0xD6E4EF);
        g.fillRect(0, h * 56 / 100, w, h * 4 / 100);

        // ---- far silhouettes (era dependent) ------------------------
        drawFar(g, w, h, era, night, life);

        // ---- street -------------------------------------------------
        int roadY = h * 60 / 100;
        int roadH = h * 26 / 100;
        int road = night == 1 ? 0x1A1E28 : 0x3A3D44;
        int roadHi = night == 1 ? 0x23272F : 0x4E5158;
        g.setColor(road);
        g.fillRect(0, roadY, w, roadH);
        g.setColor(roadHi);
        for (int x = 0; x < w; x += 20) {
            g.fillRect(x, roadY, 10, 2);
        }
        // pavement
        g.setColor(night == 1 ? 0x22262E : 0x6B6A66);
        g.fillRect(0, roadY - 12, w, 12);
        g.setColor(night == 1 ? 0x2B3038 : 0x8A887F);
        g.fillRect(0, roadY - 12, w, 2);
        // lane marker
        g.setColor(0xD8C34A);
        for (int x = 0; x < w; x += 30) {
            g.fillRect(x, roadY + roadH / 2 - 1, 16, 2);
        }

        // ---- buildings on the left ---------------------------------
        drawBlock(g, 4, roadY - 8, w * 20 / 100, h * 40 / 100, era, night,
                life, 0, frame);
        // ---- the facade of the chosen location ---------------------
        drawFacade(g, loc, era, night, life, frame, w, h, roadY);
        // ---- buildings on the right --------------------------------
        drawBlock(g, w - w * 20 / 100 - 4, roadY - 8, w * 20 / 100,
                h * 34 / 100, era, night, life, 1, frame);

        // ---- trees: the season shows up on the street --------------
        drawTrees(g, w, roadY, life, night, frame);
        // puddles when it rains
        if (life.weather == LifeSim.WEATHER_RAIN
                || life.weather == LifeSim.WEATHER_STORM) {
            g.setColor(night == 1 ? 0x2A3A52 : 0x6A7E96);
            for (int i = 0; i < 5; i++) {
                int px = 14 + i * (w - 30) / 4;
                g.fillRect(px, roadY + roadH * 30 / 100, 14, 3);
                g.fillRect(px + 3, roadY + roadH * 30 / 100 + 3, 8, 2);
            }
        }

        // ---- traffic and people -----------------------------------
        drawBus(g, w, roadY, roadH, frame, night);
        drawCar(g, w, roadY, roadH, frame, night);
        drawPeople(g, w, h, roadY, frame, night, life);

        // ---- weather on top ---------------------------------------
        drawWeather(g, w, h, life, frame);
    }

    private static void drawFar(Gfx g, int w, int h, int era, int night,
                                LifeSim life) {
        int base = h * 46 / 100;
        int tall = era >= 2 ? h * 30 / 100 : h * 14 / 100;
        g.setColor(night == 1 ? 0x172238 : 0xB8C6D6);
        int x = 2;
        while (x < w) {
            int bw = 12 + (x * 7) % 18;
            int bh = tall - (x % 3) * 5;
            g.fillRect(x, base - bh, bw, bh + 2);
            // windows of the far blocks
            g.setColor(night == 1 ? 0x3E4E70 : 0x93A6BC);
            for (int wy = base - bh + 4; wy < base - 2; wy += 5) {
                for (int wx = x + 2; wx < x + bw - 2; wx += 5) {
                    g.fillRect(wx, wy, 2, 2);
                }
            }
            if (life.season == LifeSim.SEASON_WINTER) {
                g.setColor(night == 1 ? 0xC8DCF0 : 0xE8F2FA);
                g.fillRect(x, base - bh, bw, 2);
                g.fillRect(x + bw - 3, base - bh - 4, 3, 4);
            }
            g.setColor(night == 1 ? 0x172238 : 0xB8C6D6);
            x += bw + 3;
        }
    }

    /** A side block: walls, windows, roof, antenna. */
    private static void drawBlock(Gfx g, int x, int y, int w, int h, int era,
                                  int night, LifeSim life, int side, int frame) {
        if (w < 20 || h < 20) {
            return;
        }
        int wall = night == 1 ? 0x2A3346 : (era == 0 ? 0xB9A48C : 0x9AA7B8);
        int wallHi = night == 1 ? 0x333D54 : (wall + 0x141414);
        g.setColor(wall);
        g.fillRect(x, y, w, h);
        g.setColor(wallHi);
        g.fillRect(x, y, w, 3);
        // roof
        g.setColor(night == 1 ? 0x1B2230 : 0x6B6255);
        g.fillRect(x - 2, y - 6, w + 4, 6);
        g.setColor(night == 1 ? 0x252E3D : 0x8A7E6C);
        g.fillRect(x - 2, y - 6, w + 4, 2);
        if (life.season == LifeSim.SEASON_WINTER) {
            g.setColor(night == 1 ? 0xC0D4EC : 0xEAF4FC);
            g.fillRect(x - 2, y - 7, w + 4, 2);
        }
        // chimney / antenna
        g.setColor(night == 1 ? 0x191F2B : 0x595046);
        g.fillRect(x + w * 70 / 100, y - 16, 2, 12);
        if (era >= 3) {
            g.setColor(night == 1 ? 0xFF6B6B : 0xE84A4A);
            g.fillRect(x + w * 70 / 100 + 1, y - 17, 1, 1);
        }
        // windows
        int cols = Math.max(2, w / 12);
        int rows = Math.max(2, h / 16);
        int ww = (w - 8) / cols - 3;
        int wh = (h - 10) / rows - 3;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int wx = x + 4 + c * (ww + 3);
                int wy = y + 6 + r * (wh + 3);
                boolean lit = night == 1
                        && ((r * 7 + c * 3 + (side + 1) * 11) % 5) < 3;
                g.setColor(lit ? (0xF2C86A) : (night == 1 ? 0x1C2436
                        : ((r + c + side) % 3 == 0 ? 0x8FB6D8 : 0xB7C6D8)));
                g.fillRect(wx, wy, ww, wh);
            }
        }
        // graffiti in the 2000s
        if (era >= 2 && w > 40) {
            g.setColor(night == 1 ? 0x4A5A78 : 0xE86A9A);
            g.fillRect(x + 3, y + h - 14, 10, 4);
            g.fillRect(x + 8, y + h - 16, 4, 8);
        }
    }

    /** The facade the player is standing in front of. */
    private static void drawFacade(Gfx g, int loc, int era, int night,
                                   LifeSim life, int frame, int w, int h,
                                   int roadY) {
        int fw = w * 46 / 100;
        int fx = (w - fw) / 2;
        int fh = roadY - 10;
        int fy = h * 6 / 100;
        if (fh < 40) {
            return;
        }
        boolean holiday = LifeSim.isHoliday(life);
        switch (loc) {
            case 1: // shop
                drawShop(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 2: // bazaar
                drawBazaar(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 3: // club
                drawClub(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 4: // cafe
                drawCafe(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 5: // scrapyard
                drawScrapyard(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 6: // post
                drawPost(g, fx, fy, fw, fh, era, night, frame);
                break;
            case 7: // office tower
                drawOffice(g, fx, fy, fw, fh, era, night, frame, w);
                break;
            default: // home
                drawHome(g, fx, fy, fw, fh, era, night, frame, holiday);
                break;
        }
    }

    private static void sign(Gfx g, int x, int y, int w, String text, int era,
                             int night, int color) {
        g.setColor(night == 1 ? 0x222A38 : 0x5A544B);
        g.fillRect(x, y, w, 10);
        g.setColor(color);
        g.fillRect(x + 1, y + 1, w - 2, 8);
        if (era >= 1 && w > 30) {
            // neon frame
            g.setColor(night == 1 ? color : 0x3A342B);
            g.fillRect(x, y, w, 1);
        }
    }

    private static void windowGrid(Gfx g, int x, int y, int w, int h, int era,
                                   int night) {
        int cols = Math.max(2, w / 16);
        int rows = Math.max(2, h / 18);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int wx = x + c * (w / cols);
                int wy = y + r * (h / rows);
                g.setColor(night == 1 ? ((r + c) % 3 == 0 ? 0xF0C260 : 0x222E42)
                        : 0xA8C8E0);
                g.fillRect(wx + 1, wy + 1, w / cols - 3, h / rows - 3);
            }
        }
    }

    private static void drawShop(Gfx g, int x, int y, int w, int h, int era,
                                 int night, int frame) {
        g.setColor(night == 1 ? 0x3A4152 : 0xCFC2A8);
        g.fillRect(x, y, w, h);
        g.setColor(night == 1 ? 0x454D60 : 0xE0D4BC);
        g.fillRect(x, y, w, 4);
        sign(g, x + 4, y + 6, w - 8, "", era, night, Theme.ACCENT);
        Text.draw(g, "МИТИНО", x + 8, y + 7, 0,
                night == 1 ? 0x1A1408 : Theme.TEXT);
        // big shop window with shelves and goods
        int wx = x + 4;
        int wy = y + 18;
        int ww = w - 8;
        int wh = h * 52 / 100;
        g.setColor(night == 1 ? 0x101724 : 0x6E8FA8);
        g.fillRect(wx, wy, ww, wh);
        g.setColor(night == 1 ? 0x2A3648 : 0x8FB8D8);
        for (int s = 0; s < 3; s++) {
            int sy = wy + wh * (s + 1) / 4;
            g.fillRect(wx + 1, sy, ww - 2, 2);
            // goods on the shelf
            int gx = wx + 4 + (s * 13 + frame / 8) % 6;
            for (int i = 0; i < 5; i++) {
                g.setColor((i * 37 + s) % 3 == 0 ? 0xE05A5A
                        : ((i + s) % 2 == 0 ? 0xF0C260 : 0x6BB060));
                g.fillRect(gx + i * 9, sy - 5, 7, 5);
            }
        }
        g.setColor(night == 1 ? 0x0E1420 : 0x39455A);
        g.fillRect(wx, wy + wh - 3, ww, 3);
        // door
        int dx = x + w / 2 - 8;
        g.setColor(night == 1 ? 0x1A2030 : 0x6B4A2E);
        g.fillRect(dx, y + h - 22, 16, 22);
        g.setColor(Theme.ACCENT);
        g.fillRect(dx + 12, y + h - 14, 2, 2);
        // signboard above the door
        g.setColor(night == 1 ? 0x2B3342 : 0x8A7A5E);
        g.fillRect(dx - 4, y + h - 26, 24, 5);
    }

    private static void drawBazaar(Gfx g, int x, int y, int w, int h, int era,
                                   int night, int frame) {
        g.setColor(night == 1 ? 0x2E3546 : 0xD8CCB0);
        g.fillRect(x, y, w, h);
        // fence
        g.setColor(night == 1 ? 0x242B3A : 0x8A8474);
        for (int i = 0; i < w / 6; i++) {
            g.fillRect(x + i * 6, y + 4, 2, h - 6);
        }
        g.fillRect(x, y + 4, w, 2);
        g.fillRect(x, y + h - 8, w, 2);
        // three stalls with striped canopies
        for (int s = 0; s < 3; s++) {
            int sx = x + 6 + s * (w - 12) / 3;
            int sw = (w - 18) / 3;
            int sy = y + 16;
            int sh = h * 34 / 100;
            g.setColor(night == 1 ? 0x39404F : 0xB08A5A);
            g.fillRect(sx, sy + 8, sw, sh);
            g.setColor(night == 1 ? 0x556076 : 0xD86A5A);
            for (int i = 0; i < sw / 6; i++) {
                g.fillRect(sx + i * 6, sy, 6, 8);
                if (i % 2 == 1) {
                    g.setColor(night == 1 ? 0x3A4256 : 0xF0E2C0);
                    g.fillRect(sx + i * 6, sy, 6, 8);
                } else {
                    g.setColor(night == 1 ? 0x556076 : 0xD86A5A);
                }
            }
            // goods on the counter
            for (int i = 0; i < 4; i++) {
                g.setColor(night == 1 ? 0x4A5570 : (0x5A9AE0 + i * 0x0A0A0A));
                g.fillRect(sx + 3 + i * (sw - 8) / 4, sy + 10,
                        (sw - 8) / 4 - 3, 8);
            }
            g.setColor(night == 1 ? 0x2A3140 : 0x7A6A4E);
            g.fillRect(sx, sy + sh, sw, 3);
        }
        // price tags
        g.setColor(night == 1 ? 0xE8D06A : 0xE8D06A);
        g.fillRect(x + 10, y + 8, 14, 5);
        g.fillRect(x + w - 26, y + 8, 14, 5);
    }

    private static void drawClub(Gfx g, int x, int y, int w, int h, int era,
                                 int night, int frame) {
        g.setColor(night == 1 ? 0x2C2A3A : 0xA89AA0);
        g.fillRect(x, y, w, h);
        // brick texture
        g.setColor(night == 1 ? 0x333044 : 0xB8ACB4);
        for (int r = 0; r < h / 9; r++) {
            for (int c = 0; c < w / 9; c++) {
                if ((r + c) % 2 == 0) {
                    g.fillRect(x + c * 9 + 1, y + r * 9, 8, 8);
                }
            }
        }
        // neon sign
        int sx = x + w / 2 - 22;
        int sy = y + 6;
        int pulse = (frame / 10) % 2;
        int col = night == 1 ? (pulse == 0 ? 0xFF5A8A : 0x8A2A4A) : 0xFF5A8A;
        g.setColor(night == 1 ? 0x15101C : 0x3A2A34);
        g.fillRect(sx - 4, sy - 4, 52, 20);
        g.setColor(col);
        g.fillRect(sx, sy, 44, 12);
        Text.draw(g, "КЛУБ", sx + 6, sy + 2, 0, night == 1 ? 0xFFE0EC : 0x2A0A14);
        // door with a light strip
        g.setColor(night == 1 ? 0x1A1220 : 0x5A3A44);
        g.fillRect(x + w / 2 - 10, y + h - 26, 20, 26);
        g.setColor(night == 1 ? (pulse == 0 ? 0xFF7AAA : 0x4A1A30) : 0xFF9AB0);
        g.fillRect(x + w / 2 - 8, y + h - 26, 16, 3);
        // speaker boxes on the wall
        g.setColor(night == 1 ? 0x1A1A24 : 0x3A3A44);
        g.fillRect(x + 8, y + h - 34, 10, 14);
        g.fillRect(x + w - 18, y + h - 34, 10, 14);
    }

    private static void drawCafe(Gfx g, int x, int y, int w, int h, int era,
                                 int night, int frame) {
        g.setColor(night == 1 ? 0x33404A : 0xD8C8A8);
        g.fillRect(x, y, w, h);
        // awning with stripes
        g.setColor(night == 1 ? 0x4A5A68 : 0x8A5A4A);
        g.fillRect(x + 2, y + 6, w - 4, 12);
        for (int i = 0; i < (w - 4) / 8; i++) {
            if (i % 2 == 0) {
                g.setColor(night == 1 ? 0x5A6A78 : 0xF0E0C8);
                g.fillRect(x + 2 + i * 8, y + 6, 8, 12);
            }
        }
        g.setColor(night == 1 ? 0x2A333C : 0x6A4A38);
        for (int i = 0; i < 6; i++) {
            g.fillRect(x + 6 + i * (w - 12) / 5, y + 18, 2, 12);
        }
        // cup sign
        int cx = x + w / 2 - 10;
        g.setColor(night == 1 ? 0xE8D06A : 0xC08A3A);
        g.fillRect(cx, y + 24, 20, 6);
        g.fillRect(cx + 2, y + 30, 16, 8);
        g.fillRect(cx + 18, y + 31, 4, 4);
        // window with a table and a plant outside
        int wx = x + 6;
        int wy = y + h * 40 / 100;
        g.setColor(night == 1 ? 0x141C26 : 0x7FA8C0);
        g.fillRect(wx, wy, w - 12, h * 30 / 100);
        g.setColor(night == 1 ? 0x3A4654 : 0xC09A6A);
        g.fillRect(wx + 6, wy + 14, 16, 3);
        g.setColor(night == 1 ? 0x4A5A68 : 0x5A8A5A);
        g.fillRect(wx + w - 24, wy + 4, 8, 14);
        // door
        g.setColor(night == 1 ? 0x222A34 : 0x6A4A2E);
        g.fillRect(x + w / 2 - 8, y + h - 24, 16, 24);
        g.setColor(night == 1 ? 0xFFD06A : 0xFFC24B);
        g.fillRect(x + w / 2 - 2, y + h - 14, 3, 3);
    }

    private static void drawScrapyard(Gfx g, int x, int y, int w, int h,
                                      int era, int night, int frame) {
        g.setColor(night == 1 ? 0x2A2E33 : 0x9A8E7A);
        g.fillRect(x, y, w, h);
        // corrugated fence
        g.setColor(night == 1 ? 0x353A40 : 0x7A6E5A);
        g.fillRect(x, y + 6, w, h * 24 / 100);
        for (int i = 0; i < w / 6; i++) {
            g.setColor(night == 1 ? 0x2A2E33 : 0x6A604E);
            g.fillRect(x + i * 6, y + 6, 1, h * 24 / 100);
        }
        // piles of parts
        int[][] piles = { { 6, 40, 18, 14 }, { 30, 52, 14, 10 },
                          { 52, 36, 16, 12 } };
        for (int p = 0; p < piles.length; p++) {
            int px = x + piles[p][0];
            int py = y + piles[p][1];
            g.setColor(night == 1 ? 0x333A40 : 0x6A6252);
            g.fillRect(px, py, piles[p][2], piles[p][3]);
            g.setColor(night == 1 ? 0x454D54 : 0x8A7E6A);
            g.fillRect(px + 2, py, piles[p][2] - 4, 3);
            // a fan in the pile
            g.setColor(night == 1 ? 0x555E66 : 0xA0A8B0);
            g.fillRect(px + piles[p][2] - 8, py - 6, 8, 8);
            g.fillRect(px + piles[p][2] - 4, py - 8, 4, 4);
        }
        // a container
        g.setColor(night == 1 ? 0x3A4A4A : 0x4A6A6A);
        g.fillRect(x + w * 60 / 100, y + h * 40 / 100, w * 30 / 100,
                h * 26 / 100);
        g.setColor(night == 1 ? 0x4A5C5C : 0x6A8C8C);
        for (int i = 0; i < 4; i++) {
            g.fillRect(x + w * 60 / 100 + 2, y + h * 40 / 100 + i * 8,
                    w * 30 / 100 - 4, 2);
        }
        // a cat on the fence
        g.setColor(night == 1 ? 0x1A1A20 : 0x3A3028);
        g.fillRect(x + w * 40 / 100, y + 2, 10, 5);
        g.fillRect(x + w * 40 / 100 + 3, y - 3, 4, 4);
    }

    private static void drawPost(Gfx g, int x, int y, int w, int h, int era,
                                 int night, int frame) {
        g.setColor(night == 1 ? 0x3A3F4A : 0xCFC6B4);
        g.fillRect(x, y, w, h);
        // roof with a spire
        g.setColor(night == 1 ? 0x2A2F38 : 0x6A5E4A);
        g.fillRect(x - 3, y - 6, w + 6, 6);
        g.fillRect(x + w / 2 - 2, y - 14, 4, 9);
        // sign
        g.setColor(night == 1 ? 0x141A22 : 0x2A4A8A);
        g.fillRect(x + w / 2 - 20, y + 8, 40, 12);
        Text.draw(g, "ПОЧТА", x + w / 2 - 14, y + 10, 0,
                night == 1 ? 0xD8E4F8 : 0xF0F6FF);
        // big window with a queue
        int wx = x + 8;
        int wy = y + 26;
        int ww = w - 16;
        int wh = h * 38 / 100;
        g.setColor(night == 1 ? 0x101724 : 0x8FB8D8);
        g.fillRect(wx, wy, ww, wh);
        g.setColor(night == 1 ? 0x3A4658 : 0xC09A6A);
        for (int i = 0; i < 3; i++) {
            g.fillRect(wx + 4, wy + wh - 12 - i * 12, 6, 8);
        }
        // a queue of people at the window
        for (int i = 0; i < 3; i++) {
            int qx = wx + 6 + i * (ww - 14) / 3;
            g.setColor(night == 1 ? 0x4A5570 : 0x4A6A9A);
            g.fillRect(qx, wy + wh - 16, 6, 10);
            g.setColor(night == 1 ? 0xE0C08A : 0xF0C8A0);
            g.fillRect(qx, wy + wh - 20, 6, 6);
        }
        // mailbox
        g.setColor(night == 1 ? 0x8A3A3A : 0xC03A3A);
        g.fillRect(x + 10, y + h - 22, 12, 16);
        g.setColor(night == 1 ? 0x3A1010 : 0x7A1A1A);
        g.fillRect(x + 12, y + h - 18, 8, 2);
        // door
        g.setColor(night == 1 ? 0x222A34 : 0x5A4A32);
        g.fillRect(x + w / 2 - 8, y + h - 24, 16, 24);
    }

    private static void drawOffice(Gfx g, int x, int y, int w, int h, int era,
                                   int night, int frame, int screenW) {
        int towerH = h * 88 / 100;
        g.setColor(night == 1 ? 0x2A3548 : 0x9FB4C8);
        g.fillRect(x, y + h - towerH, w, towerH);
        // glass grid
        int cols = Math.max(3, w / 12);
        int rows = Math.max(4, towerH / 16);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int wx = x + c * (w / cols);
                int wy = y + h - towerH + r * (towerH / rows);
                boolean lit = night == 1 && ((r * 5 + c * 3) % 4) < 2;
                g.setColor(lit ? 0xF0C86A : (night == 1 ? 0x1B2538 : 0xB8CFE0));
                g.fillRect(wx + 1, wy + 1, w / cols - 3, towerH / rows - 3);
            }
        }
        // roof antenna with a blinking light
        g.setColor(night == 1 ? 0x1E2735 : 0x6A7078);
        g.fillRect(x + w / 2 - 2, y + h - towerH - 16, 4, 16);
        if ((frame / 16) % 2 == 0) {
            g.setColor(0xE84A4A);
            g.fillRect(x + w / 2 - 1, y + h - towerH - 18, 2, 2);
        }
        // LED billboard
        int bx = x + w / 2 - 18;
        int by = y + h - towerH + 6;
        int pulse = (frame / 10) % 2;
        g.setColor(night == 1 ? 0x0A1220 : 0x2A3A48);
        g.fillRect(bx, by, 36, 14);
        g.setColor(pulse == 0 ? 0x4AE0C0 : 0x2A8A7A);
        g.fillRect(bx + 2, by + 2, 32, 10);
        Text.draw(g, "IT-KORP", bx + 6, by + 4, 0,
                night == 1 ? 0x0A2018 : 0x062A20);
        // entrance
        g.setColor(night == 1 ? 0x1A2230 : 0x4A5A6A);
        g.fillRect(x + w / 2 - 12, y + h - 20, 24, 20);
        g.setColor(night == 1 ? 0x4AE0C0 : 0x6AF0D8);
        g.fillRect(x + w / 2 - 10, y + h - 18, 20, 2);
    }

    private static void drawHome(Gfx g, int x, int y, int w, int h, int era,
                                 int night, int frame, boolean holiday) {
        g.setColor(night == 1 ? 0x3A4A4A : 0xC8B89A);
        g.fillRect(x, y + h * 30 / 100, w, h * 70 / 100);
        // roof
        g.setColor(night == 1 ? 0x242C36 : 0x7A4A3A);
        g.fillRect(x - 4, y + h * 26 / 100, w + 8, h * 8 / 100);
        g.setColor(night == 1 ? 0x333D48 : 0x9A6A52);
        g.fillRect(x - 4, y + h * 26 / 100, w + 8, 2);
        // chimney
        g.setColor(night == 1 ? 0x1E2630 : 0x6A4A3A);
        g.fillRect(x + w * 70 / 100, y + h * 14 / 100, 6, h * 14 / 100);
        // windows
        windowGrid(g, x + 6, y + h * 44 / 100, w * 34 / 100, h * 18 / 100,
                era, night);
        windowGrid(g, x + w * 52 / 100, y + h * 44 / 100, w * 34 / 100,
                h * 18 / 100, era, night);
        // door
        g.setColor(night == 1 ? 0x222A34 : 0x6A4A2E);
        g.fillRect(x + w / 2 - 7, y + h - 22, 14, 22);
        g.setColor(night == 1 ? 0xE8D06A : 0xC09A3A);
        g.fillRect(x + w / 2 + 2, y + h - 14, 2, 2);
        // a New Year tree during the holidays
        if (holiday) {
            g.setColor(0x2A5A3A);
            g.fillRect(x + w * 20 / 100, y + h * 62 / 100, 8, 16);
            g.fillRect(x + w * 20 / 100 - 4, y + h * 52 / 100, 16, 12);
            g.setColor(0xE84A4A);
            g.fillRect(x + w * 20 / 100 + 3, y + h * 54 / 100, 2, 2);
        }
    }

    /** A bus driving along the road. */
    private static void drawBus(Gfx g, int w, int roadY, int roadH, int frame,
                                int night) {
        int x = (frame * 2 - 120) % (w + 60) - 30;
        int y = roadY + roadH * 20 / 100;
        int bw = 34;
        int bh = 16;
        g.setColor(night == 1 ? 0x1A1E28 : 0x2A3A5A);
        g.fillRect(x, y - bh, bw, bh);
        g.setColor(night == 1 ? 0x3A4658 : 0x6FA8D8);
        g.fillRect(x + 3, y - bh + 3, bw - 8, bh - 8);
        g.setColor(night == 1 ? 0x222A34 : 0x1A2A3A);
        g.fillRect(x + 4, y - 4, bw - 8, 4);
        g.setColor(night == 1 ? 0x2A3340 : 0x1A2230);
        g.fillRect(x + 2, y, 8, 4);
        g.fillRect(x + bw - 10, y, 8, 4);
        g.setColor(0xF0C86A);
        g.fillRect(x + bw - 6, y - bh + 4, 3, 3);
    }

    private static void drawCar(Gfx g, int w, int roadY, int roadH, int frame,
                                int night) {
        int x = (frame * 3 + 260) % (w + 40) - 20;
        int y = roadY + roadH * 58 / 100;
        g.setColor(night == 1 ? 0x2A3340 : 0x8A3A3A);
        g.fillRect(x, y - 8, 20, 8);
        g.fillRect(x + 4, y - 13, 11, 6);
        g.setColor(night == 1 ? 0x1A2230 : 0x142030);
        g.fillRect(x + 5, y - 11, 9, 3);
        g.setColor(0xF8F0C8);
        g.fillRect(x + 17, y - 6, 3, 3);
        g.fillRect(x, y - 3, 3, 3);
    }

    private static void drawPeople(Gfx g, int w, int h, int roadY, int frame,
                                   int night, LifeSim life) {
        // a seller near the market
        int sx = w * 30 / 100 + (frame / 30) % 3;
        int sy = roadY - 14;
        g.setColor(night == 1 ? 0x3A4658 : 0x4A5A8A);
        g.fillRect(sx, sy - 10, 6, 10);
        g.setColor(night == 1 ? 0xE0C08A : 0xF0C8A0);
        g.fillRect(sx, sy - 15, 6, 6);
        g.setColor(night == 1 ? 0x2A3340 : 0x6A4A3A);
        g.fillRect(sx - 4, sy - 2, 14, 2);
        // a passer-by walking
        int px = (frame / 5) % (w + 30) - 15;
        int py = roadY - 12;
        g.setColor(night == 1 ? 0x4A3A5A : 0x7A3A5A);
        g.fillRect(px, py - 10, 5, 10);
        g.setColor(night == 1 ? 0xE0C08A : 0xF0C8A0);
        g.fillRect(px, py - 14, 5, 5);
        // a cat crossing
        g.setColor(night == 1 ? 0x1A1A20 : 0x3A3028);
        int cx = (frame / 7 + 180) % (w + 20) - 10;
        g.fillRect(cx, roadY - 4, 8, 4);
        g.fillRect(cx + 2, roadY - 8, 3, 4);
    }

    private static void drawWeather(Gfx g, int w, int h, LifeSim life,
                                    int frame) {
        if (life.weather == LifeSim.WEATHER_RAIN
                || life.weather == LifeSim.WEATHER_STORM) {
            g.setColor(life.weather == LifeSim.WEATHER_STORM
                    ? 0xAFC4E8 : 0x8FB4DC);
            for (int i = 0; i < 40; i++) {
                int rx = (i * 31 + frame * 4) % w;
                int ry = (i * 19 + frame * 12) % h;
                g.drawLine(rx, ry, rx - 2, ry + 6);
            }
            if (life.weather == LifeSim.WEATHER_STORM
                    && (frame / 12) % 3 == 0) {
                g.setColor(0xE8F0FF);
                g.fillRect(0, 0, w, h);
            }
        } else if (life.weather == LifeSim.WEATHER_SNOW) {
            g.setColor(0xFFFFFF);
            for (int i = 0; i < 36; i++) {
                int rx = (i * 37 + frame * 2) % w;
                int ry = (i * 21 + frame * 3) % h;
                g.fillRect(rx, ry, 2, 2);
            }
        } else if (life.weather == LifeSim.WEATHER_LEAVES) {
            g.setColor(0xC88A3A);
            for (int i = 0; i < 14; i++) {
                int rx = (i * 41 + frame * 3) % w;
                int ry = (i * 23 + frame * 4) % h;
                g.fillRect(rx, ry, 3, 2);
            }
        }
    }

    private static int mix(int a, int b, int t) {
        if (t < 0) {
            t = 0;
        }
        if (t > 100) {
            t = 100;
        }
        int r = ((a >> 16) & 0xFF) * (100 - t) + ((b >> 16) & 0xFF) * t;
        int gg = ((a >> 8) & 0xFF) * (100 - t) + ((b >> 8) & 0xFF) * t;
        int bb = (a & 0xFF) * (100 - t) + (b & 0xFF) * t;
        r /= 100;
        gg /= 100;
        bb /= 100;
        return (r << 16) | (gg << 8) | bb;
    }

    /** Trees along the pavement; the crown is the season's colour. */
    private static void drawTrees(Gfx g, int w, int roadY, LifeSim life,
                                  int night, int frame) {
        int seasons = life.season;
        int trunk = night == 1 ? 0x2A2119 : 0x6A4A2E;
        int[] crowns = { 0x7A8A5A, 0x9AD07A, 0x4A9A4A, 0xD88A3A };
        int crown = crowns[seasons];
        for (int t = 0; t < 3; t++) {
            int tx = w * (12 + t * 32) / 100;
            int ty = roadY - 30;
            g.setColor(trunk);
            g.fillRect(tx, ty, 5, 22);
            if (seasons == LifeSim.SEASON_WINTER) {
                // bare branches with snow on top
                g.setColor(night == 1 ? 0x4A4038 : 0x7A6A4A);
                g.fillRect(tx - 6, ty - 4, 17, 3);
                g.fillRect(tx - 3, ty - 8, 11, 3);
                g.setColor(night == 1 ? 0xD0E2F4 : 0xEAF4FC);
                g.fillRect(tx - 6, ty - 5, 17, 2);
                g.fillRect(tx - 3, ty - 9, 11, 2);
            } else {
                g.setColor(crown);
                g.fillRect(tx - 8, ty - 12, 18, 14);
                g.fillRect(tx - 6, ty - 16, 13, 6);
                g.fillRect(tx - 9, ty - 8, 4, 8);
                g.fillRect(tx + 5, ty - 8, 4, 8);
                if (seasons == LifeSim.SEASON_AUTUMN) {
                    g.setColor(0xE86A3A);
                    g.fillRect(tx - 6, ty - 14, 4, 4);
                    g.fillRect(tx + 3, ty - 10, 4, 4);
                }
                if (seasons == LifeSim.SEASON_SPRING) {
                    g.setColor(0xE8A8C8);
                    g.fillRect(tx - 7, ty - 12, 3, 3);
                    g.fillRect(tx + 3, ty - 9, 3, 3);
                }
            }
        }
    }
}
