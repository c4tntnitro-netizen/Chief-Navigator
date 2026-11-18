package chiefnavigator.systems;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** Activates the plague against a visible enemy, independently of Extraction Protocol. */
public final class ExodusPlagueAI implements ShipSystemAIScript {
    private final IntervalUtil interval = new IntervalUtil(.15f, .25f);
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship; this.system = system; this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused() || amount <= 0f || ship == null || system == null
                || !ExodusPlagueSystem.canOperate(ship)) return;
        interval.advance(amount);
        if (!interval.intervalElapsed() || system.getState() != ShipSystemAPI.SystemState.IDLE
                || !system.canBeActivated()) return;
        ShipAPI best = ExodusPlagueSystem.validTarget(ship, target, engine) ? target : null;
        if (best == null && ExodusPlagueSystem.validTarget(ship, ship.getShipTarget(), engine)) best = ship.getShipTarget();
        if (best == null) {
            float closest = Float.MAX_VALUE;
            for (ShipAPI candidate : engine.getShips()) {
                if (!ExodusPlagueSystem.validTarget(ship, candidate, engine)) continue;
                float distance = Misc.getDistance(ship.getLocation(), candidate.getLocation());
                if (distance < closest) { best = candidate; closest = distance; }
            }
        }
        if (best != null) { ship.setCustomData(ExodusPlagueSystem.AIM_TARGET, best); ship.useSystem(); }
    }
}
