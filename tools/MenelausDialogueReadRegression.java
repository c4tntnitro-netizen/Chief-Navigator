package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Read-only option coloring, durable completion, and native-shell integration contracts. */
public final class MenelausDialogueReadRegression {
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                }));
    }

    static final String PREFIX = "chief_navigator_";
    static final String HISTORY = "$chief_navigator_menelaus_read_";
    static final String COLLAPSE = "menelaus_labor_one_collapse";
    static final String HELEN = "menelaus_labor_one_helen";
    static final String CONTINUE = "auto_page_chief_navigator_menelaus_labor_one_collapse_2";

    static final class Fixture {
        final Map<String, Object> flags = new HashMap<>();
        final Map<String, Color> colors = new HashMap<>();
        Set<String> options = Set.of();
        int writes;
        final MemoryAPI memory = mock(MemoryAPI.class, (method, args) -> {
            if (method.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(args[0]));
            if (method.equals("set")) {
                check(args.length == 2 && ((String) args[0]).startsWith(HISTORY)
                                && Boolean.TRUE.equals(args[1]),
                        "Read history must be durable and must never alter any quest state");
                flags.put((String) args[0], args[1]);
                writes++;
                return null;
            }
            throw new AssertionError("Unexpected read-history memory operation: " + method);
        });
        final OptionPanelAPI panel = mock(OptionPanelAPI.class, (method, args) -> {
            check(method.equals("hasOption"), "Styling must not rebuild, clear, or change options");
            return options.contains(args[0]);
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (method, args) -> {
            if (method.equals("getOptionPanel")) return panel;
            check(method.equals("setOptionColor") && options.contains(args[0]),
                    "Only present, authored topic colors may change");
            colors.put((String) args[0], (Color) args[1]);
            return null;
        });
        void page(String... suffixes) {
            options = new HashSet<>();
            for (String suffix : suffixes) options.add(PREFIX + suffix);
            colors.clear();
        }
        Color color(String suffix) { return colors.get(PREFIX + suffix); }
    }

    static void authoringCoverage() throws Exception {
        Map<String, List<String>> rules = DialoguePortraitCoverageRegression.readRules(
                Files.readString(Path.of("data/campaign/rules.csv")));
        Set<String> expected = new HashSet<>(), continuations = new HashSet<>();
        for (List<String> row : rules.values()) {
            if (!row.get(0).matches("chiefNavigator(Menelaus|FobIthaca).*")) continue;
            if (row.get(0).matches("chiefNavigatorFobIthaca(Approach|Station).*")) continue;
            for (String option : row.get(5).split("\\R")) {
                if (option.isBlank()) continue;
                String[] parts = option.split(":", 3);
                String label = parts[2];
                if (label.equals("Continue.")) continuations.add(parts[1].substring(PREFIX.length()));
                else if (!label.matches("^(Back\\.|Leave\\.|Request free repairs\\.|\"Nothing for now\\.\""
                        + "|Accept the final Labor\\.|Begin the final Labor\\.|Follow Captain Kleon\\."
                        + "|Leave Menelaus.*|Leave them with Helen.*"
                        + "|Deliver and install all recovered Ithaca wall upgrades\\."
                        + "|\"I accept\\.\"|\"Understood\\.\"|\"That.s enough for now\\.\"|Enter\\.)$")) {
                    expected.add(parts[1].substring(PREFIX.length()));
                }
            }
        }
        expected.add("ithaca_menelaus_labors");
        check(expected.equals(MenelausDialogueReadState.TOPICS),
                "Every authored Menelaus topic and response, but no navigation/action choice, needs coverage");
        check(continuations.equals(MenelausDialogueReadState.CONTINUATIONS),
                "All actual multi-page continuation IDs must postpone read completion");
        check(rules.get("chiefNavigatorIthacaMenelausGreeting").get(3).replace("\r\n", "\n").equals(
                        "FireAll PopulateOptions\nChiefNavigatorMenelausCMD highlightUnread"),
                "Native directory greetings must color after all their conditional options exist");
        for (String shell : List.of("FobIthacaContactInteraction", "FobIthacaStationInteraction")) {
            String source = Files.readString(Path.of("src/chiefnavigator/quest/" + shell + ".java"));
            check(source.contains("readState.afterPage(dialog, memoryMap.get(MemKeys.GLOBAL), optionData);"),
                    "Both owned contact shells must finish read tracking after successful dispatch");
        }
    }

    static void completionAndReload() {
        Fixture f = new Fixture();
        MenelausDialogueReadState state = new MenelausDialogueReadState();
        f.page(COLLAPSE, HELEN, "fob_ithaca_leave", "menelaus_accept_final", "fob_ithaca_repair");
        f.colors.put(PREFIX + "menelaus_accept_final", Color.RED);
        state.afterPage(f.dialog, f.memory, null);
        check(f.writes == 0 && Color.YELLOW.equals(f.color(COLLAPSE))
                        && Color.YELLOW.equals(f.color(HELEN)), "Opening a menu must not mark topics read");
        check(Color.RED.equals(f.color("menelaus_accept_final"))
                        && f.color("fob_ithaca_leave") == null && f.color("fob_ithaca_repair") == null,
                "Leave, repairs, and the red final-Labor warning must remain untouched");
        f.page(CONTINUE);
        state.afterPage(f.dialog, f.memory, PREFIX + COLLAPSE);
        check(f.writes == 0 && f.colors.isEmpty(), "The opening of a multi-page answer stays unread");
        state.afterPage(f.dialog, f.memory, PREFIX + CONTINUE);
        check(f.writes == 0, "Intermediate Continue pages cannot prematurely commit read history");
        f.page(COLLAPSE, HELEN);
        state.afterPage(f.dialog, f.memory, PREFIX + CONTINUE);
        check(f.writes == 1 && Color.CYAN.equals(f.color(COLLAPSE))
                        && Color.YELLOW.equals(f.color(HELEN)),
                "Finishing one answer restores its default color and leaves other topics yellow");
        state = new MenelausDialogueReadState();
        state.afterPage(f.dialog, f.memory, null);
        check(f.writes == 1 && Color.CYAN.equals(f.color(COLLAPSE)),
                "Read flags survive reopening/reloading without being rewritten");
        state.afterPage(f.dialog, f.memory, PREFIX + HELEN);
        check(f.writes == 2 && Color.CYAN.equals(f.color(HELEN)),
                "A single-page answer becomes read independently");
        String labors = "ithaca_menelaus_labors";
        f.page(labors);
        state.afterPage(f.dialog, f.memory, null);
        check(Color.YELLOW.equals(f.color(labors)), "The Labors launcher starts unread");
        state.afterPage(f.dialog, f.memory, PREFIX + labors);
        check(Color.CYAN.equals(f.color(labors)), "The visited Labor menu becomes read");
        f.flags.put("$chief_navigator_menelaus_labor_i_reported_v1", true);
        state.afterPage(f.dialog, f.memory, null);
        check(Color.YELLOW.equals(f.color(labors)), "The next Labor exposes new unread material");
    }

    static void abandonedAnswer() {
        Fixture f = new Fixture();
        f.page(CONTINUE);
        new MenelausDialogueReadState().afterPage(f.dialog, f.memory, PREFIX + COLLAPSE);
        f.page(COLLAPSE);
        new MenelausDialogueReadState().afterPage(f.dialog, f.memory, null);
        check(f.writes == 0 && Color.YELLOW.equals(f.color(COLLAPSE)),
                "Discarding an interrupted answer must not persist a premature read flag");
    }

    static void optionalDebriefAnswer() {
        String topic = "menelaus_debrief_gautama";
        String page2 = "auto_page_chief_navigator_fob_ithaca_debrief_2";
        String page3 = "auto_page_chief_navigator_fob_ithaca_debrief_3";
        String page4 = "auto_page_chief_navigator_fob_ithaca_debrief_4";
        for (boolean readAnswer : List.of(false, true)) {
            Fixture f = new Fixture();
            MenelausDialogueReadState state = new MenelausDialogueReadState();
            f.page(topic, page3);
            state.afterPage(f.dialog, f.memory, null);
            check(f.writes == 0 && Color.YELLOW.equals(f.color(topic)),
                    "The optional victory question must start yellow without read or quest writes");
            if (readAnswer) {
                f.page(page2); state.afterPage(f.dialog, f.memory, PREFIX + topic);
                f.page(page3); state.afterPage(f.dialog, f.memory, PREFIX + page2);
                check(f.writes == 0, "The optional two-page explanation must not be marked read on entry");
            }
            f.page(page4); state.afterPage(f.dialog, f.memory, PREFIX + page3);
            f.page("fob_ithaca_debrief_leave"); state.afterPage(f.dialog, f.memory, PREFIX + page4);
            check(f.writes == (readAnswer ? 1 : 0),
                    "Finishing the debrief marks only an actually selected Gautama answer read");
            f.page(topic, page3);
            new MenelausDialogueReadState().afterPage(f.dialog, f.memory, null);
            check((readAnswer ? Color.CYAN : Color.YELLOW).equals(f.color(topic)),
                    "Reload preserves read/default styling and leaves a skipped question yellow");
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI old = Global.getSettings();
        Global.setSettings(mock(SettingsAPI.class, (method, values) -> {
            if (method.equals("getColor")) {
                return "buttonText".equals(values[0]) ? Color.CYAN : Color.YELLOW;
            }
            return null;
        }));
        try {
            authoringCoverage();
            completionAndReload();
            abandonedAnswer();
            optionalDebriefAnswer();
            System.out.println("Menelaus unread-option coverage, completion, reopen, interruption and styling checks passed.");
        } finally { Global.setSettings(old); }
    }
}
