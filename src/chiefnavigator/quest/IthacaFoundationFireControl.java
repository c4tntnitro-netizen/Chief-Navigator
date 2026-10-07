package chiefnavigator.quest;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponGroupAPI;
import java.util.List;

/** Shared vanilla-AI activation for direct-weapon Wall foundations. */
final class IthacaFoundationFireControl {
    private static final String LOG_PREFIX = "[ITHACA_NAVAL] ";
    private static final String DESTROYED_NAVAL =
            "chief_navigator_drifting_wall_colossal_destroyed";
    private static final String DESTROYED_ANNIHILATOR =
            "chief_navigator_drifting_wall_annihilator_destroyed";

    private IthacaFoundationFireControl() { }

    /**
     * Keeps the foundation operable without choosing a target or forcing a
     * shot. Unlike ordinary station parents, these foundations mount their
     * naval weapons directly, so they need their own default ship AI and live
     * autofire groups rather than relying on child-module AIs.
     */
    static void ensureActive(ShipAPI foundation) {
        if (foundation == null) return;
        foundation.setControlsLocked(false);
        ShipAIPlugin ai = foundation.getShipAI();
        boolean changed = false;
        if (ai == null) {
            foundation.resetDefaultAI();
            ai = foundation.getShipAI();
            Global.getLogger(IthacaFoundationFireControl.class).info(
                    LOG_PREFIX + "fire-control resetDefaultAI hull="
                    + hullId(foundation) + ", result=" + className(ai));
            changed = true;
        }
        List<WeaponGroupAPI> groups = foundation.getWeaponGroupsCopy();
        if (groups == null) {
            Global.getLogger(IthacaFoundationFireControl.class).warn(
                    LOG_PREFIX + "fire-control has null weapon-group list hull="
                    + hullId(foundation));
            return;
        }
        for (int i = 0; i < groups.size(); i++) {
            WeaponGroupAPI group = groups.get(i);
            if (group == null) continue;
            if (!hasOperationalWeapon(group)) {
                if (group.isAutofiring()) {
                    group.toggleOff();
                    Global.getLogger(IthacaFoundationFireControl.class).info(
                            LOG_PREFIX + "fire-control toggled inert group off hull="
                            + hullId(foundation) + ", group=" + i);
                    changed = true;
                }
                continue;
            }
            if (!group.isAutofiring()) {
                group.toggleOn();
                Global.getLogger(IthacaFoundationFireControl.class).info(
                        LOG_PREFIX + "fire-control toggled live group on hull="
                        + hullId(foundation) + ", group=" + i);
                changed = true;
            }
        }
        if (changed && ai != null) {
            ai.forceCircumstanceEvaluation();
            Global.getLogger(IthacaFoundationFireControl.class).info(
                    LOG_PREFIX + "fire-control forced AI evaluation hull="
                    + hullId(foundation) + ", ai=" + className(ai));
        }
    }

    /** Restores a live mount, while keeping authored wreck fixtures inert. */
    static void restoreWeapon(WeaponAPI weapon) {
        if (weapon == null) return;
        if (!isOperationalWeapon(weapon)) {
            weapon.stopFiring();
            weapon.setForceDisabled(true);
            return;
        }
        weapon.setForceDisabled(false);
        weapon.setCurrHealth(weapon.getMaxHealth());
    }

    private static boolean hasOperationalWeapon(WeaponGroupAPI group) {
        List<WeaponAPI> weapons = group.getWeaponsCopy();
        if (weapons == null) return false;
        for (WeaponAPI weapon : weapons) {
            if (isOperationalWeapon(weapon)) return true;
        }
        return false;
    }

    private static boolean isOperationalWeapon(WeaponAPI weapon) {
        if (weapon == null) return false;
        String id = weapon.getId();
        return !DESTROYED_NAVAL.equals(id)
                && !DESTROYED_ANNIHILATOR.equals(id);
    }

    private static String hullId(ShipAPI ship) {
        return ship == null || ship.getHullSpec() == null
                ? "none" : ship.getHullSpec().getHullId();
    }

    private static String className(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
