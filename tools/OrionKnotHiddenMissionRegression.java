import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.PlanetSpecAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.missions.cb.CBDerelict;
import com.fs.starfarer.api.impl.campaign.missions.cb.CustomBountyCreator;
import com.fs.starfarer.api.impl.campaign.missions.cb.MilitaryCustomBounty;
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent;
import java.awt.Color;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/** Native mission selection and narrowly owned THEME_HIDDEN tag lifecycle checks. */
public final class OrionKnotHiddenMissionRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("toString")) return type.getSimpleName();
                    Object result = call.invoke(name, args);
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

    static String key(String name) throws Exception {
        Field field = OdysseyExpanseSystem.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    static final String[] IDS = {OdysseyExpanseSystem.SYSTEM_ID,
        OdysseyExpanseSystem.MESSINA_ID, OdysseyExpanseSystem.SILENT_WAKE_ID,
        OdysseyExpanseSystem.ASHEN_VERGE_ID, OdysseyExpanseSystem.LAST_LIGHT_ID,
        OdysseyExpanseSystem.DEVOURED_REACH_ID};
    static final int SEEDS = 1024;

    static final class SystemFixture {
        final String id;
        final Map<String, Object> flags = new HashMap<>();
        final Set<String> tags = new LinkedHashSet<>();
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final Vector2f position = new Vector2f(150000f, -94000f);
        boolean nebula;
        boolean pulsar;
        PlanetAPI star;
        final MemoryAPI memory = mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(flags.get(args[0]));
            if (name.equals("contains")) return flags.containsKey(args[0]);
            if (name.equals("get") || name.equals("getString")) return flags.get(args[0]);
            throw new AssertionError("Tag reconciliation must preserve saved memory: " + name);
        });
        final StarSystemAPI api;

        SystemFixture(String id) {
            this.id = id;
            tags.add("another_mod_tag");
            tags.addAll(Arrays.asList(Tags.THEME_SPECIAL, Tags.THEME_UNSAFE,
                    Tags.NOT_RANDOM_MISSION_TARGET, Tags.NO_TOPOGRAPHY_SCANS,
                    Tags.DO_NOT_SHOW_STRANDED_DIALOG));
            api = mock(StarSystemAPI.class, (name, args) -> {
                switch (name) {
                    case "getId":
                    case "getOptionalUniqueId":
                    case "getName": return id;
                    case "getTags": return tags;
                    case "hasTag": return tags.contains(args[0]);
                    case "addTag": tags.add((String) args[0]); return null;
                    case "removeTag": tags.remove(args[0]); return null;
                    case "getMemoryWithoutUpdate": return memory;
                    case "getLocation": return position;
                    case "getStar": return star;
                    case "isNebula": return nebula;
                    case "hasPulsar": return pulsar;
                    case "getEntityById": return entities.get(args[0]);
                    default:
                        throw new AssertionError("Unexpected topology mutation/access: " + name);
                }
            });
        }

        void blackHole() {
            PlanetSpecAPI spec = mock(PlanetSpecAPI.class, (name, args) -> {
                if (name.equals("isBlackHole")) return true;
                throw new AssertionError("Unexpected star-spec access: " + name);
            });
            star = mock(PlanetAPI.class, (name, args) -> {
                if (name.equals("getSpec")) return spec;
                throw new AssertionError("Unexpected star access: " + name);
            });
        }
    }

    static List<SystemFixture> ownedSystems(boolean legacy) throws Exception {
        List<SystemFixture> result = new ArrayList<>();
        String[] legacyFlags = {"GENERATED_NEBULA", "GENERATED_MESSINA",
                "GENERATED_SILENT_WAKE", "GENERATED_ASHEN_VERGE", "GENERATED_WHITE_DWARF"};
        for (int index = 0; index < IDS.length; index++) {
            SystemFixture system = new SystemFixture(IDS[index]);
            if (!legacy) system.flags.put(key("AUTHORED_SYSTEM_MARKER"), true);
            else if (index < legacyFlags.length) system.flags.put(key(legacyFlags[index]), true);
            else system.entities.put(OdysseyExpanseSystem.DEVOURED_RING_ID,
                    mock(SectorEntityToken.class, (name, args) -> {
                        if (name.equals("getId") || name.equals("getCustomEntityType")) {
                            return OdysseyExpanseSystem.DEVOURED_RING_ID;
                        }
                        throw new AssertionError("Unexpected Devoured Ring access: " + name);
                    }));
            if (index % 2 == 0) system.nebula = true;
            else system.blackHole();
            check(OdysseyExpanseSystem.isInside(system.api), "Fixture must prove owned identity");
            result.add(system);
        }
        return result;
    }

    static void world(List<SystemFixture> systems) {
        List<StarSystemAPI> actual = new ArrayList<>();
        for (SystemFixture fixture : systems) actual.add(fixture.api);
        Global.setSector(mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getStarSystems")) return actual;
            throw new AssertionError("Reconciliation must not create or replace world state: " + name);
        }));
    }

    static void reconcile() throws Exception {
        OdysseyExpanseSystem.class.getMethod("reconcileRandomMissionTags").invoke(null);
    }

    static void checkGenerationHelper() throws Exception {
        Method apply = OdysseyExpanseSystem.class.getDeclaredMethod("applyClusterTags", StarSystemAPI.class);
        apply.setAccessible(true);
        for (boolean legacy : new boolean[] {false, true}) {
            for (SystemFixture system : ownedSystems(legacy)) {
                system.tags.addAll(Arrays.asList(Tags.SYSTEM_ABYSSAL,
                        Tags.SYSTEM_CUT_OFF_FROM_HYPER, Tags.STAR_HIDDEN_ON_MAP));
                Map<String, Object> flags = new HashMap<>(system.flags);
                Map<String, SectorEntityToken> entities = new HashMap<>(system.entities);
                apply.invoke(null, system.api);
                check(system.tags.contains(Tags.THEME_HIDDEN)
                                && system.tags.contains("another_mod_tag"),
                        "Actual generation helper must apply native mission hiding without removing other tags");
                check(!system.tags.contains(Tags.STAR_HIDDEN_ON_MAP)
                                && !system.tags.contains(Tags.SYSTEM_CUT_OFF_FROM_HYPER),
                        "Mission hiding must preserve authored map and navigation visibility policy");
                Set<String> first = new LinkedHashSet<>(system.tags);
                apply.invoke(null, system.api);
                check(first.equals(system.tags) && flags.equals(system.flags)
                                && entities.equals(system.entities)
                                && system.position.equals(new Vector2f(150000f, -94000f)),
                        "Repeated generation tagging must preserve topology, memory, position and IDs");
            }
        }
    }

    static void checkReconciliation(List<SystemFixture> owned,
            List<SystemFixture> outside) throws Exception {
        List<SystemFixture> all = new ArrayList<>(owned);
        all.addAll(outside);
        world(all);
        List<Set<String>> expected = new ArrayList<>();
        for (SystemFixture system : all) {
            Set<String> tags = new LinkedHashSet<>(system.tags);
            if (owned.contains(system)) tags.add(Tags.THEME_HIDDEN);
            expected.add(tags);
        }
        List<CustomBountyCreator> registry = MilitaryCustomBounty.CREATORS;
        List<CustomBountyCreator> creators = new ArrayList<>(registry);
        reconcile();
        reconcile();
        for (int index = 0; index < all.size(); index++) {
            check(expected.get(index).equals(all.get(index).tags),
                    "Reconciliation must add only THEME_HIDDEN, exclusively to owned systems");
        }
        check(MilitaryCustomBounty.CREATORS == registry && registry.equals(creators),
                "System tagging must not replace or filter native bounty registry entries");
    }

    static final class Selected extends RuntimeException { }

    /** Completes native weighted system selection before stopping native fleet creation. */
    static final class Mission extends HubMissionWithBarEvent {
        final Selected stop = new Selected();
        StarSystemAPI selected;
        int picks;

        @Override protected boolean create(MarketAPI market, boolean barEvent) {
            throw new AssertionError("Harness must not generate a real mission");
        }

        @Override public StarSystemAPI pickSystem() {
            var previous = getSearch();
            selected = super.pickSystem();
            check(previous != getSearch() && getSearch().systemReqs.isEmpty()
                            && getSearch().systemPrefs.isEmpty(),
                    "Native search state must reset independently for each offer");
            picks++;
            throw stop;
        }

        StarSystemAPI pick(CBDerelict creator, long seed) {
            selected = null;
            setGenRandom(new Random(seed));
            try {
                creator.createBounty(null, this, 5, new Object());
                throw new AssertionError("Native creator must reach real system selection");
            } catch (Selected finished) {
                check(finished == stop, "Unexpected selection interruption");
            }
            return selected;
        }
    }

    static int checkNativeSelection(boolean legacy, boolean fallback) throws Exception {
        List<SystemFixture> owned = ownedSystems(legacy);
        List<SystemFixture> outside = new ArrayList<>();
        SystemFixture plain = new SystemFixture("ordinary_inner_fallback");
        plain.position.set(1000f, 1000f);
        outside.add(plain);
        Set<StarSystemAPI> preferred = new LinkedHashSet<>();
        if (!fallback) {
            SystemFixture blackHole = new SystemFixture("ordinary_fringe_black_hole");
            blackHole.blackHole();
            SystemFixture nebula = new SystemFixture("ordinary_fringe_nebula");
            nebula.nebula = true;
            outside.addAll(Arrays.asList(blackHole, nebula));
            preferred.add(blackHole.api);
            preferred.add(nebula.api);
        }
        // Same-ID claims and unrelated marked worlds are not owned by this mod.
        for (String id : IDS) {
            SystemFixture foreign = new SystemFixture(id);
            foreign.position.set(2000f, 2000f);
            outside.add(foreign);
        }
        SystemFixture markedForeign = new SystemFixture("another_mod_world");
        markedForeign.position.set(3000f, 3000f);
        markedForeign.flags.put(key("AUTHORED_SYSTEM_MARKER"), true);
        outside.add(markedForeign);
        List<SystemFixture> all = new ArrayList<>(owned);
        all.addAll(outside);
        world(all);
        CBDerelict nativeCreator = null;
        for (CustomBountyCreator creator : new MilitaryCustomBounty().getCreators()) {
            if (creator != null && creator.getClass() == CBDerelict.class) {
                nativeCreator = (CBDerelict) creator;
                break;
            }
        }
        check(nativeCreator != null, "Military registry must retain the original native Derelict generator");
        Mission mission = new Mission();
        int baselineOrion = 0;
        Set<String> baselineIds = new LinkedHashSet<>();
        for (int index = 0; index < SEEDS; index++) {
            StarSystemAPI selected = mission.pick(nativeCreator, (index + 1L) * 0x9e3779b97f4a7c15L);
            if (OdysseyExpanseSystem.isInside(selected)) {
                baselineOrion++;
                baselineIds.add(selected.getOptionalUniqueId());
            }
        }
        check(baselineIds.size() == IDS.length,
                "Before tagging, the real native picker must demonstrably select all six Orion systems");
        checkReconciliation(owned, outside);
        Set<StarSystemAPI> ordinaryPicked = new LinkedHashSet<>();
        for (int index = 0; index < SEEDS; index++) {
            StarSystemAPI selected = mission.pick(nativeCreator, (index + 1L) * 0x9e3779b97f4a7c15L);
            check(selected != null && !OdysseyExpanseSystem.isInside(selected),
                    "Native mission search must never select a tagged Orion system");
            if (!fallback) check(preferred.contains(selected), "Native ordinary BH/nebula preferences must remain");
            ordinaryPicked.add(selected);
        }
        if (!fallback) check(ordinaryPicked.equals(preferred), "Both ordinary preferred targets must be selected");
        else check(ordinaryPicked.size() == outside.size(),
                "Every ordinary and foreign same-ID target must remain available through native fallback");
        check(mission.picks == SEEDS * 2, "Each baseline and tagged selection must complete natively");
        world(owned);
        check(mission.pick(nativeCreator, 1L) == null,
                "An all-hidden candidate pool must stay empty through native preference fallback");
        return baselineOrion;
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI priorSettings = Global.getSettings();
        SectorAPI priorSector = Global.getSector();
        FactoryAPI priorFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> {
                if (name.equals("getFloat")) return "sectorHeight".equals(values[0]) ? 104000f : 2000f;
                if (name.equals("getColor")) return Color.WHITE;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                if (name.equals("createCargo")) return mock(CargoAPI.class, (cargoCall, cargoArgs) -> {
                    throw new AssertionError("Selection must not use cargo: " + cargoCall);
                });
                throw new AssertionError("Selection must not create campaign content: " + name);
            }));
            checkGenerationHelper();
            int baseline = 0;
            for (boolean legacy : new boolean[] {false, true}) {
                for (boolean fallback : new boolean[] {false, true}) {
                    baseline += checkNativeSelection(legacy, fallback);
                }
            }
            Global.setSector(null);
            reconcile();
            String plugin = Files.readString(Path.of("src/chiefnavigator/ChiefNavigatorModPlugin.java"));
            check(plugin.contains("OdysseyExpanseSystem.reconcileRandomMissionTags();")
                            && !plugin.contains("OrionKnotDerelictBounty.install("),
                    "Load hook must reconcile native tags rather than replace bounty creators");
            System.out.println("PASS: actual shared generation tags and idempotent owned-only reconciliation; "
                    + SEEDS + " seeds x four current/legacy preferred/fallback scenarios: native baseline Orion "
                    + baseline + "/" + (SEEDS * 4) + ", native tagged Orion 0/" + (SEEDS * 4)
                    + "; all-hidden pools empty, outside systems and native registry preserved.");
        } finally {
            Global.setSettings(priorSettings);
            Global.setSector(priorSector);
            Global.setFactory(priorFactory);
        }
    }
}
