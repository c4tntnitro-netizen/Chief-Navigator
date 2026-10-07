package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static contract checks for Menelaus's Labor-IV conversation. */
public final class MenelausLaborFourDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    private static Map<String, List<String>> rows(String csv) {
        Map<String, List<String>> result = new HashMap<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append(c);
                    i++;
                } else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\n' || c == '\r')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (c != ',') {
                    if (!row.get(0).isBlank() && !row.get(0).equals("id")) {
                        check(row.size() >= 6, "Missing authored CSV fields in " + row.get(0));
                        check(result.put(row.get(0), List.copyOf(row)) == null,
                                "Duplicate authored rule " + row.get(0));
                    }
                    row.clear();
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty(), "Authored CSV must end with balanced fields");
        return result;
    }

    private static void suicideMissionChoice(String rules) {
        Map<String, List<String>> rows = rows(rules);
        String sentence = "So this is some suicide mission.";
        String selectedId = "chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_why_2";
        List<String> preceding = rows.get("chiefNavigatorMenelausLaborFourSinniWhy");
        List<String> reply = rows.get("chiefNavigatorMenelausLaborFourSinniWhyPage2");
        check(preceding != null && reply != null, "Sinni's Labor-IV motivation exchange must remain intact");
        check(preceding.get(5).equals("0:" + selectedId + ":\"" + sentence + "\""),
                "The suicide-mission statement must replace Continue as a quoted player choice");
        check(!preceding.get(4).contains(sentence) && !reply.get(4).contains(sentence),
                "The selected statement must not echo in the preceding or reply prose");
        check(reply.get(1).equals("DialogOptionSelected")
                        && reply.get(2).equals("$option == " + selectedId),
                "The existing choice ID must lead immediately to Menelaus's original Page2 reply");
        check(reply.get(3).isBlank()
                        && reply.get(4).startsWith("\"No.\" Menelaus raises the helm of his avatar."),
                "Menelaus's immediate No reply must remain unchanged, without a new dispatch or effect");
        long optionOccurrences = rows.values().stream()
                .flatMap(row -> row.get(5).lines())
                .filter(line -> line.contains(sentence)).count();
        check(optionOccurrences == 1, "The suicide-mission player line must appear in exactly one choice");
        check(rows.values().stream().noneMatch(row -> row.get(4).contains(sentence)),
                "The suicide-mission statement belongs only in the selected player's transcript line");
    }

    private static void singleVeryWell(String rules) {
        Map<String, List<String>> rows = rows(rules);
        List<String> first = rows.get("chiefNavigatorMenelausLaborFourSinniChoicePage3");
        List<String> closing = rows.get("chiefNavigatorMenelausLaborFourSinniEnd");
        check(first != null && closing != null, "The Labor-IV Sinni exchange must retain its closing handoff");
        check(first.get(4).equals("\"Very well.\"")
                        && first.get(3).equals("FireAll ChiefNavigatorMenelausLaborFourSinniEnd"),
                "Keep Menelaus's first Very well and its direct closing dispatch");
        check(!closing.get(4).contains("Very well."),
                "The common closing page must not repeat Very well");
        check(closing.get(4).contains("\"That is your fourth Labor.\"")
                        && closing.get(5).equals("0:chief_navigator_menelaus_labor_four_accept:\"I accept.\""),
                "The fourth-Labor declaration and existing acceptance choice must remain intact");
    }

    private static void sinniIdentityOnEveryRoute(String rules) {
        Map<String, List<String>> rows = rows(rules);
        for (String branch : List.of("Refuse", "Choice")) {
            List<String> direct = rows.get("chiefNavigatorMenelausLaborFourSinni" + branch);
            List<String> after = rows.get("chiefNavigatorMenelausLaborFourSinni" + branch + "AfterHelen");
            check(direct != null && after != null, "Both entry contexts must retain the " + branch + " route");
            contains(direct.get(2), "!$chiefNavigatorMenelausL4SinniHelenStated",
                    "The new declaration belongs only on direct routes");
            check(direct.get(4).split("I am not Helen", -1).length == 2,
                    "Sinni must clearly state her identity once on the direct " + branch + " route");
            contains(after.get(2), "$chiefNavigatorMenelausL4SinniHelenStated",
                    "Why-branch follow-ups must use the already-stated context");
            check(!after.get(4).contains("I am not Helen")
                            && after.get(3).equals(direct.get(3))
                            && after.get(5).equals(direct.get(5)),
                    "Preserve the original continuation without a second declaration");
        }
        List<String> original = rows.get("chiefNavigatorMenelausLaborFourSinniWhyPage3");
        check(original.get(3).isBlank()
                        && original.get(4).replace("\r\n", "\n").equals(
                                "\"Lord Strategos.\" Sinni bows, lowering her head. \"I am not Helen.\"\n\n"
                                        + "Menelaus does not answer the name.")
                        && original.get(5).replace("\r\n", "\n").equals(
                                "0:chief_navigator_menelaus_labor_four_sinni_refuse:\"Request denied. She's coming with us.\"\n"
                                        + "10:chief_navigator_menelaus_labor_four_sinni_choice:\"Sinni?\""),
                "Do not change the existing Why-branch reveal page or its two choices");
        check(rows.get("chiefNavigatorMenelausLaborFourUnderstoodPage3").get(3).equals(
                        "$chiefNavigatorMenelausL4SinniHelenStated = false")
                        && rows.get("chiefNavigatorMenelausLaborFourSinniWhy").get(3).equals(
                                "$chiefNavigatorMenelausL4SinniHelenStated = true"),
                "Reset presentation context on each entry and remember the Why-route reveal locally");
    }

    private static void starvingFeedstockExplanation(String rules) {
        List<String> row = rows(rules).get("chiefNavigatorMenelausLaborFourStarving");
        check(row != null, "The Labor-IV Starving Threat explanation must remain available");
        String text = row.get(4);
        check(!text.contains("The Starving Threat is simply Threat deprived of feedstock."),
                "Remove the redundant opening feedstock definition");
        check(text.startsWith("The projection changes.")
                        && text.contains("\"It is Threat machinery deprived of feedstock for centuries.\""),
                "Begin with the projection change and retain the later centuries-long explanation");
        check(text.split("feedstock", -1).length == 2,
                "The Starving Threat explanation must mention feedstock only once");
        check(row.get(3).replace("\r\n", "\n").equals(
                        "$chiefNavigatorMenelausL4AskedThreat = true 0\n"
                                + "FireAll ChiefNavigatorMenelausLaborFourHub"),
                "Preserve the existing asked flag and direct Labor-IV hub return");
    }

    private static void sensorReportPrelude(String rules) throws Exception {
        Map<String, List<String>> rows = rows(rules);
        List<String> report = rows.get("chiefNavigatorFobIthacaSensorReport");
        check(report != null, "The completed sensor Labor must retain its Menelaus report");
        String prelude = "When you get back, you find Menelaus with your sensor reports open.\n\n"
                + "Is it your imagination, or... does the AI's avatar look grim?\n\n";
        String original = "Menelaus opens the sensor package's final transmission. The Devoured Ring appears "
                + "as a lattice of consumed mass, failed topology models, and centuries of Starving Threat traffic.\n\n"
                + "\"The package completed its work before deleting itself. The Threat have been trying to reconstruct "
                + "a Janus Device from the ring they consumed. They failed because they possess only one side of the topology.\"\n\n"
                + "\"And the closest intact Gate left is...\"\n\n"
                + "Menelaus stares at the Gate that Task Force Spartan guards.\n\nYou realize.";
        check(report.get(4).replace("\r\n", "\n").equals(prelude + original),
                "The return and grim-avatar paragraphs must precede the unchanged sensor report in order");
        check(report.get(1).equals("ChiefNavigatorFobIthaca")
                        && report.get(2).replace("\r\n", "\n").equals(
                                "!ChiefNavigatorMenelausCMD stage early_wall\n"
                                        + "ChiefNavigatorMenelausCMD stage sensor_report"),
                "The prelude belongs only to the pending sensor-report root, not the ordinary Labor-IV hub");
        check(report.get(3).equals("ChiefNavigatorMenelausCMD init")
                        && report.get(5).equals("0:chief_navigator_menelaus_labor_5_realization_2:"
                                + "\"If they scan Ithaca's Gate, then—.\""),
                "Preserve the report's portrait initialization and existing realization choice");
        for (List<String> row : rows.values()) {
            if (row == report) continue;
            check(!row.get(4).contains("When you get back, you find Menelaus with your sensor reports open.")
                            && !row.get(4).contains("does the AI's avatar look grim?"),
                    "The report prelude must not repeat in another dialogue root: " + row.get(0));
        }
        String mirror = Files.readString(ROOT.resolve("dialogue/menelaus_labor_reports.ink"),
                StandardCharsets.UTF_8).replace("\r\n", "\n");
        check(mirror.contains("=== menelaus_labor_iv_report ===\n" + prelude
                        + original.substring(0, original.indexOf("\n\n"))),
                "The Labor-IV authoring mirror must retain the same ordered prelude before the original report");
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);

        int briefing = rules.indexOf(
                "chiefNavigatorFobIthacaSensorBriefing,");
        int finalOffer = rules.indexOf(
                "chiefNavigatorFobIthacaFinalOffer,", briefing);
        check(briefing >= 0 && finalOffer > briefing,
                "Labor-IV dialogue must be bounded before the final Labor");
        String labor = rules.substring(briefing, finalOffer);

        contains(labor,
                "!$global.chief_navigator_menelaus_labor_four_briefed",
                "Labor IV needs a one-time briefing gate");
        contains(labor,
                "$global.chief_navigator_menelaus_labor_four_briefed = true",
                "Explicit acceptance must commit the briefing state");
        contains(labor, "FireAll ChiefNavigatorMenelausLaborFourHubOptions",
                "Labor IV needs a reusable hub");
        contains(labor, "Mara Observation Array",
                "Dialogue must name the implemented deployment target");
        contains(labor, "while no hostile fleet is tracking you",
                "Dialogue must explain the implemented stealth gate");
        contains(labor, "TTS Penumbra",
                "Dialogue must preserve the optional Doom lead");
        contains(labor, "chief_navigator_menelaus_labor_four_sinni_why",
                "The Sinni motivation route must be selectable");
        contains(labor, "I am not Helen",
                "The Sinni/Helen reveal must remain intact");
        contains(labor, "$chiefNavigatorMenelausL4AskedReach",
                "Reach history needs a local one-shot flag");
        contains(labor, "$chiefNavigatorMenelausL4AskedThreat",
                "Threat history needs a local one-shot flag");
        contains(labor, "$chiefNavigatorMenelausL4AskedHelen",
                "Helen's fourth conversation needs a local one-shot flag");
        contains(labor, "Fabrication Rights Management",
                "Threat origin lore must remain intact");
        contains(labor, "ChiefNavigatorMenelausCMD repair\n"
                        + "FireAll ChiefNavigatorMenelausLaborFourHub",
                "Labor-IV repairs must return to the hub");
        check(!labor.contains("chief_navigator_fob_ithaca_contacts")
                        && !labor.contains(
                                "chief_navigator_fob_ithaca_storage"),
                "Labor IV must return to the station instead of "
                        + "recursively opening station UI");

        suicideMissionChoice(rules);
        singleVeryWell(rules);
        sinniIdentityOnEveryRoute(rules);
        starvingFeedstockExplanation(rules);
        sensorReportPrelude(rules);

        System.out.println(
                "Menelaus Labor-IV dialogue regression checks passed.");
    }
}
