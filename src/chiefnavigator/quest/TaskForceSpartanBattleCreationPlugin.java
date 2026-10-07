package chiefnavigator.quest;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.BattleCreationContext;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BattleCreationPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/** XIV line battle followed by a one-minute flanking reinforcement wave. */
public final class TaskForceSpartanBattleCreationPlugin extends BattleCreationPluginImpl {
    @Override
    public void initBattle(BattleCreationContext context, MissionDefinitionAPI api) {
        super.initBattle(context, api);
        context.enemyDeployAll = true;
        context.aiRetreatAllowed = false;
        context.fightToTheLast = true;
    }

    @Override
    public void afterDefinitionLoad(CombatEngineAPI engine) {
        super.afterDefinitionLoad(engine);
        engine.addPlugin(new SpartanReinforcementPlugin(engine));
    }

    private static final class SpartanReinforcementPlugin extends BaseEveryFrameCombatPlugin {
        private static final float REINFORCEMENT_DELAY = 60f;
        private static final String[] REINFORCEMENT_VARIANTS = {
                "eagle_xiv_Elite",
                "lasher_Assault",
                "lasher_Assault"
        };
        private static final float[] LATERAL_OFFSETS = {0f, -425f, 425f};
        private static final float[] LEAD_OFFSETS = {400f, 0f, 0f};
        private static final float BORDER_INSET = 450f;
        private static final float SIDE_MARGIN = 900f;

        private final CombatEngineAPI engine;
        private float elapsed;
        private boolean staged;
        private boolean arrived;

        private SpartanReinforcementPlugin(CombatEngineAPI engine) {
            this.engine = engine;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (!staged) {
                // Keep the battle live even if the XIV line is destroyed
                // before the delayed ambush arrives.
                engine.setCombatNotOverForAtLeast(REINFORCEMENT_DELAY + 3f);
                staged = true;
            }
            if (arrived || engine.isPaused()) return;

            elapsed += amount;
            int seconds = Math.max(0, (int) Math.ceil(REINFORCEMENT_DELAY - elapsed));
            engine.maintainStatusForPlayerShip(
                    this,
                    "graphics/icons/intel/fleet_log.png",
                    "INCOMING REINFORCEMENTS",
                    "Approaching from the southern border in " + seconds + " seconds",
                    true);
            if (elapsed >= REINFORCEMENT_DELAY) {
                deployFromBottomBorder();
                arrived = true;
            }
        }

        private void deployFromBottomBorder() {
            ShipAPI reference = findPlayerReference();
            float halfWidth = engine.getMapWidth() * 0.5f;
            float halfHeight = engine.getMapHeight() * 0.5f;
            float baseX = reference == null ? 0f : reference.getLocation().x;
            baseX = clamp(baseX, -halfWidth + SIDE_MARGIN, halfWidth - SIDE_MARGIN);
            float baseY = -halfHeight + BORDER_INSET;
            float targetFacing = 90f;

            CombatFleetManagerAPI manager = engine.getFleetManager(1);
            for (int i = 0; i < REINFORCEMENT_VARIANTS.length; i++) {
                float lateral = LATERAL_OFFSETS[Math.min(i, LATERAL_OFFSETS.length - 1)];
                float lead = LEAD_OFFSETS[Math.min(i, LEAD_OFFSETS.length - 1)];
                Vector2f location = new Vector2f(
                        clamp(baseX + lateral,
                                -halfWidth + SIDE_MARGIN,
                                halfWidth - SIDE_MARGIN),
                        baseY + lead);
                ShipAPI ship = manager.spawnShipOrWing(
                        REINFORCEMENT_VARIANTS[i], location, targetFacing, 0f, null);
                if (ship == null) continue;
                ship.setAngularVelocity(0f);
                ship.getVelocity().set(0f, 0f);
            }
        }

        private float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }

        private ShipAPI findPlayerReference() {
            ShipAPI player = engine.getPlayerShip();
            if (isValidReference(player)) return player;
            for (ShipAPI candidate : engine.getShips()) {
                if (candidate.getOwner() == 0 && isValidReference(candidate)) {
                    return candidate;
                }
            }
            return null;
        }

        private boolean isValidReference(ShipAPI ship) {
            return ship != null && ship.isAlive() && !ship.isHulk()
                    && engine.isEntityInPlay(ship);
        }

    }
}
