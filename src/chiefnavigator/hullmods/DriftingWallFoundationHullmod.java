package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/** Structural hardening for the Drifting Wall's built-in weapon systems. */
public final class DriftingWallFoundationHullmod extends BaseHullMod {
    private static final float WEAPON_HEALTH_MULT = 20f;

    @Override
    public void applyEffectsBeforeShipCreation(
            HullSize hullSize,
            MutableShipStatsAPI stats,
            String id) {
        stats.getWeaponHealthBonus().modifyMult(id, WEAPON_HEALTH_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        IthacaSiegeStalemateHullmod.markWallDefender(ship);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;
        IthacaSiegeStalemateHullmod.markWallDefender(ship);
        for (ShipAPI module : ship.getChildModulesCopy()) {
            IthacaSiegeStalemateHullmod.markWallDefender(module);
        }
    }
}
