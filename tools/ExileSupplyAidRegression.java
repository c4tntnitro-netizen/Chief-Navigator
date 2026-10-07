package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Headless supply-transfer and native-travel contracts for Sanzu's exiles. */
public final class ExileSupplyAidRegression {
    private static final String ITHACA_OWNER =
            "$chief_navigator_fob_ithaca_owned_v1";
    private static final String ITHACA_ID = "chief_navigator_fob_ithaca";
    private static final String ITHACA_TYPE = "chief_navigator_fob_ithaca_campaign";

    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object result = call.invoke(method.getName(), args);
                    if (result != null) return result;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == byte.class) return (byte) 0;
                    if (returns == short.class) return (short) 0;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    if (returns == char.class) return '\0';
                    return null;
                }));
    }

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("contains")) return values.containsKey(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static final class Cargo {
        float supplies;
        int transfers;
        final CargoAPI api;

        Cargo(float supplies) {
            this.supplies = supplies;
            api = mock(CargoAPI.class, (name, args) -> {
                if (name.equals("getSupplies")) return this.supplies;
                if (name.equals("removeSupplies")) {
                    this.supplies -= ((Number) args[0]).floatValue();
                    transfers++;
                } else if (name.equals("addSupplies")) {
                    this.supplies += ((Number) args[0]).floatValue();
                    transfers++;
                } else if (name.startsWith("add") || name.startsWith("remove")) {
                    throw new AssertionError("Aid must not transfer passengers or other cargo: " + name);
                }
                return null;
            });
        }
    }

    static final class Fleet {
        final Map<String, Object> state = new HashMap<>();
        final MemoryAPI memory = memory(state);
        final List<String> mutations = new ArrayList<>();
        final CampaignFleetAPI api;
        final Cargo cargo = new Cargo(3f);
        CargoAPI cargoApi = cargo.api;
        StarSystemAPI location;
        BattleAPI battle;
        boolean player, empty, transitioning, despawning;
        boolean abortDespawn = true;
        FleetAssignment assignment;
        SectorEntityToken destination;
        float assignmentDuration;

        Fleet(boolean player) {
            this.player = player;
            api = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getCargo")) return cargoApi;
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getBattle")) return battle;
                if (name.equals("isPlayerFleet")) return this.player;
                if (name.equals("isEmpty")) return empty;
                if (name.equals("isInHyperspaceTransition")) return transitioning;
                if (name.equals("isDespawning")) return despawning;
                if (name.startsWith("set") || name.startsWith("clear")
                        || name.startsWith("add") || name.startsWith("remove")) {
                    mutations.add(name);
                    if (name.equals("setAbortDespawn")) {
                        abortDespawn = (Boolean) args[0];
                    } else if (name.equals("addAssignment")) {
                        assignment = (FleetAssignment) args[0];
                        destination = (SectorEntityToken) args[1];
                        assignmentDuration = ((Number) args[2]).floatValue();
                    } else if (!name.equals("clearAssignments")) {
                        throw new AssertionError("Unexpected fleet mutation: " + name);
                    }
                }
                if (name.equals("getCommander") || name.equals("getFleetData")) {
                    throw new AssertionError("Aid must not create contacts or change crews/ships");
                }
                return null;
            });
        }
    }

    static final class Fixture {
        final Fleet player = new Fleet(true);
        final Fleet exiles = new Fleet(false);
        CampaignFleetAPI sectorPlayer = player.api;
        SectorEntityToken interactionTarget;
        final Map<String, Object> stationState = new HashMap<>(Map.of(ITHACA_OWNER, true));
        MemoryAPI stationMemory = memory(stationState);
        String stationId = ITHACA_ID;
        String stationType = ITHACA_TYPE;
        int removals, additions;
        final List<CampaignFleetAPI> fleets = new ArrayList<>();
        final StarSystemAPI sanzu;
        final SectorEntityToken station;

        Fixture() {
            sanzu = mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getFleets")) return fleets;
                if (name.equals("removeEntity")) removals++;
                if (name.equals("addEntity")) additions++;
                return null;
            });
            station = mock(SectorEntityToken.class, (name, args) -> {
                if (name.equals("getId")) return stationId;
                if (name.equals("getCustomEntityType")) return stationType;
                if (name.equals("getMemoryWithoutUpdate")) return stationMemory;
                if (name.startsWith("set") || name.startsWith("add")
                        || name.startsWith("remove")) {
                    throw new AssertionError("Aid must not repair or modify Ithaca: " + name);
                }
                return null;
            });
            InteractionDialogAPI dialog = mock(InteractionDialogAPI.class,
                    (name, args) -> name.equals("getInteractionTarget") ? interactionTarget : null);
            CampaignUIAPI ui = mock(CampaignUIAPI.class,
                    (name, args) -> name.equals("getCurrentInteractionDialog")
                            && interactionTarget != null ? dialog : null);
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getPlayerFleet")) return sectorPlayer;
                if (name.equals("getCampaignUI")) return ui;
                if (name.equals("getImportantPeople") || name.equals("getMemoryWithoutUpdate")
                        || name.startsWith("add") || name.startsWith("remove")) {
                    throw new AssertionError("Aid must not create contacts, passengers, or scripts: " + name);
                }
                return null;
            }));
            player.cargo.supplies = 57f;
            player.location = sanzu;
            exiles.location = sanzu;
            exiles.state.put(OdysseyStrandedFleetsScript.SANZU_EXILE_MARKER, true);
            for (String flag : List.of(MemFlags.MEMORY_KEY_NO_JUMP,
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                    MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
                    MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE,
                    MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY,
                    MemFlags.MEMORY_KEY_NO_REP_IMPACT)) {
                exiles.state.put(flag, true);
            }
            fleets.add(exiles.api);
        }
    }

    static void denied(Fixture fixture, CampaignFleetAPI fleet,
            SectorEntityToken station, String reason) {
        float playerSupplies = fixture.player.cargo.supplies;
        float exileSupplies = fixture.exiles.cargo.supplies;
        Map<String, Object> exileState = new HashMap<>(fixture.exiles.state);
        check(!ExileSupplyAid.canDonate(fleet, station), reason + " must disable aid");
        check(!ExileSupplyAid.donateSupplies(fleet, station), reason + " must reject transfer");
        check(fixture.player.cargo.supplies == playerSupplies
                        && fixture.exiles.cargo.supplies == exileSupplies
                        && fixture.player.cargo.transfers == 0
                        && fixture.exiles.cargo.transfers == 0
                        && fixture.exiles.state.equals(exileState)
                        && fixture.exiles.mutations.isEmpty()
                        && fixture.player.mutations.isEmpty()
                        && fixture.removals == 0 && fixture.additions == 0,
                reason + " must be a mutation-free refusal");
    }

    static void testRefusals() {
        Fixture f = new Fixture();
        denied(f, null, f.station, "Missing fleet");
        f = new Fixture(); f.player.cargo.supplies = 19.99f;
        denied(f, f.exiles.api, f.station, "Insufficient supplies");
        f = new Fixture(); f.exiles.state.clear();
        denied(f, f.exiles.api, f.station, "An unrelated fleet");
        f = new Fixture(); f.exiles.player = true;
        denied(f, f.exiles.api, f.station, "Player-controlled exile-marked fleet");
        f = new Fixture(); f.sectorPlayer = f.exiles.api;
        denied(f, f.exiles.api, f.station, "Player fleet identified by sector ownership");
        f = new Fixture();
        denied(f, f.exiles.api, null, "Missing Ithaca");
        f = new Fixture(); f.stationId = "some_other_station";
        denied(f, f.exiles.api, f.station, "Wrong station identity");
        f = new Fixture(); f.stationType = "station_lowtech2";
        denied(f, f.exiles.api, f.station, "Wrong Ithaca type");
        f = new Fixture(); f.stationState.clear();
        denied(f, f.exiles.api, f.station, "Unowned Ithaca token");
        f = new Fixture(); f.stationMemory = null;
        denied(f, f.exiles.api, f.station, "Malformed Ithaca memory");
        f = new Fixture(); f.exiles.empty = true;
        denied(f, f.exiles.api, f.station, "Destroyed/empty fleet");
        f = new Fixture(); f.exiles.battle = mock(BattleAPI.class, (name, args) -> null);
        denied(f, f.exiles.api, f.station, "Fleet in battle");
        f = new Fixture(); f.exiles.transitioning = true;
        denied(f, f.exiles.api, f.station, "Fleet in hyperspace transition");
        f = new Fixture(); f.exiles.despawning = true;
        denied(f, f.exiles.api, f.station, "Despawning fleet");
        f = new Fixture(); f.exiles.location = null;
        denied(f, f.exiles.api, f.station, "Fleet outside campaign locations");
        f = new Fixture(); f.exiles.cargoApi = null;
        denied(f, f.exiles.api, f.station, "Missing recipient cargo");
        f = new Fixture(); f.player.cargoApi = null;
        denied(f, f.exiles.api, f.station, "Missing player cargo");
        f = new Fixture(); f.sectorPlayer = null;
        denied(f, f.exiles.api, f.station, "Missing player fleet");
        f = new Fixture(); Global.setSector(null);
        denied(f, f.exiles.api, f.station, "Missing campaign");
    }

    static void testAidAndPopulation() throws Exception {
        Fixture f = new Fixture();
        f.interactionTarget = f.exiles.api;
        check(TroyArrivalScript.isFleetBusyForMutation(f.exiles.api),
                "The fixture must reproduce the active-dialog mutation guard");
        check(ExileSupplyAid.canDonate(f.exiles.api, f.station)
                        && ExileSupplyAid.donateSupplies(f.exiles.api, f.station),
                "Explicit aid must work while the recipient is the current dialog target");
        check(f.player.cargo.supplies == 37f && f.exiles.cargo.supplies == 23f
                        && f.player.cargo.transfers == 1 && f.exiles.cargo.transfers == 1,
                "Aid must debit and credit exactly twenty supplies once");
        check(ExileSupplyAid.hasReceivedAid(f.exiles.api),
                "The donated fleet must retain its durable aided marker");
        for (String flag : List.of(MemFlags.MEMORY_KEY_NO_JUMP,
                MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE,
                MemFlags.MEMORY_KEY_MAKE_ALWAYS_PURSUE,
                MemFlags.MEMORY_KEY_AVOID_PLAYER_SLOWLY)) {
            check(!f.exiles.state.containsKey(flag), "Aid must clear " + flag);
        }
        for (String flag : List.of(MemFlags.MEMORY_KEY_MAKE_NON_HOSTILE,
                MemFlags.NON_HOSTILE_OVERRIDES_MAKE_HOSTILE,
                MemFlags.MEMORY_KEY_MAKE_NON_AGGRESSIVE,
                MemFlags.MEMORY_KEY_NO_REP_IMPACT)) {
            check(Boolean.TRUE.equals(f.exiles.state.get(flag)),
                    "Aided exiles must remain peaceful and preserve reputation treatment: " + flag);
        }
        check(!f.exiles.abortDespawn
                        && f.exiles.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
                        && f.exiles.destination == f.station
                        && f.exiles.assignmentDuration == 1000f
                        && f.exiles.mutations.equals(List.of(
                                "setAbortDespawn", "clearAssignments", "addAssignment"))
                        && f.removals == 0 && f.additions == 0,
                "Aid must leave the original fleet alive and order native travel to actual Ithaca");
        check(f.stationState.equals(Map.of(ITHACA_OWNER, true)),
                "Aid must not mutate the serialized station or create an arrival contact");
        Map<String, Object> afterAid = new HashMap<>(f.exiles.state);
        check(!ExileSupplyAid.canDonate(f.exiles.api, f.station)
                        && !ExileSupplyAid.donateSupplies(f.exiles.api, f.station)
                        && f.player.cargo.supplies == 37f && f.exiles.cargo.supplies == 23f
                        && f.player.cargo.transfers == 1 && f.exiles.cargo.transfers == 1
                        && f.exiles.state.equals(afterAid) && f.exiles.mutations.size() == 3,
                "Recontact or duplicate callbacks must never repeat aid or replace its orders");

        // Reflection is confined to this external harness, never campaign runtime.
        f.interactionTarget = null;
        Fleet unrelated = new Fleet(false);
        unrelated.location = f.sanzu;
        f.fleets.add(unrelated.api);
        f.player.state.put(OdysseyStrandedFleetsScript.SANZU_EXILE_MARKER, true);
        f.fleets.add(f.player.api);
        Method maintain = OdysseyStrandedFleetsScript.class.getDeclaredMethod(
                "countAndConfigureExiles", StarSystemAPI.class);
        maintain.setAccessible(true);
        int count = (Integer) maintain.invoke(new OdysseyStrandedFleetsScript(), f.sanzu);
        check(count == 1, "An aided fleet still in Sanzu must count toward the two-fleet cap");
        check(f.exiles.state.equals(afterAid) && f.exiles.mutations.size() == 3
                        && f.exiles.assignment == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
                        && f.exiles.destination == f.station && !f.exiles.abortDespawn
                        && unrelated.mutations.isEmpty() && f.player.mutations.isEmpty()
                        && f.removals == 0,
                "Patrol maintenance must not confine, rehostilize, or reassign aided exiles");

        f = new Fixture(); f.player.cargo.supplies = 20f;
        check(ExileSupplyAid.donateSupplies(f.exiles.api, f.station)
                        && f.player.cargo.supplies == 0f && f.exiles.cargo.supplies == 23f,
                "Exactly twenty supplies must suffice without creating a negative cargo balance");
    }

    public static void main(String[] args) throws Exception {
        try {
            testRefusals();
            testAidAndPopulation();
            System.out.println("Exile supply aid regression checks passed.");
        } finally {
            Global.setSector(null);
        }
    }
}
