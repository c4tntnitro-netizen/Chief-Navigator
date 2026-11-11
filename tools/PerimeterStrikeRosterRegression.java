package chiefnavigator.quest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/** Exact themed compositions, variant integrity and scoped deployment costs. */
public final class PerimeterStrikeRosterRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        String[] roles = {"_fabricator_", "_hive_", "_overseer_", "_standoff_", "_assault_", "_skirmish_"};
        int[] cost = {20, 22, 8, 20, 10, 4};
        for (IthacaSectionEncounter.Section section : IthacaSectionEncounter.Section.values()) {
            List<String> roster = OdysseyPredatorScript.buildPerimeterStrikeRosterVariants(section);
            check(roster.equals(OdysseyPredatorScript.buildPerimeterStrikeRosterVariants(section)),
                    "Composition must be deterministic");
            int[] counts = new int[roles.length];
            int dp = 0;
            for (String variant : roster) {
                check(Files.isRegularFile(Path.of("data/variants", variant + ".variant")),
                        "Every selected variant must exist: " + variant);
                check(!variant.contains("scylla"), "No Gautama in a perimeter fight");
                for (int role = 0; role < roles.length; role++) {
                    if (variant.contains(roles[role])) { counts[role]++; dp += cost[role]; }
                }
            }
            for (int count : counts) check(count > 0, "Every themed Strike must retain mixed support");
            if (section != IthacaSectionEncounter.Section.EAST) {
                check(dp >= 266 && dp <= 294, "Scylla and Siren retain their original deployment budget");
            }
            switch (section) {
                case NORTH:
                    check(OdysseyPredatorScript.perimeterStrikeName(section).equals("Scylla Strike"), "North name");
                    check(Arrays.equals(counts, new int[]{1, 9, 1, 2, 1, 3}), "Scylla composition stays unchanged");
                    check(counts[1] > roster.size() / 2 && roster.get(0).contains("_hive_"), "Scylla must be mostly Hives");
                    break;
                case SOUTH:
                    check(OdysseyPredatorScript.perimeterStrikeName(section).equals("Siren Strike"), "South name");
                    check(Arrays.equals(counts, new int[]{1, 1, 2, 8, 2, 10}), "Siren composition stays unchanged");
                    check(counts[3] + counts[5] > roster.size() / 2 && counts[3] >= 8 && counts[5] >= 10,
                            "Siren must emphasize Line and Skirmish scouts");
                    break;
                case EAST:
                    check(OdysseyPredatorScript.perimeterStrikeName(section).equals("Cyclops Strike"), "East name");
                    check(Arrays.equals(counts, new int[]{5, 1, 2, 2, 2, 4}),
                            "Cyclops has exactly five Fabricators and unchanged support roles");
                    check(roster.size() == 16 && dp == 214, "Reduced Cyclops roster has 16 members and 214 DP");
                    for (int role = 1; role < counts.length; role++) check(counts[0] > counts[role], "Fabricators must dominate Cyclops");
                    check(roster.get(0).contains("_fabricator_"), "Cyclops Fabricator flagship");
                    break;
            }
        }
        String source = Files.readString(Path.of("src/chiefnavigator/quest/OdysseyPredatorScript.java"));
        check(source.contains("id, THIRD_STRIKE, section")
                        && source.contains("fleet.setName(perimeterStrikeName(section))")
                        && source.contains("invasion.setName(perimeterStrikeName(section))"),
                "Wire themed creation and retain strike names through both phases");
        System.out.println("PASS: Scylla and Siren compositions/budgets unchanged; Cyclops has exactly five Fabricators "
                + "with unchanged support, 16 members and 214 DP; deterministic mixed normal variants.");
    }
}
