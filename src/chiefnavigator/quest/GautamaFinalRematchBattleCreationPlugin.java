package chiefnavigator.quest;

import chiefnavigator.ai.GuardDroneAI;
import chiefnavigator.hullmods.GautamaMirrorHullmod;
import chiefnavigator.hullmods.GautamaReincarnation;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.DeployedFleetMemberAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI.ShipTypeHints;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.impl.combat.MoteControlScript;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod;
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect;
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/**
 * Ungaikyo's one-time battle. The historical class name is retained because
 * older saves may have serialized the campaign listener that routes here.
 * Ungaikyo calls up as many zero-DP hostile reflections from the player
 * campaign fleet as fit in a separate 240-DP pool, then reconstructs each
 * selected reflection whenever it is destroyed.
 */
public final class GautamaFinalRematchBattleCreationPlugin
        extends BattleCreationPluginImpl {
    public static final String UNGAIKYO_HULL_ID =
            "chief_navigator_ungaikyo";
    public static final String UNGAIKYO_VARIANT_ID =
            "chief_navigator_ungaikyo_Fabricator";
    static final String MIRROR_SOURCE_MEMBER_KEY =
            "chief_navigator_ungaikyo_mirror_source_member";
    static final String MIRROR_PERSONALITY = Personalities.RECKLESS;
    static final float SUMMON_DP_BUDGET = 240f;
    static final float MILITARY_DP_FLOOR = 200f;

    @Override
    public void initBattle(
            BattleCreationContext context, MissionDefinitionAPI api) {
        // BattleCreationPluginImpl consumes objectivesAllowed while defining
        // the map, so these authored context rules must be in place first.
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
        // Retain BattleCreationPluginImpl's normal map sizing and terrain.
        super.initBattle(context, api);
        // Keep them explicit for consumers that inspect the context after map
        // definition as well.
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        GautamaReincarnation.suppressForBattle(engine);
        UngaikyoMusic.playBattle();
        engine.addPlugin(new MirrorFleetController(engine));
    }

    /** Owns the full-fleet opening summon and every later resurrection. */
    private static final class MirrorFleetController
            extends BaseEveryFrameCombatPlugin {
        private static final int MAX_CONCURRENT_RESURRECTIONS = 1;
        private static final float INITIAL_SUMMON_DURATION = 2.25f;
        private static final float RESURRECTION_START_STAGGER = 2.25f;
        private static final float REBUILD_COOLDOWN = 12f;
        private static final float MIN_SPAWN_CLEARANCE = 220f;

        private final CombatEngineAPI engine;
        private final CombatFleetManagerAPI enemyManager;
        private final Map<String, MirrorSlot> slots =
                new LinkedHashMap<String, MirrorSlot>();
        private final Set<ShipAPI> unreadableCombatShips =
                Collections.newSetFromMap(
                        new IdentityHashMap<ShipAPI, Boolean>());

        private ShipAPI ungaikyo;
        private boolean sawUngaikyo;
        private boolean initialSummonStarted;
        private boolean shuttingDown;
        private float elapsed;
        private float nextBuildAt;
        private int spawnSequence;

        private MirrorFleetController(CombatEngineAPI engine) {
            this.engine = engine;
            this.enemyManager = engine.getFleetManager(FleetSide.ENEMY);
            snapshotPlayerFleet();
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            // A second caller can enable final-Labor mode during early combat
            // setup. Reassert suppression so this encounter can never race the
            // old death-and-reconstruction mechanic.
            GautamaReincarnation.suppressForBattle(engine);
            // Removal can fail transiently inside another mod's callback.
            // Retry battle-local ownership records even after shutdown, rather
            // than forgetting detached modules with the dead parent.
            MirrorLifetime.retryPending(engine, enemyManager);
            captureLifetimes();
            refreshUngaikyo();
            UngaikyoMusic.maintainBattleMusic(
                    ungaikyo, !engine.isPaused() && amount > 0f);

            if (sawUngaikyo && !isAlive(ungaikyo)) {
                shutDownMirrors();
                return;
            }
            if (shuttingDown || engine.isPaused() || amount <= 0f
                    || !isAlive(ungaikyo)) {
                return;
            }

            elapsed += amount;
            if (!initialSummonStarted) {
                initialSummonStarted = true;
                summonSelectedPool();
            }
            advanceSlots(amount);
            startEligibleResurrections();
        }

        private void snapshotPlayerFleet() {
            CampaignFleetAPI player = Global.getSector() == null
                    ? null : Global.getSector().getPlayerFleet();
            if (player == null || player.getFleetData() == null) return;
            List<FleetMemberAPI> members =
                    player.getFleetData().getMembersListCopy();
            float spent = snapshotPass(members, false, 0f);
            if (spent < MILITARY_DP_FLOOR) {
                snapshotPass(members, true, spent);
            }
        }

        private float snapshotPass(
                List<FleetMemberAPI> members,
                boolean civilianPass,
                float initialSpent) {
            float spent = initialSpent;
            for (FleetMemberAPI member : members) {
                if (civilianPass && spent >= MILITARY_DP_FLOOR) break;
                String memberId = null;
                try {
                    if (!isEligiblePlayerMember(member)) continue;
                    if (MirrorCompatibility.isMirrorOptedOut(member.getVariant())) {
                        Global.getLogger(GautamaFinalRematchBattleCreationPlugin.class)
                                .info("Skipped opted-out Ungaikyo reflection source "
                                        + member.getId() + ".");
                        continue;
                    }
                    boolean civilian = isCivilianMember(member);
                    if (civilian != civilianPass) continue;
                    memberId = member.getId();
                    if (slots.containsKey(memberId)) continue;
                    float deploymentPoints = Math.max(
                            0f, member.getDeploymentPointsCost());
                    if (!fitsSummonBudget(spent, deploymentPoints)) {
                        continue;
                    }
                    slots.put(memberId, new MirrorSlot(
                            memberId,
                            civilian ? null : member.getShipName(),
                            snapshotForMember(member),
                            deploymentPoints));
                    spent += deploymentPoints;
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    if (memberId != null && !slots.containsKey(memberId)) {
                        MirrorSlot failed = new MirrorSlot(
                                memberId, safeShipName(member), null, 0f);
                        slots.put(memberId, failed);
                        disableSlot(failed, failure);
                    } else {
                        logUnslottedFailure(member,
                                "snapshotting the player fleet", failure);
                    }
                }
            }
            return spent;
        }

        private void refreshUngaikyo() {
            ShipAPI live = null;
            for (ShipAPI ship : new ArrayList<ShipAPI>(engine.getShips())) {
                if (unreadableCombatShips.contains(ship)) continue;
                try {
                    if (!isUngaikyo(ship)) continue;
                    sawUngaikyo = true;
                    if (isAlive(ship)) {
                        live = ship;
                        break;
                    }
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    unreadableCombatShips.add(ship);
                    quarantineCombatShip(ship, failure);
                }
            }
            if (live != null && live != ungaikyo) configureUngaikyoAI(live);
            ungaikyo = live;
        }

        private void quarantineCombatShip(
                ShipAPI ship, Throwable failure) {
            for (MirrorSlot slot : slots.values()) {
                if (slot.lifetime != null && slot.lifetime.contains(ship)) {
                    disableSlot(slot, failure);
                    return;
                }
            }
            try {
                Global.getLogger(GautamaFinalRematchBattleCreationPlugin.class)
                        .warn("Ignored one unreadable combat ship while "
                                + "locating Ungaikyo.", failure);
            } catch (Throwable loggingFailure) {
                rethrowIfFatal(loggingFailure);
            }
        }

        private void advanceSlots(float amount) {
            for (MirrorSlot slot : slots.values()) {
                if (slot.active == null && slot.construction == null
                        && slot.lifetime != null
                        && slot.lifetime.isRemovalComplete(engine)) {
                    slot.lifetime = null;
                }
                if (slot.disabled) continue;
                try {
                    advanceSlot(slot, amount);
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    disableSlot(slot, failure);
                }
            }
        }

        private void captureLifetimes() {
            for (MirrorSlot slot : slots.values()) {
                if (slot.disabled || slot.lifetime == null
                        || (slot.active == null && slot.construction == null)) continue;
                Throwable failure = slot.lifetime.capture(engine, enemyManager);
                if (failure != null) disableSlot(slot, failure);
            }
        }

        private void advanceSlot(MirrorSlot slot, float amount) {
                if (slot.construction != null) {
                    ConstructionResult result =
                            slot.construction.advance(amount);
                    if (result == ConstructionResult.COMPLETE) {
                        slot.active = slot.construction.getMirror();
                        slot.construction = null;
                        slot.hasCompleted = true;
                    } else if (result == ConstructionResult.FAILED) {
                        slot.construction.cancel();
                        slot.construction = null;
                        slot.nextEligibleAt = elapsed + REBUILD_COOLDOWN;
                    }
                }

                if (slot.active != null && !isAliveInPlay(slot.active)) {
                    ShipAPI replacement = slot.lifetime == null ? null
                            : slot.lifetime.findAlivePrimary(engine);
                    if (replacement != null && isAliveInPlay(replacement)) {
                        // A real transformation is not a death. Its custom AI
                        // owns the new hull; never overwrite it or build a
                        // second reflection while this primary is alive.
                        slot.active = replacement;
                        return;
                    }
                    removeCombatOnlyShipBestEffort(engine, enemyManager, slot.active);
                    slot.active = null;
                    slot.nextEligibleAt = elapsed + REBUILD_COOLDOWN;
                }
        }

        private void summonSelectedPool() {
            for (MirrorSlot slot : slots.values()) {
                if (slot.disabled) continue;
                try {
                    slot.construction = beginConstruction(
                            slot, INITIAL_SUMMON_DURATION);
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    disableSlot(slot, failure);
                }
            }
            nextBuildAt = elapsed + INITIAL_SUMMON_DURATION;
        }

        private void startEligibleResurrections() {
            int building = countConcurrentBuilds();
            while (building < MAX_CONCURRENT_RESURRECTIONS
                    && elapsed >= nextBuildAt) {
                MirrorSlot selected = selectEligibleSlot();
                if (selected == null) return;

                // A failed animation spawn must not turn this into a hot loop.
                nextBuildAt = elapsed + RESURRECTION_START_STAGGER;
                try {
                    DynamicMirrorConstruction construction =
                            beginConstruction(selected,
                                    getConstructionTime(
                                            selected.deploymentPoints));
                    if (construction == null) return;
                    selected.construction = construction;
                    building++;
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    disableSlot(selected, failure);
                }
            }
        }

        /** Selects one destroyed opening reflection for reconstruction. */
        private MirrorSlot selectEligibleSlot() {
            for (MirrorSlot slot : slots.values()) {
                if (slot.disabled || slot.active != null
                        || slot.construction != null
                        || slot.lifetime != null
                        || slot.nextEligibleAt > elapsed
                        || !slot.hasCompleted) {
                    continue;
                }
                return slot;
            }
            return null;
        }

        private int countConcurrentBuilds() {
            int count = 0;
            for (MirrorSlot slot : slots.values()) {
                if (!slot.disabled && slot.construction != null) count++;
            }
            return count;
        }

        private DynamicMirrorConstruction beginConstruction(
                MirrorSlot slot, float constructionTime) throws Throwable {
            if (enemyManager == null || !isAlive(ungaikyo)) return null;
            FleetMemberAPI mirror = null;
            ShipAPI ship = null;
            ShipAPI swarmShip = null;
            boolean suppressionKnown = false;
            boolean previousSuppression = false;
            try {
                if (slot.snapshot == null) {
                    throw new IllegalStateException(
                            "No safe variant snapshot for mirror source");
                }
                // Military sources retain the exact player refit. Civilian
                // sources were replaced by a same-size stock THREAT variant
                // while the opening fleet snapshot was taken.
                ShipVariantAPI mirrorVariant = slot.snapshot.clone();
                mirrorVariant.addTag(Tags.UNRECOVERABLE);
                mirrorVariant.addTag(Tags.VARIANT_UNBOARDABLE);
                mirrorVariant.addPermaMod(GautamaMirrorHullmod.HULLMOD_ID);

                mirror = Global.getFactory().createFleetMember(
                        FleetMemberType.SHIP, mirrorVariant);
                if (mirror == null) {
                    throw new IllegalStateException(
                            "Factory returned no mirror fleet member");
                }
                mirror.setOwner(enemyManager.getOwner());
                if (slot.shipName != null) mirror.setShipName(slot.shipName);
                // Apply the cost modifier directly as well. The perma hullmod
                // remains authoritative after the member's next stat refresh.
                mirror.getStats().getSuppliesToRecover().modifyMult(
                        GautamaMirrorHullmod.HULLMOD_ID, 0f);
                mirror.getStats().getDynamic()
                        .getMod(Stats.DEPLOYMENT_POINTS_MOD)
                        .modifyMult(GautamaMirrorHullmod.HULLMOD_ID, 0f);
                mirror.setStatUpdateNeeded(true);
                mirror.updateStats();
                setMemberToFullCR(mirror);

                Vector2f location = findConstructionLocation(
                        mirror.getHullSpec().getCollisionRadius());
                float facing = ungaikyo.getFacing();
                previousSuppression =
                        enemyManager.isSuppressDeploymentMessages();
                suppressionKnown = true;
                enemyManager.setSuppressDeploymentMessages(true);
                ship = enemyManager.spawnFleetMember(
                        mirror, location, facing, 0f);
                if (ship == null) {
                    throw new IllegalStateException(
                            "Fleet manager returned no mirror ship");
                }
                ship.setCustomData(
                        MIRROR_SOURCE_MEMBER_KEY, slot.sourceMemberId);
                slot.lifetime = MirrorLifetime.forShip(ship);
                Throwable ownershipFailure = slot.lifetime.capture(engine, enemyManager);
                if (ownershipFailure != null) throw ownershipFailure;
                initializeConstructingMirror(ship, mirror);

                swarmShip = spawnConstructionSwarm(
                        location, facing, slot.deploymentPoints,
                        mirrorVariant.getHullSize(), ship);
                if (swarmShip == null) {
                    throw new IllegalStateException(
                            "Fleet manager returned no construction swarm");
                }
                DynamicMirrorConstruction result =
                        new DynamicMirrorConstruction(
                        engine, enemyManager, mirror, ship, swarmShip,
                        constructionTime);
                enemyManager.setSuppressDeploymentMessages(
                        previousSuppression);
                suppressionKnown = false;
                return result;
            } catch (Throwable failure) {
                rethrowIfFatal(failure);
                Throwable cleanupFailure = cleanupPartialBuildBestEffort(
                        engine, enemyManager, mirror, ship, swarmShip);
                addSuppressedFailure(failure, cleanupFailure);
                if (suppressionKnown) {
                    try {
                        enemyManager.setSuppressDeploymentMessages(
                                previousSuppression);
                    } catch (Throwable restoreFailure) {
                        rethrowIfFatal(restoreFailure);
                        addSuppressedFailure(failure, restoreFailure);
                    }
                }
                throw failure;
            }
        }

        private ShipAPI spawnConstructionSwarm(
                Vector2f location,
                float facing,
                float deploymentPoints,
                ShipAPI.HullSize hullSize,
                ShipAPI mirror) throws Throwable {
            ShipAPI carrier = null;
            try {
                carrier = enemyManager.spawnShipOrWing(
                        SwarmLauncherEffect.CONSTRUCTION_SWARM_WING,
                        new Vector2f(location), facing, 0f, null);
                if (carrier == null) return null;
                carrier.setOwner(enemyManager.getOwner());
                carrier.setShipAI(null);
                if (carrier.getWing() != null) {
                    carrier.getWing().setSourceShip(null);
                }
                carrier.setDoNotRender(true);
                carrier.setExplosionScale(0f);
                carrier.setHulkChanceOverride(0f);
                carrier.setSpawnDebris(false);
                carrier.setImpactVolumeMult(
                        SwarmLauncherEffect.IMPACT_VOLUME_MULT);
                carrier.setCollisionClass(CollisionClass.NONE);
                carrier.setControlsLocked(true);
                carrier.getArmorGrid().clearComponentMap();
                carrier.addTag(
                        ThreatShipConstructionScript.SWARM_CONSTRUCTING_SHIP);

                int fragments = ConstructionSwarmSystemScript.getFragmentCost(
                        deploymentPoints, hullSize);
                RoilingSwarmEffect swarm =
                        FragmentSwarmHullmod.createSwarmFor(carrier);
                if (swarm != null) {
                    swarm.getParams().flashFringeColor =
                            VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR;
                    RoilingSwarmEffect.getFlockingMap().remove(
                            swarm.getParams().flockingClass, swarm);
                    swarm.getParams().flockingClass = FragmentSwarmHullmod
                            .CONSTRUCTION_SWARM_FLOCKING_CLASS;
                    RoilingSwarmEffect.getFlockingMap().add(
                            swarm.getParams().flockingClass, swarm);
                    swarm.getParams().maxOffset = Math.max(
                            swarm.getParams().maxOffset * cloudScale(hullSize),
                            mirror.getCollisionRadius() * 1.1f);
                    swarm.getParams().initialMembers = fragments;
                    swarm.getParams().baseMembersToMaintain = fragments;
                    swarm.addMembers(Math.max(
                            0, fragments - swarm.getNumActiveMembers()));
                }
                Global.getSoundPlayer().playSound(
                        "threat_swarm_launched", 1f, 1f,
                        location, carrier.getVelocity());
                return carrier;
            } catch (Throwable failure) {
                rethrowIfFatal(failure);
                addSuppressedFailure(failure,
                        releaseConstructionSwarmBestEffort(
                                engine, enemyManager, carrier));
                throw failure;
            }
        }

        private Vector2f findConstructionLocation(float mirrorRadius) {
            float baseAngle = ungaikyo.getFacing() + 105f
                    + (spawnSequence++ * 137.50777f);
            float baseRadius = ungaikyo.getCollisionRadius()
                    + mirrorRadius + 460f;
            Vector2f fallback = new Vector2f(ungaikyo.getLocation());
            for (int attempt = 0; attempt < 40; attempt++) {
                float angle = baseAngle + attempt * 47f;
                float radius = baseRadius + (attempt / 3) * 260f;
                Vector2f offset = Misc.getUnitVectorAtDegreeAngle(angle);
                offset.scale(radius);
                Vector2f candidate = Vector2f.add(
                        ungaikyo.getLocation(), offset, new Vector2f());
                clampToBattleMap(candidate, mirrorRadius + 120f);
                fallback.set(candidate);
                if (isClear(candidate, mirrorRadius)) return candidate;
            }
            return fallback;
        }

        private boolean isClear(Vector2f point, float mirrorRadius) {
            for (ShipAPI other : engine.getShips()) {
                try {
                    if (other == null || other.isExpired()) continue;
                    float required = mirrorRadius + other.getCollisionRadius()
                            + MIN_SPAWN_CLEARANCE;
                    if (Misc.getDistance(point, other.getLocation())
                            < required) {
                        return false;
                    }
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    // A malformed unrelated ship must not poison construction
                    // of the selected source. Its own slot is guarded later.
                }
            }
            return true;
        }

        private void clampToBattleMap(Vector2f point, float margin) {
            float halfWidth = engine.getMapWidth() * 0.5f;
            float halfHeight = engine.getMapHeight() * 0.5f;
            float safeX = Math.max(100f, halfWidth - margin);
            float safeY = Math.max(100f, halfHeight - margin);
            point.x = Math.max(-safeX, Math.min(safeX, point.x));
            point.y = Math.max(-safeY, Math.min(safeY, point.y));
        }

        private void shutDownMirrors() {
            if (shuttingDown) return;
            shuttingDown = true;
            for (MirrorSlot slot : slots.values()) {
                try {
                    if (slot.construction != null) {
                        slot.construction.cancel();
                        slot.construction = null;
                    }
                    if (slot.active != null) {
                        removeCombatOnlyShipBestEffort(engine, enemyManager, slot.active);
                        slot.active = null;
                    }
                } catch (Throwable failure) {
                    rethrowIfFatal(failure);
                    disableSlot(slot, failure);
                }
                if (slot.lifetime != null) {
                    MirrorLifetime.queueCleanup(engine, slot.lifetime);
                }
            }
            MirrorLifetime.retryPending(engine, enemyManager);
        }

        private void disableSlot(MirrorSlot slot, Throwable failure) {
            rethrowIfFatal(failure);
            slot.disabled = true;
            Throwable combined = failure;
            if (slot.construction != null) {
                try {
                    slot.construction.cancel();
                } catch (Throwable cleanupFailure) {
                    rethrowIfFatal(cleanupFailure);
                    addSuppressedFailure(combined, cleanupFailure);
                }
                slot.construction = null;
            }
            if (slot.active != null) {
                Throwable cleanupFailure = removeCombatOnlyShipBestEffort(
                        engine, enemyManager, slot.active);
                addSuppressedFailure(combined, cleanupFailure);
                slot.active = null;
            }
            if (slot.lifetime != null) {
                MirrorLifetime.queueCleanup(engine, slot.lifetime);
            }
            if (!slot.failureLogged) {
                slot.failureLogged = true;
                try {
                    Global.getLogger(
                            GautamaFinalRematchBattleCreationPlugin.class)
                            .warn("Disabled Ungaikyo reflection source "
                                    + slot.sourceMemberId
                                    + " after a nonfatal mod-ship error; "
                                    + "this ship will not regenerate.",
                                    combined);
                } catch (Throwable loggingFailure) {
                    rethrowIfFatal(loggingFailure);
                    // Logging must never turn source quarantine into a battle
                    // controller failure.
                }
            }
        }

        private void logUnslottedFailure(
                FleetMemberAPI member, String operation, Throwable failure) {
            String id = "<unknown>";
            try {
                if (member != null && member.getId() != null) {
                    id = member.getId();
                }
            } catch (Throwable idFailure) {
                rethrowIfFatal(idFailure);
                addSuppressedFailure(failure, idFailure);
            }
            try {
                Global.getLogger(GautamaFinalRematchBattleCreationPlugin.class)
                        .warn("Skipped Ungaikyo reflection source " + id
                                + " after a nonfatal error while "
                                + operation + ".", failure);
            } catch (Throwable loggingFailure) {
                rethrowIfFatal(loggingFailure);
            }
        }

        private String safeShipName(FleetMemberAPI member) {
            try {
                return member == null ? null : member.getShipName();
            } catch (Throwable failure) {
                rethrowIfFatal(failure);
                return null;
            }
        }

    }

    /** One source campaign member, one active-or-building echo. */
    private static final class MirrorSlot {
        private final String sourceMemberId;
        private final String shipName;
        private final ShipVariantAPI snapshot;
        private final float deploymentPoints;
        private DynamicMirrorConstruction construction;
        private MirrorLifetime lifetime;
        private ShipAPI active;
        private float nextEligibleAt;
        private boolean hasCompleted;
        private boolean disabled;
        private boolean failureLogged;

        private MirrorSlot(
                String sourceMemberId,
                String shipName,
                ShipVariantAPI snapshot,
                float deploymentPoints) {
            this.sourceMemberId = sourceMemberId;
            this.shipName = shipName;
            this.snapshot = snapshot;
            this.deploymentPoints = deploymentPoints;
        }
    }

    private enum ConstructionResult {
        BUILDING,
        COMPLETE,
        FAILED
    }

    /** Visual-only reuse of vanilla's dark-red Threat construction smoke. */
    private static final class MirrorConstructionSmoke
            extends ThreatShipConstructionScript {
        private MirrorConstructionSmoke(
                ShipAPI mirror, ShipAPI swarm, float duration) {
            super(null, swarm, 0f, duration);
            ship = mirror;
        }

        @Override
        protected void spawnShip() {
            // The controller already spawned the exact cloned player variant.
            // This emitter must never create a second stock ship or run the
            // vanilla construction script's gameplay/AI/completion handling.
        }

        private void advanceSmoke(float amount) {
            if (amount <= 0f) return;
            elapsed += amount;
            spawnParticles(amount);
        }
    }

    /**
     * Dynamic counterpart to ThreatShipConstructionScript. Vanilla's script
     * only accepts a registered variant ID; this one owns a freshly cloned
     * FleetMember while retaining the real construction swarm and its flocking
     * and voltaic visual parameters.
     */
    private static final class DynamicMirrorConstruction {
        private static final String BUILD_STAT_ID =
                "chief_navigator_ungaikyo_reflection_build";

        private final CombatEngineAPI engine;
        private final CombatFleetManagerAPI manager;
        private final FleetMemberAPI member;
        private final ShipAPI mirror;
        private final ShipAPI swarmCarrier;
        private final MirrorLifetime lifetime;
        private final MirrorConstructionSmoke constructionSmoke;
        private final Map<ShipAPI, CollisionClass> componentCollisionClasses =
                new IdentityHashMap<ShipAPI, CollisionClass>();
        private final float duration;
        private float elapsed;
        private boolean complete;
        private boolean cancelled;

        private DynamicMirrorConstruction(
                CombatEngineAPI engine,
                CombatFleetManagerAPI manager,
                FleetMemberAPI member,
                ShipAPI mirror,
                ShipAPI swarmCarrier,
                float duration) {
            this.engine = engine;
            this.manager = manager;
            this.member = member;
            this.mirror = mirror;
            this.swarmCarrier = swarmCarrier;
            this.lifetime = MirrorLifetime.forShip(mirror);
            this.duration = Math.max(0.1f, duration);
            this.constructionSmoke = new MirrorConstructionSmoke(
                    mirror, swarmCarrier, this.duration);
            mirror.addTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
            mirror.setPullBackFighters(true);
            MirrorCompatibility.rememberConstructionAI(mirror);
            mirror.setShipAI(null);
            mirror.getVelocity().set(0f, 0f);
            mirror.setAngularVelocity(0f);
            for (ShipAPI component : getConstructionEntities()) {
                prepareComponent(component);
            }
        }

        private ConstructionResult advance(float amount) {
            if (cancelled || !isAliveInPlay(mirror)) {
                return ConstructionResult.FAILED;
            }
            if (complete) return ConstructionResult.COMPLETE;

            elapsed += amount;
            float progress = clamp01(elapsed / duration);
            float alpha = smoothstep(progress);
            for (ShipAPI component : getConstructionEntities()) {
                maintainComponent(component, alpha);
            }
            MirrorCompatibility.rememberConstructionAI(mirror);
            mirror.setShipAI(null);
            mirror.getVelocity().set(0f, 0f);
            mirror.setAngularVelocity(0f);
            mirror.setJitterUnder(
                    this,
                    VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR,
                    0.08f + 0.18f * progress,
                    5,
                    0f,
                    8f + 18f * progress);
            constructionSmoke.advanceSmoke(amount);
            if (swarmCarrier != null
                    && engine.isEntityInPlay(swarmCarrier)) {
                swarmCarrier.getLocation().set(mirror.getLocation());
                swarmCarrier.getVelocity().set(0f, 0f);
                swarmCarrier.setFacing(mirror.getFacing());
            }

            if (elapsed < duration) return ConstructionResult.BUILDING;
            finish();
            return ConstructionResult.COMPLETE;
        }

        private void finish() {
            if (complete || cancelled) return;
            // A failed visual-swarm removal is queued and neutralized; it must
            // not quarantine a healthy completed reflection.
            releaseSwarmBestEffort();
            mirror.removeTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION);
            for (ShipAPI component : new ArrayList<ShipAPI>(
                    componentCollisionClasses.keySet())) {
                if (isAliveInPlay(component)) {
                    finishComponent(component);
                }
            }
            mirror.setPullBackFighters(false);
            setShipToFullCR(mirror, member);
            mirror.setTimeDeployed(0f);
            // Detached modules/fighters can already have left the parent's
            // current tree. Hand off each owned live entity exactly once.
            for (ShipAPI component : getConstructionEntities()) {
                activateMirrorShip(component,
                        component == mirror ? member : component.getFleetMember(),
                        manager.getOwner());
            }
            orderCompletedMirrorToHunt(mirror, manager);
            complete = true;
        }

        private void cancel() {
            // The queue owns references until every entity is actually gone;
            // a second cancellation still retries a failed first removal.
            cancelled = true;
            MirrorLifetime.queueCleanup(engine, lifetime);
            releaseSwarmBestEffort();
            removeCombatOnlyShipBestEffort(engine, manager, mirror);
        }

        private Throwable releaseSwarmBestEffort() {
            return releaseConstructionSwarmBestEffort(
                    engine, manager, swarmCarrier);
        }

        private ShipAPI getMirror() {
            return mirror;
        }

        private void prepareComponent(ShipAPI component) {
            MirrorCompatibility.rememberConstructionAI(component);
            componentCollisionClasses.put(
                    component, component.getCollisionClass());
            component.setCollisionClass(CollisionClass.NONE);
            component.setPhased(true);
            component.setControlsLocked(true);
            component.setHoldFire(true);
            component.setAlphaMult(0f);
            component.setHulkChanceOverride(0f);
            component.setSpawnDebris(false);
            component.setTimeDeployed(0f);
            component.getMutableStats().getHullDamageTakenMult().modifyMult(
                    BUILD_STAT_ID, 0f);
            component.getMutableStats().getArmorDamageTakenMult().modifyMult(
                    BUILD_STAT_ID, 0f);
            component.getMutableStats().getShieldDamageTakenMult().modifyMult(
                    BUILD_STAT_ID, 0f);
            component.getMutableStats().getEmpDamageTakenMult().modifyMult(
                    BUILD_STAT_ID, 0f);
        }

        private void maintainComponent(ShipAPI component, float alpha) {
            // A module may be materialized lazily after its parent. Capture it
            // the first frame it appears so no functional section can fire or
            // collide ahead of the construction silhouette.
            if (!componentCollisionClasses.containsKey(component)) {
                prepareComponent(component);
            }
            MirrorCompatibility.rememberConstructionAI(component);
            component.setAlphaMult(alpha);
            component.setCollisionClass(CollisionClass.NONE);
            component.setPhased(true);
            component.setControlsLocked(true);
            component.setHoldFire(true);
            component.setTimeDeployed(0f);
        }

        private void finishComponent(ShipAPI component) {
            CollisionClass original = componentCollisionClasses.get(component);
            component.setOwner(manager.getOwner());
            component.setOriginalOwner(manager.getOwner());
            component.setAlphaMult(1f);
            component.setPhased(false);
            component.setCollisionClass(original == null
                    ? CollisionClass.SHIP : original);
            component.setControlsLocked(false);
            component.setHoldFire(false);
            component.getMutableStats().getHullDamageTakenMult().unmodify(
                    BUILD_STAT_ID);
            component.getMutableStats().getArmorDamageTakenMult().unmodify(
                    BUILD_STAT_ID);
            component.getMutableStats().getShieldDamageTakenMult().unmodify(
                    BUILD_STAT_ID);
            component.getMutableStats().getEmpDamageTakenMult().unmodify(
                    BUILD_STAT_ID);
            component.setTimeDeployed(0f);
        }

        private List<ShipAPI> getConstructionEntities() {
            Throwable failure = lifetime.capture(engine, manager);
            if (failure != null) {
                throw new MirrorIsolationException(
                        "Failed to track mirror construction descendants", failure);
            }
            List<ShipAPI> result = new ArrayList<ShipAPI>();
            for (ShipAPI component : lifetime.getShipsCopy()) {
                if (isAliveInPlay(component)) result.add(component);
            }
            return result;
        }
    }

    static boolean isEligiblePlayerMember(FleetMemberAPI member) {
        if (member == null || member.getId() == null
                || member.isFighterWing()) {
            return false;
        }
        ShipVariantAPI variant = member.getVariant();
        return variant != null
                && !variant.isFighter()
                && !variant.getHints().contains(ShipTypeHints.MODULE);
    }

    static boolean isCivilianMember(FleetMemberAPI member) {
        return member != null
                && member.getVariant() != null
                && member.getVariant().getHints().contains(
                        ShipTypeHints.CIVILIAN);
    }

    private static ShipVariantAPI snapshotForMember(FleetMemberAPI member) {
        ShipVariantAPI source = member == null ? null : member.getVariant();
        if (source == null) {
            throw new IllegalArgumentException(
                    "Ungaikyo cannot snapshot a member without a variant");
        }
        if (!isCivilianMember(member)) return source.clone();

        String replacementId = getCivilianReplacementVariantId(
                source.getHullSize());
        ShipVariantAPI replacement = Global.getSettings().getVariant(
                replacementId);
        if (replacement == null) {
            throw new IllegalStateException(
                    "Missing civilian replacement variant: "
                            + replacementId);
        }
        return replacement.clone();
    }

    /** Stock THREAT combat analogue for each civilian hull-size slot. */
    static String getCivilianReplacementVariantId(ShipAPI.HullSize size) {
        if (size == ShipAPI.HullSize.CAPITAL_SHIP) {
            return "fabricator_unit_Type450";
        }
        if (size == ShipAPI.HullSize.CRUISER) {
            return "standoff_unit_Type300";
        }
        if (size == ShipAPI.HullSize.DESTROYER) {
            return "assault_unit_Type200";
        }
        return "skirmish_unit_Type100";
    }

    static boolean fitsSummonBudget(float spent, float nextCost) {
        return Math.max(0f, spent) + Math.max(0f, nextCost)
                <= SUMMON_DP_BUDGET + 0.0001f;
    }

    static float getConstructionTime(float deploymentPoints) {
        float vanilla = ConstructionSwarmSystemScript.BASE_CONSTRUCTION_TIME
                + Math.max(0f, deploymentPoints)
                * ConstructionSwarmSystemScript.CONSTRUCTION_TIME_DP_MULT;
        // The vanilla formula is intended for a long-running fabricator ship:
        // a 40-DP hull takes 65 seconds and can remain inert for an entire boss
        // battle. Preserve its DP ordering while fitting this encounter's
        // on-the-fly fabrication cadence into a bounded 4-12 second window.
        return Math.max(4f, Math.min(12f, vanilla * 0.2f));
    }

    /**
     * Restore ship-specific AI after construction, or initialize the native
     * member-aware default AI and apply Fearless to a generic parent/module.
     */
    static void activateCompletedMirror(
            ShipAPI mirror, FleetMemberAPI member, int enemyOwner) {
        if (mirror == null || member == null) {
            throw new IllegalArgumentException(
                    "A completed Ungaikyo reflection needs a ship and member");
        }
        member.setOwner(enemyOwner);
        for (ShipAPI component : getShipTree(mirror)) {
            if (component != mirror && (!component.isAlive() || component.isHulk())) continue;
            activateMirrorShip(component, component == mirror ? member : component.getFleetMember(),
                    enemyOwner);
        }
    }

    private static void activateMirrorShip(
            ShipAPI mirror, FleetMemberAPI member, int enemyOwner) {
        if (member != null) {
            member.setOwner(enemyOwner);
        }
        mirror.setOwner(enemyOwner);
        mirror.setOriginalOwner(enemyOwner);
        mirror.setControlsLocked(false);
        mirror.setHoldFire(false);
        mirror.setPhased(false);
        ShipAIPlugin ai = mirror.getShipAI();
        ShipAIPlugin saved = MirrorCompatibility.takeConstructionAI(mirror);
        if (saved != null && saved != ai
                && (ai == null || MirrorCompatibility.isGenericDefaultAI(ai))) {
            // Native setDefaultAI only selects/installs an AI. Restoring the
            // saved public wrapper retains creator context and system ownership.
            mirror.setShipAI(saved);
            ai = mirror.getShipAI();
        }
        boolean createdFearless = false;
        if (ai == null) {
            mirror.setDefaultAI(member);
            ai = mirror.getShipAI();
            if (ai == null) {
                ai = Global.getSettings().createDefaultShipAI(
                        mirror, GuardDroneAI.createFearlessConfig());
                if (ai != null) {
                    mirror.setShipAI(ai);
                    createdFearless = true;
                }
            }
        }
        if (ai == null) {
            throw new IllegalStateException(
                    "No combat AI could be created for Ungaikyo reflection");
        }
        if (createdFearless || MirrorCompatibility.isGenericDefaultAI(ai)) {
            if (member != null) member.setPersonalityOverride(MIRROR_PERSONALITY);
            if (mirror.getCaptain() != null) {
                mirror.getCaptain().setPersonality(MIRROR_PERSONALITY);
            }
            // Mutate only the generic config in place, preserving native
            // hull-specific behavior/system modules rather than rebuilding them.
            GuardDroneAI.configureFearless(ai.getConfig());
        }
        // A fresh Mote Attractor script lazily discovers its emitter slots.
        // Native Ziggurat AI can query getLockTarget() before its first system
        // update, which dereferences that uninitialized attractor. This public
        // range query initializes the cache without advancing/using the system
        // or spawning motes; repeat only when this mirror's AI is recreated.
        ShipSystemAPI system = mirror.getSystem();
        if (system != null && system.getScript() instanceof MoteControlScript) {
            ((MoteControlScript) system.getScript()).isLocationInRange(
                    mirror, mirror.getLocation());
        }
        ai.forceCircumstanceEvaluation();
    }

    /** Combat-only zero-DP mirrors need their own hunt order, not an automatic rally/post. */
    static void orderCompletedMirrorToHunt(ShipAPI mirror, CombatFleetManagerAPI manager) {
        DeployedFleetMemberAPI deployed = manager.getDeployedFleetMember(mirror);
        CombatTaskManagerAPI tasks = manager.getTaskManager(false);
        if (deployed != null && tasks != null) tasks.orderSearchAndDestroy(deployed, false);
    }

    /** One native AI refresh per real Ungaikyo deployment, never per frame. */
    static void configureUngaikyoAI(ShipAPI ship) {
        FleetMemberAPI member = ship.getFleetMember();
        if (member != null) member.setPersonalityOverride(Personalities.CAUTIOUS);
        if (ship.getCaptain() != null) ship.getCaptain().setPersonality(Personalities.CAUTIOUS);
        ship.resetDefaultAI();
        ShipAIPlugin ai = ship.getShipAI();
        if (ai != null) {
            ShipAIConfig config = ai.getConfig();
            if (config != null) config.personalityOverride = Personalities.CAUTIOUS;
            ai.forceCircumstanceEvaluation();
        }
    }

    static float cloudScale(ShipAPI.HullSize hullSize) {
        if (hullSize == ShipAPI.HullSize.CAPITAL_SHIP) return 4f;
        if (hullSize == ShipAPI.HullSize.CRUISER) return 2.5f;
        if (hullSize == ShipAPI.HullSize.DESTROYER) return 1.6f;
        return 1f;
    }

    static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    static float smoothstep(float value) {
        float clamped = clamp01(value);
        return clamped * clamped * (3f - 2f * clamped);
    }

    private static void initializeConstructingMirror(
            ShipAPI ship, FleetMemberAPI member) {
        ship.setOwner(member.getOwner());
        ship.setHulkChanceOverride(0f);
        ship.setSpawnDebris(false);
        setShipToFullCR(ship, member);
    }

    private static void setMemberToFullCR(FleetMemberAPI member) {
        if (member == null || member.getRepairTracker() == null) return;
        member.getRepairTracker().setCR(
                member.getRepairTracker().getMaxCR());
    }

    private static void setShipToFullCR(
            ShipAPI ship, FleetMemberAPI member) {
        setMemberToFullCR(member);
        if (ship == null || member == null
                || member.getRepairTracker() == null) return;
        float maximum = member.getRepairTracker().getMaxCR();
        ship.setCRAtDeployment(maximum);
        ship.setCurrentCR(maximum);
    }

    private static boolean isUngaikyo(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        if (UNGAIKYO_HULL_ID.equals(ship.getHullSpec().getHullId())
                || UNGAIKYO_HULL_ID.equals(
                        ship.getHullSpec().getBaseHullId())) {
            return true;
        }
        return ship.getVariant() != null
                && UNGAIKYO_VARIANT_ID.equals(
                        ship.getVariant().getHullVariantId());
    }

    private static boolean isAlive(ShipAPI ship) {
        return ship != null && ship.isAlive() && !ship.isHulk();
    }

    private static boolean isAliveInPlay(ShipAPI ship) {
        CombatEngineAPI engine = Global.getCombatEngine();
        return isAlive(ship) && engine != null && engine.isEntityInPlay(ship);
    }

    private static Throwable cleanupPartialBuildBestEffort(
            CombatEngineAPI engine,
            CombatFleetManagerAPI manager,
            FleetMemberAPI member,
            ShipAPI knownShip,
            ShipAPI swarmCarrier) {
        FailureAccumulator failures = new FailureAccumulator();
        failures.capture(releaseConstructionSwarmBestEffort(
                engine, manager, swarmCarrier));

        Set<ShipAPI> found = Collections.newSetFromMap(
                new IdentityHashMap<ShipAPI, Boolean>());
        if (knownShip != null) found.add(knownShip);
        if (manager != null && member != null) {
            try {
                ShipAPI managed = manager.getShipFor(member);
                if (managed != null) found.add(managed);
            } catch (Throwable failure) {
                failures.capture(failure);
            }
        }
        if (engine != null) {
            List<ShipAPI> ships = new ArrayList<ShipAPI>();
            try {
                ships.addAll(engine.getShips());
            } catch (Throwable failure) {
                failures.capture(failure);
            }
            for (ShipAPI candidate : ships) {
                try {
                    boolean sameMember = member != null
                            && candidate.getFleetMember() == member;
                    if (sameMember) found.add(candidate);
                } catch (Throwable failure) {
                    // The newly created member's exact identity is authority;
                    // unrelated mod getters or a copied source-id string are not.
                    rethrowIfFatal(failure);
                }
            }
        }
        for (ShipAPI partial : found) {
            failures.capture(removeCombatOnlyShipBestEffort(
                    engine, manager, partial));
        }
        return failures.failure;
    }

    private static Throwable releaseConstructionSwarmBestEffort(
            CombatEngineAPI engine,
            CombatFleetManagerAPI manager,
            ShipAPI swarmCarrier) {
        if (swarmCarrier == null) return null;
        FailureAccumulator failures = new FailureAccumulator();
        try {
            RoilingSwarmEffect swarm =
                    RoilingSwarmEffect.getSwarmFor(swarmCarrier);
            if (swarm != null) {
                swarm.getParams().despawnSound = null;
                swarm.setForceDespawn(true);
            }
        } catch (Throwable failure) {
            failures.capture(failure);
        }
        failures.capture(removeCombatOnlyShipBestEffort(
                engine, manager, swarmCarrier));
        return failures.failure;
    }

    private static Throwable removeCombatOnlyShipBestEffort(
            CombatEngineAPI engine,
            CombatFleetManagerAPI manager,
            ShipAPI ship) {
        if (ship == null) return null;
        MirrorLifetime lifetime = MirrorLifetime.forShip(ship);
        // A partial spawn or failed swarm release has no slot field to retain
        // it. Publish cleanup ownership before trying any removal operation.
        MirrorLifetime.queueCleanup(engine, lifetime);
        Throwable failure = lifetime.removeBestEffort(engine, manager);
        if (failure == null && !lifetime.isRemovalComplete(engine)) {
            failure = new IllegalStateException(
                    "Combat-only mirror removal remains pending");
        }
        return failure;
    }

    private static List<ShipAPI> getShipTree(ShipAPI root) {
        List<ShipAPI> result = new ArrayList<ShipAPI>();
        Set<ShipAPI> seen = Collections.newSetFromMap(
                new IdentityHashMap<ShipAPI, Boolean>());
        addShipTree(root, result, seen);
        return result;
    }

    private static void addShipTree(
            ShipAPI ship, List<ShipAPI> result, Set<ShipAPI> seen) {
        if (ship == null || !seen.add(ship)) return;
        result.add(ship);
        for (ShipAPI module : ship.getChildModulesCopy()) {
            addShipTree(module, result, seen);
        }
    }

    static boolean isNonfatalMirrorFailure(Throwable failure) {
        return failure != null
                && !(failure instanceof VirtualMachineError)
                && !(failure instanceof ThreadDeath);
    }

    static void rethrowIfFatal(Throwable failure) {
        if (failure instanceof VirtualMachineError) {
            throw (VirtualMachineError) failure;
        }
        if (failure instanceof ThreadDeath) {
            throw (ThreadDeath) failure;
        }
    }

    private static Throwable addSuppressedFailure(
            Throwable primary, Throwable secondary) {
        if (secondary == null) return primary;
        rethrowIfFatal(secondary);
        if (primary == null) return secondary;
        if (primary != secondary) {
            try {
                primary.addSuppressed(secondary);
            } catch (Throwable ignored) {
                rethrowIfFatal(ignored);
            }
        }
        return primary;
    }

    private static final class FailureAccumulator {
        private Throwable failure;

        private void capture(Throwable next) {
            failure = addSuppressedFailure(failure, next);
        }
    }

    private static final class MirrorIsolationException
            extends RuntimeException {
        private MirrorIsolationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public GautamaFinalRematchBattleCreationPlugin() { }
}
