package rpc;

/**
 * The assembly screen's "workbench" drawing.
 *
 * 4.8 showed the assembly as a flat text list - 27 identical rows, no picture.
 * Here the PC being built is drawn: an open case with motherboard, CPU socket,
 * cooler, four RAM sticks, GPU and expansion cards, drive bays and the PSU,
 * plus the peripherals around it (monitor, keyboard, mouse, printer).
 *
 * Every slot has a rectangle in a virtual 240x140 canvas that is scaled into
 * the area the caller gives us, so the same layout works on any screen size.
 * Slots that hold a part show that part's own sprite; empty slots show a
 * dashed outline with their number.
 */
public final class BuildView {

    /** virtual canvas the geometry below is written for */
    private static final int VW = 240;
    private static final int VH = 140;

    private BuildView() {
    }

    /* slot rectangles in virtual coordinates: {x, y, w, h} */
    private static int[] vrect(int slot) {
        switch (slot) {
            case 0:  return new int[] { 20, 22, 26, 26 };    /* CPU socket  */
            case 1:  return new int[] { 14, 14, 84, 106 };   /* motherboard */
            case 2:  return new int[] { 54, 24, 6, 44 };     /* RAM 1 */
            case 3:  return new int[] { 62, 24, 6, 44 };     /* RAM 2 */
            case 4:  return new int[] { 70, 24, 6, 44 };     /* RAM 3 */
            case 5:  return new int[] { 78, 24, 6, 44 };     /* RAM 4 */
            case 6:  return new int[] { 18, 54, 50, 12 };    /* GPU */
            case 7:  return new int[] { 104, 16, 28, 12 };   /* drive 1 */
            case 8:  return new int[] { 104, 30, 28, 12 };   /* drive 2 */
            case 9:  return new int[] { 104, 44, 28, 12 };   /* drive 3 */
            case 10: return new int[] { 104, 96, 28, 24 };   /* PSU */
            case 11: return new int[] { 100, 10, 36, 116 };  /* case */
            case 12: return new int[] { 16, 18, 34, 34 };    /* cooler */
            case 13: return new int[] { 140, 14, 86, 62 };   /* monitor */
            case 14: return new int[] { 18, 70, 26, 10 };    /* sound */
            case 15: return new int[] { 140, 86, 66, 14 };   /* keyboard */
            case 16: return new int[] { 210, 86, 14, 12 };   /* mouse */
            case 17: return new int[] { 48, 70, 26, 10 };    /* network */
            case 18: return new int[] { 18, 84, 26, 10 };    /* riser */
            case 19: return new int[] { 140, 104, 46, 20 };  /* printer */
            default: return null;
        }
    }

    /** Screen rectangle of a slot (0..19), or null for the action rows. */
    public static int[] slotRect(int slot, int x, int y, int w, int h) {
        int[] v = vrect(slot);
        if (v == null) {
            return null;
        }
        int sx = w * 1000 / VW;
        int sy = h * 1000 / VH;
        return new int[] { x + v[0] * sx / 1000, y + v[1] * sy / 1000,
                v[2] * sx / 1000, v[3] * sy / 1000 };
    }

    /**
     * Draws the workbench.
     *
     * @param slots    current component in each of the 20 slots (null = empty)
     * @param cursor   selected row (0..26, &gt;=20 are the action rows)
     * @param pcOn     true when the machine is assembled and powered
     * @param frame    animation frame
     */
    public static void draw(Gfx g, Part[] slots, int cursor, boolean pcOn,
                            int x, int y, int w, int h, int frame) {
        int sx = w * 1000 / VW;
        int sy = h * 1000 / VH;

        // ---- bench surface -------------------------------------------
        g.setColor(Theme.PANEL2);
        g.fillRect(x, y, w, h);
        g.setColor(0x2A2E3C);
        g.fillRect(x, y + h - 6, w, 6);

        drawCase(g, slots, cursor, x, y, w, h, sx, sy, pcOn, frame);
        drawMonitor(g, slots, cursor, x, y, w, h, sx, sy, pcOn, frame);
        drawPeripherals(g, slots, cursor, x, y, w, h, sx, sy);

        // ---- highlight ------------------------------------------------
        int[] r = cursor < 20 ? slotRect(cursor, x, y, w, h) : null;
        if (r != null) {
            g.setColor(Theme.ACCENT);
            if ((frame / 8) % 2 == 0) {
                g.drawRect(r[0] - 1, r[1] - 1, r[2] + 2, r[3] + 2);
            } else {
                g.drawRect(r[0] - 2, r[1] - 2, r[2] + 4, r[3] + 4);
            }
        }
    }

    private static void drawCase(Gfx g, Part[] slots, int cursor, int x, int y,
                                 int w, int h, int sx, int sy, boolean pcOn,
                                 int frame) {
        // case shell
        int cx = x + 100 * sx / 1000;
        int cy = y + 10 * sy / 1000;
        int cw = 36 * sx / 1000;
        int ch = 116 * sy / 1000;
        g.setColor(cursor == 11 ? Theme.ACCENT : 0x8A90A0);
        g.drawRect(cx, cy, cw, ch);
        g.setColor(0x7E8494);
        g.fillRect(cx, cy, cw, 2);
        g.fillRect(cx + cw - 2, cy, 2, ch);

        // motherboard
        int mx = x + 14 * sx / 1000;
        int my = y + 14 * sy / 1000;
        int mw = 84 * sx / 1000;
        int mh = 106 * sy / 1000;
        g.setColor(cursor == 1 ? 0x2E5A32 : 0x1E4A2A);
        g.fillRect(mx, my, mw, mh);
        g.setColor(cursor == 1 ? Theme.ACCENT : 0x2E6B3E);
        g.drawRect(mx, my, mw, mh);
        // traces
        g.setColor(0x2A5A38);
        for (int i = 1; i < 8; i++) {
            int ty = my + mh * i / 8;
            g.drawLine(mx + 2, ty, mx + mw - 2, ty);
        }

        // CPU socket + part sprite
        Part cpu = slots[0];
        int cpux = x + 20 * sx / 1000;
        int cpuy = y + 22 * sy / 1000;
        int cpuw = 26 * sx / 1000;
        int cpuh = 26 * sy / 1000;
        g.setColor(0x2A3038);
        g.fillRect(cpux, cpuy, cpuw, cpuh);
        g.setColor(cpu != null ? 0xB8A060 : 0x4A5460);
        g.fillRect(cpux + 2, cpuy + 2, cpuw - 4, cpuh - 4);
        if (cpu != null && Assets.ready) {
            Assets.drawPart(g, cpu, cpux + 4, cpuy + 4, cpuw - 8, cpuh - 8);
        }

        // cooler: fan blades spin when the machine runs
        Part cool = slots[12];
        if (cool != null) {
            int fx = x + 16 * sx / 1000;
            int fy = y + 18 * sy / 1000;
            int fw = 34 * sx / 1000;
            int fh = 34 * sy / 1000;
            g.setColor(0x3A4152);
            g.fillRect(fx, fy, fw, fh);
            int cxr = fx + fw / 2;
            int cyr = fy + fh / 2;
            int rad = Math.min(fw, fh) / 2 - 2;
            g.setColor(0x6E7482);
            g.drawRect(cxr - rad, cyr - rad, rad * 2, rad * 2);
            g.setColor(pcOn ? 0xC8D2E0 : 0x8A90A0);
            int phase = pcOn ? (frame * 40) % 360 : 0;
            for (int b = 0; b < 3; b++) {
                double a = Math.toRadians(phase + b * 120);
                g.drawLine(cxr, cyr, cxr + (int) (Math.cos(a) * rad),
                        cyr + (int) (Math.sin(a) * rad));
            }
        }

        // RAM sticks
        for (int i = 0; i < 4; i++) {
            int slot = 2 + i;
            int[] r = vrect(slot);
            int rx = x + r[0] * sx / 1000;
            int ry = y + r[1] * sy / 1000;
            int rw = r[2] * sx / 1000;
            int rh = r[3] * sy / 1000;
            Part ram = slots[slot];
            g.setColor(ram != null ? (cursor == slot ? Theme.ACCENT2 : 0x59A86B)
                    : (cursor == slot ? Theme.ACCENT : 0x2A4A34));
            if (ram != null) {
                g.fillRect(rx, ry, rw, rh);
                g.setColor(0x1E2A20);
                for (int k = 0; k < 4; k++) {
                    g.fillRect(rx + 1, ry + 4 + k * (rh - 8) / 4, rw - 2, 1);
                }
            } else {
                g.drawRect(rx, ry, rw, rh);
            }
        }

        // GPU
        drawCard(g, slots, 6, cursor, x, y, sx, sy, 0x2E6B3E);
        // sound / network / riser
        drawCard(g, slots, 14, cursor, x, y, sx, sy, 0x2E6B3E);
        drawCard(g, slots, 17, cursor, x, y, sx, sy, 0x2E6B3E);
        drawCard(g, slots, 18, cursor, x, y, sx, sy, 0x2E6B3E);

        // drive bays
        for (int i = 0; i < 3; i++) {
            int slot = 7 + i;
            int[] r = vrect(slot);
            int dx = x + r[0] * sx / 1000;
            int dy = y + r[1] * sy / 1000;
            int dw = r[2] * sx / 1000;
            int dh = r[3] * sy / 1000;
            Part d = slots[slot];
            g.setColor(d != null ? 0x9AA0B0 : 0x4A5060);
            g.fillRect(dx, dy, dw, dh);
            g.setColor(d != null ? 0x6E7482 : 0x323846);
            g.fillRect(dx + 1, dy + 1, dw - 2, dh - 2);
            if (d != null) {
                g.setColor(pcOn ? 0x63E6A0 : 0x2C3240);
                g.fillRect(dx + dw - 4, dy + 3, 2, 2);
                g.setColor(0xDDE6F6);
                g.fillRect(dx + 3, dy + dh / 2 - 1, dw - 10, 2);
            }
            if (cursor == slot) {
                g.setColor(Theme.ACCENT);
                g.drawRect(dx - 1, dy - 1, dw + 2, dh + 2);
            }
        }

        // PSU with its own fan
        Part psu = slots[10];
        int[] pr = vrect(10);
        int px = x + pr[0] * sx / 1000;
        int py = y + pr[1] * sy / 1000;
        int pw = pr[2] * sx / 1000;
        int ph = pr[3] * sy / 1000;
        g.setColor(psu != null ? 0x8A90A0 : 0x4A5060);
        g.fillRect(px, py, pw, ph);
        g.setColor(psu != null ? 0x5A6070 : 0x323846);
        g.fillRect(px + 2, py + 2, pw - 4, ph - 4);
        g.setColor(psu != null && pcOn ? 0x63E6A0 : 0x3A4152);
        g.fillRect(px + 4, py + 4, pw - 8, 3);
        if (cursor == 10) {
            g.setColor(Theme.ACCENT);
            g.drawRect(px - 1, py - 1, pw + 2, ph + 2);
        }
    }

    private static void drawCard(Gfx g, Part[] slots, int slot, int cursor,
                                 int x, int y, int sx, int sy, int board) {
        int[] r = vrect(slot);
        int rx = x + r[0] * sx / 1000;
        int ry = y + r[1] * sy / 1000;
        int rw = r[2] * sx / 1000;
        int rh = r[3] * sy / 1000;
        Part p = slots[slot];
        g.setColor(p != null ? 0x2E7A48 : 0x26402E);
        g.fillRect(rx, ry, rw, rh);
        g.setColor(p != null ? 0x9AC8A8 : 0x3A4A3E);
        g.drawRect(rx, ry, rw, rh);
        if (p != null) {
            g.setColor(0xC8D2E0);
            g.fillRect(rx + 2, ry + rh / 2 - 1, rw - 4, 2);
        }
        if (cursor == slot) {
            g.setColor(Theme.ACCENT);
            g.drawRect(rx - 1, ry - 1, rw + 2, rh + 2);
        }
    }

    private static void drawMonitor(Gfx g, Part[] slots, int cursor, int x,
                                    int y, int w, int h, int sx, int sy,
                                    boolean pcOn, int frame) {
        int[] r = vrect(13);
        int mx = x + r[0] * sx / 1000;
        int my = y + r[1] * sy / 1000;
        int mw = r[2] * sx / 1000;
        int mh = r[3] * sy / 1000;
        Part mon = slots[13];
        g.setColor(mon != null ? 0xB9BECB : 0x5A6070);
        g.fillRoundRect(mx, my, mw, mh, 3);
        g.setColor(0x1A1E28);
        g.fillRect(mx + 3, my + 3, mw - 6, mh - 12);
        if (pcOn) {
            g.setColor(0x1B3A5A);
            g.fillRect(mx + 4, my + 4, mw - 8, mh - 14);
            g.setColor(0xE9F2FF);
            g.fillRect(mx + 6, my + 8, mw - 12, mh - 22);
            g.setColor(0x2D6FA8);
            g.fillRect(mx + 6, my + 8, mw - 12, 3);
            g.setColor(0x9AA8BC);
            for (int i = 0; i < (mh - 24) / 3 && i < 8; i++) {
                int len = (i * 11 + frame / 8) % (mw - 16);
                g.fillRect(mx + 7, my + 13 + i * 3, len, 1);
            }
        } else {
            g.setColor(0x0E1218);
            g.fillRect(mx + 4, my + 4, mw - 8, mh - 14);
        }
        // stand
        g.setColor(0x6E7482);
        g.fillRect(mx + mw / 3, my + mh, mw / 3, 3);
        if (cursor == 13) {
            g.setColor(Theme.ACCENT);
            g.drawRoundRect(mx - 1, my - 1, mw + 2, mh + 2, 3);
        }
    }

    private static void drawPeripherals(Gfx g, Part[] slots, int cursor, int x,
                                        int y, int w, int h, int sx, int sy) {
        // keyboard
        int[] kr = vrect(15);
        int kx = x + kr[0] * sx / 1000;
        int ky = y + kr[1] * sy / 1000;
        int kw = kr[2] * sx / 1000;
        int kh = kr[3] * sy / 1000;
        g.setColor(slots[15] != null ? 0xB0B6C4 : 0x4A5060);
        g.fillRect(kx, ky, kw, kh);
        g.setColor(0xE2E8F2);
        for (int r = 0; r < (kh - 2) / 3; r++) {
            for (int c = 0; c < (kw - 3) / 5; c++) {
                g.fillRect(kx + 2 + c * 5, ky + 2 + r * 3, 3, 2);
            }
        }
        if (cursor == 15) {
            g.setColor(Theme.ACCENT);
            g.drawRect(kx - 1, ky - 1, kw + 2, kh + 2);
        }

        // mouse
        int[] mr = vrect(16);
        int mox = x + mr[0] * sx / 1000;
        int moy = y + mr[1] * sy / 1000;
        int mow = mr[2] * sx / 1000;
        int moh = mr[3] * sy / 1000;
        g.setColor(slots[16] != null ? 0xC8CEDC : 0x4A5060);
        g.fillRoundRect(mox, moy, mow, moh, Math.max(2, moh / 3));
        if (cursor == 16) {
            g.setColor(Theme.ACCENT);
            g.drawRoundRect(mox - 1, moy - 1, mow + 2, moh + 2,
                    Math.max(2, moh / 3));
        }

        // printer
        int[] rr = vrect(19);
        int px = x + rr[0] * sx / 1000;
        int py = y + rr[1] * sy / 1000;
        int pw = rr[2] * sx / 1000;
        int ph = rr[3] * sy / 1000;
        g.setColor(slots[19] != null ? 0xB0B6C4 : 0x3E4452);
        g.fillRect(px, py + ph / 3, pw, ph * 2 / 3);
        g.setColor(slots[19] != null ? 0xE8EEF8 : 0x2E3440);
        g.fillRect(px + 3, py, pw - 6, ph / 3);
        g.setColor(0x4A5060);
        g.fillRect(px + 2, py + ph - 3, pw - 4, 2);
        if (cursor == 19) {
            g.setColor(Theme.ACCENT);
            g.drawRect(px - 1, py - 1, pw + 2, ph + 2);
        }
    }

    /** Full slot title, e.g. "Процессор". */
    public static String slotTitle(int slot) {
        return slot >= 0 && slot < rpc.Score.SLOT_NAMES.length
                ? rpc.Score.SLOT_NAMES[slot] : "";
    }

    /** Short label for the action buttons (index 0..6, i.e. rows 20..26). */
    public static String actionShort(int i) {
        switch (i) {
            case 0: return "\u0411азар";
            case 1: return "Доставка";
            case 2: return "Стол";
            case 3: return "ОС и сеть";
            case 4: return "Ждать";
            case 5: return "ТЕСТ!";
            default: return "BIOS";
        }
    }

    public static String actionTitle(int i) {
        switch (i) {
            case 0: return "Рынки: базар, барахолка, Avito";
            case 1: return "Доставка: заказы";
            case 2: return "Стол сборки: привезли";
            case 3: return "ОС, программы и сеть";
            case 4: return "Ждать сутки (доставка, монтаж)";
            case 5: return "ЗАПУСТИТЬ ТЕСТ";
            default: return "BIOS / POST: диагностика";
        }
    }

    public static String actionHint(int i) {
        switch (i) {
            case 0: return "покупка деталей и торг";
            case 1: return "статус посылок";
            case 2: return "собрать из того, что есть";
            case 3: return "установка системы";
            case 4: return "промотать время";
            case 5: return "проверка сборки: POST";
            default: return "прошивка и диагностика";
        }
    }
}
