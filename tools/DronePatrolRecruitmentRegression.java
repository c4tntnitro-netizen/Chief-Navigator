package data.campaign.rulecmd;

import chiefnavigator.hullmods.DomainSecurityIFFHullmod;
import chiefnavigator.quest.DronePatrolRecruitment;
import chiefnavigator.quest.MenelausTrial;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.SetStoryOption.StoryOptionParams;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Reassignment, cancellation, late validation, and exact native payment order. */
public final class DronePatrolRecruitmentRegression {
    interface Call { Object invoke(String name, Object[] args); }

    static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[] {type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> returns = method.getReturnType();
                    if (returns == boolean.class) return false;
                    if (returns == int.class) return 0;
                    if (returns == float.class) return 0f;
                    if (returns == long.class) return 0L;
                    if (returns == double.class) return 0d;
                    return null;
                }));
    }

    static MemoryAPI memory(Map<String, Object> state) {
        return mock(MemoryAPI.class, (name, args) -> {
            if (name.equals("getBoolean")) return Boolean.TRUE.equals(state.get(args[0]));
            if (name.equals("get")) return state.get(args[0]);
            if (name.equals("set")) state.put((String) args[0], args[1]);
            return null;
        });
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static final class Member {
        final FleetMemberAPI api;
        ShipVariantAPI variant;
        final Set<String> permanent = new HashSet<>();
        final Set<String> tags = new HashSet<>();
        int clones;
        boolean fighter, station;

        Member(String name) {
            ShipHullSpecAPI spec = mock(ShipHullSpecAPI.class,
                    (method, args) -> method.equals("hasTag") && "derelict".equals(args[0]));
            variant = variant();
            api = mock(FleetMemberAPI.class, (method, args) -> {
                if (method.equals("getHullId")) return "chief_navigator_combat_guard_warden";
                if (method.equals("getHullSpec")) return spec;
                if (method.equals("getVariant")) return variant;
                if (method.equals("getShipName")) return name;
                if (method.equals("isFighterWing")) return fighter;
                if (method.equals("isStation")) return station;
                if (method.equals("setVariant")) variant = (ShipVariantAPI) args[0];
                return null;
            });
        }

        ShipVariantAPI variant() {
            return mock(ShipVariantAPI.class, (name, args) -> {
                if (name.equals("hasHullMod")) return HullMods.AUTOMATED.equals(args[0])
                        || permanent.contains(args[0]);
                if (name.equals("hasTag")) return tags.contains(args[0]);
                if (name.equals("clone")) { clones++; return variant(); }
                if (name.equals("addPermaMod")) permanent.add((String) args[0]);
                if (name.equals("addTag")) tags.add((String) args[0]);
                return null;
            });
        }
    }

    static final class Fixture {
        final Map<String, Object> local = new HashMap<>();
        final Map<String, Object> patrolState = new HashMap<>();
        final List<FleetMemberAPI> drones = new ArrayList<>();
        final List<FleetMemberAPI> ships = new ArrayList<>();
        final List<Member> members = new ArrayList<>();
        final CampaignFleetAPI patrol;
        final CampaignFleetAPI player;
        final InteractionDialogAPI dialog;
        StarSystemAPI location;
        int points = 5, spent, echoes, visual;
        boolean exists = true, transitioning, failAdd;
        String id = "chief_navigator_unsealed_guard_0";

        Fixture() {
            Global.setSettings(mock(SettingsAPI.class,
                    (name, args) -> name.equals("getBonusXP") ? 0f : null));
            MemoryAPI marker = memory(patrolState);
            patrolState.put(MenelausTrial.FRIENDLY_DRONE_PATROL_MARKER, true);
            FleetDataAPI source = mock(FleetDataAPI.class, (name, args) -> {
                if (name.equals("getMembersListCopy")) return new ArrayList<>(drones);
                if (name.equals("removeFleetMember")) drones.remove(args[0]);
                if (name.equals("addFleetMember")) drones.add((FleetMemberAPI) args[0]);
                return null;
            });
            FleetDataAPI destination = mock(FleetDataAPI.class, (name, args) -> {
                if (name.equals("getMembersListCopy")) return new ArrayList<>(ships);
                if (name.equals("addFleetMember")) {
                    if (failAdd) { failAdd = false; throw new IllegalStateException("test transfer failure"); }
                    ships.add((FleetMemberAPI) args[0]);
                }
                if (name.equals("removeFleetMember")) ships.remove(args[0]);
                return null;
            });
            player = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("isPlayerFleet")) return true;
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getFleetData")) return destination;
                return null;
            });
            patrol = mock(CampaignFleetAPI.class, (name, args) -> {
                if (name.equals("getId")) return id;
                if (name.equals("getMemoryWithoutUpdate")) return marker;
                if (name.equals("getContainingLocation")) return location;
                if (name.equals("getFleetData")) return source;
                if (name.equals("isInHyperspaceTransition")) return transitioning;
                return null;
            });
            location = mock(StarSystemAPI.class, (name, args) ->
                    name.equals("getEntityById") && exists ? patrol : null);
            MutableCharacterStatsAPI stats = mock(MutableCharacterStatsAPI.class, (name, args) -> {
                if (name.equals("getStoryPoints")) return points;
                if (name.equals("addStoryPoints")) points += ((Number) args[0]).intValue();
                if (name.equals("spendStoryPoints")) {
                    int cost = ((Number) args[0]).intValue();
                    check(cost <= points, "Native payment must not overdraw story points");
                    points -= cost;
                    spent += cost;
                }
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getPlayerFleet")) return player;
                if (name.equals("getPlayerStats")) return stats;
                if (name.equals("getMemoryWithoutUpdate")) return memory(
                        new HashMap<>(Map.of(MenelausTrial.ACCEPTED, true)));
                return null;
            }));
            VisualPanelAPI panel = mock(VisualPanelAPI.class, (name, args) -> {
                if (name.equals("showFleetMemberInfo")) visual++;
                return null;
            });
            dialog = mock(InteractionDialogAPI.class, (name, args) -> {
                if (name.equals("getVisualPanel")) return panel;
                if (name.equals("addOptionSelectedText")) echoes++;
                return null;
            });
            for (int i = 0; i < 3; i++) {
                Member member = new Member("Drone " + i);
                members.add(member);
                drones.add(member.api);
            }
        }

        ChiefNavigatorDronePatrolCMD.RecruitmentAction action() {
            return new ChiefNavigatorDronePatrolCMD.RecruitmentAction(dialog,
                    new StoryOptionParams("recruit", 1, "historianBP", "technology", "recruit"),
                    patrol, memory(local));
        }

        void confirm(ChiefNavigatorDronePatrolCMD.RecruitmentAction action) {
            // Verified native sequence: preConfirm, re-read cost, spend, confirm.
            action.preConfirm();
            Global.getSector().getPlayerStats().spendStoryPoints(
                    action.getRequiredStoryPoints(), false, null, false,
                    action.getBonusXPFraction(), action.getLogText());
            action.confirm();
        }
    }

    public static void main(String[] args) {
        Fixture normal = new Fixture();
        ChiefNavigatorDronePatrolCMD.RecruitmentAction first = normal.action();
        check(first.getRequiredStoryPoints() == 1, "Confirmation must advertise exactly one point");
        check(normal.points == 5 && normal.drones.size() == 3 && normal.ships.isEmpty(),
                "Opening or canceling confirmation must have no effects");
        normal.confirm(first);
        check(normal.points == 4 && normal.spent == 1 && normal.echoes == 1,
                "Confirmed selection must cost once and echo once");
        check(normal.drones.size() == 2 && normal.ships.size() == 1
                        && normal.members.stream().anyMatch(m -> m.api == normal.ships.get(0)),
                "Recruited ship must be the original member removed from the source");
        Member joined = normal.members.stream().filter(m -> m.api == normal.ships.get(0)).findFirst().get();
        check(joined.clones == 1 && joined.permanent.contains(DomainSecurityIFFHullmod.HULLMOD_ID)
                        && joined.tags.contains(Tags.TAG_AUTOMATED_NO_PENALTY),
                "Transfer must attach normal permanent IFF/no-penalty authorization");
        check(Boolean.TRUE.equals(normal.local.get("$chiefNavigatorDroneRecruited")),
                "Result rule must receive successful transfer state");
        normal.confirm(first);
        check(normal.points == 4 && normal.ships.size() == 1 && normal.echoes == 1,
                "Stale duplicate confirmation must not spend or transfer twice");
        normal.confirm(normal.action());
        normal.confirm(normal.action());
        check(normal.drones.isEmpty() && normal.ships.size() == 3 && normal.points == 2,
                "Each remaining original drone may be recruited for its own story point");
        normal.confirm(normal.action());
        check(normal.points == 2 && normal.ships.size() == 3,
                "An exhausted patrol must not charge for or manufacture another drone");

        Fixture latePoints = new Fixture();
        ChiefNavigatorDronePatrolCMD.RecruitmentAction noPoints = latePoints.action();
        latePoints.points = 0;
        latePoints.confirm(noPoints);
        check(latePoints.spent == 0 && latePoints.ships.isEmpty(),
                "Confirmation must recheck points rather than trust opening state");
        Fixture vanished = new Fixture();
        vanished.exists = false;
        vanished.confirm(vanished.action());
        check(vanished.spent == 0 && vanished.drones.size() == 3 && vanished.ships.isEmpty(),
                "Missing source patrol must fail without spending or moving ships");
        Fixture inTransit = new Fixture();
        inTransit.transitioning = true;
        inTransit.confirm(inTransit.action());
        check(inTransit.spent == 0 && inTransit.ships.isEmpty(), "Transition must defer recruitment");
        Fixture failure = new Fixture();
        failure.failAdd = true;
        failure.confirm(failure.action());
        check(failure.points == 5 && failure.drones.size() == 3 && failure.ships.isEmpty()
                        && !Boolean.TRUE.equals(failure.local.get("$chiefNavigatorDroneRecruited")),
                "Failed transfer must roll back the member and refund exactly one point");
        Fixture foreign = new Fixture();
        foreign.id = "some_other_guard_fleet";
        check(!MenelausTrial.isFriendlyDronePatrol(foreign.patrol),
                "A marker alone must not enroll a foreign fleet");
        foreign.id = "chief_navigator_unsealed_guard_0";
        foreign.patrolState.clear();
        check(!MenelausTrial.isFriendlyDronePatrol(foreign.patrol),
                "Namespace alone must not enroll an unmarked fleet");
        Fixture random = new Fixture();
        random.members.get(0).fighter = true;
        random.members.get(1).station = true;
        check(DronePatrolRecruitment.chooseRandom(random.patrol, new Random(2))
                        == random.members.get(2).api,
                "Random selection must exclude fighters and stations");
        System.out.println("Friendly drone recruitment regression checks passed.");
    }
}
