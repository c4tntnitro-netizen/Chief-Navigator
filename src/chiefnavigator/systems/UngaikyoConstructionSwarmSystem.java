package chiefnavigator.systems;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.threat.ExtractionProtocolSystemScript;
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.State;

/**
 * Reflection construction remains owned by the encounter controller. The
 * active system gives this Fabricator an autonomous, backward Extraction
 * Protocol maneuver without opening a stock THREAT construction path.
 */
public final class UngaikyoConstructionSwarmSystem
        extends ExtractionProtocolSystemScript {
    @Override
    protected void init(ShipAPI ship) {
        // Unlike ordinary Extraction Protocol, this system does not require
        // an Overseer's Energy Lash to supply an activation charge.
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        return ship != null && ship.isAlive() && !ship.isHulk();
    }

    @Override
    public void applyImpl(
            ShipAPI ship, MutableShipStatsAPI stats, String id,
            State state, float effectLevel) {
        super.applyImpl(ship, stats, id, state, effectLevel);
        if (state == State.OUT && effectLevel > 0f) vented = true;
        if (effectLevel > 0f && (state == State.IN || state == State.ACTIVE)) {
            ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
            ship.blockCommandForOneFrame(ShipCommand.ACCELERATE);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
    }
}
