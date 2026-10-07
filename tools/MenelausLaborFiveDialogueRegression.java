package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static contract checks for the Labor-V launch, field briefing and victory debrief. */
public final class MenelausLaborFiveDialogueRegression {
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

    private static void twoPageFinalOffer(String rules, String ink) {
        Map<String, List<String>> rows = rows(rules);
        List<String> opening = rows.get("chiefNavigatorFobIthacaFinalOffer");
        List<String> decision = rows.get("chiefNavigatorFobIthacaFinalOfferPage3");
        List<String> acceptance = rows.get("chiefNavigatorMenelausAcceptFinal");
        check(opening != null && decision != null && acceptance != null,
                "The final-Labor offer must retain its root, decision page and explicit acceptance handler");
        String obsolete = "chief_navigator_auto_page_chief_navigator_fob_ithaca_final_offer_2";
        String continueId = "chief_navigator_auto_page_chief_navigator_fob_ithaca_final_offer_3";
        check(!rows.containsKey("chiefNavigatorFobIthacaFinalOfferPage2") && !rules.contains(obsolete),
                "The obsolete recap page and its Continue option must be removed entirely");
        check(opening.get(1).equals("ChiefNavigatorFobIthaca")
                        && opening.get(2).replace("\r\n", "\n").equals(
                                "!ChiefNavigatorMenelausCMD stage early_wall\n"
                                        + "ChiefNavigatorMenelausCMD stage final_offer")
                        && opening.get(3).equals("ChiefNavigatorMenelausCMD init"),
                "The two-page offer must preserve its stage gates and remain inactive until accepted");
        String openingText = "\"The fifth Labor begins only on your order. Three massive Starving Threat "
                + "formations are gathering beyond Ithaca's perimeter bastions. Once you accept, each formation "
                + "will begin a siege lasting approximately two months. Any formation still alive at its deadline "
                + "will destroy its assigned bastion and turn on Task Force Spartan; Spartan losses will not be replaced.\"";
        check(opening.get(4).equals(openingText)
                        && opening.get(5).equals("0:" + continueId + ":Continue."),
                "The offer must begin directly with the unchanged siege paragraph and lead to its decision page");
        String decisionText = "\"Gautama will not commit the Heavenly Strike until all three perimeter formations "
                + "are dead. Then every surviving Spartan formation will jump directly to the center engagement. "
                + "Accept only when you are prepared to defend Ithaca.\"";
        check(decision.get(1).equals("DialogOptionSelected")
                        && decision.get(2).equals("$option == " + continueId)
                        && decision.get(4).equals(decisionText),
                "The retained final_offer_3 option must lead directly to the unchanged Gautama warning");
        check(decision.get(3).equals(
                        "SetOptionColor chief_navigator_menelaus_accept_final chiefNavigatorSinniOptionColor"),
                "The explicit acceptance option must retain its red signal color");
        check(decision.get(5).replace("\r\n", "\n").equals(
                        "10:chief_navigator_menelaus_accept_final:Accept the final Labor.\n"
                                + "80:chief_navigator_fob_ithaca_upgrades:Review Ithaca defense upgrades.\n"
                                + "95:chief_navigator_fob_ithaca_repair:Request free repairs.\n"
                                + "100:chief_navigator_fob_ithaca_leave:Leave."),
                "The acceptance, upgrades, repairs and safe Leave options must remain unchanged");
        check(acceptance.get(1).equals("DialogOptionSelected")
                        && acceptance.get(2).equals("$option == chief_navigator_menelaus_accept_final")
                        && acceptance.get(3).equals("ChiefNavigatorMenelausCMD acceptFinalLabor"),
                "Only the existing explicit red acceptance choice may start Labor V");

        String mirror = ink.replace("\r\n", "\n").replace('“', '"').replace('”', '"');
        String start = "=== menelaus_final_labor_offer ===";
        String end = "=== menelaus_final_labor_accepted ===";
        int from = mirror.indexOf(start);
        int to = mirror.indexOf(end, from);
        check(from >= 0 && to > from, "The authoring mirror must retain its final-offer knot");
        String knot = mirror.substring(from + start.length(), to).stripLeading();
        check(knot.startsWith(openingText + "\n\n" + decisionText),
                "The authoring mirror must begin with the same two runtime offer paragraphs");
        for (String removed : List.of("alpha-core sigil burns steadily", "The four preparatory Labors are complete.",
                "another reciprocal sample.")) {
            check(!opening.get(4).contains(removed) && !decision.get(4).contains(removed)
                            && !knot.contains(removed),
                    "The removed recap must not remain in the final offer or its Ink mirror: " + removed);
        }
    }

    private static void finalAcceptanceCharge(String rules, String ink) {
        List<String> acceptance = rows(rules).get("chiefNavigatorMenelausAcceptFinal");
        check(acceptance != null, "The final Labor must retain its explicit acceptance scene");
        String text = acceptance.get(4).replace("\r\n", "\n");
        check(!text.contains("Auxiliary... no."),
                "The accepted final Labor must not retain the discarded Auxiliary correction");
        check(text.startsWith("Menelaus faces you.\n\nHis blue-white eyes burn into yours.")
                        && text.contains("\"Hero of Ithaca. Defender of the Domain.\"")
                        && text.contains("\"Save us all.\""),
                "Preserve Menelaus's facing, gaze, heroic address and final charge");
        check(acceptance.get(3).equals("ChiefNavigatorMenelausCMD acceptFinalLabor")
                        && acceptance.get(5).equals(
                                "0:chief_navigator_menelaus_labor_five_depart:\"We'll stop them.\""),
                "The real Labor-V activation and existing departure choice must remain unchanged");
        String mirror = ink.replace("\r\n", "\n").replace('“', '"').replace('”', '"');
        String start = "=== menelaus_final_labor_accepted ===";
        String end = "=== menelaus_labor_5_depart ===";
        int from = mirror.indexOf(start);
        int to = mirror.indexOf(end, from);
        check(from >= 0 && to > from, "The authoring mirror must retain the final acceptance knot");
        String knot = mirror.substring(from + start.length(), to).stripLeading();
        check(knot.startsWith(text + "\n\n") && !knot.contains("Auxiliary... no."),
                "The acceptance Ink mirror must match the shortened runtime charge");
        check(knot.contains("- [\"We'll stop them.\"] -> menelaus_labor_5_depart"),
                "The authoring mirror must retain the same departure choice");
    }

    private static void optionalVictoryExplanation(String rules, String ink) {
        Map<String, List<String>> rows = rows(rules);
        List<String> opening = rows.get("chiefNavigatorFobIthacaDebrief");
        List<String> question = rows.get("chiefNavigatorMenelausDebriefGautama");
        List<String> explanation = rows.get("chiefNavigatorFobIthacaDebriefPage2");
        List<String> acknowledgement = rows.get("chiefNavigatorFobIthacaDebriefPage3");
        List<String> closing = rows.get("chiefNavigatorFobIthacaDebriefPage4");
        List<String> departure = rows.get("chiefNavigatorFobIthacaDebriefLeave");
        String topic = "chief_navigator_menelaus_debrief_gautama";
        String page2 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_2";
        String page3 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_3";
        String page4 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_4";
        check(opening.get(4).startsWith("\"Ithaca remains. Task Force Spartan remains.")
                        && !opening.get(4).contains("Gautama") && !opening.get(4).contains("true form")
                        && !rules.contains("Menelaus is waiting before the channel finishes opening."),
                "The debrief must congratulate first without the removed waiting line or mandatory lore");
        check(opening.get(2).replace("\r\n", "\n").equals(
                        "!ChiefNavigatorMenelausCMD stage early_wall\nChiefNavigatorMenelausCMD stage debrief")
                        && opening.get(3).equals("ChiefNavigatorMenelausCMD init")
                        && opening.get(5).replace("\r\n", "\n").equals(
                                "10:" + topic + ":\"What did you learn about Gautama?\"\n"
                                        + "100:" + page3 + ":Continue."),
                "The existing stage/init must offer both the optional question and a direct skip");
        check(question.get(2).equals("$option == " + topic) && question.get(3).isBlank()
                        && question.get(4).contains("Gautama was never the complete organism.")
                        && question.get(5).equals("0:" + page2 + ":Continue.")
                        && explanation.get(2).equals("$option == " + page2)
                        && explanation.get(3).isBlank()
                        && explanation.get(4).contains("Its true form is a web of nanometer-scale black filaments")
                        && explanation.get(4).contains("The wreck cannot be approached until that knot is destroyed.")
                        && explanation.get(5).equals("0:" + page3 + ":Continue."),
                "The optional answer must preserve both lore pages and converge with the direct skip");
        check(acknowledgement.get(2).equals("$option == " + page3)
                        && acknowledgement.get(3).isBlank()
                        && acknowledgement.get(4).startsWith("\"Your fifth Labor is complete.")
                        && !acknowledgement.get(4).contains("Ithaca remains.")
                        && acknowledgement.get(5).equals("0:" + page4 + ":Continue.")
                        && closing.get(2).equals("$option == " + page4)
                        && closing.get(3).isBlank()
                        && closing.get(5).equals("0:chief_navigator_fob_ithaca_debrief_leave:Leave Menelaus to his command.")
                        && departure.get(3).equals("ChiefNavigatorMenelausCMD finish"),
                "Both routes must preserve the common farewell and existing completion action without repeated praise");
        String mirror = ink.replace("\r\n", "\n").replace('“', '"').replace('”', '"');
        int start = mirror.indexOf("=== menelaus_final_labor_debrief ===");
        int questionStart = mirror.indexOf("=== menelaus_debrief_gautama ===", start);
        int acknowledgementStart = mirror.indexOf("=== menelaus_debrief_acknowledgement ===", questionStart);
        check(start >= 0 && questionStart > start && acknowledgementStart > questionStart,
                "Ink must separate the congratulations, optional answer and shared acknowledgement");
        String intro = mirror.substring(start, questionStart);
        check(intro.contains(opening.get(4).replace("\r\n", "\n"))
                        && intro.contains("- [\"What did you learn about Gautama?\"] -> menelaus_debrief_gautama")
                        && intro.contains("- [Continue.] -> menelaus_debrief_acknowledgement")
                        && !intro.contains("Its true form")
                        && mirror.substring(questionStart, acknowledgementStart)
                                .contains("- [Continue.] -> menelaus_debrief_acknowledgement"),
                "The Ink choices must mirror the optional runtime routes");
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String command = Files.readString(
                ROOT.resolve("src/data/campaign/rulecmd/ChiefNavigatorMenelausCMD.java"),
                StandardCharsets.UTF_8);
        String ink = Files.readString(
                ROOT.resolve("dialogue/menelaus_labor_reports.ink"),
                StandardCharsets.UTF_8);

        int accept = rules.indexOf(
                "chiefNavigatorMenelausAcceptFinal,DialogOptionSelected");
        int active = rules.indexOf(
                "chiefNavigatorFobIthacaGautama,", accept);
        check(accept >= 0 && active > accept,
                "Labor-V launch must precede its reusable active state");
        String briefing = rules.substring(accept, active);

        contains(briefing, "ChiefNavigatorMenelausCMD acceptFinalLabor",
                "Red acceptance must activate Labor V before the briefing");
        contains(briefing, "Hero of Ithaca. Defender of the Domain.",
                "Menelaus's final charge must be present");
        contains(briefing, "ChiefNavigatorMenelausCMD showSpartan",
                "The scene must hand the portrait to Captain Kleon");
        contains(briefing,
                "chief_navigator_menelaus_labor_five_field_briefing",
                "The field-briefing route must be reachable");
        contains(briefing, "We've isolated four Hostswarms",
                "Briefing must establish all four attack formations");
        contains(briefing, "One is moving on North Bastion. One on South. One on East.",
                "Briefing must identify the three implemented perimeter assaults");
        contains(briefing, "The fourth comes afterward",
                "Briefing must identify Gautama's delayed final attack");
        contains(briefing, "Task Force Spartan makes its stand",
                "Briefing must identify Spartan's breach defense");
        contains(briefing,
                "chiefNavigatorMenelausLaborFiveBegin,DialogOptionSelected",
                "The final Begin option must have a handler");
        contains(briefing, "ChiefNavigatorMenelausCMD endContact",
                "The final Begin option must return safely to the comm directory");

        contains(command, "\"showSpartan\".equals(action)",
                "Menelaus command must support the portrait handoff");
        contains(command, "getOrCreateSpartanCaptain()",
                "Portrait handoff must use the persistent Aias contact");
        contains(command, "$chiefNavigatorLaborFivePlayerName",
                "Field briefing must populate the player's name token");

        contains(ink, "=== menelaus_labor_5_field_briefing ===",
                "Ink mirror must retain the supplied field-briefing knot");
        contains(ink, "=== menelaus_labor_5_line ===",
                "Ink mirror must retain the briefing conclusion");

        twoPageFinalOffer(rules, ink);
        finalAcceptanceCharge(rules, ink);
        optionalVictoryExplanation(rules, ink);

        System.out.println(
                "Menelaus Labor-V dialogue regression checks passed.");
    }
}
