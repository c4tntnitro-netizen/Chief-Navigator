package chiefnavigator.abilities;

import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel;
import chiefnavigator.topography.SinniHyperspaceTopographyEventIntel.SinniStage;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.CampaignTerrainAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.impl.campaign.abilities.GenerateSlipsurgeAbility;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.ids.Terrain;
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2;
import com.fs.starfarer.api.impl.campaign.velfield.SlipstreamTerrainPlugin2.SlipstreamParams2;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

/** Keeps vanilla Generate Slipsurge behavior until Sinni's first tier is earned. */
public final class SinniGenerateSlipsurgeAbility extends GenerateSlipsurgeAbility {
    private static final float WIDTH_MULT = 2f;
    private static final float LENGTH_MULT = 3f;

    @Override
    public JumpPointAPI findGravityWell() {
        if (!SinniHyperspaceTopographyEventIntel.isTierActive(
                SinniStage.SLIPSURGE_MASTERY)) {
            return super.findGravityWell();
        }
        boolean previous = REQUIRE_GIANT_STARS_OR_STRONGER;
        try {
            REQUIRE_GIANT_STARS_OR_STRONGER = false;
            return super.findGravityWell();
        } finally {
            REQUIRE_GIANT_STARS_OR_STRONGER = previous;
        }
    }

    @Override
    protected void generateSlipstream() {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || !fleet.isPlayerFleet()
                || !SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.PURSUIT_BURN)) {
            super.generateSlipstream();
            return;
        }

        JumpPointAPI gravityWell = well;
        if (gravityWell == null || startLoc == null) return;

        float strength = getStrengthForGravityWell(gravityWell)
                * SLIPSURGE_STRENGTH_MULT;
        float angle = Misc.getAngleInDegrees(gravityWell.getLocation(), startLoc);
        float offset = 100f;
        Vector2f from = startLoc;

        SlipstreamParams2 params = new SlipstreamParams2();
        params.enteringSlipstreamTextOverride = "Entering Sinni's slipsurge";
        params.enteringSlipstreamTextDurationOverride = 0.1f;
        params.forceNoWindVisualEffectOnFleets = true;

        float width = 600f * WIDTH_MULT;
        float length = (1000f + strength * 500f) * LENGTH_MULT;
        params.burnLevel = Math.round(400f + strength * strength * 500f);
        params.accelerationMult = 20f + strength * strength * 280f;
        params.baseWidth = width;
        params.widthForMaxSpeed = 400f * WIDTH_MULT;
        params.widthForMaxSpeedMinMult = 0.34f;
        params.slowDownInWiderSections = true;
        params.minSpeed = Misc.getSpeedForBurnLevel(
                params.burnLevel - params.burnLevel / 8);
        params.maxSpeed = Misc.getSpeedForBurnLevel(
                params.burnLevel + params.burnLevel / 8);
        params.lineLengthFractionOfSpeed = 2000f
                / ((params.maxSpeed + params.minSpeed) * 0.5f);

        float lineFactor = 0.1f;
        params.minSpeed *= lineFactor;
        params.maxSpeed *= lineFactor;
        params.maxBurnLevelForTextureScroll = (int) (params.burnLevel * 0.1f);
        params.particleFadeInTime = 0.01f;
        params.areaPerParticle = 1000f;

        Vector2f to = Misc.getUnitVectorAtDegreeAngle(angle);
        to.scale(offset + fleet.getRadius() + length);
        Vector2f.add(to, startLoc, to);

        CampaignTerrainAPI slipstream = (CampaignTerrainAPI)
                gravityWell.getContainingLocation().addTerrain(Terrain.SLIPSTREAM, params);
        slipstream.addTag(Tags.SLIPSTREAM_VISIBLE_IN_ABYSS);
        slipstream.setLocation(from.x, from.y);

        SlipstreamTerrainPlugin2 plugin =
                (SlipstreamTerrainPlugin2) slipstream.getPlugin();
        float spacing = 100f;
        float increment = spacing / length;
        Vector2f difference = Vector2f.sub(to, from, new Vector2f());
        for (float fraction = 0f; fraction <= 1f; fraction += increment) {
            Vector2f point = new Vector2f(difference);
            point.scale(fraction);
            Vector2f.add(point, from, point);
            plugin.addSegment(point,
                    width - Math.min(600f, 600f * (float) Math.sqrt(fraction)));
        }

        plugin.recomputeIfNeeded();
        plugin.despawn(1.5f, 0.2f, new Random());
        slipstream.addScript(new SlipsurgeFadeInScript(plugin));
        fleet.addScript(new SlipsurgeEffectScript(fleet, plugin));
    }

    @Override
    public void addInitialDescription(TooltipMakerAPI tooltip, boolean expanded) {
        boolean attuned = SinniHyperspaceTopographyEventIntel.isTierActive(
                SinniStage.SLIPSURGE_MASTERY);
        boolean previous = REQUIRE_GIANT_STARS_OR_STRONGER;
        try {
            if (attuned) REQUIRE_GIANT_STARS_OR_STRONGER = false;
            super.addInitialDescription(tooltip, expanded);
        } finally {
            REQUIRE_GIANT_STARS_OR_STRONGER = previous;
        }
        if (getFleet() != null && getFleet().isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.PURSUIT_BURN)) {
            tooltip.addPara("Sinni's Slipsurge Mastery makes the generated current "
                    + "twice as wide and three times as long.", 10f,
                    Misc.getPositiveHighlightColor(),
                    "twice as wide", "three times as long");
        }
        if (getFleet() != null && getFleet().isPlayerFleet()
                && SinniHyperspaceTopographyEventIntel.isTierActive(
                        SinniStage.SLIPSURGE_MASTERY)) {
            tooltip.addPara("Sinni can generate a slipsurge from any gravity well, "
                            + "not only the most powerful ones.", 10f,
                    Misc.getPositiveHighlightColor(), "any gravity well");
        }
    }

    @Override
    public boolean addNotUsableReasonBeforeFuelCost(TooltipMakerAPI tooltip,
                                                     boolean expanded) {
        boolean previous = REQUIRE_GIANT_STARS_OR_STRONGER;
        try {
            if (SinniHyperspaceTopographyEventIntel.isTierActive(
                    SinniStage.SLIPSURGE_MASTERY)) {
                REQUIRE_GIANT_STARS_OR_STRONGER = false;
            }
            return super.addNotUsableReasonBeforeFuelCost(tooltip, expanded);
        } finally {
            REQUIRE_GIANT_STARS_OR_STRONGER = previous;
        }
    }
}
