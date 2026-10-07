package data.campaign.rulecmd;

import chiefnavigator.quest.SinniContact;
import chiefnavigator.quest.SinniNavigationGuidance;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.List;
import java.util.Map;

/** Supplies Sinni's remote contact portrait and live navigation variables. */
public final class ChiefNavigatorSinniContactCMD extends BaseCommandPlugin {
    @Override
    public boolean execute(String ruleId, InteractionDialogAPI dialog,
                           List<Token> params,
                           Map<String, MemoryAPI> memoryMap) {
        if (params.isEmpty()
                || !"init".equals(params.get(0).getString(memoryMap))) {
            return false;
        }
        if (dialog != null) {
            com.fs.starfarer.api.characters.PersonAPI sinni =
                    SinniContact.getOrCreatePerson();
            if (sinni != null) {
                dialog.getVisualPanel().showPersonInfo(sinni, true);
            }
        }
        MemoryAPI local = memoryMap.get(MemKeys.LOCAL);
        if (local == null) return false;
        SinniNavigationGuidance.Guidance guidance =
                SinniNavigationGuidance.getCurrentGuidance();
        local.set("$chiefNavigatorSinniDestination",
                guidance.getDestination(), 0f);
        local.set("$chiefNavigatorSinniObjective",
                guidance.getObjective(), 0f);
        local.set("$chiefNavigatorSinniRoute", guidance.getRoute(), 0f);
        return true;
    }
}
