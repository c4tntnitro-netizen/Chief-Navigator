package chiefnavigator.hullmods;

import chiefnavigator.ai.BoardingPodFighterAI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

/** Installs the defensive-orbit/boarding AI on Spartan boarding craft. */
public final class BoardingPodFighterHullmod extends BaseHullMod {
    private static final String AI_INSTALLED_KEY =
            "chief_navigator_boarding_pod_ai_installed";

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null || !ship.isAlive()) return;
        if (Boolean.TRUE.equals(ship.getCustomData().get(AI_INSTALLED_KEY))) return;

        ship.setShipAI(new BoardingPodFighterAI(ship));
        ship.setCustomData(AI_INSTALLED_KEY, Boolean.TRUE);
    }
}
