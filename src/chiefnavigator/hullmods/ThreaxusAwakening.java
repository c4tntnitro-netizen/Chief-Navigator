package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** The inert module-bearing shell releases a living, pursuing core. */
public final class ThreaxusAwakening extends BaseHullMod {
    public static final String ID = "chief_navigator_threaxus_awakening";
    public static final String DORMANT_TAG = ID + "_dormant";
    private static final String STATE_KEY = ID + "_state";
    private static final String DORMANT = ID + "_lock";
    private static final String AWAKE = ID + "_awake";
    private static final class Phase {
        boolean sawModules;
        boolean awake;
    }
    /** Keep the core damageable in native station-death bookkeeping. Setting
     * hullDamageTakenMult to zero kills an invulnerable parent with its last
     * active module; cancel incoming hits through the public listener instead. */
    private static final class CoreGuard implements DamageTakenModifier {
        private final ShipAPI core;
        CoreGuard(ShipAPI core) { this.core = core; }
        @Override
        public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage,
                Vector2f point, boolean shieldHit) {
            if (target != core || !isDormant(core)) return null;
            damage.getModifier().modifyMult(DORMANT, 0f);
            return DORMANT;
        }
    }

    public static boolean isDormant(ShipAPI ship) {
        return ship != null && ship.hasTag(DORMANT_TAG);
    }

    private static boolean isCore(ShipHullSpecAPI spec) {
        return spec != null && (ThreatxusSwarmCloud.HULL_ID.equals(spec.getHullId())
                || ThreatxusSwarmCloud.HULL_ID.equals(spec.getBaseHullId())
                || ThreatxusSwarmCloud.HULL_ID.equals(spec.getDParentHullId()));
    }

    private static void lock(MutableShipStatsAPI stats) {
        stats.getMaxSpeed().modifyMult(DORMANT, 0f);
        stats.getAcceleration().modifyMult(DORMANT, 0f);
        stats.getMaxTurnRate().modifyMult(DORMANT, 0f);
        stats.getTurnAcceleration().modifyMult(DORMANT, 0f);
        stats.getArmorDamageTakenMult().modifyMult(DORMANT, 0f);
        stats.getShieldDamageTakenMult().modifyMult(DORMANT, 0f);
        stats.getEmpDamageTakenMult().modifyMult(DORMANT, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(ShipAPI.HullSize size, MutableShipStatsAPI stats, String id) {
        if (stats != null && stats.getVariant() != null && isCore(stats.getVariant().getHullSpec())) lock(stats);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship != null && isCore(ship.getHullSpec())) {
            ship.addTag(DORMANT_TAG);
            ship.setCustomData(STATE_KEY, new Phase());
            if (!ship.hasListenerOfClass(CoreGuard.class)) ship.addListener(new CoreGuard(ship));
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || !isCore(ship.getHullSpec()) || engine == null || engine.isPaused()
                || amount <= 0f || !ship.isAlive() || ship.isHulk() || !engine.isEntityInPlay(ship)) return;
        Phase phase = (Phase) ship.getCustomData().get(STATE_KEY);
        if (phase == null) {
            phase = new Phase(); ship.setCustomData(STATE_KEY, phase); ship.addTag(DORMANT_TAG);
            if (!ship.hasListenerOfClass(CoreGuard.class)) ship.addListener(new CoreGuard(ship));
        }
        if (!phase.awake) {
            int alive = 0;
            for (ShipAPI module : ship.getChildModulesCopy()) {
                if (module == null) continue;
                phase.sawModules = true;
                if (module.isAlive() && !module.isHulk() && module.getParentStation() == ship) alive++;
            }
            // Empty deployment warmup callbacks are not a defeated shell.
            if (phase.sawModules && alive == 0) {
                phase.awake = true;
                awaken(ship);
                if (engine.getCombatUI() != null) engine.getCombatUI().addMessage(0,
                        "All modules destroyed. The core awakens!");
            } else {
                lock(ship.getMutableStats());
                // Parent hold-fire propagates to every native child module.
                // Disable only the core's own guns so its shell keeps fighting.
                for (WeaponAPI weapon : ship.getAllWeapons()) weapon.setForceNoFireOneFrame(true);
                ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM);
                ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK);
                if (ship.getShield() != null && ship.getShield().isOn()) ship.getShield().toggleOff();
                return;
            }
        }
        if (engine.getPlayerShip() != ship) hunt(ship, engine);
    }

    private static void awaken(ShipAPI ship) {
        MutableShipStatsAPI stats = ship.getMutableStats();
        stats.getMaxSpeed().unmodify(DORMANT);
        stats.getAcceleration().unmodify(DORMANT);
        stats.getMaxTurnRate().unmodify(DORMANT);
        stats.getTurnAcceleration().unmodify(DORMANT);
        stats.getHullDamageTakenMult().unmodify(DORMANT);
        stats.getArmorDamageTakenMult().unmodify(DORMANT);
        stats.getShieldDamageTakenMult().unmodify(DORMANT);
        stats.getEmpDamageTakenMult().unmodify(DORMANT);
        // Authored base10/2/2/6/10 becomes70/20/15/15/25. Native modifiers,
        // acceleration, turning and collision momentum still apply normally.
        stats.getMaxSpeed().modifyFlat(AWAKE, 60f);
        stats.getAcceleration().modifyFlat(AWAKE, 18f);
        stats.getDeceleration().modifyFlat(AWAKE, 13f);
        stats.getMaxTurnRate().modifyFlat(AWAKE, 9f);
        stats.getTurnAcceleration().modifyFlat(AWAKE, 15f);
        ship.removeTag(DORMANT_TAG);
    }

    private static void hunt(ShipAPI ship, CombatEngineAPI engine) {
        ShipAPI target = null; float nearest = Float.MAX_VALUE;
        for (ShipAPI other : engine.getShips()) {
            if (other == ship || other.isFighter() || !other.isAlive() || other.isHulk() || other.isPhased()
                    || other.getOwner() == ship.getOwner() || other.getOwner() < 0 || other.getOwner() > 1
                    || !engine.isEntityInPlay(other) || !engine.isAwareOf(ship.getOwner(), other)) continue;
            float distance = Misc.getDistance(ship.getLocation(), other.getLocation());
            if (distance < nearest) { nearest = distance; target = other; }
        }
        if (target == null) return;
        ship.setShipTarget(target);
        float desired = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
        float delta = ((desired - ship.getFacing()) % 360f + 540f) % 360f - 180f;
        if (Math.abs(delta) > 3f) {
            ShipCommand turn = delta > 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
            ship.blockCommandForOneFrame(delta > 0f ? ShipCommand.TURN_RIGHT : ShipCommand.TURN_LEFT);
            ship.giveCommand(turn, null, 0);
        } else {
            ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT);
            ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT);
        }
        ship.giveCommand(ShipCommand.ACCELERATE, null, 0);
    }
}
