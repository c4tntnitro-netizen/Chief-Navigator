package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import org.lwjgl.util.vector.Vector2f;
import static chiefnavigator.quest.BudaiSalvageRewardRegression.*;

/** Drives the one-time lifecycle and actual campaign battle result listener. */
public final class BudaiEncounterRegression {
    private static final String SPAWNED = "$chief_navigator_budai_spawned";
    private static final String HUNTER = "$chief_navigator_budai_hunter";
    private static final String AVICI_COMPLETE = "$chief_navigator_sinni_vignette_complete_avici";
    private static final class SpawnRequested extends RuntimeException { }
    private static final class SpawnPositionCaptured extends RuntimeException { }

    private static final class Fixture {
        final Map<String, Object> saved = new HashMap<>();
        final List<FleetMemberAPI> members = new ArrayList<>();
        final List<FleetEventListener> listeners = new ArrayList<>();
        boolean playerInvolved = true;
        boolean playerFleet;
        boolean busy;
        boolean despawning;
        boolean constructForPositionTest;
        int despawns;
        int factories;
        String fleetId = TroyArrivalScript.CHARYBDIS_FLEET_ID;
        SectorEntityToken entity;
        final Vector2f center = new Vector2f(137f, -22000f);
        Vector2f initialPosition;
        final BattleAPI battle = mock(BattleAPI.class, (name, args) -> {
            if (name.equals("isPlayerInvolved")) return playerInvolved;
            if (name.equals("wasFleetDefeated")) {
                throw new AssertionError("Whole-fleet defeat is not proof of Budai's death");
            }
            return null;
        });
        final CampaignFleetAPI fleet;
        final StarSystemAPI avici;
        final OdysseyPredatorScript script = new OdysseyPredatorScript();

        Fixture() {
            Map<String, Object> local = new HashMap<>();
            FleetDataAPI roster = mock(FleetDataAPI.class, (name, args) ->
                    name.equals("getMembersListCopy") ? new ArrayList<>(members) : null);
            fleet = mock(CampaignFleetAPI.class, (name, args) -> {
                switch (name) {
                    case "getId": return fleetId;
                    case "getMemoryWithoutUpdate": return memory(local);
                    case "getFleetData": return roster;
                    case "getEventListeners": return listeners;
                    case "addEventListener": listeners.add((FleetEventListener) args[0]); break;
                    case "isPlayerFleet": return playerFleet;
                    case "getBattle": return busy ? battle : null;
                    case "isDespawning": return despawning;
                    case "despawn": despawns++; despawning = true; entity = null; break;
                    case "setLocation":
                        if (constructForPositionTest) {
                            initialPosition = new Vector2f((Float) args[0], (Float) args[1]);
                            throw new SpawnPositionCaptured();
                        }
                        break;
                    default: break;
                }
                return null;
            });
            local.put(HUNTER, true);
            com.fs.starfarer.api.fleet.RepairTrackerAPI repair = mock(
                    com.fs.starfarer.api.fleet.RepairTrackerAPI.class,
                    (name, args) -> name.equals("getMaxCR") ? 0.7f : null);
            members.add(mock(FleetMemberAPI.class, (name, args) -> {
                if (name.equals("getHullId")) return "chief_navigator_charybdis";
                if (name.equals("getRepairTracker")) return repair;
                return null;
            }));
            SectorEntityToken centerToken = mock(SectorEntityToken.class,
                    (name, args) -> name.equals("getLocation") ? center : null);
            avici = mock(StarSystemAPI.class, (name, args) -> {
                if (name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.MESSINA_ID;
                if (name.equals("getMemoryWithoutUpdate")) return memory(Collections.singletonMap(
                        "$chief_navigator_odyssey_authored_system_v1", true));
                if (name.equals("getEntityById")) return entity;
                if (name.equals("getCenter")) return centerToken;
                if (name.equals("isEnteredByPlayer")) return true;
                if (name.equals("addEntity")) entity = (SectorEntityToken) args[0];
                return null;
            });
            Global.setSector(mock(SectorAPI.class, (name, args) -> {
                if (name.equals("getMemoryWithoutUpdate")) return memory(saved);
                if (name.equals("getStarSystems")) return Collections.singletonList(avici);
                return null;
            }));
            Global.setFactory(mock(FactoryAPI.class, (name, args) -> {
                if (name.equals("createFleetMember")) return members.get(0);
                if (name.equals("createEmptyFleet")) {
                    factories++;
                    if (constructForPositionTest) return fleet;
                    throw new SpawnRequested();
                }
                return null;
            }));
        }

        void maintain() throws Exception {
            Method method = OdysseyPredatorScript.class.getDeclaredMethod("ensureCharybdisHunter");
            method.setAccessible(true);
            method.invoke(script);
        }

        FleetEventListener listener() throws Exception {
            Constructor<?> constructor = Class.forName(
                    "chiefnavigator.quest.OdysseyPredatorScript$BudaiSalvageListener")
                    .getDeclaredConstructor();
            constructor.setAccessible(true);
            return (FleetEventListener) constructor.newInstance();
        }
    }

    public static void main(String[] args) throws Exception {
        Global.setSettings(mock(com.fs.starfarer.api.SettingsAPI.class,
                (name, values) -> name.equals("getColor") ? java.awt.Color.WHITE : null));
        Fixture f = new Fixture();
        check(!OdysseyPredatorScript.isBudaiDefeated(), "A fresh encounter must be available");
        for (int i = 0; i < 10; i++) f.maintain();
        f.saved.put("$chief_navigator_sinni_vignette_complete_alpha_odyssey", true);
        f.saved.put("$chief_navigator_sinni_vignette_complete_black_hole", true);
        f.maintain();
        check(f.factories == 0 && !f.saved.containsKey(SPAWNED),
                "Visiting Avici, opening its scene, or completing other vignettes must not spawn or consume Budai");
        f.saved.put(AVICI_COMPLETE, true);
        try {
            f.maintain();
            throw new AssertionError("Completing Sinni's Avici vignette must request Budai's initial spawn");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            check(expected.getCause() instanceof SpawnRequested,
                    "The fresh encounter must reach only the fleet factory");
        }
        check(f.factories == 1, "Initial creation must be requested once");

        f = new Fixture();
        f.saved.put(AVICI_COMPLETE, true);
        f.constructForPositionTest = true;
        try {
            f.maintain();
            throw new AssertionError("The actual spawner must place Budai before configuring pursuit");
        } catch (java.lang.reflect.InvocationTargetException expected) {
            check(expected.getCause() instanceof SpawnPositionCaptured,
                    "Capture the actual initial placement, not a calculated-but-unused point");
        }
        check(f.factories == 1 && f.entity == f.fleet
                        && Math.abs(Vector2f.sub(f.initialPosition, f.center, null).length() - 100000f) < 0.01f
                        && f.center.equals(new Vector2f(137f, -22000f)),
                "Spawn the sole real fleet exactly 100,000 units from the unmodified Avici center");

        f = new Fixture();
        f.entity = f.fleet;
        f.busy = true;
        f.maintain();
        check(Boolean.TRUE.equals(f.saved.get(SPAWNED)),
                "An existing initial encounter must latch its one-time spawn");
        FleetEventListener listener = f.listener();
        listener.reportBattleOccurred(f.fleet, null, f.battle);
        check(!OdysseyPredatorScript.isBudaiDefeated()
                        && !f.saved.containsKey(BudaiSalvageDialogPlugin.PENDING),
                "Retreat or a loss leaving Budai alive must permit another attempt");
        check(OdysseyPredatorScript.isBudaiBattle(f.fleet),
                "A living Budai must retain the dedicated battle route");
        f.members.clear();
        listener.reportBattleOccurred(f.fleet, null, f.battle);
        check(OdysseyPredatorScript.isBudaiDefeated()
                        && Boolean.TRUE.equals(f.saved.get(BudaiSalvageDialogPlugin.PENDING)),
                "Removing Budai in a player battle must permanently defeat him and queue salvage");
        f.maintain();
        check(f.despawns == 0, "Wait for the battle to close before retiring survivors");
        f.busy = false;
        f.maintain();
        check(f.despawns == 1, "Retire the original encounter after battle");
        for (int i = 0; i < 10; i++) f.maintain();
        check(f.factories == 0 && f.despawns == 1
                        && !OdysseyPredatorScript.isBudaiBattle(f.fleet),
                "Repeated maintenance must never respawn a defeated Budai");

        f = new Fixture();
        f.saved.put(AVICI_COMPLETE, true);
        f.saved.put(SPAWNED, true);
        f.maintain();
        check(f.factories == 0, "A spawned encounter must never be recreated if absent");
        f = new Fixture();
        f.entity = f.fleet;
        f.playerInvolved = false;
        f.members.clear();
        f.listener().reportBattleOccurred(f.fleet, null, f.battle);
        check(OdysseyPredatorScript.isBudaiDefeated()
                        && !f.saved.containsKey(BudaiSalvageDialogPlugin.PENDING),
                "AI destruction must be permanent without awarding player salvage");

        f = new Fixture();
        f.listener().reportFleetDespawnedToListener(f.fleet,
                CampaignEventListener.FleetDespawnReason.OTHER, null);
        check(!OdysseyPredatorScript.isBudaiDefeated(),
                "An ordinary despawn callback must not fabricate a combat victory");
        f.listener().reportFleetDespawnedToListener(f.fleet,
                CampaignEventListener.FleetDespawnReason.DESTROYED_BY_BATTLE, null);
        check(OdysseyPredatorScript.isBudaiDefeated(), "Confirmed battle destruction is permanent");

        f = new Fixture();
        f.entity = f.fleet;
        f.playerFleet = true;
        f.saved.put(OdysseyPredatorScript.BUDAI_DEFEATED, true);
        f.maintain();
        check(f.despawns == 0, "Never retire a player-controlled fleet");
        f.saved.clear();
        f.members.clear();
        f.listener().reportBattleOccurred(f.fleet, null, f.battle);
        check(!OdysseyPredatorScript.isBudaiDefeated(), "Player fleets cannot report Budai's defeat");

        f = new Fixture();
        f.fleetId = "unrelated_fleet";
        f.fleet.getMemoryWithoutUpdate().unset(HUNTER);
        f.members.clear();
        f.listener().reportBattleOccurred(f.fleet, null, f.battle);
        check(!OdysseyPredatorScript.isBudaiDefeated(), "Unrelated fleets cannot report this victory");
        System.out.println("PASS: Avici vignette-gated one-time spawn at 100,000 units, permanent defeat, alive retreat, "
                + "player-only salvage, deferred retirement, and player/foreign fleet guards");
    }
}
