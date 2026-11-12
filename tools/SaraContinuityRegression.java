package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for Sara's Kshaya death continuity. */
public final class SaraContinuityRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(
            String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String habitatCommand = Files.readString(
                ROOT.resolve("src/data/campaign/rulecmd/"
                        + "ChiefNavigatorAviciHabitatCMD.java"),
                StandardCharsets.UTF_8);
        String habitatState = Files.readString(
                ROOT.resolve("src/chiefnavigator/quest/"
                        + "AviciHabitatEncounter.java"),
                StandardCharsets.UTF_8);
        String plugin = Files.readString(
                ROOT.resolve("src/chiefnavigator/ChiefNavigatorModPlugin.java"),
                StandardCharsets.UTF_8);
        String habitatInk = Files.readString(
                ROOT.resolve("dialogue/avici_habitat.ink"),
                StandardCharsets.UTF_8);
        String firstContactInk = Files.readString(
                ROOT.resolve("dialogue/menelaus_first_contact.ink"),
                StandardCharsets.UTF_8);

        contains(habitatState,
                "$chief_navigator_sara_dead",
                "Sara's death needs a stable global memory key");
        contains(habitatCommand,
                "if (\"light\".equals(outcome))",
                "Only the fatal light rescue should enter the death branch");
        contains(habitatCommand,
                "AviciHabitatEncounter.markSaraDead();",
                "The fatal rescue must record Sara's death");
        contains(plugin,
                "AviciHabitatEncounter.reconcileSaraDeathFlag();",
                "Old saves must derive the flag from serialized Kshaya state");
        contains(habitatState,
                ".getString(RESCUE_OUTCOME)",
                "The migration must inspect the recorded rescue outcome");

        contains(rules,
                "chiefNavigatorMenelausFirstSinniInsistsSaraOption,"
                        + "ChiefNavigatorMenelausFirstSinniInsistsOptions,"
                        + "!$global.chief_navigator_sara_dead",
                "The Sara-specific shuttle choice must require her survival");
        contains(rules,
                "chiefNavigatorMenelausFirstSinniGo,"
                        + "ChiefNavigatorMenelausFirstSinniGo,"
                        + "!$global.chief_navigator_sara_dead",
                "Sara's station-party exchange must require her survival");
        contains(rules,
                "chiefNavigatorMenelausFirstAbomination,"
                        + "ChiefNavigatorMenelausFirstAbomination,"
                        + "!$global.chief_navigator_sara_dead",
                "Sara's Menelaus reaction must require her survival");
        contains(rules,
                "chiefNavigatorVossRankContinuePage9,DialogOptionSelected,"
                        + "\"$option == chief_navigator_auto_page_"
                        + "chief_navigator_voss_rank_continue_9\n"
                        + "!$global.chief_navigator_sara_dead\"",
                "Sara's Voss aside must require her survival");

        contains(rules,
                "chiefNavigatorMenelausFirstSinniGoWithoutSara,"
                        + "ChiefNavigatorMenelausFirstSinniGo,"
                        + "$global.chief_navigator_sara_dead",
                "The shuttle scene needs a post-Kshaya continuation");
        contains(rules,
                "chiefNavigatorMenelausFirstAbominationWithoutSara,"
                        + "ChiefNavigatorMenelausFirstAbomination,"
                        + "$global.chief_navigator_sara_dead",
                "Menelaus's rebuke needs a post-Kshaya continuation");
        contains(rules,
                "chiefNavigatorVossRankContinuePage9WithoutSara,",
                "The Voss scene needs a post-Kshaya continuation");
        check(!rules.contains("$chief_navigator_sara_dead"),
                "Sara's sector-memory flag must never be read from local scope");

        int heavy = habitatInk.indexOf("=== rescue_fight_heavy ===");
        int light = habitatInk.indexOf("=== rescue_fight_light ===", heavy);
        int dead = habitatInk.indexOf("=== rescue_sara_dead ===", light);
        check(heavy >= 0 && light > heavy && dead > light,
                "Kshaya rescue branches must remain ordered");
        check(!habitatInk.substring(heavy, light).contains("~ sara_dead = true"),
                "The heavy rescue must leave Sara alive");
        contains(habitatInk.substring(light, dead),
                "~ sara_dead = true",
                "The fatal light rescue must mark Sara dead in the Ink source");
        contains(firstContactInk, "{ sara_dead:",
                "The first-contact Ink source must mirror the runtime branch");

        System.out.println("Sara continuity regression checks passed.");
    }
}
