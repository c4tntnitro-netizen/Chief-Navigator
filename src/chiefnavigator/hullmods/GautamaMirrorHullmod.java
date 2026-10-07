package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

/**
 * Hidden marker and zero-cost rules for Ungaikyo's combat-only reflections.
 *
 * <p>The encounter also tags every dynamic variant as unboardable and
 * unrecoverable. Keeping the cost rule in a hullmod makes every tactical and
 * post-battle query agree that the reflected member contributed no DP and
 * has no recovery cost.</p>
 */
public final class GautamaMirrorHullmod extends BaseHullMod {
    public static final String HULLMOD_ID =
            "chief_navigator_gautama_mirror";

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize,
            MutableShipStatsAPI stats,
            String id) {
        if (stats == null) return;
        stats.getSuppliesToRecover().modifyMult(id, 0f);
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD)
                .modifyMult(id, 0f);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship == null) return;
        ship.setHulkChanceOverride(0f);
        ship.setSpawnDebris(false);
    }

    @Override
    public boolean showInRefitScreenModPickerFor(ShipAPI ship) {
        return false;
    }
}
