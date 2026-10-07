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
import com.fs.starfarer.api.util.Misc.Token;
import com.fs.starfarer.api.util.Misc.TokenType;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Friendly patrol contacts cannot charge story points or transfer ships. */
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
                if (name.equals("getInteractionTarget")) return patrol;
                if (name.equals("addOptionSelectedText")) echoes++;
                return null;
            });
            for (int i = 0; i < 3; i++) {
                Member member = new Member("Drone " + i);
                members.add(member);
                drones.add(member.api);
            }
        }

    }

    public static void main(String[] args) {
        SettingsAPI previousSettings = Global.getSettings();
        SectorAPI previousSector = Global.getSector();
        try {
            Fixture fixture = new Fixture();
            check(MenelausTrial.isFriendlyDronePatrol(fixture.patrol),
                    "Exercise the exact marked patrol route, rather than an unrelated fleet");
            ChiefNavigatorDronePatrolCMD command = new ChiefNavigatorDronePatrolCMD();
            Map<String, MemoryAPI> memoryMap = Map.of(MemKeys.LOCAL, memory(fixture.local));
            check(command.execute("test", fixture.dialog,
                    List.of(new Token("init", TokenType.LITERAL)), memoryMap),
                    "Friendly contact initialization remains available");
            for (String action : List.of("hasShips", "configureStoryOption", "recruit", "transfer")) {
                check(!command.execute("test", fixture.dialog,
                        List.of(new Token(action, TokenType.LITERAL),
                                new Token("chief_navigator_drone_patrol_recruit", TokenType.LITERAL),
                                new Token("Recruited a drone", TokenType.LITERAL)), memoryMap),
                        "Retired rule actions must not install a payment delegate: " + action);
            }
            check(!DronePatrolRecruitment.isAvailable(fixture.patrol)
                            && DronePatrolRecruitment.getEligibleMembers(fixture.patrol).isEmpty()
                            && DronePatrolRecruitment.chooseRandom(fixture.patrol, new Random(2)) == null
                            && !DronePatrolRecruitment.transfer(fixture.patrol, fixture.members.get(0).api),
                    "Retired direct entry points cannot transfer a real surviving drone");
            check(fixture.points == 5 && fixture.spent == 0
                            && fixture.drones.size() == 3 && fixture.ships.isEmpty()
                            && fixture.members.stream().allMatch(member -> member.clones == 0),
                    "Contact and stale recruitment attempts preserve points, rosters and variants");
            System.out.println("PASS: friendly patrol contact and stale transfer actions cannot spend story points or move drones.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
        }
    }
}