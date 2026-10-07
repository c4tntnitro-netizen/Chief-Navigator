package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for the Foxtrot Terminus patrol cordon. */
public final class FoxtrotTerminusPatrolRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(
                ROOT.resolve("src/chiefnavigator/quest/"
                        + "OdysseyStrandedFleetsScript.java"),
                StandardCharsets.UTF_8);

        contains(source, "HEGEMONY_TROY_PATROL_ID + \"_2\"",
                "The cordon must have a second patrol");
        contains(source, "HEGEMONY_TROY_PATROL_ID + \"_3\"",
                "The cordon must have a third patrol");
        contains(source, "for (int i = 0; i < "
                        + "HEGEMONY_TROY_PATROL_IDS.length; i++)",
                "Maintenance must reconcile every patrol slot");
        contains(source,
                "memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);",
                "Every terminus patrol must be personally hostile");
        contains(source,
                "memory.set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);",
                "Every terminus patrol must engage the player");
        contains(source,
                "memory.unset(MemFlags.MEMORY_KEY_NO_REP_IMPACT);",
                "Patrol combat must affect its independent expedition faction");
        contains(source,
                "memory.set(MemFlags.MEMORY_KEY_NO_JUMP, true);",
                "The patrols must remain in Alpha Odyssey");

        System.out.println(
                "Foxtrot Terminus patrol regression checks passed.");
    }
}
