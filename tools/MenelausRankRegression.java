package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for Menelaus's displayed rank and save migration. */
public final class MenelausRankRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        String source = Files.readString(ROOT.resolve(
                "src/chiefnavigator/quest/MenelausTrial.java"),
                StandardCharsets.UTF_8);
        String faction = Files.readString(ROOT.resolve(
                "data/world/factions/task_force_spartan.faction"),
                StandardCharsets.UTF_8);

        check(source.contains("STRATEGOS_RANK_ID =\n"
                        + "            \"chief_navigator_strategos\""),
                "Menelaus must own a dedicated Strategos rank id");
        check(source.contains("person.setRankId(STRATEGOS_RANK_ID);\n"
                        + "        person.setPostId(STRATEGOS_POST_ID);\n"
                        + "        return person;"),
                "Every lookup must migrate saved Menelaus rank and post");
        check(!source.contains("person.setRankId(Ranks.SPACE_ADMIRAL)"),
                "Menelaus must not fall back to the Admiral display rank");
        check(!source.contains("person.setPostId(Ranks.POST_BASE_COMMANDER)"),
                "Menelaus must not retain the administrative base post");
        check(faction.contains("\"chief_navigator_strategos\""
                        + ":{\"name\":\"Strategos\"}"),
                "Task Force Spartan must resolve the rank as Strategos");
        check(faction.contains("\"chief_navigator_strategic_intelligence\""
                        + ":{\"name\":\"Strategic Intelligence\"}"),
                "Task Force Spartan must resolve Menelaus's clean post");

        System.out.println(
                "Menelaus Strategos identity regression passed.");
    }
}
