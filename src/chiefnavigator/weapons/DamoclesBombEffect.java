package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.awt.Color;
import java.util.List;

/** Marks every fourth Damocles bomb as a blinking, Reaper-strength danger round. */
public final class DamoclesBombEffect implements OnFireEffectPlugin {
    private static final String SEQUENCE_KEY = "chief_navigator_damocles_bomb_sequence";
    private static final int RED_BOMB_INTERVAL = 4;
    private static final float REAPER_DAMAGE_MULTIPLIER = 10f;
    private static final Color RED = new Color(255, 35, 25, 255);
    private static final Color PALE_RED = new Color(255, 150, 135, 255);

    @Override
    public void onFire(
            DamagingProjectileAPI projectile,
            WeaponAPI weapon,
            CombatEngineAPI engine) {
        if (!(projectile instanceof MissileAPI)) {
            return;
        }

        int sequence = 0;
        Object storedSequence = engine.getCustomData().get(SEQUENCE_KEY);
        if (storedSequence instanceof Number) {
            sequence = ((Number) storedSequence).intValue();
        }
        engine.getCustomData().put(SEQUENCE_KEY, sequence + 1);

        MissileAPI bomb = (MissileAPI) projectile;
        if ((sequence + 1) % RED_BOMB_INTERVAL != 0) {
            return;
        }

        bomb.setDamageAmount(bomb.getDamageAmount() * REAPER_DAMAGE_MULTIPLIER);
        bomb.setDestroyedExplosionColorOverride(RED);
        bomb.setRenderGlowAbove(true);
        bomb.setNoGlowTime(0f);
        engine.addPlugin(new RedBombBlinkPlugin(engine, bomb));
    }

    private static final class RedBombBlinkPlugin extends BaseEveryFrameCombatPlugin {
        private static final float BLINK_PERIOD = 0.36f;
        private final CombatEngineAPI engine;
        private final MissileAPI bomb;
        private float elapsed;

        private RedBombBlinkPlugin(CombatEngineAPI engine, MissileAPI bomb) {
            this.engine = engine;
            this.bomb = bomb;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused()) {
                return;
            }
            if (!engine.isMissileAlive(bomb) || !engine.isEntityInPlay(bomb)) {
                engine.removePlugin(this);
                return;
            }

            elapsed += amount;
            float pulse = 0.5f + 0.5f * (float) Math.sin(
                    elapsed * Math.PI * 2f / BLINK_PERIOD);
            int greenBlue = Math.round(255f - 205f * pulse);
            bomb.getSpriteAPI().setColor(new Color(255, greenBlue, greenBlue, 255));
            bomb.setGlowRadius(18f + 42f * pulse);
            bomb.setShineBrightness(0.25f + 0.75f * pulse);
            bomb.setJitter(this, PALE_RED, pulse, 4, 6f, 18f);
        }
    }
}
