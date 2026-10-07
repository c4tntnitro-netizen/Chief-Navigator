package chiefnavigator.quest;

import chiefnavigator.ai.GuardDroneAI;
import chiefnavigator.hullmods.IthacaSiegeStalemateHullmod;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.ArmorGridAPI;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.List;

/** An immobile station fight won by disabling the exposed rear reactor. */
public final class DriftingWallBattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        context.objectivesAllowed = false;
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new DriftingWallCombatPlugin(engine));
    }

    private static final class DriftingWallCombatPlugin extends BaseEveryFrameCombatPlugin {
        private static final String LOG_PREFIX = "[ITHACA_NAVAL] ";
        private static final String NAVAL_WEAPON_ID =
                "chief_navigator_drifting_wall_colossal_inert";
        private static final float DEBUG_INTERVAL = 5f;
        private static final String STAT_ID = "chief_navigator_drifting_wall_anchor";
        // One and a half 2,000-unit tactical-grid blocks toward the enemy side.
        private static final Vector2f ANCHOR = new Vector2f(0f, 3000f);
        private static final float FOUNDATION_FACING = 270f;
        private static final float REPAIR_DELAY_SECONDS = 6f;
        private static final float REPAIR_FRACTION_PER_SECOND = 0.005f;

        private final CombatEngineAPI engine;
        private ShipAPI core;
        private ShipAPI reactor;
        private boolean reactorInitialized;
        private boolean wallDisabled;
        private boolean guardsDeployed;
        private float lastReactorHitpoints = -1f;
        private float repairDelayRemaining;
        private float debugElapsed;

        private DriftingWallCombatPlugin(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            // Match the intact Ithaca foundation: configure fire control even
            // during the deployment pause, before the first live combat frame.
            if (core == null) {
                for (ShipAPI ship : engine.getShips()) {
                    if (ship.isAlive() && isWallFoundation(ship)) {
                        core = ship;
                        immobilize(core);
                        restoreFoundation(core);
                        restoreLiveWeapons(core);
                        logFoundationSnapshot("initialized");
                        break;
                    }
                }
            }
            if (core == null) return;

            core.getLocation().set(ANCHOR);
            core.getVelocity().set(0f, 0f);
            // The rebuilt wall art is authored horizontally. The 270-degree
            // facing preserves that presentation while turning the station to
            // face the opposite direction from its original 90-degree setup.
            core.setFacing(FOUNDATION_FACING);
            core.setAngularVelocity(0f);
            // Movement is suppressed through the zeroed movement stats and
            // hard anchor above. Do not lock controls here: doing so also
            // prevents the station AI from operating its weapon groups.
            if (wallDisabled) return;
            if (!engine.isPaused()) {
                debugElapsed += amount;
                if (debugElapsed >= DEBUG_INTERVAL) {
                    debugElapsed -= DEBUG_INTERVAL;
                    logFoundationSnapshot("periodic");
                }
            }
            findReactor();
            // Initialization above must match the intact foundation during
            // deployment. Timers, spawns, repairs, and victory state remain
            // frozen until combat itself resumes.
            if (engine.isPaused()) return;
            if (!reactorInitialized) return;

            if (reactor == null || !reactor.isAlive() || reactor.isHulk()) {
                disableWall();
                return;
            }

            deployDroneGuards();
            pulseReactor();
            repairReactor(amount);
            int integrity = Math.round(100f
                    * reactor.getHitpoints() / reactor.getMaxHitpoints());
            engine.maintainStatusForPlayerShip(
                    this,
                    "graphics/icons/hullsys/fortress_shield.png",
                    "DRIFTING WALL",
                    "Rear reactor integrity: " + integrity + "%",
                    false);
        }

        private void findReactor() {
            if (reactorInitialized) return;
            for (ShipAPI module : core.getChildModulesCopy()) {
                if (!DriftingWallEncounter.REACTOR_HULL_ID.equals(
                        module.getHullSpec().getHullId())) continue;
                reactor = module;
                reactorInitialized = true;
                lastReactorHitpoints = reactor.getHitpoints();
                reactor.setShowModuleJitterUnder(true);
                return;
            }
        }

        private void deployDroneGuards() {
            if (guardsDeployed) return;
            CombatFleetManagerAPI manager = engine.getFleetManager(
                    core.getOwner());
            String[] variants = DriftingWallEncounter.getDroneGuardVariants();
            for (int i = 0; i < variants.length; i++) {
                Vector2f location = findReactorGuardSpawn(
                        i, variants.length, 1250f + 250f * (i % 2));
                ShipAPI guard = manager.spawnShipOrWing(
                        variants[i],
                        location,
                        Misc.getAngleInDegrees(
                                reactor.getLocation(), location),
                        0f);
                if (guard != null) {
                    IthacaSiegeStalemateHullmod.markWallDefender(guard);
                    // Native combat AI owns pursuit, firing, venting, and
                    // ship-system use; the spawn location is not a fixed post.
                    GuardDroneAI.installFreeMovingFearlessAI(guard);
                }
            }
            guardsDeployed = true;
        }

        private Vector2f findReactorGuardSpawn(
                int index, int count, float startingRadius) {
            float rear = Misc.getAngleInDegrees(
                    core.getLocation(), reactor.getLocation());
            float spread = count <= 1
                    ? 0f : -82f + 164f * index / (count - 1f);
            float angle = rear + spread;
            Vector2f direction = Misc.getUnitVectorAtDegreeAngle(angle);
            float radius = startingRadius;
            Vector2f location = new Vector2f();
            for (int attempt = 0; attempt < 24; attempt++) {
                location.set(
                        reactor.getLocation().x + direction.x * radius,
                        reactor.getLocation().y + direction.y * radius);
                if (isClearGuardSpawn(location)) return location;
                radius += 220f;
            }
            return location;
        }

        private boolean isClearGuardSpawn(Vector2f location) {
            for (ShipAPI other : engine.getShips()) {
                if (other == null || other.isExpired()) continue;
                float spacing = other.getCollisionRadius() + 240f;
                if (Vector2f.sub(location, other.getLocation(), null)
                        .lengthSquared() < spacing * spacing) {
                    return false;
                }
            }
            return true;
        }

        private void pulseReactor() {
            float phase = engine.getTotalElapsedTime(false) * 3f;
            float pulse = 0.28f + 0.10f
                    * (0.5f + 0.5f * (float) Math.sin(phase));
            reactor.setJitterUnder(
                    this,
                    new Color(35, 220, 255, 150),
                    pulse,
                    5,
                    0f,
                    18f);
            reactor.setJitter(
                    this,
                    new Color(170, 250, 255, 90),
                    pulse * 0.2f,
                    2,
                    0f,
                    5f);
        }

        private void repairReactor(float amount) {
            float hitpoints = reactor.getHitpoints();
            if (lastReactorHitpoints >= 0f
                    && hitpoints < lastReactorHitpoints - 0.5f) {
                repairDelayRemaining = REPAIR_DELAY_SECONDS;
            } else if (repairDelayRemaining > 0f) {
                repairDelayRemaining = Math.max(
                        0f, repairDelayRemaining - amount);
            } else if (amount > 0f && hitpoints < reactor.getMaxHitpoints()) {
                float repaired = reactor.getMaxHitpoints()
                        * REPAIR_FRACTION_PER_SECOND * amount;
                reactor.setHitpoints(Math.min(
                        reactor.getMaxHitpoints(), hitpoints + repaired));
            }
            lastReactorHitpoints = reactor.getHitpoints();
        }

        private void disableWall() {
            wallDisabled = true;
            for (WeaponAPI weapon : core.getAllWeapons()) {
                weapon.stopFiring();
                weapon.setForceDisabled(true);
            }
            for (ShipAPI module : core.getChildModulesCopy()) {
                for (WeaponAPI weapon : module.getAllWeapons()) {
                    weapon.stopFiring();
                    weapon.setForceDisabled(true);
                }
                module.setShipSystemDisabled(true);
                module.setDefenseDisabled(true);
                module.setControlsLocked(true);
            }
            core.setShipSystemDisabled(true);
            core.setDefenseDisabled(true);
            core.setControlsLocked(true);
            DriftingWallEncounter.reportReactorDisabled();
            engine.endCombat(1.5f, FleetSide.PLAYER);
        }

        private void immobilize(ShipAPI ship) {
            ship.getMutableStats().getMaxSpeed().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getAcceleration().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getDeceleration().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getMaxTurnRate().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getTurnAcceleration().modifyMult(STAT_ID, 0f);
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                weapon.setCurrHealth(weapon.getMaxHealth());
            }
        }

        private boolean isWallFoundation(ShipAPI ship) {
            String hullId = ship.getHullSpec().getHullId();
            return DriftingWallEncounter.CORE_HULL_ID.equals(hullId)
                    || DriftingWallEncounter.BASE_HULL_ID.equals(hullId);
        }

        /**
         * The visible Vast Bulk parent must enter combat as pristine structure.
         * A zero/depleted armor grid causes the engine to paint a live damage
         * stripe and emit damage effects even though the parent is invulnerable.
         */
        private void restoreFoundation(ShipAPI ship) {
            ship.setHitpoints(ship.getMaxHitpoints());
            ArmorGridAPI armor = ship.getArmorGrid();
            float max = armor.getMaxArmorInCell();
            float[][] grid = armor.getGrid();
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    armor.setArmorValue(x, y, max);
                }
            }
            ship.syncWithArmorGridState();
            ship.syncWeaponDecalsWithArmorDamage();
        }

        /** Mirrors the working FOB Ithaca foundation initialization. */
        private void restoreLiveWeapons(ShipAPI ship) {
            IthacaFoundationFireControl.ensureActive(ship);
            for (WeaponAPI weapon : ship.getAllWeapons()) {
                IthacaFoundationFireControl.restoreWeapon(weapon);
            }
            for (ShipAPI module : ship.getChildModulesCopy()) {
                module.setControlsLocked(false);
                module.setShipSystemDisabled(false);
                module.setDefenseDisabled(false);
                for (WeaponAPI weapon : module.getAllWeapons()) {
                    IthacaFoundationFireControl.restoreWeapon(weapon);
                }
            }
        }

        private void logFoundationSnapshot(String reason) {
            if (core == null) return;
            ShipAPI nearest = findNearestHostile();
            float sightBonus = core.getMutableStats().getSightRadiusMod()
                    .computeEffective(0f);
            Global.getLogger(DriftingWallBattleCreationPlugin.class).info(
                    LOG_PREFIX + "foundation " + reason
                    + " hull=" + shipLabel(core)
                    + ", inPlay=" + engine.isEntityInPlay(core)
                    + ", ai=" + className(core.getShipAI())
                    + ", controlsLocked=" + core.controlsLocked()
                    + ", holdFire=" + core.isHoldFire()
                    + ", holdFireOneFrame=" + core.isHoldFireOneFrame()
                    + ", defenseDisabled=" + core.isDefenseDisabled()
                    + ", shipTarget=" + shipLabel(core.getShipTarget())
                    + ", nearestHostile=" + shipLabel(nearest)
                    + ", nearestCenterDistance=" + rounded(centerDistance(core, nearest))
                    + ", nearestSurfaceDistance=" + rounded(surfaceDistance(core, nearest))
                    + ", awareOfNearest="
                    + (nearest != null && engine.isAwareOf(core.getOwner(), nearest))
                    + ", sightFlatBonus=" + rounded(sightBonus)
                    + ", flux=" + rounded(core.getFluxLevel())
                    + ", overloaded=" + core.getFluxTracker().isOverloaded()
                    + ", venting=" + core.getFluxTracker().isVenting()
                    + ", groups=" + core.getWeaponGroupsCopy().size());

            List<WeaponGroupAPI> groups = core.getWeaponGroupsCopy();
            for (int i = 0; i < groups.size(); i++) {
                WeaponGroupAPI group = groups.get(i);
                if (group == null) {
                    Global.getLogger(DriftingWallBattleCreationPlugin.class).warn(
                            LOG_PREFIX + "foundation group=" + i + " is null");
                    continue;
                }
                Global.getLogger(DriftingWallBattleCreationPlugin.class).info(
                        LOG_PREFIX + "foundation group=" + i
                        + ", type=" + group.getType()
                        + ", autofire=" + group.isAutofiring()
                        + ", weapons=" + weaponList(group));
            }

            for (WeaponAPI weapon : core.getAllWeapons()) {
                if (!NAVAL_WEAPON_ID.equals(weapon.getId())) continue;
                logNavalWeapon(weapon, nearest);
            }
        }

        private void logNavalWeapon(WeaponAPI weapon, ShipAPI nearest) {
            WeaponGroupAPI group = core.getWeaponGroupFor(weapon);
            AutofireAIPlugin autofire = group == null
                    ? null : group.getAutofirePlugin(weapon);
            Vector2f targetPoint = autofire == null ? null : autofire.getTarget();
            ShipAPI autoTarget = autofire == null ? null : autofire.getTargetShip();
            Vector2f comparisonPoint = targetPoint != null
                    ? targetPoint : nearest == null ? null : nearest.getLocation();
            float distance = comparisonPoint == null ? -1f
                    : Misc.getDistance(weapon.getLocation(), comparisonPoint);
            float distanceFromArc = comparisonPoint == null ? -1f
                    : weapon.distanceFromArc(comparisonPoint);
            Global.getLogger(DriftingWallBattleCreationPlugin.class).info(
                    LOG_PREFIX + "naval slot=" + slotId(weapon)
                    + ", groupAutofire=" + (group != null && group.isAutofiring())
                    + ", defaultWeaponAI="
                    + (group != null && group.isUsingDefaultAI(weapon))
                    + ", autofireAI=" + className(autofire)
                    + ", shouldFire=" + (autofire != null && autofire.shouldFire())
                    + ", autoTarget=" + shipLabel(autoTarget)
                    + ", targetPoint=" + vectorLabel(targetPoint)
                    + ", comparisonDistance=" + rounded(distance)
                    + ", distanceFromArc=" + rounded(distanceFromArc)
                    + ", currAngle=" + rounded(weapon.getCurrAngle())
                    + ", arcFacing=" + rounded(weapon.getArcFacing())
                    + ", arc=" + rounded(weapon.getArc())
                    + ", range=" + rounded(weapon.getRange())
                    + ", firing=" + weapon.isFiring()
                    + ", inBurst=" + weapon.isInBurst()
                    + ", charge=" + rounded(weapon.getChargeLevel())
                    + ", cooldown=" + rounded(weapon.getCooldownRemaining())
                    + ", disabled=" + weapon.isDisabled()
                    + ", forceDisabled=" + weapon.isForceDisabled()
                    + ", health=" + rounded(weapon.getCurrHealth()) + "/"
                    + rounded(weapon.getMaxHealth()));
        }

        private ShipAPI findNearestHostile() {
            ShipAPI nearest = null;
            float best = Float.MAX_VALUE;
            for (ShipAPI candidate : engine.getShips()) {
                if (candidate == null || candidate == core
                        || !candidate.isAlive() || candidate.isHulk()
                        || candidate.getOwner() == core.getOwner()
                        || candidate.getOwner() == 100) continue;
                float distance = centerDistance(core, candidate);
                if (distance < best) {
                    nearest = candidate;
                    best = distance;
                }
            }
            return nearest;
        }

        private static String weaponList(WeaponGroupAPI group) {
            StringBuilder result = new StringBuilder();
            for (WeaponAPI weapon : group.getWeaponsCopy()) {
                if (result.length() > 0) result.append('|');
                result.append(weapon.getId()).append('@').append(slotId(weapon));
            }
            return result.toString();
        }

        private static String slotId(WeaponAPI weapon) {
            return weapon == null || weapon.getSlot() == null
                    ? "none" : weapon.getSlot().getId();
        }

        private static String shipLabel(ShipAPI ship) {
            if (ship == null) return "none";
            String hull = ship.getHullSpec() == null
                    ? "unknown" : ship.getHullSpec().getHullId();
            return hull + "#" + ship.getId() + "(owner=" + ship.getOwner()
                    + ", alive=" + ship.isAlive() + ", hulk=" + ship.isHulk()
                    + ", phased=" + ship.isPhased() + ")";
        }

        private static String vectorLabel(Vector2f point) {
            return point == null ? "none"
                    : "(" + rounded(point.x) + "," + rounded(point.y) + ")";
        }

        private static String className(Object value) {
            return value == null ? "null" : value.getClass().getName();
        }

        private static float centerDistance(ShipAPI one, ShipAPI two) {
            return one == null || two == null ? -1f
                    : Misc.getDistance(one.getLocation(), two.getLocation());
        }

        private static float surfaceDistance(ShipAPI one, ShipAPI two) {
            float center = centerDistance(one, two);
            return center < 0f ? -1f : Math.max(0f,
                    center - one.getCollisionRadius() - two.getCollisionRadius());
        }

        private static float rounded(float value) {
            return Math.round(value * 10f) / 10f;
        }
    }

}
