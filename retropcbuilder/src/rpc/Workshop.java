package rpc;

/**
 * Instrument panel of the workshop screen.
 *
 * The workshop mechanics (pins, capacitors, thermal paste, dust, voltages)
 * already existed in {@link WorkshopTools}, but 4.8 drew them as plain text.
 * Each tool now gets a real instrument:
 *
 *   1 tweezers  - CPU pin grid, bent pins highlighted red
 *   2 soldering - motherboard with five capacitors, bulged ones on top
 *   3 paste     - a thickness gauge with the 40-60 % sweet spot marked
 *   4 air       - a dust bar that shrinks while you blow it out
 *   5 meter     - an oscilloscope with a ripple trace and live voltages
 *   6 CMOS      - a battery / BIOS chip with the reset state
 */
public final class Workshop {

    private Workshop() {
    }

    public static void draw(Gfx g, WorkshopTools ws, int x, int y, int w, int h,
                            int frame) {
        switch (ws.activeTool) {
            case WorkshopTools.TOOL_TWEEZERS:
                drawPins(g, ws, x, y, w, h);
                break;
            case WorkshopTools.TOOL_SOLDER:
                drawCaps(g, ws, x, y, w, h, frame);
                break;
            case WorkshopTools.TOOL_PASTE:
                drawPaste(g, ws, x, y, w, h);
                break;
            case WorkshopTools.TOOL_AIR:
                drawAir(g, ws, x, y, w, h, frame);
                break;
            case WorkshopTools.TOOL_METER:
                drawScope(g, ws, x, y, w, h, frame);
                break;
            default:
                drawCmos(g, ws, x, y, w, h, frame);
                break;
        }
    }

    /* ---- 1. CPU pins ------------------------------------------------ */
    private static void drawPins(Gfx g, WorkshopTools ws, int x, int y, int w,
                                 int h) {
        Text.small(g, "\u0421\u0435\u0442\u043a\u0430 \u043d\u043e\u0436\u0435\u043a", x + 5, y + 4,
                Theme.ACCENT);
        int bentCount = 0;
        for (int i = 0; i < ws.cpuPins.length; i++) {
            if (ws.cpuPins[i] != 0) {
                bentCount++;
            }
        }
        Text.small(g, "погнуто: " + bentCount, x + 5, y + 14,
                bentCount > 0 ? Theme.RED : Theme.GREEN);
        int cols = 8;
        int rows = (ws.cpuPins.length + cols - 1) / cols;
        int cell = Math.min((w - 20) / cols, (h - 60) / rows);
        if (cell < 6) {
            cell = 6;
        }
        int ox = x + (w - cols * cell) / 2;
        int oy = y + 26 + (h - 60 - rows * cell) / 2;
        for (int i = 0; i < ws.cpuPins.length; i++) {
            int px = ox + (i % cols) * cell;
            int py = oy + (i / cols) * cell;
            boolean bent = ws.cpuPins[i] != 0;
            g.setColor(bent ? Theme.RED : Theme.STEEL);
            g.fillRect(px + 1, py + 1, cell - 3, cell - 3);
            if (i == ws.selPin) {
                g.setColor(Theme.ACCENT);
                g.drawRect(px, py, cell - 1, cell - 1);
            }
        }
        Text.small(g, "\u0432\u044b\u043f\u0440\u044f\u043c\u0438\u0442\u044c: OK", x + 5,
                y + h - 11, Theme.MUTED);
    }

    /* ---- 2. capacitors ---------------------------------------------- */
    private static void drawCaps(Gfx g, WorkshopTools ws, int x, int y, int w,
                                 int h, int frame) {
        Text.small(g, "\u041a\u043e\u043d\u0434\u0435\u043d\u0441\u0430\u0442\u043e\u0440\u044b", x + 5, y + 4,
                Theme.ACCENT);
        int badCount = 0;
        for (int i = 0; i < ws.caps.length; i++) {
            if (ws.caps[i] != 0) {
                badCount++;
            }
        }
        Text.small(g, "вздуто: " + badCount, x + 5, y + 14,
                badCount > 0 ? Theme.ORANGE : Theme.GREEN);
        // board
        int by = y + h - 26;
        g.setColor(0x1E4A2A);
        g.fillRect(x + 6, by, w - 12, 20);
        g.setColor(0x2E6B3E);
        g.fillRect(x + 6, by, w - 12, 2);
        int n = ws.caps.length;
        int cw = Math.min(20, (w - 24) / n);
        for (int i = 0; i < n; i++) {
            int cx = x + 12 + i * cw;
            int ch = Math.min(70, h - 70);
            boolean bad = ws.caps[i] != 0;
            // body
            g.setColor(bad ? Theme.ORANGE : 0x3A5A8A);
            g.fillRect(cx, by - ch, cw - 4, ch);
            g.setColor(bad ? 0xFFE0A0 : 0x8AB0E0);
            g.fillRect(cx, by - ch, cw - 4, 2);
            // vent cross
            g.setColor(0x1A1E28);
            g.drawLine(cx + (cw - 4) / 2, by - ch + 3, cx + (cw - 4) / 2,
                    by - ch + 9);
            g.drawLine(cx + 1, by - ch + 6, cx + cw - 5, by - ch + 6);
            if (bad) {
                // bulged top
                g.setColor(Theme.ORANGE);
                g.fillRect(cx - 1, by - ch - 3, cw - 2, 3);
                g.setColor(0xFFA040);
                g.fillRect(cx + 1, by - ch - 4, cw - 6, 2);
            }
            if (i == ws.selCap) {
                g.setColor(Theme.ACCENT);
                g.drawRect(cx - 2, by - ch - 5, cw, ch + 6);
            }
        }
        Text.small(g, "\u0437\u0430\u043c\u0435\u043d\u0438\u0442\u044c: OK  \u2190\u2192 \u0432\u044b\u0431\u043e\u0440", x + 5,
                y + h - 11, Theme.MUTED);
    }

    /* ---- 3. thermal paste ------------------------------------------- */
    private static void drawPaste(Gfx g, WorkshopTools ws, int x, int y, int w,
                                  int h) {
        Text.small(g, "\u0422\u0435\u0440\u043c\u043e\u043f\u0430\u0441\u0442\u0430 \u041a\u041f\u0422-8", x + 5, y + 4,
                Theme.ACCENT);
        // CPU + heatsink seen from the side
        int cpuY = y + h / 2;
        int cpuW = w - 30;
        g.setColor(0x3A4152);
        g.fillRect(x + 15, cpuY + 6, cpuW, 10);
        g.setColor(0x8A90A0);
        g.fillRect(x + 15, cpuY + 6, cpuW, 2);
        // paste layer
        int ph = 2 + ws.pasteThickness / 20;
        int good = ws.pasteThickness >= 40 && ws.pasteThickness <= 60 ? 1 : 0;
        g.setColor(good == 1 ? Theme.GREEN : Theme.ORANGE);
        g.fillRect(x + 18, cpuY + 6 - ph, cpuW - 6, ph);
        // heatsink
        g.setColor(0xB0B6C4);
        g.fillRect(x + 15, cpuY - 14, cpuW, 6);
        for (int i = 0; i < 6; i++) {
            g.setColor(0x8A90A0);
            g.fillRect(x + 18 + i * (cpuW - 12) / 6, cpuY - 24, 4, 12);
        }
        // gauge
        int gx = x + 12;
        int gy = y + h - 20;
        int gw = w - 24;
        g.setColor(Theme.BG);
        g.fillRect(gx, gy, gw, 8);
        g.setColor(0x4A7A4A);
        g.fillRect(gx + gw * 40 / 100, gy, gw * 20 / 100, 8);
        g.setColor(good == 1 ? Theme.GREEN : Theme.ORANGE);
        g.fillRect(gx + gw * Math.min(100, ws.pasteThickness) / 100 - 1, gy - 2,
                3, 12);
        g.setColor(Theme.LINE);
        g.drawRect(gx, gy, gw, 8);
        Text.small(g, ws.pasteThickness + "%", gx + 2, gy - 10, Theme.TEXT);
    }

    /* ---- 4. compressed air ------------------------------------------ */
    private static void drawAir(Gfx g, WorkshopTools ws, int x, int y, int w,
                                int h, int frame) {
        Text.small(g, "\u041f\u044b\u043b\u044c в радиаторе", x + 5, y + 4,
                Theme.ACCENT);
        Text.small(g, ws.dustRemaining + "%", x + w - 40, y + 4,
                ws.dustRemaining > 20 ? Theme.ORANGE : Theme.GREEN);
        // radiator fins
        int rx = x + 8;
        int ry = y + 20;
        int rw = w - 16;
        int rh = Math.min(60, h - 50);
        int fins = 14;
        for (int i = 0; i < fins; i++) {
            int fx = rx + i * rw / fins;
            g.setColor(0x6E7482);
            g.fillRect(fx, ry, Math.max(1, rw / fins - 2), rh);
        }
        // dust layer on top of the fins
        int dustH = rh * ws.dustRemaining / 100;
        g.setColor(0x8A7A5A);
        g.fillRect(rx, ry + rh - dustH, rw, dustH);
        g.setColor(0xB0A080);
        for (int i = 0; i < 20; i++) {
            g.fillRect(rx + (i * 37) % rw, ry + rh - 2, 2, 1);
        }
        // air puffs
        if ((frame / 4) % 2 == 0) {
            g.setColor(0xC8D2E0);
            for (int i = 0; i < 5; i++) {
                g.fillRect(rx + rw + 2 + i * 3, ry + 6 + i * 5, 3, 2);
            }
        }
        Text.small(g, ws.dustRemaining + "%  \u043f\u0440\u043e\u0434\u0443\u0442\u044c: OK", x + 5,
                ry + rh + 6, Theme.TEXT);
    }

    /* ---- 5. multimeter / oscilloscope ------------------------------- */
    private static void drawScope(Gfx g, WorkshopTools ws, int x, int y, int w,
                                  int h, int frame) {
        Text.small(g, "\u041c\u0443\u043b\u044c\u0442\u0438\u043c\u0435\u0442\u0440", x + 5, y + 4,
                Theme.ACCENT);
        Text.small(g, "+12V " + fmt(ws.v12) + "  +5V " + fmt(ws.v5)
                + "  +3.3V " + fmt(ws.v33), x + 5, y + 15, Theme.TEXT);
        Text.small(g, "VCore " + fmt(ws.vCore) + "  \u043f\u0443\u043b\u044c\u0441\u0430\u0446\u0438\u0438 "
                + ws.rippleMv + " \u043c\u0412", x + 5, y + 24,
                ws.rippleMv > 150 ? Theme.RED : Theme.GREEN);
        // scope
        int sx = x + 8;
        int sy = y + 34;
        int sw = w - 16;
        int sh = Math.min(50, h - 48);
        g.setColor(0x06120A);
        g.fillRect(sx, sy, sw, sh);
        g.setColor(0x143818);
        for (int i = 1; i < 4; i++) {
            g.drawLine(sx, sy + sh * i / 4, sx + sw, sy + sh * i / 4);
        }
        g.setColor(ws.rippleMv > 150 ? Theme.RED : 0x63E6A0);
        int prev = sy + sh / 2;
        for (int i = 0; i < sw; i++) {
            int phase = (i * 360 / 40 + frame * 6) % 360;
            int amp = sh / 3;
            int v = sy + sh / 2 + (int) (Math.sin(Math.toRadians(phase)) * amp);
            if (i > 0) {
                g.drawLine(sx + i - 1, prev, sx + i, v);
            }
            prev = v;
        }
        Text.small(g, "\u043f\u0440\u043e\u0432\u0435\u0440\u0438\u0442\u044c: OK", x + 5, sy + sh + 4,
                Theme.MUTED);
    }

    /* ---- 6. CMOS ---------------------------------------------------- */
    private static void drawCmos(Gfx g, WorkshopTools ws, int x, int y, int w,
                                 int h, int frame) {
        Text.small(g, "CMOS / BIOS", x + 5, y + 4, Theme.ACCENT);
        // battery
        int bx = x + 12;
        int by = y + 22;
        g.setColor(0xC8CEDC);
        g.fillRoundRect(bx, by, 34, 12, 4);
        g.setColor(0x3A4152);
        g.fillRect(bx + 34, by + 3, 3, 6);
        Text.small(g, "CR2032", bx + 3, by + 3, 0x1A1E28);
        // BIOS chip
        int chx = x + 60;
        g.setColor(0x2A2E38);
        g.fillRect(chx, by - 4, 40, 20);
        g.setColor(0x4A5460);
        g.fillRect(chx + 2, by - 2, 36, 16);
        for (int i = 0; i < 6; i++) {
            g.setColor(0xB0B6C4);
            g.fillRect(chx + 4 + i * 6, by - 6, 3, 3);
            g.fillRect(chx + 4 + i * 6, by + 15, 3, 3);
        }
        Text.small(g, ws.cmosResetDone ? "\u0441\u0431\u0440\u043e\u0448\u0435\u043d\u043e"
                : "\u0441\u0431\u0440\u043e\u0441\u0438\u0442\u044c: OK", x + 5, by + 24,
                ws.cmosResetDone ? Theme.GREEN : Theme.MUTED);
        // spark when resetting
        if (ws.cmosResetDone && (frame / 3) % 6 == 0) {
            g.setColor(Theme.ACCENT2);
            g.drawLine(bx + 17, by - 2, bx + 22, by - 8);
            g.drawLine(bx + 22, by - 8, bx + 19, by - 12);
        }
    }

    /** Short label for the tool column. */
    public static String shortName(int tool) {
        switch (tool) {
            case WorkshopTools.TOOL_TWEEZERS: return "Пинцет";
            case WorkshopTools.TOOL_SOLDER: return "Паяльник";
            case WorkshopTools.TOOL_PASTE: return "Паста";
            case WorkshopTools.TOOL_AIR: return "Воздух";
            case WorkshopTools.TOOL_METER: return "Мультиметр";
            default: return "CMOS";
        }
    }

    /** 0..100 how much of the repair job is done. */
    public static int progress(WorkshopTools ws) {
        int parts = 3;
        int done = 0;
        boolean pinsOk = true;
        for (int i = 0; i < ws.cpuPins.length; i++) {
            if (ws.cpuPins[i] != 0) {
                pinsOk = false;
            }
        }
        boolean capsOk = true;
        for (int i = 0; i < ws.caps.length; i++) {
            if (ws.caps[i] != 0) {
                capsOk = false;
            }
        }
        if (pinsOk) {
            done++;
        }
        if (capsOk) {
            done++;
        }
        if (ws.dustRemaining <= 20) {
            done++;
        }
        if (ws.pasteThickness >= 30 && ws.pasteThickness <= 75) {
            parts++;
            done++;
        }
        return done * 100 / parts;
    }

    /** One decimal, no String.format (CLDC has no java.text). */
    static String fmt(float f) {
        int v = (int) (f * 10 + (f >= 0 ? 0.5f : -0.5f));
        return (v / 10) + "." + Math.abs(v % 10);
    }
}
