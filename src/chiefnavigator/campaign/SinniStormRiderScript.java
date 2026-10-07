package chiefnavigator.campaign;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

/** Applies Sinni's final-tier storm resistance while the fleet is in hyperspace. */
public final class SinniStormRiderScript implements EveryFrameScript {
    private static final String MOD_ID = "chief_navigator_storm_rider";

    @Override public boolean isDone() { return false; }
    @Override public boolean runWhilePaused() { return false; }

    @Override
    public void advance(float amount) {
        if (Global.getSector() == null || Global.getSector().getPlayerFleet() == null) return;
        boolean active = Global.getSector().getPlayerFleet().isInHyperspace()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.EXTENDED_TOPOGRAPHIC_DATA);
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet()
                .getFleetData().getMembersListCopy()) {
            if (active) {
                member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT)
                        .modifyMult(MOD_ID, 0.5f);
            } else {
                member.getStats().getDynamic().getStat(Stats.CORONA_EFFECT_MULT)
                        .unmodify(MOD_ID);
            }
        }
    }
}
