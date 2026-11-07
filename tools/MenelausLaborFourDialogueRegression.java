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

    private static String normalize(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    public static void main(String[] args) throws Exception {
        String csv = Files.readString(ROOT.resolve("data/campaign/rules.csv"), StandardCharsets.UTF_8);
        check(csv.startsWith("id,trigger,"), "Native CSV requires a literal id header without a BOM");
        Map<String, List<String>> rules = rows(csv);
        String ink = Files.readString(ROOT.resolve("dialogue/menelaus_contact_labor_4.ink"), StandardCharsets.UTF_8);
        var pattern = java.util.regex.Pattern.compile("^=== (\\w+) ===\\s*\\n(.*?)(?=^=== |\\z)",
                java.util.regex.Pattern.MULTILINE | java.util.regex.Pattern.DOTALL);
        var matcher = pattern.matcher(ink);
        Map<String, String> knots = new HashMap<>();
        while (matcher.find()) knots.put(matcher.group(1), matcher.group(2));
        Map<String, String> roots = Map.ofEntries(
                Map.entry("menelaus_contact_labor_4_active", "chiefNavigatorFobIthacaSensorBriefing"),
                Map.entry("menelaus_labor_4_middle", "chiefNavigatorMenelausLaborFourMiddle"),
                Map.entry("menelaus_labor_4_package", "chiefNavigatorMenelausLaborFourPackage"),
                Map.entry("menelaus_labor_4_accept", "chiefNavigatorMenelausLaborFourUnderstood"),
                Map.entry("menelaus_labor_4_sinni_why", "chiefNavigatorMenelausLaborFourSinniWhy"),
                Map.entry("menelaus_labor_4_sinni_why_2", "chiefNavigatorMenelausLaborFourSinniWhyPage2"),
                Map.entry("menelaus_labor_4_sinni_refuse", "chiefNavigatorMenelausLaborFourSinniRefuseAfterHelen"),
                Map.entry("menelaus_labor_4_sinni_choice", "chiefNavigatorMenelausLaborFourSinniChoiceAfterHelen"),
                Map.entry("menelaus_labor_4_sinni_end", "chiefNavigatorMenelausLaborFourSinniEnd"),
                Map.entry("menelaus_contact_labor_4_menu", "chiefNavigatorMenelausLaborFourHub"),
                Map.entry("menelaus_contact_labor_4_status", "chiefNavigatorMenelausLaborFourActive"),
                Map.entry("menelaus_reach_history", "chiefNavigatorMenelausLaborFourReach"),
                Map.entry("menelaus_threat_origin", "chiefNavigatorMenelausLaborFourThreat"),
                Map.entry("menelaus_starving_threat", "chiefNavigatorMenelausLaborFourStarving"),
                Map.entry("menelaus_helen_4", "chiefNavigatorMenelausLaborFourHelen"),
                Map.entry("menelaus_helen_5", "chiefNavigatorMenelausLaborFourHelenPage2"),
                Map.entry("menelaus_helen_4_press", "chiefNavigatorMenelausLaborFourHelenPress"),
                Map.entry("menelaus_helen_4_sinni", "chiefNavigatorMenelausLaborFourHelenSinni"),
                Map.entry("menelaus_helen_4_end", "chiefNavigatorMenelausLaborFourHelenEnd"),
                Map.entry("menelaus_contact_leave", "chiefNavigatorMenelausLaborFourLeave"));
        for (var entry : roots.entrySet()) {
            List<String> row = rules.get(entry.getValue());
            StringBuilder actual = new StringBuilder();
            for (int page = 0; page < 15; page++) {
                actual.append(row.get(4)).append("\n\n");
                if (!row.get(5).matches("0:[^:]+:Continue\\.")) break;
                String option = row.get(5).split(":", 3)[1];
                row = rules.values().stream().filter(r -> r.get(1).equals("DialogOptionSelected")
                        && r.get(2).equals("$option == " + option)).findFirst().orElseThrow();
                check(page < 14, "Unbounded page loop");
            }
            String source = knots.get(entry.getKey());
            String expected = source.lines().filter(line -> !line.startsWith("-> ")
                    && !line.startsWith("+ [") && !line.startsWith("* ["))
                    .collect(java.util.stream.Collectors.joining("\n"));
            check(normalize(actual.toString()).equals(normalize(expected)), "Prose parity: " + entry.getKey());
            check(!actual.toString().contains("\\n"), "Paragraphs must contain real newlines");
            String rendered = row.get(5);
            if (entry.getKey().equals("menelaus_contact_labor_4_menu")) {
                rendered = rules.values().stream().filter(r -> r.get(1).equals("ChiefNavigatorMenelausLaborFourHubOptions"))
                        .map(r -> r.get(5)).collect(java.util.stream.Collectors.joining("\n"));
            }
            var choices = java.util.regex.Pattern.compile("^[+*] \\[(.*?)\\] -> \\w+\\s*$",
                    java.util.regex.Pattern.MULTILINE).matcher(source);
            while (choices.find()) check(rendered.contains(":" + choices.group(1)), "Choice parity: " + choices.group(1));
        }
        check(rules.get("chiefNavigatorMenelausLaborFourAccept").get(3).replace("\r\n", "\n").equals(
                "$global.chief_navigator_menelaus_labor_four_briefed = true\nFireAll ChiefNavigatorMenelausLaborFourHub"),
                "Explicit acceptance must preserve its durable callback");
        check(rules.get("chiefNavigatorMenelausLaborFourThreatDone").get(3).endsWith(
                "FireAll ChiefNavigatorMenelausLaborFourHub"), "Threat exit returns to the hub");
        System.out.println("PASS: all 20 Labor IV nodes preserve supplied prose and choices, native header and acceptance callback.");
    }
}
