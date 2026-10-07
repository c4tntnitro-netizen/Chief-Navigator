package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Static contract checks for the post-Labor-III Voss arrival scene. */
public final class VossArrivalDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.replace("\r\n", "\n").contains(expected.replace("\r\n", "\n")),
                message + ": " + expected);
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
                        check(row.size() >= 6, "Missing authored CSV fields: " + row.get(0));
                        check(result.put(row.get(0), List.copyOf(row)) == null,
                                "Duplicate authored rule: " + row.get(0));
                    }
                    row.clear();
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty(), "Authored CSV must end with balanced fields");
        return result;
    }

    private static void recognitionContracts(String rules, String command) {
        Map<String, List<String>> rows = rows(rules);
        int recognitionDispatches = 0;
        for (List<String> row : rows.values()) {
            for (String line : row.get(3).split("\\R")) {
                if (line.equals("FireBest ChiefNavigatorVossRecognition")) {
                    recognitionDispatches++;
                }
                check(!line.equals("FireAll ChiefNavigatorVossRecognition"),
                        "Recognition must dispatch exactly one branch: " + row.get(0));
            }
        }
        check(recognitionDispatches == 3, "All three interjections must dispatch one recognition branch");
        for (String interjection : List.of("Salute", "Charge", "Boss")) {
            List<String> row = rows.get("chiefNavigatorVossRank" + interjection);
            check(row != null && row.get(3).equals("FireBest ChiefNavigatorVossRecognition"),
                    "Interjection must use exclusive recognition dispatch: " + interjection);
        }
        String[] branches = {"Friendly", "Hostile", "Neutral"};
        String[] signatures = {"Golden Boy", "I should have you shot where you stand.",
                "And who the fuck are you?"};
        for (int i = 0; i < branches.length; i++) {
            String id = "chiefNavigatorVossRecognition" + branches[i];
            List<String> row = rows.get(id);
            check(row != null && row.get(1).equals("ChiefNavigatorVossRecognition"),
                    "Recognition branch must remain in its exclusive group: " + id);
            check(row.get(2).equals("ChiefNavigatorMenelausCMD vossReputation "
                            + branches[i].toLowerCase(java.util.Locale.ROOT)),
                    "Recognition must query current reputation rather than temporary flags: " + id);
            check(row.get(3).isBlank(), "Recognition query must not mutate reputation: " + id);
            for (int j = 0; j < signatures.length; j++) {
                check(row.get(4).contains(signatures[j]) == (i == j),
                        "Recognition branches must contain only their own response: " + id);
            }
            check(!row.get(4).contains("Recognition—or the lack of it"),
                    "Generic recognition heading must not precede a distinct reaction: " + id);
        }
        for (String flag : List.of("$chiefNavigatorVossHegFriendly",
                "$chiefNavigatorVossHegHostile", "$chiefNavigatorVossHegNeutral")) {
            check(!rules.contains(flag) && !command.contains(flag),
                    "Production dialogue must not read or create old temporary reputation flags: " + flag);
        }
    }

    private static void aftermathApologyContract(String rules) {
        Map<String, List<String>> rows = rows(rules);
        String transitionId = "chiefNavigatorHegemonyIthacaContinue";
        List<String> transition = rows.get(transitionId);
        check(transition != null, "Voss aftermath must retain its closing transition");
        check(transition.get(1).equals("DialogOptionSelected")
                        && transition.get(2).equals("$option == chief_navigator_hegemony_ithaca_continue"),
                "Apology must appear only after the aftermath's Continue is selected");
        check(transition.get(3).replace("\r\n", "\n").equals(
                        "ChiefNavigatorMenelausCMD hideVoss\nFireAll ChiefNavigatorFobIthaca"),
                "Apology transition must hide Voss and resume the current Labor without extra effects");
        check(transition.get(4).replace("\r\n", "\n").equals(
                        "Menelaus, for once, apologizes.\n\n"
                                + "\"I am sorry you had to witness that display of discipline, auxiliary,\" Menelaus says."),
                "Closing transition must contain exactly the authored apology paragraph");
        check(transition.get(5).isBlank(),
                "Closing transition must not add inline choices beside its nested Labor dispatch");
        List<String> aftermath = rows.get("chiefNavigatorVossAftermath");
        check(aftermath != null && aftermath.get(5).equals(
                        "0:chief_navigator_hegemony_ithaca_continue:Continue."),
                "Voss aftermath must still lead directly into the apology transition");
        for (List<String> row : rows.values()) {
            if (row.get(0).equals(transitionId)) continue;
            check(!row.get(4).contains("Menelaus, for once, apologizes.")
                            && !row.get(4).contains("witness that display of discipline"),
                    "Apology must not repeat in a reusable Labor briefing or another scene: " + row.get(0));
        }
    }

    public static void main(String[] args) throws Exception {
        String rules = Files.readString(
                ROOT.resolve("data/campaign/rules.csv"),
                StandardCharsets.UTF_8);
        String command = Files.readString(
                ROOT.resolve("src/data/campaign/rulecmd/ChiefNavigatorMenelausCMD.java"),
                StandardCharsets.UTF_8);

        int report = rules.indexOf(
                "chiefNavigatorFobIthacaWallReport,ChiefNavigatorFobIthaca");
        int arrival = rules.indexOf(
                "chiefNavigatorHegemonyIthacaArrival,", report);
        int laborFour = rules.indexOf(
                "chiefNavigatorMenelausReportSensor,", arrival);
        check(report >= 0 && arrival > report && laborFour > arrival,
                "Voss scene must remain between Labor III and Labor IV");
        String scene = rules.substring(arrival, laborFour);

        contains(rules.substring(report, arrival),
                "ChiefNavigatorMenelausCMD init\n"
                        + "ChiefNavigatorMenelausCMD report wall\n"
                        + "FireAll ChiefNavigatorHegemonyIthacaArrival",
                "Labor III report must commit silently and launch Voss's arrival");
        contains(scene, "Captain Aias Kleon",
                "Spartan's commanding officer must identify himself");
        contains(scene, "thirteen levels subordinate",
                "Menelaus must display the Domain command hierarchy");
        contains(scene, "chief_navigator_voss_rank_boss",
                "The Voss Boss response must remain selectable");
        contains(scene, "chiefNavigatorVossRecognitionFriendly",
                "Friendly Hegemony recognition variant must exist");
        contains(scene, "chiefNavigatorVossRecognitionHostile",
                "Hostile Hegemony recognition variant must exist");
        contains(scene, "chiefNavigatorVossRecognitionNeutral",
                "Neutral Hegemony recognition variant must exist");
        contains(scene, "chief_navigator_voss_nav_skill",
                "Skill-issue response must remain selectable");
        contains(scene, "I decline this order to frag my superior",
                "Aias must reject the containment order");
        contains(scene, "\"\"...HUMAN.\"\"",
                "Menelaus's declaration must remain intact");
        check(!scene.contains("Sokolova") && !scene.contains("liaison"),
                "Voss must leave without installing the retired liaison");
        contains(scene, "FireAll ChiefNavigatorFobIthaca",
                "The scene must return to Labor IV");

        contains(command, "relationship > 0.5f",
                "Friendly branch threshold must be above +50");
        contains(command, "relationship < -0.5f",
                "Hostile branch threshold must be below -50");
        contains(command,
                "relationship >= -0.5f && relationship <= 0.5f",
                "Neutral branch must include both boundary values");
        contains(command, "markIthacaCommandSceneShown()",
                "Inline scene must suppress the migration fallback replay");

        recognitionContracts(rules, command);
        aftermathApologyContract(rules);

        System.out.println("Voss arrival dialogue regression checks passed.");
    }
}
