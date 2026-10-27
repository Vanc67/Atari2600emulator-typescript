package rpc;

/**
 * The player character.
 *
 * 4.8 had no character at all: the room was a static picture and the
 * "cursor" jumped from rectangle to rectangle.  Here the player really walks
 * around the room: arrow keys move the avatar, the walk cycle animates, the
 * avatar turns to face where it walks, and pressing a number sends it walking
 * to that object, which is then used when the avatar arrives.
 *
 * Everything is procedural pixel art, so there are no image files to load.
 */
public final class Avatar {

    /** feet position, scene coordinates */
    public int x;
    public int y;
    /** 1 = facing right, -1 = facing left */
    public int face = 1;
    /** walk cycle phase, only advances while moving */
    public int walk;
    /** 0..8 breathing/blink counter */
    private int idle;
    /** walk target, -1 when standing still */
    private int tx = -1;
    private int ty = -1;
    /** hotspot that should fire when the walk finishes, -1 = none */
    public int pending = -1;
    /** true while the avatar is moving */
    public boolean moving;
    /** speed in pixels per frame */
    private int speed = 7;

    public Avatar() {
        this.x = 40;
        this.y = 0;
    }

    public boolean hasTarget() {
        return this.tx >= 0;
    }

    /** Places the avatar at a floor position without animating. */
    public void place(int px, int py) {
        this.x = px;
        this.y = py;
        this.tx = -1;
        this.ty = -1;
        this.pending = -1;
        this.moving = false;
    }

    /** Starts walking to a point; keeps the pending action. */
    public void walkTo(int px, int py, int hot) {
        this.tx = px;
        this.ty = py;
        this.pending = hot;
        this.moving = true;
    }

    /** Direct movement from the d-pad. */
    public void step(int dx, int dy, int w, int viewH) {
        this.tx = -1;
        this.ty = -1;
        this.pending = -1;
        if (dx != 0) {
            this.face = dx > 0 ? 1 : -1;
        }
        this.x += dx;
        this.y += dy;
        this.clamp(w, viewH);
        this.walk++;
        this.moving = true;
    }

    /**
     * Advances the autonomous walk.  Called once per repaint, so the character
     * moves even while the player holds no key (a walk-to command).
     *
     * @return the hotspot that became reachable this frame, or -1
     */
    public int tick(int w, int viewH) {
        this.idle = (this.idle + 1) % 24;
        if (this.tx < 0) {
            this.moving = false;
            return -1;
        }
        int dx = this.tx - this.x;
        int dy = this.ty - this.y;
        int dist = dx < 0 ? -dx : dx;
        int distY = dy < 0 ? -dy : dy;
        if (dist <= this.speed && distY <= this.speed) {
            this.x = this.tx;
            this.y = this.ty;
            this.tx = -1;
            this.ty = -1;
            this.moving = false;
            if (this.pending >= 0) {
                int hot = this.pending;
                this.pending = -1;
                return hot;
            }
            return -1;
        }
        if (dist > 0) {
            this.x += dx > 0 ? this.speed : -this.speed;
            this.face = dx > 0 ? 1 : -1;
        }
        if (distY > 0) {
            this.y += dy > 0 ? this.speed / 2 : -this.speed / 2;
        }
        this.walk++;
        this.moving = true;
        this.clamp(w, viewH);
        return -1;
    }

    private void clamp(int w, int viewH) {
        if (w < 20 || viewH < 20) {
            return;
        }
        int minY = viewH * 62 / 100;
        int maxY = viewH - 4;
        if (this.x < 10) {
            this.x = 10;
        }
        if (this.x > w - 10) {
            this.x = w - 10;
        }
        if (this.y < minY) {
            this.y = minY;
        }
        if (this.y > maxY) {
            this.y = maxY;
        }
    }

    /** Distance from the avatar to a point, horizontal only. */
    public int near(int px) {
        int d = px - this.x;
        return d < 0 ? -d : d;
    }

    /* ------------------------------------------------------------------
     *  drawing
     * ------------------------------------------------------------------ */

    /**
     * Draws the character.  {@code oy} is the y offset of the scene inside the
     * screen, {@code night} darkens the palette.
     */
    public static void draw(Gfx g, Avatar a, int oy, int night, int frame) {
        int u = 2;
        int px = a.x;
        int py = oy + a.y;
        boolean walking = a.moving && a.walk % 8 != 0;
        int phase = (a.walk / 4) % 2;
        boolean blink = a.idle > 20;

        int skin = night == 1 ? 0xC08A62 : 0xF0B98C;
        int skinDark = night == 1 ? 0x8E6547 : 0xC98E66;
        int hair = night == 1 ? 0x2A1B10 : 0x4A2C17;
        int jacket = night == 1 ? 0x1E5A4A : 0x2F8C6B;
        int jacketHi = night == 1 ? 0x2A7A64 : 0x45BE93;
        int jeans = night == 1 ? 0x232C4A : 0x39507F;
        int jeansHi = night == 1 ? 0x2E3A5E : 0x4A66A0;
        int shoe = night == 1 ? 0x6A6257 : 0xD8DEE8;

        // ---- shadow -------------------------------------------------
        g.setColor(night == 1 ? 0x10141C : 0x1B2028);
        g.fillRect(px - 5 * u, py - u, 10 * u, u);
        g.fillRect(px - 4 * u, py, 8 * u, 1);

        // ---- legs (walk cycle swaps the stride) ----------------------
        int l1 = (walking && phase == 1) ? u : 0;
        int l2 = (walking && phase == 1) ? 0 : u;
        g.setColor(jeans);
        g.fillRect(px - 3 * u, py - 6 * u - l1, 2 * u, 5 * u + l1);
        g.fillRect(px + u, py - 6 * u - l2, 2 * u, 5 * u + l2);
        g.setColor(jeansHi);
        g.fillRect(px - 3 * u, py - 6 * u - l1, u, 5 * u + l1);
        g.fillRect(px + u, py - 6 * u - l2, u, 5 * u + l2);
        // shoes
        g.setColor(shoe);
        g.fillRect(px - 4 * u, py - u - l1, 3 * u, u);
        g.fillRect(px + u, py - u - l2, 3 * u, u);

        // ---- torso ---------------------------------------------------
        int bob = walking ? (phase == 1 ? 0 : 1) : (a.idle / 12 == 0 ? 0 : 1);
        int top = py - 12 * u + bob;
        g.setColor(jacket);
        g.fillRect(px - 3 * u, top, 6 * u, 6 * u);
        g.setColor(jacketHi);
        g.fillRect(px - 3 * u, top, u, 6 * u);
        // hood
        g.setColor(jacketHi);
        g.fillRect(px - u, top - 1, 2 * u, u);
        // zipper
        g.setColor(night == 1 ? 0x123A30 : 0x1F6E54);
        g.fillRect(px, top + u, u, 5 * u);

        // ---- arms ----------------------------------------------------
        int swing = walking ? (phase == 1 ? u : -u) : 0;
        g.setColor(jacket);
        g.fillRect(px - 4 * u, top + u + swing, u, 4 * u);
        g.fillRect(px + 3 * u, top + u - swing, u, 4 * u);
        g.setColor(skin);
        g.fillRect(px - 4 * u, top + 5 * u + swing, u, u);
        g.fillRect(px + 3 * u, top + 5 * u - swing, u, u);

        // ---- head ----------------------------------------------------
        int hy = top - 5 * u;
        boolean lookLeft = a.face < 0;
        g.setColor(skin);
        g.fillRect(px - 2 * u, hy, 4 * u, 5 * u);
        g.setColor(skinDark);
        g.fillRect(px - 2 * u, hy + 3 * u, 4 * u, u);
        // hair
        g.setColor(hair);
        g.fillRect(px - 3 * u, hy - u, 6 * u, 2 * u);
        g.fillRect(px - 3 * u, hy + u, u, 2 * u);
        if (lookLeft) {
            g.fillRect(px + 2 * u, hy, u, 2 * u);
        } else {
            g.fillRect(px - 3 * u, hy, u, 2 * u);
            g.fillRect(px + 2 * u, hy, u, 2 * u);
        }
        // eyes
        g.setColor(0x1A1219);
        int ex = lookLeft ? px - 2 * u : px + u;
        if (blink) {
            g.fillRect(px - 2 * u, hy + 2 * u, u, 1);
            g.fillRect(px + u, hy + 2 * u, u, 1);
        } else {
            g.fillRect(ex, hy + 2 * u, u, u);
            g.fillRect(lookLeft ? px + u : px - 2 * u, hy + 2 * u, u, u);
        }
        // mouth
        g.setColor(skinDark);
        g.fillRect(px - u / 2, hy + 4 * u - 1, u, 1);
    }

    /** Small pointer triangle above the object being walked to. */
    public static void drawTargetMarker(Gfx g, int x, int y, int frame) {
        g.setColor(Theme.ACCENT);
        int bob = (frame / 6) % 2;
        g.fillTriangle(x - 3, y + bob, x + 3, y + bob, x, y + 3 + bob);
    }
}
