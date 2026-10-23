package psx;

/**
 * PlayStation GPU and integer software rasterizer.
 *
 * Vertices are 16.16 fixed point, attribute interpolation is 16.16, texture
 * and colour modulation is pure integer math. The rasterizer writes 15 bit
 * RGB555 pixels into the 1 MiB VRAM (1024x512 halfwords) exactly like the
 * real chip; a separate scanout step converts VRAM into an int[] RGB frame
 * at the resolution requested by the UI.
 *
 * Command formats follow the psx-spx GPU documentation.
 */
public final class Gpu {

    public static final int VRAM_W = 1024;
    public static final int VRAM_H = 512;

    public final short[] vram = new short[VRAM_W * VRAM_H];

    /* drawing attributes (GP0 E1..E6) */
    private int texPageX, texPageY, abr, texDepth, dither;
    private int texWinMaskX, texWinMaskY, texWinOffX, texWinOffY;
    private int drawX1, drawY1, drawX2 = 1023, drawY2 = 511;
    private int drawOffsetX, drawOffsetY;
    private boolean maskForceBit15, maskCheckBit15;
    private int tpageReg;

    /* display (GP1 03..08) */
    private int dispX, dispY;
    private int hRangeX1 = 0x200, hRangeX2 = 0xC00;
    private int vRangeY1 = 0x10, vRangeY2 = 0x100;
    private int videoMode;
    private boolean displayEnabled = true;
    public int dmaDirection;

    /* GP0 command state */
    private static final int M_IDLE = 0, M_VRAM_WRITE = 1, M_VRAM_READ = 2;
    private int mode = M_IDLE;
    private final int[] p = new int[16];
    private int pn, need, opcode;
    private int vramIndex, vramRemaining;
    private int readX, readY, readW, readH, readIndex, readRemaining;
    private int polylineWords;      /* -1 when not in a polyline */

    public int commandsExecuted;
    public boolean irqRequested;

    private int gpuReadLatch;
    private int status;

    private int[] frame = new int[320 * 240];
    private int frameW = 320, frameH = 240;

    public Gpu() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < vram.length; i++) vram[i] = 0;
        texPageX = texPageY = abr = texDepth = dither = 0;
        texWinMaskX = texWinMaskY = texWinOffX = texWinOffY = 0;
        drawX1 = drawY1 = 0;
        drawX2 = 1023;
        drawY2 = 511;
        drawOffsetX = drawOffsetY = 0;
        maskForceBit15 = maskCheckBit15 = false;
        tpageReg = 0;
        dispX = dispY = 0;
        hRangeX1 = 0x200;
        hRangeX2 = 0xC00;
        vRangeY1 = 0x10;
        vRangeY2 = 0x100;
        videoMode = 0;
        displayEnabled = true;
        dmaDirection = 0;
        mode = M_IDLE;
        pn = 0;
        polylineWords = -1;
        readRemaining = 0;
        irqRequested = false;
        gpuReadLatch = 0;
        status = 0x14802000;
    }

    /* ==================================================================== */
    /*  registers                                                           */
    /* ==================================================================== */

    /** GPUREAD port (used by DMA channel 2 VRAM->RAM transfers). */
    public int readGPUREAD() {
        if (mode == M_VRAM_READ && readRemaining > 0) return readVramPixel();
        return gpuReadLatch;
    }

    public int read(int addr) {
        if ((addr & 0xF) == 0) {
            if (mode == M_VRAM_READ && readRemaining > 0) return readVramPixel();
            return gpuReadLatch;
        }
        return getStatus();
    }

    public void writeGP1(int v) {
        int cmd = (v >>> 24) & 0xFF;
        switch (cmd) {
            case 0x00:
                reset();
                break;
            case 0x01:
                mode = M_IDLE;
                pn = 0;
                polylineWords = -1;
                break;
            case 0x02:
                irqRequested = false;
                break;
            case 0x03:
                displayEnabled = (v & 1) == 0;
                break;
            case 0x04:
                dmaDirection = v & 3;
                break;
            case 0x05:
                dispX = v & 0x3FF;
                dispY = (v >>> 10) & 0x1FF;
                break;
            case 0x06:
                hRangeX1 = v & 0xFFF;
                hRangeX2 = (v >>> 12) & 0xFFF;
                break;
            case 0x07:
                vRangeY1 = v & 0x3FF;
                vRangeY2 = (v >>> 10) & 0x3FF;
                break;
            case 0x08:
                videoMode = v;
                break;
            default:
                break;
        }
    }

    private int getStatus() {
        int st = 0x14802000;
        st = (st & ~0x7FF) | (tpageReg & 0x7FF);
        if (irqRequested) st |= 1 << 24;
        if (!displayEnabled) st |= 1 << 23;
        st |= (dmaDirection & 3) << 29;
        status = st;
        return st;
    }

    /* ==================================================================== */
    /*  GP0                                                                 */
    /* ==================================================================== */

    public void writeGP0(int word) {
        if (mode == M_VRAM_WRITE) {
            writeVramPixel(word & 0xFFFF);
            writeVramPixel((word >>> 16) & 0xFFFF);
            return;
        }
        if (polylineWords >= 0) {
            handlePolylineWord(word);
            return;
        }
        if (pn == 0) {
            opcode = (word >>> 24) & 0xFF;
            p[0] = word;
            pn = 1;
            need = commandWords(opcode);
            if (need == -1) {
                /* polyline: the next word is the first vertex */
                pn = 0;
                startPolyline(opcode);
                return;
            }
            if (need <= 1) {
                pn = 0;
                execute();
            }
            return;
        }
        p[pn++] = word;
        if (pn >= need) {
            pn = 0;
            execute();
        }
    }

    /** Number of 32 bit words (including the command word) for a GP0 command. */
    private int commandWords(int cmd) {
        int code = (cmd >>> 5) & 7;
        if (code == 1) {                        /* polygon */
            boolean gouraud = (cmd & 0x10) != 0;
            boolean textured = (cmd & 0x04) != 0;
            int verts = ((cmd & 0x08) != 0) ? 4 : 3;
            int n = 1 + verts;
            if (gouraud) n += verts - 1;
            if (textured) n += verts;
            return n;
        }
        if (code == 2) {                        /* line */
            boolean gouraud = (cmd & 0x10) != 0;
            boolean poly = (cmd & 0x08) != 0;
            if (poly) return -1;                /* polyline: consume until terminator */
            return gouraud ? 4 : 3;
        }
        if (code == 3) {                        /* rectangle */
            boolean textured = (cmd & 0x04) != 0;
            boolean sized = ((cmd >>> 3) & 3) == 0;
            int n = 2;
            if (textured) n++;
            if (sized) n++;
            return n;
        }
        if (code == 4) return 4;                /* VRAM to VRAM */
        if (code == 5) {                        /* CPU to VRAM */
            int wh = 0;
            /* size is only known after the header; handled in execute */
            return 3;
        }
        if (code == 6) return 3;                /* VRAM to CPU */
        /* other commands */
        if (cmd == 0x02) return 3;              /* fill rect */
        if (cmd >= 0xE0 && cmd <= 0xE6) return 1;
        return 1;
    }

    private void execute() {
        int cmd = opcode;
        commandsExecuted++;
        if (cmd == 0x02) { fillRect(); return; }
        if (cmd == 0x1F) { irqRequested = true; return; }
        if (cmd >= 0xE0 && cmd <= 0xE6) { attribute(cmd); return; }
        int code = (cmd >>> 5) & 7;
        if (code == 1) { polygon(cmd); return; }
        if (code == 2) {
            if ((cmd & 0x08) != 0) { startPolyline(cmd); return; }
            line(cmd);
            return;
        }
        if (code == 3) { rect(cmd); return; }
        if (code == 4) { vramCopy(); return; }
        if (code == 5) { vramWrite(); return; }
        if (code == 6) { vramRead(); return; }
    }

    /* ---- attributes ------------------------------------------------------ */

    private void attribute(int cmd) {
        int v = p[0];
        switch (cmd) {
            case 0xE1:
                tpageReg = v & 0xFFFF;
                texPageX = (v & 0x0F) * 64;
                texPageY = ((v >>> 4) & 1) * 256;
                abr = (v >>> 5) & 3;
                texDepth = (v >>> 7) & 3;
                dither = (v >>> 9) & 1;
                break;
            case 0xE2:
                texWinMaskX = v & 0x1F;
                texWinMaskY = (v >>> 5) & 0x1F;
                texWinOffX = (v >>> 10) & 0x1F;
                texWinOffY = (v >>> 15) & 0x1F;
                break;
            case 0xE3:
                drawX1 = v & 0x3FF;
                drawY1 = (v >>> 10) & 0x1FF;
                break;
            case 0xE4:
                drawX2 = v & 0x3FF;
                drawY2 = (v >>> 10) & 0x1FF;
                break;
            case 0xE5:
                drawOffsetX = (v << 21) >> 21;
                drawOffsetY = (v << 10) >> 21;
                break;
            case 0xE6:
                maskForceBit15 = (v & 1) != 0;
                maskCheckBit15 = (v & 2) != 0;
                break;
            default:
                break;
        }
    }

    private static int vx(int w) { return (w << 21) >> 21; }

    private static int vy(int w) { return (w << 5) >> 21; }

    /* ---- fill ------------------------------------------------------------ */

    private void fillRect() {
        int xy = p[1];
        int wh = p[2];
        int x = xy & 0x3F0;
        int y = (xy >>> 16) & 0x1FF;
        int w = ((wh & 0x3FF) + 0x0F) & ~0x0F;
        int h = (wh >>> 16) & 0x1FF;
        int color = p[0] & 0xFFFFFF;
        color = ((color & 0xFF) >> 3) | ((((color >>> 8) & 0xFF) >> 3) << 5)
                | ((((color >>> 16) & 0xFF) >> 3) << 10);
        if (w == 0 || h == 0) return;
        for (int yy = 0; yy < h; yy++) {
            int row = ((y + yy) & 0x1FF) * VRAM_W;
            for (int xx = 0; xx < w; xx++) {
                vram[row + ((x + xx) & 0x3FF)] = (short) color;
            }
        }
    }

    /* ---- rectangles ------------------------------------------------------ */

    private void rect(int cmd) {
        boolean textured = (cmd & 0x04) != 0;
        boolean semi = (cmd & 0x02) != 0;
        int siz = (cmd >>> 3) & 3;
        int color = p[0] & 0xFFFFFF;
        int x = vx(p[1]) + drawOffsetX;
        int y = vy(p[1]) + drawOffsetY;
        int idx = 2;
        int u = 0, v = 0, clutX = 0, clutY = 0;
        if (textured) {
            int uv = p[idx++];
            u = uv & 0xFF;
            v = (uv >>> 8) & 0xFF;
            clutX = (uv & 0x3F) * 16;
            clutY = (uv >>> 6) & 0x1FF;
        }
        int w, h;
        if (siz == 0) {
            int wh = p[idx];
            w = wh & 0x3FF;
            h = (wh >>> 16) & 0x1FF;
        } else if (siz == 1) { w = 1; h = 1; }
        else if (siz == 2) { w = 8; h = 8; }
        else { w = 16; h = 16; }
        if (w <= 0 || h <= 0) return;
        for (int yy = 0; yy < h; yy++) {
            for (int xx = 0; xx < w; xx++) {
                int px = x + xx, py = y + yy;
                if (px < drawX1 || px > drawX2 || py < drawY1 || py > drawY2) continue;
                if (py < 0 || py > 511) continue;
                int outColor = color;
                boolean semiPixel = semi;
                if (textured) {
                    int tex = sampleTex(u + xx, v + yy, clutX, clutY);
                    if (tex == 0 && texDepth != 2) continue;
                    outColor = modulate(color, tex);
                    semiPixel = semi && (tex & 0x8000) != 0;
                }
                putPixel(px, py, outColor, semiPixel);
            }
        }
    }

    /* ---- lines ----------------------------------------------------------- */

    private void line(int cmd) {
        boolean gouraud = (cmd & 0x10) != 0;
        boolean semi = (cmd & 0x02) != 0;
        int c0 = cmd & 0xFFFFFF;
        int xy0 = p[1];
        int x0 = vx(xy0) + drawOffsetX;
        int y0 = vy(xy0) + drawOffsetY;
        int c1;
        int xy1;
        if (gouraud) {
            c1 = p[2] & 0xFFFFFF;
            xy1 = p[3];
        } else {
            c1 = c0;
            xy1 = p[2];
        }
        int x1 = vx(xy1) + drawOffsetX;
        int y1 = vy(xy1) + drawOffsetY;
        drawLine(x0, y0, c0, x1, y1, c1, semi);
    }

    private void startPolyline(int cmd) {
        polylineWords = 0;
        polyOpcode = cmd;
        polyStarted = false;
        polyColor = cmd & 0xFFFFFF;
    }

    private int polyOpcode, polyX, polyY, polyColor;
    private boolean polyStarted;

    private void handlePolylineWord(int word) {
        if ((word & 0xF000F000) == 0x50005000) {
            polylineWords = -1;
            polyStarted = false;
            return;
        }
        if ((polyOpcode & 0x10) != 0 && !polyStarted) {
            /* first word of a gouraud polyline is the colour of vertex 0 */
            polyColor = word & 0xFFFFFF;
            return;
        }
        int x = vx(word) + drawOffsetX;
        int y = vy(word) + drawOffsetY;
        if (!polyStarted) {
            polyX = x;
            polyY = y;
            polyStarted = true;
            return;
        }
        drawLine(polyX, polyY, polyColor, x, y, polyColor, (polyOpcode & 0x02) != 0);
        polyX = x;
        polyY = y;
    }

    private void drawLine(int x0, int y0, int c0, int x1, int y1, int c1, boolean semi) {
        int dx = x1 - x0, dy = y1 - y0;
        int adx = dx < 0 ? -dx : dx;
        int ady = dy < 0 ? -dy : dy;
        if (adx > 1023 || ady > 511) return;
        int steps = adx > ady ? adx : ady;
        if (steps == 0) {
            putPixel(x0, y0, c0, semi);
            return;
        }
        int r0 = c0 & 0xFF, g0 = (c0 >>> 8) & 0xFF, b0 = (c0 >>> 16) & 0xFF;
        int r1 = c1 & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = (c1 >>> 16) & 0xFF;
        for (int i = 0; i <= steps; i++) {
            int x = x0 + (dx * i) / steps;
            int y = y0 + (dy * i) / steps;
            int r = r0 + ((r1 - r0) * i) / steps;
            int g = g0 + ((g1 - g0) * i) / steps;
            int b = b0 + ((b1 - b0) * i) / steps;
            putPixel(x, y, r | (g << 8) | (b << 16), semi);
        }
    }

    /* ---- polygons -------------------------------------------------------- */

    /* scratch buffers: polygons must not allocate on CLDC */
    private final int[] triX = new int[4];
    private final int[] triY = new int[4];
    private final int[] triR = new int[4];
    private final int[] triG = new int[4];
    private final int[] triB = new int[4];
    private final int[] triU = new int[4];
    private final int[] triV = new int[4];

    private void polygon(int cmd) {
        boolean gouraud = (cmd & 0x10) != 0;
        boolean textured = (cmd & 0x04) != 0;
        boolean semi = (cmd & 0x02) != 0;
        boolean quad = (cmd & 0x08) != 0;
        int verts = quad ? 4 : 3;

        int[] vxx = triX;
        int[] vyy = triY;
        int[] cr = triR;
        int[] cg = triG;
        int[] cb = triB;
        int[] uu = triU;
        int[] vv = triV;
        int clutX = 0, clutY = 0;

        int color = cmd & 0xFFFFFF;
        cr[0] = color & 0xFF;
        cg[0] = (color >>> 8) & 0xFF;
        cb[0] = (color >>> 16) & 0xFF;

        int idx = 1;
        for (int i = 0; i < verts; i++) {
            if (i > 0) {
                if (gouraud) {
                    int c = p[idx++];
                    cr[i] = c & 0xFF;
                    cg[i] = (c >>> 8) & 0xFF;
                    cb[i] = (c >>> 16) & 0xFF;
                } else {
                    cr[i] = cr[0];
                    cg[i] = cg[0];
                    cb[i] = cb[0];
                }
            }
            int xy = p[idx++];
            vxx[i] = vx(xy) + drawOffsetX;
            vyy[i] = vy(xy) + drawOffsetY;
            if (textured) {
                int uv = p[idx++];
                uu[i] = uv & 0xFF;
                vv[i] = (uv >>> 8) & 0xFF;
                if (i == 0) {
                    clutX = (uv & 0x3F) * 16;
                    clutY = (uv >>> 6) & 0x1FF;
                }
                if (i == 1) {
                    /* second UV word may carry a texture page for this polygon */
                    int page = (uv >>> 16) & 0xFFFF;
                    if (page != 0) {
                        texPageX = (page & 0x0F) * 64;
                        texPageY = ((page >>> 4) & 1) * 256;
                        abr = (page >>> 5) & 3;
                        texDepth = (page >>> 7) & 3;
                    }
                }
            }
        }

        if (!quad) {
            tri(vxx[0], vyy[0], cr[0], cg[0], cb[0], uu[0], vv[0],
                vxx[1], vyy[1], cr[1], cg[1], cb[1], uu[1], vv[1],
                vxx[2], vyy[2], cr[2], cg[2], cb[2], uu[2], vv[2],
                textured, semi, clutX, clutY);
        } else {
            tri(vxx[0], vyy[0], cr[0], cg[0], cb[0], uu[0], vv[0],
                vxx[1], vyy[1], cr[1], cg[1], cb[1], uu[1], vv[1],
                vxx[2], vyy[2], cr[2], cg[2], cb[2], uu[2], vv[2],
                textured, semi, clutX, clutY);
            tri(vxx[1], vyy[1], cr[1], cg[1], cb[1], uu[1], vv[1],
                vxx[2], vyy[2], cr[2], cg[2], cb[2], uu[2], vv[2],
                vxx[3], vyy[3], cr[3], cg[3], cb[3], uu[3], vv[3],
                textured, semi, clutX, clutY);
        }
    }

    /**
     * Scanline triangle rasterizer. Attributes are interpolated with planar
     * gradients in 16.16 fixed point; no division happens per pixel.
     */
    private void tri(int x0, int y0, int r0, int g0, int b0, int u0, int v0,
                     int x1, int y1, int r1, int g1, int b1, int u1, int v1,
                     int x2, int y2, int r2, int g2, int b2, int u2, int v2,
                     boolean textured, boolean semi, int clutX, int clutY) {

        /* sort by y */
        int ax = x0, ay = y0, ar = r0, ag = g0, ab = b0, au = u0, av = v0;
        int bx = x1, by = y1, br = r1, bg = g1, bb = b1, bu = u1, bv = v1;
        int cx = x2, cy = y2, cr = r2, cg = g2, cb = b2, cu = u2, cv = v2;
        if (ay > by) {
            int t;
            t = ax; ax = bx; bx = t; t = ay; ay = by; by = t;
            t = ar; ar = br; br = t; t = ag; ag = bg; bg = t; t = ab; ab = bb; bb = t;
            t = au; au = bu; bu = t; t = av; av = bv; bv = t;
        }
        if (by > cy) {
            int t;
            t = bx; bx = cx; cx = t; t = by; by = cy; cy = t;
            t = br; br = cr; cr = t; t = bg; bg = cg; cg = t; t = bb; bb = cb; cb = t;
            t = bu; bu = cu; cu = t; t = bv; bv = cv; cv = t;
        }
        if (ay > by) {
            int t;
            t = ax; ax = bx; bx = t; t = ay; ay = by; by = t;
            t = ar; ar = br; br = t; t = ag; ag = bg; bg = t; t = ab; ab = bb; bb = t;
            t = au; au = bu; bu = t; t = av; av = bv; bv = t;
        }

        if (cy - ay <= 0) return;
        if (cy - ay > 1024) return;

        long det = (long) (bx - ax) * (cy - ay) - (long) (cx - ax) * (by - ay);
        if (det == 0) return;

        /* attribute gradients per pixel: 16.16 */
        long drdx = (((long) (br - ar) * (cy - ay) - (long) (cr - ar) * (by - ay)) << 16) / det;
        long drdy = (((long) (cr - ar) * (bx - ax) - (long) (br - ar) * (cx - ax)) << 16) / det;
        long dgdx = (((long) (bg - ag) * (cy - ay) - (long) (cg - ag) * (by - ay)) << 16) / det;
        long dgdy = (((long) (cg - ag) * (bx - ax) - (long) (bg - ag) * (cx - ax)) << 16) / det;
        long dbdx = (((long) (bb - ab) * (cy - ay) - (long) (cb - ab) * (by - ay)) << 16) / det;
        long dbdy = (((long) (cb - ab) * (bx - ax) - (long) (bb - ab) * (cx - ax)) << 16) / det;
        long dudx = (((long) (bu - au) * (cy - ay) - (long) (cu - au) * (by - ay)) << 16) / det;
        long dudy = (((long) (cu - au) * (bx - ax) - (long) (bu - au) * (cx - ax)) << 16) / det;
        long dvdx = (((long) (bv - av) * (cy - ay) - (long) (cv - av) * (by - ay)) << 16) / det;
        long dvdy = (((long) (cv - av) * (bx - ax) - (long) (bv - av) * (cx - ax)) << 16) / det;

        int yy0 = ay < drawY1 ? drawY1 : ay;
        int yy1 = cy > drawY2 ? drawY2 : cy;
        if (yy0 < 0) yy0 = 0;
        if (yy1 > 511) yy1 = 511;
        if (yy0 > yy1) return;

        for (int y = yy0; y <= yy1; y++) {
            int xa = edge(ax, ay, cx, cy, y);
            int xb = (y < by) ? edge(ax, ay, bx, by, y) : edge(bx, by, cx, cy, y);
            int xl = xa < xb ? xa : xb;
            int xr = xa < xb ? xb : xa;
            if (xl < drawX1) xl = drawX1;
            if (xr > drawX2) xr = drawX2;
            if (xl > xr) continue;

            int px = xl - ax;
            int py = y - ay;
            long lr = ((long) ar << 16) + px * drdx + py * drdy;
            long lg = ((long) ag << 16) + px * dgdx + py * dgdy;
            long lb = ((long) ab << 16) + px * dbdx + py * dbdy;
            long lu = ((long) au << 16) + px * dudx + py * dudy;
            long lv = ((long) av << 16) + px * dvdx + py * dvdy;

            int rowBase = (y & 0x1FF) * VRAM_W;
            for (int x = xl; x <= xr; x++) {
                int r = (int) (lr >> 16);
                int g = (int) (lg >> 16);
                int b = (int) (lb >> 16);
                if (r < 0) r = 0; else if (r > 255) r = 255;
                if (g < 0) g = 0; else if (g > 255) g = 255;
                if (b < 0) b = 0; else if (b > 255) b = 255;
                int color = r | (g << 8) | (b << 16);
                boolean semiPixel = semi;
                if (textured) {
                    int tu = (int) (lu >> 16) & 0xFF;
                    int tv = (int) (lv >> 16) & 0xFF;
                    int tex = sampleTex(tu, tv, clutX, clutY);
                    if (tex == 0 && texDepth != 2) {
                        lr += drdx; lg += dgdx; lb += dbdx; lu += dudx; lv += dvdx;
                        continue;
                    }
                    color = modulate(color, tex);
                    if ((tex & 0x8000) != 0) semiPixel = true;
                }
                drawPixel(rowBase + x, color, semiPixel);
                lr += drdx; lg += dgdx; lb += dbdx; lu += dudx; lv += dvdx;
            }
        }
    }

    /** x coordinate of a line at scanline y (16.16 interpolation). */
    private static int edge(int x0, int y0, int x1, int y1, int y) {
        if (y1 == y0) return x0;
        return x0 + (int) (((long) (x1 - x0) * (y - y0)) / (y1 - y0));
    }

    /* ---- pixel pipeline -------------------------------------------------- */

    private void putPixel(int x, int y, int color, boolean semi) {
        if (x < drawX1 || x > drawX2 || y < drawY1 || y > drawY2) return;
        if (x < 0 || x > 1023 || y < 0 || y > 511) return;
        drawPixel((y * VRAM_W) + x, color, semi);
    }

    private void drawPixel(int idx, int color, boolean semi) {
        int dst = vram[idx] & 0xFFFF;
        if (maskCheckBit15 && (dst & 0x8000) != 0) return;
        int sr = (color & 0xFF) >> 3;
        int sg = ((color >>> 8) & 0xFF) >> 3;
        int sb = ((color >>> 16) & 0xFF) >> 3;
        int out;
        if (semi) {
            int dr = dst & 0x1F, dg = (dst >>> 5) & 0x1F, db = (dst >>> 10) & 0x1F;
            int rr, gg, bb;
            switch (abr & 3) {
                case 0: rr = (dr + sr) >> 1; gg = (dg + sg) >> 1; bb = (db + sb) >> 1; break;
                case 1: rr = dr + sr; gg = dg + sg; bb = db + sb; break;
                case 2: rr = dr - sr; gg = dg - sg; bb = db - sb; break;
                default: rr = (dr + sr) >> 2; gg = (dg + sg) >> 2; bb = (db + sb) >> 2; break;
            }
            if (rr < 0) rr = 0; else if (rr > 31) rr = 31;
            if (gg < 0) gg = 0; else if (gg > 31) gg = 31;
            if (bb < 0) bb = 0; else if (bb > 31) bb = 31;
            out = rr | (gg << 5) | (bb << 10);
        } else {
            out = sr | (sg << 5) | (sb << 10);
        }
        if (maskForceBit15) out |= 0x8000;
        vram[idx] = (short) out;
    }

    private static int modulate(int vcolor, int tex) {
        int r = ((vcolor & 0xFF) * ((tex & 0x1F) << 3)) >> 8;
        int g = (((vcolor >>> 8) & 0xFF) * (((tex >>> 5) & 0x1F) << 3)) >> 8;
        int b = (((vcolor >>> 16) & 0xFF) * (((tex >>> 10) & 0x1F) << 3)) >> 8;
        if (r > 255) r = 255;
        if (g > 255) g = 255;
        if (b > 255) b = 255;
        return r | (g << 8) | (b << 16);
    }

    private int sampleTex(int u, int v, int clutX, int clutY) {
        if ((texWinMaskX | texWinMaskY | texWinOffX | texWinOffY) != 0) {
            u = (u & ~(texWinMaskX * 8)) | ((texWinOffX & texWinMaskX) * 8);
            v = (v & ~(texWinMaskY * 8)) | ((texWinOffY & texWinMaskY) * 8);
        }
        u &= 0xFF;
        v &= 0xFF;
        int tx = (texPageX + u) & 0x3FF;
        int ty = (texPageY + v) & 0x1FF;
        int row = ty << 10;
        if (texDepth == 0) {
            int w = vram[row + (tx >> 2)] & 0xFFFF;
            int index = (w >>> ((tx & 3) * 4)) & 0xF;
            if (index == 0) return 0;
            return vram[(clutY & 0x1FF) * VRAM_W + ((clutX + index) & 0x3FF)] & 0xFFFF;
        } else if (texDepth == 1) {
            int w = vram[row + (tx >> 1)] & 0xFFFF;
            int index = ((tx & 1) == 0) ? (w & 0xFF) : ((w >>> 8) & 0xFF);
            if (index == 0) return 0;
            return vram[(clutY & 0x1FF) * VRAM_W + ((clutX + index) & 0x3FF)] & 0xFFFF;
        }
        return vram[row + tx] & 0xFFFF;
    }

    /* ---- VRAM transfers --------------------------------------------------- */

    private static int xferW(int wh) {
        int w = wh & 0x3FF;
        return ((w - 1) & 0x3FF) + 1;
    }

    private static int xferH(int wh) {
        int h = (wh >>> 16) & 0x1FF;
        return ((h - 1) & 0x1FF) + 1;
    }

    private void vramWrite() {
        int xy = p[1];
        int wh = p[2];
        int x = xy & 0x3FF;
        int y = (xy >>> 16) & 0x1FF;
        int w = xferW(wh);
        int h = xferH(wh);
        int count = w * h;
        vramIndex = y * VRAM_W + x;
        vramRemaining = count;
        mode = M_VRAM_WRITE;
        if (count <= 0) mode = M_IDLE;
    }

    private void writeVramPixel(int pixel) {
        if (vramRemaining <= 0) { mode = M_IDLE; return; }
        if (maskCheckBit15 && (vram[vramIndex] & 0x8000) != 0) {
            /* masked pixel: value is still consumed */
        } else {
            int out = pixel & 0x7FFF;
            if (maskForceBit15) out |= 0x8000;
            vram[vramIndex] = (short) out;
        }
        vramIndex++;
        vramRemaining--;
        if (vramIndex % VRAM_W == 0) {
            /* simply continue on the next row; wrapping is automatic since the
               index is absolute and the transfer is row-major */
        }
        if (vramRemaining <= 0) mode = M_IDLE;
    }

    private void vramRead() {
        int xy = p[1];
        int wh = p[2];
        readX = xy & 0x3FF;
        readY = (xy >>> 16) & 0x1FF;
        readW = xferW(wh);
        readH = xferH(wh);
        readIndex = readY * VRAM_W + readX;
        readRemaining = readW * readH;
        mode = M_VRAM_READ;
    }

    private int readVramPixel() {
        int v = vram[readIndex] & 0xFFFF;
        readIndex++;
        readRemaining--;
        if (readRemaining <= 0) mode = M_IDLE;
        return v;
    }

    private void vramCopy() {
        int src = p[1];
        int dst = p[2];
        int wh = p[3];
        int sx = src & 0x3FF, sy = (src >>> 16) & 0x1FF;
        int dx = dst & 0x3FF, dy = (dst >>> 16) & 0x1FF;
        int w = xferW(wh), h = xferH(wh);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int s = ((sy + y) & 0x1FF) * VRAM_W + ((sx + x) & 0x3FF);
                int d = ((dy + y) & 0x1FF) * VRAM_W + ((dx + x) & 0x3FF);
                int v = vram[s] & 0xFFFF;
                if (maskCheckBit15 && (vram[d] & 0x8000) != 0) continue;
                vram[d] = (short) v;
            }
        }
    }

    /* ==================================================================== */
    /*  scanout                                                             */
    /* ==================================================================== */

    public void setViewport(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (w != frameW || h != frameH || frame.length != w * h) {
            frameW = w;
            frameH = h;
            frame = new int[w * h];
        }
    }

    public int getViewportWidth() { return frameW; }

    public int getViewportHeight() { return frameH; }

    public int getNativeWidth() {
        if ((videoMode & 0x40) != 0) return 368;
        switch (videoMode & 3) {
            case 0: return 256;
            case 1: return 320;
            case 2: return 512;
            default: return 640;
        }
    }

    public int getNativeHeight() {
        return ((videoMode & 0x04) != 0) ? 480 : 240;
    }

    public int[] scanout() {
        int nw = getNativeWidth();
        int nh = getNativeHeight();
        int startX = dispX & 0x3FF;
        int startY = dispY & 0x1FF;
        int[] dst = frame;
        if (!displayEnabled) {
            for (int i = 0; i < dst.length; i++) dst[i] = 0xFF000000;
            return dst;
        }
        for (int y = 0; y < frameH; y++) {
            int sy = (startY + (y * nh) / frameH) & 0x1FF;
            int srcRow = sy * VRAM_W;
            int dstRow = y * frameW;
            for (int x = 0; x < frameW; x++) {
                int sx = (startX + (x * nw) / frameW) & 0x3FF;
                int c = vram[srcRow + sx] & 0x7FFF;
                int r = (c & 0x1F) << 3;
                int g = ((c >>> 5) & 0x1F) << 3;
                int b = ((c >>> 10) & 0x1F) << 3;
                dst[dstRow + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        return dst;
    }

    public int[] getFrame() { return frame; }
}
