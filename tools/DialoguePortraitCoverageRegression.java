package chiefnavigator.quest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authoring contracts for persistent speaker casts and the targeted portrait gaps. */
public final class DialoguePortraitCoverageRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    static Map<String, List<String>> readRules(String csv) {
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
                        check(row.size() >= 6, "Missing CSV fields in " + row.get(0));
                        check(result.put(row.get(0), List.copyOf(row)) == null,
                                "Duplicate dialogue rule " + row.get(0));
                    }
                    row.clear();
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty(), "Dialogue CSV must end with balanced fields");
        return result;
    }

    private static String actionBody(String source, String action) {
        int start = source.indexOf("if (\"" + action + "\".equals(action)) {");
        check(start >= 0, "Missing Menelaus presentation action " + action);
        int open = source.indexOf('{', start);
        int depth = 1;
        int end = open + 1;
        for (; end < source.length() && depth > 0; end++) {
            if (source.charAt(end) == '{') depth++;
            else if (source.charAt(end) == '}') depth--;
        }
        check(depth == 0, "Unclosed Menelaus action " + action);
        return source.substring(open + 1, end - 1).replaceAll("\\s+", " ");
    }

    private static void defaultCasts() throws Exception {
        String menelaus = read("src/data/campaign/rulecmd/ChiefNavigatorMenelausCMD.java");
        check(actionBody(menelaus, "init").contains(
                        "ConversationPortraits.show(dialog, menelaus, SinniContact.getOrCreatePerson());"),
                "Labor and Helen pages must default to Menelaus and Sinni together");
        check(actionBody(menelaus, "showVoss").contains(
                        "ConversationPortraits.show(dialog, menelaus, voss, "
                                + "FobIthacaContacts.getOrCreateSpartanCaptain(), SinniContact.getOrCreatePerson());"),
                "Voss conference must retain Menelaus, Voss, Aias and Sinni together");
        check(actionBody(menelaus, "hideVoss").contains(
                        "ConversationPortraits.show(dialog, MenelausTrial.getOrCreateMenelaus(), "
                                + "SinniContact.getOrCreatePerson());"),
                "Closing Voss's audience must restore Menelaus and Sinni");
        check(actionBody(menelaus, "showSpartan").contains(
                        "ConversationPortraits.show(dialog, captain, SinniContact.getOrCreatePerson());"),
                "Labor V corridor dialogue must retain Aias and Sinni together");
    }

    public static void main(String[] args) throws Exception {
        Map<String, List<String>> rows = readRules(read("data/campaign/rules.csv"));
        Map<String, String> expected = Map.ofEntries(
                Map.entry("chiefNavigatorMenelausFirstShuttleContinue", "show sinni player"),
                Map.entry("chiefNavigatorMenelausFirstSinniGo", "show sinni sara player"),
                Map.entry("chiefNavigatorMenelausFirstSinniGoPage2", "show sinni player"),
                Map.entry("chiefNavigatorMenelausFirstHolodeck", "show sinni player"),
                Map.entry("chiefNavigatorMenelausFirstAbomination", "show menelaus sinni sara"),
                Map.entry("chiefNavigatorMenelausFirstBusiness", "show menelaus sinni"),
                Map.entry("chiefNavigatorVossRankContinuePage9", "show menelaus voss aias sinni sara"),
                Map.entry("chiefNavigatorVossRankContinuePage10", "show menelaus voss aias sinni"),
                Map.entry("chiefNavigatorLeagueRescueRecoverWithSaraPage3",
                        "illustrated chief_navigator_persean_survivors_aftermath sara"),
                Map.entry("chiefNavigatorAviciHabitatLightPage4", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatHeavyPage4", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatFoodEat", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatTruth2", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatRescueSneak", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatRescueHeavy2", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatRescueLight", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatRescueLeave", "show sara"),
                Map.entry("chiefNavigatorAviciHabitatLightPage5", "clear"),
                Map.entry("chiefNavigatorAviciHabitatHeavyPage5", "clear"),
                Map.entry("chiefNavigatorAviciHabitatMotherContact0", "clear"),
                Map.entry("chiefNavigatorAviciHabitatRescueLightPage3", "clear"),
                Map.entry("chiefNavigatorAviciHabitatRescueEndFight", "clear"),
                Map.entry("chiefNavigatorAviciHabitatRescueEndSaved", "clear"),
                Map.entry("chiefNavigatorAviciHabitatRescueEndLeft", "clear"),
                Map.entry("chiefNavigatorMenelausFirstLosing2", "show menelaus sinni"),
                Map.entry("chiefNavigatorVossHostileAppeal", "show voss sinni"),
                Map.entry("chiefNavigatorVossHostileDefiance", "show voss sinni"),
                Map.entry("chiefNavigatorVossHostileHunt", "show voss sinni"),
                Map.entry("chiefNavigatorIthacaLeagueSanzu", "show contact sinni"),
                Map.entry("chiefNavigatorTroyAnchorageAdvice", "show sinni"),
                Map.entry("chiefNavigatorSinniEventideArrival",
                        "illustrated chief_navigator_sinni_eventide_tea"),
                Map.entry("chiefNavigatorSinniAlphaOdysseyArrival",
                        "illustrated chief_navigator_alpha_odyssey_arrival"),
                Map.entry("chiefNavigatorLabor5EndingWakingPage4",
                        "illustrated chief_navigator_sinni_ending"));
        Set<String> actual = new HashSet<>();
        Set<String> roles = Set.of("menelaus", "sinni", "aias", "voss", "player", "contact", "sara");
        for (List<String> row : rows.values()) {
            int assignments = 0;
            for (String line : row.get(3).split("\\R")) {
                if (!line.startsWith("ChiefNavigatorPeopleCMD ")) continue;
                assignments++;
                actual.add(row.get(0));
                String parameters = line.substring("ChiefNavigatorPeopleCMD ".length());
                check(parameters.equals(expected.get(row.get(0))),
                        "Portrait assignment must match that page's authored cast: " + row.get(0));
                String[] tokens = parameters.split("\\s+");
                if (tokens[0].equals("clear")) {
                    check(tokens.length == 1, "A portrait clear has no speaker roles");
                    continue;
                }
                int firstRole = tokens[0].equals("illustrated") ? 2 : 1;
                check(tokens[0].equals("show") || tokens[0].equals("illustrated"),
                        "Unrecognized portrait presentation in " + row.get(0));
                Set<String> seen = new HashSet<>();
                for (int i = firstRole; i < tokens.length; i++) {
                    check(roles.contains(tokens[i]) && seen.add(tokens[i]),
                            "Portrait roles must be recognized and unique in " + row.get(0));
                }
                check((tokens[0].equals("illustrated") || !seen.isEmpty())
                                && seen.size() <= 5,
                        "Only illustrated pages may omit separate portraits: " + row.get(0));
                if (tokens[0].equals("illustrated")) {
                    check(!row.get(3).contains("ShowImageVisual "),
                            "A second image command must not erase illustrated speaker portraits: " + row.get(0));
                }
            }
            check(assignments <= 1, "A page must not repeatedly replace its own portrait cast: " + row.get(0));
        }
        check(actual.equals(expected.keySet()), "All targeted speaker gaps must have explicit cast assignments");
        check(rows.get("chiefNavigatorMenelausFirstLosing").get(3).equals(
                        "ChiefNavigatorMenelausCMD showGautama"),
                "The existing Gautama ship visual must remain intact before portraits resume");
        check(rows.values().stream().filter(row -> row.get(3).contains(
                        "ChiefNavigatorMenelausCMD showGautama")).count() == 1,
                "Every authored ship-visual interruption must have a subsequent speaker restoration");
        defaultCasts();
        check(rows.get("chiefNavigatorMenelausFirstHolodeckPage2").get(3).isEmpty()
                        && rows.get("chiefNavigatorMenelausFirstHolodeckPage3").get(3)
                                .equals("ChiefNavigatorMenelausCMD init")
                        && rows.get("chiefNavigatorMenelausFirstHolodeckPage3").get(4)
                                .startsWith("The features are almost human."),
                "Menelaus must first appear on the almost-human reveal page, not the empty chamber");
        check(rows.get("chiefNavigatorAviciHabitatRescueLight").get(3).replace("\r\n", "\n").equals(
                        "ChiefNavigatorPeopleCMD show sara\nChiefNavigatorAviciHabitatCMD rescue light"),
                "The fatal scene must show Sara before its existing death-state commit");
        for (String source : List.of(read("data/campaign/rules.csv"),
                read("dialogue/menelaus_first_contact.ink"))) {
            check(!source.contains("The Domain fell two hundred years ago.")
                            && !source.contains("Ithaca kept building ships."),
                    "Both removed monorail lines must stay absent from runtime and Ink");
        }
        System.out.println("Dialogue portrait coverage passed: Sara casts, clear/restoration pages, Menelaus reveal timing and art-only exceptions.");
    }
}
