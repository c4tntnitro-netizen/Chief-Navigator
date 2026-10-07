package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Encounter-only controller for the standalone Last Light Omega core's time stop. */
public final class OmegaTimeStopCoreHullmod extends BaseHullMod {
    private static final String PLUGIN_KEY = "chief_navigator_omega_time_stop_plugin";

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize,
            com.fs.starfarer.api.combat.MutableShipStatsAPI stats,
            String id) {
        // The escorts are the armor. The command core itself is deliberately brittle.
        stats.getHullBonus().modifyMult(id, 0.55f);
        stats.getArmorBonus().modifyMult(id, 0.65f);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || engine == null || ship.getCustomData().containsKey(PLUGIN_KEY)) return;
        engine.addPlugin(new TimeStopPlugin(engine, ship));
        ship.setCustomData(PLUGIN_KEY, Boolean.TRUE);
    }

    private static final class TimeStopPlugin extends BaseEveryFrameCombatPlugin {
        private static final String TIME_KEY = "chief_navigator_omega_time_stop";
        private static final float TIME_MULT = 0.01f;
        private static final float FIRST_DELAY = 12f;
        private static final float COOLDOWN = 24f;
        private static final float WARNING = 2f;
        private static final float DURATION = 6f;
        private static final float CHARGE_SPEED = 460f;
        private static final String AMATERASU_HULL_ID = "zea_boss_amaterasu";

        private final CombatEngineAPI engine;
        private final ShipAPI core;
        private ShipAPI temporalShellImmune;
        private float timer = FIRST_DELAY;
        private Phase phase = Phase.WAITING;

        private enum Phase { WAITING, WARNING, STOPPED }

        private TimeStopPlugin(CombatEngineAPI engine, ShipAPI core) {
            this.engine = engine;
            this.core = core;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused()) return;
            if (!core.isAlive() || core.isHulk() || !engine.isEntityInPlay(core)) {
                releaseTime();
                engine.removePlugin(this);
                return;
            }

            float clockAmount = phase == Phase.STOPPED ? amount / TIME_MULT : amount;
            timer -= clockAmount;
            if (phase == Phase.WAITING && timer <= 0f) {
                phase = Phase.WARNING;
                timer = WARNING;
                Global.getSoundPlayer().playSound(
                        "chief_navigator_omega_time_freeze_charge", 1f, 1f,
                        core.getLocation(), core.getVelocity());
            } else if (phase == Phase.WARNING && timer <= 0f) {
                phase = Phase.STOPPED;
                timer = DURATION;
                Global.getSoundPlayer().playUISound(
                        "chief_navigator_omega_time_freeze", 1f, 1f);
            } else if (phase == Phase.STOPPED && timer <= 0f) {
                releaseTime();
                phase = Phase.WAITING;
                timer = COOLDOWN;
            }

            if (phase == Phase.WARNING) {
                core.setJitterUnder(this, new Color(255, 215, 80, 180),
                        1f - timer / WARNING, 18, 4f, 22f);
            } else if (phase == Phase.STOPPED) {
                maintainTimeStop();
            }
        }

        private void maintainTimeStop() {
            engine.getTimeMult().modifyMult(TIME_KEY, TIME_MULT);
            synchronizeShip(core);

            ShipAPI flagship = engine.getPlayerShip();
            if (temporalShellImmune == null && hasActiveTemporalShell(flagship)) {
                temporalShellImmune = flagship;
            }
            boolean shellActive = temporalShellImmune != null
                    && temporalShellImmune.isAlive() && !temporalShellImmune.isHulk();
            if (shellActive) {
                temporalShellImmune.getMutableStats().getTimeMult().modifyMult(
                        TIME_KEY, 1f / TIME_MULT);
            }
            for (ShipAPI candidate : engine.getShips()) {
                if (isAmaterasu(candidate)) cancelGlobalSlowdown(candidate);
            }

            if (flagship != null && flagship.isAlive() && !flagship.isHulk()) {
                chargeFlagship(flagship);
            }
            engine.maintainStatusForPlayerShip(TIME_KEY,
                    "graphics/icons/hullsys/temporal_shell.png",
                    "TIME STOP",
                    shellActive ? "Temporal Shell: free to move" : "all local time arrested",
                    !shellActive);
        }

        private void chargeFlagship(ShipAPI flagship) {
            Vector2f delta = Vector2f.sub(flagship.getLocation(), core.getLocation(), null);
            if (delta.lengthSquared() < 1f) return;
            float desired = Misc.getAngleInDegrees(core.getLocation(), flagship.getLocation());
            float turn = (desired - core.getFacing() + 540f) % 360f - 180f;
            core.giveCommand(turn >= 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, null, 0);
            core.giveCommand(ShipCommand.ACCELERATE, null, 0);
            delta.normalise();
            delta.scale(CHARGE_SPEED);
            Vector2f velocity = core.getVelocity();
            velocity.x += (delta.x - velocity.x) * 0.08f;
            velocity.y += (delta.y - velocity.y) * 0.08f;
            core.setShipTarget(flagship);
        }

        private boolean hasActiveTemporalShell(ShipAPI ship) {
            if (ship == null) return false;
            ShipSystemAPI system = ship.getSystem();
            if (system == null || !"temporalshell".equals(system.getId())) return false;
            ShipSystemAPI.SystemState state = system.getState();
            return system.isOn() || system.isActive() || system.isChargeup()
                    || system.isChargedown()
                    || state == ShipSystemAPI.SystemState.IN
                    || state == ShipSystemAPI.SystemState.ACTIVE
                    || state == ShipSystemAPI.SystemState.OUT;
        }

        private boolean isAmaterasu(ShipAPI ship) {
            return ship != null && ship.getHullSpec() != null
                    && AMATERASU_HULL_ID.equals(ship.getHullSpec().getBaseHullId());
        }

        private void synchronizeShip(ShipAPI ship) {
            ship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
            float localMult = ship.getMutableStats().getTimeMult().getModifiedValue();
            float globalMult = engine.getTimeMult().getModifiedValue();
            float net = localMult * globalMult;
            if (net > 0.000001f) {
                ship.getMutableStats().getTimeMult().modifyMult(TIME_KEY, 1f / net);
            }
        }

        private void cancelGlobalSlowdown(ShipAPI ship) {
            ship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
            float globalMult = engine.getTimeMult().getModifiedValue();
            if (globalMult > 0.000001f) {
                ship.getMutableStats().getTimeMult().modifyMult(TIME_KEY, 1f / globalMult);
            }
        }

        private void releaseTime() {
            engine.getTimeMult().unmodify(TIME_KEY);
            core.getMutableStats().getTimeMult().unmodify(TIME_KEY);
            ShipAPI flagship = engine.getPlayerShip();
            if (flagship != null) flagship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
            if (temporalShellImmune != null) {
                temporalShellImmune.getMutableStats().getTimeMult().unmodify(TIME_KEY);
                temporalShellImmune = null;
            }
            for (ShipAPI candidate : engine.getShips()) {
                if (isAmaterasu(candidate)) {
                    candidate.getMutableStats().getTimeMult().unmodify(TIME_KEY);
                }
            }
        }
    }
}
