package physicsplayground.render;

import physicsplayground.core.Definitions;
import physicsplayground.core.Vec2;
import physicsplayground.game.CameraController;
import physicsplayground.game.Effects;
import physicsplayground.game.Entity;
import physicsplayground.game.InteractionController;
import physicsplayground.game.ProjectileSystem;
import physicsplayground.game.SandboxWorld;

/** Draws the recovered world into a small software framebuffer without per-frame scene objects. */
public final class WorldRenderer {
    private final SandboxWorld world;
    private final CameraController camera;
    private final int width, height;
    private final TintDraw tint;
    private final Vec2 tail = new Vec2(), point = new Vec2(), direction = new Vec2();
    private final float[] triangle = new float[6];
    public int effectStride = 1;
    public WorldRenderer(SandboxWorld world, CameraController camera, SoftwareDraw draw) {
        this.world = world; this.camera = camera; this.width = draw.width; this.height = draw.height; tint = new TintDraw(draw);
    }
    public void render(SoftwareDraw draw) {
        draw.clear(0xff26272a); camera.setViewport(width, height); worldTransform(draw);
        if (world.gridVisible) drawGrid(draw); drawGeometry(draw);
        for (int layer = -1; layer <= 1; layer++) for (int i = 0; i < world.entities.size(); i++) {
            Entity entity = (Entity) world.entities.elementAt(i); if (entity.zIndex != layer || !visible(entity.body.position, entity.definition.boundingRadius())) continue;
            setEntityTransform(draw, entity.body.position.x, entity.body.position.y, entity.body.cosine, entity.body.sine);
            tint.setTint(1, 1, 1, entity.alpha); EntityVisual.drawEntity(tint, entity);
        }
        worldTransform(draw); drawProjectiles(draw); drawEffects(draw);
        InteractionController interaction = world.interaction;
        if (interaction != null) {
            if (interaction.drag != null && interaction.drag.isValid()) {
                Vec2 anchor = interaction.drag.anchorWorld(); draw.drawLine(anchor.x, anchor.y, interaction.drag.targetPosition.x, interaction.drag.targetPosition.y, 2 / camera.zoom, 0xcce3a33b);
                draw.fillCircle(anchor.x, anchor.y, 4 / camera.zoom, 0xcce3a33b);
            }
            if (interaction.isSpawning()) drawGhost(draw, interaction);
        }
        draw.setIdentity();
    }
    private void drawGrid(SoftwareDraw draw) {
        float spacing = 100;
        while (spacing * camera.zoom < 22) spacing *= 2;
        while (spacing * camera.zoom > 88 && spacing > 12.5f) spacing *= .5f;
        float halfW = width * .5f / camera.zoom, halfH = height * .5f / camera.zoom;
        float left = camera.position.x - halfW - spacing, right = camera.position.x + halfW + spacing, top = camera.position.y - halfH - spacing, bottom = camera.position.y + halfH + spacing;
        int firstX = (int) Math.floor(left / spacing), lastX = (int) Math.ceil(right / spacing), firstY = (int) Math.floor(top / spacing), lastY = (int) Math.ceil(bottom / spacing);
        for (int i = firstX; i <= lastX; i++) { float x = i * spacing; draw.drawLine(x, top, x, bottom, 1, mod(i, 5) == 0 ? 0x13ffffff : 0x09ffffff); }
        for (int i = firstY; i <= lastY; i++) { float y = i * spacing; draw.drawLine(left, y, right, y, 1, mod(i, 5) == 0 ? 0x13ffffff : 0x09ffffff); }
    }
    private void drawGeometry(SoftwareDraw draw) {
        float outer = world.halfWidth + 200;
        draw.fillRect(-outer, world.floorY, outer * 2, 6000, 0xff34373c);
        draw.fillRect(-outer, world.floorY - world.wallHeight, 200, world.wallHeight, 0xff34373c);
        draw.fillRect(world.halfWidth, world.floorY - world.wallHeight, 200, world.wallHeight, 0xff34373c);
        draw.drawLine(-world.halfWidth, world.floorY, world.halfWidth, world.floorY, 3, 0xff5b6068);
        draw.drawLine(-world.halfWidth, world.floorY - world.wallHeight, -world.halfWidth, world.floorY, 3, 0xff5b6068);
        draw.drawLine(world.halfWidth, world.floorY - world.wallHeight, world.halfWidth, world.floorY, 3, 0xff5b6068);
    }
    private void drawProjectiles(SoftwareDraw draw) {
        for (int i = 0; i < world.projectiles.count(); i++) {
            ProjectileSystem.Projectile p = world.projectiles.projectileAt(i); tail.set(p.position).scaleAdd(p.velocity, -p.definition.trailTime);
            if (tail.distanceSquaredTo(p.position) > p.trailStart.distanceSquaredTo(p.position)) tail.set(p.trailStart);
            draw.drawLine(tail.x, tail.y, p.position.x, p.position.y, p.definition.trailWidth, p.definition.trailColor.argb());
        }
    }
    private void drawEffects(SoftwareDraw draw) {
        Effects fx = world.effects;
        int stride = Math.max(1, effectStride);
        for (int i = 0; i < fx.alive; i += stride) {
            float t = fx.life[i] / fx.maxLife[i], size = fx.size[i]; int color = fx.colors[i];
            if (fx.shapes[i] == Effects.STREAK) draw.drawLine(fx.x[i], fx.y[i], fx.x[i] - fx.vx[i] * .02f, fx.y[i] - fx.vy[i] * .02f, size, alpha(color, t));
            else if (fx.shapes[i] == Effects.PUFF) draw.fillCircle(fx.x[i], fx.y[i], size * (1.6f - t * .6f), alpha(color, t));
            else if (fx.shapes[i] == Effects.SHARD) draw.fillRect(fx.x[i] - size * .5f, fx.y[i] - size * .5f, size, size, alpha(color, Math.min(t * 2, 1)));
            else if (fx.shapes[i] == Effects.FLAME) {
                color = greenScale(color, .6f + t * .4f); draw.fillCircle(fx.x[i], fx.y[i], size * (.5f + t * .5f), alpha(color, t));
            } else draw.fillCircle(fx.x[i], fx.y[i], size * .6f, alpha(color, Math.min(t * 2, 1)));
        }
        for (int i = 0; i < fx.flashCount; i++) {
            float t = fx.flashLife[i] / fx.flashMaxLife[i], radius = fx.flashRadius[i], x = fx.flashX[i], y = fx.flashY[i];
            if (fx.flashKind[i] == 0) {
                float sx = -fx.flashDirY[i] * radius * .35f, sy = fx.flashDirX[i] * radius * .35f;
                triangle[0] = x + sx; triangle[1] = y + sy; triangle[2] = x + fx.flashDirX[i] * radius; triangle[3] = y + fx.flashDirY[i] * radius; triangle[4] = x - sx; triangle[5] = y - sy;
                draw.fillPolygon(triangle, 3, alpha(0xe6ffd866, t)); draw.fillCircle(x, y, radius * .3f, alpha(0xfffff2b2, t));
            } else if (fx.flashKind[i] == 1) draw.fillCircle(x, y, radius * (1.2f - t * .2f), alpha(0xffffcc26, t * .85f));
            else draw.drawArc(x, y, radius * (1 - t * .7f), 0, (float) (Math.PI * 2), 48, 4 * t + 1, alpha(0xffffe5b2, t * .5f));
        }
    }
    private void drawGhost(SoftwareDraw draw, InteractionController interaction) {
        Definitions.Entity definition = interaction.spawnPlacer.definition; boolean valid = interaction.spawnPlacer.placementValid;
        tint.setTint(valid ? .75f : 1, valid ? 1 : .4f, valid ? .8f : .35f, .55f);
        if (!definition.isAssembly()) {
            setEntityTransform(draw, interaction.spawnPlacer.position.x, interaction.spawnPlacer.position.y, (float) Math.cos(interaction.spawnPlacer.placementRotation), (float) Math.sin(interaction.spawnPlacer.placementRotation));
            EntityVisual.drawDefinition(tint, definition, 1, 0); return;
        }
        for (int layer = -1; layer <= 1; layer++) for (int i = 0; i < definition.assembly.parts.size(); i++) {
            Definitions.Part part = (Definitions.Part) definition.assembly.parts.elementAt(i); if (part.zIndex != layer) continue;
            point.set(part.position).rotate(interaction.spawnPlacer.placementRotation).add(interaction.spawnPlacer.position);
            float angle = interaction.spawnPlacer.placementRotation + part.rotation;
            setEntityTransform(draw, point.x, point.y, (float) Math.cos(angle), (float) Math.sin(angle)); EntityVisual.drawDefinition(tint, part.definition, 1, 0);
        }
    }
    private boolean visible(Vec2 position, float radius) {
        float x = (position.x - camera.position.x) * camera.zoom + width * .5f, y = (position.y - camera.position.y) * camera.zoom + height * .5f, r = radius * camera.zoom;
        return x + r >= 0 && x - r < width && y + r >= 0 && y - r < height;
    }
    private void worldTransform(SoftwareDraw draw) { draw.setTransform(width * .5f - camera.position.x * camera.zoom, height * .5f - camera.position.y * camera.zoom, camera.zoom, 0); }
    private void setEntityTransform(SoftwareDraw draw, float x, float y, float cosine, float sine) {
        draw.setTransform(width * .5f + (x - camera.position.x) * camera.zoom, height * .5f + (y - camera.position.y) * camera.zoom, cosine * camera.zoom, sine * camera.zoom);
    }
    private static int alpha(int color, float factor) { int a = Math.min(255, (int) ((color >>> 24) * factor)); return a << 24 | (color & 0xffffff); }
    private static int greenScale(int color, float factor) { return color & 0xffff00ff | ((int) ((color >>> 8 & 255) * factor) & 255) << 8; }
    private static int mod(int value, int divisor) { int result = value % divisor; return result < 0 ? result + divisor : result; }
}
