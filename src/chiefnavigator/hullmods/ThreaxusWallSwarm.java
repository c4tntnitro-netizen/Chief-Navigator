package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

/** A local ammunition reserve for the three frigate-class wall modules. */
public final class ThreaxusWallSwarm extends BaseHullMod {
    public static final String HULL_ID = "chief_navigator_threaxus_wall";
    public static final String ID = "chief_navigator_threaxus_wall_swarm";
    @Override
    public void applyEffectsBeforeShipCreation(HullSize size, MutableShipStatsAPI stats, String id) {
        if (stats == null || stats.getVariant() == null
                || !HULL_ID.equals(stats.getVariant().getHullSpec().getHullId())) return;
        // Native frigate baseline20 becomes200; native Fragment Swarm still
        // owns filling, spending, regrowth and each launcher's concurrent cap.
        stats.getDynamic().getMod(Stats.FRAGMENT_SWARM_SIZE_MOD).modifyFlat(ID, 180f);
        stats.getDynamic().getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyMult(ID, 10f);
    }
}
