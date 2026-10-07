package chiefnavigator.quest;

import chiefnavigator.hullmods.GautamaReincarnation;
import chiefnavigator.hullmods.IthacaSiegeStalemateHullmod;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.RepairTrackerAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;

/** The gate-defense climax: Spartan, three laser stations, and Gautama. */
public final class IthacaFinalLaborBattleCreationPlugin
        extends BattleCreationPluginImpl {
    private static final String GAUTAMA_HULL_ID =
            "chief_navigator_scylla";
    private static final float WAVE_MINIMUM_DELAY = 8f;
    private static final float WAVE_FORCED_DELAY = 50f;
    private static final int WAVE_SURVIVOR_THRESHOLD = 4;

    private List<List<FleetMemberAPI>> finalCenterWaves =
            emptyWaveLists();
    private FleetMemberAPI gautamaMember;
    private boolean waveStagingEnabled;

    @Override
    public void initBattle(
            BattleCreationContext context, MissionDefinitionAPI api) {
        finalCenterWaves = emptyWaveLists();
        gautamaMember = null;
        waveStagingEnabled = false;
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
        StagingPlan staging = prepareStagingPlan(context);
        List<MothballSnapshot> excludedWallModules =
                hideAuthoredWallModules(context);
        List<MothballSnapshot> hidden = new ArrayList<MothballSnapshot>();
        if (staging != null) {
            try {
                for (int wave = 1; wave < staging.waves.size(); wave++) {
                    for (FleetMemberAPI member : staging.waves.get(wave)) {
                        MothballSnapshot snapshot =
                                new MothballSnapshot(member);
                        hidden.add(snapshot);
                        snapshot.hideFromInitialDeployment();
                    }
                }
            } catch (RuntimeException | LinkageError failure) {
                restoreMothballSnapshots(hidden);
                hidden.clear();
                staging = null;
                Global.getLogger(getClass()).warn(
                        "Could not stage Labor V reinforcement waves; "
                                + "falling back to ordinary deployment.",
                        failure);
            }
        }
        try {
            // The campaign fleet and battle preview retain every member. Only
            // the two future cohorts are briefly made ineligible while the
            // mission loader snapshots its initial combat-ready roster.
            super.initBattle(context, api);
        } finally {
            restoreMothballSnapshots(hidden);
            restoreMothballSnapshots(excludedWallModules);
        }
        if (staging != null) {
            finalCenterWaves = staging.waves;
            gautamaMember = staging.gautama;
            waveStagingEnabled = true;
        } else {
            finalCenterWaves = emptyWaveLists();
            gautamaMember = null;
            waveStagingEnabled = false;
        }
        api.initMap(-12000f, 12000f, -9000f, 9000f);
    }

    /**
     * The north, east, and south ring sections are close enough to be pulled
     * into the center campaign battle even while both sides ignore them.
     * Briefly make their exact members ineligible while vanilla snapshots the
     * mission roster, then restore every repair field immediately afterward.
     * This leaves the serialized section fleets untouched while ensuring the
     * broken-ring climax never gains an extra intact Wall module.
     */
    private static List<MothballSnapshot> hideAuthoredWallModules(
            BattleCreationContext context) {
        List<MothballSnapshot> hidden =
                new ArrayList<MothballSnapshot>();
        if (context == null) return hidden;
        BattleAPI battle = context.getOtherFleet() == null
                ? null : context.getOtherFleet().getBattle();
        if (battle == null && context.getPlayerFleet() != null) {
            battle = context.getPlayerFleet().getBattle();
        }
        if (battle == null) return hidden;

        try {
            for (CampaignFleetAPI source : battle.getBothSides()) {
                if (!IthacaSectionEncounter.isSectionStation(source)) {
                    continue;
                }
                for (FleetMemberAPI member : source.getFleetData()
                        .getMembersListCopy()) {
                    MothballSnapshot snapshot = new MothballSnapshot(member);
                    hidden.add(snapshot);
                    snapshot.hideFromInitialDeployment();
                }
            }
        } catch (RuntimeException | LinkageError failure) {
            restoreMothballSnapshots(hidden);
            hidden.clear();
            Global.getLogger(IthacaFinalLaborBattleCreationPlugin.class)
                    .warn("Could not exclude Ithaca ring sections from the "
                            + "Labor V center battle.", failure);
        }
        return hidden;
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        GautamaReincarnation.enableFinalLaborMode(engine);
        FinalLaborMusic.playFinalFight();
        CenterBreachCombatPlugin plugin = new CenterBreachCombatPlugin(
                engine,
                finalCenterWaves,
                gautamaMember,
                waveStagingEnabled);
        plugin.initializeWaveController();
        engine.addPlugin(plugin);
    }

    /** Pure pacing rule shared with the headless regression. */
    static boolean shouldReleaseFinalCenterWave(
            float elapsed, int livingAuthoredSupport) {
        return elapsed >= WAVE_MINIMUM_DELAY
                && (livingAuthoredSupport <= WAVE_SURVIVOR_THRESHOLD
                    || elapsed >= WAVE_FORCED_DELAY);
    }

    private static StagingPlan prepareStagingPlan(
            BattleCreationContext context) {
        if (context == null || context.getOtherFleet() == null) return null;
        List<List<FleetMemberAPI>> waves = emptyWaveLists();
        FleetMemberAPI gautama = null;
        for (FleetMemberAPI member : context.getOtherFleet()
                .getFleetData().getMembersListCopy()) {
            if (member == null) continue;
            if (GAUTAMA_HULL_ID.equals(member.getHullId())) {
                if (gautama != null) return null;
                gautama = member;
                continue;
            }
            int wave = OdysseyPredatorScript.getFinalCenterWaveIndex(
                    member.getId());
            if (wave >= 0 && wave < waves.size()) {
                waves.get(wave).add(member);
            }
        }
        if (gautama == null) return null;
        for (List<FleetMemberAPI> wave : waves) {
            if (wave.size() != OdysseyPredatorScript
                    .getFinalCenterWaveSize()) {
                return null;
            }
            for (FleetMemberAPI member : wave) {
                if (!member.canBeDeployedForCombat()) return null;
            }
        }
        return new StagingPlan(waves, gautama);
    }

    private static List<List<FleetMemberAPI>> emptyWaveLists() {
        List<List<FleetMemberAPI>> result =
                new ArrayList<List<FleetMemberAPI>>();
        for (int wave = 0;
                wave < OdysseyPredatorScript.getFinalCenterWaveCount();
                wave++) {
            result.add(new ArrayList<FleetMemberAPI>());
        }
        return result;
    }

    private static void restoreMothballSnapshots(
            List<MothballSnapshot> snapshots) {
        for (int index = snapshots.size() - 1; index >= 0; index--) {
            snapshots.get(index).restore();
        }
    }

    private static final class StagingPlan {
        private final List<List<FleetMemberAPI>> waves;
        private final FleetMemberAPI gautama;

        private StagingPlan(
                List<List<FleetMemberAPI>> waves,
                FleetMemberAPI gautama) {
            this.waves = waves;
            this.gautama = gautama;
        }
    }

    /** Restores all three mutable repair fields bit-for-bit after loading. */
    private static final class MothballSnapshot {
        private final RepairTrackerAPI tracker;
        private final boolean mothballed;
        private final float cr;
        private final float crPriorToMothballing;

        private MothballSnapshot(FleetMemberAPI member) {
            tracker = member.getRepairTracker();
            mothballed = tracker.isMothballed();
            cr = tracker.getCR();
            crPriorToMothballing = tracker.getCRPriorToMothballing();
        }

        private void hideFromInitialDeployment() {
            tracker.setMothballed(true);
        }

        private void restore() {
            if (tracker.isMothballed() != mothballed) {
                tracker.setMothballed(mothballed);
            }
            tracker.setCR(cr);
            tracker.setCRPriorToMothballing(crPriorToMothballing);
        }
    }

    private static final class CenterBreachCombatPlugin
            extends BaseEveryFrameCombatPlugin {
        private static final String STATION_VARIANT =
                "chief_navigator_spartan_battlestation_Teal";
        private static final String ANCHOR_STAT_ID =
                "chief_navigator_final_labor_station_anchor";
        private static final float STATION_X = 2200f;
        private static final float STATION_Y = 1200f;
        private static final float WAVE_DEPLOYMENT_BURN = 1.5f;

        private final CombatEngineAPI engine;
        private final List<List<FleetMemberAPI>> finalCenterWaves;
        private final FleetMemberAPI gautamaMember;
        private final Map<ShipAPI, Vector2f> stationAnchors =
                new IdentityHashMap<ShipAPI, Vector2f>();
        private CombatFleetManagerAPI invasionManager;
        private Integer invasionOwner;
        private boolean stationsDeployed;
        private boolean waveStagingActive;
        private boolean failOpenPending;
        private int nextWave = 1;
        private float waveElapsed;
        private boolean changedRetreatGuard;

        private CenterBreachCombatPlugin(
                CombatEngineAPI engine,
                List<List<FleetMemberAPI>> finalCenterWaves,
                FleetMemberAPI gautamaMember,
                boolean waveStagingActive) {
            this.engine = engine;
            this.finalCenterWaves = finalCenterWaves;
            this.gautamaMember = gautamaMember;
            this.waveStagingActive = waveStagingActive;
        }

        private void initializeWaveController() {
            invasionManager = engine.getFleetManager(FleetSide.ENEMY);
            if (invasionManager != null) {
                invasionOwner = invasionManager.getOwner();
            }
            if (!waveStagingActive) return;
            if (invasionManager == null
                    || !managerContainsMember(
                            invasionManager, gautamaMember, true)) {
                failOpenWaveController(new IllegalStateException(
                        "Could not locate Gautama on the enemy combat side"));
                return;
            }
            CombatTaskManagerAPI tasks =
                    invasionManager.getTaskManager(false);
            if (tasks != null && !tasks.isPreventFullRetreat()) {
                tasks.setPreventFullRetreat(true);
                changedRetreatGuard = true;
            }
            // Prevent a one-frame victory in the unlikely event the opening
            // cohort is erased before this plugin's first advance call.
            engine.setCombatNotOverForAtLeast(1f);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            // Combat begins paused on the deployment screen; the real-time
            // retry deliberately works before unpausing.
            FinalLaborMusic.maintainFinalFightMusic();
            // Labor V deliberately permits repeated Gautama constructions;
            // its pity state lives for exactly this combat instance.
            GautamaReincarnation.enableFinalLaborMode(engine);
            if (waveStagingActive) {
                try {
                    if (failOpenPending) {
                        retryFailOpenRelease();
                    } else {
                        advanceWaveController(amount);
                    }
                } catch (RuntimeException | LinkageError failure) {
                    failOpenWaveController(failure);
                }
            }
            discoverInvasionOwner();
            if (invasionOwner == null) return;

            int defenderOwner = invasionOwner == 0 ? 1 : 0;
            if (!stationsDeployed) {
                deployBattlestations(defenderOwner);
                stationsDeployed = true;
            }
            configureBattleShips(defenderOwner);
            anchorBattlestations();
        }

        private void discoverInvasionOwner() {
            if (invasionOwner != null) return;
            if (invasionManager == null) {
                invasionManager = engine.getFleetManager(FleetSide.ENEMY);
            }
            if (invasionManager != null
                    && managerContainsMember(
                            invasionManager, gautamaMember, true)) {
                invasionOwner = invasionManager.getOwner();
                return;
            }
            for (ShipAPI ship : engine.getShips()) {
                if (ship == null || ship.getOwner() < 0
                        || ship.getOwner() > 1) continue;
                if (ship.getHullSpec() != null
                        && GAUTAMA_HULL_ID.equals(
                                ship.getHullSpec().getHullId())) {
                    invasionOwner = ship.getOwner();
                    invasionManager = engine.getFleetManager(
                            invasionOwner);
                    return;
                }
            }
        }

        private void advanceWaveController(float amount) {
            if (!hasHeldWaves()) {
                finishWaveController();
                return;
            }
            CombatFleetManagerAPI playerManager =
                    engine.getFleetManager(FleetSide.PLAYER);
            CombatTaskManagerAPI playerTasks = playerManager == null
                    ? null : playerManager.getTaskManager(false);
            if (playerTasks != null && playerTasks.isInFullRetreat()) {
                Global.getLogger(IthacaFinalLaborBattleCreationPlugin.class)
                        .info("Player retreat cancelled unreleased Labor V "
                                + "waves; their campaign members remain "
                                + "uncommitted.");
                // These members never entered the combat manager, so leaving
                // them untouched preserves them in the campaign fleet after
                // withdrawal. Stop refreshing the bounded end hold and let
                // the current one-second window expire naturally.
                finishWaveController();
                return;
            }
            // Held members are intentionally absent from reserves, so this
            // bounded refresh is what prevents a detached swarm or a brief
            // cleared battlefield from ending combat between cohorts.
            engine.setCombatNotOverForAtLeast(1f);
            waveElapsed += Math.max(0f, amount);
            int livingSupport = countLivingReleasedSupport();
            if (!shouldReleaseFinalCenterWave(
                    waveElapsed, livingSupport)) {
                return;
            }
            deployHeldWave(nextWave);
            nextWave++;
            waveElapsed = 0f;
            if (!hasHeldWaves()) {
                // Keep the battle open through the final cohort's shared
                // deployment burn, then return termination to the engine.
                engine.setCombatNotOverForAtLeast(
                        WAVE_DEPLOYMENT_BURN + 0.5f);
                finishWaveController();
            }
        }

        private boolean hasHeldWaves() {
            return waveStagingActive
                    && nextWave < finalCenterWaves.size();
        }

        private int countLivingReleasedSupport() {
            if (invasionManager == null) return Integer.MAX_VALUE;
            int living = 0;
            int releasedWaves = Math.min(
                    nextWave, finalCenterWaves.size());
            for (int wave = 0; wave < releasedWaves; wave++) {
                for (FleetMemberAPI member : finalCenterWaves.get(wave)) {
                    ShipAPI ship = invasionManager.getShipFor(member);
                    if (ship != null && ship.isAlive() && !ship.isHulk()
                            && engine.isEntityInPlay(ship)) {
                        living++;
                    }
                }
            }
            return living;
        }

        private void deployHeldWave(int waveIndex) {
            if (invasionManager == null
                    || waveIndex < 0
                    || waveIndex >= finalCenterWaves.size()) {
                throw new IllegalStateException(
                        "Missing fleet manager or invalid wave index");
            }
            List<FleetMemberAPI> wave = finalCenterWaves.get(waveIndex);
            boolean suppressed =
                    invasionManager.isSuppressDeploymentMessages();
            invasionManager.setSuppressDeploymentMessages(true);
            try {
                float y = invasionManager.getOwner() == 0
                        ? -engine.getMapHeight() * 0.44f
                        : engine.getMapHeight() * 0.44f;
                float facing = invasionManager.getOwner() == 0
                        ? 90f : 270f;
                float span = Math.min(
                        engine.getMapWidth() * 0.7f,
                        Math.max(0f, wave.size() - 1) * 1300f);
                float spacing = wave.size() <= 1
                        ? 0f : span / (wave.size() - 1);
                for (int slot = 0; slot < wave.size(); slot++) {
                    FleetMemberAPI member = wave.get(slot);
                    if (managerContainsMember(
                            invasionManager, member, false)) {
                        continue;
                    }
                    member.setOwner(invasionManager.getOwner());
                    member.setAlly(false);
                    Vector2f location = new Vector2f(
                            -span * 0.5f + spacing * slot,
                            y);
                    ShipAPI spawned = invasionManager.spawnFleetMember(
                            member,
                            location,
                            facing,
                            WAVE_DEPLOYMENT_BURN);
                    if (spawned == null && !managerContainsMember(
                            invasionManager, member, true)) {
                        // Preserve the exact campaign member and let the
                        // vanilla admiral deploy it instead of substituting a
                        // combat-only clone.
                        invasionManager.addToReserves(member);
                    }
                }
            } finally {
                invasionManager.setSuppressDeploymentMessages(suppressed);
            }
            Global.getLogger(IthacaFinalLaborBattleCreationPlugin.class)
                    .info("Labor V mixed reinforcement wave "
                            + (waveIndex + 1) + " deployed with "
                            + wave.size() + " exact campaign members.");
        }

        private void finishWaveController() {
            restoreRetreatGuard();
            failOpenPending = false;
            waveStagingActive = false;
        }

        private void failOpenWaveController(Throwable failure) {
            if (!failOpenPending) {
                Global.getLogger(IthacaFinalLaborBattleCreationPlugin.class)
                        .warn("Labor V wave controller failed; returning "
                                + "every held campaign member to vanilla "
                                + "reserves.", failure);
            }
            failOpenPending = true;
            retryFailOpenRelease();
        }

        /**
         * Retries until every omitted exact member is either already known
         * to the manager or visibly back in its reserve set. Never allow a
         * partial fail-open to make the rest of the campaign fleet vanish
         * from battle accounting.
         */
        private void retryFailOpenRelease() {
            engine.setCombatNotOverForAtLeast(1f);
            if (invasionManager == null) {
                invasionManager = engine.getFleetManager(FleetSide.ENEMY);
            }
            boolean allReturned = invasionManager != null;
            if (invasionManager != null) {
                for (int wave = nextWave;
                        wave < finalCenterWaves.size(); wave++) {
                    for (FleetMemberAPI member
                            : finalCenterWaves.get(wave)) {
                        try {
                            if (!managerContainsMember(
                                    invasionManager, member, true)) {
                                member.setOwner(invasionManager.getOwner());
                                member.setAlly(false);
                                invasionManager.addToReserves(member);
                            }
                            if (!managerContainsMember(
                                    invasionManager, member, true)) {
                                allReturned = false;
                            }
                        } catch (RuntimeException | LinkageError failure) {
                            allReturned = false;
                            // Continue returning every unaffected member and
                            // retry this one on the next combat frame.
                        }
                    }
                }
            }
            if (allReturned) finishWaveController();
        }

        private void restoreRetreatGuard() {
            if (!changedRetreatGuard || invasionManager == null) return;
            CombatTaskManagerAPI tasks =
                    invasionManager.getTaskManager(false);
            if (tasks != null) tasks.setPreventFullRetreat(false);
            changedRetreatGuard = false;
        }

        private static boolean managerContainsMember(
                CombatFleetManagerAPI manager,
                FleetMemberAPI member,
                boolean includeReserves) {
            if (manager == null || member == null) return false;
            if (manager.getShipFor(member) != null) return true;
            for (DeployedFleetMemberAPI deployed
                    : manager.getAllEverDeployedCopy()) {
                if (deployed != null && deployed.getMember() == member) {
                    return true;
                }
            }
            if (includeReserves) {
                for (FleetMemberAPI reserve : manager.getReservesCopy()) {
                    if (reserve == member) return true;
                }
            }
            return false;
        }

        private void deployBattlestations(int defenderOwner) {
            CombatFleetManagerAPI manager = engine.getFleetManager(
                    defenderOwner);
            if (manager == null) return;
            boolean suppressed = manager.isSuppressDeploymentMessages();
            manager.setSuppressDeploymentMessages(true);
            try {
                float y = defenderOwner == 0 ? -STATION_Y : STATION_Y;
                float facing = defenderOwner == 0 ? 90f : 270f;
                spawnBattlestation(manager, defenderOwner,
                        new Vector2f(-STATION_X, y), facing);
                spawnBattlestation(manager, defenderOwner,
                        new Vector2f(STATION_X, y), facing);
            } finally {
                manager.setSuppressDeploymentMessages(suppressed);
            }
        }

        private void spawnBattlestation(
                CombatFleetManagerAPI manager,
                int defenderOwner,
                Vector2f location,
                float facing) {
            ShipAPI station = manager.spawnShipOrWing(
                    STATION_VARIANT, location, facing, 0f, null);
            if (station == null) return;
            station.setOwner(defenderOwner);
            stationAnchors.put(station, new Vector2f(location));
            preventStationTranslation(station);
            configureStationStructure(station);
        }

        private void configureBattleShips(int defenderOwner) {
            for (ShipAPI ship : engine.getShips()) {
                if (ship == null) continue;
                if (ship.getOwner() == defenderOwner
                        && ship.getVariant() != null
                        && OdysseyExpanseSystem
                                .FOB_ITHACA_GATE_DEFENSE_VARIANT_ID.equals(
                                        ship.getVariant()
                                                .getHullVariantId())) {
                    // Includes the persistent Breach Defense Station. The two
                    // combat-only reserve stations are also idempotently
                    // configured through stationAnchors below.
                    configureStationStructure(ship);
                }
                if (ship.getHullSpec() != null
                        && IthacaSectionEncounter.WALL_HULL_ID.equals(
                                ship.getHullSpec().getHullId())) {
                    IthacaSiegeStalemateHullmod.markWallDefender(ship);
                    for (ShipAPI module : ship.getChildModulesCopy()) {
                        IthacaSiegeStalemateHullmod.markWallDefender(module);
                    }
                }
            }
            for (ShipAPI station : stationAnchors.keySet()) {
                configureStationStructure(station);
            }
        }

        private void configureStationStructure(ShipAPI ship) {
            if (ship == null) return;
            markStationStructure(ship);
            for (ShipAPI module : ship.getChildModulesCopy()) {
                markStationStructure(module);
            }
        }

        private void markStationStructure(ShipAPI ship) {
            if (ship == null) return;
            // Rapid hits on the many station modules otherwise create a
            // continuous wall of yellow hull-damage flashes. This native
            // visual flag leaves damage and destruction behavior intact.
            ship.setNoDamagedExplosions(true);
            IthacaSiegeStalemateHullmod.markWallDefender(ship);
        }

        private void anchorBattlestations() {
            for (Map.Entry<ShipAPI, Vector2f> entry
                    : stationAnchors.entrySet()) {
                ShipAPI station = entry.getKey();
                if (station == null || !station.isAlive()) continue;
                station.getLocation().set(entry.getValue());
                station.getVelocity().set(0f, 0f);
                // Axial Rotation owns facing and angular velocity. Keeping
                // these reserves in place must not freeze their exposed sides.
                station.setControlsLocked(false);
            }
        }

        private void preventStationTranslation(ShipAPI ship) {
            ship.getMutableStats().getMaxSpeed().modifyMult(
                    ANCHOR_STAT_ID, 0f);
            ship.getMutableStats().getAcceleration().modifyMult(
                    ANCHOR_STAT_ID, 0f);
            ship.getMutableStats().getDeceleration().modifyMult(
                    ANCHOR_STAT_ID, 0f);
        }

    }
}
