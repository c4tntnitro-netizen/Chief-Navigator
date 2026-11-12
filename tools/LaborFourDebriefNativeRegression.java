package chiefnavigator.quest;

import java.util.List;
import java.util.Set;

/** Executes the supplied report and final-Labor handoff through native dispatch. */
public final class LaborFourDebriefNativeRegression {
    private static final String PREFIX = "chief_navigator_";
    private static final String REPORT = "menelaus_labor_four_report_";
    private static void check(boolean ok, String message) {
        MenelausFlowRegression.check(ok, message);
    }
    private static void choose(MenelausFlowRegression.Fixture f, String suffix) {
        f.choose(PREFIX + suffix);
    }
    private static void pages(MenelausFlowRegression.Fixture f) {
        for (int i = 0; i < 15; i++) {
            String next = f.options.keySet().stream()
                    .filter(id -> id.startsWith(PREFIX + "auto_page_")).findFirst().orElse(null);
            if (next == null) return;
            check(f.options.size() == 1 && f.options.get(next).equals("Continue."),
                    "Pagination must expose one Continue before the actual choices");
            f.choose(next);
        }
        throw new AssertionError("Unbounded generated continuation");
    }
    private static MenelausFlowRegression.Fixture pendingReport() throws Exception {
        FinalLaborMusic.resetForGameLoad();
        MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
        f.stage(MenelausTrial.LABOR_SENSOR, true, true);
        f.fire("ChiefNavigatorFobIthaca");
        return f;
    }
    private static void stillLaborFour(MenelausFlowRegression.Fixture f) {
        check(!MenelausTrial.isLaborReported(MenelausTrial.LABOR_SENSOR)
                        && !MenelausTrial.isFinalLaborBriefed() && !MenelausTrial.isFinalLaborActive()
                        && !FinalLaborMusic.isGospelPlaying(),
                "Reading the report cannot complete it, start the invasion, or claim the soundtrack");
    }
    static void verify() throws Exception {
        int paths = 0;
        for (String opening : List.of("eating", "doing")) {
            for (String answer : List.of("learn", "destroy")) {
                for (String reply : List.of("good_luck", "joke", "molon")) {
                    MenelausFlowRegression.Fixture f = pendingReport();
                    check(f.options.keySet().equals(Set.of(PREFIX + REPORT + "eating", PREFIX + REPORT + "doing")),
                            "Both supplied opening responses must be available with unique IDs");
                    stillLaborFour(f);
                    choose(f, REPORT + opening);
                    check(f.options.size() == 2 && f.options.containsKey(PREFIX + REPORT + "learn")
                                    && f.options.containsKey(PREFIX + REPORT + "destroy"),
                            "Either opening response converges on the same two explanations");
                    choose(f, REPORT + answer);
                    check(String.join("\n", f.history).contains(answer.equals("learn")
                                    ? "Precisely. The same process" : "No. Destruction alone cannot account"),
                            "The selected explanation preserves its own response");
                    stillLaborFour(f);
                    choose(f, REPORT + "revelation"); pages(f);
                    stillLaborFour(f);
                    String reportText = String.join("\n", f.history);
                    check(reportText.contains("They have reproduced much of the Gate's transit architecture.")
                                    && reportText.contains("The threat will pour from every Gate.")
                                    && reportText.contains("outer approaches of Lacedaemon")
                                    && reportText.contains("All four formations are approaching Ithaca's defensive perimeter."),
                            "The complete supplied revelation is rendered in order");
                    choose(f, "menelaus_labor_5_realization_3");
                    stillLaborFour(f);
                    check(f.options.keySet().equals(Set.of(PREFIX + "menelaus_report_sensor")),
                            "The final report Continue remains the only stage-commit boundary");
                    choose(f, "menelaus_report_sensor"); pages(f);
                    check(MenelausTrial.isLaborReported(MenelausTrial.LABOR_SENSOR)
                                    && !MenelausTrial.isFinalLaborActive() && !MenelausTrial.isFinalLaborBriefed()
                                    && !FinalLaborMusic.isGospelPlaying(),
                            "Committing Labor IV offers Labor V without accepting it");
                    check(f.options.containsKey(PREFIX + "menelaus_accept_final")
                                    && f.options.containsKey(PREFIX + "fob_ithaca_leave")
                                    && f.options.containsKey(PREFIX + "fob_ithaca_repair")
                                    && f.options.containsKey(PREFIX + "fob_ithaca_upgrades"),
                            "The offer retains explicit acceptance, refusal and existing services");
                    choose(f, "menelaus_accept_final");
                    check(MenelausTrial.isFinalLaborActive() && MenelausTrial.isFinalLaborBriefed()
                                    && FinalLaborMusic.isGospelPlaying(),
                            "Only actual acceptance starts the final Labor and its music");
                    check(String.join("\n", f.history).contains("Fight with us until the last light."),
                            "The supplied acceptance speech is retained");
                    choose(f, "menelaus_labor_five_depart"); pages(f);
                    check(String.join("\n", f.history).contains("Captain Avery Kestrel. Come with me.")
                                    && !String.join("\n", f.history).contains("$chiefNavigatorLaborFive"),
                            "The player address resolves from native memory on the later departure page");
                    choose(f, "menelaus_labor_five_field_briefing"); pages(f);
                    choose(f, "menelaus_labor_five_hostswarms"); pages(f);
                    choose(f, "menelaus_labor_five_breach");
                    choose(f, "menelaus_labor_five_line");
                    check(f.options.size() == 3, "Kleon's three supplied responses are all reachable");
                    choose(f, "menelaus_labor_five_line_" + reply); pages(f);
                    check(String.join("\n", f.history).split("Spartan Actual. Go.", -1).length == 2,
                            "Every reply converges on exactly one corridor departure");
                    check(f.options.keySet().equals(Set.of(PREFIX + "menelaus_labor_five_begin")),
                            "The completed handoff ends with the existing Begin action");
                    f.plugin = new FobIthacaContactInteraction(f.person);
                    choose(f, "menelaus_labor_five_begin");
                    check(f.directoryOpens > 0 || f.dismissals > 0, "Begin closes the briefing through its existing callback");
                    check(MenelausTrial.isFinalLaborActive(), "Closing the dialogue preserves the accepted mission");
                    paths++;
                }
            }
        }
        MenelausFlowRegression.Fixture interrupted = pendingReport();
        choose(interrupted, REPORT + "doing");
        choose(interrupted, REPORT + "destroy");
        choose(interrupted, REPORT + "revelation");
        interrupted.settle(); interrupted.fire("ChiefNavigatorFobIthaca");
        check(interrupted.options.containsKey(PREFIX + REPORT + "eating"),
                "An interrupted report reopens from its beginning");
        stillLaborFour(interrupted);
        choose(interrupted, REPORT + "eating"); choose(interrupted, REPORT + "learn");
        choose(interrupted, REPORT + "revelation"); pages(interrupted);
        choose(interrupted, "menelaus_labor_5_realization_3"); choose(interrupted, "menelaus_report_sensor"); pages(interrupted);
        interrupted.plugin = new FobIthacaContactInteraction(interrupted.person);
        choose(interrupted, "fob_ithaca_leave"); interrupted.settle(); interrupted.fire("ChiefNavigatorFobIthaca"); pages(interrupted);
        check(interrupted.options.containsKey(PREFIX + "menelaus_accept_final")
                        && !MenelausTrial.isFinalLaborActive(), "Declining and returning preserves the dormant final offer");
        check(paths == 12, "Exercise every opening, explanation and Kleon reply combination");
        System.out.println("PASS: 12 native report-to-Labor-V paths, interruptions, services, dormant offer and acceptance boundary.");
    }
}
