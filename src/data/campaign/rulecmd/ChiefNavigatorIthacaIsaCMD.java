package data.campaign.rulecmd;

import chiefnavigator.quest.FobIthacaContacts;
import chiefnavigator.quest.OdysseyExpanseSystem;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Presentation-only scope for Isa's placeholder Ithaca conversation. */
public final class ChiefNavigatorIthacaIsaCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
            List<Token> params, Map<String, MemoryAPI> memoryMap) {
        if (dialog == null || dialog.getPlugin() == null || memoryMap == null
                || !OdysseyExpanseSystem.isFobIthaca(dialog.getInteractionTarget())) return false;
        Object context = dialog.getPlugin().getContext();
        if (!(context instanceof PersonAPI)) return false;
        PersonAPI person = (PersonAPI) context;
        return FobIthacaContacts.isRosterIsa(person)
                && memoryMap.get(MemKeys.LOCAL) == person.getMemoryWithoutUpdate();
    }
}
