package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.EngagementResultAPI;
import java.awt.Color;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Headless checks that conversation selections remain in the text history. */
public final class DialogueChoiceHistoryRegression {
    private static final Path ROOT = Path.of("").toAbsolutePath();

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    if (returns == short.class) return (short) 0;
                    if (returns == byte.class) return (byte) 0;
                    if (returns == char.class) return (char) 0;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class Fixture {
        final List<String> events = new ArrayList<>();
        final List<String> history = new ArrayList<>();
        final Map<Object, String> options = new HashMap<>();
        final OptionPanelAPI optionPanel = mock(OptionPanelAPI.class,
                (name, args) -> {
                    if (name.equals("clearOptions")) {
                        events.add("clearOptions");
                        options.clear();
                    } else if (name.equals("addOption")) {
                        options.put(args[1], (String) args[0]);
                    }
                    return null;
                });
        final TextPanelAPI textPanel = mock(TextPanelAPI.class, (name, args) -> {
            if (name.equals("clear")) {
                throw new AssertionError("Conversation text history must not be cleared");
            }
            return null;
        });
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class,
                (name, args) -> {
                    if (name.equals("getOptionPanel")) return optionPanel;
                    if (name.equals("getTextPanel")) return textPanel;
                    if (name.equals("addOptionSelectedText")) {
                        check(options.containsKey(args[0]),
                                "The selected option must be echoed before its menu is cleared");
                        history.add(options.get(args[0]));
                        events.add("echo:" + args[0]);
                    }
                    return null;
                });

        void offer(Object id, String label) { options.put(id, label); }
    }

    private static final class Probe extends ChoicePreservingDialogPlugin {
        InteractionDialogAPI currentDialog;
        int initCalls;
        int dispatchCalls;
        String lastText;
        Object lastData;

        @Override
        protected void initConversation(InteractionDialogAPI dialog) {
            currentDialog = dialog;
            initCalls++;
        }

        @Override
        protected void handleOptionSelected(String text, Object data) {
            dispatchCalls++;
            lastText = text;
            lastData = data;
            if (currentDialog != null) {
                currentDialog.getOptionPanel().clearOptions();
            }
        }

        @Override public void optionMousedOver(String text, Object data) { }
        @Override public void advance(float amount) { }
        @Override public void backFromEngagement(EngagementResultAPI result) { }
        @Override public Object getContext() { return null; }
        @Override public Map<String, MemoryAPI> getMemoryMap() { return null; }
    }

    private static void verifySharedDefault() throws Exception {
        check(Modifier.isFinal(ChoicePreservingDialogPlugin.class.getMethod(
                        "init", InteractionDialogAPI.class).getModifiers()),
                "Subclasses must not accidentally bypass conversation binding");
        check(Modifier.isFinal(ChoicePreservingDialogPlugin.class.getMethod(
                        "optionSelected", String.class, Object.class).getModifiers()),
                "Subclasses must not accidentally bypass the default selection echo");

        Fixture first = new Fixture();
        Probe probe = new Probe();
        probe.init(first.dialog);
        first.offer("question", "\"So what do you navigate by?\"");
        probe.optionSelected("\"So what do you navigate by?\"", "question");
        check(first.history.equals(List.of("\"So what do you navigate by?\""))
                        && first.events.equals(List.of("echo:question", "clearOptions"))
                        && probe.dispatchCalls == 1,
                "A genuine choice must be preserved exactly once before dispatch clears its menu");

        first.offer("question", "\"So what do you navigate by?\"");
        probe.optionSelected("\"So what do you navigate by?\"", "question");
        check(first.history.size() == 2 && probe.dispatchCalls == 2,
                "Repeating a genuine choice must append it once on each selection");

        probe.optionSelected(null, "synthetic_stage");
        check(probe.dispatchCalls == 3 && probe.lastText == null
                        && "synthetic_stage".equals(probe.lastData)
                        && first.history.size() == 2,
                "Synthetic null-text dispatch must remain functional without printing a choice");
        probe.optionSelected("No data", null);
        check(probe.dispatchCalls == 4 && "No data".equals(probe.lastText)
                        && probe.lastData == null && first.history.size() == 2,
                "Null-data dispatch must remain functional without printing a choice");
        probe.optionSelected(null, null);
        check(probe.dispatchCalls == 5 && first.history.size() == 2,
                "An entirely synthetic callback must not create text history");

        Fixture second = new Fixture();
        probe.init(second.dialog);
        second.offer("another_question", "Ask another question.");
        probe.optionSelected("Ask another question.", "another_question");
        check(probe.initCalls == 2 && first.history.size() == 2
                        && second.history.equals(List.of("Ask another question."))
                        && second.events.equals(List.of(
                                "echo:another_question", "clearOptions")),
                "Reinitializing a plugin must bind selection history to the new dialog");

        RuleDialogSupport.preserveChoice(null, "No dialog", "question");
        probe.init(null);
        probe.optionSelected("No dialog", "question");
        check(probe.dispatchCalls == 7 && first.history.size() == 2
                        && second.history.size() == 1,
                "A missing presentation dialog must not prevent normal dispatch");
    }

    private static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    /** Stops after verifying real FireBest dispatch, before engine-only rendering. */
    private static final class ReachedRuleEngine extends RuntimeException { }

    private static void verifySinniDispatchOrdering() {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        SettingsAPI previousSettings = Global.getSettings();
        Fixture fixture = new Fixture();
        Map<String, Object> saved = new HashMap<>();
        saved.put("$chief_navigator_sinni_vignette_complete_avici", true);
        MemoryAPI global = memory(saved);
        PersonAPI player = mock(PersonAPI.class, (name, args) ->
                name.equals("getName")
                        ? new FullName("Test", "Captain", FullName.Gender.FEMALE)
                        : null);
        List<String> triggers = new ArrayList<>();
        RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
            if (name.equals("getBestMatching")) {
                String trigger = (String) args[1];
                triggers.add(trigger);
                if (trigger.endsWith("navigation_question")) {
                    check(fixture.history.equals(List.of("\"So what do you navigate by?\"")),
                            "Sinni's answer must reach the rules engine after the selected question is preserved");
                    check(fixture.events.equals(List.of(
                                    "clearOptions", "echo:navigation_question", "clearOptions")),
                            "The real Sinni dispatcher must echo before replacing options");
                }
                throw new ReachedRuleEngine();
            }
            return null;
        });
        try {
            // FireBest's real tokenizer initializes Misc's settings-backed constants.
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? Color.WHITE : null));
            Global.setFactory(mock(FactoryAPI.class, (name, args) ->
                    name.equals("createMemory") ? memory(new HashMap<>()) : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")
                        || name.equals("getPlayerMemoryWithoutUpdate")) return global;
                if (name.equals("getPlayerPerson")) return player;
                if (name.equals("getRules")) return rules;
                return null;
            }));
            SinniSystemVignetteInteraction sinni = new SinniSystemVignetteInteraction(
                    new SinniSystemVignetteScript.Candidate(
                            SinniSystemVignetteScript.Category.ALPHA_ODYSSEY,
                            "Abyssal nebula"), "Test system");
            try {
                sinni.init(fixture.dialog);
                throw new AssertionError("Sinni initialization must dispatch its authored root");
            } catch (ReachedRuleEngine expected) { }
            fixture.offer("navigation_question", "\"So what do you navigate by?\"");
            try {
                sinni.optionSelected("\"So what do you navigate by?\"", "navigation_question");
                throw new AssertionError("Sinni selection must dispatch the authored answer");
            } catch (ReachedRuleEngine expected) { }
            check(triggers.equals(List.of(
                            "ChiefNavigatorSinniVignette_sinni_alpha_odyssey",
                            "ChiefNavigatorSinniVignette_navigation_question")),
                    "Preserving selections must not alter Sinni's authored trigger IDs");
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
    }

    private static String source(String name) throws Exception {
        return Files.readString(ROOT.resolve(
                "src/chiefnavigator/quest/" + name + ".java"), StandardCharsets.UTF_8);
    }

    private static void verifyAllConversationShells() throws Exception {
        Class<?>[] custom = {
            BudaiSalvageDialogPlugin.class,
            DriftingWallStationInteraction.class,
            FobIthacaApproachInteraction.class,
            FobIthacaContactInteraction.class,
            FobIthacaStationInteraction.class,
            GoalStationInteraction.class,
            HegemonyExpeditionInteraction.class,
            HegemonyIthacaCommandScene.class,
            LeagueSurvivorRescueDialogPlugin.class,
            MenelausCompletionDialogPlugin.class,
            OdysseyExpanseInteraction.class,
            OdysseyRescueEscortInteraction.class,
            SinniSystemVignetteInteraction.class,
            TroyDepartureInteraction.class,
            TroyWormholeInteraction.class,
            VeiledSunTransitionInteraction.class
        };
        for (Class<?> conversation : custom) {
            check(ChoicePreservingDialogPlugin.class.isAssignableFrom(conversation),
                    conversation.getSimpleName() + " must inherit selection preservation");
            String code = source(conversation.getSimpleName());
            check(!code.contains("addOptionSelectedText(")
                            && !code.contains("RuleDialogSupport.preserveChoice("),
                    conversation.getSimpleName() + " must not echo the inherited selection twice");
            check(!Pattern.compile("(?:text|(?:getTextPanel\\(\\)))\\s*\\.clear\\s*\\(")
                            .matcher(code).find(),
                    conversation.getSimpleName() + " must not erase conversation text history");
        }

        Pattern directImplementation = Pattern.compile(
                "\\bimplements\\s+[^\\{;]*\\bInteractionDialogPlugin\\b");
        try (var paths = Files.walk(ROOT.resolve("src"))) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (path.getFileName().toString().equals("ChoicePreservingDialogPlugin.java")) {
                    continue;
                }
                check(!directImplementation.matcher(Files.readString(path)).find(),
                        "New direct custom dialog implementations must adopt the shared default: " + path);
            }
        }

        for (String nativeShell : List.of(
                "IthacaSectionInteraction", "IthacaSupportInteraction", "SinniBarEvent")) {
            String code = source(nativeShell);
            check(!code.contains("addOptionSelectedText(")
                            && !code.contains("RuleDialogSupport.preserveChoice("),
                    nativeShell + " must retain native selection handling without a duplicate echo");
        }
        check(!source("SinniBarEvent").contains("text.clear()"),
                "Sinni's bar initialization must retain the native engine's selected introduction");
        check(!source("TroyWormholeInteraction").contains("getTextPanel().clear()"),
                "Troy menu reinitialization must not discard earlier choices");
        System.out.println("Verified all " + custom.length + " custom conversation shells");
    }

    public static void main(String[] args) throws Exception {
        verifySharedDefault();
        verifySinniDispatchOrdering();
        verifyAllConversationShells();
        System.out.println("Dialogue choice-history regression passed");
    }
}
