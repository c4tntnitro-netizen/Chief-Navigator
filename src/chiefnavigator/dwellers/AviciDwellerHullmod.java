package chiefnavigator.dwellers;

import chiefnavigator.quest.BudaiMusic;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.dweller.DwellerHullmod;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipCreator;

/** Reuses native Dweller lifecycle callbacks without modifying its global registry. */
public final class AviciDwellerHullmod extends DwellerHullmod {
    private static final DwellerShipCreator BANSHEE = new ShroudedBansheeShipCreator();
    private static final DwellerShipCreator BRAIN = new ShroudedBrainShipCreator();
    private static final DwellerShipCreator HUNTER = new InsatiableHunterShipCreator();

    @Override
    protected DwellerShipCreator getShipCreator(String hullId) {
        if (AviciDwellerContent.BANSHEE_HULL.equals(hullId)) return BANSHEE;
        if (AviciDwellerContent.BRAIN_HULL.equals(hullId)) return BRAIN;
        if (AviciDwellerContent.BUDAI_HULL.equals(hullId)) return HUNTER;
        return null;
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        if (ship == null) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.isAlive() || ship.isHulk()
                || engine != null && !engine.isEntityInPlay(ship)) {
            ShroudedBrainAuraStats.clear(ship);
            AviciHunterDomain.clear(ship);
            return;
        }
        if (engine == null || engine.isPaused() || amount <= 0f) return;
        super.advanceInCombat(ship, amount);
        if (ship.getHullSpec() != null
                && AviciDwellerContent.isBudaiHull(ship.getHullSpec().getHullId())) {
            BudaiMusic.maintainBattleMusic(ship, true);
            AviciHunterDomain.advance(ship, amount);
        }
    }
}
