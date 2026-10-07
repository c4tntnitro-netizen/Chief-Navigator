package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** Makes Gautama's numerous replenishing escorts emphatically disposable. */
public final class ScyllaFragileSwarmHullmod extends BaseHullMod {
    private static final float HULL_MULT = 0.25f;
    private static final float DAMAGE_TAKEN_MULT = 1.5f;

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getHullBonus().modifyMult(id, HULL_MULT);
        stats.getHullDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, DAMAGE_TAKEN_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        ship.setExplosionScale(0f);
        ship.setHulkChanceOverride(0f);
        ship.setDoNotRender(true);
    }
}
