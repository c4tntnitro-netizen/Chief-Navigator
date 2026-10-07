package chiefnavigator.abilities;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.TerrainAIFlags;
import com.fs.starfarer.api.impl.campaign.abilities.BaseToggleAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** A persistent stance that enables Sinni's terrain-assisted ambushes. */
public final class SinniAmbushStanceAbility extends BaseToggleAbility {
    public static final String ID = "chief_navigator_ambush_stance";

    @Override protected void activateImpl() { }
    @Override protected void applyEffect(float amount, float level) { }
    @Override protected void deactivateImpl() { }
    @Override protected void cleanupImpl() { }

    @Override
    public boolean isUsable() {
        return super.isUsable()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.DRIVE_MITES);
    }

    public static boolean isAmbushReady(CampaignFleetAPI fleet) {
        if (fleet == null || fleet.getAbility(ID) == null
                || !fleet.getAbility(ID).isActive()) return false;
        if (fleet.getContainingLocation() == null) return false;
        for (CampaignTerrainAPI terrain :
                fleet.getContainingLocation().getTerrainCopy()) {
            if (!terrain.getPlugin().containsEntity(fleet)) continue;
            if (terrain.getPlugin().hasAIFlag(
                        TerrainAIFlags.REDUCES_SENSOR_RANGE, fleet)
                    || terrain.getPlugin().hasAIFlag(
                        TerrainAIFlags.REDUCES_DETECTABILITY, fleet)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        tooltip.addTitle("Ambush Stance");
        tooltip.addPara("While this stance is active, engaging a hostile fleet "
                        + "from terrain that reduces your sensor range or "
                        + "detectability gives you the option to force a pursuit "
                        + "battle, as if the enemy were attempting to retreat.",
                10f, Misc.getHighlightColor(),
                "hostile fleet", "force a pursuit battle");
        tooltip.addPara("Suitable terrain includes nebulae and asteroid fields.",
                10f, Misc.getGrayColor(), "nebulae", "asteroid fields");
    }
}
