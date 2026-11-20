package chiefnavigator.hullmods;

import chiefnavigator.weapons.IthacaResearchUpgrades;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/** Selects immutable, data-authored built-in wings without modifying shared hull specs. */
public final class DomaindessyAirGroup extends BaseHullMod {
    public static final String ROOT = "chief_navigator_odyssey_ith";
    public static final String LIGHTNING = "chief_navigator_lightning_wing";
    public static final String POSEIDON = "chief_navigator_poseidon_wing";
    private static final Set<String> HULLS = Set.of(ROOT, ROOT+"_interception", ROOT+"_siege",
            ROOT+"_interception_siege", ROOT+"_poseidon", ROOT+"_interception_poseidon");
    private final Set<ShipVariantAPI> failed = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<ShipVariantAPI, Boolean>()));

    public static boolean owns(ShipHullSpecAPI spec) {
        if (spec == null) return false;
        String id = spec.getHullId(), parent = spec.getDParentHullId();
        return (id != null && HULLS.contains(id)) || (parent != null && HULLS.contains(parent));
    }

    public static String profile(int mask) {
        boolean interception = IthacaResearchUpgrades.hasUpgrade(mask, IthacaResearchUpgrades.INTERCEPT_MATRIX);
        boolean siege = IthacaResearchUpgrades.hasUpgrade(mask, IthacaResearchUpgrades.MUNITIONS_COMPILER);
        boolean poseidon = siege && IthacaResearchUpgrades.hasUpgrade(mask, IthacaResearchUpgrades.AUTOLOADER_CORE);
        return ROOT + (interception ? "_interception" : "") + (poseidon ? "_poseidon" : siege ? "_siege" : "");
    }

    public static void configure(ShipVariantAPI variant, int mask, SettingsAPI settings) {
        if (variant == null || !owns(variant.getHullSpec())) return;
        ShipHullSpecAPI original = variant.getHullSpec();
        String id = profile(mask);
        ShipHullSpecAPI target = settings.getHullSpec(id);
        if (original.isDHull()) {
            target = id.equals(original.getDParentHullId()) ? original : null;
            if (target == null) {
                for (ShipHullSpecAPI candidate : settings.getAllShipHullSpecs()) {
                    if (candidate.isDHull() && id.equals(candidate.getDParentHullId())) {
                        target = candidate;
                        break;
                    }
                }
            }
        }
        if (target == null || target.getFighterBays() != 4 || target.getBuiltInWings().size() != 4)
            throw new IllegalStateException("Missing fixed air-group hull profile: " + id);
        for (String wing : target.getBuiltInWings()) {
            if (settings.getFighterWingSpec(wing) == null)
                throw new IllegalStateException("Missing built-in wing: " + wing);
        }
        // Only the owned variant changes. Settings' shared specs are never edited.
        try {
            boolean refresh = original != target;
            for (int bay = 0; !refresh && bay < 4; bay++) {
                refresh = !target.getBuiltInWings().get(bay).equals(variant.getWingId(bay));
            }
            if (original != target) variant.setHullSpecAPI(target);
            if (refresh) variant.refreshBuiltInWings();
            // Clear only the retired built-in lasers on existing prototype variants.
            // Conventional player-fitted weapons in either large mount remain intact.
            for (String slot : new String[]{"WS 002", "WS 003"}) {
                if (IthacaResearchUpgrades.ITHACA_ODYSSEY_LASER_ID.equals(variant.getWeaponId(slot)))
                    variant.clearSlot(slot);
            }
        } catch (RuntimeException | LinkageError failure) {
            try {
                variant.setHullSpecAPI(original);
                variant.refreshBuiltInWings();
            } catch (RuntimeException | LinkageError cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize size, MutableShipStatsAPI stats, String id) {
        ShipVariantAPI variant = stats == null ? null : stats.getVariant();
        if (variant == null || !owns(variant.getHullSpec()) || failed.contains(variant)) return;
        try {
            configure(variant, IthacaResearchUpgrades.getCampaignMask(), Global.getSettings());
        } catch (RuntimeException | LinkageError failure) {
            if (failed.add(variant)) Global.getLogger(DomaindessyAirGroup.class).error(
                    "[DOMAINDESSY] Air-group configuration disabled for variant " + variant.getHullVariantId(), failure);
        }
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship != null && owns(ship.getHullSpec());
    }
}
