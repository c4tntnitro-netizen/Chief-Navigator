package chiefnavigator.quest;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetTextHighlightColors;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetTextHighlights;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.campaign.rules.Memory;
import com.fs.starfarer.campaign.rules.oOOO;
import com.fs.starfarer.campaign.CampaignEngine;
import com.fs.starfarer.campaign.CampaignClock;
import data.campaign.rulecmd.ChiefNavigatorMenelausCMD;
import data.campaign.rulecmd.ChiefNavigatorPeopleCMD;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;
import sun.misc.Unsafe;

/**
 * Parsed authored rules executed by vanilla FireAll/FireBest, native expressions
 * and native expiring memory. Only campaign/UI repositories are mocked. Engine
 * internals and reflection are confined to this external headless test harness.
 * Run in a separate JVM with -Xverify:none for the game's obfuscated engine
 * fields and -Dlog4j.defaultInitOverride=true to disable its file-log bootstrap.
 * Neither setting belongs in the game's launch options or production code.
 * Run this test in its own JVM with -Xverify:none: Starsector's internal engine
 * jar intentionally contains obfuscated identifiers rejected by stock verification.
 */
public final class MenelausFlowRegression {
    private static final String INTRO = "$chief_navigator_menelaus_intro_shown_v2";
    private static final String REPORTED = "$chief_navigator_menelaus_labor_";
    private static final String MIGRATION = "$chief_navigator_menelaus_labor_reports_migrated_v1";
    private static final String UPGRADE = "chief_navigator_fob_ithaca_upgrades";
    private static final String LABORS = "chief_navigator_ithaca_menelaus_labors";
    private static final String DIRECTORY = "chief_navigator_ithaca_directory_back";
    private static final List<Row> ROWS = new ArrayList<>();
    private static int pagesVisited;

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == float.class) return 0f;
                    if (result == long.class) return 0L;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static String field(Class<?> type, String name) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    /** Native expression's static package inventory needs a headless settings document. */
    private static void initializeExpressions() throws Exception {
        JSONObject settings = new JSONObject().put("ruleCommandPackages", new JSONArray()
                .put("com.fs.starfarer.api.impl.campaign.rulecmd").put("data.campaign.rulecmd"));
        Class<?> nativeSettings = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
        for (Field f : nativeSettings.getDeclaredFields()) {
            if (f.getType() == JSONObject.class && Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                f.set(null, settings);
            }
        }
        // A native memory tick reads only the engine pause bit and clock. Avoid
        // constructing the whole graphics/economy/game engine in this process.
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        CampaignEngine engine = (CampaignEngine) unsafe.allocateInstance(CampaignEngine.class);
        Field clock = CampaignEngine.class.getDeclaredField("clock");
        clock.setAccessible(true);
        clock.set(engine, new CampaignClock());
        Field engineInstance = CampaignEngine.class.getDeclaredField("instance");
        engineInstance.setAccessible(true);
        engineInstance.set(null, engine);
        // Crucially, zero is an immediate-expiry interval, not a persistent flag.
        Memory proof = new Memory();
        Map<String, MemoryAPI> scope = Map.of(MemKeys.LOCAL, proof, MemKeys.GLOBAL, proof);
        new oOOO("$global.expiring = true 0").execute("expiry_proof", null, scope);
        check(proof.getBoolean("$expiring"), "Native assignment must initially exist");
        proof.advance(0.1f);
        check(!proof.contains("$expiring"), "Native trailing zero must expire after unpause");
        new oOOO("$global.durable = true").execute("durability_proof", null, scope);
        proof.advance(100f);
        check(proof.getBoolean("$durable"), "Native omitted duration must be durable");
    }

    private static final class Row {
        final String id, trigger, conditions, script, text;
        final List<Option> options = new ArrayList<>();
        Row(List<String> cells) {
            id = cells.get(0); trigger = cells.get(1); conditions = cells.get(2);
            script = cells.get(3); text = cells.get(4);
            for (String line : cells.get(5).split("\\R")) {
                if (line.isBlank()) continue;
                String[] pieces = line.split(":", 3);
                check(pieces.length == 3, "Malformed authored option in " + id);
                Option option = new Option();
                option.order = Float.parseFloat(pieces[0]);
                option.id = pieces[1]; option.text = pieces[2];
                options.add(option);
            }
        }

        boolean matches(Fixture f, Map<String, MemoryAPI> memory) {
            for (String line : conditions.split("\\R")) {
                if (line.isBlank()) continue;
                line = line.replaceAll("\\s+score:[0-9]+$", "");
                boolean negated = line.startsWith("!ChiefNavigatorMenelausCMD ");
                String commandLine = negated ? line.substring(1) : line;
                if (commandLine.startsWith("ChiefNavigatorMenelausCMD ")) {
                    boolean result = new ChiefNavigatorMenelausCMD().execute(id, null,
                            Misc.tokenize(commandLine.substring("ChiefNavigatorMenelausCMD ".length())), memory);
                    if (result == negated) {
                        return false;
                    }
                } else {
                    check(line.startsWith("$") || line.startsWith("!$"),
                            "Unrecognized condition in tested authored graph: " + id + ": " + line);
                    if (!new oOOO(line).isTrueFor(id, f.dialog, memory)) return false;
                }
            }
            return true;
        }

        RuleAPI nativeRule(Fixture f) {
            return mock(RuleAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getTrigger")) return trigger;
                if (name.equals("getOptions")) return options;
                if (name.equals("pickText")) { f.applied.add(id); pagesVisited++; return text; }
                if (name.equals("getText")) return List.of(text);
                if (name.equals("getScriptCopy")) return List.of();
                if (name.equals("runScript")) {
                    @SuppressWarnings("unchecked")
                    Map<String, MemoryAPI> memory = (Map<String, MemoryAPI>) args[1];
                    f.runScript(this, memory);
                }
                return null;
            });
        }
    }

    private static void readRules() throws Exception {
        String csv = Files.readString(Path.of("data/campaign/rules.csv"), StandardCharsets.UTF_8);
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') { cell.append(c); i++; }
                else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\n' || c == '\r')) {
                row.add(cell.toString()); cell.setLength(0);
                if (c != ',') {
                    if (!row.get(0).isBlank() && !row.get(0).equals("id")) ROWS.add(new Row(row));
                    row.clear();
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted && row.isEmpty(), "Authored rules must end with balanced CSV fields");
    }

    private static final class Fixture {
        final Memory global = new Memory(), local = new Memory(), entity = new Memory();
        final Map<String, MemoryAPI> scope = new LinkedHashMap<>();
        final Map<String, Memory> castMemory = new LinkedHashMap<>();
        final Map<String, PersonAPI> castPeople = new LinkedHashMap<>();
        final Map<String, String> options = new LinkedHashMap<>();
        final List<String> applied = new ArrayList<>(), history = new ArrayList<>();
        final List<IntelInfoPlugin> intel = new ArrayList<>();
        int dismissals, directoryOpens;
        int availableComponents;
        int availableMask = -1;
        String[] textHighlights = new String[0];
        Color[] textHighlightColors = new Color[0];
        InteractionDialogPlugin plugin;
        PersonAPI active;
        MarketAPI attached;
        final PersonAPI person = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getId")) return MenelausTrial.PERSON_ID;
            if (name.equals("getName")) return new FullName("Menelaus", "", FullName.Gender.ANY);
            if (name.equals("getMemoryWithoutUpdate")) return local;
            return null;
        });
        final PersonAPI player = castPerson("player", "Captain", "");
        final PersonAPI sinni = castPerson(SinniContact.PERSON_ID, "Sinni", "");
        final PersonAPI aias = castPerson(FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID, "Aias", "Kleon");
        final PersonAPI voss = castPerson(OdysseyStrandedFleetsScript.VOSS_PERSON_ID, "Oren", "Voss");

        private PersonAPI castPerson(String id, String first, String last) {
            Memory memory = new Memory();
            castMemory.put(id, memory);
            PersonAPI result = mock(PersonAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getName")) return new FullName(first, last, FullName.Gender.ANY);
                if (name.equals("getNameString")) return last.isEmpty() ? first : first + " " + last;
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPortraitSprite")) return "graphics/portraits/regression_" + id + ".png";
                return null;
            });
            castPeople.put(id, result);
            return result;
        }
        final SectorEntityToken station = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getLocation")) return new Vector2f();
            if (name.equals("getId")) return "chief_navigator_fob_ithaca";
            if (name.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
            if (name.equals("getMemoryWithoutUpdate")) return entity;
            if (name.equals("getActivePerson")) return active;
            if (name.equals("setActivePerson")) active = (PersonAPI) args[0];
            if (name.equals("getMarket")) return attached;
            if (name.equals("setMarket")) attached = (MarketAPI) args[0];
            return null;
        });
        final OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (name, args) -> {
            if (name.equals("clearOptions")) options.clear();
            if (name.equals("addOption")) {
                String prior = options.put((String) args[1], (String) args[0]);
                check(prior == null, "Overlapping rules added duplicate option: " + args[1]);
            }
            return null;
        });
        final TextPanelAPI textPanel = mock(TextPanelAPI.class, (name, args) -> {
            if (name.equals("clear")) throw new AssertionError("Conversation must retain history");
            if (name.equals("addParagraph") || name.equals("addPara")) {
                history.add((String) args[0]);
                textHighlights = new String[0];
                textHighlightColors = new Color[0];
            }
            if (name.equals("highlightInLastPara")) {
                textHighlights = ((String[]) args[args.length - 1]).clone();
            }
            if (name.equals("setHighlightColorsInLastPara")) {
                textHighlightColors = ((Color[]) args[0]).clone();
            }
            return null;
        });
        final VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> null);
        final CommDirectoryAPI directory = mock(CommDirectoryAPI.class, (name, args) -> null);
        final MarketAPI carrier = mock(MarketAPI.class, (name, args) -> name.equals("getCommDirectory") ? directory : null);
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getInteractionTarget")) return station;
            if (name.equals("getOptionPanel")) return optionPanel;
            if (name.equals("getTextPanel")) return textPanel;
            if (name.equals("getVisualPanel")) return visual;
            if (name.equals("getPlugin")) return plugin;
            if (name.equals("setPlugin")) plugin = (InteractionDialogPlugin) args[0];
            if (name.equals("showCommDirectoryDialog")) directoryOpens++;
            if (name.equals("dismiss")) dismissals++;
            if (name.equals("addOptionSelectedText")) history.add(options.get(args[0]));
            return null;
        });

        Fixture() throws Exception {
            global.set(MIGRATION, true);
            global.set("$chief_navigator_menelaus_created_v1", true);
            global.set("$chief_navigator_ithaca_spartan_captain_created_v1", true);
            global.set(field(IthacaResearchUpgrades.class, "CONSUMABLE_MIGRATION_KEY"), true);
            local.set(FobIthacaContacts.MENELAUS_MARKER, true);
            local.set("$chief_navigator_ithaca_contact_resident", true);
            entity.set("$chief_navigator_fob_ithaca_owned_v1", true);
            castMemory.get(SinniContact.PERSON_ID).set("$chief_navigator_sinni_owned_v1", true);
            castMemory.get(FobIthacaContacts.SPARTAN_CAPTAIN_PERSON_ID)
                    .set(FobIthacaContacts.SPARTAN_CAPTAIN_MARKER, true);
            castMemory.get(OdysseyStrandedFleetsScript.VOSS_PERSON_ID)
                    .set("$chief_navigator_admiral_voss_owned_v1", true);
            scope.put(MemKeys.GLOBAL, global); scope.put(MemKeys.PLAYER, global);
            scope.put(MemKeys.ENTITY, entity); scope.put(MemKeys.LOCAL, local);
        }

        void install() {
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, args) -> {
                if (name.equals("getPerson") && MenelausTrial.PERSON_ID.equals(args[0])) return person;
                if (name.equals("getPerson") && castPeople.containsKey(args[0])) return castPeople.get(args[0]);
                if (name.equals("getPerson")) return mock(PersonAPI.class, (personMethod, personArgs) -> {
                    if (personMethod.equals("getId")) return args[0];
                    if (personMethod.equals("getMemoryWithoutUpdate")) return new Memory();
                    if (personMethod.equals("getName")) return new FullName("Other", "", FullName.Gender.ANY);
                    return null;
                });
                if (name.equals("getPeopleCopy")) return List.of();
                return null;
            });
            CargoAPI cargo = mock(CargoAPI.class, (name, args) -> {
                if (!name.equals("getQuantity")) return null;
                if (availableMask < 0) return availableComponents > 0 ? 1f : 0f;
                int index = Arrays.asList(IthacaResearchUpgrades.COMPONENT_IDS)
                        .indexOf(((SpecialItemData) args[1]).getId());
                return index >= 0 && (availableMask & (1 << index)) != 0 ? 1f : 0f;
            });
            FleetDataAPI data = mock(FleetDataAPI.class, (name, args) -> name.equals("getMembersListCopy") ? List.of() : null);
            CampaignFleetAPI fleet = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getLocation")) return new Vector2f();
                if (name.equals("getMemoryWithoutUpdate")) return global;
                if (name.equals("getCargo")) return cargo;
                if (name.equals("getFleetData")) return data;
                return null;
            });
            IntelManagerAPI manager = mock(IntelManagerAPI.class, (name, args) -> {
                if (name.equals("getIntel")) return List.copyOf(intel);
                if (name.equals("addIntel")) intel.add((IntelInfoPlugin) args[0]);
                return null;
            });
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (name.equals("performTokenReplacement")) return args[1];
                if (name.equals("getAllMatching") || name.equals("getBestMatching")) {
                    String trigger = (String) args[1];
                    if (trigger.equals("FireAllIntercept")) return null;
                    @SuppressWarnings("unchecked")
                    Map<String, MemoryAPI> memory = (Map<String, MemoryAPI>) args[3];
                    List<Row> matches = ROWS.stream().filter(r -> r.trigger.equals(trigger))
                            .filter(r -> r.id.startsWith("chiefNavigator"))
                            .filter(r -> r.matches(this, memory)).toList();
                    if (name.equals("getAllMatching")) return matches.stream().map(r -> r.nativeRule(this)).toList();
                    check(matches.size() <= 1, "Ambiguous native best match for " + trigger + ": "
                            + matches.stream().map(r -> r.id).toList());
                    return matches.isEmpty() ? null : matches.get(0).nativeRule(this);
                }
                return null;
            });
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createMemory")) return new Memory();
                if (name.equals("createMarket")) return carrier;
                if (name.equals("createPerson")) {
                    Map<String, Object> fields = new HashMap<>();
                    return mock(PersonAPI.class, (method, values) -> {
                        if (method.startsWith("set") && values.length == 1) {
                            fields.put(method.substring(3), values[0]);
                        }
                        if (method.equals("getNameString")) {
                            FullName fullName = (FullName) fields.get("Name");
                            return fullName == null ? "" : fullName.getFirst();
                        }
                        if (method.startsWith("get")) return fields.get(method.substring(3));
                        return null;
                    });
                }
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate") || name.equals("getPlayerMemoryWithoutUpdate")) return global;
                if (name.equals("getPlayerFleet")) return fleet;
                if (name.equals("getPlayerPerson")) return player;
                if (name.equals("getPlayerFaction")) return mock(FactionAPI.class, (method, values) -> null);
                if (name.equals("getImportantPeople")) return people;
                if (name.equals("getStarSystems") || name.equals("getAllLocations")) return List.of();
                if (name.equals("getIntelManager")) return manager;
                if (name.equals("getRules")) return rules;
                return null;
            }));
        }

        void runScript(Row row, Map<String, MemoryAPI> memory) {
            for (String line : row.script.split("\\R")) {
                if (line.isBlank()) continue;
                if (line.startsWith("$")) new oOOO(line).execute(row.id, dialog, memory);
                else if (line.startsWith("FireAll ")) FireAll.fire(row.id, dialog, memory, line.substring(8));
                else if (line.startsWith("FireBest ")) FireBest.fire(row.id, dialog, memory, line.substring(9));
                else if (line.startsWith("SetTextHighlightColors ")) {
                    new SetTextHighlightColors().execute(row.id, dialog,
                            Misc.tokenize(line.substring("SetTextHighlightColors ".length())), memory);
                } else if (line.startsWith("SetTextHighlights ")) {
                    new SetTextHighlights().execute(row.id, dialog,
                            Misc.tokenize(line.substring("SetTextHighlights ".length())), memory);
                }
                else if (line.startsWith("ChiefNavigatorMenelausCMD ")) {
                    boolean result = new ChiefNavigatorMenelausCMD().execute(row.id, dialog,
                            Misc.tokenize(line.substring("ChiefNavigatorMenelausCMD ".length())), memory);
                    check(result, "Authored command could not execute: " + row.id + ": " + line);
                } else if (line.startsWith("ChiefNavigatorPeopleCMD ")) {
                    // Exercise the presentation-only command as authored; the
                    // native visual methods are mocked, not the dispatcher.
                    boolean result = new ChiefNavigatorPeopleCMD().execute(row.id, dialog,
                            Misc.tokenize(line.substring("ChiefNavigatorPeopleCMD ".length())), memory);
                    check(result, "Authored portrait command could not execute: " + row.id + ": " + line);
                } else if (line.equals("DismissDialog")) dialog.dismiss();
                else if (line.startsWith("SetOptionColor ") || line.startsWith("SetShortcut ")) { /* Presentation only. */ }
                else throw new AssertionError("Unexercised authored command: " + row.id + ": " + line);
            }
        }

        void fire(String trigger) {
            install(); options.clear(); applied.clear();
            check(FireAll.fire(null, dialog, scope, trigger), "No authored entry for " + trigger);
            check(!options.isEmpty() || dismissals > 0, "Entry leaves no usable options: " + trigger + " " + applied);
        }

        void choose(String id) {
            check(options.containsKey(id), "Expected visible choice " + id + ", got " + options.keySet());
            local.set("$option", id, 0f); options.clear(); applied.clear();
            check(FireBest.fire(null, dialog, scope, "DialogOptionSelected"), "Unhandled authored choice: " + id);
            check(!options.isEmpty() || dismissals > 0 || directoryOpens > 0,
                    "Choice leaves an empty dialog: " + id + " -> " + applied);
        }

        void settle() { global.advance(0.5f); local.advance(0.5f); entity.advance(0.5f); }

        void stage(int stage, boolean complete, boolean briefed) throws Exception {
            global.set(INTRO, true); global.set(MenelausTrial.ACCEPTED, true);
            if (stage != MenelausTrial.LABOR_MOTHERSHIPS) {
                global.set(MenelausTrial.MOTHERSHIPS_CLEARED, true); global.set(REPORTED + "i_reported_v1", true);
            }
            if (stage == MenelausTrial.LABOR_RESEARCH && complete) global.set(field(IthacaResearchUpgrades.class, "UNLOCKED_MASK_KEY"), 15);
            if (stage != MenelausTrial.LABOR_MOTHERSHIPS && stage != MenelausTrial.LABOR_RESEARCH) {
                global.set(REPORTED + "ii_reported_v1", true);
            }
            if (stage == MenelausTrial.LABOR_WALL && complete) global.set(MenelausTrial.WALL_FRIENDLY, true);
            if (stage == MenelausTrial.LABOR_SENSOR || stage == MenelausTrial.LABOR_GAUTAMA || stage == MenelausTrial.LABOR_COMPLETE) {
                global.set(MenelausTrial.WALL_FRIENDLY, true); global.set(REPORTED + "iii_reported_v1", true);
            }
            if (stage == MenelausTrial.LABOR_SENSOR && complete || stage == MenelausTrial.LABOR_GAUTAMA || stage == MenelausTrial.LABOR_COMPLETE) {
                global.set(MenelausSensorLabor.DEPLOYED, true);
            }
            if (stage == MenelausTrial.LABOR_GAUTAMA || stage == MenelausTrial.LABOR_COMPLETE) global.set(REPORTED + "iv_reported_v1", true);
            if (stage == MenelausTrial.LABOR_MOTHERSHIPS && complete) global.set(MenelausTrial.MOTHERSHIPS_CLEARED, true);
            if (stage == MenelausTrial.LABOR_GAUTAMA && complete || stage == MenelausTrial.LABOR_COMPLETE) global.set(MenelausTrial.GAUTAMA_DEFEATED, true);
            if (stage == MenelausTrial.LABOR_COMPLETE) global.set(MenelausTrial.FINAL_DEBRIEF_COMPLETE, true);
            if (briefed) {
                global.set("$chief_navigator_menelaus_labor_two_briefed", true);
                global.set("$chief_navigator_menelaus_labor_three_briefed", true);
                global.set("$chief_navigator_menelaus_labor_four_briefed", true);
                if (stage == MenelausTrial.LABOR_GAUTAMA || stage == MenelausTrial.LABOR_COMPLETE) global.set(MenelausTrial.FINAL_LABOR_BRIEFED, true);
            }
        }
    }

    private static void verifyUnifiedHelenTopic() throws Exception {
        String helen = "chief_navigator_menelaus_labor_one_helen";
        String oldIntro = "$chief_navigator_menelaus_helen_intro_shown_v1";
        String record2 = "chief_navigator_auto_page_chief_navigator_menelaus_labor_one_helen_2";
        for (boolean accepted : List.of(false, true)) {
            for (boolean previouslyIntroduced : List.of(false, true)) {
                for (String response : List.of("pat", "owe")) {
                    Fixture f = new Fixture();
                    f.global.set(INTRO, true);
                    f.global.set(MenelausTrial.ACCEPTED, accepted);
                    if (previouslyIntroduced) f.global.set(oldIntro, true);
                    f.fire("ChiefNavigatorFobIthaca");
                    for (int visit = 0; visit < 2; visit++) {
                        int visitStart = f.history.size();
                        f.choose(helen);
                        check(f.applied.contains("chiefNavigatorMenelausFirstSinniHelen")
                                        && !f.applied.contains("chiefNavigatorMenelausLaborOneHelen"),
                                "First/repeat/legacy Helen selections must share one introduction");
                        f.choose("chief_navigator_auto_page_chief_navigator_menelaus_first_sinni_helen_2");
                        f.choose("chief_navigator_auto_page_chief_navigator_menelaus_first_sinni_helen_3");
                        f.choose("chief_navigator_menelaus_first_helen_sinni");
                        f.choose("chief_navigator_menelaus_first_helen_" + response);
                        check(f.options.containsKey(record2) && !f.options.containsKey(helen),
                                "Both responses must immediately segue into the record, not the hub");
                        check(f.applied.contains("chiefNavigatorMenelausLaborOneHelen"),
                                "Both responses must dispatch the same service-record page");
                        if (response.equals("owe")) {
                            String transcript = String.join("\n", f.history.subList(visitStart, f.history.size()));
                            check(transcript.contains("Very well.")
                                            && transcript.indexOf("Very well.")
                                                    < transcript.indexOf("Lieutenant Commander Helen Argyros."),
                                    "Very well must precede the service record in the actual transcript");
                        }
                        f.choose(record2);
                        f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_one_helen_3");
                        check(f.options.containsKey("chief_navigator_menelaus_labor_one_helen_collapse")
                                        && f.options.containsKey("chief_navigator_menelaus_labor_one_helen_enough"),
                                "Keep both existing record follow-ups");
                        long descriptions = f.history.stream().filter(
                                text -> text.contains("There is a beautiful woman")).count();
                        check(descriptions == visit + 1,
                                "Describe Helen exactly once per complete visit");
                        check(f.history.stream().noneMatch(text -> text.contains("I am aware.")
                                        || text.contains("When circumstances permit")
                                        || text.contains("You knew her?")),
                                "No deferred promise or repeated recognition question");
                        f.choose(visit == 0
                                ? "chief_navigator_menelaus_labor_one_helen_collapse"
                                : "chief_navigator_menelaus_labor_one_helen_enough");
                        check(f.options.containsKey(helen)
                                        && f.global.getBoolean(MenelausTrial.ACCEPTED) == accepted,
                                "Return to the repeatable hub without accepting or advancing a Labor");
                        f.settle();
                    }
                    check(f.global.getBoolean(oldIntro) == previouslyIntroduced
                                    && (previouslyIntroduced || !f.global.contains(oldIntro)),
                            "Leave old introduction history untouched and unused");
                }
            }
        }
    }

    private static void verifyUpgradeLedgerHighlights() throws Exception {
        for (int mask = 0; mask <= IthacaResearchUpgrades.ALL_UPGRADES; mask++) {
            for (int recovered : new int[] {0, IthacaResearchUpgrades.ALL_UPGRADES, 42}) {
                Fixture f = new Fixture();
                f.global.set(field(IthacaResearchUpgrades.class, "UNLOCKED_MASK_KEY"), mask);
                f.availableMask = recovered;
                f.install();
                check(new ChiefNavigatorMenelausCMD().execute("ledger_setup", f.dialog,
                                Misc.tokenize("initUpgrades"), f.scope),
                        "Ledger setup must retain its existing variables");
                f.fire("ChiefNavigatorFobIthacaUpgrades");
                List<String> expected = new ArrayList<>();
                List<Color> colors = new ArrayList<>();
                for (int index = 0; index < IthacaResearchUpgrades.COMPONENT_IDS.length; index++) {
                    int bit = 1 << index;
                    if ((mask & bit) != 0) {
                        expected.add("Installed"); colors.add(Color.GREEN);
                    } else if ((recovered & bit) == 0) {
                        expected.add("Not recovered"); colors.add(Color.RED);
                    }
                }
                check(Arrays.equals(f.textHighlights, expected.toArray(new String[0]))
                                && Arrays.equals(f.textHighlightColors, colors.toArray(new Color[0])),
                        "Every repeated/mixed ledger status must get its ordered green/red highlight: "
                                + mask + "/" + recovered);
                String ledger = f.local.getString("$chiefNavigatorUpgradeLedger");
                int cursor = 0;
                for (String highlight : f.textHighlights) {
                    int next = ledger.indexOf(highlight, cursor);
                    check(next >= cursor, "Highlight strings must follow native paragraph order");
                    cursor = next + highlight.length();
                }
                check(f.options.containsKey("chief_navigator_fob_ithaca_upgrades_back")
                                && f.options.containsKey("chief_navigator_fob_ithaca_upgrades_install")
                                        == ((recovered & ~mask & IthacaResearchUpgrades.ALL_UPGRADES) != 0),
                        "Coloring must preserve Back and component-install availability");
                check(f.global.getInt(field(IthacaResearchUpgrades.class, "UNLOCKED_MASK_KEY")) == mask,
                        "Coloring must not install components or change progress");
            }
        }
    }

    private static void verifyVossAftermathHighlight() throws Exception {
        String withdrawal = "Voss withdraws to a make-shift Forward Operating Base in the Alpha Odyssey Sector.";
        Fixture f = new Fixture();
        f.fire("ChiefNavigatorVossAftermath");
        check(f.history.stream().anyMatch(text -> text.contains(withdrawal)),
                "Voss's withdrawal must use the revised player-facing wording");
        check(Arrays.equals(f.textHighlights, new String[] {withdrawal})
                        && Arrays.equals(f.textHighlightColors, new Color[] {Misc.getHighlightColor()}),
                "Highlight only the revised withdrawal sentence in standard yellow");
        check(f.options.containsKey("chief_navigator_hegemony_ithaca_continue"),
                "Preserve the continuation to Menelaus's Labor-IV briefing");
        check(Files.readString(Path.of("dialogue/voss_ithaca_confrontation.ink"),
                        StandardCharsets.UTF_8).contains(withdrawal),
                "Keep the confrontation Ink excerpt synchronized");
    }

    private static void verifySinniIdentityRoutes() throws Exception {
        for (boolean why : List.of(false, true)) {
            for (String decision : List.of("refuse", "choice")) {
                Fixture f = new Fixture();
                f.stage(MenelausTrial.LABOR_SENSOR, false, false);
                // A prior interrupted/reopened exchange must not hide the line.
                f.local.set("$chiefNavigatorMenelausL4SinniHelenStated", true);
                f.fire("ChiefNavigatorFobIthaca");
                f.choose("chief_navigator_menelaus_labor_four_understood");
                f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_understood_2");
                f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_understood_3");
                check(!f.local.getBoolean("$chiefNavigatorMenelausL4SinniHelenStated"),
                        "A fresh initial Sinni decision resets only its presentation context");
                if (why) {
                    f.choose("chief_navigator_menelaus_labor_four_sinni_why");
                    f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_why_2");
                    f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_why_3");
                }
                f.choose("chief_navigator_menelaus_labor_four_sinni_" + decision);
                if (decision.equals("choice")) {
                    f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_choice_2");
                    f.choose("chief_navigator_auto_page_chief_navigator_menelaus_labor_four_sinni_choice_3");
                }
                String transcript = String.join("\n", f.history);
                check(transcript.split("I am not Helen", -1).length == 2,
                        "Every complete route must contain exactly one Sinni declaration: " + why + "/" + decision);
                check(f.options.containsKey("chief_navigator_menelaus_labor_four_accept")
                                && !f.global.getBoolean("$chief_navigator_menelaus_labor_four_briefed"),
                        "The edited exchange must retain explicit Labor-IV acceptance");
                f.choose("chief_navigator_menelaus_labor_four_accept");
                check(f.global.getBoolean("$chief_navigator_menelaus_labor_four_briefed"),
                        "All four route combinations must still reach the reusable Labor-IV menu");
            }
        }
    }

    private static void verifyOptionalVictoryExplanation() throws Exception {
        String question = "chief_navigator_menelaus_debrief_gautama";
        String page2 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_2";
        String page3 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_3";
        String page4 = "chief_navigator_auto_page_chief_navigator_fob_ithaca_debrief_4";
        for (boolean readExplanation : List.of(false, true)) {
            Fixture f = new Fixture(); f.stage(MenelausTrial.LABOR_GAUTAMA, true, true);
            f.fire("ChiefNavigatorFobIthaca");
            check(f.options.keySet().equals(Set.of(question, page3))
                            && f.history.get(0).startsWith("\"Ithaca remains.")
                            && f.history.stream().noneMatch(t -> t.contains("Gautama was never")
                                    || t.contains("Its true form") || t.contains("Menelaus is waiting")),
                    "Victory opens with congratulations and a functional optional-question/skip decision");
            if (readExplanation) {
                f.choose(question);
                check(f.options.keySet().equals(Set.of(page2))
                                && f.history.stream().anyMatch(t -> t.contains("Gautama was never the complete organism.")),
                        "Choosing the question opens the original Gautama explanation");
                check(!MenelausTrial.isComplete() && !f.global.getBoolean(MenelausCompletionDialogPlugin.PENDING),
                        "Opening the optional explanation cannot commit the Labor or queue its ending");
                f.choose(page2);
                check(f.options.keySet().equals(Set.of(page3))
                                && f.history.stream().anyMatch(t -> t.contains("Its true form is a web"))
                                && f.history.stream().anyMatch(t -> t.contains("The wreck cannot be approached")),
                        "The optional continuation preserves Ungaikyo and Guardian information");
            }
            f.choose(page3);
            check(f.options.keySet().equals(Set.of(page4))
                            && f.history.stream().filter(t -> t.contains("Ithaca remains.")).count() == 1
                            && f.history.stream().anyMatch(t -> t.contains("The prohibition on Ithaca's gate is lifted.")),
                    "Both routes reach the same gate-release acknowledgement without repeated congratulations");
            f.choose(page4);
            check(!MenelausTrial.isComplete() && !f.global.getBoolean(MenelausCompletionDialogPlugin.PENDING),
                    "Neither reading nor skipping may finish before the existing departure choice");
            f.choose("chief_navigator_fob_ithaca_debrief_leave");
            check(MenelausTrial.isComplete() && f.dismissals == 1
                            && f.global.getBoolean(MenelausCompletionDialogPlugin.PENDING),
                    "Both routes finish Labor V once and queue the standalone cinematic on departure");
            if (!readExplanation) {
                check(f.history.stream().noneMatch(t -> t.contains("Gautama was never") || t.contains("Its true form")),
                        "Skipping must not silently render the optional explanation later");
            }
        }
        Fixture interrupted = new Fixture(); interrupted.stage(MenelausTrial.LABOR_GAUTAMA, true, true);
        interrupted.fire("ChiefNavigatorFobIthaca"); interrupted.choose(question);
        interrupted.settle(); interrupted.fire("ChiefNavigatorFobIthaca");
        check(interrupted.options.keySet().equals(Set.of(question, page3)) && !MenelausTrial.isComplete(),
                "Reopening an interrupted optional explanation retains both routes and pending completion");
    }

    private static void verifyMenuVisibility() throws Exception {
        Fixture f = new Fixture(); f.global.set(INTRO, true); f.fire("ChiefNavigatorFobIthaca");
        check(!f.options.containsKey(UPGRADE), "Upgrade ledger must not appear before Labor II acceptance");
        check(f.options.containsKey("chief_navigator_menelaus_labor_one")
                && f.options.containsKey("chief_navigator_menelaus_labor_one_helen")
                && f.options.containsKey("chief_navigator_menelaus_labor_one_repair"),
                "Gating upgrades must preserve current Labor, topics, repairs and Leave");
        f.stage(MenelausTrial.LABOR_RESEARCH, false, false);
        f.fire("ChiefNavigatorFobIthacaServiceOptions");
        check(!f.options.containsKey(UPGRADE), "Reading the Labor II briefing is not acceptance");
        f.global.set("$chief_navigator_menelaus_labor_two_briefed", true);
        f.fire("ChiefNavigatorFobIthacaServiceOptions");
        check(f.options.containsKey(UPGRADE), "Accepted Labor II exposes its ledger");
    }

    private static void verifyContactEntryAndReports() throws Exception {
        for (int stage : List.of(MenelausTrial.LABOR_MOTHERSHIPS, MenelausTrial.LABOR_RESEARCH,
                MenelausTrial.LABOR_WALL, MenelausTrial.LABOR_SENSOR,
                MenelausTrial.LABOR_GAUTAMA, MenelausTrial.LABOR_COMPLETE)) {
            for (boolean complete : List.of(false, true)) {
                Fixture f = new Fixture(); f.stage(stage, complete, true);
                f.fire("PopulateOptions");
                check(f.options.containsKey(LABORS), "Native contact must expose Labors at stage " + stage
                        + ", report-pending=" + complete);
                f.choose(LABORS);
                check(!f.options.keySet().equals(Set.of(DIRECTORY)), "Menelaus must not dead-end at stage " + stage);
                check(!f.applied.contains("chiefNavigatorFobIthacaBriefing"), "Accepted saves must never replay station introduction");
            }
        }
        Fixture report = new Fixture(); report.stage(MenelausTrial.LABOR_MOTHERSHIPS, true, true);
        report.fire("ChiefNavigatorFobIthaca");
        check(report.options.containsKey("chief_navigator_menelaus_report_motherships"), "Completed Labor I needs its report option");
        report.choose("chief_navigator_menelaus_report_motherships");
        check(MenelausTrial.isLaborReported(MenelausTrial.LABOR_MOTHERSHIPS), "Labor I report commits durable stage state");
        report.settle();
        check(MenelausTrial.getCurrentLaborStage() == MenelausTrial.LABOR_RESEARCH, "Report unlocks Labor II without replaying intro");
    }

    private static void verifyDurableIntroAndBriefings() throws Exception {
        Fixture f = new Fixture(); f.install();
        f.global.set(INTRO, true, 0f);
        f.global.set("$chief_navigator_menelaus_intro_shown_v1", true, 0f);
        Row finish = ROWS.stream().filter(r -> r.id.equals("chiefNavigatorMenelausFirstFinishOption")).findFirst().orElseThrow();
        f.runScript(finish, f.scope);
        check(f.global.getBoolean(INTRO) && !MenelausTrial.isAccepted(), "Finishing introduction must not accept Labor I");
        f.settle(); f.fire("ChiefNavigatorFobIthaca");
        check(!f.applied.contains("chiefNavigatorFobIthacaBriefing"), "Leaving before accepting must not replay intro after unpause");
        check(f.global.getExpire(INTRO) == -1f
                        && f.global.getExpire("$chief_navigator_menelaus_intro_shown_v1") == -1f,
                "Finishing introduction clears any preexisting legacy zero-duration expirations");

        Fixture accepted = new Fixture(); accepted.install();
        accepted.global.set(INTRO, true, 0f);
        accepted.global.set("$chief_navigator_menelaus_intro_shown_v1", true, 0f);
        check(new ChiefNavigatorMenelausCMD().execute("native_acceptance", null,
                        Misc.tokenize("accept"), accepted.scope), "Actual Labor-I acceptance executes");
        accepted.settle();
        check(MenelausTrial.isAccepted() && accepted.global.getBoolean(INTRO)
                        && accepted.global.getExpire(INTRO) == -1f,
                "Labor-I acceptance cancels legacy zero-duration introduction expirations");
        for (String number : List.of("two", "three", "four")) {
            Fixture briefing = new Fixture(); briefing.install();
            Row acceptance = ROWS.stream().filter(r -> r.id.equals("chiefNavigatorMenelausLabor"
                    + Character.toUpperCase(number.charAt(0)) + number.substring(1) + "Accept")).findFirst().orElseThrow();
            briefing.runScript(acceptance, briefing.scope); briefing.settle();
            check(briefing.global.getBoolean("$chief_navigator_menelaus_labor_" + number + "_briefed"),
                    "Labor " + number + " acceptance must survive campaign unpause/reopen");
        }
    }

    private static void verifyAcceptedLegacyContactWithoutMarker() throws Exception {
        Fixture f = new Fixture(); f.stage(MenelausTrial.LABOR_MOTHERSHIPS, true, true);
        f.global.unset(INTRO);
        f.global.unset("$chief_navigator_menelaus_intro_shown_v1");
        f.install();
        Set<String> before = Set.copyOf(f.global.getKeys());
        check(new ChiefNavigatorMenelausCMD().execute("legacy_query", null,
                        Misc.tokenize("introduced"), f.scope),
                "Accepted Labor progress proves an introduction even with lost legacy marker");
        check(before.equals(Set.copyOf(f.global.getKeys())) && !f.global.contains(INTRO),
                "Introduced condition is pure and does not recreate or mutate old markers");
        f.fire("PopulateOptions");
        check(f.options.containsKey(LABORS), "Completed Labor I must be reportable even after legacy intro flag expires");
        f.choose(LABORS);
        check(f.options.containsKey("chief_navigator_menelaus_report_motherships"),
                "The exact legacy missing-marker state opens the Labor I report, not an empty contact");
        check(!f.applied.contains("chiefNavigatorFobIthacaBriefing"), "Legacy accepted save never replays introductory scene");
    }

    private static void verifyInterruptedTopics() throws Exception {
        String[][] topics = {
            {"chiefNavigatorMenelausFirstLosing", "chiefNavigatorMenelausFirstLosingReturn", "$chiefNavigatorMenelausFirstAskedLosing"},
            {"chiefNavigatorMenelausLaborTwoCollapse", "chiefNavigatorMenelausLaborTwoCollapsePage3", "$chiefNavigatorMenelausL2AskedCollapse"},
            {"chiefNavigatorMenelausLaborTwoCryo", "chiefNavigatorMenelausLaborTwoGautamaEnd", "$chiefNavigatorMenelausL2AskedCryo"},
            {"chiefNavigatorMenelausLaborTwoHelen", "chiefNavigatorMenelausLaborTwoHelenEnd", "$chiefNavigatorMenelausL2AskedHelen"},
            {"chiefNavigatorMenelausLaborThreeHistory", "chiefNavigatorMenelausLaborThreeHistoryPage3", "$chiefNavigatorMenelausL3AskedHistory"},
            {"chiefNavigatorMenelausLaborThreeHelen", "chiefNavigatorMenelausLaborThreeHelenEnd", "$chiefNavigatorMenelausL3AskedHelen"},
            {"chiefNavigatorMenelausLaborFourPackage", "chiefNavigatorMenelausLaborFourDetected", "$chiefNavigatorMenelausL4AskedPackage"},
            {"chiefNavigatorMenelausLaborFourReach", "chiefNavigatorMenelausLaborFourReachPage4", "$chiefNavigatorMenelausL4AskedReach"},
            {"chiefNavigatorMenelausLaborFourThreat", "chiefNavigatorMenelausLaborFourStarving", "$chiefNavigatorMenelausL4AskedThreat"},
            {"chiefNavigatorMenelausLaborFourHelen", "chiefNavigatorMenelausLaborFourHelenEnd", "$chiefNavigatorMenelausL4AskedHelen"}
        };
        for (String[] topic : topics) {
            Fixture f = new Fixture(); f.install();
            Row start = ROWS.stream().filter(r -> r.id.equals(topic[0])).findFirst().orElseThrow();
            Row end = ROWS.stream().filter(r -> r.id.equals(topic[1])).findFirst().orElseThrow();
            f.runScript(start, f.scope);
            check(!f.local.getBoolean(topic[2]), "Interrupting mandatory follow-ups must not hide topic: " + topic[0]);
            f.runScript(end, f.scope);
            check(f.local.getBoolean(topic[2]), "Completing mandatory follow-ups consumes one-shot topic: " + topic[1]);
        }
    }

    private static void verifyNestedOptionGroups() throws Exception {
        for (boolean saraDead : List.of(false, true)) {
            Fixture f = new Fixture(); f.global.set("$chief_navigator_sara_dead", saraDead);
            f.fire("ChiefNavigatorMenelausFirstSinniInsists");
            check(f.options.containsKey("chief_navigator_menelaus_first_sinni_request"),
                    "Nested bodyguard options must preserve the request question");
            check(f.options.size() == 2 && f.options.containsKey(saraDead
                    ? "chief_navigator_menelaus_first_sinni_insists_go_yvan"
                    : "chief_navigator_menelaus_first_sinni_insists_go"),
                    "First introduction retains request and exactly the living guard route");
        }
        for (boolean components : List.of(false, true)) {
            Fixture f = new Fixture(); f.stage(MenelausTrial.LABOR_RESEARCH, false, true);
            f.availableComponents = components ? 1 : 0;
            f.fire("ChiefNavigatorFobIthacaUpgrades");
            check(f.options.containsKey("chief_navigator_fob_ithaca_upgrades_back"),
                    "Conditional Install must never overwrite the ledger Back option");
            check(f.options.containsKey("chief_navigator_fob_ithaca_upgrades_install") == components,
                    "Install follows actual recovered cargo availability");
            f.choose("chief_navigator_fob_ithaca_upgrades_back");
            check(!f.options.isEmpty(), "Ledger Back returns to a functional Labor menu");
        }
    }

    private static void verifyEarlyWallPriority() throws Exception {
        for (int stage : List.of(MenelausTrial.LABOR_MOTHERSHIPS, MenelausTrial.LABOR_COMPLETE)) {
            Fixture f = new Fixture(); f.stage(stage, false, true);
            f.global.set("$chief_navigator_drifting_wall_cleared_early_v1", true);
            f.global.set(MenelausTrial.WALL_FRIENDLY, true);
            f.fire("ChiefNavigatorFobIthaca");
            check(f.options.containsKey("chief_navigator_menelaus_early_wall_continue"),
                    "Pending early-Wall acknowledgement takes precedence without an ordinary menu overwriting it");
            check(f.applied.stream().filter(id -> id.startsWith("chiefNavigatorFobIthaca")).count() == 1,
                    "Exactly one Menelaus phase may render per dispatch");
            f.choose("chief_navigator_menelaus_early_wall_continue");
            check(f.options.containsKey(stage == MenelausTrial.LABOR_MOTHERSHIPS
                    ? "chief_navigator_menelaus_labor_one" : "chief_navigator_menelaus_helen_final"),
                    "Acknowledging early Wall returns to the unchanged durable Labor phase");
        }
    }

    private static void copyMemory(Memory source, Memory target) {
        target.clear();
        for (String key : source.getKeys()) {
            float expiry = source.getExpire(key);
            if (expiry < 0f) target.set(key, source.get(key));
            else target.set(key, source.get(key), expiry);
        }
    }

    private static Fixture fork(Fixture source) throws Exception {
        Fixture f = new Fixture();
        copyMemory(source.global, f.global); copyMemory(source.local, f.local); copyMemory(source.entity, f.entity);
        f.options.putAll(source.options); f.availableComponents = source.availableComponents;
        f.plugin = new FobIthacaStationInteraction();
        f.install();
        return f;
    }

    private static String fingerprint(Fixture f) {
        StringBuilder result = new StringBuilder(f.options.keySet().toString());
        for (Memory memory : List.of(f.global, f.local)) {
            for (String key : new TreeSet<>(memory.getKeys())) {
                // Presentation variables and dispatch bookkeeping do not affect
                // graph reachability; retaining them turns harmless loops into
                // an unbounded transcript walk.
                if (key.equals("$option") || key.equals("$fireAllTrigger")
                        || key.startsWith("$chiefNavigatorUpgrade")
                        || key.startsWith("$chiefNavigatorResearch")
                        || key.startsWith("$chiefNavigatorMothership")
                        || key.startsWith("$chiefNavigatorFriendly")
                        || key.startsWith("$chiefNavigatorWallStatus")
                        || key.startsWith("$chiefNavigatorGautamaStatus")) continue;
                result.append('|').append(key).append('=').append(memory.get(key));
            }
        }
        return result.toString();
    }

    /** Explore every authored selectable route, including question and service loops. */
    private static void verifyBranchTraversal() throws Exception {
        Deque<Fixture> queue = new ArrayDeque<>();
        Fixture opening = new Fixture(); opening.fire("ChiefNavigatorFobIthaca"); queue.add(opening);
        Fixture openingWithoutSara = new Fixture();
        openingWithoutSara.global.set("$chief_navigator_sara_dead", true);
        openingWithoutSara.fire("ChiefNavigatorFobIthaca"); queue.add(openingWithoutSara);
        Fixture offer = new Fixture(); offer.global.set(INTRO, true);
        offer.fire("ChiefNavigatorFobIthaca"); queue.add(offer);
        for (int stage : List.of(MenelausTrial.LABOR_MOTHERSHIPS, MenelausTrial.LABOR_RESEARCH,
                MenelausTrial.LABOR_WALL, MenelausTrial.LABOR_SENSOR,
                MenelausTrial.LABOR_GAUTAMA, MenelausTrial.LABOR_COMPLETE)) {
            for (boolean complete : List.of(false, true)) {
                for (boolean briefed : List.of(false, true)) {
                    Fixture f = new Fixture(); f.stage(stage, complete, briefed);
                    f.fire("ChiefNavigatorFobIthaca"); queue.add(f);
                }
            }
        }
        Set<String> seen = new HashSet<>(), selected = new HashSet<>();
        while (!queue.isEmpty()) {
            Fixture current = queue.removeFirst();
            if (!seen.add(fingerprint(current))) continue;
            check(seen.size() < 5000, "Authored graph traversal must remain bounded");
            for (String choice : current.options.keySet()) {
                // Station services and the native directory use their own shell,
                // already covered by FobIthacaDirectoryReturnRegression.
                if (choice.equals(DIRECTORY) || choice.equals("chief_navigator_fob_ithaca_directory")
                        || choice.equals("chief_navigator_fob_ithaca_fleet")
                        || choice.equals("chief_navigator_fob_ithaca_storage")
                        || choice.equals("chief_navigator_fob_ithaca_refit")
                        || choice.equals("chief_navigator_fob_ithaca_station_leave")) continue;
                Fixture next = fork(current);
                next.choose(choice); selected.add(choice);
                if (next.dismissals == 0 && next.directoryOpens == 0) queue.addLast(next);
            }
        }
        check(selected.contains("chief_navigator_menelaus_labor_one_accept")
                        && selected.contains("chief_navigator_menelaus_report_motherships")
                        && selected.contains("chief_navigator_menelaus_report_research")
                        && selected.contains("chief_navigator_menelaus_report_sensor")
                        && selected.contains("chief_navigator_menelaus_accept_final")
                        && selected.contains("chief_navigator_menelaus_debrief_gautama")
                        && selected.contains("chief_navigator_fob_ithaca_debrief_leave")
                        && selected.contains("chief_navigator_menelaus_helen_end"),
                "Full graph traversal must exercise every Labor handoff and the final Helen ending; selected " + selected);
        System.out.println("Traversed " + seen.size() + " authored dialogue states and " + selected.size() + " distinct choices.");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI oldSector = Global.getSector(); FactoryAPI oldFactory = Global.getFactory();
        SettingsAPI oldSettings = Global.getSettings();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (!name.equals("getColor")) return null;
                if (values[0].equals("textFriendColor")) return Color.GREEN;
                if (values[0].equals("textEnemyColor")) return Color.RED;
                return Color.WHITE;
            }));
            initializeExpressions(); readRules();
            verifyMenuVisibility(); verifyContactEntryAndReports(); verifyUnifiedHelenTopic();
            verifyUpgradeLedgerHighlights();
            verifyVossAftermathHighlight();
            verifySinniIdentityRoutes();
            verifyOptionalVictoryExplanation();
            verifyDurableIntroAndBriefings(); verifyInterruptedTopics();
            verifyAcceptedLegacyContactWithoutMarker();
            verifyNestedOptionGroups(); verifyEarlyWallPriority();
            verifyBranchTraversal();
            System.out.println("PASS: Menelaus parsed rules, native expressions/expiry, preacceptance reopen, all-stage contact access, Labor I report, upgrade gate, and interrupted topics ("
                    + pagesVisited + " authored rules executed).");
        } finally { Global.setSector(oldSector); Global.setFactory(oldFactory); Global.setSettings(oldSettings); }
    }
}
