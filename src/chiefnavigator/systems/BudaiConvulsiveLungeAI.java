package chiefnavigator.systems;

import chiefnavigator.hullmods.CharybdisDistortion;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.ConvulsiveLungeSystemAI;
import org.lwjgl.util.vector.Vector2f;

/** Native Maw lunge decisions, yielding only to the authored enrage sequence. */
public final class BudaiConvulsiveLungeAI extends ConvulsiveLungeSystemAI {
    @Override
    public void advance(float amount, Vector2f missileDangerDir,
            Vector2f collisionDangerDir, ShipAPI target) {
        if (ship == null || system == null || engine == null || engine.isPaused()
                || !ship.isAlive() || !engine.isEntityInPlay(ship)) {
            return;
        }
        if (ship.getCustomData().containsKey(
                CharybdisDistortion.ENRAGE_SEQUENCE_KEY)) {
            // The hullmod owns all four random destinations and their timing.
            return;
        }
        super.advance(amount, missileDangerDir, collisionDangerDir, target);
    }

    @Override
    public void giveCommand(Vector2f destination) {
        if (ship == null || system == null || engine == null || engine.isPaused()
                || !ship.isAlive() || !engine.isEntityInPlay(ship)
                || ship.getCustomData().containsKey(CharybdisDistortion.ENRAGE_SEQUENCE_KEY)) {
            return;
        }
        ship.removeCustomData(BudaiConvulsiveLungeSystem.TARGET_KEY);
        if (destination == null || !isSystemUsable() || !system.canBeActivated()
                || ship.getFluxTracker().isOverloadedOrVenting()) return;
        super.giveCommand(destination);
    }
}
