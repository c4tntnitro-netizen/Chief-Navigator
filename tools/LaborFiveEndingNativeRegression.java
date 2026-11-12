package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.SoundPlayerAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import java.awt.Color;
import java.util.*;

/** Runs the actual custom ending shell and every supplied branch through native rules. */
public final class LaborFiveEndingNativeRegression {
    private static void check(boolean value, String message) {
        MenelausFlowRegression.check(value, message);
    }
    private static void choose(MenelausFlowRegression.Fixture f,
            MenelausCompletionDialogPlugin plugin, String id) {
        check(f.options.containsKey(id), "Missing ending choice " + id + "; actual: " + f.options);
        String label = f.options.get(id);
        plugin.optionSelected(label, id);
    }
    private static void pages(MenelausFlowRegression.Fixture f, MenelausCompletionDialogPlugin plugin) {
        for (int page = 0; page < 15; page++) {
            String option = f.options.keySet().stream()
                    .filter(id -> id.startsWith("chief_navigator_auto_page_")).findFirst().orElse(null);
            if (option == null) return;
            check(f.options.size() == 1 && f.options.get(option).equals("Continue."),
                    "Generated pages must present one Continue before authored choices");
            choose(f, plugin, option);
        }
        throw new AssertionError("Unbounded ending pagination");
    }
    public static void main(String[] args) throws Exception {
        SettingsAPI oldSettings = Global.getSettings();
        SoundPlayerAPI oldSound = Global.getSoundPlayer();
        SectorAPI oldSector = Global.getSector();
        SpriteAPI sprite = MenelausFlowRegression.mock(SpriteAPI.class, (name, values) ->
                name.equals("getWidth") ? 480f : name.equals("getHeight") ? 300f : null);
        List<String> audio = new ArrayList<>();
        try {
            Global.setSettings(MenelausFlowRegression.mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getColor")) return Color.WHITE;
                if (name.equals("getSprite")) return sprite;
                if (name.equals("getSpriteName")) return "graphics/illustrations/chief_navigator_sinni_ending.png";
                return null;
            }));
            Global.setSoundPlayer(MenelausFlowRegression.mock(SoundPlayerAPI.class, (name, values) -> {
                audio.add(name); return null;
            }));
            MenelausFlowRegression.initializeExpressions();
            MenelausFlowRegression.readRules();
            List<List<String>> prefixes = new ArrayList<>();
            for (String sleep : List.of("enough", "twelve_hours")) {
                prefixes.add(List.of("sleep_at_all", sleep, "you_checked", "and_the_tea", "tea_thank_you"));
                prefixes.add(List.of("sleep_at_all", sleep, "you_checked", "shawl_thank_you"));
                prefixes.add(List.of("sleep_at_all", sleep, "look_at_ithaca"));
            }
            prefixes.add(List.of("thanks_for_tea", "tea_thank_you"));
            prefixes.add(List.of("stand_beside"));
            Set<String> selected = new HashSet<>();
            int paths = 0;
            for (String pronoun : List.of("he", "she")) {
                for (List<String> prefix : prefixes) {
                    for (String poem : List.of("chatty", "composed_now")) {
                        for (String gift : List.of("like_poem", "tease_poem")) {
                            for (String toast : List.of("cheers_period", "cheers_bang", "down_wine")) {
                                for (String reaction : List.of("spit_it_out", "cough", "what_the_hell")) {
                                    MenelausFlowRegression.Fixture f = new MenelausFlowRegression.Fixture();
                                    f.stage(MenelausTrial.LABOR_COMPLETE, true, true);
                                    f.playerPronoun = pronoun;
                                    f.install();
                                    Map<String, Object> flags = new HashMap<>();
                                    for (String key : f.global.getKeys()) flags.put(key, f.global.get(key));
                                    MenelausCompletionDialogPlugin plugin = new MenelausCompletionDialogPlugin();
                                    f.plugin = plugin;
                                    int audioBefore = audio.size();
                                    plugin.init(f.dialog);
                                    pages(f, plugin);
                                    List<String> route = new ArrayList<>(List.of("aftermath_continue", "waking_continue", "optical_continue"));
                                    route.addAll(prefix);
                                    route.addAll(List.of("surprised", poem, gift,
                                            gift.equals("like_poem") ? "now_talking_end_1" : "now_talking_end_2", toast));
                                    for (String id : route) {
                                        choose(f, plugin, id); selected.add(id); pages(f, plugin);
                                    }
                                    check(f.options.keySet().equals(Set.of("spit_it_out", "cough", "what_the_hell")),
                                            "The prose-free drinking beat exposes all three authored reactions");
                                    check(f.dismissals == 0, "The scene stays open through the toast");
                                    choose(f, plugin, reaction); selected.add(reaction); pages(f, plugin);
                                    check(f.options.keySet().equals(Set.of("ending_close")) && f.dismissals == 0,
                                            "The full Pai Chiu ending is read before the existing closing action");
                                    String history = String.join("\n", f.history);
                                    check(history.contains("REMAINS NOT RECOVERED") && history.contains("Most of the coffins are empty."),
                                            "The memorial sequence is never skipped");
                                    check(history.contains((pronoun.equals("she") ? "She" : "He") + " Speaks and We Move,")
                                                    && history.contains("Onwards, Victory.")
                                                    && history.contains(gift.equals("like_poem")
                                                        ? "Then would the Captain enjoy my second gift as well?"
                                                        : "Then perhaps the Captain would enjoy my second gift instead?"),
                                            "Player pronoun and title resolve in the supplied prose");
                                    check(!history.contains("{He/She}") && !history.contains("{player_title}")
                                                    && !history.contains("$chiefNavigatorLabor5"),
                                            "No unresolved authoring placeholders reach the dialogue");
                                    check(history.split("\"We survived,\" Sinni says.", -1).length == 2,
                                            "The shared observation beat is rendered exactly once per path");
                                    check(history.contains("\"Pai Chiu,\" Sinni says, smiling openly now.")
                                                    && history.contains("Over sixty percent alcohol.")
                                                    && history.contains("In that Sea of Stars."),
                                            "The new final scene survives every drink reaction");
                                    int selectedIndex = f.history.indexOf(f.options.get("ending_close"));
                                    check(selectedIndex < 0, "The unselected final departure is not echoed early");
                                    choose(f, plugin, "ending_close"); selected.add("ending_close");
                                    check(f.dismissals == 1 && f.history.get(f.history.size() - 1).equals("Leave."),
                                            "The real shell echoes its closing choice and dismisses exactly once");
                                    check(new HashSet<>(f.global.getKeys()).equals(flags.keySet())
                                                    && flags.entrySet().stream().allMatch(e -> Objects.equals(f.global.get(e.getKey()), e.getValue())),
                                            "The ending cannot rewrite progress, rewards or saved completion flags");
                                    check(audio.subList(audioBefore, audio.size()).stream().filter("playCustomMusic"::equals).count() == 1,
                                            "Choices never reinstall the music timer or repeat its opening silence");
                                    paths++;
                                }
                            }
                        }
                    }
                }
            }
            check(paths == 576, "Every narrative branch must run for both native pronouns");
            check(selected.containsAll(Set.of("spit_it_out", "cough", "what_the_hell", "thanks_for_tea",
                    "stand_beside", "you_checked", "shawl_thank_you", "cheers_period", "cheers_bang", "down_wine")),
                    "All supplied alternatives must be reachable");
            System.out.println("PASS: 576 actual-shell/native ending paths, both player pronouns, memorial, merged branches, drink reactions, choice echo and close; saved flags preserved.");
        } finally {
            Global.setSettings(oldSettings); Global.setSoundPlayer(oldSound); Global.setSector(oldSector);
        }
    }
}
