package chiefnavigator.skills;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

/** Sinni's permanent system skill; independent of Systems Expertise. */
public final class AdeptOfTheStarsSkill {
    public static final String ID = "chief_navigator_adept_of_the_stars";
    public static final float BONUS_CHARGES = 1f;
    public static final float REGEN_PERCENT = 10f;
    public static final float RANGE_PERCENT = 10f;
    public static final float COOLDOWN_MULT = 0.85f;
    public static final float ELITE_DAMAGE_TAKEN_MULT = 0.85f;

    private AdeptOfTheStarsSkill() { }

    public static final class Level1 extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, HullSize hullSize,
                          String id, float level) {
            stats.getSystemUsesBonus().modifyFlat(id, BONUS_CHARGES);
            stats.getSystemRegenBonus().modifyPercent(id, REGEN_PERCENT);
            stats.getSystemRangeBonus().modifyPercent(id, RANGE_PERCENT);
            stats.getSystemCooldownBonus().modifyMult(id, COOLDOWN_MULT);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getSystemUsesBonus().unmodifyFlat(id);
            stats.getSystemRegenBonus().unmodifyPercent(id);
            stats.getSystemRangeBonus().unmodifyPercent(id);
            stats.getSystemCooldownBonus().unmodifyMult(id);
        }

        @Override
        public String getEffectDescription(float level) { return null; }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats,
                SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara("If the ship's system has charges: %s charge", 0f, hc, "+1");
            info.addPara("If the ship's system regenerates charges: %s regeneration rate",
                    0f, hc, "+10%");
            info.addPara("If the ship's system has range: %s range", 0f, hc, "+10%");
            info.addPara("If the ship's system has a cooldown: %s cooldown",
                    0f, hc, "-15%");
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }

    public static final class Elite extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, HullSize hullSize,
                          String id, float level) {
            stats.getArmorDamageTakenMult().modifyMult(id, ELITE_DAMAGE_TAKEN_MULT);
            stats.getHullDamageTakenMult().modifyMult(id, ELITE_DAMAGE_TAKEN_MULT);
            stats.getShieldDamageTakenMult().modifyMult(id, ELITE_DAMAGE_TAKEN_MULT);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getArmorDamageTakenMult().unmodifyMult(id);
            stats.getHullDamageTakenMult().unmodifyMult(id);
            stats.getShieldDamageTakenMult().unmodifyMult(id);
        }

        @Override
        public String getEffectDescription(float level) { return null; }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats,
                SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara("%s damage taken", 0f, hc, "-15%");
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
}
