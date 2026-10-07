package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;

/** Retains the battery's sockets and salvo while upgrading its live ordnance. */
public final class IthacaMissileResearchEffect
        implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    private static final String REPLACED = "chief_navigator_atropos_replaced";
    private boolean initialized;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (initialized || engine.isPaused()) return;
        initialized = true;
        int upgrades = IthacaResearchUpgrades.getMask(weapon.getShip());
        boolean cycling = IthacaResearchUpgrades.hasUpgrade(
                upgrades, IthacaResearchUpgrades.AUTOLOADER_CORE);
        boolean atropos = IthacaResearchUpgrades.hasUpgrade(
                upgrades, IthacaResearchUpgrades.MUNITIONS_COMPILER);
        if (!cycling && !atropos) return;
        weapon.ensureClonedSpec();
        ProjectileWeaponSpecAPI spec = (ProjectileWeaponSpecAPI) weapon.getSpec();
        if (cycling) {
            float cycle = IthacaResearchUpgrades.MISSILE_CYCLE_MULTIPLIER;
            spec.setRefireDelay(spec.getRefireDelay() * cycle);
            weapon.setRefireDelay(spec.getRefireDelay());
            spec.setAmmoPerSecond(spec.getAmmoPerSecond() / cycle);
            weapon.getAmmoTracker().setAmmoPerSecond(spec.getAmmoPerSecond());
        }
        if (atropos) {
            spec.setWeaponName("Ithaca Atropos Battery");
            weapon.getDamage().setDamage(1000f);
        }
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile,
            WeaponAPI weapon, CombatEngineAPI engine) {
        if (!IthacaResearchUpgrades.hasUpgrade(
                weapon.getShip(), IthacaResearchUpgrades.MUNITIONS_COMPILER)) {
            return;
        }
        if (!"hammer_torp".equals(
                projectile.getProjectileSpecId())
                || projectile.getCustomData().containsKey(REPLACED)) return;
        projectile.setCustomData(REPLACED, Boolean.TRUE);
        // The replacement spec has no on-fire effect, so this cannot recurse.
        // Keep the firing weapon as source for attribution and ship modifiers.
        engine.spawnProjectile(weapon.getShip(), weapon,
                "chief_navigator_ithaca_atropos_battery",
                projectile.getLocation(), projectile.getFacing(),
                weapon.getShip().getVelocity());
        engine.removeEntity(projectile);
    }
}
