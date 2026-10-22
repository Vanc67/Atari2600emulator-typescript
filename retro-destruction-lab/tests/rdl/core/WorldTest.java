package rdl.core;

public final class WorldTest {
    private static int assertions;

    public static void main(String[] args) {
        testScenes();
        testSupportAndCollapse();
        testChainReaction();
        testWaterCooling();
        testParticleBudget();
        testOverload();
        System.out.println("WorldTest: " + assertions + " assertions passed.");
    }

    private static void testScenes() {
        World world = new World();
        int scene;
        for (scene = 0; scene < World.SCENE_COUNT; scene++) {
            world.reset(scene);
            check(world.cell[World.SIZE - 1] == World.ANCHOR, "ground is anchored");
            check(world.supported[World.SIZE - 1] == 1, "ground enters support graph");
        }
        world.reset(World.SCENE_TOWER);
        check(world.blockCount > 100, "tower is a complete scene");
        world.reset(World.SCENE_BRIDGE);
        check(world.blockCount > 80, "bridge is a complete scene");
        world.reset(World.SCENE_EMPTY);
        check(world.blockCount == 0, "empty scene is fully empty above ground");
    }

    private static void testSupportAndCollapse() {
        World world = emptyWorld();
        world.setCell(5, 30, World.STEEL);
        world.setCell(10, 10, World.WOOD);
        world.step();
        check(world.cell[30 * World.WIDTH + 5] == World.STEEL, "grounded steel stays");
        check(world.cell[10 * World.WIDTH + 10] == World.EMPTY, "unsupported block detaches");
        check(world.collapseCount == 1, "collapse is counted once");
        check(world.activeParticles() > 0, "detached block becomes physical debris");
    }

    private static void testChainReaction() {
        World world = emptyWorld();
        world.setCell(10, 30, World.GLASS);
        world.setCell(11, 30, World.TNT);
        world.impact(10, 30);
        check(world.cell[30 * World.WIDTH + 10] == World.EMPTY, "blast breaks glass");
        check(world.cell[30 * World.WIDTH + 11] == World.EMPTY, "blast consumes TNT cell");
        int before = world.explosionCount;
        world.step();
        check(world.explosionCount > before, "destroyed TNT detonates on a later budgeted tick");
    }

    private static void testWaterCooling() {
        World world = emptyWorld();
        world.setCell(8, 30, World.WOOD);
        world.setCell(8, 29, World.WATER);
        world.ignite(8, 30);
        int before = world.heat[30 * World.WIDTH + 8] & 255;
        world.step();
        int after = world.heat[30 * World.WIDTH + 8] & 255;
        check(before > 0, "spark heats flammable material");
        check(after < before, "adjacent water removes heat");
    }

    private static void testParticleBudget() {
        World world = emptyWorld();
        world.setParticleLimit(64);
        int i;
        for (i = 0; i < 20; i++) {
            world.impact(i % World.WIDTH, 28);
        }
        check(world.activeParticles() <= 64, "particle pool never exceeds adaptive budget");
    }

    private static void testOverload() {
        World world = emptyWorld();
        int y;
        for (y = 0; y <= 30; y++) {
            world.setCell(4, y, World.GLASS);
        }
        int initial = world.blockCount;
        for (y = 0; y < 40; y++) {
            world.step();
        }
        check(world.blockCount < initial, "tall overloaded glass column eventually fails");
    }

    private static World emptyWorld() {
        World world = new World();
        world.reset(World.SCENE_EMPTY);
        world.running = true;
        return world;
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new RuntimeException("Assertion failed: " + message);
        }
    }
}
