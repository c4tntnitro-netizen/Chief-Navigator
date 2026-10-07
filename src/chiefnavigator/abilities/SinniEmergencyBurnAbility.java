package chiefnavigator.abilities;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.abilities.EmergencyBurnAbility;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** Adds a fivefold hostile-pursuit boost to Emergency Burn at Sinni's last tier. */
public final class SinniEmergencyBurnAbility extends EmergencyBurnAbility {
    private static final String PURSUIT_MOD_ID =
            "chief_navigator_sinni_emergency_pursuit";
    private static final float PURSUIT_SPEED_MULT = 5f;

    @Override
    protected void applyEffect(float amount, float level) {
        super.applyEffect(amount, level);
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null) return;

        if (level > 0f && isPursuingHostile(fleet)
                && fleet.isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.PURSUIT_BURN)) {
            fleet.getStats().getFleetwideMaxBurnMod().modifyMult(
                    PURSUIT_MOD_ID, PURSUIT_SPEED_MULT,
                    "Sinni's pursuit burn");
            fleet.getStats().getAccelerationMult().modifyMult(
                    PURSUIT_MOD_ID, PURSUIT_SPEED_MULT,
                    "Sinni's pursuit burn");
        } else {
            unapplyPursuitBoost(fleet);
        }
    }

    private boolean isPursuingHostile(CampaignFleetAPI fleet) {
        SectorEntityToken target = fleet.getInteractionTarget();
        if (!(target instanceof CampaignFleetAPI)) return false;
        CampaignFleetAPI targetFleet = (CampaignFleetAPI) target;
        return fleet.isHostileTo(targetFleet) || targetFleet.isHostileTo(fleet);
    }

    private void unapplyPursuitBoost(CampaignFleetAPI fleet) {
        fleet.getStats().getFleetwideMaxBurnMod().unmodify(PURSUIT_MOD_ID);
        fleet.getStats().getAccelerationMult().unmodify(PURSUIT_MOD_ID);
    }

    @Override
    protected void cleanupImpl() {
        CampaignFleetAPI fleet = getFleet();
        super.cleanupImpl();
        if (fleet != null) unapplyPursuitBoost(fleet);
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        super.createTooltip(tooltip, expanded);
        CampaignFleetAPI fleet = getFleet();
        if (fleet != null && fleet.isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.PURSUIT_BURN)) {
            tooltip.addPara("While pursuing a hostile fleet, Sinni multiplies "
                            + "Emergency Burn's maximum speed and acceleration by %s.",
                    10f, Misc.getPositiveHighlightColor(), "five");
        }
    }
}
