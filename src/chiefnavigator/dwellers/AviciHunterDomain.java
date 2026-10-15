package chiefnavigator.dwellers;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import org.dark.shaders.distortion.DistortionShader;
import org.dark.shaders.distortion.RippleDistortion;
import org.lwjgl.util.vector.Vector2f;

/** One radius drives Budai's impulse field, localized system buffs and lightning. */
public final class AviciHunterDomain {
    public static final float MIN_RADIUS = 1000f;
    public static final float MAX_RADIUS = 4000f;
    public static final float TRANSITION_SECONDS = 5f;
    public static final String STATE_KEY = "chief_navigator_avici_hunter_domain_state";
    private static final float RADIUS_PER_SECOND = (MAX_RADIUS - MIN_RADIUS) / TRANSITION_SECONDS;
    private static final float DISTORTION_INTENSITY = 320f;
    private static final Color FRINGE = new Color(255, 0, 107, 255);
    private static final Color CORE = new Color(255, 190, 235, 255);

    private AviciHunterDomain() { }

    private static final class DomainState {
        float radius = MIN_RADIUS;
        float effectLevel;
        final String buffId;
        final Set<ShipAPI> buffed = Collections.newSetFromMap(new IdentityHashMap<>());
        final IntervalUtil lightning = new IntervalUtil(0.9f, 1.1f);
        RippleDistortion ripple;

        DomainState(ShipAPI source) {
            buffId = "chief_navigator_avici_hunter_buff_" + source.getId()
                    + "_" + System.identityHashCode(source);
        }
    }

    private static boolean isHunter(ShipAPI ship) {
        return ship != null && ship.getHullSpec() != null
                && AviciDwellerContent.isBudaiHull(ship.getHullSpec().getHullId());
    }

    private static DomainState state(ShipAPI ship) {
        Object existing = ship.getCustomData().get(STATE_KEY);
        if (existing instanceof DomainState domain) return domain;
        DomainState domain = new DomainState(ship);
        ship.setCustomData(STATE_KEY, domain);
        return domain;
    }

    /** Read by every consumer; no separate visual, damage or buff radius exists. */
    public static float getCurrentRadius(ShipAPI ship) {
        if (ship != null && ship.getCustomData() != null
                && ship.getCustomData().get(STATE_KEY) instanceof DomainState domain) {
            return domain.radius;
        }
        return MIN_RADIUS;
    }

    public static void applySystem(ShipAPI ship, float effectLevel) {
        if (!isHunter(ship)) return;
        DomainState domain = state(ship);
        domain.effectLevel = Float.isFinite(effectLevel) ? Math.max(0f, Math.min(1f, effectLevel)) : 0f;
        if (domain.effectLevel == 0f) clearBuffs(domain);
    }

    public static void advance(ShipAPI ship, float amount) {
        if (!isHunter(ship)) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (!ship.isAlive() || ship.isHulk()
                || engine != null && !engine.isEntityInPlay(ship)) {
            clear(ship);
            return;
        }
        if (engine == null || engine.isPaused() || amount <= 0f) return;
        DomainState domain = state(ship);
        float step = RADIUS_PER_SECOND * amount;
        domain.radius = domain.effectLevel > 0f
                ? Math.min(MAX_RADIUS, domain.radius + step)
                : Math.max(MIN_RADIUS, domain.radius - step);
        float radius = getCurrentRadius(ship);
        maintainBuffs(engine, ship, domain, radius);
        maintainRipple(ship, domain, radius);
        advanceLightning(engine, ship, domain, radius, amount);
    }

    private static void maintainBuffs(CombatEngineAPI engine, ShipAPI source,
            DomainState domain, float radius) {
        Set<ShipAPI> eligible = Collections.newSetFromMap(new IdentityHashMap<>());
        if (domain.effectLevel > 0f) {
            eligible.add(source);
            for (ShipAPI target : engine.getShips()) {
                if (target != null && target.isAlive() && !target.isHulk()
                        && !target.isFighter() && !target.isDrone()
                        && target.getOwner() == source.getOwner()
                        && AviciDwellerContent.isDomainAlly(target)
                        && Misc.getDistance(source.getLocation(), target.getLocation()) <= radius) {
                    eligible.add(target);
                }
            }
        }
        for (ShipAPI previous : domain.buffed) {
            if (!eligible.contains(previous)) clearBuff(previous, domain.buffId);
        }
        for (ShipAPI target : eligible) applyBuff(target, domain.buffId, domain.effectLevel);
        domain.buffed.clear();
        domain.buffed.addAll(eligible);
    }

    private static void applyBuff(ShipAPI target, String id, float effect) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getBallisticWeaponDamageMult().modifyPercent(id, 70f * effect);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, 70f * effect);
        stats.getMissileWeaponDamageMult().modifyPercent(id, 70f * effect);
        stats.getFluxDissipation().modifyPercent(id, 25f * effect);
        stats.getFluxCapacity().modifyPercent(id, 25f * effect);
        float fluxCost = 1f - 0.15f * effect;
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, fluxCost);
        stats.getEnergyWeaponFluxCostMod().modifyMult(id, fluxCost);
        stats.getMissileWeaponFluxCostMod().modifyMult(id, fluxCost);
        stats.getBallisticRoFMult().modifyPercent(id, 15f * effect);
        stats.getEnergyRoFMult().modifyPercent(id, 15f * effect);
        stats.getMissileRoFMult().modifyPercent(id, 15f * effect);
    }

    private static void clearBuff(ShipAPI target, String id) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getMissileWeaponDamageMult().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        stats.getFluxCapacity().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getEnergyWeaponFluxCostMod().unmodify(id);
        stats.getMissileWeaponFluxCostMod().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getEnergyRoFMult().unmodify(id);
        stats.getMissileRoFMult().unmodify(id);
    }

    private static void clearBuffs(DomainState domain) {
        for (ShipAPI target : domain.buffed) clearBuff(target, domain.buffId);
        domain.buffed.clear();
    }

    private static void maintainRipple(ShipAPI ship, DomainState domain, float radius) {
        if (domain.ripple == null) {
            domain.ripple = new RippleDistortion(new Vector2f(ship.getLocation()), new Vector2f());
            domain.ripple.setLifetime(1000000f);
            DistortionShader.addDistortion(domain.ripple);
        }
        domain.ripple.setLocation(new Vector2f(ship.getLocation()));
        domain.ripple.setSize(radius);
        domain.ripple.setMaxSize(radius);
        domain.ripple.setIntensity(DISTORTION_INTENSITY);
        domain.ripple.setMaxIntensity(DISTORTION_INTENSITY);
        domain.ripple.setFrameRate(0f);
        domain.ripple.setCurrentFrame(30f);
    }

    private static void advanceLightning(CombatEngineAPI engine, ShipAPI source,
            DomainState domain, float radius, float amount) {
        domain.lightning.advance(amount);
        if (!domain.lightning.intervalElapsed()) return;
        for (ShipAPI target : engine.getShips()) {
            if (target == null || target == source || !target.isAlive() || target.isHulk()
                    || target.getOwner() != 0 && target.getOwner() != 1
                    || target.getOwner() == source.getOwner()
                    || Misc.getDistance(source.getLocation(), target.getLocation()) > radius) continue;
            Vector2f origin = Misc.getPointAtRadius(target.getLocation(), target.getCollisionRadius() + 180f);
            Vector2f offset = Vector2f.sub(origin, source.getLocation(), new Vector2f());
            if (offset.lengthSquared() > radius * radius) {
                offset.normalise();
                offset.scale(radius);
                origin = Vector2f.add(source.getLocation(), offset, new Vector2f());
            }
            engine.spawnEmpArcPierceShields(source, origin, null, target, DamageType.ENERGY,
                    target.isFighter() ? 35f : 125f, target.isFighter() ? 50f : 200f,
                    100000f, "tachyon_lance_emp_impact", 22f, FRINGE, CORE);
        }
    }

    public static void clear(ShipAPI ship) {
        if (ship == null || ship.getCustomData() == null) return;
        Object existing = ship.getCustomData().get(STATE_KEY);
        if (!(existing instanceof DomainState domain)) return;
        clearBuffs(domain);
        if (domain.ripple != null) DistortionShader.removeDistortion(domain.ripple);
        ship.removeCustomData(STATE_KEY);
    }
}
