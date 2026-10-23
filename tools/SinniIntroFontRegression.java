import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.ExpressionAPI;
import com.fs.starfarer.api.campaign.rules.Option;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import data.campaign.rulecmd.ChiefNavigatorSinniIntroCMD;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Exercises the installed native FireBest with scoped CSV-text font rendering. */
public final class SinniIntroFontRegression {
    static final Color NEGATIVE = new Color(255, 70, 100);
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object value = call.invoke(name, args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == float.class) return 0f;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    static final class ExpectedFailure extends RuntimeException { }

    static final class Fixture {
        final List<String> events = new ArrayList<>();
        final MemoryAPI local = mock(MemoryAPI.class, (name, args) -> null);
        final Map<String, MemoryAPI> memory = Map.of(MemKeys.LOCAL, local);
        final SectorEntityToken target = mock(SectorEntityToken.class, (name, args) -> null);
        final InteractionDialogAPI dialog;
        final SectorAPI sector;
        String currentFont = "insignia";
        String requestedTrigger;
        boolean noMatch;
        boolean failSelection;
        boolean failRendering;
        boolean failTooltipCommit;
        boolean hasOptions;
        boolean hasScript;
        String csvProse = "Authored CSV text.";
        String substitutedProse;
        String tooltipFont;
        Color paragraphColor;
        Color highlightColor;
        String[] highlights;
        Map<String, String> parameterReplacements = Map.of();

        Fixture() {
            TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class, (name, args) -> {
                if (name.equals("setParaFont")) {
                    tooltipFont = (String) args[0];
                    events.add("tooltipFont:" + tooltipFont);
                }
                if (name.equals("addPara")) {
                    check(args[1].equals(0f), "Keep the inscription's initial paragraph padding zero");
                    events.add("tooltipText:" + args[0]);
                    if (failRendering) throw new ExpectedFailure();
                }
                return null;
            });
            TextPanelAPI text = mock(TextPanelAPI.class, (name, args) -> {
                if (name.startsWith("setFont")) {
                    currentFont = name.equals("setFontSmallInsignia") ? "small_insignia"
                            : name.substring("setFont".length()).toLowerCase();
                    events.add("font:" + currentFont);
                }
                if (name.equals("addParagraph")) {
                    events.add("text:" + currentFont + ":" + args[0]);
                    if (failRendering) throw new ExpectedFailure();
                }
                if (name.equals("addPara")) {
                    events.add("archiveText:" + currentFont + ":" + args[0]);
                    paragraphColor = (Color) args[1];
                    if (args.length == 4) {
                        highlightColor = (Color) args[2];
                        highlights = (String[]) args[3];
                    }
                    if (failRendering) throw new ExpectedFailure();
                }
                if (name.equals("beginTooltip")) {
                    events.add("beginTooltip");
                    return tooltip;
                }
                if (name.equals("addTooltip")) {
                    events.add("addTooltip");
                    if (failTooltipCommit) throw new ExpectedFailure();
                }
                return null;
            });
            dialog = mock(InteractionDialogAPI.class, (name, args) -> {
                if (name.equals("getTextPanel")) return text;
                if (name.equals("getInteractionTarget")) return target;
                return null;
            });
            RuleAPI selected = mock(RuleAPI.class, (name, args) -> {
                if (name.equals("getOptions")) return hasOptions ? List.of(new Option()) : List.of();
                if (name.equals("getScriptCopy")) return hasScript
                        ? List.of(mock(ExpressionAPI.class, (method, params) -> null)) : List.of();
                if (name.equals("getId")) return "csvStyledText";
                if (name.equals("pickText")) return csvProse;
                return null;
            });
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (name.equals("getBestMatching")) {
                    check("parentRule".equals(args[0]), "Forward the calling rule ID");
                    check(args[2] == dialog && args[3] == memory,
                            "Forward the existing dialog and memory unchanged");
                    requestedTrigger = (String) args[1];
                    events.add("trigger:" + requestedTrigger);
                    if (failSelection) throw new ExpectedFailure();
                    return noMatch ? null : selected;
                }
                if (name.equals("performTokenReplacement")) {
                    check(args[2] == target && args[3] == memory,
                            "Native rendering retains CSV row token context");
                    if ("parentRule".equals(args[0])) {
                        return parameterReplacements.getOrDefault((String) args[1], (String) args[1]);
                    }
                    check("csvStyledText".equals(args[0]), "Use the selected CSV rule ID");
                    check(csvProse.equals(args[1]), "Resolve the selected CSV prose");
                    return substitutedProse == null ? args[1] : substitutedProse;
                }
                return null;
            });
            sector = mock(SectorAPI.class, (name, args) -> name.equals("getRules") ? rules : null);
        }

        boolean execute(String... args) {
            List<Token> tokens = new ArrayList<>();
            for (String arg : args) tokens.add(new Token(arg, TokenType.LITERAL));
            return new ChiefNavigatorSinniIntroCMD().execute("parentRule", dialog, tokens, memory);
        }
    }

    static void render(String font) {
        Fixture f = new Fixture();
        Global.setSector(f.sector);
        check(f.execute("styledText", font, "ChiefNavigatorSinniStyledText"),
                "Render a matching CSV subrule");
        check(f.events.equals(List.of("font:" + font,
                        "trigger:ChiefNavigatorSinniStyledText",
                        "text:" + font + ":Authored CSV text.", "font:insignia")),
                "Only CSV text is printed under the requested native font: " + f.events);
    }

    static void restoreOnFailure(String font, boolean duringRendering) {
        Fixture f = new Fixture();
        f.failRendering = duringRendering;
        f.failSelection = !duringRendering;
        Global.setSector(f.sector);
        try {
            f.execute("styledText", font, "ChiefNavigatorSinniStyledText");
            throw new AssertionError("The native rules failure must propagate");
        } catch (ExpectedFailure expected) {
            check(f.currentFont.equals("insignia")
                            && f.events.get(f.events.size() - 1).equals("font:insignia"),
                    "Always restore Insignia after selection or rendering fails");
        }
    }

    static void victor16Text() {
        Fixture f = new Fixture();
        f.csvProse = "For $chiefNavigatorPlayerName.";
        f.substitutedProse = "For Test Captain.";
        Global.setSector(f.sector);
        check(f.execute("styledText", "victor16", "ChiefNavigatorSinniStyledText"),
                "Render larger Victor from a text-only CSV row");
        check(f.events.equals(List.of("trigger:ChiefNavigatorSinniStyledText", "beginTooltip",
                        "tooltipFont:graphics/fonts/victor16.fnt", "tooltipText:For Test Captain.",
                        "addTooltip")) && f.currentFont.equals("insignia"),
                "Use the installed Victor16 font and resolved CSV text without changing dialogue font: " + f.events);
        for (int failure = 0; failure < 3; failure++) {
            Fixture broken = new Fixture();
            broken.failSelection = failure == 0;
            broken.failRendering = failure == 1;
            broken.failTooltipCommit = failure == 2;
            Global.setSector(broken.sector);
            try {
                broken.execute("styledText", "victor16", "ChiefNavigatorSinniStyledText");
                throw new AssertionError("A failed Victor16 render must propagate");
            } catch (ExpectedFailure expected) {
                check(broken.currentFont.equals("insignia"),
                        "Failed custom-font rendering cannot change the normal dialogue font");
                if (failure == 1) check(!broken.events.contains("addTooltip"),
                        "Do not append a partially rendered tooltip");
            }
        }
        for (int invalid = 0; invalid < 4; invalid++) {
            Fixture rejected = new Fixture();
            rejected.noMatch = invalid == 0;
            rejected.hasOptions = invalid == 1;
            rejected.hasScript = invalid == 2;
            if (invalid == 3) rejected.csvProse = " ";
            Global.setSector(rejected.sector);
            check(!rejected.execute("styledText", "victor16", "ChiefNavigatorSinniStyledText")
                            && !rejected.events.contains("beginTooltip")
                            && rejected.currentFont.equals("insignia"),
                    "Reject missing, scripted, option-bearing or empty subrules before rendering");
        }
    }

    static void archiveText() {
        Fixture warning = new Fixture();
        warning.csvProse = "WARNING $captain\n\n// resetting...";
        warning.substitutedProse = "WARNING Test Captain\n\n// resetting...";
        Global.setSector(warning.sector);
        check(warning.execute("archiveText", "warning", "ChiefNavigatorSinniWarning")
                        && warning.events.equals(List.of("trigger:ChiefNavigatorSinniWarning",
                                "font:small_insignia", "archiveText:small_insignia:"
                                        + warning.substitutedProse, "font:insignia"))
                        && NEGATIVE.equals(warning.paragraphColor)
                        && warning.highlightColor == null,
                "Warning uses token-resolved CSV text, SmallInsignia, and native negative color");
        Fixture denied = new Fixture();
        denied.csvProse = "ACCESS $captain. Request permission.";
        denied.substitutedProse = "ACCESS Test Captain. Request permission.";
        denied.parameterReplacements = Map.of("ACCESS $captain.", "ACCESS Test Captain.");
        Global.setSector(denied.sector);
        check(denied.execute("archiveText", "denied", "ChiefNavigatorSinniDenied",
                        "ACCESS $captain.", "permission")
                        && denied.events.equals(List.of("trigger:ChiefNavigatorSinniDenied",
                                "font:small_insignia", "archiveText:small_insignia:"
                                        + denied.substitutedProse, "font:insignia"))
                        && new Color(82, 88, 94).equals(denied.paragraphColor)
                        && NEGATIVE.equals(denied.highlightColor)
                        && Arrays.equals(denied.highlights,
                                new String[]{"ACCESS Test Captain.", "permission"}),
                "Denial matches archive gray with native negative highlights resolved from CSV parameters");
        for (String style : List.of("warning", "denied")) {
            Fixture broken = new Fixture();
            broken.failRendering = true;
            Global.setSector(broken.sector);
            try {
                if (style.equals("warning")) broken.execute("archiveText", style, "Trigger");
                else broken.execute("archiveText", style, "Trigger", "highlight");
                throw new AssertionError("The archive rendering failure must propagate");
            } catch (ExpectedFailure expected) {
                check(broken.currentFont.equals("insignia")
                                && broken.events.get(broken.events.size() - 1).equals("font:insignia"),
                        "Restore Insignia after either archive style fails");
            }
        }
        for (int invalid = 0; invalid < 4; invalid++) {
            Fixture rejected = new Fixture();
            rejected.noMatch = invalid == 0;
            rejected.hasOptions = invalid == 1;
            rejected.hasScript = invalid == 2;
            if (invalid == 3) rejected.csvProse = " ";
            Global.setSector(rejected.sector);
            check(!rejected.execute("archiveText", "warning", "Trigger")
                            && rejected.events.stream().noneMatch(event -> event.startsWith("font:")),
                    "Archive styles reject missing, scripted, option-bearing or blank CSV rows");
        }
        Fixture invalid = new Fixture();
        Global.setSector(invalid.sector);
        check(!invalid.execute("archiveText")
                        && !invalid.execute("archiveText", "unknown", "Trigger")
                        && !invalid.execute("archiveText", "warning", "Trigger", "extra")
                        && !invalid.execute("archiveText", "denied", "Trigger")
                        && !invalid.execute("archiveText", "denied", "Trigger", " ")
                        && !invalid.execute("archiveText", "warning", "")
                        && invalid.events.isEmpty(),
                "Malformed archive arguments cannot change presentation");
    }

    public static void main(String[] args) {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class,
                    (name, values) -> name.equals("getColor")
                            ? (values[0].equals("textEnemyColor") ? NEGATIVE : Color.WHITE) : null));
            Global.setFactory(mock(FactoryAPI.class,
                    (name, values) -> name.equals("createMemory")
                            ? mock(MemoryAPI.class, (method, params) -> null) : null));
            render("orbitron");
            render("victor");
            render("small_insignia");
            victor16Text();
            archiveText();
            restoreOnFailure("orbitron", false);
            restoreOnFailure("orbitron", true);
            restoreOnFailure("small_insignia", false);
            restoreOnFailure("small_insignia", true);
            Fixture f = new Fixture();
            Global.setSector(f.sector);
            check(!f.execute("styledText") && !f.execute("styledText", "orbitron")
                            && !f.execute("styledText", "unknown", "trigger")
                            && !f.execute("styledText", "orbitron", "")
                            && !f.execute("styledText", "orbitron", "trigger", "extra")
                            && f.events.isEmpty(),
                    "Reject incomplete, unsupported or extra arguments without presentation changes");
            f.noMatch = true;
            check(!f.execute("styledText", "victor", "MissingText")
                            && f.currentFont.equals("insignia"),
                    "A missing CSV subrule returns false and restores the normal font");
            System.out.println("PASS: Sinni styled fonts and archive warning/denial match native font/color APIs, preserve CSV token context, reject non-text-only rows and restore normal font after failures.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
            Global.setFactory(previousFactory);
        }
    }
}
