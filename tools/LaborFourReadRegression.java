package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/** The revised Labor IV topic and Continue catalogs must follow actual authored choices. */
public final class LaborFourReadRegression {
    private static boolean focused(String value) {
        return value.contains("labor_four_")
                || value.startsWith("auto_page_chief_navigator_fob_ithaca_sensor_briefing_");
    }

    private static Set<String> select(Set<String> values) {
        Set<String> result = new HashSet<>();
        for (String value : values) if (focused(value)) result.add(value);
        return result;
    }

    public static void main(String[] args) throws Exception {
        Set<String> topics = new HashSet<>(), continuations = new HashSet<>();
        for (var row : DialoguePortraitCoverageRegression.readRules(
                Files.readString(Path.of("data/campaign/rules.csv"))).values()) {
            if (!row.get(0).startsWith("chiefNavigatorMenelausLaborFour")
                    && !row.get(0).startsWith("chiefNavigatorFobIthacaSensorBriefing")) continue;
            for (String option : row.get(5).split("\\R")) {
                if (option.isBlank()) continue;
                String[] parts = option.split(":", 3);
                String suffix = parts[1].substring("chief_navigator_".length());
                if (!focused(suffix)) continue;
                if (parts[2].equals("Continue.")) continuations.add(suffix);
                else if (!parts[2].matches("Request free repairs\\.|\"Nothing for now\\.\""
                        + "|\"I accept\\.\"|\"Understood\\.\"|\"That's all\\.\"")) topics.add(suffix);
            }
        }
        MenelausDialogueReadRegression.check(topics.equals(select(MenelausDialogueReadState.TOPICS)),
                "All actual Labor IV responses, without navigation, need exact topic coverage");
        MenelausDialogueReadRegression.check(continuations.equals(select(MenelausDialogueReadState.CONTINUATIONS)),
                "All actual Labor IV Continue choices must defer read completion");
        SettingsAPI previous = Global.getSettings();
        Global.setSettings(MenelausDialogueReadRegression.mock(SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? Color.YELLOW : null));
        try {
            for (String continuation : continuations) {
                MenelausDialogueReadRegression.Fixture f = new MenelausDialogueReadRegression.Fixture();
                MenelausDialogueReadState state = new MenelausDialogueReadState();
                String topic = "menelaus_labor_four_threat";
                f.page(continuation);
                state.afterPage(f.dialog, f.memory, "chief_navigator_" + topic);
                MenelausDialogueReadRegression.check(f.writes == 0,
                        "A pending answer must not be marked read during " + continuation);
                f.page("menelaus_labor_four_starving");
                state.afterPage(f.dialog, f.memory, "chief_navigator_" + continuation);
                MenelausDialogueReadRegression.check(Boolean.TRUE.equals(
                        f.flags.get("$chief_navigator_menelaus_read_" + topic)),
                        "Read history commits durably only after the final answer page");
            }
        } finally {
            Global.setSettings(previous);
        }
        System.out.println("PASS: exact Labor IV read catalogs and deferred durable completion on all "
                + continuations.size() + " Continue transitions.");
    }
}
