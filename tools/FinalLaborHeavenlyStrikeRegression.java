package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Headless composition check for Labor V's mixed Heavenly Strike waves. */
public final class FinalLaborHeavenlyStrikeRegression {
    private static final int WAVE_COUNT = 3;
    private static final int WAVE_SIZE = 11;

    public static void main(String[] args) throws Exception {
        List<String> roster = OdysseyPredatorScript
                .buildFinalCenterRosterVariants();
        check(roster.size() == WAVE_COUNT * WAVE_SIZE,
                "Three waves must exactly triple the old eleven-unit "
                        + "supporting force");

        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            int start = wave * WAVE_SIZE;
            int end = start + WAVE_SIZE;
            int fabricators = 0;
            int hives = 0;
            int assaults = 0;
            int lines = 0;
            int overseers = 0;
            int skirmishers = 0;
            for (String variant : roster.subList(start, end)) {
                if (variant.contains("fabricator")) fabricators++;
                if (variant.contains("_hive_")) hives++;
                if (variant.contains("_assault_")) assaults++;
                if (variant.contains("_standoff_")) lines++;
                if (variant.contains("_overseer_")) overseers++;
                if (variant.contains("_skirmish_")) skirmishers++;
            }
            check(fabricators == 1,
                    "Every reinforcement wave needs one Fabricator");
            check(hives == 1,
                    "Every reinforcement wave needs one Hive");
            check(assaults == 2 && lines == 1 && overseers == 3
                            && skirmishers == 3,
                    "Every wave needs the full authored escort mixture");
            check(roster.get(start).contains("fabricator")
                            && roster.get(start + 1).contains("assault")
                            && roster.get(start + 2).contains("_hive_"),
                    "Fabricator, escort, and Hive must enter interleaved");
        }

        check(roster.stream().noneMatch(
                        variant -> variant.contains("scylla")),
                "The supporting roster must not duplicate Gautama");

        for (int wave = 0; wave < WAVE_COUNT; wave++) {
            for (int slot = 0; slot < WAVE_SIZE; slot++) {
                String memberId = OdysseyPredatorScript
                        .finalCenterWaveMemberId(wave, slot);
                check(OdysseyPredatorScript.getFinalCenterWaveIndex(
                                memberId) == wave,
                        "Exact campaign-member IDs must retain their wave");
            }
        }
        check(OdysseyPredatorScript.getFinalCenterWaveIndex(
                        "chief_navigator_final_labor_wave_3_0") == -1,
                "Out-of-range wave IDs must fail closed");
        check(OdysseyPredatorScript.getFinalCenterWaveIndex(
                        "chief_navigator_final_labor_wave_1_bad") == -1,
                "Malformed wave IDs must fail closed");

        check(!IthacaFinalLaborBattleCreationPlugin
                        .shouldReleaseFinalCenterWave(7.99f, 0),
                "A cleared cohort must still respect the minimum cadence");
        check(IthacaFinalLaborBattleCreationPlugin
                        .shouldReleaseFinalCenterWave(8f, 4),
                "Four surviving authored ships must release the next wave");
        check(!IthacaFinalLaborBattleCreationPlugin
                        .shouldReleaseFinalCenterWave(49.99f, 5),
                "A healthy cohort must not cause an early entity spike");
        check(IthacaFinalLaborBattleCreationPlugin
                        .shouldReleaseFinalCenterWave(50f, 99),
                "The hard cadence ceiling must prevent a stalled wave");
        String battlePlugin = Files.readString(
                Path.of("src/chiefnavigator/quest/"
                        + "IthacaFinalLaborBattleCreationPlugin.java"),
                StandardCharsets.UTF_8);
        check(battlePlugin.contains("hideAuthoredWallModules(context)"),
                "Labor V must exclude authored ring Wall modules");
        check(battlePlugin.contains(
                        "IthacaSectionEncounter.isSectionStation(source)"),
                "Only owned Ithaca section stations may be excluded");
        check(battlePlugin.contains(
                        "restoreMothballSnapshots(excludedWallModules)"),
                "Excluded campaign Wall members must be restored exactly");
        System.out.println("PASS: Heavenly Strike has three interleaved "
                + "Fabricator/Hive/escort reinforcement waves with bounded "
                + "staged deployment and no pulled-in ring Wall module.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
