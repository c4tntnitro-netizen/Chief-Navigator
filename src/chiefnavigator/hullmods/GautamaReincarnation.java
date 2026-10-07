package chiefnavigator.hullmods;

import chiefnavigator.quest.BudaiMusic;
import chiefnavigator.quest.FinalLaborMusic;
import chiefnavigator.quest.OdysseyPredatorScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.histidine.chatter.combat.ChatterCombatPlugin;
import org.lwjgl.util.vector.Vector2f;

/**
 * Owns Gautama's THREAT construction roll.
 *
 * <p>An ordinary Starving Threat death has a small chance to begin the THREAT
 * construction animation when that battle has never contained Gautama. Labor
 * V's center battle deliberately permits repeated incarnations: each failed
 * eligible death adds ten percentage points to the next hull's size-based
 * chance, capped at certainty, and a successful construction resets the pity
 * cycle. The permanent Last Light rematch remains explicitly suppressed.</p>
 */
public final class GautamaReincarnation {
    private static final String STATE_KEY =
            "chief_navigator_gautama_reincarnation_state";
    private static final String SUPPRESSED_KEY =
            "chief_navigator_gautama_reincarnation_retired";
    private static final String FINAL_LABOR_KEY =
            "chief_navigator_gautama_reincarnation_final_labor";
    private static final String LEGACY_GAUTAMA_HULL_ID =
            "chief_navigator_scylla";
    private static final String FINAL_GAUTAMA_HULL_ID =
            "chief_navigator_scylla_final";
    private static final String REINCARNATED_GAUTAMA_HULL_ID =
            "chief_navigator_scylla_reincarnating";
    private static final String GAUTAMA_VARIANT_ID =
            "chief_navigator_scylla_reincarnating_Fabricator";
    private static final String GAUTAMA_ATTACK_SWARM_WING_ID =
            "chief_navigator_scylla_attack_swarm_wing";
    private static final int REINCARNATION_SWARM_COUNT = 20;
    private static final float REINCARNATION_SWARM_INTERVAL = 1.25f;
    private static final float SWARM_LAUNCH_SPEED = 160f;
    private static final float REINCARNATING_POPUP_SIZE = 24f;
    private static final float FINAL_LABOR_PITY_PER_FAILED_KILL = 0.10f;
    private static final int FINAL_LABOR_MAX_PITY_KILLS = 10;
    private static final String REINCARNATION_WARNING =
            "WARNING: Reincarnation\n\nRetreat Advised.";
    private static final String FINAL_LABOR_REINCARNATION_WARNING =
            "WARNING: Multiple Reincarnations Detected";
    private static final Color REINCARNATING_POPUP_COLOR =
            new Color(205, 170, 255, 255);

    private GautamaReincarnation() { }

    /** True while this battle may begin a Gautama construction. */
    static boolean canBegin(CombatEngineAPI engine) {
        if (engine == null || isSuppressed(engine)
                || OdysseyPredatorScript.isGautamaFinalRematchDefeated()) {
            return false;
        }
        try {
            State state = getState(engine);
            refreshFromBattlefield(engine, state);
            return !state.pending && state.gautama == null
                    && (isFinalLaborMode(engine) || !state.ended);
        } catch (RuntimeException failure) {
            // A third-party combat object must not turn one failed
            // reconstruction probe into a battle-ending exception.
            Global.getLogger(GautamaReincarnation.class).error(
                    "Could not inspect Gautama reconstruction state; "
                            + "suppressing this roll",
                    failure);
            return false;
        }
    }

    /** Begins the historical THREAT construction animation at a dead hull. */
    static void begin(CombatEngineAPI engine, ShipAPI source) {
        beginInternal(engine, source);
    }

    /**
     * Resolves one eligible Starving Threat death.
     *
     * <p>In Labor V, {@code failedKills} means prior failed rolls: the first
     * death uses its unmodified hull-size chance, the second receives +10
     * points, and so on. Failed construction caused by incompatible mod data
     * also advances pity after its partial entities are cleaned up.</p>
     */
    static void attemptReincarnation(
            CombatEngineAPI engine, ShipAPI source, float baseChance) {
        if (engine == null || source == null
                || !Float.isFinite(baseChance) || baseChance <= 0f) {
            return;
        }
        try {
            int owner = source.getOriginalOwner();
            if (owner != 0 && owner != 1) return;
            if (!canBegin(engine)) return;
            boolean finalLabor = isFinalLaborMode(engine);
            State state = getState(engine);
            float chance = finalLabor
                    ? getFinalLaborReincarnationChance(
                            baseChance, state.finalLaborFailedKills)
                    : clampChance(baseChance);
            if (Math.random() >= chance) {
                if (finalLabor) recordFinalLaborPlayerKill(engine, source);
                return;
            }

            if (beginInternal(engine, source)) {
                if (finalLabor) {
                    resetFinalLaborPityAfterSuccessfulConstruction(engine);
                }
            } else if (finalLabor) {
                // The destroyed hull still counts toward pity even if a
                // third-party variant makes this particular build fail.
                recordFinalLaborPlayerKill(engine, source);
            }
        } catch (RuntimeException failure) {
            // StarvingThreatHullmod runs from a ship callback; never leak a
            // modded hull/API failure into the combat engine.
            Global.getLogger(GautamaReincarnation.class).error(
                    "Could not resolve Gautama reincarnation roll; "
                            + "leaving Gautama absent",
                    failure);
        }
    }

    private static boolean beginInternal(
            CombatEngineAPI engine, ShipAPI source) {
        if (engine == null || source == null || !canBegin(engine)) return false;

        int owner = source.getOriginalOwner();
        if (owner != 0 && owner != 1) return false;
        CombatFleetManagerAPI manager = engine.getFleetManager(owner);
        if (manager == null) return false;

        State state = getState(engine);
        state.pending = true;
        boolean suppressed = manager.isSuppressDeploymentMessages();
        ShipAPI constructionSwarm = null;
        ShipAPI constructedGautama = null;
        ThreatShipConstructionScript construction = null;
        ReincarnationSwarmDeployment swarmDeployment = null;
        try {
            manager.setSuppressDeploymentMessages(true);
            constructionSwarm = manager.spawnShipOrWing(
                    SwarmLauncherEffect.CONSTRUCTION_SWARM_WING,
                    new Vector2f(source.getLocation()),
                    source.getFacing(),
                    0f,
                    null);
            if (constructionSwarm == null) {
                state.pending = false;
                return false;
            }

            constructionSwarm.setOwner(owner);
            constructionSwarm.setShipAI(null);
            if (constructionSwarm.getWing() != null) {
                constructionSwarm.getWing().setSourceShip(null);
            }
            constructionSwarm.setDoNotRender(true);
            constructionSwarm.setExplosionScale(0f);
            constructionSwarm.setHulkChanceOverride(0f);
            constructionSwarm.setImpactVolumeMult(
                    SwarmLauncherEffect.IMPACT_VOLUME_MULT);
            constructionSwarm.getArmorGrid().clearComponentMap();

            ShipVariantAPI variant = Global.getSettings().getVariant(
                    GAUTAMA_VARIANT_ID);
            float deploymentPoints = variant.getHullSpec()
                    .getSuppliesToRecover();
            int fragments = ConstructionSwarmSystemScript.getFragmentCost(
                    deploymentPoints, variant.getHullSize());
            RoilingSwarmEffect swarm = FragmentSwarmHullmod.createSwarmFor(
                    constructionSwarm);
            swarm.getParams().flashFringeColor =
                    VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;
            RoilingSwarmEffect.getFlockingMap().remove(
                    swarm.getParams().flockingClass, swarm);
            swarm.getParams().flockingClass =
                    FragmentSwarmHullmod.CONSTRUCTION_SWARM_FLOCKING_CLASS;
            RoilingSwarmEffect.getFlockingMap().add(
                    swarm.getParams().flockingClass, swarm);
            swarm.getParams().maxOffset *= 4f;
            swarm.getParams().initialMembers = fragments;
            swarm.getParams().baseMembersToMaintain = fragments;
            swarm.addMembers(Math.max(
                    0, fragments - swarm.getNumActiveMembers()));

            float constructionTime =
                    ConstructionSwarmSystemScript.BASE_CONSTRUCTION_TIME
                    + deploymentPoints
                    * ConstructionSwarmSystemScript.CONSTRUCTION_TIME_DP_MULT;
            construction = new GautamaConstruction(
                            GAUTAMA_VARIANT_ID,
                            constructionSwarm,
                            0f,
                            constructionTime);
            constructedGautama = construction.getShip();
            state.gautama = constructedGautama;
            constructedGautama.setAlphaMult(0f);
            engine.addFloatingText(
                    new Vector2f(source.getLocation()),
                    "REINCARNATING!",
                    REINCARNATING_POPUP_SIZE,
                    REINCARNATING_POPUP_COLOR,
                    source,
                    0.25f,
                    0.5f);
            engine.addPlugin(construction);
            swarmDeployment = new ReincarnationSwarmDeployment(
                    engine, constructedGautama, owner, constructionTime);
            engine.addPlugin(swarmDeployment);
            state.pending = false;

            maintainBattleMusic(engine);
            showReincarnationWarning(engine, constructedGautama);
            return true;
        } catch (RuntimeException ex) {
            if (swarmDeployment != null) {
                swarmDeployment.cancel();
            }
            cleanupFailedConstruction(
                    engine, construction, constructedGautama,
                    constructionSwarm);
            state.gautama = null;
            state.ended = false;
            Global.getLogger(GautamaReincarnation.class).error(
                    "Could not construct Gautama; cleaned partial combat entities",
                    ex);
            return false;
        } finally {
            state.pending = false;
            try {
                manager.setSuppressDeploymentMessages(suppressed);
            } catch (RuntimeException ignored) {
                // Construction state has already been released. A modded
                // fleet manager failing to restore UI messages is local.
            }
        }
    }

    /**
     * Reuses Combat Chatter's smooth hostile-fleet flash after an actual build.
     * Labor V warns once for the entire battle, not once per pity cycle.
     */
    private static void showReincarnationWarning(
            CombatEngineAPI engine, ShipAPI gautama) {
        if (engine == null || gautama == null) return;
        try {
            String warning = claimSuccessfulConstructionWarning(engine, gautama);
            if (warning == null) return;
            ChatterCombatPlugin chatter = ChatterCombatPlugin.getInstance();
            if (chatter == null) return;
            GautamaChatterBridge.showWarning(chatter, gautama, warning);
        } catch (RuntimeException failure) {
            // The warning is presentation only; never unwind a successful
            // Gautama construction because another UI plugin failed.
            Global.getLogger(GautamaReincarnation.class).error(
                    "Could not display Combat Chatter's Gautama warning",
                    failure);
        }
    }

    /** Called only after the ship and both construction plugins were installed. */
    private static String claimSuccessfulConstructionWarning(
            CombatEngineAPI engine, ShipAPI gautama) {
        if (engine == null || gautama == null || isSuppressed(engine)) return null;
        State state = getState(engine);
        if (state.pending || state.gautama != gautama) return null;
        if (isFinalLaborMode(engine)) {
            if (state.finalLaborWarningShown) return null;
            state.finalLaborWarningShown = true;
            return FINAL_LABOR_REINCARNATION_WARNING;
        }
        if (state.ordinaryWarningShown) return null;
        state.ordinaryWarningShown = true;
        return REINCARNATION_WARNING;
    }

    /** Compile-time-safe access to Combat Chatter's protected setup record. */
    private static final class GautamaChatterBridge
            extends ChatterCombatPlugin {
        private static void showWarning(
                ChatterCombatPlugin chatter, ShipAPI gautama, String text) {
            FleetIntroSetupData warning = new FleetIntroSetupData(
                    null,
                    text,
                    null,
                    null,
                    false,
                    Factions.THREAT,
                    // Chatter ORs hull-level MagicSettings static flags into
                    // hasStatic; no flagship lookup keeps this warning smooth.
                    null,
                    false,
                    System.identityHashCode(gautama));
            chatter.generateFleetIntro(warning);
        }
    }

    /** Enables Labor V's repeatable, pity-backed reconstruction cycle. */
    public static void enableFinalLaborMode(CombatEngineAPI engine) {
        if (engine == null) return;
        engine.getCustomData().put(FINAL_LABOR_KEY, Boolean.TRUE);
        // A center-fight plugin may be installed after a generic suppression
        // hook. The explicit Labor V route owns this battle's policy.
        engine.getCustomData().remove(SUPPRESSED_KEY);
        State state = getState(engine);
        state.ended = false;
    }

    /** Prevents all reconstruction rolls for this combat instance. */
    public static void suppressForBattle(CombatEngineAPI engine) {
        if (engine != null) {
            engine.getCustomData().put(SUPPRESSED_KEY, Boolean.TRUE);
        }
    }

    /** Current accumulated Labor V pity bonus, in [0, 1]. */
    public static float getFinalLaborPityChance(CombatEngineAPI engine) {
        if (!isFinalLaborMode(engine) || isSuppressed(engine)) return 0f;
        return getFinalLaborPityChanceForKills(
                getState(engine).finalLaborFailedKills);
    }

    /** Ten points per prior failed eligible death, capped at 100%. */
    static float getFinalLaborPityChanceForKills(int failedKills) {
        int boundedKills = Math.max(
                0, Math.min(FINAL_LABOR_MAX_PITY_KILLS, failedKills));
        return boundedKills * FINAL_LABOR_PITY_PER_FAILED_KILL;
    }

    static float getFinalLaborReincarnationChance(
            float baseChance, int priorFailedKills) {
        return clampChance(baseChance
                + getFinalLaborPityChanceForKills(priorFailedKills));
    }

    /** Number of prior failed eligible Labor V rolls in the current cycle. */
    public static int getFinalLaborPityKills(CombatEngineAPI engine) {
        if (!isFinalLaborMode(engine) || isSuppressed(engine)) return 0;
        return getState(engine).finalLaborFailedKills;
    }

    /** Called only after the construction plugin and its ship both exist. */
    static void resetFinalLaborPityAfterSuccessfulConstruction(
            CombatEngineAPI engine) {
        if (isFinalLaborMode(engine) && !isSuppressed(engine)) {
            getState(engine).finalLaborFailedKills = 0;
        }
    }

    public static boolean isGautamaAlive(CombatEngineAPI engine) {
        if (engine == null) return false;
        for (ShipAPI ship : engine.getShips()) {
            if (isGautama(ship) && ship.isAlive() && !ship.isHulk()) {
                return true;
            }
        }
        return false;
    }

    /** Records one failed eligible Labor V death for the next roll. */
    public static boolean recordFinalLaborPlayerKill(
            CombatEngineAPI engine, ShipAPI source) {
        if (!isFinalLaborMode(engine) || isSuppressed(engine)
                || source == null) {
            return false;
        }
        try {
            int owner = source.getOriginalOwner();
            if ((owner != 0 && owner != 1)
                    || StarvingThreatHullmod
                            .getGautamaReincarnationChance(source) <= 0f
                    || !canBegin(engine)) {
                return false;
            }
            State state = getState(engine);
            state.finalLaborFailedKills = Math.min(
                    FINAL_LABOR_MAX_PITY_KILLS,
                    state.finalLaborFailedKills + 1);
            return true;
        } catch (RuntimeException failure) {
            Global.getLogger(GautamaReincarnation.class).error(
                    "Could not record Labor V reincarnation pity; "
                            + "ignoring incompatible source hull",
                    failure);
            return false;
        }
    }

    static boolean shouldStartReincarnationMusic(
            boolean finalLabor, boolean gospelPlaying) {
        return !finalLabor && !gospelPlaying;
    }

    /** Also covers a Gautama already deployed when the player joins a defense. */
    public static void maintainBattleMusic(
            CombatEngineAPI engine, ShipAPI gautama) {
        if (engine == null || !isGautama(gautama)
                || !gautama.isAlive() || gautama.isHulk()) return;
        maintainBattleMusic(engine);
    }

    private static void maintainBattleMusic(CombatEngineAPI engine) {
        if (!shouldStartReincarnationMusic(
                isFinalLaborMode(engine), FinalLaborMusic.isGospelPlaying())) return;
        State state = getState(engine);
        if (!state.musicStarted) {
            state.musicStarted = true;
            BudaiMusic.playGautamaReincarnation();
        }
        // The shared cue controller retries once after startup, not every
        // frame. Construction and the live hull share this battle-local latch.
        BudaiMusic.maintainBattleMusic();
    }

    /** Ends an ordinary cycle, or opens a fresh Labor V pity cycle. */
    static void noteGautamaDeath(CombatEngineAPI engine, ShipAPI gautama) {
        if (engine == null || !isGautama(gautama)) return;
        State state = getState(engine);
        // ScyllaBossHullmod observes hulks every frame. Record each physical
        // incarnation once so an old hulk cannot repeatedly disturb a later
        // construction cycle.
        if (!state.notedGautamaDeaths.add(gautama)) return;
        state.pending = false;
        if (isFinalLaborMode(engine) && !isSuppressed(engine)) {
            state.ended = false;
            // A late notification from an old hulk must not clear a distinct
            // Gautama currently alive or held by its construction plugin.
            if (state.gautama == null || state.gautama == gautama) {
                state.gautama = null;
            }
        } else {
            state.ended = true;
            state.gautama = gautama;
        }
    }

    private static boolean isSuppressed(CombatEngineAPI engine) {
        return engine != null && Boolean.TRUE.equals(
                engine.getCustomData().get(SUPPRESSED_KEY));
    }

    private static boolean isFinalLaborMode(CombatEngineAPI engine) {
        return engine != null && Boolean.TRUE.equals(
                engine.getCustomData().get(FINAL_LABOR_KEY));
    }

    private static float clampChance(float chance) {
        if (!Float.isFinite(chance)) return 0f;
        return Math.max(0f, Math.min(1f, chance));
    }

    private static void cleanupFailedConstruction(
            CombatEngineAPI engine,
            ThreatShipConstructionScript construction,
            ShipAPI constructedGautama,
            ShipAPI constructionSwarm) {
        if (construction != null) {
            try {
                engine.removePlugin(construction);
            } catch (RuntimeException ignored) {
                // Continue removing combat entities even if plugin cleanup fails.
            }
        }
        removePartialEntity(engine, constructedGautama);
        if (constructionSwarm != null) {
            RoilingSwarmEffect swarm = RoilingSwarmEffect.getSwarmFor(
                    constructionSwarm);
            if (swarm != null) swarm.setForceDespawn(true);
            removePartialEntity(engine, constructionSwarm);
        }
    }

    private static void removePartialEntity(
            CombatEngineAPI engine, ShipAPI ship) {
        if (ship == null) return;
        try {
            if (engine.isEntityInPlay(ship)) engine.removeEntity(ship);
        } catch (RuntimeException ignored) {
            // State is still reset, so a bad cleanup cannot wedge future rolls.
        }
    }

    private static State getState(CombatEngineAPI engine) {
        Object existing = engine.getCustomData().get(STATE_KEY);
        if (existing instanceof State) return (State) existing;
        State state = new State();
        engine.getCustomData().put(STATE_KEY, state);
        return state;
    }

    private static void refreshFromBattlefield(
            CombatEngineAPI engine, State state) {
        boolean hadTrackedGautama = state.gautama != null;
        ShipAPI deadGautama = null;
        for (ShipAPI ship : engine.getShips()) {
            if (!isGautama(ship)) continue;
            if (ship.isAlive() && !ship.isHulk()) {
                state.gautama = ship;
                return;
            }
            deadGautama = ship;
        }

        // A freshly created ship may be held by ThreatShipConstructionScript
        // before engine.getShips() exposes or activates it. Trust the tracked
        // reference until that exact incarnation reports its death; failing
        // closed here is preferable to ever constructing two Gautamas.
        if (state.gautama != null
                && !state.notedGautamaDeaths.contains(state.gautama)) {
            return;
        }

        state.gautama = null;
        if (!isFinalLaborMode(engine)
                && (deadGautama != null || hadTrackedGautama)) {
            state.ended = true;
        }
    }

    private static boolean isGautama(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String hullId = ship.getHullSpec().getHullId();
        return LEGACY_GAUTAMA_HULL_ID.equals(hullId)
                || FINAL_GAUTAMA_HULL_ID.equals(hullId)
                || REINCARNATED_GAUTAMA_HULL_ID.equals(hullId);
    }

    /** Keeps vanilla construction intact but builds Gautama's effective armor linearly. */
    private static final class GautamaConstruction extends ThreatShipConstructionScript {
        private GautamaConstruction(String variantId, ShipAPI source,
                float delay, float fadeInTime) {
            super(variantId, source, delay, fadeInTime);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            super.advance(amount, events);
            if (Global.getCombatEngine().isPaused() || elapsed < delay
                    || elapsed > delay + fadeInTime) return;

            // Reuse vanilla's key so its normal completion removes this too.
            // Use native elapsed time, including its faster fade for destroyed hulls.
            float progress = Math.max(0f, Math.min(1f, (elapsed - delay) / fadeInTime));
            ship.getMutableStats().getEffectiveArmorBonus()
                    .modifyMult("ThreatShipConstructionScript", progress);
        }
    }

    /** Stages the reconstructed Gautama's screen instead of releasing 20 at once. */
    private static final class ReincarnationSwarmDeployment
            extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final ShipAPI gautama;
        private final int owner;
        private final float constructionTimeout;
        private float constructionElapsed;
        private float launchElapsed;
        private int launched;
        private boolean constructionComplete;
        private boolean cancelled;

        private ReincarnationSwarmDeployment(
                CombatEngineAPI engine,
                ShipAPI gautama,
                int owner,
                float constructionTime) {
            this.engine = engine;
            this.gautama = gautama;
            this.owner = owner;
            constructionTimeout = Math.max(5f, constructionTime + 5f);
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (cancelled || engine == null || gautama == null) return;
            if (engine.isPaused() || amount <= 0f) return;

            if (!constructionComplete) {
                constructionElapsed += amount;
                if (isCompletedGautamaInPlay()) {
                    constructionComplete = true;
                } else if (constructionElapsed >= constructionTimeout
                        || gautama.isHulk()) {
                    cancel();
                }
                return;
            }

            if (!engine.isEntityInPlay(gautama)
                    || !gautama.isAlive() || gautama.isHulk()) {
                cancel();
                return;
            }

            launchElapsed += amount;
            if (launchElapsed < REINCARNATION_SWARM_INTERVAL) return;
            launchElapsed -= REINCARNATION_SWARM_INTERVAL;
            deployOneSwarm(launched);
            launched++;
            if (launched >= REINCARNATION_SWARM_COUNT) cancel();
        }

        private boolean isCompletedGautamaInPlay() {
            return engine.isEntityInPlay(gautama)
                    && gautama.isAlive()
                    && !gautama.isHulk()
                    && gautama.getAlphaMult() >= 0.95f
                    && !gautama.hasTag(
                            ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
        }

        private void deployOneSwarm(int sequence) {
            CombatFleetManagerAPI manager = engine.getFleetManager(owner);
            if (manager == null) return;

            float angle = gautama.getFacing() + sequence * 137.50777f;
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(angle);
            Vector2f location = new Vector2f(direction);
            location.scale(Math.max(
                    45f, gautama.getCollisionRadius() * 0.45f));
            Vector2f.add(location, gautama.getLocation(), location);

            boolean suppressed = manager.isSuppressDeploymentMessages();
            try {
                manager.setSuppressDeploymentMessages(true);
                ShipAPI leader = manager.spawnShipOrWing(
                        GAUTAMA_ATTACK_SWARM_WING_ID,
                        location,
                        angle,
                        0f,
                        null);
                if (leader == null || leader.getWing() == null) return;

                FighterWingAPI wing = leader.getWing();
                wing.setSourceShip(null);
                for (ShipAPI member : wing.getWingMembers()) {
                    StarvingThreatHullmod.initializeAttackSwarm(member, owner);
                    FragmentSwarmHullmod.createSwarmFor(member);
                    Vector2f velocity = new Vector2f(direction);
                    velocity.scale(SWARM_LAUNCH_SPEED);
                    Vector2f.add(
                            velocity, gautama.getVelocity(), velocity);
                    member.getVelocity().set(velocity);
                }
                Global.getSoundPlayer().playSound(
                        "threat_swarm_launched",
                        1f,
                        1f,
                        location,
                        gautama.getVelocity());
            } catch (RuntimeException failure) {
                Global.getLogger(GautamaReincarnation.class).error(
                        "Could not deploy a staged Gautama attack swarm",
                        failure);
            } finally {
                try {
                    manager.setSuppressDeploymentMessages(suppressed);
                } catch (RuntimeException ignored) {
                    // The deployment attempt is already isolated to this wing.
                }
            }
        }

        private void cancel() {
            if (cancelled) return;
            cancelled = true;
            if (engine != null) {
                try {
                    engine.removePlugin(this);
                } catch (RuntimeException ignored) {
                    // Expired plugins are harmless if already removed.
                }
            }
        }
    }

    private static final class State {
        private ShipAPI gautama;
        private boolean pending;
        private boolean ended;
        private boolean musicStarted;
        private boolean ordinaryWarningShown;
        private boolean finalLaborWarningShown;
        private int finalLaborFailedKills;
        private final Set<ShipAPI> notedGautamaDeaths =
                Collections.newSetFromMap(
                        new IdentityHashMap<ShipAPI, Boolean>());
    }
}
