package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberStatusAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Authored Labor-III prose and its actual native conversation and services. */
public final class MenelausLaborThreeDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String PREFIX = "chief_navigator_menelaus_labor_three_";
    private static final String BRIEFED = "$chief_navigator_menelaus_labor_three_briefed";
    private static final String HELEN_ASKED = "$chiefNavigatorMenelausL3AskedHelen";
    private static final String HISTORY_ASKED = "$chiefNavigatorMenelausL3AskedHistory";
    private static final String UPGRADE = "chief_navigator_fob_ithaca_upgrades";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void contains(String text, String expected, String message) {
        check(text.replace("\r\n", "\n").contains(expected.replace("\r\n", "\n")),
                message + ": " + expected);
    }

    private static String normalized(String text) {
        return text.replaceAll("\\s+", " ").trim();
    }

    private static boolean laborThreeReadSuffix(String suffix) {
        return suffix.startsWith("menelaus_labor_three_")
                || suffix.startsWith("auto_page_chief_navigator_menelaus_labor_three_")
                || suffix.startsWith("auto_page_chief_navigator_fob_ithaca_wall_briefing_");
    }

    private static void readCatalog(String rules) {
        Set<String> expectedTopics = new HashSet<>(), expectedContinuations = new HashSet<>();
        Set<String> navigation = Set.of("\"I accept.\"", "\"Understood.\"",
                "\"Nothing for now.\"", "Request free repairs.");
        for (List<String> row : DialoguePortraitCoverageRegression.readRules(rules).values()) {
            if (!row.get(0).startsWith("chiefNavigatorMenelausLaborThree")
                    && !row.get(0).startsWith("chiefNavigatorFobIthacaWallBriefing")) continue;
            for (String option : row.get(5).split("\\R")) {
                if (option.isBlank()) continue;
                String[] parts = option.split(":", 3);
                check(parts.length == 3 && parts[1].startsWith("chief_navigator_"),
                        "Labor III must use well-formed native option IDs");
                String suffix = parts[1].substring("chief_navigator_".length());
                if (!laborThreeReadSuffix(suffix)) continue;
                if (parts[2].equals("Continue.")) expectedContinuations.add(suffix);
                else if (!navigation.contains(parts[2])) expectedTopics.add(suffix);
            }
        }
        Set<String> actualTopics = new HashSet<>(), actualContinuations = new HashSet<>();
        for (String topic : MenelausDialogueReadState.TOPICS) {
            if (laborThreeReadSuffix(topic)) actualTopics.add(topic);
        }
        for (String continuation : MenelausDialogueReadState.CONTINUATIONS) {
            if (laborThreeReadSuffix(continuation)) actualContinuations.add(continuation);
        }
        check(expectedTopics.equals(actualTopics),
                "Labor III topic read catalog must exactly match authored responses: expected "
                        + expectedTopics + ", actual " + actualTopics);
        check(expectedContinuations.equals(actualContinuations),
                "Labor III continuation catalog must exactly match every authored Continue: expected "
                        + expectedContinuations + ", actual " + actualContinuations);
        check(MenelausDialogueReadState.TOPICS.contains("fob_ithaca_upgrades"),
                "The preserved shared upgrade-ledger topic must retain read styling");
    }

    private static void authoredParity(String rules, String ink) {
        Map<String, List<String>> parsed = DialoguePortraitCoverageRegression.readRules(rules);
        StringBuilder runtime = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : parsed.entrySet()) {
            if (entry.getKey().startsWith("chiefNavigatorMenelausLaborThree")
                    || entry.getKey().startsWith("chiefNavigatorFobIthacaWallBriefing")) {
                runtime.append(entry.getValue().get(4)).append('\n');
            }
        }
        String prose = normalized(runtime.toString());
        // Compare complete authored paragraphs, independent of runtime-only
        // page boundaries. Options and Ink routing are exercised natively below.
        StringBuilder paragraph = new StringBuilder();
        for (String line : (ink + "\n\n").split("\\R", -1)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("===") || trimmed.startsWith("->")
                    || trimmed.startsWith("*") || trimmed.startsWith("+")
                    || trimmed.startsWith("//")) {
                if (paragraph.length() > 0) {
                    check(prose.contains(normalized(paragraph.toString())),
                            "The entire authored Ink paragraph must reach runtime: " + paragraph);
                    paragraph.setLength(0);
                }
            } else paragraph.append(trimmed).append(' ');
        }
        for (String passage : List.of("at least eighty percent", "spare no materiel",
                "Grand Navarch Kali Molina", "Between truthfulness and survival",
                "Molon Labe.", "The answer given to the Persians",
                "I did not yet possess the capability.", "I came to value that.")) {
            contains(rules, passage, "The supplied revision must be present in runtime");
            contains(ink, passage, "The supplied revision must be present in Ink");
        }
        for (String knot : List.of("menelaus_helen_3_motto_ask", "menelaus_helen_3_motto_know",
                "menelaus_helen_3_motto_end", "menelaus_helen_3_capability")) {
            contains(ink, "=== " + knot + " ===", "The new authored branch must remain in Ink");
        }
        check(!ink.contains("Not entirely.") && !prose.contains("Not entirely.")
                        && !ink.contains("I... predicted well in advance"),
                "The supplied Helen answers must replace the retired wording");
    }

    private static void autoPages(MenelausFlowRegression.Fixture f) {
        Set<String> visited = new HashSet<>();
        while (f.options.size() == 1) {
            String id = f.options.keySet().iterator().next();
            if (!id.startsWith("chief_navigator_auto_page_")) return;
            check(visited.add(id) && visited.size() < 40,
                    "Labor III pagination must terminate without repeating an option");
            choose(f, id);
            f.settle();
        }
    }

    private static void pageBudget(MenelausFlowRegression.Fixture f, int firstParagraph) {
        int lines = f.options.size();
        for (String text : f.history.subList(firstParagraph, f.history.size())) {
            for (String paragraph : text.split("(?:\\r?\\n){2,}")) {
                String flat = normalized(paragraph);
                if (!flat.isEmpty()) lines += Math.max(1, (flat.length() + 51) / 52) + 1;
            }
        }
        check(lines <= 30, "The complete native page, including FireAll prose and choices, exceeds "
                + "the 30-line budget: " + lines + " lines via " + f.applied);
    }

    private static void choose(MenelausFlowRegression.Fixture f, String id) {
        int firstParagraph = f.history.size();
        f.choose(id);
        pageBudget(f, firstParagraph);
    }

    private static MenelausFlowRegression.Fixture contact(boolean briefed) throws Exception {
        MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
        f.stage(MenelausTrial.LABOR_WALL, false, briefed);
        f.plugin = new FobIthacaContactInteraction(f.person);
        f.fire("ChiefNavigatorFobIthaca");
        pageBudget(f, 0);
        autoPages(f);
        return f;
    }

    private static void hub(MenelausFlowRegression.Fixture f) {
        check(f.options.keySet().containsAll(Set.of(PREFIX + "active", UPGRADE,
                        PREFIX + "repair", PREFIX + "leave")),
                "Labor III must return to its operational hub: " + f.options);
        check(!f.options.containsKey(PREFIX + "understood")
                        && !f.options.containsKey(PREFIX + "accept") && f.global.getBoolean(BRIEFED),
                "The accepted hub must preserve acknowledgement without replaying acceptance");
        check(MenelausTrial.getCurrentLaborStage() == MenelausTrial.LABOR_WALL
                        && !f.global.getBoolean(MenelausTrial.WALL_FRIENDLY)
                        && !MenelausTrial.isLaborReported(MenelausTrial.LABOR_WALL),
                "Conversation and services must not capture the Wall or report the Labor");
    }

    private static long openingCount(MenelausFlowRegression.Fixture f) {
        return f.history.stream().filter(text -> text.contains("Your second Labor has borne fruit.")).count();
    }

    private static MenelausFlowRegression.Fixture reopen(MenelausFlowRegression.Fixture f)
            throws Exception {
        MenelausFlowRegression.Fixture reopened = new MenelausFlowRegression.Fixture();
        for (String key : f.global.getKeys()) reopened.global.set(key, f.global.get(key));
        reopened.plugin = new FobIthacaContactInteraction(reopened.person);
        reopened.fire("ChiefNavigatorFobIthaca");
        pageBudget(reopened, 0);
        autoPages(reopened);
        return reopened;
    }

    private static void briefing() throws Exception {
        for (List<String> order : List.of(List.of("capture", "disable"), List.of("disable", "capture"))) {
            MenelausFlowRegression.Fixture f = contact(false);
            check(openingCount(f) == 1 && !f.global.getBoolean(BRIEFED)
                            && !f.options.containsKey(UPGRADE),
                    "Reading the briefing must neither accept the Labor nor expose hub services");
            check("\"I accept.\"".equals(f.options.get(PREFIX + "understood")),
                    "The existing acknowledgement ID must offer the supplied acceptance label");
            for (String topic : order) {
                choose(f, PREFIX + topic);
                autoPages(f);
                f.settle();
                check(!f.options.containsKey(PREFIX + topic)
                                && f.options.containsKey(PREFIX + "understood")
                                && !f.global.getBoolean(BRIEFED) && openingCount(f) == 1,
                        "Each briefing question disappears durably without replaying the opening");
            }
            choose(f, PREFIX + "understood");
            check(f.global.getBoolean(BRIEFED) && f.global.getExpire(BRIEFED) == -1f,
                    "Selecting I accept must durably commit acknowledgement before Continue");
            autoPages(f);
            check(f.options.keySet().equals(Set.of(PREFIX + "accept"))
                            && "Continue.".equals(f.options.get(PREFIX + "accept")),
                    "The closing response must preserve its legacy ID with an ordinary Continue");
            f.settle();
            MenelausFlowRegression.Fixture interrupted = reopen(f);
            hub(interrupted);
            check(openingCount(interrupted) == 0,
                    "Reopening after I accept must skip the opening even before its Continue");
            f.install();
            choose(f, PREFIX + "accept");
            hub(f);
            choose(f, PREFIX + "active");
            autoPages(f);
            hub(f);
            check(openingCount(f) == 1, "Status must return to the hub without replaying the briefing");
        }
    }

    private static void optionalTopics() throws Exception {
        MenelausFlowRegression.Fixture history = contact(true);
        choose(history, PREFIX + "history");
        check(!history.local.getBoolean(HISTORY_ASKED),
                "Opening Wall history must not consume an interrupted multi-page answer");
        autoPages(history);
        history.settle();
        hub(history);
        check(history.local.getBoolean(HISTORY_ASKED) && history.local.getExpire(HISTORY_ASKED) == -1f
                        && !history.options.containsKey(PREFIX + "history"),
                "Only the completed history is consumed for the current contact session");
        MenelausFlowRegression.Fixture revisit = reopen(history);
        hub(revisit);
        check(revisit.options.containsKey(PREFIX + "history") && openingCount(revisit) == 0,
                "A new contact restores local topics while retaining durable briefing acceptance");

        for (String motto : List.of("ask", "know", "continue")) {
            for (String ending : List.of("close", "capability", "rest")) {
                MenelausFlowRegression.Fixture f = contact(true);
                choose(f, PREFIX + "helen");
                autoPages(f);
                check(!f.local.getBoolean(HELEN_ASKED)
                                && f.options.keySet().equals(Set.of(PREFIX + "helen_motto_ask",
                                        PREFIX + "helen_motto_know", PREFIX + "helen_motto_continue")),
                        "All three distinct motto routes must appear before Helen is consumed");
                choose(f, PREFIX + "helen_motto_" + motto);
                autoPages(f);
                f.settle();
                check(!f.local.getBoolean(HELEN_ASKED)
                                && f.options.keySet().equals(Set.of(PREFIX + "helen_close", PREFIX + "helen_sinni")),
                        "Every motto route must reach the original close/Sinni choice exactly once");
                check(f.history.stream().filter(text -> text.contains("So you arranged for her transfer.")).count() == 1,
                        "Converging motto aliases must render the common continuation exactly once");
                if (ending.equals("close")) choose(f, PREFIX + "helen_close");
                else {
                    choose(f, PREFIX + "helen_sinni");
                    autoPages(f);
                    check(!f.local.getBoolean(HELEN_ASKED)
                                    && f.options.keySet().equals(Set.of(PREFIX + "helen_capability", PREFIX + "helen_rest")),
                            "Sinni's route must require its capability/rest follow-up before completion");
                    choose(f, PREFIX + "helen_" + ending);
                }
                autoPages(f);
                f.settle();
                hub(f);
                check(f.local.getBoolean(HELEN_ASKED) && f.local.getExpire(HELEN_ASKED) == -1f
                                && !f.options.containsKey(PREFIX + "helen"),
                        "Every completed Helen route must consume only that session's topic");
                check(f.history.stream().anyMatch(text -> text.contains("\"No.\""))
                                && f.history.stream().noneMatch(text -> text.contains("Not entirely.")),
                        "Every Helen route must reach the supplied common final answer");
            }
        }
    }

    private static void services() throws Exception {
        MenelausFlowRegression.Fixture f = contact(true);
        int[] repairs = {0};
        float[] cr = {.15f};
        FleetMemberStatusAPI status = MenelausFlowRegression.mock(FleetMemberStatusAPI.class, (name, values) -> {
            if (name.equals("repairFully")) repairs[0]++;
            return null;
        });
        RepairTrackerAPI repair = MenelausFlowRegression.mock(RepairTrackerAPI.class, (name, values) -> {
            if (name.equals("getCR")) return cr[0];
            if (name.equals("getMaxCR")) return .7f;
            if (name.equals("setCR")) cr[0] = (float) values[0];
            return null;
        });
        f.playerMembers.add(MenelausFlowRegression.mock(FleetMemberAPI.class, (name, values) ->
                name.equals("getStatus") ? status : name.equals("getRepairTracker") ? repair : null));
        choose(f, PREFIX + "repair");
        hub(f);
        check(repairs[0] == 1 && cr[0] == .7f,
                "The preserved repair choice must repair hull/armor and restore maximum CR");
        f.choose(UPGRADE);
        check(f.options.containsKey("chief_navigator_fob_ithaca_upgrades_back"),
                "The upgrade ledger must retain its return route");
        choose(f, "chief_navigator_fob_ithaca_upgrades_back");
        hub(f);
        choose(f, PREFIX + "leave");
        check(f.directoryOpens == 1 && f.dismissals == 0,
                "Leaving Labor III must return the owned contact to Ithaca's native directory");
    }

    private static void nativeConversation() throws Exception {
        SectorAPI oldSector = Global.getSector();
        FactoryAPI oldFactory = Global.getFactory();
        SettingsAPI oldSettings = Global.getSettings();
        try {
            Global.setSettings(MenelausFlowRegression.mock(SettingsAPI.class, (name, values) -> {
                if (!name.equals("getColor")) return null;
                if (values[0].equals("textFriendColor")) return Color.GREEN;
                if (values[0].equals("textEnemyColor")) return Color.RED;
                return Color.WHITE;
            }));
            MenelausFlowRegression.initializeExpressions();
            MenelausFlowRegression.readRules();
            briefing();
            optionalTopics();
            services();
        } finally {
            Global.setSector(oldSector);
            Global.setFactory(oldFactory);
            Global.setSettings(oldSettings);
        }
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

        authoredParity(rules, ink);
        readCatalog(rules);
        nativeConversation();

        System.out.println(
                "PASS: Labor-III Ink/runtime prose, exact read catalogs, native page budgets, acceptance, all nine Helen paths, history, repairs, ledger, revisits and directory return.");
    }
}
