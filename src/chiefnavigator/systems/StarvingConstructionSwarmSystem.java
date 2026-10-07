package chiefnavigator.systems;

import chiefnavigator.hullmods.StarvingThreatHullmod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript;
import java.util.HashMap;
import java.util.Map;

/** Construction Swarm that builds only Starving units for half the normal CR cost. */
public final class StarvingConstructionSwarmSystem
        extends ConstructionSwarmSystemScript {
    private static final float CR_COST_MULT = 0.5f;
    private static final Map<String, String> STARVING_VARIANTS = new HashMap<>();

    static {
        STARVING_VARIANTS.put("skirmish_unit_Type100",
                "chief_navigator_starving_skirmish_Type100");
        STARVING_VARIANTS.put("skirmish_unit_Type101",
                "chief_navigator_starving_skirmish_Type101");
        STARVING_VARIANTS.put("assault_unit_Type200",
                "chief_navigator_starving_assault_Type200");
        STARVING_VARIANTS.put("assault_unit_Type201",
                "chief_navigator_starving_assault_Type201");
        STARVING_VARIANTS.put("standoff_unit_Type300",
                "chief_navigator_starving_standoff_Type300");
        STARVING_VARIANTS.put("standoff_unit_Type301",
                "chief_navigator_starving_standoff_Type301");
        STARVING_VARIANTS.put("standoff_unit_Type302",
                "chief_navigator_starving_standoff_Type302");
        STARVING_VARIANTS.put("overseer_unit_Type250",
                "chief_navigator_starving_overseer_Type250");
        STARVING_VARIANTS.put("hive_unit_Type350",
                "chief_navigator_starving_hive_Type350");
    }

    @Override
    public SwarmConstructableVariant pickVariant(ShipAPI ship) {
        if (!isStarving(ship)) {
            return super.pickVariant(ship);
        }

        float originalCR = ship.getCurrentCR();
        SwarmConstructableVariant stock;
        try {
            // The stock picker filters by the full CR price. Doubling the
            // visible reserve lets it apply the same doctrine at half cost.
            ship.setCurrentCR(Math.min(1f, originalCR / CR_COST_MULT));
            stock = super.pickVariant(ship);
        } finally {
            ship.setCurrentCR(originalCR);
        }

        if (stock == null) {
            return null;
        }
        String starvingVariantId = STARVING_VARIANTS.get(stock.variantId);
        if (starvingVariantId == null) {
            return null;
        }

        SwarmConstructableVariant starving =
                new SwarmConstructableVariant(stock.type, starvingVariantId);
        starving.cr *= CR_COST_MULT;
        return starving;
    }

    @Override
    public boolean enoughCR(ShipSystemAPI system, ShipAPI ship) {
        if (!isStarving(ship)) {
            return super.enoughCR(system, ship);
        }
        init();
        return ship.getCurrentCR() >= MIN_CR * CR_COST_MULT;
    }

    private static boolean isStarving(ShipAPI ship) {
        return ship != null
                && ship.getVariant() != null
                && ship.getVariant().hasHullMod(StarvingThreatHullmod.HULLMOD_ID);
    }
}
