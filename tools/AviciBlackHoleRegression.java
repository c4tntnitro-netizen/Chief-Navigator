package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.fleet.MutableFleetStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.StarGenDataSpec;
import com.fs.starfarer.api.impl.campaign.terrain.*;
import com.fs.starfarer.api.impl.campaign.terrain.StarCoronaTerrainPlugin.CoronaParams;
import java.awt.Color;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.util.*;
import java.util.function.BiFunction;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Runs native corona/event-horizon physics; no renderer or campaign save required. */
public final class AviciBlackHoleRegression {
    private static final String OWNED = "$chief_navigator_odyssey_authored_system_v1";
    private static final String FIXED = "$chief_navigator_avici_event_horizon_v1";
    private static StarGenDataSpec blackHoleData;

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static <T> T mock(Class<T> type, BiFunction<String, Object[], Object> call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type },
                (proxy, method, args) -> {
                    String name = method.getName();
                    if (name.equals("equals")) return proxy == args[0];
                    if (name.equals("hashCode")) return System.identityHashCode(proxy);
                    Object result = call.apply(name, args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    return null;
                }));
    }

    private static final class Fixture {
        final Map<String, Object> flags = new HashMap<>();
        final List<CampaignTerrainAPI> terrain = new ArrayList<>();
        boolean owned = true, blackHole = true, correctStar = true, failAdd;
        int additions, removals, orbits;
        StarSystemAPI system;
        PlanetAPI star;
        CampaignFleetAPI player;
        final MemoryAPI memory = mock(MemoryAPI.class, (n, a) -> {
            if (n.equals("getBoolean")) return OWNED.equals(a[0]) ? owned : Boolean.TRUE.equals(flags.get(a[0]));
            if (n.equals("set")) {
                check(a.length == 2 && FIXED.equals(a[0]), "Only the terrain correction receipt may be written");
                flags.put((String) a[0], a[1]);
            }
            return null;
        });

        Fixture() {
            system = mock(StarSystemAPI.class, (n, a) -> {
                if (n.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.MESSINA_ID;
                if (n.equals("getMemoryWithoutUpdate")) return memory;
                if (n.equals("getStar")) return star;
                if (n.equals("getTerrainCopy")) return new ArrayList<>(terrain);
                if (n.equals("getCustomEntitiesWithTag")) return List.of();
                if (n.equals("addTerrain")) {
                    check(Terrain.EVENT_HORIZON.equals(a[0]), "Only the native event horizon may be created");
                    if (failAdd) return null;
                    additions++;
                    CampaignTerrainAPI added = terrain(Terrain.EVENT_HORIZON, (CoronaParams) a[1]);
                    terrain.add(added);
                    return added;
                }
                if (n.equals("removeEntity")) {
                    check(a[0] instanceof CampaignTerrainAPI && terrain.remove(a[0]),
                            "Only existing terrain may be removed; celestial bodies and routes are immutable");
                    removals++;
                }
                if (n.startsWith("set") || n.startsWith("init") || n.startsWith("create"))
                    throw new AssertionError("Unexpected world mutation: " + n);
                return null;
            });
            star = mock(PlanetAPI.class, (n, a) -> {
                if (n.equals("getId")) return correctStar ? OdysseyExpanseSystem.MESSINA_ID + "_black_hole" : "foreign";
                if (n.equals("isBlackHole")) return blackHole;
                if (n.equals("getContainingLocation")) return system;
                if (n.equals("getLocation")) return new Vector2f();
                if (n.equals("getRadius")) return 500f;
                if (n.startsWith("set")) throw new AssertionError("Saved star must remain untouched");
                return null;
            });
            CampaignClockAPI clock = mock(CampaignClockAPI.class,
                    (n, a) -> n.equals("getSecondsPerDay") ? 10f : null);
            Global.setSector(mock(SectorAPI.class, (n, a) -> {
                if (n.equals("getStarSystems")) return List.of(system);
                if (n.equals("getClock")) return clock;
                if (n.equals("getPlayerFleet")) return player;
                if (n.startsWith("create") || n.startsWith("remove") || n.startsWith("add"))
                    throw new AssertionError("Correction must not mutate other campaign objects");
                return null;
            }));
        }

        CampaignTerrainAPI terrain(String type, CoronaParams params) {
            // Only cosmetic color lookup is replaced. Native intensity, flare,
            // velocity, immunity and offscreen rules all execute unchanged.
            StarCoronaTerrainPlugin plugin = Terrain.EVENT_HORIZON.equals(type)
                    ? new EventHorizonPlugin() {
                        public Color getAuroraColorForAngle(float angle) { return Color.WHITE; }
                    } : new StarCoronaTerrainPlugin() {
                        public Color getAuroraColorForAngle(float angle) { return Color.WHITE; }
                    };
            CampaignTerrainAPI entity = mock(CampaignTerrainAPI.class, (n, a) -> {
                if (n.equals("getId")) return "terrain_" + System.identityHashCode(plugin);
                if (n.equals("getType")) return type;
                if (n.equals("getPlugin")) return plugin;
                if (n.equals("getContainingLocation")) return system;
                if (n.equals("getLocation")) return new Vector2f();
                if (n.equals("setCircularOrbit")) {
                    check(a[0] == star && (float) a[1] == 0f && (float) a[2] == 0f,
                            "The horizon must remain centered on the saved star");
                    orbits++;
                }
                return null;
            });
            plugin.init(type, entity, params);
            return entity;
        }

        CampaignTerrainAPI original() {
            CampaignTerrainAPI old = terrain(Terrain.CORONA, new CoronaParams(1400f, 700f, star, 10f, .25f, 3f));
            terrain.add(old);
            return old;
        }

        Vector2f impulse(StarCoronaTerrainPlugin plugin, float x, float y, boolean ignored) {
            Vector2f velocity = new Vector2f();
            FleetDataAPI data = mock(FleetDataAPI.class, (n, a) -> n.equals("getMembersListCopy")
                    ? List.of() : n.equals("getBurnLevel") ? 10f : null);
            MutableFleetStatsAPI stats = mock(MutableFleetStatsAPI.class,
                    (n, a) -> n.equals("getAccelerationMult") ? new MutableStat(1f) : null);
            player = mock(CampaignFleetAPI.class, (n, a) -> {
                if (n.equals("getLocation")) return new Vector2f(x, y);
                if (n.equals("getVelocity")) return velocity;
                if (n.equals("setVelocity")) velocity.set((float) a[0], (float) a[1]);
                if (n.equals("getRadius")) return 10f;
                if (n.equals("getAcceleration")) return 100f;
                if (n.equals("getStats")) return stats;
                if (n.equals("getContainingLocation")) return system;
                if (n.equals("getFleetData")) return data;
                if (n.equals("getViews")) return List.of();
                if (n.equals("isInCurrentLocation")) return true;
                if (n.equals("hasTag")) return ignored && Tags.FLEET_IGNORES_CORONA.equals(a[0]);
                return null;
            });
            plugin.applyEffect(player, .1f);
            return velocity;
        }
    }

    public static void main(String[] args) throws Exception {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Global.setSettings(mock(SettingsAPI.class, (n, a) -> {
                if (n.equals("getSpec")) {
                    check(a[0] == StarGenDataSpec.class && StarTypes.BLACK_HOLE.equals(a[1]),
                            "Use the installed vanilla black-hole specification");
                    return blackHoleData;
                }
                if (n.equals("getColor")) return Color.WHITE;
                return null;
            }));
            JSONArray rows = (JSONArray) Class.forName("com.fs.starfarer.loading.G")
                    .getMethod("o00000", String.class).invoke(null,
                            Files.readString(Path.of("../../starsector-core/data/campaign/procgen/star_gen_data.csv")));
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (StarTypes.BLACK_HOLE.equals(row.optString("id"))) blackHoleData = new StarGenDataSpec(row);
            }
            check(blackHoleData != null && blackHoleData.getSolarWind() == -10f
                    && blackHoleData.getCrLossMult() == 25f, "Installed vanilla black-hole values");
            Fixture f = new Fixture();
            CampaignTerrainAPI old = f.original();
            check(f.impulse((StarCoronaTerrainPlugin) old.getPlugin(), 800f, 0f, false).x > 0f,
                    "Original native corona must reproduce Avici's outward push");
            System.out.println("PASS: old native corona reproduces the outward push.");
            if (args.length > 0 && args[0].equals("--before")) return;
            CampaignTerrainAPI unrelated = f.terrain(Terrain.CORONA,
                    new CoronaParams(2000f, 1000f, null, 5f, .1f, 2f));
            f.terrain.add(unrelated);
            OdysseyExpanseSystem.repairAviciBlackHoleTerrain();
            check(f.additions == 1 && f.removals == 1 && f.orbits == 1
                    && !f.terrain.contains(old) && f.terrain.contains(unrelated),
                    "Replace only Avici's own corona and retain unrelated terrain");
            StarCoronaTerrainPlugin horizon = (StarCoronaTerrainPlugin) f.terrain.stream()
                    .filter(t -> Terrain.EVENT_HORIZON.equals(t.getType())).findFirst().orElseThrow().getPlugin();
            check(horizon instanceof EventHorizonPlugin && horizon.getParams().windBurnLevel == -10f
                    && horizon.getParams().flareProbability == 0f && horizon.getParams().crLossMult == 25f
                    && horizon.getParams().bandWidthInEngine == 1400f && horizon.getParams().middleRadius == 700f,
                    "Use native event-horizon behavior and preserve the authored hazard range");
            for (float[] point : new float[][] {{800, 0}, {-800, 0}, {0, 800}, {0, -800}}) {
                Vector2f impulse = f.impulse(horizon, point[0], point[1], false);
                check(impulse.x * point[0] + impulse.y * point[1] < 0f,
                        "Native event horizon must pull inward from every direction");
            }
            check(f.impulse(horizon, 1600f, 0f, false).lengthSquared() == 0f,
                    "No pull outside the preserved event-horizon radius");
            check(f.impulse(horizon, 800f, 0f, true).lengthSquared() == 0f,
                    "Native corona immunity must remain effective");
            OdysseyExpanseSystem.repairAviciBlackHoleTerrain();
            check(f.additions == 1 && f.removals == 1, "Saved correction must never recreate terrain");
            for (int scenario = 0; scenario < 6; scenario++) {
                Fixture guarded = new Fixture();
                if (scenario != 3) guarded.original();
                if (scenario == 0) guarded.owned = false;
                if (scenario == 1) guarded.blackHole = false;
                if (scenario == 2) guarded.correctStar = false;
                if (scenario == 4) guarded.failAdd = true;
                if (scenario == 5) guarded.original();
                OdysseyExpanseSystem.repairAviciBlackHoleTerrain();
                check(guarded.additions == 0 && guarded.removals == 0 && guarded.flags.isEmpty(),
                        "Foreign, wrong-type, absent, failed or ambiguous state must remain untouched: " + scenario);
            }
            Fixture existing = new Fixture();
            CampaignTerrainAPI nativeHorizon = existing.terrain(Terrain.EVENT_HORIZON,
                    new CoronaParams(1400f, 700f, existing.star, 10f, .25f, 3f));
            existing.terrain.add(nativeHorizon);
            OdysseyExpanseSystem.repairAviciBlackHoleTerrain();
            check(existing.additions == 0 && existing.removals == 0
                    && ((StarCoronaTerrainPlugin) nativeHorizon.getPlugin()).getParams().windBurnLevel == -10f,
                    "An existing native horizon must be corrected in place");
            System.out.println("PASS: native inward pull at four facings, range, immunity, vanilla parameters, ownership guards and once-only correction.");
        } finally {
            Global.setSettings(previousSettings);
            Global.setSector(previousSector);
        }
    }
}
