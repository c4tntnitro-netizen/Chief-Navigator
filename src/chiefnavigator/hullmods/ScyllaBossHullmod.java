package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;

/** Owns Gautama's live music callback and death notice; its screen is carrier-driven. */
public final class ScyllaBossHullmod extends BaseHullMod {
    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || engine == null) return;

        if (ship.isHulk() || !ship.isAlive()) {
            GautamaReincarnation.noteGautamaDeath(engine, ship);
        } else {
            GautamaReincarnation.maintainBattleMusic(engine, ship);
        }
    }
}
