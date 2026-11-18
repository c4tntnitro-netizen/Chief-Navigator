package chiefnavigator.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.loading.BeamWeaponSpecAPI;
import java.util.Random;
import org.lwjgl.util.vector.Vector2f;

/** Small native laser bolts sputter out before the committed naval beam. */
public final class OnslaughtChargePulseEffect {
    private static final String PULSE_ID = "chief_navigator_onslaught_charge_pulse";
    private static final float DAMAGE_FRACTION = 0.1f;
    // At the installed 60 FPS and 6000 speed, native moving rays floor the old
    // 48-unit bolt at 150 visible units. Double that observed length, then vary +/-50%.
    private static final int[] LENGTHS = {150, 188, 225, 263, 300, 338, 375, 413, 450};
    // Fractions of native windup: isolated flashes, pairs, then a quick final stutter.
    private static final float[] CADENCE = {
        .08f, .31f, .46f, .50f, .65f, .69f, .81f, .84f, .88f, .92f, .95f
    };

    private int nextPulse;
    private float previousCharge;
    private boolean cancelled;
    private final Random random = new Random();
    private float[] cadence;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon,
            boolean mainBeamVisible) {
        if (engine.isPaused() || amount <= 0f) return;
        if (!OnslaughtIthDischargeVfx.handles(weapon)) return;
        ShipAPI source = weapon.getShip();
        float charge = weapon.getChargeLevel();
        if (mainBeamVisible || weapon.getCooldownRemaining() > 0f
                || charge <= .01f || charge >= 1f) {
            nextPulse = 0;
            previousCharge = 0f;
            cancelled = false;
            cadence = null;
            return;
        }
        if (!source.isAlive() || source.isHulk() || source.isPhased()
                || weapon.isDisabled() || weapon.isForceDisabled()) cancelled = true;
        // Charge can fall on an interrupted windup; never replay pulses on its way down.
        if (charge < previousCharge - .0001f) cancelled = true;
        previousCharge = charge;
        if (cancelled || !(weapon.getSpec() instanceof BeamWeaponSpecAPI)) return;
        if (cadence == null) randomizeCadence();
        Vector2f muzzle = weapon.getFirePoint(0);
        if (muzzle == null) return;
        while (nextPulse < cadence.length && charge >= cadence[nextPulse]) {
            nextPulse++;
            // Each immutable length has its own hidden weapon registration so the
            // native spreadsheet loader initializes its projectile damage first.
            int length = LENGTHS[random.nextInt(LENGTHS.length)];
            String pulseId = PULSE_ID + (length == 300 ? "" : "_" + length);
            CombatEntityAPI spawned = engine.spawnProjectile(source, weapon, pulseId,
                    new Vector2f(muzzle), weapon.getCurrAngle(),
                    new Vector2f(source.getVelocity()));
            if (spawned instanceof DamagingProjectileAPI) {
                // Use firing DPS, not the derived cycle average (which includes cooldown).
                // Keep the native projectile's source, weapon, stats and damage modifiers.
                ((DamagingProjectileAPI) spawned).setDamageAmount(
                        ((BeamWeaponSpecAPI) weapon.getSpec()).getDamagePerSecond()
                                * DAMAGE_FRACTION);
            }
        }
    }

    private void randomizeCadence() {
        cadence = new float[CADENCE.length];
        float offset = (random.nextFloat() - .5f) * .05f;
        for (int i = 0; i < CADENCE.length; i++) {
            float before = CADENCE[i] - (i == 0 ? 0f : CADENCE[i - 1]);
            float after = (i == CADENCE.length - 1 ? 1f : CADENCE[i + 1]) - CADENCE[i];
            // Neighbour-bounded jitter preserves the accelerating pattern and pulse order.
            // Each gun owns its RNG and rolls again each shot.
            float jitter = (random.nextFloat() * 2f - 1f) * Math.min(before, after) * .45f;
            cadence[i] = Math.min(.96f, CADENCE[i] + offset + jitter);
        }
    }
}
