package rdl.platform;

import rdl.core.World;

public final class PixelRendererTest {
    private static int assertions;

    public static void main(String[] args) {
        testFontPixels();
        testScreens();
        System.out.println("PixelRendererTest: " + assertions + " assertions passed.");
    }

    private static void testFontPixels() {
        int[] target = new int[16 * 16];
        PixelFont.draw(target, 16, 16, 0, 0, 1, "A", 0x112233);
        int lit = 0;
        int i;
        for (i = 0; i < target.length; i++) {
            if (target[i] == 0x112233) {
                lit++;
            }
        }
        check(lit == 10, "A uses only the ten active glyph pixels, not zero bits");
        check(PixelFont.width("AB", 2) == 14, "font width is deterministic");
    }

    private static void testScreens() {
        PixelRenderer renderer = new PixelRenderer();
        World world = new World();
        int scene;
        int material;
        int tool;
        for (scene = 0; scene < World.SCENE_COUNT; scene++) {
            renderer.renderTitle(scene, true, scene);
            world.reset(scene);
            for (material = World.STEEL; material <= World.WATER; material++) {
                world.setCell(5, 30, (byte) material);
                for (tool = 0; tool < 4; tool++) {
                    renderer.renderGame(world, 5, 30, material, tool, 1, 15);
                }
            }
        }
        renderer.renderHelp();
        check(renderer.pixels.length == 128 * 160, "framebuffer has native K510i size");
        check(renderer.pixels[0] != 0, "help screen was actually painted");
        world.setCell(5, 30, World.WOOD);
        world.ignite(5, 30);
        check((world.heat[30 * World.WIDTH + 5] & 255) > 0, "test scene contains a heated cell");
        world.impact(5, 30);
        renderer.renderGame(world, 5, 30, World.TNT, 3, 4, 9);
        check(world.explosionCount > 0, "heated cells, debris and damaged HUD render after a real blast");
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new RuntimeException("Assertion failed: " + message);
        }
    }
}
