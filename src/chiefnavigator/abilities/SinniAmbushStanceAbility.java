package chiefnavigator.abilities;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.abilities.GoDarkAbility;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

/** Ambush Stance improves the player's ordinary Running Dark ability. */
public final class SinniAmbushStanceAbility extends GoDarkAbility {
    /** Retired standalone toggle, retained so its old instances can be removed. */
    public static final String ID = "chief_navigator_ambush_stance";
    public static final float AMBUSH_DETECTABILITY_MULT = 0.35f;

    private boolean hasAmbushBenefit(CampaignFleetAPI fleet) {
        return fleet != null && fleet.isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.DRIVE_MITES);
    }

    @Override
    protected void applyEffect(float amount, float level) {
        super.applyEffect(amount, level);
        CampaignFleetAPI fleet = getFleet();
        if (!hasAmbushBenefit(fleet)) return;
        float effectLevel = level < 1f ? 0f : level;
        float detectionMult = AMBUSH_DETECTABILITY_MULT
                * fleet.getStats().getDynamic().getValue(
                        Stats.GO_DARK_DETECTED_AT_MULT);
        // Replace the native ability's own modifier, preserving other bonuses
        // and vanilla movement, activation and cleanup behavior.
        fleet.getStats().getDetectedRangeMod().modifyMult(getModId(),
                1f + (detectionMult - 1f) * effectLevel,
                "Going dark (Ambush Stance)");
    }

    /** The replacement perk grants no forced pursuit battles. */
    public static boolean isAmbushReady(CampaignFleetAPI fleet) {
        return false;
    }

    @Override
    public void createTooltip(TooltipMakerAPI tooltip, boolean expanded) {
        String status = turnedOn ? " (on)" : " (off)";
        if (!Global.CODEX_TOOLTIP_MODE) {
            LabelAPI title = tooltip.addTitle(spec.getName() + status);
            title.highlightLast(status);
            title.setHighlightColor(Misc.getGrayColor());
        } else {
            tooltip.addSpacer(-10f);
        }
        float detectionMult = hasAmbushBenefit(getFleet())
                ? AMBUSH_DETECTABILITY_MULT : DETECTABILITY_MULT;
        String reduction = Math.round((1f - detectionMult) * 100f) + "%";
        tooltip.addPara("Turns off all non-essential systems, reducing the range "
                        + "at which the fleet can be detected by %s and forcing "
                        + "the fleet to %s*.",
                10f, Misc.getHighlightColor(), reduction, "move slowly");
        tooltip.addPara("*A fleet is considered slow-moving at a burn level "
                        + "of half that of its slowest ship.",
                Misc.getGrayColor(), 10f);
        addIncompatibleToTooltip(tooltip, expanded);
    }
}
