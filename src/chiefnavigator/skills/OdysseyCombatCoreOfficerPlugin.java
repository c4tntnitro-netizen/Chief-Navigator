package chiefnavigator.skills;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import java.awt.Color;
import java.util.Random;

/** Officer representation of the recoverable Odyssean anti-THREAT core. */
public final class OdysseyCombatCoreOfficerPlugin
        extends BaseAICoreOfficerPluginImpl
        implements AICoreOfficerPlugin {
    public static final String CORE_ID =
            "chief_navigator_odyssey_combat_core";
    private static final float AUTOMATED_POINTS_MULT = 2f;

    @Override
    public PersonAPI createPerson(
            String aiCoreId, String factionId, Random random) {
        PersonAPI person = Global.getFactory().createPerson();
        CommoditySpecAPI spec = Global.getSettings().getCommoditySpec(aiCoreId);

        person.setFaction(factionId);
        person.setAICoreId(aiCoreId);
        person.setName(new FullName(
                spec.getName(), "", FullName.Gender.ANY));
        person.setPortraitSprite("graphics/portraits/portrait_ai2b.png");
        person.setRankId(Ranks.SPACE_CAPTAIN);
        person.setPostId(null);
        person.setPersonality(Personalities.AGGRESSIVE);

        person.getStats().setSkipRefresh(true);
        person.getStats().setLevel(4);
        person.getStats().setSkillLevel(Skills.TARGET_ANALYSIS, 2);
        person.getStats().setSkillLevel(Skills.GUNNERY_IMPLANTS, 2);
        person.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
        person.getStats().setSkillLevel(
                OdysseyThreatHunterSkill.SKILL_ID, 1);
        person.getMemoryWithoutUpdate().set(
                AICoreOfficerPlugin.AUTOMATED_POINTS_MULT,
                AUTOMATED_POINTS_MULT);
        person.getStats().setSkipRefresh(false);
        return person;
    }

    @Override
    public void createPersonalitySection(
            PersonAPI person, TooltipMakerAPI tooltip) {
        Color text = person.getFaction().getBaseUIColor();
        Color background = person.getFaction().getDarkUIColor();
        tooltip.addSectionHeading(
                "Personality: aggressive",
                text,
                background,
                Alignment.MID,
                20f);
        tooltip.addPara(
                "Its surviving combat heuristics aggressively prioritize "
                        + "Starving Threat command and fabrication nodes.",
                10f);
    }
}
