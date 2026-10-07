package chiefnavigator.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BoundsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.dweller.BaseDwellerShipPart;
import com.fs.starfarer.api.impl.combat.dweller.DwellerCombatPlugin;
import com.fs.starfarer.api.impl.combat.dweller.DwellerHullmod;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipCreator;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShipPart;
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud;
import com.fs.starfarer.api.impl.combat.dweller.ShroudedMawShipCreator;
import com.fs.starfarer.api.loading.WeaponSlotAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import chiefnavigator.quest.BudaiMusic;
import chiefnavigator.systems.BudaiConvulsiveLungeSystem;
import java.awt.Color;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

/** Budai's stock Maw body, maintained distortion, and hazard-field rules. */
public final class CharybdisDistortion extends DwellerHullmod {
    private static final String DISTORTION_KEY =
            "chief_navigator_charybdis_persistent_distortion";
    private static final String RADIUS_KEY =
            "chief_navigator_charybdis_distortion_radius";
    private static final String LIGHTNING_TIMER_KEY =
            "chief_navigator_charybdis_lightning_timer";
    private static final String SIZE_APPLIED_KEY =
            "chief_navigator_charybdis_size_applied";
    private static final String ENRAGED_KEY =
            "chief_navigator_charybdis_enraged";
    public static final String ENRAGE_SEQUENCE_KEY =
            "chief_navigator_charybdis_enrage_sequence";
    private static final String SLOW_ID_PREFIX =
            "chief_navigator_charybdis_slow_";
    private static final String DAMAGE_REDUCTION_ID =
            "chief_navigator_charybdis_outside_shot";

    /** GraphicsLib's ripple size is a radius; the rendered sprite is twice this value. */
    public static final float BASE_RADIUS = 1000f;
    private static final float DISTORTION_INTENSITY = 320f;
    private static final float DISTORTION_LIFETIME = 1000000f;
    private static final float DISTORTION_FRAME = 30f;

    private static final float MOVEMENT_MULT = 0.5f;
    private static final float OUTSIDE_DAMAGE_MULT = 0.7f;
    private static final float EATEN_POPUP_COOLDOWN = 0.2f;
    private static final float EATEN_POPUP_SIZE = 28f;
    private static final float HULL_MULT = 2f;
    private static final float ARMOR_MULT = 0.5f;
    private static final float BODY_SCALE = 1.5f;
    private static final float BLUE_FROM_RED = 0.36f;
    private static final float ENRAGE_HULL_LEVEL = 0.5f;
    private static final float ENRAGED_RADIUS_MULT = 4f;
    private static final float ENRAGED_RIFT_REFIRE_MULT = 0.5f;
    private static final int ENRAGE_LUNGE_COUNT = 4;
    private static final float ENRAGE_LUNGE_MIN_DISTANCE = 700f;
    private static final float ENRAGE_LUNGE_MAX_DISTANCE = 2400f;
    private static final float ENRAGE_LUNGE_REST = 0.12f;
    private static final float ENRAGE_LUNGE_RETRY = 0.35f;
    // Four full Maw cycles plus Budai's twelve-second cooldowns take at least
    // 58 seconds. Leave room for an already-running leap and external cooldowns without
    // finishing the threshold buffs before its fourth actual completion.
    private static final float ENRAGE_SEQUENCE_TIMEOUT = 90f;
    private static final String BUDAI_LUNGE_SYSTEM_ID =
            "chief_navigator_budai_convulsive_lunge";
    private static final String BUDAI_RIFT_ID =
            "chief_navigator_charybdis_assaying_rift";
    private static final String STOCK_RIFT_ID = "assaying_rift";
    private static final String ENRAGE_SOUND_ID =
            "chief_navigator_budai_enrage_screech";

    private static final float LIGHTNING_MIN_INTERVAL = 0.9f;
    private static final float LIGHTNING_MAX_INTERVAL = 1.1f;
    private static final float LIGHTNING_DAMAGE = 125f;
    private static final float LIGHTNING_EMP = 200f;
    private static final float FIGHTER_LIGHTNING_DAMAGE = 35f;
    private static final float FIGHTER_LIGHTNING_EMP = 50f;
    private static final Color LIGHTNING_FRINGE = new Color(255, 0, 107, 255);
    private static final Color LIGHTNING_CORE = new Color(255, 190, 235, 255);
    private static final Color EATEN_POPUP_COLOR = new Color(255, 190, 235, 255);

    private static final DwellerShipCreator CHARYBDIS_CREATOR =
            new CharybdisShipCreator();
    private static final Set<String> SCALED_HULL_SPECS = new HashSet<>();

    @Override
    protected DwellerShipCreator getShipCreator(String baseHullId) {
        if ("shrouded_maw".equals(baseHullId)) {
            return CHARYBDIS_CREATOR;
        }
        return super.getShipCreator(baseHullId);
    }

    @Override
    public void applyEffectsBeforeShipCreation(
            ShipAPI.HullSize hullSize, MutableShipStatsAPI stats, String id) {
        super.applyEffectsBeforeShipCreation(hullSize, stats, id);
        if (stats == null) {
            return;
        }

        stats.getHullBonus().modifyMult(id, HULL_MULT);
        stats.getArmorBonus().modifyMult(id, ARMOR_MULT);
        scaleWeaponSlotsOnce(stats.getVariant() == null
                ? null : stats.getVariant().getHullSpec());
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        super.applyEffectsAfterShipCreation(ship, id);
        if (ship == null) {
            return;
        }

        applyPhysicalScale(ship);
        if (!ship.hasListenerOfClass(OutsideBubbleDamageModifier.class)) {
            ship.addListener(new OutsideBubbleDamageModifier(ship));
        }
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        super.advanceInCombat(ship, amount);
        CombatEngineAPI engine = Global.getCombatEngine();
        if (ship == null || engine == null) {
            return;
        }

        if (ship.isHulk() || !engine.isEntityInPlay(ship)) {
            removeDistortion(ship);
            clearSlowdown(engine, ship);
            return;
        }

        if (!engine.isPaused() && amount > 0f) {
            // Observe the damaged hull before regeneration can cross back above half.
            if (BudaiRegeneration.isBudai(ship) && ship.isAlive()) {
                BudaiMusic.maintainBattleMusic(ship, true);
            }
            if (ship.getHullLevel() <= ENRAGE_HULL_LEVEL
                    && !ship.getCustomData().containsKey(ENRAGED_KEY)) {
                beginEnrage(ship);
            }
            preventRetreat(ship);
            advanceEnrageSequence(engine, ship, amount);
            BudaiRegeneration.advance(engine, ship, amount);
        }

        float radius = getCurrentRadius(ship);
        applySlowdown(engine, ship, radius);
        advanceLightning(engine, ship, radius, amount);

        Object existing = ship.getCustomData().get(DISTORTION_KEY);
        RippleDistortion ripple;
        if (existing instanceof RippleDistortion) {
            ripple = (RippleDistortion) existing;
        } else {
            ripple = new RippleDistortion(
                    new Vector2f(ship.getLocation()), new Vector2f());
            ripple.setLifetime(DISTORTION_LIFETIME);
            DistortionShader.addDistortion(ripple);
            ship.setCustomData(DISTORTION_KEY, ripple);
        }
        maintain(ripple, ship, radius);
    }

    private static void preventRetreat(ShipAPI ship) {
        if (ship.isRetreating()) {
            ship.setRetreating(false, false);
        }
        ShipwideAIFlags flags = ship.getAIFlags();
        if (flags != null) {
            flags.unsetFlag(ShipwideAIFlags.AIFlags.BACKING_OFF);
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF, 2f);
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_BACK_OFF_EVEN_WHILE_VENTING, 2f);
        }
    }

    private static void beginEnrage(ShipAPI ship) {
        // Commit the flag before producing any side effects so this remains a
        // one-shot transition even if another combat plugin inspects Budai in
        // the same frame.
        ship.setCustomData(ENRAGED_KEY, Boolean.TRUE);
        ship.setCustomData(ENRAGE_SEQUENCE_KEY, new EnrageSequence(ship));

        Global.getSoundPlayer().playSound(
                ENRAGE_SOUND_ID,
                1f,
                1f,
                ship.getLocation(),
                ship.getVelocity());
    }

    private static void advanceEnrageSequence(
            CombatEngineAPI engine, ShipAPI ship, float amount) {
        Object value = ship.getCustomData().get(ENRAGE_SEQUENCE_KEY);
        if (!(value instanceof EnrageSequence)) {
            return;
        }

        EnrageSequence sequence = (EnrageSequence) value;
        ShipSystemAPI system = ship.getSystem();
        sequence.elapsed += amount;
        if (system == null || !BUDAI_LUNGE_SYSTEM_ID.equals(system.getId())
                || sequence.elapsed >= ENRAGE_SEQUENCE_TIMEOUT) {
            finishEnrage(engine, ship);
            return;
        }

        ShipSystemAPI.SystemState state = system.getState();
        boolean lunging = state == ShipSystemAPI.SystemState.IN
                || state == ShipSystemAPI.SystemState.ACTIVE
                || state == ShipSystemAPI.SystemState.OUT;

        if (sequence.commanded) {
            if (lunging) {
                sequence.enteredLunge = true;
                return;
            }
            if (sequence.enteredLunge) {
                sequence.completedLunges++;
                sequence.commanded = false;
                sequence.enteredLunge = false;
                sequence.restRemaining = ENRAGE_LUNGE_REST;
                ship.removeCustomData(BudaiConvulsiveLungeSystem.TARGET_KEY);
                if (sequence.completedLunges >= ENRAGE_LUNGE_COUNT) {
                    finishEnrage(engine, ship);
                }
                return;
            }

            sequence.retryRemaining -= amount;
            if (sequence.retryRemaining <= 0f) {
                activateForcedLunge(ship, system, sequence);
            }
            return;
        }

        // Do not interrupt a normal AI-directed lunge that happened to be in
        // progress when Budai crossed the threshold.
        if (lunging) {
            return;
        }
        if (sequence.restRemaining > 0f) {
            sequence.restRemaining -= amount;
            return;
        }

        activateForcedLunge(ship, system, sequence);
    }

    private static void activateForcedLunge(
            ShipAPI ship, ShipSystemAPI system, EnrageSequence sequence) {
        // The threshold sequence chooses destinations, not cooldowns. Every
        // leap, including the first and fourth, keeps normal system readiness.
        if (system.getCooldownRemaining() > 0f || !system.canBeActivated()) return;
        float angle = sequence.random.nextFloat() * 360f;
        float distance = ENRAGE_LUNGE_MIN_DISTANCE
                + sequence.random.nextFloat()
                        * (ENRAGE_LUNGE_MAX_DISTANCE - ENRAGE_LUNGE_MIN_DISTANCE);
        Vector2f target = Misc.getUnitVectorAtDegreeAngle(angle);
        target.scale(distance);
        Vector2f.add(target, ship.getLocation(), target);

        ship.setCustomData(
                BudaiConvulsiveLungeSystem.TARGET_KEY, new Vector2f(target));
        if (ship.getAIFlags() != null) {
            ship.getAIFlags().setFlag(
                    ShipwideAIFlags.AIFlags.SYSTEM_TARGET_COORDS,
                    1f,
                    new Vector2f(target));
        }
        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
        sequence.commanded = true;
        sequence.retryRemaining = ENRAGE_LUNGE_RETRY;
    }

    private static void finishEnrage(CombatEngineAPI engine, ShipAPI ship) {
        ship.removeCustomData(ENRAGE_SEQUENCE_KEY);
        ship.removeCustomData(BudaiConvulsiveLungeSystem.TARGET_KEY);

        ship.setCustomData(RADIUS_KEY, BASE_RADIUS * ENRAGED_RADIUS_MULT);

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon == null
                    || (!BUDAI_RIFT_ID.equals(weapon.getId())
                            && !STOCK_RIFT_ID.equals(weapon.getId()))) {
                continue;
            }

            float enragedDelay = weapon.getRefireDelay()
                    * ENRAGED_RIFT_REFIRE_MULT;
            weapon.setRefireDelay(enragedDelay);
            if (weapon.getCooldownRemaining() > enragedDelay) {
                weapon.setRemainingCooldownTo(enragedDelay);
            }
        }

    }

    private static float getCurrentRadius(ShipAPI ship) {
        Object value = ship.getCustomData().get(RADIUS_KEY);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        ship.setCustomData(RADIUS_KEY, BASE_RADIUS);
        return BASE_RADIUS;
    }

    private static void applySlowdown(
            CombatEngineAPI engine, ShipAPI charybdis, float radius) {
        String id = SLOW_ID_PREFIX + charybdis.getId();
        for (ShipAPI target : engine.getShips()) {
            if (target == null) {
                continue;
            }
            boolean inside = target.isAlive()
                    && Misc.getDistance(charybdis.getLocation(), target.getLocation()) <= radius;
            if (inside) {
                modifyMovement(target.getMutableStats(), id, MOVEMENT_MULT);
            } else {
                unmodifyMovement(target.getMutableStats(), id);
            }
        }
    }

    private static void clearSlowdown(CombatEngineAPI engine, ShipAPI charybdis) {
        String id = SLOW_ID_PREFIX + charybdis.getId();
        for (ShipAPI target : engine.getShips()) {
            if (target != null) {
                unmodifyMovement(target.getMutableStats(), id);
            }
        }
    }

    private static void modifyMovement(
            MutableShipStatsAPI stats, String id, float mult) {
        stats.getMaxSpeed().modifyMult(id, mult);
        stats.getAcceleration().modifyMult(id, mult);
        stats.getDeceleration().modifyMult(id, mult);
        stats.getMaxTurnRate().modifyMult(id, mult);
        stats.getTurnAcceleration().modifyMult(id, mult);
    }

    private static void unmodifyMovement(MutableShipStatsAPI stats, String id) {
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
    }

    private static void advanceLightning(
            CombatEngineAPI engine, ShipAPI charybdis, float radius, float amount) {
        Object existing = charybdis.getCustomData().get(LIGHTNING_TIMER_KEY);
        IntervalUtil timer;
        if (existing instanceof IntervalUtil) {
            timer = (IntervalUtil) existing;
        } else {
            timer = new IntervalUtil(LIGHTNING_MIN_INTERVAL, LIGHTNING_MAX_INTERVAL);
            charybdis.setCustomData(LIGHTNING_TIMER_KEY, timer);
        }

        timer.advance(amount);
        if (!timer.intervalElapsed()) {
            return;
        }

        for (ShipAPI target : engine.getShips()) {
            if (target == null || target == charybdis || !target.isAlive()
                    || target.getOwner() == charybdis.getOwner()
                    || Misc.getDistance(charybdis.getLocation(), target.getLocation()) > radius) {
                continue;
            }

            float damage = target.isFighter()
                    ? FIGHTER_LIGHTNING_DAMAGE : LIGHTNING_DAMAGE;
            float emp = target.isFighter()
                    ? FIGHTER_LIGHTNING_EMP : LIGHTNING_EMP;
            Vector2f strikeOrigin = Misc.getPointAtRadius(
                    target.getLocation(), target.getCollisionRadius() + 180f);

            engine.spawnEmpArcPierceShields(
                    charybdis,
                    strikeOrigin,
                    null,
                    target,
                    DamageType.ENERGY,
                    damage,
                    emp,
                    100000f,
                    "tachyon_lance_emp_impact",
                    22f,
                    LIGHTNING_FRINGE,
                    LIGHTNING_CORE);
        }
    }

    private static void maintain(
            RippleDistortion ripple, ShipAPI ship, float radius) {
        ripple.setLocation(new Vector2f(ship.getLocation()));
        ripple.setSize(radius);
        ripple.setMaxSize(radius);
        ripple.setIntensity(DISTORTION_INTENSITY);
        ripple.setMaxIntensity(DISTORTION_INTENSITY);
        // A fixed frame and zero internal rate make this a held field rather
        // than a one-second impact pulse, while keeping the frame below 60
        // prevents GraphicsLib from expiring the distortion.
        ripple.setFrameRate(0f);
        ripple.setCurrentFrame(DISTORTION_FRAME);
    }

    private static void removeDistortion(ShipAPI ship) {
        Object existing = ship.getCustomData().get(DISTORTION_KEY);
        if (existing instanceof RippleDistortion) {
            DistortionShader.removeDistortion((RippleDistortion) existing);
            ship.removeCustomData(DISTORTION_KEY);
        }
    }

    private static void applyPhysicalScale(ShipAPI ship) {
        if (ship.getCustomData().containsKey(SIZE_APPLIED_KEY)) {
            return;
        }

        SpriteAPI sprite = ship.getSpriteAPI();
        if (sprite != null) {
            sprite.setSize(sprite.getWidth() * BODY_SCALE,
                    sprite.getHeight() * BODY_SCALE);
            sprite.setCenter(sprite.getWidth() * 0.5f, sprite.getHeight() * 0.5f);
        }
        ship.setCollisionRadius(ship.getCollisionRadius() * BODY_SCALE);

        BoundsAPI bounds = ship.getExactBounds();
        if (bounds != null) {
            for (BoundsAPI.SegmentAPI segment : bounds.getOrigSegments()) {
                Vector2f p1 = segment.getP1();
                Vector2f p2 = segment.getP2();
                segment.set(
                        p1.x * BODY_SCALE,
                        p1.y * BODY_SCALE,
                        p2.x * BODY_SCALE,
                        p2.y * BODY_SCALE);
            }
            bounds.update(ship.getLocation(), ship.getFacing());
        }
        ship.setCustomData(SIZE_APPLIED_KEY, Boolean.TRUE);
    }

    private static void scaleWeaponSlotsOnce(ShipHullSpecAPI spec) {
        if (spec == null) {
            return;
        }
        synchronized (SCALED_HULL_SPECS) {
            if (!SCALED_HULL_SPECS.add(spec.getHullId())) {
                return;
            }
            for (WeaponSlotAPI copy : spec.getAllWeaponSlotsCopy()) {
                WeaponSlotAPI slot = spec.getWeaponSlot(copy.getId());
                if (slot != null && slot.getLocation() != null) {
                    slot.getLocation().scale(BODY_SCALE);
                }
            }
        }
    }

    private static Color addBlueFromRed(Color color) {
        if (color == null) {
            return null;
        }
        int blue = Math.min(255,
                Math.round(color.getBlue() + color.getRed() * BLUE_FROM_RED));
        return new Color(color.getRed(), color.getGreen(), blue, color.getAlpha());
    }

    private static final class EnrageSequence {
        private final Random random;
        private int completedLunges;
        private boolean commanded;
        private boolean enteredLunge;
        private float retryRemaining;
        private float restRemaining;
        private float elapsed;

        private EnrageSequence(ShipAPI ship) {
            int shipHash = ship.getId() == null ? 0 : ship.getId().hashCode();
            random = new Random(System.nanoTime() ^ shipHash);
        }
    }

    /** Gives the enlarged post-enrage field a restrained red cast. */
    private static final class CharybdisShipCreator
            extends ShroudedMawShipCreator {
        @Override
        protected DwellerCombatPlugin createPlugin(ShipAPI ship) {
            DwellerCombatPlugin plugin = super.createPlugin(ship);
            for (DwellerShipPart part : plugin.getParts()) {
                part.setColor(addBlueFromRed(part.getColor()));
                if (!(part instanceof BaseDwellerShipPart)) {
                    continue;
                }

                BaseDwellerShipPart base = (BaseDwellerShipPart) part;
                if (base.offset != null) {
                    base.offset.scale(BODY_SCALE);
                }
                if (part instanceof DwellerCombatPlugin.WobblyPart) {
                    DwellerCombatPlugin.WobblyPart wobbly =
                            (DwellerCombatPlugin.WobblyPart) part;
                    SpriteAPI sprite = wobbly.renderer.getSprite();
                    sprite.setSize(sprite.getWidth() * BODY_SCALE,
                            sprite.getHeight() * BODY_SCALE);
                    sprite.setCenter(sprite.getWidth() * 0.5f,
                            sprite.getHeight() * 0.5f);
                }
            }
            return plugin;
        }

        @Override
        protected void modifyBaselineShroudParams(
                ShipAPI ship, DwellerShroud.DwellerShroudParams params) {
            super.modifyBaselineShroudParams(ship, params);
            params.minOffset *= BODY_SCALE;
            params.maxOffset *= BODY_SCALE;
            params.baseSpriteSize *= BODY_SCALE;
            params.flashRadius *= BODY_SCALE;
            params.negativeParticleClearCenterAreaRadius *= BODY_SCALE;
            params.overloadGlowSizeMult *= BODY_SCALE;
            params.color = addBlueFromRed(params.color);
            params.flashFringeColor = addBlueFromRed(params.flashFringeColor);
            params.flashCoreColor = addBlueFromRed(params.flashCoreColor);
            params.negativeParticleColorOverride =
                    addBlueFromRed(params.negativeParticleColorOverride);
            params.overloadArcFringeColor =
                    addBlueFromRed(params.overloadArcFringeColor);
        }
    }

    private static final class OutsideBubbleDamageModifier
            implements DamageTakenModifier {
        private final ShipAPI charybdis;
        private float lastPopupTime = -1000f;

        private OutsideBubbleDamageModifier(ShipAPI charybdis) {
            this.charybdis = charybdis;
        }

        @Override
        public String modifyDamageTaken(
                Object param,
                CombatEntityAPI target,
                DamageAPI damage,
                Vector2f point,
                boolean shieldHit) {
            if (target != charybdis || damage == null || !charybdis.isAlive()) {
                return null;
            }

            // An HE detonation that reaches the body is treated as expanding from
            // inside the distortion. It therefore receives normal armor/hull
            // damage instead of the outside-origin attenuation.
            if (!shieldHit && damage.getType() == DamageType.HIGH_EXPLOSIVE) {
                return null;
            }

            Vector2f origin = null;
            if (param instanceof DamagingProjectileAPI) {
                origin = ((DamagingProjectileAPI) param).getSpawnLocation();
            } else if (param instanceof BeamAPI) {
                origin = ((BeamAPI) param).getFrom();
            }

            if (origin != null
                    && Misc.getDistance(origin, charybdis.getLocation())
                            > getCurrentRadius(charybdis)) {
                damage.getModifier().modifyMult(
                        DAMAGE_REDUCTION_ID, OUTSIDE_DAMAGE_MULT);
                showEatenPopup(point);
                return DAMAGE_REDUCTION_ID;
            }
            return null;
        }

        private void showEatenPopup(Vector2f impactPoint) {
            CombatEngineAPI engine = Global.getCombatEngine();
            if (engine == null) {
                return;
            }
            float now = engine.getTotalElapsedTime(false);
            if (now - lastPopupTime < EATEN_POPUP_COOLDOWN) {
                return;
            }
            lastPopupTime = now;
            Vector2f location = impactPoint == null
                    ? new Vector2f(charybdis.getLocation())
                    : new Vector2f(impactPoint);
            engine.addFloatingText(
                    location,
                    "EATEN!",
                    EATEN_POPUP_SIZE,
                    EATEN_POPUP_COLOR,
                    charybdis,
                    0.25f,
                    0.5f);
        }
    }
}
