package chiefnavigator.systems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.impl.combat.threat.ThreatShipConstructionScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/** Gautama's ship system projects the former pair of swarm streams from its hull. */
public final class ExodusPlagueSystem extends BaseShipSystemScript {
    public static final String SYSTEM_ID = "chief_navigator_exodus_plague";
    public static final String AIM_TARGET = SYSTEM_ID + "_aim";
    public static final float ACTIVE_TIME = 5f;
    public static final int PACKETS_PER_SECOND = 40;
    public static final int PACKETS_PER_EMITTER = PACKETS_PER_SECOND * (int) ACTIVE_TIME;
    private static final float EDGE_TIME = 1f;
    private static final String EMISSION_KEY = SYSTEM_ID + "_emission";

    private static final class Emission {
        final Object[] emitters = {new Object(), new Object()};
        float elapsed;
        final int[] shots = new int[2];
        float[][] times;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null || engine.isPaused() || stats == null || !(stats.getEntity() instanceof ShipAPI)) return;
        ShipAPI ship = (ShipAPI) stats.getEntity();
        if (!isUsable(ship.getSystem(), ship)) return;
        Emission emission = (Emission) ship.getCustomData().get(EMISSION_KEY);
        if (emission == null) {
            emission = new Emission();
            ship.setCustomData(EMISSION_KEY, emission);
        }
        if (state != State.ACTIVE) {
            emission.elapsed = 0f;
            emission.shots[0] = emission.shots[1] = 0;
            emission.times = null;
            return;
        }
        float amount = engine.getElapsedInLastFrame() * stats.getTimeMult().getModifiedValue();
        if (amount <= 0f) return;
        if (emission.times == null) {
            emission.times = new float[][] {emissionTimes(ThreadLocalRandom.current()),
                    emissionTimes(ThreadLocalRandom.current())};
        }
        emission.elapsed = Math.min(ACTIVE_TIME, emission.elapsed + amount);
        Vector2f aim = aimPoint(ship, engine);
        for (int side = 0; side < 2; side++) {
            while (emission.shots[side] < PACKETS_PER_EMITTER
                    && emission.times[side][emission.shots[side]] <= emission.elapsed) {
                float time = emission.times[side][emission.shots[side]++];
                Vector2f origin = emitterOrigin(ship, side);
                float facing = aim == null ? ship.getFacing() : Misc.getAngleInDegrees(origin, aim);
                ExodusPlaguePacketEffect.launch(ship, emission.emitters[side], origin, facing, engine,
                        .08f + .92f * edgeEnvelope(time));
            }
        }
    }

    public static float edgeEnvelope(float time) {
        float t = Math.max(0f, Math.min(1f, Math.min(time, ACTIVE_TIME - time) / EDGE_TIME));
        return t * t * (3f - 2f * t);
    }

    private static float integratedEnvelope(float time) {
        time = Math.max(0f, Math.min(ACTIVE_TIME, time));
        if (time < EDGE_TIME) {
            float t = time / EDGE_TIME;
            return EDGE_TIME * (t * t * t - .5f * t * t * t * t);
        }
        if (time <= ACTIVE_TIME - EDGE_TIME) return time - .5f * EDGE_TIME;
        return ACTIVE_TIME - EDGE_TIME - integratedEnvelope(ACTIVE_TIME - time);
    }

    /** Jitter within ordered density bins preserves all 200 packets while softening both ends. */
    public static float[] emissionTimes(Random random) {
        float[] result = new float[PACKETS_PER_EMITTER];
        for (int i = 0; i < result.length; i++) {
            float area = (i + .2f + .6f * random.nextFloat()) / result.length * (ACTIVE_TIME - EDGE_TIME);
            float low = 0f, high = ACTIVE_TIME;
            for (int step = 0; step < 16; step++) {
                float middle = (low + high) * .5f;
                if (integratedEnvelope(middle) < area) low = middle; else high = middle;
            }
            result[i] = (low + high) * .5f;
        }
        return result;
    }

    public static Vector2f emitterOrigin(ShipAPI ship, int side) {
        Vector2f forward = Misc.getUnitVectorAtDegreeAngle(ship.getFacing());
        boolean assault = ship.getHullSpec() != null && ("chief_navigator_exodus_assault".equals(ship.getHullSpec().getHullId())
                || "chief_navigator_exodus_assault".equals(ship.getHullSpec().getBaseHullId())
                || "chief_navigator_exodus_assault".equals(ship.getHullSpec().getDParentHullId()));
        float along = assault ? 50f : 35f;
        float lateral = side == 0 ? 85f : -85f;
        if (assault) lateral = side == 0 ? 21f : -21f;
        return new Vector2f(ship.getLocation().x + forward.x * along - forward.y * lateral,
                ship.getLocation().y + forward.y * along + forward.x * lateral);
    }

    private static Vector2f aimPoint(ShipAPI ship, CombatEngineAPI engine) {
        if (engine.getPlayerShip() == ship && ship.getMouseTarget() != null
                && Misc.getDistance(ship.getLocation(), ship.getMouseTarget()) > 1f) return ship.getMouseTarget();
        Object saved = ship.getCustomData().get(AIM_TARGET);
        ShipAPI target = saved instanceof ShipAPI ? (ShipAPI) saved : ship.getShipTarget();
        return validTarget(ship, target, engine) ? target.getLocation() : null;
    }

    public static boolean validTarget(ShipAPI ship, ShipAPI target, CombatEngineAPI engine) {
        return target != null && target != ship && target.isAlive() && !target.isHulk() && !target.isPhased()
                && target.getOwner() != ship.getOwner() && target.getOwner() >= 0 && target.getOwner() <= 1
                && engine.isEntityInPlay(target) && engine.isAwareOf(ship.getOwner(), target)
                && Misc.getDistance(ship.getLocation(), target.getLocation()) <= ExodusPlaguePacketEffect.RANGE;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return canOperate(ship);
    }

    public static boolean canOperate(ShipAPI ship) {
        return ship != null && ship.isAlive() && !ship.isHulk() && !ship.isPhased()
                && !ship.hasTag(ThreatShipConstructionScript.SHIP_UNDER_CONSTRUCTION)
                && ship.getFluxTracker() != null && !ship.getFluxTracker().isOverloadedOrVenting();
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (stats != null && stats.getEntity() instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) stats.getEntity();
            Emission emission = (Emission) ship.getCustomData().get(EMISSION_KEY);
            if (emission != null) {
                emission.elapsed = 0f; emission.shots[0] = emission.shots[1] = 0; emission.times = null;
            }
        }
    }

    @Override
    public StatusData getStatusData(int index, State state, float effectLevel) {
        return index == 0 && state == State.ACTIVE ? new StatusData("twin swarm streams", false) : null;
    }
}
