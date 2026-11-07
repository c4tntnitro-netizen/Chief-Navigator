package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import java.awt.Color;
import java.util.List;

/** Focused native rule dispatch for the November 7 supplied conversation. */
public final class LaborFourNativeRegression {
    private static final String PREFIX = "chief_navigator_menelaus_labor_four_";
    private static final String BRIEFED = "$chief_navigator_menelaus_labor_four_briefed";

    private static void check(boolean ok, String message) {
        MenelausFlowRegression.check(ok, message);
    }

    private static void pages(MenelausFlowRegression.Fixture f) {
        for (int i = 0; i < 15; i++) {
            String next = f.options.entrySet().stream()
                    .filter(e -> e.getValue().equals("Continue."))
                    .map(e -> e.getKey()).findFirst().orElse(null);
            if (next == null) return;
            check(f.options.size() == 1, "Continue must be the sole page transition");
            f.choose(next);
        }
        throw new AssertionError("Unbounded page continuation");
    }

    private static void choose(MenelausFlowRegression.Fixture f, String suffix) {
        f.choose(PREFIX + suffix);
        pages(f);
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(MenelausFlowRegression.mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        MenelausFlowRegression.initializeExpressions();
        MenelausFlowRegression.readRules();
        verify();
    }

    static void verify() throws Exception {
        for (String decision : List.of("refuse", "choice")) {
            MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
            f.stage(MenelausTrial.LABOR_SENSOR, false, false);
            f.fire("ChiefNavigatorFobIthaca");
            pages(f);
            for (String question : List.of("middle", "package", "middle", "package")) {
                choose(f, question);
                check(f.options.size() == 3, "Briefing questions remain repeatable");
            }
            choose(f, "understood");
            check(f.options.size() == 1 && f.options.containsKey(PREFIX + "sinni_why"),
                    "The supplied mandatory Why question must precede the Sinni decision");
            choose(f, "sinni_why");
            f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_why_2");
            pages(f);
            choose(f, "sinni_" + decision);
            check(String.join("\n", f.history).split("I am not Helen", -1).length == 2,
                    "Sinni states her identity exactly once");
            check(!f.global.getBoolean(BRIEFED), "Prose must not prematurely accept the briefing");
            choose(f, "accept");
            check(f.global.getBoolean(BRIEFED), "Explicit acceptance commits the briefing");
            choose(f, "active");
            choose(f, "reach");
            check(!f.options.containsKey(PREFIX + "reach"), "Completed history is one-shot");
            choose(f, "threat");
            long done = f.options.values().stream().filter(v -> v.equals("\"That's all.\"")).count();
            check(done == 1, "Exactly one That's all choice, with its own return handler");
            choose(f, decision.equals("refuse") ? "threat_done" : "starving");
            check(!f.options.containsKey(PREFIX + "threat"), "Threat history is consumed on either return");
            choose(f, "helen");
            check(f.options.values().contains("\"What was she to you?\""), "Helen question stays selectable");
            f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_helen_2");
            pages(f);
            choose(f, decision.equals("refuse") ? "helen_press" : "helen_sinni");
            check(!f.options.containsKey(PREFIX + "helen"), "Helen exchange ends in the hub");
            check(f.options.containsKey("chief_navigator_fob_ithaca_upgrades")
                            && f.options.containsKey(PREFIX + "repair"), "Contact services remain available");
            f.plugin = new FobIthacaContactInteraction(f.person);
            choose(f, "leave");
            check(f.directoryOpens > 0 || f.dismissals > 0, "Leave returns from the contact");
            check(!String.join("\n", f.history).contains("\\n"), "No literal escaped paragraphs");
            check(!f.global.getBoolean(MenelausTrial.FINAL_LABOR_BRIEFED), "Labor V remains unaccepted");
        }
        System.out.println("PASS: native Labor IV briefing, repeat questions, both Sinni choices, acceptance, all hub topics, both Threat returns, both Helen choices, services and leave.");
    }
}
