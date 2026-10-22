package physicsplayground.render;

import physicsplayground.core.ColorValue;
import physicsplayground.core.Definitions;
import physicsplayground.core.Random;
import physicsplayground.game.Entity;

/** Recovered EntityVisual drawing and all damage/thermal/frozen/selection overlays. */
public final class EntityVisual {
    public static final String[] STYLES = {"plain","crate","plated","barrel","concrete","ball","sphere","plank","limb","head","pistol","revolver","rifle","shotgun","smg","bat","hammer","crowbar","pipe","hazard_barrel","bomb","large_bomb","glass"};
    private static final int SELECTION = 0xffe3a33b, FROZEN_FILL = 0x4072bfff, FROZEN_OUTLINE = 0xf29edbff;
    private static final float[] polygon = new float[64], highlight = new float[64], crack = new float[6];
    private static final Random random = new Random(1);
    private EntityVisual() {}
    public static boolean isKnownStyle(String style) { for (int i = 0; i < STYLES.length; i++) if (STYLES[i].equals(style)) return true; return false; }
    public static void drawEntity(Draw canvas, Entity entity) {
        Definitions.Entity def = entity.definition;
        drawDefinition(canvas, def, entity.uid, entity.vitality);
        if (entity.damageLevel > 0) drawDamage(canvas, def, entity.damageLevel, entity.uid);
        if (entity.vitality == 2) shapeFill(canvas, def, 0x598c999e, 0);
        if (entity.thermal != null) {
            if (entity.thermal.charLevel > 0) shapeFill(canvas, def, ((int)(entity.thermal.charLevel * .75f * 255) << 24) | 0x140f0d, 0);
            float heat = entity.thermal.burning ? 0 : clamp((entity.thermal.temperature - 120) / 580, 0, 1);
            if (heat > 0) shapeFill(canvas, def, ((int)(heat * .5f * 255) << 24) | 0xff4d1a, 0);
            if (entity.thermal.burning) shapeFill(canvas, def, 0x59ff731a, 0);
        }
        if (entity.armedVisual) canvas.fillCircle(0, -def.boundingHeight() * .25f, 3.5f, entity.blink ? 0xffff261a : 0xff990f0a);
        for (int i = 0; i < entity.wounds.size(); i++) {
            physicsplayground.core.Vec2 wound = (physicsplayground.core.Vec2) entity.wounds.elementAt(i);
            canvas.fillCircle(wound.x, wound.y, Math.min(def.boundingWidth(), def.boundingHeight()) * .32f, 0xff730f0f);
        }
        if (entity.body.frozen) { shapeFill(canvas, def, FROZEN_FILL, 0); shapeOutline(canvas, def, FROZEN_OUTLINE, 2, 0); }
        if (entity.selected) shapeOutline(canvas, def, SELECTION, 2, 4);
    }
    public static void drawDefinition(Draw canvas, Definitions.Entity def, long seed, int vitality) {
        String style = def.visualStyle;
        if ("crate".equals(style)) drawCrate(canvas, def);
        else if ("plated".equals(style)) drawPlated(canvas, def);
        else if ("barrel".equals(style)) drawBarrel(canvas, def);
        else if ("concrete".equals(style)) drawConcrete(canvas, def, seed);
        else if ("ball".equals(style)) drawBall(canvas, def);
        else if ("sphere".equals(style)) drawSphere(canvas, def);
        else if ("plank".equals(style)) drawPlank(canvas, def);
        else if ("limb".equals(style)) drawLimb(canvas, def);
        else if ("head".equals(style)) drawHead(canvas, def, vitality);
        else if ("pistol".equals(style) || "revolver".equals(style) || "rifle".equals(style) || "shotgun".equals(style) || "smg".equals(style)) WeaponArt.drawFirearm(canvas, def);
        else if ("bat".equals(style) || "hammer".equals(style) || "crowbar".equals(style) || "pipe".equals(style)) WeaponArt.drawMelee(canvas, def);
        else if ("hazard_barrel".equals(style)) { drawBarrel(canvas, def); WeaponArt.drawHazardMarks(canvas, def); }
        else if ("bomb".equals(style) || "large_bomb".equals(style)) WeaponArt.drawBomb(canvas, def);
        else if ("glass".equals(style)) WeaponArt.drawGlass(canvas, def);
        else drawPlain(canvas, def);
    }
    public static void shapeFill(Draw canvas, Definitions.Entity def, int color, float grow) {
        if (def.isCircle()) canvas.fillCircle(0, 0, def.radius + grow, color);
        else if (def.isCapsule()) { int count = capsulePoints(def.radius + grow, def.size.y * .5f - def.radius, polygon); canvas.fillPolygon(polygon, count, color); }
        else canvas.fillRect(-def.size.x * .5f - grow, -def.size.y * .5f - grow, def.size.x + grow * 2, def.size.y + grow * 2, color);
    }
    public static void shapeOutline(Draw canvas, Definitions.Entity def, int color, float width, float grow) {
        if (def.isCircle()) canvas.drawArc(0, 0, def.radius + grow, 0, tau(), 48, width, color);
        else if (def.isCapsule()) { int count = capsulePoints(def.radius + grow, def.size.y * .5f - def.radius, polygon); canvas.drawPolyline(polygon, count, true, width, color); }
        else canvas.drawRect(-def.size.x * .5f - grow, -def.size.y * .5f - grow, def.size.x + grow * 2, def.size.y + grow * 2, width, color);
    }
    public static void drawDamage(Draw canvas, Definitions.Entity def, float level, long seed) {
        shapeFill(canvas, def, WeaponArt.alpha(def.damageColor, level * .6f), -1);
        if (level < .4f) return;
        int marks = level < .75f ? 2 : 5; float ex = def.boundingWidth() * .5f, ey = def.boundingHeight() * .5f;
        if (def.isCircle() || def.isCapsule()) { ex *= .6f; ey = Math.max(ey - def.radius * .4f, ex * .6f); }
        else { ex *= .8f; ey *= .8f; }
        random.seed(hash(seed * 7919)); int color = WeaponArt.dark(def.damageColor, .4f); float width = Math.max(Math.min(ex, ey) * .12f, 1.2f);
        for (int i = 0; i < marks; i++) {
            float sx = random.range(-ex, ex), sy = random.range(-ey, ey), reach = Math.min(ex, ey) * .6f;
            float bx = clamp(sx + random.range(-1, 1) * reach, -ex, ex), by = clamp(sy + random.range(-1, 1) * reach, -ey, ey);
            float fx = clamp(bx + random.range(-1, 1) * reach, -ex, ex), fy = clamp(by + random.range(-1, 1) * reach, -ey, ey);
            crack[0] = sx; crack[1] = sy; crack[2] = bx; crack[3] = by; crack[4] = fx; crack[5] = fy; canvas.drawPolyline(crack, 3, false, width, color);
        }
    }
    private static int capsulePoints(float radius, float halfStraight, float[] output) {
        halfStraight = Math.max(halfStraight, 0); int at = 0;
        for (int step = 0; step <= 8; step++) { float angle = (float) (Math.PI + Math.PI * step / 8); output[at++] = (float) Math.cos(angle) * radius; output[at++] = (float) Math.sin(angle) * radius - halfStraight; }
        for (int step = 0; step <= 8; step++) { float angle = (float) (Math.PI * step / 8); output[at++] = (float) Math.cos(angle) * radius; output[at++] = (float) Math.sin(angle) * radius + halfStraight; }
        return at / 2;
    }
    private static void drawPlain(Draw canvas, Definitions.Entity def) { shapeFill(canvas, def, def.fillColor.argb(), 0); shapeOutline(canvas, def, def.outlineColor.argb(), 2, -1); }
    private static void drawLimb(Draw canvas, Definitions.Entity def) {
        shapeFill(canvas, def, def.fillColor.argb(), 0);
        if (def.isCapsule()) { int count = capsulePoints(def.radius * .35f, def.size.y * .5f - def.radius, highlight); for (int i = 0; i < count; i++) highlight[i * 2] -= def.radius * .35f; canvas.fillPolygon(highlight, count, WeaponArt.alpha(def.detailColor, .5f)); }
        shapeOutline(canvas, def, def.outlineColor.argb(), 1.5f, -.5f);
    }
    private static void drawHead(Draw canvas, Definitions.Entity def, int vitality) {
        float r = def.radius; canvas.fillCircle(0, 0, r, def.fillColor.argb()); canvas.drawArc(0, 0, r - .75f, 0, tau(), 32, 1.5f, def.outlineColor.argb());
        float ex = r * .42f, ey = -r * .18f, size = r * .16f;
        if (vitality == 2) { canvas.drawLine(ex - size, ey - size, ex + size, ey + size, 1.5f, def.outlineColor.argb()); canvas.drawLine(ex - size, ey + size, ex + size, ey - size, 1.5f, def.outlineColor.argb()); }
        else if (vitality == 1) canvas.drawLine(ex - size, ey, ex + size, ey, 1.5f, def.outlineColor.argb());
        else canvas.fillCircle(ex, ey, size, def.outlineColor.argb());
        canvas.drawLine(r * .2f, r * .42f, r * .55f, r * .38f, 1.2f, def.detailColor.argb());
    }
    private static void drawCrate(Draw canvas, Definitions.Entity def) {
        float w = def.size.x, h = def.size.y, x = -w * .5f, y = -h * .5f, inset = Math.min(w, h) * .13f, ix = x + inset, iy = y + inset, iw = w - inset * 2, ih = h - inset * 2;
        canvas.fillRect(x, y, w, h, def.fillColor.argb());
        for (int i = 1; i < 3; i++) { float py = iy + ih * i / 3; canvas.drawLine(ix, py, ix + iw, py, 1.5f, def.detailColor.argb()); }
        canvas.drawLine(ix, iy, ix + iw, iy + ih, inset * .7f, def.detailColor.argb()); canvas.drawRect(ix, iy, iw, ih, 1.5f, def.outlineColor.argb()); canvas.drawRect(x + 1, y + 1, w - 2, h - 2, 2, def.outlineColor.argb());
    }
    private static void drawPlated(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f,inset=Math.min(w,h)*.16f;
        canvas.fillRect(x,y,w,h,def.fillColor.argb());int light=WeaponArt.light(def.fillColor,.25f),dark=WeaponArt.dark(def.fillColor,.3f);
        canvas.drawLine(x,y,x+w,y,3,light);canvas.drawLine(x,y,x,y+h,3,light);canvas.drawLine(x,y+h,x+w,y+h,3,dark);canvas.drawLine(x+w,y,x+w,y+h,3,dark);
        canvas.drawRect(x+inset,y+inset,w-2*inset,h-2*inset,1.5f,def.detailColor.argb());float offset=inset*.5f,r=Math.max(inset*.16f,1.5f);
        for(int i=0;i<4;i++){float sx=(i==0||i==3)?-1:1,sy=i<2?-1:1;canvas.fillCircle(sx*(w*.5f-offset),sy*(h*.5f-offset),r,def.detailColor.argb());}
        canvas.drawRect(x+1,y+1,w-2,h-2,2,def.outlineColor.argb());
    }
    private static void drawBarrel(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f,r=Math.min(w,h)*.2f;int count=roundedRect(x,y,w,h,r,polygon);
        canvas.fillPolygon(polygon,count,def.fillColor.argb());canvas.fillRect(x+w*.16f,y+r*.6f,w*.12f,h-r*1.2f,WeaponArt.light(def.fillColor,.18f));
        canvas.drawLine(x+1,y+h*.22f,x+w-1,y+h*.22f,3,def.detailColor.argb());canvas.drawLine(x+1,y+h*.78f,x+w-1,y+h*.78f,3,def.detailColor.argb());canvas.drawPolyline(polygon,count,true,2,def.outlineColor.argb());
    }
    private static int roundedRect(float x,float y,float w,float h,float r,float[] out) {
        int at=0;for(int corner=0;corner<4;corner++){
            float cx=corner==0||corner==1?x+w-r:x+r,cy=corner<2?(corner==0?y+r:y+h-r):y+h-r;
            if(corner==3){cx=x+r;cy=y+r;} float start=(float)(-Math.PI*.5+ corner*Math.PI*.5);
            for(int step=0;step<=5;step++){float angle=start+(float)(Math.PI*.5)*step/5;out[at++]=(float)Math.cos(angle)*r+cx;out[at++]=(float)Math.sin(angle)*r+cy;}
        }return at/2;
    }
    private static void drawConcrete(Draw canvas, Definitions.Entity def, long seed) {
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f;canvas.fillRect(x,y,w,h,def.fillColor.argb());random.seed(hash(seed));
        int count=(int)(w*h/300);for(int i=0;i<count;i++){float size=random.range(1.5f,3.5f),px=random.range(x+3,x+w-3-size),py=random.range(y+3,y+h-3-size);canvas.fillRect(px,py,size,size,def.detailColor.argb());}
        canvas.drawLine(x,y+h-2,x+w,y+h-2,4,WeaponArt.dark(def.fillColor,.15f));canvas.drawRect(x+1,y+1,w-2,h-2,2,def.outlineColor.argb());
    }
    private static void drawBall(Draw canvas, Definitions.Entity def) { float r=def.radius;canvas.fillCircle(0,0,r,def.fillColor.argb());canvas.drawLine(-r*.9f,0,r*.9f,0,r*.3f,def.detailColor.argb());canvas.drawArc(0,0,r-1,0,tau(),40,2,def.outlineColor.argb()); }
    private static void drawSphere(Draw canvas, Definitions.Entity def) { float r=def.radius;canvas.fillCircle(0,0,r,def.fillColor.argb());canvas.fillCircle(-r*.3f,-r*.3f,r*.38f,WeaponArt.alpha(def.detailColor,.55f));canvas.drawLine(0,r*.45f,0,r*.85f,2,def.outlineColor.argb());canvas.drawArc(0,0,r-1,0,tau(),40,2,def.outlineColor.argb()); }
    private static void drawPlank(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f;canvas.fillRect(x,y,w,h,def.fillColor.argb());
        if(w>=h){canvas.drawLine(x+8,y+h*.33f,x+w-8,y+h*.33f,1,def.detailColor.argb());canvas.drawLine(x+8,y+h*.66f,x+w-8,y+h*.66f,1,def.detailColor.argb());}
        else{canvas.drawLine(x+w*.33f,y+8,x+w*.33f,y+h-8,1,def.detailColor.argb());canvas.drawLine(x+w*.66f,y+8,x+w*.66f,y+h-8,1,def.detailColor.argb());}
        canvas.drawRect(x+1,y+1,w-2,h-2,2,def.outlineColor.argb());
    }
    private static long hash(long value) { return physicsplayground.core.GodotHash.integer(value) & 0xffffffffL; }
    private static float tau(){return (float)(Math.PI*2);}private static float clamp(float v,float a,float b){return v<a?a:(v>b?b:v);}
}
