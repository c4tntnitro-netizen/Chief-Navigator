package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import java.awt.Color;
import java.util.*;

/** Exercises the supplied Helen revision through the contact shell and native rules. */
public final class HelenFinalNativeRegression {
    private static final String PREFIX = "chief_navigator_menelaus_helen_";
    private static final String HEARD = "$chief_navigator_menelaus_helen_final_heard_v2";
    private static void check(boolean ok, String message) {
        MenelausFlowRegression.check(ok, message);
    }
    private static void choose(MenelausFlowRegression.Fixture f,
            FobIthacaContactInteraction plugin, String suffix) {
        String id = suffix.startsWith("chief_navigator_") ? suffix : PREFIX + suffix;
        check(f.options.containsKey(id), "Missing Helen option " + id + ": " + f.options);
        plugin.optionSelected(f.options.get(id), id);
    }
    private static void pages(MenelausFlowRegression.Fixture f, FobIthacaContactInteraction plugin) {
        for (int page = 0; page < 15; page++) {
            String id = f.options.keySet().stream()
                    .filter(value -> value.startsWith("chief_navigator_auto_page_")).findFirst().orElse(null);
            if (id == null) return;
            check(f.options.size() == 1 && "Continue.".equals(f.options.get(id)),
                    "Generated pages must defer authored choices until their final page");
            check(!f.global.getBoolean(HEARD), "A page cannot consume the final Helen conversation");
            choose(f, plugin, id);
        }
        throw new AssertionError("Unbounded Helen pagination");
    }
    private static FobIthacaContactInteraction open(MenelausFlowRegression.Fixture f) {
        f.install();
        FobIthacaContactInteraction plugin = new FobIthacaContactInteraction(f.person);
        f.plugin = plugin;
        plugin.init(f.dialog);
        choose(f, plugin, "chief_navigator_ithaca_menelaus_labors");
        return plugin;
    }
    private static Map<String, Object> story(MenelausFlowRegression.Fixture f) {
        Map<String, Object> result = new HashMap<>();
        for (String key : f.global.getKeys()) {
            if (!key.startsWith("$chief_navigator_menelaus_read_")) result.put(key, f.global.get(key));
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings = Global.getSettings();
        SectorAPI oldSector = Global.getSector();
        try {
            Global.setSettings(MenelausFlowRegression.mock(SettingsAPI.class, (name, values) ->
                    name.equals("getColor") ? Color.WHITE : null));
            MenelausFlowRegression.initializeExpressions();
            MenelausFlowRegression.readRules();
            int paths = 0;
            for (String opening : List.of("changed", "changed_2", "changed_3")) {
                for (String comparison : List.of("changed_5", "changed_5_children")) {
                    for (String explanation : List.of("sinni_explanation", "sinni_explanation_2")) {
                        for (String conclusion : List.of("sinni_punch", "kenosis_man", "kenosis_home")) {
                            MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
                            f.stage(MenelausTrial.LABOR_COMPLETE, true, true);
                            FobIthacaContactInteraction plugin = open(f);
                            check(f.options.containsKey(PREFIX + "final"),
                                    "Completed Labor V exposes the existing optional Helen conversation");
                            Map<String, Object> before = story(f);
                            choose(f, plugin, "final"); pages(f, plugin);
                            choose(f, plugin, "navarch"); pages(f, plugin);
                            choose(f, plugin, "navarch_2"); pages(f, plugin);
                            check(f.options.keySet().equals(Set.of(PREFIX + "changed", PREFIX + "changed_2", PREFIX + "changed_3")),
                                    "All three authored reactions use distinct option IDs");
                            choose(f, plugin, opening); pages(f, plugin);
                            check(f.options.keySet().equals(Set.of(PREFIX + "changed_5", PREFIX + "changed_5_children")),
                                    "All reactions reach the two separate Alpha/human questions");
                            choose(f, plugin, comparison); pages(f, plugin);
                            choose(f, plugin, "changed_6"); pages(f, plugin);
                            choose(f, plugin, explanation); pages(f, plugin);
                            String explanationText = String.join("\n", f.history);
                            check(explanationText.contains(explanation.equals("sinni_explanation")
                                            ? "It means ‘self-emptying,’ captain." : "I know stuff too."),
                                    "Both supplied explanation choices retain their authored routes and resolved title");
                            choose(f, plugin, "kenosis_continue"); pages(f, plugin);
                            choose(f, plugin, conclusion.equals("kenosis_home") ? "kenosis_home" : "kenosis_love"); pages(f, plugin);
                            if (!conclusion.equals("kenosis_home")) { choose(f, plugin, conclusion); pages(f, plugin); }
                            choose(f, plugin, "questions"); pages(f, plugin);
                            choose(f, plugin, "diary"); pages(f, plugin);
                            check(f.options.keySet().equals(Set.of(PREFIX + "diary_2")),
                                    "The diary preserves the new explicit player-look beat");
                            choose(f, plugin, "diary_2"); pages(f, plugin);
                            check(f.options.keySet().equals(Set.of(PREFIX + "end")),
                                    "Every branch reaches the original journal departure action");
                            String history = String.join("\n", f.history);
                            check(history.split("Sinni speaks before Menelaus can continue.", -1).length == 2,
                                    "Converging aliases render the revised shared reply exactly once");
                            check(history.contains("AUTHORITY: STRATEGOS KALI MOLINA")
                                            && history.contains("My first and only rebellion against my nation.")
                                            && history.contains("You are not Helen.")
                                            && history.contains("an... annoying motherfucker.")
                                            && history.contains("And Menelaus listens."),
                                    "The revised manifest, confession and complete diary ending survive every route");
                            check(history.split("You look at Menelaus\\.", -1).length == 2,
                                    "The native shell echoes the selected look once without repeating it in narration");
                            check(!history.contains("{player_title}") && !history.contains("You met my great-grandmother."),
                                    "No unresolved variable or retired converging reply remains");
                            check(story(f).equals(before) && f.dismissals == 0,
                                    "Reading does not change quest, reward, ending or one-time flags");
                            choose(f, plugin, "end");
                            check(f.global.getBoolean(HEARD) && f.dismissals == 1,
                                    "Only explicit final departure durably completes the scene and closes once");
                            Map<String, Object> after = story(f); after.remove(HEARD);
                            check(after.equals(before), "Completion changes only its existing heard marker");
                            f.settle();
                            check(f.global.getBoolean(HEARD), "The native completion assignment survives unpausing");
                            open(f);
                            check(!f.options.containsKey(PREFIX + "final"), "A completed conversation is never re-offered");
                            paths++;
                        }
                    }
                }
            }
            check(paths == 36, "Every authored branch combination must execute");
            for (int stage : List.of(MenelausTrial.LABOR_SENSOR, MenelausTrial.LABOR_GAUTAMA)) {
                MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
                f.stage(stage, true, true); open(f);
                check(!f.options.containsKey(PREFIX + "final"),
                        "Neither Labor IV nor unreported Labor V victory offers Helen's finale");
            }
            MenelausFlowRegression.Fixture interrupted = new MenelausFlowRegression.Fixture();
            interrupted.stage(MenelausTrial.LABOR_COMPLETE, true, true);
            FobIthacaContactInteraction plugin = open(interrupted);
            choose(interrupted, plugin, "final"); pages(interrupted, plugin);
            choose(interrupted, plugin, "navarch");
            interrupted.dialog.dismiss();
            check(!interrupted.global.getBoolean(HEARD), "Closing an interrupted contact keeps the scene pending");
            interrupted.settle(); plugin = open(interrupted);
            choose(interrupted, plugin, "final");
            check(!interrupted.global.getBoolean(HEARD)
                            && interrupted.history.get(interrupted.history.size() - 1).contains("Sinni looks from you to Menelaus."),
                    "An interrupted conversation reopens at its beginning without resetting progress");
            System.out.println("PASS: 36 actual-contact-shell/native Helen paths, shared aliases, diary beat, deferred completion, restart and post-Labor V gate; quest/reward/ending state preserved.");
        } finally {
            Global.setSector(oldSector); Global.setSettings(oldSettings);
        }
    }
}
