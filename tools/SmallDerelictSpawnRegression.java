package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.lwjgl.util.vector.Vector2f;

/** Executes authored spawn paths and checks installed native hull definitions. */
public final class SmallDerelictSpawnRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
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
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            Object value = args == null || args.length == 0 ? null : values.get(args[0]);
            switch (name) {
                case "get": return value;
                case "getBoolean": return Boolean.TRUE.equals(value);
                case "getInt": return value instanceof Number ? ((Number) value).intValue() : 0;
                case "getString": return value;
                case "contains": return values.containsKey(args[0]);
                case "set": values.put((String) args[0], args[1]); break;
                case "unset": values.remove(args[0]); break;
            }
            return null;
        });
    }
    static Object field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }
    static Object invoke(String name, Class<?>[] signature, Object... args) throws Exception {
        Method method = MenelausTrial.class.getDeclaredMethod(name, signature);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    static final class Fixture {
        final Map<String, Object> global = new HashMap<>();
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final Map<CampaignFleetAPI, Fleet> fleetModels = new IdentityHashMap<>();
        final Map<FleetMemberAPI, String> variants = new IdentityHashMap<>();
        final List<Fleet> created = new ArrayList<>();
        int membersCreated;
        final SectorEntityToken center = mock(SectorEntityToken.class,
                (name, args) -> name.equals("getLocation") ? new Vector2f() : null);
        final StarSystemAPI system = mock(StarSystemAPI.class, (name, args) -> {
            switch (name) {
                case "getEntityById": return entities.get(args[0]);
                case "getCenter": return center;
                case "getFleets": return created.stream().map(f -> f.api).toList();
                case "addEntity":
                    SectorEntityToken added = (SectorEntityToken) args[0];
                    entities.put(added.getId(), added); break;
                case "removeEntity":
                    entities.remove(((SectorEntityToken) args[0]).getId()); break;
            }
            return null;
        });
        final SectorEntityToken wall = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return DriftingWallEncounter.FLEET_ID;
            if (name.equals("getCustomEntityType")) return "chief_navigator_drifting_wall_station";
            if (name.equals("getMemoryWithoutUpdate")) return memory(
                    Map.of(DriftingWallEncounter.MARKER, true));
            if (name.equals("getContainingLocation")) return system;
            if (name.equals("getLocation")) return new Vector2f();
            return null;
        });

        Fixture() {
            global.put(MenelausTrial.ACCEPTED, true);
            global.put("$chief_navigator_menelaus_labor_reports_migrated_v1", true);
            entities.put(DriftingWallEncounter.FLEET_ID, wall);
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory(global);
                if (name.equals("getStarSystems")) return List.of(system);
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createEmptyFleet")) {
                    Fleet fleet = new Fleet((String) args[0], (String) args[1]);
                    created.add(fleet);
                    fleetModels.put(fleet.api, fleet);
                    return fleet.api;
                }
                if (name.equals("createFleetMember")) return member((String) args[1]);
                return null;
            }));
        }

        FleetMemberAPI member(String variant) {
            RepairTrackerAPI repair = mock(RepairTrackerAPI.class,
                    (name, args) -> name.equals("getMaxCR") ? .7f : null);
            FleetMemberAPI member = mock(FleetMemberAPI.class, (name, args) -> {
                if (name.equals("getRepairTracker")) return repair;
                if (name.equals("setVariant")) {
                    throw new AssertionError("Population upkeep rewrote a saved variant");
                }
                return null;
            });
            variants.put(member, variant);
            membersCreated++;
            return member;
        }

        final class Fleet {
            String id, name, faction;
            boolean player, transitioning;
            final Map<String, Object> flags = new HashMap<>();
            final List<FleetMemberAPI> members = new ArrayList<>();
            final Vector2f position = new Vector2f();
            FleetAssignment assignment;
            SectorEntityToken target;
            final FleetDataAPI data = mock(FleetDataAPI.class, (method, args) -> {
                if (method.equals("getMembersListCopy")) return new ArrayList<>(members);
                if (method.equals("addFleetMember")) members.add((FleetMemberAPI) args[0]);
                if (method.equals("removeFleetMember")) members.remove(args[0]);
                return null;
            });
            final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (method, args) -> {
                switch (method) {
                    case "setId": id = (String) args[0]; break;
                    case "getId": return id;
                    case "setName": name = (String) args[0]; break;
                    case "getName": return name;
                    case "setFaction": faction = (String) args[0]; break;
                    case "getMemoryWithoutUpdate": return memory(flags);
                    case "getFleetData": return data;
                    case "isEmpty": return members.isEmpty();
                    case "isPlayerFleet": return player;
                    case "isInHyperspaceTransition": return transitioning;
                    case "getContainingLocation": return system;
                    case "getLocation": return position;
                    case "setLocation": position.set((float) args[0], (float) args[1]); break;
                    case "clearAssignments": assignment = null; target = null; break;
                    case "addAssignment":
                        assignment = (FleetAssignment) args[0];
                        target = (SectorEntityToken) args[1]; break;
                    case "getCurrentAssignment":
                        if (assignment == null) return null;
                        return mock(FleetAssignmentDataAPI.class, (n, a) ->
                                n.equals("getAssignment") ? assignment
                                : n.equals("getTarget") ? target : null);
                }
                return null;
            });
            Fleet(String faction, String name) { this.faction = faction; this.name = name; }
            List<String> loadout() { return members.stream().map(variants::get).toList(); }
        }
    }

    static final Map<String, Path> STOCK_VARIANTS = new HashMap<>();
    static String nativeSize(String variantId) throws Exception {
        Path path = STOCK_VARIANTS.get(variantId);
        check(path != null, "New ship is not an installed stock variant: " + variantId);
        JSONObject variant = new JSONObject(Files.readString(path));
        String hullId = variant.getString("hullId");
        check(!hullId.startsWith("chief_navigator_"), "New ship uses a Guard compatibility hull");
        return new JSONObject(Files.readString(Path.of(
                "../../starsector-core/data/hulls", hullId + ".ship"))).getString("hullSize");
    }
    static void small(List<String> variants, int min, int max) throws Exception {
        check(variants.size() >= min && variants.size() <= max,
                "Unexpected small-patrol size: " + variants);
        int destroyers = 0;
        for (String variant : variants) {
            String size = nativeSize(variant);
            check(size.equals("FRIGATE") || size.equals("DESTROYER"),
                    "Patrol includes a native cruiser or capital: " + variant);
            if (size.equals("DESTROYER")) destroyers++;
        }
        check(destroyers <= 1, "Small patrol includes multiple destroyers");
    }

    static void checkRostersAndAssets() throws Exception {
        try (var paths = Files.walk(Path.of("../../starsector-core/data/variants"))) {
            paths.filter(path -> path.toString().endsWith(".variant"))
                    .forEach(path -> STOCK_VARIANTS.put(path.getFileName().toString()
                            .replace(".variant", ""), path));
        }
        for (String[] roster : (String[][]) field(MenelausTrial.class, "ROAMER_LOADOUTS")) {
            small(List.of(roster), 3, 3);
        }
        for (String[] roster : (String[][]) field(OdysseyPredatorScript.class,
                "ROGUE_DERELICT_LOADOUTS")) small(List.of(roster), 2, 4);
        String[] friendly = (String[]) field(MenelausTrial.class, "FRIENDLY_VARIANTS");
        for (int i = 0; i < 8; i++) small(List.of(friendly[i % friendly.length],
                friendly[(i + 1) % friendly.length], friendly[(i + 3) % friendly.length]), 3, 3);
        for (Method method : MenelausTrial.class.getDeclaredMethods()) {
            check(!method.getName().equals("convertFleetToCombatGuard"),
                    "Population conversion to Guard variants must remain retired");
        }
        for (String source : List.of("MenelausTrial", "OdysseyPredatorScript",
                "IthacaSectionEncounter", "DriftingWallLaborSupport")) {
            check(!Files.readString(Path.of("src/chiefnavigator/quest", source + ".java"))
                            .contains("chief_navigator_combat_guard_"),
                    "A Guard creation reference remains in " + source);
        }
        for (String id : List.of("bastillon_Standard", "berserker_Assault", "defender_PD",
                "picket_Assault", "rampart_Standard", "sentry_FS", "warden_Defense",
                "guardian_Standard")) {
            String oldId = "chief_navigator_combat_guard_" + id;
            JSONObject variant = new JSONObject(Files.readString(Path.of(
                    "data/variants", oldId + ".variant")));
            String hull = variant.getString("hullId");
            JSONObject skin = new JSONObject(Files.readString(Path.of(
                    "data/hulls/skins", hull + ".skin")));
            check(Files.isRegularFile(Path.of(skin.getString("spriteName"))),
                    "Saved Guard sprite must remain present: " + oldId);
            check(variant.getString("variantId").equals(oldId),
                    "Saved Guard variant identity changed: " + oldId);
        }
    }

    static void checkActualSpawns() throws Exception {
        Fixture f = new Fixture();
        for (int i = 0; i < 10; i++) invoke("ensureRoamingDerelicts",
                new Class<?>[] {StarSystemAPI.class, SectorEntityToken.class, int.class},
                f.system, f.wall, i);
        check(f.created.size() == 10, "Ten existing hostile population slots remain");
        for (Fixture.Fleet fleet : f.created) {
            small(fleet.loadout(), 3, 3);
            check(fleet.faction.equals(Factions.DERELICT), "Hostile packs use stock Derelict faction");
        }
        f.entities.remove("chief_navigator_wall_derelict_roamer_0");
        invoke("ensureRoamingDerelicts", new Class<?>[] {
                StarSystemAPI.class, SectorEntityToken.class, int.class}, f.system, f.wall, 0);
        check(f.created.size() == 10, "Lost hostile slot must not respawn");

        f = new Fixture();
        invoke("ensureFriendlyArmy", new Class<?>[] {StarSystemAPI.class, int.class}, f.system, 8);
        check(f.created.size() == 8, "Eight finite friendly population slots remain");
        for (Fixture.Fleet fleet : f.created) small(fleet.loadout(), 3, 3);
        Fixture.Fleet survivor = f.created.get(0);
        survivor.members.subList(1, survivor.members.size()).clear();
        survivor.name = "Serialized Guard Patrol";
        String savedFaction = survivor.faction;
        FleetMemberAPI saved = survivor.members.get(0);
        f.entities.remove("chief_navigator_unsealed_guard_1");
        int count = f.membersCreated;
        invoke("ensureFriendlyArmy", new Class<?>[] {StarSystemAPI.class, int.class}, f.system, 8);
        check(f.created.size() == 8 && f.membersCreated == count
                        && survivor.members.equals(List.of(saved))
                        && survivor.name.equals("Serialized Guard Patrol")
                        && survivor.faction.equals(savedFaction),
                "Friendly refresh must retain losses, names, factions and member identities");

        f = new Fixture();
        for (int index = 0; index < 4; index++) {
            String id = "chief_navigator_sealed_mothership_" + index;
            final Fixture fixture = f;
            SectorEntityToken poi = mock(SectorEntityToken.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getMemoryWithoutUpdate")) return memory(
                        Map.of(MenelausTrial.MOTHERSHIP_MARKER, true));
                if (name.equals("getContainingLocation")) return fixture.system;
                if (name.equals("getLocation")) return new Vector2f();
                return null;
            });
            CampaignFleetAPI proxy = MenelausTrial.createMothershipCombatProxy(poi);
            check(proxy != null, "Active mothership path must create its encounter");
            List<String> roster = f.fleetModels.get(proxy).loadout();
            check(roster.size() == 4 && roster.get(0)
                            .equals("station_derelict_survey_mothership_Standard"),
                    "Mothership proxy contains only required station and three escorts");
            for (String escort : roster.subList(1, roster.size())) {
                check(nativeSize(escort).equals("FRIGATE"), "Mothership escort is a stock frigate");
            }
        }
    }

    static void checkAshenEscalation() throws Exception {
        Fixture f = new Fixture();
        Class<?>[] signature = {StarSystemAPI.class, int.class, boolean.class};
        invoke("ensureAshenRoamer", signature, f.system, 0, false);
        check(f.created.size() == 1, "Initial Ashen patrol is created once");
        Fixture.Fleet fleet = f.created.get(0);
        small(fleet.loadout(), 3, 3);
        fleet.members.clear();
        FleetMemberAPI saved = f.member("chief_navigator_combat_guard_rampart_Standard");
        fleet.members.add(saved);
        fleet.name = "Saved Ascendant Combat Guard Armada";
        fleet.faction = "saved_guard_faction";
        int count = f.membersCreated;
        for (int i = 0; i < 3; i++) invoke("ensureAshenRoamer", signature, f.system, 0, true);
        check(f.created.size() == 1 && f.membersCreated == count
                        && fleet.members.equals(List.of(saved))
                        && fleet.name.equals("Saved Ascendant Combat Guard Armada")
                        && fleet.faction.equals("saved_guard_faction"),
                "Historical escalated call must not refill, convert, rename or refaction saved fleets");
        f.entities.remove(fleet.id);
        invoke("ensureAshenRoamer", signature, f.system, 0, true);
        invoke("ensureAshenRoamer", signature, f.system, 0, false);
        check(f.created.size() == 1 && f.membersCreated == count,
                "An Ashen slot lost before or after escalation remains gone");
        f.global.put("$chief_navigator_ashen_trial_roamer_spawned_1", true);
        invoke("ensureAshenRoamer", signature, f.system, 1, true);
        check(f.created.size() == 1, "Escalation cannot recreate a previously lost ordinary slot");
    }

    public static void main(String[] args) throws Exception {
        checkRostersAndAssets();
        checkActualSpawns();
        checkAshenEscalation();
        System.out.println("PASS: actual small stock Derelict spawns, native hull sizes, finite losses, saved Guard roster preservation and all eight legacy asset sets.");
    }
}
