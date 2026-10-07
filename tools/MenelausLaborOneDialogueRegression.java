package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Static contract checks for Menelaus's reusable pre-Labor-I conversation. */
public final class MenelausLaborOneDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(
            String text, String expected, String message) {
        check(text.contains(expected), message + ": " + expected);
    }

    private static int count(String text, String needle) {
        int matches = 0;
        int from = 0;
        while ((from = text.indexOf(needle, from)) >= 0) {
            matches++;
            from += needle.length();
        }
        return matches;
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String droneNotice = "You can command recovered FOB Ithaca drones "
                + "without the Automated Ships skill.";
        int reportStart = rules.indexOf("chiefNavigatorFobIthacaMothershipsReport,");
        int reportEnd = rules.indexOf("chiefNavigatorFobIthacaResearchReport,", reportStart);
        check(reportStart >= 0 && reportEnd > reportStart,
                "Labor-I completion report must remain available");
        String report = rules.substring(reportStart, reportEnd);
        contains(report, "SetTextHighlightColors hColor",
                "Drone authorization notice must use the standard highlight color");
        contains(report, "SetTextHighlights \"\"" + droneNotice + "\"\"",
                "The full authorization notice must be highlighted");
        contains(report.replace("\r\n", "\n"),
                "\"\"Labor I is complete.\"\"\n\n" + droneNotice,
                "Authorization notice must follow completion as the final paragraph");
        String ink = Files.readString(
                ROOT.resolve("dialogue/menelaus_contact_labor_1.ink"),
                StandardCharsets.UTF_8);
        String firstContactInk = Files.readString(
                ROOT.resolve("dialogue/menelaus_first_contact.ink"),
                StandardCharsets.UTF_8);
        String contactInteraction = Files.readString(
                ROOT.resolve("src/chiefnavigator/quest/"
                        + "FobIthacaContactInteraction.java"),
                StandardCharsets.UTF_8);
        String stationInteraction = Files.readString(
                ROOT.resolve("src/chiefnavigator/quest/"
                        + "FobIthacaStationInteraction.java"),
                StandardCharsets.UTF_8);
        String menelausCommand = Files.readString(
                ROOT.resolve("src/data/campaign/rulecmd/"
                        + "ChiefNavigatorMenelausCMD.java"),
                StandardCharsets.UTF_8);

        contains(rules, "chiefNavigatorMenelausLaborOneHub,",
                "Runtime must retain the reusable Labor-I hub");
        contains(rules, "FireAll ChiefNavigatorMenelausLaborOneHubOptions",
                "Both pre-acceptance and active contacts need hub options");
        check(!rules.contains("$chiefNavigatorMenelausL1AskedCollapse"),
                "Collapse question must remain repeatable");
        check(!rules.contains("$chiefNavigatorMenelausL1AskedHelen"),
                "Helen question and follow-up must remain repeatable");
        check(!rules.contains("$chiefNavigatorMenelausL1AskedName"),
                "Name question must remain repeatable");
        contains(rules, "$chiefNavigatorMenelausL1AskedDrone",
                "Drone question needs a local one-shot flag");

        contains(rules,
                "chief_navigator_menelaus_first_need_accept_direct:\"\"Understood. Show me the Labors.\"\"",
                "First contact must retain its authored closing choice");
        int sinniAssume = rules.indexOf(
                "chiefNavigatorMenelausFirstSinniAssume,"
                        + "DialogOptionSelected,$option == "
                        + "chief_navigator_menelaus_first_sinni_assume,");
        int sinniReady = rules.indexOf(
                "chiefNavigatorMenelausFirstSinniReady,", sinniAssume);
        check(sinniAssume >= 0 && sinniReady > sinniAssume,
                "Sinni's affirmative shuttle branch must be bounded");
        String sinniAssumeBranch = rules.substring(sinniAssume, sinniReady);
        contains(sinniAssumeBranch,
                "FireAll ChiefNavigatorMenelausFirstSinniGo",
                "Inviting Sinni must proceed directly to the station party");
        check(!sinniAssumeBranch.contains("Object? You?"),
                "Inviting Sinni must not offer an objection afterward");
        check(!sinniAssumeBranch.contains(
                        "chief_navigator_menelaus_first_sinni_assume_object"),
                "The affirmative branch must not route into her insistence");
        contains(firstContactInk,
                "=== fob_ithaca_sinni_assume ===",
                "The Ink source must retain Sinni's affirmative branch");
        check(!firstContactInk.contains("[\"Object? You?\"]"),
                "The Ink source must not put an objection after inviting "
                        + "Sinni");
        int introFinish = rules.indexOf(
                "chiefNavigatorMenelausFirstFinishOption,"
                        + "DialogOptionSelected,$option == "
                        + "chief_navigator_menelaus_first_finish,");
        int laborThreeStart = rules.indexOf(
                "chiefNavigatorFobIthacaWallBriefing,", introFinish);
        check(introFinish >= 0 && laborThreeStart > introFinish,
                "The station introduction ending must be bounded");
        String introEnding = rules.substring(introFinish, laborThreeStart);
        contains(introEnding,
                "chiefNavigatorMenelausFirstFinishOption,"
                        + "DialogOptionSelected,$option == "
                        + "chief_navigator_menelaus_first_finish,",
                "The terminal Continue must have a DialogOptionSelected "
                        + "rule for the stock rule-based interaction");
        contains(introEnding,
                "$global.chief_navigator_menelaus_intro_shown_v2 = true",
                "First station contact must commit its one-time flag");
        contains(introEnding,
                "ChiefNavigatorMenelausCMD finishIntro",
                "The terminal Continue must transfer to the station UI");
        check(!introEnding.contains("EndConversation"),
                "The terminal rule must not depend on the generic dialog "
                        + "closer");
        check(!introEnding.contains(
                        "FireAll ChiefNavigatorMenelausFirstFinish"),
                "The terminal Continue must not fire a contentless rule and "
                        + "leave its option installed");
        check(!introEnding.contains(
                        "FireAll ChiefNavigatorMenelausLaborOneHub"),
                "The station introduction must not open repeatable topics");
        check(!contactInteraction.contains("FIRST_CONTACT_FINISH_OPTION"),
                "The contact-only plugin must not pretend to own the station "
                        + "introduction's terminal option");
        contains(menelausCommand,
                "\"finishIntro\".equals(action)",
                "Menelaus's rules command must implement the terminal UI "
                        + "transfer");
        contains(menelausCommand,
                "FobIthacaStationInteraction.returnToStation(dialog);",
                "The terminal command must initialize the station plugin");
        contains(stationInteraction,
                "public static void returnToStation(",
                "The rules command must be able to open the station UI");
        int helenIntroRow = rules.indexOf(
                "chiefNavigatorMenelausFirstSinniHelen,DialogOptionSelected");
        int helenIntroPage2Row = rules.indexOf(
                "chiefNavigatorMenelausFirstSinniHelenPage2,", helenIntroRow);
        check(helenIntroRow >= 0 && helenIntroPage2Row > helenIntroRow,
                "Optional Helen introduction row must be bounded");
        String helenIntro = rules.substring(helenIntroRow, helenIntroPage2Row);
        contains(helenIntro,
                "$option == chief_navigator_menelaus_labor_one_helen",
                "The Labor-I Helen option must launch the moved introduction");
        check(count(rules, "$option == chief_navigator_menelaus_labor_one_helen\r\n")
                        + count(rules, "$option == chief_navigator_menelaus_labor_one_helen\n")
                        + count(rules, "$option == chief_navigator_menelaus_labor_one_helen,") == 1,
                "Exactly one handler must own the repeatable Labor-I Helen option");
        check(!rules.contains("chief_navigator_menelaus_helen_intro_shown_v1"),
                "First and repeat visits must never split on an introductory-Helen flag");
        int helenEndRow = rules.indexOf(
                "chiefNavigatorMenelausFirstHelenEnd,");
        int firstAcceptRow = rules.indexOf(
                "chiefNavigatorMenelausFirstNeedAccept,", helenEndRow);
        check(helenEndRow >= 0 && firstAcceptRow > helenEndRow,
                "Moved Helen ending row must be bounded");
        String helenEnd = rules.substring(helenEndRow, firstAcceptRow);
        contains(helenEnd, "FireAll ChiefNavigatorMenelausLaborOneHelenRecord",
                "Both player responses must proceed directly into Helen's record");
        check(!helenEnd.contains("ChiefNavigatorMenelausLaborOneHub"),
                "There must be no return-to-hub interruption before the record");
        int helenRecordRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneHelen,ChiefNavigatorMenelausLaborOneHelenRecord");
        int helenRecordPage2Row = rules.indexOf(
                "chiefNavigatorMenelausLaborOneHelenPage2,", helenRecordRow);
        check(helenRecordRow >= 0 && helenRecordPage2Row > helenRecordRow,
                "Repeatable Helen record row must be bounded");
        String helenRecord = rules.substring(helenRecordRow, helenRecordPage2Row);
        contains(helenRecord, "Lieutenant Commander Helen Argyros.",
                "The unified answer must open the existing service record");
        check(!helenRecord.contains("About Helen, please"),
                "Do not repeat Sinni's request when opening the record");
        String helenOwe = rules.substring(
                rules.indexOf("chiefNavigatorMenelausFirstHelenOwe,"), helenEndRow);
        contains(helenOwe, "\"\"Very well.\"\"", "Menelaus's revised assent");
        check(!helenOwe.contains("I am aware") && !rules.contains("When circumstances permit, Chief Navigator. I will answer."),
                "Answer now instead of promising a later scene");
        int helenRecordEnd = rules.indexOf("chiefNavigatorMenelausLaborOneHelenCollapseOption,", helenRecordRow);
        check(!rules.substring(helenRecordRow, helenRecordEnd).contains("You knew her?"),
                "Do not repeat the question already answered in the introduction");

        contains(rules, "chief_navigator_menelaus_labor_one_accept:\"\"I accept.\"\"",
                "Labor I needs an explicit final acceptance option");
        int acceptRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneAccept,DialogOptionSelected");
        int nextRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneActiveBack,", acceptRow);
        check(acceptRow >= 0 && nextRow > acceptRow,
                "Labor-I acceptance row must be bounded");
        String acceptance = rules.substring(acceptRow, nextRow);
        contains(acceptance, "ChiefNavigatorMenelausCMD accept",
                "Only the explicit acceptance route may start Labor I");
        contains(acceptance, "The four routes settle onto your campaign map",
                "Coordinates must be granted after acceptance");
        contains(acceptance, "IFF authentication package enters",
                "IFF must be granted after acceptance");

        contains(rules, "the Odyssey Expanse",
                "Runtime dialogue must use the current system name");
        contains(rules, "Odyssey Sector",
                "Runtime dialogue must use the current regional name");
        contains(rules, "AD-87 Defense Module",
                "Labor explanation must identify the Drifting Wall");
        contains(rules, "sealed command partitions",
                "Labor objective must match the implemented encounter");
        contains(rules, "Their escorts are secondary",
                "Dialogue must not require escort destruction");
        contains(rules, "Do not scan the gate housed inside FOB Ithaca",
                "Menelaus's gate prohibition must remain explicit");
        contains(rules, "two hundred and eighty-six cycles ago",
                "Collapse history must preserve the authored date");
        contains(rules, "Lieutenant Commander Helen Argyros",
                "Helen's authored branch must be present");
        contains(rules, "chief_navigator_menelaus_labor_one_name_funny",
                "First converging name response needs a unique option ID");
        contains(rules, "chief_navigator_menelaus_labor_one_name_look",
                "Second converging name response needs a unique option ID");
        contains(rules, "chief_navigator_menelaus_labor_one_repair",
                "Labor-I repairs need a route back to the reusable hub");
        int droneRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneDrone,DialogOptionSelected");
        int understoodRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneUnderstood,", droneRow);
        check(droneRow >= 0 && understoodRow > droneRow,
                "Labor-I drone answer row must be bounded");
        String droneAnswer = rules.substring(droneRow, understoodRow);
        contains(droneAnswer,
                "FireAll ChiefNavigatorMenelausLaborOneBriefingOptions",
                "Drone answer must return directly to briefing choices");
        check(!droneAnswer.contains(
                        "FireAll ChiefNavigatorMenelausLaborOneHub"),
                "Drone answer must not force the briefing to replay");
        int repairRow = rules.indexOf(
                "chiefNavigatorMenelausLaborOneRepair,DialogOptionSelected");
        int sensorRow = rules.indexOf(
                "chiefNavigatorFobIthacaSensorBriefing,", repairRow);
        check(repairRow >= 0 && sensorRow > repairRow,
                "Labor-I repair row must be bounded");
        String repair = rules.substring(repairRow, sensorRow);
        contains(repair, "ChiefNavigatorMenelausCMD repair",
                "Labor-I repair route must repair the fleet");
        contains(repair, "FireAll ChiefNavigatorMenelausLaborOneHub",
                "Labor-I repair route must return to its dialogue hub");

        contains(ink, "=== menelaus_contact_labor_1_active ===",
                "Ink must record the supplied reusable hub");
        contains(ink, "=== menelaus_labor_1 ===",
                "Ink must record the Labor explanation");
        contains(ink, "=== menelaus_collapse_1 ===",
                "Ink must record the Collapse branch");
        contains(ink, "=== menelaus_name ===",
                "Ink must record the name branch");
        contains(ink, "=== menelaus_helen_1 ===",
                "Ink must record the Helen branch");
        contains(ink, "=== menelaus_helen_router ===",
                "Ink retains the single repeatable Helen entry");
        contains(ink, "=== menelaus_sinni_helen ===",
                "Labor-I Ink must contain the moved Helen introduction");
        check(!ink.contains("helen_introduction_shown"),
                "Ink must not split first and repeat selections");
        contains(ink.replace("\r\n", "\n"),
                "=== menelaus_helen_end ===\n\n-> menelaus_helen_1",
                "Ink must continue from both player responses into the record");
        contains(ink, "“Very well.”", "Ink must mirror the revised assent");
        check(!firstContactInk.contains("=== menelaus_sinni_helen ==="),
                "First-contact Ink must not force the Helen introduction");
        check(!firstContactInk.contains(
                        "-> menelaus_sinni_helen"),
                "First contact must not route through the Helen introduction");
        contains(ink, "+ [“What happened here when the Gate Network failed?”]",
                "Collapse question must be sticky in Ink");
        contains(ink, "+ [Have Sinni ask about Helen Argyros.]",
                "Helen question must be sticky in Ink");
        contains(ink, "+ [“So... you weren't named Menelaus when you were made.”]",
                "Name question must be sticky in Ink");
        contains(ink, "+ [“What happened to her during the Collapse?”]",
                "Helen Collapse follow-up must be sticky in Ink");
        contains(ink, "-> menelaus_labor_1_briefing_options",
                "Drone answer must return to the remaining briefing choices");
        contains(ink, "“What do you require?”",
                "Ink authoring prose must retain typographic quotation marks");
        contains(ink, "the Odyssey Expanse",
                "Ink must mirror the current system name");
        contains(ink, "Odyssey Sector",
                "Ink must mirror the current regional name");
        contains(ink, "Their escorts are secondary",
                "Ink must mirror the implemented Labor objective");
        contains(ink, "Do not scan the gate housed inside FOB Ithaca",
                "Ink must mirror the gate prohibition");
        contains(ink, "=== menelaus_labor_1_repair ===",
                "Ink must mirror the Labor-I repair return route");
        check(!ink.contains("Alpha Odyssey"),
                "New Ink prose must not restore the retired system name");
        check(!ink.contains("Orion Knot Sector"),
                "New Ink prose must not restore the retired regional name");
        check(count(ink, "=== menelaus_contact_labor_1_active ===") == 1,
                "Ink must contain exactly one canonical reusable hub");

        System.out.println(
                "Menelaus Labor-I dialogue regression checks passed.");
    }
}
