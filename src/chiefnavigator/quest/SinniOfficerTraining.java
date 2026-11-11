package chiefnavigator.quest;

import chiefnavigator.skills.AdeptOfTheStarsSkill;
import chiefnavigator.skills.AdeptOfTheWavesSkill;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.plugins.OfficerLevelupPlugin;

/** Sets Sinni's starting stats and preserves saved officer progression. */
public final class SinniOfficerTraining {
    public static final int MAX_LEVEL = 8;

    private SinniOfficerTraining() { }

    public static void prepareOfficer(PersonAPI person, boolean newRecruit) {
        if (!newRecruit || !SinniContact.isOwnedPerson(person)) return;
        person.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, MAX_LEVEL);
        MutableCharacterStatsAPI stats = person.getStats();
        OfficerLevelupPlugin leveling = (OfficerLevelupPlugin) Global.getSettings().getPlugin("officerLevelUp");
        stats.setLevel(2);
        stats.setXP(leveling.getXPForLevel(2));
        stats.setSkillLevel(AdeptOfTheStarsSkill.ID, 1f);
        stats.setSkillLevel(AdeptOfTheWavesSkill.ID, 1f);
    }
}
