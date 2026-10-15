package chiefnavigator.skills;

import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.SkillSpecAPI;
import com.fs.starfarer.api.characters.ShipSkillEffect;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.skills.BaseSkillEffectDescription;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

/** Sinni's permanent maneuvering skill; independent of Helmsmanship. */
public final class AdeptOfTheWavesSkill {
    public static final String ID = "chief_navigator_adept_of_the_waves";
    public static final float MANEUVERABILITY_PERCENT = 25f;
    public static final float SPEED_PERCENT = 10f;
    public static final float ELITE_SPEED_FLAT = 15f;

    private AdeptOfTheWavesSkill() { }

    public static final class Level1 extends BaseSkillEffectDescription implements ShipSkillEffect {
        @Override
        public void apply(MutableShipStatsAPI stats, HullSize hullSize,
                          String id, float level) {
            stats.getAcceleration().modifyPercent(id, MANEUVERABILITY_PERCENT);
            stats.getDeceleration().modifyPercent(id, MANEUVERABILITY_PERCENT);
            // Match Helmsmanship's maneuverability convention for turn acceleration.
            stats.getTurnAcceleration().modifyPercent(id, MANEUVERABILITY_PERCENT * 2f);
            stats.getMaxTurnRate().modifyPercent(id, MANEUVERABILITY_PERCENT);
            stats.getMaxSpeed().modifyPercent(id, SPEED_PERCENT);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getAcceleration().unmodifyPercent(id);
            stats.getDeceleration().unmodifyPercent(id);
            stats.getTurnAcceleration().unmodifyPercent(id);
            stats.getMaxTurnRate().unmodifyPercent(id);
            stats.getMaxSpeed().unmodifyPercent(id);
        }

        @Override
        public String getEffectDescription(float level) { return null; }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats,
                SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara("%s maneuverability", 0f, hc, "+25%");
            info.addPara("%s top speed", 0f, hc, "+10%");
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
            stats.getMaxSpeed().modifyFlat(id, ELITE_SPEED_FLAT);
            stats.getAllowZeroFluxAtAnyLevel().modifyFlat(id, 1f);
        }

        @Override
        public void unapply(MutableShipStatsAPI stats, HullSize hullSize, String id) {
            stats.getMaxSpeed().unmodifyFlat(id);
            stats.getAllowZeroFluxAtAnyLevel().unmodifyFlat(id);
        }

        @Override
        public String getEffectDescription(float level) { return null; }

        @Override
        public void createCustomDescription(MutableCharacterStatsAPI stats,
                SkillSpecAPI skill, TooltipMakerAPI info, float width) {
            init(stats, skill);
            info.addPara("%s su/second to top speed", 0f, hc, "+15");
            info.addPara("The 0-flux speed boost is activated at any flux level, "
                    + "if the ship is not generating flux or is venting / overloaded",
                    0f, tc);
        }

        @Override
        public ScopeDescription getScopeDescription() {
            return ScopeDescription.PILOTED_SHIP;
        }
    }
}
