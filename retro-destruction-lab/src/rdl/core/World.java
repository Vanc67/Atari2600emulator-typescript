package rdl.core;

/**
 * Platform-independent, fixed-allocation destruction simulation.
 *
 * All positions used by particles are fixed point: 1 pixel = 16 units.
 * No MIDP or Java SE classes are referenced here.
 */
public final class World {
    public static final int WIDTH = 32;
    public static final int HEIGHT = 32;
    public static final int SIZE = WIDTH * HEIGHT;
    public static final int TILE = 4;
    public static final int FP_SHIFT = 4;
    public static final int FP_ONE = 1 << FP_SHIFT;

    public static final byte EMPTY = 0;
    public static final byte STEEL = 1;
    public static final byte CONCRETE = 2;
    public static final byte WOOD = 3;
    public static final byte GLASS = 4;
    public static final byte TNT = 5;
    public static final byte FUEL = 6;
    public static final byte WATER = 7;
    public static final byte ANCHOR = 8;

    public static final int SCENE_TOWER = 0;
    public static final int SCENE_BRIDGE = 1;
    public static final int SCENE_EMPTY = 2;
    public static final int SCENE_COUNT = 3;

    public static final int MAX_PARTICLES = 160;

    public static final int[] MAX_HP = {
        0, 170, 130, 75, 35, 45, 35, 0, 255
    };
    private static final int[] WEIGHT = {
        0, 4, 5, 2, 2, 3, 3, 0, 0
    };
    private static final int[] STRENGTH = {
        0, 260, 220, 100, 55, 65, 45, 0, 32767
    };

    /* Public read-only-by-convention buffers: the renderer reads them directly. */
    public final byte[] cell = new byte[SIZE];
    public final byte[] hp = new byte[SIZE];
    public final byte[] heat = new byte[SIZE];
    public final byte[] supported = new byte[SIZE];

    private final byte[] pendingBlast = new byte[SIZE];
    private final byte[] waterMoved = new byte[SIZE];
    private final short[] supportQueue = new short[SIZE];
    private final short[] load = new short[SIZE];

    public final short[] particleX = new short[MAX_PARTICLES];
    public final short[] particleY = new short[MAX_PARTICLES];
    public final short[] particleVX = new short[MAX_PARTICLES];
    public final short[] particleVY = new short[MAX_PARTICLES];
    public final byte[] particleMaterial = new byte[MAX_PARTICLES];
    public final byte[] particleKind = new byte[MAX_PARTICLES];
    public final byte[] particleLife = new byte[MAX_PARTICLES];

    public boolean running = true;
    public int scene;
    public int tick;
    public int blockCount;
    public int explosionCount;
    public int collapseCount;
    public int shake;

    private boolean structureDirty;
    private int particleLimit = 144;
    private int particleCursor;
    private int randomState = 0x13572468;

    public World() {
        reset(SCENE_TOWER);
    }

    public final void reset(int sceneIndex) {
        scene = sceneIndex % SCENE_COUNT;
        if (scene < 0) {
            scene += SCENE_COUNT;
        }
        int i;
        for (i = 0; i < SIZE; i++) {
            cell[i] = EMPTY;
            hp[i] = 0;
            heat[i] = 0;
            supported[i] = 0;
            pendingBlast[i] = 0;
            waterMoved[i] = 0;
            supportQueue[i] = 0;
            load[i] = 0;
        }
        for (i = 0; i < MAX_PARTICLES; i++) {
            particleX[i] = 0;
            particleY[i] = 0;
            particleVX[i] = 0;
            particleVY[i] = 0;
            particleMaterial[i] = 0;
            particleKind[i] = 0;
            particleLife[i] = 0;
        }
        tick = 0;
        blockCount = 0;
        explosionCount = 0;
        collapseCount = 0;
        shake = 0;
        particleCursor = 0;
        randomState = 0x13572468 + scene * 977;
        running = true;

        for (i = 0; i < WIDTH; i++) {
            setCell(i, HEIGHT - 1, ANCHOR);
        }

        if (scene == SCENE_TOWER) {
            buildTower();
        } else if (scene == SCENE_BRIDGE) {
            buildBridge();
        }

        recountBlocks();
        structureDirty = false;
        rebuildSupport();
    }

    private void buildTower() {
        int x;
        int y;
        for (x = 3; x <= 28; x++) {
            setCell(x, 30, CONCRETE);
        }
        for (y = 8; y <= 29; y++) {
            setCell(5, y, STEEL);
            setCell(6, y, STEEL);
            setCell(25, y, STEEL);
            setCell(26, y, STEEL);
        }
        for (y = 23; y <= 29; y++) {
            setCell(15, y, CONCRETE);
            setCell(16, y, CONCRETE);
        }
        for (x = 5; x <= 26; x++) {
            setCell(x, 8, CONCRETE);
            setCell(x, 15, CONCRETE);
            setCell(x, 22, CONCRETE);
            setCell(x, 29, STEEL);
        }
        for (y = 4; y <= 7; y++) {
            setCell(15, y, STEEL);
        }
        setCell(16, 7, STEEL);
        for (y = 9; y <= 14; y++) {
            setCell(7, y, WOOD);
            setCell(24, y, WOOD);
        }
        for (y = 16; y <= 21; y++) {
            setCell(7, y, GLASS);
            setCell(24, y, GLASS);
        }
        for (y = 23; y <= 28; y++) {
            setCell(7, y, WOOD);
            setCell(24, y, GLASS);
        }
        for (x = 8; x <= 12; x++) {
            setCell(x, 14, GLASS);
        }
        setCell(8, 13, GLASS);
        setCell(12, 13, GLASS);
        setCell(9, 13, WATER);
        setCell(10, 13, WATER);
        setCell(11, 13, WATER);
        setCell(13, 28, TNT);
        setCell(14, 28, TNT);
        setCell(17, 28, TNT);
        setCell(18, 28, TNT);
        setCell(20, 28, FUEL);
        setCell(21, 28, FUEL);
        setCell(20, 27, FUEL);
        setCell(21, 27, FUEL);
        for (x = 9; x <= 13; x++) {
            setCell(x, 21, WOOD);
        }
        for (x = 18; x <= 22; x++) {
            setCell(x, 21, WOOD);
        }
    }

    private void buildBridge() {
        int x;
        int y;
        for (y = 25; y <= 30; y++) {
            for (x = 2; x <= 5; x++) {
                setCell(x, y, CONCRETE);
            }
            for (x = 26; x <= 29; x++) {
                setCell(x, y, CONCRETE);
            }
        }
        for (x = 5; x <= 26; x++) {
            setCell(x, 24, STEEL);
            if (x > 6 && x < 25) {
                setCell(x, 23, WOOD);
            }
        }
        for (y = 18; y <= 22; y++) {
            setCell(5, y, STEEL);
            setCell(26, y, STEEL);
        }
        for (x = 5; x <= 26; x++) {
            if ((x & 3) == 1) {
                setCell(x, 22, STEEL);
            }
        }
        setCell(13, 22, FUEL);
        setCell(14, 22, FUEL);
        setCell(17, 22, FUEL);
        setCell(18, 22, FUEL);
        setCell(8, 22, TNT);
        setCell(23, 22, TNT);
        setCell(12, 17, WATER);
        setCell(13, 17, WATER);
        setCell(14, 17, WATER);
        setCell(15, 17, WATER);
        setCell(16, 17, WATER);
        setCell(17, 17, WATER);
        setCell(18, 17, WATER);
        setCell(19, 17, WATER);
    }

    public final void setCell(int x, int y, byte material) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return;
        }
        int i = y * WIDTH + x;
        byte old = cell[i];
        if (old == material) {
            return;
        }
        if (isBlock(old)) {
            blockCount--;
        }
        cell[i] = material;
        hp[i] = (byte) MAX_HP[material];
        heat[i] = 0;
        supported[i] = 0;
        pendingBlast[i] = 0;
        if (isBlock(material)) {
            blockCount++;
        }
        if (isStructural(old) || isStructural(material)) {
            structureDirty = true;
        }
    }

    public final boolean place(int x, int y, byte material) {
        if (!inside(x, y) || material <= EMPTY || material >= ANCHOR) {
            return false;
        }
        int i = y * WIDTH + x;
        if (cell[i] == ANCHOR) {
            return false;
        }
        if (cell[i] != EMPTY && cell[i] != WATER || pendingBlast[i] != 0) {
            return false;
        }
        setCell(x, y, material);
        return true;
    }

    public final boolean erase(int x, int y) {
        if (!inside(x, y)) {
            return false;
        }
        int i = y * WIDTH + x;
        if (cell[i] == EMPTY || cell[i] == ANCHOR) {
            return false;
        }
        removeCell(i, false);
        return true;
    }

    public final void ignite(int x, int y) {
        if (!inside(x, y)) {
            return;
        }
        int dx;
        int dy;
        for (dy = -1; dy <= 1; dy++) {
            for (dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                if (inside(nx, ny)) {
                    int i = ny * WIDTH + nx;
                    if (isFlammable(cell[i])) {
                        addHeat(i, dx == 0 && dy == 0 ? 150 : 80);
                    }
                }
            }
        }
        spawnBurst(y * WIDTH + x, WOOD, 1, 10, 30);
    }

    public final void impact(int x, int y) {
        if (inside(x, y)) {
            explode(y * WIDTH + x, 5, 180);
        }
    }

    public final void damage(int index, int amount) {
        if (index < 0 || index >= SIZE || amount <= 0) {
            return;
        }
        byte material = cell[index];
        if (!isBlock(material) || material == ANCHOR) {
            return;
        }
        int remaining = (hp[index] & 255) - amount;
        if (remaining > 0) {
            hp[index] = (byte) remaining;
            return;
        }
        if ((material == TNT || material == FUEL) && pendingBlast[index] == 0) {
            pendingBlast[index] = material;
        }
        removeCell(index, true);
    }

    private void removeCell(int index, boolean debris) {
        byte old = cell[index];
        if (old == EMPTY || old == ANCHOR) {
            return;
        }
        if (isBlock(old)) {
            blockCount--;
        }
        cell[index] = EMPTY;
        hp[index] = 0;
        heat[index] = 0;
        supported[index] = 0;
        load[index] = 0;
        if (isStructural(old)) {
            structureDirty = true;
        }
        if (debris) {
            spawnBurst(index, old, 0, old == GLASS ? 2 : 1, 20);
        }
    }

    public final void step() {
        if (!running) {
            return;
        }
        tick++;
        processBlasts(6);
        if (structureDirty) {
            updateStructure(8);
        }
        if ((tick & 1) == 0) {
            updateHeat();
        }
        updateWater();
        updateParticles();
        if (shake > 0) {
            shake--;
        }
    }

    private void processBlasts(int budget) {
        int processed = 0;
        int i;
        for (i = 0; i < SIZE && processed < budget; i++) {
            byte material = pendingBlast[i];
            if (material != 0) {
                pendingBlast[i] = 0;
                if (material == TNT) {
                    explode(i, 6, 210);
                } else {
                    explode(i, 4, 135);
                    heatAround(i, 120);
                }
                processed++;
            }
        }
    }

    private void explode(int index, int radius, int power) {
        int cx = index % WIDTH;
        int cy = index / WIDTH;
        int r2 = radius * radius;
        int x;
        int y;
        explosionCount++;
        shake = 4;
        for (y = cy - radius; y <= cy + radius; y++) {
            if (y < 0 || y >= HEIGHT) {
                continue;
            }
            for (x = cx - radius; x <= cx + radius; x++) {
                if (x < 0 || x >= WIDTH) {
                    continue;
                }
                int dx = x - cx;
                int dy = y - cy;
                int d2 = dx * dx + dy * dy;
                if (d2 > r2) {
                    continue;
                }
                int i = y * WIDTH + x;
                int force = power - (power * d2) / r2;
                if (isFlammable(cell[i])) {
                    addHeat(i, force >> 1);
                }
                if (force > 0) {
                    damage(i, force);
                }
            }
        }
        spawnBurst(index, TNT, 1, 18, 70);
        spawnBurst(index, CONCRETE, 2, 5, 12);
    }

    private void heatAround(int index, int amount) {
        int cx = index % WIDTH;
        int cy = index / WIDTH;
        int x;
        int y;
        for (y = cy - 2; y <= cy + 2; y++) {
            for (x = cx - 2; x <= cx + 2; x++) {
                if (inside(x, y) && isFlammable(cell[y * WIDTH + x])) {
                    addHeat(y * WIDTH + x, amount);
                }
            }
        }
    }

    private void updateStructure(int detachBudget) {
        structureDirty = false;
        rebuildSupport();

        int i;
        int x;
        int y;
        for (i = 0; i < SIZE; i++) {
            load[i] = 0;
        }

        /* Vertical dead-load transfer. Overloaded members weaken over several ticks. */
        for (y = 0; y < HEIGHT - 1; y++) {
            for (x = 0; x < WIDTH; x++) {
                i = y * WIDTH + x;
                byte material = cell[i];
                if (!isBlock(material) || material == ANCHOR || supported[i] == 0) {
                    continue;
                }
                int total = (load[i] & 65535) + WEIGHT[material];
                if (total > 65535) {
                    total = 65535;
                }
                if (total > STRENGTH[material]) {
                    damage(i, 1 + ((total - STRENGTH[material]) >> 4));
                    structureDirty = true;
                    if (cell[i] == EMPTY) {
                        continue;
                    }
                }
                int below = i + WIDTH;
                if (isBlock(cell[below]) && supported[below] != 0) {
                    int sum = (load[below] & 65535) + total;
                    load[below] = (short) (sum > 65535 ? 65535 : sum);
                }
            }
        }

        int detached = 0;
        for (i = SIZE - WIDTH - 1; i >= 0 && detached < detachBudget; i--) {
            if (isBlock(cell[i]) && cell[i] != ANCHOR && supported[i] == 0) {
                removeCell(i, true);
                collapseCount++;
                detached++;
            }
        }
        if (detached == detachBudget) {
            for (i = 0; i < SIZE - WIDTH; i++) {
                if (isBlock(cell[i]) && cell[i] != ANCHOR && supported[i] == 0) {
                    structureDirty = true;
                    break;
                }
            }
        }
    }

    private void rebuildSupport() {
        int i;
        for (i = 0; i < SIZE; i++) {
            supported[i] = 0;
        }
        int head = 0;
        int tail = 0;
        for (i = 0; i < SIZE; i++) {
            if (cell[i] == ANCHOR) {
                supported[i] = 1;
                supportQueue[tail++] = (short) i;
            }
        }
        while (head < tail) {
            i = supportQueue[head++] & 65535;
            int x = i % WIDTH;
            int y = i / WIDTH;
            if (x > 0) {
                tail = markSupport(i - 1, tail);
            }
            if (x < WIDTH - 1) {
                tail = markSupport(i + 1, tail);
            }
            if (y > 0) {
                tail = markSupport(i - WIDTH, tail);
            }
            if (y < HEIGHT - 1) {
                tail = markSupport(i + WIDTH, tail);
            }
        }
    }

    private int markSupport(int index, int tail) {
        if (supported[index] == 0 && isStructural(cell[index])) {
            supported[index] = 1;
            supportQueue[tail++] = (short) index;
        }
        return tail;
    }

    private void updateHeat() {
        int i;
        for (i = 0; i < SIZE; i++) {
            int h = heat[i] & 255;
            if (h == 0) {
                continue;
            }
            byte material = cell[i];
            if (material == EMPTY || material == WATER) {
                h -= 18;
                heat[i] = (byte) (h > 0 ? h : 0);
                continue;
            }
            if (isFlammable(material) && h > 75) {
                if ((tick & 3) == 0) {
                    damage(i, material == WOOD ? 3 : 2);
                }
                if (cell[i] != EMPTY) {
                    int direction = random() & 3;
                    int next = i;
                    if (direction == 0 && i % WIDTH > 0) {
                        next = i - 1;
                    } else if (direction == 1 && i % WIDTH < WIDTH - 1) {
                        next = i + 1;
                    } else if (direction == 2 && i >= WIDTH) {
                        next = i - WIDTH;
                    } else if (i < SIZE - WIDTH) {
                        next = i + WIDTH;
                    }
                    if (isFlammable(cell[next])) {
                        addHeat(next, 24);
                    }
                }
                if (material == TNT && h > 175 && cell[i] == TNT) {
                    pendingBlast[i] = TNT;
                    removeCell(i, true);
                } else if (h > 105 && (random() & 7) == 0) {
                    spawnBurst(i, WOOD, 1, 1, 18);
                }
            }
            h -= material == WOOD ? 1 : 3;
            heat[i] = (byte) (h > 0 ? h : 0);
        }
    }

    private void addHeat(int index, int amount) {
        int value = (heat[index] & 255) + amount;
        heat[index] = (byte) (value > 255 ? 255 : value);
    }

    private void updateWater() {
        int i;
        for (i = 0; i < SIZE; i++) {
            waterMoved[i] = 0;
        }
        int x;
        int y;
        for (y = HEIGHT - 2; y >= 0; y--) {
            for (x = 0; x < WIDTH; x++) {
                i = y * WIDTH + x;
                if (cell[i] != WATER || waterMoved[i] != 0) {
                    continue;
                }
                if (coolNeighbours(i)) {
                    if ((random() & 3) == 0) {
                        cell[i] = EMPTY;
                        spawnBurst(i, WATER, 2, 1, 8);
                        continue;
                    }
                }
                int below = i + WIDTH;
                if (cell[below] == EMPTY) {
                    moveWater(i, below);
                    continue;
                }
                int direction = (random() & 1) == 0 ? -1 : 1;
                int nx = x + direction;
                if (nx >= 0 && nx < WIDTH && cell[below + direction] == EMPTY) {
                    moveWater(i, below + direction);
                    continue;
                }
                nx = x - direction;
                if (nx >= 0 && nx < WIDTH && cell[below - direction] == EMPTY) {
                    moveWater(i, below - direction);
                    continue;
                }
                nx = x + direction;
                if (nx >= 0 && nx < WIDTH && cell[i + direction] == EMPTY) {
                    moveWater(i, i + direction);
                }
            }
        }
    }

    private boolean coolNeighbours(int index) {
        boolean hot = false;
        int x = index % WIDTH;
        int y = index / WIDTH;
        if (x > 0) {
            hot = coolCell(index - 1) || hot;
        }
        if (x < WIDTH - 1) {
            hot = coolCell(index + 1) || hot;
        }
        if (y > 0) {
            hot = coolCell(index - WIDTH) || hot;
        }
        if (y < HEIGHT - 1) {
            hot = coolCell(index + WIDTH) || hot;
        }
        return hot;
    }

    private boolean coolCell(int index) {
        int h = heat[index] & 255;
        if (h == 0) {
            return false;
        }
        h -= 48;
        heat[index] = (byte) (h > 0 ? h : 0);
        return h > 45;
    }

    private void moveWater(int from, int to) {
        cell[to] = WATER;
        hp[to] = 0;
        heat[to] = 0;
        waterMoved[to] = 1;
        cell[from] = EMPTY;
        hp[from] = 0;
        heat[from] = 0;
    }

    private void updateParticles() {
        int i;
        for (i = 0; i < particleLimit; i++) {
            int life = particleLife[i] & 255;
            if (life == 0) {
                continue;
            }
            life--;
            if (life == 0) {
                particleLife[i] = 0;
                continue;
            }
            particleLife[i] = (byte) life;
            int kind = particleKind[i];
            int x = particleX[i];
            int y = particleY[i];
            int vx = particleVX[i];
            int vy = particleVY[i];

            if (kind == 2) {
                vy -= 1;
                if (vy < -10) {
                    vy = -10;
                }
                vx += (random() & 3) - 1;
            } else {
                vy += kind == 0 ? 3 : 2;
                if (vy > 70) {
                    vy = 70;
                }
            }
            int nx = x + vx;
            int ny = y + vy;

            if (kind == 0 && solidAtFixed(nx, ny)) {
                int impact = abs(vx) + abs(vy);
                int hit = fixedCell(nx, ny);
                if (impact > 26) {
                    damage(hit, (impact - 20) >> 2);
                }
                nx = x;
                ny = y;
                vx = -vx / 2;
                vy = -vy / 3;
                if (abs(vx) + abs(vy) < 5) {
                    particleLife[i] = (byte) (life < 12 ? life : 12);
                }
            }

            if (nx < 0 || nx >= (WIDTH * TILE << FP_SHIFT)
                    || ny < 0 || ny >= (HEIGHT * TILE << FP_SHIFT)) {
                if (kind == 0 && ny >= (HEIGHT * TILE << FP_SHIFT)) {
                    ny = (HEIGHT * TILE << FP_SHIFT) - 1;
                    vy = -vy / 3;
                } else {
                    particleLife[i] = 0;
                    continue;
                }
            }
            particleX[i] = (short) nx;
            particleY[i] = (short) ny;
            particleVX[i] = (short) vx;
            particleVY[i] = (short) vy;
        }
    }

    private boolean solidAtFixed(int x, int y) {
        if (x < 0 || y < 0 || x >= (WIDTH * TILE << FP_SHIFT)
                || y >= (HEIGHT * TILE << FP_SHIFT)) {
            return false;
        }
        return isStructural(cell[fixedCell(x, y)]);
    }

    private int fixedCell(int x, int y) {
        int px = x >> FP_SHIFT;
        int py = y >> FP_SHIFT;
        return (py / TILE) * WIDTH + (px / TILE);
    }

    private void spawnBurst(int index, byte material, int kind, int count, int speed) {
        int j;
        for (j = 0; j < count; j++) {
            spawnParticle(index, material, kind, speed);
        }
    }

    private void spawnParticle(int index, byte material, int kind, int speed) {
        int tries;
        int slot = -1;
        for (tries = 0; tries < particleLimit; tries++) {
            int candidate = particleCursor;
            particleCursor++;
            if (particleCursor >= particleLimit) {
                particleCursor = 0;
            }
            if (particleLife[candidate] == 0) {
                slot = candidate;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        int cx = ((index % WIDTH) * TILE + 2) << FP_SHIFT;
        int cy = ((index / WIDTH) * TILE + 2) << FP_SHIFT;
        int spread = speed + 1;
        int vx = kind == 2 ? (random() % 7) - 3 : (random() % (spread * 2 + 1)) - spread;
        int vy = kind == 2 ? -((random() % 8) + 3) : -((random() % (spread + 1)) + spread / 3);
        if (kind == 1 && (random() & 1) != 0) {
            vy = -vy / 2;
        }
        particleX[slot] = (short) cx;
        particleY[slot] = (short) cy;
        particleVX[slot] = (short) vx;
        particleVY[slot] = (short) vy;
        particleMaterial[slot] = material;
        particleKind[slot] = (byte) kind;
        int life = kind == 0 ? 75 + (random() % 70) : kind == 1 ? 20 + (random() % 30) : 25 + (random() % 35);
        particleLife[slot] = (byte) life;
    }

    public final int activeParticles() {
        int count = 0;
        int i;
        for (i = 0; i < particleLimit; i++) {
            if (particleLife[i] != 0) {
                count++;
            }
        }
        return count;
    }

    public final void setParticleLimit(int limit) {
        if (limit < 32) {
            limit = 32;
        }
        if (limit > MAX_PARTICLES) {
            limit = MAX_PARTICLES;
        }
        particleLimit = limit;
        if (particleCursor >= particleLimit) {
            particleCursor = 0;
        }
        int i;
        for (i = limit; i < MAX_PARTICLES; i++) {
            particleLife[i] = 0;
        }
    }

    public final int getParticleLimit() {
        return particleLimit;
    }

    private void recountBlocks() {
        blockCount = 0;
        int i;
        for (i = 0; i < SIZE; i++) {
            if (isBlock(cell[i])) {
                blockCount++;
            }
        }
    }

    public static boolean isStructural(byte material) {
        return material >= STEEL && material <= FUEL || material == ANCHOR;
    }

    public static boolean isBlock(byte material) {
        return material >= STEEL && material <= FUEL;
    }

    public static boolean isFlammable(byte material) {
        return material == WOOD || material == TNT || material == FUEL;
    }

    private static boolean inside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    private int random() {
        int value = randomState;
        value ^= value << 13;
        value ^= value >>> 17;
        value ^= value << 5;
        randomState = value;
        return value & 0x7fffffff;
    }

    private static int abs(int value) {
        return value < 0 ? -value : value;
    }
}
