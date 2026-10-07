package chiefnavigator.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionClass;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.combat.threat.VoidblasterEffect;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

/** Stock Voidblaster behavior with mortal, mount-local firing feedback. */
public final class StarvingSkullblasterEffect implements
        EveryFrameWeaponEffectPlugin,
        OnFireEffectPlugin,
        OnHitEffectPlugin,
        DamageDealtModifier {
    private static final int SHOTS_PER_FEEDBACK = 2;
    private static final float HAMMER_DAMAGE = 1500f;
    private static final float EXPLOSION_RADIUS = 100f;
    private static final float EXPLOSION_CORE_RADIUS = 65f;
    private static final Color EXPLOSION_COLOR = new Color(255, 125, 80, 255);
    private static final Color PARTICLE_COLOR = new Color(255, 165, 135, 255);

    private final VoidblasterEffect delegate = new VoidblasterEffect();
    private final Map<WeaponAPI, Integer> shotsSinceFeedback =
            new WeakHashMap<WeaponAPI, Integer>();

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        delegate.advance(amount, engine, weapon);
    }

    private static void detonateFeedback(
            CombatEngineAPI engine,
            WeaponAPI weapon,
            ShipAPI ship) {
        Vector2f point = new Vector2f(weapon.getLocation());

        // Reproduce the Perdition's Hammer detonation visually without adding
        // a second area-damage event; the guaranteed localized self-hit below
        // is the actual feedback damage.
        DamagingExplosionSpec visual = new DamagingExplosionSpec(
                0.1f,
                EXPLOSION_RADIUS,
                EXPLOSION_CORE_RADIUS,
                0f,
                0f,
                CollisionClass.NONE,
                CollisionClass.NONE,
                5f,
                3f,
                1f,
                150,
                PARTICLE_COLOR,
                EXPLOSION_COLOR);
        visual.setDamageType(DamageType.HIGH_EXPLOSIVE);
        visual.setUseDetailedExplosion(true);
        engine.spawnDamagingExplosion(visual, ship, point);
        engine.spawnExplosion(
                point,
                new Vector2f(ship.getVelocity()),
                EXPLOSION_COLOR,
                230f,
                0.5f);
        Global.getSoundPlayer().playSound(
                "explosion_missile",
                1f,
                1f,
                point,
                ship.getVelocity());

        engine.applyDamage(
                ship,
                point,
                HAMMER_DAMAGE,
                DamageType.HIGH_EXPLOSIVE,
                0f,
                false,
                false,
                weapon,
                true);
    }

    @Override
    public void onFire(
            DamagingProjectileAPI projectile,
            WeaponAPI weapon,
            CombatEngineAPI engine) {
        delegate.onFire(projectile, weapon, engine);
        ShipAPI ship = weapon.getShip();
        if (ship == null || !ship.isAlive()) return;

        int shots = shotsSinceFeedback.containsKey(weapon)
                ? shotsSinceFeedback.get(weapon) + 1
                : 1;
        if (shots >= SHOTS_PER_FEEDBACK) {
            shots = 0;
            detonateFeedback(engine, weapon, ship);
        }
        shotsSinceFeedback.put(weapon, shots);
    }

    @Override
    public void onHit(
            DamagingProjectileAPI projectile,
            CombatEntityAPI target,
            Vector2f point,
            boolean shieldHit,
            ApplyDamageResultAPI damageResult,
            CombatEngineAPI engine) {
        delegate.onHit(projectile, target, point, shieldHit, damageResult, engine);
    }

    @Override
    public String modifyDamageDealt(
            Object param,
            CombatEntityAPI target,
            DamageAPI damage,
            Vector2f point,
            boolean shieldHit) {
        return delegate.modifyDamageDealt(param, target, damage, point, shieldHit);
    }
}
