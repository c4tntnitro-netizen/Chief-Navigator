package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.comm.IntelManagerAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.*;
import java.awt.Color;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Production lifecycle calls against an established, save-shaped campaign. */
public final class IthacaBastionRepairRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == int.class) return 0;
                    if (result == long.class) return 0L;
                    if (result == float.class) return 0f;
                    if (result == double.class) return 0d;
                    return null;
                }));
    }
    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    static MemoryAPI memory(Map<String,Object> map) {
        return mock(MemoryAPI.class, (n,a) -> {
            if (n.equals("set")) map.put((String)a[0], a[1]);
            if (n.equals("unset")) map.remove(a[0]);
            if (n.equals("contains")) return map.containsKey(a[0]);
            if (n.equals("getBoolean")) return Boolean.TRUE.equals(map.get(a[0]));
            if (n.equals("getString") || n.equals("get")) return map.get(a[0]);
            return null;
        });
    }
    static String stationId(IthacaSectionEncounter.Section section) {
        return "chief_navigator_ithaca_station_" + section.id;
    }
    static String proxyId(IthacaSectionEncounter.Section section) {
        return IthacaSectionEncounter.INTERACTION_ID_PREFIX + section.id;
    }
    static final class Member {
        float hull = 1f, repaired, cr;
        String id;
        final FleetMemberStatusAPI status = mock(FleetMemberStatusAPI.class, (n,a) -> {
            if (n.equals("getHullFraction")) return hull;
            if (n.equals("repairFully")) hull = 1f;
            if (n.equals("repairFraction")) {
                repaired += (Float)a[0];
                hull = Math.min(1f, hull + (Float)a[0]);
            }
            return null;
        });
        final RepairTrackerAPI tracker = mock(RepairTrackerAPI.class, (n,a) -> {
            if (n.equals("getMaxCR")) return 0.7f;
            if (n.equals("setCR")) cr = (Float)a[0];
            return null;
        });
        final FleetMemberAPI api = mock(FleetMemberAPI.class, (n,a) -> {
            if (n.equals("getHullId")) return IthacaSectionEncounter.WALL_HULL_ID;
            if (n.equals("getStatus")) return status;
            if (n.equals("getRepairTracker")) return tracker;
            if (n.equals("getId")) return id;
            if (n.equals("setId")) id = (String)a[0];
            return null;
        });
    }
    static final class Fleet {
        String id;
        boolean player, stationMode = true, busy, owned = true;
        final Map<String,Object> flags = new HashMap<>();
        final MemoryAPI mem = memory(flags);
        final List<FleetMemberAPI> members = new ArrayList<>();
        FleetMemberAPI flagship;
        final BattleAPI battle = mock(BattleAPI.class, (n,a) -> null);
        final FleetDataAPI data = mock(FleetDataAPI.class, (n,a) -> {
            if (n.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (n.equals("addFleetMember")) members.add((FleetMemberAPI)a[0]);
            if (n.equals("setFlagship")) flagship = (FleetMemberAPI)a[0];
            return null;
        });
        final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (n,a) -> {
            if (n.equals("getId")) return id;
            if (n.equals("setId")) id = (String)a[0];
            if (n.equals("getMemoryWithoutUpdate")) return mem;
            if (n.equals("getFleetData")) return data;
            if (n.equals("getFlagship")) return flagship;
            if (n.equals("isPlayerFleet")) return player;
            if (n.equals("isStationMode")) return stationMode;
            if (n.equals("setStationMode")) stationMode = (Boolean)a[0];
            if (n.equals("isEmpty")) return members.isEmpty();
            if (n.equals("getBattle")) return busy ? battle : null;
            if (n.equals("getLocation") || n.equals("getVelocity")) return new Vector2f();
            return null;
        });
        Fleet(IthacaSectionEncounter.Section section, boolean empty) {
            id = stationId(section);
            flags.put(IthacaSectionEncounter.MARKER, true);
            flags.put(IthacaSectionEncounter.SECTION_KEY, section.id);
            if (!empty) { flagship = new Member().api; members.add(flagship); }
        }
    }
    static final class Fixture {
        final Map<String,Object> centerFlags = new HashMap<>(), globalFlags = new HashMap<>();
        final Map<String,Object> systemFlags = new HashMap<>(
                Map.of(OdysseyExpanseSystem.GENERATED_ASHEN_VERGE, true));
        final MemoryAPI centerMem = memory(centerFlags), globalMem = memory(globalFlags);
        final Map<String,SectorEntityToken> entities = new HashMap<>();
        final List<Member> createdMembers = new ArrayList<>();
        boolean paused;
        SectorEntityToken interaction;
        final CampaignUIAPI ui = mock(CampaignUIAPI.class, (n,a) -> {
            if (n.equals("getCurrentInteractionDialog") && interaction != null) {
                return mock(InteractionDialogAPI.class,
                        (name,values) -> name.equals("getInteractionTarget") ? interaction : null);
            }
            return null;
        });
        final StarSystemAPI system = mock(StarSystemAPI.class, (n,a) -> {
            if (n.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.ASHEN_VERGE_ID;
            if (n.equals("getMemoryWithoutUpdate")) return memory(systemFlags);
            if (n.equals("getEntityById")) return entities.get(a[0]);
            if (n.equals("getFleets")) return new ArrayList<>(entities.values().stream()
                    .filter(e -> e instanceof CampaignFleetAPI).map(e -> (CampaignFleetAPI)e).toList());
            if (n.equals("addEntity")) entities.put(((SectorEntityToken)a[0]).getId(), (SectorEntityToken)a[0]);
            if (n.equals("removeEntity")) entities.remove(((SectorEntityToken)a[0]).getId());
            if (n.equals("addCustomEntity")) {
                String id = (String)a[0], type = (String)a[2];
                MemoryAPI mem = memory(new HashMap<>());
                CustomCampaignEntityAPI target = mock(CustomCampaignEntityAPI.class, (name,values) -> {
                    if (name.equals("getId")) return id;
                    if (name.equals("getCustomEntityType")) return type;
                    if (name.equals("getMemoryWithoutUpdate")) return mem;
                    return null;
                });
                entities.put(id, target);
                return target;
            }
            return null;
        });
        final SectorEntityToken center = mock(SectorEntityToken.class, (n,a) -> {
            if (n.equals("getId")) return IthacaSectionEncounter.FOB_ENTITY_ID;
            if (n.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
            if (n.equals("getMemoryWithoutUpdate")) return centerMem;
            if (n.equals("getContainingLocation")) return system;
            if (n.equals("getLocation")) return new Vector2f();
            return null;
        });
        Fixture() {
            centerFlags.put("$chief_navigator_fob_ithaca_owned_v1", true);
            entities.put(IthacaSectionEncounter.FOB_ENTITY_ID, center);
            IntelManagerAPI intel = mock(IntelManagerAPI.class,
                    (n,a) -> n.equals("getIntel") ? List.of() : null);
            FactionAPI faction = mock(FactionAPI.class, (n,a) -> null);
            Global.setSector(mock(SectorAPI.class, (n,a) -> {
                if (n.equals("getMemoryWithoutUpdate")) return globalMem;
                if (n.equals("getStarSystems")) return List.of(system);
                if (n.equals("getCampaignUI")) return ui;
                if (n.equals("getIntelManager")) return intel;
                if (n.equals("getFaction") || n.equals("getPlayerFaction")) return faction;
                if (n.equals("isPaused")) return paused;
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (n,a) -> {
                if (n.equals("createEmptyFleet")) return new Fleet(IthacaSectionEncounter.Section.NORTH, true).api;
                if (n.equals("createFleetMember")) {
                    Member member = new Member(); createdMembers.add(member); return member.api;
                }
                return null;
            }));
            Global.setSettings(mock(SettingsAPI.class,
                    (n,a) -> n.equals("getColor") ? Color.WHITE : null));
        }
        void advance(float days) { IthacaSectionEncounter.advanceBastionRepairs(center, days); }
        Fleet put(IthacaSectionEncounter.Section section, boolean empty) {
            Fleet fleet = new Fleet(section, empty); entities.put(fleet.id, fleet.api); return fleet;
        }
        void active() {
            systemFlags.put("$chief_navigator_fob_ithaca_final_labor_attrition_lock_v1", true);
            globalFlags.put(MenelausTrial.ACCEPTED, true);
            globalFlags.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
            globalFlags.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
        }
    }
    public static void main(String[] args) throws Exception {
        var north = IthacaSectionEncounter.Section.NORTH;
        var east = IthacaSectionEncounter.Section.EAST;
        var south = IthacaSectionEncounter.Section.SOUTH;
        Fixture f = new Fixture();
        Fleet original = f.put(east, false);
        IthacaSectionEncounter.ensureSectionStations(f.center);
        check(f.entities.get(stationId(north)) == null, "Ordinary lookup must not instantly rebuild an established loss");
        f.advance(29f);
        check(f.entities.get(stationId(north)) == null, "Destroyed bastion must remain absent before day 30");
        f.advance(1f);
        check(IthacaSectionEncounter.isBastionReady(f.center,north), "Thirty campaign days restore the owned bastion");
        check(f.entities.containsKey(proxyId(north)), "Completed rebuild restores its click proxy in established Ashen Verge");
        check(f.entities.get(stationId(east)) == original.api, "Surviving token and member must never be replaced");
        int built = f.createdMembers.size(); f.advance(100f);
        check(f.createdMembers.size() == built, "Healthy stations do not duplicate or refill rosters");

        f = new Fixture(); Fleet damaged = f.put(north, true);
        Member member = new Member(); member.hull = 0.4f; damaged.members.add(member.api); damaged.flagship = member.api;
        f.advance(3f); check(Math.abs(member.hull - 0.5f) < 0.0001f, "Survivor heals 10% over three days, not instantly");
        damaged.busy = true; f.advance(30f);
        check(Math.abs(member.hull - 0.5f) < 0.0001f, "No healing while in battle");
        damaged.busy = false; f.paused = true; f.advance(30f);
        check(Math.abs(member.hull - 0.5f) < 0.0001f, "Paused campaign cannot advance repairs");
        f.paused = false; f.interaction = damaged.api; f.advance(30f);
        check(Math.abs(member.hull - 0.5f) < 0.0001f, "Do not mutate an open station encounter");
        f.interaction = null; f.advance(15f);
        check(member.hull == 1f && damaged.members.get(0) == member.api, "Gradual repair caps at full and retains exact member");

        f = new Fixture(); Fleet empty = f.put(north, true);
        f.advance(15f); // Serialized progress lives on the center, not this helper.
        f.advance(15f);
        check(f.entities.get(stationId(north)) == empty.api && empty.members.size() == 1,
                "Empty owned token is refilled in place after the complete timer");
        f = new Fixture(); f.centerFlags.put("$chief_navigator_ithaca_north_disabled", true);
        f.advance(29f); check(IthacaSectionEncounter.isDisabled(f.center,north), "Destroyed latch remains while rebuilding");
        f.advance(1f); check(!IthacaSectionEncounter.isDisabled(f.center,north), "Successful completion releases only its disabled latch");

        f = new Fixture(); Fleet player = f.put(north,true); player.player = true;
        Fleet foreign = f.put(east,true); foreign.flags.clear();
        SectorEntityToken wrong = mock(SectorEntityToken.class, (n,a) -> null);
        f.entities.put(stationId(south),wrong); f.advance(60f);
        IthacaSectionEncounter.repairAllBastionsForFinalLabor(f.center);
        check(player.members.isEmpty() && foreign.members.isEmpty()
                && f.entities.get(stationId(south)) == wrong, "Player, foreign and wrong-type claims stay untouched even in reset");

        f = new Fixture(); Fleet incompatible = f.put(north,true);
        incompatible.members.add(mock(FleetMemberAPI.class,(n,a) -> n.equals("getHullId") ? "foreign_hull" : null));
        f.advance(60f); check(incompatible.members.size() == 1, "Nonempty incompatible roster cannot be refilled");
        f = new Fixture(); f.advance(15f); f.active(); f.advance(100f);
        check(f.entities.get(stationId(north)) == null, "Active Labor V cannot rebuild a lost module or accrue repair credit");
        f.globalFlags.put(MenelausTrial.GAUTAMA_DEFEATED,true); f.advance(14f);
        check(f.entities.get(stationId(north)) == null, "Time under siege is not banked into post-victory repair");
        f.advance(1f); check(f.entities.containsKey(stationId(north)), "Repairs resume after victory");
        f = new Fixture(); f.globalFlags.put(MenelausTrial.FINAL_LABOR_FAILED,true); f.advance(100f);
        check(f.entities.get(stationId(north)) == null, "Final failure must retain its loss consequences");

        f = new Fixture(); f.put(east,false); f.active();
        IthacaSectionEncounter.repairAllBastionsForFinalLabor(f.center);
        for (var section : IthacaSectionEncounter.Section.values()) {
            check(IthacaSectionEncounter.isBastionReady(f.center,section)
                    && f.entities.containsKey(proxyId(section)), "Explicit opening resets restore all three stations and proxies");
        }
        f = new Fixture(); Fleet hurt = f.put(north,true);
        Member injured = new Member(); injured.hull = 0.1f;
        hurt.members.add(injured.api); hurt.flagship = injured.api; f.active();
        IthacaSectionEncounter.repairAllBastionsForFinalLabor(f.center);
        check(injured.hull == 1f && hurt.members.get(0) == injured.api,
                "The opening fully repairs surviving hulls without replacing them");
        f = new Fixture(); f.advance(15f);
        Map<String,Object> serialized = new HashMap<>(f.centerFlags);
        f = new Fixture(); f.centerFlags.putAll(serialized); f.advance(14f);
        check(f.entities.get(stationId(north)) == null, "Reload preserves partial rebuild time without instant restoration");
        f.advance(1f); check(f.entities.containsKey(stationId(north)), "Saved rebuild finishes at the original 30-day total");
        f = new Fixture(); Fleet busy = f.put(north,true); busy.busy = true;
        f.active(); f.globalFlags.put("$chief_navigator_ithaca_final_labor_lane_repelled_south",true);
        check(OdysseyPredatorScript.spawnIthacaEndgameInvasionFleets().isEmpty(),
                "Actual opening defers without partially spawning if a bastion is busy");
        check(busy.members.isEmpty() && !f.globalFlags.containsKey(
                        "$chief_navigator_ithaca_final_labor_siege_started_north")
                && Boolean.TRUE.equals(f.globalFlags.get(
                        "$chief_navigator_ithaca_final_labor_lane_repelled_south")),
                "Deferred opening mutates no busy core, starts no clock and clears no previous progress");
        f = new Fixture(); f.entities.put(stationId(north), wrong); f.active();
        check(OdysseyPredatorScript.spawnIthacaEndgameInvasionFleets().isEmpty()
                && f.entities.get(stationId(north)) == wrong,
                "Conflicting station ownership blocks the real opening without destructive repair");
        f = new Fixture(); f.active();
        f.globalFlags.put("$chief_navigator_ithaca_endgame_invasion_active_v1",true);
        Method upkeep = OdysseyPredatorScript.class.getDeclaredMethod("maintainIthacaEndgameConsoleInvasion");
        upkeep.setAccessible(true); upkeep.invoke(null);
        check(OdysseyPredatorScript.getFinalLaborInvasionProgress() == 0,
                "Missing never-spawned lanes must not count as repelled in real upkeep");
        f.globalFlags.put("$chief_navigator_ithaca_final_labor_siege_started_north",1L);
        upkeep.invoke(null);
        check(OdysseyPredatorScript.getFinalLaborInvasionProgress() == 1,
                "A missing previously deployed lane still counts as an actual fleet loss");
        System.out.println("PASS: 30-day timed bastion repairs, retained members, lifecycle guards, opening reset and no phantom lane victories.");
    }
}
