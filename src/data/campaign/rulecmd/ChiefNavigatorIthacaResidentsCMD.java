package data.campaign.rulecmd;

import chiefnavigator.quest.FobIthacaContacts;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Gameplay effects for Ithaca's rules-authored resident conversations. */
public final class ChiefNavigatorIthacaResidentsCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || params.isEmpty() || dialog.getPlugin() == null) {
            return false;
        }
        Object context = dialog.getPlugin().getContext();
        if (!(context instanceof PersonAPI)) return false;
        PersonAPI survivor = (PersonAPI) context;
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null || local != survivor.getMemoryWithoutUpdate()
                || !"chief_navigator_league_future".equals(local.getString("$option"))) {
            return false;
        }
        String action = params.get(0).getString(memoryMap);
        if ("canRecruitLeagueCrew".equals(action)) {
            return FobIthacaContacts.canGrantLeagueConversationCrew(
                    dialog.getInteractionTarget(), survivor);
        }
        if ("recruitLeagueCrew".equals(action)) {
            return FobIthacaContacts.grantLeagueConversationCrew(
                    dialog.getInteractionTarget(), survivor);
        }
        return false;
    }
}
