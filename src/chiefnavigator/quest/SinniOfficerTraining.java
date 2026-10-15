package chiefnavigator.quest;

import chiefnavigator.skills.AdeptOfTheStarsSkill;
import chiefnavigator.skills.AdeptOfTheWavesSkill;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;

/** Adds inherent skills without replacing a recruited officer's saved progression. */
public final class SinniOfficerTraining {
    public static final int MAX_LEVEL = 6;

    private SinniOfficerTraining() { }

    public static void ensureInherentSkills(PersonAPI person) {
        if (!SinniContact.isOwnedPerson(person)) return;
        person.getMemoryWithoutUpdate().set(MemFlags.OFFICER_MAX_LEVEL, MAX_LEVEL);
        MutableCharacterStatsAPI stats = person.getStats();
        if (stats.getSkillLevel(AdeptOfTheWavesSkill.ID) < 1f) {
            stats.setSkillLevel(AdeptOfTheWavesSkill.ID, 1f);
        }
        if (stats.getSkillLevel(AdeptOfTheStarsSkill.ID) < 1f) {
            stats.setSkillLevel(AdeptOfTheStarsSkill.ID, 1f);
        }
    }
}
