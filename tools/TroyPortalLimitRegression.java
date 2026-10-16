package chiefnavigator.quest;

import chiefnavigator.config.ChiefNavigatorSettings;
import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import data.campaign.rulecmd.ChiefNavigatorSinniIntroCMD;
import java.awt.Color;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual optional LunaLib API and portal execution guards; no persistent files written.
 * Run in separate JVMs without LunaLib, then with installed LunaLib/its dependencies
 * and --luna. Optional library state is seeded only in this external test process.
 */
public final class TroyPortalLimitRegression {
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
    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("set")) values.put((String) a[0], a[1]);
            if (n.equals("unset")) values.remove(a[0]);
            if (n.equals("get")) return values.get(a[0]);
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(values.get(a[0]));
            if (n.equals("getString")) return values.get(a[0]);
            if (n.equals("contains")) return values.containsKey(a[0]);
            return null;
        });
    }
    static final class Fixture {
        boolean lunaEnabled;
        boolean missingManager;
        boolean failingManager;
        float dp = 50f;
        int transitions;
        int courses;
        int systemLookups;
        int dismissals;
        final Memory global = new Memory(), local = new Memory(), systemMemory = new Memory(), wormMemory = new Memory();
        final List<String> triggers = new ArrayList<>(), paragraphs = new ArrayList<>();
        final Map<String, Boolean> enabled = new HashMap<>();
        final Map<String, String> tooltips = new HashMap<>();
        StarSystemAPI troy;
        JumpPointAPI terminus;
        CampaignFleetAPI player;
        InteractionDialogAPI dialog;
        JSONObject lunaValues;

        Fixture() {
            ModManagerAPI mods = mock(ModManagerAPI.class, (n, a) -> {
                if (n.equals("isModEnabled")) {
                    if (failingManager) throw new IllegalStateException("Uninitialized mod manager fixture");
                    return "lunalib".equals(a[0]) && lunaEnabled;
                }
                return null;
            });
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
                if (n.equals("getModManager")) return missingManager ? null : mods;
                if (n.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (n, a) -> n.equals("createMemory") ? new Memory().api : null));
            global.values.put(SinniEventideIntel.BRIEFED, true);
            systemMemory.values.put("$chief_navigator_troy_authored_system_v1", true);
            wormMemory.values.put("$chief_navigator_troy_wormhole_v1", true);
            troy = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return TroyArrivalScript.SYSTEM_ID;
                if (n.equals("getMemoryWithoutUpdate")) return systemMemory.api;
                if (n.equals("getEntityById")) return TroyArrivalScript.WORMHOLE_ID.equals(a[0]) ? terminus : null;
                if (n.equals("getLocation")) return new Vector2f();
                if (n.equals("getName")) return "Waypoint Troy";
                return null;
            });
            terminus = mock(JumpPointAPI.class, (n, a) -> {
                if (n.equals("getId")) return TroyArrivalScript.WORMHOLE_ID;
                if (n.equals("getContainingLocation")) return troy;
                if (n.equals("getMemoryWithoutUpdate")) return wormMemory.api;
                if (n.equals("getLocation")) return new Vector2f();
                return null;
            });
            FleetMemberAPI member = mock(FleetMemberAPI.class, (n, a) -> n.equals("getDeploymentPointsCost") ? dp : null);
            FleetMemberAPI wing = mock(FleetMemberAPI.class, (n, a) -> {
                if (n.equals("isFighterWing")) return true;
                if (n.equals("getDeploymentPointsCost")) return 500f;
                return null;
            });
            FleetDataAPI data = mock(FleetDataAPI.class, (n, a) -> n.equals("getMembersListCopy") ? List.of(member, wing) : null);
            player = mock(CampaignFleetAPI.class, (n, a) -> {
                if (n.equals("getFleetData")) return data;
                if (n.equals("getContainingLocation")) return troy;
                if (n.equals("getLocation")) return new Vector2f();
                return null;
            });
            RuleAPI rule = mock(RuleAPI.class, (n, a) -> {
                if (n.equals("getId")) return "portal_test";
                if (n.equals("getText") || n.equals("getOptions") || n.equals("getScriptCopy")) return List.of();
                return null;
            });
            RulesAPI rules = mock(RulesAPI.class, (n, a) -> {
                if (n.equals("getBestMatching")) { triggers.add((String) a[1]); return rule; }
                if (n.equals("getAllMatching")) return List.of();
                if (n.equals("getTokenReplacements")) return Map.of();
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                switch (n) {
                    case "getPlayerFleet": return player;
                    case "getMemoryWithoutUpdate": case "getPlayerMemoryWithoutUpdate": return global.api;
                    case "getRules": return rules;
                    case "getStarSystems": systemLookups++; return List.of(troy);
                    case "layInCourseFor": courses++; break;
                    case "doHyperspaceTransition": transitions++; break;
                    default: break;
                }
                return null;
            }));
            LabelAPI label = mock(LabelAPI.class, (n, a) -> null);
            TextPanelAPI text = mock(TextPanelAPI.class, (n, a) -> {
                if (n.equals("addPara") || n.equals("addParagraph")) {
                    String value = (String) a[0];
                    if (a.length > 1 && a[a.length - 1] instanceof String[] tokens) value = String.format(value, (Object[]) tokens);
                    paragraphs.add(value);
                    return label;
                }
                return null;
            });
            OptionPanelAPI options = mock(OptionPanelAPI.class, (n, a) -> {
                if (n.equals("clearOptions")) { enabled.clear(); tooltips.clear(); }
                if (n.equals("addOption")) enabled.put((String) a[1], true);
                if (n.equals("setEnabled")) enabled.put((String) a[0], (Boolean) a[1]);
                if (n.equals("setTooltip")) tooltips.put((String) a[0], (String) a[1]);
                return null;
            });
            VisualPanelAPI visual = mock(VisualPanelAPI.class, (n, a) -> null);
            SectorEntityToken target = mock(SectorEntityToken.class, (n, a) -> {
                if (n.equals("getMemoryWithoutUpdate")) return local.api;
                return null;
            });
            dialog = mock(InteractionDialogAPI.class, (n, a) -> {
                if (n.equals("getOptionPanel")) return options;
                if (n.equals("getTextPanel")) return text;
                if (n.equals("getVisualPanel")) return visual;
                if (n.equals("getInteractionTarget")) return target;
                if (n.equals("dismiss")) dismissals++;
                return null;
            });
        }
        void useLuna() throws Exception {
            Class<?> loader = Class.forName("lunalib.backend.ui.settings.LunaSettingsLoader");
            Object singleton = loader.getField("INSTANCE").get(null);
            loader.getMethod("setHasLoaded", boolean.class).invoke(singleton, true);
            Class<?> json = Class.forName("org.lazywizard.lazylib.JSONUtils$CommonDataJSONObject");
            lunaValues = (JSONObject) json.getConstructor(String.class).newInstance("test-in-memory-only.json");
            Map<String, Object> values = new HashMap<>();
            values.put(ChiefNavigatorSettings.MOD_ID, lunaValues);
            loader.getMethod("setSettings", Map.class).invoke(null, values);
            lunaEnabled = true;
        }
        void limit(int value) throws Exception { lunaValues.put(ChiefNavigatorSettings.TROY_FLEET_DP_LIMIT, value); }
        void clean() { triggers.clear(); paragraphs.clear(); enabled.clear(); tooltips.clear(); systemLookups = 0; }
        boolean blocked(String option) { return Boolean.FALSE.equals(enabled.get(option)); }
    }

    static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        try { return method.invoke(target); }
        catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) throw cause;
            if (ex.getCause() instanceof Error error) throw error;
            throw ex;
        }
    }
    static void testSettings(boolean luna) throws Exception {
        Global.setSettings(null);
        near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "No settings API preserves default");
        // Reinstall the independent campaign fixture after the missing-API check.
        Fixture f = new Fixture();
        near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Disabled LunaLib preserves default");
        f.missingManager = true;
        near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Missing manager preserves default");
        f.missingManager = false;
        f.failingManager = true;
        near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Uninitialized optional setting is nonfatal");
        f.failingManager = false;
        f.lunaEnabled = true;
        if (!luna) {
            near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Absent LunaLib classes are safe even if reported enabled");
            return;
        }
        f.useLuna();
        for (int value : new int[]{50, 239, 240, 300, 1000}) {
            f.limit(value);
            near(value, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Native LunaLib live value " + value);
        }
        f.limit(-3);
        near(1f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Low stale value is clamped");
        f.limit(1001);
        near(1000f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "High stale value is clamped");
        org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger("lunalib.lunaSettings.LunaSettings");
        org.apache.log4j.Level oldLevel = logger.getLevel();
        try {
            // LunaLib deliberately logs its nullable result for these cases.
            // Suppress only those expected fixture errors in this process.
            logger.setLevel(org.apache.log4j.Level.OFF);
            f.lunaValues.remove(ChiefNavigatorSettings.TROY_FLEET_DP_LIMIT);
            near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Missing native field falls back to default");
            f.lunaValues.put(ChiefNavigatorSettings.TROY_FLEET_DP_LIMIT, JSONObject.NULL);
            near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Null native field falls back to default");
        } finally { logger.setLevel(oldLevel); }
        f.limit(300);
        f.lunaEnabled = false;
        near(50f, ChiefNavigatorSettings.getTroyFleetDPLimit(), "Disabled library ignores persisted slider value");
    }

    static void testPortal(Fixture f, int limit) throws Exception {
        if (f.lunaValues != null) f.limit(limit);
        f.dp = limit;
        near(limit, TroyDepartureInteraction.getFleetDP(f.player), "Fleet accounting excludes fighter-wing entries");
        TroyDepartureInteraction departure = new TroyDepartureInteraction();
        f.clean();
        departure.init(f.dialog);
        check(!f.blocked("chief_navigator_troy_depart"), "Exact DP limit permits opening departure route");
        check(Integer.toString(limit).equals(f.local.values.get("$chiefNavigatorTroyDPLimit")), "Departure displays current limit");
        f.dp = limit + .1f;
        call(departure, "showOptions");
        check(f.blocked("chief_navigator_troy_depart"), "Departure blocks any excess DP");
        f.clean();
        call(departure, "beginJourney");
        check(f.triggers.contains("ChiefNavigatorTroyAnchorage_invalid"), "Departure execution rechecks excess DP");
        f.dp = limit;
        int beforeCourse = f.courses;
        call(departure, "beginJourney");
        check(f.courses == beforeCourse + 1, "Exact DP boundary opens the actual existing terminus");
        f.global.values.remove(TroyArrivalScript.WORMHOLE_OPENED);

        TroyWormholeInteraction outward = new TroyWormholeInteraction();
        f.clean();
        outward.init(f.dialog);
        check(!f.blocked("chief_navigator_troy_enter"), "Exact DP limit permits outbound entry");
        f.dp = limit + .1f;
        outward.init(f.dialog);
        check(f.blocked("chief_navigator_troy_enter"), "Outbound entry displays excess-DP block");
        f.clean();
        call(outward, "beginTransit");
        check(f.triggers.contains("ChiefNavigatorTroyTerminus_invalid"), "Outbound selection rechecks fleet DP");
        f.dp = limit;
        f.clean();
        call(outward, "beginTransit");
        check(f.triggers.contains("ChiefNavigatorTroyTerminus_transit"), "Exact DP boundary begins outbound transit");
        f.dp = limit + .1f;
        f.clean();
        call(outward, "enterNebula");
        check(f.triggers.contains("ChiefNavigatorTroyTerminus_invalid") && f.systemLookups == 0,
                "Final outbound commit checks the limit again before world/cargo side effects");
        f.dp = limit;
        f.clean();
        call(outward, "enterNebula");
        check(f.systemLookups > 0 && !f.triggers.contains("ChiefNavigatorTroyTerminus_invalid"),
                "Exact DP final commit clears the gate before looking for an existing destination");

        OdysseyExpanseInteraction inward = new OdysseyExpanseInteraction();
        f.clean();
        inward.init(f.dialog);
        check(!f.blocked("return_to_troy"), "Exact DP boundary permits return entry");
        check(f.paragraphs.stream().anyMatch(p -> p.contains(limit + " / " + limit + " deployment points")),
                "Return manifest displays current configured limit");
        f.dp = limit + .1f;
        inward.init(f.dialog);
        check(f.blocked("return_to_troy"), "Return entry blocks excess DP");
        check(f.tooltips.get("return_to_troy").contains(limit + " DP"), "Return tooltip uses configured limit");
        int beforeTransition = f.transitions;
        call(inward, "returnToTroy");
        check(f.transitions == beforeTransition, "Return execution rechecks excess DP");
        check(f.paragraphs.get(f.paragraphs.size() - 1).contains(limit + " deployment points"),
                "Return rejection uses the live limit");
        f.dp = limit;
        call(inward, "returnToTroy");
        check(f.transitions == beforeTransition + 1, "Exact DP boundary executes actual return transition");
    }

    static void testLiveRecheck(Fixture f) throws Exception {
        f.limit(300);
        f.dp = 240;
        TroyDepartureInteraction departure = new TroyDepartureInteraction();
        departure.init(f.dialog);
        f.limit(239);
        f.clean();
        int priorCourse = f.courses;
        call(departure, "beginJourney");
        check(f.courses == priorCourse && f.triggers.contains("ChiefNavigatorTroyAnchorage_invalid"),
                "Slider reduction after opening departure UI blocks route authorization");
        check("239".equals(f.local.values.get("$chiefNavigatorTroyDPLimit")), "Departure rejection refreshes manifest tokens");
        f.limit(300);
        TroyWormholeInteraction outward = new TroyWormholeInteraction();
        outward.init(f.dialog);
        f.limit(239);
        f.clean();
        call(outward, "beginTransit");
        check(f.triggers.contains("ChiefNavigatorTroyTerminus_invalid"), "Slider reduction after opening outbound UI blocks the click");
        check("239".equals(f.local.values.get("$chiefNavigatorTroyDPLimit")), "Outbound rejection refreshes manifest tokens");
        f.limit(300);
        call(outward, "beginTransit");
        f.limit(239);
        f.clean();
        call(outward, "enterNebula");
        check(f.triggers.contains("ChiefNavigatorTroyTerminus_invalid") && f.systemLookups == 0,
                "Slider reduction during transition prose blocks final commit");
        f.limit(300);
        OdysseyExpanseInteraction inward = new OdysseyExpanseInteraction();
        inward.init(f.dialog);
        f.limit(239);
        int prior = f.transitions;
        call(inward, "returnToTroy");
        check(f.transitions == prior, "Slider reduction after opening return UI blocks transition");
        f.limit(240);
        call(inward, "returnToTroy");
        check(f.transitions == prior + 1, "Live increase permits return without campaign reload");
    }

    static void testDialogue(Fixture f, int value) throws Exception {
        if (f.lunaValues != null) f.limit(value);
        ChiefNavigatorSinniIntroCMD cmd = new ChiefNavigatorSinniIntroCMD();
        Map<String, MemoryAPI> scope = Map.of(MemKeys.LOCAL, f.local.api, MemKeys.GLOBAL, f.global.api);
        Map<String, Object> globalBefore = new HashMap<>(f.global.values);
        check(cmd.execute(null, f.dialog, List.of(new Token("routeInfo", TokenType.LITERAL)), scope), "Route-info command succeeds");
        check(Integer.toString(value).equals(f.local.values.get("$chiefNavigatorTroyDPLimit")), "Sinni prose uses live DP limit");
        boolean generous = cmd.execute(null, f.dialog, List.of(new Token("generousRoute", TokenType.LITERAL)), scope);
        check(generous == (value >= 240), "Optional dialogue appears exactly at >=240 DP");
        check(f.global.values.equals(globalBefore), "Presentation commands cannot advance or reward the quest");
    }

    static void testNavigation(Fixture f, int value) throws Exception {
        Map<String, Object> before = new HashMap<>(f.global.values);
        try {
            f.global.values.remove(OdysseyExpanseSystem.ENTERED);
            f.global.values.remove(TroyArrivalScript.WORMHOLE_OPENED);
            check(SinniNavigationGuidance.getCurrentGuidance().getObjective().contains(value + "-DP expedition limit"),
                    "Anchorage navigation advice uses the configured limit");
            f.global.values.put(TroyArrivalScript.WORMHOLE_OPENED, true);
            check(SinniNavigationGuidance.getCurrentGuidance().getObjective().contains(value + " deployment points"),
                    "Open-Terminus navigation advice uses the configured limit");
            f.global.values.remove(TroyArrivalScript.WORMHOLE_OPENED);
            f.global.values.put(TreadmillEntryScript.ARRIVAL_NOTICE_SHOWN, true);
            String status = (String) call(new WaypointTroyIntel(), "getStatus");
            check(status.contains(value + " DP maximum"), "Existing Troy Intel displays the live configured limit");
        } finally {
            f.global.values.clear();
            f.global.values.putAll(before);
        }
    }

    static void testAuthoredData() throws Exception {
        Method parse = Class.forName("com.fs.starfarer.loading.G").getMethod("o00000", String.class);
        JSONArray config = (JSONArray) parse.invoke(null, Files.readString(Path.of("data/config/LunaSettings.csv")));
        int settings = 0;
        for (int i = 0; i < config.length(); i++) {
            JSONObject field = config.getJSONObject(i);
            if (ChiefNavigatorSettings.TROY_FLEET_DP_LIMIT.equals(field.optString("fieldID"))) {
                settings++;
                check("Int".equals(field.getString("fieldType")), "Troy uses a native integer slider field");
                check(field.getInt("defaultValue") == 50 && field.getInt("minValue") == 1 && field.getInt("maxValue") == 1000,
                        "Native slider registration matches runtime bounds and default");
            }
        }
        check(settings == 1, "Register the Troy slider once");
        JSONArray rows = (JSONArray) parse.invoke(null, Files.readString(Path.of("data/campaign/rules.csv")));
        Map<String, JSONObject> rules = new HashMap<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            rules.put(row.getString("id"), row);
        }
        JSONObject dispatch = rules.get("chiefNavigatorSinniEventideRoute");
        check(dispatch.getString("script").indexOf("routeInfo") < dispatch.getString("script").indexOf("FireBest"),
                "Route limit is populated before native prose selection");
        check(rules.get("chiefNavigatorSinniEventideRouteText").getString("text").contains("$chiefNavigatorTroyDPLimit"),
                "Authored Sinni prose uses the live token");
        JSONObject normal = rules.get("chiefNavigatorSinniEventideRoutePreparationOption");
        check(normal.optString("conditions").isBlank() && normal.getString("options").contains("chief_navigator_sinni_eventide_preparation"),
                "Ordinary expedition-preparation choice remains unconditional");
        JSONObject optional = rules.get("chiefNavigatorSinniEventideRouteLittleLimitOption");
        check(optional.getString("conditions").trim().equals("ChiefNavigatorSinniIntroCMD generousRoute"),
                "Optional choice uses the tested live >=240 condition");
        check(optional.getString("options").contains("\"Doesn't seem like much of a limitation.\""),
                "Optional choice retains the requested wording");
        JSONObject reply = rules.get("chiefNavigatorSinniEventideLittleLimit");
        check(reply.getString("text").equals("\"Thank you for your praise.\" Sinni bows behind her sleeves."),
                "Optional response retains its requested wording");
        check(reply.optString("script").isBlank() && reply.getString("options").contains("chief_navigator_sinni_eventide_preparation"),
                "Optional response rejoins preparation with no progression side effect");
    }

    public static void main(String[] args) throws Exception {
        boolean luna = Arrays.asList(args).contains("--luna");
        testSettings(luna);
        Fixture fixture = new Fixture();
        if (luna) fixture.useLuna();
        for (int limit : luna ? new int[]{50, 239, 240, 300, 1000} : new int[]{50}) {
            testPortal(fixture, limit);
            testDialogue(fixture, limit);
            testNavigation(fixture, limit);
        }
        if (luna) testLiveRecheck(fixture);
        testAuthoredData();
        System.out.println("PASS: " + (luna ? "actual LunaLib live/clamped values" : "LunaLib absence/default")
                + "; portal boundaries/rechecks in both directions; dynamic manifest and Sinni >=240-DP dialogue condition");
    }
}
