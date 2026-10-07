package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EmpArcEntityAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.List;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

/** Delays the naval shell's damage until its one-second impact distortion completes. */
public final class IthacaNavalShellEffect implements OnFireEffectPlugin, OnHitEffectPlugin {
    private static final String STORED_DAMAGE_KEY =
            "chief_navigator_ithaca_naval_shell_damage";
    private static final float IMPACT_DELAY_SECONDS = 1f;
    private static final float RIPPLE_END_SIZE = 1500f;
    private static final float RIPPLE_START_SIZE = 300f;
    private static final float RIPPLE_INTENSITY = 320f;
    private static final float SOUND_FULL_VOLUME_DISTANCE = 1200f;
    private static final float SOUND_MAX_DISTANCE = 15000f;
    private static final int LASH_ARC_COUNT = 3;
    private static final float LASH_START_SPREAD = 55f;
    private static final String LOAD_SOUND_ID = "chief_navigator_naval_gun_load";
    private static final String FIRE_SOUND_ID = "chief_navigator_naval_gun_fire";

    @Override
    public void onFire(
            DamagingProjectileAPI projectile,
            WeaponAPI weapon,
            CombatEngineAPI engine) {
        projectile.setCustomData(STORED_DAMAGE_KEY, projectile.getDamageAmount());
        projectile.setDamageAmount(0f);
        playDistanceScaledCannonSounds(projectile.getLocation());
    }
    private static void playDistanceScaledCannonSounds(Vector2f sourceLocation) {
        Vector2f listener = Global.getSoundPlayer().getListenerPos();
        if (listener == null || sourceLocation == null) {
            return;
        }
        float dx = sourceLocation.x - listener.x;
        float dy = sourceLocation.y - listener.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance >= SOUND_MAX_DISTANCE) {
            return;
        }
        float progress = Math.max(0f, Math.min(
                1f,
                (distance - SOUND_FULL_VOLUME_DISTANCE)
                        / (SOUND_MAX_DISTANCE - SOUND_FULL_VOLUME_DISTANCE)));
        float smoothProgress = progress * progress * (3f - 2f * progress);
        float volume = 1f - smoothProgress;
        Global.getSoundPlayer().playUISound(LOAD_SOUND_ID, 1f, volume * 0.42f);
        Global.getSoundPlayer().playUISound(FIRE_SOUND_ID, 1f, volume);
    }
    @Override
    public void onHit(
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            Vector2f point,
            boolean shieldHit,
            ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine) {
        if (target == null || point == null) {
            return;
        }

        float delayedDamage = 200000f;
        Object storedDamage = projectile.getCustomData().get(STORED_DAMAGE_KEY);
        if (storedDamage instanceof Number) {
            delayedDamage = ((Number) storedDamage).floatValue();
        }

        RippleDistortion ripple = new RippleDistortion(point, new Vector2f());
        ripple.setSize(RIPPLE_END_SIZE);
        ripple.setIntensity(RIPPLE_INTENSITY);
        ripple.setFrameRate(55f);
        ripple.fadeInSize(0.85f);
        ripple.fadeOutIntensity(IMPACT_DELAY_SECONDS);
        ripple.setSize(RIPPLE_START_SIZE);
        DistortionShader.addDistortion(ripple);

        engine.addPlugin(new DelayedImpact(
                engine,
                target,
                point,
                projectile.getSource(),
                shieldHit,
                delayedDamage,
                ripple));
    }

    private static final class DelayedImpact extends BaseEveryFrameCombatPlugin {
        private final CombatEngineAPI engine;
        private final CombatEntityAPI target;
        private final Vector2f localImpactOffset;
        private final float targetFacingAtImpact;
        private final ShipAPI source;
        private final boolean shieldHit;
        private final float damage;
        private final RippleDistortion ripple;
        private float elapsed;

        private DelayedImpact(
                CombatEngineAPI engine,
                CombatEntityAPI target,
                Vector2f impactPoint,
                ShipAPI source,
                boolean shieldHit,
                float damage,
                RippleDistortion ripple) {
            this.engine = engine;
            this.target = target;
            this.localImpactOffset = Vector2f.sub(
                    impactPoint, target.getLocation(), new Vector2f());
            this.targetFacingAtImpact = target.getFacing();
            this.source = source;
            this.shieldHit = shieldHit;
            this.damage = damage;
            this.ripple = ripple;
        }

        @Override
        public void advance(float amount, List<InputEventAPI> events) {
            if (engine.isPaused()) {
                return;
            }

            Vector2f currentImpactPoint = getCurrentImpactPoint();
            ripple.setLocation(currentImpactPoint);
            elapsed += amount;
            if (elapsed < IMPACT_DELAY_SECONDS) {
                return;
            }

            if (engine.isEntityInPlay(target) && !target.isExpired()) {
                // A shot that originally reached armor cannot be retroactively blocked by a raised shield.
                engine.applyDamage(
                        target,
                        currentImpactPoint,
                        damage,
                        DamageType.KINETIC,
                        0f,
                        false,
                        !shieldHit,
                        source);
                spawnThreatEnergyLash(
                        engine, source, target, currentImpactPoint);
            }
            engine.removePlugin(this);
        }

        private Vector2f getCurrentImpactPoint() {
            float angle = (float) Math.toRadians(target.getFacing() - targetFacingAtImpact);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            float rotatedX = localImpactOffset.x * cos - localImpactOffset.y * sin;
            float rotatedY = localImpactOffset.x * sin + localImpactOffset.y * cos;
            return new Vector2f(
                    target.getLocation().x + rotatedX,
                    target.getLocation().y + rotatedY);
        }
    }

    /**
     * Reuses the Overseer's Energy Lash presentation at the shell impact.
     * Damage and EMP remain zero: this is a branching impact VFX, not a
     * second attack or a trigger for lash-activated Threat systems.
     */
    private static void spawnThreatEnergyLash(
            CombatEngineAPI engine,
            ShipAPI source,
            CombatEntityAPI target,
            Vector2f impactPoint) {
        if (engine == null || source == null || target == null
                || impactPoint == null) {
            return;
        }

        EmpArcEntityAPI.EmpArcParams params =
                new EmpArcEntityAPI.EmpArcParams();
        params.segmentLengthMult = 8f;
        params.zigZagReductionFactor = 0.15f;
        params.fadeOutDist = 500f;
        params.minFadeOutMult = 2f;
        params.flickerRateMult = 0.4f;

        for (int i = 0; i < LASH_ARC_COUNT; i++) {
            float angle = 360f * i / LASH_ARC_COUNT + 30f;
            float radians = (float) Math.toRadians(angle);
            Vector2f start = new Vector2f(
                    impactPoint.x + (float) Math.cos(radians)
                            * LASH_START_SPREAD,
                    impactPoint.y + (float) Math.sin(radians)
                            * LASH_START_SPREAD);
            EmpArcEntityAPI arc = engine.spawnEmpArc(
                    source,
                    start,
                    source,
                    target,
                    DamageType.ENERGY,
                    0f,
                    0f,
                    100000f,
                    i == 0 ? "energy_lash_enemy_impact" : null,
                    60f,
                    VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR,
                    new java.awt.Color(255, 255, 255, 255),
                    params);
            arc.setCoreWidthOverride(40f);
            arc.setSingleFlickerMode(true);
        }
    }
}
