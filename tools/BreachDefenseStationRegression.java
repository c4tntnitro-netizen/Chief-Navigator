package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.ai.CampaignFleetAIAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.*;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;

/** Actual maintenance/creation calls: patrol safety without final-battle immortality. */
public final class BreachDefenseStationRegression {
    private interface Call { Object invoke(String name, Object[] args); }
    private static <T> T mock(Class<T> type, Call call) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type}, (proxy, method, args) -> {
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    Object value = call.invoke(method.getName(), args);
                    if (value != null) return value;
                    Class<?> result = method.getReturnType();
                    if (result == boolean.class) return false;
                    if (result == float.class) return 0f;
                    if (result == int.class) return 0;
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
                case "contains": return values.containsKey(args[0]);
                case "set": values.put((String) args[0], args[1]); break;
                case "unset": values.remove(args[0]); break;
            }
            return null;
        });
    }
    private static final class Fleet {
        String id;
        String name;
        boolean station;
        boolean player;
        boolean transitioning;
        BattleAPI battle;
        int writes;
        final Map<String, Object> flags = new HashMap<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<SectorEntityToken> blocked = new ArrayList<>();
        final MemoryAPI memory = memory(flags);
        final CampaignFleetAIAPI ai = mock(CampaignFleetAIAPI.class, (method, args) -> {
            if (method.equals("doNotAttack")) blocked.add((SectorEntityToken) args[0]);
            return null;
        });
        final FleetDataAPI data = mock(FleetDataAPI.class, (method, args) -> {
            if (method.equals("getMembersListCopy")) return new ArrayList<>(members);
            if (method.equals("addFleetMember")) members.add((FleetMemberAPI) args[0]);
            return null;
        });
        final Vector2f position = new Vector2f();
        final CampaignFleetAPI api = mock(CampaignFleetAPI.class, (method, args) -> {
            switch (method) {
                case "getId": return id;
                case "setId": id = (String) args[0]; break;
                case "getName": return name;
                case "setName": name = (String) args[0]; writes++; break;
                case "isStationMode": return station;
                case "setStationMode": station = (Boolean) args[0]; break;
                case "isPlayerFleet": return player;
                case "isInHyperspaceTransition": return transitioning;
                case "getBattle": return battle;
                case "getMemoryWithoutUpdate": return memory;
                case "getFleetData": return data;
                case "isEmpty": return members.isEmpty();
                case "getAI": return ai;
                case "getLocation": return position;
                case "setFixedLocation": position.set((Float) args[0], (Float) args[1]); break;
            }
            return null;
        });
    }
    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            run();
        } finally {
            Global.setSector(previousSector);
            Global.setFactory(previousFactory);
        }
        System.out.println("PASS: absent/empty breach defender restored before assault only; "
                + "ambient NPCs blocked, finale/player/busy/foreign objects preserved.");
    }
    private static void run() throws Exception {
        Map<String, Object> globalFlags = new HashMap<>();
        MemoryAPI globalMemory = memory(globalFlags);
        Map<String, Object> systemFlags = new HashMap<>();
        systemFlags.put("$chief_navigator_fob_ithaca_gate_defense_created_v1", true);
        systemFlags.put("$chief_navigator_odyssey_ashen_verge_abyss_v1", true);
        MemoryAPI systemMemory = memory(systemFlags);
        Map<String, SectorEntityToken> entities = new HashMap<>();
        List<CampaignFleetAPI> fleets = new ArrayList<>();
        Fleet player = new Fleet();
        player.player = true;
        Global.setSector(mock(SectorAPI.class, (method, callArgs) -> {
            if (method.equals("getMemoryWithoutUpdate")) return globalMemory;
            if (method.equals("getPlayerFleet")) return player.api;
            return null;
        }));
        SectorEntityToken center = mock(SectorEntityToken.class, (method, callArgs) -> {
            if (method.equals("getId")) return IthacaSectionEncounter.FOB_ENTITY_ID;
            if (method.equals("getCustomEntityType")) return "chief_navigator_fob_ithaca_campaign";
            if (method.equals("getMemoryWithoutUpdate")) return mock(MemoryAPI.class,
                    (n, a) -> n.equals("getBoolean") ? true : null);
            if (method.equals("getLocation")) return new Vector2f(4500f, 0f);
            return null;
        });
        entities.put(IthacaSectionEncounter.FOB_ENTITY_ID, center);
        StarSystemAPI system = mock(StarSystemAPI.class, (method, callArgs) -> {
            if (method.equals("getEntityById")) return entities.get(callArgs[0]);
            if (method.equals("getMemoryWithoutUpdate")) return systemMemory;
            if (method.equals("getFleets")) return fleets;
            if (method.equals("addEntity")) {
                CampaignFleetAPI fleet = (CampaignFleetAPI) callArgs[0];
                entities.put(fleet.getId(), fleet);
                fleets.add(fleet);
            }
            return null;
        });
        ShipVariantAPI variant = mock(ShipVariantAPI.class, (n, a) ->
                n.equals("getHullVariantId") ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_VARIANT_ID : null);
        ShipHullSpecAPI hull = mock(ShipHullSpecAPI.class, (n, a) ->
                n.equals("getHullId") ? OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_HULL_ID : null);
        int[] repairs = {0};
        FleetMemberStatusAPI status = mock(FleetMemberStatusAPI.class, (n, a) -> {
            if (n.equals("repairFully")) repairs[0]++;
            return null;
        });
        RepairTrackerAPI repair = mock(RepairTrackerAPI.class, (n, a) ->
                n.equals("getMaxCR") ? 1f : null);
        FleetMemberAPI member = mock(FleetMemberAPI.class, (n, a) -> {
            if (n.equals("getVariant")) return variant;
            if (n.equals("getHullSpec")) return hull;
            if (n.equals("getStatus")) return status;
            if (n.equals("getRepairTracker")) return repair;
            return null;
        });
        int[] creations = {0};
        Fleet[] created = {null};
        Global.setFactory(mock(FactoryAPI.class, (method, callArgs) -> {
            if (method.equals("createEmptyFleet")) {
                creations[0]++;
                created[0] = new Fleet();
                return created[0].api;
            }
            if (method.equals("createFleetMember")) return member;
            return null;
        }));

        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        Fleet station = created[0];
        check(creations[0] == 1 && station.members.size() == 1,
                "Stale created/generated markers must not suppress this absent defender");
        check(station.name.equals("Breach Defense Station")
                        && station.position.x == 2880f && station.position.y == 0f,
                "Rename without moving the authored western breach position");
        check(station.memory.getBoolean(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS),
                "Ambient fleets must ignore the station before the finale");
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(creations[0] == 1 && repairs[0] == 1,
                "Routine maintenance must not duplicate or continuously heal the station");

        Fleet patrol = new Fleet();
        patrol.id = "chief_navigator_ashen_starving_patrol_0";
        Fleet finale = new Fleet();
        finale.id = "chief_navigator_final_labor_heavenly_strike";
        finale.flags.put(OdysseyPredatorScript.ITHACA_FINAL_CENTER_BATTLE_MARKER, true);
        Fleet busy = new Fleet();
        busy.battle = mock(BattleAPI.class, (n, a) -> null);
        fleets.addAll(Arrays.asList(patrol.api, finale.api, player.api, busy.api));
        globalFlags.put("$chief_navigator_ithaca_final_center_created_v1", true);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(!station.memory.getBoolean(MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS),
                "Real final assault must expose the station to normal combat");
        check(patrol.blocked.contains(station.api) && finale.blocked.isEmpty()
                        && player.blocked.isEmpty() && busy.blocked.isEmpty(),
                "Block ambient NPCs, not Heavenly Strike, player, or active battles");

        station.members.clear();
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(station.members.isEmpty() && creations[0] == 1,
                "No resurrection of an empty station during the real assault");
        entities.remove(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(creations[0] == 1, "No recreation of an absent station during the real assault");
        globalFlags.remove("$chief_navigator_ithaca_final_center_created_v1");
        globalFlags.put(MenelausTrial.FINAL_LABOR_FAILED, true);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(creations[0] == 1, "A final defeat must remain irreversible");
        globalFlags.remove(MenelausTrial.FINAL_LABOR_FAILED);

        entities.put(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID, station.api);
        station.transitioning = true;
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(station.members.isEmpty(), "Never rebuild a busy empty station");
        station.transitioning = false;
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(station.members.size() == 1 && creations[0] == 1 && repairs[0] == 2,
                "Restore an idle empty station in place, not as a replacement token");
        station.battle = mock(BattleAPI.class, (n, a) -> null);
        int priorWrites = station.writes;
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(station.writes == priorWrites, "Never mutate an active station battle");
        station.battle = null;

        Fleet incompatible = new Fleet();
        incompatible.id = OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID;
        incompatible.station = true;
        incompatible.members.add(mock(FleetMemberAPI.class, (n, a) -> null));
        entities.put(incompatible.id, incompatible.api);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(incompatible.members.size() == 1 && incompatible.writes == 0
                        && creations[0] == 1,
                "Never rewrite an incompatible nonempty fleet");

        SectorEntityToken foreign = mock(SectorEntityToken.class, (n, a) -> null);
        entities.put(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID, foreign);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(creations[0] == 1 && entities.get(OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID) == foreign,
                "Never overwrite a wrong-type entity at the reserved ID");
        player.id = OdysseyExpanseSystem.FOB_ITHACA_GATE_DEFENSE_STATION_ID;
        player.station = true;
        entities.put(player.id, player.api);
        OdysseyPredatorScript.maintainBreachDefenseStation(system);
        check(player.members.isEmpty() && player.writes == 0,
                "Never adopt or mutate the player fleet");

        String collision = Files.readString(Path.of("src/chiefnavigator/quest/FobIthacaRingCollisionScript.java"));
        check(collision.contains("!breachDefenseChecked || breachDefenseInterval.intervalElapsed()")
                        && collision.contains("maintainBreachDefenseStation(system)"),
                "Apply safety immediately after reload and on bounded maintenance ticks");
        String predator = Files.readString(Path.of("src/chiefnavigator/quest/OdysseyPredatorScript.java"))
                .replace("\r\n", "\n");
        check(predator.contains("gateDefense.getMemoryWithoutUpdate().unset(\n"
                + "                MemFlags.FLEET_IGNORED_BY_OTHER_FLEETS);"),
                "Finale setup must explicitly expose the station before attack orders");
        int finalSetup = predator.indexOf("private static void configureFinalCenterBreach(");
        String setup = predator.substring(finalSetup);
        check(setup.indexOf("gateDefense.getMemoryWithoutUpdate().unset(")
                        < setup.indexOf("fleet.addAssignment("),
                "Remove ambient ignore before ordering the real final assault");
    }
}
