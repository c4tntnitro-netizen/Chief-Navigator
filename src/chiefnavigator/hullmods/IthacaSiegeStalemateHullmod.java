package chiefnavigator.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** Legacy-named marker identifying ships in Ithaca's authored invasions. */
public final class IthacaSiegeStalemateHullmod extends BaseHullMod {
    public static final String HULLMOD_ID =
            "chief_navigator_ithaca_siege_stalemate";

    private static final String WALL_DEFENDER_KEY =
            "$chief_navigator_ithaca_siege_wall_defender";
    private static final String INVASION_SHIP_KEY =
            "$chief_navigator_ithaca_siege_invasion_ship";

    /** Retained for save and call-site compatibility; changes no combat stats. */
    public static void markWallDefender(ShipAPI ship) {
        if (ship == null) return;
        ship.setCustomData(WALL_DEFENDER_KEY, Boolean.TRUE);
    }

    /** Legacy no-op retained for already-compiled integrations. */
    public static void markCounterSiegeDefender(ShipAPI ship) {
        // Deliberately empty: all combat damage now remains unmodified.
    }

    /** Marks a combat-created replacement for final-battle side discovery. */
    public static void markInvasionShip(ShipAPI ship) {
        if (ship == null) return;
        ship.setCustomData(INVASION_SHIP_KEY, Boolean.TRUE);
    }

    public static boolean isWallDefender(CombatEntityAPI target) {
        if (!(target instanceof ShipAPI)) return false;
        ShipAPI ship = (ShipAPI) target;
        return Boolean.TRUE.equals(
                ship.getCustomData().get(WALL_DEFENDER_KEY));
    }

    public static boolean isStalemateInvasion(CombatEntityAPI target) {
        if (!(target instanceof ShipAPI)) return false;
        ShipAPI ship = (ShipAPI) target;
        return Boolean.TRUE.equals(
                    ship.getCustomData().get(INVASION_SHIP_KEY))
                || ship.getVariant() != null
                && ship.getVariant().hasHullMod(HULLMOD_ID);
    }
}
