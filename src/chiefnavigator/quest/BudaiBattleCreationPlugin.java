package chiefnavigator.quest;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;

/** Vanilla battle generation with Budai's authored boss theme layered in. */
public final class BudaiBattleCreationPlugin
        extends BattleCreationPluginImpl {
    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        BudaiMusic.playBattle();
        engine.addPlugin(new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                // Deployment starts paused and may discard an early streaming
                // request, so retain the same delayed verification used by
                // the final-Labor music handoff.
                ShipAPI budai = null;
                for (ShipAPI ship : engine.getShips()) {
                    if (ship != null && ship.isAlive() && !ship.isHulk()
                            && ship.getHullSpec() != null
                            && "chief_navigator_charybdis".equals(
                                    ship.getHullSpec().getHullId())
                            && (budai == null || ship.getHullLevel() < budai.getHullLevel())) {
                        budai = ship;
                    }
                }
                BudaiMusic.maintainBattleMusic(
                        budai, !engine.isPaused() && amount > 0f);
            }
        });
    }
}
