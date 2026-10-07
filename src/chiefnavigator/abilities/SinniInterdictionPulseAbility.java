package chiefnavigator.abilities;

import chiefnavigator.campaign.DriveMiteEntityPlugin;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.abilities.InterdictionPulseAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** Lets Sinni turn the player's minnows onto enemies caught by the stock pulse. */
public final class SinniInterdictionPulseAbility extends InterdictionPulseAbility {
    @Override
    protected void activateImpl() {
        super.activateImpl();
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null && fleet.isPlayerFleet() && fleet.isInHyperspace()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.DRIVE_MITES)) {
            DriveMiteEntityPlugin.spawnSwarm(fleet, getRange(fleet), 5);
        }
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltip(tooltip, expanded);
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null && fleet.isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.DRIVE_MITES)) {
            tooltip.addPara("%s minnows wheel around your fleet's drive wake. "
                            + "Interdiction Pulse lets Sinni turn them on hostile fleets "
                            + "inside its range; they circle the target and eat its drive, "
                            + "halving maximum burn and acceleration for %s days. "
                            + "Minnows can only exist in hyperspace.", 10f,
                    Misc.getPositiveHighlightColor(), "five", "three");
        }
    }
}
