package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.AutofireAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

/** Gives the naval gun a timeflow-style windup and vents it after a full beam cycle. */
public final class DriftingWallBeamEffect implements EveryFrameWeaponEffectPlugin {
    private static final String LOG_PREFIX = "[ITHACA_NAVAL] ";
    private static final Color TIMEFLOW_SHIMMER = new Color(116, 232, 238, 220);
    private static final String NAVAL_WEAPON_ID =
            "chief_navigator_drifting_wall_colossal_inert";
    private static final String ISA_SHIELD_DAMAGE_MODIFIER =
            "chief_navigator_isa_shield_refraction";
    private static final float ISA_SHIELD_DAMAGE_MULTIPLIER = 1.5f;
    private static final float REFRACTION_DAMAGE_FRACTION = 0.5f;
    private static final float REFRACTION_INTERVAL = 0.25f;
    private static final float REFRACTION_RANGE = 6000f;
    private static final float DEBUG_INTERVAL = 5f;
    private static final int SMOKE_COUNT = 48;
    private static final int STEAM_COUNT = 34;
    private static final int JET_COUNT = 18;

    private final Map<WeaponAPI, State> states = new WeakHashMap<>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine == null || weapon == null) return;
        if (engine.isPaused()) return;
        State state = states.get(weapon);
        if (state == null) {
            state = new State();
            state.upgrades = IthacaResearchUpgrades.getMask(weapon.getShip());
            state.isaRefraction = IthacaResearchUpgrades.hasIsaRefraction(
                    weapon.getShip());
            boolean macros = IthacaResearchUpgrades.hasUpgrade(
                    state.upgrades, IthacaResearchUpgrades.MACROSERVOS);
            boolean cycling = IthacaResearchUpgrades.hasUpgrade(
                    state.upgrades, IthacaResearchUpgrades.NAVAL_CYCLER);
            if (macros || cycling) {
                weapon.ensureClonedSpec();
                BeamWeaponSpecAPI spec = (BeamWeaponSpecAPI) weapon.getSpec();
                if (macros) {
                    spec.setTurnRate(spec.getTurnRate()
                            * IthacaResearchUpgrades.NAVAL_TURN_RATE_MULTIPLIER);
                }
                if (cycling) {
                    spec.setBurstCooldown(spec.getBurstCooldown()
                            * IthacaResearchUpgrades.NAVAL_CYCLE_MULTIPLIER);
                }
            }
            states.put(weapon, state);
            logInitialization(weapon, state);
        }

        state.shimmerTime += amount;
        boolean active = hasVisibleBeam(weapon);
        float chargeLevel = weapon.getChargeLevel();
        boolean windingUp = chargeLevel > 0.01f && !active;
        logFiringTransitions(weapon, state, windingUp, active);
        state.debugElapsed += amount;
        if (state.debugElapsed >= DEBUG_INTERVAL) {
            state.debugElapsed -= DEBUG_INTERVAL;
            logWeaponState(engine, weapon, state, windingUp, active);
        }
        updateTimeflowShimmer(weapon, state, chargeLevel, windingUp, active);
        updateChargingLightning(amount, engine, weapon, state, chargeLevel, windingUp);
        if (active && IthacaResearchUpgrades.hasUpgrade(
                state.upgrades, IthacaResearchUpgrades.ARC_PROJECTOR)) {
            state.empElapsed += amount;
            if (state.empElapsed >= 0.25f) {
                state.empElapsed = 0f;
                arcFromBeam(engine, weapon);
            }
        } else {
            state.empElapsed = 0f;
        }
        if (active && state.isaRefraction) {
            installIsaShieldDamageListeners(weapon);
            state.refractionElapsed += amount;
            if (state.refractionElapsed >= REFRACTION_INTERVAL) {
                state.refractionElapsed = 0f;
                refractFromEnemyShield(engine, weapon);
            }
        } else {
            state.refractionElapsed = 0f;
        }
        if (active) {
            state.everFired = true;
        } else {
            if (state.active && state.everFired && !engine.isPaused()) {
                ventWeapon(engine, weapon);
                state.everFired = false;
            }
        }
        state.active = active;
    }

    private static void logInitialization(WeaponAPI weapon, State state) {
        ShipAPI source = weapon.getShip();
        String hull = source == null || source.getHullSpec() == null
                ? "none" : source.getHullSpec().getHullId();
        float dps = weapon.getDerivedStats() == null
                ? -1f : weapon.getDerivedStats().getDps();
        Global.getLogger(DriftingWallBeamEffect.class).info(
                LOG_PREFIX + "initialized hull=" + hull
                + ", owner=" + (source == null ? -1 : source.getOwner())
                + ", upgrades=" + state.upgrades
                + ", isaRefraction=" + state.isaRefraction
                + ", dps=" + dps + ", range=" + weapon.getRange());
    }

    private static void logFiringTransitions(
            WeaponAPI weapon, State state, boolean windingUp, boolean active) {
        if (!state.transitionInitialized
                || windingUp != state.windingUp
                || active != state.active) {
            Global.getLogger(DriftingWallBeamEffect.class).info(
                    LOG_PREFIX + "beam transition slot=" + slotId(weapon)
                    + ", windingUp=" + windingUp
                    + ", activeBeam=" + active
                    + ", isFiring=" + weapon.isFiring()
                    + ", inBurst=" + weapon.isInBurst()
                    + ", charge=" + rounded(weapon.getChargeLevel())
                    + ", cooldown=" + rounded(weapon.getCooldownRemaining()));
            state.transitionInitialized = true;
        }
        state.windingUp = windingUp;
    }

    private static void logWeaponState(
            CombatEngineAPI engine,
            WeaponAPI weapon,
            State state,
            boolean windingUp,
            boolean active) {
        ShipAPI ship = weapon.getShip();
        WeaponGroupAPI group = ship == null ? null : ship.getWeaponGroupFor(weapon);
        AutofireAIPlugin autofire = group == null
                ? null : group.getAutofirePlugin(weapon);
        Vector2f targetPoint = autofire == null ? null : autofire.getTarget();
        ShipAPI targetShip = autofire == null ? null : autofire.getTargetShip();
        ShipAPI nearest = findNearestHostile(engine, ship);
        ShipAPI player = engine.getPlayerShip();
        float distance = targetPoint == null
                ? -1f : Misc.getDistance(weapon.getLocation(), targetPoint);
        float arcDistance = targetPoint == null
                ? -1f : weapon.distanceFromArc(targetPoint);
        int hostiles = 0;
        int inRange = 0;
        int awareInRange = 0;
        if (ship != null) {
            for (ShipAPI candidate : engine.getShips()) {
                if (!isHostile(ship, candidate)) continue;
                hostiles++;
                float surfaceDistance = Math.max(0f,
                        Misc.getDistance(weapon.getLocation(), candidate.getLocation())
                                - candidate.getCollisionRadius());
                if (surfaceDistance <= weapon.getRange()) {
                    inRange++;
                    if (engine.isAwareOf(ship.getOwner(), candidate)) awareInRange++;
                }
            }
        }
        Global.getLogger(DriftingWallBeamEffect.class).info(
                LOG_PREFIX + "beam state slot=" + slotId(weapon)
                + ", source=" + shipLabel(ship)
                + ", stationModule=" + (ship != null && ship.isStationModule())
                + ", parent=" + shipLabel(ship == null ? null : ship.getParentStation())
                + ", shipAI=" + className(ship == null ? null : ship.getShipAI())
                + ", controlsLocked=" + (ship != null && ship.controlsLocked())
                + ", holdFire=" + (ship != null && ship.isHoldFire())
                + ", shipTarget=" + shipLabel(ship == null ? null : ship.getShipTarget())
                + ", groupAutofire=" + (group != null && group.isAutofiring())
                + ", defaultWeaponAI="
                + (group != null && group.isUsingDefaultAI(weapon))
                + ", autofireAI=" + className(autofire)
                + ", shouldFire=" + (autofire != null && autofire.shouldFire())
                + ", autoTarget=" + shipLabel(targetShip)
                + ", targetPoint=" + vectorLabel(targetPoint)
                + ", targetDistance=" + rounded(distance)
                + ", distanceFromArc=" + rounded(arcDistance)
                + ", hostileShips=" + hostiles
                + ", hostilesInRange=" + inRange
                + ", awareHostilesInRange=" + awareInRange
                + ", nearestHostile=" + shipLabel(nearest)
                + ", nearestSurfaceDistance="
                + rounded(surfaceDistance(weapon, nearest))
                + ", awareOfNearest=" + (ship != null && nearest != null
                        && engine.isAwareOf(ship.getOwner(), nearest))
                + ", playerShip=" + shipLabel(player)
                + ", playerSurfaceDistance="
                + rounded(surfaceDistance(weapon, player))
                + ", currAngle=" + rounded(weapon.getCurrAngle())
                + ", arcFacing=" + rounded(weapon.getArcFacing())
                + ", arc=" + rounded(weapon.getArc())
                + ", range=" + rounded(weapon.getRange())
                + ", windingUp=" + windingUp
                + ", activeBeam=" + active
                + ", isFiring=" + weapon.isFiring()
                + ", inBurst=" + weapon.isInBurst()
                + ", charge=" + rounded(weapon.getChargeLevel())
                + ", cooldown=" + rounded(weapon.getCooldownRemaining())
                + ", disabled=" + weapon.isDisabled()
                + ", forceDisabled=" + weapon.isForceDisabled()
                + ", health=" + rounded(weapon.getCurrHealth()) + "/"
                + rounded(weapon.getMaxHealth()));
    }

    private static ShipAPI findNearestHostile(
            CombatEngineAPI engine, ShipAPI source) {
        if (source == null) return null;
        ShipAPI nearest = null;
        float best = Float.MAX_VALUE;
        for (ShipAPI candidate : engine.getShips()) {
            if (!isHostile(source, candidate)) continue;
            float distance = Misc.getDistance(
                    source.getLocation(), candidate.getLocation());
            if (distance < best) {
                nearest = candidate;
                best = distance;
            }
        }
        return nearest;
    }

    private static boolean isHostile(ShipAPI source, ShipAPI candidate) {
        return candidate != null && candidate != source
                && candidate.isAlive() && !candidate.isHulk()
                && candidate.getOwner() != source.getOwner()
                && candidate.getOwner() != 100;
    }

    private static float surfaceDistance(WeaponAPI weapon, ShipAPI target) {
        return weapon == null || target == null ? -1f : Math.max(0f,
                Misc.getDistance(weapon.getLocation(), target.getLocation())
                        - target.getCollisionRadius());
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

    private static float rounded(float value) {
        return Math.round(value * 10f) / 10f;
    }

    private static void updateTimeflowShimmer(
            WeaponAPI weapon,
            State state,
            float chargeLevel,
            boolean windingUp,
            boolean active) {
        float level;
        if (windingUp) {
            // Ease in slowly, then flare hard just before the beam is released.
            level = chargeLevel * chargeLevel;
        } else if (active) {
            level = 0.42f;
        } else {
            level = 0f;
        }

        if (level <= 0f) {
            weapon.setGlowAmount(0f, null);
            weapon.setWeaponGlowWidthMult(1f);
            weapon.setWeaponGlowHeightMult(1f);
            return;
        }

        float fastPulse = 0.88f + 0.12f * (float) Math.sin(state.shimmerTime * 19f);
        float slowPulse = 0.92f + 0.08f * (float) Math.sin(state.shimmerTime * 7f + 1.3f);
        float glow = Math.min(1f, (0.14f + 0.86f * level) * fastPulse);
        weapon.setGlowAmount(glow, TIMEFLOW_SHIMMER);

        // Slightly mismatched axes make the overlay quiver like vanilla timeflow jitter
        // without displacing the actual weapon or changing its aim.
        weapon.setWeaponGlowWidthMult(1f + level * 0.045f * fastPulse);
        weapon.setWeaponGlowHeightMult(1f + level * 0.065f * slowPulse);
    }

    private static boolean hasVisibleBeam(WeaponAPI weapon) {
        for (BeamAPI beam : weapon.getBeams()) {
            if (beam != null && beam.getBrightness() > 0.05f) return true;
        }
        return false;
    }

    private static void updateChargingLightning(float amount, CombatEngineAPI engine,
            WeaponAPI weapon, State state, float charge, boolean windingUp) {
        if (!windingUp) {
            state.lightningElapsed = 0f;
            return;
        }
        state.lightningElapsed += amount;
        float interval = 0.22f - charge * 0.14f;
        if (state.lightningElapsed < interval) return;
        state.lightningElapsed = 0f;
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
        Vector2f lateral = new Vector2f(-forward.y, forward.x);
        Vector2f pivot = weapon.getLocation();
        // Real visual arcs remain legible even when a glow overlay is washed
        // out by the bright sprite. This windup lightning deals no damage.
        float side = Math.random() < 0.5 ? -1f : 1f;
        Vector2f from = aroundTurretBase(pivot, forward, lateral,
                random(-100f, 30f), side * random(65f, 125f));
        Vector2f to = aroundTurretBase(pivot, forward, lateral,
                random(180f, 480f), side * random(12f, 40f));
        engine.spawnEmpArcVisual(from, weapon.getShip(), to, weapon.getShip(),
                8f + 10f * charge, TIMEFLOW_SHIMMER, Color.WHITE);
    }

    private static void arcFromBeam(CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI source = weapon.getShip();
        for (BeamAPI beam : weapon.getBeams()) {
            if (beam.getBrightness() <= 0.05f) continue;
            ShipAPI target = null;
            Vector2f origin = null;
            float closest = Float.MAX_VALUE;
            Vector2f direction = Vector2f.sub(beam.getTo(), beam.getFrom(), null);
            float lengthSquared = direction.lengthSquared();
            if (lengthSquared < 1f) continue;
            for (ShipAPI candidate : engine.getShips()) {
                if (!candidate.isAlive() || candidate.isHulk()
                        || candidate.isPhased() || candidate.getOwner() == source.getOwner()
                        || candidate.getOwner() == 100) continue;
                Vector2f offset = Vector2f.sub(candidate.getLocation(), beam.getFrom(), null);
                float along = Math.max(0f, Math.min(1f,
                        Vector2f.dot(offset, direction) / lengthSquared));
                Vector2f point = new Vector2f(beam.getFrom().x + direction.x * along,
                        beam.getFrom().y + direction.y * along);
                float distance = Math.max(0f, Vector2f.sub(candidate.getLocation(), point, null)
                        .length() - candidate.getCollisionRadius());
                if (distance <= 350f && distance < closest) {
                    target = candidate;
                    origin = point;
                    closest = distance;
                }
            }
            if (target != null) {
                engine.spawnEmpArc(source, origin, null, target, DamageType.ENERGY,
                        0f, 200f, 10000f, "tachyon_lance_emp_impact", 18f,
                        TIMEFLOW_SHIMMER, Color.WHITE);
            }
        }
    }

    /** Ensures the original naval beam receives its exact +50% shield modifier. */
    private static void installIsaShieldDamageListeners(WeaponAPI weapon) {
        for (BeamAPI beam : weapon.getBeams()) {
            ShipAPI target = shieldTarget(beam);
            if (target != null
                    && !target.hasListenerOfClass(IsaShieldDamageModifier.class)) {
                target.addListener(new IsaShieldDamageModifier());
            }
        }
    }

    /** Emits one half-strength, one-hop beam; EMP arcs never invoke this effect. */
    private static void refractFromEnemyShield(
            CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI source = weapon.getShip();
        if (source == null) return;
        for (BeamAPI beam : weapon.getBeams()) {
            ShipAPI primary = shieldTarget(beam);
            if (primary == null) continue;
            ShipAPI secondary = findRefractionTarget(
                    engine, source, primary, beam.getRayEndPrevFrame());
            if (secondary == null) continue;
            float dps = weapon.getDerivedStats() == null
                    ? 2000f : weapon.getDerivedStats().getDps();
            float damage = Math.max(1f, dps * REFRACTION_DAMAGE_FRACTION
                    * REFRACTION_INTERVAL);
            engine.spawnEmpArc(source, beam.getRayEndPrevFrame(), primary,
                    secondary, DamageType.ENERGY, damage, 0f,
                    REFRACTION_RANGE, "tachyon_lance_emp_impact", 28f,
                    TIMEFLOW_SHIMMER, Color.WHITE);
        }
    }

    private static ShipAPI shieldTarget(BeamAPI beam) {
        if (beam == null || beam.getBrightness() <= 0.05f
                || !(beam.getDamageTarget() instanceof ShipAPI)) return null;
        ShipAPI target = (ShipAPI) beam.getDamageTarget();
        Vector2f hit = beam.getRayEndPrevFrame();
        if (target.getShield() == null || !target.getShield().isOn()
                || hit == null || !target.getShield().isWithinArc(hit)) {
            return null;
        }
        return target;
    }

    private static ShipAPI findRefractionTarget(
            CombatEngineAPI engine,
            ShipAPI source,
            ShipAPI primary,
            Vector2f origin) {
        if (origin == null) return null;
        ShipAPI best = null;
        float bestDistance = Float.MAX_VALUE;
        for (ShipAPI candidate : engine.getShips()) {
            if (candidate == null || candidate == primary || candidate == source
                    || !candidate.isAlive() || candidate.isHulk()
                    || candidate.isPhased() || candidate.isFighter()
                    || candidate.isDrone()
                    || candidate.getOwner() == source.getOwner()
                    || candidate.getOwner() == 100) continue;
            float distance = Math.max(0f,
                    Misc.getDistance(origin, candidate.getLocation())
                            - candidate.getCollisionRadius());
            if (distance <= REFRACTION_RANGE && distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }
        return best;
    }

    private static final class IsaShieldDamageModifier
            implements DamageTakenModifier {
        @Override
        public String modifyDamageTaken(
                Object param,
                CombatEntityAPI target,
                DamageAPI damage,
                Vector2f point,
                boolean shieldHit) {
            if (!shieldHit || damage == null || !(param instanceof BeamAPI)) {
                return null;
            }
            BeamAPI beam = (BeamAPI) param;
            WeaponAPI weapon = beam.getWeapon();
            if (weapon == null || !NAVAL_WEAPON_ID.equals(weapon.getId())
                    || !IthacaResearchUpgrades.hasIsaRefraction(
                            beam.getSource())) return null;
            damage.getModifier().modifyMult(
                    ISA_SHIELD_DAMAGE_MODIFIER,
                    ISA_SHIELD_DAMAGE_MULTIPLIER);
            return ISA_SHIELD_DAMAGE_MODIFIER;
        }
    }

    private static void ventWeapon(CombatEngineAPI engine, WeaponAPI weapon) {
        ShipAPI ship = weapon.getShip();
        if (ship == null) return;

        Vector2f pivot = new Vector2f(weapon.getLocation());
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(weapon.getCurrAngle());
        Vector2f lateral = new Vector2f(-forward.y, forward.x);
        Vector2f shipVelocity = new Vector2f(ship.getVelocity());

        for (int i = 0; i < SMOKE_COUNT; i++) {
            Vector2f location = aroundTurretBase(
                    pivot,
                    forward,
                    lateral,
                    centeredRandom(105f),
                    centeredRandom(145f));
            float sideVelocity = random(-115f, 115f);
            float backVelocity = random(-95f, 30f);
            Vector2f velocity = combineVelocity(
                    shipVelocity, forward, lateral, backVelocity, sideVelocity);
            int shade = Math.round(random(72f, 118f));
            engine.addNebulaSmokeParticle(
                    location,
                    velocity,
                    random(150f, 340f),
                    random(1.35f, 2.25f),
                    random(0.08f, 0.20f),
                    random(0.32f, 0.62f),
                    random(3.2f, 6.2f),
                    new Color(shade, shade + 5, shade + 8, 175));
        }

        for (int i = 0; i < STEAM_COUNT; i++) {
            float sign = Math.random() < 0.5 ? -1f : 1f;
            Vector2f location = aroundTurretBase(
                    pivot,
                    forward,
                    lateral,
                    centeredRandom(70f),
                    sign * random(55f, 115f));
            float sideVelocity = sign * random(170f, 390f);
            float backVelocity = random(-145f, 15f);
            Vector2f velocity = combineVelocity(
                    shipVelocity, forward, lateral, backVelocity, sideVelocity);
            engine.addNebulaParticle(
                    location,
                    velocity,
                    random(105f, 235f),
                    random(1.7f, 2.8f),
                    random(0.04f, 0.10f),
                    random(0.18f, 0.36f),
                    random(1.4f, 3.0f),
                    new Color(202, 224, 225, 155));
        }

        for (int i = 0; i < JET_COUNT; i++) {
            float sign = Math.random() < 0.5 ? -1f : 1f;
            Vector2f location = aroundTurretBase(
                    pivot,
                    forward,
                    lateral,
                    centeredRandom(48f),
                    sign * random(60f, 105f));
            Vector2f velocity = combineVelocity(
                    shipVelocity,
                    forward,
                    lateral,
                    random(-75f, 25f),
                    sign * random(280f, 520f));
            engine.addSmoothParticle(
                    location,
                    velocity,
                    random(65f, 145f),
                    0.82f,
                    random(0.45f, 1.05f),
                    new Color(225, 242, 239, 210));
        }
    }

    private static Vector2f aroundTurretBase(
            Vector2f pivot,
            Vector2f forward,
            Vector2f lateral,
            float forwardOffset,
            float side) {
        return new Vector2f(
                pivot.x + forward.x * forwardOffset + lateral.x * side,
                pivot.y + forward.y * forwardOffset + lateral.y * side);
    }

    private static Vector2f combineVelocity(
            Vector2f shipVelocity,
            Vector2f forward,
            Vector2f lateral,
            float forwardSpeed,
            float lateralSpeed) {
        return new Vector2f(
                shipVelocity.x + forward.x * forwardSpeed + lateral.x * lateralSpeed,
                shipVelocity.y + forward.y * forwardSpeed + lateral.y * lateralSpeed);
    }

    private static float random(float minimum, float maximum) {
        return minimum + (maximum - minimum) * (float) Math.random();
    }

    /** Triangular distribution keeps most particles near the turret pivot. */
    private static float centeredRandom(float radius) {
        return (random(-1f, 1f) + random(-1f, 1f)) * 0.5f * radius;
    }

    private static final class State {
        private boolean active;
        private boolean windingUp;
        private boolean transitionInitialized;
        private boolean everFired;
        private float debugElapsed;
        private float shimmerTime;
        private int upgrades;
        private boolean isaRefraction;
        private float empElapsed;
        private float refractionElapsed;
        private float lightningElapsed;
    }
}
