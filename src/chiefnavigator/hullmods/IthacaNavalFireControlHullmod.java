package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;

/** Lets fixed colossal naval emplacements acquire targets across the battle map. */
public final class IthacaNavalFireControlHullmod extends BaseHullMod {
    public static final float SIGHT_RADIUS_BONUS = 30000f;

    @Override
    public void applyEffectsBeforeShipCreation(
            HullSize hullSize,
            MutableShipStatsAPI stats,
            String id) {
        stats.getSightRadiusMod().modifyFlat(id, SIGHT_RADIUS_BONUS);
    }
}
