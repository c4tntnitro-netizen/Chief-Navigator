package chiefnavigator.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseCombatLayeredRenderingPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ViewportAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;
import com.fs.starfarer.api.util.Misc;
import java.util.EnumSet;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

/** Stops local combat time while leaving the activating Facet synchronized. */
public final class OmegaTimeFreezeSystem extends BaseShipSystemScript {
    private static final String TIME_KEY = "chief_navigator_omega_time_freeze_system";
    private static final String VISUAL_KEY = "chief_navigator_omega_time_freeze_visual";
    private static final String EFFECT_KEY = "chief_navigator_omega_time_freeze_level";
    private static final String SOUND_KEY = "chief_navigator_omega_time_freeze_sound_played";
    private static final String IMMUNITY_VISUAL_KEY =
            "chief_navigator_omega_time_freeze_immune";
    private static final String AMATERASU_HULL_ID = "zea_boss_amaterasu";
    private static final float MIN_TIME_MULT = 0.01f;
    private static final float MANEUVER_MULT = 5f;
    private ShipAPI temporalShellImmune;

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI)) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;

        ShipAPI source = (ShipAPI) stats.getEntity();
        if (effectLevel > 0f && !source.getCustomData().containsKey(SOUND_KEY)) {
            Global.getSoundPlayer().playUISound(
                    "chief_navigator_omega_time_freeze", 1f, 1f);
            source.setCustomData(SOUND_KEY, Boolean.TRUE);
        }
        float globalMult = 1f - (1f - MIN_TIME_MULT) * effectLevel;
        engine.getTimeMult().modifyMult(TIME_KEY, globalMult);

        // Compensate against the complete aggregate global time multiplier, not
        // just this system's modifier. Temporal Shell also changes global time.
        synchronizeShip(source, engine);

        float maneuverMult = 1f + (MANEUVER_MULT - 1f) * effectLevel;
        stats.getMaxSpeed().modifyMult(id, maneuverMult);
        stats.getAcceleration().modifyMult(id, maneuverMult);
        stats.getDeceleration().modifyMult(id, maneuverMult);
        stats.getMaxTurnRate().modifyMult(id, maneuverMult);
        stats.getTurnAcceleration().modifyMult(id, maneuverMult);

        source.setCustomData(EFFECT_KEY, effectLevel);
        if (!source.getCustomData().containsKey(VISUAL_KEY)) {
            engine.addLayeredRenderingPlugin(new TimeFreezeDimming(source, engine));
            source.setCustomData(VISUAL_KEY, Boolean.TRUE);
        }

        ShipAPI flagship = engine.getPlayerShip();
        if (temporalShellImmune == null && hasActiveTemporalShell(flagship)) {
            temporalShellImmune = flagship;
            temporalShellImmune.setCustomData(IMMUNITY_VISUAL_KEY, Boolean.TRUE);
        }
        if (temporalShellImmune != null && temporalShellImmune.isAlive()
                && !temporalShellImmune.isHulk()) {
            // Cancel this freeze's multiplier directly. Temporal Shell's own
            // local/global pair then cancels itself without modifier-order risk.
            temporalShellImmune.getMutableStats().getTimeMult().modifyMult(
                    TIME_KEY, 1f / globalMult);
        }
        for (ShipAPI candidate : engine.getShips()) {
            if (isAmaterasu(candidate)) cancelGlobalSlowdown(candidate, engine);
        }

        if (effectLevel >= 0.8f) chargeTarget(source, engine);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine != null) {
            engine.getTimeMult().unmodify(TIME_KEY);
            ShipAPI flagship = engine.getPlayerShip();
            if (flagship != null) flagship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
            if (temporalShellImmune != null) {
                temporalShellImmune.getMutableStats().getTimeMult().unmodify(TIME_KEY);
                temporalShellImmune.removeCustomData(IMMUNITY_VISUAL_KEY);
                temporalShellImmune = null;
            }
            for (ShipAPI candidate : engine.getShips()) {
                if (isAmaterasu(candidate)) {
                    candidate.getMutableStats().getTimeMult().unmodify(TIME_KEY);
                }
            }
        }
        if (stats != null) {
            stats.getTimeMult().unmodify(TIME_KEY);
            stats.getMaxSpeed().unmodify(id);
            stats.getAcceleration().unmodify(id);
            stats.getDeceleration().unmodify(id);
            stats.getMaxTurnRate().unmodify(id);
            stats.getTurnAcceleration().unmodify(id);
            if (stats.getEntity() instanceof ShipAPI) {
                ShipAPI source = (ShipAPI) stats.getEntity();
                source.setCustomData(EFFECT_KEY, 0f);
                source.removeCustomData(SOUND_KEY);
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        if (index != 0) return null;
        return new StatusData("local time arrested", false);
    }

    private static boolean hasActiveTemporalShell(ShipAPI ship) {
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

    private static boolean isAmaterasu(ShipAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && AMATERASU_HULL_ID.equals(ship.getHullSpec().getBaseHullId());
    }

    private static void synchronizeShip(ShipAPI ship, CombatEngineAPI engine) {
        if (ship == null) return;
        ship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
        float otherLocalMult = ship.getMutableStats().getTimeMult().getModifiedValue();
        float aggregateGlobalMult = engine.getTimeMult().getModifiedValue();
        float netWithoutCorrection = otherLocalMult * aggregateGlobalMult;
        if (netWithoutCorrection > 0.000001f) {
            ship.getMutableStats().getTimeMult().modifyMult(
                    TIME_KEY, 1f / netWithoutCorrection);
        }
    }

    private static void cancelGlobalSlowdown(ShipAPI ship, CombatEngineAPI engine) {
        ship.getMutableStats().getTimeMult().unmodify(TIME_KEY);
        float aggregateGlobalMult = engine.getTimeMult().getModifiedValue();
        if (aggregateGlobalMult > 0.000001f) {
            ship.getMutableStats().getTimeMult().modifyMult(
                    TIME_KEY, 1f / aggregateGlobalMult);
        }
    }

    private static void chargeTarget(ShipAPI source, CombatEngineAPI engine) {
        ShipAPI target = source.getShipTarget();
        if (!isValidTarget(source, target)) target = engine.getPlayerShip();
        if (!isValidTarget(source, target)) {
            float closest = Float.MAX_VALUE;
            for (ShipAPI candidate : engine.getShips()) {
                if (!isValidTarget(source, candidate)) continue;
                float distance = Misc.getDistance(source.getLocation(), candidate.getLocation());
                if (distance < closest) {
                    closest = distance;
                    target = candidate;
                }
            }
        }
        if (!isValidTarget(source, target)) return;
        source.setShipTarget(target);
        float desired = Misc.getAngleInDegrees(source.getLocation(), target.getLocation());
        float turn = (desired - source.getFacing() + 540f) % 360f - 180f;
        source.giveCommand(turn >= 0f ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT, null, 0);
        source.giveCommand(ShipCommand.ACCELERATE, null, 0);
    }

    private static boolean isValidTarget(ShipAPI source, ShipAPI target) {
        return target != null && target.isAlive() && !target.isHulk()
                && target.getOwner() != source.getOwner() && target.getOwner() != 100;
    }

    /** Darkens the combat world while leaving the HUD readable. */
    private static final class TimeFreezeDimming extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI source;
        private final CombatEngineAPI engine;

        private TimeFreezeDimming(ShipAPI source, CombatEngineAPI engine) {
            this.source = source;
            this.engine = engine;
        }

        @Override
        public void init(CombatEntityAPI entity) {
            super.init(entity);
            if (this.entity != null) this.entity.getLocation().set(source.getLocation());
        }

        @Override
        public void advance(float amount) {
            if (entity != null && source != null) entity.getLocation().set(source.getLocation());
        }

        @Override
        public boolean isExpired() {
            return source == null || !engine.isEntityInPlay(source);
        }

        @Override
        public float getRenderRadius() {
            return Float.MAX_VALUE;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.JUST_BELOW_WIDGETS);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {
            if (layer != CombatEngineLayers.JUST_BELOW_WIDGETS || isExpired()) return;
            Object value = source.getCustomData().get(EFFECT_KEY);
            float effectLevel = value instanceof Number ? ((Number) value).floatValue() : 0f;
            if (effectLevel <= 0f) return;

            float left = viewport.getLLX();
            float bottom = viewport.getLLY();
            float right = left + viewport.getVisibleWidth();
            float top = bottom + viewport.getVisibleHeight();
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(0.015f, 0.025f, 0.06f, 0.80f * effectLevel);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2f(left, bottom);
            GL11.glVertex2f(right, bottom);
            GL11.glVertex2f(right, top);
            GL11.glVertex2f(left, top);
            GL11.glEnd();
            GL11.glPopAttrib();

            renderShipAboveOverlay(source);
            ShipAPI flagship = engine.getPlayerShip();
            if (flagship != source && flagship != null
                    && (flagship.getCustomData().containsKey(IMMUNITY_VISUAL_KEY)
                            || hasActiveTemporalShell(flagship))) {
                renderShipAboveOverlay(flagship);
            }
        }

        private void renderShipAboveOverlay(ShipAPI ship) {
            if (ship == null || ship.getSpriteAPI() == null || !ship.isAlive()) return;
            ship.getSpriteAPI().renderAtCenter(ship.getLocation().x, ship.getLocation().y);
        }
    }
}
