package chiefnavigator.quest;

import chiefnavigator.hullmods.GautamaReincarnation;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import java.util.List;

/** Keeps Gautama out of every perimeter invasion battle. */
public final class FinalLaborPerimeterBattleCreationPlugin
        extends BattleCreationPluginImpl {
    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        GautamaReincarnation.suppressForBattle(engine);
        Global.getLogger(FinalLaborPerimeterBattleCreationPlugin.class).info(
                "[ITHACA_FINAL_LABOR] Gautama reconstruction suppressed for "
                        + "perimeter invasion battle.");
        if (MenelausTrial.isFinalLaborActive()) {
            FinalLaborMusic.playPerimeterFight();
            engine.addPlugin(new BaseEveryFrameCombatPlugin() {
                @Override
                public void advance(
                        float amount, List<InputEventAPI> events) {
                    // Combat begins paused on the deployment screen; the
                    // real-time retry deliberately works before unpausing.
                    FinalLaborMusic.maintainMissionMusic();
                }
            });
        }
    }
}
