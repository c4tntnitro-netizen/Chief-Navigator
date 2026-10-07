package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

/** Permanent marker for Domain drones authenticated by Menelaus. */
public final class DomainSecurityIFFHullmod extends BaseHullMod {
    public static final String HULLMOD_ID =
            "chief_navigator_domain_security_iff";

    @Override
    public void applyEffectsBeforeShipCreation(
            HullSize hullSize, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats == null ? null : stats.getVariant();
        if (variant != null) {
            variant.addTag(Tags.TAG_AUTOMATED_NO_PENALTY);
        }
    }
}
