import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Source contracts for peaceful independent patrols, not the retired Wall swarm. */
public final class DriftingWallPatrolLeashRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Path findRoot() {
        Path candidate = Paths.get("").toAbsolutePath();
        for (int depth = 0; depth < 4 && candidate != null; depth++) {
            if (Files.isRegularFile(candidate.resolve(
                    "src/chiefnavigator/quest/MenelausTrial.java"))) {
                return candidate;
            }
            candidate = candidate.getParent();
        }
        throw new IllegalStateException("Could not locate Chief Navigator root");
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        check(start >= 0, "Missing source contract: " + signature);
        int open = source.indexOf('{', start);
        int depth = 1;
        for (int end = open + 1; end < source.length(); end++) {
            if (source.charAt(end) == '{') depth++;
            if (source.charAt(end) == '}') depth--;
            if (depth == 0) return source.substring(open + 1, end);
        }
        throw new AssertionError("Unclosed patrol method: " + signature);
    }

    public static void main(String[] args) throws Exception {
        String source = new String(Files.readAllBytes(findRoot().resolve(
                "src/chiefnavigator/quest/MenelausTrial.java")),
                StandardCharsets.UTF_8);

        String configure = method(source,
                "private static void configureFriendlyFleet(");
        check(configure.contains(".PATROL_SYSTEM")
                        && configure.contains("system.getCenter(), 1000000f"),
                "Friendly drones must independently roam their home system");
        check(configure.contains("MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE, true")
                        && configure.contains("MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE, true"),
                "Recruitable drones must remain peaceful rather than an assault force");
        check(configure.contains("FRIENDLY_DRONE_PATROL_MARKER, true")
                        && configure.contains("MemFlags.MEMORY_KEY_NO_JUMP, true"),
                "Friendly patrol identity and home-system confinement must persist");
        for (String prohibited : new String[] {"wall", "setLocation(",
                "getVelocity()", "GO_TO_LOCATION", "ORBIT_AGGRESSIVE",
                "DEFEND_LOCATION", "INTERCEPT"}) {
            check(!configure.replace("Wall-centered", "").contains(prohibited),
                    "Friendly patrol configuration restored obsolete Wall movement: "
                            + prohibited);
        }
        check(!source.contains("FRIENDLY_PATROL_RETURN_RADIUS")
                        && !source.contains("FRIENDLY_PATROL_LEASH_RADIUS")
                        && !source.contains("travelingToWall"),
                "The retired Wall reinforcement ring/leash must not return");
        String population = method(source, "private static void ensureFriendlyArmy(");
        check(population.contains("MOTHERSHIP_NAMES.length * 2")
                        && population.contains("TroyArrivalScript.isFleetBusyForMutation(fleet)")
                        && population.contains("isPlayerControlledFleet(fleet)"),
                "Eight owned patrol slots must exclude player and busy fleets");
        int spent = population.indexOf(
                "if (memory().getBoolean(FRIENDLY_SPAWNED_PREFIX + i)) continue;");
        int latch = population.indexOf(
                "memory().set(FRIENDLY_SPAWNED_PREFIX + i, true);", spent);
        int create = population.indexOf("fleet = createFleet(", latch);
        check(spent >= 0 && latch > spent && create > latch,
                "A depleted patrol must not regenerate transferred or lost ships");
        String survivors = population.substring(population.indexOf(
                "if (fleet != null && !fleet.isEmpty())"), spent);
        check(!survivors.contains("addMember(")
                        && survivors.contains("configureFriendlyFleet(fleet, system)"),
                "Population refresh must preserve surviving patrol rosters");

        System.out.println(
                "Peaceful drone patrol regression checks passed (retired Wall leash stays absent).");
    }
}
