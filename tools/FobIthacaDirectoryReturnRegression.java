package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CommDirectoryAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.InteractionDialogPlugin;
import com.fs.starfarer.api.campaign.OptionPanelAPI;
import com.fs.starfarer.api.campaign.RuleBasedDialog;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.RuleAPI;
import com.fs.starfarer.api.campaign.rules.RulesAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.DismissDialog;
import com.fs.starfarer.api.util.Misc;
import data.campaign.rulecmd.ChiefNavigatorMenelausCMD;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Headless protection against Menelaus exits leaving a dead Ithaca contact hub. */
public final class FobIthacaDirectoryReturnRegression {
    private static final String BACK = "chief_navigator_ithaca_directory_back";
    private static final String CONTACT_LEAVE = "chief_navigator_fob_ithaca_leave";
    private static final String STATION_LEAVE = "chief_navigator_fob_ithaca_station_leave";
    private static final String LABOR_LEAVE = "chief_navigator_menelaus_labor_one_leave";
    private static final String RETURN_LABEL = "Return to the comm directory.";
    private static final Path ROOT = Path.of("").toAbsolutePath();

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call, Class<?>... additional) {
        List<Class<?>> interfaces = new ArrayList<>();
        interfaces.add(type);
        interfaces.addAll(List.of(additional));
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                interfaces.toArray(Class<?>[]::new), (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get") || name.equals("getString")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    private static final class Fixture {
        final Map<Object, String> options = new LinkedHashMap<>();
        final List<String> history = new ArrayList<>(List.of("Earlier conversation remains."));
        final List<String> events = new ArrayList<>();
        final MemoryAPI stationMemory = memory(new HashMap<>(Map.of(
                "$chief_navigator_fob_ithaca_owned_v1", true)));
        final Map<String, Object> personValues = new HashMap<>(Map.of(
                FobIthacaContacts.MENELAUS_MARKER, true,
                "$chief_navigator_ithaca_contact_resident", true));
        final MemoryAPI personMemory = memory(personValues);
        final MemoryAPI globalMemory = memory(new HashMap<>(Map.of(
                "$chief_navigator_menelaus_created_v1", true,
                "$chief_navigator_ithaca_spartan_captain_created_v1", true)));
        final PersonAPI person = mock(PersonAPI.class, (name, args) -> {
            if (name.equals("getId")) return MenelausTrial.PERSON_ID;
            if (name.equals("getMemoryWithoutUpdate")) return personMemory;
            if (name.equals("getName")) return new FullName("Menelaus", "", FullName.Gender.ANY);
            return null;
        });
        PersonAPI activePerson;
        MarketAPI attachedMarket;
        InteractionDialogPlugin plugin;
        int directoryOpens;
        int dismissals;
        int nativeNotifications;
        int nativePopulation;

        final SectorEntityToken station = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return "chief_navigator_fob_ithaca";
            if (name.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
            if (name.equals("getMemoryWithoutUpdate")) return stationMemory;
            if (name.equals("getActivePerson")) return activePerson;
            if (name.equals("setActivePerson")) activePerson = (PersonAPI) args[0];
            if (name.equals("getMarket")) return attachedMarket;
            if (name.equals("setMarket")) attachedMarket = (MarketAPI) args[0];
            return null;
        });
        final OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (name, args) -> {
            if (name.equals("clearOptions")) {
                events.add("clearOptions");
                options.clear();
            }
            if (name.equals("addOption")) options.put(args[1], (String) args[0]);
            return null;
        });
        final TextPanelAPI textPanel = mock(TextPanelAPI.class, (name, args) -> {
            if (name.equals("clear")) throw new AssertionError("Returning must retain text history");
            if (name.equals("addPara")) history.add((String) args[0]);
            return null;
        });
        final VisualPanelAPI visual = mock(VisualPanelAPI.class, (name, args) -> null);
        final CommDirectoryAPI directory = mock(CommDirectoryAPI.class, (name, args) -> null);
        final MarketAPI carrier = mock(MarketAPI.class, (name, args) ->
                name.equals("getCommDirectory") ? directory : null);
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) -> {
            if (name.equals("getInteractionTarget")) return station;
            if (name.equals("getOptionPanel")) return optionPanel;
            if (name.equals("getTextPanel")) return textPanel;
            if (name.equals("getVisualPanel")) return visual;
            if (name.equals("getPlugin")) return plugin;
            if (name.equals("setPlugin")) plugin = (InteractionDialogPlugin) args[0];
            if (name.equals("addOptionSelectedText")) {
                check(options.containsKey(args[0]), "Echo must precede clearing the selected option");
                history.add(options.get(args[0]));
                events.add("echo:" + args[0]);
            }
            if (name.equals("showCommDirectoryDialog")) {
                check(activePerson == null, "Directory must not retain the previous active person");
                check(attachedMarket == carrier, "Native directory needs its temporary carrier");
                directoryOpens++;
            }
            if (name.equals("dismiss")) dismissals++;
            return null;
        });

        void install() {
            ImportantPeopleAPI people = mock(ImportantPeopleAPI.class, (name, args) -> {
                if (name.equals("getPerson") && MenelausTrial.PERSON_ID.equals(args[0])) return person;
                if (name.equals("getPeopleCopy")) return List.of();
                return null;
            });
            RulesAPI rules = mock(RulesAPI.class, (name, args) -> {
                if (name.equals("getBestMatching")) {
                    String trigger = (String) args[1];
                    @SuppressWarnings("unchecked")
                    Map<String, MemoryAPI> scope = (Map<String, MemoryAPI>) args[3];
                    return rule(trigger, scope);
                }
                if (name.equals("getAllMatching") || name.equals("getMatching")) {
                    if ("PopulateOptions".equals(args[1])) nativePopulation++;
                    return List.of();
                }
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getColor") ? Color.WHITE : null));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createMemory")) return memory(new HashMap<>());
                if (name.equals("createMarket")) return carrier;
                return null;
            }));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")
                        || name.equals("getPlayerMemoryWithoutUpdate")) return globalMemory;
                if (name.equals("getPlayerPerson")) return person;
                if (name.equals("getImportantPeople")) return people;
                if (name.equals("getRules")) return rules;
                return null;
            }));
        }

        RuleAPI rule(String trigger, Map<String, MemoryAPI> scope) {
            if (!List.of("ChiefNavigatorFobIthacaStation", "PickGreeting", "DialogOptionSelected")
                    .contains(trigger)) return null;
            return mock(RuleAPI.class, (name, args) -> {
                if (name.equals("getId")) return "regression_" + trigger;
                if (name.equals("getText") || name.equals("getOptions")
                        || name.equals("getScriptCopy")) return List.of();
                if (name.equals("runScript")) {
                    if (trigger.equals("ChiefNavigatorFobIthacaStation")) {
                        options.put(STATION_LEAVE, "Leave.");
                    } else if (trigger.equals("PickGreeting")) {
                        options.put(CONTACT_LEAVE, RETURN_LABEL);
                    } else {
                        check(scope.get(MemKeys.LOCAL) == personMemory,
                                "Labor exit must dispatch with the active resident's memory");
                        check(LABOR_LEAVE.equals(personValues.get("$option")),
                                "Labor option must reach actual rules dispatch");
                        executeEndContact(scope);
                    }
                }
                return null;
            });
        }

        void executeEndContact(Map<String, MemoryAPI> scope) {
            check(new ChiefNavigatorMenelausCMD().execute("regression_exit", dialog,
                            Misc.tokenize("endContact"), scope),
                    "The Menelaus endContact command must handle the exit");
        }

        void beginStation() {
            plugin = new FobIthacaStationInteraction();
            plugin.init(dialog);
            events.clear();
        }

        void activatePerson() {
            activePerson = person;
            ((RuleBasedDialog) plugin).notifyActivePersonChanged();
            check(plugin.getContext() == person, "Test contact must be active before selecting its exit");
        }

        void choose(String id, String label) {
            options.put(id, label);
            plugin.optionSelected(label, id);
        }

        void assertReturned(int expectedEchoes, String selectedLabel) {
            check(directoryOpens == 1 && dismissals == 0,
                    "Contact return must open the directory once without closing the station");
            check(plugin instanceof FobIthacaStationInteraction && plugin.getContext() == null,
                    "Return must restore a live station shell with no selected resident");
            check(activePerson == null && attachedMarket == null,
                    "Return must clear the active person and detach the directory's carrier");
            check(history.get(0).equals("Earlier conversation remains."),
                    "Existing dialogue history must survive the handoff");
            check(history.stream().filter(selectedLabel::equals).count() == expectedEchoes,
                    "A genuine selected return choice must be preserved exactly once");
            check(options.containsKey(STATION_LEAVE),
                    "Station Leave must remain available behind the reopened directory");
            choose(STATION_LEAVE, "Leave.");
            check(dismissals == 1, "The restored station must still be dismissible");
        }
    }

    private static void verifyStaleHub(String id) {
        Fixture f = new Fixture();
        f.install();
        f.beginStation();
        // Matches the screenshot: native EndConversation already cleared the
        // person while authored contact options remain on the custom shell.
        check(f.plugin.getContext() == null && f.activePerson == null, "Hub must start stale");
        f.choose(id, RETURN_LABEL);
        check(f.events.get(0).equals("echo:" + id), "Return choice must be echoed before handoff");
        f.assertReturned(1, RETURN_LABEL);
    }

    private static void verifyNormalContactReturn() {
        Fixture f = new Fixture();
        f.install();
        f.beginStation();
        f.activatePerson();
        f.choose(CONTACT_LEAVE, RETURN_LABEL);
        f.assertReturned(1, RETURN_LABEL);
    }

    private static void verifyAuthoredLaborExit() {
        Fixture f = new Fixture();
        f.install();
        f.beginStation();
        f.activatePerson();
        f.choose(LABOR_LEAVE, "End the briefing.");
        f.assertReturned(1, "End the briefing.");
    }

    private static void verifyLegacyContactShell() {
        Fixture f = new Fixture();
        f.install();
        f.activePerson = f.person;
        f.plugin = new FobIthacaContactInteraction(f.person);
        f.plugin.init(f.dialog);
        f.choose(LABOR_LEAVE, "End the briefing.");
        f.assertReturned(1, "End the briefing.");
    }

    private static void verifyNativeFallback() {
        Fixture f = new Fixture();
        f.install();
        f.activePerson = f.person;
        f.plugin = mock(InteractionDialogPlugin.class, (name, args) -> {
            if (name.equals("notifyActivePersonChanged")) f.nativeNotifications++;
            return null;
        }, RuleBasedDialog.class);
        InteractionDialogPlugin original = f.plugin;
        f.executeEndContact(Map.of(MemKeys.LOCAL, f.personMemory));
        check(f.plugin == original && f.directoryOpens == 0 && f.dismissals == 0,
                "Non-Ithaca dialogs must retain native EndConversation rather than Ithaca handoff");
        check(f.activePerson == null && f.nativeNotifications == 1,
                "The fallback must perform native active-person cleanup");
        check(f.nativePopulation == 1, "The fallback must fire native PopulateOptions");
    }

    /** Small RFC-4180 reader: rule prose and scripts can contain commas and newlines. */
    private static Map<String, List<String>> readRules() throws Exception {
        String csv = Files.readString(ROOT.resolve("data/campaign/rules.csv"), StandardCharsets.UTF_8);
        Map<String, List<String>> rows = new HashMap<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else quoted = !quoted;
            } else if (!quoted && (c == ',' || c == '\n' || c == '\r')) {
                row.add(cell.toString());
                cell.setLength(0);
                if (c != ',') {
                    if (!row.get(0).isBlank()) rows.put(row.get(0), List.copyOf(row));
                    row.clear();
                    if (c == '\r' && i + 1 < csv.length() && csv.charAt(i + 1) == '\n') i++;
                }
            } else cell.append(c);
        }
        check(!quoted, "rules.csv must have balanced quoted cells");
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.put(row.get(0), List.copyOf(row));
        }
        return rows;
    }

    private static void verifyRules() throws Exception {
        Map<String, List<String>> rules = readRules();
        List<String> exits = List.of("chiefNavigatorMenelausLaborOneLeave",
                "chiefNavigatorMenelausLaborTwoLeave", "chiefNavigatorMenelausLaborThreeLeave",
                "chiefNavigatorMenelausLaborFourLeave", "chiefNavigatorMenelausLaborFiveBegin",
                "chiefNavigatorFobIthacaLeave");
        for (String id : exits) {
            check(rules.containsKey(id), "Missing authored Menelaus exit: " + id);
            check(rules.get(id).get(3).equals("ChiefNavigatorMenelausCMD endContact"),
                    id + " must use the scoped directory-return bridge");
        }
        List<String> guardianLeave = rules.get("chiefNavigatorLastLightGuardianLockedLeave");
        check(guardianLeave.get(1).equals("DialogOptionSelected")
                        && guardianLeave.get(2).equals("$option == chief_navigator_last_light_guardian_locked_leave")
                        && guardianLeave.get(3).equals("DismissDialog"),
                "The standalone locked Guardian's Withdraw must dismiss, not repopulate a contact menu");
        check(rules.get("chiefNavigatorLastLightGuardianLocked").get(5)
                        .equals("100:chief_navigator_last_light_guardian_locked_leave:Withdraw."),
                "The locked Guardian must retain its one matching Withdraw option and no recovery action");
        int[] guardianDismissals = {0};
        InteractionDialogAPI guardianDialog = (InteractionDialogAPI) Proxy.newProxyInstance(
                InteractionDialogAPI.class.getClassLoader(), new Class<?>[]{InteractionDialogAPI.class},
                (proxy, method, arguments) -> {
                    check(method.getName().equals("dismiss"),
                            "Guardian withdrawal must only close its dialog, not reinitialize menus or mutate the wreck");
                    guardianDismissals[0]++;
                    return null;
                });
        check(new DismissDialog().execute(guardianLeave.get(0), guardianDialog,
                        List.of(), Map.of()) && guardianDismissals[0] == 1,
                "The authored Guardian exit must invoke native dialog dismissal exactly once");
        long bridges = rules.values().stream()
                .filter(row -> row.size() > 3 && row.get(3).equals("ChiefNavigatorMenelausCMD endContact"))
                .count();
        check(bridges == 6, "The bridge must cover exactly the six Menelaus exit rules");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        SettingsAPI previousSettings = Global.getSettings();
        try {
            verifyStaleHub(BACK);
            verifyStaleHub(CONTACT_LEAVE);
            verifyNormalContactReturn();
            verifyAuthoredLaborExit();
            verifyLegacyContactShell();
            verifyNativeFallback();
            verifyRules();
            System.out.println("FOB Ithaca directory-return regression passed");
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
    }
}
