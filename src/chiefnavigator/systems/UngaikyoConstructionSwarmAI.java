package chiefnavigator.systems;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

/** Activates Ungaikyo's backward extraction when the Fabricator is pressured. */
public final class UngaikyoConstructionSwarmAI implements ShipSystemAIScript {
    private ShipAPI ship;
    private ShipSystemAPI system;
    private CombatEngineAPI engine;

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system,
            ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.engine = engine;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir,
            Vector2f collisionDangerDir, ShipAPI target) {
        if (amount <= 0f || engine == null || engine.isPaused()
                || ship == null || !ship.isAlive() || ship.isHulk()
                || system == null || !system.canBeActivated()) return;

        boolean closeEnemy = target != null && target.isAlive()
                && !target.isHulk() && target.getOwner() != ship.getOwner()
                && Misc.getDistance(ship.getLocation(), target.getLocation())
                        < ship.getCollisionRadius()
                                + target.getCollisionRadius() + 600f;
        if (ship.getFluxLevel() >= 0.7f || closeEnemy
                || missileDangerDir != null || collisionDangerDir != null) {
            ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
        }
    }
}
