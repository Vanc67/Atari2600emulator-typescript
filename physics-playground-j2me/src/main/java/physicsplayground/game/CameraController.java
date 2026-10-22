package physicsplayground.game;

import physicsplayground.core.Json;
import physicsplayground.core.Math2;
import physicsplayground.core.Vec2;

/** Core camera math; key/mouse/touch translation is supplied by the platform layer. */
public final class CameraController {
    public float minZoom = .15f, maxZoom = 3, wheelZoomStep = 1.15f, keyboardZoomSpeed = 1.8f, smoothing = 14;
    public float keyboardPanSpeed = 1200, trackpadPanSpeed = 30;
    public final Vec2 homePosition = new Vec2(0, -220), position = new Vec2(0, -220), targetPosition = new Vec2(0, -220);
    public float zoom = 1, targetZoom = 1, homeZoom = 1;
    public float viewportWidth = 1600, viewportHeight = 900;
    public float boundsX = -100000, boundsY = -100000, boundsMaxX = 100000, boundsMaxY = 100000;
    private final Vec2 zoomAnchorScreen = new Vec2();
    private final Vec2 before = new Vec2(), after = new Vec2();
    public void setViewport(float width, float height) { viewportWidth = width; viewportHeight = height; }
    public void setBounds(float x, float y, float width, float height) { boundsX = x; boundsY = y; boundsMaxX = x + width; boundsMaxY = y + height; clampToBounds(); }
    public void zoomToward(Vec2 screenPosition, float factor) { zoomAnchorScreen.set(screenPosition); targetZoom = clamp(targetZoom * factor, minZoom, maxZoom); }
    public void resetView() { targetPosition.set(homePosition); zoomAnchorScreen.set(viewportWidth * .5f, viewportHeight * .5f); targetZoom = homeZoom; }
    public void panBy(Vec2 screenDelta) { position.scaleAdd(screenDelta, -1 / zoom); targetPosition.scaleAdd(screenDelta, -1 / zoom); clampToBounds(); }
    public void panGesture(Vec2 delta) { targetPosition.scaleAdd(delta, trackpadPanSpeed / zoom); clampToBounds(); }
    public void update(float realDelta, float panX, float panY, float zoomInput) {
        realDelta = Math.min(realDelta, .1f);
        if (panX != 0 || panY != 0) {
            targetPosition.x += panX * keyboardPanSpeed * realDelta / zoom; targetPosition.y += panY * keyboardPanSpeed * realDelta / zoom; clampToBounds();
        }
        if (zoomInput != 0) {
            before.set(viewportWidth * .5f, viewportHeight * .5f);
            zoomToward(before, (float) Math2.pow(keyboardZoomSpeed, zoomInput * realDelta));
        }
        float blend = 1 - (float) Math2.exp(-smoothing * realDelta);
        if (Math.abs(zoom - targetZoom) > .000001f) {
            float next = (float) Math2.exp(Math2.log(zoom) + (Math2.log(targetZoom) - Math2.log(zoom)) * blend);
            if (Math.abs(next - targetZoom) < .0005f) next = targetZoom;
            setZoomKeepingAnchor(next);
        }
        position.lerp(targetPosition, blend);
    }
    public Vec2 screenToWorld(Vec2 screen, Vec2 out) { return out.set(position.x + (screen.x - viewportWidth * .5f) / zoom, position.y + (screen.y - viewportHeight * .5f) / zoom); }
    private void setZoomKeepingAnchor(float value) {
        screenToWorld(zoomAnchorScreen, before); zoom = value; screenToWorld(zoomAnchorScreen, after);
        position.x += before.x - after.x; position.y += before.y - after.y;
        targetPosition.x += before.x - after.x; targetPosition.y += before.y - after.y; clampToBounds();
    }
    private void clampToBounds() {
        targetPosition.x = clamp(targetPosition.x, boundsX, boundsMaxX); targetPosition.y = clamp(targetPosition.y, boundsY, boundsMaxY);
        position.x = clamp(position.x, boundsX, boundsMaxX); position.y = clamp(position.y, boundsY, boundsMaxY);
    }
    public Json.Map serializeState() {
        Json.Map data = new Json.Map(); data.put("position", vector(targetPosition)); data.put("zoom", Json.num(targetZoom)); return data;
    }
    public void applyState(Json.Map data) {
        java.util.Vector position = Json.array(data.get("position"));
        if (position != null && position.size() == 2) targetPosition.set(((Json.Num) position.elementAt(0)).floatValue(), ((Json.Num) position.elementAt(1)).floatValue());
        zoomAnchorScreen.set(viewportWidth * .5f, viewportHeight * .5f); targetZoom = clamp((float) Json.number(data.get("zoom"), targetZoom), minZoom, maxZoom); clampToBounds();
    }
    public static java.util.Vector vector(Vec2 value) { java.util.Vector result = new java.util.Vector(); result.addElement(Json.num(value.x)); result.addElement(Json.num(value.y)); return result; }
    private static float clamp(float v, float a, float b) { return v < a ? a : (v > b ? b : v); }
}
