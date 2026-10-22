package physicsplayground.save;

import java.util.Vector;
import physicsplayground.core.Json;
import physicsplayground.core.Vec2;
import physicsplayground.game.CameraController;
import physicsplayground.game.Entity;
import physicsplayground.game.components.EntityComponent;

public final class EntitySerializer {
    private EntitySerializer() {}
    public static Json.Map serialize(Entity entity) {
        Json.Map data = new Json.Map();
        data.put("uid", Json.num(entity.uid)); data.put("type", entity.definition.id);
        data.put("position", CameraController.vector(entity.body.position)); data.put("rotation", Json.num(entity.body.rotation));
        data.put("linear_velocity", CameraController.vector(entity.body.linearVelocity)); data.put("angular_velocity", Json.num(entity.body.angularVelocity));
        data.put("frozen", Json.bool(entity.body.frozen));
        Json.Map states = new Json.Map();
        for (int i = 0; i < entity.components.size(); i++) {
            Json.Map state = ((EntityComponent) entity.components.elementAt(i)).serializeState();
            if (!state.isEmpty()) states.put(entity.componentTypes.elementAt(i), state);
        }
        if (!states.isEmpty()) data.put("components", states);
        if (!entity.properties.isEmpty()) data.put("properties", Json.deepCopy(entity.properties));
        if (entity.assembly != null) {
            Json.Map membership = new Json.Map(); membership.put("uid", Json.num(entity.assembly.uid)); membership.put("part", entity.assemblyPartName); data.put("assembly", membership);
        }
        return data;
    }
    public static void applyState(Entity entity, Json.Map data, boolean includeMotion) {
        boolean frozen = Json.bool(data.get("frozen"), false);
        if (includeMotion && !frozen) {
            vector(data.get("linear_velocity"), entity.body.linearVelocity);
            entity.body.angularVelocity = (float) Json.number(data.get("angular_velocity"), 0); entity.resetImpactHistory();
        }
        entity.setFrozen(frozen);
        Json.Map properties = Json.object(data.get("properties"));
        if (properties != null) {
            properties = (Json.Map) Json.deepCopy(properties); Vector keys = properties.orderedKeys();
            for (int i = 0; i < keys.size(); i++) { Object key = keys.elementAt(i); entity.properties.put(key, properties.get(key)); }
        }
        Json.Map states = Json.object(data.get("components"));
        if (states != null) {
            Vector keys = states.orderedKeys();
            for (int i = 0; i < keys.size(); i++) {
                String type = (String) keys.elementAt(i); EntityComponent component = entity.component(type); Json.Map state = Json.object(states.get(type));
                if (component != null && state != null) component.deserializeState(state);
            }
        }
    }
    public static Vec2 vector(Object value, Vec2 fallback) {
        Vector array = Json.array(value);
        if (array != null && array.size() == 2) return fallback.set(((Json.Num) array.elementAt(0)).floatValue(), ((Json.Num) array.elementAt(1)).floatValue());
        return fallback;
    }
}
