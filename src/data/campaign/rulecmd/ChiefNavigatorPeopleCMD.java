package data.campaign.rulecmd;

import chiefnavigator.quest.AviciHabitatEncounter;
import chiefnavigator.quest.ConversationPortraits;
import chiefnavigator.quest.FobIthacaContacts;
import chiefnavigator.quest.MenelausTrial;
import chiefnavigator.quest.OdysseyStrandedFleetsScript;
import chiefnavigator.quest.SinniContact;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.impl.campaign.rulecmd.ShowDefaultVisual;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Explicit, presentation-only cast assignments for authored dialogue pages. */
public final class ChiefNavigatorPeopleCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || Global.getSector() == null || params.isEmpty()) {
            return false;
        }
        String action = params.get(0).getString(memoryMap);
        if ("clear".equals(action) && params.size() == 1) {
            ConversationPortraits.show(dialog);
            if (dialog.getInteractionTarget() != null) {
                new ShowDefaultVisual().execute(ruleId, dialog,
                        Collections.<Token>emptyList(), memoryMap);
            }
            return true;
        }
        if (params.size() < 2) return false;
        boolean illustrated = "illustrated".equals(action);
        if (!illustrated && !"show".equals(action)) return false;
        int firstRole = illustrated ? 2 : 1;
        // An illustration may intentionally have no separate speaker cards.
        if (!illustrated && params.size() <= firstRole) return false;
        List<PersonAPI> people = new ArrayList<PersonAPI>();
        for (int i = firstRole; i < params.size(); i++) {
            String role = params.get(i).getString(memoryMap);
            PersonAPI person;
            if ("menelaus".equals(role)) {
                person = MenelausTrial.getOrCreateMenelaus();
            } else if ("sinni".equals(role)) {
                person = SinniContact.getOrCreatePerson();
            } else if ("aias".equals(role)) {
                person = FobIthacaContacts.getOrCreateSpartanCaptain();
            } else if ("voss".equals(role)) {
                person = OdysseyStrandedFleetsScript.getOrCreateVoss();
            } else if ("player".equals(role)) {
                person = Global.getSector().getPlayerPerson();
            } else if ("sara".equals(role)) {
                person = saraPortrait();
            } else if ("contact".equals(role)) {
                Object context = dialog.getPlugin() == null
                        ? null : dialog.getPlugin().getContext();
                person = context instanceof PersonAPI ? (PersonAPI) context : null;
            } else {
                return false;
            }
            if (person != null) people.add(person);
        }
        PersonAPI[] cast = people.toArray(new PersonAPI[people.size()]);
        if (illustrated) {
            String asset = params.get(1).getString(memoryMap);
            ConversationPortraits.showIllustrated(dialog,
                    Global.getSettings().getSpriteName("illustrations", asset), cast);
        } else {
            ConversationPortraits.show(dialog, cast);
        }
        return true;
    }

    /** A page-local display identity, never an officer, contact, or saved person. */
    private static PersonAPI saraPortrait() {
        if (Global.getSector().getMemoryWithoutUpdate()
                .getBoolean(AviciHabitatEncounter.SARA_DEAD)) return null;
        PersonAPI sara = Global.getFactory().createPerson();
        sara.setId("chief_navigator_sara");
        sara.setName(new FullName("Sara", "", FullName.Gender.FEMALE));
        sara.setGender(FullName.Gender.FEMALE);
        sara.setPortraitSprite("graphics/portraits/portrait37.png");
        sara.setFaction(Factions.PLAYER);
        return sara;
    }
}
