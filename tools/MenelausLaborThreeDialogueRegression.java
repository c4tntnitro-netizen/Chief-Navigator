package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for Menelaus's Labor-III conversation. */
public final class MenelausLaborThreeDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.replace("\r\n", "\n").contains(expected.replace("\r\n", "\n")),
                message + ": " + expected);
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String ink = Files.readString(
                ROOT.resolve("dialogue/menelaus_contact_labor_3.ink"),
                StandardCharsets.UTF_8);
        String reportInk = Files.readString(
                ROOT.resolve("dialogue/menelaus_labor_reports.ink"),
                StandardCharsets.UTF_8);

        contains(rules, "chiefNavigatorFobIthacaWallBriefing,",
                "Labor III must open with the authored briefing");
        contains(rules, "!$global.chief_navigator_menelaus_labor_three_briefed",
                "The briefing must be shown only once");
        contains(rules, "$global.chief_navigator_menelaus_labor_three_briefed = true",
                "Explicit acceptance must commit the briefing state");
        contains(rules, "FireAll ChiefNavigatorMenelausLaborThreeHubOptions",
                "Labor III needs a reusable hub");
        contains(rules, "$chiefNavigatorMenelausL3AskedHistory",
                "Wall history needs a local one-shot flag");
        contains(rules, "$chiefNavigatorMenelausL3AskedHelen",
                "Helen's third conversation needs a local one-shot flag");
        contains(rules, "ChiefNavigatorMenelausCMD repair\n"
                        + "FireAll ChiefNavigatorMenelausLaborThreeHub",
                "Labor-III repairs must return to the hub");
        contains(rules, "chief_navigator_fob_ithaca_upgrades",
                "The Labor-III hub must preserve the upgrade ledger");
        contains(ink, "=== menelaus_contact_labor_3_active ===",
                "Ink must retain the supplied Labor-III briefing");
        contains(ink, "=== menelaus_drifting_wall_history ===",
                "Ink must retain the Drifting Wall history");
        contains(ink, "=== menelaus_helen_3 ===",
                "Ink must retain the third Helen conversation");
        contains(ink, "=== menelaus_contact_labor_3_status ===",
                "Ink must define the reusable active-Labor topic");
        int wallBriefing = rules.indexOf(
                "chiefNavigatorFobIthacaWallBriefing,");
        int wallMenu = rules.indexOf(
                "chiefNavigatorFobIthacaWallMenu,", wallBriefing);
        check(wallBriefing >= 0 && wallMenu > wallBriefing,
                "Briefing and menu entry routes must both be present");
        int laborThreeRules = rules.indexOf(
                "chiefNavigatorMenelausLaborThreeBriefingCaptureOption,");
        int sensorRules = rules.indexOf(
                "chiefNavigatorFobIthacaSensorBriefing,", laborThreeRules);
        check(laborThreeRules >= 0 && sensorRules > laborThreeRules,
                "Labor-III rules must be bounded before Labor IV");
        check(!rules.substring(laborThreeRules, sensorRules)
                        .contains("chief_navigator_fob_ithaca_contacts")
                        && !rules.substring(laborThreeRules, sensorRules)
                                .contains("chief_navigator_fob_ithaca_storage"),
                "Labor III must return to the station instead of "
                        + "recursively opening station UI");
        check(!rules.substring(wallBriefing, wallMenu)
                        .contains("Alpha Odyssey")
                        && !rules.substring(laborThreeRules, sensorRules)
                                .contains("Alpha Odyssey")
                        && !ink.contains("Alpha Odyssey"),
                "Labor III must use the current Odyssey Expanse name");
        contains(rules,
                "chiefNavigatorFobIthacaWallReport,ChiefNavigatorFobIthaca,"
                        + "\"!ChiefNavigatorMenelausCMD stage early_wall\n"
                        + "ChiefNavigatorMenelausCMD stage wall_report\","
                        + "\"ChiefNavigatorMenelausCMD init\n"
                        + "ChiefNavigatorMenelausCMD report wall\n"
                        + "FireAll ChiefNavigatorHegemonyIthacaArrival\",,,",
                "Labor III must commit and advance without a debrief page");
        check(!rules.contains("chiefNavigatorMenelausReportWall,")
                        && !rules.contains("The Drifting Wall's shutdown "
                                + "handshake expands beside Menelaus's helm")
                        && !reportInk.contains("The Drifting Wall's shutdown "
                                + "handshake expands beside Menelaus's helm"),
                "The removed Labor-III debrief must not remain reachable "
                        + "or authored");

        System.out.println(
                "Menelaus Labor-III dialogue regression checks passed.");
    }
}
