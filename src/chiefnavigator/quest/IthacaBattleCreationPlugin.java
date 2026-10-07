package chiefnavigator.quest;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Compact assault map with FOB Ithaca implemented as one modular station. */
public final class IthacaBattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        api.initMap(-6000f, 6000f, -4000f, 4000f);
        context.setStandoffRange(3500f);
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
    }

    @Override
    public void afterDefinitionLoad(final CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new IthacaWallPlugin(engine));
    }

    private static final class IthacaWallPlugin extends BaseEveryFrameCombatPlugin {
        private static final String STAT_ID = "chief_navigator_ithaca_emplacement";
        private final CombatEngineAPI engine;
        private final Map<ShipAPI, Vector2f> anchors = new HashMap<ShipAPI, Vector2f>();

        private IthacaWallPlugin(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            for (ShipAPI ship : engine.getShips()) {
                if (ship.isAlive()
                        && TroyArrivalScript.WALL_HULL_ID.equals(
                                ship.getHullSpec().getHullId())
                        && !anchors.containsKey(ship)) {
                    anchors.put(ship, new Vector2f(0f, 3500f));
                    immobilize(ship);
                }
            }

            for (Map.Entry<ShipAPI, Vector2f> entry : anchors.entrySet()) {
                ShipAPI ship = entry.getKey();
                if (!ship.isAlive()) continue;
                ship.getLocation().set(entry.getValue());
                ship.getVelocity().set(0f, 0f);
                ship.setFacing(0f);
                ship.setAngularVelocity(0f);
            }
        }

        private void immobilize(ShipAPI ship) {
            ship.getMutableStats().getMaxSpeed().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getAcceleration().modifyMult(STAT_ID, 0f);
            ship.getMutableStats().getDeceleration().modifyMult(STAT_ID, 0f);
        }
    }
}
