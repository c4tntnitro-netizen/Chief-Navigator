package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** Gates, exact two-ship grant, scoped combat eligibility, and outcome cleanup. */
public final class DriftingWallLaborSupportRegression {
    interface Call { Object invoke(String name, Object[] args); }
    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    if (method.getReturnType() == long.class) return 0L;
                    return null;
                }));
    }

    static MemoryAPI memory(Map<String, Object> values) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(values.get(args[0]));
            if (name.equals("get")) return values.get(args[0]);
            if (name.equals("set")) values.put((String) args[0], args[1]);
            if (name.equals("unset")) values.remove(args[0]);
            return null;
        });
    }

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    static final class Fixture {
        final Map<String, Object> state = new HashMap<>();
        final Map<String, Object> escortState = new HashMap<>();
        final MemoryAPI globalMemory = memory(state);
        final MemoryAPI escortMemory = memory(escortState);
        final List<FleetMemberAPI> ships = new ArrayList<>();
        final List<String> variants = new ArrayList<>();
        final Vector2f escortPosition = new Vector2f();
        LocationAPI escortLocation;
        BattleAPI escortBattle;
        SectorEntityToken assignmentTarget;
        FleetAssignment assignment;
        String escortId;
        int fleetCreations;
        int removals;
        boolean playerControlled;
        final LocationAPI location = mock(LocationAPI.class, (name, args) -> {
            if (name.equals("addEntity")) escortLocation = locationValue();
            if (name.equals("removeEntity")) {
                check(args[0] == escortValue(), "Cleanup may remove only the owned detachment");
                removals++;
                escortLocation = null;
            }
            return null;
        });
        final CampaignFleetAPI player = mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("isPlayerFleet")) return true;
            if (name.equals("getContainingLocation")) return location;
            if (name.equals("getLocation")) return new Vector2f(100f, 200f);
            return null;
        });
        final SectorEntityToken wall = mock(SectorEntityToken.class, (name, args) -> {
            if (name.equals("getId")) return DriftingWallEncounter.FLEET_ID;
            if (name.equals("getCustomEntityType")) return "chief_navigator_drifting_wall_station";
            if (name.equals("getMemoryWithoutUpdate")) {
                return memory(Map.of(DriftingWallEncounter.MARKER, true));
            }
            if (name.equals("getContainingLocation")) return location;
            return null;
        });
        final FleetDataAPI fleetData = mock(FleetDataAPI.class, (name, args) -> {
            if (name.equals("getMembersListCopy")) return new ArrayList<>(ships);
            if (name.equals("addFleetMember")) ships.add((FleetMemberAPI) args[0]);
            return null;
        });
        final CampaignFleetAPI escort = mock(CampaignFleetAPI.class, (name, args) -> {
            if (name.equals("setId")) escortId = (String) args[0];
            if (name.equals("getId")) return escortId;
            if (name.equals("getMemoryWithoutUpdate")) return escortMemory;
            if (name.equals("getFleetData")) return fleetData;
            if (name.equals("getContainingLocation")) return escortLocation;
            if (name.equals("getBattle")) return escortBattle;
            if (name.equals("isEmpty")) return ships.isEmpty();
            if (name.equals("isPlayerFleet")) return playerControlled;
            if (name.equals("getLocation")) return escortPosition;
            if (name.equals("setLocation")) escortPosition.set((float) args[0], (float) args[1]);
            if (name.equals("isHostileTo")) return args[0] != player;
            if (name.equals("clearAssignments")) { assignment = null; assignmentTarget = null; }
            if (name.equals("addAssignment")) {
                assignment = (FleetAssignment) args[0];
                assignmentTarget = (SectorEntityToken) args[1];
            }
            if (name.equals("getCurrentAssignment") && assignment != null) {
                return mock(FleetAssignmentDataAPI.class, (n, a) -> {
                    if (n.equals("getAssignment")) return assignment;
                    if (n.equals("getTarget")) return assignmentTarget;
                    return null;
                });
            }
            return null;
        });

        LocationAPI locationValue() { return location; }
        CampaignFleetAPI escortValue() { return escort; }

        Fixture() {
            state.put(MenelausTrial.ACCEPTED, true);
            state.put(MenelausTrial.WALL_FRIENDLY, true);
            state.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
            state.put("$chief_navigator_menelaus_final_labor_forced_v1", true);
            state.put("$chief_navigator_menelaus_labor_reports_migrated_v1", true);
            Global.setSettings(mock(SettingsAPI.class, (name, args) -> null));
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return globalMemory;
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getStarSystems")) return List.of();
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createEmptyFleet")) { fleetCreations++; return escort; }
                if (name.equals("createFleetMember")) {
                    variants.add((String) args[1]);
                    RepairTrackerAPI repair = mock(RepairTrackerAPI.class,
                            (n, a) -> n.equals("getMaxCR") ? 0.7f : null);
                    return mock(FleetMemberAPI.class,
                            (n, a) -> n.equals("getRepairTracker") ? repair : null);
                }
                return null;
            }));
        }

        CampaignFleetAPI enemy(boolean finalLabor) {
            return mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return finalLabor
                        ? "chief_navigator_final_labor_heavenly_strike" : "ordinary_enemy";
                if (name.equals("getMemoryWithoutUpdate")) return memory(finalLabor
                        ? Map.of(OdysseyPredatorScript.ITHACA_FINAL_CENTER_BATTLE_MARKER, true)
                        : Map.of());
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getLocation")) return new Vector2f(300f, 400f);
                return null;
            });
        }
    }

    public static void main(String[] args) {
        Fixture f = new Fixture();
        check(DriftingWallLaborSupport.supportAvailable(f.wall), "Accepted Labor V enables support");
        f.state.put(MenelausTrial.FINAL_LABOR_BRIEFED, false);
        check(!DriftingWallLaborSupport.supportAvailable(f.wall), "Revealed but unaccepted V has no support");
        f.state.put(MenelausTrial.FINAL_LABOR_BRIEFED, true);
        f.state.put(MenelausTrial.FINAL_LABOR_FAILED, true);
        check(!DriftingWallLaborSupport.supportAvailable(f.wall), "Failed V has no support");
        f.state.remove(MenelausTrial.FINAL_LABOR_FAILED);
        f.state.put(MenelausTrial.GAUTAMA_DEFEATED, true);
        check(!DriftingWallLaborSupport.supportAvailable(f.wall), "Completed field objective has no support");
        f.state.remove(MenelausTrial.GAUTAMA_DEFEATED);
        f.state.put(MenelausTrial.WALL_FRIENDLY, false);
        check(!DriftingWallLaborSupport.supportAvailable(f.wall), "Hostile Wall grants no support");
        f.state.put(MenelausTrial.WALL_FRIENDLY, true);
        check(!DriftingWallLaborSupport.supportAvailable(f.player), "Foreign target grants no support");
        check(DriftingWallLaborSupport.grant(f.wall), "Wall assigns support automatically");
        check(f.ships.size() == 2 && f.variants.equals(List.of(
                DriftingWallLaborSupport.BASTION_VARIANT,
                DriftingWallLaborSupport.BASTION_VARIANT)), "Exactly two Guard Bastions are created");
        check(f.assignment == FleetAssignment.FOLLOW && f.assignmentTarget == f.player,
                "Detachment follows the player rather than assaulting the Wall");
        check(f.escortState.get(MemFlags.FLEET_IGNORES_OTHER_FLEETS) == Boolean.TRUE,
                "Vanilla pull-in must exclude detachment from unrelated fights");
        check(!f.escortState.containsKey(MemFlags.MEMORY_KEY_NO_JUMP), "Detachment may jump to Ithaca");
        check(f.escortState.get(MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY) == Boolean.TRUE,
                "Temporary support cannot become a free permanent recovery reward");
        check(!DriftingWallLaborSupport.grant(f.wall) && f.fleetCreations == 1,
                "Revisiting Wall cannot grant or replenish escorts");
        check(DriftingWallLaborSupport.findSupport(f.enemy(true), f.player) == f.escort,
                "Nearby detachment joins an authored final-Labor fight");
        check(DriftingWallLaborSupport.findSupport(f.enemy(false), f.player) == null,
                "Ordinary battles cannot use the final-Labor detachment");
        f.escortPosition.set(10000f, 10000f);
        check(DriftingWallLaborSupport.findSupport(f.enemy(true), f.player) == null,
                "Distant escort does not teleport into battle");
        f.escortPosition.set(400f, 500f);
        f.escortBattle = mock(BattleAPI.class, (n, a) -> null);
        f.state.put(MenelausTrial.GAUTAMA_DEFEATED, true);
        DriftingWallLaborSupport.advance();
        check(f.removals == 0, "Cleanup must wait for ongoing combat to finish");
        f.escortBattle = null;
        DriftingWallLaborSupport.advance();
        check(f.removals == 1 && Boolean.TRUE.equals(f.state.get(DriftingWallLaborSupport.GRANTED)),
                "Victory retires detachment without reopening the grant");
        f = new Fixture();
        DriftingWallLaborSupport.grant(f.wall);
        f.state.put(MenelausTrial.FINAL_LABOR_FAILED, true);
        DriftingWallLaborSupport.advance();
        check(f.removals == 1, "Failure retires detachment");
        f = new Fixture();
        DriftingWallLaborSupport.grant(f.wall);
        f.ships.clear();
        DriftingWallLaborSupport.advance();
        check(f.removals == 1 && !DriftingWallLaborSupport.supportAvailable(f.wall),
                "Destroyed escort is retired and never replenished");
        f = new Fixture();
        DriftingWallLaborSupport.grant(f.wall);
        f.playerControlled = true;
        f.state.put(MenelausTrial.FINAL_LABOR_FAILED, true);
        DriftingWallLaborSupport.advance();
        check(f.removals == 0, "Cleanup never removes a player-controlled fleet");
        System.out.println("Drifting Wall Labor V support regression checks passed.");
    }
}
