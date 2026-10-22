package physicsplayground.render;

import physicsplayground.core.ColorValue;
import physicsplayground.core.Definitions;

/** Direct port of the recovered procedural firearm, melee, bomb and glass drawings. */
public final class WeaponArt {
    private static final float[] polygon = new float[64];
    private static final float[] SHAPE_1 = {-.5f,-.5f, .5f,-.5f, .5f,-.05f, -.05f,-.05f, -.18f,.5f, -.46f,.5f, -.36f,-.05f, -.5f,-.05f};
    private static final float[] SHAPE_2 = {-.5f,-.32f,-.18f,-.32f,-.18f,-.5f,.15f,-.5f,.15f,-.22f,.5f,-.22f,.5f,.02f,-.05f,.02f,-.2f,.5f,-.46f,.5f,-.38f,0,-.5f,0};
    private static final float[] SHAPE_3 = {-.5f,-.3f,-.22f,-.32f,-.22f,-.18f,.5f,-.18f,.5f,.02f,.12f,.02f,.12f,.1f,-.05f,.1f,-.1f,.5f,-.2f,.5f,-.2f,.12f,-.5f,.38f};
    private static final float[] SHAPE_4 = {0,.08f,.1f,.08f,.06f,.5f,-.03f,.5f};
    private static final float[] SHAPE_5 = {-.5f,-.1f,-.25f,-.3f,.5f,-.3f,.5f,-.02f,-.18f,-.02f,-.22f,.12f,-.5f,.45f};
    private static final float[] SHAPE_6 = {-.5f,-.2f,-.32f,-.32f,.5f,-.32f,.5f,-.02f,.08f,-.02f,.02f,.2f,-.12f,.2f,-.16f,-.02f,-.32f,-.02f,-.5f,.15f};
    private static final float[] SHAPE_7 = {.12f,-.02f,.24f,-.02f,.22f,.5f,.12f,.5f};
    private static final float[] SHAPE_8 = {-.5f,-.18f,-.1f,-.24f,.42f,-.5f,.5f,-.35f,.5f,.35f,.42f,.5f,-.1f,.24f,-.5f,.18f};
    private static final float[] SHAPE_9 = {.18f,-.5f,.5f,-.5f,.5f,.5f,.18f,.5f};
    private static final float[] SHAPE_10 = {.4f,-.5f,.5f,-1.4f,.56f,-1.3f,.48f,.5f,.4f,.5f};
    private WeaponArt() {}
    public static void drawFirearm(Draw canvas, Definitions.Entity def) {
        float w = def.size.x, h = def.size.y; int body = def.fillColor.argb(), dark = def.outlineColor.argb(), metal = def.detailColor.argb();
        String style = def.visualStyle;
        if ("pistol".equals(style)) {
            poly(canvas, SHAPE_1, w,h,body,dark);
            bar(canvas,-.45f,.48f,-.42f,-.3f,w,h,metal); bar(canvas,-.08f,.06f,-.05f,.12f,w,h,dark);
        } else if ("revolver".equals(style)) {
            poly(canvas,SHAPE_2,w,h,body,dark);
            canvas.fillCircle(-.01f*w,-.24f*h,h*.27f,metal); canvas.drawArc(-.01f*w,-.24f*h,h*.27f,0,tau(),16,1.2f,dark);
        } else if ("rifle".equals(style)) {
            poly(canvas,SHAPE_3,w,h,body,dark);
            poly(canvas,SHAPE_4,w,h,metal,dark); bar(canvas,-.15f,.2f,-.45f,-.3f,w,h,metal);
        } else if ("shotgun".equals(style)) {
            poly(canvas,SHAPE_5,w,h,body,dark);
            bar(canvas,.05f,.35f,.02f,.28f,w,h,metal); bar(canvas,-.15f,.5f,-.3f,-.18f,w,h,dark);
        } else if ("smg".equals(style)) {
            poly(canvas,SHAPE_6,w,h,body,dark);
            poly(canvas,SHAPE_7,w,h,metal,dark);
        }
    }
    public static void drawMelee(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y; String style=def.visualStyle;
        if ("bat".equals(style)) {
            poly(canvas,SHAPE_8,w,h,def.fillColor.argb(),def.outlineColor.argb());
            bar(canvas,-.5f,-.38f,-.3f,.3f,w,h,0xff26262b);
        } else if ("hammer".equals(style)) {
            bar(canvas,-.5f,.25f,-.18f,.18f,w,h,def.fillColor.argb());
            poly(canvas,SHAPE_9,w,h,def.detailColor.argb(),def.outlineColor.argb());
            canvas.drawRect(-.5f*w,-.18f*h,.68f*w,.36f*h,1,dark(def.fillColor,.35f));
        } else if ("crowbar".equals(style)) {
            bar(canvas,-.5f,.4f,-.5f,.5f,w,h,def.fillColor.argb());
            poly(canvas,SHAPE_10,w,h,def.fillColor.argb(),def.outlineColor.argb());
            bar(canvas,-.5f,-.44f,-.5f,.5f,w,h,def.detailColor.argb());
        } else if ("pipe".equals(style)) {
            bar(canvas,-.5f,.5f,-.5f,.5f,w,h,def.fillColor.argb()); bar(canvas,-.5f,.5f,-.5f,-.15f,w,h,def.detailColor.argb());
            bar(canvas,-.5f,-.44f,-.5f,.5f,w,h,def.outlineColor.argb()); bar(canvas,.44f,.5f,-.5f,.5f,w,h,def.outlineColor.argb());
            canvas.drawRect(-w*.5f,-h*.5f,w,h,1,def.outlineColor.argb());
        }
    }
    public static void drawHazardMarks(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y,bx=-w*.5f+2,by=-h*.12f,bw=w-4,bh=h*.24f;
        canvas.fillRect(bx,by,bw,bh,def.detailColor.argb()); float step=bh*1.1f,x=bx-step;
        while (x<bx+bw) {
            polygon[0]=clamp(x,bx,bx+bw);polygon[1]=by+bh;polygon[2]=clamp(x+step*.5f,bx,bx+bw);polygon[3]=by+bh;
            polygon[4]=clamp(x+step*.5f+bh,bx,bx+bw);polygon[5]=by;polygon[6]=clamp(x+bh,bx,bx+bw);polygon[7]=by;
            canvas.fillPolygon(polygon,4,0xff1f1a14);x+=step;
        }
        canvas.fillCircle(0,-h*.32f,w*.12f,def.detailColor.argb());
    }
    public static void drawBomb(Draw canvas, Definitions.Entity def) {
        if (def.isCircle()) {
            float r=def.radius;canvas.fillCircle(0,0,r,def.fillColor.argb());canvas.fillCircle(-r*.3f,-r*.3f,r*.3f,light(def.fillColor,.25f));
            canvas.fillRect(-r*.25f,-r*1.15f,r*.5f,r*.35f,def.outlineColor.argb());canvas.drawLine(0,-r*1.15f,r*.4f,-r*1.55f,2,def.detailColor.argb());
            canvas.drawArc(0,0,r-.75f,0,tau(),32,1.5f,def.outlineColor.argb());return;
        }
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f,cap=h*.5f;
        canvas.fillRect(x+cap,y,w-2*cap,h,def.fillColor.argb());canvas.fillCircle(x+cap,0,cap,def.fillColor.argb());
        polygon[0]=x+w-cap;polygon[1]=y;polygon[2]=x+w;polygon[3]=-cap*.35f;polygon[4]=x+w;polygon[5]=cap*.35f;polygon[6]=x+w-cap;polygon[7]=y+h;
        canvas.fillPolygon(polygon,4,dark(def.fillColor,.2f));canvas.fillRect(-w*.05f,y,w*.1f,h,def.detailColor.argb());canvas.drawRect(x,y,w,h,1.5f,def.outlineColor.argb());
    }
    public static void drawGlass(Draw canvas, Definitions.Entity def) {
        float w=def.size.x,h=def.size.y,x=-w*.5f,y=-h*.5f;
        canvas.fillRect(x,y,w,h,alpha(def.fillColor,.38f));canvas.drawLine(x+w*.3f,y+h*.08f,x+w*.3f,y+h*.45f,Math.max(w*.2f,1),alpha(def.detailColor,.6f));
        canvas.drawRect(x,y,w,h,1.5f,alpha(def.outlineColor,.9f));
    }
    private static void poly(Draw canvas,float[] normalized,float w,float h,int fill,int outline) {
        int count=normalized.length/2;for(int i=0;i<count;i++){polygon[i*2]=normalized[i*2]*w;polygon[i*2+1]=normalized[i*2+1]*h;}
        canvas.fillPolygon(polygon,count,fill);canvas.drawPolyline(polygon,count,true,1.2f,outline);
    }
    private static void bar(Draw canvas,float x0,float x1,float y0,float y1,float w,float h,int color){if((color>>>24)>0)canvas.fillRect(x0*w,y0*h,(x1-x0)*w,(y1-y0)*h,color);}
    static int alpha(ColorValue c,float a){return ((int)(c.a*a)&255)<<24|c.rgb();}
    static int dark(ColorValue c,float a){return c.a<<24|((int)(c.r*(1-a))<<16)|((int)(c.g*(1-a))<<8)|(int)(c.b*(1-a));}
    static int light(ColorValue c,float a){return c.a<<24|((int)(c.r+(255-c.r)*a)<<16)|((int)(c.g+(255-c.g)*a)<<8)|(int)(c.b+(255-c.b)*a);}
    private static float tau(){return (float)(Math.PI*2);}private static float clamp(float v,float a,float b){return v<a?a:(v>b?b:v);}
}
