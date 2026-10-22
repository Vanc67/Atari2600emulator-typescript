package physicsplayground.save;

import java.util.Vector;
import physicsplayground.core.Json;
import physicsplayground.game.AssemblyJoint;
import physicsplayground.game.EntityAssembly;

public final class AssemblySerializer {
    private AssemblySerializer() {}
    public static Json.Map serialize(EntityAssembly assembly) {
        Json.Map joints = new Json.Map();
        for (int i = 0; i < assembly.joints.size(); i++) {
            AssemblyJoint joint = (AssemblyJoint) assembly.joints.elementAt(i); Json.Map state = new Json.Map(); state.put("connected", Json.bool(!joint.broken)); joints.put(joint.definition.name, state);
        }
        Json.Map data = new Json.Map(); data.put("uid", Json.num(assembly.uid)); data.put("type", assembly.definition.id); data.put("joints", joints);
        Json.Map controller = assembly.controller.serializeState(); if (!controller.isEmpty()) data.put("controller", controller); return data;
    }
    public static Vector brokenJointNames(Json.Map data) {
        Vector broken = new Vector(); Json.Map joints = Json.object(data.get("joints")); if (joints == null) return broken;
        Vector names = joints.orderedKeys();
        for (int i = 0; i < names.size(); i++) {
            String name = (String) names.elementAt(i); Json.Map state = Json.object(joints.get(name));
            if (state != null && !Json.bool(state.get("connected"), true)) broken.addElement(name);
        }
        return broken;
    }
}
