package chiefnavigator.quest;

import com.fs.starfarer.api.FactoryAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignUIAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static chiefnavigator.quest.AlphaOdysseyArrivalRegression.check;
import static chiefnavigator.quest.AlphaOdysseyArrivalRegression.key;
import static chiefnavigator.quest.AlphaOdysseyArrivalRegression.mock;

/** Runs actual Avici population retirement without spawning, moving or rewriting fleets. */
public final class AviciStarvingPopulationRegression {
    private static final class Fixture {
        final AlphaOdysseyArrivalRegression.Memory systemMemory = new AlphaOdysseyArrivalRegression.Memory();
        final List<CampaignFleetAPI> fleets = new ArrayList<>();
        final List<CampaignFleetAPI> removed = new ArrayList<>();
        final List<AlphaOdysseyArrivalRegression.Fleet> states = new ArrayList<>();
        final List<StarSystemAPI> systems = new ArrayList<>();
        final StarSystemAPI avici = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getId") || name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.MESSINA_ID;
            if (name.equals("getMemoryWithoutUpdate")) return systemMemory.api;
            if (name.equals("getFleets")) return fleets;
            if (name.equals("removeEntity")) {
                CampaignFleetAPI fleet = (CampaignFleetAPI) args[0];
                check(fleets.remove(fleet), "Retirement must remove the exact current local fleet once");
                removed.add(fleet);
                for (AlphaOdysseyArrivalRegression.Fleet state : states) {
                    if (state.api == fleet) state.system = null;
                }
            }
            if (name.equals("addEntity") || name.equals("createToken")) {
                throw new AssertionError("Avici cleanup must not create campaign objects");
            }
            return null;
        });
        final StarSystemAPI other = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getId") || name.equals("getOptionalUniqueId")) return OdysseyExpanseSystem.SILENT_WAKE_ID;
            if (name.equals("getFleets")) throw new AssertionError("Avici retirement must not inspect other systems");
            if (name.equals("removeEntity")) throw new AssertionError("Avici retirement must not remove other-system fleets");
            return null;
        });
        final AlphaOdysseyArrivalRegression.Fleet player = new AlphaOdysseyArrivalRegression.Fleet();
        SectorEntityToken interaction;
        final InteractionDialogAPI dialog = mock(InteractionDialogAPI.class, (name, args) ->
                name.equals("getInteractionTarget") ? interaction : null);
        final CampaignUIAPI ui = mock(CampaignUIAPI.class, (name, args) ->
                name.equals("getCurrentInteractionDialog") && interaction != null ? dialog : null);
        final SectorAPI sector = mock(SectorAPI.class, (name, args) -> {
            if (name.equals("getPlayerFleet")) return player.api;
            if (name.equals("getCampaignUI")) return ui;
            if (name.equals("getStarSystems")) return systems;
            if (name.equals("getMemoryWithoutUpdate")) {
                throw new AssertionError("Local Avici retirement must not use sector migration/state flags");
            }
            return null;
        });

        Fixture() throws Exception {
            systems.add(avici);
            systemMemory.values.put(key(OdysseyExpanseSystem.class, "AUTHORED_SYSTEM_MARKER"), true);
            player.id = "player";
            player.system = avici;
            player.memory.values.put(OdysseyPredatorScript.STARVING_THREAT_MARKER, true);
            fleets.add(player.api);
            states.add(player);
            Global.setSector(sector);
        }

        AlphaOdysseyArrivalRegression.Fleet fleet(String id, boolean marked) {
            AlphaOdysseyArrivalRegression.Fleet fleet = new AlphaOdysseyArrivalRegression.Fleet();
            fleet.id = id;
            fleet.system = avici;
            if (marked) fleet.memory.values.put(OdysseyPredatorScript.STARVING_THREAT_MARKER, true);
            fleets.add(fleet.api);
            states.add(fleet);
            return fleet;
        }

        void unchanged() {
            for (AlphaOdysseyArrivalRegression.Fleet fleet : states) {
                check(fleet.mutations() == 0,
                        "Retirement must not rewrite fleet flags, assignments or rosters: " + fleet.id);
            }
            check(systemMemory.mutations == 0, "Retirement must not write generation or migration markers");
        }
    }

    private static void idleAndProtectedIdentities() throws Exception {
        Fixture f = new Fixture();
        AlphaOdysseyArrivalRegression.Fleet idle = f.fleet("old-avici-patrol", true);
        AlphaOdysseyArrivalRegression.Fleet second = f.fleet("owned-avici-strike", true);
        AlphaOdysseyArrivalRegression.Fleet foreign = f.fleet("chief_navigator_avici_starving_patrol_0", false);
        AlphaOdysseyArrivalRegression.Fleet flaggedPlayer = f.fleet("marked-player", true);
        flaggedPlayer.playerFlag = true;
        AlphaOdysseyArrivalRegression.Fleet canonicalBudai = f.fleet(TroyArrivalScript.CHARYBDIS_FLEET_ID, true);
        AlphaOdysseyArrivalRegression.Fleet storyFleet = f.fleet(TroyArrivalScript.STARVING_THREAT_FLEET_ID, true);
        AlphaOdysseyArrivalRegression.Fleet markedBudai = f.fleet("renamed-budai", true);
        markedBudai.memory.values.put(key(OdysseyPredatorScript.class, "BUDAI_HUNTER_MARKER"), true);
        AlphaOdysseyArrivalRegression.Fleet otherLocation = f.fleet("stale-local-list-entry", true);
        otherLocation.system = f.other;
        Map<String, Object> originalIdleFlags = Map.copyOf(idle.memory.values);

        OdysseyPredatorScript.retireAviciStarvingThreat(f.avici);
        check(f.removed.equals(List.of(idle.api, second.api)),
                "Only actually local, idle authored Starving fleets must be retired");
        for (CampaignFleetAPI protectedFleet : List.of(f.player.api, flaggedPlayer.api,
                foreign.api, canonicalBudai.api, markedBudai.api, otherLocation.api)) {
            check(f.fleets.contains(protectedFleet),
                    "Player, foreign, Budai and wrong-containing-location tokens must be preserved");
        }
        check(f.fleets.contains(storyFleet.api),
                "The exact Heavenly/Ungaikyo story token must remain for its existing relocation controller");
        check(idle.memory.values.equals(originalIdleFlags), "Removing a fleet must not rewrite its saved flags");
        OdysseyPredatorScript.retireAviciStarvingThreat(f.avici);
        check(f.removed.size() == 2, "Repeated maintenance must never remove/recreate an already retired fleet");
        f.unchanged();
    }

    private static void busyFleetsAreDeferred() throws Exception {
        for (String reason : List.of("battle", "transition", "dialog", "despawn")) {
            Fixture f = new Fixture();
            AlphaOdysseyArrivalRegression.Fleet fleet = f.fleet("busy-" + reason, true);
            if (reason.equals("battle")) fleet.battle = mock(BattleAPI.class, (name, args) -> null);
            if (reason.equals("transition")) fleet.transitioning = true;
            if (reason.equals("dialog")) f.interaction = fleet.api;
            if (reason.equals("despawn")) fleet.despawning = true;
            OdysseyPredatorScript.retireAviciStarvingThreat(f.avici);
            OdysseyPredatorScript.retireAviciStarvingThreat(f.avici);
            check(f.removed.isEmpty() && f.fleets.contains(fleet.api),
                    "Busy fleet must remain untouched until safe: " + reason);
            fleet.battle = null;
            fleet.transitioning = false;
            fleet.despawning = false;
            f.interaction = f.player.api; // An unrelated dialog must not block safe local retirement.
            OdysseyPredatorScript.retireAviciStarvingThreat(f.avici);
            check(f.removed.equals(List.of(fleet.api)), "Safe fleet must be retired on the next pass: " + reason);
            f.unchanged();
        }
    }

    private static void wrongSystemsFailClosed() throws Exception {
        Fixture f = new Fixture();
        f.fleet("owned-avici-patrol", true);
        OdysseyPredatorScript.retireAviciStarvingThreat(null);
        OdysseyPredatorScript.retireAviciStarvingThreat(f.other);
        StarSystemAPI lookalike = mock(StarSystemAPI.class, (name, args) -> {
            if (name.equals("getId")) return "Avici";
            if (name.equals("getFleets") || name.equals("removeEntity")) {
                throw new AssertionError("A name lookalike must not be scanned or changed");
            }
            return null;
        });
        OdysseyPredatorScript.retireAviciStarvingThreat(lookalike);
        check(f.removed.isEmpty(), "Only the exact authored Avici system ID may be cleaned");
        f.unchanged();
    }

    private static void populationPassNeverRespawnsAvici() throws Exception {
        Fixture f = new Fixture();
        AlphaOdysseyArrivalRegression.Fleet old = f.fleet("existing-starving-population", true);
        Method population = OdysseyPredatorScript.class.getDeclaredMethod("ensureStarvingPopulation");
        population.setAccessible(true);
        try {
            population.invoke(null);
            population.invoke(null);
        } catch (InvocationTargetException failure) {
            if (failure.getCause() instanceof Error error) throw error;
            if (failure.getCause() instanceof Exception exception) throw exception;
            throw failure;
        }
        check(f.removed.equals(List.of(old.api)) && f.fleets.equals(List.of(f.player.api)),
                "Actual repeated population maintenance must retire Avici without spawning replacement patrols");
        f.unchanged();
    }

    private static void failedLaborGateIncursionsSkipAvici() throws Exception {
        String source = Files.readString(Path.of("src/chiefnavigator/quest/OdysseyPredatorScript.java"));
        int start = source.indexOf("private static void ensureGateBreachPopulation()");
        int end = source.indexOf("private static void ensureGateBreachFleet(", start);
        check(start >= 0 && end > start, "Failed-Labor gate-incursion population path must remain present");
        String gatePopulation = source.substring(start, end).replaceAll("\\s+", " ");
        int aviciGuard = gatePopulation.indexOf("OdysseyExpanseSystem.MESSINA_ID.equals(location.getId())) continue;");
        int gateScan = gatePopulation.indexOf("location.getEntitiesWithTag(Tags.GATE)");
        check(aviciGuard >= 0 && gateScan > aviciGuard
                        && gatePopulation.contains("if (!MenelausTrial.isFinalLaborFailed()) return;"),
                "Failed Labor must still populate other gates but skip Avici before scanning/spawning");
    }

    public static void main(String[] args) throws Exception {
        SectorAPI previousSector = Global.getSector();
        SettingsAPI previousSettings = Global.getSettings();
        FactoryAPI previousFactory = Global.getFactory();
        try {
            Global.setSettings(mock(SettingsAPI.class, (name, values) -> name.equals("getColor") ? Color.WHITE : null));
            Global.setFactory(mock(FactoryAPI.class, (name, values) -> {
                throw new AssertionError("Removing Avici Starving Threat must never call factories: " + name);
            }));
            idleAndProtectedIdentities();
            busyFleetsAreDeferred();
            wrongSystemsFailClosed();
            populationPassNeverRespawnsAvici();
            failedLaborGateIncursionsSkipAvici();
            System.out.println("PASS: Avici-only owned Starving retirement, safe repeated/no-spawn population "
                    + "maintenance, busy deferral and untouched player/foreign/Budai/other-system fleets.");
        } finally {
            Global.setSector(previousSector);
            Global.setSettings(previousSettings);
            Global.setFactory(previousFactory);
        }
    }
}
