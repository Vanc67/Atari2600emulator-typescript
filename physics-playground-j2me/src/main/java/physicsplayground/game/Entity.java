package physicsplayground.game;

import java.util.Vector;
import physicsplayground.core.Definitions;
import physicsplayground.core.Json;
import physicsplayground.physics.Body;
import physicsplayground.game.components.*;

/** Runtime SandboxEntity: definition + body + data-driven components, with no MIDP dependencies. */
public final class Entity {
    public final SandboxWorld world;
    public final Definitions.Entity definition;
    public final Definitions.Material material;
    public final long uid;
    public final Body body;
    public final Vector components = new Vector();
    public final Vector componentTypes = new Vector();
    public final Json.Map properties;
    public HealthComponent health;
    public DestructibleComponent destructible;
    public DebrisComponent debris;
    public ImpactDamageComponent impactDamage;
    public ThermalComponent thermal;
    public ElectricalComponent electrical;
    public MagazineComponent magazine;
    public FirearmComponent firearm;
    public ExplosiveComponent explosive;
    public MeleeComponent melee;
    public EntityAssembly assembly;
    public String assemblyPartName = "";
    public boolean selected;
    public boolean removed;
    public boolean destroyRequested;
    public float damageLevel;
    public float alpha = 1;
    public int vitality;
    public int zIndex;
    public boolean armedVisual;
    public boolean blink;
    public final Vector wounds = new Vector();

    public Entity(SandboxWorld world, Definitions.Entity definition, long uid) {
        this.world = world; this.definition = definition; this.uid = uid;
        this.material = world.content.registry.material(definition.materialId);
        this.body = new Body(definition, material);
        body.owner = this; body.id = uid;
        properties = (Json.Map) Json.deepCopy(definition.properties);
        Vector keys = definition.components.orderedKeys();
        for (int i = 0; i < keys.size(); i++) {
            String type = (String) keys.elementAt(i);
            Json.Map params = Json.object(definition.components.get(type));
            EntityComponent component = createComponent(type);
            if (component == null) continue;
            component.entity = this;
            component.configure(params == null ? new Json.Map() : params, definition, material);
            components.addElement(component); componentTypes.addElement(type);
        }
        for (int i = 0; i < components.size(); i++) ((EntityComponent) components.elementAt(i)).ready();
    }

    private EntityComponent createComponent(String type) {
        EntityComponent component = null;
        if ("health".equals(type)) { health = new HealthComponent(); component = health; }
        else if ("destructible".equals(type)) { destructible = new DestructibleComponent(); component = destructible; }
        else if ("debris".equals(type)) { debris = new DebrisComponent(); component = debris; }
        else if ("impact_damage".equals(type)) { impactDamage = new ImpactDamageComponent(); component = impactDamage; }
        else if ("thermal".equals(type)) { thermal = new ThermalComponent(); component = thermal; }
        else if ("electrical".equals(type)) { electrical = new ElectricalComponent(); component = electrical; }
        else if ("magazine".equals(type)) { magazine = new MagazineComponent(); component = magazine; }
        else if ("firearm".equals(type)) { firearm = new FirearmComponent(); component = firearm; }
        else if ("explosive".equals(type)) { explosive = new ExplosiveComponent(); component = explosive; }
        else if ("melee".equals(type)) { melee = new MeleeComponent(); component = melee; }
        return component;
    }

    public EntityComponent component(String type) {
        if ("health".equals(type)) return health; if ("destructible".equals(type)) return destructible;
        if ("debris".equals(type)) return debris; if ("impact_damage".equals(type)) return impactDamage;
        if ("thermal".equals(type)) return thermal; if ("electrical".equals(type)) return electrical;
        if ("magazine".equals(type)) return magazine; if ("firearm".equals(type)) return firearm;
        if ("explosive".equals(type)) return explosive; if ("melee".equals(type)) return melee;
        return null;
    }
    public void resetImpactHistory() {
        body.previousLinear.set(body.linearVelocity); body.previousAngular = body.angularVelocity;
        body.lastLinearChange.zero(); body.priorLinearChange.zero(); body.lastAngularChange = body.priorAngularChange = 0;
    }

    public void tick(float dt) {
        for (int i = 0; i < components.size(); i++) ((EntityComponent) components.elementAt(i)).tick(dt);
    }
    public void activate(boolean pressed) {
        for (int i = 0; i < components.size(); i++) ((EntityComponent) components.elementAt(i)).onActivate(pressed);
    }
    public void reload() {
        for (int i = 0; i < components.size(); i++) ((EntityComponent) components.elementAt(i)).onReload();
    }
    public boolean isActivatable() {
        for (int i = 0; i < components.size(); i++) if (((EntityComponent) components.elementAt(i)).isActivatable()) return true;
        return false;
    }
    public boolean isAssemblyPart() { return assembly != null; }
    public boolean isTransient() { return debris != null; }
    public boolean isFrozen() { return body.frozen; }
    public void setFrozen(boolean value) { body.setFrozen(value); }
    public void requestDestroy() {
        if (destroyRequested || removed) return;
        destroyRequested = true;
        world.requestDestroy(this);
    }
    public void onDamaged(DamageInfo info) { if (assembly != null) assembly.onPartDamaged(this, info); }
    public void onHealthChanged() {
        damageLevel = health == null ? 0 : health.damageFraction();
        if (destructible != null) destructible.onHealthChanged();
        if (assembly != null) assembly.onPartHealthChanged(this);
    }
    public void onDepleted(DamageInfo info) { if (explosive != null) explosive.onDepleted(info); }
    public void onTemperatureChanged() { if (explosive != null) explosive.onTemperatureChanged(thermal.temperature); }
    public void addWound(physicsplayground.core.Vec2 local) { if (wounds.size() < 24) wounds.addElement(local.copy()); }
}
