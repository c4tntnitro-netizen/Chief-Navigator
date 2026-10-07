package chiefnavigator.systems;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** Uses time freeze when the Facet has a live enemy close enough to exploit it. */
public final class OmegaTimeFreezeAI implements ShipSystemAIScript {
    private final IntervalUtil interval = new IntervalUtil(0.25f, 0.4f);
    private CombatEngineAPI engine;
    private ShipAPI ship;
    private ShipSystemAPI system;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags,
            CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir,
            Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null || engine.isPaused() || ship == null || system == null) return;
        interval.advance(amount);
        if (!interval.intervalElapsed()
                || system.getState() != ShipSystemAPI.SystemState.IDLE) return;
        if (target != null && target.isAlive() && !target.isHulk()
                && target.getOwner() != ship.getOwner()
                && Misc.getDistance(ship.getLocation(), target.getLocation()) <= 2200f) {
            ship.useSystem();
        }
    }
}
