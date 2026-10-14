package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.awt.Color;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual campaign spawning and observed AI-loss re-formation, including saved latches. */
public final class FinalLaborSpawnRegression {
    private static final String CENTER_ID = "chief_navigator_final_labor_heavenly_strike";
    private static final String CREATED = "$chief_navigator_ithaca_final_center_created_v1";
    private static final String PENDING = "$chief_navigator_ithaca_final_center_reformation_pending_v1";
    private static final String AI_LOSS = "$chief_navigator_ithaca_final_center_autoresolve_log_v1";
    private static final String INITIALIZED = "$chief_navigator_ithaca_final_labor_initialized_v1";
    private static final String ATTRITION = "$chief_navigator_fob_ithaca_final_labor_attrition_lock_v1";
    private static final String SCAN_DAYS = "$chief_navigator_final_gate_scan_days_v1";
    private static final String SCAN_LAST = "$chief_navigator_final_gate_scan_last_update_v1";
    private static final String SCAN_ACTIVE = "$chief_navigator_final_gate_scan_active_v1";
    private static final String DEPARTING = "$chief_navigator_final_gate_scan_departing_v1";
    private static final Method ADVANCE;
    static {
        try {
            ADVANCE = OdysseyPredatorScript.class.getDeclaredMethod("ensureFinalLaborInvasion");
            ADVANCE.setAccessible(true);
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
    private interface Call { Object invoke(String name, Object[] args); }
    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            switch (name) {
                case "getBoolean": return Boolean.TRUE.equals(values.get(args[0]));
                case "get": return values.get(args[0]);
                case "getString": return values.get(args[0]);
                case "getLong": return values.get(args[0]) instanceof Number
                        ? ((Number) values.get(args[0])).longValue() : 0L;
                case "getFloat": return values.get(args[0]) instanceof Number
                        ? ((Number) values.get(args[0])).floatValue() : 0f;
                case "contains": return values.containsKey(args[0]);
                case "set": values.put((String) args[0], args[1]); break;
                case "unset": values.remove(args[0]); break;
            }
            return null;
        });
    }
    private static final class Token {
        String id;
        String type;
        final Map<String, Object> flags = new HashMap<>();
        final MemoryAPI memory = memory(flags);
        Vector2f position;
        StarSystemAPI system;
        SectorEntityToken focus;
        float orbitRadius;
        final SectorEntityToken api = mock(SectorEntityToken.class, (name, args) -> {
            switch (name) {
                case "getId": return id;
                case "setId": id = (String) args[0]; break;
                case "getCustomEntityType": return type;
                case "getMemoryWithoutUpdate": return memory;
                case "getLocation": return position;
                case "getContainingLocation": return system;
                case "getOrbitFocus": return focus;
                case "getCircularOrbitRadius": return orbitRadius;
                case "setFixedLocation": focus = null;
                    position.set((Float) args[0], (Float) args[1]); break;
                case "setCircularOrbit": focus = (SectorEntityToken) args[0];
                    orbitRadius = (Float) args[2]; break;
            }
            return null;
        });
        Token(String id, String type, Vector2f position) {
            this.id = id; this.type = type; this.position = position;
        }
    }
    private static final class Member {
        String variantId;
        String id;
        final RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (n, a) ->
                n.equals("getMaxCR") ? 1f : null);
        final FleetMemberStatusAPI status = mock(FleetMemberStatusAPI.class, (n, a) -> null);
        final ShipVariantAPI variant = mock(ShipVariantAPI.class, (n, a) ->
                n.equals("getHullVariantId") ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID : null);
        final ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class, (n, a) ->
                n.equals("getHullId") ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_HULL_ID : null);
        final FleetMemberAPI api = mock(FleetMemberAPI.class, (name, args) -> {
            switch (name) {
                case "getId": return id;
                case "setId": id = (String) args[0]; break;
                case "getHullId": return variantId;
                case "getVariant": return isDefender() ? variant : null;
                case "getHullSpec": return isDefender() ? hull : null;
                case "getStatus": return status;
                case "getRepairTracker": return repair;
            }
            return null;
        });
        Member(String variantId) { this.variantId = variantId; }
        boolean isDefender() {
            return OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID.equals(variantId);
        }
    }
    private static final class Fleet {
        String id;
        String name;
        boolean station;
        boolean player;
        boolean busy;
        BattleAPI battle;
        LocationAPI location;
        final Map<String, Object> flags = new HashMap<>();
        final MemoryAPI memory = memory(flags);
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        final List<FleetAssignmentDataAPI> assignments = new ArrayList<>();
        final Map<SectorEntityToken, Float> attackBlocks = new IdentityHashMap<>();
        final CampaignFleetAIAPI ai = mock(CampaignFleetAIAPI.class, (name, args) -> {
            if (name.equals("doNotAttack")) attackBlocks.put(
                    (SectorEntityToken) args[0], (Float) args[1]);
            return null;
        });
        final Vector2f position = new Vector2f();
        final Vector2f velocity = new Vector2f();
        final FleetDataAPI data = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (name.equals("addFleetMember")) members.add((FleetMemberAPI) args[0]);
            return null;
        });
        final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (name, args) -> {
            switch (name) {
                case "getId": return id;
                case "setId": id = (String) args[0]; break;
                case "getName": return this.name;
                case "setName": this.name = (String) args[0]; break;
                case "isStationMode": return station;
                case "setStationMode": station = (Boolean) args[0]; break;
                case "isPlayerFleet": return player;
                case "isInHyperspaceTransition": return busy;
                case "getBattle": return battle;
                case "getMemoryWithoutUpdate": return memory;
                case "getContainingLocation": return location;
                case "getFleetData": return data;
                case "getAI": return ai;
                case "getCurrentAssignment": return assignments.isEmpty() ? null : assignments.get(0);
                case "clearAssignments": assignments.clear(); break;
                case "addAssignment":
                    FleetAssignment kind = (FleetAssignment) args[0];
                    SectorEntityToken target = (SectorEntityToken) args[1];
                    assignments.add(mock(FleetAssignmentDataAPI.class, (n, a) ->
                            n.equals("getAssignment") ? kind : n.equals("getTarget") ? target : null));
                    break;
                case "isEmpty": return members.isEmpty();
                case "getLocation": return position;
                case "getVelocity": return velocity;
                case "setFixedLocation":
                case "setLocation": position.set((Float) args[0], (Float) args[1]); break;
                case "getEventListeners": return listeners;
                case "addEventListener": listeners.add((FleetEventListener) args[0]); break;
            }
            return null;
        });
    }
    private static final class Fixture {
        final Map<String, Object> flags = new HashMap<>();
        final MemoryAPI memory = memory(flags);
        final Map<String, Object> systemFlags = new HashMap<>();
        final MemoryAPI systemMemory = memory(systemFlags);
        final Map<String, SectorEntityToken> entities = new HashMap<>();
        final List<CampaignFleetAPI> fleets = new ArrayList<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final Map<CampaignFleetAPI, Fleet> fleetStates = new IdentityHashMap<>();
        final Fleet player = new Fleet();
        final Token center = new Token(IthacaSectionEncounter.FOB_ENTITY_ID,
                "chief_navigator_fob_ithaca_campaign", new Vector2f(4500f, 0f));
        final Token gate = new Token(OdysseyExpanseSystem.FOB_ITHACA_GATE_ID,
                "inactive_gate", new Vector2f(2790f, 0f));
        long timestamp = 1000L;
        boolean paused;
        final CampaignClockAPI clock = mock(CampaignClockAPI.class, (name, args) -> {
            if (name.equals("getTimestamp")) return timestamp;
            if (name.equals("getElapsedDaysSince")) return (timestamp - (Long) args[0]) / 1000f;
            return null;
        });
        final IntelManagerAPI intel = mock(IntelManagerAPI.class,
                (name,args) -> name.equals("getIntel") ? Collections.emptyList() : null);
        final CampaignUIAPI ui = mock(CampaignUIAPI.class, (name,args) -> null);
        int centerCreations;
        int defenderCreations;
        int removals;
        boolean nullCenterFactory;
        boolean nullDefenderFactory;
        final StarSystemAPI system = mock(StarSystemAPI.class, (name, args) -> {
            switch (name) {
                case "getOptionalUniqueId": return OdysseyExpanseSystem.ASHEN_VERGE_ID;
                case "getEntityById": return entities.get(args[0]);
                case "getMemoryWithoutUpdate": return systemMemory;
                case "getFleets": return new ArrayList<>(fleets);
                case "addEntity": add((SectorEntityToken) args[0]); break;
                case "removeEntity": remove((SectorEntityToken) args[0]); break;
                case "createToken": return new Token(null, null,
                        new Vector2f((Float) args[0], (Float) args[1])).api;
            }
            return null;
        });
        Fixture() {
            player.player = true;
            systems.add(system);
            player.id = "player";
            player.location = system;
            center.system = system;
            gate.system = system;
            gate.flags.put("$chief_navigator_fob_ithaca_gate_v1", true);
            center.flags.put("$chief_navigator_fob_ithaca_owned_v1", true);
            // An already accepted save at the end of the perimeter phase.
            flags.put(MenelausTrial.ACCEPTED, true);
            flags.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
            flags.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
            flags.put(INITIALIZED, true);
            for (String section : Arrays.asList("north", "east", "south")) {
                flags.put("$chief_navigator_ithaca_final_labor_lane_repelled_" + section, true);
            }
            systemFlags.put("$chief_navigator_odyssey_ashen_verge_abyss_v1", true);
            systemFlags.put(ATTRITION, true);
            entities.put(center.id, center.api);
            entities.put(gate.id, gate.api);
            fleets.add(player.api);
            Global.setSettings(mock(SettingsAPI.class, (name, args) ->
                    name.equals("getCurrentState") ? GameState.CAMPAIGN
                            : name.equals("getColor") ? Color.WHITE : null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory;
                if (name.equals("getPlayerFleet")) return player.api;
                if (name.equals("getStarSystems")) return systems;
                if (name.equals("getAllLocations")) return systems;
                if (name.equals("getCampaignUI")) return ui;
                if (name.equals("getClock")) return clock;
                if (name.equals("isPaused")) return paused;
                if (name.equals("getIntelManager")) return intel;
                if (name.equals("getHyperspace")) return system;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createEmptyFleet")) {
                    boolean finalFleet = "Heavenly Strike".equals(args[1]);
                    if (finalFleet) {
                        centerCreations++;
                        if (nullCenterFactory) return null;
                    } else {
                        defenderCreations++;
                        if (nullDefenderFactory) return null;
                    }
                    Fleet fleet = new Fleet();
                    fleet.location = system;
                    fleetStates.put(fleet.api, fleet);
                    return fleet.api;
                }
                if (name.equals("createFleetMember")) return new Member((String) args[1]).api;
                return null;
            }));
        }
        void add(SectorEntityToken entity) {
            entities.put(entity.getId(), entity);
            if (entity instanceof CampaignFleetAPI && !fleets.contains(entity)) {
                fleets.add((CampaignFleetAPI) entity);
            }
        }
        void remove(SectorEntityToken entity) {
            entities.remove(entity.getId(), entity);
            fleets.remove(entity);
            removals++;
        }
        Fleet defender(boolean empty) {
            Fleet fleet = new Fleet();
            fleet.id = OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID;
            fleet.station = true;
            fleet.location = system;
            fleet.position.set(2880f, 0f);
            if (!empty) fleet.members.add(new Member(
                    OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID).api);
            fleetStates.put(fleet.api, fleet);
            add(fleet.api);
            return fleet;
        }
        Fleet finale() { return fleetStates.get(entities.get(CENTER_ID)); }
        void days(float days) { timestamp += Math.round(days * 1000f); }
        void advance() throws Exception {
            try { ADVANCE.invoke(null); }
            catch (InvocationTargetException failure) {
                Throwable cause = failure.getCause();
                if (cause instanceof Exception) throw (Exception) cause;
                if (cause instanceof Error) throw (Error) cause;
                throw failure;
            }
        }
    }
    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        SettingsAPI previousSettings = Global.getSettings();
        try { run(); }
        finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
            Global.setSettings(previousSettings);
        }
        System.out.println("PASS: saved perimeter victories spawn one Heavenly Strike; "
                + "unavailable defenders select gate fallback, failed creation remains retryable, "
                + "observed AI losses re-form once, thirty eligible scan days survive pauses/reload, "
                + "expiry queues defeat/departure, player victory prevents expiry, and foreign/busy state is preserved.");
    }
    private static void run() throws Exception {
        Fixture f = new Fixture();
        check(OdysseyPredatorScript.getFinalLaborInvasionProgress() == 3,
                "All three saved perimeter victories are retained");
        f.advance();
        check(f.finale() != null && f.centerCreations == 1 && f.defenderCreations == 0,
                "A missing pre-assault defender cannot block the finale");
        check(f.finale().position.x == -6120f && f.finale().position.y == 0f,
                "Absent defender uses the authored breach spawn coordinate");
        check(f.finale().assignments.get(0).getAssignment() == FleetAssignment.GO_TO_LOCATION
                        && f.finale().assignments.get(0).getTarget() == f.gate.api
                        && !f.finale().memory.getBoolean(MemFlags.MEMORY_KEY_PURSUE_PLAYER),
                "Fallback travels to the owned Ithaca Gate without pursuing the player");
        check(f.memory.getBoolean(CREATED) && f.finale().members.size() == 34,
                "Commit the successful full finale roster");
        Fleet original = f.finale();
        original.members.remove(0);
        f.advance();
        check(f.finale() == original && original.members.size() == 33 && f.centerCreations == 1,
                "Never duplicate or replenish a surviving serialized finale");

        f = new Fixture();
        Fleet emptyDefender = f.defender(true);
        f.advance();
        check(f.finale() != null && emptyDefender.members.isEmpty() && f.defenderCreations == 0,
                "The finale does not recreate an empty defender as a prerequisite");

        f = new Fixture();
        f.nullDefenderFactory = true;
        f.player.location = null;
        f.advance();
        check(f.finale() != null && f.defenderCreations == 0
                        && f.finale().assignments.get(0).getAssignment() == FleetAssignment.GO_TO_LOCATION
                        && !f.finale().memory.getBoolean(MemFlags.MEMORY_KEY_PURSUE_PLAYER)
                        && f.finale().assignments.get(0).getTarget() == f.gate.api,
                "Player elsewhere still routes to the gate without invoking the defender factory");
        Fleet holding = f.finale();
        f.player.location = f.system;
        f.advance();
        check(f.finale() == holding && holding.assignments.get(0).getTarget() == f.gate.api,
                "The same fleet retains its gate destination when the player arrives");
        f.player.location = null;
        f.advance();
        check(holding.assignments.get(0).getAssignment() == FleetAssignment.GO_TO_LOCATION
                        && f.centerCreations == 1,
                "Player departure never changes the gate destination");

        f = new Fixture();
        Fleet usableDefender = f.defender(false);
        f.advance();
        Fleet assault = f.finale();
        check(assault.assignments.size() == 3
                        && assault.assignments.get(2).getTarget() == usableDefender.api
                        && !assault.memory.getBoolean(MemFlags.MEMORY_KEY_PURSUE_PLAYER),
                "A usable defender retains the primary station assault queue");
        usableDefender.battle = mock(BattleAPI.class, (n,a) -> null);
        f.advance();
        check(f.finale() == assault && assault.assignments.size() == 3
                        && assault.assignments.get(2).getTarget() == usableDefender.api,
                "A live defender remains the primary target while it is busy in another battle");
        usableDefender.battle = null;
        f.advance();
        check(f.finale() == assault && assault.assignments.size() == 3
                        && assault.assignments.get(2).getTarget() == usableDefender.api
                        && f.centerCreations == 1,
                "A recovered usable station resumes the assault without another fleet");

        f = new Fixture();
        f.advance();
        Fleet defeated = f.finale();
        check(defeated.listeners.size() == 1, "The actual spawned fleet has its defeat listener");
        defeated.members.clear();
        BattleAPI aiBattle = mock(BattleAPI.class, (name, args) ->
                name.equals("wasFleetDefeated") ? true : null);
        defeated.listeners.get(0).reportBattleOccurred(defeated.api, null, aiBattle);
        check(!f.memory.getBoolean(CREATED) && defeated.memory.getBoolean(AI_LOSS),
                "Observed AI-only defeat authorizes re-formation");
        // Reproduce a save from the old upkeep race: exact defeated token
        // survives, but the creation latch was already wrongly restored.
        f.flags.put(CREATED, true);
        f.flags.remove(PENDING);
        SectorEntityToken oldDefender = f.entities.get(
                OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        if (oldDefender != null) f.remove(oldDefender);
        defeated.busy = true;
        f.advance();
        check(f.finale() == defeated && f.memory.getBoolean(CREATED)
                        && f.defenderCreations == 0,
                "Defer even an observed AI loss while the exact fleet is busy");
        defeated.busy = false;
        f.advance();
        Fleet replacement = f.finale();
        check(replacement != null && replacement != defeated && f.centerCreations == 2
                        && f.defenderCreations == 0,
                "Repair saved AI-loss relatching without requiring a defender");
        // A delayed native despawn of the old token must not delete or
        // trigger a second replacement of the new canonical token.
        f.remove(defeated.api);
        defeated.listeners.get(0).reportFleetDespawnedToListener(defeated.api, null, null);
        f.advance();
        check(f.finale() == replacement && f.centerCreations == 2 && f.memory.getBoolean(CREATED)
                        && !f.memory.getBoolean(PENDING),
                "Retained empty token followed by despawn produces one replacement");

        f = new Fixture();
        f.defender(false);
        f.nullCenterFactory = true;
        f.advance();
        check(!f.memory.getBoolean(CREATED) && f.finale() == null,
                "A null factory result cannot consume successful creation");
        f.nullCenterFactory = false;
        f.advance();
        check(f.finale() != null && f.centerCreations == 2,
                "The next eligible tick retries failed construction once");

        f = new Fixture();
        f.advance();
        defeated = f.finale();
        defeated.members.clear();
        defeated.listeners.get(0).reportBattleOccurred(defeated.api, null, aiBattle);
        f.remove(defeated.api);
        f.flags.put(CREATED, true);
        f.nullCenterFactory = true;
        f.advance();
        check(f.finale() == null && !f.memory.getBoolean(CREATED) && f.memory.getBoolean(PENDING),
                "Durable observed loss survives native despawn and failed re-formation");
        f.nullCenterFactory = false;
        f.advance();
        check(f.finale() != null && !f.memory.getBoolean(PENDING) && f.centerCreations == 3,
                "Successful re-formation consumes the durable observed-loss claim");

        f = new Fixture();
        f.advance();
        defeated = f.finale();
        defeated.members.clear();
        defeated.flags.put(AI_LOSS, true);
        f.flags.put(CREATED, true);
        f.flags.remove(PENDING);
        f.remove(defeated.api);
        defeated.listeners.get(0).reportFleetDespawnedToListener(defeated.api, null, null);
        f.advance();
        check(f.finale() != null && f.centerCreations == 2,
                "Native despawn can preserve exact legacy AI-loss evidence before upkeep");

        f = new Fixture();
        f.flags.put(CREATED, true);
        f.advance();
        check(f.finale() != null && f.centerCreations == 1 && f.defenderCreations == 0,
                "An active initialized three-of-four save recovers an absent old finale claim");

        f = new Fixture();
        f.flags.put(CREATED, true);
        SectorEntityToken elsewhereClaim = new Token(CENTER_ID, null, new Vector2f()).api;
        StarSystemAPI elsewhere = mock(StarSystemAPI.class, (name,args) -> {
            if (name.equals("getOptionalUniqueId")) return "foreign_system";
            if (name.equals("getEntityById")) return CENTER_ID.equals(args[0]) ? elsewhereClaim : null;
            if (name.equals("getFleets")) return Collections.emptyList();
            return null;
        });
        f.systems.add(elsewhere);
        f.advance();
        check(f.finale() == null && f.centerCreations == 0,
                "A canonical claim outside Ashen prevents creating a duplicate recovery fleet");

        f = new Fixture();
        Fleet busyDefender = f.defender(true);
        busyDefender.busy = true;
        f.advance();
        check(f.centerCreations == 1 && busyDefender.members.isEmpty()
                        && f.memory.getBoolean(CREATED)
                        && f.finale().assignments.get(0).getTarget() == f.gate.api,
                "Busy empty defender cannot block creation or gate fallback");
        busyDefender.busy = false;
        busyDefender.members.add(new Member(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID).api);
        f.advance();
        check(f.centerCreations == 1 && f.finale().assignments.get(2).getTarget() == busyDefender.api,
                "An externally recovered defender resumes the existing station assault");

        f = new Fixture();
        Fleet foreignDefender = f.defender(true);
        foreignDefender.members.add(new Member("foreign_variant").api);
        f.advance();
        check(f.finale() != null && f.defenderCreations == 0
                        && foreignDefender.members.size() == 1
                        && f.finale().assignments.get(0).getTarget() == f.gate.api,
                "A foreign nonempty defender remains untouched and cannot block the finale");

        for (boolean playerClaim : Arrays.asList(false, true)) {
            f = new Fixture();
            Fleet foreign = new Fleet();
            foreign.id = CENTER_ID;
            foreign.player = playerClaim;
            foreign.flags.put(AI_LOSS, true);
            if (playerClaim) foreign.flags.put(OdysseyPredatorScript.ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
            f.add(foreign.api);
            f.advance();
            check(f.entities.get(CENTER_ID) == foreign.api && f.centerCreations == 0
                            && f.defenderCreations == 0,
                    "An AI-loss-looking marker never adopts a foreign or player fleet");
        }
        f = new Fixture();
        SectorEntityToken wrongType = new Token(CENTER_ID, null, new Vector2f()).api;
        f.add(wrongType);
        f.advance();
        check(f.entities.get(CENTER_ID) == wrongType && f.centerCreations == 0
                        && f.defenderCreations == 0,
                "A wrong-type center ID claim remains untouched");
        for (String terminal : Arrays.asList(MenelausTrial.GAUTAMA_DEFEATED,
                MenelausTrial.FINAL_LABOR_FAILED)) {
            f = new Fixture();
            f.flags.put(terminal, true);
            f.advance();
            check(f.centerCreations == 0 && f.defenderCreations == 0,
                    "Player victory/final failure cannot create another assault");
        }
        verifyGateScan();
    }

    private static void verifyGateScan() throws Exception {
        Fixture f = new Fixture();
        f.advance();
        Fleet scan = f.finale();
        Map<String,Object> originalGateFlags = new HashMap<>(f.gate.flags);
        scan.position.set(f.gate.position);
        f.advance();
        check(scan.memory.getBoolean(SCAN_ACTIVE)
                        && scan.assignments.get(0).getAssignment() == FleetAssignment.ORBIT_PASSIVE,
                "Contact with the owned gate starts the thirty-day hovering scan");
        f.days(29.999f);
        f.advance();
        check(!MenelausTrial.isFinalLaborFailed() && scan.memory.getFloat(SCAN_DAYS) < 30f,
                "The scan cannot fail Labor V before thirty eligible campaign days");
        f.days(.001f);
        f.advance();
        check(MenelausTrial.isFinalLaborGateScanFailed()
                        && f.memory.getBoolean("$chief_navigator_gate_scan_failure_scene_pending_v1")
                        && scan.memory.getBoolean(DEPARTING),
                "Exactly thirty eligible days queues the defeat scene and fleet departure");
        FleetAssignmentDataAPI departure = scan.assignments.get(0);
        check(departure.getAssignment() == FleetAssignment.GO_TO_LOCATION_AND_DESPAWN
                        && departure.getTarget() == f.gate.api,
                "The failed-scan fleet disappears into the actual owned Ithaca Gate");
        f.advance();
        check(scan.assignments.get(0) == departure,
                "Repeated failed-stage upkeep preserves the native Gate departure order");
        check(f.centerCreations == 1 && f.defenderCreations == 0
                        && f.entities.get(f.center.id) == f.center.api
                        && f.entities.get(f.gate.id) == f.gate.api
                        && f.gate.flags.equals(originalGateFlags),
                "Failure does not replace the fleet or destroy/rebuild campaign objects");

        Map<String,Object> departedFlags = new HashMap<>(scan.flags);
        Map<String,Object> defeatedGlobalFlags = new HashMap<>(f.flags);
        Token savedGate = f.gate;
        long departureTimestamp = f.timestamp;
        Fixture departureLoad = new Fixture();
        departureLoad.flags.clear(); departureLoad.flags.putAll(defeatedGlobalFlags);
        departureLoad.timestamp = departureTimestamp;
        scan.flags.clear(); scan.flags.putAll(departedFlags);
        scan.location = departureLoad.system;
        savedGate.system = departureLoad.system;
        departureLoad.entities.put(savedGate.id,savedGate.api);
        departureLoad.entities.put(CENTER_ID,scan.api);
        departureLoad.fleets.add(scan.api);
        departureLoad.fleetStates.put(scan.api,scan);
        departureLoad.advance();
        check(scan.assignments.get(0) == departure
                        && departure.getTarget() == savedGate.api
                        && departureLoad.centerCreations == 0
                        && savedGate.flags.equals(originalGateFlags),
                "Reload preserves the saved Gate departure without reissuing it or activating the Gate");

        f = new Fixture();
        f.advance();
        scan = f.finale();
        scan.position.set(f.gate.position);
        f.advance();
        f.days(10f); f.advance();
        check(scan.memory.getFloat(SCAN_DAYS) == 10f, "Ten eligible scan days are saved on the fleet");
        scan.battle = mock(BattleAPI.class, (n,a) -> null);
        f.days(20f); f.advance();
        check(scan.memory.getFloat(SCAN_DAYS) == 10f && !MenelausTrial.isFinalLaborFailed(),
                "Battle time is ignored before the controller's busy early return");
        scan.battle = null; f.advance();
        f.days(5f); f.advance();
        check(scan.memory.getFloat(SCAN_DAYS) == 15f, "The scan resumes without banking the battle interval");
        f.paused = true; f.days(50f); f.advance();
        f.paused = false; f.advance();
        check(scan.memory.getFloat(SCAN_DAYS) == 15f, "Paused time is never banked into resumed progress");
        scan.position.translate(5000f,0f); f.days(50f); f.advance();
        scan.position.set(f.gate.position); f.advance();
        check(scan.memory.getFloat(SCAN_DAYS) == 15f,
                "Leaving and returning to the gate retains earned days without banking off-gate time");

        Map<String,Object> savedFleetFlags = new HashMap<>(scan.flags);
        Map<String,Object> savedGlobalFlags = new HashMap<>(f.flags);
        long savedTimestamp = f.timestamp;
        Fixture loaded = new Fixture();
        loaded.flags.clear(); loaded.flags.putAll(savedGlobalFlags);
        loaded.timestamp = savedTimestamp;
        scan.flags.clear(); scan.flags.putAll(savedFleetFlags);
        scan.location = loaded.system;
        loaded.entities.put(CENTER_ID,scan.api);
        loaded.fleets.add(scan.api);
        loaded.fleetStates.put(scan.api,scan);
        loaded.advance();
        check(loaded.finale() == scan && scan.memory.getFloat(SCAN_DAYS) == 15f
                        && loaded.centerCreations == 0,
                "A loaded saved fleet keeps its elapsed scan, roster and identity");
        loaded.days(14.999f); loaded.advance();
        check(!MenelausTrial.isFinalLaborFailed(), "Reload does not shorten the remaining fifteen days");
        loaded.days(.001f); loaded.advance();
        check(MenelausTrial.isFinalLaborGateScanFailed(), "Saved scan state expires at its original thirty-day total");

        f = new Fixture(); f.advance();
        scan = f.finale(); scan.position.set(f.gate.position); f.advance(); f.days(12f); f.advance();
        final CampaignFleetAPI losingPlayer = f.player.api;
        BattleAPI playerLoss = mock(BattleAPI.class, (name,args) -> {
            if (name.equals("isPlayerInvolved")) return true;
            if (name.equals("wasFleetDefeated")) return args[0] == losingPlayer;
            if (name.equals("wasFleetVictorious")) return args[0] != losingPlayer;
            return null;
        });
        scan.listeners.get(0).reportBattleOccurred(scan.api,scan.api,playerLoss);
        check(!MenelausTrial.isFinalLaborFailed() && scan.memory.getFloat(SCAN_DAYS) == 12f,
                "Losing a player battle does not fail Labor V or reset the earned scan days");

        f = new Fixture(); f.advance();
        scan = f.finale(); scan.position.set(f.gate.position); f.advance(); f.days(29f); f.advance();
        final CampaignFleetAPI defeatedFinale = scan.api;
        BattleAPI playerVictory = mock(BattleAPI.class, (name,args) -> {
            if (name.equals("isPlayerInvolved")) return true;
            if (name.equals("wasFleetDefeated")) return args[0] == defeatedFinale;
            return null;
        });
        scan.listeners.get(0).reportBattleOccurred(scan.api,f.player.api,playerVictory);
        f.days(100f); f.advance();
        check(MenelausTrial.isGautamaDefeated() && !MenelausTrial.isFinalLaborFailed()
                        && !f.memory.getBoolean("$chief_navigator_gate_scan_failure_scene_pending_v1"),
                "Player victory before scan expiry cancels the pending failure permanently");

        String source = Files.readString(Path.of("src/chiefnavigator/quest/OdysseyPredatorScript.java"));
        int start = source.indexOf("public void advance(float amount)");
        int end = source.indexOf("Historical compatibility entry point",start);
        check(start >= 0 && end > start && !source.substring(start,end).contains("ensureGateBreachPopulation()"),
                "The ordinary campaign advance path no longer creates failed-Labor gate incursions");
    }
}
