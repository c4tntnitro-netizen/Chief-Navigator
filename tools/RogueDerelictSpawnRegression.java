package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.RoleEntryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.lwjgl.util.vector.Vector2f;

/** Exercises new stock rogue spawns and preservation of serialized Guard fleets. */
public final class RogueDerelictSpawnRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            Object result = call.invoke(method.getName(), args);
            if (result != null) return result;
            Class<?> returns = method.getReturnType();
            if (returns == boolean.class) return false;
            if (returns == int.class) return 0;
            if (returns == float.class) return 0f;
            if (returns == long.class) return 0L;
            return null;
        }));
    }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    static Set<String> keys(JSONObject object) {
        String[] names = JSONObject.getNames(object);
        return names == null ? Set.of() : new HashSet<>(Arrays.asList(names));
    }

    static final class Memory {
        final Map<String, Object> values = new HashMap<>();
        final MemoryAPI api = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static final class Fleet {
        final Memory memory = new Memory();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final Vector2f location = new Vector2f();
        String id, faction, name;
        boolean frozenIdentity;
        FleetAssignmentDataAPI assignment;
        final FleetDataAPI data = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (name.equals("addFleetMember")) { check(!frozenIdentity, "Maintenance must not add ships"); members.add((FleetMemberAPI) args[0]); }
            if (name.equals("removeFleetMember")) throw new AssertionError("Maintenance must not remove ships");
            return null;
        });
        final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (method, args) -> {
            if (method.equals("getId")) return id;
            if (method.equals("setId")) id = (String) args[0];
            if (method.equals("setName")) { check(!frozenIdentity, "Maintenance must retain a saved fleet name"); name = (String) args[0]; }
            if (method.equals("setFaction")) throw new AssertionError("Maintenance must retain the saved fleet faction");
            if (method.equals("getFleetData")) return data;
            if (method.equals("getMemoryWithoutUpdate")) return memory.api;
            if (method.equals("getLocation")) return location;
            if (method.equals("setLocation")) location.set((Float) args[0], (Float) args[1]);
            if (method.equals("getCurrentAssignment")) return assignment;
            if (method.equals("clearAssignments")) assignment = null;
            if (method.equals("addAssignment")) {
                FleetAssignment kind = (FleetAssignment) args[0];
                SectorEntityToken target = (SectorEntityToken) args[1];
                assignment = mock(FleetAssignmentDataAPI.class, (n, a) -> n.equals("getAssignment") ? kind : n.equals("getTarget") ? target : null);
            }
            return null;
        });
    }

    static final class Fixture {
        final Fleet fleet = new Fleet();
        final Map<String, SectorEntityToken> tokens = new HashMap<>();
        final List<String> variants = new ArrayList<>();
        int adds, created;
        final StarSystemAPI system = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getJumpPoints")) return List.of();
            if (name.equals("getEntityById")) return tokens.get(args[0]);
            if (name.equals("addEntity")) { check(args[0] == fleet.api, "Spawn must add its exact fleet"); adds++; }
            if (name.equals("createToken")) {
                Memory memory = new Memory();
                Vector2f position = new Vector2f();
                SectorEntityToken[] token = new SectorEntityToken[1];
                token[0] = mock(SectorEntityToken.class, (n, a) -> {
                    if (n.equals("setId")) tokens.put((String) a[0], token[0]);
                    if (n.equals("getMemoryWithoutUpdate")) return memory.api;
                    if (n.equals("getLocation")) return position;
                    if (n.equals("setLocation")) position.set((Float) a[0], (Float) a[1]);
                    return null;
                });
                return token[0];
            }
            return null;
        });

        Fixture() {
            Global.setSector(mock(SectorAPI.class, (name, args) -> name.equals("getStarSystems") ? List.of() : null));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createEmptyFleet")) {
                    created++; fleet.faction = (String) args[0]; fleet.name = (String) args[1]; return fleet.api;
                }
                if (name.equals("createFleetMember")) {
                    String variantId = (String) args[1]; variants.add(variantId);
                    check(!variantId.startsWith("chief_navigator_"), "New rogue fleet must use only stock Derelict variants");
                    String hull = variantId.substring(0, variantId.indexOf('_'));
                    ShipVariantAPI variant = mock(ShipVariantAPI.class, (n, a) -> n.equals("getHullVariantId") ? variantId : null);
                    RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (n, a) -> n.equals("getMaxCR") ? 0.7f : null);
                    return mock(FleetMemberAPI.class, (n, a) -> {
                        if (n.equals("getHullId")) return hull;
                        if (n.equals("getVariant")) return variant;
                        if (n.equals("getRepairTracker")) return repair;
                        if (n.equals("setVariant")) throw new AssertionError("Stock or saved members must not convert into Guard skins");
                        return null;
                    });
                }
                throw new AssertionError("Unexpected factory request: " + name);
            }));
        }
    }

    static void invoke(String method, Class<?>[] types, Object... arguments) throws Exception {
        Method target = OdysseyPredatorScript.class.getDeclaredMethod(method, types);
        target.setAccessible(true);
        target.invoke(null, arguments);
    }

    static void newStockSpawns() throws Exception {
        Path modRoot = (Files.isDirectory(Path.of("data/hulls")) ? Path.of("") : Path.of("Chief Navigator")).toAbsolutePath().normalize();
        Path core = modRoot.getParent().getParent().resolve("starsector-core");
        List<List<String>> rosters = List.of(
                List.of("berserker_Assault", "defender_PD", "picket_Assault"),
                List.of("bastillon_Standard", "sentry_FS", "warden_Defense"),
                List.of("warden_Defense", "defender_PD", "sentry_FS", "picket_Assault"));
        for (int index = 0; index < 8; index++) {
            Fixture f = new Fixture();
            invoke("spawnRogueDerelictFleet", new Class<?>[] {StarSystemAPI.class, int.class}, f.system, index);
            check(f.created == 1 && f.adds == 1 && Factions.DERELICT.equals(f.fleet.faction)
                            && "Derelict Drone Flotilla".equals(f.fleet.name),
                    "New rogue fleets must be ordinary named Derelicts");
            check(f.variants.equals(rosters.get(index % rosters.size())) && f.fleet.members.size() >= 3 && f.fleet.members.size() <= 4,
                    "Each new rogue flotilla must have its small authored stock roster");
            for (String id : f.variants) {
                JSONObject variant = new JSONObject(Files.readString(core.resolve("data/variants/" + id + ".variant")));
                check(id.equals(variant.getString("variantId")) && !variant.getString("hullId").startsWith("chief_navigator_"),
                        "Every selected stock variant must resolve in the installed game");
            }
            check(!Boolean.TRUE.equals(f.fleet.memory.values.get(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY))
                            && Boolean.TRUE.equals(f.fleet.memory.values.get(MemFlags.MEMORY_KEY_NO_JUMP)),
                    "New fleets must retain ordinary recovery and authored no-jump behavior");
            List<FleetMemberAPI> savedMembers = List.copyOf(f.fleet.members);
            f.fleet.frozenIdentity = true;
            invoke("configureRogueDerelictFleet", new Class<?>[] {CampaignFleetAPI.class, StarSystemAPI.class}, f.fleet.api, f.system);
            invoke("configureRogueDerelictFleet", new Class<?>[] {CampaignFleetAPI.class, StarSystemAPI.class}, f.fleet.api, f.system);
            check(f.fleet.members.equals(savedMembers), "Repeated maintenance must preserve each exact member");
        }
    }

    static void savedGuardIdentity() throws Exception {
        Fixture f = new Fixture();
        f.fleet.id = "chief_navigator_odyssey_rogue_derelict_0";
        f.fleet.name = "Rogue Combat Guard Flotilla";
        f.fleet.faction = "chief_navigator_domain_combat_guard";
        FleetMemberAPI saved = mock(FleetMemberAPI.class, (name, args) -> {
            throw new AssertionError("Maintenance must not inspect or rewrite saved Guard ships: " + name);
        });
        f.fleet.members.add(saved); f.fleet.frozenIdentity = true;
        invoke("configureRogueDerelictFleet", new Class<?>[] {CampaignFleetAPI.class, StarSystemAPI.class}, f.fleet.api, f.system);
        check(f.fleet.members.equals(List.of(saved)) && "Rogue Combat Guard Flotilla".equals(f.fleet.name)
                        && "chief_navigator_domain_combat_guard".equals(f.fleet.faction) && f.created == 0,
                "Existing serialized Guard fleets must retain their exact roster, name and faction");
    }

    /** Native relaxed JSON and role parsers validate every active faction pick. */
    @SuppressWarnings("unchecked")
    static void factionPools() throws Exception {
        Path modRoot = (Files.isDirectory(Path.of("data/hulls")) ? Path.of("") : Path.of("Chief Navigator")).toAbsolutePath().normalize();
        Path core = modRoot.getParent().getParent().resolve("starsector-core");
        String source = Files.readString(modRoot.resolve("data/world/factions/domain_combat_guard.faction"));
        JSONObject faction = null;
        for (Method method : Class.forName("com.fs.starfarer.loading.LoadingUtils").getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) && method.getReturnType() == JSONObject.class
                    && !method.getName().equals("String")
                    && Arrays.equals(method.getParameterTypes(), new Class<?>[] {String.class, String.class})) {
                faction = (JSONObject) method.invoke(null, "domain_combat_guard.faction", source);
            }
        }
        check(faction != null && "chief_navigator_domain_combat_guard".equals(faction.getString("id")),
                "Native faction parser must preserve the historical faction ID");
        Set<String> hulls = Set.of("warden", "defender", "picket", "sentry", "bastillon", "berserker");
        check(keys(faction.getJSONObject("hullFrequency").getJSONObject("hulls")).equals(hulls),
                "Faction hull frequencies must contain only the six small stock Derelicts");
        for (String pool : List.of("knownShips", "priorityShips")) {
            JSONObject choices = faction.getJSONObject(pool);
            Set<String> actual = new HashSet<>();
            JSONArray known = choices.getJSONArray("hulls");
            for (int index = 0; index < known.length(); index++) actual.add(known.getString(index));
            check(actual.equals(hulls) && choices.getJSONArray("tags").length() == 0,
                    "Faction knowledge/priorities must not admit Guard skins or larger hulls through tags: " + pool);
        }
        check(faction.getJSONObject("factionDoctrine").getInt("shipSize") == 2,
                "Faction doctrine must favor small ships");
        JSONObject roleDefinitions = faction.getJSONObject("shipRoles");
        check(keys(roleDefinitions).equals(Set.of("combatSmall", "combatMedium")),
                "Only frigate/destroyer faction combat roles may be active");
        Class<?> hullType = Class.forName("com.fs.starfarer.loading.specs.g");
        Class<?> variantType = Class.forName("com.fs.starfarer.loading.specs.HullVariantSpec");
        Class<?> registry = Class.forName("com.fs.starfarer.loading.B");
        Map<Field, Map<Object, Object>> snapshots = new HashMap<>();
        for (Field field : registry.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                snapshots.put(field, new HashMap<>((Map<Object, Object>) field.get(null)));
            }
        }
        SettingsAPI previous = Global.getSettings();
        try {
            Map<String, ShipVariantAPI> variants = new HashMap<>();
            for (String role : keys(roleDefinitions)) {
                for (String id : keys(roleDefinitions.getJSONObject(role))) {
                    JSONObject definition = new JSONObject(Files.readString(core.resolve("data/variants/" + id + ".variant")));
                    String hullId = definition.getString("hullId");
                    check(hulls.contains(hullId), "Faction role cannot select a Guard, cruiser or capital: " + id);
                    JSONObject ship = new JSONObject(Files.readString(core.resolve("data/hulls/" + hullId + ".ship")));
                    HullSize size = HullSize.valueOf(ship.getString("hullSize"));
                    check(size == (role.equals("combatSmall") ? HullSize.FRIGATE : HullSize.DESTROYER),
                            "Stock variant must fit its native role size: " + id);
                    Object hull = hullType.getConstructor(String.class).newInstance(hullId);
                    hullType.getMethod("setFleetPoints", int.class).invoke(hull, size == HullSize.FRIGATE ? 3 : 6);
                    hullType.getMethod("setHullSize", HullSize.class).invoke(hull, size);
                    Object variant = variantType.getConstructor(String.class, hullType).newInstance(id, hull);
                    registry.getMethod("o00000", variantType).invoke(null, variant);
                    variants.put(id, (ShipVariantAPI) variant);
                }
            }
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> name.equals("getVariant") ? variants.get(args[0]) : null));
            Class<?> rolesType = Class.forName("com.fs.starfarer.loading.O0Oo");
            Object roles = rolesType.getConstructor(JSONObject.class).newInstance(roleDefinitions);
            Map<String, Object> parsed = (Map<String, Object>) rolesType.getMethod("new").invoke(roles);
            check(parsed.keySet().equals(Set.of("combatSmall", "combatMedium")), "Native faction role parser must retain exactly the small roles");
            for (String role : parsed.keySet()) {
                List<RoleEntryAPI> entries = (List<RoleEntryAPI>) parsed.get(role).getClass().getMethod("o00000").invoke(parsed.get(role));
                check(entries.size() == (role.equals("combatSmall") ? 4 : 2), "Native faction pool has an unexpected fallback or inherited entry");
                for (RoleEntryAPI entry : entries) check(variants.containsKey(entry.getVariantId()) && entry.getWeight() == 10f,
                        "Every native faction pick must resolve to the intended small stock variant");
            }
        } finally {
            Global.setSettings(previous);
            for (Map.Entry<Field, Map<Object, Object>> snapshot : snapshots.entrySet()) {
                Map<Object, Object> entries = (Map<Object, Object>) snapshot.getKey().get(null);
                entries.clear(); entries.putAll(snapshot.getValue());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        SectorAPI sector = Global.getSector(); SettingsAPI settings = Global.getSettings(); FactoryAPI factory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> null));
            newStockSpawns(); savedGuardIdentity(); factionPools();
            System.out.println("PASS: new rogue fleets and native faction role pools use only stock frigates/destroyers; ordinary recovery/no-jump safety and saved Guard fleet names, factions and exact members are preserved.");
        } finally { Global.setSector(sector); Global.setSettings(settings); Global.setFactory(factory); }
    }
}
