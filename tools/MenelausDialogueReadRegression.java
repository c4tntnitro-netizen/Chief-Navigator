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
import java.util.Arrays;
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
    static final String NAVIGATION_LABEL = "^(Back\\.|Leave\\.|Request free repairs\\.|\"Nothing for now\\.\""
            + "|Accept the final Labor\\.|Begin the final Labor\\.|Follow Captain Kleon\\."
            + "|Leave Menelaus.*|Leave them with Helen.*"
            + "|Deliver and install all recovered Ithaca wall upgrades\\."
            + "|\"I accept\\.\"|\"Understood\\.\"|\"That.s enough for now\\.\"|Enter\\.)$";

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
                else if (!label.matches(NAVIGATION_LABEL)) {
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

    static boolean firstContactSuffix(String suffix) {
        return suffix.startsWith("menelaus_first_")
                || suffix.startsWith("auto_page_chief_navigator_menelaus_first_");
    }

    static Set<String> firstContactOnly(Set<String> catalog) {
        Set<String> result = new HashSet<>();
        for (String suffix : catalog) {
            if (firstContactSuffix(suffix)) result.add(suffix);
        }
        return result;
    }

    static void firstContactCatalogCoverage() throws Exception {
        Map<String, List<String>> rules = DialoguePortraitCoverageRegression.readRules(
                Files.readString(Path.of("data/campaign/rules.csv")));
        Set<String> topics = new HashSet<>(), continuations = new HashSet<>();
        for (List<String> row : rules.values()) {
            if (!row.get(0).matches("chiefNavigator(Menelaus|FobIthaca).*")) continue;
            if (row.get(0).matches("chiefNavigatorFobIthaca(Approach|Station).*")) continue;
            for (String option : row.get(5).split("\\R")) {
                if (option.isBlank()) continue;
                String[] parts = option.split(":", 3);
                String suffix = parts[1].substring(PREFIX.length());
                if (!firstContactSuffix(suffix)) continue;
                if (parts[2].equals("Continue.")) continuations.add(suffix);
                else if (!parts[2].matches(NAVIGATION_LABEL)) topics.add(suffix);
            }
        }
        check(topics.equals(firstContactOnly(MenelausDialogueReadState.TOPICS)),
                "First-contact topics must match the actual authored choices, without retired options");
        check(continuations.equals(firstContactOnly(MenelausDialogueReadState.CONTINUATIONS)),
                "First-contact continuations must match every current authored Continue page");
    }

    static void firstContactReadHistory() {
        String topic = "menelaus_first_reception_name";
        String other = "menelaus_first_reception_red_carpet";
        String surviving = "menelaus_first_quiet_power";
        Fixture saved = new Fixture();
        saved.flags.put(HISTORY + surviving, true);
        saved.page(surviving, topic);
        new MenelausDialogueReadState().afterPage(saved.dialog, saved.memory, null);
        check(saved.writes == 0 && Color.CYAN.equals(saved.color(surviving))
                        && Color.YELLOW.equals(saved.color(topic)),
                "Surviving first-contact IDs must reuse their durable keys without migrations");
        for (String continuation : firstContactOnly(MenelausDialogueReadState.CONTINUATIONS)) {
            Fixture f = new Fixture();
            MenelausDialogueReadState state = new MenelausDialogueReadState();
            f.page(continuation);
            state.afterPage(f.dialog, f.memory, PREFIX + topic);
            check(f.writes == 0, "First-contact Continue must defer completion: " + continuation);
            state.afterPage(f.dialog, f.memory, PREFIX + continuation);
            check(f.writes == 0, "Intermediate first-contact pages must remain pending: " + continuation);
            f.page(other);
            state.afterPage(f.dialog, f.memory, PREFIX + continuation);
            check(f.writes == 1 && Boolean.TRUE.equals(f.flags.get(HISTORY + topic))
                            && Color.YELLOW.equals(f.color(other)),
                    "Reaching a choice marks only the selected first-contact answer read");
            f.page(topic, other);
            new MenelausDialogueReadState().afterPage(f.dialog, f.memory, null);
            check(f.writes == 1 && Color.CYAN.equals(f.color(topic))
                            && Color.YELLOW.equals(f.color(other)),
                    "New reception choices must retain independent read history after reopening");
        }
    }

    static boolean laborTwoSuffix(String suffix) {
        return suffix.startsWith("menelaus_labor_two_")
                || suffix.startsWith("auto_page_chief_navigator_menelaus_labor_two_")
                || suffix.startsWith("auto_page_chief_navigator_fob_ithaca_research_briefing_");
    }

    static Set<String> laborTwoOnly(Set<String> catalog) {
        Set<String> result = new HashSet<>();
        for (String suffix : catalog) {
            if (laborTwoSuffix(suffix)) result.add(suffix);
        }
        return result;
    }

    static void laborTwoCatalogCoverage() throws Exception {
        Map<String, List<String>> rules = DialoguePortraitCoverageRegression.readRules(
                Files.readString(Path.of("data/campaign/rules.csv")));
        Set<String> topics = new HashSet<>(), continuations = new HashSet<>();
        for (List<String> row : rules.values()) {
            for (String option : row.get(5).split("\\R")) {
                if (option.isBlank()) continue;
                String[] parts = option.split(":", 3);
                if (!parts[1].startsWith(PREFIX)) continue;
                String suffix = parts[1].substring(PREFIX.length());
                if (!laborTwoSuffix(suffix)) continue;
                if (parts[2].equals("Continue.")) continuations.add(suffix);
                else if (!parts[2].matches(NAVIGATION_LABEL)) topics.add(suffix);
            }
        }
        check(topics.equals(laborTwoOnly(MenelausDialogueReadState.TOPICS)),
                "Labor-II topics must match the authored choices and preserve surviving IDs");
        check(continuations.equals(laborTwoOnly(MenelausDialogueReadState.CONTINUATIONS)),
                "Every Labor-II Continue page must defer read completion");
    }

    static void laborTwoReadHistory() {
        String topic = "menelaus_labor_two_cryo";
        String other = "menelaus_labor_two_helen";
        for (String continuation : laborTwoOnly(MenelausDialogueReadState.CONTINUATIONS)) {
            Fixture f = new Fixture();
            MenelausDialogueReadState state = new MenelausDialogueReadState();
            f.page(continuation);
            state.afterPage(f.dialog, f.memory, PREFIX + topic);
            check(f.writes == 0, "A Labor-II answer stays pending at " + continuation);
            f.page(topic, other);
            state.afterPage(f.dialog, f.memory, PREFIX + continuation);
            check(f.writes == 1 && Color.CYAN.equals(f.color(topic))
                            && Color.YELLOW.equals(f.color(other)),
                    "Only the completed Labor-II topic becomes read after " + continuation);
            new MenelausDialogueReadState().afterPage(f.dialog, f.memory, null);
            check(f.writes == 1 && Color.CYAN.equals(f.color(topic)),
                    "Labor-II read history survives reopening without changing quest flags");
        }
        Fixture saved = new Fixture();
        saved.flags.put(HISTORY + topic, true);
        saved.page(topic, other);
        new MenelausDialogueReadState().afterPage(saved.dialog, saved.memory, null);
        check(saved.writes == 0 && Color.CYAN.equals(saved.color(topic)),
                "Existing Labor-II durable read keys remain valid after the rewrite");
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
            if (Arrays.asList(args).contains("--labor-two-catalog")) {
                laborTwoCatalogCoverage();
                laborTwoReadHistory();
                System.out.println("PASS: Labor-II authored topics, all Continue pages, durable read history and independent topic styling.");
                return;
            }
            if (Arrays.asList(args).contains("--first-contact-catalog")) {
                firstContactCatalogCoverage();
                firstContactReadHistory();
                completionAndReload();
                abandonedAnswer();
                System.out.println("PASS: first-contact authored topics/continuations, pending answers, durable keys and independent reception read history.");
                return;
            }
            authoringCoverage();
            completionAndReload();
            abandonedAnswer();
            optionalDebriefAnswer();
            System.out.println("Menelaus unread-option coverage, completion, reopen, interruption and styling checks passed.");
        } finally { Global.setSettings(old); }
    }
}
