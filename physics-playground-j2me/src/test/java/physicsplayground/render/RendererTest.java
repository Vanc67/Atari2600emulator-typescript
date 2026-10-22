package physicsplayground.render;

import java.util.Vector;
import physicsplayground.core.Content;
import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;
import physicsplayground.game.CameraController;
import physicsplayground.game.Entity;
import physicsplayground.game.InteractionController;
import physicsplayground.game.SandboxWorld;

public final class RendererTest {
    private static int checks, failures;
    private static void check(boolean value, String label) { checks++; if (!value) { failures++; System.out.println("FAIL " + label); } }
    private static int changed(SoftwareDraw draw, int background) { int count = 0; for (int i = 0; i < draw.pixels.length; i++) if (draw.pixels[i] != background) count++; return count; }
    private static boolean same(int[] a, int[] b) { if (a.length != b.length) return false; for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false; return true; }
    private static int[] copy(int[] source) { int[] result = new int[source.length]; for (int i = 0; i < source.length; i++) result[i] = source[i]; return result; }
    private static int styleIndex(String style) { for (int i = 0; i < EntityVisual.STYLES.length; i++) if (EntityVisual.STYLES[i].equals(style)) return i; return -1; }
    private static void cameraAt(CameraController camera, float x, float y) { camera.position.set(x, y); camera.targetPosition.set(x, y); camera.zoom = camera.targetZoom = 1; }
    public static void main(String[] args) throws Exception {
        SoftwareDraw draw = new SoftwareDraw(32, 32); draw.setIdentity(); draw.fillRect(4, 5, 8, 9, 0xff123456);
        check(draw.pixels[6 * 32 + 5] == 0xff123456, "filled rectangle writes ARGB"); check(draw.pixels[0] == 0xff000000, "rectangle leaves outside pixel");
        draw.clear(0xff000000); draw.fillRect(0, 0, 32, 32, 0x80ffffff); check(draw.pixels[0] == 0xff808080, "source alpha blends white over black");
        int[] before = copy(draw.pixels); draw.fillRect(-100, -100, 5, 5, 0xffffffff); draw.fillCircle(100, 100, 4, 0xffffffff); draw.drawLine(-50, -50, -40, -40, 2, 0xffffffff);
        check(same(before, draw.pixels), "offscreen primitives are clipped"); draw.setIdentity(); draw.clear(0xff000000); draw.setTransform(16, 16, 0, 1); draw.fillRect(-2, -6, 4, 12, 0xffffffff);
        check(draw.pixels[16 * 32 + 21] == 0xffffffff && draw.pixels[21 * 32 + 16] == 0xff000000, "affine transform rotates geometry");
        draw.setIdentity(); draw.clear(0xff000000); draw.drawLine(2, 2, 2, 2, 3, 0xffffffff); check(changed(draw, 0xff000000) > 0, "zero length line still draws cap");
        TintDraw tint = new TintDraw(draw); tint.setTint(1, 0, 0, .5f); draw.clear(0xff000000); tint.fillRect(0, 0, 32, 32, 0xffffffff); check(draw.pixels[0] == 0xff800000, "tint multiplies color and alpha");

        Content content = Content.loadDefault(); Vector definitions = content.registry.allEntities(); boolean[] styles = new boolean[EntityVisual.STYLES.length]; int renderedDefinitions = 0;
        SoftwareDraw art = new SoftwareDraw(128, 160);
        for (int i = 0; i < definitions.size(); i++) {
            Definitions.Entity definition = (Definitions.Entity) definitions.elementAt(i); if (definition.isAssembly()) continue;
            int style = styleIndex(definition.visualStyle); check(style >= 0, "known style " + definition.id); if (style >= 0) styles[style] = true;
            float scale = Math.min(2, Math.min(110 / definition.boundingWidth(), 140 / definition.boundingHeight())); art.clear(0xff26272a); art.setTransform(64, 80, scale, 0);
            EntityVisual.drawDefinition(art, definition, 17, 0); check(changed(art, 0xff26272a) > 0, "draws " + definition.id); renderedDefinitions++;
        }
        check(renderedDefinitions >= 25, "all non-assembly definitions exercised " + renderedDefinitions);
        Definitions.Entity plainDefinition = content.registry.entity("core:wooden_box"); String originalStyle = plainDefinition.visualStyle; plainDefinition.visualStyle = "plain";
        art.clear(0xff26272a); art.setTransform(64, 80, 1, 0); EntityVisual.drawDefinition(art, plainDefinition, 1, 0); plainDefinition.visualStyle = originalStyle;
        check(changed(art, 0xff26272a) > 0, "plain fallback style"); styles[styleIndex("plain")] = true;
        for (int i = 0; i < styles.length; i++) check(styles[i], "content or fallback covers style " + EntityVisual.STYLES[i]);
        Definitions.Entity concrete = content.registry.entity("core:concrete_block"); art.setTransform(64, 80, 1, 0); art.clear(0xff26272a); EntityVisual.drawDefinition(art, concrete, 5, 0); int[] concreteA = copy(art.pixels);
        art.clear(0xff26272a); EntityVisual.drawDefinition(art, concrete, 5, 0); check(same(concreteA, art.pixels), "concrete procedural seed deterministic");
        art.clear(0xff26272a); EntityVisual.drawDefinition(art, concrete, 6, 0); check(!same(concreteA, art.pixels), "concrete seed changes procedural marks");

        SandboxWorld world = new SandboxWorld(content, false, 99); CameraController camera = new CameraController(); cameraAt(camera, 0, -100); SoftwareDraw frame = new SoftwareDraw(128, 160); WorldRenderer renderer = new WorldRenderer(world, camera, frame);
        world.gridVisible = false; renderer.render(frame); int[] empty = copy(frame.pixels); Entity box = world.spawnEntity("core:wooden_box", new Vec2(0, -100), 0); renderer.render(frame);
        check(!same(empty, frame.pixels), "world renderer draws entity"); int[] normal = copy(frame.pixels); box.body.frozen = true; renderer.render(frame); check(!same(normal, frame.pixels), "frozen overlay reaches framebuffer");
        box.body.frozen = false; box.selected = true; renderer.render(frame); check(!same(normal, frame.pixels), "selection outline reaches framebuffer"); box.selected = false;
        box.health.applyDamage(50, null); renderer.render(frame); check(!same(normal, frame.pixels), "health damage changes pixels");
        box.addWound(new Vec2(0, 0)); renderer.render(frame); check(frame.pixels[80 * 128 + 64] == 0xff730f0f, "joint wound is rendered");
        box.body.setTransform(10000, 10000, 0); renderer.render(frame); check(same(empty, frame.pixels), "offscreen entity is culled"); world.removeEntity(box);
        world.interaction = new InteractionController(world); world.interaction.beginSpawn("core:wooden_box"); world.interaction.spawnPlacer.updatePreview(new Vec2(0, -100)); renderer.render(frame);
        check(!same(empty, frame.pixels), "placement ghost rendered");
        world.interaction.cancelSpawn(); world.effects.burst("sparks", new Vec2(0, -100), new Vec2(1, 0), 1); renderer.render(frame); check(!same(empty, frame.pixels), "particle effects rendered");
        System.out.println("RendererTest: " + checks + " checks, " + failures + " failure(s)"); if (failures > 0) System.exit(1);
    }
}
