package chiefnavigator.skills;

import chiefnavigator.hullmods.StarvingThreatHullmod;
import com.fs.starfarer.api.characters.AfterShipCreationSkillEffect;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.lwjgl.util.vector.Vector2f;

/** The Odyssean core's Derelict-only anti-Starving-Threat protocol. */
public final class OdysseyThreatHunterSkill {
    public static final String SKILL_ID =
            "chief_navigator_odyssey_threat_hunter";
    public static final float BONUS_DAMAGE_PERCENT = 30f;
    private static final String DAMAGE_MOD_ID =
            "chief_navigator_odyssey_threat_hunter_damage";
    private static final String STARVING_THREAT_TAG =
            "chief_navigator_starving_threat";
    private static final Set<String> DERELICT_HULLS = new HashSet<String>(
            Arrays.asList(
                    "picket",
                    "defender",
                    "warden",
                    "bastillon",
                    "berserker",
                    "rampart",
                    "sentry",
                    "guardian"));

    private OdysseyThreatHunterSkill() {
    }

    public static final class Level1
            extends BaseSkillEffectDescription
            implements AfterShipCreationSkillEffect {
        @Override
        public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
            if (!isDerelictHull(ship)
                    || ship.hasListenerOfClass(ThreatHunterDamageListener.class)) {
                return;
            }
            ship.addListener(new ThreatHunterDamageListener());
        }

        @Override
        public void unapplyEffectsAfterShipCreation(ShipAPI ship, String id) {
            ship.removeListenerOfClass(ThreatHunterDamageListener.class);
        }

        @Override
        public void apply(
                MutableShipStatsAPI stats,
                ShipAPI.HullSize hullSize,
                String id,
                float level) {
        }

        @Override
        public void unapply(
                MutableShipStatsAPI stats,
                ShipAPI.HullSize hullSize,
                String id) {
        }

        @Override
        public String getEffectDescription(float level) {
            return null;
        }

        @Override
        public void createCustomDescription(
                MutableCharacterStatsAPI stats,
                SkillSpecAPI skill,
                TooltipMakerAPI info,
                float width) {
            init(stats, skill);
            info.addPara(
                    "While installed in a Domain Derelict hull, deals %s "
                            + "more damage to Starving Threat ships.",
                    0f,
                    hc,
                    "+" + (int) BONUS_DAMAGE_PERCENT + "%");
            info.addPara(
                    "The protocol remains dormant in non-Derelict automated "
                            + "hulls and has no effect against other factions.",
                    10f,
                    tc);
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    private static boolean isDerelictHull(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) return false;
        String baseHullId = ship.getHullSpec().getBaseHullId();
        String hullId = ship.getHullSpec().getHullId();
        return DERELICT_HULLS.contains(baseHullId)
                || DERELICT_HULLS.contains(hullId);
    }

    private static boolean isStarvingThreat(ShipAPI target) {
        if (target == null || target.getHullSpec() == null) return false;
        if (target.getHullSpec().hasTag(STARVING_THREAT_TAG)) return true;
        return target.getVariant() != null
                && target.getVariant().hasHullMod(
                        StarvingThreatHullmod.HULLMOD_ID);
    }

    public static final class ThreatHunterDamageListener
            implements DamageDealtModifier {
        @Override
        public String modifyDamageDealt(
                Object param,
                CombatEntityAPI target,
                DamageAPI damage,
                Vector2f point,
                boolean shieldHit) {
            if (!(target instanceof ShipAPI)
                    || !isStarvingThreat((ShipAPI) target)) {
                return null;
            }
            damage.getModifier().modifyPercent(
                    DAMAGE_MOD_ID, BONUS_DAMAGE_PERCENT);
            return DAMAGE_MOD_ID;
        }
    }
}
