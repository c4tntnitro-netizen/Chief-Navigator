package chiefnavigator.quest;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.fleet.FleetLogisticsAPI;
import com.fs.starfarer.api.impl.campaign.GateEntityPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Entities;
import com.fs.starfarer.api.impl.campaign.ids.Items;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.GateCMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireAll;
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest;
import com.fs.starfarer.campaign.rules.A;
import com.fs.starfarer.campaign.rules.ooOO;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import data.campaign.rulecmd.ChiefNavigatorIthacaGateCMD;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual native scanned-gate registry/picker plus Chief Navigator's scoped route.
 * Public API fixtures do not render the campaign map or prove visible UI layout.
 */
public final class IthacaGateTravelRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            if (method.getName().equals("toString")) return type.getSimpleName();
            Object result = call.invoke(method.getName(), args);
            if (result != null) return result;
            Class<?> returns = method.getReturnType();
            if (returns == boolean.class) return false;
            if (returns == int.class) return 0;
            if (returns == long.class) return 0L;
            if (returns == float.class) return 0f;
            if (returns == double.class) return 0d;
            return null;
        }));
    }
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    static void near(float expected, float actual, String message) {
        check(Math.abs(expected - actual) < .001f, message + ": expected " + expected + ", got " + actual);
    }
    static String key(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }
    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("get")) return values.get(a[0]);
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(values.get(a[0]));
            if (n.equals("getString")) return values.get(a[0]);
            if (n.equals("contains")) return values.containsKey(a[0]);
            if (n.equals("set")) values.put((String) a[0], a[1]);
            if (n.equals("unset")) values.remove(a[0]);
            return null;
        });
    }
    static final class GatePlugin extends GateEntityPlugin {
        int scans;
        int uses;
        @Override public void advance(float amount) { scans++; }
        @Override public void showBeingUsed(float distance) { uses++; }
        @Override public void showBeingUsed(float distance, float angle) { uses++; }
    }
    static final class Place {
        final Memory memory = new Memory();
        final Map<String, SectorEntityToken> entities = new LinkedHashMap<>();
        final Set<String> tags = new HashSet<>();
        String id;
        final StarSystemAPI api;
        Place(String id) {
            this.id = id;
            api = mock(StarSystemAPI.class, (n, a) -> {
                switch (n) {
                    case "getOptionalUniqueId": case "getId": return this.id;
                    case "getName": case "getNameWithTypeShort": return this.id;
                    case "getMemoryWithoutUpdate": return memory.api;
                    case "getEntityById": return entities.get(a[0]);
                    case "getAllEntities": return new ArrayList<>(entities.values());
                    case "hasTag": return tags.contains(a[0]);
                    case "getLocation": return new Vector2f();
                    case "addEntity": case "removeEntity": case "addCustomEntity": case "addScript":
                        throw new AssertionError("Gate travel must not reconstruct or populate campaign locations");
                    default: break;
                }
                return null;
            });
        }
    }
    static final class Gate {
        final Memory memory = new Memory();
        final Set<String> tags = new HashSet<>();
        final Vector2f position = new Vector2f();
        final GatePlugin plugin = new GatePlugin();
        String id;
        String type = Entities.INACTIVE_GATE;
        Place where;
        boolean nativePlugin = true;
        final SectorEntityToken api;
        Gate(String id, Place where, float x) {
            this.id = id;
            this.where = where;
            position.set(x, 0);
            tags.add(Tags.GATE);
            api = mock(SectorEntityToken.class, (n, a) -> {
                switch (n) {
                    case "getId": case "getName": return this.id;
                    case "getCustomEntityType": return type;
                    case "getMemoryWithoutUpdate": return memory.api;
                    case "getContainingLocation": case "getStarSystem": return this.where == null ? null : this.where.api;
                    case "getLocation": case "getLocationInHyperspace": return position;
                    case "getCustomPlugin": return nativePlugin ? plugin : null;
                    case "hasTag": return tags.contains(a[0]);
                    case "getTags": return tags;
                    default: break;
                }
                return null;
            });
            where.entities.put(id, api);
        }
    }
    static final class Fixture {
        final Memory global = new Memory();
        final Map<String, Object> persistent = new HashMap<>();
        final Place ashen;
        final Place core;
        final Gate target;
        final Gate source;
        Gate current;
        Place playerLocation;
        final GateEntityPlugin.GateData data = new GateEntityPlugin.GateData();
        final List<String> messages = new ArrayList<>();
        final Map<String, Boolean> options = new HashMap<>();
        List<SectorEntityToken> pickedList;
        CampaignEntityPickerListener picker;
        RulesAPI rules;
        boolean strictOptionOrder;
        boolean hasJanus;
        final InteractionDialogAPI dialog;
        final CampaignFleetAPI player;
        float fuel = 1000f;
        int fuelCharges;
        int transitions;
        int dismissals;
        int transitReports;
        boolean playerInBattle;
        boolean playerTransitioning;
        float duration;
        SectorEntityToken departure;
        SectorEntityToken destination;
        Fixture() throws Exception {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
                if (n.equals("getColor")) return Color.WHITE;
                if (n.equals("getFloat")) return "unitsPerLightYear".equals(a[0]) ? 2000f : .5f;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (n, a) -> {
                if (n.equals("createMemory")) return new Memory().api;
                throw new AssertionError("Gate travel must not invoke a world factory: " + n);
            }));
            ashen = new Place(OdysseyExpanseSystem.ASHEN_VERGE_ID);
            ashen.memory.values.put("$chief_navigator_odyssey_authored_system_v1", true);
            core = new Place("core_test_system");
            target = new Gate(OdysseyExpanseSystem.FOB_ITHACA_GATE_ID, ashen, 20000);
            target.memory.values.put("$chief_navigator_fob_ithaca_gate_v1", true);
            source = new Gate("vanilla_gate_source", core, 0);
            current = source;
            playerLocation = core;
            target.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
            source.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
            data.scanned.add(source.api);
            data.scanned.add(target.api);
            persistent.put(GateEntityPlugin.GATE_DATA, data);
            global.values.put(GateEntityPlugin.GATES_ACTIVE, true);
            global.values.put(GateEntityPlugin.PLAYER_CAN_USE_GATES, true);
            global.values.put(key(MenelausTrial.class, "ACCEPTED"), true);
            global.values.put(key(MenelausTrial.class, "FINAL_LABOR_FORCED_FOR_TESTING"), true);
            global.values.put(key(MenelausTrial.class, "LABOR_REPORT_MIGRATION"), true);
            global.values.put(key(MenelausTrial.class, "GAUTAMA_DEFEATED"), true);
            global.values.put(MenelausTrial.FINAL_DEBRIEF_COMPLETE, true);
            CargoAPI cargo = mock(CargoAPI.class, (n, a) -> {
                if (n.equals("getFuel")) return fuel;
                if (n.equals("removeFuel")) { fuel -= ((Number) a[0]).floatValue(); fuelCharges++; }
                if (n.equals("getQuantity") && a.length == 2 && a[1] instanceof SpecialItemData item
                        && Items.JANUS.equals(item.getId())) return hasJanus ? 1f : 0f;
                return null;
            });
            FleetLogisticsAPI logistics = mock(FleetLogisticsAPI.class, (n, a) -> n.equals("getFuelCostPerLightYear") ? 10f : null);
            BattleAPI battle = mock(BattleAPI.class, (n, a) -> null);
            player = mock(CampaignFleetAPI.class, (n, a) -> {
                if (n.equals("getCargo")) return cargo;
                if (n.equals("getLogistics")) return logistics;
                if (n.equals("getContainingLocation")) return playerLocation.api;
                if (n.equals("getLocation") || n.equals("getLocationInHyperspace")) return source.position;
                if (n.equals("getMemoryWithoutUpdate")) return global.api;
                if (n.equals("getBattle")) return playerInBattle ? battle : null;
                if (n.equals("isInHyperspaceTransition")) return playerTransitioning;
                return null;
            });
            GateTransitListener listener = (fleet, from, to) -> {
                check(fleet == player && from == current.api && to == target.api, "Native transit event retains exact identities");
                transitReports++;
            };
            ListenerManagerAPI listeners = mock(ListenerManagerAPI.class, (n, a) -> n.equals("getListeners") ? List.of(listener) : null);
            FactionAPI faction = mock(FactionAPI.class, (n, a) -> null);
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                switch (n) {
                    case "getPlayerFleet": return player;
                    case "getMemoryWithoutUpdate": case "getPlayerMemoryWithoutUpdate": return global.api;
                    case "getPersistentData": return persistent;
                    case "getPlayerFaction": return faction;
                    case "getStarSystems": return List.of(ashen.api, core.api);
                    case "getStarSystem": return ashen.id.equals(a[0]) ? ashen.api : core.id.equals(a[0]) ? core.api : null;
                    case "getListenerManager": return listeners;
                    case "getRules": return rules;
                    case "doHyperspaceTransition":
                        check(a[0] == player, "Transit moves only the player's fleet");
                        departure = (SectorEntityToken) a[1];
                        destination = ((JumpPointAPI.JumpDestination) a[2]).getDestination();
                        duration = a.length > 3 ? (Float) a[3] : 0f;
                        transitions++;
                        break;
                    default: break;
                }
                return null;
            }));
            LabelAPI label = mock(LabelAPI.class, (n, a) -> null);
            TextPanelAPI text = mock(TextPanelAPI.class, (n, a) -> {
                if (n.equals("addPara") || n.equals("addParagraph")) { messages.add((String) a[0]); return label; }
                return null;
            });
            OptionPanelAPI optionPanel = mock(OptionPanelAPI.class, (n, a) -> {
                if (n.equals("clearOptions")) options.clear();
                if (n.equals("setEnabled")) {
                    check(!strictOptionOrder || options.containsKey(a[0]),
                            "Native option must exist before its fuel highlight executes");
                    options.put((String) a[0], (Boolean) a[1]);
                }
                if (n.equals("addOption")) options.put((String) a[1], true);
                return null;
            });
            dialog = mock(InteractionDialogAPI.class, (n, a) -> {
                if (n.equals("getInteractionTarget")) return current.api;
                if (n.equals("getTextPanel")) return text;
                if (n.equals("getOptionPanel")) return optionPanel;
                if (n.equals("dismiss")) dismissals++;
                if (n.equals("showCampaignEntityPicker")) {
                    @SuppressWarnings("unchecked") List<SectorEntityToken> list = (List<SectorEntityToken>) a[4];
                    pickedList = new ArrayList<>(list);
                    picker = (CampaignEntityPickerListener) a[5];
                }
                return null;
            });
        }
        Map<String, MemoryAPI> scope() {
            return new HashMap<>(Map.of(MemKeys.GLOBAL, global.api, MemKeys.PLAYER, global.api,
                    MemKeys.LOCAL, current.memory.api, MemKeys.ENTITY, current.memory.api));
        }
        boolean execute(String action) {
            return new ChiefNavigatorIthacaGateCMD().execute(null, dialog,
                    List.of(new Token(action, TokenType.LITERAL)), scope());
        }
    }

    static void rejected(String reason, Consumer<Fixture> mutation) throws Exception {
        Fixture fixture = new Fixture();
        mutation.accept(fixture);
        int originalScanned = fixture.data.scanned == null ? -1 : fixture.data.scanned.size();
        Map<String, Object> originalGlobal = new HashMap<>(fixture.global.values);
        Map<String, Object> originalPersistent = new HashMap<>(fixture.persistent);
        check(!fixture.execute("canTravel"), reason);
        fixture.execute("traverse");
        check(fixture.transitions == 0 && fixture.fuelCharges == 0 && fixture.transitReports == 0,
                "Rejected route cannot spend fuel, move fleets or emit events: " + reason);
        check((fixture.data.scanned == null ? -1 : fixture.data.scanned.size()) == originalScanned
                        && fixture.global.values.equals(originalGlobal) && fixture.persistent.equals(originalPersistent),
                "Rejected route cannot grant scans, Janus access or story progress: " + reason);
    }

    static void testGuards() throws Exception {
        rejected("Gautama victory alone is not a completed debrief", f -> f.global.values.remove(MenelausTrial.FINAL_DEBRIEF_COMPLETE));
        rejected("Final-Labor failure blocks access", f -> f.global.values.put(MenelausTrial.FINAL_LABOR_FAILED, true));
        rejected("Ithaca must actually have a player scan flag", f -> f.target.memory.values.remove(GateEntityPlugin.GATE_SCANNED));
        rejected("Ithaca must be in the actual persistent scanned registry", f -> f.data.scanned.remove(f.target.api));
        rejected("An absent shared gate registry is never fabricated", f -> f.persistent.remove(GateEntityPlugin.GATE_DATA));
        rejected("A foreign shared gate registry is never replaced", f -> f.persistent.put(GateEntityPlugin.GATE_DATA, "foreign claim"));
        rejected("A malformed scanned set is never repaired", f -> f.data.scanned = null);
        rejected("Removed Ithaca gate must never be recreated", f -> f.ashen.entities.remove(f.target.id));
        rejected("Wrong-type canonical ID is not adopted", f -> f.target.type = "sensor_array");
        rejected("Wrong destination plugin is not adopted", f -> f.target.nativePlugin = false);
        rejected("Destination still requires the native gate tag", f -> f.target.tags.remove(Tags.GATE));
        rejected("Foreign destination location is not adopted", f -> f.target.where = f.core);
        rejected("Foreign Lacedaemon ownership is not adopted", f -> f.ashen.memory.values.clear());
        rejected("Unscanned source gates cannot be used", f -> f.source.memory.values.remove(GateEntityPlugin.GATE_SCANNED));
        rejected("Unregistered source gates cannot be used", f -> f.data.scanned.remove(f.source.api));
        rejected("Non-gate source entities cannot be used", f -> f.source.tags.remove(Tags.GATE));
        rejected("Wrong custom plugin cannot be used", f -> f.source.nativePlugin = false);
        rejected("A stale remote source cannot be used", f -> f.playerLocation = f.ashen);
        rejected("A fleet already in battle cannot transit", f -> f.playerInBattle = true);
        rejected("A fleet already transitioning cannot transit", f -> f.playerTransitioning = true);
        rejected("Ithaca never lists itself as a destination", f -> { f.current = f.target; f.playerLocation = f.ashen; });
        rejected("Native gate activation remains required", f -> f.global.values.put(GateEntityPlugin.GATES_ACTIVE, false));
        rejected("Native Janus capability remains required", f -> f.global.values.put(GateEntityPlugin.PLAYER_CAN_USE_GATES, false));
        rejected("Devoured Ring does not become a usable source", f -> {
            f.source.id = OdysseyExpanseSystem.DEVOURED_RING_ID;
            f.source.type = "chief_navigator_devoured_ring";
        });
        rejected("A scanned lookalike cannot substitute for the live gate", f -> {
            Gate clone = new Gate(f.target.id, f.core, 20000);
            clone.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
            f.data.scanned.remove(f.target.api);
            f.data.scanned.add(clone.api);
        });
    }

    static void testNativePicker() throws Exception {
        Fixture f = new Fixture();
        Gate rat = new Gate("rat_gate_reference", f.core, 1000);
        Gate kol = new Gate("zea_nullgate_dusk_reference", f.core, 2000);
        rat.tags.add("rat_abyss_gate");
        f.data.scanned.add(rat.api);
        f.data.scanned.add(kol.api);
        new GateCMD().execute(null, f.dialog, List.of(new Token("selectDestination", TokenType.LITERAL)), f.scope());
        check(f.pickedList.contains(f.target.api) && !f.pickedList.contains(f.source.api),
                "Actual native picker receives scanned Ithaca once and omits its source");
        check(f.pickedList.contains(rat.api) && f.pickedList.contains(kol.api),
                "Native destination registry preserves other mods' special gates");
        check(f.data.scanned.size() == 4, "Opening native picker never rewrites the shared registry");
        int cost = GateCMD.computeFuelCost(f.target.api);
        check(cost > 0, "Fixture must exercise an actual nonzero native fuel cost");
        f.fuel = cost - .1f;
        check(!f.picker.canConfirmSelection(f.target.api), "Native picker retains insufficient-fuel block");
        f.fuel = cost;
        check(f.picker.canConfirmSelection(f.target.api), "Native picker accepts exactly sufficient fuel");
        f.target.memory.values.remove(GateEntityPlugin.GATE_SCANNED);
        f.data.scanned.remove(f.target.api);
        check(!f.execute("canTravel"), "No automatic scan is granted by route population");
        // Mirror the actual vanilla scan script: set its native memory flag,
        // then invoke vanilla's notification, which owns persistent registration.
        f.target.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
        GateCMD.notifyScanned(f.target.api);
        check(f.target.plugin.scans == 1 && f.execute("canTravel"),
                "The real native scan-notification path unlocks the scoped route");
    }

    static void testFuelAndTravel() throws Exception {
        Fixture f = new Fixture();
        check(f.execute("canTravel"), "Completed, actually scanned Ithaca is eligible");
        int cost = GateCMD.computeFuelCost(f.target.api);
        f.fuel = cost - .1f;
        f.execute("highlight");
        check(Boolean.FALSE.equals(f.options.get(ChiefNavigatorIthacaGateCMD.OPTION_ID)),
                "Insufficient fuel disables the special option");
        f.execute("traverse");
        check(f.transitions == 0 && f.fuelCharges == 0, "Forged insufficient-fuel click is safely rejected");
        f.fuel = cost;
        f.execute("traverse");
        check(f.transitions == 1 && f.fuelCharges == 1, "Exactly sufficient fuel performs one transit");
        near(0f, f.fuel, "Special route charges the exact native fuel cost");
        near(2f, f.duration, "Special route retains native gate transit duration");
        check(f.departure == f.source.api && f.destination == f.target.api && f.dismissals == 1,
                "Transit uses the exact existing gate entities and closes its dialog");
        check(f.transitReports == 1 && f.source.plugin.uses > 0 && f.target.plugin.uses > 0,
                "Transit preserves native gate visuals and listener notification");
        f.fuel = 1000f;
        f.execute("traverse");
        check(f.transitions == 1 && f.fuelCharges == 1 && f.transitReports == 1,
                "Repeated queued selection cannot spend or transit twice");
        for (String id : List.of("rat_abyss_gate_source", "zea_nullgate_dusk_source")) {
            Fixture special = new Fixture();
            special.source.id = id;
            special.source.tags.add(id.startsWith("rat") ? "rat_abyss_gate" : "zea_nullgate");
            check(special.execute("canTravel"), "Special scanned source remains compatible: " + id);
            special.execute("traverse");
            check(special.transitions == 1 && special.transitReports == 1,
                    "Special source follows the shared native transit contract: " + id);
        }
        Fixture stale = new Fixture();
        check(stale.execute("canTravel"), "Initially eligible selection can be displayed");
        stale.data.scanned.remove(stale.target.api);
        stale.execute("traverse");
        check(stale.transitions == 0 && stale.fuelCharges == 0, "Execution rechecks eligibility after menu opening");
    }

    static Option option(float order, String id, String text) {
        Option option = new Option();
        option.order = order;
        option.id = id;
        option.text = text;
        return option;
    }

    static RuleAPI menuRule(String id, List<Option> options, Runnable script) {
        return mock(RuleAPI.class, (n, a) -> {
            if (n.equals("getId")) return id;
            if (n.equals("getOptions")) return options;
            if (n.equals("getText") || n.equals("getScriptCopy")) return List.of();
            if (n.equals("runScript") && script != null) script.run();
            return null;
        });
    }

    static List<ooOO> authoredTravelRules() throws Exception {
        // Run the real authored predicates and scripts, including native command lookup.
        Class<?> settings = Class.forName("com.fs.starfarer.settings.StarfarerSettings");
        JSONObject config = new JSONObject().put("ruleCommandPackages", new JSONArray()
                .put("com.fs.starfarer.api.impl.campaign.rulecmd").put("data.campaign.rulecmd"));
        for (Field field : settings.getDeclaredFields()) {
            if (field.getType() == JSONObject.class && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                field.set(null, config);
            }
        }
        Class<?> store = Class.forName("com.fs.starfarer.loading.scripts.ScriptStore");
        for (Field field : store.getDeclaredFields()) {
            if (field.getType() == org.codehaus.janino.JavaSourceClassLoader.class) {
                field.setAccessible(true);
                field.set(null, new org.codehaus.janino.JavaSourceClassLoader(
                        IthacaGateTravelRegression.class.getClassLoader()));
            }
        }
        @SuppressWarnings("unchecked") Map<String, Class<?>> scripts =
                (Map<String, Class<?>>) java.util.Arrays.stream(store.getFields()).filter(field -> field.getType() == java.util.Map.class).findFirst().orElseThrow().get(null);
        scripts.put(ChiefNavigatorIthacaGateCMD.class.getName(), ChiefNavigatorIthacaGateCMD.class);
        Method parse = Class.forName("com.fs.starfarer.loading.G").getMethod("o00000", String.class);
        JSONArray rows = (JSONArray) parse.invoke(null, Files.readString(Path.of("data/campaign/rules.csv")));
        List<ooOO> rules = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (!row.optString("id").startsWith("chiefNavigatorIthacaGateTravel")) continue;
            ooOO rule = new ooOO(row.getString("id"), row.getString("trigger"), row.optString("conditions"));
            for (String line : row.optString("conditions").split("\\R")) {
                if (!line.isBlank()) rule.getConditions().add(new A(line));
            }
            for (String line : row.optString("script").split("\\R")) {
                if (!line.isBlank()) rule.getScript().add(new A(line));
            }
            for (String line : row.optString("options").split("\\R")) {
                if (line.isBlank()) continue;
                String[] parts = line.split(":", 3);
                rule.getOptions().add(option(Float.parseFloat(parts[0]), parts[1], parts[2]));
            }
            rules.add(rule);
        }
        check(rules.size() == 3, "Load exactly the authored Ithaca option, tooltip and selection rules");
        return rules;
    }

    static void testNativeMenuPopulation() throws Exception {
        Fixture f = new Fixture();
        f.strictOptionOrder = true;
        f.target.position.set(150000f, -94000f);
        List<ooOO> authored = authoredTravelRules();
        RuleAPI otherGates = menuRule("other_gate_options", List.of(
                option(10f, "gateSelectDestination", "Select a scanned destination."),
                option(15f, "ABYSSAL_GATE", "Travel to the Abyssal Gate."),
                option(16f, "NULL_GATE", "Travel to the Null Gate.")), null);
        f.rules = mock(RulesAPI.class, (n, a) -> {
            if (n.equals("getAllMatching") || n.equals("getBestMatching")) {
                @SuppressWarnings("unchecked") Map<String, MemoryAPI> scope = (Map<String, MemoryAPI>) a[3];
                List<RuleAPI> matches = new ArrayList<>();
                if (a[1].equals("PopulateGateOptions")) matches.add(otherGates);
                for (ooOO rule : authored) {
                    if (rule.getTrigger().equals(a[1]) && rule.getConditions().stream()
                            .allMatch(e -> e.isTrueFor(rule.getId(), f.dialog, scope))) matches.add(rule);
                }
                return n.equals("getAllMatching") ? matches : matches.isEmpty() ? null : matches.get(0);
            }
            if (n.equals("performTokenReplacement")) return a[1];
            return null;
        });
        int cost = GateCMD.computeFuelCost(f.target.api);
        f.fuel = cost - .1f;
        check(FireAll.fire(null, f.dialog, f.scope(), "PopulateGateOptions"), "Actual FireAll populates the special destination");
        check(f.options.keySet().equals(Set.of("gateSelectDestination", "ABYSSAL_GATE", "NULL_GATE",
                        ChiefNavigatorIthacaGateCMD.OPTION_ID)),
                "Actual native menu retains ordinary and other-mod destinations alongside Ithaca");
        check(Boolean.FALSE.equals(f.options.get(ChiefNavigatorIthacaGateCMD.OPTION_ID)),
                "Native FireAll adds the destination before disabling insufficient-fuel travel");
        f.fuel = cost;
        FireAll.fire(null, f.dialog, f.scope(), "PopulateGateOptions");
        check(Boolean.TRUE.equals(f.options.get(ChiefNavigatorIthacaGateCMD.OPTION_ID)),
                "Native menu enables travel at the exact native fuel boundary");
        f.global.values.remove(GateEntityPlugin.GATES_ACTIVE);
        f.global.values.remove(GateEntityPlugin.PLAYER_CAN_USE_GATES);
        f.hasJanus = true;
        FireAll.fire(null, f.dialog, f.scope(), "PopulateGateOptions");
        check(Boolean.TRUE.equals(f.options.get(ChiefNavigatorIthacaGateCMD.OPTION_ID)),
                "Authored menu honors native Janus-item access without raw global quest flags");
        f.hasJanus = false;
        FireAll.fire(null, f.dialog, f.scope(), "PopulateGateOptions");
        check(!f.options.containsKey(ChiefNavigatorIthacaGateCMD.OPTION_ID),
                "The authored menu still requires actual native Janus access");
        f.hasJanus = true;
        f.target.memory.values.remove(GateEntityPlugin.GATE_SCANNED);
        FireAll.fire(null, f.dialog, f.scope(), "PopulateGateOptions");
        check(!f.options.containsKey(ChiefNavigatorIthacaGateCMD.OPTION_ID)
                        && f.options.keySet().equals(Set.of("gateSelectDestination", "ABYSSAL_GATE", "NULL_GATE")),
                "Native menu removes only Ithaca's unavailable special destination");
        f.target.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
        f.source.memory.values.put("$option", ChiefNavigatorIthacaGateCMD.OPTION_ID);
        check(FireBest.fire(null, f.dialog, f.scope(), "DialogOptionSelected"),
                "The direct dialogue selection dispatches through native expressions");
        check(f.transitions == 1 && f.destination == f.target.api && f.transitReports == 1,
                "The authored shortcut reaches off-map Ithaca without opening the map picker");
        check(f.picker == null, "Direct transit must not open the map-bound destination picker");
    }

    static void testCompletedScanGuidance() throws Exception {
        Fixture f = new Fixture();
        f.global.values.put(OdysseyExpanseSystem.ENTERED, true);
        chiefnavigator.weapons.IthacaResearchUpgrades.enableMaximumUpgradeOverride();
        Gate ithaca = new Gate(key(OdysseyExpanseSystem.class, "FOB_ITHACA_ID"), f.ashen, 19000f);
        ithaca.type = key(OdysseyExpanseSystem.class, "FOB_ITHACA_TYPE");
        ithaca.memory.values.put(key(OdysseyExpanseSystem.class, "FOB_ITHACA_OWNER_MARKER"), true);
        f.target.memory.values.remove(GateEntityPlugin.GATE_SCANNED);
        SinniNavigationGuidance.Guidance before = SinniNavigationGuidance.getCurrentGuidance();
        check(before.getTarget() == f.target.api && before.getObjective().contains("Scan Ithaca"),
                "Sinni still directs the player to the actual unscanned gate after the debrief");
        f.target.memory.values.put(GateEntityPlugin.GATE_SCANNED, true);
        GateCMD.notifyScanned(f.target.api);
        for (boolean budaiDone : new boolean[]{false, true}) {
            for (boolean ungaikyoDone : new boolean[]{false, true}) {
                f.global.values.put(OdysseyPredatorScript.BUDAI_DEFEATED, budaiDone);
                f.global.values.put(OdysseyPredatorScript.GAUTAMA_FINAL_REMATCH_DEFEATED, ungaikyoDone);
                Map<String, Object> globalBefore = new HashMap<>(f.global.values);
                for (int revisit = 0; revisit < 3; revisit++) {
                    SinniNavigationGuidance.Guidance after = SinniNavigationGuidance.getCurrentGuidance();
                    check(after.getObjective().contains("has been scanned")
                                    && !after.getObjective().contains("Scan Ithaca"),
                            "An existing native scan clears Sinni's repeated scan objective");
                    check(!after.getRoute().contains("Travel to Ithaca Janus Gate"),
                            "The gate-return blurb is removed from post-scan guidance");
                    if (budaiDone && ungaikyoDone) {
                        check(after.getDestination().equals("Open exploration")
                                        && after.getTarget() == null && after.getRoute().isEmpty()
                                        && !after.getObjective().contains("Aias Kleon"),
                                "Both completed hunts release Kleon's recommendation and the lower blurb");
                    } else {
                        check(after.getDestination().contains("Captain Aias Kleon")
                                        && after.getObjective().contains("unfinished hunts")
                                        && after.getTarget() == ithaca.api,
                                "Either unfinished hunt sends the player to Kleon's actual Ithaca location");
                    }
                }
                check(f.global.values.equals(globalBefore),
                        "Reading hunt guidance never changes quest progress or grants a briefing");
            }
        }
        f.target.memory.values.remove(GateEntityPlugin.GATE_SCANNED);
        check(SinniNavigationGuidance.getCurrentGuidance().getTarget() == f.target.api,
                "Guidance reads live native scan state rather than caching completion");
    }

    static void testAuthoredRules() throws Exception {
        Method parse = Class.forName("com.fs.starfarer.loading.G").getMethod("o00000", String.class);
        JSONArray rows = (JSONArray) parse.invoke(null, Files.readString(Path.of("data/campaign/rules.csv")));
        JSONObject option = null, highlight = null, travel = null;
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            if (row.optString("trigger").equals("PopulateGateOptions")
                    && row.optString("options").contains(ChiefNavigatorIthacaGateCMD.OPTION_ID)) {
                check(option == null, "Special Ithaca option is registered once");
                option = row;
            }
            if (row.optString("trigger").equals("DialogOptionSelected")
                    && row.optString("conditions").contains(ChiefNavigatorIthacaGateCMD.OPTION_ID)) travel = row;
            if (row.optString("trigger").equals("PopulateGateOptions")
                    && row.optString("script").contains("ChiefNavigatorIthacaGateCMD highlight")) highlight = row;
        }
        check(option != null && option.getString("options").equals("17:" + ChiefNavigatorIthacaGateCMD.OPTION_ID + ":Travel to Ithaca Janus Gate."),
                "Named Ithaca destination is added to the native scanned-gate menu");
        check(option.getString("conditions").contains("ChiefNavigatorIthacaGateCMD canTravel")
                        && highlight != null && highlight.getString("conditions").equals(option.getString("conditions")),
                "Authored menu uses the tested eligibility and fuel presentation commands");
        check(travel != null && travel.getString("script").contains("ChiefNavigatorIthacaGateCMD traverse"),
                "Authored selection invokes the execution recheck");
    }

    public static void main(String[] args) throws Exception {
        org.apache.log4j.BasicConfigurator.configure();
        testGuards();
        testNativePicker();
        testFuelAndTravel();
        testNativeMenuPopulation();
        testCompletedScanGuidance();
        testAuthoredRules();
        System.out.println("PASS: completed/scanned owned Ithaca destination; actual native registry/picker; "
                + "authored off-map dialogue transit, Janus access, native fuel/listeners, repeat-click safety, "
                + "special-gate coexistence and live completed-scan Sinni guidance");
    }
}
