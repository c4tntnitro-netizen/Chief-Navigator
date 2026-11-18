package chiefnavigator.abilities;

import chiefnavigator.quest.OdysseyExpanseSystem;
import chiefnavigator.quest.OrionKnotDistressResponse;
import chiefnavigator.quest.OrionKnotDistressResponse.Outcome;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.abilities.DistressCallAbility;

/** Most signals vanish; THREAT is more likely to answer than Spartan rescuers. */
public class OrionKnotDistressCallAbility extends DistressCallAbility {
    public static Outcome pickOutcome(double roll) {
        if (roll < 0.60) return Outcome.NOTHING;
        if (roll < 0.85) return Outcome.THREAT;
        if (roll < 0.89) return Outcome.SECOND_STRIKE;
        if (roll < 0.90) return Outcome.THIRD_STRIKE;
        return Outcome.SPARTAN;
    }

    protected double getResponseRoll() { return Math.random(); }

    @Override
    protected void applyEffect(float amount, float level) {
        CampaignFleetAPI fleet = getFleet();
        if (fleet == null || !fleet.isPlayerFleet()
                || !OdysseyExpanseSystem.isInside(fleet.getContainingLocation())) {
            super.applyEffect(amount, level);
            return;
        }
        if (performed) return;
        performed = true;
        // Keep the ordinary nearby repeat-call limit.
        if (wasUsedNearby(NEARBY_USE_TIMEOUT_DAYS,
                fleet.getLocationInHyperspace(), NEARBY_USE_RADIUS_LY)) return;
        Outcome outcome = pickOutcome(getResponseRoll());
        if (outcome != Outcome.NOTHING) {
            float delay = numTimesUsed == 0
                    ? 1f + 2f * (float) Math.random()
                    : 10f + 10f * (float) Math.random();
            Global.getSector().addScript(new OrionKnotDistressResponse(delay,
                    (StarSystemAPI) fleet.getContainingLocation(),
                    fleet.getLocation(), outcome));
        }
        numTimesUsed++;
        lastUsed = Global.getSector().getClock().getTimestamp();
        uses.add(new AbilityUseData(lastUsed, fleet.getLocationInHyperspace()),
                DAYS_TO_TRACK_USAGE);
    }
}
