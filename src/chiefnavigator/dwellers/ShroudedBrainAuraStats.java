package chiefnavigator.dwellers;

// Adapted from TIDAL's Shroud Expansion Pack, revision e2bafc5, with permission.

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import java.awt.Color;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Neural Web tracks its own targets, including allies that leave or change sides. */
public final class ShroudedBrainAuraStats extends BaseShipSystemScript {
    private static final float AURA_RADIUS = 6000f;
    private static final String STATE_KEY = "chief_navigator_avici_brain_aura_state";
    private static final Color AURA_COLOR = new Color(132, 61, 190, 120);

    private static final class AuraState {
        final IntervalUtil interval = new IntervalUtil(0.2f, 0.2f);
        final Set<ShipAPI> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        final String buffId;
        AuraState(ShipAPI source, String id) {
            buffId = id + "_" + source.getId() + "_" + System.identityHashCode(source);
        }
    }

    private static AuraState state(ShipAPI source, String id) {
        Object existing = source.getCustomData().get(STATE_KEY);
        if (existing instanceof AuraState aura) return aura;
        AuraState aura = new AuraState(source, id);
        source.setCustomData(STATE_KEY, aura);
        return aura;
    }

    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI source)) return;
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine == null) return;
        if (!source.isAlive() || source.isHulk()) {
            unapply(stats, id);
            return;
        }
        if (engine.isPaused()) return;
        AuraState aura = state(source, id);
        aura.interval.advance(engine.getElapsedInLastFrame());
        if (aura.interval.intervalElapsed()) {
            Set<ShipAPI> eligible = Collections.newSetFromMap(new IdentityHashMap<>());
            for (ShipAPI target : engine.getShips()) {
                if (isEligible(source, target)
                        && Misc.getDistance(source.getLocation(), target.getLocation()) <= AURA_RADIUS) {
                    eligible.add(target);
                }
            }
            for (ShipAPI previous : aura.targets) {
                if (!eligible.contains(previous)) removeBuff(previous, aura.buffId);
            }
            for (ShipAPI target : eligible) applyBuff(target, aura.buffId, effectLevel);
            aura.targets.clear();
            aura.targets.addAll(eligible);
        }
        if (source == engine.getPlayerShip()) {
            engine.maintainStatusForPlayerShip(id, "graphics/icons/hullsys/targeting_feed.png",
                    "Neural Web", "Buffing nearby shrouded vessels", false);
        }
        source.setJitter(source, AURA_COLOR, 0.25f + 0.25f * effectLevel, 6, 0f, 8f);
        source.setJitterUnder(source, AURA_COLOR, 0.25f + 0.25f * effectLevel, 12, 16f);
    }

    private static boolean isEligible(ShipAPI source, ShipAPI target) {
        return target != null && target.getHullSpec() != null
                && target.isAlive() && !target.isHulk() && !target.isFighter()
                && target.getOwner() == source.getOwner()
                && AviciDwellerContent.isBrainAuraAlly(target.getHullSpec().getHullId());
    }

    private static void applyBuff(ShipAPI target, String id, float effect) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getMaxSpeed().modifyPercent(id, 40f * effect);
        stats.getAcceleration().modifyPercent(id, 25f * effect);
        stats.getDeceleration().modifyPercent(id, 25f * effect);
        stats.getTurnAcceleration().modifyPercent(id, 25f * effect);
        stats.getMaxTurnRate().modifyPercent(id, 25f * effect);
        stats.getFluxDissipation().modifyPercent(id, 30f * effect);
        stats.getBallisticWeaponDamageMult().modifyPercent(id, 30f * effect);
        stats.getEnergyWeaponDamageMult().modifyPercent(id, 30f * effect);
        target.setJitter(target, AURA_COLOR, 0.2f * effect, 4, 0f, 5f);
        target.setJitterUnder(target, AURA_COLOR, 0.2f * effect, 8, 12f);
    }

    private static void removeBuff(ShipAPI target, String id) {
        MutableShipStatsAPI stats = target.getMutableStats();
        stats.getMaxSpeed().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        stats.getMaxTurnRate().unmodify(id);
        stats.getFluxDissipation().unmodify(id);
        stats.getBallisticWeaponDamageMult().unmodify(id);
        stats.getEnergyWeaponDamageMult().unmodify(id);
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (stats == null || !(stats.getEntity() instanceof ShipAPI source)) return;
        clear(source);
    }

    /** Hullmod lifecycle fallback when the caster dies or leaves combat. */
    public static void clear(ShipAPI source) {
        if (source == null || source.getCustomData() == null) return;
        Object existing = source.getCustomData().get(STATE_KEY);
        if (existing instanceof AuraState aura) {
            for (ShipAPI target : aura.targets) removeBuff(target, aura.buffId);
            source.removeCustomData(STATE_KEY);
            source.setJitter(source, AURA_COLOR, 0f, 0, 0f, 0f);
            source.setJitterUnder(source, AURA_COLOR, 0f, 0, 0f, 0f);
        }
    }
}
