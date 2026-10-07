package chiefnavigator.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;

/** Shared vanilla-style Fearless configuration for autonomous Guard drones. */
public final class GuardDroneAI {
    private static final String COMBAT_GUARD_HULL_PREFIX =
            "chief_navigator_combat_guard_";
    private static final String COMBAT_GUARD_HULL_TAG =
            "chief_navigator_domain_combat_guard";
    private static final String LAST_LIGHT_GUARDIAN_HULL_ID =
            "chief_navigator_last_light_guardian";

    private GuardDroneAI() { }

    public static boolean isGuardDrone(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String hullId = ship.getHullSpec().getHullId();
        return ship.getHullSpec().hasTag(COMBAT_GUARD_HULL_TAG)
                || hullId != null
                && (hullId.startsWith(COMBAT_GUARD_HULL_PREFIX)
                || LAST_LIGHT_GUARDIAN_HULL_ID.equals(hullId));
    }

    /**
     * Matches vanilla automated-ship Fearless behavior: Reckless targeting,
     * offensive strafing, no ordinary backing-off behavior, and unrestrained
     * burn-drive use.
     */
    public static ShipAIConfig createFearlessConfig() {
        ShipAIConfig config = new ShipAIConfig();
        configureFearless(config);
        return config;
    }

    public static void configureFearless(ShipAIConfig config) {
        if (config == null) return;
        config.personalityOverride = Personalities.RECKLESS;
        config.alwaysStrafeOffensively = true;
        config.backingOffWhileNotVentingAllowed = false;
        config.turnToFaceWithUndamagedArmor = false;
        config.burnDriveIgnoreEnemies = true;
    }

    /** Replaces a spawned drone's AI without assigning it a defensive post. */
    public static void installFreeMovingFearlessAI(ShipAPI ship) {
        if (ship == null) return;
        ShipAIPlugin ai = Global.getSettings().createDefaultShipAI(
                ship, createFearlessConfig());
        ship.setShipAI(ai);
        if (ai != null) ai.forceCircumstanceEvaluation();
    }
}
