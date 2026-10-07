import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Integration contracts for rules-driven drone recruitment and Wall support. */
public final class DronePatrolDialogueRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final String RECRUIT = "chief_navigator_drone_patrol_recruit";
    private static final String CONTINUE = "chief_navigator_drone_patrol_continue";
    private static final String LEAVE = "chief_navigator_drone_patrol_leave";

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String read(String path) throws Exception {
        return Files.readString(ROOT.resolve(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        check(start >= 0, "Missing source contract: " + signature);
        int open = source.indexOf('{', start);
        int depth = 1;
        for (int end = open + 1; end < source.length(); end++) {
            if (source.charAt(end) == '{') depth++;
            if (source.charAt(end) == '}') depth--;
            if (depth == 0) {
                return source.substring(open + 1, end);
            }
        }
        throw new AssertionError("Unclosed method: " + signature);
    }

    /** Parses quoted multiline CSV cells and their escaped ASCII quotes. */
    private static Map<String, List<String>> rules() throws Exception {
        String csv = read("data/campaign/rules.csv");
        Map<String, List<String>> result = new LinkedHashMap<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append(c);
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && (c == ',' || c == '\n')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (c == '\n') {
                    if (row.size() >= 6) result.put(row.get(0), row);
                    row = new ArrayList<>();
                }
            } else {
                cell.append(c);
            }
        }
        check(!quoted, "Rules CSV has an unclosed quoted field");
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            if (row.size() >= 6) result.put(row.get(0), row);
        }
        return result;
    }

    private static List<String> rule(Map<String, List<String>> rules, String id) {
        check(rules.containsKey(id), "Missing dialogue rule: " + id);
        return rules.get(id);
    }

    private static void selected(List<String> row, String option, String script) {
        check(row.get(1).equals("DialogOptionSelected")
                        && row.get(2).trim().equals("$option == " + option),
                "Selected option has no exact handler: " + option);
        check(row.get(3).trim().equals(script),
                "Unexpected selected-option effect: " + option);
    }

    private static void verifyPatrolRouting() throws Exception {
        String router = read("src/chiefnavigator/quest/TreadmillCampaignPlugin.java");
        int patrol = router.indexOf("MenelausTrial.isFriendlyDronePatrol(target)");
        int ash = router.indexOf("OdysseyExpanseSystem.ASHEN_VERGE_ID");
        check(patrol >= 0 && patrol < ash
                        && patrol < router.indexOf("isHegemonyExpedition(enemy)"),
                "Owned recruitment patrols must precede ordinary/Ashen fleet routes");
        String branch = router.substring(patrol, router.indexOf(
                "isHegemonyExpedition(enemy)", patrol));
        check(branch.contains("new RuleBasedInteractionDialogPluginImpl(")
                        && branch.contains("\"ChiefNavigatorDronePatrol\"")
                        && branch.contains("PickPriority.HIGHEST"),
                "Patrol interaction must use the rules-authored root");
        String trial = read("src/chiefnavigator/quest/MenelausTrial.java");
        String identity = method(trial,
                "public static boolean isFriendlyDronePatrol(");
        check(identity.contains("instanceof CampaignFleetAPI")
                        && identity.contains("isPlayerControlledFleet(fleet)")
                        && identity.contains("FRIENDLY_DRONE_PATROL_MARKER")
                        && identity.contains("(FRIENDLY_PREFIX + i).equals(fleet.getId())"),
                "Recruitment routing must require exact owned fleet IDs and marker");
    }

    private static void verifyPatrolRules(Map<String, List<String>> rules) {
        List<String> hub = rule(rules, "chiefNavigatorDronePatrol");
        check(hub.get(3).contains("ChiefNavigatorDronePatrolCMD init")
                        && hub.get(3).contains("FireAll ChiefNavigatorDronePatrolOptions")
                        && !hub.get(3).contains("configureStoryOption")
                        && !hub.get(4).contains("story point") && hub.get(5).isBlank(),
                "Friendly contact must not advertise or attach story-point transfers");
        List<String> leave = rule(rules, "chiefNavigatorDronePatrolLeaveOption");
        check(leave.get(1).equals("ChiefNavigatorDronePatrolOptions")
                        && leave.get(2).isBlank()
                        && leave.get(5).contains(":" + LEAVE + ":"),
                "Friendly contacts must always retain Leave");
        for (List<String> row : rules.values()) {
            check(!row.get(5).contains(":" + RECRUIT + ":"),
                    "No runtime rule may expose the retired recruitment option");
        }
        check(!rules.containsKey("chiefNavigatorDronePatrolRecruit")
                        && !rules.containsKey("chiefNavigatorDronePatrolSuccess")
                        && !rules.containsKey("chiefNavigatorDronePatrolRejected")
                        && !rules.containsKey("chiefNavigatorDronePatrolContinue"),
                "The retired transfer/result chain must be absent");
        selected(rule(rules, "chiefNavigatorDronePatrolLeave"), LEAVE, "DismissDialog");
    }

    private static void verifyWallRules(Map<String, List<String>> rules)
            throws Exception {
        List<String> support = rule(rules, "chiefNavigatorWallLaborFiveSupport");
        List<String> normal = rule(rules, "chiefNavigatorWallFriendly");
        String friendly = "ChiefNavigatorDriftingWallCMD stage friendly";
        String available = "ChiefNavigatorDriftingWallCMD supportAvailable";
        check(support.get(1).equals("ChiefNavigatorDriftingWall")
                        && normal.get(1).equals(support.get(1))
                        && support.get(2).equals(friendly + "\n" + available)
                        && normal.get(2).equals(friendly + "\n!" + available)
                        && support.get(3).equals("ChiefNavigatorDriftingWallCMD support"),
                "Automatic Labor-V support must be exclusive with ordinary friendly Wall prose");
        check(support.get(5).contains(":chief_navigator_drifting_wall_leave:")
                        && normal.get(5).contains(":chief_navigator_drifting_wall_leave:"),
                "Both friendly Wall roots must retain a working Leave");
        selected(rule(rules, "chiefNavigatorWallLeave"),
                "chief_navigator_drifting_wall_leave", "DismissDialog");
        String command = read("src/data/campaign/rulecmd/ChiefNavigatorDriftingWallCMD.java");
        check(command.contains("DriftingWallLaborSupport.supportAvailable(station)")
                        && command.contains("DriftingWallLaborSupport.grant(station)"),
                "Wall rules must delegate availability and grant to the same helper");
        String helper = read("src/chiefnavigator/quest/DriftingWallLaborSupport.java");
        String availability = method(helper,
                "public static boolean supportAvailable(");
        check(availability.contains("DriftingWallEncounter.isDriftingWall(wall)")
                        && availability.contains("MenelausTrial.isWallFriendly()")
                        && availability.contains("MenelausTrial.isFinalLaborActive()")
                        && availability.contains("!memory().getBoolean(GRANTED)"),
                "Support needs exact friendly Wall, accepted ongoing Labor V, and one-shot state");
        String trial = read("src/chiefnavigator/quest/MenelausTrial.java");
        String active = method(trial, "public static boolean isFinalLaborActive()");
        check(active.contains("getCurrentLaborStage() == LABOR_GAUTAMA")
                        && active.contains("isFinalLaborBriefed()")
                        && active.contains("!isGautamaDefeated()")
                        && active.contains("!isFinalLaborFailed()"),
                "Labor-V support must not leak before acceptance or after its objective/failure");
        check(method(trial, "public static boolean isFinalLaborBriefed()")
                        .contains("memory().getBoolean(FINAL_LABOR_BRIEFED)"),
                "Active Labor V must retain its explicit acceptance marker");
    }

    public static void main(String[] args) throws Exception {
        Map<String, List<String>> rules = rules();
        verifyPatrolRouting();
        verifyPatrolRules(rules);
        verifyWallRules(rules);
        String support = read("src/chiefnavigator/quest/IthacaSupportInteraction.java");
        check(method(support, "protected void pullInNearbyFleets()")
                        .contains("if (!ongoingBattle) pullInWallDetachment();"),
                "Previewing an ongoing NPC defense must not dispatch escorts before player joins");
        String selected = method(support, "public void optionSelected(");
        check(selected.indexOf("super.optionSelected(optionText, optionData);")
                        < selected.indexOf("OptionId.JOIN_ONGOING_BATTLE")
                        && selected.contains("OptionId.JOIN_ONGOING_BATTLE && joinedBattle")
                        && selected.contains("pullInWallDetachment();"),
                "Owned support joins ongoing defense only after native admits the player");
        System.out.println("Drone patrol dialogue integration regression checks passed.");
    }
}
