package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/** Hidden encounter-only fire-control package for FOB Ithaca's capital guns. */
public final class IthacaLongRangeEmplacement extends BaseHullMod {
    private static final float RANGE_MULTIPLIER = 10f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getBallisticWeaponRangeBonus().modifyMult(id, RANGE_MULTIPLIER);
        stats.getEnergyWeaponRangeBonus().modifyMult(id, RANGE_MULTIPLIER);
        stats.getMissileWeaponRangeBonus().modifyMult(id, RANGE_MULTIPLIER);
        stats.getSuppliesToRecover().modifyMult(id, 0f);
    }
}
