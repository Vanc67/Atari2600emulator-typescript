package com.chiplab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Dense, deterministic silicon-layout synthesis for ChipLab.
 *
 * This is deliberately modelled after colourised die photographs / mask
 * plots (the Visual6502 look), rather than a block diagram.  The world is
 * a Manhattan lambda grid.  Diffusion, polysilicon, contacts and three
 * metal layers are emitted as separate rectangles; where a polysilicon
 * strip crosses diffusion it forms the channel of a transistor.  A whole
 * die contains its functional blocks plus the things real floorplans need
 * to fill silicon: seal/guard rings, bond pads and I/O drivers, a power
 * mesh, clock distribution, global Manhattan routing, filler cells and
 * routing channels.  Consequently there are no decorative empty block
 * rectangles and no large pieces of unused black substrate.
 *
 * The geometry is static for a given CHIPBIN1 file.  Conductors carry a
 * dynamic binding (register/memory bits, ALU operands and carries,
 * word/bit lines, pixels, bus bits, clock phase, control activity), so
 * the Android view can cache the expensive mask drawing in a Bitmap and
 * redraw only the live signal layer as Cpu state changes.
 *
 * Pure Java, no Android classes.  RenderDie in the desktop harness uses
 * exactly the same colour resolution as the phone UI.
 */
public final class DieLayout {
    // Mask layers.  Numerical order is *not* draw order; zForLayer() is.
    public static final int L_WELL = 0;
    public static final int L_NDIFF = 1;
    public static final int L_PDIFF = 2;
    public static final int L_POLY = 3;
    public static final int L_CONTACT = 4;
    public static final int L_METAL1 = 5;
    public static final int L_VIA = 6;
    public static final int L_METAL2 = 7;
    public static final int L_METAL3 = 8;
    public static final int L_PAD = 9;
    public static final int L_MARK = 10;
    public static final int L_PIXEL = 11;

    public static final int[] LAYER_COLORS = {
            0x2E241247, // well/substrate wash: very dark, mostly texture
            0xC750E67E, // n+ diffusion: yellow-green in die photos
            0xC7F2AC45, // p+ diffusion: gold/orange
            0xD9F05749, // polysilicon: red
            0xEAF6FFF1, // contact cuts
            0xA86652E8, // metal-1: violet
            0xF2FFFFFF, // vias
            0xA8EA4DAC, // metal-2: magenta
            0x8C55CFE8, // metal-3/clock: cyan
            0xFF9A8B5F, // bond-pad metal
            0xFFFFFF5C, // transistor channel highlight
            0xFF30364A  // pixel electrode placeholder (palette overrides)
    };

    public static final int[] SCREEN_PALETTE = {
            0xFF101216, 0xFFFFC107, 0xFFFF7043, 0xFFEF5350,
            0xFFAB47BC, 0xFF5C6BC0, 0xFF42A5F5, 0xFF26C6DA,
            0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFD4E157,
            0xFFFFEE58, 0xFFFFCA28, 0xFFFFA726, 0xFFFFFFFF
    };

    private static final int HOT = 0xFFFFFF63;
    private static final int HOT_DIM = 0xFFFFD740;

    // Dynamic signal bindings.  Segment fields a/b carry indices described here.
    public static final int D_NONE = 0;
    public static final int D_MEM_VAL = 1;    // a=word: brightness by value
    public static final int D_MEM_BIT = 2;    // a=word b=bit
    public static final int D_MEM_NBIT = 3;   // a=word b=bit (inverted)
    public static final int D_REG_BIT = 4;    // a=register b=bit
    public static final int D_WL = 5;         // a=row b=columns in that array
    public static final int D_BL = 6;         // a=column b=columns in that array
    public static final int D_ALU_A = 7;      // a=bit
    public static final int D_ALU_B = 8;      // a=bit
    public static final int D_ALU_S = 9;      // a=bit
    public static final int D_CARRY = 10;     // a=bit (carry into a)
    public static final int D_BUS = 11;       // a=bus bit
    public static final int D_LOGIC = 12;     // a=pseudo-random node identifier
    public static final int D_SCREEN = 13;    // a=pixel index
    public static final int D_REGROW = 14;    // a=register
    public static final int D_CLK = 15;       // a=clock phase
    public static final int D_REG_NBIT = 16;  // a=register b=bit (inverted)
    public static final int D_SCAN_ROW = 17;  // a=framebuffer cursor row
    public static final int D_SCAN_COL = 18;  // a=framebuffer cursor column

    public static final class Seg {
        public int layer;
        public float x, y, w, h;
        public int dyn, a, b;
        public int shade;

        Seg(int layer, float x, float y, float w, float h, int dyn, int a, int b, int shade) {
            this.layer = layer;
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.dyn = dyn; this.a = a; this.b = b;
            this.shade = shade;
        }
    }

    public static final class Label {
        public final String text;
        public final float x, y, size;
        public final int color;

        Label(String text, float x, float y, float size, int color) {
            this.text = text; this.x = x; this.y = y; this.size = size; this.color = color;
        }
    }

    public static final class Area {
        public final float x, y, w, h;
        public final int blockIndex;

        Area(float x, float y, float w, float h, int blockIndex) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.blockIndex = blockIndex;
        }
    }

    /** Everything colour resolution needs for one frame. */
    public static final class Ctx {
        public final ChipBin chip;
        public final Cpu cpu;
        public final int[] carryInto;
        public final int busValue;

        public Ctx(ChipBin chip, Cpu cpu) {
            this.chip = chip;
            this.cpu = cpu;
            int bits = Math.max(1, chip.bits);
            carryInto = new int[bits + 1];
            if (cpu != null && (cpu.aluOp == 0x03 || cpu.aluOp == 0x0B)) {
                int carry = 0;
                for (int i = 0; i < bits; i++) {
                    carryInto[i] = carry;
                    int sum = ((cpu.aluA >> i) & 1) + ((cpu.aluB >> i) & 1) + carry;
                    carry = sum >= 2 ? 1 : 0;
                }
                carryInto[bits] = carry;
            } else if (cpu != null && cpu.flagC) {
                carryInto[bits] = 1;
            }

            int value = 0;
            if (cpu != null) {
                if (cpu.lastMemAddr >= 0 && cpu.lastMemAddr < cpu.mem.length) {
                    value = cpu.mem[cpu.lastMemAddr];
                } else if (cpu.lastRegWrite >= 0 && cpu.lastRegWrite < cpu.regs.length) {
                    value = cpu.regs[cpu.lastRegWrite];
                } else if (cpu.lastOut >= 0) {
                    value = cpu.lastOut;
                } else if (cpu.regs.length > 0) {
                    value = cpu.regs[0];
                }
            }
            busValue = value & chip.mask();
        }

        int memVal(int idx) {
            if (cpu != null) return idx >= 0 && idx < cpu.mem.length ? cpu.mem[idx] : 0;
            if (!chip.isCpuLike() && idx >= 0 && idx < chip.program.length) {
                return chip.program[idx] & chip.mask();
            }
            return 0;
        }

        int regVal(int idx) {
            return cpu != null && idx >= 0 && idx < cpu.regs.length ? cpu.regs[idx] : 0;
        }

        long cycles() {
            return cpu != null ? cpu.cycles : 0L;
        }

        float accessGlow() {
            if (cpu == null || cpu.lastMemAddr < 0) return 0f;
            long age = cpu.cycles - cpu.lastMemCycle;
            if (age <= 2) return 1f;
            if (age <= 16) return 0.72f;
            return 0.42f;
        }
    }

    public final ArrayList<Seg> segs = new ArrayList<Seg>();
    public final ArrayList<Seg> staticSegs = new ArrayList<Seg>();
    public final ArrayList<Seg> dynamicSegs = new ArrayList<Seg>();
    public final ArrayList<Label> labels = new ArrayList<Label>();
    public final ArrayList<Area> blockAreas = new ArrayList<Area>();

    public float widthU, heightU;
    /** Whole die only: active floorplan inside the pad/seal ring. */
    public float innerX, innerY, innerW, innerH;

    private long rngState;
    private int logicSerial = 17;
    private boolean detailedLabels;
    private final ArrayList<float[]> padAnchors = new ArrayList<float[]>();

    private DieLayout(long seed, float w, float h) {
        rngState = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        widthU = w;
        heightU = h;
        innerX = 0; innerY = 0; innerW = w; innerH = h;
    }

    // ------------------------------------------------------------------ RNG
    private long nextLong() {
        long x = rngState;
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        rngState = x;
        return x;
    }

    private int rnd(int bound) {
        if (bound <= 1) return 0;
        return (int) ((nextLong() >>> 16) % bound);
    }

    private float rndRange(float lo, float hi) {
        return lo + (rnd(1000) / 999f) * (hi - lo);
    }

    private boolean chance(int pct) {
        return rnd(100) < pct;
    }

    private int hash2(int x, int y, int salt) {
        int h = x * 374761393 + y * 668265263 + salt * 1274126177;
        h ^= h >>> 13;
        h *= 1274126177;
        return h ^ (h >>> 16);
    }

    private int nextLogicId() {
        logicSerial += 1 + rnd(3);
        return logicSerial;
    }

    // --------------------------------------------------------------- segments
    private static int zForLayer(int layer) {
        switch (layer) {
            case L_WELL: return 0;
            case L_NDIFF: return 10;
            case L_PDIFF: return 11;
            case L_PIXEL: return 15;
            case L_POLY: return 20;
            case L_CONTACT: return 30;
            case L_METAL1: return 40;
            case L_VIA: return 50;
            case L_METAL2: return 60;
            case L_METAL3: return 70;
            case L_PAD: return 80;
            case L_MARK: return 90;
            default: return 25;
        }
    }

    private Seg add(int layer, float x, float y, float w, float h, int dyn, int a, int b) {
        // Clip to die.  This also makes bounds a testable API invariant for
        // imported chips with extreme block coordinates.
        if (x < 0) { w += x; x = 0; }
        if (y < 0) { h += y; y = 0; }
        if (x + w > widthU) w = widthU - x;
        if (y + h > heightU) h = heightU - y;
        if (w <= 0.03f || h <= 0.03f || x >= widthU || y >= heightU) return null;
        int shade = hash2((int) (x * 3f) + layer * 131, (int) (y * 3f) - layer * 17, (int) (w * 7f + h * 11f)) & 15;
        Seg s = new Seg(layer, x, y, w, h, dyn, a, b, shade);
        segs.add(s);
        return s;
    }

    private Seg add(int layer, float x, float y, float w, float h) {
        return add(layer, x, y, w, h, D_NONE, 0, 0);
    }

    private void hLine(int layer, float x, float y, float len, float thick, int dyn, int a, int b) {
        add(layer, x, y - thick / 2f, len, thick, dyn, a, b);
    }

    private void vLine(int layer, float x, float y, float len, float thick, int dyn, int a, int b) {
        add(layer, x - thick / 2f, y, thick, len, dyn, a, b);
    }

    private void contact(float cx, float cy, float size) {
        add(L_CONTACT, cx - size / 2f, cy - size / 2f, size, size);
    }

    private void via(float cx, float cy, float size) {
        add(L_VIA, cx - size / 2f, cy - size / 2f, size, size);
    }

    private void manhattan(int layer, float x1, float y1, float x2, float y2,
                           float thick, int dyn, int a, int b, boolean horizontalFirst) {
        if (Math.abs(x2 - x1) < 0.1f || Math.abs(y2 - y1) < 0.1f) {
            add(layer, Math.min(x1, x2), Math.min(y1, y2),
                    Math.max(thick, Math.abs(x2 - x1)), Math.max(thick, Math.abs(y2 - y1)), dyn, a, b);
            return;
        }
        if (horizontalFirst) {
            hLine(layer, Math.min(x1, x2), y1, Math.abs(x2 - x1), thick, dyn, a, b);
            vLine(layer, x2, Math.min(y1, y2), Math.abs(y2 - y1), thick, dyn, a, b);
            via(x2, y1, thick * 1.35f);
        } else {
            vLine(layer, x1, Math.min(y1, y2), Math.abs(y2 - y1), thick, dyn, a, b);
            hLine(layer, Math.min(x1, x2), y2, Math.abs(x2 - x1), thick, dyn, a, b);
            via(x1, y2, thick * 1.35f);
        }
    }

    private void label(String text, float x, float y, float size, int color) {
        if (detailedLabels) labels.add(new Label(text, x, y, size, color));
    }

    private void finish() {
        Collections.sort(segs, new Comparator<Seg>() {
            public int compare(Seg a, Seg b) {
                int d = zForLayer(a.layer) - zForLayer(b.layer);
                return d != 0 ? d : a.layer - b.layer;
            }
        });
        staticSegs.clear();
        dynamicSegs.clear();
        for (int i = 0; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (s.dyn == D_NONE) staticSegs.add(s);
            else dynamicSegs.add(s);
        }
    }

    public static boolean isDynamic(Seg s) {
        return s.dyn != D_NONE;
    }

    // ---------------------------------------------------------------- colours
    public static int baseColor(Seg s) {
        int base = LAYER_COLORS[s.layer];
        float f = 0.86f + (s.shade % 6) * 0.045f;
        return scale(base, f);
    }

    private static int energized(Seg s, int base, float amount) {
        if (amount <= 0f) return s.layer == L_MARK ? 0x00000000 : base;
        if (s.layer == L_MARK) return HOT;
        if (s.layer == L_NDIFF || s.layer == L_PDIFF || s.layer == L_WELL) {
            return blend(base, HOT_DIM, 0.30f * amount);
        }
        if (s.layer == L_CONTACT || s.layer == L_VIA) {
            return blend(base, HOT, 0.45f * amount);
        }
        return blend(base, HOT, Math.min(1f, 0.88f * amount));
    }

    /** Final colour, including live signals.  Transparent means "do not draw". */
    public static int colorFor(Seg s, Ctx ctx) {
        int base = baseColor(s);
        if (s.dyn == D_NONE) return base;
        ChipBin chip = ctx.chip;
        Cpu cpu = ctx.cpu;
        switch (s.dyn) {
            case D_MEM_VAL: {
                float t = ctx.memVal(s.a) / (float) Math.max(1, chip.mask());
                return energized(s, base, t);
            }
            case D_MEM_BIT:
                return energized(s, base, ((ctx.memVal(s.a) >> s.b) & 1) == 1 ? 1f : 0f);
            case D_MEM_NBIT:
                return energized(s, base, ((ctx.memVal(s.a) >> s.b) & 1) == 0 ? 0.82f : 0f);
            case D_REG_BIT:
                return energized(s, base, ((ctx.regVal(s.a) >> s.b) & 1) == 1 ? 1f : 0f);
            case D_REG_NBIT:
                return energized(s, base, ((ctx.regVal(s.a) >> s.b) & 1) == 0 ? 0.82f : 0f);
            case D_REGROW: {
                boolean on = cpu != null && (s.a == cpu.lastRegWrite || s.a == cpu.lastRegRead);
                return energized(s, base, on ? 0.9f : 0f);
            }
            case D_WL: {
                int cols = Math.max(1, s.b);
                boolean on = cpu != null && cpu.lastMemAddr >= 0 && cpu.lastMemAddr / cols == s.a;
                return energized(s, base, on ? ctx.accessGlow() : 0f);
            }
            case D_BL: {
                int cols = Math.max(1, s.b);
                boolean on = cpu != null && cpu.lastMemAddr >= 0 && cpu.lastMemAddr % cols == s.a;
                int c = energized(s, base, on ? ctx.accessGlow() : 0f);
                return on && cpu.lastMemWrite && s.layer != L_MARK ? blend(c, 0xFFFF5252, 0.35f) : c;
            }
            case D_ALU_A:
                return energized(s, base, cpu != null && ((cpu.aluA >> s.a) & 1) == 1 ? 1f : 0f);
            case D_ALU_B:
                return energized(s, base, cpu != null && ((cpu.aluB >> s.a) & 1) == 1 ? 1f : 0f);
            case D_ALU_S:
                return energized(s, base, cpu != null && ((cpu.aluResult >> s.a) & 1) == 1 ? 1f : 0f);
            case D_CARRY: {
                boolean on = s.a >= 0 && s.a < ctx.carryInto.length && ctx.carryInto[s.a] == 1;
                return energized(s, base, on ? 1f : 0f);
            }
            case D_BUS:
                return energized(s, base, ((ctx.busValue >> s.a) & 1) == 1 ? 0.95f : 0f);
            case D_LOGIC: {
                long h = (s.a * 2654435761L) ^ (ctx.cycles() * 0x9E3779B97F4A7C15L) ^ ((long) (s.a % 29) << 17);
                h ^= h >>> 29;
                boolean on = (h & 1L) == 1L;
                return energized(s, base, on ? 0.62f : 0f);
            }
            case D_CLK:
                return energized(s, base, (ctx.cycles() & 1L) == (s.a & 1) ? 0.55f : 0f);
            case D_SCREEN: {
                int v;
                if (cpu != null && cpu.framebuffer != null && s.a >= 0 && s.a < cpu.framebuffer.length) {
                    v = cpu.framebuffer[s.a] & 15;
                } else {
                    v = (s.a * 7 + (s.a >> 3)) & 15;
                }
                int pc = SCREEN_PALETTE[v];
                return s.layer == L_PIXEL ? scale(pc, 0.92f) : (v == 0 ? base : blend(base, pc, 0.45f));
            }
            case D_SCAN_ROW: {
                boolean on = cpu != null && cpu.framebuffer != null && chip.screenW > 0
                        && cpu.fbCursor / chip.screenW == s.a;
                return energized(s, base, on ? 0.9f : 0f);
            }
            case D_SCAN_COL: {
                boolean on = cpu != null && cpu.framebuffer != null && chip.screenW > 0
                        && cpu.fbCursor % chip.screenW == s.a;
                return energized(s, base, on ? 0.9f : 0f);
            }
            default:
                return base;
        }
    }

    static int scale(int argb, float f) {
        int a = argb & 0xFF000000;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((argb & 0xFF) * f));
        return a | (r << 16) | (g << 8) | b;
    }

    static int blend(int a, int b, float t) {
        if (t < 0) t = 0; else if (t > 1) t = 1;
        int aa = (int) ((a >>> 24) + ((b >>> 24) - (a >>> 24)) * t);
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return (aa << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    // ----------------------------------------------------------- common pieces
    private void rails(float x, float y, float w, float h) {
        if (w < 12 || h < 14) return;
        float th = clamp(h * 0.035f, 1.6f, 4.2f);
        add(L_METAL1, x, y, w, th);
        add(L_METAL2, x, y + h - th, w, th);
        float step = Math.max(14f, w / 22f);
        for (float vx = x + step * 0.55f; vx < x + w - 2; vx += step) {
            contact(vx, y + th / 2f, Math.min(3.2f, th * 0.72f));
            via(vx, y + h - th / 2f, Math.min(3.2f, th * 0.72f));
            if (h > 70) {
                vLine(L_METAL2, vx, y + th, Math.min(13f, h * 0.12f), 1.25f, D_NONE, 0, 0);
                vLine(L_METAL1, vx, y + h - th - Math.min(13f, h * 0.12f), Math.min(13f, h * 0.12f), 1.25f, D_NONE, 0, 0);
            }
        }
        label("VDD", x + 5, y + th + 11, 11, 0xFFFFD9EF);
        label("GND", x + 5, y + h - th - 4, 11, 0xFFD6F4FF);
    }

    private void guardRing(float x, float y, float w, float h) {
        // Two seal rings plus a via fence, as rectangles (all geometry is Manhattan).
        for (int i = 0; i < 2; i++) {
            float inset = i * 4.2f;
            int layer = i == 0 ? L_METAL1 : L_METAL2;
            add(layer, x + inset, y + inset, w - inset * 2, 1.5f);
            add(layer, x + inset, y + h - inset - 1.5f, w - inset * 2, 1.5f);
            add(layer, x + inset, y + inset, 1.5f, h - inset * 2);
            add(layer, x + w - inset - 1.5f, y + inset, 1.5f, h - inset * 2);
        }
        for (float px = x + 6; px < x + w - 5; px += 11f) {
            via(px, y + 2.2f, 2.0f);
            via(px, y + h - 2.2f, 2.0f);
        }
        for (float py = y + 6; py < y + h - 5; py += 11f) {
            via(x + 2.2f, py, 2.0f);
            via(x + w - 2.2f, py, 2.0f);
        }
    }

    /** One enlarged 6T SRAM layout, reused whenever a cell is large enough. */
    private void genSramCellGeometry(float x, float y, float w, float h,
                                      int qDyn, int qNa, int qNb,
                                      int invDyn, boolean annotate) {
        // Well and short supply stubs.  The shared rails/bitlines belong to
        // the enclosing array; drawing a full frame in every cell made the
        // macro view look like icons instead of abutted layout.
        add(L_WELL, x, y, w, h * 0.43f);
        vLine(L_METAL1, x + w * 0.20f, y, h * 0.16f, Math.min(3f, Math.max(1.6f, w * 0.025f)), D_NONE, 0, 0);
        vLine(L_METAL1, x + w * 0.80f, y, h * 0.16f, Math.min(3f, Math.max(1.6f, w * 0.025f)), D_NONE, 0, 0);
        vLine(L_METAL2, x + w * 0.20f, y + h * 0.86f, h * 0.14f, Math.min(3f, Math.max(1.6f, w * 0.025f)), D_NONE, 0, 0);
        vLine(L_METAL2, x + w * 0.80f, y + h * 0.86f, h * 0.14f, Math.min(3f, Math.max(1.6f, w * 0.025f)), D_NONE, 0, 0);

        // PMOS diffusion pair (top) and NMOS pull-down/access diffusion (bottom).
        add(L_PDIFF, x + w * 0.16f, y + h * 0.13f, w * 0.26f, h * 0.23f);
        add(L_PDIFF, x + w * 0.58f, y + h * 0.13f, w * 0.26f, h * 0.23f);
        add(L_NDIFF, x + w * 0.16f, y + h * 0.48f, w * 0.26f, h * 0.24f);
        add(L_NDIFF, x + w * 0.58f, y + h * 0.48f, w * 0.26f, h * 0.24f);
        add(L_NDIFF, x + w * 0.10f, y + h * 0.75f, w * 0.32f, h * 0.16f);
        add(L_NDIFF, x + w * 0.58f, y + h * 0.75f, w * 0.32f, h * 0.16f);
        // Shared diffusion: common PMOS source bar and pull-down/access drain
        // bridges turn four separate blocks into one interlocking cell shape.
        add(L_PDIFF, x + w * 0.24f, y + h * 0.16f, w * 0.52f, h * 0.075f);
        add(L_NDIFF, x + w * 0.21f, y + h * 0.66f, w * 0.16f, h * 0.14f);
        add(L_NDIFF, x + w * 0.63f, y + h * 0.66f, w * 0.16f, h * 0.14f);
        add(L_NDIFF, x + w * 0.02f, y + h * 0.78f, w * 0.16f, h * 0.10f);
        add(L_NDIFF, x + w * 0.82f, y + h * 0.78f, w * 0.16f, h * 0.10f);

        // Two inverter poly gates and the access-transistor wordline.
        float gateW = Math.min(4.5f, Math.max(2f, w * 0.045f));
        add(L_POLY, x + w * 0.255f, y + h * 0.07f, gateW, h * 0.68f);
        add(L_POLY, x + w * 0.675f, y + h * 0.07f, gateW, h * 0.68f);
        add(L_POLY, x + w * 0.255f, y + h * 0.39f, w * 0.475f, Math.min(2.6f, Math.max(1.5f, h * 0.025f)));
        add(L_POLY, x, y + h * 0.815f, w, Math.min(3.2f, Math.max(1.8f, h * 0.03f)));

        // Q and Q-bar local interconnect: thin stepped conductors, not plates.
        float thin = Math.min(3f, Math.max(1.6f, w * 0.028f));
        hLine(L_METAL1, x + w * 0.20f, y + h * 0.405f, w * 0.50f, thin, qDyn, qNa, qNb);
        hLine(L_METAL1, x + w * 0.30f, y + h * 0.465f, w * 0.50f, thin, invDyn, qNa, qNb);
        vLine(L_METAL1, x + w * 0.20f, y + h * 0.31f, h * 0.17f, thin, qDyn, qNa, qNb);
        vLine(L_METAL1, x + w * 0.80f, y + h * 0.31f, h * 0.17f, thin, invDyn, qNa, qNb);

        // Channel markers only at actual gate/diffusion intersections.
        float markW = Math.max(2.4f, w * 0.075f);
        add(L_MARK, x + w * 0.235f, y + h * 0.20f, markW, h * 0.085f, invDyn, qNa, qNb); // left PMOS
        add(L_MARK, x + w * 0.655f, y + h * 0.20f, markW, h * 0.085f, qDyn, qNa, qNb);   // right PMOS
        add(L_MARK, x + w * 0.235f, y + h * 0.56f, markW, h * 0.085f, invDyn, qNa, qNb); // left NMOS
        add(L_MARK, x + w * 0.655f, y + h * 0.56f, markW, h * 0.085f, qDyn, qNa, qNb);   // right NMOS

        // Bitlines and small contacts.
        add(L_METAL2, x + w * 0.045f, y, Math.max(2f, w * 0.045f), h);
        add(L_METAL2, x + w * 0.915f, y, Math.max(2f, w * 0.045f), h);
        float cs = clamp(Math.min(w, h) * 0.055f, 2f, 5f);
        contact(x + w * 0.20f, y + h * 0.17f, cs);
        contact(x + w * 0.80f, y + h * 0.17f, cs);
        contact(x + w * 0.20f, y + h * 0.67f, cs);
        contact(x + w * 0.80f, y + h * 0.67f, cs);
        contact(x + w * 0.16f, y + h * 0.84f, cs);
        contact(x + w * 0.84f, y + h * 0.84f, cs);
        via(x + w * 0.29f, y + h * 0.405f, cs);
        via(x + w * 0.71f, y + h * 0.465f, cs);

        if (annotate) {
            label("Q", x + w * 0.13f, y + h * 0.38f, clamp(h * 0.11f, 9, 16), 0xFFFFFFFF);
            label("/Q", x + w * 0.78f, y + h * 0.52f, clamp(h * 0.11f, 9, 16), 0xFFFFFFFF);
            label("BL", x + w * 0.02f, y + 11, 9, 0xFFFFD9EF);
            label("WL", x + w * 0.90f, y + h * 0.80f, 9, 0xFFFFD9D0);
        }
    }

    /** SRAM arrays abut mirrored cells so neighbours share rails/bitlines. */
    private void genSramCellMirrored(float x, float y, float w, float h,
                                     int qDyn, int qNa, int qNb, int invDyn,
                                     boolean annotate, boolean flipX, boolean flipY) {
        int start = segs.size();
        genSramCellGeometry(x, y, w, h, qDyn, qNa, qNb, invDyn, annotate);
        if (!flipX && !flipY) return;
        for (int i = start; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (flipX) s.x = 2f * x + w - s.x - s.w;
            if (flipY) s.y = 2f * y + h - s.y - s.h;
        }
    }

    /** Tiny physical bit-cell used in dense arrays. */
    private void genBitCell(float x, float y, float w, float h,
                            int qDyn, int qNa, int qNb, int invDyn) {
        if (w >= 52 && h >= 44) {
            genSramCellGeometry(x, y, w, h, qDyn, qNa, qNb, invDyn, false);
            return;
        }
        add(L_NDIFF, x + w * 0.10f, y + h * 0.18f, w * 0.31f, h * 0.64f, qDyn, qNa, qNb);
        add(L_PDIFF, x + w * 0.58f, y + h * 0.18f, w * 0.31f, h * 0.64f, invDyn, qNa, qNb);
        add(L_POLY, x + w * 0.205f, y + h * 0.07f, Math.max(0.85f, w * 0.085f), h * 0.86f);
        add(L_POLY, x + w * 0.685f, y + h * 0.07f, Math.max(0.85f, w * 0.085f), h * 0.86f);
        hLine(L_METAL1, x + w * 0.18f, y + h * 0.47f, w * 0.62f, Math.max(0.8f, h * 0.09f), qDyn, qNa, qNb);
        if (w >= 9 && h >= 8) {
            contact(x + w * 0.48f, y + h * 0.24f, clamp(Math.min(w, h) * 0.13f, 0.8f, 2.4f));
            contact(x + w * 0.48f, y + h * 0.74f, clamp(Math.min(w, h) * 0.13f, 0.8f, 2.4f));
        } else {
            add(L_MARK, x + w * 0.19f, y + h * 0.42f, Math.max(0.7f, w * 0.13f), Math.max(0.7f, h * 0.13f), qDyn, qNa, qNb);
        }
    }

    private void genArrayPeriphery(float x, float y, float w, float h, float leftW, float bottomH) {
        // Row decoder: alternating NAND-tree fingers on the left.
        add(L_WELL, x, y, leftW, h);
        for (float py = y + 5; py < y + h - 7; py += 9.5f) {
            add(L_POLY, x + 2.5f, py, leftW - 5f, 1.35f, D_LOGIC, nextLogicId(), 0);
            add(L_NDIFF, x + 4f, py + 2.2f, leftW * 0.32f, 4.2f);
            add(L_PDIFF, x + leftW * 0.55f, py + 2.2f, leftW * 0.32f, 4.2f);
            contact(x + leftW * (0.25f + 0.15f * ((int) (py / 9.5f) % 4)), py + 4.2f, 1.7f);
        }
        // Sense amplifiers / column mux along the bottom.
        float sy = y + h - bottomH;
        add(L_WELL, x, sy, w, bottomH);
        for (float px = x + leftW + 4; px < x + w - 8; px += 11f) {
            add(L_POLY, px, sy + 3f, 1.4f, bottomH - 6f, D_LOGIC, nextLogicId(), 0);
            add(L_NDIFF, px + 2.2f, sy + bottomH * 0.20f, 5.5f, bottomH * 0.26f);
            add(L_PDIFF, px + 2.2f, sy + bottomH * 0.56f, 5.5f, bottomH * 0.26f);
            add(L_METAL1, px + 1.4f, sy + bottomH * 0.46f, 7f, 1.15f, D_LOGIC, nextLogicId(), 0);
            contact(px + 4.5f, sy + bottomH * 0.32f, 1.6f);
        }
        label("DEC", x + 3, y + 13, 9, 0xFFE8F8FF);
        label("SENSE AMP", x + leftW + 6, y + h - 5, 9, 0xFFE8F8FF);
    }

    /** Dense physical memory: one layout microcell per stored bit. */
    private void genMemoryArray(float x, float y, float w, float h, ChipBin chip) {
        rails(x, y, w, h);
        int words = Math.max(1, chip.memSize);
        int bits = Math.max(1, chip.bits);
        float leftW = clamp(w * 0.085f, 12f, 42f);
        float bottomH = clamp(h * 0.10f, 12f, 38f);
        float ax = x + leftW + 3f, ay = y + 6f;
        float aw = w - leftW - 7f, ah = h - bottomH - 10f;
        genArrayPeriphery(x, y + 3f, w, h - 3f, leftW, bottomH);

        int bitCols = Math.max(1, (int) Math.ceil(Math.sqrt(bits * 1.55f)));
        int bitRows = Math.max(1, (bits + bitCols - 1) / bitCols);
        float macroAspect = bitCols / (float) bitRows * 0.92f;
        int wordCols = clampInt(Math.round((float) Math.sqrt(words * (aw / Math.max(1f, ah)) / macroAspect)), 1, words);
        int wordRows = (words + wordCols - 1) / wordCols;
        float macroW = aw / wordCols, macroH = ah / wordRows;

        // Global wordlines and macro-column bitline trunks.
        for (int r = 0; r < wordRows; r++) {
            float wy = ay + r * macroH + macroH * 0.50f;
            add(L_POLY, x + 1.5f, wy - Math.min(1.15f, macroH * 0.06f), w - 3f,
                    Math.min(2.3f, Math.max(0.75f, macroH * 0.11f)), D_WL, r, wordCols);
        }
        for (int c = 0; c < wordCols; c++) {
            float bx = ax + c * macroW + macroW * 0.50f;
            vLine(L_METAL2, bx, ay - 1.5f, ah + 3f, Math.min(2f, Math.max(0.7f, macroW * 0.055f)), D_BL, c, wordCols);
        }

        float bitW = macroW / bitCols, bitH = macroH / bitRows;
        for (int word = 0; word < words; word++) {
            int wc = word % wordCols, wr = word / wordCols;
            float mx = ax + wc * macroW, my = ay + wr * macroH;
            if (((word + wr) & 1) == 0) add(L_WELL, mx, my, macroW, macroH);
            for (int bit = 0; bit < bits; bit++) {
                int bc = bit % bitCols, br = bit / bitCols;
                genBitCell(mx + bc * bitW, my + br * bitH, bitW, bitH,
                        D_MEM_BIT, word, bit, D_MEM_NBIT);
            }
        }
    }

    /** Registers: one obvious SRAM/latch cell per register bit. */
    private void genRegisterArray(float x, float y, float w, float h, ChipBin chip) {
        rails(x, y, w, h);
        float leftW = clamp(w * 0.07f, 10f, 34f);
        float bottomH = clamp(h * 0.09f, 10f, 30f);
        genArrayPeriphery(x, y + 3f, w, h - 3f, leftW, bottomH);
        int regs = Math.max(1, chip.regs), bits = Math.max(1, chip.bits);
        float ax = x + leftW + 3, ay = y + 6;
        float aw = w - leftW - 7, ah = h - bottomH - 10;
        float cw = aw / bits, ch = ah / regs;
        for (int r = 0; r < regs; r++) {
            float wy = ay + r * ch + ch * 0.50f;
            add(L_POLY, x + 1.5f, wy - 1f, w - 3, Math.min(2.4f, Math.max(0.9f, ch * 0.08f)), D_REGROW, r, 0);
            for (int bc = 0; bc < bits; bc++) {
                int bit = bits - 1 - bc;
                genBitCell(ax + bc * cw, ay + r * ch, cw, ch, D_REG_BIT, r, bit, D_REG_NBIT);
            }
            if (detailedLabels) label("R" + r, x + 4, ay + r * ch + Math.min(13f, ch * 0.45f), 9, 0xFFFFFFFF);
        }
        for (int bc = 0; bc < bits; bc++) {
            vLine(L_METAL2, ax + bc * cw + cw * 0.5f, ay - 1f, ah + 2f,
                    Math.min(2f, Math.max(0.7f, cw * 0.06f)), D_REG_BIT, 0, bits - 1 - bc);
        }
    }

    /** Framebuffer / display matrix with a TFT at every actual pixel. */
    private void genScreenArray(float x, float y, float w, float h, ChipBin chip) {
        if (!chip.hasScreen()) {
            genMemoryArray(x, y, w, h, chip);
            return;
        }
        rails(x, y, w, h);
        int cols = chip.screenW, rows = chip.screenH;
        float leftW = clamp(w * 0.06f, 9f, 28f), bottomH = clamp(h * 0.08f, 9f, 26f);
        genArrayPeriphery(x, y + 3, w, h - 3, leftW, bottomH);
        float ax = x + leftW + 3, ay = y + 5;
        float aw = w - leftW - 7, ah = h - bottomH - 9;
        float cw = aw / cols, ch = ah / rows;
        for (int r = 0; r < rows; r++) {
            hLine(L_POLY, ax, ay + r * ch + ch * 0.14f, aw, Math.max(0.8f, ch * 0.075f), D_SCAN_ROW, r, 0);
            for (int c = 0; c < cols; c++) {
                float px = ax + c * cw, py = ay + r * ch;
                int idx = r * cols + c;
                add(L_PIXEL, px + cw * 0.20f, py + ch * 0.22f, cw * 0.68f, ch * 0.65f, D_SCREEN, idx, 0);
                add(L_NDIFF, px + cw * 0.07f, py + ch * 0.05f, cw * 0.27f, ch * 0.22f);
                add(L_MARK, px + cw * 0.14f, py + ch * 0.11f, Math.max(0.8f, cw * 0.10f), Math.max(0.8f, ch * 0.08f), D_SCAN_ROW, r, 0);
                if (cw > 12 && ch > 10) {
                    contact(px + cw * 0.25f, py + ch * 0.18f, clamp(Math.min(cw, ch) * 0.12f, 0.9f, 2.4f));
                    hLine(L_METAL1, px + cw * 0.18f, py + ch * 0.25f, cw * 0.30f, Math.max(0.7f, ch * 0.06f), D_SCREEN, idx, 0);
                    // Storage-capacitor stripes along the lower edge.
                    add(L_POLY, px + cw * 0.18f, py + ch * 0.88f, cw * 0.70f, Math.max(0.8f, ch * 0.07f));
                }
            }
        }
        for (int c = 0; c < cols; c++) {
            vLine(L_METAL2, ax + c * cw + cw * 0.10f, ay, ah, Math.max(0.75f, cw * 0.075f), D_SCAN_COL, c, 0);
        }
    }

    // --------------------------------------------------------------- datapath
    private void genGateStage(float x, float y, float w, float h, int bit,
                              int dyn0, int dyn1, int dyn2, int stage) {
        add(L_WELL, x, y, w, h * 0.43f);
        int[] dyns = {dyn0, dyn1, dyn2};
        float gateW = clamp(w * 0.035f, 1.1f, 4.2f);
        for (int g = 0; g < 3; g++) {
            float gx = x + w * (0.24f + 0.24f * g) + ((stage + g) % 2) * w * 0.025f;
            float islandW = w * 0.20f;
            add(L_PDIFF, gx - islandW / 2, y + h * 0.12f, islandW, h * 0.20f);
            add(L_NDIFF, gx - islandW / 2, y + h * 0.62f, islandW, h * 0.20f);
            add(L_POLY, gx - gateW / 2, y + h * 0.05f, gateW, h * 0.86f, dyns[g], bit, 0);
            float markW = Math.max(1.2f, gateW * 1.55f);
            add(L_MARK, gx - markW / 2, y + h * 0.18f, markW, h * 0.085f, dyns[g], bit, 0);
            add(L_MARK, gx - markW / 2, y + h * 0.68f, markW, h * 0.085f, dyns[g], bit, 0);
            float cs = clamp(Math.min(w, h) * 0.045f, 1.1f, 3.3f);
            contact(gx - islandW * 0.30f, y + h * 0.22f, cs);
            contact(gx + islandW * 0.30f, y + h * 0.22f, cs);
            contact(gx - islandW * 0.30f, y + h * 0.72f, cs);
            contact(gx + islandW * 0.30f, y + h * 0.72f, cs);
        }
        // Local nodes between transistor drains.
        hLine(L_METAL1, x + w * 0.14f, y + h * 0.43f, w * 0.62f, Math.max(1f, h * 0.045f),
                stage >= 2 ? D_ALU_S : D_LOGIC, stage >= 2 ? bit : nextLogicId(), 0);
        hLine(L_METAL1, x + w * 0.22f, y + h * 0.55f, w * 0.58f, Math.max(1f, h * 0.04f),
                stage == 3 ? D_CARRY : D_LOGIC, stage == 3 ? bit + 1 : nextLogicId(), 0);
        vLine(L_METAL2, x + w * 0.82f, y + h * 0.20f, h * 0.62f, Math.max(1f, w * 0.028f),
                stage >= 2 ? D_ALU_S : D_LOGIC, stage >= 2 ? bit : nextLogicId(), 0);
    }

    private void genAlu(float x, float y, float w, float h, int bits) {
        rails(x, y, w, h);
        float top = y + 7f, ah = h - 14f;
        float sliceW = w / Math.max(1, bits);
        // Four gate stages through every slice: generate/propagate/sum/carry.
        for (int i = 0; i < bits; i++) {
            int bit = bits - 1 - i;
            float sx = x + i * sliceW;
            for (int stage = 0; stage < 4; stage++) {
                int d0 = stage == 0 ? D_ALU_A : stage == 1 ? D_CARRY : stage == 2 ? D_ALU_A : D_ALU_B;
                int d1 = stage == 0 ? D_ALU_B : stage == 1 ? D_ALU_A : stage == 2 ? D_CARRY : D_ALU_A;
                int d2 = stage == 3 ? D_CARRY : D_ALU_B;
                genGateStage(sx + 1.2f, top + stage * ah / 4f, sliceW - 2.4f, ah / 4f - 1.5f,
                        bit, d0, d1, d2, stage);
            }
            // Slice input/output trunks.
            vLine(L_METAL2, sx + sliceW * 0.10f, top - 2f, ah * 0.55f, Math.max(0.9f, sliceW * 0.045f), D_ALU_A, bit, 0);
            vLine(L_METAL2, sx + sliceW * 0.90f, top - 2f, ah * 0.55f, Math.max(0.9f, sliceW * 0.045f), D_ALU_B, bit, 0);
            vLine(L_METAL1, sx + sliceW * 0.50f, top + ah * 0.43f, ah * 0.58f, Math.max(1f, sliceW * 0.05f), D_ALU_S, bit, 0);
            if (detailedLabels) {
                label("A" + bit, sx + sliceW * 0.04f, top - 1f, 8, 0xFFFFFFFF);
                label("S" + bit, sx + sliceW * 0.52f, y + h - 1f, 8, 0xFFFFFFFF);
            }
        }
        // Carry ripples from low bit (right) to high bit (left), broken per slice
        // so its colour really is carry-into-that-bit.
        float carryY = top + ah * 0.86f;
        for (int i = 0; i < bits; i++) {
            int bit = bits - 1 - i;
            float sx = x + i * sliceW;
            hLine(L_METAL2, sx, carryY, sliceW, Math.max(1.2f, ah * 0.018f), D_CARRY, bit, 0);
            via(sx + sliceW * 0.72f, carryY, 2.2f);
        }
    }

    /** One full-adder bit-slice drawn as a real standard-cell row. */
    private void genAdderRow(float x, float y, float w, float h, int bit, boolean flip) {
        // Shared supply rails on both edges; adjacent rows are mirrored.
        hLine(L_METAL1, x, y + 3f, w, 2.6f, D_NONE, 0, 0);
        hLine(L_METAL1, x, y + h - 3f, w, 2.6f, D_NONE, 0, 0);
        float bandH = h * 0.24f;
        float pY = flip ? y + h * 0.60f : y + h * 0.14f;
        float nY = flip ? y + h * 0.14f : y + h * 0.60f;
        add(L_WELL, x, pY - h * 0.055f, w, bandH + h * 0.11f);

        final int gates = 18;
        float gx0 = x + 42f, pitch = (w - 104f) / (gates - 1);
        int[] pat = {D_ALU_A, D_ALU_B, D_ALU_A, D_ALU_B, D_CARRY, D_ALU_B,
                D_ALU_A, D_CARRY, D_ALU_A, D_ALU_B, D_CARRY, D_ALU_S,
                D_ALU_A, D_ALU_B, D_CARRY, D_ALU_B, D_ALU_A, D_CARRY};
        float prevGx = 0f;
        for (int g = 0; g < gates; g++) {
            float gx = gx0 + g * pitch;
            int dyn = pat[g];
            int darg = dyn == D_CARRY ? bit : bit;
            if (g % 3 == 0) {
                // One continuous diffusion strip per three-gate cluster:
                // neighbouring transistors share source/drain regions.
                float stripX = gx - pitch * 0.48f, stripW = pitch * 2.96f;
                add(L_PDIFF, stripX, pY, stripW, bandH);
                add(L_NDIFF, stripX, nY, stripW, bandH);
            }
            vLine(L_POLY, gx, y + h * 0.07f, h * 0.86f, 2.6f, dyn, darg, 0);
            add(L_MARK, gx - 2f, pY + bandH * 0.34f, 4f, bandH * 0.32f, dyn, darg, 0);
            add(L_MARK, gx - 2f, nY + bandH * 0.34f, 4f, bandH * 0.32f, dyn, darg, 0);
            contact(gx - pitch * 0.25f, pY + bandH * 0.50f, 2.8f);
            contact(gx + pitch * 0.25f, pY + bandH * 0.50f, 2.8f);
            contact(gx - pitch * 0.25f, nY + bandH * 0.50f, 2.8f);
            contact(gx + pitch * 0.25f, nY + bandH * 0.50f, 2.8f);
            if (g % 3 == 1) {
                vLine(L_METAL1, gx - pitch * 0.25f, Math.min(pY, nY) + bandH * 0.45f,
                        Math.abs(nY - pY) + bandH * 0.1f, 1.5f, D_LOGIC, nextLogicId(), 0);
            }
            if (g % 3 == 2) {
                int cluster = g / 3;
                float nodeY = y + h * (0.47f + (cluster % 3 - 1) * 0.055f);
                int ndyn = cluster == 3 ? D_ALU_S : cluster >= 4 ? D_CARRY : D_LOGIC;
                int narg = ndyn == D_LOGIC ? nextLogicId() : ndyn == D_CARRY ? bit + 1 : bit;
                hLine(L_METAL1, gx - pitch * 2.25f, nodeY, pitch * 2.5f, 1.8f, ndyn, narg, 0);
                vLine(L_METAL2, gx - pitch * 0.9f, Math.min(pY, nY) + bandH * 0.5f,
                        Math.abs(nY - pY), 1.5f, ndyn, narg, 0);
                via(gx - pitch * 0.9f, nodeY, 2.6f);
            }
            if (g % 5 == 2) {
                via(gx, y + 3f, 2.4f);
                via(gx, y + h - 3f, 2.4f);
            }
            prevGx = gx;
        }
        // Dummy end gates and cell-boundary ties.
        vLine(L_POLY, x + 14f, y + h * 0.07f, h * 0.86f, 2.2f, D_NONE, 0, 0);
        vLine(L_POLY, x + w - 14f, y + h * 0.07f, h * 0.86f, 2.2f, D_NONE, 0, 0);
        // Carry-out leaves the majority cluster towards the right spine.
        hLine(L_METAL2, gx0 + 14f * pitch, y + h * 0.50f, w - 104f - 14f * pitch + 44f,
                1.9f, D_CARRY, bit + 1, 0);
    }

    private void genFullAdders(float x, float y, float w, float h, int bitsTotal) {
        rails(x, y, w, h);
        int slices = Math.min(Math.max(1, bitsTotal), 4);
        float top = y + 24f, ah = h - 50f, rowH = ah / slices;
        float rx = x + 36f, rw = w - 76f;
        for (int r = 0; r < slices; r++) {
            int bit = slices - 1 - r; // MSB slice on top, carry ripples upward
            float ry = top + r * rowH;
            genAdderRow(rx, ry, rw, rowH - 3f, bit, (r & 1) != 0);
            if (detailedLabels) label("BIT " + bit, x + 3, ry + rowH * 0.52f, 10, 0xFFFFE2EC);
        }
        // Datapath trunks run vertically through the abutted slices.  They are
        // segmented per row so each stretch carries that row's actual bit.
        float pitch = (rw - 104f) / 17f;
        float ax = rx + 42f + pitch, bx = rx + 42f + 3f * pitch;
        float sx = rx + 42f + 11f * pitch, cxp = x + w - 20f;
        for (int r = 0; r < slices; r++) {
            int bit = slices - 1 - r;
            float ry = top + r * rowH;
            vLine(L_METAL2, ax, ry, rowH, 2.8f, D_ALU_A, bit, 0);
            vLine(L_METAL3, bx, ry, rowH, 2.8f, D_ALU_B, bit, 0);
            vLine(L_METAL1, sx, ry, rowH, 3.1f, D_ALU_S, bit, 0);
            vLine(L_METAL2, cxp, ry, rowH, 2.8f, D_CARRY, bit + 1, 0);
            via(ax, ry + rowH * 0.5f, 2.8f);
            via(sx, ry + rowH * 0.5f, 2.8f);
        }
        if (detailedLabels) {
            label("A", ax - 4, top - 8, 12, 0xFFFFFFFF);
            label("B", bx - 4, top - 8, 12, 0xFFFFFFFF);
            label("SUM", sx - 13, y + h - 3, 12, 0xFFFFFFFF);
            label("CARRY", cxp - 31, top - 8, 11, 0xFFFFD9C8);
            label("Полный сумматор: XOR + мажоритарная цепь переноса", x + 8, y + 14, 12, 0xFFFFF3C8);
        }
    }

    // ------------------------------------------------------------------ logic
    private void genLogic(float x, float y, float w, float h, boolean withPla, int plaSeed, boolean macro) {
        rails(x, y, w, h);
        float plaW = withPla ? clamp(w * 0.31f, 70f, 300f) : 0f;
        float logicW = w - plaW - (withPla ? 6f : 0f);
        float top = y + 8f, bottom = y + h - 8f;
        float rowH = macro ? clamp(h / 8f, 48f, 78f) : clamp(h / 9f, 27f, 48f);
        float channelH = macro ? 24f : 15f;

        for (float ry = top; ry + rowH <= bottom + 0.5f; ry += rowH + channelH) {
            add(L_WELL, x + 3, ry, logicW - 6, rowH * 0.44f);
            add(L_METAL1, x + 2, ry - 2.2f, logicW - 4, 2.1f);
            add(L_METAL1, x + 2, ry + rowH - 0.5f, logicW - 4, 2.1f);
            float cx = x + 5f + rndRange(0f, 6f);
            while (cx < x + logicW - 18f) {
                float cellW = rndRange(macro ? 36f : 22f, macro ? 72f : 48f);
                if (cx + cellW > x + logicW - 3) cellW = x + logicW - 3 - cx;
                int pairs = clampInt(Math.round(cellW / (macro ? 15f : 11f)), 2, 6);
                int cellLogic = nextLogicId();
                float lastGx = cx + 3f;
                for (int g = 0; g < pairs; g++) {
                    float gx = cx + cellW * (0.16f + 0.68f * (pairs == 1 ? 0.5f : g / (float) (pairs - 1)));
                    int id = cellLogic + g * 7;
                    float islandW = Math.max(9f, Math.min(22f, cellW / pairs * 0.78f));
                    add(L_PDIFF, gx - islandW / 2, ry + rowH * 0.13f, islandW, rowH * 0.22f);
                    add(L_NDIFF, gx - islandW / 2, ry + rowH * 0.60f, islandW, rowH * 0.22f);
                    add(L_POLY, gx - 1.05f, ry + rowH * 0.05f, 2.1f, rowH * 0.84f, D_LOGIC, id, 0);
                    add(L_MARK, gx - 1.7f, ry + rowH * 0.19f, 3.4f, rowH * 0.09f, D_LOGIC, id, 0);
                    add(L_MARK, gx - 1.7f, ry + rowH * 0.66f, 3.4f, rowH * 0.09f, D_LOGIC, id, 0);
                    float cs = macro ? 3.1f : 2.2f;
                    contact(gx - islandW * 0.28f, ry + rowH * 0.24f, cs);
                    contact(gx + islandW * 0.28f, ry + rowH * 0.24f, cs);
                    contact(gx - islandW * 0.28f, ry + rowH * 0.71f, cs);
                    contact(gx + islandW * 0.28f, ry + rowH * 0.71f, cs);
                    if (g > 0 && chance(72)) {
                        // Shared source/drain diffusion between neighbouring gates.
                        add(L_PDIFF, lastGx, ry + rowH * 0.16f, gx - lastGx, rowH * 0.16f);
                        add(L_NDIFF, lastGx, ry + rowH * 0.63f, gx - lastGx, rowH * 0.16f);
                    }
                    lastGx = gx;
                }
                // A cell output and one or two local nodes.
                float outX = cx + cellW * rndRange(0.35f, 0.78f);
                vLine(L_METAL2, outX, ry + rowH * 0.42f, rowH * 0.62f, 1.7f, D_LOGIC, cellLogic + 101, 0);
                hLine(L_METAL1, cx + cellW * 0.18f, ry + rowH * 0.45f, cellW * 0.58f, 1.55f, D_LOGIC, cellLogic + 57, 0);
                via(outX, ry + rowH * 0.45f, 2.5f);
                cx += cellW + rndRange(1.5f, 5f);
            }

            // Dense routing channel: broken tracks, jogs and vias, never blank.
            float cy0 = ry + rowH + 2.5f;
            if (cy0 + channelH <= bottom + channelH) {
                int tracks = Math.max(3, (int) (channelH / 4.6f));
                for (int t = 0; t < tracks; t++) {
                    float ty = cy0 + 2.2f + t * (channelH - 4f) / Math.max(1, tracks - 1);
                    float sx = x + 3f;
                    while (sx < x + logicW - 18f) {
                        float len = rndRange(28f, 105f);
                        if (sx + len > x + logicW - 3) len = x + logicW - 3 - sx;
                        int layer = t % 3 == 2 ? L_METAL3 : t % 2 == 0 ? L_METAL1 : L_METAL2;
                        int id = nextLogicId();
                        hLine(layer, sx, ty, len, 1.35f, D_LOGIC, id, 0);
                        if (chance(68)) {
                            float jx = sx + len * rndRange(0.25f, 0.8f);
                            vLine(L_METAL2, jx, Math.max(top, ty - channelH * 0.48f), channelH * 0.92f, 1.25f, D_LOGIC, id, 0);
                            via(jx, ty, 2.1f);
                        }
                        sx += len + rndRange(3f, 12f);
                    }
                }
            }
        }

        if (withPla) {
            float px = x + logicW + 6f;
            add(L_WELL, px, top, plaW, bottom - top);
            rails(px, top - 4f, plaW, bottom - top + 8f);
            float pitch = macro ? 15f : 10.5f;
            int cols = Math.max(5, (int) ((plaW - 10) / pitch));
            int rowsN = Math.max(6, (int) ((bottom - top - 10) / pitch));
            for (int c = 0; c < cols; c++) {
                vLine(L_METAL2, px + 7 + c * pitch, top + 2, bottom - top - 4, 1.55f, D_LOGIC, 9000 + c, 0);
            }
            long hs = plaSeed * 2654435761L + 97;
            for (int r = 0; r < rowsN; r++) {
                float ly = top + 7 + r * pitch;
                hLine(L_POLY, px + 3, ly, plaW - 6, 1.55f, D_LOGIC, 10000 + r, 0);
                for (int c = 0; c < cols; c++) {
                    hs ^= hs << 13; hs ^= hs >>> 7; hs ^= hs << 17;
                    if ((hs & 3L) == 0L) {
                        float ix = px + 7 + c * pitch;
                        add(L_NDIFF, ix - 2.6f, ly - pitch * 0.32f, 5.2f, pitch * 0.64f);
                        contact(ix, ly, 2.3f);
                        add(L_MARK, ix - 1.5f, ly - 1.5f, 3f, 3f, D_LOGIC, 11000 + r * 31 + c, 0);
                    }
                }
            }
            label("PLA", px + 7, top + 13, 11, 0xFFFFFFFF);
        }
    }

    private void genBus(float x, float y, float w, float h, int bits, boolean macro) {
        float driverW = clamp(w * 0.055f, 12f, 38f);
        // Bus drivers left and right: interdigitated transistor fingers.
        for (int side = 0; side < 2; side++) {
            float dx = side == 0 ? x : x + w - driverW;
            add(L_WELL, dx, y, driverW, h * 0.42f);
            for (float fx = dx + 3f; fx < dx + driverW - 3f; fx += 5.2f) {
                add(L_PDIFF, fx, y + h * 0.08f, 2.2f, h * 0.27f);
                add(L_NDIFF, fx, y + h * 0.58f, 2.2f, h * 0.30f);
                add(L_POLY, fx + 2.4f, y + h * 0.05f, 1.7f, h * 0.86f, D_LOGIC, nextLogicId(), 0);
                add(L_MARK, fx + 1.6f, y + h * 0.66f, 3f, Math.max(2f, h * 0.045f), D_LOGIC, nextLogicId(), 0);
            }
        }
        float bx = x + driverW + 2f, bw = w - driverW * 2 - 4f;
        int tracks = clampInt((int) (h / (macro ? 8f : 6.2f)), 6, 96);
        float pitch = h / tracks;
        for (int i = 0; i < tracks; i++) {
            float ty = y + i * pitch + pitch * 0.5f;
            int bit = i % Math.max(1, bits);
            int layer = i % 5 == 4 ? L_METAL3 : i % 2 == 0 ? L_METAL1 : L_METAL2;
            float th = Math.max(1.1f, pitch * 0.30f);
            hLine(layer, bx, ty, bw, th, D_BUS, bit, 0);
            for (float hx = bx + 18f + (i * 29) % 61; hx < bx + bw - 10; hx += 67f + (i % 5) * 17f) {
                vLine(L_METAL2, hx, Math.max(y + 1, ty - pitch * 0.85f), pitch * 1.7f, 1.25f, D_BUS, bit, 0);
                via(hx, ty, 2.2f);
            }
            if (detailedLabels && i < bits) {
                label("D" + bit, bx + bw + 4, ty + 3.5f, 8, 0xFFEAF6FF);
            }
        }
    }

    private void genSramCells(float x, float y, float w, float h, int cols, int rows, int source) {
        rails(x, y, w, h);
        float top = y + 9f, ah = h - 18f;
        float cw = w / cols, ch = ah / rows;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int idx = r * cols + c;
                int qDyn, invDyn, na, nb;
                if (source == 1) {
                    qDyn = D_REG_BIT; invDyn = D_REG_NBIT; na = r; nb = cols - 1 - c;
                } else {
                    qDyn = D_MEM_BIT; invDyn = D_MEM_NBIT; na = idx; nb = 0;
                }
                genSramCellMirrored(x + c * cw + 1.5f, top + r * ch + 1.5f,
                        cw - 3f, ch - 3f, qDyn, na, nb, invDyn, r == 0 && c == 0,
                        (c & 1) != 0, (r & 1) != 0);
            }
            // One real array wordline across the macro row, on top of cell WLs.
            float wlFrac = (r & 1) != 0 ? 0.17f : 0.83f;
            hLine(L_POLY, x + 2, top + r * ch + ch * wlFrac, w - 4, 2.6f,
                    source == 1 ? D_REGROW : D_WL, r, cols);
        }
        for (int c = 0; c < cols; c++) {
            vLine(L_METAL2, x + c * cw + cw * 0.07f, top, ah, 2.0f, D_BL, c, cols);
            vLine(L_METAL2, x + (c + 1) * cw - cw * 0.07f, top, ah, 2.0f, D_BL, c, cols);
        }
    }

    private void genPixelCells(float x, float y, float w, float h, int cols, int rows) {
        rails(x, y, w, h);
        float top = y + 10, ah = h - 20;
        float cw = w / cols, ch = ah / rows;
        for (int r = 0; r < rows; r++) {
            hLine(L_POLY, x + 2, top + r * ch + ch * 0.13f, w - 4, Math.max(2.2f, ch * 0.055f), D_SCAN_ROW, r, 0);
            for (int c = 0; c < cols; c++) {
                float px = x + c * cw, py = top + r * ch;
                int idx = r * cols + c;
                add(L_PIXEL, px + cw * 0.22f, py + ch * 0.25f, cw * 0.68f, ch * 0.62f, D_SCREEN, idx, 0);
                // TFT island and gate crossing in the corner.
                add(L_NDIFF, px + cw * 0.07f, py + ch * 0.05f, cw * 0.34f, ch * 0.24f);
                add(L_PDIFF, px + cw * 0.07f, py + ch * 0.05f, cw * 0.34f, ch * 0.07f);
                add(L_MARK, px + cw * 0.15f, py + ch * 0.10f, Math.max(2f, cw * 0.09f), Math.max(2f, ch * 0.07f), D_SCAN_ROW, r, 0);
                hLine(L_METAL1, px + cw * 0.18f, py + ch * 0.25f, cw * 0.34f, Math.max(1.8f, ch * 0.045f), D_SCREEN, idx, 0);
                contact(px + cw * 0.25f, py + ch * 0.18f, clamp(Math.min(cw, ch) * 0.07f, 1.8f, 4f));
                // Storage capacitor: parallel plates at bottom of pixel.
                add(L_POLY, px + cw * 0.20f, py + ch * 0.88f, cw * 0.70f, Math.max(1.8f, ch * 0.05f));
                add(L_METAL1, px + cw * 0.20f, py + ch * 0.94f, cw * 0.70f, Math.max(1.6f, ch * 0.04f));
            }
        }
        for (int c = 0; c < cols; c++) {
            vLine(L_METAL2, x + c * cw + cw * 0.08f, top, ah, Math.max(1.8f, cw * 0.045f), D_SCAN_COL, c, 0);
        }
        label("SCAN", x + 6, top - 2, 10, 0xFFFFFFFF);
        label("DATA", x + w - 42, top - 2, 10, 0xFFFFFFFF);
    }

    // -------------------------------------------------------------- whole die
    private void genFillerCell(float x, float y, float s, int gx, int gy) {
        int mode = Math.abs(hash2(gx, gy, 811)) % 3;
        // Two miniature standard-cell strips.
        for (int strip = 0; strip < 2; strip++) {
            float sy = y + 6f + strip * s * 0.47f;
            add(L_WELL, x + 2, sy, s - 4, s * 0.18f);
            float gx2 = x + 5f + ((gx + strip * 3) % 4);
            while (gx2 < x + s - 6f) {
                int id = nextLogicId();
                add(L_PDIFF, gx2 - 3.6f, sy + 1.5f, 7.2f, s * 0.14f);
                add(L_NDIFF, gx2 - 3.6f, sy + s * 0.23f, 7.2f, s * 0.14f);
                add(L_POLY, gx2 - 0.8f, sy, 1.6f, s * 0.39f, D_LOGIC, id, 0);
                add(L_MARK, gx2 - 1.4f, sy + s * 0.27f, 2.8f, 2.8f, D_LOGIC, id, 0);
                contact(gx2 - 2.4f, sy + s * 0.09f, 1.7f);
                contact(gx2 + 2.4f, sy + s * 0.30f, 1.7f);
                gx2 += 10.5f + ((Math.abs(hash2((int) gx2, gy + strip, 17)) % 5));
            }
            hLine(strip == 0 ? L_METAL1 : L_METAL2, x + 3, sy + s * 0.21f, s - 6, 1.25f, D_LOGIC, nextLogicId(), 0);
        }
        // Through-routing: orientation changes per filler tile, so the gaps
        // read as a routed channel rather than wallpaper.
        if (mode == 0) {
            for (int i = 0; i < 4; i++) hLine(i % 2 == 0 ? L_METAL2 : L_METAL3, x, y + 8 + i * (s - 16) / 3f, s, 1.15f, D_LOGIC, nextLogicId(), 0);
            vLine(L_METAL2, x + s * (0.3f + 0.13f * (gy % 3)), y + 5, s - 10, 1.2f, D_LOGIC, nextLogicId(), 0);
        } else if (mode == 1) {
            for (int i = 0; i < 4; i++) vLine(i % 2 == 0 ? L_METAL1 : L_METAL2, x + 8 + i * (s - 16) / 3f, y, s, 1.15f, D_LOGIC, nextLogicId(), 0);
            hLine(L_METAL1, x, y + s * (0.32f + 0.12f * (gx % 3)), s, 1.2f, D_LOGIC, nextLogicId(), 0);
        } else {
            manhattan(L_METAL2, x, y + s * 0.28f, x + s, y + s * 0.72f, 1.35f, D_LOGIC, nextLogicId(), 0, (gx & 1) == 0);
            manhattan(L_METAL3, x + s * 0.22f, y + s, x + s * 0.78f, y, 1.2f, D_CLK, (gx + gy) & 1, 0, false);
        }
        via(x + s * 0.5f, y + s * 0.5f, 2.0f);
    }

    private void powerMesh(float x, float y, float w, float h) {
        float step = 106f;
        for (float px = x + step; px < x + w - 8; px += step) {
            vLine(L_METAL1, px, y, h, 2.1f, D_NONE, 0, 0);
            vLine(L_METAL2, px + 5.5f, y, h, 1.55f, D_NONE, 0, 0);
        }
        for (float py = y + step; py < y + h - 8; py += step) {
            hLine(L_METAL1, x, py, w, 2.1f, D_NONE, 0, 0);
            hLine(L_METAL2, x, py + 5.5f, w, 1.55f, D_NONE, 0, 0);
        }
        for (float px = x + step; px < x + w - 8; px += step) {
            for (float py = y + step; py < y + h - 8; py += step) via(px, py, 2.8f);
        }
    }

    private void clockTree(float x, float y, float w, float h) {
        float cx = x + w * 0.5f;
        vLine(L_METAL3, cx, y, h, 3.2f, D_CLK, 0, 0);
        float[] branchY = {0.18f, 0.38f, 0.58f, 0.78f, 0.94f};
        for (int i = 0; i < branchY.length; i++) {
            float by = y + h * branchY[i];
            hLine(L_METAL3, x + w * 0.06f, by, w * 0.88f, 2.15f, D_CLK, i & 1, 0);
            // Buffer cell at the branch root.
            add(L_PDIFF, cx - 9, by - 8, 18, 5);
            add(L_NDIFF, cx - 9, by + 3, 18, 5);
            add(L_POLY, cx - 1, by - 9, 2.2f, 18, D_CLK, i & 1, 0);
            via(cx, by, 3.2f);
            for (float dx = x + w * 0.12f; dx < x + w * 0.92f; dx += w / 9f) {
                vLine(L_METAL3, dx, by - 11, 22, 1.15f, D_CLK, i & 1, 0);
                via(dx, by, 1.9f);
            }
        }
    }

    private void padCell(float x, float y, float pw, float ph, int side, int id, float totalW, float totalH, float ring) {
        float border = 4.2f;
        add(L_PAD, x, y, pw, border);
        add(L_PAD, x, y + ph - border, pw, border);
        add(L_PAD, x, y, border, ph);
        add(L_PAD, x + pw - border, y, border, ph);
        add(L_METAL3, x + pw * 0.31f, y + ph * 0.31f, pw * 0.38f, ph * 0.38f);
        via(x + pw * 0.22f, y + ph * 0.22f, 2.6f);
        via(x + pw * 0.78f, y + ph * 0.78f, 2.6f);

        float cx = x + pw / 2f, cy = y + ph / 2f;
        float ax = cx, ay = cy;
        float dx = x, dy = y, dw = pw, dh = 13f;
        if (side == 0) { ay = ring; dy = ring - 14f; }
        else if (side == 2) { ay = totalH - ring; dy = totalH - ring + 1f; }
        else if (side == 1) { ax = ring; dx = ring - 14f; dy = y; dw = 13f; dh = ph; }
        else { ax = totalW - ring; dx = totalW - ring + 1f; dy = y; dw = 13f; dh = ph; }
        padAnchors.add(new float[]{ax, ay, side});

        // I/O driver fingers just inside each pad.
        add(L_WELL, dx, dy, dw, dh);
        if (side == 0 || side == 2) {
            for (float fx = dx + 2.5f; fx < dx + dw - 3; fx += 4.4f) {
                add(L_POLY, fx, dy + 1, 1.5f, dh - 2, D_LOGIC, id * 11 + (int) fx, 0);
                add(L_NDIFF, fx + 1.8f, dy + 1, 1.6f, dh - 2);
                add(L_PDIFF, fx + 1.8f, dy + 1, 1.6f, dh * 0.42f);
            }
            hLine(L_METAL2, dx + 1, dy + dh * 0.52f, dw - 2, 1.35f, D_LOGIC, id, 0);
        } else {
            for (float fy = dy + 2.5f; fy < dy + dh - 3; fy += 4.4f) {
                add(L_POLY, dx + 1, fy, dw - 2, 1.5f, D_LOGIC, id * 11 + (int) fy, 0);
                add(L_NDIFF, dx + 1, fy + 1.8f, dw - 2, 1.6f);
                add(L_PDIFF, dx + 1, fy + 1.8f, dw * 0.42f, 1.6f);
            }
            vLine(L_METAL2, dx + dw * 0.52f, dy + 1, dh - 2, 1.35f, D_LOGIC, id, 0);
        }
    }

    private void genPadRing(float w, float h, float ring) {
        float pw = 42f, step = 71f;
        int id = 7000;
        for (float px = ring + 13f; px + pw < w - ring - 10f; px += step) {
            padCell(px, 7f, pw, pw, 0, id++, w, h, ring);
            padCell(px, h - 7f - pw, pw, pw, 2, id++, w, h, ring);
        }
        for (float py = ring + 13f; py + pw < h - ring - 10f; py += step) {
            padCell(7f, py, pw, pw, 1, id++, w, h, ring);
            padCell(w - 7f - pw, py, pw, pw, 3, id++, w, h, ring);
        }
        // Corner supply cells.
        float cs = ring - 15f;
        float[][] corners = {{7, 7}, {w - 7 - cs, 7}, {7, h - 7 - cs}, {w - 7 - cs, h - 7 - cs}};
        for (int i = 0; i < corners.length; i++) {
            float cx = corners[i][0], cy = corners[i][1];
            add(L_PAD, cx, cy, cs, 4.5f);
            add(L_PAD, cx, cy + cs - 4.5f, cs, 4.5f);
            add(L_PAD, cx, cy, 4.5f, cs);
            add(L_PAD, cx + cs - 4.5f, cy, 4.5f, cs);
            hLine(L_METAL1, cx + 6, cy + cs / 2f, cs - 12, 2.4f, D_CLK, i & 1, 0);
            vLine(L_METAL2, cx + cs / 2f, cy + 6, cs - 12, 2.4f, D_CLK, 1 - (i & 1), 0);
            via(cx + cs / 2f, cy + cs / 2f, 3.4f);
        }
    }

    private void routePadFeeds(float innerX, float innerY, float innerW, float innerH) {
        for (int i = 0; i < padAnchors.size(); i++) {
            float[] a = padAnchors.get(i);
            if (i % 3 == 2) continue; // not every pad needs a long visible feed
            float x = a[0], y = a[1];
            int side = (int) a[2];
            float len = 24f + (i % 5) * 13f;
            int layer = i % 2 == 0 ? L_METAL2 : L_METAL3;
            int id = 12000 + i;
            if (side == 0) {
                vLine(layer, x, y, len, 1.45f, D_LOGIC, id, 0);
                hLine(layer, Math.min(x, x + ((i % 2 == 0) ? 24 : -24)), y + len, 24, 1.45f, D_LOGIC, id, 0);
                via(x, y + len, 2.3f);
            } else if (side == 2) {
                vLine(layer, x, y - len, len, 1.45f, D_LOGIC, id, 0);
                hLine(layer, Math.min(x, x + ((i % 2 == 0) ? -24 : 24)), y - len, 24, 1.45f, D_LOGIC, id, 0);
                via(x, y - len, 2.3f);
            } else if (side == 1) {
                hLine(layer, x, y, len, 1.45f, D_LOGIC, id, 0);
                vLine(layer, x + len, Math.min(y, y + ((i % 2 == 0) ? 24 : -24)), 24, 1.45f, D_LOGIC, id, 0);
                via(x + len, y, 2.3f);
            } else {
                hLine(layer, x - len, y, len, 1.45f, D_LOGIC, id, 0);
                vLine(layer, x - len, Math.min(y, y + ((i % 2 == 0) ? -24 : 24)), 24, 1.45f, D_LOGIC, id, 0);
                via(x - len, y, 2.3f);
            }
        }
    }

    private float[] edgePoint(Area from, Area to) {
        float tx = to.x + to.w / 2f, ty = to.y + to.h / 2f;
        float x = clamp(tx, from.x + 3, from.x + from.w - 3);
        float y = clamp(ty, from.y + 3, from.y + from.h - 3);
        // Move the point to the nearest edge of "from".
        float dl = Math.abs(tx - from.x), dr = Math.abs(tx - (from.x + from.w));
        float dt = Math.abs(ty - from.y), db = Math.abs(ty - (from.y + from.h));
        float m = Math.min(Math.min(dl, dr), Math.min(dt, db));
        if (m == dl) x = from.x; else if (m == dr) x = from.x + from.w;
        else if (m == dt) y = from.y; else y = from.y + from.h;
        return new float[]{x, y};
    }

    private void routeBundle(Area from, Area to, int bits, boolean dataBus) {
        float[] p1 = edgePoint(from, to);
        float[] p2 = edgePoint(to, from);
        int lines = clampInt(bits, 2, 8);
        boolean horizFirst = Math.abs(p2[0] - p1[0]) >= Math.abs(p2[1] - p1[1]);
        for (int i = 0; i < lines; i++) {
            float off = (i - (lines - 1) / 2f) * 3.4f;
            float x1 = p1[0] + (horizFirst ? 0 : off);
            float y1 = p1[1] + (horizFirst ? off : 0);
            float x2 = p2[0] + (horizFirst ? 0 : off);
            float y2 = p2[1] + (horizFirst ? off : 0);
            int dyn = dataBus ? D_BUS : D_LOGIC;
            int arg = dataBus ? i % Math.max(1, bits) : nextLogicId();
            int layer = i % 3 == 2 ? L_METAL3 : i % 2 == 0 ? L_METAL2 : L_METAL1;
            manhattan(layer, x1, y1, x2, y2, 1.35f, dyn, arg, 0, horizFirst);
        }
        via(p1[0], p1[1], 3f);
        via(p2[0], p2[1], 3f);
    }

    private void genBlockInto(ChipBin chip, ChipBin.Block b, float x, float y, float w, float h, boolean macro) {
        switch (b.kind) {
            case ChipBin.KIND_RAM:
                genMemoryArray(x, y, w, h, chip);
                break;
            case ChipBin.KIND_REGS:
                genRegisterArray(x, y, w, h, chip);
                break;
            case ChipBin.KIND_SCREEN:
                genScreenArray(x, y, w, h, chip);
                break;
            case ChipBin.KIND_ALU:
                genAlu(x, y, w, h, Math.max(1, chip.bits));
                break;
            case ChipBin.KIND_BUS:
                genBus(x, y, w, h, Math.max(1, chip.bits), macro);
                break;
            case ChipBin.KIND_CONTROL:
                genLogic(x, y, w, h, true, chip.name.hashCode() ^ programHash(chip), macro);
                break;
            default:
                genLogic(x, y, w, h, false, 0, macro);
                break;
        }
        guardRing(x - 1.2f, y - 1.2f, w + 2.4f, h + 2.4f);
    }

    private static int programHash(ChipBin chip) {
        int hv = 0x1234567;
        for (int i = 0; i < chip.program.length; i++) hv = hv * 31 + chip.program[i];
        return hv;
    }

    private static long seedFor(ChipBin chip, String extra) {
        long h = 1469598103934665603L;
        String s = chip.name + "|" + extra + "|" + chip.type + "|" + chip.bits + "|" + chip.regs + "|" + chip.memSize;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 1099511628211L;
        }
        return h ^ programHash(chip) * 0x9E3779B97F4A7C15L;
    }

    // ------------------------------------------------------------ public API
    public static DieLayout wholeDie(ChipBin chip) {
        float ring = 64f, grid = 53f;
        float inner = grid * 16f;
        float total = inner + ring * 2f;
        DieLayout d = new DieLayout(seedFor(chip, "whole"), total, total);
        d.innerX = ring; d.innerY = ring; d.innerW = inner; d.innerH = inner;

        ChipBin.Block[] blocks = chip.blocksOrDefault();
        boolean[] occupied = new boolean[256];
        for (int i = 0; i < blocks.length; i++) {
            ChipBin.Block b = blocks[i];
            int bx = clampInt(b.x, 0, 15), by = clampInt(b.y, 0, 15);
            int bw = clampInt(b.w, 1, 16 - bx), bh = clampInt(b.h, 1, 16 - by);
            Area a = new Area(ring + bx * grid, ring + by * grid, bw * grid, bh * grid, i);
            d.blockAreas.add(a);
            for (int yy = by; yy < by + bh; yy++) {
                for (int xx = bx; xx < bx + bw; xx++) occupied[yy * 16 + xx] = true;
            }
        }

        // Subtle well tiles first (never a solid neon wash).
        for (int gy = 0; gy < 16; gy++) {
            for (int gx = 0; gx < 16; gx++) {
                if (((gx + gy) & 1) == 0) {
                    d.add(L_WELL, ring + gx * grid, ring + gy * grid, grid, grid);
                }
            }
        }
        // Every unused floorplan cell becomes filler or routing.
        for (int gy = 0; gy < 16; gy++) {
            for (int gx = 0; gx < 16; gx++) {
                if (!occupied[gy * 16 + gx]) {
                    d.genFillerCell(ring + gx * grid, ring + gy * grid, grid, gx, gy);
                }
            }
        }
        d.powerMesh(ring, ring, inner, inner);

        for (int i = 0; i < blocks.length; i++) {
            Area a = d.blockAreas.get(i);
            d.genBlockInto(chip, blocks[i], a.x + 2.5f, a.y + 2.5f, a.w - 5f, a.h - 5f, false);
        }

        // Global interconnect: controller to each block, and the datapath chain.
        int control = -1, alu = -1, regs = -1, ram = -1, bus = -1;
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i].kind == ChipBin.KIND_CONTROL && control < 0) control = i;
            if (blocks[i].kind == ChipBin.KIND_ALU && alu < 0) alu = i;
            if (blocks[i].kind == ChipBin.KIND_REGS && regs < 0) regs = i;
            if ((blocks[i].kind == ChipBin.KIND_RAM || blocks[i].kind == ChipBin.KIND_SCREEN) && ram < 0) ram = i;
            if (blocks[i].kind == ChipBin.KIND_BUS && bus < 0) bus = i;
        }
        if (control >= 0) {
            for (int i = 0; i < blocks.length; i++) {
                if (i != control) d.routeBundle(d.blockAreas.get(control), d.blockAreas.get(i), chip.bits,
                        blocks[i].kind == ChipBin.KIND_BUS || blocks[i].kind == ChipBin.KIND_RAM);
            }
        }
        if (regs >= 0 && alu >= 0) d.routeBundle(d.blockAreas.get(regs), d.blockAreas.get(alu), chip.bits, true);
        if (alu >= 0 && ram >= 0) d.routeBundle(d.blockAreas.get(alu), d.blockAreas.get(ram), chip.bits, true);
        if (ram >= 0 && bus >= 0) d.routeBundle(d.blockAreas.get(ram), d.blockAreas.get(bus), chip.bits, true);
        if (regs >= 0 && bus >= 0) d.routeBundle(d.blockAreas.get(regs), d.blockAreas.get(bus), chip.bits, true);

        d.clockTree(ring, ring, inner, inner);
        d.guardRing(ring - 9f, ring - 9f, inner + 18f, inner + 18f);
        d.genPadRing(total, total, ring);
        d.routePadFeeds(ring, ring, inner, inner);
        d.finish();
        return d;
    }

    /** One block enlarged.  big=true uses macro transistor cells. */
    public static DieLayout blockDetail(ChipBin chip, ChipBin.Block b, boolean big) {
        float W = big ? 960f : 840f;
        float H = big ? 720f : 600f;
        DieLayout d = new DieLayout(seedFor(chip, "block:" + b.name + ":" + b.kind + ":" + big), W, H);
        d.detailedLabels = true;
        d.blockAreas.add(new Area(0, 0, W, H, 0));
        if (!big) {
            d.genBlockInto(chip, b, 5f, 5f, W - 10f, H - 10f, false);
            d.finish();
            return d;
        }
        switch (b.kind) {
            case ChipBin.KIND_RAM: {
                int cols = Math.min(6, Math.max(1, chip.memSize));
                int rows = Math.min(4, Math.max(1, (chip.memSize + cols - 1) / cols));
                d.genSramCells(5f, 5f, W - 10f, H - 10f, cols, rows, 0);
                break;
            }
            case ChipBin.KIND_REGS: {
                int cols = Math.min(8, Math.max(1, chip.bits));
                int rows = Math.min(4, Math.max(1, chip.regs));
                d.genSramCells(5f, 5f, W - 10f, H - 10f, cols, rows, 1);
                break;
            }
            case ChipBin.KIND_ALU:
                d.genFullAdders(5f, 5f, W - 10f, H - 10f, Math.max(1, chip.bits));
                break;
            case ChipBin.KIND_SCREEN: {
                int cols = Math.min(chip.hasScreen() ? chip.screenW : 8, 8);
                int rows = Math.min(chip.hasScreen() ? chip.screenH : 6, 5);
                d.genPixelCells(5f, 5f, W - 10f, H - 10f, Math.max(1, cols), Math.max(1, rows));
                break;
            }
            case ChipBin.KIND_BUS:
                d.genBus(5f, 5f, W - 10f, H - 10f, Math.max(1, chip.bits), true);
                break;
            case ChipBin.KIND_CONTROL:
                d.genLogic(5f, 5f, W - 10f, H - 10f, true, chip.name.hashCode() ^ programHash(chip), true);
                break;
            default:
                d.genLogic(5f, 5f, W - 10f, H - 10f, false, 0, true);
                break;
        }
        d.finish();
        return d;
    }
}
