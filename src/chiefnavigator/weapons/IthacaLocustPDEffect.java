package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.AmmoTrackerAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.GuidedMissileAI;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.ProjectileWeaponSpecAPI;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

/** Activates Ithaca's research-locked Locust arrays and keeps them pure PD. */
public final class IthacaLocustPDEffect
        implements EveryFrameWeaponEffectPlugin, OnFireEffectPlugin {
    private static final int BASE_BURST_SIZE = 41;
    private static final int AUTOLOADER_BURST_SIZE = 81;
    private static final float ACQUISITION_RANGE = 1300f;
    private static final float CRUISE_SPEED = 520f;
    private static final float LAUNCH_SPEED = 120f;
    private static final float LAUNCH_PHASE = 0.18f;
    private static final float GUIDANCE_TRANSITION = 0.32f;
    private static final float TURN_RATE = 240f;
    private static final float MAX_LEAD_TIME = 1.25f;
    private static final float TARGET_SCAN_INTERVAL = 0.2f;
    private boolean initialized;
    private CombatEntityAPI launchTarget;
    private float targetScanRemaining;

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (engine.isPaused()) return;
        ShipAPI launcher = weapon.getShip();
        // This is a fixed fortress launcher, not a target-tracking turret. The
        // missiles turn after cold ejection; the launcher housing never does.
        // getArcFacing is relative to the hull; setCurrAngle is world-facing.
        float facing = (launcher.getFacing() + weapon.getArcFacing()) % 360f;
        weapon.setCurrAngle(facing < 0f ? facing + 360f : facing);
        boolean unlocked = IthacaResearchUpgrades.hasUpgrade(
                launcher, IthacaResearchUpgrades.INTERCEPT_MATRIX);
        weapon.setPD(true);
        weapon.setPDAlso(true);
        // Only impose the research lock. Never clear an external disable:
        // Ithaca's combat controller uses the same flag after reactor loss.
        if (!unlocked) weapon.setForceDisabled(true);

        targetScanRemaining -= amount;
        if (!unlocked || !launcher.isAlive() || launcher.isHulk()) {
            launchTarget = null;
        } else if (!isValidTarget(engine, launchTarget, launcher.getOwner(),
                weapon.getLocation(), weapon.getRange(), null)) {
            launchTarget = null;
            if (targetScanRemaining <= 0f) {
                launchTarget = findTarget(engine, launcher.getOwner(),
                        weapon.getLocation(), weapon.getRange(), null);
                targetScanRemaining = TARGET_SCAN_INTERVAL;
            }
        }
        if (launchTarget == null) {
            // This flag is intentionally never cleared here: it must not
            // override another controller's hold-fire or reactor-loss lock.
            weapon.setForceNoFireOneFrame(true);
            // Native isFiring also includes charging-down. Do not restart
            // an already-running cooldown on every targetless frame.
            if (weapon.isInBurst() || (weapon.isFiring()
                    && weapon.getCooldownRemaining() <= 0f)) weapon.stopFiring();
        }

        if (initialized) return;
        initialized = true;
        boolean autoloader = IthacaResearchUpgrades.hasUpgrade(
                weapon.getShip(), IthacaResearchUpgrades.AUTOLOADER_CORE);
        weapon.ensureClonedSpec();
        ProjectileWeaponSpecAPI spec =
                (ProjectileWeaponSpecAPI) weapon.getSpec();
        // Native burst length is cached when the weapon is constructed, so
        // author an 81-shot ceiling. Its interruptible burst ends naturally
        // when the base 41-round magazine empties. The cloned spec shows the
        // actual salvo; the Autoloader supplies the full 81-round magazine.
        spec.setBurstSize(autoloader ? AUTOLOADER_BURST_SIZE : BASE_BURST_SIZE);
        if (!autoloader) return;
        AmmoTrackerAPI ammo = weapon.getAmmoTracker();
        int oldMaximum = Math.max(1, ammo.getMaxAmmo());
        int oldAmmo = ammo.getAmmo();
        float cycle = IthacaResearchUpgrades.MISSILE_CYCLE_MULTIPLIER;
        float magazineScale = (float) AUTOLOADER_BURST_SIZE / BASE_BURST_SIZE;
        spec.setMaxAmmo(AUTOLOADER_BURST_SIZE);
        spec.setReloadSize(AUTOLOADER_BURST_SIZE);
        spec.setRefireDelay(spec.getRefireDelay() * cycle);
        weapon.setRefireDelay(spec.getRefireDelay());
        spec.setAmmoPerSecond(
                spec.getAmmoPerSecond() * magazineScale / cycle);

        int upgradedAmmo = Math.min(AUTOLOADER_BURST_SIZE, Math.round(
                oldAmmo
                        * (float) AUTOLOADER_BURST_SIZE / oldMaximum));
        ammo.setMaxAmmo(AUTOLOADER_BURST_SIZE);
        ammo.setReloadSize(AUTOLOADER_BURST_SIZE);
        ammo.setAmmoPerSecond(spec.getAmmoPerSecond());
        ammo.setAmmo(upgradedAmmo);
    }

    @Override
    public void onFire(DamagingProjectileAPI projectile,
            WeaponAPI weapon, CombatEngineAPI engine) {
        if (!weapon.getShip().isAlive() || weapon.getShip().isHulk()
                || !IthacaResearchUpgrades.hasUpgrade(
                weapon.getShip(), IthacaResearchUpgrades.INTERCEPT_MATRIX)) {
            engine.removeEntity(projectile);
            return;
        }
        if (projectile instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) projectile;
            // Native fire decisions precede the every-frame effect. Recheck
            // at the actual shot as well, including later cells of a burst.
            CombatEntityAPI target = findTarget(engine,
                    weapon.getShip().getOwner(), weapon.getLocation(),
                    weapon.getRange(), missile);
            if (target == null) {
                weapon.setForceNoFireOneFrame(true);
                if (weapon.isInBurst() || (weapon.isFiring()
                        && weapon.getCooldownRemaining() <= 0f)) weapon.stopFiring();
                engine.removeEntity(projectile);
                return;
            }
            missile.setMissileAI(new InterceptorAI(engine, missile, target));
        }
    }

    private static CombatEntityAPI findTarget(CombatEngineAPI engine,
            int owner, Vector2f origin, float range, CombatEntityAPI excluded) {
        CombatEntityAPI best = null;
        float bestScore = Float.MAX_VALUE;
        List<MissileAPI> missiles = engine.getMissiles();
        if (missiles != null) {
            for (MissileAPI candidate : missiles) {
                if (!isValidTarget(engine, candidate, owner, origin, range, excluded)) continue;
                float score = distanceSquared(origin, candidate)
                        * (0.75f + (float) Math.random() * 0.5f);
                if (score < bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        if (best != null) return best;
        List<ShipAPI> ships = engine.getShips();
        if (ships != null) {
            for (ShipAPI candidate : ships) {
                if (!isValidTarget(engine, candidate, owner, origin, range, excluded)) continue;
                float score = distanceSquared(origin, candidate)
                        * (0.75f + (float) Math.random() * 0.5f);
                if (score < bestScore) {
                    best = candidate;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    private static boolean isValidTarget(CombatEngineAPI engine,
            CombatEntityAPI candidate, int owner, Vector2f origin,
            float range, CombatEntityAPI excluded) {
        if (candidate == null || candidate == excluded || origin == null
                || range <= 0f || candidate.getOwner() == 100
                || candidate.getOwner() == owner || !engine.isEntityInPlay(candidate)
                || distanceSquared(origin, candidate) > range * range) return false;
        if (candidate instanceof MissileAPI) {
            MissileAPI missile = (MissileAPI) candidate;
            return !missile.isFading() && !missile.isFizzling();
        }
        if (candidate instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) candidate;
            return ship.isAlive() && !ship.isHulk() && !ship.isPhased()
                    && (ship.isFighter() || ship.isDrone());
        }
        return false;
    }

    private static float distanceSquared(Vector2f origin, CombatEntityAPI candidate) {
        float dx = candidate.getLocation().x - origin.x;
        float dy = candidate.getLocation().y - origin.y;
        return dx * dx + dy * dy;
    }

    /**
     * Locust-derived guidance with a deliberately closed target set. It can
     * acquire hostile missiles, fighters, and drones, but never an ordinary
     * ship. Randomized scoring distributes a salvo instead of dogpiling the
     * geometrically nearest incoming missile.
     */
    private static final class InterceptorAI
            implements MissileAIPlugin, GuidedMissileAI {
        private final CombatEngineAPI engine;
        private final MissileAPI missile;
        private final float launchFacing;
        private CombatEntityAPI target;
        private float elapsed;
        private boolean retired;

        private InterceptorAI(CombatEngineAPI engine, MissileAPI missile,
                CombatEntityAPI initialTarget) {
            this.engine = engine;
            this.missile = missile;
            launchFacing = missile.getFacing();
            target = initialTarget;
            setVelocity(launchFacing, LAUNCH_SPEED);
        }

        @Override
        public void advance(float amount) {
            if (retired || engine.isPaused() || missile.isFading() || missile.isFizzling()) {
                return;
            }
            elapsed += amount;
            if (elapsed < LAUNCH_PHASE) {
                // Match the Locust's conspicuous cold-ejection fan: leave
                // each alternating tube on its authored off-axis course
                // before the guidance package turns toward an interceptor.
                missile.setFacing(launchFacing);
                missile.setAngularVelocity(0f);
                setVelocity(launchFacing, LAUNCH_SPEED);
                return;
            }
            if (!isValidTarget(target)) {
                target = findTarget();
            }
            if (target == null) {
                // Nothing remains to intercept. Do not accelerate a targetless
                // missile along its ejection heading or scan forever for prey.
                retired = true;
                missile.flameOut();
                return;
            }
            float transition = Math.min(
                    1f, (elapsed - LAUNCH_PHASE) / GUIDANCE_TRANSITION);
            float smooth = transition * transition * (3f - 2f * transition);
            if (target != null) {
                float dx = target.getLocation().x - missile.getLocation().x;
                float dy = target.getLocation().y - missile.getLocation().y;
                float distance = (float) Math.sqrt(dx * dx + dy * dy);
                float lead = Math.min(MAX_LEAD_TIME, distance / CRUISE_SPEED);
                float aimX = target.getLocation().x
                        + target.getVelocity().x * lead;
                float aimY = target.getLocation().y
                        + target.getVelocity().y * lead;
                float desiredFacing = (float) Math.toDegrees(Math.atan2(
                        aimY - missile.getLocation().y,
                        aimX - missile.getLocation().x));
                float turn = shortestRotation(
                        missile.getFacing(), desiredFacing);
                float maxTurn = TURN_RATE * smooth * amount;
                turn = Math.max(-maxTurn, Math.min(maxTurn, turn));
                missile.setFacing(missile.getFacing() + turn);
                missile.setAngularVelocity(0f);
            }
            setVelocity(missile.getFacing(), lerp(
                    LAUNCH_SPEED, CRUISE_SPEED, smooth));
        }

        private void setVelocity(float facing, float speed) {
            float radians = (float) Math.toRadians(facing);
            missile.getVelocity().set(
                    (float) Math.cos(radians) * speed,
                    (float) Math.sin(radians) * speed);
        }

        private static float lerp(float from, float to, float progress) {
            return from + (to - from) * progress;
        }

        private CombatEntityAPI findTarget() {
            return IthacaLocustPDEffect.findTarget(engine, missile.getOwner(),
                    missile.getLocation(), ACQUISITION_RANGE, missile);
        }

        private boolean isValidTarget(CombatEntityAPI candidate) {
            return IthacaLocustPDEffect.isValidTarget(engine, candidate,
                    missile.getOwner(), missile.getLocation(), ACQUISITION_RANGE, missile);
        }

        private static float shortestRotation(float from, float to) {
            float result = (to - from) % 360f;
            if (result > 180f) result -= 360f;
            if (result < -180f) result += 360f;
            return result;
        }

        @Override
        public CombatEntityAPI getTarget() {
            return target;
        }

        @Override
        public void setTarget(CombatEntityAPI target) {
            this.target = isValidTarget(target) ? target : null;
        }
    }
}
