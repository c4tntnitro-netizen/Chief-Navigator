package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.ui.*;
import com.fs.starfarer.campaign.rules.C;
import com.fs.starfarer.campaign.rules.oOOO;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import static chiefnavigator.quest.SinniVignetteCompletionRegression.mock;
import static chiefnavigator.quest.SinniVignetteCompletionRegression.check;

/** External RC7 native CSV/expression/FireBest/FireAll checks; no engine internals ship. */
public final class SinniNebulaPaintingRegression {
    static final String PREFIX = "ChiefNavigatorSinniVignette_";
    static final String SAVER = "$chief_navigator_sinni_nebula_screensaver";
    static final Map<String, C> rules = new LinkedHashMap<>();
    static final List<List<String>> paths = new ArrayList<>();

    static void load() throws Exception {
        JSONObject settings = new JSONObject().put("ruleCommandPackages", new JSONArray()
                .put("com.fs.starfarer.api.impl.campaign.rulecmd").put("data.campaign.rulecmd"));
        for (Field f : Class.forName("com.fs.starfarer.settings.StarfarerSettings").getDeclaredFields()) {
            if (f.getType() == JSONObject.class && Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true); f.set(null, settings);
            }
        }
        for (Field f : Class.forName("com.fs.starfarer.loading.scripts.ScriptStore").getDeclaredFields()) {
            if (f.getType() == org.codehaus.janino.JavaSourceClassLoader.class) {
                f.setAccessible(true);
                f.set(null, new org.codehaus.janino.JavaSourceClassLoader(
                        SinniNebulaPaintingRegression.class.getClassLoader()));
            }
        }
        JSONArray data = (JSONArray) Class.forName("com.fs.starfarer.loading.void")
                .getMethod("o00000", String.class).invoke(null,
                        Files.readString(Path.of("data/campaign/rules.csv")));
        for (int i = 0; i < data.length(); i++) {
            JSONObject row = data.getJSONObject(i);
            if (!row.getString("trigger").startsWith(PREFIX)) continue;
            C rule = new C(row.getString("id"), row.getString("trigger"), row.optString("conditions"));
            if (!row.optString("text").isBlank()) rule.getText().add(row.getString("text"));
            for (String line : row.optString("script").split("\\R")) {
                if (!line.isBlank()) rule.getScript().add(new oOOO(line));
            }
            for (String line : row.optString("options").split("\\R")) {
                if (line.isBlank()) continue;
                String[] bits = line.split(":", 3);
                Option option = new Option();
                option.order = Float.parseFloat(bits[0]); option.id = bits[1]; option.text = bits[2];
                rule.getOptions().add(option);
            }
            rules.put(rule.getTrigger(), rule);
        }
    }

    static void enumerate(String trigger, List<String> chosen, Set<String> seen) {
        check(seen.add(trigger), "Cycle at " + trigger);
        C rule = rules.get(trigger);
        check(rule != null, "Missing route " + trigger);
        if (!rule.getOptions().isEmpty()) {
            Set<String> ids = new HashSet<>(), labels = new HashSet<>();
            for (Option option : rule.getOptions()) {
                check(ids.add(option.id) && labels.add(option.text), "Duplicate menu choice");
                List<String> branch = new ArrayList<>(chosen); branch.add(option.id);
                enumerate(PREFIX + option.id, branch, new HashSet<>(seen));
            }
            return;
        }
        for (oOOO script : rule.getScript()) {
            if (script.getString().startsWith("FireAll ")) {
                enumerate(script.getString().substring(8), chosen, new HashSet<>(seen)); return;
            }
            if (script.getString().equals("DismissDialog")) {
                paths.add(chosen); return;
            }
        }
        throw new AssertionError("Dead end at " + trigger);
    }

    static void field(Object object, String name, Object value) throws Exception {
        Field f = object.getClass().getDeclaredField(name); f.setAccessible(true); f.set(object, value);
    }

    static void exercise(List<String> path) throws Exception {
        SinniVignetteCompletionRegression.Fixture gameplay =
                new SinniVignetteCompletionRegression.Fixture(
                        SinniVignetteCompletionRegression.nebula(),
                        SinniVignetteCompletionRegression.blackHole());
        SectorAPI baseSector = Global.getSector();
        Map<String, MemoryAPI> memory = Map.of(MemKeys.GLOBAL, baseSector.getMemoryWithoutUpdate(),
                MemKeys.LOCAL, baseSector.getMemoryWithoutUpdate());
        Map<String, String> options = new LinkedHashMap<>();
        List<String> prose = new ArrayList<>(), captions = new ArrayList<>();
        int[] images = {0}, dismissed = {0}, selected = {0};
        List<float[]> placements = new ArrayList<>();
        PositionAPI position = mock(PositionAPI.class, (name, args) -> {
            if (name.equals("inTL")) placements.add(new float[]{(Float)args[0], (Float)args[1]});
            return null;
        });
        TooltipMakerAPI tooltip = mock(TooltipMakerAPI.class, (name, args) -> {
            if (name.equals("addImage")) {
                check(args[0].toString().endsWith("chief_navigator_sinni_nebula_painting.jpg"), "Correct NASA asset");
                check(args[1].equals(300f) && args[2].equals(300f), "Whole square image");
                check(prose.stream().anyMatch(p -> p.contains("Sinni sits back.")), "Reveal follows final prose");
                images[0]++;
            }
            if (name.equals("addPara")) captions.add((String)args[0]);
            return null;
        });
        CustomPanelAPI panel = mock(CustomPanelAPI.class, (name, args) -> {
            if (name.equals("createUIElement")) return tooltip;
            if (name.equals("addUIElement")) return position;
            return null;
        });
        VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) ->
                name.equals("showCustomPanel") ? panel : null);
        OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (name, args) -> {
            if (name.equals("clearOptions")) options.clear();
            if (name.equals("addOption")) options.put(args[1].toString(), args[0].toString());
            return null;
        });
        TextPanelAPI text = mock(TextPanelAPI.class, (name, args) -> {
            if (name.equals("addParagraph")) prose.add((String)args[0]);
            return null;
        });
        InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getOptionPanel")) return optionPanel;
            if (name.equals("getTextPanel")) return text;
            if (name.equals("getVisualPanel")) return visual;
            if (name.equals("dismiss")) dismissed[0]++;
            if (name.equals("addOptionSelectedText")) selected[0]++;
            return null;
        });
        RulesAPI nativeDispatch = mock(RulesAPI.class, (name, args) -> {
            if (name.equals("getBestMatching")) return rules.get(args[1]);
            if (name.equals("getAllMatching")) {
                C found = rules.get(args[1]); return found == null ? List.of() : List.of(found);
            }
            if (name.equals("performTokenReplacement")) return args[1];
            return null;
        });
        Global.setSector(mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getRules")) return nativeDispatch;
            for (Method method : SectorAPI.class.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == (args == null ? 0 : args.length)) {
                    try { return method.invoke(baseSector, args); }
                    catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
                }
            }
            return null;
        }));
        Global.setSettings(mock(SettingsAPI.class, (name, args) -> {
            if (name.equals("getColor")) return Color.LIGHT_GRAY;
            if (name.equals("getSpriteName")) return "graphics/illustrations/chief_navigator_sinni_nebula_painting.jpg";
            if (name.equals("getScriptClassLoader")) return SinniNebulaPaintingRegression.class.getClassLoader();
            return null;
        }));
        SinniSystemVignetteInteraction plugin = new SinniSystemVignetteInteraction(
                new SinniSystemVignetteScript.Candidate(SinniSystemVignetteScript.Category.NEBULA, "nebula"), "Test");
        field(plugin, "dialog", dialog); field(plugin, "memoryMap", memory);
        // Enter directly at the rules root; unrelated portrait/person creation is outside this fixture.
        check(FireBest.fire(null, dialog, memory, PREFIX+"sinni_nebula"), "Root dispatch");
        for (String id : path) {
            check(options.containsKey(id), "Native dispatch lost option " + id + ": " + options);
            check(!SinniSystemVignetteScript.isComplete(SinniSystemVignetteScript.Category.NEBULA),
                    "Interrupted scenes cannot consume completion");
            check(!gameplay.saved.containsKey(SAVER), "Screensaver must wait for explicit final selection");
            if (options.containsKey("sinni_nebula_art")) check(images[0] == 1, "Artwork before reveal choices");
            plugin.handleOptionSelected(options.get(id), id);
        }
        check(dismissed[0] == 1 && images[0] == 1, "One reveal and one dismissal");
        check(SinniSystemVignetteScript.isComplete(SinniSystemVignetteScript.Category.NEBULA), "Both exits complete nebula");
        check(!SinniSystemVignetteScript.isComplete(SinniSystemVignetteScript.Category.BLACK_HOLE), "Other categories untouched");
        boolean saver = path.get(path.size()-1).equals("complete_nebula_screensaver");
        check(Boolean.parseBoolean(String.valueOf(gameplay.saved.get(SAVER))) == saver,
                "Screensaver choice persisted independently");
        check(captions.equals(List.of("Image: NASA/JPL-Caltech; NASA/ESA,",
                "T. Megeath (University of Toledo), M. Robberto (STScI)")), "Full credit from CSV");
        check(placements.size() == 2 && placements.get(1)[1] > placements.get(0)[1]+300f,
                "Credit physically below artwork");
        check(prose.stream().noneMatch(p -> p.contains("IMPLEMENTATION NOTE") || p.contains("UI IMAGE:")), "No author comments leak");
        check(prose.stream().anyMatch(p -> p.contains("-1% sensor profile")), "Existing reward remains visible");
        SinniVignetteRewards.advance(baseSector.getPlayerFleet());
        var modifier = baseSector.getPlayerFleet().getStats().getSensorProfileMod()
                .getMultBonus("chief_navigator_vignette_nebula_plotting");
        check(modifier != null && modifier.getValue() == 0.99f, "Existing -1% sensor reward applied");
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(SettingsAPI.class, (name, values) -> name.equals("getColor") ? Color.WHITE : null));
        load(); enumerate(PREFIX+"sinni_nebula", List.of(), new HashSet<>());
        check(paths.size() == 192, "Expected every authored route, got " + paths.size());
        for (List<String> path : paths) exercise(path);
        System.out.println("PASS: all 192 nebula routes through native CSV, expressions, FireBest/FireAll, and production completion/UI; reveal timing, credits, interruptions, reward, and both exits.");
    }
}
