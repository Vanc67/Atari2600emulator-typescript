package physicsplayground.core;

import java.util.Vector;

/** Runs without a MIDP VM; suitable for desktop CI and preverification checks. */
public final class ContentTest {
    private static int checks;
    private static int failures;

    public static void main(String[] args) throws Exception {
        Content content = Content.loadDefault();
        check(content.errors.isEmpty(), "core content loads without errors " + content.errors);
        check(content.registry.allEntities().size() == 36, "all 36 definitions including hidden parts and debris");
        check(content.registry.spawnableEntities().size() == 21, "21 unlocked spawn-menu entries");
        check(content.registry.categories().size() == 8, "eight original categories");
        check(content.registry.sounds().size() == 20, "twenty synthesized sound definitions");
        check(content.registry.hasEntity("core:human"), "human registered");
        Definitions.Entity human = content.registry.entity("core:human");
        check(human.assembly.parts.size() == 11 && human.assembly.joints.size() == 10, "11 ragdoll parts and 10 joints");
        check(Math.abs(human.computeMass(null) - 72.0f) < 0.01f, "human mass is 72 kg");
        check(content.registry.entity("core:pistol") != null && content.registry.projectile("core:pistol_round") != null, "firearm and projectile references resolve");
        check(content.registry.entity("core:wooden_box").computeMass(content.registry.material("core:wood")) > 0, "density-based mass");
        check(content.registry.entity("core:ice") == null, "unknown content stays unknown");

        Object parsed = Json.parse("{\"a\":[1,2.5,true,null],\"text\":\"x\\n\\u20ac\"}");
        check(parsed instanceof Json.Map, "JSON object parsed");
        check(((Vector) ((Json.Map) parsed).get("a")).size() == 4, "JSON array parsed");
        Object reparsed = Json.parse(Json.write(parsed));
        check(Json.write(parsed).equals(Json.write(reparsed)), "JSON round trip is stable");

        check(Math.abs(Math2.atan2(1, 1) - Math.PI * 0.25) < 0.000001, "CLDC atan2 quadrant");
        check(Math.abs(Math2.exp(1.0) - Math.E) < 0.000001, "CLDC exp");
        check(Math.abs(Math2.log(Math.E) - 1.0) < 0.000001, "CLDC log");
        check(Math.abs(Math2.pow(2.5, 3.0) - 15.625) < 0.000001, "CLDC pow");

        Random random = new Random(20261016);
        long[] expected = {3707747373L, 2333681017L, 1369136820L, 1482238926L, 3113541540L};
        for (int i = 0; i < expected.length; i++) check((random.nextInt() & 0xffffffffL) == expected[i], "Godot PCG32 output " + i);

        System.out.println("ContentTest: " + checks + " checks, " + failures + " failure(s)");
        if (failures > 0) System.exit(1);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) { failures++; System.out.println("FAIL " + label); }
    }
}
